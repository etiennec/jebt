package lib.jebt.xlsx;

import lib.jebt.parser.Token;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.Iterator;

/**
 * This class will read an Excel Sheet row by row and cell by cell, always forward, just like a tokenizer except it doesn't care about loops and comments as it's using excel-streaming-reader.
 * It is needed to capture empty rows and empty cells, we cannot use excel-stream-reader directly.
 * It will only return NEW_NON_TEXT_CELL, NEW_TEXT_CELL, EOD and NEW_ROW tokens. No EXPRESSION, no LOOP, no TEXT.
 */
public class SheetReader {

    private Iterator<Row> rowIterator;

    private Row currentRow;

    private Cell currentCell;

    private int expectedRowIndex = -1;

    private int expectedColumnIndex = -1;

    private Iterator<Cell> cellIterator;

    public SheetReader(Sheet docSheet) {
        rowIterator = docSheet.iterator();
        expectedRowIndex = 0;
    }

    public Token readNext() {

        // We read the next cell if there's a next cell to return.

        Token t = readNextCell();

        if (t != null) {
            return t;
        }

        return readNextRow();
    }

    private Token readNextRow() {

        if (rowIterator == null) {
            return Token.EOD;
        }

        if (currentRow != null) {
            if (expectedRowIndex < currentRow.getRowNum()) {
                // Empty rows are skipped by rowIterator but they are rows nonetheless that should be returned by the reader.
                ++expectedRowIndex;
                return new Token(Token.TokenType.NEW_ROW, null);
            } else {
                // Current row is the right active one, let's read cells.
                ++expectedRowIndex;
                cellIterator = currentRow.cellIterator();
                expectedColumnIndex = 0;
                currentRow = null;
                return new Token(Token.TokenType.NEW_ROW, null);
            }
        } else {
            if (!rowIterator.hasNext()) {
                // This sheet has no more rows.
                rowIterator = null;
                return Token.EOD;
            } else {
                // Fetch next row and try again
                currentRow = rowIterator.next();
                currentCell = null;
                cellIterator = null;
                expectedColumnIndex = 0;
                return readNext();
            }
        }
    }

    private Token readNextCell() {

        if (cellIterator == null) {
            return null;
        }


        if (currentCell != null) {
            if (expectedColumnIndex < currentCell.getColumnIndex()) {
                // Empty cells are skipped by cellIterator but they are cells nonetheless that should be returned by the reader.
                ++expectedColumnIndex;
                return new Token(null);
            } else {
                // Current cell is the one we want to return.
                Cell goodCell = currentCell;
                ++expectedColumnIndex;
                if (cellIterator.hasNext()) {
                    // Read next cell and return this one.
                    currentCell = cellIterator.next();
                    return new Token(goodCell);
                } else {
                    // There's no more cells to read
                    cellIterator = null;
                    currentCell = null;
                    return new Token(goodCell);
                }
            }
        } else {
            if (!cellIterator.hasNext()) {
                // This row has no cells.
                return null;
            } else {
                // Fetch next cell and try again
                currentCell = cellIterator.next();
                return readNext();
            }
        }
    }
}
