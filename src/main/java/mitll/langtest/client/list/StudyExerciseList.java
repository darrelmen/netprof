package mitll.langtest.client.list;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.exercise.*;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Only the exercises you need to study to practice a dialog.
 *
 * @param <T>
 */
public class StudyExerciseList<T extends CommonShell & ScoredExercise> extends ClientExerciseFacetExerciseList<T> {
  private Logger logger = Logger.getLogger("StudyExerciseList");

  private static final int TWO = 2;
  private static final int THREE = 3;
  private static final int FIVE = 5;
  private static final List<Integer> STUDY_CHOICES = Arrays.asList(1, TWO, THREE, FIVE, 10, 25);
  //private static final boolean DEBUG = false;

  /**
   *  @param secondRow
   * @param currentExerciseVPanel
   * @param controller
   * @param listOptions
   * @param listHeader
   */
  public StudyExerciseList(Panel secondRow,
                           Panel currentExerciseVPanel,
                           ExerciseController controller,
                           ListOptions listOptions,
                           DivWidget listHeader) {
    super(secondRow, currentExerciseVPanel, controller, listOptions, listHeader, INavigation.VIEWS.STUDY);
  }

  @Override
  protected List<Integer> getPageSizeChoiceValues() {
    if (logger == null) logger = Logger.getLogger("StudyExerciseList");
    return STUDY_CHOICES;
  }

  @Override
  protected int getFirstPageSize() {
    int clientHeight = Window.getClientHeight();
    return clientHeight < 650 ? TWO : clientHeight < 800 ? THREE : FIVE;
  }

  @Override
  protected ExerciseListRequest getExerciseListRequest(String prefix) {
    return super.getExerciseListRequest(prefix)
        .setDialogID(getDialogFromURL());
  }

  private int getDialogFromURL() {
    int dialog = new SelectionState().getDialog();
    if (dialog == -1) dialog=-2;
    return dialog;
  }

  protected void goGetNextPage() {
  }
}
