package mitll.langtest.client.user;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTestDatabaseAsync;

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
  private static final int TEST_AGE = 100;
  private static final int NO_USER_SET = -1;
  private static final String GRADING = "grading";
  private static final List<String> EXPERIENCE_CHOICES = Arrays.asList(
      "0-3 months (Semester 1)",
      "4-6 months (Semester 1)",
      "7-9 months (Semester 2)",
      "10-12 months (Semester 2)",
      "13-16 months (Semester 3)",
      "16+ months",
      "Native speaker");
  private final LangTestDatabaseAsync service;
  private final UserNotification langTest;
  private final boolean useCookie = false;
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
    //Thread.dumpStack();
    if (user != NO_USER_SET) {
      System.out.println("login user : " +user);
      langTest.gotUser(user);
    } else {
      displayLoginBox();
    }
  }

  void logout() {
    Cookies.setCookie("grader","");

    System.out.println("logout : grader now " + getGrader());
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

  /**
   * We don't use this anymore
   * @deprecated
   */
  private void displayChoiceBox() {

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
        //langTest.setGrading(false);

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
        graderLogin();
      }
    });

    dialogBox.setWidget(dialogVPanel);
    show(dialogBox);
  }

  public void graderLogin() {
    String sid = getGrader();
    if (sid == null || sid.length() == 0) {
      displayGraderLogin();
    }
    else {
      System.out.println("grader cookie " + getGrader());
      langTest.setGrading(true);
    }
  }

  private void displayGraderLogin() {
    final DialogBox dialogBox = new DialogBox();
    dialogBox.setText("Grader Login");
    dialogBox.setAnimationEnabled(true);

    // Enable glass background.
    dialogBox.setGlassEnabled(true);

    final TextBox user = new TextBox();
    final TextBox password = new PasswordTextBox();

    final Button closeButton = new Button("Login");
    final Button reg = new Button("Register");


    closeButton.setEnabled(false);

    // We can set the id of a widget by accessing its Element
    closeButton.getElement().setId("closeButton");


    KeyUpHandler keyHandler = new MyKeyUpHandler(user, closeButton, reg, password);
    user.addKeyUpHandler(keyHandler);
    password.addKeyUpHandler(keyHandler);



    VerticalPanel dialogVPanel = new VerticalPanel();
    dialogVPanel.addStyleName("dialogVPanel");
    dialogVPanel.add(new HTML("<b>User ID</b>"));
    dialogVPanel.add(user);
    dialogVPanel.add(new HTML("<b>Password</b>"));
    dialogVPanel.add(password);
    dialogVPanel.add(new HTML("<i>(New users : choose an id and click register.)</i>"));

    reg.setEnabled(false);
    reg.getElement().setId("registerButton");

    reg.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        if (user.getText().length() > 0 && password.getText().length() > 0) {
          service.graderExists(user.getText(), new AsyncCallback<Boolean>() {
            public void onFailure(Throwable caught) {}

            public void onSuccess(Boolean result) {
              if (result) {
                closeButton.setEnabled(false);
              } else {
                service.addGrader(user.getText(), new AsyncCallback<Void>() {
                  public void onFailure(Throwable caught) {
                  }

                  public void onSuccess(Void result) {
                    setGraderCookie(user.getText());
                    boolean passwordMatch = checkPassword(password);
                    closeButton.setEnabled(passwordMatch);
                    closeButton.click();
                  }
                });
              }
            }
          });
        }
      }
    });
    //w1.addStyleName("paddedHorizontalPanel");

    dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
    HorizontalPanel hp = new HorizontalPanel();
    hp.add(reg);

    SimplePanel spacer = new SimplePanel();
    spacer.setSize("20px", "20px");

    hp.add(spacer);
    hp.add(closeButton);

    dialogVPanel.add(hp);
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

  private boolean checkPassword(TextBox password) {
    return password.getText().trim().equalsIgnoreCase(GRADING);
  }

  private void setGraderCookie(String user) {
    System.out.println("added " + user);
    final long DURATION = 1000 * 60 * 60 * EXPIRATION_HOURS; //duration remembering login
    Date expires = new Date(System.currentTimeMillis() + DURATION);
    Cookies.setCookie("grader", "" + user, expires);
  }

  /**
   * @see #login()
   */
  private void displayLoginBox() {
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
    List<String> choices = EXPERIENCE_CHOICES;
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
      public void onKeyUp(KeyUpEvent event) {     // never called...
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
    closeButton.addKeyUpHandler(handler);

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
          closeButton.setEnabled ((age > MIN_AGE && age < MAX_AGE) || age == TEST_AGE);
        } catch (NumberFormatException e) {
          closeButton.setEnabled(false);
        }
      }
    });
    return closeButton;
  }

  private class MyKeyUpHandler implements KeyUpHandler {
    private final TextBox user;
    private final Button closeButton, regButton;
    private final TextBox password;

    public MyKeyUpHandler(TextBox user, Button closeButton, Button regButton, TextBox password) {
      this.user = user;
      this.closeButton = closeButton;
      this.password = password;
      this.regButton = regButton;
    }

    public void onKeyUp(KeyUpEvent event) {
      String text = user.getText();
      if (text.length() == 0) {
        closeButton.setEnabled(false);
        return;
      }

      service.graderExists(text, new AsyncCallback<Boolean>() {
        public void onFailure(Throwable caught) {}
        public void onSuccess(Boolean result) {
          //System.out.println("user '" + user.getText() + "' exists " + result);
          boolean passwordMatch = checkPassword(password);

          if (passwordMatch) {
            closeButton.setEnabled(result);
            regButton.setEnabled(!result);
          } else {
            closeButton.setEnabled(false);
            regButton.setEnabled(false);
          }
        }
      });
    }
  }
}
