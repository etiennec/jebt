package lib.sbite.parser;

import java.util.Map;

/**
 * Created by canaud on 10/29/2016.
 */
public class BeanEvaluator {

    private final Map<String, Object> data;

    public BeanEvaluator(Map<String, Object> data) {
        this.data = data;
    }


    public Object evaluate(String expr) {
        return new SbitWriterTextProcessor().evaluateExpression(expr, data);
    }
}
