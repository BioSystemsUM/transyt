package pt.uminho.ceb.biosystems.transyt.service.transytDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.uminho.ceb.biosystems.merlin.bioapis.externalAPI.ebi.uniprot.TaxonomyContainer;
import pt.uminho.ceb.biosystems.merlin.bioapis.externalAPI.ncbi.NcbiAPI;
import pt.uminho.ceb.biosystems.transyt.scraper.APIs.UniprotAPIExtension;
import pt.uminho.ceb.biosystems.transyt.scraper.tcdb.reactionsGenerator.GenerateTransportReactions;
import pt.uminho.ceb.biosystems.transyt.service.internalDB.WriteByMetabolitesID;
import pt.uminho.ceb.biosystems.transyt.service.neo4jRest.RestNeo4jGraphDatabase;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.BiosynthMetaboliteProperties;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.ReactionContainer;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.TcNumberContainer2;
import pt.uminho.ceb.biosystems.transyt.utilities.enumerators.Enumerators.TypeOfTransporter;
import pt.uminho.ceb.biosystems.transyt.utilities.files.FilesUtils;
import pt.uminho.ceb.biosystems.transyt.utilities.files.JSONFilesUtils;
import pt.uminho.ceb.biosystems.transyt.utilities.transytUtilities.Properties;
import uk.ac.ebi.kraken.interfaces.uniprot.NcbiTaxon;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.parser.antlr.UniprotParser.RaContext;

public class PopulateTransytNeo4jDatabase {

	private static final Logger logger = LoggerFactory.getLogger(WriteByMetabolitesID.class);
	private static final String TAXONOMIES_PATH = FilesUtils.getTaxonomicFilesDirectory().concat("taxonomiesByAccession.txt");
	private static final String ORGANISMS_PATH = FilesUtils.getTaxonomicFilesDirectory().concat("organismsByAccession.txt");

	private RestNeo4jGraphDatabase service;
	private Map<String, Set<TcNumberContainer2>> data;
	private Map<String, String> taxonomies;
	private Map<String, String> organisms;
	Map<String, BiosynthMetaboliteProperties> metabolitesProperties;

	public PopulateTransytNeo4jDatabase(Map<String, BiosynthMetaboliteProperties> metabolitesProperties, 
			Map<String, Set<TcNumberContainer2>> data, String version, Properties properties) throws Exception {

		this.data = data;
		this.metabolitesProperties = metabolitesProperties;

		service = TransytNeo4jInitializer.getDatabaseService(properties);

		init(version);

		logger.info("Transaction terminated!");

		service.close();

		logger.info("TranSyT neo4j database shutdown...");

	}

