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

package mitll.langtest.shared.instrumentation;

import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 12/13/13
 * Time: 11:35 AM
 * To change this template use File | Settings | File Templates.
 */
public class Event extends SlimEvent {
  private String widgetID;
  private String widgetType;
  private String exerciseID;
  private String context;

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
    super(userID,timestamp);
    this.widgetID = widgetID;
    this.widgetType = widgetType;
    this.exerciseID = exerciseID;
    this.context = context;
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

  public String getDevice() { return device; }
  public String getWidgetType() {
    return widgetType;
  }

  public String getHitID() {
    return hitID;
  }



  public String toString() {
      long timestamp = getTimestamp();
      if (timestamp == -1) timestamp = System.currentTimeMillis();
      return "Event on " + getWidgetID() + " by " +  getCreatorID() + " at " + new Date(timestamp) + " info " +
      getExerciseID() + "/" + getContext() + " hit " + getHitID() + " from " +device;
  }
}
