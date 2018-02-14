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

import mitll.langtest.client.project.ProjectEditForm;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.exercise.HasID;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ProjectInfo extends DominoProject implements HasID, MutableProject {
  private int id = -1;
  private String language = "";
  private String course = "";
  private String countryCode = "";
  private ProjectStatus status = ProjectStatus.DEVELOPMENT;
  private ProjectType projectType = ProjectType.NP;
  private int displayOrder = 0;

  private long created = 0;
  private long lastImport = 0;

  private String host = Project.WEBSERVICE_HOST_DEFAULT;
  private int port = -1;
  private String modelsDir = "";
  private boolean showOniOS = true;
  private boolean audioPerProject = false;

  private Map<String, String> propertyValue = new HashMap<>();

  public ProjectInfo() {
  } // for serialization

  /**
   * @see mitll.langtest.server.database.project.ProjectManagement#getProjectInfo
   * @see SlimProject#SlimProject
   */
  public ProjectInfo(int projectid,
                     String name,
                     String language,
                     String course,
                     String countryCode,
                     ProjectStatus status,
                     ProjectType projectType,
                     int displayOrder,

                     long created,
                     long lastImport, String host,
                     int port,
                     String modelsDir,
                     String first,
                     String secondType,
                     boolean showOniOS,
                     int dominoID) {
    super(dominoID, name, first, secondType);
    this.language = language;
    this.id = projectid;
    this.course = course;
    this.created = created;
    this.lastImport = lastImport;
    this.status = status;
    this.projectType = projectType;
    this.displayOrder = displayOrder;
    this.countryCode = countryCode;
    this.host = host;
    this.port = port;
    this.modelsDir = modelsDir;
    this.showOniOS = showOniOS;
  }

  public String getLanguage() {
    return language;
  }

  public int getID() {
    return id;
  }

  @Override
  public int compareTo(@NotNull HasID o) {
    if (o instanceof ProjectInfo) {
      ProjectInfo otherProject = (ProjectInfo) o;
      ProjectInfo thisProject = this;
      return thisProject.getName().compareTo(otherProject.getName());
    } else {
      return 1;
    }
  }

  public long getCreated() {
    return created;
  }

  public ProjectStatus getStatus() {
    return status;
  }

  public ProjectType getProjectType() {
    return projectType;
  }

  public void setProjectType(ProjectType projectType) {
    this.projectType = projectType;
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

  public void setCourse(String course) {
    this.course = course;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public void setFirstType(String firstType) {
    this.firstType = firstType;
  }

  public void setSecondType(String secondType) {
    this.secondType = secondType;
  }

  /**
   * @return
   * @see mitll.langtest.client.LangTest#createHostSpecificServices
   * @see mitll.langtest.client.project.ProjectChoices#checkAudio
   */
  public String getHost() {
    return host;
  }


  /**
   * @param host
   * @see ProjectEditForm#updateProject
   */
  public void setHost(String host) {
    this.host = host;
  }

  public boolean isShowOniOS() {
    return showOniOS;
  }

  /**
   * @param showOniOS
   * @see ProjectEditForm#updateProject
   */
  public void setShowOniOS(Boolean showOniOS) {
    this.showOniOS = showOniOS;
  }

  public long getLastImport() {
    return lastImport;
  }

  public boolean isAudioPerProject() {
    return audioPerProject;
  }

  public void setAudioPerProject(boolean audioPerProject) {
    this.audioPerProject = audioPerProject;
  }

  public String toString() {
    return "#" + getID() + "  " + getName() + " " + getStatus() + " " + getProjectType() +
        "\nlang      " + language +
        "\nhost      " + host + ":" + port +
        "\ntypes     [" + getFirstType() + ", " + getSecondType() + "]" +
        "\ndomino    " + getDominoID() +
        "\nown audio " + audioPerProject +
        "\nshowOniOS " + showOniOS +
        "\nimported  " + new Date(lastImport);
  }
}
