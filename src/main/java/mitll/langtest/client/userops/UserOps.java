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

package mitll.langtest.client.userops;

import com.github.gwtbootstrap.client.ui.NavHeader;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.NavList;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RequiresResize;
import mitll.langtest.client.custom.Navigation;
import mitll.langtest.client.custom.tabs.TabAndContent;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.user.MiniUser;
import mitll.langtest.shared.user.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Deprecated
public class UserOps implements RequiresResize {
  public static final String USERS = "Users";
  // private final Logger logger = Logger.getLogger("UserOps");
  private final UserManager userManager;
  private final ExerciseController controller;

  private final Map<User.Kind, Label> kindToLabel = new HashMap<>();
  private final Map<String, Label> inviteToLabel = new HashMap<>();
  private final Map<User.Kind, IconType> kindToIcon = new HashMap<>();

  /**
   * @param controller
   * @param userManager
   * @see Navigation#addUserMaintenance
   */
  public UserOps(ExerciseController controller, UserManager userManager) {
    setKindToIcon();
    this.controller = controller;
    this.userManager = userManager;
  }

  private void setKindToIcon() {
    kindToIcon.put(User.Kind.STUDENT, IconType.USER);
    kindToIcon.put(User.Kind.TEACHER, IconType.GROUP);
    kindToIcon.put(User.Kind.CONTENT_DEVELOPER, IconType.PENCIL);
    kindToIcon.put(User.Kind.AUDIO_RECORDER, IconType.MICROPHONE);
    kindToIcon.put(User.Kind.PROJECT_ADMIN, IconType.BOLT);
    kindToIcon.put(User.Kind.ADMIN, IconType.ANDROID);
    kindToIcon.put(User.Kind.TEST, IconType.COGS);
  }

  /**
   * @param users
   * @see Navigation#addUserMaintenance()
   * @see Navigation#selectPreviousTab
   */
  public void showUsers(TabAndContent users) {
    DivWidget content = users.getContent();
    content.clear();

    DivWidget left = addDiv(content);
    DivWidget right = addDiv(content);
    DivWidget detail = addDiv(content);

    NavLink first = getKinds(left, right, detail);

    right.getElement().setId("userContent");
    right.getElement().getStyle().setMarginLeft(10, Style.Unit.PX);
//    logger.info("Loaded everything...");

    showInitialState(right, detail, first);
  }

  private NavLink lastClicked = null;

  private void showInitialState(final DivWidget right, final DivWidget detail, NavLink first) {
    final NavLink toClick = first;
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        if (toClick != null) {
          showUsers(User.Kind.STUDENT, right, detail);
          toClick.setActive(true);
          lastClicked = toClick;
        }
      }
    });
  }

  /**
   * @param left
   * @param right
   * @param detail
   * @return
   * @see #showUsers
   */
  private NavLink getKinds(DivWidget left, DivWidget right, DivWidget detail) {
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
  }

  private NavLink addRoleLinks(DivWidget right, DivWidget detail, NavList kindsLinks, User current, User.Kind loggedInUserRole) {
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
  }

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
  private NavLink getUserLink(User.Kind kind, DivWidget content, DivWidget userForm) {
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
        showUsers(kind, content, userForm);
        students.setActive(true);
        lastClicked = students;
      }
    });
    return students;
  }

  private NavLink getInviteLink(DivWidget content, DivWidget pendingInvites) {
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
        //showUsers(kind, content, userForm);
        showInvitations(true, content);
        students.setActive(true);
        lastClicked = students;
      }
    });
    return students;
  }

  private User.Kind currentKind;
  private DivWidget currentContent;
  private DivWidget currentUserForm;

  public void reload() {
/*    MiniUser currentSelection = opsUserContainer.getNext();
    if (currentSelection != null) {
      opsUserContainer.storeSelectedUser(currentSelection.getID());
    }
    showUsers(currentKind, currentContent, currentUserForm);*/
  }

  /**
   * Talk to the service.
   *
   * @param kind
   * @param content
   * @param userForm
   * @see
   */
  private void showUsers(final User.Kind kind, final DivWidget content, DivWidget userForm) {
    userManager.getUserService().getKindToUser(new AsyncCallback<Map<User.Kind, Collection<MiniUser>>>() {
      @Override
      public void onFailure(Throwable throwable) {

      }

      @Override
      public void onSuccess(Map<User.Kind, Collection<MiniUser>> kindCollectionMap) {
        currentKind = kind;
        currentContent = content;
        currentUserForm = userForm;

      //  userManager.getCounts(kindToLabel);

        requiresResize = showUserList(kind, content, kindCollectionMap, userForm);

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
          @Override
          public void execute() {
            onResize();
          }
        });
      }
    });
  }

  /**
   * Invites are sent to recipient email, then they click on a link taking them to an invite sign up page
   * when they sign up, the invitation gets marked accepted, and the new user id is marked on the invitation.
   * <p>
   * Either pending or accepted
   *
   * @param pending
   */
  private void showInvitations(boolean pending, final DivWidget content) {
 /*   userManager.getUserService().getPending(
        userManager.getCurrent().getUserKind(),
        new AsyncCallback<Collection<Invitation>>() {
          @Override
          public void onFailure(Throwable throwable) {

          }

          @Override
          public void onSuccess(Collection<Invitation> invitations) {
          //  userManager.getInvitationCounts(inviteToLabel);
            content.clear();
            Button w = new Button("Invite New User", IconType.ENVELOPE);
            w.addClickHandler(new ClickHandler() {
              @Override
              public void onClick(ClickEvent clickEvent) {

                User.Kind inviteRole = User.Kind.STUDENT;

                String inviteEmail = "gordon.vidaver@ll.mit.edu";

                userManager.getUserService().invite(Window.Location.getHref(),
                    new Invitation(inviteRole,
                        userManager.getUser(), System.currentTimeMillis(), inviteEmail),
                    new AsyncCallback<Void>() {
                      @Override
                      public void onFailure(Throwable throwable) {

                      }

                      @Override
                      public void onSuccess(Void aVoid) {
                        new ModalInfoDialog("Success!", "User invited!");
                      }
                    });
              }
            });
            content.add(w);
          }
        });*/
  }

  private RequiresResize requiresResize = null;
  private OpsUserContainer opsUserContainer;

  /**
   * Show users of this kind.
   *
   * @param kind
   * @param content
   * @param kindCollectionMap
   * @param userForm
   * @return
   */
  private RequiresResize showUserList(User.Kind kind,
                                      DivWidget content,
                                      Map<User.Kind, Collection<MiniUser>> kindCollectionMap,
                                      DivWidget userForm) {
    content.clear();
    Collection<MiniUser> miniUsers = kindCollectionMap.get(kind);
    if (miniUsers == null) miniUsers = new ArrayList<>();

    opsUserContainer = getOpsUserContainer(kind, userForm);
 /*   DivWidget table = opsUserContainer.getTable(miniUsers, "", "");
    content.add(table);*/
    return opsUserContainer;
  }

  private OpsUserContainer getOpsUserContainer(User.Kind kind, DivWidget rightSide) {
    return new OpsUserContainer(controller,
        kind.getName(),
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
