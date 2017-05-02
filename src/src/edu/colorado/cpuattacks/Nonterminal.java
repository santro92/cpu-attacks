package edu.colorado.cpuattacks;

public class Nonterminal extends Identifier {
	private int ID = -1;
	
	public Nonterminal (String name){
		super(name, 0, ASTTypes.Nonterm);
	}
	public Nonterminal (String name, int weight){
		super(name, weight, ASTTypes.Nonterm);
	}
	public int  getID(){
		return this.ID;
	}
	public void setID(int newID){
		this.ID = newID;
	}
	public String toString(){
		return "Nonterminal(ID:"+this.ID+","+this.name+","+this.cost+")";
	}
}