package mitll.langtest.server.database;

import mitll.flashcard.UserState;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.connection.DatabaseConnection;
import mitll.langtest.server.database.connection.H2Connection;
import mitll.langtest.server.database.custom.AddRemoveDAO;
import mitll.langtest.server.database.custom.AnnotationDAO;
import mitll.langtest.server.database.custom.ReviewedDAO;
import mitll.langtest.server.database.custom.UserExerciseDAO;
import mitll.langtest.server.database.custom.UserListDAO;
import mitll.langtest.server.database.custom.UserListExerciseJoinDAO;
import mitll.langtest.server.database.custom.UserListManager;
import mitll.langtest.server.database.flashcard.UserStateWrapper;
import mitll.langtest.server.database.instrumentation.EventDAO;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonUserExercise;
import mitll.langtest.shared.DLIUser;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.User;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.flashcard.AVPHistoryForList;
import mitll.langtest.shared.grade.CountAndGradeID;
import mitll.langtest.shared.grade.Grade;
import mitll.langtest.shared.grade.ResultsAndGrades;
import mitll.langtest.shared.instrumentation.Event;
import mitll.langtest.shared.monitoring.Session;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
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
  private static final Logger logger = Logger.getLogger(DatabaseImpl.class);

  private static final boolean DROP_USER = false;
  private static final boolean DROP_RESULT = false;

  private String installPath;
  private ExerciseDAO exerciseDAO = null;
  private UserDAO userDAO;
  private DLIUserDAO dliUserDAO;
  private ResultDAO resultDAO;
  private AnswerDAO answerDAO;
  private GradeDAO gradeDAO;
  private UserListManager userListManager;
  private UserExerciseDAO userExerciseDAO;
  private AddRemoveDAO addRemoveDAO;
  private EventDAO eventDAO;

  private DatabaseConnection connection = null;
  private MonitoringSupport monitoringSupport;

  private String lessonPlanFile;
  private final boolean isWordPairs;
  private boolean useFile;
  private final boolean isFlashcard;
  private String language = "";
  private final boolean doImages;
  private final String configDir;
  private final String absConfigDir;
  private String mediaDir;
  private final ServerProperties serverProps;

  private final Map<Long,UserStateWrapper> userToState = new HashMap<Long,UserStateWrapper>();
  private boolean addDefects = true;

  /**
   * Just for testing
   *
   * @param configDir
   * @param pathHelper
   * @see mitll.langtest.server.LangTestDatabaseImpl#readProperties(javax.servlet.ServletContext)
   */
  public DatabaseImpl(String configDir, String configFile, String dbName, PathHelper pathHelper, boolean mustAlreadyExist) {
    this(configDir, "", dbName, new ServerProperties(configDir, configFile), pathHelper, mustAlreadyExist);
    this.lessonPlanFile = serverProps.getLessonPlan();
    this.useFile = lessonPlanFile != null;
    addDefects = false;
  }

  public DatabaseImpl(String configDir, String relativeConfigDir, String dbName, ServerProperties serverProps,
                      PathHelper pathHelper, boolean mustAlreadyExist) {
    connection = new H2Connection(configDir, dbName, mustAlreadyExist);
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
    initializeDAOs(pathHelper);
    monitoringSupport = getMonitoringSupport();
  }

  /**
   * Create or alter tables as needed.
   */
  private void initializeDAOs(PathHelper pathHelper) {
    userDAO = new UserDAO(this);
    UserListDAO userListDAO = new UserListDAO(this, userDAO);
    addRemoveDAO = new AddRemoveDAO(this);

    userExerciseDAO = new UserExerciseDAO(this);
    UserListExerciseJoinDAO userListExerciseJoinDAO = new UserListExerciseJoinDAO(this);
    dliUserDAO = new DLIUserDAO(this);
    resultDAO = new ResultDAO(this,userDAO);
    answerDAO = new AnswerDAO(this, resultDAO);
    gradeDAO = new GradeDAO(this,userDAO, resultDAO);
    userListManager = new UserListManager(userDAO, userListDAO, userListExerciseJoinDAO,
      new AnnotationDAO(this, userDAO),
      new ReviewedDAO(this, ReviewedDAO.REVIEWED),
      new ReviewedDAO(this, ReviewedDAO.SECOND_STATE),
      pathHelper);
    eventDAO = new EventDAO(this);

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
      gradeDAO.createGradesTable(getConnection());
      userDAO.createUserTable(this);
      dliUserDAO.createUserTable(this);

     // getExercises();
      userListManager.setUserExerciseDAO(userExerciseDAO);
    } catch (Exception e) {
      logger.error("got " + e, e);  //To change body of catch statement use File | Settings | File Templates.
    }

    //if (getServerProps().isGoodwaveMode()) {
     // List<ResultDAO.SimpleResult> resultsThatNeedScore = resultDAO.getResultsThatNeedScore();
     // logger.info("results that need a score "  +resultsThatNeedScore.size());
    //}
  }

  public ResultDAO getResultDAO() { return resultDAO; }
  public UserDAO getUserDAO() { return userDAO; }

  @Override
  public Connection getConnection() { return connection.getConnection();  }

  /**
   * It seems like this isn't required?
   * @param connection
   * @throws SQLException
   */
  public void closeConnection(Connection connection) {}
  public void closeConnection() throws SQLException {

    Connection connection1 = connection.getConnection();
    if (connection1 != null) {
      connection1.close();
    }
  }

  public Export getExport() {
    return new Export(exerciseDAO,resultDAO,gradeDAO);
  }

  MonitoringSupport getMonitoringSupport() {
    return new MonitoringSupport(userDAO, resultDAO,gradeDAO);
  }

  /**
   * @see #getExercises(boolean, String)
   * @param useFile
   * @return
   */
  private ExerciseDAO makeExerciseDAO(boolean useFile) {
    return useFile ?
      new FileExerciseDAO(mediaDir, language, isFlashcard, absConfigDir, serverProps.getMappingFile()) :
      new SQLExerciseDAO(this, mediaDir, absConfigDir, serverProps);
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
    logger.debug("got install path " + installPath + " media " + mediaDir);
    this.installPath = installPath;
    this.lessonPlanFile = lessonPlanFile;
    this.mediaDir = mediaDir;
    this.useFile = useFile;
    this.language = language;
  }

  public SectionHelper getSectionHelper() {
    getExercises();
    return exerciseDAO.getSectionHelper();
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExercises
   * @return
   */
  public List<CommonExercise> getExercises() { return getExercises(useFile, lessonPlanFile); }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExercise(String)
   * @param id
   * @return
   */
  public CommonExercise getExercise(String id) { return exerciseDAO.getExercise(id); }

  /**
   *
   * @param useFile
   * @param lessonPlanFile
   * @return
   * @see #getExercises()
   */
  private List<CommonExercise> getExercises(boolean useFile, String lessonPlanFile) {
    if (lessonPlanFile == null) {
      logger.error("huh? lesson plan file is null???", new Exception());
      return Collections.emptyList();
    }
    //logger.debug("using lesson plan file " +lessonPlanFile);
    boolean isExcel = lessonPlanFile.endsWith(".xlsx");
    makeDAO(useFile, lessonPlanFile, isExcel, mediaDir, installPath);

    if (useFile && !isExcel) {
      if (isWordPairs) {
        ((FileExerciseDAO) exerciseDAO).readWordPairs(lessonPlanFile, doImages);
      }
      else {
        ((FileExerciseDAO) exerciseDAO).readFastAndSlowExercises(installPath, configDir, lessonPlanFile);
      }
    }
    List<CommonExercise> rawExercises = exerciseDAO.getRawExercises();
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
  private void makeDAO(boolean useFile, String lessonPlanFile, boolean excel, String mediaDir, String installPath) {
    if (exerciseDAO == null) {
      if (useFile && excel) {
        synchronized (this) {
          this.exerciseDAO = new ExcelImport(lessonPlanFile, mediaDir, absConfigDir, serverProps, userListManager, installPath, addDefects);
        }
      }
      else {
        this.exerciseDAO = makeExerciseDAO(useFile);
      }
      userExerciseDAO.setExerciseDAO(exerciseDAO);
      exerciseDAO.setUserExerciseDAO(userExerciseDAO);
      exerciseDAO.setAddRemoveDAO(addRemoveDAO);
    }
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#editItem(mitll.langtest.shared.custom.UserExercise)
   * @param userExercise
   */
  public void editItem(UserExercise userExercise) {
    getUserListManager().editItem(userExercise, true);
    exerciseDAO.addOverlay(userExercise);
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
  public CommonExercise getNextUngradedExercise(Collection<String> activeExercises, int expectedCount, boolean filterResults,
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
  private CommonExercise getNextUngradedExerciseSlow(Collection<String> activeExercises, int expectedCount, boolean englishOnly) {
    List<CommonExercise> rawExercises = getExercises();
    logger.info("getNextUngradedExerciseSlow : checking " + rawExercises.size() + " exercises.");
    for (CommonExercise e : rawExercises) {
      if (!activeExercises.contains(e.getID()) && // no one is working on it
          resultDAO.areAnyResultsLeftToGradeFor(e, expectedCount, englishOnly)) {
        //logger.info("CommonExercise " +e + " needs grading.");

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
  private CommonExercise getNextUngradedExerciseQuick(Collection<String> activeExercises, int expectedCount,
                                                boolean filterResults, boolean useFLQ, boolean useSpoken) {
    long then = System.currentTimeMillis();
    long start = then;

    List<CommonExercise> rawExercises = getExercises();
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
        CommonExercise exerciseForID = getExercise(candidate);
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

  private UserStateWrapper createOrGetUserState(long userID, List<CommonExercise> exercises) {
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

  /**
   * TODO : do all average calc on server!
   *
   * @see mitll.langtest.server.LangTestDatabaseImpl#getUserHistoryForList
   * @see mitll.langtest.client.custom.MyFlashcardExercisePanelFactory.StatsPracticePanel#onSetComplete
   * @paramx listid
   * @return
   */
  public List<AVPHistoryForList> getUserHistoryForList(long userid, Collection<String> ids, long latestResultID) {
    logger.debug("getUserHistoryForList " +userid + " and " + ids + " latest " + latestResultID);

    List<Session> sessionsForUserIn2 = resultDAO.getSessionsForUserIn2(ids, latestResultID);

    Map<Long, User> userMap = userDAO.getUserMap();

    AVPHistoryForList sessionAVPHistoryForList  = new AVPHistoryForList(sessionsForUserIn2, userid, true);
    AVPHistoryForList sessionAVPHistoryForList2 = new AVPHistoryForList(sessionsForUserIn2, userid, false);


    // sort by correct %
    Collections.sort(sessionsForUserIn2, new Comparator<Session>() {
      @Override
      public int compare(Session o1, Session o2) {
        return o1.getCorrectPercent() < o2.getCorrectPercent() ? +1 :o1.getCorrectPercent() > o2.getCorrectPercent() ? -1 :
          compareTimestamps(o1, o2);
      }
    });

    int count = 0;
    List<AVPHistoryForList.UserScore> scores = new ArrayList<AVPHistoryForList.UserScore>();

    for (Session session : sessionsForUserIn2) {
      if (count++ < 10 || session.isLatest()) {
        scores.add(makeScore(count, userMap, session, true));
     }
    }

    logger.debug("getUserHistoryForList correct scores " + scores);

    if (scores.size() == 11) {
      scores.remove(9);
    }
    sessionAVPHistoryForList.setScores(scores);

    Collections.sort(sessionsForUserIn2, new Comparator<Session>() {
      @Override
      public int compare(Session o1, Session o2) {
        return o1.getAvgScore() < o2.getAvgScore() ? +1 : o1.getAvgScore() > o2.getAvgScore() ? -1 : compareTimestamps(o1, o2);
      }
    });

    count = 0;
    scores = new ArrayList<AVPHistoryForList.UserScore>();

    for (Session session : sessionsForUserIn2) {
      if (count++ < 10 || session.isLatest()) {
        scores.add(makeScore(count, userMap, session, false));
      }
    }
    logger.debug("getUserHistoryForList pron   scores " + scores);

    if (scores.size() == 11) {
      scores.remove(9);
    }

    sessionAVPHistoryForList2.setScores(scores);

    List<AVPHistoryForList> historyForLists = new ArrayList<AVPHistoryForList>();
    historyForLists.add(sessionAVPHistoryForList);
    historyForLists.add(sessionAVPHistoryForList2);

    logger.debug("returning " +historyForLists);

    return historyForLists;
  }

  private int compareTimestamps(Session o1, Session o2) {
    return o1.getTimestamp() < o2.getTimestamp() ? +1 :o1.getTimestamp() > o2.getTimestamp() ? -1 :0;
  }

  private AVPHistoryForList.UserScore makeScore(int count, Map<Long, User> userMap, Session session, boolean useCorrect) {
    float value = useCorrect ? session.getCorrectPercent() : 100f * session.getAvgScore();
    return new AVPHistoryForList.UserScore(count,
      userMap.get(session.getUserid()).userID,
      value,
      session.isLatest());
  }


  /**
   * @seex #getFlashcardResponse
   * @param userID
   * @param exercises
   * @return
   */
  private UserStateWrapper getUserStateWrapper(long userID, List<CommonExercise> exercises) {
    UserStateWrapper userStateWrapper;
    String[] strings = new String[0];
    if (exercises != null) {
      strings = new String[exercises.size()];
      int i = 0;
      for (CommonExercise e : exercises) {
        strings[i++] = e.getID();
      }
    }

    UserState userState = new UserState(strings);
/*    if (userState.finished()) {
      logger.info("-------------- user " + userID + " is finished ---------------- ");
    }*/
   // logger.debug("getUserStateWrapper : making user state for " + userID + " with " + strings.length + " exercises");
    userStateWrapper = new UserStateWrapper(userState, userID, exercises);
    List<ResultDAO.SimpleResult> resultsForUser = resultDAO.getResultsForUser(userID);
    //logger.debug("getUserStateWrapper : found existing " + resultsForUser.size() + " results");

    for (ResultDAO.SimpleResult result : resultsForUser) {
      userStateWrapper.addCompleted(result.id);
    }
    logger.debug("getUserStateWrapper : after found existing " + userStateWrapper.getCompleted().size() + " completed.");

    return userStateWrapper;
  }

  /**
   *
   *
   * @return unmodifiable list of exercises
   * @see mitll.langtest.server.LangTestDatabaseImpl#init
   */
  public List<CommonExercise> getUnmodExercises() {
    List<CommonExercise> exercises = getExercises(useFile, lessonPlanFile);
    return Collections.unmodifiableList(exercises);
  }

  public long addUser(HttpServletRequest request,
                      int age, String gender, int experience,
                      String nativeLang, String dialect, String userID) {
    String ip = getIPInfo(request);
    return addUser(age, gender, experience, ip, nativeLang, dialect, userID);
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
/*  private long addUser(int age, String gender, int experience, String ipAddr, String dialect) {
    long l = userDAO.addUser(age, gender, experience, ipAddr, "", dialect, "", false);
    userListManager.createFavorites(l);
    return l;
  }*/

  public long addUser(int age, String gender, int experience, String ipAddr,
                       String nativeLang, String dialect, String userID) {
    logger.debug("addUser " + userID);
    long l = userDAO.addUser(age, gender, experience, ipAddr, nativeLang, dialect, userID, false);
    userListManager.createFavorites(l);
    return l;
  }

  private String getIPInfo(HttpServletRequest request) {
    String header = request.getHeader("User-Agent");
    SimpleDateFormat sdf = new SimpleDateFormat();
    String format = sdf.format(new Date());
    return request.getRemoteHost() +/*"/"+ request.getRemoteAddr()+*/(header != null ? "/" + header : "") + " at " + format;
  }

  /**
   * Adds some sugar -- sets the answers and rate per user, and joins with dli experience data
   *
   * @see mitll.langtest.server.LangTestDatabaseImpl#getUsers()
   * @return
   */
  public List<User> getUsers() {
    Map<Long, Float> userToRate = resultDAO.getSessions().userToRate;
    List<User> users = null;
    try {
      Pair idToCount = populateUserToNumAnswers();
      users = userDAO.getUsers();
      int total = exerciseDAO.getRawExercises().size();
      for (User u : users) {
        Integer numResults = idToCount.idToCount.get(u.id);
        if (numResults != null) {
          u.setNumResults(numResults);

          if (userToRate.containsKey(u.id)) {
            u.setRate(userToRate.get(u.id));
          }
          int size = idToCount.idToUniqueCount.get(u.id).size();
          boolean complete = size >= total;
          u.setComplete(complete);
          u.setCompletePercent(Math.min(1.0f,(float)size/(float)total));
/*          logger.debug("user " +u + " : results "+numResults + " unique " + size +
            " vs total exercises " + total + " complete " + complete);*/
        }
      }
    } catch (Exception e) {
      logger.error("Got " +e,e);
    }

    joinWithDLIUsers(users);
    return users;
  }

  private Pair populateUserToNumAnswers() {
    Map<Long, Integer> idToCount = new HashMap<Long, Integer>();
    Map<Long, Set<String>> idToUniqueCount = new HashMap<Long, Set<String>>();
   // Set<String> unique = new HashSet<String>();
    for (Result r : resultDAO.getResults()) {
      Integer count = idToCount.get(r.userid);
      if (count == null) idToCount.put(r.userid, 1);
      else idToCount.put(r.userid, count + 1);

      Set<String> uniqueForUser = idToUniqueCount.get(r.userid);
      if (uniqueForUser == null) idToUniqueCount.put(r.userid, uniqueForUser = new HashSet<String>());
      uniqueForUser.add(r.id);
    }
    return new Pair(idToCount, idToUniqueCount);
  }

  public void logEvent(String id, String widgetID, String exid, String context, long userid, String hitID) {
     eventDAO.add(new Event(id,widgetID,exid,context,userid,-1, hitID));
  }

  public List<Event> getEvents() {
    return eventDAO.getAll();
  }
  public EventDAO getEventDAO() { return eventDAO; }

  private static class Pair {
    final Map<Long, Integer> idToCount;
    final Map<Long, Set<String>> idToUniqueCount;
    public Pair(Map<Long, Integer> idToCount, Map<Long, Set<String>> idToUniqueCount) {
      this.idToCount = idToCount;
      this.idToUniqueCount = idToUniqueCount;
    }
  }

  Collection<User> joinWithDLIUsers(List<User> users) {
    List<DLIUser> users1 = dliUserDAO.getUsers();
    Map<Long, User> userMap = userDAO.getMap(users);

    for (DLIUser dliUser : users1) {
      User user = userMap.get(dliUser.getUserID());
      if (user != null) {
        user.setDemographics(dliUser);
      }
    }
    if (users1.isEmpty()) logger.info("no dli users.");
    return userMap.values();
  }

  public List<Result> getResultsWithGrades() {
    List<Result> results = resultDAO.getResults();
    Map<Integer,Result> idToResult = new HashMap<Integer, Result>();
    for (Result r : results) {
      idToResult.put(r.uniqueID, r);
      r.clearGradeInfo();
    }
    Collection<Grade> grades = gradeDAO.getGrades();
   // logger.debug("found " + grades.size() + " grades");
    for (Grade g : grades) {
      Result result = idToResult.get(g.resultID);
      if (result != null) {
        result.addGrade(g);
      }
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
   * @seex mitll.langtest.client.grading.GradingExercisePanel#getAnswerWidget(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int)
   */
  private ResultsAndGrades getResultsForExercise(String exid, boolean filterByFLQAndSpoken, boolean useFLQ, boolean useSpoken) {
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
   * @see mitll.langtest.server.LangTestDatabaseImpl#addTextAnswer
   * @see mitll.langtest.client.exercise.PostAnswerProvider#postAnswers
   */
  public void addAnswer(int userID, CommonExercise e, int questionID, String answer, String answerType) {
    addAnswer(userID, e, questionID, answer, true, answerType);
  }

  private void addAnswer(int userID, CommonExercise e, int questionID, String answer, boolean correct, String answerType) {
    answerDAO.addAnswer(userID, e, questionID, answer, "",
      true,//!e.isPromptInEnglish(),
      false, answerType, correct, 0);
  }

  public AnswerDAO getAnswerDAO() { return answerDAO; }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#writeAudioFile
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
    List<CommonExercise> objects = Collections.emptyList();
    UserStateWrapper userStateWrapper = createOrGetUserState(userID, objects);
    userStateWrapper.addCompleted(exerciseID);
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

  public UserListManager getUserListManager() { return userListManager; }

  /**
   * @see mitll.langtest.client.custom.ReviewEditableExercise#duplicateExercise()
   * @param exercise
   * @return
   */
  public UserExercise duplicateExercise(UserExercise exercise) {
    logger.debug("to duplicate  " + exercise);

    //logger.debug("anno before " + exercise.getFieldToAnnotation());
    UserExercise duplicate = getUserListManager().duplicate(exercise);

    if (!exercise.isPredefined()) {
      logger.warn("huh? got non-predef " + exercise);
    }

    SectionHelper sectionHelper = getSectionHelper();
   // CommonExercise ex = duplicate.toExercise();
    List<SectionHelper.Pair> pairs = new ArrayList<SectionHelper.Pair>();
    for (Map.Entry<String, String> pair : exercise.getUnitToValue().entrySet()) {
      pairs.add(sectionHelper.addExerciseToLesson(duplicate, pair.getKey(), pair.getValue()));
    }
    sectionHelper.addAssociations(pairs);

    getAddRemoveDAO().add(duplicate.getID(), AddRemoveDAO.ADD);
    getExerciseDAO().add(duplicate);

    logger.debug("exercise state " + exercise.getState());

    userListManager.setState(duplicate, exercise.getState(), exercise.getCreator());

    logger.debug("duplicate after " + duplicate);

    return duplicate;
  }

  /**
   * @see
   * @param id
   * @return
   */
  public boolean deleteItem(String id ) {
    getAddRemoveDAO().add(id, AddRemoveDAO.REMOVE);

    getUserListManager().removeReviewed(id);

    SectionHelper sectionHelper = getSectionHelper();
    CommonExercise exercise = getExercise(id);
    for (Map.Entry<String, String> pair : exercise.getUnitToValue().entrySet()) {
      sectionHelper.removeExerciseToLesson(exercise, pair.getKey(), pair.getValue());
    }
    //TODO remove pairs?
    return getExerciseDAO().remove(id);
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExercise(String)
   * @param id
   * @return
   */
  private CommonExercise getUserExerciseWhere(String id) {
    CommonUserExercise where = userExerciseDAO.getWhere(id);
    return where != null ? where : null;
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExercise(String)
   * @param id
   * @return
   */
  public CommonExercise getCustomOrPredefExercise(String id) {
    CommonExercise byID = getUserExerciseWhere(id);  // allow custom items to mask out non-custom items
    if (byID == null) {
      byID = getExercise(id);
    }
    return byID;
  }

  public ServerProperties getServerProps() { return serverProps; }
  private AddRemoveDAO getAddRemoveDAO() { return addRemoveDAO;  }
  private ExerciseDAO getExerciseDAO() {  return exerciseDAO;  }

  public String toString() { return "Database : "+ connection.getConnection(); }
}
