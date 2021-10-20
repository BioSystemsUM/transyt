package pt.uminho.ceb.biosystems.transyt.scraper.tcdb.tcdbTransportTypesRetriever;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import pt.uminho.ceb.biosystems.transyt.utilities.capsules.ReactionContainer;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.TcNumberContainer;
import pt.uminho.ceb.biosystems.transyt.utilities.connection.LinkConnection;
import pt.uminho.ceb.biosystems.transyt.utilities.enumerators.Enumerators.TypeOfTransporter;

/**
 * @author Davide
 *
 */
public class FindTransportTypes {

	public static final int LIMIT = 10;
	private static final String URL = "http://www.tcdb.org/search/result.php?tc=";
	public static final List<String> REVERSIBLES =  List.of ("⇌", "⇌&nbsp;", "&harr;", "&#8652;");  
	public static final List<String> IRREVERSIBLES =  List.of ("&rarr;", "%u21CC");
	public static final List<String> REVERSIBLES_IMAGE =  List.of ("<img src='arrow.gif' alt='' />", "<IMG SRC='arrow.gif'>", 
			"<img src='http://saier-144-209.ucsd.edu/tcCurrent/pt.uminho.ceb.biosystems.transyt.scraper.tcdb/arrow.gif' alt='' />");  
	public static final List<String> IRREVERSIBLES_IMAGE =  List.of ("<img src='arrows/ATP.gif' alt='' />", "<img src='pmf.gif' alt='' />"); 

	/**
	 * Mathod to retrieve all information about all tcNumber (*.*.* format) from TCDB.
	 * 
	 * @return
	 * @throws IOException
	 * @throws Exception
	 */
	public static Map<String, TcNumberContainer> getAllTCNumbersInformation(Set<String> toSearch) throws IOException, Exception{

		Map<String, TcNumberContainer> data = new HashMap<>();

//		Set<String> toSearch = TcdbExplorer.getTcNumbers();
//
////		Set<String> toSearch = new HashSet<>();
//
////		toSearch.add("2.A.37");

		List<String> searched = new ArrayList<>();

		List<String> failed = new ArrayList<>();

		int attempt = 0;
		int lastProgress = -1;

		for(String tc : toSearch) {

			LinkConnection conn = new LinkConnection();

			boolean found = false;
			attempt = 0;

			while(attempt < LIMIT && !found) {

				if(conn.getCodeConnection(URL.concat(tc)) == 200){

					data.put(tc, getTcContainer(conn.getPage(), tc));

					searched.add(tc);

					found = true;

					TimeUnit.SECONDS.sleep(1);

				}
				else {
					attempt++;
					TimeUnit.SECONDS.sleep(10);
				}

			}

			int progress = ((searched.size()+failed.size())*100)/toSearch.size();

			if(progress > lastProgress){

				lastProgress = progress;
				System.out.println(progress + " % search complete" );
			}	

			if(attempt == LIMIT && !found){

				System.out.println("results not found for query: " + URL.concat(tc));
				failed.add(tc);
			}

		}

		System.out.println("FAILED: " + failed.size() + "\t" + failed);

		return data;
	}

	/**
	 * Retrieve the information of each tcnumber.
	 * 
	 * @param in
	 * @return
	 * @throws IOException 
	 */
	private static TcNumberContainer getTcContainer(BufferedReader in, String tc) throws IOException {

		TcNumberContainer container = new TcNumberContainer();


		String html;

		String family = "";
		String superFamily = "";
		ReactionContainer reaction = null;

		while ((html = in.readLine()) != null){

			Document doc = Jsoup.parse(html);
			String text = doc.body().text().trim();

			try {

				if(html.contains("(in)")) {
//					System.out.println(html);
//					System.out.println(text);
					
				}
				
				if((html.contains("<p align='center'>") && !html.contains("class=\"description\">") && !html.contains("<p align='center'><strong>") || ( html.contains("is: </p>") || html.contains("is:</p>"))) 
						&& (text.contains("(") || text.contains("+")) 
						&& !text.equalsIgnoreCase("(The carrier-mediated mode)")
						&& !text.equalsIgnoreCase("(The channel-mediated mode)")) {

					reaction = getReactionFromText(html, text, tc);

					reaction.setTransportType(findTypeOfTransport(reaction)); 

					container.addReaction(reaction);

				}

				if(html.contains("result-superfam") && superFamily.isEmpty()) 
					superFamily = text.split(": ")[1];

				if((html.contains("class=\"description\"") || html.contains("<strong>")) && family.isEmpty() && text.matches("\\d\\.\\w\\.\\d+\\.*.*")) {
					family = text.split("\\.\\d+.?\\s+")[1];

				}
			} 
			catch (Exception e) {

				System.out.println(tc);
				System.out.println(text);
				System.out.println(html);

				e.printStackTrace();
			}

		}

		if(!family.isEmpty()) {
			if(family.substring(family.length() - 1).equals("."))
				family = family.substring(0, family.length()-1);

			container.setFamily(family);
		}

		if(!superFamily.isEmpty()) {
			if(superFamily.substring(superFamily.length() - 1).equals("."))
				superFamily = superFamily.substring(0, superFamily.length()-1);

			container.setSuperFamily(superFamily);
		}

		return container;
	}


