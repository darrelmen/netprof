package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.InlineLabel;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.ScrollPanel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.content.AVPHelper;
import mitll.langtest.client.custom.content.NPFHelper;
import mitll.langtest.client.custom.content.ReviewItemHelper;
import mitll.langtest.client.custom.dialog.CreateListDialog;
import mitll.langtest.client.custom.dialog.EditItem;
import mitll.langtest.client.custom.tabs.TabAndContent;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.custom.UserList;

import java.util.List;
import java.util.logging.Logger;

/**
 * Created by go22670 on 10/20/15.
 */
public class ListManager implements RequiresResize {
  private final Logger logger = Logger.getLogger("ListManager");

  private final KeyStorage storage;
  private ScrollPanel listScrollPanel;


  private final ExerciseController controller;
  private final LangTestDatabaseAsync service;
  private final UserManager userManager;
  private TabAndContent yourStuff;
  private TabAndContent othersStuff;
  private TabAndContent browse;
  private TabAndContent create;
  private TabPanel subListTabPanel;
  private final NPFHelper npfHelper;
  private final NPFHelper avpHelper;
  private final ReviewItemHelper reviewItem;
  private final EditItem editItem;

  private static final String PRACTICE = "Audio Vocabulary Practice"; //"Audio Flashcards"; // "Do Flashcards";
  private static final String REVIEW = "review";
  private static final String COMMENT = "comment";
  private static final String ATTENTION = "attention";
  private static final String PRACTICE1 = "practice";
  private static final String ADD_OR_EDIT_ITEM = "Add/Edit Item";
  private static final String ADD_DELETE_EDIT_ITEM = "Fix Defects";
  private static final String POSSIBLE_DEFECTS = "Review";
  private static final String ITEMS_WITH_COMMENTS = "Items with comments";
  private static final String LEARN_PRONUNCIATION = "Learn Pronunciation";
  private static final String FIX_DEFECTS = "Fix Defects";

  private static final int SUBTAB_LEARN_INDEX = 0;
  private static final int SUBTAB_PRACTICE_INDEX = 1;
  private static final int SUBTAB_EDIT_INDEX = 2;

  private static final String LEARN = "learn";

  private static final String YOUR_LISTS = "Study Your Lists";
  private static final String STUDY_LISTS = "Study Lists";// and Favorites";
  private static final String OTHERS_LISTS = "Study Visited Lists";
  private static final String CREATE = "Create a New List";
  private static final String BROWSE = "Browse Lists";
  private static final String LESSONS = "lessons";
  private static final String CLICKED_USER_LIST = "clickedUserList";
  private static final String CLICKED_TAB = "clickedTab";
  private static final String SUB_TAB = "subTab";
  private static final String EDIT_ITEM = "editItem";

