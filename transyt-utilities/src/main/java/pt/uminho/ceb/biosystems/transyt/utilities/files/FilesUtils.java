package pt.uminho.ceb.biosystems.transyt.utilities.files;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilesUtils {

	private static final Logger logger = LoggerFactory.getLogger(FilesUtils.class);

	public static String generateFileName(String name, String extension) {

		LocalDateTime currentTime = LocalDateTime.now(); 

		name = name + "_" + currentTime.getHour() + "h" + currentTime.getMinute() + "m" + currentTime.getSecond() + "s" 
				+ currentTime.getDayOfMonth() + currentTime.getMonthValue() + currentTime.getYear() + extension;

		return name;

	}

	public static String generateFolderName(String name) {

		LocalDateTime currentTime = LocalDateTime.now(); 

		name = name + "_" + currentTime.getHour() + "h" + currentTime.getMinute() + "m" + currentTime.getSecond() + "s" 
				+ currentTime.getDayOfMonth() + currentTime.getMonthValue() + currentTime.getYear() + "/";

		return name;

	}
	
	public static String generateNewVersionStamp(String currentVersion) {

		Integer newVersion = null;
		String version = null;
		
		if(currentVersion != null) {
			version = currentVersion.split("_")[0];
			newVersion = Integer.valueOf(version) + 1;
		}
		
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
		
		if(version == null)
			version = "1.0_" + timeStamp;
		else
			version = newVersion + ".0_" + timeStamp;

		return version;
	}

	/**
	 * Save page in file format
	 * 
	 * @param from
	 * @param to
	 * @throws IOException
	 */
	public static void webPageSaver(InputStream from, OutputStream to) throws IOException {
		byte[] buffer = new byte[4096];
		while (true) {
			int numBytes = from.read(buffer);
			if (numBytes == -1) {
				break;
			}
			to.write(buffer, 0, numBytes);
		}
	}

	/**
	 * Save words in a given file;
	 * 
	 * @param path
	 * @return
	 */
	public static void saveWordsInFile(String path, Set<String> words){

		try {

			PrintWriter writer = new PrintWriter(path, "UTF-8");

			for(String word : words)
				writer.println(word);

			writer.close();

		} 
		catch (FileNotFoundException e) {
			e.printStackTrace();

		} 
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Save a word in a given file;
	 * 
	 * @param path
	 * @return
	 */
	public static void saveWordInFile(String path, String word){

		try {

			PrintWriter writer = new PrintWriter(path, "UTF-8");

			if(word != null)
				writer.println(word);

			writer.close();

		} 
		catch (FileNotFoundException e) {
			e.printStackTrace();

		} 
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Reads words in a given file;
	 * 
	 * @param path
	 * @return
	 */
	public static Set<String> readWordsInFile(String path){

		Set<String> words = new HashSet<>();

		try {

			BufferedReader reader = new BufferedReader(new FileReader(path));

			String line;

			while ((line = reader.readLine()) != null) {

				if(!line.isEmpty() &&  !line.contains("**"))
					words.add(line);
			}

			reader.close();

		} 
		catch (IOException e) {
			e.printStackTrace();
		}

		return words;
	}

	/**
	 * Reads words in a given file;
	 * 
	 * @param path
	 * @return
	 */
	public static List<String> readListWordsInFile(String path){

		List<String> words = new ArrayList<>();

		try {

			BufferedReader reader = new BufferedReader(new FileReader(path));

			String line;

			while ((line = reader.readLine()) != null) {

				if(!line.isEmpty() &&  !line.contains("**")) {
					words.add(line.trim());
				}
			}

			reader.close();

		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		return words;
	}

	/**
	 * Reads words in a given file;
	 * 
	 * @param path
	 * @return
	 */
	public static Map<String, String[]> readConjugatePairsFile(String path){

		Map<String, String[]> pairs = new HashMap<>();

		try {
			BufferedReader reader = new BufferedReader(new FileReader(path));

			String line;

			while ((line = reader.readLine()) != null) {

				if(!line.isEmpty() && !line.contains("#")) {
					
					String[] splitLine = line.strip().split("\\t");
					String[] object = new String[6];
					
					for(int i = 0; i < 6; i++) {
						
						String text = "";
						
						int index = i + 1;
						
						if(i < 2 || splitLine.length > 3)
							text = splitLine[index].replaceAll("\"", "");
						
						object[i] = text;
					}
					
					
					pairs.put(splitLine[0], object);
				}
			}

			reader.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		return pairs;
	}
	
	/**
	 * @param path
	 * @param map
	 */
	public static void saveMapInFile(String path, Map<String, String> map) {

		try {

			PrintWriter writer = new PrintWriter(path, "UTF-8");

			for(String key : map.keySet())
				writer.println(">" + key  + "\t" + map.get(key));
				writer.println();

			writer.close();

		} 
		catch (Exception e) {
			e.printStackTrace();

		} 

	}

	/**
	 * @param path
	 * @param map
	 */
	public static void saveMapInFile2(String path, Map<Integer, String> map) {

		try {

			PrintWriter writer = new PrintWriter(path, "UTF-8");

			for(Integer key : map.keySet())
				writer.println(key  + "\t" + map.get(key));


			writer.close();

		} 
		catch (FileNotFoundException e) {
			e.printStackTrace();

		} 
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param path
	 * @param map
	 */
	public static void saveMapInFile3(String path, Map<String, Set<String>> map) {

		try {

			PrintWriter writer = new PrintWriter(path, "UTF-8");

			for(String key : map.keySet())
				writer.println(key  + "\t" + map.get(key));


			writer.close();

		} 
		catch (FileNotFoundException e) {
			e.printStackTrace();

		} 
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param path
	 * @return
	 */
	public static Map<String, String> readMapFromFile(String path){

		Map<String, String> dic = new HashMap<>();

		try {
			BufferedReader br = new BufferedReader(new FileReader(path));

			String line = br.readLine();

			int i = 0;
			while (line != null) {
				i++;

				if(!line.trim().isEmpty()){

					//					System.out.println(line);

					try {
						String[] content = line.split("\t");

						if(content.length > 1)
							dic.put(content[0].trim(), content[1].trim());
					} 
					catch (Exception e) {
						System.out.println(path);
						System.out.println(line);
						System.out.println("line = " + i);
						e.printStackTrace();
					}
				}

				line = br.readLine();
			}

			br.close();

		} 
		catch(Exception e) {
			//			e.printStackTrace();

			logger.warn("File not found, a new file will be writen at this location: {}", path);

			return new HashMap<>();
		}
		return dic;
	}

	/**
	 * @param path
	 * @return
	 */
	public static Map<String, String> readMapFromFileDELETEME(String path){

		Map<String, String> dic = new HashMap<>();

		try {
			BufferedReader br = new BufferedReader(new FileReader(path));

			String line = br.readLine();

			int i = 0;
			while (line != null) {
				i++;

				if(!line.trim().isEmpty()){

					//					System.out.println(line);

					try {
						String[] content = line.split("\t");

						String reaction = "";

						for(int k = 1; k < content.length; k++)
							reaction = reaction.concat("  ").concat(content[k]);

						if(content.length > 1)
							dic.put(content[0].trim(), reaction);
					} 
					catch (Exception e) {
						System.out.println(path);
						System.out.println(line);
						System.out.println("line = " + i);
						e.printStackTrace();
					}
				}

				line = br.readLine();
			}

			br.close();

		} 
		catch(Exception e) {
			//			e.printStackTrace();

			logger.warn("File not found, a new file will be writen at this location: {}", path);

			return new HashMap<>();
		}
		return dic;
	}

	/**
	 * @param path
	 * @return
	 */
	public static Map<String, Integer> readGenerationsLimitFile(String path){

		Map<String, Integer> dic = new HashMap<>();

		try {
			BufferedReader br = new BufferedReader(new FileReader(path));

			String line = br.readLine();

			int i = 0;
			while (line != null) {
				i++;

				if(!line.trim().isEmpty() && !line.contains("#")){

					try {
						String[] content = line.split("\\$");

						if(content.length > 1)
							dic.put(content[0].trim(), Integer.valueOf(content[1].trim()));
					} 
					catch (Exception e) {
						System.out.println(path);
						System.out.println(line);
						System.out.println("line = " + i);
						e.printStackTrace();
					}
				}

				line = br.readLine();
			}

			br.close();

		} 
		catch(Exception e) {
			//			e.printStackTrace();

			logger.warn("Generations file not found at this location: {}", path);

			return new HashMap<>();
		}
		return dic;
	}

	/**
	 * @param path
	 * @return
	 */
	public static Map<Integer, String> readMapFromFile3(String path){

		Map<Integer, String> dic = new HashMap<>();

		try {
			BufferedReader br = new BufferedReader(new FileReader(path));

			String line = br.readLine();

			int i = 0;
			while (line != null) {
				i++;

				if(!line.trim().isEmpty() && !line.contains("#")){

					try {
						String[] content = line.split("\t");

						if(content.length > 1)
							dic.put(Integer.valueOf(content[0].trim()), content[1].trim());
					} 
					catch (Exception e) {
						System.out.println(path);
						System.out.println(line);
						System.out.println("line = " + i);
						e.printStackTrace();
					}
				}

				line = br.readLine();
			}

			br.close();

		} 
		catch(Exception e) {
			//			e.printStackTrace();

			logger.warn("Generations file not found at this location: {}", path);

			return new HashMap<>();
		}
		return dic;
	}
	
	/**
	 * @param path
	 * @return
	 */
	public static Map<Integer, Integer> readMapFromFile4(String path){ //this read methods can probably be generic, no time to do it now

        Map<Integer, Integer> dic = new HashMap<>();

        try {
            BufferedReader br = new BufferedReader(new FileReader(path));

            String line = br.readLine();

            int i = 0;
            while (line != null) {
                i++;

                if(!line.trim().isEmpty() && !line.contains("#")){

                    try {
                        String[] content = line.split("\t");

                        if(content.length > 1)
                            dic.put(Integer.valueOf(content[0].trim()), Integer.valueOf(content[1].trim()));
                    }
                    catch (Exception e) {
                        System.out.println(path);
                        System.out.println(line);
                        System.out.println("line = " + i);
                        e.printStackTrace();
                    }
                }
                line = br.readLine();
            }
            br.close();

        }
        catch(Exception e) {
            //       e.printStackTrace();

            logger.warn("Generations file not found at this location: {}", path);

            return new HashMap<>();
        }
        return dic;
    }

	/**
	 * Returns TranSyT current root directory.
	 * 
	 * @return
	 */
	public static String getRootDirectory() {

		File file = new File(System.getProperty("workdir"));
		return file.getAbsolutePath().concat("/");

	}

	/**
	 * Returns TranSyT current configurations directory.
	 * 
	 * @return
	 */
	public static String getTransytSystemDirectory() {

		File file = new File(getRootDirectory().concat("system/"));

		file.mkdir();

		return file.getAbsolutePath().concat("/");

	}

	/**
	 * Returns TranSyT current root directory for scraper project.
	 * 
	 * @return
	 */
	public static String getScraperProjectDirectory() {

		File file = new File(getRootDirectory().concat("scraper/"));

		file.mkdir();

		return file.getAbsolutePath().concat("/");
	}

	/**
	 * Returns TranSyT current configurations directory.
	 * 
	 * @return
	 */
	public static String getDictionatiesAndConfigurationsDirectory() {

		File file = new File(getTransytSystemDirectory().concat("dictionary_and_configurations/"));

		file.mkdir();

		return file.getAbsolutePath().concat("/");

	}

	/**
	 * Returns TranSyT current configurations directory.
	 * 
	 * @return
	 */
	public static String getBiosynthDatabaseDirectory() {

		File file = new File(getServiceProjectDirectory().concat("biosynthdb/"));

		file.mkdir();

		return file.getAbsolutePath().concat("/");

	}

	/**
	 * Returns TranSyT current configurations directory.
	 * 
	 * @return
	 */
	public static String getTranSyTDBDatabaseDirectory() {

		File file = new File(getServiceProjectDirectory().concat("transytdb/"));

		file.mkdir();

		return file.getAbsolutePath().concat("/");

	}

	//	/**
	//	 * Returns TranSyT current configurations directory.
	//	 * 
	//	 * @return
	//	 */
	//	public static String getBlastDirectory() {
	//		
	//		File file = new File(getServiceProjectDirectory().concat("similarities/blast"));
	//		
	//		file.mkdirs();
	//		
	//		return file.getAbsolutePath().concat("/");
	//		
	//	}

	//	/**
	//	 * Returns TranSyT current root directory for utilities project.
	//	 * 
	//	 * @return
	//	 */
	//	public static String getUtilitiesProjectDirectory() {
	//		
	//		File file = new File(getRootDirectory().concat("utilities/"));
	//		
	//		file.mkdir();
	//		
	//		return file.getAbsolutePath().concat("/");
	//	}

	/**
	 * Returns TranSyT current root directory for service project.
	 * 
	 * @return
	 */
	public static String getMetabolitesNamesMatcherFilesDirectory() {

		File file = new File(getServiceProjectDirectory().concat("names_Matcher/"));

		if(!file.exists())
			file.mkdir();

		return file.getAbsolutePath().concat("/");
	}

	/**
	 * Returns TranSyT current root directory for service project.
	 * 
	 * @return
	 */
	public static String getServiceProjectDirectory() {

		File file = new File(getRootDirectory().concat("service/"));

		file.mkdir();

		return file.getAbsolutePath().concat("/");
	}

	/**
	 * Returns TranSyT current temporary files directory.
	 * 
	 * @return
	 */
	public static String getTempDirectory() {

		File file = new File(getScraperProjectDirectory().concat("temp/"));

		file.mkdir();

		return file.getAbsolutePath().concat("/");
	}

	/**
	 * Returns TranSyT current backup files directory.
	 * 
	 * @return
	 */
	public static String getBackupFilesDirectory() {

		File file = new File(getScraperProjectDirectory().concat("backup_files/"));

		file.mkdir();

		return file.getAbsolutePath().concat("/");
	}

	/**
	 * Returns TranSyT current backup files directory.
	 * 
	 * @return
	 */
	public static String getBackupQueriesFilesDirectory() {

		File file = new File(getBackupFilesDirectory().concat("queries/"));

		file.mkdir();

		return file.getAbsolutePath().concat("/");
	}

	/**
	 * Returns TCDB fasta backup files directory.
	 * 
	 * @return
	 */
	public static String getBackupTCDBFASTADirectory() {

		File file = new File(getBackupFilesDirectory().concat("TCDB_public_fasta/"));

		file.mkdir();

		return file.getAbsolutePath().concat("/");
	}

	/**
	 * Returns TCDB scraped summary information history directory.
	 * 
	 * @return
	 */
	public static String getBackupTCDBSystemsDirectory() {

		File file = new File(getBackupFilesDirectory().concat("TCDB_systems_summary/"));

		file.mkdir();

		return file.getAbsolutePath().concat("/");
	}

	/**
	 * Returns TranSyT taxonomic backup files directory.
	 * 
	 * @return
	 */
	public static String getTaxonomicFilesDirectory() {

		File file = new File(getTransytSystemDirectory().concat("taxonomic_files/"));

		file.mkdir();

		return file.getAbsolutePath().concat("/");
	}

	/**
	 * Returns TranSyT current backup files directory.
	 * 
	 * @return
	 */
	public static String getReactionsBuilderFilesDirectory() {

		File file = new File(getScraperProjectDirectory().concat("reactions_Builder/"));

		if(!file.exists())
			file.mkdir();

		return file.getAbsolutePath().concat("/");
	}

	/**
	 * Returns TranSyT current family specific reactions files directory.
	 * 
	 * @return
	 */
	public static String getFamilySpecificFilesDirectory() {

		File file = new File(getScraperProjectDirectory().concat("Family_Specific_Reactions/"));

		file.mkdir();

		return file.getAbsolutePath().concat("/");
	}

	/**
	 * Returns TranSyT current family specific reactions files directory.
	 * 
	 * @return
	 */
	public static String getMetacycReactionsFilesDirectory() {

		File file = new File(getScraperProjectDirectory().concat("MetaCyc_Reactions/"));

		file.mkdir();

		return file.getAbsolutePath().concat("/");
	}
	
	/**
	 * Returns TranSyT current family specific reactions files directory.
	 * 
	 * @return
	 */
	public static String getModelseedReactionsFilesDirectory() {

		File file = new File(getScraperProjectDirectory().concat("ModelSEED_Reactions/"));

		file.mkdir();

		return file.getAbsolutePath().concat("/");
	}
	
	/**
	 * Returns TranSyT current family specific reactions files directory.
	 * 
	 * @return
	 */
	public static String getModelseedCompoundsFilesDirectory() {

		File file = new File(getScraperProjectDirectory().concat("ModelSEED_Compounds/"));

		file.mkdir();

		return file.getAbsolutePath().concat("/");
	}
	
	/**
	 * Returns TranSyT's directory containing KEGG data.
	 * 
	 * @return
	 */
	public static String getKeggDataDirectory() {

		File file = new File(getScraperProjectDirectory().concat("KEGG/"));

		file.mkdir();

		return file.getAbsolutePath().concat("/");
	}
	
	/**
	 * Returns TranSyT's directory containing NCBI data.
	 * 
	 * @return
	 */
	public static String getNcbiDataDirectory() {

		File file = new File(getScraperProjectDirectory().concat("NCBI/"));

		file.mkdir();

		return file.getAbsolutePath().concat("/");
	}
	
	/**
	 * Returns TranSyT's directory containing taxonomy nodes data.
	 * 
	 * @return
	 */
	public static String getNcbiTaxonomyRelationshipsFilePath() {

		return getNcbiDataDirectory().concat("taxonomy_nodes.txt");
	}
	
	/**
	 * Returns TranSyT's directory containing taxonomy names.
	 * 
	 * @return
	 */
	public static String getNcbiTaxonomyNamesFilePath() {

		return getNcbiDataDirectory().concat("taxonomy_names.txt");
	}
	
	/**
	 * Returns TranSyT's directory containing TranSyT ontologies data. This directory contains files used by 
	 * TranSyT's KBase tool for TC number ontologies search.
	 * 
	 * @return
	 */
	public static String getOntologiesDataDirectory() {

		File file = new File(getScraperProjectDirectory().concat("ontologies/"));

		file.mkdir();

		return file.getAbsolutePath().concat("/");
	}
	
	/**
	 * Returns TranSyT's directory containing KEGG built fasta files.
	 * 
	 * @return
	 */
	public static String getKeggFastaDirectory() {

		File file = new File(getKeggDataDirectory().concat("fasta_files/"));

		file.mkdir();

		return file.getAbsolutePath().concat("/");
	}

	/**
	 * Returns TranSyT current family specific reactions files directory.
	 * 
	 * @return
	 */
	public static String getTCDBFastaForAlignmentDirectory() {

		File file = new File(getScraperProjectDirectory().concat("TCDB_Fasta_alignment_files/"));

		file.mkdir();

		return file.getAbsolutePath().concat("/");
	}

	/**
	 * Returns TranSyT current system specific reactions files directory.
	 * 
	 * @return
	 */
	public static String getSystemSpecificFilesDirectory() {

		File file = new File(getScraperProjectDirectory().concat("System_Specific_Reactions/"));

		file.mkdir();

		return file.getAbsolutePath().concat("/");
	}

	/**
	 * Returns TranSyT current backup files directory.
	 * 
	 * @return
	 */
	public static String getBackupFamilyDescriptionsFilesDirectory() {

		File file = new File(getBackupFilesDirectory().concat("family_descriptions/"));

		file.mkdir();

		return file.getAbsolutePath().concat("/");
	}

	/**
	 * Method to return the path of the file containing all families to be excluded from the annotation process.
	 * 
	 * @return
	 */
	public static String getFamiliesExclusionFile() {

		return getDictionatiesAndConfigurationsDirectory().concat("Families_exclusion.txt");

	}
	
	/**
	 * Method to return the path of the file containing all KOs to be searched in KEGG.
	 * 
	 * @return
	 */
	public static String getKOsToSearchFilePath() {

		return getDictionatiesAndConfigurationsDirectory().concat("SearchKO.txt");

	}

	/**
	 * Method to return the path of the file containing given relationships missing in MetaCyc.
	 * 
	 * @return
	 */
	public static String getChildsRelationshipsMissingFilePath() {

		return getDictionatiesAndConfigurationsDirectory().concat("ChildsMissingRelationship.txt");

	}

	/**
	 * Returns TranSyT current icons directory.
	 * 
	 * @return
	 */
	public static String getIconsDirectory() {

		File file = new File(getTransytSystemDirectory().concat("icons/"));

		if(!file.exists()) {
			logger.error("System folder icons not found! Creating a new folder! Files missing...");
			file.mkdir();
		}

		return file.getAbsolutePath().concat("/");
	}



	/**
	 * @param path
	 * @return
	 */
	public static Map<String, String> readPropertiesFile(boolean defaultValues){

		String path = getTransytSystemDirectory().concat("configurations.txt");

		if(defaultValues)
			path = getTransytSystemDirectory().concat("default_configurations.txt");

		Map<String, String> dic = new HashMap<>();

		logger.info("Configuration file path {}", path);

		try {
			BufferedReader br = new BufferedReader(new FileReader(path));

			String line = br.readLine();

			int i = 0;
			while (line != null) {
				i++;

				if(!line.trim().isEmpty() && !line.contains("#")){

					try {
						String[] content = line.split("=");

						if(content.length > 1)
							dic.put(content[0].trim(), content[1].trim());
					} 
					catch (Exception e) {

						logger.warn("Error while reading configuration file on line {}", i);
						e.printStackTrace();
					}
				}

				line = br.readLine();
			}

			br.close();

		} 
		catch(Exception e) {
			//			e.printStackTrace();

			logger.warn("Properties file not found at this location: {}", path);

			return new HashMap<>();
		}
		return dic;
	}

	/**
	 * Save the last known version in file.
	 * 
	 * @param path
	 * @throws IOException
	 */
	public static void saveLastKnownVersion(String path, String text){

		File file = new File(text);

		BufferedWriter writer;

		try {

			writer = new BufferedWriter(new FileWriter(path, true));
			writer.append(file.getName());
			writer.newLine();
			writer.flush();
			writer.close();

		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Add contents of a map to an already existing file.
	 * 
	 * @param path
	 * @param content
	 */
	public static void addMapToFile(String path, Map<String, String> content){
		try {

			BufferedWriter writer = new BufferedWriter(new FileWriter(path, true));

			for(String key : content.keySet()) {
				writer.append(key  + "\t" + content.get(key));
				writer.newLine();
			}

			writer.flush();
			writer.close();

		} 
		catch (FileNotFoundException e) {
			e.printStackTrace();

		} 
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get the path of the last known version in the log file.
	 * 
	 * @param path
	 * @throws IOException
	 */
	public static String getLastKnownVersion(String path){

		BufferedReader reader;

		String word = null;

		try {

			reader = new BufferedReader(new FileReader(path));

			String currentLine;

			while (( currentLine = reader.readLine()) != null) {

				if(!currentLine.isEmpty())
					word = currentLine;

			}

			word = new File(path).getParent().concat("/").concat(word);

		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return word;
	}

	/**
	 * Create an hash of a given file.
	 * 
	 * @param digest
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static String getFileChecksum(MessageDigest digest, File file) throws IOException{
		//Get file input stream for reading the file content
		FileInputStream fis = new FileInputStream(file);

		//Create byte array to read data in chunks
		byte[] byteArray = new byte[1024];
		int bytesCount = 0;

		//Read file data and update in message digest
		while ((bytesCount = fis.read(byteArray)) != -1) {
			digest.update(byteArray, 0, bytesCount);
		};

		//close the stream; We don't need it now.
		fis.close();

		//Get the hash's bytes
		byte[] bytes = digest.digest();

		//This bytes[] has bytes in decimal format;
		//Convert it to hexadecimal format
		StringBuilder sb = new StringBuilder();
		for(int i=0; i< bytes.length ;i++)
		{
			sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
		}

		//return complete hash
		return sb.toString();
	}

	/**
	 * Reads the dictionary original file.
	 */
	public static Map<String, Set<String>> readDictionary(String path, String separatorSymbol){

		Map<String, Set<String>> dic = new HashMap<>();

		try {
			BufferedReader br = new BufferedReader(new FileReader(path));

			String line = br.readLine();

			while (line != null) {

				if(!line.contains("#") && !line.contains("**") && !line.isEmpty()){
					Set<String> setValues = new HashSet<>();

					String[] content = line.split("\t=");

					String[] values = content[1].split("\\$");

					for(int i = 0; i < values.length; i++) {

						if(!values[i].isEmpty())
							setValues.add(values[i]);
					}

					dic.put(content[0], setValues);
				}

				line = br.readLine();
			}

			br.close();

		} 
		catch(Exception e) {
			e.printStackTrace();
		}
		return dic;
	}
}
