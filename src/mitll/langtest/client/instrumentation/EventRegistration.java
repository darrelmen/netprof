package mitll.langtest.client.instrumentation;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Tab;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.UIObject;

/**
 * Created by go22670 on 7/25/14.
 */
public interface EventRegistration {
  EventLogger getButtonFactory();

  void register(Button button, String exid);

  void register(Button button, String exid, String context);

  void logEvent(Tab button, String widgetType, String exid, String context);

  void registerWidget(HasClickHandlers clickable, UIObject uiObject, String exid, String context);

  void logEvent(UIObject button, String widgetType, String exid, String context);
}
