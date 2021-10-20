package pt.uminho.ceb.biosystems.transyt.utilities.capsules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pt.uminho.ceb.biosystems.transyt.utilities.enumerators.Enumerators.CoTransportedCompound;
import pt.uminho.ceb.biosystems.transyt.utilities.enumerators.Enumerators.Direction;
import pt.uminho.ceb.biosystems.transyt.utilities.enumerators.Enumerators.TypeOfTransporter;


/**
 * @author Davide
 *
 */
public class ReactionContainer {

	public static final String REVERSIBLE_TOKEN = "$<<>>$";
	public static final String IRREVERSIBLE_TOKEN = "$>>$";
	public static final String REV_TOKEN = "\\$\\<\\<\\>\\>\\$";
	public static final String IRREV_TOKEN = "\\$\\>\\>\\$";

	public static final String INTERIOR_COMPARTMENT = "in";
	public static final String MIDDLE_COMPARTMENT = "middle";
	public static final String EXTERIOR_COMPARTMENT = "out";
	public static final String INTERIOR_COMPARTMENT_TOKEN = "(in)";
	public static final String MIDDLE_COMPARTMENT_TOKEN = "(middle)";
	public static final String EXTERIOR_COMPARTMENT_TOKEN = "(out)";
	public static final String INTERIOR_COMPARTMENT_TOKEN_REG = "\\(in\\)";
	public static final String MIDDLE_COMPARTMENT_TOKEN_REG = "\\(middle\\)";
	public static final String EXTERIOR_COMPARTMENT_TOKEN_REG= "\\(out\\)";

	public static final String i = "i";		//flag to construct the IDs of irrev reactions
	public static final String LIGHT_NAME = "light";  //check if this is the name that is assigned in the compounds dictionary
	public static final String ELECTRON_NAME = "electron";  //check if this is the name that is assigned in the compounds dictionary

	private String originalReaction = "";
	private String product = "";
	private String reactant = "";
	private boolean combineSameMetabolite = false;
	private Boolean reversible = false;
	private Direction direction = null;
	private TypeOfTransporter transportType;
	private Map<String, String> properties = new HashMap<>();
	private String reactionID = null;
	private String metaReactionID = null;
	private String confidenceLevel = "0";
	private Map<String, String> metabolitesIDs;
	private Set<String> metabolites = new HashSet<>();
	private Set<String> reactants = new HashSet<>();
	private Set<String> products = new HashSet<>();
	private String tcNumber; //only to use in the last stage (calculations)
	private String compartmentalizedMetaReactionID = null;
	private boolean oxidase = false;
	private boolean reductase = false;
	//	private String uncompartmentalizedMetaReactionID = null;

	private String reactionBase;
	private String reactionKEGG;
	private String reactionModelSEED;
	private String reactionBiGG;
	private String reactionMetaCyc;

	private String reactionWithIDs;
	private List<String> modelseedReactionIdentifiers;

	/**
	 * Container to store information regarding a single reaction;
	 * 
	 * @param product
	 * @param reactant
	 * @param reversible
	 */
	public ReactionContainer(String reactant, String product, boolean reversible) {

		this.product = preprocessReactant(product);
		this.reactant = preprocessReactant(reactant);
		this.reversible = reversible;

		saveMetabolites(reactant, false);
		saveMetabolites(product, true);
	}

	/**
	 * Container to store information regarding a single reaction;
	 * 
	 * @param product
	 * @param reactant
	 * @param reversible
	 */
	public ReactionContainer(String reaction, boolean reversible) {

		String regex = IRREV_TOKEN;

		if(reversible)
			regex = REV_TOKEN;

		String[] splitReaction = reaction.split(regex);

		this.reactant = preprocessReactant(splitReaction[0]);
		this.product = preprocessReactant(splitReaction[1]);

		this.reversible = reversible;

		saveMetabolites(reactant, false);
		saveMetabolites(product, true);
	}

	public String preprocessReactant(String text) {
		
		text = text.replaceAll(INTERIOR_COMPARTMENT_TOKEN_REG + "\\s+" + INTERIOR_COMPARTMENT_TOKEN_REG, INTERIOR_COMPARTMENT_TOKEN_REG)
				.replaceAll(EXTERIOR_COMPARTMENT_TOKEN_REG + "\\s+" + EXTERIOR_COMPARTMENT_TOKEN_REG, EXTERIOR_COMPARTMENT_TOKEN_REG)
				.replaceAll(MIDDLE_COMPARTMENT_TOKEN_REG + "\\s+" + MIDDLE_COMPARTMENT_TOKEN_REG, MIDDLE_COMPARTMENT_TOKEN_REG);
		
		return text.trim();
	}
	
