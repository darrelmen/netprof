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
import mitll.langtest.client.LangTest;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.domino.user.ChangePasswordView;
import mitll.langtest.client.download.DownloadHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.initial.InitialUI;
import mitll.langtest.client.initial.LifecycleSupport;
import mitll.langtest.client.initial.PropertyHandler;
import mitll.langtest.client.initial.UILifecycle;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.client.instrumentation.EventTable;
import mitll.langtest.client.recorder.FlashRecordPanelHeadless;
import mitll.langtest.client.result.ResultManager;
import mitll.langtest.client.services.LangTestDatabase;
import mitll.langtest.client.services.LangTestDatabaseAsync;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.client.user.UserState;
import mitll.langtest.client.user.UserTable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by go22670 on 1/8/17.
 */
public class UserMenu {
  private final Logger logger = Logger.getLogger("UserMenu");

  private static final String ABOUT_NET_PRO_F = "About Netprof";
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
    choices.add(new LinkAndTitle("Manage Users", props.getDominoURL()));
    //choices.add(new LinkAndTitle("Users", new UsersClickHandler(), true));
    return choices;
  }

  List<LinkAndTitle> getProjectSpecificChoices() {
    List<LinkAndTitle> choices = new ArrayList<>();
    String nameForAnswer = props.getNameForAnswer() + "s";
    choices.add(new LinkAndTitle(getCapitalized(nameForAnswer), new ResultsClickHandler()));
    //  choices.add(new Banner.LinkAndTitle("Monitoring", new MonitoringClickHandler(), true));
    choices.add(new LinkAndTitle("Events", new EventsClickHandler()));
    choices.add(new LinkAndTitle("Download Context", new DownloadContentsClickHandler()));
    choices.add(new LinkAndTitle("Send Report", event -> lazyGetService().sendReport(new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Void result) {
        new ModalInfoDialog("Status report sent", "Please check your email.");
      }
    })));
    choices.add(new LinkAndTitle("Show Report", "scoreServlet?report"));
    return choices;
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
    return new LinkAndTitle(LOG_OUT, new LogoutClickHandler());
  }

  private class UsersClickHandler implements ClickHandler {
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
  }

  private class EventsClickHandler implements ClickHandler {
    public void onClick(ClickEvent event) {
      GWT.runAsync(new RunAsyncCallback() {
        public void onFailure(Throwable caught) {
          downloadFailedAlert();
        }

        public void onSuccess() {
          new EventTable().show(lazyGetService());
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

  private class DownloadContentsClickHandler implements ClickHandler {
    public void onClick(ClickEvent event) {
      GWT.runAsync(new RunAsyncCallback() {
        public void onFailure(Throwable caught) {
          downloadFailedAlert();
        }

        public void onSuccess() {
          new DownloadHelper().downloadContext(controller.getHost());
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

  private class LogoutClickHandler implements ClickHandler {
    public void onClick(ClickEvent event) {
      uiLifecycle.logout();
    }
  }

  private void downloadFailedAlert() {
    Window.alert("Code download failed");
  }

  public NavLink getAbout() {
    NavLink about = new NavLink(ABOUT_NET_PRO_F);
    about.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent clickEvent) {
        Map<String, String> props = UserMenu.this.props.getProps();
        List<String> strings = new ArrayList<>();
        try {
          String recordingInfo = FlashRecordPanelHeadless.usingWebRTC() ? " Browser recording" : "Flash recording";
          props.put("Recording type", recordingInfo);
          props.remove("domino.url");
          Optional<String> max = props.keySet().stream().max(Comparator.comparingInt(String::length));
          if (max.isPresent()) {
            int maxl = max.get().length();
            props.keySet().forEach(key -> {
                  strings.add(key + getLen(maxl - key.length()));
                }
            );
          }
        } catch (Exception e) {
          //logger.warning("got " + e);
        }

        new ModalInfoDialog(ABOUT_NET_PRO_F, strings, props.values(), null, null, false, true, 600, 400) {
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
