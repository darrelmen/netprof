package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.RadioButton;
import com.github.gwtbootstrap.client.ui.TabPanel;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.InlineLabel;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.bootstrap.FlexSectionExerciseList;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.gauge.SimpleColumnChart;
import mitll.langtest.client.exercise.WaveformExercisePanel;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.scoring.GoodwaveExercisePanel;
import mitll.langtest.client.scoring.GoodwaveExercisePanelFactory;
import mitll.langtest.client.scoring.SimplePostAudioRecordButton;
import mitll.langtest.client.sound.AudioControl;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.User;
import mitll.langtest.shared.custom.UserList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
  public static final String CLASSROOM = "classroom";
  public static final String SHOW_ONLY_UNRECORDED = "Show Only Unrecorded";

  private final ExerciseController controller;
  private final LangTestDatabaseAsync service;
  private final UserManager userManager;
  private final SimpleChapterNPFHelper practiceHelper;

  private ScrollPanel listScrollPanel;
  private final ListInterface listInterface;
  private final NPFHelper npfHelper;
  private final NPFHelper avpHelper;

  private final EditItem editItem;

  private final ChapterNPFHelper defectHelper;
  private final SimpleChapterNPFHelper recorderHelper, recordExampleHelper;
  private final SimpleChapterNPFHelper contentHelper;
  private final ReviewItemHelper reviewItem;

  private final KeyStorage storage;
  
  private final SoundManagerAPI soundManager;
  private final NumberFormat decF = NumberFormat.getFormat("#.####");

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
    recorderHelper = new RecorderNPFHelper(service, feedback, userManager, controller, true);
    recordExampleHelper = new RecorderNPFHelper(service, feedback, userManager, controller, false);

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
  public Widget getNav(final Panel secondAndThird) {  return getTabPanel(secondAndThird);  }

  private TabPanel tabPanel;
  private TabAndContent yourStuff, othersStuff;
  private TabAndContent browse, chapters, create, dialog;
  private TabAndContent review, recorderTab, recordExampleTab, contentTab, practiceTab;
  private final List<TabAndContent> tabs = new ArrayList<TabAndContent>();
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
          contentHelper.showNPF(contentTab, CONTENT1);
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

  private void addPracticeTab() {
    practiceTab = makeFirstLevelTab(tabPanel, IconType.REPLY, "Practice");
    practiceTab.getContent().getElement().setId("practicePanel");
    practiceTab.getTab().addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        checkAndMaybeClearTab("Practice");
        practiceHelper.showNPF(practiceTab, "Practice");
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

  void logEvent(TabAndContent yourStuff, String context) {
    if (yourStuff != null && yourStuff.getTab() != null) {
      controller.logEvent(yourStuff.getTab().asWidget(), "Tab", "", context);
    }
  }

  private final Map<String,TabAndContent> nameToTab = new HashMap<String, TabAndContent>();
  private final Map<String,Integer> nameToIndex = new HashMap<String, Integer>();
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
          recorderHelper.showNPF(recorderTab, "record_audio");
        } else if (value.equals(RECORD_EXAMPLE)) {
          recordExampleHelper.showNPF(recordExampleTab, "record_example_audio");
        } else if (value.equals(CONTENT) && contentTab != null) {
          contentHelper.showNPF(contentTab, CONTENT1);
        } else if (value.equals(PRACTICE) && practiceTab != null) {
          practiceHelper.showNPF(practiceTab, PRACTICE);
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
     final HorizontalPanel optionPanel = new HorizontalPanel();
	 final FlowPanel forSents = new FlowPanel();	 
	 final FlowPanel forGoodPhoneScores = new FlowPanel();
	 forGoodPhoneScores.getElement().getStyle().setProperty("borderLeftStyle", "dashed");
	 forGoodPhoneScores.getElement().getStyle().setProperty("borderLeftWidth", "2px");
	 forGoodPhoneScores.getElement().getStyle().setProperty("paddingLeft", "18px");
	 final FlowPanel forBadPhoneScores = new FlowPanel();
	 forSents.getElement().getStyle().setProperty("marginBottom", "10px");
     
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
     final RadioButton yesDia = new RadioButton("showDia", "Show your part");
     final RadioButton noDia = new RadioButton("showDia", "Hide your part");
     final RadioButton regular = new RadioButton("audioSpeed", "Regular speed");
     final RadioButton slow = new RadioButton("audioSpeed", "Slow speed");
     
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
    		 mkNewDialog(availableSpeakers, availableDialogs, forSents, forGoodPhoneScores, forBadPhoneScores, dialogIndex, yesDia, regular, false);
    	 }
     });
     availableSpeakers.addChangeHandler(new ChangeHandler() {
    	 public void onChange(ChangeEvent event){
    		 mkNewDialog(availableSpeakers, availableDialogs, forSents, forGoodPhoneScores, forBadPhoneScores, dialogIndex, yesDia, regular, false);
    	 }
     });
     optionPanel.add(availableDialogs);
     optionPanel.add(availableSpeakers);
     
     VerticalPanel showDiaPanel = new VerticalPanel();
     showDiaPanel.getElement().getStyle().setProperty("margin", "10px");
     yesDia.setValue(true);
     showDiaPanel.add(yesDia);
     showDiaPanel.add(noDia);
     yesDia.addClickHandler(new ClickHandler(){
    	 public void onClick(ClickEvent event){
    		 mkNewDialog(availableSpeakers, availableDialogs, forSents, forGoodPhoneScores, forBadPhoneScores, dialogIndex, yesDia, regular, false);
    	 }
     });
     noDia.addClickHandler(new ClickHandler(){
    	 public void onClick(ClickEvent event){
    		 mkNewDialog(availableSpeakers, availableDialogs, forSents, forGoodPhoneScores, forBadPhoneScores, dialogIndex, yesDia, regular, false);
    	 }
     });
     optionPanel.add(showDiaPanel);
     
     VerticalPanel audioSpeedPanel = new VerticalPanel();
     audioSpeedPanel.getElement().getStyle().setProperty("margin", "10px");
     regular.setValue(true);
     audioSpeedPanel.add(regular);
     audioSpeedPanel.add(slow);
     regular.addClickHandler(new ClickHandler(){
    	 public void onClick(ClickEvent event){
    		 mkNewDialog(availableSpeakers, availableDialogs, forSents, forGoodPhoneScores, forBadPhoneScores, dialogIndex, yesDia, regular, false);
    	 }
     });
     slow.addClickHandler(new ClickHandler(){
    	 public void onClick(ClickEvent event){
    		 mkNewDialog(availableSpeakers, availableDialogs, forSents, forGoodPhoneScores, forBadPhoneScores, dialogIndex, yesDia, regular, false);
    	 }
     });
     optionPanel.add(audioSpeedPanel);
     
     Button startDialog = new Button("Start Recording!", new ClickHandler() {
    	 public void onClick(ClickEvent event) {
    		 if((availableSpeakers.getSelectedIndex() < 1) || (availableDialogs.getSelectedIndex() < 1)){
    		    Window.alert("Select a dialog and part first!");
    		 }
    		 else
    		    mkNewDialog(availableSpeakers, availableDialogs, forSents, forGoodPhoneScores, forBadPhoneScores, dialogIndex, yesDia, regular, true);
    	 }
     });
     
     //make startDialog green or something? also change on radio select

     startDialog.getElement().getStyle().setProperty("fontSize", "150%");
     availableDialogs.getElement().getStyle().setProperty("margin", "10px");
     forGoodPhoneScores.getElement().getStyle().setProperty("margin", "10px");
     forBadPhoneScores.getElement().getStyle().setProperty("margin", "10px");
     HorizontalPanel sentsAndPhones = new HorizontalPanel();
     VerticalPanel diaAndStart = new VerticalPanel();
     diaAndStart.add(forSents);
     diaAndStart.add(startDialog);
     contentPanel.add(optionPanel);
     sentsAndPhones.add(diaAndStart);
     sentsAndPhones.add(forGoodPhoneScores);
     sentsAndPhones.add(forBadPhoneScores);
     contentPanel.add(sentsAndPhones);
     //contentPanel.add(startDialog);
  }
  
  public void mkNewDialog(ListBox availableSpeakers, ListBox availableDialogs, FlowPanel forSents, FlowPanel forGoodPhoneScores, FlowPanel forBadPhoneScores, HashMap<Integer, String> dialogIndex, RadioButton yesDia, RadioButton regular, boolean fromButton){
	 if((availableSpeakers.getSelectedIndex() < 1) || (availableDialogs.getSelectedIndex() < 1)){
		 forSents.clear();
		 forGoodPhoneScores.clear();
		 forBadPhoneScores.clear();
		 return;
	 }
     else{
		 forSents.clear();
		 forGoodPhoneScores.clear();
		 forBadPhoneScores.clear();
		 Grid sentPanel = displayDialog(dialogIndex.get(availableDialogs.getSelectedIndex()), availableSpeakers.getValue(availableSpeakers.getSelectedIndex()), forSents, forGoodPhoneScores, forBadPhoneScores, yesDia.getValue(), regular.getValue());
		 if(fromButton)
		    setupPlayOrder(sentPanel, 0, sentPanel.getRowCount());
     }
  }
  
  private native void addPlayer() /*-{
     $wnd.basicMP3Player.init();
  }-*/;
  
  private native void resetPlayer() /*-{
     $wnd.soundManager.reset;
     $wnd.soundManager.init;
  }-*/;
  
  private SimplePostAudioRecordButton getRecordButton(String sent, final HTML resultHolder, final Button continueButton, final Image check, final Image x, final Image somethingIsHappening){
	  SimplePostAudioRecordButton s = new SimplePostAudioRecordButton(controller, service, sent) {
		  		  
		  @Override
		  public void useResult(AudioAnswer result){
			  resultHolder.setHTML(decF.format(result.getScore()));
			  continueButton.setEnabled(true);
			  check.setVisible(true);
			  x.setVisible(false);
			  somethingIsHappening.setVisible(false);
			  this.lastResult = result;
		  }
		  
		  public void useInvalidResult(AudioAnswer result){
			  continueButton.setEnabled(false);
			  check.setVisible(false);
			  x.setVisible(true);
			  somethingIsHappening.setVisible(false);
			  this.lastResult = result;
		  }
		  
		  @Override
		  public void flip(boolean first){
			  //check.setVisible(first);
			  //x.setVisible(!first);
		  }
		  
	  };
	  s.addMouseDownHandler(new MouseDownHandler() {
		  @Override
		  public void onMouseDown(MouseDownEvent e){
			  check.setVisible(false);
			  x.setVisible(false);
			  somethingIsHappening.setVisible(false);
		  }
	  });
	  s.addMouseUpHandler(new MouseUpHandler() {
		  @Override
		  public void onMouseUp(MouseUpEvent e){
			  if(! somethingIsHappening.isVisible())
			     somethingIsHappening.setVisible(true);
		  }
	  });
	  s.setWidth("120px");
	  return s;
  }
  
  
  private SimplePostAudioRecordButton getFinalRecordButton(String sent, final Button continueButton, final Image check, final Image x, final Image somethingIsHappening, final ArrayList<HTML> scoreElements, final HTML score, final HTML avg){
	  SimplePostAudioRecordButton s = new SimplePostAudioRecordButton(controller, service, sent) {
		  
	      @Override
		  public void useResult(AudioAnswer result){
	    	  avg.setVisible(true);
			  score.setHTML(decF.format(result.getScore()));
			  avg.setHTML(innerScoring(scoreElements));
			  avg.getElement().getStyle().setProperty("fontSize", "130%");
			  avg.getElement().getStyle().setProperty("margin", "10px");
			  continueButton.setEnabled(true);
			  check.setVisible(true);
			  x.setVisible(false);
			  somethingIsHappening.setVisible(false);
			  this.lastResult = result;
		  }
			  
		  @Override
		  public void flip(boolean first){
			  //avg.setVisible(!first);
	      }
		  
		  @Override
		  protected void useInvalidResult(AudioAnswer result){
		     continueButton.setEnabled(false); 
		     check.setVisible(false);
		     x.setVisible(true);
		     this.lastResult = result;
		     somethingIsHappening.setVisible(false);
		  }
		  
      };
	  s.addMouseUpHandler(new MouseUpHandler() {
		  @Override
		  public void onMouseUp(MouseUpEvent e){
			  if(! somethingIsHappening.isVisible())
			     somethingIsHappening.setVisible(true);
		  }
	  });
	  s.setWidth("120px");
	  return s;
  }
  
  private Grid displayDialog(final String dialog, String part, FlowPanel cp, final FlowPanel goodPhonePanel, final FlowPanel badPhonePanel, boolean showPart, boolean regAudio){
	  
	  HashMap<String, String> sentToAudioPath = regAudio ? getSentToAudioPath() : getSentToSlowAudioPath();
	  HashMap<String, HashMap<Integer, String>> dialogToSentIndexToSpeaker = getDialogToSentIndexToSpeaker();
	  final HashMap<String, HashMap<Integer, String>> dialogToSentIndexToSent = getDialogToSentIndexToSent();
	  HashMap<String, HashMap<String, Integer>> dialogToSpeakerToLast = getDialogToSpeakerToLast();

	  int sentIndex = 0;
	  final Grid sentPanel = new Grid(dialogToSentIndexToSent.get(dialog).size(), 9);
	  final ArrayList<HTML> scoreElements = new ArrayList<HTML>();
      String otherPart = "";
      boolean youStart = false;
      int yourLast = dialogToSpeakerToLast.get(dialog).get(part);
	  final FlowPanel rp = new FlowPanel();
	  final HTML avg = new HTML("");
	  avg.setVisible(false);
	  rp.add(avg);
	  rp.setVisible(false);
	  final ArrayList<SimplePostAudioRecordButton> recoButtons = new ArrayList<SimplePostAudioRecordButton>();
	  final ArrayList<Integer> sentIndexes = new ArrayList<Integer>();
	  final ArrayList<Image> prevResponses = new ArrayList<Image>();
	  boolean yourFirstReco = true;
	  
	  while(dialogToSentIndexToSent.get(dialog).containsKey(sentIndex)){
		  String sentence = dialogToSentIndexToSent.get(dialog).get(sentIndex);
		  HTML sent = new HTML(sentence);
		  sent.getElement().getStyle().setProperty("color", "#B8B8B8");
		  sent.getElement().getStyle().setProperty("margin", "5px 10px");
		  sent.getElement().getStyle().setProperty("fontSize", "130%");
		  if(part.equals(dialogToSentIndexToSpeaker.get(dialog).get(sentIndex))){
			  boolean nextIsSame = dialogToSentIndexToSpeaker.get(dialog).containsKey(sentIndex+1) && (dialogToSentIndexToSpeaker.get(dialog).get(sentIndex).equals(dialogToSentIndexToSpeaker.get(dialog).get(sentIndex+1)));
			  sentIndexes.add(sentIndex);
			  if (sentIndex == 0)
				  youStart = true;
			  if(!showPart)
				  sent.setText("(Say your part)"); // be careful to not get the sentence for scoring from here!
			  PlayAudioPanel play = new PlayAudioPanel(controller, "config/mandarinClassroom/bestAudio/"+sentToAudioPath.get(sentence));
			  play.setMinWidth(82);
			  play.setPlayLabel("Play");
			  final HTML score = new HTML("0.0");
			  scoreElements.add(score);
			  SimplePostAudioRecordButton recordButton = null;
			  final Button continueButton = new Button("Continue");
			  continueButton.setEnabled(false);
			  final Image check = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "checkmark32.png"));
			  final Image x = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "redx32.png"));
			  final Image somethingIsHappening = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "animated_progress28.gif"));
			  if(nextIsSame){
				  prevResponses.add(check);
				  prevResponses.add(x);
				  prevResponses.add(somethingIsHappening);
			  }
			  if(sentIndex != yourLast){
			     recordButton = getRecordButton(dialogToSentIndexToSent.get(dialog).get(sentIndex), score, continueButton, check, x, somethingIsHappening);
			     continueButton.addClickHandler(new ClickHandler() {
			    	 @Override
			    	 public void onClick(ClickEvent e){
						 for(Image i : prevResponses)
					        i.setVisible(false);
			    		 prevResponses.clear();
			    	 }
			     });
			  }
			  else{
				 recordButton = getFinalRecordButton(dialogToSentIndexToSent.get(dialog).get(sentIndex), continueButton, check, x, somethingIsHappening, scoreElements, score, avg);
				  
				  continueButton.addClickHandler(new ClickHandler() {
					  @Override
					  public void onClick(ClickEvent e){ 
						  if(continueButton.isEnabled()){
							  displayResults(dialog, scoreElements, sentIndexes, dialogToSentIndexToSent, recoButtons, sentPanel, rp, goodPhonePanel, badPhonePanel);
							  for(Image i : prevResponses)
								  i.setVisible(false);
							  prevResponses.clear();
						  }
					  }
				  });
			  }
			  
			  recordButton.addMouseUpHandler(new MouseUpHandler() {
				  @Override
				  public void onMouseUp(MouseUpEvent e){
					  //continueButton.setEnabled(true);
				  }
			  });
			  recordButton.addMouseDownHandler(new MouseDownHandler(){
				  @Override
				  public void onMouseDown(MouseDownEvent e){
					  check.setVisible(false);
					  x.setVisible(false);
				  }
			  });
			  if(yourFirstReco){
				  controller.register(recordButton, "recording started with sent " + sentence + " in dialog "+ dialog + " as speaker " + part + " with audio speed " + (regAudio ? "regular" : "slow") + " and part " + (showPart ? "visible" : "hidden"));
				  yourFirstReco = false;
			  }
			  else{
				  controller.register(recordButton, "record button for sent: " + sentence + " in dialog " + dialog);
			  }
			  if(sentIndex == yourLast){
				  controller.register(continueButton, "recording stopped with sent " + sentence + " in dialog "+dialog);
			  }
			  else{
			      controller.register(continueButton, "continue button for sent: " + sentence + " in dialog " + dialog);
			  }
			  recoButtons.add((SimplePostAudioRecordButton) recordButton);

			  sentPanel.setWidget(sentIndex, 2, recordButton);
			  sent.getElement().getStyle().setProperty("fontWeight", "900");
			  sentPanel.setWidget(sentIndex, 1, play);
			  score.setVisible(false);
			  sentPanel.setWidget(sentIndex, 3, continueButton);
			  continueButton.setVisible(false);
			  sentPanel.setWidget(sentIndex, 4, score);
			  recordButton.setVisible(false);
			  if(nextIsSame)
			     continueButton.getElement().addClassName("nextIsSame");
			  play.setVisible(false);
			  sentPanel.setWidget(sentIndex, 5, check);
			  check.setVisible(false);
			  sentPanel.setWidget(sentIndex, 6, x);
			  x.setVisible(false);
			  sentPanel.setWidget(sentIndex, 7, somethingIsHappening);
			  somethingIsHappening.setVisible(false);
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
	  
	  cp.add(rp);
	  Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
		  public void execute() {
			  addPlayer();
		  }
	  });
	  return sentPanel;
  }
  
  private void displayResults(String dialog, ArrayList<HTML> scoreElements, ArrayList<Integer> sentIndexes, HashMap<String, HashMap<Integer, String>> dialogToSentIndexToSent, ArrayList<SimplePostAudioRecordButton> recoButtons, Grid sentPanel, FlowPanel rp, FlowPanel goodPhonePanel, FlowPanel badPhonePanel){
	     final HashMap<String, ArrayList<Float>> phonesToScores = new HashMap<String, ArrayList<Float>>();
	     HashMap<String, PlayAudioPanel> phoneToAudioExample = getPlayAudioWidget();
	     rp.setVisible(true);
	     for(HTML elt : scoreElements){
	    	 elt.setVisible(true);
	     }
	     for(int i = 0; i < sentIndexes.size(); i++){
	    	 sentPanel.setWidget(sentIndexes.get(i),  0, recoButtons.get(i).getSentColors(((dialogToSentIndexToSent.get(dialog).get(sentIndexes.get(i))))));
	    	 sentPanel.setWidget(sentIndexes.get(i), 8, recoButtons.get(i).getScoreBar(Float.parseFloat(scoreElements.get(i).getText())));
	     }
	     for(SimplePostAudioRecordButton recordButton: recoButtons){
	    	 Map<String, Float> pts = recordButton.getPhoneScores();
	    	 for(String phone : pts.keySet()){
	    		 if(! phonesToScores.containsKey(phone)){
	    			 phonesToScores.put(phone, new ArrayList<Float>());
	    		 }
	    		 phonesToScores.get(phone).add(pts.get(phone));
	    	 }
	     }
	     int numToShow = 5;
	     ArrayList<String> preFilteredPhones = new ArrayList<String>(phonesToScores.keySet());
	     ArrayList<String> phones = new ArrayList<String>();
	     for(String phone : preFilteredPhones){
	    	 if(phonesToScores.get(phone).size() > 1)
	    		 phones.add(phone);
	     }
	     if(phones.size() < 10){ //strategy 2, if there aren't enough phone repeats
	    	 phones.clear();
	    	 for(String phone : preFilteredPhones){
	    		 if(avg(phonesToScores.get(phone)) > 0.1)
	    			 phones.add(phone);
	    	 }
	    	 System.out.println("Falling back to cropping worst phones");
	     }
	     else
	    	 System.out.println("Filtering out phones with only one pronunication");
	     numToShow = phones.size() < 10 ? phones.size()/2 : 5; // the magic number 5!!!
	     Collections.sort(phones, new Comparator<String>(){
	    	 @Override
	    	 public int compare(String s1, String s2){
	    		 return (int) (1000*(avg(phonesToScores.get(s2)) - avg(phonesToScores.get(s1))));
	    	 }
	     });
	     Grid goodPhoneScores = new Grid(numToShow, 2); 
	     SimpleColumnChart chart = new SimpleColumnChart();
	     for(int pi = 0; pi < numToShow; pi++){
	    	 String currPhone = phones.get(pi);
	    	 PlayAudioPanel audiWid = phoneToAudioExample.get(currPhone);
	    	 audiWid.setMinWidth(60);
	    	 audiWid.getElement().getStyle().setWidth(60, Style.Unit.PX);
	    	 goodPhoneScores.setWidget(pi, 0, audiWid);
	    	 goodPhoneScores.setWidget(pi, 1, getScoreBar(phonesToScores.get(currPhone), chart));
	     }
	     Grid badPhoneScores = new Grid(numToShow, 2);
	     for(int pi = phones.size() -1; pi > phones.size()-numToShow-1; pi--){
	    	 String currPhone = phones.get(pi);
	    	 PlayAudioPanel audiWid = phoneToAudioExample.get(currPhone);
	    	 audiWid.setMinWidth(60);
	    	 audiWid.getElement().getStyle().setWidth(60, Style.Unit.PX);
	    	 badPhoneScores.setWidget(numToShow-(phones.size()-pi), 0, audiWid);
	    	 badPhoneScores.setWidget(numToShow-(phones.size()-pi), 1, getScoreBar(phonesToScores.get(currPhone), chart));
	     }
	     HTML goodPhonesTitle = new HTML("Some Sounds You Pronounced Well");
	     goodPhonesTitle.getElement().getStyle().setProperty("fontSize", "130%");
	     goodPhonesTitle.getElement().getStyle().setProperty("color", "#048500");
	     goodPhonesTitle.getElement().getStyle().setProperty("marginBottom", "10px");
	     goodPhonePanel.add(goodPhonesTitle);
	     goodPhonePanel.add(goodPhoneScores);
	     HTML badPhonesTitle = new HTML("Some Sounds You May Need To Improve");
	     badPhonesTitle.getElement().getStyle().setProperty("fontSize", "130%");
	     badPhonesTitle.getElement().getStyle().setProperty("color", "#AD0000");
	     badPhonesTitle.getElement().getStyle().setProperty("marginBottom", "10px");
	     badPhonePanel.add(badPhonesTitle);
	     badPhonePanel.add(badPhoneScores);
  }
  
  private float avg(ArrayList<Float> scores){
	  float sum = 0;
	  for(float f: scores){
		  sum += f;
	  }
	  return sum/scores.size();
  }
  
  public DivWidget getScoreBar(ArrayList<Float> scores, SimpleColumnChart chart){
	  float score = avg(scores);
	  int iscore = (int) (100f * score);
	  final int HEIGHT = 18;
	  DivWidget bar = new DivWidget();
	  TooltipHelper tooltipHelper = new TooltipHelper();
	  bar.setWidth(iscore + "px");
	  bar.setHeight(HEIGHT + "px");
	  bar.getElement().getStyle().setBackgroundColor(chart.getColor(score));
	  bar.getElement().getStyle().setMarginTop(2, Style.Unit.PX);

	  tooltipHelper.createAddTooltip(bar, "Score " + score + "%", Placement.BOTTOM);
	  return bar;
  }
  
  private void setupPlayOrder(final Grid sentPanel, final int currIndex, final int stop){
	  setupPlayOrder(sentPanel, currIndex, stop, new ArrayList<Integer>());
  }
  
  private void setupPlayOrder(final Grid sentPanel, final int currIndex, final int stop, final ArrayList<Integer> reactingSents){
	  if(currIndex >= stop)
		  return;
	  sentPanel.getWidget(currIndex, 0).getElement().getStyle().setProperty("color", "#000000");
	  if(sentPanel.getWidget(currIndex, 2) != null){
		  sentPanel.getWidget(currIndex, 1).setVisible(true);
		  sentPanel.getWidget(currIndex,  2).setVisible(true);
		  final boolean sentsInARow = sentPanel.getWidget(currIndex, 3).getElement().hasClassName("nextIsSame");
		  if(sentsInARow){
			  reactingSents.add(currIndex);
		      setupPlayOrder(sentPanel, currIndex + 1, stop, reactingSents);
		      return;
		  }
		  sentPanel.getWidget(currIndex,  3).setVisible(true);
	  	  ((Button) sentPanel.getWidget(currIndex, 3)).addMouseUpHandler(new MouseUpHandler() {
			  @Override
			  public void onMouseUp(MouseUpEvent e){
				  if(((Button) sentPanel.getWidget(currIndex,  3)).isEnabled()){
					  reactingSents.add(currIndex);
					  for(int i : reactingSents){
						  sentPanel.getWidget(i, 0).getElement().getStyle().setProperty("color", "#B8B8B8");
						  sentPanel.getWidget(i, 2).setVisible(false);
						  sentPanel.getWidget(i, 1).setVisible(false);
						  sentPanel.getWidget(i, 3).setVisible(false);
						  sentPanel.getWidget(i, 5).setVisible(false);
						  sentPanel.getWidget(i, 6).setVisible(false);
					  }
					  if(currIndex+1 != stop){
					     sentPanel.getWidget(currIndex+1, 0).getElement().getStyle().setProperty("color", "#000000");
					  }
					  setupPlayOrder(sentPanel, currIndex + 1, stop, new ArrayList<Integer>());
				  }
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
					setupPlayOrder(sentPanel, currIndex + 1, stop, reactingSents);
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
  
  private String innerScoring(ArrayList<HTML> scoreElements){
	  double sum = 0.0;
	  for(HTML sco : scoreElements){
		//sco.setVisible(true);
		sum += Double.parseDouble(sco.getHTML());
	  }
	  return "Your average score was: "+decF.format(sum/scoreElements.size());
  }
  
  private HashMap<String, String[]> getDialogToPartsMap(){
	  HashMap<String, String[]> m = new HashMap<String, String[]>();
	  m.put("Unit 1: Part 1", new String[] {"Crane", "Wang"});
	  m.put("Unit 1: Part 2", new String[] {"Smith", "Zhao"});
	  m.put("Unit 1: Part 3", new String[] {"Kao", "He"});
	  m.put("Unit 1: Part 4", new String[] {"Mrs. Li", "Mrs. Smith"});
	  return m;
  }
  

  private HashMap<String, String> getSentToAudioPath() {
	  HashMap<String, String> m = new HashMap<String, String>();
	  m.put("Kē Léi'ēn, nǐ hăo!", "/4/regular_1403800547484_by_8.wav");
	  m.put("Nǐ dào năr qù a?", "/13/regular_1403801120710_by_8.wav");
	  m.put("Wŏ huí sùshè.", "/24/regular_1403800638873_by_8.wav");
	  m.put("Wáng Jīngshēng, nǐ hăo!", "/7/regular_1403800587874_by_8.wav");
	  m.put("Wŏ qù túshūguăn. Nĭ ne?", "/20/regular_1403800718502_by_8.wav");
	  
	  m.put("Zhào Guócái, nĭ hăo a!", "/40/regular_1403793765777_by_8.wav");
	  m.put("Hái xíng. Nĭ àirén, háizi dōu hăo ma?", "/57/regular_1403793281208_by_8.wav"); //hi
	  m.put("Wŏ yŏu yìdiănr shìr, xiān zŏule. Zàijiàn!", "/71/regular_1403792961229_by_8.wav");
	  m.put("Nĭ hăo! Hăo jiŭ bú jiànle.", "/44/regular_1403792777589_by_8.wav");
	  m.put("Zěmmeyàng a?", "/45/regular_1403792803630_by_8.wav");
	  m.put("Tāmen dōu hěn hăo, xièxie.", "/63/regular_1403793540903_by_8.wav");
	  m.put("Zàijiàn.", "/72/regular_1403792398921_by_8.wav");
	  
	  m.put("Èi, Lăo Hé!", "/90/regular_1403794727635_by_8.wav");
	  m.put("Xiăo Gāo!", "/93/regular_1403794869759_by_8.wav");
	  m.put("Zuìjìn zěmmeyàng a?", "/96/regular_1403794766781_by_8.wav");
	  m.put("Hái kéyi. Nĭ ne?", "/99/regular_1403795159594_by_8.wav");
	  m.put("Hái shi lăo yàngzi. Nĭ gōngzuò máng bu máng?", "/109/regular_1403795348397_by_8.wav");
	  m.put("Bú tài máng. Nĭ xuéxí zěmmeyàng?", "/115/regular_1403795085285_by_8.wav");
	  m.put("Tĭng jĭnzhāngde.", "/119/regular_1403795511433_by_8.wav");
	  
	  m.put("Xiè Tàitai, huānyíng, huānyíng! Qĭng jìn, qĭng jìn.", "/145/regular_1403797935594_by_8.wav");
	  m.put("Xièxie.", "/146/regular_1403798184425_by_8.wav");
	  m.put("Qĭng zuò, qĭng zuò.", "149/regular_1403798074844_by_8.wav");
	  m.put("Xièxie.", "/146/regular_1403798184425_by_8.wav");
	  m.put("Lĭ Tàitai, wŏ yŏu yìdiăn shì, děi zŏule.", "/154/regular_1403797865456_by_8.wav");
	  m.put("Lĭ Tàitai, xièxie nín le.", "/158/regular_1403797900178_by_8.wav");
	  m.put("Bú kèqi. Màn zŏu a!", "/161/regular_1403798275036_by_8.wav");
	  m.put("Zàijiàn, zàijiàn!", "/162/regular_1403797559758_by_8.wav");
	  
	  return m;
  }
  
  private HashMap<String, String> getSentToSlowAudioPath() {
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
	  
	  m.put("Èi, Lăo Hé!", "/90/slow_1403794714026_by_8.wav");
	  m.put("Xiăo Gāo!", "/93/slow_1403794875313_by_8.wav");
	  m.put("Zuìjìn zěmmeyàng a?", "/96/slow_1403794750850_by_8.wav");
	  m.put("Hái kéyi. Nĭ ne?", "/99/slow_1403795185981_by_8.wav");
	  m.put("Hái shi lăo yàngzi. Nĭ gōngzuò máng bu máng?", "/109/slow_1403795359109_by_8.wav");
	  m.put("Bú tài máng. Nĭ xuéxí zěmmeyàng?", "/115/slow_1403795093515_by_8.wav");
	  m.put("Tĭng jĭnzhāngde.", "/119/slow_1403795517517_by_8.wav");
	  
	  m.put("Xiè Tàitai, huānyíng, huānyíng! Qĭng jìn, qĭng jìn.", "/145/slow_1403797946854_by_8.wav");
	  m.put("Xièxie.", "/146/slow_1403798188972_by_8.wav");
	  m.put("Qĭng zuò, qĭng zuò.", "/149/slow_1403798082119_by_8.wav");
	  m.put("Xièxie.", "/146/slow_1403798188972_by_8.wav");
	  m.put("Lĭ Tàitai, wŏ yŏu yìdiăn shì, děi zŏule.", "/154/slow_1403797874655_by_8.wav");
	  m.put("Lĭ Tàitai, xièxie nín le.", "/158/slow_1403797916593_by_8.wav");
	  m.put("Bú kèqi. Màn zŏu a!", "/161/slow_1403798282629_by_8.wav");
	  m.put("Zàijiàn, zàijiàn!", "/162/slow_1403797569320_by_8.wav");
	  
	  return m;
  }
  
  private HashMap<String, PlayAudioPanel> getPlayAudioWidget(){
	  HashMap<String, PlayAudioPanel> pw = new HashMap<String, PlayAudioPanel>();
	  pw.put("a1",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ma1.mp3").setPlayLabel("a1"));
	  pw.put("a2",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ma2.mp3").setPlayLabel("a2"));
	  pw.put("a3",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/x.mp3").setPlayLabel("a3"));
	  pw.put("a4",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ba4.mp3").setPlayLabel("a4"));
	  pw.put("b",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/bu4.mp3").setPlayLabel("b"));
	  pw.put("c",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/cai2.mp3").setPlayLabel("c"));
	  pw.put("ch",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/x.mp3").setPlayLabel("ch"));
	  pw.put("d",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/dao2.mp3").setPlayLabel("d"));
	  pw.put("e1",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ke1.mp3").setPlayLabel("e1"));
	  pw.put("e2",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/he2.mp3").setPlayLabel("e2"));
	  pw.put("e3",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ye3.mp3").setPlayLabel("e3"));
	  pw.put("e4",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ke4.mp3").setPlayLabel("e4"));
	  pw.put("f",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/x.mp3").setPlayLabel("f"));
	  pw.put("g",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/go1.mp3").setPlayLabel("g"));
	  pw.put("h",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/he2.mp3").setPlayLabel("h"));
	  pw.put("i1",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/shi1.mp3").setPlayLabel("i1"));
	  pw.put("i2",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/yi2.mp3").setPlayLabel("i2"));
	  pw.put("i3",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ni3.mp3").setPlayLabel("i3"));
	  pw.put("i4",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/shi4.mp3").setPlayLabel("i4"));
	  pw.put("j",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ji1ng.mp3").setPlayLabel("j"));
	  pw.put("k",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ke4.mp3").setPlayLabel("k"));
	  pw.put("l",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/lao3.mp3").setPlayLabel("l"));
	  pw.put("m",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ma2.mp3").setPlayLabel("m"));
	  pw.put("n",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ni3.mp3").setPlayLabel("n"));
	  pw.put("ng",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/a2ng.mp3").setPlayLabel("ng"));
	  pw.put("o1",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/zho1.mp3").setPlayLabel("o1"));
	  pw.put("o2",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ro2.mp3").setPlayLabel("o2"));
	  pw.put("o3",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/wo3.mp3").setPlayLabel("o3"));
	  pw.put("o4",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/zuo4.mp3").setPlayLabel("o4"));
	  pw.put("p",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/x.mp3").setPlayLabel("p"));
	  pw.put("q",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/quu4.mp3").setPlayLabel("q"));
	  pw.put("r",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ro2.mp3").setPlayLabel("r"));
	  pw.put("s",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/su4.mp3").setPlayLabel("s"));
	  pw.put("sh",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/shu1.mp3").setPlayLabel("sh"));
	  pw.put("t",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/tu2.mp3").setPlayLabel("t"));
	  pw.put("u1",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/shu1.mp3").setPlayLabel("u1"));
	  pw.put("u2",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/bu2.mp3").setPlayLabel("u2"));
	  pw.put("u3",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/x.mp3").setPlayLabel("u3"));
	  pw.put("u4",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/bu4.mp3").setPlayLabel("u4"));
	  pw.put("uu1",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/x.mp3").setPlayLabel("uu1"));
	  pw.put("uu2",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/xuue2.mp3").setPlayLabel("uu2"));
	  pw.put("uu3",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/x.mp3").setPlayLabel("uu3"));
	  pw.put("uu4",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/x.mp3").setPlayLabel("uu4"));
	  pw.put("w",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/wo3.mp3").setPlayLabel("w"));
	  pw.put("x",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/xuue2.mp3").setPlayLabel("x"));
	  pw.put("y",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/yi2.mp3").setPlayLabel("y"));
	  pw.put("z",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/zou3.mp3").setPlayLabel("z"));
	  pw.put("zh",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/zho1.mp3").setPlayLabel("zh"));
	  pw.put("sil",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/x.mp3").setPlayLabel("sil"));
	  pw.put("unk",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/x.mp3").setPlayLabel("unk"));
	  return pw;
  }
  
  private HashMap<String, HashMap<String, Integer>> getDialogToSpeakerToLast(){
	  HashMap<String, HashMap<String, Integer>> m = new HashMap<String, HashMap<String, Integer>>();
	  String up1 = "Unit 1: Part 1";
	  String up2 = "Unit 1: Part 2";
	  String up3 = "Unit 1: Part 3";
	  String up4 = "Unit 1: Part 4";
	  String u2p1 = "Unit 2: Part 1";
	  String u2p2 = "Unit 2: Part 2";
	  String u2p3 = "Unit 2: Part 3";
	  String u2p4 = "Unit 2: Part 4";
	  m.put(up1, new HashMap<String, Integer>());
	  m.put(up2, new HashMap<String, Integer>());
	  m.put(up3, new HashMap<String, Integer>());
	  m.put(up4, new HashMap<String, Integer>());
	  m.put(u2p1, new HashMap<String, Integer>());
	  m.put(u2p2, new HashMap<String, Integer>());
	  m.put(u2p3, new HashMap<String, Integer>());
	  m.put(u2p4, new HashMap<String, Integer>());
	  m.get(up1).put("Wang", 4);
	  m.get(up1).put("Crane", 3);
	  m.get(up2).put("Smith", 5);
	  m.get(up2).put("Zhao", 6);
	  m.get(up3).put("He", 5);
	  m.get(up3).put("Kao", 6);
	  m.get(up4).put("Mrs. Smith", 7);
	  m.get(up4).put("Mrs. Li", 6);
	  m.get(u2p1).put("Taiwanese Student", 4);
	  m.get(u2p1).put("Parsons", 6);
	  m.get(u2p2).put("First Chinese/American", 9);
	  m.get(u2p2).put("Second Chinese", 10);
	  m.get(u2p3).put("Chinese", 9);
	  m.get(u2p3).put("American", 10);
	  m.get(u2p4).put("Taiwanese Guest", 9);
	  m.get(u2p4).put("Rogers/Holbrooke", 10);
	  
	  return m;
  }
  
  private HashMap<String, HashMap<Integer, String>> getDialogToSentIndexToSpeaker() {
	  HashMap<String, HashMap<Integer, String>> m = new HashMap<String, HashMap<Integer, String>>();
	  String up1 = "Unit 1: Part 1";
	  String up2 = "Unit 1: Part 2";
	  String up3 = "Unit 1: Part 3";
	  String up4 = "Unit 1: Part 4";
	  String u2p1 = "Unit 2: Part 1";
	  String u2p2 = "Unit 2: Part 2";
	  String u2p3 = "Unit 2: Part 3";
	  String u2p4 = "Unit 2: Part 4";
	  m.put(up1, new HashMap<Integer, String>());
	  m.put(up2, new HashMap<Integer, String>());
	  m.put(up3, new HashMap<Integer, String>());
	  m.put(up4, new HashMap<Integer, String>());
	  m.put(u2p1, new HashMap<Integer, String>());
	  m.put(u2p2, new HashMap<Integer, String>());
	  m.put(u2p3, new HashMap<Integer, String>());
	  m.put(u2p4, new HashMap<Integer, String>());
	  
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
	  
	  m.get(up3).put(0, "Kao");
	  m.get(up3).put(1, "He");
	  m.get(up3).put(2, "Kao");
	  m.get(up3).put(3, "He");
	  m.get(up3).put(4, "Kao");
	  m.get(up3).put(5, "He");
	  m.get(up3).put(6, "Kao");
	  
	  m.get(up4).put(0, "Mrs. Li");
	  m.get(up4).put(1, "Mrs. Smith");
	  m.get(up4).put(2, "Mrs. Li");
	  m.get(up4).put(3, "Mrs. Smith");
	  m.get(up4).put(4, "Mrs. Smith");
	  m.get(up4).put(5, "Mrs. Smith");
	  m.get(up4).put(6, "Mrs. Li");
	  m.get(up4).put(7, "Mrs. Smith");
	  
	  m.get(u2p1).put(0, "Taiwanese Student");
	  m.get(u2p1).put(1, "Parsons");
	  m.get(u2p1).put(2, "Taiwanese Student");
	  m.get(u2p1).put(3, "Parsons");
	  m.get(u2p1).put(4, "Taiwanese Student");
	  m.get(u2p1).put(5, "Parsons");
	  m.get(u2p1).put(6, "Parsons");
	  
	  m.get(u2p2).put(0, "First Chinese/American");
	  m.get(u2p2).put(1, "Second Chinese");
	  m.get(u2p2).put(2, "First Chinese/American");
	  m.get(u2p2).put(3, "First Chinese/American");
	  m.get(u2p2).put(4, "First Chinese/American");
	  m.get(u2p2).put(5, "Second Chinese");
	  m.get(u2p2).put(6, "First Chinese/American");
	  m.get(u2p2).put(7, "Second Chinese");
	  m.get(u2p2).put(8, "Second Chinese");
	  m.get(u2p2).put(9, "First Chinese/American");
	  m.get(u2p2).put(10, "Second Chinese");
	  
	  m.get(u2p3).put(0, "American");
	  m.get(u2p3).put(1, "Chinese");
	  m.get(u2p3).put(2, "American");
	  m.get(u2p3).put(3, "American");
	  m.get(u2p3).put(4, "Chinese");
	  m.get(u2p3).put(5, "American");
	  m.get(u2p3).put(6, "Chinese");
	  m.get(u2p3).put(7, "American");
	  m.get(u2p3).put(8, "Chinese");
	  m.get(u2p3).put(9, "Chinese");
	  m.get(u2p3).put(10, "American");
	  
	  m.get(u2p4).put(0, "Rogers/Holbrooke");
	  m.get(u2p4).put(1, "Rogers/Holbrooke");
	  m.get(u2p4).put(2, "Taiwanese Guest");
	  m.get(u2p4).put(3, "Taiwanese Guest");
	  m.get(u2p4).put(4, "Rogers/Holbrooke");
	  m.get(u2p4).put(5, "Rogers/Holbrooke");
	  m.get(u2p4).put(6, "Taiwanese Guest");
	  m.get(u2p4).put(7, "Rogers/Holbrooke");
	  m.get(u2p4).put(8, "Rogers/Holbrooke");
	  m.get(u2p4).put(9, "Taiwanese Guest");
	  m.get(u2p4).put(10, "Rogers/Holbrooke");
	  
	  return m;
  }
  
  private HashMap<String, HashMap<Integer, String>> getDialogToSentIndexToSent() {
	  HashMap<String, HashMap<Integer, String>> m = new HashMap<String, HashMap<Integer, String>>();
	  String up1 = "Unit 1: Part 1";
	  String up2 = "Unit 1: Part 2";
	  String up3 = "Unit 1: Part 3";
	  String up4 = "Unit 1: Part 4";
	  String u2p1 = "Unit 2: Part 1";
	  String u2p2 = "Unit 2: Part 2";
	  String u2p3 = "Unit 2: Part 3";
	  String u2p4 = "Unit 2: Part 4";
	  
	  m.put(up1, new HashMap<Integer, String>());
	  m.put(up2, new HashMap<Integer, String>());
	  m.put(up3, new HashMap<Integer, String>());
	  m.put(up4, new HashMap<Integer, String>());
	  m.put(u2p1, new HashMap<Integer, String>());
	  m.put(u2p2, new HashMap<Integer, String>());
	  m.put(u2p3, new HashMap<Integer, String>());
	  m.put(u2p4, new HashMap<Integer, String>());
	  
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
	  
	  m.get(up3).put(0, "Èi, Lăo Hé!");
	  m.get(up3).put(1, "Xiăo Gāo!");
	  m.get(up3).put(2, "Zuìjìn zěmmeyàng a?");
	  m.get(up3).put(3, "Hái kéyi. Nĭ ne?");
	  m.get(up3).put(4, "Hái shi lăo yàngzi. Nĭ gōngzuò máng bu máng?");
	  m.get(up3).put(5, "Bú tài máng. Nĭ xuéxí zěmmeyàng?");
	  m.get(up3).put(6, "Tĭng jĭnzhāngde.");
	  
	  m.get(up4).put(0, "Xiè Tàitai, huānyíng, huānyíng! Qĭng jìn, qĭng jìn.");
	  m.get(up4).put(1, "Xièxie.");
	  m.get(up4).put(2, "Qĭng zuò, qĭng zuò.");
	  m.get(up4).put(3, "Xièxie.");
	  m.get(up4).put(4, "Lĭ Tàitai, wŏ yŏu yìdiăn shì, děi zŏule.");
	  m.get(up4).put(5, "Lĭ Tàitai, xièxie nín le.");
	  m.get(up4).put(6, "Bú kèqi. Màn zŏu a!");
	  m.get(up4).put(7, "Zàijiàn, zàijiàn!");
	  
	  m.get(u2p1).put(0, "Qĭng wèn, nĭ shì něiguó rén?");
	  m.get(u2p1).put(1, "Wŏ shi Měiguo rén.");
	  m.get(u2p1).put(2, "Nĭ jiào shémme míngzi?");
	  m.get(u2p1).put(3, "Wŏ jiào Bái Jiéruì.");
	  m.get(u2p1).put(4, "Nĭmen dōu shi Měiguo rén ma?");
	  m.get(u2p1).put(5, "Wŏmen bù dōu shi Měiguo rén.");
	  m.get(u2p1).put(6, "Zhèiwèi tóngxué yě shi Měiguo rén, kěshi nèiwèi tóngxué shi Jiā'nádà rén.");
	  
	  m.get(u2p2).put(0, "Qĭng jìn!");
	  m.get(u2p2).put(1, "Èi, Xiăo Mă. Ò.");
	  m.get(u2p2).put(2, "Ò, Xiăo Chén, wŏ gĕi nĭ jièshao yixiar.");
	  m.get(u2p2).put(3, "Zhè shi wŏde xīn tóngwū, tā jiào Wáng Àihuá.");
	  m.get(u2p2).put(4, "Wáng Àihuá, zhè shi wŏde lăo tóngxué, Xiăo Chén.");
	  m.get(u2p2).put(5, "Ò, huānyíng nĭ dào Zhōngguo lái!");
	  m.get(u2p2).put(6, "Hĕn gāoxìng rènshi nĭ, Chén Xiáojie!");
	  m.get(u2p2).put(7, "Ò, bié zhèmme chēnghu wŏ.");
	  m.get(u2p2).put(8, "Hái shi jiào wŏ Xiăo Chén hăole.");
	  m.get(u2p2).put(9, "Xíng. Nà nĭ yě jiào wŏ Xiăo Wáng hăole.");
	  m.get(u2p2).put(10, "Hăo.");
	  
	  m.get(u2p3).put(0, "Nín guìxìng?");
	  m.get(u2p3).put(1, "Wŏ xìng Gāo. Nín guìxìng?");
	  m.get(u2p3).put(2, "Wŏ xìng Wú, Wú Sùshān.");
	  m.get(u2p3).put(3, "Gāo Xiānsheng, nín zài nĕige dānwèi gōngzuò?");
	  m.get(u2p3).put(4, "Wŏ zài Wàijiāobù gōngzuò. Nín ne?");
	  m.get(u2p3).put(5, "Wŏ zài Měiguo Dàshĭguăn gōngzuò.");
	  m.get(u2p3).put(6, "Nèiwèi shi nínde xiānsheng ba?");
	  m.get(u2p3).put(7, "Bú shì, bú shì! Tā shì wŏde tóngshì.");
	  m.get(u2p3).put(8, "Ò, Wú nǚshì, duìbuqĭ.");
	  m.get(u2p3).put(9, "Wŏ yŏu yìdiănshìr, xiān zŏule. Zàijiàn!");
	  m.get(u2p3).put(10, "Zàijiàn!");
	  
	  m.get(u2p4).put(0, "Nĭ hăo! Wŏ jiào Luó Jiésī.");
	  m.get(u2p4).put(1, "Qĭng duō zhĭ jiào.");
	  m.get(u2p4).put(2, "Wŏ xìng Shī. Duìbuqĭ, wŏ méi dài míngpiàn.");
	  m.get(u2p4).put(3, "Wŏ zài Zhōng-Měi Màoyì Gōngsī gōngzuò.");
	  m.get(u2p4).put(4, "Zŏngjīnglĭ, zhè shì Zhōng-Měi Màoyì Gōngsī Shī Xiáojie.");
	  m.get(u2p4).put(5, "À, huānyíng, huānyíng! Wŏ xìng Hóu.");
	  m.get(u2p4).put(6, "Xièxie. Zŏngjīnglĭ yě shi Yīngguo rén ba?");
	  m.get(u2p4).put(7, "Bù, wŏ gēn Luó Xiáojie dōu bú shi Yīngguo rén.");
	  m.get(u2p4).put(8, "Wŏmen shi Měiguo rén.");
	  m.get(u2p4).put(9, "Ò, duìbuqĭ, wŏ găocuòle.");
	  m.get(u2p4).put(10, "Méi guānxi.");
	  
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
    final Panel child = getContentChild(contentPanel);

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
    setScrollPanelWidth(listScrollPanel);
    npfHelper.onResize();
    avpHelper.onResize();
    defectHelper.onResize();
    reviewItem.onResize();
    recorderHelper.onResize();
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
      //row.setWidth((Window.getClientWidth() * 0.95) + "px");
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
          System.err.println("---> did not do deleteList " + ul.getUniqueID());
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

  private class RecorderNPFHelper extends SimpleChapterNPFHelper {
    boolean doNormalRecording;

    public RecorderNPFHelper(LangTestDatabaseAsync service, UserFeedback feedback, UserManager userManager, ExerciseController controller, boolean doNormalRecording) {
      super(service, feedback, userManager, controller, Navigation.this.listInterface);
      this.doNormalRecording = doNormalRecording;
    }

    @Override
    protected ExercisePanelFactory getFactory(final PagingExerciseList exerciseList) {
      return new ExercisePanelFactory(service, feedback, controller, exerciseList) {
        @Override
        public Panel getExercisePanel(final CommonExercise e) {
          //System.out.println("getting exercise for " + e.getID() + " normal rec " +doNormalRecording);
          return new WaveformExercisePanel(e, service, controller, exerciseList, doNormalRecording) {
            @Override
            public void postAnswers(ExerciseController controller, CommonExercise completedExercise) {
              super.postAnswers(controller, completedExercise);
              tellOtherListExerciseDirty(e);
            }
          };
        }
      };
    }

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
              final CheckBox w = new CheckBox(SHOW_ONLY_UNRECORDED);
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
              setOnlyExamples(!doNormalRecording);
            }
          };

        }
      };
    }
  }
}
