package pt.uminho.ceb.biosystems.transyt.service.transyt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.uminho.ceb.biosystems.merlin.bioapis.externalAPI.ebi.uniprot.TaxonomyContainer;
import pt.uminho.ceb.biosystems.merlin.bioapis.externalAPI.ncbi.NcbiAPI;
import pt.uminho.ceb.biosystems.transyt.scraper.APIs.UniprotAPIExtension;
import pt.uminho.ceb.biosystems.transyt.scraper.tcdb.tcdbTransportTypesRetriever.Retriever;
import pt.uminho.ceb.biosystems.transyt.service.internalDB.WriteByMetabolitesID;
import pt.uminho.ceb.biosystems.transyt.service.reactions.ProvideTransportReactionsToGenes;
import pt.uminho.ceb.biosystems.transyt.service.utilities.OrganismProperties;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.Organism;
import pt.uminho.ceb.biosystems.transyt.utilities.enumerators.Enumerators.STAIN;
import pt.uminho.ceb.biosystems.transyt.utilities.files.FilesUtils;
import pt.uminho.ceb.biosystems.transyt.utilities.transytUtilities.Properties;
import uk.ac.ebi.kraken.interfaces.uniprot.NcbiTaxon;

/**
 * @author Davide
 *
 */
public class TransyTestMain {

	private static final Logger logger = LoggerFactory.getLogger(TransytMain.class);

