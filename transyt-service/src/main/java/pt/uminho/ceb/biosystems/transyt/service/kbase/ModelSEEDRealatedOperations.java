package pt.uminho.ceb.biosystems.transyt.service.kbase;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import pt.uminho.ceb.biosystems.transyt.scraper.APIs.ModelSEEDAPI;
import pt.uminho.ceb.biosystems.transyt.service.neo4jRest.RestNeo4jGraphDatabase;
import pt.uminho.ceb.biosystems.transyt.service.transytDatabase.TransytNeo4jInitializer;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.ReactionContainer;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.TcNumberContainer2;
import pt.uminho.ceb.biosystems.transyt.utilities.files.FilesUtils;
import pt.uminho.ceb.biosystems.transyt.utilities.transytUtilities.Properties;

public class ModelSEEDRealatedOperations {

	/**
	 * Method to read JSON exceptions file.
	 * 
	 * @return
	 */
	public static Map<String, Map<String, String>> readFile() {

		JSONParser parser = new JSONParser();

		Map<String, Map<String, String>> data = new HashMap<>();

		try {

			Object obj = parser.parse(new FileReader("C:\\Users\\Davide\\Desktop\\Compounds.json"));

			JSONObject allObjects = (JSONObject) obj;

			@SuppressWarnings("unchecked")
			Set<String> keys = allObjects.keySet();

			for(String met : keys) {

				Map<String, String> metProperties = new HashMap<>();

				JSONObject properties = (JSONObject) allObjects.get(met);

				if(properties.get("abbreviation") != null)
					metProperties.put("abbreviation", properties.get("abbreviation").toString());

				if(properties.get("abstract_compound") != null)
					metProperties.put("abstract_compound", properties.get("abstract_compound").toString());

				if(properties.get("aliases") != null)
					metProperties.put("aliases", properties.get("aliases").toString());

				if(properties.get("charge") != null)
					metProperties.put("charge", properties.get("charge").toString());

				if(properties.get("deltag") != null)
					metProperties.put("deltag", properties.get("deltag").toString());

				if(properties.get("deltagerr") != null)
					metProperties.put("deltagerr", properties.get("deltagerr").toString());

				if(properties.get("id") != null)
					metProperties.put("id", properties.get("id").toString());

				if(properties.get("inchikey") != null)
					metProperties.put("inchikey", properties.get("inchikey").toString());

				if(properties.get("is_cofactor") != null)
					metProperties.put("is_cofactor", properties.get("is_cofactor").toString());

				if(properties.get("is_core") != null)
					metProperties.put("is_core", properties.get("is_core").toString());

				if(properties.get("is_obsolete") != null)
					metProperties.put("is_obsolete", properties.get("is_obsolete").toString());

				if(properties.get("linked_compound") != null)
					metProperties.put("linked_compound", properties.get("linked_compound").toString());

				if(properties.get("mass") != null)
					metProperties.put("mass", properties.get("mass").toString());

				if(properties.get("name") != null)
					metProperties.put("name", properties.get("name").toString());

				if(properties.get("pka") != null)
					metProperties.put("pka", properties.get("pka").toString());

				if(properties.get("pkb") != null)
					metProperties.put("pkb", properties.get("pkb").toString());

				if(properties.get("smiles") != null)
					metProperties.put("smiles", properties.get("smiles").toString());

				if(properties.get("source") != null)
					metProperties.put("source", properties.get("source").toString());

				data.put(met, metProperties);

			}
		}
		catch(Exception e) {

			e.printStackTrace();
		}
		return data;
	}

