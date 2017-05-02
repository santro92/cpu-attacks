package edu.colorado.optimizer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ParameterizedGrammerGenerator {

	public static final String PARAMETER = "{}";
	public static final String REGEX_PARAMETER = "\\{\\}";
	public static final String INPUT_FILE_NAME = "test_Grammar_param.txt";
	public static final String OUTPUT_FILE_NAME = "testGrammar3.txt";

	public static int getNoOfParameters(String fileName) {
		int no_of_parameters = 0;
		try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
			String line;
			while ((line = br.readLine()) != null) {
				int index = line.indexOf(PARAMETER);
				while (index >= 0) {
					index = line.indexOf(PARAMETER, index + 1);
					no_of_parameters++;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return no_of_parameters;
	}

	public static void generateTree(double[] vals) throws Exception {
		if (getNoOfParameters(INPUT_FILE_NAME) != vals.length) {
			throw new Exception("The no of parameters and vals dimension don't match");
		}

		try (BufferedReader br = new BufferedReader(new FileReader(INPUT_FILE_NAME));
				BufferedWriter bw = new BufferedWriter(new FileWriter(OUTPUT_FILE_NAME))) {
			int cnt = 0;
			String line;
			while ((line = br.readLine()) != null) {
				int index = line.indexOf(PARAMETER);
				while (index >= 0) {
					line = line.replaceFirst(REGEX_PARAMETER, String.valueOf((int) Math.round(vals[cnt++])));
					index = line.indexOf(PARAMETER);
				}
				bw.write(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
