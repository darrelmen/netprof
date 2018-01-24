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

package mitll.langtest.client.custom;

import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Window;
import mitll.langtest.client.exercise.ExerciseController;

import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 3/5/14.
 */
public class KeyStorage {
  private final Logger logger = Logger.getLogger("KeyStorage");

  private ExerciseController controller;
  private final boolean DEBUG = false;
  private String language;
  private int user;
  private boolean showedAlert = false;

  /**
   * @param controller
   */
  public KeyStorage(ExerciseController controller) {
    this(controller.getLanguage(), controller.getUserState().getUser());
    this.controller = controller;
  }

  /**
   * @param language
   * @param user
   * @see mitll.langtest.client.user.UserPassLogin#UserPassLogin
   */
  private KeyStorage(String language, int user) {
    this.language = language;
    this.user = user;
  }


  public void setBoolean(String name, boolean val) {
    storeValue(name, "" + val);
  }
  public void setInt(String name, int val) {
    storeValue(name, "" + val);
  }

  public boolean isTrue(String name) {
    return getValue(name).equals("true");
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

  public void storeValue(String name, String toStore) {
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
      String localStorageKey = getLocalStorageKey(name);
      try {
        localStorageIfSupported.setItem(localStorageKey, toStore);
      } catch (Exception e) {
        if (!showedAlert) {
          showedAlert = true;
          Window.alert("Your web browser does not support storing settings locally. " +
              "In Safari, the most common cause of this is using Private Browsing Mode. " +
              "Some settings may not save or some features may not work properly for you.");
        }
      }
      if (DEBUG) logger.info("KeyStorage : (" + localStorageKey +
          ") storeValue " + name + "=" + toStore + " : " + getValue(name));
    }
  }

  /**
   * @see
   * @param name
   * @return empty string if not item stored
   */
  public String getValue(String name) {
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();

      String localStorageKey = getLocalStorageKey(name);
      String item = localStorageIfSupported.getItem(localStorageKey);
      //  if (debug) System.out.println("KeyStorage : (" +localStorageKey+ ")" + " name " + name + "=" +item);
      if (item == null) item = "";
      return item;
    } else {
      return "";
    }
  }

  public boolean hasValue(String name) {
    return !getValue(name).isEmpty();
  }

  public void removeValue(String name) {
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
      localStorageIfSupported.removeItem(getLocalStorageKey(name));
      //if (debug) System.out.println("KeyStorage : removeValue " + name);
    }
  }

  private String getLocalStorageKey(String name) {
    if (controller != null) {
      language = controller.getLanguage();          // necessary???
      user     = controller.getUser();
    }

    return getKey(name);
  }

  protected String getKey(String name) {
    return "Navigation_" + language + "_" + user + "_" + name;
  }

  public String toString() {
    return getKey("");
  }
}