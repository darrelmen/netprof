package mitll.langtest.client.custom;

import com.gargoylesoftware.htmlunit.javascript.host.Console;
import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.TabPanel;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.InlineLabel;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.WavCallback;
import mitll.langtest.client.bootstrap.FlexSectionExerciseList;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.recorder.RecordButton.RecordingListener;
import mitll.langtest.client.recorder.RecordButtonPanel;
import mitll.langtest.client.scoring.AudioPanel;
import mitll.langtest.client.scoring.GoodwaveExercisePanel;
import mitll.langtest.client.scoring.GoodwaveExercisePanelFactory;
import mitll.langtest.client.scoring.ScoreListener;
import mitll.langtest.client.scoring.SimplePostAudioRecordButton;
import mitll.langtest.client.sound.AudioControl;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.client.sound.PlayAudioWidget;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.client.sound.Sound;
import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.server.PathHelper;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.User;
import mitll.langtest.shared.custom.UserList;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
public class Navigation implements RequiresResize {
  private static final String CHAPTERS = "Learn";
  private static final String CONTENT = CHAPTERS;
  private static final String YOUR_LISTS = "Study Your Lists";
  private static final String OTHERS_LISTS = "Study Visited Lists";
  private static final String PRACTICE = "Practice";
  public static final String REVIEW = "review";
  public static final String COMMENT = "comment";
  public static final String ATTENTION = "attention";
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
  public static final String RECORD_AUDIO = "Record Audio";
  public static final String CONTENT1 = "content";
  public static final String CLASSROOM = "classroom";

  private final ExerciseController controller;
  private final LangTestDatabaseAsync service;
  private final UserManager userManager;
  private final SimpleChapterNPFHelper practiceHelper;

  private ScrollPanel listScrollPanel;
  private final ListInterface listInterface;
  private final NPFHelper npfHelper;
  private final NPFHelper avpHelper;

  private EditItem editItem;

  private ChapterNPFHelper defectHelper;
  private SimpleChapterNPFHelper recorderHelper;
  private SimpleChapterNPFHelper contentHelper;
  private ReviewItemHelper reviewItem;

  private final KeyStorage storage;
  
  private final SoundManagerAPI soundManager;


  /**
   * @param service
   * @param userManager
   * @param controller
   * @param predefinedContentList
   * @param feedback
   * @see mitll.langtest.client.LangTest#populateRootPanel()
   */
  public Navigation(final LangTestDatabaseAsync service, final UserManager userManager,
                    final ExerciseController controller, final ListInterface predefinedContentList,
                    UserFeedback feedback) {
    this.service = service;
    this.userManager = userManager;
    this.controller = controller;
    this.soundManager = controller.getSoundManager();
    this.listInterface = predefinedContentList;
    storage = new KeyStorage(controller);
    npfHelper = new NPFHelper(service, feedback, userManager, controller, false);
    avpHelper = new AVPHelper(service, feedback, userManager, controller);

    defectHelper = new ChapterNPFHelper(service, feedback, userManager, controller, true);
    recorderHelper = new SimpleChapterNPFHelper(service, feedback, userManager, controller, listInterface) {
      @Override
      protected FlexListLayout getMyListLayout(LangTestDatabaseAsync service, UserFeedback feedback,
                                               UserManager userManager, ExerciseController controller, SimpleChapterNPFHelper outer) {
        return new MyFlexListLayout(service, feedback, userManager, controller, outer) {
          @Override
          protected FlexSectionExerciseList makeExerciseList(Panel topRow, Panel currentExercisePanel, String instanceName) {
            return new MyFlexSectionExerciseList(topRow, currentExercisePanel, instanceName) {
              @Override
              protected void addTableWithPager(PagingContainer pagingContainer) {
                Panel column = new FlowPanel();
                add(column);
                addTypeAhead(column);
                final CheckBox w = new CheckBox("Show Only Unrecorded");
                w.addClickHandler(new ClickHandler() {
                  @Override
                  public void onClick(ClickEvent event) {
                    setUnrecorded(w.getValue());
                    scheduleWaitTimer();
                    loadExercises(getHistoryToken(""), getTypeAheadText());
                  }
                });
                w.addStyleName("leftFiveMargin");
                add(w);
                add(pagingContainer.getTableWithPager());
              }
            };

          }
        };
      }
    };

    contentHelper = new SimpleChapterNPFHelper(service, feedback, userManager, controller, listInterface) {
      protected ExercisePanelFactory getFactory(final PagingExerciseList exerciseList) {
        return new GoodwaveExercisePanelFactory(service, feedback, controller, exerciseList, 1.0f) {
          @Override
          public Panel getExercisePanel(CommonExercise e) {
            return new CommentNPFExercise(e, controller, exerciseList, 1.0f, false, CLASSROOM);
          }
        };
      }
    };

    practiceHelper = makePracticeHelper(service, userManager, controller, feedback);
    reviewItem = new ReviewItemHelper(service, feedback, userManager, controller, null, predefinedContentList, npfHelper);
    editItem = new EditItem(service, userManager, controller, predefinedContentList, feedback, npfHelper);
  }

