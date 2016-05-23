/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.instrumentation;

import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/13/13
 * Time: 11:35 AM
 * To change this template use File | Settings | File Templates.
 */
public class Event extends SlimEvent {
  private Long id;
  private String widgetID;
  private String widgetType;
  private String exerciseID;
  private String context;

//  private String hitID;
  private String device;

  public Event() {
  }

  /**
   * @param exerciseID
   * @param userID
   * @param timestamp
   * @paramx hitID
   * @param device
   * @see mitll.langtest.server.database.custom.AnnotationDAO#getUserAnnotations(String)
   */
  public Event(String widgetID, String widgetType, String exerciseID, String context,
               long userID, long timestamp,
               String device) {
    super(userID,timestamp);
    this.widgetID = widgetID;
    this.widgetType = widgetType;
    this.exerciseID = exerciseID;
    this.context = context;
    this.device = device;
  }

  public Long getId() {
    return id;
  }

  private void setId(Long id) {
    this.id = id;
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

/*
  public Date getSQLTimestamp() {
    return new Date(timestamp);
  }

  private void setSQLTimestamp(Date timestamp) {
    this.timestamp = timestamp.getTime();
  }

*/
  public String getDevice() {
    return device;
  }

  public String getWidgetType() {
    return widgetType;
  }

/*
  public String getHitID() {
    return hitID;
  }
*/

  public String toString() {
    long timestamp = getTimestamp();
    if (timestamp == -1) timestamp = System.currentTimeMillis();
    return "Event on " + getWidgetID() + " by " + getUserID() + " at " + new Date(timestamp) + " info " +
        getExerciseID() + "/" + getContext() +
        //" hit " + getHitID() +
        " from " + device;
  }

/*
  private void setWidgetID(String widgetID) {
    this.widgetID = widgetID;
  }

  private void setWidgetType(String widgetType) {
    this.widgetType = widgetType;
  }

  private void setExerciseID(String exerciseID) {
    this.exerciseID = exerciseID;
  }
*/

  private void setContext(String context) {
    this.context = context;
  }

/*
  public void setCreatorID(long creatorID) {
    this.creatorID = creatorID;
  }
*/

/*
  private void setHitID(String hitID) {
    this.hitID = hitID;
  }

  private void setDevice(String device) {
    this.device = device;
  }
*/

  public void truncate() {
    String context = getContext();
    if (context.length() > 255) {
      setContext(context.substring(0, 255));
    }
  }
}
