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

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.database.Database;
import mitll.langtest.shared.user.User;

import java.util.Map;

public abstract class SlickUserDAOImpl extends BaseUserDAO implements IUserDAO {
  SlickUserDAOImpl(Database database) {
    super(database);
  }
  /*private static final Logger logger = LogManager.getLogger(SlickUserDAOImpl.class);
  private final UserDAOWrapper dao;
  private IUserPermissionDAO permissionDAO;

  *//**
   * @param database
   * @param dbConnection
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs(PathHelper)
   *//*
  public SlickUserDAOImpl(Database database, DBConnection dbConnection) {
    super(database);
    dao = new UserDAOWrapper(dbConnection);
  }

  public void setPermissionDAO(IUserPermissionDAO permissionDAO) {
    this.permissionDAO = permissionDAO;
  }

  public void createTable() {
    dao.createTable();
  }

  @Override
  public String getName() {
    return "";
    //return dao.dao().name();
  }

  *//**
   * @param user
   * @return
   * @see mitll.langtest.server.database.copy.CopyToPostgres#addUser
   *//*
  public int add(SlickUser user) {

    return dao.add(user);
  }

  *//**
   * Returns SlickUser from database, not necessarily same as passed in?
   *
   * @param user
   * @return
   * @see mitll.langtest.server.database.copy.CopyToPostgres#addUser(SlickUserDAOImpl, Map, User)
   *//*
  public SlickUser addAndGet(SlickUser user, Collection<User.Permission> permissions) {
    SlickUser user1 = dao.addAndGet(user);
    int i = addPermissions(permissions, user1.id());
    // if (i > 0) logger.info("inserted " + i + " permissions for " + user1.id());
    return user1;
  }

  *//**
   * Add granted permissions...
   * @param permissions
   * @param foruserid
   * @return
   *//*
  private int addPermissions(Collection<User.Permission> permissions, int foruserid) {
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
  }

  *//**
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
   * @param passwordH
   * @param emailH
   * @param email
   * @param device
   * @param first
   * @param first
   * @param last
   * @return
   * @see UserManagement#addUser
   *//*
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
                     String passwordH,
                     String emailH, String email, String device,
                     String first, String last) {
    Timestamp now = new Timestamp(System.currentTimeMillis());
   // getConvertedPermissions(permissions, now);

    return dao.add(new SlickUser(-1, userID,
        gender.equalsIgnoreCase("male"),
        userAgent,
        trueIP,
        age,
        dialect,
        now,
        enabled,
        "",
        "",
//        builder.toString(),
        kind.toString(),
        passwordH,
        emailH,
        email,
        device,
        first,
        last,
        -1,
        now
        ));
  }

*//*  private void getConvertedPermissions(Collection<User.Permission> permissions, Timestamp now) {
    List<SlickUserPermission> requested = new ArrayList<>();
    for (User.Permission permission : permissions) {
      requested.add(getPendingPermission(now, permission));
    }
  }*//*

*//*  private SlickUserPermission getPendingPermission(Timestamp now, User.Permission permission) {
    return new SlickUserPermission(-1,
        beforeLoginUser,
        beforeLoginUser,
        permission.toString(),
        now,
        User.PermissionStatus.PENDING.toString(),
        now,
        beforeLoginUser);
  }*//*

  *//**
   * For really old users with missing info
   * @param id
   * @param kind
   * @param passwordH
   * @param emailH
   * @see BaseUserDAO#addUser
   *//*
  protected void updateUser(int id, User.Kind kind, String passwordH, String emailH) {
    dao.updateUser(id, kind.name(), passwordH, emailH);//,
//        kind == User.Kind.CONTENT_DEVELOPER ? CD_PERMISSIONS.toString() : EMPTY_PERM.toString());
  }

  @Override
  public String isValidEmail(String emailH) {
    List<String> usersWithEmail = dao.isValidEmail(emailH);
    return usersWithEmail.isEmpty() ? null : usersWithEmail.get(0);
  }

  @Override
  public Integer getIDForUserAndEmail(String user, String emailH) {
    List<Integer> idForUserAndEmail = dao.getIDForUserAndEmail(user, emailH);
    return idForUserAndEmail.isEmpty() ? null : idForUserAndEmail.get(0);
  }

  @Override
  public int getIdForUserID(String id) {
    return dao.idForUser(id);
  }

  @Override
  public User getUser(String id, String passwordHash) {
    Seq<SlickUser> userByIDAndPass = dao.getUserByIDAndPassOrFallback(id, passwordHash);
    return convertOrNull(userByIDAndPass);
  }

  @Override
  public User getStrictUserWithPass(String id, String passwordHash) {
    Seq<SlickUser> userByIDAndPass = dao.getUserByIDAndPass(id, passwordHash);
    return convertOrNull(userByIDAndPass);
  }

  *//**
   * @param id
   * @return
   * @see mitll.langtest.server.database.copy.CopyToPostgres#copyUsers
   *//*
  @Override
  public User getUserByID(String id) {
    return convertOrNull(dao.getByUserID(id));
  }

*//*  public Collection<User> getAllUsersByID(String id) {
    Seq<SlickUser> byUserID = dao.getByUserID(id);
    scala.collection.Iterator<SlickUser> iterator = byUserID.iterator();
    List<User> copy = new ArrayList<>();
    while (iterator.hasNext()) copy.add(toUser(iterator.next()));
    return copy;
  }*//*

  public User getByID(int id) {
    return convertOrNull(dao.byID(id));
  }

  private User convertOrNull(Seq<SlickUser> userByIDAndPass) {
    if (userByIDAndPass.isEmpty()) return null;
    else {
      SlickUser head = userByIDAndPass.head();
      Collection<User.Permission> grantedForUser = permissionDAO.getGrantedForUser(head.id());
      return toUser(head, grantedForUser);
    }
//    return userByIDAndPass.isEmpty() ? null : toUser(userByIDAndPass.head());
  }

  @Override
  public List<User> getUsers() {
    return toUsers(dao.getAll());
  }

  private List<User> toUsers(List<SlickUser> all) {
    List<User> copy = new ArrayList<>();

    Map<Integer, Collection<String>> granted = permissionDAO.granted();
    for (SlickUser s : all) {
//      logger.info("to user " + s);
      copy.add(toUser(s, toUserPerms(granted.get(s.id()))));
    }
    return copy;
  }

  private Collection<User.Permission> toUserPerms(Collection<String> strings) {
    List<User.Permission> perms = new ArrayList<>();
    if (strings != null) {
      for (String p : strings) {
//        logger.info("value of '" + p + "'");
        perms.add(User.Permission.valueOf(p));
      }
    }
    return perms;
  }

  *//**
   * @param user
   * @param useid
   * @return
   * @see mitll.langtest.server.database.copy.CopyToPostgres#addUser(SlickUserDAOImpl, Map, User)
   *//*
  public SlickUser toSlick(User user, boolean useid) {
    Timestamp now = new Timestamp(user.getTimestampMillis());
    SlickUser user1 = new SlickUser(useid ? user.getID() : -1,
        user.getUserID(),
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
        user.getFirst(),
        user.getLast(),
        user.getID(),
        now
    );

    logger.info("toSlick made " + user1);

    return user1;
  }

  private User toUser(SlickUser s, Collection<User.Permission> perms) {
    boolean admin = isAdmin(s.userid());

    User user = new User(
        s.id(),
        s.age(),
        s.ismale() ? 0 : 1,
        0,
        s.ipaddr(),
        s.passhash(),
        "",
        s.dialect(),
        s.userid(),
        s.enabled(),
        admin,
        perms,
        User.Kind.valueOf(s.kind()),
        s.email(),
        s.emailhash(),
        s.device(),
        s.resetpasswordkey(),
        //s.enabledreqkey(),
        s.modified().getTime()
    );

    user.setFirst(s.first());
    user.setLast(s.last());

    return user;
  }

  @Override
  public List<User> getUsersDevices() {
    return toUsers(dao.getUsersFromDevices());
  }

  @Override
  public Map<Integer, MiniUser> getMiniUsers() {
    Map<Integer, MiniUser> idToUser = new HashMap<>();
    for (SlickMiniUser s : dao.getAllSlim()) idToUser.put(s.id(), getMini(s));
    return idToUser;
  }

  public Map<User.Kind, Collection<MiniUser>> getMiniByKind() {
    Map<User.Kind, Collection<MiniUser>> kindToUsers = new HashMap<>();
    for (SlickMiniUser s : dao.getAllSlim()) {
      User.Kind key = User.Kind.valueOf(s.kind());
      Collection<MiniUser> miniUsers = kindToUsers.get(key);
      if (miniUsers == null) kindToUsers.put(key, miniUsers = new ArrayList<>());
      miniUsers.add(getMini(s));
    }

    for (Collection<MiniUser> perKind : kindToUsers.values()) {
      Collections.sort((ArrayList<MiniUser>) perKind, new Comparator<MiniUser>() {
        @Override
        public int compare(MiniUser o1, MiniUser o2) {
          return -1 * Long.valueOf(o1.getTimestampMillis()).compareTo(o2.getTimestampMillis());
        }
      });
    }

    return kindToUsers;
  }

  private MiniUser getMini(SlickMiniUser s) {
    MiniUser miniUser = new MiniUser(s.id(), 0, s.ismale(), s.userid(), isAdmin(s.userid()));

    miniUser.setTimestampMillis(s.modified().getTime());
    miniUser.setFirst(s.first());
    miniUser.setLast(s.last());

    return miniUser;
  }

  @Override
  public MiniUser getMiniUser(int userid) {
    List<SlickMiniUser> match = dao.getSlimFor(userid);
    return match.isEmpty() ? null : getMini(match.iterator().next());
  }

  @Override
  public User getUserWithResetKey(String resetKey) {
    return convertOrNull(dao.getByReset(resetKey));
  }

  @Override
  public User getUserWithEnabledKey(String resetKey) {
    return convertOrNull(dao.getByEnabledReq(resetKey));
  }

  @Override
  public User getUserWhere(int userid) {
//    logger.info("getUserWhere ask for user " + userid);
    Seq<SlickUser> userByIDAndPass = dao.byID(userid);
//    logger.info("getUserWhere got " + userByIDAndPass);
    User user = convertOrNull(userByIDAndPass);
    //  logger.info("getUserWhere got " + user);

    return user;
  }

  @Override
  public Map<Integer, User> getUserMap(boolean getMale) {
    Map<Integer, SlickUser> byMale = dao.getByMaleMap(getMale);
    Map<Integer, User> idToUser = new HashMap<>();

    Map<Integer, Collection<String>> granted = permissionDAO.granted();

    byMale.forEach((k, v) -> idToUser.put(k, toUser(v, toUserPerms(granted.get(k)))));
    return idToUser;
  }

  @Override
  public Collection<Integer> getUserIDs(boolean getMale) {
    return dao.getByMaleIDs(getMale);
  }

  @Override
  public Map<Integer, User> getUserMap() {
    Map<Integer, User> idToUser = new HashMap<>();
    Map<Integer, Collection<String>> granted = permissionDAO.granted();

    dao.getIdToUser().forEach((k, v) -> idToUser.put(k, toUser(v, toUserPerms(granted.get(k)))));
    return idToUser;
  }

  *//**
   * @param user
   * @param passwordH
   * @return
   * @see mitll.langtest.server.services.UserServiceImpl#changePFor(String, String)
   *//*
  @Override
  public boolean changePassword(int user, String passwordH) {
    return dao.setPassword(user, passwordH);
  }

  @Override
  public boolean updateKey(int userid, boolean resetKey, String key) {
    return dao.updateKey(userid, resetKey, key);
  }

  @Override
  public boolean clearKey(int user, boolean resetKey) {
    return dao.updateKey(user, resetKey, "");
  }

  @Override
  public boolean enableUser(int id) {
    return changeEnabled(id, true);
  }

  @Override
  public boolean changeEnabled(int userid, boolean enabled) {
    return dao.changeEnabled(userid, enabled);
  }

  public Map<User.Kind, Integer> getCounts() {
    Map<String, Integer> counts = dao.getCounts();
    Map<User.Kind, Integer> ret = new HashMap<>();
    for (Map.Entry<String, Integer> pair : counts.entrySet()) {
      ret.put(User.Kind.valueOf(pair.getKey()), pair.getValue());
    }
    return ret;
  }

  @Override
  public void update(User toUpdate) {
  //  logger.info("update " + toUpdate);
    SlickUser toUpdate1 = toSlick(toUpdate, true);
  //  logger.info("update " + toUpdate1);
    int update = dao.update(toUpdate1);
    if (update == 0) {
      logger.warn("didn't update table with " + toUpdate1);
    }
   // logger.info("user now " + getByID(toUpdate.getID()));
  }*/
}
