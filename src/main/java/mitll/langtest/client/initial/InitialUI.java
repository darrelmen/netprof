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

package mitll.langtest.client.initial;

import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Modal;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.banner.IBanner;
import mitll.langtest.client.banner.NewBanner;
import mitll.langtest.client.banner.NewContentChooser;
import mitll.langtest.client.banner.UserMenu;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.download.DownloadIFrame;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.client.project.ProjectChoices;
import mitll.langtest.client.user.ResetPassword;
import mitll.langtest.client.user.SendResetPassword;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.client.user.UserPassLogin;
import mitll.langtest.shared.exercise.HasID;
import mitll.langtest.shared.project.ProjectMode;
import mitll.langtest.shared.project.ProjectStartupInfo;
import mitll.langtest.shared.project.SlimProject;
import mitll.langtest.shared.user.HeartbeatStatus;
import mitll.langtest.shared.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static mitll.langtest.client.LangTest.RECORDING_DISABLED;
import static mitll.langtest.client.user.ResetPassword.SHOW_ADVERTISED_IOS;
import static mitll.langtest.client.user.UserPassLogin.*;


/**
 * <br/>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 2/23/16
 */
public class InitialUI implements UILifecycle, BreadcrumbPartner {
  private final Logger logger = Logger.getLogger("InitialUI");

  /**
   * Make sure we can talk to the server...
   *
   * @see #confirmCurrentProject
   */
  public static final String CHECK_NETWORK_WIFI = "CHECK NETWORK/WIFI";
  private static final int WIFI_MAX_WAIT = 7000;

  /**
   *
   */
  private static final String PLEASE_ALLOW_RECORDING = "Please allow recording";

  /**
   * TODO : WHY???
   *
   * @see #getRootContainer
   */
  private static final int MARGIN_TOP = 47;//5;//47;

  /**
   * @see #getRootContainer
   */
  public static final String ROOT_VERTICAL_CONTAINER = "root_vertical_container";

  /**
   * Critical - don't mess with this.
   */
  private static final String LOGIN = "Login";
  private static final int NO_USER_INITIAL = -2;

  private final UserManager userManager;

  /**
   * @see #configureUIGivenUser
   * @see #gotUser
   * @see #resetState
   * @see #showUserPermissions
   */
  private long lastUser = NO_USER_INITIAL;

  private final LifecycleSupport lifecycleSupport;
  private final ExerciseController controller;
  private final PropertyHandler props;

  private final IBanner banner;

  private Widget headerRow;

  /**
   * TODO : move breadcrumbHelper up into banner
   */
  //private final Breadcrumbs breadcrumbHelper;
  private final IBreadcrumbHelper breadcrumbHelper;
  private DivWidget contentRow;
  private INavigation navigation;
  private DivWidget verticalContainer;
  private final ProjectChoices choices;

  private static final boolean DEBUG = false;

  /**
   * @param langTest
   * @param userManager
   * @see LangTest#onModuleLoad2
   */
  public InitialUI(LangTest langTest, UserManager userManager) {
    this.lifecycleSupport = langTest;
    this.props = langTest.getProps();
    this.userManager = userManager;
    this.controller = langTest;
    this.choices = new ProjectChoices(langTest, this);
    this.breadcrumbHelper = new BreadcrumbHelper(userManager, lifecycleSupport, this);
    banner =
        new NewBanner(userManager,
            this,
            new UserMenu(langTest, userManager, this),
            breadcrumbHelper.getBreadcrumbs(),
            controller);
  }

  public INavigation getNavigation() {
    return navigation;
  }


