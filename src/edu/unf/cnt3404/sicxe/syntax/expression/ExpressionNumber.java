package edu.unf.cnt3404.sicxe.syntax.expression;

import java.util.List;

import edu.unf.cnt3404.sicxe.syntax.Program;

//Represents a constant numerical value expression
public class ExpressionNumber implements ExpressionNode {

	private int value;
	
	//Creates an expression with the given value
	public ExpressionNumber(int value) {
		this.value = value;
	}
	
	@Override
	public boolean isAbsolute(Program program) {
		//Numerical constants are always absolute
		return true;
	}

	@Override
	public int getValue(Program program) {
		return value;
	}

	@Override
	public void addAbsoluteSymbols(Program program, List<String> symbols) {
		//No symbols to add
	}

	@Override
	public String toString() {
		return Integer.toString(value);
	}
}