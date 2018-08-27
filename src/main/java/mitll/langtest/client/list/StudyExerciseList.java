package mitll.langtest.client.list;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Full exercises returns CommonExercise - so we're kinda stuck with it...
 *
 * @param <T>
 */
public class StudyExerciseList<T extends CommonShell & ScoredExercise> extends LearnFacetExerciseList<T> {
  //private final Logger logger = Logger.getLogger("FacetExerciseList");
  //private static final boolean DEBUG = false;

  public StudyExerciseList(Panel secondRow,
                           Panel currentExerciseVPanel,
                           ExerciseController controller,
                           ListOptions listOptions,
                           DivWidget listHeader,
                           boolean isDrillView) {
    super(secondRow, currentExerciseVPanel, controller, listOptions, listHeader, isDrillView);
  }

  @Override
  protected ExerciseListRequest getExerciseListRequest(String prefix) {
    return super.getExerciseListRequest(prefix)
        .setDialogID(getDialogFromURL());
  }

  private int getDialogFromURL() {
    int dialog = new SelectionState().getDialog();
    return dialog == -1 ? 0 : dialog;
  }

  protected void goGetNextPage() {
  }

/*  @Override
  protected void getTypeToValues(Map<String, String> typeToSelection, int userListID) {
    super.getTypeToValues(typeToSelection, userListID);
  }*/
}
