package mitll.langtest.server.database;

import com.google.gwt.core.client.GWT;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.User;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Note with H2 that :
 *  * you can corrupt the database if you try to copy a file that's in use by another process.
 *  * one process can lock the database and make it inaccessible to a second one, seemingly this can happen
 *    more easily when H2 lives inside a servlet container (e.g. tomcat).
 *  * it's not a good idea to close connections, especially in the context of a servlet inside a container, since
 *    H2 will return "new" connections that have already been closed.
 *  * it's not a good idea to reuse one connection...?
 *
 * User: go22670
 * Date: 5/14/12
 * Time: 11:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class DatabaseImpl {
  private static final boolean TESTING = false;

  private static final String ENCODING = "UTF8";
  private static final boolean DROP_USER = false;
  private static final boolean DROP_RESULT = false;
  private static final String H2_DB_NAME = TESTING ? "vlr-parle" : "/services/apache-tomcat-7.0.27/webapps/langTest/vlr-parle";
  private static final boolean LOG_RESULTS = false;
  private static final String TIME = "time";
  private static final String EXID = "exid";
  private Map<Long, List<Schedule>> userToSchedule;

  // mysql config info
/*  private String url = "jdbc:mysql://localhost:3306/",
    dbOptions = "?characterEncoding=utf8&zeroDateTimeBehavior=convertToNull",
    driver = "com.mysql.jdbc.Driver";*/

  // h2 config info
  private String url = "jdbc:h2:" + H2_DB_NAME + ";IFEXISTS=TRUE;QUERY_CACHE_SIZE=0;",
    dbOptions = "",//"?characterEncoding=utf8&zeroDateTimeBehavior=convertToNull",
    driver = "org.h2.Driver";

  private HttpServlet servlet;

  public DatabaseImpl(HttpServlet s) {
    this.servlet = s;
    try {
      boolean open = getConnection() != null;
      if (!open) {
        System.err.println("couldn't open connection to database");
        return;
      }
    } catch (Exception e) {
      System.err.println("couldn't open connection to database, got " +e.getMessage());
      e.printStackTrace();
      return;
    }
    this.userToSchedule = getSchedule();

    if (DROP_USER) {
      try {
        dropUserTable();
        createUserTable();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    if (DROP_RESULT) {
      System.out.println("------------ dropping results table");
      dropResults();
      try {
        createResultTable(getConnection());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private static class Schedule {
    public long id;
    public String plan;
    public long userid;
    public String exid;
    public boolean flQ;
    public boolean spoken;

    public Schedule(ResultSet rs) {
      int i = 1;
      try {
        id = rs.getLong(i++);
        plan = rs.getString(i++);
        userid = rs.getLong(i++);
        exid = rs.getString(i++);
        flQ = rs.getBoolean(i++);
        spoken = rs.getBoolean(i++);
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * schedules (id LONG, plan VARCHAR, userid LONG, exid VARCHAR, flQ BOOLEAN, spoken BOOLEAN, CONSTRAINT pksched PRIMARY KEY (id, plan, userid))");
   */
  private Map<Long, List<Schedule>> getSchedule() {
    Connection connection;
    SortedSet<String> ids = new TreeSet<String>();

    List<Schedule> schedules = new ArrayList<Schedule>();
    try {
      connection = getConnection();
      PreparedStatement statement = connection.prepareStatement("SELECT * FROM schedules");

      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        schedules.add(new Schedule(rs));
      }
      rs.close();
      statement.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
    Map<Long, List<Schedule>> userToSchedule = new HashMap<Long, List<Schedule>>();
    for (Schedule s : schedules) {
      ids.add(s.exid);
      List<Schedule> forUser = userToSchedule.get(s.userid);
      if (forUser == null) {
        userToSchedule.put(s.userid, forUser = new ArrayList<Schedule>());
      }
      forUser.add(s);
    }
    return userToSchedule;
  }

  public List<Exercise> getExercises(long userID) {
    List<Schedule> forUser = userToSchedule.get(userID);
    if (forUser == null) {
      System.err.println("no schedule for user " +userID);
      return getRawExercises();
    }
    List<Exercise> exercises = new ArrayList<Exercise>();

    List<Exercise> rawExercises = getRawExercises();
    Map<String,Exercise> idToExercise = new HashMap<String, Exercise>();
    for (Exercise e : rawExercises) { idToExercise.put(e.getID(),e); }
    for (Schedule s : forUser) {
      Exercise exercise = idToExercise.get(s.exid);
      if (exercise == null) {
        System.err.println("no exercise for id " +s.exid + "? Foreign key constraint violated???");
        continue;
      }
      exercise.setPromptInEnglish(!s.flQ);
      exercise.setRecordAnswer(s.spoken);
      exercises.add(exercise);
    }
    return exercises;
  }
    /**
    * Hit the database for the exercises
    *
    * @see mitll.langtest.server.LangTestDatabaseImpl#getExercises
    * @return
    */
  public List<Exercise> getRawExercises() {
    List<Exercise> exercises = new ArrayList<Exercise>();
    Connection connection = null;
    SortedSet<String> ids = new TreeSet<String>();
    try {
      connection = getConnection();
      PreparedStatement statement = connection.prepareStatement("SELECT * FROM exercises");

      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        String plan = rs.getString(1);
        String exid = rs.getString(2);
        String s = getStringFromClob(rs.getClob(4));

        if (s.startsWith("{")) {
          net.sf.json.JSONObject obj = net.sf.json.JSONObject.fromObject(s);
          Exercise e = getExercise(plan, exid, obj);
          if (e == null) {
            System.err.println("couldn't find exercise for plan '" +plan+ "'");
            continue;
          }
          if (e.getID() == null) {
            System.err.println("no valid exid for " +e);
            continue;
          }
          ids.add(e.getID());
          exercises.add(e);
        }
      }
      rs.close();
      statement.close();
      closeConnection(connection);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (connection != null) {
        /*   try {
          //connection.close();
        } catch (SQLException e) {
          e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }*/
      }
    }
    return exercises;
  }

  private String getStringFromClob(Clob clob) throws SQLException, IOException {
    InputStreamReader utf8 = new InputStreamReader(clob.getAsciiStream(), ENCODING);
    BufferedReader br = new BufferedReader(utf8);
    int c;
    char cbuf[] = new char[1024];
    StringBuilder b = new StringBuilder();
    while ((c = br.read(cbuf)) != -1) {
      b.append(cbuf, 0, c);
    }
    return b.toString();
  }


  /**
   * Parse the json that represents the exercise.  Created during ingest process (see ingest.scala).
   *
   * @param obj
   * @return
   */
  private Exercise getExercise(String plan, String exid, JSONObject obj) {
    boolean promptInEnglish = false;
    boolean recordAudio = false;
    String content = (String) obj.get("content");
    if (content == null) {
      System.err.println("no content key in " + obj.keySet());
    }
    Exercise exercise = new Exercise(plan, exid, content, promptInEnglish, recordAudio);
    Object qa1 = obj.get("qa");
    if (qa1 == null) {
      System.err.println("no qa key in " + obj.keySet());
    }
    Collection<JSONObject> qa = JSONArray.toCollection((JSONArray) qa1, JSONObject.class);
    for (JSONObject o : qa) {
      Set<String> keys = o.keySet();
      for (String k : keys) {
        JSONObject qaForLang = (JSONObject) o.get(k);
        exercise.addQuestion(k, (String) qaForLang.get("question"), (String) qaForLang.get("answerKey"));
      }
    }
    return exercise;
  }

  /**
   * Not necessary if we use the h2 DBStarter service -- see web.xml reference
   *
   * @return
   * @throws Exception
   */
  private Connection dbLogin() throws Exception {
    try {
      Class.forName(driver).newInstance();
      try {
        url = servlet.getServletContext().getInitParameter("db.url"); // from web.xml
      } catch (Exception e) {
        System.err.println("no servlet context?");
        //e.printStackTrace();
      }
      System.out.println("connecting to " + url);

      GWT.log("connecting to " + url);
      File f = new java.io.File(H2_DB_NAME +
        ".h2.db");
      if (!f.exists()) {
        String s = "huh? no file at " + f.getAbsolutePath();
        System.err.println(s);

        GWT.log(s);
      }
      Connection connection = DriverManager.getConnection(url + dbOptions);
      connection.setAutoCommit(false);
      boolean closed = connection.isClosed();
      if (closed) {
        System.err.println("connection is closed to : " + url);
      }
      return connection;
    } catch (Exception ex) {
      ex.printStackTrace();
      throw ex;
    }
  }

  // should we have one connection???
  // Connection c = null;

  private Connection getConnection() throws Exception {
  //  if (c != null) return c;
	  Connection c;
    try {
      if (servlet == null) {
        c = this.dbLogin();
      } else {
        ServletContext servletContext = servlet.getServletContext();
        c = (Connection) servletContext.getAttribute("connection");
      }
    } catch (Exception e) {  // for standalone testing
      System.err.println("The context DBStarter is not working : " + e.getMessage());
      e.printStackTrace();
      c = this.dbLogin();
    }
    if (c == null) {
      return c;
    }
    c.setAutoCommit(true);
    if (c.isClosed())  {
      System.err.println("getConnection : conn " + c + " is closed!");
    }
    return c;
  }

  /**
   * Somehow on subsequent runs, the ids skip by 30 or so?
   *
   * Uses return generated keys to get the user id
   *
   * @param age
   * @param gender
   * @param experience
   * @param ipAddr
   * @return
   */
  public long addUser(int age, String gender, int experience, String ipAddr) {
    try {
      Connection connection = getConnection();
      PreparedStatement statement;

      //System.out.println("adding " + age + " and " + gender + " and " + experience);
      statement = connection.prepareStatement("INSERT INTO users(age,gender,experience,ipaddr) VALUES(?,?,?,?);",
        Statement.RETURN_GENERATED_KEYS);
      int i = 1;
      statement.setInt(i++, age);
      statement.setInt(i++, gender.equalsIgnoreCase("male") ? 0 : 1);
      statement.setInt(i++, experience);
      statement.setString(i++, ipAddr);
      statement.executeUpdate();

      ResultSet rs = statement.getGeneratedKeys(); // will return the ID in ID_COLUMN

      long id = 0;
      while (rs.next()) {
        id = rs.getLong(1);
        //System.out.println("DatabaseImpl : addUser got user #" + id);
        //  System.out.println(rs.getString(1) + "," + rs.getString(2) + "," + rs.getInt(3) + "," + rs.getString(4) + "," + rs.getTimestamp(5));
      }
      rs.close();
      statement.close();
      closeConnection(connection);

      return id;
    } catch (Exception ee) {
      ee.printStackTrace();
    }
    return 0;
  }

  private void createUserTable() throws Exception {
    Connection connection = getConnection();

    PreparedStatement statement;

    statement = connection.prepareStatement("CREATE TABLE if not exists users (id IDENTITY, " +
      "age INT, gender INT, experience INT, ipaddr VARCHAR, password VARCHAR, timestamp TIMESTAMP AS CURRENT_TIMESTAMP, CONSTRAINT pkusers PRIMARY KEY (id))");
    statement.execute();
    statement.close();
    closeConnection(connection);

  }

  private void dropUserTable() throws Exception {
    System.err.println("----------- dropUserTable -------------------- ");
    Connection connection = getConnection();
    PreparedStatement statement;
    statement = connection.prepareStatement("drop TABLE users");
    statement.execute();
    statement.close();
    closeConnection(connection);
  }

  /**
   * Pulls the list of users out of the database.
   * @return
   */
  public List<User> getUsers() {
    try {
      Connection connection = getConnection();
      PreparedStatement statement;

      statement = connection.prepareStatement("SELECT * from users;");
      int i = 1;

      ResultSet rs = statement.executeQuery();
      List<User> users = new ArrayList<User>();
      while (rs.next()) {
    	  i = 1;
        Timestamp timestamp;
        if (rs.getMetaData().getColumnCount() == 7) { // if we have a timestamp column --
          timestamp = rs.getTimestamp(i++);
        }
        else { // Wade's db schema doesn't have a timestamp column, currently
          timestamp = new Timestamp(System.currentTimeMillis());
        }
        users.add(new User(rs.getLong(i++), //id
          rs.getInt(i++), // age
          rs.getInt(i++), //gender
          rs.getInt(i++), // exp
          rs.getString(i++), // ip
          rs.getString(i++), // password
          timestamp.getTime()
        ));
      }
      rs.close();
      statement.close();
      closeConnection(connection);

      return users;
    } catch (Exception ee) {
      ee.printStackTrace();
    }
    return new ArrayList<User>();
  }


  /**
   * Pulls the list of results out of the database.
   * @return
   */
  public List<Result> getResults() {
    try {
      Connection connection = getConnection();
      PreparedStatement statement = connection.prepareStatement("SELECT * from results;");

      ResultSet rs = statement.executeQuery();
      List<Result> results = new ArrayList<Result>();
      while (rs.next()) {
        int i = 1;
        rs.getInt(i++);
        long userID = rs.getLong(i++);
        String plan = rs.getString(i++);
        String exid = rs.getString(i++);
        int qid = rs.getInt(i++);
        Timestamp timestamp = rs.getTimestamp(i++);
        String answer = rs.getString(i++);
        boolean valid = rs.getBoolean(i++);
        results.add(new Result(userID, //id
          plan, // plan
          exid, // id
          qid, // qid
          answer, // answer
          //rs.getString(i++), // audioFile
          valid, // valid
          timestamp.getTime()
        ));
      }
      rs.close();
      statement.close();
      closeConnection(connection);

      return results;
    } catch (Exception ee) {
      ee.printStackTrace();
    }
    return new ArrayList<Result>();
  }

  /**
   * Creates the result table if it's not there.
   *
   * @param userID
   * @param e
   * @param questionID
   * @param answer
   * @param audioFile
   * @see mitll.langtest.client.ExercisePanel#postAnswers(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.UserFeedback, mitll.langtest.client.ExerciseController, mitll.langtest.shared.Exercise)
   */
  public void addAnswer(int userID, Exercise e, int questionID, String answer, String audioFile) {
    String plan = e.getPlan();
    String id = e.getID();
    addAnswer(userID, plan, id, questionID, answer, audioFile, true);
  }

  public boolean isAnswerValid(int userID, Exercise e, int questionID) {
    boolean val = false;
    try {
      Connection connection = getConnection();
      PreparedStatement statement = connection.prepareStatement(
        "SELECT valid, " + TIME +
        " FROM results " +
        "WHERE userid = ? AND plan = ? AND " +
          EXID +
          " = ? AND qid = ? " +
        "order by " +TIME+ " desc");

      statement.setInt(1,userID);
      statement.setString(2, e.getPlan());
      statement.setString(3, e.getID());
      statement.setInt(4, questionID);

      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        val = rs.getBoolean(1);
       // Timestamp timestamp = rs.getTimestamp(2);
       // System.out.println(timestamp + " : " + val);
        break;
      }
      rs.close();
      statement.close();
      closeConnection(connection);
    } catch (Exception e1) {
      e1.printStackTrace();
    }
    return val;
  }

  public void destroy() {
    try {
  /*   Connection connection = getConnection();
     if (!connection.isClosed()) {
        connection.close();
     }*/
  //    DriverManager.deregisterDriver((Driver)Class.forName(driver).newInstance());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * @see mitll.langtest.server.UploadServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   * @param userID
   * @param plan
   * @param id
   * @param questionID
   * @param answer
   * @param audioFile
   * @param valid
   */
  public void addAnswer(int userID, String plan, String id, int questionID, String answer, String audioFile, boolean valid) {
    try {
      Connection connection = getConnection();
      addAnswerToTable(userID, plan, id, questionID, answer, audioFile, connection, valid);
      closeConnection(connection);

      if (LOG_RESULTS) { // true to see what is in the table
        try {
          showResults();
        } catch (Exception e1) {
          e1.printStackTrace();
        }
      }

    } catch (Exception ee) {
      ee.printStackTrace();
    }
  }

  private void closeConnection(Connection connection) throws SQLException {
  //  System.err.println("Closing " + connection);
    //connection.close();
   // System.err.println("Closing " + connection + " " + connection.isClosed());
  }

  private void dropResults() {
    try {
      Connection connection = getConnection();
      PreparedStatement statement = connection.prepareStatement("DROP TABLE if exists results");
      if (!statement.execute()) {
        System.err.println("couldn't create table?");
      }
      statement.close();
      closeConnection(connection);

    } catch (Exception e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.  }
    }
  }

  /**
   *
   * @param userid
   * @param plan
   * @param id
   * @param questionID
   * @param answer
   * @param connection
   * @param valid
   * @throws SQLException
   * @see #addAnswer
   */
  private void addAnswerToTable(int userid, String plan, String id, int questionID, String answer, String audioFile,
                                Connection connection, boolean valid) throws SQLException {
    PreparedStatement statement;
    //statement = connection.prepareStatement("INSERT INTO results(userid,plan,id,qid,answer,audioFile,valid) VALUES(?,?,?,?,?,?,?)");
    statement = connection.prepareStatement("INSERT INTO results(userid,plan," +
      EXID +
      ",qid,answer,valid) VALUES(?,?,?,?,?,?)");
    int i = 1;
    statement.setInt(i++, userid);
    statement.setString(i++, plan);
    statement.setString(i++, id);
    statement.setInt(i++, questionID);
    //System.err.println("got " + userid + ", " + plan +", "+ id +", " + questionID + ", " +answer + ", " +audioFile +", " + valid);

    boolean isAudioAnswer = answer == null || answer.length() == 0;
    String x = isAudioAnswer ? audioFile : answer;
    //  System.err.println("got " + answer + " and " + audioFile + " -> " + x);
    statement.setString(i++, x);
    //statement.setString(i++, audioFile);
    statement.setBoolean(i++, valid);
    statement.executeUpdate();
    statement.close();
  }

  private void showResults() throws Exception {
    Connection connection = getConnection();
    PreparedStatement statement = connection.prepareStatement("SELECT * FROM results order by " + TIME);
    ResultSet rs = statement.executeQuery();
    int c = 0;
    while (rs.next()) {
      c++;
      int i = 1;
      if (false) {
        System.out.println(rs.getInt(i++) + "," + rs.getString(i++) + "," +
          rs.getString(i++) + "," +
          rs.getInt(i++) + "," +
          rs.getString(i++) + "," +
          rs.getString(i++) + "," +
          rs.getTimestamp(i++));
      }
    }
 //   System.out.println("now " + c + " answers");
    rs.close();
    statement.close();
    closeConnection(connection);
  }

  private void createResultTable(Connection connection) throws SQLException {
    PreparedStatement statement = connection.prepareStatement("CREATE TABLE if not exists " +
      "results (id IDENTITY, userid INT, plan VARCHAR, " +
      EXID +" VARCHAR, " +
      "qid INT," +
      TIME + " TIMESTAMP AS CURRENT_TIMESTAMP," +
      "answer CLOB," +
      //"audioFile VARCHAR, " +
      "valid BOOLEAN" +
      ")");
    statement.execute();
    statement.close();
  }

  public static void main(String[] arg) {
    DatabaseImpl langTestDatabase = new DatabaseImpl(null);
    //long id = langTestDatabase.addUser(23, "male", 0);
    //System.out.println("id =" + id);
     for (Exercise e : langTestDatabase.getExercises(0)) System.err.println("e " + e);
  }
}
