package pt.uminho.ceb.biosystems.transyt.service.blast;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.bind.JAXBContext;

import org.biojava.nbio.core.sequence.template.AbstractSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.uminho.ceb.biosystems.merlin.local.alignments.core.AlignmentsUtils;
import pt.uminho.ceb.biosystems.merlin.local.alignments.core.ModelMerge.ModelAlignments;
import pt.uminho.ceb.biosystems.merlin.utilities.Enumerators.AlignmentPurpose;
import pt.uminho.ceb.biosystems.merlin.utilities.blast.ncbi_blastparser.BlastIterationData;
import pt.uminho.ceb.biosystems.merlin.utilities.blast.ncbi_blastparser.Hit;
import pt.uminho.ceb.biosystems.merlin.utilities.blast.ncbi_blastparser.NcbiBlastParser;
import pt.uminho.ceb.biosystems.merlin.utilities.containers.capsules.AlignmentCapsule;


/**
 * @author amaromorais
 *
 */
public class BlastAlignmentTransyt extends Observable implements ModelAlignments{

	//	private static final double FIXED_THRESHOLD =  1E-6;
	//	private static final double ALIGNMENT_MIN_SCORE = 0.0;
	//	private static final double BITSCORE_THRESHOLD = 50;
	//	private static final double COVERAGE_THRESHOLD = 0.20;
	//	private static final double ALIGNMENT_QUERY_LEN_THRESHOLD = 0.25;
	//	private static final double QUERY_HIT_LEN_THRESHOLD = 0.25;

	private NcbiBlastParser blout;
	private ConcurrentLinkedQueue<AlignmentCapsule> alignmentContainerSet;
	private String alignmentMatrix, queryFasta, subjectFasta, blastOutputFolderPath;
	private boolean isTransportersSearch = false;
	private AtomicBoolean cancel; 
	private Map<String,AbstractSequence<?>> querySequences;
	private JAXBContext jc;
	private String ec_number;
	private Map<String,Set<String>> closestOrthologs;
	private Map<String,Set<Integer>> modules;
	private ConcurrentLinkedQueue<String> sequencesWithoutSimilarities;
	private AlignmentPurpose blastPurpose;

	private double threshold;
	private double evalueThreshold;
	private double bitScoreThreshold;
	private double queryCoverageThreshold;

	private double alignmentMinScore;

	private Double referenceTaxonomyThreshold;
	private Map<String, List<String>> sequenceIdsSet;
	private Map<String, Integer> kegg_taxonomy_scores;
	private Integer referenceTaxonomyScore;
	private boolean forceBlast;
	private String blastExe;

	final static Logger logger = LoggerFactory.getLogger(BlastAlignmentTransyt.class);



	/**
	 * Default value for alignmentMinScore(0.0);
	 * 
	 * @param queryFasta
	 * @param subjectFasta
	 * @param querySequences
	 * @param treshold
	 * @param evalueThreshold
	 * @param bitScoreThreshold
	 * @param queryCoverageThreshold
	 * @param transportersSearch
	 * @param cancel
	 * @param alignmentContainerSet
	 * @param jc
	 */
	public BlastAlignmentTransyt(boolean forceBlast, String blastExe, String queryFasta, String subjectFasta, Map<String,AbstractSequence<?>> querySequences, double treshold, double evalueThreshold,
			double bitScoreThreshold, double queryCoverageThreshold, boolean transportersSearch, AtomicBoolean cancel, ConcurrentLinkedQueue<AlignmentCapsule> alignmentContainerSet, JAXBContext jc){

		this.forceBlast = forceBlast;
		this.blastExe = blastExe;
		this.setEvalueThreshold(evalueThreshold);
		this.setBitScoreThreshold(bitScoreThreshold);
		this.setQueryCoverageThreshold(queryCoverageThreshold);
		this.setAlignmentMinScore(0.0);
		this.queryFasta = queryFasta;
		this.subjectFasta = subjectFasta;
		this.threshold = treshold;
		this.isTransportersSearch = transportersSearch;
		this.querySequences = querySequences;
		this.alignmentContainerSet = alignmentContainerSet;
		this.cancel = cancel;
		this.jc = jc;

	}

