package pt.uminho.ceb.biosystems.transyt.utilities.transytUtilities;

import java.io.File;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.uminho.ceb.biosystems.transyt.utilities.enumerators.MetaboliteReferenceDatabaseEnum;
import pt.uminho.ceb.biosystems.transyt.utilities.files.FilesUtils;

/**
 * @author Davide
 *
 */
public class Properties {

	private static final int MINIMUM_DEFAULT_BETA_STRANDS = 8;
	private static final int MINIMUM_DEFAULT_HELICES = 1;
	private static final double SIMILARITY_DEFAULT = 0.3;

	private Map<String, String> allProperties;
	private double blastEvalue;
	private double percentageAcceptance;
	private double limitEvalueAcceptance;
	private double similarityThreshold;
	private String blastpExecutableAlias;
	private double eValueThreshold;
	private double bitScore;
	private double queryCoverage;
	private double compartmentsAllowedDifference;
	private MetaboliteReferenceDatabaseEnum defaultLabel;
	private int minimumBetaStrands;
	private int minimumAlphaHelices;
	private String databaseURI;
	private String databaseUsername;
	private String databasePassword;
	private double alpha;
	private double alphaFamiliesAnnotation;
	private double beta;
	private double reactionsAnnotationScoreThreshold;
	private int minimumHits;
	private boolean overrideCommonOntologyFilter;
	private Integer taxID;
	private String referenceDatabaseURI;
	private String referenceDatabaseUsername;
	private String referenceDatabasePassword;
	private boolean ignoreMethod1 = false;
	private boolean ignoreMethod2 = false;
	private boolean forceBlast = true;
	private boolean acceptUnknownFamily = false;

	private static final Logger logger = LoggerFactory.getLogger(Properties.class);

	public Properties() {

		this.allProperties = FilesUtils.readPropertiesFile(false);

		boolean go = assignVariables();

		if(!go) {
			logger.error("An error occurred while reading the configuration file. The default values will be used!");

			allProperties = FilesUtils.readPropertiesFile(true);
		}

		go = assignVariables();

		if(go) {

			logger.info("Configurations in use: {}", allProperties);
		}
		else {

			logger.error("The default configurations can not be loaded.");
			logger.error("TranSyT found a fatal error, the program is closing...");
			System.exit(3);
		}
	}

	/**
	 * This constructor is used when the user input must be taken into account
	 * 
	 * @param filePath
	 */
	public Properties(String filePath) {

		this.allProperties = FilesUtils.readPropertiesFile(false);

		boolean go = assignVariables();

		if(!go){

			logger.error("The default configurations can not be loaded.");
			logger.error("TranSyT found a fatal error, the program is closing...");
			System.exit(3);
		}


		File userParams = new File(filePath);

		if(userParams.exists()) {
			logger.debug("Reading user parameters...");
			readUserParameters(filePath);
		}
		else {
			logger.error("User parameters file not found!");
		}
		
		logger.info("Configurations in use: {}", allProperties);
	}

	/**
	 * Read parameters possibly manually changed by the user and update the object.
	 * 
	 * @param filePath
	 */
	public void readUserParameters(String filePath) {

		Map<String, String> params = FilesUtils.readMapFromFile(filePath);

		for(String key : params.keySet()) {

			
			switch (key) {

			case "reference_database":
				
				this.defaultLabel = selectDefaultDatabase(params.get("reference_database"));
				break;

			case "auto_accept_evalue":
				
				this.eValueThreshold = Double.valueOf(params.get("auto_accept_evalue"));
				break;

			case "override_ontologies_filter":
				
				this.overrideCommonOntologyFilter = Boolean.valueOf(params.get("override_ontologies_filter"));
				break;

			case "blast_evalue_threshold":
				
				this.blastEvalue = Double.valueOf(params.get("blast_evalue_threshold"));
				break;

			case "bitscore_threshold":

				this.bitScore = Double.valueOf(params.get("bitscore_threshold"));
				break;

			case "query_coverage_threshold":

				this.queryCoverage = Double.valueOf(params.get("query_coverage_threshold"));
				break;

			case "similarity_score":

				this.setSimilarityThreshold(params.get("similarity_score"));
				break;

			case "alpha":
				
				this.alpha = Double.valueOf(params.get("alpha"));
				break;

			case "beta":

				this.beta = Double.valueOf(params.get("beta"));
				break;
				
			case "minimum_hits_penalty":

				setMinimumHits(Integer.valueOf(params.get("minimum_hits_penalty")));
				break;
				
			case "score_threshold":

				this.reactionsAnnotationScoreThreshold = Double.valueOf(params.get("score_threshold"));
				break;
				
			case "alpha_families":

				this.alphaFamiliesAnnotation = Double.valueOf(params.get("alpha_families"));
				break;
				
			case "taxID":

				this.taxID = Integer.valueOf(params.get("taxID"));
				break;
			
			case "percent_accept":

				this.percentageAcceptance =  Double.valueOf(allProperties.get("percent_accept"));
				break;
				
			case "limit_evalue_accept":

				this.limitEvalueAcceptance =  Double.valueOf(allProperties.get("limit_evalue_accept"));
				break;
				
			case "ignore_m2":

				this.ignoreMethod2 =  Boolean.valueOf(allProperties.get("ignore_m2"));
				break;
				
			case "force_blast":

				this.forceBlast =  Boolean.valueOf(allProperties.get("force_blast"));
				break;
				
			case "accept_unk_family":

				this.acceptUnknownFamily =  Boolean.valueOf(allProperties.get("accept_unk_family"));
				break;
				
			default:
				break;
			}
		}

	}

