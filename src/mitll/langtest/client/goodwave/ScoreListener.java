package mitll.langtest.client.goodwave;

import mitll.langtest.shared.scoring.PretestScore;

/**
 * Created with IntelliJ IDEA.
 * User: go22670
 * Date: 9/12/12
 * Time: 6:59 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ScoreListener {
  void gotScore(PretestScore score);
}
