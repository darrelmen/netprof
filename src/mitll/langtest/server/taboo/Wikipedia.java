package mitll.langtest.server.taboo;

import mitll.langtest.server.URLUtils;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 8/12/13
 * Time: 1:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class Wikipedia {
  private static Logger logger = Logger.getLogger(Wikipedia.class);

  private static final String LLPROXY = "155.34.234.20";
  private static final int SEARCH_LIMIT = 10;

  public static void main(String [] arg) {
    String queryString = "it's fantastic";
/*    String urlold = "https://en.wikipedia.org/wiki/Special:ApiSandbox#action=query&list=search&format=json&srsearch=" +
      "\"" +
      queryString +
      "\"" +
      "&srwhat=text&srlimit=" +
      SEARCH_LIMIT;*/

    try {
      FileOutputStream resourceAsStream = new FileOutputStream("englishVocabSnippets2.txt");
      BufferedWriter utf8 = new BufferedWriter(new OutputStreamWriter(resourceAsStream, "UTF8"));

      recordSearchResults(queryString, utf8);
      utf8.close();
/*
      logger.debug("got titles " + titles);
      logger.debug("got snippets " + snippets);*/
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void recordSearchResults(String queryString, BufferedWriter utf8) throws Exception {
    URL url2 = getUrl(queryString);

    //String baseURL = new URLUtils().encodeURL(url2);
    //  logger.debug("after  " + baseURL);
    //    if (true) return;
    //URL oracle = new URL(url2);
    // URLConnection yc = url2.openConnection();

    URLConnection conn = getConnection(url2);
    StringBuilder builder = readFromURL(conn);

    readJson(queryString, utf8, builder);
  }

  private static void readJson(String queryString, BufferedWriter utf8, StringBuilder builder) throws IOException {
    JSONObject obj = JSONObject.fromObject(builder.toString());

    JSONObject query = obj.getJSONObject("query");
    Object search = query.get("search");


    Collection<JSONObject> result = JSONArray.toCollection((JSONArray) search, JSONObject.class);
    //   List<String> titles = new ArrayList<String>();
    //     List<String> snippets = new ArrayList<String>();
    for (JSONObject o : result) {
      String title = o.getString("title");
     // titles.add(title);
      String snippet = o.getString("snippet");
      //snippets.add(snippet);
      utf8.write(queryString);
      utf8.write("\t");
      utf8.write(title);
      utf8.write("\t");
      utf8.write(snippet);
      utf8.write("\n");
    }
  }

  private static URL getUrl(String queryString) throws Exception {
    String url = "http://en.wikipedia.org/w/api.php?action=query&list=search&format=json&srsearch=" +
      "\"" +
      queryString +
      "\"" +
      "&srwhat=text&srlimit=" +
      SEARCH_LIMIT;

    logger.debug("before " + url);
    //   URL url1 = new URL(url);
    //  logger.debug("url1   " + url1);

    URL url2 = new URLUtils().parseUrl(url);
    logger.debug("url2   " + url2);
    return url2;
  }

  private static URLConnection getConnection(URL url2) throws IOException {
    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(LLPROXY, 8080));
    URLConnection conn =url2.openConnection(proxy);
    conn.setRequestProperty("User-Agent", "WikipediaEducationSearch/0.1 (https://np.ll.mit.edu/npfESL/; gordon.vidaver@ll.mit.edu)");
    return conn;
  }

  private static StringBuilder readFromURL(URLConnection conn) throws IOException {
    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

    StringBuilder builder = new StringBuilder();
    String line;
    while ((line = in.readLine()) != null) {
       builder.append(line);
    }
    in.close();
    return builder;
  }
}
