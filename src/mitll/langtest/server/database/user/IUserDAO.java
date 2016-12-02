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

package mitll.langtest.server.database.user;

import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.IDAO;
import mitll.langtest.server.database.Report;
import mitll.langtest.server.database.result.IResultDAO;
import mitll.langtest.server.services.UserServiceImpl;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.user.MiniUser;
import mitll.langtest.shared.user.SignUpUser;
import mitll.langtest.shared.user.User;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IUserDAO extends IDAO {
  /**
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
   */
  void ensureDefaultUsers();

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
   */
  int getDefectDetector();

  /**
   * @see mitll.langtest.server.database.custom.UserListManager#getRefAudioPath(int, int, File, String, boolean, String, String)
   * @return
   */
  @Deprecated Database getDatabase();

  /**
   * @see UserManagement#addUser(SignUpUser)
   * @param user
   * @return
   */
  User addUser(SignUpUser user);

  @Deprecated boolean enableUser(int id);

  /**
   * @see mitll.langtest.server.services.UserServiceImpl#changeEnabledFor
   * @param userid
   * @param enabled
   * @return
   */
  boolean changeEnabled(int userid, boolean enabled);

  /**
   * @see mitll.langtest.server.mail.EmailHelper#resetPassword(String, String, String)
   * @see mitll.langtest.server.rest.RestUserManagement#resetPassword(String, String, String)
   * @param user
   * @param emailH
   * @return
   */
  Integer getIDForUserAndEmail(String user, String emailH);

  /**
   * @see mitll.langtest.server.services.UserServiceImpl#loginUser(String, String)
   * @param id
   * @param passwordHash
   * @return
   */
  User getUser(String id, String passwordHash);
  User getUserFreeTextPassword(String id, String freeTextPassword);

  /**
   * @see mitll.langtest.server.database.copy.CopyToPostgres#copyUsers(DatabaseImpl, int, IResultDAO)
   * @param id
   * @param passwordHash
   * @return
   */
  User getStrictUserWithPass(String id, String passwordHash);
  User getStrictUserWithFreeTextPass(String id, String freeTextPassword);

  /**
   * @see mitll.langtest.server.database.copy.CopyToPostgres#copyUsers(DatabaseImpl, int, IResultDAO)
   * @param id
   * @return
   */
  User getUserByID(String id);

  /**
   * TODO : replace with user where or rename
   * @see DatabaseImpl#getUserHistoryForList(int, Collection, int, Collection, Map)
   * @param id
   * @return
   */
  User getByID(int id);

  /**
   * @see mitll.langtest.server.database.audio.BaseAudioDAO#getUserIDs
   * @param userid
   * @return
   */
  User getUserWhere(int userid);

  /**
   * @see UserManagement#getUsers
   * @return
   */
  List<User> getUsers();

  /**
   * @see Report#getReport
   * @return
   */
  List<User> getUsersDevices();

  /**
   * @see mitll.langtest.server.database.analysis.Analysis#getUserInfos(IUserDAO, Map)
   * @return
   */
  Map<Integer, MiniUser> getMiniUsers();

  /**
   * @see UserServiceImpl#getKindToUser
   * @return
   */
  @Deprecated  Map<User.Kind,Collection<MiniUser>> getMiniByKind();

  /**
   * @see mitll.langtest.server.database.audio.BaseAudioDAO#getAudioAttribute
   * @param userid
   * @return
   */
  MiniUser getMiniUser(int userid);

  /**
   * @see mitll.langtest.server.database.audio.BaseAudioDAO#getUserIDs(int)
   * @param getMale
   * @return
   */
  Map<Integer, User> getUserMap(boolean getMale);

  /**
   * @see mitll.langtest.server.database.audio.BaseAudioDAO#getUserIDs(int)
   * @param getMale
   * @return
   */
  Collection<Integer> getUserIDs(boolean getMale);

  /**
   * @see mitll.langtest.server.database.result.ResultDAO#getUserToResults(AudioType, IUserDAO)
   * @return
   */
  Map<Integer, User> getUserMap();

  /**
   * @param emailH
   * @return
   * @see mitll.langtest.server.services.UserServiceImpl#forgotUsername
   */
  String isValidEmail(String emailH);

  /**
   * @see UserServiceImpl#changePassword(int, String, String)
   * @param user
   * @param passwordH
   * @return
   */
  boolean changePassword(int user, String passwordH);

  /**
   * @see UserServiceImpl#changePFor(String, String)
   * @param key
   * @return
   */
  User getUserWithResetKey(String key);

 @Deprecated User getUserWithEnabledKey(String key);

  /**
   * @see mitll.langtest.server.mail.EmailHelper#resetPassword(String, String, String)
   * @param userid
   * @param resetKey
   * @param key
   * @return
   */
  boolean updateKey(int userid, boolean resetKey, String key);

  /**
   * @see UserServiceImpl#changePFor(String, String)
   * @param userid
   * @param resetKey
   * @return
   */
  boolean clearKey(int userid, boolean resetKey);

  /**
   * @see DatabaseImpl#initializeDAOs
   * @return
   */
  int getBeforeLoginUser();

  /**
   * @see UserServiceImpl#getCounts()
   * @return
   */
  Map<User.Kind,Integer> getCounts();

  /**
   * @see UserServiceImpl#update
   * @param toUpdate
   */
  void update(User toUpdate);
}
