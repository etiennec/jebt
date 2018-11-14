package lib.jebt.txt;

import lib.jebt.TestConstants;
import lib.jebt.TestUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import org.junit.Test;

import java.io.Reader;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * This Test will use txt files in /test/resources/txt.
 *
 * It will read the file *TxtTemplateResult.txt and will generate JSon based on template *TxtTemplate.txt.
 * JSon will be compared to *JSonResult.json.
 *
 * If no *JSonResult.json exists, we will just generate the data and fail if any error occurs during document generation.
 */
public class JebtTextReaderTest
{

    @Test
    /**
     * Basic templating : single expressions
     */
    public void testBasic() throws Exception
    {
            testTxtTemplate("basicTxtTemplate.txt", "basicTxtTemplateResult.txt", "basicJSonResult.json");
    }

    @Test
    /**
     * Single Loops, including loops on complex objects and no text to match at the end
     */
    public void testLoopsWithNoTextToMatchAtTheEnd() throws Exception
    {
        testTxtTemplate("loopsNoEndTextTxtTemplate.txt", "loopsNoEndTextTxtTemplateResult.txt", "loopsJSonResult.json");
    }

    @Test
    /**
     * Single Loops, including loops on complex objects
     */
    public void testLoops() throws Exception
    {
        testTxtTemplate("loopsTxtTemplate.txt", "loopsTxtTemplateResult.txt", "loopsJSonResult.json");
    }


    private void testTxtTemplate(String templateFile, String documentFile, String jsonResultFile) throws Exception {
        System.out.println("## Testing Reader template name "+templateFile);

        Reader templateReader = TestUtils.getFileReader("/txt/"+templateFile);

        Reader docReader = TestUtils.getFileReader("/txt/"+documentFile);

        JebtTextReader jr = new JebtTextReader(templateReader, docReader);
        JSONObject data = new JSONObject(jr.readData());

        Reader targetJSonReader;

        if (jsonResultFile != null) {
            targetJSonReader = TestUtils.getFileReader("/txt/"+jsonResultFile);
        } else {
            // No target JSon file, skipping comparison.
            return;
        }

        // Comparing JSons.
        JSONObject targetJSon = new JSONObject((Map)new JSONParser().parse(targetJSonReader, new ContainerFactory() {
            @Override public Map createObjectContainer() {
                return new LinkedHashMap();
            }

            @Override public List creatArrayContainer() {
                return new JSONArray();
            }
        }));

        assertEquals(targetJSon.toJSONString(), data.toJSONString());
    }
}
