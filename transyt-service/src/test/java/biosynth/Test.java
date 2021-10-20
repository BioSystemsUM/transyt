package biosynth;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.biojava.nbio.ontology.Synonym;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import pt.uminho.ceb.biosystems.merlin.bioapis.externalAPI.ebi.uniprot.UniProtAPI;
import pt.uminho.ceb.biosystems.transyt.service.biosynth.initializeNeo4jdb;
import pt.uminho.ceb.biosystems.transyt.service.containers.BiosynthMetabolites;
import pt.uminho.ceb.biosystems.transyt.service.kbase.Tools;
import pt.uminho.ceb.biosystems.transyt.service.reactions.FormulaParser;
import pt.uminho.ceb.biosystems.transyt.service.reactions.TransportReactionsBuilder;
import pt.uminho.ceb.biosystems.transyt.service.utilities.FileUtils;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.BiosynthMetaboliteProperties;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.ReactionContainer;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.TcNumberContainer2;
import pt.uminho.ceb.biosystems.transyt.utilities.dictionary.Synonyms;
import pt.uminho.ceb.biosystems.transyt.utilities.enumerators.CompartmentsSource;
import pt.uminho.ceb.biosystems.transyt.utilities.enumerators.Enumerators.TypeOfTransporter;
import pt.uminho.ceb.biosystems.transyt.utilities.files.FilesUtils;
import pt.uminho.ceb.biosystems.transyt.utilities.files.JSONFilesUtils;
import pt.uminho.ceb.biosystems.transyt.utilities.files.ReadExcelFile;
import pt.uminho.ceb.biosystems.transyt.utilities.files.WriteExcel;
import pt.uminho.ceb.biosystems.transyt.utilities.transytUtilities.Utilities;
import pt.uminho.sysbio.biosynth.integration.io.dao.neo4j.MetaboliteMajorLabel;
import pt.uminho.sysbio.biosynth.integration.neo4j.BiodbMetaboliteNode;
import pt.uminho.sysbio.biosynth.integration.neo4j.BiodbPropertyNode;
import pt.uminho.sysbio.biosynthframework.BiodbGraphDatabaseService;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;

class Test {

		 @org.junit.jupiter.api.Test
	void test() {
		
		String reactionID = "iiTRUni__cpd00009";	 
			 
		reactionID = reactionID.replaceAll("^ii", "i");
		
		System.out.println(reactionID);
		
//		System.out.println(isEquationBalanced("C16H25N5O16P2@ (out)  $<<>>$  C16H25N5O16P2@ (in)", ReactionContainer.REV_TOKEN, true));
//		
//		System.out.println("#######################");
//
//		System.out.println(isEquationBalanced("(C12H15NO10(R1)(R2)3)n@ (out)  $<<>>$  (C12H15NO10(R1)(R2)3)n@ (in)", ReactionContainer.REV_TOKEN, true));
////		System.out.println(isEquationBalanced("(O10(Ra)(Rb)3)n@ (out)  $<<>>$  (O10(Ra)(Rb)3)n@ (in)", ReactionContainer.REV_TOKEN, true));
//
//		System.out.println("done");
	}
	
	/**
	 * Method to verify if a reaction is balanced
	 * 
	 * @param equation
	 * @return
	 */
	public boolean isEquationBalanced(String equation, String regex, boolean print) {
		
		Map<String, String> element_map = FormulaParser.getSymbolMap();

		try {
			String[] reactant = getMolecules(equation.split(regex)[0]);
			String[] product = getMolecules(equation.split(regex)[1]);
			
			if(print) {
				System.out.println(Arrays.asList(reactant));
				System.out.println(Arrays.asList(product));
			}

			Map<String, Integer> reactantsCounts = countAtoms(reactant, print, element_map);
			Map<String, Integer> productsCounts = countAtoms(product, print, element_map);

			if(print) {
				System.out.println(reactantsCounts);
				System.out.println(productsCounts);
			}

			if(reactantsCounts.equals(productsCounts))
				return true;
		} 
		catch (Exception e) {
			System.out.println("reaction " + equation);
			//			e.printStackTrace();
		}

		return false;
	}
	
