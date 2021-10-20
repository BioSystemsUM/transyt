package pt.uminho.ceb.biosystems.transyt.service.internalDB;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterators;

import pt.uminho.ceb.biosystems.transyt.service.containers.BiosynthMetabolites;
import pt.uminho.ceb.biosystems.transyt.service.utilities.FileUtils;
import pt.uminho.ceb.biosystems.transyt.utilities.files.FilesUtils;
import pt.uminho.ceb.biosystems.transyt.utilities.transytUtilities.Utilities;
import pt.uminho.sysbio.biosynth.integration.io.dao.neo4j.MetaboliteMajorLabel;
import pt.uminho.sysbio.biosynth.integration.io.dao.neo4j.MetabolitePropertyLabel;
import pt.uminho.sysbio.biosynth.integration.neo4j.BiodbMetaboliteNode;
import pt.uminho.sysbio.biosynthframework.BiodbGraphDatabaseService;

public class FetchCompoundsByName {

	public final static String NAMESLOWERCASEWITHOUTSIGNS_PATH = FilesUtils.getMetabolitesNamesMatcherFilesDirectory().concat("NamesLowerCaseWithoutSigns.txt");
	public final static String NAMESWITHOUTSIGNS_PATH = FilesUtils.getMetabolitesNamesMatcherFilesDirectory().concat("NamesWithoutSigns.txt");
	public final static String NAMESLOWERCASE_PATH = FilesUtils.getMetabolitesNamesMatcherFilesDirectory().concat("NamesLowerCase.txt");
	public final static String METABOLITESIDS_PATH = FilesUtils.getMetabolitesNamesMatcherFilesDirectory().concat("MetabolitesIDs.txt");
	public final static String FORREPLACEMENT_PATH = FilesUtils.getMetabolitesNamesMatcherFilesDirectory().concat("forReplacement.txt");

	private static final Set<MetaboliteMajorLabel> DEFAULT_DATABASES = Set.of(MetaboliteMajorLabel.LigandCompound, MetaboliteMajorLabel.LigandGlycan,
			MetaboliteMajorLabel.ModelSeed, MetaboliteMajorLabel.MetaCyc, MetaboliteMajorLabel.EcoCyc, MetaboliteMajorLabel.BiGG, MetaboliteMajorLabel.BiGGMetabolite);

	private Map<String, Map<MetaboliteMajorLabel, String>> compounds;
	private Map<String, String> formulas;
	private Map<String, String> forReplacement;
	private Map<String, String> namesLowerCaseWithoutSigns;	//for later comparison
	private Map<String, String> namesWithoutSigns;	//for later comparison
	private Map<String, String> namesLowerCase;		//for later comparison

	private BiodbGraphDatabaseService service;

	private static final Logger logger = LoggerFactory.getLogger(FetchCompoundsByName.class);

	/**
	 * Method to retrieve biosynts' metabolites by name from all major databases
	 * 
	 * @param service
	 * @return
	 */
	public FetchCompoundsByName(BiodbGraphDatabaseService service, boolean useCache) {

		compounds = new HashMap<>();
		forReplacement = new HashMap<>();
		formulas = new HashMap<>();

		namesLowerCaseWithoutSigns = new HashMap<>();	//for later comparison
		namesWithoutSigns = new HashMap<>();	//for later comparison
		namesLowerCase = new HashMap<>();		//for later comparison

		this.service = service;

		if(useCache) {
			
			namesLowerCaseWithoutSigns = FilesUtils.readMapFromFile(NAMESLOWERCASEWITHOUTSIGNS_PATH);
			namesWithoutSigns = FilesUtils.readMapFromFile(NAMESWITHOUTSIGNS_PATH);
			namesLowerCase = FilesUtils.readMapFromFile(NAMESLOWERCASE_PATH);

			compounds = FileUtils.readMapFromFile2(METABOLITESIDS_PATH);

		}
		else {

			getNamesMethod1();
			getNamesMethod2();

			forReplacement.put("cpd02416", "cpd00122");
			forReplacement.put("cpd11665", "cpd15291");

			for(String s : compounds.keySet()) {

				String id = compounds.get(s).get(MetaboliteMajorLabel.ModelSeed);

				if(forReplacement.containsKey(id)) {
					compounds.get(s).put(MetaboliteMajorLabel.ModelSeed, forReplacement.get(id));
				}
			}

			compounds.remove("R");
			compounds.remove("r");
			compounds.remove("P");
			compounds.remove("p");

			FilesUtils.saveMapInFile(FORREPLACEMENT_PATH, forReplacement);

			FileUtils.saveMapInFile2(METABOLITESIDS_PATH, compounds);
			FilesUtils.saveMapInFile(NAMESLOWERCASE_PATH, namesLowerCase);
			FilesUtils.saveMapInFile(NAMESLOWERCASEWITHOUTSIGNS_PATH, namesLowerCaseWithoutSigns);
			FilesUtils.saveMapInFile(NAMESWITHOUTSIGNS_PATH, namesWithoutSigns);
		}

	}

