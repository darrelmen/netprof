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

package mitll.langtest.server.database.result;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.sorter.ExerciseSorter;
import mitll.langtest.shared.UserAndTime;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.HasID;
import mitll.langtest.shared.exercise.MutableExercise;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.flashcard.ExerciseCorrectAndScore;
import mitll.langtest.shared.monitoring.Session;
import mitll.langtest.shared.result.MonitorResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.CollationKey;
import java.text.Collator;
import java.util.*;

public abstract class BaseResultDAO extends DAO {
  private static final Logger logger = LogManager.getLogger(BaseResultDAO.class);

  private static final int MINUTE = 60 * 1000;
  protected static final int SESSION_GAP = 5 * MINUTE;  // 5 minutes
  private final boolean DEBUG = false;
  List<MonitorResult> cachedMonitorResultsForQuery = null;
  private List<CorrectAndScore> cachedResultsForQuery2 = null;

  /**
   * @see SlickResultDAO
   * @param database
   */
  BaseResultDAO(Database database) {
    super(database);
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
   * @param language
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#getUserHistoryForList
   * @see mitll.langtest.client.flashcard.StatsFlashcardFactory.StatsPracticePanel#onSetComplete()
   */
  public SessionsAndScores getSessionsForUserIn2(Collection<Integer> ids,
                                                 int latestResultID,
                                                 int userid,
                                                 Collection<Integer> allIds,
                                                 Map<Integer, CollationKey> idToKey, String language) {
    List<Session> sessions = new ArrayList<>();
    Map<Integer, List<CorrectAndScore>> userToAnswers = populateUserToAnswers(getResultsForExIDIn(ids, language));
    if (DEBUG) logger.debug("Got " + userToAnswers.size() + " user->answer map");
    for (Map.Entry<Integer, List<CorrectAndScore>> userToResults : userToAnswers.entrySet()) {
      List<Session> c = partitionIntoSessions2(userToResults.getValue(), ids, latestResultID);
      if (DEBUG)
        logger.debug("\tfound " + c.size() + " sessions for " + userToResults.getKey() + " " + ids + " given  " + userToResults.getValue().size());

      sessions.addAll(c);
    }

    List<CorrectAndScore> results = getResultsForExIDInForUser(allIds, true, userid, language);
    if (DEBUG) logger.debug("found " + results.size() + " results for " + allIds.size() + " items");

    List<ExerciseCorrectAndScore> sortedResults = getSortedAVPHistory(results, allIds, idToKey);
    if (DEBUG) logger.debug("found " + sessions.size() + " sessions for " + ids);

    return new SessionsAndScores(sessions, sortedResults);
  }

  /**
   * @param results
   * @param allIds
   * @param idToKey
   * @return
   * @see #getExerciseCorrectAndScores
   */
  protected List<ExerciseCorrectAndScore> getSortedAVPHistory(List<CorrectAndScore> results,
                                                              Collection<Integer> allIds,
                                                              final Map<Integer, CollationKey> idToKey) {
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
      return comp == 0 ? Integer.valueOf(o1.getId()).compareTo(o2.getId()) : comp;
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
  protected List<ExerciseCorrectAndScore> getExerciseCorrectAndScores(List<CorrectAndScore> results, Collection<Integer> allIds) {
    SortedMap<Integer, ExerciseCorrectAndScore> idToScores = new TreeMap<>();
    if (results != null) {
      for (CorrectAndScore r : results) {
        int id = r.getExid();
        ExerciseCorrectAndScore correctAndScores = idToScores.get(id);
        if (correctAndScores == null) idToScores.put(id, correctAndScores = new ExerciseCorrectAndScore(id));
        correctAndScores.add(r);
      }
    }
    for (ExerciseCorrectAndScore exerciseCorrectAndScore : idToScores.values()) {
      exerciseCorrectAndScore.sort();
    }

    for (Integer id : allIds) {
      if (!idToScores.containsKey(id)) idToScores.put(id, new ExerciseCorrectAndScore(id));
    }
    return new ArrayList<>(idToScores.values());
  }

  /**
   * @param exercises
   * @param userid
   * @param collator
   * @param language
   * @return
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getExerciseIds
   */
  public <T extends CommonShell> List<T> getExercisesSortedIncorrectFirst(Collection<T> exercises,
                                                                          int userid,
                                                                          Collator collator,
                                                                          String language) {
    List<Integer> allIds = new ArrayList<>();
    Map<Integer, T> idToEx = new HashMap<>();
    Map<Integer, CollationKey> idToKey = new HashMap<>();
    for (T exercise : exercises) {
      Integer id = exercise.getID();
      allIds.add(id);
      idToEx.put(id, exercise);
      //  idToKey.put(id,exercise.getForeignLanguage());
      CollationKey collationKey = collator.getCollationKey(exercise.getForeignLanguage());
      idToKey.put(id, collationKey);
    }

    List<ExerciseCorrectAndScore> sortedResults = getExerciseCorrectAndScores(userid, allIds, idToKey, language);

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
   * @param language
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#getJsonScoreHistory
   * @see IResultDAO#getExercisesSortedIncorrectFirst
   */
  private List<ExerciseCorrectAndScore> getExerciseCorrectAndScores(int userid,
                                                                    List<Integer> allIds,
                                                                    Map<Integer, CollationKey> idToKey, String language) {
    List<CorrectAndScore> results = getResultsForExIDInForUser(allIds, true, userid, language);
    // if (debug) logger.debug("found " + results.size() + " results for " + allIds.size() + " items");
    return getSortedAVPHistory(results, allIds, idToKey);
  }

  /**
   * @param results
   * @param allIds
   * @param idToEx
   * @param sorter
   * @return
   * @see #getExerciseCorrectAndScoresByPhones
   */
  protected List<ExerciseCorrectAndScore> getSortedAVPHistoryByPhones(List<CorrectAndScore> results,
                                                                      Collection<Integer> allIds,
                                                                      final Map<Integer, CommonExercise> idToEx,
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

  public Collection<ExerciseCorrectAndScore> getExerciseCorrectAndScoresByPhones(int userid,
                                                                                 List<Integer> allIds,
                                                                                 Map<Integer, CommonExercise> idToEx,
                                                                                 ExerciseSorter sorter,
                                                                                 String language) {
    List<CorrectAndScore> results = getResultsForExIDInForUser(allIds, true, userid, language);
    // if (debug) logger.debug("found " + results.size() + " results for " + allIds.size() + " items");
    return getSortedAVPHistoryByPhones(results, allIds, idToEx, sorter);
  }

  /**
   * So when updating old data that is missing word and phone alignment information, we have to put it back.
   *
   * @return to re-process
   * @seex mitll.langtest.server.decoder.RefResultDecoder#doMissingInfo
   * @param language
   */
/*  @Override
  public Collection<Result> getResultsToDecode() {
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
  }*/
  private List<CorrectAndScore> getCorrectAndScores(String language) {
    try {
      synchronized (this) {
        if (cachedResultsForQuery2 != null) {
          return cachedResultsForQuery2;
        }
      }
      List<CorrectAndScore> resultsForQuery = getCorrectAndScoresForReal(language);

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
   * So multiple recordings for the same item are counted as 1.
   * @return
   * @seex #getUsers
   */
  public UserToCount getUserToNumAnswers() {
    Map<Integer, Integer> idToCount = new HashMap<>();
   // Map<Integer, Set<Integer>> idToUniqueCount = new HashMap<>();
    for (UserAndTime result : getUserAndTimes()) {
      int userid = result.getUserid();
  //    int exerciseID = result.getExid();

      Integer count = idToCount.get(userid);
      if (count == null) idToCount.put(userid, 1);
      else idToCount.put(userid, count + 1);

     // Set<Integer> uniqueForUser = idToUniqueCount.get(userid);
    //  if (uniqueForUser == null) idToUniqueCount.put(userid, uniqueForUser = new HashSet<>());
    //  uniqueForUser.add(exerciseID);
    }
    return new UserToCount(idToCount);//, idToUniqueCount);
  }

  abstract Collection<UserAndTime> getUserAndTimes();
  abstract List<CorrectAndScore> getCorrectAndScoresForReal(String language);

  /**
   * @param userID
   * @param firstExercise
   * @param isFlashcardRequest
   * @param language
   * @see mitll.langtest.server.services.ExerciseServiceImpl#addAnnotationsAndAudio
   */
  public void attachScoreHistory(int userID, CommonExercise firstExercise, boolean isFlashcardRequest, String language) {
    List<CorrectAndScore> resultsForExercise = getCorrectAndScores(userID, firstExercise, isFlashcardRequest, language);

 //   logger.debug("attachScoreHistory score history " + resultsForExercise);
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
   * @param language
   * @return
   * @see #attachScoreHistory
   */
  private List<CorrectAndScore> getCorrectAndScores(int userID, HasID firstExercise, boolean isFlashcardRequest, String language) {
    return getResultsForExIDInForUser(userID, isFlashcardRequest, firstExercise.getID(), language);
  }

  private List<CorrectAndScore> getResultsForExIDInForUser(int userID, boolean isFlashcardRequest, int id, String language) {
    return getResultsForExIDInForUser(Collections.singleton(id), isFlashcardRequest, userID, language);
  }

  abstract List<CorrectAndScore> getResultsForExIDIn(Collection<Integer> ids, String language);

  abstract List<CorrectAndScore> getResultsForExIDInForUser(Collection<Integer> ids, boolean matchAVP, int userid, String language);

  private Map<Integer, List<CorrectAndScore>> populateUserToAnswers(List<CorrectAndScore> results) {
    Map<Integer, List<CorrectAndScore>> userToAnswers = new HashMap<>();
    for (CorrectAndScore r : results) {
      int userid = r.getUserid();
      List<CorrectAndScore> results1 = userToAnswers.get(userid);
      if (results1 == null) userToAnswers.put(userid, results1 = new ArrayList<>());
      results1.add(r);
    }
    return userToAnswers;
  }

  private List<Session> partitionIntoSessions(Map<Integer, List<Session>> userToSessions,
                                              Integer userid, List<CorrectAndScore> answersForUser) {
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
      s.addExerciseID(r.getExid());
      last = timestamp;
    }
    return sessions;
  }

  /**
   * @param answersForUser
   * @return
   * @see #getSessionsForUserIn2
   */
  private List<Session> partitionIntoSessions2(List<CorrectAndScore> answersForUser, Collection<Integer> ids, long latestResultID) {
    Session s = null;
    long lastTimestamp = 0;

    Set<Integer> expected = new HashSet<>(ids);

    List<Session> sessions = new ArrayList<>();

    int id = 0;
    for (CorrectAndScore r : answersForUser) {
      //logger.debug("got " + r);
      int id1 = r.getExid();
      long timestamp = r.getTimestamp();
      if (s == null || timestamp - lastTimestamp > SESSION_GAP || !expected.contains(id1)) {
        sessions.add(s = new Session(id++, r.getUserid(), timestamp));
        expected = new HashSet<>(ids); // start a new set of expected items
//        logger.debug("\tpartitionIntoSessions2 expected " +expected.size());
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
      // logger.debug("\tpartitionIntoSessions2 expected now " + expected.size() + " session " + s);

      lastTimestamp = timestamp;
    }
    for (Session session : sessions) session.setNumAnswers();
    if (sessions.isEmpty() && !answersForUser.isEmpty()) {
      logger.error("huh? no sessions from " + answersForUser.size() + " given " + ids);
    }
//    logger.debug("\tpartitionIntoSessions2 made " +sessions.size() + " from " + answersForUser.size() + " answers");

    return sessions;
  }

  /**
   * Determine sessions per user.  If two consecutive items are more than {@link #SESSION_GAP} seconds
   * apart, then we've reached a session boundary.
   * Remove all sessions that have just one answer - must be test sessions.
   * <p>
   * Multiple answers to the same exercise count as one answer.
   *
   * @return list of duration and numAnswer pairs
   * @param language
   */
  public SessionInfo getSessions(String language) {
    Map<Integer, List<CorrectAndScore>> userToAnswers = populateUserToAnswers(getCorrectAndScores(language));
    List<Session> sessions = new ArrayList<>();

    Map<Integer, List<Session>> userToSessions = new HashMap<>();
    Map<Integer, Float> userToRate = new HashMap<>();

    for (Map.Entry<Integer, List<CorrectAndScore>> userToAnswersEntry : userToAnswers.entrySet()) {
      sessions.addAll(makeSessionsForUser(userToSessions, userToAnswersEntry));
    }
    for (Session session : sessions) session.setNumAnswers();
    removeShortSessions(sessions);

    for (Map.Entry<Integer, List<Session>> sessionPair : userToSessions.entrySet()) {
      removeShortSessions(sessionPair.getValue());
      long dur = 0;
      int num = 0;

      for (Session s : sessionPair.getValue()) {
        //logger.debug("user " +sessionPair.getKey() + " " + s);
        dur += s.duration;
        num += s.getNumAnswers();
      }

      if (num > 0) {
        float rate = (float) (dur / 1000) / (float) num;
        //logger.debug("user " +sessionPair.getKey() + " dur " + dur/1000 + " num " + num+ " rate " +rate);
        userToRate.put(sessionPair.getKey(), rate);
      }
    }

    return new SessionInfo(sessions, userToRate);
  }

  private List<Session> makeSessionsForUser(Map<Integer, List<Session>> userToSessions,
                                            Map.Entry<Integer, List<CorrectAndScore>> userToAnswersEntry) {
    Integer userid = userToAnswersEntry.getKey();
    List<CorrectAndScore> answersForUser = userToAnswersEntry.getValue();

    return makeSessionsForUser(userToSessions, userid, answersForUser);
  }

  private List<Session> makeSessionsForUser(Map<Integer, List<Session>> userToSessions,
                                            Integer userid,
                                            List<CorrectAndScore> answersForUser) {
    sortByTime(answersForUser);

    return partitionIntoSessions(userToSessions, userid, answersForUser);
  }

  private void sortByTime(List<CorrectAndScore> answersForUser) {
    Collections.sort(answersForUser);
  }

  private void removeShortSessions(List<Session> sessions) {
    Iterator<Session> iter = sessions.iterator();
    while (iter.hasNext()) if (iter.next().getNumAnswers() < 2) iter.remove();
  }

  /**
   * @see AnswerDAO#addAnswerToTable
   */
  public synchronized void invalidateCachedResults() {
    cachedResultsForQuery2 = null;
    cachedMonitorResultsForQuery = null;
  }
}
