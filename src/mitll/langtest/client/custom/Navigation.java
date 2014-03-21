package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.TabPanel;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.InlineLabel;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.scoring.GoodwaveExercisePanel;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.User;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 9/27/13
 * Time: 8:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class Navigation extends TabContainer implements RequiresResize {
  private static final String CHAPTERS = "Course Content";
  private static final String YOUR_LISTS = "Your Lists";
  private static final String OTHERS_LISTS = "Other's Lists";
  private static final String PRACTICE = "Practice";
  public static final String REVIEW = "review";
  public static final String COMMENT = "comment";
  private static final String PRACTICE1 = "practice";
  private static final String ADD_OR_EDIT_ITEM = "Add/Edit Item";
  private static final String ADD_DELETE_EDIT_ITEM = "Fix Defects";
  private static final String ITEMS_TO_REVIEW = "Possible defects";
  private static final String ITEMS_WITH_COMMENTS = "Items with comments";
  private static final String LEARN_PRONUNCIATION = "Learn Pronunciation";
  private static final String REVIEW1 = "Defects";
  private static final String REVIEWERS = "Reviewers";
  private static final String CREATE = "Create a New List";
  private static final String BROWSE = "Browse Lists";
  private static final String COMMENTS = "Comments";
  private static final String LESSONS = "lessons";
  private static final String DELETE = "Delete";
  private static final String NO_LISTS_CREATED_YET = "No lists created yet.";
  private static final String CLICKED_USER_LIST = "clickedUserList";
  private static final String CLICKED_TAB = "clickedTab";
  private static final String SUB_TAB = "subTab";
  private static final String NO_LISTS_CREATED_OR_VISITED_YET = "No lists created or visited yet.";

  private static final int YOUR_LIST_INDEX = 0;
  private static final int OTHERS_LIST_INDEX = 1;
  private static final int CREATE_TAB_INDEX = 2;
  private static final int BROWSE_TAB_INDEX = 3;
  private static final int CHAPTERS_TAB = 4;
  private static final int REVIEW_TAB_INDEX = 5;
  private static final int COMMENT_TAB_INDEX = 6;

  private static final String EDIT_ITEM = "editItem";
  private static final String LEARN = "learn";

  private final ExerciseController controller;
  private final LangTestDatabaseAsync service;
  private final UserManager userManager;

  private ScrollPanel listScrollPanel;
  private final ListInterface listInterface;
  private final NPFHelper npfHelper;
  private final NPFHelper avpHelper;
  private final EditItem editItem;
  private final EditItem reviewItem;
  private final KeyStorage storage;

  /**
   *  @see mitll.langtest.client.LangTest#resetClassroomState()
   * @param service
   * @param userManager
   * @param controller
   * @param listInterface
   * @param feedback
   */
  public Navigation(final LangTestDatabaseAsync service, final UserManager userManager,
                    final ExerciseController controller, final ListInterface listInterface,
                    UserFeedback feedback) {
    this.service = service;
    this.userManager = userManager;
    this.controller = controller;
    this.listInterface = listInterface;
    storage = new KeyStorage(controller);
    npfHelper = new NPFHelper(service, feedback, userManager, controller);
    avpHelper = new AVPHelper(service, feedback, userManager, controller);
    editItem = new EditItem(service, userManager, controller, listInterface, feedback, npfHelper);
    reviewItem = new ReviewEditItem(service, userManager, controller, listInterface, feedback, npfHelper);
  }

  /**
   * @return
   * @param secondAndThird
   * @see mitll.langtest.client.LangTest#onModuleLoad2
   */
  public Widget getNav(final Panel secondAndThird) {
    Panel container = new FlowPanel();
    container.getElement().setId("getNav_container");
    Panel buttonRow = getButtonRow2(secondAndThird);
    buttonRow.getElement().setId("getNav_buttonRow");

    container.add(buttonRow);
    this.container = container;
    return container;
  }

  public Widget getContainer() { return container; }

  private Widget container;
  private TabPanel tabPanel;
  private TabAndContent yourStuff, othersStuff;
  private TabAndContent browse;
  private TabAndContent review, commented;

  /**
   * @see #getNav(com.google.gwt.user.client.ui.Panel)
   * @param secondAndThird
   * @return
   */
  protected Panel getButtonRow2(Panel secondAndThird) {
    tabPanel = new TabPanel();

    boolean combinedMode = controller.getProps().isCombinedMode();

    // your list tab
    yourStuff = makeTab(tabPanel, IconType.FOLDER_CLOSE, YOUR_LISTS);
    yourStuff.tab.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        checkAndMaybeClearTab(YOUR_LISTS);
        refreshViewLessons(true, false);
      }
    });

    // visited lists
    othersStuff = makeTab(tabPanel, IconType.FOLDER_CLOSE, OTHERS_LISTS);
    othersStuff.tab.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        checkAndMaybeClearTab(OTHERS_LISTS);
        refreshViewLessons(false, true);
      }
    });

    // create tab
    final TabAndContent create = makeTab(tabPanel, IconType.PLUS_SIGN, CREATE);
    final CreateListDialog createListDialog = new CreateListDialog(this,service,userManager);
    create.tab.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
      //  checkAndMaybeClearTab(CREATE);
        createListDialog.doCreate(create.content);
      }
    });

    // browse tab
    browse = makeTab(tabPanel, IconType.TH_LIST, BROWSE);
    browse.tab.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        checkAndMaybeClearTab(BROWSE);
        viewBrowse();
      }
    });

    // chapter tab
    final TabAndContent chapters = makeTab(tabPanel, combinedMode ? IconType.LIGHTBULB : IconType.TH_LIST, !combinedMode ? CHAPTERS : LEARN_PRONUNCIATION);
    chapters.content.add(secondAndThird);
    chapters.tab.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        checkAndMaybeClearTab(CHAPTERS);
      }
    });

    if (controller.isReviewMode()) {
      review = makeTab(tabPanel, IconType.EDIT, REVIEW1);
      review.tab.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          checkAndMaybeClearTab(REVIEW1);
          viewReview(review.content);
        }
      });

      commented = makeTab(tabPanel, IconType.COMMENT, COMMENTS);
      commented.tab.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          checkAndMaybeClearTab(COMMENTS);
          viewComments(commented.content);
        }
      });
    }

    // so we can know when chapters is revealed and tell it to update it's lists
    tabPanel.addShowHandler(new TabPanel.ShowEvent.Handler() {
      @Override
      public void onShow(TabPanel.ShowEvent showEvent) {
       /* System.out.println("got shown event : '" +showEvent + "'\n" +
            "\ntarget " + showEvent.getTarget()+
            " ' target name '" + showEvent.getTarget().getName() + "'");*/
        String targetName = showEvent.getTarget() == null ? "" : showEvent.getTarget().toString();

        //System.out.println("getButtonRow2 : got shown event : '" +showEvent + "' target '" + targetName + "'");

        boolean wasChapters = targetName.contains(CHAPTERS);
        Panel createdPanel = listInterface.getCreatedPanel();
        boolean hasCreated = createdPanel != null;
        if (hasCreated && wasChapters) {
          //System.out.println("\tgot chapters! created panel :  has created " + hasCreated + " was revealed  " + createdPanel.getClass());
          ((GoodwaveExercisePanel) createdPanel).wasRevealed();
        }
      }
    });

    return tabPanel;    // TODO - consider how to tell panels when they are hidden by tab changes
  }

  private void checkAndMaybeClearTab(String value) {
    String value1 = storage.getValue(CLICKED_TAB);

//      System.out.println("checkAndMaybeClearTab " + value1 + " vs "+value + " clearing " + CLICKED_USER_LIST);
    storage.removeValue(CLICKED_USER_LIST);
    storage.storeValue(CLICKED_TAB, value);
  }

  /**
   * @see #getButtonRow2(com.google.gwt.user.client.ui.Panel)
   * @see #showMyLists(boolean, boolean)
   * @see #makeDeleteButton(mitll.langtest.shared.custom.UserList, boolean)
   * @see #clickOnYourLists(long)
   * @param onlyMine
   * @param onlyVisited
   */
  private void refreshViewLessons(boolean onlyMine, boolean onlyVisited) {
    viewLessons(onlyMine ? yourStuff.content : othersStuff.content, false, onlyMine, onlyVisited);
  }

  /**
   * TODO : streamline this -- there are three requests in sequence...
   * @see mitll.langtest.client.LangTest#showInitialState()
   */
  @Override
  public void showInitialState() {
    final int user = userManager.getUser();
    System.out.println("showInitialState show initial state for " + user +
      " : getting user lists " + controller.isReviewMode());
    String value = storage.getValue(CLICKED_TAB);
    if (value.isEmpty()) {   // no previous tab
      service.getListsForUser(user, true, true, new AsyncCallback<Collection<UserList>>() {
        @Override
        public void onFailure(Throwable caught) {
        }

        @Override
        public void onSuccess(Collection<UserList> result) {
          if (result.size() == 1 && // if only one empty list - one you've created
            result.iterator().next().isEmpty()) {
            tabPanel.selectTab(CHAPTERS_TAB);
          } else {
            boolean foundCreated = false;
            for (UserList ul : result) {
              if (createdByYou(ul)) {
                foundCreated = true;
                break;
              }
            }
            showMyLists(foundCreated, !foundCreated);
          }
        }
      });
    }
    else {
      selectPreviousTab();
    }
  }

  /**
   * @see #showInitialState()
   */
  private void selectPreviousTab() {
    String value = storage.getValue(CLICKED_TAB);
    try {
      if (value.equals(CHAPTERS)) {
        tabPanel.selectTab(CHAPTERS_TAB);
      } else if (value.equals(YOUR_LISTS)) {
        showMyLists(true, false);
      } else if (value.equals(OTHERS_LISTS)) {
        showMyLists(false, true);
      } else if (value.equals(CREATE)) {
        tabPanel.selectTab(CREATE_TAB_INDEX);
      } else if (value.equals(BROWSE)) {
        showBrowse();
      } else if (value.equals(REVIEW1)) {
        tabPanel.selectTab(REVIEW_TAB_INDEX);
        viewReview(review.content);
      } else if (value.equals(COMMENTS)) {
        tabPanel.selectTab(COMMENT_TAB_INDEX);
        viewComments(commented.content);
      } else {
        System.err.println("selectPreviousTab : huh? value is unexpected " + value);
        //showBrowse();
      }
    } catch (Exception e) {
      storage.removeValue(CLICKED_TAB);
      showBrowse();
    }
  }

  /**
   * @see #showInitialState()
   * @param onlyCreated
   * @param onlyVisited
   */
  private void showMyLists(boolean onlyCreated, boolean onlyVisited) {
    String value = storage.getValue(CLICKED_TAB);
    System.out.println("showMyLists " + value + " created " + onlyCreated + " visited " + onlyVisited);
    if (!value.isEmpty()) {
      if (value.equals(YOUR_LISTS)) {
        onlyCreated = true;
        onlyVisited = false;
      }
      else if (value.equals(OTHERS_LISTS)) {
        onlyCreated = false;
        onlyVisited = true;
      }
    }
    int tabToSelect = onlyCreated ? YOUR_LIST_INDEX : OTHERS_LIST_INDEX;
    tabPanel.selectTab(tabToSelect);

    TabAndContent toUse = onlyCreated ? yourStuff : othersStuff;
    clickOnTab(toUse);
    refreshViewLessons(onlyCreated, onlyVisited);
  }

  /**
   * @see mitll.langtest.client.custom.CreateListDialog#addUserList(mitll.langtest.client.user.BasicDialog.FormField, com.github.gwtbootstrap.client.ui.TextArea, mitll.langtest.client.user.BasicDialog.FormField)
   * @param userListID
   */
  public void clickOnYourLists(long userListID) {
    storeCurrentClickedList(userListID);
    storage.storeValue(SUB_TAB, EDIT_ITEM);

    tabPanel.selectTab(YOUR_LIST_INDEX);
    clickOnTab(yourStuff);
    refreshViewLessons(true, false);
  }

  private void clickOnTab(TabAndContent toUse) {
    toUse.tab.fireEvent(new ButtonClickEvent());
  }

  /**
   * @se #showInitialState()
   */
  private void showBrowse() {
    tabPanel.selectTab(BROWSE_TAB_INDEX);
    clickOnTab(browse);
    viewBrowse();
  }

  /*To call click() function for Programmatic equivalent of the user clicking the button.*/
  private class ButtonClickEvent extends ClickEvent {}

  private void viewBrowse() { viewLessons(browse.content, true, false, false); }

  /**
   * @see #viewBrowse()
   * @see #refreshViewLessons(boolean, boolean)
   * @param contentPanel
   * @param getAll
   * @param onlyMine
   * @param onlyVisited
   */
  private void viewLessons(final Panel contentPanel, boolean getAll, boolean onlyMine, boolean onlyVisited) {
    contentPanel.clear();
    contentPanel.getElement().setId("contentPanel");

    final Panel child = new DivWidget();
    contentPanel.add(child);
    child.addStyleName("exerciseBackground");
    listScrollPanel = new ScrollPanel();

    if (getAll) {
     // System.out.println("viewLessons----> getAll = " + getAll);
      service.getUserListsForText("", controller.getUser(),
        new UserListCallback(contentPanel, child, listScrollPanel, LESSONS + "_All", false, true));
    } else {
     // System.out.println("viewLessons for " + userManager.getUser());
      service.getListsForUser(userManager.getUser(), onlyMine,
        onlyVisited,
        new UserListCallback(contentPanel, child, listScrollPanel, LESSONS + (onlyMine ? "_Mine":"_Others"), onlyMine, false));
    }
  }

  /**
   * @see #getButtonRow2(com.google.gwt.user.client.ui.Panel)
   * @param contentPanel
   */
  private void viewReview(final Panel contentPanel) {
    contentPanel.clear();
    contentPanel.getElement().setId("viewReview_contentPanel");

    final Panel child = new DivWidget();
    contentPanel.add(child);
    child.addStyleName("exerciseBackground");
    service.getReviewList(new AsyncCallback<UserList>() {
      @Override
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(UserList result) {
        System.out.println("\tviewReview : reviewLessons for " + userManager.getUser() + " got " + result);

        new UserListCallback(contentPanel, child, new ScrollPanel(), REVIEW, false, false).onSuccess(Collections.singleton(result));
      }
    });
  }

  private void viewComments(final Panel contentPanel) {
    contentPanel.clear();
    contentPanel.getElement().setId("commentReview_contentPanel");

    final Panel child = new DivWidget();
    contentPanel.add(child);
    child.addStyleName("exerciseBackground");
    service.getCommentedList(new AsyncCallback<UserList>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(UserList result) {
        System.out.println("\tviewComments : commented for " + userManager.getUser() + " got " + result);

        new UserListCallback(contentPanel, child, new ScrollPanel(), COMMENT, false, false).onSuccess(Collections.singleton(result));
      }
    });
  }

  @Override
  public void onResize() {
    setScrollPanelWidth(listScrollPanel);
    npfHelper.onResize();
    avpHelper.onResize();
    editItem.onResize();
    reviewItem.onResize();
  }

  private boolean createdByYou(UserList ul) { return ul.getCreator().id == userManager.getUser(); }

  /**
   * @see Navigation.UserListCallback#getDisplayRowPerList(mitll.langtest.shared.custom.UserList, boolean, boolean)
   * @see mitll.langtest.client.custom.Navigation.UserListCallback#selectPreviousList
   * @param ul
   * @param contentPanel
   */
  private void showList(final UserList ul, Panel contentPanel, final String instanceName) {
    System.out.println("showList " +ul + " instance " + instanceName);

    String previousList = storage.getValue(CLICKED_USER_LIST);
    String currentValue = storeCurrentClickedList(ul);

    // if select a new list, clear the subtab selection
    if (previousList != null && !previousList.equals(currentValue)) {
      System.out.println("showList " +previousList + " vs " + currentValue);

      storage.removeValue(SUB_TAB);
    }
    FluidContainer container = new FluidContainer();
    contentPanel.clear();
    contentPanel.add(container);
    container.getElement().setId("showListContainer");
    DOM.setStyleAttribute(container.getElement(), "paddingLeft", "2px");
    DOM.setStyleAttribute(container.getElement(), "paddingRight", "2px");

    FluidRow child = new FluidRow();
    container.add(child);

    FluidRow r1 = new FluidRow();
    child.add(r1);
    child.addStyleName("userListDarkerBlueColor");

    String subtext = ul.getDescription() + " " + ul.getClassMarker();
    Heading widgets = new Heading(1, ul.getName(), subtext);    // TODO : better color for subtext h1->small

    r1.add(widgets);
    widgets.addStyleName("floatLeft");
    widgets.addStyleName("leftFiveMargin");
    HTML itemMarker = new HTML(ul.getExercises().size() + " items");
    listToMarker.put(ul,itemMarker);

    boolean created = createdByYou(ul) || instanceName.equals(REVIEW) || instanceName.equals(COMMENT);

    TabPanel listOperations = getListOperations(ul, created, instanceName);
    container.add(listOperations);

    addVisitor(ul);
  }

  private String storeCurrentClickedList(UserList ul) {
    long uniqueID = ul.getUniqueID();
    return storeCurrentClickedList(uniqueID);
  }

  private String storeCurrentClickedList(long uniqueID) {
    String currentValue = "" + uniqueID;
    storage.storeValue(CLICKED_USER_LIST, currentValue);
    return currentValue;
  }

  /**
   * Avoid marking lists that you have created as also visited by you.
   * @see #showList(mitll.langtest.shared.custom.UserList, com.google.gwt.user.client.ui.Panel, String)
   * @param ul
   */
  private void addVisitor(UserList ul) {
    long user = (long) controller.getUser();
    if (ul.getCreator().id != user) {
      service.addVisitor(ul, user, new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {
        }

        @Override
        public void onSuccess(Void result) {
        }
      });
    }
  }

  /**
   * @see #showList(mitll.langtest.shared.custom.UserList, com.google.gwt.user.client.ui.Panel, String)
   * @param ul
   * @param created
   * @param instanceName
   * @return
   */
  private TabPanel getListOperations(final UserList ul, final boolean created, final String instanceName) {
    final TabPanel tabPanel = new TabPanel();
    System.out.println("getListOperations : '" + instanceName + " for list " +ul);
    final boolean isReview = instanceName.equals(REVIEW);
    final boolean isComment = instanceName.equals(COMMENT);
    final String instanceName1 = isReview ? REVIEW : isComment ? COMMENT : LEARN;

    // add learn tab
    String learnTitle = isReview ? ITEMS_TO_REVIEW : isComment ? ITEMS_WITH_COMMENTS : LEARN_PRONUNCIATION;
    final TabAndContent learn = makeTab(tabPanel, isReview ? IconType.EDIT_SIGN : IconType.LIGHTBULB, learnTitle);
    learn.tab.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        storage.storeValue(SUB_TAB, LEARN);
        npfHelper.showNPF(ul, learn, instanceName1);
      }
    });

    // add practice tab
    final boolean isNormalList = !isReview && !isComment;
    TabAndContent practice = null;
    if (isNormalList) {
      practice = makeTab(tabPanel, IconType.CHECK, PRACTICE);
      final TabAndContent fpractice = practice;
      practice.tab.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          storage.storeValue(SUB_TAB, PRACTICE1);
         // System.out.println("getListOperations : got click on practice");

          avpHelper.showNPF(ul, fpractice, PRACTICE1);
        }
      });
    }
    // add add item and edit tabs (conditionally)
    TabAndContent editItem = null;
    if (created && (!ul.isPrivate() || ul.getCreator().id == controller.getUser())) {
      final TabAndContent edit = makeTab(tabPanel, IconType.EDIT, isReview ? ADD_DELETE_EDIT_ITEM :ADD_OR_EDIT_ITEM);
      editItem = edit;
      edit.tab.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          storage.storeValue(SUB_TAB, EDIT_ITEM);
          EditItem editItem1 = (isReview || isComment) ? reviewItem : Navigation.this.editItem;
          showEditItem(ul, edit, editItem1, isNormalList && !ul.isFavorite(), isReview);
        }
      });
    }

    // select the initial tab -- either add if an empty
    selectInitialTab(ul, created, tabPanel, isReview, isComment, instanceName1, learn,
      practice, editItem, isNormalList, editItem);

    return tabPanel;
  }

  /**
   * @see #getListOperations(mitll.langtest.shared.custom.UserList, boolean, String)
   * @param ul
   * @param created
   * @param tabPanel
   * @param isReview
   * @param isComment
   * @param instanceName1
   * @param learn
   * @param practice
   * @param edit
   * @param isNormalList
   * @param editItem
   */
  private void selectInitialTab(final UserList ul,
                                final boolean created, final TabPanel tabPanel,
                                final boolean isReview, final boolean isComment, final String instanceName1,
                                final TabAndContent learn,
                                final TabAndContent practice,
                                final TabAndContent edit,
                                final boolean isNormalList, TabAndContent editItem) {
    final TabAndContent finalEditItem = editItem;
/*    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        selectTabGivenHistory(tabPanel, learn, practice, edit, ul, instanceName1, isReview, isComment, isNormalList, created, finalEditItem);
      }
    });*/
    selectTabGivenHistory(tabPanel, learn, practice, edit, ul, instanceName1, isReview, isComment, isNormalList, created, finalEditItem);

  }

  private void selectTabGivenHistory(TabPanel tabPanel, TabAndContent learn, TabAndContent practice, TabAndContent edit, UserList ul, String instanceName1, boolean isReview, boolean isComment, boolean isNormalList, boolean created, TabAndContent finalEditItem) {
    boolean chosePrev = selectPreviouslyClickedSubTab(tabPanel, learn, practice, edit,
      ul, instanceName1, isReview, isComment, isNormalList);
    if (!chosePrev) {
      if (created && !ul.isPrivate() && ul.isEmpty() && finalEditItem != null) {
        String name = ul.getName();
        tabPanel.selectTab(name.equals(REVIEW1) ? 0 : CREATE_TAB_INDEX);    // 2 = add/edit item
        EditItem editItem1 = (isReview || isComment) ? reviewItem : this.editItem;
        showEditItem(ul, finalEditItem, editItem1, isNormalList && !ul.isFavorite(), isReview);
      } else {
        tabPanel.selectTab(0);
        npfHelper.showNPF(ul, learn, instanceName1);
      }
    }
  }

  /**
   * @see #selectInitialTab(mitll.langtest.shared.custom.UserList, boolean, com.github.gwtbootstrap.client.ui.TabPanel, boolean, boolean, String, mitll.langtest.client.custom.TabContainer.TabAndContent, mitll.langtest.client.custom.TabContainer.TabAndContent, mitll.langtest.client.custom.TabContainer.TabAndContent, boolean, mitll.langtest.client.custom.TabContainer.TabAndContent)
   * @param tabPanel
   * @param learn
   * @param practice
   * @param edit
   * @param ul
   * @param instanceName1
   * @param isReview
   * @param isComment
   * @param isNormalList
   * @return
   */
  private boolean selectPreviouslyClickedSubTab(TabPanel tabPanel,
                                                TabAndContent learn, TabAndContent practice, TabAndContent edit,
                                                UserList ul, String instanceName1,
                                                boolean isReview, boolean isComment, boolean isNormalList) {
    String subTab = storage.getValue(SUB_TAB);
    //System.out.println("selectPreviouslyClickedSubTab : subtab " + subTab);

    boolean chosePrev = false;
    if (subTab != null) {
      chosePrev = true;
      if (subTab.equals(LEARN)) {
        tabPanel.selectTab(0);
        clickOnTab(learn);
        npfHelper.showNPF(ul, learn, instanceName1);
      } else if (subTab.equals(PRACTICE1)) {
        tabPanel.selectTab(1);
        clickOnTab(practice);
        avpHelper.showNPF(ul, practice, PRACTICE1);
      } else if (subTab.equals(EDIT_ITEM)) {
        boolean reviewOrComment = isReview || isComment;
        tabPanel.selectTab(reviewOrComment ? 1 : CREATE_TAB_INDEX);
        clickOnTab(edit);
        showEditItem(ul, edit, reviewOrComment ? reviewItem : this.editItem, isNormalList && !ul.isFavorite(), isReview);
      } else chosePrev = false;
    }
    return chosePrev;
  }

  /**
   * @see #getListOperations(mitll.langtest.shared.custom.UserList, boolean, String)
   * @param ul
   * @param addItem
   */
  private void showEditItem(UserList ul, TabAndContent addItem, EditItem editItem,
                            boolean includeAddItem, boolean isUserReviewer) {
    //System.out.println("showEditItem --- " + ul + " : " + includeAddItem  + " reviewer " + isUserReviewer);
    addItem.content.clear();
    Widget widgets = editItem.editItem(ul, listToMarker.get(ul), includeAddItem/*, isUserReviewer*/);
    addItem.content.add(widgets);
  }

  private void setScrollPanelWidth(ScrollPanel row) {
    if (row != null) {
      //row.setWidth((Window.getClientWidth() * 0.95) + "px");
      row.setHeight((Window.getClientHeight() * 0.7) + "px");
    }
  }

  private class UserListCallback implements AsyncCallback<Collection<UserList>> {
    private final Panel contentPanel;
    private final Panel child;
    private final ScrollPanel listScrollPanel;
    private final boolean allLists;
    private final String instanceName;
    final boolean onlyMyLists;

    /**
     * @see #viewComments(com.google.gwt.user.client.ui.Panel)
     * @see #viewLessons(com.google.gwt.user.client.ui.Panel, boolean, boolean, boolean)
     * @see #viewReview(com.google.gwt.user.client.ui.Panel)
     * @param contentPanel
     * @param child
     * @param listScrollPanel
     * @param instanceName
     * @param onlyMyLists
     * @param allLists
     */
    public UserListCallback(Panel contentPanel, Panel child, ScrollPanel listScrollPanel, String instanceName,
                            boolean onlyMyLists, boolean allLists) {
      this.contentPanel = contentPanel;
      this.child = child;
      this.listScrollPanel = listScrollPanel;
      this.instanceName = instanceName;
      this.onlyMyLists = onlyMyLists;
      this.allLists = allLists;
    }

    @Override
    public void onFailure(Throwable caught) {}

    @Override
    public void onSuccess(final Collection<UserList> result) {
      //System.out.println("\tUserListCallback : Displaying " + result.size() + " user lists for " + instanceName);
      if (result.isEmpty()) {
        child.add(new Heading(3, allLists ? "No lists created yet that you haven't seen.":NO_LISTS_CREATED_YET));
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
          insideScroll.add(new Heading(3, allLists ? NO_LISTS_CREATED_OR_VISITED_YET:NO_LISTS_CREATED_YET));
        }
        child.add(listScrollPanel);

        selectPreviousList(result);
      }
    }

    private void selectPreviousList(Collection<UserList> result) {
      String clickedUserList = storage.getValue(CLICKED_USER_LIST);
      if (clickedUserList != null && !clickedUserList.isEmpty()) {
        long id = Long.parseLong(clickedUserList);
        for (UserList ul : result) {
           if (ul.getUniqueID() == id) {
             showList(ul,contentPanel,instanceName);
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

    private boolean addUserListsToDisplay(Collection<UserList> result, Panel insideScroll, Map<String, List<UserList>> nameToLists) {
      boolean anyAdded = false;
      for (final UserList ul : result) {
        List<UserList> collisions = nameToLists.get(ul.getName());
        boolean showMore = false;
        if (collisions.size() > 1) {
          if (collisions.indexOf(ul) > 0) showMore = true;
        }
        if (!ul.isEmpty() || (!ul.isPrivate()) ) {
          anyAdded = true;
          insideScroll.add(getDisplayRowPerList(ul, showMore, onlyMyLists));
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
    * @see mitll.langtest.client.custom.Navigation.UserListCallback#onSuccess(java.util.Collection)
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
          showList(ul, contentPanel, instanceName);
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
  }

  private final Map<UserList, HasText> listToMarker = new HashMap<UserList, HasText>();

  /**
   * @see mitll.langtest.client.custom.Navigation.UserListCallback#getDisplayRowPerList(mitll.langtest.shared.custom.UserList, boolean, boolean)
   * @param ul
   * @param showMore
   * @param container
   * @param onlyMyLists
   */
  private void addWidgetsForList(final UserList ul, boolean showMore, final Panel container, boolean onlyMyLists) {
    Panel r1 = new FlowPanel();
    r1.addStyleName("trueInlineStyle");
    String name = ul.getName();
    Widget child = makeItemMarker2(ul);
    child.addStyleName("leftFiveMargin");

    Heading h4 = new Heading(4,name,ul.getExercises().size() + " items");
    h4.addStyleName("floatLeft");
    r1.add(h4);

    boolean empty = ul.getDescription().trim().isEmpty();
    boolean cmempty = ul.getClassMarker().trim().isEmpty();
    String subtext = empty ? "" : ul.getDescription() + (cmempty ? "":",");

    if (!empty) {
      h4 = new Heading(4, "", subtext);
      h4.addStyleName("floatLeft");
      h4.addStyleName("leftFiveMargin");
      h4.getElement().setId("desc");

      r1.add(h4);
    }

    if (!cmempty) {
      String classMarker = ul.getClassMarker();
      h4 = new Heading(4, "", classMarker);
      h4.addStyleName("floatLeft");
      h4.addStyleName("leftFiveMargin");
      h4.getElement().setId("course");

      r1.add(h4);
    }

    boolean yourList = isYourList(ul);
    if (yourList) {
      Button deleteButton = makeDeleteButton(ul, onlyMyLists);
      deleteButton.addStyleName("floatRight");
      deleteButton.addStyleName("leftFiveMargin");
      r1.add(deleteButton);
    }

    if (!ul.isFavorite()) {
      String html1 = "by " +  (ul.getCreator().userID.equals(User.NOT_SET) ? REVIEWERS : ul.getCreator().userID);
      Heading h4Again;
      if (yourList) {
        h4Again = new Heading(5,html1);
      }
      else {
        h4Again = new Heading(4,"",html1);
      }

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

  private Widget makeItemMarker2(UserList ul) {
    InlineLabel itemMarker = new InlineLabel(ul.getExercises().size() + " items");
    itemMarker.addStyleName("numItemFont");
    listToMarker.put(ul, itemMarker);
    return itemMarker;
  }

  /**
   * @see #addWidgetsForList(mitll.langtest.shared.custom.UserList, boolean, com.google.gwt.user.client.ui.Panel, boolean)
   * @param ul
   * @param onlyMyLists
   * @return
   */
  private Button makeDeleteButton(final UserList ul, final boolean onlyMyLists) {
    Button delete = new Button(DELETE);
    delete.addStyleName("topMargin");
    DOM.setStyleAttribute(delete.getElement(), "marginBottom", "5px");

    delete.setType(ButtonType.WARNING);
    delete.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(final ClickEvent event) {
        service.deleteList(ul.getUniqueID(), new AsyncCallback<Boolean>() {
          @Override
          public void onFailure(Throwable caught) {}

          @Override
          public void onSuccess(Boolean result) {
            if (result) {
              refreshViewLessons(onlyMyLists, false);
            }
            else {
              System.err.println("---> did not do deleteList " + ul.getUniqueID());
            }
          }
        });
      }
    });
    return delete;
  }

  private boolean isYourList(UserList ul) {
    return createdByYou(ul) && !ul.getName().equals(UserList.MY_LIST);
  }

  private Widget getUserListText2(String content) {
    Widget nameInfo = new HTML(content);
    nameInfo.addStyleName("userListFont2");
    return nameInfo;
  }
}