	/**
	 * @param text
	 * @return
	 */
	private static String[] getMolecules(String text) {

		String[] text2 = text.replaceAll("\\(in\\)", "").replaceAll("\\(out\\)", "").split(" \\+ ");

		for(int i = 0; i < text2.length; i++)				//the method to assess equations balance does not recognize white spaces
			text2[i] = text2[i].replaceAll("\\s+", ""); 

		return text2;
	}
	
	/**
	 * @param reactant
	 * @return
	 */
	private Map<String, Integer> countAtoms(String[] molecules, boolean print, Map<String, String> element_map) {

		Map<String, Integer> countsMap = new HashMap<>();
		
		for(String molecule : molecules) {
			
			if(molecule.contains("\\."))
				molecule = molecule.replaceAll("\\.", "").replaceAll("\\s+", "");
			
			for(String element : TransportReactionsBuilder.ELEMENTS_EXCEPTIONS) {

				if(molecule.contains(element.toUpperCase()))
					molecule = molecule.replace(element.toUpperCase(), element);
			}
			
			Integer stoichiometry = 1;

			Pattern p;
			Matcher m;

			if(molecule.matches("^(\\d+).+")) {

				p = Pattern.compile("^(\\d+)");
				m = p.matcher(molecule);

				if(m.find()) {

					try {
						stoichiometry = Integer.valueOf(m.group());
					} 
					catch (NumberFormatException e) {
						stoichiometry = 1;
					}
				}

				molecule = molecule.replaceAll("^(\\d+)", "");
			}
			
			Map<String, Integer> res;
			try {
				res = FormulaParser.parse(molecule, element_map);
		
			for(String key : res.keySet()) {

				if(countsMap.containsKey(key)) {
					countsMap.put(key, countsMap.get(key) + (res.get(key) * stoichiometry));
				}
				else {
					countsMap.put(key, (res.get(key) * stoichiometry));
				}
			}
			
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return countsMap;
	}


//	@org.junit.jupiter.api.Test
	void test2() {

		Map<String, String> sequences = new HashMap<>();
		try {

			BufferedReader br = new BufferedReader(new FileReader("C:\\Users\\Davide\\Desktop\\rosalind\\testFiles\\fernando\\sequence.txt"));

			String line = br.readLine();

			String locus = "";
			String sequence = "";
			boolean first = true;
			
			while (line != null) {
				
				System.out.println(line);
				
				if(line.contains(">")) {

					if(!first) {
						sequences.put(locus, sequence);
						sequence = "";
						locus = null;
					}
					else
						first = false;
					
					String[] header = line.split("\\s+");
					
					for(String h : header) {
						if(h.contains("locus_tag")) {
							locus = h.replace("locus_tag=", "").replace("[", "").replace("]", "");
							break;
						}
					}
					
				}
				else
					sequence = sequence.concat(line);
				
				line = br.readLine();

			}
			
			sequences.put(locus, sequence);
			
			br.close();
		}
		catch (Exception e) {

			e.printStackTrace();

		}
		
		try {

			PrintWriter writer = new PrintWriter("C:\\Users\\Davide\\Desktop\\rosalind\\testFiles\\fernando\\genome.faa", "UTF-8");

			for(String locus : sequences.keySet()) {
				writer.println(">" + locus);
				writer.println(sequences.get(locus));
			}
			writer.close();

		} 
		catch (FileNotFoundException e) {
			e.printStackTrace();

		} 
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		System.out.println("done");

	}

	// @org.junit.jupiter.api.Test
	void names() {

		Map<MetaboliteMajorLabel, String> map = new HashMap<>();
		map.put(MetaboliteMajorLabel.LigandCompound, "C00007");
		map.put(MetaboliteMajorLabel.ModelSeed, "cpd00007");
		map.put(MetaboliteMajorLabel.BiGG, "o2");

		Set<String> queries = new HashSet<>();
		queries.add("O2");

		Map<String, Map<MetaboliteMajorLabel, String>> compounds = new HashMap<>();
		compounds.put("Oxygen", map);
		compounds.put("O2", map);

		Set<String> names = new HashSet<>();
		names.add("O2");
		names.add("Oxygen");

		Map<String, String> formulas = new HashMap<>();
		formulas.put("O2", "O2");
		formulas.put("Oxigen", "O2");

		String formula = "O2";

		Map<String, Set<String>> alias = new HashMap<>();
		alias.put("O2", names);
		alias.put("Oxygen", names);

		MetaboliteMajorLabel label = MetaboliteMajorLabel.MetaCyc;

		String entryID = "ECOLI:OXYGEN";

		for (String name : names) {
			//
			// System.out.println();
			//
			// System.out.println(name);

			name = correctName(name, formula, formulas);

			// System.out.println(name);
			// System.out.println(alias.get(name));

			if (compounds.containsKey(name)) {

				for (String name2 : alias.get(name)) {

					if (compounds.containsKey(name2))
						compounds.get(name2).put(label, entryID);
				}
			} else {
				Map<MetaboliteMajorLabel, String> value = new HashMap<>();

				alias.put(name, names);

				formulas.put(name, formula); // for comparison between case sentitive situations

				value.put(label, entryID);
				compounds.put(name, value);
			}
		}

	}

	private static String correctName(String query, String queryFormula, Map<String, String> formulas) {

		for (String name : formulas.keySet()) {

			// System.out.println("1.0 >>> " + name + "\t" + formulas.get(name) + "\t" +
			// queryFormula);

			if (name.equalsIgnoreCase(query)) {

				if (queryFormula.equalsIgnoreCase(formulas.get(name)))
					return name;
			}

		}

		return query;
	}

	// @org.junit.jupiter.api.Test
	void tests() {

		List<String> entries = new ArrayList<>();

		entries.add("P53048");
		entries.add("Q8NTX0");

		List<UniProtEntry> uniprotList = UniProtAPI.getEntriesFromUniProtIDs(entries, 0);

		for (UniProtEntry u : uniprotList)
			System.out.println(u.getTaxonomy().get(0));

	}

	//	@org.junit.jupiter.api.Test
	void tests2() {

		System.out.println("TR_".concat(String.format("%06d", 1)));

		// String alphabet = "C + A + B REV R";
		//
		// String[] splitWords = alphabet.split(" REV ");
		//
		// String[] compounds = splitWords[0].split(" \\+ ");
		//
		// Arrays.sort(compounds);
		//
		// String[] compounds2 = splitWords[1].split(" \\+ ");
		//
		// Arrays.sort(compounds2);
		//
		// String sortedString = "";
		//
		// for(int i = 0; i < compounds2.length; i++) {
		//
		// if(i == compounds2.length - 1)
		// sortedString = sortedString.concat(compounds2[i]).concat(" ");
		// else
		// sortedString = sortedString.concat(compounds2[i]).concat(" + ");
		// }
		//
		// System.out.println(sortedString);

		// System.out.println(TransportReactionsBuilder.executeDistributions(all, main,
		// 0));

		// Synonyms dic = new Synonyms();

		// System.out.println(dic.getMetabolitesDictionary().get("Mn2+"));

		// System.out.println(dic.getSynonym("Mn+".toLowerCase()));

		// String regex = "^(\\s*n\\s*)";
		//
		// System.out.println(metabolite.charAt(metabolite.length()-1));
		//
		// String met = metabolite.replaceAll(regex, "");
		//
		// System.out.println(met);

		// MetaboliteMajorLabel label = MetaboliteMajorLabel.MetaCyc;
		//
		// Set<MetaboliteMajorLabel> set = new HashSet<>();
		//
		// set.add(MetaboliteMajorLabel.MetaCyc);
		//
		// System.out.println(set.contains(label));

		// ReactionsMetabolites.getMetabolitesFromReactions(JSONfiles.readJSONtcdbReactionsFile());

		// String met = "aksbjsa " + ReactionContainer.REVERSIBLE_TOKEN + " aklusd";
		//
		// met = met.replaceAll(ReactionContainer.REV_TOKEN, "\\+");
		//
		// String[] mets = met.split(" \\+ ");
		//
		// System.out.println(mets.length);
		//

	}

	// @org.junit.jupiter.api.Test
	void readOrthoMCLResults() throws IOException {

		try {
			BufferedReader reader = new BufferedReader(new FileReader(
					new File("C:\\Users\\Davide\\Downloads\\orthomclResult-N7P68S89WH43\\orthologGroups")));

			String html = "";

			Map<String, Integer> map = new TreeMap<>();

			while ((html = reader.readLine()) != null) {

				String[] coiso = html.split("\t");

				if (map.containsKey(coiso[1])) {

					int count = map.get(coiso[1]) + 1;

					map.put(coiso[1], count);

				} else
					map.put(coiso[1], 1);
			}

			BufferedWriter writer;

			try {

				writer = new BufferedWriter(new FileWriter(
						"C:\\\\Users\\\\Davide\\\\Downloads\\\\orthomclResult-N7P68S89WH43\\\\contagens.txt", true));

				for (String key : map.keySet()) {
					writer.append(key + "\t" + map.get(key));
					writer.newLine();
				}

				writer.flush();
				writer.close();

			} catch (IOException e) {
				e.printStackTrace();
			}

			reader.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	//		@org.junit.jupiter.api.Test
	void compoundsFromSBML() {

		//		Tools.readModelMetabolitesFromSBML("C:\\Users\\Davide\\OneDrive - Universidade do Minho\\UMinho\\Tese\\KBase\\Genomes\\Escherichia coli str. K-12 substr. MG1655 - NCBI\\iAF1260_seed_iddentifiers.xml");

		Tools.readModelMetabolitesFromSBML("C:\\Users\\Davide\\OneDrive - Universidade do Minho\\UMinho\\Tese\\KBase\\Genomes\\Escherichia coli str. K-12 substr. MG1655 - NCBI\\iAF1260_constraints.xml");

	}


	//@org.junit.jupiter.api.Test
	void text() {

		String r = "cpd26818 (out) + cpd00971 (out) $REV$ cpd26818 (in) + cpd00971 (in)";


		r = r.replace(" (in)", "").replace(" (out)", "").replace("$REV$", "+");

		System.out.println(r);

		Utilities.printArray(r.split("\\s+\\+\\s+"));

	}

	//	@org.junit.jupiter.api.Test
	void mappingKeggIDToModelSEED() throws IOException {

		Map<String, Map<MetaboliteMajorLabel, String>> compounds = FileUtils.readMapFromFile2("C:\\Users\\Davide\\Documents\\InternalDB\\info\\getMetabolitesIDs3.txt");

		Map<String, Set<String>> res = new HashMap<>();

		for(String key : compounds.keySet()) {

			Map<MetaboliteMajorLabel, String> identifiers = compounds.get(key);

			if(identifiers.containsKey(MetaboliteMajorLabel.LigandCompound) && identifiers.containsKey(MetaboliteMajorLabel.ModelSeed)) {

				String keggID = identifiers.get(MetaboliteMajorLabel.LigandCompound);
				String modelSEED = identifiers.get(MetaboliteMajorLabel.ModelSeed);

				if(res.containsKey(keggID)) {

					res.get(keggID).add(modelSEED);
				}
				else {
					Set<String> set = new HashSet<>();
					set.add(modelSEED);

					res.put(keggID, set);
				}
			}
			else if(identifiers.containsKey(MetaboliteMajorLabel.LigandGlycan) && identifiers.containsKey(MetaboliteMajorLabel.ModelSeed)) {

				String keggID = identifiers.get(MetaboliteMajorLabel.LigandGlycan);
				String modelSEED = identifiers.get(MetaboliteMajorLabel.ModelSeed);

				if(res.containsKey(keggID)) {

					res.get(keggID).add(modelSEED);
				}
				else {
					Set<String> set = new HashSet<>();
					set.add(modelSEED);

					res.put(keggID, set);
				}
			}
			else if(identifiers.containsKey(MetaboliteMajorLabel.LigandDrug) && identifiers.containsKey(MetaboliteMajorLabel.ModelSeed)) {

				String keggID = identifiers.get(MetaboliteMajorLabel.LigandDrug);
				String modelSEED = identifiers.get(MetaboliteMajorLabel.ModelSeed);

				if(res.containsKey(keggID)) {

					res.get(keggID).add(modelSEED);
				}
				else {
					Set<String> set = new HashSet<>();
					set.add(modelSEED);

					res.put(keggID, set);
				}
			}
		}

		FilesUtils.saveMapInFile3("C:\\Users\\Davide\\Desktop\\KEGG_To_ModelSEED.txt", res);
	}

	//	@org.junit.jupiter.api.Test
	void readMap() {

		//			Map<String, Map<MetaboliteMajorLabel, String>> compounds = FileUtils.readMapFromFile2("C:\\Users\\Davide\\Documents\\InternalDB\\info\\getMetabolitesIDs4.txt");
		//			
		//			System.out.println("test >>> " + compounds.get("Glucose"));

		//			Synonyms dictionary = new Synonyms();
		//			
		//			String word = dictionary.getSynonym("Glucose".replaceAll("\\s+", "").toLowerCase());
		//			
		//			System.out.println(word);
		//		Map<MetaboliteMajorLabel, Map<String, Set<String>>> data = ModelSEED.readSEEDRelationshipsFile();
		//
		//
		//		for(MetaboliteMajorLabel label : data.keySet()) {
		//
		//			for(String modelSEEDid : data.get(label).keySet()) {
		//
		//				System.out.println(label + "\t" + modelSEEDid + "\t" + data.get(label).get(modelSEEDid));
		//			}
		//		}
	}

	//		@org.junit.jupiter.api.Test
	void missingMetabolitesMerge() {

		try {

			int excelVersion = 13; //////////VERSION EXCEL

			String path = "C:\\Users\\Davide\\OneDrive - Universidade do Minho\\UMinho\\Tese\\KBase\\Reports\\ecoli_Validation30\\";

			Set<String> mergeMetabolites = new HashSet<>();

			Set<String> file1 = FilesUtils.readWordsInFile(path + "missingMetabolites_e_p.txt");

			Set<String> file2 = FilesUtils.readWordsInFile(path + "missingMetabolites_p_c.txt");

			for(String m : file1)
				mergeMetabolites.add(m.substring(0, m.length()-2));

			for(String m : file2)
				mergeMetabolites.add(m.substring(0, m.length()-2));

			FilesUtils.saveWordsInFile(path + "mergeMissingMetabolites.txt", mergeMetabolites);


			Set<String> mergeMetabolitesNames = new HashSet<>();

			Set<String> file11 = FilesUtils.readWordsInFile(path + "missingMetabolitesNames_e_p.txt");

			Set<String> file22 = FilesUtils.readWordsInFile(path + "missingMetabolitesNames_p_c.txt");

			mergeMetabolitesNames.addAll(file11);
			mergeMetabolitesNames.addAll(file22);

			FilesUtils.saveWordsInFile(path + "mergeMissingMetabolitesNames.txt", mergeMetabolitesNames);

			Set<String> mergeReactions = new HashSet<>();

			Set<String> file3 = FilesUtils.readWordsInFile(path + "duplicates_e_p.txt");

			Set<String> file4 = FilesUtils.readWordsInFile(path + "duplicates_p_c.txt");

			for(String r : file3)
				mergeReactions.add(r.substring(0, r.length()-2));

			for(String r : file4)
				mergeReactions.add(r.substring(0, r.length()-2));

			FilesUtils.saveWordsInFile(path + "mergeDuplicates.txt", mergeReactions);

			Map<String, String> reactions1 = FilesUtils.readMapFromFileDELETEME(path + "missingReactions_e_p.txt");
			Map<String, String> reactions2 = FilesUtils.readMapFromFileDELETEME(path + "missingReactions_p_c.txt");

			Map<String, String> reactions3 = FilesUtils.readMapFromFileDELETEME(path + "missingReactionsWithNames_e_p.txt");
			Map<String, String> reactions4 = FilesUtils.readMapFromFileDELETEME(path + "missingReactionsWithNames_p_c.txt");

			List<String[]> data = new ArrayList<>();

			String[] line = new String[7];

			line[0] = "ID";
			line[1] = "equation";
			line[2] = "equation with names";
			line[3] = "gene rule";
			line[4] = "";
			line[5] = "status";
			line[6] = "status description";


			Map<String, String> sbmlGenes = FilesUtils.readMapFromFile(path + "sbmlGeneRules.txt");

			data.add(line);

			for(String r : reactions1.keySet()) {

				line = new String[7];

				if(reactions2.containsKey(r)) {

					line[0] = r;
					line[1] = reactions2.get(r);
					line[2] = reactions3.get(r);
					line[3]	= sbmlGenes.get(r);

					data.add(line);
				}

			}

			int previousExcelVersion = excelVersion - 1;

			WriteExcel.validationTableToExcel(data, path + "NotAvailableReactionsSBML_V" + excelVersion + ".xlsx", ReadExcelFile.readStatusValidation(path + "NotAvailableReactionsSBML_V" + previousExcelVersion + ".xlsx"));


			Map<String, String> reactions5 = FilesUtils.readMapFromFile(path + "reactionsGenerated.txt");
			Map<String, String> reactions6 = FilesUtils.readMapFromFile(path + "reactionsGeneratedWithNames.txt");

			Map<String, String> genes = FilesUtils.readMapFromFile(path + "geneRules.txt");

			data = new ArrayList<>();

			line = new String[4];

			line[0] = "ID";
			line[1] = "equation";
			line[2] = "equation with names";
			line[3] = "gene rule";

			data.add(line);

			for(String r : reactions5.keySet()) {

				line = new String[4];

				line[0] = r;
				line[1] = reactions5.get(r);
				line[2] = reactions6.get(r);
				line[3] = genes.get(r);

				data.add(line);

			}

			WriteExcel.tableToExcel(data, path + "AllTRIAGEData_V" + excelVersion + ".xlsx");

		}
		catch(Exception e) {

			e.printStackTrace();
		}


	}

	//		@org.junit.jupiter.api.Test
	void findMetabolitesFormula() {

		Map<String, String> mapping = new HashMap<>();
		try {

			GraphDatabaseService graphDatabaseService = initializeNeo4jdb.getDataDatabase(null);
			Transaction dataTx = graphDatabaseService.beginTx();

			BiodbGraphDatabaseService service = new BiodbGraphDatabaseService(graphDatabaseService);

			Set<String> modelMetabolites = Tools.readModelMetabolites("C:\\Users\\Davide\\OneDrive - Universidade do Minho\\UMinho\\Tese\\KBase\\Genomes\\Escherichia coli str. K-12 substr. MG1655 - NCBI\\ModelCompounds.xlsx");

			for(String metabolite : modelMetabolites) {

				BiodbMetaboliteNode node = service.getMetabolite(metabolite, MetaboliteMajorLabel.ModelSeed);

				if(node != null && node.hasProperty("formula")) {

					String formula = node.getProperty("formula").toString();

					mapping.put(metabolite, formula);
				}

			}

			dataTx.failure();
			dataTx.close();
			service.shutdown();
			graphDatabaseService.shutdown();
			System.out.println("Shutdown!!!");
		} 
		catch (Exception e) {
			e.printStackTrace();
		}

		FilesUtils.saveMapInFile("C:\\Users\\Davide\\OneDrive - Universidade do Minho\\UMinho\\Tese\\KBase\\Reports\\ecoli_Validation30\\metabolitesFormulas.txt", mapping);

	}
}
