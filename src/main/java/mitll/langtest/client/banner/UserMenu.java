package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.Modal;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.domino.user.ChangePasswordView;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.initial.*;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.client.instrumentation.EventTable;
import mitll.langtest.client.recorder.FlashRecordPanelHeadless;
import mitll.langtest.client.result.ActiveUsersManager;
import mitll.langtest.client.result.ReportListManager;
import mitll.langtest.client.result.ResultManager;
import mitll.langtest.client.services.LangTestDatabase;
import mitll.langtest.client.services.LangTestDatabaseAsync;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.client.user.UserState;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by go22670 on 1/8/17.
 */
public class UserMenu {
  private final Logger logger = Logger.getLogger("UserMenu");
  //private static final String PLEASE_WAIT = "Please wait... this can take awhile.";

  private static final String PLEASE_CHECK_YOUR_EMAIL = "Please check your email.";
  private static final String STATUS_REPORT_SENT = "Status report sent";
  private static final String MANAGE_USERS = "Manage Users";
  private static final String EVENTS = "Events";
  private static final String SEND_REPORT = "Send Report to You";
  private static final String REPORT_LIST = "Weekly Report List";

  private static final String ABOUT_NETPROF = "About Netprof";
  private static final String NETPROF_HELP_LL_MIT_EDU = "netprof-help@dliflc.edu";
  private static final String LOG_OUT = "Sign Out";

  private final UserManager userManager;

  private final LifecycleSupport lifecycleSupport;
  private final ExerciseController controller;
  private final UserState userState;
  private final PropertyHandler props;

  private LangTestDatabaseAsync service = null;

  /**
   * @see #getLogOut
   */
  private final UILifecycle uiLifecycle;

  /**
   * @param langTest
   * @param userManager
   * @see InitialUI#InitialUI(LangTest, UserManager)
   */
  public UserMenu(LangTest langTest,
                  UserManager userManager,
                  UILifecycle uiLifecycle) {
    this.lifecycleSupport = langTest;
    this.userState = langTest;
    this.props = langTest.getProps();
    this.userManager = userManager;
    this.controller = langTest;
    this.uiLifecycle = uiLifecycle;
  }

  /**
   * @return
   * @see NewBanner#getRightSideChoices
   */
  List<LinkAndTitle> getCogMenuChoicesForAdmin() {
    List<LinkAndTitle> choices = new ArrayList<>();
    choices.add(new LinkAndTitle(MANAGE_USERS, props.getDominoURL()));
    choices.add(new LinkAndTitle("Active Users", new ActiveUsersHandler()));
   // choices.add(new LinkAndTitle("Active Today", new ActiveUsersHandlerDay()));
    //choices.add(new LinkAndTitle("Users", new UsersClickHandler(), true));
    addSendReport(choices);
    choices.add(new LinkAndTitle(REPORT_LIST, new ReportListHandler()));

    return choices;
  }

  List<LinkAndTitle> getProjectSpecificChoices(Widget menu) {
    List<LinkAndTitle> choices = new ArrayList<>();
    String nameForAnswer = props.getNameForAnswer() + "s";
    choices.add(new LinkAndTitle(getCapitalized(nameForAnswer), new ResultsClickHandler()));
    //  choices.add(new Banner.LinkAndTitle("Monitoring", new MonitoringClickHandler(), true));
    choices.add(new LinkAndTitle(EVENTS, new EventsClickHandler()));
/*    choices.add(new LinkAndTitle(DOWNLOAD_CONTEXT + " Female", new DownloadContentsClickHandler(menu,false)));
    choices.add(new LinkAndTitle(DOWNLOAD_CONTEXT + " Male", new DownloadContentsClickHandler(menu,true)));*/
    return choices;
  }

  private void addSendReport(List<LinkAndTitle> choices) {
    choices.add(new LinkAndTitle(SEND_REPORT, event -> {
      new ModalInfoDialog("Status report being generated.", "It can take awhile to generate the report.<br>Please check your email after a few minutes.");
      sendReport();
    }));
  }

  private void sendReport() {
    lazyGetService().sendReport(new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
        if (caught.getMessage().contains("504")) {
          logger.info("OK send usage timed out...");
        } else {
          controller.handleNonFatalError("sending usage report", caught);
        }
      }

      @Override
      public void onSuccess(Void result) {
        new ModalInfoDialog(STATUS_REPORT_SENT, PLEASE_CHECK_YOUR_EMAIL);
      }
    });
  }

  @NotNull
  private String getCapitalized(String nameForAnswer) {
    return nameForAnswer.substring(0, 1).toUpperCase() + nameForAnswer.substring(1);
  }

  /**
   * @return
   * @see NewBanner#addUserMenu
   */
  List<LinkAndTitle> getStandardUserMenuChoices() {
    List<LinkAndTitle> choices = new ArrayList<>();

    choices.add(getChangePassword());
    choices.add(getLogOut());

    return choices;
  }

  @NotNull
  private LinkAndTitle getChangePassword() {
    return new LinkAndTitle("Change Password", new ChangePasswordClickHandler());
  }

  @NotNull
  private LinkAndTitle getLogOut() {
    return new LinkAndTitle(LOG_OUT, event -> uiLifecycle.logout());
  }

