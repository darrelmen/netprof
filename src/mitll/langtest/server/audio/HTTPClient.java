package mitll.langtest.server.audio;

import java.net.*;
import java.io.*;

import mitll.langtest.server.scoring.ASRWebserviceScoring;

import org.apache.log4j.Logger;

// closes and reopens the connection after every call to sendAndReceive()
public class HTTPClient {

	private HttpURLConnection httpConn;
	private OutputStreamWriter sender;
	private BufferedReader receiver;
	private String webserviceIP;
	private int webservicePort;
	private String url;
	private static final Logger logger = Logger.getLogger(HTTPClient.class);

	/* Constructor */

	public HTTPClient(String webserviceIP, int webservicePort) {
		this.webserviceIP = webserviceIP;
		this.webservicePort = webservicePort;
		this.url = "http://" + webserviceIP + ":" + webservicePort + "/jcodr";
		try {
			httpConn = setupHttpConn(url);
		}
		catch(IOException e) {
			logger.error("Error constructing HTTPClient:\n" + e.getStackTrace());
		}
	}

	/* private methods */

	private HttpURLConnection setupHttpConn(String url) throws IOException {
		HttpURLConnection httpConn = (HttpURLConnection)(new URL(url)).openConnection();
		httpConn.setRequestMethod("POST");
		httpConn.setDoOutput(true);
		httpConn.setConnectTimeout(5000);
		httpConn.setReadTimeout(20000);
		httpConn.setRequestProperty("Content-Type", "application/json");    
		httpConn.setRequestProperty("Accept-Charset", "UTF-8");
		httpConn.setRequestProperty("charset", "UTF-8");
		return httpConn;
	}

	//private void resetConn() throws IOException {
	//	setupHttpConn(url);
	//}

	public void closeConn() throws IOException {
		httpConn.disconnect();
		httpConn = null;
	}

	private void send(String input) throws IOException {
		sender = new OutputStreamWriter(httpConn.getOutputStream(), "UTF-8");
		sender.write(input);
		sender.flush();    
		sender.close();
		sender = null;
	}

	private String receive() throws IOException {
		receiver = new BufferedReader(new InputStreamReader(httpConn.getInputStream(), "UTF-8"));
		int code = httpConn.getResponseCode();
		if(code == 200) {
			String output = "";
			String current = "";
			while((current = receiver.readLine()) != null) {
				output += current + "\n";
			}
			receiver.close();
			receiver = null;
			return output;
		}
		else {
			logger.error("Received HTTP Code of: " + code + ". Expecting code 200.");
			return "";
		}
	}

	/* public methods */

	public String sendAndReceive(String input) {
		try {
			send(input);
			String response = receive();
			//closeConn();
			//resetConn();
			return response;
		}
		catch(IOException e) {
			e.printStackTrace();
			return "";
		}
	}

}
