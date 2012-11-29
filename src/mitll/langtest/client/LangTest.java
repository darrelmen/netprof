package mitll.langtest.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExerciseList;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.exercise.GradedExerciseList;
import mitll.langtest.client.exercise.ListInterface;
import mitll.langtest.client.exercise.PagingExerciseList;
import mitll.langtest.client.scoring.GoodwaveExercisePanelFactory;
import mitll.langtest.client.grading.GradingExercisePanelFactory;
import mitll.langtest.client.recorder.FlashRecordPanelHeadless;
import mitll.langtest.client.recorder.MicPermission;
import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.client.sound.SoundManagerStatic;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.client.user.UserNotification;
import mitll.langtest.client.user.UserTable;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseShell;

import java.util.Map;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class LangTest implements EntryPoint, UserFeedback, ExerciseController, UserNotification {
  // TODO : consider putting these in the .css file?
  private static final int HEADER_HEIGHT = 80;
  private static final int FOOTER_HEIGHT = 40;
  private  static final int EXERCISE_LIST_WIDTH = 200;
  private static final int EAST_WIDTH = 90;
  private static final String DLI_LANGUAGE_TESTING = "NetPron 2";
  private static final boolean DEFAULT_GOODWAVE_MODE = false;
  private static final boolean DEFAULT_ARABIC_TEXT_COLLECT = false;
  private static final boolean DEFAULT_SHOW_TURK_TOKEN = false;
  private static final int DEFAULT_SEGMENT_REPEATS = 2;
  private static final String DEFAULT_EXERCISE = null;//"nl0020_ams";
  public static final String LANGTEST_IMAGES = "langtest/images/";

  private Panel currentExerciseVPanel = new VerticalPanel();
  private ListInterface exerciseList;
  private ScrollPanel itemScroller;
  private Label status;
  private UserManager userManager;
  private final UserTable userTable = new UserTable();
  private ResultManager resultManager;
  private FlashRecordPanelHeadless flashRecordPanel;

  private long lastUser = -1;

  private final LangTestDatabaseAsync service = GWT.create(LangTestDatabase.class);
  private ExercisePanelFactory factory = new ExercisePanelFactory(service, this, this);
  private int footerHeight = FOOTER_HEIGHT;
  private int eastWidth = EAST_WIDTH;
  private final BrowserCheck browserCheck = new BrowserCheck();
  private SoundManagerStatic soundManager;
  private float screenPortion;
  private Map<String, String> props;

  // properties -- todo rationalize properties, url params
  private boolean grading = false;
  private boolean englishOnlyMode = false;
  private boolean goodwaveMode = DEFAULT_GOODWAVE_MODE;
  private boolean arabicTextDataCollect = DEFAULT_ARABIC_TEXT_COLLECT;
  private boolean showTurkToken = DEFAULT_SHOW_TURK_TOKEN;
  private int segmentRepeats = DEFAULT_SEGMENT_REPEATS;
  private boolean bkgColorForRef = false;
  private String exercise_title;
  private String appTitle = DLI_LANGUAGE_TESTING;
  private boolean readFromFile;
  private String releaseDate;

  // property file property names
  private static final String GRADING_PROP = "grading";
  private static final String ENGLISH_ONLY_MODE = "englishOnlyMode";
  private static final String GOODWAVE_MODE = "goodwaveMode";
  private static final String ARABIC_TEXT_DATA_COLLECT = "arabicTextDataCollect";
  private static final String SHOW_TURK_TOKEN = "showTurkToken";
  private static final String APP_TITLE = "appTitle";
  private static final String SEGMENT_REPEATS = "segmentRepeats";
  private static final String READ_FROM_FILE = "readFromFile";
  private static final String RELEASE_DATE = "releaseDate";
  private static final String BKG_COLOR_FOR_REF1 = "bkgColorForRef";

  // URL parameters that can override above parameters
  private static final String GRADING = "grading";
  private static final String ENGLISH = "english";
  private static final String GOODWAVE = "goodwave";
  private static final String ARABIC_COLLECT = "arabicCollect";
  private static final String TURK = "turk";
  private static final String REPEATS = "repeats";
  private static final String BKG_COLOR_FOR_REF = "bkgColorForRef";
  private static final String EXERCISE_TITLE = "exercise_title";

  /**
   * Make an exception handler that displays the exception.
   */
  public void onModuleLoad() {
    // set uncaught exception handler
    GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
      public void onUncaughtException(Throwable throwable) {
        String text = "Uncaught exception: ";
        while (throwable != null) {
          StackTraceElement[] stackTraceElements = throwable.getStackTrace();
          text += throwable.toString() + "\n";
          for (StackTraceElement stackTraceElement : stackTraceElements) {
            text += "    at " + stackTraceElement + "\n";
          }
          throwable = throwable.getCause();
          if (throwable != null) {
            text += "Caused by: ";
          }
        }
        DialogBox dialogBox = new DialogBox(true, false);
        DOM.setStyleAttribute(dialogBox.getElement(), "backgroundColor", "#ABCDEF");
        System.err.print(text);
        text = text.replaceAll(" ", "&nbsp;");
        dialogBox.setHTML("<pre>" + text + "</pre>");
        dialogBox.center();
      }
    });


    service.getProperties(new AsyncCallback<Map<String, String>>() {
      public void onFailure(Throwable caught) {}
      public void onSuccess(Map<String, String> result) {
        props = result;
        onModuleLoad2();
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
    useProps();
    userManager = new UserManager(this,service);
    resultManager = new ResultManager(service, this);
    boolean isGrading = checkParams();
    boolean usualLayout = exercise_title == null;
    final DockLayoutPanel widgets = new DockLayoutPanel(Style.Unit.PX);
    if (usualLayout) {
      RootPanel.get().add(widgets);
    }
    if (goodwaveMode) {
      footerHeight = 5;
    }

    // if you remove this line the layout doesn't work -- the dock layout appears blank!
    setMainWindowSize(widgets);
    addResizeHandler(widgets);

    // header/title line
    DockLayoutPanel hp = new DockLayoutPanel(Style.Unit.PX);
    HTML title = new HTML("<h1>" + appTitle + "</h1>");
    browserCheck.getBrowserAndVersion();
    hp.addEast(getLogout(), eastWidth);
    hp.add(title);

    VerticalPanel exerciseListPanel = new VerticalPanel();

    widgets.addNorth(hp, HEADER_HEIGHT);
    widgets.addSouth(status = new Label(), footerHeight);
    widgets.addWest(exerciseListPanel, EXERCISE_LIST_WIDTH/* +10*/);

    // set up center panel, initially with flash record panel

    if (usualLayout) {
      ScrollPanel sp = new ScrollPanel();
      sp.add(currentExerciseVPanel);
      widgets.add(sp);
    }
    else {  // show fancy lace background image
      currentExerciseVPanel.addStyleName("body");
      currentExerciseVPanel.getElement().getStyle().setBackgroundImage("url("+ LANGTEST_IMAGES +"levantine_window_bg.jpg"+")");
      currentExerciseVPanel.addStyleName("noMargin");
      RootPanel.get().add(currentExerciseVPanel);
    }
    if (!arabicTextDataCollect) {
      makeFlashContainer();
      currentExerciseVPanel.add(flashRecordPanel);
    }
    if (usualLayout) {
      currentExerciseVPanel.addStyleName("currentExercisePanel");
    }

    setupErrorDialog();

    // set up left side exercise list
    makeExerciseList(exerciseListPanel, isGrading);

    modeSelect();

    browserCheck.checkForCompatibleBrowser();

    soundManager = new SoundManagerStatic();
    soundManager.exportStaticMethods();
    soundManager.initialize();
    Element elementById = DOM.getElementById("title-tag");   // set the page title to be consistent
    if (elementById != null) {
      elementById.setInnerText(appTitle);
    }
  }

  private void useProps() {
    for (Map.Entry<String, String> kv : props.entrySet()) {
      if (kv.getKey().equals(GRADING_PROP)) grading = Boolean.parseBoolean(kv.getValue());
      else if (kv.getKey().equals(ENGLISH_ONLY_MODE)) englishOnlyMode = Boolean.parseBoolean(kv.getValue());
      else if (kv.getKey().equals(GOODWAVE_MODE)) goodwaveMode = Boolean.parseBoolean(kv.getValue());
      else if (kv.getKey().equals(ARABIC_TEXT_DATA_COLLECT)) arabicTextDataCollect = Boolean.parseBoolean(kv.getValue());
      else if (kv.getKey().equals(SHOW_TURK_TOKEN)) showTurkToken = Boolean.parseBoolean(kv.getValue());
      else if (kv.getKey().equals(APP_TITLE)) appTitle = kv.getValue();
      else if (kv.getKey().equals(SEGMENT_REPEATS)) segmentRepeats = Integer.parseInt(kv.getValue());
      else if (kv.getKey().equals(READ_FROM_FILE)) readFromFile = Boolean.parseBoolean(kv.getValue());
      else if (kv.getKey().equals(RELEASE_DATE)) releaseDate = kv.getValue();
      else if (kv.getKey().equals(BKG_COLOR_FOR_REF1)) bkgColorForRef = Boolean.parseBoolean(kv.getValue());
    }
  }

  /**
   * Tell the exercise list when the browser window changes size
   * @param widgets
   */
  private void addResizeHandler(final DockLayoutPanel widgets) {
    Window.addResizeHandler(new ResizeHandler() {
      public void onResize(ResizeEvent event) {
//        System.out.println("updating width since got event " +event + " w = " + Window.getClientWidth());
        setMainWindowSize(widgets);
        setExerciseListSize();
        exerciseList.onResize();
      }
    });
  }

  public float getScreenPortion() {
    return screenPortion;
  }

  /**
   * Supports different flavors of exercise list -- Paging, Grading, and vanilla.
   *
   * @param exerciseListPanel to add scroller to
   * @param isGrading true if grading, false if not
   */
  private void makeExerciseList(Panel exerciseListPanel, boolean isGrading) {
    final UserFeedback feedback = (UserFeedback) this;
    if (isGrading) {
      this.exerciseList = new GradedExerciseList(currentExerciseVPanel, service, feedback, factory, isArabicTextDataCollect());
    }
    else if (goodwaveMode) {
      this.exerciseList = new PagingExerciseList(currentExerciseVPanel, service, feedback, factory, goodwaveMode, isArabicTextDataCollect(), showTurkToken) {
        @Override
        protected void checkBeforeLoad(ExerciseShell e) {} // don't try to login
      };
    }
    else {
      this.exerciseList = new ExerciseList(currentExerciseVPanel, service, feedback, factory, goodwaveMode, isArabicTextDataCollect(), showTurkToken);
    }

    if (showOnlyOneExercise()) {
      exerciseList.setExercise_title(exercise_title);
    }
    if (!goodwaveMode) {
      itemScroller = new ScrollPanel(this.exerciseList.getWidget());
    }
    if (exercise_title == null) {
      setExerciseListSize();
    }
    HTML child = new HTML("<h2>Items</h2>");
    child.addStyleName("center");
    exerciseListPanel.add(child);
    exerciseListPanel.add(goodwaveMode ? this.exerciseList.getWidget() : itemScroller);
  }

  public boolean showOnlyOneExercise() {
    return exercise_title != null;
  }

  private void setMainWindowSize(DockLayoutPanel widgets) {
    int widthToUse = Window.getClientWidth() - (goodwaveMode ? 15 : eastWidth);
    widgets.setSize(Math.max(widthToUse, 50) + "px", Math.max((Window.getClientHeight()-footerHeight-15),50) + "px");
    //widgets.setSize("100%","100%");
    //widgets.setSize(Math.max((Window.getClientWidth() - EAST_WIDTH), 50) + "px","100%");
  }

  private void setExerciseListSize() {
    int height = Math.max(60, Window.getClientHeight() - (2 * HEADER_HEIGHT) - footerHeight - 60);
    if (!goodwaveMode) itemScroller.setSize(EXERCISE_LIST_WIDTH + "px", height + "px"); // 54
  }

  /**
   * Check the URL parameters for special modes.
   */
  private void modeSelect() {
    boolean isGrading = checkParams();
    //System.out.println("modeSelect english " +englishOnlyMode + " grading " +isGrading );

    logout.setVisible(!goodwaveMode);
    users.setVisible(isGrading);
    showResults.setVisible(isGrading);

    if (goodwaveMode) {
      gotUser(-1);
    }
    else if (englishOnlyMode || isGrading) {
      userManager.graderLogin();
    }
    else {
      login();
    }
  }

  /**
   * Override config.properties settings with URL parameters, if provided.
   * <br></br>
   * exercise_title=nl0002_lms&transform_score_c1=68.51101&transform_score_c2=2.67174
   * @return
   */
  private boolean checkParams() {
    String isGrading = Window.Location.getParameter(GRADING);
    String isEnglish = Window.Location.getParameter(ENGLISH);
    String goodwave = Window.Location.getParameter(GOODWAVE);
    String repeats = Window.Location.getParameter(REPEATS);
    String arabicCollect = Window.Location.getParameter(ARABIC_COLLECT);
    String turk = Window.Location.getParameter(TURK);
    String bkgColorForRefParam = Window.Location.getParameter(BKG_COLOR_FOR_REF);

    String exercise_title = Window.Location.getParameter(EXERCISE_TITLE);
    if (exercise_title != null) {
     if (goodwave == null) goodwave = "true";
      this.exercise_title = exercise_title;
    }
    else {
      this.exercise_title = DEFAULT_EXERCISE;
    }
    String screenPortionParam =  Window.Location.getParameter("screenPortion");
    screenPortion = 1.0f;
    if (screenPortionParam != null) {
      try {
        screenPortion = Float.parseFloat(screenPortionParam);
      } catch (NumberFormatException e) {
        e.printStackTrace();
      }
    }
    // System.out.println("param grading " + isGrading);
    englishOnlyMode = englishOnlyMode || (isEnglish != null && !isEnglish.equals("false"));
    goodwaveMode    = goodwaveMode || (goodwave != null && !goodwave.equals("false"));
    if (goodwave != null && goodwave.equals("false")) goodwaveMode = false;
    //GWT.log("goodwave mode = " + goodwaveMode + "/" +goodwave);
    boolean grading = this.grading || (isGrading != null && !isGrading.equals("false")) || englishOnlyMode;

    // get audio repeats
    if (repeats != null) {
      try {
        segmentRepeats = Integer.parseInt(repeats)-1;
      } catch (NumberFormatException e) {
        e.printStackTrace();
      }
    }
    if (arabicCollect != null) {
      arabicTextDataCollect = !arabicCollect.equals("false");
    }
    if (turk != null) {
      showTurkToken = !turk.equals("false");
    }
    if (bkgColorForRefParam != null) {
      bkgColorForRef = !bkgColorForRefParam.equals("false");
    }
    return grading;
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
        System.out.println("got permission!");
        flashRecordPanel.hide();
        flashRecordPanel.hide2();

        exerciseList.getExercises(lastUser);
      }

      public void gotDenial() {
        System.err.println("dude!!!!");
        showPopupOnDenial();
      }
    });
  }

  /**
   * @see UserManager#displayGraderLogin()
   * @param g
   */
  public void setGrading(boolean g) {
    this.grading = g;

    if (goodwaveMode) {
      exerciseList.setFactory(new GoodwaveExercisePanelFactory(service, this, this), userManager, 1);
    }
    else if (grading) {
      exerciseList.setFactory(new GradingExercisePanelFactory(service, this, this), userManager, englishOnlyMode ? 2 : 1);
      lastUser = -1; // no user
    }
    else {
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
  private Anchor showResults;
  /**
   * Has both a logout and a users link and a results link
   * @return
   */
  private Widget getLogout() {
    VerticalPanel vp = new VerticalPanel();

    // add logout link
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
        userTable.showUsers(service);
      }
    });
    vp.add(users);
    users.setVisible(!isArabicTextDataCollect());

    showResults = new Anchor("Results");
    showResults.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        resultManager.showResults();
      }
    });
    vp.add(showResults);
    showResults.setVisible(!isArabicTextDataCollect());

    // no click handler for this for now
    HTML statusLine = new HTML("<span><font size=-2>"+browserCheck.browser + " " +browserCheck.ver +" " +
        releaseDate+"</font></span>");
    vp.add(statusLine);

    return vp;
  }

  private void resetState() {
    userManager.clearUser();
    exerciseList.removeCurrentExercise();
    exerciseList.clear();
    modeSelect();
  }

  /**
   * @see ExerciseList#loadExercise(mitll.langtest.shared.Exercise)
   * @see #modeSelect()
   */
  public void login() {  userManager.login(); }

  /**
   * Init Flash recorder once we login.
   *
   * Only get the exercises if the user has accepted mic access.
   *
   * @see #makeFlashContainer
   * @see UserManager#login
   * @see UserManager#storeUser(long)
   * @param userID
   */
  public void gotUser(long userID) {
  //  System.out.println("gotUser " + userID + " vs " + lastUser);

    grading = false;
    setGrading(grading);
    if (!arabicTextDataCollect) flashRecordPanel.initFlash();

    if (userID != lastUser) {
      if (arabicTextDataCollect || flashRecordPanel.gotPermission()) {
        exerciseList.getExercises(userID);
      }
      lastUser = userID;
    }
  }

  /**
   * @see mitll.langtest.client.exercise.ExercisePanel#postAnswers
   * @see mitll.langtest.client.recorder.SimpleRecordPanel#stopRecording()
   * @return
   */
  public int getUser() { return userManager.getUser(); }
  public String getGrader() { return userManager.getGrader(); }
  public boolean getEnglishOnly() { return englishOnlyMode; }
  public int getSegmentRepeats() { return segmentRepeats; }
  public boolean isArabicTextDataCollect() {  return arabicTextDataCollect; }
  public boolean useBkgColorForRef() {  return bkgColorForRef; }

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

  private DialogBox dialogBox;
  private Label msgLabel;
  private Button closeButton;
  private void setupErrorDialog() {
    dialogBox = new DialogBox();
    dialogBox.setText("Information");
  //  dialogBox.setAnimationEnabled(true);
    dialogBox.setGlassEnabled(true);

    this.closeButton = new Button("Close");
    // We can set the id of a widget by accessing its Element
    closeButton.getElement().setId("closeButton");

    VerticalPanel dialogVPanel = new VerticalPanel();
    dialogVPanel.addStyleName("dialogVPanel");
    dialogVPanel.add(msgLabel = new Label());

    dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
    dialogVPanel.add(closeButton);
    dialogBox.setWidget(dialogVPanel);

    closeButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        dialogBox.hide();
      }
    });
  }

  public void showErrorMessage(String title, String msg) {
    System.err.println("got error " + msg);
    dialogBox.setText(title);
    msgLabel.setText(msg);
    dialogBox.center();
    closeButton.setFocus(true);
  }

  public void showStatus(String msg) { status.setText(msg); }

  public boolean loadNextExercise(Exercise current) { return exerciseList.loadNextExercise(current);  }
  public boolean loadPreviousExercise(Exercise current) { return exerciseList.loadPreviousExercise(current);  }
  public boolean onFirst(Exercise current) { return exerciseList.onFirst(current); }
}
