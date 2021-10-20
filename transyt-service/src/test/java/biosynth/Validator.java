package biosynth;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

//import javax.xml.parsers.ParserConfigurationException;
//import javax.xml.stream.XMLStreamException;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.biojava.nbio.core.sequence.ProteinSequence;
import org.biojava.nbio.core.sequence.io.FastaReaderHelper;
import org.junit.jupiter.api.Test;

import pt.uminho.ceb.biosystems.merlin.bioapis.externalAPI.ebi.uniprot.UniProtAPI;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.Container;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.ReactionCI;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.ReactionConstraintCI;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.ReactionTypeEnum;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.io.readers.JSBMLLevel3Reader;
import pt.uminho.ceb.biosystems.transyt.service.utilities.MappingMetabolites;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.ReactionContainer;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.TcNumberContainer;
import pt.uminho.ceb.biosystems.transyt.utilities.enumerators.Enumerators.TypeOfTransporter;
import pt.uminho.ceb.biosystems.transyt.utilities.files.FilesUtils;
import pt.uminho.ceb.biosystems.transyt.utilities.files.JSONFilesUtils;
import pt.uminho.ceb.biosystems.transyt.utilities.files.ReadExcelFile;
import pt.uminho.ceb.biosystems.transyt.utilities.transytUtilities.Utilities;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;

class Validator {

	//	@Test
	void test() {

		try {

			Map<String, String> map2 = MappingMetabolites.metaCycToModelSEEDids();

			if(map2.containsKey("META:CPD-9956"))
				System.out.println(map2.get("META:CPD-9956"));

			System.exit(0);

			Map<String, String> toUpdate = new HashMap<>();
			List<String> idsToSearch = new ArrayList<>();

			LinkedHashMap<String, ProteinSequence> map = FastaReaderHelper.readFastaProteinSequence(
					new File("C:\\Users\\Davide\\OneDrive - Universidade do Minho\\UMinho\\Tese\\KBase\\Genomes\\Escherichia coli str. K-12 substr. MG1655 - NCBI\\GCF_000005845.2_ASM584v2_protein.faa"));

			for(String key : map.keySet()) {
				idsToSearch.add(map.get(key).getAccession().getID().split("\\s+")[0]);		
			}

			if(!idsToSearch.isEmpty() && idsToSearch!=null){

				AtomicBoolean cancel = new AtomicBoolean(false);

				Map<String,List<UniProtEntry>> uniProtEntries = UniProtAPI.getUniprotEntriesFromRefSeq(idsToSearch, cancel, 0);

				for(String seqID : uniProtEntries.keySet()){

					if(uniProtEntries.get(seqID)!=null && !uniProtEntries.get(seqID).isEmpty()){

						if(uniProtEntries.get(seqID).size()==1){

							UniProtEntry entry = uniProtEntries.get(seqID).get(0);

							toUpdate.put(seqID, UniProtAPI.getLocusTag(entry));
							idsToSearch.remove(seqID);
						}
					}
				}
			}

			FilesUtils.saveMapInFile("C:\\Users\\Davide\\OneDrive - Universidade do Minho\\UMinho\\Tese\\KBase\\Reports\\ecoli_Validation90\\Acc_to_locus.txt", toUpdate);

		} 
		catch (Exception e) {
			e.printStackTrace();
		}

	}

