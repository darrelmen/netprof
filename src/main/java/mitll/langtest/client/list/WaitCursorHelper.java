package mitll.langtest.client.list;

import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;

/**
 * Created by go22670 on 3/28/17.
 */
public class WaitCursorHelper {
 // private Logger logger = Logger.getLogger("WaitCursorHelper");
  private Timer waitTimer = null;
  private static final SafeUri animated = UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "animated_progress28.gif");
  private static final SafeUri white = UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "white_32x32.png");
  private static final int delayMillis = 700;

  private final com.github.gwtbootstrap.client.ui.Image waitCursor = new com.github.gwtbootstrap.client.ui.Image(white);

  /**
   * @see TypeAhead#TypeAhead
   * @return
   */
  public Widget getWaitCursor() {
    return waitCursor;
  }

  public void showFinished() {
   // logger.info("showFinished --- " + this);
    cancelTimer();
    hideWaitCursor();
  }

  private void cancelTimer() {
    if (waitTimer != null) {
     // logger.info("cancelTimer --- " + this);
      waitTimer.cancel();
    }
    //else {
    //  logger.info("cancelTimer waitTimer is null " +this);
   // }
  }

  private void hideWaitCursor() {
    waitCursor.setUrl(white);
    hide();
  }

  public void scheduleWaitTimer() {
  //  WaitCursorHelper outer = this;
   // logger.info("scheduleWaitTimer --- " + outer);
    cancelTimer();

    waitTimer = new Timer() {
      @Override
      public void run() {
     //   logger.info("scheduleWaitTimer timer expired..." + outer);
        waitCursor.setUrl(animated);
        show();
      }
    };
    waitTimer.schedule(delayMillis);
  }

  private void hide() {
    waitCursor.setVisible(false);
  }
  private void show() {
    waitCursor.setVisible(true);
  }
}
