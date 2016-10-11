package lib.sbet.parser;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Class in charge of processing raw text with Sbet Template elements in it.
 */
public class SbetReaderTextProcessor extends SbetCommonTextProcessor {

    /**
     * This value sets an approximate text length to match with template text before deciding that the expression value is correctly matched.
     * It's mostly set for performance purpose, we cannot keep the parsed string in memory indefinitely for memory consumption reasons.
     */
    private final static int MAX_TEXT_LENGTH_TO_STOP_MATCHING_EXPRESSION = 1000;

    private Class defaultClass = String.class;

    private Map<String, Class> classesPerBeanName = new HashMap<String, Class>();

    boolean ignoreInstantiationFailures = false;

    /**
     * Reads data character by character until it can find an expression in the template.
     * When that's the case, it reads the data until it can resume matching the post-token template with the source document.
     * It then updates data with the document data that was used to fill in the template expression.
     */
    public void extractData(Reader templateReader, Reader documentReader, Map<String, Object> data) {

        SbetTextTokenizer tokenizer = new SbetTextTokenizer(templateReader);

        SbetTextTokenizer.Token token = null;

        boolean skipRead = false;

        try {
            while ((skipRead && token != null) || (token = tokenizer.readNext()) != null) {
                if (token.getType() == SbetTextTokenizer.TokenType.EXPRESSION) {
                    // EXPRESSION: We must match the document text to find the expression value. So we read the document text until we match the next text to find the end of the expression value.
                    String expression = token.getText();

                    StringBuilder textToMatch = new StringBuilder();

                    StringBuilder value = new StringBuilder("");

                    // We build the string to match to detect the end of the template value.
                    while (textToMatch.length() <= MAX_TEXT_LENGTH_TO_STOP_MATCHING_EXPRESSION && (token = tokenizer.readNext()) != null && token.getType() == SbetTextTokenizer.TokenType.TEXT) {
                        textToMatch.append(token.getText());
                    }

                    if (token != null && token.getType() == SbetTextTokenizer.TokenType.EXPRESSION) {
                        // We've already read the next expression, so we should not re-read it upon next loop
                        skipRead = true;
                    }

                    if (token != null && textToMatch.length() == 0) {
                        if (token.getType() == SbetTextTokenizer.TokenType.EXPRESSION ) {
                            // We've found a new expression immediately after the first one ; that's INVALID as it doesn't allow us to match the expression value.
                            throw new RuntimeException("Found two consecutive Expressions in the template with no text in-between. That's invalid when extracting data from document. {{" + expression + "}} / {{" + token.getText() + "}}");
                        } else {
                            // There's something wrong: We should have read some text here.
                            throw new RuntimeException("Fatal Error: We couldn't retrieve text to match even though there's more text to read from the template. Bug!");
                        }
                    }

                    if (token == null && textToMatch.length() == 0) {
                        // No more data to read from template after the EXPRESSION: Everything left in the document is the expression value.
                        int i;
                        while ((i = documentReader.read()) != -1) {
                            char c = (char) i;
                            value.append(c);
                        }
                        updateData(expression, value.toString(), data);
                        // We reached the end!
                        break;
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
                } else {
                    // TEXT - Let's read along the document to make sure the text matches.
                    Reader toMatchReader = new StringReader(token.getText());
                    int i;
                    while ((i = toMatchReader.read()) != -1) {
                        int j = documentReader.read();

                        if (i !=  j) {
                            throw new RuntimeException("Character mismatch between the template and the document : "+ i + " / "+ j);
                        }
                    }
                }
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
            data.put(elements[0], value);
            return;
        }

        Object bean = data.get(key);

        if (bean == null) {
            bean = instantiateObject(key);
            if (bean == null) {
                // We failed to instantiate the object and ignore failures (otherwise we would have got an exception by now).
                return;
            }
            data.put(key, bean);
        }

        // Removing data key from beginning of the expression
        elements[0] = elements[0].substring(key.length());

        List<AtomicExpression> atomicExprs = extractAtomicExpressions(key, elements);

        // We keep the last expression to assign instead of "get".
        AtomicExpression lastExpression = atomicExprs.remove(atomicExprs.size() -1);


        try {
            // climb up the expression ladder until the very last expression, instantiating null objects as we go up.
            for (AtomicExpression atomicExpr : atomicExprs) {
                bean = atomicExpr.resolve(bean);
                if (bean == null) {
                    Object childBean = instantiateObject(atomicExpr.getPath());
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

    private Object instantiateObject(String beanPath) {
        Class clazz = classesPerBeanName.get(beanPath);
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
            return clazz.newInstance();
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

    public void setClasses(Map<String, Class> classesPerBeanName) {
        this.classesPerBeanName = classesPerBeanName;
    }

    public void setClass(String beanPath,  Class clazz) {
        this.classesPerBeanName.put(beanPath, clazz);
    }

    public void setIgnoreInstantiationFailures(boolean ignore) {
        this.ignoreInstantiationFailures = ignore;
    }
}
