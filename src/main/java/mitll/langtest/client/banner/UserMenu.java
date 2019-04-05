/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

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
import mitll.langtest.shared.user.Permission;
import mitll.langtest.shared.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by go22670 on 1/8/17.
 */
public class UserMenu {
  public static final String REQUEST_PENDING = "Request Pending...";
  private final Logger logger = Logger.getLogger("UserMenu");

  private static final String ARE_YOU_AN_INSTRUCTOR = "Are you an instructor?";

  private static final String DOMINO_URL = "domino.url";
  private static final String NEED_HELP = "Need Help?";
  private static final String HELP_EMAIL = "Help Email";
  private static final String GETTING_ALL_TEACHERS = "getting all teachers";
  private static final String GETTING_ACTIVE_USERS = "getting active users";
  private static final String ALL_TEACHERS = "All Teachers";
  private static final String ACTIVE_TEACHERS = "Active Teachers";

  /**
   * @see #addPendingTeacher(List)
   */
  private static final String PENDING_TEACHER_REQUESTS = "Pending Instructor Requests";

  /**
   * @see #maybeAddReqInstructor(List)
   */
  private static final String REQUEST_INSTRUCTOR_STATUS = "Request Instructor Status";
  private static final List<String> REQ_MESSAGE = Arrays.asList(
      "Click OK to request instructor status.",
      "Instructors get to make quizzes and view additional analysis information.",
      "(It may take some time for your request to be confirmed.)");

  private static final String ACTIVE_USERS1 = "Active Users";
  private static final String ACTIVE_USERS = ACTIVE_USERS1;
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
  private final ExerciseController<?> controller;
  private final UserState userState;
  private final PropertyHandler props;

  /**
   * @see #getLogOut
   */
  private final UILifecycle uiLifecycle;
  private LinkAndTitle pendingTeachers;
  private IBanner banner;

  /**
   * @param langTest
   * @param userManager
   * @see InitialUI#InitialUI(LangTest, UserManager)
   */
  public UserMenu(LangTest langTest, UserManager userManager, UILifecycle uiLifecycle) {
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

    choices.add(new LinkAndTitle(MANAGE_USERS, props.getDominoURL()).setAdmin(true));
    choices.add(new LinkAndTitle(ACTIVE_USERS, new SuccessClickHandler(() -> showActiveUsers(24))).setAdmin(true));
    choices.add(new LinkAndTitle(ACTIVE_USERS_WEEK, new SuccessClickHandler(() -> showActiveUsers(7 * 24))).setAdmin(true));
    choices.add(new LinkAndTitle(ACTIVE_TEACHERS, new SuccessClickHandler(this::showActiveTeachers)).setAdmin(true));
    choices.add(new LinkAndTitle(ALL_TEACHERS, new SuccessClickHandler(this::showAllTeachers)).setAdmin(true));
    choices.addAll(getCogMenuChoicesForTeacher());
    addSendReport(choices);
    choices.add(new LinkAndTitle(REPORT_LIST, new SuccessClickHandler(() -> new ReportListManager(controller).showReportList())).setAdmin(true));
    choices.add(new LinkAndTitle(SEND_TEST_EXCEPTION, event -> sendTestExceptionToAllServers()).setAdmin(true));

    return choices;
  }

  private List<LinkAndTitle> getCogMenuChoicesForTeacher() {
    List<LinkAndTitle> choices = new ArrayList<>();
    addPendingTeacher(choices);
    return choices;
  }

