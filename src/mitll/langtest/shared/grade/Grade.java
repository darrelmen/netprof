package mitll.langtest.shared.grade;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Represents a grade of a {@link mitll.langtest.shared.Result} by a grader.
 * <br></br>
 * So it has a reference to a result id, a grade, the name of the grader, and the grade type.
 * <br></br>
 * The type allows us to differentiate between "english-only" grades and "arabic" grades.
 * <br></br>
 * For instance, we don't want to show arabic grades to the english only people and vice-versa.
 *
 * Grader logs in, registering initially.
 *
 * On the left, questions sorted (not random), each shows how many have been graded (n graders, # complete) and
 * how many responses were collected for the item (which could be a multi part question).
 *
 * E.g. if 2 graders, each could be part way done with responses and so 0 complete.
 *
 * Need a Grade object which will be for each entry in the results table.
 *
 * Either 1-5 scale or that + a Correct? Yes/No scale.  Maybe this is an option in the nature of the exercise?
 *
 * Show question, with table of responses. Note which have been graded so far.
 *
 * User: go22670
 * Date: 5/18/12
 * Time: 5:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class Grade implements IsSerializable {
  private static final int UNASSIGNED = -1;
  public int id;
  private String exerciseID;
  public int resultID;
  public int grade;
  private int grader;
  private int gradeIndex;
  private String gradeType;

  public Grade() {}

  /**
   * @see mitll.langtest.client.grading.GradingResultManager#getGradingColumn
   * @param resultID
   * @param grade
   * @param grader
   * @param gradeType
   */
  public Grade(int resultID, int grade, int grader, String gradeType, int gradeIndex) {
    this(UNASSIGNED, "", resultID,grade,grader,gradeType,gradeIndex);
  }
    /**
    * @see mitll.langtest.server.database.GradeDAO#getGradesForSQL(String)
    *
    */
  public Grade(int id, String exerciseID, int resultID, int grade, int grader, String gradeType, int gradeIndex) {
    this.id = id;
    this.exerciseID = exerciseID;
    this.resultID = resultID;
    this.grade  = grade;
    this.grader  = grader;
    this.gradeType = gradeType;
    this.gradeIndex = gradeIndex;
  }

  @Override
  public int hashCode() {
    return resultID;
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof Grade) && id == ((Grade) obj).id;
  }

  @Override
  public String toString() {
    String idToShow = id == UNASSIGNED ? "UNASSIGNED" : ""+id;
    return "ID = " + idToShow +"\t: exercise "+exerciseID +"\t: result " + resultID + "\t= " + grade +
        " by user id = " + grader + " type " +gradeType + " index " +gradeIndex;
  }
}
