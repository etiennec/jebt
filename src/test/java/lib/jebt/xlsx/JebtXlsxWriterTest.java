package lib.jebt.xlsx;

import com.monitorjbl.xlsx.StreamingReader;
import lib.jebt.TestUtils;
import lib.jebt.txt.JebtTextWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.simple.JSONArray;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests in this class fill an Excel Template with some JSON data, and compare generated Excel document with a target result document.
 */
public class JebtXlsxWriterTest extends BaseJebtXlsxTest
{

    @Test
    public void testBasicXlsx() throws Exception {
        testTxtTemplate("basicXlsxTemplate.xlsx", "basicXlsxTemplateResult.xlsx", "basicJSonData.json");
    }

    @Test
    /**
     * Loops in Excel cells
     */
    public void testLoopXlsx() throws Exception {
        testTxtTemplate("loopXlsxTemplate.xlsx", "loopXlsxTemplateResult.xlsx", "loopJSonData.json");
    }

    private void testTxtTemplate(String templateFile, String documentFile, String jsonDataFile) throws Exception {
        System.out.println("## Testing Writer template name "+templateFile);

        Map data = (Map)new JSONParser().parse(TestUtils.getFileReader("/xlsx/"+jsonDataFile), new ContainerFactory() {
            @Override public Map createObjectContainer() {
                return new LinkedHashMap();
            }

            @Override public List creatArrayContainer() {
                return new JSONArray();
            }
        });

        XSSFWorkbook wb = getWorkbook("/xlsx/"+templateFile);

        XSSFWorkbook wbCopy = getWorkbook("/xlsx/"+templateFile);

        OutputStream docOS = TestUtils.getOutputStream("/xlsx/output/out_"+documentFile);

        JebtXlsxWriter writer = new JebtXlsxWriter(wb, wbCopy, docOS);

        writer.writeData(data);

        IOUtils.closeQuietly(docOS);

        // Compare generated document with reference document
        compareXlsxFiles(TestUtils.getInputStream("/xlsx/"+documentFile), TestUtils.getInputStream("/xlsx/output/out_"+documentFile));
    }

    private void compareXlsxFiles(InputStream is1, InputStream is2) {

        Workbook w1 = StreamingReader.builder()
                .rowCacheSize(1)    // number of rows to keep in memory (defaults to 10)
                .bufferSize(4096)     // buffer size to use when reading InputStream to file (defaults to 1024)
                .open(is1);

        Workbook w2 = StreamingReader.builder()
                .rowCacheSize(1)    // number of rows to keep in memory (defaults to 10)
                .bufferSize(4096)     // buffer size to use when reading InputStream to file (defaults to 1024)
                .open(is2);

        Iterator<Sheet> s1 = w1.sheetIterator();
        Iterator<Sheet> s2 = w2.sheetIterator();

        while (s1.hasNext() && s2.hasNext()) {
            Sheet sh1 = s1.next();
            Sheet sh2 = s2.next();
            System.out.println("Checking sheet "+sh1.getSheetName());
            assertEquals(sh1.getSheetName(), sh2.getSheetName());

            Iterator<Row> r1 = sh1.rowIterator();
            Iterator<Row> r2 = sh2.rowIterator();

            while (r1.hasNext() && r2.hasNext()) {
                Row row1 = r1.next();
                Row row2 = r2.next();

                // First, making sure we're looking at the same row.
                while (row1.getRowNum() != row2.getRowNum()) {

                    if (row1.getRowNum() < row2.getRowNum()) {
                        assertRowIsEmpty(row1);
                        if (!r1.hasNext()) {
                            break;
                        }
                        row1 = r1.next();
                    } else {
                        assertRowIsEmpty(row2);
                        if (!r2.hasNext()) {
                            break;
                        }
                        row2 = r2.next();
                    }
                }

                if (row1.getRowNum() != row2.getRowNum()) {
                    // We're at the end of the document so we must make sure we're looking at an empty line.
                    assertRowIsEmpty(row1.getRowNum() < row2.getRowNum() ? row2 : row1);
                }

                compareRow(row1, row2);
            }

        }

        if (s1.hasNext()) {
            fail("First workbook has more sheets than second one");
        }
        if (s2.hasNext()) {
            fail("Second workbook has more sheets than first one");
        }


        IOUtils.closeQuietly(is1);
        IOUtils.closeQuietly(is2);
    }

