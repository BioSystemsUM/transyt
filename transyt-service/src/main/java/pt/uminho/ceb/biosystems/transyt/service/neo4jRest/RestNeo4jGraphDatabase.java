package pt.uminho.ceb.biosystems.transyt.service.neo4jRest;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.neo4j.driver.AccessMode;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
//import org.neo4j.driver.v1.AccessMode;
//import org.neo4j.driver.v1.AuthTokens;
//import org.neo4j.driver.v1.Driver;
//import org.neo4j.driver.v1.GraphDatabase;
//import org.neo4j.driver.v1.Record;
//import org.neo4j.driver.v1.Session;
//import org.neo4j.driver.v1.Statement;
//import org.neo4j.driver.v1.StatementResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.uminho.ceb.biosystems.transyt.service.transytDatabase.TransytGeneralProperties;
import pt.uminho.ceb.biosystems.transyt.service.transytDatabase.TransytNode;
import pt.uminho.ceb.biosystems.transyt.service.transytDatabase.TransytNodeLabel;
import pt.uminho.ceb.biosystems.transyt.service.transytDatabase.TransytRelationship;
import pt.uminho.ceb.biosystems.transyt.service.transytDatabase.TransytRelationshipType;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.ReactionContainer;
import pt.uminho.ceb.biosystems.transyt.utilities.enumerators.Enumerators.TypeOfTransporter;


//    public RestNeo4jGraphDatabase( String uri, String user, String password ) throws SQLException
//    {
//     // Connect
//        Connection con = DriverManager.getConnection(uri);
//
//        // Querying
//        try (Statement stmt = con.createStatement()) {
//            ResultSet rs = stmt.executeQuery("MATCH (n) RETURN n");
//            while (rs.next()) {
//                System.out.println(rs.getInt(0));
//            }
//        }
//        con.close();
//    }

public class RestNeo4jGraphDatabase implements AutoCloseable{
	
	private Driver driver;
	
	private String uri;
	private String username;
	private String password;
	
	private Set<String> temporaryCompounds;
	private Map<String, String> temporaryCompoundsNames;
	private Map<String, String> temporaryCompoundsFormulas;
	private Map<String, String> formulasBackUp;
	
	private static final Logger logger = LoggerFactory.getLogger(RestNeo4jGraphDatabase.class);
	
	public RestNeo4jGraphDatabase(String uri, String username, String password){
		
		this.uri = uri;
		this.username = username;
		this.password = password;
		
		temporaryCompounds = new HashSet<>();
		temporaryCompoundsNames = new HashMap<>();
		temporaryCompoundsFormulas = new HashMap<>();
		
//		driver = GraphDatabase.driver(uri);
		
		driver = GraphDatabase.driver( uri, AuthTokens.basic(username, password ) );
		
	}
	
	public void restartDriver() {
		
		driver = GraphDatabase.driver( uri, AuthTokens.basic( username, password ) );
		
	}
	
	/**
	 * This method is used to change neo4j's database password. This step might be needed when using a default password.
	 * 
	 * @param currentPassword
	 * @param newPassword
	 */
	public void changePassword(String currentPassword, String newPassword) {
		
		Session session = driver.session();
		
		this.password = newPassword;
		
		session.run("ALTER CURRENT USER SET PASSWORD FROM + '" + currentPassword + "' TO '" + newPassword + "'");
		
		logger.info("Neo4j password changed!");
		
		restartDriver();
		
		session.close();
		
	}
	
