package mitll.langtest.client.download;

import com.github.gwtbootstrap.client.ui.Image;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.custom.KeyStorage;

import java.util.logging.Logger;

public class SpeedChoices {
//  private final Logger logger = Logger.getLogger("SpeedChoices");

  private static final String IS_REG_SPEED = "isRegSpeed";
  private static final String REGULAR = "Regular";
  private static final String SLOW = "Slow";

  private final Image turtle = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "turtle_32.png"));
  private final Image turtleSelected = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "turtle_32_selected.png"));

  private final Image rabbit = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "rabbit32.png"));
  private final Image rabbitSelected = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "rabbit32_selected.png"));

  private boolean isRegular = true;

  private boolean isRegularSet = false;
  private ToggleButton regular, slow;
  private IShowStatus showStatus;
//  private KeyStorage keyStorage;
  private boolean setInitially = false;

  SpeedChoices(KeyStorage keyStorage, IShowStatus showStatus) {
    this.showStatus = showStatus;
  //  this.keyStorage = keyStorage;
  }

  public SpeedChoices(KeyStorage keyStorage) {
    this.showStatus = () -> {
    };
    setInitially = true;
    isRegularSet = true;
    isRegular = false;//isSpeedReg();
    //this.keyStorage = keyStorage;
  }

/*  private boolean isSpeedReg() {
    return keyStorage.isTrue(IS_REG_SPEED);
  }

  private void rememberAudioChoice(boolean isReg) {
    keyStorage.setBoolean(IS_REG_SPEED, isReg);
  }*/

  private void showSpeeds() {
    regular.setDown(isRegular);
    slow.setDown(!isRegular);
    showStatus.showStatus();
  }

  public String getStatus() {
    return (isRegularSet ? isRegular ? "Regular Speed" : "Slow Speed" : "");
  }

  public Widget getSpeedChoices() {
    Panel buttonToolbar = getToolbar2();
    buttonToolbar.setHeight("40px");

    // logger.info("getSpeedChoices got " + isRegularSet);
    {
      regular = getChoice2(REGULAR, rabbit, rabbitSelected, event -> {
        isRegular = regular.isDown();
        isRegularSet = true;
        showSpeeds();
        //rememberAudioChoice(true);
      });
      buttonToolbar.add(regular);
    }

    {
      slow = getChoice2(SLOW, turtle, turtleSelected, event -> {
        isRegular = !slow.isDown();
        //  logger.info("got slow click " + isRegular);
        isRegularSet = true;
        showSpeeds();
        //rememberAudioChoice(false);
      });

      buttonToolbar.add(slow);
    }

    if (setInitially) showSpeeds();
    buttonToolbar.getElement().getStyle().setMarginBottom(25, Style.Unit.PX);
    return buttonToolbar;
  }

  private Panel getToolbar2() {
    return new HorizontalPanel();
  }

  private ToggleButton getChoice2(String title, Image upImage, Image downImage, ClickHandler handler) {
    ToggleButton onButton = new ToggleButton(upImage, downImage);
    onButton.getElement().setId("Choice_" + title);
    onButton.addClickHandler(handler);
    onButton.getElement().getStyle().setZIndex(0);
    onButton.setWidth(50 + "px");
    onButton.setHeight(32 + "px");
    return onButton;
  }

  public boolean isRegularSet() {
    return isRegularSet;
  }

  public boolean isRegular() {
    return isRegular;
  }
}
