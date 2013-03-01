package mitll.langtest.server.database;

import mitll.langtest.shared.*;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

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
  private static final int KB = (1024);
  private static Logger logger = Logger.getLogger(DatabaseImpl.class);

  private static final boolean DROP_USER = false;
  private static final boolean DROP_RESULT = false;
  private Map<Long, List<Schedule>> userToSchedule;

  private String installPath;
  private ExerciseDAO exerciseDAO = null;
  private final UserDAO userDAO = new UserDAO(this);
  private final ResultDAO resultDAO = new ResultDAO(this);
  public final AnswerDAO answerDAO = new AnswerDAO(this);
  private final GradeDAO gradeDAO = new GradeDAO(this);
 // private final GraderDAO graderDAO = new GraderDAO(this);
  private final SiteDAO siteDAO = new SiteDAO(this, userDAO);

  private DatabaseConnection connection = null;
  private MonitoringSupport monitoringSupport;

  /**
   * TODO : consider making proper v2 database!
   */
  private String lessonPlanFile;
  private String mediaDir;
  private boolean isUrdu;
  private boolean useFile;
  private final boolean showSections;

  /**
   * Just for testing
   * @param configDir
   * @see mitll.langtest.server.LangTestDatabaseImpl#readProperties(javax.servlet.ServletContext)
   */
  public DatabaseImpl(String configDir) {
    this(configDir, "vlr-parle", false);
  }

  public DatabaseImpl(String configDir, String dbName, boolean showSections) {
    connection = new H2Connection(configDir, dbName);
    this.showSections = showSections;
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
    monitoringSupport = getMonitoringSupport();
  }

  private void initializeDAOs() {
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
    }
    try {
      resultDAO.createResultTable(getConnection());
    } catch (Exception e) {
      logger.error("got " + e, e);  //To change body of catch statement use File | Settings | File Templates.
    }

    try {
      // gradeDAO.dropGrades();
      gradeDAO.createGradesTable(getConnection());
      //graderDAO.createGraderTable(getConnection());
      //userDAO.dropUserTable(this);

      userDAO.createUserTable(this);
      siteDAO.createTable(getConnection());
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

  public MonitoringSupport getMonitoringSupport() {
    return new MonitoringSupport(userDAO, resultDAO);
  }

  /**
   * @see #getExercises(boolean, String)
   * @param useFile
   * @return
   */
  private ExerciseDAO makeExerciseDAO(boolean useFile) {
    return useFile ? new FileExerciseDAO(mediaDir, isUrdu, showSections) : new SQLExerciseDAO(this, mediaDir);
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExercises
   * @param installPath
   * @param lessonPlanFile
   * @param mediaDir
   * @param isUrdu
   */
  public void setInstallPath(String installPath, String lessonPlanFile, String mediaDir, boolean isUrdu, boolean useFile) {
    //logger.debug("got install path " + i + " media " + mediaDir + " is urdu " +isUrdu);
    this.installPath = installPath;
    this.lessonPlanFile = lessonPlanFile;
    this.mediaDir = mediaDir;
    this.isUrdu = isUrdu;
    this.useFile = useFile;

    //resultDAO.enrichResultDurations(installPath);
  }

  public void setOutsideFile(String outsideFile) { monitoringSupport.setOutsideFile(outsideFile); }

  public Map<String, Collection<String>> getTypeToSection() {
    getExercises();
    return exerciseDAO.getTypeToSections();
  }

  public Map<String, List<String>> getTypeToSectionsForTypeAndSection(String type, String section) {
    return exerciseDAO.getTypeToSectionsForTypeAndSection(type, section);
  }

  public Collection<Exercise> getExercisesForSection(String type, String section) {
    return exerciseDAO.getExercisesForSection(type, section);
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExercises
   * @return
   */
  public List<Exercise> getExercises() {
    return getExercises(useFile, lessonPlanFile);
  }

  /**
   *
   *
   * @param useFile
   * @param lessonPlanFile
   * @return
   * @see #getExercises()
   * @see #getExercises(long)
   */
  private List<Exercise> getExercises(boolean useFile, String lessonPlanFile) {
    if (lessonPlanFile == null) {
      logger.error("huh? lesson plan file is null???", new Exception());
      return Collections.emptyList();
    }
    boolean isExcel = lessonPlanFile.endsWith(".xlsx");
    makeDAO(useFile, lessonPlanFile, isExcel);

    if (useFile && !isExcel) {
      ((FileExerciseDAO) exerciseDAO).readFastAndSlowExercises(installPath, lessonPlanFile);
    }
    List<Exercise> rawExercises = exerciseDAO.getRawExercises();
    if (rawExercises.isEmpty()) {
      logger.warn("no exercises for useFile = " + useFile + " and " + lessonPlanFile + " at " + installPath);
    }
    return rawExercises;
  }

  private void makeDAO(boolean useFile, String lessonPlanFile, boolean excel) {
    if (exerciseDAO == null) {// || useFile && exerciseDAO instanceof SQLExerciseDAO || !useFile && exerciseDAO instanceof FileExerciseDAO) {
      if (useFile && excel) {
        this.exerciseDAO = new ExcelImport(lessonPlanFile);
      }
      else {
        this.exerciseDAO = makeExerciseDAO(useFile);
      }
    }
  }

  /**
   *
   *
   * @param activeExercises
   * @param expectedCount
   * @param filterResults
   * @param useFLQ
   * @param useSpoken @return
   * @param englishOnly
   * @see mitll.langtest.server.LangTestDatabaseImpl#getNextUngradedExercise
   */
  public Exercise getNextUngradedExercise(Collection<String> activeExercises, int expectedCount, boolean filterResults,
                                          boolean useFLQ, boolean useSpoken, boolean englishOnly) {
    if (expectedCount == 1) {
      return getNextUngradedExerciseQuick(activeExercises, expectedCount, filterResults, useFLQ, useSpoken);
    } else {
      return getNextUngradedExerciseSlow(activeExercises, expectedCount, englishOnly);
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
  private Exercise getNextUngradedExerciseSlow(Collection<String> activeExercises, int expectedCount, boolean englishOnly) {
    List<Exercise> rawExercises = getExercises();
    logger.info("getNextUngradedExerciseSlow : checking " + rawExercises.size() + " exercises.");
    for (Exercise e : rawExercises) {
      if (!activeExercises.contains(e.getID()) && // no one is working on it
          resultDAO.areAnyResultsLeftToGradeFor(e, expectedCount, englishOnly)) {
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
  private Exercise getNextUngradedExerciseQuick(Collection<String> activeExercises, int expectedCount,
                                                boolean filterResults, boolean useFLQ, boolean useSpoken) {
    List<Exercise> rawExercises = getExercises();
    Collection<Result> resultExcludingExercises = resultDAO.getResultExcludingExercises(activeExercises);

    GradeDAO.GradesAndIDs allGradesExcluding = gradeDAO.getAllGradesExcluding(activeExercises);
    Map<Integer, Integer> idToCount = new HashMap<Integer, Integer>();
    for (Grade g : allGradesExcluding.grades) {
      if (!idToCount.containsKey(g.resultID)) idToCount.put(g.resultID, 1);
      else idToCount.put(g.resultID, 2);
    }
    logger.info("getNextUngradedExerciseQuick found " + resultExcludingExercises.size() + " results, " +
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
   *
   * @param userID for this user
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExercises(long, boolean)
   * @deprecated we should move away from using schedules to determine exercise order, etc.
   */
  public List<Exercise> getExercises(long userID) {
    logger.info("getExercises : for user  " + userID);
    if (userID == -1 || true) {
      List<Exercise> exercises = getExercises(useFile, lessonPlanFile);
      return Collections.unmodifiableList(exercises);
     // return exercises;
    }

    if (userToSchedule == null) {
      ScheduleDAO scheduleDAO = new ScheduleDAO(this);
      this.userToSchedule = scheduleDAO.getSchedule();
    }
    List<Schedule> forUser = userToSchedule.get(userID);
    if (forUser == null) {
      logger.warn("no schedule for user " + userID);
      return getExercises(useFile, lessonPlanFile);
    }
    List<Exercise> exercises = new ArrayList<Exercise>();

    List<Exercise> rawExercises = getExercises();
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

  /**
   * Show unanswered questions first, then ones with 1, then 2, then 3,... answers
   * Also be aware of the user's gender -- if you're female, show questions that have no female answers first.
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExercises(long, boolean)
   * @param userID
   * @return
   */
  public List<Exercise> getExercisesBiasTowardsUnanswered(long userID) {
    Map<String, Exercise> idToExercise = new HashMap<String, Exercise>();
    Map<String,Integer> idToCount = new HashMap<String, Integer>();

    populateInitialExerciseIDToCount(idToExercise, idToCount);

    // only find answers that are for the gender
    getExerciseIDToResultCount(userDAO.isUserMale(userID), idToCount);

    Map<Integer, List<String>> countToIds = getCountToExerciseIDs(idToCount);
    return getResultsRandomizedPerUser(userID, idToExercise, countToIds);
  }

  /**
   * Merge the online counts with counts from an external file.
   *
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExercises(long, boolean)
   * @param userID
   * @param outsideFile
   * @return
   */
  public List<Exercise> getExercisesBiasTowardsUnanswered(long userID, String outsideFile) {
    Map<String,Integer> idToCount = new HashMap<String, Integer>();
    Map<String, Exercise> idToExercise = new HashMap<String, Exercise>();

    populateInitialExerciseIDToCount(idToExercise, idToCount);
    //logger.info("initial map of online counts is size = " + idToCount.size() +" " + idToCount.values().size());

    boolean isMale = userDAO.isUserMale(userID);

    Map<String, Integer> idToCountOutside =
        new OutsideCount().getExerciseIDToOutsideCount(isMale, outsideFile,getExercises());

    //  logger.info("map of outside counts is size = " + idToCountOutside.size() +" " + idToCountOutside.values().size());
    for (Map.Entry<String, Integer> pair : idToCountOutside.entrySet()) {
      Integer count = idToCount.get(pair.getKey());
      if (count == null) logger.warn("missing exercise id " + pair.getKey());
      else idToCount.put(pair.getKey(),count+pair.getValue());
    }

    // only find answers that are for the gender
    getExerciseIDToResultCount(userDAO.isUserMale(userID), idToCount);

    // now make a map of count at this number to exercise ids for these numbers
    Map<Integer, List<String>> countToIds = getCountToExerciseIDs(idToCount);

    return getResultsRandomizedPerUser(userID, idToExercise, countToIds);
  }

  /**
   * Given a map of answer counts to exercise ids at those counts, randomize the order based on the
   * user id, then return a list of Exercises with those ids.
   *
   * @see #getExercisesBiasTowardsUnanswered(long)
   * @see #getExercisesBiasTowardsUnanswered(long, String)
   * @param userID for this user
   * @param idToExercise so we can go from id to exercise
   * @param countToIds statistics about answers for each exercise
   * @return List of exercises in order from least answers to most
   */
  private List<Exercise> getResultsRandomizedPerUser(long userID, Map<String, Exercise> idToExercise, Map<Integer, List<String>> countToIds) {
    List<Exercise> result = new ArrayList<Exercise>();
    Random rnd = new Random(userID);

    for (List<String> itemsAtCount : countToIds.values()) {
      Collections.shuffle(itemsAtCount, rnd);
      for (String id : itemsAtCount) {
        Exercise e = idToExercise.get(id);
        if (e == null) logger.error("huh? couldn't find exercise " + id);
        else result.add(e);
      }
    }
    return result;
  }

  private Map<Integer, List<String>> getCountToExerciseIDs(Map<String, Integer> idToCount) {
    Map<Integer,List<String>> countToIds = new TreeMap<Integer, List<String>>();
    for (Map.Entry<String,Integer> pair : idToCount.entrySet()) {
      List<String> idsAtCount = countToIds.get(pair.getValue());
      if (idsAtCount == null) { countToIds.put(pair.getValue(), idsAtCount = new ArrayList<String>()); }
      idsAtCount.add(pair.getKey());
    }
    //logger.info("map is " +countToIds);
    //for (Integer c: countToIds.keySet()) logger.info("count = " +c + " : " + countToIds.get(c).size() );
    return countToIds;
  }

  private void populateInitialExerciseIDToCount(Map<String, Exercise> idToExercise, Map<String, Integer> idToCount) {
    List<Exercise> rawExercises = getExercises();
    for (Exercise e : rawExercises) {
      idToCount.put(e.getID(), 0);
      idToExercise.put(e.getID(), e);
    }
  }

  private void getExerciseIDToResultCount(boolean userMale, Map<String, Integer> idToCount) {
    Map<Long, User> userMap = userDAO.getUserMap(userMale);
    List<Result> results = getResults();
    for (Result r: results ) {
      Integer current = idToCount.get(r.id);
      if (current != null) {
        if (userMap.containsKey(r.userid)) {
          idToCount.put(r.id,current+1);
        }
      }
    }
  }

  private Random random = new Random();
  public List<Exercise> getRandomBalancedList() {
    List<Exercise> exercisesGradeBalancing = getExercisesGradeBalancing();
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

  private List<Exercise> getExercisesGradeBalancing() {
    return getExercisesGradeBalancing(true, false);
  }

    /**
     * Return exercises in an order that puts the items with all right answers towards the front and all wrong
     * toward the back (in the arabic data collect the students were too advanced for the questions, so
     * they mostly got them right).<br></br>
     *
     * Remember there can be multiple questions per exercise, so we need to average over the grades for all
     * answers for all questions for an exercise.
     *
     *
     * @param useFLQ
     * @param useSpoken
     * @return
     */
  private List<Exercise> getExercisesGradeBalancing(boolean useFLQ, boolean useSpoken) {
    List<Exercise> rawExercises = getExercises();
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

  public List<Exercise> getExercisesFirstNInOrder(long userID, int firstNInOrder) {
    List<Exercise> rawExercises = getExercises();
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

  public boolean isAdminUser(long id) {
    User user = getUser(id);
    return user != null && user.admin;
  }

  public void setUserEnabled(long id, boolean enabled) {
    userDAO.enableUser(id,enabled);
  }

  public boolean isEnabledUser(long id) {
    User user = getUser(id);
    return user != null && user.enabled;
  }

  public User getUser(long id) {
    return userDAO.getUserMap().get(id);
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
    return userDAO.addUser(age, gender, experience, ipAddr, "", "", "", "", "", false);
  }

  public long addUser(int age, String gender, int experience, String ipAddr, String firstName, String lastName,
                      String nativeLang,String dialect, String userID) {
    return userDAO.addUser(age, gender, experience, ipAddr, firstName, lastName, nativeLang, dialect, userID, false);
  }


  public List<User> getUsers() {
    return userDAO.getUsers();
  }


  /**
   * Pulls the list of results out of the database.
   *
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getResults(int, int)
   */
  public List<Result> getResults() {
    return resultDAO.getResults();
  }

  public int getNumResults() { return resultDAO.getNumResults(); }

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
   *
   * @param userID
   * @param e
   * @param questionID
   * @param answer
   * @see mitll.langtest.client.exercise.ExercisePanel#postAnswers(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserFeedback, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.shared.Exercise)
   */
  public void addAnswer(int userID, Exercise e, int questionID, String answer) {
    answerDAO.addAnswer(userID, e, questionID, answer, "", !e.promptInEnglish, false, Result.AUDIO_TYPE_UNSET);
  }

  public void addAudioAnswer(int userID, String plan, String exerciseID, int questionID,
                             String audioFile,
                             boolean valid, boolean flq, boolean spoken,
                             String audioType, int durationInMillis) {
    answerDAO.addAnswer(this, userID, plan, exerciseID, questionID, "", audioFile, valid, flq, spoken, audioType,
        durationInMillis);
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

  public int userExists(String login) {
    return userDAO.userExists(login);
  }

  public boolean isAnswerValid(int userID, Exercise exercise, int questionID, Database database) {
    return answerDAO.isAnswerValid(userID, exercise, questionID, database);
  }


  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#service(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   * @param site
   * @return
   */
  public Site addSite(Site site) { return siteDAO.addSite(site);  }
  public boolean siteExists(Site site) { return siteDAO.getSiteWithName(site.name) != null;  }
  public Site getSiteByID(long id) { return siteDAO.getSiteByID(id); }
  public List<Site> getDeployedSites() { return siteDAO.getDeployedSites(); }
  public void deploy(Site site) { siteDAO.deploy(site); }
  public Site updateSite(Site site, String name, String lang, String notes) { return siteDAO.updateSite(site,name,lang,notes); }

  /**
   * TODO : worry about duplicate userid?
   * @return map of user to number of answers the user entered
   */
  public Map<User, Integer> getUserToResultCount() { return monitoringSupport.getUserToResultCount(); }

  /**
   * Determine sessions per user.  If two consecutive items are more than {@link MonitoringSupport#SESSION_GAP} seconds
   * apart, then we've reached a session boundary.
   * Remove all sessions that have just one answer - must be test sessions.
   * @return list of duration and numAnswer pairs
   */
  public List<Session> getSessions() { return monitoringSupport.getSessions(); }
  /**
   * Given the observed rate of responses and the number of exercises to get
   * responses for, make a map of number of responses->hours
   * required to get that number of responses.
   * @return # responses->hours to get that number
   */
  public Map<Integer,Float> getHoursToCompletion() {   return  monitoringSupport.getHoursToCompletion(getExercises()); }
 /**
   * TODO : worry about duplicate userid?
   * @return
   */
  public Map<Integer, Integer> getResultCountToCount() { return  monitoringSupport.getResultCountToCount(getExercises()); }

  /**
   * Get counts of answers by date
   * TODO : worry about duplicate userid?
   *
   * @return
   */
  public Map<String, Integer> getResultByDay() { return monitoringSupport.getResultByDay(); }

  /**
   * get counts of answers by hours of the day
   * @return
   */
  public Map<String, Integer> getResultByHourOfDay() {return monitoringSupport.getResultByHourOfDay(); }

  /**
   * Split exid->count by gender.
   * @see mitll.langtest.server.LangTestDatabaseImpl#getResultPerExercise
   * @return
   */
  public Map<String,List<Integer>> getResultPerExercise() { return monitoringSupport.getResultPerExercise(getExercises()); }
  public Map<String,Map<Integer,Integer>> getResultCountsByGender() {  return monitoringSupport.getResultCountsByGender(getExercises()); }
  public Map<String, Map<Integer, Map<Integer, Integer>>> getDesiredCounts() {  return monitoringSupport.getDesiredCounts(getExercises()); }
   /**
   * Return some statistics related to the hours of audio that have been collected
   * @return
   */
  public Map<String,Number> getResultStats() { return monitoringSupport.getResultStats(); }

  public void destroy() {
    try {
      connection.contextDestroyed();
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
  }

  public void closeConnection(Connection connection) throws SQLException {}

/*
  private void logMemory() {
    Runtime rt = Runtime.getRuntime();
    long free = rt.freeMemory();
    long used = rt.totalMemory()-free;
    long max = rt.maxMemory();
    logger.debug("heap info free " + free / KB + "K used " + used / KB + "K max " + max / KB + "K total " + rt.totalMemory()/KB + " K ");
  }
*/
}
