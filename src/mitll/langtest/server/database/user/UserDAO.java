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

import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.audio.AudioDAO;
import mitll.langtest.shared.user.MiniUser;
import mitll.langtest.shared.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.*;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 */
public class UserDAO extends BaseUserDAO implements IUserDAO {
  private static final Logger logger = LogManager.getLogger(UserDAO.class);

  /**
   * @param database
   * @paramx serverProperties
   */
  public UserDAO(Database database) {
    super(database);
    try {
      createTable(database);
      ensureDefaultUsers();
    } catch (Exception e) {
      logger.error("got " + e, e);
      database.logEvent("unk", "create user table " + e.toString(), 0, UNKNOWN);
    }
  }

  /**
   * @param manager
   * @see mitll.langtest.server.database.DatabaseImpl#makeDAO
   */
/*  public void checkForFavorites(UserListManager manager) {
    for (User u : getUsers()) {
      if (manager.getListsForUser(u.getID(), true, false).isEmpty()) {
        manager.createFavorites(u.getID());
      }
    }
  }*/

  /**
   * Somehow on subsequent runs, the ids skip by 30 or so?
   * <p>
   * Uses return generated keys to get the user id
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
   * @return newly inserted user id, or 0 if something goes horribly wrong
   * @paramx freeTextPassword
   * @paramx passwordH
   * @see UserManagement#addUser
   */
  @Override
  public int addUser(int age, String gender, int experience, String userAgent,
                     String trueIP, String nativeLang, String dialect, String userID, boolean enabled,
                     Collection<User.Permission> permissions,
                     User.Kind kind,
                     //String freeTextPassword,
                     //String passwordH,
                     String emailH, String email, String device, String first, String last, String url) {
    //if (passwordH == null) new Exception().printStackTrace();
    try {
      // there are much better ways of doing this...
      int max = 0;
      for (User u : getUsers()) if (u.getID() > max) max = u.getID();
//      logger.info("addUser : max is " + max + " new user '" + userID + "' age " + age + " gender " + gender + " pass " + passwordH);

      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement = connection.prepareStatement(
          "INSERT INTO " +
              USERS +
              "(" +
              ID + "," +
              "age," +
              "gender," +
              "experience," +
              "ipaddr," +
              "nativeLang," +
              "dialect," +
              "userID," +
              ENABLED + "," +
              PERMISSIONS + "," +
              KIND + "," +
              PASS + "," +
              EMAIL + "," +
              DEVICE + "," +
              TIMESTAMP +

              ") " +
              "VALUES(" +
              "?,?,?,?,?," +
              "?,?,?,?,?," +
              "?,?,?,?,?)");
      int i = 1;
      int newID = max + 1;
      statement.setLong(i++, newID);
      statement.setInt(i++, age);
      statement.setInt(i++, gender.equalsIgnoreCase(MALE) ? 0 : 1);
      statement.setInt(i++, experience);
      statement.setString(i++, userAgent);
      statement.setString(i++, nativeLang);
      statement.setString(i++, dialect);
      statement.setString(i++, userID);
      statement.setBoolean(i++, enabled);
      StringBuilder builder = new StringBuilder();
      for (User.Permission permission : permissions) builder.append(permission).append(",");
      statement.setString(i++, builder.toString());

      statement.setString(i++, kind.toString());
      statement.setString(i++, "");
      statement.setString(i++, emailH);
      statement.setString(i++, device);
      statement.setTimestamp(i++, new Timestamp(System.currentTimeMillis()));

      statement.executeUpdate();

      finish(connection, statement);

      return newID;
    } catch (Exception ee) {
      logger.error("Got " + ee, ee);
      database.logEvent("unk", "adding user: " + ee.toString(), 0, device);
    }
    return 0;
  }

