package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.Modal;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.dialog.DialogHelper;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.domino.user.ChangePasswordView;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.initial.InitialUI;
import mitll.langtest.client.initial.LifecycleSupport;
import mitll.langtest.client.initial.PropertyHandler;
import mitll.langtest.client.initial.UILifecycle;
import mitll.langtest.client.instrumentation.EventTable;
import mitll.langtest.client.result.ActiveUsersManager;
import mitll.langtest.client.result.ReportListManager;
import mitll.langtest.client.result.ResultManager;
import mitll.langtest.client.services.AudioServiceAsync;
import mitll.langtest.client.services.LangTestDatabaseAsync;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.client.user.UserState;
import mitll.langtest.shared.project.StartupInfo;
import mitll.langtest.shared.user.ActiveUser;
import mitll.langtest.shared.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by go22670 on 1/8/17.
 */
public class UserMenu {
  private static final String ARE_YOU_AN_INSTRUCTOR = "Are you an instructor?";
  private static final String BROWSER_RECORDING = "Browser recording";
  private static final String RECORDING_TYPE = "Recording type";
  private static final String DOMINO_URL = "domino.url";

  private final Logger logger = Logger.getLogger("UserMenu");

  private static final String REQUEST_INSTRUCTOR_STATUS = "Request Instructor Status";
  private static final List<String> REQ_MESSAGE = Arrays.asList(
      "Click OK to request instructor status.",
      "Instructors get to make quizzes and view additional analysis information.",
      "(It may take some time for your request to be confirmed.)");

  private static final String ACTIVE_USERS = "Active Users";
  private static final String ACTIVE_USERS_WEEK = ACTIVE_USERS + " Week";
  private static final String TEST_EXCEPTION = "Test Exception";
  private static final String TEST_EXCEPTION_MSG = "Test Exception - this tests to see that email error reporting is configured properly. Thanks!";
  private static final String STATUS_REPORT_BEING_GENERATED = "Status report being generated.";
  private static final String MESSAGE__504 = "504";
  private static final String CHANGE_PASSWORD = "Change Password";
  private static final String SEND_TEST_EXCEPTION = "Send Test Exception";
  private static final String CHECK_YOUR_EMAIL = "Check your email.";
  private static final String IT_CAN_TAKE_AWHILE = "It can take awhile to generate the report.<br>Please check your email after a few minutes.";

  private static final String PLEASE_CHECK_YOUR_EMAIL = "Please check your email.";
  private static final String STATUS_REPORT_SENT = "Status report sent";
  private static final String MANAGE_USERS = "Manage Users";
  private static final String EVENTS = "Events";
  private static final String SEND_REPORT = "Send Report to You";
  private static final String REPORT_LIST = "Weekly Report List";

  private static final String ABOUT_NETPROF = "About Netprof";
  private static final String LOG_OUT = "Sign Out";

  private final UserManager userManager;

  private final LifecycleSupport lifecycleSupport;
  private final ExerciseController controller;
  private final UserState userState;
  private final PropertyHandler props;

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
    choices.add(new LinkAndTitle(ACTIVE_USERS, new SuccessClickHandler(() -> showActiveUsers(24))));
    choices.add(new LinkAndTitle(ACTIVE_USERS_WEEK, new SuccessClickHandler(() -> showActiveUsers(7 * 24))));
    choices.add(new LinkAndTitle("Active Teachers", new SuccessClickHandler(this::showActiveTeachers)));
    choices.add(new LinkAndTitle("All Teachers", new SuccessClickHandler(this::showAllTeachers)));

    //choices.add(new LinkAndTitle("Users", new UsersClickHandler(), true));
    addSendReport(choices);
    // choices.add(new LinkAndTitle(REPORT_LIST, new ReportListHandler()));
    choices.add(new LinkAndTitle(REPORT_LIST, new SuccessClickHandler(() -> new ReportListManager(controller).showReportList())));
    choices.add(new LinkAndTitle(SEND_TEST_EXCEPTION, event -> sendTestExceptionToAllServers()));

