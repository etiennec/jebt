package lib.sbet;

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
        BaseSbetWriter baseWriter = new BaseSbetWriter() {
            public void writeData(Map<String, Object> data) {
            }
        };

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
        assertEquals("bar", baseWriter.evaluateExpression("{{foo}}", data));
        assertEquals("bar", baseWriter.evaluateExpression("{{ foo }}", data));
        assertEquals("bar", baseWriter.evaluateExpression("{{foo }}", data));
        assertEquals("bar", baseWriter.evaluateExpression("{{ foo}}", data));

        // Indexed access
        assertEquals("red", baseWriter.evaluateExpression("{{colorsList[0]}}", data));
        assertEquals("green", baseWriter.evaluateExpression("{{colorsList[1]}}", data));
        assertEquals("blue", baseWriter.evaluateExpression("{{colorsList[2]}}", data));
        assertEquals("red", baseWriter.evaluateExpression("{{colorsArray[0]}}", data));
        assertEquals("green", baseWriter.evaluateExpression("{{colorsArray[1]}}", data));
        assertEquals("blue", baseWriter.evaluateExpression("{{colorsArray[2]}}", data));

        // property chain
        assertEquals("123", baseWriter.evaluateExpression("{{addresses[0].number}}", data));
        assertEquals("Main Boulevard", baseWriter.evaluateExpression("{{addresses[0].streetName}}", data));
        assertEquals("200093", baseWriter.evaluateExpression("{{addresses[0].postalCode}}", data));
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
