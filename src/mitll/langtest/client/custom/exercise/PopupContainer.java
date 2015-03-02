package mitll.langtest.client.custom.exercise;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.Tooltip;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.PopupHelper;

/**
 * Created by go22670 on 9/8/14.
 */
public class PopupContainer {
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
  protected FocusWidget configureTextBox(String initialText,
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

    return textBox;
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
    protected void onEnter() {
    }
  }
}
