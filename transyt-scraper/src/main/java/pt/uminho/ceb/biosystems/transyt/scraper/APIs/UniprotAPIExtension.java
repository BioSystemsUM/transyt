package pt.uminho.ceb.biosystems.transyt.scraper.APIs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.uminho.ceb.biosystems.merlin.bioapis.externalAPI.ebi.uniprot.TaxonomyContainer;
import pt.uminho.ceb.biosystems.merlin.bioapis.externalAPI.ebi.uniprot.UniProtAPI;
import pt.uminho.ceb.biosystems.merlin.bioapis.externalAPI.utilities.MySleep;
import pt.uminho.ceb.biosystems.merlin.utilities.datastructures.list.ListUtilities;
import uk.ac.ebi.kraken.interfaces.uniprot.NcbiTaxonomyId;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.dataservice.client.QueryResult;
import uk.ac.ebi.uniprot.dataservice.client.exception.ServiceException;
import uk.ac.ebi.uniprot.dataservice.client.uniprot.UniProtQueryBuilder;
import uk.ac.ebi.uniprot.dataservice.query.Query;

public class UniprotAPIExtension extends UniProtAPI{
	
	final static Logger logger = LoggerFactory.getLogger(UniprotAPIExtension.class);

	/**
	 * @param name
	 * @param errorCount
	 * @return
	 */
	public static String findTaxonmyByOrganismName(String name, int errorCount) {

		UniProtAPI.getInstance();

		try {

			Query query = UniProtQueryBuilder.organismName(name);
	        QueryResult<UniProtEntry> entries = uniProtService.getEntries(query);
	        
			while (entries.hasNext()) {

				UniProtEntry entry = entries.next();
				
				if(!entry.getTaxonomy().toString().isEmpty() && entry.getTaxonomy() != null)
					return entry.getTaxonomy().toString();
			}
		} 
		catch(Exception e) {

			if(errorCount<5) {

				MySleep.myWait(1000);
				errorCount+=1;
				logger.debug("Entries From UniProt IDs trial {}",errorCount);
				return UniprotAPIExtension.findTaxonmyByOrganismName(name, errorCount);
			}
			else {

				logger.error("Could not retrieve entries list. Returning null. {}",name);
				logger.trace("StackTrace {}",e);
				return null;
			}
		}
		return null;
	}
	
	public static TaxonomyContainer getTaxonomyFromNCBITaxnomyID(int taxID, int errorCount) {

		UniProtAPI.getInstance();

		TaxonomyContainer result = new TaxonomyContainer();

		try {

			Query query = UniProtQueryBuilder.taxonID(taxID);			//UniprotAPI uses UniProtQueryBuilder.gene() -> wrong
			
			QueryResult<UniProtEntry> entries = uniProtService.getEntries(query);
			
			while(entries.hasNext()) {

				UniProtEntry uniProtEntry = entries.next();
				
				List<NcbiTaxonomyId> taxa = uniProtEntry.getNcbiTaxonomyIds();

				for(NcbiTaxonomyId taxon : taxa) {

					if(taxon.getValue().equalsIgnoreCase(""+taxID)) {

						result.setSpeciesName(uniProtEntry.getOrganism().getScientificName().getValue());
						result.setTaxonomy(uniProtEntry.getTaxonomy());


						return result;
					}
				}
			}
			return null;
		}
		catch(ServiceException e) {

			if(errorCount<10) {

				MySleep.myWait(1000);
				errorCount = errorCount+1;
				logger.debug("getTaxonomyFromNCBITaxnomyID trial {}",errorCount);
				return getTaxonomyFromNCBITaxnomyID(taxID, errorCount+1);
			}
			else {

				logger.error("getTaxonomyFromNCBITaxnomyID eror, returning null. {}",taxID);
				logger.trace("StackTrace {}",e);
				return null;
			}
		}
	}

	/**
	 * @param uniprotIDs
	 * @param errorCount
	 * @return
	 */
	public static List<UniProtEntry> getEntriesFromUniProtIDs(List<String> uniprotIDs, int errorCount){

		UniProtAPI.getInstance();

		try {

			List<UniProtEntry> uniprotEntries = new ArrayList<>();
//			for (int i = 0; i < uniprotIDs.size(); i++)
//				uniprotEntries.add(null);

			List<List<String>> uniprotIDs_subsets = ListUtilities.split(uniprotIDs, 50);

			for(List<String> uniprotIDs_subset : uniprotIDs_subsets) {

//				try {
					Query query = UniProtQueryBuilder.accessions(new HashSet<String> (uniprotIDs_subset));

					QueryResult<UniProtEntry> entries = uniProtService.getEntries(query);
					while (entries.hasNext()) {

						UniProtEntry entry = entries.next();
						uniprotEntries.add(entry);
//						uniprotEntries.set(uniprotIDs.indexOf(entry.getPrimaryUniProtAccession().getValue()),entry);
					}
//				} 
//				catch (Exception e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
			}
			return uniprotEntries;
		} 
		catch(Exception e) {

			if(errorCount<20) {

				MySleep.myWait(1000);
				errorCount+=1;
				logger.debug("Entries From UniProt IDs trial {}",errorCount);
				return UniProtAPI.getEntriesFromUniProtIDs(uniprotIDs, errorCount);
			}
			else {

				logger.error("Could not retrieve entries list. Returning null. {}",uniprotIDs);
				logger.trace("StackTrace {}",e);
				return null;
			}
		}
	}
}
