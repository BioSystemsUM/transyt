package pt.uminho.ceb.biosystems.transyt.service.reactions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.uminho.ceb.biosystems.transyt.service.containers.BiosynthMetabolites;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.BiosynthMetaboliteProperties;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.ReactionContainer;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.TcNumberContainer2;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.TcdbMetabolitesContainer;
import pt.uminho.ceb.biosystems.transyt.utilities.dictionary.Synonyms;
import pt.uminho.sysbio.biosynth.integration.io.dao.neo4j.MetaboliteMajorLabel;
import pt.uminho.sysbio.biosynthframework.BiodbGraphDatabaseService;

public class IdentifyReactionsMetabolites {

	public static final String[] REGEX_STOICHIOMETRY =  {"^(\\s*n\\s*)", "^(\\d+\\/*\\d*\\s*)"};
	public static final String[] REGEX_NAMES =  {"ic acids*"};
	public static final String[] REPLACEMENT_NAME =  {"ate"};
	public static final String s = "s";

	private BiosynthMetabolites namesAndIDsContainer;
	private Map<String, Map<MetaboliteMajorLabel, String>> allMetabolitesByName;
	private Map<String, Map<String, MetaboliteMajorLabel>> tcdbMetabolitesIDs;
	private Map<String, Set<String>> metabolites;
	private Map<String, String[]> forChildsSearch;
	private Map<String, String> metabolitesByOriginalName = new HashMap<String, String>();
	private MetaboliteMajorLabel overrideDefauldLabel;

	private Synonyms dictionary;

	private static final Logger logger = LoggerFactory.getLogger(IdentifyReactionsMetabolites.class);

	public IdentifyReactionsMetabolites(Map<String, Set<TcNumberContainer2>> reactionsData, BiosynthMetabolites namesAndIDsContainer,
			BiodbGraphDatabaseService service) {

		this.dictionary = new Synonyms();

		metabolites = getMetabolitesFromReactions(reactionsData);

		for(String metabolite : metabolites.keySet()) {
			for(String originalMetabolite : metabolites.get(metabolite)) {
				this.metabolitesByOriginalName.put(originalMetabolite, metabolite);
			}
		}

		logger.info("Total metabolites for search: {}", metabolites.size());

		getMetabolitesIDs(namesAndIDsContainer, service);

		logger.info("Total found: {}", tcdbMetabolitesIDs.size());
		
//		System.out.println(tcdbMetabolitesIDs);
		
	}
	
	/**
	 * For use outside of TranSyT
	 * 
	 * @param reactionsData
	 * @param namesAndIDsContainer
	 * @param allData
	 * @param service
	 */
	public IdentifyReactionsMetabolites(Set<String> metabolitesList, BiosynthMetabolites namesAndIDsContainer,
			BiodbGraphDatabaseService service, MetaboliteMajorLabel overrideDefauldLabel) {

		this.overrideDefauldLabel = overrideDefauldLabel;
		
		this.dictionary = new Synonyms();

		metabolites = new HashMap<>();

		for(String metabolite : metabolitesList) {
			this.metabolites.put(metabolite, null);
		}

		logger.info("Total metabolites for search: {}", metabolites.size());

		getMetabolitesIDs(namesAndIDsContainer, service);

		logger.info("Total found: {}", tcdbMetabolitesIDs.size());

	}

