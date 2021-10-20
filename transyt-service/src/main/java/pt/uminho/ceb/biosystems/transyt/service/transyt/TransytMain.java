package pt.uminho.ceb.biosystems.transyt.service.transyt;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.uminho.ceb.biosystems.transyt.scraper.tcdb.tcdbTransportTypesRetriever.Retriever;
import pt.uminho.ceb.biosystems.transyt.service.internalDB.WriteByMetabolitesID;
import pt.uminho.ceb.biosystems.transyt.service.reactions.ProvideTransportReactionsToGenes;
import pt.uminho.ceb.biosystems.transyt.service.utilities.OrganismProperties;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.Organism;
import pt.uminho.ceb.biosystems.transyt.utilities.email.Email;
import pt.uminho.ceb.biosystems.transyt.utilities.enumerators.Enumerators.STAIN;
import pt.uminho.ceb.biosystems.transyt.utilities.files.FilesUtils;
import pt.uminho.ceb.biosystems.transyt.utilities.transytUtilities.Properties;
import validation_kbase.KbValidation;

/**
 * @author Davide
 *
 */
public class TransytMain {

	private static final Logger logger = LoggerFactory.getLogger(TransytMain.class);

	public static void main(String[] args) throws Exception {

		//		Integer taxID = Integer.valueOf(args[0]);

		logger.info("############################################ TranSyT - v0.0.13.02 - SNAPSHOT ############################################");

		String command = args[0];

		STAIN stain = STAIN.gram_negative; //optional ---> user selection
		
		String workFolderID = args[1].trim();
		
		logger.info("workfolder: " + workFolderID);

		Properties properties = null;

		String filePath = workFolderID.concat("protein.faa");

		String modelPath = workFolderID.concat("model.xml");

		String metabolitesPath = workFolderID.concat("metabolites.txt");

		String resultPath = "";

		try {

			if(args[1] != null || !args[1].isEmpty()) {
				resultPath = args[1];
			}
		}
		catch(ArrayIndexOutOfBoundsException e){

			resultPath = workFolderID;
		}
		
		Organism organism = null;

		switch (command) {
		case "1":

			boolean useCache = false;

			if(args.length > 1 && args[1] != null) {
				useCache = Boolean.valueOf(args[1]);
			}

			logger.info("Scraper and database service option selected!");

			Retriever.runRetriever(useCache, false, null);
			WriteByMetabolitesID.start(new Properties(), false);
			
			Email.sendNotificationEmail();

			break;

		case "2":  //ignore TCDB scraper

			logger.info("Database service option selected!");

			WriteByMetabolitesID.start(new Properties(), false);

			break;

		case "3": 

			logger.info("Transport reactions annotation option selected");

			properties = new Properties(workFolderID.concat("params.txt"));

			organism = new OrganismProperties(properties, stain).getOrganism();

			logger.debug("Using common ontology filter: " + !properties.isOverrideCommonOntologyFilter());

			new ProvideTransportReactionsToGenes(workFolderID, organism, filePath, modelPath,
					metabolitesPath, resultPath, properties, true);

//			if(args.length > 2) {
//				
//				logger.info("Building biolog results!");
//				
//				String[] pathSplit = workFolderID.split("/");
//				
//				String testId = pathSplit[pathSplit.length-1];
//				String rootPath = workFolderID.replace(testId, "");
//				
//				logger.info(workFolderID);
//				logger.info(testId);
//				logger.info(rootPath);
//
//				Map<String, String> growthCpds = KbValidation.read_growth_compounds(rootPath + "Biolog_Merge_Lit_Pam_Binary_Data_V1.0.xlsx", testId, rootPath);
//				Map<String, Set<String>> reactionsByCpd = KbValidation.findReactionsContaininigCompounds(growthCpds, resultPath + "results/transyt.xml");
//
//				Map<String, String> no_growthCpds = KbValidation.read_no_growth_compounds(rootPath + "Biolog_Merge_Lit_Pam_Binary_Data_V1.0.xlsx", testId, rootPath);
//				Map<String, Set<String>> reactionsNoGrowthByCpd = KbValidation.findReactionsContaininigCompounds(no_growthCpds, resultPath + "results/transyt.xml");
//				
//				FilesUtils.saveMapInFile3(resultPath + "results/biolog_compounds.txt", reactionsByCpd);
//				FilesUtils.saveMapInFile3(resultPath + "results/biolog_compounds_no_growth.txt", reactionsNoGrowthByCpd);
//			}
			break;
		
		case "4": 

			logger.info("Transporters proteins annotation option selected");

			properties = new Properties(workFolderID.concat("params.txt"));

			organism = new OrganismProperties(properties, stain).getOrganism();

			new ProvideTransportReactionsToGenes(workFolderID, organism, filePath, modelPath,
					metabolitesPath, resultPath, properties, false);

			break;

		default:
			break;
		}
	}
}
