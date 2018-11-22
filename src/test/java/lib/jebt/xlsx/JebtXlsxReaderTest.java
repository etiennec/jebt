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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

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
     * JSON Array comparison
     */
    public void testJSONEquals() throws Exception
    {
        JSONParser parser = new JSONParser();
        JSONObject obj1 = (JSONObject)parser.parse("{\"stringName\":\"World\",\"customers\":[null,null,{\"address\":{\"fields\":{\"postalCode\":200093}}}]}");
        JSONObject obj2 = (JSONObject)parser.parse("{\"stringName\":\"World\",\"customers\":[null,null,{\"address\":{\"fields\":{\"postalCode\":200093}}}]}");
        assertEquals(obj1, obj2);
    }

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
     * Simple Loop only
     */
    public void testSimpleLoop() throws Exception
    {
        testXlsxReader("simpleLoopXlsxTemplate.xlsx", "simpleLoopXlsxTemplateResult.xlsx", "simpleLoopJSonData.json");
    }


    @Test
    /**
     * Loops, including nested loops & loops on complex objects
     */
    public void testInnerLoops() throws Exception
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
                return new ArrayList();
            }
        }));

        compareJSON(targetJSon, data);
    }

    // Comparing directly JSONObjects works but doesn't give enough information when something fails like comparing an Integer with a Long.
    // By doing comparison manually here it's simpler to pinpoint the problem when comparison fails.
    private void compareJSON(JSONObject j1, JSONObject j2) {
        compareMap(j1, j2);
        compareMap(j2, j1);

    }

    private void compareMap(Map j1, Map j2) {
        if (j1 == null) {
            assertNull(j2);
            return;
        }else {
            assertNotNull(j2);
        }

        for (Object key : j1.keySet()) {
            compareObject(j1.get(key), j2.get(key));
        }

        // All match!
    }

    private void compareList(List l1, List l2) {
        if (l1 == null) {
            assertNull(l2);
            return;
        } else {
            assertNotNull(l2);
        }

        assertEquals(l1.size(), l2.size());

        for (int i = 0 ; i < l1.size() ; i++) {
            compareObject(l1.get(i), l2.get(i));
        }
    }

    private void compareObject(Object o1, Object o2) {
        if (o1 == null) {
            assertNull(o2);
            return;
        }else {
            assertNotNull(o2);
        }

        if (o1 instanceof Map) {
            assertTrue(o2 instanceof Map);
            compareMap((Map)o1, (Map)o2);
            return;
        }

        if (o1 instanceof List) {
            assertTrue(o2 instanceof List);
            compareList((List)o1, (List)o2);
            return;
        }

        assertEquals(o1, o2);
    }
}
