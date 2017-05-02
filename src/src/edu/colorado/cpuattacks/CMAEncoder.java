package edu.colorado.cpuattacks;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import fr.inria.optimization.cmaes.CMAEvolutionStrategy;
import fr.inria.optimization.cmaes.fitness.IObjectiveFunction;

import edu.colorado.cpuattacks.MosekEncoder;

public class CMAEncoder {

	class Rosenbrock implements IObjectiveFunction { // meaning implements methods valueOf and isFeasible
		public double valueOf (double[] x) {
			double res = 0;
			for (int i = 0; i < x.length-1; ++i)
				res += 100 * (x[i]*x[i] - x[i+1]) * (x[i]*x[i] - x[i+1]) + 
				(x[i] - 1.) * (x[i] - 1.);
			return res;
		}
		public boolean isFeasible(double[] x) {return true; } // entire R^n is feasible
	}
	
	class GrammarProblem implements IObjectiveFunction { // meaning implements methods valueOf and isFeasible
		double[] objectiveFn; // the coefficients of the objective function
		double[]  capacities; // the capacity constraints associated with each production
		
		public GrammarProblem(double[] objFn, double[] capacities){
			this.objectiveFn = objFn;
			this.capacities  = capacities;
		}
		
		public double valueOf (double[] x) {
			double res = 0.0;
			for(int i=0; i < x.length; i++)
				res += objectiveFn[i]*x[i];
			return -res;
		}
		
		public boolean isFeasible(double[] x) {
			boolean feas = true;
			for(int i=0; i < x.length; i++) {
				x[i] = Math.floor(x[i]);
				feas &= (0 <= x[i]);        // impose the lower bound
				if (capacities[i] >= 0)	    // if upper bounded (i.e., not -1)
					feas &= (x[i] <= capacities[i]); // impose the upper bound
			}
			return feas; 
		} 
	}
	
	public double[] encodeGrammar(CCFG gram){ 
		// we need to compute the objective function (valueOf input) and flow constraints (isFeasible)
		HashMap<String, Integer> costs = gram.getCostFunction();
		Collection<Production> productions = gram.getProductions();
		int numVariables = productions.size();
		Iterator<Production> prodIter = productions.iterator(); 
		
		double[] obj = new double[numVariables]; // initialize objective function
		for (int i=0; i < numVariables; i++) obj[i] = 0.0;
		
		double[] cap = new double[numVariables]; // initialize capacity constraints
		for (int i=0; i < numVariables; i++) cap[i] = 0.0;
		
		// initialize flow constraints
		HashMap<String, ArrayList<Integer>> ins  = new HashMap<String, ArrayList<Integer>>();
		HashMap<String, ArrayList<Integer>> outs = new HashMap<String, ArrayList<Integer>>();

		
		prodIter.forEachRemaining(p -> {
			int prodID = p.getID();
			LinkedList<Identifier> rhs = p.getRHS();
			// compute the objective function
			rhs.stream().filter(id->(id instanceof Terminal))
				.forEach(t->obj[prodID] += costs.get(t.getName()));
			// compute the capacity constraints
			cap[prodID] = p.getCapacity();
		});
		
		System.out.println("Objective function  : " + MosekEncoder.arrayToString(obj));
		System.out.println("Capacity constraints: " + MosekEncoder.arrayToString(cap));

		// proceed with setting up the call to CMA-ES 
		CMAEncoder thisObj = new CMAEncoder();
		IObjectiveFunction fitfun = thisObj.new GrammarProblem(obj,cap);

		// new a CMA-ES and set some initial values
		CMAEvolutionStrategy cma = new CMAEvolutionStrategy();
		cma.readProperties(); // read options, see file CMAEvolutionStrategy.properties
		cma.setDimension(numVariables); // overwrite some loaded properties
		cma.setInitialX(1.00); // in each dimension, also setTypicalX can be used
		cma.setInitialStandardDeviation(0.5); // also a mandatory setting 
		cma.options.stopFitness = 1e-14;       // optional setting

		// initialize cma and get fitness array to fill in later
		double[] fitness = cma.init();  // new double[cma.parameters.getPopulationSize()];

		// initial output to files
		cma.writeToDefaultFilesHeaders(0); // 0 == overwrites old files

		// iteration loop
		while(cma.stopConditions.getNumber() == 0) {

            // --- core iteration step ---
			double[][] pop = cma.samplePopulation(); // get a new population of solutions
			for(int i = 0; i < pop.length; ++i) {    // for each candidate solution i
            	// a simple way to handle constraints that define a convex feasible domain  
            	// (like box constraints, i.e. variable boundaries) via "blind re-sampling" 
            	                                       // assumes that the feasible domain is convex, the optimum is  
				while (!fitfun.isFeasible(pop[i]))     //   not located on (or very close to) the domain boundary,  
					pop[i] = cma.resampleSingle(i);    //   initialX is feasible and initialStandardDeviations are  
                                                       //   sufficiently small to prevent quasi-infinite looping here
                // compute fitness/objective value	
				fitness[i] = fitfun.valueOf(pop[i]); // fitfun.valueOf() is to be minimized
			}
			cma.updateDistribution(fitness);         // pass fitness array to update search distribution
            // --- end core iteration step ---

			// output to files and console 
			cma.writeToDefaultFiles();
			int outmod = 150;
			if (cma.getCountIter() % (15*outmod) == 1)
				cma.printlnAnnotation(); // might write file as well
			if (cma.getCountIter() % outmod == 1)
				cma.println(); 
		}
		// evaluate mean value as it is the best estimator for the optimum
		cma.setFitnessOfMeanX(fitfun.valueOf(cma.getMeanX())); // updates the best ever solution 

		// final output
		cma.writeToDefaultFiles(1);
		cma.println();
		cma.println("Terminated due to");
		for (String s : cma.stopConditions.getMessages())
			cma.println("  " + s);
		cma.println("best function value " + cma.getBestFunctionValue() 
				+ " at evaluation " + cma.getBestEvaluationNumber());
		cma.println("best function value at " + MosekEncoder.arrayToString(cma.getBestX())
				+ " is : " + fitfun.valueOf(cma.getBestX()));
		
		// we might return cma.getBestSolution() or cma.getBestX()
		return cma.getBestX();
	} // main  
	
}
