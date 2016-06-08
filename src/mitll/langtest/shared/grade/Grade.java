/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.shared.grade;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.server.database.result.Result;

/**
 * Represents a grade of a {@link Result} by a grader.
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
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 5/18/12
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
