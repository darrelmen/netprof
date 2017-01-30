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

import mitll.langtest.server.ServerProperties;
import mitll.langtest.shared.amas.QAPair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.*;
import java.io.*;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;

//

/**
 * closes and reopens the connection after every call to sendAndReceive()
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 */
public class HTTPClient {
  private static final Logger logger = LogManager.getLogger(HTTPClient.class);

  private HttpURLConnection httpConn;

  static {
    //for localhost testing only
    javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
        new javax.net.ssl.HostnameVerifier(){

          public boolean verify(String hostname,
                                javax.net.ssl.SSLSession sslSession) {
              System.err.println("verify " +hostname);
              return true;

          }
        });
  }

  @Deprecated
  public HTTPClient() {
  }



  /**
   * @param webserviceIP
   * @see mitll.langtest.server.autocrt.MiraClassifier#getMiraScore(int, String, String, String, Collection, String, QAPair)
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
   * @see mitll.langtest.server.autocrt.MiraClassifier#getMiraScore(int, String, String, String, Collection, String, QAPair)
   */
  public HTTPClient(String url) {
    try {
    //  logger.info("URL is : " + url);
      httpConn = setupPostHttpConn(url);
    } catch (IOException e) {
      logger.error("Error constructing HTTPClient:\n" + e, e);
    }
  }

  public void postFile(File theFile) {
    try {
      OutputStream outputStream = httpConn.getOutputStream();
      Files.copy(theFile.toPath(), outputStream);

      outputStream.flush();
      outputStream.close();
    } catch (IOException e) {
      logger.error("Got " + e, e);
    }
  }

  public void addRequestProperty(String k, String v) {
    httpConn.addRequestProperty(k, v);
  }

  public boolean isAvailable() {
    int responseCode = -1;
    try {
      responseCode = httpConn.getResponseCode();
      return responseCode == 200;
    } catch (IOException e) {
      logger.warn("Got " + e + " with code " + responseCode);
      return false;
    }
  }

  /**
   * @param url
   * @return
   * @see mitll.langtest.server.database.exercise.DominoReader#readProjectInfo(ServerProperties)
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
    HttpURLConnection httpConn = getHttpURLConnection(url);
    httpConn.setRequestMethod("POST");
    httpConn.setDoOutput(true);
    httpConn.setConnectTimeout(5000);
    httpConn.setReadTimeout(20000);
    //httpConn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
    setRequestProperties(httpConn);
    return httpConn;
  }

  private HttpURLConnection setupGetHttpConn(String url) throws IOException {
    HttpURLConnection httpConn = getHttpURLConnection(url);
    httpConn.setRequestMethod("GET");
    httpConn.setConnectTimeout(1000);
    //httpConn.setReadTimeout(20000);
    //httpConn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
    setRequestProperties(httpConn);
    return httpConn;
  }

  private HttpURLConnection getHttpURLConnection(String url) throws IOException {
    HttpURLConnection httpURLConnection = (HttpURLConnection) (new URL(url)).openConnection();

    try {
      SSLContext ctx = SSLContext.getInstance("TLS");
      ctx.init(new KeyManager[0], new TrustManager[] {new DefaultTrustManager()}, new SecureRandom());
      SSLContext.setDefault(ctx);
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (KeyManagementException e) {
      e.printStackTrace();
    }

//
//    httpURLConnection.seth(new HostnameVerifier() {
//      @Override
//      public boolean verify(String arg0, SSLSession arg1) {
//        return true;
//      }
//    });
    return httpURLConnection;
  }

  private static class DefaultTrustManager implements X509TrustManager {

    @Override
    public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}

    @Override
    public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}

    @Override
    public X509Certificate[] getAcceptedIssuers() {
      return null;
    }
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

  private String receive(HttpURLConnection httpConn) {
    try {
      return receive(httpConn, getReader(httpConn));
    } catch (IOException e) {
      logger.error("Got " + e, e);
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

  public String sendAndReceiveAndClose(File input) throws IOException {
    try {
      postFile(input);
      String receive = receive();
      closeConn();
      return receive;
    } catch (ConnectException ce) {
      logger.error("sending " + input + " couldn't connect to server at  " + httpConn.getURL() + " got " + ce);
      return "";
    } catch (IOException e) {
      logger.error("sending " + input + " got " + e, e);
      throw e;
    }
  }
}