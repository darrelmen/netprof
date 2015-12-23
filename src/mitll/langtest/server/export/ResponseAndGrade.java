package mitll.langtest.server.export;

/**
 * An answer and the grae for the answer
 * Also points to which result object this was taken from.
* Created by go22670 on 1/30/15.
*/
public class ResponseAndGrade {
  public final String response;
  public final float grade;
  private int resultID;

  /**
   * Put grade onto a 0-1 scale from (typically) 1-6 scale
   *
   * @param response
   * @param grade
   * @param maxGrade
   * @see ExerciseExport#addRG
   */
  public ResponseAndGrade(String response, int grade, int maxGrade) {
    this.response = response;
    this.grade = ((float) (grade - 1)) / (float) (maxGrade - 1);  // jacob's stuff wants a 0->1 scale
  }

  public int getResultID() {
    return resultID;
  }

  public void setResultID(int resultID) {
    this.resultID = resultID;
  }

  @Override
  public String toString() {
    return (resultID == 0 ? "" : "rid #" +resultID + " ") + "g " + grade + " for '" + response + "'";
  }
}
