/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.scoring;

import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.scoring.PretestScore;

/**
 * Created with IntelliJ IDEA.
 * User: go22670
 * Date: 9/12/12
 * Time: 6:59 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ScoreListener {
  void gotScore(PretestScore score, boolean showOnlyOneExercise, String path);
  int getOffsetWidth();

  void addScore(CorrectAndScore hydecScore);

  /**
   * @see mitll.langtest.client.scoring.ScoringAudioPanel#setClassAvg(float)
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#addUserRecorder(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, com.google.gwt.user.client.ui.Panel, float, mitll.langtest.shared.CommonExercise)
   * @param classAvg
   */
  void setClassAvg(float classAvg);

  void showChart(boolean showOnlyOneExercise);

  void setRefAudio(String refAudio);
}