  /**
   * So, if we're currently showing the login, let's switch to the tab panel...
   *
   * @see UILifecycle#gotUser
   * @see #configureUIGivenUser(long) (long)
   */
  private void populateRootPanelIfLogin() {
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
   * @see LangTest#showLogin()
   * @see LangTest#populateRootPanel()
   */
  @Override
  public void populateRootPanel() {
    //  logger.info("----> populateRootPanel BEGIN ------>");
    DivWidget verticalContainer = getRootContainer();
    this.verticalContainer = verticalContainer;
    // header/title line
    // first row ---------------
    choices.setContentRow(makeFirstTwoRows(verticalContainer));
    if (!showLogin()) {
      populateBelowHeader(verticalContainer);

      try {
        if (props.isShowAdvertiseIOS() && !controller.getStorage().hasValue(SHOW_ADVERTISED_IOS)) {
          showIOSAd();
        }
      } catch (Exception e) {
        logger.warning("got " + e);
      }
    }
//    logger.info("----> populateRootPanel END   ------>");
  }

  private void showIOSAd() {
    //logger.warning("showIOSAd ");
    List<String> messages = Arrays.asList(IPAD_LINE_1, IPAD_LINE_2);
    Modal modal = new ModalInfoDialog().getModal(
        INSTALL_APP,
        messages,
        Collections.emptySet(),
        null,
        hiddenEvent -> {
        },
        true,
        true);
    modal.setMaxHeigth(600 + "px");
    controller.getStorage().storeValue(SHOW_ADVERTISED_IOS, "true");
    modal.show();
  }

  /**
   * @param verticalContainer
   * @return
   * @see #populateRootPanel
   */
  private DivWidget makeFirstTwoRows(DivWidget verticalContainer) {
    // add header row
    RootPanel rootPanel = RootPanel.get();
    rootPanel.add(headerRow = makeHeaderRow());

    DivWidget contentRow = new DivWidget();
    contentRow.getElement().setId("InitialUI_contentRow");
    contentRow.setHeight("100%");

    verticalContainer.add(contentRow);
    this.contentRow = contentRow;

    rootPanel.add(getDownloadDiv());

    return contentRow;
  }

  @NotNull
  private DivWidget getDownloadDiv() {
    DivWidget w = new DivWidget();
    w.getElement().setId(DownloadIFrame.DOWNLOAD_AREA_ID);
    w.setVisible(false);
    return w;
  }

  /**
   * @return
   * @see #populateRootPanel()
   */
  private Widget makeHeaderRow() {
    return banner.getBanner();
  }

  /**
   * @return
   * @see UILifecycle#gotUser
   * @see #makeHeaderRow()
   */
  private String getGreeting() {
    return userManager.getUserID() == null ? "" : ("" + userManager.getUserID());
  }

  /**
   * @see UserMenu#getLogOut()
   */
  public void logout() {
    userManager.clearUser();

    banner.reset();
    breadcrumbHelper.clearBreadcrumbs();
    breadcrumbHelper.setVisible(false);

    controller.getUserService().logout(new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable throwable) {
        controller.handleNonFatalError("logging out user", throwable);
      }

      @Override
      public void onSuccess(Void aVoid) {
        resetState();
      }
    });

