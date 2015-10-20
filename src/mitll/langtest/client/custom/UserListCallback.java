package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.server.database.custom.UserListManager;
import mitll.langtest.shared.custom.UserList;

import java.util.*;
import java.util.logging.Logger;

/**
* Created by GO22670 on 6/5/2014.
*/
class UserListCallback implements AsyncCallback<Collection<UserList>> {
  private final Logger logger = Logger.getLogger("UserListCallback");

  private static final String NO_LISTS_CREATED_YET = "No lists created yet.";
  private static final String NO_LISTS_CREATED_OR_VISITED_YET = "No lists created or visited yet.";
  private static final String NO_LISTS_YET = "No lists created yet that you haven't seen.";
  private static final String DELETE = "Delete";
  private static final String REVIEWERS = "Reviewers";

  private final ListManager navigation;
  private final Panel contentPanel;
  private final Panel child;
  private final ScrollPanel listScrollPanel;
  private final boolean allLists;
  private final String instanceName;
  final boolean onlyMyLists;
  final UserManager userManager;
  final boolean showIsPublic;

  /**
   * @see ListManager#viewLessons(com.google.gwt.user.client.ui.Panel, boolean, boolean, boolean)
   * @see ListManager#viewReview(com.google.gwt.user.client.ui.Panel)
   * @param contentPanel
   * @param child
   * @param listScrollPanel
   * @param instanceName
   * @param onlyMyLists
   * @param allLists
   * @param userManager
   * @param showIsPublic
   */
  public UserListCallback(ListManager navigation, Panel contentPanel, Panel child, ScrollPanel listScrollPanel, String instanceName,
                          boolean onlyMyLists, boolean allLists, UserManager userManager, boolean showIsPublic) {
    this.navigation = navigation;
    logger.info("UserListCallback instance '" +instanceName + "' only my lists " + onlyMyLists);
    this.contentPanel = contentPanel;
    this.child = child;
    this.listScrollPanel = listScrollPanel;
    this.instanceName = instanceName;
    this.onlyMyLists = onlyMyLists;
    this.allLists = allLists;
    this.userManager = userManager;
    this.showIsPublic = showIsPublic;
  }

  @Override
  public void onFailure(Throwable caught) {}

  @Override
  public void onSuccess(final Collection<UserList> result) {
    logger.info("\tUserListCallback : Displaying " + result.size() + " user lists for " + instanceName);
    if (result.isEmpty()) {
      child.add(new Heading(3, allLists ? NO_LISTS_YET : NO_LISTS_CREATED_YET));
    } else {
      listScrollPanel.getElement().setId("scrollPanel");

      setScrollPanelWidth(listScrollPanel);

      final Panel insideScroll = new DivWidget();
      insideScroll.getElement().setId("insideScroll");
      insideScroll.addStyleName("userListContainer");
      listScrollPanel.add(insideScroll);

      Map<String, List<UserList>> nameToLists = populateNameToList(result);

      boolean anyAdded = addUserListsToDisplay(result, insideScroll, nameToLists);
      if (!anyAdded) {
        insideScroll.add(new Heading(3, allLists ? NO_LISTS_CREATED_OR_VISITED_YET: NO_LISTS_CREATED_YET));
      }
      child.add(listScrollPanel);

      selectPreviousList(result);
    }
  }

  /**
   * @see #onSuccess(java.util.Collection)
   * @param result
   */
  private void selectPreviousList(Collection<UserList> result) {
    String clickedUserList = navigation.getStorage().getValue(Navigation.CLICKED_USER_LIST);
    if (clickedUserList != null && !clickedUserList.isEmpty()) {
      long id = Long.parseLong(clickedUserList);
      for (UserList ul : result) {
         if (ul.getUniqueID() == id) {
           navigation.showList(ul, contentPanel, instanceName);
           break;
         }
      }
    }
  }

