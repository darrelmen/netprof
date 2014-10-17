package mitll.langtest.client.instrumentation;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Tab;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.UIObject;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.exercise.ExerciseController;

import java.util.logging.Logger;

/**
 * Does event logging for widgets -- calls service to log event.
 * Created by GO22670 on 3/24/2014.
 */
public class ButtonFactory implements EventLogger {
  private Logger logger = Logger.getLogger("ButtonFactory");

  private final LangTestDatabaseAsync service;
  private final PropertyHandler props;

  public ButtonFactory(LangTestDatabaseAsync service, PropertyHandler props) {
    this.service = service;
    this.props = props;
  }

  @Override
  public void register(ExerciseController controller, final Button button, final String exid) {
    registerButton(button, exid, controller.getUser());
  }

  private void registerButton(final Button button, final String exid, final long userid) {
    registerButton(button, exid, button.getText(), userid);
  }

  @Override
  public void registerButton(final Button button, final String exid, final String context, final long userid) {
    button.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        logEvent(button, "button", exid, context, userid);
      }
    });
  }

  @Override
  public void registerWidget(final HasClickHandlers clickable, final UIObject uiObject, final String exid, final String context, final long userid) {
    clickable.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        String widgetType = uiObject.getClass().toString();
        widgetType = widgetType.substring(widgetType.lastIndexOf(".") + 1);
        logEvent(uiObject, widgetType, exid, context, userid);
      }
    });
  }

  @Override
  public void logEvent(final UIObject button, String widgetType, String exid, String context, long userid) {
    final String id = button.getElement().getId();
    logEvent(id, widgetType, exid, context, userid);
  }

  @Override
  public void logEvent(final Tab tab, String widgetType, String exid, String context, long userid) {
    final String id = tab.asWidget().getElement().getId();
    logEvent(id, widgetType, exid, context, userid);
  }

  /**
   * Somehow this has service being null sometimes?
   * @param widgetID
   * @param widgetType
   * @param exid
   * @param context
   * @param userid
   */
  @Override
  public void logEvent(final String widgetID, final String widgetType, final String exid, final String context, final long userid) {
    // System.out.println("logEvent event for " + widgetID + " " + widgetType + " exid " + exid + " context " + context + " user " + userid);

    try {
      com.google.gwt.core.client.Scheduler.get().scheduleDeferred(new com.google.gwt.core.client.Scheduler.ScheduledCommand() {
        public void execute() {
          service.logEvent(widgetID, widgetType, exid, context, userid, props.getTurkID(), new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
              if (!caught.getMessage().trim().equals("0")) {
                // System.err.println("FAILED to send event for " + widgetID + " message '" + caught.getMessage() +"'");
                caught.printStackTrace();
              }
            }

            @Override
            public void onSuccess(Void result) {
              //System.out.println("sent event for " + widgetID);
            }
          });
        }
      });
    } catch (Exception e) {
      logger.warning("Got " +e);
    }

  }
}
