package mitll.langtest.client.custom.recording;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.ListItem;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.LearnFacetExerciseList;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.shared.answer.ActivityType;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.MatchInfo;
import mitll.langtest.shared.exercise.ScoredExercise;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class NoListFacetExerciseList<T extends CommonShell & ScoredExercise> extends LearnFacetExerciseList<T> {
  NoListFacetExerciseList(ExerciseController controller,
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
        , listHeader, true, views);
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
   * @param type
   * @param key
   * @param newUserListID
   * @return
   * @see #getChoiceHandler
   */
  @Override
  protected String getChoiceHandlerValue(String type, String key, int newUserListID) {
    return key;
  }

  /**
   * No list facet or special facet.
   *
   * @param typeToValues
   * @return
   * @see #addFacetsForReal
   */
  @Override
  protected ListItem addListFacet(Map<String, Set<MatchInfo>> typeToValues) {
    return null;
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

}
