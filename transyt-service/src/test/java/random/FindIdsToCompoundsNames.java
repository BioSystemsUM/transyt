package random;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import pt.uminho.ceb.biosystems.transyt.service.biosynth.initializeNeo4jdb;
import pt.uminho.ceb.biosystems.transyt.service.containers.BiosynthMetabolites;
import pt.uminho.ceb.biosystems.transyt.service.internalDB.FetchCompoundsByName;
import pt.uminho.ceb.biosystems.transyt.service.internalDB.WriteByMetabolitesID;
import pt.uminho.ceb.biosystems.transyt.service.reactions.IdentifyReactionsMetabolites;
import pt.uminho.ceb.biosystems.transyt.service.utilities.MappingMetabolites;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.BiosynthMetaboliteProperties;
import pt.uminho.ceb.biosystems.transyt.utilities.files.FilesUtils;
import pt.uminho.sysbio.biosynth.integration.io.dao.neo4j.MetaboliteMajorLabel;
import pt.uminho.sysbio.biosynthframework.BiodbGraphDatabaseService;

public class FindIdsToCompoundsNames {

	public static void main(String[] args) throws IOException {

		test1(FilesUtils.readWordsInFile("/Users/davidelagoa/Downloads/listIDs_RiceMultiomics.txt"));
	}

	private static void test1(Set<String> metabolites) {

		try {

			GraphDatabaseService graphDatabaseService = initializeNeo4jdb.getDataDatabase(null);
			Transaction dataTx = graphDatabaseService.beginTx();

			BiodbGraphDatabaseService service = new BiodbGraphDatabaseService(graphDatabaseService);

			BiosynthMetabolites namesAndIDsContainer = new FetchCompoundsByName(service, false).getResults();

			@SuppressWarnings("resource")
			Scanner reader = new Scanner(System.in);
			int n = 1;

			while (n != 99) {

				identify(metabolites, namesAndIDsContainer, service);

				System.out.println("Enter a random number to repeat (100 to repeat data retrieval) or 99 to finish: ");

				try {
					n = reader.nextInt();
				} catch (Exception e) {
					e.printStackTrace();

					n = reader.nextInt();
				}
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
	
	private static void identify(Set<String> metabolites, BiosynthMetabolites namesAndIDsContainer, BiodbGraphDatabaseService service) {
		
		IdentifyReactionsMetabolites metabolitesIdentification =  new IdentifyReactionsMetabolites(metabolites, namesAndIDsContainer, service, MetaboliteMajorLabel.MetaCyc);
		
		Map<String, Map<String, MetaboliteMajorLabel>> metabolitesIDs = metabolitesIdentification.getTcdbMetabolitesIDs();
		
		Map<String, String> mappingModelSEED = MappingMetabolites.getMapping(MetaboliteMajorLabel.ModelSeed, MetaboliteMajorLabel.MetaCyc);
		Map<String, String> mappingKEGG = MappingMetabolites.getMapping(MetaboliteMajorLabel.LigandCompound, MetaboliteMajorLabel.MetaCyc);
		
		Map<String, Map<String, MetaboliteMajorLabel>> results = new HashMap<>();
		
		for(String metabolite : metabolites) {
			
			if(metabolitesIDs.containsKey(metabolite)) {
				
				for(Entry<String, MetaboliteMajorLabel> entry : metabolitesIDs.get(metabolite).entrySet()){
					
					if(!results.containsKey(metabolite))
						results.put(metabolite, new HashMap<String, MetaboliteMajorLabel>());
					
					results.get(metabolite).put(entry.getKey(), entry.getValue());
					
					if(entry.getValue().equals(MetaboliteMajorLabel.ModelSeed) && mappingModelSEED.containsKey(entry.getKey())) {
						results.get(metabolite).put(mappingModelSEED.get(entry.getKey()), MetaboliteMajorLabel.MetaCyc);
					}
					else if(entry.getValue().equals(MetaboliteMajorLabel.LigandCompound) && mappingKEGG.containsKey(entry.getKey())) {
						results.get(metabolite).put(mappingKEGG.get(entry.getKey()), MetaboliteMajorLabel.MetaCyc);
					}
				}
			}
			else
				results.put(metabolite, null);
				
		}
		
		for(String metabolite : results.keySet()) {
			
			System.out.println(metabolite + "\t" + results.get(metabolite));
			
		}
		
	}
}
