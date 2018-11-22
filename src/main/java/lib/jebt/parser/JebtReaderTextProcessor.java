package lib.jebt.parser;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;

/**
 * Class in charge of processing raw text with Jebt Template elements in it, and generate data back from a text file.
 */
public class JebtReaderTextProcessor {

    /**
     * This value sets an approximate text length to match with template text before deciding that the expression value is correctly matched.
     * It's mostly setValue for performance purpose, we cannot keep the parsed string in memory indefinitely for memory consumption reasons.
     * <p>
     * It has state property and is NOT thread-safe at all.
     * </p>
     */
    private final static int MAX_TEXT_LENGTH_TO_STOP_MATCHING_EXPRESSION = 1000;

    boolean ignoreInstantiationFailures = false;

    private boolean skipReadNextTemplateToken = false;

    private Token templateToken = null;

    /**
     * Reads data character by character until it can find an expression in the template.
     * When that's the case, it reads the data until it can resume matching the post-templateToken template with the source document.
     * It then updates data with the document data that was used to fill in the template expression.
     */
    public void extractData(Reader templateReader, Reader documentReader, Map data) {

        JebtTextTokenizer templateTokenizer = new JebtTextTokenizer(templateReader);

        extractData(templateTokenizer, documentReader, data);

    }

    public void extractData(JebtTokenizer templateTokenizer, Reader documentReader, Map data) {
        skipReadNextTemplateToken = false;

        try {
            while ((skipReadNextTemplateToken && templateToken != Token.EOD)
                    || (templateToken = templateTokenizer.readNext()) != Token.EOD) {
                boolean shouldBreak = processSingleToken(templateTokenizer, documentReader, data);
                if (shouldBreak || templateToken == Token.EOD) {
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return true if loop should break (i.e. processing is finished), false if it should continue to next templateToken.
     */
    private boolean processSingleToken(JebtTokenizer templateTokenizer, Reader documentReader, Map data)
            throws IOException
    {

        if (templateToken == Token.EOD) {
            return true;
        } else if (templateToken.getType() == Token.TokenType.EXPRESSION) {
            // EXPRESSION: We must match the document text to find the expression value. So we read the document text until we match the next text to find the end of the expression value.
            String expression = templateToken.getText();

            StringBuilder textToMatch = new StringBuilder();

            StringBuilder value = new StringBuilder("");

            // We build the string to match to detect the end of the template value.
            while (textToMatch.length() <= MAX_TEXT_LENGTH_TO_STOP_MATCHING_EXPRESSION
                    && (templateToken = templateTokenizer.readNext()) != Token.EOD
                    && templateToken.getType() == Token.TokenType.TEXT) {
                textToMatch.append(templateToken.getText());
            }

            if (templateToken != Token.EOD && templateToken.getType() != Token.TokenType.TEXT) {
                // We've already read the next expression or loop, so we should not re-read it upon next templateToken processing
                skipReadNextTemplateToken = true;
            }

            if (templateToken != Token.EOD && textToMatch.length() == 0) {
                if (templateToken.getType() == Token.TokenType.EXPRESSION) {
                    // We've found a new expression immediately after the first one ; that's INVALID as it doesn't allow us to match the expression value.
                    throw new RuntimeException(
                            "Found two consecutive Expressions in the template with no text in-between. That's invalid when extracting data from document. {{"
                                    + expression + "}} / {{" + templateToken.getText() + "}}");
                } else {
                    // There's something wrong: We should have read some text here.
                    throw new RuntimeException(
                            "Fatal Error: We couldn't retrieve text to match even though there's more text to read from the template. Bug!");
                }
            }

            if (templateToken == Token.EOD && textToMatch.length() == 0) {
                // No more data to read from template after the EXPRESSION: Everything left in the document is the expression value.
                int i;
                while ((i = documentReader.read()) != -1) {
                    char c = (char)i;
                    value.append(c);
                }
                ParsingUtils.updateData(expression, value.toString(), data);
                // We reached the end!
                return true;
            }

            // We now read the document and save the value until we can match the text to match and decide that we've got a value.
            int i;
            int characterToMatchIndex = 0;
            char charToMatch = textToMatch.toString().charAt(characterToMatchIndex);

            while ((i = documentReader.read()) != -1) {
                char c = (char)i;

                if (c == charToMatch) {
                    // We continue matching the string
                    ++characterToMatchIndex;
                    if (characterToMatchIndex >= textToMatch.length()) {
                        // We matched all the characters of the textToMatch.
                        break;
                    }

                    charToMatch = textToMatch.toString().charAt(characterToMatchIndex);
                } else {
                    // It doesn't match --> We're still in the expression value.
                    if (characterToMatchIndex > 0) {
                        value.append(textToMatch.toString().substring(0, characterToMatchIndex));
                    }

                    value.append(c);
                }
            }

            ParsingUtils.updateData(expression, value.toString(), data);
        } else if (templateToken.getType() == Token.TokenType.LOOP) {
            // LOOP: We have to match the document with the loop inner contents, and detect when we go out of the loop.
            final JebtTextTokenizer.LoopToken loopToken = (JebtTextTokenizer.LoopToken)templateToken;

            StringBuilder loopBreakerTextToMatch = new StringBuilder();

            StringBuilder value = new StringBuilder("");

            // We build the string to match to detect the end of the template value.
            while (loopBreakerTextToMatch.length() <= MAX_TEXT_LENGTH_TO_STOP_MATCHING_EXPRESSION
                    && (templateToken = templateTokenizer.readNext()) != Token.EOD
                    && templateToken.getType() == Token.TokenType.TEXT) {
                loopBreakerTextToMatch.append(templateToken.getText());
            }

            if (templateToken != Token.EOD && templateToken.getType() != Token.TokenType.TEXT) {
                // We've already read the next expression or loop, so we should not re-read it upon next templateToken processing
                skipReadNextTemplateToken = true;
            }

            // We now read text from the document and decide whether we're looking at a loop breaker (i.e. we have exited the loop) or not (we are parsing contents of the loop).
            // We cannot greedily read the document until we reach the loop breaker, because this content could be too large to fit into memory.
            boolean isLoopBreakerFound = false;
            int loopedBeanIndex = 0;
            JebtImprovedReader jebtDocumentReader = new JebtImprovedReader(documentReader);
            while (!isLoopBreakerFound && jebtDocumentReader.hasNext()) {
                String documentContent;
                if ("".equals(loopBreakerTextToMatch.toString())) {
                    // An empty loopBreakerTextToMatch means that the document finishes with the closing loop tag.
                    // So we must match all remaining text with loop inner tokens until reaching end of document.
                    documentContent = String.valueOf((char)jebtDocumentReader.read());
                } else {
                    documentContent = tryToMatchLoopBreaker(jebtDocumentReader, loopBreakerTextToMatch.toString());
                }

                if (documentContent == null) {
                    // We successfully matched the loopbreaker
                    isLoopBreakerFound = true;
                    continue;
                }

                List loopedCollectionList = ParsingUtils.initCollection(data, loopToken.getCollectionJsonPath(), loopedBeanIndex, loopToken.getLoopItemName());

                // If we haven't found the loop breaker, it means that we have to match the document to one element of the loop.
                // We start by injecting what we've read from the document back into the reader.
                jebtDocumentReader = new JebtImprovedReader(documentContent, jebtDocumentReader);

                final List<Token> loopTokens = loopToken.getLoopTokens();

                JebtTokenizer tokenListTokenizer = new JebtTokenizer() {

                    private boolean hasReturnedEOD = false;

                    Iterator<Token> tokens = loopToken.getLoopTokens().iterator();

                    @Override public Token readNext() {
                        if (!tokens.hasNext()) {
                            if (hasReturnedEOD) {
                                throw new RuntimeException("Cannot return EOD token in a loop more than once");
                            } else {
                                hasReturnedEOD = true;
                                return Token.EOD;
                            }
                        }

                        return tokens.next();
                    }
                };

                JebtReaderTextProcessor loopContentProcessor = new JebtReaderTextProcessor();
                loopContentProcessor.extractData(tokenListTokenizer, jebtDocumentReader, data);

                // Now that parsing of one item has been completed we need to retrieve it, store it in data and clean the context
                Object obj = data.get(loopToken.getLoopItemName());
                loopedCollectionList.set(loopedBeanIndex, obj);
                data.remove(loopToken.getLoopItemName());

                loopedBeanIndex++;
            }

        } else if (templateToken.getType() == Token.TokenType.TEXT) {
            // TEXT - Let's read along the document to make sure the text matches.
            Reader toMatchReader = new StringReader(templateToken.getText());
            int i;
            while ((i = toMatchReader.read()) != -1) {
                int j = (char)documentReader.read();

                if (i != j) {
                    throw new RuntimeException(
                            "Character mismatch between the template and the document : '" + (char)i + "' / '" + (char)j + "' for template text \""+templateToken.getText()+"\"");
                }
            }
        } else {
            throw new RuntimeException(
                    "SbitTextReaderProcessor doesn't know how to process templateToken type " + templateToken.getType()
                            .toString());
        }

        return false;
    }

    /**
     * Tries to match the passed string against the reader. If it succeeds, return null, if it fails, return the text already read from the reader.
     *
     * @return null if the text matches against the document, or the text already read from the reader if it doesn't match.
     */
    private String tryToMatchLoopBreaker(Reader documentReader, String loopBreakerTextToMatch) {
        Reader loopTextReader = new StringReader(loopBreakerTextToMatch);

        StringBuilder documentContent = new StringBuilder();

        try {
            while (true) {

                int loopTextChar = loopTextReader.read();

                if (loopTextChar == -1) {
                    // We reached the end of the loop text; if we're still here, it means it's a match.
                    return null;
                }

                int docChar = documentReader.read();

                if (docChar == -1) {
                    // we reached the end of the document before the end of the loop text;
                    // This means that it's a critical failure since we shouldn't end the document before matching the rest of the template
                    throw new JebtParseException(
                            "Reached the end of the document before we could find the loop breaker text: '"
                                    + loopBreakerTextToMatch + "'");
                }
                documentContent.append(((char)docChar));

                if (docChar != loopTextChar) {
                    // No match!
                    return documentContent.toString();
                }

                // By then we have both character matching, so we continue reading.
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * This reader class provides two features:
     * - A constructor that allows to add back text at the beginning of an existing reader
     * - a hasNext() method to check if the reader is at the end or not without calling read().
     */
    private class JebtImprovedReader extends Reader {

        private boolean hasReachedPrefixReaderEnd = false;

        private Reader prefixReader;

        // -2 = nextChar not read.
        private int nextChar = -2;

        private Reader documentReader;

        public JebtImprovedReader(String prefixString, Reader documentReader) {
            this.prefixReader = new StringReader(prefixString);
            this.documentReader = documentReader;
        }

        public JebtImprovedReader(Reader documentReader) {
            this.prefixReader = null;
            hasReachedPrefixReaderEnd = true;
            this.documentReader = documentReader;
        }

        public boolean hasNext() throws IOException {
            if (nextChar == -2) {
                nextChar = read();
            }

            return nextChar != -1;
        }

        @Override public int read(char[] cbuf, int off, int len) throws IOException {

            if (nextChar != -2) {
                StringReader buf = new StringReader(String.valueOf((char)nextChar));
                nextChar = -2;
                return buf.read(cbuf, off, len);
            }

            if (!hasReachedPrefixReaderEnd) {
                int value = prefixReader.read(cbuf, off, len);
                if (value == -1) {
                    hasReachedPrefixReaderEnd = true;
                } else {
                    return value;
                }
            }

            return documentReader.read(cbuf, off, len);
        }

        @Override public void close() throws IOException {
            if (prefixReader != null) {
                prefixReader.close();
            }
            documentReader.close();
        }
    }
}
