package pt.uminho.ceb.biosystems.transyt.utilities.capsules;

import java.util.List;
import java.util.Map;
import java.util.Set;

import pt.uminho.ceb.biosystems.transyt.utilities.enumerators.Enumerators.TypeOfTransporter;

/**
 * @author Davide
 *
 */
public class GeneContainer {

	private int maxTax = 0;

	private String sequence = null;
	private Map<String, Integer> commomTaxaCount = null;
	private Set<String> homologousGenes = null;
	private Map<String, Double> similarities = null;
	private String annotatedFamily = null;
	private List<String> closestTCnumbers = null;
	
	/**
	 * Contain
	 * 
	 * @param gene
	 * @param homologousGenes
	 */
	public GeneContainer(String sequence, Set<String> homologousGenes, Map<String, Integer> commomTaxaCount, 
			Map<String, Double> similarities, int maxTax, String annotatedFamily, List<String> closestTCnumbers){
		
		this.sequence = sequence;
		this.homologousGenes = homologousGenes;
		this.commomTaxaCount = commomTaxaCount;
		this.similarities = similarities;
		this.maxTax = maxTax;
		this.annotatedFamily = annotatedFamily;
		this.closestTCnumbers = closestTCnumbers;
	}
	
//	/**
//	 * @return the transportType
//	 */
//	public Map<String, TypeOfTransporter> getTransportType() {
//		return transportType;
//	}

	/**
	 * @return the sequence
	 */
	public String getSequence() {
		return sequence;
	}

	/**
	 * @return the commomTaxaCount
	 */
	public Map<String, Integer> getCommomTaxaCount() {
		return commomTaxaCount;
	}

	/**
	 * @return the homologousGenes
	 */
	public Set<String> getHomologousGenes() {
		return homologousGenes;
	}

	/**
	 * @return the similarities
	 */
	public Map<String, Double> getSimilarities() {
		return similarities;
	}
	
	/**
	 * @return the maxTax
	 */
	public int getMaxTax() {
		return maxTax;
	}

	/**
	 * @return the annotatedFamily
	 */
	public String getAnnotatedFamily() {
		return annotatedFamily;
	}

	/**
	 * @param annotatedFamily the annotatedFamily to set
	 */
	public void setAnnotatedFamily(String annotatedFamily) {
		this.annotatedFamily = annotatedFamily;
	}

	/**
	 * @return the closestTCnumber
	 */
	public List<String> getClosestTCnumbers() {
		return closestTCnumbers;
	}

	/**
	 * @param closestTCnumber the closestTCnumber to set
	 */
	public void setClosestTCnumbers(List<String> closestTCnumbers) {
		this.closestTCnumbers = closestTCnumbers;
	}

//	/**
//	 * @param homologousGenes the homologousGenes to set
//	 */
//	public void setHomologousGenes(Set<String> homologousGenes) {
//		this.homologousGenes = homologousGenes;
//		
//	}
}
