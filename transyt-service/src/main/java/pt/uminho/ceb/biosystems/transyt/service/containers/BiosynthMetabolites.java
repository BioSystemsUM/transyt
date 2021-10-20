package pt.uminho.ceb.biosystems.transyt.service.containers;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.uminho.ceb.biosystems.transyt.service.reactions.IdentifyReactionsMetabolites;
import pt.uminho.ceb.biosystems.transyt.utilities.files.FilesUtils;
import pt.uminho.sysbio.biosynth.integration.io.dao.neo4j.MetaboliteMajorLabel;

public class BiosynthMetabolites {

	private Set<String> toDelete = FilesUtils.readWordsInFile(FilesUtils.getDictionatiesAndConfigurationsDirectory().concat("MetabolitesExceptions.txt"));
	private Map<String, Map<MetaboliteMajorLabel, String>> metabolitesIDs;
	private Map<String, String> namesLowerCaseWithoutSigns;
	private Map<String, String> namesWithoutSigns;
	private Map<String, String> namesLowerCase;
	
	private static final Logger logger = LoggerFactory.getLogger(IdentifyReactionsMetabolites.class);
	
	public BiosynthMetabolites(Map<String, Map<MetaboliteMajorLabel, String>> metabolitesIDs, Map<String, String> namesLowerCaseWithoutSigns, 
			Map<String, String> namesWithoutSigns, Map<String, String> namesLowerCase) {
		
		this.metabolitesIDs = metabolitesIDs;
		this.namesLowerCaseWithoutSigns = namesLowerCaseWithoutSigns;
		this.namesWithoutSigns = namesWithoutSigns;
		this.namesLowerCase = namesLowerCase;
		
		exceptions();
	}

	/**
	 * @return the metabolitesIDs
	 */
	public Map<String, Map<MetaboliteMajorLabel, String>> getMetabolitesIDs() {
		return metabolitesIDs;
	}
	
	/**
	 * @return the namesLowerCaseWithoutSigns
	 */
	public Map<String, String> getNamesLowerCaseWithoutSigns() {
		return namesLowerCaseWithoutSigns;
	}

	/**
	 * @return the namesWithoutSigns
	 */
	public Map<String, String> getNamesWithoutSigns() {
		return namesWithoutSigns;
	}

	/**
	 * @return the namesLowerCase
	 */
	public Map<String, String> getNamesLowerCase() {
		return namesLowerCase;
	}
	
	/**
	 * Method to delete metabolites considered exceptions.
	 */
	private void exceptions() {
		
		for(String key : toDelete) {
		
			if(metabolitesIDs.containsKey(key)) {
				metabolitesIDs.remove(key);
				namesLowerCaseWithoutSigns.remove(key);
				namesWithoutSigns.remove(key);
				namesLowerCase.remove(key);
				
				logger.debug("Metabolite ".concat(key).concat(" removed due to order from the configuration file."));
			}
		}
	}

}