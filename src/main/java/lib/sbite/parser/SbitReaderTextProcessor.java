package lib.sbite.parser;

import lib.sbite.Factory;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;

/**
 * Class in charge of processing raw text with Sbet Template elements in it.
 */
public class SbitReaderTextProcessor extends SbitCommonTextProcessor {

    /**
     * This value sets an approximate text length to match with template text before deciding that the expression value is correctly matched.
     * It's mostly set for performance purpose, we cannot keep the parsed string in memory indefinitely for memory consumption reasons.
     * <p>
     *     It has state property and is NOT thread-safe at all.
     * </p>
     */
    private final static int MAX_TEXT_LENGTH_TO_STOP_MATCHING_EXPRESSION = 1000;

    private Class defaultClass = String.class;

    private Map<String, Class> classesPerBeanPath = new HashMap<String, Class>();

    boolean ignoreInstantiationFailures = false;

    private boolean skipReadNextTemplateToken = false;

    private Token templateToken = null;

    private Map<String, Factory> factoriesPerBeanPath = new HashMap<String, Factory>();

    /**
     * Reads data character by character until it can find an expression in the template.
     * When that's the case, it reads the data until it can resume matching the post-templateToken template with the source document.
     * It then updates data with the document data that was used to fill in the template expression.
     */
    public void extractData(Reader templateReader, Reader documentReader, Map<String, Object> data) {

        SbitTextTokenizer templateTokenizer = new SbitTextTokenizer(templateReader);

        extractData(templateTokenizer, documentReader, data);

    }

