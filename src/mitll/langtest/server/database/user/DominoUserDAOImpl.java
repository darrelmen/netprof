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

import com.mongodb.client.MongoCollection;
import mitll.hlt.domino.server.user.*;
import mitll.hlt.domino.server.util.*;
import mitll.hlt.domino.shared.common.FilterDetail;
import mitll.hlt.domino.shared.common.FindOptions;
import mitll.hlt.domino.shared.common.SResult;
import mitll.hlt.domino.shared.model.user.*;
import mitll.hlt.json.JSONSerializer;
import mitll.langtest.client.user.Md5Hash;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.analysis.Analysis;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.user.MiniUser;
import mitll.langtest.shared.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.include;
import static mitll.hlt.domino.server.user.MongoUserServiceDelegate.USERS_C;
import static mitll.langtest.shared.user.User.Kind.ADMIN;
import static mitll.langtest.shared.user.User.Kind.PROJECT_ADMIN;

/**
 * Store user info in domino tables.
 */
public class DominoUserDAOImpl extends BaseUserDAO implements IUserDAO {
  private static final Logger logger = LogManager.getLogger(DominoUserDAOImpl.class);
  private static final String EMAIL = "admin@dliflc.edu";
  private static final mitll.hlt.domino.shared.model.user.User.Gender DMALE = mitll.hlt.domino.shared.model.user.User.Gender.Male;
  private static final mitll.hlt.domino.shared.model.user.User.Gender DFEMALE = mitll.hlt.domino.shared.model.user.User.Gender.Female;
  private static final String PRIMARY = "primary";
  private static final String DEFAULT_AFFILIATION = "OTHER";

  private IUserServiceDelegate delegate;
  private MyMongoUserServiceDelegate myDelegate;

  private final Mongo pool;

  private final Map<String, User.Kind> roleToKind = new HashMap<>();
  /**
   * get the admin user.
   *
   * @see #ensureDefaultUsers
   */
  private mitll.hlt.domino.shared.model.user.User adminUser;
  private mitll.hlt.domino.shared.model.user.User dominoImportUser;
  private DBUser dominoAdminUser;

  /**
   * @param database
   * @paramx dbConnection
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
   */
  public DominoUserDAOImpl(Database database) {
    super(database);

    for (User.Kind kind : User.Kind.values()) {
      roleToKind.put(kind.getRole(), kind);
    }
    mitll.langtest.server.ServerProperties serverProps = database.getServerProps();
    Properties props = serverProps.getProps();
    pool = Mongo.createPool(new DBProperties(props));

    if (pool != null) {
      JSONSerializer serializer = Mongo.makeSerializer();
      Mailer m = new Mailer(new MailerProperties(props));
      mitll.hlt.domino.server.util.ServerProperties dominoProps =
          new ServerProperties(props, "1.0", "demo", "0", "now");

      //
      // dominoProps.updateProperty(ServerProperties.APP_NAME_PROP, serverProps.getAppTitle());
      String appName = dominoProps.getAppName();
      logger.info("DominoUserDAOImpl app name is " + appName);

      delegate = UserServiceFacadeImpl.makeServiceDelegate(dominoProps, m, pool, serializer, null/*ignite*/);
      myDelegate = makeMyServiceDelegate(dominoProps.getUserServiceProperties(), m, pool, serializer);

      dominoAdminUser = delegate.getAdminUser();
    } else {
      logger.error("couldn't connect to user service");
    }
  }

  /*
  public static final MongoUserServiceDelegate makeServiceDelegate(ServerProperties props,
                                                                   Mailer mailer,
                                                                   Mongo mongoCP,
                                                                   JSONSerializer serializer,
                                                                   Ignite ignite) {
    MongoUserServiceDelegate d = null;
    //UserServiceProperties props, boolean isAdminPWChangeEnabled,
    //Mailer mailer, MongoConnectionPool mongoPool
    switch (props.getUserServiceProperties().serviceType) {
      case LDAP:
        d = new LDAPUserServiceDelegate(props.getUserServiceProperties(), mailer, props.getAppName(), mongoCP);
        break;
      case Mongo:
        d = (ignite != null && props.isCacheEnabled()) ?
            new CachingMongoUserService(props.getUserServiceProperties(), mailer, props.getAppName(), mongoCP, ignite) :
            new MongoDelegate(props.getUserServiceProperties(), mailer, props.getAppName(), mongoCP);
        break;
    }
    */