	/**
	 * Create constraints to avoid duplicated data.
	 */
	public void createContraints() {
		
		Session session = driver.session();
		
		session.run("CREATE CONSTRAINT ON (a:" + TransytNodeLabel.Uniprot_Accession + ") assert a." + TransytGeneralProperties.Accession_Number + " IS UNIQUE");
		session.run("CREATE CONSTRAINT ON (a:" + TransytNodeLabel.TC_Number + ") assert a." + TransytGeneralProperties.TC_Number + " IS UNIQUE");
		session.run("CREATE CONSTRAINT ON (a:" + TransytNodeLabel.Reaction + ") assert a." + TransytGeneralProperties.ReactionID + " IS UNIQUE");
		session.run("CREATE CONSTRAINT ON (a:" + TransytNodeLabel.Transport_Type + ") assert a." + TransytGeneralProperties.Transport_Type + " IS UNIQUE");
		session.run("CREATE CONSTRAINT ON (a:" + TransytNodeLabel.BiGG_Metabolite + ") assert a." + TransytGeneralProperties.MetaboliteID + " IS UNIQUE");
		session.run("CREATE CONSTRAINT ON (a:" + TransytNodeLabel.KEGG_Metabolite + ") assert a." + TransytGeneralProperties.MetaboliteID + " IS UNIQUE");
		session.run("CREATE CONSTRAINT ON (a:" + TransytNodeLabel.MetaCyc_Metabolite + ") assert a." + TransytGeneralProperties.MetaboliteID + " IS UNIQUE");
		session.run("CREATE CONSTRAINT ON (a:" + TransytNodeLabel.ModelSEED_Metabolite + ") assert a." + TransytGeneralProperties.MetaboliteID + " IS UNIQUE");
		session.run("CREATE CONSTRAINT ON (a:" + TransytNodeLabel.Metabolite_Name + ") assert a." + TransytGeneralProperties.Name + " IS UNIQUE");
		
		logger.info("All neo4j database contraints created!");
		
		session.close();
	}
	
	/**
	 * If a constraint was already created on the label and property refered, 
	 * there is no need to add an index because an indexing was already done.
	 * 
	 * @param label
	 * @param property
	 */
	public void createIndexes(TransytNodeLabel label, TransytGeneralProperties property) {
		
		Session session = driver.session();
		
		session.run("CREATE INDEX ON :" + label + "(" + property + ")");
		
		logger.info("All neo4j database indexes created!");
		
		session.close();
		
	}
	
	/** Method to create a node and set one property. Return the node created.
	 * 
	 * @param label
	 * @param properties
	 * @return
	 */
	public TransytNode createNode(TransytNodeLabel label, TransytGeneralProperties key, String value) {
		
		Map<String, String> properties = new HashMap<>();
		
		properties.put(key.toString(), value);

		return createNode(label, properties);
	}
	
	
	/** Method to create a node and set properties. The properties should be given in a map format.
	 * Returns the node created.
	 * 
	 * @param label
	 * @param properties
	 * @return
	 */
	public TransytNode createNode(TransytNodeLabel label, Map<String, String> properties) {
		
		Session session = driver.session();
		
		String auxString = buildPropertiesQuery(properties);
		
		Result res = session.run( "CREATE (n:" + label + " " + auxString + ") "
				+ "RETURN {id:id(n)}" );
		
		while(res.hasNext()) {

			Record record = res.next();
			
			int id = -1;

			String idString = record.get(0).asMap().get("id").toString();

			if(!idString.equalsIgnoreCase("NULL")) {

				id = Integer.valueOf(idString);
			}

			session.close();
			return new TransytNode(id, properties);
		}
		
		session.close();

		return null;
	}
	
	/** Method to create a relationship between two nodes.
	 * 
	 * @param label
	 * @param properties
	 * @return
	 */
	public void createRelationship(int startNodeID, int endNodeID, TransytRelationshipType type) {
		
		createRelationship(startNodeID, endNodeID, type, null);
	}
	
	/** Method to create a relationship between two nodes and set one property.
	 * 
	 * @param label
	 * @param properties
	 * @return
	 */
	public void createRelationship(int startNodeID, int endNodeID, TransytRelationshipType type,
			TransytGeneralProperties key, String value) {
		
		Map<String, String> properties = new HashMap<>();
		
		properties.put(key.toString(), value);
		
		createRelationship(startNodeID, endNodeID, type, properties);
	}
	
