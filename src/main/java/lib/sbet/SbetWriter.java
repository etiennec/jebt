package lib.sbet;

import java.util.Map;

/**
 * Writes the data to its destination (file, etc.)
 */
public interface SbetWriter {

    public void writeData(Map<String, Object> data);

}
