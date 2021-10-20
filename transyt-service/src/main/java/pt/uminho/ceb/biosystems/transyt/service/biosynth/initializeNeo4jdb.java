package pt.uminho.ceb.biosystems.transyt.service.biosynth;

import java.util.Scanner;
import java.util.Set;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uminho.biosynth.core.data.integration.neo4j.HelperNeo4jConfigInitializer;
import pt.uminho.ceb.biosystems.transyt.utilities.files.FilesUtils;
import pt.uminho.sysbio.biosynth.integration.io.dao.neo4j.MetaboliteMajorLabel;
import pt.uminho.sysbio.biosynth.integration.neo4j.BiodbMetaboliteNode;
import pt.uminho.sysbio.biosynthframework.BiodbGraphDatabaseService;

public class initializeNeo4jdb {
	
	private static final Logger logger = LoggerFactory.getLogger(initializeNeo4jdb.class);

	public static void main(String[] args) {

		GraphDatabaseService graphDatabaseService = getDataDatabase(null);

		Scanner reader = new Scanner(System.in);

		int n = 1;

		Transaction dataTx = graphDatabaseService.beginTx();

		BiodbGraphDatabaseService service = new BiodbGraphDatabaseService(graphDatabaseService);

		while (n != 99) {

			getData(service);

			System.out.println("Enter a number: ");
			n = reader.nextInt();

		}

		reader.close();

		dataTx.failure(); dataTx.close();

		service.shutdown();

		System.out.println("Done!!!");

	}




	public static GraphDatabaseService getDataDatabase(GraphDatabaseService dataDb) {
		if (dataDb == null) {
			
			logger.info("[INIT] graph database...");
			
			dataDb = HelperNeo4jConfigInitializer.initializeNeo4jDataDatabaseConstraints(FilesUtils.getBiosynthDatabaseDirectory());

			logger.info("[INIT] graph database... done!");
		}
		return dataDb;
	}


	public static void getData(BiodbGraphDatabaseService service) {
		//	    GraphDatabaseService graphDatabaseService = getDataDatabase(null);
		//	    Transaction dataTx = graphDatabaseService.beginTx();

		try {

			
			Set<BiodbMetaboliteNode> metabolites = service.listMetabolites(MetaboliteMajorLabel.LigandGlycan);
			
			for(BiodbMetaboliteNode met : metabolites)
				System.out.println(met.getAllProperties());
			
//			for(BiodbMetaboliteNode met : metabolites)
//				System.out.println(met.getEntry());
			
			System.out.println(metabolites.size());
			
			

			}
			catch(NullPointerException e){

				System.out.println("NULL exception");
				e.printStackTrace();

			}
		}



	}
