package lib.jebt.xlsx;

import lib.jebt.TestUtils;
import lib.jebt.parser.JebtXlsxTokenizer;
import lib.jebt.parser.Token;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.IOUtils;
import org.junit.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class JebtXlsxTokenizerTest extends BaseJebtXlsxTest {

    @Test
    public void testbasicXlsxTokenizer() {

        Workbook wb = getXSSFWorkbook("/xlsx/basicXlsxTemplateResult.xlsx");

        JebtXlsxTokenizer tokenizer = new JebtXlsxTokenizer(wb.getSheetAt(0));


        List<Token> tokens = new ArrayList<Token>();

        Token t;

        while ((t = tokenizer.readNext()) != Token.EOD) {
            tokens.add(t);
        }

        assertEquals(tokens.size(), 22);

        assertEquals(tokens.get(0).getType(), Token.TokenType.NEW_ROW);
        assertEquals(tokens.get(1).getType(), Token.TokenType.NEW_TEXT_CELL);
        assertEquals(tokens.get(2).getType(), Token.TokenType.NEW_ROW);
        assertEquals(tokens.get(3).getType(), Token.TokenType.NEW_TEXT_CELL);
        assertEquals(tokens.get(4).getType(), Token.TokenType.NEW_ROW);
        assertEquals(tokens.get(5).getType(), Token.TokenType.NEW_TEXT_CELL);
        assertEquals(tokens.get(6).getType(), Token.TokenType.NEW_ROW);
        assertEquals(tokens.get(7).getType(), Token.TokenType.NEW_TEXT_CELL);
        assertEquals(tokens.get(8).getType(), Token.TokenType.NEW_ROW);
        assertEquals(tokens.get(9).getType(), Token.TokenType.NEW_BLANK_CELL);
        assertEquals(tokens.get(10).getType(), Token.TokenType.NEW_TEXT_CELL);
        assertEquals(tokens.get(11).getType(), Token.TokenType.NEW_BLANK_ROW);
        assertEquals(tokens.get(12).getType(), Token.TokenType.NEW_ROW);
        assertEquals(tokens.get(13).getType(), Token.TokenType.NEW_TEXT_CELL);
        assertEquals(tokens.get(14).getType(), Token.TokenType.NEW_BLANK_ROW);
        assertEquals(tokens.get(15).getType(), Token.TokenType.NEW_ROW);
        assertEquals(tokens.get(16).getType(), Token.TokenType.NEW_BLANK_CELL);
        assertEquals(tokens.get(17).getType(), Token.TokenType.NEW_BLANK_CELL);
        assertEquals(tokens.get(18).getType(), Token.TokenType.NEW_BLANK_CELL);
        assertEquals(tokens.get(19).getType(), Token.TokenType.NEW_BLANK_CELL);
        assertEquals(tokens.get(20).getType(), Token.TokenType.NEW_BLANK_CELL);
        assertEquals(tokens.get(21).getType(), Token.TokenType.NEW_TEXT_CELL);


    }


    @Test
    public void testLoopsXlsxTokenizer() {

        Workbook wb = getXSSFWorkbook("/xlsx/simpleLoopXlsxTemplate.xlsx");

        JebtXlsxTokenizer tokenizer = new JebtXlsxTokenizer(wb.getSheetAt(0));


        List<Token> tokens = new ArrayList<Token>();

        Token t;

        while ((t = tokenizer.readNext()) != Token.EOD) {
            tokens.add(t);
        }

        assertEquals(tokens.get(0).getType(), Token.TokenType.NEW_ROW);
        assertEquals(tokens.get(1).getType(), Token.TokenType.NEW_TEXT_CELL);
        assertEquals(tokens.get(2).getType(), Token.TokenType.NEW_ROW);
        assertEquals(tokens.get(3).getType(), Token.TokenType.NEW_TEXT_CELL);
        assertEquals(tokens.get(4).getType(), Token.TokenType.NEW_ROW);
        assertEquals(tokens.get(5).getType(), Token.TokenType.NEW_TEXT_CELL);
        assertEquals(tokens.get(6).getType(), Token.TokenType.NEW_ROW);
        assertEquals(tokens.get(7).getType(), Token.TokenType.NEW_TEXT_CELL);
        assertEquals(tokens.get(8).getType(), Token.TokenType.NEW_BLANK_ROW);
        assertEquals(tokens.get(9).getType(), Token.TokenType.NEW_ROW);
        assertEquals(tokens.get(10).getType(), Token.TokenType.NEW_TEXT_CELL);
        assertEquals(tokens.get(11).getType(), Token.TokenType.NEW_ROW);
        assertEquals(tokens.get(12).getType(), Token.TokenType.NEW_TEXT_CELL);
        assertEquals(tokens.get(13).getType(), Token.TokenType.NEW_TEXT_CELL);
        assertEquals(tokens.get(14).getType(), Token.TokenType.NEW_ROW);
        assertEquals(tokens.get(15).getType(), Token.TokenType.LOOP);
        assertEquals(tokens.get(16).getType(), Token.TokenType.NEW_BLANK_ROW);
        assertEquals(tokens.get(17).getType(), Token.TokenType.NEW_ROW);
        assertEquals(tokens.get(18).getType(), Token.TokenType.NEW_TEXT_CELL);
        assertEquals(tokens.get(19).getType(), Token.TokenType.NEW_TEXT_CELL);
        assertEquals(tokens.get(20).getType(), Token.TokenType.NEW_ROW);
        assertEquals(tokens.get(21).getType(), Token.TokenType.LOOP);
        assertEquals(tokens.get(22).getType(), Token.TokenType.NEW_BLANK_ROW);
        assertEquals(tokens.get(23).getType(), Token.TokenType.NEW_BLANK_ROW);
        assertEquals(tokens.get(24).getType(), Token.TokenType.NEW_ROW);
        assertEquals(tokens.get(25).getType(), Token.TokenType.NEW_TEXT_CELL);
        assertEquals(tokens.get(26).getType(), Token.TokenType.NEW_ROW);
        assertEquals(tokens.get(27).getType(), Token.TokenType.LOOP);



        assertEquals(28, tokens.size());

    }

}
