package pt.uminho.ceb.biosystems.transyt.service.internalDB;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterable;

import pt.uminho.ceb.biosystems.transyt.utilities.capsules.BiosynthMetaboliteProperties;
import pt.uminho.sysbio.biosynthframework.BiodbGraphDatabaseService;

public class RetrieveBiosynthMetabolites {
	
	public static Set<String> getAllMetabolitesNames(BiodbGraphDatabaseService service) {
		
		ResourceIterable<Node> nodes = service.getAllNodes();
		
		Set<String> metabolites = new HashSet<>();
		
		for(Node node : nodes) {
			
			if(node.hasProperty("name")) {
				
				String res = node.getProperty("name").toString().toLowerCase();
				
				String[] newRes = res.split(";");
				
				for(int i = 0; i < newRes.length; i++)
					metabolites.add(newRes[i].replaceAll("[^A-Za-z0-9]","").toLowerCase());
				
			}
		}
		
		return metabolites;
		
	}
	
	public static Map<String, Map<String, String>> getAllMetabolitesIds(Map<String, BiosynthMetaboliteProperties> info){
		
		Map<String, Map<String, String>> map = new HashMap<>();
		
		
		
		
		
		
		
		return null;
	}

}
