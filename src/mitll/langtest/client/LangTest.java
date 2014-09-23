package mitll.langtest.client;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.visualization.client.VisualizationUtils;
import com.google.gwt.visualization.client.visualizations.corechart.ColumnChart;
import com.google.gwt.visualization.client.visualizations.corechart.LineChart;
import mitll.langtest.client.custom.Navigation;
import mitll.langtest.client.custom.exercise.CommentNPFExercise;
import mitll.langtest.client.dialog.DialogHelper;
import mitll.langtest.client.dialog.ExceptionHandlerDialog;
import mitll.langtest.client.dialog.KeyPressHelper;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.flashcard.Flashcard;
import mitll.langtest.client.instrumentation.ButtonFactory;
import mitll.langtest.client.instrumentation.EventLogger;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.client.instrumentation.EventTable;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.monitoring.MonitoringManager;
import mitll.langtest.client.qc.QCNPFExercise;
import mitll.langtest.client.recorder.FlashRecordPanelHeadless;
import mitll.langtest.client.recorder.MicPermission;
import mitll.langtest.client.result.ResultManager;
import mitll.langtest.client.scoring.GoodwaveExercisePanelFactory;
import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.client.sound.SoundManagerStatic;
import mitll.langtest.client.user.*;
import mitll.langtest.shared.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class LangTest implements EntryPoint, UserFeedback, ExerciseController, UserNotification {
  public static final String LANGTEST_IMAGES = "langtest/images/";
  private static final String DIVIDER = "|";
 // private static final String NEW_PRO_F2_PNG = "NewProF2.png";
  private static final int MAX_EXCEPTION_STRING = 300;
  private static final int MAX_CACHE_SIZE = 100;
  private static final int NO_USER_INITIAL = -2;
  private static final boolean SHOW_STATUS = false;
 // private static final boolean SHOW_EXCEPTION_TO_USER = false;
  private static final String PLEASE_ALLOW_ACCESS_TO_THE_MICROPHONE = "Please allow access to the microphone.";
  private static final String TRY_AGAIN = "Try Again";

  /**
   * @see #makeExerciseList(com.github.gwtbootstrap.client.ui.FluidRow, com.google.gwt.user.client.ui.Panel, com.google.gwt.user.client.ui.Panel)
   */
  private ListInterface exerciseList;

  private UserManager userManager;
  private ResultManager resultManager;
  private MonitoringManager monitoringManager;
  private FlashRecordPanelHeadless flashRecordPanel;

  private long lastUser = NO_USER_INITIAL;
  private String audioType = Result.AUDIO_TYPE_UNSET;

  private final LangTestDatabaseAsync service = GWT.create(LangTestDatabase.class);
  private final BrowserCheck browserCheck = new BrowserCheck();
  private SoundManagerStatic soundManager;
  private PropertyHandler props;
  private Flashcard flashcard;

  private Panel headerRow;
  private FluidRow secondRow;
  private Panel firstRow;

  private StartupInfo startupInfo;

  private Navigation navigation;
  private EventLogger buttonFactory;
  private final KeyPressHelper keyPressHelper = new KeyPressHelper(false,true);

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
        /*String exceptionAsString =*/ logException(throwable);

 /*       if (SHOW_EXCEPTION_TO_USER) {
          if (exceptionAsString.length() > 0) {
            new ExceptionHandlerDialog().showExceptionInDialog(browserCheck, exceptionAsString);
          }
        }*/
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
    if (toSend.length() > MAX_EXCEPTION_STRING) {
      toSend = toSend.substring(0, MAX_EXCEPTION_STRING)+"...";
    }
    getButtonFactory().logEvent("unknown","unknown",exerciseID,toSend,user);
  }

  private void logMessageOnServer(String message) {
    new Exception().printStackTrace();
    service.logMessage(message,
      new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {}

        @Override
        public void onSuccess(Void result) {}
      }
    );
  }

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
    String key = path + DIVIDER + type + DIVIDER + toUse + DIVIDER + height + DIVIDER + exerciseID;
    getImage(reqid, key, client);
  }

  private void getImage(int reqid, String key, AsyncCallback<ImageResponse> client) {
    String[] split = key.split("\\|");

    String path = split[0];
    String type = split[1];
    int toUse = Integer.parseInt(split[2]);
    int height = Integer.parseInt(split[3]);
    String exerciseID = split[4];

    getImage(reqid, key, path, type, toUse, height, exerciseID, client);
  }

  private void getImage(int reqid, final String key, String path, final String type, int toUse, int height,
                        String exerciseID, final AsyncCallback<ImageResponse> client) {

    ImageResponse ifPresent = imageCache.getIfPresent(key);
    if (ifPresent != null) {
      //System.out.println("getImage for key " + key+ " found  " + ifPresent);
      ifPresent.req = -1;
      client.onSuccess(ifPresent);
    } else {
      service.getImageForAudioFile(reqid, path, type, toUse, height, exerciseID, new AsyncCallback<ImageResponse>() {
        public void onFailure(Throwable caught) {
       /*   if (!caught.getMessage().trim().equals("0")) {
            Window.alert("getImageForAudioFile Couldn't contact server. Please check network connection.");
          }*/
          System.out.println("message " + caught.getMessage() + " " + caught);
          logException(caught);
          client.onFailure(caught);
        }

        public void onSuccess(ImageResponse result) {
          imageCache.put(key, result);
          //System.out.println("getImage storing key " + key+ " now  " + imageCache.size() + " cached.");

          client.onSuccess(result);
        }
      });
    }
  }

  private final Cache<String, ImageResponse> imageCache = CacheBuilder.newBuilder()
    .maximumSize(MAX_CACHE_SIZE)
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

    buttonFactory = new ButtonFactory(service, props);

    userManager = new UserManager(this, service, props);

    checkAdmin(false);

    RootPanel.get().getElement().getStyle().setPaddingTop(2, Style.Unit.PX);

    addResizeHandler();

    makeFlashContainer();

    populateRootPanel();

    setPageTitle();
    browserCheck.checkForCompatibleBrowser();

