package mitll.langtest.client.custom.tabs;

import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.tabs.TabAndContent;

import java.util.HashSet;
import java.util.Set;

/**
* Created by GO22670 on 4/16/2014.
*/
public class RememberTabAndContent extends TabAndContent {
  private Set<Widget> widgets = new HashSet<Widget>();

  public RememberTabAndContent(IconType iconType, String label) {
    super(iconType, label);
  }

  public void addWidget(Widget widget) {
    widgets.add(widget);
  }

  /**
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#getPanelForAudio
   * @param allPlayed
   */
  public void checkAllPlayed(Set<Widget> allPlayed) {
   // System.out.println("check " +allPlayed.size() + " against " + widgets.size());
    if (allPlayed.containsAll(widgets)) {
      getTab().setIcon(IconType.CHECK_SIGN);
    }
  }
}
