package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Image;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 10/29/13
 * Time: 1:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class PopupHelper {
    private static final int POPUP_DURATION = 2500;

    public void showPopup(String html) {
      showPopup(html, null, null, POPUP_DURATION, null);
    }

/*  public void showPopup(String html, CloseHandler<PopupPanel> closeHandler) {
    showPopup(html, null, null, POPUP_DURATION, closeHandler);
  }*/

    public void showPopup(String html, Image image) {
      showPopup(html, image, null, POPUP_DURATION, null);
    }

    public void showPopup(String html, Image image, int dur) {
      showPopup(html, image, null, dur, null);
    }

    public void showPopup(String html, Image image, Image image2) {
      showPopup(html, image, image2, POPUP_DURATION, null);
    }

    public void showPopup(String html, Image image, Image image2, CloseHandler<PopupPanel> closeHandler) {
      showPopup(html, image, image2, POPUP_DURATION, closeHandler);
    }

    public void showPopup(String html, Image image, Image image2, int dur, CloseHandler<PopupPanel> closeHandler) {
      final PopupPanel pleaseWait = new DecoratedPopupPanel();
      pleaseWait.setAutoHideEnabled(true);
      HTML w = new HTML(html);
      if (image != null) {
        VerticalPanel vp = new VerticalPanel();
        if (image2 != null) {
          HorizontalPanel hp = new HorizontalPanel();
          hp.add(image);
          hp.setSpacing(5);
          hp.add(image2);
          vp.add(hp);
        } else {
          vp.add(image);
        }
        vp.add(w);

        pleaseWait.add(vp);
      } else {
        pleaseWait.add(w);
      }
      pleaseWait.center();
      if (closeHandler != null) {
        pleaseWait.addCloseHandler(closeHandler);
      }

      Timer t = new Timer() {
        @Override
        public void run() {
          pleaseWait.hide();
        }
      };
      t.schedule(dur);
    }

}