	public static void assingModelSEEDIdentifiersToTranSyTReactions(boolean useCache, Map<String, Set<TcNumberContainer2>> data) throws Exception {

		Map<String, List<String>> reactionsMapping = new HashMap<>();

		Map<String, String> modelseedReactions = ModelSEEDAPI.getModelseedReaction(useCache);
		Map<String, String> notFound = new HashMap<>();

		Map<String, List<String>> msReactants = new HashMap<>();
		Map<String, List<String>> msProducts = new HashMap<>();
		Map<String, Boolean> msReversible = new HashMap<>();

		for(String identifier : modelseedReactions.keySet()) {

			String reaction = modelseedReactions.get(identifier);

			String[] splitReaction = reaction.split("<*=>*");

			if(splitReaction.length > 1 && splitReaction[0].trim().length() > 0 && splitReaction[1].trim().length() > 0) {

				msReactants.put(identifier, rearrangeModelSEEDReactant(splitReaction[0]));
				msProducts.put(identifier, rearrangeModelSEEDReactant(splitReaction[1]));

				//				if(splitReaction[0].contains("cpd00066") && splitReaction[1].contains("cpd00066") && 
				//						splitReaction[0].contains("cpd00067") && splitReaction[1].contains("cpd00067"))
				//					System.out.println(identifier + "\t" + reaction);

				if(reaction.contains("<=>"))
					msReversible.put(identifier, true);
				else
					msReversible.put(identifier, false);
			}
		}

		for(String accession : data.keySet()) {

			for(TcNumberContainer2 tcContainer : data.get(accession)) {

				for(Integer key : tcContainer.getAllReactionsIds()) {

					ReactionContainer container = tcContainer.getReactionContainer(key);

					String transytIdentifier = container.getReactionID();

					if(reactionsMapping.containsKey(transytIdentifier)) {

						if(reactionsMapping.get(transytIdentifier) != null) {
							for(String modelseedIdentifier : reactionsMapping.get(transytIdentifier))
								container.addModelseedReactionIdentifier(modelseedIdentifier);
						}
					}
					else {

						String[] splitReaction = container.getReactionModelSEED().split(container.getDirectionRegex());

						List<String> transytReactant = rearrangeTransytReactant(splitReaction[0]);
						List<String> transytProduct = rearrangeTransytReactant(splitReaction[1]);

						for(String modelSeedIdentifier : msReversible.keySet()) {

//							if(modelSeedIdentifier.equals("rxn27378")) {
//								System.out.println(msReactants.get(modelSeedIdentifier));
//								System.out.println(transytReactant);
//								System.out.println(msProducts.get(modelSeedIdentifier));
//								System.out.println(transytProduct);
//							}

							if(container.isReversible() == msReversible.get(modelSeedIdentifier)) {

								boolean save = false;

								if(transytReactant.containsAll(msReactants.get(modelSeedIdentifier)) && 
										msReactants.get(modelSeedIdentifier).containsAll(transytReactant) &&
										transytProduct.containsAll(msProducts.get(modelSeedIdentifier)) &&
										msProducts.get(modelSeedIdentifier).containsAll(transytProduct)) {
									save = true;
								}
								else if(container.isReversible() && transytReactant.containsAll(msProducts.get(modelSeedIdentifier)) && 
										msReactants.get(modelSeedIdentifier).containsAll(transytProduct) &&
										transytProduct.containsAll(msReactants.get(modelSeedIdentifier)) &&
										msProducts.get(modelSeedIdentifier).containsAll(transytReactant)) {
									save = true;
								}

								if(save) {
									if(!reactionsMapping.containsKey(transytIdentifier))
										reactionsMapping.put(transytIdentifier, new ArrayList<>());

									reactionsMapping.get(transytIdentifier).add(modelSeedIdentifier);
									container.addModelseedReactionIdentifier(modelSeedIdentifier);
								}
							}
						}

						if(container.getModelseedReactionIdentifiers() == null) {
							reactionsMapping.put(transytIdentifier, null);
//							System.out.println(transytIdentifier + "\t" + container.getReactionModelSEED());
							notFound.put(transytIdentifier, container.getReactionModelSEED());
						}	
					}
				}
			}
		}
		
//		FilesUtils.saveMapInFile("/Users/davidelagoa/Desktop/notFound.txt", notFound);
	}

	/**
	 * @param reactant
	 * @return
	 */
	private static List<String> rearrangeModelSEEDReactant(String reactant){

		List<String> components = new ArrayList<>();

		String[] reactantSplit = reactant.split("\\+");

		for(String component : reactantSplit) {

			component = component.replace("(", "").replaceAll("\\)\\s+", ""); //stoichiometry
			component = component.replace("[0]", "(in)").replace("[1]", "(out)").replace("[2]", "(middle)"); //compartments

			components.add(component.replaceAll("\\s+", "").replaceAll("^1cpd", "cpd").trim());
		}

		return components;
	}

	/**
	 * @param reactant
	 * @return
	 */
	private static List<String> rearrangeTransytReactant(String reactant){

		List<String> components = new ArrayList<>();

		String[] reactantSplit = reactant.split("\\+");

		for(String component : reactantSplit) {

			if(!component.contains("in") && !component.contains("out") && !component.contains("middle"))
				component = component.concat("(in)");

			components.add(component.replaceAll("\\s+", "").trim());
		}

		return components;
	}
}
