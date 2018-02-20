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
  //private final Logger logger = Logger.getLogger("SpeedChoices");

  private static final String IS_REG_SPEED = "isRegSpeed";
  private static final String REGULAR = "Regular";
  private static final String SLOW = "Slow";
  private static final String REGULAR_SPEED = "Regular Speed";
  private static final String SLOW_SPEED = "Slow Speed";

  private final Image turtle = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "turtle_32.png"));
  private final Image turtleSelected = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "turtle_32_selected.png"));

  private final Image rabbit = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "rabbit32.png"));
  private final Image rabbitSelected = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "rabbit32_selected.png"));

  private ToggleButton regular, slow;
  private IShowStatus showStatus;
  private boolean setInitially;

  private KeyStorage keyStorage;

  public SpeedChoices(KeyStorage keyStorage, IShowStatus showStatus, boolean setInitially) {
    this.showStatus = showStatus;
    this.keyStorage = keyStorage;
    this.setInitially = setInitially;
  }

  public SpeedChoices(KeyStorage keyStorage) {
    this.showStatus = () -> {
    };
    setInitially = true;
    this.keyStorage = keyStorage;
  }

  private void rememberAudioChoice(boolean isReg) {
    keyStorage.setBoolean(IS_REG_SPEED, isReg);
  }

  public String getStatus() {
    return (isThereASpeedChoice() ? isSpeedReg() ? REGULAR_SPEED : SLOW_SPEED : "");
  }

  public Widget getSpeedChoices() {
    Panel buttonToolbar = getToolbar2();
    buttonToolbar.setHeight("40px");
    // logger.info("getSpeedChoices got " + isThereASpeedChoice);
    {
      regular = getChoice2(REGULAR, rabbit, rabbitSelected, event -> {
        // isRegular = regular.isDown();
        rememberAudioChoice(true);
        showSpeeds();
      });
      buttonToolbar.add(regular);
    }

    {
      slow = getChoice2(SLOW, turtle, turtleSelected, event -> {
        // isRegular = !slow.isDown();
        //  logger.info("got slow click " + isRegular);
        rememberAudioChoice(false);
        showSpeeds();
      });

      buttonToolbar.add(slow);
    }

    if (setInitially) showSpeeds();
    buttonToolbar.getElement().getStyle().setMarginBottom(25, Style.Unit.PX);
    return buttonToolbar;
  }


  private void showSpeeds() {
    setButtonState();
    showStatus.showStatus();
  }

  private void setButtonState() {
    boolean isRegular = isSpeedReg();
    regular.setDown(isRegular);
    slow.setDown(!isRegular);
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

  public boolean isThereASpeedChoice() {
    return !keyStorage.getValue(IS_REG_SPEED).isEmpty();
  }

  public boolean isRegular() {
    return isSpeedReg();
  }

  private boolean isSpeedReg() {
    String value = keyStorage.getValue(IS_REG_SPEED);
 //   logger.info("isSpeedReg speed from storage " + value);
    return value.isEmpty() || value.equalsIgnoreCase("true");
  }
}
