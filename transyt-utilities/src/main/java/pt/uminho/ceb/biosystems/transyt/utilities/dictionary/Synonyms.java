package pt.uminho.ceb.biosystems.transyt.utilities.dictionary;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pt.uminho.ceb.biosystems.transyt.utilities.files.FilesUtils;

public class Synonyms {

//	private static final String TOXINS_PATH = FilesUtils.getDictionatiesAndConfigurationsDirectory().concat("Toxins.txt");		//not in use
	private static final String DIC_PATH = FilesUtils.getDictionatiesAndConfigurationsDirectory().concat("dictionary.txt");
	private static final String HIERARCHIES_PATH = FilesUtils.getDictionatiesAndConfigurationsDirectory().concat("hierarchies.txt");

	private Map<String, Set<String>> dictionary;
	private Map<String, Set<String>> hierarchies;

	public Synonyms(){

		dictionary = FilesUtils.readDictionary(DIC_PATH, "\\$");
		hierarchies = FilesUtils.readDictionary(HIERARCHIES_PATH, "\\$");
	}

	/**
	 * Get metabolite synonym;
	 * 
	 * @param word
	 * @return
	 */
	public String getSynonym(String word){

		//		if(word.trim().endsWith("ose"))
		//			word = "sugar";

		word = word.replaceAll("±", "").replaceAll(";$", "");
		
		for(String key : dictionary.keySet()){
			
			Set<String> set = dictionary.get(key);

			if(set.contains(word))
				return key;
		}
		return null;
	}

	

	/**
	 * Get entire metabolites dictionary.
	 * 
	 * @return
	 */
	public Map<String,Set<String>> getMetabolitesDictionary(){

		return dictionary;

	}

	/**
	 * Get entire hierarchies dictionary.
	 * 
	 * @return
	 */
	public Map<String,Set<String>> getHierarchiesDictionary(){

		return hierarchies;

	}

	/**
	 * Check if a metabolite is child of one other.
	 * 
	 * @return
	 */
	public Boolean isChildOf(String key, String child){

		if(this.hierarchies.containsKey(key)) {

			if(this.hierarchies.get(key).contains(child.toLowerCase().replaceAll("\\s+", "")))
				return true;
			else
				return false;
		}

		return false;

	}

	/**
	 * Method to replace a general sugar by the most specific sugar available
	 * 
	 * @param reactant
	 * @param product
	 * @param metabolites
	 * @param tc
	 * @return
	 */
	public Object[] correctSugars(String reactant, String product, List<String> metabolites) {

		Object[] regex = null;

		if(reactant.matches("(?i).*sugars*\\s+.*") || product.matches("(?i).*sugars*\\s+.*")) {

			//			if(tc.equals("4.A.6.1.9 Q97N91"))
			//				System.out.println("entrou");

			for(String metabolite : metabolites) {

				if(metabolite.endsWith("ose")) {

					regex = new Object[3];

					reactant = reactant.replaceAll("(?i)sugars*", metabolite);
					product = product.replaceAll("(?i)sugars*", metabolite);


					//					if(tc.equals("4.A.6.1.9 Q97N91"))
					//						System.out.println("reaction = " + reactant + "\t" + product);

				}
			}

			if(regex != null) {
				regex[0] = reactant;
				regex[1] = product;
				regex[2] = metabolites;

			}
		}
		if(reactant.matches("(?i).*sugars*\\-P\\s+.*") || product.matches("(?i).*sugars*\\-P\\s+.*")) {

			for(String metabolite : metabolites) {

				if(metabolite.endsWith("ose")) {

					regex = new Object[3];

					reactant = reactant.replaceAll("(?i)sugars*\\-P", metabolite);		//FALTA acrescentar 1 ou 6 phosphate
					product = product.replaceAll("(?i)sugars*\\-P", metabolite);		//FALTA acrescentar 1 ou 6 phosphate

				}
			}

			if(regex != null) {
				regex[0] = reactant;
				regex[1] = product;
				regex[2] = metabolites;

			}
		}

		for(String metabolite : metabolites) {

			if(metabolite.equals("Sugar")) {

				Pattern p = Pattern.compile("\\b(?i)\\w+ose\\b");
				Matcher m = p.matcher(reactant);

				if(m.find()) {

					metabolites.remove("Sugar");
					metabolites.add(m.group());
				}

				p = Pattern.compile("\\b(?i)\\w+ose\\b");
				m = p.matcher(reactant);

				if(m.find()) {

					metabolites.remove("Sugar");
					metabolites.add(m.group());
				}

				regex = new Object[3];

				regex[0] = reactant;
				regex[1] = product;
				regex[2] = metabolites;

			}
		}

		return regex;
	}

//	/**
//	 * Method to replace a general toxin by the most specific toxin available
//	 * 
//	 * @param reactantMetabolites
//	 * @param metabolites
//	 * @return
//	 */
//	public Map<String, String> correctToxins(Map<String, String> reactantMetabolites, Set<String> metabolites) {
//
//		Set<String> allToxins = FilesUtils.readWordsInFile(TOXINS_PATH);
//
//		Map<String, String> newMap	= new HashMap<>();
//
//		for(String key : reactantMetabolites.keySet()) {
//
//			if(key.equals("Toxin")) {
//
//				for(String metab : metabolites) {
//
//					if(allToxins.contains(metab))
//						newMap.put(metab, reactantMetabolites.get(key));
//				}
//			}
//			else
//				newMap.put(key, reactantMetabolites.get(key));
//		}
//
//		return newMap;
//	}



}
