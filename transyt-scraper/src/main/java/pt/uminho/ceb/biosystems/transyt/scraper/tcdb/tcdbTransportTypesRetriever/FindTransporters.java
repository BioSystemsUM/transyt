package pt.uminho.ceb.biosystems.transyt.scraper.tcdb.tcdbTransportTypesRetriever;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.uminho.ceb.biosystems.merlin.utilities.Pair;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.ReactionContainer;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.TcNumberContainer;
import pt.uminho.ceb.biosystems.transyt.utilities.connection.LinkConnection;
import pt.uminho.ceb.biosystems.transyt.utilities.connection.TcdbExplorer;
import pt.uminho.ceb.biosystems.transyt.utilities.enumerators.Enumerators.TypeOfTransporter;
import pt.uminho.ceb.biosystems.transyt.utilities.files.FilesUtils;
import pt.uminho.ceb.biosystems.transyt.utilities.files.JSONFilesUtils;

/**
 * @author Davide
 *
 */
public class FindTransporters {

	public static final int LIMIT = 5;
	public static final int ALL_SEARCH_LIMIT = 2;
	public static final int MAX_REACTANT_CHAR = 140;

	public static final List<String> REVERSIBLES =  List.of ("⇌", "⇌&nbsp;", "&harr;", "&#8652;", "⇋");  
	public static final List<String> IRREVERSIBLES =  List.of ("&rarr;", "%u21CC", "%u2192", "--&gt;", "&rightarrow;", "&AElig;", 
			"&agrave;");
	public static final List<String> REVERSIBLES_IMAGE =  List.of ("<img src='arrow.gif' alt='' />", "<IMG SRC='arrow.gif'>", 
			"<img src='http://saier-144-209.ucsd.edu/tcCurrent/pt.uminho.ceb.biosystems.transyt.scraper.tcdb/arrow.gif' alt='' />", "<img src='../pt.uminho.ceb.biosystems.transyt.scraper.tcdb/arrow.gif' alt='' />",
			"<img src='../pt.uminho.ceb.biosystems.transyt.scraper.tcdb/arrow.gif' alt='' width='20' height='13' />");  
	public static final List<String> IRREVERSIBLES_IMAGE =  List.of ("<img src='arrows/ATP.gif' alt='' />", "<img src='pmf.gif' alt='' />",
			"<img src='../../search/pmf.gif' alt='' />", "<img src='arrows/PtsIH.gif' alt='' />",
			"<img src='atpgtppmf.gif' alt='' />", "<sub><img src='../../images/upload/eq.gif' alt='' width='47' height='25' />"); 

	public static final List<String> PMF =  List.of ("<img src='../../search/pmf.gif' alt='' />", "<img src='atpgtppmf.gif' alt='' />"); 
	public static final List<String> ATP =  List.of ("<img src='atpgtppmf.gif' alt='' />"); 
	public static final List<String> GTP =  List.of ("<img src='atpgtppmf.gif' alt='' />"); 
	public static final List<String> GET =  List.of ("<sub><img src='../../images/upload/eq.gif' alt='' width='47' height='25' />"); 

	public static final String PATH = FilesUtils.getBackupFilesDirectory().concat("TCFilesBackup");

	private static final Logger logger = LoggerFactory.getLogger(FindTransporters.class);

