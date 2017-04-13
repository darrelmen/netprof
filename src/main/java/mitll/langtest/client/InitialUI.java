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
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.banner.IBanner;
import mitll.langtest.client.banner.NewBanner;
import mitll.langtest.client.banner.NewContentChooser;
import mitll.langtest.client.banner.UserMenu;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.download.DownloadIFrame;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.client.project.ProjectChoices;
import mitll.langtest.client.services.UserService;
import mitll.langtest.client.services.UserServiceAsync;
import mitll.langtest.client.user.*;
import mitll.langtest.shared.project.ProjectStartupInfo;
import mitll.langtest.shared.user.SlimProject;
import mitll.langtest.shared.user.User;
import org.jetbrains.annotations.NotNull;

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

  /**
   * @see #getRootContainer
   */
  public static final String ROOT_VERTICAL_CONTAINER = "root_vertical_container";

  protected static final String LOGIN = "Login";
  private static final String LANGTEST_IMAGES = LangTest.LANGTEST_IMAGES;
  private static final int NO_USER_INITIAL = -2;

  private final UserManager userManager;
 // private final UserMenu userMenu;

  /**
   * @see #configureUIGivenUser
   * @see #gotUser
   * @see #lastUser
   * @see #resetState
   * @see #showUserPermissions
   */
  protected long lastUser = NO_USER_INITIAL;

  protected final LifecycleSupport lifecycleSupport;
  protected final ExerciseController controller;
  protected final UserFeedback userFeedback;
  protected final PropertyHandler props;

  protected final LangTestDatabaseAsync service = GWT.create(LangTestDatabase.class);
  private final UserServiceAsync userService    = GWT.create(UserService.class);

  private final IBanner banner;

  protected Widget headerRow;

  /**
   * TODO : move breadcrumbs up into banner
   */
  private Breadcrumbs breadcrumbs;
  protected Panel contentRow;
  private INavigation navigation;
 // private final BrowserCheck browserCheck = new BrowserCheck();
  private Container verticalContainer;
  private static final boolean DEBUG = false;
  private final ProjectChoices choices;

  /**
   * @param langTest
   * @param userManager
   * @see LangTest#onModuleLoad2()
   */
  public InitialUI(LangTest langTest, UserManager userManager) {
    this.lifecycleSupport = langTest;
    this.props = langTest.getProps();
    this.userManager = userManager;
    this.controller = langTest;
    userFeedback = langTest;
    this.choices = new ProjectChoices(langTest, this);
    //banner = new Banner(props, this, langTest, userMenu, langTest);
    UserMenu userMenu = new UserMenu(langTest, userManager, this);
//    logger.info("made user menu"+ userMenu);
    banner = new NewBanner(userManager, this, userMenu, breadcrumbs = getBreadcrumbs());
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
    Panel contentRow = makeFirstTwoRows(verticalContainer);
    choices.setContentRow(contentRow);
    if (!showLogin()) {
      populateBelowHeader(verticalContainer);
    }
//    logger.info("----> populateRootPanel END   ------>");
  }

  /**
   * @param verticalContainer
   * @return
   * @see #populateRootPanel
   */
  protected Panel makeFirstTwoRows(Container verticalContainer) {
    // add header row
    //verticalContainer.add(headerRow = makeHeaderRow());
    RootPanel.get().add(headerRow = makeHeaderRow());
    headerRow.getElement().setId("headerRow");

    Panel contentRow = new DivWidget();
    contentRow.getElement().setId("contentRow");

    verticalContainer.add(contentRow);
    this.contentRow = contentRow;

    RootPanel.get().add(getDownloadDiv());

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
/*    ClickHandler reload = new ClickHandler() {
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
    };*/
    // logger.info("talks to domino " + props.talksToDomino());
    //reload = (props.talksToDomino()) ? reload : null;
    Widget bannerRow = banner.getBanner();
return bannerRow;
//    headerRow = new FluidRow();
//    headerRow.add(new Column(12, bannerRow));
    //return headerRow;
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
/*
  private HTML getReleaseStatus() {
    browserCheck.getBrowserAndVersion();
    return new HTML(lifecycleSupport.getInfoLine());
  }
*/

  /**
   * NO NO NO don't do this
   *
   * @return
   */
  @Deprecated
  @Override
  public boolean isRTL() {
    boolean b = controller.getProps().isRightAlignContent();//navigation != null && navigation.isRTL();
    //  if (b) logger.info("content is RTL!");
    return b;
  }

  public void logout() {
    lifecycleSupport.logEvent("No widget", "UserLoging", "N/A", "User Logout by " + lastUser);

    //verticalContainer.remove(breadcrumbs);
    breadcrumbs.clear();

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
  public void clearContent() {
    clearStartupInfo();
    contentRow.clear();
    contentRow.add(lifecycleSupport.getFlashRecordPanel()); // put back record panel
  }

  private void clearStartupInfo() {
    lifecycleSupport.clearStartupInfo();
  }

  /**
   * @return
   */
  protected Container getRootContainer() {
    RootPanel.get().clear();   // necessary?
    Container verticalContainer = new FluidContainer();
    verticalContainer.getElement().setId(ROOT_VERTICAL_CONTAINER);
    verticalContainer.getElement().getStyle().setMarginTop(49, Style.Unit.PX);
    return verticalContainer;
  }

  @Override
  public int getHeightOfTopRows() {
    return headerRow.getOffsetHeight();
  }

  Heading child;

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
      child = new Heading(3, "Please allow recording");
      child.getElement().getStyle().setMarginLeft(550, Style.Unit.PX);
      contentRow.add(child);
    }
    // logger.info("populateBelowHeader -- ");
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
   * @see #gotUser(User)
   */
  private void showNavigation() {
    if (contentRow.getElement().getChildCount() <= 2) {
      // logger.info("showNavigation : - add to content root");
      contentRow.remove(child);
      if (navigation == null) makeNavigation(); // TODO : cheesy
      contentRow.add(navigation.getNavigation());
    } else {
      logger.info("showNavigation : first row has " + contentRow.getElement().getChildCount() + " child(ren) - not adding tab panel???");
    }
  }

  /**
   * @return
   * @see #addBreadcrumbs
   */
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
   * @see #chooseProjectAgain()
   * @see #getBreadcrumbs()
   */
  private void addCrumbs(Breadcrumbs crumbs) {
  /*  NavLink home = new NavLink("Home");

    home.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent clickEvent) {
        chooseProjectAgain();
      }
    });
    crumbs.add(home);*/

    User current = userManager.getCurrent();
    if (current != null) {
      ProjectStartupInfo startupInfo = lifecycleSupport.getProjectStartupInfo();
      if (startupInfo != null) {
        addBreadcrumbLevels(crumbs, startupInfo);
      }
      else {
        logger.info("no project startup info...");
      }
    } else {
      logger.info("getBreadcrumbs no current user --- ????");
    }
  }

  private void addBreadcrumbLevels(Breadcrumbs crumbs, ProjectStartupInfo startupInfo) {
    int currentProject = startupInfo.getProjectid();
    crumbs.clear();
    for (SlimProject project : lifecycleSupport.getStartupInfo().getProjects()) {
      if (project.hasChildren() && project.hasChild(currentProject)) {
        NavLink lang = new NavLink(project.getLanguage());
        crumbs.add(lang);
        logger.info("getBreadcrumbs adding step for " + lang);
        addProjectCrumb(crumbs, project.getChild(currentProject));

        lang.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent clickEvent) {
            clearStartupInfo();
            clearContent();
            removeUntilCrumb(2);
            choices.showProject(project);
          }
        });
      } else if (project.getProjectid() == currentProject) {
        addProjectCrumb(crumbs, project);
        break;
      } else {
        logger.fine("getBreadcrumbs skipping project " + project);
      }
    }
  }

  /**
   * @param crumbs
   * @param project
   * @return
   * @see #addCrumbs(Breadcrumbs)
   */
  private void addProjectCrumb(Breadcrumbs crumbs, SlimProject project) {
    String crumbName = project.getName();
    NavLink lang = new NavLink(crumbName);
/*    lang.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent clickEvent) {
        //               breadcrumbs = getBreadcrumbs();
        //              crumbs.clear();
        //            clearStartupInfo();
      }
    });*/
    crumbs.add(lang);
    //  logger.info("getBreadcrumbs adding step for " + lang);
    //return lang;
  }

  /**
   * @see #addCrumbs(Breadcrumbs)
   */
  public void chooseProjectAgain() {
    if (userManager.hasUser()) {
      userService.forgetProject(new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable throwable) {
        }

        @Override
        public void onSuccess(Void aVoid) {
          navigation.clearCurrent();
        }
      });

      History.newItem("");
      clearStartupInfo();

      breadcrumbs.clear();
      addCrumbs(breadcrumbs);

      clearContent();
      addProjectChoices(0, null);
    }
  }

  private void makeNavigation() {
   // navigation = new Navigation(service, userManager, controller, userFeedback, lifecycleSupport);
    navigation = new NewContentChooser(controller);
    banner.setNavigation(navigation);
  }

  /**
   * @return
   * @see #populateRootPanel
   * @see mitll.langtest.client.scoring.ScoringAudioPanel#ScoringAudioPanel
   * @deprecated
   */
  private boolean showOnlyOneExercise() {
    return props.getExercise_title() != null;
  }

  /**
   * @param currentExerciseVPanel
   * @return
   * @see #populateBelowHeader
   * @deprecated - we should really test this
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

    if (!props.getSendResetPassToken().isEmpty()) {
      handleSendResetPass(verticalContainer, contentRow, eventRegistration, resetPassToken);
      return true;
    }
    // are we here to show the login screen?
    boolean show = /*userManager.isUserExpired() ||*/ userManager.getUserID() == null;
    if (show) {
      //    logger.info("showLogin user is not valid : user expired " + userManager.isUserExpired() + " / " + userManager.getUserID());
      showLogin(eventRegistration);
      return true;
    }
    //   logger.info("user is valid...");

    banner.setCogVisible(true);
    return false;
  }

  private void showLogin(EventRegistration eventRegistration) {
    contentRow.add(new UserPassLogin(props, userManager, eventRegistration, lifecycleSupport.getStartupInfo()).getContent());
    clearPadding(verticalContainer);
    RootPanel.get().add(verticalContainer);
    banner.setCogVisible(false);
  }

  /**
   * @param verticalContainer
   * @param firstRow
   * @param eventRegistration
   * @param resetPassToken
   * @see #showLogin
   */
  private void handleResetPass(final Container verticalContainer,
                               final Panel firstRow,
                               final EventRegistration eventRegistration,
                               final String resetPassToken) {
    //logger.info("showLogin token '" + resetPassToken + "' for password reset");
    firstRow.add(new ResetPassword(props, eventRegistration, userManager).getResetPassword(resetPassToken));
    clearPadding(verticalContainer);
    RootPanel.get().add(verticalContainer);
    banner.setCogVisible(false);
  }

  private void handleSendResetPass(final Container verticalContainer,
                                   final Panel firstRow,
                                   final EventRegistration eventRegistration,
                                   final String resetPassToken) {
    //logger.info("showLogin token '" + resetPassToken + "' for password reset");
    firstRow.add(new SendResetPassword(props, eventRegistration, userManager).getResetPassword(resetPassToken));
    clearPadding(verticalContainer);
    RootPanel.get().add(verticalContainer);
    banner.setCogVisible(false);
  }
