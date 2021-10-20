package pt.uminho.ceb.biosystems.transyt.utilities.files;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import pt.uminho.ceb.biosystems.transyt.utilities.capsules.ReactionContainer;

public class XMLFilesReader {

	private static final Logger logger = LoggerFactory.getLogger(XMLFilesReader.class);

	/**
	 * Get information from TCDB page.
	 * @param br 
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static Map<String, ReactionContainer> scrapeReactionInfo(BufferedReader br, String acc) {

		Map<String, ReactionContainer> containers = new HashMap<>();

		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(removeErrorsFromXML(br));

			NodeList reactions = doc.getElementsByTagName("Reaction");

			for(int i = 0; i < reactions.getLength(); i++) {
				Node node = reactions.item(i);
				if(node.getNodeType() == Node.ELEMENT_NODE) {

					try {
						Element element = (Element) node;
						String reactionId = element.getAttribute("ID");

						if(reactionId != null && !reactionId.isBlank()) {

							Element reversibilityElement = (Element) element.getElementsByTagName("reaction-direction").item(0);

							String reversibility = null;

							if(reversibilityElement != null)
								reversibility = reversibilityElement.getTextContent();
							else
								reversibility = "LEFT-TO-RIGHT";

							String reactant = buildReactant(element.getElementsByTagName("left"));
							String product = buildReactant(element.getElementsByTagName("right"));

							if(reactant != null && product != null && reversibility != null) {

								if(reactant.contains(" null") || product.contains(" null")) {

									String[] reactants = reactant.split(" \\+ ");
									String[] products = product.split(" \\+ ");

									for(String r : reactants) {
										r = r.replace(" null", "").trim();
										for(String p : products) {
											p = p.trim();
											if(p.matches(r + "\\s+" + ReactionContainer.INTERIOR_COMPARTMENT_TOKEN_REG)) {
												reactant = reactant.replaceAll(r + "\\s+null", r + " " + ReactionContainer.EXTERIOR_COMPARTMENT_TOKEN_REG);
											}
											else if (p.matches(r + "\\s+" + ReactionContainer.EXTERIOR_COMPARTMENT_TOKEN_REG)){
												reactant = reactant.replaceAll(r + "\\s+null", r + " " + ReactionContainer.INTERIOR_COMPARTMENT_TOKEN_REG);
											}
										}
									}
									for(String p : products) {
										p = p.replace(" null", "");
										for(String r : reactants) {
											if(r.matches(p + "\\s+" + ReactionContainer.INTERIOR_COMPARTMENT_TOKEN_REG)) {
												product = product.replaceAll(p + "\\s+null", p + " " + ReactionContainer.EXTERIOR_COMPARTMENT_TOKEN_REG);
											}
											else if (r.matches(p + "\\s+" + ReactionContainer.EXTERIOR_COMPARTMENT_TOKEN_REG)){
												product = product.replaceAll(p + "\\s+null", p + " " + ReactionContainer.INTERIOR_COMPARTMENT_TOKEN_REG);
											}
										}
									}
								}
								
								boolean reversible = true;

								if(reversibility.matches(".*LEFT-TO-RIGHT.*")) {
									reversible = false;
								}
								else if(reversibility.matches(".*RIGHT-TO-LEFT.*")) {
									String aux = reactant;
									reactant = product;
									product = aux;
									reversible = false;
								}
								else if(!reversibility.equalsIgnoreCase("REVERSIBLE")) {
									logger.warn("Unknown direction found:" + reversibility);
								}
								ReactionContainer container = new ReactionContainer(reactant, product, reversible);
								container.setMetaReactionID(reactionId);
								containers.put(reactionId, container);
							}
							else {
								
								logger.error("Could not retrieve reaction " + reactionId + " from accession " + acc);
							}
						}
					} 
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		} catch (ParserConfigurationException e) {
			System.out.println(acc);
			e.printStackTrace();
		} catch (SAXException e) {
			System.out.println(acc);
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println(acc);
			e.printStackTrace();
		}

		return containers;
	}

	/**
	 * Get information from TCDB page.
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static String retrieveGeneId(BufferedReader br) {

		String geneId = null;

		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(removeErrorsFromXML(br));

			NodeList genes = doc.getElementsByTagName("Gene");

			for(int i = 0; i < genes.getLength(); i++) {
				Node node = genes.item(i);
				if(node.getNodeType() == Node.ELEMENT_NODE) {

					Element element = (Element) node;
					String geneAux = element.getAttribute("ID");

					if(geneAux != null && !geneAux.isBlank()) {
						geneId = geneAux.trim();
					}
				}
			}
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return geneId;
	}

	/**
	 * Remove unwanted information from the resource tag content.
	 * 
	 * @param resource
	 * @return
	 */
	private static String scrapeResource(String resource) {

		return resource != null ? resource.split("\\?")[1] : null;
	}

