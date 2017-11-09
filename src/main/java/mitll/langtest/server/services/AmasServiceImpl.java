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

import mitll.langtest.client.services.AmasService;
import mitll.langtest.server.amas.QuizCorrect;
import mitll.langtest.server.autocrt.AutoCRT;
import mitll.langtest.shared.amas.Answer;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.flashcard.QuizCorrectAndScore;
import mitll.langtest.shared.scoring.AudioContext;

import java.util.Collection;
import java.util.Map;

@SuppressWarnings("serial")
public class AmasServiceImpl extends MyRemoteServiceServlet implements AmasService {
  /**
   * JUST FOR AMAS
   * TODO : put this back
   *
   * @param audioContext
   * @param answer
   * @param timeSpent
   * @param typeToSection
   * @return
   * @see mitll.langtest.client.amas.TextResponse#getScoreForGuess
   */
  public Answer getScoreForAnswer(AudioContext audioContext, String answer,
                                  long timeSpent,
                                  Map<String, Collection<String>> typeToSection) {
    // AutoCRT.CRTScores scoreForAnswer1 = audioFileHelper.getScoreForAnswer(exercise, questionID, answer);
    AutoCRT.CRTScores scoreForAnswer1 = new AutoCRT.CRTScores();
    double scoreForAnswer = serverProps.useMiraClassifier() ? scoreForAnswer1.getNewScore() : scoreForAnswer1.getOldScore();

    String session = "";// getLatestSession(typeToSection, userID);
    //  logger.warn("getScoreForAnswer user " + userID + " ex " + exercise.getOldID() + " qid " +questionID + " type " +typeToSection + " session " + session);
    boolean correct = scoreForAnswer > 0.5;
    long resultID = db.getAnswerDAO().addTextAnswer(audioContext,
        answer,
        correct,
        (float) scoreForAnswer, (float) scoreForAnswer, session, timeSpent);

    return new Answer(scoreForAnswer, correct, resultID);
  }

  /**
   * JUST FOR AMAS
   *
   * @param typeToSection
   * @param exids
   * @return
   */
  public QuizCorrectAndScore getScoresForUser(Map<String, Collection<String>> typeToSection,
                                              Collection<Integer> exids) {
    try {
      return new QuizCorrect(db).getScoresForUser(typeToSection, getUserIDFromSessionOrDB(), exids, getProjectIDFromUser());
    } catch (DominoSessionException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * JUST FOR AMAS
   *
   * @param resultID
   * @param correct
   */
  @Override
  public void addStudentAnswer(long resultID, boolean correct) {
    db.getAnswerDAO().addUserScore((int) resultID, correct ? 1.0f : 0.0f);
  }
}