  private void addPendingTeacher(List<LinkAndTitle> choices) {
    choices.add(pendingTeachers =
        new LinkAndTitle(PENDING_TEACHER_REQUESTS,
            new SuccessClickHandler(this::showPendingTeachers)).setTeacher(true).setAdmin(true));
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

  /**
   * Add events and recordings choices.
   *
   * @return
   */
  List<LinkAndTitle> getProjectSpecificChoices() {
    List<LinkAndTitle> choices = new ArrayList<>();
    choices.add(new LinkAndTitle(getCapitalized(props.getNameForAnswer() + "s"),
        new SuccessClickHandler(() -> new ResultManager(
            props.getNameForAnswer(),
            lifecycleSupport.getProjectStartupInfo().getTypeOrder(),
            lifecycleSupport,
            controller).showResults()
        )
    ).setAdmin(true).setMustHaveProject(true));
    //  choices.add(new Banner.LinkAndTitle("Monitoring", new MonitoringClickHandler(), true));
    choices.add(new LinkAndTitle(EVENTS,
        new SuccessClickHandler(() -> new EventTable().show(lazyGetService(), controller.getMessageHelper()))).setAdmin(true).setMustHaveProject(true)
    );
    return choices;
  }

  private void addSendReport(List<LinkAndTitle> choices) {
    choices.add(new LinkAndTitle(SEND_REPORT, event -> {
      new ModalInfoDialog(STATUS_REPORT_BEING_GENERATED, IT_CAN_TAKE_AWHILE);
      sendReport();
    }).setAdmin(true));
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

  private LinkAndTitle teacherStatus;

  /**
   * @return
   * @seex #getStandardUserMenuChoices(List)
   * @see NewBanner#addUserMenu
   */
  List<LinkAndTitle> getStandardUserMenuChoices(List<LinkAndTitle> teacherReq) {
    List<LinkAndTitle> choices = new ArrayList<>();

    choices.add(getChangePassword());

    //   logger.info("getStandardUserMenuChoices...");

    LinkAndTitle maybeAddReqInstructor = maybeAddReqInstructor(choices);
    if (maybeAddReqInstructor != null) {
      teacherStatus = maybeAddReqInstructor;
      teacherReq.add(maybeAddReqInstructor);

      updatePendingTeacherTitleLater();
    }

//    if (isATeacherAndThereIsAProject(controller.getUserManager().getCurrent())) {
//      addPendingTeacher(choices);
//    }
    choices.add(getLogOut());

    return choices;
  }

  private void updatePendingTeacherTitleLater() {
    if (controller.getProjectID() > 0) {
      controller.getUserService().getPendingUsers(controller.getProjectID(), new AsyncCallback<List<ActiveUser>>() {
        @Override
        public void onFailure(Throwable caught) {

        }

        @Override
        public void onSuccess(List<ActiveUser> result) {
          // pendingTeachers.setTitle(PENDING_TEACHER_REQUESTS + " (" + result.size() + ")");
          boolean notPending = result.stream().noneMatch(activeUser -> activeUser.getID() == controller.getUser());
          boolean pending = !notPending;
          //logger.info("got not Pending " + notPending);
          if (pending) {
            teacherStatus.getMyLink().setActive(notPending);
            teacherStatus.getMyLink().setDisabled(pending);
            teacherStatus.setTitle(REQUEST_PENDING);
          }
        }
      });
    } else {
      logger.info("no project yet...");
    }
  }

  /**
   * @param size
   * @see NewBanner#setCogTitle
   */
  void setPendingTitle(int size) {
    logger.info("setPendingTitle " + size);
    pendingTeachers.setTitle(PENDING_TEACHER_REQUESTS + ((size == 0) ? "" : " (" + size + ")"));
  }

  /**
   * Only can add a instructor request in a project.
   *
   * @param choices
   * @return
   * @see #getStandardUserMenuChoices(List)
   */
  private LinkAndTitle maybeAddReqInstructor(List<LinkAndTitle> choices) {
    if (notATeacherButThereIsAProject(userManager.getCurrent())) {
      LinkAndTitle e = new LinkAndTitle(REQUEST_INSTRUCTOR_STATUS,
          new SuccessClickHandler(() -> new DialogHelper(true)
              .show(ARE_YOU_AN_INSTRUCTOR,
                  REQ_MESSAGE, new DialogHelper.CloseListener() {
                    @Override
                    public boolean gotYes() {
                      //logger.info("Sending request.");
                      controller.getUserService().sendTeacherRequest(new AsyncCallback<ActiveUser.PENDING>() {
                        @Override
                        public void onFailure(Throwable caught) {
                        }

                        @Override
                        public void onSuccess(ActiveUser.PENDING result) {
                          if (result == ActiveUser.PENDING.REQUESTED) {
                            new DialogHelper(false).show("Request Received",
                                Arrays.asList(
                                    "Your request has been received.",
                                    "Please wait for another instructor or administrator to approve your request.",
                                    "You will receive email when you are approved."),
                                getListener(),
                                "OK", "Cancel");
                          } else {
                            new DialogHelper(false).show("Request status is " + result,
                                Arrays.asList("Your request status is " + result + "."),
                                getListener(),
                                "OK", "Cancel");
                          }
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
                  }, "OK", "Cancel"))).setMustHaveProject(true);
      choices.add(e);

      return e;
    } else return null;
  }

  private boolean notATeacherButThereIsAProject(User current) {
    return current != null &&
        !current.isTeacher() &&
        !current.getPermissions().contains(Permission.TEACHER_PERM) &&
        controller.getProjectStartupInfo() != null;
  }

  private boolean isATeacherAndThereIsAProject(User current) {
    return current != null &&
        (current.isTeacher() ||
            current.getPermissions().contains(Permission.TEACHER_PERM)) &&
        controller.getProjectStartupInfo() != null;
  }

  @NotNull
  private DialogHelper.CloseListener getListener() {
    return new DialogHelper.CloseListener() {
      @Override
      public boolean gotYes() {
        teacherStatus.getMyLink().setActive(false);
        teacherStatus.getMyLink().setDisabled(true);
        teacherStatus.setTitle(REQUEST_PENDING);
        return true;
      }

      @Override
      public void gotNo() {

      }

      @Override
      public void gotHidden() {

      }
    };
  }

  @NotNull
  private LinkAndTitle getChangePassword() {
    return new LinkAndTitle(CHANGE_PASSWORD, new SuccessClickHandler(() ->
        new ChangePasswordView(userManager.getCurrent(), false, userState,
            controller.getProps(),
            controller.getUserService()).showModal()));
  }

  @NotNull
  private LinkAndTitle getLogOut() {
    return new LinkAndTitle(LOG_OUT, event -> uiLifecycle.logout());
  }

  public void setBanner(IBanner banner) {
    this.banner = banner;
  }

  public IBanner getBanner() {
    return banner;
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
    new ActiveUsersManager(controller).show(ACTIVE_USERS1, hours);
  }

  private void showActiveTeachers() {
    new ActiveUsersManager(controller) {
      protected void getUsers(int hours, DialogBox dialogBox, Panel dialogVPanel) {
        controller.getUserService().getActiveTeachers(new AsyncCallback<List<ActiveUser>>() {
          @Override
          public void onFailure(Throwable caught) {
            handleFailure(caught, GETTING_ACTIVE_USERS);
          }

          @Override
          public void onSuccess(List<ActiveUser> result) {
            gotUsers(result, dialogVPanel, dialogBox);
          }
        });
      }
    }.show(ACTIVE_TEACHERS, 0);
  }

  private void showAllTeachers() {
    new ActiveUsersManager(controller) {
      protected void getUsers(int hours, DialogBox dialogBox, Panel dialogVPanel) {
        controller.getUserService().getTeachers(new AsyncCallback<List<ActiveUser>>() {
          @Override
          public void onFailure(Throwable caught) {
            handleFailure(caught, GETTING_ALL_TEACHERS);
          }

          @Override
          public void onSuccess(List<ActiveUser> result) {
            gotUsers(result, dialogVPanel, dialogBox);
          }
        });
      }
    }.show(ALL_TEACHERS, 0);
  }

  private void showPendingTeachers() {
    new PendingUsersManager(controller, getBanner()).show(PENDING_TEACHER_REQUESTS, 0);
  }

  private void handleFailure(Throwable caught, String msg) {
    controller.getMessageHelper().handleNonFatalError(msg, caught);
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
                    flexTable.setHTML(rowCount + 1, 0, NEED_HELP);
                    flexTable.setHTML(rowCount + 1, 1, " <a href='" + getMailTo() + "'>" + HELP_EMAIL + "</a>");
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
      props.remove("Built-By");
      props.remove(DOMINO_URL);
      props.remove("Specification-Title");

      if (!controller.getUserManager().isAdmin()) {
        props.remove("Specification-Version");
      }

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
