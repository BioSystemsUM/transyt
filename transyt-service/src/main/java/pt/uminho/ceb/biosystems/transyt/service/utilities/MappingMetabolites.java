package pt.uminho.ceb.biosystems.transyt.service.utilities;

import java.util.HashMap;
import java.util.Map;

import pt.uminho.ceb.biosystems.transyt.service.internalDB.FetchCompoundsByName;
import pt.uminho.ceb.biosystems.transyt.utilities.files.FilesUtils;
import pt.uminho.sysbio.biosynth.integration.io.dao.neo4j.MetaboliteMajorLabel;

public class MappingMetabolites {
	
	private static final String PATH = FilesUtils.getReactionsBuilderFilesDirectory();

	public static Map<String, String> getMapping(MetaboliteMajorLabel defaultLabel, MetaboliteMajorLabel target) {
		
		if(defaultLabel.equals(target))
			defaultLabel = MetaboliteMajorLabel.MetaCyc;

		if(defaultLabel.equals(MetaboliteMajorLabel.MetaCyc)) {
			switch(target) {

			case ModelSeed:
				return metaCycToModelSEEDids();

			case LigandCompound:
				return metaCycToKEGGids();

			case BiGGMetabolite:
				return mappingMetabolites(defaultLabel, MetaboliteMajorLabel.BiGGMetabolite);
			
			default:
				 mappingMetabolites(defaultLabel, target);
			}
		}

		return mappingMetabolites(defaultLabel, target);
	}

	public static Map<String, String> mappingMetabolites(MetaboliteMajorLabel defaultLabel, MetaboliteMajorLabel targetLabel) {

		Map<String, Map<MetaboliteMajorLabel, String>> compounds = FileUtils.readMapFromFile2(FetchCompoundsByName.METABOLITESIDS_PATH);

		Map<String, String> res = new HashMap<>();

		Map<String, Map<String, Integer>> counts = new HashMap<>();

		for(String key : compounds.keySet()) {

			Map<MetaboliteMajorLabel, String> identifiers = compounds.get(key);

			String id = null;

			if(defaultLabel.equals(MetaboliteMajorLabel.MetaCyc)) {
				if(identifiers.containsKey(MetaboliteMajorLabel.MetaCyc)) {

					id = identifiers.get(MetaboliteMajorLabel.MetaCyc);
				}
				else if(identifiers.containsKey(MetaboliteMajorLabel.EcoCyc)) {

					id = identifiers.get(MetaboliteMajorLabel.EcoCyc);
				}
			}
			else{
				if(identifiers.containsKey(defaultLabel))
						id = identifiers.get(defaultLabel);
			}
			
//			System.out.println(id + "\t" + identifiers.containsKey(targetLabel));

			if(id != null && identifiers.containsKey(targetLabel)) {

				Map<String, Integer> subCounts = new HashMap<>();

				if(counts.containsKey(id))
					subCounts = counts.get(id);

				String targetID = identifiers.get(targetLabel);
				
//				System.out.println(id + "\t" + targetID);

				if(!subCounts.containsKey(targetID)) {
					subCounts.put(targetID, 1);
				}
				else {
					subCounts.put(targetID, subCounts.get(targetID)+1);
				}

				counts.put(id, subCounts);
			}
		}

		for(String key : counts.keySet()) {

			for(String subKey : counts.get(key).keySet()) {

				if(res.containsKey(key)) {

					if(counts.get(key).get(subKey) > counts.get(key).get(res.get(key))) {

						res.put(key, subKey);
					}
				}
				else
					res.put(key, subKey);
			}
		}

		String path = PATH.concat(defaultLabel.toString()).concat("_").concat(targetLabel.toString()).concat(".txt");

		FilesUtils.saveMapInFile(path, res);

		return res;
	}

