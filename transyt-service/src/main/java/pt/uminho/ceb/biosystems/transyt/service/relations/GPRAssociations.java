package pt.uminho.ceb.biosystems.transyt.service.relations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.uminho.ceb.biosystems.merlin.utilities.Pair;
import pt.uminho.ceb.biosystems.merlin.utilities.containers.capsules.AlignmentCapsule;
import pt.uminho.ceb.biosystems.transyt.service.internalDB.WriteByMetabolitesID;
import pt.uminho.ceb.biosystems.transyt.service.neo4jRest.RestNeo4jGraphDatabase;
import pt.uminho.ceb.biosystems.transyt.service.reactions.ProvideTransportReactionsToGenes;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.GeneContainer;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.ReactionContainer;
import pt.uminho.ceb.biosystems.transyt.utilities.enumerators.Enumerators.TypeOfTransporter;
import pt.uminho.ceb.biosystems.transyt.utilities.files.FilesUtils;
import pt.uminho.ceb.biosystems.transyt.utilities.transytUtilities.Properties;

public class GPRAssociations {

	private static final Logger logger = LoggerFactory.getLogger(WriteByMetabolitesID.class);

	public static Map<String, Map<String, String>> getGPR(Map<String, Set<String>> complexesTCDB, 
			Map<String, List<AlignmentCapsule>> blastResults, Map<String, GeneContainer> genesContainers, Properties properties) {

		Map<String, List<AlignmentCapsule>> filteredBlastResults = filterResults(blastResults, genesContainers);

		Map<String, Map<String, Map<String, Double>>> resultsByTCnumber = new HashMap<>();

		for(String gene : filteredBlastResults.keySet()) {

			Map<String, Map<String, Double>> subunits = new HashMap<>();

			for(AlignmentCapsule container : filteredBlastResults.get(gene)) {

				String tcNumber = container.getTcdbID();
				String accession = container.getTarget();

				if(container.getEvalue() <= properties.getLimitEvalueAcceptance() && 
						complexesTCDB.containsKey(tcNumber) && complexesTCDB.get(tcNumber).size() > 1
						&& complexesTCDB.get(tcNumber).contains(accession)) {

					if(!resultsByTCnumber.containsKey(tcNumber))
						resultsByTCnumber.put(tcNumber, new HashMap<>());

					if(!resultsByTCnumber.get(tcNumber).containsKey(accession))
						resultsByTCnumber.get(tcNumber).put(accession, new HashMap<>());

					subunits = resultsByTCnumber.get(tcNumber);

					subunits.get(accession).put(gene, container.getBitScore());

					resultsByTCnumber.put(tcNumber, subunits);
				}
			}
		}

//				for(String key : resultsByTCnumber.keySet())
//					System.out.println(key + "\t" + resultsByTCnumber.get(key));

		Map<String, Map<String, String>> GPR = new HashMap<>();

		for(String tcNumber : new HashSet<>(resultsByTCnumber.keySet())) {
			
			if(resultsByTCnumber.get(tcNumber).keySet().size() == complexesTCDB.get(tcNumber).size()) {

				Map<String, String> assigned = new HashMap<>();
				Map<String, Set<String>> invertedMapping = new HashMap<>();

				for(String acc : resultsByTCnumber.get(tcNumber).keySet()) {

					for(String queryGene : resultsByTCnumber.get(tcNumber).get(acc).keySet()) {

						if(!invertedMapping.containsKey(queryGene))
							invertedMapping.put(queryGene, new HashSet<>());

						invertedMapping.get(queryGene).add(acc);
					}
				}
				
 				if(!resultsByTCnumber.isEmpty() && invertedMapping.size() >= resultsByTCnumber.get(tcNumber).size()) {

					try {
						assigned = findBestSubunits(resultsByTCnumber.get(tcNumber), assigned, invertedMapping);

					} 
					catch (StackOverflowError e) {

						logger.error("A StackOverflowError occurred while searching subunits for tcNumber {}", tcNumber);
					}
				}

				GPR.put(tcNumber, assigned);

			}
		}
		return GPR;
	}


