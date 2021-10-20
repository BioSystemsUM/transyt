package pt.uminho.ceb.biosystems.transyt.utilities.connection;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.uminho.ceb.biosystems.transyt.utilities.files.FilesUtils;
import pt.uminho.ceb.biosystems.transyt.utilities.files.JSONFilesUtils;

public class TcdbRetriever {

	public static final int LIMIT = 5;
	public static final int ALL_SEARCH_LIMIT = 2;
	public static final String SUBSTRATE = "substrate";
	public static final String TCNUMBER = "tcNumber";
	public static final String DESCRIPTION = "description";
	public static final String ORGANISM = "organism";

	private static final Logger logger = LoggerFactory.getLogger(TcdbRetriever.class);

	public static void getSubstrates() throws InterruptedException, IOException{

		logger.info("Searching entries to retrieve from TCDB...");

		Set<String> toSearch = ReadFastaTcdb.readfasta(true).keySet();
		
		String name = FilesUtils.generateFileName("queries", ".txt");

		FilesUtils.saveWordsInFile(FilesUtils.getBackupQueriesFilesDirectory().concat(name), toSearch);

		List<String> searched = new ArrayList<>();

		List<String> failed = new ArrayList<>();

		Map<String, Map<String, String>> results = new HashMap<>();
		
		logger.info("Retrieving data from TCDB...");

		int attempt = 0;
		int allAttempt = 0;

		boolean continueSearch = true;

		while(continueSearch) {
			
			int lastProgress = -1;

			for(String query : toSearch){

				boolean found = false;
				attempt = 0;

				while(attempt < LIMIT && !found){

					try {

						String[] keys = query.split("@");

						String link = "http://www.tcdb.org/search/result.php?acc=".concat(keys[0].trim()).concat("&tc=").concat(keys[1].trim());

						LinkConnection conn = new LinkConnection();

						if (conn.getCodeConnection(link) == 200){

							Map<String, String> submap = parseHTML(conn.getPage());

							searched.add(query);
							results.put(query, submap);

							found = true;

							int progress = ((searched.size()+failed.size())*100)/toSearch.size();

							if(progress > lastProgress){

								lastProgress = progress;
								String message = progress + " % search complete";
								logger.info(message);
							}

							TimeUnit.MILLISECONDS.sleep(500);	//if starts failing, add again
						}
						else{

							attempt++;

							logger.warn("Retrying connection... Attempt nr: {}", attempt);

							TimeUnit.SECONDS.sleep(30);
						}

					} 
					catch (ArrayIndexOutOfBoundsException e1) {

						attempt = LIMIT;

						logger.error("An error occurred while retrieving entry {}", query);
						logger.trace("StrackTrace: {}", e1);

					}
					catch (Exception e) {

						attempt++;
						logger.warn("Retrying connection... Attempt nr: {}", attempt);
						TimeUnit.MINUTES.sleep(2);
						logger.trace("StrackTrace: {}", e);
					}
				}

				if(attempt == LIMIT && !found){

					logger.warn("Results not found for query: {}", query);
					failed.add(query);
				}
			}
			
			if(failed.size() > 0 && allAttempt < ALL_SEARCH_LIMIT) {
				allAttempt++;
				
				toSearch = new HashSet<>(failed);
				failed = new ArrayList<>();
				
				logger.info("Retrying search of previously failed queries. Attempt nr {}", allAttempt);
			}
			else {
				continueSearch = false;
			}
		}

		if(failed.size() > 0)
			logger.warn("The following queries failed: {}", failed.toString());

		JSONFilesUtils.exportTCDBScrapedInfo(results);
		//		exportToXLS(results, failed);

	}

