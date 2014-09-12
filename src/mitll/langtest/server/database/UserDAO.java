package mitll.langtest.server.database;

import mitll.langtest.server.database.custom.UserListManager;
import mitll.langtest.shared.MiniUser;
import mitll.langtest.shared.User;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UserDAO extends DAO {
  private static final String DEFECT_DETECTOR = "defectDetector";
  private static final Logger logger = Logger.getLogger(UserDAO.class);
  public static final String USERS = "users";
  public static final String MALE = "male";
  public static final String FEMALE = "female";
  private static final String PERMISSIONS = "permissions";
  private long defectDetector;
  public static final String ID = "id";
  private static final List<String> COLUMNS2 = Arrays.asList(ID,
    "userid",
    "dialect",
    "age",
    "gender",
    "experience",
    "permissions",
    "items complete?",
    "num recordings", "rate(sec)",
    "ipaddr",
    "timestamp"
  );
  public static final int DEFAULT_USER_ID = -1;
  public static final int DEFAULT_MALE_ID = -2;
  public static final int DEFAULT_FEMALE_ID = -3;
  public static MiniUser DEFAULT_USER = new MiniUser(DEFAULT_USER_ID, 30, 0, "default", "default", "default");
  public static MiniUser DEFAULT_MALE = new MiniUser(DEFAULT_MALE_ID, 30, 0, "default", "default", "Male");
  public static MiniUser DEFAULT_FEMALE = new MiniUser(DEFAULT_FEMALE_ID, 30, 1, "default", "default", "Female");

  public UserDAO(Database database) {
    super(database);
    try {
      createUserTable(database);

      defectDetector = userExists(DEFECT_DETECTOR);
      if (defectDetector == -1) {
        defectDetector = addUser(89, MALE, 0, "", "unknown", "unknown", DEFECT_DETECTOR, false, new ArrayList<User.Permission>());
      }
    } catch (Exception e) {
      logger.error("got "+e,e);
      database.logEvent("unk","create user table "+e.toString(),0);
    }
  }

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#makeDAO(boolean, String, boolean, String, String)
   * @param manager
   */
  public void checkForFavorites(UserListManager manager) {
    for (User u : getUsers()) {
      if (manager.getListsForUser(u.getId(), true, false).isEmpty()) {
        manager.createFavorites(u.getId());
      }
    }
  }

  /**
   * @see mitll.langtest.server.database.custom.UserListManager#addDefect(String, String, String)
   * @see mitll.langtest.server.database.custom.AnnotationDAO#AnnotationDAO(Database, UserDAO)
   * @return
   */
  public long getDefectDetector() { return defectDetector; }

  /**
   * Somehow on subsequent runs, the ids skip by 30 or so?
   * <p/>
   * Uses return generated keys to get the user id
   *
   * @see DatabaseImpl#addUser
   * @param age
   * @param gender
   * @param experience
   * @param ipAddr
   * @param nativeLang
   * @param dialect
   * @param userID
   * @param enabled
   * @param permissions
   * @return newly inserted user id, or 0 if something goes horribly wrong
   */
  public long addUser(int age, String gender, int experience, String ipAddr,
                      String nativeLang, String dialect, String userID, boolean enabled, Collection<User.Permission> permissions) {
    try {
      // there are much better ways of doing this...
      long max = 0;
      for (User u : getUsers()) if (u.getId() > max) max = u.getId();
      logger.info("addUser : max is " + max + " new user '" + userID + "' age " +age + " gender " + gender);

      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement = connection.prepareStatement(
          "INSERT INTO " +
            USERS +
            "(id,age,gender,experience,ipaddr,nativeLang,dialect,userID,enabled," +
            PERMISSIONS +
            ") " +
          "VALUES(?,?,?,?,?,?,?,?,?,?);");
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

      statement.executeUpdate();

      finish(connection, statement);

      return newID;
    } catch (Exception ee) {
      logger.error("Got " + ee, ee);
      database.logEvent("unk", "adding user: " + ee.toString(), 0);
    }
    return 0;
  }

  /**
   * Not case sensitive.
   *
   * @see DatabaseImpl#userExists(String)
   * @param id
   * @return
   */
  public int userExists(String id) {
    int val = -1;
    try {
      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement = connection.prepareStatement("SELECT id from users where UPPER(userID)='" +
          id.toUpperCase() +
          "'");
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        val = rs.getInt(1);
      }
  //    logger.debug("user exists " + id + " = " + val);
      finish(connection, statement, rs);
    } catch (Exception e) {
      logger.error("Got " + e,e);
      database.logEvent(id, "userExists: " + e.toString(), 0);
    }
    return val;
  }

  public void createUserTable(Database database) throws Exception {
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
        "userID VARCHAR, " +
        "timestamp TIMESTAMP AS CURRENT_TIMESTAMP, " +
        "enabled BOOLEAN, " +
        PERMISSIONS + " VARCHAR, " +
        "CONSTRAINT pkusers PRIMARY KEY (id))");
      statement.execute();
      statement.close();

      //logger.debug("found " + numColumns + " in users table");

      Set<String> expected = new HashSet<String>();
      expected.addAll(Arrays.asList(ID,"age","gender","experience",
        //"firstname","lastname",
        "ipaddr","nativelang","dialect","userid","timestamp","enabled"));
    /*boolean users = */
      expected.removeAll(getColumns(USERS));
      if (!expected.isEmpty()) logger.info("adding columns for " + expected);
      for (String missing : expected) {
        //if (missing.equalsIgnoreCase("firstName")) { addVarchar(connection,"firstName","VARCHAR"); }
        //if (missing.equalsIgnoreCase("lastName")) { addVarchar(connection,"lastName","VARCHAR"); }

        if (missing.equalsIgnoreCase("nativeLang")) { addColumn(connection,"nativeLang","VARCHAR"); }
        if (missing.equalsIgnoreCase("dialect")) { addColumn(connection,"dialect","VARCHAR"); }
        if (missing.equalsIgnoreCase("userID")) { addColumn(connection,"userID","VARCHAR"); }
        if (missing.equalsIgnoreCase("timestamp")) { addColumn(connection,"timestamp","TIMESTAMP AS CURRENT_TIMESTAMP"); }
        if (missing.equalsIgnoreCase("enabled")) { addColumn(connection,"enabled","BOOLEAN"); }
      }
      if (!getColumns(USERS).contains(PERMISSIONS)) {
        addColumn(connection, PERMISSIONS,"VARCHAR");
      }
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
   * @return
   */
  public List<User> getUsers() { return getUsers("SELECT * from users;");  }

  /**
   * @see AudioDAO#getResultsForQuery(java.sql.Connection, java.sql.PreparedStatement)
   * @return
   */
  public Map<Long, MiniUser> getMiniUsers() {
    List<User> users = getUsers();
    Map<Long, MiniUser> mini = new HashMap<Long, MiniUser>();
    for (User user : users) mini.put(user.getId(), new MiniUser(user));
    return mini;
  }

  /**
   * @see AudioDAO#getAudioAttribute(int, int, String, String, long, String, int)
   * @param userid
   * @return
   */
  public MiniUser getMiniUser(long userid) {
    User userWhere = getUserWhere(userid);
    return userWhere == null ? null : new MiniUser(userWhere);
  }

  public boolean isMale(long userid) {
    User userWhere = getUserWhere(userid);
    return userWhere == null || userWhere.isMale();
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getUserBy(long)
   * @param userid
   * @return
   */
  public User getUserWhere(long userid) {
    String sql = "SELECT * from users where " +
        ID +
        "=" +userid+";";
    List<User> users = getUsers(sql);
    if (users.isEmpty()) {
      if (userid > 0) {
        logger.warn("no user with id " + userid);
      }
      return null;
    }
    else if (users.size() > 1) {
      logger.warn("huh? " + users.size() + " with  id " + userid);
    }

    return users.iterator().next();
  }

  private List<User> getUsers(String sql) {
    try {
      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement = connection.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      List<User> users = getUsers(rs);
      finish(connection, statement, rs);

      return users;
    } catch (Exception ee) {
      logger.error("Got " + ee,ee);
      database.logEvent("unk","getUsers: "+ee.toString(),0);
    }
    return new ArrayList<User>();
  }

  private List<User> getUsers(ResultSet rs) throws SQLException {
    int i;

    List<User> users = new ArrayList<User>();
    int columnCount = rs.getMetaData().getColumnCount();
    if (columnCount == 7) {
      while (rs.next()) {
        i = 1;
        users.add(new User(rs.getLong(i++), //id
            rs.getInt(i++), // age
            rs.getInt(i++), //gender
            rs.getInt(i++), // exp
            rs.getString(i++), // ip
            rs.getString(i++), // password
            rs.getBoolean(i++), new ArrayList<User.Permission>()));
      }
    } else {
      while (rs.next()) {
        String userid;

        String perms = rs.getString(PERMISSIONS);
        Collection<User.Permission> permissions = new ArrayList<User.Permission>();
        userid = rs.getString("userid"); // userid

        if (perms != null) {
          perms = perms.replaceAll("\\[", "").replaceAll("\\]", "");
          for (String perm : perms.split(",")) {
            try {
              if (!perm.isEmpty()) {
                permissions.add(User.Permission.valueOf(perm));
              }
            } catch (IllegalArgumentException e) {
              logger.warn("huh, for user " + userid+
                  " perm '" + perm+
                  "' is not a permission?");
            }
          }
        }


        User newUser = new User(rs.getLong(ID), //id
          rs.getInt("age"), // age
          rs.getInt("gender"), //gender
          rs.getInt("experience"), // exp
          rs.getString("ipaddr"), // ip
          rs.getString("password"), // password

          // first
          // last
          rs.getString("nativeLang"), // native
          rs.getString("dialect"), // dialect
          userid, // userid
          rs.getTimestamp("timestamp").getTime(),

          rs.getBoolean("enabled"),
          userid != null && (userid.equals("gvidaver") | userid.equals("swade")),
          permissions);

        users.add(newUser);

        if (newUser.getUserID() == null) {
          newUser.setUserID(""+ newUser.getId());
        }
      }
    }
    return users;
  }

  public Map<Long, User> getUserMap(boolean getMale) {  return getUserMap(getMale, getUsers());  }
  public Map<Long, User> getUserMap() {  return getMap(getUsers());  }

  private Map<Long, User> getUserMap(boolean getMale, List<User> users) {
    Map<Long,User> idToUser = new HashMap<Long, User>();
    for (User u : users) {
      if (u.isMale() && getMale || (!u.isMale() && !getMale)) {
        idToUser.put(u.getId(), u);
      }
    }
    return idToUser;
  }

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#joinWithDLIUsers(java.util.List)
   * @param users
   * @return
   */
  public Map<Long, User> getMap(List<User> users) {
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

    //Workbook wb = new XSSFWorkbook();
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
      row.createCell(j++).setCellValue(user.getGender() == 0 ? "male" : "female");
      row.createCell(j++).setCellValue(user.getExperience());

      row.createCell(j++).setCellValue(user.getPermissions().toString().replaceAll("\\[","").replaceAll("\\]",""));
      row.createCell(j++).setCellValue(user.isComplete() ? "Yes":("No (" +Math.round(100*user.getCompletePercent())+  "%)"));
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
      //cell = row.createCell(j++);
      //Demographics demographics = user.getDemographics();
   //   if (demographics == null) logger.warn("no demographics for " + user);
      //cell.setCellValue(demographics == null ? "" :demographics.toString());
    }
    long now = System.currentTimeMillis();
    long diff = now - then;
    if (diff > 100) {
      logger.info("toXLSX : took " + diff + " millis to write " + rownum+
          " rows to sheet, or " + diff /rownum + " millis/row." );
    }
    return wb;
  }

  private void writeToStream(OutputStream out, SXSSFWorkbook wb) {
    long now;
    try {
      long then = System.currentTimeMillis();
      wb.write(out);
      now = System.currentTimeMillis();
      if (now-then > 100) {
        logger.warn("toXLSX : took " + (now-then) + " millis to write excel to output stream ");
      }
    //  then = now;
      out.close();
      wb.dispose();
    } catch (IOException e) {
      logger.error("got " +e,e);
      database.logEvent("unk","toXLSX: "+e.toString(),0);

    }
  }

  private float roundToHundredth(double totalHours) { return ((float) ((Math.round(totalHours * 100)))) / 100f;  }
}