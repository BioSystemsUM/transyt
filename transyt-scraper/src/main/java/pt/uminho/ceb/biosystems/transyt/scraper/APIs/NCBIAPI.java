package pt.uminho.ceb.biosystems.transyt.scraper.APIs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.uminho.ceb.biosystems.mew.utilities.datastructures.pair.Pair;
import pt.uminho.ceb.biosystems.mew.utilities.io.FileUtils;
import pt.uminho.ceb.biosystems.transyt.utilities.files.FilesUtils;

public class NCBIAPI {

	public static final String BASE_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?";
	public static final int LIMIT = 5;
	private static final String URL_TAXONOMY_ZIP = "ftp://ftp.ncbi.nlm.nih.gov/pub/taxonomy/taxdmp.zip";

	private static final Logger logger = LoggerFactory.getLogger(NCBIAPI.class);
	
	/**
	 * This method tries to find the lineage in the cached files. 
	 * If no results are found, a second attempt is made using NCBI webservices.
	 * 
	 * @param taxID
	 * @return
	 */
	public static Pair<String, List<String>> getLineageFromTaxID(Integer taxID){
		
		Pair<String, List<String>> pair = getLineageFromCachedFiles(taxID);
		
		if(pair == null)
			try {
				pair = getLineageFromWebservice(taxID);
			} 
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		
		return pair;
	}

	public static Pair<String, List<String>> getLineageFromWebservice(Integer taxonomyID) throws InterruptedException {

		int attempt = 0;

		while(attempt < LIMIT){

			try {
				URL oURL = new URL(BASE_URL + "db=taxonomy&retmode=xml&id=" + taxonomyID.toString());
				URLConnection oConnection = oURL.openConnection();
				BufferedReader reader = new BufferedReader(new InputStreamReader(oConnection.getInputStream()));

				String xml;
				String organism = null;
				List<String> lineage = null;

				boolean lineageFound = false;
				boolean organismFound = false;

				while ((xml = reader.readLine()) != null){ 

					if(xml.contains("<Lineage>") && !lineageFound) {
						lineage = new ArrayList<>(Arrays.asList(xml.replace("<Lineage>", "").replace("</Lineage>", "").strip().split(";\\s+")));
						lineage.remove(0); //cellular organisms
						lineageFound = true;
					}
					else if (xml.contains("<ScientificName>") && !organismFound) {
						organism = xml.replace("<ScientificName>", "").replace("</ScientificName>", "").strip();
						organismFound = true;
					}
				}

				return new Pair<String, List<String>>(organism, lineage);
			}
			catch (ArrayIndexOutOfBoundsException e1) {

				attempt = LIMIT;
				e1.printStackTrace();
				logger.error("An error occurred while retrieving entry {}", taxonomyID);
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
	 * @param useCache
	 */
	@SuppressWarnings("resource")
	public static void downloadAndUncompressTaxonomyFiles() {

		try {	
			String tempDirPath = FilesUtils.getScraperProjectDirectory().concat("tax_temp/");
			String zipFilePath = tempDirPath.concat("download.zip");

			File tempFile = new File(tempDirPath);
			
			if(tempFile.exists())
				FileUtils.deleteDirectory(tempFile);
			
			tempFile.mkdir();

			new FileOutputStream(zipFilePath).getChannel()
			.transferFrom(Channels.newChannel(new URL(URL_TAXONOMY_ZIP)
					.openStream()), 0, Long.MAX_VALUE);

			FileUtils.extractZipFile(zipFilePath, tempDirPath);

			compressTaxonomyRelatioshipsFile(tempDirPath + "nodes.dmp");

			compressTaxonomyNamesFile(tempDirPath + "names.dmp");

			FileUtils.deleteDirectory(tempFile);
		}
		catch (Exception e) {

			e.printStackTrace();

		}
	}

	/**
	 * This method uses information previously saved in files to find the lineage using a taxonomy identifier.
	 * 
	 * @param taxID
	 * @return
	 */
	public static Pair<String, List<String>> getLineageFromCachedFiles(Integer taxID){
		
		try {
			logger.debug("Finding lineage using taxonomy identifier: " + taxID);

			Map<Integer, String> names = FilesUtils.readMapFromFile3(FilesUtils.getNcbiTaxonomyNamesFilePath());
			Map<Integer, Integer> nodes = FilesUtils.readMapFromFile4(FilesUtils.getNcbiTaxonomyRelationshipsFilePath());

			List<Integer> lineage = new ArrayList<>();
			List<String> strLineage = new ArrayList<>();
			lineage.add(taxID);

			while(taxID > 1){
				taxID = nodes.get(taxID);
				lineage.add(taxID);
			}

			for(int i = lineage.size()-1; i >= 0; i--){
				strLineage.add(names.get(lineage.get(i)));
			}

			strLineage.remove(0); //removes 'root'
			strLineage.remove(0); //removes 'celular organism'

			String organism = strLineage.get(strLineage.size()-1);
			
			strLineage.remove(strLineage.size()-1);
			
			return new Pair<String, List<String>>(organism, strLineage);
		} 
		catch (Exception e) {
			
			logger.error("An error occurred searching the taxonomy in the cached files!");
			
			return null;
		}
	}

	/**
	 * @param filePath
	 */
	private static void compressTaxonomyRelatioshipsFile(String filePath){

		try {
			PrintWriter writer = new PrintWriter(FilesUtils.getNcbiTaxonomyRelationshipsFilePath(), "UTF-8");
			BufferedReader reader = new BufferedReader(new FileReader(filePath));

			String line;

			while ((line = reader.readLine()) != null) {
				if(!line.isEmpty()) {
					line = line.replaceAll("\t", "");
					String[] splitLine = line.split("\\|");
					writer.println(splitLine[0] + "\t" + splitLine[1]);
				}
			}

			reader.close();
			writer.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param filePath
	 */
	private static void compressTaxonomyNamesFile(String filePath){

		try {
			PrintWriter writer = new PrintWriter(FilesUtils.getNcbiTaxonomyNamesFilePath(), "UTF-8");
			BufferedReader reader = new BufferedReader(new FileReader(filePath));

			String line;

			while ((line = reader.readLine()) != null) {
				if(line.contains("scientific name")) {
					line = line.replaceAll("\t", "");
					String[] splitLine = line.split("\\|");
					writer.println(splitLine[0] + "\t" + splitLine[1]);
				}
			}

			reader.close();
			writer.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}
