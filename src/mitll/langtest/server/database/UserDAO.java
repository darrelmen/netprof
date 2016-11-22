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

package mitll.langtest.server.database;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.custom.UserListManager;
import mitll.langtest.server.database.excel.UserDAOToExcel;
import mitll.langtest.shared.MiniUser;
import mitll.langtest.shared.User;
import net.sf.json.JSON;
import org.apache.log4j.Logger;

import java.io.OutputStream;
import java.sql.*;
import java.util.*;
import java.util.Date;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 */
public class UserDAO extends DAO {
  private static final Logger logger = Logger.getLogger(UserDAO.class);

  private static final String DEFECT_DETECTOR = "defectDetector";
  private static final String BEFORE_LOGIN_USER = "beforeLoginUser";

  public static final String USERS = "users";
  public static final String MALE = "male";
  public static final String FEMALE = "female";
  private static final String PERMISSIONS = "permissions";
  private static final String KIND = "kind";
  private static final String PASS = "passwordH";
  private static final String EMAIL = "emailH";
  private static final String DEVICE = "device";
  private static final String USER_ID = "userID";
  private static final List<User.Permission> CD_PERMISSIONS = Arrays.asList(User.Permission.QUALITY_CONTROL, User.Permission.RECORD_AUDIO);
  private static final List<User.Permission> EMPTY_PERM = new ArrayList<>();
  private static final String ENABLED = "enabled";
  private static final String RESET_PASSWORD_KEY = "resetPasswordKey";
  private static final String ENABLED_REQ_KEY = "enabledReqKey";
  private static final String NATIVE_LANG = "nativeLang";
  private static final String UNKNOWN = "unknown";
  private String language;
  private long defectDetector, beforeLoginUser;
  private boolean enableAllUsers;

  private static final String ID = "id";
  private static final String AGE = "age";
  private static final String GENDER = "gender";
  private static final String EXPERIENCE = "experience";
  private static final String IPADDR = "ipaddr";
  private static final String DIALECT = "dialect";
  private static final String TIMESTAMP = "timestamp";
  public static final int DEFAULT_USER_ID = -1;
  /**
   * After a default user has been marked male
   */
  public static final int DEFAULT_MALE_ID = -2;
  /**
   * After a default user has been marked female
   */
  public static final int DEFAULT_FEMALE_ID = -3;
  public static MiniUser DEFAULT_USER = new MiniUser(DEFAULT_USER_ID, 99, 0, "default", false);

  static MiniUser DEFAULT_MALE = new MiniUser(DEFAULT_MALE_ID, 99, 0, "Male", false);
  static MiniUser DEFAULT_FEMALE = new MiniUser(DEFAULT_FEMALE_ID, 99, 1, "Female", false);

  private Collection<String> admins;

  /**
   * @param database
   * @param serverProperties
   */
  public UserDAO(Database database, ServerProperties serverProperties) {
    super(database);
    try {
      admins = serverProperties.getAdmins();
      language = serverProperties.getLanguage();
      enableAllUsers = serverProperties.enableAllUsers();
      createTable(database);

      this.defectDetector = getPredefUser(DEFECT_DETECTOR);
      this.beforeLoginUser = getPredefUser(BEFORE_LOGIN_USER);
    } catch (Exception e) {
      logger.error("got " + e, e);
      database.logEvent("unk", "create user table " + e.toString(), 0, UNKNOWN);
    }
  }

  private long getPredefUser(String defectDetector) throws SQLException {
    // = DEFECT_DETECTOR;
    long i = userExists(defectDetector);
    // this.defectDetector = i;
    if (i == -1) {
      List<User.Permission> permissions = Collections.emptyList();
      try {
        i = addUser(89, MALE, 0, "", UNKNOWN, UNKNOWN, defectDetector, false, permissions, User.Kind.STUDENT,
            "", "", "", System.currentTimeMillis(), true);
      } catch (SQLException e) {
        logger.error("Got " + e, e);
        throw e;
      }
    }
    return i;
  }

  /**
   * @param manager
   * @see mitll.langtest.server.database.DatabaseImpl#makeDAO
   */
  public void checkForFavorites(UserListManager manager) {
    for (User u : getUsers()) {
      if (manager.getListsForUser(u.getId(), true, false).isEmpty()) {
        manager.createFavorites(u.getId());
      }
    }
  }

