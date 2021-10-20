package pt.uminho.ceb.biosystems.transyt.service.biosynth;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;

import pt.uminho.ceb.biosystems.transyt.service.internalDB.FetchCompoundsByName;
import pt.uminho.ceb.biosystems.transyt.service.internalDB.WriteByMetabolitesID;
import pt.uminho.ceb.biosystems.transyt.utilities.files.FilesUtils;
import pt.uminho.ceb.biosystems.transyt.utilities.transytUtilities.Utilities;
import pt.uminho.sysbio.biosynth.integration.GraphMetaboliteEntity;
import pt.uminho.sysbio.biosynth.integration.etl.CentralMetaboliteEtlDataCleansing;
import pt.uminho.sysbio.biosynth.integration.etl.DefaultMetaboliteEtlExtract;
import pt.uminho.sysbio.biosynth.integration.etl.EtlTransform;
import pt.uminho.sysbio.biosynth.integration.etl.HbmNeo4jHybridMetaboliteEtlPipeline;
import pt.uminho.sysbio.biosynth.integration.etl.HeterogenousMetaboliteEtlLoad;
import pt.uminho.sysbio.biosynth.integration.etl.biodb.ModelSeedMetaboliteTransform;
import pt.uminho.sysbio.biosynth.integration.io.dao.neo4j.MetaboliteMajorLabel;
import pt.uminho.sysbio.biosynth.integration.io.dao.neo4j.Neo4jGraphMetaboliteDaoImpl;
import pt.uminho.sysbio.biosynth.integration.neo4j.BiodbMetaboliteNode;
import pt.uminho.sysbio.biosynthframework.BiodbGraphDatabaseService;
import pt.uminho.sysbio.biosynthframework.Metabolite;
import pt.uminho.sysbio.biosynthframework.chemanalysis.cdk.CdkWrapper;
import pt.uminho.sysbio.biosynthframework.io.MetaboliteDao;

public class ModelSEED {

	private static final Logger logger = LoggerFactory.getLogger(WriteByMetabolitesID.class);

	//	public static void main(String[] args) {
	//
	//		System.out.println("opening...");
	//		
	//		EtlTransform<ModelSeedMetaboliteEntity, GraphMetaboliteEntity> t = getMetaboliteTransform();
	//		
	//		MetaboliteDao<ModelSeedMetaboliteEntity> dao = getMetaboliteDatabase();
	//		
	//		System.out.println("transforming...");
	//		
	//		etl(dao , t);
	//		
	//		System.out.println("finnish...");
	//
	//	}

	public static Json2ModelSeedMetaboliteDaoImpl getMetaboliteDatabase() {


		Json2ModelSeedMetaboliteDaoImpl dao = new Json2ModelSeedMetaboliteDaoImpl(new FileSystemResource(new File("C:\\Users\\Davide\\Desktop\\compounds.json")));

		return dao;

	}

	public static ModelSeedMetaboliteTransform getMetaboliteTransform() {

		ModelSeedMetaboliteTransform t = new ModelSeedMetaboliteTransform();

		return t;
	}

