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
import com.github.gwtbootstrap.client.ui.Fieldset;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.InitialUI;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.client.user.UserPassLogin;
import mitll.langtest.shared.user.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static mitll.langtest.shared.user.User.Kind;

@RemoteServiceRelativePath("user-manager")
public interface UserService extends RemoteService {
  User setProject(int projectid);
  User addUser(SignUpUser user, String url
               //    , boolean isCD
  );

  /**
   * @return
   * @see mitll.langtest.client.user.UserTable#showDialog
   */
  List<User> getUsers();

  Map<User.Kind, Integer> getCounts();

  Map<User.Kind, Collection<MiniUser>> getKindToUser();

  /**
   * @param login
   * @param passwordH
   * @return
   * @see mitll.langtest.client.user.SignInForm#gotLogin
   */
  User userExists(String login, String passwordH);

  /**
   * @see mitll.langtest.client.user.UserManager#getPermissionsAndSetUser(String, String)
   * @param userId
   * @param attemptedPassword - hashed - don't send clear password
   * @return
   */
  LoginResult loginUser(String userId, String attemptedPassword);

  void logout(String login);

  /**
   * @param token
   * @param passwordH
   * @return
   * @see mitll.langtest.client.user.ResetPassword#getChangePasswordButton(String, Fieldset, BasicDialog.FormField, BasicDialog.FormField)
   */
  boolean changePFor(String token, String passwordH);

  boolean changePassword(int userid, String currentPasswordH, String passwordH);

  /**
   * @param token
   * @return
   * @see InitialUI#handleResetPass(Container, Panel, EventRegistration, String)
   */
  long getUserIDForToken(String token);

  /**
   * @param userid
   * @param enabled
   * @see mitll.langtest.client.user.UserTable#addAdminCol(LangTestDatabaseAsync, CellTable)
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
   * @param userid
   * @param text
   * @param url
   * @return
   * @see UserPassLogin#getForgotPassword()
   */
  boolean resetPassword(String userid, String text, String url);

  /**
   * @param cdToken
   * @param emailR
   * @param url
   * @return
   * @see InitialUI#handleCDToken(Container, Panel, String, String)
   */
  String enableCDUser(String cdToken, String emailR, String url);

  void forgetProject();

  User getUser(int id);

  void update(User user, int changingUser);

  Collection<Invitation> getPending(User.Kind requestRole);

  void invite(String url, Invitation invite);

  Map<String, Integer> getInvitationCounts(User.Kind requestRole);
}
