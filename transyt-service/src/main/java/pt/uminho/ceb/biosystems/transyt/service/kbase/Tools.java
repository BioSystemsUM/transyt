package pt.uminho.ceb.biosystems.transyt.service.kbase;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.uminho.ceb.biosystems.mew.biocomponents.container.Container;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.CompartmentCI;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.MetaboliteCI;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.io.readers.JSBMLReader;
import pt.uminho.ceb.biosystems.transyt.utilities.files.ReadExcelFile;

public class Tools {
	
	private static final Logger logger = LoggerFactory.getLogger(Tools.class);


	/**
	 * @param path
	 * @return
	 */
	public static Set<String> readModelMetabolites(String path) {

		Set<String> set = new HashSet<>();

		List<String[]> file = ReadExcelFile.getData(path, true, null);

		for(String[] line : file)
			set.add(line[8]);

		return set;
	}

	/**
	 * @param path
	 * @return
	 */
	public static Set<String> readModelMetabolitesFromSBML(String path) {
		
		Set<String> allMetabolites = new HashSet<>();;
		
		try {
			
			JSBMLReader reader = new JSBMLReader(path, "NoName");			
			Container cont = new Container(reader);
			
			Map<String, MetaboliteCI> metabolites = cont.getMetabolites();
			
			Map<String, CompartmentCI> compartments = cont.getCompartments();
			
			for(String metabolite : metabolites.keySet()) {
				
				metabolite = metabolite.replaceAll("^M_", "");
				
				String metaboliteAux = metabolite;
				
				for(String compartment : compartments.keySet()) {
					
					metabolite = metabolite.replaceAll("_".concat(compartment).concat("$"), "");
					
					if(!metabolite.equals(metaboliteAux)) {
						
						allMetabolites.add(metabolite);
						break;
					}
				}
			}
		}
		catch (Exception e) {
			logger.error("An error occurred while reading the model!!!!");
			allMetabolites = null;
//			e.printStackTrace();
		} 
		
		return allMetabolites;
	}
}
