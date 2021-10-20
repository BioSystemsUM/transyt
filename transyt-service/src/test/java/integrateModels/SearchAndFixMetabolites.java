package integrateModels;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;

import pt.uminho.ceb.biosystems.transyt.service.biosynth.initializeNeo4jdb;
import pt.uminho.ceb.biosystems.transyt.service.containers.BiosynthMetabolites;
import pt.uminho.ceb.biosystems.transyt.service.internalDB.FetchCompoundsByName;
import pt.uminho.sysbio.biosynth.integration.io.dao.neo4j.MetaboliteMajorLabel;
import pt.uminho.sysbio.biosynth.integration.neo4j.BiodbMetaboliteNode;
import pt.uminho.sysbio.biosynthframework.BiodbGraphDatabaseService;

public class SearchAndFixMetabolites {

	private static final String WORKDIR = "C:\\Users\\Davide\\Downloads\\joana_models";

	public static void main(String[] args) {		//MADE FOR <sbml xmlns="http://www.sbml.org/sbml/level2" level="2" version="1" xmlns:html="http://www.w3.org/1999/xhtml">

		GraphDatabaseService graphDatabaseService = initializeNeo4jdb.getDataDatabase(null);	//using Liu's Biosynth database
		Transaction dataTx = graphDatabaseService.beginTx();

		BiodbGraphDatabaseService service = new BiodbGraphDatabaseService(graphDatabaseService);

		BiosynthMetabolites namesAndIDsContainer = new FetchCompoundsByName(service, true).getResults();		//168966 names

//		@SuppressWarnings("resource")
//		Scanner reader = new Scanner(System.in);
//
//		int n = 1;
//
//		while (n != 99) {
			
			execute(service, namesAndIDsContainer);

//			System.out.println("Enter a random number to repeat (100 to repeat data retrieval) or 99 to finish: ");
//
//			try {
//				n = reader.nextInt();
//			} catch (Exception e) {
//				e.printStackTrace();
//
//				n = reader.nextInt();
//			}
//
//		}
		
		dataTx.failure();
		dataTx.close();
		service.shutdown();
		graphDatabaseService.shutdown();

		System.out.println("Done!!!");

	}
	
