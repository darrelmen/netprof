package mitll.langtest.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Handles storing cookies for users, etc. IF user ids are stored as cookies.
 *
 * Prompts user for login info.
 *
 * NOTE : will keep prompting them if the browser doesn't let you store cookies.
 *
 * User: GO22670
 * Date: 5/15/12
 * Time: 11:32 AM
 * To change this template use File | Settings | File Templates.
 */
public class UserManager {
  private static final int EXPIRATION_HOURS = 240;
  private static final int MIN_AGE = 6;
  private static final int MAX_AGE = 90;
  private static final int NO_USER_SET = -1;
  private final LangTestDatabaseAsync service;
  private final UserNotification langTest;
  private boolean useCookie = false;
  private long userID = NO_USER_SET;

  public UserManager(UserNotification lt, LangTestDatabaseAsync service) {
    this.langTest = lt;
    this.service = service;
  }

  // user tracking

  /**
   * @param sessionID from database
   * @see #displayLoginBox()
   */
  private void storeUser(long sessionID) {
    final long DURATION = 1000 * 60 * 60 * EXPIRATION_HOURS; //duration remembering login
    Date expires = new Date(System.currentTimeMillis() + DURATION);
    if (useCookie) {
      Cookies.setCookie("sid", "" + sessionID, expires);
    } else {
      userID = sessionID;
    }
    langTest.gotUser(sessionID);
  }

  /**
   * @see mitll.langtest.client.LangTest#login()
   */
  public void login() {
    int user = getUser();
    if (user != NO_USER_SET) {
      langTest.gotUser(user);
    } else {
      displayLoginBox();
    }
  }

  public void logout() {
    Cookies.setCookie("grader","");

    System.out.println("grader now " + getGrader());
  }

  /**
   * @return id of user
   * @see mitll.langtest.client.LangTest#getUser
   */
  public int getUser() {
    if (useCookie) {
      String sid = Cookies.getCookie("sid");
      if (sid == null || sid.equals("" + NO_USER_SET)) {
        return NO_USER_SET;
      }
      return Integer.parseInt(sid);
    } else {
      return (int) userID;
    }
  }

  public String getGrader() { return Cookies.getCookie("grader"); }

  /**
   * @see mitll.langtest.client.LangTest#getLogout()
   */
  public void clearUser() {
    if (useCookie) {
      Cookies.setCookie("sid", "" + NO_USER_SET);
      logout();
    } else {
      userID = NO_USER_SET;
    }
  }

