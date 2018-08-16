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

import mitll.langtest.server.scoring.ASRWebserviceScoring;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.util.List;

/**
 * closes and reopens the connection after every call to sendAndReceive()
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 */
public class HTTPClient {
  private static final Logger logger = LogManager.getLogger(HTTPClient.class);
  private static final int CONNECT_TIMEOUT = 5000;
  private static final int READ_TIMEOUT = 20000;
  private static final String GET = "GET";
  private static final String POST = "POST";

  private HttpURLConnection httpConn;

  public HTTPClient() {
  }

  /**
   * @param webserviceIP
   * @see mitll.langtest.server.autocrt.MiraClassifier#getMiraScore
   */
  public HTTPClient(String webserviceIP, boolean secure) {
    this("http" + (secure ? "s" : "") + "://" + webserviceIP);
  }

  /**
   * @param webserviceIP
   * @param webservicePort
   * @see ASRWebserviceScoring#getDcodr
   */
  public HTTPClient(String webserviceIP, int webservicePort, String service) {
    this("http://" + webserviceIP + ":" + webservicePort + "/" + service);
  }

  /**
   * @param url
   * @see mitll.langtest.server.autocrt.MiraClassifier#getMiraScore
   * @see AudioFileHelper#checkForWebservice
   */
  public HTTPClient(String url) {
    try {
      // logger.info("HTTPClient URL is : " + url);
      httpConn = setupPostHttpConn(url);
    } catch (IOException e) {
      logger.error("Error constructing HTTPClient:\n" + e, e);
    }
  }

  /**
   * @param input
   * @return
   * @throws IOException
   * @see ASRWebserviceScoring#runHydra
   */
  public String sendAndReceiveAndClose(String input) throws IOException {
    String s = sendAndReceive(input);
    closeConn();
    return s;
  }

  void addRequestProperty(String k, String v) {
    httpConn.addRequestProperty(k, v);
  }

  /**
   * @param webserviceIP
   * @param webservicePort
   * @param service
   * @return
   */
  public boolean isAvailable(String webserviceIP, int webservicePort, String service) {
    try {
      readFromGET("http://" + webserviceIP + ":" + webservicePort + "/" + service + "/index.html");
      return true;
    } catch (FileNotFoundException fnf) {
      logger.debug("isAvailable for " + webserviceIP + " " + webservicePort + " " + service + " :" + fnf);
      return true;
    } catch (IOException e) {
      logger.warn("isAvailable : Got " + e);
      return false;
    }
  }

  /**
   * @param url
   * @return
   * @see #isAvailable
   */
  private String readFromGET(String url) throws IOException {
    HttpURLConnection httpConn = setupGetHttpConn(url);
    String receive = receive(httpConn);
    httpConn.disconnect();
    return receive;
  }

  private HttpURLConnection setupGetHttpConn(String url) throws IOException {
    HttpURLConnection httpConn = getHttpURLConnection(url);
    httpConn.setRequestMethod(GET);
    httpConn.setConnectTimeout(CONNECT_TIMEOUT);
    httpConn.setReadTimeout(READ_TIMEOUT);
    setRequestProperties(httpConn);
    return httpConn;
  }

  private HttpURLConnection setupPostHttpConn(String url) throws IOException {
    HttpURLConnection httpConn = getHttpURLConnection(url);
    httpConn.setRequestMethod(POST);
    httpConn.setDoOutput(true);
    httpConn.setConnectTimeout(CONNECT_TIMEOUT);
    httpConn.setReadTimeout(READ_TIMEOUT);
    setRequestProperties(httpConn);
    return httpConn;
  }

  private HttpURLConnection getHttpURLConnection(String url) throws IOException {
    return (HttpURLConnection) (new URL(url)).openConnection();
  }

  private void setRequestProperties(HttpURLConnection httpConn) {
    httpConn.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
    httpConn.setRequestProperty("Accept-Charset", "UTF8");
    httpConn.setRequestProperty("charset", "UTF8");
  }

  private void closeConn() {
    httpConn.disconnect();
  }

  private void send(String input) throws IOException {
    //logger.debug("SEND INPUT: " + input);
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
    return receive(this.httpConn, getReader(this.httpConn));
  }

