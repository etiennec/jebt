package lib.jebt;

import java.io.*;

/**
 * Created by canaud on 9/24/2016.
 */
public class TestUtils implements TestConstants {

    public static Reader getFileReader(String resourcePath) {
        try {
            return new BufferedReader(new FileReader(new File(TEST_RESOURCES_PATH + resourcePath)));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static Writer getFileWriter(String resourcePath) {
        try {
            return new BufferedWriter(new FileWriter(new File(TEST_RESOURCES_PATH + resourcePath)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
