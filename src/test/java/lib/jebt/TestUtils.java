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

    public static InputStream getInputStream(String resourcePath) {
        try {
            return new FileInputStream(new File(TEST_RESOURCES_PATH + resourcePath));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static OutputStream getOutputStream(String resourcePath) {

        File outputFile = new File(TEST_RESOURCES_PATH + resourcePath);

        try {
            if (!outputFile.exists()) {
                outputFile.getParentFile().mkdirs();
                outputFile.createNewFile();
            }

            return new FileOutputStream(outputFile);
        } catch (Exception e) {
            throw new RuntimeException("Error getting outputstream of file "+outputFile.getAbsolutePath(), e);
        }
    }
}
