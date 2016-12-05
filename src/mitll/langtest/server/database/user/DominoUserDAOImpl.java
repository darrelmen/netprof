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

//import mitll.hlt.domino.server.user.INetProfUserDelegate;

import mitll.hlt.domino.server.user.IUserServiceDelegate;
import mitll.hlt.domino.server.user.UserServiceFacadeImpl;
import mitll.hlt.domino.server.util.*;
import mitll.hlt.domino.server.util.ServerProperties;
import mitll.hlt.domino.shared.common.FilterDetail;
import mitll.hlt.domino.shared.common.FindOptions;
import mitll.hlt.domino.shared.common.SResult;
import mitll.hlt.domino.shared.model.user.*;
import mitll.hlt.json.JSONSerializer;
import mitll.langtest.client.user.Md5Hash;
import mitll.langtest.server.*;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.analysis.Analysis;
import mitll.langtest.server.database.result.IResultDAO;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.user.MiniUser;
import mitll.langtest.shared.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Store user info in domino tables.
 */
public class DominoUserDAOImpl extends BaseUserDAO implements IUserDAO {
  private static final Logger logger = LogManager.getLogger(DominoUserDAOImpl.class);

  private IUserServiceDelegate delegate;
  // private INetProfUserDelegate netProfDelegate;
  //private final String adminHash;

  /**
   * get the admin user.
   *
   * @see #ensureDefaultUsers
   */
  private mitll.hlt.domino.shared.model.user.User adminUser;
  private mitll.hlt.domino.shared.model.user.User dominoImportUser;

  /**
   * @param database
   * @paramx dbConnection
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
   */
  public DominoUserDAOImpl(Database database/*, ServletContext servletContext*/) {
    super(database);
    //adminHash = Md5Hash.getHash("adm!n");
    //  log.info("Starting Mongo connection initialization");
    mitll.langtest.server.ServerProperties serverProps = database.getServerProps();
    Properties props = serverProps.getProps();
    Mongo pool = Mongo.createPool(new DBProperties(props));

    if (pool != null) {
      JSONSerializer serializer = Mongo.makeSerializer();
      Mailer m = new Mailer(new MailerProperties(props));
      mitll.hlt.domino.server.util.ServerProperties dominoProps =
          new ServerProperties(props, "1.0", "demo", "0", "now");
      dominoProps.getProperties().put(ServerProperties.APP_NAME_PROP, serverProps.getAppTitle());
      delegate = UserServiceFacadeImpl.makeServiceDelegate(dominoProps, m, pool, serializer, null/*ignite*/);
    } else {
      logger.error("couldn't connect to user service");
    }
  }

