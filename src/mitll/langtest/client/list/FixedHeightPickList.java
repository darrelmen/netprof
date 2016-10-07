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

package mitll.langtest.client.list;

import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.NavList;
import com.github.gwtbootstrap.client.ui.constants.IconSize;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HasWidgets;
import mitll.langtest.client.domino.common.DecoratedFields;
import mitll.langtest.client.table.ListSelect;

import java.util.List;

public class FixedHeightPickList {
  private NavList contributorNL;

  public void addContributorDF(HasWidgets fields, List<String> choices, ListSelect.Selection handler) {
    contributorNL = new NavList();
    contributorNL.addStyleName("domino-picklist");
    contributorNL.addStyleName("domino-visible-picklist");
    updateContributorFields(false, choices, handler);

    DecoratedFields contributorDF = new DecoratedFields("Apply to", contributorNL);
    fields.add(contributorDF.getCtrlGroup());
   // fields.add(contributorNL);
  }

  private void updateContributorFields(boolean activeOnly, List<String> choices,  ListSelect.Selection handler) {
    if (contributorNL != null) {
      contributorNL.clear();
      addContributorFields(choices, handler);
    }
  }

  private void addContributorFields(List<String> choices,  ListSelect.Selection handler
                                    /*, String prefix*/) {
    for (String contrib : choices) {
      NavLink link = createNavLink(//prefix +
          contrib,
          IconType.CHECK_EMPTY, IconSize.DEFAULT, new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
              handler.gotSelection(contrib);
            }
          });

/*      Tooltip tip = new Tooltip(contrib);//contrib.getContribUser().getUserId());
      tip.setWidget(link);
      tip.setShowDelay(500);
      tip.setAnimation(true);
      tip.setPlacement(Placement.LEFT);
      tip.reconfigure();
      //contributorMap.put(link, contrib);
      link.addStyleName("pick-item");*/

      contributorNL.add(link);
    }
  }

/*  private <CType> void updateLink(NavLink nl, Map<NavLink, CType> nlMap, Set<CType> activeSet) {
    boolean newChecked = !nl.isActive();
    IconType icon = newChecked ? IconType.OK : IconType.CHECK_EMPTY;
    nl.setActive(newChecked);
    nl.setIcon(icon);
    CType active = nlMap.get(nl);
    if (newChecked) {
      activeSet.add(active);
    } else {
      activeSet.remove(active);
    }
  }*/

  public NavLink createNavLink(String linkText,
                               IconType linkIcon,
                               IconSize iconSize,
                               ClickHandler handler) {
    return createNavLink(linkText, linkIcon, iconSize, handler, false);
  }

  public NavLink createNavLink(String linkText,
                               IconType linkIcon,
                               IconSize iconSize,
                               ClickHandler handler,
                               boolean active) {
    NavLink theLink = new NavLink();
    if (linkText != null) {
      theLink.setText(linkText);
    }
/*    if (linkIcon != null) {
      theLink.setIcon(linkIcon);
      theLink.setIconSize(iconSize);
    }*/
    // force right dropdowns to align left.
    theLink.addStyleName("left-align");
    theLink.addClickHandler(handler);
    theLink.setActive(active);
    return theLink;
  }


}
