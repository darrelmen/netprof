package mitll.langtest.server.database;

import com.google.gwt.core.client.GWT;
import mitll.langtest.shared.*;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

/**
 * Note with H2 that :  <br></br>
 *  * you can corrupt the database if you try to copy a file that's in use by another process. <br></br>
 *  * one process can lock the database and make it inaccessible to a second one, seemingly this can happen
 *    more easily when H2 lives inside a servlet container (e.g. tomcat). <br></br>
 *  * it's not a good idea to close connections, especially in the context of a servlet inside a container, since
 *    H2 will return "new" connections that have already been closed.   <br></br>
 *  * it's not a good idea to reuse one connection...?  <br></br>
 *
 * User: go22670
 * Date: 5/14/12
 * Time: 11:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class DatabaseImpl implements Database {
  private static final boolean TESTING = false;
  private static final boolean USE_LEVANTINE = true;

  private static final boolean DROP_USER = false;
  private static final boolean DROP_RESULT = false;
  private static final String H2_DB_NAME = TESTING ? "vlr-parle" : "/services/apache-tomcat-7.0.27/webapps/langTest/vlr-parle";
  //private static final String TIME = "time";
  //private static final String EXID = "exid";
  private Map<Long, List<Schedule>> userToSchedule;

  // h2 config info
  private String url = "jdbc:h2:" + H2_DB_NAME + ";IFEXISTS=TRUE;QUERY_CACHE_SIZE=0;",
    dbOptions = "",//"?characterEncoding=utf8&zeroDateTimeBehavior=convertToNull",
    driver = "org.h2.Driver";

  private HttpServlet servlet;
  private String installPath;
  private ExerciseDAO exerciseDAO = null;
  public final UserDAO userDAO = new UserDAO(this);
  private final ResultDAO resultDAO = new ResultDAO(this);
  public final AnswerDAO answerDAO = new AnswerDAO(this);
  public final GradeDAO gradeDAO = new GradeDAO(this);
  public final GraderDAO graderDAO = new GraderDAO(this);
  private final ScheduleDAO scheduleDAO = new ScheduleDAO(this);

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#init
   * @param s
   */
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
    this.exerciseDAO = makeExerciseDAO();

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

    try {
     // gradeDAO.dropGrades();
      gradeDAO.createGradesTable(getConnection());
      graderDAO.createGraderTable(getConnection());
    } catch (Exception e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }

  }

  private ExerciseDAO makeExerciseDAO() {
    return USE_LEVANTINE ? new FileExerciseDAO() : new SQLExerciseDAO(this);
  }

  public void setInstallPath(String i) { this.installPath = i; }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExercises()
   * @return
   */
  public List<Exercise> getExercises() {
    if (USE_LEVANTINE) {
      ((FileExerciseDAO)exerciseDAO).readExercises(installPath);
    }
    return exerciseDAO.getRawExercises();
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getNextUngradedExercise(String, int)
   * @param activeExercises
   * @param expectedCount
   * @return
   */
  public Exercise getNextUngradedExercise(Collection<String> activeExercises, int expectedCount) {
    if (expectedCount == 1) {
      return getNextUngradedExerciseQuick(activeExercises,expectedCount);
    }
    else {
      return getNextUngradedExerciseSlow(activeExercises,expectedCount);
    }
  }

  /**
   * Walks through each exercise, checking if any have ungraded results.
   *
   * This gets slower as more exercises are graded.  Better to a "join" that determines after
   * two queries what the next ungraded one is.
   * Runs through each exercise in sequence -- slows down as more are completed!
   * TODO : avoid using this.
   * @see mitll.langtest.server.LangTestDatabaseImpl#getNextUngradedExercise
   * @return
   */
  private Exercise getNextUngradedExerciseSlow(Collection<String> activeExercises, int expectedCount) {
    List<Exercise> rawExercises = getExercises();
    System.out.println("getNextUngradedExercise : checking " +rawExercises.size() + " exercises.");
    for (Exercise e : rawExercises) {
      if (!activeExercises.contains(e.getID()) && // no one is working on it
          resultDAO.areAnyResultsLeftToGradeFor(e, expectedCount)) {
        //System.out.println("Exercise " +e + " needs grading.");

        return e;
      }
    }
    return null;
  }

  /**
   * Does a join of grades against results - avoid iterated solution above.
   * @param activeExercises
   * @param expectedCount
   * @return next exercise containing ungraded results
   */
  private Exercise getNextUngradedExerciseQuick(Collection<String> activeExercises, int expectedCount) {
    List<Exercise> rawExercises = getExercises();
    Collection<Result> resultExcludingExercises = resultDAO.getResultExcludingExercises(activeExercises);

    GradeDAO.GradesAndIDs allGradesExcluding = gradeDAO.getAllGradesExcluding(activeExercises);
               Map<Integer,Integer> idToCount = new HashMap<Integer, Integer>();
    for (Grade g : allGradesExcluding.grades) {
      if (!idToCount.containsKey(g.resultID)) idToCount.put(g.resultID,1);
      else idToCount.put(g.resultID,2);
    }
    System.out.println("getNextUngradedExercise found  " + resultExcludingExercises.size() + " results, " +
        "expected " +expectedCount +"," +allGradesExcluding.resultIDs.size() + " graded results");

    // remove results that have grades...
    Iterator<Result> iterator = resultExcludingExercises.iterator();
    while (iterator.hasNext()) {
      Result result = iterator.next();

      //if (allGradesExcluding.resultIDs.contains(result.uniqueID)) {
        Integer numGrades = idToCount.get(result.uniqueID);
        if (numGrades != null && expectedCount <= numGrades) {  // need 2 grades for english
          //if (result.flq)  // TODO : need to enrich Results with flq flag
          iterator.remove();
        }
/*        else if (result.id.equals("ac-L0P-001")) System.out.println("num " +numGrades+
            " result " + result);*/
      //}
    }

    //System.out.println("getNextUngradedExercise after removing  " + resultExcludingExercises.size() + " results");

    // whatever remains, find first exercise

    SortedSet<String> exids = new TreeSet<String>();
    for (Result r : resultExcludingExercises) exids.add(r.id);
    if (exids.isEmpty()) return null;
    else {
      //System.out.println("getNextUngradedExercise candidates are   " + exids);

      String first = exids.first();
      for (Exercise e : rawExercises) {
        if (e.getID().equals(first)) {
          System.out.println("getNextUngradedExercise  " + e);

          return e;
        }
      }
      if (!rawExercises.isEmpty()) {
        System.err.println("getNextUngradedExercise2 expecting an exercise to match " + first);
      }
    }

    return null;
  }

  public List<Exercise> getExercises(long userID) {
    System.out.println("getExercises : for user  " +userID);

    List<Schedule> forUser = userToSchedule.get(userID);
    if (forUser == null) {
      System.err.println("no schedule for user " +userID);
      return getExercises();
    }
    List<Exercise> exercises = new ArrayList<Exercise>();

    List<Exercise> rawExercises = getExercises();
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
   * @see mitll.langtest.server.LangTestDatabaseImpl#getResults()
   * @return
   */
  public List<Result> getResults() {
    return resultDAO.getResults();
  }

  /**
   * Find all the grades for this exercise.<br></br>
   * Find all the results for this exercise.
   * Get these schedules for this exercise and every user.
   * For every result, get the user and use it to find the schedule.
   * Use the data in the schedule to mark the en/fl and spoken/written bits on the Results.
   * This lets us make a map of spoken->lang->results
   * @see mitll.langtest.server.LangTestDatabaseImpl#getResultsForExercise(String)
   * @see mitll.langtest.client.grading.GradingExercisePanel#getAnswerWidget(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int)
   * @param exid
   * @return ResultsAndGrades
   */
  public ResultsAndGrades getResultsForExercise(String exid) {
    GradeDAO.GradesAndIDs gradesAndIDs = gradeDAO.getResultIDsForExercise(exid);
    List<Result> resultsForExercise = resultDAO.getAllResultsForExercise(exid);
    Set<Long> users =  resultDAO.getUsers(resultsForExercise);

    Map<Long, List<Schedule>> scheduleForUserAndExercise = scheduleDAO.getScheduleForUserAndExercise(users, exid);
    Map<Boolean,Map<Boolean,List<Result>>> spokenToLangToResult = new HashMap<Boolean, Map<Boolean, List<Result>>>();
    for (Result r : resultsForExercise) {
      List<Schedule> schedules = scheduleForUserAndExercise.get(r.userid);
      if (schedules == null) {
        //System.err.println("huh? couldn't find schedule for user " +r.userid +"?");
      }
      else {
        Schedule schedule = schedules.get(0);
        r.setFLQ(schedule.flQ);
        r.setSpoken(schedule.spoken);
        Map<Boolean, List<Result>> langToResult = spokenToLangToResult.get(schedule.spoken);
        if (langToResult == null)
          spokenToLangToResult.put(schedule.spoken, langToResult = new HashMap<Boolean, List<Result>>());
        List<Result> resultsForLang = langToResult.get(schedule.flQ);
        if (resultsForLang == null) langToResult.put(schedule.flQ, resultsForLang = new ArrayList<Result>());
        resultsForLang.add(r);
      }
    }
    return new ResultsAndGrades(resultsForExercise, gradesAndIDs.grades, spokenToLangToResult);
  }



/*  public void enrichResults(Collection<Result> results,String exid) { {
    Map<Long, List<Schedule>> scheduleForUserAndExercise = scheduleDAO.getScheduleForUserAndExercise(exid);
    for (Result r : results) {
      List<Schedule> schedules = scheduleForUserAndExercise.get(r.userid);
      Schedule schedule = schedules.get(0);
      r.setFLQ(schedule.flQ);
      r.setSpoken(schedule.spoken);
    }
  }*/

  /**
   * Creates the result table if it's not there.
   *
   * @param userID
   * @param e
   * @param questionID
   * @param answer
   * @param audioFile
   * @see mitll.langtest.client.exercise.ExercisePanel#postAnswers(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserFeedback, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.shared.Exercise)
   */
  public void addAnswer(int userID, Exercise e, int questionID, String answer, String audioFile) {
    answerDAO.addAnswer(userID, e, questionID, answer, audioFile);
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#addGrade
   * @param resultID
   * @param exerciseID
   * @param grade
   * @param gradeID
   * @param correct
   * @param grader
   * @param gradeType
   * @return
   * */
/*  public CountAndGradeID addGrade(int resultID, String exerciseID, int grade, long gradeID, boolean correct, String grader, String gradeType) {
    return gradeDAO.addGrade(resultID, exerciseID, grade, gradeID, correct, grader, gradeType);
  }*/

  /**
   *
   * @see mitll.langtest.server.LangTestDatabaseImpl#addGrade
   * @param exerciseID
   * @param toAdd
   * @return
   */
  public CountAndGradeID addGrade(String exerciseID, Grade toAdd) {
    return gradeDAO.addGradeEasy(exerciseID, toAdd);
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#changeGrade(mitll.langtest.shared.Grade)
   * @param toChange
   */
  public void changeGrade(Grade toChange) {
    gradeDAO.changeGrade(toChange);
  }

  public void addGrader(String login) { graderDAO.addGrader(login); }
  public boolean graderExists(String login) { return graderDAO.graderExists(login); }

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

/*  public static void main(String[] arg) {
    DatabaseImpl langTestDatabase = new DatabaseImpl(null,"");
    //long id = langTestDatabase.addUser(23, "male", 0);
    //System.out.println("id =" + id);
     for (Exercise e : langTestDatabase.getExercises(0)) System.err.println("e " + e);
  }*/
}