	/**
	 * get ids for metabolites that are present in pt.uminho.ceb.biosystems.transyt.scraper.tcdb's pt.uminho.ceb.biosystems.transyt.service.reactions
	 * 
	 * @param metabolites
	 * @param allMetabolitesByName
	 * @return
	 */
	public Map<String, Map<String, MetaboliteMajorLabel>> getMetabolitesIDs(BiosynthMetabolites namesAndIDsContainer, BiodbGraphDatabaseService service) {

		tcdbMetabolitesIDs = new HashMap<>();
		this.forChildsSearch = new HashMap<>();

		//		Set<String> forSearch = new HashSet<>(metabolites);

		//		metabolites = new HashSet<>(standardizationOfNames1(metabolitesFromFile, dictionary));

		this.namesAndIDsContainer = standardizationOfNames2(namesAndIDsContainer);

		this.allMetabolitesByName = new HashMap<>(this.namesAndIDsContainer.getMetabolitesIDs());
		
//		System.out.println(this.allMetabolitesByName.containsKey("cpd15460"));
//				System.out.println("MET>>>>> " + allMetabolitesByName.get("Electron"));
		//
		//		System.out.println("metabolites >>>" + metabolites.size());		//2078
		//
		//		System.out.println("allmetabolites >>>" + namesAndIDsContainer.getMetabolitesIDs().size()); 	//154224

		boolean print = false;
		
		//		
		identificationByDirectMatch();

		if(print)
			System.out.println("FOUND1: " + tcdbMetabolitesIDs.size());		//911

		identificationDeletingStoichiometry();

		if(print)
			System.out.println("FOUND2: " + tcdbMetabolitesIDs.size());		//975

		identificationInLowerCase();

		if(print)
			System.out.println("FOUND3: " + tcdbMetabolitesIDs.size());		//1089

		if(print)
			identificationReplacingNonAlphanumeric();

		if(print)
			System.out.println("FOUND4: " + tcdbMetabolitesIDs.size());		//1095

		identificationIntroducingDandL();

		if(print)
			System.out.println("FOUND5: " + tcdbMetabolitesIDs.size());		//1103

		identificationReplacingNonAlphanumericAndInLowercase();

		if(print)
			System.out.println("FOUND6: " + tcdbMetabolitesIDs.size());		//1109

		//		System.out.println(metabolites);

		return tcdbMetabolitesIDs;

	}	

	/**
	 * @param metabolite
	 * @param ids
	 */
	private void saveMetabolite(String metabolite, Map<MetaboliteMajorLabel, String> ids) {

		MetaboliteMajorLabel id = selectMetaboliteMajorLabel(metabolite, ids);

		if(id != null) {

			Map<String, MetaboliteMajorLabel> map = new HashMap<>();

			if(id.equals(MetaboliteMajorLabel.EcoCyc))
				map.put(ids.get(id), MetaboliteMajorLabel.MetaCyc);
			else
				map.put(ids.get(id), id);
			
//			if(metabolite.equalsIgnoreCase("quinol") || metabolite.equalsIgnoreCase("quinone"))
//				System.out.println();
			
//			if(allData.containsKey(ids.get(id)) && allData.get(ids.get(id)).getFormula() != null
//					&& !allData.get(ids.get(id)).getFormula().equalsIgnoreCase("NONE")) {

				tcdbMetabolitesIDs.put(metabolite, map);
				metabolites.remove(metabolite);
//			}
		}
	}

	/**
	 * Identification of metabolites introducing D- and L- at the beginning.
	 */
	private void identificationIntroducingDandL() {

		for(String metabolite : new HashSet<>(metabolites.keySet())) {

			try {
				if(metabolite.matches("^(D*L*-+).+")){

					if(namesAndIDsContainer.getNamesWithoutSigns().containsKey(metabolite.replaceAll("^(D*L*-+)", ""))) {

						Map<MetaboliteMajorLabel, String> ids = allMetabolitesByName.get(metabolite.replaceAll("^(D*L*-+)", "")); 

						saveMetabolite(metabolite, ids);
					}
				}
				else {
					if(namesAndIDsContainer.getNamesWithoutSigns().containsKey("D-"+metabolite)) {

						Map<MetaboliteMajorLabel, String> ids = allMetabolitesByName.get("D-"+metabolite); 

						saveMetabolite(metabolite, ids);
					}

					if(namesAndIDsContainer.getNamesWithoutSigns().containsKey("L-"+metabolite)) {

						Map<MetaboliteMajorLabel, String> ids = allMetabolitesByName.get("L-"+metabolite); 

						saveMetabolite(metabolite, ids);
					}
				}
			} 
			catch (Exception e) {
				logger.trace("StackTrace: {}", e);
			}
		}
	}


