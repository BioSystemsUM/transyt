package pt.uminho.ceb.biosystems.transyt.scraper.APIs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.uminho.ceb.biosystems.transyt.utilities.connection.LinkConnection;
import pt.uminho.ceb.biosystems.transyt.utilities.files.FilesUtils;

public class KeggAPI {

	public static final String PATH_LAST_KNOWN_VERSION = FilesUtils.getKeggFastaDirectory().concat("tcdbLastKnownVersion.log");
	private static final String KEGG_FASTA_NAME = "keggFasta";
	
	private static final Map<String, String> KOs = FilesUtils.readMapFromFile(FilesUtils.getKOsToSearchFilePath());

	public static final String BASE_URL = "http://rest.kegg.jp/get/";
	public static final int LIMIT = 5;
	public static final int BATCH_SIZE = 10;  //KEGG is limited to 10 items per request
	public static final int DEFAULT_DELAY_MILLIS = 800;

	private static final Logger logger = LoggerFactory.getLogger(KeggAPI.class);

	public static Map<String, String> searchKeegPTSGenesAndBuildFastaFiles(){

		Map<String, String> results = new HashMap<>();
		
		for(String ko : KOs.keySet()) {
			
			logger.info("Searching KO: " + ko);
			
			try {
				LinkConnection connection = getKOInfo(ko);

				Map<String, Set<String>> genes = scrapeGenesFromKoInfo(connection.getPage());

				Set<String> queries = generateBatchesDistribution(genes);

				Map<String, String> sequences = getGenesProteinSequence(queries, ko);

				results.putAll(sequences);
				
			} catch (Exception e) {
				logger.error("An error occurred retrievig KO data: " + ko);
				e.printStackTrace();
			}
		}
		
		saveResults(results);
		
		return results;
	}

	public static LinkConnection getKOInfo(String ko) throws InterruptedException {

		int attempt = 0;

		while(attempt < LIMIT){

			try {
				String link = BASE_URL + "ko:" + ko;
				LinkConnection conn = new LinkConnection();

				int code = conn.getCodeConnection(link);

				if (code == 200){
					return conn;
				}
				else{
					System.out.println(link);
					System.out.println(code);
					attempt++;
					logger.warn("Retrying connection... Attempt nr: {}", attempt);
					TimeUnit.SECONDS.sleep(30);
				}
			}
			catch (ArrayIndexOutOfBoundsException e1) {

				attempt = LIMIT;
				e1.printStackTrace();
				logger.error("An error occurred while retrieving entry {}", ko);
				logger.trace("StrackTrace: {}", e1);
			}
			catch (Exception e) {
				e.printStackTrace();
				attempt++;
				logger.warn("Retrying connection... Attempt nr: {}", attempt);
				TimeUnit.SECONDS.sleep(30);
				logger.trace("StrackTrace: {}", e);
			}
		}

		return null;
	}

	/**
	 * Method that searches the AA sequences of each entry
	 * 
	 * @param queries
	 * @return
	 * @throws InterruptedException
	 */
	public static Map<String, String> getGenesProteinSequence(Set<String> queries, String reference) throws InterruptedException{

		Map<String, String> results = new HashMap<>();

		Set<String> searched = new HashSet<>();
		Set<String> failed = new HashSet<>();

		logger.info("Searching protein sequences for each KEGG gene...");

		boolean continueSearch = true;

		while(continueSearch) {

			int lastProgress = -1;
			searched = new HashSet<>();

			for(String query : queries){

				boolean found = false;
				int attempt = 0;

				while(attempt < LIMIT && !found){

					Instant start = Instant.now();

					try {
						String link = BASE_URL + query;
						LinkConnection conn = new LinkConnection();
						int code = conn.getCodeConnection(link);

						if (code == 200){

							results.putAll(scrapeGenesSequences(conn.getPage(), reference));

							searched.add(query);
							found = true;

							int progress = ((searched.size()+failed.size())*100)/queries.size();

							if(progress > lastProgress){

								lastProgress = progress;
								String message = progress + " % search complete";
								logger.info(message);
							}
							applyWait(start);
						}
						else{
							System.out.println(link);
							System.out.println(code);
							attempt++;
							logger.warn("Retrying connection... Attempt nr: {}", attempt);
							TimeUnit.SECONDS.sleep(30);
						}
					}
					catch (ArrayIndexOutOfBoundsException e1) {

						attempt = LIMIT;
						e1.printStackTrace();
						logger.error("An error occurred while retrieving entry {}", query);
						logger.trace("StrackTrace: {}", e1);
					}
					catch (Exception e) {
						e.printStackTrace();
						attempt++;
						logger.warn("Retrying connection... Attempt nr: {}", attempt);
						TimeUnit.SECONDS.sleep(30);
						logger.trace("StrackTrace: {}", e);
					}
				}
			}
			continueSearch = false;
		}

		return results;
	}

