package lib.sbite.parser;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Reads through text, and returns tokens containing either some text, or a Sbet template expression. Text token have a limited max size. Expression don't, and are always returned as a whole.
 */
public class SbitTextTokenizer {

    private Reader reader;

    private int MAX_TEXT_TOKEN_LENGTH = 20;

    private boolean isInExpr = false;
    private boolean isInLoop = false;
    private boolean isEscaped = false;
    //private Queue<LoopToken> loopQueue = new LinkedList<LoopToken>();



    public enum TokenType {TEXT, EXPRESSION, LOOP};

    public SbitTextTokenizer(Reader reader) {
        this.reader = reader;
    }

    public SbitTextTokenizer setMaxTextLength(int maxTextLength) {
        this.MAX_TEXT_TOKEN_LENGTH = maxTextLength;
        return this;
    }

    /**
     * This method is not memory efficient and should only be used for Test purpose, or when it's guaranteed that the text to parse is of known & limited size.
     * @return
     */
    public List<Token> readAll() {
        List<Token> tokens = new ArrayList<Token>();
        Token token = null;
        while ((token = readNext()) != null) {
            tokens.add(token);
        }
        return tokens;
    }

    /**
     *
     * @return the next Token (TEXT or EXPRESSION) or null if reached the end of the Reader.
     */
    public Token readNext() {

        try {

            if (isInExpr) {
                // We read all the text until the closing }} and return the Expression token, or a TEXT token if it ends prematurely
                // While in an expression, we only care if we've got a closing }}.
                return readExpressionToken();
            }

            if(isInLoop) {
                return readLoopToken();

            }

            // we return the next TEXT token
            int i = -1;
            StringBuilder text = new StringBuilder();
            while (text.length() <= MAX_TEXT_TOKEN_LENGTH && (i = reader.read()) != -1) {
                char c = (char) i;

                // Closing any pending escaped character
                if (isEscaped && c != '{') {
                    isEscaped = false;
                    text.append('\\');
                }

                // While out of expression, we only care if we've got an escaped \{{  or an expr opening {{
                if (c == '\\') {
                    isEscaped = true;
                    continue;
                }

                // Looking for opening {{ or {[
                if (c == '{') {
                    // Is it an opening {{ or {[?
                    int j = reader.read();
                    if (j == -1) {
                        // End of the text
                        if (isEscaped) {
                            text.append('\\');
                            isEscaped = false;
                        }
                        text.append(c);
                        break;
                    }

                    char c2 = (char)j;
                    if (c2 == '{') {
                        if (isEscaped) {
                            // that's a real escaped \{{
                            text.append("{{");
                            isEscaped = false;
                            continue;
                        } else {
                            // Yes, opening {{, let's start an expression
                            isInExpr = true;
                            if (text.length() == 0) {
                                // Let's not return a TEXT token of size zero, doesn't make sense, better return directly the expression token.
                                return readNext();
                            }
                            return new Token(TokenType.TEXT, text.toString());
                        }
                    } else if (c2 == '[') {
                        if (isEscaped) {
                            // that's a real escaped \{[
                            text.append("{[");
                            isEscaped = false;
                            continue;
                        } else {
                            // Yes, opening {[, let's start a loop
                            isInLoop = true;
                            if (text.length() == 0) {
                                // Let's not return a TEXT token of size zero, doesn't make sense, better return directly the expression token.
                                return readNext();
                            }
                            return new Token(TokenType.TEXT, text.toString());
                        }
                    } else {
                        // No, not an opening {{ or {[
                        if (isEscaped) {
                            text.append('\\');
                            isEscaped = false;
                        }
                        text.append('{');
                        if (c2 == '\\') {
                            isEscaped = true;
                            continue;
                        } else {
                            text.append(c2);
                        }
                        continue;
                    }
                }

                text.append(c);
            }

            if (i == -1 && isEscaped) {
                // We need to flush the last backslash
                text.append('\\');
                isEscaped = false;
            }

            if (text.length() > 0) {
                return new Token(TokenType.TEXT, text.toString());
            } else {
                return null;
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Token readLoopToken() throws IOException {
        // We read the first LoopToken, and detect whether it is the closing loop token or the beginning of another (possibly nested) loop token.
        LoopToken baseLoopToken =  readLoopTag();

        if (StringUtils.isBlank(baseLoopToken.getCollectionBeanPath())) {
            // Closing loop tag - we don't create a new tokenizer, we just return it.
            return baseLoopToken;
        }

        LoopToken currentLoopToken = baseLoopToken;

        SbitTextTokenizer loopTokenizer = new SbitTextTokenizer(this.reader);

        while (true) {
            Token token = loopTokenizer.readNext();

            if (token == null) {
                // Premature end of loop. This is not allowed!
                throw new SbitParseException("Reached the end of the reader while still within a loop tag ("+currentLoopToken.getItemBeanName()+"). " +
                        "This is not allowed, all loop tags should be properly closed. ");
            }

            if (token.getType() != TokenType.LOOP) {
                currentLoopToken.addToken(token);
                continue;
            }
            // We reached a loop token. Is it the end of the current loopToken or a nested loop token?
            LoopToken newLoopToken = (LoopToken)token;

            if (StringUtils.isBlank(newLoopToken.getCollectionBeanPath())) {
                if ((StringUtils.isBlank(newLoopToken.getItemBeanName()) || newLoopToken.getItemBeanName().equals(currentLoopToken.getItemBeanName()))) {
                    // Closing current loop token
                    break;
                } else {
                    // Closing loop tag, but with a mismatching item bean name.
                    throw new SbitParseException("Mismatch between the item bean name of the opening tag ('"+currentLoopToken.getItemBeanName()+"') and of the closing tag ('"+newLoopToken.getItemBeanName()+"')");
                }
            } else {
                // Nested loop tag.
                currentLoopToken.addToken(token);
                continue;
            }

            // unreacheable code here.
        }

        return baseLoopToken;
    }

    /**
     * Reads a single loop tag.
     * @return
     * @throws IOException
     */
    private LoopToken readLoopTag() throws IOException  {
        int i;
        StringBuilder expression = new StringBuilder();

        while ((i = reader.read()) != -1) {
            char c = (char) i;

            if (c == ']') {
                int j = reader.read();
                if (j == -1) {
                    // We finish the text in the middle of a loop expression. This is an error!
                    expression.append(c);
                    throw new SbitParseException("Text finished in the middle of a Loop tag: ..."+expression.toString());
                }
                char c2 = (char) j;

                if (c2 == '}') {
                    // Loop tag is over.
                    isInLoop = false;
                    return new LoopToken(expression.toString());
                } else {
                    // One single } means a syntax error.
                    throw new SbitParseException("Found a single } within Sbit loop {[ expression ]}:  {["+expression.toString() );
                }
            } else {
                // Just another character in the expression.
                expression.append(c);
                continue;
            }
        }

        // if we're here, it means the text ended before expression closed. This is an error.
        throw new SbitParseException("Text finished right after an opening Loop tag {[");
    }

    /**
     *
     * @return an EXPRESSION token containing the expression text without the curly braces,
     * or a TEXT Token if the expression ended prematurely due to reaching the end of the Reader.
     * @throws IOException
     */
    private Token readExpressionToken() throws IOException {

        int i;
        StringBuilder expression = new StringBuilder();

        while ((i = reader.read()) != -1) {
            char c = (char) i;

            if (c == '}') {
                int j = reader.read();
                if (j == -1) {
                    // We finish the text in the middle of an expression; so this is a Text token.
                    expression.append(c);
                    return new Token(TokenType.TEXT, "{{"+expression.toString());
                }
                char c2 = (char) j;

                if (c2 == '}') {
                    // Expression is over.
                    isInExpr = false;
                    return new Token(TokenType.EXPRESSION, expression.toString());
                } else {
                    // One single } means a syntax error.
                    throw new SbitParseException("Found a single { within Sbet {{ expression }}:  {{"+expression.toString() );
                }
            } else {
                // Just another character in the expression.
                expression.append(c);
                continue;
            }
        }

        // if we're here, it means the text ended before expression closed. We return a text token.
        return new Token(TokenType.TEXT, "{{"+expression.toString());
    }

    public class Token {

        public Token(TokenType type, String text) {
            this.type = type;
            this.text = text;
        }

        private TokenType type;
        protected String text;

        public TokenType getType() {
            return type;
        }

        public String getText() {
            return text;
        }
    }

    /**
     * A Loop token will iterate over the contents of a Collection (or array) bean, one by one.<br>
     *     Within the loop, the current item of the collection will be referred to by a string "itemBeanName" defined as part of the loop tag.
     */
    public class LoopToken extends Token {

        private List<Token> innerTokens = new ArrayList<Token>();

        private String itemBeanName = "";

        private String collectionBeanPath = "";

        public LoopToken(String loopTagContent) {

            super(TokenType.LOOP, loopTagContent);

            String[] values =StringUtils.split(loopTagContent.trim(), '|');
            if (values.length == 0) {
                // Contents are empty. Closing loop without item name.
            } else if (values.length == 1) {
                // Only set the itemBeanName: Closing loop with item name.
                itemBeanName = values[0].trim();
            } else if (values.length == 2) {
                // Two values: Opening loop tag.
                collectionBeanPath = values[0].trim();
                itemBeanName = values[1].trim();
            } else {
                // Too many parameters
                throw new SbitParseException("Too many | in your loop tag '"+loopTagContent+"'. Only use one to separate collection bean path from item bean name!");
            }
        }

        public void addToken(Token token) {
            innerTokens.add(token);
        }

        public List<Token> getLoopTokens() {
            return new ArrayList<Token>(innerTokens);
        }

        public String getItemBeanName() {
            return itemBeanName;
        }

        public String getCollectionBeanPath() {
            return collectionBeanPath;
        }
    }
}