	/**
	 * Saves the metabolites of a given a reaction.
	 * 
	 * @param reactant
	 */
	private void saveMetabolites(String reactant, boolean product) {

		String text[] = reactant.replaceAll(INTERIOR_COMPARTMENT_TOKEN_REG, "").replaceAll(EXTERIOR_COMPARTMENT_TOKEN_REG, "")
				.replaceAll(MIDDLE_COMPARTMENT_TOKEN_REG, "").split(" \\+ ");

		for(String met : text) {
			
			met = met.replaceAll("^\\d.*\\s+", "");
			
			metabolites.add(met.trim());
			if(product)
				this.products.add(met.trim());
			else
				this.reactants.add(met.trim());
		}
	}

	/**
	 * Changes the reactant of the reaction.
	 * 
	 * @param key
	 * @param value
	 */
	public void replaceReactant(String reactant) {

		this.reactant = reactant;

	}

	/**
	 * Changes the product of the reaction.
	 * 
	 * @param key
	 * @param value
	 */
	public void replaceProduct(String product) {

		this.product = product;

	}

	/**
	 * Add property to properties map.
	 * 
	 * @param key
	 * @param value
	 */
	public void addProperty(String key, String value) {

		properties.put(key, value);

	}

	/**
	 * @return the properties
	 */
	public Map<String, String> getProperties() {
		return properties;
	}

	/**
	 * @return the product
	 */
	public String getProduct() {
		return product;
	}

	/**
	 * @return the reactant
	 */
	public String getReactant() {
		return reactant;
	}

	/**
	 * @return the reversible
	 */
	public Boolean isReversible() {
		return reversible;
	}

	/**
	 * @param reversible the reversible to set
	 */
	public void setReversible(Boolean reversible) {
		this.reversible = reversible;
	}

	/**
	 * @return the transportType
	 */
	public TypeOfTransporter getTransportType() {
		return transportType;
	}

	/**
	 * @param transportType the transportType to set
	 */
	public void setTransportType(TypeOfTransporter transportType) {
		this.transportType = transportType;
	}

	/**
	 * Get reaction in string format.
	 * 
	 * @return
	 */
	public String getReaction() {

		String reaction = reactant.concat(" ");

		if(reversible)
			reaction = reaction.concat(REVERSIBLE_TOKEN);
		else
			reaction =reaction.concat(IRREVERSIBLE_TOKEN);	

		reaction = reaction.concat(" ").concat(product);

		return reaction;

	}

	/**
	 * @param product the product to set
	 */
	public void setProduct(String product) {
		this.product = product;
	}

	/**
	 * @param reactant the reactant to set
	 */
	public void setReactant(String reactant) {
		this.reactant = reactant;
	}

	/**
	 * @return the reactionID
	 */
	public String getReactionID() {
		return reactionID;
	}

	/**
	 * @param reactionID the reactionID to set
	 */
	public void setReactionID(String reactionID) {
		this.reactionID = reactionID;
	}

	/**
	 * @param properties the properties to set
	 */
	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	/**
	 * @return the originalReaction
	 */
	public String getOriginalReaction() {
		return originalReaction;
	}

	/**
	 * @param originalReaction the originalReaction to set
	 */
	public void setOriginalReaction(String originalReaction) {
		this.originalReaction = originalReaction;
	}

	/**
	 * @return the originalReaction
	 */
	public String getDirectionToken() {

		if(this.reversible)
			return REVERSIBLE_TOKEN;
		else
			return IRREVERSIBLE_TOKEN;

	}

	/**
	 * @return the originalReaction
	 */
	public String getDirectionRegex() {

		if(this.reversible)
			return REV_TOKEN;
		else
			return IRREV_TOKEN;

	}


	public String getDirection() {

		if(this.direction != null) {
			if(this.direction.equals(Direction.O))
				return EXTERIOR_COMPARTMENT.concat(" to ").concat(INTERIOR_COMPARTMENT);
			else if(this.direction.equals(Direction.I))
				return INTERIOR_COMPARTMENT.concat(" to ").concat(EXTERIOR_COMPARTMENT);
			else if(this.direction.equals(Direction.R))
				return "reversible";
		}

		return "unknown";
	}


