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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mongodb.MongoTimeoutException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
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
import mitll.langtest.server.database.audio.AudioDAO;
import mitll.langtest.server.database.audio.BaseAudioDAO;
import mitll.langtest.server.database.project.Project;
import mitll.langtest.server.database.project.IProjectManagement;
import mitll.langtest.server.database.security.NPUserSecurityManager;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.server.services.OpenUserServiceImpl;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.user.*;
import mitll.langtest.shared.user.LoginResult;
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
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.include;
import static mitll.hlt.domino.server.ServerInitializationManager.*;
import static mitll.hlt.domino.server.user.MongoUserServiceDelegate.USERS_C;
import static mitll.hlt.domino.server.util.ServerProperties.CACHE_ENABLED_PROP;
import static mitll.hlt.domino.server.util.ServerProperties.EVT_URL_BASE_MAP_PROP;
import static mitll.langtest.server.database.project.Project.MANDARIN;
import static mitll.langtest.shared.user.Kind.*;

/**
 * Store user info in domino tables.
 */
public class DominoUserDAOImpl extends BaseUserDAO implements IUserDAO, IDominoUserDAO {
  private static final Logger logger = LogManager.getLogger(DominoUserDAOImpl.class);

  private static final mitll.hlt.domino.shared.model.user.User.Gender DMALE = mitll.hlt.domino.shared.model.user.User.Gender.Male;
  private static final mitll.hlt.domino.shared.model.user.User.Gender DFEMALE = mitll.hlt.domino.shared.model.user.User.Gender.Female;
  private static final mitll.hlt.domino.shared.model.user.User.Gender UNSPECIFIED = mitll.hlt.domino.shared.model.user.User.Gender.Unspecified;
  private static final String NETPROF1 = "Netprof";
  private static final String DEFAULT_AFFILIATION = "";

  private static final String UID_F = "userId";
  private static final String PASS_F = "pass";
  private static final String LOCALHOST = "127.0.0.1";
  private static final boolean USE_DOMINO_IGNITE = true;
  private static final boolean USE_DOMINO_CACHE = false;

  private static final String TCHR = "TCHR";

  private static final int CACHE_TIMEOUT = 1;
  private static final String UNSET = "UNSET";

  private static final boolean DEBUG_STUDENTS = false;
  private static final boolean DEBUG_TEACHERS = false;
  private static final String F_LAST = "F. Last";


  private static final List<String> DOMAINS =
      Arrays.asList(
          "dliflc.edu",
          "mail.mil", //146
          "us.af.mil",
          "gmail.com",
          "yahoo.com",
          "mail.mil",
          "ll.mit.edu",
          "hotmail.com",
          "outlook.com",
          "pom.dliflc.edu",
          "us.af.mil",
          "icloud.com",
          "aol.com",
          "live.com",
          "comcast.net"
      );

  private static final String NETPROF2 = "netprof";
  /**
   * Should be consistent with DOMINO.
   * Actually it's all lower case.
   */
  public static final String NETPROF = NETPROF2;
  private static final Set<String> APPLICATION_ABBREVIATIONS = Collections.singleton(NETPROF);
  private static final String LYDIA_01 = "Lydia01";
  private static final String PO_M = "PoM";
  private static final String ILR = "ILR";
  private static final String USER = "User";
  private static final String DEFAULT = "Default";
  private static final int EST_NUM_USERS = 8000;
  /**
   * @see #isValidAsEmail
   */
  private static final String VALID_EMAIL = "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$";

/*
  private static final String EMAIL_REGEX =
      "^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*" +
          "@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+" +
          "(?:[A-Z]{2}|com|org|net|edu|gov|mil|biz|info|mobi|name|aero|asia|jobs|museum)\\b";
*/

  private static final long STALE_DUR = 24L * 60L * 60L * 1000L;
 // private static final boolean SWITCH_USER_PROJECT = false;
  private static final String ACTIVE = "active";
  private static final String EMAIL = "email";
  public static final String UNKNOWN = "Unknown";
  public static final String MALE = "Male";
  private static final String FEMALE = "Female";

  /**
   * If false, don't use email to set the initial user password via email.
   *
   * @see #addUser(int, MiniUser.Gender, int, String, String, String, String, String, boolean, Collection, Kind, String, String, String, String, String, String, String)
   */
  private final boolean addUserViaEmail;

  /**
   *
   */
  private IUserServiceDelegate delegate = null;
  private MyMongoUserServiceDelegate myDelegate;

  private MyUserService myUserService;

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

  private Group primaryGroup = null;
  private static final boolean DEBUG_USER_CACHE = false;

  /**
   * @see #lookupUser
   * @see #refreshCacheFor
   */
  private final LoadingCache<Integer, DBUser> idToDBUser = CacheBuilder.newBuilder()
      .maximumSize(10000)
      .expireAfterWrite(CACHE_TIMEOUT, TimeUnit.MINUTES)
      .build(
          new CacheLoader<Integer, DBUser>() {
            @Override
            public DBUser load(Integer key) {
              if (DEBUG_USER_CACHE) logger.info("idToDBUser Load " + key);
              DBUser dbUser = delegate.lookupDBUser(key);
              if (dbUser == null) {
                logger.warn("idToDBUser no db user with id " + key);
                dbUser = getStandIn(key);
              }
              return dbUser;
            }
          });

  /**
   * Try to deal in a way that doesn't collapse multiple users into one.
   * @param key
   * @return
   */
  @NotNull
  private DBUser getStandIn(Integer key) {
    return new DBUser(key,
        "User_" + key,
        "Unknown",
        "Unknown",
        "",
        "",
        UNSPECIFIED,
        new HashSet<>(),

        new Group(),
        new HashSet<Group>(),
        null,
        new HashSet<>());
  }

