package pt.uminho.ceb.biosystems.transyt.service.transytDatabase;

import java.util.Map;

public class TransytRelationship {
	
	private int id;
	private TransytNode node;
	private Map<String, String> properties;
	private TransytRelationshipType type;

	public TransytRelationship(int id, Map<String, String> properties, TransytRelationshipType type,
			TransytNode endNode){
		
		this.id = id;
		this.node = endNode;
		this.properties = properties;
		this.type = type;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return the node
	 */
	public TransytNode getOtherEndNode() {
		return node;
	}

	/**
	 * @return the properties
	 */
	public Map<String, String> getProperties() {
		return properties;
	}

	/**
	 * @return the type
	 */
	public TransytRelationshipType getType() {
		return type;
	}
	
	/**
	 * Check if a relationship contains a property.
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
	 * Get a specific relationship property.
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

}