	/**
	 * Start populating the database.
	 */
	private void init(String version) {

		this.findTaxonomy();
		
		try {
			service.createContraints();
		} catch (Exception e1) {
			logger.error("Error trying to create constraints in the database.");
//			e1.printStackTrace();
		}

		Set<String> uniqueReacts = new HashSet<>();

		List<String> searched = new ArrayList<>();

		int lastProgress = 0;
		
		saveDatabaseVersionNode(version);

		logger.info("{} % entries processed!", lastProgress);

		for(String accession : data.keySet()) {

			try {

				TransytNode accesionExists = service.findUniprotAccessionNode(accession);

				TransytNode accessionNode = saveAccessionNode(accession);

				for(TcNumberContainer2 tcContainer : data.get(accession)) {

					TransytNode tcExists = service.findTcNumberNode(tcContainer.getTcNumber());
					
					if(accesionExists == null || tcExists == null) {

						TransytNode tcNumberNode = saveTcNumberNode(tcContainer);

						if(!service.existsRelationship(accessionNode.getNodeID(), tcNumberNode.getNodeID(), TransytRelationshipType.has_tc)) {

							service.createRelationship(accessionNode.getNodeID(), tcNumberNode.getNodeID(), TransytRelationshipType.has_tc);
						}

						for(Integer key : tcContainer.getAllReactionsIds()) {		///titart o i do inicio do ID

							ReactionContainer reactionContainer = tcContainer.getReactionContainer(key);

							String reactionID = reactionContainer.getReactionID();

							TransytNode reactionNode = service.findReactionNode(reactionID);

							if(reactionNode == null) {

								uniqueReacts.add(reactionContainer.getReactionID());	//control number reactions

								reactionNode = saveReactionNode(reactionID, reactionContainer);

								TransytNode typeOfTransportNode = saveTransportTypeNode(reactionContainer.getTransportType());

								if(!service.existsRelationship(reactionNode.getNodeID(), typeOfTransportNode.getNodeID(),
										TransytRelationshipType.has_transport_type)) {

									service.createRelationship(reactionNode.getNodeID(), typeOfTransportNode.getNodeID(),
											TransytRelationshipType.has_transport_type);
								}

							}

							if(!service.existsRelationship(tcNumberNode.getNodeID(), reactionNode.getNodeID(),
									TransytRelationshipType.has_reaction)) {

								Map<String, String> properties = new HashMap<>();

								properties.put(TransytGeneralProperties.Confidence_Level.toString(), reactionContainer.getConfidenceLevel());
								properties.put(TransytGeneralProperties.Reversible.toString(), reactionContainer.isReversible().toString());
								properties.put(TransytGeneralProperties.Direction.toString(), reactionContainer.getDirection());

								Boolean commonOntology = false;

								if(tcContainer.getGeneratedWithCommonOntology().contains(reactionID))
									commonOntology = true;

								properties.put(TransytGeneralProperties.CommonOntology.toString(), commonOntology.toString());

//								if(!reactionContainer.isReversible() && (reactionContainer.getTransportType().equals(TypeOfTransporter.Uniport) 
//										|| reactionContainer.getTransportType().equals(TypeOfTransporter.Symport) || reactionContainer.getTransportType().equals(TypeOfTransporter.Antiport))) {
//
//									properties.put(TransytGeneralProperties.CompartimentalizedReactionID.toString(), ReactionContainer.i.concat(reactionContainer.getCompartmentalizedReactionID()));
//								}

								service.createRelationship(tcNumberNode.getNodeID(), reactionNode.getNodeID(),
										TransytRelationshipType.has_reaction, properties);

							}
//							else
//								System.out.println("Trying to create a new has_react relationship " + accession + "\t" + tcContainer.getTcNumber() + "\t" + reactionID);
//							else {
//								TransytRelationship rel = service.getRelationship(tcNumberNode.getNodeID(),
//										reactionNode.getNodeID(), TransytRelationshipType.has_reaction);
//
//								boolean previousReversibility;
//
//								previousReversibility = Boolean.valueOf(rel.getProperty(TransytGeneralProperties.Reversible));
//
//								if(reactionContainer.isReversible() && !previousReversibility)
//									service.setRelationshipProperty(tcNumberNode.getNodeID(), reactionNode.getNodeID(),
//											TransytRelationshipType.has_reaction,
//											TransytGeneralProperties.Reversible,
//											reactionContainer.isReversible().toString());
//
//							}
						}
					}
				}
			} 
			catch (Exception e) {

				logger.error("An error occurred while savind entries for accession: {}", accession);
				logger.trace("StackTrace {}", e);
				e.printStackTrace();
			}

			searched.add(accession);

			int progress = (searched.size()*100)/data.size();

			if(progress > lastProgress){

				lastProgress = progress;
				logger.info("{} % entries processed!", progress);
			}
		}
	}

