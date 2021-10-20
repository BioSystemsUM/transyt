package pt.uminho.ceb.biosystems.transyt.service.transytDatabase;

import java.util.Map;

/**
 * @author Davide
 *
 */
public class TransytNode {

	private Map<String, String> properties;
	private int id;
	
	public TransytNode(int id, Map<String, String> properties) {
		
		this.id = id;
		this.properties = properties;
		
	}
	
	/**
	 * Check if a node contains a property.
	 * 
	 * @param property
	 * @return
	 */
	public boolean hasProperty(TransytGeneralProperties property) {
		
		if(properties.containsKey(property.toString()))
			return true;
		
		return false;
	}
	
	/**
	 * Get a specific node property.
	 * 
	 * @param property
	 * @return
	 */
	public String getProperty(TransytGeneralProperties property) {
		
		try {
			
			return properties.get(property.toString());
		} 
		catch (Exception e) {
			
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * Get node id in the database
	 * 
	 * @return
	 */
	public int getNodeID() {
		return id;
	}
}
