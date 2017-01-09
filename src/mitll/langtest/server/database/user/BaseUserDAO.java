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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public abstract class BaseUserDAO extends DAO {
  private static final Logger logger = LogManager.getLogger(BaseUserDAO.class);

  private static final String DEFECT_DETECTOR = "defectDetector";
  protected static final String BEFORE_LOGIN_USER = "beforeLogin";
  protected static final String IMPORT_USER = "importUser";
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
  public static final String DEFAULT_USER1 = "defaultUser";
  public static final String DEFAULT_MALE_USER = "defaultMaleUser";
  public static final String DEFAULT_FEMALE_USER = "defaultFemaleUser";
  @Deprecated
  final String language;
  protected int defectDetector, beforeLoginUser, importUser, defaultUser, defaultMale, defaultFemale;
  // private final boolean enableAllUsers;

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

  @Deprecated
  public static MiniUser DEFAULT_USER = new MiniUser(DEFAULT_USER_ID, 99, true, "default", false);
  @Deprecated
  public static MiniUser DEFAULT_MALE = new MiniUser(DEFAULT_MALE_ID, 99, true, "Male", false);
  @Deprecated
  public static MiniUser DEFAULT_FEMALE = new MiniUser(DEFAULT_FEMALE_ID, 99, false, "Female", false);

  protected final Collection<String> admins;
  private HashSet<String> defaultUsers;

  BaseUserDAO(Database database) {
    super(database);
    admins = database.getServerProps().getAdmins();
    language = database.getServerProps().getLanguage();
  }

  /**
   * @return
   * @see IUserListManager#addDefect(int, String, String)
   * @see AnnotationDAO#AnnotationDAO
   */
  public int getDefectDetector() {
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
   * If the user is somehow missing some info, but exists, add the missing
   * email,pass info
   * to their account
   *
   * @param user
   * @return user if add or modified, null if no change was done
   * @see UserManagement#addUser
   */
  public User addUser(SignUpUser user) {
    String userID = user.getUserID();
    User currentUser = getUserByID(userID);
    if (currentUser == null) {
      User userWhere = getUserWhere(addUserAndGetID(user));
      logger.debug(" : addUser : added new user " + userWhere);
      return userWhere;
    } else {
      // user exists!
      return null;

      /*String emailHash = currentUser.getEmailHash();

      if (emailHash != null &&
          !emailHash.isEmpty()
          ) {
        logger.debug(" : addUser : user " + userID + " is an existing user.");
        return null; // existing user!
      } else {
        int id = currentUser.getID();
        updateUser(id, user.getKind(), user.getEmailH(), user.getEmail());
        User userWhere = getUserWhere(id);
        logger.debug(" : addUser : returning updated user " + userWhere);
        return userWhere;
      }*/
    }
  }

  /**
   * @param user
   * @return
   * @paramx perms
   * @see #addUser
   */
  private int addUserAndGetID(SignUpUser user) {
    return addUser(user.getAge(),
        user.isMale() ? MALE : FEMALE,
        0,
        user.getIp(),
        "",
        "",
        user.getDialect(),
        user.getUserID(),
        true,
        Collections.emptyList(),
        user.getKind(),
        // user.getFreeTextPassword(),
        // user.getPasswordH(),

        user.getEmailH(),
        user.getEmail(),
        user.getDevice(),
        user.getFirst(), user.getLast(),
        user.getUrl(),
        user.getAffiliation());
  }

  abstract User getUserByID(String id);

  abstract void updateUser(int id, User.Kind kind, String emailH, String email);

  abstract User getUserWhere(int userid);

  @Deprecated
  boolean isAdmin(String userid) {
    return userid != null && (admins.contains(userid.toLowerCase()));
  }

  private static final List<User.Permission> EMPTY_PERMISSIONS = Collections.emptyList();

  /**
   * public for test access... for now
   *
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
   */
  public void ensureDefaultUsers() {
    this.defectDetector = getOrAdd(DEFECT_DETECTOR, "Defect", "Detector", User.Kind.QAQC);
    this.beforeLoginUser = getOrAdd(BEFORE_LOGIN_USER, "Before", "Login", User.Kind.STUDENT);
    this.importUser = getOrAdd(IMPORT_USER, "Import", "User", User.Kind.CONTENT_DEVELOPER);
    this.defaultUser = getOrAdd(DEFAULT_USER1, "Default", "User", User.Kind.AUDIO_RECORDER);
    this.defaultMale = getOrAdd(DEFAULT_MALE_USER, "Default", "Male", User.Kind.AUDIO_RECORDER);
    this.defaultFemale = getOrAdd(DEFAULT_FEMALE_USER, "Default", "Female", User.Kind.AUDIO_RECORDER);

    this.defaultUsers = new HashSet<>(Arrays.asList(DEFECT_DETECTOR, BEFORE_LOGIN_USER, IMPORT_USER, DEFAULT_USER1, DEFAULT_FEMALE_USER, DEFAULT_MALE_USER, "beforeLoginUser"));
  }

  public boolean isDefaultUser(String userid) {
    return defaultUsers.contains(userid);
  }

  private int getOrAdd(String beforeLoginUser, String first, String last, User.Kind kind) {
    int beforeLoginUserID = getIdForUserID(beforeLoginUser);
    if (beforeLoginUserID == -1) {
      beforeLoginUserID = addShellUser(beforeLoginUser, first, last, kind);
    }
    return beforeLoginUserID;
  }

  /**
   * @param defectDetector
   * @param first
   * @param last
   * @param kind
   * @return
   * @see #getOrAdd
   */
  private int addShellUser(String defectDetector, String first, String last, User.Kind kind) {
    return addUser(89,
        MALE,
        0, "", "", UNKNOWN, UNKNOWN, defectDetector, false, EMPTY_PERMISSIONS,
        kind,
        "",
        "",//"admin@dliflc.edu",
        "",
        first,
        last,
        "","MIT-LL");
  }

  abstract int getIdForUserID(String id);

  /**
   * @param age
   * @param gender
   * @param experience
   * @param userAgent
   * @param trueIP
   * @param nativeLang
   * @param dialect
   * @param userID
   * @param enabled
   * @param permissions
   * @param kind
   * @param emailH
   * @param email
   * @param device
   * @param first
   * @param last
   * @param url
   * @return
   * @paramx freeTextPassword
   * @paramx passwordH
   * @see #addShellUser
   * @see #addUserAndGetID
   */
  abstract int addUser(int age,
                       String gender,
                       int experience,
                       String userAgent,
                       String trueIP,
                       String nativeLang,
                       String dialect,
                       String userID,
                       boolean enabled,
                       Collection<User.Permission> permissions,
                       User.Kind kind,

                       //   @Deprecated  String freeTextPassword,
                       //  String passwordH,
                       String emailH,
                       String email,
                       String device,
                       String first,
                       String last,
                       String url, String affiliation);
}