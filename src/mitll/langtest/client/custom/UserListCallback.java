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
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.HasID;

import java.util.*;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 6/5/2014.
 */
class UserListCallback implements AsyncCallback<Collection<UserList<CommonShell>>> {
  private final Logger logger = Logger.getLogger("UserListCallback");

  private static final String NO_LISTS_CREATED_YET = "No lists created yet.";
  private static final String NO_LISTS_CREATED_OR_VISITED_YET = "No lists created or visited yet.";
  private static final String NO_LISTS_YET = "No lists created yet that you haven't seen.";
  private static final String DELETE = "Delete";
  private static final String REVIEWERS = "Reviewers";

  private final ListManager listManager;
  private final Panel contentPanel;
  private final Panel insideContentPanel;
  private final ScrollPanel listScrollPanel;
  private final boolean allLists;
  private final String instanceName;
  private final boolean onlyMyLists;
  private final UserManager userManager;
  private final boolean showIsPublic;
  private final String optionalExercise;

  private  boolean DEBUG = false;

  /**
   * @param contentPanel
   * @param insideContentPanel
   * @param listScrollPanel
   * @param instanceName
   * @param onlyMyLists
   * @param allLists
   * @param userManager
   * @param showIsPublic
   * @param optionalExercise
   * @see ListManager#viewLessons
   */
  UserListCallback(ListManager listManager,
                   Panel contentPanel,
                   Panel insideContentPanel,
                   ScrollPanel listScrollPanel,
                   String instanceName,
                   boolean onlyMyLists,
                   boolean allLists,
                   UserManager userManager,
                   boolean showIsPublic,
                   String optionalExercise) {
    //logger.info("UserListCallback instance '" + instanceName + "' only my lists " + onlyMyLists);
    this.listManager = listManager;
    this.contentPanel = contentPanel;
    this.insideContentPanel = insideContentPanel;
    this.listScrollPanel = listScrollPanel;
    this.instanceName = instanceName;
    this.onlyMyLists = onlyMyLists;
    this.allLists = allLists;
    this.userManager = userManager;
    this.showIsPublic = showIsPublic;
    this.optionalExercise = optionalExercise;
  }

  @Override
  public void onFailure(Throwable caught) {
  }

  @Override
  public void onSuccess(final Collection<UserList<CommonShell>> result) {
    if (DEBUG)
      logger.info("\tUserListCallback.onSuccess : Displaying " + result.size() + " user lists for " + instanceName);
    if (result.isEmpty()) {
      //  logger.info("\t\tUserListCallback.onSuccess : Displaying empty set");

      insideContentPanel.clear();
      listScrollPanel.clear();
      insideContentPanel.add(getNoListsCreated());
    } else {
      if (DEBUG)
        logger.info("\t\tUserListCallback.onSuccess : Displaying " + result.size() + " user lists for " + instanceName);
      listScrollPanel.getElement().setId("scrollPanel");

      setScrollPanelWidth(listScrollPanel);

      final Panel insideScroll = new DivWidget();
      insideScroll.getElement().setId("insideScroll");
      insideScroll.addStyleName("userListContainer");
      listScrollPanel.add(insideScroll);

      Map<String, List<UserList<CommonShell>>> nameToLists = populateNameToList(result);

      boolean anyAdded = addUserListsToDisplay(result, insideScroll, nameToLists);
      if (!anyAdded) {
        insideScroll.add(new Heading(3, allLists ? NO_LISTS_CREATED_OR_VISITED_YET : NO_LISTS_CREATED_YET));
      }
      insideContentPanel.add(listScrollPanel);

      if (!optionalExercise.isEmpty()) {
        if (DEBUG) logger.info("onSuccess find list for " + optionalExercise);
        for (UserList<? extends HasID> ul : result) {
          for (HasID ex : ul.getExercises()) {
            if (ex.getID().equals(optionalExercise)) {
              if (DEBUG) logger.info("onSuccess ex " + optionalExercise + " is on " + ul);
              listManager.showList(ul, contentPanel, instanceName, ex);
              break;
            }
          }
        }
      } else {
        selectPreviousList(result);
      }
    }
  }

  private Heading getNoListsCreated() {
    return new Heading(3, allLists ? NO_LISTS_YET : NO_LISTS_CREATED_YET);
  }

