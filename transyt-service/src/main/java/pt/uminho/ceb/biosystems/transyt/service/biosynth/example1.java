package pt.uminho.ceb.biosystems.transyt.service.biosynth;
//package pt.uminho.ceb.biosystems.transyt.service.biosynth;
//
//import java.util.HashMap;
//import java.util.Map;
//
//import pt.uminho.sysbio.biosynth.integration.GraphMetaboliteEntity;
//import pt.uminho.sysbio.biosynth.integration.etl.CentralMetaboliteEtlDataCleansing;
//import pt.uminho.sysbio.biosynth.integration.etl.DefaultMetaboliteEtlExtract;
//import pt.uminho.sysbio.biosynth.integration.etl.EtlTransform;
//import pt.uminho.sysbio.biosynth.integration.etl.HbmNeo4jHybridMetaboliteEtlPipeline;
//import pt.uminho.sysbio.biosynth.integration.etl.HeterogenousMetaboliteEtlLoad;
//import pt.uminho.sysbio.biosynth.integration.etl.biodb.LipidmapsMetaboliteTransform;
//import pt.uminho.sysbio.biosynth.integration.etl.biodb.ModelSeedMetaboliteTransform;
//import pt.uminho.sysbio.biosynth.integration.etl.biodb.bigg.Bigg2MetaboliteTransform;
//import pt.uminho.sysbio.biosynth.integration.etl.biodb.bigg.BiggMetaboliteTransform;
//import pt.uminho.sysbio.biosynth.integration.etl.biodb.biocyc.BiocycMetaboliteTransform;
//import pt.uminho.sysbio.biosynth.integration.etl.biodb.kegg.KeggCompoundTransform;
//import pt.uminho.sysbio.biosynth.integration.etl.biodb.kegg.KeggDrugTransform;
//import pt.uminho.sysbio.biosynth.integration.etl.biodb.kegg.KeggGlycanTransform;
//import pt.uminho.sysbio.biosynth.integration.io.dao.neo4j.MetaboliteMajorLabel;
//import pt.uminho.sysbio.biosynth.integration.io.dao.neo4j.Neo4jGraphMetaboliteDaoImpl;
//import pt.uminho.sysbio.biosynthframework.Metabolite;
//import pt.uminho.sysbio.biosynthframework.biodb.bigg.BiggMetaboliteEntity;
//import pt.uminho.sysbio.biosynthframework.chemanalysis.cdk.CdkWrapper;
//import pt.uminho.sysbio.biosynthframework.core.data.io.dao.biodb.ptools.biocyc.RestBiocycMetaboliteDaoImpl;
//import pt.uminho.sysbio.biosynthframework.io.MetaboliteDao;
//
//public class example1 {
//	  public static RestBiocycMetaboliteDaoImpl getMetacycMetaboliteDao() {
//		    RestBiocycMetaboliteDaoImpl dao = new RestBiocycMetaboliteDaoImpl();
//		    dao.setLocalStorage(BiosynthConfiguration.BIOCYC_DATA);
//		    dao.setDatabaseVersion(BiosynthConfiguration.METACYC_VERION_21_1);
//		    dao.setUseLocalStorage(true);
//		    dao.setSaveLocalStorage(true);
//		    dao.setPgdb(BiosynthConfiguration.PGDB);
//
//		    return dao;
//		  }
//		  
//		    public static BiocycMetaboliteTransform getMetacycMetaboliteTransform() {
//		    Map<String, String> map = new HashMap<> ();
//		    for (String cpdEntry : getBigg1MetaboliteDatabase().getAllMetaboliteEntries()) {
//		      BiggMetaboliteEntity cpd = getBigg1MetaboliteDatabase().getMetaboliteByEntry(cpdEntry);
//		      map.put(cpd.getInternalId().toString(), cpdEntry);
//		    }
//
//		    BiocycMetaboliteTransform t = new BiocycMetaboliteTransform(MetaboliteMajorLabel.MetaCyc.toString(), map);
//
//		    return t;
//		  }
//		  
//		    public static<M extends Metabolite> void etl(MetaboliteDao<M> src, EtlTransform<M, GraphMetaboliteEntity> t) {
//		    logger.info("Load database {}", src.getClass().getSimpleName());
//		    
//		    Neo4jGraphMetaboliteDaoImpl dst = new Neo4jGraphMetaboliteDaoImpl(graphDatabaseService);
//		    
//		    HbmNeo4jHybridMetaboliteEtlPipeline<M, GraphMetaboliteEntity> etlPipeline =
//		        new HbmNeo4jHybridMetaboliteEtlPipeline<>();
//		    etlPipeline.exclude.add("D05511");
//		    etlPipeline.setSkipLoad(false);
//		    etlPipeline.setGraphDatabaseService(graphDatabaseService);
//		    etlPipeline.setSessionFactory(null);
//		    etlPipeline.setEtlDataCleasingSubsystem(new CentralMetaboliteEtlDataCleansing(new CdkWrapper()));
//		    etlPipeline.setExtractSubsystem(new DefaultMetaboliteEtlExtract<M>(src));
//		    etlPipeline.setLoadSubsystem(new HeterogenousMetaboliteEtlLoad<GraphMetaboliteEntity>(dst));
//		    etlPipeline.setTransformSubsystem(t);
//		    etlPipeline.etl();
//		  }
//
//		public static void main(String[] args) {
//		    graphDatabaseService = getDataDatabase();
//		    
//		    //KEGG, MetaCyc, Bigg, Bigg2
//		    
////		    etl(getKeggCompoundDao(), new KeggCompoundTransform());
////		    etl(getKeggDrugDao(), new KeggDrugTransform());
////		    etl(getKeggGlycanDao(), new KeggGlycanTransform());		com erros penso eu estes tres
//		    
////		    etl(getBigg1MetaboliteDatabase(), new BiggMetaboliteTransform());
////		    etl(getBigg2MetaboliteDao(), new Bigg2MetaboliteTransform());
////		    etl(getMetacycMetaboliteDao(), getMetacycMetaboliteTransform());
//		    etl(getHmdbMetaboliteDao(), getHmdbMetaboliteTransform());
//		    etl(getLipidmapsMetaboliteDao(), new LipidmapsMetaboliteTransform());
//		    etl(getModelseedMetaboliteDao(), new ModelSeedMetaboliteTransform());
//		   
//		    //these are pt.uminho.ceb.biosystems.transyt.service.reactions, don't download for now 
//		    etlReaction(getKeggReactionDao(), getKeggReactionTransform());
//		    etlReaction(getBiocycReactionDao(), getMetacycReactionTransform());
//		    etlReaction(getBigg2ReactionDao(), getBigg2ReactionTransform());
//		    
//		    //????
//		    etlMetaCycProxies(graphDatabaseService);
//		    etlFixBiGG(graphDatabaseService);
//		    etlMetacycRegulation(graphDatabaseService);
//		    
//		    graphDatabaseService.shutdown();
//		  }
//}