	/**
	 * Identification of metabolites replacing non-alphanumeric characters and in lowercase.
	 */
	private void identificationReplacingNonAlphanumericAndInLowercase() {

		for(String metabolite : new HashSet<>(metabolites.keySet())) {

			try {
				if(namesAndIDsContainer.getNamesWithoutSigns().containsKey(metabolite.replaceAll("[^A-Za-z0-9]", "").toLowerCase())) {

					Map<MetaboliteMajorLabel, String> ids = allMetabolitesByName.get(metabolite.replaceAll("[^A-Za-z0-9]", "").toLowerCase()); 

					saveMetabolite(metabolite, ids);
				}
				else if(String.valueOf(metabolite.charAt(metabolite.length()-1)).equals(s)) { 
					if(namesAndIDsContainer.getNamesWithoutSigns().containsKey(metabolite.substring(0, metabolite.length()-1).replaceAll("[^A-Za-z0-9]", ""))) {

						Map<MetaboliteMajorLabel, String> ids = allMetabolitesByName.get(metabolite.substring(0, metabolite.length()-1).replaceAll("[^A-Za-z0-9]", "")); 

						saveMetabolite(metabolite, ids);
					}
				}
			} 
			catch (Exception e) {
				logger.trace("StackTrace: {}", e);
			}
		}
	}

	/**
	 * Identification of metabolites replacing non-alphanumeric characters.
	 */
	private void identificationReplacingNonAlphanumeric() {

		for(String metabolite : new HashSet<>(metabolites.keySet())) {

			try {
				if(namesAndIDsContainer.getNamesWithoutSigns().containsKey(metabolite.replaceAll("[^A-Za-z0-9]", ""))) {

					Map<MetaboliteMajorLabel, String> ids = allMetabolitesByName.get(metabolite.replaceAll("[^A-Za-z0-9]", "")); 

					saveMetabolite(metabolite, ids);
				}
				else if(String.valueOf(metabolite.charAt(metabolite.length()-1)).equals(s)) { 
					if(namesAndIDsContainer.getNamesWithoutSigns().containsKey(metabolite.substring(0, metabolite.length()-1).replaceAll("[^A-Za-z0-9]", ""))) {

						Map<MetaboliteMajorLabel, String> ids = allMetabolitesByName.get(metabolite.substring(0, metabolite.length()-1).replaceAll("[^A-Za-z0-9]", "")); 

						saveMetabolite(metabolite, ids);
					}
				}
			} 
			catch (Exception e) {
				logger.trace("StackTrace: {}", e);
			}
		}
	}

	/**
	 * Identification of metabolites using match in lower case.
	 */
	private void identificationInLowerCase() {

		for(String metabolite : new HashSet<>(metabolites.keySet())) {

			try {
				if(namesAndIDsContainer.getNamesLowerCase().containsKey(metabolite.toLowerCase())) {

					Map<MetaboliteMajorLabel, String> ids = allMetabolitesByName.get(metabolite.toLowerCase()); 

					saveMetabolite(metabolite, ids);
				}
				else if(String.valueOf(metabolite.charAt(metabolite.length()-1)).equals(s)) {

					if(namesAndIDsContainer.getNamesLowerCase().containsKey(metabolite.substring(0, metabolite.length()-1).toLowerCase())) {

						Map<MetaboliteMajorLabel, String> ids = allMetabolitesByName.get(metabolite.substring(0, metabolite.length()-1).toLowerCase()); 

						saveMetabolite(metabolite, ids);
					}
				}
			} 
			catch (Exception e) {
				logger.trace("StackTrace: {}", e);
			}
		}
	}

	/**
	 * Method to identify the metabolite by deleting the stoichiometry value (if present), and by deleting a last letter 's' if present too.
	 */
	private void identificationDeletingStoichiometry() {

		for(String metabolite : new HashSet<>(metabolites.keySet())) {

			try {
				identificationDeletingStoichiometryAux(metabolite, false);

				if(metabolites.keySet().contains(metabolite)) {

					if(String.valueOf(metabolite.charAt(metabolite.length()-1)).equals(s)) {

						identificationDeletingStoichiometryAux(metabolite, true);

					}
				}
			} 
			catch (Exception e) {
				logger.trace("StackTrace: {}", e);
			}
		}	
	}

