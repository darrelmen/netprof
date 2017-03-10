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
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.custom.Navigation;
import mitll.langtest.client.download.DownloadHelper;
import mitll.langtest.client.download.DownloadIFrame;
import mitll.langtest.client.flashcard.Banner;
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
 * <br/>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 2/23/16
 */
public class InitialUI {
  private final Logger logger = Logger.getLogger("InitialUI");

  private static final String NPF_CLASSROOM_URL = "https://np.ll.mit.edu/npfClassroom";

  public static final String ROOT_VERTICAL_CONTAINER = "root_vertical_container";
  protected static final String LOGIN = "Login";

  /**
   * How far to the right to shift the list of sites...
   *
   * @see #getLinksToSites()
   */
  private static final int LEFT_LIST_WIDTH = 20;//167;

  private static final String LANGTEST_IMAGES = "langtest/images/";
  private static final int NO_USER_INITIAL = -2;

  private final UserManager userManager;

  protected long lastUser = NO_USER_INITIAL;

  protected final LangTest langTest;
  protected final PropertyHandler props;

  protected final LangTestDatabaseAsync service = GWT.create(LangTestDatabase.class);

  private final Banner banner;

  protected Panel headerRow;
  protected Panel firstRow;
  private Navigation navigation;
  private final BrowserCheck browserCheck = new BrowserCheck();

  public InitialUI(LangTest langTest, UserManager userManager) {
    this.langTest = langTest;
    this.props = langTest.getProps();
    this.userManager = userManager;
    banner = new Banner(props);
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
    DivWidget w = new DivWidget();
    w.getElement().setId(DownloadIFrame.DOWNLOAD_AREA_ID);
    RootPanel.get().add(w);
    w.setVisible(false);
    return firstRow;
  }

  /**
   * @return
   * @see #populateRootPanel()
   */
  private Panel makeHeaderRow() {
    ClickHandler reload = new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        service.reloadExercises(new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {

          }

          @Override
          public void onSuccess(Void result) {
            Window.Location.reload();
          }
        });
      }
    };
    // logger.info("talks to domino " + props.talksToDomino());
    reload = (props.talksToDomino()) ? reload : null;
    Widget title = banner.makeNPFHeaderRow(props.getSplash(), props.isBeta(), getGreeting(),
        getReleaseStatus(),
        new LogoutClickHandler(),
        new UsersClickHandler(),
        new ResultsClickHandler(),
        new MonitoringClickHandler(),
        new EventsClickHandler(),
        reload,
        new DownloadContentsClickHandler());

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

  /**
   * @return
   */
  public boolean isRTL() {
    boolean b = navigation != null && navigation.isRTL();
    if (b) logger.info("content is RTL!");
    return b;
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
    verticalContainer.getElement().setId(ROOT_VERTICAL_CONTAINER);
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

    addResizeHandler();
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
    verticalContainer2.getElement().setId(ROOT_VERTICAL_CONTAINER);
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
       String siteChanged = site.equals("Mandarin") ? site.replaceAll("Mandarin", "CM") : site;
       Anchor w = new Anchor(site, NPF_CLASSROOM_URL + siteChanged);
      w.getElement().getStyle().setMarginRight(5, Style.Unit.PX);
      hp.add(w);
    }
    return hp;
  }

  private void addResizeHandler() {
    final InitialUI outer = this;
    Window.addResizeHandler(new ResizeHandler() {
      public void onResize(ResizeEvent event) {
        outer.onResize();
      }
    });
  }

  /**
   * @see #addResizeHandler
   */
  protected void onResize() {
    if (navigation != null) navigation.onResize();
    if (banner != null) {
      banner.onResize();
    }
  }

  /**
   * So we have three different views here : the login page, the reset password page, and the content developer
   * enabled feedback page.
   *
   * @return false if we didn't do either of the special pages and should do the normal navigation view
   * @see #populateRootPanel()
   */
  protected boolean showLogin() {
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
      banner.setCogVisible(false);
      return true;
    }
    //   logger.info("user is valid...");

    banner.setCogVisible(true);
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
    banner.setCogVisible(false);
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
    banner.setCogVisible(false);
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

    banner.setUserName(getGreeting());
    if (userID != lastUser) {
      configureUIGivenUser(userID);
      langTest.logEvent("No widget", "UserLogin", "N/A", "User Login by " + userID);
    } else {
      logger.info("ignoring got user for current user " + userID);
      if (navigation != null) navigation.showPreviouslySelectedTab();
    }
    if (userID > -1) {
      banner.setCogVisible(true);
      banner.setVisibleAdmin(user.isAdmin() || props.isAdminView() || user.isTeacher() || user.isCD());
    }
  }

  /**
   * @param userID
   * @return
   * @see #gotUser
   */
  protected void configureUIGivenUser(long userID) {
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
      // logger.info("populateRootPanelIfLogin found : '" + as.getExID() +"'");

      if (as.getId().contains(LOGIN)) {
       // logger.info("populateRootPanelIfLogin found login...");
        populateRootPanel();
      }
    }
  }

  protected void showUserPermissions(long userID) {
    lastUser = userID;
    banner.setBrowserInfo(langTest.getInfoLine());
    banner.reflectPermissions(langTest.getPermissions());
  }

  void setSplash() {
    banner.setSubtitle();
  }
}
