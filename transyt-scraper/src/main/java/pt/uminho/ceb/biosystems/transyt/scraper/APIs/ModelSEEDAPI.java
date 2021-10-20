package pt.uminho.ceb.biosystems.transyt.scraper.APIs;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.uminho.ceb.biosystems.transyt.utilities.connection.LinkConnection;
import pt.uminho.ceb.biosystems.transyt.utilities.connection.TcdbExplorer;
import pt.uminho.ceb.biosystems.transyt.utilities.files.FilesUtils;

public class ModelSEEDAPI {

	public static final String BASE_URL_REACTIONS = "https://raw.githubusercontent.com/ModelSEED/ModelSEEDDatabase/master/Biochemistry/reactions.tsv";
	public static final String BASE_URL_COMPOUNDS = "https://raw.githubusercontent.com/ModelSEED/ModelSEEDDatabase/master/Biochemistry/compounds.tsv";

	public static final int LIMIT = 5;
	public static final int BATCH_SIZE = 10;  //KEGG is limited to 10 items per request

	private static final String PATH_LAST_KNOWN_VERSION_MODELSEED_REACTIONS = FilesUtils.getModelseedReactionsFilesDirectory().concat("tcdbLastKnownVersion.log");
	private static final String PATH_LAST_KNOWN_VERSION_MODELSEED_COMPOUNDS = FilesUtils.getModelseedCompoundsFilesDirectory().concat("tcdbLastKnownVersion.log");
	
	private static final String MODELSEED_REACTIONS_FILE_NAME = "modelseed_reactions";
	private static final String MODELSEED_COMPOUNDS_FILE_NAME = "modelseed_compounds";

	private static final Logger logger = LoggerFactory.getLogger(ModelSEEDAPI.class);

	/**
	 * Get identifier and equations from ModelSEED.
	 * 
	 * @param useCache
	 * @return
	 * @throws Exception
	 */
	public static Map<String, String> getModelseedCompoundsFromGithu(boolean useCache) throws Exception {
		
		logger.info("Downloading latest ModelSEED compounds file from: " + BASE_URL_COMPOUNDS);

		Map<String, String> res = new HashMap<>();

		BufferedReader reader = getLatestCompoundsListFile(useCache);

		String html;

		boolean body = false;

		while ((html = reader.readLine()) != null){

			try {

				if(body) {
					
					String[] data = html.split("\t");
					
					
					if(Integer.valueOf(data[9]) == 0)	//check if not obsolete
						res.put(data[0], data[18]);			

				}
				else {
					body = true;
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		reader.close();
		
		logger.info("ModelSEED compounds download complete!");
		
		return res;
	}
	
	/**
	 * Get identifier and equations from ModelSEED.
	 * 
	 * @param useCache
	 * @return
	 * @throws Exception
	 */
	public static Map<String, String> getModelseedReaction(boolean useCache) throws Exception {
		
		logger.info("Downloading latest ModelSEED reactions file from: " + BASE_URL_REACTIONS);

		Map<String, String> res = new HashMap<>();

		BufferedReader reader = getLatestReactionsListFile(useCache);

		String html;

		boolean body = false;

		while ((html = reader.readLine()) != null){

			try {

				if(body) {
					
					String[] data = html.split("\t");
					
					if(Integer.valueOf(data[18]) == 0)	//check if not obsolete
						res.put(data[0], data[6]);

				}
				else {
					body = true;
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		reader.close();
		
		logger.info("ModelSEED reactions download complete!");
		
		return res;
	}
	
	/**
	 * Method to get the latest file from ModelSEED's repository, unless cache is requested! This can become generic for reactions and compounds if needed
	 * 
	 * @param useCache
	 * @return
	 * @throws InterruptedException
	 * @throws FileNotFoundException
	 */
	private static BufferedReader getLatestCompoundsListFile(boolean useCache) throws InterruptedException, FileNotFoundException {

		try {

			if(!useCache) {
				String filePath = FilesUtils.getModelseedCompoundsFilesDirectory().concat(FilesUtils.generateFileName(MODELSEED_COMPOUNDS_FILE_NAME, ".tsv"));

				OutputStream out = new FileOutputStream(filePath);

				LinkConnection conn = new LinkConnection();

				if(conn.getCodeConnection(BASE_URL_COMPOUNDS) == 200) {

					conn.webPageSaver(conn.getPageOpenStream(), out);

					FilesUtils.saveLastKnownVersion(PATH_LAST_KNOWN_VERSION_MODELSEED_COMPOUNDS, filePath);

					return conn.getPage();

				}

				out.close();
			}

			String lastFilePath = FilesUtils.getLastKnownVersion(PATH_LAST_KNOWN_VERSION_MODELSEED_COMPOUNDS);

			return new BufferedReader(new FileReader(lastFilePath));
		}
		catch (Exception e) {
			e.printStackTrace();

			String lastFilePath = FilesUtils.getLastKnownVersion(PATH_LAST_KNOWN_VERSION_MODELSEED_COMPOUNDS);

			return new BufferedReader(new FileReader(lastFilePath));
		}
	}

	/**
	 * Method to get the latest file from ModelSEED's repository, unless cache is requested!
	 * 
	 * @param useCache
	 * @return
	 * @throws InterruptedException
	 * @throws FileNotFoundException
	 */
	private static BufferedReader getLatestReactionsListFile(boolean useCache) throws InterruptedException, FileNotFoundException {

		try {

			if(!useCache) {
				String filePath = FilesUtils.getModelseedReactionsFilesDirectory().concat(FilesUtils.generateFileName(MODELSEED_REACTIONS_FILE_NAME, ".tsv"));

				OutputStream out = new FileOutputStream(filePath);

				LinkConnection conn = new LinkConnection();

				if(conn.getCodeConnection(BASE_URL_REACTIONS) == 200) {

					conn.webPageSaver(conn.getPageOpenStream(), out);

					FilesUtils.saveLastKnownVersion(PATH_LAST_KNOWN_VERSION_MODELSEED_REACTIONS, filePath);

					return conn.getPage();

				}

				out.close();
			}

			String lastFilePath = FilesUtils.getLastKnownVersion(PATH_LAST_KNOWN_VERSION_MODELSEED_REACTIONS);

			return new BufferedReader(new FileReader(lastFilePath));
		}
		catch (Exception e) {
			e.printStackTrace();

			String lastFilePath = FilesUtils.getLastKnownVersion(PATH_LAST_KNOWN_VERSION_MODELSEED_REACTIONS);

			return new BufferedReader(new FileReader(lastFilePath));
		}
	}

}