  private Map<String, List<UserList>> populateNameToList(Collection<UserList> result) {
    Map<String,List<UserList>> nameToLists = new HashMap<String, List<UserList>>();

    for (final UserList ul : result) {
      List<UserList> userLists = nameToLists.get(ul.getName());
      if (userLists == null) nameToLists.put(ul.getName(), userLists = new ArrayList<UserList>());
      userLists.add(ul);
    }
    return nameToLists;
  }

  /**
   * @see #onSuccess(java.util.Collection)
   * @param result
   * @param insideScroll
   * @param nameToLists
   * @return
   */
  private boolean addUserListsToDisplay(Collection<UserList> result, Panel insideScroll, Map<String, List<UserList>> nameToLists) {
    boolean anyAdded = false;
    for (final UserList ul : result) {
      List<UserList> collisions = nameToLists.get(ul.getName());
      boolean showMore = false;
      if (collisions.size() > 1) {
        if (collisions.indexOf(ul) > 0) showMore = true;
      }
      if (!ul.isEmpty() || createdByYou(ul) || (!ul.isPrivate()) ) {
        anyAdded = true;
        insideScroll.add(getDisplayRowPerList(ul, showMore, onlyMyLists));
      }
      else {
        logger.info("skipping " + ul.getName() + " empty " + ul.isEmpty());
      }
    }
    return anyAdded;
  }

  private void setScrollPanelWidth(ScrollPanel row) {
    if (row != null) {
      row.setHeight((Window.getClientHeight() * 0.7) + "px");
    }
  }

