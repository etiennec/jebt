package lib.sbite.xlsx;

import lib.sbite.Factory;
import lib.sbite.SbetReader;

import java.util.Map;

/**
 Implementation of {@link XlsxSbetReader} for XLSX Excel format.
 */
public class XlsxSbetReader implements SbetReader
{
    public Map<String, Object> readData() {
        return null;
    }

    public void setClass(String beanName, Class clazz) {
        // TODO
    }

    @Override
    public void setFactory(String beanPath, Factory factory) {
        // TODO
    }

    @Override
    public void setDefaultClass(Class clazz) {
        // TODO
    }
}