	/**
	 * Method to retrieve genes of the EC info page
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static Map<String, String> scrapeGenesSequences(BufferedReader in, String reference) throws IOException {

		Map<String, String> res = new HashMap<>();

		String html;

		boolean read = false;
		String entry = null;
		String organism = null;
		String sequence = "";

		while ((html = in.readLine()) != null){

			try {
				Document doc = Jsoup.parse(html);
				String text = doc.body().text().trim();

				if(text.matches("^ENTRY\\s+.*")) {
					entry = text.split("\\s+")[1].trim();
				}
				else if(text.matches("^ORGANISM\\s+.*")) {
					organism = text.split("\\s+")[1].trim();
				}
				else if(text.matches("^AASEQ\\s+.*")) {
					read = true;
				}
				else if(text.matches("^NTSEQ\\s+.*")) {
					read = false;
					
					String id = "kegg|" + reference + "|" + organism + "_" + entry;
					
					if(KOs.containsKey(reference))
						id = id.concat("|" + KOs.get(reference));
					
					res.put(id, sequence.replaceAll("\n", ""));

					sequence = "";
				}

				if(read && !text.matches("^AASEQ\\s+.*")) {
					sequence = sequence.concat(text);
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}

		return res;
	}


	/**
	 * Method to retrieve genes of the EC info page
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static Map<String, Set<String>> scrapeGenesFromKoInfo(BufferedReader in) throws IOException {

		Map<String, Set<String>> res = new HashMap<>();

		String html;

		boolean read = false;

		while ((html = in.readLine()) != null){

			try {
				Document doc = Jsoup.parse(html);
				String text = doc.body().text().trim();

				if(text.matches("^GENES\\s+.*")) {
					read = true;
					text.replaceAll("^GENES", "");
				}
				else if(text.matches("^REFERENCE\\s+.*")) {
					read = false;
				}

				if(read) {
					String[] line = text.split("\\s+");

					String db = null;

					for(int i = 0; i < line.length; i++) {
						if(i == 0) {
							db = line[0].trim().toLowerCase();
							if(!res.containsKey(db)) {
								res.put(db, new HashSet<>());
							}
						}
						else {
							Set<String> genes = res.get(db);
							genes.add(line[i].split("\\(")[0].trim());
							res.put(db, genes);
						}
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}

		return res;
	}

	public static Set<String> generateBatchesDistribution(Map<String, Set<String>> toSearch){
		Set<String> queries = new HashSet<>();

		String q = "";
		int i = 0;

		for(String db : toSearch.keySet()) {

			Set<String> genes = toSearch.get(db);

			for(String gene : genes) {

				if(!q.isEmpty())
					q = q.concat("+");

				q = q.concat(db + gene);
				i++;

				if(i == BATCH_SIZE) {
					queries.add(q.replaceAll("\n", ""));
					q = "";
					i = 0;
				}
			}
		}

		if(!q.isBlank())
			queries.add(q.replaceAll("\n", ""));

		return queries;
	}

	/**
	 * @param start
	 */
	public static void applyWait(Instant start) {

		try {
			long timeElapsed = Duration.between(start, Instant.now()).toMillis();
			if(timeElapsed < DEFAULT_DELAY_MILLIS)
				TimeUnit.MILLISECONDS.sleep(DEFAULT_DELAY_MILLIS - timeElapsed);
		} 
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param sequences
	 */
	private static void saveResults(Map<String, String> sequences) {

		try {

			String filePath = FilesUtils.getKeggFastaDirectory().concat(FilesUtils.generateFileName(KEGG_FASTA_NAME, ".faa"));

			FilesUtils.saveLastKnownVersion(PATH_LAST_KNOWN_VERSION, filePath);

			File fastaFile = new File(filePath);

			FileWriter fstream = new FileWriter(fastaFile);  
			BufferedWriter out = new BufferedWriter(fstream); 

			for(String seqID : sequences.keySet()) {

				String sequence = sequences.get(seqID);

				out.write(">" + seqID + "\n");

				out.write(sequence+"\n\n");
			}
			out.close();
		} 
		catch (Exception e) {

			e.printStackTrace();
		}
	}
}
