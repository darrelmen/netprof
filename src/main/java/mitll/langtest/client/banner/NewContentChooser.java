package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.ExerciseListContent;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;

/**
 * Created by go22670 on 4/10/17.
 */
public class NewContentChooser implements INavigation {
  private static final String LEARN = "learn";

  private static final String STUDENT_ANALYSIS = "Student Analysis";
  public static final String CLASSROOM = "classroom";

  private static final String CHAPTERS = "Learn Pronunciation";

  //private static final String CLASSES = "Classes";
  private static final String PROJECTS = "Projects";

  private static final String YOUR_LISTS = "Study Your Lists";
  private static final String STUDY_LISTS = "Study Lists";
  private static final String OTHERS_LISTS = "Study Visited Lists";
  private static final String PRACTICE = "Audio Vocabulary Practice";

  private DivWidget divWidget = new DivWidget();
  private ExerciseListContent learnHelper;
  private ExerciseListContent practiceHelper;

  public NewContentChooser(ExerciseController controller) {
    learnHelper = new NewLearnHelper(controller);
    practiceHelper = new PracticeHelper(controller);
  }

  @Override
  public void showInitialState() {
    showLearn();
  }

  @Override
  public void showLearn() {
    divWidget.clear();
    learnHelper.showContent(divWidget, LEARN);
  }

  @Override
  public void showDrill() {
    divWidget.clear();
    practiceHelper.showContent(divWidget, PRACTICE);
    practiceHelper.hideList();
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
