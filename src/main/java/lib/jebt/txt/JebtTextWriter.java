package lib.jebt.txt;

import lib.jebt.BaseJebtWriter;
import lib.jebt.JebtWriter;
import org.apache.poi.util.IOUtils;

import java.io.*;
import java.util.Map;

/**
 * Simple Implementation of {@link JebtWriter} for plain old text files.
 */
public class JebtTextWriter extends BaseJebtWriter {

    private Reader templateReader;

    private Writer outputWriter;

    private File templateFile;

    private File outputFile;

    boolean shouldCreateReaderWriterOnRun = false;

    public void writeData(Map data) {

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
                IOUtils.closeQuietly(templateReader);
                IOUtils.closeQuietly(outputWriter);
            }
        }
    }

    /**
     * @param templateReader the {@link Reader} used to read the template text.
     * @param outputWriter   the {@link Writer} used to write the result of the filled in template.
     */
    public JebtTextWriter(Reader templateReader, Writer outputWriter) {
        this.templateReader = templateReader;
        this.outputWriter = outputWriter;
    }

    public JebtTextWriter(String templateFilePath, String outputFilePath) {
        this(new File(templateFilePath), new File(outputFilePath));
    }

    public JebtTextWriter(File templateFile, File outputFile) {
        this.templateFile = templateFile;
        this.outputFile = outputFile;
        shouldCreateReaderWriterOnRun = true;
    }

}
