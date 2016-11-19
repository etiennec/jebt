package lib.sbite;

import lib.sbite.parser.SbitParseException;
import lib.sbite.parser.SbitTextTokenizer;
import lib.sbite.parser.Token;
import org.junit.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by canaud on 10/22/2016.
 */
public class SbitTokenizerTest {

    @Test
    public void testLoops() {
        String txt = "Hello {{normal.tag}} Here's the list: {[todo.list|item]} name: {{item.name}} / Value: {{item.value}}. {[item]}. How good is it?";
        StringReader reader = new StringReader(txt);

        SbitTextTokenizer tokenizer = new SbitTextTokenizer(reader);

        List<Token> tokens = tokenizer.readAll();

        assertTrue(!tokens.isEmpty());

        assertEquals(5, tokens.size());

        assertTrue(tokens.get(3) instanceof SbitTextTokenizer.LoopToken);
        SbitTextTokenizer.LoopToken loop = (SbitTextTokenizer.LoopToken) tokens.get(3);

        assertTrue(loop.getLoopTokens() != null && !loop.getLoopTokens().isEmpty());

        assertEquals(5, loop.getLoopTokens().size());
    }

    @Test
    public void testNestedLoops() {
        String txt = "List: " +
                "{[todo.list|item]} name: {{item.name}} / Colors: " +
                    "{[item.colors|color]}color:{{color}} Letters:" +
                        "{[color.letters|letter]}letter|" +
                        "{[letter]}" +
                    "{[]}" +
                "{[item]}. How good is it?";
        StringReader reader = new StringReader(txt);

        SbitTextTokenizer tokenizer = new SbitTextTokenizer(reader);

        List<Token> tokens = tokenizer.readAll();

        assertTrue(!tokens.isEmpty());
        assertEquals(3, tokens.size());

        assertTrue(tokens.get(1) instanceof SbitTextTokenizer.LoopToken);
        SbitTextTokenizer.LoopToken loop = (SbitTextTokenizer.LoopToken) tokens.get(1);

        assertTrue(loop.getLoopTokens() != null && !loop.getLoopTokens().isEmpty());
        assertEquals(4, loop.getLoopTokens().size());

        // Nested loop
        assertTrue(loop.getLoopTokens().get(3) instanceof SbitTextTokenizer.LoopToken);
    }

    @Test
    public void testSyntaxErrors() {
        try {
            new SbitTextTokenizer(new StringReader("unclosed loop tag: {[")).readAll();
            fail("Opening loop tag should be properly closed.");
        } catch (SbitParseException e) {
            // Success
        }

        try {
            new SbitTextTokenizer(new StringReader("{[apples|apple]} {{apple}} {[orange]}")).readAll();
            fail("Opening loop tag item name should match closing tag item name (or keep closing loop tag empty for auto-match)");
        } catch (SbitParseException e) {
            // Success
        }

        try {
            new SbitTextTokenizer(new StringReader("{[apples|apple]} {{apple}} pie is not finished")).readAll();
            fail("Text cannot finish while a loop tag is still unclosed.");
        } catch (SbitParseException e) {
            // Success
        }
    }
}
