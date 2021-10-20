package pt.uminho.ceb.biosystems.transyt.scraper.tcdb.tcdbTransportTypesRetriever;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.uminho.ceb.biosystems.transyt.scraper.APIs.KeggAPI;
import pt.uminho.ceb.biosystems.transyt.scraper.APIs.MetaCycAPI;
import pt.uminho.ceb.biosystems.transyt.scraper.APIs.ModelSEEDAPI;
import pt.uminho.ceb.biosystems.transyt.scraper.APIs.NCBIAPI;
import pt.uminho.ceb.biosystems.transyt.scraper.tcdb.reactionsGenerator.GenerateTransportReactions;
import pt.uminho.ceb.biosystems.transyt.scraper.tcdb.utilities.ProcessTcdbMetabolites;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.ReactionContainer;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.TcNumberContainer;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.TcdbMetabolitesContainer;
import pt.uminho.ceb.biosystems.transyt.utilities.connection.ReadFastaTcdb;
import pt.uminho.ceb.biosystems.transyt.utilities.connection.TcdbExplorer;
import pt.uminho.ceb.biosystems.transyt.utilities.connection.TcdbRetriever;
import pt.uminho.ceb.biosystems.transyt.utilities.files.FilesUtils;
import pt.uminho.ceb.biosystems.transyt.utilities.files.JSONFilesUtils;

public class Retriever {

	private static final Logger logger = LoggerFactory.getLogger(Retriever.class);


	public static void runRetriever(boolean useCache, boolean tests, String accTest) throws Exception {
		
		try {
			if(!useCache) {
				logger.info("Retrieving NCBI taxonomy files...");
				
				NCBIAPI.downloadAndUncompressTaxonomyFiles();
				
				logger.info("Retrieving TCDB FASTA file...");
				ReadFastaTcdb.readfasta(false);
				
				Thread thread1 = new Thread () {
					public void run () {

						try {
							runMetacycScraper();
						}
						catch (Exception e) {
							logger.error("Fatal error while scraping MetaCyc... Exiting TranSyT...");
							e.printStackTrace();
							System.exit(3);
						}
					}
				};
				Thread thread2 = new Thread () {
					public void run () {
						try {
							runTcdbScraper();
						}
						catch (Exception e) {
							logger.error("Fatal error while scraping TCDB... Exiting TranSyT...");
							e.printStackTrace();
							System.exit(4);
						}
					}
				};
				Thread thread3 = new Thread () {
					public void run () {
						try {
							KeggAPI.searchKeegPTSGenesAndBuildFastaFiles();
						}
						catch (Exception e) {
							logger.error("Fatal error while scraping KEGG... Exiting TranSyT...");
							e.printStackTrace();
							System.exit(5);
						}
					}
				};
				thread1.start();
				thread2.start();
				thread3.start();

				while(thread1.isAlive() || thread2.isAlive() || thread3.isAlive()) {	//wait while all threads are not finish
					TimeUnit.MINUTES.sleep(1);
				}
				
				String keggFastaPath = FilesUtils.getLastKnownVersion(KeggAPI.PATH_LAST_KNOWN_VERSION);
				
				ReadFastaTcdb.buildFastaFileForAlignments(keggFastaPath);
			}
			else
				logger.info("Searching cached data...");

			Map<String, String> proteinFamilyDescription = FilesUtils.readMapFromFile(FilesUtils.getBackupFamilyDescriptionsFilesDirectory().concat("familyDescription.txt"));

			Map<String, TcNumberContainer> data2 = processExceptions(JSONFilesUtils.readDataBackupFile());		//refactor exceptions method to read other backups or delete method

			data2 = ProcessCompartments.processCompartments(data2);

			List<String[]> table = JSONFilesUtils.readTCDBScrapedInfo();

			Map<String, TcdbMetabolitesContainer> tcdbMetabolitesAux = ProcessTcdbMetabolites.processData(table);

			Map<String, TcdbMetabolitesContainer> tcdbMetabolites = new HashMap<>();

			if(tests && accTest != null) 
				tcdbMetabolites.put(accTest, tcdbMetabolitesAux.get(accTest));
			else
				tcdbMetabolites = tcdbMetabolitesAux;

			Map<String, Map<String, ReactionContainer>> metaCycData = JSONFilesUtils.readMetaCycDataBackupFile(getMetacycCpdToModelseed(useCache));

			Map<String, Map<String, TcNumberContainer>> transportReactions = 
					GenerateTransportReactions.generateReactions(data2, metaCycData, tcdbMetabolites, proteinFamilyDescription);

			JSONFilesUtils.writeJSONtcReactions(transportReactions);		//uncomment
			
//			if(!useCache)
//				KBaseFilesBuilder.buildKBaseOntologiesFile(descriptions);

			logger.info("Process complete, scraper shutting down...");

		} 
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//		 Pattern p = Pattern.compile("\\A\\d");
		//		 Matcher m = p.matcher("asd1asf");
		//		 boolean b = m.matches();
		//		
		//		 boolean c = m.find();
		//		System.out.println(b);
		//		System.out.println(c);


	}
	
