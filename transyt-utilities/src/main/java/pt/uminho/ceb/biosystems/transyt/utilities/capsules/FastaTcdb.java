package pt.uminho.ceb.biosystems.transyt.utilities.capsules;

public class FastaTcdb {

	private  String accession = null;
	private  String sequence = null;
	private  String tcNumber = null;
	private  String organism = null;
	private  String description = null;
	
	public FastaTcdb(String id, String sequence, String tcNumber, String organism, String description) {
		
		this.accession = id;
		this.organism = organism;
		this.sequence = sequence;
		this.tcNumber = tcNumber;
		this.description = description;
		
	}

	/**
	 * @return the id
	 */
	public  String getAccession() {
		return accession;
	}

	/**
	 * @return the sequence
	 */
	public  String getSequence() {
		return sequence;
	}

	/**
	 * @return the tcNumber
	 */
	public  String getTcNumber() {
		return tcNumber;
	}

	/**
	 * @return the organism
	 */
	public  String getOrganism() {
		return organism;
	}

	/**
	 * @return the description
	 */
	public  String getDescription() {
		return description;
	}
	
}