	/** Method to create a relationship between two nodes.
	 * 
	 * @param label
	 * @param properties
	 * @return
	 */
	public void createRelationship(int startNodeID, int endNodeID, TransytRelationshipType type, Map<String, String> properties) {
		
		Session session = driver.session();
		
		String auxString = buildPropertiesQuery(properties);

		session.run("MATCH (a), (b) WHERE id(a)=" + startNodeID + " AND id(b)=" + endNodeID
				+ " CREATE (a)-[r:" + type + " " + auxString +"]->(b)");
		
		session.close();
	}
	
	/**
	 * Auxuliar method to build the properties query when adding a node or relationship.
	 * A String in format {key1: 'value1', key2: 'value2'} is returned
	 * @return
	 */
	private String buildPropertiesQuery(Map<String, String> properties) {
		
		String auxString = "";
		
		if(properties != null && !properties.isEmpty()) {
			
			auxString = " {";
			
			Iterator<String> iterator = properties.keySet().iterator();
			
			while(iterator.hasNext()) {
				
				String key = iterator.next();
				
				String auxKey = key;
				
				if(key.contains(" "))
					auxKey = "`" + key + "`";
				
				auxString = auxString + auxKey + ": '" + properties.get(key).replace("'", "\\'") + "'";
						
				if(iterator.hasNext()) {
					auxString = auxString + ", ";
				}
				else
					auxString = auxString + "} ";
			}			
		}
		
		return auxString;
	}
	
	
	/**
	 * Set a property to a node. If a value already exists, it will be replaced.
	 * 
	 * @param label
	 * @param key
	 * @param value
	 */
	public void setNodeProperty(int id, TransytNodeLabel label, TransytGeneralProperties key, String value) {
		
		Session session = driver.session();
		
		session.writeTransaction(tx -> tx.run("MATCH (n:" + label + ") "
				+ "WHERE id(n)="+ id +" SET n." + key + "='" + value.replace("'", "\\'") + "'"));
		
		session.close();
		
	}
	
	/**
	 * Set a property to a relationship. If a value already exists, it will be replaced.
	 * 
	 * @param label
	 * @param key
	 * @param value
	 */
	public void setRelationshipProperty(int startNodeID, int endNodeID, TransytRelationshipType type,
			TransytGeneralProperties key, String value) {
		
		Session session = driver.session();
		
		session.writeTransaction(tx -> tx.run("MATCH (a)-[r: "+ type +" ]->(b) "
				+ "WHERE id(a)="+ startNodeID +" AND id(a)="+ endNodeID +" SET r." + key + "='" + value.replace("'", "\\'") + "'"));
		
		session.close();
		
	}
	
	/**
	 * Get all nodes containing a specific label in the database.
	 * 
	 * @return
	 */
	public Map<String, TransytNode> getNodesByLabel(TransytNodeLabel label, TransytGeneralProperties key) {
		
		Map<String, TransytNode> map = new HashMap<>();
		
		Session session = driver.session();
		
		Result res = session.run("MATCH (n:" + label + ") "
				+ "RETURN {id:id(n), props:properties(n)}");
		
		while(res.hasNext()) {

			Record record = res.next();

			int id = -1;

			String idString = record.get(0).asMap().get("id").toString();

			if(!idString.equalsIgnoreCase("NULL")) {

				id = Integer.valueOf(idString);
			}

			Map<String, String> properties = mapProperties(record.get(0).asMap().get("props").toString());

			String value = properties.get(key.toString());
			
			map.put(value, new TransytNode(id, properties));
		}
		session.close();
		
		return map; 
	}
	
	/**
	 * @param startNodeID
	 * @param endNodeID
	 * @param type
	 * @return
	 */
	public TransytRelationship getRelationship(int startNodeID, int endNodeID, TransytRelationshipType type) {
		
		Session session = driver.session();
		
		Result res = session.run("MATCH (a)-[r:"+ type +" ]->(b) "
				+ "WHERE id(a)="+ startNodeID +" AND id(b)="+ endNodeID +" RETURN {relid:id(r), props:properties(r)}");
		
		while(res.hasNext()) {

			Record record = res.next();
			
			int id = -1;

			String idString = record.get(0).asMap().get("relid").toString();

			if(!idString.equalsIgnoreCase("NULL")) {

				id = Integer.valueOf(idString);
			}

			Map<String, String> properties = mapProperties(record.get(0).asMap().get("props").toString());

			session.close();
			return new TransytRelationship(id, properties, type, null);
		}

		session.close();
		return null;
	}

