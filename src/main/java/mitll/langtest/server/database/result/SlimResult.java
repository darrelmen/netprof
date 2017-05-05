package mitll.langtest.server.database.result;

/**
 * Created by go22670 on 4/13/17.
 */
public class SlimResult implements ISlimResult {
  protected boolean valid;
  protected float pronScore;
  private transient String jsonScore;
  private int audioID;

  /**
   * @param audioID
   * @param valid
   * @param jsonScore
   * @param pronScore
   * @see mitll.langtest.server.database.refaudio.SlickRefResultDAO#fromSlickToSlim
   */
  public SlimResult(int audioID, boolean valid, String jsonScore, float pronScore) {
    this.audioID = audioID;
    this.valid = valid;
    this.jsonScore = jsonScore;
    this.pronScore = pronScore;
  }

  public boolean isValid() {
    return valid;
  }

  @Override
  public int getAudioID() {
    return audioID;
  }

  @Override
  public String getJsonScore() {
    return jsonScore;
  }

  public void setJsonScore(String jsonScore) {
    this.jsonScore = jsonScore;
  }

  @Override
  public float getPronScore() {
    return pronScore;
  }
}
