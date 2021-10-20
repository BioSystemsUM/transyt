package pt.uminho.ceb.biosystems.transyt.service.reactions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.uminho.ceb.biosystems.transyt.scraper.tcdb.reactionsGenerator.GenerateTransportReactions;
import pt.uminho.ceb.biosystems.transyt.service.containers.BiosynthMetabolites;
import pt.uminho.ceb.biosystems.transyt.service.kbase.ModelSEEDRealatedOperations;
import pt.uminho.ceb.biosystems.transyt.service.relations.MetabolitesChilds;
import pt.uminho.ceb.biosystems.transyt.service.transytDatabase.TransytNeo4jInitializer;
import pt.uminho.ceb.biosystems.transyt.service.utilities.MappingMetabolites;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.BiosynthMetaboliteProperties;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.ReactionContainer;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.TcNumberContainer2;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.TcdbMetabolitesContainer;
import pt.uminho.ceb.biosystems.transyt.utilities.dictionary.Synonyms;
import pt.uminho.ceb.biosystems.transyt.utilities.enumerators.Enumerators.TypeOfTransporter;
import pt.uminho.ceb.biosystems.transyt.utilities.enumerators.MetaboliteReferenceDatabaseEnum;
import pt.uminho.ceb.biosystems.transyt.utilities.files.FilesUtils;
import pt.uminho.ceb.biosystems.transyt.utilities.transytUtilities.DistributionsAlgorithm;
import pt.uminho.ceb.biosystems.transyt.utilities.transytUtilities.Properties;
import pt.uminho.ceb.biosystems.transyt.utilities.transytUtilities.Utilities;
import pt.uminho.sysbio.biosynth.integration.io.dao.neo4j.MetaboliteMajorLabel;
import pt.uminho.sysbio.biosynthframework.BiodbGraphDatabaseService;

public class TransportReactionsBuilder {

	private Map<String, Set<TcNumberContainer2>> newData;
	private Map<String, BiosynthMetaboliteProperties> allData;
	private Map<String, String> element_map = FormulaParser.getSymbolMap();
	private Map<String, String> allExistingReactions; // = FilesUtils.readMapFromFile(FilesUtils.getReactionsIdentifiersFilePath());
	private Map<String, Set<String>> missingChilds = FilesUtils.readDictionary(FilesUtils.getChildsRelationshipsMissingFilePath(), "\\$");
	//	private Map<String, String> newReactionIDs;
	private Map<String, Integer> generationsLimit;
	private Map<String, String> mappingModelSeed;
	private Map<String, String> mappingBiGG;
	private Map<String, String> mappingKEGG;
	private Map<String, String> mappingMetaCyc;
	private Map<String, Map<MetaboliteMajorLabel, String>> allMetabolitesIDs;
	private MetaboliteMajorLabel defaultLabel;

	private String reaction;
	private String reactionBase;
	private String reactionMetaCyc;
	private String reactionModelSeed;
	private String reactionBiGG;
	private String reactionKEGG;
	private String formulasReaction;

	private Properties properties;

	private boolean go;
	private boolean isChild;

	public static final List<String> ELEMENTS_EXCEPTIONS = List.of("Ac", "Ar", "As", "Au", "Ba", "Be",
			"Ca", "Cd", "Ce", "Cm", "Cu", "Db", "Es", "Eu", "He", "Hg", "Fe", "Fm",
			"Li", "Lu", "Mn", "Mo", "Na", "Nb", "Nd", "Ne", "Pa", "Pd", "Pm",
			"Pu", "Pt", "Ru", "Sb", "Se", "Sg", "Sm", "Tb", "Tc", "Th", "Ti", "Uub", "Uun", "Uuu", "Yb", "Zn");

	private static final Logger logger = LoggerFactory.getLogger(TransportReactionsBuilder.class);

