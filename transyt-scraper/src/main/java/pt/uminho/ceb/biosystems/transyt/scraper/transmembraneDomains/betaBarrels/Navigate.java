package pt.uminho.ceb.biosystems.transyt.scraper.transmembraneDomains.betaBarrels;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

@Deprecated
public class Navigate {

	public static void main(String[] args) throws InterruptedException {
		// Create a new instance of the html unit driver
		// Notice that the remainder of the code relies on the interface, 
		// not the implementation.
		WebDriver driver = new HtmlUnitDriver();

		// And now use this to visit Google
		driver.get("http://195.251.108.230/PRED-TMBB2/");

		// Find the text input element by its name
		WebElement element = driver.findElement(By.name("sequence"));

		// Enter something to search for
		element.sendKeys(">2K4T_A Chain A, Solution Structure Of Human Vdac-1 In Ldao Micelles\nMAVPPTYADLGKSARDVFTKGYGFGLIKLDLKTKSENGLEFTSSGSANTETTKVTGSLETKYRWTEYGLTFTEKWNTDNTLGTEITVEDQLARGLKLTFDSSFSPNTGKKNAKIKTGYKREHINLGCDMDFDIAGPSIRGALVLGYEGWLAGYQMNFETAKSRVTQSNFAVGYKTDEFQLHTNVNDGTEFGGSIYQKVNKKLETAVNLAWTAGNSNTRFGIAAKYQIDPDACFSAKVNNSSLIGLGYTQTLKPGIKLTLSALLDGKNVNAGGHKLGLGLEFQALEHHHHHH\n"+
">5JDP_A Chain A, E73v Mutant Of The Human Voltage-dependent Anion Channel\nSAVPPTYADLGKSARDVFTKGYGFGLIKLDLKTKSENGLEFTSSGSANTETTKVTGSLETKYRWTEYGLTFTVKWNTDNTLGTEITVEDQLARGLKLTFDSSFSPNTGKKNAKIKTGYKREHINLGCDMDFDIAGPSIRGALVLGYEGWLAGYQMNFETAKSRVTQSNFAVGYKTDEFQLHTNVNDGTEFGGSIYQKVNKKLETAVNLAWTAGNSNTRFGIAAKYQIDPDACFSAKVNNSSLIGLGYTQTLKPGIKLTLSALLDGKNVNAGGHKLGLGLEFQARS\n"+
">2JK4_A Chain A, Structure Of The Human Voltage-Dependent Anion Channel\nMRGSAVPPTYADLGKSARDVFTKGYGFGLIKLDLKTKSENGLEFTSSGSANTETTKVTGSLETKYRWTEYGLTFTEKWNTDNTLGTEITVEDQLARGLKLTFDSSFSPNTGKKNAKIKTGYKREHINLGCDMDFDIAGPSIRGALVLGYEGWLAGYQMNFETAKSRVTQSNFAVGYKTDEFQLHTNVNDGTEFGGSIYQKVNKKLETAVNLAWTAGNSNTRFGIAAKYQIDPDACFSAKVNNSSLIGLGYTQTLKPGIKLTLSALLDGKNVNAGGHKLGLGLEFQARSHHHHHH\n"+
">5XDO_A Chain A, Crystal Structure Of Human Voltage-dependent Anion Channel 1 (hvdac1) In C222 Space Group\nMRGSHHHHHHGSMAVPPTYADLGKSARDVFTKGYGFGLIKLDLKTKSENGLEFTSSGSANTETTKVTGSLETKYRWTEYGLTFTEKWNTDNTLGTEITVEDQLARGLKLTFDSSFSPNTGKKNAKIKTGYKREHINLGCDMDFDIAGPSIRGALVLGYEGWLAGYQMNFETAKSRVTQSNFAVGYKTDEFQLHTNVNDGTEFGGSIYQKVNKKLETAVNLAWTAGNSNTRFGIAAKYQIDPDACFSAKVNNSSLIGLGYTQTLKPGIKLTLSALLDGKNVNAGGHKLGLGLEFQA\n");

		System.out.println(driver.getCurrentUrl());
		
		element = driver.findElement(By.xpath("/html/body/form/input[2]"));
		
		element.click();
		
		System.out.println(element.isSelected());
		
		element = driver.findElement(By.xpath("/html/body/form/input[3]"));
		
		element.click();
		
		System.out.println(element.isSelected());
		
		// Now submit the form. WebDriver will find the form for us from the element
//		element.submit();
//		driver.findElement(By.tagName("Run prediction")).click();
		WebElement element2 = driver.findElement(By.xpath("/html/body/form/input[7]"));
		
		element2.click();
		
//		TimeUnit.SECONDS.sleep();
		 
		System.out.println(driver.getCurrentUrl());
//		System.out.println(driver.getPageSource());
		
//		System.out.println(driver.getCurrentUrl());
		
		// Check the title of the page
		System.out.println("Page title is: " + driver.getTitle());
	}
}