	private void findTaxonomy(){

		taxonomies = FilesUtils.readMapFromFile(TAXONOMIES_PATH);
		organisms = FilesUtils.readMapFromFile(ORGANISMS_PATH);

		int taxInitialSize = taxonomies.size();
		int orgInitialSize = organisms.size();

		List<String> toSearch = new ArrayList<>(data.keySet());
		
		logger.info("Taxonomies to search without before filter: " + toSearch.size());

		toSearch.replaceAll(String::toUpperCase);

		for(String acc : new ArrayList<>(toSearch)) {

			if(taxonomies.containsKey(acc) && organisms.containsKey(acc))
				toSearch.remove(acc);
		}
		
		logger.info("Taxonomies to search in Uniprot: " + toSearch.size());

		Map<String, String> backupData = getOrganismsFromTCDB();

		if(!toSearch.isEmpty()) {

			List<UniProtEntry> uniprotList = UniprotAPIExtension.getEntriesFromUniProtIDs(toSearch, 0);

			if(uniprotList != null) {

				for(UniProtEntry uniprot : uniprotList) {

					if(uniprot != null) {
						String uniprotID = uniprot.getPrimaryUniProtAccession().getValue();
						
						List<String> taxonomy_aux = new ArrayList<>();
						String organism = null;
						String taxonomy = null;
						
						if(uniprot.getNcbiTaxonomyIds().size() > 0) {
						
							String taxId = uniprot.getNcbiTaxonomyIds().get(0).getValue();
							
							try {
								TaxonomyContainer taxon = NcbiAPI.getTaxonomyFromNCBI(Long.valueOf(taxId), 0);
								
								for(NcbiTaxon taxEntry : taxon.getTaxonomy()) {
									taxonomy_aux.add(taxEntry.getValue());
								}
								
								organism = taxon.getSpeciesName();
								
							}
							catch (Exception e) {
								taxonomy_aux = null;
								e.printStackTrace();
							}
							
						}
						
						if(taxonomy_aux == null || taxonomy_aux.isEmpty())
							taxonomy = uniprot.getTaxonomy().toString();
						else
							taxonomy = taxonomy_aux.toString();
						
						if(organism == null)
							organism = uniprot.getOrganism().toString();
					
						taxonomies.put(uniprotID, taxonomy);
						organisms.put(uniprotID, organism);

						toSearch.remove(uniprotID);
					}
				}
			}
			
			logger.info("Taxonomies to search in TCDB records: " + toSearch.size());

			if(!toSearch.isEmpty()) {

				for(String uniprotID : new HashSet<String>(toSearch)) {

					if(backupData.containsKey(uniprotID)) {

						String org = backupData.get(uniprotID);

						String tax = UniprotAPIExtension.findTaxonmyByOrganismName(org, 0);

						if(tax != null) {

							taxonomies.put(uniprotID.replaceAll("\\[", "").replaceAll("\\]", ""), tax);
							organisms.put(uniprotID, org);
							
							toSearch.remove(uniprotID);
						}
					}
				}
			}
			
			logger.info("Taxonomies missing: " + toSearch.size());

			if(taxonomies.size() != taxInitialSize || organisms.size() != orgInitialSize) {
				FilesUtils.saveMapInFile(TAXONOMIES_PATH, taxonomies);
				FilesUtils.saveMapInFile(ORGANISMS_PATH, organisms);
			}
		}
	}

	/**
	 * Method to save an accession in TranSyT's internal database
	 * 
	 * @param accession
	 * @return
	 */
	private TransytNode saveAccessionNode(String accession) {

		TransytNode accessionNode = service.findUniprotAccessionNode(accession);

		if(accessionNode == null) {

			Map<String, String> properties = new HashMap<>();

			String taxonomy = "null";
			String organism = "null";

			if(taxonomies.containsKey(accession)) {
				taxonomy = taxonomies.get(accession);
				organism = organisms.get(accession);
			}

			properties.put(TransytGeneralProperties.Accession_Number.toString(), accession);
			properties.put(TransytGeneralProperties.Taxonomy.toString(), taxonomy);
			properties.put(TransytGeneralProperties.Organism.toString(), organism);

			accessionNode = service.createNode(TransytNodeLabel.Uniprot_Accession, properties);

			logger.debug("Uniprot Accession node created for accession: {}", accession);
		}

		return accessionNode;

	}

