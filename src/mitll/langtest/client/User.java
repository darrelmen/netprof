package mitll.langtest.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import mitll.langtest.shared.Exercise;

import java.util.Arrays;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 5/15/12
 * Time: 11:32 AM
 * To change this template use File | Settings | File Templates.
 */
public class User {
  private LangTestDatabaseAsync service;
  private LangTest langTest;
  private Exercise exercise;
  private int question;
  public User(LangTestDatabaseAsync service) { this.service = service; }

  // user tracking
  public void storeUser(int sessionID) {
    //String sessionID = "";//BrowserInfo.getBrowserInfo();/*(Get sessionID from server's response to your login request.)*/;
    final long DURATION = 1000 * 60 * 60 * 20 ; //duration remembering login. 2 weeks in this example.
    Date expires = new Date(System.currentTimeMillis() + DURATION);
    Cookies.setCookie("sid", "" + sessionID, expires, null, "/", false);
    if (langTest != null) {
      System.out.println("user logged in as " + sessionID);
      langTest.gotUser(sessionID, exercise, question);
    }
  }

  public void setLangTest(LangTest lt,Exercise exercise, int q) { langTest = lt; this.exercise = exercise; this.question = q; }

  public void login() {
    String sessionID = Cookies.getCookie("sid");
    if ( sessionID != null ) {
      System.out.println("login got " +sessionID);
      //checkWithServerIfSessionIdIsStillLegal();
    }
    else displayLoginBox();
  }

  public boolean isUserValid() {
    return Cookies.getCookie("sid") != null;
  }

  public int getUser() {
    String sid = Cookies.getCookie("sid");
    if (sid == null) {
      System.err.println("sid not set!");
      return -1;
    }
    return Integer.parseInt(sid);
  }

  public void clearUser() {
    Cookies.removeCookie("sid");
  }

  private void displayLoginBox() {

    final TextBox nameField = new TextBox();
    nameField.setText("GWT User");
    // final Label errorLabel = new Label();
    // final Button sendButton = new Button("Send");
    // final TextBox nameField = new TextBox();
    nameField.setText("GWT User");
    // final Label errorLabel = new Label();

    // We can add style names to widgets
    // sendButton.addStyleName("sendButton");
    // We can add style names to widgets
    // sendButton.addStyleName("sendButton");

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
    dialogBox.setText("Login Questions");
    dialogBox.setAnimationEnabled(true);

    // Enable glass background.
    dialogBox.setGlassEnabled(true);

    final Button closeButton = new Button("Close");
    // We can set the id of a widget by accessing its Element
    closeButton.getElement().setId("closeButton");
    final TextBox textToServerLabel = new TextBox();
    //final Choice
    //  serverResponseLabel = new HTML();

    // Add a drop box with the list types
    final ListBox dropBox = new ListBox(false);
    for (String s : Arrays.asList("Male", "Female")) {
      dropBox.addItem(s);
    }
    dropBox.ensureDebugId("cwListBox-dropBox");
    VerticalPanel dropBoxPanel = new VerticalPanel();
    dropBoxPanel.setSpacing(4);
    //  dropBoxPanel.add(new HTML(constants.cwListBoxSelectCategory()));
    dropBoxPanel.add(dropBox);


    VerticalPanel dialogVPanel = new VerticalPanel();
    dialogVPanel.addStyleName("dialogVPanel");
    dialogVPanel.add(new HTML("<b>Please enter your age:</b>"));
    dialogVPanel.add(textToServerLabel);
    dialogVPanel.add(new HTML("<br><b>Please select gender:</b>"));
    dialogVPanel.add(dropBoxPanel);
    dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
    dialogVPanel.add(closeButton);
    dialogBox.setWidget(dialogVPanel);


    // Add a handler to close the DialogBox


    // Create a handler for the sendButton and nameField
    class MyHandler implements ClickHandler, KeyUpHandler {
      /**
       * Fired when the user clicks on the sendButton.
       */
      public void onClick(ClickEvent event) {
        dialogBox.hide();

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
        //String textToServer = nameField.getText();
        System.out.println("got " + textToServerLabel.getText());

        // Then, we send the input to the server.
        //  sendButton.setEnabled(false);
        //textToServerLabel.setText(textToServer);
        //serverResponseLabel.setText("");
        service.addUser(Integer.parseInt(textToServerLabel.getText()),
          dropBox.getValue(dropBox.getSelectedIndex())
          ,
          new AsyncCallback<Integer>() {
            public void onFailure(Throwable caught) {
              // Show the RPC error message to the user
              dialogBox.setText("Remote Procedure Call - Failure");
              /*serverResponseLabel
                  .addStyleName("serverResponseLabelError");
              serverResponseLabel.setHTML(SERVER_ERROR);*/
              dialogBox.center();
              closeButton.setFocus(true);
            }

            public void onSuccess(Integer result) {
              System.out.println("server result is " +result);
              storeUser(result);
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
    //  sendButton.addClickHandler(handler);
    closeButton.addClickHandler(handler);

    int left = (Window.getClientWidth() - 0) / 3;
    int top = (Window.getClientHeight() - 0) / 3;
    dialogBox.setPopupPosition(left, top);

    dialogBox.show();
  }
}
