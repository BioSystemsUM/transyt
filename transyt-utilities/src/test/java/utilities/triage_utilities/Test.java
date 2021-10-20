package utilities.triage_utilities;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import pt.uminho.ceb.biosystems.transyt.utilities.connection.TcdbRetriever;

public class Test{
	
	public static void main(String[] args) {
		
		List<String[]> firstFile = readTCDBScrapedInfo("C:\\Users\\Davide\\Desktop\\rosalind\\workdir\\home\\dlagoa\\dockerTests\\workdir\\scraper\\backup_files\\TCDB_systems_summary\\TCSystems_2h0m32s1612019.json");
		
		List<String[]> lastFile = readTCDBScrapedInfo("C:\\Users\\Davide\\Desktop\\rosalind\\workdir\\home\\dlagoa\\dockerTests\\workdir\\scraper\\backup_files\\TCDB_systems_summary\\TCSystems_23h34m19s1322019.json");
		
		Map<String, String> first = new HashMap<>();
		Map<String, String> last = new HashMap<>();
		
		Set<String> totalMetaboltiesFirst = new HashSet<>();
		Set<String> totalMetaboltiesLast = new HashSet<>();
		
		for(String[] line : firstFile) {
			
			String id = line[0];
			
			String[] metabolites = line[2].split(", ");
			
			for(String m : metabolites)
				totalMetaboltiesFirst.add(m);
			
//			first.put(id, metabolites);
			
			
		}
		
		for(String[] line : lastFile) {
			
			String id = line[0];
			
			String[] metabolites = line[2].split(", ");
			
			for(String m : metabolites)
				totalMetaboltiesLast.add(m);
			
//			last.put(id, metabolites);
		}
		
		
//		for(String key : last.keySet()) {
//			
//			if(first.containsKey(key)) {
//				
//				System.out.println(key + "\t" + first.get(key) + "\t" + last.get(key));
//			}
//			System.out.println(key);
//		}
		
		System.out.println(totalMetaboltiesFirst.size());
		System.out.println(totalMetaboltiesLast.size());
			
		System.out.println("done");
	}
	
	/**
	 * @param results
	 * @param failed
	 */
	public static List<String[]> readTCDBScrapedInfo(String path) {
		
		JSONParser parser = new JSONParser();

		List<String[]> data = new ArrayList<>();

		try {

			Object obj = parser.parse(new FileReader(path));

			JSONObject allObjects = (JSONObject) obj;

			@SuppressWarnings("unchecked")
			Set<String> keys = allObjects.keySet();

			for(String key : keys) {

				JSONObject accessionObject = (JSONObject) allObjects.get(key);

				@SuppressWarnings("unchecked")
				Set<String> tcNumbers = accessionObject.keySet();

				for(String tc : tcNumbers) {
					
					String[] system = new String[5];

					JSONObject sysytemProperties = (JSONObject) accessionObject.get(tc);

					system[0] = key.concat("@").concat(tc);
					system[1] = sysytemProperties.get(TcdbRetriever.ORGANISM).toString();
					system[2] = sysytemProperties.get(TcdbRetriever.SUBSTRATE).toString();
					system[3] = tc;
					system[4] = sysytemProperties.get(TcdbRetriever.DESCRIPTION).toString();
					
					data.add(system);
				}
			}
		}
		catch(Exception e) {
			
			e.printStackTrace();
		}
		return data;
	}

}
