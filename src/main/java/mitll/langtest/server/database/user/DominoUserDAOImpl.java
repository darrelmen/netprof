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

import com.mongodb.MongoTimeoutException;
import mitll.hlt.domino.server.user.IUserServiceDelegate;
import mitll.hlt.domino.server.user.MongoGroupDAO;
import mitll.hlt.domino.server.user.UserServiceFacadeImpl;
import mitll.hlt.domino.server.util.*;
import mitll.hlt.domino.shared.common.FilterDetail;
import mitll.hlt.domino.shared.common.FindOptions;
import mitll.hlt.domino.shared.common.SResult;
import mitll.hlt.domino.shared.model.user.*;
import mitll.hlt.json.JSONSerializer;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.Report;
import mitll.langtest.server.database.analysis.Analysis;
import mitll.langtest.server.database.audio.BaseAudioDAO;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.project.IProjectManagement;
import mitll.langtest.server.database.security.IUserSecurityManager;
import mitll.langtest.server.database.security.NPUserSecurityManager;
import mitll.langtest.server.services.OpenUserServiceImpl;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.project.ProjectStatus;
import mitll.langtest.shared.project.ProjectType;
import mitll.langtest.shared.user.*;
import mitll.langtest.shared.user.User;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteException;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.DeploymentMode;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.logger.slf4j.Slf4jLogger;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.ServletContext;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.include;
import static mitll.hlt.domino.server.ServerInitializationManager.*;
import static mitll.hlt.domino.server.user.MongoUserServiceDelegate.USERS_C;
import static mitll.hlt.domino.server.util.ServerProperties.CACHE_ENABLED_PROP;
import static mitll.langtest.shared.user.Kind.*;

/**
 * Store user info in domino tables.
 */
public class DominoUserDAOImpl extends BaseUserDAO implements IUserDAO, IDominoUserDAO {
  private static final Logger logger = LogManager.getLogger(DominoUserDAOImpl.class);

  private static final mitll.hlt.domino.shared.model.user.User.Gender DMALE = mitll.hlt.domino.shared.model.user.User.Gender.Male;
  private static final mitll.hlt.domino.shared.model.user.User.Gender DFEMALE = mitll.hlt.domino.shared.model.user.User.Gender.Female;
  private static final mitll.hlt.domino.shared.model.user.User.Gender UNSPECIFIED = mitll.hlt.domino.shared.model.user.User.Gender.Unspecified;
  private static final String PRIMARY = "primary";
  private static final String DEFAULT_AFFILIATION = "";

  private static final String UID_F = "userId";
  private static final String PASS_F = "pass";
  private static final String LOCALHOST = "127.0.0.1";
  private static final boolean USE_DOMINO_IGNITE = true;
  private static final boolean USE_DOMINO_CACHE = false;

  /**
   * Should be consistent with DOMINO.
   * Actually it's all lower case.
   */
  public static final String NETPROF = DLIApplication.NetProf;
  private static final Set<String> APPLICATION_ABBREVIATIONS = Collections.singleton(NETPROF);
  private static final String LYDIA_01 = "Lydia01";
  private static final String PO_M = "PoM";
  private static final String ILR = "ILR";
  private static final String USER = "User";
  private static final String DEFAULT = "Default";
  private static final int EST_NUM_USERS = 8000;
  private static final String VALID_EMAIL = "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$";
  private static final long STALE_DUR = 5L * 60L * 1000L;

  private IUserServiceDelegate delegate = null;
  private MyMongoUserServiceDelegate myDelegate;

  private Mongo pool = null;
  private boolean usedDominoResources = true;

  private final Map<String, Kind> roleToKind = new HashMap<>();
  /**
   * get the admin user.
   *
   * @see #ensureDefaultUsers
   */
  private mitll.hlt.domino.shared.model.user.User adminUser;
  private mitll.hlt.domino.shared.model.user.User dominoImportUser;
  private DBUser dominoAdminUser = null;

  private Ignite ignite = null;

  private JSONSerializer serializer = null;
  private IUserProjectDAO userProjectDAO;

  private IProjectManagement projectManagement;

