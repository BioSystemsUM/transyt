package pt.uminho.ceb.biosystems.transyt.service.utilities;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import pt.uminho.ceb.biosystems.transyt.utilities.capsules.TcNumberContainer2;

public class Tests {

	public static void main(String[] args) throws IOException {  


		//	   	all_sequences = TransportersUtilities.filterTransmembraneGenes(all_sequences, transmembraneGenes, minimum_number_of_helices, project_id, statement);

		//		Map<String, Double> querySpecificThreshold = TransportersUtilities.getHelicesSpecificThresholds(transmembraneGenes, minimum_number_of_helices, _LIMIT);

//		LinkedHashMap<String, ProteinSequence> sequences = FastaReaderHelper.readFastaProteinSequence(new File("C:\\Users\\Davide\\coiso.faa"));
//
//		ConcurrentHashMap<String, AbstractSequence<?>> all_sequences = new ConcurrentHashMap<>();
//		
//		Map<String, Double> querySpecificThreshold = new HashMap<>();
//		
//		for (String id : sequences.keySet()){
//			all_sequences.put(id, sequences.get(id));
//			querySpecificThreshold.put(id, 0.2);
//		}
		
		
//		Map<String, FastaTcdb> fastaTcdb = ReadTcdbFastaFile.readfasta(false);
//		
//		
//		System.out.println(fastaTcdb);
		
		
		
		
//		try {
//			RunSimilaritySearch run_smith_waterman = new RunSimilaritySearch(TransportersUtilities.convertTcdbToMap(), 0.3,
//					Method.SmithWaterman, all_sequences, AlignmentScoreType.SIMILARITY);
//			
//			ConcurrentLinkedQueue<AlignmentCapsule> results = null;
//			
//			if(all_sequences.keySet().size()>0)
//							results = run_smith_waterman.runTransportSearch(querySpecificThreshold);
//				
//			for(AlignmentCapsule capsule : results)
//				System.out.println(capsule.getClosestOrthologues());
//
//				
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

				

		
		//		if(all_sequences.keySet().size()>0)
		//			results = run_smith_waterman.runTransportSearch(querySpecificThreshold);
		//		else
//		System.out.println("Transporter candidates already processed.");
		//			Workbench.getInstance().warn("Transporter candidates already processed.");


		//	   ReadTcdbFastaFile.readfasta();



		//       String[] ids = new String[] {"Q21691", "A8WS47", "O48771"};  
		//       try {  
		//           multipleSequenceAlignment(ids);  
		//           
		//       } catch (Exception e){  
		//           e.printStackTrace();  
		//       }  
	}

//	private static void multipleSequenceAlignment(String[] ids) throws Exception {  
//		List<ProteinSequence> lst = new ArrayList<ProteinSequence>();  
//		for (String id : ids) {  
//			lst.add(getSequenceForId(id));  
//		}  
//		Profile<ProteinSequence, AminoAcidCompound> profile = Alignments.getMultipleSequenceAlignment(lst);  
//		System.out.printf("Clustalw:%n%s%n", profile);  
//		ConcurrencyTools.shutdown();  
//	}
//
//	private static ProteinSequence getSequenceForId(String uniProtId) throws Exception {  
//		URL uniprotFasta = new URL(String.format("http://www.uniprot.org/uniprot/%s.fasta", uniProtId));  
//		ProteinSequence seq = FastaReaderHelper.readFastaProteinSequence(uniprotFasta.openStream()).get(uniProtId);  
//		System.out.printf("id : %s %s%n%s%n", uniProtId, seq, seq.getOriginalHeader());  
//		return seq;  
//	}

}