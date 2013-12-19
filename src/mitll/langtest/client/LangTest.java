package mitll.langtest.client;

import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.Container;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Row;
import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.visualization.client.VisualizationUtils;
import com.google.gwt.visualization.client.visualizations.corechart.ColumnChart;
import com.google.gwt.visualization.client.visualizations.corechart.LineChart;
import mitll.langtest.client.custom.CommentNPFExercise;
import mitll.langtest.client.custom.NPFExercise;
import mitll.langtest.client.custom.Navigation;
import mitll.langtest.client.custom.QCNPFExercise;
import mitll.langtest.client.dialog.DialogHelper;
import mitll.langtest.client.dialog.ExceptionHandlerDialog;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.exercise.WaveformExercisePanelFactory;
import mitll.langtest.client.flashcard.CombinedResponseFlashcard;
import mitll.langtest.client.flashcard.DataCollectionFlashcardFactory;
import mitll.langtest.client.flashcard.Flashcard;
import mitll.langtest.client.flashcard.FlashcardExercisePanelFactory;
import mitll.langtest.client.flashcard.TextCRTFlashcard;
import mitll.langtest.client.grading.GradingExercisePanelFactory;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.mail.MailDialog;
import mitll.langtest.client.monitoring.MonitoringManager;
import mitll.langtest.client.recorder.FlashRecordPanelHeadless;
import mitll.langtest.client.recorder.MicPermission;
import mitll.langtest.client.result.ResultManager;
import mitll.langtest.client.scoring.GoodwaveExercisePanelFactory;
import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.client.sound.SoundManagerStatic;
import mitll.langtest.client.user.AdminUserTable;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.client.user.UserNotification;
import mitll.langtest.client.user.UserTable;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.StartupInfo;

import java.util.Date;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class LangTest implements EntryPoint, UserFeedback, ExerciseController, UserNotification {
  public static final String LANGTEST_IMAGES = "langtest/images/";

  private Panel currentExerciseVPanel = new FluidContainer();
  private ListInterface exerciseList;
  private final Label status = new Label();

  private UserManager userManager;
  private UserTable userTable;
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

  private Panel headerRow;
  private FluidRow secondRow;
  private ProgressHelper progressBar;

  private Anchor logout;
  private Anchor users;
  private Anchor showResults, monitoring;
  private HTML releaseStatus;
  private StartupInfo startupInfo;

  private Navigation navigation;

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
    int user = userManager != null ? userManager.getUser() : -1;
    String exerciseID = exerciseList != null ? exerciseList.getCurrentExerciseID() : "Unknown";
    logMessageOnServer("got browser exception : user #" + user +
      " exercise " + exerciseID + " browser " + browserCheck.getBrowserAndVersion()+
    " : " + exceptionAsString);
    return exceptionAsString;
  }

  private void logMessageOnServer(String message) {
    service.logMessage(message,
        new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {
            //Window.alert("logMessage : Couldn't contact server.  Please check your network connection.");
          }

          @Override
          public void onSuccess(Void result) {
          }
        });
  }
 Panel belowFirstRow;
  Panel bothSecondAndThird;
