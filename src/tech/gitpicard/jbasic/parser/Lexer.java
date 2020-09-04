package tech.gitpicard.jbasic.parser;

import java.util.Stack;

import tech.gitpicard.jbasic.IllegalOperationException;
import tech.gitpicard.jbasic.SyntaxException;

/**
 * Used to extract a stream of tokens from incoming source code. Tokens
 * are parsed without context and are used in the first step in transforming
 * plain text into an abstract syntax tree. The lexer recognizes the strings,
 * identifiers, keywords, literals, etc. in the source code which is used by the
 * parser to build a tree.
 */
public final class Lexer {
	private boolean running;
	private String sourceName;
	private Stack<Character> source;
	private int line;
	private int column;
	
	/**
	 * Create a new lexer with no source code.
	 */
	public Lexer() {
		running = false;
	}
	
	private Token consumeWhitespace() {
		// Parse any white space that could be waiting here.
		while (!source.empty() && Character.isWhitespace(source.peek())) {
			char c = source.pop();
			column++;
			// Keep track of where we are when we hit a new line.
			if (c == '\n') {
				line++;
				column = 1;
				return new Token(TokenType.NEW_LINE, sourceName, line - 1, column, "\n");
			}
		}
		
		return null;
	}
	
	/**
	 * Starts parsing source code for tokens. Any source code
	 * left over from previous parsing will be discarded. The
	 * source code is transformed into a stack that will be
	 * iterated character by character for tokens.
	 * @param name The name of the unit of source code to lex.
	 * @param src The source code to lex.
	 */
	public void start(String name, String src) {
		running = true;
		sourceName = name;
		source = new Stack<>();
		line = 1;
		column = 1;
		
		// Push the source code onto the stack so that we can
		// go through the characters and sequentially parse tokens
		// from it. We will need to push them on in reverse since
		// a stack is a first-in last-out data structure.
		for (int i = src.length() - 1; i >= 0; i--)
			source.push(src.charAt(i));
	}
	