  /**
   * @param result
   * @see #onSuccess(java.util.Collection)
   */
  private void selectPreviousList(Collection<UserList<CommonShell>> result) {
    String clickedUserList = listManager.getStorage().getValue(Navigation.CLICKED_USER_LIST);
    if (clickedUserList != null && !clickedUserList.isEmpty()) {
      showList(result, Long.parseLong(clickedUserList));
    }
  }

  private void showList(Collection<UserList<CommonShell>> result, long id) {
    for (UserList ul : result) {
      if (ul.getUniqueID() == id) {
        listManager.showList(ul, contentPanel, instanceName, null);
        break;
      }
    }
  }

  private Map<String, List<UserList<CommonShell>>> populateNameToList(Collection<UserList<CommonShell>> result) {
    Map<String, List<UserList<CommonShell>>> nameToLists = new HashMap<>();

    for (final UserList<CommonShell> ul : result) {
      List<UserList<CommonShell>> userLists = nameToLists.get(ul.getName());
      if (userLists == null) nameToLists.put(ul.getName(), userLists = new ArrayList<>());
      userLists.add(ul);
    }
//    logger.info("populateNameToList " +nameToLists);
    return nameToLists;
  }

  /**
   * @param result
   * @param insideScroll
   * @param nameToLists
   * @return
   * @see #onSuccess(java.util.Collection)
   */
  private boolean addUserListsToDisplay(Collection<UserList<CommonShell>> result, Panel insideScroll,
                                        Map<String, List<UserList<CommonShell>>> nameToLists) {
    boolean anyAdded = false;
    for (final UserList<CommonShell> ul : result) {
      List<UserList<CommonShell>> collisions = nameToLists.get(ul.getName());
      boolean showMore = false;
      if (collisions.size() > 1) {
        if (collisions.indexOf(ul) > 0) showMore = true;
      }
      if (!ul.isEmpty() || createdByYou(ul) || (!ul.isPrivate())) {
        anyAdded = true;
        insideScroll.add(getDisplayRowPerList(ul, showMore, onlyMyLists));
      } else {
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
   * @param ul
   * @param showMore
   * @param onlyMyLists
   * @return
   * @see UserListCallback#addUserListsToDisplay
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
        listManager.showList(ul, contentPanel, instanceName, null);
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

    boolean yourList = isYourList(ul);
    if (yourList && !onlyMyLists) {
      w.addStyleName("correctCard");
    }

    addWidgetsForList(ul, showMore, w, onlyMyLists, yourList);
    return widgets;
  }

  /**
   * @param ul
   * @param showMore
   * @param container
   * @param onlyMyLists
   * @see #getDisplayRowPerList
   */
  private void addWidgetsForList(final UserList ul, boolean showMore, final Panel container, boolean onlyMyLists, boolean yourList) {
    Panel r1 = new FlowPanel();
    r1.addStyleName("trueInlineStyle");
    String name = ul.getName();
/*    Widget insideContentPanel = makeItemMarker2(ul);
    insideContentPanel.addStyleName("leftFiveMargin");*/

    Heading h4 = new Heading(4, name, ul.getNumItems() + " items");
    h4.addStyleName("floatLeft");
    r1.add(h4);

    boolean empty = ul.getDescription().trim().isEmpty();
    boolean cmempty = ul.getClassMarker().trim().isEmpty();
    String subtext = empty ? "" : ul.getDescription() + (cmempty ? "" : ",");

    if (!empty) {
      h4 = getDescription(subtext);
      r1.add(h4);
    }

    if (!cmempty) {
      h4 = getClassMarker(ul);
      r1.add(h4);
    }

    if (yourList && onlyMyLists) {
      r1.add(getDelete(ul, onlyMyLists));
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
        listManager.setPublic(uniqueID, isPublic.getValue());
      }
    });
    return isPublic;
  }

  /**
   * @param ul
   * @param onlyMyLists
   * @return
   * @see #addWidgetsForList
   */
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
   * @param ul
   * @param onlyMyLists
   * @return
   * @see #addWidgetsForList
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
        listManager.deleteList(delete, ul, onlyMyLists);
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
  private boolean isYourList(UserList ul) {
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