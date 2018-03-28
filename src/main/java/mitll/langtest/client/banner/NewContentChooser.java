package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
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
import mitll.langtest.client.initial.InitialUI;
import mitll.langtest.client.list.FacetExerciseList;
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
public class NewContentChooser implements INavigation {
  private final Logger logger = Logger.getLogger("NewContentChooser");

  private static final String CURRENT_VIEW = "CurrentView";
  private final DivWidget divWidget = new DivWidget();
  private final ExerciseListContent learnHelper;
  private final PracticeHelper practiceHelper;
  private final ExerciseController controller;
  private final IBanner banner;
  private final ListView listView;

  private VIEWS currentSection = VIEWS.NONE;

  private static final int DRY_RUN_MINUTES = 1;
  private static final int ROUND_MINUTES = 10;

  private static final int DRY_NUM = 10;
  private static final int COMP_NUM = 100;

  private static final int MIN_POLYGLOT_SCORE = 35;

  /**
   * @param controller
   * @see InitialUI#makeNavigation
   */
  public NewContentChooser(ExerciseController controller, IBanner banner) {
    learnHelper = new NewLearnHelper(controller);
    practiceHelper = new PracticeHelper(controller);
    this.controller = controller;
    this.listView = new ListView(controller);
    this.banner = banner;
    divWidget.setId("NewContentChooser");
    divWidget.setHeight("100%");
  }

  /**
   * @see InitialUI#showInitialState
   */
  @Override
  public void showInitialState() {
    clearCurrent();
    showView(getCurrentView(), true);
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
    //   logger.info("currentView " + currentView);
    VIEWS currentStoredView = (currentView.isEmpty()) ? getInitialView() : VIEWS.valueOf(currentView);

    Set<User.Permission> userPerms = new HashSet<>(controller.getPermissions());

//    logger.info("user userPerms " + userPerms + " vs current view perms " + currentStoredView.getPerms());
    List<User.Permission> requiredPerms = currentStoredView.getPerms();
    userPerms.retainAll(requiredPerms);

//    logger.info("user userPerms " + userPerms + " overlap =  " + userPerms);

    if (userPerms.isEmpty() && !requiredPerms.isEmpty() || currentStoredView == NONE) { // if no overlap, you don't have permission
      logger.info("getCurrentView : user userPerms " + userPerms + " falling back to learn view");
      currentStoredView = LEARN;
    }

    return currentStoredView;
  }

  @NotNull
  private VIEWS getInitialView() {
    return isPolyglot() ? DRILL : LEARN;
  }

  private boolean isPolyglot() {
    ProjectStartupInfo projectStartupInfo = controller.getProjectStartupInfo();
    return projectStartupInfo != null && projectStartupInfo.getProjectType() == ProjectType.POLYGLOT;
  }

  @Override
  public void showView(VIEWS view, boolean isFirstTime) {
    String currentStoredView = getCurrentStoredView();
    //   logger.info("showView : show " + view + " current " + currentStoredView);

    if (!currentSection.equals(view)) {
      //  logger.info("showView - already showing " + view);
      //} else {
      currentSection = view;
      storeValue(view);
      switch (view) {
        case LEARN:
          clear();

          fixDivToNotScrollUnderHeader();

          if (isFirstTime && currentStoredView.isEmpty()) pushFirstUnit();

          learnHelper.showContent(divWidget, LEARN.toString());
          break;
        case DRILL:
          showDrill();
          break;
        case PROGRESS:
          clear();
          fixDivToNotScrollUnderHeader();
          showProgress();
          break;
        case LISTS:
          clear();
          fixDivToNotScrollUnderHeader();
          listView.showContent(divWidget, "listView");
          break;
        case RECORD:
          clear();
          new RecorderNPFHelper(controller, true).showNPF(divWidget, RECORD.toString());
          break;
        case CONTEXT:
          clear();
          new RecorderNPFHelper(controller, false).showNPF(divWidget, CONTEXT.toString());
          break;
        case DEFECTS:
          clear();
          new MarkDefectsChapterNPFHelper(controller).showNPF(divWidget, DEFECTS.toString());
          break;
        case FIX:
          clear();
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

  private void fixDivToNotScrollUnderHeader() {
    divWidget.getElement().getStyle().setOverflow(Style.Overflow.AUTO);
    divWidget.getElement().getStyle().setPosition(Style.Position.FIXED);
  }

  @Override
  public void showDrill() {
    clear();
    fixDivToNotScrollUnderHeader();

    if (isPolyglot()) {
      showPolyDialog();
    } else {
      showPractice();
    }
  }

  /**
   *
   */
  private void showPolyDialog() {
    mode = MODE_CHOICE.NOT_YET;

    new PolyglotDialog(
        DRY_RUN_MINUTES, DRY_NUM,
        ROUND_MINUTES, COMP_NUM,

        MIN_POLYGLOT_SCORE,
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
            practiceHelper.setVisible(true);
          }

          @Override
          public void gotHidden() {
            if (mode != MODE_CHOICE.NOT_YET) {
              setBannerVisible(false);
              practiceHelper.setVisible(false);
            }
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

//  private void setBannerVisible(boolean vi) {
//    controller.setBannerVisible(false);
//  }

  private void showPractice() {
    practiceHelper.setMode(mode, prompt);
    practiceHelper.setNavigation(this);
    practiceHelper.showContent(divWidget, DRILL.toString());
    practiceHelper.hideList();
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
        //  logger.info("First " + s);
        Set<MatchInfo> matchInfos = projectStartupInfo.getTypeToDistinct().get(s);
        if (!matchInfos.isEmpty()) {
          MatchInfo next = matchInfos.iterator().next();
          String value = next.getValue();
          //  logger.info("First " + s + " = "+ value);
          History.newItem(s + "=" + value);
        }
      }
    }
  }

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
          String value = next.getValue();
          //  logger.info("First " + s + " = "+ value);
          History.newItem(s + "=" + value);
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

  private String getCurrentStoredView() {
    return controller.getStorage().getValue(CURRENT_VIEW);
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
    divWidget.add(review);
  }

  public void showProgress() {
    divWidget.add(isTeacher() ?
        new StudentAnalysis(controller) :
        new AnalysisTab(controller, isPolyglot(), 0, () -> 1));

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

  /*
  @NotNull
  private ShowTab getShowTab() {
    return exid -> {
      boolean wasMade = learnHelper.getReloadable() != null;
      if (!wasMade) {
        banner.showLearn();
      }
      learnHelper.loadExercise(exid);
      if (wasMade) {
        banner.showLearn();
      }
    };
  }*/

  @Override
  public void showLearnList(int listid) {
    setHistoryWithList(listid);
    banner.showLearn();
  }

  /**
   * @param listid
   * @see mitll.langtest.client.LangTest#showDrillList
   */
  @Override
  public void showDrillList(int listid) {
    setHistoryWithList(listid);
    banner.showDrill();
  }

  @Override
  public void setBannerVisible(boolean visible) {
    banner.setVisible(visible);
  }

  private void setHistoryWithList(int listid) {
    History.newItem(FacetExerciseList.LISTS + "=" + listid);
  }

  @Override
  public void onResize() {
  }

  @Override
  public void showPreviousState() {
    showView(getCurrentView(), false);
  }

  @Override
  public void clearCurrent() {
    currentSection = NONE;
  }
}