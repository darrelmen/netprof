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
  TabPanel tabPanel;

  /**
   * @see mitll.langtest.client.custom.Navigation#getTabPanel(com.google.gwt.user.client.ui.Panel)
   * @see mitll.langtest.client.custom.Navigation#getListOperations(mitll.langtest.shared.custom.UserList, String)
   * @param toAddTo
   * @param iconType
   * @param label
   * @return
   */
  TabAndContent makeTab(TabPanel toAddTo, IconType iconType, String label) {
    TabAndContent tabAndContent = new TabAndContent(iconType, label);
    toAddTo.add(tabAndContent.tab.asTabLink());
    return tabAndContent;
/*    Tab create = new Tab();
    create.asWidget().getElement().setId("Tab_"+label);
    create.setIcon(iconType);
    create.setHeading(label);
    toAddTo.add(create.asTabLink());

    final DivWidget createContent = new DivWidget();
    createContent.getElement().setId("Content_"+label);
    createContent.addStyleName("positionRelative");
    create.add(createContent);
    zeroPadding(createContent);

    return new TabAndContent(create, createContent);*/
  }

/*  void zeroPadding(Panel createContent) {
    DOM.setStyleAttribute(createContent.getElement(), "paddingLeft", "0px");
    DOM.setStyleAttribute(createContent.getElement(), "paddingRight", "0px");
  }*/

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
