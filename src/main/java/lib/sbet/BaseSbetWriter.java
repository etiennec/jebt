package lib.sbet;

import lib.sbet.parser.SbetTextProcessor;

import java.io.*;
import java.util.Map;

/**
 * This class contains most common logic for text parsing & beans manipulation used by {@link SbetWriter} implementations.
 */
public abstract class BaseSbetWriter implements SbetWriter{


    private SbetTextProcessor textProcessor = new SbetTextProcessor();

    /**
     *
     * @param input an input string, possibly with some templating elements
     * @return the output string, with all templating elements evaluated using the passed data.
     */
    public String convertString(String input, Map<String, Object> data) {
        Reader reader = new StringReader(input);
        Writer writer = new StringWriter();

        convertString(reader, writer, data);

        return writer.toString();
    }

    /**
     * API to be used when the amount of text can be very large and shouldn't be stored into memory.<br>
     *     If the text is rather small (for example, text in an Excel Cell), you'd better use {@link #convertString(String, Map)}.
     *
     * @param sourceText A reader that reads template text.
     * @param outText A writer where we will write down the source text with all templating elements resolved.
     * @param data Data to use when filling template elements.
     */
    public void convertString(Reader sourceText, Writer outText, Map<String, Object> data) {
        textProcessor.convertString(sourceText, outText, data);
    }

}
