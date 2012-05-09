package mitll.langtest.client;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.DockLayoutPanel;
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
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import java.util.ArrayList;
import java.util.List;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class LangTest implements EntryPoint, UserFeedback, ExerciseController {
  public static final int WIDTH = 850, HEIGHT = 600;
  private DockLayoutPanel widgets = new DockLayoutPanel(Style.Unit.PX);
  private VerticalPanel exerciseList = new VerticalPanel();
  private ExercisePanel current = null;
  private List<Exercise> currentExercises = null;
  private List<HTML> progressMarkers = new ArrayList<HTML>();

  /**
	 * The message displayed to the user when the server cannot be reached or
	 * returns an error.
	 */
	private static final String SERVER_ERROR = "An error occurred while "
			+ "attempting to contact the server. Please check your network "
			+ "connection and try again.";

	/**
	 * Create a remote service proxy to talk to the server-side Greeting service.
	 */
/*
	private final GreetingServiceAsync greetingService = GWT
			.create(GreetingService.class);
*/

  private final LangTestDatabaseAsync service = GWT.create(LangTestDatabase.class);
  private Label status;

  public void onModuleLoad() {
    RootPanel.get().add(widgets);
    widgets.setSize(WIDTH + "px", HEIGHT + "px");

    setupErrorDialog();
    widgets.addWest(exerciseList,200);
    exerciseList.add(new HTML("<h1></h1>"));
    exerciseList.add(new HTML("<h2>Items</h2>"));
    widgets.addNorth(new HTML("<h1>Welcome to DLI Language Testing</h1>"), 80);
    service.getExercises(new AsyncCallback<List<Exercise>>() {
      public void onFailure(Throwable caught) {
        showErrorMessage("Server error - couldn't get exercises.");
      }

      public void onSuccess(List<Exercise> result) {
        currentExercises = result; // remember current exercises
        boolean first = true;
        for (Exercise e : result) {
          HTML w = new HTML("<b>" + e.getID() + "</b>");
          w.setStylePrimaryName("exercise");
          exerciseList.add(w);
          progressMarkers.add(w);
          if (first) {
            w.setStyleDependentName("highlighted", true);
            first = false;
            loadExercise(e);
          }
        }
      }
    });
    status = new Label();
    widgets.addSouth(status, 100);
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

    //final Label textToServerLabel = new Label();
    // final HTML serverResponseLabel = new HTML();
    VerticalPanel dialogVPanel = new VerticalPanel();
    dialogVPanel.addStyleName("dialogVPanel");
    //  dialogVPanel.add(new HTML("<b>Sending name to the server:</b>"));
    //dialogVPanel.add(textToServerLabel);
    //  dialogVPanel.add(new HTML("<br><b>Server replies:</b>"));
    // dialogVPanel.add(serverResponseLabel);
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
    //  serverResponseLabel
    //   .addStyleName("serverResponseLabelError");
    //serverResponseLabel.setHTML(SERVER_ERROR);
    dialogBox.center();
    closeButton.setFocus(true);
  }

  public void showStatus(String msg) { status.setText(msg); }

  private void loadExercise(Exercise e) {
    if (current != null) {
      widgets.remove(current);
    }
    widgets.add(current = new ExercisePanel(e, service, this, this));
    int i = currentExercises.indexOf(e);

    HTML html;
    if (i > 0) {
      html = progressMarkers.get(i - 1);
      html.setStyleDependentName("highlighted", false);
    }
    html = progressMarkers.get(i);
    html.setStyleDependentName("highlighted", true);
  }

  public boolean loadNextExercise(Exercise current) {
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
				service.test(new AsyncCallback<Void>() {
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

				/*	public void onSuccess(String result) {
						dialogBox.setText("Remote Procedure Call 2");
						serverResponseLabel
								.removeStyleName("serverResponseLabelError");
						serverResponseLabel.setHTML(result);
						dialogBox.center();
						closeButton.setFocus(true);
					}*/

					public void onSuccess(Void result) {
						dialogBox.setText("Remote Procedure Call 2");
						serverResponseLabel
								.removeStyleName("serverResponseLabelError");
						serverResponseLabel.setHTML("got here!");
						dialogBox.center();
						closeButton.setFocus(true);						
					}
				});
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
