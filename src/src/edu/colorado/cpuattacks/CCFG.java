package edu.colorado.cpuattacks;

import java.util.*;

public class CCFG extends AST{
	private Set<Terminal> 		    terminals;
	private Set<Nonterminal>	        nonterminals;
	private Nonterminal             start;
	private Collection<Production>  productions;
	private HashMap<String,Integer> costs;
	private HashMap<String,String>  replace;
	
	public CCFG(){ this.terminals    = null; 
	     		   this.nonterminals = null; 
	     		   this.start        = null;
				   this.productions  = null; 
				   this.costs        = null;
				   this.replace      = null;}
	
	public CCFG(Set<Terminal> terminals, Set<Nonterminal> nonterminals, 
				Nonterminal start, Collection<Production> productions, 
				HashMap<String,Integer> cost, HashMap<String,String> replace){
		this.terminals    = terminals;
		this.nonterminals = nonterminals;
		this.start        = start;
		this.productions  = productions;
		this.costs        = cost;
		this.replace      = replace;
	}
	
	public Set<Terminal> getTerminals() {
		return this.terminals;
	}
	
	public Set<Nonterminal> getNonterminals() {
		return this.nonterminals;
	}
	
	public Nonterminal getStartSymbol() {
		return this.start;
	}
	
	public Collection<Production> getProductions() {
		return this.productions;
	}
	
	public HashMap<String,Integer> getCostFunction() {
		return this.costs;
	}
	
	public HashMap<String,String> getReplaceFunction() {
		return this.replace;
	}
	
	public void setStartSymbol(Nonterminal n) {
		this.start = n;
	}

	public void setCostFunction(HashMap<String,Integer> cost) {
		this.costs = cost;
	}
	
	public void setReplace(HashMap<String,String> repl) {
		this.replace = repl;
	}
	
	public String toString(){
		return "CCFG node";
	}
}