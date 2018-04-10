package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.content.NPFlexSectionExerciseList;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.client.list.SelectionState;

import java.util.Collection;
import java.util.Map;

class PracticeFacetExerciseList extends NPFlexSectionExerciseList {
  //private final Logger logger = Logger.getLogger("PracticeFacetExerciseList");

  private final PracticeHelper practiceHelper;

  PracticeFacetExerciseList(ExerciseController controller,
                            PracticeHelper practiceHelper,
                            Panel topRow, Panel currentExercisePanel, String instanceName, DivWidget listHeader) {
    super(controller, topRow, currentExercisePanel, new ListOptions(instanceName)
        .setShowPager(false).
            setShowTypeAhead(false), listHeader, true);
    this.practiceHelper = practiceHelper;
  }

  protected void goToFirst(String searchIfAny, int exerciseID) {
    super.goToFirst(searchIfAny, exerciseID);
    if (practiceHelper.getPolyglotFlashcardFactory() != null) {
      practiceHelper.getPolyglotFlashcardFactory().setMode(practiceHelper.getMode(), practiceHelper.getPromptChoice());
    }
    practiceHelper.getStatsFlashcardFactory().setNavigation(practiceHelper.getNavigation());
  }

  protected void gotVisibleRangeChanged(Collection<Integer> idsForRange, int currrentReq) {
  }

  @Override
  protected void onLastItem() {
    practiceHelper.getStatsFlashcardFactory().resetStorage();
  }

  /**
   * The issue is there should only be only keyboard focus - either the space bar and prev/next or
   * the search box. - so we should hide the search box.
   *
   * @param typeToSection
   * @param prefix
   * @param exerciseID
   * @param onlyWithAudioAnno
   * @param onlyUnrecorded
   * @param onlyDefaultUser
   * @param onlyUninspected
   */
  @Override
  protected void loadExercisesUsingPrefix(Map<String, Collection<String>> typeToSection,
                                          String prefix,
                                          int exerciseID, boolean onlyWithAudioAnno,
                                          boolean onlyUnrecorded, boolean onlyDefaultUser, boolean onlyUninspected) {
   //   logger.info("getMyListLayout : got loadExercisesUsingPrefix " +prefix + " WERE NOT USING PREFIX");
    super.loadExercisesUsingPrefix(typeToSection, "", exerciseID, onlyWithAudioAnno, onlyUnrecorded, onlyDefaultUser, onlyUninspected);
    practiceHelper.getStatsFlashcardFactory().setSelection(typeToSection);
  }

  void restoreUI(SelectionState selectionState) {
    restoreUIState(selectionState);
  }
}