	public void run(){

		if(!this.cancel.get()) {

			try {
				
				String outputFileName = queryFasta.substring(queryFasta.lastIndexOf("/")).replace(".faa", "").concat("_blastReport.xml");
				if(isTransportersSearch)
					outputFileName = outputFileName.replace(".xml", "_transporters.xml");

				File outputFile;

				this.blastOutputFolderPath = new File(queryFasta).getParent().concat("reports");
				
				outputFile = new File(blastOutputFolderPath.concat(outputFileName));

				outputFile.getParentFile().mkdirs();
				
//				System.out.println(outputFile.getAbsolutePath());

				if(forceBlast) {
					
					String command = this.blastExe + " -evalue " + this.evalueThreshold + " -query " + this.queryFasta + " -subject " 
							+ this.subjectFasta + " -out " + outputFile.getAbsolutePath() + " -outfmt 5";
					
					logger.debug(command);
					
					Process p = Runtime.getRuntime().exec(command);

					p.waitFor();
				}
				else
					logger.info("Blast skipped, searching cached results...");

				if(outputFile.exists()){

					this.blout = new NcbiBlastParser(outputFile, this.jc);
					this.alignmentMatrix = blout.getMatrix();

					buildAlignmentCapsules();
				}
				else{

					logger.warn("blast output .xml file wasn't generated on {}", outputFile.getAbsolutePath());
				}
			}
			catch (UnknownHostException e2) {

				logger.error("NCBI service failed. Please try again. Shuting down...");

				System.exit(0);
			}
			catch (IOException | InterruptedException e) {

				e.printStackTrace();
			}
			catch (OutOfMemoryError oue) {

				oue.printStackTrace();
			}

			System.gc();

			setChanged();
			notifyObservers();
		}

		setChanged();
		notifyObservers();
	}


