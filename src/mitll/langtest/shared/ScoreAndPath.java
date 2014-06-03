package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created by GO22670 on 6/3/2014.
 */
public class ScoreAndPath implements IsSerializable {
  private Float score;
  private String path;

  public ScoreAndPath() {}
  public ScoreAndPath(Float score, String path) {
    this.score = score;
    this.path = path;
  }

  public Float getScore() {
    return score;
  }

  public String getPath() {
    return path;
  }
}
