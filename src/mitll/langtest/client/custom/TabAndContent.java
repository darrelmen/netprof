package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Tab;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Panel;

/**
* Created by GO22670 on 4/14/2014.
*/
public class TabAndContent {
  public final Tab tab;
  public final DivWidget content;

  /**
   * @param tab
   * @param panel
   * @see TabContainer#makeTab(com.github.gwtbootstrap.client.ui.TabPanel, com.github.gwtbootstrap.client.ui.constants.IconType, String)
   */
/*
  public TabAndContent(Tab tab, DivWidget panel) {
    this.tab = tab;
    this.content = panel;
  }
*/

  /**
   * @param iconType
   * @param label
   * @see TabContainer#makeTab(com.github.gwtbootstrap.client.ui.TabPanel, com.github.gwtbootstrap.client.ui.constants.IconType, String)
   */
  public TabAndContent(/*TabPanel toAddTo,*/ IconType iconType, String label) {
    Tab tab = new Tab();
    tab.asWidget().getElement().setId("Tab_" + label);
    tab.setIcon(iconType);
    tab.setHeading(label);
 //   toAddTo.add(tab.asTabLink());

    final DivWidget content = new DivWidget();
    content.getElement().setId("Content_" + label);
    content.addStyleName("positionRelative");
    tab.add(content);
    zeroPadding(content);

    this.tab = tab;
    this.content = content;
  }

  void zeroPadding(Panel createContent) {
    Style style = createContent.getElement().getStyle();
    style.setPaddingLeft(0, Style.Unit.PX);
    style.setPaddingRight(0, Style.Unit.PX);
 //   DOM.setStyleAttribute(createContent.getElement(), "paddingLeft", "0px");
 //   DOM.setStyleAttribute(createContent.getElement(), "paddingRight", "0px");
  }
}