	/**
	 * Find node with the corresponding UniProt Accession number
	 * 
	 * @param accession
	 * @return
	 */
	public TransytNode findUniprotAccessionNode(String accession) {

		return this.findNode(TransytNodeLabel.Uniprot_Accession, TransytGeneralProperties.Accession_Number, accession);

	}

	/**
	 * Find node with the corresponding TC Number
	 * 
	 * @param tcNumber
	 * @return
	 */
	public TransytNode findTcNumberNode(String tcNumber) {
		
		return this.findNode(TransytNodeLabel.TC_Number, TransytGeneralProperties.TC_Number, tcNumber);
	}
	
	/**
	 * Find node with the corresponding reactionID
	 * 
	 * @param reactionID
	 * @return
	 */
	public TransytNode findReactionNode(String reactionID) {

		return this.findNode(TransytNodeLabel.Reaction, TransytGeneralProperties.ReactionID, reactionID);
	}
	
	/**
	 * Find node with the corresponding metabolite ID.
	 * 
	 * @param label
	 * @param metaboliteID
	 * @return
	 */
	public TransytNode findMetaboliteNode(TransytNodeLabel label, String id) {

		return this.findNode(label, TransytGeneralProperties.MetaboliteID, id);
	}
	
	/**
	 * Find node with the corresponding molecular formula.
	 * 
	 * @param formula
	 * @return
	 */
	public TransytNode findMolecularFormulaNode(String formula) {

		return this.findNode(TransytNodeLabel.Molecular_formula, TransytGeneralProperties.Molecular_Formula, formula);
	}

	/**
	 * Find node with the corresponding TC Number
	 * 
	 * @param type of transport
	 * @return
	 */
	public TransytNode findTransportTypeNode(TypeOfTransporter type) {

		return this.findNode(TransytNodeLabel.Transport_Type, TransytGeneralProperties.Transport_Type, type.toString());
	}
	
	/**
	 * Find node with the corresponding metabolite name.
	 * 
	 * @param name
	 * @return
	 */
	public TransytNode findNameNode(String name) {

		return this.findNode(TransytNodeLabel.Metabolite_Name, TransytGeneralProperties.Name, name);
	}

	/**
	 * Get all nodes with the given label
	 * 
	 * @return
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonParseException 
	 */
	public TransytNode findNode(TransytNodeLabel label, TransytGeneralProperties key, String value)
	{
		Session session = driver.session();
		
		Map<String, Object> props = new HashMap<>();
		props.put(key.toString(), value);
		
		Result res = session.run("MATCH (n:" + label + ") "
				+ "WHERE n."+ key + "=$" + key + " "
				+ "RETURN {id:id(n), props:properties(n)}", props);
		//				+ "RETURN n, properties(n)" );

		while(res.hasNext()) {

			Record record = res.next();

			int id = -1;

			String idString = record.get(0).asMap().get("id").toString();

			if(!idString.equalsIgnoreCase("NULL")) {

				id = Integer.valueOf(idString);
			}

			Map<String, String> properties = mapProperties(record.get(0).asMap().get("props").toString());
			
			session.close();

			return new TransytNode(id, properties);
		}
		
		session.close();

		return null;

	}

	/**
	 * Find all subunits present in the database for each tcNumber.
	 * 
	 * @param id
	 * @param type
	 * @return
	 */
	public Map<String, Set<String>> findAllSubunitsInDatabase() {

		Map<String, Set<String>> map = new HashMap<>();
		
		Session session = driver.session();
		
		Result res = session.run(
				"MATCH (a:" + TransytNodeLabel.Uniprot_Accession + ")-[t:" + TransytRelationshipType.has_tc + "]->(b:" + TransytNodeLabel.TC_Number + ")"
				+ "RETURN {acc:a. " + TransytGeneralProperties.Accession_Number + ", tc:b." + TransytGeneralProperties.TC_Number + "}");
		
		while(res.hasNext()) {

			Record record = res.next();
			
			String acc = record.get(0).asMap().get("acc").toString();
			String tc = record.get(0).asMap().get("tc").toString();

			
			Set<String> accessions = new HashSet<>();
			
			if(map.containsKey(tc))
				accessions = new HashSet<>(map.get(tc));

			accessions.add(acc);
			map.put(tc, accessions);
		}
		
		session.close();

		return map;
	}
	
