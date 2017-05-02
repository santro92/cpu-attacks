package edu.colorado.cpuattacks;

//import edu.colorado.cpuattacks.AST.*;
import java.util.*;
import java.util.stream.Stream;

import javafx.util.Pair;
import mosek.*;

public class MosekEncoder {
	private int numConstraints;
	private int numVariables;
	private String[] varNames;     // the names of the decision variables
	private double[] decisionVars; // the decision variables
	// private int NUMANZ = 9;
	private ArrayList<int[]>    asub; // size: numConstraints int[]    of var_indices  that appear in each constraint
	private ArrayList<double[]> aval; // size: numConstraints double[] of coefficients that appear in each constraint
	private int maxSize = -1;

	
	public String arrayToString(int[] array){
		String resString = Integer.toString(array[0]);
		for(int i=1; i< array.length; i++)
			resString += "," + Integer.toString(array[i]);
		return resString;
	}
	
	public static String arrayToString(double[] array){
		String resString = Double.toString(array[0]);
		for(int i=1; i< array.length; i++)
			resString += "," + Double.toString(array[i]);
		return resString;
	}
	
	private String arrayToVarnameString(int[] is, String[] varNames2) {
		String resString = varNames2[is[0]];
		for(int i=1; i< is.length; i++)
			resString += ";" + varNames2[is[i]];
		return resString;
	}
	
