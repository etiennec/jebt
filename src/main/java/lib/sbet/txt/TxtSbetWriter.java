package lib.sbet.txt;

import lib.sbet.BaseSbetWriter;
import lib.sbet.SbetWriter;
import org.apache.poi.util.IOUtils;

import java.io.*;
import java.util.Map;

/**
 * Simple Implementation of {@link SbetWriter} for plain old text files.
 */
public class TxtSbetWriter extends BaseSbetWriter {

    private Reader templateReader;
    private Writer outputWriter;

    private File templateFile;
    private File outputFile;

    boolean shouldCreateReaderWriterOnRun = false;

    public void writeData(Map<String, Object> data) {

        try {

            if (shouldCreateReaderWriterOnRun) {
                templateReader = new BufferedReader(new FileReader(templateFile));
                outputWriter = new BufferedWriter(new FileWriter(outputFile));
            }

            convertString(templateReader, outputWriter, data);

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (shouldCreateReaderWriterOnRun) {
                if (templateReader != null) {
                    IOUtils.closeQuietly(templateReader);
                }
                if (outputWriter != null) {
                    IOUtils.closeQuietly(outputWriter);
                }
            }
        }
    }

    /**
     * @param templateReader the {@link Reader} used to read the template text.
     * @param outputWriter the {@link Writer} used to write the result of the filled in template.
     */
    public TxtSbetWriter(Reader templateReader, Writer outputWriter) {
        this.templateReader = templateReader;
        this.outputWriter = outputWriter;
    }

    public TxtSbetWriter(String templateFilePath, String outputFilePath) {
        this(new File(templateFilePath), new File(outputFilePath));
    }

    public TxtSbetWriter(File templateFile, File outputFile) {
        this.templateFile = templateFile;
        this.outputFile = outputFile;
        shouldCreateReaderWriterOnRun = true;
    }


}