	/**
	 * @param metabolite
	 * @param correctName
	 */
	private void identificationDeletingStoichiometryAux(String metabolite, boolean correctName) {

		String metabolite2 = metabolite;

		if(correctName)
			metabolite = metabolite.substring(0, metabolite.length()-1);

		try {

			String key = null;

			String met = "";

			for(int j = 0; j < REGEX_NAMES.length; j++) {
				met = metabolite.replace(REGEX_NAMES[j], REPLACEMENT_NAME[j]);
			}

			for(int i = 0; i < REGEX_STOICHIOMETRY.length && key == null; i++) {

				met =  met.replaceAll(REGEX_STOICHIOMETRY[i], "");

				if(allMetabolitesByName.containsKey(met)) {
					key = met;
				}

				if(key == null ) {

					String alias = dictionary.getSynonym(met.replace("\\s+", "").toLowerCase());

					if(alias != null) {

						if(allMetabolitesByName.containsKey(alias)) 
							key = alias;
					}
				}
			}

			if(key != null) {

				Map<MetaboliteMajorLabel, String> ids = allMetabolitesByName.get(key); 

				saveMetabolite(metabolite2, ids);
			}
		} 
		catch (Exception e) {

			logger.error("Error while processing metabolite: {}", metabolite);
			logger.trace("StackTrace {}",e);
		}
	}


	/**
	 * Method to identify the metabolites by direct match or without the last letter (if corresponds to 's' (usually indicating a plural word))
	 */
	private void identificationByDirectMatch() {

		for(String metabolite : new HashSet<>(metabolites.keySet())) {

			//			if(metabolite.equals("light"))
			//				System.out.println();

			try {
				if(metabolite.equalsIgnoreCase("galactonate"))
					metabolite = "l-galactonate";

				//			System.out.println(metabolite);

				//			System.out.println("map " + metabolite + "\t" + allMetabolitesByName.get(metabolite));

				if(allMetabolitesByName.containsKey(metabolite)) {

					//				System.out.println("yes");

					Map<MetaboliteMajorLabel, String> ids = allMetabolitesByName.get(metabolite); 
					saveMetabolite(metabolite, ids);
				}

				else if(String.valueOf(metabolite.charAt(metabolite.length()-1)).equals(s)) {

					if(allMetabolitesByName.containsKey(metabolite.substring(0, metabolite.length()-1))) {

						Map<MetaboliteMajorLabel, String> ids = allMetabolitesByName.get(metabolite.substring(0, metabolite.length()-1)); 
						saveMetabolite(metabolite, ids);
					}
				}

				if(metabolite.equalsIgnoreCase("maltooligosaccharides")) {	//resolver isto

					Map<MetaboliteMajorLabel, String> ids = new HashMap<>();
					ids.put(MetaboliteMajorLabel.MetaCyc, "META:Malto-Oligosaccharides");

					saveMetabolite(metabolite, ids);
				}
			} 
			catch (Exception e) {

				logger.warn("An error occurred during direct identification for metabolite: {}", metabolite);

				logger.trace("StackTrace {}",e);
			}
		}
	}


	/**
	 * Method to identify metabolites present in reactions.
	 * 
	 * @param data
	 * @return
	 */
	private Map<String, Set<String>> getMetabolitesFromReactions(Map<String, Set<TcNumberContainer2>> data){

		Map<String, Set<String>> metabolites = new HashMap<>();

		for(String accession : data.keySet()) {

			for(TcNumberContainer2 tcContainer : data.get(accession)) {

				for(int id : tcContainer.getAllReactionsIds()) {

					ReactionContainer reactionContainer = tcContainer.getReactionContainer(id);

					String[] reactions = new String[] {reactionContainer.getReaction(), 
							reactionContainer.getOriginalReaction()};
					
					for(String reaction : reactions) {
						
						for(Entry<String, Set<String>> entry : getMetabolitesToBeReplaced(reaction, 
								dictionary, reactionContainer.isCombineSameMetabolite()).entrySet()) {
							if(metabolites.containsKey(entry.getKey()))
								metabolites.get(entry.getKey()).addAll(entry.getValue());
							else
								metabolites.put(entry.getKey(), entry.getValue());
						}
					}
				}
			}
		}

		return metabolites;

	}

