package pt.uminho.ceb.biosystems.transyt.scraper.tcdb.utilities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.uminho.ceb.biosystems.transyt.utilities.capsules.TcdbMetabolitesContainer;
import pt.uminho.ceb.biosystems.transyt.utilities.dictionary.Synonyms;

public class ProcessTcdbMetabolites {

	public static Map<String, TcdbMetabolitesContainer> processData(List<String[]> table) {

		//		Map<String, Integer> counter = new HashMap();

		Map<String, TcdbMetabolitesContainer> containers = new HashMap<>();

		Synonyms dictionary = new Synonyms();

		for(String[] line : table) {

			String[] id = line[0].split("@");

			String uniprotAcc = id[0];
			String tcNumber = id[1];

			if(line[2].matches(".*Nucleobase-ascorbate.*")) {		//create file with these special cases

				line[2] = line[2].replace("Nucleobase-ascorbate", "Nucleobase, ascorbate");
			}

			if(line[2].matches(".*Purine-cytosine.*")) {

				line[2] = line[2].replace("Purine-cytosine", "Purine, cytosine");
			}

			if(line[2].matches(".*Lysine-arginine.*")) {

				line[2] = line[2].replace("Lysine-arginine", "Lysine, arginine");
			}

			line[2] = line[2].replace(" and ", ", ").replaceAll("N-\\s*,", "");

			String[] metabolites = line[2].split(", ");

			//			Utilities.printArray(metabolites);

			for(int i = 0; i < metabolites.length; i++) {

				String word = dictionary.getSynonym(metabolites[i].toLowerCase().replaceAll("\\s+", ""));

				if(word != null) {

					metabolites[i] = word;
				}
			}

			//			Utilities.printArray(metabolites);

			//			System.out.println();

			String description = line[4];
			//			
			//			for(int i = 0; i < metabolites.length; i++) {
			//				
			//				if(counter.containsKey(metabolites[i])) {
			//					
			//					int count = counter.get(metabolites[i].toLowerCase().trim()) + 1;
			//					
			//					counter.put(metabolites[i].toLowerCase().trim(), count);
			//				}
			//				else
			//					counter.put(metabolites[i].toLowerCase().trim(), 1);
			//			}
			//			
			if(!containers.containsKey(uniprotAcc))
				containers.put(uniprotAcc, new TcdbMetabolitesContainer(uniprotAcc));

			containers.get(uniprotAcc).addEntry(tcNumber, description, metabolites);

		}

		//		for(String key : counter.keySet())
		//			System.out.println(key + "\t" + counter.get(key));

		return containers;
	}

	/**
	 * Get TC numbers' description.
	 * 
	 * @param excel
	 * @return
	 */
	public static Map<String, String> getTCDescriptions(List<String[]> table) {

		Map<String, String> descriptions = new HashMap<>();

		for(String[] line : table) {

			String[] id = line[0].split("@");

			String tcNumber = id[1];
			String description = line[4];

			if(!descriptions.containsKey(tcNumber))
				descriptions.put(tcNumber, description);

		}
		return descriptions;
	}

}