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

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.analysis.AnalysisTab;
import mitll.langtest.client.analysis.ShowTab;
import mitll.langtest.client.analysis.StudentAnalysis;
import mitll.langtest.client.contextPractice.DialogViewer;
import mitll.langtest.client.contextPractice.DialogWindow;
import mitll.langtest.client.custom.content.FlexListLayout;
import mitll.langtest.client.custom.content.NPFlexSectionExerciseList;
import mitll.langtest.client.custom.exercise.CommentNPFExercise;
import mitll.langtest.client.custom.exercise.ContextCommentNPFExercise;
import mitll.langtest.client.custom.tabs.TabAndContent;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.scoring.GoodwaveExercisePanel;
import mitll.langtest.client.services.ExerciseService;
import mitll.langtest.client.services.ExerciseServiceAsync;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.client.userops.UserOps;
import mitll.langtest.shared.ContextPractice;
import mitll.langtest.shared.analysis.WordAndScore;
import mitll.langtest.shared.analysis.WordScore;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.user.User;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 9/27/13
 * Time: 8:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class Navigation implements RequiresResize, ShowTab {
  private final Logger logger = Logger.getLogger("Navigation");

  private static final String STUDENT_ANALYSIS = "Student Analysis";
  //private static final String CUSTOM = "Custom";
  public static final String CLASSROOM = "classroom";

  private static final String CHAPTERS = "Learn Pronunciation";
  private static final String USERS = "Users";
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
  static final String CLICKED_USER_LIST = "clickedUserList";
  private static final String CLICKED_TAB = "clickedTab";

  private static final String LEARN = "learn";
  private static final String RECORD_AUDIO = "Record Audio";
  private static final String RECORD_EXAMPLE = "Record In-context Audio";
  private static final String CONTENT1 = "content";
  private static final String MARK_DEFECTS1 = "markDefects";

  private static final int STUDY_LISTS_INDEX = 2;

  private final ExerciseController controller;
  private final LangTestDatabaseAsync service;
  private final ExerciseServiceAsync exerciseServiceAsync = GWT.create(ExerciseService.class);

  private final UserManager userManager;
  private final SimpleChapterNPFHelper practiceHelper;

  private DialogViewer dialogWindow;

  private final SimpleChapterNPFHelper recorderHelper, recordExampleHelper;
  private final SimpleChapterNPFHelper markDefectsHelper, learnHelper;

  private final KeyStorage storage;
  private ListManager listManager;
  private final UserFeedback feedback;

  private TabPanel tabPanel;
  private TabAndContent studyLists;
  private TabAndContent users;
  private TabAndContent dialog;
  private TabAndContent chapters;
  private TabAndContent analysis, studentAnalysis;
  private TabAndContent review, recorderTab, recordExampleTab, markDefectsTab;
  private TabAndContent practiceTab;

  private final Map<String, TabAndContent> nameToTab = new HashMap<>();
  private final Map<String, Integer> nameToIndex = new HashMap<>();

  private final List<TabAndContent> tabs = new ArrayList<>();

  /**
   * @param service
   * @param userManager
   * @param controller
   * @param feedback
   * @paramx predefinedContentList
   * @see mitll.langtest.client.InitialUI#populateBelowHeader
   */
  public Navigation(final LangTestDatabaseAsync service, final UserManager userManager,
                    final ExerciseController controller, UserFeedback feedback) {
    this.service = service;
    this.userManager = userManager;
    this.controller = controller;
    this.feedback = feedback;
    storage = new KeyStorage(controller);

    learnHelper = new SimpleChapterNPFHelper<CommonShell, CommonExercise>(service, feedback, userManager, controller, null,
        exerciseServiceAsync) {
      @Override
      protected FlexListLayout<CommonShell, CommonExercise> getMyListLayout(UserManager userManager,
                                                                            SimpleChapterNPFHelper<CommonShell, CommonExercise> outer) {
        return new MyFlexListLayout<CommonShell, CommonExercise>(service, feedback, controller, outer, exerciseServiceAsync) {
          @Override
          protected PagingExerciseList<CommonShell, CommonExercise> makeExerciseList(Panel topRow,
                                                                                     Panel currentExercisePanel,
                                                                                     String instanceName, boolean incorrectFirst) {
            return new NPFlexSectionExerciseList(this, topRow, currentExercisePanel, instanceName, incorrectFirst);
          }
        };
      }

      protected ExercisePanelFactory<CommonShell, CommonExercise> getFactory(final PagingExerciseList<CommonShell, CommonExercise> exerciseList) {
        return new ExercisePanelFactory<CommonShell, CommonExercise>(service, feedback, controller, exerciseList) {
          @Override
          public Panel getExercisePanel(CommonExercise e) {
            if (controller.getProps().canPracticeContext()) {
              return new ContextCommentNPFExercise<>(e, controller, exerciseList, false, CLASSROOM);
            } else {
              return new CommentNPFExercise<>(e, controller, exerciseList, false, CLASSROOM);
            }
          }
        };
      }
    };

    if (controller.getProps().hasDialog()) {
      makeDialogWindow(service, controller);
    }

    markDefectsHelper = new MarkDefectsChapterNPFHelper(service, feedback, userManager, controller, learnHelper, exerciseServiceAsync);
    practiceHelper = new PracticeHelper(service, feedback, userManager, controller, exerciseServiceAsync);
    recorderHelper = new RecorderNPFHelper(service, feedback, userManager, controller, true, learnHelper, exerciseServiceAsync);
    recordExampleHelper = new RecorderNPFHelper(service, feedback, userManager, controller, false, learnHelper, exerciseServiceAsync);

  //  setKindToIcon();
  }

  public boolean isRTL() {
    return controller.getProps().isRightAlignContent();// || (learnHelper.getExerciseList() != null && learnHelper.getExerciseList().isRTL());
  }

  private void makeDialogWindow(final LangTestDatabaseAsync service, final ExerciseController controller) {
    GWT.runAsync(new RunAsyncCallback() {
      public void onFailure(Throwable caught) {
        downloadFailedAlert();
      }

      public void onSuccess() {
        service.getContextPractice(new AsyncCallback<ContextPractice>() {
          public void onSuccess(ContextPractice cpw) {
            logger.info("run async to get dialog ui");
            dialogWindow = new DialogWindow(service, controller, cpw);
          }

          public void onFailure(Throwable caught) {
            logger.info("getContextPractice failed");
          }
          //TODO: this is naughty
        });
      }
    });
  }

  /**
   * TODO : clean this up - why a horrible hack for learn tab?
   *
   * @return
   * @see #getTabPanel
   * @see mitll.langtest.client.InitialUI#populateBelowHeader
   */
  public Widget getTabPanel() {
    tabPanel = new TabPanel();
    tabPanel.getElement().getStyle().setMarginTop(-8, Style.Unit.PX);
    tabPanel.getElement().setId("tabPanel");
    this.listManager = new ListManager(service, userManager, controller, feedback, tabPanel, learnHelper, exerciseServiceAsync);

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
   * Show student analysis by default.
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
    addLearnTab();
    addPracticeTab();
    addStudyLists();
    if (userManager.getCurrent().getUserKind() != User.Kind.STUDENT || userManager.getCurrent().isAdmin()) {
      addUserMaintenance();
    }
    addAnalysis();

    if (controller.getProps().useAnalysis()) {
      if (userManager.isTeacher()) {
        addTeacherAnalysis();
      }
    }

    if (isQC()) {
      addDefectsTabs();
    }

    if (permittedToRecord()) {
      addRecordTabs();
    }
  }

  private void addRecordTabs() {
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

  /**
   * @see #addTabs()
   */
  private void addDefectsTabs() {
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

  /**
   * @see #addTabs()
   */
  private void addAnalysis() {
    analysis = makeFirstLevelTab(tabPanel, IconType.TROPHY, ANALYSIS);
    analysis.getTab().addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        checkAndMaybeClearTab(ANALYSIS);
        logEvent(analysis, ANALYSIS);
        showAnalysis();
      }
    });
  }

  /**
   * @see #addTabs()
   */
  private void addTeacherAnalysis() {
    studentAnalysis = makeFirstLevelTab(tabPanel, IconType.TROPHY, STUDENT_ANALYSIS);
    studentAnalysis.getTab().addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        checkAndMaybeClearTab(STUDENT_ANALYSIS);
        logEvent(studentAnalysis, STUDENT_ANALYSIS);
        showStudentAnalysis();
      }
    });
  }

  /**
   * @see #addAnalysis()
   */
  private void showAnalysis() {
    learnHelper.showNPF(chapters, LEARN);

    analysis.getContent().clear();
    ShowTab showTab = this;
    AnalysisTab w = new AnalysisTab(exerciseServiceAsync, controller, getUser(), showTab, userManager.getUserID(), 1, null);
    analysis.getContent().add(w);
  }

  /**
   * @see #addAnalysis()
   */
  private void showStudentAnalysis() {
    learnHelper.showNPF(chapters, LEARN);

    studentAnalysis.getContent().clear();
    studentAnalysis.getContent().add(new StudentAnalysis(exerciseServiceAsync, controller, this));
  }

  /**
   * @see #addTabs
   */
  private void addStudyLists() {
    studyLists = makeFirstLevelTab(tabPanel, IconType.FOLDER_CLOSE, STUDY_LISTS);
    listManager.addStudyLists(studyLists);
  }

  private void addUserMaintenance() {
    users = makeFirstLevelTab(tabPanel, IconType.GROUP, USERS);

    users.getTab().addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        checkAndMaybeClearTab(USERS);
        // learnHelper.showNPF(chapters, LEARN);
        new UserOps(controller,userManager).showUsers(users);


        logEvent(users, USERS);
      }
    });
  }

