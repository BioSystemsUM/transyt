package pt.uminho.ceb.biosystems.transyt.scraper.tcdb.tcdbTransportTypesRetriever;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.uminho.ceb.biosystems.transyt.utilities.capsules.ReactionContainer;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.TcNumberContainer;
import pt.uminho.ceb.biosystems.transyt.utilities.enumerators.Compartments;
import pt.uminho.ceb.biosystems.transyt.utilities.files.FilesUtils;

public class ProcessCompartments {

	private static final String DIC_PATH = FilesUtils.getDictionatiesAndConfigurationsDirectory().concat("dictionaryComp.txt");
	private static final List<String> EXCEPTIONS = List.of ("5.B.9", "5.B.8", "5.B.1", "3.A.6", "5.B.12",		//this should go to the exceptions file
			"3.A.21", "1.A.25", "2.A.9", "2.A.64", "1.E.20", "3.D.10", "1.B.53", "9.B.35", "9.A.41", "1.A.17", "1.A.3",
			"3.D.9", "4.E.1","1.B.8","2.A.1", "9.B.16");  
	
	private static final Logger logger = LoggerFactory.getLogger(ProcessCompartments.class);

	
	/**
	 * Method to process the information of the compartments in order to identify if they are "in" or "out"
	 * 
	 * @param data
	 */
	public static Map<String, TcNumberContainer> processCompartments(Map<String, TcNumberContainer> data) {		//aparecem muitas excepcoes! implementar logger
		
		String[] reaction;

		Map<String, Set<String>> dictionary = readCompartmentsDictionary();
		
		Compartments[] compartmentsList = Compartments.values();

		//		System.out.println(data.keySet());

		for(String tc : data.keySet()) {

			TcNumberContainer tcContainer = data.get(tc);

				for(int id : tcContainer.getAllReactionsIds()){
					
					try {
						boolean go = true;
						
						ReactionContainer reactContainer = tcContainer.getReactionContainer(id);
						
//						if(tc.equals("1.A.17"))
//							System.out.println(reactContainer.getReaction());
//						
						String reactant = reactContainer.getReactant().replaceAll("in the ", "").replaceAll("\\(non-selective\\)\\s*", "").replaceAll("\\(and other compounds\\)\\s*", "and compounds")
								.replaceAll("\\(NTP\\) ", "").replaceAll("ions\\(out\\)", "ions \\(out\\)").replaceAll("ions\\(in\\)", "ions \\(in\\)").replaceAll("\\(\\?\\)\\s*", "")
								.replaceAll("\\(and other cations\\)* ", "and cations").replaceAll("with slight anion selectivity", "").replaceAll("anion selective;*\\s*", "").replaceAll("\\(Anionic\\)", "Anionic")
								.replaceAll("\\+\\(", "\\+ \\(").replaceAll("s\\(", "s \\(").replaceAll("\\(cation-selective\\)", "");
						
						String product = reactContainer.getProduct().replaceAll("in the ", "").replaceAll("\\(non-selective\\)\\s*", "").replaceAll("\\(and other compounds\\)\\s*", "and compounds")
								.replaceAll("\\(NTP\\) ", "").replaceAll("ions\\(out\\)", "ions \\(out\\)").replaceAll("ions\\(in\\)", "ions \\(in\\)").replaceAll("\\(\\?\\)\\s*", "")
								.replaceAll("\\(and other cations\\)* ", "and cations").replaceAll("with slight anion selectivity", "").replaceAll("anion selective;*\\s*", "").replaceAll("\\(Anionic\\)", "Anionic")
								.replaceAll("\\+\\(", "\\+ \\(").replaceAll("s\\(", "s \\(").replaceAll("\\(cation-selective\\)", "");
						
						if(product.substring(product.length()-1).equals(","))
							product = product.substring(0,product.length()-1);
						
						if(product.matches(".+\\(.+\\)\\s*\\(periplasm of Gram-negative bacteria\\)")) 
							product = product.replaceAll("\\(periplasm of Gram-negative bacteria\\)", "");
							
						if(reactant.matches(".+\\(.+\\)\\s*\\(periplasm of Gram-negative bacteria\\)"))
							reactant = reactant.replaceAll("\\(periplasm of Gram-negative bacteria\\)", "");
						
						
//						if(tc.equals("1.A.17"))
//							System.out.println(product);

						if(EXCEPTIONS.contains(tc)) {
							
							reaction = processException(tc, reactant, product, id);
							
							if(reaction != null) {
								
								reactant = reaction[0];
								product = reaction[1];
								
								go = false;
							}
						}
						
//						if(tc.equals("3.B.1"))
//							System.out.println(product);
						
						if(go){
							
//							System.out.println("PROD " + product);
							
							Map<String, String> reactCompToReplace = getCompartmentsFromDictionary(reactant, dictionary, tc);
							Map<String, String> reactProdToReplace = getCompartmentsFromDictionary(product, dictionary, tc);
							
							
//							if(tc.equals("2.A.39"))
//								System.out.println(reactCompToReplace);
//							if(tc.equals("2.A.39"))
//								System.out.println(reactProdToReplace);
							
							for(String key : reactCompToReplace.keySet()) 
								reactant = reactant.replaceAll(key, reactCompToReplace.get(key));

							for(String key : reactProdToReplace.keySet()) 
								product = product.replaceAll(key, reactProdToReplace.get(key));

						}
						
//						if(tc.equals("3.A.5"))
//							System.out.println(reactant + "\t" +product);
						
//						reactContainer.replaceReactant(reactant);		//delete after tests
//						reactContainer.replaceProduct(product);			//delete after tests
						
						try {
							reaction = findCompartmentsRelativePosition(reactant, product, compartmentsList);
							
							reactant = reaction[0];
							product = reaction[1];
							
//						if(tc.equals("2.A.39"))
//							System.out.println(reactant + "\t" +product);
							
							reactContainer.replaceReactant(reactant);
							reactContainer.replaceProduct(product);
						} catch (Exception e) {
							System.out.println(tc);
							e.printStackTrace();
						}
						
					} 
					
					catch (Exception e) {
						
						e.printStackTrace();
					}
				}
		}
		return data;

	}