  private MyMongoUserServiceDelegate makeMyServiceDelegate(UserServiceProperties props,
                                                           Mailer mailer, Mongo mongoCP, JSONSerializer serializer) {
    MyMongoUserServiceDelegate d = new MyMongoUserServiceDelegate(props, mailer, "dude", mongoCP);
    d.initializeDAOs(serializer);
    return d;
  }

  private static class MyMongoUserServiceDelegate extends MongoUserServiceDelegate {

    public MyMongoUserServiceDelegate(UserServiceProperties props, Mailer mailer, String appName, Mongo mongoPool) {
      super(props, mailer, appName, mongoPool);
    }

    public boolean isMatch(String userid, String encoded, String attempt) {
      return authenticate(userid, encoded, attempt);
    }
  }


  @Override
  public void ensureDefaultUsers() {
    super.ensureDefaultUsers();
    String userId = dominoAdminUser.getUserId();
    adminUser = delegate.getUser(userId);//BEFORE_LOGIN_USER);

//    adminUser.getRoles().add(mitll.hlt.domino.shared.model.user.User.Role.GrAM);
//    adminUser.getRoles().add(mitll.hlt.domino.shared.model.user.User.Role.UM);

    logger.info("ensureDefaultUsers got admin user " + adminUser + " has roles " + adminUser.getRoleAbbreviationsString());

    if (adminUser.getPrimaryGroup() == null) {
      logger.warn("ensureDefaultUsers no group for " + adminUser);

      Group group = getPrimaryGroup(PRIMARY);
      adminUser.setPrimaryGroup(group);
    }

    dominoImportUser = delegate.getUser(IMPORT_USER);
  }

  /*
  public void setPermissionDAO(IUserPermissionDAO permissionDAO) {
    this.permissionDAO = permissionDAO;
  }
*/

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
   * @param permissions - not used right at the moment
   * @return
   * @see mitll.langtest.server.database.copy.UserCopy#addUser
   */
  public ClientUserDetail addAndGet(ClientUserDetail user,
                                    String encodedPass,
                                    Collection<User.Permission> permissions) {
    invalidateCache();

//    SResult<ClientUserDetail> clientUserDetailSResult = addUserToMongo(user,
//        /*encodedPass, */
//        "",
//        false);
//
//    logger.info("addAndGet Got back " + clientUserDetailSResult);

//    boolean b = delegate.changePassword(adminUser, clientUserDetailSResult.get(), "", encodedPass);
//    boolean b = delegate.changePassword(user.getUserId(), "", encodedPass, "");
    // boolean b = delegate.changePassword(adminUser, clientUserDetailSResult.get(), "", encodedPass);

//    boolean b = myDelegate.setPassword(clientUserDetailSResult.get(), encodedPass);
//    ClientUserDetail clientUserDetail1 = clientUserDetailSResult.get();

    logger.info("addAndGet really adding " + user);
    SResult<ClientUserDetail> clientUserDetailSResult1 = delegate.migrateUser(user, encodedPass);
    boolean b = !clientUserDetailSResult1.isError();
    if (!b) {
      logger.error("\n\n\naddUserToMongo didn't set password for " + user.getUserId() + " : " +
          clientUserDetailSResult1.getResponseMessage());
      return null;
    } else {
      //return clientUserDetailSResult;
    }

//    SlickUser user1 = dao.addAndGet(user);
    //  int i = addPermissions(permissions, user1.id());
    // if (i > 0) logger.info("inserted " + i + " permissions for " + user1.id());
    ClientUserDetail clientUserDetail = clientUserDetailSResult1.get();
//    logger.info("\taddAndGet Got back " + clientUserDetailSResult);

    return clientUserDetail;
  }

