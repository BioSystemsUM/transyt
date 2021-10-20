package pt.uminho.ceb.biosystems.transyt.scraper.tcdb.utilities;

import java.util.HashSet;
import java.util.Set;

import pt.uminho.ceb.biosystems.transyt.utilities.capsules.TcdbMetabolitesContainer;

public class TRIAGEtests {

public static void main(String[] args) {
		
		try {
			
			Set<String> metabolites = new HashSet<>();
			
			metabolites.add("aief");
			metabolites.add("H+");

			System.out.println(metabolites);
			
			metabolites.remove(TcdbMetabolitesContainer.PROTON);
			
			System.out.println(metabolites);
			
//			String metabolite = "nitrate     + H+  +    Cd2+";
//			
//			String subString = evidenceOfSymOrAnti.substring(0, evidenceOfSymOrAnti.length()-1);

//			metabolite = metabolite.replaceAll("\\s+", " ");
			
//			Synonyms dictionary = new Synonyms();
//			
//			System.out.println(dictionary.getSynonym("tax�carrierprotein"));
//			System.out.println(dictionary.getMetabolitesDictionary().get("Protein"));
//			System.out.println(dictionary.isChildOf("Carbohydrate acid", "Gluconate"));
			
//			System.out.println(dictionary.isChildOf("Metal ion", metabolite));
//				System.out.println("yes");
			
//			metabolite = metabolite.replaceAll("(?i)sugars*", "2w");
//			
//			System.out.println(metabolite);
			
//			Set<String> set = new HashSet<>();
//			
//			set.add("sugar");
//			set.add("arabinose");
//			set.add("h2o");
//			
//			Pattern p = Pattern.compile("\\b(?i)\\w+ose\\b");
//		    Matcher m = p.matcher("Sugarsd (out) + H+ (out)  $REV$  maltoseds (in) + H+ (in)");
//		    m.find();
//		    
//		    System.out.println(m.find());
//		    System.out.println(m.group());
//		    
//		    set.remove("sugar");
//		    set.add(m.group());
//		    System.out.println(set);
//			
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
}
