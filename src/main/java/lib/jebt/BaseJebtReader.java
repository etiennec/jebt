package lib.jebt;

import lib.jebt.parser.JebtReaderTextProcessor;

import java.io.Reader;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 This class contains most common logic (for beans manipulation) used by {@link JebtReader} implementations.
 */
public abstract class BaseJebtReader implements JebtReader
{
    /**
     * Method to call when there's no existing data Map to use as starting data.
     */
    public Map extractData(Reader templateReader, Reader documentReader) {
        Map data = new LinkedHashMap();
        this.extractData(templateReader, documentReader, data);
        return data;
    }

    /**
     * This method will edit/insert data in the passed data Map.
     */
    public void extractData(Reader templateReader, Reader documentReader, Map data) {
        JebtReaderTextProcessor textReaderProcessor = new JebtReaderTextProcessor();
        textReaderProcessor.extractData(templateReader, documentReader, data);
    }

    /**
     * Method to use when the template & document text are already available as String (watch out for memory consumption, prefer to use Readers if possible).
     */
    public void extractData(String templateText, String documentText, Map data) {
        this.extractData(new StringReader(templateText), new StringReader(documentText), data);
    }


}
