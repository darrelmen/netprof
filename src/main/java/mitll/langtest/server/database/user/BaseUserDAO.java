/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.database.user;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.annotation.AnnotationDAO;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.shared.user.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public abstract class BaseUserDAO extends DAO {
  private static final Logger logger = LogManager.getLogger(BaseUserDAO.class);

  protected static final String DEFECT_DETECTOR = "defectDetector";
  protected static final String BEFORE_LOGIN_USER = "beforeLogin";
  protected static final String IMPORT_USER = "importUser";

  public static final String USERS = "users";

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
  static final String DEFAULT_USER1 = "defaultUser";
  static final String DEFAULT_MALE_USER = "defaultMaleUser";
  static final String DEFAULT_FEMALE_USER = "defaultFemaleUser";
  private static final String UNSET_EMAIL = "unset@unset.com";
  @Deprecated
  final String language;
  /**
   * @see DominoUserDAOImpl#ensureDefaultUsersLocal
   */
  int defectDetector = DEFAULT_USER_ID,
      beforeLoginUser = DEFAULT_USER_ID, importUser = DEFAULT_USER_ID, defaultUser = DEFAULT_USER_ID,
      defaultMale = DEFAULT_USER_ID, defaultFemale = DEFAULT_USER_ID;

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

  final Collection<String> admins;

  /**
   * @see #isDefaultUser
   * @see DominoUserDAOImpl#ensureDefaultUsers
   */
  Set<String> defaultUsers = new HashSet<>();

  BaseUserDAO(Database database) {
    super(database);
    admins = database.getServerProps().getAdmins();
    language = database.getServerProps().getLanguage();
  }

  /**
   * @return
   * @see IUserListManager#addDefect(mitll.langtest.shared.exercise.CommonExercise, String, String)
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
   * @see mitll.langtest.client.user.SignUpForm#gotSignUp
   */
  public User addUser(SignUpUser user) {
    User currentUser = getUserByID(user.getUserID());
    if (currentUser == null) {
      LoginResult loginResult = addUserAndGetID(user);
      int userid = loginResult.getId();
      if (loginResult.getResultType() == LoginResult.ResultType.Unknown) {
        logger.warn("got result " + loginResult.getResultType());
      } else {
        logger.info("result " + loginResult.getResultType() + " for user " + userid);
      }

      User userWhere = userid == -1 ? null : getUserWhere(userid);
      if (userWhere != null) {
        String token = loginResult.getToken();
        userWhere.setResetKey(token);
        if (token.isEmpty()) logger.warn("addUser huh? empty token?");
      } else {
        logger.warn("addUser : can't find user with id " + userid);
      }
      logger.info("addUser : added new " + userWhere);
      return userWhere;
    } else {
      logger.warn("addUser : user exists ");
      return null;
    }
  }

  /**
   * Will send out an email with an embedded url to click on - should match the webapp.
   *
   * @param user
   * @return -1 if we couldn't add this user
   * @paramx perms
   * @see #addUser
   */
  private LoginResult addUserAndGetID(SignUpUser user) {
    String urlToUse = "https://" + getDatabase().getServerProps().getNPServer() + "/" +
        database.getServerProps().getAppName();

    logger.info("addUserAndGetID user will see url = " + urlToUse);

    return addUser(user.getAge(),
        user.getRealGender(),
        0,
        user.getIp(),
        "",
        "",
        user.getDialect(),
        user.getUserID(),
        true,
        Collections.emptyList(),
        user.getKind(),

        user.getEmailH(),
        user.getEmail(),
        user.getDevice(),
        user.getFirst(), user.getLast(),
        urlToUse,
        user.getAffiliation());
  }

  abstract User getUserByID(String id);

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
  abstract public void ensureDefaultUsers();

  public boolean isDefaultUser(String userid) {
    return defaultUsers.contains(userid);
  }

  int getOrAdd(String beforeLoginUser, String first, String last, Kind kind) {
    int beforeLoginUserID = getIdForUserID(beforeLoginUser);
    if (beforeLoginUserID == -1) {
      beforeLoginUserID = addShellUser(beforeLoginUser, first, last, kind);
    }
    return beforeLoginUserID;
  }

  /**
   * @param userID
   * @param first
   * @param last
   * @param kind
   * @return
   * @see #getOrAdd
   */
  private int addShellUser(String userID, String first, String last, Kind kind) {
    return addUser(89,
        MiniUser.Gender.Unspecified,
        0, "", "", UNKNOWN, UNKNOWN, userID, false, EMPTY_PERMISSIONS,
        kind,
        "",
        UNSET_EMAIL,
        "",
        first,
        last,
        "",
        "MIT-LL").getId();
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
   * @see #addShellUser
   * @see #addUserAndGetID
   */
  abstract LoginResult addUser(int age,
                               MiniUser.Gender gender,
                               int experience,
                               String userAgent,
                               String trueIP,
                               String nativeLang,
                               String dialect,
                               String userID,
                               boolean enabled,
                               Collection<User.Permission> permissions,
                               Kind kind,


                               String emailH,
                               String email,
                               String device,
                               String first,
                               String last,
                               String url,
                               String affiliation);
}