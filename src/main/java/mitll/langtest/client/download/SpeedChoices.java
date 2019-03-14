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

 // private static final String IS_REG_SPEED = "isRegSpeed";
  private static final String REGULAR = "Regular";
  private static final String SLOW = "Slow";
  private static final String REGULAR_SPEED = "Regular Speed";
  private static final String SLOW_SPEED = "Slow Speed";
  public static final int WH = 32;
  public static final String WH_PX = WH + "px";

  private final Image turtle = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "turtle_32.png"));
  private final Image turtleSelected = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "turtle_32_selected.png"));

  private final Image rabbit = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "rabbit32.png"));
  private final Image rabbitSelected = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "rabbit32_selected.png"));

  private ToggleButton regular, slow;
  private final IShowStatus showStatus;
  private final boolean setInitially;
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

  void chooseReg() {
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
    onButton.getElement().getStyle().setPadding(0,Style.Unit.PX);
    onButton.getElement().setId("Choice_" + title);
    onButton.addClickHandler(handler);
    onButton.getElement().getStyle().setZIndex(0);
  //  int i = 50;
    onButton.setWidth(WH_PX);
    onButton.setHeight(WH_PX);
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
