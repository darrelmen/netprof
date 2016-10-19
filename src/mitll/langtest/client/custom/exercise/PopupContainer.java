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

package mitll.langtest.client.custom.exercise;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.Tooltip;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.PopupHelper;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 9/8/14.
 */
class PopupContainer {
  private final PopupHelper popupHelper = new PopupHelper();

  /**
   * @param commentEntryText
   * @return
   */
  public DecoratedPopupPanel makePopupAndButton(TextBox commentEntryText, ClickHandler clickHandler) {
    final DecoratedPopupPanel commentPopup = new DecoratedPopupPanel();
    commentPopup.setAutoHideEnabled(true);

    Panel hp = new HorizontalPanel();
    hp.add(commentEntryText);
    hp.add(getOKButton(commentPopup, clickHandler));

    commentPopup.add(hp);
    return commentPopup;
  }

  /**
   * Clicking OK just dismisses the popup.
   * @param commentPopup
   * @return
   */
  Button getOKButton(final PopupPanel commentPopup, ClickHandler clickHandler) {
    Button ok = new Button("OK");
    ok.setType(ButtonType.PRIMARY);
    ok.addStyleName("leftTenMargin");
    ok.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        commentPopup.hide();
      }
    });
    if (clickHandler != null) {
      ok.addClickHandler(clickHandler);
    }
    return ok;
  }

  /**
   * TODO : somehow the textEntry box loses focus when it's presented inside of another modal???
   * Maybe that's a bad thing to do???
   *
   * @see mitll.langtest.client.custom.exercise.CommentBox#configureCommentButton(com.github.gwtbootstrap.client.ui.Button, boolean, com.google.gwt.user.client.ui.PopupPanel, String, com.github.gwtbootstrap.client.ui.TextBox)
   * @param popupButton
   * @param popup
   * @param textEntry
   * @param tooltip
   */
  void configurePopupButton(final Button popupButton, final PopupPanel popup,
                                      final TextBox textEntry, final Tooltip tooltip) {
    popupButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        boolean visible = popup.isShowing();

        if (visible) {// fix for bug that Wade found -- if we click off of popup, it dismisses it,
          // but if that click is on the button, it would immediately shows it again
          //System.out.println("popup visible " + visible);
          popup.hide();
        } else {
          popup.getElement().getStyle().setZIndex(1100);
          popup.showRelativeTo(popupButton);
//          textEntry.setFocus(true);

          Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            public void execute() {
              textEntry.setFocus(true);
            }
          });

          tooltip.hide();
        }
      }
    });
  }

  public void showPopup(String html, Widget target) {
    popupHelper.showPopup(html, target);
  }

  /**
   * For this field configure the textBox box to post annotation on blur and enter
   *
   * @param initialText fill in with existing annotation, if there is one
   * @param textBox comment box to configure
   * @return
   */
  void configureTextBox(String initialText,
                               final HidePopupTextBox textBox,
                               final PopupPanel popup) {
    if (initialText != null) {
      textBox.setText(initialText);
      if (textBox.getVisibleLength() < initialText.length()) {
        textBox.setVisibleLength(70);
      }
    }

    textBox.addStyleName("leftFiveMargin");
    textBox.configure(popup);

    //return textBox;
  }

  public static class HidePopupTextBox extends TextBox {
    public void configure(final PopupPanel popup) {
      addKeyPressHandler(new KeyPressHandler() {
        @Override
        public void onKeyPress(KeyPressEvent event) {
          int keyCode = event.getNativeEvent().getKeyCode();
          if (keyCode == KeyCodes.KEY_ENTER) {
            // System.out.println("HidePopupTextBox : got key press on " + getElement().getId());
            //popupAboutToBeHidden();
            popup.hide();
            onEnter();
          }
        }
      });
    }
    void onEnter() {
    }
  }
}