  /**
   * @see #getByID
   * @see #refreshCacheFor
   */
  private final LoadingCache<Integer, User> idToUser = CacheBuilder.newBuilder()
      .maximumSize(10000)
      .expireAfterWrite(CACHE_TIMEOUT, TimeUnit.MINUTES)
      .build(
          new CacheLoader<Integer, User>() {
            @Override
            public User load(Integer key) {
              //    logger.info("idToUser Load " + key);
              return getUser(lookupUser(key));
            }
          });

  private final ConcurrentHashMap<Integer, FirstLastUser> idToFirstLastCache = new ConcurrentHashMap<>(EST_NUM_USERS);


  private final LoadingCache<Integer, FirstLastUser> idToFirstLastUser = CacheBuilder.newBuilder()
      .maximumSize(10000)
      .expireAfterWrite(CACHE_TIMEOUT, TimeUnit.MINUTES)
      .build(
          new CacheLoader<Integer, FirstLastUser>() {
            @Override
            public User load(Integer key) {
              //    logger.info("idToUser Load " + key);
              return getUser(lookupUser(key));
            }
          });

  /**
   * @param database
   * @see mitll.langtest.server.database.DatabaseImpl#connectToDatabases(PathHelper, ServletContext)
   */
  public DominoUserDAOImpl(Database database, ServletContext servletContext) {
    super(database);
    long then = System.currentTimeMillis();
    addUserViaEmail = database.getServerProps().addUserViaEmail();

    populateRoles();

    Object attribute = servletContext == null ? null : servletContext.getAttribute(USER_SVC);
    Properties props = database.getServerProps().getProps();

    if (attribute != null) {
      delegate = (IUserServiceDelegate) attribute;
      pool = (Mongo) servletContext.getAttribute(MONGO_ATT_NAME);
      logger.info("DominoUserDAOImpl got pool reference " + pool);
      serializer = (JSONSerializer) servletContext.getAttribute(JSON_SERIALIZER);

      makeUserService(database, props);
      {
        long now = System.currentTimeMillis();
        if (now - then > 30) {
          logger.info("took " + (now - then) + " millis to make user service");
        }
      }
      doAfterGetDelegate();
      {
        long now = System.currentTimeMillis();
        if (now - then > 100) {
          logger.info("took " + (now - then) + " millis to do after delegate");
        }
      }
    } else {
      if (servletContext != null) {
        Enumeration<String> attributeNames = servletContext.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
          logger.info("domino user dao: no user service (" + USER_SVC + ")" + attributeNames.nextElement());
        }
      }
      usedDominoResources = false;
      try {
        new Thread(() -> connectToMongo(database, props), "connectToMongo").start();
      } catch (Exception e) {
        logger.error("Couldn't connect to mongo - is it running and accessible? " + e, e);
        throw e;
      }
      long now = System.currentTimeMillis();
      if (now - then > 100) {
        logger.info("took " + (now - then) + " millis to connect to mongo");
      }
    }