	/**
	 * @param metabolitesMap 
	 * @param allExistingReactions, Integer lastIDGenerated 
	 * @return
	 */
	public String generateTranSyTID(Map<String, String> metabolitesMap, Map<String, String> allExistingReactions, String deleteMe) {

		//		Map<MetaboliteMajorLabel, String> labelsProton = allMetabolites.get("H+");
		//		Map<MetaboliteMajorLabel, String> labelWater = allMetabolites.get("WATER");

		//		System.out.println(reactionBase);

		this.reactionID = "T";
		this.metaReactionID = "TR";

		//		if(this.reactionBase.contains("cpd00067") && this.reactionBase.contains("cpd00971"))
		//			System.out.println(this.reactionBase);


		//		System.out.println(reactionBase);

		//		this.uncompartmentalizedMetaReactionID = ;

		//		System.out.println(uncomparmentalizedReactionID);

		String auxReactant = this.reactionBase.split(getDirectionRegex())[0];
		String auxProduct = this.reactionBase.split(getDirectionRegex())[1];

		String[] aux = auxReactant.split("\\+");

		//		this.uncompartmentalizedMetaReactionID = "";
		this.compartmentalizedMetaReactionID = "";

		CoTransportedCompound coTransComp = null;
		String cmpdNumber = null;
		Integer energyId = null;

		Direction direction = null;
		Map<String, Boolean> outToInCompounds = new HashMap<>();

		boolean skipClassification = false;

		for(int i = 0; i < aux.length; i++) {
			Boolean outToIn = null;
			String comp = "";

			String[] compoundAux = aux[i].trim().split("=");

			String metID = compoundAux[0].trim();

			//			this.uncompartmentalizedMetaReactionID = this.uncompartmentalizedMetaReactionID.concat(compoundAux[0]).concat("_");

			if(aux[i].contains(ReactionContainer.INTERIOR_COMPARTMENT_TOKEN)) {
				comp = "i";
				outToIn = false;
			}
			if(aux[i].contains(ReactionContainer.MIDDLE_COMPARTMENT_TOKEN)) {
				comp = "m";
				//				outToIn = false;
			}
			else if(aux[i].contains(ReactionContainer.EXTERIOR_COMPARTMENT_TOKEN)) {
				comp = "o";
				outToIn = true;
			}
			else if(comp.isEmpty() && (this.transportType.equals(TypeOfTransporter.BiochemicalATP) 
					|| this.transportType.equals(TypeOfTransporter.BiochemicalATP) || this.transportType.equals(TypeOfTransporter.RedoxNADH))) {
				comp = "i";
			}

			if(this.isReversible())
				comp = "";
			else if(comp.isBlank())
				comp = "i";

			this.compartmentalizedMetaReactionID = this.compartmentalizedMetaReactionID.concat(metID).concat(comp).concat("_");

			if(aux.length == 1) {

				if(metID.matches("^\\d+.*")) {
					coTransComp = null;		//this condition avoids the overlaping of identifiers
					cmpdNumber = null;
					skipClassification = true;
				}
				else {
					cmpdNumber = metID.substring(metID.length() - 5);
					direction = Direction.getDirection(isReversible(), outToIn);
				}
			}
			else if(aux.length < 3 && (transportType.equals(TypeOfTransporter.Symport) || transportType.equals(TypeOfTransporter.Antiport))) {

				if(metID.matches("^\\d+.*")) {
					coTransComp = null;		//this condition avoids the overlaping of identifiers
					cmpdNumber = null;
					skipClassification = true;
				}
				else if(coTransComp == null && !skipClassification) {

					//					if(metabolitesMap.get(metID) == null)
					//						System.out.println(deleteMe);

					try {
						coTransComp = CoTransportedCompound.getEnumIfCotransportedCompound(metabolitesMap.get(metID));
					} catch (Exception e) {
						System.out.println(metID);
						e.printStackTrace();
					}

					if((coTransComp == null && cmpdNumber == null)) {
						cmpdNumber = metID.substring(metID.length() - 5);
						direction = Direction.getDirection(isReversible(), outToIn);
					}
				}
				else if(!skipClassification) {
					if(CoTransportedCompound.getEnumIfCotransportedCompound(metabolitesMap.get(metID)) != null) {
						coTransComp = null;		//this condition avoids the reactions like antiport of Na and H from assigning a wrong ID
						cmpdNumber = null;
					}
					else {
						cmpdNumber = metID.substring(metID.length() - 5);
						direction = Direction.getDirection(isReversible(), outToIn);
					}
				}
			}

			if(outToIn != null)
				outToInCompounds.put(metID, outToIn);
		}

		if(transportType.equals(TypeOfTransporter.PEPdependent) || transportType.equals(TypeOfTransporter.Biochemical)) {

			String[] aux2 = auxProduct.split("\\+");

			for(int i = 0; i < aux2.length; i++) {

				String comp = "";

				String[] compoundAux = aux2[i].trim().split("=");

				String metID = compoundAux[0].trim();

				if(aux2[i].contains(ReactionContainer.INTERIOR_COMPARTMENT_TOKEN))
					comp = "i";
				else if(aux2[i].contains(ReactionContainer.MIDDLE_COMPARTMENT_TOKEN))
					comp = "m";
				else if(aux2[i].contains(ReactionContainer.EXTERIOR_COMPARTMENT_TOKEN))
					comp = "o";

				if(this.isReversible())
					comp = "";

				this.compartmentalizedMetaReactionID = this.compartmentalizedMetaReactionID.concat(metID).concat(comp).concat("_");
			}
		}

		if(transportType.equals(TypeOfTransporter.BiochemicalATP) || transportType.equals(TypeOfTransporter.BiochemicalGTP)) {

			int matchesCount = 0;

			energyId = TypeOfTransporter.getEnergyTransportID(transportType, aux.length);

			for(String key : metabolitesMap.keySet()) {
				if(auxReactant.contains(key) && auxProduct.contains(key)) {

					cmpdNumber = key.substring(key.length() - 5);
					matchesCount++;

					direction = Direction.getDirection(isReversible(), outToInCompounds.get(key));

					//					if(auxReactant.matches(".*" + key + ".*\\s+" + EXTERIOR_COMPARTMENT_TOKEN_REG + ".*"))
					//						direction = Direction.getDirection(isReversible(), true);

				}
			}

			if(matchesCount > 1) {
				direction = null;
				cmpdNumber = null;
			}
		}

		if(transportType.equals(TypeOfTransporter.Light)){

			if(outToInCompounds.size() < 4) {
				for(String metID : outToInCompounds.keySet()) {
					if(metabolitesMap.get(metID).equalsIgnoreCase(ReactionContainer.LIGHT_NAME))
						outToInCompounds.remove(metID);
				}
			}

			if(outToInCompounds.size() == 1) {
				cmpdNumber = new ArrayList<>(outToInCompounds.keySet()).get(0);
				cmpdNumber = cmpdNumber.substring(cmpdNumber.length() - 5);
			}

			if(cmpdNumber == null && outToInCompounds.size() == 2) {
				for(String metID : outToInCompounds.keySet()) {
					coTransComp = CoTransportedCompound.getEnumIfCotransportedCompound(metabolitesMap.get(metID));

					if(coTransComp == null)
						cmpdNumber = metID.substring(metID.length() - 5);
				}

				if(coTransComp == null)
					cmpdNumber = null;
			}

		}

		Integer typeId = TypeOfTransporter.getTransportTypeID(this.transportType);

		if(direction == null) {
			direction = decideDirectionBasedOnEvidence(isReversible(), new HashSet<>(outToInCompounds.values()));
		}

		this.reactionID = this.reactionID.concat(direction.toString()).concat(typeId.toString());
		this.direction = direction;

		boolean idComplete = false;

		if(coTransComp != null || energyId != null) {

			try {
				Integer coTypeId = energyId;

				if(coTypeId == null)
					coTypeId = CoTransportedCompound.getTransportTypeID(coTransComp);

				this.reactionID = this.reactionID.concat(coTypeId.toString());

				if(energyId == null && cmpdNumber != null) {
					this.reactionID = this.reactionID.concat(cmpdNumber);
					idComplete = true;
				}
			}
			catch (NullPointerException e) {
				e.printStackTrace();
			}
		}

		if(!idComplete) {
			if((coTransComp == null && (aux.length == 1 || (aux.length == 2 && transportType.equals(TypeOfTransporter.Light)))) 
					&& !skipClassification) {
				this.reactionID = this.reactionID.concat("0").concat(cmpdNumber);
			}
			else if(coTransComp == null) {
				if(this.reactionID.length() == 3) {
					Integer coTypeId = CoTransportedCompound.getTransportTypeID(coTransComp);
					this.reactionID = this.reactionID + coTypeId.toString();
				}

				Integer lastId = getLastReactionId(this.reactionID, allExistingReactions);
				lastId++;

				String lastIdAux = lastId.toString();

				String number = this.reactionID.toString() + lastIdAux;

				while(number.length() < 9) {
					lastIdAux = "0" + lastIdAux.toString();
					number = this.reactionID + lastIdAux.toString(); // "T[A-Z]" + number of 7 digits (include zeros to left)
				}

				this.reactionID = number;
			}
		}

		if(this.reactionID.length() > 9)
			System.out.println("more than 9 chars " + this.reactionID + " for tc system: " + deleteMe);
		else if(this.reactionID.length() < 9)
			System.out.println("less than 9 chars " + this.reactionID + " for tc system: " + deleteMe);
		//		this.uncompartmentalizedMetaReactionID = this.uncompartmentalizedMetaReactionID.replaceAll("\\s+", "").replaceAll("_$", "");
		this.compartmentalizedMetaReactionID = this.compartmentalizedMetaReactionID.replaceAll("\\s+", "").replaceAll("_$", "");

		this.compartmentalizedMetaReactionID = this.metaReactionID.concat(TypeOfTransporter.getTransportTypeAbb(transportType)).concat("__").concat(this.compartmentalizedMetaReactionID);

		if(!isReversible())
			this.compartmentalizedMetaReactionID = i.concat(this.compartmentalizedMetaReactionID);

		this.metaReactionID = this.compartmentalizedMetaReactionID;

		return this.compartmentalizedMetaReactionID;
	}

