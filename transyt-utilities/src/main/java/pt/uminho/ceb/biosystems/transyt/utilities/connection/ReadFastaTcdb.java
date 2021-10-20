package pt.uminho.ceb.biosystems.transyt.utilities.connection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.biojava.nbio.core.sequence.ProteinSequence;
import org.biojava.nbio.core.sequence.compound.AminoAcidCompound;
import org.biojava.nbio.core.sequence.compound.AminoAcidCompoundSet;
import org.biojava.nbio.core.sequence.io.FastaReader;
import org.biojava.nbio.core.sequence.io.GenericFastaHeaderParser;
import org.biojava.nbio.core.sequence.io.ProteinSequenceCreator;
import org.biojava.nbio.core.sequence.template.AbstractSequence;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import pt.uminho.ceb.biosystems.transyt.utilities.capsules.FastaTcdb;
import pt.uminho.ceb.biosystems.transyt.utilities.files.FilesUtils;
import pt.uminho.ceb.biosystems.transyt.utilities.files.ReadFasta;



public class ReadFastaTcdb {

	private static final String PATH_LAST_KNOWN_VERSION_PUBLIC_FASTA = FilesUtils.getBackupTCDBFASTADirectory().concat("tcdbLastKnownVersion.log");
	private static final String PATH_LAST_KNOWN_VERSION_ALIGN_FASTA = FilesUtils.getTCDBFastaForAlignmentDirectory().concat("tcdbLastKnownVersion.log");

	private static final String PUBLIC_FASTA_TCDB_NAME = "tcdbFasta";
	private static final String FASTA_FOR_ALIGNMENT_NAME = "tcdb_align_fasta";

