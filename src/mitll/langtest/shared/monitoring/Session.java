package mitll.langtest.shared.monitoring;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.shared.flashcard.SetScore;

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
  private int correct;
  private long userid;
  Map<String,Boolean> exidToCorrect = new HashMap<String,Boolean>();

  private Set<String> exids = new HashSet<String>();

  public Session(){} // required
  public Session(long userid) { this.userid = userid;  }

  private long getAverage() { return duration/ getNumAnswers(); }
  public long getSecAverage() { return (duration/ getNumAnswers())/1000; }

  public void addExerciseID(String id) { exids.add(id);  }

  public int getNumAnswers() {
    return numAnswers;
  }

  public void setNumAnswers() {
    this.numAnswers = exids.size();
    exids = null;
  }

  @Override
  public int getCorrect() {
    int count = 0;
    for (Boolean correct : exidToCorrect.values()) {
      if (correct) count++;
    }
    return count;
  }

  public void setCorrect(int correct) {
    this.correct = correct;
  }

  public void incrementCorrect(String id,boolean correct) {
    exidToCorrect.put(id,correct);
  }

  @Override
  public long getUserid() {
    return userid;
  }

  public void setUserid(long userid) {
    this.userid = userid;
  }

  public String toString() { return "num " + getNumAnswers() + " dur " + duration/(60*1000) + " minutes, avg " + getAverage()/1000 + " secs"; }
}
