package pt.uminho.ceb.biosystems.transyt.service.biosynth;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;

import pt.uminho.sysbio.biosynth.integration.GraphMetaboliteEntity;
import pt.uminho.sysbio.biosynth.integration.etl.CentralMetaboliteEtlDataCleansing;
import pt.uminho.sysbio.biosynth.integration.etl.DefaultMetaboliteEtlExtract;
import pt.uminho.sysbio.biosynth.integration.etl.EtlTransform;
import pt.uminho.sysbio.biosynth.integration.etl.HbmNeo4jHybridMetaboliteEtlPipeline;
import pt.uminho.sysbio.biosynth.integration.etl.HeterogenousMetaboliteEtlLoad;
import pt.uminho.sysbio.biosynth.integration.etl.biodb.biocyc.BiocycMetaboliteTransform;
import pt.uminho.sysbio.biosynth.integration.io.dao.neo4j.MetaboliteMajorLabel;
import pt.uminho.sysbio.biosynth.integration.io.dao.neo4j.Neo4jGraphMetaboliteDaoImpl;
import pt.uminho.sysbio.biosynthframework.Metabolite;
import pt.uminho.sysbio.biosynthframework.biodb.bigg.BiggMetaboliteEntity;
import pt.uminho.sysbio.biosynthframework.chemanalysis.cdk.CdkWrapper;
import pt.uminho.sysbio.biosynthframework.core.data.io.dao.biodb.bigg.CsvBiggMetaboliteDaoImpl;
import pt.uminho.sysbio.biosynthframework.core.data.io.dao.biodb.ptools.biocyc.RestBiocycMetaboliteDaoImpl;
import pt.uminho.sysbio.biosynthframework.core.data.io.factory.BiggDaoFactory;
import pt.uminho.sysbio.biosynthframework.io.MetaboliteDao;

public class Metacyc {
	
//	public static void main(String[] args) {
//
//		EtlTransform<BioCycMetaboliteEntity, GraphMetaboliteEntity> t = getMetacycMetaboliteTransform();
////		MetaboliteDao<BiggMetaboliteEntity> daoBigg = getBigg1MetaboliteDatabase();
//		MetaboliteDao<BioCycMetaboliteEntity> daoBiocyc = getMetacycMetaboliteDao();
//		etl(daoBiocyc , t);
//		
//	
//	}
	
	public static RestBiocycMetaboliteDaoImpl getMetacycMetaboliteDao(String pgdb) {
		RestBiocycMetaboliteDaoImpl dao = new RestBiocycMetaboliteDaoImpl();
		
		dao.setLocalStorage("C:\\Users\\Davide\\Documents\\BASE DE DADOS BIOSYNTH\\metacyc");
		dao.setDatabaseVersion("1.0");
		dao.setUseLocalStorage(true);
		dao.setSaveLocalStorage(true);
		dao.setPgdb(pgdb);
	
		return dao;
	}
	
	public static CsvBiggMetaboliteDaoImpl getBigg1MetaboliteDatabase() {

		CsvBiggMetaboliteDaoImpl source =
				new BiggDaoFactory().withFile(new File("C:\\\\Users\\\\Davide\\\\Documents\\\\BASE DE DADOS BIOSYNTH\\\\metaboliteList.tsv"))
				.buildCsvBiggMetaboliteDao();

//		System.out.println(source.getAllMetaboliteEntries());
//		
//		BiggMetaboliteEntity metabEntity = source.getMetaboliteByEntry("5mta");
//
//		System.out.println(metabEntity.getCompartments());
		
//		AnnotationEntityToGraphEntityTransform<BiggReactionEntity> entityTransform =
//				new AnnotationEntityToGraphEntityTransform<BiggReactionEntity>();

		//			GraphReactionEntity graphReactionEntity = entityTransform.etlTransform(biggReactionEntity);

		//			System.out.println(graphReactionEntity);
		
		return source;
		
	}
	
	public static BiocycMetaboliteTransform getMetacycMetaboliteTransform() {
		Map<String, String> map = new HashMap<> ();
		
		CsvBiggMetaboliteDaoImpl bigg1MetabDatabase = getBigg1MetaboliteDatabase();
		
		for (String cpdEntry : bigg1MetabDatabase.getAllMetaboliteEntries()) {
			BiggMetaboliteEntity cpd = bigg1MetabDatabase.getMetaboliteByEntry(cpdEntry);
			map.put(cpd.getInternalId().toString(), cpdEntry);
		}

		BiocycMetaboliteTransform t = new BiocycMetaboliteTransform(MetaboliteMajorLabel.MetaCyc.toString(), map);

		
		
		return t;
	}
	
	 public static<M extends Metabolite> void etl(MetaboliteDao<M> src, EtlTransform<M, GraphMetaboliteEntity> t,  GraphDatabaseService graphDatabaseService) {
//		 logger.info("Load database {}", src.getClass().getSimpleName());
		 
//		 GraphDatabaseService graphDatabaseService = 
//				 HelperNeo4jConfigInitializer.initializeNeo4jDataDatabaseConstraints("C:\\Users\\Davide\\Documents\\BASE DE DADOS BIOSYNTH\\db");
		 
		System.out.println("Load database " + src.getClass().getSimpleName());
	    
	    Neo4jGraphMetaboliteDaoImpl dst = new Neo4jGraphMetaboliteDaoImpl(graphDatabaseService);
	    
	    HbmNeo4jHybridMetaboliteEtlPipeline<M, GraphMetaboliteEntity> etlPipeline =
	        new HbmNeo4jHybridMetaboliteEtlPipeline<>();
//	    etlPipeline.exclude.add("D05511");
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
