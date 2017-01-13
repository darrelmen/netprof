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
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.services.UserServiceAsync;
import mitll.langtest.shared.user.LoginResult;
import mitll.langtest.shared.user.MiniUser;
import mitll.langtest.shared.user.User;

import java.util.Collection;
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

  private static final boolean DEBUG = false;

  private static final long HOUR_IN_MILLIS = 1000 * 60 * 60;

  private static final int DAY_HOURS = 24;
  private static final long WEEK_HOURS = DAY_HOURS * 7;

  private static final long EXPIRATION_HOURS = 52 * WEEK_HOURS * HOUR_IN_MILLIS;

  private static final int NO_USER_SET = -1;
  private static final String NO_USER_SET_STRING = "" + NO_USER_SET;

  private static final String USER_ID = "userID";
  private static final String USER_CHOSEN_ID = "userChosenID";
  private static final String USER_PENDING_ID = "userPendingID";

  private final UserServiceAsync userServiceAsync;
  private final UserNotification userNotification;
  private long userID = NO_USER_SET;
  private String userChosenID = "";

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
   * @see mitll.langtest.client.LangTest#checkLogin()
   */
  public void checkLogin() {
    final int user = getUser();
    if (user != NO_USER_SET) {
      //logger.info("UserManager.login : current user : " + user);
      //console("UserManager.login : current user : " + user);
      if (current == null) {
        getPermissionsAndSetUser(
            /*getUserChosenFromStorage()*/);//, getPassFromStorage());
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
//  private void getPermissionsAndSetUser() {
//    getPermissionsAndSetUser(getUserChosenFromStorage(), getPassFromStorage());
//  }
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
   * instead have call to get permissions for a user.
   *
   * @see #checkLogin
   * @see #storeUser
   */
  private void getPermissionsAndSetUser() {
    if (DEBUG) logger.info("UserManager.getPermissionsAndSetUser " +
        " asking server for info...");

    userServiceAsync.getUserFromSession(new AsyncCallback<User>() {
      @Override
      public void onFailure(Throwable caught) {

      }

      @Override
      public void onSuccess(User result) {
        if (DEBUG) {
          logger.info("UserManager.getPermissionsAndSetUser : onSuccess " +
              //user +
              " : " + result);
        }
        if (result == null ||
            !result.isEnabled() ||
            !result.isValid() ||
            ((result.getPermissions().contains(User.Permission.RECORD_AUDIO) ||
                result.getPermissions().contains(User.Permission.DEVELOP_CONTENT)) &&
                result.getRealGender() == MiniUser.Gender.Unspecified)
            ) {
          clearUser();
          userNotification.showLogin();
        } else {
          setPendingUserStorage(result.getUserID());
          gotNewUser(result);
        }
      }
    });

    //console("getPermissionsAndSetUser : " + user);
   /* if (passwordHash == null) passwordHash = "";

    userServiceAsync.loginUser(user, passwordHash, "",
        //attemptedFreeTextPassword,
        new AsyncCallback<LoginResult>() {
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
        });*/
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

  public String getPendingUserID() {
    if (Storage.isLocalStorageSupported()) {
      return Storage.getLocalStorageIfSupported().getItem(getUserPendingID());
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
/*  public boolean isUserExpired() {
    String sid = getUserFromStorage();
    //   logger.info("sid from storage "+ sid);
    return (
        sid == null ||
            sid.equals(NO_USER_SET_STRING)) ||
        checkUserExpired(sid);
  }*/

  private String getUserFromStorage() {
    Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
    String userIDCookie = getUserIDCookie();
    return localStorageIfSupported != null ? localStorageIfSupported.getItem(userIDCookie) : NO_USER_SET_STRING;
  }

/*
  private String getPassFromStorage() {
    Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
    return localStorageIfSupported != null ? localStorageIfSupported.getItem(getPassCookie()) : NO_USER_SET_STRING;
  }
*/

 /*
  private String getUserChosenFromStorage() {
    Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
    String userChosenID = getUserChosenID();
    return localStorageIfSupported != null ? localStorageIfSupported.getItem(userChosenID) : NO_USER_SET_STRING;
  }
  */

  public void setPendingUserStorage(String pendingID) {
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
      localStorageIfSupported.setItem(getUserPendingID(), pendingID);
    }
  }

  /**
   * @param sid
   * @return
   * @seex #isUserExpired()
   */
/*  private boolean checkUserExpired(String sid) {
    boolean expired = false;
    if (userExpired(sid)) {
      logger.info("user expired " + sid);
      clearUser();
      expired = true;
    }
    return expired;
  }*/

  /**
   * Need these to be prefixed by app title so if we switch webapps, we don't get weird user ids
   *
   * @return
   */
  private String getUserIDCookie() {
    return appTitle + ":" + USER_ID;
  }

  /**
   * @return
   */
  private String getUserChosenID() {
    return appTitle + ":" + USER_CHOSEN_ID;
  }

  private String getUserPendingID() {
    return appTitle + ":" + USER_PENDING_ID;
  }

/*
  private String getExpires() {
    return appTitle + ":" + "expires";
  }
*/

  /**
   * @param sid
   * @see #getUser()
   */
/*
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
*/

  /**
   * @return
   * @see #userExpired(String)
   */
/*  private String getExpiresCookie() {
    Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
    return localStorageIfSupported.getItem(getExpires());
  }*/

  /**
   * @see mitll.langtest.client.InitialUI#resetState()
   */
  public void clearUser() {
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
      localStorageIfSupported.removeItem(getUserIDCookie());
      // localStorageIfSupported.removeItem(getPassCookie());
      localStorageIfSupported.removeItem(getUserChosenID());
      localStorageIfSupported.removeItem(getUserPendingID());
    //  logger.info("clearUser : removed item " + getUserID() + " user now " + getUser());
    } else {
      userID = NO_USER_SET;
    }
  }

  /**
   * don't store the password hash in local storage :)
   *
   * @param user
   * @see UserDialog#storeUser
   */
  void storeUser(User user) {
//    logger.info("storeUser : user now " + user);
    if (Storage.isLocalStorageSupported()) {
      rememberUser(user);
      gotNewUser(user);
    } else {  // not sure what we could possibly do here...
      userID = user.getID();
      userNotification.gotUser(user);
    }
  }

  public void rememberUser(User user) {
   // final long DURATION = getUserSessionDuration();
    //long futureMoment = getUserSessionEnd(DURATION);

    Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
    userChosenID = user.getUserID();
    localStorageIfSupported.setItem(getUserIDCookie(), "" + user.getID());
    //  localStorageIfSupported.setItem(getPassCookie(), passwordHash);
    localStorageIfSupported.setItem(getUserChosenID(), "" + userChosenID);
  //  rememberUserSessionEnd(localStorageIfSupported, futureMoment);
    // localStorageIfSupported.setItem(getLoginType(), "" + userType);
    //logger.info("storeUser : user now " + user.getID() + " / " + getUser() + "' expires in " + (DURATION / 1000) + " seconds");
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
   * @paramx futureMoment
   * @see #userExpired(String)
   */
/*  private void rememberUserSessionEnd(long futureMoment) {
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
      rememberUserSessionEnd(localStorageIfSupported, futureMoment);
    }
  }*/

  /**
   * @paramx localStorageIfSupported
   * @paramx futureMoment
   * @see #storeUser
   * @seex #rememberUserSessionEnd(long)
   */
/*
  private void rememberUserSessionEnd(Storage localStorageIfSupported, long futureMoment) {
    localStorageIfSupported.setItem(getExpires(), "" + futureMoment);
  }
*/

/*
  private long getUserSessionEnd() {
    return getUserSessionEnd(getUserSessionDuration());
  }
*/

/*
  private long getUserSessionEnd(long offset) {
    return System.currentTimeMillis() + offset;
  }
*/

  /**
   * If we have lots of students moving through stations quickly, we want to auto logout once a day, once an hour?
   * <p>
   * Egyptian should never time out -- for anonymous students
   *
   * @return one year for anonymous
   */
/*  private long getUserSessionDuration() {
    long mult =*//* loginType.equals(PropertyHandler.LOGIN_TYPE.ANONYMOUS) ? 52 :*//* 4;
    return EXPIRATION_HOURS * mult;
  }*/

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
    return getPermissions().contains(permission);
  }

  public Collection<User.Permission> getPermissions() {
    return current.getPermissions();
  }
}