  private SimpleChapterNPFHelper makePracticeHelper(final LangTestDatabaseAsync service, final UserManager userManager,
                                                      final ExerciseController controller, final UserFeedback feedback) {
    return new SimpleChapterNPFHelper(service, feedback, userManager, controller, listInterface) {
      MyFlashcardExercisePanelFactory myFlashcardExercisePanelFactory;

      @Override
      protected ExercisePanelFactory getFactory(PagingExerciseList exerciseList) {
        myFlashcardExercisePanelFactory = new MyFlashcardExercisePanelFactory(service, feedback, controller, exerciseList, "practice");
        return myFlashcardExercisePanelFactory;
      }

      @Override
      protected FlexListLayout getMyListLayout(LangTestDatabaseAsync service, UserFeedback feedback,
                                               UserManager userManager, ExerciseController controller, SimpleChapterNPFHelper outer) {
        return new MyFlexListLayout(service, feedback, userManager, controller, outer) {
          @Override
          protected FlexSectionExerciseList makeExerciseList(Panel topRow, Panel currentExercisePanel, String instanceName) {
            return new MyFlexSectionExerciseList(topRow, currentExercisePanel, instanceName) {
              @Override
              protected CommonShell findFirstExercise() {
                String currentExerciseID = myFlashcardExercisePanelFactory.getCurrentExerciseID();
                if (currentExerciseID != null && !currentExerciseID.trim().isEmpty()) {
                  System.out.println("findFirstExercise ---> found previous state current ex = " + currentExerciseID);

                  CommonShell shell = byID(currentExerciseID);

                  if (shell == null) {
                    System.err.println("huh? can't find " + currentExerciseID);
                    return super.findFirstExercise();
                  }
                  else {
                    myFlashcardExercisePanelFactory.populateCorrectMap();
                    return shell;
                  }
                }
                else {
                  return super.findFirstExercise();
                }
              }

              @Override
              protected void onLastItem() {
                myFlashcardExercisePanelFactory.resetStorage();
                super.onLastItem();
              }

            };
          }

          @Override
          protected void styleBottomRow(Panel bottomRow) {
            bottomRow.addStyleName("centerPractice");
          }
        };
      }
    };
  }

  /**
   * @return
   * @param secondAndThird
   * @see mitll.langtest.client.LangTest#populateRootPanel()
   */
  public Widget getNav(final Panel secondAndThird) {
    this.container = getTabPanel(secondAndThird);
    return container;
  }

  public Widget getContainer() { return container; }

  private Widget container;
  private TabPanel tabPanel;
  private TabAndContent yourStuff, othersStuff;
  private TabAndContent browse, chapters, create, dialog;
  private TabAndContent review, recorderTab, contentTab, practiceTab;
  private List<TabAndContent> tabs = new ArrayList<TabAndContent>();
  private Panel chapterContent;

  /**
   * @see #getNav(com.google.gwt.user.client.ui.Panel)
   * @param contentForChaptersTab
   * @return
   */
  private Panel getTabPanel(Panel contentForChaptersTab) {
    tabPanel = new TabPanel();
    tabPanel.getElement().getStyle().setMarginTop(-8, Style.Unit.PX);
    tabPanel.getElement().setId("tabPanel");

    this.chapterContent = contentForChaptersTab;

    // so we can know when chapters is revealed and tell it to update it's lists
    tabPanel.addShowHandler(new TabPanel.ShowEvent.Handler() {
      @Override
      public void onShow(TabPanel.ShowEvent showEvent) {
       /* System.out.println("got shown event : '" +showEvent + "'\n" +
            "\ntarget " + showEvent.getTarget()+
            " ' target name '" + showEvent.getTarget().getName() + "'");*/
        String targetName = showEvent.getTarget() == null ? "" : showEvent.getTarget().toString();
        final String chapterNameToUse = getChapterName();

        boolean wasChapters = targetName.contains(chapterNameToUse);
        Panel createdPanel = listInterface.getCreatedPanel();
        boolean hasCreated = createdPanel != null;
       // System.out.println("getTabPanel : got shown event : '" +showEvent + "' target '" + targetName + "' hasCreated " + hasCreated);
        if (hasCreated && wasChapters && (createdPanel instanceof GoodwaveExercisePanel)) {
          //System.out.println("\tgot chapters! created panel :  has created " + hasCreated + " was revealed  " + createdPanel.getClass());
          ((GoodwaveExercisePanel) createdPanel).wasRevealed();
        }
      }
    });

    return tabPanel;    // TODO - consider how to tell panels when they are hidden by tab changes
  }