  public ListManager(final LangTestDatabaseAsync service, final UserManager userManager,
                     final ExerciseController controller,
                     UserFeedback feedback, TabPanel tabPanel) {
    this.service = service;
    this.userManager = userManager;
    this.controller = controller;
    storage = new KeyStorage(controller);

    // browse tab
    //browse = makeTab(tabPanel, IconType.TH_LIST, BROWSE);
    browse = new TabAndContent(tabPanel, IconType.TH_LIST, BROWSE);
    browse.getTab().addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        checkAndMaybeClearTab(BROWSE);
        logEvent(browse, BROWSE);
        viewBrowse();
      }
    });

    npfHelper = new NPFHelper(service, feedback, userManager, controller, false);
    reviewItem = new ReviewItemHelper(service, feedback, userManager, controller, npfHelper.getExerciseList(), npfHelper);
    avpHelper = new AVPHelper(service, feedback, userManager, controller);
    editItem = new EditItem(service, userManager, controller, npfHelper.getExerciseList(), feedback, npfHelper);
  }

  public void addStudyLists(final TabAndContent studyLists) {
    //studyLists = makeFirstLevelTab(tabPanel, IconType.FOLDER_CLOSE, STUDY_LISTS);
    subListTabPanel = new TabPanel();
    studyLists.getContent().add(subListTabPanel);

    addListTabs(subListTabPanel);

    studyLists.getTab().addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        checkAndMaybeClearTab(STUDY_LISTS);
        showFirstUserListTab(subListTabPanel, 0);
        logEvent(studyLists, STUDY_LISTS);
      }
    });
  }


  /**
   * @param w
   * @param tab
   * @see #addTabs()
   * @see #selectPreviousTab(String)
   */
  public void showFirstUserListTab(TabPanel w, int tab) {
    yourStuff.clickOnTab();
    w.selectTab(tab);
    refreshViewLessons(true, false);
  }

  /**
   * @param tabPanel
   * @see #addTabs
   */
  private void addListTabs(TabPanel tabPanel) {
    // your list tab
    yourStuff = makeTab(tabPanel, IconType.FOLDER_CLOSE, YOUR_LISTS);

    yourStuff.getTab().addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        checkAndMaybeClearTab(YOUR_LISTS);
        refreshViewLessons(true, false);
        logEvent(yourStuff, YOUR_LISTS);
      }
    });

    // visited lists
    othersStuff = makeTab(tabPanel, IconType.FOLDER_CLOSE, OTHERS_LISTS);
    othersStuff.getTab().addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        checkAndMaybeClearTab(OTHERS_LISTS);
        refreshViewLessons(false, true);
        logEvent(othersStuff, OTHERS_LISTS);
      }
    });

    // create tab
    create = makeTab(tabPanel, IconType.PLUS_SIGN, CREATE);
    final CreateListDialog createListDialog = new CreateListDialog(this, service, userManager, controller);
    create.getTab().addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        createListDialog.doCreate(create.getContent());
        logEvent(create, CREATE);
      }
    });

    // browse tab
    browse = makeTab(tabPanel, IconType.TH_LIST, BROWSE);
    browse.getTab().addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        checkAndMaybeClearTab(BROWSE);
        logEvent(browse, BROWSE);
        viewBrowse();
      }
    });

  }

  private void logEvent(TabAndContent yourStuff, String context) {
    if (yourStuff != null && yourStuff.getTab() != null) {
      controller.logEvent(yourStuff.getTab().asWidget(), "Tab", "", context);
    }
  }

  private void checkAndMaybeClearTab(String value) {
    //   String value1 = storage.getValue(CLICKED_TAB);
    //logger.info("checkAndMaybeClearTab " + value1 + " vs "+value + " clearing " + CLICKED_USER_LIST);
    storage.removeValue(CLICKED_USER_LIST);
    storage.storeValue(CLICKED_TAB, value);
  }
  /**
   * @param onlyCreated
   * @param onlyVisited
   * @see #showInitialState
   */
  public void showMyLists(boolean onlyCreated, boolean onlyVisited
  //    , TabPanel tabPanel
  ) {
    TabPanel tabPanel = subListTabPanel;
    String value = storage.getValue(CLICKED_TAB);
    //  logger.info("showMyLists '" + value + "' created " + onlyCreated + " visited " + onlyVisited);

    if (!value.isEmpty()) {
      if (value.equals(YOUR_LISTS)) {
        onlyCreated = true;
        onlyVisited = false;
      } else if (value.equals(OTHERS_LISTS)) {
        onlyCreated = false;
        onlyVisited = true;
      }
    }

    int tabToSelect = onlyCreated ? 0 : 1;//getSafeTabIndexFor(YOUR_LISTS) : getSafeTabIndexFor(OTHERS_LISTS);

    logger.info("showMyLists Select tab " + tabToSelect + " only " + onlyCreated);

    tabPanel.selectTab(tabToSelect);

    TabAndContent toUse = onlyCreated ? yourStuff : othersStuff;
    toUse.clickOnTab();
    refreshViewLessons(onlyCreated, onlyVisited);
  }

  /**
   * @param onlyMine
   * @param onlyVisited
   * @see #getNav
   * @see #showMyLists
   * @see #deleteList(com.github.gwtbootstrap.client.ui.Button, mitll.langtest.shared.custom.UserList, boolean)
   * @see #clickOnYourLists(long)
   */
  private void refreshViewLessons(boolean onlyMine, boolean onlyVisited) {
    viewLessons(onlyMine ? yourStuff.getContent() : othersStuff.getContent(), false, onlyMine, onlyVisited);
  }

  public void viewBrowse() {
    viewLessons(browse.getContent(), true, false, false);
  }

  /**
   * @param contentPanel
   * @param getAll
   * @param onlyMine
   * @param onlyVisited
   * @see #viewBrowse()
   * @see #refreshViewLessons(boolean, boolean)
   */
  private void viewLessons(final Panel contentPanel, boolean getAll, boolean onlyMine, boolean onlyVisited) {
    contentPanel.clear();
    contentPanel.getElement().setId("contentPanel");

    final Panel child = new DivWidget();
    contentPanel.add(child);
    child.addStyleName("exerciseBackground");
    listScrollPanel = new ScrollPanel();

    if (getAll) {
      logger.info("viewLessons----> getAll = " + getAll);
      service.getUserListsForText("", controller.getUser(),
          new UserListCallback(this, contentPanel, child, listScrollPanel, LESSONS + "_All", false, true, userManager, onlyMine));
    } else {
      logger.info("viewLessons for " + userManager.getUser());
      service.getListsForUser(userManager.getUser(), onlyMine,
          onlyVisited,
          new UserListCallback(this, contentPanel, child, listScrollPanel,
              LESSONS + (onlyMine ? "_Mine" : "_Others"), onlyMine, false, userManager, onlyMine));
    }
  }

  /**
   * Return all the review lists -- defects (by teachers/QA), comments (by students), and attention LL.
   *
   * @param contentPanel
   * @see #getNav()
   * @see #addTabs
   */
  public void viewReview(final Panel contentPanel) {
    final ListManager outer = this;
    final Panel child = getContentChild(contentPanel);
//    logger.info("------> viewReview : reviewLessons for " + userManager.getUser());
    service.getReviewLists(new AsyncCallback<List<UserList>>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(List<UserList> reviewLists) {
        logger.info("\tviewReview : reviewLessons for " + userManager.getUser() + " got " + reviewLists);

        new UserListCallback(outer, contentPanel, child,
            new ScrollPanel(), REVIEW, false, false, userManager, false).onSuccess(reviewLists);
      }
    });
  }

  /**
   * @param contentPanel
   * @return
   * @see #viewReview
   */
  private Panel getContentChild(Panel contentPanel) {
    contentPanel.clear();
    contentPanel.getElement().setId("defectReview_contentPanel");

    final Panel child = new DivWidget();
    contentPanel.add(child);
    child.addStyleName("exerciseBackground");
    return child;
  }

  /**
   * @param userListID
   * @see mitll.langtest.client.custom.dialog.CreateListDialog#addUserList
   */
  public void clickOnYourLists(long userListID) {
    storeCurrentClickedList(userListID);
    //  checkAndMaybeClearTab(YOUR_LISTS);
    storage.storeValue(SUB_TAB, EDIT_ITEM);
    //  showMyLists(true, false, subListTabPanel);

    subListTabPanel.selectTab(0);
    yourStuff.clickOnTab();
    refreshViewLessons(true, false);
  }

  @Override
  public void onResize() {
    setScrollPanelWidth(listScrollPanel);

    npfHelper.onResize();
    avpHelper.onResize();
    reviewItem.onResize();
    editItem.onResize();
  }
  /**
   * @param row
   * @see #onResize()
   */
  private void setScrollPanelWidth(ScrollPanel row) {
    if (row != null) {
      row.setHeight((Window.getClientHeight() * 0.7) + "px");
    }
  }
  /**
   * @param ul
   * @param contentPanel
   * @see UserListCallback#getDisplayRowPerList(mitll.langtest.shared.custom.UserList, boolean, boolean)
   * @see UserListCallback#selectPreviousList
   */
  void showList(final UserList ul, Panel contentPanel, final String instanceName) {
    logger.info("showList " + ul + " instance '" + instanceName + "'");
    //  if (!ul.isEmpty()) logger.info("\tfirst" + ul.getExercises().iterator().next());
    controller.logEvent(contentPanel, "Tab", "UserList_" + ul.getID(), "Show List");

    String previousList = storage.getValue(CLICKED_USER_LIST);
    String currentValue = storeCurrentClickedList(ul);

    // if select a new list, clear the subtab selection
    if (previousList != null && !previousList.equals(currentValue)) {
      //  logger.info("\tshowList " +previousList + " vs " + currentValue + " remove " + SUB_TAB);

      storage.removeValue(SUB_TAB);
    }
    contentPanel.clear();
    contentPanel.add(makeTabContent(ul, instanceName));

    addVisitor(ul);
  }

  public KeyStorage getStorage() {
    return storage;
  }

  /**
   * @param ul
   * @param instanceName
   * @return
   * @see #showList(mitll.langtest.shared.custom.UserList, com.google.gwt.user.client.ui.Panel, String)
   */
  private Panel makeTabContent(UserList ul, String instanceName) {
    FluidContainer container = new FluidContainer();
    container.getElement().setId("showListContainer");

    container.getElement().getStyle().setPaddingLeft(2, Style.Unit.PX);
    container.getElement().getStyle().setPaddingRight(2, Style.Unit.PX);

    Panel firstRow = new FluidRow();    // TODO : this is wacky -- clean up...
    firstRow.getElement().setId("container_first_row");

    container.add(firstRow);
    firstRow.add(getListInfo(ul));
    firstRow.addStyleName("userListDarkerBlueColor");

    Panel r1 = new FluidRow();
    r1.addStyleName("userListDarkerBlueColor");

    Anchor downloadLink = new DownloadLink(controller).getDownloadLink(ul.getUniqueID(), instanceName + "_" + ul.getUniqueID(), ul.getName());
    Node child = downloadLink.getElement().getChild(0);
    AnchorElement.as(child).getStyle().setColor("#333333");

    r1.add(downloadLink);

    container.add(r1);

    TabPanel listOperations = getListOperations(ul, instanceName);
    container.add(listOperations);
    return container;
  }


  private Panel getListInfo(UserList ul) {
    String subtext = ul.getDescription() + " " + ul.getClassMarker();
    Heading widgets = new Heading(1, ul.getName(), subtext);    // TODO : better color for subtext h1->small

    widgets.addStyleName("floatLeft");
    widgets.addStyleName("leftFiveMargin");
    widgets.getElement().getStyle().setMarginBottom(3, Style.Unit.PX);
    return widgets;
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
   *
   * @param ul
   * @see #showList(mitll.langtest.shared.custom.UserList, com.google.gwt.user.client.ui.Panel, String)
   */
  private void addVisitor(UserList ul) {
    long user = (long) controller.getUser();
    if (ul.getCreator().getId() != user) {
      service.addVisitor(ul.getUniqueID(), user, new AsyncCallback<Void>() {
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
   * @param ul
   * @param instanceName
   * @return
   * @see #showList(mitll.langtest.shared.custom.UserList, com.google.gwt.user.client.ui.Panel, String)
   */
  private TabPanel getListOperations(final UserList ul, final String instanceName) {
    logger.info("getListOperations : '" + instanceName + " for list " + ul);
    boolean created = createdByYou(ul) || instanceName.equals(REVIEW) || instanceName.equals(COMMENT);

    final TabPanel tabPanel = new TabPanel();
    final boolean isReview = instanceName.equals(REVIEW);
    final boolean isComment = instanceName.equals(COMMENT);
    final boolean isAttention = instanceName.equals(ATTENTION);
    final String instanceName1 = isReview ? REVIEW : isComment ? COMMENT : isAttention ? ATTENTION : LEARN;

    // add learn tab
    String learnTitle = isReview ? POSSIBLE_DEFECTS :
        isComment ? ITEMS_WITH_COMMENTS : isAttention ? "Items for LL" : LEARN_PRONUNCIATION;
    final TabAndContent learn = makeTab(tabPanel, isReview ? IconType.EDIT_SIGN : IconType.LIGHTBULB, learnTitle);
    final boolean isNormalList = !isReview && !isComment && !isAttention;
    learn.getTab().addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        controller.logEvent(learn.getTab(), "Tab", "UserList_" + ul.getID(), LEARN);
        storage.storeValue(SUB_TAB, LEARN);
        showLearnTab(learn, ul, instanceName1, isReview, isComment, isAttention);
      }
    });

    // add practice tab
    TabAndContent practice = null;
    if (isNormalList) {
      //logger.info("getListOperations : isNormalList ");

      practice = makeTab(tabPanel, IconType.CHECK, PRACTICE);
      final TabAndContent fpractice = practice;
      practice.getContent().addStyleName("centerPractice");
      practice.getTab().addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          storage.storeValue(SUB_TAB, PRACTICE1);
          logger.info("getListOperations : got click on practice " + fpractice.getContent().getElement().getId());
          avpHelper.setContentPanel(fpractice.getContent());
          avpHelper.showNPF(ul, fpractice, PRACTICE1, true);
          controller.logEvent(fpractice.getTab(), "Tab", "UserList_" + ul.getID(), PRACTICE1);
        }
      });
    }

    // add add item and edit tabs (conditionally)
    TabAndContent editItemTab = null;
    if (created && (!ul.isPrivate() || ul.getCreator().getId() == controller.getUser())) {
      final TabAndContent editTab = makeTab(tabPanel, IconType.EDIT, isReview ? ADD_DELETE_EDIT_ITEM : ADD_OR_EDIT_ITEM);
      editItemTab = editTab;
      //  logger.info("getListOperations : making editTab");

      editTab.getTab().addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          //    logger.info("getListOperations : got click on edit tab ");
          storage.storeValue(SUB_TAB, EDIT_ITEM);
          controller.logEvent(editTab.getTab(), "Tab", "UserList_" + ul.getID(), EDIT_ITEM);
          if ((isReview || isComment)) {
            //    logger.info("getListOperations : showNPF ");

            reviewItem.showNPF(ul, editTab, (isReview ? REVIEW : COMMENT) + "_edit", false);
          } else {
            //   logger.info("getListOperations : showEditItem ");
            showEditItem(ul, editTab, editItem, !ul.isFavorite());
          }
        }
      });
    }

    // select the initial tab -- either add if an empty
    selectTabGivenHistory(tabPanel, learn, practice, editItemTab,
        ul, instanceName1,
        isReview, isComment, isAttention, isNormalList
    );

    return tabPanel;
  }

  private void showLearnTab(TabAndContent learnTab, UserList ul, String instanceName1, boolean isReview,
                            boolean isComment, boolean isAttention) {
    showLearnTab(isReview || isComment || isAttention, ul, learnTab, instanceName1);
  }

  private void showLearnTab(boolean isReview, UserList ul, TabAndContent learn, String instanceName1) {
     npfHelper.showNPF(ul, learn, instanceName1, true);
  }

  private boolean createdByYou(UserList ul) {
    return ul.getCreator().getId() == userManager.getUser();
  }

  void deleteList(Button delete, final UserList ul, final boolean onlyMyLists) {
    controller.logEvent(delete, "Button", "UserList_" + ul.getID(), "Delete");
    service.deleteList(ul.getUniqueID(), new AsyncCallback<Boolean>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Boolean result) {
        if (result) {
          refreshViewLessons(onlyMyLists, false);
        } else {
          logger.warning("---> did not do deleteList " + ul.getUniqueID());
        }
      }
    });
  }

  void setPublic(long uniqueID, boolean isPublic) {
    service.setPublicOnList(uniqueID, isPublic, new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Void result) {
      }
    });
  }

  private TabAndContent makeTab(TabPanel tabPanel, IconType iconType, String label) {
    return new TabAndContent(tabPanel, iconType, label);
  }


  /**
   * @param ul
   * @param addItem
   * @see #getListOperations
   */
  private void showEditItem(UserList ul, TabAndContent addItem, EditItem editItem, boolean includeAddItem) {
    addItem.getContent().clear();
    addItem.getContent().add(editItem.editItem(ul,
        new InlineLabel(),   // TODO get rid of this entirely
        includeAddItem));
  }


  /**
   * @param tabPanel
   * @param learn
   * @param practice
   * @param edit
   * @param ul
   * @param instanceName1
   * @param isReview
   * @param isComment
   * @param isAttention
   * @param isNormalList
   * @see #getListOperations
   */
  private void selectTabGivenHistory(TabPanel tabPanel, TabAndContent learn, TabAndContent practice,
                                     TabAndContent edit,
                                     UserList ul, String instanceName1, boolean isReview, boolean isComment,
                                     boolean isAttention, boolean isNormalList) {
    boolean chosePrev = selectPreviouslyClickedSubTab(tabPanel, learn, practice, edit,
        ul, instanceName1, isReview, isComment, isAttention, isNormalList);

    if (!chosePrev) {
        logger.info("selectTabGivenHistory ul " + ul.getName() + " private " + ul.isPrivate() + " empty " + ul.isEmpty() + " ");

      if (createdByYou(ul) &&
          //!ul.isPrivate() &&
          ul.isEmpty() && edit != null) {
        tabPanel.selectTab(ul.getName().equals(FIX_DEFECTS) ? 0 : SUBTAB_EDIT_INDEX);    // 2 = add/edit item
           logger.info("selectTabGivenHistory doing showEditReviewOrComment");
        showEditReviewOrComment(ul, isNormalList, edit, isReview, isComment);
      } else {
         logger.info("selectTabGivenHistory doing sublearn");

        tabPanel.selectTab(SUBTAB_LEARN_INDEX);
        showLearnTab(learn, ul, instanceName1, isReview, isComment, isAttention);
      }
    } else {
      logger.info("selectTabGivenHistory choose prev ");
    }
  }


  /**
   * @param tabPanel
   * @param learnTab
   * @param practiceTab
   * @param editTab
   * @param ul
   * @param instanceName1
   * @param isReview
   * @param isComment
   * @param isAttention
   * @param isNormalList
   * @return true if we have stored what tab we clicked on before
   * @see #selectTabGivenHistory
   */
  private boolean selectPreviouslyClickedSubTab(TabPanel tabPanel,
                                                TabAndContent learnTab,
                                                TabAndContent practiceTab,
                                                TabAndContent editTab,
                                                UserList ul, String instanceName1,
                                                boolean isReview, boolean isComment, boolean isAttention,
                                                boolean isNormalList) {
    String subTab = storage.getValue(SUB_TAB);
    logger.info("selectPreviouslyClickedSubTab : subtab '" + subTab +"'");

    boolean chosePrev = false;
    if (subTab != null) {
      chosePrev = true;
      if (subTab.equals(LEARN)) {
        tabPanel.selectTab(SUBTAB_LEARN_INDEX);
        learnTab.clickOnTab();
        showLearnTab(learnTab, ul, instanceName1, isReview, isComment, isAttention);
      } else if (subTab.equals(PRACTICE1)) {
        tabPanel.selectTab(SUBTAB_PRACTICE_INDEX);
        practiceTab.clickOnTab();
        avpHelper.setContentPanel(practiceTab.getContent());
        avpHelper.showNPF(ul, practiceTab, PRACTICE1, true);
      } else if (subTab.equals(EDIT_ITEM)) {
        boolean reviewOrComment = isReview || isComment;
        tabPanel.selectTab(reviewOrComment ? 1 : SUBTAB_EDIT_INDEX);
        editTab.clickOnTab();

        if (reviewOrComment) {
          reviewItem.showNPF(ul, editTab, (isReview ? REVIEW : COMMENT) + "_edit", false);
        } else {
          logger.info("selectPreviouslyClickedSubTab : showEditItem for list " + ul.getName());

          showEditItem(ul, editTab, this.editItem, isNormalList && !ul.isFavorite());
        }

      } else {
        logger.info("selectPreviouslyClickedSubTab : no subtab?");

        chosePrev = false;
      }
    }
    return chosePrev;
  }

  /**
   * @param ul
   * @param isNormalList
   * @param finalEditItem
   * @param isReview
   * @param isComment
   * @see #selectTabGivenHistory(com.github.gwtbootstrap.client.ui.TabPanel, TabAndContent, TabAndContent, TabAndContent, mitll.langtest.shared.custom.UserList, String, boolean, boolean, boolean, boolean)
   */
  private void showEditReviewOrComment(UserList ul, boolean isNormalList, TabAndContent finalEditItem,
                                       boolean isReview, boolean isComment) {
    boolean reviewOrComment = isReview || isComment;
    if (reviewOrComment) {
      reviewItem.showNPF(ul, finalEditItem, (isReview ? REVIEW : COMMENT) + "_edit", false);
    } else {
      showEditItem(ul, finalEditItem, this.editItem, isNormalList && !ul.isFavorite());
    }
  }
}
