package mitll.langtest.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.recorder.FlashRecordPanel;
import mitll.langtest.client.recorder.SaveNotification;
import mitll.langtest.shared.Exercise;

import java.util.ArrayList;
import java.util.List;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class LangTest implements EntryPoint, UserFeedback, ExerciseController, UserNotification {
  // TODO : consider putting these in the .css file?
  public static final int WIDTH = 1440, HEIGHT = 1080;
  private static final int HEADER_HEIGHT = 80;
  private static final int FOOTER_HEIGHT = 40;
  public  static final int EXERCISE_LIST_WIDTH = 200;
  private static final int EAST_WIDTH = 40;

  private VerticalPanel exerciseList = new VerticalPanel();
  private Panel currentExerciseVPanel = new SimplePanel();
  private ExercisePanel current = null;
  private VerticalPanel items;
  private List<Exercise> currentExercises = null;
  private List<HTML> progressMarkers = new ArrayList<HTML>();
  private int currentExercise = 0;
  private Label status;
  private UserManager user;
  private FlashRecordPanel flashRecordPanel;
  private PopupPanel recordPopup;
  private boolean flashRecordPanelInited;
  private long lastUser = -1;

  /**
	 * The message displayed to the user when the server cannot be reached or
	 * returns an error.
	 */
/*	private static final String SERVER_ERROR = "An error occurred while "
			+ "attempting to contact the server. Please answerPanel your network "
			+ "connection and try again.";*/

  private final LangTestDatabaseAsync service = GWT.create(LangTestDatabase.class);

  public void onModuleLoad() {
    user = new UserManager(this,service);

    DockLayoutPanel widgets = new DockLayoutPanel(Style.Unit.PX);
    RootPanel.get().add(widgets);

    widgets.setSize(WIDTH + "px", HEIGHT + "px");

    // header/title line
    DockLayoutPanel hp = new DockLayoutPanel(Style.Unit.PX);
    HTML title = new HTML("<h1>DLI Language Testing</h1>");
    hp.addEast(getLogout(),EAST_WIDTH);
    hp.add(title);

    widgets.addNorth(hp, HEADER_HEIGHT);
    widgets.addSouth(status = new Label(), FOOTER_HEIGHT);
    widgets.addWest(exerciseList, EXERCISE_LIST_WIDTH);
    widgets.add(currentExerciseVPanel);

    setupErrorDialog();

    currentExerciseVPanel.addStyleName("currentExercisePanel");
    this.items = new VerticalPanel();
    ScrollPanel itemScroller = new ScrollPanel(items);
    itemScroller.setSize(EXERCISE_LIST_WIDTH +"px",(HEIGHT - HEADER_HEIGHT - FOOTER_HEIGHT - 60) + "px"); // 54
    exerciseList.add(new HTML("<h2>Items</h2>"));
    exerciseList.add(itemScroller);

    setupRecordPopup();

    login();
  }

  /**
   * Has both a logout and a users link
   * @return
   */
  private Widget getLogout() {
    Anchor logout = new Anchor("Logout");
    DockLayoutPanel hp2 = new DockLayoutPanel(Style.Unit.PX);
    VerticalPanel vp = new VerticalPanel();
    vp.add(logout);
    hp2.addSouth(vp, 48);
    logout.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        user.clearUser();
        removeCurrentExercise();
        items.clear();

        login();
      }
    });

    Anchor users = new Anchor("Users");
    vp.add(users);

    users.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        user.showUsers();
      }
    });
    return hp2;
  }

  /**
   * @see UserManager#login
   * @see UserManager#storeUser(long)
   * @param userID
   */
  public void gotUser(long userID) {
    System.out.println("gotUser " + userID + " vs " + lastUser);

    if (userID != lastUser) {
      getExercises(userID);
      lastUser = userID;
    }
  }

  /**
   * Get exercises for this user.
   * @param userID
   */
  private void getExercises(long userID) {
    //System.out.println("loading exercises for " + userID);
    service.getExercises(userID, new AsyncCallback<List<Exercise>>() {
      public void onFailure(Throwable caught) {
        showErrorMessage("Server error - couldn't get exercises.");
      }

      public void onSuccess(List<Exercise> result) {
        currentExercises = result; // remember current exercises
        for (final Exercise e : result) {
          addExerciseToList(e, items);
        }
        loadFirstExercise();
      }
    });
  }

  private void login()  { user.login(); }
  public int getUser() { return user.getUser(); }

  private void addExerciseToList(final Exercise e, VerticalPanel items) {
    final HTML w = new HTML("<b>" + e.getID() + "</b>");
    w.setStylePrimaryName("exercise");
    items.add(w);
    progressMarkers.add(w);

    w.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        loadExercise(e);
      }
    });
    w.addMouseOverHandler(new MouseOverHandler() {
      public void onMouseOver(MouseOverEvent event) {
        w.addStyleName("clickable");
      }
    });
    w.addMouseOutHandler(new MouseOutHandler() {
      public void onMouseOut(MouseOutEvent event) {
        w.removeStyleName("clickable");
      }
    });
  }

  private void loadFirstExercise() {
    loadExercise(currentExercises.get(0));
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

  private void loadExercise(Exercise e) {
    login();

    removeCurrentExercise();
    if (e.getType() == Exercise.EXERCISE_TYPE.RECORD) {
      currentExerciseVPanel.add(current = new RecordExercisePanel(e, service, this, this));
    } else {
      currentExerciseVPanel.add(current = new ExercisePanel(e, service, this, this));
    }
    int i = currentExercises.indexOf(e);

    markCurrentExercise(i);
    currentExercise = i;
  }

  private void removeCurrentExercise() {
    if (current != null) {
      currentExerciseVPanel.remove(current);
      current = null;
    }
  }

  private void markCurrentExercise(int i) {
    HTML html = progressMarkers.get(currentExercise);
    html.setStyleDependentName("highlighted", false);
    html = progressMarkers.get(i);
    html.setStyleDependentName("highlighted", true);
  }

  public boolean loadNextExercise(Exercise current) {
    showStatus("");
    int i = currentExercises.indexOf(current);
    boolean onLast = i == currentExercises.size() - 1;
    if (onLast) {
      showErrorMessage("Test Complete! Thank you!");
    }
    else {
      loadExercise(currentExercises.get(i+1));
    }
    return onLast;
  }

  public boolean loadPreviousExercise(Exercise current) {
    showStatus("");
    int i = currentExercises.indexOf(current);
    boolean onFirst = i == 0;
    if (onFirst) {}
    else {
      loadExercise(currentExercises.get(i-1));
    }
    return onFirst;
  }

  public boolean onFirst(Exercise current) { return currentExercises.indexOf(current) == 0; }

  // popup recording -- TODO : refactor into its own class
  private void setupRecordPopup() {
    flashRecordPanel = new FlashRecordPanel("flashcontent");

    recordPopup = new PopupPanel(true);
    recordPopup.setStyleName("RecordPopup");
    recordPopup.setWidget(flashRecordPanel);

    showPopupAt(-100,-100);
    recordPopup.hide();
  }

  /**
   * @see RecordExercisePanel.ImageAnchor#onMouseOver(com.google.gwt.event.dom.client.MouseOverEvent)
   * @param exercise
   * @param question
   * @param sender
   * @param saveFeedbackWidget
   */
  public void showRecorder(Exercise exercise, int question, Widget sender, SaveNotification saveFeedbackWidget) {
    // Create the new popup.
    // Position the popup 1/3rd of the way down and across the screen, and
    // show the popup. Since the position calculation is based on the
    // offsetWidth and offsetHeight of the popup, you have to use the
    // setPopupPositionAndShow(callback) method. The alternative would
    // be to call show(), calculate the left and top positions, and
    // call setPopupPosition(left, top). This would have the ugly side
    // effect of the popup jumping from its original position to its
    // new position.

    int userID = user.getUser();
    if (userID == -1) {
      System.err.println("huh? no user? ");
     // user.setLangTest(this, exercise, question);   // callback when available
     // user.login();
    }
    else {
      flashRecordPanel.setUpload(userID, exercise, question);
    }
    // remember feedback widget so we can indicate when save is complete
    FlashRecordPanel.setSaveCompleteFeedbackWidget(saveFeedbackWidget);

    int left = sender.getAbsoluteLeft();
    int top = sender.getAbsoluteTop()-12;
    showPopupAt(left, top);
   }

  private void showPopupAt(int left, int top) {
    recordPopup.setPopupPosition(left, top);
    recordPopup.show();
    flashRecordPanel.reset();

    if (!flashRecordPanelInited) {  // TODO is this correct???
      System.out.println("doing initializeJS");
      flashRecordPanel.initializeJS(GWT.getModuleBaseURL(), "flashcontent");
      flashRecordPanelInited = true;
    }
  }
}
