package lib.sbet;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class contains most common logic for text parsing & beans manipulation used by {@link SbetWriter} implementations.
 */
public abstract class BaseSbetWriter implements SbetWriter{


    /**
     *
     * @param input an input string, possibly with some templating elements
     * @return the output string, with all templating elements evaluated using the passed data.
     */
    public String convertString(String input, Map<String, Object> data) {
        Reader reader = new StringReader(input);
        Writer writer = new StringWriter();

        convertString(reader, writer, data);

        return writer.toString();
    }

    /**
     * API to be used when the amount of text can be very large and shouldn't be stored into memory.<br>
     *     If the text is rather small (for example, text in an Excel Cell), you'd better use {@link #convertString(String, Map)}.
     *
     * @param sourceText A reader that reads template text.
     * @param outText A writer where we will write down the source text with all templating elements resolved.
     * @param data Data to use when filling template elements.
     */
    public void convertString(Reader sourceText, Writer outText, Map<String, Object> data) {

        // Read characters one by one and write them to the output until we reach a template tag or an escaped character.
        int i;
        boolean inExpr = false;
        boolean isEscaped = false;
        StringBuilder expr = null;
        try {
            while ((i = sourceText.read()) != -1) {
                char c = (char)i;


                if (inExpr) {
                    // While in an expression, we only care if we've got a closing }}.
                    if (c == '}') {
                        int j = sourceText.read();
                        if (j == -1) {
                            // We finish the text in the middle of an expression; so the whole expression will be written
                            // as normal non-evaluated text to the output.
                            expr.append(c);
                            outText.append(expr.toString());
                            break;
                        }
                        char c2 = (char)j;
                        expr.append(c);
                        expr.append(c2);

                        if (c2 == '}') {
                            // Expression is over, let's evaluate it.
                            String exprResult = evaluateExpression(expr.toString(), data);
                            outText.append(exprResult);
                            inExpr = false;
                            continue;
                        } else {
                            // We continue the expression. One single } likely means a syntax error, but we don't care here.
                            expr.append(c);
                            expr.append(c2);
                            continue;
                        }
                    } else {
                        // Just another character in the expression.
                        expr.append(c);
                        continue;
                    }

                }


                // Closing any pending escaped character
                if (isEscaped && c != '{') {
                    isEscaped = false;
                    outText.append('\\');
                }

                // While out of expression, we only care if we've got an escaped \{{  or an expr opening {{
                if (c == '\\') {
                        isEscaped = true;
                        continue;
                }

                if (c == '{') {
                    // Is it an opening {{ ?
                    int j = sourceText.read();
                    if (j == -1) {
                        // End of the text
                        if (isEscaped) {
                            outText.append('\\');
                            isEscaped = false;
                        }
                        outText.append(c);
                        break;
                    }

                    char c2 = (char)j;
                    if (c2 == '{') {
                        if (isEscaped) {
                            // that's a real escaped \{{
                            outText.append("{{");
                            isEscaped = false;
                            continue;
                        } else {
                            // Yes, opening {{, let's start an expression
                            inExpr = true;
                            expr = new StringBuilder("{{");
                            continue;
                        }
                    } else {
                        // No, not an opening {{
                        if (isEscaped) {
                            outText.append('\\');
                            isEscaped = false;
                        }
                        outText.append('{');
                        if (c2 == '\\') {
                            isEscaped = true;
                            continue;
                        } else {
                            outText.append(c2);
                        }
                        continue;
                    }
                }

                outText.append(c);
            }

            if (isEscaped) {
                outText.append('\\');
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *
     * @param expression a Sbet expression (~ BeanUtils expression), with the enclosing curly braces
     * @param data the data to use to evaluate the expression
     * @return the evaluated expression, or an empty String if there's a null value along the path.
     */
    public String evaluateExpression(String expression, Map<String, Object> data) {
        expression = expression.trim();

        if (!expression.startsWith("{{") || !expression.endsWith("}}")) {
            throw new RuntimeException("Invalid expression (should start with {{ and end with }}): "+expression);
        }

        // Removing {{ and }}
        expression = expression.substring(2, expression.length()-2).trim();

        if ("".equals(expression)) {
            return "";
        }

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
     * For now, we return an empty string when some value is null, but in the future we could just return null.
     * @return
     */
    private String getNullResult() {
        return "";
    }

    /** Evaluates a single expression, i.e. what's between the dots. It can be:
     * <ul>
     *     <li>A simple property</li>
     *     <li>If ending with (), A method (usually a getter, but not only)</li>
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