/*
  private void trimURL() {
    Window.Location.replace(trimURL(Window.Location.getHref()));
  }
*/

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
        navigation.showPreviousState();
      } else {
        logger.warning("how can navigation be null????");
      }
    }
    if (userID > -1) {
      banner.setCogVisible(true);
      banner.setVisibleAdmin(
          user.isAdmin() ||
              // props.isAdminView() ||
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
    //logger.info("configureUIGivenUser : user changed - new " + userID + " vs last " + lastUser);
    if (lifecycleSupport.getProjectStartupInfo() == null) {
      addProjectChoices(0, null);
    } else {
      logger.info("\tconfigureUIGivenUser : " + userID + " get exercises...");
      addBreadcrumbs();
      showInitialState();
    }
    showUserPermissions(userID);
  }

  @Override
  public void showInitialState() {
    showNavigation();
    navigation.showInitialState();
  }

  /**
   * @see #configureUIGivenUser
   * @see #chooseProjectAgain
   * @see #clickOnParentCrumb
   */
  private void addProjectChoices(int level, SlimProject parent) {
    clearContent();
    addBreadcrumbs();
    choices.showProjectChoices(level, parent);
  }

  /**
   * TODO : move breadcrumbs up into banner
   *
   * @see #addProjectChoices
   */
  private void addBreadcrumbs() {
/*    int childCount = verticalContainer.getElement().getChildCount();
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
    } else {
      logger.info("addBreadcrumbs - no breadcrumbs...");
    }
    if (!found) {
      //verticalContainer.insert(breadcrumbs = getBreadcrumbs(), 1);
    //  banner.setBreadcrumbs(breadcrumbs = getBreadcrumbs());
      addCrumbs(breadcrumbs);
    }*/
    addCrumbs(breadcrumbs);

  }

  @Override
  @NotNull
  public NavLink makeBreadcrumb(String name) {
    NavLink projectCrumb = new NavLink(name);
    breadcrumbs.add(projectCrumb);
    return projectCrumb;
  }

/*
  @NotNull
  private Image getFlag(String cc) {
    return new Image("langtest/cc/" + cc + ".png");
  }
*/

  @Override
  public void clickOnParentCrumb(SlimProject parent) {
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
        // logger.info("populateRootPanelIfLogin found login...");
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
