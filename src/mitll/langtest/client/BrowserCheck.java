package mitll.langtest.client;

import com.google.gwt.user.client.Window;

import java.util.HashMap;
import java.util.Map;

public class BrowserCheck {
 // private final LangTest langTest;
  public String browser = "Unknown";
  public int ver = 0;
  public String version = "";

  private Map<String,Integer> browserToVersion = new HashMap<String,Integer>();

  public BrowserCheck() {
    browserToVersion.put("firefox",14);
    browserToVersion.put("chrome",21);
    browserToVersion.put("IE",9);
    browserToVersion.put("safari",5);
  }

  public void checkForCompatibleBrowser() {
    Integer min = browserToVersion.get(browser);
    if (browser.equals("IE")) {
      //Window.alert("Your browser is " + browser + ".<br></br>We recommend using either Firefox, Safari, or Chrome.");
    }
    if (min == null) {
      Window.alert("Your browser is " + browser + " version " + version + ". We strongly recommend any of " + browserToVersion.keySet());
    } else if (ver < min) {
      Window.alert("Your browser is " +browser + " version " + version+
          ". We strongly recommend upgrading to version " + min + " or later.");
    } else {
      System.out.println("browser " +browser + " ver " + ver + " version " + version + " vs " + min);
    }
  }

  public void getBrowserAndVersion() {
    String agent = getUserAgent();
    if (agent.contains("firefox")) {
      version = agent.substring(agent.indexOf("firefox") + "firefox".length() + 1).split("\\s+")[0];
      browser = "firefox";
    } else if (agent.contains("chrome")) {
      version = agent.substring(agent.indexOf("chrome") + "chrome".length() + 1).split("\\s+")[0];
      browser = "chrome";
    } else if (agent.contains("msie")) {
      version = agent.substring(agent.indexOf("msie") + "msie".length() + 1).split(";")[0];
      browser = "IE";
    } else if (agent.contains("safari")) {
      version = agent.substring(agent.indexOf("safari") + "safari".length() + 1).split("\\s+")[0];
      if (version.length() > 1) {
        version = version.substring(0,1);
      }
      browser = "safari";
    }
    String major = version.split("\\.")[0];
    try {
      ver = Integer.parseInt(major);
    } catch (NumberFormatException e) {
      System.err.println("couldn't parse " + agent + " and " + major);
      e.printStackTrace();
    }
  }

  private static native String getUserAgent() /*-{
    return navigator.userAgent.toLowerCase();
  }-*/;

}