  /**
   * @param user
   * @param url
   * @param sendEmail
   * @return
   * @paramx freeTextPassword
   * @see BaseUserDAO#addUser
   * @see #addAndGet(ClientUserDetail, String, Collection)
   */
  private SResult<ClientUserDetail> addUserToMongo(ClientUserDetail user,
                                                   //String freeTextPassword,
                                                   String url,
                                                   boolean sendEmail) {

//    logger.info("adding user " + user);

    SResult<ClientUserDetail> clientUserDetailSResult = delegate.addUser(
        sendEmail ? user : adminUser,// adminUser,
        user,
        url);

    logger.info("addUserToMongo Got back " + clientUserDetailSResult);

    return clientUserDetailSResult;
  /*  boolean b = delegate.changePassword(adminUser, clientUserDetailSResult.get(), "", freeTextPassword);
    if (!b) {
      logger.error("\n\n\naddUserToMongo didn't set password for " + user.getUserId() + "\n\n\n");
      return null;
    } else {
      return clientUserDetailSResult;
    }*/
  }

  /**
   * Add granted permissions...
   * @param permissions
   * @param foruserid
   * @return
   */
/*  private int addPermissions(Collection<User.Permission> permissions, int foruserid) {
    Timestamp now = new Timestamp(System.currentTimeMillis());
    int c = 0;
    for (User.Permission permission : permissions) {
      SlickUserPermission e = new SlickUserPermission(-1,
          foruserid,
          foruserid,
          permission.toString(),
          now,
          User.PermissionStatus.GRANTED.toString(),
          now,
          importUser);
      permissionDAO.insert(e);
      c++;
    }
    return c;
  }*/

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