/*
  Map<User.Kind, Label> kindToLabel = new HashMap<>();
  Map<User.Kind, IconType> kindToIcon = new HashMap<>();

  private void setKindToIcon() {
    kindToIcon.put(User.Kind.STUDENT, IconType.USER);
    kindToIcon.put(User.Kind.TEACHER, IconType.GROUP);
    kindToIcon.put(User.Kind.CONTENT_DEVELOPER, IconType.PENCIL);
    kindToIcon.put(User.Kind.PROJECT_ADMIN, IconType.BOLT);
    kindToIcon.put(User.Kind.ADMIN, IconType.ANDROID);
  }
*/

 /* private void showUsers() {
    DivWidget content = users.getContent();
    content.clear();

    NavList w = new NavList();
    w.setWidth("180px");
    content.add(w);

    w.add(new NavHeader("Users"));
    NavLink first = null;
    DivWidget w1 = new DivWidget();
    for (User.Kind kind : User.Kind.values()) {
      if (kind.shouldShow()) {
        NavLink userLink = getUserLink(kind, w1);
        w.add(userLink);
        if (first == null) first = userLink;
      }
    }
    userManager.getCounts(kindToLabel);

    content.add(w1);
    w1.getElement().setId("userContent");
    w1.getElement().getStyle().setMarginLeft(220, Style.Unit.PX);

    logger.info("Loaded everything...");

    final NavLink toClick = first;
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        if (toClick != null) {
          toClick.fireEvent(new ButtonClickEvent());
          logger.info("Fire click event on " + toClick.getElement().getId());
        }
      }
    });
  }*/

  /*private class ButtonClickEvent extends ClickEvent {
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
        userManager.getUserService().getKindToUser(new AsyncCallback<Map<User.Kind, Collection<MiniUser>>>() {
          @Override
          public void onFailure(Throwable throwable) {

          }

          @Override
          public void onSuccess(Map<User.Kind, Collection<MiniUser>> kindCollectionMap) {
            content.clear();
            Collection<MiniUser> miniUsers = kindCollectionMap.get(kind);
            content.add(getUsers(miniUsers));
          }
        });
      }

      private DivWidget getUsers(Collection<MiniUser> miniUsers) {
        return new OpsUserContainer(controller, kind.getName()).getTable(miniUsers, kind.getName(), "");
      }
    });
    return students;
  }*/

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
      //practiceHelper.setContentPanel(practiceTab.getContent());
      practiceHelper.hideList();
    }
  }

  private void addDialogTab() {
    if (controller.getProps().hasDialog()) {
      GWT.runAsync(new RunAsyncCallback() {
        public void onFailure(Throwable caught) {
          downloadFailedAlert();
        }

        public void onSuccess() {
          reallyAddDialogTab();
          showPreviouslySelectedTab();
        }
      });
    }
  }

  private void reallyAddDialogTab() {
    dialog = makeFirstLevelTab(tabPanel, IconType.TH_LIST, PRACTICE_DIALOG);
    dialog.getTab().addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        checkAndMaybeClearTab(PRACTICE_DIALOG);
        logEvent(dialog, PRACTICE_DIALOG);
        dialog.getContent().getElement().setId("contentPanel");
        dialogWindow.viewDialog(dialog.getContent());
      }
    });
  }

  private void downloadFailedAlert() {
    Window.alert("Code download failed");
  }

  private boolean isQC() {
    return controller.getUserState().getPermissions().contains(User.Permission.QUALITY_CONTROL) || controller.getUserState().isAdmin();
  }

  private boolean permittedToRecord() {
    return controller.getUserState().getPermissions().contains(User.Permission.RECORD_AUDIO) || controller.getUserState().isAdmin();
  }

  private void logEvent(TabAndContent yourStuff, String context) {
    if (yourStuff != null && yourStuff.getTab() != null) {
      controller.logEvent(yourStuff.getTab().asWidget(), "Tab", "", context);
    }
  }

  private TabAndContent makeFirstLevelTab(TabPanel tabPanel, IconType iconType, String label) {
    TabAndContent tabAndContent = makeTab(tabPanel, iconType, label);
    int size = tabs.size();
    nameToIndex.put(label, size);
    tabs.add(tabAndContent);
    nameToTab.put(label, tabAndContent);
    return tabAndContent;
  }

  /**
   * @see mitll.langtest.client.InitialUI#configureUIGivenUser
   */
  public void showInitialState() {
    addTabs();
    if (noPrevClickedTab()) {   // no previous tab
      showDefaultInitialTab(true);
    } else {
      selectPreviousTab();
    }
  }

  private int getUser() {
    return userManager.getUser();
  }

  /**
   *
   */
  public void showPreviouslySelectedTab() {
    if (noPrevClickedTab()) {   // no previous tab
      showDefaultInitialTab(true);
    } else {
      selectPreviousTab();
    }
  }

  private void checkAndMaybeClearTab(String value) {
//    String value1 = getClickedTab();
//    logger.info("checkAndMaybeClearTab " + value1 + " vs " + value + " clearing " + CLICKED_USER_LIST);
    storage.removeValue(CLICKED_USER_LIST);
    storage.storeValue(CLICKED_TAB, value);
  }

  private boolean noPrevClickedTab() {
    String value = getClickedTab();
//    logger.info("noPrevClickedTab selected tab = " + value);
    return value.isEmpty();
  }

  private String getClickedTab() {
    return storage.getValue(CLICKED_TAB);
  }

  /**
   * @see #showInitialState()
   */
  private void selectPreviousTab() {
    String clickedTab = getClickedTab();
    TabAndContent tabAndContent = nameToTab.get(clickedTab);
    Integer tabIndex = nameToIndex.get(clickedTab);

    logger.info("selectPreviousTab '" + clickedTab + "' index " + tabIndex + " tabAndContent " + tabAndContent);

    String orig = clickedTab;
    if (tabIndex == null) {
      if (clickedTab.equals(OTHERS_LISTS) ||
          clickedTab.equals(YOUR_LISTS) ||
          clickedTab.equals(CREATE) ||
          clickedTab.equals(BROWSE)) {
        clickedTab = STUDY_LISTS;
        tabIndex = nameToIndex.get(clickedTab);
      }
    }
    if (tabIndex != null) {
      tabPanel.selectTab(tabIndex);
      clickOnTab(tabAndContent);

      if (clickedTab.equals(YOUR_LISTS)) {
        listManager.showMyLists(true, false);
      } else if (clickedTab.equals(OTHERS_LISTS)) {
        listManager.showMyLists(false, true);
      } else if (clickedTab.equals(STUDY_LISTS)) {
        listManager.clickOnYourStuff();
        DivWidget content = studyLists.getContent();
        Widget widget = content.getWidget(0);

        int tab = orig.equals(YOUR_LISTS) ? 0 : orig.equals(OTHERS_LISTS) ? 1 : orig.equals(CREATE) ? 2 : orig.equals(BROWSE) ? 3 : 0;
        //  logger.info("selectPreviousTab Select tab " + tab + " orig " + orig);
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
      } else if (clickedTab.equals(BROWSE)) {
        listManager.viewBrowse(); // CHAPTERS
      } else if (clickedTab.equals(FIX_DEFECTS)) {
        listManager.viewReview(review.getContent());
      } else if (clickedTab.equals(PRACTICE_DIALOG)) {
        dialogWindow.viewDialog(dialog.getContent());
      } else if (clickedTab.equals(CHAPTERS)) {
        learnHelper.showNPF(chapters, LEARN);
      } else if (clickedTab.equals(USERS)) {
        new UserOps(controller,userManager).showUsers(users);
      } else if (clickedTab.equals(RECORD_AUDIO)) {
        recorderHelper.showNPF(recorderTab, AudioType.RECORDER.toString());
      } else if (clickedTab.equals(RECORD_EXAMPLE)) {
        recordExampleHelper.showNPF(recordExampleTab, AudioType.CONTEXT_REGULAR.toString());
      } else if (clickedTab.equals(MARK_DEFECTS) && markDefectsTab != null) {
        markDefectsHelper.showNPF(markDefectsTab, CONTENT1);
      } else if (clickedTab.equals(PRACTICE) && practiceTab != null) {
        showPracticeTab();
      } else if (clickedTab.equals(ANALYSIS) && analysis != null) {
        showAnalysis();
      } else if (clickedTab.equals(STUDENT_ANALYSIS) && studentAnalysis != null) {
        showStudentAnalysis();
      } else {
        logger.info("selectPreviousTab got unknown clickedTab '" + clickedTab + "'");
        showDefaultInitialTab(true);
      }
    } else {
      logger.warning("selectPreviousTab : found clickedTab  '" + clickedTab + "' " +
          " but I only know about tabs : " + nameToIndex.keySet());
      showDefaultInitialTab(false);
    }
  }

  /**
   * What to do when we don't know which tab to select
   * Right now show the learn pronunciation tab.
   *
   * @param setClickedStorage
   */
  private void showDefaultInitialTab(boolean setClickedStorage) {
    if (setClickedStorage) {
      checkAndMaybeClearTab(CHAPTERS);
    }
    learnHelper.showNPF(chapters, LEARN);

    tabPanel.selectTab(nameToIndex.get(CHAPTERS));
    clickOnTab(nameToTab.get(CHAPTERS));
  }

  /**
   * Horrible hack - how do we know where to jump to from clicking on an item in analysis?
   * Is the item in a user list?
   *
   * @param exid
   * @see mitll.langtest.client.analysis.PhoneExampleContainer#gotClickOnItem(WordAndScore)
   * @see mitll.langtest.client.analysis.WordContainer#gotClickOnItem(WordScore)
   */
  @Override
  public void showLearnAndItem(int exid) {
    ListInterface exerciseList = learnHelper.getExerciseList();
    if (exerciseList.byID(exid) != null) {
      showDefaultInitialTab(true);
      boolean b = exerciseList.loadByID(exid);
    } else {
      tabPanel.selectTab(STUDY_LISTS_INDEX);
      DivWidget content = studyLists.getContent();
      Widget widget = content.getWidget(0);
      ((TabPanel) widget).selectTab(3);

      listManager.findListAndSelect(exid);
    }
  }

  /**
   * @param toUse
   * @seex #clickOnYourLists(long)
   * @seex #selectPreviouslyClickedSubTab(com.github.gwtbootstrap.client.ui.TabPanel, TabAndContent, TabAndContent, TabAndContent, mitll.langtest.shared.custom.UserList, String, boolean, boolean, boolean, boolean)
   * @seex #showMyLists
   * @see #selectPreviousTab
   */
  private void clickOnTab(final TabAndContent toUse) {
    if (toUse == null) {
      logger.warning("clickOnTab : huh? toUse is nulll???\n\n");
    } else if (toUse.getTab() == null) {
      logger.warning("huh? toUse has a null tab? " + toUse);
    } else {
      // logger.info("click on tab " + toUse);
      toUse.clickOnTab();
    }
  }

  /**
   * @param tabPanel
   * @param iconType
   * @param label
   * @return
   * @see mitll.langtest.client.custom.Navigation#makeFirstLevelTab(TabPanel, IconType, String)
   */
  private TabAndContent makeTab(TabPanel tabPanel, IconType iconType, String label) {
    return new TabAndContent(tabPanel, iconType, label);
  }

  @Override
  public void onResize() {
    // logger.info("got onResize " + getClass().toString());

    learnHelper.onResize();
    recorderHelper.onResize();
    recordExampleHelper.onResize();
    markDefectsHelper.onResize();
    practiceHelper.onResize();
    if (listManager != null) listManager.onResize();
  }

  /**
   * @param ul
   * @return
   * @see #showInitialState()
   */
  /*private boolean createdByYou(UserList ul) {
    return ul.getCreator().getId() == getUser();
  }
*/
}
