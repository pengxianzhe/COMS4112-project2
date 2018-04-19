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
			
			// create array A for all 2^k - 1 subsets of set S, where the index is the integer representation of that subset
			// integer representation: bitmap <-> binary number <-> integer
			Element[] a = new Element[size];
			for (int i = 0; i < size; i++) {
				a[i] = new Element(pArray, k, i);
			}
			
			// perform the two steps in algorithm 4.11
			step1(a, configMap);
			step2(a, configMap);
			
			// print out the C code and cost for optimal plan
			printOutput(a);
		}
	}

	/**
	 * Compute the cost of a set for no branch algorithm under current config paramter settings
	 * @param a the element representing the set
	 * @param configMap map object that contains config parameters
	 * @return the cost of the set for no branch algorithm
	 */
	private static double getNoBranchCost(Element a, Map<String, Integer> configMap){
		int k = a.getN(); // number of basic terms in set a
		return k*configMap.get("r") + (k-1)*configMap.get("l") + k*configMap.get("f") + configMap.get("a");
	}

	/**
	 * Compute the cost of a set for logical and algorithm under current config parameter settings
	 * @param a the element representing the set
	 * @param configMap map object that contains config parameters
	 * @return the cost of the set for logical and algorithm
	 */
	private static double getlogicalAndCost(Element a, Map<String, Integer> configMap){
		int k = a.getN(); // number of basic terms in set a
		double combinedSelectivity = a.getP(); // selectivity of set a
		double q = Math.min(combinedSelectivity, 1 - combinedSelectivity);
		return k*configMap.get("r") + (k-1)*configMap.get("l") + k*configMap.get("f") + configMap.get("t") +
				q*configMap.get("m") + combinedSelectivity*configMap.get("a");
	}
	
	/**
	 * Perform the first step of the algorithm
	 * Compute the optimal cost for all non empty subsets using only &-terms and store it in c attribute
	 * in element object. Compare the cost of no branch algorithm and logical and algorithm. If the no 
	 * branch algorithm has a lower cost, also set b attribute in element object to true
	 * @param a the array of elements for all 2^k - 1 subsets of set S
	 * @param configMap map object that contains config parameters
	 */
	private static void step1(Element[] a, Map<String, Integer> configMap) {
		for(int i=1;i<a.length;i++){
			// compute no branch cost and logical and cost
			double logicalAndCost = getlogicalAndCost(a[i], configMap);
			double noBranchCost = getNoBranchCost(a[i], configMap);

			// compare the two costs and store the optimal cost
			if(logicalAndCost < noBranchCost)
				a[i].setC(logicalAndCost);
			else{
				a[i].setC(noBranchCost);
				a[i].setB(true);
			}
		}
	}
	
	/**
	 * Perform the second step of the algorithm
	 * Compute the optimal cost for all non empty subsets using mixed algorithm and store it in c attribute
	 * in element object. Also record the left child (first &-term) and right child (other terms) for the 
	 * optimal plan for backtrack.
	 * @param a the array of elements for all 2^k - 1 subsets of set S
	 * @param configMap map object that contains config parameters
	 */
	private static void step2(Element[] a, Map<String, Integer> configMap) {
		for (int i = 1; i < a.length; i++) {
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

	/**
	 * Compute the fixed cost of a subset under current config parameter settings
	 * @param a the array of elements for all 2^k - 1 subsets of set S
	 * @param i the integer representation of the subset
	 * @param configMap map object that contains config parameters
	 * @return fixed cost of the subset
	 */
	private static int fCost(Element[] a, int i, Map<String, Integer> configMap) {
		int n = a[i].getN(); // number of basic terms in set i
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

	private static void generateCode(List<Element> planIndices){
	    string output = "if(";
	    String innerTerm = "";
	    String noBranch = "";
	    String logicalAnd = "";
	    String branchingAnd = "";
	    string loopInner = "";

	    // code to add logical and terms
	    for(Element e: planIndices){
            Bitmap b = e.getBitmap();
            int index = -1;
            for(int i=0;i<b.length();i++){
                if(b[i].get(i) == true)
                    index = i+1;

                if(logicalAnd.length() > 0)
                    logicalAnd += " & ";
                logicalAnd += "t" + index + "[o" + n + "[i]]";
            }

            if(e.b == true){
                if(noBranch.length() > 0)
                    noBranch += " & ";
                noBranch += logicalAnd;
            }
            else{
                if(branchingAnd.length() > 0)
                    branchingAnd += " && ";
                branchingAnd += "(" + logicalAnd + ")";
            }
        }

        output += branchingAnd + ") {";

	    if(noBranch.length() == 0)
	        loopInner += "answer[j++] = i; \n }";
	    else
	        loopInner += "\t answer[j] = i; \n j+= (" + noBranch + "); \n }";

	    return output + loopInner;

    }

	private static void printOutput(Element[] a, int index, List<Element> planIndices) {
        Element e = Element[index];

        if (e.getL() == 0 && e.getR() == 0) {
            queryPlan.add(a[index]);
        } else {
            printOutput(a, a.getL(), planIndices);
            printOutput(a, a.getR(), planIndices);
        }
    }

	private static void finalOutput()
	
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
