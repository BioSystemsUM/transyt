package pt.uminho.ceb.biosystems.transyt.service.reactions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import pt.uminho.ceb.biosystems.transyt.service.containers.BiosynthMetabolites;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.ReactionContainer;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.TcNumberContainer2;
import pt.uminho.ceb.biosystems.transyt.utilities.dictionary.Synonyms;
import pt.uminho.sysbio.biosynth.integration.io.dao.neo4j.MetaboliteMajorLabel;
import pt.uminho.sysbio.biosynthframework.BiodbGraphDatabaseService;

/**
 * 
 * @deprecated Use class IdentifyReactionsMetabolites.java instead
 */
@Deprecated
public class ReactionsMetabolites {

	public static final String[] REGEX_STOICHIOMETRY =  {"^(\\s*n\\s*)", "^(\\d+\\/*\\d*\\s*)"};
	public static final String[] REGEX_NAMES =  {"ic acids*"};
	public static final String[] REPLACEMENT_NAME =  {"ate"};
	public static final String s = "s";

	public static Set<String> getMetabolitesFromReactions(Map<String, Set<TcNumberContainer2>> data){

		Set<String> metabolites	= new HashSet<>();

		int i = 0;

		for(String accession : data.keySet()) {

			for(TcNumberContainer2 tcContainer : data.get(accession)) {

				for(int id : tcContainer.getAllReactionsIds()) {

					ReactionContainer reactionContainer = tcContainer.getReactionContainer(id);

					i++;

					String reaction = reactionContainer.getReaction();

					//					System.out.println(reaction);

					reaction = reaction.replaceAll(ReactionContainer.REV_TOKEN, "\\+").replaceAll(ReactionContainer.IRREV_TOKEN, "\\+")
							.replaceAll("\\(" + ReactionContainer.INTERIOR_COMPARTMENT + "\\)", "").replaceAll("\\(" + ReactionContainer.EXTERIOR_COMPARTMENT + "\\)", "");

					String[] metabs = reaction.split(" \\+ ");

					for(String metab : metabs) 
						metabolites.add(metab.trim());

				}
			}
		}

		//		
		//		for(String metab : metabolites) 
		//			System.out.println(metab);
		//		
		//		System.out.println(metabolites.size());
		//		
		//		System.out.println(i);



		return metabolites;

	}

