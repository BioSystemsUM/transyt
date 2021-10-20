package pt.uminho.ceb.biosystems.transyt.utilities.files;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.biojava.nbio.core.sequence.ProteinSequence;
import org.biojava.nbio.core.sequence.compound.AminoAcidCompound;
import org.biojava.nbio.core.sequence.compound.AminoAcidCompoundSet;
import org.biojava.nbio.core.sequence.io.FastaReader;
import org.biojava.nbio.core.sequence.io.GenericFastaHeaderParser;
import org.biojava.nbio.core.sequence.io.ProteinSequenceCreator;
import org.biojava.nbio.core.sequence.template.AbstractSequence;

import pt.uminho.ceb.biosystems.merlin.bioapis.externalAPI.ncbi.CreateGenomeFile;
import pt.uminho.ceb.biosystems.merlin.bioapis.externalAPI.utilities.Enumerators.FileExtensions;

public class ReadFasta {
	
	/**
	 * @param path
	 * @param sequences
	 */
	public static void buildSubFastaFiles(String filesPath, Map<String, AbstractSequence<?>> allSequences, 
			List<Map<String,AbstractSequence<?>>> queriesSubSetList, List<String> queryFilesPaths, int numberOfFiles){
		
		Map<String, AbstractSequence<?>> queriesSubSet = new HashMap<>();
		
		int batch_size= allSequences.size()/numberOfFiles;
		
		String fastaFileName;
		
		int c=0;
		for (String query : allSequences.keySet()) {
			
			String seqID = query;
			
			if(query.contains(" "))
				seqID = query.split("\\s+")[0];
			
			allSequences.get(query).setOriginalHeader(seqID);
			
			queriesSubSet.put(seqID, allSequences.get(query));

			if ((c+1)%batch_size==0 && ((c+1)/batch_size < numberOfFiles)) {
				
				fastaFileName = filesPath.concat("/SubFastaFile_").concat(Integer.toString((c+1)/batch_size)).concat("_of_").
						concat(Integer.toString(numberOfFiles)).concat(FileExtensions.PROTEIN_FAA.getExtension());
				
				CreateGenomeFile.buildFastaFile(fastaFileName, queriesSubSet);
				queryFilesPaths.add(fastaFileName);
				queriesSubSetList.add(queriesSubSet);
				
				queriesSubSet = new HashMap<>();
			}
			c++;
		}
		
		fastaFileName = filesPath.concat("/SubFastaFile_").concat(Integer.toString(numberOfFiles)).concat("_of_").
				concat(Integer.toString(numberOfFiles)).concat(FileExtensions.PROTEIN_FAA.getExtension());
		
		CreateGenomeFile.buildFastaFile(fastaFileName, queriesSubSet);
		queriesSubSetList.add(queriesSubSet);
		queryFilesPaths.add(fastaFileName);

	}

	/**
	 * 
	 * @param path
	 * @return
	 * @throws Exception
	 */
	public static Map<String, String> getGenomeMapFormat(String path) throws Exception {

		Map<String, String> sequences = new HashMap<>();

		File file = new File(path);
		InputStream inputStream = new DataInputStream(new FileInputStream(file));
		BufferedReader br= new BufferedReader(new InputStreamReader(inputStream));
		String line;

		String id = null;
		String sequence = "";

		while ((line = br.readLine()) != null) {

			if(line.contains(">")) {

				if(id != null) {
					sequences.put(id, sequence.replaceAll("\n", ""));
					sequence = "";
				}
				
				id = line.replace(">", "");
			}
			else {
				sequence = sequence + line;
			}
		}
		
		sequences.put(id, sequence.replaceAll("\n", "")); //last read
		
		br.close();

		return sequences;
	}

}
