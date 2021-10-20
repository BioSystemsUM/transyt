package pt.uminho.ceb.biosystems.transyt.scraper.APIs;

import java.util.List;

import pt.uminho.ceb.biosystems.merlin.bioapis.externalAPI.ebi.chebi.ChebiAPIInterface;
import uk.ac.ebi.chebi.webapps.chebiWS.client.ChebiWebServiceClient;
import uk.ac.ebi.chebi.webapps.chebiWS.model.DataItem;
import uk.ac.ebi.chebi.webapps.chebiWS.model.Entity;
import uk.ac.ebi.chebi.webapps.chebiWS.model.LiteEntity;
import uk.ac.ebi.chebi.webapps.chebiWS.model.LiteEntityList;
import uk.ac.ebi.chebi.webapps.chebiWS.model.SearchCategory;
import uk.ac.ebi.chebi.webapps.chebiWS.model.StarsCategory;

public class ChebiAPI extends ChebiAPIInterface{

	static private ChebiWebServiceClient chebiClient = new ChebiWebServiceClient();

	public static String getMetacycIDUsingExternalReference(String id) {

		String identifier = null;

		try {
			ChebiWebServiceClient chebiClient = new ChebiWebServiceClient();

			LiteEntityList entities = chebiClient.getLiteEntity(id, SearchCategory.MANUAL_XREFS, 1, StarsCategory.THREE_ONLY);

			List<LiteEntity> resultList = entities.getListElement();

			String chebiID = "";

			for (LiteEntity liteEntity : resultList ) {
				chebiID = liteEntity.getChebiId();
			}

			if(chebiID != null && !chebiID.isEmpty()) {
				Entity entity = chebiClient.getCompleteEntity(chebiID);

				List<DataItem> db = entity.getDatabaseLinks();
				for ( DataItem dataItem : db ) { // List all synonyms

					if(dataItem.getType().trim().equalsIgnoreCase("MetaCyc accession")) {
						identifier = dataItem.getData();
						break;
					}
				}
			}
		}
		catch (Exception e1) {
			e1.printStackTrace();
		}

		return identifier;
	}

}