	/**
	 * @param reaction
	 * @param dictionary
	 * @return
	 */
	public static Set<String> getMetabolitesFromReaction(String reaction, Synonyms dictionary, boolean combineWithItself){

		Set<String> metabolites = new HashSet<>();

		reaction = reaction.replaceAll(ReactionContainer.REV_TOKEN, "\\+").replaceAll(ReactionContainer.IRREV_TOKEN, "\\+")
				.replaceAll(ReactionContainer.INTERIOR_COMPARTMENT_TOKEN_REG, "")
				.replaceAll(ReactionContainer.MIDDLE_COMPARTMENT_TOKEN_REG, "")
				.replaceAll(ReactionContainer.EXTERIOR_COMPARTMENT_TOKEN_REG, "");

		String[] metabs = reaction.split(" \\+ ");

		for(String metab : metabs) {

			if(!metab.matches("^(\\d+-).+")) {
				metab = metab.replaceAll("^(\\d+)", "");
			}

			metab = metab.replaceAll("^(\\+\\s)", "").trim();

			if(!metab.isEmpty()) {
				if(combineWithItself) {
					if(!metab.endsWith(TcdbMetabolitesContainer.SAME_METABOLITE_COMBINATION_SUFFIX))
						metabolites.add(metab);
				}
				else {
					metabolites.add(metab);
				}
			}
		}

		return metabolites;
	}

	/**
	 * Get metabolites present in one reaction
	 * 
	 * @param reaction
	 * @return
	 */
	public static Map<String, Set<String>> getMetabolitesToBeReplaced(String reaction, Synonyms dictionary, boolean combineWithItself){

		Set<String> metabolites = getMetabolitesFromReaction(reaction, dictionary, combineWithItself);

		//		System.out.println(metabolites);

		return standardizationOfNames3(metabolites, dictionary);
	}

	/**
	 * Auxiliar method for normalization of names.
	 * 
	 * @param metabolites
	 * @return
	 */
	private static Set<String> standardizationOfNames1(Set<String> metabolites, Synonyms dictionary) {

		Set<String> standardNames = new HashSet<>();

		for(String metabolite : metabolites) {

			try {
				if(metabolite.matches("(?i).+(-P)$"))
					metabolite = metabolite.replaceAll("(?i)(-P)$", "\\sphosphate");

				metabolite = metabolite.replaceAll("(?i)(ic acids*)", "ate");

				String word = dictionary.getSynonym(metabolite.replaceAll("\\s+", "").toLowerCase());

				if(word != null)
					standardNames.add(word);
				else
					standardNames.add(metabolite);
			} 
			catch (Exception e) {
				logger.trace("StackTrace: {}", e);
			}
		}

		return standardNames;
	}

