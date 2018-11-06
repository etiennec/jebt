package lib.jebt;

import lib.jebt.parser.JsonPathResolver;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class JebtJSONPathResolverTest
{

    @Test
    public void testTemplateExpressionEvaluation()
    {
        Map data = new JSONObject();

        data.put("foo", "bar");

        JSONArray colorsArray = new JSONArray();
        colorsArray.addAll(Arrays.asList(new String[]{"red", "green", "blue"}));

        data.put("colorsArray", colorsArray);

        Map address = new JSONObject();
        address.put("number", 123);
        address.put("streetName", "Main Boulevard");
        address.put("postalCode", 200093L);

        List addresses = new JSONArray();
        addresses.add(address);

        data.put("addresses", addresses);

        JsonPathResolver parser = new JsonPathResolver(data);

        // We don't care about spaces in the expression
        assertEquals("bar", parser.evaluatePathToString("foo"));
        assertEquals("bar", parser.evaluatePathToString(" foo "));
        assertEquals("bar", parser.evaluatePathToString("foo "));
        assertEquals("bar", parser.evaluatePathToString(" foo"));

        // Indexed access
        assertEquals("red", parser.evaluatePathToString("colorsArray[0]"));
        assertEquals("green", parser.evaluatePathToString("colorsArray[1]"));
        assertEquals("blue", parser.evaluatePathToString("colorsArray[2]"));

        // property chain
        assertEquals("123", parser.evaluatePathToString("addresses[0].number"));
        assertEquals("Main Boulevard", parser.evaluatePathToString("addresses[0].streetName"));
        assertEquals("200093", parser.evaluatePathToString("addresses[0].postalCode"));
    }
}
