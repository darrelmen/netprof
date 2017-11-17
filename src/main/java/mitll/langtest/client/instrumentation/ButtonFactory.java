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

import com.github.gwtbootstrap.client.ui.Tab;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.UIObject;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.initial.PropertyHandler;
import mitll.langtest.client.services.LangTestDatabaseAsync;
import mitll.langtest.shared.project.ProjectStartupInfo;

import java.util.logging.Logger;

/**
 * Does event logging for widgets -- calls service to log event.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 3/24/2014.
 */
public class ButtonFactory implements EventLogger {
  private final Logger logger = Logger.getLogger("ButtonFactory");
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
  public void register(ExerciseController controller, final UIObject button, final String exid) {
    registerButton(button, exid, controller.getUserState().getUser());
  }

  private void registerButton(final UIObject button, final String exid, final int userid) {
    String text = ((HasText)button).getText();
    registerButton(button, new EventContext(exid, text, userid));
  }

  @Override
  public void registerButton(final UIObject button, EventContext context) {
    if (button instanceof HasClickHandlers) {
      HasClickHandlers button1 = (HasClickHandlers) button;
      button1.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          logEvent(button, "button", context);
        }
      });
    }
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
   * @param context
   * @paramx exid
   * @paramx userid
   */
  @Override
  public void logEvent(final String widgetID, final String widgetType, EventContext context) {
//    logger.info("logEvent event for " + widgetID + " " + widgetType + " context " + context);
    final ButtonFactory outer = this;
    if (context.getUserid() == -1) context.setUserid(controller.getUserState().getUser());

    Scheduler.get().scheduleDeferred(() -> {
      try {
        if (outer != null &&
            props != null &&
            controller != null &&
            service != null) {
          String browserInfo = controller.getBrowserInfo();

          if (
              browserInfo != null) {
            //logger.info("\tlogEvent event for " + widgetID + " " + widgetType + " context " + context + " browser " +browserInfo);

            ProjectStartupInfo projectStartupInfo = controller.getProjectStartupInfo();
            int projID = projectStartupInfo == null? -1:projectStartupInfo.getProjectid();
            service.logEvent(widgetID, widgetType, context.exid, context.context, context.getUserid(), browserInfo,
                projID, new AsyncCallback<Void>() {
                  @Override
                  public void onFailure(Throwable caught) {
                    if (caught != null &&
                        caught.getMessage() != null &&
                        !caught.getMessage().trim().equals("0")) {
                      logger.warning("logEvent FAILED to send event for " + widgetID + " message '" + caught.getMessage() + "'");
                      //caught.printStackTrace();
                    }
                  }

                  @Override
                  public void onSuccess(Void result) {
                    //logger.info("logEvent sent event for " + widgetID);
                  }
                });
          }
        }
      } catch (Exception e) {
      }
    });
  }
}