	/**
	 * Check type of transport based on the reaction.
	 * 
	 * @param text
	 * @return
	 */
	private static TypeOfTransporter findTypeOfTransport(ReactionContainer reaction) {

		//		String reactantCompartments = "";
		//		String productCompartments = "";

		TypeOfTransporter transport = null;

		String reactant = reaction.getReactant();
		String product = reaction.getProduct();

		//		System.out.println(reactant);
		//		System.out.println(product);


		String[] react = reactant.split("\\s+");
		String[] prod = product.split("\\s+");

		Set<String> reactComp = new HashSet<>();
		Set<String> prodComp = new HashSet<>();

		int reactCompartmentsCounter = 0;
		int prodCompartmentsCounter = 0;

		//		System.out.println(react.length);
		//		System.out.println(prod.length);

		boolean skip = false;
		boolean expectNext = false;
		String temp = "";

		for(int i = 0; i < react.length; i++) {

			if(react[i].contains("(or") && !react[i].contains(")"))
				skip = true;

			else if((react[i].contains("(") || expectNext) && !skip ) {

				if(expectNext && react[i].contains("(")) {

					expectNext = false;
					temp = "";
					skip = true;
				}

				if(!react[i].subSequence(0, 1).equals("(") && !expectNext) 
					react[i] = react[i].split("\\(")[1];

				if(react[i].contains(")") && !skip) {

					reactComp.add(temp.concat(react[i].replaceAll("[()]", "").trim()));
					expectNext = false;
					temp = "";
					reactCompartmentsCounter++;
				}
				else {
					temp = temp.concat(temp.concat(react[i].replaceAll("[()]", "").trim()).concat(" "));
					expectNext = true;
				}

				//				if(skip && react[i].contains(")"))
				//					skip = false;
			}

			if(skip && react[i].contains(")"))
				skip = false;
		}


		skip = false;
		expectNext = false;
		temp = "";


		for(int i = 0; i < prod.length; i++) {

			if(prod[i].contains("(or") && !prod[i].contains(")"))
				skip = true;

			else if((prod[i].contains("(") || expectNext) && !skip) {

				//				System.out.println(prod[i] + "\t" + expectNext);

				if(expectNext && prod[i].contains("(")) {

					expectNext = false;
					temp = "";
					skip = true;
				}

				if(!prod[i].subSequence(0, 1).equals("(") && !expectNext) 
					prod[i] = prod[i].split("\\(")[1];

				if(prod[i].contains(")") && !skip) {

					prodComp.add(temp.concat(prod[i].replaceAll("[()]", "").trim()));
					expectNext = false;
					temp = "";
					prodCompartmentsCounter++;
				}
				else {
					temp = temp.concat(temp.concat(prod[i].replaceAll("[()]", "").trim()).concat(" "));
					expectNext = true;
				}
			}

			if(skip && prod[i].contains(")"))
				skip = false;
		}

		//		System.out.println(reactCompartmentsCounter + "\t" + reactComp);
		//		System.out.println(prodCompartmentsCounter + "\t" + prodComp);

		//		System.out.println(reactComp);

		if(reactComp.size() == 1) {

			transport = TypeOfTransporter.Uniport;

			if(reactCompartmentsCounter > 1 || prodCompartmentsCounter > 1 || reactant.contains("and") || product.contains("and"))
				transport = TypeOfTransporter.Symport;
		}
		else
			transport = TypeOfTransporter.Antiport;


		//		if(reactant.contains("(in)") && reactant.contains("(out)"))
		//			reactantCompartments = "BOTH";
		//
		//		else if(reactant.contains("(in)"))
		//			reactantCompartments = "IN";
		//
		//		else if(reactant.contains("(out)"))
		//			reactantCompartments = "OUT";
		//
		//
		//		if(product.contains("(in)") && product.contains("(out)"))
		//			productCompartments = "BOTH";
		//
		//		else if(product.contains("(in)"))
		//			productCompartments = "IN";
		//
		//		else if(product.contains("(out)"))
		//			productCompartments = "OUT";

		//		if((productCompartments.equals("OUT") && reactantCompartments.equals("IN")) || (productCompartments.equals("IN") && reactantCompartments.equals("OUT"))) {
		//
		//			transport = TypeOfTransporter.Uniport;
		//
		//			if(product.contains("+") || reactant.contains("+"))
		//				transport = TypeOfTransporter.Symport;
		//		}
		//
		//		if(productCompartments.equals("BOTH") ||  reactantCompartments.equals("BOTH"))
		//			transport = TypeOfTransporter.Antiport;

		return transport;
	}


