package mitll.langtest.server.database.result;

import mitll.npdata.dao.SlickRefResult;

/**
 * Created by go22670 on 4/13/17.
 */
public class SlimResult implements ISlimResult {
  private final boolean valid;
  private final float pronScore;
  private transient String jsonScore;
  private final int exID;
  private final int audioID;

  /**
   *
   * @param exID
   * @param audioID
   * @param valid
   * @param jsonScore
   * @param pronScore
   * @see mitll.langtest.server.database.refaudio.SlickRefResultDAO#fromSlickToSlim
   */
  public SlimResult(int exID, int audioID, boolean valid, String jsonScore, float pronScore) {
    this.exID = exID;
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

  /**
   * @see mitll.langtest.server.database.refaudio.SlickRefResultDAO#fromSlick
   * @param jsonScore
   */
  public void setJsonScore(String jsonScore) {
    this.jsonScore = jsonScore;
  }

  @Override
  public float getPronScore() {
    return pronScore;
  }

  @Override
  public int getExID() {
    return exID;
  }
}
