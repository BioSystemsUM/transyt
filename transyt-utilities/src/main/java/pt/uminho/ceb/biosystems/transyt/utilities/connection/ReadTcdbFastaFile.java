package pt.uminho.ceb.biosystems.transyt.utilities.connection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import pt.uminho.ceb.biosystems.transyt.utilities.capsules.FastaTcdb;
import pt.uminho.ceb.biosystems.transyt.utilities.files.FilesUtils;

public class ReadTcdbFastaFile {

	private static final String path = "C:\\Users\\Davide\\Documents\\TriageData\\";
	private static final String pathLastVersionKnown = "C:\\Users\\Davide\\Documents\\TriageData\\tcdbLastKnownVersion.log";
	private static final String fileName = "tcdbFasta";

	/**
	 * Read and get online fasta file from tcdb if available. Local tcdb file otherwise.
	 * 
	 * @return
	 */
	public static Map<String, FastaTcdb> readfasta(boolean latest) {

		try {
			
			Map<String, FastaTcdb> fastaMap = new HashMap<>();

			BufferedReader in = getTcdbFasta(latest);
			
			String html;
			
			String sequence = "";
			
			String accession = "", tcNumber = "", organism = "", description = "";
			
//			Map<String, Integer> distributions1 = new TreeMap<>();
//			Map<String, Integer> distributions2 = new TreeMap<>();
//			Map<String, Integer> distributions3 = new TreeMap<>();
//			Map<String, Integer> distributions4 = new TreeMap<>();
//			Map<String, Integer> distributions5 = new TreeMap<>();
			
			
			boolean firstTime = true;

			while ((html = in.readLine()) != null){
				
				Document doc = Jsoup.parse(html);
				String text = doc.body().text().trim();
				
				if(text.contains(">")) {
					
					if(!firstTime) {
						
						fastaMap.put(accession.concat("@").concat(tcNumber), new FastaTcdb(accession, sequence, tcNumber, organism, description));
						
					}
					firstTime = false;
					
					String[] header = text.split("\\|");

					accession = header[2];
					
					String[] subSubHeader = null;
					
					if(header[3].contains("\\[")) {
					
						String[] subHeader = header[3].split("\\[");
						
						organism = subHeader[1].replaceAll("\\]", "");
						
						subSubHeader = subHeader[0].split("\\s+");
						
						tcNumber = subSubHeader[0];
						
					}
					else {
						
						organism = null;
						
						subSubHeader = header[3].split("\\s+");
						
						tcNumber = subSubHeader[0];
						
					}
					
					//////tcdb tcNumber distributions
					
//					if(distributions5.containsKey(tcNumber)) {
//						
//						int count = distributions5.get(tcNumber);
//						distributions5.put(tcNumber, count+1);
//					}
//						
//					else {
//						distributions5.put(tcNumber, 1);
//					}
//					
////					System.out.println("tc--" + tcNumber);
//					
//					String[] newTcNumber = tcNumber.split("\\."); 
//					
////					System.out.println(newTcNumber.length);
//					
//					String newTc = newTcNumber[0].concat(".").concat(newTcNumber[1]).concat(".").concat(newTcNumber[2]).concat(".").concat(newTcNumber[3]);
//					
//					if(distributions4.containsKey(newTc)) {
//						
//						int count = distributions4.get(newTc);
//						distributions4.put(newTc, count+1);
//					}
//						
//					else
//						distributions4.put(newTc, 1);
//					
//					newTc = newTcNumber[0].concat(".").concat(newTcNumber[1]).concat(".").concat(newTcNumber[2]);
//					
//					if(distributions3.containsKey(newTc)) {
//						
//						int count = distributions3.get(newTc);
//						distributions3.put(newTc, count+1);
//					}
//						
//					else
//						distributions3.put(newTc, 1);
//					
//					newTc = newTcNumber[0].concat(".").concat(newTcNumber[1]);
//					
//					if(distributions2.containsKey(newTc)) {
//						
//						int count = distributions2.get(newTc);
//						distributions2.put(newTc, count+1);
//					}
//						
//					else
//						distributions2.put(newTc, 1);
//					
//					newTc = newTcNumber[0];
//					
//					if(distributions1.containsKey(newTc)) {
//						
//						int count = distributions1.get(newTc);
//						distributions1.put(newTc, count+1);
//					}
//						
//					else
//						distributions1.put(newTc, 1);
					
					
					description = "";
					
					for(int i = 1; i < subSubHeader.length; i++)
						description = description.concat(subSubHeader[i]).concat(" ");
					
					sequence = "";
				}
				else {
					sequence = sequence.concat(text);
				}
			}
			
			fastaMap.put(accession.concat("@").concat(tcNumber), new FastaTcdb(accession, sequence, tcNumber, organism, description));
			
//			System.out.println("groups: " + distributions3.size());
//			
//			for(String key : distributions3.keySet())
//				System.out.println(key + "\t" +distributions3.get(key));
			
			return fastaMap;
		} 
		catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Get html page or get file if connection is not available
	 * @return 
	 * 
	 * @return
	 * @throws FileNotFoundException 
	 */
	public static BufferedReader getTcdbFasta(boolean latest) throws FileNotFoundException {

		try {

			String filePath = path + FilesUtils.generateFileName(fileName, ".txt");

			OutputStream out = new FileOutputStream(filePath);

			LinkConnection conn = new LinkConnection();
			
			if(conn.getCodeConnection("http://www.tcdb.org/public/tcdb") == 200 && latest) {

				webPageSaver(conn.getPageOpenStream(), out);

				saveLastKnownVersion(filePath);

				return conn.getPage();

			}
			else {
				
				String lastFilePath = getLastKnownVersion();

				return new BufferedReader(new FileReader(lastFilePath));
				
			}
		}
		 catch (Exception e) {
			e.printStackTrace();
			
			String lastFilePath = getLastKnownVersion();

			return new BufferedReader(new FileReader(lastFilePath));
		}
	}
	
	/**
	 * Save page in file format
	 * 
	 * @param from
	 * @param to
	 * @throws IOException
	 */
	private static void webPageSaver(InputStream from, OutputStream to) throws IOException {
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
	 * Save the last known version in file.
	 * 
	 * @param path
	 * @throws IOException
	 */
	private static void saveLastKnownVersion(String path){

		BufferedWriter writer;

		try {

			writer = new BufferedWriter(new FileWriter(pathLastVersionKnown, true));
			writer.append(path);
			writer.newLine();
			writer.flush();
			writer.close();

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
	public static String getLastKnownVersion(){

		BufferedReader reader;

		String word = null;
		
		try {

			reader = new BufferedReader(new FileReader(pathLastVersionKnown));
			
			String currentLine;
			
			while (( currentLine = reader.readLine()) != null) {
		        
				if(!currentLine.isEmpty())
					word = currentLine;
				
		    }

		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return word;
	}

}
