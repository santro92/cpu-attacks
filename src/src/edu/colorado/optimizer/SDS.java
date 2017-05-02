package edu.colorado.optimizer;

import java.io.IOException;
import java.util.Random;

public class SDS {
	FitFunc func;
	int nDim;
	int min = 1;
	int max = 5;
	int noOfAgents = 20;
	int maxIteration = 30;
	int fitnessValue = 10;
	double agentPosition[][];
	double agentFitnessValue[] = new double[noOfAgents];
	Boolean agentState[] = new Boolean[noOfAgents];

	public SDS(int nDim, FitFunc func) throws IOException {
		this.nDim = nDim;
		this.func = func;
		agentPosition = new double[noOfAgents][nDim];
	}

	private void setAgentInitialPos() {
		Random random = new Random();
		for (int i = 0; i < noOfAgents; i++) {
			for (int j = 0; j < nDim; j++) {
				agentPosition[i][j] = random.nextInt(max - min + 1) + min;
			}
		}
	}

	private void setAgentNewPos() {
		Random random = new Random();
		for (int i = 0; i < noOfAgents; i++) {
			if (agentState[i] == Boolean.FALSE) {
				int handshakeAgent = random.nextInt(noOfAgents);
				if (agentState[handshakeAgent] == Boolean.TRUE) {
					for (int j = 0; j < nDim; j++) {
						agentPosition[i][j] = agentPosition[handshakeAgent][j] + 1;
					}
				} else {
					for (int j = 0; j < nDim; j++) {
						agentPosition[i][j] = random.nextInt(max - min + 1) + min;
					}
				}
			}
		}
	}

	private void calculateAgentsFitness() throws Exception {
		for (int i = 0; i < noOfAgents; i++) {
			agentFitnessValue[i] = func.valueOf(agentPosition[i]);
		}
	}

	private void calculateAgentState() {
		Random random = new Random();
		for (int i = 0; i < noOfAgents; i++) {
			if (agentFitnessValue[i] > agentFitnessValue[random.nextInt(noOfAgents)]) {
				agentState[i] = Boolean.TRUE;
			} else {
				agentState[i] = Boolean.FALSE;
			}
		}
	}

	private double maxFitnessValue() {
		double max = agentFitnessValue[0];
		int agentIndex = 0;
		for (int i = 1; i < noOfAgents; i++) {
			if (agentFitnessValue[i] > max) {
				max = agentFitnessValue[i];
				agentIndex = i;
			}
		}
		System.out.println("Max value.. " + max);
		
		System.out.print(agentPosition[agentIndex][0]);
		for(int i=1; i<nDim; i++) {
			System.out.print("," + agentPosition[agentIndex][1]);
		}
		System.out.println();
		return max;
	}

	public void run() throws Exception {
		int count = 0;

		/* Initialisation-phase */
		setAgentInitialPos();

		while (count < maxIteration) {
			System.out.println("***************" + (count + 1) + "***************");

			/* Fitness Calculation */
			calculateAgentsFitness();

			/* Test Phase */
			calculateAgentState();

			/* Objective Check */
			if (maxFitnessValue() > fitnessValue) {
				break;
			}

			/* Diffusion Phase */
			setAgentNewPos();
			count++;
		}
	}
	
	public static void main(String args[]) throws Exception {
		int dimension = ParameterizedGrammerGenerator.getNoOfParameters(ParameterizedGrammerGenerator.INPUT_FILE_NAME);
		SDS sds = new SDS(dimension, new FitFunc());
		sds.run();
	}
}
