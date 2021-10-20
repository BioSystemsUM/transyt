package pt.uminho.ceb.biosystems.transyt.scraper.transmembraneDomains;

import java.util.HashMap;
import java.util.Map;

import org.biojava.nbio.core.sequence.template.AbstractSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.uminho.ceb.biosystems.transyt.scraper.transmembraneDomains.alpha.Phobius;
import pt.uminho.ceb.biosystems.transyt.scraper.transmembraneDomains.betaBarrels.PREDTMBB2;

public class TransmembraneDomainsSearcher {

	static Map<String, Integer> alphaHelices;
	static Map<String, Integer> betaBarrels;
	
	private static final Logger logger = LoggerFactory.getLogger(TransmembraneDomainsSearcher.class);
	
	public TransmembraneDomainsSearcher(Map<String, AbstractSequence<?>> genome, String email) {
	
		alphaHelices = new HashMap<>();
		betaBarrels = new HashMap<>();
		
		search(genome, email);
	}

	/**
	 * @param genome
	 * @param email
	 * @return
	 */
	private void search(Map<String, AbstractSequence<?>> genome, String email) {

		logger.debug("Transmembrane domains search initiated!");
		
		try {
			
			Thread thread1 = new Thread() {
				public void run() {
					betaBarrels.putAll(PREDTMBB2.searchBetaBarrels(email, genome));
				}
			};
			
			Thread thread2 = new Thread() {
				public void run() {
					alphaHelices.putAll((new Phobius(email, genome)).getResults());
				}
			};
			
			thread1.start();
			thread2.start();

			thread1.join();
			thread2.join();
			
		} 
		catch (InterruptedException e) {
			logger.error("An error occurred while searching genome's transmembrane domains! Returning none...");
			logger.trace("StackTrace {}",e);
		}

	}

	/**
	 * @return the alphaHelices
	 */
	public Map<String, Integer> getAlphaHelices() {
		return alphaHelices;
	}

	/**
	 * @return the betaBarrels
	 */
	public Map<String, Integer> getBetaBarrels() {
		return betaBarrels;
	}

}
