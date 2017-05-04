package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.InitialUI;
import mitll.langtest.client.analysis.AnalysisTab;
import mitll.langtest.client.analysis.ShowTab;
import mitll.langtest.client.analysis.StudentAnalysis;
import mitll.langtest.client.custom.ExerciseListContent;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.MarkDefectsChapterNPFHelper;
import mitll.langtest.client.custom.userlist.ListManager;
import mitll.langtest.client.custom.recording.RecorderNPFHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

import static mitll.langtest.client.custom.INavigation.VIEWS.*;

/**
 * Created by go22670 on 4/10/17.
 */
public class NewContentChooser implements INavigation {
  private final Logger logger = Logger.getLogger("NewContentChooser");

  private final DivWidget divWidget = new DivWidget();
  private final ExerciseListContent learnHelper;
  private final ExerciseListContent practiceHelper;
  private final ExerciseController controller;
  private final ListManager listManager;
  private final IBanner banner;

  private VIEWS currentSection = VIEWS.NONE;

  /**
   * @see InitialUI#makeNavigation
   * @param controller
   */
  public NewContentChooser(ExerciseController controller, IBanner banner) {
    NewLearnHelper newLearnHelper = new NewLearnHelper(controller);
    learnHelper = newLearnHelper;
    practiceHelper = new PracticeHelper(controller);
    this.controller = controller;
    this.listManager = new ListManager(controller, null);
    this.banner = banner;
  }

  @Override
  public void showInitialState() {
    clearCurrent();
    showView(LEARN);
    // History.fireCurrentHistoryState();
  }

/*
  private boolean hasProjectChoice() {
    return controller.getProjectStartupInfo() != null;
  }
*/

  @Override
  public void showView(VIEWS view) {
   // logger.info("Got showView " + view  );
    if (currentSection.equals(view)) {
    //  logger.info("showView - already showing " + view);
    } else {
      currentSection = view;
      switch (view) {
        case LEARN:
          clear();
          learnHelper.showContent(divWidget, LEARN.toString());
          break;
        case DRILL:
          clear();
          practiceHelper.showContent(divWidget, DRILL.toString());
          practiceHelper.hideList();
          break;
        case PROGRESS:
          clear();
          showProgress();
          break;
        case LISTS:
          clear();
          divWidget.add(listManager.showLists());
          break;
        case ITEMS:
          // RecorderNPFHelper recorderNPFHelper = new RecorderNPFHelper(controller, true, null);
          logger.info("showRecord  - recorderNPFHelper");
          clear();
          new RecorderNPFHelper(controller, true, null).showNPF(divWidget, ITEMS.toString());
          break;
        case CONTEXT:
          clear();
          RecorderNPFHelper recorderNPFHelper = new RecorderNPFHelper(controller, false, null);
          recorderNPFHelper.showNPF(divWidget, CONTEXT.toString());
          break;
        case DEFECTS:
          clear();
          MarkDefectsChapterNPFHelper markDefectsHelper = new MarkDefectsChapterNPFHelper(controller, null);
          markDefectsHelper.showNPF(divWidget, DEFECTS.toString());
          break;
        case FIX:
          clear();
          listManager.viewReview(divWidget);
          break;
        case NONE:
//          if (hasProjectChoice() && currentSection == NONE) {
//            showView(LEARN);
//          }
//          else {
            logger.info("skipping choice " + view);
//          }
          break;
        default:
          logger.warning("huh? unknown view " + view);
      }
    }
  }

  public void showProgress() {
    boolean hasTeacher = controller.getUserManager().hasPermission(User.Permission.TEACHER_PERM);
    ShowTab showTab = getShowTab();

    DivWidget w = hasTeacher ?
        new StudentAnalysis(controller, showTab) :
        new AnalysisTab(controller, showTab, 1, null, controller.getUser());

    divWidget.add(w);
    currentSection = PROGRESS;
  }

  private void clear() {
    divWidget.clear();
  }

  @NotNull
  private ShowTab getShowTab() {
    return exid -> {
      banner.showLearn();
      learnHelper.loadExercise(exid);
    };
  }

  @Override
  public Widget getNavigation() {
    return divWidget;
  }

  @Override
  public void onResize() {
  }

  @Override
  public void showPreviousState() {
  }

  @Override
  public void clearCurrent() {
    currentSection = NONE;
  }
}