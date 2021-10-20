package pt.uminho.ceb.biosystems.transyt.utilities.transytUtilities;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pt.uminho.ceb.biosystems.transyt.utilities.files.ReadExcelFile;

public class Utilities {

	/**
	 * Print a given array
	 * 
	 * @param array
	 */
	public static void printArray(String[] array) {
		
		String text = "";
		
		for(int i = 0; i < array.length; i++)
			text = text.concat(array[i]).concat("; ");
			
		System.out.println(text);
		
	}
	
	/**
	 * Convert a Set<Long> to Set<String>
	 * 
	 * @param set
	 * @return
	 */
	public static Set<String> convertSetToSetString(Set<Long> set){
		
		Set<String> set2 = new HashSet<>();
		
		for(Long l : set)
			set2.add(l.toString());
		
		return set2;
	}
	
	/**
	 * Convert String to Set<String>
	 * 
	 * @param set
	 * @return
	 */
	public static Set<String> convertStringToSetString(String text, String regex){
		
		String[] words = text.split(regex);
		
		Set<String> set = new HashSet<>();
		
		for(String word : words) {
				
			if(!word.startsWith("cpd") && word.contains("cpd"))
				word = "cpd".concat(word.split("cpd")[1]);
			
			set.add(word.trim());
			
		}
		return set;
	}
	
	/**
	 * Convert a Set<Long> to Set<String>
	 * 
	 * @param set
	 * @return
	 */
	public static String processBiosynthName(String name){
				
		return name.replaceAll("<i>", "").replaceAll("</i>", "").replaceAll("\\[", "")
				.replaceAll("\\]", "").replaceAll("</sup>", "").replaceAll("<sup>", "").replaceAll("</I>", "").replaceAll("<I>", "")
				.replaceAll("</SUP>", "").replaceAll("<SUP>", "").replaceAll("</sub>", "").replaceAll("<sub>", "").replaceAll("^(an*\\s+)", "")
				.replaceAll("</SUB>", "").replaceAll("<SUB>", "").replaceAll("</sue>", "").replaceAll("<sue>", "").replaceAll("</suh>", "").replaceAll("<suh>", "");
	}
	
	/**
	 * @param map
	 * @return
	 */
	public static String getKeyWithMaxValue(Map<String, Double> map) {
		
		double max = 0.0;
		String value = "";
		
		for(String key : map.keySet()) {
			
			if(map.get(key) > max) {
				max = map.get(key);
				value = key;
			}
		}
		return value;
	}

}
