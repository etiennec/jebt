package lib.jebt.parser;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

/**
 * Tokenizer for one XLSX Sheet. The last token will be Token.EOD.
 *
 * We cannot read tokens with excel-streaming-reader because it cannot read cell comments, where we store loop information.
 * But it's not a problem, template files shouldn't be too large.
 */
public class JebtXlsxTokenizer implements JebtTokenizer {

    Queue<Token> tokens = new LinkedList<>();

    private int rowIndex = -1;

    private int columnIndex = -1;

    private Sheet sheet = null;

    private int maxRowIndex = -1;

    private int currentRowMaxColumnIndex = -1;

    private Stack<JebtTextTokenizer.LoopToken> loopStack = new Stack<JebtTextTokenizer.LoopToken>();

    boolean eodReturned = false;

    public JebtXlsxTokenizer(Sheet sheet) {
        this(sheet, -1, -1);
    }

    private JebtXlsxTokenizer(Sheet sheet, int rowIndex, int columnIndex) {
        this.rowIndex = rowIndex;
        this.columnIndex = columnIndex;
        this.sheet = sheet;
        this.maxRowIndex = sheet.getLastRowNum();
    }

    @Override public Token readNext() {
        // We read next token while we've got something valuable to return and all loops are closed.
        while (tokens.isEmpty() || !loopStack.empty()) {
            fetchNextToken();
        }

        return tokens.remove();
    }

    private void fetchNextToken() {
        if (eodReturned) {
            throw new RuntimeException(
                    "End-of-document Token already returned, yet you still want to read more tokens. You're doing something wrong, dev!");
        }

        // Is there a next cell to read?
        if (rowIndex >= 0 && columnIndex >= -1 && columnIndex < currentRowMaxColumnIndex) {
            // process next cell
            columnIndex++;
            Cell cell = sheet.getRow(rowIndex).getCell(columnIndex);

            // Opening any loop
            while (checkStartLoop(cell)) {
            }

            // This is either a normal cell, or a cell with loop closing tags.
            addToken(new Token(cell));

            // Closing any opened loop
            while (checkEndLoop(cell)) {
            }

            return;
        }

        // No new cell to read here, so go to next row.

        if (rowIndex >= maxRowIndex) {
            // Last line, End of sheet reached.

            if (!loopStack.empty()) {
                throw new RuntimeException("End of sheet reached but some Loop are still unclosed. Forgot a closing tag?");
            }

            addToken(Token.EOD);
            eodReturned = true;
            return;
        }

        // Go to next row
        rowIndex++;
        Row row = sheet.getRow(rowIndex);
        currentRowMaxColumnIndex = row == null ? -1 : (row.getLastCellNum()-1);
        addToken(new Token(Token.TokenType.NEW_ROW, null));
        columnIndex = -1;

        return;
    }

    /**
     * @return true if the cell contained a loop closing statement in its comments (and the currently opened loop was closed), false otherwise
     */
    private boolean checkEndLoop(Cell cell) {

        if (cell == null) {
            return false;
        }

        Comment comment = cell.getCellComment();
        if (comment == null || comment.getString() == null || StringUtils.isBlank(comment.getString().toString())) {
            return false;
        }

        String commentStr = comment.getString().toString().trim();

        if (commentStr.startsWith("{[") && commentStr.endsWith("]}")) {
            // Maybe a valid loop closing tag
            try {
                JebtTextTokenizer.LoopToken lt =
                        new JebtTextTokenizer.LoopToken(commentStr.substring(2, commentStr.indexOf("]}")));
                if (StringUtils.isBlank(lt.getCollectionJsonPath())) {
                    // Closing tag.

                    JebtTextTokenizer.LoopToken currentlyOpenedLoopToken = loopStack.peek();
                    if (currentlyOpenedLoopToken == null) {
                        throw new RuntimeException("Found closing loop tag but there was no opened loop");
                    }

                    if (!StringUtils.isBlank(lt.getLoopItemName())) {
                        // Checking that loop item names do match
                        if (!lt.getLoopItemName().equals(currentlyOpenedLoopToken.getLoopItemName())) {
                            throw new RuntimeException(
                                    "Loop item names mismatch on closing tag - expected " + currentlyOpenedLoopToken
                                            .getLoopItemName() + " but got " + lt.getLoopItemName());
                        }
                    }

                    // We remove closing tag from comment
                    cell.getCellComment().setString(new XSSFRichTextString(commentStr.substring(commentStr.indexOf("]}") + 2)));

                    // We effectively close the loop tag
                    loopStack.pop();
                    return true;
                } else {
                    throw new RuntimeException(
                            "Expected Closing loop tag but found opening loop tag with getCollectionJSonPath " + lt
                                    .getCollectionJsonPath());
                }
            } catch (Exception e) {
                return false;
            }
        }

        return false;
    }

    /**
     * @return true if the cell comments starts with a loop opening tag (and has started the loop if that's ths case), false otherwise.
     */
    private boolean checkStartLoop(Cell cell) {

        if (cell == null) {
            return false;
        }

        Comment comment = cell.getCellComment();
        if (comment == null || comment.getString() == null || StringUtils.isBlank(comment.getString().toString())) {
            return false;
        }

        String commentStr = comment.getString().toString().trim();

        if (commentStr.startsWith("{[") && commentStr.endsWith("]}")) {
            // Maybe a valid loop starting tag
            try {
                JebtTextTokenizer.LoopToken startLoop =
                        new JebtTextTokenizer.LoopToken(commentStr.substring(2, commentStr.indexOf("]}")));
                if (!StringUtils.isBlank(startLoop.getCollectionJsonPath()) && !StringUtils.isBlank(startLoop.getLoopItemName())) {
                    // Opening tag.

                    addToken(startLoop);
                    loopStack.push(startLoop);

                    // We remove opening tag from comment as it's been processed
                    cell.getCellComment().setString(new XSSFRichTextString(commentStr.substring(commentStr.indexOf("]}") + 2)));

                    return true;
                } else {
                    // Maybe a closing tag? Anyway, not another opening loop tag.
                    return false;
                }
            } catch (Exception e) {
                // Invalid opening loop tag, we'll ignore it.
                return false;
            }
        }

        return false;
    }

    private void addToken(Token t) {
        if (!loopStack.empty()) {
            // Append current token to opened loop
            loopStack.peek().addLoopInnerToken(t);
        } else {
            // add token to queue.
            tokens.add(t);
        }

    }

}
