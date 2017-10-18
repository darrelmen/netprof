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
import com.mongodb.client.MongoCollection;
import mitll.hlt.domino.server.user.*;
import mitll.hlt.domino.server.util.*;
import mitll.hlt.domino.shared.common.FilterDetail;
import mitll.hlt.domino.shared.common.FindOptions;
import mitll.hlt.domino.shared.common.SResult;
import mitll.hlt.domino.shared.model.user.*;
import mitll.hlt.json.JSONSerializer;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.Report;
import mitll.langtest.server.database.analysis.Analysis;
import mitll.langtest.server.database.security.NPUserSecurityManager;
import mitll.langtest.server.services.UserServiceImpl;
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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.include;
import static mitll.hlt.domino.server.user.MongoUserServiceDelegate.USERS_C;
import static mitll.hlt.domino.server.util.ServerProperties.CACHE_ENABLED_PROP;
import static mitll.langtest.shared.user.Kind.*;

/**
 * Store user info in domino tables.
 */
public class DominoUserDAOImpl extends BaseUserDAO implements IUserDAO {
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

  private IUserServiceDelegate delegate;
  private MyMongoUserServiceDelegate myDelegate;

  private Mongo pool;

  private final Map<String, Kind> roleToKind = new HashMap<>();
  /**
   * get the admin user.
   *
   * @see #ensureDefaultUsers
   */
  private mitll.hlt.domino.shared.model.user.User adminUser;
  private mitll.hlt.domino.shared.model.user.User dominoImportUser;
  private DBUser dominoAdminUser;
  private Ignite ignite = null;

  private JSONSerializer serializer;

  /**
   * @param database
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
   */
  public DominoUserDAOImpl(Database database) {
    super(database);

    populateRoles();

    try {
      connectToMongo(database, database.getServerProps().getProps());
    } catch (Exception e) {
      logger.error("Couldn't connect to mongo - is it running and accessible? " + e, e);
      throw e;
    }
  }

  private void connectToMongo(Database database, Properties props) throws MongoTimeoutException {
    pool = Mongo.createPool(new DBProperties(props));

    if (pool != null) {
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
      logger.debug("DominoUserDAOImpl made delegate " + delegate.getClass());
      logger.debug("DominoUserDAOImpl ignite = " + ignite);
      logger.debug("DominoUserDAOImpl isCacheEnabled = " + dominoProps.isCacheEnabled());
      myDelegate = makeMyServiceDelegate();//dominoProps.getUserServiceProperties(), mailer, pool, serializer);

      dominoAdminUser = delegate.getAdminUser();
    } else {
      logger.error("DominoUserDAOImpl couldn't connect to user service - no pool!\n\n");
    }
  }

/*
  public static final IUserServiceDelegate makeServiceDelegate(ServerProperties props,
                                                               Mailer mailer, Mongo mongoCP, JSONSerializer serializer, Ignite ignite) {
    MongoUserServiceDelegate d = null;
    switch (props.getUserServiceProperties().serviceType) {
      case LDAP:
        d = new LDAPUserServiceDelegate(props.getUserServiceProperties(), mailer, props.getAcctTypeName(), mongoCP);
        break;
      case Mongo:
        d = (ignite != null && props.isCacheEnabled()) ?
            new CachingMongoUserService(props.getUserServiceProperties(), mailer, props.getAcctTypeName(), mongoCP, ignite) :
            new MongoUserServiceDelegate(props.getUserServiceProperties(), mailer, props.getAcctTypeName(), mongoCP);
        break;
    }
    d.initializeDAOs(serializer);
    logger.info("Initialized user service of type {}", d.getClass().getSimpleName());
    return d;
  }
*/

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
    if (pool != null) {
      logger.info("closing connection to " + pool);
      pool.closeConnection();
    }
    if (ignite != null) {
      ignite.close();
    }
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
        Group group = getPrimaryGroup(PRIMARY);
        adminUser.setPrimaryGroup(group);
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
    this.importUser = getOrAdd(IMPORT_USER, "Import", "User", Kind.CONTENT_DEVELOPER);
    this.defaultUser = getOrAdd(DEFAULT_USER1, "Default", "User", Kind.AUDIO_RECORDER);
    this.defaultMale = getOrAdd(DEFAULT_MALE_USER, "Default", "Male", Kind.AUDIO_RECORDER);
    this.defaultFemale = getOrAdd(DEFAULT_FEMALE_USER, "Default", "Female", Kind.AUDIO_RECORDER);

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
        logger.info("addAndGet note : " + clientUserDetail.getUserId() + " gender is " + clientUserDetail.getGender());
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

