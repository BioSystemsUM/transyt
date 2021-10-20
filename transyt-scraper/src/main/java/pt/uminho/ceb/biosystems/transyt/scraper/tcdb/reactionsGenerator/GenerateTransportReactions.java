package pt.uminho.ceb.biosystems.transyt.scraper.tcdb.reactionsGenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.uminho.ceb.biosystems.merlin.transporters.core.utils.Enumerators.TransportType;
import pt.uminho.ceb.biosystems.transyt.scraper.tcdb.tcdbTransportTypesRetriever.FindTransporters;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.ReactionContainer;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.TcNumberContainer;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.TcdbMetabolitesContainer;
import pt.uminho.ceb.biosystems.transyt.utilities.dictionary.Synonyms;
import pt.uminho.ceb.biosystems.transyt.utilities.enumerators.Enumerators.TypeOfTransporter;
import pt.uminho.ceb.biosystems.transyt.utilities.files.FilesUtils;

public class GenerateTransportReactions {

	private static final Set<String> GENERAL_METABOLITES = FilesUtils.readWordsInFile(FilesUtils.getDictionatiesAndConfigurationsDirectory().concat("GeneralMetabolites.txt"));
	private static final Set<String> TWO_METABOLITES_EVIDENCE = FilesUtils.readWordsInFile(FilesUtils.getDictionatiesAndConfigurationsDirectory().concat("TwoMetabolitesEvidence.txt"));
	private static final Map<String, String[]> OXIDASE_PAIRS = FilesUtils.readConjugatePairsFile(FilesUtils.getDictionatiesAndConfigurationsDirectory().concat("Oxidase_pairs.txt"));
	private static final Map<String, String[]> REDUCTASE_PAIRS = FilesUtils.readConjugatePairsFile(FilesUtils.getDictionatiesAndConfigurationsDirectory().concat("Redutase_pairs.txt"));

	public static final String METACYC_CONFIDENCE_LEVEL = "M";

	private static final Logger logger = LoggerFactory.getLogger(GenerateTransportReactions.class);

	/**
	 * Method to generate all possible reactions for each transporter.
	 * 
	 * @param data
	 * @param tcdbMetabolites
	 * @return
	 */
	public static Map<String, Map<String, TcNumberContainer>> generateReactions(Map<String, TcNumberContainer> data, 
			Map<String, Map<String, ReactionContainer>> metaCycData,
			Map<String, TcdbMetabolitesContainer> tcdbMetabolites,
			Map<String, String> proteinFamilyDescription) {

		Map<String, Map<String, TcNumberContainer>> mainMap = new HashMap<>();

		Synonyms dictionary = new Synonyms();

		int noMetabolitesNoReactionsCounter = 0;

		for(String accession : tcdbMetabolites.keySet()) {

			Map<String, TcNumberContainer> newContainers = new HashMap<>();

			TcdbMetabolitesContainer tcdbMetContainer = tcdbMetabolites.get(accession);

			for(String tcNumber : tcdbMetContainer.getTcNumbers()) {

				//				if(tcNumber.contains("2.A.24.2.5"))
				//					System.out.println(tcNumber);

				if(!tcNumber.startsWith("8.")) { //Not added to exclusion configuration file because these systems are needed in the alignments

					TcNumberContainer newTcContainer = new TcNumberContainer();

					String familyTC = null;

					if(data.containsKey(tcNumber) &&
							!data.get(tcNumber).getAllReactionsIds().isEmpty()) { //allows to find specific TC nummber reactions but skips if no reaction is found
						familyTC = tcNumber;
					}
					else {
						familyTC = tcNumber.replaceAll("(\\.\\d+)$", "");

						if(!data.containsKey(familyTC) || (data.containsKey(familyTC) &&
								data.get(familyTC).getAllReactionsIds().isEmpty())) //allows to find subfamily reactions but discard them if no reaction is found
							familyTC = familyTC.replaceAll("(\\.\\d+)$", "");
					}
					if(data.containsKey(familyTC)) {

						try {

							TypeOfTransporter evidence = null;
							Boolean revEvidence = null;

							TcNumberContainer tcNumberContainer = data.get(familyTC).clone();

							String subfamilyDescription = "";

							if(proteinFamilyDescription.containsKey(tcNumber.replaceAll("(\\.\\d+)$", ""))) {

								subfamilyDescription = proteinFamilyDescription.get(tcNumber.replaceAll("(\\.\\d+)$", ""));
								tcNumberContainer.setSubfamilyDescription(subfamilyDescription);
							}

							evidence = checkForEvidenceOfTransport(tcdbMetContainer.getDescription(tcNumber), tcNumber);

							tcNumberContainer.setProteinDescription(tcdbMetContainer.getDescription(tcNumber));

							if(evidence == null && subfamilyDescription != null) {
								evidence = checkForEvidenceOfTransport(subfamilyDescription, tcNumber);
							}

							if(evidence == null && tcNumberContainer.getFamily() != null)
								evidence = checkForEvidenceOfTransport(tcNumberContainer.getFamily(), tcNumber);

							if(evidence == null && tcNumberContainer.getSuperFamily() != null)
								evidence = checkForEvidenceOfTransport(tcNumberContainer.getSuperFamily(), tcNumber);

							newTcContainer.setProteinDescription(tcNumberContainer.getProteinDescription());
							newTcContainer.setSubfamilyDescription(tcNumberContainer.getSubfamilyDescription());
							newTcContainer.setFamily(tcNumberContainer.getFamily());

							tcNumberContainer.filterReactionsNotBelongingToTransportType(evidence);

							boolean metacycContainsMiddleCompartment = false;

							if(metaCycData.containsKey(accession)) {
								for(String rKey : metaCycData.get(accession).keySet()) {
									metacycContainsMiddleCompartment = metaCycData.get(accession).get(rKey)
											.getReaction().contains(ReactionContainer.MIDDLE_COMPARTMENT_TOKEN);
								}
							}

							boolean tcdbInfoReliable = evidence != null || !tcNumberContainer.getReactions().isEmpty();

							//METACYC

							if(metacycContainsMiddleCompartment || !tcdbInfoReliable && metaCycData.containsKey(accession)) {

								boolean containsBiochemical = false;

								Map<String, ReactionContainer> metacycReactions = metaCycData.get(accession);

								for(String rKey : metacycReactions.keySet()) {
									ReactionContainer rContainer = metacycReactions.get(rKey);
									rContainer.setConfidenceLevel(METACYC_CONFIDENCE_LEVEL);
									rContainer.setOriginalReaction(rContainer.getReaction());

									TypeOfTransporter type = null;

									if(rContainer.getMetabolites().size() == 1)
										type = TypeOfTransporter.Uniport;
									else
										type = FindTransporters.findTypeOfTransport2(rContainer, tcNumber);

									//								if(accession.contains("P23895"))
									//									System.out.println(type);

									if(type.equals(TypeOfTransporter.BiochemicalCoA)) {
										String[] reactAux = correctMetaCycCoaReactions(rContainer);
										rContainer.setReactant(reactAux[0]);
										rContainer.setProduct(reactAux[1]);
										containsBiochemical = true;
									}
									else if(type.equals(TypeOfTransporter.Biochemical)){
										containsBiochemical = true;
									}

									rContainer.setTransportType(type);

									revEvidence = rContainer.isReversible();

									if(tcNumber.matches("^[129].*") && (type.equals(TypeOfTransporter.Uniport) ||
											type.equals(TypeOfTransporter.Symport) || type.equals(TypeOfTransporter.Antiport))) {
										evidence = type;
									}
									else if(type.equals(TypeOfTransporter.BiochemicalATP) && !tcNumber.matches("^[389].*")) {
										type = TypeOfTransporter.Default;	//usually are automatic reactions generated without human oversight. Last count was 24
									}


									if(!type.equals(TypeOfTransporter.Default)) {
										metacycContainsMiddleCompartment = rContainer.getReaction().contains(ReactionContainer.MIDDLE_COMPARTMENT_TOKEN);
										newTcContainer.addReaction(rContainer);
									}
								}

								if(containsBiochemical) {
									newTcContainer = filterUnwantedReactions(tcNumber, newTcContainer, TypeOfTransporter.Biochemical);
									evidence = null; //shouldn't be different than null but just to be safe
								}
								else if(evidence != null && newTcContainer.getAllTransportTypesAssociated().contains(evidence))
									newTcContainer = filterUnwantedReactions(tcNumber, newTcContainer, evidence);
							}

							if(tcdbInfoReliable && evidence == null) {
								evidence = getMostBasicTransportType(tcNumberContainer.getAllTransportTypesAssociated());
							}

							// END OF METACYC

							boolean skip = checkIfIgnoreTCDB(metacycContainsMiddleCompartment, tcdbMetContainer.getMetabolites(tcNumber),
									newTcContainer.getAllReactionsIds().isEmpty(), 
									tcNumberContainer.getAllTransportTypesAssociated());

							if(!skip && tcNumberContainer.getAllReactionsIds().size() == 0 && newTcContainer.getAllReactionsIds().isEmpty()) {

								if(evidence == null)
									evidence = TypeOfTransporter.Uniport;

								newTcContainer = correctReaction(tcNumber, revEvidence, "NONE", "C0" , evidence, null , tcdbMetContainer.getMetabolites(tcNumber), newTcContainer);
							}
							else if(!skip){
								for(int id : tcNumberContainer.getAllReactionsIds()) {

									ReactionContainer reactionContainer = tcNumberContainer.getReactionContainer(id);

									if(reactionContainer.getReactants().size() == 1 && reactionContainer.getProducts().size() == 1) //some entries are with the wrong type
										reactionContainer.setTransportType(TypeOfTransporter.Uniport);

									Boolean antiportOrSymport = selectMethodOfMetabolitesDistribution(reactionContainer.getReaction(), reactionContainer.getTransportType(), dictionary);

									//								System.out.println();

									//																if(accession.equalsIgnoreCase("P26287") && tcNumber.equals("3.D.3.5.1")) {
									//								System.out.println("AQUIII" + "\t" + reactionContainer.getReaction());
									//								System.out.println("AQUIII2" + "\t " + antiportOrSymport + " \t" + evidence + "\t" + reactionContainer.getTransportType().toString());
									//																}	

									int numberOfReactions = newTcContainer.getAllReactionsIds().size();

									for(List<String> metabolites : tcdbMetContainer.getMetabolitesCombinations(tcNumber, antiportOrSymport, evidence, accession, GENERAL_METABOLITES)) {

										//									if(accession.equalsIgnoreCase("P45539") && tcNumber.equals("2.A.3.8.17")) {
										//									System.out.println("AQUIII3" + metabolites +  "\t" + reactionContainer.getReaction());
										//									}
										//									if(accession.equals("P45539"))
										//										System.out.println(metabolites);

										//							Set<String> metabolites = tcdbMetContainer.getMetabolites(tcNumber);	
										if(revEvidence == null)
											revEvidence = reactionContainer.isReversible();

										if(evidence == null) {

											if(reactionContainer.isOxidase() || reactionContainer.isReductase())
												newTcContainer = processConjugatePairs(reactionContainer, newTcContainer);

											else if(reactionContainer.getTransportType().equals(TypeOfTransporter.PEPdependent))
												newTcContainer = processPEPReactions(tcNumber+" "+accession, reactionContainer.getReaction(), "PEP-0" , reactionContainer.getProperties(), metabolites, newTcContainer);

											else if(reactionContainer.getTransportType().equals(TypeOfTransporter.Uniport) || reactionContainer.getTransportType().equals(TypeOfTransporter.Symport))
												newTcContainer = processUniportAndSymportReactions(reactionContainer, newTcContainer, metabolites, dictionary);

											else if(!reactionContainer.getTransportType().equals(TypeOfTransporter.Antiport))
												newTcContainer = processBiochemicalReactions(reactionContainer, tcNumberContainer, newTcContainer, metabolites, dictionary, tcNumber);
											else
												newTcContainer = processAntiportReactions(reactionContainer, tcNumberContainer, newTcContainer, metabolites, dictionary);
										}
										else if(evidence.equals(TypeOfTransporter.Redox)
												||  evidence.equals(TypeOfTransporter.RedoxNADH) ||  evidence.equals(TypeOfTransporter.MoreThanOne)) {

											newTcContainer = processAntiportReactions(reactionContainer, tcNumberContainer, newTcContainer, metabolites, dictionary);
										}
										else if(evidence.equals(TypeOfTransporter.Uniport) && reactionContainer.getTransportType().equals(TypeOfTransporter.Uniport)){

											newTcContainer = processUniportAndSymportReactions(reactionContainer, newTcContainer, metabolites, dictionary);
										}
										else if(evidence.equals(TypeOfTransporter.Symport) && reactionContainer.getTransportType().equals(TypeOfTransporter.Symport)){

											newTcContainer = processUniportAndSymportReactions(reactionContainer, newTcContainer, metabolites, dictionary);
										}
										//								else if(evidence.equals(TypeOfTransporter.Antiport) && reactionContainer.getTransportType().equals(TypeOfTransporter.Antiport)){
										else if(evidence.equals(TypeOfTransporter.Antiport) && reactionContainer.getTransportType().equals(TypeOfTransporter.Antiport))

											newTcContainer = processAntiportReactions(reactionContainer, tcNumberContainer, newTcContainer, metabolites, dictionary);

										else if(!evidence.equals(TypeOfTransporter.Uniport) && !evidence.equals(TypeOfTransporter.Symport) && !evidence.equals(TypeOfTransporter.Antiport))

											newTcContainer = processAntiportReactions(reactionContainer, tcNumberContainer, newTcContainer, metabolites, dictionary);

										else if(reactionContainer.getTransportType().equals(TypeOfTransporter.RedoxNADH))

											newTcContainer = processUniportAndSymportReactions(reactionContainer, newTcContainer, metabolites, dictionary);
										//																}
										else if(!tcNumberContainer.evidenceTransportTypeExists(evidence))
											newTcContainer = correctReaction(tcNumber+" "+accession, revEvidence, reactionContainer.getReaction(), "C-1" , evidence, reactionContainer.getProperties(), metabolites, newTcContainer);

										int currentCount = newTcContainer.getAllReactionsIds().size();

										if((numberOfReactions == currentCount) && evidence != null && metabolites.size() == 1) {
											newTcContainer = correctReaction(tcNumber+" "+accession, revEvidence, reactionContainer.getReaction(), "C-2" , evidence, reactionContainer.getProperties(), metabolites, newTcContainer);
										}

										numberOfReactions = newTcContainer.getAllReactionsIds().size();;
									}
								}
							}

							//BEGIN METACYC BASED ON EVIDENCE

							if(tcdbInfoReliable && metaCycData.containsKey(accession)) {

								List<TypeOfTransporter> tcdbTransports = new ArrayList<>(newTcContainer.getAllTransportTypesAssociated());

								if(tcdbTransports.size() == 1 && tcdbTransports.get(0).equals(TypeOfTransporter.BiochemicalATP))
									evidence = TypeOfTransporter.BiochemicalATP;
								else if(tcdbTransports.size() == 1 && tcdbTransports.get(0).equals(TypeOfTransporter.PEPdependent))
									evidence = TypeOfTransporter.PEPdependent;

								Map<String, ReactionContainer> metacycReactions = metaCycData.get(accession);

								newTcContainer = generateMetacyReactionsBasedOnTCDBEvidence(newTcContainer, evidence,
										metacycReactions, tcNumber, revEvidence);  //probably not needed this return because of pointers
							}

							//END METACYC BASED ON EVIDENCE

							if(newTcContainer.getAllReactionsIds().size() == 0 && tcNumberContainer.getAllReactionsIds().size() > 0){
								newTcContainer = correctReaction(tcNumber, revEvidence, "NONE", "C-2" , TypeOfTransporter.Uniport, null , tcdbMetContainer.getMetabolites(tcNumber), newTcContainer);

							}
							else if(tcNumberContainer.getAllReactionsIds().size() == 0 && tcdbMetContainer.getMetabolites(tcNumber).size() > 0){	//keep only non-generic compounds because Metacyc reactions already exist

								List<String> compounds = tcdbMetContainer.getMetabolites(tcNumber);
								compounds.removeAll(GENERAL_METABOLITES);

								TypeOfTransporter transpAux = evidence;

								if(transpAux == null)
									transpAux = new ArrayList<>(newTcContainer.getAllTransportTypesAssociated()).get(0); //get the type used by metacyc. Warning! Sometimes 2 different types might be present

								if(transpAux.equals(TypeOfTransporter.Biochemical) && newTcContainer.getAllTransportTypesAssociated().size() > 1)
									transpAux = new ArrayList<>(newTcContainer.getAllTransportTypesAssociated()).get(1);

								if(!transpAux.equals(TypeOfTransporter.Biochemical))
									newTcContainer = correctReaction(tcNumber, revEvidence, "NONE", "C-3" , transpAux, null , tcdbMetContainer.getMetabolites(tcNumber), newTcContainer);
							}

							if(newTcContainer.getAllReactionsIds().size() == 0) {		//reactions with generic reactions such as 
								noMetabolitesNoReactionsCounter++;					//solute1 + solute2 <-> solute1 + solute2 are also included here because no metabolites where found for them

								if(tcNumberContainer.getFamily() != null)
									newTcContainer.setFamily(tcNumberContainer.getFamily());

								if(tcNumberContainer.getSuperFamily() != null)
									newTcContainer.setSuperFamily(tcNumberContainer.getSuperFamily());
							}	

							newTcContainer = filterUnwantedReactions(tcNumber, newTcContainer, evidence);


							if(tcNumber.matches("^[129].*"))
								newTcContainer = correctDirections(newTcContainer);


							//						Set<TypeOfTransporter> t = new HashSet<>();
							//						for(int ident : newTcContainer.getAllReactionsIds()) {
							//							t.add(newTcContainer.getReactionContainer(ident).getTransportType());
							//						}
							//						
							//						if(t.size() > 1)
							//							System.out.println(tcNumber);

							newContainers.put(tcNumber, newTcContainer);
						} 

						catch (Exception e) {
							e.printStackTrace();
						}
					}
					else
						logger.warn("Missing TC family: {}", familyTC);
				}
			}
			mainMap.put(accession, newContainers);

		}

		logger.info("Number of transporters with no metabolites and no reactions associated: {}", noMetabolitesNoReactionsCounter);

		return mainMap;

	}