	public Pair<int[],double[]> encodeConstraint(ArrayList<Integer> ins, ArrayList<Integer> outs) {
		int size = ins.size() + outs.size();
		if (maxSize < size)
			maxSize = size;
		
		// Check and find duplicates 
		HashMap<Integer,Double> occurs = new HashMap<Integer,Double>();
		for (int i=0; i < ins.size(); i++){
			int key = ins.get(i);
			double cnt = (occurs.get(key) == null)? 0.0: occurs.get(key);
			//System.out.println(cnt);
			occurs.put(key, cnt+1.0);
		}
		for (int i=0; i < outs.size(); i++){
			int key = outs.get(i);
			double cnt = (occurs.get(key) == null)? 0.0: occurs.get(key);
			occurs.put(key, cnt-1.0);
		}
		size = occurs.size();
		int[]    asubRow = new    int[size];
		double[] avalRow = new double[size];
		int index = 0;
		for(Map.Entry<Integer, Double> entry : occurs.entrySet()){
			asubRow[index]=entry.getKey();
			avalRow[index]=entry.getValue();
			index++;
		}
		
		Pair<int[],double[]> retList = new Pair<int[],double[]>(asubRow,avalRow);
		return retList;
	}
	
	
	public double[] encodeGrammarFirstILP(CCFG gram) {
			HashMap<String, Integer> costs = gram.getCostFunction();
				 Set<Nonterminal> nonterms = gram.getNonterminals();
		System.out.println("Nonterminals: " + nonterms);
		 Iterator<Nonterminal> nontermIter = nonterms.iterator();
		 			   int numNonterminals = nonterms.size();
							numConstraints = nonterms.size();
		Collection<Production> productions = gram.getProductions();
							  numVariables = productions.size();
			 Iterator<Production> prodIter = productions.iterator(); 
			 	   Nonterminal startSymbol = gram.getStartSymbol();
		varNames = new String[numVariables];
		for (int i = 0; i < numVariables; i++) varNames[i] = "f"+i;

		HashMap<String, ArrayList<Integer>> ins  = new HashMap<String, ArrayList<Integer>>();
		HashMap<String, ArrayList<Integer>> outs = new HashMap<String, ArrayList<Integer>>();
		
		double[] c = new double[numVariables]; // objective function
		for (int i=0; i < numVariables; i++) c[i] = 0.0;
		
		mosek.boundkey[] bkc = new mosek.boundkey[numConstraints];
		double[] blc = new double[numConstraints];
        double[] buc = new double[numConstraints];
        
		for (int i=0; i < numConstraints; i++) {
			bkc[i] = mosek.boundkey.fx;
			blc[i] = 0.0; buc[i] = 0.0;
		}
		
		mosek.boundkey[] bkx = new mosek.boundkey[numVariables];
		double[] blx = new double[numVariables];
		double[] bux = new double[numVariables];
		for (int i=0; i < numVariables; i++) {
			bkx[i] = mosek.boundkey.ra;
			blx[i] = 0.0; bux[i] = 0.0;
		}
		
		prodIter.forEachRemaining(p -> {
			int prodID = p.getID();
			assert(0 <= prodID && prodID < numVariables);
			String lhs = p.getLHS().getName();
			ArrayList<Integer> out = outs.get(lhs);
			if (out == null)
				out = new ArrayList<Integer>();
			out.add(prodID);
			outs.put(lhs, out);
			LinkedList<Identifier> rhs = p.getRHS();
			Stream<String> rhsStream = rhs.stream()
					.filter(iden->(iden instanceof Nonterminal))
					.map(nonterm->nonterm.getName());
			// compute the objective function
			rhs.stream().filter(id->(id instanceof Terminal))
						.forEach(t->c[prodID] += costs.get(t.getName()));
			// compute the in(T) and out(T) for each nonterminal T
			rhsStream.forEach(rhsString -> {
				ArrayList<Integer> in = ins.get(rhsString);
				if (in == null)
					in = new ArrayList<Integer>();
				in.add(prodID);
				ins.put(rhsString,in);
			});
			int cap = p.getCapacity();
			if (cap <= 0)
				bkx[prodID] = mosek.boundkey.lo; // only lower-bounded
			else 
				bux[prodID] = (double)cap; // upper-bounded by capacity constraint
		});
		
		System.out.println("Ins : " + ins );
		
		System.out.println("Outs: " + outs);
		
		System.out.println("Objective Function:");
		System.out.println("\t["+arrayToString(c)+"]");
		
		System.out.println("blx: "+arrayToString(blx));
		System.out.println("bux: "+arrayToString(bux));
				
		asub = new ArrayList<   int[]>();
		aval = new ArrayList<double[]>();
		ArrayList<Double> blcList = new ArrayList<Double>();
		ArrayList<Double> bucList = new ArrayList<Double>();
		nontermIter.forEachRemaining(nonterm -> {
			String nontermString = nonterm.name;
			ArrayList<Integer> insList  =  ins.get(nontermString);
			if (insList  == null)
				insList  = new ArrayList<Integer>();
			if (nonterm == startSymbol) { 
				//System.err.println("Is "+nonterm+" the start symbol?");
				blcList.add(-1.0);
				bucList.add(-1.0);
			} else {
				blcList.add( 0.0);
				bucList.add( 0.0);
			}
			ArrayList<Integer> outsList = outs.get(nontermString);
			if (outsList == null)
				outsList = new ArrayList<Integer>();
			Pair<int[],double[]> asubaval = encodeConstraint(insList, outsList);
			int[] asubArr = asubaval.getKey();
			//System.out.println("insList : "+ insList);
			//System.out.println("outsList: "+outsList);
			//System.out.println("asubArr: "+arrayToString(asubArr));
			asub.add(asubArr);
			double[] avalArr = asubaval.getValue();
			aval.add(avalArr);
		});
		
		int[][]    asubArr = new    int[numConstraints][];
		double[][] avalArr = new double[numConstraints][];
		for(int i = 0; i < numConstraints; i++) {
			asubArr[i] = asub.get(i);
			avalArr[i] = aval.get(i);
			blc[i]     = blcList.get(i);
			buc[i]     = bucList.get(i);
			//System.out.format("asub[%d] : %s%n", i, arrayToVarnameString(asubArr[i],varNames));  
			//System.out.format("aval[%d] : %s%n", i, arrayToString(avalArr[i]));
		}
		//System.out.println("blc: "+arrayToString(blc));
		//System.out.println("buc: "+arrayToString(buc));

		return solveProblem(c,bkx,blx,bux,bkc,blc,buc,asubArr,avalArr);
	}

	
	public double[] encodeGrammarSecondILP(CCFG ccfg, double[] freqSolution) {
		System.out.println("\nStarting Second ILP Encoding.");
		 Set<Nonterminal> nonterms = ccfg.getNonterminals();
		 Iterator<Nonterminal> nontermIter = nonterms.iterator();
					   int numNonterminals = nonterms.size();
		Collection<Production> productions = ccfg.getProductions();
						int	numProductions = productions.size();
			 Iterator<Production> prodIter = productions.iterator();
			 	   Nonterminal startSymbol = ccfg.getStartSymbol();
		
		double freqSum = 0.0; 
		for (int i = 0; i < freqSolution.length; i++)
			freqSum += freqSolution[i];
		int numTransitions = (int) Math.round(freqSum);
		System.out.println("Number of transitions: "+numTransitions+" "+freqSum);
		
		int numRuleVariables  = numProductions  *  numTransitions;    // rule  vars: r11, r12, r13, r14, r21, ...
		int numStateVariables = numNonterminals * (numTransitions+1); // state vars: x01, x02, x03, x04, x11, ...
		// x_(0,numNonterminals)    = <1,0,...,0>; -> Start state
		// x_(last,numNonterminals) = <0,0,...,0>; -> End state all terminals
		numVariables = numRuleVariables + numStateVariables; 
		
		double[] c = new double[numVariables];
		for (int i=0; i < numVariables; i++) c[i]=0.0;
		
		mosek.boundkey[] bkx = new mosek.boundkey[numVariables];
		double[]         blx = new double[numVariables];
		double[]         bux = new double[numVariables];
		varNames = new String[numVariables];
		for (int i = 0; i < numRuleVariables;  i++){ 
			bkx[i]      = mosek.boundkey.ra;
			blx[i]	 	= 0;
			bux[i]		= 1;
			varNames[i] = "r"+(i/numProductions + 1)+","+(i%numProductions);
		}
		for (int i = 0; i < numStateVariables; i++){
			bkx[i+numRuleVariables]      = mosek.boundkey.lo;
			blx[i+numRuleVariables]	 	 = 0;
			bux[i+numRuleVariables]	 	 = 0;
			varNames[i+numRuleVariables] = "x"+(i/numNonterminals)+","+(i%numNonterminals);
		}
		//for (int i = 0; i < numVariables; i++) System.out.println(i+" : "+varNames[i]);

		numConstraints = numTransitions + numProductions // "a rule at each step"  + "capacity constraints per rule"
				+ numStateVariables + 2*numNonterminals; // + "state update rules" + start + last 
																			  
		ArrayList<   int[]> 	  asub = new ArrayList<   int[]>();
		ArrayList<double[]>       aval = new ArrayList<double[]>();
		ArrayList<mosek.boundkey> bkc  = new ArrayList<mosek.boundkey>();
		ArrayList<Double> 	 	  blc  = new ArrayList<Double>();
		ArrayList<Double> 	  	  buc  = new ArrayList<Double>();		
		ArrayList< Integer> 	  outs = new ArrayList< Integer>();
		
		// Add the "a rule at each step" constraint
		for (int i = 0; i < numTransitions; i++){
			ArrayList<Integer> ins = new ArrayList<Integer>();
			for (int j = 0; j < numProductions; j++)
				ins.add(i*numProductions + j);
			//System.out.println("ins["+(i+1)+"] : "+ ins);
			Pair<int[],double[]> asubaval = encodeConstraint(ins,outs);
			//System.out.println("asub["+(i+1)+"] : "+ arrayToString(asubaval.getKey()  ));
			//System.out.println("aval["+(i+1)+"] : "+ arrayToString(asubaval.getValue()));
			asub.add(asubaval.getKey()  );
			aval.add(asubaval.getValue());
			 bkc.add(mosek.boundkey.fx);
			 blc.add(1.0);
			 buc.add(1.0);
		}

		// We need to encode frequency constraints
		// We need to encode state updates
		prodIter.forEachRemaining(p -> {
			int prodID = p.ID;
			
			// Frequency constraints
			System.out.println("Capacity constraints");
			ArrayList<Integer> ins = new ArrayList<Integer>();
			for (int i=0; i < numTransitions; i++)
				ins.add(i*numProductions+prodID);
			//System.out.println("ins[prodID:"+prodID+"] : "+ ins);
			Pair<int[],double[]> asubaval = encodeConstraint(ins,outs);
			asub.add(asubaval.getKey()  );
			aval.add(asubaval.getValue());
			//blc.add(0.0);
			//if (p.getCapacity() <= 0){
			//	bkc.add(mosek.boundkey.ra);
			//	buc.add((double)p.getCapacity());
			//} else {
			//	bkc.add(mosek.boundkey.lo);
			//	buc.add(0.0);
			//}
			bkc.add(mosek.boundkey.fx);
			blc.add(freqSolution[prodID]);
			buc.add(freqSolution[prodID]);
		});
			
	
		// State updates
		Iterator<Nonterminal> nonIter = nonterms.iterator();
		nonIter.forEachRemaining(n -> { // for each j in x_ij
			System.out.println(n);
			String nName = n.getName();
			int nID = n.getID(); // get j
			
			for (int i=1; i <= numTransitions; i++){ // for each transition + 1
				ArrayList<Integer> ins = new ArrayList<Integer>();
				ArrayList<Integer> ots = new ArrayList<Integer>();
				int xij       = numRuleVariables +   i  *numNonterminals + nID; // ins
				//System.out.println("xij: " + xij       + " " + varNames[xij]);
				int ximinus1j = numRuleVariables + (i-1)*numNonterminals + nID; // ots
				//System.out.println("-x : " + ximinus1j + " " + varNames[ximinus1j]);
				ins.add(xij);
				ots.add(ximinus1j);
				
				for (Production p : productions) {
					int prodID  = p.getID();
					int ruleVar = (i-1)*numProductions+prodID;
					//System.out.println("rij: "+ruleVar+" "+p+" "+nName);
					//System.out.println("rij: "+ruleVar+" "+varNames[ruleVar]);
					String lhsName = p.getLHS().getName();
					if (nName.equals(lhsName))
						ins.add(ruleVar);
					LinkedList<Identifier> rhsList = p.getRHS();
					for (Identifier id : rhsList) {
						if (nName.equals(id.getName())) {
							//System.out.println("Match! "+id);
							ots.add(ruleVar);
						}
					}				
				}
				
				Pair<int[],double[]> asubaval = encodeConstraint(ins, ots);
				//System.out.println("asub: "+arrayToString(asubaval.getKey()));
				asub.add(asubaval.getKey());
				//System.out.println("aval: "+arrayToString(asubaval.getValue()));
				aval.add(asubaval.getValue());
				bkc .add(mosek.boundkey.fx);
				blc .add(0.0);
				buc .add(0.0);
				
				/*
				 * Rule applicability constraints
				 * xi,j - (ri,k.lhs == k) >= 0 constraints
				 */
				ximinus1j = numRuleVariables + (i-1)*numNonterminals + nID; //xi,j
				ins = new ArrayList<Integer>();
				ins.add(ximinus1j);
				ots = new ArrayList<Integer>();
				
				// subtract all production vars whose lhs == nonterminal
				for (Production p : productions) {
					int prodID  = p.getID();
					int ruleVar = (i-1)*numProductions+prodID;
					String lhsName = p.getLHS().getName();
					if (nName.equals(lhsName))
						ots.add(ruleVar);
				}
				
				Pair<int[],double[]> asubavalruleapps = encodeConstraint(ins, ots);
				//System.out.println("asub: "+arrayToString(asubaval.getKey()));
				asub.add(asubavalruleapps.getKey());
				//System.out.println("aval: "+arrayToString(asubaval.getValue()));
				aval.add(asubavalruleapps.getValue());
				bkc .add(mosek.boundkey.lo);
				blc .add(0.0);
				buc .add(0.0);
			}
		});
		
		
		nontermIter.forEachRemaining(n -> {
			int nID = n.getID();
			int x0i = numRuleVariables + nID;
			
			int[] x0iVar  = {x0i};
			double[] avar = {1.0};
			asub.add(x0iVar);
			aval.add(avar);
			bkc.add(mosek.boundkey.fx);
			if (n == startSymbol){ //n.isStartSymbol()?
				//System.err.println("Start symbol identified: "+n.getName());
				blc.add(1.0);
				buc.add(1.0);
			} else {
				blc.add(0.0);
				buc.add(0.0);
			}
			int xLasti = numVariables - numNonterminals + nID;
			int[] xLastiVar  = {xLasti}; double[] alast = {1.0};
			asub.add(xLastiVar); aval.add(alast);
			bkc.add(mosek.boundkey.fx);
			blc.add(0.0); buc.add(0.0);

		}); 
		
		System.out.println("numConstraints: "+numConstraints+" "+asub.size()+" "+aval.size()+" "+bkc.size()
							+" "+blc.size()+" "+buc.size());
		numConstraints = asub.size();
		int[][]    asubArr = new    int[numConstraints][];
		double[][] avalArr = new double[numConstraints][];
		mosek.boundkey[] bkcArr = new mosek.boundkey[numConstraints];
		double[]    blcArr = new double[numConstraints];
		double[]    bucArr = new double[numConstraints];
		for(int i = 0; i < numConstraints; i++) {
			asubArr[i] = asub.get(i);
			avalArr[i] = aval.get(i);
			//System.out.format("asub[%d] : %s%n", i, arrayToVarnameString(asubArr[i],varNames));  
			//System.out.format("aval[%d] : %s%n", i, arrayToString(avalArr[i]));
			bkcArr[i]  =  bkc.get(i);
			blcArr[i]  =  blc.get(i);			
			bucArr[i]  =  buc.get(i);
		}
		System.out.println("blc: "+arrayToString(blcArr));
		System.out.println("buc: "+arrayToString(bucArr));

		double[] solution = solveProblem(c,bkx,blx,bux,bkcArr,blcArr,bucArr,asubArr,avalArr);
		//for()
		return solution;
	}
	
