package edu.unf.cnt3404.sicxe.syntax;

//Represents a line of useful code in a SicXe program.
//A command is created when the source is parsed, and the location counter
//must be incremented. A command might have a corresponding label definition
//or comment.
public interface Command {
	//Returns the number of bytes that this command would take up 
	//in the object program. 
	//Alternatively, the amount by which to increment location counter
	int getSize();
	
	void setLabel(String label);
	//Gets the label for this command, or null if there is no label
	String getLabel();
	
	void setComment(String comment);
	//Gets the comment for this command, or null if there is no comment
	String getComment();
}
