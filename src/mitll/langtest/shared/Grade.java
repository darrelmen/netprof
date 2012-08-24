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
  public int id;
  public int resultID;
  public int grade;
  public String grader;
  //public boolean correct;
  public Grade() {}

  /**
   * @see mitll.langtest.server.database.GradeDAO#getResultIDsForExercise(String)
   *
   */
  public Grade(int id, int resultID, int grade,/*, boolean correct*/String grader) {
    this.id = id;
    this.resultID = resultID;
    this.grade  = grade;
    this.grader  = grader;
    //this.correct = correct;
  }

  @Override
  public int hashCode() {
    return resultID;
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof Grade) &&
        id == ((Grade) obj).id;
  }

  @Override
  public String toString() {
    return "ID = " + id +" : result " + resultID + " = " + grade + " by " + grader;
  }
}
