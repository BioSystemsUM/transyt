package pt.uminho.ceb.biosystems.transyt.service.internalDB;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pt.uminho.ceb.biosystems.transyt.utilities.files.ReadExcelFile;

public class InternaldbMetabolites {

	private static final int INTERNAL_COL = 4;
	private static final int TCDB_COL = 5;
	private static final String FILE_PATH = "C:\\Users\\Davide\\OneDrive - Universidade do Minho\\UMinho\\Tese\\Internal database\\results_comparison-curated.xlsb.xlsx";

	//	public static void main(String[] args) {
	//
	//		//		System.out.println(getAllMetabololites());
	//		
	////		System.out.println(getAllMetabololites().size());
	//
	//		Set<String> metab = getAllMetabololites();
	//		
	////		System.out.println(metab.size());
	////
	////		for(String met : metab)
	////			System.out.println(met);
	//	}

	public static Set<String> getAllMetabololites() {

		List<String[]> data = ReadExcelFile.getData(FILE_PATH, true, null);

		Set<String> metabolites = new HashSet<>();

		for(String[] line : data) {

			Set<String> res = new HashSet<>();

//			if(line[INTERNAL_COL] != null) {			//Almost all pt.uminho.ceb.biosystems.transyt.scraper.tcdb entries will be accepted

//				String[] internal = line[INTERNAL_COL].split(";");

//				for(String met : internal) {
					//					if(met.equals("s (2-aminoethyl)-l-cysteine"))
					//						System.out.println(line[0]);
//					if(!met.isEmpty())
//						res.add(met.trim().toLowerCase());
//				}

//			}

			if(line[TCDB_COL] != null) {

				String[] tcdb = line[TCDB_COL].split(";");

				for(String met : tcdb) {
					//					if(met.equals("s (2-aminoethyl)-l-cysteine"))
					//						System.out.println(line[0]);

					if(!met.isEmpty())
						res.add(met.trim().toLowerCase());
				}

			}

			metabolites.addAll(res);

		}
		return metabolites;

	}

	public static Set<String> getAllMetabololites222() {

		List<String[]> data = ReadExcelFile.getData(FILE_PATH, true, null);

		Set<String> metabolites = new HashSet<>();

		for(String[] line : data) {

			Set<String> res = new HashSet<>();

			if(line[INTERNAL_COL] != null) {

				String[] internal = line[INTERNAL_COL].split(";");

				for(String met : internal) {
					//					if(met.equals("s (2-aminoethyl)-l-cysteine"))
					//						System.out.println(line[0]);
					res.add(met.trim().toLowerCase());
				}

			}

			if(line[TCDB_COL] != null) {

				String[] tcdb = line[TCDB_COL].split(";");

				for(String met : tcdb) {
					//					if(met.equals("s (2-aminoethyl)-l-cysteine"))
					//						System.out.println(line[0]);

					res.add(met.trim().toLowerCase());
				}

			}

			metabolites.addAll(res);

		}
		return metabolites;

	}


}
