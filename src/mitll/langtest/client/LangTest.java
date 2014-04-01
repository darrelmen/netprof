package mitll.langtest.client;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.Container;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Tab;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.visualization.client.VisualizationUtils;
import com.google.gwt.visualization.client.visualizations.corechart.ColumnChart;
import com.google.gwt.visualization.client.visualizations.corechart.LineChart;
import mitll.langtest.client.custom.Combined;
import mitll.langtest.client.custom.CommentNPFExercise;
import mitll.langtest.client.custom.Navigation;
import mitll.langtest.client.custom.QCNPFExercise;
import mitll.langtest.client.custom.TabContainer;
import mitll.langtest.client.dialog.DialogHelper;
import mitll.langtest.client.dialog.ExceptionHandlerDialog;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.flashcard.Flashcard;
import mitll.langtest.client.grading.GradingExercisePanelFactory;
import mitll.langtest.client.instrumentation.ButtonFactory;
import mitll.langtest.client.instrumentation.EventLogger;
import mitll.langtest.client.instrumentation.EventMock;
import mitll.langtest.client.instrumentation.EventTable;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.monitoring.MonitoringManager;
import mitll.langtest.client.recorder.FlashRecordPanelHeadless;
import mitll.langtest.client.recorder.MicPermission;
import mitll.langtest.client.result.ResultManager;
import mitll.langtest.client.scoring.GoodwaveExercisePanelFactory;
import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.client.sound.SoundManagerStatic;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.client.user.UserNotification;
import mitll.langtest.client.user.UserTable;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.ImageResponse;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.StartupInfo;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class LangTest implements EntryPoint, UserFeedback, ExerciseController, UserNotification {
  public static final String LANGTEST_IMAGES = "langtest/images/";
  private static final String DIVIDER = "|";
  private static final String NEW_PRO_F2_PNG = "NewProF2.png";

  private Panel currentExerciseVPanel = new FluidContainer();
  private ListInterface exerciseList;
  private final Label status = new Label();

  private UserManager userManager;
  private ResultManager resultManager;
  private MonitoringManager monitoringManager;
  private FlashRecordPanelHeadless flashRecordPanel;

  private long lastUser = -2;
  private String audioType = Result.AUDIO_TYPE_UNSET;

  private final LangTestDatabaseAsync service = GWT.create(LangTestDatabase.class);
  private final BrowserCheck browserCheck = new BrowserCheck();
  private SoundManagerStatic soundManager;
  private PropertyHandler props;
  private Flashcard flashcard;

  private Panel headerRow;
  private FluidRow secondRow;
  private ProgressHelper progressBar;

  private StartupInfo startupInfo;

  private TabContainer navigation;
  private EventLogger buttonFactory;
/*  private boolean showUnansweredFirst = false;
  private boolean showRerecord = false;*/

  /**
   * Make an exception handler that displays the exception.
   */
  public void onModuleLoad() {
    // set uncaught exception handler
    dealWithExceptions();
    final long then = System.currentTimeMillis();

    service.getStartupInfo(new AsyncCallback<StartupInfo>() {
      public void onFailure(Throwable caught) {
        if (caught instanceof IncompatibleRemoteServiceException) {
          Window.alert("This application has recently been updated.\nPlease refresh this page, or restart your browser." +
            "\nIf you still see this message, clear your cache. (" + caught.getMessage() +
            ")");
        } else {
          long now = System.currentTimeMillis();
          String message = "onModuleLoad.getProperties : (failure) took " + (now - then) + " millis";

          System.out.println(message);
          if (!caught.getMessage().trim().equals("0")) {
            System.out.println("Exception " + caught.getMessage() + " " + caught + " " + caught.getClass() + " " + caught.getCause());
            Window.alert("Couldn't contact server.  Please check your network connection. (getProperties)");
            logMessageOnServer(message);
          }
        }
      }

      public void onSuccess(StartupInfo startupInfo2) {
        long now = System.currentTimeMillis();

        startupInfo = startupInfo2;
        props = new PropertyHandler(startupInfo2.getProperties());
        if (isLogClientMessages()) {
          String message = "onModuleLoad.getProperties : (success) took " + (now - then) + " millis";
          logMessageOnServer(message);
        }

        onModuleLoad2();
      }
    });
  }

  /**
   * Log browser exception on server, include user and exercise ids.  Consider including chapter selection.
   * @see #onModuleLoad()
   */
  private void dealWithExceptions() {
    GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
      public void onUncaughtException(Throwable throwable) {
        String exceptionAsString = logException(throwable);
        if (exceptionAsString.length() > 0) {
          new ExceptionHandlerDialog().showExceptionInDialog(browserCheck, exceptionAsString);
        }
      }
    });
  }

  private boolean lastWasStackOverflow = false;

  public String logException(Throwable throwable) {
    String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(throwable);
    boolean isStackOverflow = exceptionAsString.contains("Maximum call stack size exceeded");
    if (isStackOverflow && lastWasStackOverflow) { // we get overwhelmed by repeated exceptions
      return ""; // skip repeat exceptions
    }
    else {
      lastWasStackOverflow = isStackOverflow;
    }
    String prefix = "got browser exception : ";
    logMessageOnServer(exceptionAsString, prefix);
    return exceptionAsString;
  }

  public void logMessageOnServer(String message, String prefix) {
    int user = userManager != null ? userManager.getUser() : -1;
    String exerciseID = exerciseList != null ? exerciseList.getCurrentExerciseID() : "Unknown";
    String suffix = " browser " + browserCheck.getBrowserAndVersion() +
      " : " + message;
    logMessageOnServer(prefix +
      "user #" + user +
      " exercise " + exerciseID +
      suffix);

    String toSend = prefix + suffix;
    if (toSend.length() > 100) {
      toSend = toSend.substring(0,100)+"...";
    }
    getButtonFactory().logEvent("unknown","unknown",exerciseID,toSend,user);
  }

  private void logMessageOnServer(String message) {
    service.logMessage(message,
      new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {}

        @Override
        public void onSuccess(Void result) {}
      }
    );
  }

  private Panel belowFirstRow;
  private Panel bothSecondAndThird;

  /**
   * @see mitll.langtest.client.scoring.AudioPanel#getImageURLForAudio(String, String, int, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck)
   * @param reqid
   * @param path
   * @param type
   * @param toUse
   * @param height
   * @param exerciseID
   * @param client
   */
  @Override
  public void getImage(int reqid, final String path, final String type, int toUse, int height, String exerciseID, AsyncCallback<ImageResponse> client) {
    String key = path+ DIVIDER +type+ DIVIDER +toUse+ DIVIDER +height+ DIVIDER+exerciseID;
    getImage(reqid,key, client);
  }

  private void getImage(int reqid,String key, AsyncCallback<ImageResponse> client) {
    String[] split = key.split("\\|");

    String path = split[0];
    String type = split[1];
    int toUse=Integer.parseInt(split[2]);
    int height=Integer.parseInt(split[3]);
    String exerciseID = split[4];

    getImage(reqid,key,path,type,toUse,height, exerciseID, client);
  }

  private void getImage(int reqid, final String key, String path, final String type, int toUse, int height,
                        String exerciseID, final AsyncCallback<ImageResponse> client) {

    ImageResponse ifPresent = imageCache.getIfPresent(key);
    if (ifPresent != null) {
      //System.out.println("found  " + ifPresent);

      ifPresent.req = -1;
      client.onSuccess(ifPresent);
    } else {
      service.getImageForAudioFile(reqid, path, type, toUse, height, exerciseID, new AsyncCallback<ImageResponse>() {
        public void onFailure(Throwable caught) {
          if (!caught.getMessage().trim().equals("0")) {
            Window.alert("getImageForAudioFile Couldn't contact server. Please check network connection.");
          }
          System.out.println("message " + caught.getMessage() + " " + caught);
          client.onFailure(caught);
        }

        public void onSuccess(ImageResponse result) {
          imageCache.put(key, result);
          client.onSuccess(result);
        }
      });
    }
  }

  private Cache<String, ImageResponse> imageCache = CacheBuilder.newBuilder()
    .maximumSize(100)
    .expireAfterWrite(7, TimeUnit.DAYS)
    .build();

  /**
   * Use DockLayout to put a header at the top, exercise list on the left, and eventually
   * the current exercise in the center.  There is also a status on line on the bottom.
   *
   * Initially the flash record player is put in the center of the DockLayout
   */
  private void onModuleLoad2() {
    setupSoundManager();

    if (props.doInstrumentation()) {
      buttonFactory = new ButtonFactory(service, props);
    }
    else {
      buttonFactory = new EventMock();
    }
    userManager = new UserManager(this, service, props);

    checkAdmin();

    boolean usualLayout = !showOnlyOneExercise();
    Container verticalContainer = new FluidContainer();
    if (usualLayout) {
      RootPanel.get().add(verticalContainer);
    }
    DOM.setStyleAttribute(RootPanel.get().getElement(), "paddingTop", "2px");

    addResizeHandler();

    // header/title line
    // first row ---------------
    verticalContainer.add(headerRow = makeHeaderRow());
    headerRow.getElement().setId("headerRow");

    Panel belowFirstRow = new DivWidget();
    verticalContainer.add(belowFirstRow);
    this.belowFirstRow = belowFirstRow;
    belowFirstRow.getElement().setId("belowFirstRow");

    // second row ---------------
    secondRow = new FluidRow();
    secondRow.getElement().setId("secondRow");

    // third row ---------------

    Panel exerciseListContainer = new SimplePanel();
    exerciseListContainer.addStyleName("floatLeft");

    Panel thirdRow = new HorizontalPanel();
    thirdRow.add(exerciseListContainer);
    thirdRow.getElement().setId("outerThirdRow");
    thirdRow.setWidth("100%");
    thirdRow.addStyleName("trueInlineStyle");

    Panel bothSecondAndThird = new FlowPanel();
    bothSecondAndThird.add(secondRow);
    bothSecondAndThird.add(thirdRow);
    this.bothSecondAndThird = bothSecondAndThird;
    if ((isCRTDataCollectMode() || props.isDataCollectMode()) && !props.isFlashcardTeacherView()) {
      addProgressBar(verticalContainer);
    }
    else {
      verticalContainer.add(status);
    }

    // set up center panel, initially with flash record panel
    currentExerciseVPanel = new FlowPanel();
    currentExerciseVPanel.getElement().setId("currentExercisePanel");

    reallyMakeExerciseList(belowFirstRow, exerciseListContainer, bothSecondAndThird);

    if (usualLayout) {
      currentExerciseVPanel.addStyleName("floatLeftList");
      thirdRow.add(currentExerciseVPanel);
    }
    else {  // show fancy lace background image
      currentExerciseVPanel.addStyleName("body");
      currentExerciseVPanel.getElement().getStyle().setBackgroundImage("url("+ LANGTEST_IMAGES +"levantine_window_bg.jpg"+")");
      currentExerciseVPanel.addStyleName("noMargin");
      RootPanel.get().add(currentExerciseVPanel);
    }

    // don't do flash if we're doing text only collection

    if (shouldCollectAudio()) {
      makeFlashContainer();
      belowFirstRow.add(flashRecordPanel);
    }

    setPageTitle();
    browserCheck.checkForCompatibleBrowser();

    modeSelect();
    loadVisualizationPackages();  // Note : this was formerly done in LangTest.html, since it seemed to be intermittently not loaded properly
  }

  private void checkAdmin() {
    if (props.isAdminView() || props.isGrading()) {
      final LangTest outer = this;
      GWT.runAsync(new RunAsyncCallback() {
        public void onFailure(Throwable caught) {
          Window.alert("Code download failed");
        }

        public void onSuccess() {
          resultManager = new ResultManager(service, outer, props.getNameForAnswer(), props);
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
  }

  private void reallyMakeExerciseList(Panel belowFirstRow, Panel exerciseListContainer, Panel bothSecondAndThird) {
    makeExerciseList(secondRow, exerciseListContainer);
    if (!getProps().isClassroomMode()) {
      belowFirstRow.add(bothSecondAndThird);
    }
  }

  private boolean isIPad() { return Window.Navigator.getUserAgent().toLowerCase().contains("ipad");  }

  private void loadVisualizationPackages() {
    VisualizationUtils.loadVisualizationApi(new Runnable() {
      @Override
      public void run() {
        System.out.println("\tloaded VisualizationUtils...");
        logMessageOnServer("loaded VisualizationUtils.");
      }
    }, ColumnChart.PACKAGE, LineChart.PACKAGE);
  }

  private boolean shouldCollectAudio() {
    return !isIPad() && props.isCollectAudio() && !props.isFlashcardTeacherView() || props.isFlashCard()  || props.isGoodwaveMode() ;
  }

  /**
   * @see #onModuleLoad2()
   * @return
   */
  private Panel makeHeaderRow() {
    Widget title;
    if (isGoodwaveMode()) {
      flashcard = new Flashcard(props);
      title = flashcard.makeNPFHeaderRow(props.getSplash(), props.isClassroomMode(),getGreeting(), getReleaseStatus(),new LogoutClickHandler(),

        (props.isAdminView()) ? new UsersClickHandler() : null,
        (props.isAdminView()) ? new ResultsClickHandler() : null,
        (props.isAdminView()) ? new MonitoringClickHandler() : null,
      (props.isAdminView()) ? new EventsClickHandler() : null
        );
    }
    else {
      flashcard = new Flashcard(props);
      title = flashcard.getHeaderRow(props.getSplash(), props.isClassroomMode(), NEW_PRO_F2_PNG,props.getAppTitle(), getGreeting(), getReleaseStatus(), new LogoutClickHandler(),

        (props.isAdminView()) ? new UsersClickHandler() : null,
        (props.isAdminView()) ? new ResultsClickHandler() : null,
        (props.isAdminView()) ? new MonitoringClickHandler() : null,
        (props.isAdminView()) ? new EventsClickHandler() : null);
    }


    headerRow = new FluidRow();
    headerRow.add(new Column(12,title));
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
   * Set the page title and favicon.
   */
  private void setPageTitle() {
    String appTitle = props.getAppTitle();
    setTitle(appTitle);

    Element element = DOM.getElementById("favicon");   // set the page title to be consistent
    if (props.isFlashCard() || props.isFlashcardTeacherView()) {
      if (element != null) {
        element.setAttribute("href",  LANGTEST_IMAGES + "NewProF2_48x48.png");
      }
    }
    else if (props.isGoodwaveMode()) {
      if (element != null) {
        element.setAttribute("href", LANGTEST_IMAGES + "NewProF1_48x48.png");
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
   * @seex #doDataCollectAdminView()
   * @see #makeHeaderRow()
   * @return
   */
  private HTML getReleaseStatus() {
    browserCheck.getBrowserAndVersion();

    String releaseDate = props.getReleaseDate() != null ? " " + props.getReleaseDate() : "";
    return new HTML("<span><font size=-2>" + browserCheck.browser + " " + browserCheck.ver + releaseDate + "</font></span>");
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
        if (exerciseList != null) {
          exerciseList.onResize();
        }
        if (navigation != null) navigation.onResize();
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
   * @see #reallyMakeExerciseList
   */
  private ListInterface makeExerciseList(FluidRow secondRow, Panel exerciseListContainer) {
    this.exerciseList = new ExerciseListLayout(props).makeExerciseList(secondRow, exerciseListContainer, this, currentExerciseVPanel, service, this);
    reallySetFactory();
    return exerciseList;
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
   * @see #onModuleLoad2()
   */
  private void modeSelect() {
    checkInitFlash();
  }

  private boolean showingPlugInNotice = false;
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
        showPopupOnDenial();
      }

      /**
       * @see mitll.langtest.client.recorder.FlashRecordPanelHeadless#noMicrophoneFound()
       */
      public void noMicAvailable() {
        if (!showingPlugInNotice) {
          showingPlugInNotice = true;
          new ModalInfoDialog("Plug in microphone", "Please plug in your microphone.",
            new HiddenHandler() {
              @Override
              public void onHidden(HiddenEvent hiddenEvent) {
                showingPlugInNotice = false;
                removeAndReloadFlash();
              }
            });
        }
      }
    });
  }

  /**
   * This determines which kind of exercise we're going to do.
   * @see #gotUser(long)
   */
  private void setFactory(final long userID) {
    //reallySetFactory();
    doEverythingAfterFactory(userID);
  }

  private void reallySetFactory() {
    final LangTest outer = this;
    if (props.isGoodwaveMode() && !props.isGrading()) {
      if (props.isClassroomMode()) {
        exerciseList.setFactory(new GoodwaveExercisePanelFactory(service, outer, outer, exerciseList,1.0f) {
          @Override
          public Panel getExercisePanel(CommonExercise e) {
            if (isReviewMode()) {
              return new QCNPFExercise(e, controller, exerciseList, 1.0f, false, "classroom");
            }
            else {
              return new CommentNPFExercise(e, controller, exerciseList, 1.0f, false, "classroom");
            }
          }
        }, userManager, 1);
      } else {
        exerciseList.setFactory(new GoodwaveExercisePanelFactory(service, outer, outer, exerciseList, getScreenPortion()), userManager, 1);
      }
    } else if (props.isGrading()) {
      exerciseList.setFactory(new GradingExercisePanelFactory(service, outer, outer, exerciseList), userManager, props.getNumGradesToCollect());
    }
  }

  /**
   * Show a popup telling how unhappy we are with the user's choice not to allow mic recording.
   *
   * Remove the flash player that was there, put in a new one, again, and ask the user again for permission.
   *
   */
  private void showPopupOnDenial() {
    new ModalInfoDialog("Try Again", "Please allow access to the microphone.",
      new HiddenHandler() {
        @Override
        public void onHidden(HiddenEvent hiddenEvent) {
          removeAndReloadFlash();
        }
      });
  }

  private void removeAndReloadFlash() {
    System.out.println(new Date() + " : removeAndReloadFlash - reloading...");

    belowFirstRow.remove(flashRecordPanel);
    flashRecordPanel.removeFlash();
    makeFlashContainer();
    belowFirstRow.add(flashRecordPanel);
    flashRecordPanel.initFlash();
  }

  public String getGreeting() {
    return userManager.getUserID() == null ? "" : (""+  userManager.getUserID());
  }

  /**
   * @seex #getLogout()
   */
  private void resetState() {
    History.newItem(""); // clear history!
    userManager.clearUser();
    exerciseList.removeCurrentExercise();
    exerciseList.clear();
    lastUser = -2;
    modeSelect();
  }

  private void resetClassroomState() {
    if (getProps().isClassroomMode()) {
      if (navigation != null) {
        belowFirstRow.remove(navigation.getContainer());
      }
      navigation = getProps().isCombinedMode() ? new Combined(service, userManager, this, exerciseList, this) :
          new Navigation(service, userManager, this, exerciseList, this);
      belowFirstRow.add(navigation.getNav(bothSecondAndThird));
      showInitialState();
    }
  }

  @Override
  public StartupInfo getStartupInfo() { return startupInfo; }

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
    //System.out.println("LangTest.gotUser : got user " + userID);
    flashcard.setUserName(getGreeting());
    setFactory(userID);
  }

  private boolean doEverythingAfterFactory(long userID) {
    if (userID != lastUser || (props.isGoodwaveMode() || props.isFlashCard() && !props.isTimedGame())) {
      System.out.println("doEverythingAfterFactory : user changed - new " + userID + " vs last " + lastUser);
      if (!shouldCollectAudio() || flashRecordPanel.gotPermission()) {
        if (exerciseList != null) {
//          System.out.println("\tdoEverythingAfterFactory : " + userID + " get exercises");
          exerciseList.getExercises(userID, true);
        }
        else {
          System.out.println("\tdoEverythingAfterFactory : " + userID + " exercise list is null???");
        }

        resetClassroomState();
      }
      else {
        System.out.println("\tdoEverythingAfterFactory : " + userID + " NOT getting exercises");
      }
      lastUser = userID;

      return true;
    } else if (props.isTimedGame()) {
      exerciseList.reloadExercises();
      return true;
    } else return false;
  }

  private void showInitialState() {
    if (navigation != null) {
      navigation.showInitialState();
    }
  }

  /**
   * @see #modeSelect()
   */
  // TODO : refactor all this into mode objects that decide whether we need flash or not, etc.
  private void checkInitFlash() {
    if (shouldCollectAudio() && !flashRecordPanel.gotPermission()) {
      //System.out.println("checkInitFlash : initFlash");
      flashRecordPanel.initFlash();
    }
    else {
      gotMicPermission();

      checkLogin();
    }
  }

  public boolean gotMicPermission() {
    boolean gotPermission = flashRecordPanel != null && flashRecordPanel.gotPermission();
    System.out.println("checkInitFlash : skip init flash, just checkLogin (got permission = " + gotPermission+")");
    return gotPermission;
  }

  @Override
  public EventLogger getButtonFactory() {
    return buttonFactory;
  }

  @Override
  public void register(Button button, String exid) {
    buttonFactory.register(this, button, exid);
  }

  @Override
  public void register(Button button, String exid, String context) {
    buttonFactory.registerButton(button, exid, context, getUser());
  }

  @Override
  public void logEvent(UIObject button, String widgetType, String exid, String context) {
    buttonFactory.logEvent(button, widgetType, exid, context, getUser());
  }
  @Override
  public void logEvent(Tab button, String widgetType, String exid, String context) {
    buttonFactory.logEvent(button, widgetType, exid, context, getUser());
  }

  /**
   * Called after the user clicks "Yes" in flash mic permission dialog.
   *
   * @see #checkInitFlash()
   * @see #makeFlashContainer()
   */
  private void checkLogin() {
    userManager.checkLogin();
  }

  @Override
  public void rememberAudioType(String audioType) { this.audioType = audioType;  }

  public boolean showCompleted() {
    return isReviewMode() || isCRTDataCollectMode();
  }

  @Override
  public String getAudioType() {
    return audioType;
  }
  public boolean isReviewMode() { return audioType.equals(Result.AUDIO_TYPE_REVIEW); }

  public void setShowUnansweredFirst(boolean val) {/* this.showUnansweredFirst = val;*/ }

  @Override
  public void setShowRerecord(boolean v) {/* showRerecord = v;*/ }

  /**
   * @see mitll.langtest.client.exercise.PostAnswerProvider#postAnswers
   * @return
   */
  public int getUser() { return userManager.getUser(); }
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

  public boolean isGoodwaveMode() {  return props.isGoodwaveMode(); }
  public LangTestDatabaseAsync getService() { return service; }
  public UserFeedback getFeedback() { return this; }

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

  public void showErrorMessage(String title, String msg) {
    DialogHelper dialogHelper = new DialogHelper(false);
    dialogHelper.showErrorMessage(title, msg);
    logMessageOnServer("Showing error message", title + " : " + msg);
  }

  public void showStatus(String msg) {
    status.setText(msg);
  }

  @Override
  public void showProgress() {
    showProgress(exerciseList);
  }

  private void showProgress(ListInterface exerciseList) {
    if (progressBar != null) {
      //progressBar.showAdvance(exerciseList);
    }
  }

  public ListInterface getExerciseList() { return exerciseList; }

  private class LogoutClickHandler implements ClickHandler {
    public void onClick(ClickEvent event) {
      resetState();
    }
  }

  private class UsersClickHandler implements ClickHandler {
    public void onClick(ClickEvent event) {
      new UserTable(props).showUsers(service, userManager.getUser());
    }
  }

  private class EventsClickHandler implements ClickHandler {
    public void onClick(ClickEvent event) {
      new EventTable(props).show(service);
    }
  }

  private class ResultsClickHandler implements ClickHandler {
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
  }

  private class MonitoringClickHandler implements ClickHandler {
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
  }
}