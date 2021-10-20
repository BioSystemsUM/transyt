package pt.uminho.ceb.biosystems.transyt.utilities.files;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cliftonlabs.json_simple.Jsoner;

import pt.uminho.ceb.biosystems.transyt.utilities.capsules.ReactionContainer;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.TcNumberContainer;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.TcNumberContainer2;
import pt.uminho.ceb.biosystems.transyt.utilities.connection.TcdbRetriever;
import pt.uminho.ceb.biosystems.transyt.utilities.enumerators.Enumerators.TypeOfTransporter;

public class JSONFilesUtils {

	private static final Logger logger = LoggerFactory.getLogger(JSONFilesUtils.class);
	private static final String FAMILY_EXCEPTIONS_PATH = FilesUtils.getDictionatiesAndConfigurationsDirectory().concat("Families_exceptions.json");
	private static final String FAMILY_SPECIFIC_LAST_KNOWN_VERSION_LOG = FilesUtils.getFamilySpecificFilesDirectory().concat("LastKnownVersion.log");
	private static final String SYSTEM_SPECIFIC_LAST_KNOWN_VERSION_LOG = FilesUtils.getSystemSpecificFilesDirectory().concat("LastKnownVersion.log");
	private static final String METACYC_REACTIONS_LAST_KNOWN_VERSION_LOG = FilesUtils.getMetacycReactionsFilesDirectory().concat("LastKnownVersion.log");
	private static final String TCSYSTEMS_LAST_KNOWN_VERSION_LOG = FilesUtils.getBackupTCDBSystemsDirectory().concat("LastKnownVersion.log");