  /**
   * @see #showInitialState()
   * @param contentForChaptersTab the standard npf content
   * @return
   */
  private void addTabs(Panel contentForChaptersTab) {
    tabPanel.clear();
    tabs.clear();
    nameToTab.clear();
    nameToIndex.clear();

    // your list tab
    yourStuff = makeFirstLevelTab(tabPanel, IconType.FOLDER_CLOSE, YOUR_LISTS);
    yourStuff.getTab().addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        checkAndMaybeClearTab(YOUR_LISTS);
        refreshViewLessons(true, false);
        logEvent(yourStuff, YOUR_LISTS);
      }
    });

    // visited lists
    othersStuff = makeFirstLevelTab(tabPanel, IconType.FOLDER_CLOSE, OTHERS_LISTS);
    othersStuff.getTab().addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        checkAndMaybeClearTab(OTHERS_LISTS);
        refreshViewLessons(false, true);
        logEvent(othersStuff, OTHERS_LISTS);
      }
    });

    // create tab
    create = makeFirstLevelTab(tabPanel, IconType.PLUS_SIGN, CREATE);
    final CreateListDialog createListDialog = new CreateListDialog(this, service, userManager, controller);
    create.getTab().addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        createListDialog.doCreate(create.getContent());
        logEvent(create, CREATE);
      }
    });

    // browse tab
    browse = makeFirstLevelTab(tabPanel, IconType.TH_LIST, BROWSE);
    browse.getTab().addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        checkAndMaybeClearTab(BROWSE);
        logEvent(browse, BROWSE);
        viewBrowse();
      }
    });

    dialog = makeFirstLevelTab(tabPanel, IconType.TH_LIST, PRACTICE_DIALOG);
    dialog.getTab().addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
              checkAndMaybeClearTab(PRACTICE_DIALOG);
              logEvent(dialog, PRACTICE_DIALOG);
              viewDialog();
          }
    });

    boolean isQualityControl = isQC();

    if (isQualityControl) {
      contentTab = makeFirstLevelTab(tabPanel, IconType.LIGHTBULB, CONTENT);
      contentTab.getContent().getElement().setId("content_contentPanel");
      contentTab.getTab().addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          checkAndMaybeClearTab(CONTENT);
          contentHelper.showNPF(contentTab, CONTENT1, false);
          logEvent(recorderTab, CONTENT);
        }
      });
      addPracticeTab();
    }


    // chapter tab
    final String chapterNameToUse = getChapterName();
    chapters = makeFirstLevelTab(tabPanel, isQualityControl ? IconType.FLAG : IconType.LIGHTBULB, chapterNameToUse);
    chapters.getContent().add(contentForChaptersTab);
    chapters.getTab().addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        checkAndMaybeClearTab(chapterNameToUse);
        logEvent(chapters, chapterNameToUse);
      }
    });

    if (!isQualityControl) addPracticeTab();

    if (isQualityControl) {
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
          recorderHelper.showNPF(recorderTab, "record_Audio", false);
          logEvent(recorderTab, RECORD_AUDIO);
        }
      });
    }
  }

  private void addPracticeTab() {
    practiceTab = makeFirstLevelTab(tabPanel, IconType.REPLY, "Practice");
    practiceTab.getContent().getElement().setId("practicePanel");
    practiceTab.getTab().addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        checkAndMaybeClearTab("Practice");
        practiceHelper.showNPF(practiceTab, "Practice", false);
        practiceHelper.hideList();
        logEvent(practiceTab, "Practice");
      }
    });
  }

  private String getChapterName() {
    boolean isQualityControl = isQC();

    return isQualityControl ? MARK_DEFECTS : CHAPTERS;
  }

  private boolean isQC() {  return controller.getPermissions().contains(User.Permission.QUALITY_CONTROL); }

  protected void logEvent(TabAndContent yourStuff, String context) {
    if (yourStuff != null && yourStuff.getTab() != null) {
      controller.logEvent(yourStuff.getTab().asWidget(), "Tab", "", context);
    }
  }

  private Map<String,TabAndContent> nameToTab = new HashMap<String, TabAndContent>();
  private Map<String,Integer> nameToIndex = new HashMap<String, Integer>();
  private TabAndContent makeFirstLevelTab(TabPanel tabPanel, IconType iconType, String label) {
    TabAndContent tabAndContent = makeTab(tabPanel, iconType, label);
    nameToIndex.put(label,tabs.size());
    tabs.add(tabAndContent);
    nameToTab.put(label,tabAndContent);
    return tabAndContent;
  }

  private int getSafeTabIndexFor(String tabName) {
    Integer integer = nameToIndex.get(tabName);
    if (integer == null) return 0;
    else return integer;
  }

  private void checkAndMaybeClearTab(String value) {
    //String value1 = storage.getValue(CLICKED_TAB);
    //System.out.println("checkAndMaybeClearTab " + value1 + " vs "+value + " clearing " + CLICKED_USER_LIST);
    storage.removeValue(CLICKED_USER_LIST);
    storage.storeValue(CLICKED_TAB, value);
  }

  /**
   * @see #getTabPanel(com.google.gwt.user.client.ui.Panel)
   * @see #showMyLists(boolean, boolean)
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
   * @see mitll.langtest.client.LangTest#doEverythingAfterFactory(long)
   */
  public void showInitialState() {
    final int user = userManager.getUser();

    addTabs(chapterContent);

/*    System.out.println("showInitialState show initial state for " + user +
        " : getting user lists " + controller.isReviewMode());*/
    String value = storage.getValue(CLICKED_TAB);
    if (value.isEmpty()) {   // no previous tab
      service.getListsForUser(user, true, true, new AsyncCallback<Collection<UserList>>() {
        @Override
        public void onFailure(Throwable caught) {}

        @Override
        public void onSuccess(Collection<UserList> result) {
          if (result.size() == 1 && // if only one empty list - one you've created
            result.iterator().next().isEmpty()) {
            final String chapterNameToUse = getChapterName();
            tabPanel.selectTab(getSafeTabIndexFor(chapterNameToUse));
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
      if (tabIndex != null) {
        tabPanel.selectTab(tabIndex);
        clickOnTab(tabAndContent);

        if (value.equals(YOUR_LISTS)) {
          showMyLists(true, false);
        } else if (value.equals(OTHERS_LISTS)) {
          showMyLists(false, true);
        } else if (value.equals(BROWSE)) {
          viewBrowse();
        } else if (value.equals(PRACTICE_DIALOG)) {
          viewDialog();
        } else if (value.equals(FIX_DEFECTS)) {
          viewReview(review.getContent());
        } else if (value.equals(RECORD_AUDIO)) {
          recorderHelper.showNPF(recorderTab, "record_audio", true);
        } else if (value.equals(CONTENT) && contentTab != null) {
          contentHelper.showNPF(contentTab, CONTENT1, true);
        } else if (value.equals(PRACTICE) && practiceTab != null) {
          practiceHelper.showNPF(practiceTab, PRACTICE, true);
          practiceHelper.hideList();
        }
      }
      else {
        System.out.println("selectPreviousTab : found value  '" + value + "' " +
          " but I only know about tabs : " + nameToIndex.keySet());
        final String chapterNameToUse = getChapterName();

        tabPanel.selectTab(getSafeTabIndexFor(chapterNameToUse));
      }
  }

  /**
   * @see #showInitialState()
   * @param onlyCreated
   * @param onlyVisited
   */
  private void showMyLists(boolean onlyCreated, boolean onlyVisited) {
    String value = storage.getValue(CLICKED_TAB);
//    System.out.println("showMyLists " + value + " created " + onlyCreated + " visited " + onlyVisited);
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
    int tabToSelect = onlyCreated ? getSafeTabIndexFor(YOUR_LISTS) : getSafeTabIndexFor(OTHERS_LISTS);
    tabPanel.selectTab(tabToSelect);

    TabAndContent toUse = onlyCreated ? yourStuff : othersStuff;
    clickOnTab(toUse);
    refreshViewLessons(onlyCreated, onlyVisited);
  }

  /**
   * @see mitll.langtest.client.custom.CreateListDialog#addUserList
   * @param userListID
   */
  public void clickOnYourLists(long userListID) {
    storeCurrentClickedList(userListID);
    storage.storeValue(SUB_TAB, EDIT_ITEM);

    tabPanel.selectTab(getSafeTabIndexFor(YOUR_LISTS));
    clickOnTab(yourStuff);
    refreshViewLessons(true, false);
  }

  private void clickOnTab(TabAndContent toUse) {
    if (toUse == null) {
      System.err.println("huh? toUse is nulll???\n\n");
    } else if (toUse.getTab() == null) {
      System.err.println("huh? toUse has a null tab? " + toUse);
    } else {
      toUse.getTab().fireEvent(new ButtonClickEvent());
    }
  }

  /**
   * @see Navigation#getTabPanel(com.google.gwt.user.client.ui.Panel)
   * @see Navigation#getListOperations(mitll.langtest.shared.custom.UserList, String)
   * @param tabPanel
   * @param iconType
   * @param label
   * @return
   */
  TabAndContent makeTab(TabPanel tabPanel, IconType iconType, String label) {
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

  private void viewDialog() { viewDialogOptions(dialog.getContent()); };

  private void viewDialogOptions(final Panel contentPanel) {
     contentPanel.clear();
     contentPanel.getElement().setId("contentPanel");
	 final FlowPanel forSents = new FlowPanel();
     
     final ListBox availableDialogs = new ListBox();
     final ListBox availableSpeakers = new ListBox();
     availableDialogs.getElement().getStyle().setProperty("fontSize", "150%");
     availableDialogs.getElement().getStyle().setProperty("width", "auto");
     availableDialogs.getElement().getStyle().setProperty("margin", "10px");
     availableSpeakers.getElement().getStyle().setProperty("fontSize", "150%");
     availableSpeakers.getElement().getStyle().setProperty("width", "auto");
     availableSpeakers.getElement().getStyle().setProperty("margin", "10px");
     final String CHOOSE_PART = "Choose a part to read";
     final String CHOOSE_DIALOG = "Choose a dialog to practice";
     final HashMap<String, String[]> dialogToParts = getDialogToPartsMap();
     final HashMap<Integer, String> dialogIndex = new HashMap<Integer, String>();
     
     availableDialogs.addItem(CHOOSE_DIALOG);
     availableDialogs.setVisibleItemCount(1);
     
     Integer i = 1;
     ArrayList<String> sortedDialogs = new ArrayList<String>(dialogToParts.keySet());
     java.util.Collections.sort(sortedDialogs);
     for( String dialog : sortedDialogs){
    	 dialogIndex.put(i, dialog);
    	 availableDialogs.addItem(dialog);
    	 i += 1;
     }
     
 	 availableSpeakers.addItem(CHOOSE_PART);
 	 availableSpeakers.getElement().setAttribute("disabled", "disabled");
     availableSpeakers.setVisibleItemCount(1);
     
     availableDialogs.addChangeHandler(new ChangeHandler() {
    	 public void onChange(ChangeEvent event){
    		 if(availableDialogs.getSelectedIndex() < 1){
 		    	availableSpeakers.clear();
 		    	availableSpeakers.addItem(CHOOSE_PART);
 		    	availableSpeakers.getElement().setAttribute("disabled", "disabled");
    		 }
    		 else{
    			availableSpeakers.clear();
    			availableSpeakers.addItem(CHOOSE_PART);
    			for(String part : dialogToParts.get(availableDialogs.getValue(availableDialogs.getSelectedIndex()))){
    				availableSpeakers.addItem(part);
    			}
    			availableSpeakers.getElement().removeAttribute("disabled");
    		 }
    		 forSents.clear();
    	 }
     });
     availableSpeakers.addChangeHandler(new ChangeHandler() {
    	 public void onChange(ChangeEvent event){
    		 forSents.clear();
    	 }
     });
     contentPanel.add(availableDialogs);
     contentPanel.add(availableSpeakers);
     
     Button startDialog = new Button("Start Recording!", new ClickHandler() {
    	 public void onClick(ClickEvent event) {
    		 if((availableSpeakers.getSelectedIndex() < 1) || (availableDialogs.getSelectedIndex() < 1)){
    			 Window.alert("Select a dialog and part first!");
    		 }
    		 else{
    			 //resetPlayer();
    			 forSents.clear();
    		     Grid sentPanel = displayDialog(dialogIndex.get(availableDialogs.getSelectedIndex()), availableSpeakers.getValue(availableSpeakers.getSelectedIndex()), forSents);
    		     setupPlayOrder(sentPanel, 0, sentPanel.getRowCount());
    		     //contentPanel.add(forSents);
    		 }
    	 }
     });

     startDialog.getElement().getStyle().setProperty("fontSize", "150%");
     availableDialogs.getElement().getStyle().setProperty("margin", "10px");
     contentPanel.add(startDialog);
     contentPanel.add(forSents);
  }
  
  private native void addPlayer() /*-{
     $wnd.basicMP3Player.init();
  }-*/;
  
  private native void resetPlayer() /*-{
     $wnd.soundManager.reset;
     $wnd.soundManager.init;
  }-*/;
  
  private SimplePostAudioRecordButton getRecordButton(String sent, final HTML resultHolder){
	  return new SimplePostAudioRecordButton(controller, service, sent) {
		  @Override
		  public void useResult(AudioAnswer result){
			  resultHolder.setHTML(String.valueOf(result.getScore()));
		  }
		  
		  @Override
		  public void flip(boolean first){
			  //do nothing
		  }
	  };
  }
  
  private Grid displayDialog(String dialog, String part, Panel cp){

	  HashMap<String, String> sentToAudioPath = getSentToAudioPath();
	  HashMap<String, HashMap<Integer, String>> dialogToSentIndexToSpeaker = getDialogToSentIndexToSpeaker();
	  HashMap<String, HashMap<Integer, String>> dialogToSentIndexToSent = getDialogToSentIndexToSent();
	  int sentIndex = 0;
	  final Grid sentPanel = new Grid(dialogToSentIndexToSent.get(dialog).size(), 4);
	  final ArrayList<HTML> scoreElements = new ArrayList<HTML>();
      String otherPart = "";
      boolean youStart = false;
      
	  while(dialogToSentIndexToSent.get(dialog).containsKey(sentIndex)){
		  String sentence = dialogToSentIndexToSent.get(dialog).get(sentIndex);
		  HTML sent = new HTML(sentence);
		  sent.getElement().getStyle().setProperty("color", "#B8B8B8");
		  sent.getElement().getStyle().setProperty("margin", "5px 10px");
		  sent.getElement().getStyle().setProperty("fontSize", "130%");
		  if(part.equals(dialogToSentIndexToSpeaker.get(dialog).get(sentIndex))){
			  if (sentIndex == 0)
				  youStart = true;
			  PlayAudioPanel play = new PlayAudioPanel(controller, "config/mandarinClassroom/bestAudio/"+sentToAudioPath.get(sentence));
			  HTML score = new HTML("0.0");
			  Button recordButton = getRecordButton(dialogToSentIndexToSent.get(dialog).get(sentIndex), score);
			  sentPanel.setWidget(sentIndex, 2, recordButton);
			  sent.getElement().getStyle().setProperty("fontWeight", "900");
			  sentPanel.setWidget(sentIndex, 1, play);
			  scoreElements.add(score);
			  score.setVisible(false);
			  sentPanel.setWidget(sentIndex, 3, score);
			  recordButton.setVisible(false);
			  play.setVisible(false);
		  }
		  else{
			  PlayAudioPanel play = new PlayAudioPanel(controller, "config/mandarinClassroom/bestAudio/"+sentToAudioPath.get(sentence));
			  sentPanel.setWidget(sentIndex, 1, play);
			  sent.getElement().getStyle().setProperty("fontStyle", "italic");
			  play.setVisible(false);
			  otherPart = dialogToSentIndexToSpeaker.get(dialog).get(sentIndex);
		  }
		  sentPanel.setWidget(sentIndex, 0, sent);
		  sentIndex += 1;
	  }
	  
	  cp.add(getSetupText(part, otherPart, youStart));
	  cp.add(sentPanel);
	  //hack alert, fix later
	  if((Button) sentPanel.getWidget(sentPanel.getRowCount()-1, 2) != null)
	     cp.add(setupScoring((Button) sentPanel.getWidget(sentPanel.getRowCount()-1, 2), (PlayAudioPanel) sentPanel.getWidget(sentPanel.getRowCount()-1, 1), scoreElements));
	  else
	     cp.add(setupScoring((Button) sentPanel.getWidget(sentPanel.getRowCount()-2, 2), (PlayAudioPanel) sentPanel.getWidget(sentPanel.getRowCount()-1, 1), scoreElements));
	  Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
		  public void execute() {
			  addPlayer();
		  }
	  });
	  return sentPanel;
  }
  
  private void setupPlayOrder(final Grid sentPanel, final int currIndex, final int stop){
	  if(currIndex >= stop)
		  return;
	  sentPanel.getWidget(currIndex, 0).getElement().getStyle().setProperty("color", "#000000");
	  if(sentPanel.getWidget(currIndex, 2) != null){
		  sentPanel.getWidget(currIndex, 1).setVisible(true);
		  sentPanel.getWidget(currIndex,  2).setVisible(true);
	  	  ((Button) sentPanel.getWidget(currIndex,  2)).addMouseUpHandler(new MouseUpHandler() {
			  @Override
			  public void onMouseUp(MouseUpEvent e){
			      sentPanel.getWidget(currIndex, 0).getElement().getStyle().setProperty("color", "#B8B8B8");
				  sentPanel.getWidget(currIndex, 2).setVisible(false);
				  sentPanel.getWidget(currIndex, 1).setVisible(false);
				  if(currIndex+1 != stop){
				     sentPanel.getWidget(currIndex+1, 0).getElement().getStyle().setProperty("color", "#000000");
				  }
				  setupPlayOrder(sentPanel, currIndex + 1, stop);
			  }
	  	  });
	  }
	  else{
		  ((PlayAudioPanel) sentPanel.getWidget(currIndex, 1)).playCurrent();
		  ((PlayAudioPanel) sentPanel.getWidget(currIndex, 1)).addListener(new AudioControl(){

				@Override
				public void reinitialize() {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void songFirstLoaded(double durationEstimate) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void songLoaded(double duration) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void songFinished() {
					setupPlayOrder(sentPanel, currIndex + 1, stop);
					sentPanel.getWidget(currIndex, 0).getElement().getStyle().setProperty("color", "#B8B8B8");
				}

				@Override
				public void update(double position) {
					// TODO Auto-generated method stub
					
				}  
          });
	  }
  }
  
  private HTML getSetupText(String part, String otherPart, boolean youStart){
	  HTML setup = new HTML("You are "+part+" talking to "+otherPart+". "+(youStart ? "You" : otherPart) +" begin"+(youStart ? "" : "s")+" the conversation.");
	  setup.getElement().getStyle().setProperty("fontSize", "130%");
	  setup.getElement().getStyle().setProperty("margin", "10px");
	  return setup;
  }
  
  private HTML setupScoring(Button lastB, PlayAudioPanel lastP, final ArrayList<HTML> scoreElements){
	  final HTML avg = new HTML("");
	  if(lastB != null){
		  lastB.addMouseUpHandler(new MouseUpHandler() {
			  @Override
			  public void onMouseUp(MouseUpEvent e){
				  avg.setHTML(innerScoring(scoreElements));
			  }
		  });
	  }
	  else{/*
		  lastP.addListener(new AudioControl(){

			@Override
			public void reinitialize() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void songFirstLoaded(double durationEstimate) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void songLoaded(double duration) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void songFinished() {
				avg.setHTML(innerScoring(scoreElements));
			}

			@Override
			public void update(double position) {
				// TODO Auto-generated method stub
				
			}  
          });*/
	  }
	  avg.setVisible(true);
	  avg.getElement().getStyle().setProperty("fontSize", "130%");
	  avg.getElement().getStyle().setProperty("margin", "10px");
	  return avg;
  }
  
  private String innerScoring(ArrayList<HTML> scoreElements){
	  double sum = 0.0;
	  for(HTML sco : scoreElements){
		sco.setVisible(true);
		sum += Double.parseDouble(sco.getHTML());
	  }
	  System.out.println(sum);
	  System.out.println(scoreElements.size());
	  return "Your average score was: "+String.valueOf(sum/scoreElements.size());
  }
  
  private HashMap<String, String[]> getDialogToPartsMap(){
	  HashMap<String, String[]> m = new HashMap<String, String[]>();
	  m.put("Unit 1: Part 1", new String[] {"Crane", "Wang"});
	  m.put("Unit 1: Part 2", new String[] {"Smith", "Zhao"});
	  return m;
  }
  

  private HashMap<String, String> getSentToAudioPath() {
	  HashMap<String, String> m = new HashMap<String, String>();
	  m.put("Kē Léi'ēn, nǐ hăo!", "/4/slow_1403800571291_by_8.wav");
	  m.put("Nǐ dào năr qù a?", "/13/slow_1403801128819_by_8.wav");
	  m.put("Wŏ huí sùshè.", "/24/slow_1403800649832_by_8.wav");
	  m.put("Wáng Jīngshēng, nǐ hăo!", "/7/slow_1403800597192_by_8.wav");
	  m.put("Wŏ qù túshūguăn. Nĭ ne?", "/20/slow_1403800730216_by_8.wav");
	  
	  m.put("Zhào Guócái, nĭ hăo a!", "/40/slow_1403793805369_by_8.wav");
	  m.put("Hái xíng. Nĭ àirén, háizi dōu hăo ma?", "/57/slow_1403793264402_by_8.wav"); //hi
	  m.put("Wŏ yŏu yìdiănr shìr, xiān zŏule. Zàijiàn!", "/71/slow_1403792972693_by_8.wav");
	  m.put("Nĭ hăo! Hăo jiŭ bú jiànle.", "/44/slow_1403792786355_by_8.wav");
	  m.put("Zěmmeyàng a?", "/45/slow_1403792847063_by_8.wav");
	  m.put("Tāmen dōu hěn hăo, xièxie.", "/63/slow_1403793604382_by_8.wav");
	  m.put("Zàijiàn.", "/72/slow_1403792425728_by_8.wav");
	  
	  return m;
  }
  
  private HashMap<String, HashMap<Integer, String>> getDialogToSentIndexToSpeaker() {
	  HashMap<String, HashMap<Integer, String>> m = new HashMap<String, HashMap<Integer, String>>();
	  String up1 = "Unit 1: Part 1";
	  String up2 = "Unit 1: Part 2";
	  m.put(up1, new HashMap<Integer, String>());
	  m.put(up2, new HashMap<Integer, String>());
	  
	  m.get(up1).put(0, "Wang");
	  m.get(up1).put(1, "Crane");
	  m.get(up1).put(2, "Wang");
	  m.get(up1).put(3, "Crane");
	  m.get(up1).put(4, "Wang");
	  
	  m.get(up2).put(0, "Smith");
	  m.get(up2).put(1, "Zhao");
	  m.get(up2).put(2, "Zhao");
	  m.get(up2).put(3, "Smith");
	  m.get(up2).put(4, "Zhao");
	  m.get(up2).put(5, "Smith");
	  m.get(up2).put(6, "Zhao");
	  
	  return m;
  }
  
  private HashMap<String, HashMap<Integer, String>> getDialogToSentIndexToSent() {
	  HashMap<String, HashMap<Integer, String>> m = new HashMap<String, HashMap<Integer, String>>();
	  String up1 = "Unit 1: Part 1";
	  String up2 = "Unit 1: Part 2";
	  m.put(up1, new HashMap<Integer, String>());
	  m.put(up2, new HashMap<Integer, String>());
	  
	  m.get(up1).put(0, "Kē Léi'ēn, nǐ hăo!");
	  m.get(up1).put(1, "Wáng Jīngshēng, nǐ hăo!");
	  m.get(up1).put(2, "Nǐ dào năr qù a?");
	  m.get(up1).put(3, "Wŏ qù túshūguăn. Nĭ ne?");
	  m.get(up1).put(4, "Wŏ huí sùshè.");
	  
	  m.get(up2).put(0, "Zhào Guócái, nĭ hăo a!");
	  m.get(up2).put(1, "Nĭ hăo! Hăo jiŭ bú jiànle.");
	  m.get(up2).put(2, "Zěmmeyàng a?");
	  m.get(up2).put(3, "Hái xíng. Nĭ àirén, háizi dōu hăo ma?");
	  m.get(up2).put(4, "Tāmen dōu hěn hăo, xièxie.");
	  m.get(up2).put(5, "Wŏ yŏu yìdiănr shìr, xiān zŏule. Zàijiàn!");
	  m.get(up2).put(6, "Zàijiàn.");
	  
	  return m;
  }
  
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
        new UserListCallback(this, contentPanel, child, listScrollPanel, LESSONS + "_All", false, true, userManager, onlyMine));
    } else {
     // System.out.println("viewLessons for " + userManager.getUser());
      service.getListsForUser(userManager.getUser(), onlyMine,
        onlyVisited,
        new UserListCallback(this, contentPanel, child, listScrollPanel, LESSONS + (onlyMine ? "_Mine":"_Others"), onlyMine, false, userManager, onlyMine));
    }
  }

  /**
   * @see #getTabPanel(com.google.gwt.user.client.ui.Panel)
   * @param contentPanel
   */
  private void viewReview(final Panel contentPanel) {
    final Panel child = getContentChild(contentPanel, "defectReview_contentPanel");

    System.out.println("------> viewReview : reviewLessons for " + userManager.getUser());

    service.getReviewLists(new AsyncCallback<List<UserList>>() {
      @Override
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(List<UserList> reviewLists) {
        System.out.println("\tviewReview : reviewLessons for " + userManager.getUser() + " got " + reviewLists);

        new UserListCallback(Navigation.this, contentPanel, child, new ScrollPanel(), REVIEW, false, false, userManager, false).onSuccess(reviewLists);
      }
    });
  }

  private Panel getContentChild(Panel contentPanel, String id) {
    contentPanel.clear();
    contentPanel.getElement().setId(id);

    final Panel child = new DivWidget();
    contentPanel.add(child);
    child.addStyleName("exerciseBackground");
    return child;
  }

  @Override
  public void onResize() {
    setScrollPanelWidth(listScrollPanel);
    npfHelper.onResize();
    avpHelper.onResize();
    defectHelper.onResize();
    reviewItem.onResize();
    recorderHelper.onResize();
    editItem.onResize();
    contentHelper.onResize();
    practiceHelper.onResize();
  }

  /**
   * @see UserListCallback#getDisplayRowPerList(mitll.langtest.shared.custom.UserList, boolean, boolean)
   * @see UserListCallback#selectPreviousList
   * @param ul
   * @param contentPanel
   */
  void showList(final UserList ul, Panel contentPanel, final String instanceName) {
    //System.out.println("showList " + ul + " instance " + instanceName);
  //  if (!ul.isEmpty()) System.out.println("\tfirst" + ul.getExercises().iterator().next());
    controller.logEvent(contentPanel,"Tab","UserList_"+ul.getID(),"Show List");

    String previousList = storage.getValue(CLICKED_USER_LIST);
    String currentValue = storeCurrentClickedList(ul);

    // if select a new list, clear the subtab selection
    if (previousList != null && !previousList.equals(currentValue)) {
      //System.out.println("\tshowList " +previousList + " vs " + currentValue);

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
    DOM.setStyleAttribute(container.getElement(), "paddingLeft", "2px");
    DOM.setStyleAttribute(container.getElement(), "paddingRight", "2px");

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

  private Anchor getDownloadLink(long listid, String linkid, final String name) {
    final Anchor downloadLink = new Anchor(getURLForDownload(listid));
    new TooltipHelper().addTooltip(downloadLink, "Download spreadsheet and audio for list.");
    downloadLink.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        controller.logEvent(downloadLink, "DownloadLink", "N/A", "downloading audio for " +name);
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
        public void onFailure(Throwable caught) {}

        @Override
        public void onSuccess(Void result) {}
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
    System.out.println("getListOperations : '" + instanceName + " for list " +ul);
    final boolean isReview = instanceName.equals(REVIEW);
    final boolean isComment = instanceName.equals(COMMENT);
    final boolean isAttention = instanceName.equals(ATTENTION);
    final String instanceName1 = isReview ? REVIEW : isComment ? COMMENT : isAttention ? ATTENTION : LEARN;

    // add learn tab
    String learnTitle = isReview ? POSSIBLE_DEFECTS : isComment ? ITEMS_WITH_COMMENTS : isAttention ? "Items for LL" : LEARN_PRONUNCIATION;
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
          // System.out.println("getListOperations : got click on practice");
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
   * @paramx editItem
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

  private void showLearnTab(TabAndContent learnTab, UserList ul, String instanceName1, boolean isReview, boolean isComment, boolean isAttention) {
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
      //System.out.println("showLearnTab : onClick using defect helper " + instanceName1 + " and " +ul);
      defectHelper.showNPF(ul, learn, instanceName1, false);
    }
    else {
      //System.out.println("showLearnTab : onClick using npf helper " + instanceName1 + " and " +ul);
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
  private void showEditReviewOrComment(UserList ul, boolean isNormalList, TabAndContent finalEditItem, boolean isReview, boolean isComment) {
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
                                                boolean isReview, boolean isComment, boolean isAttention, boolean isNormalList) {
    String subTab = storage.getValue(SUB_TAB);
    //System.out.println("selectPreviouslyClickedSubTab : subtab " + subTab);

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
    //System.out.println("showEditReviewOrComment --- " + ul + " : " + includeAddItem  + " reviewer " + isUserReviewer);
    addItem.getContent().clear();
    addItem.getContent().add(editItem.editItem(ul,
      //listToMarker.get(ul),
      new InlineLabel(),   // TODO get rid of this entirely
      includeAddItem));
  }

  /**
   * @see #onResize()
   * @param row
   */
  private void setScrollPanelWidth(ScrollPanel row) {
    if (row != null) {
      //row.setWidth((Window.getClientWidth() * 0.95) + "px");
      row.setHeight((Window.getClientHeight() * 0.7) + "px");
    }
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
          System.err.println("---> did not do deleteList " + ul.getUniqueID());
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


  private boolean createdByYou(UserList ul) {
    return ul.getCreator().getId() == userManager.getUser();
  }
}
