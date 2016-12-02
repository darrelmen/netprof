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
import com.google.gwt.user.client.ui.Image;
import mitll.langtest.client.custom.Navigation;
import mitll.langtest.client.domino.user.ChangePasswordView;
import mitll.langtest.client.download.DownloadIFrame;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.flashcard.Banner;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.client.instrumentation.EventTable;
import mitll.langtest.client.monitoring.MonitoringManager;
import mitll.langtest.client.result.ResultManager;
import mitll.langtest.client.services.UserService;
import mitll.langtest.client.services.UserServiceAsync;
import mitll.langtest.client.user.*;
import mitll.langtest.shared.project.ProjectStartupInfo;
import mitll.langtest.shared.user.SlimProject;
import mitll.langtest.shared.user.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;


/**
 * <br/>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 2/23/16
 */
public class InitialUI implements UILifecycle {
  private final Logger logger = Logger.getLogger("InitialUI");

  public static final String ROOT_VERTICAL_CONTAINER = "root_vertical_container";


  /**
   * Tamas doesn't like scrolling -- try to prevent it on laptops
   */
  private static final int ITEMS_IN_ROW = 5;

  protected static final String LOGIN = "Login";

  private static final String LANGTEST_IMAGES = LangTest.LANGTEST_IMAGES;
  private static final int NO_USER_INITIAL = -2;

  private final UserManager userManager;

  /**
   * @see #configureUIGivenUser
   * @see #gotUser
   * @see #lastUser
   * @see #resetState
   * @see #showUserPermissions
   *
   */
  protected long lastUser = NO_USER_INITIAL;

  protected final LifecycleSupport lifecycleSupport;
  protected final ExerciseController controller;
  protected final UserFeedback userFeedback;
  protected final UserState userState;
  private final UserNotification userNotification;
  protected final PropertyHandler props;

  protected final LangTestDatabaseAsync service = GWT.create(LangTestDatabase.class);
  private final UserServiceAsync userService = GWT.create(UserService.class);

  private final Banner banner;

  protected Panel headerRow;
  private Breadcrumbs breadcrumbs;
  protected Panel contentRow;
  private Navigation navigation;
  private final BrowserCheck browserCheck = new BrowserCheck();
  private Container verticalContainer;
  private static final boolean DEBUG = false;

  /**
   * @param langTest
   * @param userManager
   * @see LangTest#onModuleLoad2()
   */
  public InitialUI(LangTest langTest, UserManager userManager) {
    this.lifecycleSupport = langTest;
    this.userState = langTest;
    this.props = langTest.getProps();
    this.userManager = userManager;
    this.controller = langTest;
    userFeedback = langTest;
    this.userNotification = langTest;
    banner = new Banner(props, userService, langTest, langTest);
  }

  /**
   * @see LangTest#showLogin()
   * @see LangTest#populateRootPanel()
   */
  @Override
  public void populateRootPanel() {
  //  logger.info("----> populateRootPanel BEGIN ------>");
    Container verticalContainer = getRootContainer();
    this.verticalContainer = verticalContainer;
    // header/title line
    // first row ---------------
    makeFirstTwoRows(verticalContainer);

    if (!showLogin()) {
      populateBelowHeader(verticalContainer);
    }
//    logger.info("----> populateRootPanel END   ------>");
  }

  /**
   * @param verticalContainer
   * @return
   * @see #populateRootPanel()
   */
  protected Panel makeFirstTwoRows(Container verticalContainer) {
    // add header row
    verticalContainer.add(headerRow = makeHeaderRow());
    headerRow.getElement().setId("headerRow");

    Panel contentRow = new DivWidget();
    contentRow.getElement().setId("contentRow");

    verticalContainer.add(contentRow);
    this.contentRow = contentRow;

    DivWidget w = new DivWidget();
    w.getElement().setId(DownloadIFrame.DOWNLOAD_AREA_ID);
    RootPanel.get().add(w);
    w.setVisible(false);

    return contentRow;
  }

  private static final String LOG_OUT = "Log Out";

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

    List<Banner.LinkAndTitle> choices = getCogMenuChoices();

