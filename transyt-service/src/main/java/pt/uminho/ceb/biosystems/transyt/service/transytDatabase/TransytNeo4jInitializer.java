package pt.uminho.ceb.biosystems.transyt.service.transytDatabase;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.uminho.ceb.biosystems.transyt.service.internalDB.WriteByMetabolitesID;
import pt.uminho.ceb.biosystems.transyt.service.neo4jRest.RestNeo4jGraphDatabase;
import pt.uminho.ceb.biosystems.transyt.utilities.transytUtilities.Properties;

public class TransytNeo4jInitializer {

	private static final Logger logger = LoggerFactory.getLogger(WriteByMetabolitesID.class);

	//	private static final String PATH = FilesUtils.getTranSyTDBDatabaseDirectory();

	/**
	 * initialize TranSyT neo4j database.
	 * 
	 * @return
	 */
	public static RestNeo4jGraphDatabase getDatabaseService(Properties properties) {

		try {

			String uri = properties.getDatabaseURI();
			String username = properties.getDatabaseUsername();
			String password = properties.getDatabasePassword();

			logger.info("Initializing TranSyT graph database...");

			logger.debug("Connecting to TranSyT neo4j database at: {}", properties.getDatabaseURI());

			return new RestNeo4jGraphDatabase(uri, username, password);
		} 
		catch (Exception e) {
			logger.error("Something went wrong while initializing connection to TranSyT's internal database. "
					+ "TranSyT will shut down, please try again later!");
			logger.trace("StackTrace {}",e);

			System.exit(0);
		}

		return null;


		//		if(readOnly) {
		//
		//			graphDb = new GraphDatabaseFactory()
		//					.newEmbeddedDatabaseBuilder(new File(PATH.concat(properties.getTransytDBName())))
		//					.setConfig( GraphDatabaseSettings.read_only, "true" )
		//					.newGraphDatabase();		//faster
		//
		//			logger.info("TranSyT graph database (read-only) open!");
		//		}
		//		else {
		//			graphDb = new GraphDatabaseFactory()
		//					.newEmbeddedDatabase(new File(PATH.concat(properties.getTransytDBName())));
		//
		//			logger.info("TranSyT graph database open!");
		//		}

		//		return graphDb;
	}

	/**
	 * IMPORTANT STEP
	 * Method to search reactionIDs already generated in the previous database in order to inherit the identifiers
	 * 
	 * @param properties
	 * @return
	 */
	public static Map<String, String> getAllReactionsIdentifiersInReferenceDatabase(Properties properties){

		Map<String, String> data = new HashMap<>();

		try {

			String uri = properties.getReferenceDatabaseURI();
			String username = properties.getReferenceDatabaseUsername();
			String password = properties.getReferenceDatabasePassword();

			logger.debug("Connecting to reference TranSyT neo4j database at: {}", properties.getReferenceDatabaseURI());

			RestNeo4jGraphDatabase service = new RestNeo4jGraphDatabase(uri, username, password);

			Map<String, TransytNode> reactions = service.getNodesByLabel(TransytNodeLabel.Reaction, 
					TransytGeneralProperties.ReactionID);

			for(String reactionID : reactions.keySet()) {

				TransytNode node = reactions.get(reactionID);

				String metaID = node.getProperty(TransytGeneralProperties.MetaID);

				data.put(metaID, reactionID); //the mapping must be one to one
			}

			service.close();
		} 
		catch (Exception e) {
			logger.error("Something went wrong while accessing TranSyT's reference database. "
					+ "TranSyT will shut down, please try again later!");
			logger.trace("StackTrace {}",e);

			System.exit(2);
		}

		return data;
	}
	
	/**
	 * Method to return the version of the current live database.
	 * 
	 * @param properties
	 * @return
	 */
	public static String getDatabaseVersion(Properties properties){

		String version = null;

		try {

			String uri = properties.getReferenceDatabaseURI();
			String username = properties.getReferenceDatabaseUsername();
			String password = properties.getReferenceDatabasePassword();

			logger.debug("Connecting to reference TranSyT neo4j database at: {}", properties.getReferenceDatabaseURI());

			RestNeo4jGraphDatabase service = new RestNeo4jGraphDatabase(uri, username, password);

			Map<String, TransytNode> versions = service.getNodesByLabel(TransytNodeLabel.Database_Version, 
					TransytGeneralProperties.Version); //this should return only one node

			for(String db : versions.keySet()) {

				TransytNode node = versions.get(db);

				version = node.getProperty(TransytGeneralProperties.Version).toString();
			}

			service.close();
		} 
		catch (Exception e) {
			logger.error("Something went wrong while accessing TranSyT's reference database. "
					+ "TranSyT will shut down, please try again later!");
			logger.trace("StackTrace {}",e);

			System.exit(2);
		}

		return version;
	}

}
