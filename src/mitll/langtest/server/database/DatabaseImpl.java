package mitll.langtest.server.database;

<<<<<<< HEAD
=======
import mitll.flashcard.UserState;
>>>>>>> 9ea1717642f00415277fe4e6a352158a7530b162
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
import mitll.langtest.server.database.instrumentation.EventDAO;
import mitll.langtest.shared.AudioAttribute;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonUserExercise;
import mitll.langtest.shared.DLIUser;
import mitll.langtest.shared.ExerciseAnnotation;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.User;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.flashcard.AVPHistoryForList;
import mitll.langtest.shared.grade.Grade;
import mitll.langtest.shared.instrumentation.Event;
import mitll.langtest.shared.monitoring.Session;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.io.OutputStream;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

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

  private String installPath;
  private ExerciseDAO exerciseDAO = null;
  private UserDAO userDAO;
  private DLIUserDAO dliUserDAO;
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
    long then = System.currentTimeMillis();
    connection = new H2Connection(configDir, dbName, mustAlreadyExist);
    long now = System.currentTimeMillis();
    if (now - then > 1000) logger.info("took " + (now - then) + " millis to open database");

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
    then = System.currentTimeMillis();

    initializeDAOs(pathHelper);
    now = System.currentTimeMillis();
    if (now - then > 1000) logger.info("took " + (now - then) + " millis to initialize DAOs");

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

/*    if (DROP_USER) {
      try {
        userDAO.dropUserTable();
        userDAO.createUserTable(this);
      } catch (Exception e) {
        logger.error("got " + e, e);
      }
    }*/
/*    if (DROP_RESULT) {
      logger.info("------------ dropping results table");
      resultDAO.dropResults();
    }*/
    try {
      resultDAO.createResultTable(getConnection());
    } catch (Exception e) {
      logger.error("got " + e, e);  //To change body of catch statement use File | Settings | File Templates.
    }

    try {
      userDAO.createUserTable(this);
      dliUserDAO.createUserTable(this);
      userListManager.setUserExerciseDAO(userExerciseDAO);
    } catch (Exception e) {
      logger.error("got " + e, e);  //To change body of catch statement use File | Settings | File Templates.
    }

    try {
      gradeDAO.createGradesTable(getConnection());
    } catch (SQLException e) {
      logger.error("Got " + e, e);
    }
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
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExercises
   * @return
   */
  public List<CommonExercise> getExercises() { return getExercises(useFile, lessonPlanFile); }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExercise
   * @param id
<<<<<<< HEAD
   * @return
   */
  public CommonExercise getExercise(String id) { return exerciseDAO.getExercise(id); }
=======
   * @param userid
   * @return
   */
  public Exercise getExercise(String id, long userid) {
    Exercise exercise = exerciseDAO.getExercise(id);
    annotateWithTotalRecordings(id, exercise, userid);
    return exercise;
  }

  public void annotateWithTotalRecordings(Exercise exercise, long userid) {
    annotateWithTotalRecordings(exercise.getID(), exercise, userid);
  }

  private void annotateWithTotalRecordings(String id, Exercise exercise, long userid) {
    if (serverProps.isDataCollectMode() && exercise != null) {
      PathHelper helper = new PathHelper();
      List<Result> resultsForExercise = resultDAO.getAllResultsForExercise(id);
     // logger.debug("Annotating " + id + " with " +resultsForExercise.size() + " results");
      Set<Long> regs = new HashSet<Long>();
      Set<Long> slows = new HashSet<Long>();

      exercise.setRefAudio(null);
      exercise.setSlowRefAudio(null);

      for (Result r : resultsForExercise) {
        if (r.valid) {
          String audioType = r.getAudioType();
          if (audioType.equals(Result.AUDIO_TYPE_REGULAR) || audioType.equals(Result.AUDIO_TYPE_FAST_AND_SLOW)) {
            regs.add(r.userid);
            if (r.userid == userid) {
               exercise.setRefAudio(helper.ensureForwardSlashes(r.answer).replaceAll(".wav",".mp3"));
               //logger.debug("now " + id + " ref audio " + exercise.getRefAudio());
            }
          }
          else if (audioType.equals(Result.AUDIO_TYPE_SLOW)) {
            slows.add(r.userid);
            if (r.userid == userid) {
              exercise.setSlowRefAudio(helper.ensureForwardSlashes(r.answer).replaceAll(".wav",".mp3"));
              //logger.debug("now " + id + " slow ref audio " + exercise.getSlowAudioRef());

            }
          }
        }
        else {
          //logger.debug("\t invalid recording " +r + " for " + id);
        }
      }
      exercise.setReg(regs.size());
      exercise.setSlow(slows.size());
    }
    else {
      //logger.debug("not annotating " + id + " since " + serverProps.isDataCollectMode() + " or " + exercise);
    }
  }
