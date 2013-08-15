package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.ControlLabel;
import com.github.gwtbootstrap.client.ui.Controls;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.Modal;
import com.github.gwtbootstrap.client.ui.PasswordTextBox;
import com.github.gwtbootstrap.client.ui.Popover;
import com.github.gwtbootstrap.client.ui.RadioButton;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.constants.BackdropType;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.ControlGroupType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.github.gwtbootstrap.client.ui.event.ShowEvent;
import com.github.gwtbootstrap.client.ui.event.ShowHandler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
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
  //private static final int ONE_MONTH_HOURS = 24 * 30;

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
  private static final int INACTIVE_PERIOD_MILLIS = 1000 * 60 * 10; // ten minutes
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
  private boolean trackUsers;

  private Timer userTimer;
  private String loginTitle = "Data Collector Login";

  /**
   * @see mitll.langtest.client.LangTest#onModuleLoad2()
   * @see mitll.langtest.client.LangTest#doFlashcard()
   * @see mitll.langtest.client.LangTest#doDataCollectAdminView
   * @param lt
   * @param service
   * @param isDataCollectAdmin
   * @param isFlashcard
   * @param props
   */
  public UserManager(UserNotification lt, LangTestDatabaseAsync service,
                     boolean isDataCollectAdmin, boolean isFlashcard, PropertyHandler props) {
    this.langTest = lt;
    this.service = service;
    this.isCollectAudio = props.isCollectAudio();
    this.isDataCollectAdmin = isDataCollectAdmin;
    this.isFlashcard = isFlashcard;
    this.loginType = props.getLoginType();
    this.appTitle = props.getAppTitle();
    this.trackUsers = props.isTrackUsers();
    if (trackUsers) loginTitle = "Taboo Login";
  }

  // user tracking

  /**
   * TODO : move cookie manipulation to separate class
   *
   * @param sessionID from database
   * @param audioType
   * @param userChosenID
   * @see #addTeacher(int, com.github.gwtbootstrap.client.ui.ListBox, com.github.gwtbootstrap.client.ui.ListBox, com.github.gwtbootstrap.client.ui.TextBox, com.github.gwtbootstrap.client.ui.TextBox, com.github.gwtbootstrap.client.ui.TextBox, com.github.gwtbootstrap.client.ui.Modal, com.github.gwtbootstrap.client.ui.Button, boolean)
   * @see #addUser(int, String, int, String, mitll.langtest.client.PropertyHandler.LOGIN_TYPE)
   * @see #displayTeacherLogin()
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

    if (trackUsers) {
      userAlive();
    }
    langTest.gotUser(sessionID);
  }

  /**
   * Somebody should call this when the user does something in taboo
   */
  public void userAlive() {
    int user = getUser();
    System.out.println(new Date() +" --------> userAlive : " + user);
    userOnline(user, true);
    waitThenInactivate();
  }

  public void userInactive() {
    int user = getUser();
    System.out.println(new Date() +" --------> userInactive : " + user);

    userOnline(user, false);
  }

  private void userOnline(int user, final boolean active) {
    service.userOnline(user, active, new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
        Window.alert("Couldn't contact server, check network connection.");
      }

      @Override
      public void onSuccess(Void result) {
        System.out.println("registered " + getUser() + " as being " + (active ?
          "online." : " offline."));
      }
    });
  }

  private void waitThenInactivate() {
    if (userTimer != null) userTimer.cancel();
    userTimer = new Timer() {
      @Override
      public void run() {
        userInactive();
      }
    };
    userTimer.schedule(INACTIVE_PERIOD_MILLIS);
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

      //System.out.println("user id cookie for " +getUserIDCookie() + " is " + sid);
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
          //System.out.println("checkExpiration : " +sid + " has expires on " + new Date(expirationDate) + " vs now " + new Date());
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
   * @see mitll.langtest.client.LangTest#login
   */
  public void teacherLogin() {
    int user = getUser();
    if (user != NO_USER_SET) {
      System.out.println("teacherLogin: got cached user : " + user);
      rememberAudioType();
      langTest.gotUser(user);
      if (trackUsers) {
        userAlive();
      }
    } else {
      displayTeacherLogin();
    }
  }

  /**
   * Really should be named data collector (audio recorder) login
   */
  private void displayTeacherLogin() {
    final Modal dialogBox = new Modal();
    dialogBox.setCloseVisible(false);
    dialogBox.setKeyboard(false);
    dialogBox.setBackdrop(BackdropType.STATIC);

    dialogBox.setTitle(loginTitle);

    final Button login = new Button("Login");
    login.setType(ButtonType.PRIMARY);
    login.setEnabled(true);
    login.setTitle("Hit enter to log in.");
    // We can set the id of a widget by accessing its Element
    login.getElement().setId("login");
    final FormField user = addControlFormField(dialogBox, "User ID");
    final FormField password = addControlFormField(dialogBox, "Password", true);
    final RadioButton regular = new RadioButton("AudioType","Regular Audio Recording");
    final RadioButton fastThenSlow = new RadioButton("AudioType","Record Regular Speed then Slow");

    final ControlGroup recordingStyle = new ControlGroup();

    regular.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        recordingStyle.setType(ControlGroupType.NONE);   // clear any error markings
      }
    });
    fastThenSlow.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        recordingStyle.setType(ControlGroupType.NONE);
      }
    });

    if (isCollectAudio) {
      recordingStyle.add(new ControlLabel("<b>Audio Recording Style</b>"));
      Controls controls = new Controls();
      controls.add(regular);
      controls.add(fastThenSlow);
      recordingStyle.add(controls);
      dialogBox.add(recordingStyle);
    }

    SimplePanel spacer = new SimplePanel();
    spacer.setSize("20px", "5px");
    dialogBox.add(spacer);
    dialogBox.add(new HTML("<i>New users : click on Registration below and fill in the fields.</i>"));
    SimplePanel spacer2 = new SimplePanel();
    spacer2.setSize("20px", "5px");
    dialogBox.add(spacer2);

    final FormField nativeLangGroup,dialectGroup,ageEntryGroup;
    final ListBoxFormField genderGroup,experienceGroup;
      VerticalPanel register = new VerticalPanel();
      nativeLangGroup = addControlFormField(register, "Native Lang (L1)");
      dialectGroup = addControlFormField(register, "Dialect");
      ageEntryGroup = addControlFormField(register, "Your age");
      genderGroup = getListBoxFormField(register, "Select gender", getGenderBox());
      experienceGroup = getListBoxFormField(register, "Select months of experience", getExperienceBox());

     dialogBox.setMaxHeigth(Window.getClientHeight()*0.8 + "px");
    if (!isDataCollectAdmin) {
      dp = new DisclosurePanel("Registration");
      dp.setContent(register);
      dp.addOpenHandler(new OpenHandler<DisclosurePanel>() {
        @Override
        public void onOpen(OpenEvent<DisclosurePanel> event) {
          centerVertically(dialogBox.getElement()); // need to resize the dialog when reveal hidden widgets
        }
      });

      dp.addCloseHandler(new CloseHandler<DisclosurePanel>() {
        @Override
        public void onClose(CloseEvent<DisclosurePanel> event) {
          centerVertically(dialogBox.getElement());
        }
      });
      dialogBox.add(dp);
    }

    FlowPanel hp = new FlowPanel();
    hp.getElement().getStyle().setFloat(Style.Float.RIGHT);
    hp.add(login);

    dialogBox.add(hp);

    login.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        // System.out.println("login button got click " + event);

        final String userID = user.box.getText();
        if (userID.length() == 0) {
          markError(user, "Please enter a user id.");
        } else {
          service.userExists(userID, new AsyncCallback<Integer>() {
            public void onFailure(Throwable caught) {
              Window.alert("userExists : Couldn't contact server");
            }

            public void onSuccess(Integer result) {
              boolean exists = result != -1;
              if (exists) {
                if (!checkPassword(password)) {
                  markError(password, "Please use password from the email.");
                } else if (checkAudioSelection(regular, fastThenSlow)) {
                  markError(recordingStyle, regular, "Try again", "Please choose either regular or regular then slow audio recording.");

                } else {
                  dialogBox.hide();
                  String audioType = fastThenSlow.getValue() ? Result.AUDIO_TYPE_FAST_AND_SLOW : Result.AUDIO_TYPE_REGULAR;
                  storeAudioType(audioType);
                  storeUser(result, audioType, userID, PropertyHandler.LOGIN_TYPE.DATA_COLLECTOR);
                }
              } else {
                System.out.println(userID + " doesn't exist");
                if (checkPassword(password)) {
                  doRegistration(user, password, recordingStyle,
                    regular, fastThenSlow, nativeLangGroup, dialectGroup, ageEntryGroup,
                    experienceGroup, genderGroup, dialogBox, login);
                } else {
                  markError(password, "Please use password from the email.");
                }
              }
            }
          });
        }
      }
    });
    dialogBox.show();

    dialogBox.addHiddenHandler(new HiddenHandler() {
      @Override
      public void onHidden(HiddenEvent hiddenEvent) {
        removeKeyHandler();
      }
    });

    dialogBox.addShowHandler(new ShowHandler() {
      @Override
      public void onShow(ShowEvent showEvent) {
        addKeyHandler(login);
      }
    });
  }

  private HandlerRegistration keyHandler;

  private void addKeyHandler(final Button send) {
    keyHandler = Event.addNativePreviewHandler(new
                                                 Event.NativePreviewHandler() {

                                                   @Override
                                                   public void onPreviewNativeEvent(Event.NativePreviewEvent event) {
                                                     NativeEvent ne = event.getNativeEvent();
                                                     int keyCode = ne.getKeyCode();

                                                     boolean isEnter = keyCode == KeyCodes.KEY_ENTER;

                                                     //   System.out.println("key code is " +keyCode);
                                                     if (isEnter && event.getTypeInt() == 512 &&
                                                       "[object KeyboardEvent]".equals(ne.getString())) {
                                                       ne.preventDefault();
                                                       send.fireEvent(new ButtonClickEvent());
                                                     }
                                                   }
                                                 });
    // System.out.println("addKeyHandler made click handler " + keyHandler);
  }

  private class ButtonClickEvent extends ClickEvent{
        /*To call click() function for Programmatic equivalent of the user clicking the button.*/
  }

  public void removeKeyHandler() {
    System.out.println("removeKeyHandler : " + keyHandler);

    if (keyHandler != null) keyHandler.removeHandler();
  }

  /**
   * From Modal code.
   * Centers fixed positioned element vertically.
   * @param e Element to center vertically
   */
  private native void centerVertically(Element e) /*-{
      $wnd.jQuery(e).css("margin-top", (-1 * $wnd.jQuery(e).outerHeight() / 2) + "px");
  }-*/;

  private FormField addControlFormField(Panel dialogBox, String label) { return addControlFormField(dialogBox, label, false); }

  private FormField addControlFormField(Panel dialogBox, String label, boolean isPassword) {
    final TextBox user = isPassword ? new PasswordTextBox() : new TextBox();

    return getFormField(dialogBox, label, user);
  }

  private FormField getFormField(Panel dialogBox, String label, TextBox user) {
    final ControlGroup userGroup = addControlGroupEntry(dialogBox, label, user);
    return new FormField(user, userGroup);
  }

  private ListBoxFormField getListBoxFormField(Panel dialogBox, String label, ListBox user) {
    final ControlGroup userGroup = addControlGroupEntry(dialogBox, label, user);
    return new ListBoxFormField(user, userGroup);
  }

  private ControlGroup addControlGroupEntry(Panel dialogBox, String label, Widget user) {
    final ControlGroup userGroup = new ControlGroup();
    userGroup.add(new ControlLabel(label));
    dialogBox.add(userGroup);
    userGroup.add(user);
    userGroup.addStyleName("leftFiveMargin");
    return userGroup;
  }

  private static class FormField {
    public final TextBox box;
    public final ControlGroup group;

    public FormField(final TextBox box, final ControlGroup group) {
      this.box = box;

      box.addKeyUpHandler(new KeyUpHandler() {
        public void onKeyUp(KeyUpEvent event) {
          if (box.getText().length() > 0) {
            group.setType(ControlGroupType.NONE);
          }
        }
      });

      this.group = group;
    }

    public String getText() { return box.getText(); }
  }

  private static class ListBoxFormField {
    public final ListBox box;
    public final ControlGroup group;

    public ListBoxFormField(ListBox box, ControlGroup group) {
      this.box = box;
      this.group = group;
    }
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
  private void doRegistration(FormField user, FormField password, ControlGroup audioGroup,
                              RadioButton regular,
                              RadioButton fastThenSlow,
                              FormField nativeLang, FormField dialect, FormField ageEntryBox,
                              ListBoxFormField experienceBox, ListBoxFormField genderBox,
                              Modal dialogBox,
                              Button login) {

    boolean valid = user.box.getText().length() > 0;
    if (!valid) {
      markError(user,"Please enter a userid.");
    } else {
      valid = password.box.getText().length() > 0;
      if (!valid) {
        markError(user,"Please enter a userid.");
      }
    }
    if (valid) {
      valid = checkPassword(password);
      if (!valid) {
        markError(password, "Please use password from the email sent to you.");
        valid = false;
      } else if (!isDataCollectAdmin && checkAudioSelection(regular, fastThenSlow)) {
        markError(audioGroup, regular, "Try Again", "Please choose either regular or regular then slow audio recording.");
        valid = false;
      } else if (!isDataCollectAdmin && nativeLang.getText().isEmpty()) {
        if (!dp.isOpen()) {
          dp.setOpen(true);   // reveal registration fields
        } else {
          markError(nativeLang, "Language is empty");
        }
        valid = false;
      } else if (!isDataCollectAdmin && dialect.getText().isEmpty()) {
        markError(dialect, "Dialect is empty");
        valid = false;
      }

      if (valid) {
        try {
          int age = getAge(ageEntryBox.box);
          if (!isDataCollectAdmin && (age < MIN_AGE) || (age > MAX_AGE && age != TEST_AGE)) {
            valid = false;
            markError(ageEntryBox, "age '" + age + "' is too young or old.");
          }
        } catch (NumberFormatException e) {
          markError(ageEntryBox, "age '" + ageEntryBox.getText() + "' is invalid.");
          valid = false;
        }
      }
      if (valid) {
        int enteredAge = getAge(ageEntryBox.box);
        checkUserOrCreate(enteredAge, user, experienceBox.box, genderBox.box, nativeLang.box, dialect.box, dialogBox,
          login, fastThenSlow.getValue());
      } else {
        //System.out.println("not valid ------------ ?");
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
  private void checkUserOrCreate(final int enteredAge, final FormField user, final ListBox experienceBox,
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
            experienceBox, genderBox, nativeLang, dialect, user.box, dialogBox, closeButton, isFastAndSlow);
        } else {
          markError(user, "User " + user.getText() + " already registered, click login.");
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

  private boolean checkPassword(FormField password) {
    return checkPassword(password.box);
  }

  private boolean checkPassword(TextBox password) {
    String trim = password.getText().trim();
    boolean valid = trim.equalsIgnoreCase(GRADING) || trim.equalsIgnoreCase(TESTING);
    return valid;
  }

  /**
   * TODO : make this use convenience methods for making control groups
   * @see #login()
   */
  private void displayLoginBox() {
    // Create the popup dialog box
     final Modal dialogBox = new Modal();
     dialogBox.setCloseVisible(false);
    dialogBox.setKeyboard(false);
    dialogBox.setBackdrop(BackdropType.STATIC);
    dialogBox.setTitle("Login Questions");

    final TextBox ageEntryBox = new TextBox();
    final ControlGroup ageGroup = new ControlGroup();
    ageGroup.add(new ControlLabel("Please enter your age"));
    ageGroup.add(ageEntryBox);
    final Button closeButton = makeCloseButton(ageEntryBox, ageGroup);

    // add experience drop box
    dialogBox.add(ageGroup);

    ControlGroup genderGroup = new ControlGroup();
    genderGroup.add(new ControlLabel("Select gender"));
    final ListBox genderBox = getGenderBox();
    genderGroup.add(genderBox);
    dialogBox.add(genderGroup);

    ControlGroup expGroup = new ControlGroup();
    expGroup.add(new ControlLabel("Select months of experience"));
    final ListBox experienceBox = getExperienceBox();
    expGroup.add(experienceBox);
    dialogBox.add(expGroup);

    final ControlGroup dialectGroup = new ControlGroup();
    dialectGroup.add(new ControlLabel("Enter dialect"));
    final TextBox dialect = new TextBox();
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
            markError(dialectGroup, dialect,"Try again", "Please enter a language dialect.");
            dialect.setFocus(true);
          } else {
            dialogBox.hide();
            sendNameToServer();
          }
        }
        else {
          setupPopover(ageEntryBox, "Try again", "Please enter age between " + MIN_AGE + " and " + MAX_AGE+".");
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

  private void markError(FormField dialectGroup, String message) {
    markError(dialectGroup.group, dialectGroup.box, "Try Again", message);
  }

/*  private void markError(FormField dialectGroup, String header, String message) {
    markError(dialectGroup.group, dialectGroup.box, header, message);
  }*/

/*  private void markError(ListBoxFormField dialectGroup, String message) {
    markError(dialectGroup.group, dialectGroup.box, "Try Again", message);
  }*/

/*
  private void markError(ListBoxFormField dialectGroup, String header, String message) {
    markError(dialectGroup.group, dialectGroup.box, header, message);
  }
*/

  private void markError(ControlGroup dialectGroup, FocusWidget dialect, String header, String message) {
    dialectGroup.setType(ControlGroupType.ERROR);
    dialect.setFocus(true);

    setupPopover(dialect, header, message);//"Try again", "Please enter a language dialect.");
  }

  private void setupPopover(final Widget w, String heading, final String message) {
   // System.out.println("triggering popover on " + w + " with " + message);
    final Popover popover = new Popover();
    popover.setWidget(w);
    popover.setText(message);
    popover.setHeading(heading);
    popover.setPlacement(Placement.RIGHT);
    popover.reconfigure();
    popover.show();

    Timer t = new Timer() {
      @Override
      public void run() {
        //System.out.println("hide popover on " + w + " with " + message);
        popover.hide();
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
   */
  private void addUser(int monthsOfExperience, TextBox ageEntryBox, ListBox genderBox, TextBox dialectBox) {
    int age = getAge(ageEntryBox);
    String gender = genderBox.getValue(genderBox.getSelectedIndex());
    addUser(age, gender, monthsOfExperience, dialectBox.getText(), PropertyHandler.LOGIN_TYPE.STUDENT);
  }

  /**
   * @see #addAnonymousUser()
   * @param age
   * @param gender
   * @param monthsOfExperience
   */
  private void addUser(int age, String gender, int monthsOfExperience) {
    addUser(age, gender, monthsOfExperience, "", PropertyHandler.LOGIN_TYPE.ANONYMOUS);
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

  private ListBox getGenderBox() {
    final ListBox genderBox = new ListBox(false);
    for (String s : Arrays.asList("Male", "Female")) {
      genderBox.addItem(s);
    }
    genderBox.ensureDebugId("cwListBox-dropBox");
    return genderBox;
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
}
