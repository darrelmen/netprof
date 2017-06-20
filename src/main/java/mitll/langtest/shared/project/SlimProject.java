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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

public class SlimProject extends ProjectInfo {
  private boolean hasModel;
  private boolean isRTL;
  private List<SlimProject> children = new ArrayList<>();
  private TreeMap<String, String> props;

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
   * @param created
   * @param port
   * @param modelsDir
   * @see mitll.langtest.server.database.project.ProjectManagement#getProjectInfo
   */
  public SlimProject(int projectid,
                     String name,
                     String language,
                     String course,
                     String countryCode,
                     ProjectStatus status,
                     int displayOrder,

                     boolean hasModel,
                     boolean isRTL, long created,
                     String host,
                     int port,
                     String modelsDir,
                     String firstType,
                     String secondType,
                     boolean showOniOS,
                     TreeMap<String, String> props) {
    super(projectid, name, language, course, countryCode, status, displayOrder, created, host, port, modelsDir,
        firstType, secondType, showOniOS);
    this.hasModel = hasModel;
    this.isRTL = isRTL;
    this.props = props;
  }

  public void addChild(SlimProject projectInfo) {
    children.add(projectInfo);
  }

  public boolean hasChildren() {
    return !children.isEmpty();
  }

  public boolean hasChild(int projectid) {
    for (SlimProject child : children) {
      if (child.getID() == projectid) return true;
    }
    return false;
  }

  public SlimProject getChild(int projectid) {
    for (SlimProject child : children) {
      if (child.getID() == projectid) return child;
    }
    return null;
  }

  public List<SlimProject> getChildren() {
    Collections.sort(children, (o1, o2) -> {
      int i = Integer.valueOf(o1.getDisplayOrder()).compareTo(o2.getDisplayOrder());
      return i == 0 ? o1.getName().compareTo(o2.getName()) : i;
    });
    return children;
  }

  public boolean isHasModel() {
    return hasModel;
  }

  public boolean isRTL() {
    return isRTL;
  }

  public String toString() {
    return "Project #" + getID() + " " + getName() + " " + getLanguage() + " " + getStatus() +
        " num children " + children.size() + " " + getFirstType() + " " + getSecondType();
  }

  public TreeMap<String, String> getProps() {
    return props;
  }
}
