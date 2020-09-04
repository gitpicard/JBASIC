package tech.gitpicard.jbasic;

public class SyntaxException extends Exception {
	private String fileName;
	private int line;
	private int column;
	
	public SyntaxException(String name, String message, int ln, int col) {
		super(message);
		
		fileName = name;
		line = ln;
		column = col;
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
	
	private static final long serialVersionUID = -8952661043245572315L;

}
