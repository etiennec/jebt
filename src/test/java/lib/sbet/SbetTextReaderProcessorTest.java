package lib.sbet;

import lib.sbet.parser.SbetReaderTextProcessor;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SbetTextReaderProcessorTest
{

    @Test
    public void testExtractData()
    {
        BaseSbetReader reader = new BaseSbetReader() {
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
    }
}
