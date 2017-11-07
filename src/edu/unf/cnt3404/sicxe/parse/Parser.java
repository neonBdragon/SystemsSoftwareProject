package edu.unf.cnt3404.sicxe.parse;

import java.util.Stack;

import edu.unf.cnt3404.sicxe.global.Format;
import edu.unf.cnt3404.sicxe.global.Mnemonic;
import edu.unf.cnt3404.sicxe.syntax.Command;
import edu.unf.cnt3404.sicxe.syntax.Expression;
import edu.unf.cnt3404.sicxe.syntax.command.Comment;
import edu.unf.cnt3404.sicxe.syntax.command.instruction.Format1Instruction;
import edu.unf.cnt3404.sicxe.syntax.command.instruction.Format2Instruction;
import edu.unf.cnt3404.sicxe.syntax.command.instruction.Format34Instruction;
import edu.unf.cnt3404.sicxe.syntax.command.instruction.TargetMode;
import edu.unf.cnt3404.sicxe.syntax.expression.ExpressionNode;
import edu.unf.cnt3404.sicxe.syntax.expression.ExpressionNumber;
import edu.unf.cnt3404.sicxe.syntax.expression.ExpressionOperator;
import edu.unf.cnt3404.sicxe.syntax.expression.ExpressionStar;
import edu.unf.cnt3404.sicxe.syntax.expression.ExpressionSymbol;

//Calls the lexer repeatedly in order to create Commands.
public class Parser {
	
	private Lexer lexer;
	
	//Creates a parser from the reader
	public Parser(Lexer lexer) {
		this.lexer = lexer;
	}
	
	//Returns the next command from the source, or, null if there
	//are no commands left and the parsing is finished
	public Command next() {
		//Advance to the beginning of the next useful token
		while (lexer.accept(Token.Type.WHITESPACE) != null
			|| lexer.accept(Token.Type.NEWLINE) != null);
	
		Command result;
		
		//Then, get the line number
		int line = lexer.getRow();
		
		//Comment only on this line
		Token token = lexer.accept(Token.Type.COMMENT);
		if (token != null) {
			result = new Comment();
			result.setComment(token.asComment());
		} else {

			//See if there's a label
			String label = null;
			token = lexer.accept(Token.Type.SYMBOL);
			if (token != null) {
				label = token.asSymbol();
				lexer.expect(Token.Type.WHITESPACE);
			}
			
			//If it's extended...
			boolean extended = lexer.accept('+');
			
			//Get the mnemonic
			Mnemonic mnemonic = lexer.expect(Token.Type.MNEMONIC).asMnemonic();
			
			//Ensure that the source only extends a F34 mnemonic
			//This implies someone can extend an RSUB (The only F34 mnemonic)
			if (extended && mnemonic.getFormat() != Format.FORMAT34 
				&& mnemonic.getFormat() != Format.FORMAT34M) {
				throw new ParseError(token, "Cannot extend mnemonic " + token);
			}
			
			//Parse the rest of it, which depends on the Mnemonic itself
			switch(mnemonic.getFormat()) {
			case FORMAT1: result = new Format1Instruction(mnemonic); break;
			case FORMAT2N: result = parseFormat2NCommand(mnemonic); break;
			case FORMAT2R: result = parseFormat2RCommand(mnemonic); break;
			case FORMAT2RN: result = parseFormat2RNCommand(mnemonic); break;
			case FORMAT2RR: result = parseFormat2RRCommand(mnemonic); break;
			case FORMAT34: result = new Format34Instruction(extended, mnemonic); break;
			case FORMAT34M: result = parseFormat34MCommand(extended, mnemonic); break;
			default: throw new IllegalStateException(mnemonic.toString());
			}
			
			result.setLabel(label);
		}
		//Get all whitespace on this line
		while (lexer.accept(Token.Type.WHITESPACE) != null);
		//Make sure there's a new line
		lexer.expect(Token.Type.NEWLINE);
		result.setLine(line);
		return result;
	}
	
	//Precondition: mnemonic.getFormat() == Format.FORMAT2N
	//Likewise for related parse methods
	private Command parseFormat2NCommand(Mnemonic mnemonic) {
		lexer.expect(Token.Type.WHITESPACE);
		int n = lexer.expect(Token.Type.NUMBER).asNumber();
		return new Format2Instruction(mnemonic, (byte)n);
	}
	
	private Command parseFormat2RCommand(Mnemonic mnemonic) {
		lexer.expect(Token.Type.WHITESPACE);
		String r = lexer.expect(Token.Type.SYMBOL).asSymbol();
		return new Format2Instruction(mnemonic, r);
	}
	
	private Command parseFormat2RRCommand(Mnemonic mnemonic) {
		lexer.expect(Token.Type.WHITESPACE);
		
		String r1 = lexer.expect(Token.Type.SYMBOL).asSymbol();
		lexer.expect(',');
		String r2 = lexer.expect(Token.Type.SYMBOL).asSymbol();
		
		return new Format2Instruction(mnemonic, r1, r2);
	}
	
	private Command parseFormat2RNCommand(Mnemonic mnemonic) {
		lexer.expect(Token.Type.WHITESPACE);
		String r = lexer.expect(Token.Type.SYMBOL).asSymbol();
		lexer.expect(',');
		int n = lexer.expect(Token.Type.NUMBER).asNumber();
		return new Format2Instruction(mnemonic, r, (byte)n);
	}
	
