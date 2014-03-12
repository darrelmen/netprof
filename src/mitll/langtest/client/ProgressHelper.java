package mitll.langtest.client;

import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.github.gwtbootstrap.client.ui.Tooltip;
import com.github.gwtbootstrap.client.ui.base.ProgressBarBase;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.list.ListInterface;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 8/6/13
 * Time: 12:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class ProgressHelper {
  private ProgressBar progressBar;
  public ProgressHelper() {
    progressBar = makeProgressBar();
  }

  private ProgressBar makeProgressBar() {
    ProgressBar progressBar = new ProgressBar(ProgressBarBase.Style.DEFAULT);
    progressBar.setWidth("70%");
    progressBar.setPercent(100);
    progressBar.setText("No items completed.");
    progressBar.setColor(ProgressBarBase.Color.DEFAULT);
    progressBar.addStyleName("leftFifteenPercentMargin");
    progressBar.addStyleName("topMargin");
    addTooltip(progressBar, "The number of items you have completed.");
    return progressBar;
  }

  public void showAdvance(ListInterface exerciseList) {
    int percentComplete = exerciseList.getPercentComplete();
    getProgressBar().setPercent(percentComplete == 0 ? 100 : percentComplete);
    getProgressBar().setText(percentComplete == 0 ? "No items completed." :exerciseList.getComplete() + " complete."); // TODO when is this +1 and when not??
  }

  public ProgressBar getProgressBar() {
    return progressBar;
  }

  protected Tooltip addTooltip(Widget w, String tip) {
    return createAddTooltip(w, tip, Placement.LEFT);
  }

  /**
    * @param widget
   * @param tip
   * @param placement
   * @return
   */
  private Tooltip createAddTooltip(Widget widget, String tip, Placement placement) {
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
