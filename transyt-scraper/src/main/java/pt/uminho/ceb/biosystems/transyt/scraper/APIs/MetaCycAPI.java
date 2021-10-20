package pt.uminho.ceb.biosystems.transyt.scraper.APIs;

import java.io.BufferedReader;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.uminho.ceb.biosystems.transyt.utilities.capsules.ReactionContainer;
import pt.uminho.ceb.biosystems.transyt.utilities.connection.LinkConnection;
import pt.uminho.ceb.biosystems.transyt.utilities.files.JSONFilesUtils;
import pt.uminho.ceb.biosystems.transyt.utilities.files.XMLFilesReader;
import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseCrossReference;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;

public class MetaCycAPI {

	public static final int LIMIT = 5;
	public static final int ALL_SEARCH_LIMIT = 2;
	public static final int DEFAULT_DELAY_MILLIS = 800;
	public static final int BATCH_SIZE = 250;

	protected static final String[] DATABASES = new String[] {"ECOLI", "META", "BSUB", "YEAST"};
	//	protected static final String[] DATABASES = new String[] {"META"};


	protected Map<String, Map<String, String>> internalIDsByAccession;
	protected Map<String, Map<String, ReactionContainer>> reactionsByProtein;

	private static final Logger logger = LoggerFactory.getLogger(MetaCycAPI.class);

