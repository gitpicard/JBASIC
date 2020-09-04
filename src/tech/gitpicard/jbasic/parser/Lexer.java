package tech.gitpicard.jbasic.parser;

import java.util.Stack;

import tech.gitpicard.jbasic.IllegalOperationException;
import tech.gitpicard.jbasic.SyntaxException;

public final class Lexer {
	
	private boolean running;
	private String sourceName;
	private Stack<Character> source;
	private int line;
	private int column;
	
	public Lexer() {
		running = false;
	}
	
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
	
	public Token next() throws SyntaxException {
		if (!running)
			throw new IllegalOperationException("lexer has no source code to tokenize");
		
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
			
			return new Token(TokenType.IDENTIFIER, sourceName, line, start, str);
		}
		else {
			// If we make it here, that means we where unable to identify what kind
			// of token we have. We will throw an exception and consume the token. This
			// is done so that lexing can continue after hitting an error. This lets the
			// interpreter report all the syntax errors rather than just the one that stopped
			// the program.
			String msg = "unexpected " + source.pop();
			throw new SyntaxException(sourceName, msg, line, column);
		}
	}
	
	public boolean isLexing() {
		return running;
	}
}