	public void buildAlignmentCapsules(){

		List<BlastIterationData> iterations = this.blout.getResults();

		Map<String, Double> queriesMaxScores = AlignmentsUtils.getSequencesAlignmentMaxScoreMap(querySequences, alignmentMatrix);

		//		System.out.println("querySequences----->"+querySequences);
		//		System.out.println(querySequences.keySet()+"\t"+querySequences.size());
		//		System.out.println("queriesMaxScores----->"+queriesMaxScores);
		//		System.out.println(queriesMaxScores.keySet()+"\t"+querySequences.size());


		for(BlastIterationData iteration : iterations){

			String queryID = iteration.getQueryDef().trim();
			Integer queryLength = iteration.getQueryLen();
			
//			if(queryID.equals("b2169"))
//				System.out.println();
			
			String [] query_array; 
			String query_org = "";
			String queryLocus = "";

			if(queryID.contains(":")) {
				query_array = queryID.split(":"); 
				query_org = query_array [0].trim();
				queryLocus = query_array[1].trim();
			}
			else {
				if(queryID.contains(" ")) {
					queryID = new StringTokenizer(queryID," ").nextToken();
				}

				if(this.blastPurpose!=null && this.blastPurpose.equals(AlignmentPurpose.ORTHOLOGS)) {					
					for(String seqID : this.querySequences.keySet()) {
						if(seqID.contains(queryID)) {
							queryID = seqID;
							query_array = queryID.split(":"); 
							query_org = query_array [0].trim();
							queryLocus = query_array[1].trim();
						}
					}
				}
			}

			if(this.blastPurpose==null || !this.blastPurpose.equals(AlignmentPurpose.ORTHOLOGS) || (!this.sequenceIdsSet.containsKey(queryLocus) || sequenceIdsSet.get(queryLocus).isEmpty())){

				//				System.out.println("QUERY----->"+queryID);

				double maxScore = queriesMaxScores.get(iteration.getQueryDef().trim());
				double specificThreshold = this.threshold;

				if(this.kegg_taxonomy_scores!=null && this.referenceTaxonomyScore!=null && this.referenceTaxonomyThreshold!=null)
					if(this.kegg_taxonomy_scores.get(query_org)>=this.referenceTaxonomyScore) 
						specificThreshold = this.referenceTaxonomyThreshold;

				List<Hit> hits = iteration.getHits();

				if(hits!=null && !hits.isEmpty()){

					for(Hit hit : hits){

						if(!this.cancel.get()){

							try {
								String tcdbID = "";
								String hitNum = hit.getHitNum();
								String target = hit.getHitId();

								Integer targetLength = iteration.getHitLength(hitNum);
								Integer alignmentLength = iteration.getHitAlignmentLength(hitNum);

								double alignmentScore = (iteration.getHitScore(hit)-this.alignmentMinScore)/(maxScore-this.alignmentMinScore);//alignmentMethod.getSimilarity(); //(((double)alignmentMethod.getScore()-alignmentMethod.getMinScore())/(alignmentMethod.getMaxScore()-alignmentMethod.getMinScore()))
								//double similarityScore = iteration.getPositivesScore(hitNum);
								//double identityScore = iteration.getIdentityScore(hitNum);

								double bitScore = iteration.getHitBitScore(hit);
								double eValue = iteration.getHitEvalue(hit);

								double queryCoverage = iteration.getHitQueryCoverage(hitNum);//(double)(alingmentLength-iteration.getHitAlignmentGaps(hitNum))/(double)queryLength;
								double tragetCoverage = iteration.getHiTargetCoverage(hitNum);//(double)(alingmentLength-iteration.getHitAlignmentGaps(hitNum))/(double)targetLength;

								//								double l1 = (double)queryLength/(double)targetLength;
								//								double l2 = (double)alingmentLength/(double)queryLength;
								//								double l3 = (double)alingmentLength/(double)targetLength;

								double score = alignmentScore;//-1;

								//				if(this.alignmentScoreType.equals(AlignmentScoreType.ALIGNMENT))
								//					score = alignmentScore;
								//				else if(this.alignmentScoreType.equals(AlignmentScoreType.IDENTITY))
								//					score = identityScore;
								//				else if(this.alignmentScoreType.equals(AlignmentScoreType.SIMILARITY))
								//					score = similarityScore;

								//								System.out.println(queryID+"\t"+target+"\t"+score+"\t"+specificThreshold+"\t"+iteration.getHitEvalue(hit)+"\t"+iteration.getHitBitScore(hit)
								//								+"\t"+l1);//)+"\t"+l2+"\t"+l3);

								boolean go = false;

								if(isTransportersSearch){
									if(eValue<this.evalueThreshold && bitScore>this.bitScoreThreshold && Math.abs(1-queryCoverage)<=(1-queryCoverageThreshold))
										go=true;
								}
								else if(blastPurpose.equals(AlignmentPurpose.ORTHOLOGS)){
									if(score>specificThreshold)
										go=true;
								}

								if(go){
									//									&& Math.abs(1-l1)<=ALIGNMENT_QUERY_LEN_THRESHOLD && Math.abs(1-l2)<=QUERY_HIT_LEN_THRESHOLD){

									if(this.sequencesWithoutSimilarities!=null && this.sequencesWithoutSimilarities.contains(queryID)) {
										System.out.println("REMOVING "+queryID+" from sequencesWithoutSimilarities");

										this.sequencesWithoutSimilarities.remove(queryID);
									}

									if(isTransportersSearch){

										String hitdef = hit.getHitDef();

										StringTokenizer st = new StringTokenizer(hitdef,"|");
										st.nextToken();
										st.nextToken();
										target = st.nextToken().toUpperCase().trim();
										tcdbID = st.nextToken().split("\\s+")[0].toUpperCase().trim();
									}

									AlignmentCapsule alignContainer = new AlignmentCapsule(queryID, target, tcdbID, this.alignmentMatrix, score);

									alignContainer.setEvalue(eValue);
									alignContainer.setBitScore(bitScore);
									alignContainer.setAlignmentLength(alignmentLength);
									alignContainer.setQueryLength(queryLength);
									alignContainer.setTargetLength(targetLength);	
									alignContainer.setNumIdenticals(iteration.getHitIdentity(hitNum));
									alignContainer.setNumSimilars(iteration.getHitPositive(hitNum));
									alignContainer.setCoverageQuery(queryCoverage);
									alignContainer.setCoverageTarget(tragetCoverage);

									alignContainer.setEcNumber(this.ec_number);
									alignContainer.setClosestOrthologues(this.closestOrthologs);
									alignContainer.setModules(modules);

									//					alignContainer.setMaxScore(maxScore);
									//					alignContainer.setMinScore(0);
									//					alignContainer.setAlignedScore(alignedScore);

									//					iterationAlignments.add(align);
									this.alignmentContainerSet.add(alignContainer);
								}
							} 
							catch (Exception e) {
								e.printStackTrace();
							}
						}
					}

					//			this.alignments.put(queryID,iterationAlignments);
				}
//				else{	//Annoying irrelevant message
//
//					logger.debug(iteration.getIteration().getIterationMessage().concat(" for {}"), queryID);
//				}
			}
			else{
				if(this.sequencesWithoutSimilarities!=null && this.sequencesWithoutSimilarities.contains(queryID)) {
					System.out.println("REMOVING "+queryID+" from sequencesWithoutSimilarities");
					this.sequencesWithoutSimilarities.remove(queryID);
				}
			}
		}
	}


	public ConcurrentLinkedQueue<AlignmentCapsule> getAlignmentsCapsules(){

		return this.alignmentContainerSet;
	}