	/**
	 * Mathod to store all information about all tcNumber (*.*.* format) from TCDB.
	 * 
	 * @return
	 * @throws IOException
	 * @throws Exception
	 */
	public static void saveAllTCFamiliesInformation(Set<String> toSearch) throws IOException, Exception{

		Map<String, TcNumberContainer> data = new HashMap<>();

		File directory = new File(PATH);

		if(!directory.exists())
			directory.mkdir();

		String path = PATH.concat(FilesUtils.generateFolderName("/version_"));

		directory = new File(path);

		if(!directory.exists())
			directory.mkdir();

		List<String> searched = new ArrayList<>();

		List<String> failed = new ArrayList<>();

		logger.info("Searching family specific information...");

		int attempt = 0;
		int allAttempt = 0;
		boolean continueSearch = true;

		while(continueSearch) {

			int lastProgress = -1;

			for(String tc : toSearch) {

				try {
					OutputStream out = new FileOutputStream(path.concat(tc).concat(".txt"));

					LinkConnection conn = new LinkConnection();

					boolean found = false;
					attempt = 0;

					while(attempt < LIMIT && !found) {

						if(conn.getCodeConnection(TcdbExplorer.TCDB_TCNUMBER_URL.concat(tc)) == 200){

							FilesUtils.webPageSaver(conn.getPageOpenStream(), out);

							//					data.put(tc, getTcContainer(conn.getPage(), tc, data));

							data.putAll(getTcContainer(conn.getPage(), tc));

							searched.add(tc);

							found = true;

							TimeUnit.MILLISECONDS.sleep(500);

						}
						else {
							attempt++;
							TimeUnit.SECONDS.sleep(10);
						}

					}

					int progress = ((searched.size()+failed.size())*100)/toSearch.size();

					if(progress > lastProgress){

						lastProgress = progress;
						String message = progress + " % search complete";
						logger.info(message);
					}	

					if(attempt == LIMIT && !found){

						logger.warn("results not found for query: " + TcdbExplorer.TCDB_TCNUMBER_URL.concat(tc));
						failed.add(tc);
					}
				} 
				catch (Exception e) {
					failed.add(tc);
					logger.trace("StackTrace {}",e);
				}
			}

			if(failed.size() > 0 && allAttempt < ALL_SEARCH_LIMIT) {
				allAttempt++;

				toSearch = new HashSet<>(failed);
				failed = new ArrayList<>();

				logger.info("Retrying search of previously failed queries. Attempt nr {}", allAttempt);
			}
			else {
				continueSearch = false;
			}
		}

		if(failed.size() > 0)
			logger.warn("The following queries failed: {}", failed.toString());

		JSONFilesUtils.writeJSONtcFamilyReactions(data);
	}

