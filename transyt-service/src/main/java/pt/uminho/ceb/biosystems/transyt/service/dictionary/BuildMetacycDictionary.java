package pt.uminho.ceb.biosystems.transyt.service.dictionary;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import pt.uminho.ceb.biosystems.transyt.service.biosynth.initializeNeo4jdb;
import pt.uminho.sysbio.biosynth.integration.io.dao.neo4j.MetaboliteMajorLabel;
import pt.uminho.sysbio.biosynth.integration.neo4j.BiodbMetaboliteNode;
import pt.uminho.sysbio.biosynthframework.BiodbGraphDatabaseService;

public class BuildMetacycDictionary {
	
	private static final String PATH_META = "C:\\Users\\Davide\\Documents\\Backup\\BASE DE DADOS BIOSYNTH -\\db\\1.0\\META\\compound";
	private static final String PATH_ECOLI = "C:\\Users\\Davide\\Documents\\Backup\\BASE DE DADOS BIOSYNTH -\\db\\1.0\\ECOLI\\compound";
	
	public static void main(String[] args) {	//currently not in use - may not be necessary

		try {
			GraphDatabaseService graphDatabaseService = initializeNeo4jdb.getDataDatabase(null);
			Transaction dataTx = graphDatabaseService.beginTx();
			
			BiodbGraphDatabaseService service = new BiodbGraphDatabaseService(graphDatabaseService);
			
			@SuppressWarnings("resource")
			Scanner reader = new Scanner(System.in);
			
			int n = 1;
			
			while (n != 99) {
				
				Map<String, String> names = new HashMap<>();
				
				Set<String> queries = getAllQueriesToSearch(service, MetaboliteMajorLabel.MetaCyc);
						
				for(String query : queries)
					names.put(query, parser(PATH_META.concat("\\").concat(query)));
				
				queries = getAllQueriesToSearch(service, MetaboliteMajorLabel.EcoCyc);
				
				for(String query : queries)
					names.put(query, parser(PATH_ECOLI.concat("\\").concat(query)));
				
				System.out.println("Entradas: " + names.size());
				
				System.out.println("Enter a random number to repeat or 99 to finish: ");

				n = reader.nextInt();

			}
			dataTx.failure();
			dataTx.close();
			service.shutdown();
			graphDatabaseService.shutdown();
			System.out.println("Shutdown!!!");
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get the correct name of the metabolite from metacyc.
	 * 
	 * @param path
	 * @return
	 */
	public static String parser(String path) {	//uncomment when javax problem is solved, or change this method

//		try {
//			
//			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//
//			DocumentBuilder builder = factory.newDocumentBuilder();		
//			Document doc = builder.parse(new File(path));
//			
//			NodeList compoundList = doc.getElementsByTagName("molecule");
//
//			for(int i = 0; i < compoundList.getLength(); i++) {
//
//				Node c = compoundList.item(i);
//
//				if(c.getNodeType() == Node.ELEMENT_NODE) {
//
//					Element compound = (Element) c;
//
//					if(compound.hasAttribute("title"))
//						return compound.getAttribute("title").replaceAll("%2b", "");
//
//				}
//			}
//			
//			return null;
//
//		}
//		catch (Exception e) {
//			e.printStackTrace();
//		}
//
		return null;
	}
	
	/**
	 * Get all files from a given folder.
	 * 
	 * @param path
	 * @return
	 */
	public static File[] getAllXmlFiles(String path){
		
		try {
			
			File folder = new File(path);
			
			return folder.listFiles();
			
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * Get all queries to searched in the files.
	 * 
	 * @param service
	 * @return
	 */
	public static Set<String> getAllQueriesToSearch(BiodbGraphDatabaseService service, MetaboliteMajorLabel label){
		
		try {
			
			Set<String> queries = new HashSet<>();
			
			Set<BiodbMetaboliteNode> metabolites = service.listMetabolites(label);
			
			for(BiodbMetaboliteNode met : metabolites)
				queries.add(met.getEntry());
				
			return queries;
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}	
}