  @Override
  public void ensureDefaultUsers() {
    super.ensureDefaultUsers();
    adminUser = delegate.getUser(BEFORE_LOGIN_USER);
    //   logger.info("got admin user " +adminUser);
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
   * Returns ClientUserDetail from database, not necessarily same as passed in?
   * <p>
   * TODO : add roles and permissions
   *
   * @param user
   * @param encodedPass
   * @param permissions - not used right at the moment
   * @return
   * @see mitll.langtest.server.database.copy.UserCopy#addUser(DominoUserDAOImpl, Map, User)
   */
  public ClientUserDetail addAndGet(ClientUserDetail user, String encodedPass, Collection<User.Permission> permissions) {
    invalidateCache();

    SResult<ClientUserDetail> clientUserDetailSResult = addUserToMongo(user, encodedPass);

//    SlickUser user1 = dao.addAndGet(user);
    //  int i = addPermissions(permissions, user1.id());
    // if (i > 0) logger.info("inserted " + i + " permissions for " + user1.id());
    ClientUserDetail clientUserDetail = clientUserDetailSResult.get();
//    logger.info("\taddAndGet Got back " + clientUserDetailSResult);

    return clientUserDetail;
  }

  /**
   * @param user
   * @param freeTextPassword
   * @return
   * @see BaseUserDAO#addUser(int, String, int, String, String, String, String, String, boolean, Collection, User.Kind, String, String, String, String, String, String, String)
   * @see #addAndGet(ClientUserDetail, String, Collection)
   */
  private SResult<ClientUserDetail> addUserToMongo(ClientUserDetail user, String freeTextPassword) {
    SResult<ClientUserDetail> clientUserDetailSResult = delegate.addUser(adminUser, user, "");

    logger.info("addUserToMongo Got back " + clientUserDetailSResult);

    boolean b = delegate.changePassword(adminUser, user, "", freeTextPassword);
    if (!b) {
      logger.warn("addUserToMongo didn't set password for " + user.getUserId());
    }
    return clientUserDetailSResult;
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
   * @param freeTextPassword
   * @param passwordH
   * @param emailH
   * @param email
   * @param device
   * @param first
   * @param last             @return
   * @see UserManagement#addUser
   * @deprecated remember to change the password to the freetext password -
   */
  @Override
  public int addUser(int age,
                     String gender,
                     int experience,
                     String userAgent,
                     String trueIP,
                     String nativeLang,
                     String dialect, String userID, boolean enabled,
                     Collection<User.Permission> permissions,
                     User.Kind kind,
                     String freeTextPassword,
                     String passwordH,
                     String emailH,
                     String email,
                     String device,
                     String first,
                     String last) {
    // Timestamp now = new Timestamp(System.currentTimeMillis());
    // getConvertedPermissions(permissions, now);

    List<mitll.hlt.domino.shared.model.user.User.Role> ts = Collections.emptyList();
    ClientUserDetail updateUser = new ClientUserDetail(
        //useid ? user.getID() : -1,
        userID,
        first,
        last,
        email,
        ts,
        new Group()
    );
    AccountDetail acctDetail = new AccountDetail();
    updateUser.setAcctDetail(acctDetail);
    acctDetail.setCrTime(new Date());
//    SResult<ClientUserDetail> clientUserDetailSResult = getClientUserDetailSResult(updateUser, passwordH);

    SResult<ClientUserDetail> clientUserDetailSResult = addUserToMongo(updateUser, freeTextPassword);//"password_change_me_please");

    ClientUserDetail clientUserDetail = clientUserDetailSResult.get();

    invalidateCache();
    return clientUserDetail.getDocumentDBID();
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
   * TODO: SKIP ROLE FOR NOW.
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
   * @param freeTextPassword
   * @return
   * @see BaseUserDAO#updateUser
   * @see #changePassword
   */
  private DBUser savePasswordAndGetUser(int id, String freeTextPassword) {
    DBUser dbUser = delegate.lookupDBUser(id);
    if (dbUser != null) {
      dbUser.setPrimaryGroup(new Group()); // TODO just for now... so it doesn't crash
      boolean b = delegate.changePassword(adminUser, dbUser, "", freeTextPassword);
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
    logger.info("getDbUsers " + opts + " = " + users);
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
    List<DBUser> users = getDbUsersByUserID(id);
    return users.isEmpty() ? -1 : users.get(0).getDocumentDBID();
  }

  /**
   * @param id
   * @return
   * @see #getUser
   * @see #getIdForUserID
   */
  private List<DBUser> getDbUsersByUserID(String id) {
    Set<FilterDetail<UserColumn>> filterDetails = new HashSet<>();
    addUserID(id, filterDetails);
    return getDbUsers(filterDetails);
  }

  /**
   * Make sure to do string comparison.
   *
   * @param user
   * @param emailFilter
   */
  private void addUserID(String user, Set<FilterDetail<UserColumn>> emailFilter) {
    FilterDetail<UserColumn> filterDetail = new FilterDetail<>(UserColumn.UserId, user, FilterDetail.Operator.EQ);
    filterDetail.setStringField(true);
    //  logger.info("Adding filter detail " + filterDetail + " match " + filterDetail.getMatchValue());
    emailFilter.add(filterDetail);
  }

  /**
   * TODO : remove the password arg.
   *
   * @param id
   * @param passwordHash ignored for now
   * @return
   * @deprecated we don't check the password here
   */
  @Override
  public User getUser(String id, String passwordHash) {
    List<DBUser> users = getDbUsersByUserID(id);
    return users.isEmpty() ? null : toUser(users.iterator().next());
  }

//  @Override
//  public User getUserFreeTextPassword(String id, String freeTextPassword) {
//    logger.error("confirm this is the right thing\n\n\n ");
//    return getUser(id, freeTextPassword);
//  }

  public User loginUser(String userId,
                        String attemptedPassword,
                        //String remoteIP,
                        String userAgent,
                        //String sessionId,
                        String remoteAddr,
                        String sessionID) {
    DBUser loggedInUser =
        delegate.loginUser(
            userId,
            attemptedPassword,
            remoteAddr,
            userAgent,
            sessionID);
    return loggedInUser == null ? null : toUser(loggedInUser, Collections.emptyList());
  }

  /**
   * TODO : don't use password hash - use free text instead
   *
   * @param id
   * @param passwordHash
   * @return
   * @see mitll.langtest.server.database.copy.UserCopy#copyUsers(DatabaseImpl, int, IResultDAO)
   */
  @Override
  public User getStrictUserWithPass(String id, String passwordHash) {
    User user = getUser(id, "");

    if (user != null) {
      logger.info("getStrictUserWithPass '" + id + "' and password hash '" + passwordHash + "'");

      // TODO : allow admin masquerade login - e.g. gvidaver/steve - with gvidaver's password logs you in as steve

      boolean magicMatch = passwordHash.equals(adminHash);

      // TODO : put this back
      // TODO : put this back
      // TODO : put this back

      if (true) {//netProfDelegate.isPasswordMatch(user.getID(), passwordHash) || magicMatch) {
        boolean isadmin = database.getServerProps().getAdmins().contains(user.getUserID());
        user.setAdmin(isadmin);
        return user;
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

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
    List<DBUser> users = getDbUsersByUserID(id);
    return users.isEmpty() ? null : toUser(users.iterator().next());
//    return getUser(id, "");
  }

  public User getByID(int id) {
    DBUser dbUser = delegate.lookupDBUser(id);
    return dbUser == null ? null : toUser(dbUser);
  }

  /**
   * TODO : lookup permissions for user
   *
   * @return
   * @paramx xuserByIDAndPass
   */
  private User toUser(mitll.hlt.domino.shared.model.user.User head) {
    Collection<User.Permission> grantedForUser = Collections.emptyList();
    return toUser(head, grantedForUser);
  }

  @Override
  public List<User> getUsers() {
    return toUsers(getAll());
  }

  /**
   * TODO : add permissions
   *
   * @param all
   * @return
   */
  private List<User> toUsers(List<DBUser> all) {
    List<User> copy = new ArrayList<>();
//    Map<Integer, Collection<String>> granted = permissionDAO.granted();
    for (DBUser s : all) {
//      logger.info("to user " + s);
      copy.add(toUser(s, Collections.emptyList()
          //    toUserPerms(granted.get(s.id())))
      ));
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
   * @param useid
   * @return
   * @seex mitll.langtest.server.database.copy.CopyToPostgres#addUser(DominoUserDAOImpl, Map, User)
   */
  public ClientUserDetail toClientUserDetail(User user, boolean useid) {
    //Timestamp now = new Timestamp(user.getTimestampMillis());
    String first = user.getFirst();
    if (first == null) first = user.getUserID();
    String last = user.getLast();
    if (last == null) last = "Unknown";
    String email = user.getEmail();
    if (email == null || email.isEmpty()) {
      email = "admin@dliflc.edu";
    }
    ClientUserDetail clientUserDetail = new ClientUserDetail(
        //useid ? user.getID() : -1,
        user.getUserID(),
        first,
        last,
        email,
        Collections.emptyList(),
        new Group()/*

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

    clientUserDetail.setAcctDetail(new AccountDetail(dominoImportUser, new Date(user.getTimestampMillis())));

    logger.info("toClientUserDetail made " + clientUserDetail);

    return clientUserDetail;
  }

  /**
   * TODO: figure out how to add gender -
   *
   * @param dominoUser
   * @param perms
   * @return
   */
  private User toUser(mitll.hlt.domino.shared.model.user.User dominoUser, Collection<User.Permission> perms) {
    // logger.info("toUser " + dominoUser);
    boolean admin = isAdmin(dominoUser);
    long creationTime = 0;
    // logger.info("\ttoUser admin " + admin);
    if (dominoUser instanceof DBUser) {
      AccountDetail acctDetail = ((DBUser) dominoUser).getAcctDetail();
      if (acctDetail != null) {
        if (acctDetail.getCrTime() != null) {
          creationTime = acctDetail.getCrTime().getTime();
        } else {
          logger.info("no cr time on " + acctDetail);
        }
      } else {
        logger.info("no acct detail for " + dominoUser.getDocumentDBID());
      }
    } else {
      logger.info("domino user " + dominoUser.getDocumentDBID() + " is not a db user");
    }

    String email = dominoUser.getEmail();

    User user = new User(
        dominoUser.getDocumentDBID(),
        99,//dominoUser.age(),
        0,//dominoUser.ismale() ? 0 : 1,
        0,
        "",//dominoUser.ipaddr(),
        "",//"BOGUS_HASH_PASS",//dominoUser.passhash(),
        "",
        "",//dominoUser.dialect(),
        dominoUser.getUserId(),
        dominoUser.isActive(),
        admin,
        perms,
        getUserKind(dominoUser),
        email,
        email == null ? "" : Md5Hash.getHash(email),//dominoUser.emailhash(),
        "",//        dominoUser.device(),
        "",//dominoUser.resetpasswordkey(),
        //dominoUser.enabledreqkey(),
        creationTime//dominoUser.modified().getTime()
    );

    user.setFirst(dominoUser.getFirstName());
    user.setLast(dominoUser.getLastName());

//    logger.info("\ttoUser return " + user);

    return user;
  }

  private boolean isAdmin(mitll.hlt.domino.shared.model.user.User dominoUser) {
    return dominoUser.hasRole(mitll.hlt.domino.shared.model.user.User.Role.GrAM) ||
        dominoUser.hasRole(mitll.hlt.domino.shared.model.user.User.Role.PrAdmin);
  }

  /**
   * TODO : convert domino role to NetProF role.
   *
   * @param dominoUser
   * @return
   */
  private User.Kind getUserKind(mitll.hlt.domino.shared.model.user.User dominoUser) {
    return User.Kind.STUDENT;
  }

  /**
   * TOOD : get device info from domino user?
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
   *
   * @return
   * @see Analysis#getUserInfos
   * @see mitll.langtest.server.database.audio.SlickAudioDAO#getAudioAttributesByProject(int)
   */
  @Override
  public synchronized Map<Integer, MiniUser> getMiniUsers() {
    if (miniUserCache == null) {
      Map<Integer, MiniUser> idToUser = new HashMap<>();
      for (DBUser s : getAll()) idToUser.put(s.getDocumentDBID(), getMini(s));
      miniUserCache = idToUser;
      return idToUser;
    } else {
      return miniUserCache;
    }
  }

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
   * TODO : figure out how to get ROLE from DBUser
   *
   * @param s
   * @return
   */
  private User.Kind getRole(DBUser s) {
    return User.Kind.STUDENT;
  }

  @Override
  public MiniUser getMiniUser(int userid) {
    DBUser byID = delegate.lookupDBUser(userid);
    return byID == null ? null : getMini(byID);
  }

  /**
   * TODO : figure out how to get gender
   *
   * @param dominoUser
   * @return
   */
  private MiniUser getMini(DBUser dominoUser) {
    boolean admin = isAdmin(dominoUser);

    MiniUser miniUser = new MiniUser(
        dominoUser.getDocumentDBID(),
        0,  // age
        true,//dominoUser.ismale(),
        dominoUser.getUserId(),
        admin);

    //   logger.info("getMini for " + dominoUser);

    AccountDetail acctDetail = dominoUser.getAcctDetail();
    long time = acctDetail == null ?
        System.currentTimeMillis() :
        acctDetail.getCrTime() == null ? System.currentTimeMillis() : acctDetail.getCrTime().getTime();

    miniUser.setTimestampMillis(time);
    miniUser.setFirst(dominoUser.getFirstName());
    miniUser.setLast(dominoUser.getLastName());

    return miniUser;
  }

  /**
   * TODO: Reset password works differently now...?
   *
   * @param resetKey
   * @return
   * @see mitll.langtest.server.rest.RestUserManagement#changePFor(String, String)
   * @see mitll.langtest.server.rest.RestUserManagement#getUserIDForToken(String)
   * @see mitll.langtest.server.services.UserServiceImpl#changePFor(String, String)
   * @see mitll.langtest.server.services.UserServiceImpl#getUserIDForToken(String)
   */
  @Override
  @Deprecated
  public User getUserWithResetKey(String resetKey) {
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
   * TODO:add permissions?
   * TODO:add gender
   *
   * @param getMale
   * @return
   */
  @Override
  public Map<Integer, User> getUserMap(boolean getMale) {
    logger.warn("getUserMap: NOTE : no gender support yet.");
    //Map<Integer, SlickUser> byMale = dao.getByMaleMap(getMale);

    Map<Integer, User> idToUser = new HashMap<>();
    getAll().forEach(dbUser -> idToUser.put(dbUser.getDocumentDBID(), toUser(dbUser)));
//    Map<Integer, Collection<String>> granted = permissionDAO.granted();
//    byMale.forEach((k, v) -> idToUser.put(k, toUser(v, toUserPerms(granted.get(k)))));
    return idToUser;
  }

  /**
   * TODO:add gender
   *
   * @param getMale
   * @return
   */
  @Override
  public Collection<Integer> getUserIDs(boolean getMale) {
    logger.warn("getUserIDs: NOTE : no gender support yet.");

//    return dao.getByMaleIDs(getMale);
    List<Integer> ids = getAll()
        .stream()
        .map(UserDescriptor::getDocumentDBID)
        .collect(Collectors.toList());

    return ids;
  }

  /**
   * TODO: add permissions
   *
   * @return
   * @see mitll.langtest.server.database.result.ResultDAO#getUserToResults(AudioType, IUserDAO)
   */
  @Override
  public Map<Integer, User> getUserMap() {
    logger.warn("getUserMap: NOTE : no gender support yet.");

/*    Map<Integer, User> idToUser = new HashMap<>();
//    Map<Integer, Collection<String>> granted = permissionDAO.granted();
    dao.getIdToUser().forEach((k, v) -> idToUser.put(k, toUser(v, toUserPerms(granted.get(k)))));
    return idToUser;*/

    return getUserMap(true);
  }

  /**
   * @param user
   * @param freeTextPassword
   * @return
   * @see mitll.langtest.server.services.UserServiceImpl#changePFor(String, String)
   */
  @Override
  public boolean changePassword(int user, String freeTextPassword) {

    return savePasswordAndGetUser(user, freeTextPassword) != null;
    //return dao.setPassword(user, freeTextPassword);
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
   */
  public void forgetPassword(int userid) {
    DBUser dbUser = delegate.lookupDBUser(userid);

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
}
