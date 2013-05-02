package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.Controls;
import com.github.gwtbootstrap.client.ui.RadioButton;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.shared.Result;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Handles storing cookies for users, etc. IF user ids are stored as cookies.
 * <p/>
 * Prompts user for login info.
 * <p/>
 * NOTE : will keep prompting them if the browser doesn't let you store cookies.
 * <p/>
 * User: GO22670
 * Date: 5/15/12
 * Time: 11:32 AM
 * To change this template use File | Settings | File Templates.
 */
public class UserManager {
  private static final int EXPIRATION_HOURS = 24*7;
  private static final int SHORT_EXPIRATION_HOURS = 24*1;
  private static final int FOREVER_HOURS = 24*365;

  private static final int MIN_AGE = 6;
  private static final int MAX_AGE = 90;
  private static final int TEST_AGE = 100;
  private static final int NO_USER_SET = -1;
  private static final String GRADING = "grading";
  private static final String TESTING = "testing";
  private static final List<String> EXPERIENCE_CHOICES = Arrays.asList(
      "0-3 months (Semester 1)",
      "4-6 months (Semester 1)",
      "7-9 months (Semester 2)",
      "10-12 months (Semester 2)",
      "13-16 months (Semester 3)",
      "16+ months",
      "Native speaker");
  private static final String USER_ID = "userID";
  private static final String USER_CHOSEN_ID = "userChosenID";
  private static final String AUDIO_TYPE = "audioType";
  private final LangTestDatabaseAsync service;
  private final UserNotification langTest;
  private final boolean useCookie = false;
  private long userID = NO_USER_SET;
  private String userChosenID = "";
  private boolean isCollectAudio;
  private Storage stockStore = null;
  private final boolean isDataCollectAdmin;
  private final boolean useShortExpiration;
  private static final boolean COLLECT_NAMES = false;
  private final boolean isFlashcard;
  private String appTitle;

  /**
   * @see mitll.langtest.client.LangTest#onModuleLoad2()
   * @param lt
   * @param service
   * @param isDataCollectAdmin
   * @param useShortExpiration
   * @param appTitle
   * @param isFlashcard
   */
  public UserManager(UserNotification lt, LangTestDatabaseAsync service, boolean isCollectAudio,
                     boolean isDataCollectAdmin, boolean useShortExpiration, String appTitle, boolean isFlashcard) {
    this.langTest = lt;
    this.service = service;
    this.isCollectAudio = isCollectAudio;
    stockStore = Storage.getLocalStorageIfSupported();
    this.isDataCollectAdmin = isDataCollectAdmin;
    this.isFlashcard = isFlashcard;
    this.useShortExpiration = useShortExpiration;
    this.appTitle = appTitle;
  }

  // user tracking

  /**
   *
   *
   * @param sessionID from database
   * @param audioType
   * @param userChosenID
   * @see #displayLoginBox()
   * @see #displayTeacherLogin()
   * @see #addTeacher
   */
  private void storeUser(long sessionID, String audioType, String userChosenID) {
    //System.out.println("storeUser : user now " + sessionID);
    final long DURATION = 1000 * 60 * 60 * (
      isFlashcard ? FOREVER_HOURS : (useShortExpiration ? SHORT_EXPIRATION_HOURS : EXPIRATION_HOURS)); //duration remembering login
    long now = System.currentTimeMillis();
    long futureMoment = now + DURATION;
    if (useCookie) {
      Date expires = new Date(futureMoment);
      Cookies.setCookie("sid", "" + sessionID, expires);
    } else if (stockStore != null) {
      stockStore.setItem(getUserIDCookie(), "" + sessionID);
      stockStore.setItem(getUserChosenID(), "" + userChosenID);
      stockStore.setItem(getExpires(), "" + futureMoment);
      stockStore.setItem(getAudioType(), "" + audioType);
      System.out.println("storeUser : user now " + sessionID + " / " + getUser() + " expires in " + (DURATION/1000) + " seconds");
    } else {
      userID = sessionID;
      this.userChosenID = userChosenID;
    }

    langTest.gotUser(sessionID);
  }

  private void storeAudioType(String type) {
    if (isCollectAudio) {
      langTest.rememberAudioType(type);
    }
  }

