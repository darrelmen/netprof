package mitll.langtest.server.database.custom;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/13/13
 * Time: 11:35 AM
 * To change this template use File | Settings | File Templates.
 */
public class UserAnnotation {
  String exerciseID; String field; String status; String comment;
  // long timestamp
  public UserAnnotation(String exerciseID, String field, String status, String comment) {
    this.exerciseID = exerciseID;
    this.field = field;
    this.status = status;
    this.comment =comment;
  }
}