	//		@Test
	public void findReactionsInSBML() throws Exception {

		JSBMLLevel3Reader reader = new JSBMLLevel3Reader("C:\\Users\\Davide\\Downloads\\GCF_000005845.2\\simGCF_000005845.2.new_template.xml", "ecoli");
		//		JSBMLLevel3Reader reader = new JSBMLLevel3Reader("C:\\Users\\Davide\\OneDrive - Universidade do Minho\\UMinho\\Tese\\KBase\\Reports\\ecoli_Validation30\\SBML\\sbmlKBaseNew.xml", "ecoli");

		Container containerSBML = new Container(reader);
		containerSBML.verifyDepBetweenClass();

		Map<String, ReactionConstraintCI> defaultEC = containerSBML.getDefaultEC();

		Set<String> transporters = containerSBML.getReactionsByType(ReactionTypeEnum.Transport);

		//		for(String key : transporters)
		//			System.out.println(key);

		System.out.println("TRANSPORTADORES: " + transporters.size());


		Map<String, ReactionCI> sbmlReactions = containerSBML.getReactions();

		for(String react : sbmlReactions.keySet()) {

			if(transporters.contains(react) && !react.equals("R_BIOMASS_Ec_iAF1260_core_59p81M")) {

				String reaction = sbmlReactions.get(react).toStringStoiquiometry();

				String reversibility = ReactionContainer.REV_TOKEN;

				if(defaultEC.get(react).getLowerLimit() == 0.0 || defaultEC.get(react).getUpperLimit() == 0.0)
					reversibility = ReactionContainer.IRREV_TOKEN;


				reaction = reaction.replaceAll("<->", reversibility).replaceAll("M_", "");

				if(reaction.contains("_p") && reaction.contains("_c"))
					reaction = reaction.replaceAll("_p", " (out)").replaceAll("_c", " (in)");
				else if(reaction.contains("_e") && reaction.contains("_p"))
					reaction = reaction.replaceAll("_e", " (out)").replaceAll("_p", " (in)");

				reaction = reaction.replaceAll("\\.0\\s*\\*\\s", "").replaceAll("\\$\\s*\\+", "\\$ ").replaceAll("\\+", "\\+ ").replaceAll("^\\s*\\+\\s*", "");

				reaction.replaceAll("\\s+", "\\s").replaceAll("\\t", " ");

				String[] reactionIDsSplit = reaction.split(reversibility);

				String reactantIDs = sortReactantsAndProducts(reactionIDsSplit[0]);

				String id = generateTranSyTID(reactantIDs);

				if(reversibility.equals(ReactionContainer.IRREV_TOKEN))

					id = "i".concat(id);

				System.out.println(react + "\t" + id);
				//				System.out.println();
			}

		}
	}

	public static String sortReactantsAndProducts(String reaction) {

		String[] reactants = reaction.split(" \\+ ");

		//		Utilities.printArray(reactants);

		String sortedString = "";

		String water = "";

		for(int i = 0; i < reactants.length; i++) {

			if(reactants[i].matches("^1cpd.*"))
				reactants[i] = reactants[i].replaceAll("1cpd", "cpd");

			if(reaction.contains("cpd00002") && reactants[i].contains("cpd00001")) {
				water = reactants[i];
			}			
			else if(i == reactants.length - 1)
				sortedString = sortedString.concat(reactants[i]);
			else 
				sortedString = sortedString.concat(reactants[i]).concat(" + ");
		}

		if(!water.isEmpty()) {
			sortedString = sortedString.concat(" + " + water.substring(0, water.length()-4));
		}

		reactants = sortedString.split(" \\+ ");

		//		Utilities.printArray(reactants);

		sortedString = "";

		Arrays.sort(reactants);

		for(int i = 0; i < reactants.length; i++) {

			sortedString = sortedString.concat(reactants[i]).concat(" + ");

		}

		//		System.out.println(sortedString);

		sortedString = sortedString.replaceAll("\\s+\\+\\s*$", "").replaceAll("\\s+", " ");

		//		System.out.println(sortedString);

		return sortedString;
	}

	/**
	 * @return
	 */
	public String generateTranSyTID(String reactant) {

		//		Map<MetaboliteMajorLabel, String> labelsProton = allMetabolites.get("H+");
		//		Map<MetaboliteMajorLabel, String> labelWater = allMetabolites.get("WATER");

		String id = "TR";

		String[] aux = reactant.split("\\+");

		//		Utilities.printArray(aux);

		reactant = "";

		boolean in = false;
		boolean out = false;

		for(int i = 0; i < aux.length; i++) {
			String comp = "";

			String[] compoundAux = aux[i].split("=");

			if(aux[i].contains("(in)")) {
				comp = "i";
				in = true;
			}
			else if(aux[i].contains("(out)")) {
				comp = "o";
				out = true;
			}
			//			MetaboliteMajorLabel label = MetaboliteMajorLabel.valueOf(compoundAux[1]);
			//			
			//			if(transportType.equals(TypeOfTransporter.BiochemicalATP) && 
			//					(compoundAux[0].equals(labelsProton.get(label)) || compoundAux[0].equals(labelWater.get(label))))
			//				continue;
			//			else

			//			System.out.println(reactant);

			reactant = reactant.concat(compoundAux[0]).replaceAll("\\(in\\)", comp).replaceAll("\\(out\\)", comp).concat("_");

			//			System.out.println(reactant);
		}

		//		Utilities.printArray(aux);

		reactant = reactant.replaceAll("\\s+", "").replaceAll("_$", "");

		TypeOfTransporter transportType = null;

		if(reactant.contains("cpd00002") && reactant.contains("cpd00010")) {
			transportType = TypeOfTransporter.BiochemicalCoA;
		}
		else if(reactant.contains("cpd00002") || reactant.contains("cpd00008") || reactant.contains("cpd00038") || reactant.contains("cpd00031")) {
			transportType = TypeOfTransporter.BiochemicalATP;
			reactant = reactant.replaceAll("cpd00002i", "cpd00002");
		}
		else if(reactant.contains("cpd00003") || reactant.contains("cpd00004"))
			transportType = TypeOfTransporter.RedoxNADH;
		else if(reactant.contains("cpd00061"))
			transportType = TypeOfTransporter.PEPdependent;
		else if(aux.length > 1 && in && out)
			transportType = TypeOfTransporter.Antiport;
		else if(aux.length > 1)
			transportType = TypeOfTransporter.Symport;
		else if(aux.length == 1)
			transportType = TypeOfTransporter.Uniport;
		else
			transportType = TypeOfTransporter.Redox;

		id = id.concat(TypeOfTransporter.getTransportTypeAbb(transportType)).concat("__").concat(reactant).replaceAll("_$", "");

		return id;
	}

