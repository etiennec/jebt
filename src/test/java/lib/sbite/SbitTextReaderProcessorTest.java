package lib.sbite;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SbitTextReaderProcessorTest
{

    @Test
    public void testExtractData()
    {
        BaseSbitReader reader = new BaseSbitReader() {
            @Override
            public Map<String, Object> readData() {
                return null;
            }
        };

        Map<String, Object> data = new HashMap<String, Object>();

        // Templates with no bean elements shouldn't set anything.
        reader.extractData("", "", data);
        reader.extractData("Hello", "Hello", data);

        assertTrue(data.isEmpty());

        // Setting basic String values in the map

        reader.extractData("Hello {{foo}}!", "Hello bar!", data);
        assertEquals("bar", data.get("foo"));

        // Try to set an Integer as map value
        reader.setClass("myNumber", Double.class);
        reader.extractData("This {{myNumber}} is delicious!", "This 3.1416 is delicious!", data);
        assertEquals(new Double(3.1416d), data.get("myNumber"));

        // Try to set a value in an existing array
        data.put("myArray", new String[] {"Hello", "Jim"});
        assertEquals("Jim", ((String[])data.get("myArray"))[1]);
        reader.extractData("Hi {{myArray[1]}}", "Hi Jack", data);
        assertEquals("Jack", ((String[])data.get("myArray"))[1]);

        // Try to set a value in an existing map
        Map myMap = new HashMap();
        myMap.put("foo", "bar");
        data.put("myMap", myMap);
        assertEquals("bar", myMap.get("foo"));
        reader.extractData("Hi {{myMap(\"foo\")}}", "Hi Barry", data);
        assertEquals("Barry",  myMap.get("foo"));

        // Nested Instantiation & Factories
        reader.setClass("theMap", java.util.HashMap.class);
        reader.setFactory("theMap(\"customer\")", new Factory() {
                    @Override
                    public Object createObject() {
                        SbitTextReaderProcessorTest parentClass = new SbitTextReaderProcessorTest();
                        Customer c = parentClass.new Customer();
                        return c;
                    }
                });
        assertEquals(null, data.get("theMap"));
        reader.extractData("Hi {{theMap(\"customer\").name}}", "Hi Barry", data);
        assertEquals("Barry",  ((Customer)((Map)data.get("theMap")).get("customer")).name);
    }

    public class Customer {

        public Customer() {};

        public Customer(String name) {
            this.name = name;
        }

        public String name = "test";
        public Address address = new Address();
    }

    public class Address {
        public Map<String, String> fields = new HashMap<String, String>();
    }
}
