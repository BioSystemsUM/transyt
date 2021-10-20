package pt.uminho.ceb.biosystems.transyt.service.containers;

import java.util.Map;

import pt.uminho.sysbio.biosynth.integration.io.dao.neo4j.MetaboliteMajorLabel;

public class InternalMetabolitesProperties {
	
	private String internalName = "";
	private String biosynthName = "";
	private Map<MetaboliteMajorLabel, String> entries;

	/**
	 * Class to store the internal metabolites' properties.
	 * 
	 * @param internalName
	 * @param biosynthName
	 * @param entries
	 */
	public InternalMetabolitesProperties(String internalName, String biosynthName, Map<MetaboliteMajorLabel, String> entries) {
		
		this.internalName = internalName;
		this.biosynthName = biosynthName;
		this.entries = entries;
	}

	/**
	 * @return the internalName
	 */
	public String getInternalName() {
		return internalName;
	}

	/**
	 * @param internalName the internalName to set
	 */
	public void setInternalName(String internalName) {
		this.internalName = internalName;
	}

	/**
	 * @return the biosynthName
	 */
	public String getBiosynthName() {
		return biosynthName;
	}

	/**
	 * @param biosynthName the biosynthName to set
	 */
	public void setBiosynthName(String biosynthName) {
		this.biosynthName = biosynthName;
	}

	/**
	 * @return the entries
	 */
	public Map<MetaboliteMajorLabel, String> getEntries() {
		return entries;
	}

	/**
	 * @param entries the entries to set
	 */
	public void setEntries(Map<MetaboliteMajorLabel, String> entries) {
		this.entries = entries;
	}
	
}