	//	@Test
	public void countEquations() {

		Map<String, TcNumberContainer> file = JSONFilesUtils.readDataBackupFile();

		int counter = 0;

		Set<String> differentEq = new HashSet<>();

		for(String tc : file.keySet()) {

			counter = counter + file.get(tc).getAllReactionsIds().size();

			for(int id : file.get(tc).getAllReactionsIds()) {

				ReactionContainer reaction = file.get(tc).getReactionContainer(id);

				differentEq.add(reaction.getReaction());

			}
		}

		System.out.println("TOTAL EQUATIONS: " + counter);
		System.out.println("DIFFERENT EQUATIONS: " + differentEq.size());

	}

	//		@Test
	public void countmetabolitesReactionsGenerated() {

		List<String[]> table = ReadExcelFile.getData("C:\\Users\\Davide\\OneDrive - Universidade do Minho\\UMinho\\Tese\\KBase\\Reports\\ecoli_Validation30\\AllTRIAGEData_V13.xlsx", true, null);

		Set<String> metabolite = new HashSet<>();
		Set<String> genes = new HashSet<>();

		for(String[] line : table) {

			String reaction = line[1];
			String geneRule = line[3].replaceAll("[\\[\\]]", "");

			genes.addAll(Utilities.convertStringToSetString(geneRule, ", "));

			String rev = ReactionContainer.REV_TOKEN;

			if(reaction.contains("$IRREV$"))
				rev = ReactionContainer.IRREV_TOKEN;

			reaction = reaction.replaceAll(rev, " + ").replaceAll("\\(in\\)", "").replaceAll("\\(out\\)", "");

			String[] compounds = reaction.split(" \\+ ");

			for(String c : compounds) {

				metabolite.add(c.replaceAll("\\d+\\s*cpd", "cpd").trim().replaceAll("ncpd", "cpd").replaceAll("-->", "").trim());

			}
		}

		List<String[]> table2 = ReadExcelFile.getData("C:\\Users\\Davide\\OneDrive - Universidade do Minho\\UMinho\\Tese\\KBase\\Reports\\ecoli_Validation30\\ValidationByReaction.xlsx", true, null);

		Set<String> metabolite2 = new HashSet<>();
		Set<String> genes2 = new HashSet<>();


		for(String[] line : table2) {

			String reaction = line[1].replaceAll("M_", "");

			if(line[3] != null) {
				String geneRule = line[3].replaceAll("[\\(\\)]", "").replaceAll("and", ",").replaceAll("or", ",").replaceAll("\\s+", "");

				genes2.addAll(Utilities.convertStringToSetString(geneRule, ","));
			}

			reaction = reaction.replaceAll("<->", " + ").replaceAll("_p", "").replaceAll("_c", "").replaceAll("_e", "").replaceAll("\\d+\\.\\d+\\s*\\*", "");

			String[] compounds = reaction.split(" \\+ ");

			for(String c : compounds) {

				c = c.trim();

				if(!c.isEmpty()) {

					metabolite2.add(c.replaceAll("\\d+cpd", "cpd").replaceAll("ncpd", "cpd").replaceAll("-->", "").trim());
				}
			}
		}

		System.out.println(genes2.size());

		genes2.removeAll(genes);

		System.out.println(genes2.size());
	}

