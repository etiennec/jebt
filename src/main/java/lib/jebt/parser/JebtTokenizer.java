package lib.jebt.parser;

/**
 * Reads next token (text or expression)
 */
public interface JebtTokenizer {
    Token readNext();
}