    public void extractData(SbitTokenizer templateTokenizer, Reader documentReader, Map<String, Object> data) {
        skipReadNextTemplateToken = false;

        try {
            while ((skipReadNextTemplateToken && templateToken != null) || (templateToken = templateTokenizer.readNext()) != null) {
                boolean shouldBreak = processSingleToken(templateTokenizer, documentReader, data);
                if (shouldBreak) {
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *
     * @return true if loop should break (i.e. processing is finished), false if it should continue to next templateToken.
     */
    private boolean processSingleToken(SbitTokenizer templateTokenizer, Reader documentReader, Map<String, Object> data) throws IOException {

        if (templateToken.getType() == Token.TokenType.EXPRESSION) {
            // EXPRESSION: We must match the document text to find the expression value. So we read the document text until we match the next text to find the end of the expression value.
            String expression = templateToken.getText();

            StringBuilder textToMatch = new StringBuilder();

            StringBuilder value = new StringBuilder("");

            // We build the string to match to detect the end of the template value.
            while (textToMatch.length() <= MAX_TEXT_LENGTH_TO_STOP_MATCHING_EXPRESSION && (templateToken = templateTokenizer.readNext()) != null && templateToken.getType() == Token.TokenType.TEXT) {
                textToMatch.append(templateToken.getText());
            }

            if (templateToken != null && templateToken.getType() != Token.TokenType.TEXT) {
                // We've already read the next expression or loop, so we should not re-read it upon next templateToken processing
                skipReadNextTemplateToken = true;
            }

            if (templateToken != null && textToMatch.length() == 0) {
                if (templateToken.getType() == Token.TokenType.EXPRESSION ) {
                    // We've found a new expression immediately after the first one ; that's INVALID as it doesn't allow us to match the expression value.
                    throw new RuntimeException("Found two consecutive Expressions in the template with no text in-between. That's invalid when extracting data from document. {{" + expression + "}} / {{" + templateToken.getText() + "}}");
                } else {
                    // There's something wrong: We should have read some text here.
                    throw new RuntimeException("Fatal Error: We couldn't retrieve text to match even though there's more text to read from the template. Bug!");
                }
            }

            if (templateToken == null && textToMatch.length() == 0) {
                // No more data to read from template after the EXPRESSION: Everything left in the document is the expression value.
                int i;
                while ((i = documentReader.read()) != -1) {
                    char c = (char) i;
                    value.append(c);
                }
                updateData(expression, value.toString(), data);
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

            updateData(expression, value.toString(), data);
        } else if (templateToken.getType() == Token.TokenType.LOOP) {
            // LOOP: We have to match the document with the loop inner contents, and detect when we go out of the loop.
            final SbitTextTokenizer.LoopToken loopToken = (SbitTextTokenizer.LoopToken) templateToken;

            StringBuilder loopBreakerTextToMatch = new StringBuilder();

            StringBuilder value = new StringBuilder("");

            // We build the string to match to detect the end of the template value.
            while (loopBreakerTextToMatch.length() <= MAX_TEXT_LENGTH_TO_STOP_MATCHING_EXPRESSION && (templateToken = templateTokenizer.readNext()) != null && templateToken.getType() == Token.TokenType.TEXT) {
                loopBreakerTextToMatch.append(templateToken.getText());
            }

            if (templateToken != null && templateToken.getType() != Token.TokenType.TEXT) {
                // We've already read the next expression or loop, so we should not re-read it upon next templateToken processing
                skipReadNextTemplateToken = true;
            }

            // We now read text from the document and decide whether we're looking at a loop breaker (i.e. we have exited the loop) or not (we are parsing contents of the loop).
            // We cannot greedily read the document until we reach the loop breaker, because this content could be too large to fit into memory.
            boolean isLoopBreakerFound = false;
            Object loopedCollectionBean = null; // Must be array or list
            int loopedBeanIndex = 0;
            while (!isLoopBreakerFound) {
                String documentContent = tryToMatchLoopBreaker(documentReader, loopBreakerTextToMatch.toString());

                if (documentContent == null) {
                    // We successfully matched the loopbreaker
                    isLoopBreakerFound = true;
                    continue;
                }

                if (loopedCollectionBean == null) {
                    loopedCollectionBean = new BeanEvaluator(data).evaluateBean(loopToken.getCollectionBeanPath());
                    if (loopedCollectionBean == null) {
                        loopedCollectionBean = instantiateObject(loopToken.getCollectionBeanPath(), null);
                    }
                    if (loopedCollectionBean == null) {
                        throw new SbitEvaluationException("Couldn't instantiate collection "+loopToken.getCollectionBeanPath());
                    }
                    if (!(loopedCollectionBean instanceof List) && !(loopedCollectionBean.getClass().isArray())) {
                        throw new SbitEvaluationException("Looped object "+loopToken.getCollectionBeanPath()+" should be an array or a List but it's a " +loopedCollectionBean.getClass().toString());
                    }
                }

                Object loopedItem = null;
                if (loopedCollectionBean instanceof List) {
                    loopedItem = ((List)loopedCollectionBean).get(loopedBeanIndex);
                    if (loopedItem == null) {
                        // TODO
                        throw new RuntimeException("TODO - instantiate new List item object");
                    }
                } else {
                    // Array
                    loopedItem = ((Object[])loopedCollectionBean)[loopedBeanIndex];
                    if (loopedItem == null) {
                        // TODO
                        throw new RuntimeException("TODO - instantiate new array item object");
                    }
                }

                loopedBeanIndex++;

                data.put(loopToken.getItemBeanName(), loopedItem);


                // If we haven't found the loop breaker, it means that we have to match the document to one element of the loop.
                // We start by injecting what we've read from the document back into the reader.
                documentReader = new ReaderWithTextAtTheBeginning(documentContent, documentReader);

                final List<Token> loopTokens = loopToken.getLoopTokens();

                SbitTokenizer tokenListTokenizer = new SbitTokenizer() {

                    Iterator<Token> tokens = loopToken.getLoopTokens().iterator();

                    @Override
                    public Token readNext() {
                        if (!tokens.hasNext()) {
                            return null;
                        }

                        return tokens.next();
                    }
                };

                SbitReaderTextProcessor loopContentProcessor = new SbitReaderTextProcessor();
                loopContentProcessor.extractData(tokenListTokenizer, documentReader, data);

            }


        } else if (templateToken.getType() == Token.TokenType.TEXT){
            // TEXT - Let's read along the document to make sure the text matches.
            Reader toMatchReader = new StringReader(templateToken.getText());
            int i;
            while ((i = toMatchReader.read()) != -1) {
                int j = documentReader.read();

                if (i !=  j) {
                    throw new RuntimeException("Character mismatch between the template and the document : "+ (char)i + " / "+ (char)j);
                }
            }
        } else {
            throw new RuntimeException("SbitTextReaderProcessor doesn't know how to process templateToken type "+ templateToken.getType().toString());
        }

        return false;
    }

    /**
     * Tries to match the passed string against the reader. If it succeeds, return null, if it fails, return the text already read from the reader.
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
                documentContent.append(((char)docChar));

                if (docChar == -1) {
                    // we reached the end of the document before the end of the loop text;
                    // This means that it's a critical failure since we shouldn't end the document before matching the rest of the template
                    throw new SbitParseException("Reached the end of the document before we could find the loop breaker text '"+loopBreakerTextToMatch+"'");
                }

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

    private void updateData(String expression, String value, Map<String, Object> data) {

        if (StringUtils.isBlank(expression)) {
            return;
        }

        expression = expression.trim();

        String[] elements = StringUtils.split(expression, '.');

        final String key = getLeadingText(elements[0]);

        if (elements.length == 1 && key.equals(elements[0])) {
            // We're directly setting the String value in the data map.
            data.put(elements[0], instantiateObject(elements[0], value));
            return;
        }

        Object bean = data.get(key);

        if (bean == null) {
            bean = instantiateObject(key, null);
            if (bean == null) {
                // We failed to instantiate the object and ignore failures (otherwise we would have got an exception by now).
                return;
            }
            data.put(key, bean);
        }

        // Removing data key from beginning of the expression
        elements[0] = elements[0].substring(key.length());

        List<AtomicExpression> atomicExprs = extractAtomicExpressions(key, new BeanEvaluator(data), elements);

        // We keep the last expression to assign instead of "get".
        AtomicExpression lastExpression = atomicExprs.remove(atomicExprs.size() -1);


        try {
            // climb up the expression ladder until the very last expression, instantiating null objects as we go up.

            for (AtomicExpression atomicExpr : atomicExprs) {
                Object childBean = atomicExpr.resolve(bean);
                if (childBean == null) {
                    childBean = instantiateObject(atomicExpr.getPath(), null);
                    if (childBean == null) {
                        // We failed to instantiate the object and ignore failures (otherwise we would have got an exception by now).
                        return;
                    }
                    atomicExpr.set(bean, childBean);
                    bean = childBean;
                }
            }

            // Use the very last expression to assign the value.
            lastExpression.set(bean, value);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * If value is null, we use no-arg constructor. Otherwise, we use String constructor.
     */
    private Object instantiateObject(String beanPath, String value) {

        Factory f = factoriesPerBeanPath.get(beanPath);
        if (f != null) {
            return f.createObject();
        }

        Class clazz = classesPerBeanPath.get(beanPath);
        if (clazz == null) {
            clazz = defaultClass;
        }

        if (clazz == null) {
            if (ignoreInstantiationFailures) {
                return null;
            } else {
                throw new RuntimeException("Couldn't find the class to instantiate for bean path " + beanPath);
            }
        }

        try {
            if (value == null) {
                return clazz.newInstance();
            } else {
                return clazz.getDeclaredConstructor(String.class).newInstance(value);
            }
        } catch (Exception e) {
            if (ignoreInstantiationFailures) {
                return null;
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    public void setDefaultClassToInstantiate(Class defaultClass) {
        this.defaultClass = defaultClass;
    }

    public void setClasses(Map<String, Class> classesPerBeanPath) {
        this.classesPerBeanPath = classesPerBeanPath;
    }

    public void setIgnoreInstantiationFailures(boolean ignore) {
        this.ignoreInstantiationFailures = ignore;
    }

    public void setFactories(Map<String, Factory> factoriesPerBeanPath) { this.factoriesPerBeanPath = factoriesPerBeanPath;  }

    public void setFactory(String beanPath,  Factory factory) {
        this.factoriesPerBeanPath.put(beanPath, factory);
    }

    private class ReaderWithTextAtTheBeginning extends Reader {

        private boolean hasReachedPrefixReaderEnd = false;
        private Reader prefixReader;
        private Reader documentReader;

        public ReaderWithTextAtTheBeginning(String prefixString, Reader documentReader) {
            this.prefixReader = new StringReader(prefixString);
            this.documentReader = documentReader;
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
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

        @Override
        public void close() throws IOException {
            prefixReader.close();
            documentReader.close();
        }
    }
}
