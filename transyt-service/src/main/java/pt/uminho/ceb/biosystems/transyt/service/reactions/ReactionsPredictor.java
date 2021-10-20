package pt.uminho.ceb.biosystems.transyt.service.reactions;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.uminho.ceb.biosystems.merlin.utilities.containers.capsules.AlignmentCapsule;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.GeneContainer;
import pt.uminho.ceb.biosystems.transyt.utilities.transytUtilities.Properties;

public class ReactionsPredictor {

	private static final Logger logger = LoggerFactory.getLogger(ReactionsPredictor.class);

	public static Map<String, Set<String>> getReactionsForGenesBySimilarities(Map<String, GeneContainer> data, Map<String, Set<String>> reactionsByTcNumber, 
			Map<String, List<AlignmentCapsule>> blastResults, Map<String, List<String>> mainReactions, Set<String> reactionsToIgnore, Properties properties) {

		Map<String, Set<String>> results = new HashMap<>();

		logger.debug("Starting reactions to genes association.");

		int lastProgress = -1;
		int current = 0;

		for(String queryAccession : data.keySet()) {
			
//			if(queryAccession.equals("b4296"))
//				System.out.println();

			if(data.get(queryAccession).getAnnotatedFamily() != null) {

				try {
					LinkedHashMap<String, Double> sortedReactionsMap = findTransportReactions(data.get(queryAccession), reactionsByTcNumber, 
							blastResults.get(queryAccession), mainReactions, reactionsToIgnore, properties);

					results.put(queryAccession, sortedReactionsMap.keySet());

				} 
				catch (Exception e) {

					logger.error("A problem occurred while searching the reactions for gene: {}", queryAccession);

					e.printStackTrace();
				}

				current++;

				Integer progress = (current*100)/data.size();

				if(progress > lastProgress){

					lastProgress = progress;
					logger.trace(progress.toString().concat(" % search complete"));
				}
			}
		}

		return results;
	}

	/**
	 * @param geneContainer
	 */
	private static LinkedHashMap<String, Double> findTransportReactions(GeneContainer geneContainer, Map<String, Set<String>> reactionsByTcNumber, List<AlignmentCapsule> blastResults, 
			Map<String, List<String>> mainReactions, Set<String> reactionsToIgnore, Properties properties) {

		Map<String, Integer> hits = new HashMap<>();

		Map<String, Double> reactionsSimilarity = new HashMap<>();

		Double similaritySum = 0.0;

		Set<String> allReactions = new LinkedHashSet<>();

		Map<String, Integer> commonTaxaSum = new HashMap<>();

		for(AlignmentCapsule capsule : blastResults) {

			String tcNumber = capsule.getTcdbID();

 			if(geneContainer.getCommomTaxaCount().containsKey(capsule.getTarget())){

				double simScore = capsule.getScore();

				int taxaCount = geneContainer.getCommomTaxaCount().get(capsule.getTarget());

				if(reactionsByTcNumber.containsKey(tcNumber)) {

					//				System.out.println(reactionsByTcNumber.get(tcNumber).contains("TR0000971"));

					for(String reactionID : reactionsByTcNumber.get(tcNumber)) {

						allReactions.add(reactionID);

						if(hits.containsKey(reactionID)) {

							hits.put(reactionID, hits.get(reactionID) + 1);
							commonTaxaSum.put(reactionID, commonTaxaSum.get(reactionID) + taxaCount);

							reactionsSimilarity.put(reactionID, reactionsSimilarity.get(reactionID) + simScore);
						}
						else {
							hits.put(reactionID, 1);

							commonTaxaSum.put(reactionID, taxaCount);
							reactionsSimilarity.put(reactionID, simScore);
						}
					}

					//				System.out.println(similaritySum + " + " + simScore);

					similaritySum = similaritySum + simScore;//geneContainer.getSimilarities().get(capsule.getTarget());
				}
			}
		}

		int tMax = geneContainer.getMaxTax();

		Map<String, Double> res = new HashMap<>();

		for(String reactionID : allReactions) {

			//			if(reactionID.equals("TR0000971"))
			//				System.out.println(reactionID);

			double gap = 0.0;

			if(!reactionsToIgnore.contains(reactionID)) {

				int hit = hits.get(reactionID);

				if(hit < properties.getMinimumHits())
					gap = properties.getMinimumHits() - hit;


				//				System.out.println("freqScore = " + reactionsSimilarity.get(reactionID) + " / " + similaritySum);
				//				System.out.println("taxScore = " + commonTaxaSum.get(reactionID) + " * " + (1-gap * properties.getBeta())+ " / " + hit * tMax);

				double freqScore = reactionsSimilarity.get(reactionID) / similaritySum;
				double taxScore = commonTaxaSum.get(reactionID) * (1-gap * properties.getBeta()) / (hit * tMax);

				Double score = properties.getAlpha() * freqScore + (1-properties.getAlpha()) * taxScore;

				//				System.out.println(reactionID + " ---->>> \t" +  properties.getAlpha() + " * " + freqScore + " + (1 - " + properties.getAlpha() + " ) * " + taxScore + " = " + score) ;

				if(score > properties.getReactionsAnnotationScoreThreshold()) {

					//					System.out.println(reactionID + "\t" + score);

					res.put(reactionID, score);
				}
			}
		}

		LinkedHashMap<String, Double> sortedMap = new LinkedHashMap<>();

		res.entrySet()	//sorting the results to select the best transport type latter
		.stream()
		.sorted(Map.Entry.comparingByValue())
		.forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));

		return sortedMap;
	}

	/**
	 * Annotate the correct family by having into account the similarities and frequency of a family
	 * 
	 * @param totalEntries
	 * @param similaritySum
	 * @param familiesFrequency
	 * @param familiesSimilarity
	 * @return
	 */
	public static String annotateTcFamily(int totalEntries, double similaritySum, Map<String, Integer> familiesFrequency,
			Map<String, Double> familiesSimilarity, Properties properties) {

		String annotation = "";
		double value = 0.0;

		for(String tcF : familiesFrequency.keySet()) {

			double score = (familiesFrequency.get(tcF) / totalEntries) * properties.getAlphaFamiliesAnnotation()
					+ (familiesSimilarity.get(tcF) / similaritySum) * (1 - properties.getAlphaFamiliesAnnotation());

			if(score > value) {
				annotation = tcF;
				value = score;
			}
		}

		return annotation;
	}
}