	/**
	 * Auxiliar method for normalization of names.
	 * 
	 * @param allMetabolitesByName
	 * @return
	 */
	private static BiosynthMetabolites standardizationOfNames2(BiosynthMetabolites namesAndIDsContainer) {

		Synonyms dictionary = new Synonyms();

		for(String metabolite : new HashSet<>(namesAndIDsContainer.getMetabolitesIDs().keySet())) {

			String word = dictionary.getSynonym(metabolite.replaceAll("\\s+", "").toLowerCase());

			if(word != null) {

				if(namesAndIDsContainer.getMetabolitesIDs().containsKey(word)) {

					namesAndIDsContainer.getMetabolitesIDs().get(word).putAll(namesAndIDsContainer.getMetabolitesIDs().get(metabolite));
				}
				else {
					namesAndIDsContainer.getMetabolitesIDs().put(word, namesAndIDsContainer.getMetabolitesIDs().get(metabolite));

					namesAndIDsContainer.getNamesLowerCase().put(word, word.toLowerCase());
					namesAndIDsContainer.getNamesLowerCaseWithoutSigns().put(word, word.replaceAll("[^A-Za-z0-9]", "").toLowerCase());
					namesAndIDsContainer.getNamesWithoutSigns().put(word, word.replaceAll("[^A-Za-z0-9]", ""));
				}
			}
			else {
				//				namesAndIDsContainer.getMetabolitesIDs().put(metabolite, namesAndIDsContainer.getMetabolitesIDs().get(metabolite));
				//
				//				namesAndIDsContainer.getNamesLowerCase().put(metabolite, metabolite.toLowerCase());
				//				namesAndIDsContainer.getNamesLowerCaseWithoutSigns().put(metabolite, metabolite.replaceAll("[^A-Za-z0-9]", "").toLowerCase());
				//				namesAndIDsContainer.getNamesWithoutSigns().put(metabolite, metabolite.replaceAll("[^A-Za-z0-9]", ""));

				word = metabolite;
			}

			String originalWord = word;

			if(word.matches("ic acids*"))
				word = word.replaceAll("ic acids*", "ate");

			if(!word.equals(originalWord)) {
				namesAndIDsContainer.getMetabolitesIDs().put(word, namesAndIDsContainer.getMetabolitesIDs().get(metabolite));
				namesAndIDsContainer.getNamesLowerCase().put(word, word.toLowerCase());
				namesAndIDsContainer.getNamesLowerCaseWithoutSigns().put(word, word.replaceAll("[^A-Za-z0-9]", "").toLowerCase());
				namesAndIDsContainer.getNamesWithoutSigns().put(word, word.replaceAll("[^A-Za-z0-9]", ""));
			}

		}

		return namesAndIDsContainer;

	}

	/**
	 * Auxiliar method for normalization of names.
	 * 
	 * @param metabolites
	 * @return
	 */
	public static Map<String, Set<String>> standardizationOfNames3(Set<String> metabolites, Synonyms dictionary) {

		Map<String, Set<String>> standardNames = new HashMap<>();

		for(String metabolite : metabolites) {

			String original = metabolite;

			if(metabolite.matches("(?i).+(-P)$"))
				metabolite = metabolite.replaceAll("(?i)(-P)$", "-phosphate");

			metabolite = metabolite.replaceAll("(?i)(ic acids*)", "ate");

			if((metabolite.contains("Fe3+-") || metabolite.contains("fe3+-"))) {

				if(metabolite.contains("ferri"))
					metabolite = metabolite.replace("Fe3+-", "").replace("fe3+-", "");
				else
					metabolite = metabolite.replace("Fe3+-", "ferri").replace("fe3+--", "ferri");

			}

			String word = dictionary.getSynonym(metabolite.replaceAll("\\s+", "").toLowerCase());

			//			System.out.println(metabolite + "\t" + word);

			if(metabolite.equalsIgnoreCase("Arabinose"))
				metabolite = "Arabinoses";	
			if(metabolite.equalsIgnoreCase("Fatty acyl-CoA"))
				metabolite = "Acyl coenzyme A";	

			if(word != null) {
				if(standardNames.containsKey(word))
					standardNames.get(word).add(original);
				else { 
					Set<String> set = new HashSet<>();
					set.add(original);

					standardNames.put(word, set);
				}
			}
			else {
				if(standardNames.containsKey(metabolite))
					standardNames.get(metabolite).add(original);
				else { 
					Set<String> set = new HashSet<>();
					set.add(original);

					standardNames.put(metabolite, set);
				}
			}
		}
		return standardNames;
	}