  private String receiveCookie() throws IOException {
    String s = receiveCookie(this.httpConn);
    this.httpConn.getInputStream().close();
    return s;
  }

  private String receive(HttpURLConnection httpConn) throws IOException {
    return receive(httpConn, getReader(httpConn));
  }

  private BufferedReader getReader(HttpURLConnection httpConn) throws IOException {
    InputStream inputStream = httpConn.getInputStream();
    return new BufferedReader(new InputStreamReader(inputStream, "UTF8"));
  }

  private String receive(HttpURLConnection httpConn, BufferedReader reader) throws IOException {
    int code = httpConn.getResponseCode();
//
//    List<String> strings = httpConn.getHeaderFields().get("Set-Cookie");
//
//    if (strings == null) {
//      logger.info("no header fields for cookie, got " + httpConn.getHeaderFields());
//    } else {
//      for (String s : strings) logger.info("cookie receive " + s);
//    }

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

  private String receiveCookie(HttpURLConnection httpConn) throws IOException {
    List<String> strings = httpConn.getHeaderFields().get("Set-Cookie");
    if (strings == null) {
      logger.info("no header fields for cookie, got " + httpConn.getHeaderFields());
      return null;
    } else {
      //for (String s : strings) logger.info("cookie receive " + s);
      return (strings.isEmpty() ? null : strings.iterator().next());
    }
  }

  /**
   * @param input
   * @return
   * @throws IOException
   */
  public String sendAndReceive(String input) throws IOException {
    try {
      //logger.info("sending START " + input.length());
      send(input);
      //logger.info("sending END   " + input.length());
    } catch (ConnectException ce) {
      String sending = "sending";
      logError(input, ce, sending);
      return "";
    } catch (IOException e) {
      logger.error("sendAndReceive sending " + input + " got " + e, e);
      throw e;
    }

    try {
      //logger.info("receive START " + input.length());
      String receive = receive();
      //logger.info("receive END   " + input.length());
      return receive;
    } catch (ConnectException ce) {
      logError(input, ce, "receiving");
      return "";
    } catch (IOException e) {
      logger.error("sendAndReceive receiving from " + input + " got " + e, e);
      throw e;
    }
  }

  public String sendAndReceiveCookie(String input) throws IOException {
    try {
      //logger.info("sending START " + input.length());
      send(input);
      //logger.info("sending END   " + input.length());
    } catch (ConnectException ce) {
      String sending = "sending";
      logError(input, ce, sending);
      return "";
    } catch (IOException e) {
      logger.error("sendAndReceive sending " + input + " got " + e, e);
      throw e;
    }

    try {
      //logger.info("receive START " + input.length());
      String receive = receiveCookie();
      //logger.info("receive END   " + input.length());
      return receive;
    } catch (ConnectException ce) {
      logError(input, ce, "receiving");
      return "";
    } catch (IOException e) {
      logger.error("sendAndReceive receiving from " + input + " got " + e, e);
      throw e;
    }
  }


  private void logError(String input, ConnectException ce, String sending) {
    logger.error("sendAndReceive " + sending +
        "\n\tmessage " + input +
        "\n\tcouldn't connect to server " + httpConn.getURL() +
        "\n\tgot     " + ce);
  }

  /**
   * @param input
   * @return
   * @throws IOException
   * @see AudioFileHelper#getProxyScore
   */
  String sendAndReceiveAndClose(File input) throws IOException {
    try {
      return postAndClose(input);
    } catch (ConnectException ce) {
      logger.error("sendAndReceiveAndClose sending " + input + " couldn't connect to server at  " + httpConn.getURL() + " got " + ce);
      return "";
    } catch (IOException e) {
      logger.error("sendAndReceiveAndClose sending " + input + " got " + e, e);
      throw e;
    }
  }

  @NotNull
  private String postAndClose(File input) throws IOException {
    postFile(input);
    String receive = receive();
    closeConn();
    return receive;
  }

  private void postFile(File theFile) {
    try {
      OutputStream outputStream = httpConn.getOutputStream();
      Files.copy(theFile.toPath(), outputStream);

      outputStream.flush();
      outputStream.close();
    } catch (IOException e) {
      logger.error("Got " + e, e);
    }
  }
}