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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Class in charge of processing raw text with Sbet Template elements in it.
 */
public class SbetTextProcessor {

    /**
     * This value sets an approximate text length to match with template text before deciding that the expression value is correctly matched.
     * It's mostly set for performance purpose, we cannot keep the parsed string in memory indefinitely for memory consumption reasons.
     */
    private final static int MAX_TEXT_LENGTH_TO_STOP_MATCHING_EXPRESSION = 1000;

    /**
     * @param sourceText A reader that reads template text.
     * @param outText A writer where we will write down the source text with all templating elements resolved.
     * @param data Data to use when filling template elements.
     */
    public void convertString(Reader sourceText, Writer outText, Map<String, Object> data) {

        SbetTextTokenizer tokenizer = new SbetTextTokenizer(sourceText);

        SbetTextTokenizer.Token token;

        try {
            while ((token = tokenizer.readNext()) != null) {
                if (token.getType() == SbetTextTokenizer.TokenType.EXPRESSION) {
                    outText.append(evaluateExpression(token.getText(), data));
                } else {
                    // TEXT Token
                    outText.append(token.getText());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    /**
     *
     * @param expression a Sbet expression (~ BeanUtils expression), without the enclosing curly braces
     * @param data the data to use to evaluate the expression
     * @return the evaluated expression, or an empty String if there's a null value along the path.
     */
    public String evaluateExpression(String expression, Map<String, Object> data) {

        if (StringUtils.isBlank(expression)) {
            return "";
        }

        expression = expression.trim();

        String[] elements = StringUtils.split(expression, '.');

        // elements[0] is special as it's always an element of the data Map<>.
        Object bean = evaluateAsFirstElement(elements[0], data);

        if (bean == null) {
            return getNullResult();
        }

        for (int i = 1 ;  i < elements.length ; i++) {
            bean = evaluateBetweenTheDotsExpression(bean, elements[i].trim());
            if (bean == null) {
                return "";
            }
        }

        return bean.toString();

    }


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
            // We're directly setting the value in the data map.
            data.put(elements[0], value);
            return;
        }

        Object obj = data.get(key);

        if (obj == null) {
            // TODO instantiate the object.
            obj = instantiateObject(T);
            if (obj == null) {
                // cannot instantiate, so we don't do anything. Logging later?
                return;
            }
            data.put(key, obj);
        }

        String remainingExpr = elements[0].substring(key.length());

        // TODO refactor to allow defining types to instantiate with factories and SbetReader#setClass. Also allow to decide behavior upon instantiation failure (ignore / throw exception)

        // TODO climb up the expression ladder until the very last expression, instantiating null objects as we go up.
        // Use the very last expression to assign the value.

        if (bean == null) {
            return getNullResult();
        }

        for (int i = 1 ;  i < elements.length ; i++) {
            bean = evaluateBetweenTheDotsExpression(bean, elements[i].trim());
            if (bean == null) {
                return "";
            }
        }
    }

    /**
     * For now, we return an empty string when some value is null, but in the future we could just return null.
     * @return
     */
    private String getNullResult() {
        return "";
    }



    /** Evaluates a single expression, i.e. what's between the dots. It can be:
     * <ul>
     *     <li>A simple property</li>
     *     <li>If ending with (), A method (usually a getter, but not only)</li
     *     <li>If ending with [index], an indexed value, working on both Array, List, or Iterable</li>
     *     <li>If ending with (key), a mapped value</li>
     * </ul>
     * @param bean
     * @param expression
     * @return
     */
    private Object evaluateBetweenTheDotsExpression(Object bean, String expression) {
        // We break down the single expression into atomic expression, and evaluate them one by one.
        for (AtomicExpression atomicExpr : extractAtomicExpressions(expression)) {
            bean = atomicExpr.resolve(bean);
            if (bean == null) {
                return null;
            }
        }

        return bean;
    }

    /**
     * Method specific for the first element that as to be invoked on the data Map.
     * @param element
     * @param data
     * @return
     */
    private Object evaluateAsFirstElement(String element, Map<String, Object> data) {

        final String key = getLeadingText(element);
        Object obj = data.get(key);

        if (obj == null) {
            return null;
        }

        String remainingExpr = element.substring(key.length());

        return evaluateBetweenTheDotsExpression(obj, remainingExpr);

    }

    // This will recursively parse all atomic elements one after the other.
    private List<AtomicExpression> extractAtomicExpressions(String expression) {
        List<AtomicExpression> exprs = new ArrayList<AtomicExpression>();

        String remainingExpression = null;

        if (StringUtils.isBlank(expression)) {
            return exprs;
        }

        if (expression.startsWith("[")) {
            // Indexed property
            String indexStr = getLeadingText(expression.substring(1)); // removing leading [
            final int index = Integer.parseInt(indexStr);
            exprs.add(new AtomicExpression() {
                @Override
                protected Object doResolve(Object obj) throws Exception {
                    if (obj.getClass().isArray()) {
                        return Array.get(obj, index);
                    } else if (obj instanceof List) {
                        return ((List<?>)obj).get(index);
                    } else if (obj instanceof Iterable) {
                        Iterator it = ((Iterable)obj).iterator();
                        obj = it.next();
                        for (int i = 0 ; i < index ; i++) {
                            obj = it.next();
                        }
                        return obj;
                    }

                    // Not a list, array, or iterable
                    return null;
                }
            });

            remainingExpression = expression.substring(indexStr.length()+2); // We remove the [ and ]
        }

        else if (expression.startsWith("(")) {
            // Mapped Property
            final String key = getLeadingText(expression.substring(1)); // removing leading (
            exprs.add(new AtomicExpression() {
                @Override
                protected Object doResolve(Object obj) throws Exception {
                    return  ((java.util.Map<?, ?>)obj).get(key);
                }
            });
            remainingExpression = expression.substring(key.length()+2); // We remove the ( and )
        }

        else if (expression.contains("()")) {
            // Method call, there will always be only one and at the beginning
            final String token = getLeadingText(expression);
            exprs.add(new AtomicExpression() {
                @Override
                protected Object doResolve(Object obj) throws Exception {
                    return MethodUtils.invokeMethod(obj, token, null); // we only support parameterless invocation().
                }
            });
            remainingExpression = expression.substring(token.length()+2); // We add the ()
        }
        else {
            // Only single property remains.
            final String token = getLeadingText(expression);
            exprs.add(new AtomicExpression() {
                @Override
                protected Object doResolve(Object obj) throws Exception {
                    try {
                        return BeanUtils.getSimpleProperty(obj, token);
                    } catch (NoSuchMethodException e) {
                        // If BeanUtils cannot find the getter, maybe we can read the field ; we'll only read it if it's public.
                        Field field = obj.getClass().getDeclaredField(token);
                        return field.get(obj);
                    }
                }
            });
            remainingExpression = expression.substring(token.length());
        }

        exprs.addAll(extractAtomicExpressions(remainingExpression));

        return exprs;
    }

    private String getLeadingText(String expr) {
        expr = StringUtils.replace(expr, "[", "(");
        expr = StringUtils.replace(expr, "]", "(");
        expr = StringUtils.replace(expr, ")", "(");
        int endIndex = expr.indexOf('(');
        if (endIndex > -1) {
            return expr.substring(0, endIndex).trim();
        }
        return expr.trim();
    }



    private abstract class AtomicExpression {
        public Object resolve (Object bean) {
            try {
                return doResolve(bean);
            } catch (Exception e) {
                return null;
            }
        }

        protected abstract Object doResolve(Object bean) throws Exception;
    }


}
