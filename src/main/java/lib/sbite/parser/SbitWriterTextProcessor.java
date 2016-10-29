package lib.sbite.parser;

import org.apache.commons.collections.iterators.ArrayIterator;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
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
                processToken(token, outText, data);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void processToken(SbitTextTokenizer.Token token, Writer outText, Map<String, Object> data) throws IOException{
        if (token.getType() == SbitTextTokenizer.TokenType.EXPRESSION) {
            outText.append(evaluateExpression(token.getText(), data));
        } else if (token.getType() == SbitTextTokenizer.TokenType.LOOP) {
            SbitTextTokenizer.LoopToken loop = (SbitTextTokenizer.LoopToken)token;
            Object bean = evaluateBean(loop.getCollectionBeanPath(), data);
            if (bean == null) {
                return;
            }
            Iterator it;
            if (bean.getClass().isArray()) {
                it = new ArrayIterator(bean);
            } else if (bean instanceof Iterable) {
                it = ((Iterable)bean).iterator();
            } else {
                // Bean is not an array nor an iterable
                throw new SbitEvaluationException("Bean found at "+loop.getCollectionBeanPath()+" is not an Array or an Iterable");
            }

            while (it.hasNext()) {
                Object obj = it.next();

                Map<String, Object> dataCopy = new HashMap<String, Object>(data);
                dataCopy.put(loop.getItemBeanName(), obj);

                for (SbitTextTokenizer.Token tok : loop.getLoopTokens()) {
                    processToken(tok, outText, dataCopy);
                }
            }

        } else {
            // TEXT Token
            outText.append(token.getText());
        }
    }

    /**
     *
     * @param expression a Sbet expression (~ BeanUtils expression), without the enclosing curly braces
     * @param data the data to use to evaluate the expression
     * @return the evaluated expression, or an empty String if there's a null value along the path.
     */
    public String evaluateExpression(String expression, Map<String, Object> data) {

        Object bean = evaluateBean(expression, data);

        if (bean == null) {
            return getNullResult();
        }

        return bean.toString();
    }

    public Object evaluateBean(String expression, Map<String, Object> data) {
        if (StringUtils.isBlank(expression)) {
            return null;
        }

        expression = expression.trim();

        String[] elements = StringUtils.split(expression, '.');

        // elements[0] is special as it's always an element of the data Map<>.
        final String key = getLeadingText(elements[0]);
        Object bean = data.get(key);

        if (bean == null) {
            return null;
        }

        elements[0] = elements[0].substring(key.length());

        List<AtomicExpression> atomicExprs = extractAtomicExpressions(key, new BeanEvaluator(data), elements);

        for (AtomicExpression atomicExpr : atomicExprs) {
            bean = atomicExpr.resolve(bean);
            if (bean == null) {
                return null;
            }
        }

        return bean;
    }
}
