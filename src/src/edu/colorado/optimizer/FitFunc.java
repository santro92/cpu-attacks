package edu.colorado.optimizer;

import java.io.IOException;
import java.util.LinkedList;

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import edu.colorado.ccfgparser.CCFGLexer;
import edu.colorado.ccfgparser.CCFGParser;
import edu.colorado.cpuattacks.AST;
import edu.colorado.cpuattacks.CCFG;
import edu.colorado.cpuattacks.MosekEncoder;
import edu.colorado.cpuattacks.MyCCFGVisitor;

public class FitFunc
{
	private CCFG getCCFG() 
	{
		ANTLRInputStream input = null;
		try 
		{
			input = new ANTLRFileStream(ParameterizedGrammerGenerator.OUTPUT_FILE_NAME);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
        CCFGLexer lexer = new CCFGLexer((CharStream) input);
        CommonTokenStream tokens = new CommonTokenStream((TokenSource) lexer);
        CCFGParser parser = new CCFGParser((TokenStream) tokens);
        
        ParseTree tree = parser.costgram();
        MyCCFGVisitor ccfgVisitor = new MyCCFGVisitor();
        LinkedList<AST> traverseResult = ccfgVisitor.visit(tree);
        
        return (CCFG) traverseResult.getFirst();
	}
	
	double valueOf (double[] x) throws Exception 
	{
		ParameterizedGrammerGenerator.generateTree(x);
		CCFG ccfg = getCCFG();
        MosekEncoder mosekEnc = new MosekEncoder();
        double[] freqSolution = mosekEnc.encodeGrammarFirstILP(ccfg);
        double[] solution     = mosekEnc.encodeGrammarSecondILP(ccfg,freqSolution);
        String result = mosekEnc.extractSolution(ccfg,solution,freqSolution);
        return result.length();
	}
	 
	boolean isFeasible(double[] x) 
	{
		for(int i=0; i< x.length; i++) 
		{
			if (x[i] < 0)
			{
				return false;
			}
		}
		return true; 
	}
}
