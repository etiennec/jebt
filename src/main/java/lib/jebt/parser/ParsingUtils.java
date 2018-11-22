package lib.jebt.parser;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by canaud on 10/18/2018.
 */
public class ParsingUtils {

    public static String getLeadingText(String expr) {
        expr = StringUtils.replace(expr, "]", "[");
        int endIndex = expr.indexOf('[');
        if (endIndex > -1) {
            return expr.substring(0, endIndex).trim();
        }
        return expr.trim();
    }

    /**
     * Code used in loops ; it'll initialize the collection object.
     */
    public static List initCollection(Map data, String collectionJSonPath, int loopedBeanIndex, String loopItemName) {

        Object loopedCollectionBean = new JsonPathResolver(data).evaluatePathToObject(collectionJSonPath);
        if (loopedCollectionBean == null) {
            loopedCollectionBean = new ArrayList();
            updateData(collectionJSonPath, loopedCollectionBean, data);
        }
        if (!(loopedCollectionBean instanceof List)) {
            throw new JebtEvaluationException(
                    "Looped object " + collectionJSonPath + " should be a List but it's a " + loopedCollectionBean
                            .getClass().toString());
        }

        Object loopedItem = null;

        List loopedCollectionList = (List)loopedCollectionBean;

        if (loopedBeanIndex >= 0) {
            // We need to fill the List with empty objects if it doesn't have enough items in it.
            while (loopedCollectionList.size() <= loopedBeanIndex) {
                // The list is not large enough, we have to add more data to it.
                loopedCollectionList.add(null);
            }
            loopedItem = loopedCollectionList.get(loopedBeanIndex);
            if (loopedItem == null) {
                loopedItem = new LinkedHashMap();
                loopedCollectionList.set(loopedBeanIndex, loopedItem);
            }
        }

        // We now put the object at the root of the data to make it available to context.
        data.put(loopItemName, loopedItem);

        return loopedCollectionList;
    }

    /**
     * Update the entry at the given path to the given value.
     *
     * @param jsonPath
     * @param value
     * @param data
     */
    public static void updateData(String jsonPath, Object value, Map data) {

        if (StringUtils.isBlank(jsonPath)) {
            return;
        }

        List<AtomicExpression> atomicExprs = AtomicExpression.extractAtomicExpressions(jsonPath);

        // We keep the last expression to assign instead of "get".
        AtomicExpression lastExpression = atomicExprs.remove(atomicExprs.size() - 1);

        Object obj = data;

        try {

            // climb up the expression ladder until the very last expression, instantiating null objects as we go up.
            for (int i = 0; i < atomicExprs.size(); i++) {
                AtomicExpression atomicExpr = atomicExprs.get(i);
                Object childBean = atomicExpr.resolve(obj, data);
                if (childBean == null) {
                    // We instantiate a new Child bean, either a Map or an ArrayList depending on the following token
                    AtomicExpression nextAtomicExpr =
                            i + 1 < atomicExprs.size() ? atomicExprs.get(i + 1) : lastExpression;
                    if (nextAtomicExpr.isExprOnArray()) {
                        // JSon Array
                        childBean = new ArrayList();
                    } else {
                        // JSon Object.
                        childBean = new LinkedHashMap();
                    }
                    atomicExpr.setValue(obj, childBean, data);
                }
                obj = childBean;
            }

            // Use the very last expression to assign the value.
            lastExpression.setValue(obj, value, data);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
