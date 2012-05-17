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
  private static final int EXPIRATION_MINUTES = 30;
  private static final int MIN_AGE = 6;
  private static final int MAX_AGE = 90;
  private LangTestDatabaseAsync service;
  private UserNotification langTest;
  //Logger logger = Logger.getLogger("User");

  public User(UserNotification lt, LangTestDatabaseAsync service) {
    this.langTest = lt;
    this.service = service;
  }

  // user tracking
  public void storeUser(long sessionID) {
    final long DURATION = 1000 * 60 * 60 * EXPIRATION_MINUTES; //duration remembering login. 2 weeks in this example.
    Date expires = new Date(System.currentTimeMillis() + DURATION);
    Cookies.setCookie("sid", "" + sessionID, expires);
    if (langTest != null) {
      langTest.gotUser(sessionID);
    }
  }

  /**
   * @see mitll.langtest.client.LangTest#login()
   */
  public void login() {
    int user = getUser();
    if ( user != -1) {
     // alert("login Cookie now " + Cookies.getCookie("sid"));
//      alert("user:login sessionID not null = " + sessionID + " and there are " + Cookies.getCookieNames().size() + " cookies!");
      langTest.gotUser(user);
    }
    else {
   //   alert("login Cookie now " + Cookies.getCookie("sid"));
      displayLoginBox();
    }
  }

  public int getUser() {
    String sid = Cookies.getCookie("sid");
    if (sid == null || sid.equals("-1")) {
     // System.err.println("sid not set!");
      return -1;
    }
    return Integer.parseInt(sid);
  }

  public void clearUser() {
    Cookies.setCookie("sid","-1");
    //alert("clearUser Cookie now " + Cookies.getCookie("sid"));
    //Cookies.removeCookie("sid");    // this doesn't always seem to work???
  }

  private void displayLoginBox() {
    final TextBox nameField = new TextBox();
    nameField.setText("GWT User");
    nameField.setText("GWT User");

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
    closeButton.setEnabled(false);

    // We can set the id of a widget by accessing its Element
    closeButton.getElement().setId("closeButton");
    final TextBox ageEntryBox = new TextBox();
    ageEntryBox.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        String text = ageEntryBox.getText();
        if (text.length() == 0) {
          closeButton.setEnabled(false);
          return;
        }
        try {
          int age = Integer.parseInt(text);
          closeButton.setEnabled (age > MIN_AGE && age < MAX_AGE);
        } catch (NumberFormatException e) {
          closeButton.setEnabled(false);
        }
      }
    });

    // Add a drop box with the list types
    final ListBox genderBox = new ListBox(false);
    for (String s : Arrays.asList("Male", "Female")) {
      genderBox.addItem(s);
    }
    genderBox.ensureDebugId("cwListBox-dropBox");
    VerticalPanel genderPanel = new VerticalPanel();
    genderPanel.setSpacing(4);
    //  dropBoxPanel.add(new HTML(constants.cwListBoxSelectCategory()));
    genderPanel.add(genderBox);

    // add experience drop box
    final ListBox experienceBox = new ListBox(false);
    for (int i = 0; i < 21; i += 3) {
      experienceBox.addItem(i + " to " + (i + 3) + " months");
    }
    experienceBox.addItem("More than 22 months");
    experienceBox.addItem("Native Speaker");
    experienceBox.ensureDebugId("cwListBox-dropBox");
    VerticalPanel experiencePanel = new VerticalPanel();
    experiencePanel.setSpacing(4);
    //  dropBoxPanel.add(new HTML(constants.cwListBoxSelectCategory()));
    experiencePanel.add(experienceBox);

    VerticalPanel dialogVPanel = new VerticalPanel();
    dialogVPanel.addStyleName("dialogVPanel");
    dialogVPanel.add(new HTML("<b>Please enter your age</b>"));
    dialogVPanel.add(ageEntryBox);
    dialogVPanel.add(new HTML("<br><b>Please select gender</b>"));
    dialogVPanel.add(genderPanel);
    dialogVPanel.add(new HTML("<br><b>Please select months of experience</b>"));
    dialogVPanel.add(experiencePanel);
    dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
    dialogVPanel.add(closeButton);
    dialogBox.setWidget(dialogVPanel);

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
        int monthsOfExperience = experienceBox.getSelectedIndex()*3;
        if (monthsOfExperience == 21) {
          monthsOfExperience++;
        }
        else if (monthsOfExperience == 24) {
          monthsOfExperience = 20*24;
        }
        service.addUser(Integer.parseInt(ageEntryBox.getText()),
          genderBox.getValue(genderBox.getSelectedIndex()),
          monthsOfExperience, new AsyncCallback<Long>() {
          public void onFailure(Throwable caught) {
            // Show the RPC error message to the user
            dialogBox.setText("Remote Procedure Call - Failure");
            dialogBox.center();
            closeButton.setFocus(true);
          }

          public void onSuccess(Long result) {
            System.out.println("server result is " + result);
            storeUser(result);
          }
        });
      }
    }

    // Add a handler to send the name to the server
    MyHandler handler = new MyHandler();
    closeButton.addClickHandler(handler);

    int left = (Window.getClientWidth() - 0) / 3;
    int top  = (Window.getClientHeight() - 0) / 3;
    dialogBox.setPopupPosition(left, top);

    dialogBox.show();
  }
}