	/**
	 * @param newTcContainer
	 * @return
	 */
	private static TcNumberContainer correctDirections(TcNumberContainer newTcContainer) {

		Map<Boolean, Integer> revs = new HashMap<>();
		List<Boolean> meta = new ArrayList<>();

		for(Integer reaction : newTcContainer.getAllReactionsIds()) {

			Boolean isRev = newTcContainer.getReactionContainer(reaction).isReversible();

			if(!revs.containsKey(isRev))
				revs.put(isRev, 1);
			else
				revs.put(isRev, revs.get(isRev)+1);

			if(newTcContainer.getReactionContainer(reaction).getConfidenceLevel().equals(METACYC_CONFIDENCE_LEVEL) && !meta.contains(isRev))
				meta.add(isRev);
		}

		if(revs.size() > 1) {

			Boolean reversibility = null;

			Boolean uptake = isUptake(getProteinDescriptions(newTcContainer)); //just check reversibility, compartments might already be corrected before by validateReactionDirection() method

			if(uptake != null) 
				reversibility = false;
			else if(meta.size() == 1) 
				reversibility = meta.get(0);
			else {

				int maxCount = 0;
				Boolean tempRev = null;


				for(Boolean rev : revs.keySet()) {

					if(revs.get(rev) == maxCount && maxCount > 0) {
						reversibility = true;
					}
					else if(revs.get(rev) > maxCount) {
						maxCount = revs.get(rev);
						tempRev = rev;
					}
				}

				if(reversibility == null)
					reversibility = tempRev;
			}

			for(Integer reaction : newTcContainer.getAllReactionsIds()) {

				newTcContainer.getReactionContainer(reaction).setReversible(reversibility);
			}
		}

		return newTcContainer;
	}

	/**
	 * Generate reactions based on evidence gathered from reliable TCDB reactions
	 * 
	 * @param newTcContainer
	 * @param evidence
	 * @param metacycReactions
	 * @param tcNumber
	 * @param revEvidence
	 * @return
	 */
	private static TcNumberContainer generateMetacyReactionsBasedOnTCDBEvidence(TcNumberContainer newTcContainer, TypeOfTransporter evidence,
			Map<String, ReactionContainer> metacycReactions, String tcNumber, Boolean revEvidence) {

		for(String rKey : metacycReactions.keySet()) {
			ReactionContainer rContainer = metacycReactions.get(rKey);
			rContainer.setConfidenceLevel(METACYC_CONFIDENCE_LEVEL);
			rContainer.setOriginalReaction(rContainer.getReaction());

			TypeOfTransporter type = null;

			if(rContainer.getMetabolites().size() == 1)
				type = TypeOfTransporter.Uniport;
			else
				type = FindTransporters.findTypeOfTransport2(rContainer, tcNumber);

			if(type.equals(TypeOfTransporter.BiochemicalCoA)) {
				String[] reactAux = correctMetaCycCoaReactions(rContainer);
				rContainer.setReactant(reactAux[0]);
				rContainer.setProduct(reactAux[1]);
			}

			rContainer.setTransportType(type);

			if(revEvidence == null)
				revEvidence = rContainer.isReversible();

			else if(evidence != null && !evidence.equals(type) && !type.equals(TypeOfTransporter.Biochemical)) {

				Set<String> metabolitesAux = rContainer.getMetabolites();

				if(evidence != TypeOfTransporter.Uniport)
					metabolitesAux.remove(TcdbMetabolitesContainer.PROTON);

				for(String metabolite : metabolitesAux) {
					rContainer = generateReactionBasedOnEvidence(rContainer.getOriginalReaction(),
							evidence, metabolite, METACYC_CONFIDENCE_LEVEL, rContainer.getProperties(),
							revEvidence, getProteinDescriptions(newTcContainer));

				}
			}
			else if(evidence != null && !evidence.equals(type) && newTcContainer.getAllReactionsIds().size() > 0){
				type = TypeOfTransporter.Default;
			}

			if(!type.equals(TypeOfTransporter.Default)) {
				newTcContainer.addReaction(rContainer);
			}
		}

		return newTcContainer;
	}

	private static TypeOfTransporter getMostBasicTransportType(Set<TypeOfTransporter> types) {

		if(types.size() == 2 && types.contains(TypeOfTransporter.Uniport) && types.contains(TypeOfTransporter.Symport))
			return TypeOfTransporter.Uniport;

		else if(types.size() == 2 && types.contains(TypeOfTransporter.Uniport) && types.contains(TypeOfTransporter.Antiport))
			return TypeOfTransporter.Uniport;

		else if(types.size() == 2 && types.contains(TypeOfTransporter.Symport) && types.contains(TypeOfTransporter.Antiport))
			return TypeOfTransporter.Symport;

		else if(types.size() == 3 && types.contains(TypeOfTransporter.Uniport) && types.contains(TypeOfTransporter.Symport) 
				&& types.contains(TypeOfTransporter.Antiport))
			return TypeOfTransporter.Uniport;

		return null;
	}

