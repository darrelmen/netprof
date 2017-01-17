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

package mitll.langtest.server.database.annotation;

import java.util.Date;

public class UserAnnotation {
  private int exerciseID;
  private final String field;
  private final String status;
  private final String comment;
  private long creatorID;
  private long timestamp;
  private String oldExID;

  public UserAnnotation(int exerciseID, String field, String status, String comment, long userID, long timestamp) {
    this(exerciseID, field, status, comment, userID, timestamp, "" + exerciseID);
  }

  /**
   * @param exerciseID
   * @param field
   * @param status
   * @param comment
   * @param userID
   * @param timestamp
   * @param oldExID
   * @paramx uniqueID
   * @see AnnotationDAO#getUserAnnotations(String)
   */
  public UserAnnotation(int exerciseID, String field, String status, String comment, long userID, long timestamp, String oldExID) {
    this.exerciseID = exerciseID;
    this.field = field;
    this.status = status;
    this.comment = comment;
    this.creatorID = userID;
    this.timestamp = timestamp;
    this.oldExID = oldExID;
  }

  public int getExerciseID() {
    return exerciseID;
  }
  public void setExerciseID(int exerciseID) { this.exerciseID = exerciseID;}

  public String getField() {
    return field;
  }

  public String getStatus() {
    return status;
  }

  public String getComment() {
    return comment;
  }

  public long getCreatorID() {
    return creatorID;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public void setCreatorID(Integer creatorID) {
    this.creatorID = (long) creatorID;
  }

  public String toString() {
    return "Annotation " + getExerciseID() + "/" + getField() +
        " : " + getStatus() +
        "/" + getComment() +
        " by " + getCreatorID() +
        " at " + new Date(getTimestamp());
  }

  public String getOldExID() {
    return oldExID;
  }
}
