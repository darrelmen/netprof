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
  private final Tab tab;
  private final DivWidget content;

  /**
   * @param iconType
   * @param label
   * @see mitll.langtest.client.custom.Navigation#makeTab(com.github.gwtbootstrap.client.ui.TabPanel, com.github.gwtbootstrap.client.ui.constants.IconType, String)
   */
  public TabAndContent(IconType iconType, String label) {
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
  }

  private void zeroPadding(Panel createContent) {
    Style style = createContent.getElement().getStyle();
    style.setPaddingLeft(0, Style.Unit.PX);
    style.setPaddingRight(0, Style.Unit.PX);
  }

  public Tab getTab() {
    return tab;
  }

  public DivWidget getContent() {
    return content;
  }

  public String toString() { return "Tab_Content label="+tab.getHeading(); }
}
