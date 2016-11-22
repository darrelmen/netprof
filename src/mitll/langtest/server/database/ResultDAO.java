/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.server.database;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.database.analysis.Analysis;
import mitll.langtest.server.database.excel.ResultDAOToExcel;
import mitll.langtest.server.sorter.ExerciseSorter;
import mitll.langtest.shared.MonitorResult;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.User;
import mitll.langtest.shared.UserAndTime;
import mitll.langtest.shared.analysis.UserPerformance;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.HasID;
import mitll.langtest.shared.exercise.MutableExercise;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.flashcard.ExerciseCorrectAndScore;
import mitll.langtest.shared.monitoring.Session;
import org.apache.log4j.Logger;

import java.io.OutputStream;
import java.sql.*;
import java.text.CollationKey;
import java.text.Collator;
import java.util.*;

import static mitll.langtest.server.database.Database.EXID;

/**
 * Create, drop, alter, read from the results table.
 * Note that writing to the table takes place in the {@link AnswerDAO}. Not sure if that's a good idea or not. :)
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 */
public class ResultDAO extends DAO {
  private static final Logger logger = Logger.getLogger(ResultDAO.class);

  private static final Map<String, String> EMPTY_MAP = new HashMap<>();
  private static final int MINUTE = 60 * 1000;
  private static final int SESSION_GAP = 5 * MINUTE;  // 5 minutes

  public static final String ID = "id";
  public static final String USERID = "userid";
  private static final String PLAN = "plan";
  private static final String QID = "qid";
  public static final String ANSWER = "answer";
  public static final String SCORE_JSON = "scoreJson";
  public static final String WITH_FLASH = "withFlash";
  private static final String VALID = "valid";

  public static final String RESULTS = "results";

  static final String FLQ = "flq";
  static final String SPOKEN = "spoken";
  public static final String AUDIO_TYPE = "audioType";
  public static final String DURATION = "duration";
  public static final String CORRECT = "correct";
  public static final String PRON_SCORE = "pronscore";
  private static final String STIMULUS = "stimulus";
  public static final String DEVICE_TYPE = "deviceType";  // iPad, iPhone, browser, etc.
  public static final String DEVICE = "device"; // device id, or browser type
  public static final String PROCESS_DUR = "processDur";
  public static final String ROUND_TRIP_DUR = "roundTripDur";
  // public static final int FIVE_MINUTES = 5 * 60 * 1000;
  // public static final int HOUR = 60 * 60 * 1000;
  // public static final int DAY = 24 * HOUR;
  private static final String DEVICETYPE = "devicetype";
  public static final String VALIDITY = "validity";
  public static final String SNR = "SNR";
  private static final String SESSION = "session"; // from ODA

  static final String USER_SCORE = "userscore";
  static final String TRANSCRIPT = "transcript";
  public static final String MODEL = "model";
  public static final String MODELUPDATE = "modelupdate";

  private final boolean DEBUG = false;

  /**
   * @param database
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs(mitll.langtest.server.PathHelper)
   */
  public ResultDAO(Database database) {
    super(database);
  }

  //private List<Result> cachedResultsForQuery = null;
  private List<CorrectAndScore> cachedResultsForQuery2 = null;
  private List<MonitorResult> cachedMonitorResultsForQuery = null;

  /**
   * @param id
   * @param phoneDAO
   * @param minRecordings
   * @param exToRef
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getPerformanceForUser
   * @see mitll.langtest.client.analysis.AnalysisPlot#AnalysisPlot
   */
  public UserPerformance getPerformanceForUser(long id, PhoneDAO phoneDAO, int minRecordings, Map<String, String> exToRef) {
    return new Analysis(database, phoneDAO, exToRef).getPerformanceForUser(id, minRecordings);
  }

  /**
   * Pulls the list of results out of the database.
   *
   * @return
   * @see UserManagement#populateUserToNumAnswers
   * @see #getUserToResults
   * @seex Report#getResults(StringBuilder, Set, JSONObject, int)
   */
  public List<Result> getResults() {
    try {
/*      synchronized (this) {
        if (cachedResultsForQuery != null) {
          return cachedResultsForQuery;
        }
      }*/
      String sql = "SELECT * FROM " + RESULTS;
      List<Result> resultsForQuery = getResultsSQL(sql);

/*      synchronized (this) {
        cachedResultsForQuery = resultsForQuery;
      }*/
      return resultsForQuery;
    } catch (Exception ee) {
      logException(ee);
    }
    return new ArrayList<>();
  }

  Collection<UserAndTime> getUserAndTimes() {
    try {
      String sql = "SELECT " + USERID + "," + EXID + "," + Database.TIME + ", "
          + QID + " FROM " + RESULTS;
      return getUserAndTimeSQL(sql);
    } catch (Exception ee) {
      logException(ee);
    }
    return new ArrayList<>();
  }

  List<Result> getResultsForPractice() {
    try {
/*      synchronized (this) {
        if (cachedResultsForQuery != null) {
          return cachedResultsForQuery;
        }
      }*/
      String sql = "SELECT * FROM " + RESULTS + " where " + AUDIO_TYPE + " is not null";
      List<Result> resultsForQuery = getResultsSQL(sql);

/*      synchronized (this) {
        cachedResultsForQuery = resultsForQuery;
      }*/
      return resultsForQuery;
    } catch (Exception ee) {
      logException(ee);
    }
    return new ArrayList<>();
  }

  List<Result> getResultsDevices() {
    try {
      String sql = "SELECT * FROM " + RESULTS + " where " + DEVICETYPE + " like 'i%'";
      return getResultsSQL(sql);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
      logException(ee);
    }
    return new ArrayList<>();
  }

  /**
   * So when updating old data that is missing word and phone alignment information, we have to put it back.
   *
   * @return to re-process
   * @see mitll.langtest.server.decoder.RefResultDecoder#doMissingInfo
   */
  public List<Result> getResultsToDecode() {
    try {
      String scoreJsonClause = " AND " +
          "(" +
          SCORE_JSON + " IS NULL OR " +
          SCORE_JSON +
          " = '{}' " +
          "OR " + SCORE_JSON + " = '{\"words\":[]}')";
      //  scoreJsonClause = "";

      String sql = "SELECT" +
          " * " +
          " FROM " +
          RESULTS +
          " where " +
          PRON_SCORE +
          ">=0" + " AND " + AUDIO_TYPE + " != 'regular' " +
          " AND " + AUDIO_TYPE + " != 'slow' " +
          " AND " + VALID + "=true " +
          " AND " + DURATION + ">0.7 " +
          scoreJsonClause;

      logger.info("sql\n" + sql);
      return getResultsSQL(sql);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
      logException(ee);
    }
    return new ArrayList<>();
  }

