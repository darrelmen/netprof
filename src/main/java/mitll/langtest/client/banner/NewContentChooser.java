package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.analysis.AnalysisTab;
import mitll.langtest.client.analysis.ShowTab;
import mitll.langtest.client.analysis.StudentAnalysis;
import mitll.langtest.client.custom.ExerciseListContent;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.ListManager;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Created by go22670 on 4/10/17.
 */
public class NewContentChooser implements INavigation {
  public static final String LISTS = "Lists";
  public static final String PROGRESS = "Progress";
  private Logger logger = Logger.getLogger("NewContentChooser");

  public static final String LEARN = "Learn";

  //public static final String STUDENT_ANALYSIS = "Student Analysis";
  public static final String CLASSROOM = "classroom";

  //public static final String CHAPTERS = "Learn Pronunciation";

  //private static final String CLASSES = "Classes";
  public static final String PROJECTS = "Projects";

  public static final String YOUR_LISTS = "Study Your Lists";
  private static final String STUDY_LISTS = "Study Lists";
  private static final String OTHERS_LISTS = "Study Visited Lists";
  public static final String DRILL = "Drill";//Audio Vocabulary Practice";

  private DivWidget divWidget = new DivWidget();
  private ExerciseListContent learnHelper;
  private ExerciseListContent practiceHelper;
  private ExerciseController controller;
  ListManager listManager;
  TabPanel tabPanel = new TabPanel();

  String currentSection = "";

  public NewContentChooser(ExerciseController controller) {
    NewLearnHelper newLearnHelper = new NewLearnHelper(controller);
    learnHelper = newLearnHelper;
    practiceHelper = new PracticeHelper(controller);
    this.controller = controller;
    this.listManager = new ListManager(controller, null, newLearnHelper);
  }

  @Override
  public void showInitialState() {
    History.fireCurrentHistoryState();
  }

  @Override
  public void showLearn() {
    if (currentSection.equals(LEARN)) return;
    clear();
    learnHelper.showContent(divWidget, LEARN);
    currentSection = LEARN;
  }

  @Override
  public void showDrill() {
    if (currentSection.equals(DRILL)) return;
    clear();
    practiceHelper.showContent(divWidget, DRILL);
    practiceHelper.hideList();
    currentSection = DRILL;

  }

  @Override
  public void showProgress() {
    if (currentSection.equals(PROGRESS)) return;
    clear();
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

  private void clear() {
    divWidget.clear();
  }

  @NotNull
  private ShowTab getShowTab() {
    return exid -> {
      showLearn();
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
}