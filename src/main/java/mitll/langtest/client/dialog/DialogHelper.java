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

package mitll.langtest.client.dialog;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.constants.BackdropType;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Widget;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 */
public class DialogHelper {
  //private final Logger logger = Logger.getLogger("DialogHelper");
  private final boolean doYesAndNo;
  private Modal dialogBox;

  private Button closeButton;

  public interface CloseListener {
    boolean gotYes();
    void gotNo();
    void gotHidden();
  }

  public DialogHelper(boolean doYesAndNo) {
    this.doYesAndNo = doYesAndNo;
  }

  public void showErrorMessage(String title, String msg) {
    List<String> msgs = new ArrayList<>();
    msgs.add(msg);
    showErrorMessage(title, msgs);
  }

  private void showErrorMessage(String title, Collection<String> msgs) {
    show(title, msgs, null, "Close", "No", null, -1);
  }
/*  public void show(String title, String msg, String buttonName, final CloseListener listener) {
    show(title, Collections.singletonList(msg), buttonName, listener);
  }*/

  public void show(String title, Collection<String> msgs, final CloseListener listener) {
    show(title, msgs, null, "Yes", "No", listener, -1);
  }

  public Button show(String title, Widget contents, CloseListener listener, int max, int width) {
    return show(title, Collections.emptyList(), contents, "OK", "Cancel", listener, max, width, false);
  }

  public Button show(String title,
                     Collection<String> msgs,
                     Widget other,
                     String buttonName,
                     String cancelButtonName,
                     final CloseListener listener,
                     int maxHeight) {
    return show(title, msgs, other, buttonName, cancelButtonName, listener, maxHeight, -1, false);
  }

  /**
   * Note : depends on bootstrap
   *
   * @param title
   * @param msgs
   * @param maxHeight
   * @param width
   * @see mitll.langtest.client.download.DownloadHelper#showDialog
   */
  public Button show(String title,
                     Collection<String> msgs,
                     Widget other,
                     String buttonName,
                     String cancelButtonName,
                     final CloseListener listener,
                     int maxHeight,
                     int width, boolean isBig) {
    return showDialog(title, msgs, other, cancelButtonName, listener, maxHeight, width, getCloseButton(buttonName), isBig);
  }

  @NotNull
  private Button showDialog(String title, Collection<String> msgs, Widget other, String cancelButtonName,
                            CloseListener listener, int maxHeight, int width, Button closeButton,
                            boolean isBig) {
    dialogBox = new Modal();
    if (width>900) {
      DOM.setStyleAttribute(dialogBox.getElement(), "left", 310 + "px");
    }

    this.closeButton = closeButton;
    if (isBig) {
      dialogBox.addStyleName("big-modal");
      dialogBox.addStyleName("domino-modal");
      dialogBox.setCloseVisible(false);
      dialogBox.setAnimation(true);
      dialogBox.getWidget(1).getElement().getStyle().setPaddingTop(5, Style.Unit.PX);

      dialogBox.setBackdrop(BackdropType.STATIC);
    }
    dialogBox.setTitle("<b>" + title + "</b>");
    if (maxHeight > 0) dialogBox.setMaxHeigth(maxHeight + "px");
    if (width > 0) dialogBox.setWidth(width + "px");

    FluidContainer container = getPrompt(msgs);

    if (other != null) container.add(other);

    // add buttons
    FluidRow row = new FluidRow();
    row.addStyleName("topFiveMargin");
    row.getElement().setId("buttonRow");
    if (doYesAndNo && cancelButtonName != null) {
      row.add(new Column(4, getCancel(cancelButtonName, listener, dialogBox)));
      row.add(new Column(6, new Heading(4)));
      row.add(new Column(2, closeButton));
    } else {
      row.add(new Column(2, 10, closeButton));
    }
    container.add(row);

    closeButton.addClickHandler(event -> {
      closeButton.setEnabled(false);

      boolean shouldHide = true;
      if (listener != null) shouldHide = listener.gotYes();

      closeButton.setEnabled(true);

      if (shouldHide) {
        dialogBox.hide();
      }
    });

    if (listener != null) {
      dialogBox.addHiddenHandler(hiddenEvent -> listener.gotHidden());
    }
    dialogBox.add(container);
    dialogBox.show();

    return closeButton;
  }

  @NotNull
  private FluidContainer getPrompt(Collection<String> msgs) {
    FluidContainer container = new FluidContainer();

    for (String msg : msgs) {
      FluidRow row = new FluidRow();
      Column column = new Column(12);
      row.add(column);
      Heading para = new Heading(4);
      para.setText(msg);

      column.add(para);
      container.add(row);
    }
    return container;
  }

  public void hide() {
    dialogBox.hide();
  }

  @NotNull
  public Button getCloseButton(String buttonName) {
    Button closeButton;
    closeButton = new Button(buttonName);
    closeButton.setType(ButtonType.PRIMARY);
    closeButton.setFocus(true);
    return closeButton;
  }

  public void setCloseEnabled(boolean enabled) {
    closeButton.setEnabled(enabled);
  }

  @NotNull
  private Button getCancel(String cancelButtonName, CloseListener listener, Modal dialogBox) {
    Button noButton = new Button(cancelButtonName);
    noButton.setType(ButtonType.INVERSE);

    noButton.addClickHandler(event -> {
      dialogBox.hide();
      if (listener != null) listener.gotNo();
    });
    return noButton;
  }
}