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

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RequiresResize;
import mitll.langtest.client.custom.Navigation;
import mitll.langtest.client.custom.tabs.TabAndContent;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.services.ProjectService;
import mitll.langtest.client.services.ProjectServiceAsync;
import mitll.langtest.shared.project.ProjectInfo;

import java.util.List;

public class ProjectOps implements RequiresResize {
  public static final String USERS = "Users";
  // private final Logger logger = Logger.getLogger("UserOps");
//  private final UserManager userManager;
  private final ExerciseController controller;

//  private final Map<User.Kind, Label> kindToLabel = new HashMap<>();
//  private final Map<String, Label> inviteToLabel = new HashMap<>();
  // private final Map<User.Kind, IconType> kindToIcon = new HashMap<>();

  private final ProjectServiceAsync projectServiceAsync = GWT.create(ProjectService.class);

  /**
   * @param controller
   * @param userManager
   * @see Navigation#addUserMaintenance
   */
  public ProjectOps(ExerciseController controller/*, UserManager userManager*/) {
    // setKindToIcon();
    this.controller = controller;
    // this.userManager = userManager;
  }

  /**
   * @param tabAndContent
   * @see Navigation#addProjectMaintenance
   * @see Navigation#selectPreviousTab
   */
  public void show(TabAndContent tabAndContent) {
    DivWidget content = tabAndContent.getContent();
    content.clear();


    DivWidget left  = addDiv(content);

    left.getElement().setId("productionProjects");
    left.getElement().getStyle().setMarginLeft(10, Style.Unit.PX);

    DivWidget right = addDiv(content);

    DivWidget detail = addDiv(content);

    //  NavLink first = getKinds(left, right, detail);
    right.getElement().setId("projectContent");
    right.getElement().getStyle().setMarginLeft(10, Style.Unit.PX);
//    logger.info("Loaded everything...");
    showInitialState(left, detail/*, first*/);
  }

  private NavLink lastClicked = null;

  private void showInitialState(final DivWidget left, final DivWidget detail/*, NavLink first*/) {
    // final NavLink toClick = first;
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        //   if (toClick != null) {
        showProjects(//User.Kind.STUDENT,
            left, detail);
        //   toClick.setActive(true);
        //   lastClicked = toClick;
        // }
      }
    });
  }

  /**
   * @param left
   * @param right
   * @param detail
   * @return
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
   * @param kind
   * @param content
   * @param userForm
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

//  private User.Kind currentKind;
  private DivWidget currentContent;
  private DivWidget currentUserForm;

  public void reload() {
/*
    MiniUser currentSelection = projectContainer.getNext();
    if (currentSelection != null) {
      projectContainer.storeSelectedUser(currentSelection.getID());
    }
*/
    showProjects(/*currentKind,*/ currentContent, currentUserForm);
  }

  /**
   * Talk to the service.
   *
   * @param kind
   * @param content
   * @param userForm
   * @see
   */
  private void showProjects(//final User.Kind kind,
                            final DivWidget content, DivWidget userForm) {

    projectServiceAsync.getAll(new AsyncCallback<List<ProjectInfo>>() {
      @Override
      public void onFailure(Throwable throwable) {

      }

      @Override
      public void onSuccess(List<ProjectInfo> projectInfos) {
        requiresResize = showProjectList(content, projectInfos, userForm);

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
          @Override
          public void execute() {
            onResize();
          }
        });
      }
    });
  }


  private RequiresResize requiresResize = null;
  private ProjectContainer<ProjectInfo> projectContainer;

  /**
   * Show users of this kind.
   *
   * @param kind
   * @param content
   * @param kindCollectionMap
   * @param userForm
   * @return
   */
  private RequiresResize showProjectList(
      DivWidget content,
      List<ProjectInfo> projectInfos,
      DivWidget userForm) {
    content.clear();

    projectContainer = getProjectContainer(userForm);
    DivWidget table = projectContainer.getTable(projectInfos, "", "");

    Heading production = new Heading(3, "Production");
    content.add(production);
    content.add(table);
    return projectContainer;
  }

  private ProjectContainer<ProjectInfo> getProjectContainer(DivWidget rightSide) {
    return new ProjectContainer<>(
        controller,
        "Projects",
        rightSide,
        this
    );
  }

  @Override
  public void onResize() {
    if (requiresResize != null) {
      requiresResize.onResize();
    }
  }
}
