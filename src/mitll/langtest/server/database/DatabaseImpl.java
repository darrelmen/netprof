/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.database;

import mitll.langtest.server.*;
import mitll.langtest.server.amas.FileExerciseDAO;
import mitll.langtest.server.audio.DecodeAlignOutput;
import mitll.langtest.server.audio.SLFFile;
import mitll.langtest.server.database.analysis.Analysis;
import mitll.langtest.server.database.connection.DatabaseConnection;
import mitll.langtest.server.database.connection.H2Connection;
import mitll.langtest.server.database.connection.MySQLConnection;
import mitll.langtest.server.database.connection.PostgreSQLConnection;
import mitll.langtest.server.database.contextPractice.ContextPracticeImport;
import mitll.langtest.server.database.custom.*;
import mitll.langtest.server.database.exercise.*;
import mitll.langtest.server.database.instrumentation.EventDAO;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.server.scoring.ParseResultJson;
import mitll.langtest.server.sorter.ExerciseSorter;
import mitll.langtest.shared.*;
import mitll.langtest.shared.amas.AmasExerciseImpl;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.flashcard.AVPHistoryForList;
import mitll.langtest.shared.flashcard.AVPScoreReport;
import mitll.langtest.shared.instrumentation.Event;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.monitoring.Session;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.CollationKey;
import java.util.*;

