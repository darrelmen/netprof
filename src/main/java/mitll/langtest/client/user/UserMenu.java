package mitll.langtest.client.user;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabase;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.LifecycleSupport;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.UILifecycle;
import mitll.langtest.client.domino.user.ChangePasswordView;
import mitll.langtest.client.download.DownloadHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.flashcard.Banner;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.client.instrumentation.EventTable;
import mitll.langtest.client.result.ResultManager;
import mitll.langtest.client.services.UserService;
import mitll.langtest.client.services.UserServiceAsync;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by go22670 on 1/8/17.
 */
public class UserMenu {
 private final Logger logger = Logger.getLogger("InitialUI");

  public static final String ROOT_VERTICAL_CONTAINER = "root_vertical_container";

  protected static final String LOGIN = "Login";
  private static final int NO_USER_INITIAL = -2;
  private final UserManager userManager;


  protected long lastUser = NO_USER_INITIAL;

  private final LifecycleSupport lifecycleSupport;
  private final ExerciseController controller;
  private final UserState userState;
  private final PropertyHandler props;

  private final LangTestDatabaseAsync service = GWT.create(LangTestDatabase.class);
  private final UserServiceAsync userService = GWT.create(UserService.class);

  protected Panel headerRow;
  protected Panel contentRow;
  private static final String LOG_OUT = "Log Out";
  private final UILifecycle uiLifecycle;

  /**
   * @param langTest
   * @param userManager
   * @see LangTest#onModuleLoad2()
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

  public List<Banner.LinkAndTitle> getCogMenuChoices() {
    List<Banner.LinkAndTitle> choices = new ArrayList<>();
    choices.add(new Banner.LinkAndTitle("Users", new UsersClickHandler(), true));
    Banner.LinkAndTitle e = new Banner.LinkAndTitle("Manage Users", null, true);
    e.setLinkURL(props.getDominoURL());
    choices.add(e);

    String nameForAnswer = props.getNameForAnswer() + "s";
    choices.add(new Banner.LinkAndTitle(
        nameForAnswer.substring(0, 1).toUpperCase() + nameForAnswer.substring(1), new ResultsClickHandler(), true));
  //  choices.add(new Banner.LinkAndTitle("Monitoring", new MonitoringClickHandler(), true));
    choices.add(new Banner.LinkAndTitle("Events", new EventsClickHandler(), true));
    choices.add(new Banner.LinkAndTitle("Change Password", new ChangePasswordClickHandler(), false));
    choices.add(new Banner.LinkAndTitle("Download Context", new DownloadContentsClickHandler(), true));
    choices.add(new Banner.LinkAndTitle(LOG_OUT, new LogoutClickHandler(), false));
    return choices;
  }

  private class UsersClickHandler implements ClickHandler {
    public void onClick(ClickEvent event) {
      GWT.runAsync(new RunAsyncCallback() {
        public void onFailure(Throwable caught) {
          downloadFailedAlert();
        }
        public void onSuccess() {
          new UserTable(props, userManager.isAdmin()).showUsers(userService);
        }
      });
    }
  }
  private class ManageUsersClickHandler implements ClickHandler {
    public void onClick(ClickEvent event) {
      GWT.runAsync(new RunAsyncCallback() {
        public void onFailure(Throwable caught) {
          downloadFailedAlert();
        }
        public void onSuccess() {
          new UserTable(props, userManager.isAdmin()).showUsers(userService);
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
          new EventTable().show(service);
        }
      });
    }
  }

  private class ChangePasswordClickHandler implements ClickHandler {
    //UserState outer = LangTest.this;
    public void onClick(ClickEvent event) {
      GWT.runAsync(new RunAsyncCallback() {
        public void onFailure(Throwable caught) {
          downloadFailedAlert();
        }

        public void onSuccess() {
          new ChangePasswordView(userManager.getCurrent(), false, userState, userService).showModal();
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
          new DownloadHelper(null).downloadContext();
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
              // resultService,
              props.getNameForAnswer(),
              lifecycleSupport.getProjectStartupInfo().getTypeOrder(),
              outer,
              controller);
          resultManager.showResults();
        }
      });
    }
  }
/*
  private class MonitoringClickHandler implements ClickHandler {
    public void onClick(ClickEvent event) {
      GWT.runAsync(new RunAsyncCallback() {
        public void onFailure(Throwable caught) {
          downloadFailedAlert();
        }

        public void onSuccess() {
          new MonitoringManager(props).showResults();
        }
      });
    }
  }*/
  private class LogoutClickHandler implements ClickHandler {
    public void onClick(ClickEvent event) {
      uiLifecycle.logout();
    }
  }

  private void downloadFailedAlert() {
    Window.alert("Code download failed");
  }

}
