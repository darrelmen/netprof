package mitll.langtest.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
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
import mitll.langtest.client.grading.GradingExercisePanelFactory;
import mitll.langtest.client.recorder.FlashRecordPanelHeadless;
import mitll.langtest.client.recorder.MicPermission;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.client.user.UserNotification;
import mitll.langtest.client.user.UserTable;
import mitll.langtest.shared.Exercise;

import java.util.HashMap;
import java.util.Map;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class LangTest implements EntryPoint, UserFeedback, ExerciseController, UserNotification {
  // TODO : consider putting these in the .css file?
  private static final int HEADER_HEIGHT = 80;
  private static final int FOOTER_HEIGHT = 40;
  public  static final int EXERCISE_LIST_WIDTH = 200;
  private static final int EAST_WIDTH = 90;
  private static final String DLI_LANGUAGE_TESTING = "NetPron 2";

  private Panel currentExerciseVPanel = new VerticalPanel();
  private ExerciseList exerciseList;
  private Label status;
  private UserManager userManager;
  private final UserTable userTable = new UserTable();
  private ResultManager resultManager;
  private FlashRecordPanelHeadless flashRecordPanel;

  private long lastUser = -1;
  private boolean grading = false;
  private boolean englishOnlyMode = false;
  private final LangTestDatabaseAsync service = GWT.create(LangTestDatabase.class);
  private ExercisePanelFactory factory = new ExercisePanelFactory(service, this, this);

  private final BrowserCheck browserCheck = new BrowserCheck();
  private ScrollPanel itemScroller;

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

    // use a deferred command so that the handler catches onModuleLoad2() exceptions
    Scheduler.get().scheduleDeferred(new Command() {
      public void execute() {
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
    userManager = new UserManager(this,service);
    resultManager = new ResultManager(service, this);

    DockLayoutPanel widgets = new DockLayoutPanel(Style.Unit.PX);
    RootPanel.get().add(widgets);

    setMainWindowSize(widgets);
    addResizeHandler(widgets);

    // header/title line
    DockLayoutPanel hp = new DockLayoutPanel(Style.Unit.PX);
    HTML title = new HTML("<h1>" + DLI_LANGUAGE_TESTING + "</h1>");
    browserCheck.getBrowserAndVersion();
    hp.addEast(getLogout(),EAST_WIDTH);
  //  hp.setHeight("180px");
    hp.add(title);

    widgets.addNorth(hp, HEADER_HEIGHT);
    widgets.addSouth(status = new Label(), FOOTER_HEIGHT);
    VerticalPanel exerciseListPanel = new VerticalPanel();
    widgets.addWest(exerciseListPanel, EXERCISE_LIST_WIDTH);

    // set up center panel, initially with flash record panel
    ScrollPanel sp = new ScrollPanel();
    sp.add(currentExerciseVPanel);
    widgets.add(sp);
    makeFlashContainer();
    currentExerciseVPanel.add(flashRecordPanel);
    currentExerciseVPanel.addStyleName("currentExercisePanel");

    setupErrorDialog();

    // set up left side exercise list
    makeExerciseList(exerciseListPanel);

    modeSelect();

    browserCheck.checkForCompatibleBrowser();

    Element elementById = DOM.getElementById("title-tag");   // set the page title to be consistent
    elementById.setInnerText(DLI_LANGUAGE_TESTING);
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

  private void makeExerciseList(VerticalPanel exerciseListPanel) {
    this.exerciseList = new ExerciseList(currentExerciseVPanel,service,this, factory);
    itemScroller = new ScrollPanel(this.exerciseList);
    setExerciseListSize();
    exerciseListPanel.add(new HTML("<h2>Items</h2>"));
    exerciseListPanel.add(itemScroller);
  }

  private void setMainWindowSize(DockLayoutPanel widgets) {
    widgets.setSize((Window.getClientWidth()-EAST_WIDTH) + "px", (Window.getClientHeight()-FOOTER_HEIGHT) + "px");
  }

  private void setExerciseListSize() {
    itemScroller.setSize(EXERCISE_LIST_WIDTH +"px",(Window.getClientHeight() - (2*HEADER_HEIGHT) - FOOTER_HEIGHT - 60) + "px"); // 54
  }

  /**
   * Check the URL parameters for special modes.
   */
  private void modeSelect() {
    String isGrading = Window.Location.getParameter("grading");
    String isEnglish = Window.Location.getParameter("english");
    englishOnlyMode = isEnglish != null && !isEnglish.equals("false");

    if (englishOnlyMode || (isGrading != null && !isGrading.equals("false"))) {
      //System.out.println("jump to choice box " + isGrading);
      userManager.graderLogin();
    }
    else {
      login();
    }
  }

  /**
   * Hookup feedback for events from Flash generated from the user's response to the Mic Access dialog
   *
   * @see mitll.langtest.client.recorder.FlashRecordPanelHeadless#micConnected()
   */
  private void makeFlashContainer() {
    flashRecordPanel = new FlashRecordPanelHeadless();

    FlashRecordPanelHeadless.setMicPermission(new MicPermission() {
      public void gotPermission() {
        System.out.println("got permission!");
        flashRecordPanel.hide();
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
  //  System.out.println("setGrading " + g);
    this.grading = g;

    if (grading) {
      exerciseList.setFactory(new GradingExercisePanelFactory(service, this, this), userManager, grading, englishOnlyMode ? 2 : 1);
      lastUser = -1; // no user
    }
    else {
      exerciseList.setFactory(new ExercisePanelFactory(service, this, this), userManager, grading, 1);
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
    final Image image = new Image("images/really.png");
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
    //DockLayoutPanel hp2 = new DockLayoutPanel(Style.Unit.PX);
    VerticalPanel vp = new VerticalPanel();
    //hp2.addSouth(vp, 75);

    // add logout link
    Anchor logout = new Anchor("Logout");
    vp.add(logout);
    logout.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        resetState();
      }
    });

    Anchor users = new Anchor("Users");
    users.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        userTable.showUsers(service);
      }
    });
    vp.add(users);

    Anchor showResults = new Anchor("Results");
    showResults.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        resultManager.showResults();
      }
    });
    vp.add(showResults);

    // HTML html = new HTML(browser + " " +ver);
    //html.getElement().setId("status");
   // SimplePanel sp = new SimplePanel();
   // sp.add(html);

    // no click handler for this for now
    Anchor status = new Anchor(browserCheck.browser + " " +browserCheck.ver +" 9/19");
    vp.add(status);

    return vp;//hp2;
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
  public void login() {
    userManager.login();
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
//    System.out.println("gotUser " + userID + " vs " + lastUser);
    //askedMode = true;
    grading = false;
    setGrading(grading);
    flashRecordPanel.initFlash();

    if (userID != lastUser) {
      if (flashRecordPanel.gotPermission()) {
        exerciseList.getExercises(userID);
      }
      lastUser = userID;
    }
  }

  /**
   * @see mitll.langtest.client.exercise.ExercisePanel#postAnswers
   * @see mitll.langtest.client.recorder.SimpleRecordPanel#stopClicked
   * @return
   */
  public int getUser() { return userManager.getUser(); }
  public String getGrader() { return userManager.getGrader(); }
  public boolean getEnglishOnly() { return englishOnlyMode; }

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

  private DialogBox dialogBox;
  private Button closeButton;
  private void setupErrorDialog() {
    dialogBox = new DialogBox();
    dialogBox.setText("Information");
    dialogBox.setAnimationEnabled(true);
    this.closeButton = new Button("Close");
    // We can set the id of a widget by accessing its Element
    closeButton.getElement().setId("closeButton");

    VerticalPanel dialogVPanel = new VerticalPanel();
    dialogVPanel.addStyleName("dialogVPanel");
    dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
    dialogVPanel.add(closeButton);
    dialogBox.setWidget(dialogVPanel);

    closeButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        dialogBox.hide();
      }
    });
  }

  public void showErrorMessage(String msg) {
    dialogBox.setText(msg);
    dialogBox.center();
    closeButton.setFocus(true);
  }

  public void showStatus(String msg) { status.setText(msg); }

  public boolean loadNextExercise(Exercise current) { return exerciseList.loadNextExercise(current);  }
  public boolean loadPreviousExercise(Exercise current) { return exerciseList.loadPreviousExercise(current);  }
  public boolean onFirst(Exercise current) { return exerciseList.onFirst(current); }
}