	/**
	 * @param blastResults
	 * @param genesContainers
	 * @return
	 */
	private static Map<String, List<AlignmentCapsule>> filterResults(Map<String, List<AlignmentCapsule>> blastResults,
			Map<String, GeneContainer> genesContainers) {

		Map<String, List<AlignmentCapsule>> filteredBlastResults = new HashMap<>(); 

		for(String queryGene : blastResults.keySet()) {

			for(AlignmentCapsule capsule : blastResults.get(queryGene)) {

				if(genesContainers.get(queryGene).getAnnotatedFamily() != null && 
						capsule.getTcdbID().contains(genesContainers.get(queryGene).getAnnotatedFamily())) {

					if(!filteredBlastResults.containsKey(queryGene))
						filteredBlastResults.put(queryGene, new ArrayList<>());

					filteredBlastResults.get(queryGene).add(capsule);
				}
			}

		}

		return filteredBlastResults;
	}


	/**
	 * @param data
	 * @param assigned
	 * @param invertedMapping
	 * @return
	 */
	private static Map<String, String> findBestSubunits(Map<String, Map<String, Double>> data, Map<String, String> assigned, Map<String, Set<String>> invertedMapping){

		if(assigned.size() == data.size())
			return assigned;

		Set<String> allAccessionRemaining = new HashSet<>(data.keySet());

		if(assigned.size() > 0) {
			for(String queryGene : new HashSet<>(invertedMapping.keySet())) {

				invertedMapping.get(queryGene).removeAll(assigned.keySet());
				//			allAccessionRemaining.addAll(invertedMapping.get(queryGene));
			}
			allAccessionRemaining.removeAll(assigned.values());
		}



		Pair<String, String> pair =  findBestGene(data, allAccessionRemaining, invertedMapping, assigned);

		if(pair == null)
			return null;

		String queryGene = pair.getB();
		String accession = pair.getA();

		//		for(String queryGene : new HashSet<>(invertedMapping.keySet())) {

		if(!assigned.containsValue(queryGene)) {

			if(invertedMapping.get(queryGene).size() == 0) {
				return null;
			}

			else if(invertedMapping.get(queryGene).size() == 1) {

				//					for(String acc : invertedMapping.get(queryGene))
				//						assigned.put(acc, queryGene);

				assigned.put(queryGene, accession);

				//				invertedMapping.keySet().remove(queryGene);

				return findBestSubunits(data, assigned, invertedMapping);
			}
			//				allAccessionRemaining.addAll(invertedMapping.get(queryGene));
		}
		//		}
		//		Pair<String, String> pair =  findBestGene(data, allAccessionRemaining, invertedMapping);

		assigned.put(queryGene, accession);

		return findBestSubunits(data, assigned, invertedMapping);
	}

	/**
	 * @param data
	 * @param accession
	 * @param invertedMapping
	 * @param assigned 
	 * @return
	 */
	private static Pair<String, String> findBestGene(Map<String, Map<String, Double>> data, Set<String> allAccessionRemaining, Map<String, Set<String>> invertedMapping, Map<String, String> assigned){

		boolean found = false;

		double val = 0.0;
		String gene = "";
		String accession = "";

		Set<String> exclude = new HashSet<>();

		for(String acc : allAccessionRemaining) {

			if(data.get(acc).size() == 1)
				return new Pair<String, String>(acc, data.get(acc).keySet().iterator().next());

		}

		while(!found) {

			Map<String, Map<String, Double>> dataClone = new HashMap<>(data);

			val = -1.0;
			gene = "";

			for(String acc : allAccessionRemaining) {
				for(String queryGene : data.get(acc).keySet()) {

					if(!exclude.contains(queryGene)) {

						double currentVal = data.get(acc).get(queryGene);

						if(currentVal > val) {

							gene = queryGene;
							val = currentVal;
							accession = acc;
							found = true;
						}
					}
				}
			}

			if(gene.isEmpty() && accession.isEmpty()) {

				return null;

			}
			Set<String> allGenes = new HashSet<>();

			for(String newAcc : new HashSet<>(dataClone.keySet())) {
				dataClone.get(newAcc).remove(gene);
				allGenes.addAll(dataClone.get(newAcc).keySet());
				allGenes.removeAll(assigned.keySet());
			}

			if(allGenes.size() < allAccessionRemaining.size() - 1) { //-1 because off the own accession
				found = false;		
				exclude.add(gene);			// redo the search for the highest similarit, excluding this gene from further searches
				gene = "";
				accession = "";
			}
		}

		return new Pair<String, String>(accession, gene);
	}


