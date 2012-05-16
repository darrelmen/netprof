package mitll.langtest.server.database;

import com.google.gwt.core.client.GWT;
import mitll.langtest.shared.Exercise;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import javax.servlet.http.HttpServlet;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: go22670
 * Date: 5/14/12
 * Time: 11:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class DatabaseImpl {
  private static final String ENCODING = "UTF8";
  private static final boolean DROP_CREATED_TABLES = false;
  private static final String H2_DB_NAME = "vlr-parle";//"new";
  private Map<Long, List<Schedule>> userToSchedule;

  // mysql config info
/*  private String url = "jdbc:mysql://localhost:3306/",
    dbOptions = "?characterEncoding=utf8&zeroDateTimeBehavior=convertToNull",
    driver = "com.mysql.jdbc.Driver";*/

  // h2 config info
  private String url = "jdbc:h2:" + H2_DB_NAME + ";IFEXISTS=TRUE",
    dbOptions = "",//"?characterEncoding=utf8&zeroDateTimeBehavior=convertToNull",
    driver = "org.h2.Driver";

  private HttpServlet servlet;

  public DatabaseImpl(HttpServlet s) {
    this.servlet = s;
    this.userToSchedule = getSchedule();
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

    try {
      connection = getConnection();
      PreparedStatement statement = connection.prepareStatement("SELECT * FROM exercises");

      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        String plan = rs.getString(1);
      //  String exid = rs.getString(2);
     //   String exType = rs.getString(3);
        Clob clob = rs.getClob(4);

        String s = getStringFromClob(clob);
        //  System.out.println("b " +b.toString());

        if (s.startsWith("{")) {
          net.sf.json.JSONObject obj = net.sf.json.JSONObject.fromObject(s);
          Exercise e = getExercise(plan, obj);
          exercises.add(e);
        }
      }
      rs.close();
      statement.close();
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
  private Exercise getExercise(String plan, JSONObject obj) {
    boolean promptInEnglish = false;
    boolean recordAudio = false;
    Exercise exercise = new Exercise(plan, (String) obj.get("exid"), (String) obj.get("content"),
      promptInEnglish, recordAudio);
    Collection<JSONObject> qa = JSONArray.toCollection((JSONArray) obj.get("qa"), JSONObject.class);
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
      System.out.println("connecting to " + url);

      GWT.log("connecting to " + url);
      File f = new java.io.File(H2_DB_NAME +
        ".h2.db");
      if (!f.exists()) {
        String s = "huh? no file at " + f.getAbsolutePath();
        System.err.println(s);

        GWT.log(s);
      }
      Connection connection = DriverManager.getConnection(url
        + /*getInitParameter(PRETEST_DATABASE) +*/ dbOptions/*,
        "",
        ""*/);
      connection.setAutoCommit(false);
      boolean closed = connection.isClosed();
      System.out.println("closed " + closed);
      return connection;
    } catch (Exception ex) {
      ex.printStackTrace();
      throw ex;
    }
  }

  private Connection getConnection() throws Exception {
    try {
      return (Connection) servlet.getServletContext().getAttribute("connection");
    } catch (Exception e) {  // for standalone testing
      return this.dbLogin();
    }
  }

  public long addUser(int age, String gender, int experience) {
    try {
      Connection connection = getConnection();
      PreparedStatement statement;
      if (true) {
        statement = connection.prepareStatement("drop TABLE users");
        statement.execute();
        statement.close();
      }

      statement = connection.prepareStatement("CREATE TABLE if not exists users (id IDENTITY, " +
        "age INT, gender INT, experience INT, password VARCHAR, CONSTRAINT pkusers PRIMARY KEY (id))");
      statement.execute();
      statement.close();

      System.out.println("adding " + age + " and " + gender + " and " + experience);
      statement = connection.prepareStatement("INSERT INTO users(age,gender,experience) VALUES(?,?,?);");
      int i = 1;
      statement.setInt(i++, age);
      statement.setInt(i++, gender.equalsIgnoreCase("male") ? 0 : 1);
      statement.setInt(i++, experience);
      statement.executeUpdate();

      ResultSet rs = statement.getGeneratedKeys(); // will return the ID in ID_COLUMN

      long id = 0;
      while (rs.next()) {
        id = rs.getLong(1);
        System.out.println("addUser got user #" + id);
        //  System.out.println(rs.getString(1) + "," + rs.getString(2) + "," + rs.getInt(3) + "," + rs.getString(4) + "," + rs.getTimestamp(5));
      }
      rs.close();
      statement.close();
      return id;
    } catch (Exception ee) {
      ee.printStackTrace();
    }
    return 0;
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
    String ip = "";//servlet.getThreadLocalRequest().getRemoteAddr();
    //  System.out.println("Got " +e + " and " + questionID + " and " + answer + " at " +ip);

    if (DROP_CREATED_TABLES) {
      dropResults();
    }

    String plan = e.getPlan();
    String id = e.getID();
    addAnswer(userID, plan, id, questionID, answer, audioFile);
  }

  public void addAnswer(int userID, String plan, String id, int questionID, String answer, String audioFile) {
    try {
      Connection connection = getConnection();
      createResultTable(connection);

      addAnswerToTable(userID, plan, id, questionID, answer, audioFile, connection);

      if (true) { // true to see what is in the table
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

  private void dropResults() {
    try {
      Connection connection = getConnection();
      PreparedStatement statement = connection.prepareStatement("DROP TABLE if exists results");
      if (!statement.execute()) {
        System.err.println("couldn't create table?");
      }
      statement.close();

    } catch (Exception e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.  }
    }
  }

  public void addUser() {
    String sql = "CREATE TABLE if not exists users (id INT AUTO_INCREMENT, " +
      "age INT, gender INT, experience INT, password VARCHAR, CONSTRAINT pkusers PRIMARY KEY (id))";
  }

  /**
   * @param userid
   * @param plan
   * @param id
   * @param questionID
   * @param answer
   * @param connection
   * @throws SQLException
   * @see #addAnswer
   */
  private void addAnswerToTable(int userid, String plan, String id, int questionID, String answer, String audioFile,
                                Connection connection) throws SQLException {
    if (DROP_CREATED_TABLES) {
      dropResults();
    }
    createResultTable(connection);

    PreparedStatement statement;
    statement = connection.prepareStatement("INSERT INTO results(userid,plan,id,qid,answer,audioFile) VALUES(?,?,?,?,?,?)");
    int i = 1;
    statement.setInt(i++, userid);
    statement.setString(i++, plan);
    statement.setString(i++, id);
    statement.setInt(i++, questionID);
    statement.setString(i++, answer);
    statement.setString(i++, audioFile);
    statement.executeUpdate();
    statement.close();
  }

  private void showResults() throws Exception {
    PreparedStatement statement = getConnection().prepareStatement("SELECT * FROM results order by timestamp");
    ResultSet rs = statement.executeQuery();
    while (rs.next()) {
      int i = 1;
      System.out.println(rs.getInt(i++) + "," + rs.getString(i++) + "," +
        rs.getString(i++) + "," +
        rs.getInt(i++) + "," +
        rs.getString(i++) + "," +
        rs.getString(i++) + "," +
        rs.getTimestamp(i++));
    }
    rs.close();
    statement.close();
  }

  private void createResultTable(Connection connection) throws SQLException {
    PreparedStatement statement = connection.prepareStatement("CREATE TABLE if not exists " +
      "results (userid INT, plan VARCHAR, id VARCHAR, qid INT, answer VARCHAR, audioFile VARCHAR, timestamp TIMESTAMP AS CURRENT_TIMESTAMP)");
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
