/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.client.user;

import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.custom.KeyStorage;
import mitll.langtest.client.initial.InitialUI;
import mitll.langtest.client.services.UserServiceAsync;
import mitll.langtest.shared.user.MiniUser;
import mitll.langtest.shared.user.Permission;
import mitll.langtest.shared.user.SimpleUser;
import mitll.langtest.shared.user.User;

import java.util.Collection;
import java.util.logging.Logger;

public class UserManager {
  private final Logger logger = Logger.getLogger("UserManager");

  private static final boolean DEBUG = false;

  private static final int NO_USER_SET = -1;
  private final UserServiceAsync userServiceAsync;

  private final UserNotification userNotification;
  private final UserFeedback userFeedback;
  private int userID = NO_USER_SET;
  private String userChosenID = "";

  /**
   * @see #gotNewUser
   */
  private User current;
  private KeyStorage storage;

  /**
   * @param lt
   * @param userServiceAsync
   * @see mitll.langtest.client.LangTest#onModuleLoad2()
   */
  public UserManager(UserNotification lt,
                     UserFeedback userFeedback,
                     UserServiceAsync userServiceAsync,
                     KeyStorage storage) {
    this.userNotification = lt;
    this.userServiceAsync = userServiceAsync;
    this.userFeedback = userFeedback;
    this.storage = storage;
  }

  /**
   * @return
   * @see SignInForm#gotLogin
   */
  UserServiceAsync getUserService() {
    return userServiceAsync;
  }

  /**
   * @see mitll.langtest.client.LangTest#checkLogin
   */
  public void checkLogin() {
    final int user = getUser();

    if (DEBUG) logger.info("UserManager.checkLogin : current user : " + user);

    if (user != NO_USER_SET) {
      if (DEBUG) logger.info("\n\nUserManager.checkLogin : we have a current user : " + user);
      if (current == null) {
        getPermissionsAndSetUser();
      } else {
        if (DEBUG)
          logger.info("UserManager.checkLogin : user " + user + " and full info " + current.getUserID() + " " + current.getUserKind());
      }
    } else {
      userNotification.showLogin();
    }
  }

  /**
   * instead have call to get permissions for a user.
   *
   * @see #checkLogin
   * @see InitialUI#getUserPermissions()
   */
  public void getPermissionsAndSetUser() {
    if (DEBUG) {
      logger.info("UserManager.getPermissionsAndSetUser asking server for info...");
    }

    final long then = System.currentTimeMillis();
    userServiceAsync.getUserFromSession(new AsyncCallback<User>() {
      @Override
      public void onFailure(Throwable caught) {
        userFeedback.onFailure(caught, then);
      }

      @Override
      public void onSuccess(User result) {
        //   logger.info("took " + (System.currentTimeMillis()-then)  + " to get current user.");
        gotSessionUser(result);
      }
    });
  }

  /**
   * Kick the user out to the login screen if they're not valid in any way.
   *
   * @param result
   * @see #getPermissionsAndSetUser
   * @see #gotNewUser
   */
  private void gotSessionUser(User result) {
    if (DEBUG) {
      logger.info("UserManager.getPermissionsAndSetUser : onSuccess " + result);
    }
    if (result == null ||
        !result.isEnabled() ||
        !result.isValid() ||
        !result.isHasAppPermission() ||
        ((result.getPermissions().contains(Permission.RECORD_AUDIO) ||
            result.getPermissions().contains(Permission.DEVELOP_CONTENT)) &&
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
      return storage.getUserChosenID();
    } else {
      return userChosenID;
    }
  }

  String getPendingUserID() {
    if (Storage.isLocalStorageSupported()) {
      return storage.getUserPendingID();
    } else {
      return userChosenID;
    }
  }

  /**
   * So we only have a valid user id here if we've logged in.
   *
   * @return
   */
  public boolean hasUser() {
    return getUser() != -1;//!getUserID().isEmpty();
  }

  /**
   * @return id of user
   * @see mitll.langtest.client.LangTest#getUser
   */
  public int getUser() {
    if (Storage.isLocalStorageSupported()) {
      return storage.getUserID();
//      String sid = getUserFromStorage();
//      return (sid == null || sid.equals("" + NO_USER_SET)) ? NO_USER_SET : Integer.parseInt(sid);
    } else {
      return userID;
    }
  }

  void setPendingUserStorage(String pendingID) {
    storage.setPendingUserStorage(pendingID);
  }


  /**
   * don't store the password hash in local storage :)
   *
   * @param user
   * @see UserDialog#storeUser
   */
  void storeUser(User user) {
    if (Storage.isLocalStorageSupported()) {
      if (DEBUG) logger.info("storeUser : user now " + user);
      rememberUser(user);
      gotNewUser(user);
    } else {  // not sure what we could possibly do here...
      if (DEBUG) logger.info("storeUser : ??? user now " + user);
      userID = user.getID();
      userNotification.gotUser(user);
    }
  }

  /**
   * @param user
   * @see #storeUser
   */
  void rememberUser(SimpleUser user) {
    storage.rememberUser(userChosenID = user.getUserID(), user.getID());
//    storage.storeValue(getUserIDCookie(), "" + user.getID());
//    storage.storeValue(getUserChosenID(), "" + userChosenID);

    if (DEBUG) {
      logger.info("storeUser : user now " + user.getID() + " / " + getUser() +
          "\n\tstorage " + storage.getUserChosenID() + " " + storage.getUserID());
    }
  }

  /**
   * @see InitialUI#resetState()
   */
  public void clearUser() {
    if (Storage.isLocalStorageSupported()) {
      storage.clearUser();
//      storage.removeValue(getUserPendingID());
      current = null;
      if (DEBUG) logger.info("clearUser : removed user id = " + getUserID() + " user now " + getUser());
    } else {
      userID = NO_USER_SET;
    }
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
      setAbbreviation(result);
      this.current = result;
//      logger.info("gotNewUser ab " + abbreviation +
//          " current user " + current);
      userNotification.gotUser(result);
    } else {
      logger.warning("gotNewUser failed?");
    }
  }

  private void setAbbreviation(User result) {
    String first = result.getFirst();
    String last = result.getLast();

    boolean fvalid = !first.isEmpty() && !first.equalsIgnoreCase("First");
    boolean lvalid = !last.isEmpty() && !last.equalsIgnoreCase("Last");

    abbreviation = fvalid && lvalid ?
        first.substring(0, 1).toUpperCase() + last.substring(0, 1).toUpperCase() :
        getUserID();
  }

  private String abbreviation;

  public String getAbbreviation() {
    return abbreviation;
  }

  public boolean isMale() {
    return current.isMale();
  }

  public boolean isAdmin() {
    return current != null && current.isAdmin();
  }

  public User getCurrent() {
    return current;
  }

  public boolean hasPermission(Permission permission) {
    Collection<Permission> permissions = getPermissions();
    return permissions != null && permissions.contains(permission);
  }

  public Collection<Permission> getPermissions() {
    return current.getPermissions();
  }
}
