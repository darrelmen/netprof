/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.instrumentation;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.client.instrumentation.EventRegistration;

import java.util.Comparator;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/13/13
 * Time: 11:35 AM
 * To change this template use File | Settings | File Templates.
 */
public class Event implements IsSerializable, Comparable<Event> {
  private String widgetID;
  private String widgetType;
  private String exerciseID;
  private String context;
  private long creatorID;
  private long timestamp;
  private String hitID;
  private String device;

  public Event() {}

  /**
   * @param exerciseID
   * @param userID
   * @param timestamp
   * @param hitID
   * @param device
   * @see mitll.langtest.server.database.custom.AnnotationDAO#getUserAnnotations(String)
   */
  public Event(String widgetID, String widgetType, String exerciseID, String context, long userID, long timestamp, String hitID, String device) {
    this.widgetID = widgetID;
    this.widgetType = widgetType;
    this.exerciseID = exerciseID;
    this.context = context;
    this.creatorID = userID;
    this.timestamp = timestamp;
    this.hitID = hitID;
    this.device = device;
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
  public String getDevice() { return device; }
  public String getWidgetType() {
    return widgetType;
  }

  public String getHitID() {
    return hitID;
  }

  @Override
  public int compareTo(Event o) {
    return timestamp < o.timestamp ? -1 :  timestamp > o.timestamp ? +1 :0;
  }

  public String toString() {
      long timestamp = getTimestamp();
      if (timestamp == -1) timestamp = System.currentTimeMillis();
      return "Event on " + getWidgetID() + " by " +  getCreatorID() + " at " + new Date(timestamp) + " info " +
      getExerciseID() + "/" + getContext() + " hit " + getHitID() + " from " +device;
  }
}
