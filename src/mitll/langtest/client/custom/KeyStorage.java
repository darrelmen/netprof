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

  /**
   * @see mitll.langtest.client.user.StudentDialog#setDefaultControlValues(int)
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

  public void storeValue(String name, String toStore) {
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();

      String localStorageKey = getLocalStorageKey(name);
      localStorageIfSupported.setItem(localStorageKey, toStore);
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
}
