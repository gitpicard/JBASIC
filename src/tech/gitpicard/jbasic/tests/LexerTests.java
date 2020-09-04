package tech.gitpicard.jbasic.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedList;

import org.junit.jupiter.api.Test;

import tech.gitpicard.jbasic.SyntaxException;
import tech.gitpicard.jbasic.parser.Lexer;
import tech.gitpicard.jbasic.parser.Token;
import tech.gitpicard.jbasic.parser.TokenType;

class LexerTests {
	private void expect(String source, Token[] expected, boolean ignoreLoc) throws SyntaxException {
		Lexer lexer = new Lexer();
		LinkedList<Token> tokens = new LinkedList<>();
		
		lexer.start("unitTest", source);
		
		while (true) {
			Token t = lexer.next();
			tokens.add(t);
			if (t.getType() == TokenType.EOF)
				break;
		}
		
		// Make sure that we got the correct tokens.
		assertEquals(expected.length, tokens.size());
		
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i].getType(), tokens.get(i).getType());
			assertEquals(expected[i].getSourceName(), tokens.get(i).getSourceName());
			// We don't always want to have to keep track of where in the source code we are,
			// so we can turn off the testing for that.
			if (!ignoreLoc) {
				assertEquals(expected[i].getSourceLine(), tokens.get(i).getSourceLine());
				assertEquals(expected[i].getSourceColumn(), tokens.get(i).getSourceColumn());
			}
			assertTrue(expected[i].getContent().equals(tokens.get(i).getContent()));
		}
	}
	
	@Test
	public void testEof() throws SyntaxException {
		Token[] expected = {
				new Token(TokenType.EOF, "unitTest", 1, 1, "")
		};
		expect("", expected, true);
	}
	
	@Test
	public void testBadCharacter() {
		assertThrows(SyntaxException.class, () -> {
			Lexer lex = new Lexer();
			lex.start("unitTest", "{");
			lex.next();
		});
	}
	
	@Test
	public void testWhitespace() throws SyntaxException {
		Token[] expected = {
				new Token(TokenType.EOF, "unitTest", 1, 3, "")
		};
		expect("  ", expected, false);
		expect("\t", expected, true);
		expect(" \t ", expected, true);
	}
	
	@Test
	public void testInt() throws SyntaxException {
		Token[] expected = {
				new Token(TokenType.INT_LITERAL, "unitTest", 1, 1, "5"),
				new Token(TokenType.EOF, "unitTest", 1, 1, "")
		};
		expect("5", expected, true);
		expect(" 5", expected, true);
		expect("\t 5", expected, true);
		
		Token[] expected2 = {
				new Token(TokenType.INT_LITERAL, "unitTest", 1, 1, "893252"),
				new Token(TokenType.EOF, "unitTest", 1, 1, "")
		};
		expect(" 893252", expected2, true);
		
		Token[] expected3 = {
				new Token(TokenType.INT_LITERAL, "unitTest", 1, 1, "67"),
				new Token(TokenType.INT_LITERAL, "unitTest", 1, 1, "345"),
				new Token(TokenType.EOF, "unitTest", 1, 1, "")
		};
		expect(" 67\t345", expected3, true);
	}
	
	@Test
	public void testReal() throws SyntaxException {
		Token[] expected = {
				new Token(TokenType.REAL_LITERAL, "unitTest", 1, 1, "5.3"),
				new Token(TokenType.EOF, "unitTest", 1, 1, "")
		};
		expect("5.3", expected, true);
		
		Token[] expected2 = {
				new Token(TokenType.REAL_LITERAL, "unitTest", 1, 1, "189.9"),
				new Token(TokenType.REAL_LITERAL, "unitTest", 1, 1, "0.0"),
				new Token(TokenType.EOF, "unitTest", 1, 1, "")
		};
		expect(" 189.9 \t\t 0.0 \t ", expected2, true);
	}
	
	@Test
	public void testString() throws SyntaxException {
		Token[] expected = {
				new Token(TokenType.STR_LITERAL, "unitTest", 1, 1, "hello world"),
				new Token(TokenType.EOF, "unitTest", 1, 1, "")
		};
		expect("\"hello world\"", expected, true);
		
		Token[] expected2 = {
				new Token(TokenType.STR_LITERAL, "unitTest", 1, 1, ""),
				new Token(TokenType.EOF, "unitTest", 1, 1, "")
		};
		expect("\"\"", expected2, true);
		
		Token[] expected3 = {
				new Token(TokenType.STR_LITERAL, "unitTest", 1, 1, "test"),
				new Token(TokenType.INT_LITERAL, "unitTest", 1, 1, "57"),
				new Token(TokenType.EOF, "unitTest", 1, 1, "")
		};
		expect("\"test\"57", expected3, true);
		
		// Make sure that bad strings are recognized as well.
		assertThrows(SyntaxException.class, () -> {
			Lexer lex = new Lexer();
			lex.start("unitTest", "\"test");
			lex.next();
		});
		assertThrows(SyntaxException.class, () -> {
			Lexer lex = new Lexer();
			lex.start("unitTest", "\"test\n");
			lex.next();
		});
	}
	
	@Test
	public void testStringEscapeSequences() throws SyntaxException {
		Token[] expected = {
				new Token(TokenType.STR_LITERAL, "unitTest", 1, 1, "hello\tworld"),
				new Token(TokenType.EOF, "unitTest", 1, 1, "")
		};
		expect("\"hello\\tworld\"", expected, true);
		
		assertThrows(SyntaxException.class, () -> {
			Lexer lex = new Lexer();
			lex.start("unitTest", "\"test\\ytest\"");
			lex.next();
		});
		
		assertThrows(SyntaxException.class, () -> {
			Lexer lex = new Lexer();
			lex.start("unitTest", "\"test\\\ntest\"");
			lex.next();
		});
	}
	
	@Test
	public void testTrueFalseNull() throws SyntaxException {
		Token[] expected = {
				new Token(TokenType.TRUE_LITERAL, "unitTest", 1, 1, "TRUE"),
				new Token(TokenType.EOF, "unitTest", 1, 1, "")
		};
		expect("true", expected, true);
		expect("tRue", expected, true);
		expect("TRUE", expected, true);
		
		Token[] expected1 = {
				new Token(TokenType.FALSE_LITERAL, "unitTest", 1, 1, "FALSE"),
				new Token(TokenType.EOF, "unitTest", 1, 1, "")
		};
		expect("false", expected1, true);
		expect("False", expected1, true);
		expect("FALSE", expected1, true);
		
		Token[] expected2 = {
				new Token(TokenType.NULL_LITERAL, "unitTest", 1, 1, "NULL"),
				new Token(TokenType.EOF, "unitTest", 1, 1, "")
		};
		expect("null", expected2, true);
		expect("Null", expected2, true);
		expect("NULL", expected2, true);
	}
	
	@Test
	public void testIdents() throws SyntaxException {
		Token[] expected = {
				new Token(TokenType.IDENTIFIER, "unitTest", 1, 1, "helloWorld"),
				new Token(TokenType.EOF, "unitTest", 1, 1, "")
		};
		expect("helloWorld", expected, true);
		
		Token[] expected1 = {
				new Token(TokenType.IDENTIFIER, "unitTest", 1, 1, "hello_world"),
				new Token(TokenType.EOF, "unitTest", 1, 1, "")
		};
		expect("hello_world", expected1, true);
		
		Token[] expected2 = {
				new Token(TokenType.IDENTIFIER, "unitTest", 1, 1, "hello10world"),
				new Token(TokenType.EOF, "unitTest", 1, 1, "")
		};
		expect("hello10world", expected2, true);
		
		Token[] expected3 = {
				new Token(TokenType.INT_LITERAL, "unitTest", 1, 1, "5"),
				new Token(TokenType.IDENTIFIER, "unitTest", 1, 1, "hello"),
				new Token(TokenType.EOF, "unitTest", 1, 1, "")
		};
		expect("5hello", expected3, true);
	}
	
	@Test
	public void testOp() throws SyntaxException {
		Token[] expected = {
			new Token(TokenType.PLUS, "unitTest", 1, 1, "+"),
			new Token(TokenType.MINUS, "unitTest", 1, 2, "-"),
			new Token(TokenType.MINUS, "unitTest", 1, 4, "-"),
			new Token(TokenType.EOF, "unitTest", 1, 5, ""),
		};
		expect("+- -", expected, false);
	}
	
	@Test
	public void testComments() throws SyntaxException {
		Token[] expected = {
				new Token(TokenType.EOF, "unitTest", 1, 1, "")
		};
		expect("' this is a comment", expected, true);
		
		Token[] expected1 = {
				new Token(TokenType.NEW_LINE, "unitTest", 1, 1, "\n"),
				new Token(TokenType.IDENTIFIER, "unitTest", 1, 1, "test"),
				new Token(TokenType.EOF, "unitTest", 1, 1, "")
		};
		expect("' comment \ntest", expected1, true);
	}
}