  /**
   * Will set the EMPTY_PERMISSIONS according to the user kind.
   *
   * @param id
   * @param kind
   * @param emailH
   * @param email
   * @see BaseUserDAO#addUser
   */
  protected void updateUser(int id, User.Kind kind, String emailH, String email) {
    try {
/*      if (passwordH == null) {
        logger.error("Got null password Hash?", new Exception("empty password hash"));
      }*/
      logger.debug(language + " : update user #" + id + " kind " + kind);

      Connection connection = getConnection();
      PreparedStatement statement = connection.prepareStatement(
          "UPDATE " + USERS + " SET " +
              KIND + "=?," +
              PASS + "=?," +
              EMAIL + "=?," +
              PERMISSIONS + "=?" +
              " WHERE " +
              ID + "=?");
      int i = 1;
      statement.setString(i++, kind.toString());
      statement.setString(i++, "");//passwordH);
      statement.setString(i++, emailH);
      String kind1 = kind == User.Kind.CONTENT_DEVELOPER ? CD_PERMISSIONS.toString() : EMPTY_PERM.toString();
      statement.setString(i++, kind1);
      statement.setInt(i++, id);

      int i1 = statement.executeUpdate();

      statement.close();
      database.closeConnection(connection);

      if (i1 == 0) {
        logger.warn("huh? didn't update user for " + id + " kind " + kind);
      }
    } catch (Exception ee) {
      logger.error("Got " + ee, ee);
      database.logEvent("unk", "update user: " + ee.toString(), 0, UNKNOWN);
    }
  }


  @Override
  public String isValidEmail(String emailH) {
    String sql = "SELECT " +
        ID +
        " from " + USERS +
        " where " + EMAIL +
        "='" +
        emailH +
        "'";

    int i = userExistsSQL("N/A", sql);
    return i == -1 ? null : getUserWhere(i).getUserID();
  }

  @Override
  public Integer getIDForUserAndEmail(String user, String emailH) {
    String sql = "SELECT " +
        ID +
        " from " + USERS +
        " where " +
        EMAIL + "='" + emailH + "' AND UPPER(" +
        USER_ID + ")='" + user.toUpperCase() + "'";

    int i = userExistsSQL("N/A", sql);
    return i == -1 ? null : getUserWhere(i).getID();
  }

  @Override
  public User loginUser(String userId, String attemptedPassword, String userAgent, String remoteAddr, String sessionID) {
    return null;
  }

  /**
   * Not case sensitive.
   *
   * @param id
   * @return
   * @see DatabaseImpl#userExists
   */
  @Override
  public int getIdForUserID(String id) {
    String sql = "SELECT id from users where UPPER(userID)='" + id.toUpperCase() + "'";
    return userExistsSQL(id, sql);
  }

  /**
   * Case insensitive match.
   * TODO : Super misleading -- password not really required???
   *
   * @param id
   * @param passwordHash
   * @return a
   * @see mitll.langtest.server.LangTestDatabaseImpl#userExists
   * @see mitll.langtest.server.ScoreServlet#doGet
   */
/*  @Override
  public User getUser(String id, String passwordHash) {
    User userWhere = getStrictUserWithPass(id, passwordHash);
    if (userWhere == null) {
      logger.debug("getUser : no user with id '" + id + "' and pass " + passwordHash);
      userWhere = getUserByID(id);
      logger.debug(language + " : getUser user with id '" + id + "' pass " + passwordHash +
          " and empty or different pass is " + userWhere);
    }
    return userWhere;
  }*/

  /**
   * Shouldn't call this...
   *
   * @return
   * @paramx id
   * @paramx freeTextPassword
   * @deprecated
   */
  /*@Override
  public User getUserFreeTextPassword(String id, String freeTextPassword) {
    return getUser(id, freeTextPassword);
  }
*/
  @Override
  public User getStrictUserWithPass(String id, String passwordHash) {
    logger.debug(language + " : getUser getting user with id '" + id + "' and pass '" + passwordHash + "'");
    String sql = "SELECT * from " +
        USERS +
        " where " +
        "(UPPER(" +
        USER_ID +
        ")='" + id.toUpperCase() + "' OR " +
        EMAIL + "='" + id.toUpperCase() +

        "') and UPPER(" + PASS + ")='" + passwordHash.toUpperCase() +
        "'";

    return getUserWhere(-1, sql);
  }

