/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.client.custom;

import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Window;
import mitll.langtest.client.initial.InitialUI;
import mitll.langtest.client.initial.PropertyHandler;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class KeyStorage {
  public static final String TRUE = "true";
  private final Logger logger = Logger.getLogger("KeyStorage");

  private int user;
  private boolean showedAlert = false;

  private static final String USER_ID = "userID";
  private static final String USER_CHOSEN_ID = "userChosenID";
  private static final String USER_PENDING_ID = "userPendingID";

  private final boolean DEBUG = false;

  /**
   *
   */
  public KeyStorage() {
    this.user = getUserID();
  }

  public void setBoolean(String name, boolean val) {
    storeValue(name, "" + val);
  }

  public void setInt(String name, int val) {
    storeValue(name, "" + val);
  }

  public boolean isTrue(String name) {
    return getValue(name).equals(TRUE);
  }

  public int getInt(String name) {
    String value = getValue(name);

    if (value == null) return -1;
    else {
      try {
        return Integer.parseInt(value);
      } catch (NumberFormatException e) {
        return -1;
      }
    }
  }

  public int getSimpleInt(String name) {
    String value = getSimpleValue(name);

    if (value == null) return -1;
    else {
      try {
        return Integer.parseInt(value);
      } catch (NumberFormatException e) {
        return -1;
      }
    }
  }

  public boolean hasValue(String name) {
    return !getValue(name).isEmpty();
  }

  /**
   * @param name
   * @return empty string if not item stored
   * @see
   */
  public String getValue(String name) {
    if (Storage.isLocalStorageSupported()) {
      String localStorageKey = getKey(name);
      String item = Storage.getLocalStorageIfSupported().getItem(localStorageKey);
      if (DEBUG) logger.info("getValue : (" + localStorageKey + ")" + " '" + name + "' = '" + item + "'");
      if (item == null) item = "";
      return item;
    } else {
      return "";
    }
  }

  private String getSimpleValue(String name) {
    if (Storage.isLocalStorageSupported()) {
      String localStorageKey = getSimpleKey(name);
      String item = Storage.getLocalStorageIfSupported().getItem(localStorageKey);
      if (DEBUG) logger.info("getValue : (" + localStorageKey + ")" + " '" + name + "' = '" + item + "'");
      if (item == null) item = "";
      return item;
    } else {
      return "";
    }
  }

  public void storeValue(String name, String toStore) {
    if (Storage.isLocalStorageSupported()) {
      store(name, toStore, getKey(name));
    }
  }

  private void store(String name, String toStore, String localStorageKey) {
    if (DEBUG) logger.info("storeValue : (" + localStorageKey + ")" + " '" + name + "' = '" + toStore + "'");

    try {
      Storage.getLocalStorageIfSupported().setItem(localStorageKey, toStore);
    } catch (Exception e) {
      showAlert();
    }
    if (DEBUG) {
      logger.info("KeyStorage : (" + localStorageKey +
          ") storeValue " + name + "=" + toStore + " : " + getValue(name));
    }
  }

  private void showAlert() {
    if (!showedAlert) {
      showedAlert = true;
      Window.alert("Your web browser does not support storing settings locally. " +
          "In Safari, the most common cause of this is using Private Browsing Mode. " +
          "Some settings may not save or some features may not work properly for you.");
    }
  }


  private void storeSimpleValue(String name, String toStore) {
    if (Storage.isLocalStorageSupported()) {
      store(name, toStore, getSimpleKey(name));
    }
  }

  public void removeValue(String name) {
    if (Storage.isLocalStorageSupported()) {
      Storage.getLocalStorageIfSupported().removeItem(getKey(name));
      //if (debug) System.out.println("KeyStorage : removeValue " + name);
    }
  }

  private void removeSimpleValue(String name) {
    if (Storage.isLocalStorageSupported()) {
      Storage.getLocalStorageIfSupported().removeItem(getSimpleKey(name));
      //if (debug) System.out.println("KeyStorage : removeValue " + name);
    }
  }

  public int getUserID() {
    //String userIDCookie = getUserIDCookie();
    //   logger.info("getUserID " + userIDCookie);
    int anInt = getSimpleInt(USER_ID);
    // logger.info("getUserID value " + anInt);

//    String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception("getUserID"));
//    logger.info("logException stack " + exceptionAsString);

    return anInt;
  }


  public String toString() {
    return getKey("");
  }


  public String getUserChosenID() {
    if (Storage.isLocalStorageSupported()) {
      return getSimpleValue(USER_CHOSEN_ID);
    } else {
      return "";
    }
  }

  public String getUserPendingID() {
    if (Storage.isLocalStorageSupported()) {
      return getSimpleValue(USER_PENDING_ID);
    } else {
      return "";
    }
  }

  public void setPendingUserStorage(String pendingID) {
    storeSimpleValue(USER_PENDING_ID, pendingID);
  }

  public void rememberUser(String userChosenID, int userID) {
    storeSimpleValue(USER_ID, "" + userID);
    storeSimpleValue(USER_CHOSEN_ID, "" + userChosenID);
    // if (DEBUG) logger.info("storeUser : user now " + user.getID() + " / " + getUser());
  }

  /**
   * @see InitialUI#resetState()
   */
  public void clearUser() {
    if (Storage.isLocalStorageSupported()) {
      removeSimpleValue(USER_ID);
      removeSimpleValue(USER_CHOSEN_ID);
      removeSimpleValue(USER_PENDING_ID);
    }
  }

  /**
   * So try to make separate name space for different apps "netprof" vs "dialog"
   * and in the space of a user.
   *
   * @param name
   * @return
   */
  protected String getKey(String name) {
    return getAppName() + "_" + user + "_" + name;
  }

  private String getSimpleKey(String name) {
    return getAppName() + "_" + name;
  }

  @NotNull
  private String getAppName() {
    String appName = PropertyHandler.getAppName();
    String path = Window.Location.getPath();
    String app = path.substring(0, path.lastIndexOf("/"));
    appName = app.isEmpty() ? appName : app;

    if (DEBUG && !appName.equalsIgnoreCase("Netscape")) {
      logger.info("getAppName : appName = '" + appName + "'");
    }
    return appName;
  }

  public void setUser(int user) {
    this.user = user;
  }
}