	/**
	 * Method to save a TCnumber in TranSyT's internal database
	 * 
	 * @param accession
	 * @return
	 */
	private TransytNode saveTcNumberNode(TcNumberContainer2 tcContainer) {

		TransytNode tcNumberNode = service.findTcNumberNode(tcContainer.getTcNumber());

		Map<String, String> properties = new HashMap<>();

		if(tcNumberNode == null) {

			properties.put(TransytGeneralProperties.TC_Number.toString(), tcContainer.getTcNumber());

			if(!tcContainer.getMainReactions().isEmpty())
				properties.put(TransytGeneralProperties.MainReactionsIDs.toString(), 
						tcContainer.getMainReactions().toString().replaceAll("\\[", "").replaceAll("\\]", ""));

			if(tcContainer.getSuperFamily() != null)
				properties.put(TransytGeneralProperties.Superfamily.toString(), tcContainer.getFamily());

			if(tcContainer.getFamily() != null)
				properties.put(TransytGeneralProperties.Family.toString(), tcContainer.getFamily());

			tcNumberNode = service.createNode(TransytNodeLabel.TC_Number, properties);

			logger.debug("TC Number node created for tc: {}", tcContainer.getTcNumber());
		}

		return tcNumberNode;
	}

	/**
	 * Method to save a reaction in TranSyT's internal database
	 * 
	 * @param accession
	 * @return
	 */
	private TransytNode saveReactionNode(String id, ReactionContainer reactionContainer) {

		Map<String, String> properties = new HashMap<>();

		properties.put(TransytGeneralProperties.ReactionID.toString(), id);
		properties.put(TransytGeneralProperties.MetaID.toString(), reactionContainer.getMetaReactionID());
		properties.put(TransytGeneralProperties.Reaction.toString(), reactionContainer.getReaction());
		properties.put(TransytGeneralProperties.Original_Reaction.toString(), reactionContainer.getOriginalReaction());
		
		if(reactionContainer.getModelseedReactionIdentifiers() != null) {
			String identifiers = reactionContainer.getModelseedReactionIdentifiers()
					.stream().map(i -> i.toString()).collect(Collectors.joining(";"));
			properties.put(TransytGeneralProperties.ModelSEED_Reaction_ids.toString(), identifiers);
		}

		for(String property : reactionContainer.getProperties().keySet())
			properties.put(property, reactionContainer.getProperties().get(property));

		TransytNode reactionNode = service.createNode(TransytNodeLabel.Reaction, properties);

		if(reactionContainer.getReactionMetaCyc() != null)
			saveReactionEquation(TransytNodeLabel.MetaCyc_Metabolite, reactionNode, reactionContainer.getReactionMetaCyc(), reactionContainer.isReversible());

		if(reactionContainer.getReactionModelSEED() != null)
			saveReactionEquation(TransytNodeLabel.ModelSEED_Metabolite, reactionNode, reactionContainer.getReactionModelSEED(), reactionContainer.isReversible());

		if(reactionContainer.getReactionKEGG() != null)
			saveReactionEquation(TransytNodeLabel.KEGG_Metabolite, reactionNode, reactionContainer.getReactionKEGG(), reactionContainer.isReversible());

		if(reactionContainer.getReactionBiGG() != null)
			saveReactionEquation(TransytNodeLabel.BiGG_Metabolite, reactionNode, reactionContainer.getReactionBiGG(), reactionContainer.isReversible());

		logger.debug("Reaction node created for reactionID: {} and metaID: {}", id, reactionContainer.getMetaReactionID());

		return reactionNode;
	}