    return choices;
  }

  private void sendTestExceptionToAllServers() {
    List<String> strings = new ArrayList<>();
    Collection<AudioServiceAsync> allAudioServices = controller.getAllAudioServices();
    allAudioServices.forEach(audioServiceAsync ->
        audioServiceAsync.logMessage(TEST_EXCEPTION,
            TEST_EXCEPTION_MSG, true, new AsyncCallback<Void>() {
              @Override
              public void onFailure(Throwable caught) {
                new ModalInfoDialog("Error", "Somehow couldn't send test emails.");

              }

              @Override
              public void onSuccess(Void result) {
                strings.add("got");
                if (strings.size() == allAudioServices.size()) {
                  new ModalInfoDialog(CHECK_YOUR_EMAIL, "Email should arrive sent from all servers.");
                }
              }
            })
    );

    controller.logMessageOnServer(TEST_EXCEPTION, TEST_EXCEPTION_MSG, true);
  }


  List<LinkAndTitle> getProjectSpecificChoices() {
    List<LinkAndTitle> choices = new ArrayList<>();
    String nameForAnswer = props.getNameForAnswer() + "s";
    // choices.add(new LinkAndTitle(getCapitalized(nameForAnswer), new ResultsClickHandler()));
    choices.add(new LinkAndTitle(getCapitalized(nameForAnswer),
//        new ResultsClickHandler()
        new SuccessClickHandler(() -> new ResultManager(
            props.getNameForAnswer(),
            lifecycleSupport.getProjectStartupInfo().getTypeOrder(),
            lifecycleSupport,
            controller).showResults()
        )
    ));
    //  choices.add(new Banner.LinkAndTitle("Monitoring", new MonitoringClickHandler(), true));
    choices.add(new LinkAndTitle(EVENTS,
        new SuccessClickHandler(() -> new EventTable().show(lazyGetService(), controller.getMessageHelper()))));
    return choices;
  }

  private void addSendReport(List<LinkAndTitle> choices) {
    choices.add(new LinkAndTitle(SEND_REPORT, event -> {
      new ModalInfoDialog(STATUS_REPORT_BEING_GENERATED, IT_CAN_TAKE_AWHILE);
      sendReport();
    }));
  }

  private void sendReport() {
    lazyGetService().sendReport(new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
        if (caught.getMessage().contains(MESSAGE__504)) {
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
  List<LinkAndTitle> getStandardUserMenuChoices(List<LinkAndTitle> teacherReq) {
    List<LinkAndTitle> choices = new ArrayList<>();

    choices.add(getChangePassword());

    LinkAndTitle linkAndTitle = maybeAddReqInstructor(choices);
    if (linkAndTitle != null) teacherReq.add(linkAndTitle);

    choices.add(getLogOut());

    return choices;
  }

  private LinkAndTitle maybeAddReqInstructor(List<LinkAndTitle> choices) {
    User current = userManager.getCurrent();

    if (current != null && !current.isTeacher() && !current.getPermissions().contains(User.Permission.TEACHER_PERM)) {
      LinkAndTitle e = new LinkAndTitle(REQUEST_INSTRUCTOR_STATUS,
          new SuccessClickHandler(() -> new DialogHelper(true)
              .show(ARE_YOU_AN_INSTRUCTOR,
                  REQ_MESSAGE, new DialogHelper.CloseListener() {
                    @Override
                    public boolean gotYes() {
                      //logger.info("Sending request.");
                      controller.getUserService().sendTeacherRequest(new AsyncCallback<Void>() {
                        @Override
                        public void onFailure(Throwable caught) {
                        }

                        @Override
                        public void onSuccess(Void result) {
                        }
                      });
                      return true;
                    }

                    @Override
                    public void gotNo() {

                    }

                    @Override
                    public void gotHidden() {

                    }
                  }, "OK", "Cancel")));
      choices.add(e);
      return e;
    }
    else return null;


  }

  @NotNull
  private LinkAndTitle getChangePassword() {
    return new LinkAndTitle(CHANGE_PASSWORD, new SuccessClickHandler(() ->
        new ChangePasswordView(userManager.getCurrent(), false, userState,
            controller.getUserService()).showModal()));
  }

  @NotNull
  private LinkAndTitle getLogOut() {
    return new LinkAndTitle(LOG_OUT, event -> uiLifecycle.logout());
  }

  private class SuccessClickHandler implements ClickHandler {
    final OnSuccess onSuccess;

    SuccessClickHandler(OnSuccess onSuccess) {
      this.onSuccess = onSuccess;
    }

    public void onClick(ClickEvent event) {
      GWT.runAsync(new RunAsyncCallback() {
        public void onFailure(Throwable caught) {
          downloadFailedAlert();
        }

        public void onSuccess() {
          onSuccess.onSuccess();
        }
      });
    }
  }

  interface OnSuccess {
    void onSuccess();
  }

  private LangTestDatabaseAsync lazyGetService() {
    return controller.getService();
  }

  private void showActiveUsers(int hours) {
    new ActiveUsersManager(controller).show("Active Users", hours);
  }

  private void showActiveTeachers() {
    new ActiveUsersManager(controller) {
      protected void getUsers(int hours, DialogBox dialogBox, Panel dialogVPanel) {
        controller.getUserService().getActiveTeachers(new AsyncCallback<List<ActiveUser>>() {
          @Override
          public void onFailure(Throwable caught) {
            controller.getMessageHelper().handleNonFatalError("getting active users", caught);
          }

          @Override
          public void onSuccess(List<ActiveUser> result) {
            gotUsers(result, dialogVPanel, dialogBox);
          }
        });
      }
    }.show("Active Teachers", 0);
  }

  private void showAllTeachers() {
    new ActiveUsersManager(controller) {
      protected void getUsers(int hours, DialogBox dialogBox, Panel dialogVPanel) {
        controller.getUserService().getTeachers(new AsyncCallback<List<ActiveUser>>() {
          @Override
          public void onFailure(Throwable caught) {
            controller.getMessageHelper().handleNonFatalError("getting active users", caught);
          }

          @Override
          public void onSuccess(List<ActiveUser> result) {
            gotUsers(result, dialogVPanel, dialogBox);
          }
        });
      }
    }.show("All Teachers", 0);
  }

  private void downloadFailedAlert() {
    Window.alert("Code download failed");
  }

  public NavLink getAbout() {
    NavLink about = new NavLink(ABOUT_NETPROF);
    about.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent clickEvent) {
        Map<String, String> props = UserMenu.this.props.getProps();

        List<Map<String, String>> allProps = new ArrayList<>();
        allProps.add(props);
        Collection<AudioServiceAsync> allAudioServices = controller.getAllAudioServices();
        allAudioServices.forEach(audioServiceAsync ->
            audioServiceAsync.getStartupInfo(new AsyncCallback<StartupInfo>() {
              @Override
              public void onFailure(Throwable caught) {

              }

              @Override
              public void onSuccess(StartupInfo result) {
                Map<String, String> properties = result.getProperties();
                allProps.add(properties);
                List<String> keys = new ArrayList<>();
                List<String> values = new ArrayList<>();
                if (allProps.size() == allAudioServices.size() + 1) {
                  showAllProps(keys, values);
                }
              }

              private void showAllProps(List<String> keys, List<String> values) {
                for (int i = 0; i < allProps.size(); i++) {
                  List<String> strings = getPropKeys(allProps.get(i), i + 1);
                  keys.addAll(strings);
                  values.addAll(allProps.get(i).values());
                }

                new ModalInfoDialog(ABOUT_NETPROF, keys, values, null, null, false, true, 600, 600) {
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
            }));
      }
    });
    return about;
  }


  @NotNull
  private List<String> getPropKeys(Map<String, String> props, int server) {
    List<String> strings = new ArrayList<>();
    try {
      if (server == 1) {
        props.put(RECORDING_TYPE, BROWSER_RECORDING);
      }

      props.remove(DOMINO_URL);
      Optional<String> max = props.keySet().stream().max(Comparator.comparingInt(String::length));
      if (max.isPresent()) {
        int maxl = max.get().length();
        props.keySet().forEach(key -> strings.add("Server #" + server + " : " + key + getLen(maxl - key.length()))
        );
      }
    } catch (Exception e) {
      //logger.warning("got " + e);
    }
    return strings;
  }

  private String getLen(int len) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < len; i++) builder.append(" ");
    return builder.toString();
  }

  @NotNull
  private String getMailTo() {
    return "mailto:" + controller.getProps().getHelpEmail() +
        "?Subject=" +
        "Question%20about%20Netprof";
  }
}
