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

import mitll.hlt.domino.shared.common.SResult;
import mitll.hlt.domino.shared.model.user.ClientUserDetail;
import mitll.hlt.domino.shared.model.user.DBUser;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.audio.AudioDAO;
import mitll.langtest.shared.user.FirstLastUser;
import mitll.langtest.shared.user.Kind;
import mitll.langtest.shared.user.MiniUser;
import mitll.langtest.shared.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.*;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 */
public class UserDAO extends BaseUserDAO implements IUserDAO {
  private static final Logger logger = LogManager.getLogger(UserDAO.class);
  public static final String OTHER = "OTHER";
  public static final boolean DEBUG = false;

  /**
   * @param database
   */
  public UserDAO(Database database) {
    super(database);
    try {
      createTable(database);
      ensureDefaultUsers();
    } catch (Exception e) {
      logger.error("UserDAO : got " + e, e);
  //    database.logEvent("unk", "create user table " + e.toString(), 0, UNKNOWN, projID);
    }
  }

  /**
   * public for test access... for now
   *
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
   */
  public void ensureDefaultUsers() {
  }

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
  public int addUser(int age, MiniUser.Gender gender, int experience, String userAgent,
                     String trueIP, String nativeLang, String dialect, String userID, boolean enabled,
                     Collection<User.Permission> permissions,
                     Kind kind,
                     //String freeTextPassword,
                     //String passwordH,
                     String emailH, String email, String device, String first, String last, String url, String aff) {
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
      statement.setInt(i++, gender == MiniUser.Gender.Male ? 0 : 1);
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
      logger.error("addUser Got " + ee, ee);
   //   database.logEvent("unk", "adding user: " + ee.toString(), 0, device, projID);
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
  protected void updateUser(int id, Kind kind, String emailH, String email) {
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
      String kind1 = kind == Kind.CONTENT_DEVELOPER ? CD_PERMISSIONS.toString() : EMPTY_PERM.toString();
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
      database.logEvent("unk", "update user: " + ee.toString(), 0, UNKNOWN, -1);
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

  @Override
  public boolean isKnownUser(String userid) {
    return false;
  }

  /**
   * Not case sensitive.
   *
   * @param id
   * @return
   * @see BaseUserDAO#getOrAdd
   */
  @Override
  public int getIdForUserID(String id) {
    String sql = "SELECT id from users where UPPER(userID)='" + id.toUpperCase() + "'";
    return userExistsSQL(id, sql);
  }

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
  public DBUser getDBUser(String userID) {
    return null;
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
      database.logEvent(id, "getIdForUserID: " + e.toString(), 0, UNKNOWN, -1);
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
  public ReportUsers getReportUsers() {
    return null;
  }

  /**
   * @return
   * @see AudioDAO#getResultsForQuery(java.sql.Connection, java.sql.PreparedStatement)
   */
  @Override
  public Map<Integer, MiniUser> getMiniUsers() {
    Map<Integer, MiniUser> mini = new HashMap<>();
    for (User user : getUsers()) mini.put(user.getID(), new MiniUser(user));
    return mini;
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
  public Map<Integer, FirstLastUser> getFirstLastFor(Collection<Integer> userDBIds) {
    return null;
  }

  @Override
  public String getUserChosenID(int userid) {
    return null;
  }

  @Override
  public User getUserWithResetKey(String resetKey) {
    return getUserWhere(-1, "SELECT * from users where " + RESET_PASSWORD_KEY + "='" + resetKey + "'");
  }

  /**
   * @param userid
   * @return null if no user with that id else the user object
   * @seex mitll.langtest.server.LangTestDatabaseImpl#getUserBy
   * @deprecated
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
      database.logEvent("unk", "getUsers: " + ee.toString(), 0, UNKNOWN, -1);
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
      database.logEvent("unk", "getUsers: " + ee.toString(), 0, UNKNOWN, -1);
    }
    return new ArrayList<>();
  }

  /**
   * @param rs
   * @return
   * @throws SQLException
   * @see #getUsers(String)
   */
  private List<User> getUsers(ResultSet rs) throws SQLException {
    List<User> users = new ArrayList<>();

    while (rs.next()) {
      String userid = rs.getString("userid"); // userid

      Collection<User.Permission> permissions = getPermissions(rs, userid);
      boolean isAdmin = isAdmin(userid);
      String userKind = rs.getString(KIND);

      int id = rs.getInt(ID);

      String password = rs.getString(PASS);
      String userID = rs.getString(USER_ID);
//      String emailH = rs.getString(EMAIL);
      String device = rs.getString(DEVICE);

      if (userKind == null) {
        if (DEBUG) logger.debug("getUsers user kind for " + id + " " + userID + " is null?");
      }
      // if the user kind is unmarked, we'll make them a student, we can always change it later.

      Kind userKind1 = getKind(userKind);

      if (admins.contains(userID)) userKind1 = Kind.ADMIN;
      String resetKey = rs.getString(RESET_PASSWORD_KEY);
      int anInt = rs.getInt(GENDER);
      MiniUser.Gender realGender = anInt == 0 ? MiniUser.Gender.Male : MiniUser.Gender.Female;

      User newUser = new User(id, //id
          userID, rs.getInt(AGE), // age
          anInt, //gender
          realGender,
          rs.getInt(EXPERIENCE), // exp
          rs.getString(IPADDR), // ip
          password, // password
          // first
          // last
          rs.getString(NATIVE_LANG), // native
          rs.getString(DIALECT), // dialect

          rs.getBoolean(ENABLED),// || (userKind1 != User.Kind.CONTENT_DEVELOPER),
          isAdmin,
          permissions,
          userKind1,
          "",
          device,
          resetKey,
          //  "",
          rs.getTimestamp(TIMESTAMP).getTime(),
          OTHER,
          true);

      users.add(newUser);

      if (newUser.getUserID() == null) {
        newUser.setUserID("" + newUser.getID());
      }
    }
    return users;
  }

  @NotNull
  private Kind getKind(String userKind) {
    Kind userKind1;
    try {
      userKind1 = userKind == null ? Kind.UNSET : Kind.valueOf(userKind);
    } catch (IllegalArgumentException e) {
      userKind1 = Kind.UNSET;
    }
    return userKind1;
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
      logger.info("getPermissions : no perms column???");
    }
    return permissions;
  }

  @Override
  public boolean isMale(int userid) {
    return false;
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
      database.logEvent("unk", "update user: " + ee.toString(), 0, UNKNOWN, -1);
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

  @Override
  public SResult<ClientUserDetail> updateUser(DBUser dbUser) {

    return null;
  }

  @Override
  public void update(User toUpdate) {
  }

  @Override
  public void close() {
  }

  @Override
  public boolean isStudent(int userIDFromSessionOrDB) {
    return false;
  }

  @Override
  public DBUser getDominoAdminUser() {
    return null;
  }

  @Override
  public boolean forgotPassword(String user
      , String url
                                //    ,                              String emailForLegacy
  ) {
    return false;
  }
}