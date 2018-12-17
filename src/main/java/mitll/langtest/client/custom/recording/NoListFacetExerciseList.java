package mitll.langtest.client.custom.recording;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ClientExerciseFacetExerciseList;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.shared.answer.ActivityType;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.ScoredExercise;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Only show one item at a time, no lists.
 *
 * @param <T>
 */
public class NoListFacetExerciseList<T extends CommonShell & ScoredExercise>
    extends ClientExerciseFacetExerciseList<T> {

  private static final String ALL_INSPECTED_OR_VISITED = "All inspected or visited";
  private static final String NONE_INSPECTED = "None Inspected";

  public NoListFacetExerciseList(ExerciseController controller,
                                 Panel topRow,
                                 Panel currentExercisePanel,
                                 INavigation.VIEWS instanceName,
                                 DivWidget listHeader,
                                 INavigation.VIEWS views) {
    super(
        topRow,
        currentExercisePanel,
        controller,

        new ListOptions(instanceName)
            .setShowFirstNotCompleted(true)
            .setActivityType(ActivityType.RECORDER)
        ,
        listHeader,
        views);
  }

  /**
   * @param exerciseID
   * @param searchIfAny
   */
  @Override
  protected void pushFirstSelection(int exerciseID, String searchIfAny) {
    askServerForExercise(-1);
  }

  /**
   * @param visibleIDs
   * @param alreadyFetched
   * @return
   * @see #reallyGetExercises
   */
  @NotNull
  @Override
  protected Set<Integer> getRequested(Collection<Integer> visibleIDs, List<ClientExercise> alreadyFetched) {
    return new HashSet<>(visibleIDs);
  }

  @Override
  protected void goGetNextAndCacheIt(int itemID) {
  }

  @Override
  protected void hidePrevNext() {
    setProgressVisible(true);
  }

  @Override
  protected void setProgressVisible(boolean visible) {
    practicedProgress.setVisible(true);
    scoreProgress.setVisible(visible);
  }

  @Override
  protected Collection<Integer> getVisibleForDrill(int itemID, Collection<Integer> visibleIDs) {
    if (itemID > 0) {
      visibleIDs = new ArrayList<>();
      visibleIDs.add(itemID);
    }
    return visibleIDs;
  }

  @Override
  protected void showExercises(final Collection<ClientExercise> result, final int reqID) {
    hidePrevNextWidgets();
    showOnlyOneExercise(result);
    goGetNextPage();
    setProgressVisible(false);
  }

  private void showOnlyOneExercise(Collection<ClientExercise> result) {
    ClientExercise next = result.iterator().next();
    markCurrentExercise(next.getID());
    addExerciseWidget(next);
    // logger.info("showOnlyOneExercise result size " + result.size());
    int num = getIndex(getCurrentExercise().getID());
    // showNumberPracticed(num, pagingContainer.getSize());
    showProgress(num, pagingContainer.getSize(), practicedProgress,
        getNoneDoneMessage(), getAllDoneMessage(), "", false);
  }

  @NotNull
  protected String getAllDoneMessage() {
    return ALL_INSPECTED_OR_VISITED;
  }

  @NotNull
  protected String getNoneDoneMessage() {
    return NONE_INSPECTED;
  }

  protected String getPracticedText(int num, int denom, String zeroPercent, String oneHundredPercent, String suffix) {
    boolean allDone = num == denom;
    return num == 0 ? getPracticedText(num, denom, suffix) :
        allDone ? oneHundredPercent : getPracticedText(num, denom, suffix);
  }

  @NotNull
  protected String getPracticedText(int num, int denom, String suffix) {
    return " " + num + "/" + denom;
  }
}
