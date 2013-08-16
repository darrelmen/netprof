package mitll.langtest.client;

import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.Container;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Row;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.visualization.client.VisualizationUtils;
import com.google.gwt.visualization.client.visualizations.corechart.ColumnChart;
import com.google.gwt.visualization.client.visualizations.corechart.LineChart;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExerciseList;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.exercise.ListInterface;
import mitll.langtest.client.exercise.WaveformExercisePanelFactory;
import mitll.langtest.client.flashcard.Flashcard;
import mitll.langtest.client.flashcard.FlashcardExercisePanelFactory;
import mitll.langtest.client.grading.GradingExercisePanelFactory;
import mitll.langtest.client.mail.MailDialog;
import mitll.langtest.client.monitoring.MonitoringManager;
import mitll.langtest.client.recorder.FlashRecordPanelHeadless;
import mitll.langtest.client.recorder.MicPermission;
import mitll.langtest.client.scoring.GoodwaveExercisePanelFactory;
import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.client.sound.SoundManagerStatic;
import mitll.langtest.client.taboo.GiverExerciseFactory;
import mitll.langtest.client.taboo.ReceiverExerciseFactory;
import mitll.langtest.client.taboo.SinglePlayerRobot;
import mitll.langtest.client.taboo.Taboo;
import mitll.langtest.client.taboo.TabooExerciseList;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.client.user.UserNotification;
import mitll.langtest.client.user.UserTable;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Result;

