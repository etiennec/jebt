package lib.sbet;

import lib.sbet.parser.SbetTextProcessor;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 This class contains most common logic (for beans manipulation) used by {@link SbetReader} implementations.
 */
public abstract class BaseSbetReader<T> implements SbetReader<T>
{

    private SbetTextProcessor parser = new SbetTextProcessor();

    private Map<String, Class> classesPerBeanName = new HashMap<String, Class>();

    public void setClass(String beanName, Class clazz) {
        classesPerBeanName.put(beanName, clazz);
    }

    public Map<String, Object> extractData(Reader templateReader, Reader documentReader) {

        Map<String, Object> data = new HashMap<String, Object>();

        parser.extractData(templateReader, documentReader, data);

        return data;
    }


}
