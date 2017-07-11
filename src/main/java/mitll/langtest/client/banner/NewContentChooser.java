package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.analysis.AnalysisTab;
import mitll.langtest.client.analysis.ShowTab;
import mitll.langtest.client.analysis.StudentAnalysis;
import mitll.langtest.client.custom.ExerciseListContent;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.MarkDefectsChapterNPFHelper;
import mitll.langtest.client.custom.recording.RecorderNPFHelper;
import mitll.langtest.client.custom.userlist.ListManager;
import mitll.langtest.client.custom.userlist.ListView;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.initial.InitialUI;
import mitll.langtest.client.list.FacetExerciseList;
import mitll.langtest.shared.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

import static mitll.langtest.client.custom.INavigation.VIEWS.*;

/**
 * Created by go22670 on 4/10/17.
 */
public class NewContentChooser implements INavigation {
  public static final String CURRENT_VIEW = "CurrentView";
  private final Logger logger = Logger.getLogger("NewContentChooser");

  private final DivWidget divWidget = new DivWidget();
  private final ExerciseListContent learnHelper;
  private final ExerciseListContent practiceHelper;
  private final ExerciseController controller;
  private final ListManager listManager;
  private final IBanner banner;
  private final ListView listView;

  private VIEWS currentSection = VIEWS.NONE;

  /**
   * @param controller
   * @see InitialUI#makeNavigation
   */
  public NewContentChooser(ExerciseController controller, IBanner banner) {
    learnHelper = new NewLearnHelper(controller);
    practiceHelper = new PracticeHelper(controller);
    this.controller = controller;
    this.listManager = new ListManager(controller, null);
    this.listView = new ListView(controller);
    this.banner = banner;
  }

  @Override
  public void showInitialState() {
    clearCurrent();
    showView(getCurrentView());
  }

  @Override
  @NotNull
  public VIEWS getCurrentView() {
    String currentView = controller.getStorage().getValue(CURRENT_VIEW);
    return (currentView.isEmpty()) ? LEARN : VIEWS.valueOf(currentView);
  }

  @Override
  public void showView(VIEWS view) {
    // logger.info("Got showView " + view  );
    if (currentSection.equals(view)) {
      //  logger.info("showView - already showing " + view);
    } else {
      currentSection = view;
      storeValue(view);
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
//          divWidget.add(listManager.showLists());
          listView.showContent(divWidget, "listView");
          break;
        case ITEMS:
          clear();
          new RecorderNPFHelper(controller, true).showNPF(divWidget, ITEMS.toString());
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
          listManager.viewReview(divWidget);  // TODO : no more list manager
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

  private void storeValue(VIEWS view) {
    controller.getStorage().storeValue(CURRENT_VIEW, view.name());
  }

  public void showProgress() {
    boolean hasTeacher = controller.getUserManager().hasPermission(User.Permission.TEACHER_PERM);
    ShowTab showTab = getShowTab();

    DivWidget w = hasTeacher ?
        new StudentAnalysis(controller, showTab) :
        new AnalysisTab(controller, showTab, 1, null,
            controller.getUser(), controller.getUserManager().getUserID()
        );

    divWidget.add(w);
    currentSection = PROGRESS;
  }

  private void clear() {
    divWidget.clear();
  }

  @NotNull
  private ShowTab getShowTab() {
    return exid -> {
      learnHelper.loadExercise(exid);
      banner.showLearn();
    };
  }

  @Override
  public void showLearnList(int listid) {
    setHistoryWithList(listid);
    banner.showLearn();
  //  learnHelper.showList(listid);
  }

  private void setHistoryWithList(int listid) {
    History.newItem(FacetExerciseList.LISTS +"=" + listid);
  }

  @Override
  public void showDrillList(int listid) {
    setHistoryWithList(listid);
    banner.showDrill();
   // practiceHelper.showList(listid);
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
    //  banner.setVisibleChoices(false);
  }
}