import java.util.Date;
import java.util.Map;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class LangTest implements EntryPoint, UserFeedback, ExerciseController, UserNotification {
  public static final String LANGTEST_IMAGES = "langtest/images/";

  private Panel currentExerciseVPanel = new FluidContainer();
  private ListInterface exerciseList;
  private Label status = new Label();

  private UserManager userManager;
  private final UserTable userTable = new UserTable();
  private ResultManager resultManager;
  private MonitoringManager monitoringManager;
  private FlashRecordPanelHeadless flashRecordPanel;

  private long lastUser = -2;
  private String audioType = Result.AUDIO_TYPE_UNSET;

  private final LangTestDatabaseAsync service = GWT.create(LangTestDatabase.class);
  private final BrowserCheck browserCheck = new BrowserCheck();
  private SoundManagerStatic soundManager;
  private PropertyHandler props;
  private HTML userline;
  private Flashcard flashcard;
  Heading pageTitle;

  private Panel headerRow;
  private FluidRow secondRow;
  private ProgressHelper progressBar;

  private Anchor logout;
  private Anchor users;
  private Anchor showResults, monitoring;
  private Taboo taboo;

  /**
   * Make an exception handler that displays the exception.
   */
  public void onModuleLoad() {
    // set uncaught exception handler
    dealWithExceptions();
    final long then = System.currentTimeMillis();

    service.getProperties(new AsyncCallback<Map<String, String>>() {
      public void onFailure(Throwable caught) {
        if (caught instanceof IncompatibleRemoteServiceException) {
          Window.alert("This application has recently been updated.\nPlease refresh this page, or restart your browser." +
            "\nIf you still see this message, clear your cache. (" +caught.getMessage()+
            ")");
        } else {
          long now = System.currentTimeMillis();
          System.out.println("onModuleLoad.getProperties : (failure) took " + (now - then) + " millis");
          Window.alert("Couldn't contact server.  Please check your network connection.");
        }
      }

      public void onSuccess(Map<String, String> result) {
        long now = System.currentTimeMillis();
        props = new PropertyHandler(result);
        onModuleLoad2();
        if (isLogClientMessages()) {
          String message = "onModuleLoad.getProperties : (success) took " + (now - then) + " millis";
          logMessageOnServer(message);
        }
      }
    });
  }

  private void dealWithExceptions() {
    GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
      public void onUncaughtException(Throwable throwable) {
        ExceptionHandlerDialog exceptionHandlerDialog = new ExceptionHandlerDialog();
        String exceptionAsString = exceptionHandlerDialog.getExceptionAsString(throwable);
        logMessageOnServer("got browser exception : " + exceptionAsString);
        exceptionHandlerDialog.showExceptionInDialog(browserCheck, exceptionAsString);
      }
    });
  }

  private void logMessageOnServer(String message) {
    service.logMessage(message,
      new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {
          Window.alert("Couldn't contact server.  Please check your network connection.");
        }

        @Override
        public void onSuccess(Void result) {
        }
      });
  }

  /**
   * Use DockLayout to put a header at the top, exercise list on the left, and eventually
   * the current exercise in the center.  There is also a status on line on the bottom.
   *
   * Initially the flash record player is put in the center of the DockLayout
   */
  public void onModuleLoad2() {
    userManager = new UserManager(this,service, false, false, props);
    if (props.isTrackUsers()) taboo = new Taboo(userManager, service, this);
    loadVisualizationPackages();
    if (props.isFlashCard()) {
      loadFlashcard();
      return;
    }
    if (props.isDataCollectAdminView()) {
      GWT.runAsync(new RunAsyncCallback() {
        public void onFailure(Throwable caught) {
          Window.alert("Code download failed");
        }

        public void onSuccess() {
          doDataCollectAdminView();
        }
      });

      return;
    }

    if (props.isAdminView() || props.isGrading()) {
      final LangTest outer = this;
      GWT.runAsync(new RunAsyncCallback() {
        public void onFailure(Throwable caught) {
          Window.alert("Code download failed");
        }

        public void onSuccess() {
          resultManager = new ResultManager(service, outer, props.getNameForAnswer());
        }
      });

      GWT.runAsync(new RunAsyncCallback() {
        public void onFailure(Throwable caught) {
          Window.alert("Code download failed");
        }

        public void onSuccess() {
          monitoringManager = new MonitoringManager(service, props);
        }
      });
    }

    boolean usualLayout = !showOnlyOneExercise();
    Container widgets = new FluidContainer();

    if (usualLayout) {
      RootPanel.get().add(widgets);
    }
    DOM.setStyleAttribute(RootPanel.get().getElement(), "paddingTop", "2px");

    addResizeHandler();

    // header/title line
    // first row ---------------
    widgets.add(headerRow = makeHeaderRow());

    // second row ---------------
    secondRow = new FluidRow();
    widgets.add(secondRow);

    // third row ---------------

    Panel thirdRow = new HorizontalPanel();
    Panel leftColumn = new SimplePanel();
    thirdRow.add(leftColumn);
    widgets.add(thirdRow);

    if (isCRTDataCollectMode()) {
      addProgressBar(widgets);
    }
    else {
      widgets.add(status);
    }

    // set up center panel, initially with flash record panel
    currentExerciseVPanel = new FluidContainer();

    DOM.setStyleAttribute(currentExerciseVPanel.getElement(), "paddingLeft", "5px");
    DOM.setStyleAttribute(currentExerciseVPanel.getElement(), "paddingRight", "2px");
    makeExerciseList(secondRow, leftColumn);
    if (usualLayout) {
      currentExerciseVPanel.addStyleName("floatLeft");
      thirdRow.add(currentExerciseVPanel);
    }
    else {  // show fancy lace background image
      currentExerciseVPanel.addStyleName("body");
      currentExerciseVPanel.getElement().getStyle().setBackgroundImage("url("+ LANGTEST_IMAGES +"levantine_window_bg.jpg"+")");
      currentExerciseVPanel.addStyleName("noMargin");
      RootPanel.get().add(currentExerciseVPanel);
    }

    // don't do flash if we're doing text only collection
    //System.out.println("teacher view " + props.isTeacherView() + " arabic text data " + props.isArabicTextDataCollect() + " collect audio " + props.isCollectAudio());

    if (shouldCollectAudio()) {
      makeFlashContainer();
      currentExerciseVPanel.add(flashRecordPanel);
    }
    else {
      System.out.println("*not* allowing recording of audio.");
    }

    setPageTitle();
    browserCheck.checkForCompatibleBrowser();
    setupSoundManager();

    modeSelect();
  }

  private void loadFlashcard() {
    doFlashcard();
    addResizeHandler();
  }

  private void loadVisualizationPackages() {
    System.out.println("loadVisualizationPackages...");

    VisualizationUtils.loadVisualizationApi(new Runnable() {
      @Override
      public void run() {
        System.out.println("\tloaded VisualizationUtils...");
        logMessageOnServer("loaded VisualizationUtils.");
      }
    }, ColumnChart.PACKAGE, LineChart.PACKAGE);
  }

  private boolean shouldCollectAudio() {
    return props.isCollectAudio() && !props.isFlashcardTeacherView() || props.isFlashCard()  || props.isGoodwaveMode() ;
  }

  /**
   * @see #onModuleLoad2()
   * @return
   */
  private Panel makeHeaderRow() {
    FluidRow headerRow = new FluidRow();

    Widget title;
    if (isGoodwaveMode()) {
      flashcard = new Flashcard();
      title = flashcard.makeNPFHeaderRow(props.getSplash());
    }
    else if (props.isTrackUsers()) {
      flashcard = new Flashcard();
      title = flashcard.makeNPFHeaderRow(props.getSplash(), props.getAppTitle());
    }
    else {
      title = getTitleWidget();
    }

    boolean isStudent = getLoginType().equals(PropertyHandler.LOGIN_TYPE.STUDENT);
    boolean takeWholeWidth = isStudent || props.isFlashcardTeacherView() || props.isShowSections() || props.isGoodwaveMode();

    Column titleColumn = new Column(takeWholeWidth ? 12 : 10, title);
    headerRow.add(titleColumn);
    if (!takeWholeWidth) {
      headerRow.add(new Column(2, getLogout()));
    } else if (isStudent || props.isAdminView() || props.isDataCollectMode()) {
      FluidRow adminRow = new FluidRow();
      adminRow.addStyleName("alignCenter");
      adminRow.addStyleName("inlineStyle");

      this.userline = new HTML(getUserText());
      Anchor logout = getLogoutLink();
      HTML releaseStatus = getReleaseStatus();

      if (props.isAdminView()) {
        adminRow.add(new Column(2, userline));
        adminRow.add(new Column(2, logout));
        getLogout();
        adminRow.add(new Column(2, users));
        adminRow.add(new Column(2, showResults));
        adminRow.add(new Column(2, monitoring));
        adminRow.add(new Column(2, releaseStatus));
      }
      else {
        adminRow.add(new Column(1, userline));
        adminRow.add(new Column(2,  releaseStatus));
        adminRow.add(new Column(2, 7,  logout));
      }

      titleColumn.add(adminRow);
    }
    return headerRow;
  }

  private void addProgressBar(Panel widgets) {
    if (props.isGrading()) {
      widgets.add(status);
    } else {
      progressBar = new ProgressHelper();
      widgets.add(progressBar.getProgressBar());
    }
  }

  /**
   * @see mitll.langtest.client.exercise.SectionExerciseList#getEmailWidget()
   * @param subject
   * @param linkTitle
   * @param token
   */
  @Override
  public void showEmail(final String subject, final String linkTitle, final String token) {
    GWT.runAsync(new RunAsyncCallback() {
      public void onFailure(Throwable caught) {
        Window.alert("Code download failed");
      }

      public void onSuccess() {
        new MailDialog(service, userManager).showEmail(subject, linkTitle, token);
      }
    });
  }

  private Widget getTitleWidget() {
    FluidRow titleRow = new FluidRow();
    titleRow.addStyleName("alignCenter");
    titleRow.addStyleName("inlineStyle");
    pageTitle = new Heading(2, props.getAppTitle());

    titleRow.add(pageTitle);

    return titleRow;
  }

  /**
   * Set the page title and favicon.
   */
  private void setPageTitle() {
    String appTitle = props.getAppTitle();
    setTitle(appTitle);

    Element element = DOM.getElementById("favicon");   // set the page title to be consistent
    if (props.isFlashCard() || props.isFlashcardTeacherView()) {
      if (element != null) {
        element.setAttribute("href", "flashFavIcon.gif");
      }
    }
    else if (props.isGoodwaveMode() || props.isTrackUsers()) {
      if (element != null) {
        element.setAttribute("href", LANGTEST_IMAGES + "npfFavIcon.gif");
      }
    }
  }

  private void setTitle(String appTitle) {
    Element elementById = DOM.getElementById("title-tag");   // set the page title to be consistent
    if (elementById != null) {
      elementById.setInnerText(appTitle);
    }
  }

  /**
   * TODO : test without flash???
   * @see #onModuleLoad2()
   */
  private void doFlashcard() {
    setPageTitle();
    FluidContainer container = new FluidContainer();
    RootPanel.get().add(container);
    RootPanel.get().addStyleName("noPadding");
    currentExerciseVPanel = container;

    flashcard = new Flashcard();

    HorizontalPanel headerRow = flashcard.makeFlashcardHeaderRow(props.getSplash());
    container.add(headerRow);

    userManager = new UserManager(this, service, false, true, props);
    this.exerciseList = new ExerciseListLayout(props).makeFlashcardExerciseList(container, service, userManager);

    makeFlashContainer();

    Row flashRow = new FluidRow();
    container.add(flashRow);
    flashRow.addStyleName("whiteBackground");
    flashRow.add(new Column(1, flashRecordPanel));

    setupSoundManager();

    if (!props.isTimedGame()) {
      showHelpNewUser();
    }

    modeSelect();
  }

  private void showHelpNewUser() {
    Storage stockStore = Storage.getLocalStorageIfSupported();
    boolean showedHelpAlready = false;
    if (stockStore != null) {
      showedHelpAlready = stockStore.getItem("showedHelp") != null;
      stockStore.setItem("showedHelp", "showedHelp");
    }

    if (!showedHelpAlready) {
      showFlashHelp();
    }
  }

  @Override
  public void showFlashHelp() { flashcard.showFlashHelp(this); }

  private void doDataCollectAdminView() {
    setPageTitle();

    VerticalPanel vp = new VerticalPanel();
    VerticalPanel fp1 = new VerticalPanel();

    vp.addStyleName("grayColor");
    HTML title = new HTML("<h2>" + props.getAppTitle() + "</h2>");
    title.addStyleName("darkerBlueColor");
    title.addStyleName("grayColor");

    fp1.getElement().getStyle().setFloat(Style.Float.LEFT);
    fp1.add(title);

    users = makeUsersAnchor(true);
    fp1.add(users);
    userManager = new UserManager(this,service, props.isDataCollectAdminView(), false, props);

    logout = new Anchor("Logout");
    logout.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        userManager.clearUser();
        lastUser = -2;
        userManager.teacherLogin();
      }
    });
    fp1.add(logout);

    HTML statusLine = getReleaseStatus();
    fp1.add(statusLine);
    vp.add(fp1);
    this.userline = new HTML(getUserText());
    vp.add(userline);

    vp.add(currentExerciseVPanel);
    DataCollectAdmin dataCollectAdmin = new DataCollectAdmin(userManager, service);
    dataCollectAdmin.makeDataCollectNewSiteForm(currentExerciseVPanel);

    FlowPanel fp = new FlowPanel();
    fp.getElement().getStyle().setFloat(Style.Float.LEFT);
    fp.add(vp);
    RootPanel.get().add(fp);

    browserCheck.checkForCompatibleBrowser();
    userManager.teacherLogin();
  }

  /**
   * @see #doDataCollectAdminView()
   * @see #getLogout()
   * @see #makeHeaderRow()
   * @return
   */
  private HTML getReleaseStatus() {
    browserCheck.getBrowserAndVersion();

    String releaseDate = props.getReleaseDate() != null ? " " +
      props.getReleaseDate() : " 07/19_19";
    return new HTML("<span><font size=-2>" + browserCheck.browser + " " + browserCheck.ver + releaseDate + "</font></span>");
  }

  private void checkForAdminUser() {
    service.isAdminUser(userManager.getUser(), new AsyncCallback<Boolean>() {
      @Override
      public void onFailure(Throwable caught) {
        Window.alert("Can't contact server");
      }

      @Override
      public void onSuccess(Boolean result) {
        users.setVisible(result);
      }
    });
  }

  private void setupSoundManager() {
    soundManager = new SoundManagerStatic();
    soundManager.exportStaticMethods();
    soundManager.initialize();
  }

  /**
   * Tell the exercise list when the browser window changes size
   * @paramx widgets
   */
  private void addResizeHandler() {
    Window.addResizeHandler(new ResizeHandler() {
      public void onResize(ResizeEvent event) {
        exerciseList.onResize();
        if (flashcard != null) {
          flashcard.onResize();
        }
      }
    });
  }

  public float getScreenPortion() {
    return props.getScreenPortion();
  }

  /**
   * Supports different flavors of exercise list -- Paging, Grading, and vanilla.
   *
   * @see #onModuleLoad2()
   */
  private void makeExerciseList(FluidRow secondRow, Panel leftColumn) {
    this.exerciseList = new ExerciseListLayout(props).makeExerciseList(secondRow, leftColumn, this, currentExerciseVPanel,service,this);
  }

  public int getHeightOfTopRows() {
    return headerRow.getOffsetHeight() + secondRow.getOffsetHeight();
  }

  @Override
  public int getLeftColumnWidth() {
    return exerciseList.getWidget().getOffsetWidth();
  }

  public boolean showOnlyOneExercise() {
    return props.getExercise_title() != null;
  }

  /**
   * Check the URL parameters for special modes.
   *
   * If in goodwave (pronunciation scoring) mode or auto crt mode, skip the user login.
   */
  private void modeSelect() {
    boolean isGrading = props.isGrading();
    if (logout != null) logout.setVisible(!props.isGoodwaveMode() && !props.isFlashcardTeacherView());
    if (userline != null) {
      boolean isStudent = getLoginType().equals(PropertyHandler.LOGIN_TYPE.STUDENT);

      userline.setVisible(isStudent || (!props.isGoodwaveMode() && !props.isFlashcardTeacherView()));
    }
    if (users != null) users.setVisible(isGrading || props.isAdminView());
    if (showResults != null) showResults.setVisible(isGrading || props.isAdminView());
    if (monitoring != null) monitoring.setVisible(isGrading || props.isAdminView());

    System.out.println("modeSelect : goodwave mode " + props.isGoodwaveMode() + " auto crt mode = " + isAutoCRTMode());

    checkInitFlash();
  }

  /**
   * Hookup feedback for events from Flash generated from the user's response to the Mic Access dialog
   *
   * @see #onModuleLoad2()
   * @see mitll.langtest.client.recorder.FlashRecordPanelHeadless#micConnected()
   */
  private void makeFlashContainer() {
    flashRecordPanel = new FlashRecordPanelHeadless();

    FlashRecordPanelHeadless.setMicPermission(new MicPermission() {
      public void gotPermission() {
        System.out.println(new Date() + " : makeFlashContainer - got permission!");
        flashRecordPanel.hide();
        flashRecordPanel.hide2(); // must be a separate call!

        checkLogin();
      }

      public void gotDenial() {
        System.err.println("dude!!!!");
        showPopupOnDenial();
      }
    });
  }

  /**
   * This determines which kind of exercise we're going to do.
   * @see #gotUser(long)
   */
  public void setFactory(final long userID) {
    final LangTest outer =this;
    if (props.isGoodwaveMode() && !props.isGrading()) {
      exerciseList.setFactory(new GoodwaveExercisePanelFactory(service, outer, outer), userManager, 1);
    } else if (props.isGrading()) {
      exerciseList.setFactory(new GradingExercisePanelFactory(service, outer, outer), userManager, props.getNumGradesToCollect());
    } else if (props.isFlashCard()) {
      exerciseList.setFactory(new FlashcardExercisePanelFactory(service, outer, outer), userManager, 1);
    } else if (props.isDataCollectMode() && props.isCollectAudio() && !props.isCRTDataCollectMode()) {
      exerciseList.setFactory(new WaveformExercisePanelFactory(service, outer, outer), userManager, 1);
    } else {
      exerciseList.setFactory(new ExercisePanelFactory(service, outer, outer), userManager, 1);
    }
    doEverythingAfterFactory(userID);

    if (getLanguage().equalsIgnoreCase("Pashto")) {
      new FontChecker(this).checkPashto();
    }
  }

  private SinglePlayerRobot singlePlayerRobot;

  /**
   * @see Taboo#chooseRoleModal(long)
   * @see Taboo#checkForPartner(long)
   * @see
   * @param userID
   * @param isGiver
   * @param singlePlayer
   */
  public void setTabooFactory(long userID, boolean isGiver, boolean singlePlayer) {
    System.out.println("setTabooFactory : User " + userID + " is a giver " + isGiver + " single " + singlePlayer);
    ((TabooExerciseList)exerciseList).setGiver(isGiver);

    String appTitle = props.getAppTitle();
    String appTitle1 = appTitle + " : Giver";
    boolean changed;
    if (isGiver) {
      changed = (exerciseList.getFactory() instanceof ReceiverExerciseFactory);
      GiverExerciseFactory factory = new GiverExerciseFactory(service, this, this);
      exerciseList.setFactory(factory, userManager, 1);

    } else {
      changed = (exerciseList.getFactory() instanceof ReceiverExerciseFactory);

      if (singlePlayer && singlePlayerRobot == null) {
        singlePlayerRobot = new SinglePlayerRobot(service);
      }

      exerciseList.setFactory(new ReceiverExerciseFactory(service, this, this, singlePlayer ? singlePlayerRobot : null), userManager, 1);
      appTitle1 = appTitle + (singlePlayer ? " : Single Player" : " : Receiver");
    }
    setTitle(appTitle1);
    if (pageTitle == null) {
      flashcard.setAppTitle(appTitle1);
    }
    else {
      pageTitle.setText(appTitle1);
    }

    if (!doEverythingAfterFactory(userID) && changed) {
      exerciseList.getExercises(userID);
    }
  }

  /**
   * Show a popup telling how unhappy we are with the user's choice not to allow mic recording.
   *
   * Remove the flash player that was there, put in a new one, again, and ask the user again for permission.
   *
   */
  private void showPopupOnDenial() {
    final PopupPanel popupImage = new PopupPanel();
    popupImage.setAutoHideEnabled(true);
    final Image image = new Image(LANGTEST_IMAGES +"really.png");
    image.addLoadHandler(new LoadHandler() {
      public void onLoad(LoadEvent event) {
        // since the image has been loaded, the dimensions are known
        popupImage.center();
        // only now show the image
        popupImage.setVisible(true);
      }
    });

    popupImage.add(image);
    // hide the image until it has been fetched
    popupImage.setVisible(false);
    // this causes the image to be loaded into the DOM
    popupImage.center();

    Timer t = new Timer() {
      @Override
      public void run() {
        popupImage.hide();
        currentExerciseVPanel.remove(flashRecordPanel);

        flashRecordPanel.removeFlash();

        makeFlashContainer();
        currentExerciseVPanel.add(flashRecordPanel);
        flashRecordPanel.initFlash();
      }
    };

    // Schedule the timer to run once in 1 seconds.
    t.schedule(1000);
  }

  /**
   * Has both a logout and a users link and a results link
   * @return
   */
  private Widget getLogout() {
    VerticalPanel vp = new VerticalPanel();

    // add logout link

    this.userline = new HTML(getUserText());
    vp.add(userline);

    logout = getLogoutLink();
    vp.add(logout);

    makeUsersAnchor(false);
    vp.add(users);

    showResults = new Anchor("Results");
    showResults.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        GWT.runAsync(new RunAsyncCallback() {
          public void onFailure(Throwable caught) {
            Window.alert("Code download failed");
          }

          public void onSuccess() {
            resultManager.showResults();
          }
        });
      }
    });
    vp.add(showResults);
    showResults.setVisible(props.isAdminView());

    monitoring = new Anchor("Monitoring");
    monitoring.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        GWT.runAsync(new RunAsyncCallback() {
          public void onFailure(Throwable caught) {
            Window.alert("Code download failed");
          }

          public void onSuccess() {
            monitoringManager.showResults();
          }
        });
      }
    });
    vp.add(monitoring);
    monitoring.setVisible(props.isAdminView());

    // no click handler for this for now
    HTML statusLine = getReleaseStatus();
    vp.add(statusLine);

    return vp;
  }

  private Anchor getLogoutLink() {
    SafeHtmlBuilder b = new SafeHtmlBuilder();
    String s = "<font size=+1>Sign Out</font>";
    b.appendHtmlConstant(s);
    Anchor logout = new Anchor(b.toSafeHtml());
    logout.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        resetState();
      }
    });
    return logout;
  }

  private Anchor makeUsersAnchor(final boolean isDataCollectAdminView) {
    users = new Anchor("Users");
    users.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        userTable.showUsers(service, userManager.getUser(), isDataCollectAdminView);
      }
    });
    users.setVisible(props.isAdminView());
    return users;
  }

  private String getUserText() {
    String greeting = userManager.getUserID() == null ? "" : ("Hello " + userManager.getUserID());
    return "<span><font size=-2>" + greeting + "</font></span>";
  }

  /**
   * @see #getLogout()
   */
  private void resetState() {
    userManager.clearUser();
    exerciseList.removeCurrentExercise();
    exerciseList.clear();
    lastUser = -2;
    modeSelect();
  }

  /**
   * @see #checkLogin()
   * @see ExerciseList#checkBeforeLoad(mitll.langtest.shared.ExerciseShell)
   */
  public void login() {
    PropertyHandler.LOGIN_TYPE loginType = getLoginType();

    System.out.println("loginType " + loginType +
      " data collect mode " + props.isDataCollectMode() +
      " crt data collect " + props.isCRTDataCollectMode() +
      " teacher " + props.isTeacherView() + " grading " + props.isGrading());

    if (loginType.equals(PropertyHandler.LOGIN_TYPE.STUDENT)) {
      userManager.login();
    }
    else if (loginType.equals(PropertyHandler.LOGIN_TYPE.DATA_COLLECTOR)) {
      userManager.teacherLogin();
    } // next has already been done in checkLogin
  /*  else if (loginType.equals(PropertyHandler.LOGIN_TYPE.ANONYMOUS)) {
      userManager.teacherLogin();
    }*/
    else if (!props.isFlashCard() && ((props.isDataCollectMode() && !props.isCRTDataCollectMode()) || props.isTeacherView() || props.isGrading())) {
      System.out.println("doing teacher login");
      userManager.teacherLogin();
    }
    else {
      System.out.println("doing student login");

      userManager.login();
    }
  }

  /**
   * Init Flash recorder once we login.
   *
   * Only get the exercises if the user has accepted mic access.
   *
   * @see #makeFlashContainer
   * @see UserManager#login
   * @see UserManager#storeUser
   * @param userID
   */
  public void gotUser(long userID) {
    System.out.println("gotUser : got user " + userID);
    if (userline != null) userline.setHTML(getUserText());
    if (props.isDataCollectAdminView()) {
      checkForAdminUser();
    } else {
      if (props.isTrackUsers()) { // OK we're playing taboo!
        taboo.initialCheck(userID);
      } else {
        setFactory(userID);
      }
    }
  }

  private boolean doEverythingAfterFactory(long userID) {
    if (userID != lastUser || (props.isGoodwaveMode() || props.isFlashCard() && !props.isTimedGame())) {
      System.out.println("doEverythingAfterFactory : user changed - new " + userID + " vs last " + lastUser);
      if (!shouldCollectAudio() || flashRecordPanel.gotPermission()) {
        System.out.println("\tdoEverythingAfterFactory : " + userID + " get exercises");
        exerciseList.getExercises(userID);
      }
      lastUser = userID;
      return true;
    } else if (props.isTimedGame()) {
      exerciseList.reloadExercises();
      return true;
    } else return false;
  }

  /**
   * @see #modeSelect()
   */
  // TODO : refactor all this into mode objects that decide whether we need flash or not, etc.
  private void checkInitFlash() {
    if (shouldCollectAudio() && !flashRecordPanel.gotPermission()) {
      System.out.println("checkInitFlash : initFlash");

      flashRecordPanel.initFlash();
    }
    else {
      System.out.println("checkInitFlash : skip init flash, just checkLogin");

      checkLogin();
    }
  }

  /**
   * Called after the user clicks "Yes" in flash mic permission dialog.
   *
   * @see #checkInitFlash()
   * @see #makeFlashContainer()
   */
  private void checkLogin() {
    PropertyHandler.LOGIN_TYPE loginType = getLoginType();

    if (loginType.equals(PropertyHandler.LOGIN_TYPE.ANONYMOUS)) { // explicit setting of login type
      userManager.anonymousLogin();
    } else if (loginType.equals(PropertyHandler.LOGIN_TYPE.UNDEFINED) && // no explicit setting, so it's dependent on the mode
      (props.isGoodwaveMode() || isAutoCRTMode() || (props.isFlashcardTeacherView() && !props.isFlashCard()))) {   // no login for pron mode
      userManager.anonymousLogin();
    } else {
      if (props.isTimedGame()) {
        flashcard.showTimedGameHelp(this);
      } else {
        login();
      }
    }
  }

  @Override
  public void rememberAudioType(String audioType) {
    this.audioType = audioType;
  }

  @Override
  public String getAudioType() {
    return audioType;
  }

  /**
   * @see mitll.langtest.client.exercise.PostAnswerProvider#postAnswers
   * @see mitll.langtest.client.recorder.SimpleRecordPanel#stopRecording()
   * @return
   */
  public int getUser() { return userManager.getUser(); }
  public void pingAliveUser() { userManager.userAlive(); }
  public PropertyHandler getProps() { return props; }
  public boolean getEnglishOnly() { return props.isEnglishOnlyMode(); }
  public int getNumGradesToCollect() { return props.getNumGradesToCollect(); }
  public int getSegmentRepeats() { return props.getSegmentRepeats(); }
  public boolean isArabicTextDataCollect() {  return props.isArabicTextDataCollect(); }
  public boolean useBkgColorForRef() {  return props.isBkgColorForRef(); }
  public boolean isDemoMode() {  return props.isDemoMode(); }
  public boolean isAutoCRTMode() {  return props.isAutocrt(); }
  public int getRecordTimeout() {  return props.getRecordTimeout(); }
  public boolean isDataCollectMode() {  return props.isDataCollectMode(); }
  public boolean isCRTDataCollectMode() {  return props.isCRTDataCollectMode(); }
  public boolean isCollectAudio() {  return props.isCollectAudio(); }
  public boolean isMinimalUI() {  return props.isMinimalUI(); }
  public boolean isGrading() {  return props.isGrading(); }
  public boolean isLogClientMessages() {  return props.isLogClientMessages(); }
  public String getLanguage() {  return props.getLanguage(); }
  public boolean isPromptBeforeNextItem() {  return props.isPromptBeforeNextItem(); }
  public boolean isRightAlignContent() {  return props.isRightAlignContent(); }
  public boolean isFlashCard() {  return props.isFlashCard(); }
  public boolean isGoodwaveMode() {  return props.isGoodwaveMode(); }
  public boolean shouldAddRecordKeyBinding() { return props.shouldAddRecordKeyBinding(); }
  public int getFlashcardPreviewFrameHeight() { return props.getFlashcardPreviewFrameHeight(); }
  public LangTestDatabaseAsync getService() { return service; }
  public UserFeedback getFeedback() { return this; }

  private PropertyHandler.LOGIN_TYPE getLoginType() { return props.getLoginType(); }

  // recording methods...
  /**
   * Recording interface
   */
  public void startRecording() {
    flashRecordPanel.recordOnClick();
  }

  /**
   * Recording interface
   */
  public void stopRecording() {
    flashRecordPanel.stopRecording();
  }

  /**
   * Recording interface
   */
  public String getBase64EncodedWavFile() {
    return flashRecordPanel.getWav();
  }

  public SoundManagerAPI getSoundManager() {
    return soundManager;
  }

  public void showErrorMessage(String title,String msg) {
    DialogHelper dialogHelper = new DialogHelper(false);
    dialogHelper.showErrorMessage(title, msg);
  }

  public void showStatus(String msg) { status.setText(msg); }

  public boolean loadNextExercise(Exercise current) {
    if (progressBar != null) {
      progressBar.showAdvance(exerciseList);
    }
    return exerciseList.loadNextExercise(current);
  }
  public boolean loadPreviousExercise(Exercise current) { return exerciseList.loadPreviousExercise(current);  }
  public boolean onFirst(Exercise current) { return exerciseList.onFirst(current); }
  public void addAdHocExercise(String label) { exerciseList.addAdHocExercise(label); }
}