	/**
	 * @return the bitScore
	 */
	public double getBitScore() {
		return bitScore;
	}


	/**
	 * @return the queryCoverage
	 */
	public double getQueryCoverage() {
		return queryCoverage;
	}


	/**
	 * 
	 */
	private boolean assignVariables(){ 

		try {

			blastEvalue = Double.valueOf(allProperties.get("Evalue"));

			this.setSimilarityThreshold(allProperties.get("similarity"));

			blastpExecutableAlias = allProperties.get("blastpAlias");

			databaseURI = allProperties.get("uri");

			databaseUsername = allProperties.get("username");

			databasePassword = allProperties.get("password");
			
			referenceDatabaseURI = allProperties.get("ref_uri");

			referenceDatabaseUsername = allProperties.get("ref_username");

			referenceDatabasePassword = allProperties.get("ref_password");

			bitScore = Double.valueOf(allProperties.get("bitScore"));

			queryCoverage = Double.valueOf(allProperties.get("coverage"));

			eValueThreshold = Double.valueOf(allProperties.get("EvalueAccept"));

			defaultLabel = selectDefaultDatabase(allProperties.get("IDsDatabase"));

			setCompartmentsAllowedDifference(allProperties.get("difference"));

			setMinimumAlphaHelices(allProperties.get("helices"));

			setMinimumBetaStrands(allProperties.get("strands"));
			
			setMinimumHits(Integer.valueOf(allProperties.get("minHits")));
			
			this.alpha = Double.valueOf(allProperties.get("alpha"));
			
			this.alphaFamiliesAnnotation = Double.valueOf(allProperties.get("alphaFamilies"));
			
			this.beta = Double.valueOf(allProperties.get("beta"));
			
			this.reactionsAnnotationScoreThreshold = Double.valueOf(allProperties.get("annotationScore"));
			
			this.overrideCommonOntologyFilter = Boolean.valueOf(allProperties.get("overrideCommonOnt"));
			
			this.percentageAcceptance =  Double.valueOf(allProperties.get("percent_accept"));
			
			this.limitEvalueAcceptance =  Double.valueOf(allProperties.get("limit_evalue_accept"));
			
			this.ignoreMethod1 =  Boolean.valueOf(allProperties.get("ignore_m1"));
			
			this.ignoreMethod2 =  Boolean.valueOf(allProperties.get("ignore_m2"));
			
			this.forceBlast = Boolean.valueOf(allProperties.get("force_blast"));
			
		}
		catch(Exception e) {

			e.printStackTrace();

			return false;
		}

		return true;
	}

	/**
	 * 
	 */
	public MetaboliteReferenceDatabaseEnum selectDefaultDatabase(String text) {

		if(text.equalsIgnoreCase(MetaboliteReferenceDatabaseEnum.ModelSEED.toString()))
			return MetaboliteReferenceDatabaseEnum.ModelSEED;

		else if(text.equalsIgnoreCase(MetaboliteReferenceDatabaseEnum.KEGG.toString()))
			return MetaboliteReferenceDatabaseEnum.KEGG;

		else if(text.equalsIgnoreCase(MetaboliteReferenceDatabaseEnum.BiGG.toString()))
			return MetaboliteReferenceDatabaseEnum.BiGG;

		else if(text.equalsIgnoreCase(MetaboliteReferenceDatabaseEnum.MetaCyc.toString()))
			return MetaboliteReferenceDatabaseEnum.MetaCyc;

		return null;

	}

