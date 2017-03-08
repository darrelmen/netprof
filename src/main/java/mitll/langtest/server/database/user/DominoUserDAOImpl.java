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
import mitll.hlt.domino.server.user.IGroupDAO;
import mitll.hlt.domino.server.user.IUserServiceDelegate;
import mitll.hlt.domino.server.user.MongoGroupDAO;
import mitll.hlt.domino.server.user.UserServiceFacadeImpl;
import mitll.hlt.domino.server.util.*;
import mitll.hlt.domino.shared.common.FilterDetail;
import mitll.hlt.domino.shared.common.FindOptions;
import mitll.hlt.domino.shared.common.SResult;
import mitll.hlt.domino.shared.model.user.*;
import mitll.hlt.json.JSONSerializer;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.Report;
import mitll.langtest.server.database.analysis.Analysis;
import mitll.langtest.server.services.UserServiceImpl;
import mitll.langtest.shared.user.MiniUser;
import mitll.langtest.shared.user.User;
import org.apache.ignite.Ignite;
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

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.include;
import static mitll.hlt.domino.server.user.MongoUserServiceDelegate.USERS_C;
import static mitll.langtest.shared.user.User.Kind.*;

/**
 * Store user info in domino tables.
 */
public class DominoUserDAOImpl extends BaseUserDAO implements IUserDAO {
  private static final Logger logger = LogManager.getLogger(DominoUserDAOImpl.class);
  private static final mitll.hlt.domino.shared.model.user.User.Gender DMALE = mitll.hlt.domino.shared.model.user.User.Gender.Male;
  private static final mitll.hlt.domino.shared.model.user.User.Gender DFEMALE = mitll.hlt.domino.shared.model.user.User.Gender.Female;
  private static final mitll.hlt.domino.shared.model.user.User.Gender UNSPECIFIED = mitll.hlt.domino.shared.model.user.User.Gender.Unspecified;
  private static final String PRIMARY = "primary";
  private static final String DEFAULT_AFFILIATION = "";//"OTHER";
  public static final String MALE = "male";
  private static final String UID_F = "userId";
  private static final String PASS_F = "pass";
  public static final String LOCALHOST = "127.0.0.1";

  private IUserServiceDelegate delegate;
  private MyMongoUserServiceDelegate myDelegate;

  private Mongo pool;

  private final Map<String, User.Kind> roleToKind = new HashMap<>();
  /**
   * get the admin user.
   *
   * @see #ensureDefaultUsers
   */
  private mitll.hlt.domino.shared.model.user.User adminUser;
  private mitll.hlt.domino.shared.model.user.User dominoImportUser;
  private DBUser dominoAdminUser;
  private Ignite ignite = null;

  /**
   * @param database
   * @paramx dbConnection
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
   */
  public DominoUserDAOImpl(Database database) {
    super(database);

    populateRoles();

    Properties props = database.getServerProps().getProps();

    try {
      connectToMongo(database, props);
    } catch (Exception e) {
      logger.error("Couldn't connect to mongo - is it running and accessible? " + e);
    }
  }

