package mitll.langtest.server.database;

import mitll.langtest.server.sorter.ExerciseSorter;
import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.database.connection.DatabaseConnection;
import mitll.langtest.server.database.connection.H2Connection;
import mitll.langtest.server.database.contextPractice.ContextPracticeImport;
import mitll.langtest.server.database.custom.*;
import mitll.langtest.server.database.exercise.ExcelImport;
import mitll.langtest.server.database.exercise.ExerciseDAO;
import mitll.langtest.server.database.exercise.SectionHelper;
import mitll.langtest.server.database.instrumentation.EventDAO;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.shared.*;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.flashcard.AVPHistoryForList;
import mitll.langtest.shared.flashcard.AVPScoreReport;
import mitll.langtest.shared.instrumentation.Event;
import mitll.langtest.shared.monitoring.Session;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.CollationKey;
import java.text.Collator;
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
  private static final String UNKNOWN = "unknown";

  private String installPath;
  private ExerciseDAO exerciseDAO = null;
  private UserDAO userDAO;
  private ResultDAO resultDAO;
  private RefResultDAO refresultDAO;
  private WordDAO wordDAO;
  private PhoneDAO phoneDAO;
  private AudioDAO audioDAO;
  private AnswerDAO answerDAO;
  private UserListManager userListManager;
  private UserExerciseDAO userExerciseDAO;
  private AddRemoveDAO addRemoveDAO;
  private EventDAO eventDAO;
  private ContextPractice contextPractice;

  private DatabaseConnection connection = null;
  private MonitoringSupport monitoringSupport;

  private String lessonPlanFile;
  private boolean useFile;
  private final String configDir;

  private String mediaDir;
  private final ServerProperties serverProps;
  private final LogAndNotify logAndNotify;

  private JsonSupport jsonSupport;

  private final boolean addDefects = true;
  private UserManagement userManagement;

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
    if (now - then > 300)
      logger.info("took " + (now - then) + " millis to open database for " + serverProps.getLanguage());

    this.configDir = relativeConfigDir;
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
    if (now - then > 300) {
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
    userDAO = new UserDAO(this, serverProps);
    UserListDAO userListDAO = new UserListDAO(this, userDAO);
    addRemoveDAO = new AddRemoveDAO(this);

    userExerciseDAO = new UserExerciseDAO(this, logAndNotify);
    UserListExerciseJoinDAO userListExerciseJoinDAO = new UserListExerciseJoinDAO(this);
    resultDAO = new ResultDAO(this, logAndNotify);
    refresultDAO = new RefResultDAO(this, logAndNotify, getServerProps().shouldDropRefResult());
    wordDAO = new WordDAO(this, logAndNotify);
    phoneDAO = new PhoneDAO(this, logAndNotify);
    audioDAO = new AudioDAO(this, userDAO);
    answerDAO = new AnswerDAO(this, resultDAO);
    userListManager = new UserListManager(userDAO, userListDAO, userListExerciseJoinDAO,
        new AnnotationDAO(this, userDAO),
        new ReviewedDAO(this, ReviewedDAO.REVIEWED),
        new ReviewedDAO(this, ReviewedDAO.SECOND_STATE),
        pathHelper);

    eventDAO = new EventDAO(this, userDAO, logAndNotify);

    Connection connection1 = getConnection();
    try {
      resultDAO.createResultTable(connection1);
      refresultDAO.createResultTable(connection1);
      connection1 = getConnection();  // huh? why?
    } catch (Exception e) {
      logger.error("got " + e, e);  //To change body of catch statement use File | Settings | File Templates.
    } finally {
      closeConnection(connection1);
    }

    try {
      userDAO.createTable(this);
      userListManager.setUserExerciseDAO(userExerciseDAO);
    } catch (Exception e) {
      logger.error("got " + e, e);  //To change body of catch statement use File | Settings | File Templates.
    }
  }

  public ResultDAO getResultDAO() {
    return resultDAO;
  }

  public RefResultDAO getRefResultDAO() {  return refresultDAO;  }

  public UserDAO getUserDAO() {    return userDAO;  }

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

  private MonitoringSupport getMonitoringSupport() { return new MonitoringSupport(userDAO, resultDAO);  }

  /**
   * @param installPath
   * @param lessonPlanFile
   * @param mediaDir
   * @see mitll.langtest.server.LangTestDatabaseImpl#setInstallPath
   */
  public void setInstallPath(String installPath, String lessonPlanFile, boolean useFile, String mediaDir) {
    logger.debug("got install path " + installPath + " media " + mediaDir);
    this.installPath = installPath;
    this.lessonPlanFile = lessonPlanFile;
    this.mediaDir = mediaDir;
    this.useFile = useFile;
    makeDAO(lessonPlanFile, mediaDir, installPath);
    this.jsonSupport = new JsonSupport(getSectionHelper(), getResultDAO(), getRefResultDAO(), getAudioDAO(),
        getPhoneDAO(), configDir, installPath);
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
//    boolean isExcel = lessonPlanFile.endsWith(".xlsx");
 //   makeDAO(lessonPlanFile, mediaDir, installPath);

    List<CommonExercise> rawExercises = exerciseDAO.getRawExercises();
    if (rawExercises.isEmpty()) {
      logger.warn("no exercises for useFile = " + useFile + " and " + lessonPlanFile + " at " + installPath);
    }
    return rawExercises;
  }

  /**
   * Lazy, latchy instantiation of DAOs.
   * Not sure why it really has to be this way.
   *
   * @param lessonPlanFile
   * @param mediaDir
   * @see #getExercises(boolean, String)
   */
  private void makeDAO(String lessonPlanFile, String mediaDir, String installPath) {
    if (exerciseDAO == null) {
      logger.debug(lessonPlanFile);
      synchronized (this) {
        this.exerciseDAO = new ExcelImport(lessonPlanFile, mediaDir, serverProps, userListManager, installPath, addDefects);
      }
      userExerciseDAO.setExerciseDAO(exerciseDAO);
      exerciseDAO.setUserExerciseDAO(userExerciseDAO);
      exerciseDAO.setAddRemoveDAO(addRemoveDAO);
      exerciseDAO.setAudioDAO(audioDAO);

      exerciseDAO.getRawExercises();

      userDAO.checkForFavorites(userListManager);
      userExerciseDAO.setAudioDAO(audioDAO);

      userManagement = new UserManagement(userDAO,exerciseDAO,resultDAO,userListManager);
    }
  }

  private void makeContextPractice(String contextPracticeFile, String installPath) {
    if (contextPractice == null && contextPracticeFile != null) {
      synchronized (this) {
        this.contextPractice = new ContextPracticeImport(installPath + File.separator + contextPracticeFile).getContextPractice();
      }
    }
  }

  /**
   * @param userExercise
   * @see mitll.langtest.server.LangTestDatabaseImpl#editItem(mitll.langtest.shared.custom.UserExercise)
   * @see mitll.langtest.client.custom.dialog.EditableExercise#postEditItem
   */
  public void editItem(UserExercise userExercise) {
    logger.debug("editItem ex #" + userExercise.getID() + " mediaDir : " + serverProps.getMediaDir() + " initially audio was\n\t " + userExercise.getAudioAttributes());

    userExercise.setTooltip();

    logger.debug("tooltip now " + userExercise.getTooltip());

    getUserListManager().editItem(userExercise, true, serverProps.getMediaDir());

    Set<AudioAttribute> original = new HashSet<AudioAttribute>(userExercise.getAudioAttributes());
    Set<AudioAttribute> defects = getAndMarkDefects(userExercise, userExercise.getFieldToAnnotation());

    logger.debug("originally had " + original.size() + " attribute, and " + defects.size() + " defects");

    CommonExercise exercise = exerciseDAO.addOverlay(userExercise);
    boolean notOverlay = exercise == null;
    if (notOverlay) {
      // not an overlay! it's a new user exercise
      exercise = getUserExerciseWhere(userExercise.getID());
      logger.debug("not an overlay " + exercise);
    } else {
      exercise = userExercise;
      logger.debug("made overlay " + exercise);
    }

    if (notOverlay) {
      logger.error("huh? couldn't make overlay or find user exercise for " + userExercise);
    } else {
      boolean b = original.removeAll(defects);  // TODO - does this work really without a compareTo?
      logger.debug(b?"removed defects " +original.size() + " now" : " didn't remove any defects - " + defects.size());

      for (AudioAttribute attribute : defects) {
        if (!exercise.removeAudio(attribute)) {
          logger.warn("huh? couldn't remove " + attribute.getKey() + " from " + exercise.getID());
        }
      }

      // why would this make sense to do???
/*      String overlayID = exercise.getID();

      logger.debug("editItem copying " + original.size() + " audio attrs under exercise overlay id " + overlayID);

      for (AudioAttribute toCopy : original) {
        if (toCopy.getUserid() < UserDAO.DEFAULT_FEMALE_ID) {
          logger.error("bad user id for " + toCopy);
        }
        logger.debug("\t copying " + toCopy);
        audioDAO.add((int) toCopy.getUserid(), toCopy.getAudioRef(), overlayID, toCopy.getTimestamp(), toCopy.getAudioType(), toCopy.getDurationInMillis());
      }*/
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
   * @see mitll.langtest.server.ScoreServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   * @param userid
   * @param typeToSection
   * @paramx collator
   * @return
   */
  public JSONObject getJsonScoreHistory(long userid,
                                        Map<String, Collection<String>> typeToSection,
                                        ExerciseSorter sorter) {
    return jsonSupport.getJsonScoreHistory(userid, typeToSection, sorter);
  }

  public JSONObject getJsonRefResult(Map<String, Collection<String>> typeToSection) {
    return jsonSupport.getJsonRefResults(typeToSection);
  }

  /**
   * A special, slimmed down history just for the Appen recording app.
   * @see mitll.langtest.server.ScoreServlet#getRecordHistory
   * @param userid
   * @param typeToSection
   * @param collator
   * @return
   */
  public JSONObject getJsonScoreHistoryRecorded(long userid,
                                                Map<String, Collection<String>> typeToSection,
                                                Collator collator) {
    return jsonSupport.getJsonScoreHistoryRecorded(userid, typeToSection, collator);
  }

  /**
   * For all the exercises in a chapter

   Get latest results
   Get phones for latest

   //Score phones
   Sort phone scores – asc

   Map phone->example

   Join phone->word

   Sort word by score asc
   * @return
   * @see mitll.langtest.server.ScoreServlet#getPhoneReport
   */
  public JSONObject getJsonPhoneReport(long userid, Map<String, Collection<String>> typeToValues) {
    return jsonSupport.getJsonPhoneReport(userid, typeToValues);
  }

  /**
   * does all average calc on server
   *
   * @return
   * @paramx listid
   * @see mitll.langtest.server.LangTestDatabaseImpl#getUserHistoryForList
   * @see mitll.langtest.client.flashcard.StatsFlashcardFactory.StatsPracticePanel#onSetComplete
   */
  public AVPScoreReport getUserHistoryForList(long userid, Collection<String> ids, long latestResultID,
                                              Collection<String> allIDs, Map<String, CollationKey> idToKey) {
    logger.debug("getUserHistoryForList " + userid + " and " + ids.size() + " ids, latest " + latestResultID);

    ResultDAO.SessionsAndScores sessionsAndScores = resultDAO.getSessionsForUserIn2(ids, latestResultID, userid, allIDs, idToKey);
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
  public void preloadExercises() {  getExercises(useFile, lessonPlanFile); }

  public void preloadContextPractice() {makeContextPractice(serverProps.getDialogFile(), installPath);}

  public ContextPractice getContextPractice() {
    if(this.contextPractice == null){
      makeContextPractice(serverProps.getDialogFile(), installPath);
    }
    return this.contextPractice;
  }

  /**
   * Check other sites to see if the user exists somewhere else, and if so go ahead and use that person
   * here.
   *
   * @param login
   * @param passwordH
   * @return
   * @see mitll.langtest.client.user.UserPassLogin#gotLogin
   * @see mitll.langtest.client.user.UserPassLogin#makeSignInUserName(com.github.gwtbootstrap.client.ui.Fieldset)
   * @see mitll.langtest.server.LangTestDatabaseImpl#userExists(String, String)
   */
  public User userExists(HttpServletRequest request, String login, String passwordH) {
    return userManagement.userExists(request, login, passwordH);
  }

  /**
   *
   * @param userID
   * @param passwordH
   * @param emailH
   * @param deviceType
   * @param device
   * @return
   * @see mitll.langtest.server.ScoreServlet#doPost
   */
  public User addUser(String userID, String passwordH, String emailH, String deviceType, String device) {
    return userManagement.addUser(userID, passwordH, emailH, deviceType, device);
  }

  public User addUser(String userID, String passwordH, String emailH, String deviceType, String device, User.Kind kind,
                      boolean isMale, int age, String dialect) {
    return userManagement.addUser(userID, passwordH, emailH, deviceType, device, kind, isMale, age, dialect);
  }

  /**
   * @param request
   * @param userID
   * @param passwordH
   * @param emailH
   * @param kind
   * @param isMale
   * @param age
   * @param dialect
   * @param device
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#addUser
   */
  public User addUser(HttpServletRequest request, String userID, String passwordH, String emailH, User.Kind kind,
                      boolean isMale, int age, String dialect, String device) {
    return userManagement.addUser(request, userID, passwordH, emailH,  kind, isMale, age, dialect,device);
  }

  /**
   * @param user
   * @return
   * @seex mitll.langtest.server.database.ImportCourseExamples#copyUser
   */
  public long addUser(User user) {
    return userManagement.addUser(user);
  }

  /**
   * Somehow on subsequent runs, the ids skip by 30 or so?
   * <p/>
   * Uses return generated keys to get the user id
   *
   * JUST FOR TESTING
   *
   * @param age
   * @param gender
   * @param experience
   * @param ipAddr      user agent info
   * @param dialect     speaker dialect
   * @param permissions
   * @param device
   * @return assigned id
   */
  public long addUser(int age, String gender, int experience, String ipAddr,
                      String nativeLang, String dialect, String userID, Collection<User.Permission> permissions, String device) {
    return userManagement.addUser(age, gender, experience, ipAddr, nativeLang, dialect, userID,permissions,device);
  }

  /**
   * @see mitll.langtest.server.DownloadServlet
   * @param out
   */
  public void usersToXLSX(OutputStream out) {  userManagement.usersToXLSX(out);  }

  /**
   * Adds some sugar -- sets the answers and rate per user, and joins with dli experience data
   *
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getUsers()
   */
  public List<User> getUsers() {
    return userManagement.getUsers();
  }

  public void logEvent(String exid, String context, long userid, String device) {
    if (context.length() > 100) context = context.substring(0, 100).replace("\n", " ");
    logEvent(UNKNOWN, "server", exid, context, userid, UNKNOWN, device);
  }

  /**
   * @see mitll.langtest.server.ScoreServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   * @param id
   * @param widgetType
   * @param exid
   * @param context
   * @param userid
   * @param device
   * @return
   */
  public boolean logEvent(String id, String widgetType, String exid, String context, long userid, String device) {
    return logEvent(id, widgetType, exid, context, userid, UNKNOWN, device);
  }

  public boolean logEvent(String id, String widgetType, String exid, String context, long userid, String hitID,
                          String device)  {
    return eventDAO.add(new Event(id, widgetType, exid, context, userid, -1, hitID, device));
  }

  public void logAndNotify(Exception e) { logAndNotify.logAndNotifyServerException(e);  }

  public EventDAO getEventDAO() {
    return eventDAO;
  }
  public AudioDAO getAudioDAO() {
    return audioDAO;
  }

  public WordDAO getWordDAO() { return wordDAO;  }
  public PhoneDAO getPhoneDAO() { return phoneDAO;  }

/*  public List<Result> getResultsWithGrades() {
    List<Result> results = resultDAO.getResults();
*//*    Map<Integer,Result> idToResult = new HashMap<Integer, Result>();
    for (Result r : results) {
      idToResult.put(r.getUniqueID(), r);
      r.clearGradeInfo();
    }
    Collection<Grade> grades = gradeDAO.getGrades();
    // logger.debug("found " + grades.size() + " grades");
    for (Grade g : grades) {
      Result result = idToResult.get(g.resultID);
      if (result != null) {
        result.addGrade(g);
      }
    }*//*
    return results;
  }*/

/*  public int getNumResults() {
    return resultDAO.getNumResults();
  }*/

  /**
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getResultAlternatives(java.util.Map, long, String, String)
   * @see mitll.langtest.server.LangTestDatabaseImpl#getResults(java.util.Map, long, String)
   */
  public List<MonitorResult> getMonitorResults() {
    return getMonitorResultsWithText(resultDAO.getMonitorResults());
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getResults(java.util.Map, long, String)
   * @see #getMonitorResults()
   * @param monitorResults
   * @return
   */
  public List<MonitorResult> getMonitorResultsWithText(List<MonitorResult> monitorResults) {
    Map<String, CommonExercise> join = getIdToExerciseMap();
    resultDAO.addUnitAndChapterToResults(monitorResults, join);
    return monitorResults;
  }

  private Map<String, CommonExercise> getIdToExerciseMap() {
    Map<String, CommonExercise> join = new HashMap<String, CommonExercise>();

    for (CommonExercise exercise : getExercises()) {
      String id = exercise.getID();
      join.put(id, exercise);
    }

    for (CommonExercise exercise : userExerciseDAO.getAll()) {
      String id = exercise.getID();
      join.put(id, exercise);
    }
    return join;
  }

  public AnswerDAO getAnswerDAO() {
    return answerDAO;
  }

  /**
   * @param userID
   * @param exerciseID
   * @param questionID
   * @param audioFile
   * @param valid
   * @param audioType
   * @param durationInMillis
   * @param correct
   * @param score
   * @param recordedWithFlash
   * @param deviceType
   * @param device
   * @param scoreJson
   * @param processDur
   * @see mitll.langtest.server.LangTestDatabaseImpl#writeAudioFile
   * @see mitll.langtest.server.audio.AudioFileHelper#getAudioAnswer
   */
  public long addAudioAnswer(int userID, String exerciseID, int questionID,
                             String audioFile,
                             boolean valid,
                             String audioType, long durationInMillis, boolean correct, float score,
                             boolean recordedWithFlash, String deviceType, String device, String scoreJson, int processDur) {
    //logger.debug("addAudioAnser json = " + scoreJson);
    return answerDAO.addAnswer(this, userID, exerciseID, questionID, "", audioFile, valid,
        audioType,// + (recordedWithFlash ? "" : "_by_WebRTC"),
        durationInMillis, correct, score, deviceType, device, scoreJson, recordedWithFlash, processDur, 0);
  }

  /**
   * @see mitll.langtest.server.audio.AudioFileHelper#getRefAudioAnswerDecoding
   * @param userID
   * @param exerciseID
   * @param audioFile
   * @param durationInMillis
   * @param correct
   * @param isMale
   * @param speed
   * @return
   */
  public long addRefAnswer(int userID, String exerciseID,
                           String audioFile,
                           long durationInMillis, boolean correct,
                           AudioFileHelper.DecodeAlignOutput alignOutput,
                           AudioFileHelper.DecodeAlignOutput decodeOutput,

                           AudioFileHelper.DecodeAlignOutput alignOutputOld,
                           AudioFileHelper.DecodeAlignOutput decodeOutputOld,

                           boolean isMale, String speed) {
    return refresultDAO.addAnswer(this, userID, exerciseID, audioFile, durationInMillis, correct,
        alignOutput,decodeOutput,
        alignOutputOld, decodeOutputOld,
        isMale, speed);
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

  public void destroy() {
    try {
      connection.contextDestroyed();
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
  }

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
  private ExerciseDAO getExerciseDAO()   { return exerciseDAO; }

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
    new AudioExport(serverProps).writeZip(out, typeToSection, getSectionHelper(), exercisesForSelectionState, getLanguage(),
        getAudioDAO(), installPath, configDir, false);
  }

  @Override
  public String getLanguage() {
    return getServerProps().getLanguage();
  }

  public void writeContextZip(OutputStream out, Map<String, Collection<String>> typeToSection) throws Exception {
    Collection<CommonExercise> exercisesForSelectionState = typeToSection.isEmpty() ?
        getExercises() :
        getSectionHelper().getExercisesForSelectionState(typeToSection);
    new AudioExport(serverProps).writeContextZip(out, typeToSection, getSectionHelper(), exercisesForSelectionState, getLanguage(),
        getAudioDAO(), installPath, configDir);
  }

  public void writeZip(OutputStream out) throws Exception {
    new AudioExport(serverProps).writeZipJustOneAudio(out, getSectionHelper(), getExercises(), installPath);
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
  public String writeZip(OutputStream out, long listid, PathHelper pathHelper) throws Exception {
    String language1 = getLanguage();
    if (listid == -1) return language1 + "_Unknown";

    UserList userListByID = getUserListByID(listid);

    if (userListByID == null) {
      logger.error("huh? can't find user list " + listid);
      return language1 + "_Unknown";
    } else {
      //logger.debug("writing contents of " + userListByID);
      long then = System.currentTimeMillis();
      Collection<CommonUserExercise> exercises = userListByID.getExercises();
      for (CommonExercise ex : exercises) {
        userListManager.addAnnotations(ex);
        getAudioDAO().attachAudio(ex, pathHelper.getInstallPath(), configDir);
        //logger.debug("ex " + ex.getID() + " males   " + ex.getUserMap(true));
        //logger.debug("ex " + ex.getID() + " females " + ex.getUserMap(false));
      }
      long now = System.currentTimeMillis();
      logger.debug("\nTook " +(now-then) + " millis to annotate and attach.");
      new AudioExport(serverProps).writeZip(out, userListByID.getName(), getSectionHelper(), exercises, language1,
          getAudioDAO(), installPath, configDir, listid == UserListManager.REVIEW_MAGIC_ID);
    }
    return language1 + "_" + userListByID.getName();
  }

  public int attachAudio(CommonExercise ex) {
    return getAudioDAO().attachAudio(ex, installPath, configDir);
  }

  public String getUserListName(long listid) {
    UserList userListByID = getUserListByID(listid);
    String language1 = getLanguage();
    if (userListByID == null) {
      logger.error("huh? can't find user list " + listid);
      return language1 + "_Unknown";
    } else {
      return language1 + "_" + userListByID.getName();
    }
  }

  public UserList getUserListByID(long listid) {
    return getUserListManager().getUserListByID(listid, getSectionHelper().getTypeOrder());
  }

  public String getPrefix(Map<String, Collection<String>> typeToSection) {
    return new AudioExport(serverProps).getPrefix(getSectionHelper(), typeToSection);
  }

  /**
   * @param serverProps
   * @param site
   * @param mailSupport
   * @param pathHelper
   * @see mitll.langtest.server.LangTestDatabaseImpl#init
   */
  public void doReport(ServerProperties serverProps, String site, MailSupport mailSupport, PathHelper pathHelper) {
    new Report(userDAO, resultDAO, eventDAO, audioDAO).doReport(serverProps, site, mailSupport, pathHelper);
  }

  public void doReport(PathHelper pathHelper) {
    try {
      new Report(userDAO, resultDAO, eventDAO, audioDAO).writeReport(pathHelper);
    } catch (IOException e) {
      logger.error("got " +e);
    }
  }
/*
  public Map<Long, Map<String, Integer>> getUserToDayToRecordings() {
    return new Report(userDAO, resultDAO, eventDAO, audioDAO).getUserToDayToRecordings(null);
  }
*/

  public String toString() {   return "Database : " + this.getClass().toString();  }
}