  /**
   * @return
   * @see mitll.langtest.server.database.custom.UserListManager#addDefect(String, String, String)
   * @see mitll.langtest.server.database.custom.AnnotationDAO#AnnotationDAO(Database, UserDAO)
   */
  public long getDefectDetector() {
    return defectDetector;
  }

  /**
   * Check if the user exists already, and return null if so.
   * If it exists but is a legacy user, update its fields.
   *
   * @param userID
   * @param passwordH
   * @param emailH
   * @param kind
   * @param ipAddr
   * @param isMale
   * @param age
   * @param dialect
   * @param device
   * @return null if existing, valid user (email and password)
   * @see mitll.langtest.server.database.DatabaseImpl#addUser
   */
  public User addUser(String userID, String passwordH, String emailH, User.Kind kind, String ipAddr,
                      boolean isMale, int age, String dialect, String device) {
    User userByID = getUserByID(userID);
    if (userByID != null && kind != User.Kind.ANONYMOUS) {
      // user exists!
      if (userByID.getEmailHash() != null && userByID.getPasswordHash() != null &&
          !userByID.getEmailHash().isEmpty() && !userByID.getPasswordHash().isEmpty()) {
        logger.debug(language + " : addUser : user " + userID + " is an existing user.");
        return null; // existing user!
      } else {
        updateUser(userByID.getId(), kind, passwordH, emailH);
        User userWhere = getUserWhere(userByID.getId());
        logger.debug(language + " : addUser : returning updated user " + userWhere);
        return userWhere;
      }
    } else {
      Collection<User.Permission> perms = (kind == User.Kind.CONTENT_DEVELOPER) ? CD_PERMISSIONS : EMPTY_PERM;
      boolean enabled = (kind != User.Kind.CONTENT_DEVELOPER) || isAdmin(userID) || enableAllUsers;

      long l = 0;
      try {
        l = addUser(age, isMale ? MALE : FEMALE, 0, ipAddr, "", dialect, userID, enabled, perms, kind,
            passwordH, emailH, device, System.currentTimeMillis(), false);
      } catch (SQLException e) {
        logger.error("Got " + e, e);
      }
      User userWhere = getUserWhere(l);
      logger.debug(language + " : addUser : added new user " + userWhere);

      return userWhere;
    }
  }

  /**
   * Somehow on subsequent runs, the ids skip by 30 or so?
   * <p>
   * Uses return generated keys to get the user id
   *
   * @param age
   * @param gender
   * @param experience
   * @param ipAddr
   * @param nativeLang
   * @param dialect
   * @param userID
   * @param enabled
   * @param permissions
   * @param kind
   * @param passwordH
   * @param emailH
   * @param device
   * @param modified
   * @param doThrow
   * @return newly inserted user id, or 0 if something goes horribly wrong
   * @see DatabaseImpl#addUser
   */
  public long addUser(int age, String gender, int experience, String ipAddr,
                      String nativeLang, String dialect, String userID, boolean enabled,
                      Collection<User.Permission> permissions,
                      User.Kind kind,
                      String passwordH,
                      String emailH,
                      String device, long modified, boolean doThrow) throws SQLException {
    if (passwordH == null) {
      logger.warn("no password hash for " + userID);
      passwordH = "";
//      new Exception().printStackTrace();
    }
    try {
      // there are much better ways of doing this...
      long max = 0;
      for (User u : getUsers()) if (u.getId() > max) max = u.getId();
      logger.info("addUser : max is " + max +
          " new user '" + userID + "'" +
          " age " + age +
          " gender '" + gender +
          "'" +
          " experience " + experience +
          " ipAddr " + ipAddr +
          " dialect " + dialect +
          " pass '" + passwordH + "' perm " + permissions + " kind " + kind + "\ntime " + new Date(modified));

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
      long newID = max + 1;
      statement.setLong(i++, newID);
      statement.setInt(i++, age);
      statement.setInt(i++, gender.equalsIgnoreCase(MALE) ? 0 : 1);
      statement.setInt(i++, experience);
      statement.setString(i++, ipAddr);
      statement.setString(i++, nativeLang);
      statement.setString(i++, dialect);
      statement.setString(i++, userID);
      statement.setBoolean(i++, enabled);

      StringBuilder builder = new StringBuilder();
      for (User.Permission permission : permissions) builder.append(permission).append(",");
      statement.setString(i++, builder.toString());

      statement.setString(i++, kind.toString());
      statement.setString(i++, passwordH);
      statement.setString(i++, emailH);
      statement.setString(i++, device);
      Timestamp x = new Timestamp(modified);
      logger.info("addUser insert " + userID + " : " + x);
      statement.setTimestamp(i++, x);

      statement.executeUpdate();

      finish(connection, statement);

      return newID;
    } catch (Exception ee) {
      logger.error("Got " + ee, ee);
      database.logEvent("unk", "adding user: " + ee.toString(), 0, device);
      if (doThrow) throw ee;
    }
    return 0;
  }

