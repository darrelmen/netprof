package mitll.langtest.client.user;

import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.BrowserCheck;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.User;

import java.util.ArrayList;
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

  private static final int DAY_HOURS = 24;
  private static final int WEEK_HOURS = DAY_HOURS * 7;
  //private static final int ONE_YEAR = 24 * 365;

  private static final int EXPIRATION_HOURS = WEEK_HOURS;
 // private static final int SHORT_EXPIRATION_HOURS = DAY_HOURS;
//  private static final int FOREVER_HOURS = ONE_YEAR;

  private static final int NO_USER_SET = -1;

  private static final String USER_ID = "userID";
  private static final String USER_CHOSEN_ID = "userChosenID";
  private static final String AUDIO_TYPE = "audioType";
  private static final String LOGIN_TYPE = "loginType";

  private final LangTestDatabaseAsync service;
  private final UserNotification userNotification;
  private final static boolean USE_COOKIE = false;
  private long userID = NO_USER_SET;
  private String userChosenID = "";

  private final PropertyHandler.LOGIN_TYPE loginType;
  private final String appTitle;
  private final PropertyHandler props;

  /**
   * @see mitll.langtest.client.LangTest#onModuleLoad2()
   * @param lt
   * @param service
   * @param props
   */
  public UserManager(UserNotification lt, LangTestDatabaseAsync service, PropertyHandler props) {
    this.userNotification = lt;
    this.service = service;
    this.props = props;
    this.loginType = props.getLoginType();
    this.appTitle = props.getAppTitle();
  }

  /**
   * @see mitll.langtest.client.LangTest#checkLogin()
   */
  public void checkLogin() {
    //System.out.println("loginType " + loginType);
    if (loginType.equals(PropertyHandler.LOGIN_TYPE.ANONYMOUS)) { // explicit setting of login type
      anonymousLogin();
    } else if (loginType.equals(PropertyHandler.LOGIN_TYPE.UNDEFINED) && // no explicit setting, so it's dependent on the mode
      (props.isGoodwaveMode() || isInitialFlashcardTeacherView())) {   // no login for pron mode
      anonymousLogin();
    } else {
      login();
    }
  }

  private boolean isInitialFlashcardTeacherView() {
    return (props.isFlashcardTeacherView() && !props.isFlashCard());
  }

  /**
   * @see #checkLogin()
   */
  private void login() {
    final int user = getUser();
    if (user != NO_USER_SET) {
      //System.out.println("UserManager.login : current user : " + user);
      console("UserManager.login : current user : " + user);
      rememberAudioType();
      getPermissionsAndSetUser(user);
    }
    else {
      new StudentDialog(service, props, this, userNotification).displayLoginBox();
    }
  }

  private void console(String message) {
    int ieVersion = BrowserCheck.getIEVersion();
    if (ieVersion == -1 || ieVersion > 9) {
      consoleLog(message);
    }
  }

  private native static void consoleLog( String message) /*-{
      console.log( "UserManager:" + message );
  }-*/;


  /**
   * TODO : instead have call to get permissions for a user.
   * @param user
   */
  private void getPermissionsAndSetUser(final int user) {
    console("getPermissionsAndSetUser : " + user);

    service.getUserBy(user, new AsyncCallback<User>() {
      @Override
      public void onFailure(Throwable caught) {

      }

      @Override
      public void onSuccess(User result) {
        gotNewUser(result);
      }
    });
  }

  public void gotNewUser(User result) {
    userNotification.getPermissions().clear();
    if (result != null) {
      for (User.Permission permission : result.getPermissions()) {
        userNotification.setPermission(permission, true);
      }
    }
    //console("getPermissionsAndSetUser.onSuccess : " + user);

    userNotification.gotUser(result.getId());
  }

  /**
   * @see mitll.langtest.client.LangTest#checkLogin
   */
  private void anonymousLogin() {
    int user = getUser();
    if (user != NO_USER_SET) {
      //System.out.println("UserManager.anonymousLogin : current user : " + user);
      rememberAudioType();
      userNotification.gotUser(user);
    }
    else {
      System.out.println("UserManager.anonymousLogin : make new user, since user = " + user);

      addAnonymousUser();
  }
  }

  private void addAnonymousUser() {
    StudentDialog studentDialog = new StudentDialog(service,props,this,userNotification);
    System.out.println("UserManager.addAnonymousUser : adding anonymous user");

    studentDialog.addUser(89, "male", 0,"", new ArrayList<User.Permission>());
  }

  /**
   * For display purposes
   * @return
   * @see mitll.langtest.client.LangTest#getGreeting()
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
      userNotification.rememberAudioType(audioType);
    }
  }

/*
  private void addBinaryKey(boolean val, String unansweredKey) {
    Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
    localStorageIfSupported.setItem(unansweredKey, val ? "true" : "false");
  }*/


