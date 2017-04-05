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

package mitll.langtest.shared.project;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.server.services.ProjectServiceImpl;
import mitll.langtest.shared.exercise.HasID;

public class ProjectInfo implements HasID, IsSerializable, MutableProject {
  private String name;
  private String language;
  private String course;
  private String countryCode;
  private ProjectStatus status;
  private int displayOrder;
  private int id;
  private long created;

  private int port = -1;
  private String modelsDir;

  public ProjectInfo() {
  } // for serialization

  /**
   * @see ProjectServiceImpl#getAll
   */
  public ProjectInfo(int projectid,
                     String language,
                     String name,
                     String course,
                     long created,
                     ProjectStatus status,
                     int displayOrder,
                     String countryCode,
                     String port, String modelsDir) {
    this.language = language;
    this.id = projectid;
    this.name = name;
    this.course = course;
    this.created = created;
    this.status = status;
    this.displayOrder = displayOrder;
    this.countryCode = countryCode;
    try {
      this.port = port == null ? -1 : Integer.parseInt(port);
    } catch (NumberFormatException e) {
      // bad port
    }
    this.modelsDir = modelsDir;
  }

  public String getLanguage() {
    return language;
  }

  public int getID() {
    return id;
  }

/*  public void addChild(ProjectInfo projectInfo) {
    children.add(projectInfo);
  }

  public List<ProjectInfo> getChildren() {
    return children;
  }*/

  @Override
  public int compareTo(HasID o) {
    if (o instanceof ProjectInfo) {
      ProjectInfo otherProject = (ProjectInfo) o;
      ProjectInfo thisProject = this;
      return thisProject.name.compareTo(otherProject.name);
    } else {
      return 1;
    }
  }

  public String getName() {
    return name;
  }

  public long getCreated() {
    return created;
  }

  public ProjectStatus getStatus() {
    return status;
  }

  public int getDisplayOrder() {
    return displayOrder;
  }

  public String getCourse() {
    return course;
  }

  public String getCountryCode() {
    return countryCode;
  }

  @Override
  public void setStatus(ProjectStatus status) {
    this.status = status;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getModelsDir() {
    return modelsDir;
  }

  public void setModelsDir(String modelsDir) {
    this.modelsDir = modelsDir;
  }

  public String toString() {
    return getName() + " " + getStatus() + " lang " + language;
  }
}
