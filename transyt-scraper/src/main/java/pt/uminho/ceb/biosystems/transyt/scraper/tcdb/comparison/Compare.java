package pt.uminho.ceb.biosystems.transyt.scraper.tcdb.comparison;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import pt.uminho.ceb.biosystems.transyt.utilities.capsules.RowInfo;
import pt.uminho.ceb.biosystems.transyt.utilities.dictionary.Synonyms;
import pt.uminho.ceb.biosystems.transyt.utilities.files.ReadExcelFile;

public class Compare {

	private static final List<String> INCORRECTIONS = Arrays.asList("cu+", "querosine", "ergothioneine", "ai2-", "udp-d-glucose", "isoflavonoid", "colanate", "fe", "coa", "paraquot","sodium", "paat protein",
			"paralytic shellfish toxin", "fes", "tap");

	private static final String PROTON_INTERNAL = "PROTON_INTERNALDB";
	private static final String PROTON_TCDB = "PROTON_TCDB";
	private static final String DIFFERENT = "DIFFERENT";
	private static final String NONE = "NONE";
	private static final String SAME = "SAME";
	private static final String UNKNOWN_INTERNAL = "UNKNOWN_INTERNAL";
	private static final String UNKNOWN_TCDB = "UNKNOWN_TCDB";
	private static final String BIOCHEMICAL = "BIOCHEMICAL";
	private static final String SUBSET_INTERNAL = "SUBSET_INTERNALDB";
	private static final String SUBSET_TCDB = "SUBSET_TCDB";
	private static final String MISSING_ENTRY_TCDB = "MISSING_ENTRY_TCDB";
	private static final String NOT_ANNOTATED_TCDB = "NOT_ANNOTATED_TCDB";
	private static final String FOR_MERGE = "FOR_MERGE";					//esta mal
	private static final String CONJUGATE = "CONJUGATE";					//esta mal
	private static final String WRONG = "WRONG";							//esta mal
	private static final String COLICIN = "COLICIN";
	private static final String SMALL_MOLECULE = "SMALL_MOLECULE";
	private static final String PROTEIN = "PROTEIN";
	private static final String ION = "ION";
	private static final String FIMBRIAL = "FIMBRIAL";
	private static final String AMINOACID = "AMINOACID";
	private static final String SUGAR = "SUGAR";
	private static final String PEPTIDE = "PEPTIDE";
	private static final String NEUROTRANSMITTER = "NEUROTRANSMITTER";
	private static final String MICROCIN = "MICROCIN";
	private static final String HISTIDINE = "HISTIDINE";
	private static final String HEME = "HEME";
	private static final String GLUCOSE = "GLUCOSE";
	private static final String FATTY_ACID = "FATTY ACID";
	private static final String ELECTRON = "ELECTRON";
	private static final String DNA = "DNA";
	private static final String CATION = "CATION";
	private static final String ANION = "ANION";
	private static final String ALLOSE = "ALLOSE";
	private static final String ARABINOSE = "ARABIONESE";
	private static final String HEAVY_METAL = "HEAVY_METAL";
	private static final String LIPID = "LIPID";
	private static final String POLYSACCHARIDE = "POLYSACCHARIDE";
	private static final String METABOLITE = "METABOLITE";
	private static final String PHOSPHOLIPID = "PHOSPHOLIPID";
	private static final String RIBOSE = "RIBOSE";
	private static final String PRECURSOR = "PRECURSOR";
	private static final String HORMONE = "HORMONE";
	private static final String AMIDE = "AMIDE";
	private static final String SERINE = "SERINE";
	private static final String NUCLEOPORIN = "NUCLEOPORIN";
	private static final String ORGANIC = "ORGANIC";
	private static final String NODULATION = "NODULATION";

	private int idColumn;
	private int typeColumn;
	private int originalInternalColumn;
	private int tcColumn;
	private int substrateColumn;
	private int subtrateColumnTCDB;
	private int resultColumn;
	private int classificationColumn;
	private int descriptionColumn;

	private List<String[]> data;

	private Synonyms syn;