  /**
   * Need a group - just use the first one.
   *
   * @return
   */
  @NotNull
  public Group getGroup() {
    List<Group> groups = delegate.getGroupDAO().searchGroups("");
    Group primaryGroup = groups.isEmpty() ? null : groups.iterator().next();

    if (primaryGroup == null) { //defensive
      logger.warn("\n\n\ngetGroup making a new group...?\n\n\n");
      primaryGroup = getPrimaryGroup(PRIMARY);
    }
    return primaryGroup;
  }

  private Map<String,Group> nameToGroup = new HashMap<>();

  /**
   * Cache secondary group so don't have to search for it.
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

    LocalDateTime thirtyYearsFromNow = LocalDateTime.now().plusYears(30);
    Date out = Date.from(thirtyYearsFromNow.atZone(ZoneId.systemDefault()).toInstant());

    SResult<ClientGroupDetail> name1 = groupDAO1.doAdd(adminUser, new ClientGroupDetail(name, "name", 365, 24 * 365, out, adminUser));

    Group group = null;
    if (name1.isError()) {
      logger.error("couldn't make " + name1);
    } else {
      group = name1.get();
    }
    return group;
  }

  @NotNull
  private Group getPrimaryGroup(String name) {
    LocalDateTime f = LocalDateTime.now().plusYears(30);
    Date out = Date.from(f.atZone(ZoneId.systemDefault()).toInstant());
    return new Group(name, name + "Group", 365, 24 * 365, out, adminUser);
  }

  private ClientUserDetail getClientUserDetail(DBUser dbUser) {
    return new ClientUserDetail(dbUser, new AccountDetail());
  }

  /**
   * @param id
   * @param newHashedPassword
   * @param baseURL
   * @return
   * @seex BaseUserDAO#updateUser
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

  @Override
  public String isValidEmail(String email) {
    List<DBUser> users = getDbUsers(getEmailFilter(email));
    return users.isEmpty() ? null : users.get(0).getUserId();
  }

  /**
   * @param filterDetails
   * @return
   * @see #getIDForUserAndEmail
   */
  private List<DBUser> getDbUsers(Set<FilterDetail<UserColumn>> filterDetails) {
    FindOptions<UserColumn> opts = new FindOptions<>(filterDetails);
    List<DBUser> users = delegate.getUsers(-1, opts);
    //  logger.info("getDbUsers " + opts + " = " + users);
    return users;
  }

  private Set<FilterDetail<UserColumn>> getEmailFilter(String emailH) {
    Set<FilterDetail<UserColumn>> filterDetails = new HashSet<>();
    filterDetails.add(new FilterDetail<>(UserColumn.Email, emailH, FilterDetail.Operator.EQ));
    return filterDetails;
  }

