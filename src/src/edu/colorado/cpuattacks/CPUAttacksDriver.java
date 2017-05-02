package edu.colorado.cpuattacks;

import java.io.IOException;
import java.io.InputStream;

import java.util.LinkedList;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import edu.colorado.ccfgparser.*;
import edu.colorado.cpuattacks.AST.*;

public class CPUAttacksDriver {
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("Parser started.");
		System.out.println("Input file: " + args[0]);
		ANTLRInputStream input;
		try {
			input = new ANTLRFileStream(args[0]);
		} catch (IOException e) {
			try { // try again by reading the stdin
				input = new ANTLRInputStream(System.in);
			} catch (IOException eLast) {
				eLast.printStackTrace();
				input = new ANTLRInputStream();
			}
		}

        CCFGLexer lexer = new CCFGLexer((CharStream) input);

        CommonTokenStream tokens = new CommonTokenStream((TokenSource) lexer);

        CCFGParser parser = new CCFGParser((TokenStream) tokens);
        ParseTree tree = parser.costgram(); // begin parsing at rule 'gram'
        System.out.println("----- Done parsing. -----");
        System.out.println("Parsed tree:");
        System.out.println("\t"+tree.toStringTree(parser)); // print LISP-style tree
        
        MyCCFGVisitor ccfgVisitor = new MyCCFGVisitor();
        LinkedList<AST> traverseResult = ccfgVisitor.visit(tree);
        CCFG ccfg = (CCFG) traverseResult.getFirst();
        System.out.println("----- Done visiting. -----");
        
        MosekEncoder mosekEnc = new MosekEncoder();
        double[] freqSolution = mosekEnc.encodeGrammarFirstILP(ccfg);
        double[] solution     = mosekEnc.encodeGrammarSecondILP(ccfg,freqSolution);
        mosekEnc.extractSolution(ccfg,solution,freqSolution);
        System.out.println("Done!");
        
        //CMAEncoder cmaEncoder = new CMAEncoder();
        //cmaEncoder.encodeGrammar(ccfg); //main(args);
	}
}
