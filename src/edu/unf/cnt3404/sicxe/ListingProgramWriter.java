package edu.unf.cnt3404.sicxe;

import java.io.PrintWriter;

import edu.unf.cnt3404.sicxe.syntax.Command;
import edu.unf.cnt3404.sicxe.syntax.Program;
import edu.unf.cnt3404.sicxe.syntax.command.Comment;
import edu.unf.cnt3404.sicxe.syntax.command.WriteableCommand;

//Prints a listing file to a PrintWriter
public class ListingProgramWriter {
	
	private Program program;
	private Alignment align;
	private PrintWriter out;
	
	public ListingProgramWriter(Program program, Alignment align, PrintWriter out) {
		this.program = program;
		this.align = align;
		this.out = out;
	}
	
	//Appends the given command to the listing
	public void write(Command c) {
		writeColumn(align.getMaxLineLength(), c.getLine());
		out.printf("%04X    ", program.getLocationCounter()); //4 spaces 
		//Only non-comments print a label field
		if (c instanceof Comment) {
			out.println("." + c.getComment());
		} else {
			writeColumn(align.getMaxLabelLength(), c.getLabel());
			writeColumn(align.getMaxNameLength(), c.getName());
			writeColumn(align.getMaxArgumentLength(), c.getArgument());
			if (c instanceof WriteableCommand) {
				out.println(((WriteableCommand) c).getHexObjectCode());
			}
		}
		
	}
	
	private void writeColumn(int width, int i) {
		String format = "%" + width + "d    "; //4 spaces
		out.printf(format, i);
	}
	
	private void writeColumn(int width, String s) {
		String format = "%-" + width + "s "; //1 space
		if (s == null) s = ""; //Don't print null
		out.printf(format, s);
	}
}