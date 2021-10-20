package pt.uminho.ceb.biosystems.transyt.service.transytDatabase;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.QueryExecutionException;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.event.KernelEventHandler;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.graphdb.traversal.BidirectionalTraversalDescription;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.helpers.collection.Iterators;

import pt.uminho.ceb.biosystems.transyt.utilities.capsules.ReactionContainer;
import pt.uminho.ceb.biosystems.transyt.utilities.enumerators.Enumerators.TypeOfTransporter;

public class TransytGraphDatabaseService implements GraphDatabaseService{

	private final GraphDatabaseService service;
	private Set<String> temporaryCompounds;
	private Map<String, String> temporaryCompoundsNames;
	private Map<String, String> temporaryCompoundsFormulas;

	public TransytGraphDatabaseService(GraphDatabaseService service) {

		this.service = service;

		temporaryCompounds = new HashSet<>();
		temporaryCompoundsNames = new HashMap<>();
		temporaryCompoundsFormulas = new HashMap<>();
	}

	/**
	 * Get all nodes with the given label
	 * 
	 * @return
	 */
	public Set<Node> getAllNodesByLabel(TransytNodeLabel label){

		Set<Node> result = new HashSet<> ();

		for (Node node : Iterators.asSet(service.findNodes(label))) {
			result.add(node);
		}
		return result;
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

	@Override
	public Node createNode() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node createNode(Label... labels) {
		Node node = service.createNode();
		for (Label l : labels) {
			node.addLabel(l);
		}

		return node;
	}

	/**
	 * Find node with the corresponding UniProt Accession number
	 * 
	 * @param accession
	 * @return
	 */
	public Node findUniprotAccessionNode(String accession) {

		return this.findNode(TransytNodeLabel.Uniprot_Accession, TransytGeneralProperties.Accession_Number.toString(), accession);

	}

	/**
	 * Find node with the corresponding TC Number
	 * 
	 * @param tcNumber
	 * @return
	 */
	public Node findTcNumberNode(String tcNumber) {

		return this.findNode(TransytNodeLabel.TC_Number, TransytGeneralProperties.TC_Number.toString(), tcNumber);
	}

	/**
	 * Find node with the corresponding TC Number
	 * 
	 * @param tcNumber
	 * @return
	 */
	public Node findTransportTypeNode(TypeOfTransporter type) {

		return this.findNode(TransytNodeLabel.Transport_Type, TransytGeneralProperties.Transport_Type.toString(), type.toString());
	}

	/**
	 * Find node with the corresponding metabolite ID.
	 * 
	 * @param tcNumber
	 * @return
	 */
	public Node findMetaboliteNode(TransytNodeLabel label, String id) {

		return this.findNode(label, TransytGeneralProperties.MetaboliteID.toString(), id);
	}
	
	/**
	 * Find node with the corresponding metabolite name.
	 * 
	 * @param tcNumber
	 * @return
	 */
	public Node findNameNode(String name) {

		return this.findNode(TransytNodeLabel.Metabolite_Name, TransytGeneralProperties.Name.toString(), name);
	}

	/**
	 * Find node with the corresponding molecular formula.
	 * 
	 * @param tcNumber
	 * @return
	 */
	public Node findMolecularFormulaNode(String formula) {

		return this.findNode(TransytNodeLabel.Molecular_formula, TransytGeneralProperties.Molecular_Formula.toString(), formula);
	}
	
	/**
	 * Read nodes and relationships of metabolites to construct the reaction's equation.
	 * 
	 * @param tcNumber
	 * @return
	 */
	public String getReactionEquation(Node reactionNode, TransytRelationshipType reactantRel, TransytRelationshipType productRel, boolean reversible) {

		temporaryCompounds = new HashSet<>();
		temporaryCompoundsNames = new HashMap<>();
		temporaryCompoundsFormulas = new HashMap<>();

		String reaction = "";

		String reversibility = ReactionContainer.IRREVERSIBLE_TOKEN;

		if(reversible)
			reversibility = ReactionContainer.REVERSIBLE_TOKEN;

		Iterable<Relationship> reactants = reactionNode.getRelationships(reactantRel);
		Iterable<Relationship> products = reactionNode.getRelationships(productRel);

		String reactant = buildEquationAux(reactants);
		String product = buildEquationAux(products);

		reaction = reactant.concat(reversibility).concat(product);

		return reaction;
	}

	private String buildEquationAux(Iterable<Relationship> relationships) {

		String reaction = "";

		String metabolite = "";

		Iterator<Relationship> iterator = relationships.iterator();

		while(iterator.hasNext()){

			Relationship rel = iterator.next();
			
			Node node = rel.getEndNode();
			
//			System.out.println(rel.getEndNode().getAllProperties());
			metabolite = node.getProperty(TransytGeneralProperties.MetaboliteID.toString()).toString();

			temporaryCompounds.add(metabolite);
			
			String name = node.getProperty(TransytGeneralProperties.Name.toString()).toString();
			
//			System.out.println(metabolite + "\t" + names);
			
			if(name != null) 
				temporaryCompoundsNames.put(metabolite, name);
			else
				temporaryCompoundsNames.put(metabolite, metabolite);
			
			temporaryCompoundsFormulas.put(metabolite, getMolecularFormula(node));

			String stoichiometry = rel.getProperty(TransytGeneralProperties.Stoichiometry.toString()).toString();

			if(stoichiometry.equals("1"))
				stoichiometry = "";
			else
				stoichiometry = stoichiometry.concat(" ");

			String direction = " ";

			if(rel.hasProperty(TransytGeneralProperties.Direction.toString())) {

				direction = rel.getProperty(TransytGeneralProperties.Direction.toString()).toString();
				direction = " (".concat(direction).concat(") ");
			}

			reaction = reaction.concat(stoichiometry).concat(metabolite).concat(direction);

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
	public String getMolecularFormula(Node node) {
		
		Iterator<Relationship> iterator = node.getRelationships(TransytRelationshipType.has_molecular_formula).iterator();

		while(iterator.hasNext()){

			Relationship rel = iterator.next();
			
			return rel.getEndNode().getProperty(TransytGeneralProperties.Molecular_Formula.toString()).toString();
			
		}
		return null;
	}

	/**
	 * find is a specific relationship between two nodes exists
	 * 
	 * @param nodeA
	 * @param nodeB
	 * @param rel
	 * @return
	 */
	public boolean existsRelationship(Node nodeA, Node nodeB, TransytRelationshipType typeOfRel) {

		Iterable<Relationship> rels = nodeA.getRelationships();

		for(Relationship rel : rels) {

			if(rel.getType().toString().equals(typeOfRel.toString())) {

				if(rel.getOtherNode(nodeA).equals(nodeB))
					return true;
			}
		}
		return false;
	}

	/**
	 * Retrieves a specific relationship between two nodes exists.
	 * Only one relationship can exist, otherwise null is returned.
	 * 
	 * @param nodeA
	 * @param nodeB
	 * @param rel
	 * @return
	 */
	public Relationship getRelationship(Node nodeA, Node nodeB, TransytRelationshipType typeOfRel) {

		Iterable<Relationship> rels = nodeA.getRelationships();

		Relationship relToReturn = null;
		int count = 0;

		for(Relationship rel : rels) {

//			System.out.println(rel.getType());

			if(rel.getType().toString().equals(typeOfRel.toString())) {

				if(rel.getOtherNode(nodeA).equals(nodeB)) {

					relToReturn = rel;
					count++;
				}
			}
		}

		if(count > 1) {
//			System.out.println("Mais do que uma!!!!");
			return null;
		}
//		else if(count == 0) {
//			System.out.println("No relations!");
//
//		}

		return relToReturn;
	}

	/**
	 * Finds the node in the other end of a relationship with the described features.
	 * 
	 * @param nodeA
	 * @param nodeB
	 * @param rel
	 * @return null if the node does not exist
	 */
	public Node getEndNodeByEntryIDAndLabel(Node node, String entryID, TransytGeneralProperties propertyLabel, TransytRelationshipType typeOfRel) {

		Iterable<Relationship> accessionRelationships = node.getRelationships(typeOfRel);

		for(Relationship rel : accessionRelationships) {

			Node endNode = rel.getEndNode();

			if(endNode.hasProperty(propertyLabel.toString())) {

				if(endNode.getProperty(propertyLabel.toString()).equals(entryID)) {

					return endNode;
				}
			}
		}

		return null;
	}

	/**
	 * Get all related nodes.
	 * 
	 * @param nodeA
	 * @param nodeB
	 * @param rel
	 * @return null if the node does not exist
	 */
	public Set<Node> getAllRelatedNodes(Node node, TransytRelationshipType typeOfRel) {

		Set<Node> result = new HashSet<>();

		Iterable<Relationship> accessionRelationships = node.getRelationships(typeOfRel);

		for(Relationship rel : accessionRelationships) 

			result.add(rel.getEndNode());

		return result;
	}

	/**
	 * Method to retrieve all subunits known in TCDB for the given TCNumbers.
	 * 
	 * @param tcNumbers
	 * @return
	 */
	public Map<String, Set<String>> findSubunitsInDatabase(Set<String> tcNumbers) {

		Map<String, Set<String>> subunits = new HashMap<>();

		for(String tcNumber : tcNumbers) {

			Node tcNode = findTcNumberNode(tcNumber);

			if(tcNode != null) {

				Iterable<Relationship> relations = tcNode.getRelationships(TransytRelationshipType.has_tc);

				for(Relationship rel : relations) {

					Set<String> accessions = new HashSet<>();

					String acc = rel.getStartNode().getProperty(TransytGeneralProperties.Accession_Number.toString()).toString();

					if(subunits.containsKey(tcNumber))
						accessions = subunits.get(tcNumber);

					accessions.add(acc);
					subunits.put(tcNumber, accessions);

				}
			}
		}

		return subunits;
	}

	/**
	 * Find node with the corresponding reactionID
	 * 
	 * @param reactionID
	 * @return
	 */
	public Node findReactionNode(String reactionID) {

		return this.findNode(TransytNodeLabel.Reaction, TransytGeneralProperties.ReactionID.toString(), reactionID);
	}

	@Override
	public Long createNodeId() {
		Node node = service.createNode();
		return node.getId();
	}

	@Override
	public Result execute(String query) throws QueryExecutionException {
		return service.execute(query);
	}

	@Override
	public Result execute(String query, long timeout, TimeUnit unit) throws QueryExecutionException {
		return service.execute(query, timeout, unit);
	}

	@Override
	public Result execute(String query, Map<String, Object> parameters) throws QueryExecutionException {
		return service.execute(query, parameters);
	}

	@Override
	public Result execute(String query, Map<String, Object> parameters, long timeout, TimeUnit unit)
			throws QueryExecutionException {
		return service.execute(query, parameters, timeout, unit);
	}

	@Override
	public ResourceIterable<String> getAllPropertyKeys() {
		return service.getAllPropertyKeys();
	}

	public Set<Node> listNodes(Label label, String key, Object value) {
		return Iterators.asSet(service.findNodes(label, key, value));
	}

	@Override
	public ResourceIterable<Label> getAllLabelsInUse() {
		return service.getAllLabelsInUse();
	}

	@Override
	public ResourceIterable<RelationshipType> getAllRelationshipTypesInUse() {
		return service.getAllRelationshipTypesInUse();
	}

	@Override
	public ResourceIterable<Label> getAllLabels() {
		return service.getAllLabels();
	}


	@Override
	public Transaction beginTx(long arg0, TimeUnit arg1) {
		return service.beginTx(arg0, arg1);
	}

	@Override
	public Node getNodeById(long id) {
		return service.getNodeById(id);
	}

	@Override
	public Relationship getRelationshipById(long id) {
		return service.getRelationshipById(id);
	}

	@Override
	public boolean isAvailable(long timeout) {
		return service.isAvailable(timeout);
	}

	@Override
	public void shutdown() {
		service.shutdown();
	}

	@Override
	public Transaction beginTx() {
		return service.beginTx();
	}

	@Override
	public <T> TransactionEventHandler<T> registerTransactionEventHandler(TransactionEventHandler<T> handler) {
		return service.registerTransactionEventHandler(handler);
	}

	@Override
	public <T> TransactionEventHandler<T> unregisterTransactionEventHandler(TransactionEventHandler<T> handler) {
		return service.unregisterTransactionEventHandler(handler);
	}

	@Override
	public KernelEventHandler registerKernelEventHandler(KernelEventHandler handler) {
		return service.registerKernelEventHandler(handler);
	}

	@Override
	public KernelEventHandler unregisterKernelEventHandler(KernelEventHandler handler) {
		return service.unregisterKernelEventHandler(handler);
	}

	@Override
	public Schema schema() {
		return service.schema();
	}

	@Override
	public IndexManager index() {
		return service.index();
	}

	@Override
	public TraversalDescription traversalDescription() {
		return service.traversalDescription();
	}

	@Override
	public BidirectionalTraversalDescription bidirectionalTraversalDescription() {
		return service.bidirectionalTraversalDescription();
	}

	@Override
	public ResourceIterable<Node> getAllNodes() {
		return service.getAllNodes();
	}

	@Override
	public ResourceIterable<Relationship> getAllRelationships() {
		return service.getAllRelationships();
	}

	@Override
	public ResourceIterator<Node> findNodes(Label label, String key, Object value) {
		return service.findNodes(label, key, value);
	}

	@Override
	public Node findNode(Label label, String key, Object value) {
		return service.findNode(label, key, value);
	}

	@Override
	public ResourceIterator<Node> findNodes(Label label) {
		return service.findNodes(label);
	}

	@Override
	public ResourceIterable<RelationshipType> getAllRelationshipTypes() {
		return service.getAllRelationshipTypes();
	}

}
