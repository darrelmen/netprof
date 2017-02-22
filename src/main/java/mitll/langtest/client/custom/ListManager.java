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
import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.TabPanel;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SimpleHtmlSanitizer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.TextArea;
import mitll.langtest.client.custom.content.AVPHelper;
import mitll.langtest.client.custom.content.NPFHelper;
import mitll.langtest.client.custom.content.ReviewItemHelper;
import mitll.langtest.client.custom.dialog.CreateListDialog;
import mitll.langtest.client.custom.dialog.EditItem;
import mitll.langtest.client.custom.tabs.TabAndContent;
import mitll.langtest.client.dialog.DialogHelper;
import mitll.langtest.client.download.DownloadLink;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.services.ListService;
import mitll.langtest.client.services.ListServiceAsync;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.HasID;
import mitll.langtest.shared.user.User;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/20/15.
 */
public class ListManager implements RequiresResize {
  private final Logger logger = Logger.getLogger("ListManager");
  private static final String IMPORT_ITEM = "importItem";

  private final KeyStorage storage;
  private ScrollPanel listScrollPanel;

  private final ExerciseController controller;
   private final ListServiceAsync listService = GWT.create(ListService.class);

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

  private static final String PRACTICE = "Audio Vocabulary Practice";
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

  /**
   * @param controller
   * @param tabPanel
   * @see Navigation#getTabPanel
   */
  public ListManager(final ExerciseController controller,
                     TabPanel tabPanel,
                     ReloadableContainer exerciseList) {
    if (exerciseList == null) logger.warning("huh? exerciselist is null?\n\n\n");

   // this.service = service;
    this.userManager = controller.getUserManager();
    this.controller = controller;
    storage = new KeyStorage(controller);

    // browse tab
    browse = new TabAndContent(tabPanel, IconType.TH_LIST, BROWSE);
    browse.getTab().addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        checkAndMaybeClearTab(BROWSE);
        logEvent(browse, BROWSE);
        viewBrowse();
      }
    });

    npfHelper = new NPFHelper(controller, false, false);
    reviewItem = new ReviewItemHelper(controller, exerciseList);
    avpHelper = new AVPHelper(controller);
    editItem = new EditItem(controller, exerciseList);
  }

  void addStudyLists(final TabAndContent studyLists) {
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
   * @see #addStudyLists(TabAndContent)
   * @see Navigation#selectPreviousTab
   */
  public void showFirstUserListTab(TabPanel w, int tab) {
    yourStuff.clickOnTab();
    w.selectTab(tab);
    refreshViewLessons(true, false);
  }

  public void clickOnYourStuff() {
    yourStuff.clickOnTab();
  }

  public void clickOnCreate() {
    create.clickOnTab();
  }

  /**
   * @param tabPanel
   * @see #addStudyLists(TabAndContent)
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
    final CreateListDialog createListDialog = new CreateListDialog(this, userManager, controller);
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
   * @see Navigation#showInitialState
   */
  void showMyLists(boolean onlyCreated, boolean onlyVisited) {
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
   * @seex #getTabPanel
   * @see #showMyLists
   * @see #deleteList(com.github.gwtbootstrap.client.ui.Button, mitll.langtest.shared.custom.UserList, boolean)
   * @see #clickOnYourLists(long)
   */
  private void refreshViewLessons(boolean onlyMine, boolean onlyVisited) {
    viewLessons(onlyMine ? yourStuff.getContent() : othersStuff.getContent(), false, onlyMine, onlyVisited, -1);
  }

  /**
   * @see Navigation#selectPreviousTab
   * @see #addListTabs(TabPanel)
   */
  public void viewBrowse() {
    viewLessons(browse.getContent(), true, false, false, -1);
  }

  public void findListAndSelect(int exerciseID) {
    viewLessons(browse.getContent(), true, false, false, exerciseID);
  }

  /**
   * @param contentPanel
   * @param getAllPredef
   * @param onlyMine
   * @param onlyVisited
   * @param optionalExercise
   * @see #viewBrowse()
   * @see #refreshViewLessons(boolean, boolean)
   */
/*  private void viewLessons(final Panel contentPanel, boolean getAllPredef, boolean onlyMine, boolean onlyVisited,
                           String optionalExercise) {
    viewLessons(contentPanel, getAllPredef, onlyMine, onlyVisited, optionalExercise);
  }*/

  /**
   * @param contentPanel
   * @param getAll
   * @param onlyMine
   * @param onlyVisited
   * @param optionalExercise
   * @see #viewBrowse()
   * @see #refreshViewLessons(boolean, boolean)
   */
  private void viewLessons(final Panel contentPanel, boolean getAll, boolean onlyMine, boolean onlyVisited,
                           int optionalExercise) {
    contentPanel.clear();
    contentPanel.getElement().setId("contentPanel");

    final Panel insideContentPanel = new DivWidget();
    insideContentPanel.getElement().setId("insideContentPanel");
    contentPanel.add(insideContentPanel);
    insideContentPanel.addStyleName("exerciseBackground");
    listScrollPanel = new ScrollPanel();

    if (getAll) {
      // logger.info("viewLessons----> getAllPredef optional " + optionalExercise);
      listService.getUserListsForText("",
          new UserListCallback(this, contentPanel, insideContentPanel, listScrollPanel,
              LESSONS + "_All",
              false, true,
              userManager, onlyMine, optionalExercise));
    } else {
      logger.info("viewLessons for user #" + userManager.getUser());
      listService.getListsForUser(onlyMine,
          onlyVisited,
          new UserListCallback(this, contentPanel, insideContentPanel, listScrollPanel,
              LESSONS + (onlyMine ? "_Mine" : "_Others"),
              onlyMine, false,
              userManager, onlyMine, optionalExercise));
    }
  }

  /**
   * Return all the review lists -- defects (by teachers/QA), comments (by students), and attention LL.
   *
   * @param contentPanel
   * @see Navigation#addTabs
   * @see Navigation#selectPreviousTab
   */
  public void viewReview(final Panel contentPanel) {
    final ListManager outer = this;
    final Panel child = getContentChild(contentPanel);

    long then = System.currentTimeMillis();
//    logger.info("------> viewReview : reviewLessons for " + userManager.getUser());
    listService.getReviewLists(new AsyncCallback<List<UserList<CommonShell>>>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(List<UserList<CommonShell>> reviewLists) {

        long now = System.currentTimeMillis();

        logger.info("\tviewReview : reviewLessons for " + userManager.getUser() + " got " + reviewLists.size() + " in " + (now - then) + " millis");
        new UserListCallback(outer, contentPanel, child,
            new ScrollPanel(), REVIEW, false, false, userManager, false, -1).onSuccess(reviewLists);
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
    storage.storeValue(SUB_TAB, EDIT_ITEM);
    subListTabPanel.selectTab(0);
    yourStuff.clickOnTab();
    refreshViewLessons(true, false);
  }

  @Override
  public void onResize() {
    // logger.info("got onResize " + getClass().toString());
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
   * @param toSelect
   * @see UserListCallback#getDisplayRowPerList(mitll.langtest.shared.custom.UserList, boolean, boolean)
   * @see UserListCallback#selectPreviousList
   */
  void showList(final UserList ul, Panel contentPanel, final String instanceName, HasID toSelect) {
    logger.info("showList " + ul + " instance '" + instanceName + "'");
    controller.logEvent(contentPanel, "Tab", getListID(ul), "Show List");

    String previousList = storage.getValue(CLICKED_USER_LIST);
    String currentValue = storeCurrentClickedList(ul);

/*
    Collection exercises = ul.getExercises();
    for (Object ex : exercises) {
      logger.info(ul.getID() + " has " + ex);
    }
    */

    // if select a new list, clear the subtab selection
    if (previousList != null && !previousList.equals(currentValue)) {
      //  logger.info("\tshowList " +previousList + " vs " + currentValue + " remove " + SUB_TAB);
      storage.removeValue(SUB_TAB);
    }
    contentPanel.clear();
    contentPanel.add(makeTabContent(ul, instanceName, toSelect));

    addVisitor(ul);
  }

  public KeyStorage getStorage() {
    return storage;
  }

  /**
   * @param ul
   * @param instanceName
   * @param toSelect
   * @return
   * @see #showList(UserList, Panel, String, HasID)
   */
  private Panel makeTabContent(UserList ul, String instanceName, HasID toSelect) {
    FluidContainer container = new FluidContainer();
    container.getElement().setId("showListContainer");

    container.getElement().getStyle().setPaddingLeft(2, Style.Unit.PX);
    container.getElement().getStyle().setPaddingRight(2, Style.Unit.PX);

    Panel firstRow = getFirstInfoRow(ul);
    container.add(firstRow);

    Panel secondRow = new FluidRow();    // TODO : this is wacky -- clean up...

    String userID = ul.getUserChosenID();
    if (!userID.equals(User.NOT_SET)) {
      addCreatedBy(container, secondRow, userID);
    }

    container.add(gwtDownloadLinkRow(ul, instanceName));
    container.add(getListOperations(ul, instanceName, toSelect));

    return container;
  }

  private Panel gwtDownloadLinkRow(UserList ul, String instanceName) {
    Panel r1 = new FluidRow();
    r1.addStyleName("userListDarkerBlueColor");
    r1.add(getDownloadLink(ul, instanceName));
    return r1;
  }

  private Anchor getDownloadLink(UserList ul, String instanceName) {
    long listID = ul.getID();
    String linkid = instanceName + "_" + listID;
    Anchor downloadLink = new DownloadLink(controller).getDownloadLink(listID, linkid, ul.getName());
    Node child = downloadLink.getElement().getChild(0);
    AnchorElement.as(child).getStyle().setColor("#333333");
    return downloadLink;
  }

  private void addCreatedBy(FluidContainer container, Panel secondRow, String userID) {
    Heading child1 = new Heading(5, "created by " + userID);
    secondRow.add(child1);
    child1.addStyleName("leftFiveMargin");
    Style style = child1.getElement().getStyle();
    style.setMarginTop(3, Style.Unit.PX);
    style.setMarginBottom(3, Style.Unit.PX);
    secondRow.addStyleName("userListDarkerBlueColor");

    container.add(secondRow);
  }

  private Panel getFirstInfoRow(UserList ul) {
    Panel firstRow = new FluidRow();    // TODO : this is wacky -- clean up...
    firstRow.getElement().setId("container_first_row");
    firstRow.add(getListInfo(ul));
    firstRow.addStyleName("userListDarkerBlueColor");
    return firstRow;
  }


  private Panel getListInfo(UserList ul) {
    String subtext = ul.getDescription() + " " + ul.getClassMarker();
    Heading widgets = new Heading(1, ul.getName(), subtext);    // TODO : better color for subtext h1->small

    widgets.addStyleName("floatLeft");
    widgets.addStyleName("leftFiveMargin");
    widgets.getElement().getStyle().setMarginBottom(3, Style.Unit.PX);
    return widgets;
  }

  private String storeCurrentClickedList(HasID ul) {
    return storeCurrentClickedList(ul.getID());
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
   * @see #showList
   */
  private void addVisitor(UserList ul) {
    if (ul.getUserID() != controller.getUser()) {
      listService.addVisitor(ul.getID(), controller.getUser(), new AsyncCallback<Void>() {
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
   * Add tabs for list
   *
   * @param ul
   * @param instanceName
   * @param toSelect
   * @return
   * @see #showList
   */
  private TabPanel getListOperations(final UserList<CommonShell> ul, final String instanceName, final HasID toSelect) {
    logger.info("getListOperations : '" + instanceName + " for list " + ul);
    boolean isMyList = createdByYou(ul);
    boolean created = isMyList ||
        instanceName.equals(REVIEW) ||
        instanceName.equals(COMMENT);

    final TabPanel tabPanel = new TabPanel();
    final boolean isReview = instanceName.equals(REVIEW);
    final boolean isComment = instanceName.equals(COMMENT);
    final boolean isAttention = instanceName.equals(ATTENTION);
    final String instanceName1 = isReview ? REVIEW :
        isComment ? COMMENT : isAttention ? ATTENTION : LEARN;

    // add learn tab
    String learnTitle =
        isReview ? POSSIBLE_DEFECTS :
            isComment ? ITEMS_WITH_COMMENTS :
                isAttention ? "Items for LL" : LEARN_PRONUNCIATION;

    final boolean isNormalList = !isReview &&
        !isComment && !isAttention;

    final TabAndContent learn = isNormalList ? getLearnTab(ul, tabPanel, isReview, instanceName1, learnTitle) : null;

    // add practice tab
    TabAndContent practice = null;
    if (isNormalList) {
      //logger.info("getListOperations : isNormalList ");
      practice = getPracticeTab(ul, toSelect, tabPanel);
    }

    // add add item and edit tabs (conditionally)
    TabAndContent editItemTab = null;

    // see bug #650 - teachers should be able to record items for a student
    //  logger.info("edit tab created " + created);
    if ((created || userManager.isTeacher()) &&
        (!ul.isPrivate() || isMyList)) {
      editItemTab = getEditTab(ul, toSelect, tabPanel,
          isReview,
          //false,
          isComment);
    }
    // if (SHOW_IMPORT) {
    if (isMyList &&
        !isReview &&
        !isComment && !ul.isFavorite()) {
      getImportTab(ul, tabPanel, learn, instanceName1);
    }
    // }

    // select the initial tab -- either add if an empty
    selectTabGivenHistory(tabPanel,
        learn,
        practice,
        editItemTab,
        ul,
        instanceName1,
        isReview,
        isComment,
        isNormalList,
        toSelect);

    return tabPanel;
  }

  private TabAndContent getLearnTab(final UserList<CommonShell> ul, TabPanel tabPanel, boolean isReview, final String instanceName1, String learnTitle) {
    final TabAndContent learn = makeTab(tabPanel,
        isReview ?
            IconType.EDIT_SIGN :
            IconType.LIGHTBULB,
        learnTitle);
    learn.getTab().addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        controller.logEvent(learn.getTab(), "Tab", getListID(ul), LEARN);
        storage.storeValue(SUB_TAB, LEARN);
        showLearnTab(learn, ul, instanceName1, null);
      }
    });
    return learn;
  }

  private TabAndContent getPracticeTab(final UserList<CommonShell> ul, final HasID toSelect, TabPanel tabPanel) {
    TabAndContent practice;
    practice = makeTab(tabPanel, IconType.CHECK, PRACTICE);
    final TabAndContent fpractice = practice;
    practice.getContent().addStyleName("centerPractice");
    practice.getTab().addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        storage.storeValue(SUB_TAB, PRACTICE1);
        //       logger.info("getListOperations : got click on practice " + fpractice.getContent().getElement().getExID());
        avpHelper.setContentPanel(fpractice.getContent());
        avpHelper.showNPF(ul, fpractice, PRACTICE1, true, toSelect);
        controller.logEvent(fpractice.getTab(), "Tab", getListID(ul), PRACTICE1);
      }
    });
    return practice;
  }

  /**
   * @param ul
   * @param toSelect
   * @param tabPanel
   * @param isReview
   * @param isComment
   * @return
   * @see #getListOperations(UserList, String, HasID)
   */
  private TabAndContent getEditTab(final UserList<CommonShell> ul,
                                   final HasID toSelect,
                                   TabPanel tabPanel,
                                   final boolean isReview,
                                   final boolean isComment) {
    final TabAndContent editTab = makeTab(tabPanel, IconType.EDIT, isReview ? ADD_DELETE_EDIT_ITEM : ADD_OR_EDIT_ITEM);
    // logger.info("getListOperations : making editTab for list " + ul.getName());

    editTab.getTab().addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        //    logger.info("getListOperations : got click on edit tab ");
        storage.storeValue(SUB_TAB, EDIT_ITEM);
        controller.logEvent(editTab.getTab(), "Tab", getListID(ul), EDIT_ITEM);
        if ((isReview || isComment)) {
          //    logger.info("getListOperations : showNPF ");
          reviewItem.showNPF(ul, editTab, getInstanceName(isReview), false, toSelect);
        } else {
//          logger.info("getEditTab : showEditItem "  + " : " + ul.getName());
          showEditItem(ul, editTab, editItem);
        }
      }
    });
    return editTab;
  }

  private String getListID(HasID ul) {
    return "UserList_" + ul.getID();
  }

  private TabAndContent getImportTab(final UserList<CommonShell> ul, final TabPanel tabPanel,
                                     final TabAndContent learnTab, final String instanceName
  ) {
    final TabAndContent importTab = makeTab(tabPanel, IconType.UPLOAD_ALT, "Import");
    importTab.getTab().addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        //    logger.info("getListOperations : got click on edit tab ");
        storage.storeValue(SUB_TAB, IMPORT_ITEM);
        controller.logEvent(importTab.getTab(), "Tab", getListID(ul), IMPORT_ITEM);
        showImportItem(ul, importTab, learnTab, instanceName, tabPanel);
      }
    });
    return importTab;
  }

  /**
   * @param learnTab
   * @param ul
   * @param instanceName1
   * @see #getListOperations
   */
  private void showLearnTab(TabAndContent learnTab, UserList<CommonShell> ul, String instanceName1, HasID toSelect) {
    npfHelper.showNPF(ul, learnTab, instanceName1, true, toSelect);
  }

  private boolean createdByYou(UserList<?> ul) {
    return ul.getUserID() == getUser();
  }

  private int getUser() {
    return userManager.getUser();
  }

  void deleteList(Button delete, final UserList ul, final boolean onlyMyLists) {
    controller.logEvent(delete, "Button", getListID(ul), "Delete");
    final long uniqueID = ul.getID();

    listService.deleteList(uniqueID, new AsyncCallback<Boolean>() {
      @Override
      public void onFailure(Throwable caught) {
        logger.warning("delete list call failed?");
      }

      @Override
      public void onSuccess(Boolean result) {
        if (result) {
          logger.info("deleteList ---> did do deleteList " + uniqueID);
          refreshViewLessons(onlyMyLists, false);
        } else {
          logger.warning("deleteList ---> did not do deleteList " + uniqueID);
        }
      }
    });
  }

  /**
   * @param uniqueID
   * @param isPublic
   * @see UserListCallback#getIsPublic(UserList, long)
   */
  void setPublic(long uniqueID, boolean isPublic) {
    listService.setPublicOnList(uniqueID, isPublic, new AsyncCallback<Void>() {
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
   * @param container
   * @see #getListOperations
   */
  private void showEditItem(UserList ul, TabAndContent container, EditItem editItem) {
    container.getContent().clear();
    container.getContent().add(editItem.editItem(ul));
  }

  /**
   * @param ul
   * @param container
   * @param learnTab
   * @param instanceName
   * @param tabPanel
   * @see #getImportTab(UserList, TabPanel, TabAndContent, String)
   */
  private void showImportItem(final UserList ul, final TabAndContent container, final TabAndContent learnTab, final String instanceName,
                              final TabPanel tabPanel) {
    container.getContent().clear();
    DivWidget inner = new DivWidget();
    DivWidget upper = new DivWidget();
    upper.setWidth("600px");
    final TextArea w = new TextArea();
    w.setWidth("600px");

    upper.add(w);
    inner.add(upper);
    w.setVisibleLines(20);
    w.setCharacterWidth(150);

    inner.add(new Heading(4, "Copy and paste tab separated lines with pairs of " + controller.getLanguage() + " item and its translation."));
    inner.add(new Heading(4, "(Quizlet export format.)"));
    Button anImport = new Button("Import");
    anImport.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        listService.reallyCreateNewItems(ul.getID(), sanitize(w.getText()),
            new AsyncCallback<Collection<CommonExercise>>() {
              @Override
              public void onFailure(Throwable caught) {
              }

              @Override
              public void onSuccess(Collection<CommonExercise> newExercise) {
                logger.info("before " + ul.getExercises().size());
                for (CommonExercise exercise : newExercise) ul.addExercise(exercise);
                logger.info("after  " + ul.getExercises().size());

                reallyShowLearnTab(tabPanel, learnTab, ul, instanceName);
              }
            });
      }
    });
    DivWidget bottom = new DivWidget();
    bottom.add(anImport);
    bottom.addStyleName("topFiveMargin");
    inner.add(bottom);
    container.getContent().add(inner);
  }

  private String sanitize(String text) {
    return SimpleHtmlSanitizer.sanitizeHtml(text).asString();
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
   * @param isNormalList
   * @param toSelect
   * @paramx isAttention
   * @see #getListOperations
   */
  private void selectTabGivenHistory(TabPanel tabPanel,
                                     TabAndContent learn,
                                     TabAndContent practice,
                                     TabAndContent edit,
                                     UserList ul,
                                     String instanceName1,
                                     boolean isReview,
                                     boolean isComment,
                                     boolean isNormalList,
                                     HasID toSelect) {
    boolean chosePrev = selectPreviouslyClickedSubTab(tabPanel, learn, practice, edit,
        ul, instanceName1, isReview, isComment, isNormalList);

    if (!chosePrev) {
      //logger.info("selectTabGivenHistory ul " + ul.getName() + " private " + ul.isPrivate() + " empty " + ul.isEmpty() + " ");
      if (createdByYou(ul) &&
          //!ul.isPrivate() &&
          ul.isEmpty() && edit != null) {
        tabPanel.selectTab(ul.getName().equals(FIX_DEFECTS) ? 0 : SUBTAB_EDIT_INDEX);    // 2 = add/edit item
        logger.info("selectTabGivenHistory doing showEditReviewOrComment");
        showEditReviewOrComment(ul, isNormalList, edit, isReview, isComment);
      } else {
        //  logger.info("selectTabGivenHistory doing sublearn " + instanceName1+ " learn " + learn);

        if (learn == null) {
          tabPanel.selectTab(0); // first tab
          //   reviewItem.showNPF(ul, edit, getInstanceName(isReview), false, toSelect);
          showEditReviewOrComment(ul, isNormalList, edit, isReview, isComment);

        } else {
          tabPanel.selectTab(SUBTAB_LEARN_INDEX);
          showLearnTab(learn, ul, instanceName1, toSelect);
        }
      }
    } else {
//      logger.info("selectTabGivenHistory choose prev ");
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
   * @param isNormalList
   * @return true if we have stored what tab we clicked on before
   * @paramx isAttention
   * @see #selectTabGivenHistory
   */
  private boolean selectPreviouslyClickedSubTab(TabPanel tabPanel,
                                                TabAndContent learnTab,
                                                TabAndContent practiceTab,
                                                TabAndContent editTab,
                                                UserList ul, String instanceName1,
                                                boolean isReview, boolean isComment,
                                                boolean isNormalList) {
    String subTab = storage.getValue(SUB_TAB);
    //   logger.info("selectPreviouslyClickedSubTab : subtab '" + subTab + "'");

    boolean chosePrev = false;
    if (subTab != null) {
      chosePrev = true;
      switch (subTab) {
        case LEARN:
          reallyShowLearnTab(tabPanel, learnTab, ul, instanceName1);
          break;
        case PRACTICE1:
          tabPanel.selectTab(SUBTAB_PRACTICE_INDEX);
          practiceTab.clickOnTab();
          avpHelper.setContentPanel(practiceTab.getContent());
          avpHelper.showNPF(ul, practiceTab, PRACTICE1, true);
          break;
        case EDIT_ITEM:
          boolean reviewOrComment = isReview || isComment;
          tabPanel.selectTab(reviewOrComment ? 1 : SUBTAB_EDIT_INDEX);
          editTab.clickOnTab();

          if (reviewOrComment) {
            reviewItem.showNPF(ul, editTab, getInstanceName(isReview), false);
          } else {
            logger.info("selectPreviouslyClickedSubTab : showEditItem for list " + ul.getName());
            showEditItem(ul, editTab, this.editItem);
          }

          break;
        default:
          //  logger.info("selectPreviouslyClickedSubTab : no subtab?");
          chosePrev = false;
          break;
      }
    }
    return chosePrev;
  }

  private void reallyShowLearnTab(TabPanel tabPanel, TabAndContent learnTab, UserList ul, String instanceName1) {
    if (!ul.isEmpty()) {
      tabPanel.selectTab(SUBTAB_LEARN_INDEX);
      learnTab.clickOnTab();
      showLearnTab(learnTab, ul, instanceName1, ul.getLast());
    } else {
      new DialogHelper(false).showErrorMessage("No items imported", "No items had valid " + controller.getLanguage() + " text.");
    }
  }

  private String getInstanceName(boolean isReview) {
    String s = isReview ? REVIEW : COMMENT;
    return s + "_edit";
  }

  /**
   * @param ul
   * @param isNormalList
   * @param finalEditItem
   * @param isReview
   * @param isComment
   * @see #selectTabGivenHistory
   */
  private void showEditReviewOrComment(UserList ul, boolean isNormalList, TabAndContent finalEditItem,
                                       boolean isReview, boolean isComment) {
    boolean reviewOrComment = isReview || isComment;
    if (reviewOrComment) {
      reviewItem.showNPF(ul, finalEditItem, getInstanceName(isReview), false);
    } else {
      showEditItem(ul, finalEditItem, this.editItem);
    }
  }
}
