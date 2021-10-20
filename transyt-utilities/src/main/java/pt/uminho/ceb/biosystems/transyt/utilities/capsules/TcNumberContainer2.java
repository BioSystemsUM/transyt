package pt.uminho.ceb.biosystems.transyt.utilities.capsules;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TcNumberContainer2 {

	private Map<Integer, ReactionContainer> reactions;
	private Integer id = 0;
	private String superFamily;
	private String family;
	private String tcNumber;
	private Set<String> mainReactions = new HashSet<>(); //not coming from childs
	private Set<String> generatedWithCommonOntology = new HashSet<>();
	
	/**
	 * Container with all information about a given transporter. 
	 */
	public TcNumberContainer2(String tcNumber){
		
		this.tcNumber = tcNumber;
		this.reactions = new HashMap<>();
		
	}
	
	/**
	 * Method to add reaction to the container.
	 * 
	 * @param reaction
	 * @param transporter
	 * @param reversibility
	 */
	public void addReaction(ReactionContainer reaction) {
		
		reactions.put(id, reaction);
		
		id++; 
	}
	
	/**
	 * Get all ids stored in the container.
	 * 
	 * @return
	 */
	public Set<Integer> getAllReactionsIds(){
		return reactions.keySet();
	}

	/**
	 * @return the reactions
	 */
	public Map<Integer, ReactionContainer> getReactionsByID() {
		return reactions;
	}

	/**
	 * @return the superFamily
	 */
	public String getSuperFamily() {
		return superFamily;
	}

	/**
	 * @param superFamily the superFamily to set
	 */
	public void setSuperFamily(String superFamily) {
		this.superFamily = superFamily;
	}

	/**
	 * @return the family
	 */
	public String getFamily() {
		return family;
	}

	/**
	 * @param family the family to set
	 */
	public void setFamily(String family) {
		this.family = family;
	}
	
	/**
	 * Get reaction container by ID.
	 * 
	 * @return
	 */
	public ReactionContainer getReactionContainer(Integer id){
		
		if(reactions.keySet().contains(id))
			return reactions.get(id);
		
		return null;
	}

	/**
	 * @return the tcNumber
	 */
	public String getTcNumber() {
		return tcNumber;
	}

	/**
	 * @return the mainReactions
	 */
	public Set<String> getMainReactions() {
		return mainReactions;
	}

	/**
	 * @param mainReactions the mainReactions to set
	 */
	public void addMainReaction(String mainReactions) {
		this.mainReactions.add(mainReactions);
	}

	/**
	 * @return the generatedWithCommonOntology
	 */
	public Set<String> getGeneratedWithCommonOntology() {
		return generatedWithCommonOntology;
	}
	
	/**
	 * @param generatedWithCommonOntology the generatedWithCommonOntology to set
	 */
	public void addReactionGeneratedByCommonOntology(String generatedWithCommonOntology) {
		this.generatedWithCommonOntology.add(generatedWithCommonOntology);
	}
	
	/**
	 * Get all transyt identifiers assigned to this tc number
	 * 
	 * @return
	 */
	public Set<String> getAllReactionTransytIds(){
		
		Set<String> ids = new HashSet<>();
		
		for(Integer i : reactions.keySet())
			ids.add(reactions.get(i).getReactionID());
		
		return ids;
	}

}
