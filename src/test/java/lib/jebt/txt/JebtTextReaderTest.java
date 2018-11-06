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

public class JebtTextReaderTest extends BaseTextTest implements TestConstants
{

    @Test
    /**
     * This Test will automatically test all txt files in /test/resources/txt.
     *
     * It will read the file *TxtTemplateResult.txt and will generate JSon based on template *TxtTemplate.txt.
     * JSon will be compared to *JSonResult.json.
     *
     * If no *JSonResult.json exists, we will just generate the data and fail if any error occurs during document generation.
     */
    public void testRead() throws Exception
    {
        for (String templateName : getAllTestTemplateNames()) {
            testTxtTemplate(templateName);
        }
    }

    private void testTxtTemplate(String templateName) throws Exception {
        System.out.println("## Testing Reader template name "+templateName);

        Reader templateReader = TestUtils.getFileReader("/txt/"+templateName+TXT_TEMPLATE_FILE_SUFFIX);

        Reader docReader = TestUtils.getFileReader("/txt/"+templateName+TXT_RESULT_TEMPLATE_FILE_SUFFIX);

        JebtTextReader jr = new JebtTextReader(templateReader, docReader);
        JSONObject data = new JSONObject(jr.readData());

        Reader targetJSonReader;

        try {
            targetJSonReader = TestUtils.getFileReader("/txt/"+templateName+JSON_RESULT_FILE_SUFFIX);
        } catch (RuntimeException e) {
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
