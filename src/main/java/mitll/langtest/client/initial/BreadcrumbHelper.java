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

package mitll.langtest.client.initial;

import com.github.gwtbootstrap.client.ui.Breadcrumbs;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.google.gwt.dom.client.Style;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.project.ProjectChoices;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.project.ProjectStartupInfo;
import mitll.langtest.shared.project.SlimProject;
import mitll.langtest.shared.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Created by go22670 on 6/13/17.
 */
public class BreadcrumbHelper implements IBreadcrumbHelper {
  private final Logger logger = Logger.getLogger("BreadcrumbHelper");

  private Breadcrumbs breadcrumbs;
  private static final String DIVIDER = ">";
  private static final String HOME = "Home";
  private final UserManager userManager;
  private final LifecycleSupport lifecycleSupport;
  private final BreadcrumbPartner breadcrumbPartner;

  private static final boolean DEBUG = false;

  BreadcrumbHelper(UserManager userManager,
                   LifecycleSupport lifecycleSupport,
                   BreadcrumbPartner breadcrumbPartner) {
    this.userManager = userManager;
    this.lifecycleSupport = lifecycleSupport;
    this.breadcrumbPartner = breadcrumbPartner;
  }

  @Override
  public void setVisible(boolean visible) {
    breadcrumbs.setVisible(visible);
  }

  /**
   * @return
   * @see InitialUI#InitialUI
   */
  @Override
  public Breadcrumbs getBreadcrumbs() {
    Breadcrumbs crumbs = new Breadcrumbs(DIVIDER);
    crumbs.getElement().setId("breadcrumb");

    crumbs.addStyleName("floatLeft");

    Style style = crumbs.getElement().getStyle();
    style.setMarginTop(7, Style.Unit.PX);
    style.setMarginBottom(0, Style.Unit.PX);
    crumbs.addStyleName("rightTwentyMargin");
    style.clearProperty("backgroundColor");
    crumbs.setVisible(false);
    breadcrumbs = crumbs;

    addCrumbs(true);
    // logger.info("getBreadcrumbs now has " + crumbs.getElement().getChildCount() + " links");
    return crumbs;
  }


  /**
   * @param showOnlyHomeLink
   * @paramx crumbs
   * @see InitialUI#chooseProjectAgain()
   * @see #getBreadcrumbs()
   */
  @Override
  public void addCrumbs(boolean showOnlyHomeLink) {
    Breadcrumbs crumbs = breadcrumbs;
    User current = userManager.getCurrent();
    if (current != null) {
      ProjectStartupInfo startupInfo = lifecycleSupport.getProjectStartupInfo();
      if (startupInfo == null) {
        //  logger.info("addCrumbs no project startup info yet for " + current.getUserID());
        if (showOnlyHomeLink) {
          //   logger.info("\taddCrumbs add all link");
          addHomeLink(crumbs);
        }
      } else {
        addBreadcrumbLevels(crumbs, startupInfo);
      }
      // logger.info("addCrumbs ");

      // WHY???
      //  banner.checkProjectSelected();
    }
    if (crumbs != null) {
      crumbs.setVisible(crumbs.getWidgetCount() > 0);
    }
/*    else {
     // logger.warning("addCrumbs no current user");
    }*/
  }

  /**
   * @param crumbs
   * @param startupInfo
   * @see #addCrumbs
   */
  private void addBreadcrumbLevels(Breadcrumbs crumbs, ProjectStartupInfo startupInfo) {
    int currentProject = startupInfo.getProjectid();
    crumbs.setVisible(true);

    addHomeLink(crumbs);

    for (SlimProject project : lifecycleSupport.getStartupInfo().getProjects()) {
      if (project.hasChildren() && project.hasChild(currentProject)) {
        if (DEBUG) {
          logger.info("addBreadcrumbLevels add for " + project.getName() + " children " + project.getChildren().size());
        }

        crumbs.add(getLangBreadcrumb(project));

        addProjectCrumb(crumbs, project.getChildByMode(currentProject, breadcrumbPartner.getNavigation().getCurrentView().getMode()));
        break;
      } else if (project.getID() == currentProject) {
        if (DEBUG) {
          logger.info("addBreadcrumbLevels add for " + project.getName() + " children " + project.getChildren().size());
        }
        addProjectCrumb(crumbs, project);
        break;
      }
      //else {
      // logger.info("addBreadcrumbLevels skipping project " + project);
      //}
    }
  }

  /**
   * @param crumbs
   * @param project
   * @return
   * @see #addCrumbs
   */
  private void addProjectCrumb(Breadcrumbs crumbs, SlimProject project) {
    NavLink lang = new NavLink(project.getName());

    if (DEBUG) logger.info("addProjectCrumb  for " + project.getName() + " " + project.getMode());

    lang.addClickHandler(clickEvent -> {
      logger.info("addProjectCrumb choose project again for " + project.getName() + " " + project.getMode());
      breadcrumbPartner.chooseProjectAgain();
    });
    crumbs.add(lang);
  }

  /**
   * @param project
   * @return
   * @see #addBreadcrumbLevels
   */
  @NotNull
  private NavLink getLangBreadcrumb(SlimProject project) {
    String text = project.getLanguage().toDisplay();
    NavLink lang = new NavLink(text);

    if (DEBUG) {
      logger.info("getLangBreadcrumb for " + project.getName() + " (" + text + ") : " + project.getMode());
    }

    lang.addClickHandler(clickEvent -> {
      if (DEBUG) {
        logger.info("getLangBreadcrumb got click on " + project.getName());
      }
      removeUntilCrumb(2);
      breadcrumbPartner.resetLanguageSelection(2, project);
      //choices.showProject(project);
    });
    return lang;
  }


  /**
   * @param crumbs
   */
  private void addHomeLink(Breadcrumbs crumbs) {
    crumbs.clear();

    NavLink all = new NavLink(HOME);
    all.addClickHandler(event -> breadcrumbPartner.chooseProjectAgain());

    crumbs.add(all);
  }

  @Override
  public void clearBreadcrumbs() {
    // logger.info("breadcrumbs clear");
    breadcrumbs.clear();
  }

  /**
   * @param name
   * @return
   * @see ProjectChoices#gotClickOnFlag
   */
  @Override
  @NotNull
  public NavLink makeBreadcrumb(String name) {
    NavLink projectCrumb = new NavLink(name);
    //   logger.info("makeBreadcrumb add " + name + " now " + breadcrumbs.getWidgetCount());
    breadcrumbs.add(projectCrumb);
    breadcrumbs.setVisible(true);
    return projectCrumb;
  }

  /**
   * @see InitialUI#clickOnParentCrumb(SlimProject) ()
   */
  @Override
  public void removeLastCrumb() {
    // logger.info("removeLastCrumb has " + breadcrumbs.getWidgetCount());
    breadcrumbs.remove(breadcrumbs.getWidgetCount() - 1);
    // logger.info("removeLastCrumb now " + breadcrumbs.getWidgetCount());
  }

  /**
   * @param count
   * @see #getLangBreadcrumb
   */
  private void removeUntilCrumb(int count) {
    int widgetCount = breadcrumbs.getWidgetCount();
    int initial = widgetCount - 1;
    //logger.info("removeUntilCrumb crumbs " + widgetCount + " remove to " + count + " initial " + initial);

    for (int i = initial; i >= count; i--) {
      /*boolean remove =*/
      breadcrumbs.remove(i);
      // logger.info("removeUntilCrumb remove at " + i + "  " + remove);
    }
//    logger.info("removeUntilCrumb now " + breadcrumbs.getWidgetCount());
  }

}
