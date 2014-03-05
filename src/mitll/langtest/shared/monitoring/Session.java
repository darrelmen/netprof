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
  private float avgScore;
  private Map<String,Boolean> exidToCorrect = new HashMap<String,Boolean>();
  private Map<String,Float> exidToScore = new HashMap<String,Float>();

  private Set<String> exids = new HashSet<String>();

  public Session(){} // required
  public Session(long userid) { this.userid = userid;  }

  public long getAverageDurMillis() { return duration/ getNumAnswers(); }
  public long getSecAverage() { return (duration/ getNumAnswers())/1000; }

  public void addExerciseID(String id) { exids.add(id);  }

  public int getNumAnswers() {
    return numAnswers;
  }

  public void setNumAnswers() {
    this.numAnswers = exids.size();
    this.avgScore = calcAvgScore();
    this.correct = calcCorrect();
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
    System.out.println("scores "+ values);
    for (Float score : values) {
      total += Math.max(0f,score);
    }
    return total/(float)values.size();
  }

  @Override
  public int getCorrect() {
    return correct;
  }

  @Override
  public float getAvgScore() { return avgScore; }

  public void incrementCorrect(String id,boolean correct) {
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

  public String toString() {
    return "user " + userid+
      " num " + getNumAnswers() + " dur " + duration/(60*1000) + " minutes, avg " + getAverageDurMillis()/1000 +
      " secs " + correct + " avg score : " + avgScore;
  }
}
