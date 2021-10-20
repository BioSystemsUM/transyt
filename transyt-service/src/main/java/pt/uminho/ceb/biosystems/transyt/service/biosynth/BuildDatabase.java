package pt.uminho.ceb.biosystems.transyt.service.biosynth;

import org.neo4j.graphdb.GraphDatabaseService;

import edu.uminho.biosynth.core.data.integration.neo4j.HelperNeo4jConfigInitializer;
import pt.uminho.sysbio.biosynth.integration.GraphMetaboliteEntity;
import pt.uminho.sysbio.biosynth.integration.etl.EtlTransform;
import pt.uminho.sysbio.biosynthframework.biodb.bigg.Bigg2MetaboliteEntity;
import pt.uminho.sysbio.biosynthframework.biodb.bigg.BiggMetaboliteEntity;
import pt.uminho.sysbio.biosynthframework.biodb.biocyc.BioCycMetaboliteEntity;
import pt.uminho.sysbio.biosynthframework.biodb.seed.ModelSeedMetaboliteEntity;
import pt.uminho.sysbio.biosynthframework.io.MetaboliteDao;

public class BuildDatabase {

	public static void main(String[] args) {
		
		System.out.println("setting GraphDatabaseService...");
		
		GraphDatabaseService graphDatabaseService = HelperNeo4jConfigInitializer
				.initializeNeo4jDataDatabaseConstraints("C:\\Users\\Davide\\Documents\\BASE DE DADOS BIOSYNTH\\db5_0");
		
		///////////////////////////////////////////////////////////
		System.out.println();
		System.out.println("requesting kegg compounds...");
		
		KEGG.etl(KEGG.getKeggCompoundMetaboliteDao() , KEGG.getKeggCompoundTransform(), graphDatabaseService);
		
		System.out.println("requesting kegg glycans...");
		
		KEGG.etl(KEGG.getKeggGlycanMetaboliteDao() , KEGG.getKeggGlycanTransform(), graphDatabaseService);	
		
		///////////////////////////////////////////////////////////
		
		System.out.println();
		System.out.println("opening BIGG1...");
		
		EtlTransform<BiggMetaboliteEntity, GraphMetaboliteEntity> t1 = BIGG1.getBiggMetaboliteTransform();
		
		MetaboliteDao<BiggMetaboliteEntity> daoBigg1 = BIGG1.getBigg1MetaboliteDatabase();
		
		System.out.println("transforming...");
		
		BIGG1.etl(daoBigg1, t1, graphDatabaseService);

		System.out.println("BIGG1 complete...");
		
		///////////////////////////////////////////////////////////
		System.out.println();
		System.out.println("opening BIGG2...");
		
		EtlTransform<Bigg2MetaboliteEntity, GraphMetaboliteEntity> t2 = BIGG2.getBIGG2MetaboliteTransform();
		MetaboliteDao<Bigg2MetaboliteEntity> daoBigg2 = BIGG2.getBigg2MetaboliteDao();
		
		System.out.println("transforming...");
		
		BIGG2.etl(daoBigg2 , t2, graphDatabaseService);
		
		System.out.println("BIGG2 complete...");
		
		///////////////////////////////////////////////////////////
		System.out.println();
		System.out.println("opening Metacyc META...");
		
		EtlTransform<BioCycMetaboliteEntity, GraphMetaboliteEntity> t3 = Metacyc.getMetacycMetaboliteTransform();
//		MetaboliteDao<BiggMetaboliteEntity> daoBigg = getBigg1MetaboliteDatabase();
		MetaboliteDao<BioCycMetaboliteEntity> daoBiocyc3 = Metacyc.getMetacycMetaboliteDao("META");
		
		System.out.println("transforming...");
		
		Metacyc.etl(daoBiocyc3 , t3, graphDatabaseService);
		
		System.out.println("Metacyc META complete...");
		
		///////////////////////////////////////////////////////////
		System.out.println();
		
		System.out.println("opening Metacyc ECOLI...");
		
		EtlTransform<BioCycMetaboliteEntity, GraphMetaboliteEntity> t4 = Metacyc.getMetacycMetaboliteTransform();
		MetaboliteDao<BioCycMetaboliteEntity> daoBiocyc4 = Metacyc.getMetacycMetaboliteDao("ECOLI");
		
		System.out.println("transforming...");
		
		Metacyc.etl(daoBiocyc4 , t4, graphDatabaseService);
		
		System.out.println("Metacyc ECOLI complete...");
		//////////////////////////////////////////////
		
		System.out.println("opening ModelSEED...");
		
		EtlTransform<ModelSeedMetaboliteEntity, GraphMetaboliteEntity> t0 = ModelSEED.getMetaboliteTransform();
		
		MetaboliteDao<ModelSeedMetaboliteEntity> dao0 = ModelSEED.getMetaboliteDatabase();
		
		System.out.println("transforming...");
		
		ModelSEED.etl(dao0 , t0, graphDatabaseService);
		
		System.out.println("ModelSEED complete...");
		
		ModelSEED.createModelSEEDRelationships(graphDatabaseService);
		
		System.out.println("database load complete...");
		
		graphDatabaseService.shutdown();
		
		System.out.println("shutdown...");
	}
	
	
}
