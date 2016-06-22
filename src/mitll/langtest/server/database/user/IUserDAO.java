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
import mitll.langtest.shared.MiniUser;
import mitll.langtest.shared.User;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IUserDAO {
  int getDefectDetector();

  Database getDatabase();

  User addUser(String userID, String passwordH, String emailH, User.Kind kind, String ipAddr,
               boolean isMale, int age, String dialect, String device);

  int addUser(int age, String gender, int experience, String ipAddr,
               String trueIP, String nativeLang, String dialect, String userID, boolean enabled,
               Collection<User.Permission> permissions,
               User.Kind kind, String passwordH, String emailH, String device);

  boolean enableUser(int id);

  boolean changeEnabled(int userid, boolean enabled);

  Integer getIDForUserAndEmail(String user, String emailH);

  int getIdForUserID(String id);

  User getUser(String id, String passwordHash);

  User getUserWithPass(String id, String passwordHash);

  User getUserByID(String id);

  User getUserWhere(int userid);


  List<User> getUsers();

  List<User> getUsersDevices();

  Map<Integer, MiniUser> getMiniUsers();

  MiniUser getMiniUser(int userid);

  Map<Integer, User> getUserMap(boolean getMale);
  Collection<Integer> getUserIDs(boolean getMale);

  Map<Integer, User> getUserMap();

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#forgotUsername(String, String, String)
   * @param emailH
   * @return
   */
  String isValidEmail(String emailH);

  boolean changePassword(int user, String passwordH);

  User getUserWithResetKey(String key);

  User getUserWithEnabledKey(String key);

  boolean updateKey(int userid, boolean resetKey, String key);

  boolean clearKey(int userid, boolean resetKey);

  int getBeforeLoginUser();
}
