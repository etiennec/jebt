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

/**
 * These Tests will test all txt files in /test/resources/txt.
 *
 * It will fill the *txtTemplate.txt files with the json from *JSonData.json, and will compare results with *txtTemplateResult.txt.
 */
public class JebtTextWriterTest
{

    @Test
    public void testBasic() throws Exception {
            testTxtTemplate("basicTxtTemplate.txt", "basicTxtTemplateResult.txt", "basicJSonResult.json");
    }

    @Test
    public void testLoops() throws Exception {
        testTxtTemplate("loopsTxtTemplate.txt", "loopsTxtTemplateResult.txt", "loopsJSonResult.json");
    }

    @Test
    public void testLoopsNoEndText() throws Exception {
        testTxtTemplate("loopsNoEndTextTxtTemplate.txt", "loopsNoEndTextTxtTemplateResult.txt", "loopsJSonResult.json");
    }

    private void testTxtTemplate(String templateFile, String documentFile, String jsonDataFile) throws Exception {
        System.out.println("## Testing XLSX Writer template name " + templateFile);

        Map data = (Map)new JSONParser().parse(TestUtils.getFileReader("/txt/"+jsonDataFile), new ContainerFactory() {
            @Override public Map createObjectContainer() {
                return new LinkedHashMap();
            }

            @Override public List creatArrayContainer() {
                return new JSONArray();
            }
        });

        Reader templateReader = TestUtils.getFileReader("/txt/"+templateFile);

        StringWriter output = new StringWriter();

        JebtTextWriter writer = new JebtTextWriter(templateReader, output);

        writer.writeData(data);

        String actualResult = output.toString();

        StringBuilder expectedResult = new StringBuilder();

        Reader solutionReader = TestUtils.getFileReader("/txt/"+documentFile);
        int intValueOfChar;
        while ((intValueOfChar = solutionReader.read()) != -1) {
            expectedResult.append((char) intValueOfChar);
        }

        solutionReader.close();
        templateReader.close();

        assertEquals(expectedResult.toString(), actualResult);
    }




}