	/**
	 * Find the tcNumbers associated to each reaction in the database.
	 * 
	 * @param id
	 * @param type
	 * @return
	 */
	public Map<String, Set<String>> findAllTcNumbersByReaction() {

		Map<String, Set<String>> map = new HashMap<>();
		
		Session session = driver.session();
		
		Result res = session.run(
				"MATCH (a:" + TransytNodeLabel.TC_Number + ")-[t:" + TransytRelationshipType.has_reaction + "]->(b:" + TransytNodeLabel.Reaction + ")"
				+ "RETURN {tc:a. " + TransytGeneralProperties.TC_Number + ", react:b." + TransytGeneralProperties.ReactionID + "}");
		
		while(res.hasNext()) {

			Record record = res.next();
			
			String tc = record.get(0).asMap().get("tc").toString();
			String reactionID = record.get(0).asMap().get("react").toString();

			
			Set<String> accessions = new HashSet<>();
			
			if(map.containsKey(reactionID))
				accessions = new HashSet<>(map.get(reactionID));

			accessions.add(tc);
			map.put(reactionID, accessions);
		}
		
		session.close();

		return map;
	}
	
	/**
	 * Retrieves all relationships of tcNumber has reaction.
	 * 
	 * @return
	 */
	public Map<String, Set<TransytRelationship>> getAllTCNumberHasReactionRelationships() {

		Map<String, Set<TransytRelationship>> map = new HashMap<>();

		Session session = driver.session();
		
		Result res = session.run(
				"MATCH (a:" + TransytNodeLabel.TC_Number + ")-[t:" + TransytRelationshipType.has_reaction + "]->(b:" + TransytNodeLabel.Reaction + ")"
				+ "RETURN {tc:a." + TransytGeneralProperties.TC_Number + ", relid:id(t), relProps:properties(t), endid:id(b), endNodeProps:properties(b)}");

		while(res.hasNext()) {
			
			Record record = res.next();
			
			String tc = record.get(0).asMap().get("tc").toString();
			
			int idRelationship = -1;
			int idEndNode = -1;

			String idRelationshipString = record.get(0).asMap().get("relid").toString();
			String idEndNodeString = record.get(0).asMap().get("endid").toString();

			if(!idRelationshipString.equalsIgnoreCase("NULL")) {

				idRelationship = Integer.valueOf(idRelationshipString);
			}

			if(!idEndNodeString.equalsIgnoreCase("NULL")) {

				idEndNode = Integer.valueOf(idEndNodeString);
			}

			Map<String, String> relProperties = mapProperties(record.get(0).asMap().get("relProps").toString());
			Map<String, String> nodeProperties = mapProperties(record.get(0).asMap().get("endNodeProps").toString());
			
			Set<TransytRelationship> rels = new HashSet<>();
			
			if(map.containsKey(tc))
				rels = new HashSet<>(map.get(tc));

			rels.add(new TransytRelationship(idRelationship, relProperties, null, new TransytNode(idEndNode, nodeProperties)));

			map.put(tc, rels);
		}
		session.close();
		
		return map;
	}
	
