package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.analysis.AnalysisTab;
import mitll.langtest.client.analysis.ShowTab;
import mitll.langtest.client.analysis.StudentAnalysis;
import mitll.langtest.client.custom.ExerciseListContent;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.MarkDefectsChapterNPFHelper;
import mitll.langtest.client.custom.content.ReviewItemHelper;
import mitll.langtest.client.custom.recording.RecorderNPFHelper;
import mitll.langtest.client.custom.userlist.ListView;
import mitll.langtest.client.dialog.DialogHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.flashcard.PolyglotDialog;
import mitll.langtest.client.flashcard.PolyglotDialog.MODE_CHOICE;
import mitll.langtest.client.flashcard.PolyglotDialog.PROMPT_CHOICE;
import mitll.langtest.client.flashcard.StatsFlashcardFactory;
import mitll.langtest.client.initial.InitialUI;
import mitll.langtest.client.list.FacetExerciseList;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.MatchInfo;
import mitll.langtest.shared.project.ProjectStartupInfo;
import mitll.langtest.shared.project.ProjectType;
import mitll.langtest.shared.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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
  private final DialogViewHelper dialogHelper;
  private final ListenViewHelper listenHelper;
  private final PracticeHelper practiceHelper;
  private final QuizHelper quizHelper;
  private final ExerciseController controller;
  private final IBanner banner;
  private final ListView listView;

  private VIEWS currentSection = VIEWS.NONE;
  private HandlerRegistration handlerRegistration;

  /**
   * @param controller
   * @see InitialUI#makeNavigation
   */
  public NewContentChooser(ExerciseController controller, IBanner banner) {
    learnHelper = new NewLearnHelper(controller, this, LEARN);
    practiceHelper = new PracticeHelper(controller, this, DRILL);
    quizHelper = new QuizHelper(controller, this, VIEWS.QUIZ, this);


    dialogHelper = new DialogViewHelper(controller, this, DIALOG);
    listenHelper = new ListenViewHelper(controller, this, LISTEN);


    this.controller = controller;
    this.listView = new ListView(controller);
    this.banner = banner;
    divWidget.setId("NewContentChooser");
    divWidget.setHeight("100%");

    addHistoryListener();
  }

  /**
   * @see InitialUI#showInitialState
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
    //    logger.info("getCurrentView currentView " + currentView);
    VIEWS currentStoredView = (currentView.isEmpty()) ? getInitialView(isNPQUser()) : VIEWS.valueOf(currentView);

    Set<User.Permission> userPerms = new HashSet<>(controller.getPermissions());

    //    logger.info("user userPerms " + userPerms + " vs current view perms " + currentStoredView.getPerms());
    List<User.Permission> requiredPerms = currentStoredView.getPerms();
    userPerms.retainAll(requiredPerms);

    if (userPerms.isEmpty() && !requiredPerms.isEmpty()) { // if no overlap, you don't have permission
      logger.info("getCurrentView : user userPerms " + userPerms + " falling back to learn view");
      currentStoredView = LEARN;
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
    return npqUser ? VIEWS.QUIZ : isPolyglotProject() ? DRILL : LEARN;
  }

  private boolean isPolyglotProject() {
    ProjectStartupInfo projectStartupInfo = controller.getProjectStartupInfo();
    return projectStartupInfo != null && projectStartupInfo.getProjectType() == ProjectType.POLYGLOT;
  }

  /**
   * @param view
   * @see StatsFlashcardFactory#showDrill
   */
  @Override
  public void showView(VIEWS view) {
    showView(view, false, false);
  }

  @Override
  public void showView(VIEWS view, boolean isFirstTime, boolean fromClick) {
    String currentStoredView = getCurrentStoredView();
    //  logger.info("showView : show " + view + " current " + currentStoredView + " from click " + fromClick);

    if (!currentSection.equals(view)) {
      //  logger.info("showView - already showing " + view);
      //} else {
      currentSection = view;
      storeValue(view);
      switch (view) {
        case LEARN:
          clearAndFixScroll();

          if (isFirstTime && currentStoredView.isEmpty()) pushFirstUnit();

          setInstanceHistory(LEARN);
          learnHelper.showContent(divWidget, LEARN.toString(), fromClick);
          break;
        case DRILL:
          setInstanceHistory(DRILL);
          showDrill();
          break;
        case QUIZ:
          showQuiz(fromClick);
          break;
        case PROGRESS:
          clearAndFixScroll();
          setInstanceHistory(PROGRESS);
          showProgress();
          break;
        case LISTS:
          clearAndFixScroll();
          setInstanceHistory(LISTS);
          listView.showContent(divWidget, "listView", fromClick);
          break;
        case DIALOG:
          clearAndFixScroll();

          if (isFirstTime && currentStoredView.isEmpty()) pushFirstUnit();

          setInstanceHistory(DIALOG);


          dialogHelper.showContent(divWidget, DIALOG.toString(), fromClick);
          break;

        case LISTEN:
          clearAndFixScroll();

          if (isFirstTime && currentStoredView.isEmpty()) pushFirstUnit();

          setInstanceHistory(LISTEN);
          listenHelper.showContent(divWidget, LISTEN.toString(), fromClick);
          break;

        case RECORD:
          clearAndFixScroll();
          setInstanceHistory(RECORD);
          new RecorderNPFHelper(controller, true, this, RECORD).showNPF(divWidget, RECORD.toString());
          break;
        case CONTEXT:
          clearAndFixScroll();
          setInstanceHistory(CONTEXT);
          new RecorderNPFHelper(controller, false, this, CONTEXT).showNPF(divWidget, CONTEXT.toString());
          break;
        case DEFECTS:
          clear();
          setInstanceHistory(DEFECTS);
          new MarkDefectsChapterNPFHelper(controller, this, DEFECTS).showNPF(divWidget, DEFECTS.toString());
          break;
        case FIX:
          clear();
          setInstanceHistory(FIX);
          getReviewList();
          break;
        case NONE:
          logger.info("skipping choice " + view);
          break;
        default:
          logger.warning("huh? unknown view " + view);
      }
    }
  }

  private void setInstanceHistory(VIEWS views) {
    if (!getCurrentInstance().equalsIgnoreCase(views.toString())) {
      //   logger.info("setInstanceHistory clearing history for instance " + views);
      History.newItem(SelectionState.INSTANCE + "=" + views.toString());
    } else {
      //  logger.info("setInstanceHistory NOT clearing history for instance " + views);
    }
  }

  private void clearAndFixScroll() {
    clear();
    fixDivToNotScrollUnderHeader();
  }

  private void fixDivToNotScrollUnderHeader() {
    divWidget.getElement().getStyle().setOverflow(Style.Overflow.AUTO);
    divWidget.getElement().getStyle().setPosition(Style.Position.FIXED);
  }

  //  @Override
  private void showDrill() {
    clearAndFixScroll();

    if (isPolyglotProject()) {
      showPolyDialog();
    } else {
      showPractice();
    }
  }

  private void showQuiz(boolean fromClick) {
    clearAndFixScroll();
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
            showPractice();
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

  private void showPractice() {
    practiceHelper.setMode(mode, prompt);
    practiceHelper.setNavigation(this);
    practiceHelper.showContent(divWidget, DRILL.toString(), true);
    practiceHelper.hideList();
  }

  /**
   * @param fromClick
   * @see #showQuiz(boolean)
   */
  private void showQuizForReal(boolean fromClick) {
    quizHelper.setMode(mode, prompt);
    quizHelper.setNavigation(this);
    quizHelper.showContent(divWidget, VIEWS.QUIZ.toString(), fromClick);
    quizHelper.hideList();
    if (fromClick) quizHelper.showQuizIntro();
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
    // logger.info("pushUnitOrChapter ");
    History.newItem(s + "=" + next.getValue());
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

  private void getReviewList() {
    controller.getListService().getReviewList(new AsyncCallback<UserList<CommonShell>>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("getting defect list", caught);
      }

      @Override
      public void onSuccess(UserList<CommonShell> result) {
        showReviewItems(result);
      }
    });
  }

  /**
   * So an view specified in the url trumps a stored one in storage, but if there's none in storage, use it.
   *
   * @return
   */
  private String getCurrentStoredView() {
    String instance = getCurrentInstance();
    // logger.info("getCurrentStoredView instance = " + instance);

    VIEWS views = null;
    try {
      views = instance.isEmpty() ? null : VIEWS.valueOf(instance.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.info("bad instance " + instance);
    }
    //logger.info("getCurrentStoredView instance = " + instance + "/" + views);

//    return views == null ? controller.getStorage().getValue(CURRENT_VIEW).toUpperCase() : views.toString().toUpperCase();

    if (views == null) {
      String value = controller.getStorage().getValue(CURRENT_VIEW);
      if (value == null || value.isEmpty()) return "";
      else return value.toUpperCase();
    } else {
      return views.toString().toUpperCase();
    }
  }


  /**
   * @return
   */
  private String getCurrentInstance() {
    return new SelectionState(History.getToken(), false).getInstance();
  }

  private void storeValue(VIEWS view) {
    controller.getStorage().storeValue(CURRENT_VIEW, view.name());
  }

  private void showReviewItems(UserList<CommonShell> result) {
    List<CommonShell> exercises = result.getExercises();
    // logger.info("got back " + result.getNumItems() + " exercises");
    CommonShell toSelect = exercises.isEmpty() ? null : exercises.get(0);
    Panel review = new ReviewItemHelper(controller)
        .doNPF(result, "review", true, toSelect);
    if (getCurrentView() == VIEWS.FIX) {
      divWidget.add(review);
    } else {
      logger.warning("not adding review since current is " + getCurrentView());
    }
  }

  private void showProgress() {
    boolean polyglotProject = isPolyglotProject();
    divWidget.add(isTeacher() ?
        new StudentAnalysis(controller) :
        new AnalysisTab(controller, polyglotProject, 0, () -> 1));

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

    Style style = divWidget.getElement().getStyle();
    style.clearProperty("overflow");
    style.clearProperty("position");
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
   * @return
   */
  @NotNull
  public ShowTab getShowTab() {
    return exid -> {
      boolean wasMade = learnHelper.getReloadable() != null;
      //   logger.info("getShowTab history - " + History.getToken());
      if (!wasMade) {
        banner.show(LEARN);
      }
      learnHelper.loadExercise(exid);
      if (wasMade) {
        banner.show(LEARN);
      }

      //   logger.info("getShowTab history after - " + History.getToken());

      History.newItem(
          SelectionState.INSTANCE + "=" + LEARN.toString() +
              SelectionState.SECTION_SEPARATOR + SelectionState.ITEM + "=" + exid
      );
    };
  }

  @Override
  public void showListIn(int listid, VIEWS view) {
    // logger.info("showListIn - " + listid + " " + view);
    setHistoryWithList(listid, view);
    banner.show(view);
  }
  private void setHistoryWithList(int listid, VIEWS views) {
    // logger.info("showListIn - " + listid + " " + views);
    History.newItem(
        FacetExerciseList.LISTS + "=" + listid + SelectionState.SECTION_SEPARATOR +
            SelectionState.INSTANCE + "=" + views.toString());
  }

  @Override
  public void showDialogIn(int dialogid, VIEWS view) {
    // logger.info("showListIn - " + listid + " " + view);
    History.newItem(
        SelectionState.DIALOG + "=" + dialogid + SelectionState.SECTION_SEPARATOR +
            SelectionState.INSTANCE + "=" + view.toString());
    banner.show(view);
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

  private void removeHistoryListener() {
    if (handlerRegistration != null) {
      handlerRegistration.removeHandler();
      handlerRegistration = null;
    }
  }
  @Override
  public void onValueChange(ValueChangeEvent<String> event) {
    SelectionState selectionState=new SelectionState();
    String instance = selectionState.getInstance();
    if (!instance.isEmpty()) {
      try {
        VIEWS views = VIEWS.valueOf(instance.toUpperCase());
        logger.info("url says " + views);
        String currentStoredView = getCurrentStoredView();
        logger.info("currentStoredView says " + currentStoredView);

      } catch (IllegalArgumentException e) {
        e.printStackTrace();
      }
    }

  }
}