	/**
	 * @param service 
	 * @param container
	 * @param proteinComplexes
	 * @param phosphotransferaseHomologues 
	 * @param hprHomologues 
	 * @param reactionContainersByID
	 * @param finalResults 
	 * @throws Exception 
	 */
	public static Map<String, String> buildGeneRules(String reportPath, RestNeo4jGraphDatabase service, Map<String, Map<String, String>> proteinComplexes,
			Map<String, Map<String, Set<String>>> results, Map<String, Set<String>> complexesTCDB,
			Map<String, Map<String, Double>> hprHomologues, Map<String, Map<String, Double>> phosphotransferaseHomologues, 
			Set<String> alreadyAssigned) throws Exception {

		Map<String, Map<String, String>> initalRules = new HashMap<>();
		Set<String> reactions = new HashSet<>();
		
		Map<String, Set<String>> method2Report = new HashMap<>();

		for(String acc : results.keySet()) {
			
//			if(acc.equals("b1329"))
//				System.out.println();

			String gene = acc;

			for(String tc : results.get(acc).keySet()) {

				reactions.addAll(results.get(acc).get(tc));

				for(String r : results.get(acc).get(tc)) {

					if(initalRules.containsKey(r)) {
						Map<String, String> set = initalRules.get(r);
						set.put(gene, tc);

						initalRules.put(r, set);
					}
					else {
						Map<String, String> set = new HashMap<>();
						set.put(gene, tc);

						initalRules.put(r, set);
					}
				}

			}
		}

		Map<String, String> rules = new HashMap<>();

		//		Map<String, TransytNode> allReactionNodes = service.getAllReactionNodes();

		logger.trace("Searching all tc Numbers by reactions in the database...");

		Map<String, Set<String>> tcNumbersByReaction = service.findAllTcNumbersByReaction();

		logger.trace("Search complete!");

		service.close();

		String hprEnzyme = selectEnzymeWithLowestEvalue(hprHomologues, alreadyAssigned);
		String phosphEnzyme = selectEnzymeWithLowestEvalue(phosphotransferaseHomologues, alreadyAssigned);
		
		for(String reactID : reactions) {
			
//			if(reactID.equals("TO3000376"))
//				System.out.println();

			if(tcNumbersByReaction.containsKey(reactID)) {

				String geneRule = "";

				Set<String> TCs = tcNumbersByReaction.get(reactID);

				List<Set<String>> allRules = new ArrayList<>();

				for(String tcNumber : TCs) {
					
//					if(tcNumber.equals("3.A.1.5.41"))
//						System.out.println();
					
					if(proteinComplexes.containsKey(tcNumber) && proteinComplexes.get(tcNumber) != null) {
						Set<String> complex = proteinComplexes.get(tcNumber).keySet();

						Set<String> rule = new HashSet<>();

						String subGeneRule = "(";

						for(String query : complex) {

							query = query.split("\\s+")[0];

							subGeneRule = subGeneRule.concat(query).concat(" and ");
							rule.add(query);
						}

						boolean save = true;

						for(Set<String> rule2 : new ArrayList<>(allRules)) {

							if(rule.size() == rule2.size() && (rule2.containsAll(rule))) {
								save = false;
							}
						}

						if((allRules.isEmpty() && rule.isEmpty()) || subGeneRule.equals("("))
							save = false;
						else
							allRules.add(rule);

						if(save) {
							if(reactID.matches("T." + TypeOfTransporter.getTransportTypeID(TypeOfTransporter.PEPdependent) + ".+"))
								subGeneRule = addPtsEnzymesToRule(subGeneRule, hprEnzyme, phosphEnzyme);
							
							geneRule = geneRule.concat(subGeneRule).replaceAll("\\sand\\s$", "").concat(")").concat(" or ");
						}
					}
				}

				geneRule = geneRule.replaceAll("\\sor\\s$", "");
				
				boolean ignoreSecondMethodGenes = false; //ignore method-2 GPR if reliable genes assigned
				
				if(!geneRule.isBlank() || (initalRules.get(reactID).values().size() > 1 
						&& initalRules.get(reactID).values().contains(ProvideTransportReactionsToGenes.NO_TCNUMBER_ASSOCIATED))){
					ignoreSecondMethodGenes = true;
				}

				for(Entry<String, String> entry : initalRules.get(reactID).entrySet()) {

					String g = entry.getKey();
					String tcNumber = entry.getValue();
					
					if(!geneRule.contains(g) && complexesTCDB.get(tcNumber) != null && complexesTCDB.get(tcNumber).size() < 2) {	//avoids creating incomplete GPRs

						if(reactID.matches("T." + TypeOfTransporter.getTransportTypeID(TypeOfTransporter.PEPdependent) + ".+"))
							g = addPtsEnzymesToRule(g, hprEnzyme, phosphEnzyme);

						geneRule = geneRule.concat(" or ").concat(g);
					}
					else if(!ignoreSecondMethodGenes && tcNumber.equals(ProvideTransportReactionsToGenes.NO_TCNUMBER_ASSOCIATED)) {
					
						if(!method2Report.containsKey(g))
							method2Report.put(g, new HashSet<>());
						
						method2Report.get(g).add(reactID);
						
						geneRule = geneRule.concat(" or ").concat(g);
					}
				}

				geneRule = geneRule.replaceAll("^\\sor\\s", "");
				
				rules.put(reactID, geneRule);


			}
		}
		
		reportPath = reportPath.concat("scoresMethod2.txt");

		FilesUtils.saveMapInFile3(reportPath, method2Report);

		logger.info("Reactions association report using method-2 saved at: {}", reportPath);
		
		//		WriteExcel.tableToExcel(excel, "C:\\Users\\Davide\\OneDrive - Universidade do Minho\\UMinho\\Tese\\KBase\\Reports\\ecoli_Validation30\\NewGPRs.xlsx");
		return rules;

	}

