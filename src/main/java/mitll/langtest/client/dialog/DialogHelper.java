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
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.download.DownloadHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 */
public class DialogHelper {
  private final boolean doYesAndNo;

  public interface CloseListener {
    void gotYes();
    void gotNo();
  }

  public DialogHelper(boolean doYesAndNo) {
    this.doYesAndNo = doYesAndNo;
  }

/*
  public DialogHelper(String title, Collection<String> msgs, CloseListener listener) {
    this.doYesAndNo = true;
    show(title, msgs, null, "Yes", "No", listener);
  }
*/

  public void showErrorMessage(String title, String msg) {
    List<String> msgs = new ArrayList<String>();
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

  /**
   * Note : depends on bootstrap
   *
   * @param title
   * @param msgs
   * @param maxHeight
   * @see DownloadHelper#showDialog
   */
  public Button show(String title,
                     Collection<String> msgs,
                     Widget other,
                     String buttonName,
                     String cancelButtonName,
                     final CloseListener listener,
                     int maxHeight) {
    final Modal dialogBox = new Modal();
    dialogBox.setTitle("<b>" + title + "</b>");
    if (maxHeight > 0) dialogBox.setMaxHeigth(maxHeight + "px");
    Button closeButton;
    closeButton = new Button(buttonName);
    closeButton.setType(ButtonType.PRIMARY);
    closeButton.setFocus(true);

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

    if (other != null) container.add(other);

    // add buttons
    FluidRow row = new FluidRow();
    if (doYesAndNo) {
      row.add(new Column(4, closeButton));
      row.add(new Column(4, new Heading(4)));
      Button noButton = new Button(cancelButtonName);
      noButton.setType(ButtonType.INVERSE);

      row.add(new Column(4, noButton));
      noButton.addClickHandler(new ClickHandler() {
        public void onClick(ClickEvent event) {
          dialogBox.hide();
          if (listener != null) listener.gotNo();
        }
      });
    } else {
      row.add(new Column(2, 6, closeButton));
    }
    container.add(row);
    FluidRow w = new FluidRow();
    w.add(new Column(12, new Heading(6)));
    container.add(w);

    closeButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        dialogBox.hide();
        if (listener != null) listener.gotYes();
      }
    });

    dialogBox.add(container);
    dialogBox.show();

    return closeButton;
  }
}