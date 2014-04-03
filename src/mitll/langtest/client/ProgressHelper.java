package mitll.langtest.client;

import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.github.gwtbootstrap.client.ui.base.ProgressBarBase;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 8/6/13
 * Time: 12:09 PM
 * To change this template use File | Settings | File Templates.
 */
class ProgressHelper {
  private final ProgressBar progressBar;
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
    return progressBar;
  }

/*  public void showAdvance(ListInterface exerciseList) {
    int percentComplete = exerciseList.getPercentComplete();
    getProgressBar().setPercent(percentComplete == 0 ? 100 : percentComplete);
    getProgressBar().setText(percentComplete == 0 ? "No items completed." :exerciseList.getComplete() + " complete."); // TODO when is this +1 and when not??
  }*/

  public ProgressBar getProgressBar() {
    return progressBar;
  }
}
