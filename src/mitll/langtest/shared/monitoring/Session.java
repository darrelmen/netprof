package mitll.langtest.shared.monitoring;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.HashSet;
import java.util.Set;

/**
* Created with IntelliJ IDEA.
* User: GO22670
* Date: 1/11/13
* Time: 7:07 PM
* To change this template use File | Settings | File Templates.
*/
public class Session implements IsSerializable {
  private int numAnswers;
  public long duration;
  private Set<String> exids = new HashSet<String>();

  private long getAverage() { return duration/ getNumAnswers(); }
  public long getSecAverage() { return (duration/ getNumAnswers())/1000; }

  public void addExerciseID(String id) {
    exids.add(id);
  }

  public int getNumAnswers() {
    return numAnswers;
  }

  public void setNumAnswers() {
    this.numAnswers = exids.size();
    exids = null;
  }

  public String toString() { return "num " + getNumAnswers() + " dur " + duration/(60*1000) + " minutes, avg " + getAverage()/1000 + " secs"; }
}
