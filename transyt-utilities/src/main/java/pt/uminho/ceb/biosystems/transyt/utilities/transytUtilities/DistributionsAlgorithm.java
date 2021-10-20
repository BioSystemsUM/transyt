package pt.uminho.ceb.biosystems.transyt.utilities.transytUtilities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DistributionsAlgorithm {

	/**
	 * @param sets
	 * @return
	 */
	public static List<Set<String>> getAllDistributions(List<Set<String>> sets, boolean combineItself){

		List<Set<String>> set1 = new ArrayList<>();
		List<Set<String>> set2 = new ArrayList<>();
		
		if(combineItself) {
			for(Set<String> set : new ArrayList<>(sets))
				sets.add(set);
		}

		for(int i = 0; i < sets.size(); i++) {

			if(i == 0) {
				for(String met : sets.get(i)) {

					Set<String> set = new HashSet<>();

					set.add(met);
					set1.add(set);
				}
			}
			else
				set2.add(sets.get(i));
		}

		return executeDistributions(set1, set2, 0);

	}

	/**
	 * Recursive method to find all possible distributions of compounds.
	 * 
	 * @param set1
	 * @param set2
	 * @param cycle
	 * @return
	 */
	private static List<Set<String>> executeDistributions(List<Set<String>> set1, List<Set<String>> set2, int cycle){

		List<Set<String>> distributions = new ArrayList<>();

		if(cycle == 0 && set2.size() == 0) {

			for(Set<String> set : set1) {

				for(String metabolite : set) {

					Set<String> newSet = new HashSet<>();

					newSet.add(metabolite);
					distributions.add(newSet);

				}
			}
			return distributions;
		}

		if(cycle == set2.size()) 
			return set1;

		Set<String> l2 = set2.get(cycle);

		for(Set<String> er : set1) {

			for(String el: l2) {

				Set<String> erClone = new HashSet<>(er);

				erClone.add(el);

				distributions.add(erClone);
			}

		}

		set1.clear();

		set1.addAll(distributions);

		cycle++;

		return executeDistributions(set1, set2, cycle);

	}
	
	public static List<Set<String>> bruteForceCombinations(List<String> items) {
		
		List<Set<String>> distributions = new ArrayList<>();
		
		for(String item : items) {
			for(String item2 : items) {
				
				Set<String> combination = new HashSet<>();
				combination.add(item);
				combination.add(item2);
				
				if(combination.size() > 1)
					distributions.add(combination);
			}
		}
		
		return distributions;
	}
}
