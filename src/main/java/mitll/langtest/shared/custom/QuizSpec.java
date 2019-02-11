package mitll.langtest.shared.custom;

import com.google.gwt.user.client.rpc.IsSerializable;

public class QuizSpec implements IsSerializable {
  private int roundMinutes;
  private int minScore;
  private boolean showAudio;
  private boolean isDefault = true;
  private String accessCode = "";

  public QuizSpec() {
    this(10, 30, false, true, "");
  }

  /**
   * @param roundMinutes
   * @param minScore
   * @param showAudio
   * @param accessCode
   * @see mitll.langtest.server.database.custom.UserListManager#getQuizInfo
   */
  public QuizSpec(int roundMinutes, int minScore, boolean showAudio, boolean isDefault, String accessCode) {
    this.roundMinutes = roundMinutes;
    this.minScore = minScore;
    this.showAudio = showAudio;
    this.isDefault = isDefault;
    this.accessCode = accessCode;
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

  public String getAccessCode() {
    return accessCode;
  }

  public String toString() {
    return "quiz " +
        "\n\tminutes    " + roundMinutes +
        "\n\tminScore   " + minScore +
        "\n\tdefault    " + isDefault +
        "\n\taccessCode " + accessCode +
        "\n\tshowAudio  " + showAudio;
  }
}
