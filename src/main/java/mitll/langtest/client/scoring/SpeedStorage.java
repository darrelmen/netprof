package mitll.langtest.client.scoring;

import com.google.gwt.storage.client.Storage;
import mitll.langtest.client.exercise.ExerciseController;

/**
 * Created by go22670 on 4/6/17.
 * @deprecated redundant with key storage
 */
public class SpeedStorage {
  private final String instance;
  private final ExerciseController controller;
  private String speedKey = "audioSpeed";

  public SpeedStorage(ExerciseController controller, String instance) {
    this.controller = controller;
    this.instance = instance;
  }

  public SpeedStorage(ExerciseController controller, String instance, String speedKey) {
    this(controller, instance);
    this.speedKey = speedKey;
  }

  public boolean isRegularSpeed() {
    return isRegularSpeed(getStorageKey());
  }

  private boolean isRegularSpeed(String selectedUserKey) {
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
      String item = localStorageIfSupported.getItem(selectedUserKey);
      //    logger.info("isRegularSpeed value for " + selectedUserKey + "='" + item+ "'");
      if (item != null) {
        return item.toLowerCase().equals("true");
      } else {
        storeIsSet(true);
        return true;
      }
    }
    // else {
    return false;
    // }
  }

  private String getStorageKey(ExerciseController controller, String appTitle) {
    return getStoragePrefix(controller, appTitle) + speedKey;
  }

  private String getStoragePrefix(ExerciseController controller, String appTitle) {
    return appTitle + ":" + controller.getUser() + ":" + instance + ":";
  }

  public void storeIsSet(boolean speed) {
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
      localStorageIfSupported.setItem(getStorageKey(), "" + speed);
    }
  }

  private String getStorageKey() {
    return getStorageKey(controller, controller.getLanguage());
  }

}