	/**
	 * Retrieve the information of each tcnumber.
	 * 
	 * @param in
	 * @return
	 * @throws IOException 
	 */
	private static Map<String, TcNumberContainer> getTcContainer(BufferedReader in, String tc) throws IOException {

		Map<String, TcNumberContainer> tcMap = new HashMap<>();

		TcNumberContainer container = new TcNumberContainer();

		String html;

		String originalTC = tc;

		String family = "";
		String superFamily = "";
		ReactionContainer reaction = null;

		boolean expectReaction = false;
		boolean expectNextInput = false;

		String previousText = "";
		String previousHtml = "";

		while ((html = in.readLine()) != null){

			Document doc = Jsoup.parse(html);
			String text = doc.body().text().trim();

			//			System.out.println(text);
			//			System.out.println(html);

			try {

				//				if(((html.contains("<p align='CENTER'>") || html.contains("<p align='center'>")) && !html.contains("class=\"description\">") && !html.contains("<p align='center'><strong>") 
				//						|| ( html.contains("is: </p>") || html.contains("is:</p>"))) 
				//						&& (text.contains("(")) 
				//						&& !text.equalsIgnoreCase("(The carrier-mediated mode)")
				//						&& !text.equalsIgnoreCase("(The channel-mediated mode)")) {

				if(html.contains("<strong>") && text.matches("\\d\\.\\w\\.\\d+\\.\\d+\\s+.*")) {
					tc = text.split("\\s+")[0];
					container = new TcNumberContainer();

				}

				for(String symbol : REVERSIBLES) {
					if(html.contains(symbol))
						expectReaction = true;
				}

				if(!expectReaction) {
					for(String symbol : IRREVERSIBLES) {
						if(html.contains(symbol))
							expectReaction = true;
					}
				}

				if(!expectReaction) {
					for(String symbol : IRREVERSIBLES_IMAGE) {
						if(html.contains(symbol))
							expectReaction = true;
					}
				}

				if(!expectReaction) {
					for(String symbol : REVERSIBLES_IMAGE) {
						if(html.contains(symbol))
							expectReaction = true;
					}
				}

				//				System.out.println(expectNextInput);

				if((expectReaction || expectNextInput) && !html.contains("1.B.20#ref309\">Poole <em>et al</em>., 1988</a>)") && !html.contains("--&gt; 4 TMSs --&gt; 8 TMSs --&gt; 7 TMSs --&gt;") && !html.contains("&bull") 
						&& !html.contains("to give 10 TMS proteins: 2 &rarr; 4 &rarr; 5 &rarr; 10") && !html.contains("(<a class=\"reflink\" href=\"/search/result.php?tc=3.E.1#ref2025\">Royant <em>et al</em>., 2001</a>)")) {		//hard coded! sorry

					if(expectNextInput) {
						html = previousHtml.concat(" ").concat(html);
						text = previousText.concat(" ").concat(text);
					}

					expectNextInput = false;

					reaction = getReactionFromText(html, text, tc);

					if(reaction == null) {

						expectNextInput = true;

						previousHtml = html;
						previousText = text;

					}
					else {
						TypeOfTransporter type;

						type = findTypeOfTransport(reaction, tc);

						if(type.equals(TypeOfTransporter.Default)) {

							reaction.setProduct(reaction.getProduct().concat(" (out)"));
							reaction.setReactant(reaction.getReactant().concat(" (in)"));

							reaction.addProperty("reaction edited", "yes");

							type = TypeOfTransporter.Uniport;
						}

						reaction.setTransportType(type);

						reaction.setProduct(reaction.getProduct().replace("S ", "Solute "));	//avoids the confusion of the algorithm with Sulfur
						reaction.setReactant(reaction.getReactant().replace("S ", "Solute "));

						if((reaction.getReactant().length() > 0 && reaction.getReactant().length() < MAX_REACTANT_CHAR) && //attempt to ignore false positives
								(reaction.getProduct().length() > 0 && reaction.getProduct().length() < MAX_REACTANT_CHAR)) {

							container.addReaction(reaction);
						}
					}

				}

				if(html.contains("result-superfam") && superFamily.isEmpty()) 
					superFamily = text.split(": ")[1];

				if((html.contains("class=\"description\"") || html.contains("<strong>")) && family.isEmpty() && text.matches("\\d\\.\\w\\.\\d+\\.*.*")) {
					family = text.split("\\.\\d+.?\\s+")[1];

				}
				tcMap.put(tc, container);

				expectReaction = false;
			} 
			catch (Exception e) {

				logger.trace(originalTC);
				logger.trace(text);
				logger.trace(html);

				logger.trace("StrackTrace: {}", e);
			}

		}

		for(String key : tcMap.keySet()) {

			TcNumberContainer newContainer = tcMap.get(key);

			if(!family.isEmpty()) {
				if(family.substring(family.length() - 1).equals("."))
					family = family.substring(0, family.length()-1);

				newContainer.setFamily(family);
			}

			if(!superFamily.isEmpty()) {
				if(superFamily.substring(superFamily.length() - 1).equals("."))
					superFamily = superFamily.substring(0, superFamily.length()-1);

				newContainer.setSuperFamily(superFamily);
			}

			tcMap.put(key, newContainer);

		}

		return tcMap;
	}




	////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Check type of transport based on the reaction.
	 * 
	 * @param text
	 * @return
	 */
	public static TypeOfTransporter findTypeOfTransport(ReactionContainer reaction, String tc) {

		String reactant = reaction.getReactant();
		String product = reaction.getProduct();

		if(tc.matches("4\\.A\\..+"))
			return TypeOfTransporter.PEPdependent;

		else if(reactant.contains("ATP") && reactant.contains("Coenzymes A"))
			return TypeOfTransporter.BiochemicalCoA;

		else if(reactant.contains("ATP"))
			return TypeOfTransporter.BiochemicalATP;

		else if(reactant.contains("NADH"))
			return TypeOfTransporter.RedoxNADH;

		else if (tc.matches("5\\.A\\.3\\..+") || tc.matches("3\\.D\\.9\\..+") ||
				tc.matches("3\\.D\\.3\\..+") || tc.matches("3\\.D\\.4\\..+") || tc.matches("3\\.D\\.10\\..+")
				|| tc.matches("5\\.B\\.2\\..+"))  // create a config file to apply this
			return TypeOfTransporter.Redox;

		Pair<Set<String>, Set<String>> reactants = compartmentsParser(reactant.toLowerCase());
		Pair<Set<String>, Set<String>> products = compartmentsParser(product.toLowerCase());

		Set<String> reactantsMetab = reactants.getA();
		Set<String> reactantsComp = reactants.getB();
		Set<String> productsMetab = products.getA();
		Set<String> productsComp = products.getB();

		if(reactantsComp.isEmpty() && productsComp.isEmpty())
			return TypeOfTransporter.Default;

		if(reactantsMetab.contains("hv") || reactantsMetab.contains("light") ||
				reactantsMetab.contains("photon") || reactantsMetab.contains("hnu"))
			return TypeOfTransporter.Light;

		if(reactantsMetab.containsAll(productsMetab) && productsMetab.containsAll(reactantsMetab)) {

			if((reactantsMetab.size() == 1 && productsMetab.size() == 1))
				return TypeOfTransporter.Uniport;

			else if((reactantsMetab.size() > reactantsComp.size()) && reactantsComp.size() == 1) {

				if(reactantsMetab.size() == 2 && reaction.getReactant().matches(".*\\s+[^\\+]*.*\\(\\s*and.*"))	//false positive. Ex: "Ca2+ (and other cations) (out)"
					return TypeOfTransporter.Uniport;

				return TypeOfTransporter.Symport;
			}
			//			else if(reactantsComp.size() == 1 && reactantsMetab.size() == 1)
			//				return TypeOfTransporter.Uniport;

			else if(reactantsComp.size() != productsComp.size())
				return null;

			return TypeOfTransporter.Antiport;

		}
		else
			return TypeOfTransporter.Biochemical;

	}