	/**
	 * Retrieve all relationships of a specific type between reaction an metabolite.
	 * 
	 * @return
	 */
	public Map<String, Set<TransytRelationship>> getAllReactionHasMetaboliteRelationships(TransytRelationshipType type) {

		Map<String, Set<TransytRelationship>> map = new HashMap<>();

		Session session = driver.session();
		
		Result res = session.run(
				"MATCH (a:" + TransytNodeLabel.Reaction + ")-[t:" + type + "]->(b) "
				+ "RETURN {reactid:a." + TransytGeneralProperties.ReactionID + ", relid:id(t), relProps:properties(t), endid:id(b), endNodeProps:properties(b)}");

		while(res.hasNext()) {
			
			Record record = res.next();
			
			String reactionID = record.get(0).asMap().get("reactid").toString();
			
			int idRelationship = -1;
			int idEndNode = -1;

			String idRelationshipString = record.get(0).asMap().get("relid").toString();
			String idEndNodeString = record.get(0).asMap().get("endid").toString();

			if(!idRelationshipString.equalsIgnoreCase("NULL")) {

				idRelationship = Integer.valueOf(idRelationshipString);
			}

			if(!idEndNodeString.equalsIgnoreCase("NULL")) {

				idEndNode = Integer.valueOf(idEndNodeString);
			}

			Map<String, String> relProperties = mapProperties(record.get(0).asMap().get("relProps").toString());
			Map<String, String> nodeProperties = mapProperties(record.get(0).asMap().get("endNodeProps").toString());
			
			Set<TransytRelationship> rels = new HashSet<>();
			
			if(map.containsKey(reactionID))
				rels = new HashSet<>(map.get(reactionID));

			rels.add(new TransytRelationship(idRelationship, relProperties, null, new TransytNode(idEndNode, nodeProperties)));

			map.put(reactionID, rels);
		}
		session.close();
		
		return map;
	}
	
	/**
	 * Retrieve the transport type of all reactions.
	 * 
	 * @return
	 */
	public Map<String, TypeOfTransporter> getAllReactionHasTransportType() {

		Map<String, TypeOfTransporter> map = new HashMap<>();

		Session session = driver.session();
		
		Result res = session.run(
				"MATCH (a:" + TransytNodeLabel.Reaction + ")-[t:" + TransytRelationshipType.has_transport_type + "]->(b) "
				+ "RETURN {reactid:a." + TransytGeneralProperties.ReactionID + ", type:b." + TransytGeneralProperties.Transport_Type + "}");

		while(res.hasNext()) {
			
			Record record = res.next();
			
			String reactionID = record.get(0).asMap().get("reactid").toString();
			String transportType = record.get(0).asMap().get("type").toString();
			
			map.put(reactionID, TypeOfTransporter.valueOf(transportType));
		}
		session.close();
		
		return map;
	}
	
	/**
	 * Find the moleculas formulas of each metabolite.
	 * 
	 * @return
	 */
	public void searchFormulas(TransytNodeLabel label) {
		
		formulasBackUp = new HashMap<>();

		Session session = driver.session();
		
		Result res = session.run(
				"MATCH (a:" + label + ")-[t:" + TransytRelationshipType.has_molecular_formula + "]->(b) "
				+ "RETURN {metabID:a." + TransytGeneralProperties.MetaboliteID + ", formula:b." + 					
				TransytGeneralProperties.Molecular_Formula +"}");

		while(res.hasNext()) {
			
			Record record = res.next();
			
			String metaboliteID = record.get(0).asMap().get("metabID").toString();
			String formula = record.get(0).asMap().get("formula").toString();
			
			formulasBackUp.put(metaboliteID, formula);
		}
		session.close();
		
	}
	
	/**
	 * Retrieve node's all downstream relationships of a specific type.
	 * 
	 * @param id
	 * @param type
	 * @return
	 */
	public Set<TransytRelationship> getDownstreamRelationships(int id, TransytRelationshipType type) {	//VERIFICAR SE EST� A CHAM;AR BEM

		Set<TransytRelationship> rels = new HashSet<>();

		Session session = driver.session();
		
		Map<String, Object> props = new HashMap<>();
		props.put("id", id);

		Result res = session.run("MATCH (a)-[t:" + type + "]->(b) WHERE id(a)=$id "
				+ "RETURN {relid:id(t), relProps:properties(t), endid:id(b), endNodeProps:properties(b)}", props);

		while(res.hasNext()) {

			Record record = res.next();

			int idRelationship = -1;
			int idEndNode = -1;

			String idRelationshipString = record.get(0).asMap().get("relid").toString();
			String idEndNodeString = record.get(0).asMap().get("endid").toString();

			if(!idRelationshipString.equalsIgnoreCase("NULL")) {

				idRelationship = Integer.valueOf(idRelationshipString);
			}

			if(!idEndNodeString.equalsIgnoreCase("NULL")) {

				idEndNode = Integer.valueOf(idEndNodeString);
			}

			Map<String, String> relProperties = mapProperties(record.get(0).asMap().get("relProps").toString());
			Map<String, String> nodeProperties = mapProperties(record.get(0).asMap().get("endNodeProps").toString());

			rels.add(new TransytRelationship(idRelationship, relProperties, type, new TransytNode(idEndNode, nodeProperties)));

		}

		session.close();
		
		return rels;
	}
	
