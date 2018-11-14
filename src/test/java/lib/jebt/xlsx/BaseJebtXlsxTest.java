package lib.jebt.xlsx;

import lib.jebt.TestUtils;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;

public class BaseJebtXlsxTest {

    protected XSSFWorkbook getWorkbook(String resourcePath) {

        InputStream is = TestUtils.getInputStream(resourcePath);

        try {
            XSSFWorkbook workbook = (XSSFWorkbook)WorkbookFactory.create(is);
            return workbook;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
}
