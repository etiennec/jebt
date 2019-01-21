package lib.jebt.xlsx;

import lib.jebt.BaseJebtWriter;
import lib.jebt.JebtWriter;
import lib.jebt.parser.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link JebtWriter} for XLSX format.
 * It works by cloning the template workbook metadata only, and then reads from template cells one by one and insert them in output document with on-the-fly filling from data.
 */
public class JebtXlsxWriter extends BaseJebtWriter {

    boolean isWritten = false;

    private OutputStream docOS;

    private XSSFWorkbook templateWorkbook;

    private XSSFWorkbook documentBaseWorkbook;

    public JebtXlsxWriter(XSSFWorkbook templateWorkbook, XSSFWorkbook documentBaseWorkbook, OutputStream docOS) {
        this.docOS = docOS;
        this.templateWorkbook = templateWorkbook;
        this.documentBaseWorkbook = documentBaseWorkbook;
    }

    public void writeData(Map data) {

        if (isWritten) {
            throw new RuntimeException("Output XLSX file has already been generated");
        }
        isWritten = true;

        // We delete all cells from sheets, but keep the sheets as they may contain valuable metadata.
        for (Sheet sheet: documentBaseWorkbook) {
            for (int index = sheet.getLastRowNum(); index >= sheet.getFirstRowNum(); index--) {
                Row row = sheet.getRow(index);
                if (row != null) {
                    sheet.removeRow(sheet.getRow(index));
                }
            }
        }

        final SXSSFWorkbook sDocWorkbook = new SXSSFWorkbook(documentBaseWorkbook);

        // Fill Document sheet by sheet
        for (int i = 0; i < templateWorkbook.getNumberOfSheets(); i++) {
            XSSFSheet templateSheet = templateWorkbook.getSheetAt(i);
            documentBaseWorkbook.setSheetName(i, WorkbookUtil.createSafeSheetName(convertString(templateSheet.getSheetName(), data)));
            SXSSFSheet docSheet = sDocWorkbook.getSheetAt(i);

            fillDocSheetFromTemplate(templateSheet, docSheet, data);
        }

        try {
            sDocWorkbook.write(docOS);
        } catch (IOException e) {
            throw new RuntimeException("Error writing document Workbook", e);
        } finally {
            IOUtils.closeQuietly(docOS);
        }

    }

    /**
     * Core method that makes the job of filling Excel template.
     * It goes over cell events of the sourceSheet, and applies them to the target sheet in a streaming way.
     */
    private void fillDocSheetFromTemplate(XSSFSheet sourceSheet, SXSSFSheet targetSheet, Map data) {
        JebtXlsxTokenizer sheetTokenizer = new JebtXlsxTokenizer(sourceSheet);

        SheetContext targetSheetContext = new SheetContext(targetSheet);

        Token t;

        while ((t = sheetTokenizer.readNext()) != Token.EOD) {
            processToken(t, data, targetSheetContext);
        }

    }

    private void processToken(Token t, Map data, SheetContext targetSheetContext) {

        SXSSFSheet sheet = targetSheetContext.getSheet();

        // While filling Excel sheet, tokens are either NEW_TEXT_CELL, NEW_NON_TEXT_CELL, NEW_BLANK_CELL, NEW_BLANK_ROW or NEW_ROW (or LOOP).
        if (t.getType() == Token.TokenType.NEW_ROW || t.getType() == Token.TokenType.NEW_BLANK_ROW) {
            // We go to the next row, and create it if it doesn't already exist.
            targetSheetContext.rowId++;
            // We reinitialize Column Index too
            targetSheetContext.columnId = -1;
            SXSSFRow row = sheet.getRow(targetSheetContext.rowId);
            if (row == null) {
                row = sheet.createRow(targetSheetContext.rowId);
            }

        } else if (t.getType() == Token.TokenType.NEW_NON_TEXT_CELL || t.getType() == Token.TokenType.NEW_BLANK_CELL) {
            // We just copy the cell and its contents to the destination sheet.
            SXSSFCell cell = initCellCopy(t, targetSheetContext, data);

            if (t.getCell() == null) {
                // null cell means this was a Blank cell, it was already created in initCell and there's nothing to copy.
                return;
            }

            // Copying value
            switch (t.getCell().getCellTypeEnum()) {
                case _NONE:
                    break;
                case BLANK:
                    break;
                case STRING:
                    // A String cell here can only be blank, i.e. contain empty string.
                    cell.setCellValue("");
                    break;
                case BOOLEAN:
                    cell.setCellValue(t.getCell().getBooleanCellValue());
                    break;
                case NUMERIC:
                    cell.setCellValue(t.getCell().getNumericCellValue());
                    break;
                case FORMULA:
                    cell.setCellValue(t.getCell().getStringCellValue());
                    cell.setCellFormula(t.getCell().getCellFormula());
                    break;
                case ERROR:
                    break;
            }

        } else if (t.getType() == Token.TokenType.NEW_TEXT_CELL) {
            SXSSFCell cell = initCellCopy(t, targetSheetContext, data);
            processTextCell(t.getCell(), cell, data);
        } else if (t.getType() == Token.TokenType.LOOP) {
            JebtTextTokenizer.LoopToken loop = (JebtTextTokenizer.LoopToken)t;
            List collection = null;
            try {
                collection = (List)new JsonPathResolver(data).evaluatePathToObject(loop.getCollectionJsonPath());
            } catch (ClassCastException e) {
                throw new JebtEvaluationException("Object found at " + loop.getCollectionJsonPath() + " is not a List",
                        e);
            }

            if (collection == null) {
                // The list is empty or non-existent, so we have nothing to iterate.
                return;
            }
            Iterator it = collection.iterator();

            boolean first = true;

            targetSheetContext.loopDepth++;

            while (it.hasNext()) {

                if (targetSheetContext.loopDepth <= 1) {
                    // We must go to the next row before every new record for the main Loop
                    if (first) {
                        first = false;
                    } else {
                        processToken(new Token(Token.TokenType.NEW_ROW, null), data, targetSheetContext);
                    }
                }
                Object obj = it.next();

                Map dataCopy = new LinkedHashMap(data);
                dataCopy.put(loop.getLoopItemName(), obj);

                for (Token tok : loop.getLoopTokens()) {
                    processToken(tok, dataCopy, targetSheetContext);
                }
            }

            targetSheetContext.loopDepth--;
        } else {
            throw new RuntimeException("Unknown Token: " + t.getType());
        }
    }

