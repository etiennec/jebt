package lib.jebt.parser;

/**
 * Represents one token read while going through a Jebt template.
 */
public class Token {

    public static Token EOD = new Token();

    public Token(TokenType type, String text) {

        if (type == TokenType.END_OF_DOCUMENT) {
            throw new RuntimeException("Don't instantiate END_OF_DOCUMENT tokens, use static Token.EOD instead.");
        }

        this.type = type;
        this.text = text;
    }

    private Token() {
        this.type = TokenType.END_OF_DOCUMENT;
        this.text = null;
    }

    private TokenType type;
    protected String text;

    public TokenType getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public enum TokenType {TEXT, EXPRESSION, LOOP, NEW_CELL, NEW_ROW, END_OF_DOCUMENT}
}
