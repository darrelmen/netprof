package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Tooltip;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.user.client.ui.Widget;

/**
 * Created by go22670 on 5/16/2014.
 */
public class TooltipHelper {
  public Tooltip addTooltip(Widget w, String tip) {
    return createAddTooltip(w, tip, Placement.RIGHT);
  }

  /**
   * @see mitll.langtest.client.custom.NPFExercise#makeAddToList(mitll.langtest.shared.CommonExercise, mitll.langtest.client.exercise.ExerciseController)
   * @param widget
   * @param tip
   * @param placement
   * @return
   */
  public Tooltip createAddTooltip(Widget widget, String tip, Placement placement) {
    Tooltip tooltip = new Tooltip();
    tooltip.setWidget(widget);
    tooltip.setText(tip);
    tooltip.setAnimation(true);
// As of 4/22 - bootstrap 2.2.1.0 -
// Tooltips have an bug which causes the cursor to
// toggle between finger and normal when show delay
// is configured.

    tooltip.setShowDelay(500);
    tooltip.setHideDelay(500);

    tooltip.setPlacement(placement);
    tooltip.reconfigure();
    return tooltip;
  }
}