    private SXSSFCell initCellCopy(Token t, SheetContext targetSheetContext, Map data) {
        targetSheetContext.columnId++;
        SXSSFRow row = targetSheetContext.getSheet().getRow(targetSheetContext.rowId);
        SXSSFCell cell = row.getCell(targetSheetContext.columnId);
        if (cell == null) {
            cell = row.createCell(targetSheetContext.columnId);
        }
        if (t.getCell() == null) {
            cell.setCellType(CellType.BLANK);
        } else {
            cell.setCellType(t.getCell().getCellTypeEnum());
            // Not sure if this call will work since styles are coming from different workbooks...
            cell.setCellStyle(t.getCell().getCellStyle());
        }

        // Remove any loop tag from comments & Resolve any expression in it.
        if (cell.getCellComment() != null && cell.getCellComment().getString() != null && !StringUtils
                .isBlank(cell.getCellComment().getString().toString())) {
            String commentStr = cell.getCellComment().getString().toString();
            // Removing loop tags
            while (commentStr.indexOf("{[") >= 0 && commentStr.indexOf("]}", commentStr.indexOf("{[")) >= 0) {
                commentStr = commentStr.substring(0, commentStr.indexOf("{[")) + commentStr
                        .substring(commentStr.indexOf("]}", commentStr.indexOf("{[")) + 2);
            }
            cell.getCellComment().setString(new XSSFRichTextString(convertString(commentStr, data)));
        }

        return cell;
    }

    /**
     * Copies cell contents from source to target, and resolves any token in cell's text in the process.
     * SXSSF doesn't support RichTextString, so we'll discard any text-part specific font, and will rely on Cell style only for formatting.
     */
    private void processTextCell(Cell sourceCell, SXSSFCell targetCell, Map data) {
        String sourceStr = sourceCell.getRichStringCellValue().getString();
        String targetStr = escapeExcelInjection(sourceStr, convertString(sourceStr, data));

        targetCell.setCellValue(targetStr);
    }

    /**
     * An excel injection can occur if a user passes a string with a malicious content, such as
     * =SUM(1+1)*cmd|' /C calc'!A0
     * When we detect this, we escape the string with a single quote.
     * We don't exepect anyone to pass to such a formula in a normal text label, so that protection will suffice for now.
     * Later we might want to consider using Apache POI setQuotePrefixed(boolean) on CellStyle.
     */
    private String escapeExcelInjection(String sourceStr, String evaluatedStr) {
        if (sourceStr == null || evaluatedStr == null) {
            // Should never happen.
            return evaluatedStr;
        }

        if (sourceStr.equals(evaluatedStr)) {
            // Nothing was changed
            return evaluatedStr;
        }

        if (sourceStr.startsWith("{{") && !"".equals(evaluatedStr) && "=-+@".indexOf(evaluatedStr.charAt(0)) >= 0) {
            // Escaping any generated value starting with =-+@ with a starting quote.
            return "'" + evaluatedStr;
        }

        return evaluatedStr;

    }

    private class SheetContext {

        public int rowId = -1;

        public int columnId = -1;

        public XSSFRow currentRow;

        public int loopDepth = 0;

        private SXSSFSheet sheet;

        public SheetContext(SXSSFSheet sheet) {
            this.sheet = sheet;
        }

        public SXSSFSheet getSheet() {
            return sheet;
        }
    }
}