  /**
   * @see #getSessions
   * @return
   */
  private List<CorrectAndScore> getCorrectAndScores() {
    try {
      synchronized (this) {
        if (cachedResultsForQuery2 != null) {
          return cachedResultsForQuery2;
        }
      }
      List<CorrectAndScore> resultsForQuery = getScoreResultsSQL(getCSSelect() + " FROM " + RESULTS);

      synchronized (this) {
        cachedResultsForQuery2 = resultsForQuery;
      }
      return resultsForQuery;
    } catch (Exception ee) {
      logException(ee);
    }
    return new ArrayList<>();
  }

  /**
   * @return
   * @see DatabaseImpl#getMonitorResults()
   */
  List<MonitorResult> getMonitorResults() {
    try {
      synchronized (this) {
        if (cachedMonitorResultsForQuery != null) {
          return cachedMonitorResultsForQuery;
        }
      }
      List<MonitorResult> resultsForQuery = getMonitorResultsSQL("SELECT * FROM " + RESULTS);

      synchronized (this) {
        cachedMonitorResultsForQuery = resultsForQuery;
      }
      return resultsForQuery;
    } catch (Exception ee) {
      logException(ee);
    }

    return new ArrayList<>();
  }

  public Result getResultByID(long id) {
    String sql = "SELECT * FROM " + RESULTS + " WHERE " + ID + "='" + id + "'";
    try {
      List<Result> resultsSQL = getResultsSQL(sql);
      if (resultsSQL.size() > 1) {
        logger.error("for " + id + " got " + resultsSQL);
      } else if (resultsSQL.isEmpty()) {
        logger.error("no result for " + id);
      }
      return resultsSQL.isEmpty() ? null : resultsSQL.iterator().next();
    } catch (SQLException e) {
      logger.error("Got " + e, e);
    }
    return null;
  }


  /**
   * Add info from exercises.
   *
   * @param monitorResults
   * @param join
   * @see mitll.langtest.server.database.DatabaseImpl#getMonitorResultsWithText(java.util.List)
   */
  void addUnitAndChapterToResults(List<MonitorResult> monitorResults, Map<String, CommonExercise> join) {
    int n = 0;
    Set<String> unknownIDs = new HashSet<>();
    logger.info("addUnitAndChapterToResults  " + monitorResults.size());
    int c = 0;
    int t = 0;
    for (MonitorResult result : monitorResults) {
      String id = result.getExID();
      if (id.contains("\\/")) id = id.substring(0, id.length() - 2);
      CommonExercise exercise = join.get(id);
      if (exercise == null) {
//        if (n < 5) {
//          logger.error("addUnitAndChapterToResults : for exid " + id + " couldn't find " + result);
//        }
        unknownIDs.add(id);
        n++;
        result.setUnitToValue(EMPTY_MAP);
        //result.setForeignText("");
      } else {
        result.setUnitToValue(exercise.getUnitToValue());
        if (result.getForeignText().isEmpty()) {
          result.setForeignText(exercise.getForeignLanguage());
//          if (c <10) logger.info("setting fl for " + result.getUniqueID() + " " + result.getExID() + " to " + exercise.getForeignLanguage());
          c++;
        } else {
          t++;
        }
      }
    }
    if (n > 0) {
      logger.warn("addUnitAndChapterToResults : skipped " + n + " out of " + monitorResults.size() +
          " # bad join ids = " + unknownIDs.size());
    }
    logger.info("addUnitAndChapterToResults set " + c + " results, used transcript for " + t);

  }

  public List<MonitorResult> getMonitorResultsByID(String id) {
    try {
      String sql = "SELECT * FROM " + RESULTS + " WHERE " + EXID + "='" + id + "'";
      return getMonitorResultsSQL(sql);
    } catch (Exception ee) {
      logException(ee);
    }

    return new ArrayList<>();
  }

  /**
   * For a set of exercise ids, find the results for each and make a map of user->results
   * Then for each user's results, make a list of sessions representing a sequence of grouped results
   * A session will have statistics - # correct, avg pronunciation score, maybe duration, etc.
   *
   * @param ids            only those items that were actually practiced (or scored)
   * @param latestResultID
   * @param userid         who did them
   * @param allIds         all the item ids in the chapter or set of chapters that were covered
   * @param idToKey
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#getUserHistoryForList
   * @see mitll.langtest.client.flashcard.StatsFlashcardFactory.StatsPracticePanel#onSetComplete()
   */
  SessionsAndScores getSessionsForUserIn2(Collection<String> ids,
                                          long latestResultID,
                                          long userid,
                                          Collection<String> allIds,
                                          Map<String, CollationKey> idToKey) {
    List<Session> sessions = new ArrayList<>();
    Map<Long, List<CorrectAndScore>> userToAnswers = populateUserToAnswers(getResultsForExIDIn(ids, true));
    if (DEBUG) logger.debug("getSessionsForUserIn2 Got " + userToAnswers.size() + " user->answer map");
    for (Map.Entry<Long, List<CorrectAndScore>> userToResults : userToAnswers.entrySet()) {
      List<Session> c = partitionIntoSessions2(userToResults.getValue(), ids, latestResultID);
      if (DEBUG)
        logger.debug("\tgetSessionsForUserIn2 found " + c.size() + " sessions for user " + userToResults.getKey() +
            " ids " + ids + " given " + userToResults.getValue().size() + " correct and score");

      sessions.addAll(c);
    }

    List<CorrectAndScore> results = getResultsForExIDInForUser(allIds, true, userid);
    if (DEBUG)
      logger.debug("getSessionsForUserIn2 found " + results.size() + " results for " + allIds.size() + " items");

    List<ExerciseCorrectAndScore> sortedResults = getSortedAVPHistory(results, allIds, idToKey);
    if (DEBUG) logger.debug("getSessionsForUserIn2 found " + sessions.size() + " sessions for " + ids);

    return new SessionsAndScores(sessions, sortedResults);
  }

  /**
   * @param exercises
   * @param userid
   * @param collator
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExerciseIds
   */
  public <T extends CommonShell> List<T> getExercisesSortedIncorrectFirst(Collection<T> exercises, long userid, Collator collator) {
    List<String> allIds = new ArrayList<>();
    Map<String, T> idToEx = new HashMap<>();
    Map<String, CollationKey> idToKey = new HashMap<>();
    for (T exercise : exercises) {
      String id = exercise.getID();
      allIds.add(id);
      idToEx.put(id, exercise);
      //  idToKey.put(id,exercise.getForeignLanguage());
      CollationKey collationKey = collator.getCollationKey(exercise.getForeignLanguage());
      idToKey.put(id, collationKey);
    }

    List<ExerciseCorrectAndScore> sortedResults = getExerciseCorrectAndScores(userid, allIds, idToKey);

    List<T> commonExercises = new ArrayList<>(exercises.size());
    for (ExerciseCorrectAndScore score : sortedResults) {
      commonExercises.add(idToEx.get(score.getId()));
    }
    return commonExercises;
  }

