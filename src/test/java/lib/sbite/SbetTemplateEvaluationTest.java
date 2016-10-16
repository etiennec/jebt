package lib.sbite;

import lib.sbite.parser.SbetWriterTextProcessor;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class SbetTemplateEvaluationTest
{

    @Test
    public void testTemplateExpressionEvaluation()
    {
        SbetWriterTextProcessor parser = new SbetWriterTextProcessor();

        Map<String, Object> data = new HashMap<String, Object>();

        data.put("foo", "bar");

        String[] colorsArray = {"red", "green", "blue"};

        List<String> colorsList = Arrays.asList(colorsArray);
        data.put("colorsList", colorsList);
        data.put("colorsArray", colorsArray);

        Address address = new Address();
        address.setNumber(123);
        address.setStreetName("Main Boulevard");
        address.setPostalCode(200093L);

        data.put("addresses", new Address[] {address});


        // We don't care about spaces in the expression
        assertEquals("bar", parser.evaluateExpression("foo", data));
        assertEquals("bar", parser.evaluateExpression(" foo ", data));
        assertEquals("bar", parser.evaluateExpression("foo ", data));
        assertEquals("bar", parser.evaluateExpression(" foo", data));

        // Indexed access
        assertEquals("red", parser.evaluateExpression("colorsList[0]", data));
        assertEquals("green", parser.evaluateExpression("colorsList[1]", data));
        assertEquals("blue", parser.evaluateExpression("colorsList[2]", data));
        assertEquals("red", parser.evaluateExpression("colorsArray[0]", data));
        assertEquals("green", parser.evaluateExpression("colorsArray[1]", data));
        assertEquals("blue", parser.evaluateExpression("colorsArray[2]", data));

        // property chain
        assertEquals("123", parser.evaluateExpression("addresses[0].number", data));
        assertEquals("Main Boulevard", parser.evaluateExpression("addresses[0].streetName", data));
        assertEquals("200093", parser.evaluateExpression("addresses[0].postalCode", data));
    }

    public class Address {
        private String streetName;
        private Integer number;
        private Long postalCode;

        public String getStreetName() {
            return streetName;
        }

        public void setStreetName(String streetName) {
            this.streetName = streetName;
        }

        public Integer getNumber() {
            return number;
        }

        public void setNumber(Integer number) {
            this.number = number;
        }

        public Long getPostalCode() {
            return postalCode;
        }

        public void setPostalCode(Long postalCode) {
            this.postalCode = postalCode;
        }
    }
}