/*    if (isAdmin()) {
      loadVisualizationPackages();  // Note : this was formerly done in LangTest.html, since it seemed to be intermittently not loaded properly
    }*/
  }


  /**
   * @see #onModuleLoad2()
   * @return
   */
  private void populateRootPanel() {
    RootPanel.get().clear();   // necessary?

    Container verticalContainer = new FluidContainer();
    verticalContainer.getElement().setId("root_vertical_container");

    // header/title line
    // first row ---------------
    Panel firstRow = makeFirstTwoRows(verticalContainer);

    if (!showLogin(verticalContainer, firstRow)) {
      //System.out.println("populate below header...");

      populateBelowHeader(verticalContainer, firstRow);
    }
  }

  private void populateBelowHeader(Container verticalContainer, Panel firstRow) {
    // second row ---------------
    secondRow = new FluidRow();
    secondRow.getElement().setId("secondRow");

    // third row ---------------

    Panel exerciseListContainer = new SimplePanel();
    exerciseListContainer.addStyleName("floatLeft");
    exerciseListContainer.getElement().setId("exerciseListContainer");

    Panel thirdRow = new HorizontalPanel();
    thirdRow.add(exerciseListContainer);   // left side of third row is exercise list
    thirdRow.getElement().setId("thirdRow");
    thirdRow.setWidth("100%");
    thirdRow.addStyleName("trueInlineStyle");

    Panel bothSecondAndThird = new FlowPanel();
    bothSecondAndThird.getElement().setId("secondAndThirdRowContainer");
    bothSecondAndThird.add(secondRow);
    bothSecondAndThird.add(thirdRow);

    // set up center right panel, initially with flash record panel
    Panel currentExerciseVPanel = new FlowPanel();
    currentExerciseVPanel.getElement().setId("currentExercisePanel");

    makeExerciseList(secondRow, exerciseListContainer, currentExerciseVPanel);

    if (showOnlyOneExercise()) {
      RootPanel.get().add(getHeadstart(currentExerciseVPanel));
    } else {
      currentExerciseVPanel.addStyleName("floatLeftList");
      thirdRow.add(currentExerciseVPanel);     // right side of third row is exercise panel
      RootPanel.get().add(verticalContainer);
    }

    if (!showOnlyOneExercise()) {
      /**
       * {@link #makeFlashContainer}
       */
      firstRow.add(flashRecordPanel);
    }
    modeSelect();

    navigation = new Navigation(service, userManager, this, exerciseList, this);

    firstRow.add(navigation.getNav(bothSecondAndThird));

    if (SHOW_STATUS) {
      DivWidget w = new DivWidget();
      w.getElement().setId("status");
      verticalContainer.add(w);
    }
  }

  private Container getHeadstart(Panel currentExerciseVPanel) {
    // show fancy lace background image
    currentExerciseVPanel.addStyleName("body");
    currentExerciseVPanel.getElement().getStyle().setBackgroundImage("url(" + LANGTEST_IMAGES + "levantine_window_bg.jpg" + ")");
    currentExerciseVPanel.addStyleName("noMargin");

    Container verticalContainer2 = new FluidContainer();
    verticalContainer2.getElement().setId("root_vertical_container");
    verticalContainer2.add(flashRecordPanel);
    verticalContainer2.add(currentExerciseVPanel);
    return verticalContainer2;
  }

  @Override
  public void showLogin() {
    System.out.println("show login!");

    RootPanel.get().clear();   // necessary?

    Container verticalContainer = new FluidContainer();
    verticalContainer.getElement().setId("login_container");

    // header/title line
    // first row ---------------
    Panel firstRow = makeFirstTwoRows(verticalContainer);

    showLogin(verticalContainer, firstRow);
  }

  private String staleToken = "";

  /**
   * @see #showLogin()
   * @see #populateRootPanel()
   * @param verticalContainer
   * @param firstRow
   * @return
   */
  private boolean showLogin(final Container verticalContainer, final Panel firstRow) {
    final EventRegistration eventRegistration = this;

    final String resetPassToken = props.getResetPassToken();
    if (!resetPassToken.isEmpty() && !resetPassToken.equals(staleToken)) {

      System.out.println("showLogin token '" + resetPassToken + "' for password reset");

      staleToken = resetPassToken;
      service.getUserIDForToken(resetPassToken, new AsyncCallback<Long>() {
        @Override
        public void onFailure(Throwable caught) {}

        @Override
        public void onSuccess(Long result) {
          if (result == null || result < 0) {
            System.out.println("token '" + resetPassToken + "' is stale. Showing normal view");
     //       firstRow.add(new Heading(4,"Password reset has been done before."));
            populateBelowHeader(verticalContainer,firstRow);
          }
          else {
            UserPassLogin userPassLogin = new UserPassLogin(service, getProps(), userManager, eventRegistration);
            Panel content = userPassLogin.getResetPassword(resetPassToken);
            firstRow.add(content);
            content.getElement().setId("ResetPassswordContent");
            verticalContainer.getElement().getStyle().setPaddingLeft(0, Style.Unit.PX);
            verticalContainer.getElement().getStyle().setPaddingRight(0, Style.Unit.PX);
          }
        }
      });

      RootPanel.get().add(verticalContainer);
      flashcard.setCogVisible(false);

      return true;
    }

    final String cdToken = props.getCdEnableToken();
    if (!cdToken.isEmpty() && !cdToken.equals(staleToken)) {
      System.out.println("showLogin token '" + resetPassToken + "' for enabling cd user");

      handleCDToken(verticalContainer, firstRow, cdToken, props.getEmailRToken());
      return true;
    }

    boolean show = userManager.isUserExpired() || userManager.getUserID() == null;
    if (show) {
      Panel content = new UserPassLogin(service, getProps(), userManager, eventRegistration).getContent();
      firstRow.add(content);
      content.getElement().setId("UserPassLogin");
      verticalContainer.getElement().getStyle().setPaddingLeft(0, Style.Unit.PX);
      verticalContainer.getElement().getStyle().setPaddingRight(0, Style.Unit.PX);
      RootPanel.get().add(verticalContainer);
      flashcard.setCogVisible(false);

      return true;
    }


    flashcard.setCogVisible(true);
    return false;
  }

  private void handleCDToken(final Container verticalContainer, final Panel firstRow, final String cdToken, String emailR) {
    service.enableCDUser(cdToken, emailR, Window.Location.getHref(), new AsyncCallback<Long>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Long result) {
        staleToken = cdToken;
        if (result == null || result < 0) {
          System.out.println("handleCDToken enable - token " + cdToken + " is stale. Showing normal view");
          //       firstRow.add(new Heading(4,"Password reset has been done before."));
          populateBelowHeader(verticalContainer, firstRow);
        } else {
          //Panel content = new UserPassLogin(service, getProps(), userManager, eventRegistration).getResetPassword(resetPassToken);
          firstRow.add(new Heading(2, "OK, content developer has been approved."));
          // content.getElement().setId("ResetPassswordContent");
          verticalContainer.getElement().getStyle().setPaddingLeft(0, Style.Unit.PX);
          verticalContainer.getElement().getStyle().setPaddingRight(0, Style.Unit.PX);

          Timer t = new Timer() {
            @Override
            public void run() {
              Window.Location.replace(trimURL(Window.Location.getHref()));
              Window.Location.reload();
            }
          };
          t.schedule(3000);
        }
      }
    });

    RootPanel.get().add(verticalContainer);
    flashcard.setCogVisible(false);
  }

  private String trimURL(String url) {
    if (url.contains("127.0.0.1")) return url;
    else return url.split("\\?")[0].split("\\#")[0];
  }

  private Panel makeFirstTwoRows(Container verticalContainer) {
    verticalContainer.add(headerRow = makeHeaderRow());
    headerRow.getElement().setId("headerRow");

    Panel firstRow = new DivWidget();
    verticalContainer.add(firstRow);
    this.firstRow = firstRow;
    firstRow.getElement().setId("firstRow");
    return firstRow;
  }

  /**
   * @see #onModuleLoad2
   */
  private void checkAdmin(boolean isAdmin) {
    if (isAdmin || props.isGrading()) {
    //  final LangTest outer = this;
      GWT.runAsync(new RunAsyncCallback() {
        public void onFailure(Throwable caught) {
          Window.alert("Code download failed");
        }

        public void onSuccess() {
          resultManager = new ResultManager(service, props.getNameForAnswer());
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

  //private boolean isAdmin() {
  //  return props.isAdminView();
  //}

  /**
   * Supports different flavors of exercise list -- Paging, Grading, and vanilla.
   *
   * @see #populateBelowHeader(com.github.gwtbootstrap.client.ui.Container, com.google.gwt.user.client.ui.Panel)
   */
  private ListInterface makeExerciseList(FluidRow secondRow, Panel exerciseListContainer, Panel currentExerciseVPanel) {
    this.exerciseList = new ExerciseListLayout(props).makeExerciseList(secondRow, exerciseListContainer, this, currentExerciseVPanel, service, this);
    return exerciseList;
  }

  private void loadVisualizationPackages() {
    VisualizationUtils.loadVisualizationApi(new Runnable() {
      @Override
      public void run() {
        //System.out.println("\tloaded VisualizationUtils...");
        logMessageOnServer("loaded VisualizationUtils.");
      }
    }, ColumnChart.PACKAGE, LineChart.PACKAGE);
  }

  /**
   * @return
   * @see #populateRootPanel()
   */
  private Panel makeHeaderRow() {
    flashcard = new Flashcard(props);
    Widget title = flashcard.makeNPFHeaderRow(props.getSplash(), true, getGreeting(), getReleaseStatus(), new LogoutClickHandler(),
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
   * Set the page title and favicon.
   */
  private void setPageTitle() {
    String appTitle = props.getAppTitle();
    Window.setTitle(appTitle + " : " + "Learn pronunciation and practice vocabulary.");

    Element element = DOM.getElementById("favicon");   // set the page title to be consistent
    if (element != null) {
       element.setAttribute("href", LANGTEST_IMAGES + "NewProF1_48x48.png");
    }
  }

  /**
   * @see #makeHeaderRow()
   * @return
   */
  private HTML getReleaseStatus() {
    browserCheck.getBrowserAndVersion();
    return new HTML(getInfoLine());
  }

  private String getInfoLine() {
    String releaseDate = props.getReleaseDate() != null ? " " + props.getReleaseDate() : "";
    return "<span><font size=-2>" +
      //browserCheck.browser + " " +
      browserCheck.ver +
      releaseDate + (flashRecordPanel.usingWebRTC() ? " Flashless recording" : "")+
      "</font></span>";
  }

  private void setupSoundManager() {  soundManager = new SoundManagerStatic();  }

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

  public int getHeightOfTopRows() {
    return headerRow.getOffsetHeight() + secondRow.getOffsetHeight();
  }

  @Override
  public int getLeftColumnWidth() {
    return exerciseList.getWidget().getOffsetWidth();
  }

  /**
   * @see #populateRootPanel()
   * @see mitll.langtest.client.scoring.ScoringAudioPanel#ScoringAudioPanel(String, String, LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, boolean, mitll.langtest.client.scoring.ScoreListener, int, String, String)
   * @return
   */
  public boolean showOnlyOneExercise() {
    return props.getExercise_title() != null;
  }

  /**
   * Check the URL parameters for special modes.
   *
   * If in goodwave (pronunciation scoring) mode or auto crt mode, skip the user login.
   * @see #onModuleLoad2()
   * @see #resetState()
   */
  private void modeSelect() {
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        checkInitFlash();
      }
    });
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
        hideFlash();
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
          List<String> messages = Arrays.asList("If you want to record audio, ", "plug in or enable your mic and reload the page.");
          new ModalInfoDialog("Plug in microphone", messages, null,
              new HiddenHandler() {
                @Override
                public void onHidden(HiddenEvent hiddenEvent) {
                  hideFlash();
                  checkLogin();

                  flashcard.setSplash();
                }
              }
          );
        }
      }

      public void noRecordingMethodAvailable() {
        System.out.println(new Date() + " : makeFlashContainer - no way to record");
        hideFlash();
        new ModalInfoDialog("Can't record audio", "Recording audio is not supported.", new HiddenHandler() {
          @Override
          public void onHidden(HiddenEvent hiddenEvent) {
            checkLogin();
          }
        });

        flashcard.setSplash();
      }
    });
  }

  void hideFlash() {
    flashRecordPanel.hide();
    flashRecordPanel.hide2(); // must be a separate call!
  }

  /**
   * This determines which kind of exercise we're going to do.
   *
   * Two things determine what kind of UI is shown - what kind of user has logged in (students get basic view, content developers see more tabs)
   * And whether the ability to record audio is there -- if flash, whether permission has been granted or is allowed via
   * configuration.  If webrtc (in browser) recording, whether permission has been granted or not.  Also, there are
   * physical considerations - is there a mic available at all?  Or is there a flashblocker preventing flash from running?
   *
   * So we want to configure the UI after login and after the recording state has been settled.
   *
   * @see #gotUser
   * @see #configureUIGivenUser(long) (long)
   */
  private void reallySetFactory() {
    int childCount = firstRow.getElement().getChildCount();

    //System.out.println("reallySetFactory root " + firstRow.getElement().getNodeName() + " childCount " + childCount);
    if (childCount > 0) {
      Node child = firstRow.getElement().getChild(0);
      Element as = Element.as(child);
      if (as.getId().contains("Login")) {
        populateRootPanel();
      }
      //else {
      //  System.out.println("elem is Got " + as.getId());
      //}
    }
    // have to wait until we know what kind of user has logged in before knowing what to present
    exerciseList.setFactory(new GoodwaveExercisePanelFactory(service, this, this, exerciseList, 1.0f) {
      @Override
      public Panel getExercisePanel(CommonExercise e) {
        boolean reviewer = permissions.contains(User.Permission.QUALITY_CONTROL);
        if (reviewer) {
          return new QCNPFExercise(e, controller, exerciseList, "classroom");
        } else {
          return new CommentNPFExercise(e, controller, exerciseList, false, "classroom");
        }
      }
    }, userManager);
  }

  /**
   * Show a popup telling how unhappy we are with the user's choice not to allow mic recording.
   *
   * Remove the flash player that was there, put in a new one, again, and ask the user again for permission.
   * @see #makeFlashContainer()
   * @deprecated fall back to in browswer recording
   */
  private void showPopupOnDenial() {
    new ModalInfoDialog(TRY_AGAIN, PLEASE_ALLOW_ACCESS_TO_THE_MICROPHONE,
      new HiddenHandler() {
        @Override
        public void onHidden(HiddenEvent hiddenEvent) {
          removeAndReloadFlash();
        }
      });
  }

  private void removeAndReloadFlash() {
    System.out.println(new Date() + " : removeAndReloadFlash - reloading...");

    firstRow.remove(flashRecordPanel);
    flashRecordPanel.removeFlash();
    makeFlashContainer();
    firstRow.add(flashRecordPanel);
    flashRecordPanel.initFlash();
  }

  /**
   * @see #gotUser
   * @see #makeHeaderRow()
   * @return
   */
  private String getGreeting() {
    return userManager.getUserID() == null ? "" : (""+  userManager.getUserID());
  }

  /**
   * @seex #getLogout()
   */
  private void resetState() {
    System.out.println("resetState");
    exerciseList.removeHistoryListener();
    History.newItem(""); // clear history!
    userManager.clearUser();
    exerciseList.removeCurrentExercise();
    exerciseList.clear();
    lastUser = NO_USER_INITIAL;
    modeSelect();
  }

  @Override
  public StartupInfo getStartupInfo() { return startupInfo; }

  /**
   * Init Flash recorder once we login.
   *
   * Only get the exercises if the user has accepted mic access.
   *
   * @see #makeFlashContainer
   * @see UserManager#gotNewUser(mitll.langtest.shared.User)
   * @see UserManager#storeUser
   * @param user
   */
  public void gotUser(User user) {

    long userID= -1;
    if (user != null) userID = user.getId();

    System.out.println("gotUser : userID " +userID);

    flashcard.setUserName(getGreeting());
    if (userID != lastUser) {
      configureUIGivenUser(userID);
      logEvent("No widget", "UserLogin", "N/A", "User Login by " + userID);
    }
    else {
      System.out.println("ignoring got user for current user " + userID);
      navigation.refreshInitialState();
    }
    if (userID > -1) {
      flashcard.setCogVisible(true);
      checkAdmin(user.isAdmin());
      flashcard.setVisibleAdmin(user.isAdmin());
    }
  }

  /**
   * @see #gotUser
   * @param userID
   * @return
   */
  private void configureUIGivenUser(long userID) {
    System.out.println("configureUIGivenUser : user changed - new " + userID + " vs last " + lastUser +
        " audio type " + getAudioType() + " perms " + getPermissions());
    reallySetFactory();

    if (getPermissions().contains(User.Permission.QUALITY_CONTROL)) {
      exerciseList.setInstance(User.Permission.QUALITY_CONTROL.toString());
    } else {
      exerciseList.setInstance("flex");
    }

    System.out.println("\tconfigureUIGivenUser : " + userID + " get exercises...");

    boolean askedForExercises = exerciseList.getExercises(userID);
    if (askedForExercises){
      System.out.println(
          "\n" +
          "\n\tconfigureUIGivenUser : " + userID + " not reloading - asked for exercises");
    }
    else {
      System.out.println("\tconfigureUIGivenUser : " + userID + " initially list and user now " + userID);

      exerciseList.reload();
    }
    navigation.showInitialState();

    lastUser = userID;
    flashcard.setBrowserInfo(getInfoLine());
    flashcard.reflectPermissions(getPermissions());

  }

  /**
   * @see #modeSelect()
   */
  // TODO : refactor all this into mode objects that decide whether we need flash or not, etc.
  private void checkInitFlash() {
    if (!flashRecordPanel.gotPermission()) {
      System.out.println("checkInitFlash : initFlash - no permission yet");

      if (flashRecordPanel.initFlash()) {
        checkLogin();
      }
    }
    else {
      System.out.println("checkInitFlash : initFlash - has permission");
      checkLogin();
    }
  }

  @Override
  public EventLogger getButtonFactory() { return buttonFactory; }

  @Override
  public void register(Button button) { buttonFactory.register(this, button, "N/A");  }

  @Override
  public void register(Button button, String exid) {  buttonFactory.register(this, button, exid);  }

  @Override
  public void register(Button button, String exid, String context) {
    buttonFactory.registerButton(button, exid, context, getUser());
  }

  @Override
  public void registerWidget(HasClickHandlers clickable, UIObject uiObject, String exid, String context) {
    buttonFactory.registerWidget(clickable, uiObject, exid, context, getUser());
  }

  @Override
  public void logEvent(UIObject button, String widgetType, String exid, String context) {
    buttonFactory.logEvent(button, widgetType, exid, context, getUser());
  }
  @Override
  public void logEvent(Tab button, String widgetType, String exid, String context) {
    buttonFactory.logEvent(button, widgetType, exid, context, getUser());
  }
  void logEvent(String widgetID, String widgetType, String exid, String context) {
    buttonFactory.logEvent(widgetID, widgetType, exid, context, getUser());
  }

  /**
   * Called after the user clicks "Yes" in flash mic permission dialog.
   *
   * @see #checkInitFlash()
   * @see #makeFlashContainer()
   */
  private void checkLogin() {
    //console("checkLogin");
    //System.out.println("checkLogin -- ");
    userManager.isUserExpired();
    userManager.checkLogin();
  }

  /**
   * @see mitll.langtest.client.list.ExerciseList#askServerForExercise(String)
   */
  public void checkUser() {
    if (userManager.isUserExpired()) {
      checkLogin();
    }
  }
