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

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.excel.UserDAOToExcel;
import mitll.langtest.shared.MiniUser;
import mitll.langtest.shared.User;
import net.sf.json.JSON;
import org.apache.log4j.Logger;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public abstract class BaseUserDAO extends DAO {
  private static final Logger logger = Logger.getLogger(BaseUserDAO.class);

  static final String DEFECT_DETECTOR = "defectDetector";
  public static final String USERS = "users";
  public static final String MALE = "male";
  public static final String FEMALE = "female";
  static final String PERMISSIONS = "permissions";
  static final String KIND = "kind";
  static final String PASS = "passwordH";
  static final String EMAIL = "emailH";
  static final String DEVICE = "device";
  static final String USER_ID = "userID";
  static final List<User.Permission> CD_PERMISSIONS = Arrays.asList(User.Permission.QUALITY_CONTROL, User.Permission.RECORD_AUDIO);
  static final List<User.Permission> EMPTY_PERM = new ArrayList<>();
  static final String ENABLED = "enabled";
  static final String RESET_PASSWORD_KEY = "resetPasswordKey";
  static final String ENABLED_REQ_KEY = "enabledReqKey";
  static final String NATIVE_LANG = "nativeLang";
  static final String UNKNOWN = "unknown";
  String language;
  int defectDetector;
  boolean enableAllUsers;

  static final String ID = "id";
  static final String AGE = "age";
  static final String GENDER = "gender";
  static final String EXPERIENCE = "experience";
  static final String IPADDR = "ipaddr";
  static final String DIALECT = "dialect";
  static final String TIMESTAMP = "timestamp";
  public static final int DEFAULT_USER_ID = -1;

  /**
   * After a default user has been marked male
   */
  public static final int DEFAULT_MALE_ID = -2;

  /**
   * After a default user has been marked female
   */
  public static final int DEFAULT_FEMALE_ID = -3;
  public static MiniUser DEFAULT_USER = new MiniUser(DEFAULT_USER_ID, 99, true, "default", false);

  public static MiniUser DEFAULT_MALE   = new MiniUser(DEFAULT_MALE_ID,   99, true, "Male", false);
  public static MiniUser DEFAULT_FEMALE = new MiniUser(DEFAULT_FEMALE_ID, 99, false, "Female", false);

  Collection<String> admins;
  
  BaseUserDAO(Database database) {
    super(database);
  }

  /**
   * @return
   * @see mitll.langtest.server.database.custom.UserListManager#addDefect(String, String, String)
   * @see mitll.langtest.server.database.custom.AnnotationDAO#AnnotationDAO
   */
  public int getDefectDetector() {
    return defectDetector;
  }

  public void toXLSX(OutputStream out, List<User> users) {
    new UserDAOToExcel().toXLSX(out, users, language);
  }

  public JSON toJSON(List<User> users) {
    return new UserDAOToExcel().toJSON(users);
  }

  /**
   * Check if the user exists already, and return null if so.
   * If it exists but is a legacy user, update its fields.
   *
   * @param userID
   * @param passwordH
   * @param emailH
   * @param kind
   * @param ipAddr
   * @param isMale
   * @param age
   * @param dialect
   * @param device
   * @return null if existing, valid user (email and password)
   * @see mitll.langtest.server.database.DatabaseImpl#addUser
   * @see UserManagement#addAndGetUser(String, String, String, User.Kind, boolean, int, String, String, String)
   */
  public User addUser(String userID, String passwordH, String emailH, User.Kind kind, String ipAddr,
                      boolean isMale, int age, String dialect, String device) {
    User userByID = getUserByID(userID);
    if (userByID != null && kind != User.Kind.ANONYMOUS) {
      // user exists!
      if (userByID.getEmailHash() != null && userByID.getPasswordHash() != null &&
          !userByID.getEmailHash().isEmpty() && !userByID.getPasswordHash().isEmpty()) {
        logger.debug(language + " : addUser : user " + userID + " is an existing user.");
        return null; // existing user!
      } else {
        updateUser(userByID.getId(), kind, passwordH, emailH);
        User userWhere = getUserWhere(userByID.getId());
        logger.debug(language + " : addUser : returning updated user " + userWhere);
        return userWhere;
      }
    } else {
      Collection<User.Permission> perms = (kind == User.Kind.CONTENT_DEVELOPER) ? CD_PERMISSIONS : EMPTY_PERM;
      boolean enabled = (kind != User.Kind.CONTENT_DEVELOPER) || isAdmin(userID) || enableAllUsers;

      int l = addUser(age, isMale ? MALE : FEMALE, 0, ipAddr, "", "", dialect, userID, enabled, perms, kind, passwordH, emailH, device);
      User userWhere = getUserWhere(l);
      logger.debug(language + " : addUser : added new user " + userWhere);

      return userWhere;
    }
  }

  abstract User getUserByID(String id);
  abstract void updateUser(int id, User.Kind kind, String passwordH, String emailH);
  abstract User getUserWhere(int userid);
  boolean isAdmin(String userid) {
    return userid != null && (admins.contains(userid));
  }

  abstract int addUser(int age, String gender, int experience, String userAgent,
                       String trueIP, String nativeLang, String dialect, String userID, boolean enabled, Collection<User.Permission> permissions,
                       User.Kind kind, String passwordH, String emailH, String device);
}
