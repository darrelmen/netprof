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

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import mitll.langtest.client.initial.InitialUI;
import mitll.langtest.client.user.*;
import mitll.langtest.shared.user.ChoosePasswordResult;
import mitll.langtest.shared.user.LoginResult;
import mitll.langtest.shared.user.SignUpUser;
import mitll.langtest.shared.user.User;

@RemoteServiceRelativePath("open-user-manager")
public interface OpenUserService extends RemoteService {
  /**
   * OPEN call - this is how we get a session.
   * @param userId
   * @param attemptedFreeTextPassword
   * @return
   * @see SignInForm#gotLogin
   */
  LoginResult loginUser(String userId, String attemptedFreeTextPassword);

  /**
   * Open call. No session - creates a session.
   * @param userId
   * @param userKey
   * @param newPassword
   * @return
   * @see ResetPassword#onChangePassword
   */
  ChoosePasswordResult changePasswordWithToken(String userId, String userKey, String newPassword);

  /**
   * Open call - no session.
   * @param userid
   * @return
   * @see SendResetPassword#onChangePassword
   */
  boolean resetPassword(String userid);

  /**
   * Open call - no session.
   * @param emailH
   * @param email
   * @return
   * @see UserPassLogin#getForgotUser
   */
  boolean forgotUsername(String emailH, String email);

  /**
   * No session created - we need to do set password via email first.
   * @param user
   * @param url
   * @return
   * @see SignUpForm#gotSignUp
   */
  LoginResult addUser(SignUpUser user, String url);

  boolean isKnownUser(String id);
  boolean isKnownUserWithEmail(String id);
  boolean isValidUser(String id);

  /**
   * @param projectid
   * @return
   * @see mitll.langtest.client.project.ProjectChoices#reallySetTheProject
   */
  User setProject(int projectid);

  /**
   * @see InitialUI#chooseProjectAgain
   */
  void forgetProject();

  boolean setCurrentUserToProject(int projid);
}