    Widget bannerRow = banner.makeNPFHeaderRow(props.getSplash(), props.isBeta(), getGreeting(),
        getReleaseStatus(),
        choices
    );

    headerRow = new FluidRow();
    headerRow.add(new Column(12, bannerRow));
    return headerRow;
  }

  private List<Banner.LinkAndTitle> getCogMenuChoices() {
    List<Banner.LinkAndTitle> choices = new ArrayList<>();
    choices.add(new Banner.LinkAndTitle("Users", new UsersClickHandler(), true));
    String nameForAnswer = props.getNameForAnswer() + "s";
    choices.add(new Banner.LinkAndTitle(
        nameForAnswer.substring(0, 1).toUpperCase() + nameForAnswer.substring(1), new ResultsClickHandler(), true));
    choices.add(new Banner.LinkAndTitle("Monitoring", new MonitoringClickHandler(), true));
    choices.add(new Banner.LinkAndTitle("Events", new EventsClickHandler(), true));
    choices.add(new Banner.LinkAndTitle("Change Password", new ChangePasswordClickHandler(), false));
    choices.add(new Banner.LinkAndTitle(LOG_OUT, new LogoutClickHandler(), false));
    return choices;
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
    return new HTML(lifecycleSupport.getInfoLine());
  }


  @Override
  public boolean isRTL() {
    boolean b = controller.getProps().isRightAlignContent();//navigation != null && navigation.isRTL();
  //  if (b) logger.info("content is RTL!");
    return b;
  }

  private class LogoutClickHandler implements ClickHandler {
    public void onClick(ClickEvent event) {
      logout();
    }
  }

  public void logout() {
    lifecycleSupport.logEvent("No widget", "UserLoging", "N/A", "User Logout by " + lastUser);
    verticalContainer.remove(breadcrumbs);

    userService.logout(userManager.getUserID(), new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable throwable) {
      }

      @Override
      public void onSuccess(Void aVoid) {
        resetState();
      }
    });
  }

  /**
   * @seex mitll.langtest.client.LangTest.LogoutClickHandler#onClick(com.google.gwt.event.dom.client.ClickEvent)
   */
  private void resetState() {
    History.newItem(""); // clear history!
    userManager.clearUser();
    lastUser = NO_USER_INITIAL;
    lifecycleSupport.recordingModeSelect();
    clearContent();
  }

  /**
   * @see #getBreadcrumbs()
   */
  private void clearContent() {
    clearStartupInfo();
    contentRow.clear();
    contentRow.add(lifecycleSupport.getFlashRecordPanel()); // put back record panel
  }

  private void clearStartupInfo() {
    lifecycleSupport.clearStartupInfo();
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

  private class ResultsClickHandler implements ClickHandler {
    final EventRegistration outer = lifecycleSupport;

    public void onClick(ClickEvent event) {
      GWT.runAsync(new RunAsyncCallback() {
        public void onFailure(Throwable caught) {
          downloadFailedAlert();
        }

        public void onSuccess() {
          ResultManager resultManager = new ResultManager(
              service,
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
  }

  private void downloadFailedAlert() {
    Window.alert("Code download failed");
  }

  /**
   * @return
   */
  protected Container getRootContainer() {
    RootPanel.get().clear();   // necessary?
    Container verticalContainer = new FluidContainer();
    verticalContainer.getElement().setId(ROOT_VERTICAL_CONTAINER);
    return verticalContainer;
  }

  @Override
  public int getHeightOfTopRows() {
    return headerRow.getOffsetHeight();
  }

  /**
   * * TODO : FIX ME for headstart?
   *
   * @param verticalContainer
   * @paramx contentRow        where we put the flash permission window if it gets shown
   * @seex #handleCDToken(com.github.gwtbootstrap.client.ui.Container, com.google.gwt.user.client.ui.Panel, String, String)
   * @see #populateRootPanel()
   * @see #showLogin()
   */
  protected void populateBelowHeader(Container verticalContainer) {
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
      contentRow.add(lifecycleSupport.getFlashRecordPanel());
    }
   // logger.info("populateBelowHeader -- ");
    lifecycleSupport.recordingModeSelect();
    makeNavigation();
    addResizeHandler();
  }

  /**
   * Breadcrumb shows sequence of choices - who - languge - course (FL100 or FL200 or FL300) Elementary FL/Intermediate FL/Advanced FL - what do you want to do
   *
   * @see #configureUIGivenUser(long)
   * @see #gotUser(User)
   * @see #setProjectForUser(int)
   */
  private void showNavigation() {
    if (contentRow.getElement().getChildCount() == 1) {
     // logger.info("showNavigation : - add to content root");
      contentRow.add(navigation.getTabPanel());
    } else {
      logger.info("showNavigation : first row has " + contentRow.getElement().getChildCount() + " child(ren) - not adding tab panel???");
    }
  }

  private Breadcrumbs getBreadcrumbs() {
    //   logger.info("getBreadcrumbs --->");
    Breadcrumbs crumbs = new Breadcrumbs(">");
    crumbs.getElement().setId("breadcrumb");
    crumbs.getElement().getStyle().setMarginBottom(0, Style.Unit.PX);
    addCrumbs(crumbs);
    // logger.info("getBreadcrumbs now has " + crumbs.getElement().getChildCount() + " links");

    return crumbs;
  }

  /**
   * @param crumbs
   * @see #clickOnUserCrumb()
   * @see #getBreadcrumbs()
   */
  private void addCrumbs(Breadcrumbs crumbs) {
    NavLink home = new NavLink("Home");

    home.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent clickEvent) {
        logout();
      }
    });
    crumbs.add(home);
    // logger.info("\tgetBreadcrumbs add home");

    User current = userManager.getCurrent();
    if (current != null) {
      NavLink me = new NavLink(current.getUserID());
      me.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent clickEvent) {
          clickOnUserCrumb();
        }
      });
      crumbs.add(me);
      // logger.info("getBreadcrumbs adding step for current user " + current);

      ProjectStartupInfo startupInfo = lifecycleSupport.getProjectStartupInfo();
      if (startupInfo != null) {
        int currentProject = startupInfo.getProjectid();
        for (SlimProject project : lifecycleSupport.getStartupInfo().getProjects()) {
          if (project.hasChildren() && project.hasChild(currentProject)) {
            String crumbName = project.getLanguage();
            NavLink lang = new NavLink(crumbName);

            crumbs.add(lang);
            //   logger.info("getBreadcrumbs adding step for " + lang);

            SlimProject child = project.getChild(currentProject);
            /*final NavLink projectCrumb =*/
            addProjectCrumb(crumbs, child);

            lang.addClickHandler(new ClickHandler() {
              @Override
              public void onClick(ClickEvent clickEvent) {
                clearStartupInfo();
                clearContent();
//                boolean remove = crumbs.remove(projectCrumb);
                removeUntilCrumb(2);
                // if (!remove) {
                //   logger.warning("didn't remove " + projectCrumb);
                // }
                showProjectChoices(project.getChildren(), 1);
              }
            });
          } else if (project.getProjectid() == currentProject) {
            addProjectCrumb(crumbs, project);
            break;
          } else {
            //logger.fine("getBreadcrumbs skipping project " + project);
          }
        }
      }
    } else {
      logger.info("getBreadcrumbs no current user --- ????");
    }
  }

  /**
   * @param crumbs
   * @param project
   * @return
   * @see #addCrumbs(Breadcrumbs)
   */
  private NavLink addProjectCrumb(Breadcrumbs crumbs, SlimProject project) {
    String crumbName = project.getName();
    NavLink lang = new NavLink(crumbName);
    lang.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent clickEvent) {
        //               breadcrumbs = getBreadcrumbs();
        //              crumbs.clear();
        //            clearStartupInfo();
      }
    });
    crumbs.add(lang);
 //   logger.info("getBreadcrumbs adding step for " + lang);
    return lang;
  }

  /**
   * @see #addCrumbs(Breadcrumbs)
   */
  private void clickOnUserCrumb() {
    userService.forgetProject(new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable throwable) {
      }

      @Override
      public void onSuccess(Void aVoid) {
      }
    });

    History.newItem("");
    clearStartupInfo();

    breadcrumbs.clear();
    addCrumbs(breadcrumbs);

    clearContent();
    addProjectChoices(0, null);
  }

  private void makeNavigation() {
    navigation = new Navigation(service, userManager, controller, userFeedback);
    banner.setNavigation(navigation);
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
    verticalContainer2.add(lifecycleSupport.getFlashRecordPanel());
    verticalContainer2.add(currentExerciseVPanel);
    return verticalContainer2;
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
    final EventRegistration eventRegistration = lifecycleSupport;

    // check if we're here as a result of resetting a password
    final String resetPassToken = props.getResetPassToken();
    if (!resetPassToken.isEmpty()) {
      handleResetPass(verticalContainer, contentRow, eventRegistration, resetPassToken);
      return true;
    }

    // are we here to enable a CD user?
    final String cdToken = props.getCdEnableToken();
    if (!cdToken.isEmpty()) {
      logger.info("showLogin token '" + resetPassToken + "' for enabling cd user");
      handleCDToken(verticalContainer, contentRow, cdToken, props.getEmailRToken());
      return true;
    }

    // are we here to show the login screen?
    boolean show = userManager.isUserExpired() || userManager.getUserID() == null;
    if (show) {
      logger.info("showLogin user is not valid : user expired " + userManager.isUserExpired() + " / " + userManager.getUserID());
      showLogin(eventRegistration);
      return true;
    }
    //   logger.info("user is valid...");

    banner.setCogVisible(true);
    return false;
  }

  private void showLogin(EventRegistration eventRegistration) {
    contentRow.add(new UserPassLogin(props, userManager, eventRegistration).getContent());
    clearPadding(verticalContainer);
    RootPanel.get().add(verticalContainer);
    banner.setCogVisible(false);
  }

  private void handleResetPass(final Container verticalContainer,
                               final Panel firstRow,
                               final EventRegistration eventRegistration,
                               final String resetPassToken) {
    //logger.info("showLogin token '" + resetPassToken + "' for password reset");
    userService.getUserIDForToken(resetPassToken, new AsyncCallback<Long>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Long result) {
        if (result == null || result < 0) {
          logger.info("token '" + resetPassToken + "' is stale. Showing normal view");
          trimURL();
          populateBelowHeader(verticalContainer);
        } else {
          firstRow.add(new ResetPassword(props, eventRegistration).getResetPassword(resetPassToken));
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
   * @deprecated
   */
  private void handleCDToken(final Container verticalContainer, final Panel firstRow, final String cdToken, String emailR) {
    logger.info("enabling token " + cdToken + " for email " + emailR);
    userService.enableCDUser(cdToken, emailR, Window.Location.getHref(), new AsyncCallback<String>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(String result) {
        if (result == null) {
          logger.info("handleCDToken enable - token " + cdToken + " is stale. Showing normal view");
          trimURL();
          populateBelowHeader(verticalContainer);
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

  private String trimURL(String url) {
    if (url.contains("127.0.0.1")) {
      return "http://127.0.0.1:8888/LangTest.html?gwt.codesvr=127.0.0.1:9997";
    } else return url.split("\\?")[0].split("\\#")[0];
  }

  private void clearPadding(Container verticalContainer) {
    verticalContainer.getElement().getStyle().setPaddingLeft(0, Style.Unit.PX);
    verticalContainer.getElement().getStyle().setPaddingRight(0, Style.Unit.PX);
  }

  /**
   * Init Flash recorder once we login.
   * <p>
   * Only get the exercises if the user has accepted mic access.
   *
   * @param user
   * @see LangTest#makeFlashContainer
   * @see UserManager#gotNewUser(User)
   * @see UserManager#storeUser
   */
  @Override
  public void gotUser(User user) {
    populateRootPanelIfLogin();
    long userID = -1;
    if (user != null) {
      userID = user.getID();
    }

    if (DEBUG) logger.info("gotUser : userID " + userID);

    banner.setUserName(getGreeting());
    if (userID != lastUser) {
      configureUIGivenUser(userID);
      lifecycleSupport.logEvent("No widget", "UserLogin", "N/A", "User Login by " + userID);
    } else {
      logger.info("ignoring got user for current user " + userID);
      if (navigation != null) {
        showNavigation();
        navigation.showPreviouslySelectedTab();
      } else {
        logger.warning("how can navigation be null????");
      }
    }
    if (userID > -1) {
      banner.setCogVisible(true);
      banner.setVisibleAdmin(
          user.isAdmin() ||
          props.isAdminView() ||
          user.getUserKind() == User.Kind.PROJECT_ADMIN ||
          user.isCD());
    }
  }

  /**
   * So we know who the user is... now decide what to show them next
   *
   * @param userID
   * @return
   * @see #gotUser
   */
  protected void configureUIGivenUser(long userID) {
    logger.info("configureUIGivenUser : user changed - new " + userID + " vs last " + lastUser);
    if (lifecycleSupport.getProjectStartupInfo() == null) {
      addProjectChoices(0, null);
    } else {
      logger.info("\tconfigureUIGivenUser : " + userID + " get exercises...");
      addBreadcrumbs();
      showNavigation();
      navigation.showInitialState();
    }
    showUserPermissions(userID);
  }

  /**
   * @see #configureUIGivenUser(long)
   * @see #clickOnUserCrumb()
   * @see #clickOnParentCrumb
   */
  private void addProjectChoices(int level, SlimProject parent) {
    clearContent();
    addBreadcrumbs();
    List<SlimProject> projects = parent == null ? lifecycleSupport.getStartupInfo().getProjects() : parent.getChildren();
  //  logger.info("addProjectChoices found " + projects.size() + " initial projects, nest " + level);
    showProjectChoices(projects, level);
  }

  /**
   * @see #addProjectChoices(int, SlimProject)
   */
  private void addBreadcrumbs() {
    int childCount = verticalContainer.getElement().getChildCount();
    boolean found = false;
    // logger.info("populateRootPanelIfLogin root " + contentRow.getElement().getNodeName() + " childCount " + childCount);
    if (childCount > 0) {
      for (int i = 0; i < childCount; i++) {
        Node child = verticalContainer.getElement().getChild(i);
        Element as = Element.as(child);
        String id = as.getId();

        if (id.equals("breadcrumb")) {
          found = true;
    //      logger.info("found " + id);
        }
      }
    }
    if (!found) {
      verticalContainer.insert(breadcrumbs = getBreadcrumbs(), 1);
    }
  }

  /**
   * @see #addProjectChoices(int, SlimProject)
   * @param result
   * @param nest
   */
  private void showProjectChoices(List<SlimProject> result, int nest) {
    logger.info("showProjectChoices " + result.size() + " : " + nest);

    final Container flags = new Container();
    final Section section = new Section("section");
    contentRow.add(section);

    DivWidget header = new DivWidget();
    header.addStyleName("container");
    String text = "Please select a language";
    if (nest == 1) {
      text = "Please select a course";
    }

    if (result.isEmpty()) {
      text = "No languages loaded yet. Please wait.";
    }
    Heading child = new Heading(3, text);
    header.add(child);
    child.getElement().getStyle().setMarginLeft(10, Style.Unit.PX);

    section.add(header);
    section.add(flags);

    Panel current = new Thumbnails();
    flags.add(current);
    int numInRow = ITEMS_IN_ROW;
    List<SlimProject> languages = new ArrayList<SlimProject>(result);
    Collections.sort(languages, new Comparator<SlimProject>() {
      @Override
      public int compare(SlimProject o1, SlimProject o2) {
        if (nest == 0) {

          return o1.getLanguage().toLowerCase().compareTo(o2.getLanguage().toLowerCase());
        } else {
          int i = Integer.valueOf(o1.getDisplayOrder()).compareTo(o2.getDisplayOrder());
          return i == 0 ? o1.getName().compareTo(o2.getName()) : i;
        }
      }
    });

    int size = languages.size();
  //  logger.info("addProjectChoices " + size + "-------- nest " + nest);
    for (int i = 0; i < size; i += numInRow) {
      int max = i + numInRow;
      if (max > size) max = size;
      for (int j = i; j < max; j++) {
        SlimProject project = languages.get(j);
        Panel langIcon = getLangIcon(project.getLanguage(), project, nest);
        current.add(langIcon);
      }

      current = new Thumbnails();
      flags.add(current);
    }
  }

  private Panel getLangIcon(String lang, SlimProject projectForLang, int nest) {
    String lang1 = nest == 0 ? lang : projectForLang.getName();
    Panel imageAnchor = getImageAnchor(lang1, projectForLang);
    return imageAnchor;
  }

  /**
   * TODO : Consider arbitrarily deep nesting...
   *
   * @param name
   * @param projectForLang
   * @return
   */
  private Panel getImageAnchor(String name, SlimProject projectForLang) {
    int nest = 1;

    Heading child1 = new Heading(5, name);
    Thumbnail widgets = new Thumbnail();
    widgets.setSize(2);
    final int projid = projectForLang.getProjectid();
    String cc = projectForLang.getCountryCode();

    Image imageAnchor = new Image("langtest/cc/" + cc + ".png");
    PushButton button = new PushButton(imageAnchor);
    button.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent clickEvent) {
        NavLink projectCrumb = new NavLink(name);
        breadcrumbs.add(projectCrumb);
        List<SlimProject> children = projectForLang.getChildren();
        // logger.info("project " + projid + " has " + children);
        if (children.size() < 2) {
          //logger.info("onClick select leaf project " + projid + " current user " + userManager.getUser() + " : " + userManager.getUserID());
          setProjectForUser(projid);
        } else {
          logger.info("onClick select parent project " + projid + " and " + children.size() + " children ");
          projectCrumb.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
              clickOnParentCrumb(projectForLang);
            }
          });
          clearContent();
          showProjectChoices(children, nest);
        }
      }
    });
    widgets.add(button);
    widgets.add(child1);

    return widgets;
  }

  private void clickOnParentCrumb(SlimProject parent) {
    removeLastCrumb();
    addProjectChoices(1, parent);
  }

  private void removeLastCrumb() {
    int widgetCount = breadcrumbs.getWidgetCount();
    breadcrumbs.remove(widgetCount - 1);
  }

  private void removeUntilCrumb(int count) {
    int widgetCount = breadcrumbs.getWidgetCount();
    for (int i = widgetCount - 1; i > count; i--) {
      boolean remove = breadcrumbs.remove(i);
      logger.info("remove at " + i + "  " + remove);
    }
  }

  /**
   * @param projectid
   * @see #getImageAnchor(String, SlimProject)
   */
  private void setProjectForUser(int projectid) {
    logger.info("setProjectForUser set project for " + projectid);
    clearContent();
    userService.setProject(projectid, new AsyncCallback<User>() {
      @Override
      public void onFailure(Throwable throwable) {

      }

      @Override
      public void onSuccess(User aUser) {
        if (aUser == null) {
          logger.warning("huh? no current user? ");
        } else {
          userNotification.setProjectStartupInfo(aUser);
          logger.info("setProjectForUser set project for " + aUser + " show initial state " + lifecycleSupport.getProjectStartupInfo());
          showNavigation();
          navigation.showInitialState();
        }
      }
    });
  }

  /**
   * So, if we're currently showing the login, let's switch to the tab panel...
   *
   * @see #gotUser
   * @see #configureUIGivenUser(long) (long)
   */
  protected void populateRootPanelIfLogin() {
    int childCount = contentRow.getElement().getChildCount();

    if (DEBUG)
      logger.info("populateRootPanelIfLogin root " + contentRow.getElement().getNodeName() + " childCount " + childCount);
    if (childCount > 0) {
      Node child = contentRow.getElement().getChild(0);
      Element as = Element.as(child);
      if (DEBUG) logger.info("populateRootPanelIfLogin found : '" + as.getId() + "'");

      if (as.getId().contains(LOGIN)) {
        logger.info("populateRootPanelIfLogin found login...");
        populateRootPanel();
      } else {
        if (DEBUG) logger.info("populateRootPanelIfLogin no login...");
      }
    }
  }

  /**
   * @param userID
   * @see #configureUIGivenUser(long)
   */
  protected void showUserPermissions(long userID) {
    lastUser = userID;
    banner.setBrowserInfo(lifecycleSupport.getInfoLine());
    banner.reflectPermissions(lifecycleSupport.getPermissions());
  }

  /**
   * @see LangTest#makeFlashContainer()
   */
  @Override
  public void setSplash() {
    banner.setSubtitle();
  }
}
