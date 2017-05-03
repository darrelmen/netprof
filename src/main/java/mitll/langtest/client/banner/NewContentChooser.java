package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.analysis.AnalysisTab;
import mitll.langtest.client.analysis.ShowTab;
import mitll.langtest.client.analysis.StudentAnalysis;
import mitll.langtest.client.custom.ExerciseListContent;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.MarkDefectsChapterNPFHelper;
import mitll.langtest.client.custom.SimpleChapterNPFHelper;
import mitll.langtest.client.custom.userlist.ListManager;
import mitll.langtest.client.custom.recording.RecorderNPFHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.user.User;
import org.jetbrains.annotations.NotNull;
import scala.tools.nsc.backend.icode.Primitives;

import java.util.logging.Logger;

import static mitll.langtest.client.custom.INavigation.VIEWS.*;

/**
 * Created by go22670 on 4/10/17.
 */
public class NewContentChooser implements INavigation {
  private final Logger logger = Logger.getLogger("NewContentChooser");

//  public static final String LISTS = "Lists";
//  public static final String PROGRESS = "Progress";
//  public static final String LEARN = "Learn";
//  public static final String DRILL = "Drill";//Audio Vocabulary Practice";
//
//  public static final String RECORD_AUDIO = "Items";
//  public static final String RECORD_EXAMPLE = "Context";
//
//  public static final String MARK_DEFECTS = "Mark Defects";
//  public static final String FIX = "Fix";

  private final DivWidget divWidget = new DivWidget();
  private final ExerciseListContent learnHelper;
  private final ExerciseListContent practiceHelper;
  private final ExerciseController controller;
  private final ListManager listManager;

  private VIEWS currentSection = VIEWS.NONE;

  public NewContentChooser(ExerciseController controller) {
    NewLearnHelper newLearnHelper = new NewLearnHelper(controller);
    learnHelper = newLearnHelper;
    practiceHelper = new PracticeHelper(controller);
    this.controller = controller;
    this.listManager = new ListManager(controller, null, newLearnHelper);
  }

  @Override
  public void showInitialState() {
    clearCurrent();
    showView(LEARN);
    // History.fireCurrentHistoryState();
  }

  private boolean hasProjectChoice() {
    return controller.getProjectStartupInfo() != null;
  }

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

//  @Override
//  public void showLearn() {
//    if (currentSection.equals(LEARN)) return;
//    clear();
//    learnHelper.showContent(divWidget, LEARN);
//    currentSection = LEARN;
//  }
/*
  @Override
  public void showDrill() {
    if (currentSection.equals(DRILL)) return;

    logger.info("showDrill ------ ");
    clear();
    practiceHelper.showContent(divWidget, DRILL);
    practiceHelper.hideList();
    currentSection = DRILL;

  }*/

  //  @Override
  public void showProgress() {
//    if (currentSection.equals(PROGRESS)) return;
//    clear();
    boolean hasTeacher = controller.getUserManager().hasPermission(User.Permission.TEACHER_PERM);

    logger.info("has teacher " + hasTeacher);
    logger.info("controller " + controller);

    ShowTab showTab = getShowTab();
    logger.info("showTab " + showTab);

    DivWidget w = hasTeacher ?
        new StudentAnalysis(controller, showTab) :
        new AnalysisTab(controller, showTab, 1, null);

    divWidget.add(w);
    currentSection = PROGRESS;
  }

  public void showLists() {
    if (currentSection.equals(LISTS)) return;
    clear();
    divWidget.add(listManager.showLists());
    currentSection = LISTS;
  }

/*  public void showRecord() {
    if (currentSection.equals(RECORD_AUDIO)) {
      logger.info("showRecord  - skip current");
      return;
    }
    clear();

    RecorderNPFHelper recorderNPFHelper = new RecorderNPFHelper(controller, true, null);
    logger.info("showRecord  - recorderNPFHelper");
    recorderNPFHelper.showNPF(divWidget, RECORD_AUDIO);
    currentSection = RECORD_AUDIO;
  }*/

//  public void showRecordExample() {
//    if (currentSection.equals(RECORD_EXAMPLE)) return;
//    clear();
//
//    RecorderNPFHelper recorderNPFHelper = new RecorderNPFHelper(controller, false, null);
//    recorderNPFHelper.showNPF(divWidget, RECORD_EXAMPLE);
//    currentSection = RECORD_EXAMPLE;
//  }


/*  public void showMarkDefects() {
    if (currentSection.equals(MARK_DEFECTS)) return;
    clear();

    //RecorderNPFHelper recorderNPFHelper = new RecorderNPFHelper(controller, false, null);

    MarkDefectsChapterNPFHelper markDefectsHelper = new MarkDefectsChapterNPFHelper(controller, null);
    markDefectsHelper.showNPF(divWidget, MARK_DEFECTS);

    currentSection = MARK_DEFECTS;
  }*/

  private void clear() {
    divWidget.clear();
  }

  @NotNull
  private ShowTab getShowTab() {
    return exid -> {
      showView(LEARN);
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