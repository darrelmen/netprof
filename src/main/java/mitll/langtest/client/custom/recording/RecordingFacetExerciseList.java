package mitll.langtest.client.custom.recording;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.ListItem;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.LearnFacetExerciseList;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.shared.answer.ActivityType;
import mitll.langtest.shared.exercise.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @see RecorderNPFHelper#getMyListLayout
 */
class RecordingFacetExerciseList<T extends CommonShell & ScoredExercise> extends LearnFacetExerciseList<T> {
  //private final Logger logger = Logger.getLogger("RecordingFacetExerciseList");

  private static final String RECORD = "Record";
  private static final String UNRECORD = "Unrecord";

  private static final String RECORDED = "Recorded";
  private boolean isContext;

  /**
   * @param controller
   * @param topRow
   * @param currentExercisePanel
   * @param instanceName
   * @param listHeader
   * @param isContext
   */
  RecordingFacetExerciseList(ExerciseController controller,
                             Panel topRow,
                             Panel currentExercisePanel,
                             INavigation.VIEWS instanceName,
                             DivWidget listHeader,
                             boolean isContext) {
    super(
        topRow,
        currentExercisePanel,
        controller,
        new ListOptions(instanceName)
            .setInstance(instanceName)
            .setShowFirstNotCompleted(true)
            .setActivityType(ActivityType.RECORDER)
        , listHeader, true);
    this.isContext = isContext;
  }

  @Override
  protected ExerciseListRequest getExerciseListRequest(Map<String, Collection<String>> typeToSection, String prefix, boolean onlyWithAudioAnno, boolean onlyDefaultUser, boolean onlyUninspected) {
    ExerciseListRequest exerciseListRequest = super.getExerciseListRequest(typeToSection, prefix, onlyWithAudioAnno, onlyDefaultUser, onlyUninspected);
    exerciseListRequest.setOnlyRecordedByMatchingGender(true);
    exerciseListRequest.setOnlyUnrecordedByMe(true);
    return exerciseListRequest;
  }

  @NotNull
  @Override
  protected FilterRequest getFilterRequest(int userListID, List<Pair> pairs) {
    return new FilterRequest(incrReqID(), pairs, userListID)
        .setRecordRequest(true)
        .setExampleRequest(isContext);
  }

  @NotNull
  @Override
  protected String getDynamicFacet() {
    return RECORDED;
  }

  protected boolean isDynamicFacetInteger() {
    return false;
  }

  @Override
  protected void pushFirstSelection(int exerciseID, String searchIfAny) {
    askServerForExercise(-1);
  }

  /**
   * @param prefix
   * @return
   */
  @Override
  protected ExerciseListRequest getExerciseListRequest(String prefix) {
    ExerciseListRequest request = super.getExerciseListRequest(prefix);
    if (getTypeToSelection().containsKey(RECORDED)) {
      String s = getTypeToSelection().get(RECORDED);
      request.setOnlyUnrecordedByMe(s.startsWith(UNRECORD));
      request.setOnlyRecordedByMatchingGender(s.startsWith(RECORD));

    } else {
      //logger.info("getExerciseListRequest no recorded selection in " + getTypeToSelection().keySet());
    }
    //   logger.info("getExerciseListRequest req     " + request);
    request.setOnlyExamples(isContext);
    return request;
  }

  @Override
  protected String getChoiceHandlerValue(String type, String key, int newUserListID) {
    return key;
  }

  /**
   * No list facet or special facet.
   * @param typeToValues
   * @return
   */
  @Override
  protected ListItem addListFacet(Map<String, Set<MatchInfo>> typeToValues) {
    return null;
  }

  @NotNull
  @Override
  protected Set<Integer> getRequested(Collection<Integer> visibleIDs, List<ClientExercise> alreadyFetched) {
    return new HashSet<>(visibleIDs);
  }

  @Override
  protected void goGetNextAndCacheIt(int itemID) {
  }

  void restoreUI(SelectionState selectionState) {
    restoreUIState(selectionState);
  }
}
