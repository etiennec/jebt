package lib.sbite;

import java.util.Map;

/**
 * Writes the data to its destination (file, etc.)
 */
public interface SbitWriter {

    public void writeData(Map<String, Object> data);

}
