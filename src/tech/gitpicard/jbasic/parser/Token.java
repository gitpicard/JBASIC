package tech.gitpicard.jbasic.parser;

public final class Token {
	
	private TokenType type;
	private String fileName;
	private int line;
	private int column;
	private String content;
	
	public Token(TokenType t, String name, int ln, int col, String s) {
		type = t;
		fileName = name;
		line = ln;
		column = col;
		content = s;
	}
	
	public TokenType getType() {
		return type;
	}
	
	public String getSourceName() {
		return fileName;
	}
	
	public int getSourceLine() {
		return line;
	}
	
	public int getSourceColumn() {
		return column;
	}
	
	public String getContent() {
		return content;
	}
}
