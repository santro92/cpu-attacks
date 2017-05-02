package edu.colorado.cpuattacks;

import java.util.*;
import java.util.stream.Stream;

import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;

import edu.colorado.ccfgparser.CCFGBaseVisitor;
import edu.colorado.ccfgparser.CCFGParser;
import edu.colorado.ccfgparser.CCFGParser.CostgramContext;
import edu.colorado.cpuattacks.AST.*;

public class MyCCFGVisitor extends CCFGBaseVisitor<LinkedList<AST>>{
	HashMap<String,Integer>   costs;
	Collection<Production>    prods;
	Set<Nonterminal>          nonterms;
	Nonterminal               start;
	Set<Terminal> 	          terms;
	HashMap<String,String>    replace;
	private int productionID  = 0;
	private int nonterminalID = 0;
	
	@Override
	public LinkedList<AST> visitCostgram(CostgramContext ctx) {
		System.out.println("visitCostgram" + ctx + "ctx:\n" + ctx.getText());
		costs    = new HashMap<String,Integer>();
		replace  = new HashMap<String,String>();
		prods    = new LinkedList<Production>();
		nonterms = new TreeSet<Nonterminal>();
		start    = null;
		terms    = new TreeSet<Terminal>();
		
		AST theCCFG = visitChildren(ctx).getFirst();
		//System.out.println("CCFG: "+theCCFG);
		assert(theCCFG instanceof CCFG);
		CCFG ccfg = (CCFG) theCCFG;
		assert(start != null);
		// Check to make sure replace macro and start are in place
		//System.out.println("Replace macro: ");
		ccfg.setReplace(replace);
		//HashMap<String,String> repl = ccfg.getReplaceFunction();
		//repl.forEach((k,v) -> System.out.println("K: "+k+", V: "+v));
		System.out.println("Start symbol: " + start);
		ccfg.setStartSymbol(start);
		
		LinkedList<AST> retList = new LinkedList<AST>();
		retList.add(ccfg);
		return retList;
	}
	