  /**
   * @param id
   * @param freeTextPassword
   * @return
   * @deprecated
   */
/*  @Override
  public User getStrictUserWithFreeTextPass(String id, String freeTextPassword) {
    return getStrictUserWithPass(id, freeTextPassword);
  }*/

  /**
   * Case insensitive
   *
   * @param id
   * @return
   */
  @Override
  public User getUserByID(String id) {
    return getUserWhere(-1, "SELECT * from users where UPPER(" + USER_ID + ")='" + id.toUpperCase() + "'");
  }

  @Override
  public User getByID(int id) {
    return null;
  }

  private int userExistsSQL(String id, String sql) {
    int val = -1;
    try {
      Connection connection = getConnection();
      PreparedStatement statement = connection.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        val = rs.getInt(1);
      }

      //    logger.debug("user exists " + id + " = " + val);
      finish(connection, statement, rs, sql);

    } catch (Exception e) {
      logger.error("Got " + e, e);
      database.logEvent(id, "getIdForUserID: " + e.toString(), 0, UNKNOWN);
    }
    return val;
  }

  public void createTable(Database database) throws Exception {
    Connection connection = database.getConnection(this.getClass().toString());

    try {
      String sql = "CREATE TABLE IF NOT EXISTS users (" +
          ID + " " + getIdentity() + ", " +
          "age INT, " +
          "gender INT, " +
          "experience INT, " +
          "ipaddr " + getVarchar() + ", " +
          "password " + getVarchar() + ", " +
          "nativeLang " + getVarchar() + ", " +
          "dialect " + getVarchar() + ", " +
          USER_ID + " " + getVarchar() + ", " +
          TIMESTAMP + " TIMESTAMP, " +//" AS CURRENT_TIMESTAMP, " +
          "enabled BOOLEAN, " +
          RESET_PASSWORD_KEY + " " + getVarchar() + ", " +
          ENABLED_REQ_KEY + " " + getVarchar() + ", " +
          PERMISSIONS + " " + getVarchar() + ", " +
          KIND + " " + getVarchar() + ", " +
          PASS + " " + getVarchar() + ", " +
          EMAIL + " " + getVarchar() + ", " +
          DEVICE + " " + getVarchar() + ", " +
          //getPrimaryKey() +
          "CONSTRAINT pkusers PRIMARY KEY (id))";

      //    logger.info("sql\n"+sql);
      PreparedStatement statement = connection.prepareStatement(sql);
      statement.execute();
      statement.close();
      database.closeConnection(connection);

      //logger.debug("found " + numColumns + " in users table");

      Set<String> expected = new HashSet<>();
      expected.addAll(Arrays.asList(ID, AGE, GENDER, EXPERIENCE,
          IPADDR, "nativelang", DIALECT, USER_ID.toLowerCase(), TIMESTAMP, ENABLED));
      expected.removeAll(getColumns(USERS));
      if (!expected.isEmpty()) logger.info("adding columns for " + expected);
      for (String missing : expected) {
        if (missing.equalsIgnoreCase(TIMESTAMP)) {
          addColumn(connection, TIMESTAMP, "TIMESTAMP");
        }
        if (missing.equalsIgnoreCase(ENABLED)) {
          addColumn(connection, ENABLED, "BOOLEAN");
        }
      }
      Collection<String> columns = getColumns(USERS);

      for (String col : new HashSet<>(Arrays.asList(NATIVE_LANG, DIALECT, USER_ID, PERMISSIONS, KIND, PASS, EMAIL,
          DEVICE, ENABLED_REQ_KEY, RESET_PASSWORD_KEY))) {
        if (!columns.contains(col.toLowerCase())) {
          addVarchar(connection, USERS, col);
        }
      }
      // drop old default current timestamp
/*      statement = connection.prepareStatement("ALTER TABLE " + USERS + " ALTER " + TIMESTAMP + " TIMESTAMP NOT NULL");
      statement.execute();
      statement.close();*/
    } finally {
      database.closeConnection(connection);
    }
  }

  private void addColumn(Connection connection, String column, String type) throws SQLException {
    PreparedStatement statement = connection.prepareStatement("ALTER TABLE users ADD " + column + " " + type);
    statement.execute();
    statement.close();
  }

  private void dropDefaultColumn(Connection connection, String column, String type) throws SQLException {
    PreparedStatement statement = connection.prepareStatement("ALTER TABLE users ALTER COLUMN " + column +
            " DROP DEFAULT"
        //    " SET DEFAULT NULL"
    );
    //  logger.info("drop default on " +this);

    statement.execute();
    statement.close();

    try {
      String sql = "ALTER TABLE users ALTER COLUMN " + column +
          " TIMESTAMP DEFAULT NOT NULL";

      statement = connection.prepareStatement(sql
      );
      // logger.info("drop default on " + this);

      statement.execute();
      statement.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  /**
   * Pulls the list of users out of the database.
   *
   * @return
   */
  @Override
  public List<User> getUsers() {
    return getUsers("SELECT * from users order by " + ID + " ASC");
  }

  @Override
  public List<User> getUsersDevices() {
    return getUsers("SELECT * from users" +
        " where device like 'i%'" +
        " order by " + ID + " ASC"
    );
  }

  /**
   * @return
   * @see AudioDAO#getResultsForQuery(java.sql.Connection, java.sql.PreparedStatement)
   */
  @Override
  public Map<Integer, MiniUser> getMiniUsers() {
    List<User> users = getUsers();
    Map<Integer, MiniUser> mini = new HashMap<>();
    for (User user : users) mini.put(user.getID(), new MiniUser(user));
    return mini;
  }

  @Override
  public Map<User.Kind, Collection<MiniUser>> getMiniByKind() {
    return null;
  }

  /**
   * @param userid
   * @return null if can't find by id
   * @see AudioDAO#getAudioAttribute
   */
  @Override
  public MiniUser getMiniUser(int userid) {
    User userWhere = getUserWhere(userid);
    return userWhere == null ? null : new MiniUser(userWhere);
  }

  @Override
  public User getUserWithResetKey(String resetKey) {
    return getUserWhere(-1, "SELECT * from users where " + RESET_PASSWORD_KEY + "='" + resetKey + "'");
  }

  /**
   * @param resetKey
   * @return
   * @see mitll.langtest.server.mail.EmailHelper#enableCDUser(String, String, String, String)
   */
  @Override
  public User getUserWithEnabledKey(String resetKey) {
    return getUserWhere(-1, "SELECT * from users where " + ENABLED_REQ_KEY + "='" + resetKey + "'");
  }

  /**
   * @param userid
   * @return null if no user with that id else the user object
   * @see mitll.langtest.server.LangTestDatabaseImpl#getUserBy
   */
  @Override
  public User getUserWhere(int userid) {
    String sql = "SELECT * from users where " + ID + "=" + userid;
    //long then = System.currentTimeMillis();
    Collection<User> users = getUsers(sql);
    // long now = System.currentTimeMillis();
    //logger.debug("getUserWhere took " +(now-then) + " millis.");

    if (users.isEmpty()) {
      if (userid > 0) {
        logger.warn(language + " : no user with id " + userid);
      }
      return null;
    } else if (users.size() > 1) {
      logger.warn(language + " : getUserWhere huh? " + users.size() + " with id " + userid + " expecting only one.");
    }

    return users.iterator().next();
  }

  private User getUserWhere(int userid, String sql) {
    List<User> users = getUsers(sql);
    if (users.isEmpty()) {
      if (userid > 0) {
        logger.warn(language + " : for " + sql + " no user with id '" + userid + "'");
      }
      return null;
    }

    User next = users.iterator().next();
    logger.debug("For " + userid + " found " + next);
    return next;
  }

  private Set<Long> getUserIDs(String sql) {
    try {
      Connection connection = getConnection();
      PreparedStatement statement = connection.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      // logger.info("sql:\n" +sql);
      Set<Long> users = new TreeSet<>();
      while (rs.next()) {
        users.add(rs.getLong(1));
      }

      finish(connection, statement, rs, sql);

      return users;
    } catch (Exception ee) {
      logger.error("Got " + ee, ee);
      database.logEvent("unk", "getUsers: " + ee.toString(), 0, UNKNOWN);
    }
    return new HashSet<>();
  }

  private List<User> getUsers(String sql) {
    try {
      Connection connection = getConnection();
      PreparedStatement statement = connection.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      List<User> users = getUsers(rs);
      finish(connection, statement, rs, sql);

      return users;
    } catch (Exception ee) {
      logger.error("Got " + ee, ee);
      database.logEvent("unk", "getUsers: " + ee.toString(), 0, UNKNOWN);
    }
    return new ArrayList<>();
  }

  private List<User> getUsers(ResultSet rs) throws SQLException {
    List<User> users = new ArrayList<>();

    while (rs.next()) {
      String userid = rs.getString("userid"); // userid

      Collection<User.Permission> permissions = getPermissions(rs, userid);

//      if (!permissions.isEmpty()) {
//        logger.info("For " + userid + " : " + permissions);
//      }
      boolean isAdmin = isAdmin(userid);
      String userKind = rs.getString(KIND);

      int id = rs.getInt(ID);

      String password = rs.getString(PASS);
      String userID = rs.getString(USER_ID);
      String emailH = rs.getString(EMAIL);
      String device = rs.getString(DEVICE);

      if (userKind == null) {
        logger.warn("user kind for " + id + " " + userID + " is null?");
      }

      // if the user kind is unmarked, we'll make them a student, we can always change it later.

      User.Kind userKind1 = userKind == null ? User.Kind.STUDENT : User.Kind.valueOf(userKind);
      if (admins.contains(userID)) userKind1 = User.Kind.ADMIN;
      String resetKey = rs.getString(RESET_PASSWORD_KEY);
      User newUser = new User(id, //id
          rs.getInt(AGE), // age
          rs.getInt(GENDER), //gender
          rs.getInt(EXPERIENCE), // exp
          rs.getString(IPADDR), // ip
          password, // password
          // first
          // last
          rs.getString(NATIVE_LANG), // native
          rs.getString(DIALECT), // dialect
          userID,

          rs.getBoolean(ENABLED),// || (userKind1 != User.Kind.CONTENT_DEVELOPER),
          isAdmin,
          permissions,
          userKind1,
          "",
          emailH,
          device,
          resetKey,
          //  "",
          rs.getTimestamp(TIMESTAMP).getTime());

      users.add(newUser);

      if (newUser.getUserID() == null) {
        newUser.setUserID("" + newUser.getID());
      }
    }
    return users;
  }

  private Collection<User.Permission> getPermissions(ResultSet rs, String userid) throws SQLException {
    String perms = rs.getString(PERMISSIONS);
    Collection<User.Permission> permissions = new ArrayList<>();

    if (perms != null) {
//      logger.info("For " + userid + " " + perms);

      perms = perms.replaceAll("\\[", "").replaceAll("\\]", "");
      for (String perm : perms.split(",")) {
        perm = perm.trim();
        try {
          if (!perm.isEmpty()) {
            permissions.add(User.Permission.valueOf(perm));
          }
        } catch (IllegalArgumentException e) {
          logger.warn(language + " : huh, for user " + userid +
              " perm '" + perm +
              "' is not a permission?");
        }
      }
    } else {
      logger.info("no perms column???");
    }
    return permissions;
  }

  @Override
  public Map<Integer, User> getUserMap(boolean getMale) {
    return getUserMap(getMale, getUsers());
  }

  @Override
  public Set<Integer> getUserIDs(boolean getMale) {
    return getUserMap(getMale).keySet();
  }

  /**
   * @see AudioDAO#getUserIDsMatchingGender
   * @param getMale
   * @return
   */
  Set<Long> getUserIDsMatchingGender(boolean getMale) {
    return getUserIDs("SELECT " + ID + " FROM " + USERS + " WHERE " + GENDER + " = " + (getMale ? 0 : 1));
  }


  @Override
  public Map<Integer, User> getUserMap() {
    return getMap(getUsers());
  }

  private Map<Integer, User> getUserMap(boolean getMale, List<User> users) {
    Map<Integer, User> idToUser = new HashMap<>();
    for (User u : users) {
      if (u.isMale() && getMale || (!u.isMale() && !getMale)) {
        idToUser.put(u.getID(), u);
      }
    }
    return idToUser;
  }

  /**
   * @param users
   * @return
   */
  private Map<Integer, User> getMap(List<User> users) {
    Map<Integer, User> idToUser = new HashMap<>();
    for (User u : users) {
      idToUser.put(u.getID(), u);
    }
    return idToUser;
  }


  @Override
  public boolean changePassword(int user, String newHashPassword, String baseURL) {
    try {
      Connection connection = getConnection();
      PreparedStatement statement;

      statement = connection.prepareStatement(
          "UPDATE " + USERS + " SET " +
              PASS + "=? " +
              " WHERE " +
              ID + "=?");
      int i = 1;
      statement.setString(i++, newHashPassword);
      statement.setLong(i++, user);

      int i1 = statement.executeUpdate();

      statement.close();
      database.closeConnection(connection);

      User userByID = getUserWhere(user);
      logger.debug("after password set to " + newHashPassword + " user now " + userByID);
      return i1 != 0;
    } catch (Exception ee) {
      logger.error("Got " + ee, ee);
      database.logEvent("unk", "update user: " + ee.toString(), 0, UNKNOWN);
    }
    return false;
  }

  @Override
  public boolean changePasswordWithCurrent(int user, String currentHashPassword, String newHashPassword, String baseURL) {
    return true;
  }

  @Override
  public boolean changePasswordForToken(String userId, String userKey, String newPassword, String url) {
    return false;
  }

  /**
   * @param userid
   * @param resetKey
   * @param key
   * @return
   * @see IUserDAO#clearKey(int, boolean)
   */
  @Override
  public boolean updateKey(int userid, boolean resetKey, String key) {
    try {
      Connection connection = getConnection();
      String s = resetKey ? RESET_PASSWORD_KEY : ENABLED_REQ_KEY;
      PreparedStatement statement = connection.prepareStatement(
          "UPDATE " + USERS + " SET " +
              s + "=?" +
              " WHERE " +
              ID + "=?");
      int i = 1;
      statement.setString(i++, key);
      statement.setLong(i++, userid);
      int i1 = statement.executeUpdate();

      statement.close();
      database.closeConnection(connection);
      logger.debug("for " + language + " update " + key + "/" + s + " for " + userid);
      return i1 != 0;
    } catch (Exception ee) {
      logger.error("Got " + ee, ee);
      database.logEvent("unk", "clearKey user: " + userid + " " + ee.toString(), 0, UNKNOWN);
    }
    return false;
  }

  /**
   * @param user
   * @param resetKey
   * @return
   * @see mitll.langtest.server.services.UserServiceImpl#changePFor
   */
  @Override
  public boolean clearKey(int user, boolean resetKey) {
    return updateKey(user, resetKey, "");
  }

  @Override
  public Map<User.Kind, Integer> getCounts() {
    return null;
  }

  @Override
  public void update(User toUpdate) {

  }

  @Override
  public boolean forgotPassword(String user, String url, String emailForLegacy) {
    return false;
  }

  @Override
  public boolean changeEnabled(int userid, boolean enabled) {
    try {
      Connection connection = getConnection();

      PreparedStatement statement = connection.prepareStatement(
          "UPDATE " + USERS + " SET " +
              ENABLED + "=?" +
              " WHERE " +
              ID + "=?");
      int i = 1;
      statement.setBoolean(i++, enabled);
      statement.setLong(i++, userid);
      int i1 = statement.executeUpdate();

      statement.close();
      database.closeConnection(connection);
      logger.debug("for " + language + " update " + ENABLED + "/" + enabled + " for " + userid + "  " + i1);
      return i1 != 0;
    } catch (Exception ee) {
      logger.error("Got " + ee, ee);
      database.logEvent("unk", "changeEnabled user: " + userid + " " + ee.toString(), 0, UNKNOWN);
    }
    return false;
  }

  /**
   * @param id
   * @return
   * @see mitll.langtest.server.mail.EmailHelper#enableCDUser(String, String, String, String)
   */
  @Override
  public boolean enableUser(int id) {
    return changeEnabled(id, true);
  }
}