	public MetaCycAPI(Set<String> toSearch){

		try {

			//			String s = "https://websvc.biocyc.org/ECOLI/foreignid?ids=UniProt:P54709,UniProt:P54708,UniProt:AXH81214.1,UniProt:Q8IL79,UniProt:F0SI03,UniProt:P54710,UniProt:Q769E5,UniProt:D3LPG3,UniProt:K0VHI2,UniProt:O81209,UniProt:Q4Q9L4,UniProt:P54715,UniProt:Q9HBJ8,UniProt:AKM80794.1,UniProt:Q8TL61,UniProt:P40100,UniProt:P16482,UniProt:Q9JI12,UniProt:P03170,UniProt:Q9LNV3,UniProt:Q9Y371,UniProt:P40107,UniProt:Q9VKP2,UniProt:A6DJS4,UniProt:P28472,UniProt:Q9UHM6,UniProt:Q0S719,UniProt:P39109,UniProt:Q0S718,UniProt:L0A2L0,UniProt:Q0S717,UniProt:R8RPP1,UniProt:P39108,UniProt:P39115,UniProt:B3QLC8,UniProt:P39111,UniProt:Q9X051,UniProt:Q9Y2W3,UniProt:Q9X053,UniProt:D6ST26,UniProt:Q9X050,UniProt:Q5P037,UniProt:V7IHL6,UniProt:W2U1N3,UniProt:E8VYH0,UniProt:Q8Y8W0,UniProt:O32261,UniProt:Q5P036,UniProt:M4QVE3,UniProt:Q99643,UniProt:Q6I681,UniProt:P0A9T8,UniProt:P0A9T4,UniProt:Q8Y8V9,UniProt:Q8Y8V8,UniProt:Q9Y2P4,UniProt:D7FG51,UniProt:WP_104228733.1,UniProt:XP_005656685.1 ,UniProt:Q9Y2P5,UniProt:WP_150583089.1,UniProt:B7MI53,UniProt:WP_052823361.1,UniProt:A9M262,UniProt:Q9JHE5,UniProt:A2RJJ9,UniProt:Q96NL1,UniProt:O32244,UniProt:P0A9U1,UniProt:Q02725,UniProt:O32241,UniProt:O32243,UniProt:D5EPC3,UniProt:Q8X638,UniProt:Q02728,UniProt:WP_048600991.1,UniProt:Q8REB9,UniProt:Q9Y2Q0,UniProt:P05706,UniProt:Q3KNW5,UniProt:B8H2Y5,UniProt:W2KVR5,UniProt:Q99624,UniProt:Q8YZW3,UniProt:WP_015691163.1,UniProt:O32273,UniProt:OLS18108,UniProt:D5SYL6,UniProt:Q50392,UniProt:P0A9R7,UniProt:W2RJE2"

			//			Set<String> toSearchAux = new HashSet<>();
			//
			//			int i =0;
			//
			//			for(String a : toSearch) {
			//				if(i < 300)
			//					toSearchAux.add(a);
			//				else
			//					break;
			//				i++;
			//			}
			//			System.out.println(toSearch.size());
			//			toSearch = new HashSet<>(toSearchAux);
			//			System.out.println(toSearch.size());

			//			toSearch = new HashSet<>();
			//			toSearch.add("P02916");

			Map<String, Map<String, String>> identifiers = convertUniprotToMetacycIds(toSearch);

			getReactionsToSearch(identifiers);

			JSONFilesUtils.writeJSONMetaCycInfo(this.reactionsByProtein);

		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		//		Thread thread1 = new Thread () {
		//			public void run () {
		//				
		//				try {
		//					int i = 0;
		//					
		//					while(i<15) {
		//						TimeUnit.SECONDS.sleep(1);
		//						System.out.println(i);
		//						i++;
		//					}
		//				} catch (InterruptedException e) {
		//					// TODO Auto-generated catch block
		//					e.printStackTrace();
		//				}
		//			}
		//		};
		//		Thread thread2 = new Thread () {
		//			public void run () {
		//				try {
		//					int i = 1000;
		//					
		//					while(i<1020) {
		//						TimeUnit.SECONDS.sleep(1);
		//						System.out.println(i);
		//						i++;
		//					}
		//				} catch (InterruptedException e) {
		//					// TODO Auto-generated catch block
		//					e.printStackTrace();
		//				}
		//			}
		//		};
		//		thread1.start();
		//		thread2.start();
		//		
		//		while(thread1.isAlive() || thread2.isAlive()) {
		//			TimeUnit.SECONDS.sleep(1);
		//			System.out.println("checking");
		//		}
		//		System.out.println("done");
	}

	private Map<String, Map<String, String>> convertUniprotToMetacycIds(Set<String> toSearch) throws InterruptedException{

		Set<String> searched = new HashSet<>();
		Set<String> failed = new HashSet<>();

		internalIDsByAccession = new HashMap<>();

		logger.info("Converting Uniprot accessions to MetaCyc identifiers using UniProt...");

		getCrossReferencesMetacycUniprot(toSearch);
		
		toSearch.removeAll(this.internalIDsByAccession.keySet());

		System.out.println(internalIDsByAccession.size());

		logger.info("Converting Uniprot accessions to MetaCyc identifiers using MetaCyc...");

		Set<String> queries = generateBatchesDistribution(toSearch);

		boolean continueSearch = true;

		while(continueSearch) {

			for(String orgID : DATABASES) {

				int lastProgress = -1;
				searched = new HashSet<>();

				logger.info("Searching identifiers for database: " + orgID);

				for(String query : queries){

					boolean found = false;
					int attempt = 0;

					while(attempt < LIMIT && !found){

						Instant start = Instant.now();

						try {
							String link = "https://websvc.biocyc.org/" + orgID + "/foreignid?ids=" + query;
							LinkConnection conn = new LinkConnection();
							int code = conn.getCodeConnection(link);
							if (code == 200){

								parseResultsForeingToMetaCycID(conn.getPage(), orgID);

								searched.add(query);
								found = true;

								int progress = ((searched.size()+failed.size())*100)/queries.size();

								if(progress > lastProgress){

									lastProgress = progress;
									String message = progress + " % search complete";
									logger.info(message);
								}
								applyWait(start);
							}
							else{
								System.out.println(link);
								System.out.println(code);
								attempt++;
								logger.warn("Retrying connection... Attempt nr: {}", attempt);
								TimeUnit.SECONDS.sleep(30);
							}
						}
						catch (ArrayIndexOutOfBoundsException e1) {

							attempt = LIMIT;
							e1.printStackTrace();
							logger.error("An error occurred while retrieving entry {}", query);
							logger.trace("StrackTrace: {}", e1);
						}
						catch (Exception e) {
							e.printStackTrace();
							attempt++;
							logger.warn("Retrying connection... Attempt nr: {}", attempt);
							TimeUnit.SECONDS.sleep(30);
							logger.trace("StrackTrace: {}", e);
						}
					}
				}
			}
			//			if(failed.size() > 0 && allAttempt < ALL_SEARCH_LIMIT) {
			//				allAttempt++;
			//
			//				queries = new HashSet<>(failed);
			//				failed = new HashSet<>();
			//
			//				logger.info("Retrying search of previously failed queries. Attempt nr {}", allAttempt);
			//			}
			//			else {
			continueSearch = false;
			//			}
		}
		
		System.out.println(internalIDsByAccession.size());
		
		return internalIDsByAccession;
	}

	/**
	 * Gets biocyc cross references from uniprot. Returns the ids not found.
	 * 
	 * @param toSearch
	 * @return
	 */
	private void getCrossReferencesMetacycUniprot(Set<String> toSearch) {

		List<UniProtEntry> entries = UniprotAPIExtension.getEntriesFromUniProtIDs(new ArrayList<>(toSearch), 0);

		for(UniProtEntry entry : entries) {
			String accession = entry.getPrimaryUniProtAccession().getValue();

			Map<String, String> ids = new HashMap<>();

			for(DatabaseCrossReference db : entry.getDatabaseCrossReferences()) {
				if(db.getDatabase().getName().equalsIgnoreCase("biocyc")) {

					String[] res = db.getPrimaryId().getValue().split(":");

					if(!ids.containsKey(res[0]))
						ids.put(res[0], res[1]);
				}
			}

			if(!ids.isEmpty())
				this.internalIDsByAccession.put(accession, ids);
		}
	}

	private void getReactionsToSearch(Map<String, Map<String, String>> identifiers) throws InterruptedException {

		this.reactionsByProtein = new HashMap<>();

		Set<String> searched = new HashSet<>();
		Set<String> failed = new HashSet<>();

		logger.info("Retrieving reactions...");

		boolean continueSearch = true;

		while(continueSearch) {

			int lastProgress = -1;

			for(String query : identifiers.keySet()){

				boolean found = false;
				int attempt = 0;

				while(attempt < LIMIT && !found){

					for(Entry<String, String> entry : identifiers.get(query).entrySet()) {
						try {
							String link = "https://websvc.biocyc.org/apixml?fn=reactions-of-enzyme&id=" + entry.getKey() + ":" + entry.getValue() + "&detail=full";

							Instant start = Instant.now();
							LinkConnection conn = new LinkConnection();
							
							int code = conn.getCodeConnection(link);

							if (code == 200){

								Map<String, ReactionContainer> reactions = XMLFilesReader.scrapeReactionInfo(conn.getPage(), query);

								if(reactions.isEmpty()) {	//begin search of reaction assigned to the encoding gene

									int geneAttempt = 0;
									String gene = null;

									while(!found && geneAttempt < LIMIT) {

										applyWait(start);
										start = Instant.now();

										link = "https://websvc.biocyc.org/apixml?fn=genes-of-protein&id=" + entry.getKey() + ":" + entry.getValue() + "&detail=low";
										code = conn.getCodeConnection(link);

										if(code == 200) {
											gene = XMLFilesReader.retrieveGeneId(conn.getPage());
											found = true;
										}
										else if(code == 404) { //not found, next entry
											found = true;
										}
										else {
											geneAttempt++;
											logger.error("Failed access to: " + link);
											TimeUnit.SECONDS.sleep(10);
										}
									}

									if(gene != null && !gene.isEmpty()) {

										geneAttempt = 0;
										found = false;

										while(!found && geneAttempt < LIMIT) {

											applyWait(start);
											start = Instant.now();

											link = "https://websvc.biocyc.org/apixml?fn=reactions-of-gene&id=" + gene + "&detail=full";
											code = conn.getCodeConnection(link);

											if(code == 200) {
												reactions = XMLFilesReader.scrapeReactionInfo(conn.getPage(), query);
												found = true;
											}
											else if(code == 404) { //not found, next entry
												found = true;
											}
											else {
												System.out.println(link);
												geneAttempt++;
												logger.error("Failed access to: " + link);
												TimeUnit.SECONDS.sleep(10);
											}
										}
									}
								}

								searched.add(query);
								found = true;

								int progress = ((searched.size()+failed.size())*100)/identifiers.size();

								if(progress > lastProgress){
									lastProgress = progress;
									String message = progress + " % search complete";
									logger.info(message);
								}

								if(this.reactionsByProtein.containsKey(query))
									this.reactionsByProtein.get(query).putAll(reactions);
								else
									this.reactionsByProtein.put(query, reactions);

								applyWait(start);
							}
							else if(code == 404) {
//								System.out.println(query);
//								System.out.println(link);
//								System.out.println();
								searched.add(query);
								found = true;
								applyWait(start);
							}
							else{
								System.out.println(link);
								attempt++;
								logger.warn("Retrying connection... Attempt nr: {}", attempt);
								TimeUnit.SECONDS.sleep(30);
							}
						}
						catch (ArrayIndexOutOfBoundsException e1) {

							e1.printStackTrace();
							attempt = LIMIT;
							logger.error("An error occurred while retrieving entry {}", query);
							logger.trace("StrackTrace: {}", e1);

						}
						catch (Exception e) {

							e.printStackTrace();
							attempt++;
							logger.warn("Retrying connection... Attempt nr: {}", attempt);
							TimeUnit.SECONDS.sleep(30);
							logger.trace("StrackTrace: {}", e);
						}
					}
				}

				if(attempt == LIMIT && !found){

					logger.warn("Results not found for query: {}", query);
					failed.add(query);
				}
			}

			//			if(failed.size() > 0 && allAttempt < ALL_SEARCH_LIMIT) {
			//				allAttempt++;
			//
			//				toSearch = new HashSet<>(failed);
			//				failed = new HashSet<>();
			//
			//				logger.info("Retrying search of previously failed queries. Attempt nr {}", allAttempt);
			//			}
			//			else {
			continueSearch = false;
			//			}
		}


	}

	public static void applyWait(Instant start) {

		try {
			long timeElapsed = Duration.between(start, Instant.now()).toMillis();
			if(timeElapsed < DEFAULT_DELAY_MILLIS)
				TimeUnit.MILLISECONDS.sleep(DEFAULT_DELAY_MILLIS - timeElapsed);
		} 
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get information from TCDB page.
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	private Map<String, String> parseResultsForeingToMetaCycID(BufferedReader in, String orgID) throws IOException {

		String html;

		while ((html = in.readLine()) != null){

			try {
				Document doc = Jsoup.parse(html);
				String text = doc.body().text().trim();

				String[] res = text.split("\\s+");

				if(res.length == 3) {
					if(res[1].equals("1")) {
						String query = res[0].split(":")[1];
						String internalID = res[2];
						if(internalID != null) {
							if(!this.internalIDsByAccession.containsKey(query))
								this.internalIDsByAccession.put(query, new HashMap<>());

							Map<String, String> ids = this.internalIDsByAccession.get(query);

							if(!ids.containsKey(orgID))
								ids.put(orgID, internalID);
						}
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}

	private Set<String> generateBatchesDistribution(Set<String> toSearch){
		Set<String> queries = new HashSet<>();
		Iterator<String> itereator = toSearch.iterator();

		String q = "";
		int i = 0;

		while(itereator.hasNext()) {

			String acc = itereator.next();

			q = q.concat("UniProt:"+acc);

			boolean save = false;

			if(i == BATCH_SIZE) {
				save = true;
			}
			else {
				if(itereator.hasNext())
					q = q + ",";
				else
					save =true;
			}

			if(save) {
				queries.add(q.replaceAll("\n", ""));
				i = -1;
				q = "";
			}
			i++;
		}
		return queries;
	}
}
