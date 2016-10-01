package lib.sbet;

import lib.sbet.txt.TxtSbetWriter;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class SbetParserTest
{

    @Test
    public void testParser()
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
}
