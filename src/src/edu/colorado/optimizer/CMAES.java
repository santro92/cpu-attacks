package edu.colorado.optimizer;

import fr.inria.optimization.cmaes.CMAEvolutionStrategy;

public class CMAES 
{
	int dimension;
	FitFunc func;
	
	CMAES(int dimension, FitFunc func) 
	{
		this.dimension = dimension;
		this.func = func;
	}
	
	public void run() throws Exception 
	{	
		CMAEvolutionStrategy cma = new CMAEvolutionStrategy();
		cma.readProperties();
		cma.setDimension(this.dimension);
		cma.setInitialX(10);
		cma.setInitialStandardDeviation(5);
		cma.options.stopFitness = -1E7;
		double[] fitness = cma.init();
		cma.writeToDefaultFilesHeaders(0);

		while (cma.stopConditions.getNumber() == 0) 
		{
			double[][] pop = cma.samplePopulation();
			System.out.println("pop length -->" + pop.length);
			for (int i = 0; i < pop.length; ++i) 
			{
				while (!func.isFeasible(pop[i]))
				{
					pop[i] = cma.resampleSingle(i);
				}
				fitness[i] = -1 * func.valueOf(pop[i]);
			}
			cma.updateDistribution(fitness);
			cma.writeToDefaultFiles();
			int outmod = 150;
			if (cma.getCountIter() % (15 * outmod) == 1)
			{
				cma.printlnAnnotation();
			}
			if (cma.getCountIter() % outmod == 1)
			{
				cma.println();
			}
		}
		
		cma.setFitnessOfMeanX(func.valueOf(cma.getMeanX()));
		cma.writeToDefaultFiles(1);
		cma.println();
		cma.println("Terminated due to");
		for (String s : cma.stopConditions.getMessages())
		{
			cma.println("  " + s);
		}
		cma.println("best function value " + cma.getBestFunctionValue() + " at evaluation " + cma.getBestEvaluationNumber());
	}
	
	public static void main(String args[]) throws Exception 
	{
		int dimension = ParameterizedGrammerGenerator.getNoOfParameters(ParameterizedGrammerGenerator.INPUT_FILE_NAME);
		CMAES cmaes = new CMAES(dimension, new FitFunc());
		cmaes.run();
	}
}
