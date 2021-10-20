package pt.uminho.ceb.biosystems.transyt.service.internalDB;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.uminho.ceb.biosystems.transyt.scraper.tcdb.tcdbTransportTypesRetriever.Retriever;
import pt.uminho.ceb.biosystems.transyt.scraper.tcdb.utilities.ProcessTcdbMetabolites;
import pt.uminho.ceb.biosystems.transyt.service.biosynth.initializeNeo4jdb;
import pt.uminho.ceb.biosystems.transyt.service.containers.BiosynthMetabolites;
import pt.uminho.ceb.biosystems.transyt.service.reactions.TransportReactionsBuilder;
import pt.uminho.ceb.biosystems.transyt.service.transytDatabase.PopulateTransytNeo4jDatabase;
import pt.uminho.ceb.biosystems.transyt.service.transytDatabase.TransytGeneralProperties;
import pt.uminho.ceb.biosystems.transyt.service.transytDatabase.TransytNeo4jInitializer;
import pt.uminho.ceb.biosystems.transyt.service.transytDatabase.TransytNodeLabel;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.BiosynthMetaboliteProperties;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.TcNumberContainer2;
import pt.uminho.ceb.biosystems.transyt.utilities.enumerators.MetaboliteReferenceDatabaseEnum;
import pt.uminho.ceb.biosystems.transyt.utilities.files.FilesUtils;
import pt.uminho.ceb.biosystems.transyt.utilities.files.JSONFilesUtils;
import pt.uminho.ceb.biosystems.transyt.utilities.transytUtilities.Properties;
import pt.uminho.sysbio.biosynth.integration.io.dao.neo4j.MetaboliteMajorLabel;
import pt.uminho.sysbio.biosynth.integration.neo4j.BiodbMetaboliteNode;
import pt.uminho.sysbio.biosynthframework.BiodbGraphDatabaseService;

/**
 * @author Davide
 *
 */
public class WriteByMetabolitesID {

	private static final Logger logger = LoggerFactory.getLogger(WriteByMetabolitesID.class);

	public static void start(Properties properties, boolean tests) {

		try {

			GraphDatabaseService graphDatabaseService = initializeNeo4jdb.getDataDatabase(null);
			Transaction dataTx = graphDatabaseService.beginTx();

			BiodbGraphDatabaseService service = new BiodbGraphDatabaseService(graphDatabaseService);

			//			BiosynthMetabolites l = new FetchCompoundsByName(service, false).getResults();
			//			
			//			
			//			dataTx.failure();
			//			dataTx.close();
			//			service.shutdown();
			//			graphDatabaseService.shutdown();
			//			System.out.println("Shutdown!!!");

			//			BiodbGraphDatabaseService service = null;

			logger.info("Retrieving data from Biosynth database...");

			Map<String, BiosynthMetaboliteProperties> data = getBiosynthDBData(service);
			//						Map<String, BiosynthMetaboliteProperties> data = null;

			//						System.out.println("Writing Excel...");
			//						WriteExcel.writeNeo4jKeggInfo(data);

			logger.info("Retrieving data from Biosynth database by metabolite name...");
			BiosynthMetabolites namesAndIDsContainer = new FetchCompoundsByName(service, false).getResults();		//168966 names

			Map<String, Set<TcNumberContainer2>> reactionsData = JSONFilesUtils.readJSONtcdbReactionsFile();  //uncomment

			if(tests) {

				@SuppressWarnings("resource")
				Scanner reader = new Scanner(System.in);
				int n = 1;

				while (n != 99) {

					test(namesAndIDsContainer, data, service, reactionsData, properties);

					System.out.println("Enter a random number to repeat (100 to repeat data retrieval) or 99 to finish: ");

					try {
						n = reader.nextInt();
					} catch (Exception e) {
						e.printStackTrace();

						n = 1;
					}
				}
			}
			else {
				Map<String, Set<TcNumberContainer2>> newData = new TransportReactionsBuilder(reactionsData, service, data, namesAndIDsContainer, properties, false).getResults();		//uncomment

				String currentVersion = TransytNeo4jInitializer.getDatabaseVersion(properties);
				String version = FilesUtils.generateNewVersionStamp(currentVersion);
				
				List<String[]> table = JSONFilesUtils.readTCDBScrapedInfo();

				Map<String, String> descriptions = ProcessTcdbMetabolites.getTCDescriptions(table);
				JSONFilesUtils.buildKBaseOntologiesFile(version, descriptions);
				
				new PopulateTransytNeo4jDatabase(data, newData, version, properties);
			}

			dataTx.failure();
			dataTx.close();
			service.shutdown();
			graphDatabaseService.shutdown();
			System.out.println("Shutdown!!!");
		} 
		catch (Exception e) {
			logger.trace("StackTrace {}",e);
			//			e.printStackTrace();
		}
	}

