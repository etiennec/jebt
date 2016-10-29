package lib.sbite.parser;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class in charge of processing raw text with Sbet Template expressions in it.
 * <br>
 * So far, it's not supported to invoke method with parameters; only parameterless methods() are allowed, as text within parenthesis is reserved for mapped properties access.
 */
public abstract class SbitCommonTextProcessor {

    /**
     * For now, we return an empty string when some value is null, but in the future we could just return null.
     * @return
     */
    protected String getNullResult() {
        return "";
    }



    // This will recursively parse all atomic elements one after the other.
    protected List<AtomicExpression> extractAtomicExpressions(String currentPath,  BeanEvaluator evaluator, String... expressions) {
        List<AtomicExpression> exprs = new ArrayList<AtomicExpression>();

        boolean mustAddDot = false;

        for (String expression : expressions) {

            if (mustAddDot) {
                currentPath += ".";
            } else {
                mustAddDot = true;
            }

            String remainingExpression = null;

            if (StringUtils.isBlank(expression)) {
                continue;
            }

            if (expression.startsWith("[")) {
                // Indexed property
                String indexStr = getLeadingText(expression.substring(1)); // removing leading [
                final int index = Integer.parseInt(indexStr);
                currentPath += "["+indexStr+"]";
                exprs.add(new AtomicExpression() {

                    @Override
                    public void set(Object bean, Object value) throws Exception {
                        if (bean.getClass().isArray()) {
                            Array.set(bean, index, value);
                        } else if (bean instanceof List) {
                            ((List) bean).set(index, value);
                        } else if (bean instanceof Iterable) {
                           throw new RuntimeException("Iterables can only be read, not set");
                        }
                    }

                    @Override
                    protected Object doResolve(Object obj) throws Exception {
                        if (obj.getClass().isArray()) {
                            return Array.get(obj, index);
                        } else if (obj instanceof List) {
                            return ((List<?>) obj).get(index);
                        } else if (obj instanceof Iterable) {
                            Iterator it = ((Iterable) obj).iterator();
                            obj = it.next();
                            for (int i = 0; i < index; i++) {
                                obj = it.next();
                            }
                            return obj;
                        }

                        // Not a list, array, or iterable
                        return null;
                    }
                }.setPath(currentPath));

                remainingExpression = expression.substring(indexStr.length() + 2); // We remove the [ and ]
            } else if (expression.startsWith("(")) {
                // Mapped Property
                final String content = getLeadingText(expression.substring(1)); // removing leading (
                currentPath += "("+content+")";
                String keyStr = content.trim();

                Object keyObj = null;

                // Key can be a litteral String (fields("address")) or a variable that has to be resolved to an object that will be the key (fields(keyValue)).
                if (keyStr.startsWith("\"") && keyStr.endsWith("\"")) {
                    // Litteral
                    keyObj = keyStr.substring(1, keyStr.length()-1);
                } else {
                    // Expression
                    keyObj = evaluator.evaluate(keyStr);
                }

                final Object key = keyObj;

                exprs.add(new AtomicExpression() {
                    @Override
                    public void set(Object bean, Object value) throws Exception {
                        ((java.util.Map) bean).put(key, value);
                    }

                    @Override
                    protected Object doResolve(Object obj) throws Exception {
                        return ((java.util.Map<?, ?>) obj).get(key);
                    }
                }.setPath(currentPath));
                remainingExpression = expression.substring(content.length() + 2); // We remove the ( and )
            } else if (expression.contains("()")) {
                // Method call without parameters, if it's there it's at the beggining of the expression, right after the .
                final String token = getLeadingText(expression);
                currentPath += token+"()";
                exprs.add(new AtomicExpression() {
                    @Override
                    public void set(Object bean, Object value) throws Exception{
                        // We expect the method to be a getter, and we replace it as a setter.
                        if (!token.startsWith("get")) {
                            throw new RuntimeException("Method "+token+" is not a getter and cannot be replaced by a setter to assign value");
                        }
                        String setterMethod = "set"+token.substring(3);

                        MethodUtils.invokeMethod(bean, token, value);
                    }

                    @Override
                    protected Object doResolve(Object obj) throws Exception {
                        return MethodUtils.invokeMethod(obj, token, null); // we only support parameterless invocation().
                    }
                }.setPath(currentPath));
                remainingExpression = expression.substring(token.length() + 2); // We add the ()
            } else {
                // Only single property remains.
                final String token = getLeadingText(expression);
                currentPath += token;
                exprs.add(new AtomicExpression() {

                    @Override
                    public void set(Object bean, Object value) throws Exception {
                        try {
                            // First we try to set the value if it's a public property. If it's not public, we won't do it.
                            Field field = bean.getClass().getDeclaredField(token);
                            field.set(bean, value);
                        } catch (Exception e) {
                            // If we cannot set the field as a public property, we try with BeanUtils. This will work if it's a setter.
                            // Note however that BeanUtils will do nothing and not throw an exception if the field is read-only.
                            BeanUtils.setProperty(bean, token, value);
                        }
                    }

                    @Override
                    protected Object doResolve(Object obj) throws Exception {
                        try {
                            return BeanUtils.getSimpleProperty(obj, token);
                        } catch (NoSuchMethodException e) {
                            // If BeanUtils cannot find the getter, we will read the field ; we'll only read it if it's public.
                            Field field = obj.getClass().getDeclaredField(token);
                            return field.get(obj);
                        }
                    }
                }.setPath(currentPath));
                remainingExpression = expression.substring(token.length());
            }

            exprs.addAll(extractAtomicExpressions(currentPath, evaluator, remainingExpression));

        }

        return exprs;

    }

    protected String getLeadingText(String expr) {
        expr = StringUtils.replace(expr, "[", "(");
        expr = StringUtils.replace(expr, "]", "(");
        expr = StringUtils.replace(expr, ")", "(");
        int endIndex = expr.indexOf('(');
        if (endIndex > -1) {
            return expr.substring(0, endIndex).trim();
        }
        return expr.trim();
    }



    protected abstract class AtomicExpression {

        private String path;

        public Object resolve (Object bean) {
            try {
                return doResolve(bean);
            } catch (Exception e) {
                return null;
            }
        }

        // Set the value to the bean according to the atomic expression.
        public abstract void set(Object bean, Object value) throws Exception;

        // Apply the atomic expression to the passed parent bean to retrieve the child bean.
        protected abstract Object doResolve(Object bean) throws Exception;

        public String getPath() {
            return path;
        }

        public AtomicExpression setPath(String path) {
            this.path = path;
            return this;
        }
    }


}
