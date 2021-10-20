package pt.uminho.ceb.biosystems.transyt.service.transytDatabase;

import org.neo4j.graphdb.RelationshipType;

public enum TransytRelationshipType implements RelationshipType {
	has_tc,
	has_reaction,
	has_transport_type,
	has_name,
	has_molecular_formula,
	has_MetaCyc_reactant,
	has_MetaCyc_product,
	has_KEGG_reactant,
	has_KEGG_product,
	has_BiGG_reactant,
	has_BiGG_product,
	has_ModelSEED_reactant,
	has_ModelSEED_product;
}
