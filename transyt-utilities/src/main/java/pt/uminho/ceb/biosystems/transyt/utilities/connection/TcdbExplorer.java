package pt.uminho.ceb.biosystems.transyt.utilities.connection;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.uminho.ceb.biosystems.transyt.utilities.files.FilesUtils;

/**
 * @author Davide
 *
 */
public class TcdbExplorer {

	private static int LIMIT = 10;
	public static final int ALL_SEARCH_LIMIT = 2;

	public static final String TCDB_FASTA_URL = "http://www.tcdb.org/public/tcdb";
	public static final String URL_SUPERFAMILIES = "http://www.tcdb.org/superfamily.php";
	public static final String TCDB_TCNUMBER_URL = "http://www.tcdb.org/search/result.php?tc=";

	private static final Logger logger = LoggerFactory.getLogger(TcdbExplorer.class);

	/**
	 * Get the tcNumbers for which the information is supposed to be retrieved.
	 * 
	 * @return
	 */
	public static Set<String> getTcNumbers(boolean useCache){

		try {

			return ReadFastaTcdb.readTcNumbersFromFasta(useCache);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Method to get only the tcNumbers belonging to superfamilies.
	 * 
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
	private static Set<String> getTcNumbersBelongingToSuperfamilies() throws Exception{

		Set<String> tcNumbers = new HashSet<>();

		LinkConnection conn = new  LinkConnection();

		int code = conn.getCodeConnection(URL_SUPERFAMILIES);

		if(code == 200) {

			BufferedReader in = conn.getPage();

			String html;

			while ((html = in.readLine()) != null){

				Document doc = Jsoup.parse(html);
				String text = doc.body().text().trim();

				//				System.out.println(text);


				Pattern p = Pattern.compile("\\A\\d.\\w");		//meaning string beginning with 'digit - dot - any word character'
				Matcher m = p.matcher(text);

				//				System.out.println(m.find());

				if(m.find())
					tcNumbers.add(text.split("\\s+")[0]);
			}

			//			System.out.println(tcNumbers);
			//			System.out.println(tcNumbers.size());

		}
		return tcNumbers;

	}

	/**
	 * Convert normal tcNumber (*.$.*.*.*) in *.$.* format
	 * 
	 * @param tcNumbers
	 * @return
	 */
	public static Set<String> generateTCsFamily(Set<String> tcNumbers){

		Set<String> tcFamilies = new HashSet<>();

		for(String tcNumber : tcNumbers) {

			String[] newTcNumber = tcNumber.split("\\."); 

			String newTc = newTcNumber[0].concat(".").concat(newTcNumber[1]).concat(".").concat(newTcNumber[2]);

			if(!tcFamilies.contains(newTc)) 
				tcFamilies.add(newTc);

		}
		return tcFamilies;
	}

	/**
	 * Method to find the description of the family where the tcNumber belong
	 * 
	 * @param tcNumbers
	 * @return
	 * @throws Exception 
	 * @throws InterruptedException 
	 */
	public static void getProteinsBelongingToFamilyDescription(Set<String> tcNumbers) throws InterruptedException, Exception{

		Map<String, String> proteinFamilyDescription = new HashMap<>();

		List<String> searched = new ArrayList<>();
		List<String> failed = new ArrayList<>();

		int allAttempt = 0;
		
		boolean continueSearch = true;

		logger.info("Searching types of transport evidences...");

		while(continueSearch) {
			
			int lastProgress = -1;

			for(String tcNumber : tcNumbers) {

				tcNumber = tcNumber.replaceAll("(\\.\\d+)$", "");

				int attempt = 0;
				boolean found = false;

				if(!proteinFamilyDescription.containsKey(tcNumber) && !searched.contains(tcNumber)) {
					while(attempt < LIMIT && !found) {

						try {

							LinkConnection conn = new LinkConnection();

							if(conn.getCodeConnection(TcdbExplorer.TCDB_TCNUMBER_URL.concat(tcNumber)) == 200){

								String html;
								BufferedReader in = conn.getPage();

								while ((html = in.readLine()) != null){

									Document doc = Jsoup.parse(html);
									String text = doc.body().text().trim();

									if(text.matches("^(\\d+\\.\\w+\\.\\d+\\.\\d+:).*")) {

										String[] auxText = text.split(":\\s+");

										proteinFamilyDescription.put(auxText[0].trim(), auxText[1].trim());
									}

								}

								searched.add(tcNumber);

								found = true;

								TimeUnit.MILLISECONDS.sleep(500);

							}
							else {
								attempt++;
								TimeUnit.SECONDS.sleep(10);
							}
						} 
						catch (ArrayIndexOutOfBoundsException e1) {

							attempt = LIMIT;

							logger.error("An error occurred while retrieving entry {}", tcNumber);
							logger.trace("StrackTrace: {}", e1);

						}
						catch (Exception e) {

							attempt++;
							logger.warn("Retrying connection... Attempt nr: {}", attempt);
							TimeUnit.MINUTES.sleep(2);
							logger.trace("StrackTrace: {}", e);
						}

					}

					int progress = ((searched.size()+failed.size())*100)/tcNumbers.size();

					if(progress > lastProgress){

						lastProgress = progress;
						String message = progress + " % search complete";
						logger.info(message);
					}	

					if(attempt == LIMIT && !found){

						logger.warn("results not found for query: {}", TcdbExplorer.TCDB_TCNUMBER_URL.concat(tcNumber));
						failed.add(tcNumber);
					}

				}
				else
					searched.add(tcNumber);

			}
			
			if(failed.size() > 0 && allAttempt < ALL_SEARCH_LIMIT) {
				allAttempt++;
				
				tcNumbers = new HashSet<>(failed);
				failed = new ArrayList<>();
				
				logger.info("Retrying search of previously failed queries. Attempt nr {}", allAttempt);
			}
			else {
				continueSearch = false;
			}
		}
		
		if(failed.size() > 0)
			logger.warn("The following queries failed: {}", failed.toString());
		
		FilesUtils.saveMapInFile(FilesUtils.getBackupFamilyDescriptionsFilesDirectory().concat("familyDescription.txt"), proteinFamilyDescription);

	}
}
