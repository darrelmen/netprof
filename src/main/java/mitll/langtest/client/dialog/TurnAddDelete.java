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

package mitll.langtest.client.dialog;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.UIObject;
import org.jetbrains.annotations.NotNull;

class TurnAddDelete {
  private AddDeleteListener listener;

  TurnAddDelete(AddDeleteListener listener) {
    this.listener = listener;
  }

  Button addAddTurnButton() {
    Button w = getTripleButton();

    w.addClickHandler(event -> listener.gotPlus());
    w.setIcon(IconType.PLUS);
    w.setType(ButtonType.SUCCESS);

    tripleButtonStyle(w);

    return w;
  }

  Button addDeleteButton(boolean isFirstTurn) {
    Button w = getTripleButton();

    w.addClickHandler(event -> listener.gotMinus());
    w.setType(ButtonType.WARNING);
    w.setIcon(IconType.MINUS);

    // can't blow away the first turn!
    w.setEnabled(!isFirstTurn);

    tripleFirstStyle(w);

    w.addFocusHandler(event -> listener.deleteGotFocus());
    //  w.addBlurHandler(event -> listener.deleteGotBlur());
    return w;
  }

  @NotNull
  Button getTripleButton() {
    return new Button() {
      @Override
      protected void onAttach() {
        int tabIndex = getTabIndex();
        super.onAttach();

        if (-1 == tabIndex) {
          setTabIndex(-1);
        }
      }
    };
  }

  void tripleButtonStyle(Button w) {
    tripleFirstStyle(w);
    //   w.addFocusHandler(event -> turnContainer.moveFocusToNext());
    removeFromTabSequence(w);
//    logger.info("aftr " + getExID() + " " + w.getTabIndex());
  }

  private void tripleFirstStyle(Button w) {
    addPressAndHoldStyle(w);

    w.addStyleName("topFiveMargin");
    w.addStyleName("leftFiveMargin");
  }

  void removeFromTabSequence(Focusable postAudioRecordButton) {
    postAudioRecordButton.setTabIndex(-1);
  }

  private void addPressAndHoldStyle(UIObject postAudioRecordButton) {
    Style style = postAudioRecordButton.getElement().getStyle();
    style.setProperty("borderRadius", 21 + "px");
    style.setPadding(9, Style.Unit.PX);
    style.setWidth(26, Style.Unit.PX);
    style.setMarginRight(5, Style.Unit.PX);
    style.setHeight(20, Style.Unit.PX);
  }
}
