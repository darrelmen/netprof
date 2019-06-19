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
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.project.ProjectChoices;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.project.ProjectMode;
import mitll.langtest.shared.project.ProjectStartupInfo;
import mitll.langtest.shared.project.ProjectType;
import mitll.langtest.shared.project.SlimProject;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static mitll.langtest.shared.project.ProjectMode.EITHER;

/**
 * Created by go22670 on 6/13/17.
 */
public class BreadcrumbHelper implements IBreadcrumbHelper {
  private final Logger logger = Logger.getLogger("BreadcrumbHelper");

  private Breadcrumbs breadcrumbs;
  private static final String DIVIDER = ">";

  private final UserManager userManager;
  private final LifecycleSupport lifecycleSupport;
  private final BreadcrumbPartner breadcrumbPartner;

  private static final boolean DEBUG = false;
  private static final boolean DEBUG_REMOVE = false;

  /**
   * @param userManager
   * @param lifecycleSupport
   * @param breadcrumbPartner
   * @see InitialUI#InitialUI(LangTest, UserManager)
   */
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
   * @param uiLifecycle
   * @return
   * @see InitialUI#InitialUI
   */
  @Override
  public Breadcrumbs getBreadcrumbs(UILifecycle uiLifecycle) {
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

    addCrumbs(true, uiLifecycle);

    // logger.info("getBreadcrumbs now has " + crumbs.getElement().getChildCount() + " links");

    return crumbs;
  }

  /**
   * Given the current project, reconstruct the breadcrumb state that you would have seen
   * as a result of the choices made in the UI.
   *
   * @param showOnlyHomeLink
   * @param uiLifecycle
   * @paramx crumbs
   * @see InitialUI#chooseProjectAgain()
   * @see IBreadcrumbHelper#getBreadcrumbs(UILifecycle)
   */
  @Override
  public void addCrumbs(boolean showOnlyHomeLink, UILifecycle uiLifecycle) {
    Breadcrumbs crumbs = breadcrumbs;

    if (userManager.getCurrent() != null) {
      ProjectStartupInfo startupInfo = lifecycleSupport.getProjectStartupInfo();
      if (startupInfo == null) {
        //  logger.info("addCrumbs no project startup info yet for " + current.getUserID());
        if (showOnlyHomeLink) {
          if (DEBUG) logger.info("addCrumbs : addHomeLink - show only home");
          addHomeLink(crumbs);
        }
      } else {
        addBreadcrumbLevels(crumbs, startupInfo, uiLifecycle);
      }
    } else {
      if (DEBUG) logger.info("no user yet...");
    }

    if (crumbs != null) {
      crumbs.setVisible(crumbs.getWidgetCount() > 1);
    }
/*    else {
     // logger.warning("addCrumbs no current user");
    }*/
  }

  /**
   * @param crumbs
   * @param startupInfo
   * @param uiLifecycle
   * @see #addCrumbs
   */
  private void addBreadcrumbLevels(Breadcrumbs crumbs, ProjectStartupInfo startupInfo, UILifecycle uiLifecycle) {
    int currentProject = startupInfo.getProjectid();
    crumbs.setVisible(true);
    if (DEBUG) {
      logger.info("addBreadcrumbLevels for project #" + currentProject);
    }
    addHomeLink(crumbs);

    for (SlimProject project : lifecycleSupport.getStartupInfo().getProjects()) {
      if (project.hasChildren() && project.hasChild(currentProject)) {
        addBreadcrumbsForCurrentState(crumbs, uiLifecycle, currentProject, project);
        break;
      } else if (project.getID() == currentProject) {
        if (DEBUG) {
          logger.info("addBreadcrumbLevels add for '" + project.getName() + "' children " + project.getChildren().size());
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
   * Maybe have to add three : one for language choice, one for project choice, one for mode choice - dialog vs vocab
   *
   * @param crumbs
   * @param uiLifecycle
   * @param currentProject
   * @param project
   */
  private void addBreadcrumbsForCurrentState(Breadcrumbs crumbs, UILifecycle uiLifecycle, int currentProject, SlimProject project) {
    ProjectType type = project.getProjectType();

    if (DEBUG) {
      ProjectMode mode = project.getMode();

      logger.info("addBreadcrumbLevels add for " +
          "\n\tproject id " + project.getID() +
          "\n\tproject  " + project.getName() +
          "\n\tkind     " + mode +
          "\n\ttype     " + type +
          "\n\tchildren " + project.getChildren().size());
    }

    // first the language
    crumbs.add(getLangBreadcrumb(project));

    INavigation.VIEWS currentView = breadcrumbPartner.getNavigation().getCurrentView();
    ProjectMode mode = getModeMaybeFromView(currentView);

    if (DEBUG) logger.info("view " + currentView + " mode " + mode);
    // then the project underneath the language - could be several projects under a language - like chinese simplified and traditional
    SlimProject childByMode = project.getChildByMode(currentProject, mode);
    addProjectCrumb2(childByMode, uiLifecycle);

    if (type == ProjectType.DIALOG) {
      List<SlimProject> children = childByMode.getChildren();

      //   logger.info("project " + childByMode.getName() + " has " + children.size());

      if (children.size() == 2) {
        List<SlimProject> childrenMatchingMode = children.stream().filter(slimProject ->
            (slimProject.getMode() == mode)).collect(Collectors.toList());

//        childrenMatchingMode.forEach(slimProject -> logger.info(slimProject.getID() + " " + slimProject.getName() + " " + slimProject.getMode()));
        if (!childrenMatchingMode.isEmpty()) {
          SlimProject modeChoice = childrenMatchingMode.get(0);
          if (DEBUG) logger.info("modeChoice " + modeChoice);
          addProjectCrumb2(modeChoice, uiLifecycle);
        }
      }
    }
  }

  private ProjectMode getModeMaybeFromView(INavigation.VIEWS currentView) {
    ProjectMode mode = lifecycleSupport.getMode();
    if (DEBUG) logger.info("currentView " + currentView);
    ProjectMode mode1 = currentView.getMode();
    if (mode1 != EITHER) {
      if (mode1 != mode) {
        if (DEBUG) logger.info("currentView " + currentView + " trumps the stored mode " + mode);
        mode = mode1;
      }
    }

    return mode;
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
      if (DEBUG) logger.info("addProjectCrumb choose project again for " + project.getName() + " " + project.getMode());
      breadcrumbPartner.chooseProjectAgain();
    });
    crumbs.add(lang);
  }

  /**
   * Repeat what happens in click on flag
   *
   * @param crumbs
   * @param project
   */
  private void addProjectCrumb2(SlimProject project, UILifecycle uiLifecycle) {
    NavLink breadcrumb = makeBreadcrumb(project.getName());
    breadcrumb.addClickHandler(clickEvent -> uiLifecycle.clickOnParentCrumb(project, breadcrumb));
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
    });
    return lang;
  }

