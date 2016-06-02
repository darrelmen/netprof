/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.server.audio;

import mitll.langtest.shared.amas.QAPair;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.Collection;

//

/**
 * closes and reopens the connection after every call to sendAndReceive()
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since
 */
public class HTTPClient {
  private static final Logger logger = Logger.getLogger(HTTPClient.class);

  private HttpURLConnection httpConn;

  public HTTPClient() {}

  /**
   * @see mitll.langtest.server.autocrt.MiraClassifier#getMiraScore(int, String, String, String, Collection, String, QAPair)
   * @param url
   */
  public HTTPClient(String url) {
    try {
      httpConn = setupPostHttpConn(url);
    } catch (IOException e) {
      logger.error("Error constructing HTTPClient:\n" + e, e);
    }
  }

  /**
   * @see mitll.langtest.server.autocrt.MiraClassifier#getMiraScore(int, String, String, String, Collection, String, QAPair)
   * @param webserviceIP
   */
  public HTTPClient(String webserviceIP, boolean secure) {
    this("http" + (secure ? "s" : "") + "://" + webserviceIP);
  }

  /**
   * @param webserviceIP
   * @param webservicePort
   * @see mitll.langtest.server.scoring.ASRWebserviceScoring#runHydra
   */
  public HTTPClient(String webserviceIP, int webservicePort, String service) {
    this("http://" + webserviceIP + ":" + webservicePort + "/" + service);
  }

  /**
   * @param url
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#userExists(HttpServletRequest, String, String)
   */
  public String readFromGET(String url) {
    try {
      logger.info("Reading from " + url);

      return receive(setupGetHttpConn(url));
    } catch (IOException e) {
      e.printStackTrace();
      return "";
    }
  }

  private HttpURLConnection setupPostHttpConn(String url) throws IOException {
    HttpURLConnection httpConn = (HttpURLConnection) (new URL(url)).openConnection();
    httpConn.setRequestMethod("POST");
    httpConn.setDoOutput(true);
    httpConn.setConnectTimeout(5000);
    httpConn.setReadTimeout(20000);
    //httpConn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
    setRequestProperties(httpConn);
    return httpConn;
  }

  private HttpURLConnection setupGetHttpConn(String url) throws IOException {
    HttpURLConnection httpConn = (HttpURLConnection) (new URL(url)).openConnection();
    httpConn.setRequestMethod("GET");
    httpConn.setConnectTimeout(1000);
    //httpConn.setReadTimeout(20000);
    //httpConn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
    setRequestProperties(httpConn);
    return httpConn;
  }

  private void setRequestProperties(HttpURLConnection httpConn) {
    httpConn.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
    httpConn.setRequestProperty("Accept-Charset", "UTF8");
    httpConn.setRequestProperty("charset", "UTF8");
  }

/*
  private String read(HttpURLConnection conn) {
		return receive(conn);
	}
*/

  private void closeConn() {
    httpConn.disconnect();
    //httpConn = null;
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

  public String receive() throws IOException {
    return receive(this.httpConn, getReader(this.httpConn));
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
      StringBuilder builder = new StringBuilder();
      String current;
      while ((current = reader.readLine()) != null) {
        builder.append(current);
        builder.append("\n");
      }
      reader.close();
      return builder.toString();
    } else {
      logger.error("Reading " + httpConn.getURL() + " received HTTP Code of: " + code + ".");
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