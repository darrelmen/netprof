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
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.services.UserServiceAsync;
import mitll.langtest.shared.user.LoginResult;
import mitll.langtest.shared.user.User;

import java.util.Date;
import java.util.Map;
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
  private static final boolean DEBUG = false;

  private static final int DAY_HOURS = 24;
  private static final long WEEK_HOURS = DAY_HOURS * 7;

  private static final long EXPIRATION_HOURS = 52 * WEEK_HOURS * HOUR_IN_MILLIS;

  private static final int NO_USER_SET = -1;
  private static final String NO_USER_SET_STRING = "" + NO_USER_SET;

  private static final String USER_ID = "userID";
  private static final String USER_CHOSEN_ID = "userChosenID";
  // private static final String AUDIO_TYPE = "audioType";

  private final UserServiceAsync userServiceAsync;
  private final UserNotification userNotification;
  private long userID = NO_USER_SET;
  private String userChosenID = "";

  //  private final PropertyHandler.LOGIN_TYPE loginType;
  private final String appTitle;
  private User current;

  /**
   * @param lt
   * @param userServiceAsync
   * @param props
   * @see mitll.langtest.client.LangTest#onModuleLoad2()
   */
  public UserManager(UserNotification lt, UserServiceAsync userServiceAsync, PropertyHandler props) {
    this.userNotification = lt;
    this.userServiceAsync = userServiceAsync;
    // this.props = props;
    //  this.loginType = props.getLoginType();
    this.appTitle = props.getAppTitle();
  }

  public UserServiceAsync getUserService() {
    return userServiceAsync;
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
    //  if (loginType.equals(PropertyHandler.LOGIN_TYPE.ANONYMOUS)) { // explicit setting of login type
    //     anonymousLogin();
    //  } else {
    login();
    //}
  }

  /**
   * TODO : if we have a user id from any site, try to use it to log in to this site.
   *
   * @see #checkLogin()
   */
  private void login() {
    final int user = getUser();
    if (user != NO_USER_SET) {
      //logger.info("UserManager.login : current user : " + user);
      //console("UserManager.login : current user : " + user);
      if (current == null) {
        getPermissionsAndSetUser();
      } else {
        logger.info("user " + user + " and full info " + current);
      }
    } else {
      userNotification.showLogin();
    }
  }

  /**
   * @see #login
   */
  private void getPermissionsAndSetUser() {
    getPermissionsAndSetUser(getUserChosenFromStorage(), getPassFromStorage());
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
   * @param passwordHash
   * @see #login()
   * @see #storeUser
   * @see #getPermissionsAndSetUser()
   */
  private void getPermissionsAndSetUser(final String user, String passwordHash) {
    //console("getPermissionsAndSetUser : " + user);
    if (DEBUG || true) logger.info("UserManager.getPermissionsAndSetUser " + user + " asking server for info...");
    if (passwordHash == null) passwordHash = "";

    userServiceAsync.loginUser(user, passwordHash, new AsyncCallback<LoginResult>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(LoginResult result) {
        if (DEBUG) logger.info("UserManager.getPermissionsAndSetUser : onSuccess " + user + " : " + result);
        if (result == null ||
            result.getResultType() != LoginResult.ResultType.Success
            ) {
          clearUser();
          userNotification.showLogin();
        } else {
          gotNewUser(result.getLoggedInUser());
        }
      }
    });
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

/*
  public int getUserPasswordHash() {
    if (Storage.isLocalStorageSupported()) {
      String sid = getPassFromStorage();
      return (sid == null || sid.equals("" + NO_USER_SET)) ? NO_USER_SET : Integer.parseInt(sid);
    } else {
      return (int) userID;
    }
  }
*/

  /**
   * @return
   * @see mitll.langtest.client.LangTest#checkLogin();
   */
  public boolean isUserExpired() {
    String sid = getUserFromStorage();
    //   logger.info("sid from storage "+ sid);
    return (
        sid == null ||
            sid.equals(NO_USER_SET_STRING)) ||
        checkUserExpired(sid);
  }

  private String getUserFromStorage() {
    Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
    String userIDCookie = getUserIDCookie();
    return localStorageIfSupported != null ? localStorageIfSupported.getItem(userIDCookie) : NO_USER_SET_STRING;
  }

  private String getPassFromStorage() {
    Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
    return localStorageIfSupported != null ? localStorageIfSupported.getItem(getPassCookie()) : NO_USER_SET_STRING;
  }

  private String getUserChosenFromStorage() {
    Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
    String userChosenID = getUserChosenID();
    return localStorageIfSupported != null ? localStorageIfSupported.getItem(userChosenID) : NO_USER_SET_STRING;
  }

  /**
   * @param sid
   * @return
   * @see #isUserExpired()
   */
  private boolean checkUserExpired(String sid) {
    boolean expired = false;
    if (userExpired(sid)) {
      logger.info("user expired " + sid);
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

  /**
   * @deprecated
   * @return
   */
  private String getPassCookie() {
    return appTitle + ":" + "pwd";
  }

  private String getUserChosenID() {
    return appTitle + ":" + USER_CHOSEN_ID;
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
/*
    if (USE_COOKIE) {
      Cookies.setCookie("sid", "" + NO_USER_SET);
    } else
*/
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();

      localStorageIfSupported.removeItem(getUserIDCookie());
      localStorageIfSupported.removeItem(getPassCookie());
      localStorageIfSupported.removeItem(getUserChosenID());
      logger.info("clearUser : removed item " + getUserID() + " user now " + getUser());
    } else {
      userID = NO_USER_SET;
    }
  }

  /**
   * TODO : do we store the password hash local storage???
   *
   * @param user
   * @paramx passwordHash
   * @seex SignInForm#foundExistingUser(User, boolean, String)
   * @see SignUpForm#gotSignUp(String, String, String, User.Kind)
   * @see UserDialog#storeUser
   */
  void storeUser(User user
                 //    , String passwordHash
  ) {
    logger.info("storeUser : user now " + user);

    final long DURATION = getUserSessionDuration();
    long futureMoment = getUserSessionEnd(DURATION);
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
      userChosenID = user.getUserID();
      localStorageIfSupported.setItem(getUserIDCookie(), "" + user.getID());
      //  localStorageIfSupported.setItem(getPassCookie(), passwordHash);
      localStorageIfSupported.setItem(getUserChosenID(), "" + userChosenID);
      rememberUserSessionEnd(localStorageIfSupported, futureMoment);
      // localStorageIfSupported.setItem(getLoginType(), "" + userType);
      logger.info("storeUser : user now " + user.getID() + " / " + getUser() + "' expires in " + (DURATION / 1000) + " seconds");

      gotNewUser(user);
    } else {  // not sure what we could possibly do here...
      userID = user.getID();
      userNotification.gotUser(user);
    }
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
    // logger.info("UserManager.gotNewUser " + result);
//    userNotification.getPermissions().clear();
    if (result != null) {
/*      for (User.Permission permission : result.getPermissions()) {
        boolean valid = true;
        if (permission == User.Permission.QUALITY_CONTROL ||
            permission == User.Permission.RECORD_AUDIO) {
          valid = result.isCD();
        }
        if (valid) {
          userNotification.setPermission(permission, true);
        }
      }*/
      this.current = result;
      // logger.info("\tgotNewUser current user " + current);
      userNotification.gotUser(result);
    }
    //console("getPermissionsAndSetUser.onSuccess : " + user);
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
    long mult =/* loginType.equals(PropertyHandler.LOGIN_TYPE.ANONYMOUS) ? 52 :*/ 4;
    return EXPIRATION_HOURS * mult;
  }

  public boolean isMale() {
    return current.isMale();
  }

  public boolean isTeacher() {
    return current.isTeacher() || current.isCD();
  }

  public boolean isAdmin() {
    return current.isAdmin();
  }

  public User getCurrent() {
    return current;
  }

  public boolean hasPermission(User.Permission permission) {
    return current.getPermissions().contains(permission);
  }

/*  public void getCounts(Map<User.Kind, Label> kindToLabel) {
    userServiceAsync.getCounts(new AsyncCallback<Map<User.Kind, Integer>>() {
      @Override
      public void onFailure(Throwable throwable) {

      }

      @Override
      public void onSuccess(Map<User.Kind, Integer> kindIntegerMap) {
        logger.info("got back " + kindIntegerMap);

        for (Map.Entry<User.Kind, Label> pair : kindToLabel.entrySet()) {
          Integer count = kindIntegerMap.get(pair.getKey());
          if (count != null) {
            pair.getValue().setText("" + count);
          } else {
            pair.getValue().setText("0");
          }
        }
      }
    });
  }*/

/*  public void getInvitationCounts(Map<String, Label> kindToLabel) {
    userServiceAsync.getInvitationCounts(current.getUserKind(),
        new AsyncCallback<Map<String, Integer>>() {
          @Override
          public void onFailure(Throwable throwable) {

          }

          @Override
          public void onSuccess(Map<String, Integer> kindIntegerMap) {
            logger.info("got back " + kindIntegerMap);

            for (Map.Entry<String, Label> pair : kindToLabel.entrySet()) {
              Integer count = kindIntegerMap.get(pair.getKey());
              if (count != null) {
                pair.getValue().setText("" + count);
              } else {
                pair.getValue().setText("0");
              }
            }
          }
        });
  }*/
}
