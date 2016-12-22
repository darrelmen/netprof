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

import com.github.gwtbootstrap.client.ui.Container;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.InitialUI;
import mitll.langtest.client.domino.user.ChangePasswordView;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.client.user.UserPassLogin;
import mitll.langtest.shared.user.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@RemoteServiceRelativePath("user-manager")
public interface UserService extends RemoteService {
  User getUserFromSession();

  /**
   * @param login
   * @param freeTextPassword
   * @return
   * @see mitll.langtest.client.user.SignInForm#gotLogin
   */
  //User userExists(String login, String freeTextPassword);

  /**
   * @see mitll.langtest.client.user.UserManager#getPermissionsAndSetUser(String, String)
   * @param userId
   * @param attemptedHashedPassword - hashed - don't send clear password
   * @param attemptedFreeTextPassword
   * @return
   */
  LoginResult loginUser(String userId, String attemptedHashedPassword, String attemptedFreeTextPassword);

  void logout(String login);

  /**
   * @paramz token
   * @paramz newHashedPassword
   * @return
   * @see mitll.langtest.client.user.ResetPassword#onChangePassword
   */
 // boolean changePFor(String token, String newHashedPassword);

  /**
   * @see ChangePasswordView#changePassword
   * @param userid
   * @param currentHashedPassword
   * @param newHashedPassword
   * @return
   */
  boolean changePasswordWithCurrent(int userid, String currentHashedPassword, String newHashedPassword);

  /**
   * @see mitll.langtest.client.user.ResetPassword#onChangePassword
   * @param userId
   * @param userKey
   * @param newPassword
   * @return
   */
  User changePasswordWithToken(String userId, String userKey, String newPassword);

  /**
   * @param userid
   * @param url
   * @param emailForLegacy
   * @return
   * @see mitll.langtest.client.user.SignInForm#onSendReset
   */
  boolean resetPassword(String userid, String url, String emailForLegacy);

  /**
   * @deprecated no tokens anymore
   * @param token
   * @return
   * @see InitialUI#handleResetPass(Container, Panel, EventRegistration, String)
   */
  long getUserIDForToken(String token);

  /**
   * @param userid
   * @param enabled
   * @see mitll.langtest.client.user.UserTable#addAdminCol
   */
  void changeEnabledFor(int userid, boolean enabled);

  /**
   * @param emailH
   * @param email
   * @param url
   * @return
   * @see UserPassLogin#getForgotUser()
   */
  boolean forgotUsername(String emailH, String email, String url);




  /**
   * @see mitll.langtest.client.user.SignUpForm#gotSignUp
   * @param user
   * @param url
   * @return
   */
  User addUser(SignUpUser user, String url);

  /**
   * @return
   * @see mitll.langtest.client.user.UserTable#showDialog
   */
  List<User> getUsers();

  User getUser(int id);

  User getUserByID(String id);

  /**
   * @param cdToken
   * @param emailR
   * @param url
   * @return
   * @deprecated not sure if we're doing this anymore
   * @see InitialUI#handleCDToken(Container, Panel, String, String)
   */
  String enableCDUser(String cdToken, String emailR, String url);

  /**
   * @see InitialUI#setProjectForUser(int)
   * @param projectid
   * @return
   */
  User setProject(int projectid);

  void forgetProject();



  @Deprecated
  void update(User user, int changingUser);

  @Deprecated
  Collection<Invitation> getPending(User.Kind requestRole);

  @Deprecated
  void invite(String url, Invitation invite);

  @Deprecated
  Map<String, Integer> getInvitationCounts(User.Kind requestRole);

  @Deprecated
  Map<User.Kind, Integer> getCounts();

  @Deprecated
  Map<User.Kind, Collection<MiniUser>> getKindToUser();
}
