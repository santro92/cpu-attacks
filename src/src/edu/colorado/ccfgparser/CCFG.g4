grammar CCFG; // start a constrained context free grammar definition
@header{
	package edu.colorado.ccfgparser;
}
costgram: gram start (costfun (NEWLINE)*| macro (NEWLINE)*)* (NEWLINE)*;    // a CCFG & a cost function | a CCFG
start: 'start ' n=Nonterm NEWLINE*; // Specify 'start' symbol for the CCFG
gram:  (prod NEWLINE)* | (prod SEMI NEWLINE)*; 
prod:  lhs=Nonterm ARROW WS* rhs_hd=crhs rhs_rest=extra_crhs # MultipleProd  // Example S -> (aB)_2 | (a)_1
	|  lhs=Nonterm ARROW WS* rhs_hd=crhs                     # SingletonProd // Example S -> (aBc)_2
	;
extra_crhs: PIPE rule_hd=crhs                      # LastCrhs
		  | PIPE rule_hd=crhs rule_rest=extra_crhs # MoreCrhs
		  ;
crhs:  LPAREN inner=rhs RPAREN UNDRSCR c=INT # ConstrainedProd // a constrained production rule (aBc)_2
	|  t=Term UNDRSCR c=INT           # ConstrainedTermProd    // a constraint terminal production a_2
	|  LPAREN t=Term RPAREN           # UnconstrainedTermParen // (an unconstrained) terminal production (z) 
	|  t=Term                         # UnconstrainedTerm      // (an unconstrained) terminal production z
	;
rhs :  id (id)*;   // the usual rhs of a production rule
id  : (Term | Nonterm);   
costfun : 'cost ' termcost (COMMA termcost)*;  // cost function, i.e., w(a) = 1 
termcost: t=Term ' ' c=INT; // w(a) = 1 
macro   : 'replace ' termrepl (COMMA termrepl)*; // replace macro, i.e., a "public class", b "{", c "}" etc.
termrepl: t=Term ' ' s=STRINGLIT; //replace t with string s
Nonterm : [A-Z] ;           // a nonterminal
Term    : [a-z] ;           // a terminal
PIPE    : WS* '|' WS* ;
LPAREN  : '(';
RPAREN  : ')';
UNDRSCR : '_';
COMMA   : WS* ',' WS* ;
SEMI    : WS* ';' WS*  ;         // Semicolon rule separator
ARROW   : WS* '->'  WS*
		| WS* '::=' WS*   
		| WS* ':'   WS*;         // the arrow for productions
NEWLINE : [\r \n]+ ;           	 // a new line
INT     : [0-9]+ ;  				 // an int definition
STRINGLIT : '"' ( '\\' [btnfr"'\\] | ~[\r\n\\"] )* '"';
SEP     : ' '
		| ',';           
WS      : [ \t \r \n]+ -> skip ; // whitespace 