	/**
	 * Method that convert the ConcurrentLinkedQueue of alignmentCapsules into a Map where the keys are the querySequence Ids 
	 * and the values list of the correspondent AlignmentCapsules
	 * 
	 * @return
	 */
	public Map<String,List<AlignmentCapsule>> getAlignmentsByQuery(){

		Map<String,List<AlignmentCapsule>> alignmentMap = new HashMap<>();

		for(AlignmentCapsule alignContainer : this.alignmentContainerSet){

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
	 * @return the threshold
	 */
	public double getThreshold() {
		return threshold;
	}

	/**
	 * @param threshold the threshold to set
	 */
	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}

	/**
	 * @return the evalueThreshold
	 */
	public double getEvalueThreshold() {
		return evalueThreshold;
	}

	/**
	 * @param evalueThreshold the evalueThreshold to set
	 */
	public void setEvalueThreshold(double evalueThreshold) {
		this.evalueThreshold = evalueThreshold;
	}

	/**
	 * @return the bitScoreThreshold
	 */
	public double getBitScoreThreshold() {
		return bitScoreThreshold;
	}

	/**
	 * @param bitScoreThreshold the bitScoreThreshold to set
	 */
	public void setBitScoreThreshold(double bitScoreThreshold) {
		this.bitScoreThreshold = bitScoreThreshold;
	}

	/**
	 * @return the queryCoverageThreshold
	 */
	public double getQueryCoverageThreshold() {
		return queryCoverageThreshold;
	}

	/**
	 * @param queryCoverageThreshold the queryCoverageThreshold to set
	 */
	public void setQueryCoverageThreshold(double queryCoverageThreshold) {
		this.queryCoverageThreshold = queryCoverageThreshold;
	}

	/**
	 * @return the alignmentMinScore
	 */
	public double getAlignmentMinScore() {
		return alignmentMinScore;
	}


	/**
	 * @param alignmentMinScore the alignmentMinScore to set
	 */
	public void setAlignmentMinScore(double alignmentMinScore) {
		this.alignmentMinScore = alignmentMinScore;
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
	 * @return the closestOrthologs
	 */
	public Map<String,Set<String>> getClosestOrthologs() {
		return closestOrthologs;
	}

	/**
	 * @param closestOrthologs the closestOrthologs to set
	 */
	public void setClosestOrthologs(Map<String,Set<String>> closestOrthologs) {
		this.closestOrthologs = closestOrthologs;
	}

	/**
	 * @return the modules
	 */
	public Map<String,Set<Integer>> getModules() {
		return modules;
	}

	/**
	 * @param modules the modules to set
	 */
	public void setModules(Map<String,Set<Integer>> modules) {
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
	public void setSequencesWithoutSimilarities(ConcurrentLinkedQueue<String> sequencesWithoutSimilarities) {
		this.sequencesWithoutSimilarities = sequencesWithoutSimilarities;
	}

	/**
	 * @return the blastPurpose
	 */
	public AlignmentPurpose getBlastPurpose() {
		return blastPurpose;
	}

	/**
	 * @param blastPurpose the blastPurpose to set
	 */
	public void setBlastPurpose(AlignmentPurpose blastPurpose) {
		this.blastPurpose = blastPurpose;
	}

	/**
	 * @return the sequenceIdsSet
	 */
	public Map<String, List<String>> getSequenceIdsSet() {
		return sequenceIdsSet;
	}

	/**
	 * @param sequenceIdsSet the sequenceIdsSet to set
	 */
	public void setSequenceIdsSet(Map<String, List<String>> sequenceIdsSet) {
		this.sequenceIdsSet = sequenceIdsSet;
	}

	/**
	 * @return the kegg_taxonomy_scores
	 */
	public Map<String, Integer> getKegg_taxonomy_scores() {
		return kegg_taxonomy_scores;
	}

	/**
	 * @param kegg_taxonomy_scores the kegg_taxonomy_scores to set
	 */
	public void setKegg_taxonomy_scores(Map<String, Integer> kegg_taxonomy_scores) {
		this.kegg_taxonomy_scores = kegg_taxonomy_scores;
	}

	/**
	 * @return the referenceTaxonomyScore
	 */
	public int getReferenceTaxonomyScore() {
		return referenceTaxonomyScore;
	}

	/**
	 * @param referenceTaxonomyScore the referenceTaxonomyScore to set
	 */
	public void setReferenceTaxonomyScore(int referenceTaxonomyScore) {
		this.referenceTaxonomyScore = referenceTaxonomyScore;
	}

	/**
	 * @return the referenceTaxonomyThreshold
	 */
	public double getReferenceTaxonomyThreshold() {
		return referenceTaxonomyThreshold;
	}

	/**
	 * @param referenceTaxonomyThreshold the referenceTaxonomyThreshold to set
	 */
	public void setReferenceTaxonomyThreshold(double referenceTaxonomyThreshold) {
		this.referenceTaxonomyThreshold = referenceTaxonomyThreshold;
	}

}