	/**
	 * Check type of transport based on the reaction (composed of metacyc ids only).
	 * 
	 * @param text
	 * @return
	 */
	public static TypeOfTransporter findTypeOfTransport2(ReactionContainer reaction, String tc) {

		String reactant = reaction.getReactant();
		String product = reaction.getProduct();

		if(reaction.getReaction().contains(ReactionContainer.MIDDLE_COMPARTMENT_TOKEN))
			return TypeOfTransporter.Biochemical;

		else if(tc.matches("4\\.A\\..+"))
			return TypeOfTransporter.PEPdependent;

		else if(reactant.contains(":ATP") && reactant.contains(":CO-A"))
			return TypeOfTransporter.BiochemicalCoA;

		else if(reactant.contains(":ATP") && !product.contains(":ATP"))
			return TypeOfTransporter.BiochemicalATP;

		else if(reactant.contains(":GTP") && !product.contains(":GTP"))
			return TypeOfTransporter.BiochemicalGTP;

		else if(reactant.contains(":NADH")) {
			return TypeOfTransporter.RedoxNADH;
		}
		else if (tc.matches("5\\.A\\.3\\..+") || tc.matches("3\\.D\\.9\\..+") ||
				tc.matches("3\\.D\\.3\\..+") || tc.matches("3\\.D\\.4\\..+") || tc.matches("3\\.D\\.10\\..+")
				|| tc.matches("5\\.B\\.2\\..+"))  // create a config file to apply this
			return TypeOfTransporter.Redox;

		Pair<Set<String>, Set<String>> reactants = compartmentsParser(reactant.toLowerCase());
		Pair<Set<String>, Set<String>> products = compartmentsParser(product.toLowerCase());

		Set<String> reactantsMetab = reactants.getA();
		Set<String> reactantsComp = reactants.getB();
		Set<String> productsMetab = products.getA();
		Set<String> productsComp = products.getB();

		if(reactantsComp.isEmpty() && productsComp.isEmpty())
			return TypeOfTransporter.Default;

		if(reactantsMetab.contains(":hv") || reactantsMetab.contains(":light") || 
				reactantsMetab.contains(":photon") || reactantsMetab.contains(":hnu"))
			return TypeOfTransporter.Light;

		if(reactantsMetab.containsAll(productsMetab) && productsMetab.containsAll(reactantsMetab)) {

			if((reactantsMetab.size() == 1 && productsMetab.size() == 1))
				return TypeOfTransporter.Uniport;

			else if((reactantsMetab.size() > reactantsComp.size()) && reactantsComp.size() == 1)
				return TypeOfTransporter.Symport;

			else if(reactantsComp.size() != productsComp.size())
				return null;

			return TypeOfTransporter.Antiport;
		}
		else
			return TypeOfTransporter.Biochemical;

	}