	/**
	 * Get compartments to replace by alias for reactants or products
	 * 
	 * @param reaction
	 * @param dictionary
	 * @return
	 */
	private static Map<String, String> getCompartmentsFromDictionary(String reaction, Map<String, Set<String>> dictionary, String tc) {
		
		Map<String, String> compartments = new HashMap<>();

		String[] metabolites = reaction.split("\\s\\+\\s");

//		if(tc.equals("2.A.39"))
//			System.out.println(metabolites[0]);
		
		for(int i = 0; i < metabolites.length; i++) {

			if(metabolites[i].contains("(") && !metabolites[i].contains("(or")){

				//				System.out.println(metabolites[i]);

				String[] text = metabolites[i].split("\\s\\(");

				//				System.out.println(text[0]);

				for(int j = 0; j < text.length; j++) {
					
					if(text[j].contains(")")) {
						
						text[j] = text[j].substring(0, text[j].indexOf(")"));
						
						String value;
						
//						if(text[j].contains(" or "))
//							text[j] = text[j].split(" or ")[0];
							
						if(text[j].contains(" or "))
							value = text[j].split(" or ")[0];
						else
							value = text[j];
						
						value = value.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
						
//						if(value.contains(" or "))
//							value = value.split(" or ")[0];
						
//						if(tc.equals("3.A.5"))
//							System.out.println(value);
//						
//						if(tc.equals("3.A.5"))
//							System.out.println(text[j]);
						
						
						for(String alias : dictionary.keySet()) {
							
							if(dictionary.get(alias).contains(value.trim())) {
								compartments.put(text[j], alias);
								break;
							}
						}
					}
				}
			}
		}
		
		return compartments;
	}
	