	public static <M extends Metabolite> void etl(MetaboliteDao<M> src, EtlTransform<M, GraphMetaboliteEntity> t, GraphDatabaseService graphDatabaseService) {
		// logger.info("Load database {}", src.getClass().getSimpleName());

		System.out.println("Load database " + src.getClass().getSimpleName());

		Neo4jGraphMetaboliteDaoImpl dst = new Neo4jGraphMetaboliteDaoImpl(graphDatabaseService);

		HbmNeo4jHybridMetaboliteEtlPipeline<M, GraphMetaboliteEntity> etlPipeline = new HbmNeo4jHybridMetaboliteEtlPipeline<>();
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

	public static void createModelSEEDRelationships(GraphDatabaseService graphDatabaseService) {

		Map<MetaboliteMajorLabel, Map<String, Set<String>>> data = readSEEDRelationshipsFile();

				Transaction dataTx = graphDatabaseService.beginTx();
		
				BiodbGraphDatabaseService service = new BiodbGraphDatabaseService(graphDatabaseService);
				
				for(MetaboliteMajorLabel label : data.keySet()) {
				
					for(String modelSEEDid : data.get(label).keySet()) {
						
						BiodbMetaboliteNode node = service.getMetabolite(modelSEEDid, MetaboliteMajorLabel.ModelSeed);
						
						if(node != null) {
				
							for(String externalID : data.get(label).get(modelSEEDid)) {
								
								if(node != null) {
									
									try {
										BiodbMetaboliteNode node2 = service.getMetabolite(externalID, label);
										
										node.createRelationshipTo(node2, RelationshipType.withName("has_crossreference_to"));
									
										String msg = "Relationship created between compound ".concat(externalID).concat(" with source ").concat(label.toString())
												.concat(" and ModelSEED compound ").concat(modelSEEDid);
										logger.info(msg);
									} 
									catch (Exception e) {
										String msg = "Compound ".concat(externalID).concat(" with source ").concat(label.toString())
												.concat(" not found to create relationship with ModelSEED compound ").concat(modelSEEDid);
										logger.warn(msg);
									}
								
								}
								else {
									String msg = "Compound ".concat(externalID).concat(" with source ").concat(label.toString())
											.concat(" not found to create relationship with ModelSEED compound ").concat(modelSEEDid);
									logger.warn(msg);
								}
								}
						}
						else
							logger.warn("Relationship NOT created for ModelSEED compound {}", modelSEEDid);
					}
				}
				
				dataTx.success();
				
				logger.info("All reationships created!");
				
				dataTx.close();
				service.shutdown();

	}

	public static Map<MetaboliteMajorLabel, Map<String, Set<String>>> readSEEDRelationshipsFile() {

		Map<MetaboliteMajorLabel, Map<String, Set<String>>> data = new HashMap<>();

		String path = System.getProperty("user.dir").concat("\\src\\main\\resources\\Compounds_Aliases.tsv");

		logger.info("Reading external references file at: {}", path);

		try {
			BufferedReader reader = new BufferedReader(new FileReader(path));

			String line;

			while ((line = reader.readLine()) != null) {

				if(!line.isEmpty() && !line.startsWith("MS ID")) {

					Map<String, Set<String>> submap = new HashMap<>();

					String[] text = line.split("\t");

					String id = text[0].trim();
					String oldID;
					String externalID = text[2];
					String source = text[3].trim();
					
					if(text.length == 3) 
						externalID = text[1].trim();
					else if (!text[1].trim().isEmpty()) {
						oldID = text[1].trim();
					
					Integer currentID = FetchCompoundsByName.getIDNumberFormat(id, MetaboliteMajorLabel.ModelSeed);

					Integer previousID = FetchCompoundsByName.getIDNumberFormat(oldID, MetaboliteMajorLabel.ModelSeed);

						if(previousID < currentID) {
							id = oldID;
						}
					}
					
					MetaboliteMajorLabel label = null;

					if(source.equalsIgnoreCase("BiGG1"))
						label = MetaboliteMajorLabel.BiGG;

					else if(source.equalsIgnoreCase("KEGG")) {
						label = MetaboliteMajorLabel.LigandCompound;
						
						if(externalID.startsWith("G"))
							label = MetaboliteMajorLabel.LigandGlycan;
					}

					else if(source.equalsIgnoreCase("MetaCyc")) {
						label = MetaboliteMajorLabel.MetaCyc;
						externalID = "META:".concat(externalID);
					}

					if(source.equalsIgnoreCase("BiGG"))
						label = MetaboliteMajorLabel.BiGGMetabolite;

					if(label != null && !id.isEmpty() && !externalID.isEmpty()) {

						if(!data.containsKey(label)) 
							data.put(label, new HashMap<>());

						submap = data.get(label);

						if(submap.containsKey(id)) { 
							submap.get(id).add(externalID);
						}
						else {
							Set<String> set = new HashSet<>();
							set.add(externalID);
							
							submap.put(id, set);
						}
						
						data.put(label, submap);
					}
				}
			}

			reader.close();
			
		} 
		catch (FileNotFoundException e) {
			e.printStackTrace();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println(data);

		return data;
	}

}