	public Compare(){

		syn = new Synonyms();

		int i = 0 ;

		idColumn = i;
		i++;

		typeColumn = i;
		i++;

		originalInternalColumn = i;
		i++;

		tcColumn = i;
		i++;

		substrateColumn = i;
		i++;

		subtrateColumnTCDB = i;
		i++;

		resultColumn = i;
		i++;

		classificationColumn = i;
		i++;

		descriptionColumn = i+1;

		data = ReadExcelFile.getData("C:\\Users\\Davide\\OneDrive - Universidade do Minho\\UMinho\\Tese\\Internal database\\results.xlsx", true, null);

		performComparison(prepareInfomation());

	}

	/**
	 * Constructs an array containing information prepared for the further comparison.
	 */
	private List<RowInfo> prepareInfomation(){

		List<RowInfo> info = new ArrayList<>();

		for(int i = 0; i < data.size(); i++){

			String[] row = data.get(i);

			String id = row[0];

			if(!id.isEmpty()){

				String subsIntDB = row[1];
				String subsTCDB = row[2];
				String tcNumber = row[3];
				String description = row[4];

				Set<String> transportType = getTransportType(subsIntDB, id);

				Set<String> internalDB = getSetForComparison(subsIntDB, true, id);
				Set<String> tcdb = getSetForComparison(subsTCDB, false, id);

				RowInfo capsule = new RowInfo(id, tcNumber, description, internalDB, tcdb, transportType, subsIntDB);

				info.add(capsule);
			}


		}

		return info;
	}

	private Set<String> getTransportType(String metabolites, String id) {

		Set<String> type = new HashSet<>();

		String[] reactions = new String[1];

		reactions[0] = metabolites;

		reactions = metabolites.split("\\|\\|");

		for(int i = 0; i < reactions.length; i++){

			if(reactions[i].contains("//"))
				type.add("Antiport");


			if(reactions[i].contains(":"))
				type.add("Symport");


			if(reactions[i].contains(";") && type.isEmpty())
				type.add("Uniport");
		}

		if(!reactions[0].isEmpty() && type.isEmpty())
			type.add("Uniport");

		return type;
	}

	/**
	 * Puts both sets with the same format and ignores repeated entries
	 * 
	 * @param elements
	 * @param internaldb
	 * @return
	 */
	private Set<String> getSetForComparison(String elements, boolean internaldb, String id){

		Set<String> result = new HashSet<>();

		String[] substrates;
		
		elements = elements.replace("α", "alpha").trim();
		
		elements = elements.replace("β", "beta").trim();

		if(internaldb){

			List<String> signals = Arrays.asList("||", "//", ";", ":");

			for(String signal : signals){
				elements = elements.replace(signal, ";").trim();
			}

			substrates = elements.split(";");
		}
		else {

			//			String[] set = elements.split(",");
			//
			//			Set<String> metabolites = new HashSet<>();
			//
			//			int i = 0;
			//
			//			String compound = "";
			//
			//			while(i < set.length) {
			//
			//				try {
			//
			//					int prefix = Integer.parseInt(set[i].trim());
			//
			//					compound = compound.concat(prefix+"").concat(",");
			//
			//				} 
			//				catch (NumberFormatException e) {
			//
			//					compound = compound.concat(set[i].trim());
			//
			//					metabolites.add(compound);
			//
			//					compound = "";
			//				}
			//				i++;
			
			substrates = elements.split(", ");

		}


		for(int i = 0; i < substrates.length; i++){

			String query = substrates[i].trim().toLowerCase().replaceAll("ic acid", "ate");

			if(query.length() > 3 && !query.contains("drugs")){

				if(query.substring(query.length() - 1).equals("s"))
					query = query.substring(0, query.length() - 1);

			}

			String synonym = syn.getSynonym(query.replaceAll("\\s+", ""));

			if(synonym == null)
				result.add(query);
			else
				result.add(synonym);

		}	

		return result;
	}

