import java.io.*;
import java.util.*;

/** 
 * COMS 4112 project2
 * Xianzhe Peng, xp2155@columbia.edu
 * Siddharth Preetam, sp3567@columbia.edu
 * 
 * Main class for the project.
 * It contains implementations for input/output and algorithm
 */

public class Main {
	public static void main(String[] args) {
		// check number of arguments
		if (args.length != 2) {
			System.out.println("Usage: java Main <query file> <config file>");
			System.exit(0);
		}
		
		// read query and config files
		String queryFilePath = args[0];
		String configFilePath = args[1];
		
		List<String> queryList = new ArrayList<String>();
		Map<String, Integer> configMap = new HashMap<String, Integer>();
		try {
			readQuery(queryFilePath, queryList);
			readConfig(configFilePath, configMap);
		} catch (FileNotFoundException e) {
			System.out.println("Qeury or config file not found, please check file path");
			System.exit(0);
		}
		
		// main loop
		for (String query : queryList) {
			// preprocessing query, compute number of basic terms and extract selectivity values
			System.out.println(query);
			String[] queryArray = query.trim().split(" ");
			int k = queryArray.length; // number of basic terms for this query
			int size = (int) Math.pow(2, k); // number of subsets for set S: 2 ^ k
			double[] pArray = new double[k]; // array of selectivity for each basic term, ith element in pArray is p_i
			for (int i = 0; i < k; i++) {
				pArray[i] = Double.parseDouble(queryArray[i].trim());
			}
			
			// create array A for all subsets of set S
			Element[] a = new Element[size];
			for (int i = 0; i < size; i++) {
				a[i] = new Element(pArray, k, i);
			}
			
			//TODO: Step 1
			//TODO: Step 2
			//TODO: print C code and final cost
		}
	}

	private static double getNoBranchCost(Element a, Map<String, Integer> configMap){
		int k = a.getN();

		double noBranchCost = k*configMap.get("r") + (k-1)*configMap.get("l") + k*configMap.get("f") + configMap.get("a");

		return noBranchCost;
	}

	private static double getlogicalAndCost(Element a, Map<String, Integer> configMap){
		int k = a.getN();
		double combinedSelectivity = a.getP();

		double q = Math.min(combinedSelectivity, 1 - combinedSelectivity);

		double logicalAndCost = k*configMap.get("r") + (k-1)*configMap.get("l") + k*configMap.get("f") + configMap.get("t") + q*configMap.get("m") + combinedSelectivity*configMap.get("a");

		return logicalAndCost;
	}
	
	private static void step1(Element[] a, Map<String, Integer> configMap) {
		for(int i=0;i<a.length;i++){
			double logicalAndCost = getlogicalAndCost(a[i], configMap);
			double noBranchCost = getNoBranchCost(a[i], configMap);

			double lowerCost = 0.0;
			if(logicalAndCost < noBranchCost)
				lowerCost = logicalAndCost;
			else{
				lowerCost = noBranchCost;
				a[i].setB(true);
			}

			a[i].setC(lowerCost);
		}
	}
	
	private static void step2(Element[] a, Map<String, Integer> configMap) {
		int size = a.length;
		for (int i = 1; i < size; i++) {
			// i is the integer representation of set s' union s
			for (int j = 1; j < i; j++) {
				// j is the integer representation of set s'
				int k = a[i].difference(a[j]); 
				// k is the integer representation of set s
				if (k == -1) {
					// j is not a subset of i, skip this combination
					continue;
				}
				
				double pj = a[j].getP(); // selectivity of set j
				int jFCost = fCost(a, j, configMap); // fixed cost of set j
				
				// prune the search space using lemma 4.8 and 4.9
				if (!cMetricCheck(a, pj, jFCost, k, configMap)) {
					// c metric of j is dominated by c metric of first &-term in k, skip this combination
					continue;
				}
				if (pj <= 0.5 && !dMetricCheck(a, pj, jFCost, k, true, configMap)) {
					// d metric of j is dominated by d metric of other &-term in k, skip this combination
					continue;
				}
				
				// compute the cost of set i for current combination
				double q = Math.min(pj, 1 - pj);
				double cost = jFCost + configMap.get("m") * q + pj * a[k].getC();
				
				// compare and update the cost of set i
				if (cost < a[i].getC()) {
					// the cost of current combination is the best so far
					a[i].setC(cost); // set the cost of i to the cost of this combination
					a[i].setL(j); // set the left child of i to j (s')
					a[i].setR(k); // set the right child of i to k (s)
				}
			}
		}
	}

	private static int fCost(Element[] a, int i, Map<String, Integer> configMap) {
		int n = a[i].getN(); // number of basic terms in set n
		return n * configMap.get("r") + (n - 1) * configMap.get("l") + n * configMap.get("f") + configMap.get("t");
	}
	
	private static boolean cMetricCheck(Element[] a, double pj, int jFCost, int k, Map<String, Integer> configMap) {
		int kLeft = a[k].getL(); // integer representation of left most &-term in k
		if (kLeft == 0) {
			// if the set k contains only one &-term, then kLeft is itself
			kLeft = k;
		}
		
		double pkLeft = a[kLeft].getP(); // selectivity of set kLeft
		int kLeftFCost = fCost(a, kLeft, configMap); // fixed cost of set kLeft
		return pkLeft > pj && (pkLeft - 1) / kLeftFCost >= (pj - 1) / jFCost;
	}
	
	private static boolean dMetricCheck(Element[] a, double pj, int jFCost, int i, boolean leftMost,
			Map<String, Integer> configMap) {
		int left = a[i].getL(); // integer representation of left child of i
		int right = a[i].getR(); // integer representation of right child of i
		if (left == 0 && right == 0) {
			// base case: set i is a &-term
			if (leftMost) {
				// i is the left most &-term, skip
				return true;
			}
			double pi = a[i].getP(); // selectivity of set i
			int iFCost = fCost(a, i, configMap); // fixed cost of set i
			return pi >= pj && iFCost >= jFCost;
		} else {
			// set i is not a &-term, find all &-terms in set i
			return dMetricCheck(a, pj, jFCost, left, leftMost, configMap) &&
					dMetricCheck(a, pj, jFCost, right, false, configMap);
		}
	}

	private static void printOutput(Element[] a) {
		
	}
	
	/**
	 * Read query file and store each line as an element in the query list
	 * @param queryFilePath path of query file
	 * @param queryList list to store queries
	 * @throws FileNotFoundException if query file is not found
	 */
	private static void readQuery(String queryFilePath, List<String> queryList) throws FileNotFoundException {
		Scanner s = new Scanner(new File(queryFilePath));
		while (s.hasNextLine()) {
			String line = s.nextLine();
			queryList.add(line);
		}
		s.close();
	}
	
	/**
	 * Read config file and store each line as an key value pair in the config map
	 * @param configFilePath path of config file
	 * @param configMap map to store config key value pairs
	 * @throws FileNotFoundException if config file is not found
	 */
	private static void readConfig(String configFilePath, Map<String, Integer> configMap) throws FileNotFoundException {
		Scanner s = new Scanner(new File(configFilePath));
		while (s.hasNextLine()) {
			String line = s.nextLine();
			String[] parts = line.split("=");
			String key = parts[0].trim();
			Integer value = Integer.parseInt(parts[1].trim());
			configMap.put(key, value);
		}
		s.close();
	}
}