/*
  private void console(String message) {
    int ieVersion = BrowserCheck.getIEVersion();
    if (ieVersion == -1 || ieVersion > 9) {
      consoleLog(message);
    }
  }*/
/*

  private native static void consoleLog( String message) */
/*-{
      console.log( "LangTest:" + message );
  }-*//*
;
*/

  @Override
  public void rememberAudioType(String audioType) {  this.audioType = audioType;  }

  public boolean showCompleted() {
    return isReviewMode() || getAudioType().equals(Result.AUDIO_TYPE_RECORDER);
  }

  /**
   * TODO : Hack - don't use audio type like this
   * @return
   */
  @Override
  public String getAudioType() {
    if (permissions.contains(User.Permission.RECORD_AUDIO)) return Result.AUDIO_TYPE_RECORDER;
    else return audioType;
  }
  private boolean isReviewMode() { return audioType.equals(Result.AUDIO_TYPE_REVIEW); }

  private final Set<User.Permission> permissions = new HashSet<User.Permission>();

  /**
   * When we login, we ask for permissions for the user from the server.
   *
   * @see mitll.langtest.client.user.UserManager#login()
   * @param permission
   * @param on
   */
  public void setPermission(User.Permission permission, boolean on) {
    if (on) permissions.add(permission);
    else permissions.remove(permission);
  }

  public Collection<User.Permission> getPermissions() { return permissions; }

  /**
   * @see mitll.langtest.client.exercise.PostAnswerProvider#postAnswers
   * @return
   */
  public int getUser() { return userManager.getUser(); }
  public PropertyHandler getProps() { return props; }

  public boolean useBkgColorForRef() {  return props.isBkgColorForRef(); }

  public int getRecordTimeout() {  return props.getRecordTimeout(); }
  public boolean isGrading() {  return props.isGrading(); }
  public boolean isLogClientMessages() {  return props.isLogClientMessages(); }
  public String getLanguage() {  return props.getLanguage(); }
  public boolean isRightAlignContent() {  return props.isRightAlignContent(); }

  public LangTestDatabaseAsync getService() { return service; }
  public UserFeedback getFeedback() { return this; }

  private long then;
  // recording methods...
  /**
   * Recording interface
   */
  public void startRecording() {

    then = System.currentTimeMillis();
    flashRecordPanel.recordOnClick();
  }

  /**
   * Recording interface
   * @see mitll.langtest.client.scoring.PostAudioRecordButton#stopRecording()
   * @see mitll.langtest.client.recorder.RecordButtonPanel#stopRecording()
   */
  public void stopRecording(WavCallback wavCallback) {
    long now = System.currentTimeMillis();
    System.out.println("stopRecording : time recording in UI " + (now -then) + " millis");

    flashRecordPanel.stopRecording(wavCallback);
  }

  /**
   * Recording interface
   */

  public SoundManagerAPI getSoundManager() { return soundManager;  }

  public void showErrorMessage(String title, String msg) {
    DialogHelper dialogHelper = new DialogHelper(false);
    dialogHelper.showErrorMessage(title, msg);
    logMessageOnServer("Showing error message", title + " : " + msg);
  }

  @Override
  public void showProgress() { showProgress(exerciseList);  }

  private void showProgress(ListInterface exerciseList) {
/*    if (progressBar != null) {
      //progressBar.showAdvance(exerciseList);
    }*/
  }

  public ListInterface getExerciseList() { return exerciseList; }

  /**
   * @see mitll.langtest.client.recorder.FlashcardRecordButton#FlashcardRecordButton(int, mitll.langtest.client.recorder.RecordButton.RecordingListener, boolean, boolean, mitll.langtest.client.exercise.ExerciseController, String)
   * @param listener
   */
  @Override
  public void addKeyListener(KeyPressHelper.KeyListener listener) {
    keyPressHelper.addKeyHandler(listener);
    if (keyPressHelper.getSize() > 2) {
      System.out.println("addKeyListener " + listener.getName() +
        " key press handler now " + keyPressHelper);
    }
  }

  @Override
  public boolean isRecordingEnabled() {  return flashRecordPanel.gotPermission();  }

  @Override
  public boolean usingFlashRecorder() {  return flashRecordPanel.usingFlash();  }

  private class LogoutClickHandler implements ClickHandler {
    public void onClick(ClickEvent event) {
      logEvent("No widget", "UserLoging", "N/A", "User Logout by " + lastUser);
      resetState();
    }
  }

  private class UsersClickHandler implements ClickHandler {
    public void onClick(ClickEvent event) {
      GWT.runAsync(new RunAsyncCallback() {
        public void onFailure(Throwable caught) {
          Window.alert("Code download failed");
        }

        public void onSuccess() {
          new UserTable(props).showUsers(service);
        }
      });
    }
  }

  private class EventsClickHandler implements ClickHandler {
    public void onClick(ClickEvent event) {
      GWT.runAsync(new RunAsyncCallback() {
        public void onFailure(Throwable caught) {
          Window.alert("Code download failed");
        }

        public void onSuccess() {
          new EventTable().show(service);
        }
      });
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