	/**
	 * @param rule
	 * @param hpr
	 * @param phosphotransferase
	 * @return
	 */
	private static String addPtsEnzymesToRule(String rule, String hprEnzyme, String phosphEnzyme) {

		rule = rule.replaceAll("\\sand\\s$", "");

		if(hprEnzyme != null)
			rule = rule.concat(" and ").concat(hprEnzyme);
		else
			logger.error("Hpr-PTS enzyme is null! Fix this!");

		if(phosphEnzyme != null)
			rule = rule.concat(" and ").concat(phosphEnzyme);
		else
			logger.error("Phosphotransferase enzyme is null! Fix this!");

		return rule;
	}

	/**
	 * This 
	 * 
	 * @param results
	 * @return
	 */
	private static String selectEnzymeWithLowestEvalue(Map<String, Map<String, Double>> results, Set<String> alreadyAssigned) {

		String enzyme = null;
		Double minEvalue = null;

		for(String tc : results.keySet()) {
			for(String geneId : results.get(tc).keySet()) {
				
				if(!alreadyAssigned.contains(geneId)) {

					Double evalue = results.get(tc).get(geneId);
	
					if(minEvalue == null || evalue < minEvalue) {
						minEvalue = evalue;
						enzyme = geneId;
					}
				}
			}
		}

		return enzyme;
	}
}
