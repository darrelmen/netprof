package mitll.langtest.server.amas;

import com.google.gwt.visualization.client.formatters.DateFormat;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.flashcard.QuizCorrectAndScore;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.*;

/**
 * Created by go22670 on 1/4/16.
 */
public class QuizCorrect {
  private static final Logger logger = Logger.getLogger(QuizCorrect.class);
  private static final String QUIZ = "Quiz";
  private static final String ILR_LEVEL = "ILR Level";
  private static final String TEST_TYPE = "Test type";
  private final DatabaseImpl db;

  public QuizCorrect(DatabaseImpl db) {
    this.db = db;
  }
  /**
   * @param typeToSection
   * @param userID
   * @param exids
   * @return
   * @see mitll.langtest.client.recorder.FeedbackRecordPanel#getScores
   */
  public QuizCorrectAndScore getScoresForUser(Map<String, Collection<String>> typeToSection, int userID, Collection<String> exids) {
    //   String session = getLatestSession(typeToSection, userID);

/*    if (typeToSection == null || typeToSection.isEmpty()) {
      List<CorrectAndScore> resultsForUser = db.getResultDAO().getResultsForUser(userID, "");
      Collection<CorrectAndScore> copy = getFirstCorrectAndScore(resultsForUser);
      // filter by session
      List<CorrectAndScore> bySession = filterBySession(copy, session);

      logger.error("\n\n\ngetScoresForUser for user " + userID + "/" + session + " and " + typeToSection + " got " + resultsForUser.size() + " vs unique " + bySession.size());
      return new QuizCorrectAndScore(getExercises().size() == resultsForUser.size(), getExercises().size(), bySession);
    } else {*/
    Collection<String> allIDs = new ArrayList<String>();
    if (exids == null || exids.isEmpty()) {
      Collection<CommonExercise> exercisesForState = db.getSectionHelper().getExercisesForSelectionState(typeToSection);
      for (CommonExercise exercise : exercisesForState) {
        allIDs.add(exercise.getID());
      }
    } else {
      allIDs = exids;
    }

    return getQuizCorrectAndScore(typeToSection, userID, allIDs);
    //  }
  }

  /**
   * @see #filterByUnrecorded(long, Collection, Map)
   * @param typeToSection
   * @param userID
   * @param allIDs
   * @return
   */
  private QuizCorrectAndScore getQuizCorrectAndScore(Map<String, Collection<String>> typeToSection, int userID,
                                                     Collection<String> allIDs) {
    String session = "";//getLatestSession(typeToSection, userID);
    //  logger.info("exercises " +allIDs.size() + " for session " + session);

    List<CorrectAndScore> resultsForExIDInForUser = db.getResultDAO().getResultsForExIDInForUser(allIDs, userID, session);
    //  for (CorrectAndScore cs:resultsForExIDInForUser) logger.info("found " +cs );
    Collection<CorrectAndScore> copy = getFirstCorrectAndScore(resultsForExIDInForUser);

    List<CorrectAndScore> bySession = filterBySession(copy/*, session*/);

/*
    logger.info("getQuizCorrectAndScore for user " + userID + " session " + session +
        " got " + resultsForExIDInForUser.size() + " vs unique " + bySession.size());
*/

    int numExercises = allIDs.size();
    return new QuizCorrectAndScore(numExercises == bySession.size(), numExercises, bySession);
  }

/*  private String getLatestSession(Map<String, Collection<String>> typeToSection, long userID) {
    String test = typeToSection == null || typeToSection.isEmpty() || !typeToSection.containsKey(QUIZ) ? "Test" : typeToSection.get(QUIZ).iterator().next();
    String type = typeToSection == null || typeToSection.isEmpty() || !typeToSection.containsKey(TEST_TYPE) ? "" : typeToSection.get(TEST_TYPE).iterator().next();
    String ilr  = typeToSection == null || typeToSection.isEmpty() || !typeToSection.containsKey(ILR_LEVEL) ? "" : typeToSection.get(ILR_LEVEL).iterator().next();
    return getLatestSession(userID, test, type, ilr);
  }*/
/*
  private String getLatestSession(long userID, String test, String testType, String ilr) {
    String full = test + ":" + testType + ":" + ilr;
    int latestSession = getLatestSession(userID, full);
    //  logger.warn("full session key is " + full + " -> " + latestSession);
    return latestSession == 0 ? "" : "" + latestSession;
  }*/
/*
  public void incrementSession(long userid, String test) {
    int session = getLatestSession(userid, test);
    db.getTestSessionDAO().addSession(new TestSessionDAO.TestSession(userid, test, session));
  }

  private int getLatestSession(long userid, String test) {
    try {
      TestSessionDAO testSessionDAO = db.getTestSessionDAO();
      TestSessionDAO.TestSession latest = testSessionDAO.getLatest(userid, test);
      int i = latest == null ? 0 : latest.getSession() + 1;
//      logger.info("getLatest - for " + userid + " and " + test + " = " + i);
      return i;
    } catch (SQLException e) {
      logger.error("Got " + e, e);
      return 0;
    }
  }*/

  /**
   * @see #getQuizCorrectAndScore(Map, int, Collection)
   * @param copy
   * @paramx session
   * @return
   */
  private List<CorrectAndScore> filterBySession(Collection<CorrectAndScore> copy/*, String session*/) {
    List<CorrectAndScore> bySession = new ArrayList<>();
    for (CorrectAndScore correctAndScore : copy) {
/*      String session1 = correctAndScore.getSession();
      if (session1 == null) logger.error("huh? session is null for " + correctAndScore);
      if (session == null || session.isEmpty() || session1 == null || session1.equals(session)) {
        bySession.add(correctAndScore);
      }*/
      if (correctAndScore.hasUserScore()) {
        bySession.add(correctAndScore);
      }
    }
//    if (copy.size() != bySession.size()) logger.error("huh? copy " + copy + " vs " + bySession);
    return bySession;
  }

  /**
   * If you have a user score for an item, it will replace a previous answer that doesn't have one.
   * All Results *must* have both an auto score and a self score.
   *
   * @param resultsForUser
   * @return
   * @see #getQuizCorrectAndScore(Map, int, Collection)
   */
  private Collection<CorrectAndScore> getFirstCorrectAndScore(List<CorrectAndScore> resultsForUser) {
    Map<String, CorrectAndScore> idToCorrect = new HashMap<String, CorrectAndScore>();

    for (CorrectAndScore correctAndScore : resultsForUser) {
      String key = correctAndScore.getId() + "/" + correctAndScore.getQid();
      idToCorrect.put(key, correctAndScore);
    }
    return new ArrayList<CorrectAndScore>(idToCorrect.values()); // required since can't send a values set
  }

}
