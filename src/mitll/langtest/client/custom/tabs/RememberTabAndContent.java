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

package mitll.langtest.client.custom.tabs;

import com.github.gwtbootstrap.client.ui.TabPanel;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.user.client.ui.Widget;

import java.util.HashSet;
import java.util.Set;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 4/16/2014.
 */
public class RememberTabAndContent extends TabAndContent {
  private final Set<Widget> widgets = new HashSet<>();
  boolean isCheckable;

  /**
   * @param iconType
   * @param label
   * @param isCheckable
   * @see mitll.langtest.client.qc.QCNPFExercise#addTabsForUsers
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#getRememberTabAndContent(TabPanel, String, boolean, boolean)
   */
  public RememberTabAndContent(IconType iconType, String label, boolean isCheckable) {
    super(iconType, label);
    this.isCheckable = isCheckable;
  }

  public void addWidget(Widget widget) {
    widgets.add(widget);
  }

  public boolean isCheckable() {
    return isCheckable;
  }

  /**
   * @param allPlayed
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#getPanelForAudio
   * @see mitll.langtest.client.qc.QCNPFExercise#getPanelForAudio
   */
  public void checkAllPlayed(Set<Widget> allPlayed) {
    // System.out.println("check " +allPlayed.size() + " against " + widgets.size());
    if (allPlayed.containsAll(widgets)) {
      if (isCheckable)
        getTab().setIcon(IconType.CHECK_SIGN);
    }
  }
}
