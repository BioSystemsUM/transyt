package pt.uminho.ceb.biosystems.transyt.utilities.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class Tests {

	public static int getCodeConnection() throws Exception{
//	     doTrustToCertificates();//  
	     URL url = new URL("http://www.tcdb.org/search/result.php?acc=p0afs1");
	     HttpURLConnection conn = (HttpURLConnection)url.openConnection(); 
//	     System.out.println("ResponseCode ="+conn.getResponseCode());
	     
	     BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
//	     String inputLine;
//	     
//	     String html = null;
//	     
//		 while ((inputLine = in.readLine()) != null){
//	    	 html = inputLine;
//   		 Document doc = Jsoup.parse(html);
//   		 String text = doc.body().text();
//   		 System.out.println(text);
//		}
	     
	    parseHTML(in);
	     
		return conn.getResponseCode();
	}
	
	private static Map<String, String> parseHTML(BufferedReader in) throws IOException {

		String html;
		
		String substrate = "";
		
		String description = "";
		
		Map<String, String> info = new HashMap<>();
		
		boolean searchComplete = false;
		boolean searchSubstrate = false;
		boolean searchTcNumber = false;
		boolean searchDescription = false;
		
		String auxDescription = "";
		
		while ((html = in.readLine()) != null && !searchComplete){
			
			Document doc = Jsoup.parse(html);
			String text = doc.body().text().trim();
			
			if(searchSubstrate == true){
				
				if(text.contains("database")){
					info.put("SUB", substrate);
					searchSubstrate = false;
					
					searchComplete = true;
				}
				else{
					if(!text.isEmpty()){
						substrate = substrate.concat(text);
					}
				}
				
			}
			else if(searchTcNumber == true && !text.isEmpty()){
				
				if(text.contains("Accession Number:")){
					
					searchDescription = false;
					searchTcNumber = false;
					
					info.put("DES", description.replace("-->", ""));
				}
				
				else if(searchDescription == false){
					
					String[] line = text.split("\\s");
					info.put("TC", line[0]);
					
					if(line.length > 1)
						description = description.concat(text.substring(line[0].length() + 1));
					
					auxDescription = new String(description);
					
					searchDescription = true;
				}
				else if(searchDescription == true){
					
					if(description.length() > 0){
						if(description.substring(description.length() - 1).equals("-")){

							description = description.substring(0, description.length()-1).concat(text);

						}
						else{
							description = description.concat(" ").concat(text);
						}
					}
					else{
						description = description.concat(" ").concat(text);
					}
					
					if(text.contains("{") || text.contains("}"))
						description = auxDescription;
					
				}
				
			}
			
			if(text.equals("Substrate"))
				searchSubstrate = true;
			
			else if(text.contains("See all members of the family"))
				searchTcNumber = true;
			
		}
		
		System.out.println(info);
		
		
		return info;
	}
	
}