  /**
   * @param userid
   * @param allIds
   * @param idToKey
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#getJsonScoreHistory
   * @see ResultDAO#getExercisesSortedIncorrectFirst
   */
  private List<ExerciseCorrectAndScore> getExerciseCorrectAndScores(long userid, List<String> allIds,
                                                                    Map<String, CollationKey> idToKey) {
    List<CorrectAndScore> results = getResultsForExIDInForUser(allIds, true, userid);
    // if (DEBUG) logger.DEBUG("found " + results.size() + " results for " + allIds.size() + " items");
    return getSortedAVPHistory(results, allIds, idToKey);
  }

  public List<ExerciseCorrectAndScore> getExerciseCorrectAndScoresByPhones(long userid, List<String> allIds,
                                                                           Map<String, CommonExercise> idToEx,
                                                                           ExerciseSorter sorter) {
    List<CorrectAndScore> results = getResultsForExIDInForUser(allIds, true, userid);
    // if (DEBUG) logger.DEBUG("found " + results.size() + " results for " + allIds.size() + " items");
    return getSortedAVPHistoryByPhones(results, allIds, idToEx, sorter);
  }

  /**
   * @param results
   * @param allIds
   * @param idToKey
   * @return
   * @see #getExerciseCorrectAndScores
   */
  private List<ExerciseCorrectAndScore> getSortedAVPHistory(List<CorrectAndScore> results, Collection<String> allIds,
                                                            final Map<String, CollationKey> idToKey) {
    List<ExerciseCorrectAndScore> sortedResults = getExerciseCorrectAndScores(results, allIds);
    Collections.sort(sortedResults, new Comparator<ExerciseCorrectAndScore>() {
      @Override
      public int compare(ExerciseCorrectAndScore o1, ExerciseCorrectAndScore o2) {
        CollationKey fl = idToKey.get(o1.getId());
        CollationKey otherFL = idToKey.get(o2.getId());
        return compareTo(o1, o2, fl, otherFL);
      }
    });
    return sortedResults;
  }

  private int compareTo(ExerciseCorrectAndScore o1, ExerciseCorrectAndScore o2, CollationKey fl, CollationKey otherFL) {
    if (o1.isEmpty() && o2.isEmpty()) {
      return fl.compareTo(otherFL);
    } else if (o1.isEmpty()) return -1;
    else if (o2.isEmpty()) return +1;
    else { // neither is empty
      return compScores(o1, o2);
    }
  }

  /**
   * @param results
   * @param allIds
   * @param idToEx
   * @param sorter
   * @return
   * @see #getExerciseCorrectAndScoresByPhones
   */
  private List<ExerciseCorrectAndScore> getSortedAVPHistoryByPhones(List<CorrectAndScore> results, Collection<String> allIds,
                                                                    final Map<String, CommonExercise> idToEx,
                                                                    final ExerciseSorter sorter
  ) {
    List<ExerciseCorrectAndScore> sortedResults = getExerciseCorrectAndScores(results, allIds);
    Collections.sort(sortedResults, new Comparator<ExerciseCorrectAndScore>() {
      @Override
      public int compare(ExerciseCorrectAndScore o1, ExerciseCorrectAndScore o2) {
        CommonExercise o1Ex = idToEx.get(o1.getId());
        CommonExercise o2Ex = idToEx.get(o2.getId());
        return compareUsingPhones(o1, o2, o1Ex, o2Ex, sorter);
      }
    });
    return sortedResults;
  }

  private int compareUsingPhones(ExerciseCorrectAndScore o1, ExerciseCorrectAndScore o2,
                                 CommonExercise o1Ex, CommonExercise o2Ex, final ExerciseSorter sorter) {
    if (o1.isEmpty() && o2.isEmpty()) {
      return sorter.phoneCompByFirst(o1Ex, o2Ex);
    } else if (o1.isEmpty()) return -1;
    else if (o2.isEmpty()) return +1;
    else { // neither is empty
      return compScoresPhones(o1, o2, o1Ex, o2Ex, sorter);
    }
  }

  /**
   * @param o1
   * @param o2
   * @return
   * @see #compareTo
   */
  private int compScores(ExerciseCorrectAndScore o1, ExerciseCorrectAndScore o2) {
    int myI = o1.getDiff();
    int oI = o2.getDiff();
    int i = myI < oI ? -1 : myI > oI ? +1 : 0;
    if (i == 0) {
      float myScore = o1.getAvgScore();
      float otherScore = o2.getAvgScore();
      int comp = new Float(myScore).compareTo(otherScore);
      return comp == 0 ? o1.getId().compareTo(o2.getId()) : comp;
    } else {
      return i;
    }
  }

  /**
   * @param o1
   * @param o2
   * @param o1Ex
   * @param o2Ex
   * @param sorter
   * @return
   * @see #compareUsingPhones
   */
  private int compScoresPhones(ExerciseCorrectAndScore o1, ExerciseCorrectAndScore o2,
                               CommonExercise o1Ex, CommonExercise o2Ex, final ExerciseSorter sorter) {
    int myI = o1.getDiff();
    int oI = o2.getDiff();
    int i = myI < oI ? -1 : myI > oI ? +1 : 0;
    if (i == 0) {
      float myScore = o1.getAvgScore();
      float otherScore = o2.getAvgScore();
      int comp = new Float(myScore).compareTo(otherScore);
      return (comp == 0) ? sorter.phoneCompByFirst(o1Ex, o2Ex) : comp;
    } else {
      return i;
    }
  }

  /**
   * @param results
   * @param allIds
   * @return
   * @see #getSortedAVPHistory(List, Collection, Map)
   */
  private List<ExerciseCorrectAndScore> getExerciseCorrectAndScores(List<CorrectAndScore> results, Collection<String> allIds) {
    SortedMap<String, ExerciseCorrectAndScore> idToScores = new TreeMap<>();
    if (results != null) {
      for (CorrectAndScore r : results) {
        String id = r.getId();
        ExerciseCorrectAndScore correctAndScores = idToScores.get(id);
        if (correctAndScores == null) idToScores.put(id, correctAndScores = new ExerciseCorrectAndScore(id));
        correctAndScores.add(r);
      }
    }
    for (ExerciseCorrectAndScore exerciseCorrectAndScore : idToScores.values()) {
      exerciseCorrectAndScore.sort();
    }

    for (String id : allIds) {
      if (!idToScores.containsKey(id)) idToScores.put(id, new ExerciseCorrectAndScore(id));
    }
    return new ArrayList<>(idToScores.values());
  }

  static class SessionsAndScores {
    final List<Session> sessions;
    final List<ExerciseCorrectAndScore> sortedResults;

