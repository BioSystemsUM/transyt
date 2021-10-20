package pt.uminho.ceb.biosystems.transyt.utilities.capsules;

import java.util.Set;

public class RowInfo {

	private String id;
	private Set<String> internalSubstrates;
	private Set<String> tcdbSubstrates;
	private String tcNumber;
	private String description;
	private Set<String> transportType;
	private String originalInternalDB;
	
	public RowInfo(String id, String tcNumber, String description, Set<String> internalSubstrates, Set<String> tcdbSubstrates,
			Set<String> transportType, String originalInternalDB){
		
		this.id = id;
		this.tcNumber = tcNumber;
		this.description = description;
		this.internalSubstrates = internalSubstrates;
		this.tcdbSubstrates = tcdbSubstrates;
		this.transportType = transportType;
		this.originalInternalDB = originalInternalDB;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the internalSubstrates
	 */
	public Set<String> getInternalSubstrates() {
		return internalSubstrates;
	}

	/**
	 * @param internalSubstrates the internalSubstrates to set
	 */
	public void setInternalSubstrates(Set<String> internalSubstrates) {
		this.internalSubstrates = internalSubstrates;
	}

	/**
	 * @return the tcdbSubstrates
	 */
	public Set<String> getTcdbSubstrates() {
		return tcdbSubstrates;
	}

	/**
	 * @param tcdbSubstrates the tcdbSubstrates to set
	 */
	public void setTcdbSubstrates(Set<String> tcdbSubstrates) {
		this.tcdbSubstrates = tcdbSubstrates;
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
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the transportType
	 */
	public Set<String> getTransportType() {
		return transportType;
	}

	/**
	 * @param transportType the transportType to set
	 */
	public void setTransportType(Set<String> transportType) {
		this.transportType = transportType;
	}

	/**
	 * @return the originalInternalDB
	 */
	public String getOriginalInternalDB() {
		return originalInternalDB;
	}

	/**
	 * @param originalInternalDB the originalInternalDB to set
	 */
	public void setOriginalInternalDB(String originalInternalDB) {
		this.originalInternalDB = originalInternalDB;
	}
	
}