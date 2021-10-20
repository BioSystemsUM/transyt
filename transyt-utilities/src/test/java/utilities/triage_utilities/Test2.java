package utilities.triage_utilities;

import java.util.HashMap;
import java.util.Map;

import pt.uminho.ceb.biosystems.transyt.utilities.files.FilesUtils;

public class Test2 {

	public static void main(String[] args) {
		
		String path = "C:\\Users\\BioSystems\\Desktop\\map.txt";
		
		Map<String, String> map = new HashMap<>();
		
		map.put("key", "value");
		map.put("coiso", "coisa");
		
		FilesUtils.addMapToFile(path, map);
	}
	
}
