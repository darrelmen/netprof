package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.flashcard.ControlState;
import mitll.langtest.client.flashcard.StatsFlashcardFactory;
import mitll.langtest.client.list.LearnFacetExerciseList;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.ScoredExercise;

import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

public class PracticeFacetExerciseList<T extends CommonShell & ScoredExercise, U extends ClientExercise>
    extends LearnFacetExerciseList<T> {
   private final Logger logger = Logger.getLogger("PracticeFacetExerciseList");
  private final PracticeHelper<T, U> practiceHelper;
  private ControlState controlState;

  PracticeFacetExerciseList(ExerciseController controller,
                            PracticeHelper<T, U> practiceHelper,
                            Panel topRow, Panel currentExercisePanel,
                            INavigation.VIEWS instanceName,
                            DivWidget listHeader,
                            INavigation.VIEWS views) {
    super(
        topRow,
        currentExercisePanel,
        controller,
        new ListOptions(instanceName)
            .setShowPager(false).
            setShowTypeAhead(false),
        listHeader,
        true,
        views);
    this.practiceHelper = practiceHelper;
  }

  public void setControlState(ControlState state) {
    this.controlState = state;
  }

  /**
   * Turn off auto advance if not visible any more.
   * Thanks Reina!
   */
  @Override
  protected void onDetach() {
    super.onDetach();
// logger.info("\n\ngot detach ---> \n\n\n");

    if (controlState.isAutoPlay()) {
      // logger.info("onDetach : audio on, so turn auto advance OFF");
      controlState.setAutoPlay(false);
    }
  }

  protected void goToFirst(String searchIfAny, int exerciseID) {
    //logger.info("\ngoToFirst ---> \n\n\n");

    super.goToFirst(searchIfAny, exerciseID);
    if (practiceHelper.getPolyglotFlashcardFactory() != null) {
      practiceHelper.getPolyglotFlashcardFactory().setMode(practiceHelper.getMode());
    }
    getStatsFlashcardFactory().setNavigation(practiceHelper.getNavigation());
  }

  @Override
  protected void onLastItem() {
    getStatsFlashcardFactory().resetStorage();
  }

  /**
   * The issue is there should only be only keyboard focus - either the space bar and prev/next or
   * the search box. - so we should hide the search box.
   *
   * @param typeToSection
   * @param prefix
   * @param exerciseID
   * @param onlyWithAudioAnno
   * @param onlyDefaultUser
   * @param onlyUninspected
   */
  @Override
  protected void loadExercisesUsingPrefix(Map<String, Collection<String>> typeToSection,
                                          String prefix,
                                          int exerciseID, boolean onlyWithAudioAnno,
                                          boolean onlyDefaultUser, boolean onlyUninspected) {
    super.loadExercisesUsingPrefix(typeToSection, "", exerciseID, onlyWithAudioAnno, onlyDefaultUser, onlyUninspected);
    getStatsFlashcardFactory().setSelection(typeToSection);
  }

  private StatsFlashcardFactory<T, U> getStatsFlashcardFactory() {
    return practiceHelper.getStatsFlashcardFactory();
  }

  /**
   * No one call this...
   * @param selectionState
   */
  @Deprecated
  void restoreUI(SelectionState selectionState) {
    restoreUIState(selectionState);
  }
}