	/**
	 * Check if a node contains a specific relationship type.
	 * 
	 * @param id
	 * @param type
	 * @return
	 */
	public boolean nodeHasRelationshipType(int id, TransytRelationshipType type) {

		Session session = driver.session();
		
		Map<String, Object> props = new HashMap<>();
		props.put("id", id);

		Result res = session.run("MATCH (a)-[t:" + type + "]->(b) WHERE id(a)=$id "
				+ "RETURN {relid:id(t), endid:id(b)}", props);
		
		while(res.hasNext()) {
			
			session.close();
			return true;
		}

		session.close();
		return false;
	}
	
	/**
	 * Check if a specific relationship exists between two nodes.
	 * 
	 * @param id
	 * @param type
	 * @return
	 */
	public boolean existsRelationship(int id, int otherId, TransytRelationshipType type) {

		Session session = driver.session();
		
		Map<String, Object> props = new HashMap<>();
		props.put("id1", id);
		props.put("id2", otherId);

		Result res = session.run("MATCH (a)-[t:" + type + "]-(b) "
				+ "WHERE id(a)=$id1 AND id(b)=$id2 "
				+ "RETURN {relid:id(t)}", props);
		
		while(res.hasNext()) {
			
			session.close();
			return true;
		}

		session.close();
		return false;
	}
	
	/**
	 * Read nodes and relationships of metabolites to construct the reaction's equation.
	 * 
	 * @param tcNumber
	 * @return
	 */
	public String getReactionEquation(String reactionID, boolean reversible, Set<TransytRelationship> reactants, Set<TransytRelationship> products) {

		temporaryCompounds = new HashSet<>();
		temporaryCompoundsNames = new HashMap<>();
		temporaryCompoundsFormulas = new HashMap<>();

		String reaction = "";

		String reversibility = ReactionContainer.IRREVERSIBLE_TOKEN;

		if(reversible)
			reversibility = ReactionContainer.REVERSIBLE_TOKEN;

//		Set<TransytRelationship> reactants = getDownstreamRelationships(reactionNodeID, reactantRel);
//		Set<TransytRelationship> products = getDownstreamRelationships(reactionNodeID, productRel);

		try {
			String reactant = buildEquationAux(reactants);
			String product = buildEquationAux(products);

			if(reactant == null || product == null)
				return null;
			
			reaction = reactant.concat(reversibility).concat(product);
		} 
		catch (Exception e) {
			
			logger.error("An error occurred while building the equation for reaction {}", reactionID);
			logger.trace("StackTrace: {}", e);
			
			return null;
		}

		return reaction;
	}
	
