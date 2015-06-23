package mitll.langtest.server.audio;

import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.io.*;

import mitll.langtest.server.scoring.ASRWebserviceScoring;

import org.apache.log4j.Logger;

// closes and reopens the connection after every call to sendAndReceive()
public class HTTPClient {
	private HttpURLConnection httpConn;
	private BufferedWriter sender;
	private BufferedReader receiver;
	@SuppressWarnings("unused")
	private String webserviceIP;
	@SuppressWarnings("unused")
	private int webservicePort;
	private String url;
	private static final Logger logger = Logger.getLogger(HTTPClient.class);

	/* Constructor */

  /**
   * @see mitll.langtest.server.scoring.ASRWebserviceScoring#runHydra
   * @param webserviceIP
   * @param webservicePort
   */
	public HTTPClient(String webserviceIP, int webservicePort) {
		this.webserviceIP = webserviceIP;
		this.webservicePort = webservicePort;
		this.url = "http://" + webserviceIP + ":" + webservicePort + "/dcodr";
		try {
			httpConn = setupHttpConn(url);
		}
		catch(IOException e) {
			logger.error("Error constructing HTTPClient:\n" + e,e);
		}
	}

	private HttpURLConnection setupHttpConn(String url) throws IOException {
		HttpURLConnection httpConn = (HttpURLConnection)(new URL(url)).openConnection();
		httpConn.setRequestMethod("POST");
		httpConn.setDoOutput(true);
		httpConn.setConnectTimeout(5000);
		httpConn.setReadTimeout(20000);
		//httpConn.setRequestProperty("Content-Type", "application/json; charset=utf-8");  
		httpConn.setRequestProperty("Content-Type", "text/plain; charset=utf-8");  
		httpConn.setRequestProperty("Accept-Charset", "UTF8");
		httpConn.setRequestProperty("charset", "UTF8");
		return httpConn;
	}

	public String readFromGET(String url) {
		try {
			return receive(setupGetHttpConn(url));
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
	}

	private HttpURLConnection setupGetHttpConn(String url) throws IOException {
		HttpURLConnection httpConn = (HttpURLConnection)(new URL(url)).openConnection();
		httpConn.setRequestMethod("GET");
		httpConn.setConnectTimeout(1000);
		//httpConn.setReadTimeout(20000);
		//httpConn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
		httpConn.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
		httpConn.setRequestProperty("Accept-Charset", "UTF8");
		httpConn.setRequestProperty("charset", "UTF8");
		return httpConn;
	}

	public String read(HttpURLConnection conn) {
		return receive(conn);
	}

	//private void resetConn() throws IOException {
	//	setupHttpConn(url);
	//}

	public void closeConn() throws IOException {
		httpConn.disconnect();
		httpConn = null;
	}

	private void send(String input) throws IOException {
		logger.debug("SEND INPUT: " + input);
		CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
		encoder.onMalformedInput(CodingErrorAction.REPORT);
		encoder.onUnmappableCharacter(CodingErrorAction.REPORT);
		sender = new BufferedWriter(new OutputStreamWriter(httpConn.getOutputStream(), encoder));
		sender.write(input);
		sender.flush();    
		sender.close();
		sender = null;
	}

	private String receive() throws IOException {
		HttpURLConnection httpConn = this.httpConn;
		receiver = getReader(httpConn);
		return receive(httpConn, receiver);
	}

	private String receive(HttpURLConnection httpConn) {
		try {
			return receive(httpConn, getReader(httpConn));
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
	}

	private String receive(HttpURLConnection httpConn, BufferedReader reader) throws IOException {
		int code = httpConn.getResponseCode();
		if (code == 200) {
			String output = "";
			String current = "";
			while ((current = reader.readLine()) != null) {
				output += current + "\n";
			}
			reader.close();
			receiver = null;
			return output;
		} else {
			logger.error("Received HTTP Code of: " + code + ". Expecting code 200.");
			return "";
		}
	}

	private BufferedReader getReader(HttpURLConnection httpConn) throws IOException {
		return new BufferedReader(new InputStreamReader(httpConn.getInputStream(), "UTF8"));
	}

	public String sendAndReceive(String input) {
		try {
			send(input);
			String response = receive();
			return response;
		}
		catch(IOException e) {
			e.printStackTrace();
			return "";
		}
	}
}
