package pt.uminho.ceb.biosystems.transyt.scraper.transmembraneDomains.alpha;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.biojava.nbio.core.sequence.template.AbstractSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.uminho.ceb.biosystems.merlin.bioapis.externalAPI.ebi.EbiAPI;
	
/**
 * @author Davide
 *
 */
public class Phobius implements Observer{

	private static final Logger logger = LoggerFactory.getLogger(Phobius.class);
	Map<String, Integer> results;
	
	long waitingPeriod = 300000;
	
	/**
	 * @param genome
	 * @param email
	 */
	public Phobius(String email, Map<String, AbstractSequence<?>> genome) {
		
		logger.debug("Transmembrane alpha helices search using Phobius initiated!");
		
		results = new HashMap<>();
		
		search(genome, email);
	}

	/**
	 * @param genome
	 * @param email
	 * @return
	 */
	public Map<String, Integer> search(Map<String, AbstractSequence<?>> genome, String email){
		
		try {
			
			EbiAPI ebiAPI = new EbiAPI();
			ebiAPI.addObserver(this);
			results = ebiAPI.getHelicesFromPhobius(genome, new AtomicInteger(0), new AtomicBoolean(), waitingPeriod, new AtomicInteger(0), email);
			
			logger.debug("Transmembrane alpha helices search finished!");
		} 
		catch (InterruptedException e) {
			logger.error("An error occurred while searching genome's alpha helices using Phobius! Returning none...");
			logger.trace("StackTrace {}",e);
			
			return null;
		}
		
		return results;
		
	}

	@Override
	public void update(Observable arg0, Object arg1) {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * @return the results
	 */
	public Map<String, Integer> getResults() {
		return results;
	}
	
}
