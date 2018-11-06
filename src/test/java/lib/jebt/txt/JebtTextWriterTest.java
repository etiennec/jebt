package lib.jebt.txt;

import lib.jebt.TestConstants;
import lib.jebt.TestUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;

import java.io.*;
import java.util.*;

import static org.junit.Assert.assertEquals;

public class JebtTextWriterTest extends BaseTextTest implements TestConstants
{

    @Test
    /**
     * This Test will automatically test all txt files in /test/resources/txt.
     *
     * It will fill the *txtTemplate.txt files with the json from *JSonData.json, and will compare results with *txtTemplateResult.txt.
     * If no JSon file exists, it will use defaultJSonData.json.
     */
    public void testWriteAllTxt() throws Exception {

        for (String templateName : getAllTestTemplateNames()) {
            testTxtTemplate(templateName);
        }

    }

    private void testTxtTemplate(String templateName) throws Exception {
        System.out.println("## Testing Writer template name "+templateName);

        Map data = (Map)new JSONParser().parse(getJSONDataFileReader(templateName), new ContainerFactory() {
            @Override public Map createObjectContainer() {
                return new LinkedHashMap();
            }

            @Override public List creatArrayContainer() {
                return new JSONArray();
            }
        });

        Reader templateReader = TestUtils.getFileReader("/txt/"+templateName+TXT_TEMPLATE_FILE_SUFFIX);

        StringWriter output = new StringWriter();

        JebtTextWriter writer = new JebtTextWriter(templateReader, output);

        writer.writeData(data);

        String actualResult = output.toString();

        StringBuilder expectedResult = new StringBuilder();

        Reader solutionReader = TestUtils.getFileReader("/txt/"+templateName+TXT_RESULT_TEMPLATE_FILE_SUFFIX);
        int intValueOfChar;
        while ((intValueOfChar = solutionReader.read()) != -1) {
            expectedResult.append((char) intValueOfChar);
        }

        solutionReader.close();
        templateReader.close();

        assertEquals(expectedResult.toString(), actualResult);
    }

    private Reader getJSONDataFileReader(String templateName) {

        File f = new File("/txt/"+templateName+JSON_DATA_FILE_SUFFIX);

        if (!f.exists()) {
            templateName = "default";
        }

        return TestUtils.getFileReader("/txt/"+templateName+JSON_DATA_FILE_SUFFIX);
    }


}
