package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.analysis.*;
import mitll.langtest.client.custom.*;
import mitll.langtest.client.custom.recording.RecorderNPFHelper;
import mitll.langtest.client.custom.userlist.ListView;
import mitll.langtest.client.dialog.*;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.flashcard.PolyglotDialog;
import mitll.langtest.client.flashcard.PolyglotDialog.MODE_CHOICE;
import mitll.langtest.client.flashcard.PolyglotDialog.PROMPT_CHOICE;
import mitll.langtest.client.flashcard.StatsFlashcardFactory;
import mitll.langtest.client.initial.InitialUI;
import mitll.langtest.client.initial.UILifecycle;
import mitll.langtest.client.list.FacetExerciseList;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.client.qc.FixNPFHelper;
import mitll.langtest.client.qc.MarkDefectsChapterNPFHelper;
import mitll.langtest.client.quiz.QuizChoiceHelper;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.MatchInfo;
import mitll.langtest.shared.project.ProjectMode;
import mitll.langtest.shared.project.ProjectStartupInfo;
import mitll.langtest.shared.project.ProjectType;
import mitll.langtest.shared.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

import static mitll.langtest.client.custom.INavigation.VIEWS.*;

/**
 * Created by go22670 on 4/10/17.
 */
public class NewContentChooser implements INavigation, ValueChangeHandler<String> {
  private final Logger logger = Logger.getLogger("NewContentChooser");

  private static final String CURRENT_VIEW = "CurrentView";

  private final DivWidget divWidget = new DivWidget();
  private final ExerciseListContent learnHelper;

  private final ExerciseListContent studyHelper;

  private final DialogViewHelper dialogHelper;
  private final ListenViewHelper listenHelper;
  private final ListenViewHelper rehearseHelper;
  private final ListenViewHelper performHelper;

  private final PracticeHelper practiceHelper;
  private final QuizChoiceHelper quizHelper;
  private final ExerciseController controller;
  private final IBanner banner;
  //private final ListView listView;

  private VIEWS currentSection = VIEWS.NONE;
  private HandlerRegistration handlerRegistration;

  private static final boolean DEBUG = false;
  private static final boolean DEBUG_VIEW = false;

  /**
   * @param controller
   * @see InitialUI#makeNavigation
   */
  public NewContentChooser(ExerciseController controller, IBanner banner) {
    learnHelper = new LearnHelper(controller);
    practiceHelper = new PracticeHelper(controller, PRACTICE);
    // quizHelper = new QuizHelper(controller, this);
    quizHelper = new QuizChoiceHelper(controller, this);

    dialogHelper = new DialogViewHelper(controller);

    studyHelper = new StudyHelper<>(controller);
    listenHelper = new ListenViewHelper(controller);
    rehearseHelper = new RehearseViewHelper(controller);
    performHelper = new PerformViewHelper(controller);


    this.controller = controller;
   // this.listView = new ListView(controller);
    this.banner = banner;
    //  divWidget.setId("NewContentChooser");

    divWidget.addStyleName("topFiveMargin");

    addHistoryListener();
  }

  /**
   * @see UILifecycle#showInitialState
   */
  @Override
  public void showInitialState() {
    clearCurrent();
    showView(getCurrentView(), true, false);
  }

  /**
   * So it could be that you've lost a permssion, so the view you were on before isn't allowed for you any more.
   * In which case we have to drop down to an allowed view.
   *
   * @return
   * @see NewBanner#checkProjectSelected
   * @see #showInitialState
   */
  @Override
  @NotNull
  public VIEWS getCurrentView() {
    String currentView = getCurrentStoredView();
    if (DEBUG_VIEW) logger.info("getCurrentView currentView " + currentView);
    VIEWS currentStoredView;
    try {
      currentStoredView = currentView == null ? VIEWS.NONE : // Not sure how this can ever happen but saw exceptions.
          (currentView.isEmpty()) ? getInitialView(isNPQUser()) : VIEWS.valueOf(currentView);
    } catch (IllegalArgumentException e) {
      currentStoredView = VIEWS.NONE;
    }

    {
      Set<User.Permission> userPerms = new HashSet<>(controller.getPermissions());

      //    logger.info("user userPerms " + userPerms + " vs current view perms " + currentStoredView.getPerms());
      List<User.Permission> requiredPerms = currentStoredView.getPerms();
      userPerms.retainAll(requiredPerms);

      if (userPerms.isEmpty() && !requiredPerms.isEmpty()) { // if no overlap, you don't have permission
        logger.info("getCurrentView : user userPerms " + userPerms + " falling back to learn view");
        currentStoredView = controller.getProjectStartupInfo() != null && controller.getProjectStartupInfo().getProjectType() == ProjectType.DIALOG ? DIALOG : LEARN;
      }
    }

    if (currentStoredView == NONE) {
      currentStoredView = controller.getProjectStartupInfo() != null && controller.getProjectStartupInfo().getProjectType() == ProjectType.DIALOG ? DIALOG : LEARN;
    }
    return currentStoredView;
  }