	/**
	 * @param isReversible
	 * @param values
	 * @return
	 */
	public static Direction decideDirectionBasedOnEvidence(boolean isReversible, Set<Boolean> values) {

		Direction direction = null;

		if(values.size() == 1) {
			if(values.contains(true))
				direction = Direction.getDirection(isReversible, true);
			else if(values.contains(false))
				direction = Direction.getDirection(isReversible, false);

			return direction;
		}

		return Direction.getDirection(null, null);
	}


	public static Integer getLastReactionId(String category, Map<String, String> allExistingReactions) {

		Integer maxValue = -1;

		for(String id : allExistingReactions.values()) {

			if(id.startsWith(category)) {

				Integer value = Integer.valueOf(id.replace(category, ""));

				if(value > maxValue) {
					maxValue = value;
				}
			}
		}

		return maxValue;
	}

	/**
	 * @return the confidenceLevel
	 */
	public String getConfidenceLevel() {
		return confidenceLevel;
	}

	/**
	 * @param confidenceLevel the confidenceLevel to set
	 */
	public void setConfidenceLevel(String confidenceLevel) {
		this.confidenceLevel = confidenceLevel;
	}

	/**
	 * @return the metabolitesIDs
	 */
	public Map<String, String> getMetabolitesIDs() {
		return metabolitesIDs;
	}

