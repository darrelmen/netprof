package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.TabPanel;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.InlineLabel;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.bootstrap.FlexSectionExerciseList;
import mitll.langtest.client.contextPractice.DialogWindow;
import mitll.langtest.client.custom.content.AVPHelper;
import mitll.langtest.client.custom.content.ChapterNPFHelper;
import mitll.langtest.client.custom.content.NPFHelper;
import mitll.langtest.client.custom.content.ReviewItemHelper;
import mitll.langtest.client.custom.dialog.CreateListDialog;
import mitll.langtest.client.custom.dialog.EditItem;
import mitll.langtest.client.custom.exercise.CommentNPFExercise;
import mitll.langtest.client.custom.tabs.TabAndContent;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.flashcard.StatsFlashcardFactory;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.qc.QCNPFExercise;
import mitll.langtest.client.scoring.GoodwaveExercisePanel;
import mitll.langtest.client.scoring.GoodwaveExercisePanelFactory;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.User;
import mitll.langtest.shared.custom.UserList;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 9/27/13
 * Time: 8:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class Navigation implements RequiresResize {
  private Logger logger = Logger.getLogger("Navigation");

  private static final String CHAPTERS = "Learn Pronunciation";
//  private static final String CONTENT = CHAPTERS;
  private static final String YOUR_LISTS = "Study Your Lists";
  private static final String STUDY_LISTS = "Study Lists";// and Favorites";
  private static final String OTHERS_LISTS = "Study Visited Lists";
  private static final String PRACTICE = "Audio Flashcards"; // "Do Flashcards";
  public static final String REVIEW = "review";
  public static final String COMMENT = "comment";
  private static final String ATTENTION = "attention";
  private static final String PRACTICE1 = "practice";
  private static final String ADD_OR_EDIT_ITEM = "Add/Edit Item";
  private static final String ADD_DELETE_EDIT_ITEM = "Fix Defects";
  private static final String MARK_DEFECTS = "Mark Defects";
  private static final String POSSIBLE_DEFECTS = "Review";
  private static final String ITEMS_WITH_COMMENTS = "Items with comments";
  private static final String LEARN_PRONUNCIATION = "Learn Pronunciation";
  private static final String PRACTICE_DIALOG = "Practice Dialog";
  private static final String FIX_DEFECTS = "Fix Defects";
  private static final String CREATE = "Create a New List";
  private static final String BROWSE = "Browse Lists";
  private static final String LESSONS = "lessons";
  public static final String CLICKED_USER_LIST = "clickedUserList";
  private static final String CLICKED_TAB = "clickedTab";
  private static final String SUB_TAB = "subTab";

  private static final int SUBTAB_LEARN_INDEX = 0;
  private static final int SUBTAB_PRACTICE_INDEX = 1;
  private static final int SUBTAB_EDIT_INDEX = 2;

  private static final String EDIT_ITEM = "editItem";
  private static final String LEARN = "learn";
  private static final String RECORD_AUDIO = "Record Audio";
  private static final String RECORD_EXAMPLE = "Record In-context Audio";
  private static final String CONTENT1 = "content";
 // public static final String CLASSROOM = "classroom";
  public static final String MARK_DEFECTS1 = "markDefects";

  private final ExerciseController controller;
  private final LangTestDatabaseAsync service;
  private final UserManager userManager;
  private final SimpleChapterNPFHelper practiceHelper;

  private ScrollPanel listScrollPanel;
  private final NPFHelper npfHelper;
  private final NPFHelper avpHelper;
  private final DialogWindow dialogWindow;

  private final EditItem editItem;

  private final ChapterNPFHelper defectHelper;
  private final SimpleChapterNPFHelper recorderHelper, recordExampleHelper;
  private final SimpleChapterNPFHelper markDefectsHelper, learnHelper;
  private final ReviewItemHelper reviewItem;

  private final KeyStorage storage;


  /**
   * @param service
   * @param userManager
   * @param controller
   * @paramx predefinedContentList
   * @param feedback
   * @see mitll.langtest.client.LangTest#populateRootPanel()
   */
  public Navigation(final LangTestDatabaseAsync service, final UserManager userManager,
                    final ExerciseController controller, UserFeedback feedback) {
    this.service = service;
    this.userManager = userManager;
    this.controller = controller;
    storage = new KeyStorage(controller);

    learnHelper = new SimpleChapterNPFHelper(service, feedback, userManager, controller, null
    ) {
      protected ExercisePanelFactory getFactory(final PagingExerciseList exerciseList) {
        return new GoodwaveExercisePanelFactory(service, feedback, controller, exerciseList, 1.0f) {
          @Override
          public Panel getExercisePanel(CommonExercise e) {
            return new CommentNPFExercise(e, controller, exerciseList, false, "classroom");
          }
        };
      }
    };

    npfHelper = new NPFHelper(service, feedback, userManager, controller, false);

    avpHelper = new AVPHelper(service, feedback, userManager, controller);
    
    dialogWindow = new DialogWindow(service, controller);

    defectHelper = new ChapterNPFHelper(service, feedback, userManager, controller, true);

    markDefectsHelper = new SimpleChapterNPFHelper(service, feedback, userManager, controller,
        learnHelper.getExerciseList()
    ) {
      protected ExercisePanelFactory getFactory(final PagingExerciseList exerciseList) {
        return new GoodwaveExercisePanelFactory(service, feedback, controller, exerciseList, 1.0f) {
          @Override
          public Panel getExercisePanel(CommonExercise e) {
            return new QCNPFExercise(e, controller, exerciseList, MARK_DEFECTS1);

          }
        };
      }
    };

    practiceHelper = makePracticeHelper(service, userManager, controller, feedback);
    ListInterface exerciseList = npfHelper.getExerciseList();
    logger.info("Navigation : npfHelper exercise list is " + exerciseList);
    reviewItem = new ReviewItemHelper(service, feedback, userManager, controller, exerciseList, npfHelper);
   // logger.info("Navigation : made review item helper " + reviewItem);

    editItem = new EditItem(service, userManager, controller, exerciseList, feedback, npfHelper);

    recorderHelper = new RecorderNPFHelper(service, feedback, userManager, controller, true, exerciseList);
    recordExampleHelper = new RecorderNPFHelper(service, feedback, userManager, controller, false, exerciseList);

  }

  /**
   * @see #Navigation(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserManager, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.user.UserFeedback)
   * @param service
   * @param userManager
   * @param controller
   * @param feedback
   * @return
   */
  private SimpleChapterNPFHelper makePracticeHelper(final LangTestDatabaseAsync service, final UserManager userManager,
                                                    final ExerciseController controller, final UserFeedback feedback) {
    return new SimpleChapterNPFHelper(service, feedback, userManager, controller,
        null
    ) {
      StatsFlashcardFactory statsFlashcardFactory;
      Widget outerBottomRow;

      @Override
      protected ExercisePanelFactory getFactory(PagingExerciseList exerciseList) {
        statsFlashcardFactory = new StatsFlashcardFactory(service, feedback, controller, exerciseList, "practice", null);
        statsFlashcardFactory.setContentPanel(outerBottomRow);
        return statsFlashcardFactory;
      }

      @Override
      public void onResize() {
        super.onResize();
        if (statsFlashcardFactory != null) {
          statsFlashcardFactory.onResize();
        }
      }

      @Override
      protected NPFHelper.FlexListLayout getMyListLayout(LangTestDatabaseAsync service, UserFeedback feedback,
                                                         UserManager userManager, ExerciseController controller,
                                                         SimpleChapterNPFHelper outer) {
        return new MyFlexListLayout(service, feedback, userManager, controller, outer) {
          @Override
          protected FlexSectionExerciseList makeExerciseList(Panel topRow, Panel currentExercisePanel, String instanceName,
                                                             boolean incorrectFirst) {
            return new MyFlexSectionExerciseList(topRow, currentExercisePanel, instanceName, true) {
              @Override
              protected CommonShell findFirstExercise() {
                String currentExerciseID = statsFlashcardFactory.getCurrentExerciseID();
                if (currentExerciseID != null && !currentExerciseID.trim().isEmpty()) {
                  logger.info("findFirstExercise ---> found previous state current ex = " + currentExerciseID);

                  CommonShell shell = byID(currentExerciseID);

                  if (shell == null) {
                    logger.warning("huh? can't find " + currentExerciseID);
                    return super.findFirstExercise();
                  }
                  else {
                    statsFlashcardFactory.populateCorrectMap();
                    return shell;
                  }
                }
                else {
                  return super.findFirstExercise();
                }
              }

              @Override
             protected void onLastItem() {  statsFlashcardFactory.resetStorage();  }

              @Override
              protected void loadExercises(Map<String, Collection<String>> typeToSection, String item) {
                super.loadExercises(typeToSection, item);
                statsFlashcardFactory.setSelection(typeToSection);
              }
            };
          }

          @Override
          protected void styleBottomRow(Panel bottomRow) {
        //    logger.info("-----\n\n Adding style to " + bottomRow.getElement().getId());
            bottomRow.addStyleName("centerPractice");
            outerBottomRow = bottomRow;
          }
        };
      }
    };
  }

  private TabPanel tabPanel;
  private TabAndContent yourStuff, othersStuff, studyLists;
  private TabAndContent browse, dialog, chapters, create;
  private TabAndContent review, recorderTab, recordExampleTab, markDefectsTab, practiceTab;
  private final List<TabAndContent> tabs = new ArrayList<TabAndContent>();

  /**
   * @see #getNav
   * @return
   * @see mitll.langtest.client.LangTest#populateRootPanel()
   */
  public Widget getNav() {
    tabPanel = new TabPanel();
    tabPanel.getElement().getStyle().setMarginTop(-8, Style.Unit.PX);
    tabPanel.getElement().setId("tabPanel");

    // so we can know when chapters is revealed and tell it to update it's lists
    tabPanel.addShowHandler(new TabPanel.ShowEvent.Handler() {
      @Override
      public void onShow(TabPanel.ShowEvent showEvent) {
        TabLink target = showEvent.getTarget();
        String targetName = target == null ? "" : target.toString();
 /*       logger.info("got shown event : '" +showEvent + "'\n" +
            "\ntarget " + target +
            " ' target name '" + targetName+ "'");*/

        boolean wasChapters = targetName.contains(CHAPTERS);
        Panel createdPanel = learnHelper.getExerciseList() != null ? learnHelper.getExerciseList().getCreatedPanel() : null;
        boolean hasCreated = createdPanel != null;
        // logger.info("getTabPanel : got shown event : '" +showEvent + "' target '" + targetName + "' hasCreated " + hasCreated);
        if (hasCreated && wasChapters && (createdPanel instanceof GoodwaveExercisePanel)) {
       //   logger.info("\taddShowHandler got chapters! created panel :  has created " + hasCreated + " was revealed  " + createdPanel.getClass());
          ((GoodwaveExercisePanel) createdPanel).wasRevealed();
        } else {
     /*     logger.info("\taddShowHandler ignoring target " + targetName);
          logger.info("\taddShowHandler ignoring target " + learnHelper);
          logger.info("\taddShowHandler ignoring target " + learnHelper.getExerciseList());
          if (learnHelper.getExerciseList() != null) {
            logger.info("\taddShowHandler ignoring target " + learnHelper.getExerciseList().getCreatedPanel());
          }*/
        }
      }
    });

    return tabPanel;    // TODO - consider how to tell panels when they are hidden by tab changes
  }

  TabPanel subListTabPanel;

  /**
   * Defines order of tabs...
   *
   * @return
   * @see #showInitialState()
   */
  private void addTabs() {
    tabPanel.clear();
    tabs.clear();
    nameToTab.clear();
    nameToIndex.clear();

    addDialogTab();

    // learn tab

    chapters = makeFirstLevelTab(tabPanel, IconType.LIGHTBULB, CHAPTERS);
    chapters.getTab().addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        checkAndMaybeClearTab(CHAPTERS);
        learnHelper.showNPF(chapters, LEARN);
        logEvent(chapters, CHAPTERS);
      }
    });

    addPracticeTab();

    studyLists = makeFirstLevelTab(tabPanel, IconType.FOLDER_CLOSE, STUDY_LISTS);
    subListTabPanel = new TabPanel();
    studyLists.getContent().add(subListTabPanel);

    addListTabs(subListTabPanel);

    studyLists.getTab().addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        checkAndMaybeClearTab(STUDY_LISTS);
        showFirstUserListTab(subListTabPanel,0);
        logEvent(studyLists, STUDY_LISTS);
      }
    });

    if (isQC()) {
      markDefectsTab = makeFirstLevelTab(tabPanel, IconType.FLAG, MARK_DEFECTS);
      markDefectsTab.getContent().getElement().setId("content_contentPanel");
      markDefectsTab.getTab().addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          checkAndMaybeClearTab(MARK_DEFECTS);
          markDefectsHelper.showNPF(markDefectsTab, MARK_DEFECTS1);
          logEvent(recorderTab, MARK_DEFECTS);
        }
      });

      review = makeFirstLevelTab(tabPanel, IconType.EDIT, FIX_DEFECTS);
      review.getContent().getElement().setId("viewReview_contentPanel");
      review.getTab().addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          checkAndMaybeClearTab(FIX_DEFECTS);
          viewReview(review.getContent());
          logEvent(review, FIX_DEFECTS);
        }
      });
    }

    if (controller.getPermissions().contains(User.Permission.RECORD_AUDIO)) {
      recorderTab = makeFirstLevelTab(tabPanel, IconType.MICROPHONE, RECORD_AUDIO);
      recorderTab.getContent().getElement().setId("recorder_contentPanel");
      recorderTab.getTab().addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          checkAndMaybeClearTab(RECORD_AUDIO);
          recorderHelper.showNPF(recorderTab, "record_Audio");
          logEvent(recorderTab, RECORD_AUDIO);
        }
      });

      recordExampleTab = makeFirstLevelTab(tabPanel, IconType.MICROPHONE, RECORD_EXAMPLE);
      recordExampleTab.getContent().getElement().setId("record_example_contentPanel");
      recordExampleTab.getTab().addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          checkAndMaybeClearTab(RECORD_EXAMPLE);
          recordExampleHelper.showNPF(recordExampleTab, "record_Example_Audio");
          logEvent(recordExampleTab, RECORD_EXAMPLE);
        }
      });
    }
  }

  private void showFirstUserListTab(TabPanel w, int tab) {
    clickOnTab(yourStuff);
    w.selectTab(tab);
    refreshViewLessons(true, false);
  }

  /**
   * @see #addTabs
   * @param tabPanel
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

  /**
   * Make the practice tab...
   * @see #addTabs
   */
  private void addPracticeTab() {
    practiceTab = makeFirstLevelTab(tabPanel, IconType.REPLY, PRACTICE);
    practiceTab.getContent().getElement().setId("practicePanel");
    practiceTab.getTab().addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        showPracticeTab();
        logEvent(practiceTab, PRACTICE);
      }
    });
  }

  private void showPracticeTab() {
    if (practiceTab != null) {
      checkAndMaybeClearTab(PRACTICE);
     // logger.info(" ------- showPracticeTab make practice tab  - " + practiceTab.getContent());
      practiceHelper.showNPF(practiceTab, PRACTICE);
      practiceHelper.setContentPanel(practiceTab.getContent());
      practiceHelper.hideList();
    }
//    else {
      //logger.warning(" ------- showPracticeTab make practice tab  - " + practiceTab);

  //  }
  }
  
  private void addDialogTab(){
	  if(controller.getProps().hasDialog()){
	    	dialog = makeFirstLevelTab(tabPanel, IconType.TH_LIST, PRACTICE_DIALOG);
	    	dialog.getTab().addClickHandler(new ClickHandler() {
	    		@Override
	    		public void onClick(ClickEvent event) {
	    			checkAndMaybeClearTab(PRACTICE_DIALOG);
	    			logEvent(dialog, PRACTICE_DIALOG);
	    			dialogWindow.viewDialog(dialog.getContent());
	    		}
	    	});
	    }
  }

  private boolean isQC() {  return controller.getPermissions().contains(User.Permission.QUALITY_CONTROL); }

  void logEvent(TabAndContent yourStuff, String context) {
    if (yourStuff != null && yourStuff.getTab() != null) {
      controller.logEvent(yourStuff.getTab().asWidget(), "Tab", "", context);
    }
  }

  private final Map<String, TabAndContent> nameToTab = new HashMap<String, TabAndContent>();
  private final Map<String, Integer> nameToIndex = new HashMap<String, Integer>();

  private TabAndContent makeFirstLevelTab(TabPanel tabPanel, IconType iconType, String label) {
    //logger.info("makeFirstLevelTab " + label);
    TabAndContent tabAndContent = makeTab(tabPanel, iconType, label);
    nameToIndex.put(label, tabs.size());
    tabs.add(tabAndContent);
    nameToTab.put(label, tabAndContent);
    return tabAndContent;
  }

  private int getSafeTabIndexFor(String tabName) {
    Integer integer = nameToIndex.get(tabName);
    if (integer == null) return 0;
    else return integer;
  }

  private void checkAndMaybeClearTab(String value) {
 //   String value1 = storage.getValue(CLICKED_TAB);
    //logger.info("checkAndMaybeClearTab " + value1 + " vs "+value + " clearing " + CLICKED_USER_LIST);
    storage.removeValue(CLICKED_USER_LIST);
    storage.storeValue(CLICKED_TAB, value);
  }

  /**
   * @see #getNav()
   * @see #showMyLists
   * @see #deleteList(com.github.gwtbootstrap.client.ui.Button, mitll.langtest.shared.custom.UserList, boolean)
   * @see #clickOnYourLists(long)
   * @param onlyMine
   * @param onlyVisited
   */
  void refreshViewLessons(boolean onlyMine, boolean onlyVisited) {
    viewLessons(onlyMine ? yourStuff.getContent() : othersStuff.getContent(), false, onlyMine, onlyVisited);
  }

  /**
   *
   * @see mitll.langtest.client.LangTest#configureUIGivenUser(long)
   */
  public void showInitialState() {
    addTabs();

/*    logger.info("showInitialState show initial state for " + user +
        " : getting user lists " + controller.isReviewMode());*/
    String value = storage.getValue(CLICKED_TAB);
    if (value.isEmpty()) {   // no previous tab
      service.getListsForUser(userManager.getUser(), true, true, new AsyncCallback<Collection<UserList>>() {
        @Override
        public void onFailure(Throwable caught) {}

        @Override
        public void onSuccess(Collection<UserList> result) {
          if (result.size() == 1 && // if only one empty list - one you've created
              result.iterator().next().isEmpty()) {
            // choose default tab to show
            showDefaultInitialTab();
          } else {
            boolean foundCreated = false;
            for (UserList ul : result) {
              if (createdByYou(ul)) {
                foundCreated = true;
                break;
              }
            }
            showMyLists(foundCreated, !foundCreated, subListTabPanel);
          }
        }
      });
    } else {
      //logger.info("previous selection was " + value);
      selectPreviousTab(value);
    }
  }

  public void refreshInitialState() {
    String value = storage.getValue(CLICKED_TAB);
    if (value.isEmpty()) {   // no previous tab
      showDefaultInitialTab();
    }
    else {
      selectPreviousTab(value);
    }
  }

  /**
   * @see #showInitialState()
   */
  private void selectPreviousTab(String value) {
    TabAndContent tabAndContent = nameToTab.get(value);
    Integer tabIndex = nameToIndex.get(value);
    String orig = value;
    if (tabIndex == null) {
      if (value.equals(OTHERS_LISTS) || value.equals(YOUR_LISTS) || value.equals(CREATE) || value.equals(BROWSE)) {
        value = STUDY_LISTS;
        tabIndex = nameToIndex.get(value);
      }
    }
    if (tabIndex != null) {
      tabPanel.selectTab(tabIndex);
      clickOnTab(tabAndContent);

      if (value.equals(YOUR_LISTS)) {
        showMyLists(true, false,subListTabPanel);
      } else if (value.equals(OTHERS_LISTS)) {
        showMyLists(false, true,subListTabPanel);
      } else if (value.equals(STUDY_LISTS)/* || value.equals(OTHERS_LISTS) || value.equals(YOUR_LISTS)*/) {
        //showMyLists(false, true);
        clickOnTab(yourStuff);
       // refreshViewLessons(true, false);
        DivWidget content = studyLists.getContent();
        Widget widget = content.getWidget(0);

        int tab = orig.equals(YOUR_LISTS) ? 0 : orig.equals(OTHERS_LISTS) ? 1 :orig.equals(CREATE) ? 2: orig.equals(BROWSE) ? 3:0;
      //  logger.info("Select tab " + tab + " orig " + orig);
        showFirstUserListTab((TabPanel) widget, tab);
        if (tab == 0) {
          showMyLists(true, false,subListTabPanel);
        }
        else if (tab == 1) {
          showMyLists(false, true,subListTabPanel);
        }
        else if (tab == 2) {
          clickOnTab(create);
        }
        else {
          viewBrowse(); // CHAPTERS
        }
      } else if (value.equals(BROWSE)) {
        viewBrowse(); // CHAPTERS
      } else if (value.equals(FIX_DEFECTS)) {
        viewReview(review.getContent());
      } else if (value.equals(PRACTICE_DIALOG)) {
        dialogWindow.viewDialog(dialog.getContent());
      } else if (value.equals(CHAPTERS)) {
        learnHelper.showNPF(chapters, LEARN);
      } else if (value.equals(RECORD_AUDIO)) {
        recorderHelper.showNPF(recorderTab, "record_audio");
      } else if (value.equals(RECORD_EXAMPLE)) {
        recordExampleHelper.showNPF(recordExampleTab, "record_example_audio");
      } else if (value.equals(MARK_DEFECTS) && markDefectsTab != null) {
        markDefectsHelper.showNPF(markDefectsTab, CONTENT1);
      } else if (value.equals(PRACTICE) && practiceTab != null) {
        showPracticeTab();
      } else {
        logger.info("selectPreviousTab got unknown value '" + value + "'");
        showDefaultInitialTab();
      }
    } else {
      logger.warning("selectPreviousTab : found value  '" + value + "' " +
          " but I only know about tabs : " + nameToIndex.keySet());
      showDefaultInitialTab();
    }

  }

  /**
   * What to do when we don't know which tab to select
   * Right now show the learn pronunciation tab.
   */
  private void showDefaultInitialTab() {
    checkAndMaybeClearTab(CHAPTERS);
    learnHelper.showNPF(chapters, LEARN);

    TabAndContent tabAndContent = nameToTab.get(CHAPTERS);
    Integer tabIndex = nameToIndex.get(CHAPTERS);
    tabPanel.selectTab(tabIndex);
    clickOnTab(tabAndContent);
  }

  /**
   * @see #showInitialState()
   * @param onlyCreated
   * @param onlyVisited
   */
  private void showMyLists(boolean onlyCreated, boolean onlyVisited, TabPanel tabPanel) {
    String value = storage.getValue(CLICKED_TAB);
  //  logger.info("showMyLists '" + value + "' created " + onlyCreated + " visited " + onlyVisited);

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

    int tabToSelect = onlyCreated ? 0:1;//getSafeTabIndexFor(YOUR_LISTS) : getSafeTabIndexFor(OTHERS_LISTS);

  //  logger.info("Select tab " + tabToSelect + " only " + onlyCreated);

    tabPanel.selectTab(tabToSelect);

    TabAndContent toUse = onlyCreated ? yourStuff : othersStuff;
    clickOnTab(toUse);
    refreshViewLessons(onlyCreated, onlyVisited);
  }

  /**
   * @see mitll.langtest.client.custom.dialog.CreateListDialog#addUserList
   * @param userListID
   */
  public void clickOnYourLists(long userListID) {
    storeCurrentClickedList(userListID);
    storage.storeValue(SUB_TAB, EDIT_ITEM);

    tabPanel.selectTab(getSafeTabIndexFor(YOUR_LISTS));
    clickOnTab(yourStuff);
    refreshViewLessons(true, false);
  }

  /**
   * @see #clickOnYourLists(long)
   * @see #selectPreviouslyClickedSubTab(com.github.gwtbootstrap.client.ui.TabPanel, TabAndContent, TabAndContent, TabAndContent, mitll.langtest.shared.custom.UserList, String, boolean, boolean, boolean, boolean)
   * @see #selectPreviousTab(String)
   * @see #showMyLists
   * @param toUse
   */
  private void clickOnTab(final TabAndContent toUse) {
    if (toUse == null) {
      logger.warning("huh? toUse is nulll???\n\n");
    } else if (toUse.getTab() == null) {
      logger.warning("huh? toUse has a null tab? " + toUse);
    } else {
   //   logger.info("click on tab " + toUse);
      toUse.getTab().fireEvent(new ButtonClickEvent());
    }
  }

  /**
   * @see mitll.langtest.client.custom.Navigation#getNav()
   * @see Navigation#getListOperations(mitll.langtest.shared.custom.UserList, String)
   * @param tabPanel
   * @param iconType
   * @param label
   * @return
   */
  private TabAndContent makeTab(TabPanel tabPanel, IconType iconType, String label) {
    TabAndContent tabAndContent = new TabAndContent(iconType, label);
    tabPanel.add(tabAndContent.getTab().asTabLink());
    return tabAndContent;
  }

  public KeyStorage getStorage() {
    return storage;
  }

  /*To call click() function for Programmatic equivalent of the user clicking the button.*/
  private class ButtonClickEvent extends ClickEvent {}

  private void viewBrowse() { viewLessons(browse.getContent(), true, false, false); }
  

  
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
      // logger.info("viewLessons----> getAll = " + getAll);
      service.getUserListsForText("", controller.getUser(),
          new UserListCallback(this, contentPanel, child, listScrollPanel, LESSONS + "_All", false, true, userManager, onlyMine));
    } else {
      // logger.info("viewLessons for " + userManager.getUser());
      service.getListsForUser(userManager.getUser(), onlyMine,
          onlyVisited,
          new UserListCallback(this, contentPanel, child, listScrollPanel,
              LESSONS + (onlyMine ? "_Mine":"_Others"), onlyMine, false, userManager, onlyMine));
    }
  }

  /**
   * Return all the review lists -- defects (by teachers/QA), comments (by students), and attention LL.
   * @see #getNav()
   * @see #addTabs
   * @param contentPanel
   */
  private void viewReview(final Panel contentPanel) {
    final Panel child = getContentChild(contentPanel);

//    logger.info("------> viewReview : reviewLessons for " + userManager.getUser());

    service.getReviewLists(new AsyncCallback<List<UserList>>() {
      @Override
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(List<UserList> reviewLists) {
        logger.info("\tviewReview : reviewLessons for " + userManager.getUser() + " got " + reviewLists);

        new UserListCallback(Navigation.this, contentPanel, child,
            new ScrollPanel(), REVIEW, false, false, userManager, false).onSuccess(reviewLists);
      }
    });
  }

  /**
   * @see #viewReview
   * @param contentPanel
   * @return
   */
  private Panel getContentChild(Panel contentPanel) {
    contentPanel.clear();
    contentPanel.getElement().setId("defectReview_contentPanel");

    final Panel child = new DivWidget();
    contentPanel.add(child);
    child.addStyleName("exerciseBackground");
    return child;
  }

  @Override
  public void onResize() {
  //  logger.info("got onResize");
    setScrollPanelWidth(listScrollPanel);
    learnHelper.onResize();
    npfHelper.onResize();
    avpHelper.onResize();
    defectHelper.onResize();
    reviewItem.onResize();
    recorderHelper.onResize();
    recordExampleHelper.onResize();
    editItem.onResize();
    markDefectsHelper.onResize();
    practiceHelper.onResize();
  }

  /**
   * @see UserListCallback#getDisplayRowPerList(mitll.langtest.shared.custom.UserList, boolean, boolean)
   * @see UserListCallback#selectPreviousList
   * @param ul
   * @param contentPanel
   */
  void showList(final UserList ul, Panel contentPanel, final String instanceName) {
    //logger.info("showList " + ul + " instance " + instanceName);
    //  if (!ul.isEmpty()) logger.info("\tfirst" + ul.getExercises().iterator().next());
    controller.logEvent(contentPanel,"Tab","UserList_"+ul.getID(),"Show List");

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

  /**
   * @see #showList(mitll.langtest.shared.custom.UserList, com.google.gwt.user.client.ui.Panel, String)
   * @param ul
   * @param instanceName
   * @return
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

    Anchor downloadLink = getDownloadLink(ul.getUniqueID(), instanceName + "_" + ul.getUniqueID(), ul.getName());
    Node child = downloadLink.getElement().getChild(0);
    AnchorElement.as(child).getStyle().setColor("#333333");

    r1.add(downloadLink);

    container.add(r1);

    TabPanel listOperations = getListOperations(ul, instanceName);
    container.add(listOperations);
    return container;
  }

  /**
   * @see #makeTabContent
   * @param listid
   * @param linkid
   * @param name
   * @return
   */
  private Anchor getDownloadLink(long listid, String linkid, final String name) {
    final Anchor downloadLink = new Anchor(getURLForDownload(listid));
    new TooltipHelper().addTooltip(downloadLink, "Download spreadsheet and audio for list.");
    downloadLink.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        controller.logEvent(downloadLink, "DownloadLink", "N/A", "downloading audio for " + name);
      }
    });
    downloadLink.getElement().setId("DownloadLink_" + linkid);
    downloadLink.addStyleName("leftFiveMargin");
    return downloadLink;
  }

  /**
   * @seex #showSelectionState(mitll.langtest.client.list.SelectionState)
   * @paramx xselectionState
   * @return
   */
  private SafeHtml getURLForDownload(long listid) {
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    sb.appendHtmlConstant("<a class='" +"icon-download"+
        "' href='" +
        "downloadAudio" +
        "?list=" + listid+
        "'" +
        ">");
    sb.appendEscaped(" Download");
    sb.appendHtmlConstant("</a>");
    return sb.toSafeHtml();
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
   * @see #showList(mitll.langtest.shared.custom.UserList, com.google.gwt.user.client.ui.Panel, String)
   * @param ul
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
   * @see #showList(mitll.langtest.shared.custom.UserList, com.google.gwt.user.client.ui.Panel, String)
   * @param ul
   * @param instanceName
   * @return
   */
  private TabPanel getListOperations(final UserList ul, final String instanceName) {
    boolean created = createdByYou(ul) || instanceName.equals(REVIEW) || instanceName.equals(COMMENT);

    final TabPanel tabPanel = new TabPanel();
    logger.info("getListOperations : '" + instanceName + " for list " + ul);
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
      practice = makeTab(tabPanel, IconType.CHECK, PRACTICE);
      final TabAndContent fpractice = practice;
      practice.getContent().addStyleName("centerPractice");
      practice.getTab().addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          storage.storeValue(SUB_TAB, PRACTICE1);
        //   logger.info("getListOperations : got click on practice " + fpractice.getContent());
          avpHelper.setContentPanel(fpractice.getContent());
          avpHelper.showNPF(ul, fpractice, PRACTICE1, true);
          controller.logEvent(fpractice.getTab(), "Tab", "UserList_" + ul.getID(), PRACTICE1);
        }
      });
    }

    // add add item and edit tabs (conditionally)
    TabAndContent editItemTab = null;
    if (created && (!ul.isPrivate() || ul.getCreator().getId() == controller.getUser())) {
      final TabAndContent editTab = makeTab(tabPanel, IconType.EDIT, isReview ? ADD_DELETE_EDIT_ITEM :ADD_OR_EDIT_ITEM);
      editItemTab = editTab;
      editTab.getTab().addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          storage.storeValue(SUB_TAB, EDIT_ITEM);
          controller.logEvent(editTab.getTab(), "Tab", "UserList_" + ul.getID(), EDIT_ITEM);
          if ((isReview || isComment)) {
            reviewItem.showNPF(ul, editTab, (isReview ? REVIEW : COMMENT) + "_edit", false);
          } else {
            showEditItem(ul, editTab, Navigation.this.editItem, !ul.isFavorite());
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

  /**
   * @see #getListOperations
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
   */
  private void selectTabGivenHistory(TabPanel tabPanel, TabAndContent learn, TabAndContent practice,
                                     TabAndContent edit,
                                     UserList ul, String instanceName1, boolean isReview, boolean isComment,
                                     boolean isAttention, boolean isNormalList) {
    boolean chosePrev = selectPreviouslyClickedSubTab(tabPanel, learn, practice, edit,
        ul, instanceName1, isReview, isComment, isAttention, isNormalList);

    if (!chosePrev) {
      if (createdByYou(ul) && !ul.isPrivate() && ul.isEmpty() && edit != null) {
        tabPanel.selectTab(ul.getName().equals(FIX_DEFECTS) ? 0 : SUBTAB_EDIT_INDEX);    // 2 = add/edit item
        showEditReviewOrComment(ul, isNormalList, edit, isReview,isComment);
      } else {
        tabPanel.selectTab(SUBTAB_LEARN_INDEX);
        showLearnTab(learn, ul, instanceName1, isReview, isComment, isAttention);
      }
    }
  }

  private void showLearnTab(TabAndContent learnTab, UserList ul, String instanceName1, boolean isReview,
                            boolean isComment, boolean isAttention) {
    showLearnTab(isReview || isComment || isAttention, ul, learnTab, instanceName1);
  }

  /**
   * @see #getListOperations
   * @see #selectTabGivenHistory
   * @param isReview
   * @param ul
   * @param learn
   * @param instanceName1
   */
  private void showLearnTab(boolean isReview, UserList ul, TabAndContent learn, String instanceName1) {
    if (isReview) {
      //logger.info("showLearnTab : onClick using defect helper " + instanceName1 + " and " +ul);
      defectHelper.showNPF(ul, learn, instanceName1, false);
    }
    else {
      //logger.info("showLearnTab : onClick using npf helper " + instanceName1 + " and " +ul);
      npfHelper.showNPF(ul, learn, instanceName1, true);
    }
  }

  /**
   * @see #selectTabGivenHistory(com.github.gwtbootstrap.client.ui.TabPanel, TabAndContent, TabAndContent, TabAndContent, mitll.langtest.shared.custom.UserList, String, boolean, boolean, boolean, boolean)
   * @param ul
   * @param isNormalList
   * @param finalEditItem
   * @param isReview
   * @param isComment
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

  /**
   * @see #selectTabGivenHistory
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
   */
  private boolean selectPreviouslyClickedSubTab(TabPanel tabPanel,
                                                TabAndContent learnTab,
                                                TabAndContent practiceTab,
                                                TabAndContent editTab,
                                                UserList ul, String instanceName1,
                                                boolean isReview, boolean isComment, boolean isAttention,
                                                boolean isNormalList) {
    String subTab = storage.getValue(SUB_TAB);
    //logger.info("selectPreviouslyClickedSubTab : subtab " + subTab);

    boolean chosePrev = false;
    if (subTab != null) {
      chosePrev = true;
      if (subTab.equals(LEARN)) {
        tabPanel.selectTab(SUBTAB_LEARN_INDEX);
        clickOnTab(learnTab);
        showLearnTab(learnTab, ul, instanceName1, isReview, isComment, isAttention);
      } else if (subTab.equals(PRACTICE1)) {
        tabPanel.selectTab(SUBTAB_PRACTICE_INDEX);
        clickOnTab(practiceTab);
        avpHelper.setContentPanel(practiceTab.getContent());
        avpHelper.showNPF(ul, practiceTab, PRACTICE1, true);
      } else if (subTab.equals(EDIT_ITEM)) {
        boolean reviewOrComment = isReview || isComment;
        tabPanel.selectTab(reviewOrComment ? 1 : SUBTAB_EDIT_INDEX);
        clickOnTab(editTab);

        if (reviewOrComment) {
          reviewItem.showNPF(ul, editTab, (isReview ? REVIEW : COMMENT) + "_edit", false);
        } else {
          showEditItem(ul, editTab, this.editItem, isNormalList && !ul.isFavorite());
        }

      } else {
        chosePrev = false;
      }
    }
    return chosePrev;
  }

  /**
   * @see #getListOperations
   * @param ul
   * @param addItem
   */
  private void showEditItem(UserList ul, TabAndContent addItem, EditItem editItem, boolean includeAddItem) {
    addItem.getContent().clear();
    addItem.getContent().add(editItem.editItem(ul,
        new InlineLabel(),   // TODO get rid of this entirely
        includeAddItem));
  }

  /**
   * @see #onResize()
   * @param row
   */
  private void setScrollPanelWidth(ScrollPanel row) {
    if (row != null) {
      row.setHeight((Window.getClientHeight() * 0.7) + "px");
    }
  }

  void deleteList(Button delete, final UserList ul, final boolean onlyMyLists) {
    controller.logEvent(delete, "Button", "UserList_" + ul.getID(), "Delete");
    service.deleteList(ul.getUniqueID(), new AsyncCallback<Boolean>() {
      @Override
      public void onFailure(Throwable caught) {}

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
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(Void result) {}
    });
  }


  private boolean createdByYou(UserList ul) {
    return ul.getCreator().getId() == userManager.getUser();
  }

}
