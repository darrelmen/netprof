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
import java.util.logging.Logger;

/**
 * @see RecorderNPFHelper#getMyListLayout
 */
class RecordingFacetExerciseList<T extends CommonShell & ScoredExercise> extends NoListFacetExerciseList<T> {
  private final Logger logger = Logger.getLogger("RecordingFacetExerciseList");

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
        controller,
        topRow,
        currentExercisePanel,
        instanceName,
        listHeader,
        isContext ? INavigation.VIEWS.RECORD_CONTEXT : INavigation.VIEWS.RECORD_ENTRIES);
    this.isContext = isContext;
  }

  @Override
  protected ExerciseListRequest getExerciseListRequest(Map<String, Collection<String>> typeToSection, String prefix,
                                                       boolean onlyWithAudioAnno, boolean onlyDefaultUser, boolean onlyUninspected) {
    ExerciseListRequest exerciseListRequest = super.getExerciseListRequest(typeToSection, prefix, onlyWithAudioAnno, onlyDefaultUser, onlyUninspected);
    exerciseListRequest.setOnlyRecordedByMatchingGender(true);
    exerciseListRequest.setOnlyUnrecordedByMe(true);
    return exerciseListRequest;
  }

  /**
   * @param userListID
   * @param pairs
   * @return
   * @see #getTypeToValues
   */
  @NotNull
  @Override
  protected FilterRequest getFilterRequest(int userListID, List<Pair> pairs) {
    return new FilterRequest(incrReqID(), pairs, userListID)
        .setRecordRequest(true)
        .setExampleRequest(isContext);
  }

  /**
   * @return
   * @see #addWidgets()
   */
  @NotNull
  @Override
  protected String getDynamicFacet() {
    return RECORDED;
  }

  protected boolean isDynamicFacetInteger() {
    return false;
  }

  /**
   * @param prefix
   * @return
   * @see #getExercises
   */
  @Override
  protected ExerciseListRequest getExerciseListRequest(String prefix) {
    ExerciseListRequest request = super.getExerciseListRequest(prefix);
    if (getTypeToSelection().containsKey(RECORDED)) {
      String s = getTypeToSelection().get(RECORDED);
      request.setOnlyUnrecordedByMe(s.startsWith(UNRECORD));
      request.setOnlyRecordedByMatchingGender(s.startsWith(RECORD));
      logger.info("getExerciseListRequest selection is " + s);

    } else {
      //logger.info("getExerciseListRequest no recorded selection in " + getTypeToSelection().keySet());
    }
    logger.info("getExerciseListRequest req     " + request);
    request.setOnlyExamples(isContext);
    return request;
  }


//  void restoreUI(SelectionState selectionState) {
//    restoreUIState(selectionState);
//  }
}
