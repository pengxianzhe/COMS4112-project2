/* COMS 4112 project2
 * Xianzhe Peng, xp2155@columbia.edu
 * Siddharth Preetam, sp3567@columbia.edu
 * 
 * Main class for the project
 */

import java.io.*;
import java.util.*;

public class Main {
	public static void main(String[] args) {
		// check number of arguments
		if (args.length != 2) {
			System.out.println("Usage: java Main <query file> <config file>");
			System.exit(0);
		}
		
		String queryFilePath = args[0];
		String configFilePath = args[1];
		
		Map<String, Integer> configMap = new HashMap<String, Integer>();
		try {
			readConfig(configFilePath, configMap);
		} catch (FileNotFoundException e) {
			System.out.println("Config file not found, please check config file path");
			System.exit(0);
		}
	}
	
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
