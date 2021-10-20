package pt.uminho.ceb.biosystems.transyt.scraper.transmembraneDomains.betaBarrels;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.biojava.nbio.core.sequence.template.AbstractSequence;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PREDTMBB2 {

	private final static String LINK = "http://195.251.108.230/PRED-TMBB2/";
	private final static int LIMIT = 5;

	private static final Logger logger = LoggerFactory.getLogger(PREDTMBB2.class);

	public static Map<String, Integer> searchBetaBarrels(String email, Map<String, AbstractSequence<?>> sequences){

		logger.debug("Transmembrane beta barrels search using PRED-TMBB2 initiated!");

		String currentUrl;
		
		int batchNum = 1;

		Map<String, Integer> results = new HashMap<>();

		Iterator<String> iterator = sequences.keySet().iterator();

		boolean submit = true;

		while(submit) {
			
			logger.debug("Batch number {} beta-barrels search initiated!", batchNum);
			
			Map<String, AbstractSequence<?>> batch = new HashMap<>();

			while(iterator.hasNext() && batch.size() < 1000) {
				
				String key = iterator.next();		//PRED-TMBB2 changes all symbols, only accepts "_"
				batch.put(key.replace(".", "___"), sequences.get(key));		//Change to the original during the reading process!
			}
			
			try {
				String query = getCorrectFormatFasta(batch);

				WebDriver driver = new HtmlUnitDriver();

				// And now use this to visit the website
				driver.get(LINK);

				// Find the text input element by its name
				WebElement element = driver.findElement(By.name("sequence"));			//text area
				element.sendKeys(query);

				element = driver.findElement(By.xpath("/html/body/form/input[2]"));		//check prediction for batch
				element.click();

				element = driver.findElement(By.xpath("/html/body/form/input[3]"));		//uncheck Signal peptide predictions
				element.click();

				element = driver.findElement(By.xpath("/html/body/form/input[7]"));		//email
				element.click();
				element.sendKeys(email);

				element = driver.findElement(By.xpath("/html/body/form/input[8]"));		//run prediction
				element.click();

				TimeUnit.SECONDS.sleep(2);		//	see if necessary

				boolean go = false;
				currentUrl = null;
				int errorCounter = 0;

				while(!go && errorCounter < LIMIT){

					try{

						currentUrl = driver.getCurrentUrl();

						go = true;
					}
					catch(Exception e){

						errorCounter ++;
						TimeUnit.SECONDS.sleep(10);
					}
				}
				
				if(errorCounter == LIMIT) {
					logger.error("An url was not returned by PRED-TMBB2 servers at batch{}! Returning none...", batchNum);
					return new HashMap<>();
				}
	
			} 
			catch (Exception e) {

				logger.error("An error occurred while searching genome's beta barrels using PRED-TMBB2! Returning none...");
				logger.trace("StackTrace {}",e);

				return new HashMap<>();
			}
			
			logger.info("Successfully submitted! Seaching beta barrels at: {}", currentUrl);
			
			results.putAll(ReadPREDTMBB2.readResults(currentUrl));
			
			if(!iterator.hasNext())
				submit = false;
			
			batchNum++;
		}
		
		logger.debug("Transmembrane beta barrels search finished!");

		return results;
	}

	/**
	 * Method to put the query in fasta format for submission
	 * 
	 * @param sequences
	 * @return
	 */
	public static String getCorrectFormatFasta(Map<String, AbstractSequence<?>> sequences){

		String query = "";

		for(String sequence : sequences.keySet()){

			query = query.concat(">").concat(sequence).concat("\n").concat(sequences.get(sequence).toString()).concat("\n");

		}

		return query;
	}


}
