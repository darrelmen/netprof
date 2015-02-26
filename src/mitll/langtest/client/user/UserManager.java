package mitll.langtest.client.user;

import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.BrowserCheck;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.custom.KeyStorage;
import mitll.langtest.client.flashcard.ControlState;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.User;

import java.util.Date;
import java.util.logging.Logger;

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
  private Logger logger = Logger.getLogger("UserManager");

  private static final long HOUR_IN_MILLIS = 1000 * 60 * 60;

  private static final int DAY_HOURS = 24;
  private static final long WEEK_HOURS = DAY_HOURS * 7;
  //private static final int ONE_YEAR = 24 * 365;

  private static final long EXPIRATION_HOURS = 52*WEEK_HOURS * HOUR_IN_MILLIS;
 // private static final int SHORT_EXPIRATION_HOURS = DAY_HOURS;
//  private static final int FOREVER_HOURS = ONE_YEAR;

  private static final int NO_USER_SET = -1;

  private static final String USER_ID = "userID";
  private static final String USER_CHOSEN_ID = "userChosenID";
  private static final String AUDIO_TYPE = "audioType";

  private final LangTestDatabaseAsync service;
  private final UserNotification userNotification;
  private final static boolean USE_COOKIE = false;
  private long userID = NO_USER_SET;
  private String userChosenID = "";

  private final PropertyHandler.LOGIN_TYPE loginType;
  private final String appTitle;
  private final PropertyHandler props;
  private boolean isMale;
  private boolean isTeacher;

  /**
   * @param lt
   * @param service
   * @param props
   * @see mitll.langtest.client.LangTest#onModuleLoad2()
   */
  public UserManager(UserNotification lt, LangTestDatabaseAsync service, PropertyHandler props) {
    this.userNotification = lt;
    this.service = service;
    this.props = props;
    this.loginType = props.getLoginType();
    this.appTitle = props.getAppTitle();
  }

  /**
   * Keeping option to do an anonymous login...
   * for egyptian class...
   * 8/22/14
   *
   * @see mitll.langtest.client.LangTest#checkLogin()
   */
  public void checkLogin() {
    //logger.info("loginType " + loginType);
    if (loginType.equals(PropertyHandler.LOGIN_TYPE.ANONYMOUS)) { // explicit setting of login type
      anonymousLogin();
    } else {
      login();
    }
  }

  /**
   * @see #checkLogin()
   */
  private void login() {
    final int user = getUser();
    if (user != NO_USER_SET) {
      //logger.info("UserManager.login : current user : " + user);
      console("UserManager.login : current user : " + user);
      rememberAudioType();
      getPermissionsAndSetUser(user);
    }
    else {
      userNotification.showLogin();
    }
  }

  private void console(String message) {
    int ieVersion = BrowserCheck.getIEVersion();
    if (ieVersion == -1 || ieVersion > 9) {
      consoleLog(message);
    }
  }

  private native static void consoleLog(String message) /*-{
      console.log("UserManager:" + message);
  }-*/;


  /**
   * TODOx : instead have call to get permissions for a user.
   * @param user
   * @see #login()
   * @see #storeUser
   */
  private void getPermissionsAndSetUser(final int user) {
    console("getPermissionsAndSetUser : " + user);
   // logger.info("UserManager.getPermissionsAndSetUser " + user + " asking server for info...");

    service.getUserBy(user, new AsyncCallback<User>() {
      @Override
      public void onFailure(Throwable caught) {

      }

      @Override
      public void onSuccess(User result) {
//        logger.info("UserManager.getPermissionsAndSetUser : onSuccess " + user + " : " + result);

        if (loginType == PropertyHandler.LOGIN_TYPE.ANONYMOUS && result.getUserKind() != User.Kind.ANONYMOUS) {
          clearUser();
          addAnonymousUser();
        } else if (loginType != PropertyHandler.LOGIN_TYPE.ANONYMOUS && result.getUserKind() == User.Kind.ANONYMOUS) {
          clearUser();
          userNotification.showLogin();
        } else {
          gotNewUser(result);
        }
      }
    });
  }

  /**
   * Only content developers can do quality control or record audio.
   *
   * Legacy people must get approval.
   *
   * @see #storeUser
   * @param result
   */
  private void gotNewUser(User result) {
    logger.info("UserManager.gotNewUser " + result);
    userNotification.getPermissions().clear();
    if (result != null) {
      boolean isCD = result.getUserKind() == User.Kind.CONTENT_DEVELOPER;
      for (User.Permission permission : result.getPermissions()) {
        boolean valid = true;
        if (permission == User.Permission.QUALITY_CONTROL ||
            permission == User.Permission.RECORD_AUDIO) valid = isCD;
        if (valid) {
          userNotification.setPermission(permission, true);
        }
      }
      isMale = result.isMale();
      isTeacher = (result.getUserKind() == User.Kind.TEACHER)|| isCD;
      //logger.info("\t is male " + isMale + " is CD " + isCD + " is teacher " + isTeacher);

      userNotification.gotUser(result);
    }
    //console("getPermissionsAndSetUser.onSuccess : " + user);
  }

  /**
   * So if there's a current user, ask the server about them.
   * If not add a new anonymous user.
   * @see #checkLogin()
   * @see mitll.langtest.client.LangTest#checkLogin
   */
  private void anonymousLogin() {
    int user = getUser();
    if (user != NO_USER_SET) {
      //logger.info("UserManager.anonymousLogin : current user : " + user);
      rememberAudioType(); // TODO : necessary?
    //  userNotification.gotUser(user);
      getPermissionsAndSetUser(user);
    } else {
      logger.info("UserManager.anonymousLogin : make new user, since user = " + user);

      addAnonymousUser();
    }
  }

  private void addAnonymousUser() {
    logger.info("UserManager.addAnonymousUser : adding anonymous user");

    service.addUser("anonymous", "", "", User.Kind.ANONYMOUS, Window.Location.getHref(), "", true, 0, "unknown", false, "browser", new AsyncCallback<User>() {
      @Override
      public void onFailure(Throwable caught) {

      }

      @Override
      public void onSuccess(User result) {
        setDefaultControlValues((int)result.getId());
        storeUser(result, Result.AUDIO_TYPE_PRACTICE);
      }
    });
  }

  private void setDefaultControlValues(int user) {
    ControlState controlState = new ControlState();
    controlState.setStorage(new KeyStorage(props.getLanguage(), user));
    controlState.setAudioOn(true);
    controlState.setAudioFeedbackOn(true);
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
      //logger.info("found key " +unansweredKey + " = " + unanswered);
      showUnansweredFirst = unanswered.equalsIgnoreCase("true");
    }
    else {
      //logger.info("===> no key " +unansweredKey);
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
      String sid = getUserFromStorage();
      return (sid == null || sid.equals("" + NO_USER_SET)) ? NO_USER_SET : Integer.parseInt(sid);
    } else {
      return (int) userID;
    }
  }

  /**
   * @return
   * @see mitll.langtest.client.LangTest#checkLogin();
   */
  public boolean isUserExpired() {
    String sid = getUserFromStorage();

    String shownHello = Storage.getLocalStorageIfSupported().getItem(UserPassLogin.SHOWN_HELLO);
    logger.info("user id cookie for " + getUserIDCookie() + " is " + sid + " shown hello " + shownHello);
    return (sid == null || sid.equals("" + NO_USER_SET)) ||
        //shownHello == null ||
        checkUserExpired(sid);
  }

  private String getUserFromStorage() {
    Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
    return localStorageIfSupported.getItem(getUserIDCookie());
  }

  /**
   * @param sid
   * @return
   * @see #isUserExpired()
   */
  private boolean checkUserExpired(String sid) {
    boolean expired = false;
    if (userExpired(sid)) {
      clearUser();
      expired = true;
    }
    return expired;
  }

  /**
   * Need these to be prefixed by app title so if we switch webapps, we don't get weird user ids
   *
   * @return
   */
  private String getUserIDCookie() { return appTitle + ":" + USER_ID;  }
  private String getUserChosenID() {
    return appTitle + ":" + USER_CHOSEN_ID;
  }
  private String getAudioType() {
    return appTitle + ":" + AUDIO_TYPE;
  }
  private String getExpires() {
    return appTitle + ":" + "expires";
  }

  /**
   * @param sid
   * @see #getUser()
   */
  private boolean userExpired(String sid) {
    String expires = getExpiresCookie();
    if (expires == null) {
      logger.info("userExpired : checkExpiration : no expires item?");
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
          logger.info("userExpired : checkExpiration : " + sid + " has expired : " + new Date(expirationDate));
          return true;
        }
      } catch (NumberFormatException e) {
        e.printStackTrace();
      }
    }
    return false;
  }

  /**
   * @return
   * @see #userExpired(String)
   */
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
      logger.info("clearUser : removed item " + getUserID() + " user now " + getUser());
    } else {
      userID = NO_USER_SET;
    }
  }

  /**
   * @see mitll.langtest.client.user.UserPassLogin#storeUser(mitll.langtest.shared.User)
   * @param user
   * @param audioType
   */
  void storeUser(User user, String audioType) {
    logger.info("storeUser : user now " + user + " audio type '" + audioType + "'");
    final long DURATION = getUserSessionDuration();
    long futureMoment = getUserSessionEnd(DURATION);
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
      userChosenID = user.getUserID();
      localStorageIfSupported.setItem(getUserIDCookie(), "" + user.getId());
      localStorageIfSupported.setItem(getUserChosenID(), "" + userChosenID);
      rememberUserSessionEnd(localStorageIfSupported, futureMoment);
      localStorageIfSupported.setItem(getAudioType(), "" + audioType);
      // localStorageIfSupported.setItem(getLoginType(), "" + userType);
      logger.info("storeUser : user now " + user.getId() + " / " + getUser() + " audio '" + audioType + "' expires in " + (DURATION / 1000) + " seconds");
      userNotification.rememberAudioType(audioType);

      gotNewUser(user);

    } else {  // not sure what we could possibly do here...
      userID = user.getId();
      userNotification.gotUser(user);
    }
  }

  /**
   * @param futureMoment
   * @see #userExpired(String)
   */
  private void rememberUserSessionEnd(long futureMoment) {
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
      rememberUserSessionEnd(localStorageIfSupported, futureMoment);
    }
  }

  /**
   * @param localStorageIfSupported
   * @param futureMoment
   * @see #storeUser
   * @see #rememberUserSessionEnd(long)
   */
  private void rememberUserSessionEnd(Storage localStorageIfSupported, long futureMoment) {

    localStorageIfSupported.setItem(getExpires(), "" + futureMoment);

    String expires = getExpiresCookie();

    long expirationDate = Long.parseLong(expires);
    logger.info("rememberUserSessionEnd : user will expire on " + new Date(expirationDate));
  }

  private long getUserSessionEnd() {
    return getUserSessionEnd(getUserSessionDuration());
  }

  private long getUserSessionEnd(long DURATION) {
    return System.currentTimeMillis() + DURATION;
  }

  /**
   * If we have lots of students moving through stations quickly, we want to auto logout once a day, once an hour?
   * <p/>
   * Egyptian should never time out -- for anonymous students
   *
   * @return one year for anonymous
   */
  private long getUserSessionDuration() {
    long mult = loginType.equals(PropertyHandler.LOGIN_TYPE.ANONYMOUS) ? 52 : 4;
    return EXPIRATION_HOURS * mult;
  }

  public boolean isMale() {
    return isMale;
  }

  public boolean isTeacher() {
    return isTeacher;
  }
}
