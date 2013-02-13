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
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.visualization.client.VisualizationUtils;
import com.google.gwt.visualization.client.visualizations.corechart.ColumnChart;
import com.google.gwt.visualization.client.visualizations.corechart.LineChart;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExerciseList;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.exercise.GradedExerciseList;
import mitll.langtest.client.exercise.ListInterface;
import mitll.langtest.client.exercise.PagingExerciseList;
import mitll.langtest.client.exercise.WaveformExercisePanelFactory;
import mitll.langtest.client.grading.GradingExercisePanelFactory;
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

import java.util.Date;
import java.util.Map;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class LangTest implements EntryPoint, UserFeedback, ExerciseController, UserNotification {
  // TODO : consider putting these in the .css file?
  private static final int HEADER_HEIGHT = 90;
  private static final int FOOTER_HEIGHT = 40;
  private static final int EXERCISE_LIST_WIDTH = 210;
  private static final int EAST_WIDTH = 90;

  public static final String LANGTEST_IMAGES = "langtest/images/";
  private DataCollectAdmin dataCollectAdmin;

  private Panel currentExerciseVPanel = new VerticalPanel();
  private ListInterface exerciseList;
  private Label status;
  private UserManager userManager;
  private final UserTable userTable = new UserTable();
  private ResultManager resultManager;
  private MonitoringManager monitoringManager;
  private FlashRecordPanelHeadless flashRecordPanel;

  private long lastUser = -1;
  private String audioType = Result.AUDIO_TYPE_UNSET;

  private final LangTestDatabaseAsync service = GWT.create(LangTestDatabase.class);
  private int footerHeight = FOOTER_HEIGHT;
  private int eastWidth = EAST_WIDTH;
  private final BrowserCheck browserCheck = new BrowserCheck();
  private SoundManagerStatic soundManager;
  private PropertyHandler props;

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

    service.getProperties(new AsyncCallback<Map<String, String>>() {
      public void onFailure(Throwable caught) {}
      public void onSuccess(Map<String, String> result) {
        props = new PropertyHandler(result);
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
    if (props.isDataCollectAdminView()) {
      doDataCollectAdminView();
      return;
    }
    // Load the visualization api, passing the onLoadCallback to be called
    // when loading is done.
    VisualizationUtils.loadVisualizationApi(new Runnable() {
      @Override
      public void run() {}
    }, ColumnChart.PACKAGE, LineChart.PACKAGE);

    userManager = new UserManager(this,service, isCollectAudio());
    resultManager = new ResultManager(service, this);
    monitoringManager = new MonitoringManager(service, props);
    boolean usualLayout = props.getExercise_title() == null;
    final DockLayoutPanel widgets = new DockLayoutPanel(Style.Unit.PX);
    if (usualLayout) {
      RootPanel.get().add(widgets);
    }
    if (props.isGoodwaveMode()) {
      footerHeight = 5;
    }

    // if you remove this line the layout doesn't work -- the dock layout appears blank!
    setMainWindowSize(widgets);
    addResizeHandler(widgets);

    // header/title line
    DockLayoutPanel hp = new DockLayoutPanel(Style.Unit.PX);
    HTML title = new HTML("<h1>" + props.getAppTitle() + "</h1>");
   // browserCheck.getBrowserAndVersion();
    hp.addEast(getLogout(), eastWidth);
    hp.add(title);

    VerticalPanel exerciseListPanel = new VerticalPanel();

    widgets.addNorth(hp, HEADER_HEIGHT);
    widgets.addSouth(status = new Label(), footerHeight);
    widgets.addWest(exerciseListPanel, EXERCISE_LIST_WIDTH/* +10*/);
    if ((props.isMinimalUI()&& !props.isGrading()) && !props.isAdminView() || props.isTeacherView()) {
      exerciseListPanel.setVisible(false);
    }
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

    // don't do flash if we're doing text only collection
    if (!props.isTeacherView() && !props.isArabicTextDataCollect() && props.isCollectAudio()) {
      makeFlashContainer();
      currentExerciseVPanel.add(flashRecordPanel);
    }
    if (usualLayout) {
      currentExerciseVPanel.addStyleName("currentExercisePanel");
    }

    // set up left side exercise list
    makeExerciseList(exerciseListPanel, props.isGrading());

    setPageTitle();
    browserCheck.checkForCompatibleBrowser();
    setupSoundManager();

    modeSelect();
  }

  private void setPageTitle() {
    Element elementById = DOM.getElementById("title-tag");   // set the page title to be consistent
    if (elementById != null) {
      elementById.setInnerText(props.getAppTitle());
    }
  }

  private void doDataCollectAdminView() {
    //ScrollPanel sp = new ScrollPanel();
    VerticalPanel vp = new VerticalPanel();
    vp.addStyleName("grayColor");
    HTML title = new HTML("<h2>" + props.getAppTitle() + "</h2>");
    title.addStyleName("darkerBlueColor");
    title.addStyleName("grayColor");
    vp.add(title);
//    sp.add(title);
    //vp.add(sp);
    //sp.setWidth(Window.getClientWidth() -50 + "px");
    //sp.setHeight(Window.getClientHeight() -50 + "px");
    //sp.add(currentExerciseVPanel);
    vp.add(currentExerciseVPanel);
    userManager = new UserManager(this,service, false);
    dataCollectAdmin = new DataCollectAdmin(userManager,service);
    dataCollectAdmin.makeDataCollectNewSiteForm(currentExerciseVPanel);


    FlowPanel fp = new FlowPanel();
    fp.getElement().getStyle().setFloat(Style.Float.LEFT);
    SimplePanel w = new SimplePanel();
    w.setWidth("50px");
    w.setHeight("50px");
    fp.add(w);
    fp.add(vp);
    SimplePanel w2 = new SimplePanel();
    w2.setWidth("50px");
    w2.setHeight("50px");
    fp.add(w2);
    RootPanel.get().add(fp);

    browserCheck.checkForCompatibleBrowser();
    userManager.teacherLogin();
  }
/*

  private void makeDataCollectNewSiteForm(Panel currentExerciseVPanel) {


    // form.setAction("url");

    dataCollectAdmin.makeDataCollectNewSiteForm(currentExerciseVPanel);
  }
*/

  private void setupSoundManager() {
    soundManager = new SoundManagerStatic();
    soundManager.exportStaticMethods();
    soundManager.initialize();
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
    return props.getScreenPortion();
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
      this.exerciseList = new GradedExerciseList(currentExerciseVPanel, service, feedback, props.isReadFromFile(),
          true);
    }
    else {
      this.exerciseList = new PagingExerciseList(currentExerciseVPanel, service, feedback, props.isReadFromFile(),
          isArabicTextDataCollect(), props.isShowTurkToken(), isAutoCRTMode()) {
        @Override
        protected void checkBeforeLoad(ExerciseShell e) {} // don't try to login
      };
    }

    if (showOnlyOneExercise()) {
      exerciseList.setExercise_title(props.getExercise_title());
    }
    if (props.getExercise_title() == null) {
      setExerciseListSize();
    }
    HTML child = new HTML("<h2>Items</h2>");
    child.addStyleName("center");
    exerciseListPanel.add(child);
    exerciseListPanel.add(this.exerciseList.getWidget());
  }

  public boolean showOnlyOneExercise() {
    return props.getExercise_title() != null;
  }

  private void setMainWindowSize(DockLayoutPanel widgets) {
    int widthToUse = Window.getClientWidth() - (props.isGoodwaveMode() ? 15 : eastWidth);
    widgets.setSize(Math.max(widthToUse, 50) + "px", Math.max((Window.getClientHeight()-footerHeight-15),50) + "px");
    //widgets.setSize("100%","100%");
    //widgets.setSize(Math.max((Window.getClientWidth() - EAST_WIDTH), 50) + "px","100%");
  }

  private void setExerciseListSize() {
   // int height = Math.max(60, Window.getClientHeight() - (2 * HEADER_HEIGHT) - footerHeight - 60);
  }

  /**
   * Check the URL parameters for special modes.
   *
   * If in goodwave (pronunciation scoring) mode or auto crt mode, skip the user login.
   */
  private void modeSelect() {
    boolean isGrading = props.isGrading();
    //System.out.println("modeSelect : english " +props.isEnglishOnlyMode() + " grading " +isGrading + " is auto crt " + isAutoCRTMode());

    logout.setVisible(!props.isGoodwaveMode());
    users.setVisible(isGrading || props.isAdminView());
    showResults.setVisible(isGrading || props.isAdminView());
    monitoring.setVisible(isGrading || props.isAdminView());

    if (props.isGoodwaveMode() || isAutoCRTMode()) {   // no login for pron mode
      gotUser(-1);
    }
    else {
      login();
    }
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
        System.out.println(new Date() + " : got permission!");
        flashRecordPanel.hide();
        flashRecordPanel.hide2(); // must be a separate call!

        exerciseList.getExercises(lastUser);
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
    }
    else if (props.isGrading()) {
      exerciseList.setFactory(new GradingExercisePanelFactory(service, this, this), userManager, props.isEnglishOnlyMode() ? 2 : 1);
    }
    else if (props.isDataCollectMode() && props.isCollectAudio()) {
      exerciseList.setFactory(new WaveformExercisePanelFactory(service, this, this), userManager, 1);
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
  private Anchor showResults, monitoring;
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
    HTML statusLine = new HTML("<span><font size=-2>"+browserCheck.browser + " " +browserCheck.ver +" " +
        props.getReleaseDate()+"</font></span>");
    vp.add(statusLine);

    return vp;
  }

  private void resetState() {
    userManager.clearUser();
    exerciseList.removeCurrentExercise();
    exerciseList.clear();
    lastUser = -1;
    modeSelect();
  }

  /**
   * @see ExerciseList#loadExercise
   * @see #modeSelect()
   */
  public void login() {
    if (props.isDataCollectMode() || props.isTeacherView()) userManager.teacherLogin();
    else userManager.login();
  }

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
    if (props.isDataCollectAdminView()) {
      // go get list of current deployed sites
    } else {
      setFactory();

      if (!props.isArabicTextDataCollect() && props.isCollectAudio()) {
        flashRecordPanel.initFlash();
      }

      if (userID != lastUser) {
        System.out.println("gotUser : " + userID + " vs " + lastUser);
        if (props.isArabicTextDataCollect() || !props.isCollectAudio() || flashRecordPanel.gotPermission()) {
          exerciseList.getExercises(userID);
        }
        lastUser = userID;
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
  public int getSegmentRepeats() { return props.getSegmentRepeats(); }
  public boolean isArabicTextDataCollect() {  return props.isArabicTextDataCollect(); }
  public boolean useBkgColorForRef() {  return props.isBkgColorForRef(); }
  public boolean isDemoMode() {  return props.isDemoMode(); }
  public boolean isAutoCRTMode() {  return props.isAutocrt(); }
  public int getRecordTimeout() {  return props.getRecordTimeout(); }
  public boolean isDataCollectMode() {  return props.isDataCollectMode(); }
  public boolean isCollectAudio() {  return props.isCollectAudio(); }
  public boolean isMinimalUI() {  return props.isMinimalUI(); }
  public boolean isGrading() {  return props.isGrading(); }

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
    final DialogBox dialogBox;
    Button closeButton;

    dialogBox = new DialogBox();
    dialogBox.setText(title);

    closeButton = new Button("Close");
    closeButton.getElement().setId("closeButton");
    closeButton.setFocus(true);

    VerticalPanel dialogVPanel = new VerticalPanel();
    dialogVPanel.addStyleName("dialogVPanel");
    dialogVPanel.add(new Label(msg));

    dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
    dialogVPanel.add(closeButton);
    dialogBox.setWidget(dialogVPanel);

    closeButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        dialogBox.hide();
      }
    });
    dialogBox.center();
  }

  public void showStatus(String msg) { status.setText(msg); }

  public boolean loadNextExercise(Exercise current) { return exerciseList.loadNextExercise(current);  }
  public boolean loadPreviousExercise(Exercise current) { return exerciseList.loadPreviousExercise(current);  }
  public boolean onFirst(Exercise current) { return exerciseList.onFirst(current); }
}