    SessionsAndScores(List<Session> sessions, List<ExerciseCorrectAndScore> sortedResults) {
      this.sessions = sessions;
      this.sortedResults = sortedResults;
    }

    public String toString() {
      return "# sessions =  " + sessions.size() + " results " + sortedResults.size();
    }
  }

  /**
   * @param userID
   * @param firstExercise
   * @param isFlashcardRequest
   * @see mitll.langtest.server.LangTestDatabaseImpl#attachScoreHistory(long, mitll.langtest.shared.exercise.CommonExercise, boolean)
   */
  public void attachScoreHistory(long userID, CommonExercise firstExercise, boolean isFlashcardRequest) {
    List<CorrectAndScore> resultsForExercise = getCorrectAndScores(userID, firstExercise, isFlashcardRequest);

    //logger.DEBUG("score history " + resultsForExercise);
    int total = 0;
    float scoreTotal = 0f;
    for (CorrectAndScore r : resultsForExercise) {
      float pronScore = r.getScore();
      if (pronScore > 0) { // overkill?
        total++;
        scoreTotal += pronScore;
      }
    }
    MutableExercise mutable = firstExercise.getMutable();
    mutable.setScores(resultsForExercise);
    mutable.setAvgScore(total == 0 ? 0f : scoreTotal / total);
  }

  /**
   * @param userID
   * @param firstExercise
   * @param isFlashcardRequest
   * @return
   * @see #attachScoreHistory
   */
  private List<CorrectAndScore> getCorrectAndScores(long userID, HasID firstExercise, boolean isFlashcardRequest) {
    return getResultsForExIDInForUser(userID, isFlashcardRequest, firstExercise.getID());
  }

  /**
   * Only take latest five.
   *
   * @param userID
   * @param id
   * @return
   * @see mitll.langtest.server.ScoreServlet#doGet
   */
/*  public JSONObject getHistoryAsJson(long userID, String id) {
    List<CorrectAndScore> resultsForExercise = getResultsForExIDInForUser(userID, true, id);
    int size = resultsForExercise.size();
    if (size > LAST_NUM_RESULTS) {
      resultsForExercise = resultsForExercise.subList(size - LAST_NUM_RESULTS, size);
    }
//    logger.DEBUG("score history " + resultsForExercise);
    int total = 0;
    float scoreTotal = 0f;
    net.sf.json.JSONArray jsonArray = new net.sf.json.JSONArray();

    for (CorrectAndScore r : resultsForExercise) {
      float pronScore = r.getScore();
      if (pronScore > 0) { // overkill?
        total++;
        scoreTotal += pronScore;
      }
      jsonArray.add(r.isCorrect() ? YES : NO);
    }

    JSONObject jsonObject = new JSONObject();
    jsonObject.put("score", total == 0 ? 0f : scoreTotal / new Integer(total).floatValue());
    jsonObject.put("history", jsonArray);
    return jsonObject;
  }*/
  public List<CorrectAndScore> getResultsForExIDInForUser(long userID, boolean isFlashcardRequest, String id) {
    //return getResultsForExIDInForUser(Collections.singleton(id), isFlashcardRequest, userID);

    try {
      String sql = getCSSelect() + " FROM " + RESULTS + " WHERE " +
          EXID + " ='" + id + "' AND "+
          USERID + "=? AND " +
          VALID + "=true" +
          (isFlashcardRequest ? " AND " + getAVPClause(true) : "")
          ;

      return getCorrectAndScores(1, userID, sql);
    } catch (Exception ee) {
      logger.error("exception getting results for user " + userID + " and id " + id);
      logException(ee);
    }
    return new ArrayList<>();
  }

  /**
   * Only take avp audio type and *valid* audio.
   *
   * @param ids
   * @param matchAVP
   * @return
   * @see #getSessionsForUserIn2
   */

  private List<CorrectAndScore> getResultsForExIDIn(Collection<String> ids, boolean matchAVP) {
    try {
      String list = getInList(ids);

      String sql = getCSSelect() + " FROM " + RESULTS + " WHERE " +
          VALID + "=true" +

          (matchAVP ? " AND " + getAVPClause(matchAVP) : "") +
          " AND " +

          EXID + " in (" + list + ")" +
          " order by " + Database.TIME + " asc";

      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement = connection.prepareStatement(sql);

      List<CorrectAndScore> scores = getScoreResultsForQuery(connection, statement, sql);

      if (DEBUG) logger.debug("getResultsForExIDIn for  " + sql + " got\n\t" + scores.size());
      return scores;
    } catch (Exception ee) {
      logException(ee);
    }
    return new ArrayList<>();
  }


  /**
   * JUST FOR AMAS!
   * @param ids
   * @param userid
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getScoresForUser
   */
  public List<CorrectAndScore> getResultsForExIDInForUser(Collection<String> ids, long userid, String session) {
    try {
      String list = getInList(ids);

      String sessionClause = session != null && !session.isEmpty() ? SESSION + "='" + session + "' AND " : "";
      String sql = getCSSelect() + " FROM " + RESULTS + " WHERE " +
          USERID + "=? AND " +
          sessionClause +

          VALID + "=true" +
          " AND " +
          EXID + " in (" + list + ")";

      return getCorrectAndScoresForUser(userid, sql);
    } catch (Exception ee) {
      logger.error("exception getting results for user " + userid + " and ids " + ids);
      logException(ee);
    }
    return new ArrayList<>();
  }

  /**
   * JUST FOR AMAS
   * @param userid
   * @param sql
   * @return
   * @throws SQLException
   */
  private List<CorrectAndScore> getCorrectAndScoresForUser(long userid, String sql) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement(sql);
    statement.setLong(1, userid);

    List<CorrectAndScore> scores = getScoreResultsForQuery(connection, statement, sql);

