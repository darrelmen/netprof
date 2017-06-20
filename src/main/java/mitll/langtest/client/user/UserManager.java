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
import mitll.langtest.client.initial.InitialUI;
import mitll.langtest.client.initial.PropertyHandler;
import mitll.langtest.client.services.UserServiceAsync;
import mitll.langtest.shared.user.MiniUser;
import mitll.langtest.shared.user.User;

import java.util.Collection;
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
  private static final int NO_USER_SET = -1;
  private static final String NO_USER_SET_STRING = "" + NO_USER_SET;

  private static final String USER_ID = "userID";
  private static final String USER_CHOSEN_ID = "userChosenID";
  private static final String USER_PENDING_ID = "userPendingID";

  private final UserServiceAsync userServiceAsync;
  private final UserNotification userNotification;
  private final UserFeedback userFeedback;
  private long userID = NO_USER_SET;
  private String userChosenID = "";

  private final String appTitle;
  /**
   * @see #gotNewUser
   */
  private User current;

  /**
   * @param lt
   * @param userServiceAsync
   * @param props
   * @see mitll.langtest.client.LangTest#onModuleLoad2()
   */
  public UserManager(UserNotification lt,
                     UserFeedback userFeedback,
                     UserServiceAsync userServiceAsync, PropertyHandler props) {
    this.userNotification = lt;
    this.userServiceAsync = userServiceAsync;
    this.appTitle = props.getAppTitle();
    this.userFeedback = userFeedback;
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
   * instead have call to get permissions for a user.
   *
   * @see #checkLogin
   * @see #storeUser
   */
  private void getPermissionsAndSetUser() {
    if (DEBUG) logger.info("UserManager.getPermissionsAndSetUser " +
        " asking server for info...");
    final long then = System.currentTimeMillis();
    userServiceAsync.getUserFromSession(new AsyncCallback<User>() {
      @Override
      public void onFailure(Throwable caught) {
        userFeedback.onFailure(caught, then);
      }

      @Override
      public void onSuccess(User result) {
        gotSessionUser(result);
      }
    });
  }

  private void gotSessionUser(User result) {
    if (DEBUG) {
      logger.info("UserManager.getPermissionsAndSetUser : onSuccess " + result);
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

  /**
   * For display purposes
   *
   * @return
   * @see InitialUI#getGreeting
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

  public boolean hasUser() {
    return getUserID() != null;
  }

  private String getUserFromStorage() {
    Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
    String userIDCookie = getUserIDCookie();
    return localStorageIfSupported != null ? localStorageIfSupported.getItem(userIDCookie) : NO_USER_SET_STRING;
  }

  void setPendingUserStorage(String pendingID) {
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
      localStorageIfSupported.setItem(getUserPendingID(), pendingID);
    }
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
   * @return
   */
  private String getUserChosenID() {
    return appTitle + ":" + USER_CHOSEN_ID;
  }

  private String getUserPendingID() {
    return appTitle + ":" + USER_PENDING_ID;
  }

  /**
   * @see InitialUI#resetState()
   */
  public void clearUser() {
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
      localStorageIfSupported.removeItem(getUserIDCookie());
      localStorageIfSupported.removeItem(getUserChosenID());
      localStorageIfSupported.removeItem(getUserPendingID());

      logger.info("clearUser : removed user id = " + getUserID() + " user now " + getUser());
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
    if (Storage.isLocalStorageSupported()) {
      logger.info("storeUser : user now " + user);
      rememberUser(user);
      gotNewUser(user);
    } else {  // not sure what we could possibly do here...
      userID = user.getID();
      userNotification.gotUser(user);
    }
  }

  /**
   * @see #storeUser
   * @param user
   */
  void rememberUser(User user) {
    Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
    userChosenID = user.getUserID();
    localStorageIfSupported.setItem(getUserIDCookie(), "" + user.getID());
    localStorageIfSupported.setItem(getUserChosenID(), "" + userChosenID);
    //logger.info("storeUser : user now " + user.getID() + " / " + getUser() + "' expires in " + (DURATION / 1000) + " seconds");
  }

  /**
   * Only content developers can do quality control or record audio.
   * <p>
   * Legacy people must get approval.
   *
   * @param result
   * @see #storeUser
   * @see #gotSessionUser
   */
  private void gotNewUser(User result) {
    if (DEBUG) logger.info("UserManager.gotNewUser " + result);
    if (result != null) {
      this.current = result;
      // logger.info("\tgotNewUser current user " + current);
      userNotification.gotUser(result);
    }
  }

  public boolean isMale() {
    return current.isMale();
  }

  public boolean isTeacher() {
    return current.isTeacher() || current.isCD();
  }

  public boolean isAdmin() {
    return current != null && current.isAdmin();
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
