package lib.jebt;

import lib.jebt.parser.JebtParseException;
import lib.jebt.parser.JebtTextTokenizer;
import lib.jebt.parser.Token;
import org.junit.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by canaud on 10/22/2016.
 */
public class JebtTokenizerTest {

    @Test
    public void testLoops() {
        String txt = "Hello {{normal.tag}} Here's the list: {[todo.list|item]} name: {{item.name}} / Value: {{item.value}}. {[item]}. How good is it?";
        StringReader reader = new StringReader(txt);

        JebtTextTokenizer tokenizer = new JebtTextTokenizer(reader);

        List<Token> tokens = tokenizer.readAll();

        assertTrue(!tokens.isEmpty());

        assertEquals(6, tokens.size());

        assertTrue(tokens.get(3) instanceof JebtTextTokenizer.LoopToken);
        JebtTextTokenizer.LoopToken loop = (JebtTextTokenizer.LoopToken) tokens.get(3);

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

        JebtTextTokenizer tokenizer = new JebtTextTokenizer(reader);

        List<Token> tokens = tokenizer.readAll();

        assertTrue(!tokens.isEmpty());
        assertEquals(4, tokens.size());

        assertTrue(tokens.get(1) instanceof JebtTextTokenizer.LoopToken);
        JebtTextTokenizer.LoopToken loop = (JebtTextTokenizer.LoopToken) tokens.get(1);

        assertTrue(loop.getLoopTokens() != null && !loop.getLoopTokens().isEmpty());
        assertEquals(4, loop.getLoopTokens().size());

        // Nested loop
        assertTrue(loop.getLoopTokens().get(3) instanceof JebtTextTokenizer.LoopToken);
    }

    @Test
    public void testSyntaxErrors() {
        try {
            new JebtTextTokenizer(new StringReader("unclosed loop tag: {[")).readAll();
            fail("Opening loop tag should be properly closed.");
        } catch (JebtParseException e) {
            // Success
        }

        try {
            new JebtTextTokenizer(new StringReader("{[apples|apple]} {{apple}} {[orange]}")).readAll();
            fail("Opening loop tag item name should match closing tag item name (or keep closing loop tag empty for auto-match)");
        } catch (JebtParseException e) {
            // Success
        }

        try {
            new JebtTextTokenizer(new StringReader("{[apples|apple]} {{apple}} pie is not finished")).readAll();
            fail("Text cannot finish while a loop tag is still unclosed.");
        } catch (JebtParseException e) {
            // Success
        }
    }
}
