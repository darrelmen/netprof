/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.shared.grade;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.server.database.result.Result;

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
