package lib.jebt.xlsx;

import com.monitorjbl.xlsx.StreamingReader;
import lib.jebt.BaseJebtReader;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.util.Map;

/**
 Implementation of {@link JebtXlsxReader} for XLSX Excel format.

 The template XLSX should be small enough fit in memory so it's passed as POI workbook.
 The document XLSX can be huge and is passed as an Input Stream so that we can parse it with excel-stream-reader to be memory-efficient and avoid OOME.

 Calling readData() will NOT close the InputStream at the end, invoking code should do it.

 */
public class JebtXlsxReader extends BaseJebtReader
{
    private InputStream documentIS;
    private XSSFWorkbook templateWorkbook;

    public JebtXlsxReader(XSSFWorkbook templateWorkbook, InputStream documentIS) {
        this.templateWorkbook = templateWorkbook;
        this.documentIS = documentIS;
    }

    public Map readData() {
        Workbook doc = StreamingReader.builder()
                .rowCacheSize(1)    // number of rows to keep in memory (defaults to 10)
                .bufferSize(4096)     // buffer size to use when reading InputStream to file (defaults to 1024)
                .open(documentIS);

        // TODO
        return null;

    }

}