	/**
	 * Perform comparisons between internal db and pt.uminho.ceb.biosystems.transyt.scraper.tcdb
	 * 
	 * @param info
	 */
	private void performComparison(List<RowInfo> info){

		try {
			String excelFileName = "C:\\Users\\Davide\\OneDrive - Universidade do Minho\\UMinho\\Tese\\Internal database\\results_comparison.xlsx";

			String sheetName = "Sheet1";//name of sheet

			Workbook workbook = new XSSFWorkbook();
			Sheet sheet  = workbook.createSheet(sheetName) ;

			Row row = sheet.createRow(0);

			row.createCell(idColumn).setCellValue("ID");
			row.createCell(substrateColumn).setCellValue("internal db");
			row.createCell(subtrateColumnTCDB).setCellValue("TCDB");
			row.createCell(tcColumn).setCellValue("tcNumber");
			row.createCell(descriptionColumn + 1).setCellValue("description");
			row.createCell(resultColumn).setCellValue("result");
			row.createCell(typeColumn).setCellValue("Transport type");
			row.createCell(originalInternalColumn).setCellValue("Original Cell Internal db");
			row.createCell(classificationColumn).setCellValue("classification");

			int r = 1;

			for (RowInfo capsule : info){

				row = sheet.createRow(r);

				Set<String> internalSubst = capsule.getInternalSubstrates();
				Set<String> tcdbSubst = capsule.getTcdbSubstrates();

				String intern ="";
				String tcdb = "";

				for(String met : internalSubst)
					intern = intern.concat(met).concat(";");

				for(String met : tcdbSubst)
					tcdb = tcdb.concat(met).concat(";");


				if(!intern.isEmpty())
					intern = intern.substring(0, intern.length() - 1);

				if(!tcdb.isEmpty())
					tcdb = tcdb.substring(0, tcdb.length() - 1);


				row.createCell(idColumn).setCellValue(capsule.getId());
				row.createCell(substrateColumn).setCellValue(intern);
				row.createCell(subtrateColumnTCDB).setCellValue(tcdb);
				row.createCell(tcColumn).setCellValue(capsule.getTcNumber());
				row.createCell(descriptionColumn).setCellValue(capsule.getDescription());
				row.createCell(originalInternalColumn).setCellValue(capsule.getOriginalInternalDB());
				row.createCell(typeColumn).setCellValue(capsule.getTransportType().toString().replaceAll("\\[", "").replaceAll("\\]", ""));

				String result = compareSets(internalSubst, tcdbSubst, capsule.getTcNumber(), capsule.getId());

				String classif = classify(internalSubst, tcdbSubst);

				row.createCell(resultColumn).setCellValue(result);
				row.createCell(classificationColumn).setCellValue(classif);

				r++;
			}

			FileOutputStream fileOut = new FileOutputStream(excelFileName);

			workbook.write(fileOut);
			fileOut.flush();
			fileOut.close();
			
			workbook.close();

			System.out.println("xlsx containing the comparison results created");

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Method for comparison of two sets
	 * 
	 * @param internal
	 * @param pt.uminho.ceb.biosystems.transyt.scraper.tcdb
	 * @return
	 */
	private String compareSets(Set<String> internal, Set<String> tcdb, String tcNumber, String id){

		Set<String> auxInternal = new HashSet<>(internal);
		Set<String> auxTcdb = new HashSet<>(tcdb);

		if(tcdb.contains(NONE.toLowerCase())){
			return NONE;
		}

		else if((tcdb.isEmpty() && tcNumber.isEmpty()) || (tcdb.contains("") && tcNumber.isEmpty())){
			return MISSING_ENTRY_TCDB;
		}

		else if(tcdb.isEmpty() || tcdb.contains("") ){
			return NOT_ANNOTATED_TCDB;
		}

		else if(tcdb.contains("unknown")){
			return UNKNOWN_TCDB;
		}

		else if(internal.contains("unknown")){
			return UNKNOWN_INTERNAL;
		}

		else if(internal.contains("--")){
			return BIOCHEMICAL;
		}

		else if(internal.size() == tcdb.size()){

			auxInternal.removeAll(tcdb);
			auxTcdb.removeAll(internal);

			if(auxInternal.isEmpty() && auxTcdb.isEmpty())
				return SAME;

			else if(auxInternal.size() == internal.size() && auxTcdb.size() == tcdb.size())
				return DIFFERENT;

		}

		else if(internal.size() > tcdb.size()){

			auxInternal.removeAll(tcdb);

			if(auxInternal.size() == 1 && auxInternal.contains("H+"))
				return PROTON_INTERNAL;

			else if(auxInternal.size() == internal.size() - tcdb.size())
				return SUBSET_TCDB;
		}

		else if(tcdb.size() > internal.size()){

			auxTcdb.removeAll(internal);

			if(auxTcdb.size() == 1 && auxTcdb.contains("H+"))
				return PROTON_TCDB;

			else if(auxTcdb.size() == tcdb.size() - internal.size())
				return SUBSET_INTERNAL;
		}

		return FOR_MERGE;
	}

	/**
	 * Classifies based in the content of both sets
	 * 
	 * @param internalSubst
	 * @param tcdbSubst
	 * @return
	 */
	private String classify(Set<String> internalSubst, Set<String> tcdbSubst) {

		String result = "";

		Set<Set<String>> setOfSets = new HashSet<>();
		setOfSets.add(internalSubst);
		setOfSets.add(tcdbSubst);

		for(Set<String> set : setOfSets){

			for(String metabolite : set){
				if(metabolite.contains("conjugate"))
					result = CONJUGATE;

				else if(metabolite.contains("colicin"))
					result = COLICIN;

				else if(metabolite.contains("fimbrial"))
					result = FIMBRIAL;

				else if(metabolite.equals("protein"))
					result = PROTEIN;

				else if(metabolite.equals("ion"))
					result = ION;

				else if(metabolite.contains("small molecule"))
					result = SMALL_MOLECULE;

				else if(metabolite.equals("amino acid"))
					result = AMINOACID;

				else if(metabolite.contains("sugar"))
					result = SUGAR;

				else if(metabolite.contains("peptide"))
					result = PEPTIDE;

				else if(metabolite.contains("neurotransmitter"))
					result = NEUROTRANSMITTER;

				else if(metabolite.contains("microcin"))
					result = MICROCIN;

				else if(metabolite.contains("histidine"))
					result = HISTIDINE;

				else if(metabolite.contains("heme"))
					result = HEME;

				else if(metabolite.contains("glucose"))
					result = GLUCOSE;

				else if(metabolite.contains("fatty acid"))
					result = FATTY_ACID;

				else if(metabolite.equals("electron"))
					result = ELECTRON;

				else if(metabolite.equals("DNA"))
					result = DNA;

				else if(metabolite.equals("cation"))
					result = CATION;

				else if(metabolite.equals("anion"))
					result = ANION;

				else if(metabolite.contains("allose"))
					result = ALLOSE;

				else if(metabolite.contains("arabinose"))
					result = ARABINOSE;

				else if(metabolite.contains("enterocin"))
					result = ARABINOSE;

				else if(metabolite.contains("heavy metal"))
					result = HEAVY_METAL;

				else if(metabolite.contains("lipid"))
					result = LIPID;

				else if(metabolite.contains("polysaccharide"))
					result = POLYSACCHARIDE;

				else if(metabolite.equals("metabolite"))
					result = METABOLITE;

				else if(metabolite.contains("phospholipid"))
					result = PHOSPHOLIPID;

				else if(metabolite.equals("ribose"))
					result = RIBOSE;

				else if(metabolite.contains("hormone"))
					result = HORMONE;

				else if(metabolite.contains("precursor"))
					result = PRECURSOR;

				else if(metabolite.contains("amide"))
					result = AMIDE;

				else if(metabolite.contains("serine"))
					result = SERINE;

				else if(metabolite.contains("nucleoporin"))
					result = NUCLEOPORIN;

				else if(metabolite.contains("organic"))
					result = ORGANIC;

				else if(metabolite.contains("nodulation"))
					result = NODULATION;

				else if(INCORRECTIONS.contains(metabolite))
					result = WRONG;
			}
		}


		return result;
	}


}
