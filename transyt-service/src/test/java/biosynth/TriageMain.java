package biosynth;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;

import pt.uminho.ceb.biosystems.transyt.service.blast.Blast;
import pt.uminho.ceb.biosystems.transyt.service.kbase.ModelSEEDRealatedOperations;
import pt.uminho.ceb.biosystems.transyt.service.kbase.Tools;
import pt.uminho.ceb.biosystems.transyt.service.reactions.ProvideTransportReactionsToGenes;
import pt.uminho.ceb.biosystems.transyt.service.transytDatabase.TransytGraphDatabaseService;
import pt.uminho.ceb.biosystems.transyt.service.transytDatabase.TransytNeo4jInitializer;
import pt.uminho.ceb.biosystems.transyt.utilities.files.FilesUtils;
import pt.uminho.ceb.biosystems.transyt.utilities.transytUtilities.Properties;

class TriageMain {
	
//	@Test
	void test() {

//		int count = 1;

//		double[] blastThresholds = {1E-30, 1E-60, 1E-90};
//		double[] queryCoverages = {0.4, 0.6, 0.8};
		
		Map<String, String> modelMetabolites = FilesUtils.readMapFromFile("C:\\Users\\Davide\\OneDrive - Universidade do Minho\\UMinho\\Tese\\KBase\\Reports\\ecoli_Validation30\\metabolitesFormulas.txt");
		
		String path = "C:\\\\Users\\\\Davide\\\\OneDrive - Universidade do Minho\\\\UMinho\\\\Tese\\\\KBase\\\\Genomes\\\\Escherichia coli str. K-12 substr. MG1655 - NCBI\\GCF_000005845.2_ASM584v2_protein.faa";
		
//		String path = "C:\\\\Users\\\\Davide\\\\OneDrive - Universidade do Minho\\\\UMinho\\\\Tese\\\\KBase\\\\Genomes\\\\Escherichia coli str. K-12 substr. MG1655 - NCBI\\GCF_000005845.2_ASM584v2_proteinC.faa";
		
//		new ProvideTransportReactionsToGenes(null, path, modelMetabolites, new Properties());		//29421 - Nitrobacter vulgaris

	}
	
//	@Test
	void batchTest() {

		int count = 1;

		double[] blastThresholds = {1E-30, 1E-60, 1E-90};
		double[] queryCoverages = {0.4, 0.6, 0.8};

		Set<String> paths = FilesUtils.readWordsInFile("C:\\Users\\Davide\\OneDrive - Universidade do Minho\\UMinho\\Tese\\KBase\\Reports\\paths.txt");
		
		for(String path : paths) {

			for(double blastThresh : blastThresholds) {

				for(double queryCoverage : queryCoverages) {

					File file = new File(path);

					String taxPath = file.getParent().concat("\\taxID.txt");

					Set<String> ids = FilesUtils.readWordsInFile(taxPath);

					int taxID = Integer.valueOf(ids.toString().replaceAll("\\[", "").replaceAll("\\]", "").trim());
					
					Set<String> modelMetabolites = Tools.readModelMetabolites(file.getParent().concat("\\ModelCompounds.xlsx"));

					Properties properties = new Properties();

					properties.setQueryCoverage(queryCoverage);
					properties.seteValueThreshold(blastThresh);
					
					System.out.println(path);

//					new ProvideTransportReactionsToGenes(taxID, path, modelMetabolites, properties);		//29421 - Nitrobacter vulgaris

					System.out.println(count + " job complete");
					System.out.println();
					System.out.println();
					System.out.println();
					System.out.println();
					System.out.println();
					System.out.println();

					count++;
				}	//309800 - Haloferax volcanii
			}			
		}//392416 - lactobacilus
	}
	
//	@Test
	void readCompoundsTest() {
		
//		String path = "C:\\Users\\Davide\\Downloads\\test\\test.xlsx";

//		Set<String> set = Tools.readModelMetabolites(path);
		
		Map<String, Map<String, String>> compounds = ModelSEEDRealatedOperations.readFile();
		
		
		System.out.println(compounds.get("cpd00048").get("name"));
	}
	
//	@Test
//	void databaseTestSpeed() {
//		
//		try {
//
//			LocalDateTime currentTime = LocalDateTime.now();
//			System.out.println(currentTime.getHour() + "h" + currentTime.getMinute() + "m" + currentTime.getSecond() + "s" );
//			
//			GraphDatabaseService graphDatabaseService = TransytNeo4jInitializer.getDatabaseService(true, new Properties());
//
//			TransytGraphDatabaseService service = new TransytGraphDatabaseService(graphDatabaseService);
//
//			Transaction dataTx = graphDatabaseService.beginTx();
//			
//			ResourceIterator<Node> allNodes = service.getAllNodes().iterator();
//			
//			int i = 0;
//			
//			while(allNodes.hasNext()) {
//				allNodes.next();
//				i++;
//			}
//			
//			System.err.println(i);
//			
//			dataTx.close();
//			service.shutdown();
//			graphDatabaseService.shutdown();
//			
//			currentTime = LocalDateTime.now();
//			System.out.println(currentTime.getHour() + "h" + currentTime.getMinute() + "m" + currentTime.getSecond() + "s" );
//
//
//		} 
//		catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
}
