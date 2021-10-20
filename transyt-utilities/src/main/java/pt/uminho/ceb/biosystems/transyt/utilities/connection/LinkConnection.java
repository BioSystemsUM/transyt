package pt.uminho.ceb.biosystems.transyt.utilities.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LinkConnection {
	
	private URL url;
	
	public LinkConnection(){}
	
	public int getCodeConnection(String link) throws Exception{
		
		try {
			url = new URL(link);
			HttpURLConnection conn = (HttpURLConnection)url.openConnection(); 
			
			return conn.getResponseCode();
			
		} catch (Exception e) {
			
//			e.printStackTrace();
			
			return -1000;
		}
	}
	
	public BufferedReader getPage() throws IOException{
		
		BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
		
		return in;
	}
	
	public InputStream getPageOpenStream() throws IOException{
		
		return url.openStream();
	}

	/**
	 * Save page in file format
	 * 
	 * @param from
	 * @param to
	 * @throws IOException
	 */
	public void webPageSaver(InputStream from, OutputStream to) throws IOException {
		byte[] buffer = new byte[4096];
		while (true) {
			int numBytes = from.read(buffer);
			if (numBytes == -1) {
				break;
			}
			to.write(buffer, 0, numBytes);
		}
	}
}