	public static void main(String[] args) throws Exception {

		Properties properties = null;
		
		properties = new Properties();
		properties.setTaxID(2485141);
		
		new OrganismProperties(properties, null).getOrganism();

		//		String workFolderID2 = "/Users/davidelagoa/Desktop/biolog_data/109/";
		//		
		//		String[] pathSplit = workFolderID2.split("/");
		//		
		//		String testId2 = pathSplit[pathSplit.length-1];
		//		
		//		System.out.println(testId2);

		//		KeggAPI.searchKeegPTSGenesAndBuildFastaFiles();
		//		
		//		String keggFastaPath = FilesUtils.getLastKnownVersion(KeggAPI.PATH_LAST_KNOWN_VERSION);
		//		
		//		ReadFastaTcdb.buildFastaFileForAlignments(keggFastaPath);		
		//		Set<String> toSearch = ReadFastaTcdb.getAccessionForAlignment();

		//		Set<String> toSearch = new HashSet<>();
		//		toSearch.add("P56579");
		//		MetaCycAPI api = new MetaCycAPI(toSearch);

		//		List<String> l = new ArrayList<>();
		//		l.add("WP_072782069.1");
		//
		//		List<UniProtEntry> entries = UniprotAPIExtension.getEntriesFromUniProtIDs(l, 0);
		//
		//		for(UniProtEntry entry : entries) {
		//			System.out.println(entry.getPrimaryUniProtAccession());
		//			for(DatabaseCrossReference db : entry.getDatabaseCrossReferences()) {
		//
		//				if(db.getDatabase().getName().equalsIgnoreCase("biocyc")) {
		////					System.out.println(db.getDatabase().getDisplayName());
		//					System.out.println(db.getPrimaryId());
		//				}
		//			}
		//			System.out.println();
		//		}

		//		Set<String> toSearch = ReadFastaTcdb.getAccessionForAlignment();
		//		Set<String> toSearch = new HashSet<>();	
		//		toSearch.add("P0AE06");
		//		MetaCycAPI api = new MetaCycAPI(toSearch);

		//		Integer taxID = Integer.valueOf(args[0]);

		//		String command = args[0];

		//		JSONFilesUtils.readJSONtcdbReactionsFile();

		//		Set<String> coiso = new HashSet<>();
		//		coiso.add("2.A.1");
		//		FindTransporters.saveAllTCFamiliesInformation(coiso);

		//				ReadFastaTcdb.buildFastaFileForAlignments();
		//
		//				Retriever.runRetriever(true, true, "P0AAF1");
		//		List<String> tests = List.of("results_m1_ev-50_p0", "results_m1_ev-50_p5", "results_m1_ev-50_p10", "results_m1_ev-50_p20",
		//				"results_m2_a0.5_s0.75", "results_m2_a0.75_s0.75");
		//
		//		for(String testDir : tests) {
		//
		//			File file = new File("/Users/davidelagoa/Desktop/biolog_data/results_biolog/" + testDir);
		//
		//			String auxRoot = "/Users/davidelagoa/Desktop/biolog_data/";
		//
		//			File[] directories = file.listFiles();
		//
		//			for(File directory : directories) {
		//
		//				Map<String, Set<String>> reactionsNoGrowthByCpd;
		//				try {
		//
		//					Map<String, String> no_growthCpds = KbValidation.read_no_growth_compounds(auxRoot + "Biolog_Merge_Lit_Pam_Binary_Data_V1.0.xlsx", directory.getName(), auxRoot);
		//					reactionsNoGrowthByCpd = KbValidation.findReactionsContaininigCompounds(no_growthCpds, directory.getAbsolutePath() + "/results/transyt.xml");
		//					FilesUtils.saveMapInFile3(directory.getAbsolutePath() + "/results/biolog_compounds_no_growth.txt", reactionsNoGrowthByCpd);
		//
		//
		//				} catch (Exception e) {
		//					System.out.println("Error on :" + directory.getName());
		//				}
		//
		//
		//			}
		//		}
		//		String directory = "/Users/davidelagoa/Desktop/biolog_data/results_biolog/results_m1_ev-50_p0/105";
		//		
		//		String auxRoot = "/Users/davidelagoa/Desktop/biolog_data/";
		//		
		//		Map<String, String> no_growthCpds = KbValidation.read_no_growth_compounds(auxRoot + "Biolog_Merge_Lit_Pam_Binary_Data_V1.0.xlsx", "105", auxRoot);
		//		
		//		PrettyPrints.printMap(no_growthCpds);
		//		
		//		
		//		Map<String, Set<String>> reactionsNoGrowthByCpd = KbValidation.findReactionsContaininigCompounds(no_growthCpds, directory + "/results/transyt.xml");
		//		FilesUtils.saveMapInFile3(directory + "/results/biolog_compounds_no_growth.txt", reactionsNoGrowthByCpd);
		//
		//		ModelSEEDRealatedOperations.assingModelSEEDIdentifiersToTranSyTReactions(true, null);

		//		String keggFastaPath = FilesUtils.getLastKnownVersion(KeggAPI.PATH_LAST_KNOWN_VERSION);
		//		
		//		ReadFastaTcdb.buildFastaFileForAlignments(keggFastaPath);
		//		
		//		List<String[]> table = JSONFilesUtils.readTCDBScrapedInfo();
		//
		//		Map<String, String> descriptions = ProcessTcdbMetabolites.getTCDescriptions(table);
		//		JSONFilesUtils.buildKBaseOntologiesFile("1.0", descriptions);
		
		System.exit(0);

		//		String path = "/Users/davidelagoa/Desktop/ecoli/ecoli_iML1515/";



		String testId = "GCF_000008925.1";
		//		String testId = "GCF_000005845.2";

		if(args.length > 1)
			testId = args[1];

		String rootPath = "/Users/davidelagoa/Desktop/reference_data_kbase2/tests";
		//		
		//		String rootPath = "/Users/davidelagoa/Desktop/ecoli";

		if(args.length > 2)
			rootPath = args[2];

		String path = rootPath + "/" + testId + "/";

		String filePath = path + "protein.faa";

		String modelPath = path + "model.xml";

		String command = "3";

		if(args.length > 0)
			command = args[0];


		String resultPath = path + "results/";
		//String resultPath = path + "results_e#1/";

		String metabolitesPath = path + "metabolites.txt";

		String workFolderID = path + "workdir/";

		File workF = new File(workFolderID);

		if(!workF.exists())
			workF.mkdir();

		STAIN stain = STAIN.gram_negative; //optional ---> user selection
		Organism organism = null;

		switch (command) {
		case "1":

			logger.info("Scraper and database service option selected!");

			Retriever.runRetriever(true, true, "P46482");
			//			WriteByMetabolitesID.start(properties);

			break;

		case "2":  //ignore TCDB scraper

			logger.info("Database service option selected!");

			properties = new Properties();

			WriteByMetabolitesID.start(properties, true);

			break;

		case "3": 

			logger.info("Transport reactions annotation option selected");

			properties = new Properties(path.concat("params.txt"));

			//			String workFolderID = args[1].trim();
			//
			//			String taxPath = workFolderID.concat("taxID.txt");
			//
			//			Integer taxID = Integer.valueOf(readWordInFile(path + "taxId.txt"));
			//
			//			String filePath = workFolderID.concat("genome.faa");
			//
			//			String modelPath = workFolderID.concat("model.xml");
			//			
			//			String metabolitesPath = workFolderID.concat("metabolites.txt");
			//
			//			String resultPath = "";
			//
			//			try {
			//
			//				if(args[2] != null || !args[2].isEmpty()) {
			//					resultPath = args[2];
			//				}
			//			}
			//			catch(ArrayIndexOutOfBoundsException e){
			//
			//				resultPath = workFolderID.concat("transyt.xml");
			//			}

			//			properties.setDefaultLabel(MetaboliteReferenceDatabaseEnum.KEGG);

			//			properties.setDefaultLabel(MetaboliteReferenceDatabaseEnum.ModelSEED);
			properties.setOverrideCommonOntologyFilter(false);	//this option is false by default
			properties.setDatabaseURI("bolt://localhost:7687");
			properties.setForceBlast(true);

			organism = new OrganismProperties(properties, stain).getOrganism();

			//			String filePath = args[1];
			//			String modelPath = args[2];

			new ProvideTransportReactionsToGenes(workFolderID, organism, filePath, modelPath, 
					metabolitesPath, resultPath, properties, true);

			//			Map<String, String> growthCpds = KbValidation.read_growth_compounds(rootPath + "Biolog_Merge_Lit_Pam_Binary_Data_V1.0.xlsx", testId, rootPath);
			//			Map<String, Set<String>> reactionsByCpd = KbValidation.findReactionsContaininigCompounds(growthCpds, resultPath + "results/transyt.xml");
			//
			//			Map<String, String> no_growthCpds = KbValidation.read_no_growth_compounds(rootPath + "Biolog_Merge_Lit_Pam_Binary_Data_V1.0.xlsx", testId, rootPath);
			//			Map<String, Set<String>> reactionsNoGrowthByCpd = KbValidation.findReactionsContaininigCompounds(no_growthCpds, resultPath + "results/transyt.xml");
			//
			//			FilesUtils.saveMapInFile3(resultPath + "results/biolog_compounds_no_growth.txt", reactionsNoGrowthByCpd);
			//			FilesUtils.saveMapInFile3(resultPath + "results/biolog_compounds.txt", reactionsByCpd);

			break;

		case "4": 

			logger.info("Transporters proteins annotation option selected");

			properties = new Properties(path.concat("params.txt"));

			//			String workFolderID = args[1].trim();
			//
			//			String taxPath = workFolderID.concat("taxID.txt");
			//
			//			Integer taxID = Integer.valueOf(readWordInFile(path + "taxId.txt"));
			//
			//			String filePath = workFolderID.concat("genome.faa");
			//
			//			String modelPath = workFolderID.concat("model.xml");
			//			
			//			String metabolitesPath = workFolderID.concat("metabolites.txt");
			//
			//			String resultPath = "";
			//
			//			try {
			//
			//				if(args[2] != null || !args[2].isEmpty()) {
			//					resultPath = args[2];
			//				}
			//			}
			//			catch(ArrayIndexOutOfBoundsException e){
			//
			//				resultPath = workFolderID.concat("transyt.xml");
			//			}

			//			properties.setDefaultLabel(MetaboliteReferenceDatabaseEnum.KEGG);

			//			properties.setDefaultLabel(MetaboliteReferenceDatabaseEnum.ModelSEED);
			//			properties.setOverrideCommonOntologyFilter(false);	//this option is false by default
			properties.setDatabaseURI("bolt://localhost:7687");
			properties.setForceBlast(false);

			organism = new OrganismProperties(properties, stain).getOrganism();

			//			String filePath = args[1];
			//			String modelPath = args[2];

			new ProvideTransportReactionsToGenes(workFolderID, organism, filePath, modelPath, 
					metabolitesPath, resultPath, properties, false);

			//			Map<String, String> growthCpds = KbValidation.read_growth_compounds(rootPath + "Biolog_Merge_Lit_Pam_Binary_Data_V1.0.xlsx", testId, rootPath);
			//			Map<String, Set<String>> reactionsByCpd = KbValidation.findReactionsContaininigCompounds(growthCpds, resultPath + "results/transyt.xml");
			//
			//			Map<String, String> no_growthCpds = KbValidation.read_no_growth_compounds(rootPath + "Biolog_Merge_Lit_Pam_Binary_Data_V1.0.xlsx", testId, rootPath);
			//			Map<String, Set<String>> reactionsNoGrowthByCpd = KbValidation.findReactionsContaininigCompounds(no_growthCpds, resultPath + "results/transyt.xml");
			//
			//			FilesUtils.saveMapInFile3(resultPath + "results/biolog_compounds_no_growth.txt", reactionsNoGrowthByCpd);
			//			FilesUtils.saveMapInFile3(resultPath + "results/biolog_compounds.txt", reactionsByCpd);

			break;

		default:
			break;
		}
	}

