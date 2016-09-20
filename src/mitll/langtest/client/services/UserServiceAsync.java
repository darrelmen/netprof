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

import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.user.LoginResult;
import mitll.langtest.shared.user.MiniUser;
import mitll.langtest.shared.user.SignUpUser;
import mitll.langtest.shared.user.User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface UserServiceAsync {
  void getUsers(AsyncCallback<List<User>> async);

  void userExists(String login, String passwordH, AsyncCallback<User> async);

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
   //   boolean isCD,
      AsyncCallback<User> async);

  void resetPassword(String userid, String text, String url, AsyncCallback<Boolean> asyncCallback);

  void forgotUsername(String emailH, String email, String url, AsyncCallback<Boolean> async);

  void getUserIDForToken(String token, AsyncCallback<Long> async);

  void changePFor(String token, String first, AsyncCallback<Boolean> asyncCallback);

  void changeEnabledFor(int userid, boolean enabled, AsyncCallback<Void> async);

  void enableCDUser(String cdToken, String emailR, String url, AsyncCallback<String> asyncCallback);

  /**
   * @param userId
   * @param attemptedPassword
   * @param async
   * @see UserManager#getPermissionsAndSetUser
   */
  void loginUser(String userId, String attemptedPassword, AsyncCallback<LoginResult> async);

  void setProject(int projectid, AsyncCallback<User> async);

  void forgetProject(AsyncCallback<Void> async);

  void changePassword(int userid, String currentPasswordH, String newPasswordH, AsyncCallback<Boolean> async);

  void getCounts(AsyncCallback<Map<User.Kind, Integer>> async);

  void getKindToUser(AsyncCallback<Map<User.Kind, Collection<MiniUser>>> async);
}