	/**
	 * @param rContainer
	 * @return
	 */
	private static String[] correctMetaCycCoaReactions(ReactionContainer rContainer) {

		String[] res = new String[2];

		String reactant = rContainer.getReactant();
		String product = rContainer.getProduct();

		String[] aux = reactant.split("\\s+\\+\\s+");

		for(String mAux : aux) {

			mAux = mAux.trim();

			if(!mAux.equalsIgnoreCase("META:ATP") && !mAux.equalsIgnoreCase("META:CO-A"))
				reactant = reactant.replace(mAux, mAux + " " + ReactionContainer.EXTERIOR_COMPARTMENT_TOKEN);
			else
				reactant = reactant.replace(mAux, mAux + " " + ReactionContainer.INTERIOR_COMPARTMENT_TOKEN);
		}

		aux = product.split("\\s+\\+\\s+");

		for(String mAux : aux) {

			mAux = mAux.trim();
			product = product.replace(mAux, mAux + " " + ReactionContainer.INTERIOR_COMPARTMENT_TOKEN);
		}

		res[0] = reactant;
		res[1] = product + " + META:PROTON (in)";

		return res;
	}

	/**
	 * Method to assess if TCDB reactions should be ignored.
	 * 
	 * @param metabolites
	 * @param currentContainerIsEmpty
	 * @return
	 */
	private static boolean checkIfIgnoreTCDB(boolean containsMiddleCompartment, List<String> metabolites, boolean currentContainerIsEmpty, Set<TypeOfTransporter> types) {

		if(containsMiddleCompartment)
			return true;

		if(types.contains(TypeOfTransporter.BiochemicalCoA))
			return false;

		boolean allIons = true;

		for(String metabolite : metabolites) {

			if(!metabolite.matches(".+[//+//-]"))
				allIons = false;
		}

		if(!currentContainerIsEmpty) {

			if(metabolites.isEmpty())
				return true;

			for(String metabolite : metabolites) {
				if(GENERAL_METABOLITES.contains(metabolite)) {
					return true;
				}
			}
		}

		return false;
	}

	private static TcNumberContainer processBiochemicalReactions(ReactionContainer reactionContainer, TcNumberContainer tcContainer,
			TcNumberContainer newTcContainer, List<String> metabolites, Synonyms dictionary, String tc) {

		//		Set<String> preReactions = new HashSet<>();
		//		Set<String> reactions = new HashSet<>();

		//		if(tc.equals("3.A.1.7.5 O51235")) {
		//
		//			System.out.println("entrou!!!");
		//		}

		String originalReaction = reactionContainer.getReaction();

		//		if(tc.equals("3.A.1.7.5 O51235")) {
		//
		//			System.out.println(originalReaction);
		//		}

		List<ReactionContainer> tempListRepeated = new ArrayList<>();
		List<ReactionContainer> tempListNew = new ArrayList<>();

		String[] aux = correctMetabolitesNames(originalReaction, reactionContainer.getReactant(), reactionContainer.getProduct());

		originalReaction = aux[0];
		String reactant = aux[1];
		String product = aux[2];


		List<String> reactants = getAllPossibleReactantsorProducts(reactant);
		List<String> products = getAllPossibleReactantsorProducts(product);

		Map<Integer, Integer> positions = associateReactantsToProducts(reactants, products);

		for(int key : positions.keySet()) {

			TypeOfTransporter type = reactionContainer.getTransportType();

			//			for(String metabolite : metabolites) {

			boolean notReplaced = false;

			String newReactant = "";
			String newProduct = "";

			if(type.equals(TypeOfTransporter.BiochemicalCoA) && metabolites.size() == 1) { //brute force

				String m = metabolites.get(0);
				newReactant = reactants.get(key).replace("Fatty acid", m);

				m.replaceAll("s$", "");

				if(m.endsWith("ate"))
					m = m.replace("ate$", "oyl-CoA");
				else if(m.endsWith("ne"))
					m = m.replaceAll("ne$", "nyl-CoA");
				else if(m.endsWith("id"))
					m = m.replaceAll("id", "yl-CoA");

				newProduct = products.get(positions.get(key)).replace("Fatty acyl-CoA", m);

			}
			else {
				newReactant = replaceGenericMetabolite(reactants.get(key), dictionary, metabolites);
				newProduct = replaceGenericMetabolite(products.get(positions.get(key)), dictionary, metabolites);
			}

			if(newReactant.equals(reactants.get(key)) && newProduct.equals(products.get(positions.get(key))))
				notReplaced = true;

			//				boolean save = true;
			//				
			//				if(type.equals(TypeOfTransporter.BiochemicalATP) || type.equals(TypeOfTransporter.BiochemicalGTP)) {
			//
			//					Boolean uptake = isUptake(getProteinDescriptions(newTcContainer));
			//
			//					if(uptake != null) {
			//						if(uptake && !newReactant.contains(ReactionContainer.EXTERIOR_COMPARTMENT_TOKEN) && !newProduct.contains(ReactionContainer.INTERIOR_COMPARTMENT_TOKEN))
			//							save = false;
			//
			//						else if(!uptake && newReactant.contains(ReactionContainer.EXTERIOR_COMPARTMENT_TOKEN) && newProduct.contains(ReactionContainer.INTERIOR_COMPARTMENT_TOKEN))
			//							save = false;
			//					}
			//				}

			//				if(result[2].equalsIgnoreCase("3")) {
			//					System.out.println(tc);
			//					System.out.println(originalReaction);
			//					System.out.println(result[0] + " " + reactionContainer.getDirection() +  " " + result[1]);
			//					//					
			//					System.out.println();
			//				}

			//				if(save) {

			ReactionContainer newReactContainer = new ReactionContainer(newReactant, newProduct, reactionContainer.isReversible());

			if(reactionContainer.getMetabolites().size() == 1) {
				type = TypeOfTransporter.Uniport;
			}
			newReactContainer.setTransportType(type);
			newReactContainer = validateReactionDirection(newReactContainer, getProteinDescriptions(newTcContainer));


			newReactContainer.setProperties(reactionContainer.getProperties());
			newReactContainer.setOriginalReaction(originalReaction);
			newReactContainer.setConfidenceLevel("B-0");

			//			System.out.println(newReactContainer.getReaction());

			if(notReplaced)
				tempListRepeated.add(newReactContainer);
			else
				tempListNew.add(newReactContainer);

			//				}

			//					if(tc.equals("3.A.1.7.5 O51235")) {
			//
			//						System.out.println(newReactContainer.getReaction());
			//					}


			//			else {
			//				System.out.println(tc + " --->> antiport" + " -----> " + metabolites);
			//				System.out.println(reactants.get(key) + " " + reactionContainer.getDirection() +  " " +  products.get(positions.get(key)));
			//				
			//				System.out.println();
			//			}
		}
		//		}

		if(!tempListNew.isEmpty()) {
			for(ReactionContainer reaction : tempListNew)
				newTcContainer.addReaction(reaction);
		}
		else {
			for(ReactionContainer reaction : tempListRepeated)
				newTcContainer.addReaction(reaction);
		}

		//		for(String r : reactions)
		//			System.out.println(r);

		//		for(String react : reactions) {
		//
		//			System.out.println(react);
		//			String[] newReaction;
		//
		//			if(reactionContainer.getReversible())
		//				newReaction = react.split(ReactionContainer.REV_TOKEN);
		//			else
		//				newReaction = react.split(ReactionContainer.IRREV_TOKEN);
		//
		//			ReactionContainer newReactContainer = new ReactionContainer(newReaction[0], newReaction[1], reactionContainer.getReversible());
		//
		//			newReactContainer.setTransportType(reactionContainer.getTransportType());
		//			newReactContainer.setProperties(reactionContainer.getProperties());
		//			newReactContainer.setOriginalReaction(originalReaction);
		//
		//			newTcContainer.addReaction(newReactContainer);
		//		}
		//		System.out.println();
		return newTcContainer;
	}

	/**
	 * @param string
	 * @return
	 */
	private static String replaceGenericMetabolite(String text, Synonyms dictionary, List<String> metabolites) {

		boolean twoMetabolites = false;

		for(String metabolite : metabolites) {

			boolean replaced = false;

			for(String target : GENERAL_METABOLITES) {

				if(text.contains(target)) {

					text = text.replace(target, metabolite);
					replaced = true;
					break;
				}
			}

			if(!replaced) {
				for(String target : TWO_METABOLITES_EVIDENCE) {

					if(!replaced) {
						for(String alias : dictionary.getMetabolitesDictionary().get(target)) {

							if(twoMetabolites)
								alias = alias.replace("2", "1");

							if(text.matches("(?i).*\\s+" + alias + "\\s+.*") || text.matches("(?i)^" + alias + "\\s+.*")) {
								text = text.replaceAll("(?i)"+alias, metabolite);
								twoMetabolites = true;
								replaced = true;
								break;
							}
						}
					}
					else
						break;
				}
			}
		}
		return text;
	}

	private static TcNumberContainer filterUnwantedReactions(String tcNumber, TcNumberContainer newTcContainer, TypeOfTransporter evidence) {

		Set<TypeOfTransporter> set = newTcContainer.getAllTransportTypesAssociated();

		TypeOfTransporter toKeep = TypeOfTransporter.Uniport;

		if(tcNumber.startsWith("3.") && set.contains(TypeOfTransporter.BiochemicalATP))
			toKeep = TypeOfTransporter.BiochemicalATP;

		else if(evidence == null && !set.contains(TypeOfTransporter.Uniport) && !set.contains(TypeOfTransporter.Symport) && !set.contains(TypeOfTransporter.Antiport))
			toKeep = TypeOfTransporter.MoreThanOne;

		else if(set.size() == 2 && set.contains(TypeOfTransporter.RedoxNADH) && set.toString().contains("port"))
			toKeep = TypeOfTransporter.RedoxNADH;

		else if(evidence != null && (set.contains(evidence) || evidence.equals(TypeOfTransporter.MoreThanOne)))
			toKeep = evidence;

		else if(!set.contains(toKeep) && set.contains(TypeOfTransporter.Symport))
			toKeep = TypeOfTransporter.Symport;

		else if(!set.contains(toKeep) && set.contains(TypeOfTransporter.Antiport))
			toKeep = TypeOfTransporter.Antiport;

		else if(!set.contains(TypeOfTransporter.Uniport)) {
			for(TypeOfTransporter t : set)
				toKeep = t;
		}

		if(set.size() > 1 && !toKeep.equals(TypeOfTransporter.MoreThanOne)) {

			//			System.out.println(set + "\t" + tcNumber);

			for(int id : newTcContainer.getAllReactionsIds()) {

				ReactionContainer container = newTcContainer.getReactionContainer(id);

				if(container.getConfidenceLevel().equals(METACYC_CONFIDENCE_LEVEL) && !container.getTransportType().equals(toKeep))
					newTcContainer.removeReaction(id);
				else if(!container.getConfidenceLevel().equals(METACYC_CONFIDENCE_LEVEL) 
						&& !container.getTransportType().equals(toKeep) 
						&& !container.getTransportType().equals(TypeOfTransporter.Biochemical))
					newTcContainer.removeReaction(id);


			}
		}

		return newTcContainer;
	}

