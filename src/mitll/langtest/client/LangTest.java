package mitll.langtest.client;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.recorder.FlashRecordPanel;
import mitll.langtest.client.recorder.SaveNotification;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.FieldVerifier;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class LangTest implements EntryPoint, UserFeedback, ExerciseController, UserNotification {
  public static final int WIDTH = 1440, HEIGHT = 1080;
  private static final int HEADER_HEIGHT = 80;
  private static final int FOOTER_HEIGHT = 40;
  public static final int EXERCISE_LIST_WIDTH = 200;
  private VerticalPanel exerciseList = new VerticalPanel();
  private Panel currentExerciseVPanel = new SimplePanel();
  private ExercisePanel current = null;
  private VerticalPanel items;
  private List<Exercise> currentExercises = null;
  private List<HTML> progressMarkers = new ArrayList<HTML>();
  private int currentExercise = 0;
  private Label status;
  private User user;
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
    user = new User(this,service);

    DockLayoutPanel widgets = new DockLayoutPanel(Style.Unit.PX);
    RootPanel.get().add(widgets);

    widgets.setSize(WIDTH + "px", HEIGHT + "px");
    HTML title = new HTML("<h1>DLI Language Testing</h1>");
    Anchor logout = new Anchor("Logout");
    logout.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        user.clearUser();
        loadFirstExercise();
      }
    });
    widgets.addNorth(title, HEADER_HEIGHT);
    widgets.addSouth(status = new Label(), FOOTER_HEIGHT);
    widgets.addWest(exerciseList, EXERCISE_LIST_WIDTH);
    widgets.addEast(logout, 40);
    widgets.add(currentExerciseVPanel);

    setupErrorDialog();

    currentExerciseVPanel.addStyleName("currentExercisePanel");
    this.items = new VerticalPanel();
    ScrollPanel itemScroller = new ScrollPanel(items);
    itemScroller.setSize(EXERCISE_LIST_WIDTH +"px",(HEIGHT - HEADER_HEIGHT - FOOTER_HEIGHT - 60) + "px"); // 54
    exerciseList.add(new HTML("<h2>Items</h2>"));
    exerciseList.add(itemScroller);

    flashRecordPanel = new FlashRecordPanel("flashcontent");

    recordPopup = new PopupPanel(true);
    recordPopup.setStyleName("RecordPopup");
    recordPopup.setWidget(flashRecordPanel);

    login();
  }

  public void gotUser(long userID) {
    System.out.println("gotUser " + userID + " vs " + lastUser);

    if (userID != lastUser) {
      items.clear();
      getExercises(userID);
      lastUser = userID;
    }
  }

  private void getExercises(long userID) {
    System.out.println("loading exercises for " + userID);
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

    System.err.println("loading exercise " + e.getID() + " " +e.getType());
    if (current != null) {
      currentExerciseVPanel.remove(current);
    }
    if (e.getType() == Exercise.EXERCISE_TYPE.RECORD) {
      currentExerciseVPanel.add(current = new RecordExercisePanel(e, service, this, this));
    } else {
      currentExerciseVPanel.add(current = new ExercisePanel(e, service, this, this));
    }
    int i = currentExercises.indexOf(e);

    HTML html = progressMarkers.get(currentExercise);
    html.setStyleDependentName("highlighted", false);
    html = progressMarkers.get(i);
    html.setStyleDependentName("highlighted", true);
    currentExercise = i;
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
    recordPopup.setPopupPosition(left, top);
    recordPopup.show();

    if (!flashRecordPanelInited) {  // TODO is this correct???
      System.out.println("doing initializeJS");
      flashRecordPanel.initializeJS(GWT.getModuleBaseURL(), "flashcontent");
      flashRecordPanelInited = true;
    }
   }

  /**
     * This is the entry point method.
     */
	public void onModuleLoad2() {
		final Button sendButton = new Button("Send");
		final TextBox nameField = new TextBox();
		nameField.setText("GWT User");
		final Label errorLabel = new Label();

		// We can add style names to widgets
		sendButton.addStyleName("sendButton");

		// Add the nameField and sendButton to the RootPanel
		// Use RootPanel.get() to get the entire body element
/*		RootPanel.get("nameFieldContainer").add(nameField);
		RootPanel.get("sendButtonContainer").add(sendButton);
		RootPanel.get("errorLabelContainer").add(errorLabel);*/

		// Focus the cursor on the name field when the app loads
		nameField.setFocus(true);
		nameField.selectAll();

		// Create the popup dialog box
		final DialogBox dialogBox = new DialogBox();
		dialogBox.setText("Remote Procedure Call");
		dialogBox.setAnimationEnabled(true);
		final Button closeButton = new Button("Close");
		// We can set the id of a widget by accessing its Element
		closeButton.getElement().setId("closeButton");
		final Label textToServerLabel = new Label();
		final HTML serverResponseLabel = new HTML();
		VerticalPanel dialogVPanel = new VerticalPanel();
		dialogVPanel.addStyleName("dialogVPanel");
		dialogVPanel.add(new HTML("<b>Sending name to the server:</b>"));
		dialogVPanel.add(textToServerLabel);
		dialogVPanel.add(new HTML("<br><b>Server replies:</b>"));
		dialogVPanel.add(serverResponseLabel);
		dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
		dialogVPanel.add(closeButton);
		dialogBox.setWidget(dialogVPanel);

		// Add a handler to close the DialogBox
		closeButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				dialogBox.hide();
				sendButton.setEnabled(true);
				sendButton.setFocus(true);
			}
		});

		// Create a handler for the sendButton and nameField
		class MyHandler implements ClickHandler, KeyUpHandler {
			/**
			 * Fired when the user clicks on the sendButton.
			 */
			public void onClick(ClickEvent event) {
				sendNameToServer();
			}

			/**
			 * Fired when the user types in the nameField.
			 */
			public void onKeyUp(KeyUpEvent event) {
				if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
					sendNameToServer();
				}
			}

			/**
			 * Send the name from the nameField to the server and wait for a response.
			 */
			private void sendNameToServer() {
				// First, we validate the input.
				errorLabel.setText("");
				String textToServer = nameField.getText();
				if (!FieldVerifier.isValidName(textToServer)) {
					errorLabel.setText("Please enter at least four characters");
					return;
				}

				// Then, we send the input to the server.
				sendButton.setEnabled(false);
				textToServerLabel.setText(textToServer);
				serverResponseLabel.setText("");
	/*			service.test(new AsyncCallback<Void>() {
					public void onFailure(Throwable caught) {
						// Show the RPC error message to the user
						dialogBox
								.setText("Remote Procedure Call - Failure");
						serverResponseLabel
								.addStyleName("serverResponseLabelError");
						serverResponseLabel.setHTML(SERVER_ERROR);
						dialogBox.center();
						closeButton.setFocus(true);
					}

					public void onSuccess(String result) {
						dialogBox.setText("Remote Procedure Call 2");
						serverResponseLabel
								.removeStyleName("serverResponseLabelError");
						serverResponseLabel.setHTML(result);
						dialogBox.center();
						closeButton.setFocus(true);
					}

					public void onSuccess(Void result) {
						dialogBox.setText("Remote Procedure Call 2");
						serverResponseLabel
								.removeStyleName("serverResponseLabelError");
						serverResponseLabel.setHTML("got here!");
						dialogBox.center();
						closeButton.setFocus(true);						
					}
				});*/
		/*		greetingService.greetServer(textToServer,
						new AsyncCallback<String>() {
							public void onFailure(Throwable caught) {
								// Show the RPC error message to the user
								dialogBox
										.setText("Remote Procedure Call - Failure");
								serverResponseLabel
										.addStyleName("serverResponseLabelError");
								serverResponseLabel.setHTML(SERVER_ERROR);
								dialogBox.center();
								closeButton.setFocus(true);
							}

							public void onSuccess(String result) {
								dialogBox.setText("Remote Procedure Call");
								serverResponseLabel
										.removeStyleName("serverResponseLabelError");
								serverResponseLabel.setHTML(result);
								dialogBox.center();
								closeButton.setFocus(true);
							}
						});*/
			}
		}

		// Add a handler to send the name to the server
		MyHandler handler = new MyHandler();
		sendButton.addClickHandler(handler);
		nameField.addKeyUpHandler(handler);
	}
}
