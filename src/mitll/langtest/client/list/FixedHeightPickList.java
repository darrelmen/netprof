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
import com.github.gwtbootstrap.client.ui.Tooltip;
import com.github.gwtbootstrap.client.ui.constants.IconSize;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HasWidgets;
import mitll.hlt.domino.shared.model.project.ClientPMProject;
import mitll.hlt.domino.shared.model.project.ClientProject;
import mitll.hlt.domino.shared.model.project.Contributor;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FixedHeightPickList {
  NavList contributorNL;

  private void addContributorDF(HasWidgets fields) {
    contributorNL = new NavList();
    contributorNL.addStyleName("domino-picklist");
    contributorNL.addStyleName("domino-visible-picklist");
    updateContributorFields(false);
    //contributorDF = new DecoratedFields("Apply to", contributorNL);
    //fields.add(contributorDF.getCtrlGroup());
  }

  private void updateContributorFields(boolean activeOnly) {
    if (contributorNL != null) {
      contributorNL.clear();
//      Set<Contributor> newAC = new HashSet<>();
//      ClientProject selProj = getSelectedProject();
//      ClientPMProject currProj = getState().getPMProject();
//      if (selProj != null && currProj != null && selProj instanceof ClientPMProject) {
//        ClientPMProject selPMProj = (ClientPMProject)selProj;
//        addContributorFields(selPMProj.getProjectContributors(activeOnly), newAC, "");
//        if (currProj.getId() != selPMProj.getId()) {
//          addContributorFields(currProj.getProjectContributors(activeOnly), newAC, currProj.getName() + " - ");
//        }
//        this.activeTeam = newAC;
//      }


  //    if (currProj.getId() != selPMProj.getId()) {

      }
  }
/*
  private void addContributorFields(List<Contributor> contribs, Set<Contributor> newAC, String prefix) {
    for (Contributor contrib : contribs) {
      NavLink link = createNavLink(prefix + contrib.getContribUser().getFullName(),
          IconType.CHECK_EMPTY, IconSize.DEFAULT, new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {

            }
          });
      Tooltip tip = new Tooltip(contrib.getContribUser().getUserId());
      tip.setWidget(link);
      tip.setShowDelay(500);
      tip.setAnimation(true);
      tip.setPlacement(Placement.LEFT);
      tip.reconfigure();
      //contributorMap.put(link, contrib);
      link.addStyleName("pick-item");
      contributorNL.add(link);
*//*      if (activeTeam.contains(contrib)) {
        updateLink(link, contributorMap, newAC);
      }*//*
    }
  }*/

  private <CType> void updateLink(NavLink nl, Map<NavLink, CType> nlMap, Set<CType> activeSet) {
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
  }

  public NavLink createNavLink(String linkText, IconType linkIcon,
                               IconSize iconSize, ClickHandler handler) {
    return createNavLink(linkText, linkIcon, iconSize, handler, false);
  }

  public NavLink createNavLink(String linkText, IconType linkIcon,
                               IconSize iconSize, ClickHandler handler, boolean active) {
    NavLink theLink = new NavLink();
    if (linkText != null) {
      theLink.setText(linkText);
    }
    if (linkIcon != null) {
      theLink.setIcon(linkIcon);
      theLink.setIconSize(iconSize);
    }
    // force right dropdowns to align left.
    theLink.addStyleName("left-align");
    theLink.addClickHandler(handler);
    theLink.setActive(active);
    return theLink;
  }


}
