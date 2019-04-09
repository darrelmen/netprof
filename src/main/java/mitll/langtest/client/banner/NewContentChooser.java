/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.analysis.*;
import mitll.langtest.client.custom.ContentView;
import mitll.langtest.client.custom.ExerciseListContent;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.KeyStorage;
import mitll.langtest.client.custom.recording.RecorderNPFHelper;
import mitll.langtest.client.custom.userlist.ListView;
import mitll.langtest.client.dialog.*;
import mitll.langtest.client.exercise.ExerciseController;
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
import mitll.langtest.shared.user.Permission;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

import static mitll.langtest.client.custom.INavigation.VIEWS.*;
import static mitll.langtest.shared.project.ProjectMode.VOCABULARY;

/**
 * Created by go22670 on 4/10/17.
 */
public class NewContentChooser implements INavigation, ValueChangeHandler<String> {
  public static final String MODE = "Mode";
  public static final String LISTS = "Lists";
  private final Logger logger = Logger.getLogger("NewContentChooser");

  private static final String CURRENT_VIEW = "CurrentView";

  private final DivWidget divWidget = new DivWidget();
  private final ExerciseListContent learnHelper;

  private final ExerciseListContent studyHelper;

  private final DialogViewHelper dialogHelper;
  private final ListenViewHelper listenHelper;
  private final ListenViewHelper rehearseHelper, coreRehearseHelper;
  private final ListenViewHelper performPressAndHoldHelper, performHelper;

  private final ContentView oovHelper;

  private final PracticeHelper practiceHelper;
  private final QuizChoiceHelper quizHelper;
  private final ExerciseController<?> controller;
  private final IBanner banner;

  private VIEWS currentSection = VIEWS.NONE;
  private HandlerRegistration handlerRegistration;

  private static final boolean DEBUG = false;
  private static final boolean DEBUG_PUSH_ITEM = false;
  private static final boolean DEBUG_VIEW = false;

