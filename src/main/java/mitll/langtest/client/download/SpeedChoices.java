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
  private boolean choiceMade = false;
  private boolean isRegular = true;
//  private KeyStorage keyStorage;

  public SpeedChoices(IShowStatus showStatus, boolean setInitially) {
    this.showStatus = showStatus;
//    this.keyStorage = keyStorage;
    this.setInitially = setInitially;

    if (setInitially) {
      choiceMade = true;
      isRegular  = true;
    }
  }

  public SpeedChoices() {
    this.showStatus = () -> {
    };
    setInitially = true;
    //  this.keyStorage = keyStorage;
  }

  public String getStatus() {
    return (isThereASpeedChoice() ? isRegular() ? REGULAR_SPEED : SLOW_SPEED : "");
  }

  public Widget getSpeedChoices() {
    Panel buttonToolbar = getToolbar2();
    buttonToolbar.setHeight("40px");

    buttonToolbar.add(regular = getChoice2(REGULAR, rabbit, rabbitSelected, event -> chooseReg()));
    buttonToolbar.add(slow = getChoice2(SLOW, turtle, turtleSelected, event -> clickSlow()));

    if (setInitially) showSpeeds();
    buttonToolbar.getElement().getStyle().setMarginBottom(25, Style.Unit.PX);
    return buttonToolbar;
  }

  public void chooseReg() {
    gotClickForSpeed(true);
  }

  private void clickSlow() {
    gotClickForSpeed(false);
  }

  private void gotClickForSpeed(boolean isReg) {
    rememberAudioChoice(isReg);
    showSpeeds();
  }


  private void showSpeeds() {
    setButtonState();
    showStatus.showStatus();
  }

  private void setButtonState() {
    boolean isRegular = isRegular();
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

   boolean isThereASpeedChoice() {
    return choiceMade;//!keyStorage.getValue(IS_REG_SPEED).isEmpty();
  }

  private void rememberAudioChoice(boolean isReg) {
    //keyStorage.setBoolean(IS_REG_SPEED, isReg);
    choiceMade = true;
    isRegular = isReg;
  }

  public boolean isRegular() {
    return isRegular;
/*    String value = keyStorage.getValue(IS_REG_SPEED);
    //   logger.info("isSpeedReg speed from storage " + value);
    return value.isEmpty() || value.equalsIgnoreCase("true");*/
  }

  public void setEnabled(boolean b) {
    regular.setEnabled(b);
    slow.setEnabled(b);
  }
}