  @Override
  public Integer getIDForUserAndEmail(String user, String emailH) {
    Set<FilterDetail<UserColumn>> emailFilter = getEmailFilter(emailH);
    addUserID(user, emailFilter);

    List<DBUser> users = getDbUsers(emailFilter);
    return users.isEmpty() ? null : users.get(0).getDocumentDBID();
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
   * @see mitll.langtest.server.services.UserServiceImpl#loginUser
   */
  public User loginUser(String userId,
                        String attemptedTxtPass,
                        String userAgent,
                        String remoteAddr,
                        String sessionID) {
    String encodedCurrPass = getUserCredentials(userId);

    logger.info("loginUser '" + userId + "' pass num chars " + attemptedTxtPass.length() + " existing credentials " + encodedCurrPass);

    DBUser loggedInUser =
        delegate.loginUser(
            userId,
            attemptedTxtPass,
            remoteAddr,
            userAgent,
            sessionID);

    if (loggedInUser == null) {
      myDelegate.isMatch(encodedCurrPass, attemptedTxtPass);
    }
    return getUser(loggedInUser);
  }

/*
  private void getUserFields() {
    delegate.getUserFields();
  }
*/

  /**
   * @param userID
   * @param encodedPassword
   * @return
   * @seex #getUserIfMatch
   * @see mitll.langtest.server.database.copy.UserCopy#copyUsers
   */
  public boolean isMatchingPassword(String userID, String encodedPassword) {
//  logger.info("isMatchingPassword '" + id + "' and dominoPassword hash '" + encodedPassword.length() + "'");
    String dominoPassword = getUserCredentials(userID);

    long then = System.currentTimeMillis();
    boolean match = myDelegate.isMatch(dominoPassword, encodedPassword);
    long now = System.currentTimeMillis();

    long diff = now - then;
//    if (diff > 50) {
//      logger.warn("isMatchingPassword : took " + diff + " to check for password match.");
//    }
    if (dominoPassword != null && (match || dominoPassword.equals(encodedPassword))) {//dominoPassword.equals(encodedPassword)) {//netProfDelegate.isPasswordMatch(user.getID(), encodedPassword) || magicMatch) {
      if (diff > 100) {
        logger.warn("getUserIfMatch match in of " + dominoPassword + " vs encoded " + encodedPassword.length() +
            " match " + match + " took " + diff + " millis");
      }

      return true;
    } else {
      logger.warn("getUserIfMatch no match in db " + dominoPassword + " vs encoded " + encodedPassword.length() + " took " + diff);
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
    Document user = users().find(query).projection(include(PASS_F)).first();

    if (user != null) {
      return user.getString(PASS_F);
    } else {
      logger.warn("getUserCredentials : User not found in DB! Query: " + query);
    }
    return null;
  }

  private MongoCollection<Document> users() {
    return pool.getMongoCollection(USERS_C);
  }

  /**
   * @param userID
   * @return
   * @seex mitll.langtest.server.database.copy.CopyToPostgres#copyUsers
   */
  @Override
  public User getUserByID(String userID) {
    mitll.hlt.domino.shared.model.user.DBUser dominoUser = getDBUser(userID);
    if (dominoUser == null) {
      logger.warn("getUserByID no user by '" + userID + "'");
    } else {
      logger.info("found " + userID);
    }

    return getUser(dominoUser);
  }

  @Override
  public DBUser getDBUser(String userID) {
    return delegate.getDBUser(userID);
  }

  private User getUser(DBUser dominoUser) {
    return dominoUser == null ? null : toUser(dominoUser);
  }

  /**
   * @param id
   * @return
   * @see NPUserSecurityManager#getUserForID
   */
  public User getByID(int id) {
    return getUser(lookupUser(id));
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
  public ClientUserDetail toClientUserDetail(User user, String projectName,  Group group) {
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
    user.setPermissions(permissionSet);

//    logger.info("\ttoUser return " + user);
    return user;
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
    Set<String> roleAbbreviations = dominoUser.getRoleAbbreviations();
    return isAdmin(roleAbbreviations);
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
      seen.add(kind);
      permissionSet.addAll(User.getInitialPermsForRole(kind));
      //    logger.info(userId + " has " + role);
    }
    return seen;
  }

  private final Set<String> unknownRoles = new HashSet<>();

  @NotNull
  private Kind getKindForRole(String firstRole) {
    if (firstRole.equals("PoM")) firstRole = Kind.PROJECT_ADMIN.getRole();
    Kind kind = roleToKind.get(firstRole);
    if (kind == null) {
      try {
        kind = Kind.valueOf(firstRole.toUpperCase());
        // shouldn't need this
        logger.debug("getUserKind lookup by NetProF user role " + firstRole);
      } catch (IllegalArgumentException e) {
        Kind kindByName = getKindByName(firstRole);
        if (kindByName == null) {
          if (!firstRole.startsWith("ILR")) {
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
   * @see mitll.langtest.server.database.audio.SlickAudioDAO#getAudioAttributesByProjectThatHaveBeenChecked(int)
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
   * TODO: try to avoid?
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
    FindOptions<UserColumn> opts = new FindOptions<>();
    FilterDetail<UserColumn> netProf = new FilterDetail<>(UserColumn.DLIApplications, "NetProf", FilterDetail.Operator.RegEx);
    opts.addFilter(netProf);
    List<DBUser> users = delegate.getUsers(-1, opts);
    long now = System.currentTimeMillis();
    if (now - then > 20) logger.warn("getAll took " + (now - then) + " to get " + users.size() + " users");
    return users;
  }

/*
  private List<DBUser> getAllByGender(boolean isMale) {
    long then = System.currentTimeMillis();
    logger.warn("getAll calling get all users");
    Set<FilterDetail<UserColumn>> filterDetails = new HashSet<>();

    UserColumn col = new UserColumn("Gender", true, true, 20, ITableColumnEnum.Alignment.Left);
    filterDetails.add(new FilterDetail<>(UserColumn.Email, MiniUser.Gender.Male.name(), FilterDetail.Operator.EQ));

    getDbUsers();
    List<DBUser> users = delegate.getUsers(-1, new FindOptions<>());
    long now = System.currentTimeMillis();
    if (now - then > 20) logger.warn("getAll took " + (now - then) + " to get " + users.size() + " users");
    return users;
  }
*/

  /**
   * @return
   * @see UserServiceImpl#getKindToUser
   * @deprecated
   */
/*  public Map<User.Kind, Collection<MiniUser>> getMiniByKind() {
    Map<User.Kind, Collection<MiniUser>> kindToUsers = new HashMap<>();
    for (DBUser s : getAll()) {
      User.Kind key = getRole(s);//User.Kind.valueOf(s.kind());
      Collection<MiniUser> miniUsers = kindToUsers.get(key);
      if (miniUsers == null) kindToUsers.put(key, miniUsers = new ArrayList<>());
      miniUsers.add(getMini(s));
    }

    for (Collection<MiniUser> perKind : kindToUsers.values()) {
      Collections.sort((ArrayList<MiniUser>) perKind, (o1, o2) -> -1 * Long.valueOf(o1.getTimestampMillis()).compareTo(o2.getTimestampMillis()));
    }

    return kindToUsers;
  }*/

  /**
   * gets ROLE from DBUser
   *
   * @return
   * @paramx s
   */
/*
  private User.Kind getRole(DBUser s) {
    return getUserKind(s, new HashSet<>());
  }
*/
  @Override
  public MiniUser getMiniUser(int userid) {
    DBUser byID = lookupUser(userid);
//    logger.info("getMiniUser " + userid);
    return byID == null ? null : getMini(byID);
  }

  public Map<Integer, FirstLastUser> getFirstLastUsers() {
    Collection<List<Object>> userFields = delegate.getUserFields("_id", "userId", "firstName", "lastName");

    Map<Integer, FirstLastUser> idToFirstLast = new HashMap<>();
    for (List<Object> userField : userFields) {
      int i = 0;
      Object o = userField.get(i++);
      Integer o1 = o instanceof Integer ? (Integer) o : ((Double) o).intValue();
      String o2 = (String) userField.get(i++);
      String o3 = (String) userField.get(i++);
      String o4 = (String) userField.get(i++);
      idToFirstLast.put(o1, new FirstLastUser(o1, o2, o3, o4));
    }
    return idToFirstLast;
  }

  public String getUserChosenID(int userid) {
    DBUser byID = lookupUser(userid);
    return byID == null ? null : byID.getUserId();
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

  private boolean isMale(DBUser dominoUser) {
    return isGenderMale(dominoUser.getGender());
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

  public boolean isMale(int userid) {
    return getByID(userid).isMale();
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
   * user updates happen in domino UI...
   *
   * @param toUpdate
   * @see UserServiceImpl#addUser
   */
  @Override
  public void update(User toUpdate) {
    DBUser dbUser = lookupUser(toUpdate.getID());

    if (dbUser != null) {
      dbUser.setEmail(toUpdate.getEmail());
      dbUser.setFirstName(toUpdate.getFirst());
      dbUser.setLastName(toUpdate.getLast());
      dbUser.setAffiliation(toUpdate.getAffiliation());
      MiniUser.Gender realGender = toUpdate.getRealGender();

      mitll.hlt.domino.shared.model.user.User.Gender gender =
          mitll.hlt.domino.shared.model.user.User.Gender.valueOf(realGender.name());
      dbUser.setGender(gender);
      updateUser(dbUser);
    }
  }

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
    if (updateUser.getEmail().isEmpty()) logger.error("empty email for " + updateUser);
    if (updateUser.getPrimaryGroup() == null) logger.error("no primary group for " + updateUser);

    return delegate.updateUser(adminUser, getClientUserDetail(updateUser));
  }

  private boolean isValidAsEmail(String text) {
    return text.trim().toUpperCase().matches("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$");
  }

  /**
   * @param user
   * @param url
   * @return
   * @see UserServiceImpl#resetPassword
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
      ClientUserDetail clientUserDetail = getClientUserDetail(next);

      logger.info("forgotPassword users for " + user + " : " + clientUserDetail);

      ClientUserDetail clientUserDetail1 = null;
      try {
        if (!isValidAsEmail(clientUserDetail.getEmail())) {
          logger.error("huh? email " + clientUserDetail.getEmail() + " not valid?");
        }

        clientUserDetail1 = delegate.forgotPassword(next,
            clientUserDetail,
            url);

        logger.info("forgotPassword forgotPassword users for " + user + " : " + clientUserDetail1);

      } catch (Exception e) {
        logger.error("Got " + e, e);
      }

      return clientUserDetail1 != null;
    }
  }
}
