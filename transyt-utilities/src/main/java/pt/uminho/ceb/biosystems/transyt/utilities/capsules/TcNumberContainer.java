package pt.uminho.ceb.biosystems.transyt.utilities.capsules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pt.uminho.ceb.biosystems.transyt.utilities.enumerators.Enumerators.TypeOfTransporter;

public class TcNumberContainer{

	private Map<Integer, ReactionContainer> reactions;
	public Integer id = 0;
	private String superFamilyDescription;
	private String familyDescription;
	private String subfamilyDescription;
	private String proteinDescription;

	/**
	 * Container with all information about a given transporter. 
	 */
	public TcNumberContainer(){

		this.reactions = new HashMap<>();

	}

	public TcNumberContainer clone(){ 

		TcNumberContainer container = new TcNumberContainer();

		container.setReactions(new HashMap<>(this.reactions));
		
		if(this.superFamilyDescription != null)
			container.setSuperFamily(new String(this.superFamilyDescription));
		if(this.familyDescription != null)
			container.setFamily(new String(this.familyDescription));
		if(this.subfamilyDescription != null)
			container.setSubfamilyDescription(new String(this.subfamilyDescription));
		if(this.proteinDescription != null)
			container.setProteinDescription(new String(this.proteinDescription));
		
		container.id = this.id;

		return container; 
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
//		transportTypesAssociatedToProtein.add(reaction.getTransportType());

		id++; 
	}

	/**
	 * Get all ids stored in the container.
	 * 
	 * @return
	 */
	public Set<Integer> getAllReactionsIds(){
		return new HashSet<>(reactions.keySet());
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
		return superFamilyDescription;
	}

	/**
	 * @param superFamily the superFamily to set
	 */
	public void setSuperFamily(String superFamily) {
		this.superFamilyDescription = superFamily;
	}

	/**
	 * @return the family
	 */
	public String getFamily() {
		return familyDescription;
	}

	/**
	 * @param family the family to set
	 */
	public void setFamily(String family) {
		this.familyDescription = family;
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
	 * @param id
	 */
	public void removeReaction(Integer id){

		reactions.remove(id);
	}

	/**
	 * @return
	 */
	public Set<TypeOfTransporter> getAllTransportTypesAssociated(){
		
		Set<TypeOfTransporter> transportTypesAssociatedToProtein = new HashSet<>();
		
		for(Integer reaction : this.reactions.keySet())
			transportTypesAssociatedToProtein.add(this.reactions.get(reaction).getTransportType());

		return transportTypesAssociatedToProtein;
	}

	/**
	 * @param type
	 * @return
	 */
	public boolean evidenceTransportTypeExists(TypeOfTransporter type){

		if(type == null)
			return false;

		if(this.getAllTransportTypesAssociated().contains(type))
			return true;

		return false;

	}

	/**
	 * @return the familyDescription
	 */
	public String getFamilyDescription() {
		return familyDescription;
	}

	/**
	 * @param familyDescription the familyDescription to set
	 */
	public void setFamilyDescription(String familyDescription) {
		this.familyDescription = familyDescription;
	}

	/**
	 * @return the proteinDescription
	 */
	public String getProteinDescription() {
		return proteinDescription;
	}

	/**
	 * @param proteinDescription the proteinDescription to set
	 */
	public void setProteinDescription(String proteinDescription) {
		this.proteinDescription = proteinDescription;
	}

	/**
	 * @return the subfamilyDescription
	 */
	public String getSubfamilyDescription() {
		return subfamilyDescription;
	}

	/**
	 * @param subfamilyDescription the subfamilyDescription to set
	 */
	public void setSubfamilyDescription(String subfamilyDescription) {
		this.subfamilyDescription = subfamilyDescription;
	}

	/**
	 * Deletes all reactions not belonging to the correct transport type if given type is available.
	 * 
	 * @param evidence
	 */
	public void filterReactionsNotBelongingToTransportType(TypeOfTransporter evidence) {

		List<Integer> toDelete = new ArrayList<>();
		boolean typeAvailable = false;

		for(Integer id : this.reactions.keySet()) {

			if(this.reactions.get(id).getTransportType().equals(evidence))
				typeAvailable = true;
			else
				toDelete.add(id);
		}

		if(typeAvailable)
			for(Integer id : toDelete) 
				this.reactions.remove(id);
	}

	/**
	 * @return the reactions
	 */
	public Map<Integer, ReactionContainer> getReactions() {
		return reactions;
	}

	/**
	 * @param reactions the reactions to set
	 */
	public void setReactions(Map<Integer, ReactionContainer> reactions) {
		this.reactions = reactions;
	}
}