	/**
	 * @param metabolitesIDs the metabolitesIDs to set
	 */
	public void setMetabolitesIDs(Map<String, String> metabolitesIDs) {
		this.metabolitesIDs = metabolitesIDs;
	}

	/**
	 * @return the reactionWithIDs
	 */
	public String getReactionMetaCyc() {
		return reactionMetaCyc;
	}

	/**
	 * @param reactionWithIDs the reactionWithIDs to set
	 */
	public void setReactionMetaCyc(String reactionMetaCyc) {
		this.reactionMetaCyc = reactionMetaCyc;
	}

	/**
	 * @return the reactionKEGG
	 */
	public String getReactionKEGG() {
		return reactionKEGG;
	}

	/**
	 * @param reactionKEGG the reactionKEGG to set
	 */
	public void setReactionKEGG(String reactionKEGG) {
		this.reactionKEGG = reactionKEGG;
	}

	/**
	 * @return the reactionModelSEED
	 */
	public String getReactionModelSEED() {
		return reactionModelSEED;
	}

	/**
	 * @param reactionModelSEED the reactionModelSEED to set
	 */
	public void setReactionModelSEED(String reactionModelSEED) {
		this.reactionModelSEED = reactionModelSEED;
	}

	/**
	 * @return the reactionBiGG
	 */
	public String getReactionBiGG() {
		return reactionBiGG;
	}

