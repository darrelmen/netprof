package mitll.langtest.shared.monitoring;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.shared.flashcard.SetScore;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
* Created with IntelliJ IDEA.
* User: GO22670
* Date: 1/11/13
* Time: 7:07 PM
* To change this template use File | Settings | File Templates.
*/
public class Session implements IsSerializable, SetScore {
  private int numAnswers;
  public long duration;
  private long userid;
  private int correct;
  private float correctPercent;
  private float avgScore;
  private Map<String,Boolean> exidToCorrect = new HashMap<String,Boolean>();
  private Map<String,Float> exidToScore = new HashMap<String,Float>();

  private Set<String> exids = new HashSet<String>();

  public Session(){} // required

  /**
   * @see mitll.langtest.server.database.ResultDAO#partitionIntoSessions2
   * @param userid
   */
  public Session(long userid) { this.userid = userid;  }

  public long getAverageDurMillis() { return duration/ getNumAnswers(); }
  public long getSecAverage() { return (duration/ getNumAnswers())/1000; }

  /**
   * @see mitll.langtest.server.database.ResultDAO#partitionIntoSessions2
   * @param id
   */
  public void addExerciseID(String id) { exids.add(id);  }

  public int getNumAnswers() {
    return numAnswers;
  }

  public void setNumAnswers() {
    this.numAnswers = exids.size();
    this.avgScore = calcAvgScore();
    this.correct = calcCorrect();
    correctPercent = 100f*((float)correct/(float)numAnswers);

   // System.out.println("setNumAnswers correct "+ correct + "total "  +exidToCorrect.size() + " % = " + correctPercent);

    exids = null;
    exidToCorrect = null;
    exidToScore = null;
  }

  private int calcCorrect() {
    int count = 0;
    for (Boolean correct : exidToCorrect.values()) {
      if (correct) count++;
    }
    return count;
  }

  private float calcAvgScore() {
    float total = 0f;
    Collection<Float> values = exidToScore.values();
    int num = 0;
    for (Float score : values) {
      if (score > 0f) {
        num++;
        total += score;
      }
    }
    float v = num == 0 ? 0f : (total / (float) num);
    //System.out.println("calcAvgScore scores "+ values + " = total " + total + " num " + num + " = "  +v);

    return v;
  }

  @Override
  public int getCorrect() {
    return correct;
  }

  @Override
  public float getAvgScore() { return avgScore; }

  public void incrementCorrect(String id, boolean correct) {
    exidToCorrect.put(id, correct);
  }

  @Override
  public long getUserid() {
    return userid;
  }

  public void setUserid(long userid) {
    this.userid = userid;
  }

  public void setScore(String id, float pronScore) {
    exidToScore.put(id, pronScore);
  }

  public float getCorrectPercent() {
    return correctPercent;
  }

  public String toString() {
    return "user " + userid+
      " num " + getNumAnswers() + " dur " + duration/(60*1000) + " minutes, avg " + getAverageDurMillis()/1000 +
      " secs, correct = " + correct + "(" + correctPercent+
      "%) avg score : " + avgScore;
  }
}
