package pt.uminho.ceb.biosystems.transyt.utilities.biocomponents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;import org.sbml.jsbml.util.IdManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.CompartmentCI;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.GeneCI;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.MetaboliteCI;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.ReactionCI;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.ReactionConstraintCI;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.ReactionTypeEnum;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.StoichiometryValueCI;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.interfaces.IContainerBuilder;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.ReactionContainer;
import pt.uminho.ceb.biosystems.transyt.utilities.files.FilesUtils;

public class OutputTransytFormat implements IContainerBuilder{

	private static final long serialVersionUID = 1L;
	private static final String innerComp = "c";
	private static final String outerComp = "e";
	private static final String middleComp = "p";

	protected HashMap<String, ReactionConstraintCI> defaultEC = null;
	protected HashMap<String, CompartmentCI> compartmentList = null;
	protected HashMap<String, MetaboliteCI> metaboliteList = null;
	protected HashMap<String, ReactionCI> reactionList = null;
	protected HashMap<String, GeneCI> genes = null;
	protected Map<String, Map<String, String>> metabolitesExtraInfo = null;
	protected Map<String, Map<String, String>> reactionsExtraInfo = null;
	protected HashMap<String, String> mapMetaboliteIdCompartment = null;
	protected HashMap<String, Double> boundsParameters=null; 

	private Map<String, Map<String, Set<String>>> info;
	private Map<String, String> modelMetabolites;
	private Map<String, ReactionContainer> reactionContainers;
	//	private String biomassID;
	private Map<String, String> geneRules;
	private Map<String, String> metabolitesNames;

	private static final Logger logger = LoggerFactory.getLogger(OutputTransytFormat.class);

	public OutputTransytFormat(String referencesReportPath, Map<String, Map<String, Set<String>>> info, Map<String, String> modelMetabolites,
			Map<String, ReactionContainer> reactionContainers, Map<String, String> geneRules, Map<String, String> metabolitesNames) {

		this.info = info;
		this.modelMetabolites = modelMetabolites;
		this.reactionContainers = reactionContainers;
		this.geneRules = geneRules;
		this.metabolitesNames = metabolitesNames;

		populateInformation(referencesReportPath);
	}

	/**
	 * 
	 */
	public void populateInformation(String referencesReportPath){

		compartmentList = new HashMap<String, CompartmentCI>();
		genes = new HashMap<String, GeneCI>();
		metaboliteList = new HashMap<String, MetaboliteCI>();
		defaultEC = new HashMap<String, ReactionConstraintCI>();
		reactionList = new HashMap<String, ReactionCI>();
		mapMetaboliteIdCompartment = new HashMap<String, String>();
		metabolitesExtraInfo = new HashMap<String, Map<String, String>>(); 
		reactionsExtraInfo = new HashMap<String, Map<String, String>>();

		addCompartments();
		addMetabolites();
		addReactions(referencesReportPath);

	}

	/**
	 * 
	 */
	private void addCompartments() {

		CompartmentCI ogcomp = new CompartmentCI(innerComp, "internal", null);
		compartmentList.put(innerComp, ogcomp);

		ogcomp = new CompartmentCI(outerComp, "external", null);
		compartmentList.put(outerComp, ogcomp);

		ogcomp = new CompartmentCI(middleComp, "periplasm", null);
		compartmentList.put(middleComp, ogcomp);

	}

	/**
	 * 
	 */
	private void addMetabolites() {

		String[] compartments = new String[]{innerComp, outerComp, middleComp};
		
		for(String comp : compartments) {

			for (String idInModel : modelMetabolites.keySet()) {
				
				//	System.out.println(idInModel + "\t" + metabolitesNames.get(idInModel));

				String name = "M_" + metabolitesNames.get(idInModel).concat("_").concat(comp).concat("0");

				idInModel = "M_" + idInModel.concat("_".concat(comp).concat("0"));

				compartmentList.get(comp).addMetaboliteInCompartment(idInModel);
				mapMetaboliteIdCompartment.put(idInModel, comp);

				MetaboliteCI ogspecies = new MetaboliteCI(idInModel, name);

				ogspecies.setFormula(modelMetabolites.get(idInModel));
				
				metaboliteList.put(idInModel, ogspecies);
			}
		}
	}

