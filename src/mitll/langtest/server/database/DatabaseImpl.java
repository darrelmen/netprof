package mitll.langtest.server.database;

import com.google.gwt.core.client.GWT;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.User;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
public class DatabaseImpl implements Database {
  private static final boolean TESTING = false;

  private static final boolean DROP_USER = false;
  private static final boolean DROP_RESULT = false;
  private static final String H2_DB_NAME = TESTING ? "vlr-parle" : "/services/apache-tomcat-7.0.27/webapps/langTest/vlr-parle";
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
  private final ExerciseDAO exerciseDAO = new ExerciseDAO(this);
  public final UserDAO userDAO = new UserDAO(this);
  private final ResultDAO resultDAO = new ResultDAO(this);
  public final AnswerDAO answerDAO = new AnswerDAO(this);

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
    ScheduleDAO scheduleDAO = new ScheduleDAO(this);
    this.userToSchedule = scheduleDAO.getSchedule();

    if (DROP_USER) {
      try {
        userDAO.dropUserTable(this);
        userDAO.createUserTable(this);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    if (DROP_RESULT) {
      System.out.println("------------ dropping results table");
      resultDAO.dropResults(this);
      try {
        resultDAO.createResultTable(getConnection());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }


  public List<Exercise> getExercises(long userID) {
    List<Schedule> forUser = userToSchedule.get(userID);
    if (forUser == null) {
      System.err.println("no schedule for user " +userID);
      return exerciseDAO.getRawExercises();
    }
    List<Exercise> exercises = new ArrayList<Exercise>();

    List<Exercise> rawExercises = exerciseDAO.getRawExercises();
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

  public Connection getConnection() throws Exception {
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
    return userDAO.addUser(age, gender, experience, ipAddr);
  }
  public List<User> getUsers() { return userDAO.getUsers(); }


  /**
   * Pulls the list of results out of the database.
   * @return
   */
  public List<Result> getResults() {
    return resultDAO.getResults();
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
    answerDAO.addAnswer(userID, e, questionID, answer, audioFile);
  }

  public boolean isAnswerValid(int userID, Exercise exercise, int questionID, Database database) {
    return answerDAO.isAnswerValid(userID, exercise, questionID, database);
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

  public void closeConnection(Connection connection) throws SQLException {
  //  System.err.println("Closing " + connection);
    //connection.close();
   // System.err.println("Closing " + connection + " " + connection.isClosed());
  }

  public static void main(String[] arg) {
    DatabaseImpl langTestDatabase = new DatabaseImpl(null);
    //long id = langTestDatabase.addUser(23, "male", 0);
    //System.out.println("id =" + id);
     for (Exercise e : langTestDatabase.getExercises(0)) System.err.println("e " + e);
  }
}