	/**
	 * Process metabolites associated to the wrong transport types. 
	 * 
	 * @param evidence
	 * @param reactionContainer
	 * @param metabolites
	 * @return
	 */
	private static TcNumberContainer processPEPReactions(String tc, String originalReaction, String confLevel, Map<String, String> properties, List<String> metabolites, TcNumberContainer newTcContainer) {

		String[] result = originalReaction.split(ReactionContainer.IRREV_TOKEN);

		String reactant = result[0];

		reactant = reactant.replace("Sugar", metabolites.get(0));

		String product = result[1].replace("Sugar-P", metabolites.get(0).concat(" 6-phosphate")); //Alternative to try to force match phosphates not reacheable through MetaCyc hierarchies

		List<String> products = new ArrayList<>();

		products.add(result[1]);
		products.add(product);

		for(String p : products) {

			ReactionContainer newReactContainer = new ReactionContainer(reactant, p, false);

			newReactContainer.setTransportType(TypeOfTransporter.PEPdependent);
			newReactContainer.setProperties(properties);
			newReactContainer.setOriginalReaction(originalReaction);
			newReactContainer.setConfidenceLevel(confLevel);

			newTcContainer.addReaction(newReactContainer);

		}

		return newTcContainer;
	}

	/**
	 * @param reactionContainer
	 * @param newTcContainer
	 * @return
	 */
	public static TcNumberContainer processConjugatePairs(ReactionContainer reactionContainer, TcNumberContainer newTcContainer) {

		String proteinDescription = newTcContainer.getProteinDescription();

		Map<String, String[]> pairs = null;

		if(reactionContainer.isOxidase())
			pairs = OXIDASE_PAIRS;
		else if(reactionContainer.isReductase())
			pairs = REDUCTASE_PAIRS;

		for(String key : pairs.keySet()) {

			if(proteinDescription.matches("(?i).*" + key.trim() + ".*")) {

				String pair1 = pairs.get(key)[0].trim();
				String pair2 = pairs.get(key)[1].trim();

				String reactant = executePairReplacement(
						reactionContainer.getReactant().replaceAll(pairs.get(key)[2], pairs.get(key)[3]), pair1, "SoluteX");
				String product = executePairReplacement(
						reactionContainer.getProduct().replaceAll(pairs.get(key)[4], pairs.get(key)[5]), pair2, "SoluteY");

				ReactionContainer newReactContainer = new ReactionContainer(reactant, product, reactionContainer.isReversible());

				newReactContainer.setTransportType(reactionContainer.getTransportType());
				newReactContainer.setProperties(reactionContainer.getProperties());
				newReactContainer.setOriginalReaction(reactionContainer.getOriginalReaction());
				newReactContainer.setConfidenceLevel("R-1");
				newReactContainer.setReductase(reactionContainer.isReductase());
				newReactContainer.setOxidase(reactionContainer.isOxidase());

				newTcContainer.addReaction(newReactContainer);

			}
		}

		return newTcContainer;
	}

	public static String executePairReplacement(String text, String pair, String toReplace) {

		if(pair.matches(".*\\s+\\+\\s+.*")) {

			String[] compounds = text.split("\\s+\\+\\s+");

			String compartment = ReactionContainer.INTERIOR_COMPARTMENT_TOKEN_REG;

			for(String compound : compounds) {
				if(compound.contains(toReplace)) {
					if(compound.contains(ReactionContainer.INTERIOR_COMPARTMENT_TOKEN))
						compartment = ReactionContainer.INTERIOR_COMPARTMENT_TOKEN_REG;
					else if(compound.contains(ReactionContainer.EXTERIOR_COMPARTMENT_TOKEN))
						compartment = ReactionContainer.EXTERIOR_COMPARTMENT_TOKEN_REG;
					else if(compound.contains(ReactionContainer.MIDDLE_COMPARTMENT_TOKEN))
						compartment = ReactionContainer.MIDDLE_COMPARTMENT_TOKEN_REG;
					break;
				}
			}

			String[] newCompounds = pair.split("\\s+\\+\\s+");
			String newString = "";

			for(String newCompound : newCompounds)
				newString = newString + newCompound + " " + compartment + " \\+ ";

			newString = newString.replaceAll("\\s+\\\\\\+\\s+$", "");

			text = text.replaceAll(toReplace + "\\s+" + compartment, newString);

			//			if(!reductase && pair.equalsIgnoreCase("ethylbenzene"))
			//				text = text.replace(" 2H+ (out)", " H2O (in) + H+ (out)");
		}
		else {
			text = text.replace(toReplace, pair);
		}

		return text;
	}

	/**
	 * Process metabolites associated to the wrong transport types. 
	 * 
	 * @param evidence
	 * @param reactionContainer
	 * @param metabolites
	 * @return
	 */
	private static TcNumberContainer correctReaction(String tc, Boolean revEvidence, String originalReaction, String confLevel, 
			TypeOfTransporter evidence, Map<String, String> properties, List<String> metabolites, TcNumberContainer newTcContainer) {

		//		if(tc.equals("3.D.1.1.1 P0AFD6"))
		//			System.out.println("entrou " + evidence);

		//		if(confLevel.equals("C-17asghghsa" + metabolites + "\t" + tc);

		if(evidence != TypeOfTransporter.Uniport)
			metabolites.remove(TcdbMetabolitesContainer.PROTON);

		metabolites.remove(TcdbMetabolitesContainer.EMPTY);
		metabolites.remove(TcdbMetabolitesContainer.UNKNOWN);

		//		if(confLevel.equals("C-1"))
		//			System.out.println(metabolites + "\t" + tc);

		for(String metabolite : metabolites) {
			ReactionContainer newReaction = generateReactionBasedOnEvidence(originalReaction, evidence, metabolite,
					confLevel, properties, revEvidence, getProteinDescriptions(newTcContainer));

			newTcContainer.addReaction(newReaction);

		}

		return newTcContainer;
	}

	private static ReactionContainer generateReactionBasedOnEvidence(String originalReaction, TypeOfTransporter evidence, String metabolite,
			String confLevel, Map<String, String> properties, Boolean revEvidence, List<String> proteinDescriptions) {

		String reaction = generateReactionFromMetabolites(originalReaction, evidence, metabolite);

		String[] result = reaction.split(ReactionContainer.REV_TOKEN);

		if(result.length < 2)
			result = reaction.split(ReactionContainer.IRREV_TOKEN);

		ReactionContainer newReactContainer = null;

		newReactContainer = new ReactionContainer(result[0], result[1], (revEvidence != null ? revEvidence : true));
		newReactContainer.setTransportType(evidence);

		newReactContainer = validateReactionDirection(newReactContainer, proteinDescriptions);

		newReactContainer.setProperties(properties);
		newReactContainer.setOriginalReaction(originalReaction);
		newReactContainer.setConfidenceLevel(confLevel);

		return newReactContainer;

	}

	/**
	 * @param transportType
	 * @param metabolites
	 * @return
	 */
	private static String generateReactionFromMetabolites(String originalReaction, TypeOfTransporter transportType, String metabolite){

		boolean metacyc = false;

		if(metabolite.contains("META:"))
			metacyc = true;

		if(transportType.equals(TypeOfTransporter.Uniport)) {
			return metabolite.concat(" (out) ").concat(ReactionContainer.REVERSIBLE_TOKEN).concat(" ").concat(metabolite).concat(" (in)");
		}
		else if(transportType.equals(TypeOfTransporter.Symport)){
			return metabolite.concat(" (out) + ").concat(TcdbMetabolitesContainer.PROTON).concat(" (out) ")
					.concat(ReactionContainer.REVERSIBLE_TOKEN).concat(metabolite).concat(" (in) + ").concat(TcdbMetabolitesContainer.PROTON).concat(" (in)");
		}
		else if(transportType.equals(TypeOfTransporter.Antiport)) {
			return metabolite.concat(" (in) + ").concat(TcdbMetabolitesContainer.PROTON).concat(" (out) ").concat(ReactionContainer.REVERSIBLE_TOKEN)
					.concat(metabolite).concat(" (out) + ").concat(TcdbMetabolitesContainer.PROTON).concat(" (in)");
		}
		else if(transportType.equals(TypeOfTransporter.PEPdependent)) {
			return metabolite.concat(" (out) + ").concat(TcdbMetabolitesContainer.PHOSPHOENOLPYRUVATE).concat(" (in) ").concat(ReactionContainer.IRREVERSIBLE_TOKEN)
					.concat(metabolite).concat(" 6-phosphate (in) + ").concat(TcdbMetabolitesContainer.PYRUVATE).concat(" (in)");
		}
		else if(transportType.equals(TypeOfTransporter.BiochemicalATP)) {
			if(metacyc)
				return metabolite.concat(" (out) + ").concat(TcdbMetabolitesContainer.ATP).concat(" (in) + ").concat("META:WATER (in) ").concat(ReactionContainer.IRREVERSIBLE_TOKEN)
						.concat(metabolite).concat(" (in) + ").concat(TcdbMetabolitesContainer.ADP).concat(" (in) + ").concat(TcdbMetabolitesContainer.PHOSPHATE).concat(" (in) + ")
						.concat(TcdbMetabolitesContainer.PROTON).concat(" (in)");
			else
				return metabolite.concat(" (out) + ").concat(TcdbMetabolitesContainer.ATP).concat(" ").concat(ReactionContainer.IRREVERSIBLE_TOKEN)
						.concat(metabolite).concat(" (in) + ").concat(TcdbMetabolitesContainer.ADP).concat(" ").concat(TcdbMetabolitesContainer.PHOSPHATE);
		}
		return originalReaction;
	}

	/**
	 * Method to check if the antiport reaction has no specific metabolites already. true means "no specific metabolites", null means "not antiport", false means "antiport with specific metabolites"
	 * 
	 * @param reaction
	 * @param transportType
	 * @param dictionary
	 * @return
	 */
	private static Boolean selectMethodOfMetabolitesDistribution(String reaction, TypeOfTransporter transportType, Synonyms dictionary) {

		for(String metabolite : TWO_METABOLITES_EVIDENCE) {

			for(String alias : dictionary.getMetabolitesDictionary().get(metabolite)) {

				if(reaction.matches("(?i).*\\s+" + alias + "\\s+.*")) {
					return true;
				}
			}
		}

		if(!transportType.equals(TypeOfTransporter.Antiport))
			return null;

		return false;
	}

