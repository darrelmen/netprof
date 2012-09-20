package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Represents a grade of a {@link Result} by a grader.
 * <br></br>
 * So it has a reference to a result id, a grade, the name of the grader, and the grade type.
 * <br></br>
 * The type allows us to differentiate between "english-only" grades and "arabic" grades.
 * <br></br>
 * For instance, we don't want to show arabic grades to the english only people and vice-versa.
 *
 * User: go22670
 * Date: 5/18/12
 * Time: 5:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class Grade implements IsSerializable {
  public static final int UNASSIGNED = -1;
  public int id;
  public int resultID;
  public int grade;
  public String grader;
  public String gradeType;

  public Grade() {}

  /**
   * @see mitll.langtest.client.grading.GradingResultManager#getGradingColumn(java.util.Collection, String, int, boolean)
   * @param resultID
   * @param grade
   * @param grader
   * @param gradeType
   */
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
    return "ID = " + id +"\t: result " + resultID + "\t= " + grade + " by " + grader + " type " +gradeType ;
  }
}