	@Override public LinkedList<AST> visitGram(CCFGParser.GramContext ctx) { 
		System.out.println("visitGram" + ctx + "ctx:\n" + ctx.getText());
	 	
		LinkedList<Production>   productions = (LinkedList<Production>)(LinkedList<?>)visitChildren(ctx); 
		ListIterator<Production> prodsIter   = productions.listIterator();
		
		prodsIter.forEachRemaining(System.out::println);
		System.out.println("Done printing productions.");
		
		Iterator<Nonterminal> nontermIters = nonterms.iterator();
		nontermIters.forEachRemaining(n->n.setID(nonterminalID++));
				
		CCFG myCCFG = new CCFG(terms,nonterms,start,productions,costs,replace);
		LinkedList<AST> retList = new LinkedList<AST>();
		retList.add(myCCFG);
		return retList;
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public LinkedList<AST> visitStart(CCFGParser.StartContext ctx) { 
		if(ctx.getChildCount() <= 0)
			System.err.println("Start symbol missing! Add: \"start T\"");
		String startString = ctx.n.getText();
		for (Nonterminal n : nonterms) {
	        if (n.getName().equals(startString))
	        		start = n;
	    }
		
		LinkedList<AST> retList = new LinkedList<AST>();
		retList.add(start);
		return retList; 
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public LinkedList<AST> visitLastCrhs(CCFGParser.LastCrhsContext ctx) { 
		// the last "| (aB)_n" node of a production rule
		//System.out.println("ENTER: visitLastCrhs" + ctx + ctx.getText());
		LinkedList<AST> retList = visit(ctx.rule_hd); // cast super type to subtype
		return retList;
		
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public LinkedList<AST> visitMoreCrhs(CCFGParser.MoreCrhsContext ctx) { 
		//System.out.println("ENTER: visitMoreCrhs" + ctx + ctx.getText());
		LinkedList<AST> retList = visitChildren(ctx); 
		//System.out.println("EXIT : visitMoreCrhs" + ctx + ctx.getText());
		//ListIterator<AST> listIterator = retList.listIterator();
		//listIterator.forEachRemaining(System.out::println);
		return retList;
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public LinkedList<AST> visitMultipleProd(CCFGParser.MultipleProdContext ctx) { 
		// T -> U | V 
		//System.out.println("ENTER: visitMultipleProd" + ctx + ctx.getText());
		Nonterminal nonterm = new Nonterminal(ctx.lhs.getText()); // T 
		nonterms.add(nonterm);
		LinkedList<Production> rhs_hdList   = (LinkedList<Production>)(LinkedList<?>)visit(  ctx.rhs_hd);
		LinkedList<Production> rhs_restList = (LinkedList<Production>)(LinkedList<?>)visit(ctx.rhs_rest);
		rhs_hdList.addAll(rhs_restList);
		ListIterator<Production> iter  = rhs_hdList.listIterator();
		iter.forEachRemaining(p->p.setLHS(nonterm));
		//int numChildren = ctx.getChildCount();
		//assert(numChildren > 2);
		//for (int i = 2; i < numChildren; i++) // 0: T; 1: -> ; 2 -> U, ...
		//	System.out.println(ctx.getChild(i).getPayload());
		//visitChildren(ctx);
		//System.out.println("EXIT : visitMultipleProd" + ctx + ctx.getText());
		//retList = visitChildren(ctx); 
		return (LinkedList<AST>)(LinkedList<?>)rhs_hdList;
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public LinkedList<AST> visitSingletonProd(CCFGParser.SingletonProdContext ctx) { 
		// T -> (aB)_3 
		System.out.println("visitSingletonProd" + ctx + ctx.getText());
		Nonterminal nonterm = new Nonterminal(ctx.lhs.getText()); // T 
		nonterms.add(nonterm);
		LinkedList<Production> tmpList = (LinkedList<Production>)(LinkedList<?>) visit(ctx.rhs_hd);
		ListIterator<Production> iter  = tmpList.listIterator();
		iter.forEachRemaining(p->p.setLHS(nonterm));
		LinkedList<AST> retList = (LinkedList<AST>)(LinkedList<?>)tmpList;
		return retList;
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public LinkedList<AST> visitConstrainedProd(CCFGParser.ConstrainedProdContext ctx) { 
		//System.out.println("ENTER: visitConstrainedProd" + ctx + ctx.getText());
		LinkedList<Identifier> rhsList = (LinkedList<Identifier>)(LinkedList<?>) visit(ctx.inner); // cast super type to subtype
		int capacity    = Integer.parseInt(ctx.c.getText());
		Production prod = new Production(productionID++,null,rhsList,capacity);
		
		LinkedList<AST> pList = new LinkedList<AST>();
		pList.add(prod);
		return pList;
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public LinkedList<AST> visitConstrainedTermProd(CCFGParser.ConstrainedTermProdContext ctx) { 
		//System.out.println("ENTER: visitConstrainedTermProd" + ctx + ctx.getText());
		//System.out.println("EXIT : visitConstrainedTermProd" + ctx + ctx.getText());
		LinkedList<AST> pList = new LinkedList<AST>();
		Terminal tokenT = new Terminal(ctx.t.getText(),-1);
		terms.add(tokenT);
		LinkedList<Identifier> idents = new LinkedList<Identifier>();
		idents.add(tokenT);
		int capacity    = Integer.parseInt(ctx.c.getText());
		Production prod = new Production(productionID++,null,idents,capacity);
		pList.add(prod);
		return pList;
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public LinkedList<AST> visitUnconstrainedTermParen(CCFGParser.UnconstrainedTermParenContext ctx) { 
		//System.out.println("ENTER: visitUnconstrainedTermParen" + ctx + ctx.getText());
		//System.out.println("EXIT : visitUnconstrainedTermParen" + ctx + ctx.getText());
		LinkedList<Identifier> retList = (LinkedList<Identifier>)(LinkedList<?>)visitChildren(ctx); 
		Production prod = new Production(productionID++,null,retList,-1);
		LinkedList<AST> pList = new LinkedList<AST>();
		pList.add(prod);
		return pList;
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public LinkedList<AST> visitUnconstrainedTerm(CCFGParser.UnconstrainedTermContext ctx) {
		LinkedList<Identifier> retList = (LinkedList<Identifier>)(LinkedList<?>)visitChildren(ctx); 
		Production prod = new Production(productionID++,null,retList,-1);
		LinkedList<AST> pList = new LinkedList<AST>();
		pList.add(prod);
		return pList;
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public LinkedList<AST> visitRhs(CCFGParser.RhsContext ctx) { 
		//System.out.println("ENTER: visitRHS" + ctx + ctx.getText());
		LinkedList<AST> retList = visitChildren(ctx); 		
		//System.out.println("EXIT : visitRHS" + ctx + ctx.getText());
		//ListIterator<AST> listIterator = retList.listIterator();
		//listIterator.forEachRemaining(System.out::println);
		return retList;
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public LinkedList<AST> visitCostfun(CCFGParser.CostfunContext ctx) { 
		//System.out.println("ENTER: visitCostfun" + ctx + ctx.getText());
		LinkedList<AST> retList = new LinkedList<AST>();
		//for (int i = 0; i < ctx.getChildCount(); i++){
		//	retList = visitChildren(ctx);
		visitChildren(ctx);
		//}
		//System.out.println("EXIT : visitCostfun" + ctx + ctx.getText());
		//ListIterator<AST> listIterator = retList.listIterator();
		//listIterator.forEachRemaining(System.out::println);
		return retList; // ignore this return type
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public LinkedList<AST> visitTermcost(CCFGParser.TermcostContext ctx) { 
		//visitChildren(ctx);
		//int numChildren = ctx.getChildCount();
		//assert(numChildren == 3);
		//CommonToken terminalObj = (CommonToken) ctx.t;
		String term       = ctx.t.getText(); //terminalObj.getText();
		//CommonToken costObj     = (CommonToken) ctx.getChild(2).getPayload();
		Integer cost      = Integer.parseInt(ctx.c.getText());
		
		Terminal terminal = new Terminal(ctx.t.getText(), cost);
		terms.add(terminal);
		//System.out.println("visitTermcost" + ctx + ctx.getText() + " " + term);
		costs.put(term, cost);
		
		LinkedList<AST> retList = new LinkedList<AST>();
		retList.add(terminal);
		return retList;
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public LinkedList<AST> visitMacro(CCFGParser.MacroContext ctx) { 
		//System.out.println("ENTER: visitCostfun" + ctx + ctx.getText());
		LinkedList<AST> retList = new LinkedList<AST>();
		//for (int i = 0; i < ctx.getChildCount(); i++){
		//	retList = visitChildren(ctx);
		visitChildren(ctx);
		//}
		//System.out.println("EXIT : visitCostfun" + ctx + ctx.getText());
		//ListIterator<AST> listIterator = retList.listIterator();
		//listIterator.forEachRemaining(System.out::println);
		return retList; // ignore this return type
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public LinkedList<AST> visitTermrepl(CCFGParser.TermreplContext ctx) { 
		//visitChildren(ctx);
		//int numChildren = ctx.getChildCount();
		//assert(numChildren == 3);
		//CommonToken terminalObj = (CommonToken) ctx.t;
		String term       = ctx.t.getText(); //terminalObj.getText();
		//CommonToken costObj     = (CommonToken) ctx.getChild(2).getPayload();
		String repl      = ctx.s.getText(); // replace the term with repl string
		String replNoQuotes = repl.substring(1,repl.length()-1);
		
		//System.out.println("visitTermcost" + ctx + ctx.getText() + " " + term);
		replace.put(term, replNoQuotes);
		
		LinkedList<AST> retList = new LinkedList<AST>();
		return retList;
	}
	@Override
	public LinkedList<AST> visitTerminal(TerminalNode node) {
		LinkedList<AST> retList = new LinkedList<AST>();
		switch(node.getSymbol().getType()){
			case 5: // nonterminal
				Nonterminal nonterm = new Nonterminal(node.getSymbol().getText());
				nonterms.add(nonterm);
				retList.add(nonterm);
				return retList;
			case 6: // terminal
				Terminal term = new Terminal(node.getSymbol().getText(),-1);
				terms.add(term);
				retList.add(term);
				return retList;
			case 15: // INT 
				Identifier id = new Identifier("-",Integer.parseInt(node.getSymbol().getText()));
				retList.add(id);
				return retList;
			case 16: // STRINGLIT
				System.err.println("STRINGLIT: "+node.getSymbol().getText());
				return null;
			case  1: // 'start '
			case  2: // 'cost '
			case  3: // ' '
			case  4: // 'replace '
			case  7: // '|'
			case  8: // '('
			case  9: // ')'
			case 10: // '_'
			case 11: // ','
			case 12: // ';'
			case 13: // ARROW
			case 17: // SEP
			case 14: // '\n'
			case 18: // White space
				return retList;
			default: 
				System.err.println("visitTerminal: Unhandled TOKEN: "+node.getSymbol()+" "+node.getSymbol().getType() );
		}
		return super.visitTerminal(node);
	}
	@Override
	protected LinkedList<AST> aggregateResult(LinkedList<AST> aggregate, LinkedList<AST> nextResult) {
		if (aggregate == null || aggregate.isEmpty())
			return nextResult;
		if (nextResult == null || nextResult.isEmpty())
			return aggregate;
		aggregate.addAll(nextResult);
		return aggregate;
	}
}