	private void getNamesMethod1() {

		//		BiodbMetaboliteNode node1 = service.getMetabolite("cpd00027", MetaboliteMajorLabel.ModelSeed);
		//
		//
		//
		//		System.out.println(node1.getAllProperties());
		//
		//
		//		Iterable<RelationshipType> rels1 = node1.getRelationshipTypes();
		//		Iterable<RelationshipType> rels2 = node1.getRelationshipTypes();
		//
		//		for(RelationshipType r : rels1) {
		//
		//			System.out.println(r.name());
		//
		//		}
		//
		//		Iterable<Relationship> crossref1 = node1.getRelationships(RelationshipType.withName("has_name"));
		//
		//
		//		for(Relationship r : crossref1) {
		//
		//			//			System.out.println(r.getAllProperties());
		//			System.out.println(r.getEndNode().getAllProperties());
		//
		//		}
		//
		//		System.out.println();
		//		System.out.println("##############################");
		//		System.out.println();
		//
		//		BiodbMetaboliteNode node2 = service.getMetabolite("cpd26821", MetaboliteMajorLabel.ModelSeed);
		//		System.out.println(node2.getAllProperties());
		//
		//		for(RelationshipType r : rels2) {
		//
		//			System.out.println(r.name());
		//
		//		}
		//
		//		Iterable<Relationship> crossref2 = node2.getRelationships(RelationshipType.withName("has_name"));
		//
		//
		//		for(Relationship r : crossref2) {
		//
		//			//			System.out.println(r.getAllProperties());
		//			System.out.println(r.getEndNode().getAllProperties());
		//
		//		}

		logger.trace("Searching using method 1");

		int lastProgress = -1;
		int current = 0;

		ResourceIterator<Node> nodes = service.findNodes(MetabolitePropertyLabel.Name);

		int dataSize = Iterators.size(nodes);

		nodes = service.findNodes(MetabolitePropertyLabel.Name);

		while(nodes.hasNext()) {

			Map<MetaboliteMajorLabel, Set<Long>> counts = new HashMap<>();

			Node node = nodes.next();

			if(node.hasProperty("key")) {
				//				if(node.hasProperty("key") && node.getProperty("key").toString().equals("Mn2+")) {

				String name = node.getProperty("key").toString();

				for(Relationship rel : node.getRelationships()) {

					Node metaboliteNode = rel.getStartNode();

					if(metaboliteNode.hasProperty("major_label") && metaboliteNode.hasProperty("entry")) {

						MetaboliteMajorLabel label = getMetaboliteLabel(metaboliteNode);

						//												System.out.println(label + "\t" + metaboliteNode.getAllProperties());

						Set<Long> set = new HashSet<>();
						if(counts.containsKey(label))
							set = counts.get(label);

						set.add(metaboliteNode.getId());
						counts.put(label, set);
					}
				}
				saveNameAux(name, counts);
			}

			current++;

			Integer progress = (current*100)/dataSize;

			if(progress > lastProgress){

				lastProgress = progress;
				logger.trace(progress.toString().concat(" % search complete"));
			}
		}
	}