/*  Panel belowFirstRow;
  Panel leftColumn;
  FluidContainer bothSecondAndThird;*/

  /**
   * Use DockLayout to put a header at the top, exercise list on the left, and eventually
   * the current exercise in the center.  There is also a status on line on the bottom.
   *
   * Initially the flash record player is put in the center of the DockLayout
   */
  private void onModuleLoad2() {
    setupSoundManager();

    userManager = new UserManager(this, service, props);
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

    checkAdmin();

    boolean usualLayout = !showOnlyOneExercise();
    Container verticalContainer = new FluidContainer();
    //verticalContainer.addStyleName("rootContainer");
    if (usualLayout) {
      RootPanel.get().add(verticalContainer);
    }
    DOM.setStyleAttribute(RootPanel.get().getElement(), "paddingTop", "2px");

    addResizeHandler();

    // header/title line
    // first row ---------------
    verticalContainer.add(headerRow = makeHeaderRow());
    headerRow.getElement().setId("headerRow");

    Panel belowFirstRow = new FluidRow();
    verticalContainer.add(belowFirstRow);
     this.belowFirstRow = belowFirstRow;
    // second row ---------------
    secondRow = new FluidRow();
    secondRow.getElement().setId("secondRow");

    // third row ---------------

  Panel thirdRow = new HorizontalPanel();
   // Panel thirdRow = new FlowPanel();
    Panel leftColumn = new SimplePanel();
    thirdRow.add(leftColumn);
    leftColumn.addStyleName("floatLeft");
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
/*    DOM.setStyleAttribute(currentExerciseVPanel.getElement(), "paddingLeft", "5px");
    DOM.setStyleAttribute(currentExerciseVPanel.getElement(), "paddingRight", "2px");*/

    reallyMakeExerciseList(belowFirstRow, leftColumn, bothSecondAndThird);

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

    //System.out.println("user agent " + Window.Navigator.getUserAgent());
    if (shouldCollectAudio()) {
      makeFlashContainer();
  //    currentExerciseVPanel.add(flashRecordPanel);
      belowFirstRow.add(flashRecordPanel);
    }
    else {
      System.out.println("*not* allowing recording of audio.");
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

  private void reallyMakeExerciseList(Panel belowFirstRow, Panel leftColumn, Panel bothSecondAndThird) {
    ListInterface listInterface = makeExerciseList(secondRow, leftColumn);
    if (getProps().isClassroomMode()) {
      //navigation = new Navigation(service, userManager, this, listInterface);
      //belowFirstRow.add(navigation.getNav(bothSecondAndThird, this));
    }
    else {
      belowFirstRow.add(bothSecondAndThird);
    }
  }

  private boolean isIPad() { return Window.Navigator.getUserAgent().toLowerCase().contains("ipad");  }

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
    return !isIPad() && props.isCollectAudio() && !props.isFlashcardTeacherView() || props.isFlashCard()  || props.isGoodwaveMode() ;
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
    } else if (props.isFlashcardTeacherView() || props.isAutocrt()) {
      flashcard = new Flashcard();
      title = flashcard.getHeaderRow(props.getSplash(), "NewProF2.png",props.getAppTitle());
    }
    else {
      title = getTitleWidget();
    }

    boolean isStudent = getLoginType().equals(PropertyHandler.LOGIN_TYPE.STUDENT) ||  getLoginType().equals(PropertyHandler.LOGIN_TYPE.SIMPLE);
    boolean takeWholeWidth = isStudent || props.isFlashcardTeacherView() || props.isShowSections() || props.isGoodwaveMode();

    Column titleColumn = new Column(takeWholeWidth ? 12 : 10, title);
    headerRow.add(titleColumn);
    makeLogoutParts();
    if (!takeWholeWidth) {
      headerRow.add(new Column(2, getLogout()));
    } else if (isStudent || props.isAdminView() || props.isDataCollectMode()) {
      FluidRow adminRow = new FluidRow();
      adminRow.addStyleName("alignCenter");
      adminRow.addStyleName("inlineBlockStyle");

      this.userline.setHTML(getUserText());
      if (props.isAdminView()) {
        adminRow.add(new Column(2, userline));
        adminRow.add(new Column(2, logout));
        adminRow.add(new Column(2, users));
        adminRow.add(new Column(2, showResults));
        adminRow.add(new Column(2, monitoring));
        adminRow.add(new Column(2, releaseStatus));
      }
      else {
        adminRow.add(new Column(2, userline));
        adminRow.add(new Column(2, releaseStatus));
        adminRow.add(new Column(6, new SimplePanel()));
        adminRow.add(new Column(2, logout));
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
   * @see mitll.langtest.client.list.section.SectionExerciseList#getEmailWidget()
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
        new MailDialog(service, userManager).showEmail(subject, token);
      }
    });
  }

  private Widget getTitleWidget() {
    FluidRow titleRow = new FluidRow();
    titleRow.addStyleName("alignCenter");
    titleRow.addStyleName("inlineBlockStyle");
    Heading pageTitle = new Heading(2, props.getAppTitle());

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

    userManager = new UserManager(this, service, props);
    this.exerciseList = new ExerciseListLayout(props).makeFlashcardExerciseList(container, service, userManager);

    // setup flash
    makeFlashContainer();

    Row flashRow = new FluidRow();
    container.add(flashRow);
    flashRow.addStyleName("whiteBackground");
    flashRow.add(new Column(1, flashRecordPanel));

    // setup sound manager
    setupSoundManager();

/*    if (!props.isTimedGame()) {
      showHelpNewUser();
    }*/

    modeSelect();
  }

  /**
   * @see #doFlashcard()
   */