	private Command parseFormat34MCommand(boolean extended, Mnemonic mnemonic) {
		lexer.expect(Token.Type.WHITESPACE);
		
		//Figure out what target mode
		TargetMode target;
		if (lexer.accept('#')) {
			target = TargetMode.IMMEDIATE;
		} else if (lexer.accept('@')) {
			target = TargetMode.INDIRECT;
		} else {
			//XE Assembler NEVER uses SIMPLE, because that is ni = 00.
			target = TargetMode.SIMPLE_XE;
		}
		
		//Get the operand
		Expression expr = parseExpression();
		
		boolean indexed = false;
		//See if it's indexed or not
		if (lexer.accept(',')) {
			Token x = lexer.expect(Token.Type.SYMBOL);
			if (!x.asSymbol().equals("X")) {
				throw new ParseError(x, "Expected X not " + x.asSymbol());
			}
			indexed = true;
		}
		
		return new Format34Instruction(extended, mnemonic, target, expr, indexed);
	}
	
	//Attempts to parse an expression. Will stop parsing when the lexer reaches a newline
	//token, a comment token, or an illegal token. Throws an exception if an illegal token 
	//is reached. Implements Dijkstra's Shunting-Yard Algorithm to turn an infix
	//arithmetic expression into a syntax tree
	public Expression parseExpression() {
		Stack<ExpressionNode> nodes = new Stack<>();
		//Null element in operators indicates parentheses
		Stack<ExpressionOperator.Type> operators = new Stack<>();
		Token token;
		//This flag toggles between tokens. It is required to distinguish star (*) as a
		//multiplication operator versus as a location counter operand.
		boolean expectsOperator = false; //Initially expect a value
		//While there are tokens to be read
		while ((token = lexer.peek()) != null) {
			//System.out.println("OPERATORS: " + operators);
			//System.out.println("OPERANDS: " + nodes);
			if (lexer.accept(Token.Type.WHITESPACE) != null) {
				continue;
			}
			if (expectsOperator) {
				ExpressionOperator.Type operator;
				//Read the operator
				if (lexer.accept('+')) {
					operator = ExpressionOperator.Type.ADD;
				} else if (lexer.accept('-')) {
					operator = ExpressionOperator.Type.SUB;
				} else if (lexer.accept('*')) {
					operator = ExpressionOperator.Type.MUL;
				} else if (lexer.accept('/')) {
					operator = ExpressionOperator.Type.DIV;
				} else if (lexer.accept(')')) {
					//Pop while the operator is not a parentheses (null)
					while (!operators.isEmpty() && operators.peek() != null) {
						//nodes stack reverses order of the nodes, so right pops off first
						ExpressionNode right = nodes.pop();
						ExpressionNode left = nodes.pop();
						nodes.add(new ExpressionOperator(operators.pop(), left, right));
					}
					if (operators.isEmpty()) {
						//Therefore, peek() never returned null, and there was no opening parentheses
						throw new ParseError(token, "Did not expect )");
					}
					//Otherwise, peek() returned null, so pop it
					operators.pop();
					continue; //<-- This is so bad; this is because no operator was read
					//in this branch, so 1) I cannot push an operator onto the stack, and
					//2) I should not toggle expectsOperator
				} else {
					//throw new ParseError(token, "Expected operator not " + token);
					break;
				}
				//Output higher precedence operators into the tree
				//If the stack top is ever null, then it is a parentheses on the stack.
				//Wikipedia did not seem to say what to do, or if this scenario is possible.
				while (!operators.isEmpty() && operators.peek() != null && 
						operators.peek().getPrecedence() >= operator.getPrecedence()) {
					ExpressionNode right = nodes.pop();
					ExpressionNode left = nodes.pop();
					nodes.add(new ExpressionOperator(operators.pop(), left, right));
				}
				operators.push(operator);
			} else { //If the parser expects an operand
				if (lexer.accept(Token.Type.SYMBOL) != null) {
					nodes.add(new ExpressionSymbol(token.asSymbol()));
				} else if (lexer.accept(Token.Type.NUMBER) != null) {
					nodes.add(new ExpressionNumber(token.asNumber()));
				} else if (lexer.accept('*')) {
					nodes.add(new ExpressionStar());
				} else if (lexer.accept('(')) {
					operators.push(null);
					continue; //Continue so that I do not toggle expectsOperator
				} else {
					//throw new ParseError(token, "Expected operand not " + token);
					break;
				}
			}
			expectsOperator = !expectsOperator;
		}
		//Pop all excess operators from the stack
		while(!operators.isEmpty()) {
			ExpressionOperator.Type operator = operators.pop();
			//If a parentheses was found on the stack
			if (operator == null) {
				//Token is always null at this point
				throw new ParseError(token, "Unbalanced parentheses" + token);
			}
			//Ensure left is the first dequeue
			//Pop operator into nodes
			//nodes stack reverses order of the nodes, so right pops off first
			ExpressionNode right = nodes.pop();
			ExpressionNode left = nodes.pop();
			nodes.add(new ExpressionOperator(operator, left, right));
		}
		return new Expression(nodes.pop());
	}
}
