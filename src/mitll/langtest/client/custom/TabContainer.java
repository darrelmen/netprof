package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.Tab;
import com.github.gwtbootstrap.client.ui.TabPanel;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;

/**
 * Created by go22670 on 2/10/14.
 */
public abstract class TabContainer implements RequiresResize {
  public static final String LEARN_PRONUNCIATION = "Learn Pronunciation";
  protected static final String PRACTICE = "Practice";
  protected Widget container;
  protected TabPanel tabPanel;

  protected TabAndContent makeTab(TabPanel toAddTo, IconType iconType, String label) {
    Tab create = new Tab();
    create.setIcon(iconType);
    create.setHeading(label);
    toAddTo.add(create.asTabLink());
    final FluidContainer createContent = new FluidContainer();
    create.add(createContent);
    zeroPadding(createContent);
    return new TabAndContent(create, createContent);
  }

  void zeroPadding(Panel createContent) {
    DOM.setStyleAttribute(createContent.getElement(), "paddingLeft", "0px");
    DOM.setStyleAttribute(createContent.getElement(), "paddingRight", "0px");
  }

  /**
   * @return
   * @param secondAndThird
   * @see mitll.langtest.client.LangTest#onModuleLoad2
   */
  public Widget getNav(final Panel secondAndThird) {
    Panel container = new FlowPanel();
    container.getElement().setId("getNav_container");
    Panel buttonRow = getButtonRow2(secondAndThird);
    buttonRow.getElement().setId("getNav_buttonRow");

    container.add(buttonRow);
    this.container = container;
    return container;
  }

  public void showInitialState() {
  }

    public Widget getContainer() { return container; }

  protected abstract Panel getButtonRow2(Panel secondAndThird);

  public static class TabAndContent {
    public Tab tab;
    public FluidContainer content;

    public TabAndContent(Tab tab, FluidContainer panel) {
      this.tab = tab;
      this.content = panel;
    }
  }
}