	//		@Test
	public void countmetabolitesReactionsModel() {

		List<String[]> table = ReadExcelFile.getData("C:\\Users\\Davide\\OneDrive - Universidade do Minho\\UMinho\\Tese\\KBase\\Reports\\ecoli_Validation30\\ValidationByReaction.xlsx", true, null);

		Set<String> metabolite = new HashSet<>();

		for(String[] line : table) {

			String reaction = line[1].replaceAll("M_", "");

			reaction = reaction.replaceAll("<->", " + ").replaceAll("_p", "").replaceAll("_c", "").replaceAll("_e", "").replaceAll("\\d+\\.\\d+\\s*\\*", "");

			String[] compounds = reaction.split(" \\+ ");

			for(String c : compounds) {

				c = c.trim();

				if(!c.isEmpty()) {

					metabolite.add(c.replaceAll("\\d+cpd", "cpd").replaceAll("ncpd", "cpd").replaceAll("-->", "").trim());
				}
			}
		}

		System.out.println(metabolite.size());
	}

	//	@Test
	//	public void annotateByGene() throws FileNotFoundException, XMLStreamException, ErrorsException, IOException, ParserConfigurationException, SAXException, JSBMLValidationException {
	//
	//		JSBMLReader reader = new JSBMLReader("C:\\Users\\Davide\\OneDrive - Universidade do Minho\\UMinho\\Tese\\KBase\\Genomes\\Escherichia coli str. K-12 substr. MG1655 - NCBI\\iAF1260_constraints.xml", "ecoli");
	//
	//		Container containerSBML = new Container(reader);
	//		containerSBML.verifyDepBetweenClass();
	//
	//		Map<String, ReactionConstraintCI> defaultEC = containerSBML.getDefaultEC();
	//
	//		Set<String> transporters = containerSBML.getReactionsByType(ReactionTypeEnum.Transport);
	//
	//		System.out.println("TRANSPORTADORES: " + transporters.size());
	//
	//		Map<String, ReactionCI> sbmlReactions = containerSBML.getReactions();
	//
	//		Map<String, Set<String>> reactionsByGeneTriage = new HashMap<>();
	//
	//		Map<String, Set<String>> reactionsByGeneModel = new HashMap<>();
	//
	//		for(String react : sbmlReactions.keySet()) {
	//
	//			if(transporters.contains(react) && !react.equals("R_BIOMASS_Ec_iAF1260_core_59p81M")) {
	//
	//				String[] genes = sbmlReactions.get(react).getGeneRuleString().replaceAll("[\\(\\)]", "").replaceAll("and", "@").replaceAll("or", "@").split("@");
	//
	//				for(String g : genes) {
	//
	//					g = g.trim();
	//
	//					Set<String> reactions = new HashSet<>();
	//
	//					if(reactionsByGeneModel.containsKey(g))
	//						reactions = reactionsByGeneModel.get(g);
	//
	//					reactions.add(react);
	//
	//					reactionsByGeneModel.put(g, reactions);
	//				}
	//			}
	//		}
	//
	//		List<String[]> table = ReadExcelFile.getData("C:\\Users\\Davide\\OneDrive - Universidade do Minho\\UMinho\\Tese\\KBase\\Reports\\ecoli_Validation30\\AllTRIAGEData_V13.xlsx");
	//
	//		Map<String, String> mappingReactions = new HashMap<>();
	//		
	//		for(String[] line : table) {
	//
	//			String id = line[0];
	//			
	//			mappingReactions.put(id, line[1]);
	//			
	//			if(!line[2].equals("Genes")) {
	//
	//				String[] genes = line[3].replaceAll("[\\[\\]]", "").split(", ");
	//				
	//				for(String g : genes) {
	//
	//					g = g.trim();
	//
	//					Set<String> reactions = new HashSet<>();
	//
	//					if(reactionsByGeneTriage.containsKey(g))
	//						reactions = reactionsByGeneTriage.get(g);
	//
	//					reactions.add(id);
	//					
	//					reactionsByGeneTriage.put(g, reactions);
	//
	//				}
	//				
	//			}
	//		}
	//		writeExcel(reactionsByGeneModel, reactionsByGeneTriage, sbmlReactions, mappingReactions);
	//	}



