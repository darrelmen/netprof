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
import mitll.langtest.client.custom.tabs.TabAndContent;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.user.MiniUser;
import mitll.langtest.shared.user.User;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class UserOps {
  private final Logger logger = Logger.getLogger("UserOps");

  private static final String USERS = "Users";
  private final UserManager userManager;
  private final ExerciseController controller;

  Map<User.Kind, Label> kindToLabel = new HashMap<>();
  Map<User.Kind, IconType> kindToIcon = new HashMap<>();

/*  private void addUserMaintenance(TabAndContent users) {
   // users = makeFirstLevelTab(tabPanel, IconType.GROUP, USERS);

    users.getTab().addClickHandler(new ClickHandler() {
       @Override
      public void onClick(ClickEvent event) {
        checkAndMaybeClearTab(USERS);
        // learnHelper.showNPF(chapters, LEARN);
        showUsers();


        logEvent(users, USERS);
      }
    });
  }*/

  public UserOps(ExerciseController controller, UserManager userManager) {
    setKindToIcon();
    this.controller = controller;
    this.userManager = userManager;
  }

  private void setKindToIcon() {
    kindToIcon.put(User.Kind.STUDENT, IconType.USER);
    kindToIcon.put(User.Kind.TEACHER, IconType.GROUP);
    kindToIcon.put(User.Kind.CONTENT_DEVELOPER, IconType.PENCIL);
    kindToIcon.put(User.Kind.PROJECT_ADMIN, IconType.BOLT);
    kindToIcon.put(User.Kind.ADMIN, IconType.ANDROID);
  }

  public void showUsers(TabAndContent users) {
    DivWidget content = users.getContent();
    content.clear();

    DivWidget left = addDiv(content);
    DivWidget right = addDiv(content);
    NavLink first = getKinds(left, right);
    userManager.getCounts(kindToLabel);

    right.getElement().setId("userContent");
    right.getElement().getStyle().setMarginLeft(10, Style.Unit.PX);

//    logger.info("Loaded everything...");

    final NavLink toClick = first;
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        if (toClick != null) {
          //showUsers(users);
          showUsers(User.Kind.STUDENT, right);
          toClick.setActive(true);
          lastClicked = toClick;
          //toClick.fireEvent(new Navigation.ButtonClickEvent());
          logger.info("Fire click event on " + toClick.getElement().getId());
        }
      }
    });
  }

  NavLink lastClicked = null;
  private NavLink getKinds(DivWidget left, DivWidget right) {
    NavList kindsLinks = new NavList();
    kindsLinks.setWidth("180px");
    left.add(kindsLinks);

    kindsLinks.add(new NavHeader("Users"));

    NavLink first = null;
    for (User.Kind kind : User.Kind.values()) {
      if (kind.shouldShow()) {
        NavLink userLink = getUserLink(kind, right);
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

  private class ButtonClickEvent extends ClickEvent {
  }

  private NavLink getUserLink(User.Kind kind, DivWidget content) {
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
        showUsers(kind, content);
        students.setActive(true);
      }
    });
    return students;
  }

  private void showUsers(final User.Kind kind, final DivWidget content) {
    userManager.getUserService().getKindToUser(new AsyncCallback<Map<User.Kind, Collection<MiniUser>>>() {
      @Override
      public void onFailure(Throwable throwable) {

      }

      @Override
      public void onSuccess(Map<User.Kind, Collection<MiniUser>> kindCollectionMap) {
        showUserList(kind, content, kindCollectionMap);
      }
    });
  }

  private void showUserList(User.Kind kind,
                            DivWidget content,
                            Map<User.Kind, Collection<MiniUser>> kindCollectionMap) {
    content.clear();
    Collection<MiniUser> miniUsers = kindCollectionMap.get(kind);
    content.add(getUsers(kind, miniUsers));
  }

  private DivWidget getUsers(User.Kind kind, Collection<MiniUser> miniUsers) {
    return new OpsUserContainer(controller, kind.getName()).getTable(miniUsers, "", "");
  }
}