  /**
   * @param crumbs
   */
  private void addHomeLink(Breadcrumbs crumbs) {
    // logger.info("addHomeLink " + crumbs.getWidgetCount());
    crumbs.clear();

    NavLink all = new NavLink("");
    all.setIcon(IconType.HOME);

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

    if (DEBUG) {
      logger.info("makeBreadcrumb add " + name + " now " + breadcrumbs.getWidgetCount());
    }

    breadcrumbs.add(projectCrumb);
    breadcrumbs.setVisible(true);
    return projectCrumb;
  }

  /**
   * @see UILifecycle#clickOnParentCrumb
   */
  @Override
  public void removeLastCrumb() {
//    logger.info("removeLastCrumb has " + breadcrumbs.getWidgetCount());
    breadcrumbs.remove(breadcrumbs.getWidgetCount() - 1);
    // logger.info("removeLastCrumb now " + breadcrumbs.getWidgetCount());
  }

  public void removeUntil(Widget widget) {
    int widgetCount = breadcrumbs.getWidgetCount();
    int initial = widgetCount - 1;
    if (DEBUG_REMOVE) {
      logger.info("removeUntilCrumb crumbs " + widgetCount + " initial " + initial);
    }

    Widget found = null;
    int fi = -1;
    for (int i = initial; i >= 0; i--) {
      Widget candidate = breadcrumbs.getWidget(i);
      if (candidate == widget) {
        found = candidate;

        if (DEBUG_REMOVE) {
          String text = ((NavLink) found).getText();
          logger.info("removeUntil: found bread '" + text + "' at " + i);
        }
        fi = i;
        break;
      }
    }

    if (found != null) {
      for (int i = initial; i > fi; i--) {
        Widget candidate = breadcrumbs.getWidget(i);
        String text = getText(candidate);
        if (candidate == widget) {
          if (DEBUG_REMOVE) {
            logger.info("removeUntil: stop at " + i + " " + text);
          }
          break;
        } else {
          if (DEBUG_REMOVE) {
            logger.info("removeUntil: remove at " + i + " " + text);
          }
          breadcrumbs.remove(i);
        }
      }
    }
  }

  /**
   * @param count
   * @see #getLangBreadcrumb
   */
  private void removeUntilCrumb(int count) {
    int widgetCount = breadcrumbs.getWidgetCount();
    int initial = widgetCount - 1;
    //  logger.info("removeUntilCrumb crumbs " + widgetCount + " remove to " + count + " initial " + initial);

    for (int i = initial; i >= count; i--) {
      /*boolean remove =*/
      Widget widget = breadcrumbs.getWidget(i);
      String text = getText(widget);
      breadcrumbs.remove(i);
      if (DEBUG_REMOVE) logger.info("removeUntilCrumb remove at " + i + " : " + text);
    }
//    logger.info("removeUntilCrumb now " + breadcrumbs.getWidgetCount());
  }

  private String getText(Widget widget) {
    return widget instanceof NavLink ? ((NavLink) widget).getText() : widget.getClass().toString();
  }
}
