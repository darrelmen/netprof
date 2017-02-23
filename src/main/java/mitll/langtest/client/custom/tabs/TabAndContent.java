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

package mitll.langtest.client.custom.tabs;

import com.github.gwtbootstrap.client.ui.Tab;
import com.github.gwtbootstrap.client.ui.TabPanel;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;

import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 4/14/2014.
 */
public class TabAndContent implements RequiresResize {
  private final Logger logger = Logger.getLogger("TabAndContent");

  private final Tab tab;
  private final DivWidget content;
  private final String label;
  private RequiresResize resizeable = null;

  public TabAndContent(TabPanel tabPanel, IconType iconType, String label) {
    this(iconType,label);
    tabPanel.add(getTab().asTabLink());
  }

  /**
   * @param iconType
   * @param label
   * @see mitll.langtest.client.custom.Navigation#makeTab(com.github.gwtbootstrap.client.ui.TabPanel, com.github.gwtbootstrap.client.ui.constants.IconType, String)
   */
  TabAndContent(IconType iconType, String label) {
    Tab tab = new Tab();
    tab.asWidget().getElement().setId("Tab_" + label);
    tab.setIcon(iconType);
    tab.setHeading(label);

    final DivWidget content = new DivWidget();
    content.getElement().setId("Content_" + label);
    content.addStyleName("positionRelative");
    tab.add(content);
    zeroPadding(content);

    this.tab = tab;
    this.content = content;
    this.label = label;
  }

  private void zeroPadding(Panel createContent) {
    Style style = createContent.getElement().getStyle();
    style.setPaddingLeft(0, Style.Unit.PX);
    style.setPaddingRight(0, Style.Unit.PX);
  }

  public Tab getTab() {
    return tab;
  }

  public DivWidget getContent() {   return content;  }

  public void clickOnTab() {
    getTab().fireEvent(new ButtonClickEvent());
  }

  @Override
  public void onResize() {
    if (resizeable != null) {
      logger.info("on resize");
      resizeable.onResize();
    }
  }

  public void setResizeable(RequiresResize resizeable) {
    this.resizeable = resizeable;
  }

  /*To call click() function for Programmatic equivalent of the user clicking the button.*/
  private class ButtonClickEvent extends ClickEvent {
  }

  public String toString() {
    return "TabAndContent " + label;
  }
}
