package tcdb.triage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.biojava.nbio.core.sequence.template.AbstractSequence;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import pt.uminho.ceb.biosystems.transyt.scraper.APIs.UniprotAPIExtension;
import pt.uminho.ceb.biosystems.transyt.scraper.tcdb.tcdbTransportTypesRetriever.ProcessCompartments;
import pt.uminho.ceb.biosystems.transyt.scraper.transmembraneDomains.betaBarrels.ReadPREDTMBB2;
import pt.uminho.ceb.biosystems.transyt.utilities.enumerators.Compartments;
import pt.uminho.ceb.biosystems.transyt.utilities.files.JSONFilesUtils;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;

public class Test {

	//	public static void main(String[] args) {
	//		
	//		Compartments[] compartmentsList = Compartments.values();
	//
	//		ProcessCompartments.findCompartmentsRelativePosition("DNA (in the phage capsid)", "DNA (in the bacterial cytoplasm)", compartmentsList);
	//		
	//	}
	//	
	public static void main(String[] args) {



		WebDriver driver = new HtmlUnitDriver();

		// And now use this to visit the website
		driver.get("https://www.ncbi.nlm.nih.gov/gene/?term=STER_A2");
		

		// Find the text input element by its name
//		WebElement element = driver.findElement(By.name("sequence"));			//text area
		//				element.sendKeys(query);
		//
		//				element = driver.findElement(By.xpath("/html/body/form/input[2]"));		//check prediction for batch
		//				element.click();
		//
		//				element = driver.findElement(By.xpath("/html/body/form/input[3]"));		//uncheck Signal peptide predictions
		//				element.click();
		//
		//				element = driver.findElement(By.xpath("/html/body/form/input[7]"));		//email
		//				element.click();
		//				element.sendKeys(email);
		//
		//				element = driver.findElement(By.xpath("/html/body/form/input[8]"));		//run prediction
		//				element.click();
		
		System.out.println(driver.getCurrentUrl());
		
		System.out.println("AQUI " + driver.getTitle());
		
//		System.out.println(driver.getPageSource());
		
	}
		
}
