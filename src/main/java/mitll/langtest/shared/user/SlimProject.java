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

package mitll.langtest.shared.user;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.project.ProjectStatus;
import mitll.npdata.dao.SlickProject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SlimProject implements IsSerializable {
  private int projectid;
  private String name;
  private String language;
  private String course;
  private String countryCode;
  private ProjectStatus status;
  private int displayOrder;
  private boolean hasModel;
  private boolean isRTL;
  private List<SlimProject> children = new ArrayList<>();

  //  List<>
  public SlimProject() {
  }

  /**
   * @param projectid
   * @param name
   * @param language
   * @param countryCode
   * @param displayOrder
   * @param hasModel
   * @param isRTL
   * @see mitll.langtest.server.LangTestDatabaseImpl#getProjectInfo(SlickProject)
   */
  public SlimProject(int projectid,
                     String name,
                     String language,
                     String countryCode,
                     String course,
                     ProjectStatus status,
                     int displayOrder,
                     boolean hasModel,
                     boolean isRTL) {
    this.name = name;
    this.language = language;
    this.projectid = projectid;
    this.countryCode = countryCode;
    this.course = course;
    this.status = status;
    this.displayOrder = displayOrder;
    this.hasModel = hasModel;
    this.isRTL = isRTL;
  }

  public String getName() {
    return name;
  }

  public String getLanguage() {
    return language;
  }

  public String getCourse() {
    return course;
  }

  public int getProjectid() {
    return projectid;
  }

  public String getCountryCode() {
    return countryCode;
  }

  public void addChild(SlimProject projectInfo) {
    children.add(projectInfo);
  }

  public boolean hasChildren() {
    return !children.isEmpty();
  }

  public boolean hasChild(int projectid) {
    for (SlimProject child : children) {
      if (child.getProjectid() == projectid) return true;
    }
    return false;
  }

  public SlimProject getChild(int projectid) {
    for (SlimProject child : children) {
      if (child.getProjectid() == projectid) return child;
    }
    return null;
  }

  public List<SlimProject> getChildren() {
    Collections.sort(children, new Comparator<SlimProject>() {
      @Override
      public int compare(SlimProject o1, SlimProject o2) {
        int i = Integer.valueOf(o1.displayOrder).compareTo(o2.displayOrder);
        return i == 0 ? o1.getName().compareTo(o2.getName()) : i;
      }
    });
    return children;
  }

  public int getDisplayOrder() {
    return displayOrder;
  }

  public boolean isHasModel() {
    return hasModel;
  }

  public boolean isRTL() {
    return isRTL;
  }

  public ProjectStatus getStatus() {
    return status;
  }

  public String toString() {
    return "Project #" + projectid + " " + name + " " + language + " " + status +
        " num children " + children.size();
  }
}
