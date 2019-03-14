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
import com.github.gwtbootstrap.client.ui.Modal;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.Widget;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ModalInfoDialog {
  // private final Logger logger = Logger.getLogger("ModalInfoDialog");
  private static final List<String> MESSAGES = Collections.emptyList();
  private final KeyPressHelper enterKeyButtonHelper = new KeyPressHelper();

  public ModalInfoDialog() {
  }

  public ModalInfoDialog(String title, String message) {
    this(title, message, null);
  }

  public ModalInfoDialog(String title, Widget widget, int height) {
    this(title, null, Collections.emptyList(), widget, null, false, true, 600, height);
  }

  public ModalInfoDialog(String title, Collection<String> messages) {
    this(title, messages, Collections.emptyList(),
        null, null, false, true, 600, 200);
  }

  public ModalInfoDialog(String title, Collection<String> messages, int height, boolean bigger) {
    this(title, messages, Collections.emptyList(),
        null, null, bigger, true, 600, height);
  }

  public ModalInfoDialog(String title, Widget widget, boolean addEnterKeyBinding, int width, int height) {
    this(title, MESSAGES, Collections.emptyList(), widget, null, false, addEnterKeyBinding, width, height);
  }

  public ModalInfoDialog(String title, String message, HiddenHandler handler) {
    this(title, Collections.singleton(message), Collections.emptyList(),
        null, handler, false, true, 600, 150);
  }

  public ModalInfoDialog(String title, Collection<String> messages, Collection<String> values, Widget widget,
                         HiddenHandler handler, boolean bigger, boolean addEnterKeyBinding, int width, int height) {
    Modal modal = getModal(title, messages, values, widget, handler, bigger, addEnterKeyBinding);
    modal.setWidth(width);
    modal.setHeight(height + "px");
    modal.setMaxHeigth(height + "px");

    modal.show();
  }

  public Modal getModal(String title,
                        Collection<String> messages,
                        Collection<String> values,
                        Widget widget,
                        HiddenHandler handler,
                        boolean bigger,
                        boolean addEnterKeyBinding) {
    final Modal modal = new Modal(true);
    modal.setTitle(title);

    if (!messages.isEmpty()) {
      addContent(messages, values, modal, bigger);
    }

    if (widget != null) modal.add(widget);

    final Button begin = getOKButton(modal, addEnterKeyBinding);
    begin.addStyleName("floatRight");
    modal.add(begin);

    if (handler != null) {
      modal.addHiddenHandler(handler);
    }

    modal.addHiddenHandler(hiddenEvent -> enterKeyButtonHelper.removeKeyHandler());
    return modal;
  }

  protected FlexTable addContent(Collection<String> messages, Collection<String> values, Modal modal, boolean bigger) {
    FlexTable flexTable = new FlexTable();

    int r = 0;

    HTMLTable.RowFormatter rf = flexTable.getRowFormatter();


    Iterator<String> iterator = values.iterator();
    for (String m : messages) {
      flexTable.setHTML(r, 0, m);

      if (iterator.hasNext()) {
        flexTable.setHTML(r, 1, "&nbsp;" + "<b>" + iterator.next() + "</b>");
      }
      if (bigger) {
        rf.addStyleName(r, "Instruction-data-with-wrap");//"Instruction-title");
      }
      r++;
    }
    modal.add(flexTable);
    return flexTable;
  }

  private Button getOKButton(final Modal modal, boolean addEnterKeyBinding) {
    final Button begin = new Button("OK");
    begin.setType(ButtonType.PRIMARY);
    begin.setEnabled(true);
    begin.setFocus(true);

    // Set focus on the widget. We have to use a deferred command or a
    // timer since GWT will lose it again if we set it in-line here
    Scheduler.get().scheduleDeferred((Command) () -> begin.setFocus(true));

    if (addEnterKeyBinding) {
      enterKeyButtonHelper.addKeyHandler(begin);
    }

    begin.addClickHandler(event -> modal.hide());
    return begin;
  }
}