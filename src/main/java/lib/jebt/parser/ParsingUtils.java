package lib.jebt.parser;

import org.apache.commons.lang3.StringUtils;

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
}
