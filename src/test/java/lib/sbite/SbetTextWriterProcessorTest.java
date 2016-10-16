package lib.sbite;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SbetTextWriterProcessorTest
{

    @Test
    public void testConvertString()
    {
        BaseSbetWriter baseWriter = new BaseSbetWriter() {
            public void writeData(Map<String, Object> data) {

            }
        };

        Map<String, Object> data = new HashMap<String, Object>();

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
        BaseSbetReader baseReader = new BaseSbetReader() {

            @Override
            public Map<String, Object> readData() {
                return null;
            }
        };

        Map<String, Object> data = new HashMap<String, Object>();

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
