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

package mitll.langtest.client.services;

import com.github.gwtbootstrap.client.ui.Fieldset;
import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.domino.user.ChangePasswordView;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.user.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface UserServiceAsync {

  /**
   * @see mitll.langtest.client.user.SignInForm#gotLogin(String, String, boolean)
   * @see mitll.langtest.client.user.SignInForm#makeSignInUserName(Fieldset)
   * @param login
   * @param passwordH
   * @param async
   */
 // void userExists(String login, String passwordH, AsyncCallback<User> async);

  /**
   * @param userId
   * @param attemptedFreeTextPassword
   * @param async
   * @see UserManager#getPermissionsAndSetUser
   * @see mitll.langtest.client.user.SignInForm#getPermissionsAndSetUser
   */
  void loginUser(String userId, String attemptedFreeTextPassword, AsyncCallback<LoginResult> async);

  /**
   * TODO : don't do a token - I guess it should be the userid?
   * @see mitll.langtest.client.user.ResetPassword#onChangePassword
   * @param token
   * @param newFreeTextPassword
   * @param asyncCallback
   */
  void changePFor(String token, String newFreeTextPassword, AsyncCallback<Boolean> asyncCallback);

  /**
   * @see ChangePasswordView#changePassword
   * @param userid
   * @param currentFreeTextPassword
   * @param newFreeTextPassword
   * @param async
   */
  void changePassword(int userid,
                      String currentFreeTextPassword,
                      String newFreeTextPassword,
                      AsyncCallback<Boolean> async);

  void resetPassword(String userid, String userEmail, String url, AsyncCallback<Boolean> asyncCallback);


  void getUsers(AsyncCallback<List<User>> async);

  void getUser(int id, AsyncCallback<User> async);


  /**
   * No real need to pass this in
   *
   * @param login
   * @param async
   */
  void logout(String login, AsyncCallback<Void> async);

  void addUser(
      SignUpUser user,
      String url,
      AsyncCallback<User> async);


  void forgotUsername(String emailH, String email, String url, AsyncCallback<Boolean> async);

  void getUserIDForToken(String token, AsyncCallback<Long> async);


  void changeEnabledFor(int userid, boolean enabled, AsyncCallback<Void> async);

  /**
   * @Deprecated - not doing this anymore...
   * @param cdToken
   * @param emailR
   * @param url
   * @param asyncCallback
   */
  void enableCDUser(String cdToken, String emailR, String url, AsyncCallback<String> asyncCallback);

  void setProject(int projectid, AsyncCallback<User> async);

  void forgetProject(AsyncCallback<Void> async);

  void getCounts(AsyncCallback<Map<User.Kind, Integer>> async);

  void getInvitationCounts(User.Kind requestRole, AsyncCallback<Map<String, Integer>> async);

  void getKindToUser(AsyncCallback<Map<User.Kind, Collection<MiniUser>>> async);

  void update(User user, int changingUser, AsyncCallback<Void> async);

  void getPending(User.Kind requestRole, AsyncCallback<Collection<Invitation>> async);

  void invite(String url, Invitation invite, AsyncCallback<Void> async);

  void getUserByID(String id, AsyncCallback<User> async);
}