	//	private void addMetabolite(String metabolite) {
	//		
	//		String comp = metabolite.substring(metabolite.length()-1, metabolite.length());
	//		
	//		compartmentList.get(comp).addMetaboliteInCompartment(metabolite);
	//		mapMetaboliteIdCompartment.put(metabolite, comp);
	//
	//		MetaboliteCI ogspecies = new MetaboliteCI(metabolite, metabolite);
	//
	//		ogspecies.setFormula(modelMetabolites.get(metabolite));
	//
	//		metaboliteList.put(metabolite, ogspecies);
	//	}

	private void addReactions(String referencesReportPath) {

		Set<String> speciesInReactions = new TreeSet<String>();
		long maxMetabInReaction = 0;

		int i = 0;

		Set<String> allApprovedReactions = new HashSet<>();

		for(String acc : info.keySet()) {

			genes.put(acc, new GeneCI(acc, acc)); //genes added here to avoid iteration of another cycle

			for(String tc : info.get(acc).keySet()) {

				//				if(info.get(acc).get(tc).contains("TRabc__cpd00001_cpd00002_cpd00039o"))
				//					System.out.println(acc + "\t" + tc + "\t" + info.get(acc).get(tc));

				allApprovedReactions.addAll(info.get(acc).get(tc));
			}
		}
		
		Map<String, Set<String>> references = new HashMap<>(); //find a way to save this in the SBML directly

		//		System.out.println("coiso " + allApprovedReactions.size());

		for (String reactionID : allApprovedReactions) {

			if(reactionContainers.containsKey(reactionID) && !geneRules.get(reactionID).isEmpty()) {

				ReactionContainer container = reactionContainers.get(reactionID);

				Set<String> products = getSpeciesOfReaction(container.getProduct());
				Set<String> reactants = getSpeciesOfReaction(container.getReactant());

				/** add mappings for products */
				Map<String, StoichiometryValueCI> productsCI = addMapping(products, reactionID, speciesInReactions);

				/** add mappings for reactants */
				Map<String, StoichiometryValueCI> reactantsCI = addMapping(reactants, reactionID, speciesInReactions);

				boolean isReversible = container.isReversible();

				maxMetabInReaction = kinetic(reactionID, isReversible, reactantsCI, productsCI, maxMetabInReaction);

				ReactionCI ogreaction = new ReactionCI(reactionID, reactionID, isReversible,
						reactantsCI, productsCI);

				if (products.size() == 0 || reactants.size() == 0) {
					ogreaction.setType(ReactionTypeEnum.Drain);
				} else {
					ogreaction.setType(ReactionTypeEnum.Internal);
				}
				
				ogreaction.setGeneRule(geneRules.get(reactionID));

				//add reaction
				reactionList.put(reactionID, ogreaction);
				
				if(container.getModelseedReactionIdentifiers() != null)
					references.put(reactionID, new HashSet<>(container.getModelseedReactionIdentifiers()));

				i++;

			}
		}
		
		FilesUtils.saveMapInFile3(referencesReportPath, references);

		logger.info("Total reactions associated: {}", i);

		//		reactionList.get(biomassID).setType(ReactionTypeEnum.Transport);	//check this

		removeSpeciesNonAssociatedToReactions(speciesInReactions);

	}

	public long kinetic(String reactionID, boolean isReversible, Map<String, StoichiometryValueCI> reactantsCI,
			Map<String, StoichiometryValueCI> productsCI, long maxMetabInReaction) {

		double lower = 0;
		double upper = 1000;

		if(isReversible)
			lower = -1000;

		//		long nMetabolitesInReaction = reactantsCI.size() + productsCI.size();
		//		if (nMetabolitesInReaction > maxMetabInReaction) {
		//			maxMetabInReaction = nMetabolitesInReaction;
		//
		//			biomassID = reactionID;
		//		}

		defaultEC.put(reactionID, new ReactionConstraintCI(lower, upper));

		return maxMetabInReaction;
	}

