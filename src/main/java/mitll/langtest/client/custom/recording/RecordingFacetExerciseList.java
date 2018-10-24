package mitll.langtest.client.custom.recording;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.exercise.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

import static mitll.langtest.client.custom.content.NPFHelper.COMPLETE;
import static mitll.langtest.client.custom.content.NPFHelper.LIST_COMPLETE;

/**
 * @see RecorderNPFHelper#getMyListLayout
 */
class RecordingFacetExerciseList<T extends CommonShell & ScoredExercise> extends NoListFacetExerciseList<T> {
  private final Logger logger = Logger.getLogger("RecordingFacetExerciseList");

 // private static final String RECORD = "Record";
  private static final String UNRECORD = "Unrecord";
  private static final String RECORDED = "Recorded";

  private final boolean isContext;

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
        instanceName);
    this.isContext = isContext;
  }

  @NotNull
  @Override
  protected String getNoneDoneMessage() {
    return "None Recorded Yet";
  }

  @Override
  @NotNull
  protected String getAllDoneMessage() {
    return "All Recorded.";
  }

  @Override
  protected void onLastItem() {
    new ModalInfoDialog(COMPLETE, LIST_COMPLETE, hiddenEvent -> showEmptySelection());
  }

  @Override protected String getEmptySearchMessage() {
    return "<b>You've completed recording for this selection.</b>" +
        "<p>Please clear one of your selections and select a different unit or chapter.</p>";
  }

  @Override
  protected ExerciseListRequest getExerciseListRequest(Map<String, Collection<String>> typeToSection,
                                                       String prefix,
                                                       boolean onlyWithAudioAnno,
                                                       boolean onlyUninspected) {
    ExerciseListRequest exerciseListRequest = super.getExerciseListRequest(typeToSection, prefix, onlyWithAudioAnno, onlyUninspected);
    //  exerciseListRequest.setOnlyRecordedByMatchingGender(true);
    exerciseListRequest.setOnlyUnrecordedByMe(true);

    logger.info("getExerciseListRequest req " + exerciseListRequest);

//    String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception("getExerciseListRequest"));
//    logger.info("logException stack " + exceptionAsString);

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
      //   request.setOnlyRecordedByMatchingGender(s.startsWith(RECORD));
      logger.warning("getExerciseListRequest selection is " + s);

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