	/**
	 * Reads word in a given file;
	 * 
	 * @param path
	 * @return
	 */
	public static String readWordInFile(String path){

		try {

			BufferedReader reader = new BufferedReader(new FileReader(path));

			String line;

			while ((line = reader.readLine()) != null) {

				if(!line.isEmpty() &&  !line.contains("**")) {
					reader.close();
					return line.trim();
				}
			}

			reader.close();

		} 
		catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}


	//	Map<String, List<AlignmentCapsule>> blastResults;
	//	TransytGraphDatabaseService service;


	//	public TriageMain(){}

	//	public void start() {
	//		
	//		String targetTCnumber = "1.A.4.6.2";
	//		
	//		Node accessionNode = service.findUniprotAccessionNode("Q7Z020");
	//		
	//		Node tcNumberNode = service.getEndNodeByEntryIDAndLabel(accessionNode, targetTCnumber, TransytGeneralProperties.TC_Number, TransytRelationshipType.has_tc);
	//		
	//		Set<Node> reactionNodes = service.getAllRelatedNodes(tcNumberNode, TransytRelationshipType.has_reaction);
	//		
	//		Set<ReactionContainer> reactionContainers = getAllNodesAsReactionContainers(targetTCnumber, reactionNodes);


	//		System.out.println("coiso " + blastResults.keySet());
	//		
	//		for(String key : blastResults.keySet()) {
	//			
	//			System.out.println(key + " >>>>>> " + blastResults.get(key).size());
	//			
	//			for(AlignmentCapsule capsule : blastResults.get(key)) {
	//				
	//				if(capsule.getScore() > 0.5)
	//					System.out.println(capsule.getTarget());
	//				
	//			}
	//			
	//		}