	/**
	 * @return the blastEvalue
	 */
	public double getBlastEvalueThreshold() {
		return blastEvalue;
	}

	/**
	 * @return the eValueThreshold
	 */
	public double geteValueThreshold() {
		return eValueThreshold;
	}


	/**
	 * @param queryCoverage the queryCoverage to set
	 */
	public void setQueryCoverage(double queryCoverage) {
		this.queryCoverage = queryCoverage;
	}


	/**
	 * @param eValueThreshold the eValueThreshold to set
	 */
	public void seteValueThreshold(double eValueThreshold) {
		this.eValueThreshold = eValueThreshold;
	}


	/**
	 * @return the defaultLabel
	 */
	public MetaboliteReferenceDatabaseEnum getDefaultLabel() {
		return defaultLabel;
	}


	public void setDefaultLabel(MetaboliteReferenceDatabaseEnum defaultLabel) {
		this.defaultLabel = defaultLabel;
	}


	/**
	 * @return the compartmentsAllowedDifference
	 */
	public double getCompartmentsAllowedDifference() {
		return compartmentsAllowedDifference;
	}


	/**
	 * @param compartmentsAllowedDifference the compartmentsAllowedDifference to set
	 */
	public void setCompartmentsAllowedDifference(String compartmentsAllowedDifference) {

		double diff = Double.valueOf(compartmentsAllowedDifference.replace("%", ""));

		if(diff >= 0 && diff <= 50) {

			this.compartmentsAllowedDifference = diff;
		}
		else {
			this.compartmentsAllowedDifference = 10.0;

			logger.warn("Allowed difference between compartments value in configuration file not recognized. Default value {} assumed!", compartmentsAllowedDifference);
		}

	}


	/**
	 * @return the minimumBetaStrands
	 */
	public int getMinimumBetaStrands() {
		return minimumBetaStrands;
	}


	/**
	 * @param minimumBetaStrands the minimumBetaStrands to set
	 */
	public void setMinimumBetaStrands(String string) {
		try {
			minimumBetaStrands = Integer.parseInt(string);
		} 
		catch (Exception e) {

			this.minimumBetaStrands = MINIMUM_DEFAULT_BETA_STRANDS;

			logger.warn("Number of minimum beta stands in configuration file for beta barrels identification not recognized. Default value {} assumed!", minimumBetaStrands);
		}
	}

	/**
	 * @return the minimumAlphaHelices
	 */
	public int getMinimumAlphaHelices() {
		return minimumAlphaHelices;
	}


	/**
	 * @param string the minimumAlphaHelices to set
	 */
	public void setMinimumAlphaHelices(String string) {

		try {
			minimumAlphaHelices = Integer.parseInt(string);
		} 
		catch (Exception e) {

			this.minimumAlphaHelices = MINIMUM_DEFAULT_HELICES;
			logger.warn("Number of minimum helices in configuration file not recognized. Default value {} assumed!", minimumAlphaHelices);
		}
	}

	/**
	 * @return the similarityThreshold
	 */
	public double getSimilarityThreshold() {
		return similarityThreshold;
	}


	/**
	 * @param similarityThreshold the similarityThreshold to set
	 */
	public void setSimilarityThreshold(String similarityThreshold) {

		try {
			Double sim = Double.valueOf(similarityThreshold); 
			this.similarityThreshold = sim;
		} 
		catch (NumberFormatException e) {

		}
	}


	/**
	 * @return the transytDBName
	 */
	public String getBlastpExecutableAlias() {
		return blastpExecutableAlias;
	}


	/**
	 * @return the databaseURI
	 */
	public String getDatabaseURI() {
		return databaseURI;
	}

	/**
	 *
	 */
	public void setDatabaseURI(String databaseURI) {
		this.databaseURI = databaseURI;
	}


	/**
	 * @return the databaseUsername
	 */
	public String getDatabaseUsername() {
		return databaseUsername;
	}


	/**
	 * @return the databasePassword
	 */
	public String getDatabasePassword() {
		return databasePassword;
	}

	public double getAlpha() {
		return alpha;
	}

	public void setAlpha(double alpha) {
		this.alpha = alpha;
	}

	public double getAlphaFamiliesAnnotation() {
		return alphaFamiliesAnnotation;
	}

	public void setAlphaFamiliesAnnotation(double alphaFamiliesAnnotation) {
		this.alphaFamiliesAnnotation = alphaFamiliesAnnotation;
	}

	public double getBeta() {
		return beta;
	}

	public void setBeta(double beta) {
		this.beta = beta;
	}