	/**
	 * @return
	 * @throws Exception
	 */
	public static Map<String, String> getMetacycCpdToModelseed(boolean useCache) throws Exception{
		
		Map<String, String> compounds = ModelSEEDAPI.getModelseedCompoundsFromGithu(useCache);
		
		Map<String, String> metacycMapping = new HashMap<>();
		
		for(String cpd : compounds.keySet()) {
			
			String[] aliases = compounds.get(cpd).split("\\|");
			
			for(String alias : aliases) {
			
				if(alias.contains(" CPD")) {
					
					String identifier = alias.split("CPD")[1].split(";")[0];
					
					metacycMapping.put("CPD" + identifier, cpd);
				}
			}
		}
		return metacycMapping;
	}

	/**
	 * 
	 */
	private static void runMetacycScraper() {

		logger.info("Retrieving MetaCyc data...");

		Set<String> toSearch = ReadFastaTcdb.getAccessionForAlignment();

		new MetaCycAPI(toSearch);

		logger.info("MetaCyc search complete...");
	}

	/**
	 * @param useCache
	 * @throws Exception
	 */
	private static void runTcdbScraper() throws Exception {

		logger.info("Retrieving TCDB data...");

		TcdbRetriever.getSubstrates();

		Set<String> tcNumbers = TcdbExplorer.getTcNumbers(true);

		Set<String> toSearch = TcdbExplorer.generateTCsFamily(tcNumbers);

		FindTransporters.saveAllTCFamiliesInformation(toSearch);

		TcdbExplorer.getProteinsBelongingToFamilyDescription(tcNumbers);

		logger.info("TCDB search complete...");

	}

	/**
	 * Replace entries that are present in the exceptions file.
	 * @param data
	 * @return
	 */
	private static Map<String, TcNumberContainer> processExceptions(Map<String, TcNumberContainer> data) {

		Map<String, TcNumberContainer> exceptions = JSONFilesUtils.readJSONExceptionsFile();

		for(String key : exceptions.keySet()) {
			data.put(key, exceptions.get(key));

		}
		
		return data;
	}

	//	/**
	//	 * @param tcdbMetabolites
	//	 */
	//	private static void checkDescriptions(Map<String, TcdbMetabolitesContainer> tcdbMetabolites) {
	//		
	//		Set<String> symport = new HashSet<>();
	//		Set<String> uniport = new HashSet<>();
	//		Set<String> antiport = new HashSet<>();
	//		
	//		
	//		for(String accession : tcdbMetabolites.keySet()) {
	//			
	//			TcdbMetabolitesContainer container = tcdbMetabolites.get(accession);
	//			
	//			for(String tcNumber : container.getTcNumbers()) {
	//				
	//				String description = container.getDescription(tcNumber);
	//			
	//				if(description.matches("(?i).*uniport.*"))
	//					uniport.add(tcNumber);
	//				else if(description.matches("(?i).*symport.*"))
	//					symport.add(tcNumber);
	//				else if(description.matches("(?i).*antiport.*"))
	//					antiport.add(tcNumber);
	//				
	//			}
	//		}
	//		
	//	}

	//	List<String> metabolitesAux2 = new ArrayList<>();
	//	
	//	int n = 0;
	//	
	//	for(int i = 0; i < metabolitesAux.length; i++) {
	//		
	//		if(metabolitesAux[i].matches("\\w+\\sor\\s\\w+\\s*\\(\\w+\\)")) {
	//			
	//			String[] aux = metabolitesAux[i].split(" or ");
	//			
	//			if(metabolitesAux[i].contains("(in)"))
	//				metabolitesAux2.add(aux[0].concat(" (in)"));
	//			else if(metabolitesAux[i].contains("(out)")) 
	//				metabolitesAux2.add(aux[0].concat(" (out)"));
	//			else
	//				metabolitesAux2.add(aux[0]);
	//			
	//			metabolitesAux2.add(aux[1]);
	//		}
	//		else {
	//			metabolitesAux2.add(metabolitesAux[i]);
	//		}
	//	}
	//	
	//	String[] metabolites = new String[metabolitesAux2.size()];
	//	
	//	metabolites = metabolitesAux2.toArray()
}
