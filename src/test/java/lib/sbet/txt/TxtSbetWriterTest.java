package lib.sbet.txt;

import lib.sbet.TestUtils;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class TxtSbetWriterTest
{

    @Test
    public void testWrite()
    {
        TxtSbetWriter writer = new TxtSbetWriter();

        Map<String, Object> data = new HashMap<String, Object>();

        writer.setTemplateReader(TestUtils.getFileReader("/txt/basicTxtTemplate.txt"));

        writer.setOutputWriter(TestUtils.getFileWriter("/txt/basicTxtTemplateResult.txt"));

        writer.writeData(data);


    }
}
