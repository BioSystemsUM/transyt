package pt.uminho.ceb.biosystems.transyt.utilities.capsules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pt.uminho.ceb.biosystems.transyt.utilities.enumerators.Enumerators.TypeOfTransporter;
import pt.uminho.ceb.biosystems.transyt.utilities.transytUtilities.DistributionsAlgorithm;

/**
 * @author Davide
 *
 */
public class TcdbMetabolitesContainer {

	public static final String EMPTY = "Empty";
	public static final String ELECTRON = "Electron";
	public static final String UNKNOWN = "Unknown";
	public static final String PROTON = "H+";
	public static final String ATP = "ATP";
	public static final String ADP = "ADP";
	public static final String PHOSPHATE = "Pi";
	public static final String PYRUVATE = "pyruvate";
	public static final String PHOSPHOENOLPYRUVATE = "PEP";
	public static final String SAME_METABOLITE_COMBINATION_SUFFIX = "___%%%";	

	private String uniprotID;

	private Set<String> tcNumbers;
	private Map<String, List<String>> metabolitesMap;
	private Map<String, String> descriptionsMap;

	/**
	 * Container with information retrieved from tcdb about the transported metabolites by each transpot system.
	 * 
	 * @param uniprotID
	 */
	public TcdbMetabolitesContainer(String uniprotID) {

		this.uniprotID = uniprotID;

		this.tcNumbers = new HashSet<>();
		this.metabolitesMap = new HashMap<>();
		this.descriptionsMap = new HashMap<>();

	}

	/**
	 * @return the uniprotID
	 */
	public String getUniprotID() {
		return uniprotID;
	}

	/**
	 * Add entry to the container
	 * 
	 * @param tcNumber
	 * @param description
	 * @param metabolites
	 */
	public void addEntry(String tcNumber, String description, String[] metabolites) {

		tcNumbers.add(tcNumber);
		descriptionsMap.put(tcNumber, description);

		List<String> met = new ArrayList<>();

		for(int i = 0; i < metabolites.length; i ++) {

			if(!metabolites[i].trim().isEmpty())
				met.add(metabolites[i].trim());
		}

		if(met.isEmpty())
			met.add(EMPTY);

		metabolitesMap.put(tcNumber, met);

	}

	/**
	 * Get the metabolites of a given tcnumber
	 * 
	 * @param tcNumber
	 * @return
	 */
	public List<String> getMetabolites(String tcNumber){

		return metabolitesMap.get(tcNumber);
	}
	
	/**
	 * Get the metabolites of a given tcnumber
	 * 
	 * @param tcNumber
	 * @return
	 */
	public void addMetabolites(String tcNumber, Set<String> metabolites){

		this.metabolitesMap.get(tcNumber).addAll(metabolites);
	}

	/**
	 * Get the description of a given tcnumber
	 * 
	 * @param tcNumber
	 * @return
	 */
	public String getDescription(String tcNumber){

		return descriptionsMap.get(tcNumber);
	}

	/**
	 * @return the tcNumbers
	 */
	public Set<String> getTcNumbers() {
		return tcNumbers;
	}

	/**
	 * Get all combinations of ions and metabolites to generate new possible reactions.
	 * 
	 * @param tcNumber
	 * @return
	 */
	public Set<List<String>> getMetabolitesCombinations(String tcNumber, Boolean antiportOrSymport, TypeOfTransporter evidence, 
			String acc, Set<String> generalMetabolites){

		//		if(tcNumber.equals("2.A.1.11.1")) {
		//			
		//			System.out.println("RRRR>>> " + antiportOrSymport);
		//			
		//		}


		if(antiportOrSymport == null)
			return auxCombinations(tcNumber, acc, evidence);

		else if(antiportOrSymport) {

			List<Set<String>> sets = auxCombinationsAntiport(tcNumber, acc, generalMetabolites);

			Set<List<String>> combinations = new HashSet<>();

			for(Set<String> set : sets) //fix this compatibility of results problem
				combinations.add(new ArrayList<>(set));

			return combinations;
		}

		return auxCombinationsAntiport2(tcNumber, acc);
	}

