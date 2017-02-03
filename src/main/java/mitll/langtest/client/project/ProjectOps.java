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

package mitll.langtest.client.project;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import mitll.langtest.client.LifecycleSupport;
import mitll.langtest.client.custom.Navigation;
import mitll.langtest.client.custom.tabs.TabAndContent;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.services.ProjectService;
import mitll.langtest.client.services.ProjectServiceAsync;
import mitll.langtest.shared.project.ProjectInfo;
import mitll.langtest.shared.project.ProjectStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ProjectOps implements RequiresResize {
  private final Logger logger = Logger.getLogger("ProjectOps");
  private final ExerciseController controller;

  private DivWidget currentContent;
  private DivWidget currentUserForm;

  private final ProjectServiceAsync projectServiceAsync = GWT.create(ProjectService.class);
  private final LifecycleSupport lifecycleSupport;

  /**
   * @param controller
   * @paramx userManager
   * @see Navigation#addProjectMaintenance()
   */
  public ProjectOps(ExerciseController controller, LifecycleSupport lifecycleSupport) {
    this.controller = controller;
    this.lifecycleSupport = lifecycleSupport;
  }

  public void refreshStartupInfo() {
    lifecycleSupport.refreshStartupInfo();
  }

  /**
   * @param tabAndContent
   * @see Navigation#addProjectMaintenance
   * @see Navigation#selectPreviousTab
   */
  public void show(TabAndContent tabAndContent) {
    DivWidget content = tabAndContent.getContent();
    content.clear();

    DivWidget left = addDiv(content);

    left.getElement().setId("productionProjects");
    left.getElement().getStyle().setMarginLeft(10, Style.Unit.PX);

    //DivWidget right = addDiv(content);
    DivWidget detail = addDiv(content);
    detail.getElement().getStyle().setClear(Style.Clear.LEFT);
    detail.addStyleName("leftFiveMargin");

    currentContent = left;
    currentUserForm = detail;
    showInitialState(left, detail);
  }

  private void showInitialState(final DivWidget left, final DivWidget detail/*, NavLink first*/) {
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        showProjects(//User.Kind.STUDENT,
            left, detail);
        // }
      }
    });
  }

  /**
   * @return
   * @paramx left
   * @paramx right
   * @paramx detail
   * @see #showProjects
   */
/*  private NavLink getKinds(DivWidget left, DivWidget right, DivWidget detail) {
    NavList kindsLinks = new NavList();
    kindsLinks.setWidth("180px");
    left.add(kindsLinks);

    // add users ----
    kindsLinks.add(new NavHeader(USERS));
    User current = controller.getUserState().getCurrent();
    User.Kind loggedInUserRole = current.getUserKind();
    NavLink first = addRoleLinks(right, detail, kindsLinks, current, loggedInUserRole);

    kindsLinks.add(new NavHeader("Invitations"));
    kindsLinks.add(getInviteLink(right, detail));
    return first;
  }*/

/*  private NavLink addRoleLinks(DivWidget right, DivWidget detail, NavList kindsLinks, User current, User.Kind loggedInUserRole) {
    NavLink first = null;

    for (User.Kind kind : User.Kind.values()) {
      if (kind.shouldShow() &&
          (kind.compareTo(loggedInUserRole) < 0 || current.isAdmin())) {
        NavLink userLink = getUserLink(kind, right, detail);
        kindsLinks.add(userLink);
        if (first == null) first = userLink;
      }
    }
    return first;
  }*/
  private DivWidget addDiv(DivWidget content) {
    DivWidget left = new DivWidget();
    left.addStyleName("floatLeft");
    content.add(left);
    return left;
  }

  /**
   * @paramx kind
   * @paramx content
   * @paramx userForm
   * @return
   */
/*  private NavLink getUserLink(User.Kind kind, DivWidget content, DivWidget userForm) {
    NavLink students = new NavLink(kind.getName() + "s");
    students.setIcon(kindToIcon.get(kind));
    students.getElement().setId("link_" + kind);
    Label w5 = new Label("");
    kindToLabel.put(kind, w5);
    w5.getElement().getStyle().setFloat(Style.Float.RIGHT);
    students.add(w5);
    students.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent clickEvent) {
        lastClicked.setActive(false);
        showProjects(kind, content, userForm);
        students.setActive(true);
        lastClicked = students;
      }
    });
    return students;
  }*/

