package mitll.langtest.server.database;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.custom.UserListManager;
import mitll.langtest.shared.MiniUser;
import mitll.langtest.shared.User;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class UserDAO extends DAO {
  private static final String DEFECT_DETECTOR = "defectDetector";
  private static final Logger logger = Logger.getLogger(UserDAO.class);
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
  private static final List<User.Permission> EMPTY_PERM = new ArrayList<User.Permission>();
  private static final String ENABLED = "enabled";
  private static final String RESET_PASSWORD_KEY = "resetPasswordKey";
  private static final String ENABLED_REQ_KEY = "enabledReqKey";
  private static final String NATIVE_LANG = "nativeLang";
  public static final String UNKNOWN = "unknown";
  private String language;
  private long defectDetector;
private boolean enableAllUsers;

  private static final String ID = "id";
  public static final String AGE = "age";
  public static final String GENDER = "gender";
  public static final String EXPERIENCE = "experience";
  public static final String IPADDR = "ipaddr";
  public static final String DIALECT = "dialect";
  /**
   * For spreadsheet download
   */
  private static final List<String> COLUMNS2 = Arrays.asList(ID,
      "userid",
      DIALECT,
      AGE,
      GENDER,
      EXPERIENCE,
      "permissions",
      "items complete?",
      "num recordings", "rate(sec)",
      IPADDR,
      "timestamp",
      KIND,
      PASS,
      EMAIL,
      DEVICE
  );
  public static final int DEFAULT_USER_ID = -1;
  public static final int DEFAULT_MALE_ID = -2;
  public static final int DEFAULT_FEMALE_ID = -3;
  public static MiniUser DEFAULT_USER = new MiniUser(DEFAULT_USER_ID, 30, 0, "default", false);
  public static MiniUser DEFAULT_MALE = new MiniUser(DEFAULT_MALE_ID, 30, 0, "Male", false);
  public static MiniUser DEFAULT_FEMALE = new MiniUser(DEFAULT_FEMALE_ID, 30, 1, "Female", false);

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

      defectDetector = userExists(DEFECT_DETECTOR);
      if (defectDetector == -1) {
        defectDetector = addUser(89, MALE, 0, "", UNKNOWN, UNKNOWN, DEFECT_DETECTOR, false, new ArrayList<User.Permission>(), User.Kind.STUDENT, "", "", "");
      }
    } catch (Exception e) {
      logger.error("got " + e, e);
      database.logEvent("unk", "create user table " + e.toString(), 0, UNKNOWN);
    }
  }

  /**
   * @param manager
   * @see mitll.langtest.server.database.DatabaseImpl#makeDAO(boolean, String, boolean, String, String)
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
  public User addUser(String userID, String passwordH, String emailH, User.Kind kind, String ipAddr, boolean isMale, int age, String dialect, String device) {
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

      long l = addUser(age, isMale ? MALE : FEMALE, 0, ipAddr, "", dialect, userID, enabled, perms, kind, passwordH, emailH, device);
      User userWhere = getUserWhere(l);
      logger.debug(language + " : addUser : added new user " + userWhere);

      return userWhere;
    }
  }

  /**
   * Somehow on subsequent runs, the ids skip by 30 or so?
   * <p/>
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
   * @return newly inserted user id, or 0 if something goes horribly wrong
   * @see DatabaseImpl#addUser
   */
  public long addUser(int age, String gender, int experience, String ipAddr,
                      String nativeLang, String dialect, String userID, boolean enabled, Collection<User.Permission> permissions,
                      User.Kind kind, String passwordH, String emailH, String device) {
    if (passwordH == null) new Exception().printStackTrace();
    try {
      // there are much better ways of doing this...
      long max = 0;
      for (User u : getUsers()) if (u.getId() > max) max = u.getId();
//      logger.info("addUser : max is " + max + " new user '" + userID + "' age " + age + " gender " + gender + " pass " + passwordH);

      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement = connection.prepareStatement(
          "INSERT INTO " +
              USERS +
              "(" +
              ID +
              ",age,gender,experience,ipaddr,nativeLang,dialect,userID," +
              ENABLED + "," +
              PERMISSIONS + "," +
              KIND + "," +
              PASS + "," +
              EMAIL + "," +
              DEVICE +

              ") " +
              "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?);");
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
        EMAIL + "='" + emailH + "' AND UPPER(" +
        USER_ID + ")='" + user.toUpperCase() + "'";

    int i = userExistsSQL("N/A", sql);
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
   *
   * @param id
   * @param passwordHash
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#userExists(String, String)
   * @see mitll.langtest.server.ScoreServlet#doGet
   */
  public User getUser(String id, String passwordHash) {
    logger.debug(language + " : getUser getting user with id '" + id + "' and pass '" + passwordHash +"'");
    String sql = "SELECT * from " +
        USERS +
        " where " +
        "(UPPER(" +
        USER_ID +
        ")='" + id.toUpperCase() + "' OR " +
        EMAIL + "='" + id.toUpperCase() +

        "') and " + PASS + "='" + passwordHash +
        "'";

    User userWhere = getUserWhere(-1, sql);
    if (userWhere == null) {
/*
      logger.debug("getUser sql '" +sql+
          "' " +
          " no user with id '" + id + "' and pass " + passwordHash);
*/

      userWhere = getUserByID(id);
      logger.debug(language + " : getUser user with id '" + id + "' pass " + passwordHash +
          " and empty or different pass is " + userWhere);
    }
    return userWhere;
  }

  /**
   * Case insensitive
   *
   * @param id
   * @return
   */
  private User getUserByID(String id) {
    return getUserWhere(-1, "SELECT * from users where UPPER(" +
        USER_ID +
        ")='" +
        id.toUpperCase() +
        "'");
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
      finish(connection, statement, rs);

    } catch (Exception e) {
      logger.error("Got " + e, e);
      database.logEvent(id, "userExists: " + e.toString(), 0, UNKNOWN);
    }
    return val;
  }

  public void createTable(Database database) throws Exception {
    Connection connection = database.getConnection(this.getClass().toString());

    try {
      PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS users (" +
          ID +
          " IDENTITY, " +
          "age INT, " +
          "gender INT, " +
          "experience INT, " +
          "ipaddr VARCHAR, " +
          "password VARCHAR, " +
          "nativeLang VARCHAR, " +
          "dialect VARCHAR, " +
          USER_ID + " VARCHAR, " +
          "timestamp TIMESTAMP AS CURRENT_TIMESTAMP, " +
          "enabled BOOLEAN, " +
          RESET_PASSWORD_KEY + " VARCHAR, " +
          ENABLED_REQ_KEY + " VARCHAR, " +
          PERMISSIONS + " VARCHAR, " +
          KIND + " VARCHAR, " +
          PASS + " VARCHAR, " +
          EMAIL + " VARCHAR, " +
          DEVICE + " VARCHAR, " +
          "CONSTRAINT pkusers PRIMARY KEY (id))");
      statement.execute();
      statement.close();
      database.closeConnection(connection);

      //logger.debug("found " + numColumns + " in users table");

      Set<String> expected = new HashSet<String>();
      expected.addAll(Arrays.asList(ID, AGE, GENDER, EXPERIENCE,
          IPADDR, "nativelang", DIALECT, USER_ID.toLowerCase(), "timestamp", ENABLED));
      expected.removeAll(getColumns(USERS));
      if (!expected.isEmpty()) logger.info("adding columns for " + expected);
      for (String missing : expected) {
        if (missing.equalsIgnoreCase("timestamp")) {
          addColumn(connection, "timestamp", "TIMESTAMP AS CURRENT_TIMESTAMP");
        }
        if (missing.equalsIgnoreCase(ENABLED)) {
          addColumn(connection, ENABLED, "BOOLEAN");
        }
      }
      Collection<String> columns = getColumns(USERS);

      for (String col : new HashSet<String>(Arrays.asList(NATIVE_LANG, DIALECT, USER_ID, PERMISSIONS, KIND, PASS, EMAIL, DEVICE, ENABLED_REQ_KEY, RESET_PASSWORD_KEY))) {
        if (!columns.contains(col.toLowerCase())) {
          addVarcharColumn(connection, col);
        }
      }
    } finally {
      database.closeConnection(connection);
    }
  }

  private void addVarcharColumn(Connection connection, String column) throws SQLException {
    addColumn(connection, column, "VARCHAR");
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
  public List<User> getUsers() {
    return getUsers("SELECT * from users order by " + ID + " ASC");
  }

  /**
   * @return
   * @see AudioDAO#getResultsForQuery(java.sql.Connection, java.sql.PreparedStatement)
   */
  public Map<Long, MiniUser> getMiniUsers() {
    List<User> users = getUsers();
    Map<Long, MiniUser> mini = new HashMap<Long, MiniUser>();
    for (User user : users) mini.put(user.getId(), new MiniUser(user));
    return mini;
  }

  /**
   * @param userid
   * @return
   * @see AudioDAO#getAudioAttribute(int, int, String, String, long, String, int)
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
   * @see mitll.langtest.server.mail.EmailHelper#enableCDUser(String, String, String)
   * @param resetKey
   * @return
   */
  public User getUserWhereEnabledReq(String resetKey) {
    String sql = "SELECT * from users where " +
        ENABLED_REQ_KEY +
        "='" + resetKey + "';";
    return getUserWhere(-1, sql);
  }

  public boolean isMale(long userid) {
    User userWhere = getUserWhere(userid);
    return userWhere == null || userWhere.isMale();
  }

  /**
   * @param userid
   * @return null if no user with that id else the user object
   * @see mitll.langtest.server.LangTestDatabaseImpl#getUserBy(long)
   */
  public User getUserWhere(long userid) {
    String sql = "SELECT * from users where " + ID + "=" + userid + ";";
    List<User> users = getUsers(sql);
    if (users.isEmpty()) {
      if (userid > 0) {
        logger.warn(language + " : no user with id " + userid);
      }
      return null;
    } else if (users.size() > 1) {
      logger.warn(language + " : huh? " + users.size() + " with  id " + userid);
    }

    return users.iterator().next();
  }

  private User getUserWhere(long userid, String sql) {
    List<User> users = getUsers(sql);
    if (users.isEmpty()) {
      if (userid > 0) {
        logger.warn(language + " : for " + sql + " no user with id '" + userid +"'");
      }
      return null;
    }

    User next = users.iterator().next();
    logger.debug("For " + userid + " found " +next);
    return next;
  }

  private List<User> getUsers(String sql) {
    try {
      Connection connection = getConnection();
      PreparedStatement statement = connection.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      List<User> users = getUsers(rs);
      finish(connection, statement, rs);

      return users;
    } catch (Exception ee) {
      logger.error("Got " + ee, ee);
      database.logEvent("unk", "getUsers: " + ee.toString(), 0, UNKNOWN);
    }
    return new ArrayList<User>();
  }

  private List<User> getUsers(ResultSet rs) throws SQLException {
    List<User> users = new ArrayList<User>();

    while (rs.next()) {
      String userid;

      String perms = rs.getString(PERMISSIONS);
      Collection<User.Permission> permissions = new ArrayList<User.Permission>();
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
          "");

      users.add(newUser);

      if (newUser.getUserID() == null) {
        newUser.setUserID("" + newUser.getId());
      }
    }
    return users;
  }

  private boolean isAdmin(String userid) {
    return userid != null && (admins.contains(userid));
  }

  public Map<Long, User> getUserMap(boolean getMale) {
    return getUserMap(getMale, getUsers());
  }

  public Map<Long, User> getUserMap() {
    return getMap(getUsers());
  }

  private Map<Long, User> getUserMap(boolean getMale, List<User> users) {
    Map<Long, User> idToUser = new HashMap<Long, User>();
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
    Map<Long, User> idToUser = new HashMap<Long, User>();
    for (User u : users) {
      idToUser.put(u.getId(), u);
    }
    return idToUser;
  }

  public void toXLSX(OutputStream out, List<User> users) {
    writeToStream(out, getSpreadsheet(users));
  }

  private SXSSFWorkbook getSpreadsheet(List<User> users) {
    long then = System.currentTimeMillis();

    SXSSFWorkbook wb = new SXSSFWorkbook(1000); // keep 100 rows in memory, exceeding rows will be flushed to disk

    Sheet sheet = wb.createSheet("Users");
    int rownum = 0;
    Row headerRow = sheet.createRow(rownum++);
    for (int i = 0; i < COLUMNS2.size(); i++) {
      Cell headerCell = headerRow.createCell(i);
      headerCell.setCellValue(COLUMNS2.get(i));
    }

    CellStyle cellStyle = wb.createCellStyle();
    DataFormat dataFormat = wb.createDataFormat();

    cellStyle.setDataFormat(dataFormat.getFormat("MMM dd HH:mm:ss"));

    for (User user : users) {
      Row row = sheet.createRow(rownum++);
      int j = 0;
      row.createCell(j++).setCellValue(user.getId());
      row.createCell(j++).setCellValue(user.getUserID());
      row.createCell(j++).setCellValue(user.getDialect());
      row.createCell(j++).setCellValue(user.getAge());
      row.createCell(j++).setCellValue(user.getGender() == 0 ? MALE : FEMALE);
      row.createCell(j++).setCellValue(user.getExperience());

      row.createCell(j++).setCellValue(user.getPermissions().toString().replaceAll("\\[", "").replaceAll("\\]", ""));
      row.createCell(j++).setCellValue(user.isComplete() ? "Yes" : ("No (" + Math.round(100 * user.getCompletePercent()) + "%)"));
      row.createCell(j++).setCellValue("" + user.getNumResults());
      row.createCell(j++).setCellValue("" + roundToHundredth(user.getRate()));

//      row.createCell(j++).setCellValue(user.getNativeLang());
      row.createCell(j++).setCellValue(user.getIpaddr());

      Cell cell = row.createCell(j++);
      try {
        cell.setCellValue(user.getTimestamp());
      } catch (Exception e) {
        cell.setCellValue("Unknown");
      }
      cell.setCellStyle(cellStyle);

      User.Kind userKind = user.getUserKind();
      row.createCell(j++).setCellValue(userKind.toString());
      String passwordHash = user.getPasswordHash();
      row.createCell(j++).setCellValue(passwordHash == null || passwordHash.isEmpty() ? "NO_PASSWORD" : "HAS_PASSWORD");
      String emailHash = user.getEmailHash();
      row.createCell(j++).setCellValue(emailHash == null || emailHash.isEmpty() ? "NO_EMAIL" : "HAS_EMAIL");
    }
    long now = System.currentTimeMillis();
    long diff = now - then;
    if (diff > 100) {
      logger.info("toXLSX : took " + diff + " millis to write " + rownum +
          " rows to sheet, or " + diff / rownum + " millis/row.");
    }
    return wb;
  }

  private void writeToStream(OutputStream out, SXSSFWorkbook wb) {
    long now;
    try {
      long then = System.currentTimeMillis();
      wb.write(out);
      now = System.currentTimeMillis();
      if (now - then > 100) {
        logger.warn(language + " : toXLSX : took " + (now - then) + " millis to write excel to output stream ");
      }
      out.close();
      wb.dispose();
    } catch (IOException e) {
      logger.error("got " + e, e);
      database.logEvent("unk", "(" +language+ ") toXLSX: " + e.toString(), 0, UNKNOWN);
    }
  }

  private float roundToHundredth(double totalHours) {
    return ((float) ((Math.round(totalHours * 100)))) / 100f;
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
      logger.debug("after password set to " + passwordH+      " user now " +userByID);
      return i1 != 0;
    } catch (Exception ee) {
      logger.error("Got " + ee, ee);
      database.logEvent("unk", "update user: " + ee.toString(), 0, UNKNOWN);
    }
    return false;
  }

  /**
   *
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
      logger.debug("for " + language + " update " + key + "/" +s+ " for " + userid);
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
}