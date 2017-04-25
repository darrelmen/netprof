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

import java.util.HashMap;
import java.util.Map;

public class ProjectInfo implements HasID, IsSerializable, MutableProject {
  private int id = -1;
  private String name = "";
  private String language = "";
  private String course = "";
  private String countryCode = "";
  private ProjectStatus status = ProjectStatus.DEVELOPMENT;
  private int displayOrder = 0;

  private long created = 0;

  private int port = -1;
  private String modelsDir = "";
  private String firstType = "";
  private String secondType = "";

  private Map<String, String> propertyValue = new HashMap<>();

  public ProjectInfo() {
  } // for serialization

  /**
   * @see ProjectServiceImpl#getAll
   */
  public ProjectInfo(int projectid,
                     String name,
                     String language,
                     String course,
                     String countryCode,
                     ProjectStatus status,
                     int displayOrder,

                     long created,
                     int port,
                     String modelsDir, String first, String secondType) {
    this.language = language;
    this.id = projectid;
    this.name = name;
    this.course = course;
    this.created = created;
    this.status = status;
    this.displayOrder = displayOrder;
    this.countryCode = countryCode;
    this.port = port;
    this.modelsDir = modelsDir;
    this.firstType = first;
    this.secondType =secondType;
  }

  public String getLanguage() {
    return language;
  }

  public int getID() {
    return id;
  }

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

  public Map<String, String> getPropertyValue() {
    return propertyValue;
  }

  public void addProp(String key, String value) {
    propertyValue.put(key, value);
  }

  public String getProp(String key) {
    return propertyValue.get(key);
  }

  public String removeProp(String key) {
    return propertyValue.remove(key);
  }

  public String toString() {
    return getName() + " " + getStatus() + " lang " + language;
  }

  public String getFirstType() {
    return firstType;
  }

  public String getSecondType() {
    return secondType;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setCourse(String course) {
    this.course = course;
  }

  public void setCountryCode(String countryCode) {
    this.countryCode = countryCode;
  }

  public void setDisplayOrder(int displayOrder) {
    this.displayOrder = displayOrder;
  }

  public void setLanguage(String language) {
    this.language = language;
  }
}