	/**
	 * Attempt to read the next token from the source code. This will
	 * consume the characters for that token and move to the next token
	 * in the source code. If a syntax error is encountered, the lexer
	 * will attempt to advance past the error to keep lexing. This will
	 * allow for multiple syntax errors to be found rather than just one.
	 * One the end of the code is reached, a EOF token is returned.
	 * @return The next token in the source code.
	 * @throws SyntaxException When a syntax error is encountered.
	 */
	public Token next() throws SyntaxException {
		if (!running)
			throw new IllegalOperationException("lexer has no source code to tokenize");
		
		Token t = consumeWhitespace();
		if (t != null)
			return t;
		
		// Check if we are finished.
		if (source.empty()) {
			running = false;
			return new Token(TokenType.EOF, sourceName, line, column, "");
		}
		
		// Consume a comment if there is one chilling here.
		if (source.peek() == '\'') {
			while (!source.empty() && source.peek() != '\n') {
				source.pop();
				column++;
			}
		}
		
		t = consumeWhitespace();
		if (t != null)
			return t;
		
		// Check if we are finished.
		if (source.empty()) {
			running = false;
			return new Token(TokenType.EOF, sourceName, line, column, "");
		}		
		
		if (Character.isDigit(source.peek())) {
			// We have found a number literal. It could still be a integer
			// or a floating pointer number.
			int start = column;
			StringBuilder buffer = new StringBuilder();
			boolean isInt = true;	// Keeps track of if we have hit a period yet or not.
			
			while (!source.empty() && (Character.isDigit(source.peek()) || (isInt && source.peek() == '.'))) {
				column++;
				buffer.append(source.peek());
				if (source.pop() == '.')
					isInt = false;
			}
			
			return new Token(isInt ? TokenType.INT_LITERAL : TokenType.REAL_LITERAL, sourceName, line, start, buffer.toString());
		}
		else if (source.peek() == '"') {
			// Move past the opening quotes since they are not apart of the content.
			int start = column++;
			source.pop();
			StringBuilder buffer = new StringBuilder();
			
			while (true) {
				// We hit the end of the string without there being an end quote
				// so we will trigger a syntax error.
				if (source.empty())
					throw new SyntaxException(sourceName, "expected \"", line, column);
				char c = source.pop();
				// We also don't allow new lines without a ending quote.
				if (c == '\n') {
					line++;
					column = 1;
					throw new SyntaxException(sourceName, "expected \"", line, column);
				}
				
				column++;
				
				// Handle the end of string logic.
				if (c == '"')
					break;
				else if (c == '\\') {
					// Handle the logic for string escape sequences.
					if (source.empty())
						throw new SyntaxException(sourceName, "expected an escape character", line, column);
					char next = source.pop();
					column++;
					
					// Insert the correct ASCII character for the escape code that we found
					// in the source.
					switch (next) {
					case 'n':
						buffer.append('\n');
						break;
					case 'r':
						buffer.append('\r');
						break;
					case 't':
						buffer.append('\t');
						break;
					case '\\':
						buffer.append('\\');
						break;
					case '"':
						buffer.append('"');
						break;
					case '\n':
						// New lines are not allowed which is why they fall through
						// to the default error case but we need special logic here to
						// make sure that we keep a correct line numbering count.
						line++;
						column = 1;
					default:
						String msg = "unexpected escape character" + next;
						throw new SyntaxException(sourceName, msg, line, column);
					}
				}
				else {
					// The character is a part of the string literal so add it in.
					buffer.append(c);
				}
			}
			
			return new Token(TokenType.STR_LITERAL, sourceName, line, start, buffer.toString());
		}
		else if (Character.isLetter(source.peek())) {
			// This indicates an identifier or a keyword.
			int start = column;
			StringBuilder buffer = new StringBuilder();
			
			while (!source.empty()) {
				char c = source.peek();
				if (Character.isLetterOrDigit(c) || c == '_') {
					buffer.append(source.pop());
					column++;
				}
				// What we have no longer matches the identifier or keyword
				// rules so leave the loop and don't consume that character.
				else
					break;
			}
			
			// Check to see if what we have is a keyword or constant value.
			String str = buffer.toString();
			
			if (str.equalsIgnoreCase("true"))
				return new Token(TokenType.TRUE_LITERAL, sourceName, line, start, "TRUE");
			else if (str.equalsIgnoreCase("false"))
				return new Token(TokenType.FALSE_LITERAL, sourceName, line, start, "FALSE");
			else if (str.equalsIgnoreCase("null"))
				return new Token(TokenType.NULL_LITERAL, sourceName, line, start, "NULL");
			// Handle all the keywords...
			else if (str.equalsIgnoreCase("if"))
				return new Token(TokenType.IF, sourceName, line, start, "IF");
			else if (str.equalsIgnoreCase("then"))
				return new Token(TokenType.THEN, sourceName, line, start, "THEN");
			else if (str.equalsIgnoreCase("else"))
				return new Token(TokenType.ELSE, sourceName, line, start, "ELSE");
			else if (str.equalsIgnoreCase("elseif"))
				return new Token(TokenType.ELSEIF, sourceName, line, start, "ELSEIF");
			else if (str.equalsIgnoreCase("end"))
				return new Token(TokenType.END, sourceName, line, start, "END");
			else if (str.equalsIgnoreCase("while"))
				return new Token(TokenType.WHILE, sourceName, line, start, "WHILE");
			else if (str.equalsIgnoreCase("do"))
				return new Token(TokenType.DO, sourceName, line, start, "DO");
			else if (str.equalsIgnoreCase("for"))
				return new Token(TokenType.FOR, sourceName, line, start, "FOR");
			else if (str.equalsIgnoreCase("in"))
				return new Token(TokenType.IN, sourceName, line, start, "IN");
			else if (str.equalsIgnoreCase("continue"))
				return new Token(TokenType.CONTINUE, sourceName, line, start, "CONTINUE");
			else if (str.equalsIgnoreCase("break"))
				return new Token(TokenType.BREAK, sourceName, line, start, "BREAK");
			else if (str.equalsIgnoreCase("return"))
				return new Token(TokenType.RETURN, sourceName, line, start, "RETURN");
			else if (str.equalsIgnoreCase("function"))
				return new Token(TokenType.FUNCTION, sourceName, line, start, "FUNCTION");
			else if (str.equalsIgnoreCase("import"))
				return new Token(TokenType.IMPORT, sourceName, line, start, "IMPORT");
			else if (str.equalsIgnoreCase("let"))
				return new Token(TokenType.LET, sourceName, line, start, "LET");
			else if (str.equalsIgnoreCase("call"))
				return new Token(TokenType.CALL, sourceName, line, start, "CALL");
			else if (str.equalsIgnoreCase("new"))
				return new Token(TokenType.NEW, sourceName, line, start, "NEW");
			else if (str.equalsIgnoreCase("class"))
				return new Token(TokenType.CLASS, sourceName, line, start, "CLASS");
			else if (str.equalsIgnoreCase("private"))
				return new Token(TokenType.PRIVATE, sourceName, line, start, "PRIVATE");
			else if (str.equalsIgnoreCase("protected"))
				return new Token(TokenType.PROTECTED, sourceName, line, start, "PROTECTED");
			else if (str.equalsIgnoreCase("public"))
				return new Token(TokenType.PUBLIC, sourceName, line, start, "PUBLIC");
			else if (str.equalsIgnoreCase("extends"))
				return new Token(TokenType.EXTENDS, sourceName, line, start, "EXTENDS");
			else if (str.equalsIgnoreCase("super"))
				return new Token(TokenType.SUPER, sourceName, line, start, "SUPER");
			else if (str.equalsIgnoreCase("self"))
				return new Token(TokenType.SELF, sourceName, line, start, "SELF");
			else if (str.equalsIgnoreCase("and"))
				return new Token(TokenType.AND, sourceName, line, start, "AND");
			else if (str.equalsIgnoreCase("or"))
				return new Token(TokenType.OR, sourceName, line, start, "OR");
			else if (str.equalsIgnoreCase("not"))
				return new Token(TokenType.NOT, sourceName, line, start, "NOT");
			
			return new Token(TokenType.IDENTIFIER, sourceName, line, start, str);
		}
		else {
			// Test for special characters. We can go ahead and pop the character
			// from the stack because if we don't use it here, then we where unable
			// to identify it and it will be an error that gets discarded anyways.
			char c = source.pop();
			column++;
			
			switch (c) {
			case '.':
				return new Token(TokenType.DOT, sourceName, line, column - 1, ".");
			case '+':
				return new Token(TokenType.PLUS, sourceName, line, column - 1, "+");
			case '-':
				return new Token(TokenType.MINUS, sourceName, line, column - 1, "-");
			case '*':
				return new Token(TokenType.STAR, sourceName, line, column - 1, "*");
			case '/':
				return new Token(TokenType.SLASH, sourceName, line, column - 1, "/");
			case '=':
				return new Token(TokenType.EQUALS, sourceName, line, column - 1, "=");
			case '<':
				return new Token(TokenType.LESS_THAN, sourceName, line, column - 1, "<");
			case '>':
				return new Token(TokenType.GREATER_THAN, sourceName, line, column - 1, ">");
			case ',':
				return new Token(TokenType.COMMA, sourceName, line, column - 1, ",");
			case '(':
				return new Token(TokenType.LEFT_PARAN, sourceName, line, column - 1, "(");
			case ')':
				return new Token(TokenType.RIGHT_PARAN, sourceName, line, column - 1, ")");
			default:
				// If we make it here, that means we where unable to identify what kind
				// of token we have. We will throw an exception and consume the token. This
				// is done so that lexing can continue after hitting an error. This lets the
				// interpreter report all the syntax errors rather than just the one that stopped
				// the program.
				String msg = "unexpected " + c;
				throw new SyntaxException(sourceName, msg, line, column);
			}
		}
	}
	
	/**
	 * Does the lexer still have source code left to tokenize?
	 * @return True if there is still unlexed code.
	 */
	public boolean isLexing() {
		return running;
	}
}
