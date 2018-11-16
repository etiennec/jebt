package lib.jebt.xlsx;

import com.monitorjbl.xlsx.StreamingReader;
import lib.jebt.BaseJebtReader;
import lib.jebt.parser.JebtXlsxTokenizer;
import lib.jebt.parser.Token;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementation of {@link JebtXlsxReader} for XLSX Excel format.
 * The template XLSX should be small enough fit in memory so it's passed as POI workbook.
 * The document XLSX can be huge and is passed as an Input Stream so that we can parse it with excel-stream-reader to be memory-efficient and avoid OOME.
 * Calling readData() will NOT close the InputStream at the end, invoking code should do it.
 */
public class JebtXlsxReader extends BaseJebtReader {
    private InputStream documentIS;

    private XSSFWorkbook templateWorkbook;

    private boolean skipReadNextTemplateToken;

    private Token templateToken;

    private int rowIndex, columnIndex = -1;

    public JebtXlsxReader(XSSFWorkbook templateWorkbook, InputStream documentIS) {
        this.templateWorkbook = templateWorkbook;
        this.documentIS = documentIS;
    }

    public Map readData() {
        Workbook doc = StreamingReader.builder().rowCacheSize(1)    // number of rows to keep in memory (defaults to 10)
                .bufferSize(4096)     // buffer size to use when reading InputStream to file (defaults to 1024)
                .open(documentIS);

        Map data = new LinkedHashMap();

        int i = 0;
        for (Sheet docSheet : doc) {
            if (templateWorkbook.getNumberOfSheets() < i + 1) {
                break;
            }

            XSSFSheet templateSheet = templateWorkbook.getSheetAt(i);

            JebtXlsxTokenizer tokenizer = new JebtXlsxTokenizer(templateSheet);

            extractData(tokenizer, new SheetReader(docSheet), data);

            ++i;
        }

        return data;

    }

    private void extractData(JebtXlsxTokenizer templateTokenizer, SheetReader docSheetReader, Map data) {
        skipReadNextTemplateToken = false;

        while ((skipReadNextTemplateToken && templateToken != Token.EOD)
                || (templateToken = templateTokenizer.readNext()) != Token.EOD) {
            boolean shouldBreak = processSingleToken(templateTokenizer, docSheetReader, data);
            if (shouldBreak || templateToken == Token.EOD) {
                break;
            }
        }

    }

    private boolean processSingleToken(JebtXlsxTokenizer templateTokenizer, SheetReader docSheetReader, Map data) {

        Token docToken;

        switch (templateToken.getType()) {
            case END_OF_DOCUMENT:
                // We reached the end of the template, so we stop even if there's extra data in the document Sheet. It'll be ignored.
                return true;
            case NEW_ROW:
                ++rowIndex;
                columnIndex = -1;
                // Should have same thing in document.
                docToken = docSheetReader.readNext();
                if (docToken.getType() != Token.TokenType.NEW_ROW) {
                    throw new RuntimeException("Expected a new Row in the document but found a " + docToken.getType());
                }
                return false;
            case NEW_NON_TEXT_CELL:
                ++columnIndex;
                // Should have same cell in document as non-text cell cannot contain expressions
                docToken = docSheetReader.readNext();
                if (docToken.getType() != Token.TokenType.NEW_NON_TEXT_CELL) {
                    throwError(
                            "Expected a non-text cell in the document but found a " + docToken.getType());
                }
                // We should also check that the contents are identical, but to be honest, we don't really care...
                return false;
            case NEW_TEXT_CELL:
                ++columnIndex;
                // Contents of the cells are considered as text and evaluated accordingly.
                docToken = docSheetReader.readNext();
                String  docStr  = "";
                if (docToken.getCell() != null) {
                    docStr = getCellValueAsString(docToken.getCell());
                }
                extractData(templateToken.getCell().getStringCellValue(), docStr, data);
                return false;
            case LOOP:
                // TODO Implement loops
                throw new UnsupportedOperationException();
            default:
                throw new UnsupportedOperationException("Do not expect token "+templateToken.getType()+" while parsing Excel template");
        }
    }

    private void throwError(String errorMessage) {
        errorMessage = errorMessage + " / RowIndex:"+rowIndex+":ColumnIndex:"+columnIndex;
        throw new RuntimeException(errorMessage);
    }

    private String getCellValueAsString(Cell cell) {
        String strCellValue = "";
        if (cell != null) {
            if (cell.getCellTypeEnum() == CellType.STRING) {
                strCellValue = cell.getStringCellValue();
            } else if (cell.getCellTypeEnum() == CellType.NUMERIC) {
                if (DateUtil.isCellDateFormatted(cell)) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
                    strCellValue = dateFormat.format(cell.getDateCellValue());
                } else {
                    Double value = cell.getNumericCellValue();
                    strCellValue = value.toString();
                }
            } else if (cell.getCellTypeEnum() == CellType.BOOLEAN) {
                strCellValue = new Boolean(cell.getBooleanCellValue()).toString();
            } else if (cell.getCellTypeEnum() == CellType.BLANK) {
                strCellValue = "";
            } else if (cell.getCellTypeEnum() == CellType.FORMULA) {
                strCellValue = cell.getCellFormula();
            }
        }

        return strCellValue;
    }
}