    private void assertRowIsEmpty(Row row) {
        if (row == null) {
            return;
        }

        for (Cell c : row) {
            if (c.getCellTypeEnum() == CellType.BLANK) {
                return;
            } else if (c.getCellTypeEnum() == CellType.STRING) {
                assertTrue(StringUtils.isBlank(c.getStringCellValue()));
            } else {
                fail("Cell is not empty while it should be on an empty line.");
            }
        }

    }

    private void compareRow(Row row1, Row row2) {


        if (row1 == null && row2 == null) {
            return;
        }

        System.out.println("Checking row at index " + (row1 != null ? row1.getRowNum() : row2.getRowNum()));

        if (row1 == null) {
            for (Cell c : row2) {
                compareCell(null, c);
            }
        }

        if (row2 == null) {
            for (Cell c : row1) {
                compareCell(null, c);
            }
        }

        for (int i = 0 ; i <= (Math.max(row1.getLastCellNum(), row2.getLastCellNum())) ; i++) {
            Cell c1 = row1.getCell(i);
            Cell c2 = row2.getCell(i);

            System.out.println("Checking Cell at column index "+i);

            compareCell(c1, c2);
        }
    }

    private void compareCell(Cell c1, Cell c2) {
        if (c1 == null && c2 == null) {
            return;
        }

        if (c1 == null) {
            if (c2.getCellTypeEnum() == CellType.BLANK) {
                return;
            } else {
                fail("c1 is null but not c2");
            }
        }

        if (c2 == null) {
            if (c1.getCellTypeEnum() == CellType.BLANK) {
                return;
            } else {
                if (c1.getCellTypeEnum() == CellType.STRING) {
                    assertTrue(StringUtils.isBlank(c1.getStringCellValue()));
                } else {
                    fail("Cell c2 is not Blank or Empty");
                }
            }
        }

        if (c1 == null) {
            if (c2.getCellTypeEnum() == CellType.BLANK) {
                return;
            } else {
                if (c2.getCellTypeEnum() == CellType.STRING) {
                    assertTrue(StringUtils.isBlank(c2.getStringCellValue()));
                } else {
                    fail("Cell c1 is not Blank or Empty");
                }
            }
        }

        assertEquals(c1.getCellTypeEnum(), c2.getCellTypeEnum());

        switch(c1.getCellTypeEnum()) {
            case BLANK:
                break; // Other cell is also blank
            case ERROR:
                break; // No idea how to compare that
            case FORMULA:
                assertEquals(c1.getCellFormula(), c2.getCellFormula());
                break;
            case BOOLEAN:
                assertEquals(c1.getBooleanCellValue(), c2.getBooleanCellValue());
                break;
            case NUMERIC:
                assertEquals(c1.getNumericCellValue(), c2.getNumericCellValue(), 0.00001d);
                break;
            case STRING:
                assertEquals(c1.getStringCellValue(), c2.getStringCellValue());
                break;
        }

        // Not sure this will work fine...
        compareStyle((XSSFCellStyle)c1.getCellStyle(), (XSSFCellStyle)c2.getCellStyle());
    }

    private void compareStyle(XSSFCellStyle s1, XSSFCellStyle s2) {
        assertEquals(s1.getFont().getBold(), s2.getFont().getBold());
        assertEquals(s1.getFont().getItalic(), s2.getFont().getItalic());
        assertEquals(s1.getFont().getUnderline(), s2.getFont().getUnderline());
        assertEquals(s1.getFont().getFontName(), s2.getFont().getFontName());
        assertEquals(s1.getFont().getFontHeight(), s2.getFont().getFontHeight());
        assertEquals(s1.getFillBackgroundColor(), s2.getFillBackgroundColor());
        assertEquals(s1.getFillForegroundColor(), s2.getFillForegroundColor());
    }
}
