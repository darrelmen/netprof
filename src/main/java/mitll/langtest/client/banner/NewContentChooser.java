package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
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
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.initial.InitialUI;
import mitll.langtest.client.list.FacetExerciseList;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
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
  private final ExerciseListContent practiceHelper;
  private final ExerciseController controller;
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
    this.listView = new ListView(controller);
    this.banner = banner;
  }

  @Override
  public void showInitialState() {
    clearCurrent();
    showView(getCurrentView());
  }

  /**
   * So it could be that you've lost a permssion, so the view you were on before isn't allowed for you any more.
   * In which case we have to drop down to an allowed view.
   * @return
   * @see NewBanner#checkProjectSelected
   * @see #showInitialState
   */
  @Override
  @NotNull
  public VIEWS getCurrentView() {
    String currentView = getCurrentStoredView();
    VIEWS currentStoredView = (currentView.isEmpty()) ? LEARN : VIEWS.valueOf(currentView);

    List<User.Permission> requiredPerms = currentStoredView.getPerms();

    Set<User.Permission> userPerms = new HashSet<>(controller.getPermissions());

//    logger.info("user userPerms " + userPerms + " vs current view perms " + currentStoredView.getPerms());
    userPerms.retainAll(requiredPerms);

//    logger.info("user userPerms " + userPerms + " overlap =  " + userPerms);

    if (userPerms.isEmpty() && !requiredPerms.isEmpty()) { // if no overlap, you don't have permission
      logger.info("getCurrentView : user userPerms " + userPerms + " falling back to learn view");
      currentStoredView = LEARN;
    }

    return currentStoredView;
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

  private void getReviewList() {
    controller.getListService().getReviewList(new AsyncCallback<UserList<CommonShell>>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("getting defect list", caught);
      }

      @Override
      public void onSuccess(UserList<CommonShell> result) {
        List<CommonShell> exercises = result.getExercises();
        logger.info("got back " + result.getNumItems() + " exercises");
        CommonShell toSelect = exercises.isEmpty() ? null : exercises.get(0);
        Panel review = new ReviewItemHelper(controller)
            .doNPF(result, "review", true, toSelect);
        divWidget.add(review);
      }
    });
  }

  private String getCurrentStoredView() {
    return controller.getStorage().getValue(CURRENT_VIEW);
  }

  private void storeValue(VIEWS view) {
    controller.getStorage().storeValue(CURRENT_VIEW, view.name());
  }

  public void showProgress() {
    ShowTab showTab = getShowTab();

    divWidget.add(controller.getUserManager().hasPermission(User.Permission.TEACHER_PERM) ?
        new StudentAnalysis(controller, showTab) :
        new AnalysisTab(controller, showTab));

    currentSection = PROGRESS;
  }

  private void clear() {
    divWidget.clear();
  }

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
  }

  @Override
  public void showLearnList(int listid) {
    setHistoryWithList(listid);
    banner.showLearn();
  }

  @Override
  public void showDrillList(int listid) {
    setHistoryWithList(listid);
    banner.showDrill();
  }

  private void setHistoryWithList(int listid) {
    History.newItem(FacetExerciseList.LISTS + "=" + listid);
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
    showView(getCurrentView());
  }

  @Override
  public void clearCurrent() {   currentSection = NONE;  }
}