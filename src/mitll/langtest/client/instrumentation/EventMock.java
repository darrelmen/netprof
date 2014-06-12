package mitll.langtest.client.instrumentation;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Tab;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.UIObject;
import mitll.langtest.client.exercise.ExerciseController;

/**
 * Created by GO22670 on 3/25/2014.
 */
public class EventMock implements EventLogger {
  @Override
  public void register(ExerciseController controller, Button button, String exid) {

  }

  @Override
  public void registerWidget(HasClickHandlers clickable, UIObject uiObject, String exid, String context, long userid) {

  }

  @Override
  public void registerButton(Button button, String exid, String context, long userid) {

  }

  @Override
  public void logEvent(UIObject button, String widgetType, String exid, String context, long userid) {

  }

  @Override
  public void logEvent(Tab tab, String widgetType, String exid, String context, long userid) {

  }

  @Override
  public void logEvent(String widgetID, String widgetType, String exid, String context, long userid) {

  }
}
