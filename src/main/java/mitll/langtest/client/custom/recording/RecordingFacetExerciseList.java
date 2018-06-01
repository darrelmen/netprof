package mitll.langtest.client.custom.recording;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.ListItem;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.client.list.FacetExerciseList;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.shared.answer.ActivityType;
import mitll.langtest.shared.exercise.MatchInfo;

import java.util.Map;
import java.util.Set;

class RecordingFacetExerciseList extends FacetExerciseList {
  //private final Logger logger = Logger.getLogger("PracticeFacetExerciseList");
  private final RecorderNPFHelper helper;

  RecordingFacetExerciseList(ExerciseController controller,
                             RecorderNPFHelper practiceHelper,
                             Panel topRow, Panel currentExercisePanel, String instanceName, DivWidget listHeader) {
    super(
        topRow,
        currentExercisePanel,
        controller,
        new ListOptions(instanceName)
            .setInstance(instanceName)
            .setShowFirstNotCompleted(true)
            .setActivityType(ActivityType.RECORDER)
        //    .setShowPager(false).
          //  setShowTypeAhead(false)
        , listHeader, true);
    this.helper = practiceHelper;
  }

//  @Override
//  protected void addTableWithPager(SimplePagingContainer<?> pagingContainer) {
//    addEventHandler(instanceName);
//  }
  @Override
  protected ListItem addListFacet(Map<String, Set<MatchInfo>> typeToValues) {
    return null;
  }
/*  protected void goToFirst(String searchIfAny, int exerciseID) {
    super.goToFirst(searchIfAny, exerciseID);
    if (practiceHelper.getPolyglotFlashcardFactory() != null) {
      practiceHelper.getPolyglotFlashcardFactory().setMode(practiceHelper.getMode());
    }
    getStatsFlashcardFactory().setNavigation(practiceHelper.getNavigation());
  }*/

/*  @Override
  protected void onLastItem() {
    getStatsFlashcardFactory().resetStorage();
  }*/

  /**
   * The issue is there should only be only keyboard focus - either the space bar and prev/next or
   * the search box. - so we should hide the search box.
   *
   * @paramx typeToSection
   * @paramx prefix
   * @paramx exerciseID
   * @paramx onlyWithAudioAnno
   * @paramx onlyUnrecorded
   * @paramx onlyDefaultUser
   * @paramx onlyUninspected
   */
/*  @Override
  protected void loadExercisesUsingPrefix(Map<String, Collection<String>> typeToSection,
                                          String prefix,
                                          int exerciseID, boolean onlyWithAudioAnno,
                                          boolean onlyUnrecorded, boolean onlyDefaultUser, boolean onlyUninspected) {
    super.loadExercisesUsingPrefix(typeToSection, "", exerciseID, onlyWithAudioAnno,
        onlyUnrecorded, onlyDefaultUser, onlyUninspected);
    getStatsFlashcardFactory().setSelection(typeToSection);
  }*/

  void restoreUI(SelectionState selectionState) {
    restoreUIState(selectionState);
  }
}
