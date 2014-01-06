package mitll.langtest.client;

import com.google.gwt.user.client.Window;

import java.util.HashMap;
import java.util.Map;

public class BrowserCheck {
  private static final String FIREFOX = "firefox";
  private static final String CHROME = "chrome";
  private static final String MSIE = "msie";
  private static final String SAFARI = "safari";
  private static final String IE = "IE";

  public String browser = "Unknown";
  public int ver = 0;
  public String version = "";

  private Map<String,Integer> browserToVersion = new HashMap<String,Integer>();

  public BrowserCheck() {
    browserToVersion.put(FIREFOX,14);
    browserToVersion.put(CHROME,21);
    browserToVersion.put(IE,9);
    browserToVersion.put(SAFARI,5);
  }

  public BrowserCheck checkForCompatibleBrowser() {
    if (browser.equals("Unknown")) getBrowserAndVersion();
    Integer min = browserToVersion.get(browser);
    if (browser.toLowerCase().contains("ie")) { // just skip IE
      //Window.alert("Your browser is " + browser + ".<br></br>We recommend using either Firefox, Safari, or Chrome.");
      return this;
    }
    if (min == null) {
      Window.alert("Your browser is " + browser + " version " + version + ". We strongly recommend any of " + browserToVersion.keySet());
    } else if (ver < min) {
        if (!browser.equals(FIREFOX) || !version.startsWith("10.0")) {
            Window.alert("Your browser is " + browser + " version " + version +
                    ". We strongly recommend upgrading to version " + min + " or later.");
        }
    } else {
      //System.out.println("browser " +browser + " ver " + ver + " version " + version + " vs " + min);
    }
    return this;
  }

  public boolean isIE7() { return getBrowserAndVersion().equals("IE 7"); }
  //public boolean isIE() { return browser.equals("IE"); }

  /**
   * @see mitll.langtest.client.LangTest#getReleaseStatus()
   * @return
   */
  public String getBrowserAndVersion() {
    String agent = getUserAgent();
    return getBrowser(agent);
  }

  public String getBrowser(String agent) {
    agent = agent.toLowerCase();
    if (agent.contains(FIREFOX)) {
      version = agent.substring(agent.indexOf(FIREFOX) + FIREFOX.length() + 1).split("\\s+")[0];
      browser = FIREFOX;
    } else if (agent.contains(CHROME)) {
      version = agent.substring(agent.indexOf(CHROME) + CHROME.length() + 1).split("\\s+")[0];
      browser = CHROME;
    } else if (agent.contains(MSIE)) {
      version = agent.substring(agent.indexOf(MSIE) + MSIE.length() + 1).split(";")[0];
      browser = "IE";
    } else if (agent.contains(SAFARI)) {
      version = agent.substring(agent.indexOf(SAFARI) + SAFARI.length() + 1).split("\\s+")[0];
      if (version.length() > 1) {
        version = version.substring(0,1);
      }
      browser = SAFARI;
    }
    String major = version.split("\\.")[0];
    try {
      ver = Integer.parseInt(major);
    } catch (NumberFormatException e) {
      System.err.println("couldn't parse " + agent + " and " + major);
      e.printStackTrace();
    }

    return browser + " " + ver;
  }

 // public boolean isFirefox() { return browser.equals(FIREFOX); }

  private static native String getUserAgent() /*-{
    return navigator.userAgent.toLowerCase();
  }-*/;

}