    long now = System.currentTimeMillis();
    if (now - then > 100) {
      logger.info("took " + (now - then) + " millis to start user dao");
    }
  }


  private void makeUserService(Database database, Properties props) {
    ServerProperties dominoProps = getDominoProps(database, props);
    UserServiceProperties userServiceProperties = dominoProps.getUserServiceProperties();
    myUserService = new MyUserService(userServiceProperties,
        new Mailer(new MailerProperties(props)), dominoProps.getAcctTypeName(), pool,
        new MailSupport(database.getServerProps()));

    myUserService.initializeDAOs(serializer);
  }

  /**
   * Fall back if somehow we can't get domino services from servlet context?
   *
   * @param database
   * @param props
   * @throws MongoTimeoutException
   */
  private void connectToMongo(Database database, Properties props) throws MongoTimeoutException {
    noServletContextSetup(database, props);
    doAfterGetDelegate();
  }

  private void noServletContextSetup(Database database, Properties props) {
    props.setProperty(EVT_URL_BASE_MAP_PROP, "a,b");
    pool = Mongo.createPool(new DBProperties(props));
    serializer = Mongo.makeSerializer();
    logger.info("noServletContextSetup connectToMongo : OK made serializer " + serializer);
    Mailer mailer = new Mailer(new MailerProperties(props));
    ServerProperties dominoProps = getDominoProps(database, props);
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
    makeUserService(database, props);

    logger.info("noServletContextSetup made" +
        "\ndelegate " + delegate.getClass() +
        "\nignite   " + ignite +
        "\nisCacheEnabled " + dominoProps.isCacheEnabled());
  }

  @NotNull
  private ServerProperties getDominoProps(Database database, Properties props) {
    props.setProperty(CACHE_ENABLED_PROP, "" + USE_DOMINO_CACHE);
    ServerProperties dominoProps =
        new ServerProperties(props, "1.0", "demo", "0", "now");

    dominoProps.updateProperty(ServerProperties.APP_NAME_PROP, database.getServerProps().getAppTitle());
    return dominoProps;
  }

  private void doAfterGetDelegate() {
    myDelegate = makeMyServiceDelegate();
    dominoAdminUser = delegate.getAdminUser();
    ensureDefaultUsers();
  }

  /**
   * DNS LOOKUP.
   *
   * @param host
   * @return
   */
  @Override
  public boolean isValidServer(String host) {
    boolean contains = DOMAINS.contains(host);
    if (contains) {
      return true;
    } else {
      InetAddress byName;
      try {
        byName = InetAddress.getByName(host);
      } catch (Exception e) {
        logger.warn("for " + host + "got " + e);
        return false;
      }
      logger.info(host + " = " + byName);
      return true;
    }
  }

  private void populateRoles() {
    for (Kind kind : Kind.values()) {
      roleToKind.put(kind.getRole(), kind);
    }
  }

  @Override
  public void close() {
    if (!usedDominoResources) {
//      logger.info("closing connection to " + pool, new Exception());
      if (pool != null) pool.closeConnection();
    }
    if (!usedDominoResources && ignite != null) {
      ignite.close();
    }

    if (pool != null) {
      logger.info("close : closing connection to " + pool);
      pool.closeConnection();
    }

    if (ignite != null) {
      ignite.close();
    }
/*    if (pool != null) {
      logger.info("close : closing connection to " + pool);
      pool.closeConnection();
      if (ignite != null) ignite.close();
    }*/
  }

  /**
   * Really a student - sometimes can be student with additional permissions!
   *
   * @param userIDFromSessionOrDB
   * @return
   */
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

  @Override
  public boolean isAdmin(int userid) {
    User user = getByID(userid);
    if (user == null) {
      logger.error("huh? no user " + userid);
      return false;
    } else {
      return user.isAdmin();
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
      logger.error("ensureDefaultUsers : no delegate - couldn't connect to Mongo!");
    } else {
      ensureDefaultUsersLocal();
      adminUser = delegate.getUser(dominoAdminUser.getUserId());//BEFORE_LOGIN_USER);
/*      logger.info("ensureDefaultUsers got admin user " + adminUser +
          " has roles " + adminUser.getRoleAbbreviationsString());*/

      if (adminUser.getPrimaryGroup() == null) {
        logger.warn("\n\n\nensureDefaultUsers no group for " + adminUser);
      }

      dominoImportUser = delegate.getUser(IMPORT_USER);
    //  DBUser defaultDBUser = delegate.lookupDBUser(defaultUser);
    }
  }

  /**
   * public for test access... for now
   *
   * @see #ensureDefaultUsers
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
   */
  private void ensureDefaultUsersLocal() {
    // logger.info("ensureDefaultUsersLocal --- ");
    this.defectDetector = getOrAdd(DEFECT_DETECTOR, "Defect", "Detector", Kind.QAQC);
    this.beforeLoginUser = getOrAdd(BEFORE_LOGIN_USER, "Before", "Login", Kind.STUDENT);
    this.importUser = getOrAdd(IMPORT_USER, "Import", USER, Kind.CONTENT_DEVELOPER);
    this.defaultUser = getOrAdd(DEFAULT_USER1, DEFAULT, USER, Kind.AUDIO_RECORDER);
    this.defaultMale = getOrAdd(DEFAULT_MALE_USER, DEFAULT, MALE, Kind.AUDIO_RECORDER);
    this.defaultFemale = getOrAdd(DEFAULT_FEMALE_USER, DEFAULT, FEMALE, Kind.AUDIO_RECORDER);
    //  logger.info("ensureDefaultUsersLocal defaultUser " + defaultUser);

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
   * @param user
   * @param url
   * @param sendEmail
   * @return
   * @see #addUser(int, MiniUser.Gender, int, String, String, String, String, String, boolean, Collection, Kind, String, String, String, String, String, String, String)
   */
  private MyUserService.LoginResult addUserToMongoNoEmail(ClientUserDetail user, String url, boolean sendEmail) {
    logger.info("addUserToMongoNoEmail " + user + " send email " + sendEmail);
    return myUserService.addUserNoEmail(sendEmail ? user : adminUser, user, url);
  }

  /**
   * Create a user and send email to the new user email account.
   * Also we have an option to not sent an email and use the reset password token directly.
   *
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
  public LoginResult addUser(int age,
                             MiniUser.Gender gender,
                             int experience,
                             String userAgent,
                             String trueIP,
                             String nativeLang,
                             String dialect,
                             String userID,
                             boolean enabled,
                             Collection<Permission> permissions,
                             Kind kind,

                             String emailH,
                             String email,
                             String device,
                             String first,
                             String last,
                             String url,
                             String affiliation) {

    ClientUserDetail updateUser = new ClientUserDetail(
        userID,
        first,
        last,
        email,
        affiliation,
        mitll.hlt.domino.shared.model.user.User.Gender.valueOf(gender.name()),
        Collections.singleton(kind.getRole()),
        getGroup(),

        APPLICATION_ABBREVIATIONS
    );

    setCreationTime(updateUser);

    boolean useUsualLogin = shouldUseUsualDominoEmail(email);

    if (useUsualLogin) {
      logger.info("addUser : usual login " + useUsualLogin + " for " + email);
    }

    MyUserService.LoginResult loginResult =
        useUsualLogin ?
            loginViaEmail(url, updateUser) :
            addUserToMongoNoEmail(updateUser, url, true);

    SResult<ClientUserDetail> clientUserDetailSResult = loginResult.result;
    if (clientUserDetailSResult == null) {
      return new LoginResult(-1, ""); // password error?
    } else {
      ClientUserDetail clientUserDetail = clientUserDetailSResult.get();
      String emailToken = loginResult.emailToken;
      if (emailToken.isEmpty()) {
        logger.error("addUser (useUsualLogin = " + useUsualLogin +
            ") : no email token for " + clientUserDetail);
      }
      return new LoginResult(clientUserDetail == null ? -1 : clientUserDetail.getDocumentDBID(),
          useUsualLogin ? "" : emailToken).setResultType(LoginResult.ResultType.Added);
    }
  }

  private void setCreationTime(ClientUserDetail updateUser) {
    AccountDetail acctDetail = new AccountDetail();
    updateUser.setAcctDetail(acctDetail);
    acctDetail.setCrTime(new Date());
  }

  private MyUserService.LoginResult loginViaEmail(String url, ClientUserDetail updateUser) {
    return new MyUserService.LoginResult(addUserToMongo(updateUser, url, true), "");
  }

  /**
   * Need a group - use "Netprof"
   *
   * @return
   * @see #addUser(int, MiniUser.Gender, int, String, String, String, String, String, boolean, Collection, Kind, String, String, String, String, String, String, String)
   * @see #forgotPassword
   */
  @NotNull
  public Group getGroup() {
    if (primaryGroup == null) {
      logger.info("getGroup : Search groups for " + NETPROF1);

      List<Group> groups = delegate.getGroupDAO().searchGroups(NETPROF1);
      if (groups.isEmpty()) {
        logger.warn("getGroup no groups for " + NETPROF1 + "?");
        groups = delegate.getGroupDAO().searchGroups(NETPROF2);
      }
      if (groups.isEmpty()) {
        logger.warn("getGroup no groups for " + NETPROF2 + "?");

        groups = delegate.getGroupDAO().searchGroups("");

        if (groups.isEmpty()) logger.warn("no groups at all?");
        else {
          logger.info("OK, groups is " + groups);
        }
      }

      if (groups.isEmpty()) {
        primaryGroup = null;
      } else {
        primaryGroup = groups.get(0);
        logger.warn("getGroup OK using " + primaryGroup);
      }

      if (primaryGroup == null) { //defensive
        logger.warn("\n\n\ngetGroup need a new group...?\n\n\n");
        //     primaryGroup = makePrimaryGroup(PRIMARY);
      }
    }

    return primaryGroup;
  }

  /* @NotNull
   private Group makePrimaryGroup(String name) {
     Date out = Date.from(getZonedDateThirtyYears().toInstant());
     String description = name + "Group";
     UserDescriptor adminUser = this.adminUser;
     return new Group(
         name,
         description,
         365,
         24 * 365,
         out,
         adminUser,
         NETPROF2);
   }
 */
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
      logger.info("getGroupOrMake : Search groups for " + name);
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
    logger.warn("\n\ngetGroupOrMake making a new group " + name);
    MongoGroupDAO groupDAO1 = (MongoGroupDAO) delegate.getGroupDAO();

    Date out = Date.from(getZonedDateThirtyYears().toInstant());
    SResult<ClientGroupDetail> name1 = groupDAO1.doAdd(adminUser,
        new ClientGroupDetail(name, "name", 365, 24 * 365, out,
            5,
            adminUser, NETPROF2));

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
   * @see #changePassword
   * @see #changePasswordWithCurrent
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
   * @see OpenUserServiceImpl#forgotUsername
   */
  @Override
  public List<String> isValidEmail(String email) {
    List<DBUser> users = getDbUsers(getEmailFilter(email));
    List<User> npUsers = new ArrayList<>(users.size());
    users.forEach(dbUser -> {
      User user = getUser(dbUser);
      if (user.isHasAppPermission()) npUsers.add(user);
    });
    List<String> ids = new ArrayList<>(users.size());
    npUsers.forEach(dbUser -> ids.add(dbUser.getUserID()));
    return ids;
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
    List<DBUser> users = getDbUsers(getEmailFilter(emailH));
    return users.isEmpty() ? null : users.get(0).getFullName();
  }

  /**
   * @param filterDetails
   * @return
   * @see #getIDForUserAndEmail
   */
  private List<DBUser> getDbUsers(Set<FilterDetail<UserColumn>> filterDetails) {
    List<DBUser> users = delegate.getUsers(-1, new FindOptions<>(filterDetails));
    //   logger.info("getDbUsers " + filterDetails + " = " + users);
    return users;
  }

  @NotNull
  public <T> Map<Integer, T> getJustTeachers(Map<Integer, T> activeSince) {
    logger.info("getJustTeachers active " + activeSince.size());

    Set<Integer> teacherIDs = getTeacherIDs();

    logger.info("getJustTeachers teacherIDs = " + teacherIDs.size());

    Map<Integer, T> justTeachers = new HashMap<>();

    activeSince.forEach((k, v) -> {
      if (teacherIDs.contains(k)) justTeachers.put(k, v);
    });

    logger.info("getJustTeachers justTeachers = " + teacherIDs.size());

    return justTeachers;
  }

  @NotNull
  public Set<Integer> getTeacherIDs() {
    return getTeachers().stream().map(UserDescriptor::getDocumentDBID).collect(Collectors.toSet());
  }

  private List<DBUser> getTeachers() {
    HashSet<FilterDetail<UserColumn>> filterDetails = new HashSet<>();
    FilterDetail<UserColumn> tchr = new FilterDetail<>(UserColumn.Roles, TCHR, FilterDetail.Operator.RegEx);
    filterDetails.add(tchr);
    List<DBUser> users = delegate.getUsers(-1, new FindOptions<>(filterDetails));
    logger.info("getTeachers " + tchr + " = " + users.size());
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
   * @see NPUserSecurityManager#getLoginResult
   * @see mitll.langtest.server.services.OpenUserServiceImpl#loginUser
   * @see mitll.langtest.client.user.SignInForm#gotLogin
   */
  public LoginResult loginUser(String userId,
                               String attemptedTxtPass,
                               String userAgent,
                               String remoteAddr,
                               String sessionID) {
    String encodedCurrPass = getUserCredentials(userId);
    logger.info("loginUser userid " + userId + " " + encodedCurrPass);

    String toUse = userId;

    boolean byEmail = false;
    if (encodedCurrPass == null && isValidAsEmail(userId)) {
      logger.info("loginUser userid " + userId + " is email ");
      List<String> userCredentialsEmail1 = getUsersWithThisEmail(userId);

      if (userCredentialsEmail1.size() > 1) {
        String mostRecentUserID = getMostRecentUserID(userCredentialsEmail1);
        if (!mostRecentUserID.isEmpty()) {
          toUse = mostRecentUserID;
          byEmail = true;
        }
      } else if (userCredentialsEmail1.size() == 1) {
        toUse = userCredentialsEmail1.get(0);
        byEmail = true;
      }
    }

    DBUser loggedInUser =
        delegate.loginUser(
            toUse,
            attemptedTxtPass,
            remoteAddr,
            userAgent,
            sessionID);

    User user = getUser(loggedInUser);
    LoginResult loginResult = new LoginResult(user, byEmail ? LoginResult.ResultType.Email : LoginResult.ResultType.Success).setUserID(toUse);

    logger.info("loginUser '" + userId + "'/'" + toUse +
        "' pass num chars " + attemptedTxtPass.length() +
        "\n\texisting credentials " + encodedCurrPass +
        "\n\tyielded              " + loggedInUser +
        "\n\tuser                 " + user +
        "\n\tloginResult          " + loginResult
    );

    return loginResult;
  }

  public String getMostRecentUserID(List<String> userCredentialsEmail1) {
    String latest = "";
    List<User> users = getUserWithNetprofPermission(userCredentialsEmail1);

    Optional<User> max = users.stream().max(Comparator.comparingLong(SimpleUser::getTimestampMillis));
    if (max.isPresent()) {
      User user = max.get();
      latest = user.getUserID();
      logger.info("getMostRecentUserID most recent user " + latest + " = " + new Date(user.getTimestampMillis()));
    }
    return latest;
  }

  @NotNull
  private List<User> getUserWithNetprofPermission(List<String> userCredentialsEmail1) {
    List<User> users = new ArrayList<>(userCredentialsEmail1.size());
    userCredentialsEmail1.forEach(userWithEmail -> {
      User userByID = getUserByID(userWithEmail);
      if (userByID != null && userByID.isHasAppPermission()) {
        users.add(userByID);
//        logger.info("\tgetMostRecentUserID user " + userByID + " = " + new Date(userByID.getTimestampMillis()));
      }
    });
    return users;
  }

/*
  public boolean isValidEmailRegex(String text) {
    return text.trim().toLowerCase().matches(EMAIL_REGEX);
  }*/

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
      //  logger.info("isMatchingPassword return remembered match for " + userID + " in " + dominoToEncodedToMatch.size() + " and " + encodedToMatch.size());
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
      if (diff > 200) {
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
   * Match on email OK.
   *
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

  public List<String> getUsersWithThisEmail(String email) {
    Bson query = and(eq(EMAIL, email), eq(ACTIVE, true));
    FindIterable<Document> id = pool
        .getMongoCollection(USERS_C)
        .find(query)
        .projection(include(UID_F));

    MongoCursor<Document> iterator = id.iterator();

    List<String> matches = new ArrayList<>();
    for (; iterator.hasNext(); ) {
      matches.add(iterator.next().getString(UID_F));
    }

    logger.info("getUsersWithThisEmail : found " + matches.size() +
        //" (" + new HashSet<>(matches) + ")" +
        " accounts with email " + email);

    return matches;
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
      logger.info("getUserByID no user by '" + userID + "'");
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

  @Override
  public User getUserWhere(int userid) {
    return getByID(userid);
  }

  /**
   * FROM CACHE
   *
   * @param id
   * @return
   * @see NPUserSecurityManager#getUserForID
   */
  public User getByID(int id) {
    try {
      return idToUser.get(id);
    } catch (ExecutionException e) {
      logger.warn("getByID got " + e);
      return getUser(lookupUser(id));
    }
  }

  private User getUser(DBUser dominoUser) {
    return dominoUser == null ? null : toUser(dominoUser);
  }

  /**
   * Use a cache.
   * <p>
   * Keys age out at 10 minutes
   *
   * @param id
   * @return
   * @see #idToDBUser
   */
  private DBUser lookupUser(int id) {
    try {
      DBUser dbUser = idToDBUser.get(id);
      if (dbUser == null) {
        logger.warn("lookupUser can't find user by id " + id);
        //     dbUser = idToDBUser.get(defaultUser);
      }
      return dbUser;
    } catch (ExecutionException e) {
      logger.warn("lookupUser got " + e);
      return delegate.lookupDBUser(id);
    }
  }

  public String lookupUserId(int id) {
    return delegate.lookupUserId(id);
  }

  public void refreshCacheFor(int userid) {
    idToDBUser.refresh(userid);
    idToUser.refresh(userid);
  }

  @Override
  public List<User> getUsers() {
    return toUsers(getAll());
  }

  @Override
  public ReportUsers getReportUsers() {
//    long then = System.currentTimeMillis();
//    Map<Integer, ReportUser> reportUsersQuick = getReportUsersQuick();
//    long now = System.currentTimeMillis();
//    logger.info("took " + (now - then) +
//        " got " + reportUsersQuick.size());
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
    if (last == null) last = UNKNOWN;
    String email = user.getEmail();

    Kind userKind = user.getUserKind();

    Set<String> roleAbbreviations = Collections.singleton(userKind.getRole());
    // logger.info("toClientUserDetail " + user.getUserID() + " role is " + roleAbbreviations + " email " +email);

    mitll.hlt.domino.shared.model.user.User.Gender gender = getGender(user, userID, userKind);

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

  private mitll.hlt.domino.shared.model.user.User.Gender getGender(User user, String userID, Kind userKind) {
    boolean copyGender = user.getPermissions().contains(Permission.RECORD_AUDIO) ||
        user.getPermissions().contains(Permission.DEVELOP_CONTENT) ||
        userID.equalsIgnoreCase("FernandoM01");

    return userKind ==
        STUDENT && !copyGender ? UNSPECIFIED :
        user.isMale() ? DMALE : DFEMALE;
  }

  /**
   * Convert domino user into a netprof user.
   * <p>
   * adds gender - only binary (for now?)
   *
   * @param dominoUser
   * @return
   * @see #getByID
   * @see #getUser(DBUser)
   */
  private User toUser(mitll.hlt.domino.shared.model.user.DBUser dominoUser) {
    // logger.info("toUser " + dominoUser);
    long creationTime = System.currentTimeMillis();
    String device = "";

    AccountDetail acctDetail = dominoUser.getAcctDetail();
    int documentDBID = dominoUser.getDocumentDBID();
    if (acctDetail != null) {
      device = acctDetail.getDevice().toString();
      if (acctDetail.getCrTime() != null) {
        creationTime = acctDetail.getCrTime().getTime();
      } else {
        logger.warn("toUser no creation time on " + acctDetail);
      }
    } else {
      logger.warn("toUser no acct detail for " + documentDBID);
    }
    //   logger.debug("toUser : user " + dominoUser.getUserId() + " email " + email);//, new Exception());

    Set<Permission> permissionSet = new HashSet<>();
//    String emailHash = email == null ? "" : isValidEmailGrammar(email) ? Md5Hash.getHash(email) : email;
    boolean isMale = isMaleHardChoice(dominoUser);

    boolean hasAppPermission = isHasAppPermission(dominoUser);
    Kind userKind = getUserKind(dominoUser, permissionSet);
    String userId = dominoUser.getUserId();

    if (userKind != STUDENT) {
      if (DEBUG_TEACHERS) {
        logger.info("toUser : User #" + documentDBID + " " + userId + " is a " + userKind);
      }
    } else if (DEBUG_STUDENTS) {
      logger.info("toUser : User #" + documentDBID + " " + userId + " is a " + userKind + " with roles " + dominoUser.getRoleAbbreviations());
    }

    User user = new User(
        documentDBID,
        userId,
        isMale ? 0 : 1,
        isMale ? MiniUser.Gender.Male : MiniUser.Gender.Female,
        dominoUser.isActive(),
        isAdmin(dominoUser),
        userKind,
        dominoUser.getEmail(),
        device,
        creationTime,
        dominoUser.getAffiliation(),
        hasAppPermission);

    user.setFirst(dominoUser.getFirstName());
    user.setLast(dominoUser.getLastName());
    if (user.isAdmin()) {
      permissionSet.add(Permission.PROJECT_ADMIN); // a little redundant with isAdmin...
    }

 /*   if (user.isPoly()) {
      //logger.info("\n\n\ntoUser user " + user.getUserID() + " is a polyglot user.");
      handleAffiliationUser(dominoUser, permissionSet, user, true);
    } else if (user.isNPQ()) {
      handleAffiliationUser(dominoUser, permissionSet, user, false);
    } else {
//      logger.info("toUser  user " + user.getUserID() + " is not a polyglot user.");
    }*/
    user.setPermissions(permissionSet);
//    logger.info("\ttoUser return " + user);
    return user;
  }

  /**
   * @param dominoUser
   * @param permissionSet
   * @param user
   * @param isPoly
   * @see #toUser
   */
/*  private void handleAffiliationUser(DBUser dominoUser, Set<Permission> permissionSet, User user, boolean isPoly) {
    if (isPoly) permissionSet.add(Permission.POLYGLOT);

    int id = user.getID();
    int mostRecentByUser = userProjectDAO.getCurrentProjectForUser(id);

    if (mostRecentByUser == -1) {  // none yet...
      int projectAssignment = getProjectAssignment(dominoUser, id, isPoly);
      if (projectAssignment != -1) {
        logger.info("handlePolyglotUser 1 before poly " + user.getUserID() + " was #" + mostRecentByUser + " will now be #" + projectAssignment);
        userProjectDAO.upsert(id, projectAssignment);
      }
    } else if (SWITCH_USER_PROJECT) {
      int projectAssignment = getProjectAssignment(dominoUser, id, isPoly);
      if (projectAssignment != -1 && projectAssignment != mostRecentByUser) {
        logger.info("handlePolyglotUser 2 before poly " + user.getUserID() + " was #" + mostRecentByUser + " will now be #" + projectAssignment);
        userProjectDAO.setCurrentProjectForUser(id, projectAssignment);
      }
    }
  }*/

  /**
   * Get language from secondary group, then try to match language to a polyglot project.
   *
   * @param dominoUser
   * @param id
   * @param isPoly
   * @return
   * @see #handleAffiliationUser
   */
/*  private int getProjectAssignment(DBUser dominoUser, int id, boolean isPoly) {
    Collection<Group> secondaryGroups = dominoUser.getSecondaryGroups();
    int projID = -1;
    if (!secondaryGroups.isEmpty()) {
      Group next = secondaryGroups.iterator().next();
      Language languageMatchingGroup = getLanguageMatchingGroup(next);
      if (languageMatchingGroup != Language.UNKNOWN) {
        List<Project> collect = projectManagement.getMatchingProjects(languageMatchingGroup, isPoly);

        if (!collect.isEmpty()) {
          if (collect.size() > 1) {
            logger.info("getProjectAssignment found multiple projects ");
          }

          projID = collect.iterator().next().getID();
          logger.info("getProjectAssignment : match " + next + " to project " + projID);
        } else {
          logger.info("getProjectAssignment no polyglot project for " + languageMatchingGroup);
        }
      } else {
        logger.warn("getProjectAssignment no language matching group " + next);
      }
    } else {
      logger.info("getProjectAssignment no groups for user id " + id);
    }
    return projID;
  }*/

  /**
   * @param next
   * @return
   * @see #getProjectAssignment
   */
/*  private Language getLanguageMatchingGroup(Group next) {
    //logger.info("getLanguageMatchingGroup : found secondary " + next);
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
  }*/

  private Language getLanguage(String lang) {
    Language language = Language.UNKNOWN;
    try {

      String name = lang.toUpperCase();
      if (name.equalsIgnoreCase(MANDARIN)) name = Language.MANDARIN.name();
      language = Language.valueOf(name);
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
  private Kind getUserKind(mitll.hlt.domino.shared.model.user.User dominoUser, Set<Permission> permissionSet) {
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

  /**
   * @param permissionSet     populate this set
   * @param roleAbbreviations from these
   * @return
   */
  @NotNull
  private Set<Kind> setPermissions(Set<Permission> permissionSet, Set<String> roleAbbreviations) {
    Set<Kind> seen = new HashSet<>();
    //    logger.warn("getUserKind user " + userId + " has multiple roles - choosing first one... " + roleAbbreviations.size());
    for (String role : roleAbbreviations) {
      Kind kind = getKindForRole(role);
/*
      if (kind != STUDENT) {
        logger.info("setPermissions : has " + role + " -> " + kind);
      }
*/

//      else {
//        logger.info("setPermissions : has " + role + " -> " + kind);
//      }

      seen.add(kind);

      permissionSet.addAll(User.getInitialPermsForRole(kind));
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
        logger.info("getKindForRole lookup by NetProF user role " + firstRole);
      } catch (IllegalArgumentException e) {
        Kind kindByName = getKindByName(firstRole);
        if (kindByName == null) {
          if (!firstRole.startsWith(ILR)) {
            if (unknownRoles.contains(firstRole)) {
              logger.warn("getKindForRole no mapping for role " + firstRole + " default to student...");
            } else {
              logger.warn("getKindForRole no mapping for role " + firstRole +
                  " : now seen these unmapped roles " + unknownRoles + " default to student...");
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
  private List<ReportUser> getUsersDevices(List<ReportUser> users) {
    return users
        .stream()
        .filter(user -> user.getDevice() != null && user.getDevice().startsWith("i"))
        .collect(Collectors.toList());
  }


  /**
   * It seems like it's slow to get users out of domino users table, without ignite...
   * Maybe we should add ignite, but maybe we can avoid it for the time being?
   * <p>
   * At least every hour we go fetch the users again.
   *
   * @return
   * @see Analysis#getUserInfos
   * @see BaseAudioDAO#getAudioAttributesByProjectThatHaveBeenChecked(int, boolean)
   * @see AudioDAO#getResultsForQuery
   */
  @Override
  public synchronized Map<Integer, MiniUser> getMiniUsers() {
    logger.warn("should not be called ---\n\n\n\n");
    return new HashMap<>();
/*    long now = System.currentTimeMillis();

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
    }*/
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
    logger.info("getAll calling get all users");
    if (delegate == null) {
      logger.warn("\n\ngetAll delegate is null?\n\n");
    }

    long then = System.currentTimeMillis();
    List<DBUser> users = delegate == null ? Collections.emptyList() : delegate.getUsers(-1, getUserColumnFindOptions());
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

  public String getFirstInitialName(int userid) {
    Map<Integer, FirstLastUser> firstLastFor = getFirstLastFor(Collections.singleton(userid));
    return getFirstInitialName(firstLastFor.get(userid));
  }

  @Nullable
  private String getFirstInitialName(SimpleUser firstLastUser) {
    String s = firstLastUser == null ? null :
        (firstLastUser.getFirst().length() > 0 ?
            firstLastUser.getFirst().substring(0, 1) + ". " : "") +
            firstLastUser.getLast();

    // logger.info("getFirstInitialName Got " +userid + " " + firstLastUser + " : " + s);

    if (s != null && s.equalsIgnoreCase(F_LAST)) {
      s = firstLastUser.getUserID();
    }
    // logger.info("now Got " +userid + " " + firstLastUser + " : " + s);

    return s;
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
   * @param userid assumes a valid user id
   * @return
   */
  public boolean isMale(int userid) {
    return lookupUser(userid).getGender() == mitll.hlt.domino.shared.model.user.User.Gender.Male;
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
   * @param email
   * @return
   */
  public boolean changePasswordForToken(String userId, String userKey, String newPassword, String url, String email) {
    return shouldUseUsualDominoEmail(email) ?
        delegate.changePassword(userId, userKey, newPassword, url) :
        myUserService.changePassword(userId, userKey, newPassword, url);
  }

  @Override
  public boolean shouldUseUsualDominoEmail(String email) {
    boolean b = hasBlessedEmail(email);

    // logger.info("shouldUseUsualDominoEmail (addUserViaEmail = " + addUserViaEmail + ") '" + email + "' - " + b);

    return addUserViaEmail && !b;
  }

  @NotNull
  private boolean hasBlessedEmail(String email) {
    String lc = email.toLowerCase().trim();
    return DOMAINS.stream().anyMatch(lc::endsWith);
  }

  /**
   * @param user
   * @param currentHashPass
   * @param newHashPass
   * @param baseURL
   * @return
   * @see mitll.langtest.server.services.UserServiceImpl#changePasswordWithCurrent
   * @see ChangePasswordView#changePassword
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

  @Override
  public boolean addTeacherRole(int userid) {
    DBUser dbUser = lookupUser(userid);

    if (dbUser != null) {
      Set<String> roleAbbreviations = dbUser.getRoleAbbreviations();
      boolean hasTeacher = roleAbbreviations.contains(TCHR);
      if (hasTeacher) {
        logger.info("addTeacherRole user " + dbUser + " already has teacher role.");
        return false;
      } else {
        roleAbbreviations.add(TCHR);
        boolean b = doUpdate(dbUser);
        if (b) {
          refresh(userid);
        }
        else {
          logger.warn("addTeacherRole couldn't update " +dbUser);
        }
        return b;
      }

    } else {
      logger.error("addTeacherRole huh? couldn't find user to update " + userid);
      return false;
    }
  }

  private void refresh(int userid) {
    idToDBUser.refresh(userid);
    idToUser.refresh(userid);
    idToFirstLastUser.refresh(userid);
    refreshUserCache(Collections.singleton(userid));
  }

  @Override
  public boolean removeTeacherRole(int userid) {
    DBUser dbUser = lookupUser(userid);

    if (dbUser != null) {
      Set<String> roleAbbreviations = dbUser.getRoleAbbreviations();
      boolean hasTeacher = roleAbbreviations.contains(TCHR);
      if (hasTeacher) {
//        logger.info("user " + dbUser + " already has teacher role.");
        roleAbbreviations.remove(TCHR);

        boolean b = doUpdate(dbUser);
        if (b) {
          refresh(userid);
        }

        return b;
      } else {
        logger.warn("user " + dbUser + " doesn't have teacher role.");
        return false;
      }
    } else {
      logger.error("addTeacherRole huh? couldn't find user to update " + userid);
      return false;
    }
  }

  private boolean doUpdate(DBUser dbUser) {
    SResult<ClientUserDetail> clientUserDetailSResult = updateUser(dbUser);

    if (clientUserDetailSResult.isSuccess()) {
      return true;
    } else {
      logger.warn("doUpdate error " + clientUserDetailSResult);
      return false;
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
    if (updateUser.getEmail().isEmpty()) updateUser.setEmail(UNSET);

    if (updateUser.getUserId() == null) logger.error("no user id for " + updateUser);
    if (updateUser.getUserId().isEmpty()) logger.error("empty user id for " + updateUser);
    if (updateUser.getFirstName() == null) logger.error("no first  for " + updateUser);
    if (updateUser.getLastName() == null) logger.error("no last for " + updateUser);
    if (updateUser.getEmail() == null) logger.error("no email for " + updateUser);
    if (updateUser.getEmail().isEmpty()) logger.error("updateUser empty email for " + updateUser);
    if (updateUser.getPrimaryGroup() == null) logger.error("no primary group for " + updateUser);
    if (updateUser.getAffiliation() == null) logger.warn("updateUser no affiliation for " + updateUser);

//    else if (updateUser.getAffiliation().isEmpty()) {
//      //logger.warn("updateUser empty affiliation for " + updateUser);
//    }

    return delegate.updateUser(adminUser, getClientUserDetail(updateUser));
  }


  public boolean isValidAsEmail(String text) {
    return text.trim().toUpperCase().matches(VALID_EMAIL);
  }

  /**
   * @param user
   * @param url
   * @param optionalEmail
   * @return
   * @see OpenUserServiceImpl#resetPassword
   */
  @Override
  public boolean forgotPassword(String user, String url, String optionalEmail) {
    logger.info("forgotPassword " + user + " url " + url);
    DBUser next = getDBUser(user);
    logger.info("forgotPassword " + user + " next " + next);

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
        {
          String email = next.getEmail();
          if (!isValidAsEmail(email)) {
            logger.error("forgotPassword huh? email " + email + " not valid?");
          }
        }

        ClientUserDetail clientUserDetail = getClientUserDetail(next);

        maybeFixEmailForReset(user, optionalEmail, clientUserDetail);

        if (!isValidAsEmail(clientUserDetail.getEmail())) {
          logger.warn("forgotPassword users for " + user + " bad email " + clientUserDetail.getEmail());
          return false;
        } else {
          logger.info("forgotPassword users clientUserDetail " + clientUserDetail);
          clientUserDetail1 = delegate.forgotPassword(next, clientUserDetail, url);

          logger.info("forgotPassword forgotPassword users for " + user + " : " + clientUserDetail1);
        }
      } catch (Exception e) {
        logger.error("forgotPassword Got " + e, e);
      }

      return clientUserDetail1 != null;
    }
  }

  /**
   * If we don't have old email, use the optional email.
   *
   * @param user
   * @param optionalEmail
   * @param clientUserDetail
   */
  private void maybeFixEmailForReset(String user, String optionalEmail, ClientUserDetail clientUserDetail) {
    String email = clientUserDetail.getEmail();
    boolean validAsEmail = isValidAsEmail(optionalEmail);
    if (email.equalsIgnoreCase(UNSET) || !isValidAsEmail(email)) {
      if (validAsEmail) {
        logger.info("using " + optionalEmail + " instead of " + email + " for " + user);
        clientUserDetail.setEmail(optionalEmail);
      }
    } else if (validAsEmail) {
      logger.info("override - using " + optionalEmail + " instead of " + email + " for " + user);
      clientUserDetail.setEmail(optionalEmail);
    }
  }

  @Override
  public DBUser getDominoAdminUser() {
    return dominoAdminUser;
  }

//
//  public void setUserProjectDAO(IUserProjectDAO userProjectDAO) {
//    IUserProjectDAO userProjectDAO1 = userProjectDAO;
//  }
//
//  public void setProjectManagement(IProjectManagement projectManagement) {
//    IProjectManagement projectManagement1 = projectManagement;
//  }

  @Override
  public boolean updateProject(int oldID, int newprojid) {
    return false;
  }
}