 /**
  * When you click on the panel, show the list.
  *
  * @see UserListCallback#addUserListsToDisplay
  * @param ul
  * @param showMore
  * @param onlyMyLists
  * @return
  */
  private Panel getDisplayRowPerList(final UserList ul, boolean showMore, boolean onlyMyLists) {
    final FocusPanel widgets = new FocusPanel();

    widgets.addStyleName("userListContent");
    widgets.addStyleName("userListBackground");
    widgets.addStyleName("leftTenMargin");
    widgets.addStyleName("rightTenMargin");

    widgets.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        navigation.showList(ul, contentPanel, instanceName);
      }
    });
    widgets.addMouseOverHandler(new MouseOverHandler() {
      @Override
      public void onMouseOver(MouseOverEvent event) {
        widgets.removeStyleName("userListBackground");
        widgets.addStyleName("blueBackground");
        widgets.addStyleName("handCursor");
        widgets.getElement().getStyle().setCursor(Style.Cursor.POINTER);
      }
    });
    widgets.addMouseOutHandler(new MouseOutHandler() {
      @Override
      public void onMouseOut(MouseOutEvent event) {
        widgets.removeStyleName("blueBackground");
        widgets.addStyleName("userListBackground");
        widgets.removeStyleName("handCursor");
      }
    });
    FluidContainer w = new FluidContainer();
    widgets.add(w);

    addWidgetsForList(ul, showMore, w, onlyMyLists);
    return widgets;
  }

  /**
   * @see UserListCallback#getDisplayRowPerList
   * @param ul
   * @param showMore
   * @param container
   * @param onlyMyLists
   */
  private void addWidgetsForList(final UserList ul, boolean showMore, final Panel container, boolean onlyMyLists) {
    Panel r1 = new FlowPanel();
    r1.addStyleName("trueInlineStyle");
    String name = ul.getName();
/*    Widget child = makeItemMarker2(ul);
    child.addStyleName("leftFiveMargin");*/

    Heading h4 = new Heading(4,name,ul.getExercises().size() + " items");
    h4.addStyleName("floatLeft");
    r1.add(h4);

    boolean empty = ul.getDescription().trim().isEmpty();
    boolean cmempty = ul.getClassMarker().trim().isEmpty();
    String subtext = empty ? "" : ul.getDescription() + (cmempty ? "":",");

    if (!empty) {
      h4 = getDescription(subtext);

      r1.add(h4);
    }

    if (!cmempty) {
      h4 = getClassMarker(ul);

      r1.add(h4);
    }

    boolean yourList = isYourList(ul);
    if (yourList) {
      Button deleteButton = getDelete(ul, onlyMyLists);
      r1.add(deleteButton);
    }

    if (!ul.isFavorite()) {
      final long uniqueID = ul.getUniqueID();

      if (showIsPublic) {
        r1.add(getIsPublic(ul, uniqueID));
      }
    //  String prefix = showIsPublic ? "" :
      String html1 = //(ul.isPrivate() ? "" : "Public ") +
        " by " +
          (uniqueID == UserListManager.COMMENT_MAGIC_ID ? "Students" :
            uniqueID == UserListManager.REVIEW_MAGIC_ID ? REVIEWERS :
              ul.getCreator().getUserID());
      Heading h4Again = yourList ? new Heading(5, html1) : new Heading(4, "", html1);

      h4Again.addStyleName("floatRight");
      r1.add(h4Again);
    }

    container.add(r1);

    if (showMore && !ul.getDescription().isEmpty()) {
      Panel r2 = new FluidRow();
      container.add(r2);
      r2.add(getUserListText2(ul.getDescription()));
    }
  }

  private CheckBox getIsPublic(UserList ul, final long uniqueID) {
    final CheckBox isPublic = new CheckBox("Public");
    isPublic.setValue(!ul.isPrivate());
    isPublic.addStyleName("floatRight");
    isPublic.addStyleName("leftFiveMargin");
    isPublic.getElement().getStyle().setMarginTop(10, Style.Unit.PX);
    isPublic.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        event.stopPropagation();
        logger.info("For " + uniqueID + " value " + isPublic.getValue());
        navigation.setPublic(uniqueID, isPublic.getValue());
      }
    });
    return isPublic;
  }

  private Button getDelete(UserList ul, boolean onlyMyLists) {
    Button deleteButton = makeDeleteButton(ul, onlyMyLists);
    deleteButton.addStyleName("floatRight");
    deleteButton.addStyleName("leftFiveMargin");
    return deleteButton;
  }

  private Heading getClassMarker(UserList ul) {
    Heading h4;
    String classMarker = ul.getClassMarker();
    h4 = new Heading(4, "", classMarker);
    h4.addStyleName("floatLeft");
    h4.addStyleName("leftFiveMargin");
    h4.getElement().setId("course");
    return h4;
  }

  private Heading getDescription(String subtext) {
    Heading h4;
    h4 = new Heading(4, "", subtext);
    h4.addStyleName("floatLeft");
    h4.addStyleName("leftFiveMargin");
    h4.getElement().setId("desc");
    return h4;
  }

  /**
   * @see #addWidgetsForList
   * @param ul
   * @param onlyMyLists
   * @return
   */
  private Button makeDeleteButton(final UserList ul, final boolean onlyMyLists) {
    final Button delete = new Button(DELETE);
    delete.getElement().setId("UserList_" + ul.getID() + "_delete");
    delete.addStyleName("topMargin");
    delete.getElement().getStyle().setMarginBottom(5, Style.Unit.PX);

    delete.setType(ButtonType.WARNING);
    delete.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(final ClickEvent event) {
        event.stopPropagation();
        navigation.deleteList(delete, ul, onlyMyLists);
      }
    });
    return delete;
  }

  /**
   * Any list of yours but not your favorites
   *
   * @param ul
   * @return
   */
  private boolean isYourList(UserList ul)   {
    return createdByYou(ul) && !ul.getName().equals(UserList.MY_LIST);
  }
  private boolean createdByYou(UserList ul) {
    return ul.getCreator().getId() == userManager.getUser();
  }

  private Widget getUserListText2(String content) {
    Widget nameInfo = new HTML(content);
    nameInfo.addStyleName("userListFont2");
    return nameInfo;
  }
}