	public static Map<String, String> metaCycToModelSEEDids() {

		MetaboliteMajorLabel label = MetaboliteMajorLabel.ModelSeed;

		Map<String, Map<MetaboliteMajorLabel, String>> compounds = FileUtils.readMapFromFile2(FetchCompoundsByName.METABOLITESIDS_PATH);

		Map<String, String> res = new HashMap<>();

		for(String key : compounds.keySet()) {

			Map<MetaboliteMajorLabel, String> identifiers = compounds.get(key);

			String metaCyc = null;

			if(identifiers.containsKey(MetaboliteMajorLabel.MetaCyc)) {

				metaCyc = identifiers.get(MetaboliteMajorLabel.MetaCyc);
			}
			else if(identifiers.containsKey(MetaboliteMajorLabel.EcoCyc)) {

				metaCyc = identifiers.get(MetaboliteMajorLabel.EcoCyc);
			}

			if(metaCyc != null && identifiers.containsKey(label)) {

				String targetID = identifiers.get(label);

				if(!res.containsKey(metaCyc)) {
					res.put(metaCyc, targetID);
				}
				else {

					Integer currentID = FetchCompoundsByName.getIDNumberFormat(targetID, label);

					Integer previousID = FetchCompoundsByName.getIDNumberFormat(res.get(metaCyc), label);

					if(previousID > currentID) {

						res.put(metaCyc, targetID);
					}
				}
			}
		}
		
		res.put("META:CPD-9728", "cpd15500");	//exception
		
		String path = PATH.concat("mappingMetaCyc_").concat(label.toString()).concat(".txt");

		FilesUtils.saveMapInFile(path, res);

		return res;
	}

	public static Map<String, String> metaCycToKEGGids(){

		Map<String, Map<MetaboliteMajorLabel, String>> compounds = FileUtils.readMapFromFile2(FetchCompoundsByName.METABOLITESIDS_PATH);

		Map<String, String> res = new HashMap<>();

		String metaCyc = null;
		String keggID = null;
		MetaboliteMajorLabel label = null;

		for(String key : compounds.keySet()) {

			Map<MetaboliteMajorLabel, String> identifiers = compounds.get(key);

			if(identifiers.containsKey(MetaboliteMajorLabel.LigandCompound) && identifiers.containsKey(MetaboliteMajorLabel.MetaCyc)) {

				keggID = identifiers.get(MetaboliteMajorLabel.LigandCompound);
				metaCyc = identifiers.get(MetaboliteMajorLabel.MetaCyc);
				label = MetaboliteMajorLabel.LigandCompound;

			}
			else if(identifiers.containsKey(MetaboliteMajorLabel.LigandCompound) && identifiers.containsKey(MetaboliteMajorLabel.EcoCyc)) {

				keggID = identifiers.get(MetaboliteMajorLabel.LigandCompound);
				metaCyc = identifiers.get(MetaboliteMajorLabel.EcoCyc);
				label = MetaboliteMajorLabel.LigandCompound;

			}
			else if(identifiers.containsKey(MetaboliteMajorLabel.LigandGlycan) && identifiers.containsKey(MetaboliteMajorLabel.MetaCyc)) {

				keggID = identifiers.get(MetaboliteMajorLabel.LigandGlycan);
				metaCyc = identifiers.get(MetaboliteMajorLabel.MetaCyc);
				label = MetaboliteMajorLabel.LigandGlycan;

			}
			else if(identifiers.containsKey(MetaboliteMajorLabel.LigandGlycan) && identifiers.containsKey(MetaboliteMajorLabel.EcoCyc)) {

				keggID = identifiers.get(MetaboliteMajorLabel.LigandGlycan);
				metaCyc = identifiers.get(MetaboliteMajorLabel.EcoCyc);
				label = MetaboliteMajorLabel.LigandGlycan;

			}

			if(metaCyc != null && keggID != null) {

				if(!res.containsKey(metaCyc)) {
					res.put(metaCyc, keggID);
				}
				else {

					Integer currentID = FetchCompoundsByName.getIDNumberFormat(keggID, label);

					Integer previousID = FetchCompoundsByName.getIDNumberFormat(res.get(metaCyc), label);

					if(previousID > currentID) {

						res.put(metaCyc, keggID);
					}
				}
			}
		}

		FilesUtils.saveMapInFile(PATH.concat("mappingMetaCyc_KEGG.txt"), res);

		return res;
	}

}
