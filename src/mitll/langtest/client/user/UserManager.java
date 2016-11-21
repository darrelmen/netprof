/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.user;

import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
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
 * <p>
 * Prompts user for login info.
 * <p>
 * NOTE : will keep prompting them if the browser doesn't let you store cookies.
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 5/15/12
 * Time: 11:32 AM
 * To change this template use File | Settings | File Templates.
 */
public class UserManager {
  private final Logger logger = Logger.getLogger("UserManager");

  private static final long HOUR_IN_MILLIS = 1000 * 60 * 60;

  private static final int DAY_HOURS = 24;
  private static final long WEEK_HOURS = DAY_HOURS * 7;

  private static final long EXPIRATION_HOURS = 52 * WEEK_HOURS * HOUR_IN_MILLIS;

  private static final int NO_USER_SET = -1;
  private static final String NO_USER_SET_STRING = "" + NO_USER_SET;

  private static final String USER_ID = "userID";
  private static final String USER_CHOSEN_ID = "userChosenID";
  private static final String AUDIO_TYPE = "audioType";

  private final LangTestDatabaseAsync service;
  private final UserNotification userNotification;
 // private final static boolean USE_COOKIE = false;
  private long userID = NO_USER_SET;
  private String userChosenID = "";

  private final PropertyHandler.LOGIN_TYPE loginType;
  private final String appTitle;
  private final PropertyHandler props;
  private boolean isMale;
  private boolean isTeacher, isAdmin;
  private User.Kind userKind;

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
   * for egyptian class and headstart?
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
   * TODO : if we have a user id from any site, try to use it to log in to this site.
   * @see #checkLogin()
   */
  private void login() {
    final int user = getUser();
    if (user != NO_USER_SET) {
     // logger.info("UserManager.login : current user : " + user);
      //console("UserManager.login : current user : " + user);
      //rememberAudioType();
      getPermissionsAndSetUser(user);
    } else {
      userNotification.showLogin();
    }
  }

/*  private void console(String message) {
    int ieVersion = BrowserCheck.getIEVersion();
    if (ieVersion == -1 || ieVersion > 9) {
     // consoleLog(message);
      logger.info(message);
    }
  }*/
/*

  private native static void consoleLog(String message) */
/*-{
      console.log("UserManager:" + message);
  }-*//*
;
*/