    lifecycleSupport.logEvent("No widget", "UserLoging", "N/A", "User Logout by " + lastUser);
  }

  /**
   * @seex mitll.langtest.client.LangTest.LogoutClickHandler#onClick(com.google.gwt.event.dom.client.ClickEvent)
   */
  private void resetState() {
    pushClearHistory(); // clear history!
    userManager.clearUser();
    lastUser = NO_USER_INITIAL;
    lifecycleSupport.recordingModeSelect();
    clearContent();
  }

  private void pushClearHistory() {
    History.newItem("");
  }

  /**
   * @see BreadcrumbHelper#getBreadcrumbs
   */
  public void clearContent() {
    //logger.info("clearContent -");
    clearStartupInfo();
    contentRow.clear();
    contentRow.getElement().getStyle().setPosition(Style.Position.FIXED);

    contentRow.add(lifecycleSupport.getFlashRecordPanel()); // put back record panel
  }

  /**
   *
   */
  private void clearStartupInfo() {
    //logger.info("clearStartupInfo -");
    lifecycleSupport.clearStartupInfo();
  }

  /**
   * @return
   * @see #populateRootPanel
   */
  private DivWidget getRootContainer() {
    RootPanel.get().clear();   // necessary?

    DivWidget verticalContainer = new FluidContainer();
//    DivWidget verticalContainer = new DivWidget();
    verticalContainer.setId("rootVerticalContainer");
    addMouseOverHandler(verticalContainer, event -> confirmCurrentProject());
    // logger.info("getRootContainer Add mouse over to " + verticalContainer.getId());
    com.google.gwt.user.client.Element element = verticalContainer.getElement();
    element.setId(ROOT_VERTICAL_CONTAINER);
    element.getStyle().setMarginTop(MARGIN_TOP, Style.Unit.PX);
    //   verticalContainer.getElement().getStyle().setProperty("height", "calc(100% - 49px)");

    return verticalContainer;
  }

  /**
   * So if we open multiple tabs with multiple projects, if we go back to an earlier project tab, we want to notice
   * that and make it the current one.
   * <p>
   * Also, if we log out of one tab and go to another, we'll notice here.
   *
   * Does heartbeat wifi connectivity checking...
   *
   * @see #getRootContainer
   */
  private void confirmCurrentProject() {
    if (userManager.getCurrent() != null) {
      ProjectStartupInfo projectStartupInfo = lifecycleSupport.getProjectStartupInfo();
      String implementationVersion = lifecycleSupport.getStartupInfo().getImplementationVersion();
      if (projectStartupInfo == null) {
        long then = System.currentTimeMillis();
        Timer timer = getWifiTimer();
        controller.getOpenUserService().checkHeartbeat(implementationVersion, new AsyncCallback<HeartbeatStatus>() {
          @Override
          public void onFailure(Throwable caught) {
            cancelHeartbeatTimer(timer);
          }

          @Override
          public void onSuccess(HeartbeatStatus result) {
            cancelHeartbeatTimer(timer);
            long now = System.currentTimeMillis();
            //logger.info("1 waited " + (now - then));
            if (result.isCodeHasUpdated()) {
              logger.info("confirmCurrentProject : took " + (now - then) + " millis to check : CODE HAS CHANGED!");
              Window.Location.reload();
            }
          }
        });
      } else {
        long then = System.currentTimeMillis();

        Timer timer = getWifiTimer();

        controller.getOpenUserService().setCurrentUserToProject(projectStartupInfo.getProjectid(), implementationVersion, new AsyncCallback<HeartbeatStatus>() {
          @Override
          public void onFailure(Throwable caught) {
            cancelHeartbeatTimer(timer);
          }

          @Override
          public void onSuccess(HeartbeatStatus result) {
            cancelHeartbeatTimer(timer);
            long now = System.currentTimeMillis();
            //logger.info("2 waited " + (now - then));
            if (result.isCodeHasUpdated()) {
              logger.info("confirmCurrentProject : took " + (now - then) + " millis to check : CODE HAS CHANGED!");
              Window.Location.reload();
            } else if (!result.isHasSession()) {
              logger.info("confirmCurrentProject : took " + (now - then) + " millis to check on user - logging out!");
              logout();
            }
          }
        });
      }
    }
  }

  private void cancelHeartbeatTimer(Timer timer) {
    controller.setHasNetworkProblem(false);
    timer.cancel();
    setSplash(controller.isMicAvailable() ? "" : RECORDING_DISABLED);
  }

  @NotNull
  private Timer getWifiTimer() {
    long then = System.currentTimeMillis();
    Timer timer = new Timer() {
      @Override
      public void run() {
        logger.warning("getWifiTimer waited " + (System.currentTimeMillis() - then) + " for a response");
        setSplash(CHECK_NETWORK_WIFI);
        controller.setHasNetworkProblem(true);
      }
    };
    timer.schedule(WIFI_MAX_WAIT);
    return timer;
  }

  private HandlerRegistration addMouseOverHandler(DivWidget container, MouseOverHandler handler) {
    return container.addDomHandler(handler, MouseOverEvent.getType());
  }

  @Override
  public int getHeightOfTopRows() {
    return headerRow.getOffsetHeight();
  }

  private Heading child;

  /**
   * * TODOx : FIX ME for headstart?
   *
   * @param verticalContainer
   * @see #populateRootPanel
   * @see #showLogin()
   */
  private void populateBelowHeader(DivWidget verticalContainer) {
    RootPanel.get().add(verticalContainer);

    /**
     * {@link #makeFlashContainer}
     */
    contentRow.add(lifecycleSupport.getFlashRecordPanel());

    {
      child = new Heading(3, PLEASE_ALLOW_RECORDING);
      child.setVisible(false);

      Timer waitTimer = new Timer() {
        @Override
        public void run() {
          child.setVisible(true);
        }
      };
      waitTimer.schedule(1000);

      child.getElement().getStyle().setMarginLeft(550, Style.Unit.PX);
      contentRow.add(child);
    }

    lifecycleSupport.recordingModeSelect();
    makeNavigation();
    addResizeHandler();
  }

  /**
   * Breadcrumb shows sequence of choices :
   * <p>
   * languge - course (FL100 or FL200 or FL300) Elementary FL/Intermediate FL/Advanced FL - what do you want to do
   *
   * @see #configureUIGivenUser
   * @see UILifecycle#gotUser(HasID)
   */
  private void showNavigation() {
    int childCount = contentRow.getElement().getChildCount();
    if (childCount <= 2) {
      //   logger.info("showNavigation : - add to content root = " + childCount);
      contentRow.remove(child);
      if (navigation == null) makeNavigation(); // TODO : cheesy
      contentRow.add(navigation.getNavigation());
    } else {
      logger.warning("showNavigation : first row has " + childCount + " child(ren) - not adding tab panel???");
    }
  }


  @Override
  public void resetLanguageSelection(int levelToRemove, SlimProject project) {
    pushClearHistory();
    clearStartupInfo();
    clearContent();
//    removeUntilCrumb(levelToRemove);
    banner.setVisibleChoices(false);
    choices.showProject(project);
  }


  /**
   * @see BreadcrumbHelper#addCrumbs
   */
  @Override
  public void chooseProjectAgain() {
    if (userManager.hasUser()) {
      // logger.info("chooseProjectAgain user : " + userManager.getUser() + " " + userManager.getUserID());

      if (userManager.isPolyglot()) {
        logger.info("\tpolyglot users don't get to change projects.");
      } else {
        forgetProjectForUser();

        pushClearHistory();
        clearStartupInfo();

        breadcrumbHelper.clearBreadcrumbs();
        breadcrumbHelper.addCrumbs(true);

        clearContent();
        addProjectChoices(0, null);
        showCogMenu();
      }
    } else {
      logger.warning("chooseProjectAgain no user --- ");
    }
  }