    //  if (sql.contains("session")) {
    //    logger.DEBUG("getCorrectAndScoresForUser for  " + sql + " got " + scores.size() + " scores");
    //  }
    return scores;
  }

  /**
   * TODOx : inefficient - better ways to do this in h2???
   * Long story here on h2 support (or lack of) for efficient in query...
   *
   * @param ids
   * @param matchAVP
   * @param userid
   * @return
   * @see #getSessionsForUserIn2
   * @see #attachScoreHistory(long, mitll.langtest.shared.exercise.CommonExercise, boolean)
   * @see mitll.langtest.server.database.DatabaseImpl#getJsonScoreHistory
   */
  private List<CorrectAndScore> getResultsForExIDInForUser(Collection<String> ids, boolean matchAVP, long userid) {
    try {
      String list = getInList(ids);

      String sql = getCSSelect() + " FROM " + RESULTS + " WHERE " +
          USERID + "=? AND " +
          VALID + "=true" +
          (matchAVP ? " AND " + getAVPClause(matchAVP) : "") +
          " AND " +
          EXID + " in (" + list + ")";

      return getCorrectAndScores(ids.size(), userid, sql);
    } catch (Exception ee) {
      logger.error("exception getting results for user " + userid + " and ids " + ids);
      logException(ee);
    }
    return new ArrayList<>();
  }

  /**
   * @see #getResultsForExIDInForUser(long, boolean, String)
   * @param numIDs
   * @param userid
   * @param sql
   * @return
   * @throws SQLException
   */
  private List<CorrectAndScore> getCorrectAndScores(int numIDs, long userid, String sql) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement(sql);
    statement.setLong(1, userid);

    long then = System.currentTimeMillis();
    List<CorrectAndScore> scores = getScoreResultsForQuery(connection, statement, sql);
    long now = System.currentTimeMillis();

    if (now - then > 100) {
      logger.warn("getCorrectAndScores " + getLanguage() +
          " took " + (now - then) + " millis : " +
          " query for " + numIDs + " and userid " + userid + " returned " + scores.size() + " scores"
      //   + ", sql:\n\t" + sql
      );
    }
    if (DEBUG) {
      logger.debug("getCorrectAndScores for  " + sql + " got\n\t" + scores.size());
    }
    return scores;
  }

  private String getAVPClause(boolean matchAVP) {
    return matchAVP ?
        "(" +
            AUDIO_TYPE + " LIKE 'avp%' OR " +
            AUDIO_TYPE + " = 'flashcard' OR " +
            AUDIO_TYPE + " = 'practice' " +
            ")" :
        "(" +
            AUDIO_TYPE + " NOT LIKE 'avp%' AND " +
            AUDIO_TYPE + " <> 'flashcard' AND " +
            AUDIO_TYPE + " <> 'practice' " +
            ")";
  }

  private String getCSSelect() {
    return "SELECT " + ID + ", " + USERID + ", " +
        EXID + ", " + Database.TIME + ", " + CORRECT + ", " + PRON_SCORE + ", " + ANSWER + ", " + SCORE_JSON;
  }

  /**
   * @param sql
   * @return
   * @throws SQLException
   * @see #getResults()
   * @see #getResultsDevices()
   */
  private List<Result> getResultsSQL(String sql) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement(sql);

    return getResultsForQuery(connection, statement, sql);
  }

  private List<MonitorResult> getMonitorResultsSQL(String sql) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement(sql);

    return getMonitorResultsForQuery(connection, statement, sql);
  }

  private List<UserAndTime> getUserAndTimeSQL(String sql) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement(sql);

    return getUserAndTimeForQuery(connection, statement, sql);
  }

  /**
   * @see mitll.langtest.server.database.AnswerDAO#addAnswerToTable
   */
  synchronized void invalidateCachedResults() {
    cachedResultsForQuery2 = null;
    cachedMonitorResultsForQuery = null;
  }

  public int getNumResults() {
    int numResults = 0;
    try {
      Connection connection = database.getConnection(this.getClass().toString());
      String sql = "SELECT COUNT(*) FROM " + RESULTS;
      PreparedStatement statement = connection.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        numResults = rs.getInt(1);
      }
      finish(connection, statement, rs, sql);
    } catch (Exception ee) {
      logException(ee);
    }
    return numResults;
  }

  /**
   * Get a list of Results for this Query.
   *
   * @param connection
   * @param statement
   * @param sql
   * @return
   * @throws SQLException
   * @see #getResultsSQL(String)
   */
  private List<Result> getResultsForQuery(Connection connection, PreparedStatement statement, String sql) throws SQLException {
    ResultSet rs = statement.executeQuery();
    List<Result> results = new ArrayList<>();
    long then = System.currentTimeMillis();
    while (rs.next()) {
      int uniqueID = rs.getInt(ID);
      long userID = rs.getLong(USERID);
      String plan = rs.getString(PLAN);
      String exid = rs.getString(EXID);
      int qid = rs.getInt(QID);
      Timestamp timestamp = rs.getTimestamp(Database.TIME);
      String answer = rs.getString(ANSWER);
      boolean valid = rs.getBoolean(VALID);
      String type = rs.getString(AUDIO_TYPE);
      int dur = rs.getInt(DURATION);

      boolean correct = rs.getBoolean(CORRECT);
      float pronScore = rs.getFloat(PRON_SCORE);
      String json = rs.getString(SCORE_JSON);
      String device = rs.getString(DEVICE);
      String model = rs.getString(MODEL);

      Result result = new Result(uniqueID, userID, //id
          plan, // plan
          exid, // id
          qid, // qid
          trimPathForWebPage2(answer), // answer
          valid, // valid
          timestamp.getTime(),

          type, dur, correct, pronScore, device,
          model);
      result.setJsonScore(json);
      results.add(result);
    }
    finish(connection, statement, rs, sql);
    long now = System.currentTimeMillis();
    long diff = now - then;
    if (diff > 100) logger.warn("took " + diff + " to get " + results.size() + " results");
    return results;
  }

  private List<UserAndTime> getUserAndTimeForQuery(Connection connection, PreparedStatement statement, String sql) throws SQLException {
    ResultSet rs = statement.executeQuery();
    List<UserAndTime> results = new ArrayList<>();
    while (rs.next()) {
      long userID = rs.getLong(USERID);
      String exid = rs.getString(EXID);
      Timestamp timestamp = rs.getTimestamp(Database.TIME);

      UserAndTime userAndTime = new MyUserAndTime(userID, exid, timestamp.getTime(), rs.getInt(QID));

      results.add(userAndTime);
    }
    finish(connection, statement, rs, sql);

    return results;
  }

  private List<MonitorResult> getMonitorResultsForQuery(Connection connection, PreparedStatement statement, String sql) throws SQLException {
    ResultSet rs = statement.executeQuery();
    List<MonitorResult> results = new ArrayList<>();
    while (rs.next()) {
      int uniqueID = rs.getInt(ID);
      long userID = rs.getLong(USERID);
      String exid = rs.getString(EXID);
      Timestamp timestamp = rs.getTimestamp(Database.TIME);
      String answer = rs.getString(ANSWER);
      boolean valid = rs.getBoolean(VALID);
      String type = rs.getString(AUDIO_TYPE);
      int dur = rs.getInt(DURATION);

      boolean correct = rs.getBoolean(CORRECT);
      float pronScore = rs.getFloat(PRON_SCORE);
      String dtype = rs.getString(DEVICE_TYPE);
      String simpleDevice = rs.getString(DEVICE);
      String device = dtype == null ? "Unk" : dtype.equals("browser") ? simpleDevice : (dtype + "/" + simpleDevice);
      String validity = rs.getString(VALIDITY);
      float snr = rs.getFloat(SNR);

      int processDur = rs.getInt(PROCESS_DUR);
      int roundTripDur = rs.getInt(ROUND_TRIP_DUR);
      String json = rs.getString(SCORE_JSON);
      String transcript = rs.getString(TRANSCRIPT);
      if (transcript == null) transcript = "";

      MonitorResult result = new MonitorResult(uniqueID, userID, //id
          exid,
          trimPathForWebPage2(answer), // answer
          valid, // valid
          timestamp.getTime(),
          type, dur, correct, pronScore, device, rs.getBoolean(WITH_FLASH),
          processDur, roundTripDur, validity, snr,
          dtype,
          simpleDevice,
          json,
          transcript);

      results.add(result);
    }
    finish(connection, statement, rs, sql);

    return results;
  }

  /**
   * @see #getCorrectAndScores
   * @param sql
   * @return
   * @throws SQLException
   */
  private List<CorrectAndScore> getScoreResultsSQL(String sql) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement(sql);
    return getScoreResultsForQuery(connection, statement, sql);
  }

  /**
   * @param connection
   * @param statement
   * @param sql
   * @return
   * @throws SQLException
   * @see #getResultsForExIDInForUser(java.util.Collection, boolean, long)
   */
  private List<CorrectAndScore> getScoreResultsForQuery(Connection connection, PreparedStatement statement, String sql) throws SQLException {
    long then = System.currentTimeMillis();
    ResultSet rs = statement.executeQuery();
    long now = System.currentTimeMillis();
//    if (now-then > 0) {
//      logger.info("getScoreResultsForQuery took " + (now - then) + " millis to exec query");
//    }

    List<CorrectAndScore> results = new ArrayList<>();

    then = now;
    while (rs.next()) {
      int uniqueID = rs.getInt(ID);
      long userid = rs.getInt(USERID);
      String id = rs.getString(EXID);
      Timestamp timestamp = rs.getTimestamp(Database.TIME);
      boolean correct = rs.getBoolean(CORRECT);
      float pronScore = rs.getFloat(PRON_SCORE);
      String path = rs.getString(ANSWER);
      String json = rs.getString(SCORE_JSON);

      CorrectAndScore result = new CorrectAndScore(uniqueID, userid, id, correct, pronScore, timestamp.getTime(),
          trimPathForWebPage2(path), json);
      results.add(result);
    }

    now = System.currentTimeMillis();
//    if (now-then > 0) {
//      logger.info("getScoreResultsForQuery took " + (now - then) + " millis to read from query and get " + results.size() + " results");
//    }

    then = now;
    finish(connection, statement, rs, sql);
    now = System.currentTimeMillis();
//    if (now-then > 0) {
//      logger.info("getScoreResultsForQuery took " + (now - then) + " millis to finish query.");
//    }

    return results;
  }

  private String trimPathForWebPage2(String path) {
    int answer = path.indexOf(PathHelper.ANSWERS);
    return (answer == -1) ? path : path.substring(answer);
  }

  /**
   * @param e
   * @param expected
   * @param englishOnly
   * @return
   * @see DatabaseImpl#getNextUngradedExerciseSlow
   */
