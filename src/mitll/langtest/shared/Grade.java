package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created with IntelliJ IDEA.
 * User: go22670
 * Date: 5/18/12
 * Time: 5:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class Grade implements IsSerializable {
  //userid INT, plan VARCHAR, id VARCHAR, qid INT, answer VARCHAR, audioFile VARCHAR, valid BOOLEAN, timestamp
  public int resultID;
  public int grade;
  public boolean correct;
  public Grade() {}

  /**
   * @see mitll.langtest.server.database.GradeDAO
   *
   */
  public Grade(int resultID, int grade, boolean correct) {
    this.resultID = resultID;
    this.grade  = grade;
    this.correct = correct;
  }
}
