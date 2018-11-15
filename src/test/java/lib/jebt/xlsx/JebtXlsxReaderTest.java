package lib.jebt.xlsx;

import lib.jebt.TestUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import org.junit.Test;

import java.io.InputStream;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * This Test will use txt files in /test/resources/txt.
 *
 * It will read the file *XlsxTemplateResult.xlsx and will generate JSon based on template *XlsxTemplate.xlsx.
 * JSon will be compared to *JSonResult.json.
 *
 * If no *JSonResult.json exists, we will just generate the data and fail if any error occurs during document generation.
 */
public class JebtXlsxReaderTest extends BaseJebtXlsxTest
{

    @Test
    /**
     * Basic templating : single expressions
     */
    public void testBasic() throws Exception
    {
        testXlsxReader("basicXlsxTemplate.xlsx", "basicXlsxTemplateResult.xlsx", "basicJSonData.json");
    }

    @Test
    /**
     * Loops, including loops on complex objects
     */
    public void testLoops() throws Exception
    {
        testXlsxReader("loopXlsxTemplate.xlsx", "loopXlsxTemplateResult.xlsx", "loopJSonData.json");
    }


    private void testXlsxReader(String templateFile, String documentFile, String jsonResultFile) throws Exception {
        System.out.println("## Testing Reader template name "+templateFile);



        InputStream docIS = TestUtils.getInputStream("/xlsx/"+documentFile);

        XSSFWorkbook template = getXSSFWorkbook("/xlsx/"+templateFile);

        JebtXlsxReader jr = new JebtXlsxReader(template, docIS);
        JSONObject data = new JSONObject(jr.readData());

        IOUtils.closeQuietly(docIS);

        Reader targetJSonReader;

        if (jsonResultFile != null) {
            targetJSonReader = TestUtils.getFileReader("/xlsx/"+jsonResultFile);
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
