/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
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
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 3/24/2014.
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
   *
   * @param widgetID
   * @param widgetType
   * @param exid
   * @param context
   * @param userid
   */
  @Override
  public void logEvent(final String widgetID, final String widgetType, final String exid, final String context,
                       final long userid) {
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
              service.logEvent(widgetID, widgetType, exid, context, userid, turkID, browserInfo,
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
