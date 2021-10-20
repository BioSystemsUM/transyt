package pt.uminho.ceb.biosystems.transyt.service.blast;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.biojava.nbio.core.sequence.ProteinSequence;
import org.biojava.nbio.core.sequence.compound.AminoAcidCompound;
import org.biojava.nbio.core.sequence.compound.AminoAcidCompoundSet;
import org.biojava.nbio.core.sequence.io.FastaReader;
import org.biojava.nbio.core.sequence.io.FastaReaderHelper;
import org.biojava.nbio.core.sequence.io.GenericFastaHeaderParser;
import org.biojava.nbio.core.sequence.io.ProteinSequenceCreator;
import org.biojava.nbio.core.sequence.template.AbstractSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.uminho.ceb.biosystems.merlin.bioapis.externalAPI.ncbi.CreateGenomeFile;
import pt.uminho.ceb.biosystems.merlin.utilities.Enumerators.AlignmentScoreType;
import pt.uminho.ceb.biosystems.merlin.utilities.Enumerators.Method;
import pt.uminho.ceb.biosystems.merlin.utilities.containers.capsules.AlignmentCapsule;
import pt.uminho.ceb.biosystems.transyt.utilities.connection.ReadFastaTcdb;
import pt.uminho.ceb.biosystems.transyt.utilities.files.FilesUtils;
import pt.uminho.ceb.biosystems.transyt.utilities.transytUtilities.Properties;

/**
 * @author Davide
 *
 */
public class Blast implements Observer{

	private static final Logger logger = LoggerFactory.getLogger(Blast.class);
	
	private ConcurrentLinkedQueue<AlignmentCapsule> results;
//	private String blastDirectory;
	private String workFolderID;
	private Integer queryFileSize = 0;
	private Properties properties;
	private String queryFilePath;

	public Blast(String workFolderID, String queryFilePath, Properties properties) {

		try {
			
			logger.info("Blast process initializing...");
			
			this.properties = properties;
			
			this.queryFilePath = queryFilePath;
			
			this.workFolderID = workFolderID;

			results = performBlast();

			logger.info("Blast process finished!");
		} 
		catch(FileNotFoundException e1) {
			
			logger.error("The genome file does not exist in the given path!!");
			
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Method were all configurations to perform BLAST are set.
	 * 
	 * @return
	 * @throws Exception
	 */
	private ConcurrentLinkedQueue<AlignmentCapsule> performBlast() throws Exception {

		String tcdbFastaFile = ReadFastaTcdb.getPathFastaForAlignmentLastKnownVersion();
		
		logger.debug("Reading TCDB fasta in local folder: {}", tcdbFastaFile);

		logger.debug("Reading given target genome FASTA at: {}", queryFilePath);
		
		ConcurrentHashMap<String, AbstractSequence<?>> sequences= new ConcurrentHashMap<String, AbstractSequence<?>>();
		sequences.putAll(FastaReaderHelper.readFastaProteinSequence(new File(queryFilePath)));
		
		queryFileSize = sequences.size();

		logger.info("Blast process initialized!");

		RunSimilaritySearchTransyt run_similaritySearch = new RunSimilaritySearchTransyt(properties.isForceBlast(), properties.getSimilarityThreshold(),
				Method.SmithWaterman, sequences, new AtomicBoolean(false), new AtomicInteger(0), AlignmentScoreType.ALIGNMENT);

		run_similaritySearch.setSubjectFastaFilePath(tcdbFastaFile);
		run_similaritySearch.addObserver(this);
		run_similaritySearch.setWorkspaceTaxonomyFolderPath(workFolderID);
		ConcurrentLinkedQueue<AlignmentCapsule> results = null;

		if(sequences.keySet().size()>0)
			results = run_similaritySearch.runBlastSearch(properties.getBlastpExecutableAlias(), true, properties.getBlastEvalueThreshold(), properties.getBitScore(), properties.getQueryCoverage());

		return results;
	}

	@Override
	public void update(Observable arg0, Object arg1) {
		// TODO Auto-generated method stub

	}
	
	/**
     * @return
     */
    public Map<String,List<AlignmentCapsule>> getAlignmentsByQuery(){

        Map<String,List<AlignmentCapsule>> alignmentMap = new HashMap<>();

        for(AlignmentCapsule alignContainer : this.results){

            String query = alignContainer.getQuery();

            if(alignmentMap.containsKey(query)){

                alignmentMap.get(query).add(alignContainer);
            }
            else{
                List<AlignmentCapsule> containersList = new ArrayList<>();
                containersList.add(alignContainer);
                alignmentMap.put(query, containersList);
            }
        }

        return alignmentMap;
    }

	/**
	 * @return the results
	 */
	public ConcurrentLinkedQueue<AlignmentCapsule> getResults() {
		return results;
	}

	/**
	 * @return the queryFileSize
	 */
	public Integer getQueryFileSize() {
		return queryFileSize;
	}


}