	/**
	 * Get the reaction content in the correct format.
	 * 
	 * @param text
	 * @return
	 */
	private static ReactionContainer getReactionFromText(String html, String text, String tc) {

		try {
			
			if(html.contains("is: </p>")) {
				html = html.split("is: </p>")[1];
				text = text.split("is: ")[1];
			}
			
			else if(html.contains("is:</p>")) {
				html = html.split("is:</p>")[1];
				text = text.split("is:")[1];
			}

			System.out.println(text);
			System.out.println(html);

			String reactant = "";
			String product = "";

			boolean rev = false;

			if(html.contains("<img") || html.contains("&rarr;") || html.contains("<IMG") || html.contains("&#8652;")) {

				System.out.println("entrou");

				String value = "";

				for(String sign : REVERSIBLES_IMAGE) {

					if(html.contains(sign)) {
						value = sign;
						rev = true;
					}
				}

				if(value.isEmpty()) {

					for(String sign : IRREVERSIBLES_IMAGE) {

						if(html.contains(sign)) 
							value = sign;
					}
				}

				if(!value.isEmpty()) {
					for(String arrow : REVERSIBLES)
						html = html.replaceAll(arrow, "");

					for(String arrow : IRREVERSIBLES)
						html = html.replaceAll(arrow, "");
				}
				else {

					for(String sign : REVERSIBLES) {

						if(html.contains(sign)) {
							value = sign;
							rev = true;
						}
					}

					if(value.isEmpty()) {

						for(String sign : IRREVERSIBLES) {

							if(html.contains(sign)) 
								value = sign;
						}
					}
				}

				System.out.println("value" + value);

				String[] reaction = html.split(value);

				Document doc = Jsoup.parse(reaction[0]);
				reactant = doc.body().text().trim();

				if(reaction[1].length() > 8) {
					if(reaction[1].contains("<br />") && !reaction[1].substring(0, 8).contains("<br />"))
						reaction[1] = reaction[1].split("<br />")[0].trim();
				}

				doc = Jsoup.parse(reaction[1]);
				product = doc.body().text().trim();

			}
			else {

				String value = "";

				for(String sign : REVERSIBLES) {

					if(text.contains(sign)) {
						value = sign;
						rev = true;
					}
				}

				if(value.isEmpty()) {

					for(String sign : IRREVERSIBLES) {

						if(text.contains(sign)) 
							value = sign;
					}
				}
				//			System.out.println(text);
				//			System.out.println("VALUE " + value);

				System.out.println("value" + value);


				reactant = text.split(value)[0].trim();
				product = text.split(value)[1].trim();

				//			System.out.println(product);
				//			System.out.println(reactant);
			}
			
			
			if(reactant.contains(": "))
				reactant = reactant.split(": ")[1];
			else if(reactant.contains(" - "))
				reactant = reactant.split(" - ")[1];


			reactant = reactant.replaceAll("\\(\\w\\)", "").trim();

			System.out.println("AQUI" + product);

			if(product.substring(product.length() - 1).equals("."))
				product = product.substring(0, product.length()-1);




			return new ReactionContainer(reactant, product, rev);
		} 
		catch (Exception e) {
			System.out.println(tc);
			System.out.println(html);
			System.out.println(text);
			e.printStackTrace();
		}

		return null;
	}
}