  /**
   * @param controller
   * @see InitialUI#makeNavigation
   */
  public NewContentChooser(ExerciseController controller, IBanner banner) {
    learnHelper = new LearnHelper(controller);
    practiceHelper = new PracticeHelper(controller, PRACTICE);
    quizHelper = new QuizChoiceHelper(controller, this);

    dialogHelper = new DialogViewHelper(controller);

    studyHelper = new StudyHelper<>(controller);
    listenHelper = new ListenViewHelper(controller, LISTEN);
    rehearseHelper = new RehearseViewHelper(controller, REHEARSE);
    coreRehearseHelper = new CoreRehearseViewHelper(controller, CORE_REHEARSE);
    performPressAndHoldHelper = new PerformViewHelper(controller, PERFORM_PRESS_AND_HOLD);
    performHelper = new PerformViewHelper(controller, PERFORM);
    oovHelper = new OOVViewHelper(controller);
    this.controller = controller;
    this.banner = banner;

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

  public VIEWS getCurrent() {
    return currentSection;
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
    boolean hasStartup = controller.getProjectStartupInfo() != null;
    if (DEBUG_VIEW) {
      logger.info("getCurrentView currentView '" + currentView + "' has startup " + hasStartup);
    }
    VIEWS currentStoredView;
    try {
      currentStoredView = currentView == null ? VIEWS.NONE : // Not sure how this can ever happen but saw exceptions.
          (currentView.isEmpty()) ? getInitialView() : VIEWS.valueOf(currentView);
    } catch (IllegalArgumentException e) {
      currentStoredView = VIEWS.NONE;
    }

//    ProjectType projectType = hasStartup ? controller.getProjectStartupInfo().getProjectType() : ProjectType.NP;

    ProjectMode storedMode = VOCABULARY;
    if (hasStartup) {
      String value = getStorage().getValue(MODE);
      if (value != null && !value.isEmpty()) {
        storedMode = ProjectMode.valueOf(value);
        //ProjectMode viewMode = currentStoredView.getMode();
      //  logger.info("getCurrentView : storedMode " + storedMode + " mode " + viewMode);
      }
    }
    {
      Set<Permission> userPerms = new HashSet<>(controller.getPermissions());

      //  logger.info("getCurrentView user userPerms " + userPerms + " vs current view perms " + currentStoredView.getPerms());
      List<Permission> requiredPerms = currentStoredView.getPerms();
      userPerms.retainAll(requiredPerms);

      if (userPerms.isEmpty() && !requiredPerms.isEmpty()) { // if no overlap, you don't have permission
        logger.info("getCurrentView : user userPerms " + userPerms + " falling back to default view for (" + storedMode + ")");
        currentStoredView = hasStartup && storedMode == ProjectMode.DIALOG ? DIALOG : LEARN;
      } else /*if (!userPerms.isEmpty())*/ {
        if (hasStartup) {
          //String value = getStorage().getValue(MODE);
          // logger.info("selected mode is " + value);

          try {
            //ProjectMode storedMode = ProjectMode.valueOf(value);

            ProjectMode viewMode = currentStoredView.getMode();
            // logger.info("getCurrentView : storedMode " + storedMode + " mode " + viewMode);

            if (viewMode == ProjectMode.DIALOG && storedMode == ProjectMode.VOCABULARY) {
              // currentStoredView = LEARN;
              logger.warning("force learn view?");
            } else if (viewMode == VOCABULARY && storedMode == ProjectMode.DIALOG) {
              //currentStoredView = DIALOG;
              logger.warning("force dialog view?");
            } else {
              //   logger.info("OK - no inconsistency...");
            }

          } catch (IllegalArgumentException e) {
            e.printStackTrace();
          }

          //ProjectType projectType = controller.getProjectStartupInfo().getProjectType();

        }
      }
    }

    if (currentStoredView == NONE) {
      currentStoredView = hasStartup && storedMode == ProjectMode.DIALOG ? DIALOG : LEARN;
    }

    return currentStoredView;
  }

  @NotNull
  private VIEWS getInitialView() {
    return isPolyglotProject() ? PRACTICE : LEARN;
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
      logger.info("showView : " +
          "\n\tshow    " + view +
          "\n\tcurrent " + currentStoredView + " from click " + fromClick);

//      String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception("showView " + view));
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
          setInstanceHistory(VIEWS.LISTS);
          new ListView(controller).showContent(divWidget, VIEWS.LISTS);
          break;
        case DIALOG:
          clearAndPush(isFirstTime, currentStoredView, DIALOG, true, keepList);
          dialogHelper.showContent(divWidget, DIALOG);
          break;
        case STUDY:
          clearPushAndShow(studyHelper, STUDY);
          break;
        case LISTEN:
          clearPushAndShow(listenHelper, LISTEN);
          break;
        case REHEARSE:
          clearPushAndShow(rehearseHelper, REHEARSE);
          break;
        case CORE_REHEARSE:
          clearPushAndShow(coreRehearseHelper, CORE_REHEARSE);
          break;
        case PERFORM:
          clearPushAndShow(performHelper, PERFORM);
          break;
        case PERFORM_PRESS_AND_HOLD:
          clearPushAndShow(performPressAndHoldHelper, PERFORM_PRESS_AND_HOLD);
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

        case OOV_EDITOR:
          clearPushAndShow(oovHelper, OOV_EDITOR);
          break;
        case NONE:
          logger.warning("showView skipping choice '" + view + "'");
          break;
        default:
          logger.warning("showView huh? unknown view " + view);
      }
    } else {
      if (DEBUG || true) logger.warning("showView skip current view " + view);
    }
  }

  private void clearPushAndShow(ContentView contentView, VIEWS perform) {
    clearAndPushKeep(perform);
    //   logger.info("show perform " + PERFORM);
    contentView.showContent(divWidget, perform);
  }

  private boolean shouldKeepList(String currentStoredView) {
    boolean keepList = true;
    try {
      VIEWS views = valueOf(currentStoredView);
      if (views == QUIZ || views == VIEWS.LISTS) {
        if (DEBUG) logger.info("clear list in url!");
        keepList = false;
      }
    } catch (IllegalArgumentException e) {

    }
    return keepList;
  }

  private void showScores(DivWidget divWidget) {
    controller.getDialogService().getDialog(new SelectionState().getDialog(), new AsyncCallback<IDialog>() {
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
    DivWidget header = new DialogHeader(controller, SCORES, VIEWS.PERFORM, null).getHeader(dialog);
    header.addStyleName("bottomFiveMargin");
    divWidget.add(header);
    divWidget.add(isTeacher() ?
        new StudentScores(controller) :
        new SessionAnalysis(controller, controller.getUser(), null));
    currentSection = SCORES;
  }

  private void clearAndPush(boolean isFirstTime, String currentStoredView, VIEWS listen, boolean doPushItem, boolean keepList) {
    clear();

    if (DEBUG)
      logger.info("clearAndPush isFirst " + isFirstTime + " current " + currentStoredView + " now " + listen + " push " + doPushItem);

    if (doPushItem) {
      if (isFirstTime && currentStoredView.isEmpty()) pushFirstUnit();
      setInstanceHistory(listen, true, keepList);
    }
  }

  /**
   * @param views
   * @see #showView(VIEWS, boolean, boolean)
   * @see #clearPushAndShow(ContentView, VIEWS)
   */
  private void clearAndPushKeep(VIEWS views) {
    clear();

    SelectionState selectionState = new SelectionState();
    if (selectionState.getView() != views) {
      //   logger.info("setInstanceHistory clearing history for instance " + views);
      // int dialog = selectionState.getDialog();
      pushItem(getInstanceParam(views) + maybeAddDialogParam(selectionState.getDialog()));
    } else {
      logger.info("setInstanceHistory NOT clearing history for instance " + views);


      String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception("views " + views));
      logger.info("logException stack " + exceptionAsString);
    }
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
    }
    //else {
    //  logger.info("setInstanceHistory NOT clearing history for instance " + views);
    //}
  }

  @NotNull
  private String getTypeToSelection() {
    // Map<String, Collection<String>> typeToSection = getURLTypeToSelection();
    //   logger.info("setInstanceHistory clearing history for instance " + views);
    return getURLParams(getURLTypeToSelection());
  }

  private Map<String, Collection<String>> getURLTypeToSelection() {
    return new SelectionState(History.getToken(), false).getTypeToSection();
  }

  @NotNull
  private String getTypeToSelectionNoList() {
    Map<String, Collection<String>> typeToSection = getURLTypeToSelection();
    typeToSection.remove(LISTS);
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

//    if (isPolyglotProject()) {
//      showPolyDialog();
//    } else {
    showPractice(practiceHelper, views);
//    }
  }

  /**
   * @param fromClick
   * @see #showView(VIEWS, boolean, boolean)
   */
  private void showQuiz(boolean fromClick) {
    clear();
    showQuizForReal(fromClick);
  }

  private void showPractice(PracticeHelper toUse, VIEWS views) {
    //  toUse.setMode(mode);
    toUse.setNavigation(this);
    toUse.showContent(divWidget, views);
    toUse.hideList();
  }

  /**
   * TODO: add option so when we jump from list view, the time remaining on the quiz will reset!
   *
   * @param fromClick
   * @see #showQuiz(boolean)
   */
  private void showQuizForReal(boolean fromClick) {
    int list = new SelectionState().getList();

    if (DEBUG) logger.info("showQuizForReal fromClick " + fromClick + " for list " + list);

    if (fromClick || list == -1) {
      quizHelper.showContent(divWidget, QUIZ);
    } else {
      if (DEBUG) logger.info("showQuizForReal NOT from a click, maybe from jump or reload for " + list);
      quizHelper.showChosenQuiz(divWidget);
    }
  }

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
    //  logger.info("pushUnitOrChapter ");
    pushItem(s + "=" + next.getValue());
  }

  /**
   * Only for polyglot...
   */
