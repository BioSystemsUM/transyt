package pt.uminho.ceb.biosystems.transyt.utilities.connection;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;

public class SendPost {
	
	private static final String URL = "http://rosalind.di.uminho.pt:8085";
	
	public SendPost() {
		
		
	}
	
	public void postFiles() throws IOException {
	
	String uploadUrl = URL.concat("/upload");
	
	String charset = "UTF-8";
	String param = "value";
	File textFile = new File("C:\\Users\\Davide\\Desktop\\coisa2.txt");
	File textFile2 = new File("C:\\Users\\Davide\\Desktop\\tcdb2.faa");
//	File binaryFile = new File("C:\\Users\\Davide\\Desktop\\roff2018.pdf");
	String boundary = Long.toHexString(System.currentTimeMillis()); // Just generate some unique random value.
	String CRLF = "\r\n"; // Line separator required by multipart/form-data.

	URLConnection connection = new URL(uploadUrl).openConnection();
	connection.setDoOutput(true);
	connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

	try (
	    OutputStream output = connection.getOutputStream();
	    PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, charset), true);
	) {
	    // Send normal param.
	    writer.append("--" + boundary).append(CRLF);
	    writer.append("Content-Disposition: form-data; name=\"param\"").append(CRLF);
	    writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
	    writer.append(CRLF).append(param).append(CRLF).flush();

	    // Send text file.
	    writer.append("--" + boundary).append(CRLF);
	    writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + textFile.getName() + "\"").append(CRLF);
	    writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF); // Text file itself must be saved in this charset!
	    writer.append(CRLF).flush();
	    Files.copy(textFile.toPath(), output);
	    output.flush(); // Important before continuing with writer!
	    writer.append(CRLF).flush(); // CRLF is important! It indicates end of boundary.
	    
	    // Send text file.
	    writer.append("--" + boundary).append(CRLF);
	    writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + textFile2.getName() + "\"").append(CRLF);
	    writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF); // Text file itself must be saved in this charset!
	    writer.append(CRLF).flush();
	    Files.copy(textFile2.toPath(), output);
	    output.flush(); // Important before continuing with writer!
	    writer.append(CRLF).flush(); // CRLF is important! It indicates end of boundary.


	    // End of multipart/form-data.
	    writer.append("--" + boundary + "--").append(CRLF).flush();
	}

	// Request is lazily fired whenever you need to obtain information about response.
	int responseCode = ((HttpURLConnection) connection).getResponseCode();
	System.out.println(responseCode); // Should be 200
	}
	
	public void downloadFile() throws IOException {
		
		//implement response code reader
		
		URL downloadUrl = new URL(URL.concat("/download"));
		
		File f = new File("C:\\\\Users\\\\Davide\\\\Desktop\\\\abc.pdf");
		
		FileUtils.copyURLToFile(downloadUrl, f);
		
		
	}

}