	/**
	 * Get information from TCDB page.
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	private static Map<String, String> parseHTML(BufferedReader in) throws IOException {

		String html;

		String substrate = "";

		String description = "";

		String organism = "";

		Map<String, String> info = new HashMap<>();

		boolean searchComplete = false;
		boolean searchSubstrate = false;
		boolean searchTcNumber = false;
		boolean searchDescription = false;

		String auxDescription = "";

		while ((html = in.readLine()) != null && !searchComplete){

			Document doc = Jsoup.parse(html);
			String text = doc.body().text().trim();

			if(text.contains("Species:")) {
				String[] organismSplit = text.split(":");

				if(organismSplit.length > 1)
					organism = organismSplit[1];
				else
					organism = "";

				info.put(ORGANISM, organism);
			}

			if(searchSubstrate == true){

				if(text.contains("database")){
					info.put(SUBSTRATE, substrate);
					searchSubstrate = false;

					searchComplete = true;
				}
				else{
					if(!text.isEmpty()){
						substrate = substrate.concat(text);
					}
				}

			}
			else if(searchTcNumber == true && !text.isEmpty()){

				if(text.contains("Accession Number:")){

					searchDescription = false;
					searchTcNumber = false;

					info.put(DESCRIPTION, description.replace("-->", ""));
				}

				else if(searchDescription == false){

					String[] line = text.split("\\s");
					info.put(TCNUMBER, line[0]);

					if(line.length > 1)
						description = description.concat(text.substring(line[0].length() + 1));

					auxDescription = new String(description);

					searchDescription = true;
				}
				else if(searchDescription == true){

					if(description.length() > 0){
						if(description.substring(description.length() - 1).equals("-")){

							description = description.substring(0, description.length()-1).concat(text);

						}
						else{
							description = description.concat(" ").concat(text);
						}
					}
					else{
						description = description.concat(" ").concat(text);
					}

					if(text.contains("\\{") || text.contains("\\}"))
						description = auxDescription;

				}

			}

			if(text.equals("Substrate"))
				searchSubstrate = true;

			else if(text.contains("See all members of the family"))
				searchTcNumber = true;

		}
		return info;
	}

	@Deprecated
	private static void exportToXLS(Map<String, Map<String, String>> results, List<String> failed){

		try {
			String excelFileName = "C:\\Users\\Davide\\OneDrive - Universidade do Minho\\UMinho\\Tese\\Internal database\\results2.xlsx";

			String sheetName = "Sheet1";//name of sheet

			Workbook workbook = new XSSFWorkbook();
			Sheet sheet  = workbook.createSheet(sheetName) ;

			Row row = sheet.createRow(0);

			row.createCell(0).setCellValue("ID");
			row.createCell(1).setCellValue("Organism");
			row.createCell(2).setCellValue("TCDB");
			row.createCell(3).setCellValue("tcNumber");
			row.createCell(4).setCellValue("Description");
			//			row.createCell(10).setCellValue("FAILED");


			int r = 1;

			for (String key : results.keySet()){

				Map <String, String> submap = results.get(key);

				row = sheet.createRow(r);

				//iterating c number of columns
				for (int c=0;c < 5; c++ )
				{
					Cell cell = row.createCell(c);

					if(c == 0)
						cell.setCellValue(key);

					else if(c == 1){
						if(results.get(key) != null)
							cell.setCellValue(submap.get(ORGANISM));
					}

					else if(c == 2){
						if(results.get(key) != null)
							cell.setCellValue(submap.get(SUBSTRATE));
					}

					else if(c == 3){
						if(results.get(key) != null)
							cell.setCellValue(submap.get(TCNUMBER));
					}

					else if(c == 4){
						if(results.get(key) != null)
							cell.setCellValue(submap.get(DESCRIPTION));
					}
				}
				r++;
			}

			//			for (String entry : failed){
			//
			//				sheet.createRow(r).createCell(10).setCellValue(entry);
			//
			//				r++;
			//			}

			FileOutputStream fileOut = new FileOutputStream(excelFileName);

			//write this workbook to an Outputstream.
			workbook.write(fileOut);
			fileOut.flush();
			fileOut.close();

			workbook.close();

			System.out.println("xlsx containing the results created");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}






}
