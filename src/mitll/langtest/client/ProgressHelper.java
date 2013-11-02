package mitll.langtest.client;

import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.github.gwtbootstrap.client.ui.base.ProgressBarBase;
import mitll.langtest.client.exercise.ListInterface;

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
    return progressBar;
  }

  public void showAdvance(ListInterface exerciseList) {
    getProgressBar().setPercent(100 - exerciseList.getPercentComplete());
    getProgressBar().setText(exerciseList.getComplete() + " complete."); // TODO when is this +1 and when not??
  }

  public ProgressBar getProgressBar() {
    return progressBar;
  }
}