  private void connectToMongo(Database database, Properties props) throws MongoTimeoutException {
    pool = Mongo.createPool(new DBProperties(props));

    if (pool != null) {
      JSONSerializer serializer = Mongo.makeSerializer();
      Mailer mailer = new Mailer(new MailerProperties(props));
      ServerProperties dominoProps =
          new ServerProperties(props, "1.0", "demo", "0", "now");

      dominoProps.updateProperty(ServerProperties.APP_NAME_PROP, database.getServerProps().getAppTitle());
      //String appName = dominoProps.getAppName();
      //     logger.info("DominoUserDAOImpl app name is " + appName);

      ignite = null;
      if (/*dominoProps.isCacheEnabled() ||*/ true) {
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
      logger.debug("DominoUserDAOImpl made delegate");
      myDelegate = makeMyServiceDelegate();//dominoProps.getUserServiceProperties(), mailer, pool, serializer);

      dominoAdminUser = delegate.getAdminUser();
    } else {
      logger.error("DominoUserDAOImpl couldn't connect to user service - no pool!\n\n");
    }
  }

  private void populateRoles() {
    for (User.Kind kind : User.Kind.values()) {
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

    return Ignition.start(cfg);
  }

  private MyMongoUserServiceDelegate makeMyServiceDelegate() {  return new MyMongoUserServiceDelegate();  }

  @Override
  public void ensureDefaultUsers() {
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

  /**
   * public for test access... for now
   *
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
   */
  public void ensureDefaultUsersLocal() {
    this.defectDetector = getOrAdd(DEFECT_DETECTOR, "Defect", "Detector", User.Kind.QAQC);
    this.beforeLoginUser = getOrAdd(BEFORE_LOGIN_USER, "Before", "Login", User.Kind.STUDENT);
    this.importUser = getOrAdd(IMPORT_USER, "Import", "User", User.Kind.CONTENT_DEVELOPER);
    this.defaultUser = getOrAdd(DEFAULT_USER1, "Default", "User", User.Kind.AUDIO_RECORDER);
    this.defaultMale = getOrAdd(DEFAULT_MALE_USER, "Default", "Male", User.Kind.AUDIO_RECORDER);
    this.defaultFemale = getOrAdd(DEFAULT_FEMALE_USER, "Default", "Female", User.Kind.AUDIO_RECORDER);

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
    if (user.getGender() != UNSPECIFIED) {
      logger.info("addAndGet going in " + user.getGender() + " for user '" + user.getUserId() + "'");
    }

    SResult<ClientUserDetail> clientUserDetailSResult1 = delegate.migrateUser(user, encodedPass);
    boolean b = !clientUserDetailSResult1.isError();
    if (!b) {
      logger.error("\n\n\naddUserToMongo didn't set password for " + user.getUserId() + " : " +
          clientUserDetailSResult1.getResponseMessage());
      return null;
    } else {
      ClientUserDetail clientUserDetail = clientUserDetailSResult1.get();
      if (clientUserDetail.getGender() == UNSPECIFIED) {
        logger.info("huh? " +clientUserDetail.getUserId() + " is " + clientUserDetail.getGender());
      }
      return clientUserDetail;
    }
  }

  /**
   * @param user
   * @param url
   * @param sendEmail
   * @return
   * @paramx freeTextPassword
   * @see BaseUserDAO#addUser
   * @see #addAndGet(ClientUserDetail, String)
   */
  private SResult<ClientUserDetail> addUserToMongo(ClientUserDetail user,
                                                   //String freeTextPassword,
                                                   String url,
                                                   boolean sendEmail) {
//    logger.info("adding user " + user);
    SResult<ClientUserDetail> clientUserDetailSResult = delegate.addUser(
        sendEmail ? user : adminUser,
        user,
        url);
    //logger.info("addUserToMongo Got back " + clientUserDetailSResult);

    return clientUserDetailSResult;
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
   * @paramx freeTextPassword
   * @paramx passwordH
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
                     User.Kind kind,

                     String emailH,
                     String email,
                     String device,
                     String first,
                     String last,
                     String url,
                     String affiliation) {
    // Timestamp now = new Timestamp(System.currentTimeMillis());
    mitll.hlt.domino.shared.model.user.User.Gender gender1 = mitll.hlt.domino.shared.model.user.User.Gender.valueOf(gender.name());
    ClientUserDetail updateUser = new ClientUserDetail(
        userID,
        first,
        last,
        email,
        affiliation,
        gender1,//.equalsIgnoreCase(MALE) ? DMALE : DFEMALE,
        Collections.singleton(kind.getRole()),
        getGroup()
    );

    AccountDetail acctDetail = new AccountDetail();
    updateUser.setAcctDetail(acctDetail);
    acctDetail.setCrTime(new Date());
    SResult<ClientUserDetail> clientUserDetailSResult = addUserToMongo(updateUser,
        url,
        true);

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
  private Group getGroup() {
    List<Group> groups = delegate.getGroupDAO().searchGroups("");
    Group primaryGroup = groups.isEmpty() ? null : groups.iterator().next();

    if (primaryGroup == null) { //defensive
      logger.warn("\n\n\ngetGroup making a new group...?\n\n\n");
      primaryGroup = getPrimaryGroup(PRIMARY);
    }
    return primaryGroup;
  }

  private Group getGroupOrMake(String name) {
    List<Group> groups = delegate.getGroupDAO().searchGroups(name);
    Group group = groups.isEmpty() ? null : groups.iterator().next();

    if (group == null) { //defensive
      logger.warn("getGroupOrMake making a new group " + name);
      //  group = getPrimaryGroup(name);

      IGroupDAO groupDAO = delegate.getGroupDAO();

      MongoGroupDAO groupDAO1 = (MongoGroupDAO) groupDAO;

      LocalDateTime f = LocalDateTime.now().plusYears(30);
      Date out = Date.from(f.atZone(ZoneId.systemDefault()).toInstant());

      SResult<ClientGroupDetail> name1 = groupDAO1.doAdd(adminUser, new ClientGroupDetail(name, "name", 365, 24 * 365, out, adminUser));

      if (name1.isError()) {
        logger.error("couldn't make " + name1);
      } else group = name1.get();
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
    Set<FilterDetail<UserColumn>> filterDetails = getEmailFilter(email);
    List<DBUser> users = getDbUsers(filterDetails);
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
    return loggedInUser == null ? null : toUser(loggedInUser);
  }

  /**
   * TODOx : don't use password hash - use free text instead
   * <p>
   * TODO : (somewhere else) allow admin masquerade login - e.g. gvidaver/steve - with gvidaver's password logs you in as steve
   *
   * @param id
   * @param encodedPassword
   * @return
   * @seex mitll.langtest.server.database.copy.UserCopy#copyUsers
   * @see mitll.langtest.server.rest.RestUserManagement#gotHasUser
   */
//  @Override
//  public User getStrictUserWithPass(String id, String encodedPassword) {
//    return getUserIfMatch(getUserByID(id), id, encodedPassword);
//  }

  /**
   * @param user
   * @param id
   * @param encodedPassword
   * @return
   * @see #getStrictUserWithPass
   */
/*  private User getUserIfMatch(User user, String id, String encodedPassword) {
    if (user != null) {
      return getUserIfMatchPass(user, id, encodedPassword);
    } else {
      logger.info("getUserIfMatch '" + id + "' is an unknown user");
      return null;
    }
  }*/

  /**
   * @param user
   * @param id
   * @param encodedPassword
   * @return
   * @seex #getUserIfMatch
   * @see mitll.langtest.server.database.copy.UserCopy#copyUsers
   */
  public User getUserIfMatchPass(User user, String id, String encodedPassword) {
    logger.info("getUserIfMatchPass '" + id + "' and dominoPassword hash '" + encodedPassword.length() + "'");
    String dominoPassword = getUserCredentials(id);

    long then = System.currentTimeMillis();
    boolean match = myDelegate.isMatch(dominoPassword, encodedPassword);
    long now = System.currentTimeMillis();

    long diff = now - then;
//    if (diff > 50) {
//      logger.warn("getUserIfMatchPass : took " + diff + " to check for password match.");
//    }
    if (dominoPassword != null && (match || dominoPassword.equals(encodedPassword))) {//dominoPassword.equals(encodedPassword)) {//netProfDelegate.isPasswordMatch(user.getID(), encodedPassword) || magicMatch) {
      logger.warn("getUserIfMatch match in of " + dominoPassword + " vs encoded " + encodedPassword.length() + " match " + match + " took " + diff + " millis");
      boolean isadmin = database.getServerProps().getAdmins().contains(user.getUserID());
      user.setAdmin(isadmin);
      return user;
    } else {
      logger.warn("getUserIfMatch no match in db " + dominoPassword + " vs encoded " + encodedPassword.length() + " took " + diff);
      return null;
    }
  }

  /**
   * @param userId
   * @return
   * @see #getUserIfMatchPass(User, String, String)
   */
  private String getUserCredentials(String userId) {
    return getUserCredentials(eq(UID_F, userId));
  }

  private MongoCollection<Document> users() {
    return pool.getMongoCollection(USERS_C);
  }

  private String getUserCredentials(Bson query) {
    Document user = users().find(query).projection(include(PASS_F)).first();

    if (user != null) {
      return user.getString(PASS_F);
    } else {
      logger.warn("getUserCredentials : User not found in DB! Query: " + query);
    }
    return null;
  }

  /**
   * @param id
   * @return
   * @seex mitll.langtest.server.database.copy.CopyToPostgres#copyUsers
   */
  @Override
  public User getUserByID(String id) {
    mitll.hlt.domino.shared.model.user.DBUser dominoUser = delegate.getDBUser(id);
    if (dominoUser == null) {
      logger.warn("getUserByID no user by '" + id + "'");
    }
    return dominoUser == null ? null : toUser(dominoUser);
  }

  /**
   * @param id
   * @return
   * @see mitll.langtest.server.database.security.UserSecurityManager#getUserForID
   */
  public User getByID(int id) {
    DBUser dbUser = lookupUser(id);
    return dbUser == null ? null : toUser(dbUser);
  }

  @Override
  public User getUserWhere(int userid) { return getByID(userid);  }

  private DBUser lookupUser(int id) {   return delegate.lookupDBUser(id);  }

  @Override
  public List<User> getUsers() {
    return toUsers(getAll());
  }

  /**
   * adds permissions
   *
   * @param all
   * @return
   */
  private List<User> toUsers(List<DBUser> all) {
    List<User> copy = new ArrayList<>();
    for (DBUser s : all) {
      copy.add(toUser(s));
    }
    return copy;
  }

  /**
   * Convert a netprof user into a domino user.
   *
   * @param user
   * @param projectName
   * @return
   * @see mitll.langtest.server.database.copy.UserCopy#addUser
   */
  public ClientUserDetail toClientUserDetail(User user, String projectName) {
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

    User.Kind userKind = user.getUserKind();

    Set<String> roleAbbreviations = Collections.singleton(userKind.getRole());
    // logger.info("toClientUserDetail " + user.getUserID() + " role is " + roleAbbreviations + " email " +email);

    mitll.hlt.domino.shared.model.user.User.Gender gender = userKind ==
        STUDENT ? UNSPECIFIED :
        user.isMale() ? DMALE : DFEMALE;

    if (gender == UNSPECIFIED) {
      logger.info("toClientUserDetail for " + user.getID() + " '" + userID + "' "+ user.getUserKind() + " gender is unspecified.");
    }
    else {
      logger.info("toClientUserDetail for " + user.getID() + " '" + userID + "' "+ user.getUserKind() + " gender is "+gender);

    }

    ClientUserDetail clientUserDetail = new ClientUserDetail(
        userID,
        first,
        last,
        email,
        DEFAULT_AFFILIATION,
        gender,
        roleAbbreviations,
        getGroup()
    );

    if (clientUserDetail.getGender() != gender) logger.error("huh? wrote "+ gender + " but got back " +clientUserDetail.getGender());

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
   * adds gender -
   *
   * @param dominoUser
   * @return
   * @see #getByID
   */
  private User toUser(mitll.hlt.domino.shared.model.user.DBUser dominoUser) {
    // logger.info("toUser " + dominoUser);
    boolean admin = isAdmin(dominoUser);
    long creationTime = System.currentTimeMillis();
    String device = "";

    AccountDetail acctDetail = dominoUser.getAcctDetail();
    if (acctDetail != null) {
      device = acctDetail.getDevice().toString();
      if (acctDetail.getCrTime() != null) {
        creationTime = acctDetail.getCrTime().getTime();
      } else {
        logger.info("toUser no cr time on " + acctDetail);
      }
    } else {
      logger.info("toUser no acct detail for " + dominoUser.getDocumentDBID());
    }

    String email = dominoUser.getEmail();

 //   logger.debug("toUser : user " + dominoUser.getUserId() + " email " + email);//, new Exception());

    Set<User.Permission> permissionSet = new HashSet<>();
//    String emailHash = email == null ? "" : isValidEmailGrammar(email) ? Md5Hash.getHash(email) : email;
    mitll.hlt.domino.shared.model.user.User.Gender gender = dominoUser.getGender();
    User user = new User(
        dominoUser.getDocumentDBID(),
        99,//dominoUser.age(),
        gender.equals(mitll.hlt.domino.shared.model.user.User.Gender.Male) ? 0 : 1,
        0,
        "",
        "",
        "",
        "",//dominoUser.dialect(),
        dominoUser.getUserId(),
        dominoUser.isActive(),
        admin,
        Collections.emptyList(),
        getUserKind(dominoUser, permissionSet),
        email,
        //dominoUser.emailhash(),
        device,//        dominoUser.device(),
        "",//dominoUser.resetpasswordkey(),
        creationTime,
        dominoUser.getAffiliation());

    try {
      user.setRealGender(User.Gender.valueOf(gender.name()));
    } catch (IllegalArgumentException e) {
      logger.error("couldn't parse gender " + gender.name());
    }

    user.setFirst(dominoUser.getFirstName());
    user.setLast(dominoUser.getLastName());
    user.setPermissions(permissionSet);

//    logger.info("\ttoUser return " + user);
    return user;
  }

  private boolean isValidEmailGrammar(String text) {
    return text.trim().toUpperCase().matches("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$");
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
    return roleAbbreviations.contains(ADMIN.getRole()) || roleAbbreviations.contains(PROJECT_ADMIN.getRole());
  }

  /**
   * convert dominos role to NetProF kind and permissions.
   *
   * @param dominoUser
   * @return
   * @see
   */
  private User.Kind getUserKind(mitll.hlt.domino.shared.model.user.User dominoUser, Set<User.Permission> permissionSet) {
    Set<String> roleAbbreviations = dominoUser.getRoleAbbreviations();

    User.Kind kindToUse = User.Kind.STUDENT;
    Set<User.Kind> seen = new HashSet<>();

    //    logger.warn("getUserKind user " + userId + " has multiple roles - choosing first one... " + roleAbbreviations.size());
    for (String role : roleAbbreviations) {
      User.Kind kind = getKindForRole(role);
      seen.add(kind);
      permissionSet.addAll(User.getInitialPermsForRole(kind));
      //    logger.info(userId + " has " + role);
    }

    // teacher trumps others... for the moment
    // need to have ordering over roles...?
    if (seen.contains(User.Kind.TEACHER)) {
      kindToUse = User.Kind.TEACHER;
    }

    // String firstRole = roleAbbreviations.iterator().next();

    //User.Kind kind = getKindForRole(firstRole);
    return kindToUse;
  }

  @NotNull
  private User.Kind getKindForRole(String firstRole) {
    if (firstRole.equals("PoM")) firstRole = User.Kind.PROJECT_ADMIN.getRole();
    User.Kind kind = roleToKind.get(firstRole);
    if (kind == null) {
      try {
        kind = User.Kind.valueOf(firstRole.toUpperCase());
        // shouldn't need this
        logger.debug("getUserKind lookup by NetProF user role " + firstRole);
      } catch (IllegalArgumentException e) {
        User.Kind kindByName = getKindByName(firstRole);
        if (kindByName == null) {
          if (!firstRole.startsWith("ILR")) {
            logger.warn("getUserKind no user for " + firstRole);
          }
          kind = User.Kind.STUDENT;
        } else {
          kind = kindByName;
        }
      }
    }
    return kind;
  }

  @Nullable
  private User.Kind getKindByName(String firstRole) {
    for (User.Kind testKind : User.Kind.values()) {
      if (testKind.getName().equalsIgnoreCase(firstRole)) {
        return testKind;
      }
    }
    return null;
  }

  /**
   * TODO : get device info from domino user
   * <p>
   * For Reporting.
   *
   * @return
   * @see Report#getReport
   */
  @Override
  public List<User> getUsersDevices() {

    return Collections.emptyList();//dao.getUsersFromDevices());
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
   * @see mitll.langtest.server.database.audio.SlickAudioDAO#getAudioAttributesByProject(int)
   */
  @Override
  public synchronized Map<Integer, MiniUser> getMiniUsers() {
    long now = System.currentTimeMillis();

    int userCount = delegate.getUserCount();
    logger.debug("getMiniUsers user count is " + userCount, new Exception());

    if (miniUserCache == null || (now - lastCache) > 60 * 60 * 1000 || lastCount != userCount) {
      Map<Integer, MiniUser> idToUser = new HashMap<>();

      for (DBUser s : getAll()) {
        idToUser.put(s.getDocumentDBID(), getMini(s));
      }

      miniUserCache = idToUser;

      if (!miniUserCache.isEmpty()) {
        lastCache = now;
        long then = System.currentTimeMillis();
        lastCount = userCount;
        long now2 = System.currentTimeMillis();

        if (now2 - then > 10) {
          logger.warn("getMiniUsers took " + (now2 - then) + " millis to get count of db users = " + miniUserCache.size());
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
   * @deprecated
   */
  public List<DBUser> getAll() {
    long then = System.currentTimeMillis();
    logger.warn("getAll calling get all users");
    List<DBUser> users = delegate.getUsers(-1, null);
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
  public Map<User.Kind, Collection<MiniUser>> getMiniByKind() {
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
  }

  /**
   * gets ROLE from DBUser
   *
   * @param s
   * @return
   */
  private User.Kind getRole(DBUser s) {
    return getUserKind(s, new HashSet<>());
  }

  @Override
  public MiniUser getMiniUser(int userid) {
    DBUser byID = lookupUser(userid);
    return byID == null ? null : getMini(byID);
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

    MiniUser miniUser = new MiniUser(
        dominoUser.getDocumentDBID(),
        0,  // age
        isMale(dominoUser),
        dominoUser.getUserId(),
        admin);

    //   logger.info("getMini for " + dominoUser);

    AccountDetail acctDetail = dominoUser.getAcctDetail();
    long now = System.currentTimeMillis();

    long time = acctDetail == null ?
        now :
        acctDetail.getCrTime() == null ? now : acctDetail.getCrTime().getTime();

    miniUser.setTimestampMillis(time);
    miniUser.setFirst(dominoUser.getFirstName());
    miniUser.setLast(dominoUser.getLastName());

    return miniUser;
  }

  private boolean isMale(DBUser dominoUser) {
    return dominoUser.getGender() == DMALE;
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

  public Map<Integer, User> getUserMapFromUsers(boolean getMale, List<DBUser> all) {
    Map<Integer, User> idToUser = new HashMap<>();
    all
        .stream()
        .filter(dbUser ->
            getMale ?
                dbUser.getGender() == mitll.hlt.domino.shared.model.user.User.Gender.Male :
                dbUser.getGender() == mitll.hlt.domino.shared.model.user.User.Gender.Female)
        .forEach(dbUser -> idToUser.put(dbUser.getDocumentDBID(), toUser(dbUser)));
    return idToUser;
  }

  public boolean isMale(int userid) {
    return getByID(userid).isMale();
  }

  /**
   * @param user
   * @param newHashPassword
   * @param baseURL
   * @return
   * @see mitll.langtest.server.services.UserServiceImpl#changePFor
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
   * We don't support this UI anymore.
   *
   * @return
   */
/*  @Deprecated
  public Map<User.Kind, Integer> getCounts() {
    // Map<String, Integer> counts = dao.getCounts();
    Map<User.Kind, Integer> ret = new HashMap<>();
 *//*   for (Map.Entry<String, Integer> pair : counts.entrySet()) {
      ret.put(User.Kind.valueOf(pair.getKey()), pair.getValue());
    }
 *//*
    return ret;
  }*/

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

      mitll.hlt.domino.shared.model.user.User.Gender gender = mitll.hlt.domino.shared.model.user.User.Gender.valueOf(realGender.name());
      dbUser.setGender(gender);
      delegate.updateUser(adminUser, getClientUserDetail(dbUser));
    }
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
    DBUser next = delegate.getDBUser(user);

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
