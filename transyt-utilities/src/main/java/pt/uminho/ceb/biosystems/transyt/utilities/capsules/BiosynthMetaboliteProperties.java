package pt.uminho.ceb.biosystems.transyt.utilities.capsules;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BiosynthMetaboliteProperties {
	
	public static final String NONE = "NONE";

	private String source;
	private String entryID = NONE;
	private Set<String> synonyms;
	private String formula = NONE;
	private String remark;
	private String bestReferenceURL = NONE;
	private String bestReferenceID = NONE;
	private String bestReferenceSource = NONE;
	private Map<String, Map<String, Object>> allReferences;
	private Set<Long> inherentNodeIDChilds;
	
	
	public BiosynthMetaboliteProperties(String entryID, String source, Set<String> synonyms) {
		
		this.source = source;
		this.entryID = entryID;
		this.synonyms = new HashSet<>();
		this.allReferences = new HashMap<>();
		this.synonyms = synonyms;
		
	}


	/**
	 * @return the synonyms
	 */
	public Set<String> getSynonyms() {
		return synonyms;
	}


	/**
	 * @param synonyms the synonyms to set
	 */
	public void setSynonyms(Set<String> synonyms) {
		this.synonyms = synonyms;
	}


	/**
	 * @return the formula
	 */
	public String getFormula() {
		return formula;
	}


	/**
	 * @param formula the formula to set
	 */
	public void setFormula(String formula) {
		
		if(formula.equalsIgnoreCase("null") || formula.equalsIgnoreCase("none"))
			formula = null;
			
		this.formula = formula;
	}


	/**
	 * @return the references
	 */
	public String getRemark() {
		return remark;
	}


	/**
	 * @param references the references to set
	 */
	public void setRemark(String references) {
		this.remark = references;
	}

	/**
	 * @return the entry
	 */
	public String getEntry() {
		return entryID;
	}


	/**
	 * @return the references
	 */
	public Map<String, Map<String, Object>> getReferences() {
		return allReferences;
	}


	/**
	 * @param references the references to set
	 */
	public void setReferences(Map<String, Map<String, Object>> references) {
		this.allReferences = references;
	}


	/**
	 * @return the source
	 */
	public String getSource() {
		return source;
	}


	/**
	 * @param source the source to set
	 */
	public void setSource(String source) {
		this.source = source;
	}


	/**
	 * @return the bestReferenceSource
	 */
	public String getBestReferenceSource() {
		return bestReferenceSource;
	}


	/**
	 * @param bestReferenceSource the bestReferenceSource to set
	 */
	public void setBestReferenceSource(String bestReferenceSource) {
		this.bestReferenceSource = bestReferenceSource;
	}


	/**
	 * @return the bestReferenceID
	 */
	public String getBestReferenceID() {
		return bestReferenceID;
	}

	/**
	 * @param bestReferenceID the bestReferenceID to set
	 */
	public void setBestReferenceID(String bestReferenceID) {
		this.bestReferenceID = bestReferenceID;
	}

	/**
	 * @return the keggLink
	 */
	public String getBestReferenceURL() {
		return bestReferenceURL;
	}

	/**
	 * @param keggLink the keggLink to set
	 */
	public void setBestReferenceURL(String bestReferenceURL) {
		this.bestReferenceURL = bestReferenceURL;
	}


	/**
	 * @return the inherentNodeIDChilds
	 */
	public Set<Long> getInherentNodeIDChilds() {
		return inherentNodeIDChilds;
	}


	/**
	 * @param inherentNodeIDChilds the inherentNodeIDChilds to set
	 */
	public void setInherentNodeIDChilds(Set<Long> inherentNodeIDChilds) {
		this.inherentNodeIDChilds = inherentNodeIDChilds;
	}
}
