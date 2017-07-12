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

package mitll.langtest.client.initial;

import com.google.gwt.user.client.Window;
import mitll.langtest.client.LangTest;

import java.util.HashMap;
import java.util.Map;
//import java.util.logging.Logger;

/**
 * <br/>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 2/23/16
 */
public class BrowserCheck {
//  private final Logger logger = Logger.getLogger("BrowserCheck");

  private static final String FIREFOX = "firefox";
  private static final String CHROME = "chrome";
  private static final String MSIE = "msie";
  private static final String SAFARI = "safari";
  private static final String IE = "IE";

  private String browser = "Unknown";
  int ver = 0;
  private String version = "";

  private final Map<String, Integer> browserToVersion = new HashMap<>();

  public BrowserCheck() {
    browserToVersion.put(FIREFOX, 14);
    browserToVersion.put(CHROME, 21);
    browserToVersion.put(IE, 9);
    browserToVersion.put(SAFARI, 5);
  }

  /**
   * @return
   * @see LangTest#onModuleLoad2()
   */
  public BrowserCheck checkForCompatibleBrowser() {
    if (browser.equals("Unknown")) getBrowserAndVersion();
    Integer min = browserToVersion.get(browser);
    if (browser.toLowerCase().contains("ie")) { // just skip IE
      //Window.alert("Your browser is " + browser + ".<br></br>We recommend using either Firefox, Safari, or Chrome.");
      return this;
    }
    int ieVersion = getIEVersion();
    if (ieVersion < 9 && ieVersion > 0) {
      Window.alert("Your browser is IE version " + version +
          ". We strongly recommend upgrading to version " + min + " or later.");
    }
    if (min == null) {
      // Window.alert("Your browser is " + browser + " version " + version + ". We strongly recommend any of " + browserToVersion.keySet());
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

  public boolean isIE7() {
    return getBrowserAndVersion().equals("IE 7");
  }

/*
  public boolean isIE() {
    return browser.equals("IE");
  }
*/

  public static boolean isIPad() {
    String userAgent = getUserAgent();
    boolean b = userAgent.contains("ipad") || userAgent.contains("iphone") || userAgent.contains("ipod");
    //if (b) logger.info("found iPad " + userAgent);
    return b;
  }

  /**
   * @return
   * @see LangTest#getBrowserInfo()
   */
  public String getBrowserAndVersion() {
    return getBrowser(getUserAgent());
  }

  private String getBrowser(String agent) {
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
        version = version.substring(0, 1);
      }
      browser = SAFARI;
    } else {
      browser = getAppName();
    }

    String major = version.split("\\.")[0];
    try {
      ver = Integer.parseInt(major);
    } catch (NumberFormatException e) {
      //   logger.warning("couldn't parse '" + agent + "' and '" + major + "'");
      //e.printStackTrace();
    }
    return browser + " " + ver;
  }

  /**
   * Gets the navigator.appName.
   *
   * @return the window's navigator.appName.
   */
  private static native String getAppName() /*-{
      return $wnd.navigator.appName;
  }-*/;

  private static native String getUserAgent() /*-{
      return navigator.userAgent.toLowerCase();
  }-*/;

  public static native int getIEVersion() /*-{
      var ua = window.navigator.userAgent;
      var msie = ua.indexOf('MSIE ');
      var trident = ua.indexOf('Trident/');

      if (msie > 0) {
          // IE 10 or older => return version number
          return parseInt(ua.substring(msie + 5, ua.indexOf('.', msie)), 10);
      }

      if (trident > 0) {
          // IE 11 (or newer) => return version number
          var rv = ua.indexOf('rv:');
          return parseInt(ua.substring(rv + 3, ua.indexOf('.', rv)), 10);
      }

      // other browser
      return -1;
  }-*/;
}