/**
 * Note with H2 that :  <br></br>
 * * you can corrupt the database if you try to copy a file that's in use by another process. <br></br>
 * * one process can lock the database and make it inaccessible to a second one, seemingly this can happen
 * more easily when H2 lives inside a servlet container (e.g. tomcat). <br></br>
 * * it's not a good idea to close connections, especially in the context of a servlet inside a container, since
 * H2 will return "new" connections that have already been closed.   <br></br>
 * * it's not a good idea to reuse one connection...?  <br></br>
 * <p>
 * User: go22670
 * Date: 5/14/12
 * Time: 11:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class DatabaseImpl<T extends CommonShell> implements Database {
  private static final Logger logger = Logger.getLogger(DatabaseImpl.class);
  private static final int LOG_THRESHOLD = 10;
  private static final String UNKNOWN = "unknown";
  private static final String SIL = "sil";
  private static final String TRANSLITERATION = "transliteration";

  private String installPath;
  private ExerciseDAO<CommonExercise> exerciseDAO = null;

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

  private final String configDir;
  private final ServerProperties serverProps;
  private final LogAndNotify logAndNotify;

  private JsonSupport jsonSupport;

  private static final boolean ADD_DEFECTS = true;
  private UserManagement userManagement;
  private Analysis analysis;
  private final String absConfigDir;
  private SimpleExerciseDAO<AmasExerciseImpl> fileExerciseDAO;

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
    this(serverProps.useH2() ?
            new H2Connection(configDir, dbName, mustAlreadyExist, logAndNotify) :
            serverProps.usePostgres() ?
                new PostgreSQLConnection(dbName, logAndNotify) : new MySQLConnection(dbName, logAndNotify),
        configDir, relativeConfigDir, dbName,
        serverProps,
        pathHelper, logAndNotify);
  }

  public DatabaseImpl(DatabaseConnection connection,
                      String configDir,
                      String relativeConfigDir,
                      String dbName,
                      ServerProperties serverProps,
                      PathHelper pathHelper,
                      LogAndNotify logAndNotify) {
    long then;
    long now;
    this.connection = connection;
    absConfigDir = configDir;
    this.configDir = relativeConfigDir;
    this.serverProps = serverProps;
    this.logAndNotify = logAndNotify;

    try {
      Connection connection1 = getConnection();
      if (connection1 == null) {
        logger.warn("couldn't open connection to database at " + relativeConfigDir + " : " + dbName);
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
    userDAO = new UserDAO(this, getServerProps());
    addRemoveDAO = new AddRemoveDAO(this);

    userExerciseDAO = new UserExerciseDAO(this);
    UserListExerciseJoinDAO userListExerciseJoinDAO = new UserListExerciseJoinDAO(this);
    resultDAO = new ResultDAO(this);
    refresultDAO = new RefResultDAO(this, getServerProps().shouldDropRefResult());
    wordDAO = new WordDAO(this);
    phoneDAO = new PhoneDAO(this);
    audioDAO = new AudioDAO(this, userDAO);
    answerDAO = new AnswerDAO(this, resultDAO);
    userListManager = new UserListManager(userDAO, new UserListDAO(this, userDAO), userListExerciseJoinDAO,
        new AnnotationDAO(this, userDAO),
        new ReviewedDAO(this, ReviewedDAO.REVIEWED),
        new ReviewedDAO(this, ReviewedDAO.SECOND_STATE),
        pathHelper);

    eventDAO = new EventDAO(this, userDAO);

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

    long then = System.currentTimeMillis();
    putBackWordAndPhone();
    long now = System.currentTimeMillis();
    if (now - then > 1000) logger.info("took " + (now - then) + " millis to put back word and phone");
  }

  public ResultDAO getResultDAO() {
    return resultDAO;
  }

  /**
   * @return
   */
  public Analysis getAnalysis() {
    return analysis;
  }

  public RefResultDAO getRefResultDAO() {
    return refresultDAO;
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
/*
    logger.debug("   ------- testing only closeConnection : now " + connection.connectionsOpen() + " open.");
    Connection connection1 = connection.getConnection(this.getClass().toString());
    if (connection1 != null) {
      connection1.close();
    }
*/
  }

  /**
   * Fixes after the fact a bug where we didn't record info in the phone and word tables.
   *
   * @see #initializeDAOs(PathHelper)
   */
  private void putBackWordAndPhone() {
    List<Result> results = resultDAO.getResultsForPractice();
    Map<Integer, Result> idToResult = new HashMap<>();
    //int skipped = 0;
    for (Result r : results) {
      if (r.getJsonScore() != null && r.getJsonScore().length() > 13) {
        idToResult.put(r.getUniqueID(), r);
      } else {
        //  skipped++;
      }
    }

    //logger.info("skipped " + skipped);

    List<WordDAO.Word> all = wordDAO.getAll();
    Set<Integer> already = new HashSet<>();
    for (WordDAO.Word word : all) {
      long rid = word.rid;
      already.add((int) rid);
    }
    //  logger.debug("putBackWordAndPhone current word results " + already.size());
    Set<Integer> allKeys = new HashSet<>(idToResult.keySet());
    //  logger.debug("putBackWordAndPhone before " + allKeys.size());
    allKeys.removeAll(already);
//    logger.debug("putBackWordAndPhone after " + allKeys.size());
    ParseResultJson parseResultJson = new ParseResultJson(getServerProps());
    int count = 0;
    for (Integer key : allKeys) {
      count++;
      Result result = idToResult.get(key);
      String jsonScore = result.getJsonScore();

      if (jsonScore != null && !jsonScore.isEmpty() && !jsonScore.equals("{}")) {
        Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap = parseResultJson.parseJson(jsonScore);
        recordWordAndPhoneInfo(result.getUniqueID(), netPronImageTypeListMap);
      } else {
        logger.warn("skipping empty json for " + key);
      }
    }
    if (count > 0) {
      logger.debug("putBackWordAndPhone fixed " + count);
    }
  }

  private MonitoringSupport getMonitoringSupport() {
    return new MonitoringSupport(userDAO, resultDAO);
  }

  /**
   * @param installPath
   * @param lessonPlanFile
   * @param mediaDir
   * @see mitll.langtest.server.LangTestDatabaseImpl#setInstallPath
   */
  public void setInstallPath(String installPath, String lessonPlanFile, String mediaDir) {
    //  logger.debug("got install path " + installPath + " media " + mediaDir);
    this.installPath = installPath;
    makeDAO(lessonPlanFile, mediaDir, installPath);
    this.jsonSupport = new JsonSupport(getSectionHelper(), getResultDAO(), getRefResultDAO(), getAudioDAO(),
        getPhoneDAO(), configDir, installPath);
  }

  /**
   * @return
   * @see mitll.langtest.server.DownloadServlet#returnSpreadsheet(HttpServletResponse, DatabaseImpl, String)
   * @see DatabaseImpl#getTypeOrder
   */
  public SectionHelper<CommonExercise> getSectionHelper() {
    if (serverProps.isAMAS()) {
      return new SectionHelper<>();
    }

    getExercises();
    return exerciseDAO.getSectionHelper();
  }

  public Collection<String> getTypeOrder() {
    SectionHelper sectionHelper = (serverProps.isAMAS()) ? getAMASSectionHelper() : getSectionHelper();
    if (sectionHelper == null) logger.warn("no section helper for " + this);
    List<String> objects = Collections.emptyList();
    return (sectionHelper == null) ? objects : sectionHelper.getTypeOrder();
  }

  public Collection<SectionNode> getSectionNodes() {
    SectionHelper<?> sectionHelper = (serverProps.isAMAS()) ? getAMASSectionHelper() : getSectionHelper();
    return sectionHelper.getSectionNodes();
  }

  /**
   * @param id
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getResultASRInfo(long, int, int)
   * @see mitll.langtest.server.DownloadServlet#getFilenameForDownload(DatabaseImpl, String, String)
   * @see #deleteItem(String)
   * @see #getCustomOrPredefExercise(String)
   */
  public CommonExercise getExercise(String id) {
    return exerciseDAO.getExercise(id);
  }

  /**
   * @return
   * @see #getExercises()
   */
  public Collection<CommonExercise> getExercises() {
    if (serverProps.isAMAS()) {
      return Collections.emptyList();
    }
    List<CommonExercise> rawExercises = exerciseDAO.getRawExercises();
    if (rawExercises.isEmpty()) {
      logger.warn("getExercises no exercises in " + getServerProps().getLessonPlan() + " at " + installPath);
    }
    return rawExercises;
  }

  public Collection<AmasExerciseImpl> getAMASExercises() {
    return fileExerciseDAO.getRawExercises();
  }

  public AmasExerciseImpl getAMASExercise(String id) {
    return fileExerciseDAO.getExercise(id);
  }

  public SectionHelper<AmasExerciseImpl> getAMASSectionHelper() {
    return fileExerciseDAO.getSectionHelper();
  }

  /**
   * Lazy, latchy instantiation of DAOs.
   * Not sure why it really has to be this way.
   *
   * @param mediaDir
   */
  private void makeDAO(String lessonPlanFile, String mediaDir, String installPath) {
    if (exerciseDAO == null) {
      synchronized (this) {
        boolean isURL = serverProps.getLessonPlan().startsWith("http");
        boolean amas = serverProps.isAMAS();
        if (amas) {
          int numExercises;
//          logger.info("Got " + lessonPlanFile);
          if (isURL) {
            this.fileExerciseDAO = new AMASJSONURLExerciseDAO(getServerProps());
            numExercises = fileExerciseDAO.getNumExercises();
          } else {
            fileExerciseDAO = new FileExerciseDAO<>(mediaDir, serverProps.getLanguage(), absConfigDir, lessonPlanFile, installPath);
            numExercises = fileExerciseDAO.getNumExercises();
          }
          userManagement = new UserManagement(userDAO, numExercises, resultDAO, userListManager);

        } else {
          if (isURL) {
            this.exerciseDAO = new JSONURLExerciseDAO(getServerProps(), userListManager, ADD_DEFECTS);
          } else if (lessonPlanFile.endsWith(".json")) {
            this.exerciseDAO = new JSONExerciseDAO(lessonPlanFile, getServerProps(), userListManager, ADD_DEFECTS);
          } else {
            this.exerciseDAO = new ExcelImport(lessonPlanFile, getServerProps(), userListManager, ADD_DEFECTS);
          }

          userExerciseDAO.setExerciseDAO(exerciseDAO);
          setDependencies(mediaDir, installPath);

          exerciseDAO.getRawExercises();

          userDAO.checkForFavorites(userListManager);
          userExerciseDAO.setAudioDAO(audioDAO);

          userManagement = new UserManagement(userDAO, exerciseDAO.getNumExercises(), resultDAO, userListManager);

          analysis = new Analysis(this, phoneDAO, getExerciseIDToRefAudio());
        }

        audioDAO.setExerciseDAO(exerciseDAO);

        audioDAO.markTranscripts();
      }
    }
  }

  public void reloadExercises() {
    exerciseDAO.reload();
  }

  /**
   * @param mediaDir
   * @param installPath
   * @see #makeDAO(String, String, String)
   */
  private void setDependencies(String mediaDir, String installPath) {
    ExerciseDAO exerciseDAO = this.exerciseDAO;
    setDependencies(mediaDir, installPath, exerciseDAO);
  }

  /**
   * Public for testing only...
   *
   * @param mediaDir
   * @param installPath
   * @param exerciseDAO
   */
  public void setDependencies(String mediaDir, String installPath, ExerciseDAO exerciseDAO) {
    exerciseDAO.setDependencies(mediaDir, installPath, userExerciseDAO, addRemoveDAO, audioDAO);
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
   * @see mitll.langtest.server.LangTestDatabaseImpl#editItem
   * @see mitll.langtest.client.custom.dialog.EditableExerciseDialog#postEditItem
   */
  public void editItem(CommonExercise userExercise) {
    logger.debug("editItem ex #" + userExercise.getID() + " mediaDir : " + getServerProps().getMediaDir() +
        " initially audio was\n\t " + userExercise.getAudioAttributes());

    getUserListManager().editItem(userExercise, true, getServerProps().getMediaDir());

    Set<AudioAttribute> original = new HashSet<>(userExercise.getAudioAttributes());
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
      logger.debug(b ? "removed defects " + original.size() + " now" : " didn't remove any defects - " + defects.size());

      MutableAudioExercise mutableAudio = exercise.getMutableAudio();
      for (AudioAttribute attribute : defects) {
        if (!mutableAudio.removeAudio(attribute)) {
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
   * @see #editItem
   */
  private Set<AudioAttribute> getAndMarkDefects(AudioAttributeExercise userExercise,
                                                Map<String, ExerciseAnnotation> fieldToAnnotation) {
    Set<AudioAttribute> defects = new HashSet<AudioAttribute>();

    for (Map.Entry<String, ExerciseAnnotation> fieldAnno : fieldToAnnotation.entrySet()) {
      if (!fieldAnno.getValue().isCorrect()) {  // i.e. defect
        AudioAttribute audioAttribute = userExercise.getAudioRefToAttr().get(fieldAnno.getKey());
        if (audioAttribute != null) {
          logger.debug("getAndMarkDefects : found defect " + audioAttribute +
              " anno : " + fieldAnno.getValue() +
              " field  " + fieldAnno.getKey());
          // logger.debug("\tmarking defect on audio");
          defects.add(audioAttribute);
          audioDAO.markDefect(audioAttribute);
        } else if (!fieldAnno.getKey().equals(TRANSLITERATION)) {
          logger.warn("\tcan't mark defect on audio : looking for field '" + fieldAnno.getKey() +
              "' in " + userExercise.getAudioRefToAttr().keySet());
        }
      }
    }

    return defects;
  }

  /**
   * @param audioAttribute
   * @see mitll.langtest.server.LangTestDatabaseImpl#markAudioDefect(mitll.langtest.shared.exercise.AudioAttribute, String)
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#getPanelForAudio
   */
  public void markAudioDefect(AudioAttribute audioAttribute) {
    if (audioDAO.markDefect(audioAttribute) < 1) {
      logger.error("huh? couldn't mark error on " + audioAttribute);
    }
  }

  /**
   * @param userid
   * @param typeToSection
   * @return
   * @paramx collator
   * @see mitll.langtest.server.ScoreServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
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
   * For all the exercises in a chapter
   * <p>
   * Get latest results
   * Get phones for latest
   * <p>
   * //Score phones
   * Sort phone scores – asc
   * <p>
   * Map phone->example
   * <p>
   * Join phone->word
   * <p>
   * Sort word by score asc
   *
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
    List<AVPHistoryForList.UserScore> scores = new ArrayList<>();

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
    scores = new ArrayList<>();

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

    List<AVPHistoryForList> historyForLists = new ArrayList<>();
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
    getExercises();
  }

  /**
   * @see LangTestDatabaseImpl#init()
   */
  public void preloadContextPractice() {
    makeContextPractice(getServerProps().getDialogFile(), installPath);
  }

  /**
   * @return
   * @see LangTestDatabaseImpl#getContextPractice()
   */
  public ContextPractice getContextPractice() {
    if (this.contextPractice == null) {
      makeContextPractice(getServerProps().getDialogFile(), installPath);
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
    return userManagement.userExists(request, login, passwordH, serverProps);
  }

  /**
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
    return userManagement.addUser(request, userID, passwordH, emailH, kind, isMale, age, dialect, device);
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
   * <p>
   * Uses return generated keys to get the user id
   * <p>
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
    return userManagement.addUser(age, gender, experience, ipAddr, nativeLang, dialect, userID, permissions, device);
  }

  /**
   * @param out
   * @see mitll.langtest.server.DownloadServlet#returnSpreadsheet(HttpServletResponse, DatabaseImpl, String)
   */
  public void usersToXLSX(OutputStream out) {
    userManagement.usersToXLSX(out);
  }

  public JSON usersToJSON() {
    return userManagement.usersToJSON();
  }

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
   * @param id
   * @param widgetType
   * @param exid
   * @param context
   * @param userid
   * @param device
   * @return
   * @see mitll.langtest.server.ScoreServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  public boolean logEvent(String id, String widgetType, String exid, String context, long userid, String device) {
    return logEvent(id, widgetType, exid, context, userid, UNKNOWN, device);
  }

  public boolean logEvent(String id, String widgetType, String exid, String context, long userid, String hitID,
                          String device) {
    return eventDAO != null && eventDAO.add(new Event(id, widgetType, exid, context, userid, -1, hitID, device));
  }

  public void logAndNotify(Exception e) {
    getLogAndNotify().logAndNotifyServerException(e);
  }

  public EventDAO getEventDAO() {
    return eventDAO;
  }

  public AudioDAO getAudioDAO() {
    return audioDAO;
  }

  private WordDAO getWordDAO() {
    return wordDAO;
  }

  public PhoneDAO getPhoneDAO() {
    return phoneDAO;
  }

  /**
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getResultAlternatives(java.util.Map, long, String, String)
   * @see mitll.langtest.server.LangTestDatabaseImpl#getResults(java.util.Map, long, String)
   */
  public List<MonitorResult> getMonitorResults() {
    List<MonitorResult> monitorResults = resultDAO.getMonitorResults();

    for (MonitorResult result : monitorResults) {
      CommonExercise exercise = getExercise(result.getId());
      if (exercise != null) {
        result.setDisplayID(exercise.getDisplayID());
      }
    }
    return getMonitorResultsWithText(monitorResults);
  }

  /**
   * @param monitorResults
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getResults(java.util.Map, long, String)
   * @see #getMonitorResults()
   */
  public List<MonitorResult> getMonitorResultsWithText(List<MonitorResult> monitorResults) {
    Map<String, CommonExercise> join = getIdToExerciseMap();
    resultDAO.addUnitAndChapterToResults(monitorResults, join);
    return monitorResults;
  }

  private Map<String, CommonExercise> getIdToExerciseMap() {
    Map<String, CommonExercise> join = new HashMap<>();

    for (CommonExercise exercise : getExercises()) {
      join.put(exercise.getID(), exercise);
    }

    if (userExerciseDAO != null && exerciseDAO != null) {
      for (CommonExercise exercise : userExerciseDAO.getAll()) {
        join.put(exercise.getID(), exercise);
      }
    }
    return join;
  }

  /**
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getPerformanceForUser(long, int)
   * @see DatabaseImpl#makeDAO(String, String, String)
   */
  public Map<String, String> getExerciseIDToRefAudio() {
    Map<String, String> join = new HashMap<>();
    populateIDToRefAudio(join, getExercises());
    Collection<CommonExercise> all = userExerciseDAO.getAll();
    exerciseDAO.attachAudio(all);
    populateIDToRefAudio(join, all);
    return join;
  }

  private <T extends Shell & AudioAttributeExercise> void populateIDToRefAudio(Map<String, String> join, Collection<CommonExercise> all) {
    for (CommonExercise exercise : all) {
      String refAudio = exercise.getRefAudio();
      if (refAudio == null) {
        //   logger.warn("getExerciseIDToRefAudio huh? user exercise : no ref audio for " +id);
      } else {
        String id = exercise.getID();
        join.put(id, refAudio);
      }
    }
    if (join.isEmpty()) logger.warn("huh? no ref audio on " + all.size() + " exercises???");
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
/*  public long addAudioAnswer(int userID, String exerciseID, int questionID,
                             String audioFile,
                             boolean valid,
                             String audioType, long durationInMillis, boolean correct, float score,
                             boolean recordedWithFlash, String deviceType, String device, String scoreJson, int processDur
      , String validity, double snr) {
    //logger.debug("addAudioAnser json = " + scoreJson);
    if (valid && scoreJson.isEmpty()) {
      logger.warn("huh? no score json for valid audio " + audioFile + " on " + exerciseID);
    }
*//*    AnswerInfo info =new AnswerInfo(userID,
        exerciseID,
        questionID,
        "",
        audioFile,
        valid,
        audioType,
        durationInMillis,
        correct,
        score,
        deviceType,
        device,
        scoreJson,
        recordedWithFlash,
        processDur,
     //   0,
        validity,
        snr);*//*

    return answerDAO.addAnswer(this, info);
  }*/

  /**
   * @param userID
   * @param exerciseID
   * @param audioFile
   * @param durationInMillis
   * @param correct
   * @param isMale
   * @param speed
   * @return
   * @see mitll.langtest.server.audio.AudioFileHelper#getRefAudioAnswerDecoding
   */
  public long addRefAnswer(int userID, String exerciseID,
                           String audioFile,
                           long durationInMillis, boolean correct,
                           DecodeAlignOutput alignOutput,
                           DecodeAlignOutput decodeOutput,

                           DecodeAlignOutput alignOutputOld,
                           DecodeAlignOutput decodeOutputOld,

                           boolean isMale, String speed) {
    return refresultDAO.addAnswer(this, userID, exerciseID, audioFile, durationInMillis, correct,
        alignOutput, decodeOutput,
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
   * @see mitll.langtest.server.LangTestDatabaseImpl#duplicateExercise
   */
  public CommonExercise duplicateExercise(CommonExercise exercise) {
    logger.debug("to duplicate  " + exercise);

    //logger.debug("anno before " + exercise.getFieldToAnnotation());
    CommonExercise duplicate = getUserListManager().duplicate(exercise);

    if (!exercise.isPredefined()) {
      logger.warn("huh? got non-predef " + exercise);
    }

    SectionHelper sectionHelper = getSectionHelper();

    List<SectionHelper.Pair> pairs = new ArrayList<>();
    for (Map.Entry<String, String> pair : exercise.getUnitToValue().entrySet()) {
      pairs.add(sectionHelper.addExerciseToLesson(duplicate, pair.getKey(), pair.getValue()));
    }
    sectionHelper.addAssociations(pairs);

    getAddRemoveDAO().add(duplicate.getID(), AddRemoveDAO.ADD);
    getExerciseDAO().add(duplicate);

    logger.debug("exercise state " + exercise.getState());

    userListManager.setState(duplicate, exercise.getState(), exercise.getCombinedMutableUserExercise().getCreator());

    logger.debug("duplicate after " + duplicate);

    return duplicate;
  }

  /**
   * @param id
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#deleteItem(String)
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#deleteItem
   */
  public boolean deleteItem(String id) {
    getAddRemoveDAO().add(id, AddRemoveDAO.REMOVE);
    getUserListManager().removeReviewed(id);
    getSectionHelper().removeExercise(getExercise(id));
    return getExerciseDAO().remove(id);
  }

  /**
   * Special code to mask out unit/chapter from database in userexercise table.
   * <p>
   * Must check update times to make sure we don't mask out a newer entry.
   *
   * @param id
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExercise(String, long, boolean)
   */
  public CommonExercise getCustomOrPredefExercise(String id) {
    CommonExercise userEx = getUserExerciseWhere(id);  // allow custom items to mask out non-custom items

    CommonExercise toRet;

    if (userEx == null) {
      toRet = getExercise(id);
    } else {
      //logger.info("got user ex for " + id);
      long updateTime = userEx.getUpdateTime();
      CommonExercise predef = getExercise(id);

      boolean usePredef = predef != null && predef.getUpdateTime() > updateTime;
      toRet = usePredef ? predef : userEx;

      if (predef != null && !usePredef) {
        // DON'T use the unit/chapter from database, at least for now
        userEx.getCombinedMutableUserExercise().setUnitToValue(predef.getUnitToValue());
      }
    }
    return toRet;
  }

  /**
   * @param id
   * @return
   * @see #editItem
   * @see #getCustomOrPredefExercise(String)
   */
  private CommonExercise getUserExerciseWhere(String id) {
    return userExerciseDAO.getWhere(id);
  }

  @Override
  public ServerProperties getServerProps() {
    return serverProps;
  }

  private AddRemoveDAO getAddRemoveDAO() {
    return addRemoveDAO;
  }

  public ExerciseDAO getExerciseDAO() {
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
    new AudioExport(getServerProps()).writeZip(out, typeToSection, getSectionHelper(), exercisesForSelectionState, getLanguage(),
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
    new AudioExport(getServerProps()).writeContextZip(out, typeToSection, getSectionHelper(), exercisesForSelectionState, getLanguage(),
        getAudioDAO(), installPath, configDir);
  }

  public void writeZip(OutputStream out) throws Exception {
    new AudioExport(getServerProps()).writeZipJustOneAudio(out, getSectionHelper(), getExercises(), installPath);
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
    String language = getLanguage();
    if (listid == -1) return language + "_Unknown";

    UserList<CommonShell> userListByID = getUserListByID(listid);

    if (userListByID == null) {
      logger.error("huh? can't find user list " + listid);
      return language + "_Unknown";
    } else {
      //logger.debug("writing contents of " + userListByID);
      long then = System.currentTimeMillis();
      List<CommonExercise> copyAsExercises = new ArrayList<>();

      for (CommonShell ex : userListByID.getExercises()) {
        copyAsExercises.add(getCustomOrPredefExercise(ex.getID()));
      }
      for (CommonExercise ex : copyAsExercises) {
        userListManager.addAnnotations(ex);
        getAudioDAO().attachAudio(ex, pathHelper.getInstallPath(), configDir);
      }
      long now = System.currentTimeMillis();
      logger.debug("\nTook " + (now - then) + " millis to annotate and attach.");
      new AudioExport(getServerProps()).writeZip(out, userListByID.getName(), getSectionHelper(), copyAsExercises, language,
          getAudioDAO(), installPath, configDir, listid == UserListManager.REVIEW_MAGIC_ID);
    }
    return language + "_" + userListByID.getName();
  }

  /**
   * JUST FOR TESTING?
   *
   * @param ex
   * @return
   */
  public int attachAudio(CommonExercise ex) {
    return getAudioDAO().attachAudio(ex, installPath, configDir);
  }

  /**
   * Expensive ?
   * @see ScoreServlet#getJSONExport
   */
  public void attachAllAudio() {
    AudioDAO audioDAO = getAudioDAO();

    Map<String, List<AudioAttribute>> exToAudio = audioDAO.getExToAudio();

    long then = System.currentTimeMillis();
    Collection<CommonExercise> exercises = getExercises();
    for (CommonExercise exercise : exercises) {
      List<AudioAttribute> audioAttributes = exToAudio.get(exercise.getID());
      if (audioAttributes != null) {
        audioDAO.attachAudio(exercise, installPath, configDir, audioAttributes);
      }
      //if (!debug) ensureMP3s(exercise);
      // exercises.add(getJsonForExercise(exercise));
    }
    long now = System.currentTimeMillis();
    logger.info(getLanguage() + " took " + (now-then) + " millis to attachAllAudio to " + exercises.size()+ " exercises");
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

  public UserList<CommonShell> getUserListByID(long listid) {
    return getUserListManager().getUserListByID(listid, getSectionHelper().getTypeOrder());
  }

  public String getPrefix(Map<String, Collection<String>> typeToSection) {
    return new AudioExport(getServerProps()).getPrefix(getSectionHelper(), typeToSection);
  }

  /**
   * @param serverProps
   * @param site
   * @param mailSupport
   * @param pathHelper
   * @see mitll.langtest.server.LangTestDatabaseImpl#init
   */
  public void doReport(ServerProperties serverProps, String site, MailSupport mailSupport, PathHelper pathHelper) {
    getReport("").doReport(serverProps, site, mailSupport, pathHelper);
  }

  /**
   * @param year
   * @param jsonObject
   * @return
   * @see mitll.langtest.server.ScoreServlet#getReport(JSONObject, int)
   */
  public String getReport(int year, JSONObject jsonObject) {
    return getReport("").getReport(jsonObject, year);
  }

  private Report getReport(String prefix) {
    return new Report(userDAO, resultDAO, eventDAO, audioDAO, serverProps.getLanguage(), prefix);
  }

  /**
   * JUST FOR TESTING
   *
   * @param pathHelper
   * @param prefix
   */
  public JSONObject doReport(PathHelper pathHelper, String prefix, int year) {
    try {
      return getReport(prefix).writeReportToFile(pathHelper, serverProps.getLanguage(), year);
    } catch (IOException e) {
      logger.error("got " + e);
      return null;
    }
  }

  public void rememberScore(long resultID, PretestScore asrScoreForAudio) {
    getAnswerDAO().changeAnswer(resultID, asrScoreForAudio.getHydecScore(), asrScoreForAudio.getProcessDur(), asrScoreForAudio.getJson());
    recordWordAndPhoneInfo(resultID, asrScoreForAudio);
  }

  /**
   * @param answer
   * @param answerID
   * @see mitll.langtest.server.audio.AudioFileHelper#getAudioAnswerDecoding
   */
  public void recordWordAndPhoneInfo(AudioAnswer answer, long answerID) {
    PretestScore pretestScore = answer.getPretestScore();
    if (pretestScore == null) {
      logger.debug("recordWordAndPhoneInfo pretest score is null for " + answer + " and " + answerID);
    } else {
      logger.debug("recordWordAndPhoneInfo pretest score is " + pretestScore + " for " + answer + " and " + answerID);
    }
    recordWordAndPhoneInfo(answerID, pretestScore);
  }

  private void recordWordAndPhoneInfo(long answerID, PretestScore pretestScore) {
    if (pretestScore != null) {
      Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap = pretestScore.getsTypeToEndTimes();
      recordWordAndPhoneInfo(answerID, netPronImageTypeListMap);
    }
  }

  /**
   * @param answerID
   * @param netPronImageTypeListMap
   * @see #putBackWordAndPhone()
   */
  private void recordWordAndPhoneInfo(long answerID, Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap) {
    List<TranscriptSegment> words = netPronImageTypeListMap.get(NetPronImageType.WORD_TRANSCRIPT);
    List<TranscriptSegment> phones = netPronImageTypeListMap.get(NetPronImageType.PHONE_TRANSCRIPT);
    if (words != null) {
      int windex = 0;
      int pindex = 0;

      for (TranscriptSegment segment : words) {
        String event = segment.getEvent();
        if (!event.equals(SLFFile.UNKNOWN_MODEL) && !event.equals(SIL)) {
          long wid = getWordDAO().addWord(new WordDAO.Word(answerID, event, windex++, segment.getScore()));
          for (TranscriptSegment pseg : phones) {
            if (pseg.getStart() >= segment.getStart() && pseg.getEnd() <= segment.getEnd()) {
              String pevent = pseg.getEvent();
              if (!pevent.equals(SLFFile.UNKNOWN_MODEL) && !pevent.equals(SIL)) {
                getPhoneDAO().addPhone(new PhoneDAO.Phone(answerID, wid, pevent, pindex++, pseg.getScore()));
              }
            }
          }
        }
      }
    }
  }

  /**
   * @return
   * @see LangTestDatabaseImpl#getMaleFemaleProgress()
   */
  public Map<String, Float> getMaleFemaleProgress() {
    UserDAO userDAO = getUserDAO();
    Map<Long, User> userMapMales = userDAO.getUserMap(true);
    Map<Long, User> userMapFemales = userDAO.getUserMap(false);

    Collection<? extends CommonShell> exercises = getExercises();
    float total = exercises.size();
    Set<String> uniqueIDs = new HashSet<String>();

    for (CommonShell shell : exercises) {
      boolean add = uniqueIDs.add(shell.getID());
      if (!add) {
        logger.warn("getMaleFemaleProgress found duplicate id " + shell.getID() + " : " + shell);
      }
    }
/*    logger.info("found " + total + " total exercises, " +
        uniqueIDs.size() +
        " unique");*/

    return getAudioDAO().getRecordedReport(userMapMales, userMapFemales, total, uniqueIDs);
  }

  public String toString() {
    return "Database : " + this.getClass().toString();
  }

  @Override
  public LogAndNotify getLogAndNotify() {
    return logAndNotify;
  }
}
