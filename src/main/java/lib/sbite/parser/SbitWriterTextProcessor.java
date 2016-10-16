package lib.sbite.parser;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.Map;

/**
 * Class in charge of processing raw text with Sbet Template elements in it.
 */
public class SbitWriterTextProcessor extends SbitCommonTextProcessor {



    /**
     * @param sourceText A reader that reads template text.
     * @param outText A writer where we will write down the source text with all templating elements resolved.
     * @param data Data to use when filling template elements.
     */
    public void convertString(Reader sourceText, Writer outText, Map<String, Object> data) {

        SbitTextTokenizer tokenizer = new SbitTextTokenizer(sourceText);

        SbitTextTokenizer.Token token;

        try {
            while ((token = tokenizer.readNext()) != null) {
                if (token.getType() == SbitTextTokenizer.TokenType.EXPRESSION) {
                    outText.append(evaluateExpression(token.getText(), data));
                } else {
                    // TEXT Token
                    outText.append(token.getText());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    /**
     *
     * @param expression a Sbet expression (~ BeanUtils expression), without the enclosing curly braces
     * @param data the data to use to evaluate the expression
     * @return the evaluated expression, or an empty String if there's a null value along the path.
     */
    public String evaluateExpression(String expression, Map<String, Object> data) {

        if (StringUtils.isBlank(expression)) {
            return "";
        }

        expression = expression.trim();

        String[] elements = StringUtils.split(expression, '.');

        // elements[0] is special as it's always an element of the data Map<>.
        final String key = getLeadingText(elements[0]);
        Object bean = data.get(key);

        if (bean == null) {
            return getNullResult();
        }

        elements[0] = elements[0].substring(key.length());

        List<AtomicExpression> atomicExprs = extractAtomicExpressions(key, elements);

        for (AtomicExpression atomicExpr : atomicExprs) {
            bean = atomicExpr.resolve(bean);
            if (bean == null) {
                return getNullResult();
            }
        }

        return bean.toString();

    }
}
