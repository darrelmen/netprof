package mitll.langtest.server.database.result;

/**
 * Created by go22670 on 4/13/17.
 */
public class SlimResult implements  ISlimResult{
  protected boolean valid;
  protected float pronScore;
  private transient String jsonScore;

  public SlimResult(boolean valid, String jsonScore, float pronScore) {
    this.valid = valid;
    this.jsonScore = jsonScore;
    this.pronScore = pronScore;
  }

  public boolean isValid() {
    return valid;
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
