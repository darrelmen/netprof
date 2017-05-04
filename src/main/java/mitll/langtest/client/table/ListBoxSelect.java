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

package mitll.langtest.client.table;

import com.github.gwtbootstrap.client.ui.ListBox;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import mitll.langtest.client.list.ListSectionWidget;
import mitll.langtest.client.list.SimpleSelectExerciseList;

import java.util.List;

/**
 * @Deprecated
 * @see ListSectionWidget#addChoices
 * @deprecated
 */
public class ListBoxSelect {
  //  private final Logger logger = Logger.getLogger("TableSelect");
  public static final String ALL = "All";
  //  public static final String FONT_TO_USE = "bold 24px Arial";
  private SimpleSelectExerciseList singleSelectExerciseList;
  private ListSectionWidget menuSectionWidget;

  static {
    new ListBoxSelect().register();
  }

  /**
   * @param values
   * @param width
   * @param singleSelectExerciseList
   * @param menuSectionWidget
   * @param initialSelection
   * @return
   */
  public ListBox makeSymbolButton(List<String> values,
                                  int width,
                                  SimpleSelectExerciseList singleSelectExerciseList,
                                  ListSectionWidget menuSectionWidget,
                                  String initialSelection) {
    List<String> values1 = values;
    values1.add(0, ALL);
    this.singleSelectExerciseList = singleSelectExerciseList;
    this.menuSectionWidget = menuSectionWidget;

    box = new ListBox();
    box.setHeight("35px");
    box.getElement().getStyle().setProperty("fontSize", "large");

    box.addStyleName("topMargin");

    int widthSoFar = 150;

    for (String value : values) {
      box.addItem(value);
    }

    box.setWidth(widthSoFar + "px");
    box.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        String selectedValue = box.getSelectedValue();
        reallyGotSelection(selectedValue);
      }
    });
    box.setSelectedValue(initialSelection);
    return box;
  }


  /**
   * @return
   * @see
   */
  public native int getWidth(String text, String font) /*-{
      return $wnd.jQuery.fn.textWidth(text, font)
  }-*/;

  public native void register() /*-{
      $wnd.jQuery.fn.textWidth = function (text, font) {
          if (!$wnd.jQuery.fn.textWidth.fakeEl) {
              $wnd.jQuery.fn.textWidth.fakeEl = $wnd.jQuery('<span>').hide().appendTo(document.body);
          }
          $wnd.jQuery.fn.textWidth.fakeEl.text(text || this.val() || this.text()).css('font', font || this.css('font'));
          return $wnd.jQuery.fn.textWidth.fakeEl.width();
      };
  }-*/;

  private ListBox box;

  private void reallyGotSelection(String s) {
    box.setSelectedValue(s);
    menuSectionWidget.gotSelection(s);
    singleSelectExerciseList.gotSelection();
  }
}

