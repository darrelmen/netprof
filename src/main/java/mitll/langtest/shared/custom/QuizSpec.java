package mitll.langtest.shared.custom;

import com.google.gwt.user.client.rpc.IsSerializable;

public class QuizSpec implements IsSerializable {
  private int roundMinutes;
  private int minScore;
  private boolean showAudio;

  public QuizSpec() {
  }

  public QuizSpec(int roundMinutes, int minScore, boolean showAudio) {
    this.roundMinutes = roundMinutes;
    this.minScore = minScore;
    this.showAudio = showAudio;
  }

  public int getRoundMinutes() {
    return roundMinutes;
  }

  public int getMinScore() {
    return minScore;
  }

  public boolean isShowAudio() {
    return showAudio;
  }

  public String toString() {
    return "quiz " +
        "\n\tminutes   " + roundMinutes +
        "\n\tminScore  " + minScore +
        "\n\tshowAudio " + showAudio;
  }
}
