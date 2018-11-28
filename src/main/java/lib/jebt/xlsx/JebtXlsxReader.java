package lib.jebt.xlsx;

import com.monitorjbl.xlsx.StreamingReader;
import lib.jebt.BaseJebtReader;
import lib.jebt.parser.JebtTextTokenizer;
import lib.jebt.parser.JebtXlsxTokenizer;
import lib.jebt.parser.ParsingUtils;
import lib.jebt.parser.Token;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

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

    private int loopDepth = 0;

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

            // We must not forget to read from Sheet name as it can also contain tokens.
            try {
                extractData(templateSheet.getSheetName(), docSheet.getSheetName(), data);
            } catch (Exception e) {
                // But if end users modified sheet name and broke matching, we don't care too much.
            }

            extractData(tokenizer, new SheetReader(docSheet), data);

            ++i;
        }

        return data;

    }

    private void extractData(JebtXlsxTokenizer templateTokenizer, SheetReader docSheetReader, Map data) {
        skipReadNextTemplateToken = false;

        Token templateToken = null;

        while ((skipReadNextTemplateToken && templateToken != Token.EOD)
                || (templateToken = templateTokenizer.readNext()) != Token.EOD) {
            boolean shouldBreak = processSingleToken(templateToken, docSheetReader, data);
            if (shouldBreak || templateToken == Token.EOD) {
                break;
            }
        }

    }

    private boolean processSingleToken(Token templateToken, SheetReader docSheetReader, Map data) {

        Token docToken;

        switch (templateToken.getType()) {
            case END_OF_DOCUMENT:
                // We reached the end of the template, so we stop even if there's extra data in the document Sheet. It'll be ignored.
                return true;
            case NEW_ROW:
            case NEW_BLANK_ROW:
                ++rowIndex;
                columnIndex = -1;
                // Should have same thing in document.
                docToken = docSheetReader.readNext();
                if (docToken.getType() != templateToken.getType()) {
                    throwError("Expected a New [BLANK] Row in the document but found a " + docToken.getType());
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
            case NEW_BLANK_CELL:
                ++columnIndex;
                // Should also have a blank cell in document
                docToken = docSheetReader.readNext();
                if (docToken.getType() != Token.TokenType.NEW_BLANK_CELL) {
                    throwError(
                            "Expected a BLANK cell in the document but found a " + docToken.getType());
                }
                // We should also check that the contents are identical, but to be honest, we don't really care...
                return false;
            case NEW_TEXT_CELL:
                ++columnIndex;
                // Contents of the cells are considered as text and evaluated accordingly.
                docToken = docSheetReader.readNext();

                if (docToken.getType() != Token.TokenType.NEW_TEXT_CELL && docToken.getType() != Token.TokenType.NEW_NON_TEXT_CELL && docToken.getType() != Token.TokenType.NEW_BLANK_CELL) {
                    throwError(
                            "Expected a CELL in the document but found a " + docToken.getType());
                }

                String  docStr  = "";
                if (docToken.getCell() != null) {
                    docStr = getCellValueAsString(docToken.getCell());
                }
                extractData(templateToken.getCell().getStringCellValue(), docStr, data);
                return false;
            case LOOP:
                // We try to map the loop inner tokens with the document tokens; every time we have a match, we record a loop.
                JebtTextTokenizer.LoopToken loopToken = (JebtTextTokenizer.LoopToken)templateToken;

                List<Token> innerTokens = loopToken.getLoopTokens();

                // We initialize the list here in order to have an empty JSONArray in our object even if the loop doesn't match anything in the document.
                // It's better than not having the JSONArray key appear in the generated JSON.
                ParsingUtils.initCollection(data, loopToken.getCollectionJsonPath(), -1, loopToken.getLoopItemName());

                int loopedBeanIndex = 0;

                boolean isFirst = true;

                ++loopDepth;

                if (loopDepth == 1) {
                    // main loop, can contain new rows
                    boolean matched = false;
                    List<Token> loopMatchCandidates = null;
                    do {
                        Token rowSeparator = null;

                        if (isFirst) {
                            isFirst = false;
                        } else {
                            // There's a new Row between each loop match, so we'll read it first.
                            rowSeparator = docSheetReader.readNext();
                        }

                        loopMatchCandidates = getLoopPotentialMatchingTokens(innerTokens, docSheetReader);

                        if (loopMatchCandidates != null && isMatch(innerTokens, loopMatchCandidates)) {
                            matched = true;
                            applyLoopMatch(loopToken, loopMatchCandidates, data, loopedBeanIndex);
                            ++loopedBeanIndex;
                        } else {
                            // Not a match, let's pretend nothing happened and move on.
                            matched = false;
                            if (loopMatchCandidates != null) {
                                docSheetReader.reinjectTokens(loopMatchCandidates);
                                // Row separator should be returned next.
                                docSheetReader.reinjectToken(rowSeparator);
                            }
                        }

                    } while (matched);
                } else {
                    // inner loops cannot contain new rows, so it's just cell-by-cell matching.
                    // The SheetReader here is just looking at some potential matches, but it can still contain NEW_ROW
                    boolean matched = false;
                    List<Token> remainingRowCandidates = null;
                    do {

                        // Passing an empty array List will return everything until the end of the first line.
                        remainingRowCandidates = getLoopPotentialMatchingTokens(new ArrayList<Token>(), docSheetReader);

                        if (remainingRowCandidates != null && isMatch(innerTokens, remainingRowCandidates)) {
                            matched = true;
                            List<Token> remainingTokens = applyLoopMatch(loopToken, remainingRowCandidates, data, loopedBeanIndex);
                            docSheetReader.reinjectTokens(remainingTokens);
                            ++loopedBeanIndex;
                        } else {
                            // Not a match, let's pretend nothing happened and move on.
                            matched = false;
                            if (remainingRowCandidates != null) {
                                docSheetReader.reinjectTokens(remainingRowCandidates);
                            }
                        }

                    } while (matched);

                }

                --loopDepth;
                // As soon as we don't have a match, we consider the loop is over and we move on to the rest of the template.
                return false;
            default:
                throwError("Do not expect token "+templateToken.getType()+" while parsing Excel template");
                return true;
        }
    }

    /**
     * This method will match inner tokens of the Loop tokens against the loopMatchCandidates,
     * and return whatever tokens from loopMatchCandidates weren't part of the match (if any).
     */
    private List<Token> applyLoopMatch(JebtTextTokenizer.LoopToken loopToken, List<Token> loopMatchCandidates, Map data, int loopedBeanIndex)
    {
        SheetReader candidatesReader = new SheetReader(loopMatchCandidates);

        List loopedCollectionList = ParsingUtils
                .initCollection(data, loopToken.getCollectionJsonPath(), loopedBeanIndex, loopToken.getLoopItemName());

        // Since it's a match, we read from the reader and update data accordingly.

        for (Token loopInnerToken: loopToken.getLoopTokens()) {
            processSingleToken(loopInnerToken, candidatesReader, data);
        }

        // Now that parsing of one item has been completed we need to retrieve it, store it in data and clean the context
        Object obj = data.get(loopToken.getLoopItemName());
        loopedCollectionList.set(loopedBeanIndex, obj);
        data.remove(loopToken.getLoopItemName());

        List<Token> remainingTokens = new ArrayList<>();
        Token t;
        while ((t = candidatesReader.readNext()) != Token.EOD) {
            remainingTokens.add(t);
        }

        return remainingTokens;
    }

    private boolean isMatch(List<Token> innerTokens, List<Token> loopMatchCandidates) {
        SheetReader candidatesReader = new SheetReader(loopMatchCandidates);

        Map dummy = new HashMap();

        try {
            for (Token templateToken : innerTokens) {
                processSingleToken(templateToken, candidatesReader, dummy);
            }
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    // Potentially matching tokens have the same number of NEW_ROW or NEW_BLANK_ROW as in the passed innerTokens.
    private List<Token> getLoopPotentialMatchingTokens(List<Token> innerTokens, SheetReader docSheetReader) {
        List<Token> potentialMatchingTokens = new ArrayList<>();

        // First, we extract key tokens from innerTokens, i.e. NEW_ROW and NEW_BLANK_ROW tokens.
        List<Token> keyTokens = new ArrayList<>();
        for (Token token:innerTokens) {
            if (token.getType() == Token.TokenType.NEW_ROW || token.getType() == Token.TokenType.NEW_BLANK_ROW) {
                keyTokens.add(token);
            }
        }

        // Then, we read from the reader and allow any number of cells in-between the key tokens
        Token t = readNonRowsTokens(docSheetReader, potentialMatchingTokens);



        for (Token keyToken:keyTokens) {

            potentialMatchingTokens.add(t);

            if (t.getType() != keyToken.getType() && t != Token.EOD) {
                // Not a match!
                docSheetReader.reinjectTokens(potentialMatchingTokens);
                return null;
            } else {
                // match so far.
            }

            t = readNonRowsTokens(docSheetReader, potentialMatchingTokens);
        }

        // last matching ROW token should not be included in the matching Tokens
        docSheetReader.reinjectToken(t);

        return potentialMatchingTokens;
    }

    /**
     * Reads token from the reader and adds them to the List, until hitting a ROW or EOD token, that it will return.
     */
    private Token readNonRowsTokens(SheetReader sheetReader, List<Token> potentialMatchingTokens) {
        Token t;
        while ((t = sheetReader.readNext()) != Token.EOD && t.getType() != Token.TokenType.NEW_ROW && t.getType() != Token.TokenType.NEW_BLANK_ROW) {
            potentialMatchingTokens.add(t);
        }
        return t;
    }

    /**
     * @return true if you can read from the reader and that it'll match the loopTokens. The reader is returned in the same read state it was passed.
     *
     */
    private boolean isLoopMatch(SheetReader docSheetReader, List<Token> loopTokens) {

        List<Token> matchedTokens = new ArrayList<>();
        Map dummy = new HashMap();

        // First, we try to match all tokens, but pass a dummy Data object so that changes do not impact us.
        for (Token loopToken:loopTokens) {

            Token sheetToken = docSheetReader.readNext();
            matchedTokens.add(sheetToken);

            docSheetReader.reinjectToken(sheetToken);

            try {
                processSingleToken(loopToken, docSheetReader, dummy);
            } catch (Exception e) {
                // An exception means that it's not a match. Let's cancel loop matching.
                docSheetReader.reinjectTokens(matchedTokens);
                return false;
            }
        }

        if (loopDepth == 1) {
            // Main Loops have a new row at the end of every record.
            Token lastToken = docSheetReader.readNext();

            matchedTokens.add(lastToken);

            if (lastToken.getType() != Token.TokenType.NEW_ROW && lastToken.getType() != Token.TokenType.NEW_BLANK_ROW) {
                // not a match!
                docSheetReader.reinjectTokens(matchedTokens);
                return false;
            }
        }

        // If we're here, it means it's a match.
        docSheetReader.reinjectTokens(matchedTokens);
        return true;
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
                    // Double value = cell.getNumericCellValue(); // Doing so will turn integer into double by adding .0 at the end, we don't want that.
                    strCellValue = cell.getStringCellValue();
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
