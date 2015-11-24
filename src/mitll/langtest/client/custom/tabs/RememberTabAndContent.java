/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.custom.tabs;

import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.user.client.ui.Widget;

import java.util.HashSet;
import java.util.Set;

/**
* Created by GO22670 on 4/16/2014.
*/
public class RememberTabAndContent extends TabAndContent {
  private final Set<Widget> widgets = new HashSet<Widget>();

  /**
   * @see mitll.langtest.client.qc.QCNPFExercise#addTabsForUsers
   * @param iconType
   * @param label
   */
  public RememberTabAndContent(IconType iconType, String label) {
    super(iconType, label);
  }

  public void addWidget(Widget widget) {
    widgets.add(widget);
  }

  /**
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#getPanelForAudio
   * @see mitll.langtest.client.qc.QCNPFExercise#getPanelForAudio
   * @param allPlayed
   */
  public void checkAllPlayed(Set<Widget> allPlayed) {
   // System.out.println("check " +allPlayed.size() + " against " + widgets.size());
    if (allPlayed.containsAll(widgets)) {
      getTab().setIcon(IconType.CHECK_SIGN);
    }
  }
}