	// S- > AB (0,1,1) -> aBB (0,0,2)
	// replaceString("AB", "A", "aB")
	// method replaceString (String currentString, String replacePattern, String replaceWith)
	public void extractSolution(CCFG gram, double[] solution, double[] freqSolution){
		System.out.println("Extracting Solution:");
		Collection<Production> productions = gram.getProductions();
		Iterator<Production> prodIter = productions.iterator();
		int numProductions = productions.size();
		Production[] productionArray = new Production[numProductions];
		prodIter.forEachRemaining(prod->{
			productionArray[prod.getID()]=prod;
			//System.out.format("Production %d: rhs: %s%n",prod.getID(),prod.getRHSString());
		});
		double freqSum = 0.0; 
		for (int i = 0; i < freqSolution.length; i++)
			freqSum += freqSolution[i];
		int numTransitions = (int) Math.round(freqSum);
		System.out.println("Number of transitions: "+numTransitions+" "+freqSum);
		int numRuleVariables  = numProductions  *  numTransitions;
		
        String stringFinal = ""; // S -> AB -> ASB -> ...
        String currentWord = "";
        String newWord     = "";
		for (int i = 0; i < numRuleVariables; i++){
			if (solution[i] > 0.5) {//If the rule choice variable is ==1
				int prodID = i%numProductions;
				String lhsString = productionArray[prodID].getLHSString();
				String rhsString = productionArray[prodID].getRHSString();
				if (currentWord.equals("")){
					currentWord = lhsString;
					stringFinal = currentWord;
					//System.err.println("New start symbol: "+stringFinal);
				}
				newWord = currentWord.replaceFirst(lhsString, rhsString);
				stringFinal += " -> "+newWord+"\n";
				//System.out.println("Current: "+currentWord+", prod: "+productionArray[prodID]+" newWord: "+newWord);//+"\n"+stringFinal);
				currentWord = newWord;
				//Extract the rule RHS into string replacePattern from the corresponding variable
				//In string final, identify the corresponding LHS for the RHS defined above
				//Replace LHS in final with the RHS extracted in string replacePattern
				//System.out.format("Rule Variable num: %d name: %s prodID: %d is 1%n", i,varNames[i],i%numProductions);
			}
		}
		System.out.println(stringFinal);
		HashMap<String,String> replaceMap = gram.getReplaceFunction();
		if (!replaceMap.isEmpty()){
			String replacedString = ""; 
			for(int i=0; i < currentWord.length(); i++){
				replacedString += replaceMap.get(Character.toString(currentWord.charAt(i)));
			}
			System.out.println("Attack String:\n"+replacedString);
		}
	}
	
