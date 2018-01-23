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

import mitll.hlt.domino.shared.common.SResult;
import mitll.hlt.domino.shared.model.user.ClientUserDetail;
import mitll.hlt.domino.shared.model.user.DBUser;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.IDAO;
import mitll.langtest.server.services.UserServiceImpl;
import mitll.langtest.shared.user.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IUserDAO extends IDAO, AutoCloseable {
  /**
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
   */
  void ensureDefaultUsers();

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
   */
  int getDefectDetector();

  /**
   * @return
   * @see mitll.langtest.server.database.custom.UserListManager#getRefAudioPath
   */
  Database getDatabase();

  /**
   * @param user
   * @return
   * @see UserManagement#addUser(SignUpUser)
   */
  User addUser(SignUpUser user);

  /**
   * @param user
   * @param emailH
   * @return
   * @see mitll.langtest.server.mail.EmailHelper#resetPassword(String, String, String)
   * @see mitll.langtest.server.rest.RestUserManagement#resetPassword(String, String, String)
   */
  Integer getIDForUserAndEmail(String user, String emailH);

  String getNameForEmail(String emailH);

  /**
   * @param userId
   * @param attemptedPassword
   * @param userAgent
   * @param remoteAddr
   * @param sessionID
   * @return
   * @see UserServiceImpl#loginUser
   */
  User loginUser(String userId,
                 String attemptedPassword,
                 String userAgent,
                 String remoteAddr,
                 String sessionID);

  boolean isKnownUser(String userid);

  /**
   * @param id
   * @return
   * @see mitll.langtest.server.database.copy.UserCopy#copyUsers
   * @see mitll.langtest.server.rest.RestUserManagement#tryToLogin
   */
  User getUserByID(String id);

  DBUser getDBUser(String userID);

  /**
   * TODO : replace with user where or rename
   *
   * @param id
   * @return
   * @see DatabaseImpl#getUserHistoryForList(int, Collection, int, Collection, Map, String)
   */
  User getByID(int id);

  /**
   * @param userid
   * @return
   * @see mitll.langtest.server.ScoreServlet#getUser
   */
  User getUserWhere(int userid);

  /**
   * @return
   * @see UserManagement#getUsers
   */
  List<User> getUsers();

  ReportUsers getReportUsers();

  /**
   * @return
   * @see mitll.langtest.server.database.audio.AudioDAO#getResultsForQuery(Connection, PreparedStatement)
   * @deprecated - don't get all the users
   */
  Map<Integer, MiniUser> getMiniUsers();

  /**
   * @param userid
   * @return
   * @see mitll.langtest.server.database.audio.BaseAudioDAO#getAudioAttribute
   */
  MiniUser getMiniUser(int userid);


  Map<Integer, FirstLastUser> getFirstLastFor(Collection<Integer> userDBIds);

  String getUserChosenID(int userid);

  boolean isMale(int userid);

  /**
   * @param emailH
   * @return
   * @see mitll.langtest.server.services.UserServiceImpl#forgotUsername
   */
  String isValidEmail(String emailH);

  /**
   * @param user
   * @param newHashPassword
   * @param baseURL
   * @return
   * @@deprecated
   * @see mitll.langtest.server.rest.RestUserManagement#changePFor
   */
  boolean changePassword(int user, String newHashPassword, String baseURL);

  /**
   * @param user
   * @param currentHashPassword
   * @param newHashPassword
   * @param baseURL
   * @return
   * @see UserServiceImpl#changePasswordWithCurrent(String, String)
   */
  boolean changePasswordWithCurrent(int user, String currentHashPassword, String newHashPassword, String baseURL);

  /**
   * @param userId
   * @param userKey
   * @param newPassword
   * @param url
   * @return
   * @see
   */
  boolean changePasswordForToken(String userId, String userKey, String newPassword, String url);

  SResult<ClientUserDetail> updateUser(DBUser dbUser);

  boolean forgotPassword(String user, String url);

  /**
   * @param key
   * @return
   * @@deprecated
   */
  User getUserWithResetKey(String key);

  /**
   * @return
   * @see DatabaseImpl#initializeDAOs
   */
  int getBeforeLoginUser();

  int getDefaultUser();

  int getDefaultMale();

  int getDefaultFemale();

  int getImportUser();

  /**
   * @param toUpdate
   * @see UserServiceImpl#addUser
   */
  void update(User toUpdate);

  void close() throws Exception;

  boolean isStudent(int userIDFromSessionOrDB);

  DBUser getDominoAdminUser();

  class ReportUsers {
    private List<ReportUser> allUsers;
    private List<ReportUser> deviceUsers;

    public ReportUsers(List<ReportUser> allUsers, List<ReportUser> deviceUsers) {
      this.allUsers = allUsers;
      this.deviceUsers = deviceUsers;
    }

    public List<ReportUser> getAllUsers() {
      return allUsers;
    }

    public List<ReportUser> getDeviceUsers() {
      return deviceUsers;
    }
  }
}