/*  public boolean areAnyResultsLeftToGradeFor(CommonExercise e, int expected, boolean englishOnly) {
    String exerciseID = e.getID();
    GradeDAO.GradesAndIDs resultIDsForExercise = gradeDAO.getResultIDsForExercise(exerciseID);
    return !areAllResultsGraded(exerciseID, resultIDsForExercise.grades, expected, englishOnly);
  }*/

  /**
   * Return true if all results have been graded at the grade number
   * <p/>
   * Does some fancy filtering for english --
   *
   * @param exerciseID
   * @param gradedResults
   * @param expected         if > 1 remove flq results (hack!), if = 2 assumes english-only
   * @param useEnglishGrades true if we should only look at english grades...
   * @return ungraded answers
   * @see #areAnyResultsLeftToGradeFor
   */
/*  private boolean areAllResultsGraded(String exerciseID, Collection<Grade> gradedResults, int expected, boolean useEnglishGrades) {
    List<Result> resultsForExercise = getAllResultsForExercise(exerciseID);
*//*    if (DEBUG && !resultsForExercise.isEmpty()) {
      logger.DEBUG("for " + exerciseID + " expected " + expected +
        " grades/item before " + resultsForExercise.size() + " results, and " + gradedResults.size() + " grades");
    }*//*
    if (resultsForExercise.isEmpty()) {
      return true;
    }

    // conditionally narrow down to only english results
    // hack!
    for (Iterator<Result> iter = resultsForExercise.iterator(); iter.hasNext(); ) {
      Result next = iter.next();
      if (useEnglishGrades && next.flq || next.userid == -1) {
        // logger.DEBUG("removing result " + next + " since userid is " + next.userid);
        iter.remove();
      }
    }

    //if (DEBUG && false) logger.DEBUG("\tafter removing flq " + resultsForExercise.size());

    int countAtIndex = 0;
    for (Grade g : gradedResults) {
      if (g.gradeIndex == expected - 1 && g.grade != Grade.UNASSIGNED) {
        countAtIndex++;
      }
    }

    int numResults = resultsForExercise.size();
    boolean allGraded = numResults <= countAtIndex;

    if (DEBUG) {
      logger.DEBUG("areAllResultsGraded checking exercise " + exerciseID +
        " found " + countAtIndex + " grades at index at grade # " + expected +
        " given " + numResults + " results -- all graded is " + allGraded);
    }

    return allGraded;
  }*/

  /**
   * @param exerciseID
   * @return
   * @seex DatabaseImpl#getResultsForExercise(String, boolean, boolean, boolean)
   */
