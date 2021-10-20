package pt.uminho.ceb.biosystems.transyt.service.reactions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gargoylesoftware.htmlunit.javascript.host.dom.Node;

import pt.uminho.ceb.biosystems.merlin.transporters.core.utils.Enumerators.TransportType;
import pt.uminho.ceb.biosystems.merlin.utilities.containers.capsules.AlignmentCapsule;
import pt.uminho.ceb.biosystems.merlin.utilities.io.FileUtils;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.Container;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.MetaboliteCI;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.ReactionCI;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.ReactionConstraintCI;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.ReactionTypeEnum;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.io.exceptions.ReactionAlreadyExistsException;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.io.readers.JSBMLLevel3Reader;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.io.readers.JSBMLReader;
import pt.uminho.ceb.biosystems.transyt.scraper.tcdb.reactionsGenerator.GenerateTransportReactions;
import pt.uminho.ceb.biosystems.transyt.scraper.tcdb.utilities.ProcessTcdbMetabolites;
import pt.uminho.ceb.biosystems.transyt.service.blast.Blast;
import pt.uminho.ceb.biosystems.transyt.service.internalDB.WriteByMetabolitesID;
import pt.uminho.ceb.biosystems.transyt.service.kbase.ModelSEEDRealatedOperations;
import pt.uminho.ceb.biosystems.transyt.service.kbase.Reports;
import pt.uminho.ceb.biosystems.transyt.service.kbase.Tools;
import pt.uminho.ceb.biosystems.transyt.service.neo4jRest.RestNeo4jGraphDatabase;
import pt.uminho.ceb.biosystems.transyt.service.relations.GPRAssociations;
import pt.uminho.ceb.biosystems.transyt.service.transytDatabase.TransytGeneralProperties;
import pt.uminho.ceb.biosystems.transyt.service.transytDatabase.TransytNeo4jInitializer;
import pt.uminho.ceb.biosystems.transyt.service.transytDatabase.TransytNode;
import pt.uminho.ceb.biosystems.transyt.service.transytDatabase.TransytNodeLabel;
import pt.uminho.ceb.biosystems.transyt.service.transytDatabase.TransytRelationship;
import pt.uminho.ceb.biosystems.transyt.service.transytDatabase.TransytRelationshipType;
import pt.uminho.ceb.biosystems.transyt.utilities.biocomponents.OutputTransytFormat;
import pt.uminho.ceb.biosystems.transyt.utilities.biocomponents.TransytSBMLLevel3Writer;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.GeneContainer;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.Organism;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.ReactionContainer;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.Subunits;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.TcdbMetabolitesContainer;
import pt.uminho.ceb.biosystems.transyt.utilities.enumerators.Enumerators;
import pt.uminho.ceb.biosystems.transyt.utilities.enumerators.Enumerators.TypeOfTransporter;
import pt.uminho.ceb.biosystems.transyt.utilities.enumerators.MetaboliteReferenceDatabaseEnum;
import pt.uminho.ceb.biosystems.transyt.utilities.files.FilesUtils;
import pt.uminho.ceb.biosystems.transyt.utilities.files.JSONFilesUtils;
import pt.uminho.ceb.biosystems.transyt.utilities.files.WriteExcel;
import pt.uminho.ceb.biosystems.transyt.utilities.transytUtilities.Properties;

public class ProvideTransportReactionsToGenes {

	public static final String NO_TCNUMBER_ASSOCIATED = "Undefined_TCnumber";
	public static final String Hpr_FAMILY = "8.A.8.";
	public static final String Phosphotransferase_FAMILY = "8.A.7.";

	private Map<String, List<AlignmentCapsule>> blastResults;
	private RestNeo4jGraphDatabase service;
	//	private Map<String, Set<String>> homologousGenes;
	private String[] taxonomy;
	private String organism;
	//	private Map<String, Map<String, Set<ReactionContainer>>> reactionContainers;
	private Map<String, Map<String, Double>> hprHomologues = new HashMap<>();
	private Map<String, Map<String, Double>> phosphotransferaseHomologues = new HashMap<>();
	private Map<String, String[]> taxonomies;
	private Map<String, String> organisms;
	private Map<String, Set<String>> resultsByEvalue;
	private Map<String, List<String>> mainReactions;
	private Map<String, Set<String>> reactionsByTcNumber;
	private Map<String, Set<String>> reactionsByTcNumberForAnnotation;	//1
	private Set<String> hitsWithoutReactions;
	private Map<String, Map<String, Set<String>>> finalResults;
	private Map<String, ReactionContainer> reactionContainersByID;
	private Properties properties;
	private Set<String> reactionsToIgnore;

	private Set<String> tcNumbersNotPresentInTransytDatabase;		//tcNumbers present in this set are not yet in the 
	private Set<String> modelMetabolites;				//neo4j database (solution: update the database)

	private TransytRelationshipType defaultRelationshipReactants;
	private TransytRelationshipType defaultRelationshipProducts;
	private TransytNodeLabel defaultMetaboliteLabel;
	private Subunits subunits;
	private String modelPath = "";
	private Integer taxonomyID = null;
	private Map<String, String> metabolitesNames;
	private Map<String, String> metabolitesFormulas;
	private Map<String, Map<String, Double>> reportByEvalue = new HashMap<>();
	private Map<String, Map<String, Set<String>>> reportByEvalueAux = new HashMap<>();

	//	private static final Map<String, Integer> GENERATIONS_EXCEPTION_FILE = FilesUtils.readGenerationsLimitFile(FilesUtils.getDictionatiesAndConfigurationsDirectory().concat("ChildsLimits.txt"));

	private static final Logger logger = LoggerFactory.getLogger(WriteByMetabolitesID.class);

