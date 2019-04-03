/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.NavLink;
import com.google.gwt.event.dom.client.ClickHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Created by go22670 on 4/10/17.
 */
class LinkAndTitle {
  private final ClickHandler clickHandler;
  private String title;
  private final String linkURL;
  private NavLink myLink;

  /**
   * @param title
   * @param click
   * @see UserMenu#getChangePassword
   */
  LinkAndTitle(String title, ClickHandler click) {
    this.title = title;
    this.clickHandler = click;
    this.linkURL = null;
  }

  /**
   * @param title
   * @param linkURL
   * @see UserMenu#getCogMenuChoicesForAdmin
   * @see NewBanner#getRightSideChoices
   */
  LinkAndTitle(String title, String linkURL) {
    this.title = title;
    this.linkURL = linkURL;
    this.clickHandler = null;
  }

  /**
   * @return
   */
  @NotNull
  NavLink makeNewLink() {
    NavLink monitoringC = new NavLink(title);
    if (linkURL != null) {
      monitoringC.setHref(linkURL);
      monitoringC.setTarget("_blank");
    } else {
      monitoringC.addClickHandler(clickHandler);
    }

    return myLink = monitoringC;
  }

  NavLink getMyLink() {
    return myLink;
  }

  public void setTitle(String title) {
    this.title = title;
    myLink.setText(title);
  }

  @Override
  public String toString() {
    return "Nav " + title;
  }
}