>>>>>>> 9ea1717642f00415277fe4e6a352158a7530b162

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

    List<CommonExercise> rawExercises = exerciseDAO.getRawExercises();//getRawExercises(useFile, lessonPlanFile, isExcel);
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
      exerciseDAO.setAudioDAO(audioDAO);

      getRawExercises(useFile, lessonPlanFile, excel);

      userDAO.checkForFavorites(userListManager);
      userExerciseDAO.setAudioDAO(audioDAO);
    }
  }

<<<<<<< HEAD
  private List<CommonExercise> getRawExercises(boolean useFile, String lessonPlanFile, boolean isExcel) {
    if (useFile && !isExcel) {
      if (isWordPairs) {
        ((FileExerciseDAO) exerciseDAO).readWordPairs(lessonPlanFile, doImages);
=======
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
        Exercise exerciseForID = getExercise(candidate, -1);
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
>>>>>>> 9ea1717642f00415277fe4e6a352158a7530b162
      }
      else {
        ((FileExerciseDAO) exerciseDAO).readFastAndSlowExercises(installPath, configDir, lessonPlanFile);
      }
    }
    return exerciseDAO.getRawExercises();
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#editItem(mitll.langtest.shared.custom.UserExercise)
   * @see mitll.langtest.client.custom.EditableExercise#postEditItem
   * @param userExercise
   */
  public void editItem(UserExercise userExercise) {
    logger.debug("editItem " + userExercise.getID() + " mediaDir : " + serverProps.getMediaDir() +" initially audio was\n\t " + userExercise.getAudioAttributes());

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
    }
    else {
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
   *
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
   * @see mitll.langtest.server.LangTestDatabaseImpl#markAudioDefect(mitll.langtest.shared.AudioAttribute, String)
   * @see mitll.langtest.client.custom.ReviewEditableExercise#getPanelForAudio(mitll.langtest.shared.CommonExercise, mitll.langtest.shared.AudioAttribute, mitll.langtest.client.custom.RememberTabAndContent)
   * @param audioAttribute
   */
<<<<<<< HEAD
  public void markAudioDefect(AudioAttribute audioAttribute) {
    if (audioDAO.markDefect(audioAttribute) < 1) {
      logger.error("huh? couldn't mark error on " + audioAttribute);
=======
  public List<Exercise> getUnmodExercises() {
    List<Exercise> exercises = getExercises(useFile, lessonPlanFile);
    return Collections.unmodifiableList(exercises);
  }

  /**
   * Show unanswered questions first, then ones with 1, then 2, then 3,... answers
   * Also be aware of the user's gender -- if you're female, show questions that have no female answers first.
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExercisesInModeDependentOrder
   * @see #getNextExercise(long, boolean, boolean)
   * @param userID so we can show gender aware orderings (i.e. show entries with fewer female responses to females, etc.)
   * @return ordered list of exercises
   */
  public List<Exercise> getExercisesBiasTowardsUnanswered(long userID, boolean useWeights) {
    List<Exercise> rawExercises = getExercises();
    return getExercisesBiasTowardsUnanswered(userID, rawExercises, useWeights);
  }

  /**
   * Make sure we put unanswered by this user at the front, no matter what.
   * Answered exercises go at the end.
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

    List<ResultDAO.SimpleResult> simpleResults = getSimpleResults();

    List<ResultDAO.SimpleResult> resultsForUser = resultDAO.getResultsForUser(userID);

    for (Exercise e : rawExercises) {   idToExercise.put(e.getID(), e); }

    List<Exercise> alreadyByUser = new ArrayList<Exercise>();
    for (ResultDAO.SimpleResult r : resultsForUser) {
      Exercise remove = idToExercise.remove(r.id);
      if (remove != null) alreadyByUser.add(remove);
    }
    rawExercises = new ArrayList<Exercise>(idToExercise.values());
   // logger.debug("after pruning, there are " + rawExercises.size() + " unanswered, "  + alreadyByUser.size() + " answered");

    populateInitialExerciseIDToCount(rawExercises, idToExercise, idToCount, idToWeight, useWeights);

    // only find answers that are for the gender
    getExerciseIDToResultCount(userID, idToCount, simpleResults);

    //logger.debug("getExercisesBiasTowardsUnanswered for " + userID+ " id->count " + idToCount.size());

    Map<String, Integer> idToCountScaled = getScaledIdToCount(idToCount, idToWeight);

  //  logger.debug("getExercisesBiasTowardsUnanswered idToCountScaled " + idToCountScaled);

    SortedMap<Integer, List<String>> countToIds = getCountToExerciseIDs(idToCountScaled);
    //List<String> sampleIDs = countToIds.get(countToIds.firstKey());
   // logger.debug("getExercisesBiasTowardsUnanswered count keys " + countToIds.keySet().size() + " first set sample " + sampleIDs.subList(0, Math.min(sampleIDs.size(),20)));
    /*logger.debug("getExercisesBiasTowardsUnanswered count keys " + countToIds.keySet() + " first set sample (" +
      countToIds.firstKey() +
      ") = " + sampleIDs);*/

    //logger.debug("getExercisesBiasTowardsUnanswered map " + countToIds);
    List<Exercise> exercisesRandomizedPerUser = getExercisesRandomizedPerUser(userID, idToExercise, countToIds, idToWeight);
    exercisesRandomizedPerUser.addAll(alreadyByUser);// put answered at end
    return exercisesRandomizedPerUser;
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
>>>>>>> 9ea1717642f00415277fe4e6a352158a7530b162
    }
  }

  /**
   * TODOx : do all average calc on server!
   *
   * @see mitll.langtest.server.LangTestDatabaseImpl#getUserHistoryForList
   * @see mitll.langtest.client.custom.MyFlashcardExercisePanelFactory.StatsPracticePanel#onSetComplete
   * @paramx listid
   * @return
   */
  public List<AVPHistoryForList> getUserHistoryForList(long userid, Collection<String> ids, long latestResultID) {
    logger.debug("getUserHistoryForList " +userid + " and " + ids.size() + " ids, latest " + latestResultID);

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

<<<<<<< HEAD
    int count = 0;
    List<AVPHistoryForList.UserScore> scores = new ArrayList<AVPHistoryForList.UserScore>();

    for (Session session : sessionsForUserIn2) {
      if (count++ < 10 || session.isLatest()) {
        scores.add(makeScore(count, userMap, session, true));
     }
=======
    return getExercisessRandomizedPerUser(userID, idToExercise, countToIds,idToWeight);
  }*/

  /**
   * Given a map of answer counts to exercise ids at those counts, randomize the order based on the
   * user id, then return a list of Exercises with those ids.
   *
   * @see #getExercisesBiasTowardsUnanswered(long,boolean)
   * @see #getExercisesGradeBalancing(long) (long, String, boolean)
   * @param userID for this user
   * @param idToExercise so we can go from id to exercise
   * @param countToIds statistics about answers for each exercise
   * @return List of exercises in order from least answers to most
   */
  private List<Exercise> getExercisesRandomizedPerUser(long userID,
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
>>>>>>> 9ea1717642f00415277fe4e6a352158a7530b162
    }

    logger.debug("getUserHistoryForList correct scores " + scores);

<<<<<<< HEAD
    if (scores.size() == 11) {
      scores.remove(9);
=======
  /**
   * @see #getExercisesBiasTowardsUnanswered(long,boolean)
   * @param idToExercise
   * @param idToCount
   */
  private void populateInitialExerciseIDToCount(Collection<Exercise> rawExercises,
                                                Map<String, Exercise> idToExercise,
                                                Map<String, Integer> idToCount,
                                                Map<String, Double> idToWeight, boolean useWeights) {
    boolean foundWeights = false;
    for (Exercise e : rawExercises) {
      idToCount.put(e.getID(), 0);
      double weight = !useWeights || e.getWeight() == 0 ? 1 : Math.max(1, Math.log(e.getWeight())); // 1->n
      if (weight > 0) foundWeights = true;
      idToWeight.put(e.getID(), weight);
      idToExercise.put(e.getID(), e);
>>>>>>> 9ea1717642f00415277fe4e6a352158a7530b162
    }
    sessionAVPHistoryForList.setScores(scores);

<<<<<<< HEAD
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
=======
  /**
   * multiple responses by the same user count as one in count->id map.
   *
   * @see #getExercisesBiasTowardsUnanswered(long,boolean)
   * @param userID
   * @paramx userMale
   * @param idToCount exercise id->count
   */
  private void getExerciseIDToResultCount(long userID, Map<String, Integer> idToCount, List<ResultDAO.SimpleResult> results) {
    List<User> users = userDAO.getUsers();

    boolean userMale = userDAO.isUserMale(userID, users);
    getExerciseIDToResultCount(userDAO.getUserMap(userMale, users), results, idToCount);
  }

  /**
   * @see #getExerciseIDToResultCount(long, java.util.Map, java.util.List)
   * @paramx userID
   * @param userMap
   * @param results
   * @param idToCount
   * @return set of exercises already answered by userID
   */
  private void getExerciseIDToResultCount(Map<Long, User> userMap, List<ResultDAO.SimpleResult> results,
                                          Map<String, Integer> idToCount // what gets populated
  ) {
    Map<String, Set<Long>> keyToUsers = new HashMap<String, Set<Long>>();

    for (ResultDAO.SimpleResult r : results) {
      Integer current = idToCount.get(r.id);
      if (current != null) {  // unlikely not null
        if (userMap.containsKey(r.userid)) { // only get male or female results
          String exerciseID = r.id;
          Set<Long> usersForResult = keyToUsers.get(exerciseID);

          if (usersForResult == null) {
            keyToUsers.put(exerciseID, usersForResult = new HashSet<Long>());
          }
          if (!usersForResult.contains(r.userid)) {   // ignore re-recordings
            usersForResult.add(r.userid);
            Integer c = idToCount.get(exerciseID);
            if (c == null) {
              logger.warn("huh? exerciseID " + exerciseID + " not found?");
              idToCount.put(exerciseID, 1);
            } else {
              idToCount.put(exerciseID, c + 1);
            }
          }
        }
      }
    }
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

    List<Exercise> fewResponses = getExercisesRandomizedPerUser(userID, idToExercise, integerListSortedMap, idToWeight);
    Set<String> fewSet = new HashSet<String>();
    for (Exercise e : fewResponses) fewSet.add(e.getID());
   // logger.debug("getExercisesGradeBalancing for " + MIN_INCORRECT_ANSWERS + " fewSet size " + fewSet.size());
    if (fewSet.size() == rawExercises.size()) {
      logger.debug("we haven't gotten enough questions answered yet to bias towards easy questions");
      return fewResponses;
    } else {
      // join results with grades
      then = System.currentTimeMillis();
>>>>>>> 9ea1717642f00415277fe4e6a352158a7530b162

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
                      String nativeLang, String dialect, String userID, Collection<User.Permission> permissions) {
    String ip = getIPInfo(request);
    return addUser(age, gender, experience, ip, nativeLang, dialect, userID, permissions);
  }


  public long addUser(User user) {
    long l;
    if ((l = userDAO.userExists(user.getUserID())) == -1) {
      logger.debug("addUser " + user);
       l = userDAO.addUser(user.getAge(), user.getGender() == 0 ? UserDAO.MALE : UserDAO.FEMALE,
         user.getExperience(), user.getIpaddr(), user.getNativeLang(), user.getDialect(), user.getUserID(), false,
         user.getPermissions());
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
   * @param ipAddr user agent info
   * @param dialect speaker dialect
   * @param permissions
   * @return assigned id
   * @see mitll.langtest.server.LangTestDatabaseImpl#addUser
   */
  public long addUser(int age, String gender, int experience, String ipAddr,
                      String nativeLang, String dialect, String userID, Collection<User.Permission> permissions) {
    logger.debug("addUser " + userID);
    long l = userDAO.addUser(age, gender, experience, ipAddr, nativeLang, dialect, userID, false, permissions);
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
    userDAO.toXLSX(out,getUsers());
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
        Integer numResults = idToCount.idToCount.get(u.getId());
        if (numResults != null) {
          u.setNumResults(numResults);

          if (userToRate.containsKey(u.getId())) {
            u.setRate(userToRate.get(u.getId()));
          }
          int size = idToCount.idToUniqueCount.get(u.getId()).size();
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
    eventDAO.add(new Event(id, widgetID, exid, context, userid, -1, hitID));
  }

  public void logEvent(String exid, String context, long userid) {
    if (context.length()>100) context = context.substring(0,100).replace("\n"," ");
    logEvent("unknown","server",exid,context,userid,"unknown");
  }

  public List<Event> getEvents() {
    return eventDAO.getAll();
  }
  public EventDAO getEventDAO() { return eventDAO; }

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

  Collection<User> joinWithDLIUsers(List<User> users) {
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
  }

<<<<<<< HEAD
=======
  /**
   * @see #getExercisesBiasTowardsUnanswered(long, java.util.Collection, boolean)
   * @return
   */
  private List<ResultDAO.SimpleResult> getSimpleResults() { return resultDAO.getSimpleResults(); }

>>>>>>> 9ea1717642f00415277fe4e6a352158a7530b162
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
   * @param recordedWithFlash
   */
  public long addAudioAnswer(int userID, String plan, String exerciseID, int questionID,
                             String audioFile,
                             boolean valid, boolean flq, boolean spoken,
                             String audioType, int durationInMillis, boolean correct, float score, boolean recordedWithFlash) {
    return answerDAO.addAnswer(this, userID, plan, exerciseID, questionID, "", audioFile, valid, flq, spoken, audioType + (recordedWithFlash ? "" : "_by_WebRTC"),
      durationInMillis, correct, score, "");
  }

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
   * @see mitll.langtest.client.custom.ReviewEditableExercise#duplicateExercise
   * @see mitll.langtest.server.LangTestDatabaseImpl#duplicateExercise(mitll.langtest.shared.custom.UserExercise)
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
   * @see mitll.langtest.server.LangTestDatabaseImpl#deleteItem(String)
   * @see mitll.langtest.client.custom.ReviewEditableExercise#deleteItem(String, long, mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.PagingExerciseList, mitll.langtest.client.list.PagingExerciseList)
   * @param id
   * @return
   */
  public boolean deleteItem(String id ) {
    getAddRemoveDAO().add(id, AddRemoveDAO.REMOVE);
    getUserListManager().removeReviewed(id);
    getSectionHelper().removeExercise(getExercise(id));
    return getExerciseDAO().remove(id);
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExercise
   * @param id
   * @return
   */
  private CommonExercise getUserExerciseWhere(String id) {
    CommonUserExercise where = userExerciseDAO.getWhere(id);
    return where != null ? where : null;
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExercise
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

  /**
   * @see mitll.langtest.server.DownloadServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   * @param out
   * @param typeToSection
   * @throws Exception
   */
  public void writeZip(OutputStream out, Map<String, Collection<String>> typeToSection) throws Exception {
    Collection<CommonExercise> exercisesForSelectionState = typeToSection.isEmpty() ?
      getExercises() :
      getSectionHelper().getExercisesForSelectionState(typeToSection);
    String language1 = getServerProps().getLanguage();

    new AudioExport().writeZip(out, typeToSection, getSectionHelper(), exercisesForSelectionState, language1, getAudioDAO(), installPath, configDir);
  }

  /**
   * @see mitll.langtest.server.DownloadServlet#writeUserList(javax.servlet.http.HttpServletResponse, DatabaseImpl, String)
   * @param out
   * @param listid
   * @return
   * @throws Exception
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
    return "Database : " + connection.getConnection();
  }
}
