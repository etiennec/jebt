package lib.jebt.xlsx;

import com.monitorjbl.xlsx.StreamingReader;
import lib.jebt.TestUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;

public class BaseJebtXlsxTest {

    protected XSSFWorkbook getXSSFWorkbook(String resourcePath) {

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

    protected Workbook getStreamingWorkbook(InputStream fileIS) {
        Workbook workbook = StreamingReader.builder()
                .rowCacheSize(1)    // number of rows to keep in memory (defaults to 10)
                .bufferSize(4096)     // buffer size to use when reading InputStream to file (defaults to 1024)
                .open(fileIS);

        return workbook;

    }
}
