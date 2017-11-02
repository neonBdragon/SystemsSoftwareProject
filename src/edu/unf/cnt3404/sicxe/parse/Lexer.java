package edu.unf.cnt3404.sicxe.parse;

import java.io.BufferedReader;

import edu.unf.cnt3404.sicxe.parse.Token.Type;

//Reads many characters at a time and creates tokens
public class Lexer {
	
	//Creates a lexer from a buffered reader. The reader can be from a file
	//Or from the standard in, etc.
	public Lexer(BufferedReader reader) {
		
	}

	//Gets the next token and consumes it.
	//Throws an exception if there is no available token.
	//Never returns null
	public Token next() {
		throw new RuntimeException();
	}
	
	//Gets the next token but doesn't consume it
	//Returns null if there are no more tokens
	public Token peek() {
		return null;
	}
	
	//Returns whether the next token is simple and matches the given character
	//If this method returns true, then the token is consumed.
	public boolean accept(char c) {
		Token token = peek();
		boolean result = token != null && token.getType() == Type.SIMPLE
			&& token.is(c);
		if (result) {
			next();
		}
		return result;
	}
	
	//Return whether the next token is of the given type and returns the token if possible
	//If this method returns null, then the token was not of the given type
	public Token accept(Type type) {
		Token result = peek();
		if (result.getType() == type) {
			next();
			return result;
		}
		return null;
	}
	
	//Parses the next token and returns, or throws an exception if the next token was not
	//a simple token, or did not match the given character.
	public void expect(char c) {
		Token token = next();
		if (token.getType() != Type.SIMPLE || !token.is(c)) {
			throw new ParseError(token, "Expected " + c + " not " + token);
		}
	}
	
	//Parses the next token and returns it, or throws an exception if the next token was 
	//not of the given type
	public Token expect(Type type) {
		Token result = next();
		if (result.getType() != type) {
			throw new ParseError(result, "Expected " + type + " not " + result);
		}
		return result;
	}
}
