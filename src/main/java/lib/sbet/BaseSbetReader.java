package lib.sbet;

import lib.sbet.parser.SbetReaderTextProcessor;

import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

/**
 This class contains most common logic (for beans manipulation) used by {@link SbetReader} implementations.
 */
public abstract class BaseSbetReader implements SbetReader
{
    private Map<String, Class> classesPerBeanPath = new HashMap<String, Class>();

    private Map<String, Factory> factoriesPerBeanPath = new HashMap<String, Factory>();

    private Class defaultClass = null;

    @Override
    public void setClass(String beanName, Class clazz) {
        classesPerBeanPath.put(beanName, clazz);
    }

    @Override
    public void setFactory(String beanPath, Factory factory) {
        factoriesPerBeanPath.put(beanPath, factory);
    }

    @Override
    public void setDefaultClass(Class clazz) {
        this.defaultClass = clazz;
    }

    /**
     * Method to call when there's no existing data Map to use as starting data.
     */
    public Map<String, Object> extractData(Reader templateReader, Reader documentReader) {
        Map<String, Object> data = new HashMap<String, Object>();
        this.extractData(templateReader, documentReader, data);
        return data;
    }

    /**
     * This method will edit/insert data in the passed data Map.
     */
    public void extractData(Reader templateReader, Reader documentReader, Map<String, Object> data) {
        SbetReaderTextProcessor textReaderProcessor = new SbetReaderTextProcessor();
        if (defaultClass != null) {
            textReaderProcessor.setDefaultClassToInstantiate(defaultClass);
        }
        textReaderProcessor.setClasses(classesPerBeanPath);
        textReaderProcessor.setFactories(factoriesPerBeanPath);
        textReaderProcessor.extractData(templateReader, documentReader, data);
    }

    /**
     * Method to use when the template & document text are already available as String (watch out for memory consumption, prefer to use Readers if possible).
     */
    public void extractData(String templateText, String documentText, Map<String, Object> data) {
        this.extractData(new StringReader(templateText), new StringReader(documentText), data);
    }


}
