package lib.sbite.parser;

/**
 * Represents one token read while going through a Sbit template.
 */
public class Token {

    public Token(TokenType type, String text) {
        this.type = type;
        this.text = text;
    }

    private TokenType type;
    protected String text;

    public TokenType getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public enum TokenType {TEXT, EXPRESSION, LOOP};
}
