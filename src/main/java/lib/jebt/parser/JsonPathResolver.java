package lib.jebt.parser;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * This class will evaluate a JSON Path expression on a JSONObject.
 *
 * Our JSON Path supports following syntaxes:
 * <ul>
 *     <li>property.access.with.dots</li>
 *     <li>property[access]['with']["brackets"]</li>
 *     <li>indexed.array[0]</li>
 * </ul>
 *
 * @see AtomicExpression
 */
public class JsonPathResolver {

    private final Map data;

    public JsonPathResolver(Map data) {
        this.data = data;
    }

    /**
     * For now, we return a null when some value is null, but in the future we could decide to return an empty string.
     * @return
     */
    private String getNullResult() {
        return null;
    }


    /** Returns the String value of an expression on the data, or an empty string if the jsonPath is blank */
    public String evaluatePathToString(String jsonPath) {
        if (StringUtils.isBlank(jsonPath)) {
            return "";
        }
        return evaluateJSonPathToString(jsonPath, data);
    }

    /** returns a JSonObject or JSonArray or String / Number by evaluating the expression on the data */
    public Object evaluatePathToObject(String jsonPath) {
        return evaluateJSonPathToObject(jsonPath, data);
    }

    /**
     *
     * @param jsonPath a Sbet expression (~ BeanUtils expression), without the enclosing curly braces
     * @param data the data to use to evaluate the expression
     * @return the evaluated expression, or an empty String if there's a null value along the path.
     */
    private String evaluateJSonPathToString(String jsonPath, Map data) {

        Object obj = evaluateJSonPathToObject(jsonPath, data);

        if (obj == null) {
            return getNullResult();
        }

        return obj.toString();
    }

    /**
     * @return the result of the JSon path on the JSONObject data.
     * Result can be a JSONObject, a JSONArray or a String/Double
     */
    private Object evaluateJSonPathToObject(String jsonPath, Map data) {
        if (StringUtils.isBlank(jsonPath)) {
            return null;
        }


        List<AtomicExpression>
                atomicExprs = AtomicExpression.extractAtomicExpressions(jsonPath);

        Object obj = data;

        for (AtomicExpression atomicExpr : atomicExprs) {
            // We walk down the JSon path to retrieve the right JSon Object
            obj = atomicExpr.resolve(obj, data);
            if (obj == null) {
                return null;
            }
        }

        return obj;
    }
}
