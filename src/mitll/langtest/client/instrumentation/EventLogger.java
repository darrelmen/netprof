package mitll.langtest.client.instrumentation;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Tab;
import com.google.gwt.user.client.ui.UIObject;
import mitll.langtest.client.exercise.ExerciseController;

/**
 * Created by GO22670 on 3/25/2014.
 */
public interface EventLogger {
  void register(ExerciseController controller, Button button, String exid);

  //void registerButton(Button button, String exid, long userid);

  void registerButton(Button button, String exid, String context, long userid);

  void logEvent(UIObject button, String widgetType, String exid, String context, long userid);

  void logEvent(Tab tab, String widgetType, String exid, String context, long userid);

  void logEvent(String widgetID, String widgetType, String exid, String context, long userid);
}
