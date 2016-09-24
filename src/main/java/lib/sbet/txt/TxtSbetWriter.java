package lib.sbet.txt;

import lib.sbet.BaseSbetWriter;
import lib.sbet.SbetWriter;

import java.io.Reader;
import java.io.Writer;
import java.util.Map;

/**
 * Simple Implementation of {@link SbetWriter} for plain old text.
 */
public class TxtSbetWriter extends BaseSbetWriter {

    private Reader templateReader;

    private Writer outputWriter;

    public void writeData(Map<String, Object> data) {
         // TODO
    }

    public void setTemplateReader(Reader templateReader) {
        this.templateReader = templateReader;
    }

    public void setOutputWriter(Writer outputWriter) {
        this.outputWriter = outputWriter;
    }
}
