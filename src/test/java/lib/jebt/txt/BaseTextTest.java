package lib.jebt.txt;

import lib.jebt.TestConstants;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by canaud on 10/31/2018.
 */
public class BaseTextTest implements TestConstants {

    protected List<String> getAllTestTemplateNames() {
        List<String> templateNames = new ArrayList<String>();

        File[] files = new File(TEST_RESOURCES_PATH + "/txt").listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(TXT_TEMPLATE_FILE_SUFFIX);
            }
        });

        for (File file : files) {
            templateNames.add(file.getName().substring(0, (file.getName().length() - TXT_TEMPLATE_FILE_SUFFIX.length())));
        }

        return templateNames;
    }
}
