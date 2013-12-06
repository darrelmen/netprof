package mitll.langtest.client.recorder;

import com.github.gwtbootstrap.client.ui.Image;
import com.google.gwt.safehtml.shared.UriUtils;
import mitll.langtest.client.LangTest;

/**
 * Created by GO22670 on 12/3/13.
 */
public abstract class SimpleRecordButton /*extends RecordButton*/ {
  private Image recordImage1 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-3.png"));
  private Image recordImage2 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-4.png"));
  // private boolean hasFocus = false;
  //private Image record1, record2;
 // MyCustomIconType record1;
//  MyCustomIconType record2;
  //private final boolean addKeyBinding;
  private boolean warnUserWhenNotSpace;

/*  public SimpleRecordButton(Widget recordButton, int delay, boolean warnNotASpace) {
    super(title, delay, null, warnNotASpace);
  }*/

  protected boolean showInitialRecordImage() {
    recordImage1.setVisible(true);
    return true;
  }

  protected void showFirstRecordImage() {
    recordImage1.setVisible(true);
    recordImage2.setVisible(false);
  }

  protected void showSecondRecordImage() {
    recordImage1.setVisible(false);
    recordImage2.setVisible(true);
  }

  protected void hideBothRecordImages() {
    recordImage1.setVisible(false);
    recordImage2.setVisible(false);
  }
}