	/**
	 * @param tcNumber
	 * @return
	 */
	private List<Set<String>> auxCombinationsAntiport(String tcNumber, String acc, Set<String> generalMetabolites){

		List<Set<String>> allCombinations = new ArrayList<>();

		List<String> allMetabolites = new ArrayList<>(metabolitesMap.get(tcNumber));

		//		if(allMetabolites.size() == 1) {
		//			
		//			for(String met : allMetabolites) {
		//			
		//				if(met.isEmpty()) {
		//					
		//					allMetabolites.clear();
		//					allMetabolites2.clear();
		//					allMetabolites.add(EMPTY);
		//					allMetabolites2.add(EMPTY);
		////					System.out.println("vazio");
		//				}
		//			}
		//		}

		//		if(tcNumber.equals("2.A.1.14.1")) {
		//			System.out.println(allMetabolites);
		//		}

		if(allMetabolites.size() == 1) {	
			
			Set<String> set = new HashSet<>();

			if(generalMetabolites.contains(allMetabolites.get(0))) {

				set.add(allMetabolites.get(0));
				set.add(allMetabolites.get(0).concat(SAME_METABOLITE_COMBINATION_SUFFIX));	//add this entry to the dictionary
				
				allCombinations.add(set);
			}
			else {
				//it is not possible to perform antiport with less than two metabolite
				for(String met : allMetabolites) {

					if(!met.equals("Unknown") && !met.equals(EMPTY) && !met.equals(PROTON)) {

						set.add(met);
						set.add(PROTON);

						allCombinations.add(set);
					}
				}
			}
			return allCombinations;
		}

		Set<String> ions = new HashSet<>();
		Set<String> normal = new HashSet<>();
		List<Set<String>> sets = new ArrayList<>();

		for(String met : allMetabolites) {

			if(met.matches(".*[\\+\\-]$")) {
				ions.add(met);
			}
			else {
				normal.add(met);
			}
		}

		sets.add(ions);
		sets.add(normal);

		allCombinations = DistributionsAlgorithm.getAllDistributions(sets, false);
		
		if(allCombinations.isEmpty()) {
			
			allCombinations = DistributionsAlgorithm.bruteForceCombinations(allMetabolites);
		}

		return allCombinations;
	}

	/**
	 * @param tcNumber
	 * @param acc
	 * @return
	 */
	private Set<List<String>> auxCombinationsAntiport2(String tcNumber, String acc) {

		Set<List<String>> allCombinations = new HashSet<>();

		Set<String> allMetabolites = new HashSet<>(metabolitesMap.get(tcNumber));

		//		if(allMetabolites.size() == 1) {
		//			
		//			for(String met : allMetabolites) {
		//			
		//				if(met.isEmpty()) {
		//					
		//					allMetabolites.clear();
		//					allMetabolites.add(EMPTY);
		////					System.out.println("vazio");
		//				}
		//			}
		//		}

		for(String met1 : allMetabolites) {

			List<String> set = new ArrayList<>();

			set.add(met1);

			allCombinations.add(set);
		}

		return allCombinations;
	}

	/**
	 * @param tcNumber
	 * @return
	 */
	private Set<List<String>> auxCombinations(String tcNumber , String acc, TypeOfTransporter evidence){

		//		System.out.println("entrou aqui1");

		Set<List<String>> allCombinations = new HashSet<>();

		List<String> allMetabolites = new ArrayList<>(metabolitesMap.get(tcNumber));

		//		if(tcNumber.equals("2.A.1.11.1"))
		//			System.out.println("metabolites >> " + metabolitesMap.get(tcNumber).size()  + "\t" + metabolitesMap.get(tcNumber));

		Set<String> ions =  new HashSet<>();

		if(allMetabolites.size() < 2) {

			//			System.out.println("entrou aqui2");
			//			System.out.println(allMetabolites.size());

			//			if(allMetabolites.isEmpty()) {
			//				allMetabolites.add(EMPTY);
			//			}
			//			else if(allMetabolites.size() == 1) {
			//				
			//				for(String met : allMetabolites) {
			//					
			////					if(tcNumber.equals("2.A.1.11.1")) {
			////						System.out.println("metab$" + met + "$");
			////						System.out.println(met.isEmpty());
			////					}
			//						
			//					if(met.isEmpty()) {
			//						
			//						allMetabolites.clear();
			//						allMetabolites.add(EMPTY);
			////						System.out.println("vazio");
			//					}
			//				}
			//			}
			//			


			//			System.out.println(allMetabolites);

			allCombinations.add(allMetabolites);

			return allCombinations;
		}

		for(String metabolite : metabolitesMap.get(tcNumber)) {

			if((metabolite.matches("\\w+\\+") || metabolite.contains("\\w+\\-")) && !metabolite.contains("\\w+\\-\\w")) {

				allMetabolites.remove(metabolite);
				ions.add(metabolite);
			}
		}

		if(ions.size() > 0 && allMetabolites.size() > 0 && (evidence != null && !evidence.equals(TypeOfTransporter.Uniport))) {		//for combinations of metabolites with ions

			for(String metabolite : allMetabolites) {

				for(String ion : ions) {

					List<String> newCombination = new ArrayList<>();

					newCombination.add(ion);
					newCombination.add(metabolite);

					allCombinations.add(newCombination);
				}
			}
		}
		else if(ions.size() > 0  && (evidence != null && !evidence.equals(TypeOfTransporter.Uniport))) {	//when there's only ions

			for(String ion : ions) {

				List<String> newCombination = new ArrayList<>();

				newCombination.add(ion);

				allCombinations.add(newCombination);
			}
		}
		else {

			for(String metabolite : allMetabolites) {

				List<String> newCombination = new ArrayList<>();

				newCombination.add(metabolite);

				allCombinations.add(newCombination);
			}
			for(String ion : ions) {

				List<String> newCombination = new ArrayList<>();

				newCombination.add(ion);

				allCombinations.add(newCombination);
			}
		}

		return allCombinations;
	}
}
