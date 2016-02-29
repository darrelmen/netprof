package mitll.langtest.client;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.amas.AutoCRTChapterNPFHelper;
import mitll.langtest.client.custom.Navigation;
import mitll.langtest.client.flashcard.Flashcard;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.client.instrumentation.EventTable;
import mitll.langtest.client.monitoring.MonitoringManager;
import mitll.langtest.client.result.ResultManager;
import mitll.langtest.client.user.ResetPassword;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.client.user.UserPassLogin;
import mitll.langtest.client.user.UserTable;
import mitll.langtest.shared.User;

import java.util.logging.Logger;

/**
 * Created by go22670 on 2/23/16.
 */
public class InitialUI {
  private final Logger logger = Logger.getLogger("InitialUI");

  public static final String LOGIN = "Login";

  /**
   * How far to the right to shift the list of sites...
   *
   * @see #getLinksToSites()
   */
  private static final int LEFT_LIST_WIDTH = 267;

  private static final String LANGTEST_IMAGES = "langtest/images/";
  private static final int NO_USER_INITIAL = -2;

  private final UserManager userManager;

  protected long lastUser = NO_USER_INITIAL;

  protected final LangTest langTest;
  protected final PropertyHandler props;
  protected AutoCRTChapterNPFHelper learnHelper;

  protected final LangTestDatabaseAsync service = GWT.create(LangTestDatabase.class);

  private final Flashcard flashcard;

  protected Panel headerRow;
  protected Panel firstRow;
  private Navigation navigation;
  private final BrowserCheck browserCheck = new BrowserCheck();

  public InitialUI(LangTest langTest, UserManager userManager) {
    this.langTest = langTest;
    this.props = langTest.getProps();
    this.userManager = userManager;
    flashcard = new Flashcard(props);
  }

  private Container verticalContainer;

  /**
   * @see LangTest#showLogin()
   * @see LangTest#populateRootPanel()
   */
  void populateRootPanel() {
    Container verticalContainer = getRootContainer();
    this.verticalContainer = verticalContainer;
    // header/title line
    // first row ---------------
    Panel firstRow = makeFirstTwoRows(verticalContainer);

    if (!showLogin()) {
     // logger.info("not show login -");
      populateBelowHeader(verticalContainer, firstRow);
    }
  }

  /**
   * @param verticalContainer
   * @return
   * @see #populateRootPanel()
   */
  protected Panel makeFirstTwoRows(Container verticalContainer) {
    verticalContainer.add(headerRow = makeHeaderRow());
    headerRow.getElement().setId("headerRow");

    Panel firstRow = new DivWidget();
    verticalContainer.add(firstRow);
    this.firstRow = firstRow;
    firstRow.getElement().setId("firstRow");
    return firstRow;
  }

  /**
   * @return
   * @see #populateRootPanel()
   */
  private Panel makeHeaderRow() {
    Widget title = flashcard.makeNPFHeaderRow(props.getSplash(), true, getGreeting(),
        getReleaseStatus(),
        new LogoutClickHandler(),
        new UsersClickHandler(),
        new ResultsClickHandler(),
        new MonitoringClickHandler(),
        new EventsClickHandler()
    );

    headerRow = new FluidRow();
    headerRow.add(new Column(12, title));
    return headerRow;
  }


  /**
   * @return
   * @see #gotUser
   * @see #makeHeaderRow()
   */
  private String getGreeting() {
    return userManager.getUserID() == null ? "" : ("" + userManager.getUserID());
  }


  /**
   * @return
   * @see #makeHeaderRow()
   */
  private HTML getReleaseStatus() {
    browserCheck.getBrowserAndVersion();
    return new HTML(langTest.getInfoLine());
  }

  private class LogoutClickHandler implements ClickHandler {
    public void onClick(ClickEvent event) {
      langTest.logEvent("No widget", "UserLoging", "N/A", "User Logout by " + lastUser);
      resetState();
    }
  }

  /**
   * @seex mitll.langtest.client.LangTest.LogoutClickHandler#onClick(com.google.gwt.event.dom.client.ClickEvent)
   */
  private void resetState() {
    History.newItem(""); // clear history!
    userManager.clearUser();
    lastUser = NO_USER_INITIAL;
    langTest.modeSelect();
  }

