package lib.sbite;

import java.util.Map;

/**
 * Reads the source (file, etc.), returns the data.<br>
 *     The class passed in {@link #setDefaultClass(Class)} will be instantiated by default whenever we need to create new data objects when reading the template. String will be used by default if nothing is set.
 *     It's possible to set the class of other objects depending on the bean path by using {@link #setClass(String, Class)}.
 */
public interface SbitReader {

    public Map<String, Object> readData();

    public void setClass(String beanPath, Class clazz);

    public void setFactory(String beanPath, Factory factory);

    public void setDefaultClass(Class clazz);
}