  /**
   * TODOx : instead have call to get permissions for a user.
   *
   * @param user
   * @see #login()
   * @see #storeUser
   */
  private void getPermissionsAndSetUser(final int user) {
    //console("getPermissionsAndSetUser : " + user);
    // logger.info("UserManager.getPermissionsAndSetUser " + user + " asking server for info...");
    service.getUserBy(user, new AsyncCallback<User>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(User result) {
//        logger.info("UserManager.getPermissionsAndSetUser : onSuccess " + user + " : " + result);
//        if (loginType == PropertyHandler.LOGIN_TYPE.ANONYMOUS && result.getUserKind() != User.Kind.ANONYMOUS) {
//          clearUser();
//          addAnonymousUser();
//        } else
//
        if (result == null || //loginType != PropertyHandler.LOGIN_TYPE.ANONYMOUS &&
            getUserKind(result) == User.Kind.ANONYMOUS) {
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
   * <p>
   * Legacy people must get approval.
   *
   * @param result
   * @see #storeUser
   */
  private void gotNewUser(User result) {
//    logger.info("UserManager.gotNewUser " + result);
    userNotification.getPermissions().clear();
    if (result != null) {
      boolean isCD = getUserKind(result) == User.Kind.CONTENT_DEVELOPER;
      for (User.Permission permission : result.getPermissions()) {
        boolean valid = true;
        if (permission == User.Permission.QUALITY_CONTROL ||
            permission == User.Permission.RECORD_AUDIO) valid = isCD;
        if (valid) {
          userNotification.setPermission(permission, true);
        }
      }
      isMale = result.isMale();
      isTeacher = ((this.userKind = getUserKind(result)) == User.Kind.TEACHER) || isCD;
      isAdmin = result.isAdmin();
      userNotification.gotUser(result);
    }
    //console("getPermissionsAndSetUser.onSuccess : " + user);
  }

  private User.Kind getUserKind(User result) {
    return result.getUserKind();
  }

  public User.Kind getUserKind() {
    return userKind;
  }

  /**
   * So if there's a current user, ask the server about them.
   * If not add a new anonymous user.
   *
   * @see #checkLogin()
   * @see mitll.langtest.client.LangTest#checkLogin
   */

  private void anonymousLogin() {
    int user = getUser();
    if (user != NO_USER_SET) {
      //logger.info("UserManager.anonymousLogin : current user : " + user);
      //rememberAudioType(); // TODO : necessary?
      getPermissionsAndSetUser(user);
    } else {
      logger.info("UserManager.anonymousLogin : make new user, since user = " + user);
      addAnonymousUser();
    }
  }

  /**
   * This is useful in headstart context.
   */
  private void addAnonymousUser() {
    logger.info("UserManager.addAnonymousUser : adding anonymous user");

    service.addUser("anonymous", "", "", User.Kind.ANONYMOUS, Window.Location.getHref(), "", true, 0, "unknown", false, "browser", new AsyncCallback<User>() {
      @Override
      public void onFailure(Throwable caught) {

      }

      @Override
      public void onSuccess(User result) {
        setDefaultControlValues((int) result.getId());
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
   *
   * @return
   * @see mitll.langtest.client.InitialUI#getGreeting
   */
  public String getUserID() {
    if (Storage.isLocalStorageSupported()) {
      return Storage.getLocalStorageIfSupported().getItem(getUserChosenID());
    } else {
      return userChosenID;
    }
  }

/*  private void rememberAudioType() {
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();

      String audioType = localStorageIfSupported.getItem(getAudioType());
      if (audioType == null) {
        audioType = Result.AUDIO_TYPE_FAST_AND_SLOW;
      }
     // userNotification.rememberAudioType(audioType);
    }
  }*/

  /**
   * @return id of user
   * @see mitll.langtest.client.LangTest#getUser
   */
  public int getUser() {
    if (Storage.isLocalStorageSupported()) {
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
    return (sid == null || sid.equals(NO_USER_SET_STRING)) ||
        checkUserExpired(sid);
  }

  private String getUserFromStorage() {
    Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
    String userIDCookie = getUserIDCookie();
    return localStorageIfSupported != null ? localStorageIfSupported.getItem(userIDCookie) : NO_USER_SET_STRING;
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
  private String getUserIDCookie() {
    return appTitle + ":" + USER_ID;
  }

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
   * @see mitll.langtest.client.InitialUI#resetState()
   */
  public void clearUser() {
    clearCookieState();
  }

  private void clearCookieState() {
   // userNotification.rememberAudioType(Result.AUDIO_TYPE_UNSET);
/*
    if (USE_COOKIE) {
      Cookies.setCookie("sid", "" + NO_USER_SET);
    } else
*/
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();

      localStorageIfSupported.removeItem(getUserIDCookie());
      localStorageIfSupported.removeItem(getUserChosenID());
     // logger.info("clearUser : removed item " + getUserID() + " user now " + getUser());
    } else {
      userID = NO_USER_SET;
    }
  }

  /**
   * @param user
   * @param audioType
   * @see mitll.langtest.client.user.UserPassLogin#storeUser(mitll.langtest.shared.User)
   */
  void storeUser(User user, String audioType) {
    //logger.info("storeUser : user now " + user + " audio type '" + audioType + "'");
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
      logger.info("storeUser : user now " + user.getId() + " / " + getUser() + " audio '" + audioType +
          "' expires in " + (DURATION / 1000) + " seconds");
     // userNotification.rememberAudioType(audioType);

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
  }

  private long getUserSessionEnd() {
    return getUserSessionEnd(getUserSessionDuration());
  }

  private long getUserSessionEnd(long offset) {
    return System.currentTimeMillis() + offset;
  }

  /**
   * If we have lots of students moving through stations quickly, we want to auto logout once a day, once an hour?
   * <p>
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

  public boolean isAdmin() {
    return isAdmin;
  }
}