	/**
	 * Read and get online fasta file from tcdb if available. Local tcdb file otherwise.
	 * 
	 * @return
	 */
	public static Set<String> readTcNumbersFromFasta(boolean useCache) {

		try {

			Map<String, FastaTcdb> fastaMap = new HashMap<>();

			BufferedReader in = getTcdbFasta(useCache);

			Set<String> accessions = new HashSet<>();

			String html;

			String sequence = "";

			String accession = "", tcNumber = "", organism = "", description = "";

			//			Map<String, Integer> distributions1 = new TreeMap<>();
			//			Map<String, Integer> distributions2 = new TreeMap<>();
			//			Map<String, Integer> distributions3 = new TreeMap<>();
			//			Map<String, Integer> distributions4 = new TreeMap<>();
			Map<String, Integer> distributions5 = new TreeMap<>();


			boolean firstTime = true;

			while ((html = in.readLine()) != null){

				Document doc = Jsoup.parse(html);
				String text = doc.body().text().trim();

				if(text.contains(">")) {

					if(!firstTime) {
						fastaMap.put(accession.concat("_").concat(tcNumber), new FastaTcdb(accession, sequence, tcNumber, organism, description));
						accessions.add(accession);

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

					if(distributions5.containsKey(tcNumber)) {

						int count = distributions5.get(tcNumber);
						distributions5.put(tcNumber, count+1);
					}

					else {
						distributions5.put(tcNumber, 1);
					}

					description = "";

					for(int i = 1; i < subSubHeader.length; i++)
						description = description.concat(subSubHeader[i]).concat(" ");

					sequence = "";
				}
				else {
					sequence = sequence.concat(text);
				}
			}

			accessions.add(accession);

			fastaMap.put(accession.concat("_").concat(tcNumber), new FastaTcdb(accession, sequence, tcNumber, organism, description));

			return distributions5.keySet();
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
	public static BufferedReader getTcdbFasta(boolean useCache) throws FileNotFoundException {

		try {

			if(!useCache) {
				String filePath = FilesUtils.getBackupTCDBFASTADirectory().concat(FilesUtils.generateFileName(PUBLIC_FASTA_TCDB_NAME, ".faa"));

				OutputStream out = new FileOutputStream(filePath);

				LinkConnection conn = new LinkConnection();

				if(conn.getCodeConnection(TcdbExplorer.TCDB_FASTA_URL) == 200) {

					conn.webPageSaver(conn.getPageOpenStream(), out);

					FilesUtils.saveLastKnownVersion(PATH_LAST_KNOWN_VERSION_PUBLIC_FASTA, filePath);

					return conn.getPage();

				}

				out.close();
			}

			String lastFilePath = getPathFastaLastKnownVersion();

			return new BufferedReader(new FileReader(lastFilePath));
		}
		catch (Exception e) {
			e.printStackTrace();

			String lastFilePath = getPathFastaLastKnownVersion();

			return new BufferedReader(new FileReader(lastFilePath));
		}
	}

	/**
	 * Method to find to path to the last known version retrieved of the public fasta from TCDB.
	 * 
	 * @return
	 */
	public static String getPathFastaLastKnownVersion() {

		return FilesUtils.getLastKnownVersion(PATH_LAST_KNOWN_VERSION_PUBLIC_FASTA);

	}
	
	/**
	 * Method to find to path to the last known version of the fasta generated for alignment.
	 * 
	 * @return
	 */
	public static String getPathFastaForAlignmentLastKnownVersion() {

		return FilesUtils.getLastKnownVersion(PATH_LAST_KNOWN_VERSION_ALIGN_FASTA);

	}

	/**
	 * Read fasta file and retrieve all information
	 * 
	 * @return
	 */
	public static Map<String, FastaTcdb> readfasta(boolean useCache) {

		try {

			Map<String, FastaTcdb> fastaMap = new HashMap<>();

			BufferedReader in = getTcdbFasta(useCache);

			Set<String> accessions = new HashSet<>();

			String html;

			String sequence = "";

			String accession = "", tcNumber = "", organism = "", description = "";

			boolean firstTime = true;

			while ((html = in.readLine()) != null){

				Document doc = Jsoup.parse(html);
				String text = doc.body().text().trim();

				if(text.contains(">")) {

					if(!firstTime) {
						fastaMap.put(accession.concat("@").concat(tcNumber), new FastaTcdb(accession, sequence, tcNumber, organism, description));
						accessions.add(accession);

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

					description = "";

					for(int i = 1; i < subSubHeader.length; i++)
						description = description.concat(subSubHeader[i]).concat(" ");

					sequence = "";
				}
				else {
					sequence = sequence.concat(text);
				}
			}

			accessions.add(accession);

			fastaMap.put(accession.concat("@").concat(tcNumber), new FastaTcdb(accession, sequence, tcNumber, organism, description));

			return fastaMap;
		} 
		catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Method to pre-build the fasta file that will be used in the alignments excluding families set in config file.
	 * 
	 * @param path
	 * @param sequences
	 */
	public static void buildFastaFileForAlignments(String keggFastaPath){

		try {
			Map<String, AbstractSequence<?>> tcdbGenes = getTcdbInMapFormat();
			Map<String, String> keggSequences = ReadFasta.getGenomeMapFormat(keggFastaPath);

			Set<String> familiesExcluded = FilesUtils.readWordsInFile(FilesUtils.getFamiliesExclusionFile());
			
			String filePath = FilesUtils.getTCDBFastaForAlignmentDirectory().concat(FilesUtils.generateFileName(FASTA_FOR_ALIGNMENT_NAME, ".faa"));

			FilesUtils.saveLastKnownVersion(PATH_LAST_KNOWN_VERSION_ALIGN_FASTA, filePath);

			File fastaFile = new File(filePath);

			FileWriter fstream = new FileWriter(fastaFile);  
			BufferedWriter out = new BufferedWriter(fstream); 

			for(String seqID : tcdbGenes.keySet()) {

				AbstractSequence<?> sequence = tcdbGenes.get(seqID);

				String header = sequence.getOriginalHeader();

				if(sequence.getOriginalHeader()==null || sequence.getOriginalHeader().isEmpty())
					header = seqID;

				String tcNumber = header.split("\\|")[3].split("\\s+")[0];

				boolean go = true;

				for(String family : familiesExcluded) {
					
					if(!family.endsWith("."))
						family = family.concat(".");
					
					if(tcNumber.contains(family)) {
						go = false;
						break;
					}
				}

				if(tcNumber.matches("\\d+\\.\\w+\\.\\d+\\.\\d+\\.\\d+") && go) {

					out.write(">" + header + "\n");

					out.write(sequence.getSequenceAsString()+"\n\n");
				}
			}
			
			Map<String, String> filteredSequences = filterDuplicatedSequences(keggSequences);
			
			for(String id : filteredSequences.keySet()) {
				
				out.write(">" + id + "\n");

				out.write(filteredSequences.get(id)+"\n\n");
			}
			
			out.close();
		} 
		catch (Exception e) {

			e.printStackTrace();
		}
	}
	
	/**
	 * @param sequences
	 * @return
	 */
	private static Map<String, String> filterDuplicatedSequences(Map<String, String> sequences){
		
		Map<String, String> res = new HashMap<>();
		
		for(String id : sequences.keySet()) {
			
			String seq = sequences.get(id);
			
			if(!res.values().contains(seq))
				res.put(id, seq);	// assuming that only the sequence is important, not the identifier
		}
		
		return res;
	}

	/**
	 * 
	 * @param url
	 * @return
	 * @throws Exception
	 */
	public static Map<String, AbstractSequence<?>> getTcdbInMapFormat() throws Exception {

		BufferedReader br = ReadFastaTcdb.getTcdbFasta(true);
		StringBuilder sb = new StringBuilder();
		String line;

		while ((line = br.readLine()) != null)
			sb.append(line.concat("\n"));

		String theString = sb.toString().replace("</p>", "").replace("<p>", "").replace(">gnl|TC-DB|xxxxxx 3.A.1.205.14 \ndsfgdfg", "");
		byte[] bytes = theString.getBytes("utf-8");
		InputStream tcdbInputStream =  new ByteArrayInputStream(bytes);

		FastaReader<ProteinSequence,AminoAcidCompound> fastaReader = new FastaReader<ProteinSequence,AminoAcidCompound>(
				tcdbInputStream, 
				new GenericFastaHeaderParser<ProteinSequence,AminoAcidCompound>(), 
				new ProteinSequenceCreator(AminoAcidCompoundSet.getAminoAcidCompoundSet()));


		Map<String, AbstractSequence<?>> tcdb  =  new HashMap<>();
		tcdb.putAll(fastaReader.process());

		return tcdb;
	}

	/**
	 * Method to read all uniprot accessions present in the alignments file.
	 * 
	 * @param useCache
	 * @return
	 */
	public static Set<String> getAccessionForAlignment() {
		
		Set<String> familiesExcluded = FilesUtils.readWordsInFile(FilesUtils.getFamiliesExclusionFile());
		
		Set<String> accessions = new HashSet<>();

		try {

			BufferedReader in = new BufferedReader(new FileReader(getPathFastaLastKnownVersion()));

			String line;

			while ((line = in.readLine()) != null){

				Document doc = Jsoup.parse(line);
				String text = doc.body().text().trim();
				
				String[] header = text.split("\\|");
				
				if(text.contains(">")) {
					
					String tcNumber = text.split("\\|")[3].split("\\s+")[0];

					boolean go = true;

					for(String family : familiesExcluded) {
						
						if(!family.endsWith("."))
							family = family.concat(".");
						
						if(tcNumber.contains(family)) {
							go = false;
							break;
						}
					}
					
					if(go)
						accessions.add(header[2].trim());
				}
			}
			
			in.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}

		return accessions;
	}
}