  private boolean isNPQUser() {
    boolean isNPQ = false;
    String affiliation = controller.getUserState().getCurrent().getAffiliation();
    if (affiliation != null && affiliation.equalsIgnoreCase("NPQ")) {
      isNPQ = true;
    }
    return isNPQ;
  }

  @NotNull
  private VIEWS getInitialView(boolean npqUser) {
    return npqUser ? VIEWS.QUIZ : isPolyglotProject() ? PRACTICE : LEARN;
  }

  private boolean isPolyglotProject() {
    ProjectStartupInfo projectStartupInfo = controller.getProjectStartupInfo();
    return projectStartupInfo != null && projectStartupInfo.getProjectType() == ProjectType.POLYGLOT;
  }


  @Override
  public void show(VIEWS views) {
    banner.show(views);
  }

  /**
   * @param view
   * @see StatsFlashcardFactory#showDrill
   */
  @Override
  public void showView(VIEWS view) {
    if (DEBUG) logger.info("showView : show " + view);
    showView(view, false, false);
  }

  /**
   * @param view
   * @param isFirstTime
   * @param fromClick
   * @see NewBanner#showSection(String, boolean)
   */
  @Override
  public void showView(VIEWS view, boolean isFirstTime, boolean fromClick) {
    String currentStoredView = getCurrentStoredView();
    if (DEBUG) {
      logger.info("showView : show " + view + " current " + currentStoredView + " from click " + fromClick);
//      String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception("showView "  + view));
//      logger.info("logException stack " + exceptionAsString);
    }

    // if coming from quiz, clear the list selection.
    boolean keepList = shouldKeepList(currentStoredView);

    if (!currentSection.equals(view)) {
      if (DEBUG) logger.info("showView - showing " + view);

      currentSection = view;
      storeValue(view);
      switch (view) {
        case LEARN:
          clearAndPush(isFirstTime, currentStoredView, LEARN, fromClick, keepList);
          learnHelper.showContent(divWidget, LEARN);
          break;
        case PRACTICE:
          clear();
          setInstanceHistory(PRACTICE, true, keepList);
          showDrill(practiceHelper, PRACTICE);
          break;
        case QUIZ:
          showQuiz(fromClick);
          setInstanceHistory(QUIZ);
          break;
        case PROGRESS:
          clear();
          setInstanceHistory(PROGRESS);
          showProgress();
          break;
        case LISTS:
          clear();
          setInstanceHistory(LISTS);
          new ListView(controller).showContent(divWidget, LISTS);
          break;
        case DIALOG:
          clearAndPush(isFirstTime, currentStoredView, DIALOG, true, keepList);
          dialogHelper.showContent(divWidget, DIALOG);
          break;
        case STUDY:
          clearAndPushKeep(STUDY);
          studyHelper.showContent(divWidget, STUDY);
          break;
        case LISTEN:
          clearAndPushKeep(LISTEN);
          listenHelper.showContent(divWidget, LISTEN);
          break;
        case REHEARSE:
          clearAndPushKeep(REHEARSE);
          rehearseHelper.showContent(divWidget, REHEARSE);
          break;
        case PERFORM:
          clearAndPushKeep(PERFORM);
          performHelper.showContent(divWidget, PERFORM);
          break;
        case SCORES:
          clearAndPushKeep(SCORES);
          showScores(divWidget);
          break;
        case RECORD_ENTRIES:
          clear();
          setInstanceHistory(RECORD_ENTRIES, false, true);
          new RecorderNPFHelper(controller, true, RECORD_ENTRIES).showNPF(divWidget, RECORD_ENTRIES);
          break;
        case RECORD_SENTENCES:
          clear();
          setInstanceHistory(RECORD_SENTENCES, false, true);
          new RecorderNPFHelper(controller, false, RECORD_SENTENCES).showNPF(divWidget, RECORD_SENTENCES);
          break;
        case QC_ENTRIES:
          clear();
          setInstanceHistory(QC_ENTRIES, true, false);
          new MarkDefectsChapterNPFHelper(controller, false).showNPF(divWidget, QC_ENTRIES);
          break;
        case FIX_ENTRIES:
          clear();
          setInstanceHistory(FIX_ENTRIES, true, false);
          new FixNPFHelper(controller, false, FIX_ENTRIES).showNPF(divWidget, FIX_ENTRIES);
          break;
        case QC_SENTENCES:
          clear();
          setInstanceHistory(QC_SENTENCES, true, false);
          new MarkDefectsChapterNPFHelper(controller, true).showNPF(divWidget, QC_SENTENCES);
          break;
        case FIX_SENTENCES:
          clear();
          setInstanceHistory(FIX_SENTENCES, true, false);
          new FixNPFHelper(controller, true, FIX_SENTENCES).showNPF(divWidget, FIX_SENTENCES);
          break;
        case NONE:
          logger.info("showView skipping choice " + view);
          break;
        default:
          logger.warning("showView huh? unknown view " + view);
      }
    } else {
      if (DEBUG) logger.info("showView skip current view " + view);
    }
  }

