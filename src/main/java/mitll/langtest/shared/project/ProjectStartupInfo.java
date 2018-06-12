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
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.exercise.MatchInfo;
import mitll.langtest.shared.exercise.SectionNode;
import mitll.langtest.shared.user.User;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Once a user has chosen a project, what the UI needs to show initial views
 */
public class ProjectStartupInfo implements IsSerializable {
  private Map<String, String> properties;
  private List<String> typeOrder;
  private Collection<SectionNode> sectionNodes;
  private int projectid;
  private String language;

  /**
   * TODO : Might want to do more with this - attach associated font, etc.
   */
  private Language languageInfo;
  private boolean hasModel;
  private Map<String, Set<MatchInfo>> typeToDistinct;
  private Set<String> rootNodes;
  private Map<String, String> parentToChild;
  private String locale;
  private ProjectType projectType;

  public ProjectStartupInfo() {
  } // for serialization

  /**
   * @param properties
   * @param typeOrder
   * @param sectionNodes
   * @param projectid
   * @param languageInfo
   * @param hasModel
   * @param rootNodes
   * @param parentToChild
   * @param projectType
   * @see mitll.langtest.server.database.project.ProjectManagement#setStartupInfoOnUser
   */
  public ProjectStartupInfo(Map<String, String> properties,
                            List<String> typeOrder,
                            Collection<SectionNode> sectionNodes,
                            int projectid,
                            String language,
                            Language languageInfo,
                            String locale,
                            boolean hasModel,
                            Map<String, Set<MatchInfo>> typeToDistinct,
                            Set<String> rootNodes,
                            Map<String, String> parentToChild, ProjectType projectType) {
    this.properties = properties;
    this.typeOrder = typeOrder;
    this.sectionNodes = sectionNodes;
    this.projectid = projectid;
    this.language = language;
    this.languageInfo = languageInfo;
    this.locale = locale;
    this.hasModel = hasModel;
    this.typeToDistinct = typeToDistinct;
    this.rootNodes = rootNodes;
    this.parentToChild = parentToChild;
    this.projectType = projectType;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public List<String> getTypeOrder() {
    return typeOrder;
  }

  public Collection<SectionNode> getSectionNodes() {
    return sectionNodes;
  }

  public int getProjectid() {
    return projectid;
  }

  public String getLanguage() {
    return language;
  }

  public void setProjectid(int projectid) {
    this.projectid = projectid;
  }

  public boolean isHasModel() {
    return hasModel;
  }

  public Map<String, Set<MatchInfo>> getTypeToDistinct() {
    return typeToDistinct;
  }

  public Set<String> getRootNodes() {
    return rootNodes;
  }

  public Map<String, String> getParentToChild() {
    return parentToChild;
  }

  public String getLocale() {
    return locale;
  }

  public Language getLanguageInfo() {
    return languageInfo;
  }

  public ProjectType getProjectType() {
    return projectType;
  }

  public String toString() {
    Collection<SectionNode> sectionNodes = getSectionNodes();

    String sectionInfo = sectionNodes == null ? "missing section nodes???" : " num nodes " + sectionNodes.size();
    return "Project  " + projectid + " " + getProjectType()+
        " Order " + getTypeOrder() +
        sectionInfo +
        " has model " + hasModel;
  }
}
