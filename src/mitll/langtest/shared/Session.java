package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
* Created with IntelliJ IDEA.
* User: GO22670
* Date: 1/11/13
* Time: 7:07 PM
* To change this template use File | Settings | File Templates.
*/
public class Session implements IsSerializable {
  public int numAnswers;
  public long duration;
  public long getAverage() { return duration/numAnswers; }
  public long getSecAverage() { return (duration/numAnswers)/1000; }
  public String toString() { return "num " + numAnswers + " dur " + duration/(60*1000) + " min, avg " + getAverage()/1000 + " secs"; }
}
