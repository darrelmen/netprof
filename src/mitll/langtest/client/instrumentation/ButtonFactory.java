/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.instrumentation;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Tab;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.UIObject;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.exercise.ExerciseController;

/**
 * Does event logging for widgets -- calls service to log event.
 * Created by GO22670 on 3/24/2014.
 */
public class ButtonFactory implements EventLogger {
  //private Logger logger = Logger.getLogger("ButtonFactory");
  private final LangTestDatabaseAsync service;
  private final PropertyHandler props;
  private final ExerciseController controller;

  /**
   * @param service
   * @param props
   * @param controller
   * @see mitll.langtest.client.LangTest#onModuleLoad2
   */
  public ButtonFactory(LangTestDatabaseAsync service, PropertyHandler props, ExerciseController controller) {
    this.service = service;
    this.props = props;
    this.controller = controller;
  }

  @Override
  public void register(ExerciseController controller, final Button button, final String exid) {
    registerButton(button, exid, controller.getUser());
  }

  private void registerButton(final Button button, final String exid, final int userid) {
    registerButton(button, new EventContext(exid,button.getText(),userid));
  }

  @Override
  public void registerButton(final Button button, EventContext context) {
    button.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        logEvent(button, "button", context);
      }
    });
  }

  @Override
  public void registerWidget(final HasClickHandlers clickable, final UIObject uiObject, EventContext context) {
    clickable.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        String widgetType = uiObject.getClass().toString();
        widgetType = widgetType.substring(widgetType.lastIndexOf(".") + 1);
        logEvent(uiObject, widgetType, context);
      }
    });
  }

  @Override
  public void logEvent(final UIObject button, String widgetType, EventContext context) {
    final String id = button.getElement().getId();
    logEvent(id, widgetType, context);
  }

  @Override
  public void logEvent(final Tab tab, String widgetType, EventContext context) {
    final String id = tab.asWidget().getElement().getId();
    logEvent(id, widgetType, context);
  }

  /**
   * Somehow this has service being null sometimes?
   *
   * @param widgetID
   * @param widgetType
   * @param exid
   * @param context
   * @param userid
   */
  @Override
  public void logEvent(final String widgetID, final String widgetType, EventContext context) {
    // System.out.println("logEvent event for " + widgetID + " " + widgetType + " exid " + exid + " context " + context + " user " + userid);
    final ButtonFactory outer = this;

    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        try {
          if (outer != null &&
              props != null &&
              controller != null &&
              service != null) {
            String turkID = props.getTurkID();
            String browserInfo = controller.getBrowserInfo();

            if (turkID != null && browserInfo != null) {
              service.logEvent(widgetID, widgetType, context.exid, context.context, context.userid, turkID, browserInfo,
                  new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable caught) {
                      if (caught != null &&
                          caught.getMessage() != null &&
                          !caught.getMessage().trim().equals("0")) {
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
          }
        } catch (Exception e) {
        }
      }
    });
  }
}