  private class UsersClickHandler implements ClickHandler {
    public void onClick(ClickEvent event) {
      GWT.runAsync(new RunAsyncCallback() {
        public void onFailure(Throwable caught) {
          downloadFailedAlert();
        }

        public void onSuccess() {
          new UserTable(props, userManager.isAdmin()).showUsers(service);
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

  private class ResultsClickHandler implements ClickHandler {
    final EventRegistration outer = langTest;

    public void onClick(ClickEvent event) {
      GWT.runAsync(new RunAsyncCallback() {
        public void onFailure(Throwable caught) {
          downloadFailedAlert();
        }

        public void onSuccess() {
          ResultManager resultManager = new ResultManager(service, props.getNameForAnswer(),
              langTest.getStartupInfo().getTypeOrder(), outer, langTest);
          resultManager.showResults();
        }
      });
    }
  }

  private class MonitoringClickHandler implements ClickHandler {
    public void onClick(ClickEvent event) {
      GWT.runAsync(new RunAsyncCallback() {
        public void onFailure(Throwable caught) {
          downloadFailedAlert();
        }

        public void onSuccess() {
          new MonitoringManager(service, props).showResults();
        }
      });
    }
  }

  private void downloadFailedAlert() {
    Window.alert("Code download failed");
  }

  /**
   * @return
   */
  protected Container getRootContainer() {
   // logger.info("getRootContainer");
    RootPanel.get().clear();   // necessary?

    Container verticalContainer = new FluidContainer();
    verticalContainer.getElement().setId("root_vertical_container");
    return verticalContainer;
  }

  public int getHeightOfTopRows() {
    return headerRow.getOffsetHeight();
  }

  /**
   * * TODO : FIX ME for headstart
   *
   * @param verticalContainer
   * @param firstRow          where we put the flash permission window if it gets shown
   * @seex #handleCDToken(com.github.gwtbootstrap.client.ui.Container, com.google.gwt.user.client.ui.Panel, String, String)
   * @see #populateRootPanel()
   * @see #showLogin()
   */
  protected void populateBelowHeader(Container verticalContainer, Panel firstRow) {
    if (showOnlyOneExercise()) {
      Panel currentExerciseVPanel = new FlowPanel();
      currentExerciseVPanel.getElement().setId("currentExercisePanel");
      logger.info("adding headstart current exercise");
      RootPanel.get().add(getHeadstart(currentExerciseVPanel));
    } else {
      //  logger.info("adding normal container...");
      RootPanel.get().add(verticalContainer);

      /**
       * {@link #makeFlashContainer}
       */
      firstRow.add(langTest.getFlashRecordPanel());
    }
    langTest.modeSelect();

    navigation = new Navigation(service, userManager, langTest, langTest);

    firstRow.add(navigation.getTabPanel());
    verticalContainer.add(getLinksToSites());
  }

  /**
   * @return
   * @see #populateRootPanel()
   * @see mitll.langtest.client.scoring.ScoringAudioPanel#ScoringAudioPanel
   */
  private boolean showOnlyOneExercise() {
    return props.getExercise_title() != null;
  }

  /**
   * @param currentExerciseVPanel
   * @return
   * @see #populateBelowHeader
   */
  private Container getHeadstart(Panel currentExerciseVPanel) {
    // show fancy lace background image
    currentExerciseVPanel.addStyleName("body");
    currentExerciseVPanel.getElement().getStyle().setBackgroundImage("url(" + LANGTEST_IMAGES + "levantine_window_bg.jpg" + ")");
    currentExerciseVPanel.addStyleName("noMargin");

    Container verticalContainer2 = new FluidContainer();
    verticalContainer2.getElement().setId("root_vertical_container");
    verticalContainer2.add(langTest.getFlashRecordPanel());
    verticalContainer2.add(currentExerciseVPanel);
    return verticalContainer2;
  }

  private Panel getLinksToSites() {
    Panel hp = new HorizontalPanel();
    Style style = hp.getElement().getStyle();
    style.setMarginLeft(LEFT_LIST_WIDTH, Style.Unit.PX);
    style.setMarginTop(10, Style.Unit.PX);

    for (String site : props.getSites()) {
      Anchor w = new Anchor(site, "https://np.ll.mit.edu/npfClassroom" + site.replaceAll("Mandarin", "CM"));
      w.getElement().getStyle().setMarginRight(5, Style.Unit.PX);
      hp.add(w);
    }
    return hp;
  }

  public void onResize() {
    if (navigation != null) navigation.onResize();
    if (flashcard != null) {
      flashcard.onResize();
    }
  }

  /**
   * So we have three different views here : the login page, the reset password page, and the content developer
   * enabled feedback page.
   *
   * @return false if we didn't do either of the special pages and should do the normal navigation view
   * @see #populateRootPanel()
   */
  public boolean showLogin() {
    final EventRegistration eventRegistration = langTest;

    // check if we're here as a result of resetting a password
    final String resetPassToken = props.getResetPassToken();
    if (!resetPassToken.isEmpty()) {
      handleResetPass(verticalContainer, firstRow, eventRegistration, resetPassToken);
      return true;
    }

    // are we here to enable a CD user?
    final String cdToken = props.getCdEnableToken();
    if (!cdToken.isEmpty()) {
      logger.info("showLogin token '" + resetPassToken + "' for enabling cd user");

      handleCDToken(verticalContainer, firstRow, cdToken, props.getEmailRToken());
      return true;
    }

    // are we here to show the login screen?
    boolean show = userManager.isUserExpired() || userManager.getUserID() == null;
    if (show) {
      logger.info("user is not valid : user expired " + userManager.isUserExpired() + " / " + userManager.getUserID());

      firstRow.add(new UserPassLogin(service, props, userManager, eventRegistration).getContent());
      clearPadding(verticalContainer);
      RootPanel.get().add(verticalContainer);
      flashcard.setCogVisible(false);
      return true;
    }
    //   logger.info("user is valid...");

    flashcard.setCogVisible(true);
    return false;
  }

  private void handleResetPass(final Container verticalContainer, final Panel firstRow,
                               final EventRegistration eventRegistration, final String resetPassToken) {
    //logger.info("showLogin token '" + resetPassToken + "' for password reset");
    service.getUserIDForToken(resetPassToken, new AsyncCallback<Long>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Long result) {
        if (result == null || result < 0) {
          logger.info("token '" + resetPassToken + "' is stale. Showing normal view");
          trimURL();
          populateBelowHeader(verticalContainer, firstRow);
        } else {
          firstRow.add(new ResetPassword(service, props, eventRegistration).getResetPassword(resetPassToken));
          clearPadding(verticalContainer);
        }
      }
    });

    RootPanel.get().add(verticalContainer);
    flashcard.setCogVisible(false);
  }

  /**
   * Reload window after showing content developer has been approved.
   * <p>
   * Mainly something Tamas would see.
   *
   * @param verticalContainer
   * @param firstRow
   * @param cdToken
   * @param emailR
   */
  private void handleCDToken(final Container verticalContainer, final Panel firstRow, final String cdToken, String emailR) {
    logger.info("enabling token " + cdToken + " for email " + emailR);
    service.enableCDUser(cdToken, emailR, Window.Location.getHref(), new AsyncCallback<String>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(String result) {
        if (result == null) {
          logger.info("handleCDToken enable - token " + cdToken + " is stale. Showing normal view");
          trimURL();
          populateBelowHeader(verticalContainer, firstRow);
        } else {
          firstRow.add(new Heading(2, "OK, content developer <u>" + result + "</u> has been approved."));
          firstRow.addStyleName("leftFiveMargin");
          clearPadding(verticalContainer);
          trimURLAndReload();
        }
      }
    });

    RootPanel.get().add(verticalContainer);
    flashcard.setCogVisible(false);
  }

  private void trimURLAndReload() {
    Timer t = new Timer() {
      @Override
      public void run() {
        trimURL();
        Window.Location.reload();
      }
    };
    t.schedule(3000);
  }

  private void trimURL() {
    Window.Location.replace(trimURL(Window.Location.getHref()));
  }

  private void clearPadding(Container verticalContainer) {
    verticalContainer.getElement().getStyle().setPaddingLeft(0, Style.Unit.PX);
    verticalContainer.getElement().getStyle().setPaddingRight(0, Style.Unit.PX);
  }

  private String trimURL(String url) {
    if (url.contains("127.0.0.1")) {
      return "http://127.0.0.1:8888/LangTest.html?gwt.codesvr=127.0.0.1:9997";
    } else return url.split("\\?")[0].split("\\#")[0];
  }

  /**
   * Init Flash recorder once we login.
   * <p>
   * Only get the exercises if the user has accepted mic access.
   *
   * @param user
   * @see LangTest#makeFlashContainer
   * @see UserManager#gotNewUser(mitll.langtest.shared.User)
   * @see UserManager#storeUser
   */
  public void gotUser(User user) {
    populateRootPanelIfLogin();
    long userID = -1;
    if (user != null) {
      userID = user.getId();
    }

    // logger.info("gotUser : userID " + userID);

    flashcard.setUserName(getGreeting());
    if (userID != lastUser) {
      configureUIGivenUser(userID);
      langTest.logEvent("No widget", "UserLogin", "N/A", "User Login by " + userID);
    } else {
      logger.info("ignoring got user for current user " + userID);
      if (navigation != null) navigation.showPreviouslySelectedTab();
    }
    if (userID > -1) {
      flashcard.setCogVisible(true);
      flashcard.setVisibleAdmin(user.isAdmin() || props.isAdminView() || user.isTeacher() || user.isCD());
    }
  }

  /**
   * @param userID
   * @return
   * @see #gotUser
   */
  public void configureUIGivenUser(long userID) {
//    logger.info("configureUIGivenUser : user changed - new " + userID + " vs last " + lastUser +
//        " audio type " + getAudioType() + " perms " + getPermissions());
    populateRootPanelIfLogin();
//    logger.info("\tconfigureUIGivenUser : " + userID + " get exercises...");
    navigation.showInitialState();
    showUserPermissions(userID);
  }

  /**
   * @see #gotUser
   * @see #configureUIGivenUser(long) (long)
   */
  protected void populateRootPanelIfLogin() {
    int childCount = firstRow.getElement().getChildCount();

    //logger.info("populateRootPanelIfLogin root " + firstRow.getElement().getNodeName() + " childCount " + childCount);
    if (childCount > 0) {
      Node child = firstRow.getElement().getChild(0);
      Element as = Element.as(child);
      logger.info("populateRootPanelIfLogin found : '" + as.getId() +"'");

      if (as.getId().contains(LOGIN)) {
        logger.info("populateRootPanelIfLogin found login...");
        populateRootPanel();
      }
    }
  }

  protected void showUserPermissions(long userID) {
    lastUser = userID;
    flashcard.setBrowserInfo(langTest.getInfoLine());
    flashcard.reflectPermissions(langTest.getPermissions());
  }

  public void setSplash() {
    flashcard.setSplash();
  }
}
