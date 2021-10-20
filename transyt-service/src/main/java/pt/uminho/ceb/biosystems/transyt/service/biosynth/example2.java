package pt.uminho.ceb.biosystems.transyt.service.biosynth;
//package pt.uminho.ceb.biosystems.transyt.service.biosynth;
//
//import java.util.HashSet;
//import java.util.Set;
//
//import org.neo4j.graphdb.GraphDatabaseService;
//import org.neo4j.graphdb.Node;
//import org.neo4j.graphdb.Relationship;
//import org.neo4j.graphdb.Transaction;
//import org.neo4j.graphdb.factory.GraphDatabaseFactory;
//
//import pt.uminho.sysbio.biosynth.integration.io.dao.neo4j.GlobalLabel;
//import pt.uminho.sysbio.biosynth.integration.io.dao.neo4j.MetaboliteMajorLabel;
//import pt.uminho.sysbio.biosynth.integration.io.dao.neo4j.MetaboliteRelationshipType;
//import pt.uminho.sysbio.biosynth.integration.io.dao.neo4j.Neo4jUtils;
//import pt.uminho.sysbio.biosynth.integration.neo4j.BiodbMetaboliteNode;
//import pt.uminho.sysbio.biosynthframework.BiodbGraphDatabaseService;
//
//public class example2 {
//  public static void main(String[] args) {
//    GraphDatabaseService graphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase(databasePath);
//    Transaction dataTx = graphDatabaseService.beginTx();
//    
//    BiodbGraphDatabaseService service = new BiodbGraphDatabaseService(graphDatabaseService);
//    
//    BiodbMetaboliteNode cpdNode = service.getMetabolite("C00045", MetaboliteMajorLabel.LigandCompound);
//    
//    System.out.println(cpdNode.getId() + " " + Neo4jUtils.getPropertiesMap(cpdNode));
//    
//    Set<Node> metacycs = new HashSet<>();
//    
//    //primary labels
//    //metabolites MetaboliteMajorLabel
//    //pt.uminho.ceb.biosystems.transyt.service.reactions ReactionMajorLabel
//    
//    for (Relationship r : cpdNode.getRelationships(MetaboliteRelationshipType.has_crossreference_to)) {
//      Node refNode = r.getOtherNode(cpdNode);
//      
//      if (refNode.hasLabel(MetaboliteMajorLabel.MetaCyc) &&
//          refNode.hasLabel(GlobalLabel.Metabolite)) {
//        metacycs.add(refNode);
//      }
//    }
//    
//    for (Node refNode : metacycs) {
//      System.out.println("[r]>\t" + Neo4jUtils.getPropertiesMap(refNode));
//      for (Node p : Neo4jUtils.collectNodeRelationshipNodes(refNode, MetaboliteRelationshipType.instance_of)) {
//        BiodbMetaboliteNode bnode = new BiodbMetaboliteNode(p);
//        System.out.println("[p]>>" + bnode.getEntry());
//      }
//    }
//    
//    dataTx.failure(); dataTx.close();
//    
//    service.shutdown();
//  }
//}
//
