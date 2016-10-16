package lib.sbite.txt;

import lib.sbite.BaseSbitWriter;
import lib.sbite.SbitWriter;
import org.apache.poi.util.IOUtils;

import java.io.*;
import java.util.Map;

/**
 * Simple Implementation of {@link SbitWriter} for plain old text files.
 */
public class SbitextWriter extends BaseSbitWriter {

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
    public SbitextWriter(Reader templateReader, Writer outputWriter) {
        this.templateReader = templateReader;
        this.outputWriter = outputWriter;
    }

    public SbitextWriter(String templateFilePath, String outputFilePath) {
        this(new File(templateFilePath), new File(outputFilePath));
    }

    public SbitextWriter(File templateFile, File outputFile) {
        this.templateFile = templateFile;
        this.outputFile = outputFile;
        shouldCreateReaderWriterOnRun = true;
    }


}
