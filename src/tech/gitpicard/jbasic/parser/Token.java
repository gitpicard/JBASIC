package tech.gitpicard.jbasic.parser;

/**
 * Stores information about a specific token. This class
 * stores the type of token but also where the token is
 * found so that useful error information can be preserved.
 */
public final class Token {
	private TokenType type;
	private String fileName;
	private int line;
	private int column;
	private String content;
	
	/**
	 * Creates a new token from the provided information.
	 * @param t The type of token that was found.
	 * @param name The name of the source code where the token is located.
	 * @param ln The line number that the token was on counting from one.
	 * @param col The column that the token is in on the line.
	 * @param s The characters making up the token.
	 */
	public Token(TokenType t, String name, int ln, int col, String s) {
		type = t;
		fileName = name;
		line = ln;
		column = col;
		content = s;
	}
	
	/**
	 * Get the type of token represented.
	 * @return Token type.
	 */
	public TokenType getType() {
		return type;
	}
	
	/**
	 * Get the name of the source code unit that the token
	 * was found in. Usually, this is a file name.
	 * @return The name of where the token is found.
	 */
	public String getSourceName() {
		return fileName;
	}
	
	/**
	 * The line number counting from one where the token is found on.
	 * @return The line number.
	 */
	public int getSourceLine() {
		return line;
	}
	
	/**
	 * The column on the line returned from getSourceLine() that the token is
	 * found in. The columns are counted from one.
	 * @return The column on the line.
	 */
	public int getSourceColumn() {
		return column;
	}
	
	public String getContent() {
		return content;
	}
}
