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
import mitll.langtest.client.domino.user.ChangePasswordView;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.user.ChoosePasswordResult;
import mitll.langtest.shared.user.LoginResult;
import mitll.langtest.shared.user.SignUpUser;
import mitll.langtest.shared.user.User;

import java.util.List;

public interface UserServiceAsync {
  /**
   * @param userId
   * @param attemptedFreeTextPassword
   * @param async
   */
  void loginUser(String userId, String attemptedFreeTextPassword, AsyncCallback<LoginResult> async);

  /**
   * @param currentHashedPassword
   * @param newHashedPassword
   * @param async
   * @see ChangePasswordView#changePassword
   */
  void changePasswordWithCurrent(String currentHashedPassword,
                                 String newHashedPassword,
                                 AsyncCallback<Boolean> async);

  void changePasswordWithToken(String userId, String userKey, String newPassword, AsyncCallback<ChoosePasswordResult> async);

  void resetPassword(String userid, AsyncCallback<Boolean> asyncCallback);

  /**
   * No real need to pass this in
   *
   * @param async
   */
  void logout(AsyncCallback<Void> async);

  void addUser(
      SignUpUser user,
      String url,
      AsyncCallback<LoginResult> async);

  void forgotUsername(String emailH, String email, AsyncCallback<Boolean> async);

  void setProject(int projectid, AsyncCallback<User> async);

  void forgetProject(AsyncCallback<Void> async);

  void getUserByID(String id, AsyncCallback<User> async);
  void isKnownUser(String id, AsyncCallback<Boolean> async);
  void isValidUser(String id, AsyncCallback<Boolean> async);

  /**
   * @param async
   * @see UserManager#getPermissionsAndSetUser
   */
  void getUserFromSession(AsyncCallback<User> async);

  /**
   * No user session needed.
   * @param id
   * @param async
   */
  void isKnownUserWithEmail(String id, AsyncCallback<Boolean> async);
}
