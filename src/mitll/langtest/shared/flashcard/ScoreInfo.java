package mitll.langtest.shared.flashcard;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Collection;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 8/5/13
 * Time: 7:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class ScoreInfo implements IsSerializable {
  public long userid;
  public int correct;
  private int incorrect;
  private long timeTaken;
  private long timestamp;
  public Map<String, Collection<String>> selection;

  public ScoreInfo() {}

  public ScoreInfo(long userid, int correct, int incorrect, long timeTaken, Map<String, Collection<String>> selection) {
    this.userid = userid;
    this.correct = correct;
    this.incorrect = incorrect;
    this.timeTaken = timeTaken;
    this.selection = selection;
    this.timestamp = System.currentTimeMillis();
  }

  public String toString() {
    return "user " + userid + " correct " + correct + " for " + selection;
  }
}