	/**
	 * Decide the relative compartment based on the MetaCyc identifier.
	 * 
	 * @param resource
	 * @return
	 */
	private static String decideRelativePosition(String resource) {

		if(resource != null) {
			if(resource.contains("IN") || resource.contains("CYTOSOL"))
				return ReactionContainer.INTERIOR_COMPARTMENT_TOKEN;
			else if(resource.contains("MIDDLE"))
				return ReactionContainer.MIDDLE_COMPARTMENT_TOKEN;
			else if(resource.contains("OUT"))
				return ReactionContainer.EXTERIOR_COMPARTMENT_TOKEN;
		}
		return null;
	}

	private static String buildReactant(NodeList reactantChilds) throws Exception{

		String reactant = "";
		String previousCompt = "";

		for(int j = 0; j < reactantChilds.getLength(); j++) {
			Node reactantNode = reactantChilds.item(j);
			if(reactantNode.getNodeType() == Node.ELEMENT_NODE) {
				Element reactantElement = (Element) reactantNode;

				Element compound = (Element) reactantElement.getElementsByTagName("Compound").item(0);
				
				String cpd = null;

				if(compound == null) {
					compound = (Element) reactantElement.getElementsByTagName("Protein").item(0);
					
					if(compound == null)
						return null;
					
					cpd = scrapeResource(compound.getAttribute("resource"));
					
					if(cpd.contains(":PTSH-PHOSPHORYLATED"))	//this can be done because TranSyT will later add these enzymes to the GPRs
						cpd = cpd.replace(":PTSH-PHOSPHORYLATED", ":PHOSPHO-ENOL-PYRUVATE");
					else if(cpd.contains(":PTSH-MONOMER"))
						cpd = cpd.replace(":PTSH-MONOMER", ":PYRUVATE");
					else
						return null;
					
				}
				else {
					cpd = scrapeResource(compound.getAttribute("resource"));
				}
				
				Element coefficient = (Element) reactantElement.getElementsByTagName("coefficient").item(0);
				String coeff = "";

				if(coefficient != null) {
					coeff = coefficient.getTextContent();
					if(coeff.matches("\\d+") && !coeff.equals("1"))
						cpd = coeff.concat("$").concat(cpd);
				}

				Element compartment = (Element) reactantElement.getElementsByTagName("cco").item(0);

				String cmpt = null;

				if(compartment != null && compartment.hasAttribute("resource"))
					cmpt = decideRelativePosition(scrapeResource(compartment.getAttribute("resource")));

				if(cmpt != null)
					previousCompt = cmpt;
				else if(cmpt == null && previousCompt != null) {
					cmpt = previousCompt;
				}

				reactant = reactant.concat(cpd + " " + cmpt + " + ");
			}

		}

		return reactant.replaceAll("\\s+\\+\\s+$", "");
	}

	/**
	 * MetaCyc's XML files have several errors. This helps remove them.
	 * @param br
	 * @return
	 * @throws IOException
	 */
	private static InputStream removeErrorsFromXML(BufferedReader br) throws IOException {
		StringBuilder sb = new StringBuilder();
		String line;

		boolean ignore = false;

		while ((line = br.readLine()) != null) {

			boolean save = true;

			if(line.startsWith("This XML file does")) {
				save = false;
			}
			else if(line.equals("<query>")) {	//prevents following lines from being saved
				save = false;
				ignore = true;
			}
			else if(line.equals("</query>")) {	//will start saving only in the next line
				save = false;
				ignore = false;
			}

			if(save && !ignore)
				sb.append(line.concat("\n"));
		}

		String theString = sb.toString().replace("</p>", "").replace("<p>", "");
		byte[] bytes = theString.getBytes("utf-8");
		InputStream inputStream =  new ByteArrayInputStream(bytes);
		return inputStream;
	}

}
