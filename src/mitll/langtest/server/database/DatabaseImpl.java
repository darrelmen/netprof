package mitll.langtest.server.database;

import mitll.flashcard.UserState;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.connection.DatabaseConnection;
import mitll.langtest.server.database.connection.H2Connection;
import mitll.langtest.server.database.custom.UserListManager;
import mitll.langtest.server.database.flashcard.UserStateWrapper;
import mitll.langtest.shared.DLIUser;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.Site;
import mitll.langtest.shared.User;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.flashcard.FlashcardResponse;
import mitll.langtest.shared.flashcard.ScoreInfo;
import mitll.langtest.shared.grade.CountAndGradeID;
import mitll.langtest.shared.grade.Grade;
import mitll.langtest.shared.grade.ResultsAndGrades;
import mitll.langtest.shared.monitoring.Session;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
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

  private static final boolean DO_SIMPLE_FLASHCARDS = true;
  private static final boolean DROP_USER = false;
  private static final boolean DROP_RESULT = false;
  private static final int MIN_INCORRECT_ANSWERS = 10;

  private String installPath;
  private ExerciseDAO exerciseDAO = null;
  private final UserDAO userDAO = new UserDAO(this);
  private final DLIUserDAO dliUserDAO = new DLIUserDAO(this);
  private final ResultDAO resultDAO = new ResultDAO(this,userDAO);
  private final AnswerDAO answerDAO = new AnswerDAO(this, resultDAO);
  private final GradeDAO gradeDAO = new GradeDAO(this,userDAO, resultDAO);
  private final SiteDAO siteDAO = new SiteDAO(this, userDAO);
  private final UserListManager userListManager = new UserListManager(userDAO);
  private UserExerciseDAO userExerciseDAO;

  private DatabaseConnection connection = null;
  private MonitoringSupport monitoringSupport;

  /**
   * TODO : consider making proper v2 database!
   */
  private String lessonPlanFile;
  private boolean isWordPairs;
  private boolean useFile;
  private boolean isFlashcard;
  private String language = "";
  private boolean doImages;
  private final String configDir;
  private final String absConfigDir;
  private String mediaDir;
  private ServerProperties serverProps;

  private final Map<Long,UserStateWrapper> userToState = new HashMap<Long,UserStateWrapper>();

  /**
   * Just for testing
   * @param configDir
   * @see mitll.langtest.server.LangTestDatabaseImpl#readProperties(javax.servlet.ServletContext)
   */

  public DatabaseImpl(String configDir, String dbName, String lessonPlanFile) {
    this(configDir, dbName, "", new ServerProperties());
    this.useFile = true;
    this.lessonPlanFile = lessonPlanFile;
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#makeDatabaseImpl
   * @param configDir
   * @param dbName
   * @param serverProps
   */
  public DatabaseImpl(String configDir, String dbName, String relativeConfigDir, ServerProperties serverProps) {
    connection = new H2Connection(configDir, dbName);
    absConfigDir = configDir;
    this.configDir = relativeConfigDir;

    this.isWordPairs = serverProps.isWordPairs();
    this.doImages = serverProps.doImages();
    this.language = serverProps.getLanguage();
    this.isFlashcard = serverProps.isFlashcard();
    this.serverProps = serverProps;

    try {
      if (getConnection() == null) {
        logger.warn("couldn't open connection to database at " + configDir + " : " + dbName);
        return;
      }
    } catch (Exception e) {
      logger.error("couldn't open connection to database, got " + e.getMessage(),e);
      return;
    }
    initializeDAOs();
    monitoringSupport = getMonitoringSupport();
  }

  /**
   * Create or alter tables as needed.
   */
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
   //   dliUserDAO.dropUserTable(this);
      dliUserDAO.createUserTable(this);

      siteDAO.createTable(getConnection());
      userExerciseDAO = new UserExerciseDAO(this);
      userListManager.setUserExerciseDAO(userExerciseDAO);
    } catch (Exception e) {
      logger.error("got " + e, e);  //To change body of catch statement use File | Settings | File Templates.
    }
  }

  @Override
  public Connection getConnection()/* throws Exception*/ {
    return connection.getConnection();
  }

  public void closeConnection(Connection connection) throws SQLException {}

  public Export getExport() {
    //if (exerciseDAO == null) logger.error("huh? exercise dao is null?");
    return new Export(exerciseDAO,resultDAO,gradeDAO);
  }

  public MonitoringSupport getMonitoringSupport() {
    return new MonitoringSupport(userDAO, resultDAO,gradeDAO);
  }

  /**
   * @see #getExercises(boolean, String)
   * @param useFile
   * @return
   */
  private ExerciseDAO makeExerciseDAO(boolean useFile) {
    return useFile ? new FileExerciseDAO(mediaDir, language, isFlashcard) : new SQLExerciseDAO(this, mediaDir, absConfigDir);
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#setInstallPath
   * @param installPath
   * @param lessonPlanFile
   * @param language
   * @param mediaDir
   */
  public void setInstallPath(String installPath, String lessonPlanFile, String language,
                             boolean useFile, String mediaDir) {
   // logger.debug("got install path " + installPath + " media " + mediaDir + " is urdu " +isUrdu);
    this.installPath = installPath;
    this.lessonPlanFile = lessonPlanFile;
    this.mediaDir = mediaDir;
    this.useFile = useFile;
    this.language = language;
  }

  public void setOutsideFile(String outsideFile) { monitoringSupport.setOutsideFile(outsideFile); }

  public SectionHelper getSectionHelper() {
    getExercises();
    return exerciseDAO.getSectionHelper();
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExercises
   * @return
   */
  public List<Exercise> getExercises() { return getExercises(useFile, lessonPlanFile); }

  public Exercise getExercise(String id) { return exerciseDAO.getExercise(id); }

  /**
   *
   *
   * @param useFile
   * @param lessonPlanFile
   * @return
   * @see #getExercises()
   */
  private List<Exercise> getExercises(boolean useFile, String lessonPlanFile) {
    if (lessonPlanFile == null) {
      logger.error("huh? lesson plan file is null???", new Exception());
      return Collections.emptyList();
    }
    boolean isExcel = lessonPlanFile.endsWith(".xlsx");
    makeDAO(useFile, lessonPlanFile, isExcel, mediaDir);

    if (useFile && !isExcel) {
      if (isWordPairs) {
        ((FileExerciseDAO) exerciseDAO).readWordPairs(lessonPlanFile, language, doImages);
      }
      else {
        ((FileExerciseDAO) exerciseDAO).readFastAndSlowExercises(installPath, configDir, lessonPlanFile);
      }
    }
    List<Exercise> rawExercises = exerciseDAO.getRawExercises();
    if (rawExercises.isEmpty()) {
      logger.warn("no exercises for useFile = " + useFile + " and " + lessonPlanFile + " at " + installPath);
    }
    return rawExercises;
  }

  /**
   * @see #getExercises(boolean, String)
   * @param useFile
   * @param lessonPlanFile
   * @param excel
   * @param mediaDir
   */
  private void makeDAO(boolean useFile, String lessonPlanFile, boolean excel, String mediaDir) {
    if (exerciseDAO == null) {
      if (useFile && excel) {
        synchronized (this) {
          this.exerciseDAO = new ExcelImport(lessonPlanFile, mediaDir, absConfigDir, serverProps);
        }
      }
      else {
        this.exerciseDAO = makeExerciseDAO(useFile);
      }
    }
  }

  /**
   * TODO : consider how to make this faster, not have split between 1 and more than 1 case
   *
   * @param activeExercises
   * @param expectedCount
   * @param filterResults
   * @param useFLQ
   * @param useSpoken
   * @param englishOnly
   * @return
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
   * @param useFLQ
   * @param useSpoken
   * @return next exercise containing ungraded results
   */
  private Exercise getNextUngradedExerciseQuick(Collection<String> activeExercises, int expectedCount,
                                                boolean filterResults, boolean useFLQ, boolean useSpoken) {
    long then = System.currentTimeMillis();
    long start = then;

    List<Exercise> rawExercises = getExercises();
    long now = System.currentTimeMillis();
    if (now-then > 100) logger.debug("getNextUngradedExerciseQuick took " +(now-then) + " to get exercises");

    then = System.currentTimeMillis();
    Collection<Result> resultExcludingExercises = resultDAO.getResultExcludingExercises(activeExercises);
    now = System.currentTimeMillis();
    if (now-then > 100) logger.debug("getNextUngradedExerciseQuick took " +(now-then) + " to get results");

    then = System.currentTimeMillis();
    GradeDAO.GradesAndIDs allGradesExcluding = gradeDAO.getAllGradesExcluding(activeExercises);
    now = System.currentTimeMillis();

    if (now-then > 100) logger.debug("getNextUngradedExerciseQuick took " +(now-then) + " to get grades");

    Map<Integer, Integer> idToCount = getResultIdToGradeCount(expectedCount, allGradesExcluding);
/*    logger.info("getNextUngradedExerciseQuick found " + resultExcludingExercises.size() + " results, " +
      "expected count = " + expectedCount + ", " +
      allGradesExcluding.resultIDs.size() + " graded results, filter results = " + filterResults +
      " use flq " + useFLQ + " spoken " + useSpoken);*/

    // remove results that have grades...
    //int skipped = 0;
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
        //skipped++;
      }
      else if (numGrades != null) {
        logger.warn("\tfound grade " + numGrades + " for " +result +"?");
      }
    }

/*    logger.debug("getNextUngradedExercise after removing graded, there were " + resultExcludingExercises.size() + " results" +//);
      ", skipped " + skipped);*/

    // whatever remains, find first exercise
    if (resultExcludingExercises.isEmpty()) {
      logger.debug("all results have been graded.");
      return null;
    }
    else {
      //  logger.debug("getNextUngradedExercise candidates are   " + exids);
      SortedSet<String> exids = new TreeSet<String>(); // sort by id
      for (Result r : resultExcludingExercises) {
        exids.add(r.id);
      }

      int skipped2 = 0;
      for (String candidate : exids) {
        Exercise exerciseForID = getExercise(candidate);
        if (exerciseForID != null) {
          now = System.currentTimeMillis();
          if (skipped2 > 0) {
            logger.debug("getNextUngradedExerciseQuick note : skipped " + skipped2 + " exercises...");
          }
          logger.debug("getNextUngradedExerciseQuick : took " +(now-start) + " millis to get next ungraded exid : " +exerciseForID);
          return exerciseForID;
        }
        else {
          skipped2++;
        }
      }
      if (!rawExercises.isEmpty()) {
        logger.error("getNextUngradedExerciseQuick expecting an exercise to match any of " + exids.size() +
          " (e.g." + exids.iterator().next()+
          ") candidates in " + rawExercises.size() + " exercises.");
      }
    }

    return null;
  }

  /**
   * @see #getNextUngradedExerciseQuick(java.util.Collection, int, boolean, boolean, boolean)
   * @param expectedCount
   * @param allGradesExcluding
   * @return
   */
  private Map<Integer, Integer> getResultIdToGradeCount(int expectedCount, GradeDAO.GradesAndIDs allGradesExcluding) {
    Map<Integer, Integer> idToCount = new HashMap<Integer, Integer>();
   // int atExpected = 0;
    for (Grade g : allGradesExcluding.grades) {
      if (g.gradeIndex == expectedCount - 1 && g.grade != Grade.UNASSIGNED) {
   //     atExpected++;
        if (!idToCount.containsKey(g.resultID)) {
          idToCount.put(g.resultID, 1);
        } else {
          idToCount.put(g.resultID, idToCount.get(g.resultID) + 1);
        }
      }
    }
/*    logger.warn("found " + atExpected + " grades at " + expectedCount +
      " out of " + allGradesExcluding.grades.size() + " returning map of " +idToCount.size() + " results to count");*/

    return idToCount;
  }

  /**
   * remember state for user so they can resume their flashcard exercise from that point.
   * synchronize!
   *
   * @see mitll.langtest.server.LangTestDatabaseImpl#getNextExercise
   * @param userID
   * @param isTimedGame
   * @param getNext
   * @return
   */
  public FlashcardResponse getNextExercise(List<Exercise> exercises, long userID, boolean isTimedGame, boolean getNext) {
    return getFlashcardResponse(userID, isTimedGame, exercises, getNext);
  }

  public FlashcardResponse getNextExercise(long userID, boolean isTimedGame, boolean getNext) {
    List<Exercise> exercises = getExercises(useFile, lessonPlanFile);
    return getFlashcardResponse(userID, isTimedGame, exercises, getNext);
  }

  private FlashcardResponse getFlashcardResponse(long userID, boolean isTimedGame, List<Exercise> exercises,
                                                 boolean getNext) {
    UserStateWrapper userStateWrapper = createOrGetUserState(userID, exercises);

    if (isTimedGame || DO_SIMPLE_FLASHCARDS) {
      FlashcardResponse flashcardResponse;
      if (userStateWrapper.isComplete()) {
        userStateWrapper.shuffle();
      }
      Exercise nextExercise = getNext ? userStateWrapper.getNextExercise() : userStateWrapper.getPrevExercise();

      boolean onFirst = userStateWrapper.onFirst();
      boolean onLast = userStateWrapper.onLast();

      flashcardResponse =
        new FlashcardResponse(nextExercise,
          userStateWrapper.getCorrect(),
          userStateWrapper.getIncorrect(), onFirst, onLast);

      logger.debug("getFlashcardResponse returning " + flashcardResponse);
      return flashcardResponse;
    }
    else {
      Map<String, Exercise> idToExercise = new HashMap<String, Exercise>();
      for (Exercise e : exercises) idToExercise.put(e.getID(), e);
      return getFlashcardResponse(idToExercise, userStateWrapper);
    }
  }

  private UserStateWrapper createOrGetUserState(long userID, List<Exercise> exercises) {
    UserStateWrapper userStateWrapper;

    synchronized (userToState) {
      userStateWrapper = userToState.get(userID);
      logger.info("createOrGetUserState : for user " + userID +
        " exercises has " + exercises.size() + " user state " + userStateWrapper);
      if (userStateWrapper == null || (!exercises.isEmpty() && userStateWrapper.getNumExercises() != exercises.size())) {
        userStateWrapper = getUserStateWrapper(userID, exercises);
        userToState.put(userID, userStateWrapper);
      }
      else if (!exercises.isEmpty()) {
        logger.debug("user state " + userStateWrapper.getNumExercises() + " vs " + exercises.size() + " now " + userStateWrapper);
      }
    }
    return userStateWrapper;
  }

  private Map<Long, Integer> userToCorrect = new HashMap<Long, Integer>();

  public ScoreInfo getScoreInfo(long userID, long timeTaken, Map<String, Collection<String>> selection) {
    UserStateWrapper userStateWrapper = userToState.get(userID);
    int incorrect = userStateWrapper.getPincorrect();

    int diffI = Math.max(0, userStateWrapper.getIncorrect() - incorrect);
    logger.debug("getScoreInfo : diff  " + userToCorrect.get(userID) + " inc " + diffI);

    ScoreInfo scoreInfo = new ScoreInfo(userID, -1, userToCorrect.get(userID), 0, timeTaken, selection);
    userStateWrapper.setPincorrect(userStateWrapper.getIncorrect());

    userToCorrect.put(userID, 0);
    return scoreInfo;
  }

  /**
   * @see #createOrGetUserState(long, java.util.List)
   * @param userID
   * @param exercises
   * @return
   */
  private UserStateWrapper getUserStateWrapper(long userID, List<Exercise> exercises) {
    UserStateWrapper userStateWrapper;
    String[] strings = new String[0];
    if (exercises != null) {
      strings = new String[exercises.size()];
      int i = 0;
      for (Exercise e : exercises) {
        strings[i++] = e.getID();
      }
    }

    UserState userState = new UserState(strings);
/*    if (userState.finished()) {
      logger.info("-------------- user " + userID + " is finished ---------------- ");
    }*/
    logger.debug("getUserStateWrapper : making user state for " + userID + " with " + strings.length + " exercises");
    userStateWrapper = new UserStateWrapper(userState, userID, exercises);
    List<ResultDAO.SimpleResult> resultsForUser = resultDAO.getResultsForUser(userID);
    //logger.debug("getUserStateWrapper : found existing " + resultsForUser.size() + " results");

    for (ResultDAO.SimpleResult result : resultsForUser) {
      userStateWrapper.addCompleted(result.id);
    }
    logger.debug("getUserStateWrapper : after found existing " + userStateWrapper.getCompleted().size() + " completed.");

    return userStateWrapper;
  }

  private FlashcardResponse getFlashcardResponse(Map<String, Exercise> idToExercise, UserStateWrapper userState) {
    try {
      UserState state = userState.state;
      if (state.finished()) {
        return new FlashcardResponse(true, userState.getCorrect(), userState.getIncorrect());
      }
      else {
        Exercise exercise = idToExercise.get(state.next());
        return new FlashcardResponse(exercise,
          userState.getCorrect(),
          userState.getIncorrect(), false, false);
      }
    } catch (Exception e) {
      return new FlashcardResponse(true,
        userState.getCorrect(),
        userState.getIncorrect());
    }
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#writeAudioFile
   * @see mitll.langtest.server.audio.AudioFileHelper#getAudioAnswer(String, int, int, int, java.io.File, mitll.langtest.server.audio.AudioCheck.ValidityAndDur, String, boolean, mitll.langtest.client.LangTestDatabase)
   * @param userID
   * @param exerciseID
   * @param isCorrect
   */
  public UserStateWrapper updateFlashcardState(long userID, String exerciseID, boolean isCorrect) {
    if (isCorrect) {
      Integer integer = userToCorrect.get(userID);
      userToCorrect.put(userID, integer == null ? 1 : integer + 1);
    }
    UserStateWrapper state = null;
    synchronized (userToState) {
      try {
        state = userToState.get(userID);
        if (state == null) {
          logger.error("can't find state for user id " + userID);
        } else {
          state.state.update(exerciseID, isCorrect);
          if (isCorrect) {
            state.setCorrect(state.getCorrect() + 1);
          }
          else {
            state.setIncorrect(state.getIncorrect() + 1);
          }
        }
      } catch (Exception e) {
        logger.error("got " + e + " from updating "+ userID + " with " + exerciseID);
        userToState.remove(userID);
      }
    }
    return state;
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#resetUserState(long)
   * @param userID
   */
  public void resetUserState(long userID) {
    synchronized (userToState) {
      UserStateWrapper userStateWrapper = userToState.get(userID);
      logger.debug("resetUserState for " + userID);
      userStateWrapper.reset();     // remember past state
    }
  }

  public void clearUserState(long userID) {
    synchronized (userToState) {
      userToState.remove(userID);
    }
  }

  /**
   *
   *
   * @return unmodifiable list of exercises
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExercisesInModeDependentOrder(long)
   */
  public List<Exercise> getUnmodExercises() {
    List<Exercise> exercises = getExercises(useFile, lessonPlanFile);
    return Collections.unmodifiableList(exercises);
  }

  /**
   * Show unanswered questions first, then ones with 1, then 2, then 3,... answers
   * Also be aware of the user's gender -- if you're female, show questions that have no female answers first.
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExercisesInModeDependentOrder(long)
   * @see #getNextExercise(long, boolean, boolean)
   * @param userID so we can show gender aware orderings (i.e. show entries with fewer female responses to females, etc.)
   * @return ordered list of exercises
   */
  public List<Exercise> getExercisesBiasTowardsUnanswered(long userID, boolean useWeights) {
    List<Exercise> rawExercises = getExercises();
    return getExercisesBiasTowardsUnanswered(userID, rawExercises, useWeights);
  }

  /**
   *
   * @param userID
   * @param rawExercises
   * @param useWeights
   * @return
   */
  public List<Exercise> getExercisesBiasTowardsUnanswered(long userID, Collection<Exercise> rawExercises, boolean useWeights) {
    Map<String, Exercise> idToExercise = new HashMap<String, Exercise>();
    Map<String,Integer> idToCount = new HashMap<String, Integer>();
    Map<String,Double> idToWeight = new HashMap<String, Double>();

    populateInitialExerciseIDToCount(rawExercises, idToExercise, idToCount,idToWeight, useWeights);

    // only find answers that are for the gender
    List<ResultDAO.SimpleResult> results = getSimpleResults();

    getExerciseIDToResultCount(userID, idToCount,results);

    logger.debug("getExercisesBiasTowardsUnanswered for " + userID+ " id->count " + idToCount.size());
    //logger.debug("count " +idToCount.get())

    Map<String, Integer> idToCountScaled = getScaledIdToCount(idToCount, idToWeight);

  //  logger.debug("getExercisesBiasTowardsUnanswered idToCountScaled " + idToCountScaled);

    SortedMap<Integer, List<String>> countToIds = getCountToExerciseIDs(idToCountScaled);
  //  List<String> sampleIDs = countToIds.get(countToIds.firstKey());
    //logger.debug("getExercisesBiasTowardsUnanswered count keys " + countToIds.keySet() + " first set sample " + sampleIDs.subList(0, Math.min(sampleIDs.size(),20)));
    /*logger.debug("getExercisesBiasTowardsUnanswered count keys " + countToIds.keySet() + " first set sample (" +
      countToIds.firstKey() +
      ") = " + sampleIDs);*/

    //logger.debug("getExercisesBiasTowardsUnanswered map " + countToIds);
    return getResultsRandomizedPerUser(userID, idToExercise, countToIds, idToWeight);
  }

  /**
   * Use the weight on each exercise to bias how many times they are presented.
   * We divide the current raw count by log(weight).  An item with weight 100 will get recorded twice as often
   * as one with weight 10...
   * @param idToCount
   * @param idToWeight
   * @return map of scaled counts
   */
  private Map<String, Integer> getScaledIdToCount(Map<String, Integer> idToCount, Map<String, Double> idToWeight) {
    Map<String,Integer> idToCountScaled = new HashMap<String, Integer>();

    for (Map.Entry<String,Integer> idAndCount : idToCount.entrySet()) {
      String id = idAndCount.getKey();
      double doubleCount = (double) idAndCount.getValue();
      Double weight = idToWeight.get(id);
      int round = (int)Math.round(doubleCount / weight);
     // if (round != idAndCount.getValue()) logger.debug("different " + round + " vs " + idAndCount.getValue() + " for " + id);
      idToCountScaled.put(id, round);
    }
    return idToCountScaled;
  }

  /**
   * Merge the online counts with counts from an external file.
   *
   * A one-off thing only.
   *
   * @see mitll.langtest.server.LangTestDatabaseImpl#useOutsideResultCounts(long)
   * @param userID
   * @param outsideFile
   * @param useWeights
   * @return
   */
  public List<Exercise> getExercisesBiasTowardsUnanswered(long userID, String outsideFile, boolean useWeights) {
    Map<String,Integer> idToCount = new HashMap<String, Integer>();
    Map<String, Exercise> idToExercise = new HashMap<String, Exercise>();
    Map<String,Double> idToWeight = new HashMap<String, Double>();
    List<Exercise> rawExercises = getExercises();

    populateInitialExerciseIDToCount(rawExercises, idToExercise, idToCount,idToWeight,useWeights);
    //logger.info("initial map of online counts is size = " + idToCount.size() +" " + idToCount.values().size());

    boolean isMale = userDAO.isUserMale(userID);

    Map<String, Integer> idToCountOutside =
        new OutsideCount().getExerciseIDToOutsideCount(isMale, outsideFile, getExercises());

    //  logger.info("map of outside counts is size = " + idToCountOutside.size() +" " + idToCountOutside.values().size());
    for (Map.Entry<String, Integer> pair : idToCountOutside.entrySet()) {
      Integer count = idToCount.get(pair.getKey());
      if (count == null) logger.warn("missing exercise id " + pair.getKey());
      else idToCount.put(pair.getKey(),count+pair.getValue());
    }

    // only find answers that are for the gender
    List<ResultDAO.SimpleResult> results = getSimpleResults();

    getExerciseIDToResultCount(userID, idToCount,results);

    // now make a map of count at this number to exercise ids for these numbers
    SortedMap<Integer, List<String>> countToIds = getCountToExerciseIDs(idToCount);

    return getResultsRandomizedPerUser(userID, idToExercise, countToIds,idToWeight);
  }

  /**
   * Given a map of answer counts to exercise ids at those counts, randomize the order based on the
   * user id, then return a list of Exercises with those ids.
   *
   * @see #getExercisesBiasTowardsUnanswered(long,boolean)
   * @see #getExercisesBiasTowardsUnanswered(long, String, boolean)
   * @param userID for this user
   * @param idToExercise so we can go from id to exercise
   * @param countToIds statistics about answers for each exercise
   * @return List of exercises in order from least answers to most
   */
  private List<Exercise> getResultsRandomizedPerUser(long userID,
                                                     Map<String, Exercise> idToExercise,
                                                     SortedMap<Integer, List<String>> countToIds,
                                                     Map<String, Double> idToWeight) {
    List<Exercise> result = new ArrayList<Exercise>();
    Random rnd = new Random(userID);

    //int count2 = 0;
    for (Map.Entry<Integer, List<String>> pair : countToIds.entrySet()) {
      //Integer countOrig = pair.getKey();
      List<String> itemsAtCount = pair.getValue();
      //logger.debug("doing items at result count = " + countOrig + " : " + itemsAtCount.size());

      // take each exercise and make a second map of exercise to int weight, reverse it, and shuffle each value
      Map<String,Integer> exToWeight = new HashMap<String, Integer>();
      for (String exid : itemsAtCount) {
        int round = (int) Math.round(idToWeight.get(exid));
       // if (count2++ < 20) logger.debug("weight for " +exid + " is " +round);
        exToWeight.put(exid, round);
      }
      SortedMap<Integer, List<String>> countToIds2 = getCountToExerciseIDs(exToWeight);
      List<Integer> counts = new ArrayList<Integer>(countToIds2.keySet());
      Collections.reverse(counts); // 9,4,3,2,1 -> 1,2,3,4,9

      for (Integer count : counts) {
        List<String> itemsAtCount2 = countToIds2.get(count);
       // logger.debug("doing items at weight count = " + count + " : " + itemsAtCount2.size());
        Collections.shuffle(itemsAtCount2, rnd);
        for (String id : itemsAtCount2) {
          Exercise e = idToExercise.get(id);
          if (e == null) logger.error("huh? couldn't find exercise " + id);
          else result.add(e);
        }
      }
    }
    return result;
  }

  /**
   * Reverse the map -- make a map of result count->list of ids at that count
   * @see #getExercisesBiasTowardsUnanswered(long,boolean)
   * @see #getExercisesBiasTowardsUnanswered(long, String, boolean)
   * @param idToCount
   * @return
   */
  private SortedMap<Integer, List<String>> getCountToExerciseIDs(Map<String, Integer> idToCount) {
    SortedMap<Integer,List<String>> countToIds = new TreeMap<Integer, List<String>>();
    for (Map.Entry<String,Integer> pair : idToCount.entrySet()) {
      Integer countAtID = pair.getValue();
      List<String> idsAtCount = countToIds.get(countAtID);
      if (idsAtCount == null) { countToIds.put(countAtID, idsAtCount = new ArrayList<String>()); }
      idsAtCount.add(pair.getKey());
    }
    return countToIds;
  }

  /**
   * @see #getExercisesBiasTowardsUnanswered(long,boolean)
   * @see #getExercisesBiasTowardsUnanswered(long, String, boolean)
   * @param idToExercise
   * @param idToCount
   */
  private void populateInitialExerciseIDToCount(Collection<Exercise> rawExercises,
                                                Map<String, Exercise> idToExercise, Map<String, Integer> idToCount,
                                                Map<String, Double> idToWeight, boolean useWeights) {
    for (Exercise e : rawExercises) {
      idToCount.put(e.getID(), 0);
      double weight = !useWeights || e.getWeight() == 0 ? 1 : Math.max(1, Math.log(e.getWeight())); // 1->n
      idToWeight.put(e.getID(), weight);
      idToExercise.put(e.getID(), e);
    }
  }

  /**
   * multiple responses by the same user count as one in count->id map.
   *
   * @see #getExercisesBiasTowardsUnanswered(long,boolean)
   * @see #getExercisesBiasTowardsUnanswered(long, String, boolean)
   * @param userID
   * @paramx userMale
   * @param idToCount exercise id->count
   */
  private Collection<String> getExerciseIDToResultCount(long userID, Map<String, Integer> idToCount, List<ResultDAO.SimpleResult> results) {
    List<User> users = userDAO.getUsers();

    boolean userMale = userDAO.isUserMale(userID, users);
    Map<Long, User> userMap = userDAO.getUserMap(userMale, users);
    return getExerciseIDToResultCount(userID, userMap, results, idToCount);
  }

  private Collection<String> getExerciseIDToResultCount(long userID, Map<Long, User> userMap, List<ResultDAO.SimpleResult> results,
                                                        Map<String, Integer> idToCount // what gets populated
  ) {
    List<String> alreadyAnsweredByThisUser = new ArrayList<String>();
    Map<String, Set<Long>> keyToUsers = new HashMap<String, Set<Long>>();

    for (ResultDAO.SimpleResult r : results) {
      Integer current = idToCount.get(r.id);
      if (current != null) {  // unlikely not null
        if (userMap.containsKey(r.userid)) { // only get male or female results
          String key = r.id;
          Set<Long> usersForResult = keyToUsers.get(key);

          if (usersForResult == null) {
            keyToUsers.put(key, usersForResult = new HashSet<Long>());
          }
          if (!usersForResult.contains(r.userid)) {   // ignore re-recordings
            usersForResult.add(r.userid);
            Integer c = idToCount.get(key);
            if (c == null) {
              logger.warn("huh? key " + key + " not found?");
              idToCount.put(key, 1);
            } else {
              idToCount.put(key, c + 1);
            }
          }
        }
        if (r.userid == userID) {
          alreadyAnsweredByThisUser.add(r.id);
          //logger.debug("user " + userID + " has already answered exercise " + r.id + " at result " + r.uniqueID);
        }
      }
    }
    return alreadyAnsweredByThisUser;
  }

  /**
   * Return exercises in an order that puts the items with all right answers towards the front and all wrong
   * toward the back (in the arabic data collect the students were too advanced for the questions, so
   * they mostly got them right).<br></br>
   *
   * Remember there can be multiple questions per exercise, so we need to average over the grades for all
   * answers for all questions for an exercise.
   *
   * Complicated... for those items that have few answers, put those at the front so we get even coverage.
   * For those items that have a decent number of answers, sort them by grade putting all correct items toward the
   * front.
   *
   * @param userID
   * @return
   */
  public List<Exercise> getExercisesGradeBalancing(long userID) {
    logger.debug("getExercisesGradeBalancing " +userID);
    long start = System.currentTimeMillis();
    long then = System.currentTimeMillis();

    Map<String, Exercise> idToExercise = new HashMap<String, Exercise>();
    Map<String,Integer> idToCount = new HashMap<String, Integer>();
    Map<String,Double> idToWeight = new HashMap<String, Double>();

    List<Exercise> rawExercises = getExercises();
    long now = System.currentTimeMillis();

    if (now-then > 100) logger.debug("getExercisesGradeBalancing took " +(now-then) + " to get exercises");

    populateInitialExerciseIDToCount(rawExercises, idToExercise, idToCount, idToWeight, false);

    // only find answers that are for the gender
    then = System.currentTimeMillis();
    List<ResultDAO.SimpleResult> results = getSimpleResults();
    now = System.currentTimeMillis();

    if (now-then > 100) logger.debug("getExercisesGradeBalancing took " +(now-then) + " to get results");

    then = System.currentTimeMillis();
    getExerciseIDToResultCount(userID, idToCount,results);
    now = System.currentTimeMillis();
    if (now-then > 100) logger.debug("getExercisesGradeBalancing took " +(now-then) + " to get users");

    // now make a map of count at this number to exercise ids for these numbers
    SortedMap<Integer, List<String>> countToIds = getCountToExerciseIDs(idToCount);
    SortedMap<Integer, List<String>> integerListSortedMap = countToIds.subMap(0, MIN_INCORRECT_ANSWERS);
    // logger.debug("num with less than ten are " + integerListSortedMap);

    List<Exercise> fewResponses = getResultsRandomizedPerUser(userID, idToExercise, integerListSortedMap, idToWeight);
    Set<String> fewSet = new HashSet<String>();
    for (Exercise e : fewResponses) fewSet.add(e.getID());
   // logger.debug("getExercisesGradeBalancing for " + MIN_INCORRECT_ANSWERS + " fewSet size " + fewSet.size());
    if (fewSet.size() == rawExercises.size()) {
      logger.debug("we haven't gotten enough questions answered yet to bias towards easy questions");
      return fewResponses;
    } else {
      // join results with grades
      then = System.currentTimeMillis();

      Map<Integer, List<Grade>> idToGrade = gradeDAO.getIdToGrade();
      now = System.currentTimeMillis();
      if (now-then > 100) logger.debug("getExercisesGradeBalancing took " +(now-then) + " to get grades");

      List<ResultAndGrade> rgs = getResultAndGrades(results, fewSet, idToGrade);

      // make a map of count of wrong answers -> list of results at that count
      Map<Integer,List<ResultAndGrade>> countToRGs = new TreeMap<Integer, List<ResultAndGrade>>();
      for (ResultAndGrade rg : rgs) {
        List<ResultAndGrade> resultAndGrades = countToRGs.get(rg.getNumWrong());
        if (resultAndGrades == null) countToRGs.put(rg.getNumWrong(), resultAndGrades = new ArrayList<ResultAndGrade>());
        resultAndGrades.add(rg);
      }
      List<Exercise> ret = new ArrayList<Exercise>(fewResponses);
      Random rnd = new Random(userID);

      // now at each count of wrong answers, shuffle for this user.
      for (List<ResultAndGrade> rgsAtCount : countToRGs.values()) {
        //logger.debug("result at " + rgsAtCount.iterator().next().getNumWrong() + " is " +rgsAtCount.size());
        Collections.shuffle(rgsAtCount, rnd);

        for (ResultAndGrade rg : rgsAtCount) {
          ret.add(idToExercise.get(rg.getResult().id));
        }
      }
      if (ret.size() != rawExercises.size()) {
        logger.error("huh? returning only " + ret.size() + " exercises, expecting " + rawExercises.size());
      }

      now = System.currentTimeMillis();
      logger.debug("took " +(now-start) + " millis to get exercise list for " +userID);
      return ret;
    }
  }

  private List<ResultAndGrade> getResultAndGrades(List<ResultDAO.SimpleResult> results, Set<String> fewSet, Map<Integer, List<Grade>> idToGrade) {
    Map<String, ResultAndGrade> exidToRG = new HashMap<String, ResultAndGrade>();

    for (ResultDAO.SimpleResult r : results) {
    //  if (r.flq == useFLQ && r.spoken == useSpoken) {
        if (!fewSet.contains(r.id)) {
          //    logger.debug("Skipping " + r.id);
     //   } else {
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
      //}
    }

    return new ArrayList<ResultAndGrade>(exidToRG.values());
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExercisesInModeDependentOrder(long)
   * @param userID
   * @param firstNInOrder
   * @return
   */
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

  public long addUser(HttpServletRequest request, int age, String gender, int experience, String dialect) {
    String ip = getIPInfo(request);
    return addUser(age, gender, experience, ip,dialect);
  }

  /**
   * Somehow on subsequent runs, the ids skip by 30 or so?
   * <p/>
   * Uses return generated keys to get the user id
   *
   * @param age
   * @param gender
   * @param experience
   * @param ipAddr user agent info
   * @param dialect speaker dialect
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#addUser
   */
  private long addUser(int age, String gender, int experience, String ipAddr, String dialect) {
    return userDAO.addUser(age, gender, experience, ipAddr, "", dialect, "", false);
  }

  public long addUser(HttpServletRequest request,
                      int age, String gender, int experience,
                      String nativeLang, String dialect, String userID) {
    String ip = getIPInfo(request);
    return addUser(age, gender, experience, ip, nativeLang, dialect, userID);
  }

  private String getIPInfo(HttpServletRequest request) {
    String header = request.getHeader("User-Agent");
    SimpleDateFormat sdf = new SimpleDateFormat();
    String format = sdf.format(new Date());
    return request.getRemoteHost() +/*"/"+ request.getRemoteAddr()+*/(header != null ? "/" + header : "") + " at " + format;
  }

  private long addUser(int age, String gender, int experience, String ipAddr,
                       String nativeLang, String dialect, String userID) {
    return userDAO.addUser(age, gender, experience, ipAddr, nativeLang, dialect, userID, false);
  }

  /**
   * Adds some sugar -- sets the answers and rate per user, and joins with dli experience data
   *
   * @see mitll.langtest.server.LangTestDatabaseImpl#getUsers()
   * @return
   */
  public List<User> getUsers() {
    Map<Long, Float> userToRate = resultDAO.getSessions().userToRate;
    Map<Long, Integer> idToCount = populateUserToNumAnswers();
    List<User> users = userDAO.getUsers();

    for (User u : users) {
      Integer numResults = idToCount.get(u.id);
      if (numResults != null) {
        u.setNumResults(numResults);

        if (userToRate.containsKey(u.id)) {
          u.setRate(userToRate.get(u.id));
        }
      }
    }

    joinWithDLIUsers(users);
    return users;
  }

  private Map<Long, Integer> populateUserToNumAnswers() {
    Map<Long,Integer> idToCount = new HashMap<Long, Integer>();
    for (Result r : resultDAO.getResults()) {
      Integer count = idToCount.get(r.userid);
      if (count == null) idToCount.put(r.userid, 1);
      else idToCount.put(r.userid, count+1);
    }
    return idToCount;
  }

  private void joinWithDLIUsers(List<User> users) {
    List<DLIUser> users1 = dliUserDAO.getUsers();
    Map<Long, User> userMap = userDAO.getMap(users);

    for (DLIUser dliUser : users1) {
      User user = userMap.get(dliUser.getUserID());
      if (user != null) {
        user.setDemographics(dliUser);
      }
    }
    if (users1.isEmpty()) logger.info("no dli users.");
  }

  /**
   * Pulls the list of results out of the database.
   *
   * @return
   * @see mitll.langtest.server.audio.SplitAudio#getIDToResultsMap(DatabaseImpl, java.util.Set)
   */
  public List<Result> getResults() { return resultDAO.getResults(); }
  private List<ResultDAO.SimpleResult> getSimpleResults() { return resultDAO.getSimpleResults(); }

  public List<Result> getResultsWithGrades() {
    List<Result> results = resultDAO.getResults();
    Map<Integer,Result> idToResult = new HashMap<Integer, Result>();
    for (Result r : results) idToResult.put(r.uniqueID, r);
    for (Grade g : gradeDAO.getGrades()) {
      Result result = idToResult.get(g.resultID);
      result.addGrade(g);
    }
    return results;
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
    logger.debug("for exid " + exid + " got " +resultsForExercise.size() + " results and " + gradesAndIDs.grades.size() + " grades");
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

        if (!spoken && r.answer.endsWith(".wav")) { // recover from badly marked results
          spoken = true;
          if (r.audioType.equals(Result.AUDIO_TYPE_UNSET)) {
            flq = true;
          }
        }
        Map<Boolean, List<Result>> langToResult = spokenToLangToResult.get(spoken);
        if (langToResult == null)
          spokenToLangToResult.put(spoken, langToResult = new HashMap<Boolean, List<Result>>());
        List<Result> resultsForLang = langToResult.get(flq);
        if (resultsForLang == null) langToResult.put(flq, resultsForLang = new ArrayList<Result>());
        resultsForLang.add(r);
      }
    }

    logger.debug("for exid " + exid + " got " +resultsForExercise.size() + " results and " + gradesAndIDs.grades.size() + " grades and " + spokenToLangToResult.size());

    return new ResultsAndGrades(resultsForExercise, gradesAndIDs.grades, spokenToLangToResult);
  }

  /**
   * Creates the result table if it's not there.
   *
   *
   * @param userID
   * @param e
   * @param questionID
   * @param answer
   * @see mitll.langtest.server.LangTestDatabaseImpl#addTextAnswer(int, mitll.langtest.shared.Exercise, int, String)
   * @see mitll.langtest.client.exercise.PostAnswerProvider#postAnswers(mitll.langtest.client.exercise.ExerciseController, mitll.langtest.shared.Exercise)
   */
  public void addAnswer(int userID, Exercise e, int questionID, String answer) {
    addAnswer(userID, e, questionID, answer, true);
  }

  public AnswerDAO getAnswerDAO() { return answerDAO; }

  private void addAnswer(int userID, Exercise e, int questionID, String answer, boolean correct) {
    answerDAO.addAnswer(userID, e, questionID, answer, "", !e.isPromptInEnglish(), false, Result.AUDIO_TYPE_UNSET, correct, 0);
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#writeAudioFile(String, String, String, int, int, int, boolean, String, boolean)
   * @param userID
   * @param plan
   * @param exerciseID
   * @param questionID
   * @param audioFile
   * @param valid
   * @param flq
   * @param spoken
   * @param audioType
   * @param durationInMillis
   * @param correct
   * @param score
   */
  public long addAudioAnswer(int userID, String plan, String exerciseID, int questionID,
                             String audioFile,
                             boolean valid, boolean flq, boolean spoken,
                             String audioType, int durationInMillis, boolean correct, float score) {
    if (valid) addCompleted(userID, exerciseID);

    return answerDAO.addAnswer(this, userID, plan, exerciseID, questionID, "", audioFile, valid, flq, spoken, audioType,
      durationInMillis, correct, score, "");
  }

  public void addCompleted(int userID, String exerciseID) {
    List<Exercise> objects = Collections.emptyList();
    UserStateWrapper userStateWrapper = createOrGetUserState(userID, objects);
    userStateWrapper.addCompleted(exerciseID);
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getCompletedExercises(int)
   * @param userID
   * @return
   */
  public Set<String> getCompletedExercises(int userID) {
    List<Exercise> objects = Collections.emptyList();

    UserStateWrapper userStateWrapper = createOrGetUserState(userID, objects);
    return userStateWrapper.getCompleted();
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
   * @see mitll.langtest.server.LangTestDatabaseImpl#changeGrade(mitll.langtest.shared.grade.Grade)
   */
  public void changeGrade(Grade toChange) {  gradeDAO.changeGrade(toChange);  }
  public int userExists(String login) { return userDAO.userExists(login);  }

  /**
   * @see mitll.langtest.server.SiteDeployer#doSiteResponse(DatabaseImpl, javax.servlet.http.HttpServletResponse, mitll.langtest.server.SiteDeployer, mitll.langtest.shared.Site)
   * @param site
   * @return
   */
  public Site addSite(Site site) { return siteDAO.addSite(site);  }
  public boolean siteExists(Site site) { return siteDAO.getSiteWithName(site.name) != null;  }
  public Site getSiteByID(long id) { return siteDAO.getSiteByID(id); }
  public List<Site> getDeployedSites() { return siteDAO.getDeployedSites(); }
  public void deploy(Site site) { siteDAO.deploy(site); }

  /**
   * @see mitll.langtest.server.SiteDeployer#deploySite(DatabaseImpl, mitll.langtest.server.mail.MailSupport, javax.servlet.http.HttpServletRequest, String, String, long, String, String, String)
   * @param site
   * @param name
   * @param lang
   * @param notes
   * @return
   */
  public Site updateSite(Site site, String name, String lang, String notes) { return siteDAO.updateSite(site, name, lang, notes); }

  /**
   * @see mitll.langtest.server.SiteDeployer#updateExerciseFile(mitll.langtest.shared.Site, String, DatabaseImpl)
   * @param site
   */
  public void updateSiteFile(Site site) { siteDAO.updateSiteFileInDB(site); }

  /**
   * TODO : worry about duplicate userid?
   * @return map of user to number of answers the user entered
   */
  public Map<User, Integer> getUserToResultCount() { return monitoringSupport.getUserToResultCount(); }

  /**
   * Determine sessions per user.  If two consecutive items are more than {@link ResultDAO#SESSION_GAP} seconds
   * apart, then we've reached a session boundary.
   * Remove all sessions that have just one answer - must be test sessions.
   * @see mitll.langtest.server.LangTestDatabaseImpl#getSessions()
   * @return list of duration and numAnswer pairs
   */
  public List<Session> getSessions() { return monitoringSupport.getSessions().sessions; }

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
  public Map<String, Map<String, Integer>> getResultPerExercise() { return monitoringSupport.getResultPerExercise(getExercises()); }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getResultCountsByGender()
   * @return
   */
  public Map<String,Map<Integer,Integer>> getResultCountsByGender() {  return monitoringSupport.getResultCountsByGender(getExercises()); }
  public Map<String, Map<Integer, Map<Integer, Integer>>> getDesiredCounts() {  return monitoringSupport.getDesiredCounts(getExercises()); }
   /**
   * Return some statistics related to the hours of audio that have been collected
    * @see mitll.langtest.server.LangTestDatabaseImpl#getResultStats()
   * @return
   */
  public Map<String,Number> getResultStats() { return monitoringSupport.getResultStats(); }

  public Map<Integer, Map<String, Map<String,Integer>>> getGradeCountPerExercise() { return monitoringSupport.getGradeCountPerExercise(getExercises());}

  public void destroy() {
    try {
      connection.contextDestroyed();
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
  }
  public void addDLIUser(DLIUser dliUser) throws Exception { dliUserDAO.addUser(dliUser);  }

  public String toString() { return "Database : "+ connection.getConnection(); }

  public UserListManager getUserListManager() { return userListManager; }

  public Exercise getUserExerciseWhere(String id) {
    UserExercise where = userExerciseDAO.getWhere(id);
    return where != null ? where.toExercise(language) : null;
  }

/*  private static String getConfigDir(String language) {
    String installPath = ".";
    String dariConfig = File.separator +
      "war" +
      File.separator +
      "config" +
      File.separator +
      language +
      File.separator;
    return installPath + dariConfig;
  }*/

/*  public static void main(String [] arg) {

    String language = "pilot";
    final String configDir = getConfigDir(language);

    DatabaseImpl unitAndChapter = new DatabaseImpl(
      configDir,
      "arabicText",
      "");
    unitAndChapter.useFile =false;
    unitAndChapter.mediaDir = "config";
    unitAndChapter.getRandomBalancedList(0);
  }*/
}