	/**
	 * Method to aux finding the transport types.
	 * 
	 * @param substances
	 * @return
	 */
	private static Pair<Set<String>, Set<String>> compartmentsParser(String query) {

		String[] substances = query.replaceAll("in the ", "").split(" \\+ ");

		Set<String> compartments = new HashSet<>();
		Set<String> metabolites = new HashSet<>();

		try {
			for(int i = 0; i < substances.length; i++) {

				//				System.out.println(substances[i]);

				String[] words = substances[i].split(" \\(");

				if(words.length == 1 && substances[i].contains("+(")) {
					substances[i] = substances[i].replace("+(", "+ (");
					words = substances[i].split(" \\(");
				}

				for(int j = 0; j < words.length; j ++) {

					String word = words[j];

					//					System.out.println(word);
					if(j == 0 && word.startsWith("(")) 
						metabolites.add(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase());

					else if(word.contains(")")) {

						if(word.contains("or ")) {

						}
						else if(word.contains("and ")) {

							word = word.replace("(and ", "");
							metabolites.add(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase());

						}
						else {

							compartments.add(word.replaceAll("\\)", "").toLowerCase());

						}
					}
					else {
						metabolites.add(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase());

					}
				}

			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		}

		//		Pair<Set<String>, Set<String>> pair = new Pair<>(metabolites, compartments);

		return new Pair<>(metabolites, compartments);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////



	/**
	 * Get the reaction content in the correct format.
	 * 
	 * @param text
	 * @return
	 */
	private static ReactionContainer getReactionFromText(String html, String text, String tc) {

		try {
			//			
			//			System.out.println(text);
			//			System.out.println(html);

			boolean original = true; 

			boolean pmf = false;
			boolean gtp = false;
			boolean energy = false;

			//			if(html.contains("+ (pmf)")) {
			//				html = html.replaceAll("\\+*\\s*(pmf)", "");
			//				text = text.replaceAll("\\+*\\s*(pmf)", "");
			//				
			//				pmf = true;
			//				original = false;
			//			}

			//			if(html.contains("and other")) {
			//				
			//				html = html.replaceAll("and other", "+");
			//				text = text.replaceAll("and other", "+");
			//				
			//				original = false;
			//			}

			if(text.matches("\\d\\.\\s*.+")){

				html = html.replaceAll("\\d\\.+", "");
				text = text.replaceAll("\\d\\.+", "");

				original = false;
			}

			if(html.contains("(outer surface of outer membrane)")) {

				html = html.replaceAll("\\(outer surface of outer membrane\\)", "(out)");
				text = text.replaceAll("\\(outer surface of outer membrane\\)", "(out)");

				original = false;
			}

			if(html.contains("translocation across")) {

				html = html.replaceAll("translocation across", "");
				text = text.replaceAll("translocation across", "");

				original = false;
			}

			if(html.contains("energy (GTP hydrolysis)")) {

				html = html.replaceAll("\\+*\\s*energy\\s\\(GTP hydrolysis\\)", "");
				text = text.replaceAll("\\+*\\s*energy\\s\\(GTP hydrolysis\\)", "");

				gtp = true;
				original = false;
			}

			if(html.contains("energy")) {
				html = html.replaceAll("\\+*\\s*energy", "");
				text = text.replaceAll("\\+*\\s*energy", "");

				energy = true;
				original = false;
			}

			if(html.contains("soutes")) {
				html = html.replaceAll("soutes", "solutes");
				text = text.replaceAll("soutes", "solutes");

				original = false;
			}

			if(html.contains("� ")) {
				html = html.replaceAll("� ", "");
				text = text.replaceAll("� ", "");

				original = false;
			}

			if(html.contains("large and small molecules")) {							//transform all these if into a cycle that searches for the entries in a map
				html = html.replaceAll("large and small molecules", "molecules");
				text = text.replaceAll("large and small molecules", "molecules");

				original = false;
			}

			if(html.contains("TA ")) {
				html = html.replaceAll("TA ", "Tail-anchored ");
				text = text.replaceAll("TA ", "Tail-anchored ");

				original = false;
			}

			if(html.contains("<sub><size=3>"))
				html = html.replaceAll("<sub><size=3>", " ");

			if(html.contains("is:</P>")) {
				html = html.split("is:</P>")[1];
				text = text.split("is:")[1];
			}
			else if(html.contains("is: </P>")) {
				html = html.split("is: </P>")[1];
				text = text.split("is:")[1];
			}
			else if(html.contains("is: </p>")) {
				html = html.split("is: </p>")[1];
				text = text.split("is:")[1];
			}
			else if(html.contains("is:</p>")) {
				html = html.split("is:</p>")[1];
				text = text.split("is:")[1];
			}
			else if(html.contains("are:  </p>")) {
				html = html.split("are:  </p>")[1];
				text = text.split("are:")[1];
			}

			String reactant = "";
			String product = "";

			boolean rev = false;

			String value = "";

			if(html.contains("<img") || html.contains("&rarr;") || html.contains("&harr;") || html.contains("<IMG") 
					|| html.contains("&#8652;") || html.contains("--&gt;") || html.contains("&rightarrow;") || html.contains("&AElig;")
					|| html.contains("&agrave;")) {

				if(html.contains("&nu;"))
					html = html.replace("&nu;", "v");

				//				System.out.println("entrou");

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

				//				System.out.println("value" + value);

				String[] reaction = html.split(value);

				Document doc = Jsoup.parse(reaction[0]);
				reactant = doc.body().text().trim();

				if(reaction[1].length() > 8) {
					if(reaction[1].contains("<br />") && !reaction[1].substring(0, 8).contains("<br />"))
						reaction[1] = reaction[1].split("<br />")[0].trim();
				}

				doc = Jsoup.parse(reaction[1]);
				product = doc.body().text().trim();

				if(product.length() < 2)
					return null;

			}
			else {

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

				//				System.out.println("value" + value);

				String[] auxText = text.split(value);

				if(auxText.length == 1)
					return null;

				reactant = auxText[0].trim();
				product = auxText[1].trim();

				//			System.out.println(product);
				//			System.out.println(reactant);
			}

			if(reactant.contains(": "))
				reactant = reactant.split(": ")[1];
			else if(reactant.contains(" - "))
				reactant = reactant.split(" - ")[1];

			if(product.contains("(e.g.,"))
				product = product.split("\\(e.g.,")[0];


			reactant = reactant.replaceAll("\\(\\w\\)", "").trim();

			//			System.out.println("AQUI" + product);

			//			if(product.substring(product.length() - 1).equals("."))
			//				product = product.substring(0, product.length()-1);

			if(product.contains("."))
				product = product.split("\\.")[0];


			//			System.out.println(reactant.contains(" out"));
			//			System.out.println(product.contains(" out"));

			if(reactant.contains(" out") || product.contains(" out")) {
				reactant = reactant.replaceAll(" in", " (IN)");
				reactant = reactant.replaceAll(" out", " (OUT)");

				product = product.replaceAll(" in", " (IN)");
				product = product.replaceAll(" out", " (OUT)");
			}

			if(reactant.contains("� _P98 pyrimidal structure�")) {
				reactant = reactant.replaceAll("� _P98 pyrimidal structure�", "");
				original = false;
			}
			else if(product.contains("� _P98 pyrimidal structure�")) {
				product = product.replaceAll("� _P98 pyrimidal structure�", "");
				original = false;
			}

			if(reactant.contains("pmf")) {
				reactant = reactant.replaceAll("\\+*\\s*\\(*pmf\\)*", "");

				pmf = true;
				original = false;
			}
			else if(product.contains("pmf")) {
				product = product.replaceAll("\\+*\\s*\\(*pmf\\)*", "");

				pmf = true;
				original = false;
			}

			if(reactant.split("\\s+")[0].matches("\\d+\\)"))
				reactant = reactant.substring(3);

			ReactionContainer container = new ReactionContainer(reactant, product, rev);

			if(PMF.contains(value) || pmf)
				container.addProperty("PMF", "yes");

			if(ATP.contains(value))
				container.addProperty("ATP", "yes");

			if(GTP.contains(value) || gtp)
				container.addProperty("GTP", "yes");

			if(GET.contains(value))
				container.addProperty("GET", "yes");

			if(energy)
				container.addProperty("energy", "yes");

			if(!original)
				container.addProperty("reaction string as retrieved from TCDB", "no");

			return container;
		} 
		catch (Exception e) {

			logger.trace(tc);
			logger.trace(text);
			logger.trace(html);

			logger.trace("StrackTrace: {}", e);
		}

		return null;
	}

}