  public void displayChoiceBox() {

    final DialogBox dialogBox = new DialogBox();
    // Enable glass background.
    dialogBox.setGlassEnabled(true);
    VerticalPanel dialogVPanel = new VerticalPanel();
    dialogVPanel.addStyleName("dialogVPanel");
    dialogVPanel.addStyleName("center");
    dialogVPanel.add(new HTML("<b>Please choose to </b><br>"));
    //dialogVPanel.add(new HTML(""));

    SimplePanel spacer = new SimplePanel();
    spacer.setSize("20px", "20px");
    dialogVPanel.add(spacer);

    Anchor w = new Anchor("Take a test");
    w.addStyleName("paddedHorizontalPanel");
    w.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        dialogBox.hide();
        langTest.setGrading(false);

        displayLoginBox();
      }
    });
    dialogVPanel.add(w);
    HTML w2 = new HTML("<br><i>&nbsp;&nbsp;&nbsp;Or</i><br>");
    w2.addStyleName("paddedHorizontalPanel");

    spacer = new SimplePanel();
    spacer.addStyleName("paddedHorizontalPanel");
    spacer.setSize("20px", "20px");
    spacer.add(w2);
    dialogVPanel.add(spacer);

    spacer = new SimplePanel();
    spacer.setSize("20px", "20px");
    dialogVPanel.add(spacer);
    spacer = new SimplePanel();
    spacer.setSize("20px", "20px");
    dialogVPanel.add(spacer);

    Anchor w1 = new Anchor("Grade tests");
    dialogVPanel.add(w1);
    w1.addStyleName("paddedHorizontalPanel");

    w1.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        dialogBox.hide();
        String sid = Cookies.getCookie("grader");
        if (sid == null || sid.length() == 0) {
          displayGraderLogin();
        }
        else {
          System.out.println("grader cookie " + getGrader());
          langTest.setGrading(true);
        }
      }
    });

    dialogBox.setWidget(dialogVPanel);
    show(dialogBox);
  }

  private void displayGraderLogin() {
    final DialogBox dialogBox = new DialogBox();
    dialogBox.setText("Grader Login");
    dialogBox.setAnimationEnabled(true);

    // Enable glass background.
    dialogBox.setGlassEnabled(true);

    final TextBox user = new TextBox();

    final Button closeButton = new Button("Login");
    closeButton.setEnabled(false);

    // We can set the id of a widget by accessing its Element
    closeButton.getElement().setId("closeButton");
    user.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        String text = user.getText();
        if (text.length() == 0) {
          closeButton.setEnabled(false);
          return;
        }

        service.graderExists(text, new AsyncCallback<Boolean>() {
          public void onFailure(Throwable caught) {}
          public void onSuccess(Boolean result) {
            closeButton.setEnabled(result);
          }
        });
      }
    });


    VerticalPanel dialogVPanel = new VerticalPanel();
    dialogVPanel.addStyleName("dialogVPanel");
    dialogVPanel.add(new HTML("<b>Please enter userid</b>"));
    dialogVPanel.add(user);
    dialogVPanel.add(new HTML("<b>Or click here to</b>"));

    Anchor w1 = new Anchor("Register");
    dialogVPanel.add(w1);
    w1.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        if (user.getText().length() > 0) {
          service.graderExists(user.getText(), new AsyncCallback<Boolean>() {
            public void onFailure(Throwable caught) {
            }

            public void onSuccess(Boolean result) {
              if (!result) service.addGrader(user.getText(), new AsyncCallback<Void>() {
                public void onFailure(Throwable caught) {
                }

                public void onSuccess(Void result) {
                  setGraderCookie(user.getText());
                  closeButton.setEnabled(true);
                  closeButton.click();
                }
              });
              else {
                closeButton.setEnabled(false);
              }
            }
          });
        }
      }
    });
    w1.addStyleName("paddedHorizontalPanel");

    dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
    dialogVPanel.add(closeButton);
    dialogBox.setWidget(dialogVPanel);

    closeButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        dialogBox.hide();
        setGraderCookie(user.getText());
        langTest.setGrading(true);
      }
    });
    show(dialogBox);
  }

  private void setGraderCookie(String user) {
    System.out.println("added " + user);
    final long DURATION = 1000 * 60 * 60 * EXPIRATION_HOURS; //duration remembering login
    Date expires = new Date(System.currentTimeMillis() + DURATION);
    Cookies.setCookie("grader", "" + user, expires);
  }

  private void displayLoginBox() {
    langTest.setGrading(false);
    // Create the popup dialog box
    final DialogBox dialogBox = new DialogBox();
    dialogBox.setText("Login Questions");
    dialogBox.setAnimationEnabled(true);

    // Enable glass background.
    dialogBox.setGlassEnabled(true);

    final TextBox ageEntryBox = new TextBox();
    final Button closeButton = makeCloseButton(ageEntryBox);

    // Add a drop box with the list types
    final ListBox genderBox = new ListBox(false);
    for (String s : Arrays.asList("Male", "Female")) {
      genderBox.addItem(s);
    }
    genderBox.ensureDebugId("cwListBox-dropBox");
    VerticalPanel genderPanel = new VerticalPanel();
    genderPanel.setSpacing(4);
    genderPanel.add(genderBox);

    // add experience drop box
    final ListBox experienceBox = new ListBox(false);
    List<String> choices = Arrays.asList(
      "0-3 months (Semester 1)",
      "4-6 months (Semester 1)",
      "7-9 months (Semester 2)",
      "10-12 months (Semester 2)",
      "13-16 months (Semester 3)",
      "16+ months",
      "Native speaker");
    final int lastChoice = choices.size()-1;
    for (String c : choices) {
      experienceBox.addItem(c);
    }
    experienceBox.ensureDebugId("cwListBox-dropBox");
    VerticalPanel experiencePanel = new VerticalPanel();
    experiencePanel.setSpacing(4);
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
        if (experienceBox.getSelectedIndex() == lastChoice) {
          monthsOfExperience = 20*12;
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

    show(dialogBox);
  }

  private void show(DialogBox dialogBox) {
    int left = Window.getClientWidth() / 3;
    int top  = Window.getClientHeight() / 3;
    dialogBox.setPopupPosition(left, top);

    dialogBox.show();
  }

  private Button makeCloseButton(final TextBox ageEntryBox) {
    final Button closeButton = new Button("Login");
    closeButton.setEnabled(false);

    // We can set the id of a widget by accessing its Element
    closeButton.getElement().setId("closeButton");
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
    return closeButton;
  }
}
