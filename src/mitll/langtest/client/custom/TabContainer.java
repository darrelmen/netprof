package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.TabPanel;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;

/**
 * Created by go22670 on 2/10/14.
 */
public abstract class TabContainer implements RequiresResize {
  static final String LEARN_PRONUNCIATION = "Learn Pronunciation";
  static final String PRACTICE = "Practice";
  private Widget container;
 // private TabPanel tabPanel;
  //private List<TabAndContent> tabs = new ArrayList<TabAndContent>();

  /**
   * @see mitll.langtest.client.custom.Navigation#getTabPanel(com.google.gwt.user.client.ui.Panel)
   * @see mitll.langtest.client.custom.Navigation#getListOperations(mitll.langtest.shared.custom.UserList, String)
   * @param tabPanel
   * @param iconType
   * @param label
   * @return
   */
  TabAndContent makeTab(TabPanel tabPanel, IconType iconType, String label) {
    TabAndContent tabAndContent = new TabAndContent(iconType, label);
    tabPanel.add(tabAndContent.tab.asTabLink());
    return tabAndContent;
  }

  /**
   * @param secondAndThird
   * @return
   * @see mitll.langtest.client.LangTest#resetClassroomState()
   */
  public Widget getNav(final Panel secondAndThird) {
    Panel container = new FlowPanel();
    container.getElement().setId("getNav_container");
    Panel buttonRow = getTabPanel(secondAndThird);
    buttonRow.getElement().setId("getNav_buttonRow");

    container.add(buttonRow);
    this.container = container;
    return container;
  }

  public void showInitialState() {}
  public Widget getContainer() {
    return container;
  }
  protected abstract Panel getTabPanel(Panel secondAndThird);

}
