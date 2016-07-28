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
import mitll.langtest.shared.SectionNode;

import java.util.Collection;
import java.util.Map;

public class ProjectStartupInfo implements IsSerializable {
  private Map<String, String> properties;
  private Collection<String> typeOrder;
  private Collection<SectionNode> sectionNodes;
  private int projectid;
  private String language;

  public ProjectStartupInfo() {
  } // for serialization

  /**
   * @param properties
   * @param typeOrder
   * @param sectionNodes
   * @param projectid
   * @see mitll.langtest.server.LangTestDatabaseImpl#getStartupInfo()
   */
  public ProjectStartupInfo(Map<String, String> properties, Collection<String> typeOrder,
                            Collection<SectionNode> sectionNodes, int projectid, String language) {
    this.properties = properties;
    this.typeOrder = typeOrder;
    this.sectionNodes = sectionNodes;
    this.projectid = projectid;
    this.language = language;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public Collection<String> getTypeOrder() {
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

  public String toString() {
    return "Project  " + projectid +
        " Order " + getTypeOrder() +
        " nodes " + getSectionNodes();
  }
}
