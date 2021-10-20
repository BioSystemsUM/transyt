package pt.uminho.ceb.biosystems.transyt.service.biosynth;

import org.neo4j.graphdb.GraphDatabaseService;

import pt.uminho.sysbio.biosynth.integration.GraphMetaboliteEntity;
import pt.uminho.sysbio.biosynth.integration.etl.CentralMetaboliteEtlDataCleansing;
import pt.uminho.sysbio.biosynth.integration.etl.DefaultMetaboliteEtlExtract;
import pt.uminho.sysbio.biosynth.integration.etl.EtlTransform;
import pt.uminho.sysbio.biosynth.integration.etl.HbmNeo4jHybridMetaboliteEtlPipeline;
import pt.uminho.sysbio.biosynth.integration.etl.HeterogenousMetaboliteEtlLoad;
import pt.uminho.sysbio.biosynth.integration.etl.biodb.kegg.KeggCompoundTransform;
import pt.uminho.sysbio.biosynth.integration.etl.biodb.kegg.KeggDrugTransform;
import pt.uminho.sysbio.biosynth.integration.etl.biodb.kegg.KeggGlycanTransform;
import pt.uminho.sysbio.biosynth.integration.io.dao.neo4j.Neo4jGraphMetaboliteDaoImpl;
import pt.uminho.sysbio.biosynthframework.Metabolite;
import pt.uminho.sysbio.biosynthframework.chemanalysis.cdk.CdkWrapper;
import pt.uminho.sysbio.biosynthframework.core.data.io.dao.biodb.kegg.RestKeggCompoundMetaboliteDaoImpl;
import pt.uminho.sysbio.biosynthframework.core.data.io.dao.biodb.kegg.RestKeggDrugMetaboliteDaoImpl;
import pt.uminho.sysbio.biosynthframework.core.data.io.dao.biodb.kegg.RestKeggGlycanMetaboliteDaoImpl;
import pt.uminho.sysbio.biosynthframework.io.MetaboliteDao;


public class KEGG {

//	public static void main(String[] args) {
//
////		System.out.println("requesting kegg compounds...");
//		
////		etl(getKeggCompoundMetaboliteDao() , getKeggCompoundTransform());
//		
////		System.out.println("requesting kegg glycans...");
//		
////		etl(getKeggGlycanMetaboliteDao() , getKeggGlycanTransform());
//		
//		System.out.println("requesting kegg drugs...");
//		
//		etl(getKeggDrugMetaboliteDao() , getKeggDrugTransform());
//
//		System.out.println("done...");
//		
//	}

	public static RestKeggCompoundMetaboliteDaoImpl getKeggCompoundMetaboliteDao() {

		return  new RestKeggCompoundMetaboliteDaoImpl();
	}
	

	public static RestKeggDrugMetaboliteDaoImpl getKeggDrugMetaboliteDao() {

		return new RestKeggDrugMetaboliteDaoImpl();
	}
	

	public static RestKeggGlycanMetaboliteDaoImpl getKeggGlycanMetaboliteDao() {

		return new RestKeggGlycanMetaboliteDaoImpl();
	}
	
	public static KeggCompoundTransform getKeggCompoundTransform() {

		return new KeggCompoundTransform();
	}
	
	public static KeggDrugTransform getKeggDrugTransform() {

		return new KeggDrugTransform();
	}
	
	public static KeggGlycanTransform getKeggGlycanTransform() {

		return new KeggGlycanTransform();
	}

	public static<M extends Metabolite> void etl(MetaboliteDao<M> src, EtlTransform<M, GraphMetaboliteEntity> t, GraphDatabaseService graphDatabaseService) {
		//			 logger.info("Load database {}", src.getClass().getSimpleName());
		
//		GraphDatabaseService graphDatabaseService = 
//				HelperNeo4jConfigInitializer.initializeNeo4jDataDatabaseConstraints("C:\\Users\\Davide\\Documents\\BASE DE DADOS BIOSYNTH\\db");
		
//		System.out.println("Load database " + src.getClass().getSimpleName());

		Neo4jGraphMetaboliteDaoImpl dst = new Neo4jGraphMetaboliteDaoImpl(graphDatabaseService);

		HbmNeo4jHybridMetaboliteEtlPipeline<M, GraphMetaboliteEntity> etlPipeline =
				new HbmNeo4jHybridMetaboliteEtlPipeline<>();
				    etlPipeline.exclude.add("D05511");
		etlPipeline.setSkipLoad(false);
		etlPipeline.setGraphDatabaseService(graphDatabaseService);
		etlPipeline.setSessionFactory(null);
		etlPipeline.setEtlDataCleasingSubsystem(new CentralMetaboliteEtlDataCleansing(new CdkWrapper()));
		etlPipeline.setExtractSubsystem(new DefaultMetaboliteEtlExtract<M>(src));
		etlPipeline.setLoadSubsystem(new HeterogenousMetaboliteEtlLoad<GraphMetaboliteEntity>(dst));
		etlPipeline.setTransformSubsystem(t);
		etlPipeline.etl();	
		
	}




}