	private static void execute(BiodbGraphDatabaseService service, BiosynthMetabolites namesAndIDsContainer) {
		
		Map<String, String> allMetabolitesFound = new HashMap<>();
		Map<String, String> allMetabolitesFormulas = new HashMap<>();
		Map<String, String> allMetabolitesNames = new HashMap<>();
		Map<String, String> allMetabolitesCharges = new HashMap<>();
		Map<String, String> differentDatabase = new HashMap<>();
		Set<String> notFound = new HashSet<>();
		Set<String> searchedByName = new HashSet<>();
		Set<String> checkFormula = new HashSet<>();
		
		allMetabolitesFound.put("Biomass", "Biomass");
		allMetabolitesFormulas.put("Biomass", "");
		allMetabolitesNames.put("Biomass", "Biomass");
		
		File folder = new File(WORKDIR);
		File[] listOfFiles = folder.listFiles();
		
		File reports = new File(WORKDIR.concat("\\reports"));
		reports.mkdir();
		
		File results = new File(WORKDIR.concat("\\results"));
		results.mkdir();

		for(File f : listOfFiles) {
			try {
				
				System.out.println(f.getName());

				BufferedReader reader = new BufferedReader(new FileReader(f.getAbsolutePath()));
				
				PrintWriter writer = new PrintWriter(results.getAbsolutePath().concat("\\").concat(f.getName()), "UTF-8");

				String line;

				Set<String> notFoundForFile = new HashSet<>();
				Map<String, String> differentDatabaseForFile = new HashMap<>();
				Set<String> searchedByNameForFile = new HashSet<>();

				while ((line = reader.readLine()) != null) {

					if(line.contains("<species ")) {
						
//						System.out.println(line);
						
						String[] features = line.split(" ");

						String name = "";
						String id = "";
						String oldId = "";
						String oldName = "";
						String formula = null;
						String charge = null;
						String newName = null;
						String newId = null;

						for(String feature : features) {
							
							if(feature.contains("name=")) {
								oldName = feature.split("=")[1].replaceAll("\"", "");
								name = feature.split("=")[1].replaceAll("_*\\[[pext]\\]\"", "").replaceAll("\"", "");
							}
							else if(feature.contains("id=")) {

								oldId = feature.split("=")[1].replaceAll("^\"M*_*", "").replaceAll("_*[Extrecp]*\">", ""); //only works with compartments represented by this letters (check later if necessary)
								id = oldId.replace("_DASH_", "__").replace("CPD", "cpd");
							}	//add more fields if necessary
						}
						
						if(allMetabolitesFound.containsKey(id)) {

							if(allMetabolitesNames.containsKey(id)) 
								newName = allMetabolitesNames.get(id);

							if(allMetabolitesFormulas.containsKey(id))
								formula = allMetabolitesFormulas.get(id);

							if(allMetabolitesCharges.containsKey(id))
								charge = allMetabolitesCharges.get(id);

							newId = allMetabolitesFound.get(id);
							
							if(differentDatabase.containsKey(oldId))
								differentDatabaseForFile.put(oldId, differentDatabase.get(oldId));
							
							if(searchedByName.contains(oldId))
								searchedByNameForFile.add(oldId);

						}
						else if(!notFound.contains(id)){
							BiodbMetaboliteNode node = searchMetaboliteByID(id, null, service);

							if(node == null) {

								node = searchByName(name, namesAndIDsContainer, service);
								
								if(node == null)
									notFound.add(oldId);
								else {
									searchedByName.add(oldId);
									searchedByNameForFile.add(oldId);
								}
							}
							
							if(node != null) {

								Map<String, Object> nodeProperties = node.getAllProperties();

								if(nodeProperties.containsKey("major_label")) {

									String database = nodeProperties.get("major_label").toString();

									if(nodeProperties.containsKey("entry")) {
										newId = nodeProperties.get("entry").toString();
										allMetabolitesFound.put(id, newId);
									}

									if(nodeProperties.containsKey("formula")) {
										formula = nodeProperties.get("formula").toString();
										allMetabolitesFormulas.put(id, formula);
									}

									if(nodeProperties.containsKey("name")) {
										newName = nodeProperties.get("name").toString();
										allMetabolitesNames.put(id, newName);
									}

									if(nodeProperties.containsKey("charge")) {
										charge = nodeProperties.get("charge").toString();
										allMetabolitesCharges.put(id, charge);
									}

									if(!database.contains("BiGG")) {
										differentDatabase.put(oldId, newId);
										differentDatabaseForFile.put(oldId, differentDatabase.get(id));
									}
								}
							}
							else
								notFound.add(oldId);
						}
						
						if(notFound.contains(oldId))
							notFoundForFile.add(oldId);

						if(newName != null)
							line = line.replace(oldName, newName);

						if(newId != null)
							line = line.replace(oldId, newId);

						if(formula != null) {
							
							if(formula.contains(";")) {
								
								formula = formula.split(";")[0];
								
								checkFormula.add(newId);
							}	
								
							line = line.replace(">", " ".concat("formula=\"").concat(formula).concat("\">"));
							
						}

//						if(charge != null)
//							line = line.replace(">", " ".concat("charge=").concat(charge).concat(">"));

					}

					writer.println(line);
					
				} 
				
				saveReports(reports.getAbsolutePath().concat("\\").concat(f.getName()), 
						notFoundForFile, searchedByNameForFile, differentDatabaseForFile, checkFormula);
				
				writer.close();
				reader.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		
	}

	private static void saveReports(String path, Set<String> notFoundForFile, Set<String> searchedByNameForFile,
			Map<String, String> differentDatabaseForFile, Set<String> checkFormula) {
		
		try {
			
			System.out.print("saving report... ");

			PrintWriter writer = new PrintWriter(path.replace(".xml", ".txt"), "UTF-8");

			
			writer.println("Entries not found");
			writer.println(notFoundForFile.toString());
			writer.println();

			writer.println("Entries searched by name");
			writer.println(searchedByNameForFile.toString());
			writer.println();
			
			writer.println("Entries with different database");
			writer.println(differentDatabaseForFile.toString());
			writer.println();
			
			writer.println("Check the molecular formula of the following compounds");
			writer.println(checkFormula.toString());
			writer.println();

			writer.close();
			
			System.out.println("done!");

		} 
		catch (FileNotFoundException e) {
			e.printStackTrace();

		} 
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
	}

	private static BiodbMetaboliteNode searchByName(String name, BiosynthMetabolites namesAndIDsContainer,
			BiodbGraphDatabaseService service) {

		IdentifyReactionsMetabolites2 identification = new IdentifyReactionsMetabolites2(name, namesAndIDsContainer, service);

		Map<String, Map<String, MetaboliteMajorLabel>> identified = identification.getTcdbMetabolitesIDs();

		if(!identified.isEmpty()) {
			for(String key : identified.get(name).keySet())
				return searchMetaboliteByID(key, identified.get(name).get(key), service);
		}

		return null;
	}

	private static Boolean isLabelBiGG(Map<String, Object> nodeProperties) {

		String label = null;

		if(nodeProperties.containsKey("major_label"))
			label = nodeProperties.get("major_label").toString();
		else 
			return null;

		if(label.contains("BiGG"))
			return true;
		else
			return false;

	}

	private static BiodbMetaboliteNode searchMetaboliteByID(String id, MetaboliteMajorLabel defaultLabel, BiodbGraphDatabaseService service) {

		MetaboliteMajorLabel label = MetaboliteMajorLabel.BiGGMetabolite;
		
		if(defaultLabel == null) {

			if(id.matches("^cpd\\d+"))
				label = MetaboliteMajorLabel.ModelSeed;
			else if(id.matches("^C\\d+"))
				label = MetaboliteMajorLabel.LigandCompound;
			else if(id.matches("^D\\d+"))
				label = MetaboliteMajorLabel.LigandDrug;
			else if(id.matches("^G\\d+"))
				label = MetaboliteMajorLabel.LigandGlycan;
		}
		else
			label = defaultLabel;

		BiodbMetaboliteNode node = service.getMetabolite(id, label);

		if(label == MetaboliteMajorLabel.BiGGMetabolite && node == null) {

			node = service.getMetabolite(id, MetaboliteMajorLabel.BiGG);

			if(node == null)
				node = service.getMetabolite(id, MetaboliteMajorLabel.BiGG2);
		}

		Node newNode = null;
		MetaboliteMajorLabel newLabel = null;

		if(node != null && !label.equals(MetaboliteMajorLabel.BiGGMetabolite)) {

			for(Relationship rel : node.getRelationships(RelationshipType.withName("has_crossreference_to"))) {

				Node metaboliteNode = rel.getOtherNode(node);
				
				if(metaboliteNode.hasProperty("major_label") && metaboliteNode.hasProperty("entry")) {
					label = MetaboliteMajorLabel.valueOf(metaboliteNode.getProperty("major_label").toString());

					if(label.equals(MetaboliteMajorLabel.BiGGMetabolite)) {

						newNode = metaboliteNode;
						newLabel = MetaboliteMajorLabel.valueOf(metaboliteNode.getProperty("major_label").toString());
						break;
					}
					else if(label.equals(MetaboliteMajorLabel.BiGG)) {

						newNode = metaboliteNode;
						newLabel = MetaboliteMajorLabel.valueOf(metaboliteNode.getProperty("major_label").toString());
					}
					else if(label.equals(MetaboliteMajorLabel.BiGG2)) {

						newNode = metaboliteNode;
						newLabel = MetaboliteMajorLabel.valueOf(metaboliteNode.getProperty("major_label").toString());
					}
				}
			}
		}

		if(newNode != null)
			return service.getMetabolite(newNode.getProperty("entry").toString(), newLabel);

		return node;
	}
	

}
