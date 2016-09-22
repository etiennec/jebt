package lib.sbet;

import java.util.HashMap;
import java.util.Map;

/**
 This class contains most common logic (for beans manipulation) used by {@link SbetReader} implementations.
 */
public abstract class BaseSbetReader<T> implements SbetReader<T>
{

    private Map<String, Class> classesPerBeanName = new HashMap<String, Class>();

    public Map<String, T> readData() {
        // TODO
        return null;
    }

    public void setClass(String beanName, Class clazz) {
        classesPerBeanName.put(beanName, clazz);
    }


}
