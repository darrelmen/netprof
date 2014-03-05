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
public class ScoreInfo implements IsSerializable,SetScore {
  private long userid;
  private long giverID;
  private int correct;
 // private int incorrect;
 // private long timeTaken;
//  private long timestamp;
  public Map<String, Collection<String>> selection;

  public ScoreInfo() {}

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#getScoreInfo(long, long, java.util.Map)
   * @param userid
   * @param giverID
   * @param correct
   * @param incorrect
   * @param timeTaken
   * @param selection
   */
  public ScoreInfo(long userid, long giverID, int correct, int incorrect, long timeTaken, Map<String, Collection<String>> selection) {
    this.userid = userid;
    this.giverID = giverID;
    this.correct = correct;
  //  this.incorrect = incorrect;
  //  this.timeTaken = timeTaken;
    this.selection = selection;
  //  this.timestamp = System.currentTimeMillis();
  }

  public long getUserid() {
    return userid;
  }

  public long getGiverID() {
    return giverID;
  }

  public int getCorrect() {
    return correct;
  }

  public String toString() {
    return "ScoreInfo : user " + getUserid() + " correct " + getCorrect() + " for " + selection;
  }
}