/*  public List<Result> getAllResultsForExercise(String exerciseID) {
    try {
      Connection connection = database.getConnection();
      String sql = "SELECT * FROM results WHERE EXID='" + exerciseID + "'";
      PreparedStatement statement = connection.prepareStatement(sql);
      return getResultsForQuery(connection, statement);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return new ArrayList<Result>();
  }*/

  /**
   * Determine sessions per user.  If two consecutive items are more than {@link #SESSION_GAP} seconds
   * apart, then we've reached a session boundary.
   * Remove all sessions that have just one answer - must be test sessions.
   * <p>
   * Multiple answers to the same exercise count as one answer.
   *
   * @return list of duration and numAnswer pairs
   */
  public SessionInfo getSessions() {
    Map<Long, List<CorrectAndScore>> userToAnswers = populateUserToAnswers(getCorrectAndScores());
    List<Session> sessions = new ArrayList<>();

    Map<Long, List<Session>> userToSessions = new HashMap<>();
    Map<Long, Float> userToRate = new HashMap<>();

    for (Map.Entry<Long, List<CorrectAndScore>> userToAnswersEntry : userToAnswers.entrySet()) {
      sessions.addAll(makeSessionsForUser(userToSessions, userToAnswersEntry));
    }
    for (Session session : sessions) session.setNumAnswers();
    removeShortSessions(sessions);

    for (Map.Entry<Long, List<Session>> sessionPair : userToSessions.entrySet()) {
      removeShortSessions(sessionPair.getValue());
      long dur = 0;
      int num = 0;

      for (Session s : sessionPair.getValue()) {
        //logger.DEBUG("user " +sessionPair.getKey() + " " + s);
        dur += s.duration;
        num += s.getNumAnswers();
      }

      if (num > 0) {
        float rate = (float) (dur / 1000) / (float) num;
        //logger.DEBUG("user " +sessionPair.getKey() + " dur " + dur/1000 + " num " + num+ " rate " +rate);
        userToRate.put(sessionPair.getKey(), rate);
      }
    }

    return new SessionInfo(sessions, userToRate);
  }

  private List<Session> makeSessionsForUser(Map<Long, List<Session>> userToSessions,
                                            Map.Entry<Long, List<CorrectAndScore>> userToAnswersEntry) {
    Long userid = userToAnswersEntry.getKey();
    List<CorrectAndScore> answersForUser = userToAnswersEntry.getValue();

    return makeSessionsForUser(userToSessions, userid, answersForUser);
  }

  private List<Session> makeSessionsForUser(Map<Long, List<Session>> userToSessions,
                                            Long userid,
                                            List<CorrectAndScore> answersForUser) {
    sortByTime(answersForUser);

    return partitionIntoSessions(userToSessions, userid, answersForUser);
  }

  private List<Session> partitionIntoSessions(Map<Long, List<Session>> userToSessions,
                                              Long userid, List<CorrectAndScore> answersForUser) {
    Session s = null;
    long last = 0;

    List<Session> sessions = new ArrayList<>();

    for (CorrectAndScore r : answersForUser) {
      long timestamp = r.getTimestamp();
      if (s == null || timestamp - last > SESSION_GAP) {
        s = new Session();
        sessions.add(s);

        List<Session> sessions1 = userToSessions.get(userid);
        if (sessions1 == null) userToSessions.put(userid, sessions1 = new ArrayList<>());
        sessions1.add(s);
      } else {
        s.duration += timestamp - last;
      }
      s.addExerciseID(r.getId());
      last = timestamp;
    }
    return sessions;
  }

  /**
   * @param answersForUser
   * @return
   * @see #getSessionsForUserIn2
   */
  private List<Session> partitionIntoSessions2(List<CorrectAndScore> answersForUser, Collection<String> ids, long latestResultID) {
    Session s = null;
    long lastTimestamp = 0;

    Set<String> expected = new HashSet<>(ids);

    List<Session> sessions = new ArrayList<>();

    int id = 0;
    for (CorrectAndScore r : answersForUser) {
      //logger.DEBUG("got " + r);
      String id1 = r.getId();
      long timestamp = r.getTimestamp();
      if (s == null || timestamp - lastTimestamp > SESSION_GAP || !expected.contains(id1)) {
        sessions.add(s = new Session(id++, r.getUserid(), timestamp));
        expected = new HashSet<>(ids); // start a new set of expected items
//        logger.DEBUG("\tpartitionIntoSessions2 expected " +expected.size());
      } else {
        s.duration += timestamp - lastTimestamp;
      }

      s.addExerciseID(id1);
      s.incrementCorrect(id1, r.isCorrect());
      s.setScore(id1, r.getScore());

      if (r.getUniqueID() == latestResultID) {
        logger.debug("\tpartitionIntoSessions2 found current session " + s);

        s.setLatest(true);
      }

      expected.remove(id1);
      // logger.DEBUG("\tpartitionIntoSessions2 expected now " + expected.size() + " session " + s);

      lastTimestamp = timestamp;
    }
    for (Session session : sessions) session.setNumAnswers();
    if (sessions.isEmpty() && !answersForUser.isEmpty()) {
      logger.error("huh? no sessions from " + answersForUser.size() + " given " + ids);
    }
//    logger.DEBUG("\tpartitionIntoSessions2 made " +sessions.size() + " from " + answersForUser.size() + " answers");

    return sessions;
  }

  public static class SessionInfo {
    public final List<Session> sessions;
    public final Map<Long, Float> userToRate;

    public SessionInfo(List<Session> sessions, Map<Long, Float> userToRate) {
      this.sessions = sessions;
      this.userToRate = userToRate;
    }
  }

  private void sortByTime(List<CorrectAndScore> answersForUser) {
    Collections.sort(answersForUser);
  }

  private void removeShortSessions(List<Session> sessions) {
    Iterator<Session> iter = sessions.iterator();
    while (iter.hasNext()) if (iter.next().getNumAnswers() < 2) iter.remove();
  }

  private Map<Long, List<CorrectAndScore>> populateUserToAnswers(List<CorrectAndScore> results) {
    Map<Long, List<CorrectAndScore>> userToAnswers = new HashMap<>();
    for (CorrectAndScore r : results) {
      long userid = r.getUserid();
      List<CorrectAndScore> results1 = userToAnswers.get(userid);
      if (results1 == null) userToAnswers.put(userid, results1 = new ArrayList<>());
      results1.add(r);
    }
    return userToAnswers;
  }

  /**
   * No op if table exists and has the current number of columns.
   *
   * @param connection
   * @throws SQLException
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
   */
  void createResultTable(Connection connection) throws SQLException {
    createTable(connection);
    removeTimeDefault(connection);

    int numColumns = getNumColumns(connection, RESULTS);
    if (numColumns == 8) {
      addColumnToTable(connection);
    }
    if (numColumns <= 11) {
      addTypeColumnToTable(connection);
    }
    if (numColumns < 12) {
      addDurationColumnToTable(connection);
    }
    if (numColumns < 14) {
      addFlashcardColumnsToTable(connection);
    }
/*    if (numColumns < 15) {
      addStimulus(connection);
    }*/
    Collection<String> columns = getColumns(RESULTS);
    if (!columns.contains(DEVICE_TYPE.toLowerCase())) {
      addVarchar(connection, RESULTS, DEVICE_TYPE);
      addVarchar(connection, RESULTS, DEVICE);
    }

    if (!columns.contains(SCORE_JSON.toLowerCase())) {
      addVarchar(connection, RESULTS, SCORE_JSON);
    }
    if (!columns.contains(WITH_FLASH.toLowerCase())) {
      addBoolean(connection, RESULTS, WITH_FLASH);
    }
    if (!columns.contains(PROCESS_DUR.toLowerCase())) {
      addInt(connection, RESULTS, PROCESS_DUR);
    }
    if (!columns.contains(ROUND_TRIP_DUR.toLowerCase())) {
      addInt(connection, RESULTS, ROUND_TRIP_DUR);
    }

    if (!columns.contains(VALIDITY.toLowerCase())) {
      addVarchar(connection, RESULTS, VALIDITY);
    }
    if (!columns.contains(SNR.toLowerCase())) {
      addFloat(connection, RESULTS, SNR);
    }

    if (!columns.contains(TRANSCRIPT.toLowerCase())) {
      addVarchar(connection, RESULTS, TRANSCRIPT);
    }

    if (!columns.contains(MODEL.toLowerCase())) {
      addVarchar(connection, RESULTS, MODEL);
    }

    if (!columns.contains(MODELUPDATE.toLowerCase())) {
      addTimestamp(connection, RESULTS, MODELUPDATE);
    }

    database.closeConnection(connection);

    createTableIndex(database, EXID.toUpperCase(), RESULTS);
    createIndex(database, VALID, RESULTS);
    createIndex(database, AUDIO_TYPE, RESULTS);
  }

  /**
   * So we don't want to use CURRENT_TIMESTAMP as the default for TIMESTAMP
   * b/c if we ever alter the table, say by adding a new column, we will effectively lose
   * the timestamp that was put there when we inserted the row initially.
   * <p></p>
   * Note that the answer column can be either the text of an answer for a written response
   * or a relative path to an audio file on the server.
   *
   * @param connection to make a statement from
   * @throws SQLException
   */
  private void createTable(Connection connection) throws SQLException {
    PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS " +
        RESULTS +
        " (" +
        ID + " IDENTITY, " +
        USERID + " INT, " +
        "plan VARCHAR, " +
        EXID + " VARCHAR, " +
        "qid INT," +
        Database.TIME + " TIMESTAMP, " +// " AS CURRENT_TIMESTAMP," +
        "answer CLOB," +
        "valid BOOLEAN," +
        FLQ + " BOOLEAN," +
        SPOKEN + " BOOLEAN," +
        AUDIO_TYPE + " VARCHAR," +
        DURATION + " INT," +
        CORRECT + " BOOLEAN," +
        PRON_SCORE + " FLOAT," +
        STIMULUS + " CLOB," +
        DEVICE_TYPE + " VARCHAR," +
        DEVICE + " VARCHAR," +
        SCORE_JSON + " VARCHAR," +
        WITH_FLASH + " BOOLEAN," +
        PROCESS_DUR + " INT," +
        ROUND_TRIP_DUR + " INT, " +
        VALIDITY + " VARCHAR, " +
        TRANSCRIPT + " VARCHAR, " +
        SNR + " FLOAT" +
        ")");
    statement.execute();
    statement.close();
  }

  private void addColumnToTable(Connection connection) {
    try {
      addBoolean(connection, RESULTS, FLQ);
    } catch (SQLException e) {
      logger.warn("addColumnToTable : flq got " + e);
    }

    try {
      addBoolean(connection, RESULTS, SPOKEN);
    } catch (SQLException e) {
      logger.warn("addColumnToTable : spoken got " + e);
    }
  }

  private void addTypeColumnToTable(Connection connection) {
    PreparedStatement statement;

    try {
      statement = connection.prepareStatement("ALTER TABLE " + RESULTS + " ADD " +
          AUDIO_TYPE +
          " " +
          getVarchar());
      statement.execute();
      statement.close();
    } catch (SQLException e) {
      logger.warn("addTypeColumnToTable : got " + e);
    }
  }

  private void removeTimeDefault(Connection connection) throws SQLException {
    //logger.info("removing time default value - current_timestamp steps on all values with NOW.");
    PreparedStatement statement = connection.prepareStatement("ALTER TABLE " + RESULTS + " ALTER COLUMN " + Database.TIME +
        " DROP DEFAULT");
    statement.execute();
    statement.close();
  }

  private void addDurationColumnToTable(Connection connection) {
    try {
      PreparedStatement statement = connection.prepareStatement("ALTER TABLE " + RESULTS + " ADD " +
          DURATION +
          " " +
          "INT");
      statement.execute();
      statement.close();
    } catch (SQLException e) {
      logger.warn("addDurationColumnToTable : got " + e);
    }
  }

  private void addFlashcardColumnsToTable(Connection connection) {
    try {
      PreparedStatement statement = connection.prepareStatement("ALTER TABLE " + RESULTS + " ADD " +
          CORRECT +
          " " +
          "BOOLEAN");
      statement.execute();
      statement.close();
    } catch (SQLException e) {
      logger.warn("addDurationColumnToTable : got " + e);
    }

    try {
      PreparedStatement statement = connection.prepareStatement("ALTER TABLE " + RESULTS + " ADD " +
          PRON_SCORE +
          " " +
          "FLOAT");
      statement.execute();
      statement.close();
    } catch (SQLException e) {
      logger.warn("addFlashcardColumnsToTable : got " + e);
    }
  }

  /**
   * Just for import
   *
   * @param isRegular
   * @param userDAO
   * @return
   */
  public Map<Long, Map<String, Result>> getUserToResults(boolean isRegular, UserDAO userDAO) {
    String typeToUse = isRegular ? Result.AUDIO_TYPE_REGULAR : Result.AUDIO_TYPE_SLOW;
    return getUserToResults(typeToUse, userDAO);
  }

  private Map<Long, Map<String, Result>> getUserToResults(String typeToUse, UserDAO userDAO) {
    Map<Long, Map<String, Result>> userToResult = new HashMap<>();

    Map<Long, User> userMap = userDAO.getUserMap();

    for (Result r : getResults()) {
      if (r.isValid() && r.getAudioType().equals(typeToUse)) {
        User user = userMap.get(r.getUserid());
        if (user != null && user.getExperience() == 240) {    // only natives!
          Map<String, Result> results1 = userToResult.get(r.getUserid());
          if (results1 == null)
            userToResult.put(r.getUserid(), results1 = new HashMap<>());
          String exerciseID = r.getExerciseID();
          Result result = results1.get(exerciseID);
          if (result == null || (r.getTimestamp() > result.getTimestamp())) {
            results1.put(exerciseID, r);
          }
        }
      }
    }
    return userToResult;
  }

  /**
   * @param typeOrder
   * @param out
   * @see mitll.langtest.server.DownloadServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  public void writeExcelToStream(Collection<MonitorResult> results, Collection<String> typeOrder, OutputStream out) {
    new ResultDAOToExcel().writeExcelToStream(results, typeOrder, out);
  }


  private static class MyUserAndTime implements UserAndTime {
    private final long userID;
    private final String exid;
    private final long time;
    private final int qid;

    MyUserAndTime(long userID, String exid, long time, int qid) {
      this.userID = userID;
      this.exid = exid;
      this.time = time;
      this.qid = qid;
    }

    @Override
    public long getUserid() {
      return userID;
    }

    @Override
    public long getTimestamp() {
      return time;
    }

    @Override
    public String getExid() {
      return exid;
    }

    @Override
    public String getID() {
      return getExid() + "/" + qid;
    }
  }
}