	private void getNamesMethod2() {

		Map<String, Map<MetaboliteMajorLabel, Set<Long>>> namesCounts = new HashMap<>();

		Set<BiodbMetaboliteNode> allMetabolites = service.listMetabolites();

		logger.trace("Searching using method 2");

		int lastProgress = -1;
		int current = 0;

		for(BiodbMetaboliteNode node : allMetabolites) {

			Map<MetaboliteMajorLabel, Set<Long>> counts = new HashMap<>();
			
			String entryID = node.getEntry();

			if(!entryID.isEmpty()) {
				
				

				Map<String, Object> nodeProperties = node.getAllProperties();

				Set<String> names = getSynonyms(node, nodeProperties, service);
				
				if(entryID.matches("META:.*") || entryID.matches("cpd.*"))
					names.add(entryID);

				for(String name : names) {

					if(!compounds.containsKey(name)) {

						if(!namesCounts.containsKey(name))
							namesCounts.put(name, new HashMap<>());

						counts = namesCounts.get(name);

						if(node.hasProperty("major_label")) {

							MetaboliteMajorLabel label = getMetaboliteLabel(node);

							if(counts.containsKey(label)) {

								counts.get(label).add(node.getId());
							}
							else {
								Set<Long> set = new HashSet<>();
								set.add(node.getId());

								counts.put(label, set);
							}
						}

						namesCounts.put(name, counts);
					}
				}
			}
		}

		for(String name : namesCounts.keySet()) {

//			if(name.equals("Mn2+")) {

				saveNameAux(name, namesCounts.get(name));

				current++;

				Integer progress = (current*100)/namesCounts.size();

				if(progress > lastProgress){

					lastProgress = progress;
					logger.trace(progress.toString().concat(" % search complete"));
				}

//			}
		}
	}

	private void saveNameAux(String name, Map<MetaboliteMajorLabel, Set<Long>> counts) {

		Map<MetaboliteMajorLabel, String> references = new HashMap<>();

		//		System.out.println(counts);

		Long id = selectBestNode(counts);

		//		id = service.getMetabolite("META:CPD-9956", MetaboliteMajorLabel.MetaCyc).getId();

		Node metaboliteNode = null;

		if(id != null) {

			metaboliteNode = service.getNodeById(id);

			String formula = "";

			if(metaboliteNode.hasProperty("formula"))
				formula = metaboliteNode.getProperty("formula").toString();

			Iterable<Relationship> relationships = metaboliteNode.getRelationships(RelationshipType.withName("has_crossreference_to"));

			for(Relationship rel : relationships) {

				Set<Node> setRelatedNodes = Set.of(rel.getStartNode(), rel.getEndNode());

				for(Node relatedNode : setRelatedNodes) {

//					System.out.println(relatedNode.getAllProperties());

					if(relatedNode.hasProperty("entry")) {

						String entryID = relatedNode.getProperty("entry").toString();

						MetaboliteMajorLabel label = getMetaboliteLabel(relatedNode);

						//						System.out.println(label + "\t" + entryID);

						String formula2 = "";

						if(relatedNode.hasProperty("formula"))
							formula2 =  relatedNode.getProperty("formula").toString();

						if(label.equals(MetaboliteMajorLabel.ModelSeed) && counts.containsKey(label)) {

							for(Long previousID : counts.get(label)) {

								if(!id.equals(previousID)) {

									Node previousNode = service.getNodeById(previousID);

									if(previousNode.hasProperty("formula")) {
										String previousFormula = previousNode.getProperty("formula").toString();

										if(formula2.equals(previousFormula)) {
											
											Integer currentEntryID = FetchCompoundsByName.getIDNumberFormat(entryID, label);

											Integer previousEntryID = FetchCompoundsByName.getIDNumberFormat(previousNode.getProperty("entry").toString(), label);

											if(currentEntryID > previousEntryID) {
												
												forReplacement.put(entryID, previousNode.getProperty("entry").toString());
											}
										}

									}
								}
							}
						}


						if(DEFAULT_DATABASES.contains(label) && saveEntry(entryID, name, formula, relatedNode, label, references, formulas)) {
							references.put(label, entryID);

							formulas.put(label.toString().concat(entryID), formula2);
						}
					}
				}
			}
		}
		else
			logger.warn("Conflicts while searching metabolites for metabolite alias: {}. Consider creating an exception!", name);

		if(!references.containsKey(MetaboliteMajorLabel.MetaCyc) && !name.isEmpty() && metaboliteNode != null) {

			String metacycEntry = getMetacycEntry(metaboliteNode, counts);

			if(metacycEntry != null)
				references.put(MetaboliteMajorLabel.MetaCyc, metacycEntry);

			//			String idForSearch = null;
			//
			//			if(references.containsKey(MetaboliteMajorLabel.LigandCompound))
			//				idForSearch = references.get(MetaboliteMajorLabel.LigandCompound);
			//
			//			if(idForSearch != null) {
			//
			//				String identifier = ChebiAPI.getMetacycIDUsingExternalReference(idForSearch);
			//
			//				if(identifier != null && !identifier.isEmpty()) {
			//
			//					BiodbMetaboliteNode metaboliteNode = service.getMetabolite("META:".concat(identifier), MetaboliteMajorLabel.MetaCyc);
			//
			//					if(metaboliteNode != null)
			//						references.put(MetaboliteMajorLabel.MetaCyc, metaboliteNode.getEntry());
			//				}
			//			}
		}
		
//		if(name.equalsIgnoreCase("Solute"))
//			System.out.println();
//		
//		if(Utilities.processBiosynthName(name).equalsIgnoreCase("Solute"))
//			System.out.println();
		
		compounds.put(Utilities.processBiosynthName(name), references);

		namesWithoutSigns.put(name, name.replaceAll("[^A-Za-z0-9]", ""));
		namesLowerCase.put(name, name.toLowerCase());
		namesLowerCaseWithoutSigns.put(name, name.replaceAll("[^A-Za-z0-9]", "").toLowerCase());

	}