	/**
	 * get ids for metabolites that are present in pt.uminho.ceb.biosystems.transyt.scraper.tcdb's pt.uminho.ceb.biosystems.transyt.service.reactions
	 * 
	 * @param metabolites
	 * @param allMetabolitesByName
	 * @return
	 */
	public static Map<String, Map<String, MetaboliteMajorLabel>> getMetabolitesIDs(Set<String> metabolites, BiosynthMetabolites namesAndIDsContainer, BiodbGraphDatabaseService service) {

		Synonyms dictionary = new Synonyms();

		Map<String, Map<String, MetaboliteMajorLabel>> tcdbMetabolitesIDs = new HashMap<>();

		//		Set<String> forSearch = new HashSet<>(metabolites);

		metabolites = new HashSet<>(standardizationOfNames1(metabolites));

		namesAndIDsContainer = standardizationOfNames2(namesAndIDsContainer);

		System.out.println("metabolites >>>" + metabolites.size());

		System.out.println("allmetabolites >>>" + namesAndIDsContainer.getMetabolitesIDs().size());

		Map<String, Map<MetaboliteMajorLabel, String>> allMetabolitesByName = new HashMap<>(namesAndIDsContainer.getMetabolitesIDs());

		//direct match

		for(String metabolite : new HashSet<>(metabolites)) {

			//			System.out.println(metabolite);

			if(allMetabolitesByName.containsKey(metabolite)) {

				//				System.out.println("MET!!!" + metabolite);

				Map<MetaboliteMajorLabel, String> ids = allMetabolitesByName.get(metabolite); 

				MetaboliteMajorLabel id = selectMetaboliteMajorLabel(ids);

				if(id != null) {
					Map<String, MetaboliteMajorLabel> map = new HashMap<>();

					map.put(ids.get(id), id);

					tcdbMetabolitesIDs.put(metabolite, map);

					metabolites.remove(metabolite);
				}
			}

			else if(String.valueOf(metabolite.charAt(metabolite.length()-1)).equals(s)) {

				if(allMetabolitesByName.containsKey(metabolite.substring(0, metabolite.length()-1))) {

					//				System.out.println("MET!!!" + metabolite);

					Map<MetaboliteMajorLabel, String> ids = allMetabolitesByName.get(metabolite.substring(0, metabolite.length()-1)); 

					MetaboliteMajorLabel id = selectMetaboliteMajorLabel(ids);

					if(id != null) {
						Map<String, MetaboliteMajorLabel> map = new HashMap<>();

						map.put(ids.get(id), id);

						tcdbMetabolitesIDs.put(metabolite, map);

						metabolites.remove(metabolite);
					}
				}
			}
		}

		System.out.println("FOUND1: " + tcdbMetabolitesIDs.size());		//818

		//stoichiometry

		for(String metabolite : new HashSet<>(metabolites)) {

			try {
				//			System.out.println(metabolite);

				//			if(metabolite.equalsIgnoreCase("nhco3-"))
				//				System.out.println(metabolite + "\t" + REGEX);

				String key = null;

				String met = "";

				for(int j = 0; j < REGEX_NAMES.length; j++) {
					met = metabolite.replace(REGEX_NAMES[j], REPLACEMENT_NAME[j]);
					//				System.out.println(metabolite.replace(REGEX_NAMES[j], REPLACEMENT_NAME[j]));
					//				System.out.println("ff " +met);
				}

				for(int i = 0; i < REGEX_STOICHIOMETRY.length && key == null; i++) {

					//				System.out.println("MET>>>" + met);

					met =  met.replaceAll(REGEX_STOICHIOMETRY[i], "");

					//				if(metabolite.equalsIgnoreCase("nhco3-"))
					//					System.out.println(met + REGEX[i]);

					if(allMetabolitesByName.containsKey(met)) {
						key = met;
						//						System.out.println(key + "\t" + allMetabolitesByName.get(met));
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

					//				System.out.println("key " + key);

					Map<MetaboliteMajorLabel, String> ids = allMetabolitesByName.get(key); 

					MetaboliteMajorLabel id = selectMetaboliteMajorLabel(ids);

					if(id != null) {
						Map<String, MetaboliteMajorLabel> map = new HashMap<>();

						map.put(ids.get(id), id);

						tcdbMetabolitesIDs.put(metabolite, map);

						metabolites.remove(metabolite);
					}
				}
				//				else
				//					System.out.println(met + "\t <<<>>>" + metabolite);
			} 
			catch (Exception e) {

				System.out.println("[ERROR] while processing metabolite: " + metabolite);
				e.printStackTrace();
			}
		}	

		for(String metabolite : new HashSet<>(metabolites)) {

			String metabolite2 = metabolite;

			if(String.valueOf(metabolite.charAt(metabolite.length()-1)).equals(s)) {

				metabolite = metabolite.substring(0, metabolite.length()-1);

				try {
					//			System.out.println(metabolite);

					//			if(metabolite.equalsIgnoreCase("nhco3-"))
					//				System.out.println(metabolite + "\t" + REGEX);

					String key = null;

					String met = "";

					for(int j = 0; j < REGEX_NAMES.length; j++) {
						met = metabolite.replace(REGEX_NAMES[j], REPLACEMENT_NAME[j]);
						//				System.out.println(metabolite.replace(REGEX_NAMES[j], REPLACEMENT_NAME[j]));
						//				System.out.println("ff " +met);
					}

					for(int i = 0; i < REGEX_STOICHIOMETRY.length && key == null; i++) {

						//				System.out.println("MET>>>" + met);

						met =  met.replaceAll(REGEX_STOICHIOMETRY[i], "");

						//				if(metabolite.equalsIgnoreCase("nhco3-"))
						//					System.out.println(met + REGEX[i]);

						if(allMetabolitesByName.containsKey(met)) {
							key = met;
							//						System.out.println(key + "\t" + allMetabolitesByName.get(met));
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

						//				System.out.println("key " + key);

						Map<MetaboliteMajorLabel, String> ids = allMetabolitesByName.get(key); 

						MetaboliteMajorLabel id = selectMetaboliteMajorLabel(ids);

						if(id != null) {
							Map<String, MetaboliteMajorLabel> map = new HashMap<>();

							map.put(ids.get(id), id);

							tcdbMetabolitesIDs.put(metabolite2, map);

							metabolites.remove(metabolite2);
						}
					}
					//				else
					//					System.out.println(met + "\t <<<>>>" + metabolite);
				} 
				catch (Exception e) {

					System.out.println("[ERROR] while processing metabolite: " + metabolite);
					e.printStackTrace();
				}
			}	
		}

		System.out.println("FOUND2: " + tcdbMetabolitesIDs.size());		//878

		for(String metabolite : new HashSet<>(metabolites)) {



			//			System.out.println(metabolite);

			if(namesAndIDsContainer.getNamesLowerCase().containsKey(metabolite.toLowerCase())) {

				//				System.out.println("MET!!!" + metabolite);

				Map<MetaboliteMajorLabel, String> ids = allMetabolitesByName.get(metabolite.toLowerCase()); 

				//				System.out.println(ids);

				MetaboliteMajorLabel id = selectMetaboliteMajorLabel(ids);

				if(id != null) {
					Map<String, MetaboliteMajorLabel> map = new HashMap<>();

					map.put(ids.get(id), id);

					tcdbMetabolitesIDs.put(metabolite, map);

					metabolites.remove(metabolite);
				}
			}
			else if(String.valueOf(metabolite.charAt(metabolite.length()-1)).equals(s)) {

				if(namesAndIDsContainer.getNamesLowerCase().containsKey(metabolite.substring(0, metabolite.length()-1).toLowerCase())) {

					//				System.out.println("MET!!!" + metabolite);

					Map<MetaboliteMajorLabel, String> ids = allMetabolitesByName.get(metabolite.substring(0, metabolite.length()-1).toLowerCase()); 

					//				System.out.println(ids);

					MetaboliteMajorLabel id = selectMetaboliteMajorLabel(ids);

					if(id != null) {
						Map<String, MetaboliteMajorLabel> map = new HashMap<>();

						map.put(ids.get(id), id);

						tcdbMetabolitesIDs.put(metabolite, map);

						metabolites.remove(metabolite);
					}
				}
			}
		}
		
		System.out.println("FOUND3: " + tcdbMetabolitesIDs.size());		//926

		for(String metabolite : new HashSet<>(metabolites)) {

			//			System.out.println(metabolite);

			if(namesAndIDsContainer.getNamesWithoutSigns().containsKey(metabolite.replaceAll("[^A-Za-z0-9]", ""))) {

				//				System.out.println("MET!!!" + metabolite);

				Map<MetaboliteMajorLabel, String> ids = allMetabolitesByName.get(metabolite.replaceAll("[^A-Za-z0-9]", "")); 

				//				System.out.println(ids);

				MetaboliteMajorLabel id = selectMetaboliteMajorLabel(ids);

				if(id != null) {
					Map<String, MetaboliteMajorLabel> map = new HashMap<>();

					map.put(ids.get(id), id);

					tcdbMetabolitesIDs.put(metabolite, map);

					metabolites.remove(metabolite);
				}
			}
			else if(String.valueOf(metabolite.charAt(metabolite.length()-1)).equals(s)) { 
				if(namesAndIDsContainer.getNamesWithoutSigns().containsKey(metabolite.substring(0, metabolite.length()-1).replaceAll("[^A-Za-z0-9]", ""))) {

					//				System.out.println("MET!!!" + metabolite);

					Map<MetaboliteMajorLabel, String> ids = allMetabolitesByName.get(metabolite.substring(0, metabolite.length()-1).replaceAll("[^A-Za-z0-9]", "")); 

					//				System.out.println(ids);

					MetaboliteMajorLabel id = selectMetaboliteMajorLabel(ids);

					if(id != null) {
						Map<String, MetaboliteMajorLabel> map = new HashMap<>();

						map.put(ids.get(id), id);

						tcdbMetabolitesIDs.put(metabolite, map);

						metabolites.remove(metabolite);
					}
				}
			}
		}
		System.out.println("FOUND4: " + tcdbMetabolitesIDs.size());		//931
		
		for(String metabolite : new HashSet<>(metabolites)) {

			//			System.out.println(metabolite);

			if(namesAndIDsContainer.getNamesWithoutSigns().containsKey("D-"+metabolite)) {

				//				System.out.println("MET!!!" + metabolite);

				Map<MetaboliteMajorLabel, String> ids = allMetabolitesByName.get("D-"+metabolite); 

				//				System.out.println(ids);

				MetaboliteMajorLabel id = selectMetaboliteMajorLabel(ids);
				
				if(id != null) {
					
					Map<String, MetaboliteMajorLabel> map = new HashMap<>();

					map.put(ids.get(id), id);

					tcdbMetabolitesIDs.put(metabolite, map);

					metabolites.remove(metabolite);
				}
			}
			
			if(namesAndIDsContainer.getNamesWithoutSigns().containsKey("L-"+metabolite)) {

				//				System.out.println("MET!!!" + metabolite);

				Map<MetaboliteMajorLabel, String> ids = allMetabolitesByName.get("L-"+metabolite); 

				//				System.out.println(ids);

				MetaboliteMajorLabel id = selectMetaboliteMajorLabel(ids);
				
				if(id != null) {
					
					if(tcdbMetabolitesIDs.containsKey(metabolite)) {
						
						tcdbMetabolitesIDs.get(metabolite).put(ids.get(id), id);
					}
					else {
						
						Map<String, MetaboliteMajorLabel> map = new HashMap<>();

						map.put(ids.get(id), id);

						tcdbMetabolitesIDs.put(metabolite, map);

						metabolites.remove(metabolite);
					}
				}
			}
		}
		System.out.println("FOUND5: " + tcdbMetabolitesIDs.size());		//931

		//		for(String metabolite : new HashSet<>(metabolites)) {
		//			
		////			System.out.println(metabolite);
		//
		//			if(namesAndIDsContainer.getNamesLowerCaseWithoutSigns().containsKey(metabolite.replaceAll("[^A-Za-z0-9]", "").toLowerCase())) {
		//				
		//				System.out.println("MET!!!" + metabolite);
		//
		//				Map<MetaboliteMajorLabel, String> ids = allMetabolitesByName.get(metabolite.replaceAll("[^A-Za-z0-9]", "").toLowerCase()); 
		//
		//				System.out.println(ids);
		//				
		//				MetaboliteMajorLabel id = selectMetaboliteMajorLabel(ids);
		//
		//				if(id != null) {
		//					Map<String, MetaboliteMajorLabel> map = new HashMap<>();
		//
		//					map.put(ids.get(id), id);
		//
		//					tcdbMetabolitesIDs.put(metabolite, map);
		//
		//					metabolites.remove(metabolite);
		//				}
		//			}
		//		}

		System.out.println();
//
//		for(String metabolite : metabolites)
//			System.out.println(metabolite);
//
//		System.out.println(metabolites.size());
//
		System.out.println();

		return tcdbMetabolitesIDs;		
	}
	
	/**
	 * Auxiliar method for normalization of names.
	 * 
	 * @param metabolites
	 * @return
	 */
	private static Set<String> standardizationOfNames1(Set<String> metabolites) {

		Set<String> standardNames = new HashSet<>();

		Synonyms dictionary = new Synonyms();

		for(String metabolite : metabolites) {

			metabolite = metabolite.replaceAll("ic acid", "ate");

			String word = dictionary.getSynonym(metabolite.replaceAll("\\s+", "").toLowerCase());

			if(word != null)
				standardNames.add(word);
			else
				standardNames.add(metabolite);
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
				namesAndIDsContainer.getMetabolitesIDs().put(metabolite, namesAndIDsContainer.getMetabolitesIDs().get(metabolite));

				namesAndIDsContainer.getNamesLowerCase().put(metabolite, metabolite.toLowerCase());
				namesAndIDsContainer.getNamesLowerCaseWithoutSigns().put(metabolite, metabolite.replaceAll("[^A-Za-z0-9]", "").toLowerCase());
				namesAndIDsContainer.getNamesWithoutSigns().put(metabolite, metabolite.replaceAll("[^A-Za-z0-9]", ""));

				word = metabolite;
			}

			String originalWord = word;

			if(word.matches("ic acids*"))
				word = word.replaceAll("ic acids*", "ate");
			else if(word.matches("(ates*)$")) 
				word = word.replaceAll("(ates*)$", "ic acid");

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
	 * Get the best database to retrieve the key
	 * 
	 * @param ids
	 * @return
	 */
	private static MetaboliteMajorLabel selectMetaboliteMajorLabel(Map<MetaboliteMajorLabel, String> ids){

		if(ids.containsKey(MetaboliteMajorLabel.ModelSeed))
			return MetaboliteMajorLabel.ModelSeed;

		else if(ids.containsKey(MetaboliteMajorLabel.MetaCyc))
			return MetaboliteMajorLabel.MetaCyc;

		else if(ids.containsKey(MetaboliteMajorLabel.EcoCyc))
			return MetaboliteMajorLabel.EcoCyc;

		else if(ids.containsKey(MetaboliteMajorLabel.LigandCompound))
			return MetaboliteMajorLabel.LigandCompound;

		else {										//returns a 'random' key
			for(MetaboliteMajorLabel key : ids.keySet())		
				return key;			
		}

		return null;
	}
}
