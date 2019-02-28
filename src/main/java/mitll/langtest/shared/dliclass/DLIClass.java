/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.shared.dliclass;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Date;

public class DLIClass implements IsSerializable {
  private int id;
  private int teacherid;
  private Date created;
  private Date end;
  private int projid;
  private String name;
  private String room;
  private String timeOfDay;

  public DLIClass() {
  }

  public DLIClass(int id,
                  int projid,
                  int teacherid,

                  Date created,
                  Date end,
                  String name,
                  String room,
                  String timeOfDay) {
    this.id = id;
    this.projid = projid;
    this.teacherid = teacherid;
    this.created = created;
    this.end = end;
    this.name = name;
    this.room = room;
    this.timeOfDay = timeOfDay;
  }


  public int getId() {
    return id;
  }

  public int getTeacherid() {
    return teacherid;
  }

  /**
   * TODO : joba time?
   */
  public Date getCreated() {
    return created;
  }

  public Date getEnd() {
    return end;
  }

  public int getProjid() {
    return projid;
  }

  public String getName() {
    return name;
  }

  public String getRoom() {
    return room;
  }

  public String getTimeOfDay() {
    return timeOfDay;
  }

  public String toString() { return "Class " + name; }
}