	/**
	 * @param metaboliteNode
	 * @param counts
	 * @return
	 */
	private String getMetacycEntry(Node metaboliteNode, Map<MetaboliteMajorLabel, Set<Long>> counts) {

		MetaboliteMajorLabel label = null;

		if(metaboliteNode.hasProperty("major_label"))
			label = getMetaboliteLabel(metaboliteNode);

		if(counts.containsKey(MetaboliteMajorLabel.MetaCyc)) {

			return getMetacycEntryAux(counts);
		}

		if((label.equals(MetaboliteMajorLabel.LigandCompound) && counts.containsKey(MetaboliteMajorLabel.ModelSeed) || 
				label.equals(MetaboliteMajorLabel.ModelSeed) && counts.containsKey(MetaboliteMajorLabel.LigandCompound))
				&& !counts.containsKey(MetaboliteMajorLabel.MetaCyc)) {

			MetaboliteMajorLabel targetLabel = MetaboliteMajorLabel.ModelSeed;

			if(label.equals(MetaboliteMajorLabel.LigandCompound))
				targetLabel = MetaboliteMajorLabel.LigandCompound;

			String bestEntry = "";

			if(counts.get(targetLabel).size() > 1) {

				Set<String> referees = new HashSet<>();
				Integer min = 99999999;

				for(Long l : counts.get(targetLabel)) {

					Node relatedNode = service.getNodeById(l);

					String entry = relatedNode.getProperty("entry").toString();

					Integer val = getIDNumberFormat(entry, targetLabel);

					if(val < min) {
						bestEntry = entry;
						min = val;
					}
					referees.add(entry);
				}

				if(targetLabel.equals(MetaboliteMajorLabel.ModelSeed)) {
					referees.remove(bestEntry);
					for(String r : referees)
						forReplacement.put(r, bestEntry);
				}
			}
			else {
				for(Long l : counts.get(targetLabel)) {
					Node relatedNode = service.getNodeById(l);
					bestEntry = relatedNode.getProperty("entry").toString();
				}
			}

			BiodbMetaboliteNode relatedNode = service.getMetabolite(bestEntry, targetLabel);

			Iterable<Relationship> relationships = relatedNode.getRelationships(RelationshipType.withName("has_crossreference_to"));

			for(Relationship rel : relationships) {

				Set<Node> setRelatedNodes = Set.of(rel.getStartNode(), rel.getEndNode());

				for(Node relatedNode2 : setRelatedNodes) {

					if(relatedNode2.hasProperty("entry")) {

						MetaboliteMajorLabel metaboliteLabel = getMetaboliteLabel(relatedNode2);

						Set<Long> set = new HashSet<>();
						if(counts.containsKey(metaboliteLabel))
							set = counts.get(metaboliteLabel);

						set.add(metaboliteNode.getId());
						counts.put(metaboliteLabel, set);
					}
				}
			}
		}

		if(counts.containsKey(MetaboliteMajorLabel.MetaCyc)) {

			return getMetacycEntryAux(counts);
		}

		return null;
	}

