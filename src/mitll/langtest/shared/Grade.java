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
  private static final int UNASSIGNED = -1;
  public int id;
  public int resultID;
  public int grade;
  public String grader;
  public String gradeType;

  public Grade() {}

  public Grade(int resultID, int grade, String grader, String gradeType) {
    this(UNASSIGNED,resultID,grade,grader,gradeType);
  }
    /**
    * @see mitll.langtest.server.database.GradeDAO#getResultIDsForExercise(String)
    *
    */
  public Grade(int id, int resultID, int grade,String grader, String gradeType) {
    this.id = id;
    this.resultID = resultID;
    this.grade  = grade;
    this.grader  = grader;
    this.gradeType = gradeType;
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
    return "ID = " + id +" : result " + resultID + " = " + grade + " by " + grader + " type " +gradeType ;
  }
}
