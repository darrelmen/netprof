package mitll.langtest.server.database.instrumentation;

import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/13/13
 * Time: 11:35 AM
 * To change this template use File | Settings | File Templates.
 */
public class Event {
  private final String widgetID;
  private final String exerciseID;
  private final String context;
  private final long creatorID;
  private long timestamp;

  /**
   * @paramx uniqueID
   * @param exerciseID
   * @paramx field
   * @paramx status
   * @paramx comment
   * @param userID
   * @param timestamp
   * @see mitll.langtest.server.database.custom.AnnotationDAO#getUserAnnotations(String)
   */
  public Event(String widgetID,String exerciseID, String context, long userID, long timestamp) {
    this.widgetID = widgetID;
    this.exerciseID = exerciseID;
    this.context = context;
    this.creatorID = userID;
    this.timestamp = timestamp;
  }

  public String getWidgetID() {
    return widgetID;
  }
  public String getExerciseID() {
    return exerciseID;
  }
  public String getContext() {
    return context;
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
    return "Event on " + getWidgetID() + " by " +  getCreatorID() + " at " + new Date(getTimestamp()) + " info " +
      getExerciseID() + "/" + getContext();
  }
}