	/**
	 * Process compartments exceptions
	 * 
	 * @param tc
	 * @param reactant
	 * @param product
	 * @param id
	 * @return
	 */
	private static String[] processException(String tc, String reactant, String product, int id) {
		
		String[] reaction = new String[2];
		
			if(tc.equals("5.B.9") && id == 0) {
				
				reactant = reactant.replaceAll("electron from ", "");
				product = product.replace(" a periplasmic protein", "");
			}
			else if(tc.equals("5.B.8") && id == 0) {
				
				reactant = reactant.replaceAll("e.g., in reduced cytochrome periplasm", "in").replace("extracellular", "out");
				product = product.replace("extracellular", "out");
			}
			else if(tc.equals("5.B.1") && id == 1) {
				
				product = product.replaceAll("\\(superoxide\\) ", "");
			}
			else if(tc.equals("3.A.6") && id == 0) {
				
				reactant = reactant.replaceAll("bacterial cytoplasm", "in");
				product = product.replaceAll("out or host cell cytoplasm", "out");
			}
			else if(tc.equals("5.B.12") && id == 0) {
				
				reactant = reactant.replaceAll("periplasmic electron donor", "out");
				product = product.replaceAll("cytoplasmic sulfite reductase, DsrABC", "in");
			}
			else if(tc.equals("3.A.21") && id == 0) {
				
				//membrane inserted -> endoplasmatic reticulum
				
				product = product.replaceAll("membrane inserted", "in");
				reactant = reactant.replaceAll("cytoplasm", "out");
			}
			else if(tc.equals("1.A.25") && id == 1) {
				
				reactant = reactant.replaceAll("cell cytoplasm", "in");
			}
			else if(tc.equals("2.A.9") && id == 0) {
				
				product = product.replaceAll("\\(membrane\\) or", "\\(mitochondria\\)");
			}
			else if(tc.equals("1.E.20") && id == 0) {
				
				reactant = reactant.replaceAll("\\(Lys\\)in", "\\(in\\)");
				product = product.replaceAll("\\(Lys\\)out", "\\(out\\)");
			}
			else if(tc.equals("3.D.10") && id == 0) {
				
				reactant = reactant.replaceAll("membrane", "out");
				product = product.replaceAll("membrane", "out");
			}
			else if(tc.equals("1.B.53") && id == 0) {
				
				reactant = reactant.replaceAll("phage coat", "out").replaceAll("phage particles", "out");
				product = product.replaceAll("bacterial envelope", "in").replaceAll("bacterial cytoplasm", "in");
			}
			else if(tc.equals("9.B.35") && id == 0) {
				
				reactant = reactant.replaceAll("blood", "out");
				product = product.replaceAll("brain", "in");
			}
			else if(tc.equals("9.A.41") && id == 0) {
				
				product = product.replaceAll("cell surface", "in");
			}
			else if(tc.equals("1.A.17") && id == 1) {		//this exception should not be necessary, something is happening while retrieving the reaction. Check this later
				
				reactant = reactant.replaceAll("\\(e.g., Ca2\\+\\) ", "");
				product = product.concat(" " + ReactionContainer.INTERIOR_COMPARTMENT_TOKEN);
			}
			else if(tc.equals("1.A.3") && id == 0) {		
				
				reactant = reactant.replaceAll("out, or sequestered ER or SR", "out");
				product = product.replaceAll("cell cytoplasm", "in");
			}
			else if(tc.equals("3.D.9") && id == 0) {		
				
				reactant = reactant.replaceAll("\\(2e-\\) ", "");
				product = product.replaceAll("\\(2e-\\) ", "");
			}
			else if(tc.equals("4.E.1") && id == 0) {		
				
				reactant = reactant.replaceAll("cytoplasm", "\\(cytoplasm\\)");
				product = product.replaceAll("valuolar lumen", "\\(vacuoles\\)").replaceAll("\\(P\\)", "");
			}
			else if(tc.equals("1.B.8") && id == 0) {		
				
				product = product.replaceAll("intermembrane space", "in");
			}
			else if(tc.equals("2.A.1") && id == 2) {		
				
				product = product.replaceAll("\\(S1 may be H\\+ or a solute\\)", "");
			}
			else if(tc.equals("5.A.3") && id == 0) {		
				
				reactant = reactant.replaceAll("\\(NO3\\-\\) ", "");
				product = product.replaceAll("\\(NO2\\-\\) ", "");
			}
			else if(tc.equals("9.B.16") && id == 0) {		
				
				reactant = reactant.replaceAll("cytoplasm", "in");
				product = product.replaceAll("out or cytoplasm of an adjacent cell", "out");
			}

			else
				return null;
			
			reaction[0] = reactant;
			reaction[1] = product;
			
			return reaction;
		}


