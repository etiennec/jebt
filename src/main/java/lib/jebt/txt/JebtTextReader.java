package lib.jebt.txt;

import lib.jebt.BaseJebtReader;
import org.apache.poi.util.IOUtils;

import java.io.*;
import java.util.Map;

/**
 * Simple Implementation of {@link JebtTextReader} for plain old text.
 */
public class JebtTextReader extends BaseJebtReader {

    private Reader templateReader;

    private Reader documentReader;

    private File templateFile;

    private File documentFile;

    // We only instantiate readers at the last moment on read operation
    boolean shouldCreateReadersOnRun = false;

    public Map readData() {

        try {

            if (shouldCreateReadersOnRun) {
                templateReader = new BufferedReader(new FileReader(templateFile));
                documentReader = new BufferedReader(new FileReader(documentFile));
            }

            return extractData(templateReader, documentReader);

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (shouldCreateReadersOnRun) {
                IOUtils.closeQuietly(templateReader);
                IOUtils.closeQuietly(documentReader);
            }
        }
    }

    /**
     * @param templateReader the {@link Reader} used to read the template text.
     * @param documentReader the {@link Reader} used to read the document with data filled in.
     */
    public JebtTextReader(Reader templateReader, Reader documentReader) {
        this.templateReader = templateReader;
        this.documentReader = documentReader;
    }

    public JebtTextReader(String templateFilePath, String documentFilePath) {
        this(new File(templateFilePath), new File(documentFilePath));
    }

    public JebtTextReader(File templateFile, File documentFile) {
        this.templateFile = templateFile;
        this.documentFile = documentFile;
        shouldCreateReadersOnRun = true;
    }
}