	public ProvideTransportReactionsToGenes(String workFolderID, Organism organismProperties, String queryPath, String modelPath,
			String metabolitesPath, String resultPath, Properties properties, boolean searchReactions) {

		logger.info("TranSyT initialized!");

		reportByEvalueAux = new HashMap<>();

		//		homologousGenes = new HashMap<>();
		//		reactionContainers = new HashMap<>();
		this.taxonomies = new HashMap<>();
		this.organisms = new HashMap<>();
		this.tcNumbersNotPresentInTransytDatabase = new HashSet<>();
		this.reactionContainersByID = new HashMap<>();
		this.modelPath = modelPath;

		this.taxonomyID = organismProperties.getTaxonomyID();
		this.taxonomy = organismProperties.getTaxonomy();	
		this.organism = organismProperties.getOrganism();	

		this.properties = properties;
		this.metabolitesNames = new HashMap<>();
		this.metabolitesFormulas = new HashMap<>();

		this.subunits = new Subunits();
		setDefaultRelationshipsToSearch();

		try {
			Blast blast = new Blast(workFolderID, queryPath, properties);
			blastResults = blast.getAlignmentsByQuery();

			logger.info("Beginning transaction with neo4j TranSyT database...");

			service = TransytNeo4jInitializer.getDatabaseService(properties);

			//			service = new TransytGraphDatabaseService(graphDatabaseService);

			resultPath = resultPath.concat("results/");

			File resultsFile = new File(resultPath);

			if(!resultsFile.exists())
				resultsFile.mkdirs();

			//						resultsByEvalue = getReactionsForGenesByEvalue();
			resultsByEvalue = getReactionsForGenesByEvalueNewMethod();  //new method

			Map<String, GeneContainer> genesContainers = buildGenesContainers();

			if(searchReactions)
				annotateReactionsToGenes(resultPath, metabolitesPath, genesContainers);
			else
				annotateTransportProteinsFunctionTCNumbers(resultPath, genesContainers);

			service.close();

			logger.info("Transaction terminated!");

			logger.info("TranSyT neo4j database shutdown...");

		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void start(String metabolitesPath,String queryPath, String resultPath, boolean searchReactions) {

		try {

			//			String path = new File(queryPath).getParent().concat("/");



		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Models' reactions annotation method
	 * 
	 * @param resultPath
	 * @param genesContainers
	 * @throws Exception
	 */
	private void annotateReactionsToGenes(String resultPath, String metabolitesPath, Map<String, GeneContainer> genesContainers) throws Exception {

		reactionsToIgnore = new HashSet<>();

		this.modelMetabolites = Tools.readModelMetabolitesFromSBML(modelPath);		//Alterar isto para ler do container directamente

		if(this.modelMetabolites == null) {
			logger.info("Searching metabolites.txt file...");
			this.modelMetabolites = FilesUtils.readWordsInFile(metabolitesPath);

			if(this.modelMetabolites.isEmpty()) {	//quick fix to not brake generic method
				this.modelMetabolites = null;
				logger.warn("Filter by compounds in model disabled!");
			}
		}	

		if(this.modelMetabolites != null)
			logger.info("Model metabolites size: " + this.modelMetabolites.size());

		logger.debug("Searching reactions...");

		Set<String> tcNumbers = identifyTcNumbersForSearch();
		logger.info("Searching reactions in {} tcNumbers", tcNumbers.size());

		reactionsByTcNumber = getReactionsByTcNumber(tcNumbers);

		if(reactionContainersByID.keySet().size() == 0 && modelMetabolites != null)
			logger.warn("No metabolites present in the model are available in the selected reactions!");

		Map<String, Set<String>> similaritiesResults = new HashMap<>();	//the results of this map will be sorted by similarity

		if(!properties.isIgnoreMethod2())
			similaritiesResults = ReactionsPredictor.getReactionsForGenesBySimilarities(genesContainers,
					reactionsByTcNumber, blastResults, mainReactions, reactionsToIgnore, properties);

		//			for(String tc : reactionsByTcNumber.keySet())
		//				System.out.println(tc + "\t" + reactionsByTcNumber.get(tc));


		//			String path = "C:\\Users\\Davide\\OneDrive - Universidade do Minho\\UMinho\\Tese\\KBase\\Reports\\ecoli_Validation30\\";

		//			generateFinalResults(similaritiesResults);

		generateFinalResultsAuxValidation_filter_reactions(similaritiesResults, genesContainers);

		Reports.saveReportByEvalue(resultPath, reportByEvalue, reportByEvalueAux);

		//						System.out.println(finalResults);

		logger.debug("Reactions search complete!");

		Map<String, Set<String>> subunitsInDatabase = service.findAllSubunitsInDatabase();

		Map<String, Map<String, String>> proteinComplexes = GPRAssociations.getGPR(subunitsInDatabase, 
				blastResults, genesContainers, properties);

		//			for(String key : finalResults.keySet()) 
		//				for(String newkey : finalResults.get(key).keySet()) 
		//					System.out.println(key + "\t" + newkey + "\t" + finalResults.get(key).get(newkey));


		Map<String, String> geneRules = GPRAssociations.buildGeneRules(resultPath, service, proteinComplexes, finalResults, 
				subunitsInDatabase, this.hprHomologues, this.phosphotransferaseHomologues, this.finalResults.keySet());

		//	System.out.println("here");

		OutputTransytFormat output = new OutputTransytFormat(resultPath + "/reactions_references.txt", finalResults, metabolitesFormulas, 
				reactionContainersByID, geneRules, metabolitesNames);

		Container container = new Container(output);

		//			countReactionsByGene(geneRules, container.getReactions());

		//				
		//			Map<String, String> reactionsIDS = Reports.generateKBaseReport(jobIdentification, organism, taxID, queryFileTotalOfGenes, properties, finalResults, service);
		//
		//
		//
		//			
		//String fileName = "sbmlResult".concat("_qCov_").concat(Double.toString(properties.getQueryCoverage())).concat("_eValThresh_").concat(Double.toString(properties.geteValueThreshold())).concat(".xml");
		//

		String sbmlPath = resultPath.concat("/transyt.xml");
		File fileResults = new File(sbmlPath);

		TransytSBMLLevel3Writer sbml = new TransytSBMLLevel3Writer(fileResults.getAbsolutePath(), container, taxonomyID, false);
		//			TransytSBMLLevel3Writer sbml = new TransytSBMLLevel3Writer(path.concat("SBML/").concat(fileName), container, taxonomyID, false);

		sbml.writeToFile();

		logger.info("Generating md5 checksum.");

		String hash = FilesUtils.getFileChecksum(MessageDigest.getInstance("MD5"), fileResults);

		logger.info("md5 checksum generated: {}", hash);

		FilesUtils.saveWordInFile(fileResults.getParent().concat("/checksum.md5"), hash);

		logger.info("Saving results in .zip file");

		System.out.println(fileResults.getParent());
		System.out.println(fileResults.getAbsolutePath().concat("/results.zip"));

		FileUtils.createZipFile(fileResults.getParent(), fileResults.getParentFile().getParent().concat("/results.zip"), 5);

		logger.info("zip file created!");
	}

	/**
	 * Method to just try to annotate the genes with a TC number and short description, ignoring reactions association.
	 * 
	 * @param resultPath
	 * @param genesContainers
	 * @throws Exception
	 */
	private void annotateTransportProteinsFunction(String resultPath, Map<String, GeneContainer> genesContainers) throws Exception {

		List<String[]> table = JSONFilesUtils.readTCDBScrapedInfo();

		Map<String, String> descriptions = ProcessTcdbMetabolites.getTCDescriptions(table);

		Map<String, String> annotationsByGene = new HashMap<>();

		for(String queryAccession : this.resultsByEvalue.keySet()) {

			String annotation = null;

			if(genesContainers.get(queryAccession).getAnnotatedFamily() != null) {

				if(resultsByEvalue.containsKey(queryAccession)) {

					String tcFamily = genesContainers.get(queryAccession).getAnnotatedFamily();

					for(String tcNumber : resultsByEvalue.get(queryAccession)) {

						if(tcNumber.contains(tcFamily)){

							String shortDescription = descriptions.get(tcNumber).split("\\.")[0];

							if(annotation != null)
								annotation = annotation + "### ";
							else
								annotation = "";

							annotation = annotation + tcNumber + " - " + shortDescription;
						}
					}
				}
			}

			if(annotation != null)
				annotationsByGene.put(queryAccession, annotation);
		}

		FilesUtils.saveMapInFile(resultPath + "transport_genes_annotation.txt", annotationsByGene);
	}
	
	/**
	 * Method to just try to annotate the genes with a TC number, ignoring reactions association and descriptions.
	 * 
	 * @param resultPath
	 * @param genesContainers
	 * @throws Exception
	 */
	private void annotateTransportProteinsFunctionTCNumbers(String resultPath, Map<String, GeneContainer> genesContainers) throws Exception {

//		List<String[]> table = JSONFilesUtils.readTCDBScrapedInfo();
//		Map<String, String> descriptions = ProcessTcdbMetabolites.getTCDescriptions(table);

		Map<String, String> annotationsByGene = new HashMap<>();

		for(String queryAccession : this.resultsByEvalue.keySet()) {

			String tcNumbers = "";

			if(genesContainers.get(queryAccession).getAnnotatedFamily() != null) {

				if(resultsByEvalue.containsKey(queryAccession)) {

					String tcFamily = genesContainers.get(queryAccession).getAnnotatedFamily();

					for(String tcNumber : resultsByEvalue.get(queryAccession)) {

						if(tcNumber.contains(tcFamily))
							tcNumbers = tcNumbers + tcNumber + ";";
					}
				}
			}

			if(!tcNumbers.isEmpty())
				annotationsByGene.put(queryAccession, tcNumbers.replaceAll(";$", ""));
		}
		
		FilesUtils.saveMapInFile(resultPath + "transport_genes_annotation.txt", annotationsByGene);
	}

	private void countReactionsByGene(Map<String, String> geneRules, Map<String, ReactionCI> sbmlReactions) {

		Map<String, String> counts2 = new HashMap<>();

		Map<String, Set<String>> listOfReactionsPerGene = new HashMap<>();

		for(String react : geneRules.keySet()) {

			if(geneRules.get(react) != null) {

				String genes = geneRules.get(react).replace("and", ",").replaceAll("or", ",").replaceAll("\\(", "").replaceAll("\\)", "").replaceAll("\\s+", "");

				//				System.out.println(genes);

				String[] all = genes.split(",");

				for(String g : all) {

					if(listOfReactionsPerGene.containsKey(g)) {
						listOfReactionsPerGene.get(g).add(react);
					}
					else {
						Set<String> set = new HashSet<>();
						set.add(react);

						listOfReactionsPerGene.put(g, set);
					}
				}
			}
		}

		for(String key : listOfReactionsPerGene.keySet())
			counts2.put(key, listOfReactionsPerGene.get(key).size()+"");

		//		FilesUtils.saveMapInFile("C:\\Users\\Davide\\OneDrive - Universidade do Minho\\UMinho\\Tese\\KBase\\Reports\\ecoli_Validation30\\SBML\\countsReactionsSBMLGenes.txt", counts2);

		//		writeExcelByGene(listOfReactionsPerGene, sbmlReactions, finalResults);

	}

	public void writeExcelByGene(Map<String, Set<String>> reactionsByGeneModel, Map<String, ReactionCI> sbmlReactions, Map<String, Map<String, Set<String>>> finalResults ) {

		try {

			String path = "C:\\Users\\Davide\\OneDrive - Universidade do Minho\\UMinho\\Tese\\KBase\\Reports\\ecoli_Validation30\\SBML\\listOfReactionsPerGene.xlsx";

			String sheetName = "Sheet1";//name of sheet

			Workbook workbook = new XSSFWorkbook();
			Sheet sheet  = workbook.createSheet(sheetName) ;

			int i = 1;

			for(String gene : reactionsByGeneModel.keySet()) {

				if(finalResults.containsKey(gene)) {

					Set<String> reactions = reactionsByGeneModel.get(gene);

					Row row = sheet.createRow(i);

					int headerLine = i;

					row.createCell(0).setCellValue(gene);

					row.createCell(5).setCellValue(finalResults.get(gene).keySet().toString());

					i++;

					for(String r : reactions) {

						if(sbmlReactions.containsKey(r)) {

							row = sheet.createRow(i);

							String reaction = sbmlReactions.get(r).toStringStoiquiometry();

							row.createCell(1).setCellValue(r);
							row.createCell(2).setCellValue(reaction);

							Set<String> names = new HashSet<>();

							for(String m : sbmlReactions.get(r).getMetaboliteSetIds()) {

								m = m.split("_")[1];

								if(metabolitesNames.containsKey(m)) {
									names.add(metabolitesNames.get(m));

									reaction = reaction.replace(m, metabolitesNames.get(m));
								}

							}
							row.createCell(3).setCellValue(reaction);
							row.createCell(4).setCellValue(names.toString());

							i++;

						}

					}

					row = sheet.getRow(headerLine);

					if(row == null)
						row = sheet.createRow(headerLine);

					i++;
				}
			}

			FileOutputStream fileOut = new FileOutputStream(path);

			workbook.write(fileOut);
			fileOut.flush();
			fileOut.close();

			workbook.close();

			System.out.println("xlsx table created");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Map<String, Set<String>> getReactionsForGenesByEvalue() {

		Map<String, Set<String>> results = new LinkedHashMap<>();

		reportByEvalue = new HashMap<>();

		////////////////////////
		//		Map<String, List<AlignmentCapsule>> blastResults2 = new HashMap<>();
		//		
		//		blastResults2.put("WP_046131684.1 MULTISPECIES: Na+/H+ antiporter subunit D [Bacillus]", blastResults.get("WP_046131684.1 MULTISPECIES: Na+/H+ antiporter subunit D [Bacillus]"));
		///////////////////////

		for(String key : blastResults.keySet()) {

			Map<String, Double> auxMap = new HashMap<>();

			Map<String, Set<String>> subunitsFound = new HashMap<>();
			Map<String, Double> evaluesEntry = new HashMap<>();

			Set<String> tcNumbers = new HashSet<>();

			for(AlignmentCapsule capsule: blastResults.get(key)) {

				String tcNumber = capsule.getTcdbID();

				//								if(tcNumber.equals("1.B.1.1.1")) {
				//									System.out.println(key);
				//									System.out.println(tcNumber);
				//									System.out.println(capsule.getEvalue() <= properties.geteValueThreshold());
				//									
				//								}	

				if(!evaluesEntry.containsKey(tcNumber))
					evaluesEntry.put(tcNumber, capsule.getEvalue());

				if(capsule.getEvalue() <= properties.geteValueThreshold()) {
					tcNumbers.add(tcNumber);

					auxMap.put(tcNumber, capsule.getEvalue());
				}

				Set<String> accessions = new HashSet<>();

				if(subunitsFound.containsKey(tcNumber))
					accessions = subunitsFound.get(tcNumber);

				accessions.add(capsule.getTarget());
				subunitsFound.put(tcNumber, accessions);
			}

			this.subunits.addEntry(key, evaluesEntry, subunitsFound);

			results.put(key, tcNumbers);

			if(auxMap.size() > 0)
				reportByEvalue.put(key, auxMap);
		}

		//				for(String key : results.keySet()) 
		//					System.out.println(key + "\t" + results.get(key).size());

		return results;

	}

	/**
	 * @return
	 */
	private Map<String, Set<String>> getReactionsForGenesByEvalueNewMethod() {

		Map<String, Set<String>> results = new LinkedHashMap<>();

		reportByEvalue = new HashMap<>();

		for(String key : blastResults.keySet()) {

			Map<String, Double> auxMap = new HashMap<>();

			Map<String, Set<String>> subunitsFound = new HashMap<>();
			Map<String, Double> evaluesEntry = new HashMap<>();

			Set<String> tcNumbers = new HashSet<>();

			Map<String, Double> notAccepted = new HashMap<>();

			List<String> positions = new ArrayList<>();

			for(AlignmentCapsule capsule: blastResults.get(key)) {

				String tcNumber = capsule.getTcdbID();
				String geneId = capsule.getTarget();
				double evalue = capsule.getEvalue();

				String auxId = tcNumber + "@" + geneId;

				if(tcNumber.startsWith(Hpr_FAMILY)) {
					Map<String, Double> hpr = new HashMap<>();

					if(this.hprHomologues.containsKey(auxId))
						hpr = this.hprHomologues.get(auxId);

					hpr.put(key, evalue);
					this.hprHomologues.put(auxId, hpr);
				}
				else if(tcNumber.startsWith(Phosphotransferase_FAMILY)) {
					Map<String, Double> phos = new HashMap<>();

					if(this.phosphotransferaseHomologues.containsKey(auxId))
						phos = this.phosphotransferaseHomologues.get(auxId);

					phos.put(key, evalue);
					this.phosphotransferaseHomologues.put(auxId, phos);
				}
				else {
					if(!evaluesEntry.containsKey(tcNumber))
						evaluesEntry.put(tcNumber, capsule.getEvalue());

					if(evalue <= properties.geteValueThreshold()) {
						tcNumbers.add(tcNumber);

						auxMap.put(tcNumber, capsule.getEvalue());
					}
					else if(evalue <= properties.getLimitEvalueAcceptance()){

						notAccepted.put(auxId, capsule.getEvalue());

						for(int i = 0; i < positions.size(); i++) {		//creates a sorted list of the evalues of all entries

							if(i == 0 && evalue < notAccepted.get(positions.get(i))) {
								positions.add(i, auxId);
								break;
							}
							else if(i > 0 && evalue > notAccepted.get(positions.get(i-1)) && evalue < notAccepted.get(positions.get(i))){
								positions.add(i, auxId);
								break;
							}
						}

						if(!positions.contains(auxId))	//adds when list is empty and to the end of the list
							positions.add(auxId);

					}

					Set<String> accessions = new HashSet<>();

					if(subunitsFound.containsKey(tcNumber))
						accessions = subunitsFound.get(tcNumber);

					accessions.add(geneId);
					subunitsFound.put(tcNumber, accessions);
				}
			}

			double entriesToAccept = Math.ceil(positions.size() * (properties.getPercentageAcceptance() / 100));	//round up

			for(int i = 0; i < entriesToAccept; i++) {	

				String tcNumber = positions.get(i).split("\\@")[0];

				tcNumbers.add(tcNumber);

				auxMap.put(tcNumber, notAccepted.get(positions.get(i)));
			}

			this.subunits.addEntry(key, evaluesEntry, subunitsFound);

			results.put(key, tcNumbers);

			if(auxMap.size() > 0)
				reportByEvalue.put(key, auxMap);
		}

		//				for(String key : results.keySet()) 
		//					System.out.println(key + "\t" + results.get(key).size());

		return results;

	}


	/**
	 * Merge the results of both methods
	 * 
	 * @param similaritiesResults
	 * @param genesContainers 
	 */
	private void generateFinalResultsAuxValidation(Map<String, Set<String>> similaritiesResults, 
			Map<String, GeneContainer> genesContainers) {

		finalResults = new HashMap<>();

		if(this.properties.isIgnoreMethod1())
			resultsByEvalue = new HashMap<String, Set<String>>();

		for(String queryAccession : blastResults.keySet()) {

			Set<String> reactionsAlreadyAssigned = new HashSet<>();
			Set<String> accepted = new HashSet<>();


			Map<String, Set<String>> res = new HashMap<>();

			boolean save = false;

			//			boolean in = true;
			//			boolean both = false;

			//			for(String tc : reactionsByTcNumberForAnnotation.keySet())
			//				System.out.println("new" + tc + "\t" + reactionsByTcNumberForAnnotation.get(tc));

			if(resultsByEvalue.containsKey(queryAccession)) {

				String tcFamily = genesContainers.get(queryAccession).getAnnotatedFamily();

				for(String tcNumber : resultsByEvalue.get(queryAccession)) {

					if(reactionsByTcNumberForAnnotation.containsKey(tcNumber) && tcNumber.contains(tcFamily)){

						Set<String> reactions = reactionsByTcNumberForAnnotation.get(tcNumber);

						//					if(!reactions.isEmpty())
						//						System.out.println(reactions);

						//					System.out.println("dddd " + reactions);

						Iterator<String> iterator = reactions.iterator();

						while (iterator.hasNext()) {

							String id = iterator.next();

							if(!reactionsAlreadyAssigned.contains(id) || accepted.contains(id)) {

								reactionsAlreadyAssigned.add(id);
								accepted.add(id);

								if(!res.containsKey(tcNumber))
									res.put(tcNumber, new HashSet<String>());

								Set<String> set = res.get(tcNumber);
								set.add(id);

								res.put(tcNumber, set);

								save = true;

							}
						}
					}

				}

				if(res.size() > 0) {

					reportByEvalueAux.put(queryAccession, new HashMap<>(res));

				}
			}
			if(similaritiesResults.containsKey(queryAccession) && !similaritiesResults.get(queryAccession).isEmpty()) {

				Set<String> toAdd = new HashSet<>();

				Set<String> reactions = similaritiesResults.get(queryAccession);

				Iterator<String> iterator = reactions.iterator();

				while (iterator.hasNext()) {

					String id = iterator.next();

					if(!reactionsAlreadyAssigned.contains(id)) {

						reactionsAlreadyAssigned.add(id);

						toAdd.add(id);

						save = true;
					}
				}

				if(!toAdd.isEmpty()) 
					res.put(NO_TCNUMBER_ASSOCIATED, toAdd);
			}

			//			if(queryAccession.equals("NP_415185.1")) {
			//				System.out.println("AQUI!!!!!!! ");
			//
			//				for(String key : res.keySet())
			//					System.out.println(key + "\t" + res.get(key));
			//			}

			//			if(!save && !similaritiesResults.get(queryAccession).isEmpty())
			//				res.put("No TC family", similaritiesResults.get(queryAccession));

			String name = queryAccession.split("\\s+")[0];


			//			if(locus.containsKey(name))
			//				name = locus.get(name);


			if(save)
				finalResults.put(name, res);
		}

		//		System.exit(0);
	}

	/**
	 * Merge the results of both methods
	 * 
	 * @param similaritiesResults
	 * @param genesContainers 
	 */
	private void generateFinalResultsAuxValidation_filter_reactions(Map<String, Set<String>> similaritiesResults, 
			Map<String, GeneContainer> genesContainers) {

		//		Map<String, String> similaritiesReport = new HashMap<>();

		Set<String> compounds = new HashSet<>();

		finalResults = new HashMap<>();

		if(this.properties.isIgnoreMethod1())
			resultsByEvalue = new HashMap<String, Set<String>>();

		for(String queryAccession : blastResults.keySet()) {

//			if(queryAccession.equals("b4321"))
//				System.out.println();

			Set<String> reactionsAlreadyAssigned = new HashSet<>();
			Set<String> accepted = new HashSet<>();

			Map<String, Set<String>> res = new HashMap<>();

			boolean save = false;

			Map<String, TypeOfTransporter> transportTypes = new HashMap<>();

			if(genesContainers.get(queryAccession).getAnnotatedFamily() != null) {

				TypeOfTransporter transportType = null;

				if(resultsByEvalue.containsKey(queryAccession)) {

					String tcFamily = genesContainers.get(queryAccession).getAnnotatedFamily();

					for(String tcNumber : resultsByEvalue.get(queryAccession)) {

						if(reactionsByTcNumberForAnnotation.containsKey(tcNumber) && tcNumber.contains(tcFamily)){

							Set<String> reactions = reactionsByTcNumberForAnnotation.get(tcNumber);

							Iterator<String> iterator = reactions.iterator();

							while (iterator.hasNext()) {

								String id = iterator.next();

								if(!reactionsAlreadyAssigned.contains(id) || accepted.contains(id)) {

									reactionsAlreadyAssigned.add(id);
									accepted.add(id);

									if(!res.containsKey(tcNumber))
										res.put(tcNumber, new HashSet<String>());

									Set<String> set = res.get(tcNumber);
									set.add(id);

									res.put(tcNumber, set);

									compounds.addAll(this.reactionContainersByID.get(id).getMetabolites());

									save = true;

									if(!transportTypes.containsKey(tcNumber)) {
										transportTypes.put(tcNumber, this.reactionContainersByID.get(id).getTransportType());
									}
								}
							}
						}
					}

					for(String tcNumber : genesContainers.get(queryAccession).getClosestTCnumbers()) { //this list is sorted by similarity

						transportType = transportTypes.get(tcNumber);

						if(transportType != null)
							break;
					}
					if(res.size() > 0) {

						String pattern = ".*T.[" + TypeOfTransporter.getTransportTypeID(TypeOfTransporter.BiochemicalATP).toString() + "-9].*"; 

						if(!reactionsAlreadyAssigned.toString().matches(pattern)) {

							String regex = ".*T." + TypeOfTransporter.getTransportTypeID(transportType).toString() + ".*";

							for(String tc : new HashMap<>(res).keySet()) {

								for(String reaction : new HashSet<>(res.get(tc))) {
									if(!reaction.matches(regex))
										res.get(tc).remove(reaction);
								}

								if(res.get(tc).isEmpty())
									res.remove(tc);
							}
						}
						reportByEvalueAux.put(queryAccession, new HashMap<>(res));

					}
				}
				if(similaritiesResults.containsKey(queryAccession) && !similaritiesResults.get(queryAccession).isEmpty()) {

					Set<String> toAdd = new HashSet<>();

					Set<String> reactions = similaritiesResults.get(queryAccession); 	//this map is sorted by similarity

					Iterator<String> iterator = reactions.iterator();

					while (iterator.hasNext()) {

						String id = iterator.next();

						if(this.reactionContainersByID.containsKey(id) && !reactionsAlreadyAssigned.contains(id) && 
								(transportType == null || this.reactionContainersByID.get(id).getTransportType().equals(transportType))) {

								for(String compound : this.reactionContainersByID.get(id).getMetabolites()) {

									if(!compounds.contains(compound)) {	//complete the results of the first method and avoid creating bad GPRs

										reactionsAlreadyAssigned.add(id);

										toAdd.add(id);

										save = true;

										break;
									}
								}
						}
					}

					//					if(toAdd.size() > 0)
					//						similaritiesReport.put(queryAccession.split("\\s+")[0], toAdd.toString());

					if(!toAdd.isEmpty()) 
						res.put(NO_TCNUMBER_ASSOCIATED, toAdd);
				}

				//			if(queryAccession.equals("NP_415185.1")) {
				//				System.out.println("AQUI!!!!!!! ");
				//
				//				for(String key : res.keySet())
				//					System.out.println(key + "\t" + res.get(key));
				//			}

				//			if(!save && !similaritiesResults.get(queryAccession).isEmpty())
				//				res.put("No TC family", similaritiesResults.get(queryAccession));
			}

			//			if(locus.containsKey(name))
			//				name = locus.get(name);

			if(save) {
				String name = queryAccession.split("\\s+")[0];
				finalResults.put(name, res);
			}
		}
	}

	/**
	 * Merge the results of both methods
	 * 
	 * @param similaritiesResults
	 * @param genesContainers 
	 */
	private void generateFinalResultsAuxValidation2(Map<String, Set<String>> similaritiesResults, 		//this one was used before
			Map<String, GeneContainer> genesContainers) {

		//		Map<String, Map<String, Set<String>>> finalResultsAux = new HashMap<>();

		finalResults = new HashMap<>();

		//				Map<String, String> locus = 
		//						FilesUtils.readMapFromFile(path.concat("Acc_to_locus.txt"));

		for(String queryAccession : resultsByEvalue.keySet()) {

			Set<String> reactionsAlreadyAssigned = new HashSet<>();
			Set<String> accepted = new HashSet<>();

			String tcFamily = genesContainers.get(queryAccession).getAnnotatedFamily();

			Map<String, Set<String>> res = new HashMap<>();

			boolean save = false;

			boolean in = true;
			boolean both = false;

			//			for(String tc : reactionsByTcNumberForAnnotation.keySet())
			//				System.out.println("new" + tc + "\t" + reactionsByTcNumberForAnnotation.get(tc));

			for(String tcNumber : resultsByEvalue.get(queryAccession)) {

				//				System.out.println(tcNumber + "\t" + reactionsByTcNumberForAnnotation.get(tcNumber));

				if(reactionsByTcNumberForAnnotation.containsKey(tcNumber) && tcNumber.contains(tcFamily)){

					Set<String> reactions = reactionsByTcNumberForAnnotation.get(tcNumber);

					//					System.out.println("dddd " + reactions);

					Iterator<String> iterator = reactions.iterator();

					while (iterator.hasNext()) {

						String id = iterator.next().replaceAll("^i+", "i");			//resolver problema do 'ii'
						//						
						//						if(id.equals("TRUni__cpd00018o"))
						//							System.out.println("1 - est� aqui!!!!!!");

						String idAux = id.replaceAll("(i_)*(o_)*(i$)*(o$)*", "");

						if(!reactionsAlreadyAssigned.contains(idAux) || accepted.contains(id)) {

							if(reactionsAlreadyAssigned.isEmpty()) {

								if(id.matches(".+i.*") && id.matches(".+o.*"))
									both = true;

								if(id.matches(".+o.*"))
									in = false;
							}

							if(both || (id.matches(".+o.*") && !in) || (id.matches(".+i.*") && in)) {

								reactionsAlreadyAssigned.add(idAux);
								accepted.add(id);

								if(!res.containsKey(tcNumber))
									res.put(tcNumber, new HashSet<String>());

								Set<String> set = res.get(tcNumber);
								set.add(id);

								res.put(tcNumber, set);

								save = true;

							}
						}
					}
				}

			}

			if(res.size() > 0) {

				reportByEvalueAux.put(queryAccession, new HashMap<>(res));

			}

			if(similaritiesResults.containsKey(queryAccession) && !similaritiesResults.get(queryAccession).isEmpty()) {

				Set<String> toAdd = new HashSet<>();

				Set<String> reactions = similaritiesResults.get(queryAccession);

				Iterator<String> iterator = reactions.iterator();

				while (iterator.hasNext()) {

					String id = iterator.next().replaceAll("^i+", "i");;	//resolver problema do 'ii'

					//					if(id.equals("TRUni__cpd00018o"))
					//						System.out.println("2 - afinal est� aqui!!!!!!");

					String idAux = id.replaceAll("(i_)*(o_)*(i$)*(o$)*", "");

					if(!reactionsAlreadyAssigned.contains(idAux)) {

						if(both || (id.matches(".+o.*") && !in) || (id.matches(".+i.*") && in)) {

							reactionsAlreadyAssigned.add(idAux);

							toAdd.add(id);

							save = true;
						}
					}
				}

				if(!toAdd.isEmpty()) 
					res.put(NO_TCNUMBER_ASSOCIATED, toAdd);
			}

			//			if(queryAccession.equals("NP_415185.1")) {
			//				System.out.println("AQUI!!!!!!! ");
			//
			//				for(String key : res.keySet())
			//					System.out.println(key + "\t" + res.get(key));
			//			}

			//			if(!save && !similaritiesResults.get(queryAccession).isEmpty())
			//				res.put("No TC family", similaritiesResults.get(queryAccession));

			String name = queryAccession.split("\\s+")[0];


			//			if(locus.containsKey(name))
			//				name = locus.get(name);


			if(save)
				finalResults.put(name, res);
		}

		//		System.exit(0);
	}

	private void constructSBMLKBase(Container containerTransyt, String path) throws Exception {

		JSBMLReader reader = null;

		//		JSBMLReader reader = new JSBMLReader(modelPath, "ecoli");

		Container containerSBML = new Container(reader);

		Set<String> transporters = containerSBML.getReactionsByType(ReactionTypeEnum.Transport);

		System.out.println("TRANSPORTADORES TranSyT!!  >>> " + containerTransyt.getReactions().size());
		System.out.println("TRANSPORTADORES SBML!!  >>> " + transporters.size());

		for(String react : transporters) 
			containerSBML.removeReaction(react);

		for(String react : containerSBML.getDrains()) {

			if(!react.contains("_c0"))
				containerSBML.removeReaction(react);
		}


		transporters = containerSBML.getReactionsByType(ReactionTypeEnum.Transport);

		System.out.println("TRANSPORTADORES SBML!!  >>> " + transporters.size());

		Map<String, ReactionCI> transytReactions = containerTransyt.getReactions();


		//		Map<String, Set<String>> allres = new HashMap<>();

		for(String react : transytReactions.keySet()) {

			//			System.out.println(react);


			//			for(String key : list) {
			//
			//				if(react.contains(key)) {
			//
			//					if(!allres.containsKey(key))
			//						allres.put(key, new HashSet<>());
			//
			//					Set<String> set = allres.get(key);
			//					set.add(react);
			//
			//					allres.put(key, set);
			//				}
			//			}

			try {

				Map<String, MetaboliteCI> sbmlMetabolites = containerSBML.getMetabolites();
				sbmlMetabolites.putAll(containerTransyt.getReactionMetabolite(react));
				containerSBML.setMetabolites(sbmlMetabolites);

				containerSBML.addReaction(transytReactions.get(react));
			}
			catch (Exception e) {

				//				e.printStackTrace();

			}
		}

		String fileName = "sbmlKBaseNew2.xml";
		//
		TransytSBMLLevel3Writer sbml = new TransytSBMLLevel3Writer(path.concat("SBML\\").concat(fileName), containerSBML, taxonomyID, false);
		//
		sbml.writeToFile();

		//		System.out.println("#########################");
		//		for(String key : allres.keySet()) {
		//			System.out.println(key + "\t" + allres.get(key).toString().replaceAll("[\\[\\]]", ""));
		//		}
		//		System.out.println("#########################");


	}

	/**
	 * @param containerTriage
	 * @param reactionsIDS
	 */
	private void validation(Container containerTriage, String path) {

		try {
			Set<String> duplicate = new HashSet<>();
			Map<String, String> missingReactions = new HashMap<>();
			Map<String, String> missingReactionsWithNames = new HashMap<>();

			Set<String> missingMetabolites = new HashSet<>();
			Set<String> missingMetabolitesNames = new HashSet<>();

			System.out.println("Merge Containers...");

			JSBMLLevel3Reader reader = new JSBMLLevel3Reader(modelPath, "ecoli");

			Container containerSBML = new Container(reader);
			containerSBML.verifyDepBetweenClass();

			System.out.println("4");

			//			Set<String> smblReactions = containerSBML.getReactionsByType(ReactionTypeEnum.Transport);
			//			
			//			for(String r : smblReactions)
			//				System.out.println(r);

			//			Container subContainer = ContainerUtils.subContainer(containerSBML, containerSBML.getReactionsByType(ReactionTypeEnum.Transport));


			//			
			//			Map<String, ReactionCI> triageReactions = containerTriage.getReactions();
			//			for(String react : triageReactions.keySet())
			//				subContainer.addReaction(triageReactions.get(react));

			Map<String, ReactionCI> sbmlReactions = containerSBML.getReactions();

			Set<String> transporters = containerSBML.getReactionsByType(ReactionTypeEnum.Transport);

			System.out.println("TRANSPORTADORES!!  >>> " + transporters.size());

			//			containerSBML.getReaction("").hasSameStoichiometry(r, rev_in_account, ignoreCompartments)	//comparison

			System.exit(0);

			Map<String, MetaboliteCI> metabolites = containerTriage.getMetabolites();

			System.out.println("Validating...");

			Map<String, Map<String, String>> compounds = ModelSEEDRealatedOperations.readFile();

			Map<String, String> sbmlGeneRules = new HashMap<>();

			Map<String, ReactionConstraintCI> defaultEC = containerSBML.getDefaultEC();

			for(String react : sbmlReactions.keySet()) {

				//				System.out.println(react);

				//				if(react.equals("R_rxn05594_p")) {
				//					System.out.println("existe");
				//					
				//				}

				sbmlGeneRules.put(react, sbmlReactions.get(react).getGeneRuleString());

				if(transporters.contains(react) && !react.equals("R_BIOMASS_Ec_iAF1260_core_59p81M")) {

					Set<String> metabolitesSBML = sbmlReactions.get(react).getMetaboliteSetIds();

					try {
						if(metabolites.keySet().containsAll(metabolitesSBML)) {

							//							if(react.equals("R_rxn05594_p")) {
							//								System.out.println("entrou aqui");
							//								
							//							}

							boolean rev = true;

							if(defaultEC.get(react).getLowerLimit() == 0.0 || defaultEC.get(react).getUpperLimit() == 0.0)
								rev = false;

							sbmlReactions.get(react).setReversible(rev);

							containerTriage.addReaction(sbmlReactions.get(react));

							String reactionByName = sbmlReactions.get(react).toStringStoiquiometry();

							for(String id : sbmlReactions.get(react).getMetaboliteSetIds()) {

								String auxID = id.substring(2, id.length()-2);

								if(compounds.containsKey(auxID) && !compounds.get(auxID).get("name").isEmpty()) {
									reactionByName = reactionByName.replace(id, compounds.get(auxID).get("name"));
								}
							}

							missingReactions.put(react, sbmlReactions.get(react).toStringStoiquiometry());
							missingReactionsWithNames.put(react, reactionByName);
						}
						else {

							//							if(react.equals("R_rxn05594_p")) {
							//								System.out.println("entrou aqui2");
							//
							//							}
							//
							for(String metabolite : metabolitesSBML) {

								if(!metabolites.containsKey(metabolite)) {
									missingMetabolites.add(metabolite);

									String auxID = metabolite.substring(2, metabolite.length()-2);

									if(compounds.containsKey(auxID) && !compounds.get(auxID).get("name").isEmpty()) {
										missingMetabolites.add(compounds.get(auxID).get("name"));
									}


								}
							}

							String reactionByName = sbmlReactions.get(react).toStringStoiquiometry();

							for(String id : sbmlReactions.get(react).getMetaboliteSetIds()) {

								String auxID = id.substring(2, id.length()-2);

								if(compounds.containsKey(auxID) && !compounds.get(auxID).get("name").isEmpty()) {
									reactionByName = reactionByName.replace(id, compounds.get(auxID).get("name"));
								}
							}

							missingReactions.put(react, sbmlReactions.get(react).toStringStoiquiometry());
							missingReactionsWithNames.put(react, reactionByName);
						}
					} 
					catch (ReactionAlreadyExistsException e) {

						if(react.equals("R_rxn05594_p")) {
							System.out.println("DUPLICATE");

						}
						System.out.println(react);
						duplicate.add(react);
					}
				}
			}

			String format = "_e_p.txt";

			FilesUtils.saveWordsInFile(path + "duplicates" + format, duplicate);

			FilesUtils.saveMapInFile(path + "sbmlGeneRules.txt", sbmlGeneRules);

			FilesUtils.saveWordsInFile(path + "missingMetabolites" + format, missingMetabolites);
			FilesUtils.saveWordsInFile(path + "missingMetabolitesNames" + format, missingMetabolitesNames);

			FilesUtils.saveMapInFile(path + "missingReactions" + format, missingReactions);
			FilesUtils.saveMapInFile(path + "missingReactionsWithNames" + format, missingReactionsWithNames);

			//			subContainer.verifyDepBetweenClass();

			//			System.out.println();
			//			
			//			for(String r : subContainer.getReactionsByType(ReactionTypeEnum.Transport)) {
			//				System.out.println(r);
			//			}

			//			for(String r : containerTriage.getReactionsByType(ReactionTypeEnum.Transport)) {
			//				System.out.println(r);
			//			}
			//			
			//			System.out.println(containerTriage.getReactionsByType(ReactionTypeEnum.Transport).size());

			//			FilesUtils.saveMapInFile(path + "reactionsGenerated.txt", 
			//					reactionsIDS);
			//
			//			Map<String, String> reactionsIDSWithNames = new HashMap<>();
			//
			//			for(String r : reactionsIDS.keySet()) {
			//
			//				reactionsIDSWithNames.put(r, reactionContainersByID.get(r).getReactionWithIDs());
			//			}
			//
			//			FilesUtils.saveMapInFile(path + "reactionsGeneratedWithNames.txt", 
			//					reactionsIDSWithNames);


			Map<String, Set<String>> geneRules = new HashMap<>();

			for(String acc : finalResults.keySet()) {

				for(String tc : finalResults.get(acc).keySet()) {

					for(String r : finalResults.get(acc).get(tc)) {

						if(geneRules.containsKey(r)) {
							Set<String> set = geneRules.get(r);
							set.add(acc);

							geneRules.put(r, set);
						}
						else {
							Set<String> set = new HashSet<>();
							set.add(acc);

							geneRules.put(r, set);
						}
					}
				}
			}

			FilesUtils.saveMapInFile3(path + "geneRules.txt", geneRules);

			generatedReactionsByTCNumber(path);

		} 
		catch (Exception e) {
			e.printStackTrace();
		} 
	}

	private Map<String, Set<String>> getReactionsByTcNumber(Set<String> identifyTcNumbersForSearch) throws Exception {

		//		reactionContainersByID = new HashMap<>();
		mainReactions = new HashMap<>();

		Map<String, Set<String>> containers = new HashMap<>();

		reactionsByTcNumberForAnnotation = new HashMap<>();

		//		System.out.println(identifyTcNumbersForSearch.contains("2.A.63.1.4"));


		logger.trace("Searching all tcNumber nodes...");

		Map<String, TransytNode> tcNodes = this.service.getNodesByLabel(TransytNodeLabel.TC_Number, TransytGeneralProperties.TC_Number);

		logger.trace("Searching all relationships TCnumber has Reaction...");

		Map<String, Set<TransytRelationship>> tcHasReaction = this.service.getAllTCNumberHasReactionRelationships();

		logger.trace("Searching all relationships Reaction has reactant...");

		Map<String, Set<TransytRelationship>> reactionHasReactant = this.service.getAllReactionHasMetaboliteRelationships(defaultRelationshipReactants);

		logger.trace("Searching all relationships Reaction has product...");

		Map<String, Set<TransytRelationship>> reactionHasProduct = this.service.getAllReactionHasMetaboliteRelationships(defaultRelationshipProducts);

		logger.trace("Searching transport types of each reaction...");

		Map<String, TypeOfTransporter> transportTypes = this.service.getAllReactionHasTransportType();

		logger.trace("Searching compounds molecular fomulas...");

		this.service.searchFormulas(defaultMetaboliteLabel);		// this step is important to store information in cache

		logger.trace("Relationships search complete.");

		for(String tcNumber : identifyTcNumbersForSearch) {

			try {

				if(!tcNumbersNotPresentInTransytDatabase.contains(tcNumber)) {

					//					System.out.println(tcNumber);
					//					System.out.println(tcNumberNode == null);

					if(tcNodes.containsKey(tcNumber)) {

						TransytNode tcNumberNode = tcNodes.get(tcNumber);

						Map<String, Boolean> reversibility = new HashMap<>();

						if(!mainReactions.containsKey(tcNumber) && tcNumberNode.hasProperty(TransytGeneralProperties.MainReactionsIDs)) {

							String ids  = (String) tcNumberNode.getProperty(TransytGeneralProperties.MainReactionsIDs);
							mainReactions.put(tcNumber, Arrays.asList(ids.split(", ")));
						}

						Set<TransytNode> reactionNodes = new HashSet<>();
						Set<String> ids = new HashSet<>();
						Set<String> confidenceLeves = new HashSet<>();

						if(tcHasReaction.containsKey(tcNumber)) {

							Set<TransytRelationship> relations = tcHasReaction.get(tcNumber);

							for(TransytRelationship rel : relations) {

								if(properties.isOverrideCommonOntologyFilter()  || Boolean.valueOf(rel.getProperty(TransytGeneralProperties.CommonOntology))) {

									TransytNode node = rel.getOtherEndNode();

									boolean rev = Boolean.valueOf(rel.getProperty(TransytGeneralProperties.Reversible));

									if(tcNumber.startsWith("9.")) //just to not waste time, change in the future as needed
										confidenceLeves.add(rel.getProperty(TransytGeneralProperties.Confidence_Level));

									String reactionID = node.getProperty(TransytGeneralProperties.ReactionID);

									reversibility.put(reactionID, rev);

									ids.add(reactionID);
									reactionNodes.add(node);
								}
							}
						}
						containers.put(tcNumber, ids);

						//						if(tcNumber.equals("2.A.86.1.4"))
						//							System.out.println();

						Set<String> reactionsToSave = null;
						boolean search = false;

						if(tcNumber.startsWith("9.")) {

							if(properties.isAcceptUnknownFamily()) {
								search = true;
							}
							else {
								for(String confidence : confidenceLeves) {	//cycle because not sure if there are several sud-levels inside this level
									if(confidence.startsWith(GenerateTransportReactions.METACYC_CONFIDENCE_LEVEL)) {
										search = true;
										break;
									}
								}
							}
						}
						else {
							search = true;
						}

						if(search)
							reactionsToSave = getAllNodesReactionsIDsForAnnotation(reactionNodes, reversibility,
									reactionHasReactant, reactionHasProduct, transportTypes);

						if(reactionsToSave != null)
							reactionsByTcNumberForAnnotation.put(tcNumber, reactionsToSave);
					}
					else {
						if(!tcNumbersNotPresentInTransytDatabase.contains(tcNumber)) {

							tcNumbersNotPresentInTransytDatabase.add(tcNumber);

							if(!tcNumber.startsWith("8."))
								logger.warn("No results found in TranSyT neo4j database for TCnumber {}! Please consider update the database.", tcNumber);
						}
					}
				}

			} 
			catch (Exception e) {

				logger.error("An error occurred while retrieving the reactions for TCnumber: ", tcNumber);

				e.printStackTrace();
			}

		}

		//		for(String tc : reactionsByTcNumberForAnnotation.keySet())
		//			System.out.println(tc + "\t" + reactionsByTcNumberForAnnotation.get(tc));

		//		System.out.println(tcNumbersNotPresentInTRIAGEdatabase.contains("2.A.63.1.4"));
		//		System.out.println(containers.containsKey("2.A.63.1.4"));

		return containers;
	}

	/**
	 * Method to decide which TcNumbers should be searched in order to retrieve the respective reactions. 
	 * Blast entries without results at this stage are also identified to process them later. 
	 * 
	 * @return
	 */
	private Set<String> identifyTcNumbersForSearch() {

		Set<String> toSearchTcNumber = new HashSet<>();

		hitsWithoutReactions = new HashSet<>();

		for(String query : resultsByEvalue.keySet()) {

			if(resultsByEvalue.get(query).isEmpty()) {

				hitsWithoutReactions.add(query);

				for(AlignmentCapsule capsule : blastResults.get(query)) {

					toSearchTcNumber.add(capsule.getTcdbID());
				}
			}
			else {

				toSearchTcNumber.addAll(resultsByEvalue.get(query));
			}
		}

		return toSearchTcNumber;
	}

	/**
	 * Build containers and count common taxa.
	 * 
	 * @param organism
	 * @param taxonomy
	 * @param tcdbGenes
	 * @param homologousGenes
	 * @return 
	 * @throws Exception 
	 */
	private Map<String, GeneContainer> buildGenesContainers() throws Exception {

		Map<String, GeneContainer> genes = new HashMap<>();

		Set<String> tcFamilies = new HashSet<>();

		Map<String, TransytNode> allAccessions = service.getNodesByLabel(TransytNodeLabel.Uniprot_Accession,
				TransytGeneralProperties.Accession_Number);

		for(String queryAccession : blastResults.keySet()) {

			//			if(queryAccession.equals("b2416"))
			//				System.out.println();

			Set<String> homologousGenes = new HashSet<>();
			Map<String, Double> similarities = new HashMap<>();
			Map<String, Integer> counts = new HashMap<>();
			//			Map<String, TypeOfTransporter> transportType = new HashMap<>();
			//				Map<String, String> tcNumbers = new HashMap<>();

			Map<String, Double> familiesSimilarity = new HashMap<>();
			Map<String, Integer> familiesFrequency = new HashMap<>();

			Double similaritySum = 0.0;
			int totalEntries = blastResults.size();

			String sequence = null;

			boolean first = true;
			boolean auxiliaryGene = false;

			List<String> closestTcs = new ArrayList<>();

			for(AlignmentCapsule capsule : blastResults.get(queryAccession)) {

				String homologue = capsule.getTarget();

				String[] tcAux = capsule.getTcdbID().split("\\.");

				String tc = tcAux[0].concat(".").concat(tcAux[1]).concat(".").concat(tcAux[2]).concat(".");

				if(first && (tc.startsWith(Hpr_FAMILY) || tc.startsWith(Phosphotransferase_FAMILY))) {
					auxiliaryGene = true;
					break;
				}
				else {
					first = false;
				}

				if(!tc.startsWith(Hpr_FAMILY) && !tc.startsWith(Phosphotransferase_FAMILY)) {

					closestTcs.add(capsule.getTcdbID());

					homologousGenes.add(homologue);

					//			for(String homologue : homologousGenes.get(queryAccession)) {

					Integer count = 0;

					//					tcNumbers.put(homologue, pair.getKey());
					similarities.put(homologue, capsule.getBitScore());

					if(!taxonomies.containsKey(homologue) && allAccessions.containsKey(homologue)) {
						TransytNode node = allAccessions.get(homologue);

						String[] tax = node.getProperty(TransytGeneralProperties.Taxonomy).replaceAll("\\[", "").replaceAll("\\]", "").split(", ");

						taxonomies.put(homologue, tax);

						organisms.put(homologue, node.getProperty(TransytGeneralProperties.Organism));

					}

					if(taxonomies.containsKey(homologue)) {

						String[] hTaxonomy = taxonomies.get(homologue);

						for(int i = 0; i < taxonomy.length; i++) {

							if(i < hTaxonomy.length && taxonomy[i].trim().equalsIgnoreCase(hTaxonomy[i]))
								count++;
							else
								break;
						}

						if(organism.equals(organisms.get(homologue)))
							count++;
					}
					else { 

						if(!capsule.getTcdbID().contains("-.-"))
							logger.warn("Taxonomy missing for homologue {}", homologue);
					}
					counts.put(homologue, count);

					if(familiesSimilarity.containsKey(tc)) {
						familiesSimilarity.put(tc, familiesSimilarity.get(tc) + capsule.getBitScore());

						familiesFrequency.put(tc, familiesFrequency.get(tc) + 1);
					}
					else {
						familiesSimilarity.put(tc, capsule.getBitScore());

						familiesFrequency.put(tc, 1);
					}

					similaritySum = similaritySum + capsule.getBitScore();
				}
			}

			String tcFamily = null;

			if(!auxiliaryGene) {
				tcFamily = ReactionsPredictor.annotateTcFamily(totalEntries, similaritySum, familiesFrequency, 
						familiesSimilarity, properties);

				tcFamilies.add(tcFamily);
			}

			genes.put(queryAccession, new GeneContainer(sequence, homologousGenes, counts, similarities, 
					taxonomy.length + 1, tcFamily, closestTcs));
		}

		logger.info("FAMILIES: " + tcFamilies.size());

		return genes;
	}

	/**
	 * Method to retrive all the reactions regarding a given tc number and construct all the respective reaction containers.
	 * 
	 * @param tcNumber
	 * @param reactionNodes
	 * @param reversibility 
	 * @param irreversibleIDs 
	 * @return
	 */
	private Set<String> getAllNodesReactionsIDsForAnnotation(Set<TransytNode> reactionNodes, Map<String, Boolean> reversibility,
			Map<String, Set<TransytRelationship>> reactionHasReactant, Map<String, Set<TransytRelationship>> reactionHasProduct,
			Map<String, TypeOfTransporter> transportTypes){

		Set<String> set = new HashSet<>();

		try {
			for(TransytNode node : reactionNodes) {

				String reactionID = node.getProperty(TransytGeneralProperties.ReactionID);

				boolean go = reactionHasReactant.containsKey(reactionID);

				if(reactionID.matches("T[ORXYZ]" + TypeOfTransporter.getTransportTypeID(TypeOfTransporter.Uniport) + ".*00067$"))
					go = false; //ignore proton uniport importers

				if(!reactionsToIgnore.contains(reactionID) && go) {

					String reaction = service.getReactionEquation(reactionID, reversibility.get(reactionID), reactionHasReactant.get(reactionID), reactionHasProduct.get(reactionID));

					metabolitesNames.putAll(service.getTemporaryCompoundsNames());
					metabolitesFormulas.putAll(service.getTemporaryCompoundsFormulas());

					Set<String> compounds = service.getTemporaryReactionCompounds();
					
					if((modelMetabolites == null || modelMetabolites.containsAll(compounds)) && reaction != null) {  //null in case no sbml was inserted (accept all)

						set.add(reactionID);

						if(!reactionContainersByID.containsKey(reactionID)) {

							String reactionWithNames = node.getProperty(TransytGeneralProperties.Reaction);

							ReactionContainer container = new ReactionContainer(reaction, reversibility.get(reactionID));

							container.setTransportType(transportTypes.get(reactionID));
							container.setReactionWithIDs(reactionWithNames);
							
							if(node.hasProperty(TransytGeneralProperties.ModelSEED_Reaction_ids)) {
								
								String[] m_ids = node.getProperty(TransytGeneralProperties.ModelSEED_Reaction_ids).split(";\\s+");
								container.setModelseedReactionIdentifier(m_ids);
							}

							reactionContainersByID.put(reactionID, container);
						}
					}
					else {
						reactionsToIgnore.add(reactionID);
					}

				}
				else
					reactionsToIgnore.add(reactionID);
			}
		} 
		catch (Exception e) {

			e.printStackTrace();
		}

		return set;
	}

	private void generatedReactionsByTCNumber(String path) {

		Map<String, Set<String>> newData = new HashMap<>();

		for(String accession : finalResults.keySet()) {

			for(String tcNumber : finalResults.get(accession).keySet()) {

				for(String reactionID : finalResults.get(accession).get(tcNumber)) {

					Set<String> tcNumbers = new HashSet<>();

					if(newData.containsKey(reactionID))
						tcNumbers = newData.get(reactionID);

					tcNumbers.add(tcNumber);

					newData.put(reactionID, tcNumbers);
				}
			}
		}

		List<String[]> table = new ArrayList<>();

		for(String reactionID : newData.keySet()) {

			String[] line = new String[3];

			line[0] = reactionID;
			line[1] = reactionContainersByID.get(reactionID).getReactionWithIDs();
			line[2] = newData.get(reactionID).toString().replaceAll("\\[", "").replaceAll("\\]", "");

			table.add(line);
		}

		WriteExcel.tableToExcel(table, path + "reactionsByTCNumber.xlsx");

	}

	/**
	 * 
	 * @return
	 */
	private void setDefaultRelationshipsToSearch() {

		MetaboliteReferenceDatabaseEnum label = properties.getDefaultLabel();

		if(label.equals(MetaboliteReferenceDatabaseEnum.ModelSEED)) {
			defaultRelationshipReactants = TransytRelationshipType.has_ModelSEED_reactant;
			defaultRelationshipProducts = TransytRelationshipType.has_ModelSEED_product;
			defaultMetaboliteLabel = TransytNodeLabel.ModelSEED_Metabolite;
		}

		else if(label.equals(MetaboliteReferenceDatabaseEnum.KEGG)) {
			defaultRelationshipReactants = TransytRelationshipType.has_KEGG_reactant;
			defaultRelationshipProducts = TransytRelationshipType.has_KEGG_product;
			defaultMetaboliteLabel = TransytNodeLabel.KEGG_Metabolite;
		}

		else if(label.equals(MetaboliteReferenceDatabaseEnum.BiGG)) {
			defaultRelationshipReactants = TransytRelationshipType.has_BiGG_reactant;
			defaultRelationshipProducts = TransytRelationshipType.has_BiGG_product;
			defaultMetaboliteLabel = TransytNodeLabel.BiGG_Metabolite;
		}
		else {
			defaultRelationshipReactants = TransytRelationshipType.has_MetaCyc_reactant;
			defaultRelationshipProducts = TransytRelationshipType.has_MetaCyc_product;
			defaultMetaboliteLabel = TransytNodeLabel.MetaCyc_Metabolite;
		}

	}

	/**
	 * @return the results
	 */
	public Map<String, Set<String>> getResults() {
		return resultsByEvalue;
	}


}