	/**
	 * Get the best database to retrieve the key
	 * 
	 * @param ids
	 * @return
	 */
	private MetaboliteMajorLabel selectMetaboliteMajorLabel(String metabolite, Map<MetaboliteMajorLabel, String> ids){

		//		if(metabolite.equalsIgnoreCase("AMP"))
		//			System.out.println(metabolite + "\t" + ids);
		//		if(metabolite.equalsIgnoreCase("Fatty acid"))
		//			System.out.println(metabolite + "\t" + ids);
		//		if(metabolite.equalsIgnoreCase("Coenzyme A"))
		//			System.out.println(metabolite + "\t" + ids);
		//		if(metabolite.equalsIgnoreCase("acyl-Coenzyme A"))
		//			System.out.println(metabolite + "\t" + ids);
		//		if(metabolite.equalsIgnoreCase("Diphosphate"))
		//			System.out.println(metabolite + "\t" + ids);
		//		if(metabolite.equalsIgnoreCase("ATP"))
		//			System.out.println(metabolite + "\t" + ids);

		try {

			if(ids.containsKey(MetaboliteMajorLabel.MetaCyc)) {

				String[] entry = new String[2];

				entry[0] = ids.get(MetaboliteMajorLabel.MetaCyc);
				entry[1] = MetaboliteMajorLabel.MetaCyc.toString();
				
				if(metabolite.equals("META:Amino-Acids-20"))
					entry[0] = metabolite;

				forChildsSearch.put(metabolite, entry);	
			}
			else if(ids.containsKey(MetaboliteMajorLabel.EcoCyc)) {		//ecocyc after metacyc

				String[] entry = new String[2];

				entry[0] = ids.get(MetaboliteMajorLabel.MetaCyc);
				entry[1] = MetaboliteMajorLabel.MetaCyc.toString();
				
				if(metabolite.equals("META:Amino-Acids-20"))
					entry[0] = metabolite;

				forChildsSearch.put(metabolite, entry);
			}
			
			if(overrideDefauldLabel != null && ids.containsKey(overrideDefauldLabel))
				return overrideDefauldLabel;

			else if(ids.containsKey(MetaboliteMajorLabel.ModelSeed))
				return MetaboliteMajorLabel.ModelSeed;

			else if(ids.containsKey(MetaboliteMajorLabel.MetaCyc))
				return MetaboliteMajorLabel.MetaCyc;

			else if(ids.containsKey(MetaboliteMajorLabel.EcoCyc))
				return MetaboliteMajorLabel.EcoCyc;

			else if(ids.containsKey(MetaboliteMajorLabel.LigandCompound))
				return MetaboliteMajorLabel.LigandCompound;

			else if(ids.containsKey(MetaboliteMajorLabel.BiGG))
				return MetaboliteMajorLabel.BiGG;

			else if(ids.containsKey(MetaboliteMajorLabel.BiGGMetabolite))
				return MetaboliteMajorLabel.BiGGMetabolite;

			else {										//returns a 'random' key
				for(MetaboliteMajorLabel key : ids.keySet())		
					return key;			
			}
		}
		catch (Exception e) {

			logger.error("Error while selecting best metabolite major label for: {}", metabolite);
			logger.trace("StackTrace: {}", e);
		}

		return null;
	}

	/**
	 * @return the tcdbMetabolitesIDs
	 */
	public Map<String, Map<String, MetaboliteMajorLabel>> getTcdbMetabolitesIDs() {
		return tcdbMetabolitesIDs;
	}

	/**
	 * @return the forChildsSearch
	 */
	public Map<String, String[]> getforChildsSearch() {
		return forChildsSearch;
	}

	public Map<String, String> getMetabolitesByOriginalName() {
		return this.metabolitesByOriginalName;
	}

	/**
	 * @return the overrideDefauldLabel
	 */
	public MetaboliteMajorLabel getOverrideDefauldLabel() {
		return overrideDefauldLabel;
	}

	/**
	 * @param overrideDefauldLabel the overrideDefauldLabel to set
	 */
	public void setOverrideDefauldLabel(MetaboliteMajorLabel overrideDefauldLabel) {
		this.overrideDefauldLabel = overrideDefauldLabel;
	}

}
