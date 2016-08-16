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
import mitll.langtest.server.database.annotation.AnnotationDAO;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.shared.user.MiniUser;
import mitll.langtest.shared.user.SignUpUser;
import mitll.langtest.shared.user.User;
import org.apache.log4j.Logger;

import java.util.*;

public abstract class BaseUserDAO extends DAO {
  private static final Logger logger = Logger.getLogger(BaseUserDAO.class);

  private static final String DEFECT_DETECTOR = "defectDetector";
  private static final String BEFORE_LOGIN_USER = "beforeLogin";
  private static final String IMPORT_USER = "importUser";
  public static final String USERS = "users";
  public static final String MALE = "male";
  public static final String FEMALE = "female";
  static final String PERMISSIONS = "EMPTY_PERMISSIONS";
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
  @Deprecated
  final String language;
  private int defectDetector, beforeLoginUser, importUser, defaultUser, defaultMale, defaultFemale;
  private final boolean enableAllUsers;

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
  public static final int UNDEFINED_USER = -5;
  public static MiniUser DEFAULT_USER = new MiniUser(DEFAULT_USER_ID, 99, true, "default", false);
  public static MiniUser DEFAULT_MALE = new MiniUser(DEFAULT_MALE_ID, 99, true, "Male", false);
  public static MiniUser DEFAULT_FEMALE = new MiniUser(DEFAULT_FEMALE_ID, 99, false, "Female", false);

  private final Collection<String> admins;

  BaseUserDAO(Database database) {
    super(database);
    admins = database.getServerProps().getAdmins();
    language = database.getServerProps().getLanguage();
    enableAllUsers = database.getServerProps().enableAllUsers();
  }

  /**
   * @return
   * @see IUserListManager#addDefect(int, String, String)
   * @see AnnotationDAO#AnnotationDAO
   * @param value
   */
  public int getDefectDetector(String value) {
    return defectDetector;
  }

  public int getBeforeLoginUser() {
    return beforeLoginUser;
  }

  public int getImportUser() {
    return importUser;
  }

  public int getDefaultUser() {
    return defaultUser;
  }

  public int getDefaultMale() {
    return defaultMale;
  }

  public int getDefaultFemale() {
    return defaultFemale;
  }

  /**
   * Check if the user exists already, and return null if so.
   * If it exists but is a legacy user, update its fields.
   *
   * @param userID
   * @param passwordH
   * @param emailH
   * @param email
   * @param kind
   * @param ipAddr
   * @param isMale
   * @param age
   * @param dialect
   * @param device
   * @param first
   * @param last      @return null if existing, valid user (email and password)
   * @see UserManagement#addAndGetUser
   */
/*  public User addUser(String userID, String passwordH, String emailH, String email,
                      User.Kind kind, String ipAddr,
                      boolean isMale, int age, String dialect, String device, String first, String last) {
    User userByID = getUserByID(userID);
    if (userByID != null && kind != User.Kind.ANONYMOUS) {
      // user exists!
      String emailHash = userByID.getEmailHash();
      String passwordHash = userByID.getPasswordHash();
      if (emailHash != null && passwordHash != null &&
          !emailHash.isEmpty() && !passwordHash.isEmpty()) {
        logger.debug(" : addUser : user " + userID + " is an existing user.");
        return null; // existing user!
      } else {
        int id = userByID.getId();
        updateUser(id, kind, passwordH, emailH);
        User userWhere = getUserWhere(id);
        logger.debug(" : addUser : returning updated user " + userWhere);
        return userWhere;
      }
    } else {
      Collection<User.Permission> perms = (kind == User.Kind.CONTENT_DEVELOPER) ? CD_PERMISSIONS : EMPTY_PERM;
      boolean enabled = (kind != User.Kind.CONTENT_DEVELOPER) || isAdmin(userID) || enableAllUsers;

      int l = addUser(age, isMale ? MALE : FEMALE, 0, ipAddr, "", "", dialect, userID, enabled, perms, kind, passwordH,
          emailH, email, device, first, last);
      User userWhere = getUserWhere(l);
      logger.debug(" : addUser : added new user " + userWhere);

      return userWhere;
    }
  }*/

