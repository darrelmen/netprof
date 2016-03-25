/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.instrumentation;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/13/13
 * Time: 11:35 AM
 * To change this template use File | Settings | File Templates.
 */
@Entity
@Table(name = "EVENT")
public class Event implements IsSerializable, Comparable<Event> {
  private Long id;

  private String widgetID;
  private String widgetType;
  private String exerciseID;
  private String context;
  private long creatorID;
  private long timestamp;
  private String hitID;
  private String device;

  public Event() {
  }

  /**
   * @param exerciseID
   * @param userID
   * @param timestamp
   * @param hitID
   * @param device
   * @see mitll.langtest.server.database.custom.AnnotationDAO#getUserAnnotations(String)
   */
  public Event(//long id,
               String widgetID, String widgetType, String exerciseID, String context,
               long userID, long timestamp, String hitID, String device) {
    //this.id = id;
    this.widgetID = widgetID;
    this.widgetType = widgetType;
    this.exerciseID = exerciseID;
    this.context = context;
    this.creatorID = userID;
    this.timestamp = timestamp;
    this.hitID = hitID;
    this.device = device;
  }

  @Id
  @GeneratedValue(generator = "increment")
  @GenericGenerator(name = "increment", strategy = "increment")
  //@Column(name = "uniqueid")
  public Long getId() {
    return id;
  }

  private void setId(Long id) {
    this.id = id;
  }

  //@Column(name = "widgettype")
  public String getWidgetID() {
    return widgetID;
  }

  //@Column(name = "exerciseid")
  public String getExerciseID() {
    return exerciseID;
  }

  //@Column(name = "creatorid")
  public String getContext() {
    return context;
  }

  //@Column(name = "creatorid")
  public long getCreatorID() {
    return creatorID;
  }

  //@Column(name = "modified")
  @Transient
  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  @Temporal(TemporalType.TIMESTAMP)
  public Date getSQLTimestamp() {
    return new Date(timestamp);
  }

  private void setSQLTimestamp(Date timestamp) {
    this.timestamp = timestamp.getTime();
  }

  //@Column(name = "creatorid")
  public String getDevice() {
    return device;
  }

  //@Column(name = "creatorid")
  public String getWidgetType() {
    return widgetType;
  }

  //@Column(name = "creatorid")
  public String getHitID() {
    return hitID;
  }

  @Override
  public int compareTo(Event o) {
    return timestamp < o.timestamp ? -1 : timestamp > o.timestamp ? +1 : 0;
  }

  public String toString() {
    long timestamp = getTimestamp();
    if (timestamp == -1) timestamp = System.currentTimeMillis();
    return "Event on " + getWidgetID() + " by " + getCreatorID() + " at " + new Date(timestamp) + " info " +
        getExerciseID() + "/" + getContext() + " hit " + getHitID() + " from " + device;
  }

  private void setWidgetID(String widgetID) {
    this.widgetID = widgetID;
  }

  private void setWidgetType(String widgetType) {
    this.widgetType = widgetType;
  }

  private void setExerciseID(String exerciseID) {
    this.exerciseID = exerciseID;
  }

  private void setContext(String context) {
    this.context = context;
  }

  private void setCreatorID(long creatorID) {
    this.creatorID = creatorID;
  }

  private void setHitID(String hitID) {
    this.hitID = hitID;
  }

  private void setDevice(String device) {
    this.device = device;
  }
}
