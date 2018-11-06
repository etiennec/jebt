package lib.jebt.parser;

import org.json.simple.JSONArray;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class in charge of processing raw text with Jebt Template elements in it to turned it into filled text document
 */
public class JebtWriterTextProcessor extends JebtCommonTextProcessor {

    /**
     * @param sourceText A reader that reads template text.
     * @param outText A writer where we will write down the source text with all templating elements resolved.
     * @param data Data to use when filling template elements.
     */
    public void convertString(Reader sourceText, Writer outText, Map data) {

        JebtTextTokenizer tokenizer = new JebtTextTokenizer(sourceText);

        Token token;

        try {
            while ((token = tokenizer.readNext()) != Token.EOD) {
                processToken(token, outText, data);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void processToken(Token token, Writer outText, Map data) throws IOException{
        if (token.getType() == Token.TokenType.EXPRESSION) {
            outText.append(new JsonPathResolver(data).evaluatePathToString(token.getText()));
        } else if (token.getType() == Token.TokenType.LOOP) {
            JebtTextTokenizer.LoopToken loop = (JebtTextTokenizer.LoopToken)token;
            JSONArray collection = null;
            try {
                collection = (JSONArray)new JsonPathResolver(data).evaluatePathToObject(loop.getCollectionJsonPath());
            } catch (ClassCastException e) {
                throw new JebtEvaluationException("Object found at "+loop.getCollectionJsonPath()+" is not a JsonArray", e);
            }

            if (collection == null) {
                return;
            }
            Iterator it = collection.iterator();

            while (it.hasNext()) {
                Object obj = it.next();

                Map dataCopy = new LinkedHashMap(data);
                dataCopy.put(loop.getLoopItemName(), obj);

                for (Token tok : loop.getLoopTokens()) {
                    processToken(tok, outText, dataCopy);
                }
            }

        } else {
            // TEXT Token
            outText.append(token.getText());
        }
    }


}