	/**
	 * Method to save pt.uminho.ceb.biosystems.transyt.service.reactions equations in TranSyT's internal database
	 * 
	 * @param accession
	 * @return
	 */
	private void saveReactionEquation(TransytNodeLabel label, TransytNode reactionNode, String equation, boolean reversible) {

		String regex = ReactionContainer.IRREV_TOKEN;

		if(reversible)
			regex = ReactionContainer.REV_TOKEN;

		String[] aux = equation.split(regex);

		if(label.equals(TransytNodeLabel.ModelSEED_Metabolite)) {
			saveReactantsOrProducts(label, TransytRelationshipType.has_ModelSEED_reactant, reactionNode, aux[0]);
			saveReactantsOrProducts(label, TransytRelationshipType.has_ModelSEED_product, reactionNode, aux[1]);
		}
		else if(label.equals(TransytNodeLabel.BiGG_Metabolite)) {
			saveReactantsOrProducts(label, TransytRelationshipType.has_BiGG_reactant, reactionNode, aux[0]);
			saveReactantsOrProducts(label, TransytRelationshipType.has_BiGG_product, reactionNode, aux[1]);
		}
		else if(label.equals(TransytNodeLabel.KEGG_Metabolite)) {
			saveReactantsOrProducts(label, TransytRelationshipType.has_KEGG_reactant, reactionNode, aux[0]);
			saveReactantsOrProducts(label, TransytRelationshipType.has_KEGG_product, reactionNode, aux[1]);
		}
		else if(label.equals(TransytNodeLabel.MetaCyc_Metabolite)) {
			saveReactantsOrProducts(label, TransytRelationshipType.has_MetaCyc_reactant, reactionNode, aux[0]);
			saveReactantsOrProducts(label, TransytRelationshipType.has_MetaCyc_product, reactionNode, aux[1]);
		}

	}

	private void saveReactantsOrProducts(TransytNodeLabel label, TransytRelationshipType relationshipType , TransytNode reactionNode, String equation) {

		Map<String, Integer> stoichiometry = new HashMap<>();
		Map<String, String> directions = new HashMap<>();

		String[] metabolites = equation.split("\\s+\\+\\s+");

		for(String metabolite : metabolites) {

			metabolite = metabolite.trim();

			int currentStoichiometry = 1;

			Pattern p;
			Matcher m;

			if(metabolite.matches("^(\\d+).+")) {

				p = Pattern.compile("^(\\d+)");
				m = p.matcher(metabolite);

				if(m.find()) {

					try {
						currentStoichiometry = Integer.valueOf(m.group());
					} 
					catch (NumberFormatException e) {
						currentStoichiometry = 1;
					}
				}

				metabolite = metabolite.replaceAll("^(\\d+)", "");
			}

			String direction = null;

			if(metabolite.contains(ReactionContainer.INTERIOR_COMPARTMENT_TOKEN)) {
				metabolite = metabolite.replace(ReactionContainer.INTERIOR_COMPARTMENT_TOKEN, "");
				direction = ReactionContainer.INTERIOR_COMPARTMENT;
			}
			else if(metabolite.contains(ReactionContainer.EXTERIOR_COMPARTMENT_TOKEN)) {
				metabolite =  metabolite.replace(ReactionContainer.EXTERIOR_COMPARTMENT_TOKEN, "");
				direction = ReactionContainer.EXTERIOR_COMPARTMENT;
			}

			metabolite = metabolite.trim();

			if(direction != null)
				directions.put(metabolite, direction);

			if(!stoichiometry.containsKey(metabolite))
				stoichiometry.put(metabolite, 0);

			stoichiometry.put(metabolite, stoichiometry.get(metabolite) + currentStoichiometry);
		}

		for(String id : stoichiometry.keySet()) {
			TransytNode metaboliteNode = saveMetaboliteNode(label, id);

			Map<String, String> relProperties = new HashMap<>();

			relProperties.put(TransytGeneralProperties.Stoichiometry.toString(), stoichiometry.get(id).toString());

			if(directions.containsKey(id))
				relProperties.put(TransytGeneralProperties.Direction.toString(), directions.get(id));

			service.createRelationship(reactionNode.getNodeID(), metaboliteNode.getNodeID(),
					relationshipType, relProperties);
		}
	}

