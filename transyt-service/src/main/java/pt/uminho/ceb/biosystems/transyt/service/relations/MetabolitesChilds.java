package pt.uminho.ceb.biosystems.transyt.service.relations;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.uminho.ceb.biosystems.transyt.service.reactions.TransportReactionsBuilder;
import pt.uminho.sysbio.biosynth.integration.io.dao.neo4j.MetaboliteMajorLabel;
import pt.uminho.sysbio.biosynth.integration.io.dao.neo4j.MetaboliteRelationshipType;
import pt.uminho.sysbio.biosynthframework.BiodbGraphDatabaseService;

public class MetabolitesChilds {

	public static final Integer ALL_CODE = -1;
	private static final Logger logger = LoggerFactory.getLogger(MetabolitesChilds.class);

	/**
	 * Get all childs of a given metabolite.
	 * Generation limit is the limit of generations until which the algorithm will retrieve childs (limit number included)
	 * 
	 * @param entryID
	 * @param label
	 * @param service
	 * @return
	 */
	public static Set<Long> getMetaboliteChilds(String metabolite, Integer generationLimit, long id, 
			BiodbGraphDatabaseService service, Map<String, Set<String>> missingChilds) {

		Map<Long, Integer> generation = new HashMap<>();

		Set<Long> visited = new HashSet<>();

		LinkedList<Long> toVisit = new LinkedList<>();

		Node node = service.getNodeById(id);

		//		 relation = node.getRelationships(Direction.INCOMING);

		//		for(Relationship rel : relation) {
		//			
		//			Node[] nodes = rel.getNodes();
		//			
		//			if(nodes[1].getId() == id && rel.getType().toString().equals(MetaboliteRelationshipType.instance_of.toString())) { 
		//				toVisit.add(nodes[0].getId());
		//				generation.put(nodes[0].getId(), 1);
		//			}
		//
		//		}

		//		System.out.println(id);

		toVisit.add(id);
		generation.put(id, 0);	//root

		//		System.out.println();

		while(toVisit.size() > 0) {

			//			if(entryID.equals("META:ACYL-COA"))
			//				System.out.println("entrou");

			Long currentNodeID = toVisit.poll();

			node = service.getNodeById(currentNodeID);

			Iterable<Relationship> relation = node.getRelationships(Direction.INCOMING);

			for(Relationship rel : relation) {
				//				if(entryID.equals("META:ACYL-COA"))
				//					System.out.println(rel.getId());

				Node[] nodes = rel.getNodes();

				try {
					if(nodes[1].getId() == currentNodeID && rel.getType().toString().equals(MetaboliteRelationshipType.instance_of.toString())) {

						Integer currentGeneration = generation.get(currentNodeID);

						if(currentGeneration < generationLimit || generationLimit == ALL_CODE) {

							if(!visited.contains(nodes[0].getId()) 
									&& !nodes[0].getProperty("entry").toString().matches("(?i).*modified.*")) {

								if(!nodes[0].hasProperty("name") 
										|| (nodes[0].hasProperty("name") && !nodes[0].getProperty("name").toString().matches("(?i).*modified.*"))) {

									toVisit.add(nodes[0].getId());
									generation.put(nodes[0].getId(), currentGeneration + 1);
								}
							}
						}
					}
				} catch (Exception e) {
					logger.error(e.getMessage());
					//					e.printStackTrace();
				}
			}

			visited.add(currentNodeID);
		}

		if(missingChilds != null && metabolite != null && missingChilds.containsKey(metabolite)) {

			for(String child : missingChilds.get(metabolite)) {

				Long missingId = identifyNode("META:"+child, MetaboliteMajorLabel.MetaCyc, service);

				if(missingId != null)
					visited.add(missingId);
			}
		}

		return visited;
	}

	public static Long identifyNode(String entryID, MetaboliteMajorLabel label, BiodbGraphDatabaseService service) {

		try {
			Node node = service.getNodeByEntryAndLabel(entryID,  label);

			return node.getId();
		} 
		catch (Exception e) {
			logger.error("Missing node " + label.toString() +":" + entryID + " in biosynth database!");
			e.printStackTrace();
		}
		return null;
	}


	//	public static void relations(BiodbGraphDatabaseService service) {
	//
	//
	//		try {
	//			//			Node node = service.getNodeByEntryAndLabel("C00070",  MetaboliteMajorLabel.LigandCompound);
	//
	//			Node node = service.getNodeByEntryAndLabel("META:Isoflavans", MetaboliteMajorLabel.MetaCyc);
	//
	//			System.out.println(node.getProperty("name"));
	//
	//			System.out.println(node.getAllProperties());
	//
	//			System.out.println(node.getId());
	//
	//			Iterable<Relationship> direction = node.getRelationships(Direction.OUTGOING);
	//
	//			System.out.println();
	//
	//			for(Relationship dir : direction) {
	//
	//
	//				Node[] nodes = dir.getNodes();
	//
	//				//			if(nodes[0].getProperty("major_label").equals("LigandCompound")) {
	//
	//				System.out.println(nodes[0]);
	//
	//				System.out.println(nodes[0].getAllProperties());
	//
	//				if(nodes[0].hasProperty("entry"))
	//					System.out.println(nodes[0].getProperty("entry"));
	//
	//				if(nodes[0].hasProperty("name"))
	//					System.out.println(nodes[0].getProperty("name"));
	//
	//				System.out.println(dir.getType().toString());
	//
	//				System.out.println(nodes[1]);
	//
	//				System.out.println(nodes[1].getAllProperties());
	//
	//
	//				if(nodes[1].hasProperty("entry"))
	//					System.out.println(nodes[1].getProperty("entry"));
	//
	//				if(nodes[1].hasProperty("name"))
	//					System.out.println(nodes[1].getProperty("name"));
	//
	//
	//				System.out.println();
	//			}
	//			//		}
	//			//		Node node = service.getNodeByEntryAndLabel("C00001",  MetaboliteMajorLabel.MetaCyc);
	//
	//			System.out.println(node.getId());
	//
	//
	//
	//
	//
	//
	//			//ID - 343339
	//		} 
	//		catch (Exception e) {
	//			// TODO Auto-generated catch block
	//			e.printStackTrace();
	//		}
	//
	//
	//	}

}
