package pt.uminho.ceb.biosystems.transyt.service.kbase;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONObject;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cliftonlabs.json_simple.Jsoner;

import pt.uminho.ceb.biosystems.transyt.service.transytDatabase.TransytGraphDatabaseService;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.TcNumberContainer;
import pt.uminho.ceb.biosystems.transyt.utilities.files.JSONFilesUtils;
import pt.uminho.ceb.biosystems.transyt.utilities.files.ReadExcelFile;
import pt.uminho.ceb.biosystems.transyt.utilities.transytUtilities.Properties;

/**
 * @author Davide
 *
 */
public class Reports {
	
	private static final Logger logger = LoggerFactory.getLogger(Reports.class);

	public static Map<String, String> generateKBaseReport(String jobIdentification, String organism, Integer taxID, Integer totalOfGenes, 
			Properties properties, Map<String, Map<String, Set<String>>> results, TransytGraphDatabaseService service) {

		String fileName = jobIdentification.replaceAll("\\.", "_").concat("_qCov_").concat(Double.toString(properties.getQueryCoverage())).concat("_eValThresh_").concat(Double.toString(properties.geteValueThreshold())).concat(".json");

		Map<String, TcNumberContainer> backupData = JSONFilesUtils.readDataBackupFile();

		Map<String, String> descriptions = new HashMap<>();
		List<String[]> data = ReadExcelFile.getData("C:\\Users\\Davide\\OneDrive - Universidade do Minho\\UMinho\\Tese\\Internal database\\results2.xlsx", true, null);

		for(String[] line : data) {
			if(!descriptions.containsKey(line[3]))
				descriptions.put(line[3], line[4]);
		}

		Set<String> reactions = new HashSet<>();
		Map<String, String> reactionsByIDS = new HashMap<>();

		for(String key : results.keySet()) {
			
			for(String tc : results.get(key).keySet()) {
				
				reactions.addAll(results.get(key).get(tc));

				for(String r : results.get(key).get(tc)) {

					if(!reactionsByIDS.containsKey(r)) {
						
						Node node = service.findReactionNode(r);
						
						if(node != null) {
//							String reaction = node.getProperty(TransytGeneralProperties.ReactionModelSEED.toString()).toString();		//ALTERAR ISTO SE FOREM FEITOS MAIS REPORTS!!! ISTO FOI ALTERADO!!!!

//							reactionsByIDS.put(r, reaction);
						}
					}
				}
			}
		}

		Set<String> allReactions = new HashSet<>();

		Integer transportGenes = results.size();

		Integer totalGenesAnnotated = 0;

		String path = "C:\\Users\\Davide\\OneDrive - Universidade do Minho\\UMinho\\Tese\\KBase\\Reports\\".concat(fileName);

		Set<String> totalFamilyTCs = new HashSet<>();

		try {
			FileWriter file = new FileWriter(path);

			JSONObject stats = new JSONObject();

			stats.put("Identification", jobIdentification);
			stats.put("Source", "refseq");
			stats.put("Organism", organism);
			stats.put("Taxonomy ID", taxID);
			stats.put("Total of genes", totalOfGenes);
			stats.put("Total of Genes with results", transportGenes);
			stats.put("Blast Evalue threshold", properties.getBlastEvalueThreshold());
			stats.put("Query coverage threshold", properties.getQueryCoverage());
			stats.put("Bit score threshold", properties.getBitScore());
			stats.put("Evalue threshold to automatically accept gene as transporter", properties.geteValueThreshold());

			JSONObject mappingObj = new JSONObject();

			for(String queryAccession : results.keySet()) {

				JSONObject obj = new JSONObject();

				Map<String, Set<String>> container = results.get(queryAccession);

				//				obj.put("TC Number", tc);

				//				Set<String> reactionsByAccession = new HashSet<>();

				JSONObject obj2 = new JSONObject();

				String tcFamily = "";

				if(!tcFamily.equals("none"))
					totalGenesAnnotated++;

				totalFamilyTCs.add(tcFamily);

				Set<String> selectedReactions = new HashSet<>();

				for(String tc : container.keySet()) {

					if(tc.contains(tcFamily))
						selectedReactions.addAll(container.get(tc));

				}

				allReactions.addAll(selectedReactions);

				String familyDescription = "none";

				if(backupData.containsKey(tcFamily))
					familyDescription = backupData.get(tcFamily).getFamily();

				obj2.put("TC Family", tcFamily);
				obj2.put("Family description", familyDescription);
				obj2.put("Reactions", selectedReactions.toString());


				JSONObject obj3 = new JSONObject();

				for(String tc : container.keySet()) {

					String str = "No description found!";

					if(descriptions.get(tc) != null)
						str = descriptions.get(tc);

					obj3.put(tc, str);

				}

				obj.put("annotation", obj2);
				obj.put("homologies", obj3);



				//				JSONObject reactionObject = new JSONObject();

				//				for(Integer idReaction : container.getAllReactionsIds()) {
				//
				//					ReactionContainer reaction = container.getReactionContainer(idReaction);
				//
				//					JSONObject properties = new JSONObject();
				//
				//					properties.put("reaction", reaction.getReaction());
				//					properties.put("reactants", reaction.getReactant());
				//					properties.put("products", reaction.getProduct());
				//					properties.put("reactants", reaction.getReactant());
				//					properties.put("reversible", reaction.isReversible());
				//					properties.put("transport type", reaction.getTransportType().toString());
				//
				//					for(String key : reaction.getProperties().keySet())
				//						properties.put(key, reaction.getProperties().get(key));
				//
				//					reactionObject.put(idReaction, properties);
				//
				//
				//				}

				//				obj.put("TC Numbers List", reactionObject);
				// try-with-resources statement based on post comment below :)

//				mappingObj.put(queryAccession.split("\\s+")[0], obj);
								mappingObj.put(queryAccession, obj);

			}

			Integer totalReactionsGenerated = reactions.size();
			double averageReactionsPerGene = totalReactionsGenerated / totalGenesAnnotated;

			stats.put("Total of TC families associated", totalFamilyTCs.size());
			stats.put("Total of pt.uminho.ceb.biosystems.transyt.service.reactions generated", totalReactionsGenerated);
			stats.put("Average of pt.uminho.ceb.biosystems.transyt.service.reactions by transporter", averageReactionsPerGene);
			stats.put("Total of Genes annotated", totalGenesAnnotated);
			stats.put("Results", mappingObj);

			file.write(Jsoner.prettyPrint(stats.toJSONString()));

			file.close();
			
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		return reactionsByIDS;
	}

	/**
	 * @param container
	 * @return
	 */
	public static String getFamilyForAnnotation(Map<String, Set<String>> container) { 
		Map<String, Integer> counts = new HashMap<>();

		int maxCount = 0;
		String tcFamily = "none";

		for(String tc : container.keySet()) {

			String[] split = tc.split("\\.");

			try {
				tc = split[0].concat(".").concat(split[1]).concat(".").concat(split[2]).concat(".");
			} 
			catch (Exception e) {
				System.out.println("ERRO FAMILIA " + tc);
			}

			if(counts.containsKey(tc)) {

				int count = counts.get(tc) + 1;

				counts.put(tc, count);

			}
			else {
				counts.put(tc, 1);
			}

			if(counts.get(tc) > maxCount) {

				maxCount = counts.get(tc);
				tcFamily =  tc;
			}
		}

		return tcFamily;
	}

	public static void saveReportByEvalue(String reportPath, Map<String, Map<String, Double>> reportByEvalue,
			Map<String, Map<String, Set<String>>> reportByEvalueAux) {
		
		reportPath = reportPath.concat("scoresMethod1.txt");
		
		try {

			PrintWriter writer = new PrintWriter(reportPath, "UTF-8");

			for(String key : reportByEvalueAux.keySet()) {
				writer.println(">" + key);
//				System.out.println(">" + key);
				
				for(String tcNumber : reportByEvalueAux.get(key).keySet()) {
				
					writer.println(tcNumber + " - Evalue: " + reportByEvalue.get(key).get(tcNumber) + "\t" + reportByEvalueAux.get(key).get(tcNumber).toString());
//					System.out.println(tcNumber + " - Evalue: " + reportByEvalue.get(key).get(tcNumber) + "\t" + reportByEvalueAux.get(key).get(tcNumber).toString());
				}
				writer.println();
			}


			writer.close();
			
			logger.info("Reactions association report using method-1 saved at: {}", reportPath);

		} 
		catch (FileNotFoundException e) {
			e.printStackTrace();

		} 
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
	}

}
