package mitll.langtest.client.instrumentation;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Tab;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.UIObject;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.exercise.ExerciseController;

/**
 * Created by GO22670 on 3/24/2014.
 */
public class ButtonFactory implements EventLogger {
  private final LangTestDatabaseAsync service;
  private PropertyHandler props;

  public ButtonFactory(LangTestDatabaseAsync service, PropertyHandler props) {
    this.service = service;
    this.props = props;
  }
/*
  public void register(ExerciseController controller, final Button button, final String exid, final String context, final long userid) {
    if (controller.getProps().doInstrumentation()) {
      registerButton(button, exid, context, userid);
    }
  }
*/

/*  public void register(ExerciseController controller, final Button button, final String exid, final long userid) {
    if (controller.getProps().doInstrumentation()) {
      registerButton(button, exid, userid);
    }
  }*/

  @Override
  public void register(ExerciseController controller, final Button button, final String exid) {
    registerButton(button, exid, controller.getUser());
  }

  private void registerButton(final Button button, final String exid, final long userid) {
    registerButton(button, exid, button.getText(), userid);
  }

  /* public void registerButton(final Button button, final String exid, final long userid) {
    registerButton(button, exid, button.getText(), userid);
  }*/

  @Override
  public void registerButton(final Button button, final String exid, final String context, final long userid) {
    //System.out.println("registering " + button.getElement().getId());
    button.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        logEvent(button, "button",exid, context, userid);
      }
    });
  }

  @Override
  public void logEvent(final UIObject button, String widgetType, String exid, String context, long userid) {
    final String id = button.getElement().getId();
    logEvent(id, widgetType,exid, context, userid);
  }

  @Override
  public void logEvent(final Tab tab, String widgetType, String exid, String context, long userid) {
    final String id = tab.asWidget().getElement().getId();
    logEvent(id, widgetType,exid, context, userid);
  }

  @Override
  public void logEvent(final String widgetID, String widgetType, String exid, String context, long userid) {
    service.logEvent(widgetID, widgetType, exid, context, userid, props.getTurkID(), new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
        System.err.println("FAILED to send event for " + widgetID);

      }

      @Override
      public void onSuccess(Void result) {

        //System.out.println("sent event for " + widgetID);
      }
    });
  }
}
