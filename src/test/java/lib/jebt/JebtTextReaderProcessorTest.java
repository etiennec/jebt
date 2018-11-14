package lib.jebt;

import org.json.simple.JSONObject;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class JebtTextReaderProcessorTest {

    @Test public void testExtractData()
    {
        BaseJebtReader reader = new BaseJebtReader() {
            @Override public Map readData() {
                return null;
            }
        };

        Map data = new JSONObject();

        // Templates with no bean elements shouldn't setValue anything.
        reader.extractData("", "", data);
        reader.extractData("Hello", "Hello", data);

        assertTrue(data.isEmpty());

        // Setting basic String values in the map

        reader.extractData("Hello {{foo}}!", "Hello bar!", data);
        assertEquals("bar", data.get("foo"));

        // Try to setValue an Integer as map value
        reader.extractData("This {{myNumber}} is delicious!", "This 3.1416 is delicious!", data);
        assertEquals(new Double(3.1416d), data.get("myNumber"));

        // Try to setValue a value in an existing array
        data.put("myArray", Arrays.asList(new String[] {"Hello", "Jim"}));
        assertEquals("Jim", ((List)data.get("myArray")).get(1));
        reader.extractData("Hi {{myArray[1]}}", "Hi Jack", data);
        assertEquals("Jack", ((List)data.get("myArray")).get(1));

        // Try to setValue a value in an existing map
        Map myMap = new HashMap();
        myMap.put("foo", "bar");
        data.put("myMap", myMap);
        assertEquals("bar", myMap.get("foo"));
        reader.extractData("Hi {{myMap[\"foo\"]}}", "Hi Barry", data);
        assertEquals("Barry", myMap.get("foo"));

        assertEquals(null, data.get("theMap"));
        reader.extractData("Hi {{theMap[\"customer\"].name}}", "Hi Barry", data);
        assertEquals("Barry", ((Map)((Map)data.get("theMap")).get("customer")).get("name"));

        // Set a value to a non-existing indexed access should create a list, not a Map.
        assertEquals(null, data.get("theList"));
        reader.extractData("Hi {{theList[0].name}}", "Hi Barry", data);
        assertEquals("Barry", ((Map)((List)data.get("theList")).get(0)).get("name"));

        // Multi lists access should work too..
        data.remove("theList");
        assertEquals(null, data.get("theList"));
        reader.extractData("Hi {{theList[0][0][0].name}}", "Hi Barry", data);
        assertEquals("Barry", ((Map)((List)((List)((List)data.get("theList")).get(0)).get(0)).get(0)).get("name"));

    }

    @Test public void testExtractLoopData() {

        BaseJebtReader reader = new BaseJebtReader() {
            @Override public Map readData() {
                return null;
            }
        };

        Map data = new JSONObject();

        // Basic loop string values on array
        data.put("colors", new ArrayList());
        reader.extractData("Colors: {[colors|color]}{{color}},{[]}.", "Colors: red,green,blue,.", data);
        assertEquals("red", ((List)data.get("colors")).get(0));
        assertEquals("green", ((List)data.get("colors")).get(1));
        assertEquals("blue", ((List)data.get("colors")).get(2));

    }
}
