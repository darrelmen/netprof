package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.TabLink;
import com.github.gwtbootstrap.client.ui.TabPanel;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.analysis.AnalysisTab;
import mitll.langtest.client.bootstrap.FlexSectionExerciseList;
import mitll.langtest.client.contextPractice.DialogWindow;
import mitll.langtest.client.custom.content.AVPHelper;
import mitll.langtest.client.custom.content.ChapterNPFHelper;
import mitll.langtest.client.custom.content.NPFHelper;
import mitll.langtest.client.custom.content.ReviewItemHelper;
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
import mitll.langtest.shared.ContextPractice;
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
  private final Logger logger = Logger.getLogger("Navigation");

  private static final String CHAPTERS = "Learn Pronunciation";
  private static final String YOUR_LISTS = "Study Your Lists";
  private static final String STUDY_LISTS = "Study Lists";
  private static final String OTHERS_LISTS = "Study Visited Lists";
  private static final String PRACTICE = "Audio Vocabulary Practice";
  private static final String ANALYSIS = "Analysis";

  public static final String REVIEW = "review";
  public static final String COMMENT = "comment";
  private static final String MARK_DEFECTS = "Mark Defects";
  private static final String PRACTICE_DIALOG = "Practice Dialog";
  private static final String FIX_DEFECTS = "Fix Defects";
  private static final String CREATE = "Create a New List";
  private static final String BROWSE = "Browse Lists";
  public static final String CLICKED_USER_LIST = "clickedUserList";
  private static final String CLICKED_TAB = "clickedTab";

  private static final String LEARN = "learn";
  private static final String RECORD_AUDIO = "Record Audio";
  private static final String RECORD_EXAMPLE = "Record In-context Audio";
  private static final String CONTENT1 = "content";
  private static final String MARK_DEFECTS1 = "markDefects";

  private final ExerciseController controller;
  private final LangTestDatabaseAsync service;
  private final UserManager userManager;
  private final SimpleChapterNPFHelper practiceHelper;

  private final NPFHelper npfHelper;
  private final NPFHelper avpHelper;
  private DialogWindow dialogWindow;

  private final EditItem editItem;

  private final ChapterNPFHelper defectHelper;
  private final SimpleChapterNPFHelper recorderHelper, recordExampleHelper;
  private final SimpleChapterNPFHelper markDefectsHelper, learnHelper;
  private final ReviewItemHelper reviewItem;

  private final KeyStorage storage;
  private ListManager listManager;
  private AnalysisPlot analysisPlot;
  private UserFeedback feedback;

  private TabPanel tabPanel;
  private TabAndContent studyLists;
  private TabAndContent dialog;
  private TabAndContent chapters;
  private TabAndContent analysis;
  private TabAndContent review, recorderTab, recordExampleTab, markDefectsTab, practiceTab;

  private final Map<String, TabAndContent> nameToTab = new HashMap<String, TabAndContent>();
  private final Map<String, Integer> nameToIndex = new HashMap<String, Integer>();

  private final List<TabAndContent> tabs = new ArrayList<TabAndContent>();

  /**
   * @param service
   * @param userManager
   * @param controller
   * @param feedback
   * @paramx predefinedContentList
   * @see mitll.langtest.client.LangTest#populateRootPanel()
   */
  public Navigation(final LangTestDatabaseAsync service, final UserManager userManager,
                    final ExerciseController controller, UserFeedback feedback) {
    this.service = service;
    this.userManager = userManager;
    this.controller = controller;
    this.feedback = feedback;
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

    service.getContextPractice(new AsyncCallback<ContextPractice>() {
      public void onSuccess(ContextPractice cpw) {
        dialogWindow = new DialogWindow(service, controller, cpw);
      }

      public void onFailure(Throwable caught) {
        logger.info("getContextPractice failed");
      }
       //TODO: this is naughty
    });

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
   * @param service
   * @param userManager
   * @param controller
   * @param feedback
   * @return
   * @see #Navigation(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserManager, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.user.UserFeedback)
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
                  } else {
                    statsFlashcardFactory.populateCorrectMap();
                    return shell;
                  }
                } else {
                  return super.findFirstExercise();
                }
              }

              @Override
              protected void onLastItem() {
                statsFlashcardFactory.resetStorage();
              }

              @Override
              protected void loadExercisesUsingPrefix(Map<String, Collection<String>> typeToSection, String prefix, boolean onlyWithAudioAnno) {
                super.loadExercisesUsingPrefix(typeToSection, prefix, onlyWithAudioAnno);
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

  /**
   * @return
   * @see #getNav
   * @see mitll.langtest.client.LangTest#populateRootPanel()
   */
  public Widget getNav() {
    tabPanel = new TabPanel();
    tabPanel.getElement().getStyle().setMarginTop(-8, Style.Unit.PX);
    tabPanel.getElement().setId("tabPanel");
    this.listManager = new ListManager(service,userManager,controller,feedback,tabPanel);

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

    addLearnTab();

    addPracticeTab();

    addStudyLists();

    if (controller.getProps().useAnalysis()) {
      addAnalysis();
    }

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
          listManager.viewReview(review.getContent());
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

  private void addAnalysis() {
    analysis = makeFirstLevelTab(tabPanel, IconType.TROPHY, ANALYSIS);
    analysis.getTab().addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        checkAndMaybeClearTab(ANALYSIS);
        logEvent(dialog, ANALYSIS);
        showAnalysis();
      }
    });
  }

  private void showAnalysis() {
    analysis.getContent().clear();
//    analysisPlot = new AnalysisPlot(service, userManager.getUser());
//    analysis.getContent().add(analysisPlot);
//    learnHelper.getExerciseList().getExercises();
//    new VerticalPanel();

    ListInterface exerciseList = npfHelper.getExerciseList();

    logger.info("Got " + exerciseList);
    analysis.getContent().add(new AnalysisTab(service,controller,exerciseList,userManager.getUser()));

  }

  private void addStudyLists() {
    studyLists = makeFirstLevelTab(tabPanel, IconType.FOLDER_CLOSE, STUDY_LISTS);
    listManager.addStudyLists(studyLists);
  }

  private void addLearnTab() {
    chapters = makeFirstLevelTab(tabPanel, IconType.LIGHTBULB, CHAPTERS);
    chapters.getTab().addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        checkAndMaybeClearTab(CHAPTERS);
        learnHelper.showNPF(chapters, LEARN);
        logEvent(chapters, CHAPTERS);
      }
    });
  }

  /**
   * Make the practice tab...
   *
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
  //    logger.info(" ------- showPracticeTab make practice tab  - ");
      practiceHelper.showNPF(practiceTab, PRACTICE);
      practiceHelper.setContentPanel(practiceTab.getContent());
      practiceHelper.hideList();
    }
  }

  private void addDialogTab() {
    if (controller.getProps().hasDialog()) {
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

  private boolean isQC() {
    return controller.getPermissions().contains(User.Permission.QUALITY_CONTROL);
  }

  private void logEvent(TabAndContent yourStuff, String context) {
    if (yourStuff != null && yourStuff.getTab() != null) {
      controller.logEvent(yourStuff.getTab().asWidget(), "Tab", "", context);
    }
  }

  private TabAndContent makeFirstLevelTab(TabPanel tabPanel, IconType iconType, String label) {
    TabAndContent tabAndContent = makeTab(tabPanel, iconType, label);
    nameToIndex.put(label, tabs.size());
    tabs.add(tabAndContent);
    nameToTab.put(label, tabAndContent);
    return tabAndContent;
  }

  private void checkAndMaybeClearTab(String value) {
    //   String value1 = storage.getValue(CLICKED_TAB);
    //logger.info("checkAndMaybeClearTab " + value1 + " vs "+value + " clearing " + CLICKED_USER_LIST);
    storage.removeValue(CLICKED_USER_LIST);
    storage.storeValue(CLICKED_TAB, value);
  }

  /**
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
        public void onFailure(Throwable caught) {
        }

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
            listManager.showMyLists(foundCreated, !foundCreated);
          }
        }
      });
    } else {
      selectPreviousTab(value);
    }
  }

  public void refreshInitialState() {
    String value = storage.getValue(CLICKED_TAB);
    if (value.isEmpty()) {   // no previous tab
      showDefaultInitialTab();
    } else {
      selectPreviousTab(value);
    }
  }

  /**
   * @see #showInitialState()
   */
  private void selectPreviousTab(String value) {
    TabAndContent tabAndContent = nameToTab.get(value);
    Integer tabIndex = nameToIndex.get(value);

    logger.info("selectPreviousTab '" + value + "' index " + tabIndex + " tabAndContent " +tabAndContent);

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
        listManager.showMyLists(true, false);
      } else if (value.equals(OTHERS_LISTS)) {
        listManager.showMyLists(false, true);
      } else if (value.equals(STUDY_LISTS)) {
        listManager.clickOnYourStuff();
        DivWidget content = studyLists.getContent();
        Widget widget = content.getWidget(0);

        int tab = orig.equals(YOUR_LISTS) ? 0 : orig.equals(OTHERS_LISTS) ? 1 : orig.equals(CREATE) ? 2 : orig.equals(BROWSE) ? 3 : 0;
        logger.info("selectPreviousTab Select tab " + tab + " orig " + orig);
        listManager.showFirstUserListTab((TabPanel) widget, tab);
        if (tab == 0) {
          listManager.showMyLists(true, false);
        } else if (tab == 1) {
          listManager.showMyLists(false, true);
        } else if (tab == 2) {
          listManager.clickOnCreate();
        } else {
          listManager.viewBrowse(); // CHAPTERS
        }
      } else if (value.equals(BROWSE)) {
        listManager.viewBrowse(); // CHAPTERS
      } else if (value.equals(FIX_DEFECTS)) {
        listManager.viewReview(review.getContent());
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
      } else if (value.equals(ANALYSIS) && analysis != null) {
        showAnalysis();
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
   * @param toUse
   * @seex #clickOnYourLists(long)
   * @seex #selectPreviouslyClickedSubTab(com.github.gwtbootstrap.client.ui.TabPanel, TabAndContent, TabAndContent, TabAndContent, mitll.langtest.shared.custom.UserList, String, boolean, boolean, boolean, boolean)
   * @see #selectPreviousTab(String)
   * @seex #showMyLists
   */
  private void clickOnTab(final TabAndContent toUse) {
    if (toUse == null) {
      logger.warning("clickOnTab : huh? toUse is nulll???\n\n");
    } else if (toUse.getTab() == null) {
      logger.warning("huh? toUse has a null tab? " + toUse);
    } else {
      logger.info("click on tab " + toUse);
      toUse.clickOnTab();
    }
  }

  /**
   * @param tabPanel
   * @param iconType
   * @param label
   * @return
   * @see mitll.langtest.client.custom.Navigation#getNav()
   * @see Navigation#getListOperations(mitll.langtest.shared.custom.UserList, String)
   */
  private TabAndContent makeTab(TabPanel tabPanel, IconType iconType, String label) {
    return new TabAndContent(tabPanel, iconType, label);
  }

  @Override
  public void onResize() {
    //  logger.info("got onResize");
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
    listManager.onResize();
  }

  private boolean createdByYou(UserList ul) {
    return ul.getCreator().getId() == userManager.getUser();
  }
}
