package mitll.langtest.shared.custom;

import com.google.gwt.user.client.rpc.IsSerializable;

public class QuizSpec implements IsSerializable {
  private int roundMinutes;
  private int minScore;
  private boolean showAudio;
  private boolean isDefault = true;

  public QuizSpec() {
  }

  /**
   * @param roundMinutes
   * @param minScore
   * @param showAudio
   * @see mitll.langtest.server.database.custom.UserListManager#getQuizInfo
   */
  public QuizSpec(int roundMinutes, int minScore, boolean showAudio, boolean isDefault) {
    this.roundMinutes = roundMinutes;
    this.minScore = minScore;
    this.showAudio = showAudio;
    this.isDefault = isDefault;
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

  public boolean isDefault() {
    return isDefault;
  }

  public void setDefault(boolean aDefault) {
    isDefault = aDefault;
  }

  public String toString() {
    return "quiz " +
        "\n\tminutes   " + roundMinutes +
        "\n\tminScore  " + minScore +
        "\n\tdefault   " + isDefault +
        "\n\tshowAudio " + showAudio;
  }
}