  private boolean shouldKeepList(String currentStoredView) {
    boolean keepList = true;
    try {
      VIEWS views = valueOf(currentStoredView);
      if (views == QUIZ || views == LISTS) {
        if (DEBUG) logger.info("clear list in url!");
        keepList = false;
      }
    } catch (IllegalArgumentException e) {

    }
    return keepList;
  }

  private void showScores(DivWidget divWidget) {
    int dialog = new SelectionState().getDialog();
    //   if (dialog == -1) {
    controller.getDialogService().getDialog(dialog, new AsyncCallback<IDialog>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("getting dialogs", caught);
      }

      @Override
      public void onSuccess(IDialog dialog) {
        NewContentChooser.this.showScores(divWidget, dialog);
      }
    });
  }

  private void showScores(DivWidget divWidget, IDialog dialog) {
    DivWidget header = new DialogHeader(controller, VIEWS.PERFORM, null).getHeader(dialog);
    header.addStyleName("bottomFiveMargin");
    divWidget.add(header);
    divWidget.add(isTeacher() ?
        new StudentScores(controller) :
        new SessionAnalysis(controller, controller.getUser(), null));
    currentSection = SCORES;
  }

  private void clearAndPush(boolean isFirstTime, String currentStoredView, VIEWS listen, boolean doPushItem, boolean keepList) {
    clear();

    if (DEBUG) logger.info("clearAndPush isFirst " + isFirstTime + " current " + currentStoredView + " now " + listen + " push " + doPushItem);

    if (doPushItem) {
      if (isFirstTime && currentStoredView.isEmpty()) pushFirstUnit();
      setInstanceHistory(listen, true, keepList);
    }
  }

  private void clearAndPushKeep(VIEWS views) {
    clear();

    SelectionState selectionState = new SelectionState();
    if (selectionState.getView() != views) {
      //   logger.info("setInstanceHistory clearing history for instance " + views);
      int dialog = selectionState.getDialog();
      pushItem(getInstanceParam(views) + maybeAddDialogParam(dialog));
    }
    //else {
    //  logger.info("setInstanceHistory NOT clearing history for instance " + views);
    //}
  }

  @NotNull
  private String maybeAddDialogParam(int dialog) {
    return dialog > 0 ? (SelectionState.SECTION_SEPARATOR + SelectionState.DIALOG + "=" + dialog) : "";
  }

  /**
   * @param views
   */
  private void setInstanceHistory(VIEWS views) {
    setInstanceHistory(views, true, true);
  }

  private void setInstanceHistory(VIEWS views, boolean keepTypeToSelection, boolean keepLists) {
    VIEWS currentView = new SelectionState().getView();
    if (currentView != views) {
      String typeToSelection1 = keepLists ? getTypeToSelection() : getTypeToSelectionNoList();
      String typeToSelection = keepTypeToSelection ? typeToSelection1 : "";
      //    logger.info("typeToSelection " + typeToSelection);

      pushItem(getInstanceParam(views) +
          (typeToSelection.isEmpty() ? "" : SelectionState.SECTION_SEPARATOR + typeToSelection));
    } else {
      //  logger.info("setInstanceHistory NOT clearing history for instance " + views);
    }
  }

  @NotNull
  private String getTypeToSelection() {
    Map<String, Collection<String>> typeToSection = getURLTypeToSelection();
    //   logger.info("setInstanceHistory clearing history for instance " + views);
    return getURLParams(typeToSection);
  }

  private Map<String, Collection<String>> getURLTypeToSelection() {
    return new SelectionState(History.getToken(), false).getTypeToSection();
  }

  @NotNull
  private String getTypeToSelectionNoList() {
    Map<String, Collection<String>> typeToSection = getURLTypeToSelection();
    typeToSection.remove("Lists");
    //   logger.info("setInstanceHistory clearing history for instance " + views);
    return getURLParams(typeToSection);
  }

  @NotNull
  private String getURLParams(Map<String, Collection<String>> typeToSection) {
    StringBuilder stringBuilder = new StringBuilder();
    typeToSection.forEach((k, v) -> stringBuilder
        .append(k)
        .append("=")
        .append(v.iterator().next()).append(SelectionState.SECTION_SEPARATOR));
    return stringBuilder.toString();
  }


  /**
   * @see #showView(VIEWS, boolean, boolean)
   */
  private void showDrill(PracticeHelper practiceHelper, VIEWS views) {
    clear();

    if (isPolyglotProject()) {
      showPolyDialog();
    } else {
      showPractice(practiceHelper, views);
    }
  }

  /**
   * @param fromClick
   * @see #showView(VIEWS, boolean, boolean)
   */
  private void showQuiz(boolean fromClick) {
    clear();
    showQuizForReal(fromClick);
  }

  /**
   * @see #showDrill
   */
  private void showPolyDialog() {
    mode = MODE_CHOICE.NOT_YET;

    new PolyglotDialog(
        new DialogHelper.CloseListener() {
          @Override
          public boolean gotYes() {
            mode = candidateMode;
            if (mode == MODE_CHOICE.DRY_RUN) {
              pushFirstUnit();
            } else if (mode == MODE_CHOICE.POLYGLOT) {
              pushSecondUnit();
            } else {
              return false;
            }

            prompt = candidatePrompt;

            return true;
          }

          @Override
          public void gotNo() {
            setBannerVisible(true);
            PracticeHelper practiceHelper = NewContentChooser.this.practiceHelper;
            practiceHelper.setVisible(true);
          }

          @Override
          public void gotHidden() {
            if (mode != MODE_CHOICE.NOT_YET) {
              setBannerVisible(false);
              PracticeHelper practiceHelper = NewContentChooser.this.practiceHelper;
              practiceHelper.setVisible(false);
            }
//        logger.info("mode is " + mode);
            showPractice(practiceHelper, PRACTICE);
          }
        },
        new PolyglotDialog.ModeChoiceListener() {
          @Override
          public void gotMode(MODE_CHOICE choice) {
            candidateMode = choice;
          }

          @Override
          public void gotPrompt(PROMPT_CHOICE choice) {
            candidatePrompt = choice;
          }
        }
    );
  }

  private void showPractice(PracticeHelper toUse, VIEWS views) {
    toUse.setMode(mode);
    toUse.setNavigation(this);
    toUse.showContent(divWidget, views);
    toUse.hideList();
  }

  /**
   * @param fromClick
   * @see #showQuiz(boolean)
   */
  private void showQuizForReal(boolean fromClick) {
//    logger.info("fromClick " + fromClick);
    int list = new SelectionState().getList();
    if (fromClick) {
      quizHelper.showContent(divWidget, QUIZ);
    } else {
      if (list == -1) {
       // logger.info("showQuizForReal no list but not from click");

        quizHelper.showContent(divWidget, QUIZ);
      } else {
      //  logger.info("showQuizForReal NOT from a click, maybe from jump or reload for " + list);
        quizHelper.showChosenQuiz(divWidget);
      }
    }
  }

  private MODE_CHOICE candidateMode = MODE_CHOICE.NOT_YET;
  private MODE_CHOICE mode = MODE_CHOICE.NOT_YET;

  private PROMPT_CHOICE candidatePrompt = PROMPT_CHOICE.NOT_YET;
  private PROMPT_CHOICE prompt = PROMPT_CHOICE.NOT_YET;

  private void pushFirstUnit() {
    ProjectStartupInfo projectStartupInfo = controller.getProjectStartupInfo();

    if (projectStartupInfo != null) {
      List<String> typeOrder = projectStartupInfo.getTypeOrder();
      if (!typeOrder.isEmpty()) {
        String s = typeOrder.get(0);
        Set<MatchInfo> matchInfos = projectStartupInfo.getTypeToDistinct().get(s);
        if (matchInfos != null && !matchInfos.isEmpty()) {
          pushUnitOrChapter(s, matchInfos.iterator().next());
        }
      }
    }
  }

  private void pushUnitOrChapter(String s, MatchInfo next) {
    logger.info("pushUnitOrChapter ");
    pushItem(s + "=" + next.getValue());
  }

  /**
   * Only for polyglot...
   */
  private void pushSecondUnit() {
    ProjectStartupInfo projectStartupInfo = controller.getProjectStartupInfo();

    if (projectStartupInfo != null) {
      List<String> typeOrder = projectStartupInfo.getTypeOrder();
      if (!typeOrder.isEmpty()) {
        String s = typeOrder.get(0);
        //  logger.info("First " + s);
        Set<MatchInfo> matchInfos = projectStartupInfo.getTypeToDistinct().get(s);
        if (!matchInfos.isEmpty()) {
          Iterator<MatchInfo> iterator = matchInfos.iterator();
          MatchInfo next = iterator.next();
          if (iterator.hasNext()) next = iterator.next();
          pushUnitOrChapter(s, next);
        }
      }
    }
  }

  /**
   * So an view specified in the url trumps a stored one in storage, but if there's none in storage, use it.
   *
   * @return
   */
  private String getCurrentStoredView() {
    VIEWS views = getCurrentViewFromURL();
    if (views == NONE) {
      String storedView = getStoredView();
      if (DEBUG_VIEW) logger.info("getCurrentStoredView storedView " + storedView);
      return storedView;
    } else {
      if (DEBUG_VIEW) logger.info("getCurrentStoredView from url view '" + views + "'");
      return views.name();
    }
  }

  /**
   * @return view on url or in storage
   */
  @NotNull
  private VIEWS getCurrentViewFromURL() {
    VIEWS views = new SelectionState().getView();
    if (views == NONE) {
      String storedView = getStoredView();

      if (storedView.isEmpty()) {
        views = VIEWS.NONE;
      } else {
        try {
          views = VIEWS.valueOf(storedView.toUpperCase());
        } catch (IllegalArgumentException e) {
          logger.warning("getCurrentViewFromURL : bad instance " + storedView);
          views = VIEWS.NONE;
        }
      }
    }
    return views;
  }

  /**
   * @param mode
   * @see mitll.langtest.client.project.ProjectChoices#setProjectForUser
   */
  @Override
  public void storeViewForMode(ProjectMode mode) {
    // logger.info("storeViewForMode " + mode);
    //pushItem(""); // clear instance in url
    storeValue(mode == ProjectMode.DIALOG ? DIALOG : LEARN);
  }

  @NotNull
  private String getStoredView() {
    String value = controller.getStorage().getValue(CURRENT_VIEW);
    if (value == null || value.isEmpty()) return "";
    else return value.toUpperCase();
  }

  /**
   * @param view
   */
  private void storeValue(VIEWS view) {
    controller.getStorage().storeValue(CURRENT_VIEW, view.name());
  }

  private void showProgress() {
    boolean polyglotProject = isPolyglotProject();
    divWidget.add(isTeacher() ?
        new StudentAnalysis(controller) :
        new AnalysisTab(controller, polyglotProject, 0, () -> 1, LEARN));

    currentSection = PROGRESS;
  }

  private boolean isTeacher() {
    return controller.getUserManager().hasPermission(User.Permission.TEACHER_PERM);
  }

  /**
   * We want to support proper scrolling in the learn view...
   */
  private void clear() {
    divWidget.clear();
  }

  /**
   * @return
   * @see InitialUI#showNavigation
   */
  @Override
  public Widget getNavigation() {
    return divWidget;
  }

  /**
   * Clear any current selection of unit and chapter before choosing an exercise, so we guarantee it will appear.
   *
   * @param views
   * @return
   */
  @NotNull
  public ShowTab getShowTab(VIEWS views) {
    return (exid) -> {
      banner.show(views);
      pushItem(
          getInstanceParam(views) +
              maybeAddDialogParam(new SelectionState().getDialog()) +
              SelectionState.SECTION_SEPARATOR + SelectionState.ITEM + "=" + exid
      );
    };
  }

  /**
   * @param listid
   * @param view
   * @see mitll.langtest.client.LangTest#showListIn
   */
  @Override
  public void showListIn(int listid, VIEWS view) {
  //  logger.info("showListIn - " + listid + " " + view);
    setHistoryWithList(listid, view);
    banner.show(view, false);
  }

  private void setHistoryWithList(int listid, VIEWS views) {
    // logger.info("showListIn - " + listid + " " + views);
    pushItem(
        SelectionState.SECTION_SEPARATOR +
            FacetExerciseList.LISTS + "=" + listid +
            SelectionState.SECTION_SEPARATOR +
            SelectionState.JUMP + "=" + "true" +
            SelectionState.SECTION_SEPARATOR +
            getInstanceParam(views));
  }

  @NotNull
  private String getInstanceParam(VIEWS view) {
    return SelectionState.INSTANCE + "=" + view.toString();
  }

  @Override
  public void showDialogIn(int dialogid, VIEWS view) {
    // logger.info("showDialogIn - " + dialogid + " " + view);
    pushItem(SelectionState.DIALOG + "=" + dialogid + SelectionState.SECTION_SEPARATOR +
        getInstanceParam(view));
    banner.show(view);
  }

  /**
   * @param url
   */
  private void pushItem(String url) {
   if (DEBUG) logger.info("pushItem - " + url);
    //    String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception("pushItem " + url));
//     logger.info("logException stack " + exceptionAsString);
    History.newItem(url);
  }

  @Override
  public void setBannerVisible(boolean visible) {
    banner.setVisible(visible);
  }

  @Override
  public void onResize() {
  }

  @Override
  public void showPreviousState() {
    showView(getCurrentView(), false, false);
  }

  @Override
  public void clearCurrent() {
    currentSection = NONE;
  }

  private void addHistoryListener() {
    if (handlerRegistration == null) {
      handlerRegistration = History.addValueChangeHandler(this);
    }
  }

/*  private void removeHistoryListener() {
    if (handlerRegistration != null) {
      handlerRegistration.removeHandler();
      handlerRegistration = null;
    }
  }*/

  /**
   * Do forward/back between DIALOG and LISTEN.
   *
   * TODO : add more views - STUDY, SCORE.
   *
   * @param event
   */
  @Override
  public void onValueChange(ValueChangeEvent<String> event) {
    VIEWS instance = new SelectionState().getView();
    if (instance != NONE) {
      try {
        VIEWS views = VIEWS.valueOf(getStoredView());
        VIEWS currentStoredView = VIEWS.valueOf(getCurrentStoredView());

        if (views != currentStoredView) {
          if (DEBUG) logger.info("onValueChange url says               " + views);
          if (DEBUG) logger.info("onValueChange currentStoredView says " + currentStoredView);

          if (currentStoredView.getMode() == ProjectMode.DIALOG) {
            banner.show(currentStoredView);
          }
        }
      } catch (IllegalArgumentException e) {
        logger.warning("got " + e);
      }
    }
  }
}