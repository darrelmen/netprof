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

package mitll.langtest.server.amas;

import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.flashcard.QuizCorrectAndScore;

import java.util.*;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 1/4/16.
 */
public class QuizCorrect {
//  private static final Logger logger = LogManager.getLogger(QuizCorrect.class);
//  private static final String QUIZ = "Quiz";
//  private static final String ILR_LEVEL = "ILR Level";
//  private static final String TEST_TYPE = "Test type";
  private final DatabaseImpl db;

  public QuizCorrect(DatabaseImpl db) {
    this.db = db;
  }
  /**
   * @param typeToSection
   * @param userID
   * @param exids
   * @param projectid
   * @return
   * @seex mitll.langtest.client.recorder.FeedbackRecordPanel#getScores
   */
  public QuizCorrectAndScore getScoresForUser(Map<String, Collection<String>> typeToSection,
                                              int userID,
                                              Collection<Integer> exids,
                                              int projectid) {
    //   String session = getLatestSession(typeToSection, userID);

/*    if (typeToSection == null || typeToSection.isEmpty()) {
      List<CorrectAndScore> resultsForUser = db.getResultDAO().getResultsForUser(userID, "");
      Collection<CorrectAndScore> copy = getFirstCorrectAndScore(resultsForUser);
      // filter by session
      List<CorrectAndScore> bySession = filterBySession(copy, session);

      logger.error("\n\n\ngetScoresForUser for user " + userID + "/" + session + " and " + typeToSection + " got " + resultsForUser.size() + " vs unique " + bySession.size());
      return new QuizCorrectAndScore(getExercises().size() == resultsForUser.size(), getExercises().size(), bySession);
    } else {*/
    Collection<Integer> allIDs = new ArrayList<>();
    if (exids == null || exids.isEmpty()) {
      Collection<CommonExercise> exercisesForState =
          db.getSectionHelper(projectid).getExercisesForSelectionState(typeToSection);
      for (CommonExercise exercise : exercisesForState) {
        allIDs.add(exercise.getID());
      }
    } else {
      allIDs = exids;
    }

    String language = db.getProject(projectid).getLanguage();
    return getQuizCorrectAndScore(typeToSection, userID, allIDs, language);
    //  }
  }

  /**
   * @seez #filterByUnrecorded(long, Collection, Map)
   * @param typeToSection
   * @param userID
   * @param allIDs
   * @param language
   * @return
   */
  private QuizCorrectAndScore getQuizCorrectAndScore(Map<String, Collection<String>> typeToSection,
                                                     int userID,
                                                     Collection<Integer> allIDs, String language) {
    String session = "";//getLatestSession(typeToSection, userID);
    //  logger.info("exercises " +allIDs.size() + " for session " + session);

    Collection<CorrectAndScore> resultsForExIDInForUser = db.getResultDAO().getResultsForExIDInForUser(allIDs, userID, session, language);
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
   * @see #getQuizCorrectAndScore(Map, int, Collection, String)
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
   * @see #getQuizCorrectAndScore(Map, int, Collection, String)
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
