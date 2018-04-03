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
import mitll.langtest.client.project.ProjectEditForm;

public enum ProjectStatus implements IsSerializable {
  /**
   * Everyone can see projects in production - students, teachers, everyone
   */
  PRODUCTION,
  /**
   * Only admins and content developers can see projects in development.
   */
  DEVELOPMENT,
  /**
   * The final stage before Production - when we have a model and we're testing it.
   * Only visible to admins, content developers.
   */
  EVALUATION,
  /**
   * Projects that have become obsolete but we might want to examine or report on.
   * Only admins can see them.
   */
  RETIRED(true, true, false, true),
  /**
   * Projects that just exist to demonstrate a new feature - they should not be reported on.
   */
  DEMO(true, true, true, false),
  /**
   * Completed removed projects that can't be reloaded.
   */
  DELETED(false, false, false, false);

  private boolean show = true; // as a choice
  private boolean showOnlyToAdmins = false; // in the project display
  private boolean load = true; // should load the project
  private boolean reportOn = true; // should include in reports on usage

  ProjectStatus() {
  }

  ProjectStatus(boolean show, boolean showOnlyToAdmins, boolean load, boolean reportOn) {
    this.show = show;
    this.showOnlyToAdmins = showOnlyToAdmins;
    this.load = load;
    this.reportOn=reportOn;
  }

  /**
   * @see ProjectEditForm#getBox
   * @return
   */
  public boolean shouldShow() {
    return show;
  }

  public boolean shouldLoad() {
    return load;
  }

  /**
   * @see mitll.langtest.client.project.ProjectChoices#getVisibleProjects
   * @return
   */
  public boolean shouldShowOnlyToAdmins() {
    return showOnlyToAdmins;
  }

  public boolean shouldReportOn() {
    return reportOn;
  }
}