/*  private class UsersClickHandler implements ClickHandler {
    public void onClick(ClickEvent event) {
      GWT.runAsync(new RunAsyncCallback() {
        public void onFailure(Throwable caught) {
          downloadFailedAlert();
        }

        public void onSuccess() {
          new UserTable(props, userManager.isAdmin()).showUsers(controller.getUserService());
        }
      });
    }
  }*/

  private class EventsClickHandler implements ClickHandler {
    public void onClick(ClickEvent event) {
      GWT.runAsync(new RunAsyncCallback() {
        public void onFailure(Throwable caught) {
          downloadFailedAlert();
        }

        public void onSuccess() {
          new EventTable().show(lazyGetService(), controller.getMessageHelper());
        }
      });
    }
  }

  private LangTestDatabaseAsync lazyGetService() {
    if (service == null) {
      service = GWT.create(LangTestDatabase.class);
    }
    return service;
  }

  private class ChangePasswordClickHandler implements ClickHandler {
    public void onClick(ClickEvent event) {
      GWT.runAsync(new RunAsyncCallback() {
        public void onFailure(Throwable caught) {
          downloadFailedAlert();
        }

        public void onSuccess() {
          new ChangePasswordView(userManager.getCurrent(), false, userState, controller.getUserService()).showModal();
        }
      });
    }
  }

  private class ResultsClickHandler implements ClickHandler {
    final EventRegistration outer = lifecycleSupport;

    public void onClick(ClickEvent event) {
      GWT.runAsync(new RunAsyncCallback() {
        public void onFailure(Throwable caught) {
          downloadFailedAlert();
        }

        public void onSuccess() {
          ResultManager resultManager = new ResultManager(
              props.getNameForAnswer(),
              lifecycleSupport.getProjectStartupInfo().getTypeOrder(),
              outer,
              controller);
          resultManager.showResults();
        }
      });
    }
  }

  private class ReportListHandler implements ClickHandler {
    final EventRegistration outer = lifecycleSupport;

    public void onClick(ClickEvent event) {
      GWT.runAsync(new RunAsyncCallback() {
        public void onFailure(Throwable caught) {
          downloadFailedAlert();
        }

        public void onSuccess() {
          new ReportListManager(controller).showReportList();
        }
      });
    }
  }
  private class ActiveUsersHandler implements ClickHandler {
    final EventRegistration outer = lifecycleSupport;

    public void onClick(ClickEvent event) {
      GWT.runAsync(new RunAsyncCallback() {
        public void onFailure(Throwable caught) {
          downloadFailedAlert();
        }

        public void onSuccess() {
          new ActiveUsersManager(controller).show(24);
        }
      });
    }
  }
 /* private class ActiveUsersHandlerDay implements ClickHandler {
    final EventRegistration outer = lifecycleSupport;

    public void onClick(ClickEvent event) {
      GWT.runAsync(new RunAsyncCallback() {
        public void onFailure(Throwable caught) {
          downloadFailedAlert();
        }

        public void onSuccess() {
          new ActiveUsersManager(controller).show(24);
        }
      });
    }
  }*/

  private void downloadFailedAlert() {
    Window.alert("Code download failed");
  }

  public NavLink getAbout() {
    NavLink about = new NavLink(ABOUT_NETPROF);
    about.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent clickEvent) {
        Map<String, String> props = UserMenu.this.props.getProps();
        List<String> strings = new ArrayList<>();
        try {
          String recordingInfo = FlashRecordPanelHeadless.usingWebRTC() ? "Browser recording" : "Flash recording";
          props.put("Recording type", recordingInfo);
          props.remove("domino.url");
          Optional<String> max = props.keySet().stream().max(Comparator.comparingInt(String::length));
          if (max.isPresent()) {
            int maxl = max.get().length();
            props.keySet().forEach(key -> strings.add(key + getLen(maxl - key.length()))
            );
          }
        } catch (Exception e) {
          //logger.warning("got " + e);
        }

        new ModalInfoDialog(ABOUT_NETPROF, strings, props.values(), null, null, false, true, 600, 400) {
          @Override
          protected FlexTable addContent(Collection<String> messages, Collection<String> values, Modal modal, boolean bigger) {
            FlexTable flexTable = super.addContent(messages, values, modal, bigger);

            int rowCount = flexTable.getRowCount();
            flexTable.setHTML(rowCount + 1, 0, "Need Help?");
            flexTable.setHTML(rowCount + 1, 1, " <a href='" + getMailTo() + "'>Help Email</a>");
            return flexTable;
          }
        };
      }
    });
    return about;
  }

  private String getLen(int len) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < len; i++) builder.append(" ");
    return builder.toString();
  }

  @NotNull
  private String getMailTo() {
    return "mailto:" +
        NETPROF_HELP_LL_MIT_EDU + "?" +
        //   "cc=" + LTEA_DLIFLC_EDU + "&" +
        "Subject=Question%20about%20NetProF";
  }
}
