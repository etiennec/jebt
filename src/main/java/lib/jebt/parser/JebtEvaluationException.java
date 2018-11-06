package lib.jebt.parser;

/**
 * Created by canaud on 10/22/2016.
 */
public class JebtEvaluationException extends RuntimeException {
    public JebtEvaluationException(String message) {
        super(message);
    }

    public JebtEvaluationException(String message, Throwable cause) {
        super(message, cause);
    }
}
