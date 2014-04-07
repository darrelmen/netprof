package mitll.langtest.client.custom;

import com.google.gwt.storage.client.Storage;
import mitll.langtest.client.exercise.ExerciseController;

/**
 * Created by GO22670 on 3/5/14.
 */
public class KeyStorage {
  private ExerciseController controller;
  private final boolean debug = false;
  private String language;
  private int user;

  public KeyStorage(String language, int user) {
    this.language = language;
    this.user = user;
  }

  public KeyStorage(ExerciseController controller) {
    this(controller.getLanguage(),controller.getUser());
    this.controller = controller;
  }

  public void storeValue(String name, String toStore) {
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();

      localStorageIfSupported.setItem(getLocalStorageKey(name), toStore);
      if (debug) System.out.println("storeValue " + name + "="+toStore + " : " + getValue(name));

      //if (showMessage()) {
      //   System.err.println("----------------> huh? should not show again");
      // }
    }
  }

  public String getValue(String name) {
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();

      String item = localStorageIfSupported.getItem(getLocalStorageKey(name));
      if (debug) System.out.println("name " + name + "=" +item);
      if (item == null) item = "";
      return item;
    }
    else {
      return "";
    }
  }

  public void removeValue(String name) {
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();

      localStorageIfSupported.removeItem(getLocalStorageKey(name));
      if (debug) System.out.println("removeValue " + name);

    }
  }

  String getLocalStorageKey(String name) {
    if (controller != null) {
      language = controller.getLanguage();          // necessary???
      user = controller.getUser();
    }

    return "Navigation_" + language + "_" + user + "_" +name;
  }
}