	public TransportReactionsBuilder(Map<String, Set<TcNumberContainer2>> reactionsData, BiodbGraphDatabaseService service, 
			Map<String, BiosynthMetaboliteProperties> allData, BiosynthMetabolites biosynthMetabolites, Properties properties, 
			boolean useCache) {

		this.allExistingReactions = TransytNeo4jInitializer.getAllReactionsIdentifiersInReferenceDatabase(properties);

		this.allData = allData;
		this.newData = new HashMap<>();
		//		this.newReactionIDs = new HashMap<>();

		//		this.tcdbMetabolitesIDs = tcdbMetabolitesIDs;
		this.allMetabolitesIDs = biosynthMetabolites.getMetabolitesIDs();

		this.properties = properties;

		//		this.reactionsIDsPath = path.concat(this.reactionsIDsPath);
		//		this.reactionsIDSTranslationPath = path.concat(this.reactionsIDSTranslationPath);

		//		this.reactionsIDsMap = FilesUtils.readMapFromFile(this.reactionsIDsPath);
		//		this.reactionsIDsMapTranslation = FilesUtils.readMapFromFile(reactionsIDSTranslationPath);

		this.generationsLimit = FilesUtils.readGenerationsLimitFile(FilesUtils.getDictionatiesAndConfigurationsDirectory().concat("ChildsLimits.txt"));

		defaultLabel = getDefaultLabel();

		mappingModelSeed = MappingMetabolites.getMapping(defaultLabel, MetaboliteMajorLabel.ModelSeed);
		mappingBiGG = MappingMetabolites.getMapping(defaultLabel, MetaboliteMajorLabel.BiGGMetabolite);
		mappingKEGG = MappingMetabolites.getMapping(defaultLabel, MetaboliteMajorLabel.LigandCompound);
		mappingMetaCyc = MappingMetabolites.getMapping(defaultLabel, MetaboliteMajorLabel.MetaCyc);

		mappingModelSeed.put("META:CU+", "cpd30760"); //apagar depois de corrigir os mapeamentos
		mappingModelSeed.put("META:CPD0-2124", "cpd04761"); //apagar depois de corrigir os mapeamentos
		mappingModelSeed.put("META:CPD-20940", "cpd04819"); //apagar depois de corrigir os mapeamentos
		mappingBiGG.put("cpd12713", "e");	//apagar depois de corrigir os mapeamentos

		generateTransportReactions(reactionsData, biosynthMetabolites, service);
		
		try {
			ModelSEEDRealatedOperations.assingModelSEEDIdentifiersToTranSyTReactions(useCache, this.newData);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//				Map<String, Set<TcNumberContainer2>> reactionsData2 = new HashMap<>();									//for debug purposes
		//				reactionsData2.put("P0A334", reactionsData.get("P0A334"));
		//				generateTransportReactions(tcdbMetabolitesIDs, reactionsData2, forChildsSearch, service);

		//		FilesUtils.saveMapInFile(this.reactionsIDsPath, reactionsIDsMap);
		//		FilesUtils.saveMapInFile(reactionsIDSTranslationPath, reactionsIDsMapTranslation);
		//		FilesUtils.saveMapInFile("C:\\Users\\Davide\\Documents\\InternalDB\\".concat(FilesUtils.generateFileName("reactionsIDS", ".txt")), reactionsIDsMap);	//backup


		//				Map<String, Set<TcNumberContainer2>> reactionsData = new HashMap<>();
		//
		//				reactionsData.put("P29897", reactionsDataaaaaa.get("P29897"));
	}

	private void generateTransportReactions(Map<String, Set<TcNumberContainer2>> reactionsData,
			BiosynthMetabolites biosynthMetabolites, BiodbGraphDatabaseService service) {

		Map<String, String> mainMetabolitesMap = new HashMap<>();
		Map<String, Set<Long>> originalMetabolitesChilds = new HashMap<>();
		Map<String, String> metabolites = new HashMap<>();
		Map<String, String> formulas = new HashMap<>();

		IdentifyReactionsMetabolites metabolitesIdentification =  new IdentifyReactionsMetabolites(reactionsData, biosynthMetabolites, service);

		Map<String, Map<String, MetaboliteMajorLabel>> tcdbMetabolitesIDs = metabolitesIdentification.getTcdbMetabolitesIDs();

		Map<String, String[]> forChildsSearch = metabolitesIdentification.getforChildsSearch();

		Map<String, String> metabolitesByOriginalName = metabolitesIdentification.getMetabolitesByOriginalName();

		Synonyms dictionary = new Synonyms();

		int reactionsCounter = 0;

		//		int i = 0;

		for(String accession : reactionsData.keySet()) {

			Set<TcNumberContainer2> setTcContainers = new HashSet<>();

			for(TcNumberContainer2 tcContainer : reactionsData.get(accession)) {

				TcNumberContainer2 newTcContainer = new TcNumberContainer2(tcContainer.getTcNumber());

				Map<String, Set<Long>> childs = new HashMap<>();	//this can not be cached because of the dinamic limits
				Map<String, String> childNames = new HashMap<>();

				for(Integer key : tcContainer.getAllReactionsIds()) {

					try {
						Set<String> mainMetabolites = new HashSet<>();  //No childs

						Map<String, String> reverseKeys = new HashMap<>();

						List<Set<String>> sets = new ArrayList<>();

						ReactionContainer reactionContainer = tcContainer.getReactionContainer(key);

						Map<String, Set<String>> metabolitesForReplacement = IdentifyReactionsMetabolites.getMetabolitesToBeReplaced(reactionContainer.getReaction(), 
								dictionary, reactionContainer.isCombineSameMetabolite());

						//						System.out.println(metabolitesForReplacement);

						Set<String> originalMetabolites = IdentifyReactionsMetabolites.getMetabolitesFromReaction(reactionContainer.getOriginalReaction(), dictionary, false);

						if(!originalMetabolites.isEmpty() && !(originalMetabolites.size() == 1 && originalMetabolites.contains(BiosynthMetaboliteProperties.NONE))) {
							for(String originalM : new HashSet<>(originalMetabolites)) {	// this step is important for the common ontologies filter

								if(metabolitesByOriginalName.containsKey(originalM)) {
									originalMetabolites.remove(originalM);
									originalM = metabolitesByOriginalName.get(originalM);
									originalMetabolites.add(originalM);
								}

								String entryID = "";
								MetaboliteMajorLabel label = null;

								if(!originalMetabolitesChilds.containsKey(originalM) && tcdbMetabolitesIDs.containsKey(originalM)) {

									for(String id : tcdbMetabolitesIDs.get(originalM).keySet()) {		//this map will always have only one entry

										label = tcdbMetabolitesIDs.get(originalM).get(id);
										entryID = id;
									}

									if(!label.equals(MetaboliteMajorLabel.MetaCyc) && forChildsSearch.containsKey(originalM)) {

										entryID = forChildsSearch.get(originalM)[0];
										label = MetaboliteMajorLabel.valueOf(forChildsSearch.get(originalM)[1]);
									}

									long identifier = MetabolitesChilds.identifyNode(entryID, label, service);
									
									originalMetabolitesChilds.put(originalM, MetabolitesChilds.getMetaboliteChilds(originalM, -1, identifier, service, this.missingChilds));	//no limits here
									mainMetabolitesMap.put(originalM, Long.toString(identifier));
								}
							}
						}
						for(String metabolite : metabolitesForReplacement.keySet()) {

							int childGenerationLimit = MetabolitesChilds.ALL_CODE;

							if(generationsLimit.containsKey(metabolite))
								childGenerationLimit = generationsLimit.get(metabolite);
							else if(reactionContainer.getConfidenceLevel().equals(GenerateTransportReactions.METACYC_CONFIDENCE_LEVEL) 
									&& !reactionContainer.getTransportType().equals(TypeOfTransporter.RedoxNADH))
								childGenerationLimit = 2;
							
							if(tcdbMetabolitesIDs.containsKey(metabolite)) {
								
								String entryID = "";
								MetaboliteMajorLabel label = null;

								for(String id : tcdbMetabolitesIDs.get(metabolite).keySet()) {		//this map will always have only one entry

									label = tcdbMetabolitesIDs.get(metabolite).get(id);
									entryID = id;
								}

								metabolites.put(metabolite, entryID.concat("=").concat(label.toString()));

								if(allData.containsKey(entryID) && !formulas.containsKey(entryID))
									formulas.put(entryID, allData.get(entryID).getFormula());

								if(!childs.containsKey(metabolite)) {

									if(!label.equals(MetaboliteMajorLabel.MetaCyc) && forChildsSearch.containsKey(metabolite)) {

										entryID = forChildsSearch.get(metabolite)[0];
										label = MetaboliteMajorLabel.valueOf(forChildsSearch.get(metabolite)[1]);
									}

									long identifier = MetabolitesChilds.identifyNode(entryID, label, service);

									childs.put(metabolite, MetabolitesChilds.getMetaboliteChilds(metabolite, childGenerationLimit, identifier, service, this.missingChilds));	
									mainMetabolitesMap.put(metabolite, Long.toString(identifier));
								}

								if(childs.containsKey(metabolite)) {

									Set<String> childsAsString = new HashSet<>();
									for(Long childID : childs.get(metabolite)) {

										String childStr = childID.toString();

										reverseKeys.put(childStr, metabolite);

										if(!childNames.containsKey(metabolite)) {

											String[] properties = getMetaboliteProperties(childStr, defaultLabel, service);

											String childName = dictionary.getSynonym(properties[0]);

											if(childName == null && !properties[0].equalsIgnoreCase("null"))
												childName = properties[0];
											else if(childName == null)
												childName = properties[1].replace("META:", "").replace("ECOLI:", "");   

											String childEntryID = properties[1];
											String childLabel = properties[2];

											childNames.put(childStr, childName);
											metabolites.put(childStr, childEntryID.concat("=").concat(childLabel));

											if(allData.containsKey(childEntryID) && !formulas.containsKey(childEntryID))
												formulas.put(childEntryID, allData.get(childEntryID).getFormula());

										}
										childsAsString.add(childStr);
									}

									sets.add(childsAsString);
								}
								else {							
									metabolites.put(metabolite, metabolite);

									Set<String> met = new HashSet<>();
									met.add(metabolite);

									reverseKeys.put(metabolite, metabolite);

									if(!childNames.containsKey(metabolite))
										childNames.put(metabolite, metabolite);

									sets.add(met);

								}

								mainMetabolites.add(mainMetabolitesMap.get(metabolite));

							}
							else {
								metabolites.put(metabolite, metabolite);

								Set<String> met = new HashSet<>();
								met.add(metabolite);

								reverseKeys.put(metabolite, metabolite);

								if(!childNames.containsKey(metabolite))
									childNames.put(metabolite, metabolite);

								sets.add(met);
							}
						}

						List<Set<String>> allReactions = new ArrayList<>();

						for(Set<String> s : sets)		
							allReactions.add(new HashSet<>(s)); //deep copy

						List<Set<String>> commonOntologyMetabolites = filterMetabolitesByCommonOntology(allReactions, originalMetabolites, originalMetabolitesChilds);

						if(allReactions != null) {

							if(commonOntologyMetabolites != null) {
								List<Set<String>>  distributions = DistributionsAlgorithm.getAllDistributions(commonOntologyMetabolites, reactionContainer.isCombineSameMetabolite());

								Map<ReactionContainer, Boolean>  reactionsGenerated =  generateAllPossibleReactions(distributions, reverseKeys, childNames, metabolites, formulas, 
										reactionContainer, tcContainer.getTcNumber(), metabolitesForReplacement, mainMetabolites, defaultLabel);

								for(Entry<ReactionContainer, Boolean> entry : reactionsGenerated.entrySet()) {
									if(!newTcContainer.getAllReactionTransytIds().contains(entry.getKey().getReactionID())) {
										newTcContainer.addReaction(entry.getKey());

										if(!entry.getValue()) //is child
											newTcContainer.addMainReaction(entry.getKey().getReactionID());

										newTcContainer.addReactionGeneratedByCommonOntology(entry.getKey().getReactionID());   //flag to assing reaction as generated by common ontology
									}
								}

							}
						}
						List<Set<String>> distributions = DistributionsAlgorithm.getAllDistributions(sets, reactionContainer.isCombineSameMetabolite());

						Map<ReactionContainer, Boolean>  reactionsGenerated =  generateAllPossibleReactions(distributions, reverseKeys, childNames, metabolites, formulas, 
								reactionContainer, tcContainer.getTcNumber(), metabolitesForReplacement, mainMetabolites, defaultLabel);


						for(Entry<ReactionContainer, Boolean> entry : reactionsGenerated.entrySet()) {

							if(!newTcContainer.getAllReactionTransytIds().contains(entry.getKey().getReactionID())) {

								newTcContainer.addReaction(entry.getKey());

								if(!entry.getValue()) //is child
									newTcContainer.addMainReaction(entry.getKey().getReactionID());
								
//								if(reactionContainer.getConfidenceLevel().startsWith(GenerateTransportReactions.METACYC_CONFIDENCE_LEVEL))
//									newTcContainer.addReactionGeneratedByCommonOntology(entry.getKey().getReactionID());  
							}
							
							if((reactionContainer.getConfidenceLevel().startsWith(GenerateTransportReactions.METACYC_CONFIDENCE_LEVEL)
									|| entry.getKey().getReactionID().matches("T.[" + TypeOfTransporter.getTransportTypeID(TypeOfTransporter.PEPdependent).toString() + "-9].*"))
									&& !newTcContainer.getGeneratedWithCommonOntology().contains(entry.getKey().getReactionID())) {
								newTcContainer.addReactionGeneratedByCommonOntology(entry.getKey().getReactionID());  //flag to assing metacyc reactions as generated by common ontology 
							}
						}

						reactionsCounter = reactionsCounter + newTcContainer.getAllReactionsIds().size();
					}

					catch (Exception e) {
						logger.error("An error occurred while generating transport reactions for accession ".concat(accession)
								.concat(" with TC number ").concat(tcContainer.getTcNumber()));

						e.printStackTrace();
					}
				}

				setTcContainers.add(newTcContainer);
			}
			newData.put(accession, setTcContainers);
		}

		//		FilesUtils.addMapToFile(FilesUtils.getReactionsIdentifiersFilePath(), this.newReactionIDs);	

		//		logger.info(" All TranSyT reactions generated!!!");				//CHECK THESE COUNTERS
		//		logger.debug("TOTAl of reactions generated (not filtered): {}", reactionsCounter);
		//		logger.debug("TOTAl of unique reactions generated: {}", uniqueReactions.size());
		//		logger.debug("TOTAl of hascodes generated: {}", uniqueReactionsHashCode.size());

	}

	private static List<Set<String>> filterMetabolitesByCommonOntology(List<Set<String>> sets,
			Set<String> originalMetabolites, Map<String, Set<Long>> originalMetabolitesChilds) {

		List<Set<String>> newSet = new ArrayList<>();

		for(Set<String> metabolites : new ArrayList<>(sets)) {

			for(String metabolite : new HashSet<>(metabolites)) {
				
				boolean contains = false;

				for(String key : originalMetabolites) {

					try {
						if(!originalMetabolitesChilds.containsKey(key)) {
							contains = true;		//not restrictive
						}
						else if(originalMetabolitesChilds.get(key).contains(Long.valueOf(metabolite))) {
							contains = true;
							break;
						}
					} 
					catch (NumberFormatException e) { //No childs available for metabolite, if there was a child, its numeric id would reach this position and not its name
						contains = true;
						//						logger.warn("Number Format Exception! No childs available for metabolite: {}", metabolite);
						break;
					}
				}

				if(!contains)
					metabolites.remove(metabolite);
			}

			if(metabolites.isEmpty())
				return null;
			else
				newSet.add(metabolites);
		}

		return newSet;
	}

	/**
	 * @param distributions
	 * @param reverseKeys
	 * @param childNames
	 * @param reactionContainer
	 * @param newTcContainer
	 * @return
	 */
	private Map<ReactionContainer, Boolean> generateAllPossibleReactions(List<Set<String>> distributions, Map<String, String> reverseKeys, Map<String, String> childNames, 
			Map<String, String> metabolites, Map<String, String> formulas, ReactionContainer reactionContainer, String tcNumber,
			Map<String, Set<String>> metabolitesForReplacement, Set<String> mainMetabolites, MetaboliteMajorLabel defaultLabel) {

		Map<ReactionContainer, Boolean> reactionsGenerated = new HashMap<>();

		Map<String, String> metabolitesMapByName = new HashMap<>();
		Map<String, String> metabolitesMapById = new HashMap<>();

		for(Set<String> distribution : distributions) {

			this.isChild = false;

			reaction = reactionContainer.getReaction() + " "; //important for correct following replacements

			reactionBase = reaction;
			reactionMetaCyc = reaction;
			reactionModelSeed = reaction;
			reactionBiGG = reaction;
			reactionKEGG = reaction;
			formulasReaction = reaction;

			this.go = true;

			Set<String> metabolitesToReplace = new HashSet<>();

			for(String id : distribution) {

				if(reactionContainer.isCombineSameMetabolite() && distribution.size() < 2)
					this.go = false;
				else if(go)
					executeSubstitutions(id, metabolitesToReplace, reactionContainer, distribution, 
							childNames, metabolites, metabolitesForReplacement, metabolitesMapById, 
							metabolitesMapByName, reverseKeys, mainMetabolites, formulas);

			}

			String[] res = reaction.replace(ReactionContainer.EXTERIOR_COMPARTMENT_TOKEN, "").replace(ReactionContainer.INTERIOR_COMPARTMENT_TOKEN, "")
					.split(reactionContainer.getDirectionRegex());

			if(this.go)
				this.go = areCompoundsNotRepeated(res[0]) && areCompoundsNotRepeated(res[1]); //check if the same metabolite is twice+ in the reaction

			if(this.go && formulasReaction != null) {

				//				System.out.println(reaction);
				//				System.out.println(reactionBase);
				//				System.out.println(formulasReaction);
				//				
				//				System.out.println();

				String[] reactionSplit = reaction.split(reactionContainer.getDirectionRegex());

				String reactant = reactionSplit[0];
				String product = reactionSplit[1];

				String[] reactionIDsSplit = reactionBase.split(reactionContainer.getDirectionRegex());

				String reactantIDs = reactionIDsSplit[0];
				String productIDs = reactionIDsSplit[1];

				//						if(tcNumber.equals("3.A.1.7.1")) {
				//							
				//							System.out.println(reaction);
				//							System.out.println(idsReaction);
				//							System.out.println(formulasReaction);
				//							System.out.println();
				//							
				//							print = true;
				//							
				//						}


				//			if(idsReaction.contains("META:BUTYRIC_ACID=MetaCyc (out) + META:CO-A=MetaCyc (in) + META:ATP=MetaCyc (in) $IRREV$ META:BU")) {
				//				System.out.println(formulasReaction);
				//				print = true;
				//			}

				TypeOfTransporter newTransportType = null;

				//				if(formulasReaction != null) {

				//					System.out.println(formulasReaction);

				if(!reactionContainer.getConfidenceLevel().equals(GenerateTransportReactions.METACYC_CONFIDENCE_LEVEL)) {

					if((reactant.contains("ATP") && product.contains("ADP")) || (reactant.contains("GTP") && product.contains("GDP"))) {

						newTransportType = TypeOfTransporter.BiochemicalATP;

						reactant = correctBalanceReactionATP(reactant, reactantIDs, false, reactionContainer);
						product = correctBalanceReactionATP(product, productIDs, true, reactionContainer);
						
						reactantIDs = correctBalanceReactionIDsATP(reactantIDs, false, reactionContainer);
						productIDs = correctBalanceReactionIDsATP(productIDs, true, reactionContainer);

						String[] formulaSplit = formulasReaction.split(reactionContainer.getDirectionRegex());
						formulasReaction = correctBalanceReactionFormulaATP(formulaSplit[0], reactantIDs, false, reactionContainer).concat(reactionContainer.getDirectionToken())
								.concat(correctBalanceReactionFormulaATP(formulaSplit[1], reactantIDs, true, reactionContainer));
						
					}
					else if(reactant.contains("ATP") && product.contains("AMP") || (reactant.contains("Diphosphate") && product.contains("Phosphate"))) {

//						if(reactant.contains("ATP"))
//							newTransportType = TypeOfTransporter.BiochemicalCoA;

						//						reactant = correctBalanceReactionATP(reactant, reactantIDs, false);
						product = correctBalanceReactionATP(product, productIDs, true, reactionContainer);

						//						reactantIDs = correctBalanceReactionIDsATP(reactantIDs, false);
						productIDs = correctBalanceReactionIDsATP(productIDs, true, reactionContainer);

						String[] formulaSplit = formulasReaction.split(reactionContainer.getDirectionRegex());
						formulasReaction = formulaSplit[0].concat(reactionContainer.getDirectionToken())
								.concat(correctBalanceReactionFormulaATP(formulaSplit[1], reactantIDs, true, reactionContainer));

					}
					else if((reactant.contains("NADH") && reactant.contains("4H+")) && product.contains("NAD+") && product.contains("4H+")) {

						newTransportType = TypeOfTransporter.RedoxNADH;

						reactant = reactant.replace("4H+", "5H+");

						reactantIDs = correctBalanceReactionIDsNAD(reactantIDs, metabolitesMapByName);

						String[] formulaSplit = formulasReaction.split(reactionContainer.getDirectionRegex());
						formulasReaction = formulaSplit[0].replace(" 4H", " 5H").concat(reactionContainer.getDirectionToken()).concat(formulaSplit[1]);

					}
				}
				//					System.out.println("verificador");
				//					System.out.println(formulasReaction);
				//					System.out.println(reactionContainer.getDirectionRegex());
				//					System.out.println();

				boolean print = false;
				
//				if(reactant.contains(" menaquinol-6") && product.contains(" menaquinone-6"))
//					System.out.println();

				if(print) {
					System.out.println(reactant + " <<>> " + product);
					System.out.println(reactantIDs + " <<>> " + productIDs);
					System.out.println(formulasReaction);
				}

				boolean balanced = isEquationBalanced(formulasReaction, reactionContainer.getDirectionRegex(), print);

				//								System.out.println("balanced: " + balanced);
				//								System.out.println(reactant + " <<>> " + product);
				//								System.out.println(reactantIDs + " <<>> " + productIDs);
				//								System.out.println(formulasReaction);
				//								System.out.println(reactionKEGG);
				//								System.out.println();

				boolean sugarPEPcorrect = true;				

				if(balanced && reactionContainer.getTransportType().equals(TypeOfTransporter.PEPdependent)
					&& !reactionContainer.getConfidenceLevel().equals(GenerateTransportReactions.METACYC_CONFIDENCE_LEVEL)) {
					sugarPEPcorrect = checkPEPReactions(reactant, product, reactionContainer.getDirectionRegex());
				}

				if(balanced && sugarPEPcorrect) {

					reactantIDs = sortReactantsAndProducts(reactantIDs);
					productIDs = sortReactantsAndProducts(productIDs);

					ReactionContainer newReactContainer = new ReactionContainer(reactant, product, reactionContainer.isReversible()); 

					if(newTransportType != null)
						newReactContainer.setTransportType(newTransportType);
					else
						newReactContainer.setTransportType(reactionContainer.getTransportType());

					newReactContainer.setProperties(reactionContainer.getProperties());
					newReactContainer.setOriginalReaction(reactionContainer.getOriginalReaction());
					newReactContainer.setConfidenceLevel(reactionContainer.getConfidenceLevel());
					newReactContainer.setReactionBase(reactantIDs.concat(" ").concat(reactionContainer.getDirectionToken().concat(" ").concat(productIDs)));

					String auxReaction = newReactContainer.getReactionBase().replaceAll("=".concat(defaultLabel.toString()), "");

					if(reactionBiGG != null) {
						newReactContainer.setReactionBiGG(reactionBiGG);
					}
					else if(defaultLabel.equals(MetaboliteMajorLabel.BiGGMetabolite))
						newReactContainer.setReactionBiGG(auxReaction);

					if(reactionMetaCyc != null) {
						newReactContainer.setReactionMetaCyc(reactionMetaCyc);
					}
					else if(defaultLabel.equals(MetaboliteMajorLabel.MetaCyc))
						newReactContainer.setReactionMetaCyc(auxReaction);

					if(reactionModelSeed != null) {
						newReactContainer.setReactionModelSEED(reactionModelSeed);	
					}
					else if(defaultLabel.equals(MetaboliteMajorLabel.ModelSeed))
						newReactContainer.setReactionModelSEED(auxReaction);

					if(reactionKEGG != null) {
						newReactContainer.setReactionKEGG(reactionKEGG);
					}
					else if(defaultLabel.equals(MetaboliteMajorLabel.LigandCompound))
						newReactContainer.setReactionKEGG(auxReaction);

					//					if(reactionBase.contains("cpd00205") && newReactContainer.getMetabolites().size() == 2)
					//						System.out.println();

					newReactContainer.generateTranSyTID(metabolitesMapById, this.allExistingReactions, tcNumber);

					String metaId = newReactContainer.getMetaReactionID();

					//					if(!reactionsAlreadyGenerated.contains(metaId)) {

					if(this.allExistingReactions.containsKey(metaId)) {
						newReactContainer.setReactionID(this.allExistingReactions.get(metaId));
					}
					else {
						this.allExistingReactions.put(metaId, newReactContainer.getReactionID());
						//						this.newReactionIDs.put(metaId, newReactContainer.getReactionID());
					}

					reactionsGenerated.put(newReactContainer, isChild);
					//					}
					//					System.out.println(newReactContainer.getReactionID());
				}
			}
		}

		return reactionsGenerated;
	}

	public void executeSubstitutions(String id, Set<String> metabolitesToReplace, ReactionContainer reactionContainer, Set<String> distribution,
			Map<String, String> childNames, Map<String, String> metabolites, Map<String, Set<String>> metabolitesForReplacement,
			Map<String, String> metabolitesMapById, Map<String, String> metabolitesMapByName, Map<String, String> reverseKeys,
			Set<String> mainMetabolites, Map<String, String> formulas) {

		if(metabolites.get(id).contains("=") && MetaboliteMajorLabel.valueOf(metabolites.get(id).split("=")[1]).equals(defaultLabel) 
				&& (metabolitesForReplacement.size() == distribution.size() || reactionContainer.isCombineSameMetabolite())) {

			if(!mainMetabolites.contains(id))
				isChild = true;

			//					System.out.println(metabolitesForReplacement);
			//					System.out.println("reverse" + metabolitesForReplacement.get(reverseKeys.get(id)));

			if(metabolitesToReplace.isEmpty()) {
				for(String s : metabolitesForReplacement.get(reverseKeys.get(id))) {
					metabolitesToReplace.add(s);	//deep copy

					if(reactionContainer.isCombineSameMetabolite())
						metabolitesToReplace.add(s.concat(TcdbMetabolitesContainer.SAME_METABOLITE_COMBINATION_SUFFIX));
				}
			}

			String replace = metabolitesToReplace.iterator().next();
			metabolitesToReplace.remove(replace);
			//			for(String replace : metabolitesToReplace) { 

			if(reactionContainer.getTransportType().equals(TypeOfTransporter.PEPdependent)) {   //PEP dependent	

				replace = replace + "\\s+\\(";

				reaction = reaction.replaceAll(replace, childNames.get(id) + " \\(");
				reactionBase = reactionBase.replaceAll(replace, metabolites.get(id) + " \\(");
			}
			else {
				replace = replace + " ";

				reaction = reaction.replace(replace, childNames.get(id) + " ");
				reactionBase = reactionBase.replace(replace, metabolites.get(id) + " ");
			}

			String repl = "";

			String entryID = metabolites.get(id).split("=")[0];
			
			if(mappingModelSeed.containsKey(entryID) && reactionModelSeed != null) {
				repl = mappingModelSeed.get(entryID);

				if(reactionContainer.getTransportType().equals(TypeOfTransporter.PEPdependent))
					reactionModelSeed = reactionModelSeed.replaceAll(replace, repl + " \\(");
				else
					reactionModelSeed = reactionModelSeed.replace(replace, repl + " ");
			}
			else {
				reactionModelSeed = null;
			}

			if(mappingBiGG.containsKey(entryID) && reactionBiGG != null) {
				repl = mappingBiGG.get(entryID);
				if(reactionContainer.getTransportType().equals(TypeOfTransporter.PEPdependent))
					reactionBiGG = reactionBiGG.replaceAll(replace, repl + " \\(");
				else
					reactionBiGG = reactionBiGG.replace(replace, repl+ " ");
			}
			else {
				reactionBiGG = null;
			}

			if(mappingKEGG.containsKey(entryID) && reactionKEGG != null) {
				repl = mappingKEGG.get(entryID);
				if(reactionContainer.getTransportType().equals(TypeOfTransporter.PEPdependent))
					reactionKEGG = reactionKEGG.replaceAll(replace, repl + " \\(");
				else
					reactionKEGG = reactionKEGG.replace(replace, repl+ " ");
			}
			else {
				reactionKEGG = null;
			}

			if(mappingMetaCyc.containsKey(entryID) && reactionMetaCyc != null) {
				repl = mappingMetaCyc.get(entryID);
				if(reactionContainer.getTransportType().equals(TypeOfTransporter.PEPdependent))
					reactionMetaCyc = reactionMetaCyc.replaceAll(replace, repl + " \\(");
				else
					reactionMetaCyc = reactionMetaCyc.replace(replace, repl + " ");
			}
			else {
				reactionMetaCyc = null;
			}

			metabolitesMapByName.put(childNames.get(id), metabolites.get(id));
			metabolitesMapById.put(metabolites.get(id).split("=")[0], childNames.get(id));

			if(formulasReaction != null && formulas.containsKey(entryID) && formulas.get(entryID) != null 
					&& !formulas.get(entryID).equals(BiosynthMetaboliteProperties.NONE)) {

				String previousFormula = formulasReaction;

				if(reactionContainer.getTransportType().equals(TypeOfTransporter.PEPdependent))
					formulasReaction = formulasReaction.replaceAll(replace, 
							formulas.get(entryID).concat("@") + " \\(");	
				else
					formulasReaction = formulasReaction.replace(replace, 
							formulas.get(entryID).concat("@") + " ");	//@ is added to control if replacements are really happening bacause some names might be like the formula

				if(formulasReaction.equals(previousFormula))
					formulasReaction = null;

			}
			else if((reactionContainer.getTransportType().equals(TypeOfTransporter.Light) && 
					childNames.get(id).equalsIgnoreCase(ReactionContainer.LIGHT_NAME)) ||
					childNames.get(id).equalsIgnoreCase(ReactionContainer.ELECTRON_NAME)) {

				formulasReaction = formulasReaction.replace(replace, " ");
			}
			else {
				formulasReaction = null;
			}
			//			}
		}
		else {
			go = false;
		}

		if(go && !reactionContainer.isCombineSameMetabolite() && !metabolitesToReplace.isEmpty())
			executeSubstitutions(id, metabolitesToReplace, reactionContainer, distribution, 
					childNames, metabolites, metabolitesForReplacement, metabolitesMapById, 
					metabolitesMapByName, reverseKeys, mainMetabolites, formulas);
	}

	//	/**
	//	 * @param newReactContainer
	//	 * @return
	//	 */
	//	private String findReactionTranSyTID(ReactionContainer reactContainer) {

	//		String id = "";
	//		String reactionHashCode = String.valueOf(reactContainer.getReactionBase().replaceAll(ReactionContainer.IRREV_TOKEN, ReactionContainer.REV_TOKEN).hashCode());

	//		if(!reactionsIDsMap.containsKey(reactionHashCode)) {

	//			id = reactContainer.generateTranSyTID();

	//			reactionsIDsMap.put(reactionHashCode, id);
	//			reactionsIDsMapTranslation.put(id, reactContainer.getReaction().replaceAll(ReactionContainer.IRREV_TOKEN, ReactionContainer.REV_TOKEN));
	//
	//		}
	//		else {
	//			id = reactionsIDsMap.get(reactionHashCode);
	//		}

	//		id = reactContainer.generateTranSyTID();
	//
	//		if(!reactContainer.isReversible())
	//			id = "i".concat(id);

	//		return id;
	//	}

	private boolean areCompoundsNotRepeated(String string) {
		Map<String, Integer> occurrences = new HashMap<String, Integer>();

		if(string.matches("(?i).*Phosphate.*\\+.*ADP.*\\+.*Phosphate.*")) {	//exception
			return true;
		}

		String[] split = string.split(" \\+ ");

		for (String word : split) {

			word = word.trim();
			Integer oldCount = occurrences.get(word);
			if ( oldCount == null ) {
				oldCount = 0;
			}
			occurrences.put(word, oldCount + 1);

			if(oldCount > 0) {
				return false;
			}
		}

		return true;
	}

	private boolean checkPEPReactions(String reactant, String product, String reactionDirection) {

		String sugar = reactant.split(" \\(out\\)")[0].trim();
		String sugarP	= product.split("\\(in\\) \\+")[0].trim();

		if(sugarP.toLowerCase().contains(sugar.toLowerCase()) && (sugarP.matches("(?i).*-phosphate") || sugarP.matches("(?i).*-\\d-*p")))
			return true;
		else if(this.reactionMetaCyc != null && reactionDirection != null){

			String[] reactantsM = this.reactionMetaCyc.split(reactionDirection);

			return checkPEPReactions(reactantsM[0], reactantsM[1], null);	//repeat with metacyc IDs

		}
		//		else if((sugar.contains("glucose") || sugar.contains("glucopyranose")) 
		//				&& (sugarP.contains("glucose") || sugarP.contains("glucopyranose"))){
		//
		//			if(!sugar.contains("-phosphate") && sugarP.contains("-phosphate"))
		//				return true;
		//		}

		return false;
	}

	/**
	 * Corrects the reversability of the reaction.
	 * 
	 * @param text
	 * @param reactantIDs
	 * @param product
	 * @return
	 */
	private String correctBalanceReactionATP(String text, String reactantIDs, boolean product, ReactionContainer reactionContainer) {

		if(product) {

			if(reactantIDs.contains("=".concat(MetaboliteMajorLabel.MetaCyc.toString())) 
					|| reactantIDs.contains("=".concat(MetaboliteMajorLabel.BiGG.toString()))
					|| reactantIDs.contains("=".concat(MetaboliteMajorLabel.ModelSeed.toString()))) {

				return text.concat(" + proton");
			}
		}
		else if (!reactionContainer.getOriginalReaction().matches(".*NR\\s+\\(.*NMN\\s+\\(.*")){
			return text.concat(" + water");
		}

		return text;
	}

	/**
	 * Corrects the reversability of the reaction.
	 * 
	 * @param text
	 * @param reactantIDs
	 * @param product
	 * @return
	 */
	private String correctBalanceReactionIDsATP(String reactantIDs, boolean product, ReactionContainer reactionContainer) {

		if(product) {

			Map<MetaboliteMajorLabel, String> labels = allMetabolitesIDs.get("H+");

			for(MetaboliteMajorLabel label : labels.keySet()) {

				String id = labels.get(label);

				if(label.equals(defaultLabel) && reactionBase != null) {
					reactionBase = reactionBase.concat(" + ").concat(id).concat("=").concat(defaultLabel.toString());
				}

				if(label.equals(MetaboliteMajorLabel.MetaCyc) && reactionMetaCyc != null) {
					reactionMetaCyc = reactionMetaCyc.concat(" + ").concat(id);
				}
				else if(label.equals(MetaboliteMajorLabel.ModelSeed) && reactionModelSeed != null) {
					reactionModelSeed = reactionModelSeed.concat(" + ").concat(id);
				}
				//				else if(label.equals(MetaboliteMajorLabel.LigandCompound) && reactionKEGG != null) {
				//					reactionKEGG = reactionKEGG.concat(" + ").concat(id);
				//				}
				else if(label.equals(MetaboliteMajorLabel.BiGG) && reactionBiGG != null) {
					reactionBiGG = reactionBiGG.concat(" + ").concat(id);
				}
			}

			if(labels.containsKey(defaultLabel))
				reactantIDs = reactantIDs.concat(" + ").concat(labels.get(defaultLabel)).concat("=").concat(defaultLabel.toString());

		}
		else if(!reactionContainer.getOriginalReaction().matches(".*NR\\s+\\(.*NMN\\s+\\(.*")){


			Map<MetaboliteMajorLabel, String> labels = allMetabolitesIDs.get("WATER");

			for(MetaboliteMajorLabel label : labels.keySet()) {

				String id = labels.get(label);

				if(label.equals(defaultLabel) && reactionBase != null) {
					reactionBase = id.concat("=").concat(defaultLabel.toString()).concat(" + ").concat(reactionBase);
				}

				if(label.equals(MetaboliteMajorLabel.MetaCyc) && reactionMetaCyc != null) {
					reactionMetaCyc = id.concat(" + ").concat(reactionMetaCyc);
				}
				else if(label.equals(MetaboliteMajorLabel.ModelSeed) && reactionModelSeed != null) {
					reactionModelSeed = id.concat(" + ").concat(reactionModelSeed);
				}
				else if(label.equals(MetaboliteMajorLabel.LigandCompound) && reactionKEGG != null) {
					reactionKEGG = id.concat(" + ").concat(reactionKEGG);
				}
				else if(label.equals(MetaboliteMajorLabel.BiGG) && reactionBiGG != null) {
					reactionBiGG = id.concat(" + ").concat(reactionBiGG);
				}

			}

			if(labels.containsKey(defaultLabel))
				reactantIDs = reactantIDs.concat(" + ").concat(labels.get(defaultLabel)).concat("=").concat(defaultLabel.toString());

		}

		return reactantIDs;
	}

	/**
	 * Corrects the reversability of the reaction.
	 * 
	 * @param text
	 * @param reactantIDs
	 * @param product
	 * @return
	 */
	private String correctBalanceReactionIDsNAD(String reactantIDs, Map<String, String> metabolites) {

		String regex = "";

		if(metabolites.containsKey("H+"))
			regex = metabolites.get("H+");

		String baseEntry = "";

		Map<MetaboliteMajorLabel, String> labels = allMetabolitesIDs.get("H+");

		if(labels.containsKey(defaultLabel)) {
			baseEntry = labels.get(defaultLabel);

			if(reactionBase != null) 
				reactionBase = reactionBase.replace("4".concat(regex), "5".concat(regex));

			if(reactionMetaCyc != null)
				reactionMetaCyc = reactionMetaCyc.replace("4".concat(mappingMetaCyc.get(baseEntry)), "5".concat(mappingMetaCyc.get(baseEntry)));

			if(reactionModelSeed != null)
				reactionModelSeed = reactionModelSeed.replace("4".concat(mappingModelSeed.get(baseEntry)), "5".concat(mappingModelSeed.get(baseEntry)));

			if(reactionKEGG != null)
				reactionKEGG = reactionKEGG.replace("4".concat(mappingKEGG.get(baseEntry)), "5".concat(mappingKEGG.get(baseEntry)));

			if(reactionBiGG != null)
				reactionBiGG = reactionBiGG.replace("4".concat(mappingBiGG.get(baseEntry)), "5".concat(mappingBiGG.get(baseEntry)));

		}

		return reactantIDs.replace("4".concat(regex), "5".concat(regex));
	}

	/**
	 * Corrects the reversability of the reaction.
	 * 
	 * @param text
	 * @param reactantIDs
	 * @param product
	 * @return
	 */
	private String correctBalanceReactionFormulaATP(String formula, String reactantIDs, boolean product, ReactionContainer reactionContainer) {

		if(product) {

			if(reactantIDs.contains("=".concat(MetaboliteMajorLabel.MetaCyc.toString())) 
					|| reactantIDs.contains("=".concat(MetaboliteMajorLabel.BiGG.toString()))
					|| reactantIDs.contains("=".concat(MetaboliteMajorLabel.ModelSeed.toString()))) {

				return formula.concat(" + H+ ");
			}
		}
		else if(!reactionContainer.getOriginalReaction().matches(".*NR\\s+\\(.*NMN\\s+\\(.*")){
			return formula.concat(" + H2O ");
		}

		return formula;
	}

	/**
	 * Method to verify if a reaction is balanced
	 * 
	 * @param equation
	 * @return
	 */
	public boolean isEquationBalanced(String equation, String regex, boolean print) {

		try {
			String[] reactant = getMolecules(equation.split(regex)[0]);
			String[] product = getMolecules(equation.split(regex)[1]);

			Map<String, Integer> reactantsCounts = countAtoms(reactant, print);
			Map<String, Integer> productsCounts = countAtoms(product, print);

			if(print) {
				System.out.println(reactantsCounts);
				System.out.println(productsCounts);
				System.out.println();
			}

			if(reactantsCounts.equals(productsCounts))
				return true;
		} 
		catch (Exception e) {
			System.out.println("reaction " + equation);
			//			e.printStackTrace();
		}

		return false;
	}

	/**
	 * @param reactant
	 * @return
	 */
	private Map<String, Integer> countAtoms(String[] molecules, boolean print) {

		Map<String, Integer> countsMap = new HashMap<>();

		for(String molecule : molecules) {

			if(molecule.contains("\\."))
				molecule = molecule.replaceAll("\\.", "").replaceAll("\\s+", "");

			for(String element : ELEMENTS_EXCEPTIONS) {

				if(molecule.contains(element.toUpperCase()))
					molecule = molecule.replace(element.toUpperCase(), element);
			}

			Integer stoichiometry = 1;

			Pattern p;
			Matcher m;

			if(molecule.matches("^(\\d+).+")) {

				p = Pattern.compile("^(\\d+)");
				m = p.matcher(molecule);

				if(m.find()) {

					try {
						stoichiometry = Integer.valueOf(m.group());
					} 
					catch (NumberFormatException e) {
						stoichiometry = 1;
					}
				}

				molecule = molecule.replaceAll("^(\\d+)", "");
			}

			Map<String, Integer> res = FormulaParser.parse(molecule, element_map);

			for(String key : res.keySet()) {

				if(countsMap.containsKey(key)) {
					countsMap.put(key, countsMap.get(key) + (res.get(key) * stoichiometry));
				}
				else {
					countsMap.put(key, (res.get(key) * stoichiometry));
				}
			}
		}

		return countsMap;
	}

	/**
	 * @param text
	 * @return
	 */
	private static String[] getMolecules(String text) {

		String[] text2 = text.replaceAll(ReactionContainer.INTERIOR_COMPARTMENT_TOKEN_REG, "")
				.replaceAll(ReactionContainer.MIDDLE_COMPARTMENT_TOKEN_REG, "")
				.replaceAll(ReactionContainer.EXTERIOR_COMPARTMENT_TOKEN_REG, "").split(" \\+ ");

		for(int i = 0; i < text2.length; i++)				//the method to assess equations balance does not recognize white spaces
			text2[i] = text2[i].replaceAll("\\s+", ""); 

		return text2;
	}

	/**
	 * @param reaction
	 * @return
	 */
	public static String sortReactantsAndProducts(String reaction) {

		String[] reactants = reaction.split(" \\+ ");

		Arrays.sort(reactants);

		String sortedString = "";

		for(int i = 0; i < reactants.length; i++) {

			if(i == reactants.length - 1)
				sortedString = sortedString.concat(reactants[i]);
			else 
				sortedString = sortedString.concat(reactants[i]).concat(" + ");

		}

		return sortedString;
	}



	private String[] getMetaboliteProperties(String id, MetaboliteMajorLabel metaboliteReferenceDatabaseEnum, BiodbGraphDatabaseService service) {

		String[] properties = new String[3];		//name, entry, label

		String label = "";
		String entryID = "";

		Node node = service.getNodeById(Long.valueOf(id));

		//		System.out.println(node.getAllProperties());

		String name = "NAME NOT FOUND";

		if(node.getAllProperties().containsKey("name"))
			name = node.getProperty("name").toString();

		else if(node.getAllProperties().containsKey("frameId"))
			name = node.getProperty("name").toString();

		else
			logger.warn("Name not found for metabolite with properties -> {}", node.getAllProperties());
		//			System.out.println("[WARN] Name not found for metabolite with properties -> " + node.getAllProperties());

		if(node.hasProperty("major_label"))
			label = node.getProperties("major_label").toString().split("=")[1].replaceAll("[{}]", "");

		if(node.hasProperty("entry"))
			entryID = node.getProperties("entry").toString().split("=")[1].replaceAll("[{}]", "");

		properties[0] = Utilities.processBiosynthName(name);
		properties[1] = entryID;
		properties[2] = label;

		if(metaboliteReferenceDatabaseEnum.equals(MetaboliteMajorLabel.ModelSeed) && mappingModelSeed.containsKey(entryID)) {
			properties[1] = mappingModelSeed.get(entryID);
			properties[2] = MetaboliteMajorLabel.ModelSeed.toString();
		}
		else if(metaboliteReferenceDatabaseEnum.equals(MetaboliteMajorLabel.LigandCompound) && mappingKEGG.containsKey(entryID)) {
			properties[1] = mappingKEGG.get(entryID);
			properties[2] = MetaboliteMajorLabel.LigandCompound.toString();
		}
		else if(metaboliteReferenceDatabaseEnum.equals(MetaboliteMajorLabel.BiGGMetabolite) && mappingBiGG.containsKey(entryID)) {
			properties[1] = mappingBiGG.get(entryID);
			properties[2] = MetaboliteMajorLabel.BiGGMetabolite.toString();
		}

		return properties;
	}

	public MetaboliteMajorLabel getDefaultLabel() {

		if(this.properties.getDefaultLabel().equals(MetaboliteReferenceDatabaseEnum.ModelSEED))
			return MetaboliteMajorLabel.ModelSeed;
		else if(this.properties.getDefaultLabel().equals(MetaboliteReferenceDatabaseEnum.KEGG))
			return MetaboliteMajorLabel.LigandCompound;
		else if(this.properties.getDefaultLabel().equals(MetaboliteReferenceDatabaseEnum.BiGG))
			return MetaboliteMajorLabel.BiGGMetabolite;

		return MetaboliteMajorLabel.MetaCyc;

	}


	/**
	 * @return the newData
	 */
	public Map<String, Set<TcNumberContainer2>> getResults() {
		return newData;
	}

}