	private String getMetacycEntryAux(Map<MetaboliteMajorLabel, Set<Long>> counts) {

		Long id = null;

		for(Long s : counts.get(MetaboliteMajorLabel.MetaCyc))
			id = s;

		if(id != null) {
			Node metaboliteNode = service.getNodeById(id);

			return metaboliteNode.getProperty("entry").toString();
		}
		return null;
	}

	private boolean saveEntry(String entryID, String name, String correctFormula, Node relatedNode, MetaboliteMajorLabel label,
			Map<MetaboliteMajorLabel, String> references, Map<String, String> formulas) {

		if(!references.containsKey(label))
			return true;

		if(references.containsKey(label) && !references.get(label).equals(entryID)) {

			String currentFormula = "";
			String previousFormula = formulas.get(label.toString().concat(references.get(label)));

			if(relatedNode.hasProperty("formula"))
				currentFormula = relatedNode.getProperty("formula").toString();

			if(label.equals(MetaboliteMajorLabel.ModelSeed) || label.equals(MetaboliteMajorLabel.LigandCompound) || label.equals(MetaboliteMajorLabel.LigandGlycan)) {

				try {

					Integer currentID = getIDNumberFormat(entryID, label);

					Integer previousID = getIDNumberFormat(references.get(label), label);

					if(currentFormula.equals(correctFormula) && previousFormula.equals(correctFormula)) {
						if(previousID > currentID) {
							forReplacement.put(references.get(label), entryID);
							return true;
						}
					}
					else if(currentFormula.equals(correctFormula) && !previousFormula.equals(correctFormula))
						return true;

				} 
				catch (NumberFormatException e) {
					e.printStackTrace();
				}
			}
			else {

				if(currentFormula.equals(correctFormula) && !previousFormula.equals(correctFormula))
					return true;
				else {

					String msg = "Entry already exists for metabolite alias " + name + " with label " + label + "! Saved entry: " + references.get(label) +
							"! Ignored entry: " + entryID + "!!";

					logger.warn(msg);
				}
			}	
		}

		return false;

	}

	public static Integer getIDNumberFormat(String id, MetaboliteMajorLabel label) {

		try {
			String regex = "cpd";

			if(label.equals(MetaboliteMajorLabel.LigandCompound))
				regex = "^[CDG]";
			else if(label.equals(MetaboliteMajorLabel.LigandGlycan))
				regex = "^[CDG]";

			return Integer.valueOf(id.replaceAll(regex, ""));
		} 
		catch (NumberFormatException e) {

			System.out.println(id + "\t" + label);

			e.printStackTrace();
		}

		return 999999;
	}