	/**
	 * Method to save metabolites in TranSyT's internal database
	 * 
	 * @param accession
	 * @return
	 */
	private TransytNode saveMetaboliteNode(TransytNodeLabel label, String id) {

		TransytNode metaboliteNode = service.findMetaboliteNode(label, id);

		//		try {
		if(metaboliteNode == null) {

			String formula = null;
			Set<String> names = null;

			if(metabolitesProperties.containsKey(id)) {

				formula = metabolitesProperties.get(id).getFormula();
				names = metabolitesProperties.get(id).getSynonyms();
			}

			metaboliteNode = service.createNode(label, TransytGeneralProperties.MetaboliteID, id);

			if(formula != null && !formula.equals(BiosynthMetaboliteProperties.NONE)) {

				TransytNode metaboliteFormula = service.findMolecularFormulaNode(formula);

				if(metaboliteFormula == null) {
					metaboliteFormula = service.createNode(TransytNodeLabel.Molecular_formula, 
							TransytGeneralProperties.Molecular_Formula, formula);

					logger.debug("Formula node created for: {}", formula);
				}

				service.createRelationship(metaboliteNode.getNodeID(), metaboliteFormula.getNodeID(),
						TransytRelationshipType.has_molecular_formula);
			}

			if(names != null) {

				String nameAux = "";

				for(String name : names) {

					if(nameAux.isEmpty())
						nameAux = name;

					TransytNode nameNode = service.findNameNode(name);

					if(nameNode == null) {

						nameNode = service.createNode(TransytNodeLabel.Metabolite_Name, TransytGeneralProperties.Name, name);

						logger.debug("Name node created for metabolite with name: {}", name);
					}

					service.createRelationship(metaboliteNode.getNodeID(), nameNode.getNodeID(),
							TransytRelationshipType.has_name);
				}

				service.setNodeProperty(metaboliteNode.getNodeID(), label, TransytGeneralProperties.Name, nameAux);
			}

			String msg = "Metabolite node created for database ".concat(label.toString()).concat(" with identifier: ").concat(id);

			logger.debug(msg);
		}
		//		} 
		//		catch (Exception e) {
		//			
		//			System.out.println(id);
		//			
		//			e.printStackTrace();
		//		}

		return metaboliteNode;
	}

	/**
	 * Method to save reaction transport types in TranSyT's internal database
	 * 
	 * @param accession
	 * @return
	 */
	private TransytNode saveTransportTypeNode(TypeOfTransporter type) {

		TransytNode typeOfTransportNode = service.findTransportTypeNode(type);

		if(typeOfTransportNode == null) {

			typeOfTransportNode = service.createNode(TransytNodeLabel.Transport_Type, TransytGeneralProperties.Transport_Type, type.toString());

			logger.debug("Transport type node created for: {}", type.toString());
		}

		return typeOfTransportNode;
	}
	
	/**
	 * Method to save reaction transport types in TranSyT's internal database
	 * 
	 * @param accession
	 * @return
	 */
	private void saveDatabaseVersionNode(String version) {

		service.createNode(TransytNodeLabel.Database_Version, TransytGeneralProperties.Version, version);

		logger.debug("Database_version node created for value {}", version);

	}

	/**
	 * @param accession
	 * @return
	 */
	private static Map<String, String> getOrganismsFromTCDB() {

		List<String[]> excel = JSONFilesUtils.readTCDBScrapedInfo();
		Map<String, String> data = new HashMap<>(); 

		for(String[] line : excel) {

			String accession = line[0].split("@")[0];
			String organism = line[1];

			if(!data.containsKey(accession) && organism != null && !organism.isEmpty())
				data.put(accession, organism);
		}

		return data;
	}
}
