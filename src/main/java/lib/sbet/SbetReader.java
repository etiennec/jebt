package lib.sbet;

import java.util.Map;

/**
 * Reads the source (file, etc.), returns the data.<br>
 *     The type T will be instantiated by default whenever we need to create new data objects when reading the template. It's possible to set the class of other objects depending on the bean name by using {@link #setClass(String, Class)}.
 */
public interface SbetReader<T> {

    public Map<String, Object> readData();

    public void setClass(String beanName, Class clazz);

}