	@SuppressWarnings("unchecked")
	public static void writeJSONtcFamilyReactions(Map<String, TcNumberContainer> transportReactions) {

		try {

			String filePath = FilesUtils.getFamilySpecificFilesDirectory() + FilesUtils.generateFileName("familySpecificReactions", ".json");

			FilesUtils.saveLastKnownVersion(FAMILY_SPECIFIC_LAST_KNOWN_VERSION_LOG, filePath);

			FileWriter file = new FileWriter(filePath);

			JSONObject mappingObj = new JSONObject();

			for(String tc : transportReactions.keySet()) {

				JSONObject obj = new JSONObject();

				TcNumberContainer container = transportReactions.get(tc);

				//				obj.put("TC Number", tc);
				obj.put("Superfamily", container.getSuperFamily());
				obj.put("Family", container.getFamily());

				JSONObject reactionObject = new JSONObject();

				for(Integer idReaction : container.getAllReactionsIds()) {

					ReactionContainer reaction = container.getReactionContainer(idReaction);

					JSONObject properties = new JSONObject();

					properties.put("reaction", reaction.getReaction());
					properties.put("reactants", reaction.getReactant());
					properties.put("products", reaction.getProduct());
					properties.put("reactants", reaction.getReactant());
					properties.put("reversible", reaction.isReversible());
					properties.put("transport type", reaction.getTransportType().toString());

					for(String key : reaction.getProperties().keySet())
						properties.put(key, reaction.getProperties().get(key));

					reactionObject.put(idReaction, properties);


				}

				obj.put("Reactions List", reactionObject);
				// try-with-resources statement based on post comment below :)

				mappingObj.put(tc, obj);
			}

			file.write(Jsoner.prettyPrint(mappingObj.toJSONString()));

			file.close();

			logger.info("JSON object saved successfully to file...");
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public static void writeJSONMetaCycInfo(Map<String, Map<String, ReactionContainer>> transportReactions) {

		try {
			String filePath = FilesUtils.getMetacycReactionsFilesDirectory() + FilesUtils.generateFileName("MetaCycReactions", ".json");

			FilesUtils.saveLastKnownVersion(METACYC_REACTIONS_LAST_KNOWN_VERSION_LOG, filePath);

			FileWriter file = new FileWriter(filePath);

			JSONObject mappingObj = new JSONObject();

			for(String acc : transportReactions.keySet()) {

				JSONObject obj = new JSONObject();

				Map<String, ReactionContainer> container = transportReactions.get(acc);

				//				obj.put("accession", acc);
				//				obj.put("Superfamily", container.getSuperFamily());
				//				obj.put("Family", container.getFamily());

				JSONObject reactionObject = new JSONObject();

				for(String idReaction : container.keySet()) {

					ReactionContainer reaction = container.get(idReaction);

					JSONObject properties = new JSONObject();

					properties.put("metacycID", reaction.getMetaReactionID());
					properties.put("reaction", reaction.getReaction());
					properties.put("reactants", reaction.getReactant());
					properties.put("products", reaction.getProduct());
					properties.put("reversible", reaction.isReversible());
					//					properties.put("transport type", reaction.getTransportType().toString());

					//					for(String key : reaction.getProperties().keySet())
					//						properties.put(key, reaction.getProperties().get(key));

					reactionObject.put(idReaction, properties);


				}

				obj.put("Reactions List", reactionObject);
				// try-with-resources statement based on post comment below :)

				mappingObj.put(acc, obj);
			}

			file.write(Jsoner.prettyPrint(mappingObj.toJSONString()));

			file.close();

			logger.info("JSON object saved successfully to file...");
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}


	@SuppressWarnings("unchecked")
	public static void writeJSONtcReactions(Map<String, Map<String, TcNumberContainer>> data) {

		try {

			String filePath = FilesUtils.getSystemSpecificFilesDirectory() + FilesUtils.generateFileName("reactions", ".json");

			FilesUtils.saveLastKnownVersion(SYSTEM_SPECIFIC_LAST_KNOWN_VERSION_LOG, filePath);

			FileWriter file = new FileWriter(filePath);

			JSONObject mappingObj = new JSONObject();

			for(String acc : data.keySet()) {

				JSONObject tcObj = new JSONObject();

				//				boolean empty = true;

				for(String tc : data.get(acc).keySet()) {

					JSONObject obj = new JSONObject();

					TcNumberContainer container = data.get(acc).get(tc);

					//				obj.put("TC Number", tc);
					obj.put("Superfamily", container.getSuperFamily());
					obj.put("Family", container.getFamily());

					JSONObject reactionObject = new JSONObject();

					for(Integer idReaction : container.getAllReactionsIds()) {

						ReactionContainer reaction = container.getReactionContainer(idReaction);

						JSONObject properties = new JSONObject();

						properties.put("reactionID", reaction.getReactionID());
						properties.put("reaction", reaction.getReaction());
						properties.put("reactants", reaction.getReactant());
						properties.put("products", reaction.getProduct());
						properties.put("reactants", reaction.getReactant());
						properties.put("reversible", reaction.isReversible());
						properties.put("transport type", reaction.getTransportType().toString());
						properties.put("original reaction", reaction.getOriginalReaction());
						properties.put("confidence level", reaction.getConfidenceLevel());
						properties.put("combine with same compound", reaction.isCombineSameMetabolite());

						if(reaction.getProperties() != null) {
							for(String key : reaction.getProperties().keySet())
								properties.put(key, reaction.getProperties().get(key));
						}

						reactionObject.put(idReaction, properties);

						//						empty = false;

					}

					obj.put("Reactions List", reactionObject);
					// try-with-resources statement based on post comment below :)

					tcObj.put(tc, obj);
				}

				//				if(!empty)
				mappingObj.put(acc, tcObj);
			}
			file.write(Jsoner.prettyPrint(mappingObj.toJSONString()));

			file.close();

			logger.info("JSON object saved successfully to file...");
			//			System.out.println("\nJSON Object: " + obj);
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Method to read JSON exceptions file.
	 * 
	 * @return
	 */
	public static Map<String, TcNumberContainer> readJSONExceptionsFile() {

		JSONParser parser = new JSONParser();

		Map<String, TcNumberContainer> data = new HashMap<>();

		try {

			Object obj = parser.parse(new FileReader(FAMILY_EXCEPTIONS_PATH));

			JSONObject allObjects = (JSONObject) obj;

			@SuppressWarnings("unchecked")
			Set<String> keys = allObjects.keySet();

			for(String tc : keys) {

				TcNumberContainer tcContainer = new TcNumberContainer();

				JSONObject properties = (JSONObject) allObjects.get(tc);

				if(properties.get("Family") != null)
					tcContainer.setFamily(properties.get("Family").toString());

				if(properties.get("Superfamily") != null)
					tcContainer.setSuperFamily(properties.get("Superfamily").toString());

				JSONObject reactionList = (JSONObject) properties.get("Reactions List");

				@SuppressWarnings("unchecked")
				Set<String> reactionsKeys = reactionList.keySet();

				for(String key : reactionsKeys) {

					JSONObject reactionProperties = (JSONObject) reactionList.get(key);

					String reactants = reactionProperties.get("reactants").toString();
					String products = reactionProperties.get("products").toString();
					//            		String reaction = reactionProperties.get("reaction").toString();
					Boolean reversible = Boolean.valueOf(reactionProperties.get("reversible").toString());

					TypeOfTransporter transpType = TypeOfTransporter.valueOf(reactionProperties.get("transport type").toString());

					ReactionContainer reactContainer = new ReactionContainer(reactants, products, reversible);

					if(transpType != null)
						reactContainer.setTransportType(transpType);

					if(reactionProperties.containsKey("GET"))
						reactContainer.addProperty("GET", reactionProperties.get("GET").toString());

					if(reactionProperties.containsKey("PMF"))
						reactContainer.addProperty("PMF", reactionProperties.get("PMF").toString());

					if(reactionProperties.containsKey("GTP"))
						reactContainer.addProperty("GTP", reactionProperties.get("GTP").toString());

					if(reactionProperties.containsKey("ATP"))
						reactContainer.addProperty("ATP", reactionProperties.get("ATP").toString());

					if(reactionProperties.containsKey("energy"))
						reactContainer.addProperty("energy", reactionProperties.get("energy").toString());

					if(reactionProperties.containsKey("reaction string as retrieved from TCDB"))
						reactContainer.addProperty("reaction string as retrieved from TCDB", "no");

					if(reactionProperties.containsKey("combine with same compound"))
						reactContainer.setCombineSameMetabolite(Boolean.valueOf(reactionProperties.get("reversible").toString()));

					if(reactionProperties.containsKey("reductase"))
						reactContainer.setReductase(Boolean.valueOf(reactionProperties.get("reductase").toString()));

					if(reactionProperties.containsKey("oxidase"))
						reactContainer.setOxidase(Boolean.valueOf(reactionProperties.get("oxidase").toString()));

					tcContainer.addReaction(reactContainer);
				}

				data.put(tc, tcContainer);
			}
		}
		catch(Exception e) {

			e.printStackTrace();
		}
		return data;
	}

	/**
	 * Method to read JSON exceptions file.
	 * 
	 * @return
	 */
	public static Map<String, TcNumberContainer> readDataBackupFile() {

		JSONParser parser = new JSONParser();

		Map<String, TcNumberContainer> data = new HashMap<>();

		try {

			Object obj = parser.parse(new FileReader(FilesUtils.getLastKnownVersion(FAMILY_SPECIFIC_LAST_KNOWN_VERSION_LOG)));

			JSONObject allObjects = (JSONObject) obj;

			@SuppressWarnings("unchecked")
			Set<String> keys = allObjects.keySet();

			for(String tc : keys) {

				TcNumberContainer tcContainer = new TcNumberContainer();

				JSONObject properties = (JSONObject) allObjects.get(tc);

				if(properties.get("Family") != null)
					tcContainer.setFamily(properties.get("Family").toString());

				if(properties.get("Superfamily") != null)
					tcContainer.setSuperFamily(properties.get("Superfamily").toString());

				JSONObject reactionList = (JSONObject) properties.get("Reactions List");

				@SuppressWarnings("unchecked")
				Set<String> reactionsKeys = reactionList.keySet();

				for(String key : reactionsKeys) {

					JSONObject reactionProperties = (JSONObject) reactionList.get(key);

					String reactants = reactionProperties.get("reactants").toString();
					String products = reactionProperties.get("products").toString();
					//            		String reaction = reactionProperties.get("reaction").toString();
					Boolean reversible = Boolean.valueOf(reactionProperties.get("reversible").toString());

					TypeOfTransporter transpType = TypeOfTransporter.valueOf(reactionProperties.get("transport type").toString());

					ReactionContainer reactContainer = new ReactionContainer(reactants, products, reversible);

					if(transpType != null)
						reactContainer.setTransportType(transpType);

					if(reactionProperties.containsKey("GET"))
						reactContainer.addProperty("GET", reactionProperties.get("GET").toString());

					if(reactionProperties.containsKey("PMF"))
						reactContainer.addProperty("PMF", reactionProperties.get("PMF").toString());

					if(reactionProperties.containsKey("GTP"))
						reactContainer.addProperty("GTP", reactionProperties.get("GTP").toString());

					if(reactionProperties.containsKey("ATP"))
						reactContainer.addProperty("ATP", reactionProperties.get("ATP").toString());

					if(reactionProperties.containsKey("energy"))
						reactContainer.addProperty("energy", reactionProperties.get("energy").toString());

					if(reactionProperties.containsKey("reaction string as retrieved from TCDB"))
						reactContainer.addProperty("reaction as retrieved from TCDB", reactionProperties.get("reaction string as retrieved from TCDB").toString());

					if(reactionProperties.containsKey("reductase"))
						reactContainer.setReductase(Boolean.valueOf(reactionProperties.get("reductase").toString()));

					if(reactionProperties.containsKey("oxidase"))
						reactContainer.setOxidase(Boolean.valueOf(reactionProperties.get("oxidase").toString()));

					tcContainer.addReaction(reactContainer);
				}

				data.put(tc, tcContainer);
			}
		}
		catch(Exception e) {

			e.printStackTrace();
		}
		return data;
	}

	/**
	 * Method to read JSON exceptions file.
	 * 
	 * @return
	 */
	public static Map<String, Map<String, ReactionContainer>> readMetaCycDataBackupFile(Map<String, String> metacycCompoundsToModelSEED) {

		JSONParser parser = new JSONParser();

		Map<String, Map<String, ReactionContainer>> data = new HashMap<>();

		try {

			Object obj = parser.parse(new FileReader(FilesUtils.getLastKnownVersion(METACYC_REACTIONS_LAST_KNOWN_VERSION_LOG)));

			JSONObject allObjects = (JSONObject) obj;

			@SuppressWarnings("unchecked")
			Set<String> keys = allObjects.keySet();

			for(String acc : keys) {

				Map<String, ReactionContainer> containers = new HashMap<>();

				JSONObject properties = (JSONObject) allObjects.get(acc);
				JSONObject reactionList = (JSONObject) properties.get("Reactions List");

				@SuppressWarnings("unchecked")
				Set<String> reactionsKeys = reactionList.keySet();

				for(String key : reactionsKeys) {

					JSONObject reactionProperties = (JSONObject) reactionList.get(key);

					String reactants = reactionProperties.get("reactants").toString().replace(":Hpr-pi-phospho-L-histidines", ":PHOSPHO-ENOL-PYRUVATE")
							.replaceAll("[a-zA-Z0-9_]+:", "META:").replace("$", "").replace("META:Unspecified-Ion-Or-Solute", "META:Ions")
							.replace("META:OLIGOPEPTIDES", "META:Amino-Acids-20").trim();
					String products = reactionProperties.get("products").toString().replace(":Hpr-Histidine", ":PYRUVATE")
							.replaceAll("[a-zA-Z0-9_]+:", "META:").replace("$", "").replace("META:Unspecified-Ion-Or-Solute", "META:Ions")
							.replace("META:OLIGOPEPTIDES", "META:Amino-Acids-20").trim();
					//            		String reaction = reactionProperties.get("reaction").toString();
					Boolean reversible = Boolean.valueOf(reactionProperties.get("reversible").toString());

					//					TypeOfTransporter transpType = TypeOfTransporter.valueOf(reactionProperties.get("transport type").toString());

					boolean transport = reactants.contains(ReactionContainer.INTERIOR_COMPARTMENT_TOKEN) 
							|| reactants.contains(ReactionContainer.EXTERIOR_COMPARTMENT_TOKEN) 
							|| reactants.contains(ReactionContainer.MIDDLE_COMPARTMENT_TOKEN);

					if(transport && !reactants.isBlank() && !products.isBlank()) {

						ReactionContainer reactContainer = null;

						if(reactants.contains("META:NADPH") && reactants.contains("META:NAD") && reactants.contains("META:PROTON") &&
								products.contains("META:NADP") && products.contains("META:NADH") && products.contains("META:PROTON"))
							reactContainer = new ReactionContainer(products, reactants, reversible);	//just to use TCDB standard
						else
							reactContainer = new ReactionContainer(reactants, products, reversible);
						reactContainer.setMetaReactionID(key);


						reactContainer = replaceCpdMetabolites(reactContainer, metacycCompoundsToModelSEED);

						//					if(transpType != null)
						//						reactContainer.setTransportType(transpType);
						//
						//					if(reactionProperties.containsKey("GET"))
						//						reactContainer.addProperty("GET", reactionProperties.get("GET").toString());
						//
						//					if(reactionProperties.containsKey("PMF"))
						//						reactContainer.addProperty("PMF", reactionProperties.get("PMF").toString());
						//
						//					if(reactionProperties.containsKey("GTP"))
						//						reactContainer.addProperty("GTP", reactionProperties.get("GTP").toString());
						//
						//					if(reactionProperties.containsKey("ATP"))
						//						reactContainer.addProperty("ATP", reactionProperties.get("ATP").toString());
						//
						//					if(reactionProperties.containsKey("energy"))
						//						reactContainer.addProperty("energy", reactionProperties.get("energy").toString());
						//
						//					if(reactionProperties.containsKey("reaction string as retrieved from TCDB"))
						//						reactContainer.addProperty("reaction string as retrieved from TCDB", reactionProperties.get("reaction string as retrieved from TCDB").toString());

						//					tcContainer.addReaction(reactContainer);

						containers.put(key, reactContainer);
					}
				}

				data.put(acc, containers);
			}
		}
		catch(Exception e) {

			e.printStackTrace();
		}
		return data;
	}

	public static ReactionContainer replaceCpdMetabolites(ReactionContainer reactContainer, Map<String, String> metacycCompoundsToModelSEED) {

		for(String compound : new HashSet<>(reactContainer.getMetabolites())) {

			String auxCompound = compound.replace("META:", "");

			if(metacycCompoundsToModelSEED.containsKey(auxCompound)) {

				String newCompound = metacycCompoundsToModelSEED.get(auxCompound);

				String newReactant = reactContainer.getReactant().replace(compound, newCompound);
				String newProduct = reactContainer.getProduct().replace(compound, newCompound);

				reactContainer.setReactant(newReactant);
				reactContainer.setProduct(newProduct);
			}
		}

		return reactContainer;
	}

	/**
	 * Method to read JSON file containing tcdb' reactions data.
	 * 
	 * @return
	 */
	public static Map<String, Set<TcNumberContainer2>> readJSONtcdbReactionsFile() {

		Set<String> stats = new HashSet<>();

		JSONParser parser = new JSONParser();

		Map<String, Set<TcNumberContainer2>> data = new HashMap<>();

		try {

			//			Object obj = parser.parse(new FileReader("C:\\Users\\Davide\\Documents\\Exceptions.json"));
			Object obj = parser.parse(new FileReader(FilesUtils.getLastKnownVersion(SYSTEM_SPECIFIC_LAST_KNOWN_VERSION_LOG)));

			JSONObject allObjects = (JSONObject) obj;

			@SuppressWarnings("unchecked")
			Set<String> keys = allObjects.keySet();

			for(String acc : keys) {

				//				if(acc.equals("O33823"))
				//					System.out.println();

				JSONObject tcNumbers = (JSONObject) allObjects.get(acc);

				@SuppressWarnings("unchecked")
				Set<String> tcKeys = tcNumbers.keySet();

				for(String tc : tcKeys ) {

					TcNumberContainer2 tcContainer = new TcNumberContainer2(tc);

					JSONObject properties = (JSONObject) tcNumbers.get(tc);

					if(properties.get("Family") != null)
						tcContainer.setFamily(properties.get("Family").toString());

					if(properties.get("Superfamily") != null)
						tcContainer.setSuperFamily(properties.get("Superfamily").toString());

					JSONObject reactionList = (JSONObject) properties.get("Reactions List");

					@SuppressWarnings("unchecked")
					Set<String> reactionsKeys = reactionList.keySet();

					for(String key : reactionsKeys) {

						JSONObject reactionProperties = (JSONObject) reactionList.get(key);

						String reactants = reactionProperties.get("reactants").toString();
						String products = reactionProperties.get("products").toString();
						//            		String reaction = reactionProperties.get("reaction").toString();
						Boolean reversible = Boolean.valueOf(reactionProperties.get("reversible").toString());

						TypeOfTransporter transpType = TypeOfTransporter.valueOf(reactionProperties.get("transport type").toString());

						ReactionContainer reactContainer = new ReactionContainer(reactants, products, reversible);

						String confidenceLevel = reactionProperties.get("confidence level").toString();
						String originalReaction = reactionProperties.get("original reaction").toString();

						reactContainer.setConfidenceLevel(confidenceLevel);
						reactContainer.setOriginalReaction(originalReaction);

						if(transpType != null)
							reactContainer.setTransportType(transpType);

						if(reactionProperties.containsKey("GET"))
							reactContainer.addProperty("GET", reactionProperties.get("GET").toString());

						if(reactionProperties.containsKey("PMF"))
							reactContainer.addProperty("PMF", reactionProperties.get("PMF").toString());

						if(reactionProperties.containsKey("GTP"))
							reactContainer.addProperty("GTP", reactionProperties.get("GTP").toString());

						if(reactionProperties.containsKey("ATP"))
							reactContainer.addProperty("ATP", reactionProperties.get("ATP").toString());

						if(reactionProperties.containsKey("energy"))
							reactContainer.addProperty("energy", reactionProperties.get("energy").toString());

						if(reactionProperties.containsKey("reaction string as retrieved from TCDB"))
							reactContainer.addProperty("reaction string as retrieved from TCDB", reactionProperties.get("reaction string as retrieved from TCDB").toString());

						tcContainer.addReaction(reactContainer);
					}

					if(data.containsKey(acc))
						data.get(acc).add(tcContainer);
					else {
						Set<TcNumberContainer2> set = new HashSet<TcNumberContainer2>();
						set.add(tcContainer);

						data.put(acc, set);
					}

					stats.add(acc);
				}
			}
		}
		catch(Exception e) {

			e.printStackTrace();
		}
		return data;
	}

	@SuppressWarnings("unchecked")
	@Deprecated
	public static void writeJSONTriageReactions(Map<String, Set<TcNumberContainer2>> data) {

		try {
			String path = "C:\\Users\\Davide\\Documents\\triageReactions.json";

			FileWriter file = new FileWriter(path);

			JSONObject mappingObj = new JSONObject();

			for(String acc : data.keySet()) {

				JSONObject tcObj = new JSONObject();

				boolean empty = true;

				for(TcNumberContainer2 tcContainer : data.get(acc)) {

					JSONObject obj = new JSONObject();

					//				obj.put("TC Number", tc);
					obj.put("Superfamily", tcContainer.getSuperFamily());
					obj.put("Family", tcContainer.getFamily());

					JSONObject reactionObject = new JSONObject();

					for(Integer idReaction : tcContainer.getAllReactionsIds()) {

						ReactionContainer reaction = tcContainer.getReactionContainer(idReaction);

						JSONObject properties = new JSONObject();

						properties.put("reactionID", reaction.getReactionID());
						properties.put("reaction", reaction.getReaction());
						properties.put("reactants", reaction.getReactant());
						properties.put("products", reaction.getProduct());
						properties.put("reactants", reaction.getReactant());
						properties.put("reversible", reaction.isReversible());
						properties.put("transport type", reaction.getTransportType().toString());
						properties.put("original reaction", reaction.getOriginalReaction());
						properties.put("confidence level", reaction.getConfidenceLevel());
						properties.put("reaction with IDs", reaction.getReactionMetaCyc());

						if(reaction.getProperties() != null) {
							for(String key : reaction.getProperties().keySet())
								properties.put(key, reaction.getProperties().get(key));
						}

						reactionObject.put(idReaction, properties);

						empty = false;

					}

					obj.put("Reactions List", reactionObject);
					// try-with-resources statement based on post comment below :)

					tcObj.put(tcContainer.getTcNumber(), obj);
				}

				if(!empty)
					mappingObj.put(acc, tcObj);
			}
			file.write(Jsoner.prettyPrint(mappingObj.toJSONString()));

			file.close();

			logger.info("Successfully Copied JSON TRIAGE Object to file at: {}", path);

		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param results
	 * @param failed
	 */
	@SuppressWarnings("unchecked")
	public static void exportTCDBScrapedInfo(Map<String, Map<String, String>> results) {

		try {

			logger.info("Saving retrieved information into JSON object...");

			String filePath = FilesUtils.getBackupTCDBSystemsDirectory() + FilesUtils.generateFileName("TCSystems", ".json");

			FilesUtils.saveLastKnownVersion(TCSYSTEMS_LAST_KNOWN_VERSION_LOG, filePath);

			FileWriter file = new FileWriter(filePath);

			JSONObject mappingObj = new JSONObject();

			for(String key : results.keySet()) {

				String accession = key.split("@")[0];

				JSONObject obj = new JSONObject();

				Map <String, String> submap = results.get(key);

				if(submap.get(TcdbRetriever.TCNUMBER) != null) {

					obj.put(TcdbRetriever.ORGANISM, submap.get(TcdbRetriever.ORGANISM));
					obj.put(TcdbRetriever.SUBSTRATE, submap.get(TcdbRetriever.SUBSTRATE));
					obj.put(TcdbRetriever.DESCRIPTION, submap.get(TcdbRetriever.DESCRIPTION));

					JSONObject systemObject = new JSONObject();

					if(mappingObj.containsKey(accession))
						systemObject = (JSONObject) mappingObj.get(accession);

					systemObject.put(submap.get(TcdbRetriever.TCNUMBER), obj);

					mappingObj.put(accession, systemObject);
				}
			}

			file.write(Jsoner.prettyPrint(mappingObj.toJSONString()));

			file.close();

			logger.info("JSON object saved successfully to file...");
		} 
		catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * @param results
	 * @param failed
	 */
	public static List<String[]> readTCDBScrapedInfo() {

		JSONParser parser = new JSONParser();

		List<String[]> data = new ArrayList<>();

		try {

			Object obj = parser.parse(new FileReader(FilesUtils.getLastKnownVersion(TCSYSTEMS_LAST_KNOWN_VERSION_LOG)));

			JSONObject allObjects = (JSONObject) obj;

			@SuppressWarnings("unchecked")
			Set<String> keys = allObjects.keySet();

			for(String key : keys) {

				JSONObject accessionObject = (JSONObject) allObjects.get(key);

				@SuppressWarnings("unchecked")
				Set<String> tcNumbers = accessionObject.keySet();

				for(String tc : tcNumbers) {

					String[] system = new String[5];

					JSONObject sysytemProperties = (JSONObject) accessionObject.get(tc);

					//					System.out.println(key);
					//					System.out.println(tc);

					system[0] = key.concat("@").concat(tc);
					if(sysytemProperties.get(TcdbRetriever.ORGANISM) != null)
						system[1] = sysytemProperties.get(TcdbRetriever.ORGANISM).toString();
					if(sysytemProperties.get(TcdbRetriever.SUBSTRATE) != null)
						system[2] = sysytemProperties.get(TcdbRetriever.SUBSTRATE).toString();
					system[3] = tc;
					if(sysytemProperties.get(TcdbRetriever.DESCRIPTION) != null)
						system[4] = sysytemProperties.get(TcdbRetriever.DESCRIPTION).toString();

					data.add(system);
				}
			}
		}
		catch(Exception e) {

			logger.error("An error occurred while reading last known version of TC systems information retrieved from TCDB!");

			e.printStackTrace();
		}
		return data;
	}

	@SuppressWarnings("unchecked")
	public static void buildKBaseOntologiesFile(String dataVersion, Map<String, String> descriptions) {

		try {

			logger.info("Saving retrieved information into JSON object...");

			String filePath = FilesUtils.getOntologiesDataDirectory().concat("kb_transyt_ontologies.json");

			//			FilesUtils.saveLastKnownVersion(TCSYSTEMS_LAST_KNOWN_VERSION_LOG, filePath);

			FileWriter file = new FileWriter(filePath);

			JSONObject mappingObj = new JSONObject();
			
			String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
			mappingObj.put("date", timeStamp);
			
			JSONObject ontologies = new JSONObject();

			for(String tc : descriptions.keySet()) {

				JSONObject obj = new JSONObject();

				List<String> synonyms = new ArrayList<>();

				obj.put("id", tc);
				obj.put("synonyms", synonyms);
				obj.put("name", descriptions.get(tc));

				ontologies.put(tc, obj);
			}
			
			timeStamp = new SimpleDateFormat("MM-dd").format(new Date());
			
			mappingObj.put("term_hash", ontologies);
			mappingObj.put("format_version", "N/A");
			mappingObj.put("ontology", "transyt_tc_numbers");
			mappingObj.put("data_version", dataVersion);
			

			file.write(Jsoner.prettyPrint(mappingObj.toJSONString()));
			file.close();

			logger.info("JSON object saved successfully to file...");
		} 
		catch (Exception e) {
			e.printStackTrace();
		}

	}
}