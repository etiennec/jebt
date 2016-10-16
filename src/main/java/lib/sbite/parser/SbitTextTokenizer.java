package lib.sbite.parser;

import java.io.IOException;
import java.io.Reader;

/**
 * Reads through text, and returns tokens containing either some text, or a Sbet template expression. Text token have a limited max size. Expression don't, and are always returned as a whole.
 */
public class SbitTextTokenizer {

    private Reader reader;

    private int MAX_TEXT_LENGTH = 20;

    private boolean isInExpr = false;
    private boolean isEscaped = false;



    public enum TokenType {TEXT, EXPRESSION};

    public SbitTextTokenizer(Reader reader) {
        this.reader = reader;
    }

    public SbitTextTokenizer setMaxTextLength(int maxTextLength) {
        this.MAX_TEXT_LENGTH = maxTextLength;
        return this;
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

            // we return the next TEXT token
            int i = -1;
            StringBuilder text = new StringBuilder();
            while (text.length() <= MAX_TEXT_LENGTH && (i = reader.read()) != -1) {
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

                if (c == '{') {
                    // Is it an opening {{ ?
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
                    } else {
                        // No, not an opening {{
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
                    throw new RuntimeException("Found a single { within Sbet {{ expression }}:  {{"+expression.toString() );
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
        private String text;

        public TokenType getType() {
            return type;
        }

        public String getText() {
            return text;
        }
    }
}
