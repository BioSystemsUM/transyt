package random;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import pt.uminho.ceb.biosystems.transyt.utilities.files.FilesUtils;

public class Random {

	public static void main(String[] args) throws IOException {
//		Map<String, String> reactionsRules = readGenesReactionsRuleFile("/Users/davidelagoa/Downloads/geneReactionRuleFile.txt");
//
//		booleanRulesParser(reactionsRules);

		test1();
	}
	
	private static void test1() {

		Map<String, String> allExistingReactions = FilesUtils.readMapFromFile("/Users/davidelagoa/TranSyT/workdir/system/dictionary_and_configurations/rectionsIDs.txt");

		Map<String, Set<String>> coiso = new HashMap<>();

		for(String metaID : allExistingReactions.keySet()) {

			String id = allExistingReactions.get(metaID);

			if(!coiso.containsKey(id)) {
				coiso.put(id, new HashSet<>());
			}
			
			coiso.get(id).add(metaID);
		}

		for(String id : coiso.keySet()) {
			
			if(coiso.get(id).size() > 1)
				System.out.println(id + "\t" + coiso.get(id));
			
		}
		System.out.println("done");
	}


	public static Map<String,String> readGenesReactionsRuleFile(String fileName) throws IOException{
		Map<String,String> map = new HashMap<String, String>();
		BufferedReader br = null;
		//		String fileName = dockerPath+"Bigg_Files/Results/"+"geneReactionRuleFile.txt";
		try {
			br = new BufferedReader(new FileReader(fileName));
			String line;
			while ((line = br.readLine()) != null) {
				String key = line.split("-")[0].trim();
				String value = line.split("-")[1].trim();
				map.put(key, value);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				br.close();
			}
		}
		return map;
	}


	/**
	 * @param reactionsRules
	 * @return
	 */
	public static Map<String,String> booleanRulesParser(Map<String, String> reactionsRules){

		Map<String, List<String>> newRules = new HashMap<>();
		Map<String, String> returnMap = new HashMap<>();

		for(String reaction : reactionsRules.keySet()) {

			try {
				String rule = reactionsRules.get(reaction).replace("\\s+", "\\s");

				if(!newRules.containsKey(reaction))
					newRules.put(reaction, new ArrayList<>());

				String[] genes = rule.split(" or ");

				Set<String> orGenesForAndRule = new HashSet<>();
				List<Set<String>> allOrGenesForAndRule = new ArrayList<>();

				for(int i = genes.length-1; i > -1; i--) {

					if(genes[i].endsWith(")") && !genes[i].contains(" and "))
						orGenesForAndRule.add(genes[i].replace(")", "").trim());

					else if(!orGenesForAndRule.isEmpty() && !genes[i].contains(" and ") && genes[i].contains("(")) {
						orGenesForAndRule.add(genes[i].replace("(", "").trim());	//prevent false positives

						allOrGenesForAndRule.add(orGenesForAndRule);

						List<Set<String>> res = getAllDistributions(allOrGenesForAndRule);
						newRules.get(reaction).addAll(getRulesFromDistributions("", res));

						allOrGenesForAndRule = new ArrayList<>();
						orGenesForAndRule = new HashSet<>();
					}

					else if(!orGenesForAndRule.isEmpty() && !genes[i].contains(" and "))
						orGenesForAndRule.add(genes[i].trim()); //in case of a or rule with 2+ genes

					else if(!orGenesForAndRule.isEmpty() && genes[i].matches(".*\\)\\s*and\\s*\\(.*")) {

						String[] splitRule = genes[i].split("\\)\\s*and\\s*\\(");

						if(splitRule.length > 1) {

							orGenesForAndRule.add(splitRule[1].replaceAll("[()]", "").trim());  //the list is being read backwards

							allOrGenesForAndRule.add(orGenesForAndRule);

							orGenesForAndRule = new HashSet<>();

							orGenesForAndRule.add(splitRule[0].replaceAll("[()]", "").trim());

							if(StringUtils.countMatches(genes[i], "(") > 1 && StringUtils.countMatches(genes[i], ")") < 2) {
								allOrGenesForAndRule.add(orGenesForAndRule);

								List<Set<String>> res = getAllDistributions(allOrGenesForAndRule);
								newRules.get(reaction).addAll(getRulesFromDistributions("", res));


								allOrGenesForAndRule = new ArrayList<>();
								orGenesForAndRule = new HashSet<>();
							}
						}

					}

					else if(!orGenesForAndRule.isEmpty()) {
						int index = genes[i].lastIndexOf("(");

						String orGene = genes[i].substring(index+1);
						String andGenes = genes[i].substring(0, index).replaceAll("[()]", "");
						orGenesForAndRule.add(orGene.trim());

						allOrGenesForAndRule = new ArrayList<>();

						List<Set<String>> res = getAllDistributions(allOrGenesForAndRule);
						newRules.get(reaction).addAll(getRulesFromDistributions(andGenes, res));


						allOrGenesForAndRule = new ArrayList<>();
						orGenesForAndRule = new HashSet<>();

					}
					else
						newRules.get(reaction).add(genes[i].replaceAll("[()]", "").trim());

				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		for(String reaction : newRules.keySet()) {

			if(newRules.get(reaction).isEmpty())
				returnMap.put(reaction, "");
			else
				returnMap.put(reaction, String.join(" or ", newRules.get(reaction)).replace("\\s+", "\\s"));
		}

		return returnMap;
	}

	/**
	 * @param prefix
	 * @param distributions
	 * @return
	 */
	private static List<String> getRulesFromDistributions(String prefix, List<Set<String>> distributions){

		List<String> res = new ArrayList<>();

		for(Set<String> set : distributions) {
			res.add(prefix + String.join(" and ", set));
		}
		return res;
	}



	/**
	 * @param sets
	 * @return
	 */
	private static List<Set<String>> getAllDistributions(List<Set<String>> sets){

		List<Set<String>> set1 = new ArrayList<>();
		List<Set<String>> set2 = new ArrayList<>();

		for(int i = 0; i < sets.size(); i++) {

			if(i == 0) {
				for(String met : sets.get(i)) {

					Set<String> set = new HashSet<>();

					set.add(met);
					set1.add(set);
				}
			}
			else
				set2.add(sets.get(i));
		}

		return executeDistributions(set1, set2, 0);

	}

	/**
	 * 
	 * @param set1
	 * @param set2
	 * @param cycle
	 * @return
	 */
	private static List<Set<String>> executeDistributions(List<Set<String>> set1, List<Set<String>> set2, int cycle){

		List<Set<String>> distributions = new ArrayList<>();

		if(cycle == 0 && set2.size() == 0) {

			for(Set<String> set : set1) {

				for(String item : set) {

					Set<String> newSet = new HashSet<>();

					newSet.add(item);
					distributions.add(newSet);

				}
			}
			return distributions;
		}

		if(cycle == set2.size()) 
			return set1;

		Set<String> l2 = set2.get(cycle);

		for(Set<String> er : set1) {

			for(String el: l2) {

				Set<String> erClone = new HashSet<>(er);

				erClone.add(el);

				distributions.add(erClone);
			}

		}

		set1.clear();

		set1.addAll(distributions);

		cycle++;

		return executeDistributions(set1, set2, cycle);

	}

}
