package mitll.langtest.client.initial;

import com.github.gwtbootstrap.client.ui.Breadcrumbs;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.google.gwt.dom.client.Style;
import mitll.langtest.shared.project.ProjectStartupInfo;
import mitll.langtest.shared.project.SlimProject;
import mitll.langtest.shared.user.User;
import org.jetbrains.annotations.NotNull;

/**
 * Created by go22670 on 6/13/17.
 */
public class BreadcrumbHelper {
  /**
   * TODO : move breadcrumbs up into banner
   */
  private final Breadcrumbs breadcrumbs;

  public BreadcrumbHelper() {
    breadcrumbs = getBreadcrumbs();
  }

  /**
   * @return
   * @see #addBreadcrumbs
   *//*
  private Breadcrumbs getBreadcrumbs() {
    Breadcrumbs crumbs = new Breadcrumbs(">");
    crumbs.getElement().setId("breadcrumb");
    Style style = crumbs.getElement().getStyle();
    style.setMarginBottom(0, Style.Unit.PX);
    style.clearProperty("backgroundColor");
    crumbs.setVisible(false);
    addCrumbs(crumbs);
    // logger.info("getBreadcrumbs now has " + crumbs.getElement().getChildCount() + " links");
    return crumbs;
  }

  *//**
   * @param crumbs
   * @see #chooseProjectAgain()
   * @see #getBreadcrumbs()
   *//*
  private void addCrumbs(Breadcrumbs crumbs) {
    User current = userManager.getCurrent();
    if (current != null) {
      ProjectStartupInfo startupInfo = lifecycleSupport.getProjectStartupInfo();
      if (startupInfo != null) {
        addBreadcrumbLevels(crumbs, startupInfo);
      } else {
        logger.info("no project startup info...");
      }

      banner.checkProjectSelected();
    }
  }
  private void addBreadcrumbLevels(Breadcrumbs crumbs, ProjectStartupInfo startupInfo) {
    int currentProject = startupInfo.getProjectid();
    crumbs.clear();
    breadcrumbs.setVisible(true);
    for (SlimProject project : lifecycleSupport.getStartupInfo().getProjects()) {
      if (project.hasChildren() && project.hasChild(currentProject)) {
        crumbs.add(getLangBreadcrumb(project));
        addProjectCrumb(crumbs, project.getChild(currentProject));
*//*        for (int i = 0; i < crumbs.getWidgetCount(); i++) {
          logger.info("breadcrumb has " + crumbs.getWidget(i));
        }*//*
        break;
      } else if (project.getID() == currentProject) {
        addProjectCrumb(crumbs, project);
        break;
      } else {
        // logger.fine("getBreadcrumbs skipping project " + project);
      }
    }
  }

  *//**
   * @param project
   * @return
   * @see #addBreadcrumbLevels
   *//*
  @NotNull
  private NavLink getLangBreadcrumb(SlimProject project) {
    NavLink lang = new NavLink(project.getLanguage());
    lang.addClickHandler(clickEvent -> {
      //  logger.info("getLangBreadcrumb got click on " + project.getName());
      clearStartupInfo();
      clearContent();
      removeUntilCrumb(1);
      choices.showProject(project);
    });
    return lang;
  }
*/
}
