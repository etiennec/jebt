package lib.sbet;

import java.util.Map;

/**
 * Reads the source (file, etc.), returns the data.
 */
public interface SbetReader<T> {

    public Map<String, T> readData();

}
