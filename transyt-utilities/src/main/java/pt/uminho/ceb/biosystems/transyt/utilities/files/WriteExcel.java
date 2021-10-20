package pt.uminho.ceb.biosystems.transyt.utilities.files;

import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import pt.uminho.ceb.biosystems.transyt.utilities.capsules.BiosynthMetaboliteProperties;
import pt.uminho.ceb.biosystems.transyt.utilities.transytUtilities.Utilities;

public class WriteExcel {

	public static void writeNeo4jKeggInfo(Map<String, BiosynthMetaboliteProperties> info){

		try {
//			String excelFileName = "C:\\Users\\Davide\\Documents\\InternalDB\\alphaBetaGamma.xlsx";
			String excelFileName = "C:\\Users\\Davide\\Documents\\InternalDB\\neo4jdataNew.xlsx";

			String sheetName = "Sheet1";//name of sheet

			Workbook workbook = new XSSFWorkbook();
			Sheet sheet  = workbook.createSheet(sheetName) ;

			Row row = sheet.createRow(0);

			row.createCell(0).setCellValue("entry ID");
			row.createCell(1).setCellValue("source");
			//			row.createCell(1).setCellValue("Kegg ID");
			row.createCell(2).setCellValue("names");
			row.createCell(3).setCellValue("formula");
			row.createCell(4).setCellValue("best reference source");
			row.createCell(5).setCellValue("best reference ID");
			row.createCell(5).setCellValue("best reference url");
			row.createCell(7).setCellValue("all references");
			//			row.createCell(4).setCellValue("Kegg Link");

			int r = 1;

			for (String entryID : info.keySet()){

				BiosynthMetaboliteProperties metabolite = info.get(entryID);

				String names = metabolite.getSynonyms().toString();

//				if(!names.equals("[]") //&& names.contains("&")
//						) {

					row = sheet.createRow(r);

					row.createCell(0).setCellValue(entryID);
					row.createCell(1).setCellValue(metabolite.getSource());
					//				row.createCell(1).setCellValue(metabolite.getKeggID());
					row.createCell(2).setCellValue(names.replaceAll("<i>", "").replaceAll("</i>", "").replaceAll("\\[", "")
							.replaceAll("\\]", "").replaceAll("</sup>", "").replaceAll("<sup>", "").replaceAll("</I>", "").replaceAll("<I>", ""));
					row.createCell(3).setCellValue(metabolite.getFormula());
					row.createCell(4).setCellValue(metabolite.getBestReferenceSource());
					row.createCell(5).setCellValue(metabolite.getBestReferenceID());
					row.createCell(6).setCellValue(metabolite.getBestReferenceURL());
					row.createCell(7).setCellValue(metabolite.getReferences().toString());
					//				row.createCell(4).setCellValue(metabolite.getKeggLink());

					r++;

//				}

				
//				if(r == 30000) {
//
//					System.out.println("BREAKING...");
//					break;
//				}
			}

			FileOutputStream fileOut = new FileOutputStream(excelFileName);

			workbook.write(fileOut);
			fileOut.flush();
			fileOut.close();

			workbook.close();

			System.out.println("xlsx containing the biosynth results created");

		} catch (Exception e) {
			e.printStackTrace();
		}

	}


	public static void tableToExcel(List<String[]> info, String path){

		try {

			String sheetName = "Sheet1";//name of sheet

			Workbook workbook = new XSSFWorkbook();
			Sheet sheet  = workbook.createSheet(sheetName) ;

			Row row = sheet.createRow(0);
			
			for(int i = 0; i < info.get(0).length; i++)
				row.createCell(i).setCellValue(info.get(0)[i]);

			for(int i = 1; i < info.size(); i++) {
				
				row = sheet.createRow(i);

				for(int j = 0; j < info.get(0).length; j++) {

					row.createCell(j).setCellValue(info.get(i)[j]);

				}
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
	
	public static void validationTableToExcel(List<String[]> info, String path, Map<String, List<String>> validations){

		try {

			String sheetName = "Sheet1";//name of sheet

			Workbook workbook = new XSSFWorkbook();
			Sheet sheet  = workbook.createSheet(sheetName) ;

			Row row = sheet.createRow(0);
			
			for(int i = 0; i < info.get(0).length; i++)
				row.createCell(i).setCellValue(info.get(0)[i]);

			for(int i = 1; i < info.size(); i++) {
				
				row = sheet.createRow(i);
				
				for(int j = 0; j < info.get(0).length; j++) {
					
					row.createCell(j).setCellValue(info.get(i)[j]);
				}
				
				if(validations.containsKey(info.get(i)[0])) {
					
					if(validations.get(info.get(i)[0]).size() > 0)
							row.createCell(5).setCellValue(validations.get(info.get(i)[0]).get(0));
					if(validations.get(info.get(i)[0]).size() > 1)
							row.createCell(6).setCellValue(validations.get(info.get(i)[0]).get(1));
					
				}
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

}
