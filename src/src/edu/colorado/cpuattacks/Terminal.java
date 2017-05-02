package edu.colorado.cpuattacks;

import edu.colorado.cpuattacks.AST.ASTTypes;

public class Terminal extends Identifier {
	public Terminal (String name, int weight){
		super(name, weight, ASTTypes.Term);
	}
}