	/**
	 * Check if the enty has evidence of type of transporter in the retrieved description.
	 * 
	 * @param description
	 * @return
	 */
	private static TypeOfTransporter checkForEvidenceOfTransport(String description, String tcNumber) {

		try {
			if(tcNumber.equals("2.A.1.4.8"))			//create exception
				return TypeOfTransporter.Uniport;

			else if(tcNumber.equals("2.A.60.1.5") || tcNumber.contains("2.A.49."))
				return TypeOfTransporter.Antiport;

			else if(tcNumber.matches("3\\.D\\..+")) {

				if(description.matches("(?i).*cytochrome.*") && description.matches("(?i).*quinol.*")) {
					return TypeOfTransporter.Redox;
				}
				return null;
			}

			if(description.matches("(?i).*uniporte*r*[\\s*\\.*]*.*"))
				return TypeOfTransporter.Uniport;

			else if(description.matches("(?i).*symporte*r*[\\s*\\.*]*.*"))
				return TypeOfTransporter.Symport;

			else if(description.matches("(?i).*antiporte*r*[\\s*\\.*]*.*"))
				return TypeOfTransporter.Antiport;

		} 
		catch (Exception e) {
			System.out.println(tcNumber);
			System.out.println(description);

			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Method to process all "and", "and other" and ", " expressions inside a given reaction.
	 * 
	 * @param originalReaction
	 * @return
	 */
	private static Set<String> processReaction(String originalReaction, Boolean reversible) {

		Set<String> allReactions = new HashSet<>();

		String reactantCompartment = "(out)";
		String productCompartment = "(in)";

		String direction = "";
		String[] parseReact;

		if(reversible) {
			direction = " " + ReactionContainer.REVERSIBLE_TOKEN + " ";
			parseReact = originalReaction.split(ReactionContainer.REV_TOKEN);
		}
		else {
			direction = " " + ReactionContainer.IRREVERSIBLE_TOKEN + " ";
			parseReact = originalReaction.split(ReactionContainer.IRREV_TOKEN);
		}

		String reactant = parseReact[0];
		String product = parseReact[1];

		if(reactant.contains("(in)")) {
			reactantCompartment = "(in)";
			productCompartment = "(out)";
		}

		if(originalReaction.contains("{")) {

			String[] text = reactant.split(" \\+ ");

			for(int i = 0; i < text.length; i++) {

				if(!text[i].contains(reactantCompartment))
					reactant = reactant.replace(text[i],text[i].concat(" ").concat(reactantCompartment));

			}

			text = product.split(" \\+ ");

			for(int i = 0; i < text.length; i++) {

				if(!text[i].contains(productCompartment))
					product = product.replace(text[i],text[i].concat(" ").concat(productCompartment));

			}

			reactant = reactant.replaceAll("[\\{\\}]", "");
			product = product.replaceAll("[\\{\\}]", "");
			originalReaction = reactant.concat(direction).concat(product);
		}

		if(originalReaction.matches(".+\\w\\s\\+\\s.*")) {

			reactant = reactant.replace(" + ", " " + reactantCompartment + " + ");
			product = product.replace(" + ", " " + productCompartment + " + ");
			originalReaction = originalReaction.replace(" + ", " ");

		}

		if(originalReaction.contains(" and other ")) {

			reactant = reactant.replace(" and other ", reactantCompartment + " $ ");
			product = product.replace("and other ", productCompartment + " $ ");
			originalReaction = originalReaction.replace("and other ", " ");
		}

		if(originalReaction.contains(" and/or ")) {
			reactant = reactant.replace("and/or ", reactantCompartment + " $ ");
			product = product.replace("and/or ", productCompartment + " $ ");
			originalReaction = originalReaction.replace(" and/or ", " ");
		}

		if(originalReaction.contains("/")) {
			reactant = reactant.replace("/", " " + reactantCompartment + " $ ");
			product = product.replace("/", " " + productCompartment + " $ ");
			originalReaction = originalReaction.replace("/", " ");
		}

		if(originalReaction.contains(") and ")) {
			reactant = reactant.replace("and ", " $ ").concat(reactantCompartment);
			product = product.replace("and ", " $ ").concat(productCompartment);
			originalReaction = originalReaction.replace("and ", " ");
		}

		if(originalReaction.contains(" and ")) {
			reactant = reactant.replace("and ", reactantCompartment + " $ ");
			product = product.replace("and ", productCompartment + " $ ");
			originalReaction = originalReaction.replace("and ", " ");
		}

		if(originalReaction.contains(") or ")) {
			reactant = reactant.replace("or", " $ ");
			product = product.replace("or ", " $ ");
			originalReaction = originalReaction.replace("or ", " ");
		}

		if(originalReaction.contains(" or ")) {
			reactant = reactant.replace("or", reactantCompartment + " $ ");
			product = product.replace("or ", productCompartment + " $ ");
			originalReaction = originalReaction.replace("or ", " ");
		}

		if(originalReaction.matches(".*,\\s.*")) {
			reactant = reactant.replace(", ", " "+ reactantCompartment + " $ ");
			product = product.replace(", ", " " + productCompartment + " $ ");
			originalReaction = originalReaction.replace(", ", " ");
		}

		String[] metabolitesReact = reactant.split("\\$");
		String[] metabolitesProd = product.split("\\$");

		for(int i = 0; i < metabolitesReact.length; i++) {

			for(int j = 0; j < metabolitesProd.length; j++) {

				if(metabolitesProd[j].toLowerCase().contains(metabolitesReact[i].split("\\(")[0].trim().toLowerCase()))
					allReactions.add(metabolitesReact[i].concat(direction).concat(metabolitesProd[j]).trim());
			}
		}

		return allReactions;
	}

	/**
	 * Generate all possible reactions for a given 'reaction' representation
	 * 
	 * @param reaction
	 * @param reactant
	 * @return
	 */
	private static Set<String> getAllPossibleReactions(String originalReaction, String reactant, String product, Boolean reversible) {

		Set<String> preReactions = new HashSet<>();
		Set<String> reactions = new HashSet<>();

		//		if(tc.equals("1.A.26.2.2 Q96JW4"))
		//			System.out.println("originalReaction " + originalReaction);

		if((originalReaction.contains(" and") || originalReaction.contains(", ") || originalReaction.contains("{") || originalReaction.contains("/") 
				|| (originalReaction.contains("or") && !originalReaction.contains(" + ")) || originalReaction.matches(".+\\w\\s\\+\\s.*"))
				&& !originalReaction.contains("[")) {

			if(originalReaction.matches(".*S*s*mall and l*L*arge.*")) {

				originalReaction = originalReaction.replaceAll("S*s*mall and l*L*arge", "");
				reactant = reactant.replaceAll("S*s*mall and l*L*arge", "");
				product = product.replaceAll("S*s*mall and l*L*arge", "");
			}

			preReactions = processReaction(originalReaction, reversible);

		}

		//		if(tc.equals("1.A.26.2.2 Q96JW4"))
		//			System.out.println("preReactions " + preReactions);

		if(preReactions.isEmpty())
			preReactions.add(originalReaction);

		for(String reaction : preReactions) {

			String newReactant;

			if(reversible) 
				newReactant = reaction.split(ReactionContainer.REV_TOKEN)[0];

			else 
				newReactant = reaction.split(ReactionContainer.IRREV_TOKEN)[0];

			Map<Integer, Map<String, String>> positions = getMetabolitesAndRegexFromReaction(newReactant);

			Map<String, String> regex = new HashMap<>();

			for(int key : positions.keySet()) {

				for(String met : positions.get(key).keySet()) {

					regex.put(met, positions.get(key).get(met));
					//					regex.put(met.replace("\\(in\\)", "").replace("\\(out\\)", ""), positions.get(key).get(met).replace("\\(in\\)", "").replace("\\(out\\)", ""));
				}
			}

			List<Set<String>> all = new ArrayList<>();
			List<Set<String>> main = new ArrayList<>();

			for(int i : positions.keySet()) {

				if(i == 0) {
					for(String met : positions.get(i).keySet()) {

						Set<String> set = new HashSet<>();

						set.add(met);
						main.add(set);
					}
				}
				else
					all.add(positions.get(i).keySet());

			}

			List<Set<String>> distributions = executeDistributions(main, all, 0);

			//			if(tc.equals("2.A.1.38.3 Q8ZR35")) {
			//				System.out.println("distributions " + distributions);
			//				System.out.println("regex " + regex);
			//			}

			for(Set<String> set : distributions) {

				String newReaction = new String(reaction);

				for(String cmpd : set) {

					if(!regex.get(cmpd).isEmpty()) {
						newReaction = newReaction.replace(regex.get(cmpd), cmpd);
					}

				}
				reactions.add(newReaction);

			}
		}

		//		if(tc.equals("2.A.39.3.3 P38196")) {
		//			System.out.println("reactionsfinal " + reactions);
		//			System.out.println();
		//		}
		//		for(String r : reactions)
		//			System.out.println(r);

		return reactions;
	}

	/**
	 * Recursive method to find all possible distributions of compounds.
	 * 
	 * @param reactions
	 * @param l
	 * @param cycle
	 * @return
	 */
	private static List<Set<String>> executeDistributions(List<Set<String>> reactions, List<Set<String>> l, int cycle){

		List<Set<String>> reactions2 = new ArrayList<>();

		if(cycle == 0 && l.size() == 0) {

			for(Set<String> set : reactions) {

				for(String metabolite : set) {

					Set<String> newSet = new HashSet<>();

					newSet.add(metabolite);
					reactions2.add(newSet);

				}
			}
			return reactions2;
		}

		if(cycle == l.size()) 
			return reactions;

		Set<String> l2 = l.get(cycle);

		for(Set<String> er : reactions) {

			for(String el: l2) {

				Set<String> erClone = new HashSet<>(er);

				erClone.add(el);

				reactions2.add(erClone);
			}

		}

		reactions.clear();

		reactions.addAll(reactions2);

		cycle++;

		return executeDistributions(reactions, l, cycle);

	}

	/**
	 * Get the possible metabolites and the possitions where they should be introduced in the reaction, as well as the regex to replace them in the original reaction
	 * 
	 * @param reaction
	 * @return
	 */
	private static Map<Integer, Map<String, String>> getMetabolitesAndRegexFromReaction(String reaction){

		Map<Integer, Map<String, String>> metabolites = new HashMap<>();

		String[] text = reaction.split(" \\+ ");

		String reactCompartment = "";

		for(int i = 0; i < text.length; i++) {

			//			System.out.println(text[i]);

			Map<String, String> met = new HashMap<>();

			//			System.out.println(text[i]);

			//			text[i] = text[i].replaceAll("\\(in\\)", "").replaceAll("\\(out\\)", "");

			if(text[i].contains("(in)"))
				reactCompartment = " (in) ";
			else if(text[i].contains("(out)"))
				reactCompartment = " (out) ";

			if(text[i].contains(") [or")) {

				String regex = text[i];

				text[i] = text[i].replaceAll("\\(in", "").replaceAll("\\(out", "");

				String[] subtext = text[i].split("\\) \\[or");

				for(int j = 0; j < subtext.length; j++) 
					met.put(subtext[j].replaceAll("\\]", "").replaceAll("[()]", "").trim().concat(reactCompartment), regex.trim());

			}

			else if(text[i].contains("[")) {

				//				System.out.println(text[i]);
				//				System.out.println("compart >>" + reactCompartment);

				if(text[i].contains("[")) {
					text[i] = text[i].split("\\]")[0];

					//					System.out.println("czs" + text[i]);

					if(!text[i].contains(reactCompartment.trim()))
						reactCompartment = "";
				}

				//				System.out.println("compart1 >>" + reactCompartment);

				String regex = text[i].concat("]");

				text[i] = text[i].replaceAll("\\(in\\)", "").replaceAll("\\(out\\)", "");

				if(text[i].contains("(or ")) {

					String[] subtext = text[i].split("\\(or ");

					for(int j = 0; j < subtext.length; j++) 
						met.put(subtext[j].replace("[", "").replace(")", "").trim().concat(reactCompartment), regex.trim());
				}
				else if(text[i].contains(", ")) {

					String[] subtext = text[i].split(", ");

					for(int j = 0; j < subtext.length; j++) {

						if(subtext[j].contains(" or ")) {

							String[] subsubtext = subtext[j].split(" or ");

							for(int k = 0; k < subsubtext.length; k++)
								met.put(subsubtext[k].replace("[", "").replace(")", "").trim(), regex.trim());
						}
						else
							met.put(subtext[j].replace("[", "").trim().concat(reactCompartment), regex.trim());

					}

				}
				else if(text[i].contains(" or ")) {

					String[] subtext = text[i].split(" or ");

					for(int j = 0; j < subtext.length; j++) 
						met.put(subtext[j].replace("[", "").trim().concat(reactCompartment), regex.trim());
				}
			}
			else if(text[i].contains("(or")) {

				//				if(tc.equals("1.A.26.2.2 Q96JW4"))
				//					System.out.println("text[i] " + text[i]);

				text[i] = text[i].replaceAll("\\(in\\)", "").replaceAll("\\(out\\)", "");

				String regex = text[i];

				String[] subtext = text[i].split("\\(or ");

				for(int j = 0; j < subtext.length; j++) 
					met.put(subtext[j].replace(")", "").trim(), regex.trim());

				//				if(tc.equals("1.A.26.2.2 Q96JW4"))
				//					System.out.println("met " + met);

			}
			else if(text[i].contains(" or ")) {

				//				String regex = text[i];

				text[i] = text[i].replaceAll("\\(in\\)", "").replaceAll("\\(out\\)", "");

				String regex = text[i];

				String[] subtext = text[i].split(" or ");

				for(int j = 0; j < subtext.length; j++) { 

					if(subtext[j].contains(", ")) {

						String[] subsubtext = subtext[j].split(", ");

						for(int k = 0; k < subtext.length; k++)
							met.put(subsubtext[k].trim().concat(reactCompartment), regex.trim());

					}
					else
						met.put(subtext[j].trim(), regex.trim());
				}
			}
			else {
				met.put(text[i].trim(), "");

			}

			metabolites.put(i, met);
		}

		//		if(tc.equals("1.A.26.2.2 Q96JW4"))
		//			System.out.println("metabolites " + metabolites);

		return metabolites;
	}


	/**
	 * Method to process Uniport and Symport reactions
	 * 
	 * @param reactionContainer
	 * @param newTcContainer
	 * @param familyTC
	 * @return
	 */
	public static TcNumberContainer processUniportAndSymportReactions(ReactionContainer reactionContainer, TcNumberContainer newTcContainer, List<String> metabolites, Synonyms dictionary) throws Exception {

		reactionContainer = validateReactionDirection(reactionContainer, getProteinDescriptions(newTcContainer));

		String originalReaction = reactionContainer.getReaction();

		String reactant = reactionContainer.getReactant();
		String product = reactionContainer.getProduct();

		String[] aux = correctMetabolitesNames(originalReaction, reactant, product);

		originalReaction = aux[0];
		reactant = aux[1];
		product = aux[2];

		Set<String> reactions = getAllPossibleReactions(originalReaction, reactant, product, reactionContainer.isReversible());

		//		if(tc.equals("2.A.20.1.2 P43676")) {
		//			System.out.println("Areactions " + reactions);
		//			System.out.println("metabolites " + metabolites);
		//		}

		for(String react : reactions) {

			//			System.out.println(react);
			String[] newReaction;

			if(reactionContainer.isReversible())
				newReaction = react.split(ReactionContainer.REV_TOKEN);
			else
				newReaction = react.split(ReactionContainer.IRREV_TOKEN);

			//			if(tc.equals("3.D.1.1.1 P33602")) {
			//				System.out.println(newReaction[0]);
			//					System.out.println(newReaction[1]);
			//			}

			String[] result = checkReactionsMetabolites(newReaction[0], newReaction[1], metabolites, dictionary, reactionContainer.getTransportType(), react);

			//			if(tc.equals("1.C.10.3.1 K7JEL9"))
			//				System.out.println(result[1]);

			if(result != null) {

				//				if(tc.equals("1.C.30.1.2 O32831"))
				//					System.out.println(result[0]);

				//				if(result[2].equalsIgnoreCase("3")) {
				//					System.out.println(tc);
				//					System.out.println(originalReaction);
				//					System.out.println(result[0] + " " + reactionContainer.getDirection() +  " " + result[1]);
				//					//					
				//					System.out.println();
				//				}

				ReactionContainer newReactContainer = new ReactionContainer(result[0], result[1], reactionContainer.isReversible());

				newReactContainer.setTransportType(reactionContainer.getTransportType());
				newReactContainer.setProperties(reactionContainer.getProperties());
				newReactContainer.setOriginalReaction(originalReaction);
				newReactContainer.setConfidenceLevel(result[2]);

				newTcContainer.addReaction(newReactContainer);
			}
			//			else {
			//				System.out.println(tc + " -----> " + metabolites);
			//				System.out.println(newReaction[0] + " " + reactionContainer.getDirection() +  " " + newReaction[1]);
			//				
			//				System.out.println();
			//			}
		}
		//		System.out.println();
		return newTcContainer;
	}

	/**
	 * @param reactionContainer
	 * @param newTcContainer
	 * @return
	 */
	private static ReactionContainer validateReactionDirection(ReactionContainer reactionContainer, List<String> proteinDescriptions) {

		if(!(reactionContainer.getReactant().contains(ReactionContainer.INTERIOR_COMPARTMENT_TOKEN) 
				&& reactionContainer.getReactant().contains(ReactionContainer.EXTERIOR_COMPARTMENT_TOKEN)) || 
				reactionContainer.getTransportType().equals(TypeOfTransporter.BiochemicalATP)){

			boolean switch_ = false;
			Boolean uptake = isUptake(proteinDescriptions);

			if(uptake != null) {

				reactionContainer.setReversible(false);

				if(uptake) {
					if(reactionContainer.getReactant().contains(ReactionContainer.INTERIOR_COMPARTMENT_TOKEN))
						switch_ = true;
				}
				if(!uptake) {
					if(reactionContainer.getReactant().contains(ReactionContainer.EXTERIOR_COMPARTMENT_TOKEN))
						switch_ = true;
				}

				if(switch_) {
					reactionContainer.invertDirection();
				}
			}
		}

		return reactionContainer;
	}

	/**
	 * Get protein, subfamily and family descriptions.
	 * 
	 * @param newTcContainer
	 * @return
	 */
	private static List<String> getProteinDescriptions(TcNumberContainer newTcContainer) {

		List<String>  descriptions = new ArrayList<>();

		if(newTcContainer.getProteinDescription() != null)
			descriptions.add(newTcContainer.getProteinDescription());

		if(newTcContainer.getSubfamilyDescription() != null)
			descriptions.add(newTcContainer.getSubfamilyDescription());

		if(newTcContainer.getFamily() != null)
			descriptions.add(newTcContainer.getFamily());

		return descriptions;
	}

	/**
	 * Retrieves the metabolites of a reaction and compares them with a dictionary, standardizing the names
	 * 
	 * @param reaction
	 * @param dictionary
	 * @return
	 */
	private static Map<String, String> retrieveReactionMetabolites(String reaction, Synonyms dictionary) {

		Map<String, String> regex = new HashMap<>();

		reaction = reaction.replaceAll("\\(in\\)", "").replaceAll("\\(out\\)", "");

		String[] result = reaction.split(" \\+ ");

		for(int i = 0; i < result.length; i++) {

			String target = result[i];

			if(target.equalsIgnoreCase("nMe1"))
				target = "Me1";

			else if(target.equalsIgnoreCase("mMe2"))
				target = "Me2";

			target = target.replaceAll("^(\\s*n\\s*)", "");
			target = target.replaceAll("^(\\s*\\d+\\s*)", "").trim();

			//			if(tc.equals("2.A.1.19.28 A1A5C7")) {
			//				System.out.println(target);
			//				System.out.println(dictionary.getSynonym(target.toLowerCase().replaceAll("\\s+", "")));
			//
			//			}

			String word = dictionary.getSynonym(target.toLowerCase().replaceAll("\\s+", ""));

			if(word != null) {

				regex.put(word.trim(), target);
			}
			else
				regex.put(target, target);
		}

		return regex;
	}

	/**
	 * Checks the reaction searching for the presence of the metabolites described in TCDB.
	 * 
	 * @param string
	 * @param string2
	 * @param metabolites
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private static String[] checkReactionsMetabolites(String reactant, String product, List<String> metabolites, Synonyms dictionary, TypeOfTransporter transportType, String originalReaction) {

		//		if(tc.equals("3.D.1.1.1 P33602"))
		//			System.out.println(reactant + " <<<<>>>> " + product + "\t\t" + metabolites);
		//				
		boolean accept = false;

		Object[] regex = dictionary.correctSugars(reactant, product, metabolites);

		if(regex != null) {

			reactant = (String) regex[0];
			product = (String) regex[1];

			metabolites = new ArrayList<>((List<String>) regex[2]);
		}



		Map<String, String> reactantMetabolites = retrieveReactionMetabolites(reactant, dictionary);
		Map<String, String> productMetabolites = retrieveReactionMetabolites(product, dictionary);

		// TESTING


		//		if(reactantMetabolites.keySet().contains("Toxin") || productMetabolites.keySet().contains("Toxin")) {
		//			
		//			reactantMetabolites = new HashMap<>(dictionary.correctToxins(reactantMetabolites, metabolites));
		//			productMetabolites = new HashMap<>(dictionary.correctToxins(productMetabolites, metabolites));
		//		}


		//		if(tc.equals("3.D.1.1.1 P33602")) {
		//			System.out.println(reactant + "\t" + product + "\t\t" + metabolites);
		//			System.out.println(reactantMetabolites);
		//			System.out.println(productMetabolites);
		//		}

		int count = 1;

		int confidenceLevel = 0;

		for(String evidenceOfSymOrAnti : TWO_METABOLITES_EVIDENCE) {

			if(reactantMetabolites.containsKey(evidenceOfSymOrAnti) || productMetabolites.containsKey(evidenceOfSymOrAnti)) {

				if(metabolites.size() == 2) {

					boolean first = true;

					Collections.reverse(metabolites);	//this is important to respect TCDB order

					for(String metabolite : metabolites) {

						if(first) {
							reactant = reactant.replaceAll(reactantMetabolites.get(evidenceOfSymOrAnti), metabolite);		//example --> replaces by 'solute2'
							product = product.replaceAll(productMetabolites.get(evidenceOfSymOrAnti), metabolite);

							first = false;
						}
						else {

							String subString = evidenceOfSymOrAnti.substring(0, evidenceOfSymOrAnti.length()-1);

							reactant = reactant.replaceAll(reactantMetabolites.get(subString), metabolite);							//example --> replaces by 'solute'
							product = product.replaceAll(productMetabolites.get(subString), metabolite);

						}
					}

					accept = true;
					confidenceLevel = count;

					break;
				}

			}
		}

		count++;

		//transport of electrons

		if(!accept && metabolites.size() == 1) {

			for(String metabolite : metabolites) {

				if(metabolite.equalsIgnoreCase(TcdbMetabolitesContainer.ELECTRON)) {

					for(String react : reactantMetabolites.keySet())
						reactant = reactant.replace(reactantMetabolites.get(react), react);

					for(String prod : productMetabolites.keySet())
						product = product.replace(productMetabolites.get(prod), prod);

					accept = true;
					confidenceLevel = count;

					break;
				}

			}

		}

		count++;


		//'perfect' match
		if(!accept) {
			if(metabolites.containsAll(reactantMetabolites.keySet()) || metabolites.containsAll(productMetabolites.keySet())) {

				for(String react : reactantMetabolites.keySet())
					reactant = reactant.replace(reactantMetabolites.get(react), react);

				for(String prod : productMetabolites.keySet())
					product = product.replace(productMetabolites.get(prod), prod);

				accept = true;
				confidenceLevel = count;

				//			if(tc.equals("1.C.30.1.2 O32831")) {
				//				System.out.println(metabolites + "\t" + reactantMetabolites);
				//			}
			}
		}

		count++;

		//general "Ion" to specific ion

		if(!accept) {

			for(String react : reactantMetabolites.keySet()) {

				//				if(react.matches(""))

				for(String metabolite : metabolites) {

					if((react.replaceAll("[^A-Za-z0-9]", "").equalsIgnoreCase("Ion") || react.matches("Metal\\s\\w*ion"))  && (metabolite.matches("\\w+\\+") || metabolite.matches("\\w+\\-") || metabolite.matches("Metal\\s\\w*ion"))) {

						if((react.replaceAll("[^A-Za-z0-9]", "").equalsIgnoreCase("Ion") || metabolite.matches("Metal\\s\\w*ion")) || 
								(react.matches("Metal\\s\\w*ion") && dictionary.isChildOf("Metal ion", metabolite.replaceAll("\\d*\\+*\\-*", "")))){

							reactant = reactant.replace(reactantMetabolites.get(react), metabolite);
							accept = true;
						}						
					}
					else if((react.replaceAll("[^A-Za-z0-9]", "").equalsIgnoreCase("Cation") || react.matches("Metal cation"))  && metabolite.matches("\\w+\\+") || metabolite.matches("Metal\\scation")) {

						if((react.replaceAll("[^A-Za-z0-9]", "").equalsIgnoreCase("Cation") || metabolite.matches("Metal\\scation")) || 
								(react.matches("Metal\\scation") && dictionary.isChildOf("Metal ion", metabolite.replaceAll("\\d*\\+*", "")))){

							reactant = reactant.replace(reactantMetabolites.get(react), metabolite);
							accept = true;
						}	
					}	
					else if((react.replaceAll("[^A-Za-z0-9]", "").equalsIgnoreCase("Anion") || react.matches("Metal anion"))  && metabolite.matches("\\w+\\-") || metabolite.matches("Metal\\sanion")) {

						if((react.replaceAll("[^A-Za-z0-9]", "").equalsIgnoreCase("Anion") || metabolite.matches("Metal\\sanion")) || 
								(react.matches("Metal\\sanion") && dictionary.isChildOf("Metal ion", metabolite.replaceAll("\\d*\\-*", "")))){

							reactant = reactant.replace(reactantMetabolites.get(react), metabolite);
							accept = true;
						}	
					}	
					else if((metabolite.equalsIgnoreCase("Ion") || metabolite.matches("Metal ion")) && (react.matches("\\w+\\+") || react.matches("\\w+\\-") || react.matches("Metal\\s\\w*ion"))) {

						if((metabolite.replaceAll("[^A-Za-z0-9]", "").equalsIgnoreCase("Ion") || react.matches("Metal\\s\\w*ion")) || 
								(metabolite.matches("Metal\\s\\w*ion") && dictionary.isChildOf("Metal ion", react.replaceAll("\\d*\\+*\\-*", "")))){

							reactant = reactant.replace(reactantMetabolites.get(react), react);		//different case where the reaction is more specific than the metabolite
							accept = true;
						}	
					}
					else if((metabolite.equalsIgnoreCase("Anion") || metabolite.matches("Metal anion")) && (react.matches("\\w+\\-") || react.matches("Metal anion"))) {

						if((metabolite.replaceAll("[^A-Za-z0-9]", "").equalsIgnoreCase("Anion") || react.matches("Metal anion")) || 
								(metabolite.matches("Metal anion") && dictionary.isChildOf("Metal ion", react.replaceAll("\\d*\\-*", "")))){

							reactant = reactant.replace(reactantMetabolites.get(react), react);		//different case where the reaction is more specific than the metabolite
							accept = true;
						}	
					}
					else if((metabolite.equalsIgnoreCase("Cation") || metabolite.matches("Metal cation")) && (react.matches("\\w+\\+") || react.matches("Metal cation"))) {

						if((metabolite.replaceAll("[^A-Za-z0-9]", "").equalsIgnoreCase("Cation") || react.matches("Metal cation")) || 
								(metabolite.matches("Metal cation") && dictionary.isChildOf("Metal ion", react.replaceAll("\\d*\\+*", "")))){

							reactant = reactant.replace(reactantMetabolites.get(react), react);		//different case where the reaction is more specific than the metabolite
							accept = true;
						}	
					}
					else if(react.replaceAll("[^A-Za-z0-9]", "").equalsIgnoreCase("Anion") && metabolite.matches(".*ate")){

						reactant = reactant.replace(reactantMetabolites.get(react), metabolite);
						accept = true;
					}

				}
			}

			for(String prod : productMetabolites.keySet()) {

				for(String metabolite : metabolites) {

					if((prod.replaceAll("[^A-Za-z0-9]", "").equalsIgnoreCase("Ion") || prod.matches("Metal\\s\\w*ion")) && (metabolite.matches("\\w+\\+") || metabolite.matches("\\w+\\-") || metabolite.matches("Metal\\s\\w*ion"))) {

						if((prod.replaceAll("[^A-Za-z0-9]", "").equalsIgnoreCase("Ion") || metabolite.matches("Metal\\s\\w*ion")) || 
								(prod.matches("Metal\\s\\w*ion") && dictionary.isChildOf("Metal ion", metabolite.replaceAll("\\d*\\+*\\-*", "")))){

							product = product.replace(productMetabolites.get(prod), metabolite);
							accept = true;
						}	
					}
					else if((prod.replaceAll("[^A-Za-z0-9]", "").equalsIgnoreCase("Cation") || prod.matches("Metal cation")) && metabolite.matches("\\w+\\+") || metabolite.matches("Metal\\scation")) {

						if((prod.replaceAll("[^A-Za-z0-9]", "").equalsIgnoreCase("Cation") || metabolite.matches("Metal\\scation")) || 
								(prod.matches("Metal\\scation") && dictionary.isChildOf("Metal ion", metabolite.replaceAll("\\d*\\+*", "")))){

							product = product.replace(productMetabolites.get(prod), metabolite);
							accept = true;
						}	
					}	
					else if((prod.replaceAll("[^A-Za-z0-9]", "").equalsIgnoreCase("Anion") || prod.matches("Metal anion"))  && metabolite.matches("\\w+\\-") || metabolite.matches("Metal\\sanion")) {

						if((prod.replaceAll("[^A-Za-z0-9]", "").equalsIgnoreCase("Anion") || metabolite.matches("Metal\\sanion")) || 
								(prod.matches("Metal\\sanion") && dictionary.isChildOf("Metal ion", metabolite.replaceAll("\\d*\\-*", "")))){

							product = product.replace(productMetabolites.get(prod), metabolite);
							accept = true;
						}	
					}	
					else if((metabolite.equalsIgnoreCase("Ion") || metabolite.matches("Metal\\s\\w*ion")) && (prod.matches("\\w+\\+") || prod.matches("\\w+\\-") || prod.matches("Metal\\s\\w*ion"))) {

						if((metabolite.replaceAll("[^A-Za-z0-9]", "").equalsIgnoreCase("Ion") || prod.matches("Metal\\s\\w*ion")) || 
								(metabolite.matches("Metal\\s\\w*ion") && dictionary.isChildOf("Metal ion", prod.replaceAll("\\d*\\+*\\-*", "")))){

							product = product.replace(productMetabolites.get(prod), prod);		//different case where the reaction is more specific than the metabolite
							accept = true;
						}	
					}
					else if((metabolite.equalsIgnoreCase("Anion") || metabolite.matches("Metal anion")) && (prod.matches("\\w+\\-") || prod.matches("Metal anion"))) {

						if((metabolite.replaceAll("[^A-Za-z0-9]", "").equalsIgnoreCase("Anion") || prod.matches("Metal anion")) || 
								(metabolite.matches("Metal anion") && dictionary.isChildOf("Metal ion", prod.replaceAll("\\d*\\-*", "")))){

							product = product.replace(productMetabolites.get(prod), prod);		//different case where the reaction is more specific than the metabolite
							accept = true;
						}	
					}
					else if((metabolite.equalsIgnoreCase("Cation") || metabolite.matches("Metal cation")) && (prod.matches("\\w+\\+") || prod.matches("Metal cation"))) {

						if((metabolite.replaceAll("[^A-Za-z0-9]", "").equalsIgnoreCase("Cation") || prod.matches("Metal cation")) || 
								(metabolite.matches("Metal cation") && dictionary.isChildOf("Metal ion", prod.replaceAll("\\d*\\+*", "")))){

							product = product.replace(productMetabolites.get(prod), prod);		//different case where the reaction is more specific than the metabolite
							accept = true;
						}	
					}
					else if(prod.replaceAll("[^A-Za-z0-9]", "").equalsIgnoreCase("Anion") && metabolite.matches(".*ate")){

						product = product.replace(productMetabolites.get(prod), metabolite);
						accept = true;
					}
				}
			}

			if(accept)
				confidenceLevel = count;
		}

		count++;

		//general childs

		if(!accept) {

			for(String react : reactantMetabolites.keySet()) {

				for(String metabolite : metabolites) {

					if(dictionary.isChildOf(react, metabolite)) {

						reactant = reactant.replace(reactantMetabolites.get(react), metabolite);
						accept = true;
					}

					else if(dictionary.isChildOf(metabolite, react)) {

						reactant = reactant.replace(reactantMetabolites.get(react), react);
						accept = true;
					}
				}
			}

			for(String prod : productMetabolites.keySet()) {

				for(String metabolite : metabolites) {

					if(dictionary.isChildOf(prod, metabolite)) {

						product = product.replace(productMetabolites.get(prod), metabolite);
						accept = true;
					}

					else if(dictionary.isChildOf(metabolite, prod)) {

						product = product.replace(productMetabolites.get(prod), prod);
						accept = true;
					}
				}
			}

			if(accept)
				confidenceLevel = count;
		}


		count++;

		//solutes

		if(!accept) {

			for(String react : reactantMetabolites.keySet()) {

				//				if(react.matches(""))

				for(String metabolite : metabolites) {

					if(GENERAL_METABOLITES.contains(react)
							&& (!metabolites.contains(TcdbMetabolitesContainer.EMPTY) && !metabolite.equalsIgnoreCase("Unknown"))
							&& (!metabolite.matches("\\w+\\+") && !metabolite.matches("\\w+\\-"))) {

						reactant = reactant.replace(reactantMetabolites.get(react), metabolite);
						accept = true;
					}

					else if(GENERAL_METABOLITES.contains(metabolite)
							&& (!metabolites.contains(TcdbMetabolitesContainer.EMPTY) && !metabolite.equalsIgnoreCase("Unknown"))
							&& (!metabolite.matches("\\w+\\+") && !metabolite.matches("\\w+\\-"))) {

						reactant = reactant.replace(reactantMetabolites.get(react), react);
						accept = true;
					}

				}
			}

			for(String prod : productMetabolites.keySet()) {

				for(String metabolite : metabolites) {

					if(GENERAL_METABOLITES.contains(prod)
							&& (!metabolites.contains(TcdbMetabolitesContainer.EMPTY) && !metabolite.equalsIgnoreCase("Unknown"))
							&& (!metabolite.matches("\\w+\\+") && !metabolite.matches("\\w+\\-"))){

						product = product.replace(productMetabolites.get(prod), metabolite);
						accept = true;

					}

					else if(GENERAL_METABOLITES.contains(metabolite)
							&& (!metabolites.contains(TcdbMetabolitesContainer.EMPTY) && !metabolite.equalsIgnoreCase("Unknown"))
							&& (!metabolite.matches("\\w+\\+") && !metabolite.matches("\\w+\\-"))){

						product = product.replace(productMetabolites.get(prod), prod);
						accept = true;

					}
				}
			}

			if(accept)
				confidenceLevel = count;
		}

		count++;

		if(!accept) {		//the same as before but ions are accepted

			boolean go = true;

			for(String metabolite : metabolites) {	//to avoid A(in) + A(out) -> A (in) + A (out)
				if(reactantMetabolites.containsKey(metabolite) || productMetabolites.containsKey(metabolite)) {
					go = false;
				}
			}

			if(go) {
				for(String react : reactantMetabolites.keySet()) {

					for(String metabolite : metabolites) {



						if(GENERAL_METABOLITES.contains(react) &&
								(!metabolites.contains(TcdbMetabolitesContainer.EMPTY) && !metabolite.equalsIgnoreCase("Unknown"))) {

							reactant = reactant.replace(reactantMetabolites.get(react), metabolite);
							accept = true;
						}

					}
				}

				for(String prod : productMetabolites.keySet()) {

					for(String metabolite : metabolites) {

						if(GENERAL_METABOLITES.contains(prod) &&
								(!metabolites.contains(TcdbMetabolitesContainer.EMPTY) && !metabolite.equalsIgnoreCase("Unknown"))){

							product = product.replace(productMetabolites.get(prod), metabolite);
							accept = true;

						}

					}
				}
			}
			if(accept)
				confidenceLevel = count;
		}

		count++;

		if(!accept) {		//small molecules accept ions

			for(String react : reactantMetabolites.keySet()) {

				if(react.equalsIgnoreCase("Small molecule")) {

					for(String metabolite : metabolites) {

						reactant = reactant.replace(reactantMetabolites.get(react), metabolite);
						accept = true;

					}
				}

			}

			for(String prod : productMetabolites.keySet()) {

				if(prod.equalsIgnoreCase("Small molecule")) {

					for(String metabolite : metabolites) {

						product = product.replace(productMetabolites.get(prod), metabolite);
						accept = true;

					}
				}

			}

			if(accept)
				confidenceLevel = count;
		}

		count++;
		//at least one match in reactants or products

		if(!accept) {
			for(String react : reactantMetabolites.keySet()) {

				for(String metabolite : metabolites) {

					if(react.replaceAll("[^A-Za-z0-9]", "").equalsIgnoreCase(metabolite.replaceAll("[^A-Za-z0-9]", ""))){

						reactant = reactant.replace(reactantMetabolites.get(react), metabolite);
						accept = true;

					}

					else if(react.replace("D-", "").replaceAll("[^A-Za-z0-9]", "").equalsIgnoreCase(metabolite.replaceAll("[^A-Za-z0-9]", ""))){

						reactant = reactant.replace(reactantMetabolites.get(react), metabolite);
						accept = true;

					}

					else if(react.replace("L-", "").replaceAll("[^A-Za-z0-9]", "").equalsIgnoreCase(metabolite.replaceAll("[^A-Za-z0-9]", ""))){

						reactant = reactant.replace(reactantMetabolites.get(react), metabolite);
						accept = true;

					}
				}
			}

			for(String prod : productMetabolites.keySet()) {

				for(String metabolite : metabolites) {

					if(prod.replaceAll("[^A-Za-z0-9]", "").equalsIgnoreCase(metabolite.replaceAll("[^A-Za-z0-9]", ""))) {

						product = product.replace(productMetabolites.get(prod), metabolite);
						accept = true;
					}
					else if(prod.replace("D-", "").replaceAll("[^A-Za-z0-9]", "").equalsIgnoreCase(metabolite.replaceAll("[^A-Za-z0-9]", ""))) {

						product = product.replace(productMetabolites.get(prod), metabolite);
						accept = true;
					}
					else if(prod.replace("L-", "").replaceAll("[^A-Za-z0-9]", "").equalsIgnoreCase(metabolite.replaceAll("[^A-Za-z0-9]", ""))) {

						product = product.replace(productMetabolites.get(prod), metabolite);
						accept = true;
					}
				}
			}

			if(accept) {
				confidenceLevel = count;
			}

		}

		count++;

		if(!accept) {

			if(metabolites.contains("Unknown") || metabolites.contains(TcdbMetabolitesContainer.EMPTY)) {

				for(String react : reactantMetabolites.keySet())
					reactant = reactant.replace(reactantMetabolites.get(react), react);

				for(String prod : productMetabolites.keySet())
					product = product.replace(productMetabolites.get(prod), prod);

				accept = true;
				confidenceLevel = count;
			}
		}

		if(accept) {

			String[] result = new String[3];

			result[0] = reactant.replaceAll("\\s+", " ").trim();
			result[1] = product.replaceAll("\\s+", " ").trim();
			result[2] = "C"+confidenceLevel;

			return result;
		}

		return null;
	}

	/**
	 * Correct metabolites names.
	 * 
	 * @param originalReaction
	 * @param reactant
	 * @param product
	 * @return
	 */
	private static String[] correctMetabolitesNames(String originalReaction, String reactant, String product) {

		String[] aux = new String[3];

		if(originalReaction.contains("Di-") && (originalReaction.matches(".*m*M*onocarboxylate.*") || originalReaction.matches(".*t*T*ricarboxylate.*"))) {

			originalReaction = originalReaction.replace("Di-", "Dicarboxylate");
			reactant = reactant.replace("Di-", "Dicarboxylate");
			product = product.replace("Di-", "Dicarboxylate");

		}
		if(originalReaction.contains("D-") && originalReaction.matches(".*L*l*\\-L*l*actate.*")) {

			originalReaction = originalReaction.replace("D-", "D-lactate");
			reactant = reactant.replace("D-", "D-lactate");
			product = product.replace("D-", "D-lactate");

		}
		if(originalReaction.contains("Mono-") && (originalReaction.matches(".*d*D*icarboxylate.*") || originalReaction.matches(".*t*T*ricarboxylate.*"))) {

			originalReaction = originalReaction.replace("Mono-", "Monocarboxylate");
			reactant = reactant.replace("Mono-", "Monocarboxylate");
			product = product.replace("Mono-", "Monocarboxylate");

		}
		if(originalReaction.contains("including H+")) {

			originalReaction = originalReaction.replace("including H+", "");
			reactant = reactant.replace("including H+", "");
			product = product.replace("including H+", "");

		}

		aux[0] = originalReaction;
		aux[1] = reactant;
		aux[2] = product;

		return aux;
	}

	/**
	 * Method to process Antiport reactions
	 * 
	 * @param reactionContainer
	 * @param newTcContainer
	 * @param familyTC
	 * @return
	 */
	public static TcNumberContainer processAntiportReactions(ReactionContainer reactionContainer, TcNumberContainer tcContainer,
			TcNumberContainer newTcContainer, List<String> metabolites, Synonyms dictionary) throws Exception {

		String originalReaction = reactionContainer.getReaction();

		String[] aux = correctMetabolitesNames(originalReaction, reactionContainer.getReactant(), reactionContainer.getProduct());

		originalReaction = aux[0];
		String reactant = aux[1];
		String product = aux[2];


		List<String> reactants = getAllPossibleReactantsorProducts(reactant);
		List<String> products = getAllPossibleReactantsorProducts(product);

		Map<Integer, Integer> positions = associateReactantsToProducts(reactants, products);

		for(int key : positions.keySet()) {

			String[] result = checkReactionsMetabolites(reactants.get(key),  products.get(positions.get(key)), metabolites, dictionary, reactionContainer.getTransportType(), originalReaction);

			if(result != null) {

				ReactionContainer newReactContainer = new ReactionContainer(result[0], result[1], reactionContainer.isReversible());

				newReactContainer.setTransportType(reactionContainer.getTransportType());
				newReactContainer.setProperties(reactionContainer.getProperties());
				newReactContainer.setOriginalReaction(originalReaction);
				newReactContainer.setConfidenceLevel(result[2]);

				for(String s : metabolites) {
					if(s.endsWith(TcdbMetabolitesContainer.SAME_METABOLITE_COMBINATION_SUFFIX))
						newReactContainer.setCombineSameMetabolite(true);
				}

				newTcContainer.addReaction(newReactContainer);
			}
		}
		return newTcContainer;

	}

	/**
	 * Method to check if a system or subfamily family has evidence of efflux or uptake.
	 * 
	 * @param proteinDescription
	 * @return
	 */
	private static Boolean isUptake(List<String> descriptions) {

		try {
			boolean uptake = false;
			boolean efflux = false;

			for(String description : descriptions) {

				if(description.matches("(?i).*uptake[\\s*\\.*]*.*")
						|| description.matches("(?i).*import[\\s*\\.*]*.*"))
					uptake = true;

				if(description.matches("(?i).*efflux[\\s*\\.*]*.*") || description.matches("(?i).*excrete[\\s*\\.*]*.*") 
						|| description.matches("(?i).*output[\\s*\\.*]*.*") || description.matches("(?i).*export[\\s*\\.*]*.*"))
					efflux = true;

				if(uptake && efflux)
					return null;
			}

			if(!uptake && !efflux)
				return null;

			if(uptake)
				return true;
			else
				return false;
		} 
		catch (Exception e) {
			return null;
		}
	}

	/**
	 * @param reactants
	 * @param products
	 * @return
	 */
	private static Map<Integer, Integer> associateReactantsToProducts(List<String> reactants, List<String> products) {

		Map<Integer, Integer> positions = new HashMap<>();

		int common = 0;
		int maxCommon = 0;

		for(int i = 0; i < reactants.size(); i++) {

			String auxReact = reactants.get(i).replaceAll("\\(in\\)", "").replaceAll("\\(out\\)", "").replace("\\+", "");		//this replace("\\+", "") might not be correct

			String[] auxReactMetab = auxReact.split("\\s+");

			for(int j = 0; j < products.size(); j++) {

				String auxProd = products.get(j).replaceAll("\\(in\\)", "").replaceAll("\\(out\\)", "").replace("\\+", "");		//this replace("\\+", "") might not be correct

				String[] auxProdMetab = auxProd.split("\\s+");

				for(int x = 0; x < auxReactMetab.length; x++) {

					for(int z = 0; z < auxProdMetab.length; z++) {

						if(auxReactMetab[x].equalsIgnoreCase(auxProdMetab[z]))
							common++;

					}

					if(positions.containsKey(i)) {

						if(maxCommon < common) {

							positions.put(i, j);
							maxCommon = common;
						}
					}
					else {
						positions.put(i, j);
						maxCommon = common;
					}


				}

				common = 0;
			}
			maxCommon = 0;
		}

		return positions;

	}

	/**
	 * Get all possible reactants or products from a reaction
	 * 
	 * @param reaction
	 * @return
	 */
	private static List<String> getAllPossibleReactantsorProducts(String reaction) {

		List<String> reactions = new ArrayList<>();

		Map<Integer, Map<String, String>> positions = getMetabolitesAndRegexFromReaction(reaction);


		//				System.out.println(positions);

		Map<String, String> regex = new HashMap<>();

		for(int key : positions.keySet()) {

			for(String met : positions.get(key).keySet()) {

				regex.put(met, positions.get(key).get(met));
			}
		}

		List<Set<String>> all = new ArrayList<>();
		List<Set<String>> main = new ArrayList<>();

		for(int i : positions.keySet()) {

			if(i == 0) {
				for(String met : positions.get(i).keySet()) {

					Set<String> set = new HashSet<>();

					set.add(met);
					main.add(set);
				}
			}
			else
				all.add(positions.get(i).keySet());

		}

		List<Set<String>> distributions = executeDistributions(main, all, 0);

		//		System.out.println(distributions);

		for(Set<String> set : distributions) {

			String newReaction = new String(reaction);

			for(String cmpd : set) {

				if(!regex.get(cmpd).isEmpty()) {
					newReaction = newReaction.replace(regex.get(cmpd), cmpd);
				}

			}
			reactions.add(newReaction);

		}

		return reactions;
	}
}
