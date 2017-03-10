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

package mitll.langtest.server.services;

import mitll.langtest.server.database.exercise.ISection;
import mitll.langtest.server.database.exercise.SectionHelper;
import mitll.langtest.server.database.result.IResultDAO;
import mitll.langtest.shared.amas.AmasExerciseImpl;
import mitll.langtest.shared.exercise.HasID;
import mitll.langtest.shared.exercise.STATE;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.flashcard.QuizCorrectAndScore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

class AmasSupport {
  private static final Logger logger = LogManager.getLogger(AmasSupport.class);
  //  private static final String WAV = ".wav";
//  private static final String MP3 = ".mp3";
//  public static final String DATABASE_REFERENCE = "databaseReference";
//  public static final String AUDIO_FILE_HELPER_REFERENCE = "audioFileHelperReference";
//  private static final int SLOW_EXERCISE_EMAIL = 2000;
//  private static final String ENGLISH = "English";
//  private static final int SLOW_MILLIS = 40;
  private static final String QUIZ = "Quiz";
  private static final String ILR_LEVEL = "ILR Level";
  private static final String TEST_TYPE = "Test type";


  /**
   * @param typeToSection
   * @param prefix
   * @param userID
   * @return
   * @see mitll.langtest.client.list.HistoryExerciseList#loadExercises
   */
  public Collection<AmasExerciseImpl> getExercisesForSelectionState(
      Map<String, Collection<String>> typeToSection, String prefix,
      long userID,
      ISection<AmasExerciseImpl> sectionHelper,
      IResultDAO resultDAO
  ) {
    Collection<AmasExerciseImpl> exercisesForState = sectionHelper.getExercisesForSelectionState(typeToSection);
    exercisesForState = filterByUnrecorded(userID, exercisesForState, typeToSection, resultDAO);
    //exercisesForState = filterByOnlyAudioAnno(forgotUsername(), exercisesForState);
    return exercisesForState;
  }


  /**
   * For all the exercises the user has not recorded, do they have the required reg and slow speed recordings by a matching gender.
   * <p>
   * Or if looking for example audio, find ones missing examples.
   *
   * @param userID    exercise not recorded by this user and matching the user's gender
   * @param exercises to filter
   * @return exercises missing audio, what we want to record
   * @seex #getExerciseIds
   * @see #getExercisesForSelectionState
   */
  Collection<AmasExerciseImpl> filterByUnrecorded(long userID,
                                                         Collection<AmasExerciseImpl> exercises,
                                                         Map<String, Collection<String>> typeToSection,
                                                         IResultDAO resultDAO) {
    Collection<Integer> allIDs = new ArrayList<>();

    for (HasID exercise : exercises) {
      allIDs.add(exercise.getID());
    }

    QuizCorrectAndScore correctAndScores = getQuizCorrectAndScore(typeToSection, (int) userID, allIDs, resultDAO);

    Map<Integer, List<Integer>> exToQIDs = new HashMap<>();

    Collection<CorrectAndScore> correctAndScoreCollection = correctAndScores.getCorrectAndScoreCollection();

    //logger.info("found " + correctAndScoreCollection.size() + " scores for " + userID + " and " + allIDs);

    for (CorrectAndScore cs : correctAndScoreCollection) {
      if (cs.hasUserScore()) {
        List<Integer> answered = exToQIDs.get(cs.getExid());
        if (answered == null) exToQIDs.put(cs.getExid(), answered = new ArrayList<>());
        answered.add(cs.getQid());
        //  logger.info("Adding " + cs.getExID() + "/" + cs.getQid());
      } else {
        //  logger.warn("skipping " + cs + " since no user score was set.");
      }
    }

    markExercisesAsComplete(exercises, exToQIDs);

    return exercises;
  }

  private void markExercisesAsComplete(Collection<AmasExerciseImpl> exercises, Map<Integer, List<Integer>> exToQIDs) {
    int marked = 0;
    // mark with answered
    for (AmasExerciseImpl ex : exercises) {
      List<Integer> integers = exToQIDs.get(ex.getID());
      if (integers == null) {
        ex.setState(STATE.UNSET);
      } else {
        if (ex.getForeignLanguageQuestions().size() == integers.size()) {
          ex.setState(STATE.APPROVED);
          marked++;
        } else {
          ex.setState(STATE.UNSET);
        }
      }
    }

//    logger.info("of " +exercises.size() + " exercises marked " + marked);
  }

  /**
   * @param typeToSection
   * @param userID
   * @param allIDs
   * @return
   * @see #filterByUnrecorded
   */
  private QuizCorrectAndScore getQuizCorrectAndScore(Map<String, Collection<String>> typeToSection, int userID,
                                                     Collection<Integer> allIDs,
                                                     IResultDAO resultDAO) {
    String session = getLatestSession(typeToSection, userID);
    //  logger.info("exercises " +allIDs.size() + " for session " + session);

    Collection<CorrectAndScore> resultsForExIDInForUser = resultDAO.getResultsForExIDInForUser(allIDs, userID, session, "");
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

  private String getLatestSession(long userID, String test, String testType, String ilr) {
    String full = test + ":" + testType + ":" + ilr;
    int latestSession = 0;//getLatestSession(userID, full);
    //  logger.warn("full session key is " + full + " -> " + latestSession);
    return latestSession == 0 ? "" : "" + latestSession;
  }

  private String getLatestSession(Map<String, Collection<String>> typeToSection, long userID) {
    String test = typeToSection == null || typeToSection.isEmpty() || !typeToSection.containsKey(QUIZ) ? "Test" : typeToSection.get(QUIZ).iterator().next();
    String type = typeToSection == null || typeToSection.isEmpty() || !typeToSection.containsKey(TEST_TYPE) ? "" : typeToSection.get(TEST_TYPE).iterator().next();
    String ilr = typeToSection == null || typeToSection.isEmpty() || !typeToSection.containsKey(ILR_LEVEL) ? "" : typeToSection.get(ILR_LEVEL).iterator().next();
    return getLatestSession(userID, test, type, ilr);
  }


/*
  @Override
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
  }
*/


  /**
   * @param copy
   * @return
   * @paramx session
   * @see #getQuizCorrectAndScore(Map, int, Collection)
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
  private Collection<CorrectAndScore> getFirstCorrectAndScore(Collection<CorrectAndScore> resultsForUser) {
    Map<String, CorrectAndScore> idToCorrect = new HashMap<String, CorrectAndScore>();

    for (CorrectAndScore correctAndScore : resultsForUser) {
      String key = correctAndScore.getExid() + "/" + correctAndScore.getQid();
      idToCorrect.put(key, correctAndScore);
    }
    return new ArrayList<CorrectAndScore>(idToCorrect.values()); // required since can't send a values set
  }

}