  /**
   * @see mitll.langtest.client.LangTest#login()
   */
  public void login() {
    int user = getUser();
    if (user != NO_USER_SET) {
      System.out.println("UserManager.login : user : " + user);
      rememberAudioType();
      langTest.gotUser(user);
    }
    else if (isFlashcard) {
      addUser(89,"male",0);
    }
    else {
      displayLoginBox();
    }
  }

  /**
   * For display purposes
   * @return
   */
  public String getUserID() {
    if (stockStore != null) {
      return stockStore.getItem(getUserChosenID());
    }
    else {
      return userChosenID;
    }
  }

  private void rememberAudioType() {
    if (stockStore != null) {
      String audioType = stockStore.getItem(getAudioType());
      if (audioType == null) {
        audioType = Result.AUDIO_TYPE_FAST_AND_SLOW;
      }
      langTest.rememberAudioType(audioType);
    }
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
    }
    else if (stockStore != null) {
      String sid = stockStore.getItem(getUserIDCookie());
      System.out.println("user id cookie for " +getUserIDCookie() + " is " + sid);
      if (sid != null && !sid.equals("" + NO_USER_SET)) {
        checkExpiration(sid);
        sid = stockStore.getItem(getUserIDCookie());
      }
      return (sid == null || sid.equals("" + NO_USER_SET)) ? NO_USER_SET : Integer.parseInt(sid);
    }
    else {
      return (int) userID;
    }
  }

  private String getUserIDCookie() {
    return appTitle + ":"+ USER_ID;
  }
  private String getUserChosenID() {
    return appTitle + ":"+ USER_CHOSEN_ID;
  }
  private String getAudioType() {
    return appTitle + ":"+ AUDIO_TYPE;
  }
  private String getExpires() {
    return appTitle + ":"+ "expires";
  }

  private void checkExpiration(String sid) {
    String expires = stockStore.getItem(getExpires());
    if (expires == null) {
      System.out.println("checkExpiration : no expires item?");
    }
    else {
      try {
        long expirationDate = Long.parseLong(expires);
        if (expirationDate < System.currentTimeMillis()) {
          System.out.println("checkExpiration : " +sid + " has expired.");
          clearUser();
        }
        else {
          System.out.println("checkExpiration : " +sid + " has expires on " + new Date(expirationDate) + " vs now " + new Date());
        }
      } catch (NumberFormatException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * @see mitll.langtest.client.LangTest#getLogout()
   */
  public void clearUser() {
    langTest.rememberAudioType(Result.AUDIO_TYPE_UNSET);
    if (useCookie) {
      Cookies.setCookie("sid", "" + NO_USER_SET);
    } else if (stockStore != null) {
      stockStore.removeItem(getUserIDCookie());
      stockStore.removeItem(getUserChosenID());
      System.out.println("clearUser : removed item " + getUserID() +
        " user now " + getUser());
    } else {
      userID = NO_USER_SET;
    }
  }

  /**
   * @see mitll.langtest.client.LangTest#doDataCollectAdminView
   */
  public void teacherLogin() {
    int user = getUser();
    if (user != NO_USER_SET) {
      System.out.println("login user : " + user);
      rememberAudioType();
      langTest.gotUser(user);
    } else {
      displayTeacherLogin();
    }
  }

  private void displayTeacherLogin() {
    final DialogBox dialogBox = new DialogBox();
    dialogBox.setText("Data Collector Login");
   // dialogBox.setAnimationEnabled(true);

    // Enable glass background.
    dialogBox.setGlassEnabled(true);

    final TextBox user = new TextBox();
/*    user.addKeyDownHandler(new KeyDownHandler() {
      @Override
      public void onKeyDown(KeyDownEvent event) {
        System.out.println ("key = "+event.getNativeKeyCode());
      }
    });*/
    final TextBox password = new PasswordTextBox();
    final RadioButton regular = new RadioButton("AudioType","Regular Audio Recording");
    final RadioButton fastThenSlow = new RadioButton("AudioType","Record Regular Speed then Slow");
   // ControlGroup cg = new ControlGroup();
    Controls controls = new Controls();
    controls.add(regular);
    controls.add(fastThenSlow);
   // final TextBox first = new TextBox();
 //   final TextBox last = new TextBox();
    final TextBox nativeLang = new TextBox();
    final TextBox dialect = new TextBox();
    final TextBox ageEntryBox = new TextBox();

    final Button login = new Button("Login");
   // final Button reg = new Button("Register");

    final ListBox genderBox = getGenderBox();
    VerticalPanel genderPanel = getGenderPanel(genderBox);

    final ListBox experienceBox = getExperienceBox();
    VerticalPanel experiencePanel = getGenderPanel(experienceBox);

    login.setEnabled(true);

    // We can set the id of a widget by accessing its Element
    login.getElement().setId("login");

    VerticalPanel dialogVPanel = new VerticalPanel();
    dialogVPanel.addStyleName("dialogVPanel");
    dialogVPanel.add(new HTML("<b>User ID</b>"));
    dialogVPanel.add(user);

    HTML w = new HTML("<b>Password</b>");
    w.setTitle("See email for your password.");
    dialogVPanel.add(w);
    dialogVPanel.add(password);

    HTML ww = new HTML("<b>Audio Recording Style</b>");
    w.setTitle("Choose type of audio recording.");
    if (isCollectAudio) {
      dialogVPanel.add(ww);
      dialogVPanel.add(controls);
/*      dialogVPanel.add(regular);
      dialogVPanel.add(fastThenSlow);*/
    }

    SimplePanel spacer = new SimplePanel();
    spacer.setSize("20px", "5px");
    dialogVPanel.add(spacer);
    dialogVPanel.add(new HTML("<i>New users : click on Registration below and fill in the fields.</i>"));
    SimplePanel spacer2 = new SimplePanel();
    spacer2.setSize("20px", "5px");
    dialogVPanel.add(spacer2);

    VerticalPanel register = new VerticalPanel();
    DisclosurePanel dp = new DisclosurePanel("Registration");
    dp.setContent(register);
    dp.setAnimationEnabled(true);

    dialogVPanel.add(dp);

/*
    if (COLLECT_NAMES) {
      register.add(new HTML("<b>First Name</b>"));
      register.add(first);
      register.add(new HTML("<b>Last Name</b>"));
      register.add(last);
    }
*/

    if (!isDataCollectAdmin) {
      register.add(new HTML("<b>Native Lang (L1)</b>"));
      register.add(nativeLang);
      register.add(new HTML("<b>Dialect</b>"));
      register.add(dialect);
      register.add(new HTML("<b>Your age</b>"));
      register.add(ageEntryBox);
      register.add(new HTML("<b>Select gender</b>"));
      register.add(genderPanel);
      register.add(new HTML("<b>Select months of experience</b>"));
      register.add(new HTML("<b>in this language</b>"));
      register.add(experiencePanel);
    }

    FlowPanel hp = new FlowPanel();
    hp.getElement().getStyle().setFloat(Style.Float.RIGHT);
    hp.add(login);

    dialogVPanel.add(hp);
    dialogBox.setWidget(dialogVPanel);

    login.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        // System.out.println("login button got click " + event);

        service.userExists(user.getText(), new AsyncCallback<Integer>() {
          public void onFailure(Throwable caught) {
            Window.alert("userExists : Couldn't contact server");
          }

          public void onSuccess(Integer result) {
            boolean exists = result != -1;
            if (exists) {
              if (checkAudioSelection(regular, fastThenSlow)) {
                Window.alert("Please choose either regular or regular then slow audio recording.");
              } else {
                dialogBox.hide();
                String audioType = fastThenSlow.getValue() ? Result.AUDIO_TYPE_FAST_AND_SLOW : Result.AUDIO_TYPE_REGULAR;
                storeAudioType(audioType);
                storeUser(result, audioType, user.getText());
              }
            } else {
              System.out.println(user.getText() + " doesn't exist");
              if (user.getText().length() == 0) {
                Window.alert("Please enter a user id.");
              } else {
                if (checkPassword(password)) {
                  doRegistration(user, password, regular, fastThenSlow, nativeLang, dialect, ageEntryBox, experienceBox, genderBox, /*first, last,*/ dialogBox, login);
                } else {
                  Window.alert("Please use password from the email");
                }
              }
            }
          }
        });
      }
    });
    show(dialogBox);
  }

  private void doRegistration(TextBox user, TextBox password, RadioButton regular,
                              RadioButton fastThenSlow,
                              TextBox nativeLang, TextBox dialect, TextBox ageEntryBox,
                              ListBox experienceBox, ListBox genderBox,
                              //TextBox first, TextBox last,
                              DialogBox dialogBox, Button login) {
    boolean valid = user.getText().length() > 0;
    if (!valid) {
      Window.alert("Please enter a userid.");
    } else {
      valid = password.getText().length() > 0;
      if (!valid) {
        Window.alert("Please enter a password.");
      }
    }
    if (valid) {
      valid = checkPassword(password);
      if (!valid) {
        Window.alert("Please use password from the email sent to you.");
        valid = false;
      }
      else if (!isDataCollectAdmin && checkAudioSelection(regular, fastThenSlow)) {
        Window.alert("Please choose either regular or regular then slow audio recording.");
        valid = false;
      }
      else if (!isDataCollectAdmin && nativeLang.getText().isEmpty()) {
        Window.alert("Language is empty");
        valid = false;
      }
      else if (!isDataCollectAdmin && dialect.getText().isEmpty()) {
        Window.alert("Dialect is empty");
        valid = false;
      }

      if (valid) {
        try {
          int age = getAge(ageEntryBox);
          if (!isDataCollectAdmin && (age < MIN_AGE) || (age > MAX_AGE && age != TEST_AGE)) {
            valid = false;
            Window.alert("age '" + age + "' is too young or old.");
          }
        } catch (NumberFormatException e) {
          Window.alert("age '" + ageEntryBox.getText() + "' is invalid.");
          valid = false;
        }
      }
      if (valid) {
        int enteredAge = getAge(ageEntryBox);
        checkUserOrCreate(enteredAge, user, experienceBox, genderBox, nativeLang, dialect, dialogBox,
            login, fastThenSlow.getValue());
      } else {
        System.out.println("not valid ------------ ?");
      }
    }
  }

  private int getAge(TextBox ageEntryBox) {
    return isDataCollectAdmin ? 89: Integer.parseInt(ageEntryBox.getText());
  }

  private boolean checkAudioSelection(RadioButton regular, RadioButton fastThenSlow) {
    return isCollectAudio && !regular.getValue() && !fastThenSlow.getValue();
  }

  /**
   * @see #displayTeacherLogin()
   * @param enteredAge
   * @param user
   * @param experienceBox
   * @param genderBox
   * @paramx first
   * @paramx last
   * @param nativeLang
   * @param dialect
   * @param dialogBox
   * @param closeButton
   * @param isFastAndSlow
   */
  private void checkUserOrCreate(final int enteredAge, final TextBox user, final ListBox experienceBox,
                                 final ListBox genderBox,
                          //       final TextBox first, final TextBox last,
                                 final TextBox nativeLang, final TextBox dialect,
                                 final DialogBox dialogBox, final Button closeButton,
                                 final boolean isFastAndSlow) {
    service.userExists(user.getText(), new AsyncCallback<Integer>() {
      public void onFailure(Throwable caught) {
      }

      public void onSuccess(Integer result) {
        System.out.println("user '" + user.getText() + "' exists " + result);
        if (result == -1) {
          addTeacher(enteredAge,
            experienceBox, genderBox, nativeLang, dialect, user, dialogBox, closeButton, isFastAndSlow);
        } else {
          Window.alert("User " + user.getText() + " already registered, click login.");
        }
      }
    });
  }

  private void addTeacher(int age, ListBox experienceBox, ListBox genderBox,
                          //TextBox first, TextBox last,
                          TextBox nativeLang,
                          TextBox dialect, final TextBox user, final DialogBox dialogBox, final Button closeButton,
                          final boolean isFastAndSlow) {
    int monthsOfExperience = experienceBox.getSelectedIndex() * 3;
    if (experienceBox.getSelectedIndex() == EXPERIENCE_CHOICES.size() - 1) {
      monthsOfExperience = 20 * 12;
    }

    service.addUser(age,
      genderBox.getValue(genderBox.getSelectedIndex()),
      monthsOfExperience,
      "",
     "",
      nativeLang.getText(),
      dialect.getText(),
      user.getText(),

      new AsyncCallback<Long>() {
        public void onFailure(Throwable caught) {
          // Show the RPC error message to the user
          dialogBox.setText("Remote Procedure Call - Failure");
          dialogBox.center();
          closeButton.setFocus(true);
        }

        public void onSuccess(Long result) {
          System.out.println("addUser : server result is " + result);
          dialogBox.hide();
          String audioType = isFastAndSlow ? Result.AUDIO_TYPE_FAST_AND_SLOW : Result.AUDIO_TYPE_REGULAR;
          storeAudioType(audioType);
          storeUser(result, audioType, user.getText());
        }
      });
  }

  private boolean checkPassword(TextBox password) {
    String trim = password.getText().trim();
    return trim.equalsIgnoreCase(GRADING) || trim.equalsIgnoreCase(TESTING);
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
    final ListBox genderBox = getGenderBox();
    VerticalPanel genderPanel = getGenderPanel(genderBox);

    // add experience drop box
    final ListBox experienceBox = getExperienceBox();
    VerticalPanel experiencePanel = getGenderPanel(experienceBox);

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
        int monthsOfExperience = experienceBox.getSelectedIndex() * 3;
        if (experienceBox.getSelectedIndex() == EXPERIENCE_CHOICES.size() - 1) {
          monthsOfExperience = 20 * 12;
        }
        addUser(monthsOfExperience, ageEntryBox, genderBox, dialogBox, closeButton);
      }
    }

    // Add a handler to send the name to the server
    MyHandler handler = new MyHandler();
    closeButton.addClickHandler(handler);
    closeButton.addKeyUpHandler(handler);

    show(dialogBox);
  }

  private void addUser(int monthsOfExperience, TextBox ageEntryBox, ListBox genderBox) {
    addUser(monthsOfExperience, ageEntryBox, genderBox, null, null);
  }

  private void addUser(int monthsOfExperience, TextBox ageEntryBox, ListBox genderBox, final DialogBox dialogBox, final Button closeButton) {
    int age = getAge(ageEntryBox);
    String gender = genderBox.getValue(genderBox.getSelectedIndex());
    addUser(age, gender, monthsOfExperience, dialogBox, closeButton);
  }

  private void addUser(int age, String gender, int monthsOfExperience) {
    addUser(age, gender, monthsOfExperience, null, null);
  }

  private void addUser(int age, String gender, int monthsOfExperience, final DialogBox dialogBox, final Button closeButton) {
    service.addUser(age,
      gender,
      monthsOfExperience, new AsyncCallback<Long>() {
      public void onFailure(Throwable caught) {
        if (dialogBox == null) {
          Window.alert("addUser : Couldn't contact server.");
        } else {
          // Show the RPC error message to the user
          dialogBox.setText("addUser : Couldn't contact server.");
          dialogBox.center();
          closeButton.setFocus(true);
        }
      }

      public void onSuccess(Long result) {
        System.out.println("addUser : server result is " + result);
        storeUser(result, "", ""+result);
      }
    });
  }

  private ListBox getExperienceBox() {
    final ListBox experienceBox = new ListBox(false);
    List<String> choices = EXPERIENCE_CHOICES;
    for (String c : choices) {
      experienceBox.addItem(c);
    }
    experienceBox.ensureDebugId("cwListBox-dropBox");
    return experienceBox;
  }

  private VerticalPanel getGenderPanel(ListBox genderBox) {
    VerticalPanel genderPanel = new VerticalPanel();
    genderPanel.setSpacing(4);
    genderPanel.add(genderBox);
    return genderPanel;
  }

  private ListBox getGenderBox() {
    final ListBox genderBox = new ListBox(false);
    for (String s : Arrays.asList("Male", "Female")) {
      genderBox.addItem(s);
    }
    genderBox.ensureDebugId("cwListBox-dropBox");
    return genderBox;
  }

  private void show(DialogBox dialogBox) {
    int left = Window.getClientWidth() / 10;
    int top = Window.getClientHeight() / 10;
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
          closeButton.setEnabled((age > MIN_AGE && age < MAX_AGE) || age == TEST_AGE);
        } catch (NumberFormatException e) {
          closeButton.setEnabled(false);
        }
      }
    });
    return closeButton;
  }
}
