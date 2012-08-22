package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created with IntelliJ IDEA.
 * User: go22670
 * Date: 5/18/12
 * Time: 5:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class Result implements IsSerializable {
  //userid INT, plan VARCHAR, id VARCHAR, qid INT, answer VARCHAR, audioFile VARCHAR, valid BOOLEAN, timestamp
  public int uniqueID;
  public long userid;
  public String plan;
  public String id;
  public int qid;
  public String answer;
 // public String audioFile;
  public boolean valid;
  public long timestamp;
  public boolean flq;
  public boolean spoken;

  public Result() {}

  /**
   * @see mitll.langtest.server.database.ResultDAO#getResults()
   * @param userid
   * @param plan
   * @param id
   * @param qid
   * @param answer
   * @param valid
   * @param timestamp
   */
  public Result(int uniqueID
                ,long userid, String plan, String id, int qid, String answer, /*String audioFile,*/ boolean  valid, long timestamp) {
   this.uniqueID = uniqueID;
    this.userid = userid;
    this.plan  = plan;
    this.id = id;
    this.qid = qid;
    this.answer = answer;
//    this.audioFile = audioFile;
    this.valid = valid;
    this.timestamp = timestamp;
  }

  public void setFLQ(boolean flq)  { this.flq = flq; }
  public void setSpoken(boolean v) { this.spoken = v; }

  @Override
  public String toString() {
    return "Result #"+uniqueID + " by user " + userid + " id " + id;
  }
}