	/**
	 * @param text
	 * @return
	 */
	private Set<String> getSpeciesOfReaction(String text) {

		//		System.out.println(text);

		Set<String> set = new HashSet<>();

		String regexOut = "_".concat(outerComp).concat("0");
		String regexIn = "_".concat(innerComp).concat("0");
		String regexMiddle = "_".concat(middleComp).concat("0");

		String[] species = text.replace(" (in)", regexIn).replace(" (out)", regexOut).replace(" (middle)", regexMiddle).split(" \\+ ");

		//		System.out.println(text);
		//		Utilities.printArray(species);

		for(String s : species) {

			//			if(s.matches("^\\d+\\s*cpd.+"))
			//				s = s.replaceAll("^\\d+\\s*", "");

			if(s.matches("^n\\s*cpd.+"))
				s = s.replaceAll("^n\\s*", "");

			if(s.matches(".+\\s*\\+$"))
				s = s.replaceAll("\\s*\\+$", "");

			if(!s.contains("_"))
				s = s.concat(regexIn);

			set.add("M_" + s.trim().replaceAll("\\s+", ""));

		}

		return set;
	}

	/**
	 * @param list
	 * @param reactionId
	 * @param speciesInReactions
	 * @return
	 */
	private Map<String, StoichiometryValueCI> addMapping(Set<String> list, String reactionId,
			Set<String> speciesInReactions) {

		Map<String, StoichiometryValueCI> result = new HashMap<String, StoichiometryValueCI>();

		//		System.out.println(list);

		String id = "";

		for (String idInModel : list) {

			try {

				id = idInModel;

				if(id.matches("^M_\\d+\\s*\\w.+"))
					id = id.replaceAll("^M_\\d+\\s*", "M_");


				result.put(id, new StoichiometryValueCI(id, getMetaboliteStoichiometry(idInModel),
						mapMetaboliteIdCompartment.get(id)));
				//			System.out.println(idInModel);

				//			if(!metaboliteList.containsKey(idInModel)) {
				//			
				//				addMetabolite(idInModel);
				//				
				//			}
				//			
				metaboliteList.get(id).addReaction(reactionId);
				speciesInReactions.add(id);
			} 
			catch (Exception e) {

				System.out.println(idInModel);
				System.out.println(id);
				System.out.println(list);
				e.printStackTrace();
			}
		}

		return result;
	}

	/**
	 * @param molecule
	 * @return
	 */
	private double getMetaboliteStoichiometry(String molecule) {

		Integer stoichiometry = 1;

		Pattern p;
		Matcher m;

		if(molecule.matches("^M_(\\d+).+")) {

			p = Pattern.compile("M_(\\d+)");
			m = p.matcher(molecule);

			if(m.find()) {

				try {
					stoichiometry = Integer.valueOf(m.group().replace("M_", ""));
				} 
				catch (NumberFormatException e) {
					stoichiometry = 1;
				}
			}

		}

		return stoichiometry;
	}

	/**
	 * This method removes the species that aren't associated with any reaction
	 * 
	 * @param speciesInReactions
	 *            A set with all the species that are associated to one ore more
	 *            reactions
	 */
	private void removeSpeciesNonAssociatedToReactions(Set<String> speciesInReactions) {

		List<String> toRemove = new ArrayList<String>();

		for (String metId : metaboliteList.keySet())
			if (!speciesInReactions.contains(metId))
				toRemove.add(metId);

		for (String metId : toRemove) {
			metaboliteList.remove(metId);
		}
	}

	/**
	 * 
	 */
	private void addGenes() {

		for(String gene : info.keySet())
			genes.put(gene, new GeneCI(gene, gene));	

	}

	@Override
	public String getModelName() {
		return null;
	}

	@Override
	public String getOrganismName() {
		return null;
	}

	@Override
	public String getNotes() {
		return null;
	}

	@Override
	public Integer getVersion() {
		return null;
	}

	@Override
	public Map<String, CompartmentCI> getCompartments() {

		return compartmentList;
	}

	@Override
	public Map<String, ReactionCI> getReactions() {

		return reactionList;
	}

	@Override
	public Map<String, MetaboliteCI> getMetabolites() {

		return metaboliteList;
	}

	@Override
	public Map<String, GeneCI> getGenes() {

		return genes;
	}

	@Override
	public Map<String, Map<String, String>> getMetabolitesExtraInfo() {
		return null;
	}

	@Override
	public Map<String, Map<String, String>> getReactionsExtraInfo() {
		return null;
	}

	@Override
	public String getBiomassId() {
		return null;
	}

	@Override
	public Map<String, ReactionConstraintCI> getDefaultEC() {
		return null;
	}

	@Override
	public String getExternalCompartmentId() {
		return null;
	}



}
