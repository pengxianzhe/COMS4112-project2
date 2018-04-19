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
			//TODO: print final cost
		}
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
