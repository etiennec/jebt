package lib.sbite.txt;

import lib.sbite.TestConstants;
import lib.sbite.TestUtils;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class SbitextWriterTest implements TestConstants
{

    @Test
    /**
     * This Test will automatically test all txt files in /test/resources/txt.
     * You just need to te
     */
    public void testWriteAllTxt() throws IOException {


        Map<String, Object> data = getDefaultData();

        for (String templateName : getAllTestTemplateNames()) {
            testTxtTemplate(templateName, data);
        }


    }

    private void testTxtTemplate(String templateName, Map<String, Object> data) throws IOException {
        System.out.println("## Testing template name "+templateName);



        Reader templateReader = TestUtils.getFileReader("/txt/"+templateName+TXT_TEMPLATE_FILE_SUFFIX);

        StringWriter output = new StringWriter();

        SbitextWriter writer = new SbitextWriter(templateReader, output);

        writer.writeData(data);

        String actualResult = output.toString();

        StringBuilder expectedResult = new StringBuilder();

        Reader solutionReader = TestUtils.getFileReader("/txt/"+templateName+TXT_RESULT_TEMPLATE_FILE_SUFFIX);
        int intValueOfChar;
        while ((intValueOfChar = solutionReader.read()) != -1) {
            expectedResult.append((char) intValueOfChar);
        }

        solutionReader.close();
        templateReader.close();

        assertEquals(expectedResult.toString(), actualResult);
    }

    private List<String> getAllTestTemplateNames() {
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

    private Map<String, Object> getDefaultData() {
        Map<String, Object> data = new HashMap<String, Object>();

        data.put("stringName", "World");

        List<Customer> customers= new ArrayList<Customer>();
        customers.add(new Customer());
        customers.add(null);
        customers.add(new Customer());

        customers.get(2).address.fields.put("postalCode", "200093");

        data.put("customers", customers);

        return data;
    }

    public class Customer {
        public String name = "test";
        public Address address = new Address();
    }

    public class Address {
        public Map<String, String> fields = new HashMap<String, String>();
    }


}
