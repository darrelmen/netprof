package mitll.langtest.client.table;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.HasRows;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 2/15/13
 * Time: 3:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class PagerTable {
  protected Panel getPagerAndTable(HasRows table, Widget tableAsPanel, int pageSize, int fastForwardRows) {
    SimplePager.Resources DEFAULT_RESOURCES = GWT.create(SimplePager.Resources.class);
    SimplePager pager = new SimplePager(SimplePager.TextLocation.CENTER, DEFAULT_RESOURCES, true, fastForwardRows, true);

    // Set the cellList as the display.
    pager.setDisplay(table);
    pager.setPageSize(pageSize);
    // Add the pager and list to the page.
    VerticalPanel vPanel = new VerticalPanel();
    vPanel.add(pager);
    vPanel.add(tableAsPanel);
    return vPanel;
  }
}
