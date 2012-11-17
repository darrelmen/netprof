package mitll.langtest.server.database;

import com.google.gwt.core.client.GWT;
import mitll.langtest.shared.CountAndGradeID;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Grade;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.ResultsAndGrades;
import mitll.langtest.shared.User;
import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Note with H2 that :  <br></br>
 * * you can corrupt the database if you try to copy a file that's in use by another process. <br></br>
 * * one process can lock the database and make it inaccessible to a second one, seemingly this can happen
 * more easily when H2 lives inside a servlet container (e.g. tomcat). <br></br>
 * * it's not a good idea to close connections, especially in the context of a servlet inside a container, since
 * H2 will return "new" connections that have already been closed.   <br></br>
 * * it's not a good idea to reuse one connection...?  <br></br>
 * <p/>
 * User: go22670
 * Date: 5/14/12
 * Time: 11:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class DatabaseImpl implements Database {
  private static Logger logger = Logger.getLogger(DatabaseImpl.class);
  private static final boolean TESTING = false;
  private static final boolean USE_FAST_SLOW_LEVANTINE = true;

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
  private String h2DbName = H2_DB_NAME;

  public DatabaseImpl(String dburl) {
    this.h2DbName = dburl;
    this.url = "jdbc:h2:" + h2DbName + ";IFEXISTS=TRUE;QUERY_CACHE_SIZE=0;";
    try {
      getConnection();
      resultDAO.createResultTable(getConnection());
    } catch (Exception e) {
      logger.error("got " + e, e);  //To change body of catch statement use File | Settings | File Templates.
    }
  }

  /**
   * @param s
   * @see mitll.langtest.server.LangTestDatabaseImpl#init
   */
  public DatabaseImpl(HttpServlet s) {
    this.servlet = s;
    try {
      boolean open = getConnection() != null;
      if (!open) {
        logger.warn("couldn't open connection to database");
        return;
      }
    } catch (Exception e) {
      logger.warn("couldn't open connection to database, got " + e.getMessage());
      logger.error("got " + e, e);
      return;
    }
    ScheduleDAO scheduleDAO = new ScheduleDAO(this);
    this.userToSchedule = scheduleDAO.getSchedule();

    if (DROP_USER) {
      try {
        userDAO.dropUserTable(this);
        userDAO.createUserTable(this);
      } catch (Exception e) {
        logger.error("got " + e, e);
      }
    }
    if (DROP_RESULT) {
      logger.info("------------ dropping results table");
      resultDAO.dropResults(this);
  /*    try {
        resultDAO.createResultTable(getConnection());
      } catch (Exception e) {
        logger.error("got " + e, e);
      }*/
    }
    //else {
      try {
        resultDAO.createResultTable(getConnection());
      } catch (Exception e) {
        logger.error("got " + e, e);  //To change body of catch statement use File | Settings | File Templates.
      }
 //   }

    try {
      // gradeDAO.dropGrades();
      gradeDAO.createGradesTable(getConnection());
      graderDAO.createGraderTable(getConnection());
    } catch (Exception e) {
      logger.error("got " + e, e);  //To change body of catch statement use File | Settings | File Templates.
    }

  }

  private ExerciseDAO makeExerciseDAO(boolean useFile) {
    return useFile ? new FileExerciseDAO() : new SQLExerciseDAO(this);
  }

  public void setInstallPath(String i) {
    logger.debug("got install path " + i);
    this.installPath = i;
  }


  public List<ExerciseExport> getExport(boolean useFLQ,boolean useSpoken) {
    List<ExerciseExport> names = new ArrayList<ExerciseExport>();
//    int n = 20;
    for (Exercise e : getExercises()) {
      System.out.println("on " + e);
      List<ExerciseExport> resultsForExercise = getExports(e, useFLQ, useSpoken);
      names.addAll(resultsForExercise);
     // if (n-- == 0) break;
    }
    return names;
  }

  public static class ExerciseExport {
    public String id;
    public List<String> key = new ArrayList<String>();
    public List<ResponseAndGrade> rgs = new ArrayList<ResponseAndGrade>();
    public ExerciseExport(String id, String key) {
      this.id  = id;
      this.key = Arrays.asList(key.split("\\|\\|"));
   //   this.key = key;
    }
    public void addRG(String response, int grade) { rgs.add(new ResponseAndGrade(response,grade)); }

    public String toString() {
      return "id " + id + " " + key.size() + " keys : " + new HashSet<String>(key) +
          " " + rgs.size() + " responses " + new HashSet<ResponseAndGrade>(rgs);
    }
  }

  public static class ResponseAndGrade {
    public String response;
    public float grade;
    public ResponseAndGrade(String response, int grade) {
      this.response = response;
      this.grade = ((float) grade)/5;
    }
    @Override
    public String toString() { return "" + grade + " for '" + response +"'"; }
  }

  private List<Exercise> getExercises() {
    return getExercises(false);
  }
    /**
    * @param useFile
    * @return
    * @see mitll.langtest.server.LangTestDatabaseImpl#getExercises
    */
  public List<Exercise> getExercises(boolean useFile) {
    logger.info("use file = " + useFile);

    if (exerciseDAO == null || useFile && exerciseDAO instanceof SQLExerciseDAO || !useFile && exerciseDAO instanceof FileExerciseDAO) {
      this.exerciseDAO = makeExerciseDAO(useFile);
    }

    if (useFile) {
      if (USE_FAST_SLOW_LEVANTINE) {
        ((FileExerciseDAO) exerciseDAO).readFastAndSlowExercises(installPath);
      } else {
        ((FileExerciseDAO) exerciseDAO).readExercises(installPath);
      }
    }
    return exerciseDAO.getRawExercises();
  }

  /**
   * @param activeExercises
   * @param expectedCount
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getNextUngradedExercise(String, int)
   */
  public Exercise getNextUngradedExercise(Collection<String> activeExercises, int expectedCount) {
    if (expectedCount == 1) {
      return getNextUngradedExerciseQuick(activeExercises, expectedCount);
    } else {
      return getNextUngradedExerciseSlow(activeExercises, expectedCount);
    }
  }

  /**
   * Walks through each exercise, checking if any have ungraded results.
   * <p/>
   * This gets slower as more exercises are graded.  Better to a "join" that determines after
   * two queries what the next ungraded one is.
   * Runs through each exercise in sequence -- slows down as more are completed!
   * TODO : avoid using this.
   *
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getNextUngradedExercise
   */
  private Exercise getNextUngradedExerciseSlow(Collection<String> activeExercises, int expectedCount) {
    List<Exercise> rawExercises = getExercises(false);
    logger.info("getNextUngradedExercise : checking " + rawExercises.size() + " exercises.");
    for (Exercise e : rawExercises) {
      if (!activeExercises.contains(e.getID()) && // no one is working on it
          resultDAO.areAnyResultsLeftToGradeFor(e, expectedCount)) {
        //logger.info("Exercise " +e + " needs grading.");

        return e;
      }
    }
    return null;
  }

  /**
   * Does a join of grades against results - avoid iterated solution above.
   *
   * @param activeExercises
   * @param expectedCount
   * @return next exercise containing ungraded results
   */
  private Exercise getNextUngradedExerciseQuick(Collection<String> activeExercises, int expectedCount) {
    List<Exercise> rawExercises = getExercises(false);
    Collection<Result> resultExcludingExercises = resultDAO.getResultExcludingExercises(activeExercises);

    GradeDAO.GradesAndIDs allGradesExcluding = gradeDAO.getAllGradesExcluding(activeExercises);
    Map<Integer, Integer> idToCount = new HashMap<Integer, Integer>();
    for (Grade g : allGradesExcluding.grades) {
      if (!idToCount.containsKey(g.resultID)) idToCount.put(g.resultID, 1);
      else idToCount.put(g.resultID, 2);
    }
    logger.info("getNextUngradedExercise found  " + resultExcludingExercises.size() + " results, " +
        "expected " + expectedCount + "," + allGradesExcluding.resultIDs.size() + " graded results");

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
          logger.info("getNextUngradedExercise  " + e);

          return e;
        }
      }
      if (!rawExercises.isEmpty()) {
        logger.warn("getNextUngradedExercise2 expecting an exercise to match " + first);
      }
    }

    return null;
  }

  /**
   * @param userID
   * @param useFile
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExercises
   */
  public List<Exercise> getExercises(long userID, boolean useFile) {
    logger.info("getExercises : for user  " + userID);

    List<Schedule> forUser = userToSchedule.get(userID);
    if (forUser == null) {
      logger.warn("no schedule for user " + userID);
      return getExercises(useFile);
    }
    List<Exercise> exercises = new ArrayList<Exercise>();

    List<Exercise> rawExercises = getExercises(useFile);
    Map<String, Exercise> idToExercise = new HashMap<String, Exercise>();
    for (Exercise e : rawExercises) {
      idToExercise.put(e.getID(), e);
    }
    for (Schedule s : forUser) {
      Exercise exercise = idToExercise.get(s.exid);
      if (exercise == null) {
        logger.warn("no exercise for id " + s.exid + "? Foreign key constraint violated???");
        continue;
      }
      exercise.setPromptInEnglish(!s.flQ);
      exercise.setRecordAnswer(s.spoken);
      exercises.add(exercise);
    }
    return exercises;
  }

  private Random random = new Random();
  public List<Exercise> getRandomBalancedList() {
    List<Exercise> exercisesGradeBalancing = getExercisesGradeBalancing(false);
    List<Exercise> randomList = new ArrayList<Exercise>();
   // int focusGroupSize = exercisesGradeBalancing.size()/8;
  //  Set<Integer> chosen = new HashSet<Integer>();
    int orig = exercisesGradeBalancing.size();
    while (randomList.size() < orig) {
      double v = random.nextGaussian();
      double shift = Math.abs(v);
      int remaining = exercisesGradeBalancing.size();
      double inRemaining = (remaining / 8) * Math.abs(shift);
      double scale = Math.min(remaining -1, inRemaining);
      int index = (int) scale;
     // if (!chosen.contains(index)) {
        Exercise remove = exercisesGradeBalancing.remove(index);
        randomList.add(remove);
       // System.out.println("v " + v + " s " + shift + " scale " + index + "/"+ inRemaining+ " of " + remaining +" ex " + remove);
    }
    logger.info("Returning " + randomList.size() + " orig " + orig);
    return randomList;
    // random.nextInt(exercisesGradeBalancing.size());
  }

  private List<Exercise> getExercisesGradeBalancing(boolean useFile) {
    return getExercisesGradeBalancing(useFile, true, false);
  }

    /**
     * Remember there can be multiple questions per exercise, so we need to average over the grades for all
     * answers for all questions for an exercise.
     *
     * @paramx userID
     * @param useFile
     * @paramx doGradeBalancing
     * @return
     */
  private List<Exercise> getExercisesGradeBalancing(boolean useFile, boolean useFLQ, boolean useSpoken) {
    List<Exercise> rawExercises = getExercises(useFile);
    Map<String, Exercise> idToExercise = new HashMap<String, Exercise>();
    for (Exercise e : rawExercises) {
      idToExercise.put(e.getID(), e);
    }

    // join results with grades
    List<Result> results = getResults();
    Collection<Grade> grades = gradeDAO.getGrades();

    Map<Integer, List<Grade>> idToGrade = getIdToGrade(grades);

    Map<String,ResultAndGrade> exidToRG = new HashMap<String, ResultAndGrade>();
    for (Result r : results) {
      if (r.flq == useFLQ && r.spoken == useSpoken) {
        List<Grade> grades1 = idToGrade.get(r.uniqueID);
        if (grades1 != null) {
          ResultAndGrade resultAndGrade = exidToRG.get(r.id);
          if (resultAndGrade == null) {
            exidToRG.put(r.id, new ResultAndGrade(r, grades1));
          } else {
            resultAndGrade.addGrades(grades1);
          }
        }
      }
    }
    List<ResultAndGrade> rgs = new ArrayList<ResultAndGrade>(exidToRG.values());
    Collections.sort(rgs);
    logger.info("worst is " + rgs.get(0) + "\nbest is  " + rgs.get(rgs.size() - 1));
    for (ResultAndGrade rg : rgs)  System.out.println("rg " + rg);
    List<Exercise> ret = new ArrayList<Exercise>();
    int total = 0;
    for (ResultAndGrade rg : rgs) {
      total += rg.totalValidGrades();
      ret.add(idToExercise.get(rg.result.id));
    }
    logger.info("total valid grades " + total);
    return ret;
  }

  private static class ResultAndGrade implements Comparable<ResultAndGrade> {
    private Result result;
    private List<Grade> grades = new ArrayList<Grade>();

    public ResultAndGrade(Result result, List<Grade> grades) {
      this.result = result;
      this.grades = grades;
    }

    public void addGrade(Grade g) {
      grades.add(g);
    }

    public void addGrades(List<Grade> grades) {
      this.grades.addAll(grades);
    }

    public float getRatio() {
      int right = 0;
      int wrong = 0;
      for (Grade g : grades) {
        if (g.grade > 3) right++; else if (g.grade > 0 && g.grade < 3) wrong++;
      }
      if (right == 0 && wrong == 0) return 0.5f; // items with no valid grades sort to the middle

      float ratio = (float) right / (float) (right + wrong);
    //  if (grades.size() > 100)
   //   System.out.println("num = " + grades.size() +" : right " + right + " wrong " + wrong + " ratio " + ratio);
      return ratio;
    }

    public int totalValidGrades() {
      int total = 0;
      for (Grade g : grades) {
         if (g.grade > 0) total++;
      }
      return total;
    }

    private int getNumRight() {
      int right = 0;
      for (Grade g : grades) {
        if (g.grade > 3) right++;
      }
      return right;
    }
    private int getNumWrong() {
      int wrong = 0;
      for (Grade g : grades) {
        if (g.grade > 0 && g.grade < 3) wrong++;
      }
      return wrong;
    }

    @Override
    public int compareTo(ResultAndGrade o) {
      float ratio = getRatio();
      float oratio = o.getRatio();
      int numRight = getNumRight();
      int numRight1 = o.getNumRight();

      int numWrong = getNumWrong();
      int numWrong1 = o.getNumWrong();
      return ratio < oratio ? +1 : ratio > oratio ? -1 : numRight > numRight1 ? -1 : numRight < numRight1 ? +1 : numWrong > numWrong1 ? -1 : numWrong < numWrong1 ? +1 :0;
    }

    @Override
    public String toString() {
      return "'" + result + "'\tand grades (" + grades.size() + ")" + " ratio " + getRatio() +
          new HashSet<Grade>(grades);
    }
  }

    /**
     * Just for dbLogin
     */
  private Connection localConnection;
  /**
   * Not necessary if we use the h2 DBStarter service -- see web.xml reference
   *
   * @return
   * @throws Exception
   */
  private Connection dbLogin() throws Exception {
    if (localConnection != null) return localConnection;
    try {
      Class.forName(driver).newInstance();
      try {
        url = servlet.getServletContext().getInitParameter("db.url"); // from web.xml
      } catch (Exception e) {
        logger.warn("no servlet context?");
      }
      logger.info("connecting to " + url);

      GWT.log("connecting to " + url);
      File f = new java.io.File(h2DbName + ".h2.db");
      if (!f.exists()) {
        String s = "huh? no file at " + f.getAbsolutePath();
        logger.warn(s);

        GWT.log(s);
      }
      Connection connection = DriverManager.getConnection(url + dbOptions);
      connection.setAutoCommit(false);
      boolean closed = connection.isClosed();
      if (closed) {
        logger.warn("connection is closed to : " + url);
      }
      this.localConnection = connection;
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
    //  logger.info("install path is " +servlet.getInitParameter());
    try {
      if (servlet == null) {
        c = this.dbLogin();
      } else {
        ServletContext servletContext = servlet.getServletContext();
        c = (Connection) servletContext.getAttribute("connection");
      }
    } catch (Exception e) {  // for standalone testing
      logger.warn("The context DBStarter is not working : " + e.getMessage(), e);
      c = this.dbLogin();
    }
    if (c == null) {
      return c;
    }
    c.setAutoCommit(true);
    if (c.isClosed()) {
      logger.warn("getConnection : conn " + c + " is closed!");
    }
    return c;
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
   * @return
   */
  public long addUser(int age, String gender, int experience, String ipAddr) {
    return userDAO.addUser(age, gender, experience, ipAddr);
  }

  public List<User> getUsers() {
    return userDAO.getUsers();
  }


  /**
   * Pulls the list of results out of the database.
   *
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getResults()
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
   *
   * @param exid
   * @return ResultsAndGrades
   * @see mitll.langtest.server.LangTestDatabaseImpl#getResultsForExercise(String)
   * @see mitll.langtest.client.grading.GradingExercisePanel#getAnswerWidget(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int)
   */
  public ResultsAndGrades getResultsForExercise(String exid) {
    GradeDAO.GradesAndIDs gradesAndIDs = gradeDAO.getResultIDsForExercise(exid);
    List<Result> resultsForExercise = resultDAO.getAllResultsForExercise(exid);
    //Set<Long> users = resultDAO.getUsers(resultsForExercise);

   // Map<Long, List<Schedule>> scheduleForUserAndExercise = scheduleDAO.getScheduleForUserAndExercise(users, exid);
    Map<Boolean, Map<Boolean, List<Result>>> spokenToLangToResult = new HashMap<Boolean, Map<Boolean, List<Result>>>();
    for (Result r : resultsForExercise) {
     /* List<Schedule> schedules = scheduleForUserAndExercise.get(r.userid);
      if (schedules == null) {
        //System.err.println("huh? couldn't find schedule for user " +r.userid +"?");
      } else {*/
        //Schedule schedule = schedules.get(0);
        //r.setFLQ(schedule.flQ);
        //r.setSpoken(schedule.spoken);
        boolean spoken = r.spoken;
        boolean flq = r.flq;

        Map<Boolean, List<Result>> langToResult = spokenToLangToResult.get(spoken);
        if (langToResult == null)
          spokenToLangToResult.put(spoken, langToResult = new HashMap<Boolean, List<Result>>());
        List<Result> resultsForLang = langToResult.get(flq);
        if (resultsForLang == null) langToResult.put(flq, resultsForLang = new ArrayList<Result>());
        resultsForLang.add(r);
    //  }
    }
    return new ResultsAndGrades(resultsForExercise, gradesAndIDs.grades, spokenToLangToResult);
  }

  /**
   * Complicated.  To figure out spoken/written, flq/english we have to go back and join against the schedule.
   * Unless the result column is set.
   * @param exercise
   * @param useFLQ
   * @param useSpoken
   * @return
   */
  public List<ExerciseExport> getExports(Exercise exercise, boolean useFLQ, boolean useSpoken) {
    boolean debug = false;
    String exid = exercise.getID();
    GradeDAO.GradesAndIDs gradesAndIDs = gradeDAO.getResultIDsForExercise(exid);
    List<Result> resultsForExercise = resultDAO.getAllResultsForExercise(exid);
    Set<Long> users = resultDAO.getUsers(resultsForExercise);

    Map<Long, List<Schedule>> scheduleForUserAndExercise = scheduleDAO.getScheduleForUserAndExercise(users, exid);

    List<ExerciseExport> ret = new ArrayList<ExerciseExport>();

    Map<Integer,ExerciseExport> qidToExport = new HashMap<Integer, ExerciseExport>();
  //  for (Exercise e : getExercises()) {
      int qid = 0;
      for (Exercise.QAPair q : exercise.getQuestions()) {
        ExerciseExport e1 = new ExerciseExport(exercise.getID()+"_"+ ++qid, q.getAnswer());
      //  ret.add(e1);
        qidToExport.put(qid,e1);
      }
    Set<ExerciseExport> valid = new HashSet<ExerciseExport>();
   // }

    Map<Integer, List<Grade>> idToGrade = getIdToGrade(gradesAndIDs.grades);

    // find results, after join with schedule, add join with the grade
    for (Result r : resultsForExercise) {
      List<Schedule> schedules = scheduleForUserAndExercise.get(r.userid);
      if (schedules == null) {
        //System.err.println("huh? couldn't find schedule for user " +r.userid +"?");
      } else {
      //  System.out.println("for " + r + " there were " +schedules.size() + " schedules");
        if (schedules.size() > 1) System.err.println("ERROR for " + r + " there were " +schedules.size() + " schedules");

        Schedule schedule = schedules.get(0);

        Collection<Grade> grades = gradesAndIDs.grades;
        if (debug) System.out.println("for " + r + " there were " +schedules.size() + " and " +grades.size() + " grades");
        if (schedule.flQ == useFLQ && schedule.spoken == useSpoken) {
          ExerciseExport exerciseExport = qidToExport.get(r.qid);
          //for (Grade g : grades) {
          List<Grade> gradesForResult = idToGrade.get(r.uniqueID);
          for (Grade g : gradesForResult) {
            exerciseExport.addRG(r.answer, g.grade);
            if (!valid.contains(exerciseExport)) {
              ret.add(exerciseExport);
              valid.add(exerciseExport);
            }
          } /*else {
            if (debug) System.out.println("\tSkipping grade " + grade + " since not match to " + r.uniqueID);
          }*/
          //}
        }
        else {
          if (debug) System.out.println("\tSkipping schedule " + schedule + " since not match to " +useFLQ + " and " + useSpoken);
        }
      }
    }
    return ret;
  }

  private Map<Integer, List<Grade>> getIdToGrade(Collection<Grade> grades) {
    Map<Integer,List<Grade>> idToGrade = new HashMap<Integer, List<Grade>>();
    for (Grade g : grades) {
      List<Grade> gradesForResult = idToGrade.get(g.resultID);
      if (gradesForResult == null) {
        idToGrade.put(g.resultID, gradesForResult = new ArrayList<Grade>());
      }
      gradesForResult.add(g);
     /* if (gradesForResult.size() > 1)
        System.out.println("r  " +g.resultID+ " grades " + gradesForResult.size());*/

    }
    System.out.println("r->g " +idToGrade.size() + " keys " + idToGrade.keySet());
    return idToGrade;
  }

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
   * @param exerciseID
   * @param toAdd
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#addGrade
   */
  public CountAndGradeID addGrade(String exerciseID, Grade toAdd) {
    return gradeDAO.addGradeEasy(exerciseID, toAdd);
  }

  /**
   * @param toChange
   * @see mitll.langtest.server.LangTestDatabaseImpl#changeGrade(mitll.langtest.shared.Grade)
   */
  public void changeGrade(Grade toChange) {
    gradeDAO.changeGrade(toChange);
  }

  public void addGrader(String login) {
    graderDAO.addGrader(login);
  }

  public boolean graderExists(String login) {
    return graderDAO.graderExists(login);
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
      logger.error("got " + e, e);
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

  public static void main(String[] arg) {
    DatabaseImpl langTestDatabase = new DatabaseImpl("C:\\Users\\go22670\\mt_repo\\jdewitt\\pilot\\vlr-parle");
    //List<Exercise> exercises = langTestDatabase.getExercises();
    if (false) {
      List<ExerciseExport> exerciseNames = langTestDatabase.getExport(true, false);

      System.out.println("names " + exerciseNames.size() + " e.g. " + exerciseNames.get(0));
      for (ExerciseExport ee : exerciseNames) {
        System.out.println("ee " + ee);
      }
    } else {
      List<Exercise> exercises = langTestDatabase.getRandomBalancedList();

    }
  }
}