/*

  private void clearBreadcrumbs() {
    // logger.info("breadcrumbHelper clear");
    breadcrumbHelper.clear();
  }
*/

  private void forgetProjectForUser() {
    controller.getOpenUserService().forgetProject(new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable throwable) {
        controller.handleNonFatalError("forgetting project for user", throwable);
      }

      @Override
      public void onSuccess(Void aVoid) {
        banner.setVisibleChoices(false);
        navigation.clearCurrent();
      }
    });
  }

  private void makeNavigation() {
    navigation = new NewContentChooser(controller, banner);
    banner.setNavigation(navigation);
  }

  private void addResizeHandler() {
    final InitialUI outer = this;
    Window.addResizeHandler(event -> outer.onResize());
  }

  /**
   * @see #addResizeHandler
   */
  private void onResize() {
    if (navigation != null) navigation.onResize();
  }

  /**
   * So we have three different views here : the login page, the reset password page, and the content developer
   * enabled feedback page.
   *
   * @return false if we didn't do either of the special pages and should do the normal navigation view
   * @see #populateRootPanel()
   */
  private boolean showLogin() {
    final EventRegistration eventRegistration = lifecycleSupport;


    // check if we're here as a result of resetting a password
    final String resetPassToken = props.getResetPassToken();
    if (!resetPassToken.isEmpty()) {
      handleResetPass(verticalContainer, contentRow, eventRegistration, resetPassToken);
      return true;
    }

    if (!props.getSendResetPassToken().isEmpty()) {
      handleSendResetPass(verticalContainer, contentRow, eventRegistration);
      return true;
    }
    // are we here to show the login screen?
    boolean show = userManager.getUserID() == null;
    if (show) {
      // logger.info("showLogin user is not valid : user expired " + userManager.getUserID());
      showLogin(eventRegistration);
      return true;
    }
    // logger.info("user is valid...");

    showCogMenu();
    return false;
  }

  public void showCogMenu() {
    banner.setCogVisible(true);
  }

  private void showLogin(EventRegistration eventRegistration) {
    contentRow.add(new UserPassLogin(props, userManager, eventRegistration, lifecycleSupport.getStartupInfo()).getContent());
    contentRow.getElement().getStyle().setPosition(Style.Position.RELATIVE);

    clearPadding(verticalContainer);
    RootPanel.get().add(verticalContainer);
    hideCogMenu();
  }

  private void hideCogMenu() {
    banner.setCogVisible(false);
  }

  /**
   * @param verticalContainer
   * @param firstRow
   * @param eventRegistration
   * @param resetPassToken
   * @see #showLogin
   */
  private void handleResetPass(final DivWidget verticalContainer,
                               final Panel firstRow,
                               final EventRegistration eventRegistration,
                               final String resetPassToken) {
    //logger.info("showLogin token '" + resetPassToken + "' for password reset");
    firstRow.add(new ResetPassword(props, eventRegistration, userManager, controller.getStorage()).getResetPassword(resetPassToken));
    clearPadding(verticalContainer);
    RootPanel.get().add(verticalContainer);
    hideCogMenu();
  }

  private void handleSendResetPass(final DivWidget verticalContainer,
                                   final Panel firstRow,
                                   final EventRegistration eventRegistration) {
    //logger.info("showLogin token '" + resetPassToken + "' for password reset");
    firstRow.add(new SendResetPassword(props, eventRegistration, userManager).getResetPassword());
    clearPadding(verticalContainer);
    RootPanel.get().add(verticalContainer);
    hideCogMenu();
  }

  private void clearPadding(DivWidget verticalContainer) {
    verticalContainer.getElement().getStyle().setPaddingLeft(0, Style.Unit.PX);
    verticalContainer.getElement().getStyle().setPaddingRight(0, Style.Unit.PX);
  }

  /**
   * Init Flash recorder once we login.
   * <p>
   * Only get the exercises if the user has accepted mic access.
   *
   * @param user
   * @see LangTest#gotUser
   * @see UserManager#gotNewUser(User)
   * @see UserManager#storeUser
   */
  @Override
  public void gotUser(HasID user) {
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
      // logger.info("gotUser ignoring got user for current user " + userID + " perms " + userManager.getPermissions());
      if (navigation != null) {
        showNavigation();
        navigation.showPreviousState();
      } else {
        logger.warning("gotUser how can navigation be null????");
      }
    }

    if (userID > -1) {
      showCogMenu();
    }
  }

  /**
   * @see ProjectChoices#showDeleteDialog
   */
  @Override
  public void startOver() {
    breadcrumbHelper.clearBreadcrumbs();
    configureUIGivenUser(userManager.getUser());
  }

  @Override
  public void getUserPermissions() {
    userManager.getPermissionsAndSetUser();
  }

  /**
   * So we know who the user is... now decide what to show them next
   *
   * @param userID
   * @return
   * @see UILifecycle#gotUser
   */
  private void configureUIGivenUser(long userID) {
    // logger.info("configureUIGivenUser : user changed - new " + userID + " vs last " + lastUser);
    boolean hasStartupInfo = lifecycleSupport.getProjectStartupInfo() != null;
    if (hasStartupInfo) {
     logger.info("\tconfigureUIGivenUser : " + userID + " get exercises...");
      addBreadcrumbs();
      showInitialState();
    } else {
      addProjectChoices(0, null);
    }
    showUserPermissions(userID);
  }

  /**
   * @see #configureUIGivenUser
   * @see LangTest#reallySetTheProject
   * @see ProjectChoices#setProjectForUser
   */
  @Override
  public void showInitialState() {
    // setMode(mode);
    logger.info("showInitialState ");
    showNavigation();
    banner.checkProjectSelected();
    //banner.setVisibleChoicesByMode(getMode());
    navigation.showInitialState();
  }
