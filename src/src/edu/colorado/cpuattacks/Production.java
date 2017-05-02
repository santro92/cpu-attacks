package edu.colorado.cpuattacks;

import java.util.LinkedList;

public class Production extends AST {
	public  int                     ID;
	private Nonterminal 		   lhs;
	private LinkedList<Identifier> rhs;
	private int    			  capacity;
	
	public Production () { this.ID = -1; this.lhs = null; this.rhs = null; this.capacity = -1; }
	public Production (int id, Nonterminal lhs, LinkedList<Identifier> rhs, int capacity){
		this.ID  = id;
		this.lhs = lhs;
		this.rhs = rhs;
		this.capacity = capacity;
	}
	
	public int getID() {
		return this.ID;
	}
	
	public Nonterminal getLHS() {
		return this.lhs;
	}
	
	public LinkedList<Identifier> getRHS() {
		return this.rhs;
	}
	
	public int getCapacity() {
		return this.capacity;
	}
	
	public void setLHS(Nonterminal lhs) {
		this.lhs = lhs;
	}
	
	public String toString(){
		return "Production("+ "ID: " + this.ID + "," 
							+ ((lhs!=null)? lhs : "---") + ","
				 			+ rhs.toString() + ","
				 			+ capacity + ")";
	}
	
	public String getRHSString(){
		String rhsString = "";
		for (Identifier id : this.rhs){
			rhsString = rhsString+id.getName();
		}
		return rhsString;
		//this.rhs.forEach(id -> {rhsString = rhsString+ "" + id.toString();});
	}
	
	public String getLHSString(){
		return this.lhs.getName();
	}
}
