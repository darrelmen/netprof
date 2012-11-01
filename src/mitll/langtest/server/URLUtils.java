package mitll.langtest.server;

import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 10/12/12
 * Time: 4:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class URLUtils {
  private static Logger logger = Logger.getLogger(URLUtils.class);
  private HttpServletRequest request;
  public URLUtils(HttpServletRequest request) {
    this.request = request;
  }
  public String makeURL(String path) {
   // HttpServletRequest request = getThreadLocalRequest();
    if (isDevMode(request)) {
      // debug mode, skip it
      //System.out.println("debug mode detected : req url " + request.getRequestURL());

      return path;
    }
    else {
      // System.out.println("making full url, req url " + request.getRequestURL());
    }
    String encoded = encodeURL(constructURL(request, path));
    // System.out.println("rel path " + path + " encoded as " + encoded);
    return encoded;
  }

  private boolean isDevMode() {
    return isDevMode(request);
  }

  private boolean isDevMode(HttpServletRequest request) {
    return request.getRequestURL().toString().contains("127.0.0.1:8888");
  }

  /**
   * constructs a URL string within the Context URL of this
   * servlet request.  Basically, the specified path is used
   * to replace the context path of the specified request object's
   * URL.  If the specified path does not begin with a "/" (i.e., is
   * absolute) then that will be asserted.
   *
   * @param request the request object
   * @param path    absolute path within the context of the request URL
   * @return a URL.
   */
  private URL constructURL(HttpServletRequest request, String path) {
    path = path == null ? "/" : path;
    URL url;
    try {
      if (!path.startsWith("/")) {
        path = "/" + path;
      }
      url = request == null ? new URL("file://") : new URL(request.getRequestURL().toString());
      String root = getRootFromURL(url);
      url = new URL(url, root + path);
    } catch (MalformedURLException e) {
      String msg = "Error constructing new url for request=" + request + " & path=" + path;
      // logger.log(Level.ERROR, msg, e);
      throw new RuntimeException(e);
    }
    return url;
  }

  private String getRootURL() {
    //return getRootFromRequest(getThreadLocalRequest());
    return constructURL(request,"").toString();
  }

  private String getRootFromRequest(HttpServletRequest request) {
    try {
      return getRootFromURL(new URL(request.getRequestURL().toString()));
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    return null;
  }

  private String getRootFromURL(URL url) {
    String root = "";
    String sa[] = url.getPath().split("/");
    for (int i = 0; root.equals("") && i < sa.length; i++) {
      if (!sa[i].isEmpty()) {
        root = "/" + sa[i];
      }
    }
    return root;
  }

  private String encodeURL(URL url) {
    try {
      URI uri = new URI(url.getProtocol(),null,url.getHost(),url.getPort(),url.getPath(),url.getQuery(),null);
      String s = uri.toString().replaceAll("&", "&amp;");
      return s;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "";
  }

  public String convertURLToRelativeFile(String audioFile) {
    if (!isDevMode()) {
      String rootURL = getRootURL();
      if (!audioFile.startsWith(rootURL)) {
        logger.error("getWavForMP3 :huh? expecting " +audioFile + " to start with " + rootURL);
      }

      String relPath = audioFile.substring(rootURL.length());
      logger.info("converted URL " +audioFile + " to rel file " + relPath);
      audioFile = relPath;
    }
    return audioFile;
  }
}