	public void writeExcel(Map<String, Set<String>> reactionsByGeneModel, Map<String, Set<String>> reactionsByGeneTriage, Map<String, ReactionCI> sbmlReactions, Map<String, String> mappingReactions) {

		String path = "C:\\Users\\Davide\\OneDrive - Universidade do Minho\\UMinho\\Tese\\KBase\\Reports\\ecoli_Validation30\\validationByGene.xlsx";

		try {

			String sheetName = "Sheet1";//name of sheet

			Workbook workbook = new XSSFWorkbook();
			Sheet sheet  = workbook.createSheet(sheetName) ;

			int i = 1;

			for(String gene : reactionsByGeneModel.keySet()) {

				Set<String> reactions = reactionsByGeneModel.get(gene);

				Row row = sheet.createRow(i);

				int headerLine = i;

				row.createCell(0).setCellValue(gene);

				i++;

				int firstLine = i;
				int lastLine = i;

				for(String r : reactions) {

					row = sheet.createRow(i);

					row.createCell(1).setCellValue(r);
					row.createCell(2).setCellValue(sbmlReactions.get(r).toStringStoiquiometry());

					i++;

					lastLine = i;
				}

				if(reactionsByGeneTriage.containsKey(gene)) {

					reactions = reactionsByGeneTriage.get(gene);

					i = firstLine;

					for(String r : reactions) {

						row = sheet.getRow(i);

						if(row == null)
							row = sheet.createRow(i);

						row.createCell(4).setCellValue(r);
						row.createCell(5).setCellValue(mappingReactions.get(r));

						i++;

					}

					if(i < lastLine)
						i = lastLine;
				}

				row = sheet.getRow(headerLine);

				if(row == null)
					row = sheet.createRow(headerLine);

				Set<String> modelMetabolites = getModelMetabolitesFromReactions(reactionsByGeneModel.get(gene), sbmlReactions);


				Set<String> triageMetabolites = new HashSet<>();

				if(reactionsByGeneTriage.containsKey(gene))
					triageMetabolites = getTriageMetabolitesFromReactions(reactionsByGeneTriage.get(gene), mappingReactions);

				row.createCell(6).setCellValue("model metabolites: ");
				row.createCell(7).setCellValue(modelMetabolites.size());

				row.createCell(8).setCellValue("transyt metabolites: ");
				row.createCell(9).setCellValue(triageMetabolites.size());

				row.createCell(10).setCellValue("model contains all metabolites from transyt's reactions: ");
				row.createCell(11).setCellValue(modelMetabolites.containsAll(triageMetabolites));



				Set<String> notMatchModel = new HashSet<>();
				Set<String> notMatchTriage = new HashSet<>();

				if(!modelMetabolites.containsAll(triageMetabolites)) {

					for(String m : triageMetabolites) {
						if(!modelMetabolites.contains(m))
							notMatchModel.add(m);
					}
				}

				if(notMatchModel.size() > 0)
					row.createCell(12).setCellValue(notMatchModel.toString());

				row.createCell(13).setCellValue("transyt contains all metabolites model reactions: ");
				row.createCell(14).setCellValue(triageMetabolites.containsAll(modelMetabolites));

				if(!triageMetabolites.containsAll(modelMetabolites)) {

					for(String m : modelMetabolites) {
						if(!triageMetabolites.contains(m))
							notMatchTriage.add(m);
					}
				}

				if(notMatchTriage.size() > 0)
					row.createCell(15).setCellValue(notMatchTriage.toString());

				i++;

			}

			FileOutputStream fileOut = new FileOutputStream(path);

			workbook.write(fileOut);
			fileOut.flush();
			fileOut.close();

			workbook.close();

			System.out.println("xlsx table created");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Set<String> getTriageMetabolitesFromReactions(Set<String> reactions, Map<String, String> mappingReactions) {

		Set<String> metabolites = new HashSet<>();

		for(String r : reactions) {

			String reaction = mappingReactions.get(r);

			String rev = ReactionContainer.REV_TOKEN;

			if(reaction.contains("$IRREV$"))
				rev = ReactionContainer.IRREV_TOKEN;

			reaction = reaction.replaceAll(rev, " + ").replaceAll("\\(in\\)", "").replaceAll("\\(out\\)", "");

			String[] compounds = reaction.split(" \\+ ");

			for(String c : compounds) {

				metabolites.add(c.replaceAll("\\d+cpd", "cpd").trim());

			}
		}

		metabolites.remove("cpd00067");
		metabolites.remove("cpd00971");

		return metabolites;
	}

	public Set<String> getModelMetabolitesFromReactions(Set<String> reactions, Map<String, ReactionCI> sbmlReactions) {

		Set<String> metabolites = new HashSet<>();

		for(String r : reactions) {

			String reaction = sbmlReactions.get(r).toStringStoiquiometry().replaceAll("M_", "");

			reaction = reaction.replaceAll("<->", " + ").replaceAll("_p", "").replaceAll("_c", "").replaceAll("_e", "").replaceAll("\\d+\\.\\d+\\s*\\*", "");

			String[] compounds = reaction.split(" \\+ ");

			for(String c : compounds) {

				c = c.trim();

				if(!c.isEmpty()) {

					metabolites.add(c.replaceAll("\\d+cpd", "cpd").trim());
				}
			}
		}

		metabolites.remove("cpd00067");
		metabolites.remove("cpd00971");

		return metabolites;
	}


	//	@Test
	//	public void findDuplicatedReactions() throws FileNotFoundException, XMLStreamException, ErrorsException, IOException, ParserConfigurationException, SAXException, JSBMLValidationException {
	//		
	//		JSBMLReader reader = new JSBMLReader("C:\\Users\\Davide\\OneDrive - Universidade do Minho\\UMinho\\Tese\\KBase\\Genomes\\Escherichia coli str. K-12 substr. MG1655 - NCBI\\iAF1260_constraints.xml", "ecoli");
	//
	//		Container containerSBML = new Container(reader);
	//		containerSBML.verifyDepBetweenClass();
	//
	//		Map<String, ReactionConstraintCI> defaultEC = containerSBML.getDefaultEC();
	//
	//		Set<String> transporters = containerSBML.getReactionsByType(ReactionTypeEnum.Transport);
	//
	//		System.out.println("TRANSPORTADORES: " + transporters.size());
	//
	//		Map<String, ReactionCI> sbmlReactions = containerSBML.getReactions();
	//
	//		Map<String, String> duplicatedReactions = readFileDuplicates();
	//		
	//		Map<String, Map<String, String>> compounds = ModelSEEDCompoundsFileReader.readFile();
	//		
	//		Map<String, String> genes = FilesUtils.readMapFromFile("C:\\Users\\Davide\\OneDrive - Universidade do Minho\\UMinho\\Tese\\KBase\\Reports\\ecoli_Validation30\\geneRules.txt");
	//		
	//		for(String react : duplicatedReactions.keySet()) {
	//			
	//			System.out.println(react + "\t" + duplicatedReactions.get(react));
	//			
	////				boolean rev = true;
	////
	////				if(defaultEC.get(react).getLowerLimit() == 0.0 || defaultEC.get(react).getUpperLimit() == 0.0)
	////					rev = false;
	////				
	////				sbmlReactions.get(react).setReversible(rev);
	////
	////				String reaction = sbmlReactions.get(react).toStringStoiquiometry();
	////				
	////				String reactionByName = sbmlReactions.get(react).toStringStoiquiometry();
	////
	////				for(String id : sbmlReactions.get(react).getMetaboliteSetIds()) {
	////
	////					String auxID = id.substring(2, id.length()-2);
	////
	////					if(compounds.containsKey(auxID) && !compounds.get(auxID).get("name").isEmpty()) {
	////						reactionByName = reactionByName.replace(id, compounds.get(auxID).get("name"));
	////					}
	////				}
	////				
	////				System.out.println(react + "\t" + reaction + "\t" + reactionByName + "\t" + sbmlReactions.get(react).getGeneRuleString() + "\t" + genes.get(duplicatedReactions.get(react)));
	//		}
	//		
	//	}

	//	private Map<String, String> readFileDuplicates(){
	//		
	//		List<String> file = FilesUtils.readWordsInFile("C:\\Users\\Davide\\OneDrive - Universidade do Minho\\UMinho\\Tese\\KBase\\Reports\\ecoli_Validation30\\Duplicates gene rules.txt");
	//		
	//		Map<String, String> res = new HashMap<>();
	//		
	//		String modelR = null;
	//		String transytR = null;
	//		
	//		for(String line : file) {
	//			
	//			if(line.matches("^\\$.*") || line.matches("^R_.*")) {
	//				
	//				if(line.matches("^R_.*"))
	//					modelR = line;
	//				
	//				if(line.matches("^\\$.*"))
	//					transytR = line.split("#")[0].replaceAll("\\$", "").replaceAll("\\s+", "");
	//				
	//				if(modelR != null && transytR != null) {
	//					
	//					res.put(modelR, transytR);
	//					
	//					modelR = null;
	//					transytR = null;
	//				}
	//			}
	//		}
	//		
	//		return res;
	//	}



}