	/**
	 * Method to build Transyt's reaction equation.
	 * 
	 * @param relationships
	 * @return
	 */
	private String buildEquationAux(Set<TransytRelationship> relationships) {

		String reaction = "";

		String metaboliteID = "";

		Iterator<TransytRelationship> iterator = relationships.iterator();

		while(iterator.hasNext()){

			TransytRelationship rel = iterator.next();
			
			TransytNode node = rel.getOtherEndNode();
			
//			System.out.println(rel.getEndNode().getAllProperties());
			metaboliteID = node.getProperty(TransytGeneralProperties.MetaboliteID);
			
			metaboliteID = metaboliteID.replace(" (middle)", "").trim(); //delete when fixed
			
			if(metaboliteID == null)
				return null;

			temporaryCompounds.add(metaboliteID);
			
			String name = node.getProperty(TransytGeneralProperties.Name);
			
//			System.out.println(metabolite + "\t" + names);
			
			if(name != null) 
				temporaryCompoundsNames.put(metaboliteID, name);
			else
				temporaryCompoundsNames.put(metaboliteID, metaboliteID);
			
			temporaryCompoundsFormulas.put(metaboliteID, this.getMolecularFormula(metaboliteID));

			String stoichiometry = rel.getProperty(TransytGeneralProperties.Stoichiometry);

			if(stoichiometry.equals("1"))
				stoichiometry = "";
			else
				stoichiometry = stoichiometry.concat(" ");

			String direction = " ";

			if(rel.hasProperty(TransytGeneralProperties.Direction)) {

				direction = rel.getProperty(TransytGeneralProperties.Direction);
				direction = " (".concat(direction).concat(") ");
			}

			reaction = reaction.concat(stoichiometry).concat(metaboliteID).concat(direction);

			if(iterator.hasNext())
				reaction = reaction.concat("+ ");
		}
		return reaction;
	}
	
	/**
	 * Retrieve molecular formula from 
	 * 
	 * @param node
	 * @return
	 */
	public String getMolecularFormula(String metaboliteID) {
		
		try {
			return formulasBackUp.get(metaboliteID);
		} catch (Exception e) {
			logger.error("No molecular formula found for metabolite with identifier: {}", metaboliteID);
		}
		
		return null;
		
//		Set<TransytRelationship> rels = getDownstreamRelationships(id, TransytRelationshipType.has_molecular_formula);
//		
//		for(TransytRelationship rel : rels) {
//			
//			TransytNode node = rel.getOtherEndNode();
//
//			return node.getProperty(TransytGeneralProperties.Molecular_Formula);
//			
//		}
//		return null;
	}

	@Override
	public void close() throws Exception
	{
		driver.close();
	}

	/**
	 * Convert neo4j properties mapping to hashmap.
	 * 
	 * @param text
	 * @return
	 */
	private Map<String, String> mapProperties(String text){ 

		Map<String, String> properties = new HashMap<>();

		try {
			String[] res = text.substring(1, text.length()-1).split(", ");  //ignore '{' and '}'

			String previousKey = "";
			
			for(String property : res) {
				try {
					if(!property.isEmpty() && property.contains("=")) {
						String[] aux = property.split("=");
						
						if(aux.length > 1) {
						
							properties.put(aux[0], aux[1]);
							previousKey = aux[0];
						}
						else
							previousKey = "";
						
					}
					else if(!property.isEmpty()){
						
						properties.put(previousKey, properties.get(previousKey).concat(", ").concat(property));
						
					}
				} 
				catch (Exception e) {
					logger.warn("An error occurred while mapping the node properties for the following sentence: {}", text);
				}
			}
		} 
		catch (Exception e) {
			
			properties = new HashMap<>();
			logger.trace("StackTrace: {}", e);
		}

		return properties;
	}
	
	/**
	 * Method to return the compounds temporarily saved in this variable, previously processed by the method getReactionEquation().
	 * If not used after calling the previous method, this method should not be used.
	 * 
	 * @return
	 */
	public Set<String> getTemporaryReactionCompounds() { 

		return temporaryCompounds;

	}

	/**
	 * Method to return the names of the compounds temporarily saved in this variable, previously processed by the method getReactionEquation().
	 * If not used after calling the previous method, this method should not be used.
	 * 
	 * @return
	 */
	public Map<String, String> getTemporaryCompoundsNames() {
		return temporaryCompoundsNames;
	}
	
	/**
	 * Method to return the names of the compounds temporarily saved in this variable, previously processed by the method getReactionEquation().
	 * If not used after calling the previous method, this method should not be used.
	 * 
	 * @return
	 */
	public Map<String, String> getTemporaryCompoundsFormulas() {
		return temporaryCompoundsFormulas;
	}
}
