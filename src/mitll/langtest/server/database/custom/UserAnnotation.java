package mitll.langtest.server.database.custom;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/13/13
 * Time: 11:35 AM
 * To change this template use File | Settings | File Templates.
 */
public class UserAnnotation {
  private long uniqueID;
  private String exerciseID; private String field; private String status; private String comment;
  private long creatorID;

   private long timestamp;
  public UserAnnotation(String exerciseID, String field, String status, String comment, long userID) {
    this.exerciseID = exerciseID;
    this.field = field;
    this.status = status;
    this.comment =comment;
    this.creatorID = userID;
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

  public void setUniqueID(long uniqueID) {
    this.uniqueID = uniqueID;
  }

  public String toString() { return "Annotation " + getExerciseID() + "/"+ getField() + " : " + getStatus() +"/" + getComment() + " by " + getCreatorID();}
}
