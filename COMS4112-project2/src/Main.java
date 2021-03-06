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
		StringBuffer sb = new StringBuffer();
		for (String query : queryList) {
			// preprocessing query, compute number of basic terms and extract selectivity values
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
			
			// create the C code and cost for optimal plan
			formatOutput(a, query, sb);
		}
		
		// print output for all queries
		sb.append("==================================================================");
		System.out.println(sb.toString());
		try {
			writeFile("output.txt", sb.toString());
		} catch (FileNotFoundException e) {
			System.out.println("Cannot create or write to file output.txt");
			System.exit(0);
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
	
	/**
	 * Compare the c metric of the &-term j and the left most &-term in subset i and return the result
	 * @param a the array of elements for all 2^k - 1 subsets of set S
	 * @param pj the selectivity of &-term j
	 * @param jFCost the fixed cost of &-term j
	 * @param i the integer representation of subset
	 * @param configMap map object that contains config parameters
	 * @return false if c metric of j is dominated by the c metric of left most &-term in i, true otherwise
	 */
	private static boolean cMetricCheck(Element[] a, double pj, int jFCost, int i, Map<String, Integer> configMap) {
		int iLeft = a[i].getL(); // integer representation of left most &-term in set i
		if (iLeft == 0) {
			// if the set i contains only one &-term, then iLeft is itself
			iLeft = i;
		}
		
		double piLeft = a[iLeft].getP(); // selectivity of set iLeft
		int iLeftFCost = fCost(a, iLeft, configMap); // fixed cost of set iLeft
		return piLeft > pj || (piLeft - 1) / iLeftFCost >= (pj - 1) / jFCost;
	}

	/**
	 * Compare the d metric of the &-term j with all other (not the left most) &-term in subset i and return the result
	 * @param a the array of elements for all 2^k - 1 subsets of set S
	 * @param pj the selectivity of &-term j
	 * @param jFCost the fixed cost of &-term j
	 * @param i the integer representation of subset
	 * @param leftMost whether current subset or &-term is left-most in i
	 * @param configMap object that contains config parameters
	 * @return false if d metric of j is dominated by the d metric of one of 'other' &-terms in i, true otherwise
	 */
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
            return pi >= pj || iFCost >= jFCost;
        } else {
            // set i is not a &-term, find all &-terms in set i
            return dMetricCheck(a, pj, jFCost, left, leftMost, configMap) &&
                    dMetricCheck(a, pj, jFCost, right, false, configMap);
        }
    }

    /**
     * Construct the optimal plan from the last element (for the entire set) and append its c code to string buffer
     * @param a a the array of elements for all 2^k - 1 subsets of set S
     * @param index index of current element object
     * @param last if current element is the last subset in the set
     * @param sb string buffer to append to
     */
	private static void backtrackPlan(Element[] a, int index, boolean last, StringBuffer sb) {
        Element e = a[index];

        if (e.getL() == 0 && e.getR() == 0) {
        	// base case: current subset index is an &-term, construct code for it
        	String logicalAnd = ""; // string for current &-term, connected by logical and &
        	Bitmap b = e.getBitmap(); // bitmap of current &-term
            int count = 0; // number of basic terms in current &-term
            for(int i = 0;i < b.length(); i++) {
                if (b.get(i) == true) {
                	// basic term i is included in current &-term, construct string and increment count
                    count++;
                    int num = i+1;
	                if (logicalAnd.length() > 0)
	                    logicalAnd += " & ";
	                logicalAnd += "t" + num + "[o" + num + "[i]]";
                }
            }
            
            // if there are more than 1 basic terms, put parenthesis () to wrap the &-term
            if (count != 1) {
        		logicalAnd = "(" + logicalAnd + ")";
        	}
            
            if (last) {
            	// current &-term is last &-term, check for no branch
            	if (e.isB()) {
            		// current &-term is using no branch
                	if (index != a.length - 1) {
                    	// current element is not the entire set, delete the extra &&, 
                		// move string to later lines and finish the if condition
                    	sb.delete(sb.length() - 4, sb.length());
                		sb.append(") {\n\tanswer[j] = i;\n\tj+= " + logicalAnd + ";\n}\n");
                	} else {
                		// current element is the entire set, remove if condition, and add increment codes
                		sb.delete(sb.length() - 3, sb.length());
                		sb.append("answer[j] = i;\nj+= " + logicalAnd + ";\n");
                	}
            	} else {
            		// current &-term is not using no branch, append string and close the if condition
            		sb.append(logicalAnd);
            		sb.append(") {\n\tanswer[j++] = i;\n}");
            	}
            } else {
            	sb.append(logicalAnd);
            }
        } else {
        	// current subset index is not an &-term, recursively combine all &-terms in it
            backtrackPlan(a, a[index].getL(), false, sb);
            sb.append(" && ");
            backtrackPlan(a, a[index].getR(), last, sb);
        }
    }
	
	/**
	 * Construct the output string for a query and append it to string buffer
	 * @param a the array of elements for all 2^k - 1 subsets of set S for that query
	 * @param query the input string for that query
	 * @param sb string buffer to append to
	 */
	private static void formatOutput(Element[] a, String query, StringBuffer sb) {
		// query string
		sb.append("==================================================================\n");
		sb.append(query);
		sb.append('\n');
		
		// the C code for optimal plan
		sb.append("------------------------------------------------------------------\n");
		sb.append("if(");
		backtrackPlan(a, a.length - 1, true, sb);
		
		// cost for optimal plan
		sb.append("------------------------------------------------------------------\n");
		sb.append("cost: " + a[a.length - 1].getC());
		sb.append('\n');
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
	
	/**
	 * Remove the content of output file and write a string to that file. The file is created if it doesn't exist
	 * @param outputFilePath path of output file
	 * @param output string to write
	 * @throws FileNotFoundException if output file is not found and cannot be created
	 */
	private static void writeFile(String outputFilePath, String output) throws FileNotFoundException {
		PrintStream outFile = new PrintStream(new File(outputFilePath));
		outFile.println(output);
		outFile.close();
	}
}