	//		List<AlignmentCapsule> accessionRes = blastResults.get("OPH81701.1");
	//		
	//		System.out.println(blastResults.get("OPH81701.1"));
	//		
	//		for(AlignmentCapsule align : accessionRes) {
	//			
	//			System.out.println(align.getTcdbID());
	//			
	//			
	//			
	//			
	//		}

	//	}

	//	private static Set<ReactionContainer> getAllNodesAsReactionContainers(String tcNumber, Set<Node> reactionNodes){
	//		
	//		Set<ReactionContainer> set = new HashSet<>();
	//		
	//			try {
	//				for(Node node : reactionNodes) {
	//				 
	//					ReactionContainer container = new ReactionContainer(node.getProperty(TriageGeneralProperties.Reaction.toString()).toString(),
	//							Boolean.valueOf(node.getProperty(TriageGeneralProperties.Reversible.toString()).toString()));
	//					
	//					container.setTransportType(TypeOfTransporter.valueOf(node.getProperty(TriageGeneralProperties.TransportType.toString()).toString()));
	//					container.setReactionWithIDs(node.getProperty(TriageGeneralProperties.Reaction_With_IDs.toString()).toString());
	//					container.setReactionID(node.getProperty(TriageGeneralProperties.ReactionID.toString()).toString());
	//					
	//					set.add(container);
	//				}
	//			} 
	//			catch (Exception e) {
	//				
	//				logger.warn("An error occurred while retrieving the pt.uminho.ceb.biosystems.transyt.service.reactions of the TC number node {}", tcNumber);
	//				
	//				e.printStackTrace();
	//			}
	//		
	//		return set;
	//	}

	public static void crawl_directories() throws FileNotFoundException, UnsupportedEncodingException {

		PrintWriter writer = new PrintWriter("/Users/davidelagoa/Desktop/reference_data_kbase/taxonomies.tsv", "UTF-8");

		File dir = new File("/Users/davidelagoa/Desktop/reference_data_kbase/genomes");

		for(File sub_dir : dir.listFiles()) {

			if(sub_dir.isDirectory()) {

				try {
					String taxId = FilesUtils.readMapFromFile(sub_dir + "/params.txt").get("taxID");
					String text = get_taxa(Integer.valueOf(taxId));

					writer.println(sub_dir.getName() + "\t" + taxId + "\t" + text);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}


			}
		}

		writer.close();
	}

	public static String get_taxa(Integer taxID) throws Exception{

		List<String> taxonomy_aux = new ArrayList<>();
		String organism = null;

		try {
			TaxonomyContainer taxon = NcbiAPI.getTaxonomyFromNCBI(Long.valueOf(taxID.toString()), 0);

			for(NcbiTaxon taxEntry : taxon.getTaxonomy()) {
				taxonomy_aux.add(taxEntry.getValue());
			}

			organism = taxon.getSpeciesName();
		}
		catch (Exception e) {

			e.printStackTrace();
		}

		if(taxonomy_aux == null || taxonomy_aux.isEmpty()) {

			TaxonomyContainer uniprot = UniprotAPIExtension.getTaxonomyFromNCBITaxnomyID(taxID, 0);

			if(uniprot != null) {

				organism = uniprot.getSpeciesName();
				taxonomy_aux = Arrays.asList(uniprot.getTaxonomy().toString().replaceAll("\\[", "").replaceAll("\\]", "").split(", "));

			}
		}

		taxonomy_aux.remove(taxonomy_aux.size()-1);

		return taxonomy_aux.toString().replace("[", "").replace("]", "") + ", " + organism;

	}


}
