/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

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
        showAnimated();
      }
    };
    waitTimer.schedule(delayMillis);
  }

  public void showAnimated() {
    waitCursor.setUrl(animated);
    show();
  }

  private void hide() {
    waitCursor.setVisible(false);
  }
  private void show() {
    waitCursor.setVisible(true);
  }
}
