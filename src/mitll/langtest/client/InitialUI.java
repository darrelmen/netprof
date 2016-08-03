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
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.flashcard.Banner;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.client.instrumentation.EventTable;
import mitll.langtest.client.monitoring.MonitoringManager;
import mitll.langtest.client.result.ResultManager;
import mitll.langtest.client.services.UserService;
import mitll.langtest.client.services.UserServiceAsync;
import mitll.langtest.client.user.*;
import mitll.langtest.shared.user.SlimProject;
import mitll.langtest.shared.user.User;

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

  protected static final String LOGIN = "Login";

  private static final String LANGTEST_IMAGES = LangTest.LANGTEST_IMAGES;
  private static final int NO_USER_INITIAL = -2;

  private final UserManager userManager;

  protected long lastUser = NO_USER_INITIAL;

  protected final LifecycleSupport lifecycleSupport;
  protected final ExerciseController controller;
  protected final UserFeedback userFeedback;
  protected final PropertyHandler props;

  protected final LangTestDatabaseAsync service = GWT.create(LangTestDatabase.class);
  private final UserServiceAsync userService = GWT.create(UserService.class);

  private final Banner banner;

  protected Panel headerRow;
  protected Panel firstRow;
  private Navigation navigation;
  private final BrowserCheck browserCheck = new BrowserCheck();
  private Container verticalContainer;

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
    banner = new Banner(props, userService, langTest, langTest);
  }

  /**
   * @see LangTest#showLogin()
   * @see LangTest#populateRootPanel()
   */
  @Override
  public void populateRootPanel() {
    logger.info("----> populateRootPanel BEGIN ------>");
    Container verticalContainer = getRootContainer();
    this.verticalContainer = verticalContainer;
    // header/title line
    // first row ---------------
    Panel firstRow = makeFirstTwoRows(verticalContainer);

    if (!showLogin()) {
      populateBelowHeader(verticalContainer, firstRow);
    }
    logger.info("----> populateRootPanel END   ------>");
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
    firstRow.getElement().setId("firstRow");

    verticalContainer.add(firstRow);
    this.firstRow = firstRow;
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
    Widget bannerRow = banner.makeNPFHeaderRow(props.getSplash(), props.isBeta(), getGreeting(),
        getReleaseStatus(),
        new LogoutClickHandler(),
        new UsersClickHandler(),
        new ResultsClickHandler(),
        new MonitoringClickHandler(),
        new EventsClickHandler(),
        reload
    );

    headerRow = new FluidRow();
    headerRow.add(new Column(12, bannerRow));
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
    return new HTML(lifecycleSupport.getInfoLine());
  }

  @Override
  public boolean isRTL() {
    return navigation != null && navigation.isRTL();
  }

  private class LogoutClickHandler implements ClickHandler {
    public void onClick(ClickEvent event) {
      lifecycleSupport.logEvent("No widget", "UserLoging", "N/A", "User Logout by " + lastUser);
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
  }

  /**
   * @seex mitll.langtest.client.LangTest.LogoutClickHandler#onClick(com.google.gwt.event.dom.client.ClickEvent)
   */
  private void resetState() {
    //logger.info("clearing current history token");
    History.newItem(""); // clear history!
    userManager.clearUser();
    lastUser = NO_USER_INITIAL;
    lifecycleSupport.recordingModeSelect();
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

  private class ResultsClickHandler implements ClickHandler {
    final EventRegistration outer = lifecycleSupport;

    public void onClick(ClickEvent event) {
      GWT.runAsync(new RunAsyncCallback() {
        public void onFailure(Throwable caught) {
          downloadFailedAlert();
        }

        public void onSuccess() {
          ResultManager resultManager = new ResultManager(service, props.getNameForAnswer(),
              lifecycleSupport.getStartupInfo().getTypeOrder(), outer, controller);
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
          new MonitoringManager(/*service,*/ props).showResults();
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
    verticalContainer.getElement().setId("root_vertical_container");
    return verticalContainer;
  }

  @Override
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
      firstRow.add(lifecycleSupport.getFlashRecordPanel());
    }
    lifecycleSupport.recordingModeSelect();

    //  addNavigationToMainContent();
    makeNavigation();
    addResizeHandler();
  }

  private void addNavigationToMainContent() {
    makeNavigation();

    //showNavigation(firstRow);
  }

  private void showNavigation() {
    if (firstRow.getElement().getChildCount() == 1) {
      firstRow.add(navigation.getTabPanel());
    } else {
      logger.info("first row has " + firstRow.getElement().getChildCount() + " child(ren)");
    }
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
      showLogin(eventRegistration);
      return true;
    }
    //   logger.info("user is valid...");

    banner.setCogVisible(true);
    return false;
  }

  private void showLogin(EventRegistration eventRegistration) {
    firstRow.add(new UserPassLogin(service, userService, props, userManager, eventRegistration).getContent());
    clearPadding(verticalContainer);
    RootPanel.get().add(verticalContainer);
    banner.setCogVisible(false);
  }

  private void handleResetPass(final Container verticalContainer, final Panel firstRow,
                               final EventRegistration eventRegistration, final String resetPassToken) {
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
    userService.enableCDUser(cdToken, emailR, Window.Location.getHref(), new AsyncCallback<String>() {
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
   * @see UserManager#gotNewUser(User)
   * @see UserManager#storeUser
   */
  @Override
  public void gotUser(User user) {
    populateRootPanelIfLogin();
    long userID = -1;
    if (user != null) {
      userID = user.getId();
    }

    logger.info("gotUser : userID " + userID);

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
      banner.setVisibleAdmin(user.isAdmin() || props.isAdminView() || user.isTeacher() || user.isCD());
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
//    logger.info("configureUIGivenUser : user changed - new " + userID + " vs last " + lastUser +
//        " audio type " + getAudioType() + " perms " + getPermissions());
    // populateRootPanelIfLogin();
    if (lifecycleSupport.getStartupInfo() == null) {
      addProjectChoices();
    } else {
      logger.info("\tconfigureUIGivenUser : " + userID + " get exercises...");
      showNavigation();
      navigation.showInitialState();
      showUserPermissions(userID);
    }
  }

  private void addProjectChoices() {

    userService.getProjects(new AsyncCallback<List<SlimProject>>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(List<SlimProject> result) {
//        projects = result;
//        projectChoices.clear();
//        projectChoices.addItem("Choose a Language");

        final Container flags = new Container();
        final Section section = new Section("section");
        section.add(flags);
        firstRow.add(section);

        //   Row current = new Row();
        Panel current = new Thumbnails();
        flags.add(current);
        int numInRow = 4;
        logger.info("got " + result.size() + "-------- ");
        for (int i = 0; i < result.size(); i += numInRow) {

          int max = i + numInRow;
          if (max > result.size()) max = result.size();
          for (int j = i; j < max; j++) {
            //Column child = new Column(1);
            //current.add(child);
            Panel langIcon = getLangIcon(result, j);
            //child.add(langIcon);
            current.add(langIcon);
          }

          current = new Row();
          flags.add(current);
        }

      }
    });
  }


  private Panel getLangIcon(List<SlimProject> result, int j) {
    SlimProject choice = result.get(j);
    Panel imageAnchor = getImageAnchor(choice.getName(), choice.getCountryCode());
    //  addClickHandler(samples, choice, imageAnchor);
    return imageAnchor;
  }

/*  private void addClickHandler(final Container samples, final LIDResp choice, ImageAnchor imageAnchor) {
    imageAnchor.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        String code = choice.getCode();
        //    logger.info("got click on " + code);
        LLClassDemoService.App.getInstance().getSamplesFor(code, new AsyncCallback<List<LIDSample>>() {
          @Override
          public void onFailure(Throwable caught) {
          }

          @Override
          public void onSuccess(List<LIDSample> result) {
            samples.clear();
            ScrollPanel scrollPanel = new ScrollPanel();
            samples.add(scrollPanel);
            Container inside = new Container();
            scrollPanel.add(inside);

            //      logger.info("Adding " + result.size() + " items");
            for (LIDSample sample : result) {
              Row current = new Row();

              inside.add(current);

              Column truthCol = new Column(ColumnSize.MD_1);
              current.add(truthCol);

              MediaList truthMediaList = new MediaList();
              getImageAnchor(sample.getTruth(), truthMediaList, false, true);
              truthCol.add(truthMediaList);

              Column tweet = new Column(ColumnSize.MD_8);
              current.add(tweet);

              Heading tweetHeading = new Heading(HeadingSize.H4, sample.getTweet());
              tweet.add(tweetHeading);

              Column scoreCol = new Column(ColumnSize.MD_1);
              current.add(scoreCol);

              MediaList mediaList = new MediaList();
              getImageAnchor(sample.getScore(), mediaList, false, false);
              scoreCol.add(mediaList);
            }
          }
        });
      }
    });
  }*/

  private Panel getImageAnchor(String name, String cc) {
    // ListItem listItem = new ListItem();
    //    listItem.getElement().setId("listItemFor_" + choice.getCode());
//    listItem.setMarginLeft(5);

    // MediaBody mediaBody = new MediaBody();
    Heading child1 = new Heading(5, name);//choice.getLabel());
    Thumbnail widgets = new Thumbnail();
    widgets.setSize(2);
    //FocusPanel widgets= new FocusPanel();
//    child1.setMarginTop(5);
    // mediaBody.add(child1);

    Image imageAnchor = new Image("langtest/cc/" + cc + ".png");
    PushButton button = new PushButton(imageAnchor);
    //button.getUpFace().setText(name);

    widgets.add(button);
    widgets.add(child1);


    return widgets;
  }

//  private ImageAnchor getImageAnchor(/*LIDResp choice*/) {
//    ImageAnchor imageAnchor = new ImageAnchor();
//    imageAnchor.setAsMediaObject(true);
//    imageAnchor.setPull(Pull.LEFT);
////      imageAnchor.setAlt(choice.getCode());
//    //     imageAnchor.setUrl("images/" + choice.getImageURL());
//    return imageAnchor;
//  }

  /**
   * So, if we're currently showing the login, let's switch to the tab panel...
   *
   * @see #gotUser
   * @see #configureUIGivenUser(long) (long)
   */
  protected void populateRootPanelIfLogin() {
    int childCount = firstRow.getElement().getChildCount();

    logger.info("populateRootPanelIfLogin root " + firstRow.getElement().getNodeName() + " childCount " + childCount);
    if (childCount > 0) {
      Node child = firstRow.getElement().getChild(0);
      Element as = Element.as(child);
      logger.info("populateRootPanelIfLogin found : '" + as.getId() + "'");

      if (as.getId().contains(LOGIN)) {
        logger.info("populateRootPanelIfLogin found login...");
        populateRootPanel();
      } else {
        logger.info("populateRootPanelIfLogin no login...");
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