	/**
	 * Method to read and retrieve the compartments dictionary
	 * 
	 * @return
	 */
	private static Map<String, Set<String>> readCompartmentsDictionary() {

		Map<String, Set<String>> dictionary = new HashMap<>();

		try {
			BufferedReader br = new BufferedReader(new FileReader(DIC_PATH));

			String line = br.readLine();

			while (line != null) {

				if(!line.contains("**")){
					
					Set<String> setValues = new HashSet<>();

					String[] content = line.split("\t=");
					
					String[] values = content[1].split("\\$");

					for(int i = 0; i < values.length; i++)
						setValues.add(values[i]);

					dictionary.put(content[0].trim(), setValues);
				}

				line = br.readLine();
			}

			br.close();

		} 
		catch(Exception e) {
			logger.error("Compartments dictionary file at '{}' not available!", DIC_PATH);
			
		}
		return dictionary;
	}
	
	/**
	 * Get compartments from reaction's text
	 * 
	 * @param reaction
	 */
	private static Set<String> getCompartments(String reaction) {
		
		Set<String> compartments = new HashSet<>();

//				System.out.println(reaction);

		String[] metabolites = reaction.split("\\s\\+\\s");

		//		System.out.println(metabolites[0]);

		for(int i = 0; i < metabolites.length; i++) {

			if(metabolites[i].contains("(") && !metabolites[i].contains("(or ")){

				//				System.out.println(metabolites[i]);

				String[] text = metabolites[i].split("\\s\\(");

				//				System.out.println(text[0]);

				for(int j = 0; j < text.length; j++) {

					if(text[j].contains(")")) {
						
						text[j] = text[j].substring(0, text[j].indexOf(")"));

						if(text[j].contains(" or "))
							text[j] = text[j].split(" or ")[0];
						
						compartments.add(text[j]);
						
					}
				}
			}
		}
		
		return compartments;
		
	}
	
	/**
	 * Finds the relative position of a compartment relative to an other.
	 * 
	 * @param reactant
	 * @param product
	 * @param compartmentsList
	 * @return
	 */
	public static String[] findCompartmentsRelativePosition(String reactant, String product, Compartments[] compartmentsList) {
		
		String[] reaction = new String[2];
		
		Map<Integer, String> compartmentsRelativePositions = new TreeMap<>();
		
		Set<String> allCompartments = new HashSet<>();
		
		Set<String> auxReactant = getCompartments(reactant);
		Set<String> auxProduct = getCompartments(product);
		
		allCompartments.addAll(auxReactant);
		allCompartments.addAll(auxProduct);
		
		int error = 0;
		
		for(String comp : allCompartments) {
			
			if(!comp.contains("E:CH3") && !comp.contains("E:Co")){
			
				for(int i = 0; i < compartmentsList.length; i++) {
					
					try {
//						if(comp.equalsIgnoreCase("int"))	//can't create an enumerator with reserved word
//							comp = "in";
						
						if(compartmentsList[i].equals(Compartments.valueOf(comp.toLowerCase().trim()))) {
							
							compartmentsRelativePositions.put(i, comp);
							break;
						}
					} 
					catch (Exception e) {
						error++;
						logger.error("Compartment '" + comp.toLowerCase() + "' not found to assign relative positions");
						logger.info("reactant: " + reactant);
						logger.info("product: " + product);
						compartmentsRelativePositions.put(compartmentsList.length+error, comp);
						e.printStackTrace();
						break;
					}
				}
			}
		}
	
		boolean first = true;
		
		for(Integer key : compartmentsRelativePositions.keySet()) {
			
			if(first) {
				
				reactant = reactant.replaceAll(compartmentsRelativePositions.get(key), "out");
				product = product.replaceAll(compartmentsRelativePositions.get(key), "out");
				
				first = false;
			}
			else {
				
				reactant = reactant.replaceAll(compartmentsRelativePositions.get(key), "in");
				product = product.replaceAll(compartmentsRelativePositions.get(key), "in");
			}
		}
		
		reaction[0] = reactant;
		reaction[1] = product;
		
		return reaction;
	}
}
