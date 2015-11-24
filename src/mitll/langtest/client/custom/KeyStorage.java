/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.custom;

import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Window;
import mitll.langtest.client.exercise.ExerciseController;

/**
 * Created by GO22670 on 3/5/14.
 */
public class KeyStorage {
  private ExerciseController controller;
  private final boolean debug = false;
  private String language;
  private int user;

  /**
   * @see mitll.langtest.client.user.UserPassLogin#UserPassLogin
   * @param language
   * @param user
   */
  public KeyStorage(String language, int user) {
    this.language = language;
    this.user = user;
  }

  public KeyStorage(ExerciseController controller) {
    this(controller.getLanguage(), controller.getUser());
    this.controller = controller;
  }

  private final boolean showedAlert = false;
  public void storeValue(String name, String toStore) {
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();

      String localStorageKey = getLocalStorageKey(name);
      try {
        localStorageIfSupported.setItem(localStorageKey, toStore);
      } catch (Exception e) {
        if (!showedAlert) {
          Window.alert("Your web browser does not support storing settings locally. " +
              "In Safari, the most common cause of this is using Private Browsing Mode. " +
              "Some settings may not save or some features may not work properly for you.");
        }
      }
      if (debug) System.out.println("KeyStorage : (" + localStorageKey+
        ") storeValue " + name + "="+toStore + " : " + getValue(name));
    }
  }

  public String getValue(String name) {
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();

      String localStorageKey = getLocalStorageKey(name);
      String item = localStorageIfSupported.getItem(localStorageKey);
      if (debug) System.out.println("KeyStorage : (" +localStorageKey+ ")" + " name " + name + "=" +item);
      if (item == null) item = "";
      return item;
    }
    else {
      return "";
    }
  }

  public boolean hasValue(String name) { return !getValue(name).isEmpty(); }

  public void removeValue(String name) {
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();

      localStorageIfSupported.removeItem(getLocalStorageKey(name));
      if (debug) System.out.println("KeyStorage : removeValue " + name);

    }
  }

  private String getLocalStorageKey(String name) {
    if (controller != null) {
      language = controller.getLanguage();          // necessary???
      user = controller.getUser();
    }

    return getKey(name);
  }

  protected String getKey(String name) {
    return "Navigation_" + language + "_" + user + "_" +name;
  }

  public String toString() { return getKey(""); }
}
