package mitll.langtest.client.list;

import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;

/**
 * Created by go22670 on 3/28/17.
 */
public class WaitCursorHelper {
  private Timer waitTimer = null;
  private final SafeUri animated = UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "animated_progress28.gif");
  private final SafeUri white = UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "white_32x32.png");
  private static final int delayMillis = 700;

  /**
   * @seex #addTypeAhead
   * @seex #scheduleWaitTimer
   * @seex #showFinishedGettingExercises
   */
  private final com.github.gwtbootstrap.client.ui.Image waitCursor = new com.github.gwtbootstrap.client.ui.Image(white);

  public Widget getWaitCursor() {
    return waitCursor;
  }

  public void showFinished() {
    if (waitTimer != null) {
      waitTimer.cancel();
    }
    hideWaitCursor();
  }

  private void hideWaitCursor() {
    waitCursor.setUrl(white);
    hide();
  }

  public void scheduleWaitTimer() {
    if (waitTimer != null) {
      waitTimer.cancel();
    }
    waitTimer = new Timer() {
      @Override
      public void run() {
        waitCursor.setUrl(animated);
        show();
      }
    };
    waitTimer.schedule(delayMillis);
  }

  public void hide() {
    waitCursor.setVisible(false);
  }

  public void show() {
    waitCursor.setVisible(true);
  }
}
