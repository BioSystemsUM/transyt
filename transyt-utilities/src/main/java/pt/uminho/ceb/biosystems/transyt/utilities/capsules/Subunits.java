package pt.uminho.ceb.biosystems.transyt.utilities.capsules;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Subunits {
	
	//CONSIDER DELETE ALL CODE

	private Map<String, Map<String, Double>> evalues;
	private Map<String, Map<String, Set<String>>> subunitsBlast;
	private Map<String, Set<String>> subunitsDatabase;
	private Map<String, Map<String, Boolean>> areAllSubunitsPresent;

	public Subunits() {

		this.evalues = new HashMap<>();
		this.subunitsBlast = new HashMap<>();
		this.subunitsDatabase = new HashMap<>();
		this.areAllSubunitsPresent = new HashMap<>();

	}

	/**
	 * @param subunitsDatabase the subunitsDatabase to set
	 */
	public void setSubunitsDatabase(Map<String, Set<String>> subunitsDatabase) {

		this.subunitsDatabase = subunitsDatabase;

		for(String key : subunitsBlast.keySet()) {

			Map<String, Boolean> allPresent = new HashMap<>();

			for(String tcNumber : subunitsBlast.get(key).keySet()) {

				if(subunitsDatabase.containsKey(tcNumber)) {

					if(subunitsBlast.get(key).get(tcNumber).containsAll(subunitsDatabase.get(tcNumber)))
						allPresent.put(tcNumber, true);
					else
						allPresent.put(tcNumber, false);
				}
			}

			areAllSubunitsPresent.put(key, allPresent);
		}
	}

	public void addEntry(String entry, Map<String, Double> evaluesEntry, Map<String, Set<String>> subunitsEntry) {

		this.subunitsBlast.put(entry, subunitsEntry);
		this.evalues.put(entry, evaluesEntry);

	}

	public Map<String, Set<String>> getSubunits(String acc){

		if(subunitsBlast.containsKey(acc)) {

			Map<String, Set<String>> blast = subunitsBlast.get(acc);
			Map<String, Double> evaluesBlast = evalues.get(acc);

			boolean search = true;

			int i = 0;

			Set<String> toIgnore = new HashSet<>();

			while(search && i < evaluesBlast.size()) {

				String min = "";
				double minVal = 1000000.0;

				for(String tcNumber : evaluesBlast.keySet()) {

					if(minVal > evaluesBlast.get(tcNumber) && !toIgnore.contains(tcNumber)) {

						min = tcNumber;
						minVal = evaluesBlast.get(tcNumber);
					}
				}

				if(areAllSubunitsPresent.get(acc).get(min)) {
					Map<String, Set<String>> map = new HashMap<>();
					map.put(min, blast.get(min));

					return map;
				}
				else
					toIgnore.add(min);
			}
		}
		else 
			return null;


		return null;
	}

	/**
	 * @return the subunitsDatabase
	 */
	public Map<String, Set<String>> getSubunitsDatabase() {
		return subunitsDatabase;
	}
}