	public void replaceString (String currentString, String replacePattern, String replaceWith) {
		//String replaceWith = "";
		//String replacePattern = "";
		String finalstring = "";//We need to add the first production rule LHS into the string (i.e. S)
	}
	
	public double[] solveProblem(double[] c,                  // objective function
							 mosek.boundkey[] bkx,            // type of each constraint
							 double[] blx, double[] bux,      // lower, upper bounds for each constraint
							 mosek.boundkey[] bkc,            // type of bound for decision var
							 double[] blc, double[] buc,      // lower, upper bounds for each decision var
							 int[][] asub, double[][] aval) { // matrix A encoding
		try (Env env = new Env(); Task task = new Task(env, 0, 0)) {
			// Directs the log task stream to the user specified
			// method task_msg_obj.stream
			task.set_Stream(mosek.streamtype.log, new mosek.Stream() {
				public void stream(String msg) {
					System.out.print(msg);
				}
			});

			decisionVars = new double[numVariables];
			
			/* FOR INTEGER PROGRAM */
			task.set_ItgSolutionCallback(new mosek.ItgSolutionCallback() {
				public void callback(double[] decisionVars) {
					System.out.print("New integer solution: ");
					for (double v : decisionVars)
						System.out.print("" + v + " ");
					System.out.println("");
				}
			});
			/* END FOR INTEGER PROGRAM */

			/*
			 * Give MOSEK an estimate of the size of the input data. This is
			 * done to increase the speed of inputting data. However, it is
			 * optional.
			 */
			/*
			 * Append 'numConstraints' empty constraints. The constraints will initially
			 * have no bounds.
			 */
			task.appendcons(numConstraints);

			/*
			 * Append 'numVariables' variables. The variables will initially be fixed
			 * at zero (x=0).
			 */
			task.appendvars(numVariables);

			for (int j = 0; j < numVariables; ++j) {
				/* Set the linear term c_j in the objective. */
				task.putcj(j, c[j]);
				/*
				 * Set the bounds on variable j. blx[j] <= x_j <= bux[j]
				 */
				//System.out.println(j + " : " + varNames[j]+" "+bkx[j]+" "+blx[j]+" "+bux[j]);
				task.putvarbound(j, bkx[j], blx[j], bux[j]);

				// NO -- Input column j of A
				// task.putacol(j, asub[j], aval[j]);
			}

			/*
			 * Set the bounds on constraints. for i=1, ...,numConstraints : blc[i] <=
			 * constraint i <= buc[i]
			 */
			for (int i = 0; i < numConstraints; ++i) {
				//System.out.println("asub["+i+"] : "+arrayToVarnameString(asub[i],varNames));
				//System.out.println("aval["+i+"] : "+arrayToString(aval[i]) + " " + bkc[i] +" "+blc[i]+" "+ buc[i]);
				task.putbound(mosek.accmode.con, i, bkc[i], blc[i], buc[i]);

				/* Input row i of A */
				task.putarow(i, /* Row index. */
						asub[i], /* Column indexes of non-zeros in row i. */
						aval[i]); /* Non-zero Values of row i. */
			}
			/* FOR INTEGER PROGRAM */
			/* Specify integer variables. */
			for (int j = 0; j < numVariables; ++j)
				task.putvartype(j, mosek.variabletype.type_int);
			/* END FOR INTEGER PROGRAM */

			/* A maximization problem */
			task.putobjsense(mosek.objsense.maximize);

			/* Solve the problem */
			try {
				task.optimize();
			} catch (mosek.Warning e) {
				System.out.println(" Mosek warning:");
				System.out.println(e.toString());
			}

			// Print a summary containing information
			// about the solution for debugging purposes
			task.solutionsummary(mosek.streamtype.msg);

			/* Get status information about the solution */
			mosek.solsta solsta[] = new mosek.solsta[1];
			task.getsolsta(mosek.soltype.itg, solsta); // get the integer
														// solution type

			/* INTEGER PROGRAM HANDLER */
			task.getxx(mosek.soltype.itg, // Request the integer solution.
					decisionVars);
			switch (solsta[0]) {
			case integer_optimal:
			case near_integer_optimal:
				System.out.println("Optimal solution\n");
				double obj = 0.0;
				for (int j = 0; j < numVariables; ++j) {
					System.out.format("%s: %.9f%n", varNames[j], decisionVars[j]); // ("f[" +
																		// j +
																		// "]:"
																		// +
																		// decisionVars[j]);
					obj += c[j] * decisionVars[j];
				}
				;
				System.out.println("obj :" + obj);
				return decisionVars;
			case prim_feas:
				System.out.println("Feasible solution\n");
				for (int j = 0; j < numVariables; ++j)
					//System.out.format("%s: %.9f%n", varNames[j], decisionVars[j]);
				return decisionVars;
			case unknown:
				mosek.prosta prosta[] = new mosek.prosta[1];
				task.getprosta(mosek.soltype.itg, prosta);
				switch (prosta[0]) {
				case prim_infeas_or_unbounded:
					System.out.println("Problem status Infeasible or unbounded");
					break;
				case prim_infeas:
					System.out.println("Problem status Infeasible.");
					break;
				case unknown:
					System.out.println("Problem status unknown.");
					break;
				default:
					System.out.println("Other problem status.");
					break;
				}
				break;

			default:
				System.out.println("Other solution status");
				break;
			}
			return new double[numVariables];
		} catch (mosek.Exception e) {
			System.out.println("An error or warning was encountered");
			System.out.println(e.getMessage());
			throw e;
		}
	}


}
