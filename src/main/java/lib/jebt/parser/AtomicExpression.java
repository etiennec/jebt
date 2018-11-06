package lib.jebt.parser;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An Atomic Expression is the result of parsing an element of a JSon Path.
 *
 * You can then either "get" the result of this expression on a JSON object (doResolve()), or set a value to the result of this expression on a JSON object (setValue()).
 */
public abstract class AtomicExpression {

    // Path is only used for debugging purpose, it's not used in the logic.
    private String path;

    public final Object resolve (Object json, Map context) {
        try {
            Object obj = doResolve(json, context);

            if (obj != null && obj instanceof String) {
                return convertToOtherType(obj.toString());
            } else {
                return obj;
            }

        } catch (Exception e) {
            return null;
        }
    }

    public final void setValue(Object obj, Object value, Map context) throws Exception {
            if (value != null && value instanceof String) {
                value = convertToOtherType(value.toString());
            }

            doSetValue(obj, value, context);
    }

    // In order to comply with JSon spec, we should handle booleans and doubles and return them as such.
    private Object convertToOtherType(String s) {
        if ("true".equals(s)) {
            return Boolean.TRUE;
        }
        if ("false".equals(s)) {
            return Boolean.FALSE;
        }
        try {
            return new Integer(Integer.parseInt(s));
        } catch (NumberFormatException e) {
            // Ignore.
        }
        try {
            return new Double(Double.parseDouble(s));
        } catch (NumberFormatException e) {
            // Ignore
        }
        return s;
    }

    // Set the value to the object according to the atomic expression.
    public abstract void doSetValue(Object object, Object value, Map context) throws Exception;

    // Apply the atomic expression to the passed parent bean to retrieve the child bean.
    protected abstract Object doResolve(Object parent, Map context) throws Exception;

    public String getPath() {
        return path;
    }

    public AtomicExpression setPath(String path) {
        this.path = path;
        return this;
    }

    // This will recursively parse all atomic elements from the path one after the other.
    public static List<AtomicExpression> extractAtomicExpressions(String jsonPath) {

        List<AtomicExpression> exprs = new ArrayList<AtomicExpression>();

        // First, trim the path start make sure there's no space or dot at the beginning.
        while (jsonPath.startsWith(" ") || jsonPath.startsWith(".")) {
            jsonPath = jsonPath.substring(1);
        }

        if (StringUtils.isBlank(jsonPath)) {
            // We reached the end of the path.
            return exprs;
        }

        Object nextElement = null;
        String remainingJsonPath = null;

        // Read the first element. It's either a property string or a [stuff]
        if (jsonPath.startsWith("[")) {
            // [stuff]
            String element = jsonPath.substring(1, jsonPath.indexOf("]"));
            try {
                nextElement = Integer.parseInt(element.trim());
            } catch (NumberFormatException e) {
                nextElement = cleanUpElementName(element);
            }

            remainingJsonPath = jsonPath.substring(jsonPath.indexOf("]")+1);
        } else {
            // property.
            StringBuilder propName = new StringBuilder();
            int i = 0;
            while (i < jsonPath.length() && jsonPath.charAt(i) != '.' && jsonPath.charAt(i) != '[') {
                propName.append(jsonPath.charAt(i));
                ++i;
            }
            nextElement = cleanUpElementName(propName.toString());
            if (StringUtils.isBlank((String)nextElement)) {
                throw new RuntimeException("Syntax error - empty identifier in path "+jsonPath);
            }
            remainingJsonPath = jsonPath.substring(propName.toString().length());
        }

        if (nextElement instanceof Integer) {
                // Indexed array access
                final int index = (Integer)nextElement;

                exprs.add(new AtomicExpression() {

                    @Override
                    public void doSetValue(Object object, Object value, Map context) throws Exception {
                        if (object instanceof List) {
                            List list = (List) object;
                            // We fill object with nulls if it's not the right size.
                            while (index >= list.size()) {
                                list.add(null);
                            }
                            // We set the value at the right index.
                            ((List) object).set(index, value);
                        } else  {
                            throw new RuntimeException("Value Setting error: Object accessed with [index] is not a List");
                        }
                    }

                    @Override
                    protected Object doResolve(Object obj, Map context) throws Exception {
                        if (obj instanceof List) {
                            return ((List<?>) obj).get(index);
                        }

                        // Not a list
                        return null;
                    }

                    @Override public boolean isExprOnArray() {
                        return true;
                    }
                }.setPath("["+index+"]"));

        } else {
            // Property access
            final String token = nextElement.toString();

            /** Let's not do $special$ #tokens# for now as they break bi-directionality. */

//            if ("$names$".equalsIgnoreCase(token)) {
//                // $names$ special token returns the keys of a JSONObject as a Collection<String>.
//                // It can only be used when generating document, not to be used for getting back data.
//                exprs.add(new AtomicExpression() {
//
//                    @Override public void doSetValue(Object obj, Object value, Map context) throws Exception {
//                        throw new RuntimeException("$names$ cannot be set, only read in a collection element.");
//                    }
//
//                    @Override protected Object doResolve(Object parent, Map context) throws Exception {
//                        Set keys = ((Map)parent).keySet();
//                        List names = new JSONArray();
//                        names.addAll(keys);
//                        return names;
//                    }
//
//                    @Override public boolean isExprOnArray() {
//                        return false;
//                    }
//
//                }.setPath("." + token));
//            } else if (token.startsWith("#") && token.endsWith("#")) {
//                // #token# referencing a string entry in context
//                final String attributeName = token.substring(1, token.length() - 1);
//                exprs.add(new AtomicExpression() {
//
//                    @Override public void doSetValue(Object obj, Object value, Map context) throws Exception {
//
//                        ((Map)obj).put(context.get(attributeName).toString(), value);
//                    }
//
//                    @Override protected Object doResolve(Object parent, Map context) throws Exception {
//                        return ((Map)parent).get(context.get(attributeName).toString());
//                    }
//
//                    @Override public boolean isExprOnArray() {
//                        return false;
//                    }
//                }.setPath("." + token));
//            } else {


                // Normal property access
                exprs.add(new AtomicExpression() {

                    @Override public void doSetValue(Object obj, Object value, Map context) throws Exception {

                        ((Map)obj).put(token, value);
                    }

                    @Override protected Object doResolve(Object parent, Map context) throws Exception {
                        return ((Map)parent).get(token);
                    }

                    @Override public boolean isExprOnArray() {
                        return false;
                    }
                }.setPath("." + token));

        }

        exprs.addAll(extractAtomicExpressions(remainingJsonPath));

        return exprs;


    }

    /**
     * We need to remove any quote or spaces around element names
     */
    private static String cleanUpElementName(String e) {
        while (e.startsWith("\'") || e.startsWith("\"") || e.startsWith(" ")) {
            e = e.substring(1);
        }

        while (e.endsWith("\'") || e.endsWith("\"") || e.endsWith(" ")) {
            e = e.substring(0, e.length()-1);
        }

        return e;
    }

    /**
     * @return true if the expression should be performed on an Array/List object, false if it should be performed on a Map/Object.
     */
    public abstract boolean isExprOnArray();
}