/*  private void pushSecondUnit() {
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
  }*/

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
    getStorage().storeValue(MODE, mode.name());

    if (DEBUG) {
      logger.info("storeViewForMode OK mode now = " + controller.getMode());
    }
  }

  @NotNull
  private String getStoredView() {
    String value = getStorage().getValue(CURRENT_VIEW);
    if (value == null || value.isEmpty()) return "";
    else return value.toUpperCase();
  }

  private KeyStorage getStorage() {
    return controller.getStorage();
  }

  /**
   * @param view
   */
  private void storeValue(VIEWS view) {
    KeyStorage storage = getStorage();
    if (storage != null && view != null) {
      storage.storeValue(CURRENT_VIEW, view.name());
    }
  }

  private void showProgress() {
    boolean polyglotProject = isPolyglotProject();
    divWidget.add(isTeacher() ?
        new StudentAnalysis(controller) :
        new AnalysisTab(controller, polyglotProject, 0, () -> 1, LEARN, -1));

    currentSection = PROGRESS;
  }

  private boolean isTeacher() {
    return controller.getUserManager().hasPermission(Permission.TEACHER_PERM);
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
      banner.show(views, false);
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
    if (DEBUG_PUSH_ITEM) logger.info("pushItem - " + url);
//    String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception("pushItem " + url));
//    logger.info("logException stack " + exceptionAsString);

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