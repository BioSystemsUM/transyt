package pt.uminho.ceb.biosystems.transyt.service.utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.uminho.ceb.biosystems.merlin.bioapis.externalAPI.ebi.uniprot.TaxonomyContainer;
import pt.uminho.ceb.biosystems.mew.utilities.datastructures.pair.Pair;
import pt.uminho.ceb.biosystems.transyt.scraper.APIs.NCBIAPI;
import pt.uminho.ceb.biosystems.transyt.scraper.APIs.UniprotAPIExtension;
import pt.uminho.ceb.biosystems.transyt.utilities.capsules.Organism;
import pt.uminho.ceb.biosystems.transyt.utilities.enumerators.Enumerators.KINGDOM;
import pt.uminho.ceb.biosystems.transyt.utilities.enumerators.Enumerators.STAIN;
import pt.uminho.ceb.biosystems.transyt.utilities.transytUtilities.Properties;

public class OrganismProperties {

	String[] taxonomy;
	String species;
	Properties properties;
	Organism organism;

	private static final Logger logger = LoggerFactory.getLogger(OrganismProperties.class);

	/**
	 * @param taxonomyID
	 * @param properties
	 */
	public OrganismProperties(Properties properties, STAIN stain){

		Integer taxonomyID = properties.getTaxID();

		this.properties = properties;

		findTaxonomyByTaxonomyID(taxonomyID);

		organism = new Organism(taxonomyID, species, taxonomy, stain, KINGDOM.Bacteria); //////MUDAR ISTO
	}

	/**
	 * @param taxID
	 */
	private void findTaxonomyByTaxonomyID(Integer taxID) {


		List<String> taxonomy_aux = null;
		String organism = null;

		try {
			Pair<String, List<String>> pair = NCBIAPI.getLineageFromTaxID(taxID);

			organism = pair.getA();
			taxonomy_aux = pair.getB();
		}
		catch (Exception e) {

			e.printStackTrace();
		}

		if(taxonomy_aux == null || taxonomy_aux.isEmpty()) {

			TaxonomyContainer uniprot = UniprotAPIExtension.getTaxonomyFromNCBITaxnomyID(taxID, 0);

			if(uniprot != null) {

				this.species = uniprot.getSpeciesName();
				this.taxonomy = uniprot.getTaxonomy().toString().replaceAll("\\[", "").replaceAll("\\]", "").split(", ");

			}
			else {

				logger.error("Taxonomy ID not recognized by UniProt! Please insert a valid TaxonomyID!");
				logger.info("SHUTING DOWN TranSyT...");
				System.exit(7);
			}
		}
		else {
			this.taxonomy = taxonomy_aux.toArray(new String[0]);
			this.species = organism;
		}

		logger.info("Taxonomy: {}", Arrays.toString(this.taxonomy));
		logger.info("Organism: {}", this.species);

	}

	/**
	 * @return the organism
	 */
	public Organism getOrganism() {
		return organism;
	}

}
