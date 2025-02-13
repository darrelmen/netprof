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

package mitll.langtest.client.list;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.TextBox;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;

import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 9/25/14.
 */
public class TypeAhead {
  private Logger logger = Logger.getLogger("TypeAhead");
  private final SafeUri white = UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "white_32x32.png");
  private final TextBox typeAhead = new TextBox();
  private final boolean isPashto;

  /**
   * @param column
   * @param waitCursor
   * @param title
   * @param hasFirstFocus
   * @param language
   * @see mitll.langtest.client.list.PagingExerciseList#addTypeAhead(com.google.gwt.user.client.ui.Panel)
   */
  TypeAhead(Panel column, Image waitCursor, String title, boolean hasFirstFocus, String language) {
    this.isPashto = language.equalsIgnoreCase("Pashto");
    makeTypeAhead();
    column.add(title.equals(PagingExerciseList.SEARCH) ? getSearch(waitCursor) : getControlGroup(waitCursor, title));
    checkFocus(hasFirstFocus);
  }

  private void checkFocus(boolean hasFirstFocus) {
    if (hasFirstFocus) {
      Scheduler.get().scheduleDeferred(new Command() {
        public void execute() {
          getTypeAhead().setFocus(true);
        }
      });
    }
  }

  public String getText() {
    return typeAhead.getText();
  }

  public Widget getWidget() {
    return typeAhead;
  }

  /**
   * On key up, do something, like go get a new list given a search term.
   */
  private void makeTypeAhead() {
    typeAhead.setWidth(240 +       "px");
    getTypeAhead().getElement().setId("ExerciseList_TypeAhead");

    if (isPashto) {
      typeAhead.addStyleName("pashtofont");
      typeAhead.getElement().getStyle().setProperty("fontSize", "xx-large");
      typeAhead.setHeight("32px");
    } else {
      typeAhead.getElement().getStyle().setFontSize(14, Style.Unit.PT);
    }

    getTypeAhead().setDirectionEstimator(true);   // automatically detect whether text is RTL
    getTypeAhead().addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        //  logger.info("got key up " + event);
        String current = getTypeAhead().getText();
        if (current.equals(previous)) {

        }
        else {
          gotTypeAheadEntry(current);
        }
        previous = current;
      }
    });
  }

  String previous = "";
  /**
   * Subclass please.
   *
   * @param text
   */
  public void gotTypeAheadEntry(String text) {
  }

  private Widget getControlGroup(Image waitCursor, String title) {
    Panel flow = new HorizontalPanel();
    flow.add(getTypeAhead());
    flow.add(waitCursor);
    configureWaitCursor(waitCursor);

    return getControlGroup(title, flow);
  }

  private Widget getSearch(Image waitCursor) {
    Panel flow = new HorizontalPanel();
    Icon child = new Icon(IconType.SEARCH);

    Style style = child.getElement().getStyle();
    style.setMarginRight(5, Style.Unit.PX);
    style.setColor("gray");
    flow.add(child);
    flow.add(getTypeAhead());
    flow.add(waitCursor);
    return flow;
  }

  private void configureWaitCursor(Image waitCursor) {
    waitCursor.getElement().getStyle().setMarginTop(-7, Style.Unit.PX);
    waitCursor.setUrl(white);
  }

  /**
   * @param label
   * @param user
   * @return
   * @see mitll.langtest.client.result.ResultManager#populateTable
   */
  public static ControlGroup getControlGroup(String label, Widget user) {
    final ControlGroup userGroup = new ControlGroup();
    userGroup.addStyleName("leftFiveMargin");

    Controls controls = new Controls();
    userGroup.add(new ControlLabel(label));
    controls.add(user);
    userGroup.add(controls);
    return userGroup;
  }

  /**
   * @return
   * @see PagingExerciseList#addTypeAhead(Panel)
   */
  TextBox getTypeAhead() {
    return typeAhead;
  }

  public void setText(String text) {
    typeAhead.setText(text);
  }
}