/*  private void showHelpNewUser() {
    Storage stockStore = Storage.getLocalStorageIfSupported();
    boolean showedHelpAlready = false;
    if (stockStore != null) {
      showedHelpAlready = stockStore.getItem("showedHelp") != null;
      stockStore.setItem("showedHelp", "showedHelp");
    }

    if (!showedHelpAlready) {
      showFlashHelp();
    }
  }*/

  @Override
  public void showFlashHelp() { flashcard.showFlashHelp(this, props.isFlashCard()); }

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
    userManager = new UserManager(this,service, props);

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
    System.out.println("setupSoundManager " );
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
   * @see #onModuleLoad2()
   */
  private ListInterface makeExerciseList(FluidRow secondRow, Panel leftColumn) {
    this.exerciseList = new ExerciseListLayout(props).makeExerciseList(secondRow, leftColumn, this, currentExerciseVPanel,service,this);
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
    boolean isGrading = props.isGrading();
    boolean isStudent = getLoginType().equals(PropertyHandler.LOGIN_TYPE.STUDENT);
    boolean showUserLine = isStudent || (!props.isGoodwaveMode() && !props.isFlashcardTeacherView());
    if (logout != null) logout.setVisible(showUserLine);
    if (userline != null) userline.setVisible(showUserLine);
    if (users != null) users.setVisible(isGrading || props.isAdminView());
    if (showResults != null) showResults.setVisible(isGrading || props.isAdminView());
    if (monitoring != null) monitoring.setVisible(isGrading || props.isAdminView());

    System.out.println("modeSelect : goodwave mode " + props.isGoodwaveMode() + " auto crt mode = " + isAutoCRTMode());

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

      public void gotDenial() {  showPopupOnDenial();   }

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
    final LangTest outer = this;
    if (props.isGoodwaveMode() && !props.isGrading()) {
      if (props.isClassroomMode()) {
        exerciseList.setFactory(new GoodwaveExercisePanelFactory(service, outer, outer, exerciseList,1.0f) {
          @Override
          public Panel getExercisePanel(Exercise e) {
            if (isReviewMode()) {
              System.out.println("\nmaking new QCNPFExercise for " +e + " instance " + "classroom");

              return new QCNPFExercise(e, controller, exerciseList, 1.0f, false, "classroom");
            }
            else {
              System.out.println("\nmaking new CommentNPFExercise for " +e + " instance " + "classroom");

              return new CommentNPFExercise(e, controller, exerciseList, 1.0f, false, "classroom");
            }
          }
        }, userManager, 1);
      } else {
        exerciseList.setFactory(new GoodwaveExercisePanelFactory(service, outer, outer, exerciseList, getScreenPortion()), userManager, 1);
      }
    } else if (props.isGrading()) {
      exerciseList.setFactory(new GradingExercisePanelFactory(service, outer, outer, exerciseList), userManager, props.getNumGradesToCollect());
    } else if (props.getFlashcardNextAndPrev()) {
      String responseType = props.getResponseType();

      //System.out.println("got response type " + responseType);
      if (responseType.equalsIgnoreCase("Text")) {
        exerciseList.setFactory(new ExercisePanelFactory(service, outer, outer, exerciseList) {
          @Override
          public Panel getExercisePanel(Exercise e) {
            return new TextCRTFlashcard(e, service, controller);
          }
        }, userManager, 1);
      } else if (responseType.equalsIgnoreCase("Audio")) {
        exerciseList.setFactory(new DataCollectionFlashcardFactory(service, outer, outer, exerciseList), userManager, 1);
      } else if (responseType.equalsIgnoreCase("Both")) {
        exerciseList.setFactory(new ExercisePanelFactory(service, outer, outer, exerciseList) {
          @Override
          public Panel getExercisePanel(Exercise e) {
            return new CombinedResponseFlashcard(e, service, controller);
          }
        }, userManager, 1);
      }
    } else if (props.isFlashCard()) {
      exerciseList.setFactory(new FlashcardExercisePanelFactory(service, outer, outer, exerciseList), userManager, 1);
    } else if (props.isDataCollectMode() && props.isCollectAudio() && !props.isCRTDataCollectMode()) {
      exerciseList.setFactory(new WaveformExercisePanelFactory(service, outer, outer, exerciseList), userManager, 1);
    } else {
      exerciseList.setFactory(new ExercisePanelFactory(service, outer, outer, exerciseList), userManager, 1);
    }
    doEverythingAfterFactory(userID);
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

    currentExerciseVPanel.remove(flashRecordPanel);
    flashRecordPanel.removeFlash();
    makeFlashContainer();
    currentExerciseVPanel.add(flashRecordPanel);
    flashRecordPanel.initFlash();
  }

  /**
   * Has both a logout and a users link and a results link
   * @return
   */
  private Widget getLogout() {
    VerticalPanel vp = new VerticalPanel();

    vp.add(userline);
    vp.add(logout);
    vp.add(users);

    vp.add(showResults);
    vp.add(monitoring);
    vp.add(releaseStatus);

    return vp;
  }

  private void makeLogoutParts() {
    this.userline = new HTML(getUserText());
    logout = getLogoutLink();

    makeUsersAnchor(false);
    showResults = new Anchor(props.getNameForAnswer()+"s");
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
    monitoring.setVisible(props.isAdminView());

    releaseStatus = getReleaseStatus();
  }

  private Anchor getLogoutLink() {
    SafeHtmlBuilder b = new SafeHtmlBuilder();
   // String s = "<font size=+1>Sign Out</font>";
    String s = "Sign Out";
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
        userTable = isDataCollectAdminView ? new AdminUserTable() : new UserTable();
        userTable.showUsers(service, userManager.getUser());
      }
    });
    users.setVisible(props.isAdminView());
    return users;
  }

  private String getUserText() {
    String greeting = getGreeting();
    return "<span><font size=-2>" + greeting + "</font></span>";
  }

  public String getGreeting() {
    return userManager.getUserID() == null ? "" : ("Hello " + userManager.getUserID());
  }

  /**
   * @see #getLogout()
   */
  @Override
  public void resetState() {
    everShownInitialState = false;
    History.newItem(""); // clear history!
    userManager.clearUser();
    exerciseList.removeCurrentExercise();
    exerciseList.clear();
    lastUser = -2;
    modeSelect();
  }

  private void resetClassroomState() {
    if (getProps().isClassroomMode()) {
     // System.out.println("\n\n\nreset classroom state : " + isReviewMode());
      //belowFirstRow.clear();
      if (navigation != null) {
        belowFirstRow.remove(navigation.getContainer());
      }
      navigation = new Navigation(service, userManager, this, exerciseList);
      belowFirstRow.add(navigation.getNav(bothSecondAndThird, this));
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
    System.out.println("LangTest.gotUser : got user " + userID);
    if (userline != null) {
      String userText = getUserText();
      userline.setHTML(userText);
    }
    if (props.isDataCollectAdminView()) {
      checkForAdminUser();
    } else {
      setFactory(userID);
    }
  }

  boolean everShownInitialState =false;
  private boolean doEverythingAfterFactory(long userID) {

    if (userID != lastUser || (props.isGoodwaveMode() || props.isFlashCard() && !props.isTimedGame())) {
      System.out.println("doEverythingAfterFactory : user changed - new " + userID + " vs last " + lastUser);
      if (!shouldCollectAudio() || flashRecordPanel.gotPermission()) {
        if (exerciseList != null) {
          System.out.println("\tdoEverythingAfterFactory : " + userID + " get exercises");
          exerciseList.getExercises(userID, true);
        }
        else {
          System.out.println("\tdoEverythingAfterFactory : " + userID + " exercise list is null???");
        }

       // showInitialState();
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
      System.out.println("showInitialState : " + getUser());
      navigation.showInitialState();
    }
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
      boolean gotPermission = flashRecordPanel != null && flashRecordPanel.gotPermission();
      System.out.println("checkInitFlash : skip init flash, just checkLogin (got permission = " + gotPermission+")");

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
    userManager.checkLogin();

    if (props.isTimedGame()) {
      flashcard.showTimedGameHelp(this);
    }
  }

  @Override
  public void rememberAudioType(String audioType) {
    this.audioType = audioType;
    System.out.println("audio type " + audioType + " review " + isReviewMode());
  }

  public boolean showCompleted() {
    boolean b = isReviewMode() || isCRTDataCollectMode();
    //System.out.println("showCompleted " + b);

    return b;
  }

  @Override
  public String getAudioType() {
    return audioType;
  }
  public boolean isReviewMode() { return audioType.equals(Result.AUDIO_TYPE_REVIEW); }

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

  public void showStatus(String msg) {
    status.setText(msg);
  }

  @Override
  public void showProgress() {
//    System.err.println("todo : fix this!");
  }

  // TODO fix thsi
  public void showProgress(ListInterface exerciseList) {
    if (progressBar != null) {
      progressBar.showAdvance(exerciseList);
    }
  }

  public ListInterface getExerciseList() { return exerciseList; }
}