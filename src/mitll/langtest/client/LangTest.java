package mitll.langtest.client;

import com.github.gwtbootstrap.client.ui.*;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.visualization.client.VisualizationUtils;
import com.google.gwt.visualization.client.visualizations.corechart.ColumnChart;
import com.google.gwt.visualization.client.visualizations.corechart.LineChart;
import mitll.langtest.client.bootstrap.BootstrapFlashcardExerciseList;
import mitll.langtest.client.bootstrap.FlexSectionExerciseList;
import mitll.langtest.client.bootstrap.TableSectionExerciseList;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExerciseList;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.exercise.GradedExerciseList;
import mitll.langtest.client.exercise.ListInterface;
import mitll.langtest.client.exercise.PagingExerciseList;
import mitll.langtest.client.exercise.WaveformExercisePanelFactory;
import mitll.langtest.client.flashcard.FlashcardExerciseList;
import mitll.langtest.client.flashcard.FlashcardExercisePanelFactory;
import mitll.langtest.client.grading.GradingExercisePanelFactory;
import mitll.langtest.client.mail.MailDialog;
import mitll.langtest.client.monitoring.MonitoringManager;
import mitll.langtest.client.recorder.FlashRecordPanelHeadless;
import mitll.langtest.client.recorder.MicPermission;
import mitll.langtest.client.scoring.GoodwaveExercisePanelFactory;
import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.client.sound.SoundManagerStatic;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.client.user.UserNotification;
import mitll.langtest.client.user.UserTable;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.Result;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class LangTest implements EntryPoint, UserFeedback, ExerciseController, UserNotification {
  public static final String LANGTEST_IMAGES = "langtest/images/";
  public static final String RECORDING_KEY = "SPACE BAR";
  private final DialogHelper dialogHelper = new DialogHelper(false);
  private final TimedGame timedGame = new TimedGame(this);
  private final Flashcard flashcard = new Flashcard();

  private Panel currentExerciseVPanel = new FluidContainer();
  private ListInterface exerciseList;

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

  /**
   * Make an exception handler that displays the exception.
   */
  public void onModuleLoad() {
    // set uncaught exception handler
    GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
      public void onUncaughtException(Throwable throwable) {
        new ExceptionHandlerDialog(browserCheck,throwable);
      }
    });
    final long then = System.currentTimeMillis();

    service.getProperties(new AsyncCallback<Map<String, String>>() {
      public void onFailure(Throwable caught) {
        long now = System.currentTimeMillis();
        System.out.println("onModuleLoad.getProperties : (failure) took " + (now - then) + " millis");
        Window.alert("Couldn't contact server.  Please check your network connection.");
      }

      public void onSuccess(Map<String, String> result) {
        long now = System.currentTimeMillis();
//        System.out.println("onModuleLoad.getProperties : (success) took " + (now - then) + " millis");

        props = new PropertyHandler(result);
        onModuleLoad2();
        if (isLogClientMessages()) {
          service.logMessage("onModuleLoad.getProperties : (success) took " + (now - then) + " millis",
            new AsyncCallback<Void>() {
              @Override
              public void onFailure(Throwable caught) {
              }

              @Override
              public void onSuccess(Void result) {
              }
            });
        }
      }
    });
  }

  private FluidRow headerRow, secondRow;

  /**
   * Use DockLayout to put a header at the top, exercise list on the left, and eventually
   * the current exercise in the center.  There is also a status on line on the bottom.
   *
   * Initially the flash record player is put in the center of the DockLayout
   */
  public void onModuleLoad2() {
    if (props.isFlashCard()) {
      doFlashcard();
      return;
    }
    if (props.isDataCollectAdminView()) {
      doDataCollectAdminView();
      return;
    }

    if (props.isAdminView()) {
      VisualizationUtils.loadVisualizationApi(new Runnable() {
        @Override
        public void run() {
          System.out.println("\tloaded VisualizationUtils...");
        }
      }, ColumnChart.PACKAGE, LineChart.PACKAGE);
    }

    userManager = new UserManager(this,service, isCollectAudio(), false, isCRTDataCollectMode() || isArabicTextDataCollect(), props.getAppTitle(),false);
    resultManager = new ResultManager(service, this, props.getNameForAnswer());
    monitoringManager = new MonitoringManager(service, props);
    boolean usualLayout = !showOnlyOneExercise();
    Container widgets = new FluidContainer();

    if (usualLayout) {
      RootPanel.get().add(widgets);
      DOM.setStyleAttribute(RootPanel.get().getElement(), "paddingTop", "2px");
    }

    addResizeHandler();

    // header/title line
    // first row ---------------
    widgets.add(headerRow = makeHeaderRow());

    // second row ---------------
    secondRow = new FluidRow();
  //  secondRow.ensureDebugId("secondRow");

    widgets.add(secondRow);
    // third row ---------------

    Panel thirdRow = new HorizontalPanel();
    Panel leftColumn = new SimplePanel();
    thirdRow.add(leftColumn);
    widgets.add(thirdRow);

    // set up center panel, initially with flash record panel
    currentExerciseVPanel = new FluidContainer();

    DOM.setStyleAttribute(currentExerciseVPanel.getElement(), "marginLeft", "10px");
    DOM.setStyleAttribute(currentExerciseVPanel.getElement(), "paddingRight", "2px");
    makeExerciseList(secondRow, leftColumn, props.isGrading());
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

    // set up left side exercise list

    // don't do flash if we're doing text only collection
    //System.out.println("teacher view " + props.isTeacherView() + " arabic text data " + props.isArabicTextDataCollect() + " collect audio " + props.isCollectAudio());

    if (props.isCollectAudio() && !props.isFlashcardTeacherView() || props.isFlashCard()) {
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

  private FluidRow makeHeaderRow() {
    FluidRow headerRow = new FluidRow();

    Widget title = getTitleWidget();
    Column titleColumn = new Column(props.isFlashcardTeacherView() ? 12 : 10, title);
    headerRow.add(titleColumn);
    if (!props.isFlashcardTeacherView()) {
      headerRow.add(new Column(2, getLogout()));
    }
    else if (props.isAdminView()) {
      getLogout();
      FluidRow adminRow = new FluidRow();
      adminRow.addStyleName("alignCenter");
      adminRow.addStyleName("inlineStyle");
      adminRow.add(new Column(1, 2,users));
      adminRow.add(new Column(1, 2, showResults));
      adminRow.add(new Column(1, 2, monitoring));
      titleColumn.add(adminRow);
    }
    //widgets.add(headerRow);
    return headerRow;
  }

  /**
   * @see mitll.langtest.client.exercise.SectionExerciseList#getEmailWidget()
   * @param subject
   * @param linkTitle
   * @param token
   */
  @Override
  public void showEmail(final String subject, final String linkTitle, final String token) {
    new MailDialog(service, userManager).showEmail(subject, linkTitle, token);
  }

  private Widget getTitleWidget() {
    FluidRow titleRow = new FluidRow();
    titleRow.addStyleName("alignCenter");
    titleRow.addStyleName("inlineStyle");
    Heading w = new Heading(2, props.getAppTitle());

    titleRow.add(w);

    return titleRow;
  }

  private void setPageTitle() {
    Element elementById = DOM.getElementById("title-tag");   // set the page title to be consistent
    if (elementById != null) {
      elementById.setInnerText(props.getAppTitle());
    }
  }

  /**
   * TODO : test without flash???
   */
  private void doFlashcard() {
    setPageTitle();
    FluidContainer container = new FluidContainer();
    RootPanel.get().add(container);
    RootPanel.get().addStyleName("noPadding");
    currentExerciseVPanel = container;

    HorizontalPanel headerRow = flashcard.makeFlashcardHeaderRow(props.getSplash());
    container.add(headerRow);

    userManager = new UserManager(this, service, isCollectAudio(), false, isCRTDataCollectMode(), props.getAppTitle(), true);
    this.exerciseList = new BootstrapFlashcardExerciseList(container, service, userManager,
      props.isTimedGame(), props.getGameTimeSeconds());

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

  private void showTimedGameHelp() {
    timedGame.showTimedGameHelp(props);
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
  public void showFlashHelp() {
    if (props.isTimedGame()) {
      showTimedGameHelp();
    } else {
      List<String> msgs = new ArrayList<String>();
      msgs.add("Practice your vocabulary by saying the matching " + props.getLanguage() + " phrase.");
      msgs.add("Press and hold the " + RECORDING_KEY + " to record.");
      msgs.add("Release to stop recording.");
      dialogHelper.showErrorMessage("Help", msgs);
    }
  }

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

    users = new Anchor("Users");
    users.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        userTable.showUsers(service, userManager.getUser(), true);
      }
    });
    fp1.add(users);
    userManager = new UserManager(this,service, isCollectAudio(), props.isDataCollectAdminView(), false, props.getAppTitle(),false);

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

  private HTML getReleaseStatus() {
    browserCheck.getBrowserAndVersion();

    String releaseDate = props.getReleaseDate() != null ? " " +
      props.getReleaseDate() : "";
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
   * @paramx exerciseListPanel to add scroller to
   * @param isGrading true if grading, false if not
   */
  private void makeExerciseList(FluidRow secondRow,Panel leftColumn, boolean isGrading) {
    final UserFeedback feedback = (UserFeedback) this;

    boolean hideExerciseList = (props.isMinimalUI() && !props.isGrading()) && !props.isAdminView();
    this.exerciseList = makeExerciseList(secondRow, isGrading, feedback);

    if (hideExerciseList) {
      exerciseList.getWidget().setVisible(false);
      exerciseList.getWidget().setWidth("1px");
    }

    if (showOnlyOneExercise()) {
      exerciseList.setExercise_title(props.getExercise_title());
    }
    addExerciseListOnLeftSide(leftColumn);
  }

  private ListInterface makeExerciseList(FluidRow secondRow, boolean isGrading, final UserFeedback feedback) {
    if (isGrading) {
      return new GradedExerciseList(currentExerciseVPanel, service, feedback,
        true, props.isEnglishOnlyMode(), this);
    } else {
      if (props.isShowSections()) {
        boolean showSectionWidgets = props.isShowSectionWidgets();
        if (props.isFlashcardTeacherView()) {
          return new TableSectionExerciseList(secondRow, currentExerciseVPanel, service, feedback,
            props.isShowTurkToken(), isAutoCRTMode(), showSectionWidgets, this);
        } else {
          return new FlexSectionExerciseList(secondRow, currentExerciseVPanel, service, feedback,
            props.isShowTurkToken(), isAutoCRTMode(), showSectionWidgets, this);
        }
      } else {
        return new PagingExerciseList(currentExerciseVPanel, service, feedback,
          props.isShowTurkToken(), isAutoCRTMode(), this) {
          @Override
          protected void checkBeforeLoad(ExerciseShell e) {
          } // don't try to login
        };
      }
    }
  }

  private void addExerciseListOnLeftSide(Panel leftColumnContainer) {
    if (props.isTeacherView()) {
      leftColumnContainer.add(exerciseList.getWidget());
    } else {

      Heading items = new Heading(4, "Items");
      items.addStyleName("center");

      FlowPanel leftColumn = new FlowPanel();
      leftColumn.addStyleName("floatLeft");
      DOM.setStyleAttribute(leftColumn.getElement(), "paddingRight", "10px");

      leftColumnContainer.add(leftColumn);
      leftColumnContainer.addStyleName("inlineStyle");
      if (!props.isFlashcardTeacherView()) {
        leftColumn.add(items);
      }
      leftColumn.add(exerciseList.getWidget());
    }
  }

  public int getHeightOfTopRows() {
    return headerRow.getOffsetHeight() + secondRow.getOffsetHeight();
  }

  @Override
  public int getLeftColumnWidth() {
    int offsetWidth = exerciseList.getWidget().getOffsetWidth();
  //  System.out.println("left col width " +offsetWidth);
    return offsetWidth;
  }

  public boolean showOnlyOneExercise() {
    return props.getExercise_title() != null /*|| props.isFlashCard()*/;
  }

  /**
   * Check the URL parameters for special modes.
   *
   * If in goodwave (pronunciation scoring) mode or auto crt mode, skip the user login.
   */
  private void modeSelect() {
    boolean isGrading = props.isGrading();
    if (logout != null) logout.setVisible(!props.isGoodwaveMode() && !props.isFlashcardTeacherView());
    if (userline != null) userline.setVisible(!props.isGoodwaveMode() && !props.isFlashcardTeacherView());
    if (users != null) users.setVisible(isGrading || props.isAdminView());
    if (showResults != null) showResults.setVisible(isGrading || props.isAdminView());
    if (monitoring != null) monitoring.setVisible(isGrading || props.isAdminView());

    System.out.println("modeSelect : goodwave mode " + props.isGoodwaveMode() + " " + isAutoCRTMode());

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

  private void setFactory() {
    if (props.isGoodwaveMode()) {
      exerciseList.setFactory(new GoodwaveExercisePanelFactory(service, this, this), userManager, 1);
    } else if (props.isGrading()) {
      exerciseList.setFactory(new GradingExercisePanelFactory(service, this, this), userManager, props.getNumGradesToCollect());
    } else if (props.isFlashCard()) {
      exerciseList.setFactory(new FlashcardExercisePanelFactory(service, this, this), userManager, 1);
    } else if (props.isDataCollectMode() && props.isCollectAudio() && !props.isCRTDataCollectMode()) {
      exerciseList.setFactory(new WaveformExercisePanelFactory(service, this, this), userManager, 1);
    } else {
      exerciseList.setFactory(new ExercisePanelFactory(service, this, this), userManager, 1);
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

  private Anchor logout;
  private Anchor users;
  private Anchor showResults, monitoring;
  /**
   * Has both a logout and a users link and a results link
   * @return
   */
  private Widget getLogout() {
    VerticalPanel vp = new VerticalPanel();

    // add logout link

    this.userline = new HTML(getUserText());
    vp.add(userline);

    logout = new Anchor("Logout");
    vp.add(logout);
    logout.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        resetState();
      }
    });

    users = new Anchor("Users");
    users.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        userTable.showUsers(service, userManager.getUser(), false);
      }
    });
    vp.add(users);
    users.setVisible(props.isAdminView());

    showResults = new Anchor("Results");
    showResults.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        resultManager.showResults();
      }
    });
    vp.add(showResults);
    showResults.setVisible(props.isAdminView());

    monitoring = new Anchor("Monitoring");
    monitoring.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        monitoringManager.showResults();
      }
    });
    vp.add(monitoring);
    monitoring.setVisible(props.isAdminView());

    // no click handler for this for now
    HTML statusLine = getReleaseStatus();
    vp.add(statusLine);

    return vp;
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
   * @see #modeSelect()
   * @see ExerciseList#checkBeforeLoad(mitll.langtest.shared.ExerciseShell)
   */
  public void login() {
    System.out.println("data collect mode " + props.isDataCollectMode() +
      " crt data collect " + props.isCRTDataCollectMode() +
      " teacher " + props.isTeacherView() + " grading " +props.isGrading());
    if ((props.isDataCollectMode() && !props.isCRTDataCollectMode()) || props.isTeacherView() || props.isGrading()) {
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
    System.out.println("gotUser : got user " +userID);
    if (userline != null) userline.setHTML(getUserText());
    if (props.isDataCollectAdminView()) {
      checkForAdminUser();
    } else {
      setFactory();

      if (userID != lastUser || (props.isGoodwaveMode() || props.isFlashCard() && !props.isTimedGame())) {
        System.out.println("gotUser : user changed - new " + userID + " vs last " + lastUser);
        if (!props.isCollectAudio() || flashRecordPanel.gotPermission()) {
          System.out.println("\tgotUser : " + userID + " get exercises");
          exerciseList.getExercises(userID);
        }
        lastUser = userID;
      }
      else if (props.isTimedGame()) {
        exerciseList.reloadExercises();
      }
    }
  }

  // TODO : refactor all this into mode objects that decide whether we need flash or not, etc.
  private void checkInitFlash() {
    if ((props.isCollectAudio() || props.isGoodwaveMode() || props.isFlashCard()) && !flashRecordPanel.gotPermission()) {
      System.out.println("checkInitFlash : initFlash");

      flashRecordPanel.initFlash();
    }
    else {
      System.out.println("checkInitFlash : skip init flash, just checkLogin");

      checkLogin();
    }
  }

  private void checkLogin() {
    if (props.isGoodwaveMode() || isAutoCRTMode() || props.isFlashCard() || props.isFlashcardTeacherView()) {   // no login for pron mode
      gotUser(-1);
    }
    else {
      if (props.isTimedGame())  {
        showTimedGameHelp();
      }
      else {
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
   * @see mitll.langtest.client.exercise.ExercisePanel#postAnswers
   * @see mitll.langtest.client.recorder.SimpleRecordPanel#stopRecording()
   * @return
   */
  public int getUser() { return userManager.getUser(); }
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
    dialogHelper.showErrorMessage(title, msg);
  }

    /**
     * Note : depends on bootstrap
     * @param title
     * @param msgs
     */
  @Override
  public void showErrorMessage(String title, List<String> msgs, String buttonName, final DialogHelper.CloseListener listener) {
    dialogHelper.showErrorMessage(title, msgs, buttonName, listener);
  }

  public void showStatus(String msg) {
    //status.setText(msg);
  }

  public boolean loadNextExercise(Exercise current) { return exerciseList.loadNextExercise(current);  }
  public boolean loadPreviousExercise(Exercise current) { return exerciseList.loadPreviousExercise(current);  }
  public boolean onFirst(Exercise current) { return exerciseList.onFirst(current); }
}
