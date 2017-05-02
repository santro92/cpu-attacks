package edu.colorado.cpuattacks;

public class Identifier extends AST implements Comparable<Identifier>{
	public String name;
	public int cost;
	private ASTTypes type;
	
	public Identifier(String name, int weight){
		this.name = name;
		this.cost = weight;
		this.type = ASTTypes.Ident;
	}
	
	public Identifier(String name, int weight, ASTTypes typ){
		this.name = name;
		this.cost = weight;
		this.type = typ;
	}
	
	public String getName() {
		return this.name;
	}
	
	public int getCost() {
		return this.cost;
	}
	
	public String toString() {
		switch(this.type){
			case Term:
				return "Terminal("+this.name+","+this.cost+")";
			case Nonterm:
				return "Nonterminal("+this.name+","+this.cost+")";
			case Ident:
				return "Identifier("+this.name+","+this.cost+")";
			default:
				return "AST("+this.type+","+this.name+","+this.cost+")";
		}
	}

	@Override
	public int compareTo(Identifier id) {
		return this.name.compareTo(id.name);
	}
}