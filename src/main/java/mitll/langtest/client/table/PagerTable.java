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

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.HasRows;

import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 2/15/13
 * Time: 3:15 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class PagerTable {
/*  public Panel getPagerAndTable(HasRows table, Widget tableAsPanel, int pageSize, int fastForwardRows) {
    com.github.gwtbootstrap.client.ui.SimplePager.Resources DEFAULT_RESOURCES = GWT.create(com.github.gwtbootstrap.client.ui.SimplePager.Resources.class);
    com.github.gwtbootstrap.client.ui.SimplePager pager = new com.github.gwtbootstrap.client.ui.SimplePager(com.github.gwtbootstrap.client.ui.SimplePager.TextLocation.CENTER, DEFAULT_RESOURCES, true, fastForwardRows, true);

    // Set the cellList as the display.
    pager.setDisplay(table);
    pager.setPageSize(pageSize);
    // Add the pager and list to the page.
    VerticalPanel vPanel = new VerticalPanel();
    vPanel.add(pager);
    vPanel.add(tableAsPanel);

    return vPanel;
  }*/

  protected Panel getOldSchoolPagerAndTable(HasRows table, Widget tableAsPanel, int pageSize, int fastForwardRows, Widget toRightOfPager) {
    SimplePager.Resources DEFAULT_RESOURCES = GWT.create(SimplePager.Resources.class);
    SimplePager pager = new SimplePager(SimplePager.TextLocation.CENTER, DEFAULT_RESOURCES, true, fastForwardRows, true);
    pager.getElement().setId("SimplePager");
    // Set the cellList as the display.
    pager.setDisplay(table);
    pager.setPageSize(pageSize);
    // Add the pager and list to the page.

    VerticalPanel vPanel = new VerticalPanel();
    HorizontalPanel horizontalPanel = new HorizontalPanel();
    horizontalPanel.add(pager);
    if (toRightOfPager != null) horizontalPanel.add(toRightOfPager);
    vPanel.add(horizontalPanel);
    vPanel.add(tableAsPanel);

    return vPanel;
  }

  private final DateTimeFormat yformat = DateTimeFormat.getFormat("yy");
  private final DateTimeFormat format = DateTimeFormat.getFormat("MM-dd-yy h:mm:ss a");
  private final DateTimeFormat sformat = DateTimeFormat.getFormat("MM-dd h:mm:ss a");
  private final String thisYear = yformat.format(new Date());

  protected SafeHtml getSafeHTMLForTimestamp(long timestamp) {
    Date date = new Date(timestamp);
    String sampleYear = yformat.format(date);
    DateTimeFormat dtf = (sampleYear.equals(thisYear)) ? sformat : format;
    String noWrapContent = dtf.format(date);
    return getNoWrapContent(noWrapContent);
  }

  protected SafeHtml getNoWrapContent(String noWrapContent) {
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    sb.appendHtmlConstant("<div style='white-space: nowrap;'><span>" +
        noWrapContent +
        "</span>");

    sb.appendHtmlConstant("</div>");
    return sb.toSafeHtml();
  }

  protected SafeHtml getAnchorHTML(String href, String label) {
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    sb.appendHtmlConstant("<a href='" +
        href +
        "'" +
        ">");
    sb.appendEscaped(label);
    sb.appendHtmlConstant("</a>");
    return sb.toSafeHtml();
  }

  protected Anchor getDownloadAnchor() {
    Anchor w = new Anchor(getURL2());
    w.addStyleName("leftFiveMargin");
    w.addStyleName("topFiveMargin");
    return w;
  }

  protected abstract SafeHtml getURL2();
}
