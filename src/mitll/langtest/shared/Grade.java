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
  public int resultID;
  public int grade;
  //public boolean correct;
  public Grade() {}

  /**
   * @see mitll.langtest.server.database.GradeDAO
   *
   */
  public Grade(int resultID, int grade/*, boolean correct*/) {
    this.resultID = resultID;
    this.grade  = grade;
    //this.correct = correct;
  }

  @Override
  public int hashCode() {
    return resultID;
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof Grade) && resultID == ((Grade) obj).resultID && grade == ((Grade) obj).grade;
  }

  @Override
  public String toString() {
    return "Result " + resultID + " = " + grade;
  }
}