  /**
   * Will set the permissions according to the user kind.
   *
   * @param id
   * @param kind
   * @param passwordH
   * @param emailH
   * @see #addUser(String, String, String, mitll.langtest.shared.User.Kind, String, boolean, int, String, String)
   */
  private void updateUser(long id, User.Kind kind, String passwordH, String emailH) {
    try {
      if (passwordH == null) {
        logger.error("Got null password Hash?", new Exception("empty password hash"));
      }
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
      statement.setString(i++, passwordH);
      statement.setString(i++, emailH);
      String kind1 = kind == User.Kind.CONTENT_DEVELOPER ? CD_PERMISSIONS.toString() : EMPTY_PERM.toString();
      statement.setString(i++, kind1);
      statement.setLong(i++, id);

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

  /**
   * @param id
   * @return
   * @see mitll.langtest.server.mail.EmailHelper#enableCDUser(String, String, String)
   */
  public boolean enableUser(long id) {
    try {
      Connection connection = getConnection();
      PreparedStatement statement = connection.prepareStatement(
          "UPDATE " + USERS + " SET " +
              ENABLED + "=TRUE" +
              " WHERE " +
              ID + "=?");
      int i = 1;
      statement.setLong(i++, id);

      int i1 = statement.executeUpdate();

      statement.close();
      database.closeConnection(connection);

      return i1 != 0;
    } catch (Exception ee) {
      logger.error("Got " + ee, ee);
      database.logEvent("unk", "enableUser: " + ee.toString(), 0, UNKNOWN);
    }
    return false;
  }

  public User isValidEmail(String emailH) {
    String sql = "SELECT " +
        ID +
        " from " + USERS +
        " where " + EMAIL +
        "='" +
        emailH +
        "'";

    int i = userExistsSQL("N/A", sql);
    return i == -1 ? null : getUserWhere(i);
  }

  public User isValidUserAndEmail(String user, String emailH) {
    String sql = "SELECT " +
        ID +
        " from " + USERS +
        " where " +
        EMAIL + "='" + emailH + "' AND " +
        getUserIDMatchClause(user);

//    logger.debug("using sql:\n"+sql);

    int i = userExistsSQL(user, sql);
    return i == -1 ? null : getUserWhere(i);
  }

  /**
   * Not case sensitive.
   *
   * @param id
   * @return
   * @see DatabaseImpl#userExists(String)
   */
  public int userExists(String id) {
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
   * @see mitll.langtest.server.LangTestDatabaseImpl#userExists(String, String)
   * @see mitll.langtest.server.ScoreServlet#doGet
   */
  public User getUser(String id, String passwordHash) {
    User userWhere = getUserWithPass(id, passwordHash);
    if (userWhere == null) {
      logger.debug("getUser : no user with id '" + id + "' and pass " + passwordHash);
      userWhere = getUserByID(id);
      logger.debug(language + " : getUser user with id '" + id + "' pass " + passwordHash +
          " and empty or different pass is " + userWhere);
    }
    return userWhere;
  }

  /**
   * @param id
   * @param passwordHash
   * @return
   * @see #getUser(String, String)
   */
  public User getUserWithPass(String id, String passwordHash) {
    //String idNoSuffix = id.split("@")[0];
    logger.debug(language + " : getUserWithPass getting user with id '" + id + "' and pass '" + passwordHash + "'");
    String sql = "SELECT * from " +
        USERS +
        " where " +
        getUserIDMatchClause(id) +
        "and UPPER(" + PASS + ")='" + passwordHash.toUpperCase() +
        "'";

    return getUserWhere(-1, sql);
  }

  private String getUserIDMatchClause(String id) {
    return "(UPPER(" +
        USER_ID +
        ")='" + id.toUpperCase() + "' OR " +
        "UPPER(" +
        USER_ID +
        ")='" + id.split("@")[0].toUpperCase() + "'" +

        //" OR " +
        //EMAIL + "='" + id.toUpperCase() +
        //"'" +

        ") ";
  }

  /**
   * Case insensitive
   *
   * @param id
   * @return
   */
  public User getUserByID(String id) {
    return getUserWhere(-1, "SELECT * from users where UPPER(" + USER_ID + ")='" + id.toUpperCase() + "'");
  }

  private int userExistsSQL(String exid, String sql) {
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
      database.logEvent(exid, "userExists: " + e.toString(), 0, UNKNOWN);
    }
    return val;
  }

  public void createTable(Database database) throws Exception {
    Connection connection = database.getConnection(this.getClass().toString());

    try {
      String sql = "CREATE TABLE IF NOT EXISTS users (" +
          ID + " " + getIdentity() +
          ", " +
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

      dropDefaultColumn(connection, TIMESTAMP, "");
      // drop old default current timestamp
/*      statement = connection.prepareStatement("ALTER TABLE " + USERS + " ALTER " + TIMESTAMP + " TIMESTAMP NOT NULL");
      statement.execute();
      statement.close();*/
    } finally {
      database.closeConnection(connection);
    }
  }

/*  private void addVarcharColumn(Connection connection, String column) throws SQLException {
    addColumn(connection, column, "VARCHAR");
  }*/

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
  public List<User> getUsers() {
    return getUsers("SELECT * from users order by " + ID + " ASC");
  }

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
  public Map<Long, MiniUser> getMiniUsers() {
    List<User> users = getUsers();
    Map<Long, MiniUser> mini = new HashMap<>();
    for (User user : users) mini.put(user.getId(), new MiniUser(user));
    return mini;
  }

  /**
   * @param userid
   * @return
   * @see AudioDAO#getAudioAttribute
   */
  public MiniUser getMiniUser(long userid) {
    User userWhere = getUserWhere(userid);
    return userWhere == null ? null : new MiniUser(userWhere);
  }

  public User getUserWhereResetKey(String resetKey) {
    String sql = "SELECT * from users where " +
        RESET_PASSWORD_KEY +
        "='" + resetKey + "';";
    return getUserWhere(-1, sql);
  }

  /**
   * @param resetKey
   * @return
   * @see mitll.langtest.server.mail.EmailHelper#enableCDUser(String, String, String)
   */
  public User getUserWhereEnabledReq(String resetKey) {
    String sql = "SELECT * from users where " +
        ENABLED_REQ_KEY +
        "='" + resetKey + "';";
    return getUserWhere(-1, sql);
  }

  /**
   * @param userid
   * @return null if no user with that id else the user object
   * @see mitll.langtest.server.LangTestDatabaseImpl#getUserBy(long)
   */
  public User getUserWhere(long userid) {
    String sql = "SELECT * from users where " + ID + "=" + userid + ";";
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

  private User getUserWhere(long userid, String sql) {
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
      // logger.info("sql:\n" +sql);
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
      String userid;

      String perms = rs.getString(PERMISSIONS);
      Collection<User.Permission> permissions = new ArrayList<>();
      userid = rs.getString("userid"); // userid

      if (perms != null) {
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
      }


      boolean isAdmin = isAdmin(userid);
      String userKind = rs.getString(KIND);

      long id = rs.getLong(ID);

      String password = rs.getString(PASS);
      String userID = rs.getString(USER_ID);
      String email = rs.getString(EMAIL);
      String device = rs.getString(DEVICE);
      User.Kind userKind1 = userKind == null ? User.Kind.UNSET : User.Kind.valueOf(userKind);
      String resetKey = rs.getString(RESET_PASSWORD_KEY);
      Timestamp timestamp = rs.getTimestamp(TIMESTAMP);
      long time = timestamp == null ? System.currentTimeMillis() : timestamp.getTime();

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

          rs.getBoolean(ENABLED) || (userKind1 != User.Kind.CONTENT_DEVELOPER),
          isAdmin,
          permissions,
          userKind1,
          email,
          device,
          resetKey,
          "",
          time);

      if (timestamp == null) {
        long timestampMillis = newUser.getTimestampMillis();
        logger.error("timestamp null for " + newUser.getUserID() + " time " + new Date(timestampMillis));
      } else if (false) {
        long timestampMillis = newUser.getTimestampMillis();
        long now = System.currentTimeMillis();

        if (now - timestampMillis < 60 * 60 * 1000 && !newUser.getUserID().equals(UserDAO.BEFORE_LOGIN_USER)) {
          logger.warn("timestamp for " + newUser.getUserID() + " time " + new Date(newUser.getTimestampMillis()));
        }
      }
      users.add(newUser);

      if (newUser.getUserID() == null) {
        newUser.setUserID("" + newUser.getId());
      }
    }
    return users;
  }

  private boolean isAdmin(String userid) {
    return userid != null && (admins.contains(userid.toLowerCase()));
  }

  public Map<Long, User> getUserMap(boolean getMale) {
    return getUserMap(getMale, getUsers());
  }

  /**
   * @see AudioDAO#getUserIDsMatchingGender
   * @param getMale
   * @return
   */
  Set<Long> getUserIDsMatchingGender(boolean getMale) {
    return getUserIDs("SELECT " + ID + " FROM " + USERS + " WHERE " + GENDER + " = " + (getMale ? 0 : 1));
  }

  /**
   * @see mitll.langtest.server.database.analysis.Analysis#getUserInfo
   * @return
   */
  public Map<Long, User> getUserMap() {
    return getMap(getUsers());
  }

  private Map<Long, User> getUserMap(boolean getMale, List<User> users) {
    Map<Long, User> idToUser = new HashMap<>();
    for (User u : users) {
      if (u.isMale() && getMale || (!u.isMale() && !getMale)) {
        idToUser.put(u.getId(), u);
      }
    }
    return idToUser;
  }

  /**
   * @param users
   * @return
   */
  private Map<Long, User> getMap(List<User> users) {
    Map<Long, User> idToUser = new HashMap<>();
    for (User u : users) {
      idToUser.put(u.getId(), u);
    }
    return idToUser;
  }


  public boolean changePassword(Long remove, String passwordH) {
    try {
      Connection connection = getConnection();
      PreparedStatement statement;

      statement = connection.prepareStatement(
          "UPDATE " + USERS + " SET " +
              PASS + "=? " +
              " WHERE " +
              ID + "=?");
      int i = 1;
      statement.setString(i++, passwordH);
      statement.setLong(i++, remove);

      int i1 = statement.executeUpdate();

      statement.close();
      database.closeConnection(connection);

      User userByID = getUserWhere(remove);
      logger.debug("after password set to " + passwordH + " user now " + userByID);
      return i1 != 0;
    } catch (Exception ee) {
      logger.error("Got " + ee, ee);
      database.logEvent("unk", "update user: " + ee.toString(), 0, UNKNOWN);
    }
    return false;
  }

  /**
   * @param userid
   * @param resetKey
   * @param key
   * @return
   * @see #clearKey(Long, boolean)
   */
  public boolean updateKey(Long userid, boolean resetKey, String key) {
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
      // logger.debug("updateKey : for " + language + " update " + key + "/" + s + " for " + userid);
      return i1 != 0;
    } catch (Exception ee) {
      logger.error("Got " + ee, ee);
      database.logEvent("unk", "clearKey user: " + userid + " " + ee.toString(), 0, UNKNOWN);
    }
    return false;
  }

  /**
   * @param remove
   * @param resetKey
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#changePFor(String, String)
   */
  public boolean clearKey(Long remove, boolean resetKey) {
    return updateKey(remove, resetKey, "");
  }

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

  public void toXLSX(OutputStream out, List<User> users) {
    new UserDAOToExcel().toXLSX(out, users, language);
  }

  public JSON toJSON(List<User> users) {
    return new UserDAOToExcel().toJSON(users);
  }

  public long getBeforeLoginUser() {
    return beforeLoginUser;
  }
}