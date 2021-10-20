package pt.uminho.ceb.biosystems.transyt.service.utilities;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.biojava.nbio.core.sequence.ProteinSequence;
import org.biojava.nbio.core.sequence.compound.AminoAcidCompound;
import org.biojava.nbio.core.sequence.compound.AminoAcidCompoundSet;
import org.biojava.nbio.core.sequence.io.FastaReader;
import org.biojava.nbio.core.sequence.io.GenericFastaHeaderParser;
import org.biojava.nbio.core.sequence.io.ProteinSequenceCreator;
import org.biojava.nbio.core.sequence.template.AbstractSequence;

import pt.uminho.ceb.biosystems.transyt.utilities.connection.TcdbExplorer;
import pt.uminho.sysbio.biosynth.integration.io.dao.neo4j.MetaboliteMajorLabel;

public class FileUtils {
	
	public static void saveMapInFile2(String path, Map<String, Map<MetaboliteMajorLabel, String>> map) {

		try {

			PrintWriter writer = new PrintWriter(path, "UTF-8");

			for(String key : map.keySet())
				writer.println(key  + "\t" + map.get(key));


			writer.close();

		} 
		catch (FileNotFoundException e) {
			e.printStackTrace();

		} 
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public static Map<String, Map<MetaboliteMajorLabel, String>> readMapFromFile2(String path){

		Map<String, Map<MetaboliteMajorLabel, String>> dic = new HashMap<>();

		try {
			BufferedReader br = new BufferedReader(new FileReader(path));

			String line = br.readLine();

			int i = 0;

			while (line != null) {

				i++;

				Map<MetaboliteMajorLabel, String> newMap = new HashMap<>();

				if(!line.isEmpty()){

					try {
						String[] content = line.split("\t");

						if(content.length > 1) {

							String[] values = content[1].replaceAll("[{}]", "").split(", ");

							for(String value : values) {

								String[] entry = value.split("=");

								MetaboliteMajorLabel label = MetaboliteMajorLabel.valueOf(entry[0]);

								String id = entry[1];

								newMap.put(label, id);
							}
							dic.put(content[0], newMap);
						}
					} 
					catch (IllegalArgumentException e1) {}
					catch (Exception e) {

						System.out.println("line = " + i);
						System.out.println(line);
						e.printStackTrace();
					}
				}

				line = br.readLine();
			}

			br.close();

		} 
		catch(Exception e) {
			e.printStackTrace();
		}
		return dic;
	}
	
}
