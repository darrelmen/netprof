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

package mitll.langtest.client.custom.dialog;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.analysis.ButtonMemoryItemContainer;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.shared.custom.IPublicPrivate;
import mitll.langtest.shared.exercise.HasID;
import org.jetbrains.annotations.NotNull;

public abstract class ButtonHelper<T extends HasID & IPublicPrivate> {
  private static final int MIN_WIDTH = 668;//659;
  private static final String SHARE = "Share";
  private static final String SHARE_THE_LIST = "Share the ";// + LIST + " with someone.";

  public Button getShare(ButtonMemoryItemContainer<T> container) {
    Button successButton = getSuccessButton(container, SHARE);
    successButton.setIcon(IconType.SHARE);
    addTooltip(successButton, SHARE_THE_LIST + getSuffix());
    return successButton;
  }

  @NotNull
  public Button getSuccessButton(ButtonMemoryItemContainer<T> container, String learn1) {
    Button learn = getSuccess(learn1);
    container.addButton(learn);
    return learn;
  }

  @NotNull
  public Button getSuccess(String learn1) {
    Button learn = new Button(learn1);
    learn.setType(ButtonType.SUCCESS);
    learn.addStyleName("leftFiveMargin");
    return learn;
  }

  @NotNull
  protected String getSuffix() {
    return " " + getName();
  }

  protected abstract String getName();

  public void addTooltip(Widget add, String tip) {
    new TooltipHelper().addTopTooltip(add, tip);
  }

  @NotNull
  public DivWidget getCommonButtonContainer() {
    DivWidget buttons = getButtonContainer();
    buttons.getElement().getStyle().setProperty("minWidth", MIN_WIDTH + "px");
    return buttons;
  }

  @NotNull
  public DivWidget getButtonContainer() {
    DivWidget buttons = new DivWidget();
    buttons.addStyleName("inlineFlex");
    buttons.addStyleName("topFiveMargin");
    buttons.getElement().getStyle().setClear(Style.Clear.BOTH);
    return buttons;
  }

}
