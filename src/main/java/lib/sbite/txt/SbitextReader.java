package lib.sbite.txt;

import lib.sbite.BaseSbitReader;
import org.apache.poi.util.IOUtils;

import java.io.*;
import java.util.Map;

/**
 Simple Implementation of {@link SbitextReader} for plain old text.
 */
public class SbitextReader extends BaseSbitReader
{

    private Reader templateReader;
    private Reader documentReader;

    private File templateFile;
    private File documentFile;

    boolean shouldCreateReadersOnRun = false;

    public Map<String, Object> readData() {

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
                if (templateReader != null) {
                    IOUtils.closeQuietly(templateReader);
                }
                if (documentReader != null) {
                    IOUtils.closeQuietly(documentReader);
                }
            }
        }


    }

    /**
     * @param templateReader the {@link Reader} used to read the template text.
     * @param documentReader the {@link Reader} used to read the document with data filled in.
     */
    public SbitextReader(Reader templateReader, Reader documentReader) {
        this.templateReader = templateReader;
        this.documentReader = documentReader;
    }

    public SbitextReader(String templateFilePath, String documentFilePath) {
        this(new File(templateFilePath), new File(documentFilePath));
    }

    public SbitextReader(File templateFile, File documentFile) {
        this.templateFile = templateFile;
        this.documentFile = documentFile;
        shouldCreateReadersOnRun = true;
    }
}