	public double getReactionsAnnotationScoreThreshold() {
		return reactionsAnnotationScoreThreshold;
	}

	public void setReactionsAnnotationScoreThreshold(double reactionsAnnotationScoreThreshold) {
		this.reactionsAnnotationScoreThreshold = reactionsAnnotationScoreThreshold;
	}

	public int getMinimumHits() {
		return minimumHits;
	}

	public void setMinimumHits(int minimumHits) {
		this.minimumHits = minimumHits;
	}

	/**
	 * @return the overrideCommonOntologyFilter
	 */
	public boolean isOverrideCommonOntologyFilter() {
		return overrideCommonOntologyFilter;
	}

	/**
	 * @param overrideCommonOntologyFilter the overrideCommonOntologyFilter to set
	 */
	public void setOverrideCommonOntologyFilter(boolean overrideCommonOntologyFilter) {
		this.overrideCommonOntologyFilter = overrideCommonOntologyFilter;
	}

	/**
	 * @return the taxID
	 */
	public Integer getTaxID() {
		return taxID;
	}

	/**
	 * @param taxID the taxID to set
	 */
	public void setTaxID(Integer taxID) {
		this.taxID = taxID;
	}

	/**
	 * @return the referenceDatabaseURI
	 */
	public String getReferenceDatabaseURI() {
		return referenceDatabaseURI;
	}

	/**
	 * @param referenceDatabaseURI the referenceDatabaseURI to set
	 */
	public void setReferenceDatabaseURI(String referenceDatabaseURI) {
		this.referenceDatabaseURI = referenceDatabaseURI;
	}

	/**
	 * @return the referenceDatabaseUsername
	 */
	public String getReferenceDatabaseUsername() {
		return referenceDatabaseUsername;
	}

	/**
	 * @param referenceDatabaseUsername the referenceDatabaseUsername to set
	 */
	public void setReferenceDatabaseUsername(String referenceDatabaseUsername) {
		this.referenceDatabaseUsername = referenceDatabaseUsername;
	}

	/**
	 * @return the referenceDatabasePassword
	 */
	public String getReferenceDatabasePassword() {
		return referenceDatabasePassword;
	}

	/**
	 * @param referenceDatabasePassword the referenceDatabasePassword to set
	 */
	public void setReferenceDatabasePassword(String referenceDatabasePassword) {
		this.referenceDatabasePassword = referenceDatabasePassword;
	}

	/**
	 * @return the percentageAcceptance
	 */
	public double getPercentageAcceptance() {
		return percentageAcceptance;
	}

	/**
	 * @param percentageAcceptance the percentageAcceptance to set
	 */
	public void setPercentageAcceptance(double percentageAcceptance) {
		this.percentageAcceptance = percentageAcceptance;
	}

	/**
	 * @return the limitEvalueAcceptance
	 */
	public double getLimitEvalueAcceptance() {
		return limitEvalueAcceptance;
	}

	/**
	 * @param limitEvalueAcceptance the limitEvalueAcceptance to set
	 */
	public void setLimitEvalueAcceptance(double limitEvalueAcceptance) {
		this.limitEvalueAcceptance = limitEvalueAcceptance;
	}

	/**
	 * @return the ignoreMethod1
	 */
	public boolean isIgnoreMethod1() {
		return ignoreMethod1;
	}

	/**
	 * @param ignoreMethod1 the ignoreMethod1 to set
	 */
	public void setIgnoreMethod1(boolean ignoreMethod1) {
		this.ignoreMethod1 = ignoreMethod1;
	}

	/**
	 * @return the ignoreMethod2
	 */
	public boolean isIgnoreMethod2() {
		return ignoreMethod2;
	}

	/**
	 * @param ignoreMethod1 the ignoreMethod1 to set
	 */
	public void setIgnoreMethod2(boolean ignoreMethod2) {
		this.ignoreMethod2 = ignoreMethod2;
	}

	/**
	 * @return the forceBlast
	 */
	public boolean isForceBlast() {
		return forceBlast;
	}

	/**
	 * @param forceBlast the forceBlast to set
	 */
	public void setForceBlast(boolean forceBlast) {
		this.forceBlast = forceBlast;
	}

	/**
	 * @return the acceptUnknownFamily
	 */
	public boolean isAcceptUnknownFamily() {
		return acceptUnknownFamily;
	}

	/**
	 * @param acceptUnknownFamily the acceptUnknownFamily to set
	 */
	public void setAcceptUnknownFamily(boolean acceptUnknownFamily) {
		this.acceptUnknownFamily = acceptUnknownFamily;
	}
}
