package mitll.langtest.server.audio;

import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;

// closes and reopens the connection after every call to sendAndReceive()
public class HTTPClient {
	private static final Logger logger = Logger.getLogger(HTTPClient.class);

	private HttpURLConnection httpConn;
	private BufferedReader receiver;

	/* Constructor */
  public HTTPClient() {}

/*	public HTTPClient(String url) {
		logger.debug("connect to " + url);
		try {
			httpConn = setupHttpConn(url);
		} catch (IOException e) {
			logger.error("Error constructing HTTPClient:\n" + e, e);
		}
	}*/

	/**
	 * @see mitll.langtest.server.database.UserManagement#userExists(HttpServletRequest, String, String)
	 * @param webserviceIP
	 * @param webservicePort
	 */
	public HTTPClient(String webserviceIP, int webservicePort) {
		String url = "http://" + webserviceIP + ":" + webservicePort;

		logger.debug("connect to " + url);
		try {
			httpConn = setupHttpConn(url);
		} catch (IOException e) {
			logger.error("Error constructing HTTPClient:\n" + e, e);
		}
	}

	/**
	 * @param webserviceIP
	 * @param webservicePort
	 * @see mitll.langtest.server.scoring.ASRWebserviceScoring#runHydra
	 */
	public HTTPClient(String webserviceIP, int webservicePort, String service) {
		String url = "http://" + webserviceIP + ":" + webservicePort + "/" + service;

		//  logger.debug("connect to " + url);
		try {
			httpConn = setupHttpConn(url);
		} catch (IOException e) {
			logger.error("Error constructing HTTPClient:\n" + e, e);
		}
	}

	/**
	 * @param url
	 * @return
	 * @see mitll.langtest.server.database.DatabaseImpl#userExists(HttpServletRequest, String, String)
	 */
	public String readFromGET(String url) {
		try {
			return receive(setupGetHttpConn(url));
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
	}

	private HttpURLConnection setupHttpConn(String url) throws IOException {
		HttpURLConnection httpConn = (HttpURLConnection) (new URL(url)).openConnection();
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

	private HttpURLConnection setupGetHttpConn(String url) throws IOException {
		HttpURLConnection httpConn = (HttpURLConnection) (new URL(url)).openConnection();
		httpConn.setRequestMethod("GET");
		httpConn.setConnectTimeout(1000);
		//httpConn.setReadTimeout(20000);
		//httpConn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
		httpConn.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
		httpConn.setRequestProperty("Accept-Charset", "UTF8");
		httpConn.setRequestProperty("charset", "UTF8");
		return httpConn;
	}

/*
	private String read(HttpURLConnection conn) {
		return receive(conn);
	}
*/

	private void closeConn() {
		httpConn.disconnect();
		httpConn = null;
	}

	private void send(String input) throws IOException {
		logger.debug("SEND INPUT: " + input);

		OutputStream outputStream = httpConn.getOutputStream();

		CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
		encoder.onMalformedInput(CodingErrorAction.REPORT);
		encoder.onUnmappableCharacter(CodingErrorAction.REPORT);
		BufferedWriter sender = new BufferedWriter(new OutputStreamWriter(outputStream, encoder));
		sender.write(input);
		sender.flush();
		sender.close();
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

	private String sendAndReceive(String input) throws IOException {
		try {
			send(input);
			return receive();
		} catch (ConnectException ce) {
			logger.error("sending " + input + " couldn't connect to server at  " + httpConn.getURL() + " got " + ce);
			return "";
		} catch (IOException e) {
			logger.error("sending " + input + " got " + e, e);
			throw e;
		}
	}

	public String sendAndReceiveAndClose(String input) throws IOException {
		String s = sendAndReceive(input);
		closeConn();
		return s;
	}
}