package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.ControlLabel;
import com.github.gwtbootstrap.client.ui.Controls;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.Modal;
import com.github.gwtbootstrap.client.ui.Popover;
import com.github.gwtbootstrap.client.ui.RadioButton;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.ControlGroupType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.github.gwtbootstrap.client.ui.constants.Trigger;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
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
  public static final long HOUR_IN_MILLIS = 1000 * 60 * 60;

  private static final int WEEK_HOURS = 24 * 7;
  private static final int DAY_HOURS = 24;
  private static final int ONE_YEAR = 24 * 365;
  private static final int ONE_MONTH_HOURS = 24 * 30;

  private static final int EXPIRATION_HOURS = WEEK_HOURS;
  private static final int SHORT_EXPIRATION_HOURS = DAY_HOURS;
  private static final int FOREVER_HOURS = ONE_YEAR;

  private static final int MIN_AGE = 12;
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
  private static final String LOGIN_TYPE = "loginType";
  public static final int NATIVE_MONTHS = 20 * 12;
  private final LangTestDatabaseAsync service;
  private final UserNotification langTest;
  private final boolean useCookie = false;
  private long userID = NO_USER_SET;
  private String userChosenID = "";
  private boolean isCollectAudio;
  private final boolean isDataCollectAdmin;
  private PropertyHandler.LOGIN_TYPE loginType;
  private final boolean isFlashcard;
  private String appTitle;
  private DisclosurePanel dp;

  /**
   * @see mitll.langtest.client.LangTest#onModuleLoad2()
   * @see mitll.langtest.client.LangTest#doFlashcard()
   * @see mitll.langtest.client.LangTest#doDataCollectAdminView
   * @param lt
   * @param service
   * @param isDataCollectAdmin
   * @param loginType
   * @param appTitle
   * @param isFlashcard
   */
  public UserManager(UserNotification lt, LangTestDatabaseAsync service, boolean isCollectAudio,
                     boolean isDataCollectAdmin, PropertyHandler.LOGIN_TYPE loginType, String appTitle, boolean isFlashcard) {
    this.langTest = lt;
    this.service = service;
    this.isCollectAudio = isCollectAudio;
    this.isDataCollectAdmin = isDataCollectAdmin;
    this.isFlashcard = isFlashcard;
    this.loginType = loginType;
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
  private void storeUser(long sessionID, String audioType, String userChosenID, PropertyHandler.LOGIN_TYPE userType) {
    //System.out.println("storeUser : user now " + sessionID);
    final long DURATION = getUserSessionDuration();
    long futureMoment = getUserSessionEnd(DURATION);
    if (useCookie) {
      Date expires = new Date(futureMoment);
      Cookies.setCookie("sid", "" + sessionID, expires);
    } else if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();

      localStorageIfSupported.setItem(getUserIDCookie(), "" + sessionID);
      localStorageIfSupported.setItem(getUserChosenID(), "" + userChosenID);
      rememberUserSessionEnd(localStorageIfSupported, futureMoment);
      localStorageIfSupported.setItem(getAudioType(), "" + audioType);
      localStorageIfSupported.setItem(getLoginType(), "" + userType);
      System.out.println("storeUser : user now " + sessionID + " / " + getUser() + " expires in " + (DURATION/1000) + " seconds");
    } else {
      userID = sessionID;
      this.userChosenID = userChosenID;
    }

    langTest.gotUser(sessionID);
  }

  private void rememberUserSessionEnd(long futureMoment) {
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
      rememberUserSessionEnd(localStorageIfSupported, futureMoment);
    }
  }

  private void rememberUserSessionEnd(Storage localStorageIfSupported, long futureMoment) {
    localStorageIfSupported.setItem(getExpires(), "" + futureMoment);
  }

  private long getUserSessionEnd() {
    return getUserSessionEnd(getUserSessionDuration());
  }

  private long getUserSessionEnd(long DURATION) {
    return System.currentTimeMillis() + DURATION;
  }

  /**
   * If we have lots of students moving through stations quickly, we want to auto logout once a day, once an hour?
   * TODO : add another parameter for default session length
   * @return
   */
  private long getUserSessionDuration() {
    boolean useShortExpiration = loginType.equals(PropertyHandler.LOGIN_TYPE.STUDENT);
    return HOUR_IN_MILLIS * (
      isFlashcard ? FOREVER_HOURS :
      (useShortExpiration ? SHORT_EXPIRATION_HOURS : EXPIRATION_HOURS));
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
      System.out.println("UserManager.login : adding anonymous user");
      addAnonymousUser();
    }
    else {
      displayLoginBox();
    }
  }

  /**
   * @see mitll.langtest.client.LangTest#checkLogin
   */
  public void anonymousLogin() {
    int user = getUser();
    if (user != NO_USER_SET) {
      System.out.println("UserManager.anonymousLogin : user : " + user);
      rememberAudioType();
      langTest.gotUser(user);
    }
    else {
      addAnonymousUser();
    }
  }

  private void addAnonymousUser() {
    addUser(89,"male",0);
  }

  /**
   * For display purposes
   * @return
   */
  public String getUserID() {
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();

      return localStorageIfSupported.getItem(getUserChosenID());
    }
    else {
      return userChosenID;
    }
  }

  private void rememberAudioType() {
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();

      String audioType = localStorageIfSupported.getItem(getAudioType());
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
    else if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
      String sid = localStorageIfSupported.getItem(getUserIDCookie());
      System.out.println("user id cookie for " +getUserIDCookie() + " is " + sid);
      if (sid != null && !sid.equals("" + NO_USER_SET)) {
        if (userExpired(sid)) {
          clearUser();
        } else if (getLoginTypeFromStorage() != loginType) {
          System.out.println("current login type : " + getLoginTypeFromStorage() + " vs mode " + loginType);
          clearUser();
        }

        sid = localStorageIfSupported.getItem(getUserIDCookie());
      }
      return (sid == null || sid.equals("" + NO_USER_SET)) ? NO_USER_SET : Integer.parseInt(sid);
    }
    else {
      return (int) userID;
    }
  }

  /**
   * Need these to be prefixed by app title so if we switch webapps, we don't get weird user ids
   * @return
   */
  private String getUserIDCookie() {
    return appTitle + ":"+ USER_ID;
  }
  private String getUserChosenID() {
    return appTitle + ":"+ USER_CHOSEN_ID;
  }
  private String getAudioType() {
    return appTitle + ":"+ AUDIO_TYPE;
  }
  private String getLoginType() {
    return appTitle + ":"+ LOGIN_TYPE;
  }
  private String getExpires() {
    return appTitle + ":"+ "expires";
  }

  /**
   * @see #getUser()
   * @param sid
   */
  private boolean userExpired(String sid) {
    String expires = getExpiresCookie();
    if (expires == null) {
      System.out.println("checkExpiration : no expires item?");
    }
    else {
      try {
        long expirationDate = Long.parseLong(expires);

        long farthestPossibleTime = getUserSessionEnd();
        if (farthestPossibleTime < expirationDate) {  // OR log them out?
          // we switched user modes...
          rememberUserSessionEnd(farthestPossibleTime);
          expirationDate = Long.parseLong(getExpiresCookie());
        }

        if (expirationDate < System.currentTimeMillis()) {
          System.out.println("checkExpiration : " + sid + " has expired.");
          return true;
        }
        else {
          System.out.println("checkExpiration : " +sid + " has expires on " + new Date(expirationDate) + " vs now " + new Date());
        }
      } catch (NumberFormatException e) {
        e.printStackTrace();
      }
    }
    return false;
  }

  private String getExpiresCookie() {
    Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
    return localStorageIfSupported.getItem(getExpires());
  }

  private PropertyHandler.LOGIN_TYPE getLoginTypeFromStorage() {
    Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
    String item = localStorageIfSupported.getItem(getLoginType());
    try {
      if (item == null) {
        return PropertyHandler.LOGIN_TYPE.UNDEFINED;
      }
      else {
        return PropertyHandler.LOGIN_TYPE.valueOf(item.toUpperCase());
      }
    } catch (IllegalArgumentException e) {
      System.err.println("couldn't parse " + item);
      return PropertyHandler.LOGIN_TYPE.UNDEFINED;
    }
  }

  /**
   * @see mitll.langtest.client.LangTest#getLogout()
   */
  public void clearUser() {
    langTest.rememberAudioType(Result.AUDIO_TYPE_UNSET);
    if (useCookie) {
      Cookies.setCookie("sid", "" + NO_USER_SET);
    } else if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();

      localStorageIfSupported.removeItem(getUserIDCookie());
      localStorageIfSupported.removeItem(getUserChosenID());
      System.out.println("clearUser : removed item " + getUserID() + " user now " + getUser());
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
      System.out.println("teacherLogin: login user : " + user);
      rememberAudioType();
      langTest.gotUser(user);
    } else {
      displayTeacherLogin();
    }
  }

  private void displayTeacherLogin() {
   // final DialogBox dialogBox = new DialogBox();
    final Modal dialogBox = new Modal();
    //   dialogBox.setText("Data Collector Login");
       dialogBox.setTitle("Data Collector Login");
   // dialogBox.setAnimationEnabled(true);

    // Enable glass background.
  //  dialogBox.setGlassEnabled(true);

    final TextBox user = new TextBox();
/*    user.addKeyDownHandler(new KeyDownHandler() {
      @Override
      public void onKeyDown(KeyDownEvent event) {
        System.out.println ("key = "+event.getNativeKeyCode());
      }
    });*/
    final PasswordTextBox password = new PasswordTextBox();
    final RadioButton regular = new RadioButton("AudioType","Regular Audio Recording");
    final RadioButton fastThenSlow = new RadioButton("AudioType","Record Regular Speed then Slow");
    Controls controls = new Controls();
    controls.add(regular);
    controls.add(fastThenSlow);
    final TextBox nativeLang = new TextBox();
    final TextBox dialect = new TextBox();
    final TextBox ageEntryBox = new TextBox();

    final Button login = new Button("Login");

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
    dp = new DisclosurePanel("Registration");
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
    dialogBox.add(dialogVPanel);

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
                showPopup("Please choose either regular or regular then slow audio recording.");
              } else {
                dialogBox.hide();
                String audioType = fastThenSlow.getValue() ? Result.AUDIO_TYPE_FAST_AND_SLOW : Result.AUDIO_TYPE_REGULAR;
                storeAudioType(audioType);
                storeUser(result, audioType, user.getText(), PropertyHandler.LOGIN_TYPE.DATA_COLLECTOR);
              }
            } else {
              System.out.println(user.getText() + " doesn't exist");
              if (user.getText().length() == 0) {
                showPopup("Please enter a user id.");
              } else {
                if (checkPassword(password)) {
                  doRegistration(user, password, regular, fastThenSlow, nativeLang, dialect, ageEntryBox,
                    experienceBox, genderBox, /*first, last,*/ dialogBox, login);
                } else {
                  showPopup("Please use password from the email");
                }
              }
            }
          }
        });
      }
    });
    dialogBox.show();
  }

  /**
   * @see #displayTeacherLogin()
   * @param user
   * @param password
   * @param regular
   * @param fastThenSlow
   * @param nativeLang
   * @param dialect
   * @param ageEntryBox
   * @param experienceBox
   * @param genderBox
   * @param dialogBox
   * @param login
   */
  private void doRegistration(TextBox user, PasswordTextBox password, RadioButton regular,
                              RadioButton fastThenSlow,
                              TextBox nativeLang, TextBox dialect, TextBox ageEntryBox,
                              ListBox experienceBox, ListBox genderBox,
                              Modal dialogBox,
                              Button login) {

    boolean valid = user.getText().length() > 0;
    if (!valid) {
      showPopup("Please enter a userid.");
    } else {
      valid = password.getText().length() > 0;
      if (!valid) {
        showPopup("Please enter a password.");
      }
    }
    if (valid) {
      valid = checkPassword(password);
      if (!valid) {
        showPopup("Please use password from the email sent to you.");
        valid = false;
      }
      else if (!isDataCollectAdmin && checkAudioSelection(regular, fastThenSlow)) {
        showPopup("Please choose either regular or regular then slow audio recording.");
        valid = false;
      } else if (!isDataCollectAdmin && nativeLang.getText().isEmpty()) {
        if (!dp.isOpen()) {
          dp.setOpen(true);
        } else {
          showPopup("Language is empty");
        }
        valid = false;
      }
      else if (!isDataCollectAdmin && dialect.getText().isEmpty()) {
        showPopup("Dialect is empty");
        valid = false;
      }

      if (valid) {
        try {
          int age = getAge(ageEntryBox);
          if (!isDataCollectAdmin && (age < MIN_AGE) || (age > MAX_AGE && age != TEST_AGE)) {
            valid = false;
            showPopup("age '" + age + "' is too young or old.");
          }
        } catch (NumberFormatException e) {
          showPopup("age '" + ageEntryBox.getText() + "' is invalid.");
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
    int i = 0;
    try {
      i = isDataCollectAdmin ? 89 : Integer.parseInt(ageEntryBox.getText());
    } catch (NumberFormatException e) {
      System.out.println("couldn't parse " + ageEntryBox.getText());
    }
    return i;
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
   * @param nativeLang
   * @param dialect
   * @param dialogBox
   * @param closeButton
   * @param isFastAndSlow
   */
  private void checkUserOrCreate(final int enteredAge, final TextBox user, final ListBox experienceBox,
                                 final ListBox genderBox,
                                 final TextBox nativeLang, final TextBox dialect,
                                 final Modal dialogBox,
                                 final Button closeButton,
                                 final boolean isFastAndSlow) {
    service.userExists(user.getText(), new AsyncCallback<Integer>() {
      public void onFailure(Throwable caught) {
        Window.alert("Couldn't contact server.");
      }

      public void onSuccess(Integer result) {
        System.out.println("user '" + user.getText() + "' exists " + result);
        if (result == -1) {
          addTeacher(enteredAge,
            experienceBox, genderBox, nativeLang, dialect, user, dialogBox, closeButton, isFastAndSlow);
        } else {
          showPopup("User " + user.getText() + " already registered, click login.");
        }
      }
    });
  }

  /**
   * @see #checkUserOrCreate
   * @param age
   * @param experienceBox
   * @param genderBox
   * @param nativeLang
   * @param dialect
   * @param user
   * @param dialogBox
   * @param closeButton
   * @param isFastAndSlow
   */
  private void addTeacher(int age, ListBox experienceBox, ListBox genderBox,
                          TextBox nativeLang,
                          TextBox dialect, final TextBox user,
                          final Modal dialogBox,
                          final Button closeButton,
                          final boolean isFastAndSlow) {
    int monthsOfExperience = experienceBox.getSelectedIndex() * 3;
    if (experienceBox.getSelectedIndex() == EXPERIENCE_CHOICES.size() - 1) {
      monthsOfExperience = NATIVE_MONTHS;
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
          Window.alert("addUser : Can't contact server.");
          closeButton.setFocus(true);
        }

        public void onSuccess(Long result) {
          System.out.println("addUser : server result is " + result);
          dialogBox.hide();
          String audioType = isFastAndSlow ? Result.AUDIO_TYPE_FAST_AND_SLOW : Result.AUDIO_TYPE_REGULAR;
          storeAudioType(audioType);
          storeUser(result, audioType, user.getText(), PropertyHandler.LOGIN_TYPE.DATA_COLLECTOR);
        }
      });
  }

  private boolean checkPassword(PasswordTextBox password) {
    String trim = password.getText().trim();
    return trim.equalsIgnoreCase(GRADING) || trim.equalsIgnoreCase(TESTING);
  }

  /**
   * @see #login()
   */
  private void displayLoginBox() {
    // Create the popup dialog box
     final Modal dialogBox = new Modal();
     dialogBox.setCloseVisible(false);
     dialogBox.setTitle("Login Questions");

    final TextBox ageEntryBox = new TextBox();
    final ControlGroup ageGroup = new ControlGroup();
    ageGroup.add(new ControlLabel("Please enter your age"));
    ageGroup.add(ageEntryBox);
    final Button closeButton = makeCloseButton(ageEntryBox, ageGroup);

    // Add a drop box with the list types
    final ListBox genderBox = getGenderBox();

    // add experience drop box
    final ListBox experienceBox = getExperienceBox();
    final TextBox dialect = new TextBox();
    dialogBox.add(ageGroup);

    ControlGroup genderGroup = new ControlGroup();
    genderGroup.add(new ControlLabel("Select gender"));
    genderGroup.add(genderBox);
    dialogBox.add(genderGroup);

    ControlGroup expGroup = new ControlGroup();
    expGroup.add(new ControlLabel("Select months of experience"));
    expGroup.add(experienceBox);
    dialogBox.add(expGroup);

    final ControlGroup dialectGroup = new ControlGroup();
    dialectGroup.add(new ControlLabel("Enter dialect"));
    dialectGroup.add(dialect);
    dialect.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        if (dialect.getText().length() > 0) {
          dialectGroup.setType(ControlGroupType.NONE);
        }
      }
    });
    dialogBox.add(dialectGroup);
    dialogBox.add(closeButton);

    closeButton.setFocus(true);

    // Create a handler for the sendButton and nameField
    class MyHandler implements ClickHandler {
      /**
       * Do validation.
       * Fired when the user clicks on the sendButton.
       */
      public void onClick(ClickEvent event) {
        if (highlightAgeBox(ageEntryBox, ageGroup)) {
          if (dialect.getText().isEmpty()) {
            dialectGroup.setType(ControlGroupType.ERROR);
            setupPopover(dialect,"Please enter a language dialect.","Try again");
            dialect.setFocus(true);
          } else {
            dialogBox.hide();
            sendNameToServer();
          }
        }
        else {
          setupPopover(ageEntryBox, "Please enter age between " + MIN_AGE + " and " + MAX_AGE+".","Try again");
          ageEntryBox.setFocus(true);
        }
      }

      /**
       * Send the name from the nameField to the server and wait for a response.
       */
      private void sendNameToServer() {
        int monthsOfExperience = experienceBox.getSelectedIndex() * 3;
        if (experienceBox.getSelectedIndex() == EXPERIENCE_CHOICES.size() - 1) {
          monthsOfExperience = NATIVE_MONTHS;
        }
        addUser(monthsOfExperience, ageEntryBox, genderBox, dialect);
      }
    }

    // Add a handler to send the name to the server
    MyHandler handler = new MyHandler();
    closeButton.addClickHandler(handler);

    dialogBox.show();
  }

  private void setupPopover(final Widget w, String message, String heading) {
    final Popover popover = new Popover();
    popover.setWidget(w);
    popover.setText(message);
    popover.setHeading(heading);
    popover.setPlacement(Placement.RIGHT);
    popover.setHideDelay(3000);
    popover.setTrigger(Trigger.MANUAL);
    popover.reconfigure();
    popover.show();

    Timer t = new Timer() {
      @Override
      public void run() {
        popover.hide();
        popover.clear();
      }
    };
    t.schedule(3000);
  }

  /**
   * @see #displayLoginBox()
   * @param monthsOfExperience
   * @param ageEntryBox
   * @param genderBox
   * @param dialectBox
   * @paramx dialogBox
   * @paramxx closeButton
   */
  private void addUser(int monthsOfExperience, TextBox ageEntryBox, ListBox genderBox, TextBox dialectBox) {
    int age = getAge(ageEntryBox);
    String gender = genderBox.getValue(genderBox.getSelectedIndex());
    addUser(age, gender, monthsOfExperience, dialectBox.getText(),
      PropertyHandler.LOGIN_TYPE.STUDENT);
  }

  /**
   * @see #addAnonymousUser()
   * @param age
   * @param gender
   * @param monthsOfExperience
   */
  private void addUser(int age, String gender, int monthsOfExperience) {
    addUser(age, gender, monthsOfExperience, "",
      PropertyHandler.LOGIN_TYPE.ANONYMOUS);
  }

  /**
   * @see #addUser(int, String, int)
   * @param age
   * @param gender
   * @param monthsOfExperience
   * @param dialect
   * @param loginType
   */
  private void addUser(int age, String gender, int monthsOfExperience, String dialect,
                       final PropertyHandler.LOGIN_TYPE loginType) {
    service.addUser(age,
      gender,
      monthsOfExperience, dialect, new AsyncCallback<Long>() {
      public void onFailure(Throwable caught) {
        Window.alert("addUser : Couldn't contact server.");
      }

      public void onSuccess(Long result) {
        System.out.println("addUser : server result is " + result);
        storeUser(result, "", "" + result, loginType);
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

  private Button makeCloseButton(final TextBox ageEntryBox, final ControlGroup group) {
    final Button closeButton = new Button("Login");
    closeButton.setType(ButtonType.PRIMARY);

    // We can set the id of a widget by accessing its Element
    closeButton.getElement().setId("closeButton");
    ageEntryBox.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        highlightAgeBox(ageEntryBox, group);
      }
    });
    return closeButton;
  }

  private boolean highlightAgeBox(TextBox ageEntryBox, ControlGroup group) {
    String text = ageEntryBox.getText();
    boolean validAge = false;
    if (text.length() == 0) {
      group.setType(ControlGroupType.WARNING);
    } else {
      try {
        int age = Integer.parseInt(text);
        validAge = (age > MIN_AGE && age < MAX_AGE) || age == TEST_AGE;
        group.setType(validAge ? ControlGroupType.NONE : ControlGroupType.ERROR);
      } catch (NumberFormatException e) {
        group.setType(ControlGroupType.ERROR);
      }
    }

    return validAge;
  }

  private void showPopup(String html) {
    final PopupPanel pleaseWait = new DecoratedPopupPanel();
    pleaseWait.setAutoHideEnabled(true);
    pleaseWait.add(new HTML(html));
    pleaseWait.center();

    Timer t = new Timer() {
      @Override
      public void run() {
        pleaseWait.hide();
      }
    };
    t.schedule(3000);
  }
}