  /**
   * @param database
   * @param userProjectDAO
   * @see mitll.langtest.server.database.DatabaseImpl#connectToDatabases(PathHelper, ServletContext)
   */
  public DominoUserDAOImpl(Database database, ServletContext servletContext) {
    super(database);

    populateRoles();

    Object attribute = servletContext == null ? null : servletContext.getAttribute(USER_SVC);
    if (attribute != null) {
      delegate = (IUserServiceDelegate) attribute;
      pool = (Mongo) servletContext.getAttribute(MONGO_ATT_NAME);
      serializer = (JSONSerializer) servletContext.getAttribute(JSON_SERIALIZER);
      doAfterGetDelegate();
    } else {
      if (servletContext != null) {
        Enumeration<String> attributeNames = servletContext.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
          logger.info("domino user dao: no user service (" + USER_SVC + ")" + attributeNames.nextElement());
        }
      }
      usedDominoResources = false;
      try {
        connectToMongo(database, database.getServerProps().getProps());
      } catch (Exception e) {
        logger.error("Couldn't connect to mongo - is it running and accessible? " + e, e);
        throw e;
      }
    }
  }

  private void connectToMongo(Database database, Properties props) throws MongoTimeoutException {
    pool = Mongo.createPool(new DBProperties(props));

    //if (pool != null) {
    serializer = Mongo.makeSerializer();
//      logger.info("OK made serializer " + serializer);
    Mailer mailer = new Mailer(new MailerProperties(props));
    props.setProperty(CACHE_ENABLED_PROP, "" + USE_DOMINO_CACHE);
    ServerProperties dominoProps =
        new ServerProperties(props, "1.0", "demo", "0", "now");

    dominoProps.updateProperty(ServerProperties.APP_NAME_PROP, database.getServerProps().getAppTitle());
    //String appName = dominoProps.getAppName();
    //     logger.info("DominoUserDAOImpl app name is " + appName);

    ignite = null;
    if (/*dominoProps.isCacheEnabled() ||*/ USE_DOMINO_IGNITE) {
      ignite = getIgnite();
      if (ignite != null) {
        ignite.configuration().setGridLogger(new Slf4jLogger());
      }

      //logger.debug("DominoUserDAOImpl cache - ignite!");
      // newContext.setAttribute(IGNITE, ignite);
    } else {
      logger.debug("DominoUserDAOImpl no cache");
    }
    delegate = UserServiceFacadeImpl.makeServiceDelegate(dominoProps, mailer, pool, serializer, ignite);

    logger.debug("made" +
        "\ndelegate " + delegate.getClass() +
        "\nignite = " + ignite +
        "\nisCacheEnabled = " + dominoProps.isCacheEnabled());

    doAfterGetDelegate();
    //} else {
    //  logger.error("DominoUserDAOImpl couldn't connect to user service - no pool!\n\n");
    // }
  }

  private void doAfterGetDelegate() {
    myDelegate = makeMyServiceDelegate();
    dominoAdminUser = delegate.getAdminUser();
  }

  public JSONSerializer getSerializer() {
    return serializer;
  }

  private void populateRoles() {
    for (Kind kind : Kind.values()) {
      roleToKind.put(kind.getRole(), kind);
    }
  }

  @Override
  public void close() {
    if (!usedDominoResources) {
      logger.info("closing connection to " + pool);
      pool.closeConnection();
    }
    if (!usedDominoResources) {
      ignite.close();
    }
  }

  @Override
  public boolean isStudent(int userIDFromSessionOrDB) {
    User user = getByID(userIDFromSessionOrDB);
    boolean b = user.isStudent() && user.getPermissions().isEmpty();
    if (b) {
      //logger.info("isStudent " + userIDFromSessionOrDB);
    } else {
      logger.debug("isStudent : not a student #" + userIDFromSessionOrDB);
    }
    return b;
  }

  private Ignite getIgnite() {
    TcpDiscoverySpi spi = new TcpDiscoverySpi();
    TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
    ipFinder.setAddresses(Collections.singletonList(LOCALHOST));
    spi.setIpFinder(ipFinder);
    IgniteConfiguration cfg = new IgniteConfiguration();
    cfg.setDeploymentMode(DeploymentMode.PRIVATE);
    // Override default discovery SPI.
    cfg.setDiscoverySpi(spi);

    try {
      return Ignition.start(cfg);
    } catch (IgniteException e) {
      logger.error("getIgnite : Couldn't start ignite - got " + e, e);
      return null;
    }
  }

  private MyMongoUserServiceDelegate makeMyServiceDelegate() {
    return new MyMongoUserServiceDelegate();
  }

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
   */
  @Override
  public void ensureDefaultUsers() {
    if (delegate == null) {
      logger.error("no delegate - couldn't connect to Mongo!");
    } else {
      ensureDefaultUsersLocal();
      String userId = dominoAdminUser.getUserId();
      adminUser = delegate.getUser(userId);//BEFORE_LOGIN_USER);
      // logger.info("ensureDefaultUsers got admin user " + adminUser + " has roles " + adminUser.getRoleAbbreviationsString());

      if (adminUser.getPrimaryGroup() == null) {
        logger.warn("ensureDefaultUsers no group for " + adminUser);
        adminUser.setPrimaryGroup(makePrimaryGroup(PRIMARY));
      }

      dominoImportUser = delegate.getUser(IMPORT_USER);
    }
  }

  /**
   * public for test access... for now
   *
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
   */
  private void ensureDefaultUsersLocal() {
    this.defectDetector = getOrAdd(DEFECT_DETECTOR, "Defect", "Detector", Kind.QAQC);
    this.beforeLoginUser = getOrAdd(BEFORE_LOGIN_USER, "Before", "Login", Kind.STUDENT);
    this.importUser = getOrAdd(IMPORT_USER, "Import", USER, Kind.CONTENT_DEVELOPER);
    this.defaultUser = getOrAdd(DEFAULT_USER1, DEFAULT, USER, Kind.AUDIO_RECORDER);
    this.defaultMale = getOrAdd(DEFAULT_MALE_USER, DEFAULT, "Male", Kind.AUDIO_RECORDER);
    this.defaultFemale = getOrAdd(DEFAULT_FEMALE_USER, DEFAULT, "Female", Kind.AUDIO_RECORDER);

    this.defaultUsers = new HashSet<>(Arrays.asList(DEFECT_DETECTOR, BEFORE_LOGIN_USER, IMPORT_USER, DEFAULT_USER1, DEFAULT_FEMALE_USER, DEFAULT_MALE_USER, "beforeLoginUser"));
  }

  public void createTable() {
  }

  @Override
  public String getName() {
    return "";
  }

  /**
   * JUST USED IN PROJECT IMPORT.
   * <p>
   * Returns ClientUserDetail from database, not necessarily same as passed in?
   * <p>
   * TODO : permissions
   *
   * @param user
   * @param encodedPass
   * @return
   * @see mitll.langtest.server.database.copy.UserCopy#addUser
   */
  @Override
  public ClientUserDetail addAndGet(ClientUserDetail user, String encodedPass) {
    invalidateCache();

/*    if (user.getGender() != UNSPECIFIED) {
      logger.info("addAndGet going in " + user.getGender() + " for user '" + user.getUserId() + "'");
    }*/

    SResult<ClientUserDetail> clientUserDetailSResult1 = delegate.migrateUser(user, encodedPass);
    boolean b = !clientUserDetailSResult1.isError();
    if (!b) {
      logger.error("\n\n\naddAndGet didn't set password for " + user.getUserId() + " : " +
          clientUserDetailSResult1.getResponseMessage());
      return null;
    } else {
      ClientUserDetail clientUserDetail = clientUserDetailSResult1.get();
      if (clientUserDetail.getGender() == UNSPECIFIED) {
//        logger.info("addAndGet note : " + clientUserDetail.getUserId() + " gender is " + clientUserDetail.getGender());
      }
      return clientUserDetail;
    }
  }

  /**
   * @param user
   * @param url
   * @param sendEmail
   * @return
   * @see BaseUserDAO#addUser
   * @see #addUser(int, MiniUser.Gender, int, String, String, String, String, String, boolean, Collection, Kind, String, String, String, String, String, String, String)
   */
  private SResult<ClientUserDetail> addUserToMongo(ClientUserDetail user, String url, boolean sendEmail) {
    return delegate.addUser(sendEmail ? user : adminUser, user, url);
  }

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
   * @see UserManagement#addUser
   */
  @Override
  public int addUser(int age,
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
                     String affiliation) {
    mitll.hlt.domino.shared.model.user.User.Gender gender1 = mitll.hlt.domino.shared.model.user.User.Gender.valueOf(gender.name());
    ClientUserDetail updateUser = new ClientUserDetail(
        userID,
        first,
        last,
        email,
        affiliation,
        gender1,
        Collections.singleton(kind.getRole()),
        getGroup(),

        APPLICATION_ABBREVIATIONS
    );

    AccountDetail acctDetail = new AccountDetail();
    updateUser.setAcctDetail(acctDetail);
    acctDetail.setCrTime(new Date());
    SResult<ClientUserDetail> clientUserDetailSResult = addUserToMongo(updateUser, url, true);

    if (clientUserDetailSResult == null) {
      return -1; // password error?
    } else {
      ClientUserDetail clientUserDetail = clientUserDetailSResult.get();
      invalidateCache();
      return clientUserDetail.getDocumentDBID();
    }
  }

  private Group primaryGroup = null;

  /**
   * Need a group - just use the first one.
   *
   * @return
   * @see #addUser(int, MiniUser.Gender, int, String, String, String, String, String, boolean, Collection, Kind, String, String, String, String, String, String, String)
   * @see #forgotPassword
   */
  @NotNull
  public Group getGroup() {
    if (primaryGroup == null) {
      List<Group> groups = delegate.getGroupDAO().searchGroups("");
      primaryGroup = groups.isEmpty() ? null : groups.iterator().next();

      if (primaryGroup == null) { //defensive
        logger.warn("\n\n\ngetGroup making a new group...?\n\n\n");
        primaryGroup = makePrimaryGroup(PRIMARY);
      }
    }

    return primaryGroup;
  }

  @NotNull
  private Group makePrimaryGroup(String name) {
    Date out = Date.from(getZonedDateThirtyYears().toInstant());
    return new Group(name, name + "Group", 365, 24 * 365, out, adminUser);
  }

  @NotNull
  private LocalDateTime getThirtyYearsFromNow() {
    return LocalDateTime.now().plusYears(30);
  }

  private final Map<String, Group> nameToGroup = new HashMap<>();

  /**
   * Cache secondary group so don't have to search for it.
   *
   * @param name
   * @return
   */
  private Group getGroupOrMake(String name) {
    Group group = nameToGroup.get(name);
    if (group == null) {
      List<Group> groups = delegate.getGroupDAO().searchGroups(name);
      group = groups.isEmpty() ? null : groups.iterator().next();

      if (group == null) { //defensive
        group = makeAGroup(name);
      }
      nameToGroup.put(name, group);
      logger.info("now " + nameToGroup.size() + " groups.");
    }
    return group;
  }

  private Group makeAGroup(String name) {
    logger.warn("getGroupOrMake making a new group " + name);
    MongoGroupDAO groupDAO1 = (MongoGroupDAO) delegate.getGroupDAO();

    Date out = Date.from(getZonedDateThirtyYears().toInstant());
    SResult<ClientGroupDetail> name1 = groupDAO1.doAdd(adminUser,
        new ClientGroupDetail(name, "name", 365, 24 * 365, out, adminUser));

    Group group = null;
    if (name1.isError()) {
      logger.error("couldn't make " + name1);
    } else {
      group = name1.get();
    }
    return group;
  }

  @NotNull
  private ZonedDateTime getZonedDateThirtyYears() {
    return getThirtyYearsFromNow().atZone(ZoneId.systemDefault());
  }

  private ClientUserDetail getClientUserDetail(DBUser dbUser) {
    AccountDetail acctDtl = new AccountDetail();
    acctDtl.setUpdater(dbUser);
    return new ClientUserDetail(dbUser, acctDtl);
  }

  /**
   * @param id
   * @param newHashedPassword
   * @param baseURL
   * @return
   * @see IUserDAO#changePassword
   */
  private DBUser savePasswordAndGetUser(int id, String currentPassword, String newHashedPassword, String baseURL) {
    DBUser dbUser = lookupUser(id);
    if (dbUser != null) {
      boolean b = delegate.changePassword(adminUser, dbUser, currentPassword, newHashedPassword, baseURL);
      if (!b) {
        logger.error("huh? didn't change password for " + id + "\n");
      }
    }
    return dbUser;
  }

  /**
   * @param email
   * @return
   * @see OpenUserServiceImpl#forgotUsername(String, String)
   */
  @Override
  public String isValidEmail(String email) {
    List<DBUser> users = getDbUsers(getEmailFilter(email));
    return users.isEmpty() ? null : users.get(0).getUserId();
  }

  @Override
  public Integer getIDForUserAndEmail(String user, String emailH) {
    Set<FilterDetail<UserColumn>> emailFilter = getEmailFilter(emailH);
    addUserID(user, emailFilter);
    List<DBUser> users = getDbUsers(emailFilter);
    return users.isEmpty() ? null : users.get(0).getDocumentDBID();
  }

  @Override
  public String getNameForEmail(String emailH) {
    Set<FilterDetail<UserColumn>> emailFilter = getEmailFilter(emailH);
    List<DBUser> users = getDbUsers(emailFilter);
    return users.isEmpty() ? null : users.get(0).getFullName();
  }

  /**
   * @param filterDetails
   * @return
   * @see #getIDForUserAndEmail
   */
  private List<DBUser> getDbUsers(Set<FilterDetail<UserColumn>> filterDetails) {
    List<DBUser> users = delegate.getUsers(-1, new FindOptions<>(filterDetails));
    //  logger.info("getDbUsers " + opts + " = " + users);
    return users;
  }

  private Set<FilterDetail<UserColumn>> getEmailFilter(String emailH) {
    Set<FilterDetail<UserColumn>> filterDetails = new HashSet<>();
    filterDetails.add(new FilterDetail<>(UserColumn.Email, emailH, FilterDetail.Operator.EQ));
    return filterDetails;
  }

  @Override
  public int getIdForUserID(String id) {
    mitll.hlt.domino.shared.model.user.User user = delegate.getUser(id);
    return user == null ? -1 : user.getDocumentDBID();
  }

  /**
   * Make sure to do string comparison.
   *
   * @param user
   * @param filterDetails
   */
  private void addUserID(String user, Set<FilterDetail<UserColumn>> filterDetails) {
    FilterDetail<UserColumn> filterDetail = new FilterDetail<>(UserColumn.UserId, user, FilterDetail.Operator.EQ);
    filterDetail.setStringField(true);
    filterDetails.add(filterDetail);
  }

  /**
   * @param userId
   * @param attemptedTxtPass
   * @param userAgent
   * @param remoteAddr
   * @param sessionID
   * @return
   * @seex #getStrictUserWithPass(String, String)
   * @see IUserSecurityManager#getLoginResult
   */
  public User loginUser(String userId,
                        String attemptedTxtPass,
                        String userAgent,
                        String remoteAddr,
                        String sessionID) {
    String encodedCurrPass = getUserCredentials(userId);

    DBUser loggedInUser =
        delegate.loginUser(
            userId,
            attemptedTxtPass,
            remoteAddr,
            userAgent,
            sessionID);

    logger.info("loginUser '" + userId + "' pass num chars " + attemptedTxtPass.length() +
        "\n\texisting credentials " + encodedCurrPass +
        "\n\tyielded " + loggedInUser);

    return getUser(loggedInUser);
  }

  private final Map<String, Map<String, Boolean>> dominoToEncodedToMatch = new HashMap<>();

  /**
   * Remember pair of user password and encoded password and their match result to speed up this call.
   * <p>
   * remember password for user id
   *
   * @param userID
   * @param encodedPassword
   * @param userIDToPass
   * @return
   * @seex #getUserIfMatch
   * @see mitll.langtest.server.database.copy.UserCopy#copyUsers
   */
  public boolean isMatchingPassword(String userID, String encodedPassword, Map<String, String> userIDToPass) {
//  logger.info("isMatchingPassword '" + id + "' and dominoPassword hash '" + encodedPassword.length() + "'");
    String dominoPassword = userIDToPass.get(userID);
    if (dominoPassword == null) {
      userIDToPass.put(userID, dominoPassword = getUserCredentials(userID));
    }

    long then = System.currentTimeMillis();

    Map<String, Boolean> encodedToMatch = dominoToEncodedToMatch.computeIfAbsent(dominoPassword, k -> new HashMap<>());
    Boolean isRememberedMatch = encodedToMatch.get(encodedPassword);

    if (isRememberedMatch != null) {
      logger.info("isMatchingPassword return remembered match for " + userID + " in " + dominoToEncodedToMatch.size() + " and " + encodedToMatch.size());
      return isRememberedMatch;
    }

    boolean match = myDelegate.isMatch(dominoPassword, encodedPassword);

    encodedToMatch.put(encodedPassword, match);
//    logger.info("isMatchingPassword remember match for " + userID + " in " + dominoToEncodedToMatch.size() + " and " + encodedToMatch.size());

    long now = System.currentTimeMillis();

    long diff = now - then;
//    if (diff > 50) {
//      logger.warn("isMatchingPassword : took " + diff + " to check for password match.");
//    }
    if (dominoPassword != null && (match || dominoPassword.equals(encodedPassword))) {//dominoPassword.equals(encodedPassword)) {//netProfDelegate.isPasswordMatch(user.getID(), encodedPassword) || magicMatch) {
      if (diff > 100) {
        logger.warn("isMatchingPassword match in of " + dominoPassword + " vs encoded " + encodedPassword.length() +
            " match " + match + " took " + diff + " millis");
      }

      return true;
    } else {
      logger.warn("isMatchingPassword no match in db " + dominoPassword + " vs encoded " + encodedPassword.length() + " took " + diff);
      return false;
    }
  }

  /**
   * @param userId
   * @return
   * @see #isMatchingPassword
   */
  private String getUserCredentials(String userId) {
    Bson query = eq(UID_F, userId);
    Document user = pool
        .getMongoCollection(USERS_C)
        .find(query)
        .projection(include(PASS_F))
        .first();

    if (user != null) {
      return user.getString(PASS_F);
    } else {
      logger.warn("getUserCredentials : User not found in DB! Query: " + query);
    }
    return null;
  }

  /**
   * @param userID
   * @return
   * @see mitll.langtest.server.database.copy.UserCopy#makeCollisionAccount
   */
  @Override
  public User getUserByID(String userID) {
    mitll.hlt.domino.shared.model.user.DBUser dominoUser = getDBUser(userID);
    if (dominoUser == null) {
      logger.warn("getUserByID no user by '" + userID + "'");
    } else {
      logger.info("getUserByID found " + userID + " user #" + dominoUser.getDocumentDBID());
    }

    return getUser(dominoUser);
  }

  public boolean isKnownUser(String userid) {
    return getDBUser(userid) != null;
  }

  @Override
  public DBUser getDBUser(String userID) {
    return delegate.getDBUser(userID);
  }

  /**
   * @param id
   * @return
   * @see NPUserSecurityManager#getUserForID
   */
  public User getByID(int id) {
    return getUser(lookupUser(id));
  }

  private User getUser(DBUser dominoUser) {
    return dominoUser == null ? null : toUser(dominoUser);
  }

  @Override
  public User getUserWhere(int userid) {
    return getByID(userid);
  }

  private DBUser lookupUser(int id) {
    return delegate.lookupDBUser(id);
  }

  @Override
  public List<User> getUsers() {
    return toUsers(getAll());
  }

  @Override
  public ReportUsers getReportUsers() {
    List<ReportUser> copy = new ArrayList<>();
    getAll().forEach(u -> copy.add(toUser(u)));
    return new ReportUsers(copy, getUsersDevices(copy));
  }

  /**
   * adds permissions
   *
   * @param all
   * @return
   */
  private List<User> toUsers(List<DBUser> all) {
    List<User> copy = new ArrayList<>();
    all.forEach(dbUser -> copy.add(toUser(dbUser)));
    return copy;
  }

  /**
   * Convert a netprof user into a domino user.
   * <p>
   * special weird case for FernandoM01
   *
   * @param user        to import
   * @param projectName so we can make a secondary group for this language/project
   * @return the domino user
   * @see mitll.langtest.server.database.copy.UserCopy#addUser
   */
  @Override
  public ClientUserDetail toClientUserDetail(User user, String projectName, Group group) {
    String first = user.getFirst();
    String userID = user.getUserID();
    if (userID.isEmpty()) {
      userID = UNKNOWN;
    }
    if (first == null) first = userID;
    String last = user.getLast();
    if (last == null) last = "Unknown";
    String email = user.getEmail();
//    if (email == null || email.isEmpty()) {
//      email = user.getEmailHash();
//    }

    Kind userKind = user.getUserKind();

    Set<String> roleAbbreviations = Collections.singleton(userKind.getRole());
    // logger.info("toClientUserDetail " + user.getUserID() + " role is " + roleAbbreviations + " email " +email);

    boolean copyGender = user.getPermissions().contains(User.Permission.RECORD_AUDIO) ||
        user.getPermissions().contains(User.Permission.DEVELOP_CONTENT) ||
        userID.equalsIgnoreCase("FernandoM01");

    mitll.hlt.domino.shared.model.user.User.Gender gender =
        userKind ==
            STUDENT && !copyGender ? UNSPECIFIED :
            user.isMale() ? DMALE : DFEMALE;

    if (gender == UNSPECIFIED) {
      logger.info("toClientUserDetail for " + user.getID() + " '" + userID + "' " + user.getUserKind() + " gender is unspecified.");
    } else {
      logger.info("toClientUserDetail for " + user.getID() + " '" + userID + "' " + user.getUserKind() + " gender is " + gender);
    }

    if (userID.equals(LYDIA_01) && gender != mitll.hlt.domino.shared.model.user.User.Gender.Female) {
      logger.warn("\n\n\ntoClientUserDetail make sure " + userID + " is marked as a female! In english she was marked as a male.\n\n\n");
      //gender = mitll.hlt.domino.shared.model.user.User.Gender.Female;
    }

    ClientUserDetail clientUserDetail = new ClientUserDetail(
        userID,
        first,
        last,
        email,
        DEFAULT_AFFILIATION,
        gender,
        roleAbbreviations,
        group,
        APPLICATION_ABBREVIATIONS
    );

    if (clientUserDetail.getGender() != gender)
      logger.error("toClientUserDetail huh? wrote " + gender + " but got back " + clientUserDetail.getGender());

    clientUserDetail.addSecondaryGroup(getGroupOrMake(projectName));
    clientUserDetail.setAcctDetail(new AccountDetail(
        dominoImportUser,
        new Date(user.getTimestampMillis())));
    //logger.info("toClientUserDetail " + " groups for\n\t" + clientUserDetail + " : \n\t" + clientUserDetail.getSecondaryGroups());

    return clientUserDetail;
  }

  /**
   * Convert domino user into a netprof user.
   * <p>
   * adds gender - only binary (for now?)
   *
   * @param dominoUser
   * @return
   * @see #getByID
   */
  private User toUser(mitll.hlt.domino.shared.model.user.DBUser dominoUser) {
    // logger.info("toUser " + dominoUser);
    long creationTime = System.currentTimeMillis();
    String device = "";

    AccountDetail acctDetail = dominoUser.getAcctDetail();
    if (acctDetail != null) {
      device = acctDetail.getDevice().toString();
      if (acctDetail.getCrTime() != null) {
        creationTime = acctDetail.getCrTime().getTime();
      } else {
        logger.warn("toUser no creation time on " + acctDetail);
      }
    } else {
      logger.warn("toUser no acct detail for " + dominoUser.getDocumentDBID());
    }
    //   logger.debug("toUser : user " + dominoUser.getUserId() + " email " + email);//, new Exception());

    Set<User.Permission> permissionSet = new HashSet<>();
//    String emailHash = email == null ? "" : isValidEmailGrammar(email) ? Md5Hash.getHash(email) : email;
    boolean isMale = isMaleHardChoice(dominoUser);

    boolean hasAppPermission = isHasAppPermission(dominoUser);
    User user = new User(
        dominoUser.getDocumentDBID(),
        dominoUser.getUserId(),
        isMale ? 0 : 1,
        isMale ? MiniUser.Gender.Male : MiniUser.Gender.Female,
        dominoUser.isActive(),
        isAdmin(dominoUser),
        getUserKind(dominoUser, permissionSet),
        dominoUser.getEmail(),
        device,
        creationTime,
        dominoUser.getAffiliation(),
        hasAppPermission);

    user.setFirst(dominoUser.getFirstName());
    user.setLast(dominoUser.getLastName());
    if (user.isAdmin()) {
      permissionSet.add(User.Permission.PROJECT_ADMIN); // a little redundant with isAdmin...
    }

    if (user.isPoly()) {
      logger.info("toUser user " + user.getUserID() + " is a polyglot user.");
      handlePolyglotUser(dominoUser, permissionSet, user);
    } else {
      logger.info("toUser  user " + user.getUserID() + " is not a polyglot user.");
    }
    user.setPermissions(permissionSet);
//    logger.info("\ttoUser return " + user);
    return user;
  }

  private void handlePolyglotUser(DBUser dominoUser, Set<User.Permission> permissionSet, User user) {
    permissionSet.add(User.Permission.POLYGLOT);

    int id = user.getID();
    int mostRecentByUser = userProjectDAO.getCurrentProjectForUser(id);
    int projectAssignment = getProjectAssignment(dominoUser, id);

    if (mostRecentByUser == -1) {  // none yet...
      if (projectAssignment != -1) {
        userProjectDAO.add(id, projectAssignment);
      }
    } else if (projectAssignment != -1 && projectAssignment != mostRecentByUser) {
      logger.info("handlePolyglotUser before poly " + user.getUserID() + " was #" + mostRecentByUser + " will now be #" + projectAssignment);
      userProjectDAO.setCurrentProjectForUser(id, projectAssignment);
    }
  }

  /**
   * Get language from secondary group, then try to match language to a polyglot project.
   *
   * @param dominoUser
   * @param id
   * @return
   */
  private int getProjectAssignment(DBUser dominoUser, int id) {
    Collection<Group> secondaryGroups = dominoUser.getSecondaryGroups();
    int projID = -1;
    if (!secondaryGroups.isEmpty()) {
      Group next = secondaryGroups.iterator().next();
      Language languageMatchingGroup = getLanguageMatchingGroup(next);
      if (languageMatchingGroup != Language.UNKNOWN) {
        List<Project> collect = getMatchingProjects(languageMatchingGroup);

        if (!collect.isEmpty()) {
          if (collect.size() > 1) {
            logger.info("getProjectAssignment found multiple polyglot projects ");
          }

          projID = collect.iterator().next().getID();
          logger.info("getProjectAssignment : match " + next + " to project " + projID);
        } else {
          logger.warn("getProjectAssignment no polyglot project for " + languageMatchingGroup);
        }
      } else {
        logger.warn("getProjectAssignment no language matching group " + next);
      }
    } else {
      logger.info("getProjectAssignment no groups for user id " + id);
    }
    return projID;
  }

  private List<Project> getMatchingProjects(Language languageMatchingGroup) {
    List<Project> projectByLangauge = projectManagement.getProjectByLangauge(languageMatchingGroup);
    return projectByLangauge.stream()
        .filter(project ->
            project.getKind() == ProjectType.POLYGLOT &&
                project.getStatus() == ProjectStatus.PRODUCTION)
        .collect(Collectors.toList());
  }

  private Language getLanguageMatchingGroup(Group next) {
    logger.info("found secondary " + next);
    String name = next.getName();
    Language language = getLanguage(name);
    if (language == Language.UNKNOWN) {
      List<Language> languages = Arrays.asList(Language.values());
      List<Language> matches = languages.stream()
          .filter(language1 -> language1.getDominoName().equalsIgnoreCase(name))
          .collect(Collectors.toList());
      if (!matches.isEmpty()) {
        language = matches.iterator().next();
      }
    }
    return language;
  }

  private Language getLanguage(String lang) {
    Language language = Language.UNKNOWN;
    try {
      language = Language.valueOf(lang.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.error("unknown language " + lang);
    }
    return language;
  }

  /**
   * NOT case insensitive.
   *
   * @param dominoUser
   * @return
   */
  private boolean isHasAppPermission(DBUser dominoUser) {
    Set<String> applicationAbbreviations = dominoUser.getApplicationAbbreviations();
    boolean hasAppPermission = false;
    for (String app : applicationAbbreviations) {
      if (app.equals(NETPROF)) hasAppPermission = true;
    }
    if (!hasAppPermission) {
      logger.info("isHasAppPermission user #" + dominoUser.getDocumentDBID() + " not a netprof user only has : " + applicationAbbreviations);
    }
    return hasAppPermission;
  }

  /**
   * Unspecified assumed to be male... Good idea?
   *
   * @param dominoUser
   * @return
   */
  private boolean isMaleHardChoice(DBUser dominoUser) {
    mitll.hlt.domino.shared.model.user.User.Gender gender = dominoUser.getGender();
    return gender.equals(mitll.hlt.domino.shared.model.user.User.Gender.Male);
  }

  /**
   * checks roles
   *
   * @param dominoUser
   * @return
   * @see #getMini(DBUser)
   * @see #toUser
   */
  private boolean isAdmin(mitll.hlt.domino.shared.model.user.User dominoUser) {
    return isAdmin(dominoUser.getRoleAbbreviations());
  }

  private boolean isAdmin(Set<String> roleAbbreviations) {
    return roleAbbreviations.contains(ADMIN.getRole()) || roleAbbreviations.contains(PROJECT_ADMIN.getRole());
  }

  /**
   * convert dominos role to NetProF kind and permissions.
   *
   * @param dominoUser
   * @return
   * @see
   */
  private Kind getUserKind(mitll.hlt.domino.shared.model.user.User dominoUser, Set<User.Permission> permissionSet) {
    Set<String> roleAbbreviations = dominoUser.getRoleAbbreviations();

    Kind kindToUse = Kind.STUDENT;
    Set<Kind> seen = setPermissions(permissionSet, roleAbbreviations);

    // teacher trumps others... for the moment
    // need to have ordering over roles...?
    if (seen.contains(Kind.TEACHER)) {
      kindToUse = Kind.TEACHER;
    }

    return kindToUse;
  }

  @NotNull
  private Set<Kind> setPermissions(Set<User.Permission> permissionSet, Set<String> roleAbbreviations) {
    Set<Kind> seen = new HashSet<>();
    //    logger.warn("getUserKind user " + userId + " has multiple roles - choosing first one... " + roleAbbreviations.size());
    for (String role : roleAbbreviations) {
      Kind kind = getKindForRole(role);
//      logger.info(" has " + role + " -> " + kind + " ");
      seen.add(kind);
      Collection<User.Permission> initialPermsForRole = User.getInitialPermsForRole(kind);
      permissionSet.addAll(initialPermsForRole);
    }
    return seen;
  }

  private final Set<String> unknownRoles = new HashSet<>();

  @NotNull
  private Kind getKindForRole(String firstRole) {
    if (firstRole.equals(PO_M)) firstRole = Kind.PROJECT_ADMIN.getRole();
    Kind kind = roleToKind.get(firstRole);
    if (kind == null) {
      try {
        kind = Kind.valueOf(firstRole.toUpperCase());
        // shouldn't need this
        logger.debug("getUserKind lookup by NetProF user role " + firstRole);
      } catch (IllegalArgumentException e) {
        Kind kindByName = getKindByName(firstRole);
        if (kindByName == null) {
          if (!firstRole.startsWith(ILR)) {
            if (unknownRoles.contains(firstRole)) {

            } else {
              logger.warn("getUserKind no user for " + firstRole + " : now seen these unmapped roles " + unknownRoles);
              unknownRoles.add(firstRole);
            }
          }
          kind = Kind.STUDENT;
        } else {
          kind = kindByName;
        }
      }
    }
    return kind;
  }

  @Nullable
  private Kind getKindByName(String firstRole) {
    for (Kind testKind : Kind.values()) {
      if (testKind.getName().equalsIgnoreCase(firstRole)) {
        return testKind;
      }
    }
    return null;
  }

  /**
   * TODOx : get device info from domino user
   * <p>
   * For Reporting.
   *
   * @return
   * @see Report#getReport
   */
//  public List<ReportUser> getUsersDevices() {
//    return getUsersDevices(getUsers());
//  }
  private List<ReportUser> getUsersDevices(List<ReportUser> users) {
    return users
        .stream()
        .filter(user -> user.getDevice() != null && user.getDevice().startsWith("i"))
        .collect(Collectors.toList());
  }

  private Map<Integer, MiniUser> miniUserCache = null;

  /**
   * It seems like it's slow to get users out of domino users table, without ignite...
   * Maybe we should add ignite, but maybe we can avoid it for the time being?
   * <p>
   * At least every hour we go fetch the users again.
   *
   * @return
   * @see Analysis#getUserInfos
   * @see BaseAudioDAO#getAudioAttributesByProjectThatHaveBeenChecked(int, boolean)
   */
  @Override
  public synchronized Map<Integer, MiniUser> getMiniUsers() {
    long now = System.currentTimeMillis();

    int userCount = delegate.getUserCount();
    logger.debug("getMiniUsers user childCount is " + userCount, new Exception());

    if (miniUserCache == null || (now - lastCache) > 60 * 60 * 1000 || lastCount != userCount) {
      Map<Integer, MiniUser> idToUser = new HashMap<>();

      getAll().forEach(dbUser -> idToUser.put(dbUser.getDocumentDBID(), getMini(dbUser)));

      miniUserCache = idToUser;

      if (!miniUserCache.isEmpty()) {
        lastCache = now;
        long then = System.currentTimeMillis();
        lastCount = userCount;
        long now2 = System.currentTimeMillis();

        if (now2 - then > 10) {
          logger.warn("getMiniUsers took " + (now2 - then) + " millis to get childCount of db users = " + miniUserCache.size());
        }

      }
      return idToUser;
    } else {
      return miniUserCache;
    }
  }

  private int lastCount = -1;
  private long lastCache = 0;

  /**
   * It seems like getting users in and out of mongo is slow... trying to use a cache to mitigate that.
   */
  private synchronized void invalidateCache() {
    miniUserCache = null;
  }

  /**
   * TODO: try to avoid - super slow, doesn't scale...
   *
   * @return
   * @see #getMiniUsers
   * @see #getReportUsers
   * @see #getUsers
   * @deprecated
   */
  public List<DBUser> getAll() {
    long then = System.currentTimeMillis();
    logger.warn("getAll calling get all users");
    FindOptions<UserColumn> opts = getUserColumnFindOptions();
    List<DBUser> users = delegate.getUsers(-1, opts);
    long now = System.currentTimeMillis();
    if (now - then > 20) logger.warn("getAll took " + (now - then) + " to get " + users.size() + " users");
    return users;
  }

  @NotNull
  private FindOptions<UserColumn> getUserColumnFindOptions() {
    FindOptions<UserColumn> opts = new FindOptions<>();
    FilterDetail<UserColumn> netProf = new FilterDetail<>(UserColumn.DLIApplications, "NetProf", FilterDetail.Operator.RegEx);
    opts.addFilter(netProf);
    return opts;
  }

  @Override
  public MiniUser getMiniUser(int userid) {
    DBUser byID = lookupUser(userid);
    return byID == null ? null : getMini(byID);
  }

/*
  public Map<Integer, FirstLastUser> getFirstLastUsers() {
    Collection<List<Object>> userFields = delegate.getUserFields("_id", "userId", "firstName", "lastName");
    long now = System.currentTimeMillis();

    Map<Integer, FirstLastUser> idToFirstLast = new HashMap<>();
    for (List<Object> userField : userFields) {
      int i = 0;
      Object o = userField.get(i++);
      Integer o1 = o instanceof Integer ? (Integer) o : ((Double) o).intValue();
      String o2 = (String) userField.get(i++);
      String o3 = (String) userField.get(i++);
      String o4 = (String) userField.get(i++);
      idToFirstLast.put(o1, new FirstLastUser(o1, o2, o3, o4, now));
    }
    return idToFirstLast;
  }
*/

  private final ConcurrentHashMap<Integer, FirstLastUser> idToFirstLastCache = new ConcurrentHashMap<>(EST_NUM_USERS);

  /**
   * @param userDBIds
   * @return
   * @see Analysis#getUserInfos(Map, IUserDAO)
   * @see mitll.langtest.server.database.project.ProjectManagement#configureProject(Project, boolean, boolean)
   */
  @Override
  public Map<Integer, FirstLastUser> getFirstLastFor(Collection<Integer> userDBIds) {
    refreshUserCache(userDBIds);

    Map<Integer, FirstLastUser> idToFirstLast = new HashMap<>(userDBIds.size());
    userDBIds.forEach(k -> idToFirstLast.put(k, idToFirstLastCache.get(k)));
    return idToFirstLast;
  }

  /**
   * TODO : for now we skip getting the affiliation b/c lookupDBUsers is much slower.
   * Not really worth it - not showing lincoln users in analysis? Why is that a big deal?
   *
   * @param userDBIds
   */
  private void refreshUserCache(Collection<Integer> userDBIds) {
    long now = System.currentTimeMillis();

    Set<Integer> toAskFor = getMissingOrStale(userDBIds, now);
    if (!toAskFor.isEmpty()) {
      long then = System.currentTimeMillis();
      Map<Integer, UserDescriptor> idToUserD = delegate.lookupUserDescriptors(toAskFor);
//      Map<Integer, DBUser> idToUserD = delegate.lookupDBUsers(toAskFor);
      long now2 = System.currentTimeMillis();

      if (now2 - then > 100)
        logger.info("getFirstLastFor ask for " + toAskFor.size() + " users from " + userDBIds.size() + " took " + (now2 - then) + " millis");

      idToUserD.forEach((k, v) -> {
        FirstLastUser value = new FirstLastUser(k, v.getUserId(), v.getFirstName(), v.getLastName(), now, "");// v.getAffiliation());
        idToFirstLastCache.put(k, value);
      });
    }
  }

  /**
   * Go get the user every 5 minutes...?
   *
   * @param userDBIds
   * @param now
   * @return
   */
  @NotNull
  private Set<Integer> getMissingOrStale(Collection<Integer> userDBIds, long now) {
    Set<Integer> toAskFor = new HashSet<>();

    long stale = now - STALE_DUR;

    userDBIds.forEach(id -> {
      FirstLastUser firstLastUser = idToFirstLastCache.get(id);
      if (firstLastUser == null || firstLastUser.getLastChecked() < stale) {
        toAskFor.add(id);
      }
    });
    return toAskFor;
  }

  public String getUserChosenID(int userid) {
    Map<Integer, FirstLastUser> firstLastFor = getFirstLastFor(Collections.singleton(userid));
    FirstLastUser firstLastUser = firstLastFor.get(userid);
    return firstLastUser == null ? null : firstLastUser.getUserID();
  }

  /**
   * adds gender
   *
   * @param dominoUser
   * @return
   */
  private MiniUser getMini(DBUser dominoUser) {
    boolean admin = isAdmin(dominoUser);

    mitll.hlt.domino.shared.model.user.User.Gender gender = dominoUser.getGender();
    MiniUser miniUser = new MiniUser(
        dominoUser.getDocumentDBID(),
        0,  // age
        isGenderMale(gender),
        MiniUser.Gender.valueOf(gender.name()),
        dominoUser.getUserId(),
        admin);

    //   logger.info("getMini for " + dominoUser);
    {
      AccountDetail acctDetail = dominoUser.getAcctDetail();
      long now = System.currentTimeMillis();

      long time = acctDetail == null ?
          now :
          acctDetail.getCrTime() == null ? now : acctDetail.getCrTime().getTime();

      miniUser.setTimestampMillis(time);
    }
    miniUser.setFirst(dominoUser.getFirstName());
    miniUser.setLast(dominoUser.getLastName());

    return miniUser;
  }

  private boolean isGenderMale(mitll.hlt.domino.shared.model.user.User.Gender dominoUser) {
    return dominoUser == DMALE;
  }

  /**
   * TODO: Reset password works differently now...?
   *
   * @param resetKey
   * @return
   * @see mitll.langtest.server.rest.RestUserManagement#changePFor
   * @see mitll.langtest.server.rest.RestUserManagement#getUserIDForToken(String)
   */
  @Override
  @Deprecated
  public User getUserWithResetKey(String resetKey) {
    logger.warn("no reset key! " + resetKey);
    return null;//convertOrNull(dao.getByReset(resetKey));
  }

  /**
   * @param userid assumes a valid user id
   * @return
   */
  public boolean isMale(int userid) {
    return lookupUser(userid).getGender() == mitll.hlt.domino.shared.model.user.User.Gender.Male;
//    User byID = getByID(userid);
//    return byID.isMale();
  }

  /**
   * @param user
   * @param newHashPassword
   * @param baseURL
   * @return
   * @see mitll.langtest.server.rest.RestUserManagement#changePFor
   * @deprecated
   */
  @Override
  public boolean changePassword(int user, String newHashPassword, String baseURL) {
    return savePasswordAndGetUser(user, "", newHashPassword, baseURL) != null;
  }

  /**
   * @param userId
   * @param userKey
   * @param newPassword
   * @param url
   * @return
   */
  public boolean changePasswordForToken(String userId, String userKey, String newPassword, String url) {
    return delegate.changePassword(userId, userKey, newPassword, url);
  }

  /**
   * @param user
   * @param currentHashPass
   * @param newHashPass
   * @param baseURL
   * @return
   * @see mitll.langtest.server.services.UserServiceImpl#changePasswordWithCurrent
   */
  public boolean changePasswordWithCurrent(int user, String currentHashPass, String newHashPass, String baseURL) {
    return savePasswordAndGetUser(user, currentHashPass, newHashPass, baseURL) != null;
  }

  /**
   * For taking old legacy users and adding the new netprof 2 info to them.
   * <p>
   * user updates happen in domino UI too...
   *
   * @param toUpdate
   * @see OpenUserServiceImpl#addUser
   */
  @Override
  public void update(User toUpdate) {
    DBUser dbUser = lookupUser(toUpdate.getID());

    if (dbUser != null) {
      dbUser.setEmail(toUpdate.getEmail());
      dbUser.setFirstName(toUpdate.getFirst());
      dbUser.setLastName(toUpdate.getLast());
      String affiliation = toUpdate.getAffiliation();
      //logger.info("update Set affiliation '" + affiliation + "' for " + toUpdate.getID());
      dbUser.setAffiliation(affiliation);
      setGender(toUpdate, dbUser);

      updateUser(dbUser);
//      User byID = getByID(toUpdate.getID());
//      logger.info("after update\n" + byID);
    } else {
      logger.error("huh? couldn't find user to update " + toUpdate.getID() + "\n" + toUpdate);
    }
  }

  private void setGender(User toUpdate, DBUser dbUser) {
    MiniUser.Gender realGender = toUpdate.getRealGender();

    mitll.hlt.domino.shared.model.user.User.Gender gender =
        mitll.hlt.domino.shared.model.user.User.Gender.valueOf(realGender.name());
    dbUser.setGender(gender);
  }

  /**
   * @param updateUser
   * @return
   * @see mitll.langtest.server.database.copy.UserCopy#checkMatchingGender
   */
  @Override
  public SResult<ClientUserDetail> updateUser(DBUser updateUser) {
    if (updateUser.getFirstName() == null) updateUser.setFirstName("");
    if (updateUser.getLastName() == null) updateUser.setLastName("");
    if (updateUser.getEmail().isEmpty()) updateUser.setEmail("UNSET");


    if (updateUser.getUserId() == null) logger.error("no user id for " + updateUser);
    if (updateUser.getUserId().isEmpty()) logger.error("empty user id for " + updateUser);
    if (updateUser.getFirstName() == null) logger.error("no first  for " + updateUser);
    if (updateUser.getLastName() == null) logger.error("no last for " + updateUser);
    if (updateUser.getEmail() == null) logger.error("no email for " + updateUser);
    if (updateUser.getEmail().isEmpty()) logger.error("updateUser empty email for " + updateUser);
    if (updateUser.getPrimaryGroup() == null) logger.error("no primary group for " + updateUser);
    if (updateUser.getAffiliation() == null) logger.warn("updateUser no affiliation for " + updateUser);
    else if (updateUser.getAffiliation().isEmpty()) {
      //logger.warn("updateUser empty affiliation for " + updateUser);
    }

    ClientUserDetail clientUserDetail1 = getClientUserDetail(updateUser);

    {
      String affiliation = clientUserDetail1.getAffiliation();
      if (affiliation == null || affiliation.isEmpty()) {
        // logger.warn("client user affilation = '" + affiliation + "' for " + clientUserDetail1);
      }
    }

    SResult<ClientUserDetail> clientUserDetailSResult = delegate.updateUser(adminUser, clientUserDetail1);

/*
    ClientUserDetail clientUserDetail = clientUserDetailSResult.get();

    if (clientUserDetail != null) {
      String affiliation = clientUserDetail.getAffiliation();
      if (affiliation == null || affiliation.isEmpty()) {
        logger.warn("after " + affiliation + " for " + clientUserDetail);
      }
    }
    logger.info("after " + clientUserDetailSResult);*/

    return clientUserDetailSResult;
  }

  private boolean isValidAsEmail(String text) {
    return text.trim().toUpperCase().matches(VALID_EMAIL);
  }

  /**
   * @param user
   * @param url
   * @return
   * @see OpenUserServiceImpl#resetPassword
   */
  @Override
  public boolean forgotPassword(String user, String url) {
    DBUser next = getDBUser(user);

    if (next == null) {
      logger.warn("forgotPassword - can't find user " + user);
      return false;
    } else {
      if (next.getPrimaryGroup() == null) {
        next.setPrimaryGroup(getGroup());
      }

      logger.info("forgotPassword users for " + user + " : " + next);

      ClientUserDetail clientUserDetail1 = null;
      try {
        String email = next.getEmail();
        if (!isValidAsEmail(email)) {
          logger.error("forgotPassword huh? email " + email + " not valid?");
        }

        clientUserDetail1 = delegate.forgotPassword(next,
            getClientUserDetail(next),
            url);

        logger.info("forgotPassword forgotPassword users for " + user + " : " + clientUserDetail1);
      } catch (Exception e) {
        logger.error("Got " + e, e);
      }

      return clientUserDetail1 != null;
    }
  }

  @Override
  public DBUser getDominoAdminUser() {
    return dominoAdminUser;
  }

  public void setUserProjectDAO(IUserProjectDAO userProjectDAO) {
    this.userProjectDAO = userProjectDAO;
  }

  public void setProjectManagement(IProjectManagement projectManagement) {
    this.projectManagement = projectManagement;
  }
}
