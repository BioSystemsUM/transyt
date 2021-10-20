package pt.uminho.ceb.biosystems.transyt.service.blast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.bind.JAXBContext;

import org.biojava.nbio.core.sequence.template.AbstractSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.uminho.ceb.biosystems.merlin.bioapis.externalAPI.ncbi.CreateGenomeFile;
import pt.uminho.ceb.biosystems.merlin.local.alignments.core.ModelMerge.BlastAlignment;
import pt.uminho.ceb.biosystems.merlin.local.alignments.core.ModelMerge.ModelAlignments;
import pt.uminho.ceb.biosystems.merlin.utilities.Enumerators.AlignmentScoreType;
import pt.uminho.ceb.biosystems.merlin.utilities.Enumerators.Method;
import pt.uminho.ceb.biosystems.merlin.utilities.blast.ncbi_blastparser.BlastOutput;
import pt.uminho.ceb.biosystems.merlin.utilities.containers.capsules.AlignmentCapsule;
import pt.uminho.ceb.biosystems.transyt.utilities.files.ReadFasta;

/**
 * @author ODias
 *
 */
public class RunSimilaritySearchTransyt extends Observable implements Observer {

	private AtomicBoolean cancel;
	private AtomicInteger querySize;
	private double similarity_threshold;
	private ConcurrentHashMap<String, AbstractSequence<?>> querySequences;
	private List<String> annotatedGenes;
	private ConcurrentLinkedQueue<String> sequencesWithoutSimilarities;
	private String ec_number;
	private Map<String, Set<Integer>> modules;
	private double referenceTaxonomyThreshold;
	private boolean compareToFullGenome;
//	private String tcdbFastaFilePath;
	private String subjectFastaFilePath;
	private boolean gapsIdentification;
	private String workspaceTaxonomyFolderPath;
	private boolean forceBlast;
	final static Logger logger = LoggerFactory.getLogger(RunSimilaritySearchTransyt.class);


	/**
	 * Run similarity searches constructor.
	 * 
	 * @param dbAccess
	 * @param staticGenesSet
	 * @param minimum_number_of_helices
	 * @param similarity_threshold
	 * @param method
	 * @param querySequences
	 * @param cancel
	 * @param querySize
	 * @param counter
	 * @param project_id
	 * @param alignmentScoreType
	 * @throws Exception
	 */
	public RunSimilaritySearchTransyt(boolean forceBlast, double similarity_threshold, Method method, ConcurrentHashMap<String, AbstractSequence<?>> querySequences, 
			AtomicBoolean cancel, AtomicInteger querySize, AlignmentScoreType alignmentScoreType) throws Exception {

		this.forceBlast = forceBlast;
		this.setQuerySize(querySize);
		this.setCancel(cancel);
		this.similarity_threshold = similarity_threshold;
		this.querySequences = querySequences;
		this.sequencesWithoutSimilarities = null;
		
		this.gapsIdentification = false;
	}
	
	///////////////////////////////////
	
	/**
	 * Run the transport similarity searches.
	 * If some threshold parameters were null, this method use the default values.
	 * 
	 * Default values: evalueThreshold(1E-6), bitScoreThreshold(50), queryCoverageThreshold(0.80)
	 * 
	 * @param isTransportersSearch
	 * @param eValueThreshold 
	 * @param bitScoreThreshold
	 * @param queryCoverageThreshold
	 * @return
	 * @throws Exception
	 */
	public ConcurrentLinkedQueue<AlignmentCapsule> runBlastSearch(String blastExe, boolean isTransportersSearch, Double eValueThreshold, Double bitScoreThreshold, Double queryCoverageThreshold) throws Exception {
		
		List<Thread> threads = new ArrayList<Thread>();
//		ConcurrentLinkedQueue<String> queryArray = new ConcurrentLinkedQueue<String>(this.querySequences.keySet());
		int numberOfCores = Runtime.getRuntime().availableProcessors();
		//int numberOfCores = new Double(Runtime.getRuntime().availableProcessors()*1.5).intValue();
		
		if(this.querySequences.keySet().size()<numberOfCores)
			numberOfCores=this.querySequences.keySet().size();

		this.querySize.set(new Integer(this.querySequences.size()));
		setChanged();
		notifyObservers();
		
		//Distribute querySequences into fastaFiles
		
		logger.debug("Writting query sequences temporary fasta files... ");
		
		List<String> queryFilesPaths = new ArrayList<>();
		List<Map<String,AbstractSequence<?>>> queriesSubSetList = new ArrayList<>();
		
		String path = this.workspaceTaxonomyFolderPath.concat("/queryBlast");
		
		File f = new File (path);
		if(!f.exists())
			f.mkdir();
		
		ReadFasta.buildSubFastaFiles(path, this.querySequences, queriesSubSetList, queryFilesPaths, numberOfCores);
		
		ConcurrentLinkedQueue<AlignmentCapsule> alignmentContainerSet = new ConcurrentLinkedQueue<>();
		JAXBContext jc = JAXBContext.newInstance(BlastOutput.class);
		
		for(int i=0; i<numberOfCores; i++) {
			
			ModelAlignments blastAlign;
			
				blastAlign = new BlastAlignmentTransyt(forceBlast, blastExe, queryFilesPaths.get(i), this.subjectFastaFilePath, queriesSubSetList.get(i), 
						this.similarity_threshold, eValueThreshold, bitScoreThreshold, queryCoverageThreshold, isTransportersSearch, this.cancel, alignmentContainerSet, jc);
			
			if(eValueThreshold!=null)
				((BlastAlignmentTransyt) blastAlign).setEvalueThreshold(eValueThreshold);
			if(bitScoreThreshold!=null)
				((BlastAlignmentTransyt) blastAlign).setBitScoreThreshold(bitScoreThreshold);
			if(queryCoverageThreshold!=null)
				((BlastAlignmentTransyt) blastAlign).setQueryCoverageThreshold(queryCoverageThreshold);	
			
			((BlastAlignmentTransyt) blastAlign).addObserver(this); 

			Thread thread = new Thread(blastAlign);
			threads.add(thread);
			thread.start();
		}
		
		for(Thread thread :threads)
			thread.join();
		
		return alignmentContainerSet;
	}
	///////////////////////////////
	
