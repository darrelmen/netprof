/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.database.custom;

import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/13/13
 * Time: 11:35 AM
 * To change this template use File | Settings | File Templates.
 */
public class UserAnnotation {
  private final String exerciseID;
  private final String field;
  private final String status;
  private final String comment;
  private final long creatorID;
  private long timestamp;

  /**
   * @paramx uniqueID
   * @param exerciseID
   * @param field
   * @param status
   * @param comment
   * @param userID
   * @param timestamp
   * @see mitll.langtest.server.database.custom.AnnotationDAO#getUserAnnotations(String)
   */
  public UserAnnotation(String exerciseID, String field, String status, String comment, long userID, long timestamp) {
    this.exerciseID = exerciseID;
    this.field = field;
    this.status = status;
    this.comment = comment;
    this.creatorID = userID;
    this.timestamp = timestamp;
  }


  public String getExerciseID() {
    return exerciseID;
  }
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

  public String toString() {
    return "Annotation " + getExerciseID() + "/" + getField() + " : " + getStatus() + "/" + getComment() +
      " by " + getCreatorID() + " at " + new Date(getTimestamp());
  }
}
