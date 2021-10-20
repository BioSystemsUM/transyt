package pt.uminho.ceb.biosystems.transyt.utilities.capsules;

import pt.uminho.ceb.biosystems.transyt.utilities.enumerators.Enumerators.KINGDOM;
import pt.uminho.ceb.biosystems.transyt.utilities.enumerators.Enumerators.STAIN;

public class Organism {
	
	Integer taxonomyID;
	String organism;
	String[] taxonomy;
	STAIN stain;
	KINGDOM kingdom;

	public Organism(Integer taxonomyID, String organism, String[] taxonomy, STAIN stain, KINGDOM kingdom) {
		
		this.taxonomyID = taxonomyID;
		this.taxonomy = taxonomy;
		this.organism = organism;
		this.stain = stain;
		this.kingdom = kingdom;
		
	}

	/**
	 * @return the taxonomyID
	 */
	public Integer getTaxonomyID() {
		return taxonomyID;
	}

	/**
	 * @return the organism
	 */
	public String getOrganism() {
		return organism;
	}

	/**
	 * @return the taxonomy
	 */
	public String[] getTaxonomy() {
		return taxonomy;
	}

	/**
	 * @return the stain
	 */
	public STAIN getStain() {
		return stain;
	}

	/**
	 * @return the kingdom
	 */
	public KINGDOM getKingdom() {
		return kingdom;
	}
	
}
