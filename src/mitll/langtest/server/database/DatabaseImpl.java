package mitll.langtest.server.database;

import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.connection.DatabaseConnection;
import mitll.langtest.server.database.connection.H2Connection;
import mitll.langtest.server.database.custom.*;
import mitll.langtest.server.database.instrumentation.EventDAO;
import mitll.langtest.shared.*;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.flashcard.AVPHistoryForList;
import mitll.langtest.shared.flashcard.AVPScoreReport;
import mitll.langtest.shared.grade.Grade;
import mitll.langtest.shared.instrumentation.Event;
import mitll.langtest.shared.monitoring.Session;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
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
  private static final Logger logger = Logger.getLogger(DatabaseImpl.class);
  private static final int LOG_THRESHOLD = 10;

  private String installPath;
  private ExerciseDAO exerciseDAO = null;
  private UserDAO userDAO;
  //private DLIUserDAO dliUserDAO;
  private ResultDAO resultDAO;
  private AudioDAO audioDAO;
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
  private final LogAndNotify logAndNotify;

  private boolean addDefects = true;

  /**
   * Just for testing
   *
   * @param configDir
   * @param pathHelper
   * @see mitll.langtest.server.LangTestDatabaseImpl#readProperties(javax.servlet.ServletContext)
   */
  public DatabaseImpl(String configDir, String configFile, String dbName, PathHelper pathHelper, boolean mustAlreadyExist) {
    this(configDir, "", dbName, new ServerProperties(configDir, configFile), pathHelper, mustAlreadyExist, null);
    this.lessonPlanFile = serverProps.getLessonPlan();
    this.useFile = lessonPlanFile != null;
    addDefects = false;
  }

  /**
   * @param configDir
   * @param relativeConfigDir
   * @param dbName
   * @param serverProps
   * @param pathHelper
   * @param mustAlreadyExist
   * @param logAndNotify
   * @see mitll.langtest.server.LangTestDatabaseImpl#makeDatabaseImpl(String)
   */
  public DatabaseImpl(String configDir, String relativeConfigDir, String dbName, ServerProperties serverProps,
                      PathHelper pathHelper, boolean mustAlreadyExist, LogAndNotify logAndNotify) {
    long then = System.currentTimeMillis();
    connection = new H2Connection(configDir, dbName, mustAlreadyExist);
    long now = System.currentTimeMillis();
    if (now - then > 1000)
      logger.info("took " + (now - then) + " millis to open database for " + serverProps.getLanguage());

    absConfigDir = configDir;
    this.configDir = relativeConfigDir;

    this.isWordPairs = serverProps.isWordPairs();
    this.doImages = serverProps.doImages();
    this.language = serverProps.getLanguage();
    this.isFlashcard = serverProps.isFlashcard();
    this.serverProps = serverProps;
    this.lessonPlanFile = serverProps.getLessonPlan();
    this.useFile = lessonPlanFile != null;
    this.logAndNotify = logAndNotify;

    try {
      Connection connection1 = getConnection();
      if (connection1 == null) {
        logger.warn("couldn't open connection to database at " + configDir + " : " + dbName);
        return;
      } else {
        closeConnection(connection1);
      }
    } catch (Exception e) {
      logger.error("couldn't open connection to database, got " + e.getMessage(), e);
      return;
    }
    then = System.currentTimeMillis();

    initializeDAOs(pathHelper);
    now = System.currentTimeMillis();
    if (now - then > 1000) {
      logger.info("took " + (now - then) + " millis to initialize DAOs for " + serverProps.getLanguage());
    }

    monitoringSupport = getMonitoringSupport();
  }

  private Connection getConnection() {
    return getConnection(this.getClass().toString());
  }

  /**
   * Create or alter tables as needed.
   */
  private void initializeDAOs(PathHelper pathHelper) {
//    logger.debug("initializeDAOs ---");
    userDAO = new UserDAO(this);
    UserListDAO userListDAO = new UserListDAO(this, userDAO);
    addRemoveDAO = new AddRemoveDAO(this);

    userExerciseDAO = new UserExerciseDAO(this);
    UserListExerciseJoinDAO userListExerciseJoinDAO = new UserListExerciseJoinDAO(this);
    //dliUserDAO = new DLIUserDAO(this);
    resultDAO = new ResultDAO(this);
    audioDAO = new AudioDAO(this, userDAO);
    answerDAO = new AnswerDAO(this, resultDAO);
    gradeDAO = new GradeDAO(this, userDAO, resultDAO);
    userListManager = new UserListManager(userDAO, userListDAO, userListExerciseJoinDAO,
        new AnnotationDAO(this, userDAO),
        new ReviewedDAO(this, ReviewedDAO.REVIEWED),
        new ReviewedDAO(this, ReviewedDAO.SECOND_STATE),
        pathHelper);

    eventDAO = new EventDAO(this, userDAO);

    Connection connection1 = getConnection();
    try {
      resultDAO.createResultTable(connection1);
      connection1 = getConnection();
      gradeDAO.createGradesTable(connection1);
    } catch (Exception e) {
      logger.error("got " + e, e);  //To change body of catch statement use File | Settings | File Templates.
    } finally {
      closeConnection(connection1);
    }

    try {
      userDAO.createUserTable(this);
      // dliUserDAO.createUserTable(this);
      userListManager.setUserExerciseDAO(userExerciseDAO);
    } catch (Exception e) {
      logger.error("got " + e, e);  //To change body of catch statement use File | Settings | File Templates.
    }
  }

  public ResultDAO getResultDAO() {
    return resultDAO;
  }

  public UserDAO getUserDAO() {
    return userDAO;
  }

  @Override
  public Connection getConnection(String who) {
    return connection.getConnection(who);
  }

  /**
   * It seems like this isn't required?
   *
   * @param conn
   * @throws SQLException
   */
  public void closeConnection(Connection conn) {
    try {
      int before = connection.connectionsOpen();
      if (conn != null && !conn.isClosed()) {
        if (connection.usingCP()) {
          conn.close();
        }
      }
      //else {
      //logger.warn("trying to close a null connection...");
      // }
      if (connection.connectionsOpen() > LOG_THRESHOLD) {
        logger.debug("closeConnection : now " + connection.connectionsOpen() + " open vs before " + before);
      }

    } catch (SQLException e) {
      logger.error("Got " + e, e);
    }
  }

  /**
   * @throws SQLException
   * @seex mitll.langtest.server.database.custom.UserListManagerTest#tearDown
   */
  public void closeConnection() throws SQLException {
    logger.debug("   ------- testing only closeConnection : now " + connection.connectionsOpen() + " open.");

    Connection connection1 = connection.getConnection(this.getClass().toString());
    if (connection1 != null) {
      connection1.close();
    }
  }

  public Export getExport() {
    return new Export(exerciseDAO, resultDAO, gradeDAO);
  }

  MonitoringSupport getMonitoringSupport() {
    return new MonitoringSupport(userDAO, resultDAO, gradeDAO);
  }

  /**
   * @param useFile
   * @return
   * @see #getExercises(boolean, String)
   */
  private ExerciseDAO makeExerciseDAO(boolean useFile) {
    return useFile ?
        new FileExerciseDAO(mediaDir, language, isFlashcard, absConfigDir, serverProps.getMappingFile()) :
        new SQLExerciseDAO(this, mediaDir, absConfigDir, serverProps);
  }

  /**
   * @param installPath
   * @param lessonPlanFile
   * @param language
   * @param mediaDir
   * @see mitll.langtest.server.LangTestDatabaseImpl#setInstallPath
   */
  public void setInstallPath(String installPath, String lessonPlanFile, String language,
                             boolean useFile, String mediaDir) {
    // logger.debug("got install path " + installPath + " media " + mediaDir);
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
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExercises
   */
  public List<CommonExercise> getExercises() {
    return getExercises(useFile, lessonPlanFile);
  }

  /**
   * @param id
   * @return
   * @see mitll.langtest.server.LoadTesting#getExercise
   */
  public CommonExercise getExercise(String id) {
    return exerciseDAO.getExercise(id);
  }

  /**
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
    //logger.debug("using lesson plan file " +lessonPlanFile + " at " + installPath);
    boolean isExcel = lessonPlanFile.endsWith(".xlsx");
    makeDAO(useFile, lessonPlanFile, isExcel, mediaDir, installPath);

    List<CommonExercise> rawExercises = exerciseDAO.getRawExercises();//getRawExercises(useFile, lessonPlanFile, isExcel);
    if (rawExercises.isEmpty()) {
      logger.warn("no exercises for useFile = " + useFile + " and " + lessonPlanFile + " at " + installPath);
    }
    return rawExercises;
  }

  /**
   * @param useFile
   * @param lessonPlanFile
   * @param excel
   * @param mediaDir
   * @see #getExercises(boolean, String)
   */
  private void makeDAO(boolean useFile, String lessonPlanFile, boolean excel, String mediaDir, String installPath) {
    if (exerciseDAO == null) {
      if (useFile && excel) {
        synchronized (this) {
          this.exerciseDAO = new ExcelImport(lessonPlanFile, mediaDir, absConfigDir, serverProps, userListManager, installPath, addDefects);
        }
      } else {
        this.exerciseDAO = makeExerciseDAO(useFile);
      }
      userExerciseDAO.setExerciseDAO(exerciseDAO);
      exerciseDAO.setUserExerciseDAO(userExerciseDAO);
      exerciseDAO.setAddRemoveDAO(addRemoveDAO);
      exerciseDAO.setAudioDAO(audioDAO);

      getRawExercises(useFile, lessonPlanFile, excel);

      userDAO.checkForFavorites(userListManager);
      userExerciseDAO.setAudioDAO(audioDAO);
    }
  }

  private void getRawExercises(boolean useFile, String lessonPlanFile, boolean isExcel) {
    if (useFile && !isExcel) {
      if (isWordPairs) {
        ((FileExerciseDAO) exerciseDAO).readWordPairs(lessonPlanFile, doImages);
      } else {
        ((FileExerciseDAO) exerciseDAO).readFastAndSlowExercises(installPath, configDir, lessonPlanFile);
      }
    }
    exerciseDAO.getRawExercises();
  }

  /**
   * @param userExercise
   * @see mitll.langtest.server.LangTestDatabaseImpl#editItem(mitll.langtest.shared.custom.UserExercise)
   * @see mitll.langtest.client.custom.dialog.EditableExercise#postEditItem
   */
  public void editItem(UserExercise userExercise) {
    logger.debug("editItem " + userExercise.getID() + " mediaDir : " + serverProps.getMediaDir() + " initially audio was\n\t " + userExercise.getAudioAttributes());

    userExercise.setTooltip();

    logger.debug("tooltip now " + userExercise.getTooltip());

    getUserListManager().editItem(userExercise, true, serverProps.getMediaDir());

    Set<AudioAttribute> original = new HashSet<AudioAttribute>(userExercise.getAudioAttributes());
    Set<AudioAttribute> defects = getAndMarkDefects(userExercise, userExercise.getFieldToAnnotation());

    CommonExercise exercise = exerciseDAO.addOverlay(userExercise);
    if (exercise == null) {
      // not an overlay! it's a new user exercise
      exercise = getUserExerciseWhere(userExercise.getID());
      logger.debug("not an overlay " + exercise);
    } else {
      exercise = userExercise;
      logger.debug("made overlay " + exercise);
    }

    if (exercise == null) {
      logger.error("huh? couldn't make overlay or find user exercise for " + userExercise);
    } else {
      original.removeAll(defects);

      for (AudioAttribute attribute : defects) {
        if (!exercise.removeAudio(attribute)) {
          logger.warn("huh? couldn't remove " + attribute.getKey() + " from " + exercise.getID());
        }
      }

      String overlayID = exercise.getID();

      logger.debug("editItem copying " + original.size() + " audio attrs under exercise overlay id " + overlayID);

      for (AudioAttribute toCopy : original) {
        if (toCopy.getUserid() < UserDAO.DEFAULT_FEMALE_ID) {
          logger.error("bad user id for " + toCopy);
        }

        audioDAO.add((int) toCopy.getUserid(), toCopy.getAudioRef(), overlayID, toCopy.getTimestamp(), toCopy.getAudioType(), toCopy.getDuration());
      }
    }

    getSectionHelper().refreshExercise(exercise);
  }

  /**
   * Marks defects too...?
   *
   * @param userExercise
   * @param fieldToAnnotation
   * @return
   * @see #editItem(mitll.langtest.shared.custom.UserExercise)
   */
  private Set<AudioAttribute> getAndMarkDefects(UserExercise userExercise, Map<String, ExerciseAnnotation> fieldToAnnotation) {
    Set<AudioAttribute> defects = new HashSet<AudioAttribute>();

    for (Map.Entry<String, ExerciseAnnotation> fieldAnno : fieldToAnnotation.entrySet()) {
      if (!fieldAnno.getValue().isCorrect()) {
        AudioAttribute audioAttribute = userExercise.getAudioRefToAttr().get(fieldAnno.getKey());
        if (audioAttribute != null) {
          logger.debug("getAndMarkDefects : found defect " + audioAttribute + " anno : " + fieldAnno.getValue() + " field  " + fieldAnno.getKey());
          // logger.debug("\tmarking defect on audio");
          defects.add(audioAttribute);
          audioDAO.markDefect(audioAttribute);
        } else if (!fieldAnno.getKey().equals("transliteration")) {
          logger.warn("\tcan't mark defect on audio : looking for field '" + fieldAnno.getKey() + "' in " + userExercise.getAudioRefToAttr().keySet());
        }
      }
    }

    return defects;
  }

  /**
   * @param audioAttribute
   * @see mitll.langtest.server.LangTestDatabaseImpl#markAudioDefect(mitll.langtest.shared.AudioAttribute, String)
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#getPanelForAudio(mitll.langtest.shared.CommonExercise, mitll.langtest.shared.AudioAttribute, mitll.langtest.client.custom.tabs.RememberTabAndContent)
   */
  public void markAudioDefect(AudioAttribute audioAttribute) {
    if (audioDAO.markDefect(audioAttribute) < 1) {
      logger.error("huh? couldn't mark error on " + audioAttribute);
    }
  }

  /**
   * TODOx : do all average calc on server!
   *
   * @return
   * @paramx listid
   * @see mitll.langtest.server.LangTestDatabaseImpl#getUserHistoryForList
   * @see mitll.langtest.client.flashcard.StatsFlashcardFactory.StatsPracticePanel#onSetComplete
   */
  public AVPScoreReport getUserHistoryForList(long userid, Collection<String> ids, long latestResultID,
                                              Collection<String> allIDs) {
    logger.debug("getUserHistoryForList " + userid + " and " + ids.size() + " ids, latest " + latestResultID);

    ResultDAO.SessionsAndScores sessionsAndScores = resultDAO.getSessionsForUserIn2(ids, latestResultID, userid, allIDs);
    List<Session> sessionsForUserIn2 = sessionsAndScores.sessions;

    Map<Long, User> userMap = userDAO.getUserMap();

    AVPHistoryForList sessionAVPHistoryForList = new AVPHistoryForList(sessionsForUserIn2, userid, true);
    AVPHistoryForList sessionAVPHistoryForList2 = new AVPHistoryForList(sessionsForUserIn2, userid, false);

    // sort by correct %
    Collections.sort(sessionsForUserIn2, new Comparator<Session>() {
      @Override
      public int compare(Session o1, Session o2) {
        float correctPercent = o1.getCorrectPercent();
        float correctPercent1 = o2.getCorrectPercent();
        return correctPercent < correctPercent1 ? +1 : correctPercent > correctPercent1 ? -1 :
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
    logger.debug("getUserHistoryForList pron    scores " + scores);

    if (scores.size() == 11) {
      scores.remove(9);
    }

    sessionAVPHistoryForList2.setScores(scores);

    List<AVPHistoryForList> historyForLists = new ArrayList<AVPHistoryForList>();
    historyForLists.add(sessionAVPHistoryForList);
    historyForLists.add(sessionAVPHistoryForList2);

//    logger.debug("returning " + historyForLists);
//    logger.debug("correct/incorrect history " + sessionsAndScores.sortedResults);
    return new AVPScoreReport(historyForLists, sessionsAndScores.sortedResults);
  }

  private int compareTimestamps(Session o1, Session o2) {
    return o1.getTimestamp() < o2.getTimestamp() ? +1 : o1.getTimestamp() > o2.getTimestamp() ? -1 : 0;
  }

  private AVPHistoryForList.UserScore makeScore(int count, Map<Long, User> userMap, Session session, boolean useCorrect) {
    float value = useCorrect ? session.getCorrectPercent() : 100f * session.getAvgScore();
    long userid = session.getUserid();
    User user = userMap.get(userid);
    String userID;
    if (user == null) {
      logger.warn("huh? couldn't find userid " + userid + " in map with keys " + userMap.keySet());
      userID = "Default User";
    } else {
      userID = user.getUserID();
    }
    return new AVPHistoryForList.UserScore(count,
        userID,
        value,
        session.isLatest());
  }

  /**
   * @return unmodifiable list of exercises
   * @see mitll.langtest.server.LangTestDatabaseImpl#init
   */
  public void preloadExercises() {
    getExercises(useFile, lessonPlanFile);
  }

  /**
   * @param request
   * @param age
   * @param gender
   * @param experience
   * @param nativeLang
   * @param dialect
   * @param userID
   * @param permissions
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#addUser(int, String, int, String, String, String, java.util.Collection)
   */
 /* public long addUser(HttpServletRequest request,
                      int age, String gender, int experience,
                      String nativeLang, String dialect, String userID, Collection<User.Permission> permissions) {
    String ip = getIPInfo(request);
    return addUser(age, gender, experience, ip, nativeLang, dialect, userID, permissions);
  }*/

  /**
   * @param request
   * @param userID
   * @param passwordH
   * @param emailH
   * @param kind
   * @param isMale
   * @param age
   * @param dialect
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#addUser
   */
  public User addUser(HttpServletRequest request, String userID, String passwordH, String emailH, User.Kind kind, boolean isMale, int age, String dialect) {
    String ip = getIPInfo(request);
    User user = userDAO.addUser(userID, passwordH, emailH, kind, ip, isMale, age, dialect);
    if (user != null) {
      userListManager.createFavorites(user.getId());
    }
    return user;
  }

  /**
   * @param user
   * @return
   * @see mitll.langtest.server.database.ImportCourseExamples#copyUser(DatabaseImpl, java.util.Map, java.util.Map, long)
   */
  public long addUser(User user) {
    long l;
    if ((l = userDAO.userExists(user.getUserID())) == -1) {
      logger.debug("addUser " + user);
      l = userDAO.addUser(user.getAge(), user.getGender() == 0 ? UserDAO.MALE : UserDAO.FEMALE,
          user.getExperience(), user.getIpaddr(), user.getNativeLang(), user.getDialect(), user.getUserID(), false,
          user.getPermissions(), User.Kind.STUDENT, "", "");
    }
    return l;
  }

  /**
   * Somehow on subsequent runs, the ids skip by 30 or so?
   * <p/>
   * Uses return generated keys to get the user id
   *
   * @param age
   * @param gender
   * @param experience
   * @param ipAddr      user agent info
   * @param dialect     speaker dialect
   * @param permissions
   * @return assigned id
   * @see mitll.langtest.server.LangTestDatabaseImpl#addUser
   */
  public long addUser(int age, String gender, int experience, String ipAddr,
                      String nativeLang, String dialect, String userID, Collection<User.Permission> permissions) {
    logger.debug("addUser " + userID);
    long l = userDAO.addUser(age, gender, experience, ipAddr, nativeLang, dialect, userID, false, permissions, User.Kind.STUDENT, "", "");
    userListManager.createFavorites(l);
    return l;
  }

  private String getIPInfo(HttpServletRequest request) {
    String header = request.getHeader("User-Agent");
    SimpleDateFormat sdf = new SimpleDateFormat();
    String format = sdf.format(new Date());
    return request.getRemoteHost() +/*"/"+ request.getRemoteAddr()+*/(header != null ? "/" + header : "") + " at " + format;
  }

  public void usersToXLSX(OutputStream out) {
    userDAO.toXLSX(out, getUsers());
  }

  /**
   * Adds some sugar -- sets the answers and rate per user, and joins with dli experience data
   *
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getUsers()
   */
  public List<User> getUsers() {
    Map<Long, Float> userToRate = resultDAO.getSessions().userToRate;
    List<User> users = null;
    try {
      Pair idToCount = populateUserToNumAnswers();
      users = userDAO.getUsers();
      int total = exerciseDAO.getRawExercises().size();
      for (User u : users) {
        Integer numResults = idToCount.idToCount.get(u.getId());
        if (numResults != null) {
          u.setNumResults(numResults);

          if (userToRate.containsKey(u.getId())) {
            u.setRate(userToRate.get(u.getId()));
          }
          int size = idToCount.idToUniqueCount.get(u.getId()).size();
          boolean complete = size >= total;
          u.setComplete(complete);
          u.setCompletePercent(Math.min(1.0f, (float) size / (float) total));
/*          logger.debug("user " +u + " : results "+numResults + " unique " + size +
            " vs total exercises " + total + " complete " + complete);*/
        }
      }
    } catch (Exception e) {
      logger.error("Got " + e, e);
    }

    //joinWithDLIUsers(users);
    return users;
  }

  private Pair populateUserToNumAnswers() {
    Map<Long, Integer> idToCount = new HashMap<Long, Integer>();
    Map<Long, Set<String>> idToUniqueCount = new HashMap<Long, Set<String>>();
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

  public void logEvent(String id, String widgetID, String exid, String context, long userid, String hitID) throws SQLException {
    eventDAO.add(new Event(id, widgetID, exid, context, userid, -1, hitID));
  }

  public void logEvent(String exid, String context, long userid) {
    if (context.length() > 100) context = context.substring(0, 100).replace("\n", " ");
    try {
      logEvent("unknown", "server", exid, context, userid, "unknown");
    } catch (SQLException e) {
      logAndNotify(e);
    }
  }

  public void logAndNotify(Exception e) {
    logAndNotify.logAndNotifyServerException(e);
  }

  public List<Event> getEvents() {
    return eventDAO.getAll();
  }

  public EventDAO getEventDAO() {
    return eventDAO;
  }

  public AudioDAO getAudioDAO() {
    return audioDAO;
  }

  private static class Pair {
    final Map<Long, Integer> idToCount;
    final Map<Long, Set<String>> idToUniqueCount;

    public Pair(Map<Long, Integer> idToCount, Map<Long, Set<String>> idToUniqueCount) {
      this.idToCount = idToCount;
      this.idToUniqueCount = idToUniqueCount;
    }
  }

/*  Collection<User> joinWithDLIUsers(List<User> users) {
    List<DLIUser> users1 = dliUserDAO.getUsers();
    Map<Long, User> userMap = userDAO.getMap(users);

    for (DLIUser dliUser : users1) {
      User user = userMap.get(dliUser.getUserID());
      if (user != null) {
        user.setDemographics(dliUser);
      }
    }
    //if (users1.isEmpty()) logger.info("no dli users.");
    return userMap.values();
  }*/

  public List<Result> getResultsWithGrades() {
    List<Result> results = resultDAO.getResults();
    Map<Integer, Result> idToResult = new HashMap<Integer, Result>();
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

  public int getNumResults() {
    return resultDAO.getNumResults();
  }

  /**
   * Creates the result table if it's not there.
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

  public AnswerDAO getAnswerDAO() {
    return answerDAO;
  }

  /**
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
   * @param recordedWithFlash
   * @see mitll.langtest.server.LangTestDatabaseImpl#writeAudioFile
   */
  public long addAudioAnswer(int userID, String plan, String exerciseID, int questionID,
                             String audioFile,
                             boolean valid, boolean flq, boolean spoken,
                             String audioType, int durationInMillis, boolean correct, float score, boolean recordedWithFlash) {
    return answerDAO.addAnswer(this, userID, plan, exerciseID, questionID, "", audioFile, valid, flq, spoken, audioType + (recordedWithFlash ? "" : "_by_WebRTC"),
        durationInMillis, correct, score, "");
  }

  public int userExists(String login) {
    return userDAO.userExists(login);
  }

  /**
   * TODO : worry about duplicate userid?
   *
   * @return map of user to number of answers the user entered
   */
  public Map<User, Integer> getUserToResultCount() {
    return monitoringSupport.getUserToResultCount();
  }

  /**
   * Determine sessions per user.  If two consecutive items are more than {@link ResultDAO#SESSION_GAP} seconds
   * apart, then we've reached a session boundary.
   * Remove all sessions that have just one answer - must be test sessions.
   *
   * @return list of duration and numAnswer pairs
   * @see mitll.langtest.server.LangTestDatabaseImpl#getSessions()
   */
  public List<Session> getSessions() {
    return monitoringSupport.getSessions().sessions;
  }

  /**
   * TODO : worry about duplicate userid?
   *
   * @return
   */
  public Map<Integer, Integer> getResultCountToCount() {
    return monitoringSupport.getResultCountToCount(getExercises());
  }

  /**
   * Get counts of answers by date
   * TODO : worry about duplicate userid?
   *
   * @return
   */
  public Map<String, Integer> getResultByDay() {
    return monitoringSupport.getResultByDay();
  }

  /**
   * get counts of answers by hours of the day
   *
   * @return
   */
  public Map<String, Integer> getResultByHourOfDay() {
    return monitoringSupport.getResultByHourOfDay();
  }

  /**
   * Split exid->count by gender.
   *
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getResultPerExercise
   */
  public Map<String, Map<String, Integer>> getResultPerExercise() {
    return monitoringSupport.getResultPerExercise(getExercises());
  }

  /**
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getResultCountsByGender()
   */
  public Map<String, Map<Integer, Integer>> getResultCountsByGender() {
    return monitoringSupport.getResultCountsByGender(getExercises());
  }

  public Map<String, Map<Integer, Map<Integer, Integer>>> getDesiredCounts() {
    return monitoringSupport.getDesiredCounts(getExercises());
  }

  /**
   * Return some statistics related to the hours of audio that have been collected
   *
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getResultStats()
   */
  public Map<String, Number> getResultStats() {
    return monitoringSupport.getResultStats();
  }

  public Map<Integer, Map<String, Map<String, Integer>>> getGradeCountPerExercise() {
    return monitoringSupport.getGradeCountPerExercise(getExercises());
  }

  public void destroy() {
    try {
      connection.contextDestroyed();
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
  }

/*
  public void addDLIUser(DLIUser dliUser) throws Exception {
    dliUserDAO.addUser(dliUser);
  }
*/

  public UserListManager getUserListManager() {
    return userListManager;
  }

  /**
   * @param exercise
   * @return
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#duplicateExercise
   * @see mitll.langtest.server.LangTestDatabaseImpl#duplicateExercise(mitll.langtest.shared.custom.UserExercise)
   */
  public UserExercise duplicateExercise(UserExercise exercise) {
    logger.debug("to duplicate  " + exercise);

    //logger.debug("anno before " + exercise.getFieldToAnnotation());
    UserExercise duplicate = getUserListManager().duplicate(exercise);

    if (!exercise.isPredefined()) {
      logger.warn("huh? got non-predef " + exercise);
    }

    SectionHelper sectionHelper = getSectionHelper();

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
   * @param id
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#deleteItem(String)
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#deleteItem(String, long, mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.PagingExerciseList, mitll.langtest.client.list.PagingExerciseList)
   */
  public boolean deleteItem(String id) {
    getAddRemoveDAO().add(id, AddRemoveDAO.REMOVE);
    getUserListManager().removeReviewed(id);
    getSectionHelper().removeExercise(getExercise(id));
    return getExerciseDAO().remove(id);
  }

  /**
   * @param id
   * @return
   * @see mitll.langtest.server.LoadTesting#getExercise
   */
  public CommonExercise getCustomOrPredefExercise(String id) {
    CommonExercise byID = getUserExerciseWhere(id);  // allow custom items to mask out non-custom items
    if (byID == null) {
      byID = getExercise(id);
    }
    return byID;
  }

  /**
   * @param id
   * @return
   * @see mitll.langtest.server.LoadTesting#getExercise
   */
  private CommonExercise getUserExerciseWhere(String id) {
    CommonUserExercise where = userExerciseDAO.getWhere(id);
    return where != null ? where : null;
  }

  public ServerProperties getServerProps() {
    return serverProps;
  }

  private AddRemoveDAO getAddRemoveDAO() {
    return addRemoveDAO;
  }

  private ExerciseDAO getExerciseDAO() {
    return exerciseDAO;
  }

  /**
   * @param out
   * @param typeToSection
   * @throws Exception
   * @see mitll.langtest.server.DownloadServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  public void writeZip(OutputStream out, Map<String, Collection<String>> typeToSection) throws Exception {
    Collection<CommonExercise> exercisesForSelectionState = typeToSection.isEmpty() ?
        getExercises() :
        getSectionHelper().getExercisesForSelectionState(typeToSection);
    String language1 = getServerProps().getLanguage();

    new AudioExport().writeZip(out, typeToSection, getSectionHelper(), exercisesForSelectionState, language1, getAudioDAO(), installPath, configDir);
  }

  public void writeZip(OutputStream out) throws Exception {
    Collection<CommonExercise> exercisesForSelectionState = getExercises();
    new AudioExport().writeZipJustOneAudio(out, getSectionHelper(), exercisesForSelectionState, installPath);
  }

  /**
   * For downloading a user list.
   *
   * @param out
   * @param listid
   * @return
   * @throws Exception
   * @see mitll.langtest.server.DownloadServlet#writeUserList(javax.servlet.http.HttpServletResponse, DatabaseImpl, String)
   */
  public String writeZip(OutputStream out, long listid) throws Exception {
    UserList userListByID = getUserListManager().getUserListByID(listid, getSectionHelper().getTypeOrder());

    String language1 = getServerProps().getLanguage();
    if (userListByID == null) {
      logger.error("huh? can't find user list " + listid);
      return language1 + "_Unknown";
    } else {
      //logger.debug("writing contents of " + userListByID);
      new AudioExport().writeZip(out, userListByID.getName(), getSectionHelper(), userListByID.getExercises(), language1, getAudioDAO(), installPath, configDir);
    }
    return language1 + "_" + userListByID.getName();
  }

  public String getUserListName(long listid) {
    UserList userListByID = getUserListManager().getUserListByID(listid, getSectionHelper().getTypeOrder());
    String language1 = getServerProps().getLanguage();
    if (userListByID == null) {
      logger.error("huh? can't find user list " + listid);
      return language1 + "_Unknown";
    } else {
      return language1 + "_" + userListByID.getName();
    }
  }

  public String getPrefix(Map<String, Collection<String>> typeToSection) {
    return new AudioExport().getPrefix(getSectionHelper(), typeToSection);
  }

  public String toString() {
    return "Database : " + this.getClass().toString();
  }
}