	private static void test2(BiodbGraphDatabaseService service, Map<String, BiosynthMetaboliteProperties> data,
			BiosynthMetabolites namesAndIDsContainer) {
		try {
			//			Node node1 = service.getMetaboliteProperty("e", MetabolitePropertyLabel.Name);
			//
			//			
			//			if(node1 != null)
			//			{
			//				System.out.println(node1.getAllProperties());
			//				
			//				
			//				
			//				Iterable<RelationshipType> rels = node1.getRelationships();
			//				
			//				for(RelationshipType rel : rels) {
			//					System.out.println(rel.name());
			//				}
			//			}

			Node node = service.getNodeByEntryAndLabel("META:Amino-Acids-20", MetaboliteMajorLabel.MetaCyc);

			System.out.println(node.getAllProperties());

//			Node node = service.getNodeByEntryAndLabel("cpd11420", MetaboliteMajorLabel.ModelSeed);
//
//			System.out.println(node.getAllProperties());
			
			//			Iterable<Relationship> rels = node.getRelationships();
			//
			//			for(Relationship rel : rels) {
			//				Long otherNode = rel.getOtherNodeId(node.getId());
			//
			//				System.out.println(service.getNodeById(otherNode).getAllProperties());
			//			}



		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("done!");

	}



	@SuppressWarnings("unused")
	public static Map<String, Set<TcNumberContainer2>> test(BiosynthMetabolites namesAndIDsContainer, Map<String, BiosynthMetaboliteProperties> data, BiodbGraphDatabaseService service,
			Map<String, Set<TcNumberContainer2>> reactionsData, Properties properties) {

		try {
			Boolean generate = true;
			String accession = "P0AAJ3"; //

//			test2(service, null, null);

			Retriever.runRetriever(true, true, accession);

			if(generate) {

				reactionsData = JSONFilesUtils.readJSONtcdbReactionsFile();

				Map<String, Set<TcNumberContainer2>> reactionsData2 = new HashMap<>();

				if(accession == null) {
					reactionsData2 = new HashMap<>(reactionsData);
				}
				else {
					String[] accessions = new String[] {accession};

					for(String acc : accessions)
						reactionsData2.put(acc, reactionsData.get(acc));
				}
				/////TRANSYT
				Map<String, Set<TcNumberContainer2>> newData = new TransportReactionsBuilder(reactionsData2, service, data, namesAndIDsContainer, properties, true).getResults();		//uncomment

				if(accession != null) {
					for(TcNumberContainer2 container : newData.get(accession)) {

						System.out.println(container.getTcNumber());

						for( Integer id : container.getAllReactionsIds()) {
							System.out.println();

							System.out.println(container.getReactionContainer(id).getReactionID());
							System.out.println(container.getReactionContainer(id).getMetaReactionID());
							System.out.println(container.getReactionContainer(id).getCompartmentalizedReactionID());
							System.out.println(container.getReactionContainer(id).getReaction());
							System.out.println(container.getReactionContainer(id).getReactionBase());
							System.out.println(container.getReactionContainer(id).getReactionKEGG());
							System.out.println(container.getReactionContainer(id).getReactionBiGG());
							System.out.println(container.getReactionContainer(id).getReactionMetaCyc());
							System.out.println(container.getReactionContainer(id).getReactionModelSEED());
							System.out.println(container.getReactionContainer(id).getOriginalReaction());
							List<String> reactions = container.getReactionContainer(id).getModelseedReactionIdentifiers();
							if(reactions != null)
								System.out.println(reactions.stream().map(i -> i.toString()).collect(Collectors.joining(";")));
						}


						System.out.println();
					}
				}

//								new PopulateTransytNeo4jDatabase(data, newData, properties);
			}

			//			JSONFilesUtils.writeJSONTriageReactions(newData);

			System.out.println("Done!!!");

			return null;
		} 
		catch (Exception e) {
			e.printStackTrace();
		}

		return null;


	}

	/**
	 * Method to get all data of interest from the pt.uminho.ceb.biosystems.transyt.service.biosynth database.
	 * 
	 * @param service
	 * @return
	 */
	public static Map<String, BiosynthMetaboliteProperties> getBiosynthDBData(BiodbGraphDatabaseService service) {

		Map<String, BiosynthMetaboliteProperties> compounds = new HashMap<>();

		MetaboliteReferenceDatabaseEnum[] databases = MetaboliteReferenceDatabaseEnum.values();

		try { 

			Set<BiodbMetaboliteNode> allMetabolites = service.listMetabolites();

			//	Set<String> allDatabases = new HashSet<>();

			for(BiodbMetaboliteNode node : allMetabolites) {

				String entryID = node.getEntry();

				if(!entryID.isEmpty()) {

					Map<String, Object> nodeProperties = node.getAllProperties();

					BiosynthMetaboliteProperties bioSynMetab;

					Set<String> synonyms = FetchCompoundsByName.getSynonyms(node, nodeProperties, service);

					if(entryID.matches("META:.*") || entryID.matches("cpd.*")) {
						synonyms.add(entryID);
					}
					//					String names ="";
					//					
					//					if(node.hasProperty("name")) {
					//						String res = (String) nodeProperties.get("name");
					//						if(!res.contains("&"))
					//							names = names.concat(res);
					//					}
					//					if(node.hasProperty("frameid"))		//metacyc names are correct in frameID, not in names because of the &alpha, &beta, &delta, &gamma...
					//						names = names.concat(";").concat((String) nodeProperties.get("frameid"));
					//
					//					for(String name : names.split(";")) {
					//						if(!name.isEmpty()) {
					//							synonyms.add(name.replaceAll("<i>", "").replaceAll("</i>", "").replaceAll("\\[", "")
					//									.replaceAll("\\]", "").replaceAll("</sup>", "").replaceAll("<sup>", "").replaceAll("</I>", "").replaceAll("<I>", ""));
					//						}
					//					}

					if(!synonyms.isEmpty()) {

						if(node.hasProperty("major_label"))
							bioSynMetab = new BiosynthMetaboliteProperties(entryID, nodeProperties.get("major_label").toString(), synonyms);
						else
							bioSynMetab = new BiosynthMetaboliteProperties(entryID, BiosynthMetaboliteProperties.NONE, synonyms);

						if(node.hasProperty("formula")) {
							String formula = (String) nodeProperties.get("formula");

							if(formula != null && !(formula.equalsIgnoreCase("none") || formula.equalsIgnoreCase("null")))
								bioSynMetab.setFormula(formula);
						}	

						if(entryID.equalsIgnoreCase("cpd03805"))
							bioSynMetab.setFormula("C6H11O8P");   //there's an error in the database, it needs to be updated
						else if(entryID.equalsIgnoreCase("cpd15391"))
							bioSynMetab.setFormula("C11H17NO11P");   //there's an error in the database, it needs to be updated
						else if(entryID.equalsIgnoreCase("cpd26871"))
							bioSynMetab.setFormula("C4H6N2O3R2");   //there's an error in the database, it needs to be updated
						else if(entryID.equalsIgnoreCase("cpd28237"))
							bioSynMetab.setFormula("C6H8N3O4R3");   //there's an error in the database, it needs to be updated
						else if(entryID.equalsIgnoreCase("cpd16486"))
							bioSynMetab.setFormula("C13H18O2");   //there's an error in the database, it needs to be updated
						else if(entryID.equalsIgnoreCase("cpd02817"))
							bioSynMetab.setFormula("C11H19NO7PS");   //there's an error in the database, it needs to be updated
						
						if(node.hasProperty("remark"))
							bioSynMetab.setRemark((String) nodeProperties.get("remark"));


						Map<String, Map<String, Object>> references = new HashMap<>();

						if(node.hasRelationship()) {

							Iterable<Relationship> relationships = node.getRelationships();

							for(Relationship rel : relationships) {

								Map<String, Object> relProperties = rel.getAllProperties();

								if(relProperties.containsKey("ref")) {

									String reference = relProperties.get("ref").toString();

									relProperties.remove("ref");

									references.put(reference.replaceAll("\\s+","").replaceAll("-", ""), relProperties);

								}
							}

							bioSynMetab.setReferences(references);

							boolean found = false;
							int i = 0;

							while(!found && i < databases.length) {

								if(references.containsKey(databases[i].toString())) {

									Map<String, Object> bestReferenceDatabase = references.get(databases[i].toString());

									bioSynMetab.setBestReferenceSource(databases[i].toString());
									bioSynMetab.setBestReferenceID(bestReferenceDatabase.get("value").toString());

									if(bestReferenceDatabase.containsKey("url"))
										bioSynMetab.setBestReferenceURL(bestReferenceDatabase.get("url").toString());

									else if(bestReferenceDatabase.containsKey("link"))
										bioSynMetab.setBestReferenceURL(bestReferenceDatabase.get("link").toString());

									found = true;
								}
								i++;
							}
						}

						compounds.put(entryID, bioSynMetab);
					}
				}
			}
		}
		catch(NullPointerException e){

			System.out.println("NULL exception");
			e.printStackTrace();

		}

		return compounds;
	}

}
