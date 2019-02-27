/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.initial;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 */
public class PopupHelper {
  private final Logger logger = Logger.getLogger("PopupHelper");

  private static final int HIDE_DELAY = 2500;

/*
  public void showPopup(String html) {
    final PopupPanel pleaseWait = new DecoratedPopupPanel();
    pleaseWait.setAutoHideEnabled(true);
    pleaseWait.add(new HTML(html));
    pleaseWait.center();

    Timer t = new Timer() {
      @Override
      public void run() {
        pleaseWait.hide();
      }
    };
    t.schedule(HIDE_DELAY);
  }
*/

  public void showPopup(String html, Widget target) {
    showPopup(html, target, 2000);
  }

  public PopupPanel showPopup(String html, Widget button, int hideDelay) {
    final PopupPanel pleaseWait = new DecoratedPopupPanel();
    pleaseWait.setAutoHideEnabled(true);
    pleaseWait.add(new HTML(html));
    pleaseWait.showRelativeTo(button);

    Timer t = new Timer() {
      @Override
      public void run() {
        pleaseWait.hide();
      }
    };
    t.schedule(hideDelay);
    return pleaseWait;
  }

/*  public void showPopup(String toShow, String toShow2, Widget over) {
    final PopupPanel popupImage = new PopupPanel(true);
    Panel vp = new VerticalPanel();
    vp.add(new HTML(toShow));
    vp.add(new HTML(toShow2));
    popupImage.add(vp);
    if (over.getParent() == null) {
      logger.warning("no parent for " + over);
    }
    popupImage.showRelativeTo(over);
    Timer t = new Timer() {
      @Override
      public void run() {
        popupImage.hide();
      }
    };
    t.schedule(3000);
  }*/
}