/*
  @Override
  public ProjectMode getMode() {
    String mode = controller.getStorage().getValue("Mode");
    ProjectMode modeEnum = ProjectMode.VOCABULARY;

    if (mode == null) {

    } else {
      try {
        modeEnum = ProjectMode.valueOf(mode);
      } catch (IllegalArgumentException e) {
      }
    }
    return modeEnum;
  }

  @Override
  public void setMode(ProjectMode mode) {
    controller.getStorage().storeValue("Mode", mode.toString());
    logger.info("mode now " + getMode());
  }*/

  /**
   * @see #configureUIGivenUser
   * @see #chooseProjectAgain
   * @see #clickOnParentCrumb
   */
  private void addProjectChoices(int level, SlimProject parent) {
    clearContent();
    //logger.info("addProjectChoices level " + level + " parent " + parent);
    // if (level == 0) {
    ///  logger.info("\taddProjectChoices level " + level + " parent " + parent);
    breadcrumbHelper.addCrumbs(level == 0);
    //}
    choices.showProjectChoices(parent, level);
  }

  /**
   * TODO : move breadcrump stuff to another class!
   *
   * @see #addProjectChoices
   */
  @Override
  public void addBreadcrumbs() {
    breadcrumbHelper.addCrumbs(true);
  }

  /**
   * @param name
   * @return
   * @see ProjectChoices#gotClickOnFlag
   */
  @Override
  @NotNull
  public NavLink makeBreadcrumb(String name) {
    return breadcrumbHelper.makeBreadcrumb(name);
  }

  /**
   * @param parent
   * @see ProjectChoices#gotClickOnFlag
   */
  @Override
  public void clickOnParentCrumb(SlimProject parent) {
    pushClearHistory(); // clear history!
    breadcrumbHelper.removeLastCrumb();
    addProjectChoices(1, parent);
  }

  /**
   * @see #clickOnParentCrumb
   */
//  private void removeLastCrumb() {
//    breadcrumbHelper.removeLastCrumb();
//  }

/*
  public String getLastBreadcrumb() {
    return breadcrumbHelper.getLastBreadcrumb();
  }
*/

  /**
   * @param userID
   * @see #configureUIGivenUser(long)
   */
  private void showUserPermissions(long userID) {
    lastUser = userID;
    banner.reflectPermissions(lifecycleSupport.getPermissions());
  }

  /**
   * @param message
   * @see LangTest#makeFlashContainer
   */
  @Override
  public void setSplash(String message) {
    banner.setSubtitle(message);
  }
}
