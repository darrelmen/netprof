package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.user.client.ui.Widget;

import java.util.HashSet;
import java.util.Set;

/**
* Created by GO22670 on 4/16/2014.
*/
class RememberTabAndContent extends TabAndContent {
  Set<Widget> widgets = new HashSet<Widget>();

  public RememberTabAndContent(IconType iconType, String label) {
    super(iconType, label);
  }

  public void addWidget(Widget widget) {
    widgets.add(widget);
  }

  /**
   * @see #getPanelForAudio
   * @param allPlayed
   */
  public void checkAllPlayed(Set<Widget> allPlayed) {
   // System.out.println("check " +allPlayed.size() + " against " + widgets.size());
    if (allPlayed.containsAll(widgets)) {
      tab.setIcon(IconType.CHECK_SIGN);
    }
  }
}