                     String emailH,
                     String email,
                     String device,
                     String first,
                     String last,
                     String url) {
    // Timestamp now = new Timestamp(System.currentTimeMillis());
    // getConvertedPermissions(permissions, now);

    ClientUserDetail updateUser = new ClientUserDetail(
        userID,
        first,
        last,
        email,
        DEFAULT_AFFILIATION,
        gender.equalsIgnoreCase("male") ? DMALE : DFEMALE,
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
    //List<Group> groups = delegate.getGroupDelegate().searchGroups("");
    List<Group> groups = delegate.getGroupDAO().searchGroups("");

    Group primaryGroup = groups.isEmpty() ? null : groups.iterator().next();

    if (primaryGroup == null) { //defensive
      logger.warn("\n\n\ngetGroup making a new group...?\n\n\n");
      primaryGroup = getPrimaryGroup(PRIMARY);
    }
    return primaryGroup;
  }

  private Group getGroupOrMake(String name) {
    //List<Group> groups = delegate.getGroupDelegate().searchGroups("");
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

/*  private void getConvertedPermissions(Collection<User.Permission> permissions, Timestamp now) {
    List<SlickUserPermission> requested = new ArrayList<>();
    for (User.Permission permission : permissions) {
      requested.add(getPendingPermission(now, permission));
    }
  }*/

/*  private SlickUserPermission getPendingPermission(Timestamp now, User.Permission permission) {
    return new SlickUserPermission(-1,
        beforeLoginUser,
        beforeLoginUser,
        permission.toString(),
        now,
        User.PermissionStatus.PENDING.toString(),
        now,
        beforeLoginUser);
  }*/

  /**
   * For really old users with missing info
   * <p>
   * Just set the email...
   * <p>
   * <p>
   * TODOx: SKIP ROLE FOR NOW.
   *
   * @param id
   * @param kind
   * @param emailH
   * @param email
   * @see BaseUserDAO#addUser
   */
  protected void updateUser(int id, User.Kind kind, String emailH, String email) {
//    DBUser dbUser = savePasswordAndGetUser(id, passwordH);

    DBUser dbUser = delegate.lookupDBUser(id);

    if (dbUser != null) {
      dbUser.setEmail(email);
      delegate.updateUser(adminUser, getClientUserDetail(dbUser));
    }
//    dao.updateUser(id, kind.name(), passwordH, emailH);//,
//        kind == User.Kind.CONTENT_DEVELOPER ? CD_PERMISSIONS.toString() : EMPTY_PERM.toString());
  }

  @NotNull
  private ClientUserDetail getClientUserDetail(DBUser dbUser) {
    return new ClientUserDetail(dbUser, new AccountDetail());
  }

  /**
   * @param id
   * @param newHashedPassword
   * @param baseURL
   * @return
   * @see BaseUserDAO#updateUser
   * @see IUserDAO#changePassword
   */
  private DBUser savePasswordAndGetUser(int id, String currentPassword, String newHashedPassword, String baseURL) {
    DBUser dbUser = delegate.lookupDBUser(id);
    if (dbUser != null) {
      dbUser.setPrimaryGroup(new Group()); // TODO just for now... so it doesn't crash
      boolean b = delegate.changePassword(adminUser, dbUser, currentPassword, newHashedPassword, baseURL);
      if (!b) {
        logger.error("huh? didn't change password for " + id + "\n");
      }
    }
    return dbUser;
  }

  @Override
  public String isValidEmail(String emailH) {
    Set<FilterDetail<UserColumn>> filterDetails = getEmailFilter(emailH);
    List<DBUser> users = getDbUsers(filterDetails);

    return users.isEmpty() ? null : users.get(0).getUserId();
  }

  /**
   * @param filterDetails
   * @return
   * @see #getDbUsersByUserID
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
//    List<DBUser> users = getDbUsersByUserID(id);
//    int i = users.isEmpty() ? -1 : users.get(0).getDocumentDBID();
//    logger.info("getIdForUserID " + id + " = " + i);
//    return i;
  }

  /**
   * @param id
   * @return
   * @see #getUserByID
   * @see #getIdForUserID
   */
/*  private List<DBUser> getDbUsersByUserID(String id) {

    mitll.hlt.domino.shared.model.user.User user = delegate.getUser(id);

    Set<FilterDetail<UserColumn>> filterDetails = new HashSet<>();
    addUserID(id, filterDetails);
    return getDbUsers(filterDetails);
  }*/

  /**
   * Make sure to do string comparison.
   *
   * @param user
   * @param filterDetails
   */
  private void addUserID(String user, Set<FilterDetail<UserColumn>> filterDetails) {
    FilterDetail<UserColumn> filterDetail = new FilterDetail<>(UserColumn.UserId, user, FilterDetail.Operator.EQ);
    filterDetail.setStringField(true);
    //  logger.info("Adding filter detail " + filterDetail + " match " + filterDetail.getMatchValue());
    filterDetails.add(filterDetail);
  }

  /**
   * TODO : remove the password arg.
   *
   * @return
   * @paramx id
   * @paramx passwordHash ignored for now
   */
/*  @Override
  public User getUser(String id, String passwordHash) {
    List<DBUser> users = getDbUsersByUserID(id);
    return users.isEmpty() ? null : toUser(users.iterator().next());
  }*/

//  @Override
//  public User getUserFreeTextPassword(String id, String freeTextPassword) {
//    logger.error("confirm this is the right thing\n\n\n ");
//    return getUser(id, freeTextPassword);
//  }

  /**
   * @param userId
   * @param attemptedPassword
   * @param userAgent
   * @param remoteAddr
   * @param sessionID
   * @return
   * @see #getStrictUserWithPass(String, String)
   * @see mitll.langtest.server.services.UserServiceImpl#loginUser(String, String, String)
   */
  public User loginUser(String userId,
                        String attemptedPassword,
                        //String remoteIP,
                        String userAgent,
                        //String sessionId,
                        String remoteAddr,
                        String sessionID) {
    logger.info("loginUser " + userId + " pass " + attemptedPassword);
    DBUser loggedInUser =
        delegate.loginUser(
            userId,
            attemptedPassword,
            remoteAddr,
            userAgent,
            sessionID);
    return loggedInUser == null ? null : toUser(loggedInUser);
  }

  /**
   * TODO : don't use password hash - use free text instead
   * <p>
   * TODO : (somewhere else) allow admin masquerade login - e.g. gvidaver/steve - with gvidaver's password logs you in as steve
   *
   * @param id
   * @param encodedPassword
   * @return
   * @see mitll.langtest.server.database.copy.UserCopy#copyUsers
   */
  @Override
  public User getStrictUserWithPass(String id, String encodedPassword) {
    User user = getUserByID(id);
    // User user = loginUser(id, encodedPassword, "", "", "");
    return getUserIfMatch(user, id, encodedPassword);
  }

  private User getUserIfMatch(User user, String id, String encodedPassword) {
    if (user != null) {
      return getUserIfMatchPass(user, id, encodedPassword);
    } else {
      logger.info("getStrictUserWithPass '" + id + "' is an unknown user");
      return null;
    }
  }

  // @Override
  public User getUserIfMatchPass(User user, String id, String encodedPassword) {
    logger.info("getStrictUserWithPass '" + id + "' and password hash '" + encodedPassword + "'");
    String password = getUserCredentials(id);

    boolean match = myDelegate.isMatch(id, password, encodedPassword);
    //boolean magicMatch = encodedPassword.equals(adminHash);

    if (password != null && (match || password.equals(encodedPassword))) {//password.equals(encodedPassword)) {//netProfDelegate.isPasswordMatch(user.getID(), encodedPassword) || magicMatch) {
      logger.warn("\n\n\ngetStrictUserWithPass match in of " + password + " vs " + encodedPassword + "\n\n\n");

      boolean isadmin = database.getServerProps().getAdmins().contains(user.getUserID());
      user.setAdmin(isadmin);
      return user;
    } else {
      logger.warn("\n\tgetStrictUserWithPass no match in db " + password + " vs " + encodedPassword);
      return null;
    }
  }

  private static final String UID_F = "userId";
  private static final String PASS_F = "pass";

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

//  private String getEncodedPassword(int id) {
//    return users().find(Filters.eq("_id", id)).first().getString("pass");
//  }

//  @Override
//  public User getStrictUserWithFreeTextPass(String id, String freeTextPassword) {
//    logger.error("2 confirm this is the right thing\n\n\n ");
//    return getStrictUserWithPass(id, freeTextPassword);
//  }

  /**
   * @param id
   * @return
   * @seex mitll.langtest.server.database.copy.CopyToPostgres#copyUsers
   */
  @Override
  public User getUserByID(String id) {
    mitll.hlt.domino.shared.model.user.User user1 = delegate.getUser(id);
    if (user1 == null) {
      logger.warn("getUserByID no user by '" + id + "'");
    }
    return user1 == null ? null : toUser(user1);
//    List<DBUser> users = getDbUsersByUserID(id);
//    User user = users.isEmpty() ? null : toUser(users.iterator().next());
//    return user;
//    return getUser(id, "");
  }

  public User getByID(int id) {
    DBUser dbUser = delegate.lookupDBUser(id);
    return dbUser == null ? null : toUser(dbUser);
  }

  /**
   * lookup permissions for user
   *
   * @return
   * @paramx xuserByIDAndPass
   */
//  private User toUser(mitll.hlt.domino.shared.model.user.User head) {
//    Collection<User.Permission> grantedForUser = Collections.emptyList();
//
//    return toUser(head);
//  }
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

/*  private Collection<User.Permission> toUserPerms(Collection<String> strings) {
    List<User.Permission> perms = new ArrayList<>();
    if (strings != null) {
      for (String p : strings) {
//        logger.info("value of '" + p + "'");
        perms.add(User.Permission.valueOf(p));
      }
    }
    return perms;
  }*/

  /**
   * @param user
   * @param projectName
   * @return
   * @seex mitll.langtest.server.database.copy.CopyToPostgres#addUser(DominoUserDAOImpl, Map, User)
   */
  public ClientUserDetail toClientUserDetail(User user, String projectName) {
    //Timestamp now = new Timestamp(user.getTimestampMillis());
    String first = user.getFirst();
    if (first == null) first = user.getUserID();
    String last = user.getLast();
    if (last == null) last = "Unknown";
    String email = user.getEmail();
    if (email == null || email.isEmpty()) {
      email = EMAIL;
    }

    Group primaryGroup = getGroup();

    Group secondary = getGroupOrMake(projectName);

    Set<String> roleAbbreviations = new HashSet<>();
    roleAbbreviations.add(user.getUserKind().getRole());

    logger.info("toClientUserDetail " + user.getUserID() + " role is " + roleAbbreviations);
    logger.info("toClientUserDetail " + user.getUserID() + " group is " + secondary);

    ClientUserDetail clientUserDetail = new ClientUserDetail(
        user.getUserID(),
        first,
        last,
        email,
        DEFAULT_AFFILIATION,
        user.isMale() ? DMALE : DFEMALE,
        roleAbbreviations,
        primaryGroup

        /*

        user.isMale(),
        user.getIpaddr() == null ? "" : user.getIpaddr(),
        "",
        user.getAge(),
        user.getDialect(),
        now,
        user.isEnabled(),
        user.getResetKey() == null ? "" : user.getResetKey(),
        "",
        //  user.getPermissions().toString(),
        user.getUserKind().name(),
        user.getPasswordHash() == null ? "" : user.getPasswordHash(),
        user.getEmailHash() == null ? "" : user.getEmailHash(),
        "",
        user.getDevice() == null ? "" : user.getDevice(),

        user.getID(),
        now*/
    );
    AccountDetail acctDetail = new AccountDetail(
        dominoImportUser,
        new Date(user.getTimestampMillis()));

    clientUserDetail.addSecondaryGroup(secondary);
    clientUserDetail.setAcctDetail(acctDetail);

    logger.info("toClientUserDetail " + " groups for\n\t" + clientUserDetail + " : \n\t" + clientUserDetail.getSecondaryGroups());

//    logger.info("toClientUserDetail made " + clientUserDetail);

    return clientUserDetail;
  }

  /**
   * adds gender -
   *
   * @param dominoUser
   * @return
   */
  private User toUser(mitll.hlt.domino.shared.model.user.User dominoUser) {
    // logger.info("toUser " + dominoUser);
    boolean admin = isAdmin(dominoUser);
    long creationTime = 0;
    String device = "";
    // logger.info("\ttoUser admin " + admin);
    if (dominoUser instanceof DBUser) {
      AccountDetail acctDetail = ((DBUser) dominoUser).getAcctDetail();
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
    } else {
      logger.warn("toUser domino user " + dominoUser.getDocumentDBID() + " is not a db user?");
    }

    String email = dominoUser.getEmail();

    Set<User.Permission> permissionSet = new HashSet<>();
    User user = new User(
        dominoUser.getDocumentDBID(),
        99,//dominoUser.age(),
        dominoUser.getGender().equals(mitll.hlt.domino.shared.model.user.User.Gender.Male) ? 0 : 1,
        0,
        "",//dominoUser.ipaddr(),
        "",//"BOGUS_HASH_PASS",//dominoUser.passhash(),
        "",
        "",//dominoUser.dialect(),
        dominoUser.getUserId(),
        dominoUser.isActive(),
        admin,
        Collections.emptyList(),
        getUserKind(dominoUser, permissionSet),
        email,
        email == null ? "" : Md5Hash.getHash(email),//dominoUser.emailhash(),
        device,//        dominoUser.device(),
        "",//dominoUser.resetpasswordkey(),
        //dominoUser.enabledreqkey(),
        creationTime//dominoUser.modified().getTime()
    );

    user.setFirst(dominoUser.getFirstName());
    user.setLast(dominoUser.getLastName());
    user.setPermissions(permissionSet);
//    logger.info("\ttoUser return " + user);
    return user;
  }

  /**
   * checks roles
   *
   * @param dominoUser
   * @return
   * @see #getMini(DBUser)
   * @see #toUser(mitll.hlt.domino.shared.model.user.User)
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
//    Set<User.Permission> permissionSet = new HashSet<>();
    Set<User.Kind> seen = new HashSet<>();

    //if (!roleAbbreviations.size() > 1) {
//      String userId = dominoUser.getUserId();
    //    logger.warn("getUserKind user " + userId + " has multiple roles - choosing first one... " + roleAbbreviations.size());
    for (String role : roleAbbreviations) {
      User.Kind kind = getKindForRole(role);
      seen.add(kind);
      permissionSet.addAll(User.getInitialPermsForRole(kind));
      //    logger.info(userId + " has " + role);
    }
    // }

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
    User.Kind kind = roleToKind.get(firstRole);
    if (kind == null) {
      try {
        kind = User.Kind.valueOf(firstRole.toUpperCase());
        // shouldn't need this
        logger.debug("getUserKind lookup by NetProF user role " + firstRole);
      } catch (IllegalArgumentException e) {
        logger.error("getUserKind no user for " + firstRole);
        kind = User.Kind.STUDENT;
      }
    }
    return kind;
  }

  /**
   * TOOD : get device info from domino user?
   * For Reporting.
   *
   * @return
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

    if (miniUserCache == null || (now - lastCache) > 60 * 60 * 1000 || lastCount != delegate.getUserCount()) {
      Map<Integer, MiniUser> idToUser = new HashMap<>();
      for (DBUser s : getAll()) idToUser.put(s.getDocumentDBID(), getMini(s));
      miniUserCache = idToUser;
      if (!miniUserCache.isEmpty()) {
        lastCache = now;
        long then = System.currentTimeMillis();
        lastCount = delegate.getUserCount();
        long now2 = System.currentTimeMillis();

        if (now2 - then > 10) {
          logger.warn("took " + (now2 - then) + " millis to get count of db users.");
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

  private List<DBUser> getAll() {
    long then = System.currentTimeMillis();
    List<DBUser> users = delegate.getUsers(-1, null);
    long now = System.currentTimeMillis();
    if (now - then > 20) logger.warn("took " + (now - then) + " to get " + users.size() + " users");
    return users;
  }

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
    DBUser byID = delegate.lookupDBUser(userid);
    return byID == null ? null : getMini(byID);
  }

  /**
   * TODOx : figure out how to get gender
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
   * @seex mitll.langtest.server.services.UserServiceImpl#changePFor
   * @see mitll.langtest.server.rest.RestUserManagement#changePFor
   * @see mitll.langtest.server.rest.RestUserManagement#getUserIDForToken(String)
   * @see mitll.langtest.server.services.UserServiceImpl#getUserIDForToken(String)
   */
  @Override
  @Deprecated
  public User getUserWithResetKey(String resetKey) {

    logger.warn("no reset key! " + resetKey);
    return null;//convertOrNull(dao.getByReset(resetKey));
  }

  /**
   * TODO : enable content developer will happen in domino user management UI.
   *
   * @param resetKey
   * @return
   * @see mitll.langtest.server.mail.EmailHelper#enableCDUser(String, String, String, String)
   */
  @Override
  @Deprecated
  public User getUserWithEnabledKey(String resetKey) {
    //return convertOrNull(dao.getByEnabledReq(resetKey));
    return null;
  }

  @Override
  public User getUserWhere(int userid) {
    return getByID(userid);
  }

  /**
   * adds permissions
   * adds gender
   *
   * @param getMale
   * @return
   */
  @Override
  public Map<Integer, User> getUserMap(boolean getMale) {
    Map<Integer, User> idToUser = new HashMap<>();
    getAll()
        .stream()
        .filter(dbUser ->
            getMale ?
                dbUser.getGender() == mitll.hlt.domino.shared.model.user.User.Gender.Male :
                dbUser.getGender() == mitll.hlt.domino.shared.model.user.User.Gender.Female)
        .forEach(dbUser -> idToUser.put(dbUser.getDocumentDBID(), toUser(dbUser)));
    return idToUser;
  }

  /**
   * adds gender
   *
   * @param getMale
   * @return
   */
  @Override
  public Collection<Integer> getUserIDs(boolean getMale) {
    //logger.warn("getUserIDs: NOTE : no gender support yet.");

    List<Integer> ids = getAll()
        .stream()
        .filter(dbUser -> getMale ? dbUser.getGender() == mitll.hlt.domino.shared.model.user.User.Gender.Male :
            dbUser.getGender() == mitll.hlt.domino.shared.model.user.User.Gender.Female)
        .map(UserDescriptor::getDocumentDBID)
        .collect(Collectors.toList());

    return ids;
  }

  /**
   * has permissions
   *
   * @return
   * @see mitll.langtest.server.database.result.ResultDAO#getUserToResults(AudioType, IUserDAO)
   */
  @Override
  public Map<Integer, User> getUserMap() {
    Map<Integer, User> idToUser = getUserMap(true);
    idToUser.putAll(getUserMap(false));
    return idToUser;
  }

  /**
   * @param user
   * @param newHashPassword
   * @param baseURL
   * @return
   * @see mitll.langtest.server.services.UserServiceImpl#changePFor
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
   * @see mitll.langtest.server.services.UserServiceImpl#changePassword
   */
  public boolean changePasswordWithCurrent(int user, String currentHashPass, String newHashPass, String baseURL) {
    return savePasswordAndGetUser(user, currentHashPass, newHashPass, baseURL) != null;
  }

  /**
   * TODO : we can't store reset password keys yet
   *
   * @param userid
   * @param resetKey
   * @param key
   * @return
   * @see mitll.langtest.server.mail.EmailHelper#resetPassword(String, String, String)
   */
  @Override
  @Deprecated
  public boolean updateKey(int userid, boolean resetKey, String key) {

    return false;
//    return dao.updateKey(userid, resetKey, key);
  }

  /**
   * TODO : can't store keys for the moment...
   *
   * @param user
   * @param resetKey
   * @return
   * @see mitll.langtest.server.rest.RestUserManagement#changePFor
   */
  @Override
  public boolean clearKey(int user, boolean resetKey) {
//    return dao.updateKey(user, resetKey, "");
    return false;
  }

  /**
   * TODO : how do we do this in domino?
   *
   * @param id
   * @return
   */
  @Override
  public boolean enableUser(int id) {
    logger.warn("Calling enable user on " + id);
    return changeEnabled(id, true);
  }

  /**
   * TODO : Not sure what to put in for urlBase...
   *
   * @param userid
   * @see
   */
  public void forgetPassword(int userid) {
    DBUser dbUser = delegate.lookupDBUser(userid);

    logger.info("forgetPassword " + userid);

    if (dbUser == null) {
      logger.error("no user with id " + userid);
    } else {
      ClientUserDetail clientUserDetail = getClientUserDetail(dbUser);
      delegate.forgotPassword(adminUser, clientUserDetail, "");
    }
  }

  /**
   * TODO : this will be done in the Domino user UI.
   *
   * @param userid
   * @param enabled
   * @return
   */
  @Deprecated
  @Override
  public boolean changeEnabled(int userid, boolean enabled) {
//    return dao.changeEnabled(userid, enabled);
    return false;
  }

  /**
   * We don't support this UI anymore.
   *
   * @return
   */
  @Deprecated
  public Map<User.Kind, Integer> getCounts() {
    // Map<String, Integer> counts = dao.getCounts();
    Map<User.Kind, Integer> ret = new HashMap<>();
 /*   for (Map.Entry<String, Integer> pair : counts.entrySet()) {
      ret.put(User.Kind.valueOf(pair.getKey()), pair.getValue());
    }
 */
    return ret;
  }

  /**
   * TODO : user updates happen in domino UI...
   *
   * @param toUpdate
   */
  @Override
  @Deprecated
  public void update(User toUpdate) {
/*
    //  logger.info("update " + toUpdate);
    SlickUser toUpdate1 = toSlick(toUpdate, true);
    //  logger.info("update " + toUpdate1);
    int update = dao.update(toUpdate1);
    if (update == 0) {
      logger.warn("didn't update table with " + toUpdate1);
    }
*/
    // logger.info("user now " + getByID(toUpdate.getID()));
  }

  private boolean isValidAsEmail(String text) {
    return text.trim().toUpperCase().matches("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$");
  }

  @Override
  public boolean forgotPassword(String user, String url, String emailForLegacy) {
    DBUser next = delegate.getDBUser(user);

    if (next.getPrimaryGroup() == null) {
      next.setPrimaryGroup(getGroup());
    }
    ClientUserDetail clientUserDetail = getClientUserDetail(next);

    logger.info("forgotPassword users for " + user + " : " + clientUserDetail);

    ClientUserDetail clientUserDetail1 = null;
    try {
      if (!isValidAsEmail(clientUserDetail.getEmail())) {
        clientUserDetail.setEmail(emailForLegacy);
        logger.info("forgotPassword email now " + emailForLegacy);
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

/*  private static class MyMongoUserServiceDelegate extends MongoUserServiceDelegate {
    public MyMongoUserServiceDelegate(ServerProperties props, Mailer mailer, Mongo mongoCP) {
      super(props.getUserServiceProperties(), mailer, props.getAppName(), mongoCP);
    }

    public boolean setPassword(mitll.hlt.domino.shared.model.user.User user, String encodedPass) {
      return doSavePassword(user, encodedPass) != null;
    }
  }*/
}