	/**
	 * @param querySize the querySize to set
	 */
	public void setQuerySize(AtomicInteger querySize) {
		this.querySize = querySize;
	}

	/**
	 * @return the querySize
	 */
	public AtomicInteger getQuerySize() {
		return querySize;
	}


	/**
	 * @param cancel the cancel to set
	 */
	public void setCancel(AtomicBoolean cancel) {
		this.cancel = cancel;
	}

	/**
	 * @param annotatedGenes
	 */
	public void setAnnotatedGenes(List<String> annotatedGenes) {

		this.annotatedGenes = annotatedGenes;		
	}

	/**
	 * @return
	 */
	public List<String> getAnnotatedGenes() {

		return this.annotatedGenes;		
	}

	/**
	 * @return the ec_number
	 */
	public String getEc_number() {
		return ec_number;
	}

	/**
	 * @param ec_number the ec_number to set
	 */
	public void setEc_number(String ec_number) {
		this.ec_number = ec_number;
	}

	/**
	 * @return the idModule
	 */
	public Map<String, Set<Integer>> getModules() {
		return modules;
	}

	/**
	 * @param genes_ko_modules the idModule to set
	 */
	public void setModules(Map<String, Set<Integer>> modules) {
		this.modules = modules;
	}

	/**
	 * @return the sequencesWithoutSimilarities
	 */
	public ConcurrentLinkedQueue<String> getSequencesWithoutSimilarities() {
		return sequencesWithoutSimilarities;
	}

	/**
	 * @param sequencesWithoutSimilarities the sequencesWithoutSimilarities to set
	 */
	public void setSequencesWithoutSimilarities(
			ConcurrentLinkedQueue<String> sequencesWithoutSimilarities) {
		this.sequencesWithoutSimilarities = sequencesWithoutSimilarities;
	}

	public double getReferenceTaxonomyThreshold() {
		return referenceTaxonomyThreshold;
	}

	public void setReferenceTaxonomyThreshold(double referenceTaxonomyThreshold) {
		this.referenceTaxonomyThreshold = referenceTaxonomyThreshold;
	}
	
	
	/**
	 * @return
	 */
	public String getSubjectFastaFilePath() {
		return this.subjectFastaFilePath;
	}

	/**
	 * @param subjectFastaFilePath
	 */
	public void setSubjectFastaFilePath(String subjectFastaFilePath) {
		this.subjectFastaFilePath = subjectFastaFilePath;
	}

//	/**
//	 * @return the tcdbFastaFilePath
//	 */
//	public String getTcdbFastaFilePath() {
//		return tcdbFastaFilePath;
//	}
//
//	/**
//	 * @param tcdbFastaFilePath the tcdbFastaFilePath to set
//	 */
//	public void setTcdbFastaFilePath(String tcdbFastaFilePath) {
//		this.tcdbFastaFilePath = tcdbFastaFilePath;
//	}

	public boolean isCompareToFullGenome() {
		return compareToFullGenome;
	}

	public void setCompareToFullGenome(boolean compareToFullGenome) {
		this.compareToFullGenome = compareToFullGenome;
	}
	
	/**
	 * @return the gapsIdentification
	 */
	public boolean isGapsIdentification() {
		return gapsIdentification;
	}

	/**
	 * @param gapsIdentification the gapsIdentification to set
	 */
	public void setGapsIdentification(boolean gapsIdentification) {
		this.gapsIdentification = gapsIdentification;
	}

	public String getWorkspaceTaxonomyFolderPath() {
		return workspaceTaxonomyFolderPath;
	}

	public void setWorkspaceTaxonomyFolderPath(String workspaceTaxonomyFolderPath) {
		this.workspaceTaxonomyFolderPath = workspaceTaxonomyFolderPath;
	}

	@Override
	public void update(Observable arg0, Object arg1) {

		setChanged();
		notifyObservers();
	}

}
