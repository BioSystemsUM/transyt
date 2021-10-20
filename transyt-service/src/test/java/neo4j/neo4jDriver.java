package neo4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import pt.uminho.ceb.biosystems.transyt.service.neo4jRest.RestNeo4jGraphDatabase;
import pt.uminho.ceb.biosystems.transyt.service.transytDatabase.TransytGeneralProperties;
import pt.uminho.ceb.biosystems.transyt.service.transytDatabase.TransytNode;
import pt.uminho.ceb.biosystems.transyt.service.transytDatabase.TransytNodeLabel;
import pt.uminho.ceb.biosystems.transyt.service.transytDatabase.TransytRelationshipType;

class neo4jDriver {

	@Test
	void test() {
		try {
			RestNeo4jGraphDatabase service = new RestNeo4jGraphDatabase( "bolt://localhost:7687", "neo4j", "password" );
			
//		service.getAllNodesByLabel(TransytNodeLabel.TC_Number);
//		service.findTcNumberNode("1.A.22.1.9");
//			service.getDownstreamRelationships(61, TransytRelationshipType.has_tc);
			
//			System.out.println(service.nodeHasRelationshipType(61, TransytRelationshipType.has_tc));
//			System.out.println(service.checkIfRelationshipExists(60, 61, TransytRelationshipType.has_tc));
			
			Map<String, String> coiso = new HashMap<>();
			
			coiso.put(TransytGeneralProperties.ReactionID.toString(), "TRSym__cpd00971o_cpd11574o");
			coiso.put(TransytGeneralProperties.Reaction.toString(), "molybdate (out) + Na+ (out)  $IRREV$  molybdate (in) + Na+ (in)");
			coiso.put(TransytGeneralProperties.Original_Reaction.toString(), "Anion2- (out) + nNa+ (out) $IRREV$ Anion2- (in) + nNa+ (in)");
			coiso.put("reaction string as retrieved from TCDB", "no");
			
			
//			service.createNode(TransytNodeLabel.TC_Number, TransytGeneralProperties.Confidence_Level, "meh");
			
//			service.createRelationship(48, 49, TransytRelationshipType.has_tc, TransytGeneralProperties.Confidence_Level, "uhhhhh");
			
//			service.getRelationship(48, 49, TransytRelationshipType.has_tc);
			
//			TransytNode accessionNode = service.findUniprotAccessionNode("O32167");
			
//			System.out.println(accessionNode == null);
			
			service.createNode(TransytNodeLabel.Reaction, coiso);
			
			System.out.println("done");
			
			service.close();
		} 
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