/*  private NavLink getInviteLink(DivWidget content, DivWidget pendingInvites) {
    NavLink students = new NavLink("Pending");
    students.setIcon(IconType.ENVELOPE);
    students.getElement().setId("link_" + "pending");
    Label w5 = new Label("");
    inviteToLabel.put("pending", w5);
    w5.getElement().getStyle().setFloat(Style.Float.RIGHT);
    students.add(w5);
    students.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent clickEvent) {
        lastClicked.setActive(false);
        //showProjects(kind, content, userForm);
        showInvitations(true, content);
        students.setActive(true);
        lastClicked = students;
      }
    });
    return students;
  }*/
  public void reload() {
    showProjects(currentContent, currentUserForm);
  }

  /**
   * Talk to the service.
   *
   * @param content
   * @param userForm
   * @paramx kind
   * @see #showInitialState
   */
  private void showProjects(
                            final DivWidget content, DivWidget userForm) {

    projectServiceAsync.getAll(new AsyncCallback<List<ProjectInfo>>() {
      @Override
      public void onFailure(Throwable throwable) {

      }

      @Override
      public void onSuccess(List<ProjectInfo> projectInfos) {
        Panel hp = new HorizontalPanel();

        projectContainers.clear();
//        DivWidget contentP = new DivWidget();
//        hp.add(contentP);
//
//        DivWidget content1 = getProjectListContainer(hp);
//        DivWidget content2 = getProjectListContainer(hp);
//        DivWidget content3 = getProjectListContainer(hp);

        boolean first = true;
        for (ProjectStatus status : ProjectStatus.values()) {
          DivWidget contentP;
          if (first) {
            contentP = new DivWidget();
            hp.add(contentP);
          } else {
            contentP = getProjectListContainer(hp);

          }
          showProjects(projectInfos, status, contentP, userForm, first);
          first = false;
        }
/*
        showProjectList(contentP, projectInfos, userForm, "Projects", "Production", ProjectStatus.PRODUCTION, true);

        showProjects(projectInfos, DEVELOPMENT, content1, userForm);
        showProjectList(content2, projectInfos, userForm, "Projects", "Evaluation", ProjectStatus.EVALUATION, false);
        showProjectList(content3, projectInfos, userForm, "Projects", "Retired", ProjectStatus.RETIRED, false);
*/

        content.clear();
        content.add(hp);

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
          @Override
          public void execute() {
            onResize();
          }
        });
      }
    });
  }

  private void showProjects(List<ProjectInfo> projectInfos, ProjectStatus development, DivWidget content1, DivWidget userForm, boolean first) {
    showProjectList(content1, projectInfos, userForm, "Project",
        development.name().substring(0, 1).toUpperCase() + development.name().substring(1).toLowerCase(), development, first);
  }

  @NotNull
  private DivWidget getProjectListContainer(Panel hp) {
    DivWidget content2 = new DivWidget();
    hp.add(content2);
    content2.addStyleName("leftFiveMargin");
    return content2;
  }

  @Deprecated private RequiresResize requiresResize = null;

  /**
   * Show projects of this kind.
   *
   * @param content
   * @param userForm
   * @param rowHeader
   * @param listTitle
   * @param selectFirst
   * @return
   * @paramx kind
   * @paramx kindCollectionMap
   * @see #showProjects(DivWidget, DivWidget)
   */
  private ProjectContainer<ProjectInfo> showProjectList(
      DivWidget content,
      List<ProjectInfo> projectInfos,
      DivWidget userForm,
      String rowHeader,
      String listTitle,
      ProjectStatus status, boolean selectFirst) {
    List<ProjectInfo> filtered = projectInfos.stream()
        .filter(p -> p.getStatus() == status).collect(Collectors.toList());

    ProjectContainer<ProjectInfo> projectContainer = getProjectContainer(content, filtered, userForm, rowHeader, listTitle, selectFirst);
    //  this.projectContainer = projectContainer;
    projectContainers.add(projectContainer);
    return projectContainer;
  }

  private final List<ProjectContainer<?>> projectContainers = new ArrayList<>();

  public void clearOthers(ProjectContainer<?> current) {
    //logger.info("clearOthers of " + current);
    for (ProjectContainer<?> container : projectContainers) {
      if (current != container) {
        //  logger.info("\tclear " + container);
        container.clearSelection();
      }
    }
  }

  @NotNull
  private ProjectContainer<ProjectInfo> getProjectContainer(DivWidget content,
                                                            List<ProjectInfo> projectInfos,
                                                            DivWidget userForm,
                                                            String rowHeader,
                                                            String listTitle,
                                                            boolean selectFirst) {
    ProjectContainer<ProjectInfo> projectContainer =
        getProjectContainer(rowHeader, userForm, selectFirst);
    content.clear();
    content.add(projectContainer.getTable(projectInfos, listTitle, ""));
    return projectContainer;
  }

  private ProjectContainer<ProjectInfo> getProjectContainer(String rowHeader, DivWidget rightSide,
                                                            boolean selectFirst) {
    return new ProjectContainer<>(
        controller,
        rowHeader,
        rightSide,
        this,
        selectFirst);
  }

  @Override
  public void onResize() {
    if (requiresResize != null) {
      requiresResize.onResize();
    }
  }
}