/*  private boolean getBinaryKey(String unansweredKey) {
    Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();

    boolean showUnansweredFirst = false;
    String unanswered = localStorageIfSupported.getItem(unansweredKey);
    if (unanswered != null) {
      //System.out.println("found key " +unansweredKey + " = " + unanswered);
      showUnansweredFirst = unanswered.equalsIgnoreCase("true");
    }
    else {
      //System.out.println("===> no key " +unansweredKey);
    }
    return showUnansweredFirst;
  }*/

  /**
   * @return id of user
   * @see mitll.langtest.client.LangTest#getUser
   */
  public int getUser() {
    if (USE_COOKIE) {
      String sid = Cookies.getCookie("sid");
      if (sid == null || sid.equals("" + NO_USER_SET)) {
        return NO_USER_SET;
      }
      return Integer.parseInt(sid);
    } else if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
      String sid = localStorageIfSupported.getItem(getUserIDCookie());

      //System.out.println("user id cookie for " +getUserIDCookie() + " is " + sid);
 /*     if (sid != null && !sid.equals("" + NO_USER_SET)) {
        boolean expired = checkUserExpired(sid);
        if (expired) checkLogin();
        sid = localStorageIfSupported.getItem(getUserIDCookie());
      }*/
      return (sid == null || sid.equals("" + NO_USER_SET)) ? NO_USER_SET : Integer.parseInt(sid);
    } else {
      return (int) userID;
    }
  }

  /**
   * @see mitll.langtest.client.LangTest#checkLogin();
   * @return
   */
  public boolean isUserExpired() {
    Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
    String sid = localStorageIfSupported.getItem(getUserIDCookie());

    //System.out.println("user id cookie for " +getUserIDCookie() + " is " + sid);
    return (sid == null || sid.equals("" + NO_USER_SET)) || checkUserExpired(sid);
  }

  /**
   * @see #isUserExpired()
   * @param sid
   * @return
   */
  private boolean checkUserExpired(String sid) {
    boolean expired = false;
    if (userExpired(sid)) {
      clearUser();
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
   * @see mitll.langtest.client.LangTest#resetState()
   */
  public void clearUser() {
    clearCookieState();
  }

  private void clearCookieState() {
    userNotification.rememberAudioType(Result.AUDIO_TYPE_UNSET);
    if (USE_COOKIE) {
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
   * TODO : move cookie manipulation to separate class
   *
   * @param sessionID    from database
   * @param audioType
   * @param userChosenID
   * @see StudentDialog#addUser
   */
  void storeUser(long sessionID, String audioType, String userChosenID, PropertyHandler.LOGIN_TYPE userType) {
    //System.out.println("storeUser : user now " + sessionID + " audio type '" + audioType +"'");
    final long DURATION = getUserSessionDuration();
    long futureMoment = getUserSessionEnd(DURATION);
    if (USE_COOKIE) {
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
      userNotification.rememberAudioType(audioType);

      getPermissionsAndSetUser((int)sessionID);

    } else {  // not sure what we could possibly do here...
      userID = sessionID;
      this.userChosenID = userChosenID;
      userNotification.gotUser(sessionID);
    }
  }

  void storeUser(User user, String audioType) {
    //System.out.println("storeUser : user now " + sessionID + " audio type '" + audioType +"'");
    final long DURATION = getUserSessionDuration();
    long futureMoment = getUserSessionEnd(DURATION);
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();

      localStorageIfSupported.setItem(getUserIDCookie(), "" + user.getId());
      localStorageIfSupported.setItem(getUserChosenID(), "" + userChosenID);
      rememberUserSessionEnd(localStorageIfSupported, futureMoment);
      localStorageIfSupported.setItem(getAudioType(), "" + audioType);
      // localStorageIfSupported.setItem(getLoginType(), "" + userType);
      System.out.println("storeUser : user now " + user.getId() + " / " + getUser() + " audio '" + audioType + "' expires in " + (DURATION / 1000) + " seconds");
      userNotification.rememberAudioType(audioType);

      gotNewUser(user);

    } else {  // not sure what we could possibly do here...
      userID = user.getId();
      userNotification.gotUser(userID);
    }
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

/*
  private void updateUserSessionExpires() {
    final long DURATION = getUserSessionDuration();
    long futureMoment = getUserSessionEnd(DURATION);
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
      rememberUserSessionEnd(localStorageIfSupported, futureMoment);
    }
  }
*/

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
   * @return
   */
  private long getUserSessionDuration() {
    //boolean useShortExpiration = loginType.equals(PropertyHandler.LOGIN_TYPE.STUDENT);
    return HOUR_IN_MILLIS * EXPIRATION_HOURS;//(useShortExpiration ? SHORT_EXPIRATION_HOURS : EXPIRATION_HOURS);
  }
}
