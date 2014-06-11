package mitll.langtest.client.scoring;

import mitll.langtest.shared.ScoreAndPath;
import mitll.langtest.shared.scoring.PretestScore;

/**
 * Created by GO22670 on 6/11/2014.
 */
public class EmptyScoreListener implements ScoreListener {
  @Override
  public void setClassAvg(float classAvg) {}

  @Override
  public void gotScore(PretestScore score, boolean showOnlyOneExercise, String path) {}

  @Override
  public int getOffsetWidth() { return 0; }

  @Override
  public void addScore(ScoreAndPath hydecScore) {}

  @Override
  public void showChart(boolean showOnlyOneExercise) {}

  @Override
  public void setRefAudio(String refAudio) {}
}
