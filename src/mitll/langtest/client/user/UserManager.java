package mitll.langtest.client.user;

import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Timer;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.shared.Result;

import java.util.Date;

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
  private static final long HOUR_IN_MILLIS = 1000 * 60 * 60;

  private static final int WEEK_HOURS = 24 * 7;
  private static final int DAY_HOURS = 24;
  //private static final int ONE_YEAR = 24 * 365;

  private static final int EXPIRATION_HOURS = WEEK_HOURS;
  private static final int SHORT_EXPIRATION_HOURS = DAY_HOURS;
//  private static final int FOREVER_HOURS = ONE_YEAR;

  private static final int NO_USER_SET = -1;

  private static final String USER_ID = "userID";
  private static final String USER_CHOSEN_ID = "userChosenID";
  private static final String AUDIO_TYPE = "audioType";
  private static final String LOGIN_TYPE = "loginType";
  private static final int INACTIVE_PERIOD_MILLIS = 1000 * 60 * 10; // ten minutes

  private final LangTestDatabaseAsync service;
  private final UserNotification langTest;
  private final boolean useCookie = false;
  private long userID = NO_USER_SET;
  private String userChosenID = "";

  private final PropertyHandler.LOGIN_TYPE loginType;
  private final String appTitle;
  private final PropertyHandler props;

  private Timer userTimer;

  /**
   * @see mitll.langtest.client.LangTest#onModuleLoad2()
   * @see mitll.langtest.client.LangTest#doFlashcard()
   * @see mitll.langtest.client.LangTest#doDataCollectAdminView
   * @param lt
   * @param service
   * @param props
   */
  public UserManager(UserNotification lt, LangTestDatabaseAsync service, PropertyHandler props) {
    this.langTest = lt;
    this.service = service;
    this.props = props;
    this.loginType = props.getLoginType();
    this.appTitle = props.getAppTitle();
  }

  public void checkLogin() {
    //System.out.println("loginType " + loginType);
    if (loginType.equals(PropertyHandler.LOGIN_TYPE.ANONYMOUS)) { // explicit setting of login type
      anonymousLogin();
    } else if (loginType.equals(PropertyHandler.LOGIN_TYPE.UNDEFINED) && // no explicit setting, so it's dependent on the mode
      (props.isGoodwaveMode() || props.isAutocrt() || isInitialFlashcardTeacherView())) {   // no login for pron mode
      anonymousLogin();
    } else {
      loginDifferentTypes();
    }
  }

  private boolean isInitialFlashcardTeacherView() {
    return (props.isFlashcardTeacherView() && !props.isFlashCard());
  }

  /**
   * @see #checkLogin()
   */
  private void loginDifferentTypes() {
/*    System.out.println("loginType " + loginType +
      " data collect mode " + props.isDataCollectMode() +
      " crt data collect " + props.isCRTDataCollectMode() +
      " teacher " + props.isTeacherView() + " grading " + props.isGrading());*/

    if (loginType.equals(PropertyHandler.LOGIN_TYPE.STUDENT)) {
      login();
    }
    else if (loginType.equals(PropertyHandler.LOGIN_TYPE.DATA_COLLECTOR)) {
      teacherLogin();
    } // next has already been done in checkLogin
  /*  else if (loginType.equals(PropertyHandler.LOGIN_TYPE.ANONYMOUS)) {
      teacherLogin();
    }*/
    else if (!props.isFlashCard() && ((props.isDataCollectMode() && !props.isCRTDataCollectMode()) || props.isTeacherView() || props.isGrading())) {
      System.out.println("doing teacher login");
      teacherLogin();
    }
    else {
      System.out.println("doing student login");

      login();
    }
  }
  // user tracking

  /**
   * Somebody should call this when the user does something in taboo
   * @see mitll.langtest.client.LangTest#pingAliveUser()
   */
  public void userAlive() {
    updateUserSessionExpires();
    if (!isActive()) System.err.println("huh? user is not active...\n\n\n");
    // System.out.println(new Date() +" --------> userAlive : " + user);
    //userOnline(user, true);
    waitThenInactivate();
  }

  private void userInactive() {
    int user = getUser();
    System.out.println(new Date() + " --------> userInactive : " + user);
  }

  public boolean isActive() {
    return getUser() != NO_USER_SET;
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

  /**
   * @see #userExpired(String)
   * @param futureMoment
   */
  private void rememberUserSessionEnd(long futureMoment) {
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
      rememberUserSessionEnd(localStorageIfSupported, futureMoment);
    }
  }

  private void updateUserSessionExpires() {
    final long DURATION = getUserSessionDuration();
    long futureMoment = getUserSessionEnd(DURATION);
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
      rememberUserSessionEnd(localStorageIfSupported, futureMoment);
    }
  }

  /**
   * @see #storeUser(long, String, String, mitll.langtest.client.PropertyHandler.LOGIN_TYPE)
   * @see #rememberUserSessionEnd(long)
   * @param localStorageIfSupported
   * @param futureMoment
   */
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
      (useShortExpiration ? SHORT_EXPIRATION_HOURS : EXPIRATION_HOURS));
  }

  /**
   * @see #loginDifferentTypes()
   */
  private void login() {
    int user = getUser();
    if (user != NO_USER_SET) {
      System.out.println("UserManager.login : current user : " + user);
      rememberAudioType();
      langTest.gotUser(user);
    }
    else {
      StudentDialog studentDialog = new StudentDialog(service,props,this, langTest);
      studentDialog.displayLoginBox();
    }
  }

  /**
   * @see mitll.langtest.client.LangTest#checkLogin
   */
  protected void anonymousLogin() {
    int user = getUser();
    if (user != NO_USER_SET) {
      System.out.println("UserManager.anonymousLogin : current user : " + user);
      rememberAudioType();
      langTest.gotUser(user);
    }
    else {
      System.out.println("UserManager.anonymousLogin : make new user, since user = " + user);

      addAnonymousUser();
    }
  }

  private void addAnonymousUser() {
    StudentDialog studentDialog = new StudentDialog(service,props,this,langTest);
    System.out.println("UserManager.addAnonymousUser : adding anonymous user");

    studentDialog.addUser(89, "male", 0,"");
  }

  /**
   * For display purposes
   * @return
   */
  public String getUserID() {
    if (Storage.isLocalStorageSupported()) {
      return Storage.getLocalStorageIfSupported().getItem(getUserChosenID());
    } else {
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
    } else if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
      String sid = localStorageIfSupported.getItem(getUserIDCookie());

      //System.out.println("user id cookie for " +getUserIDCookie() + " is " + sid);
      if (sid != null && !sid.equals("" + NO_USER_SET)) {
        boolean expired = checkUserExpired(sid);
        if (expired) checkLogin();
        sid = localStorageIfSupported.getItem(getUserIDCookie());
      }
      return (sid == null || sid.equals("" + NO_USER_SET)) ? NO_USER_SET : Integer.parseInt(sid);
    } else {
      return (int) userID;
    }
  }

  /**
   * @see #getUser()
   * @param sid
   * @return
   */
  private boolean checkUserExpired(String sid) {
    int userID1 = Integer.parseInt(sid);
    boolean expired = false;
    if (userExpired(sid)) {
      clearUser(userID1);
      expired = true;
    }
    // this seems like a bad idea if we can login as data collector or as anonymous...
    /* else if (getLoginTypeFromStorage() != loginType) {
      System.out.println("current login type : " + getLoginTypeFromStorage() + " vs mode " + loginType);
      clearUser(userID1);
      expired = true;
    }*/
    return expired;
  }

  /**
   * Need these to be prefixed by app title so if we switch webapps, we don't get weird user ids
   * @return
   */
  private String getUserIDCookie() {
    return appTitle + ":" + USER_ID;
  }
  private String getUserChosenID() {
    return appTitle + ":" + USER_CHOSEN_ID;
  }
  private String getAudioType() {
    return appTitle + ":" + AUDIO_TYPE;
  }
  private String getLoginType() {
    return appTitle + ":" + LOGIN_TYPE;
  }
  private String getExpires() {
    return appTitle + ":" + "expires";
  }

  /**
   * @see #getUser()
   * @param sid
   */
  private boolean userExpired(String sid) {
    String expires = getExpiresCookie();
    if (expires == null) {
      System.out.println("checkExpiration : no expires item?");
    } else {
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

  /**
   * @see mitll.langtest.client.LangTest#getLogout()
   */
  public void clearUser() {
    userInactive();
    clearCookieState();
  }

  private void clearUser(int userID) {
    clearCookieState();
  }

  private void clearCookieState() {
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
   * @see #loginDifferentTypes()
   */
  public void teacherLogin() {
    int user = getUser();
    if (user != NO_USER_SET) {
      System.out.println("teacherLogin: got cached user : " + user);
      rememberAudioType();
      langTest.gotUser(user);
    } else {
      DataCollectorDialog dataCollectorDialog = new DataCollectorDialog(service, props, langTest, this);
      dataCollectorDialog.displayTeacherLogin("Data Collector Login");
    }
  }

  /**
   * TODO : move cookie manipulation to separate class
   *
   * @param sessionID    from database
   * @param audioType
   * @param userChosenID
   * @see DataCollectorDialog#addTeacher(int, com.github.gwtbootstrap.client.ui.ListBox, com.github.gwtbootstrap.client.ui.ListBox, com.github.gwtbootstrap.client.ui.TextBox, com.github.gwtbootstrap.client.ui.TextBox, com.github.gwtbootstrap.client.ui.TextBox, com.github.gwtbootstrap.client.ui.Modal, com.github.gwtbootstrap.client.ui.Button, boolean)
   * @see StudentDialog#addUser
   * @see DataCollectorDialog#displayTeacherLogin
   */
  void storeUser(long sessionID, String audioType, String userChosenID, PropertyHandler.LOGIN_TYPE userType) {
    System.out.println("storeUser : user now " + sessionID + " audio type '" + audioType +"'");
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
      System.out.println("storeUser : user now " + sessionID + " / " + getUser() + " audio '" + audioType+"' expires in " + (DURATION/1000) + " seconds");
      langTest.rememberAudioType(audioType);
    } else {
      userID = sessionID;
      this.userChosenID = userChosenID;
    }
    langTest.gotUser(sessionID);
  }
}