	/**
	 * @param reactionBiGG the reactionBiGG to set
	 */
	public void setReactionBiGG(String reactionBiGG) {
		this.reactionBiGG = reactionBiGG;
	}

	/**
	 * @return the metabolites
	 */
	public Set<String> getMetabolites() {
		return metabolites;
	}

	/**
	 * @return the tcNumber
	 */
	public String getTcNumber() {
		return tcNumber;
	}

	/**
	 * @param tcNumber the tcNumber to set
	 */
	public void setTcNumber(String tcNumber) {
		this.tcNumber = tcNumber;
	}

	/**
	 * @return the reactionWithIDs
	 */
	public String getReactionWithIDs() {
		return reactionWithIDs;
	}

	/**
	 * @param reactionWithIDs the reactionWithIDs to set
	 */
	public void setReactionWithIDs(String reactionWithIDs) {
		this.reactionWithIDs = reactionWithIDs;
	}

	/**
	 * @return the reactionBase
	 */
	public String getReactionBase() {
		return reactionBase;
	}

	/**
	 * @param reactionBase the reactionBase to set
	 */
	public void setReactionBase(String reactionBase) {
		this.reactionBase = reactionBase;
	}

	public String getMetaReactionID() {
		return metaReactionID;
	}

	/**
	 * @param metaReactionID the metaReactionID to set
	 */
	public void setMetaReactionID(String metaReactionID) {
		this.metaReactionID = metaReactionID;
	}

	/**
	 * @return the compartmentalizedReactionID
	 */
	public String getCompartmentalizedReactionID() {
		return this.compartmentalizedMetaReactionID;
	}

	public void invertDirection() {

		String temp = "(temp)";

		this.reactant = this.reactant.replace(" " + INTERIOR_COMPARTMENT_TOKEN , " " + temp);
		this.reactant = this.reactant.replace(" " + EXTERIOR_COMPARTMENT_TOKEN, " " + INTERIOR_COMPARTMENT_TOKEN);
		this.reactant = this.reactant.replace(temp, " " + EXTERIOR_COMPARTMENT_TOKEN);

		this.product = this.product.replace(" " + INTERIOR_COMPARTMENT_TOKEN , " " + temp);
		this.product = this.product.replace(" " + EXTERIOR_COMPARTMENT_TOKEN, " " + INTERIOR_COMPARTMENT_TOKEN);
		this.product = this.product.replace(temp, " " + EXTERIOR_COMPARTMENT_TOKEN);


	}

	/**
	 * @return the combineSameMetabolite
	 */
	public boolean isCombineSameMetabolite() {
		return combineSameMetabolite;
	}

	/**
	 * @param combineSameMetabolite the combineSameMetabolite to set
	 */
	public void setCombineSameMetabolite(boolean combineSameMetabolite) {
		this.combineSameMetabolite = combineSameMetabolite;
	}

	/**
	 * @return the modelseedReactionIdentifiers
	 */
	public List<String> getModelseedReactionIdentifiers() {
		return modelseedReactionIdentifiers;
	}

	/**
	 * @param id the modelseedReactionIdentifiers to set
	 */
	public void addModelseedReactionIdentifier(String id) {

		if(this.modelseedReactionIdentifiers == null)
			this.modelseedReactionIdentifiers = new ArrayList<>();

		this.modelseedReactionIdentifiers.add(id);
	}
	
	/**
	 * @param id the modelseedReactionIdentifiers to set
	 */
	public void setModelseedReactionIdentifier(String[] ids) {

		this.modelseedReactionIdentifiers = Arrays.asList(ids);
	}
	
	/**
	 * @param id the modelseedReactionIdentifiers to set
	 */
	public void setModelseedReactionIdentifier(List<String> ids) {

		this.modelseedReactionIdentifiers = ids;
	}

	/**
	 * @return the reactants
	 */
	public Set<String> getReactants() {
		return reactants;
	}

	/**
	 * @return the products
	 */
	public Set<String> getProducts() {
		return products;
	}

	/**
	 * @return the oxidase
	 */
	public boolean isOxidase() {
		return oxidase;
	}

	/**
	 * @param oxidase the oxidase to set
	 */
	public void setOxidase(boolean oxidase) {
		this.oxidase = oxidase;
	}

	/**
	 * @return the reductase
	 */
	public boolean isReductase() {
		return reductase;
	}

	/**
	 * @param reductase the reductase to set
	 */
	public void setReductase(boolean reductase) {
		this.reductase = reductase;
	}

}
