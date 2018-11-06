package lib.jebt;

import org.json.simple.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SbitTextWriterProcessorTest
{

    @Test
    public void testConvertString()
    {
        BaseJebtWriter baseWriter = new BaseJebtWriter() {
            public void writeData(Map data) {

            }
        };

        JSONObject data = new JSONObject();

        data.put("foo", "bar");

        // Checking invariant Strings with tricky escaped blocks
        assertEquals(baseWriter.convertString("Hello", data), "Hello");
        assertEquals(baseWriter.convertString("Hel{lo", data), "Hel{lo");
        assertEquals(baseWriter.convertString("Hel{}}lo", data), "Hel{}}lo");
        assertEquals(baseWriter.convertString("Hel{{}}lo", data), "Hello");
        assertEquals(baseWriter.convertString("Hello {{foo}}", data), "Hello bar");
        assertEquals(baseWriter.convertString("{{foo}} Hello", data), "bar Hello");
        assertEquals(baseWriter.convertString("Hello {{foo}} world", data), "Hello bar world");
        assertEquals(baseWriter.convertString("Hello\\", data), "Hello\\");
        assertEquals(baseWriter.convertString("Hello\\{", data), "Hello\\{");
        assertEquals(baseWriter.convertString("Hello\\{{", data), "Hello{{");
        assertEquals(baseWriter.convertString("Hello\\\\", data), "Hello\\\\");
        assertEquals(baseWriter.convertString("Hello\\{{World", data), "Hello{{World"); // Escaped
        assertEquals(baseWriter.convertString("Hello\\\\{{World", data), "Hello\\{{World"); // Escaped with leading \
        assertEquals(baseWriter.convertString("Hello\\{\\{World", data), "Hello\\{\\{World"); // Not really escaped
        assertEquals(baseWriter.convertString("Hello\\{\\{{World", data), "Hello\\{{{World"); // Only second one is escaped
    }

    @Test
    public void testExtractData() {
        BaseJebtReader baseReader = new BaseJebtReader() {

            @Override
            public Map readData() {
                return null;
            }
        };

        JSONObject data = new JSONObject();

        // Should do nothing
        baseReader.extractData(" text ", " text ", data);

        baseReader.extractData(" text ", " text mismatch at the end is not a problem and will be ignored ", data);

        try {
            baseReader.extractData(" text ", " te - mistmatch in the middle is a problem - xt  ", data);
            fail("An exception should occur if template and document text don't match");
        } catch (RuntimeException e) {
            // Success !
        }

        // TODO make following tests relevant


        baseReader.extractData("{{myText}} World", "Hello World", data);
        baseReader.extractData("Hello {{myText}} World", "Hello dear World", data);
        baseReader.extractData("Hello {{myText}}", "Hello World", data);
        baseReader.extractData("{{myText}}", "Hello World", data);
        baseReader.extractData("{{myText1}} {{myText2}} {{myText3}}", "Hello dear World", data);
        baseReader.extractData("_{{myText1}} {{myText2}} {{myText3}}_", "_Hello dear World_", data);
    }
}