  public User addUser(SignUpUser user) {
    String userID = user.getUserID();
    User userByID = getUserByID(userID);
    User.Kind kind = user.getKind();
    if (userByID != null && kind != User.Kind.ANONYMOUS) {
      // user exists!
      String emailHash = userByID.getEmailHash();
      String passwordHash = userByID.getPasswordHash();
      if (emailHash != null && passwordHash != null &&
          !emailHash.isEmpty() && !passwordHash.isEmpty()) {
        logger.debug(" : addUser : user " + userID + " is an existing user.");
        return null; // existing user!
      } else {
        int id = userByID.getId();
        updateUser(id, kind, user.getPasswordH(), user.getEmailH());
        User userWhere = getUserWhere(id);
        logger.debug(" : addUser : returning updated user " + userWhere);
        return userWhere;
      }
    } else {
      Collection<User.Permission> perms = (kind == User.Kind.CONTENT_DEVELOPER) ? CD_PERMISSIONS : EMPTY_PERM;
      boolean enabled = (kind != User.Kind.CONTENT_DEVELOPER) || isAdmin(userID) || enableAllUsers;

      int l = addUserAndGetID(user, perms, enabled);
      User userWhere = getUserWhere(l);
      logger.debug(" : addUser : added new user " + userWhere);
      return userWhere;
    }
  }

  private int addUserAndGetID(SignUpUser user, Collection<User.Permission> perms, boolean enabled) {
    return addUser(user.getAge(),
        user.isMale() ? MALE : FEMALE,
        0,
        user.getIp(), "", "",
        user.getDialect(),
        user.getUserID(), enabled, perms, user.getKind(), user.getPasswordH(),
        user.getEmailH(), user.getEmail(), user.getDevice(), user.getFirst(), user.getLast());
  }

  abstract User getUserByID(String id);

  abstract void updateUser(int id, User.Kind kind, String passwordH, String emailH);

  abstract User getUserWhere(int userid);

  boolean isAdmin(String userid) {
    return userid != null && (admins.contains(userid.toLowerCase()));
  }

  private static final List<User.Permission> EMPTY_PERMISSIONS = Collections.emptyList();

  /**
   * public for test access... for now
   */
  public void findOrMakeDefectDetector() {
    this.defectDetector = getOrAdd(DEFECT_DETECTOR);
    this.beforeLoginUser = getOrAdd(BEFORE_LOGIN_USER);
    this.importUser = getOrAdd(IMPORT_USER);
    this.defaultUser = getOrAdd("defaultUser");
    this.defaultMale = getOrAdd("defaultMaleUser");
    this.defaultFemale = getOrAdd("defaultFemaleUser");
  }

  private int getOrAdd(String beforeLoginUser) {
    int beforeLoginUserID = getIdForUserID(beforeLoginUser);
    if (beforeLoginUserID == -1) {
      beforeLoginUserID = addShellUser(beforeLoginUser);
    }
    return beforeLoginUserID;
  }

  private int addShellUser(String defectDetector) {
    return addUser(89, MALE, 0, "", "", UNKNOWN, UNKNOWN, defectDetector, false, EMPTY_PERMISSIONS,
        User.Kind.STUDENT, "", "", "", "", "", "");
  }

  abstract int getIdForUserID(String id);

  abstract int addUser(int age, String gender, int experience, String userAgent,
                       String trueIP,
                       String nativeLang,
                       String dialect,
                       String userID,
                       boolean enabled,
                       Collection<User.Permission> permissions,
                       User.Kind kind,
                       String passwordH, String emailH, String email, String device, String first, String last);
}
