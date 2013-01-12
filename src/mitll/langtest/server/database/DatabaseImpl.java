package mitll.langtest.server.database;

import mitll.langtest.shared.CountAndGradeID;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Grade;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.ResultsAndGrades;
import mitll.langtest.shared.Session;
import mitll.langtest.shared.User;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeMap;
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
 // private static final boolean TESTING = false;

  private static final boolean DROP_USER = false;
  private static final boolean DROP_RESULT = false;
  private Map<Long, List<Schedule>> userToSchedule;

  private String installPath;
  private ExerciseDAO exerciseDAO = null;
  public final UserDAO userDAO = new UserDAO(this);
  private final ResultDAO resultDAO = new ResultDAO(this);
  public final AnswerDAO answerDAO = new AnswerDAO(this);
  public final GradeDAO gradeDAO = new GradeDAO(this);
  public final GraderDAO graderDAO = new GraderDAO(this);
  private DatabaseConnection connection = null;

  /**
   * TODO : consider making proper v2 database!
   */
  private String lessonPlanFile;
  private String mediaDir;
  private boolean isUrdu;

  /**
   * @param configDir
   * @see mitll.langtest.server.LangTestDatabaseImpl#init
   */
  public DatabaseImpl(String configDir) {
    this(configDir, "vlr-parle");
  }

  public DatabaseImpl(String configDir, String dbName) {
    connection = new H2Connection(configDir, dbName);
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
    initializeDAOs();
  }

  private void initializeDAOs() {
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
      //userDAO.dropUserTable(this);

      userDAO.createUserTable(this);
    } catch (Exception e) {
      logger.error("got " + e, e);  //To change body of catch statement use File | Settings | File Templates.
    }
  }

  @Override
  public Connection getConnection() throws Exception {
    return connection.getConnection();
  }

  public Export getExport() {
    return new Export(exerciseDAO,resultDAO,gradeDAO);
  }

  /**
   * @see #getExercises(boolean, String)
   * @param useFile
   * @return
   */
  private ExerciseDAO makeExerciseDAO(boolean useFile) {
    //System.out.println("isurdu " + isUrdu);
    return useFile ? new FileExerciseDAO(mediaDir, isUrdu) : new SQLExerciseDAO(this, mediaDir);
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExercises
   * @param i
   * @param lessonPlanFile
   * @param mediaDir
   * @param isUrdu
   */
  public void setInstallPath(String i, String lessonPlanFile, String mediaDir, boolean isUrdu) {
    //logger.debug("got install path " + i + " media " + mediaDir + " is urdu " +isUrdu);
    this.installPath = i;
    this.lessonPlanFile = lessonPlanFile;
    this.mediaDir = mediaDir;
    this.isUrdu = isUrdu;
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExercises(boolean)
   * @param useFile
   * @return
   */
  public List<Exercise> getExercises(boolean useFile) {
    List<Exercise> exercises = getExercises(useFile, lessonPlanFile);
    return exercises;
  }

  /**
   *
   *
   * @param useFile
   * @param lessonPlanFile
   * @return
   * @see #getExercises(boolean)
   * @see #getExercises(long, boolean)
   */
  private List<Exercise> getExercises(boolean useFile, String lessonPlanFile) {
    if (exerciseDAO == null || useFile && exerciseDAO instanceof SQLExerciseDAO || !useFile && exerciseDAO instanceof FileExerciseDAO) {
      this.exerciseDAO = makeExerciseDAO(useFile);
    }

    if (useFile) {
      ((FileExerciseDAO) exerciseDAO).readFastAndSlowExercises(installPath, lessonPlanFile);
    }
    return exerciseDAO.getRawExercises();
  }

  /**
   *
   * @param activeExercises
   * @param expectedCount
   * @param filterResults
   * @param useFLQ
   * @param useSpoken @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getNextUngradedExercise
   */
  public Exercise getNextUngradedExercise(Collection<String> activeExercises, int expectedCount, boolean filterResults, boolean useFLQ, boolean useSpoken) {
    if (expectedCount == 1) {
      return getNextUngradedExerciseQuick(activeExercises, expectedCount, filterResults, useFLQ, useSpoken);
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
   * @see #getNextUngradedExercise
   * @param activeExercises
   * @param expectedCount
   * @param filterResults
   *@param useFLQ
   * @param useSpoken @return next exercise containing ungraded results
   */
  private Exercise getNextUngradedExerciseQuick(Collection<String> activeExercises, int expectedCount, boolean filterResults, boolean useFLQ, boolean useSpoken) {
    List<Exercise> rawExercises = getExercises(false);
    Collection<Result> resultExcludingExercises = resultDAO.getResultExcludingExercises(activeExercises);

    GradeDAO.GradesAndIDs allGradesExcluding = gradeDAO.getAllGradesExcluding(activeExercises);
    Map<Integer, Integer> idToCount = new HashMap<Integer, Integer>();
    for (Grade g : allGradesExcluding.grades) {
      if (!idToCount.containsKey(g.resultID)) idToCount.put(g.resultID, 1);
      else idToCount.put(g.resultID, 2);
    }
    logger.info("getNextUngradedExercise found " + resultExcludingExercises.size() + " results, " +
        "expected " + expectedCount + ", " + allGradesExcluding.resultIDs.size() + " graded results");

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
      else if (filterResults && (result.flq != useFLQ || result.spoken != useSpoken)) {
        //logger.debug("getNextUngradedExercise excluding " + result + " since no match for flq = " + useFLQ + " and spoken = " +useSpoken);
        iterator.remove();
      }
      else if (numGrades != null) System.out.println("\tfound grade " + numGrades + " for " +result +"?");
    }

    logger.debug("getNextUngradedExercise after removing graded, there were " + resultExcludingExercises.size() + " results");

    // whatever remains, find first exercise

    SortedSet<String> exids = new TreeSet<String>();
    //int n = 0;
    for (Result r : resultExcludingExercises) {
      //if (n++ < 10) System.out.println("ungraded include " + r + " and grade " + idToCount.get(r.uniqueID));
      exids.add(r.id);
    }
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
   *
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
      return getExercises(useFile, lessonPlanFile);
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
   // for (ResultAndGrade rg : rgs)  System.out.println("rg " + rg);
    List<Exercise> ret = new ArrayList<Exercise>();
    int total = 0;
    for (ResultAndGrade rg : rgs) {
      total += rg.totalValidGrades();
      ret.add(idToExercise.get(rg.result.id));
    }
    logger.info("total valid grades " + total);
    return ret;
  }

  public List<Exercise> getExercisesFirstNInOrder(long userID, boolean useFile, int firstNInOrder) {
    List<Exercise> rawExercises = getExercises(useFile);
    int numInOrder = Math.min(firstNInOrder, rawExercises.size());
    List<Exercise> newList = new ArrayList<Exercise>(rawExercises.subList(0, numInOrder));

    List<Exercise> randomExercises = rawExercises.size() > numInOrder ? new ArrayList<Exercise>(rawExercises.subList(numInOrder,rawExercises.size())) : new ArrayList<Exercise>();

    Collections.shuffle(randomExercises,new Random(userID));
    newList.addAll(randomExercises);

    //logger.debug("got " + newList.size());
    if (newList.isEmpty()) logger.warn("no exercises for " + userID + "?");

    String exerciseIDLastResult = resultDAO.getExerciseIDLastResult(userID);
    if (!exerciseIDLastResult.equals("INVALID")) {
      int i = 0;
      for (Exercise e : newList) {
        if (e.getID().equals(exerciseIDLastResult)) {
          break;
        }
        else { i++; }
      }
      i++;
      if (i == newList.size()-1) i = 0;
      List<Exercise> back = newList.subList(i, newList.size());
      List<Exercise> front = newList.subList(0, i);
      logger.info("starting from #" + i + " or " + exerciseIDLastResult + " back " + back.size() + " front " + front.size());
      back.addAll(front);
      newList = back;
      assert(newList.size() == back.size());//, "huh? sizes aren't equal " + newList.size() + " vs " + back.size());
    }
    return newList;
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
   * Somehow on subsequent runs, the ids skip by 30 or so?
   * <p/>
   * Uses return generated keys to get the user id
   *
   * @see mitll.langtest.server.LangTestDatabaseImpl#addUser(int, String, int)
   * @param age
   * @param gender
   * @param experience
   * @param ipAddr
   * @return
   */
  public long addUser(int age, String gender, int experience, String ipAddr) {
    return userDAO.addUser(age, gender, experience, ipAddr, "", "", "", "", "");
  }

  public long addUser(int age, String gender, int experience, String ipAddr, String firstName, String lastName, String nativeLang,String dialect, String userID) {
    return userDAO.addUser(age, gender, experience, ipAddr, firstName, lastName, nativeLang, dialect, userID);
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

  public ResultsAndGrades getResultsForExercise(String exid) {
    return getResultsForExercise(exid, false, false, false);
  }

  /**
   * Find all the grades for this exercise.<br></br>
   * Find all the results for this exercise. <br></br>
   * Get these schedules for this exercise and every user. <br></br>
   * For every result, get the user and use it to find the schedule.  <br></br>
   * Use the data in the schedule to mark the en/fl and spoken/written bits on the Results. <br></br>
   * This lets us make a map of spoken->lang->results
   *
   * @param exid
   * @return ResultsAndGrades
   * @see mitll.langtest.server.LangTestDatabaseImpl#getResultsForExercise
   * @see mitll.langtest.client.grading.GradingExercisePanel#getAnswerWidget(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int)
   */
  public ResultsAndGrades getResultsForExercise(String exid, boolean filterByFLQAndSpoken, boolean useFLQ, boolean useSpoken) {
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

      boolean takeThisOne = !filterByFLQAndSpoken || (r.flq == useFLQ && r.spoken == useSpoken);
      if (takeThisOne) {
        boolean spoken = r.spoken;
        boolean flq = r.flq;

        Map<Boolean, List<Result>> langToResult = spokenToLangToResult.get(spoken);
        if (langToResult == null)
          spokenToLangToResult.put(spoken, langToResult = new HashMap<Boolean, List<Result>>());
        List<Result> resultsForLang = langToResult.get(flq);
        if (resultsForLang == null) langToResult.put(flq, resultsForLang = new ArrayList<Result>());
        resultsForLang.add(r);
      }
    }
    return new ResultsAndGrades(resultsForExercise, gradesAndIDs.grades, spokenToLangToResult);
  }

  private Map<Integer, List<Grade>> getIdToGrade(Collection<Grade> grades) {
    Map<Integer,List<Grade>> idToGrade = new HashMap<Integer, List<Grade>>();
    for (Grade g : grades) {
      List<Grade> gradesForResult = idToGrade.get(g.resultID);
      if (gradesForResult == null) {
        idToGrade.put(g.resultID, gradesForResult = new ArrayList<Grade>());
      }
      gradesForResult.add(g);
    }

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

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#graderExists(String)
   * @param login
   * @return
   */
  public boolean graderExists(String login) {
    return graderDAO.graderExists(login);
  }

  public int userExists(String login) {
    return userDAO.userExists(login);
  }

  public boolean isAnswerValid(int userID, Exercise exercise, int questionID, Database database) {
    return answerDAO.isAnswerValid(userID, exercise, questionID, database);
  }

  /**
   * TODO : worry about duplicate userid?
   * @return
   */
  public Map<User, Integer> getUserToResultCount() {
    List<User> users = getUsers();
    List<Result> results = getResults();

    Map<User,Integer> idToCount = new HashMap<User, Integer>();
    Map<Long,User> idToUser = new HashMap<Long, User>();
    for (User u : users) {
      idToUser.put(u.id,u);
      idToCount.put(u,0);
    }
    for (Result r : results) {
      User user = idToUser.get(r.userid);
      Integer c = idToCount.get(user);
      if (c != null) {
        idToCount.put(user, c + 1);
      }
    }
    return idToCount;
  }

  public Map<Exercise, Integer> getResultIdToCount(boolean useFile, boolean getMale) {
    List<Result> results = getResults();
    List<Exercise> exercises = getExercises(useFile);
    Map<String,Exercise> idToEx = new HashMap<String, Exercise>();
    for (Exercise e: exercises) idToEx.put(e.getID(),e);
    boolean isInt = false;
    try {
      String id = exercises.iterator().next().getID();
      Integer.parseInt(id);
      isInt = true;
    } catch (NumberFormatException e) {
    }

    final boolean fint = isInt;
    Map<Exercise,Integer> idToCount = new TreeMap<Exercise, Integer>(new Comparator<Exercise>() {
      @Override
      public int compare(Exercise o1, Exercise o2) {
        String fid = o1.getID();
        String sid = o2.getID();
        if (fint) {
          Integer fint = Integer.parseInt(fid);
          Integer sint = Integer.parseInt(sid);
          return fint.compareTo(sint);
        }
        else {
          return fid.compareTo(sid);
        }
      }
    });
    Map<Long, User> idToUser = getUserMap(getMale);
    for (Result r : results) {
      User user = idToUser.get(r.userid);
      if (user != null) {
        Exercise exercise = idToEx.get(r.id);
        if (exercise != null) {
          Integer c = idToCount.get(exercise);
          if (c != null) {
            idToCount.put(exercise, c + 1);
          } else {
            idToCount.put(exercise, 1);
          }
        }
      }
    }
    return idToCount;
  }

  private Map<Long, User> getUserMap(boolean getMale) {
    List<User> users = getUsers();
    Map<Long,User> idToUser = new HashMap<Long, User>();
    for (User u : users) {
      if (u.isMale() && getMale || (!u.isMale() && !getMale)) {
        idToUser.put(u.id, u);
      }
    }
    return idToUser;
  }

  public List<Session> getSessions() {
    List<Result> results = getResults();
    logger.debug("total " + results.size());
    Map<Long,List<Result>> userToAnswers = new HashMap<Long, List<Result>>();
    for (Result r : results) {
      List<Result> results1 = userToAnswers.get(r.userid);
      if (results1 == null) userToAnswers.put(r.userid, results1 = new ArrayList<Result>());
      results1.add(r);
    }
    List<Session> sessions = new ArrayList<Session>();
    for (List<Result> resultList : userToAnswers.values()) {
      Collections.sort(resultList, new Comparator<Result>() {
        @Override
        public int compare(Result o1, Result o2) {
          return o1.timestamp < o2.timestamp ? -1 : o1.timestamp > o2.timestamp ? +1 : 0;
        }
      });
      Session s = null;
      long last = 0;
      for (Result r : resultList) {
        int sessionGap = 10 * 60 * 1000;
        if (s == null || r.timestamp - last > sessionGap) {
          //if (s != null
          s = new Session();
          sessions.add(s);
          //s.duration = 0l;
        } else {
          s.duration += r.timestamp - last;
        }
        s.numAnswers++;
        last = r.timestamp;
      }
    }
    return sessions;
  }

  /**
   * TODO : worry about duplicate userid?
   * @return
   */
  public Map<Integer, Integer> getResultCountToCount(boolean useFile) {
    Map<String, Integer> idToCount = getExToCount(useFile);
    Map<Integer,Integer> resCountToCount = new HashMap<Integer, Integer>();

    for (Integer c : idToCount.values()) {
      Integer rc = resCountToCount.get(c);
      if (rc == null) resCountToCount.put(c, 1);
      else resCountToCount.put(c, rc + 1);
    }
    return resCountToCount;
  }

  private Map<String, Integer> getExToCount(boolean useFile) {
    Map<String, Integer> idToCount = getInitialIdToCount(useFile);

    List<Result> results = getResults();
    for (Result r : results) {
      String key = r.id + "/" + r.qid;
      Integer c = idToCount.get(key);
      if (c == null) {
        idToCount.put(key, 1);
      }
      else idToCount.put(key, c + 1);
    }

    return idToCount;
  }

  private Map<String, Integer> getExToCountMaleOrFemale(boolean useFile,boolean isMale) {
    Map<String, Integer> idToCount = getInitialIdToCount(useFile);

    Map<Long, User> userMap = getUserMap(isMale);

    List<Result> results = getResults();
    for (Result r : results) {
      if (userMap.containsKey(r.userid)) {
        String key = r.id + "/" + r.qid;
        Integer c = idToCount.get(key);
        if (c == null) {
          idToCount.put(key, 1);
        } else idToCount.put(key, c + 1);
      }
    }

    return idToCount;
  }

  private Map<String, Integer> getInitialIdToCount(boolean useFile) {
    List<Exercise> exercises = getExercises(useFile);
    Map<String,Integer> idToCount = new HashMap<String, Integer>();
    for (Exercise e : exercises) {
      if (e.getNumQuestions() == 0) {
        String key = e.getID() + "/0";
        idToCount.put(key,0);
      }
      else {
        for (int i = 0; i < e.getNumQuestions(); i++) {
          String key = e.getID() + "/" + i;
          idToCount.put(key,0);
        }
      }
    }
    return idToCount;
  }

  private static class CompoundKey implements Comparable<CompoundKey> {
    public final int first,second;

    public CompoundKey(int first, int second) {
      this.first = first;
      this.second = second;
    }

    @Override
    public boolean equals(Object obj) {
      CompoundKey other =(CompoundKey) obj;
      return compareTo(other) == 0;
    }

    @Override
    public int compareTo(CompoundKey o) {
      return first < o.first ? -1 : first > o.first ? +1 : second < o.second ? -1 : second > o.second ? +1 : 0;
    }

    public String toString() { return first +"/"+second; }
  }

  /**
   * TODO : worry about duplicate userid?
   * @return
   */
  public Map<String, Integer> getResultByDay() {
    List<Result> results = getResults();
    SimpleDateFormat df = new SimpleDateFormat("MM-dd-yy");
    Map<String,Integer> dayToCount = new HashMap<String, Integer>();
    for (Result r : results) {
      Date date = new Date(r.timestamp);
      String day = df.format(date);
      Integer c = dayToCount.get(day);
      if (c == null) {
        dayToCount.put(day, 1);
      }
      else dayToCount.put(day, c + 1);
    }
    return dayToCount;
  }

  public Map<String, Integer> getResultByHourOfDay() {
    List<Result> results = getResults();
    SimpleDateFormat df = new SimpleDateFormat("HH");
    Map<String,Integer> dayToCount = new HashMap<String, Integer>();
    for (Result r : results) {
      Date date = new Date(r.timestamp);
      String day = df.format(date);
      Integer c = dayToCount.get(day);
      if (c == null) {
        dayToCount.put(day, 1);
      }
      else dayToCount.put(day, c + 1);
    }
    return dayToCount;
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getResultPerExercise(boolean)
   * @param useFile
   * @return
   */
  public Map<String,List<Integer>> getResultPerExercise(boolean useFile) {
    Map<String, Integer> exToCount = getExToCount(useFile);
    List<Integer> overall = getCountArray(exToCount);

    List<Integer> male = getCountArray(getExToCountMaleOrFemale(useFile,true));

    List<Integer> female = getCountArray(getExToCountMaleOrFemale(useFile,false));

    Map<String,List<Integer>> typeToList = new HashMap<String, List<Integer>>();
    typeToList.put("overall",overall);
    typeToList.put("male",male);
    typeToList.put("female",female);
    return typeToList;
  }

  private List<Integer> getCountArray(Map<String, Integer> exToCount) {
    List<Integer> countArray = new ArrayList<Integer>(exToCount.size());
    String next = exToCount.keySet().iterator().next();
    boolean isInt = false;
    try {
      String left = next.split("/")[0];
      Integer.parseInt(left);
      isInt = true;
    } catch (NumberFormatException e) {
    }
    if (isInt) {
      Map<CompoundKey, Integer> keyToCount = new TreeMap<CompoundKey, Integer>();
      for (Map.Entry<String, Integer> pair : exToCount.entrySet()) {
        try {
          String[] split = pair.getKey().split("/");
          String left = split[0];
          int exid = Integer.parseInt(left);
          String right = split[0];
          int qid = Integer.parseInt(right);

          keyToCount.put(new CompoundKey(exid, qid), pair.getValue());
        } catch (NumberFormatException e) {
          e.printStackTrace();
        }
      }
      countArray.addAll(keyToCount.values());
      return countArray;
    } else {
      for (Map.Entry<String, Integer> pair : exToCount.entrySet()) {
        countArray.add(pair.getValue());
      }
      return countArray;
    }
  }

  public void destroy() {
    try {
      connection.contextDestroyed();
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
/*    DatabaseImpl langTestDatabase = new DatabaseImpl("C:\\Users\\go22670\\mt_repo\\jdewitt\\pilot\\vlr-parle");
    langTestDatabase.mediaDir = "C:\\Users\\go22670\\DLITest\\clean\\netPron2\\war\\config\\autocrt\\";
     langTestDatabase.getExercises(false);
     langTestDatabase.getClassifier();
    double score = langTestDatabase.getScoreForExercise("bc-R10-k227",1,"bueller");
    System.out.println("Score was " + score);
    if (true) {
*//*      List<ExerciseExport> exerciseNames = langTestDatabase.getExport(true, false);

      System.out.println("names " + exerciseNames.size() + " e.g. " + exerciseNames.get(0));
      for (ExerciseExport ee : exerciseNames) {
        System.out.println("ee " + ee);
      }*//*
    } else {
      //List<Exercise> exercises = langTestDatabase.getRandomBalancedList();

    }*/

    DatabaseImpl langTestDatabase = new DatabaseImpl("C:\\Users\\go22670\\DLITest\\","farsi2");
    langTestDatabase.setInstallPath("C:\\Users\\go22670\\DLITest\\clean\\netPron2\\war\\config\\urdu","C:\\Users\\go22670\\DLITest\\clean\\netPron2\\war\\config\\urdu\\5000-no-english.unvow.farsi.txt","",false);


    List<Session> sessions = langTestDatabase.getSessions();
    long total = 0;
    for (Session s : sessions) {
      System.out.println(s);
      total += s.numAnswers;
    }
    System.out.println("total " + total);
    /*Map<Exercise, Integer> idToCount = langTestDatabase.getResultIdToCount(true,true);
    writeMap(idToCount, "farsiMaleCounts.csv");
    Map<Exercise, Integer> idToCount2 = langTestDatabase.getResultIdToCount(true,false);
    writeMap(idToCount2,"farsiFemaleCounts.csv");

    langTestDatabase = new DatabaseImpl("C:\\Users\\go22670\\DLITest\\","urdu");
    langTestDatabase.setInstallPath("","C:\\Users\\go22670\\DLITest\\clean\\netPron2\\war\\config\\urdu\\urdu-3684-no-english.txt","",false);
    idToCount = langTestDatabase.getResultIdToCount(true,true);
    writeMap(idToCount,"urduMaleCounts.csv");
    idToCount2 = langTestDatabase.getResultIdToCount(true,false);
    writeMap(idToCount2,"urduFemaleCounts.csv");
*/
//    langTestDatabase.getUserToResultCount();
   // System.out.println("map " + langTestDatabase.getResultCountToCount());
  }

  private static void writeMap(Map<Exercise, Integer> idToCount2, String fileName) {
    try {
      Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), "UTF-8"));
      for (Map.Entry<Exercise, Integer> pair : idToCount2.entrySet())
        w.write(pair.getKey().getID() + "," + pair.getValue() + "," + pair.getKey().getTooltip().trim()+ "\n");
      w.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