	private Long selectBestNode(Map<MetaboliteMajorLabel, Set<Long>> counts) {

		Long id = null;

		//		for(MetaboliteMajorLabel l : counts.keySet()) {
		//			
		//			for(Long id2 : counts.get(l))
		//				System.out.println(service.getNodeById(id2).getAllProperties());
		//		}

		if(counts.containsKey(MetaboliteMajorLabel.ModelSeed) && counts.get(MetaboliteMajorLabel.ModelSeed).size() > 1) {

			//			System.out.println(counts);

			String bestEntry = null;
			Set<String> referees = new HashSet<>();
			Integer min = 99999999;

			for(Long l : counts.get(MetaboliteMajorLabel.ModelSeed)) {

				Node relatedNode = service.getNodeById(l);

				String entry = relatedNode.getProperty("entry").toString();

				Integer val = getIDNumberFormat(entry, MetaboliteMajorLabel.ModelSeed);

				if(val < min) {
					bestEntry = entry;
					min = val;
				}

				referees.add(entry);
			}

			if(bestEntry != null) {
				referees.remove(bestEntry);
				for(String r : referees)
					forReplacement.put(r, bestEntry);
			}
		}

		if(counts.containsKey(MetaboliteMajorLabel.LigandCompound) && counts.get(MetaboliteMajorLabel.LigandCompound).size() == 1) 	//verify relations created between modelseed and bigg/bigg2/biggmetabolite
			id = counts.get(MetaboliteMajorLabel.LigandCompound).iterator().next();

		else if(counts.containsKey(MetaboliteMajorLabel.LigandGlycan) && counts.get(MetaboliteMajorLabel.LigandGlycan).size() == 1) 
			id = counts.get(MetaboliteMajorLabel.LigandGlycan).iterator().next();

		else if(counts.containsKey(MetaboliteMajorLabel.ModelSeed) && counts.get(MetaboliteMajorLabel.ModelSeed).size() == 1) 
			id = counts.get(MetaboliteMajorLabel.ModelSeed).iterator().next();

		else if(counts.containsKey(MetaboliteMajorLabel.BiGG) && counts.get(MetaboliteMajorLabel.BiGG).size() == 1) 
			id = counts.get(MetaboliteMajorLabel.BiGG).iterator().next();

		else if(counts.containsKey(MetaboliteMajorLabel.BiGGMetabolite) && counts.get(MetaboliteMajorLabel.BiGGMetabolite).size() == 1) 
			id = counts.get(MetaboliteMajorLabel.BiGGMetabolite).iterator().next();

		else if(counts.containsKey(MetaboliteMajorLabel.MetaCyc) && counts.get(MetaboliteMajorLabel.MetaCyc).size() == 1) 
			id = counts.get(MetaboliteMajorLabel.MetaCyc).iterator().next();

		else if(counts.containsKey(MetaboliteMajorLabel.EcoCyc) && counts.get(MetaboliteMajorLabel.EcoCyc).size() == 1) 
			id = counts.get(MetaboliteMajorLabel.EcoCyc).iterator().next();		

		return id;

	}

	private static MetaboliteMajorLabel getMetaboliteLabel(Node node) {

		if(node.getProperty("entry").toString().contains("ECOLI:"))
			return MetaboliteMajorLabel.EcoCyc;
		else
			return MetaboliteMajorLabel.valueOf(node.getProperty("major_label").toString());

	}

	 /**
	 * Get all metabolite' synonyms in the correct format.
	 * 
	 * @param node
	 * @param nodeProperties
	 * @return
	 */
	public static Set<String> getSynonyms(Node node, Map<String, Object> nodeProperties, BiodbGraphDatabaseService service){

		//		if(node.getAllProperties().get("entry").equals("META:CU+2"))
		//			System.out.println("AQUIIII");
		//		else
		//			System.out.println("not found!!!!!!!!");

		String names ="";
		Set<String> synonyms = new HashSet<>();

		if(node.hasProperty("name")) {
			String res = (String) nodeProperties.get("name");
			if(!res.contains("&"))									//correct this (metacyc names)
				names = names.concat(res).concat(";");
		}

		if(node.hasProperty("frameId")) {		//metacyc names are sometimes correct in frameID, not in names because of the &alpha, &beta, &delta, &gamma...

			//			if(!names.isEmpty()) 
			//				names = names.concat(";");

			names = names.concat((String) nodeProperties.get("frameId")).concat(";");
		}

		for(Relationship rel : node.getRelationships(RelationshipType.withName("has_name"))) {

			if(rel.hasProperty("DCS-original")) {

				if(!rel.getProperty("DCS-original").toString().contains("&"))					//correct this (metacyc names)
					names = names.concat(rel.getProperty("DCS-original").toString()).concat(";");
			}

			Relationship rels = service.getRelationshipById(rel.getId());

			for(Node n : rels.getNodes())

				if(n.getId() != node.getId()) {
					if(n.getProperty("major_label" ).equals("Name"))
						names = names.concat(n.getProperty("key").toString()).concat(";");
				}
			//					System.out.println(n.getAllProperties());

		}

		for(String name : names.split(";")) {

			if(!name.isEmpty()) {
				synonyms.add(Utilities.processBiosynthName(name));
			}
		}

		return synonyms;
	}

	public BiosynthMetabolites getResults() {

		return new BiosynthMetabolites(compounds, namesLowerCaseWithoutSigns, namesWithoutSigns, namesLowerCase);
	}

}
