package pt.uminho.ceb.biosystems.transyt.service.transytDatabase;

import org.neo4j.graphdb.Label;

public enum TransytNodeLabel implements Label {

	Uniprot_Accession,
	TC_Number,
	Reaction,
	Transport_Type,
	Metabolite_Name,
	Molecular_formula,
	ModelSEED_Metabolite,
	KEGG_Metabolite,
	BiGG_Metabolite,
	Database_Version,
	MetaCyc_Metabolite;
}
