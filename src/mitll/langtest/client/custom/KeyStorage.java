package mitll.langtest.client.custom;

import com.google.gwt.storage.client.Storage;
import mitll.langtest.client.exercise.ExerciseController;

/**
 * Created by GO22670 on 3/5/14.
 */
public class KeyStorage {
  private ExerciseController controller;
  public KeyStorage(ExerciseController controller) {
    this.controller = controller;
  }
  public void storeValue(String name, String toStore) {
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();

      localStorageIfSupported.setItem(getLocalStorageKey(name), toStore);
      System.out.println("storeValue " + name + "="+toStore + " : " + getValue(name));

      //if (showMessage()) {
      //   System.err.println("----------------> huh? should not show again");
      // }
    }
  }

  public String getValue(String name) {
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();

      String item = localStorageIfSupported.getItem(getLocalStorageKey(name));
      System.out.println("name " + name + "=" +item);
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
      System.out.println("removeValue " + name);

    }
    else {
      //return "";
    }
  }

  protected String getLocalStorageKey(String name) {
    return "Navigation_" + controller.getLanguage() + "_" + controller.getUser() + "_" +name;
  }

}
