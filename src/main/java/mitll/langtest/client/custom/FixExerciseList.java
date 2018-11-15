package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.recording.NoListFacetExerciseList;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.HistoryExerciseList;
import mitll.langtest.shared.exercise.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static mitll.langtest.client.custom.content.NPFHelper.COMPLETE;
import static mitll.langtest.client.custom.content.NPFHelper.LIST_COMPLETE;

/**
 * A list for items to fix (review).
 * @param <T>
 */
class FixExerciseList<T extends CommonShell & ScoredExercise> extends NoListFacetExerciseList<T> {
  private final Logger logger = Logger.getLogger("FixExerciseList");
  private final boolean isContext;

  /**
   * @see FixNPFHelper#getMyListLayout(SimpleChapterNPFHelper)
   * @param controller
   * @param topRow
   * @param currentExercisePanel
   * @param instanceName
   * @param listHeader
   * @param isContext
   */
  FixExerciseList(ExerciseController controller,
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

  /**
   * @param typeToSection
   * @param prefix
   * @param onlyUninspected
   * @return
   * @see HistoryExerciseList#loadExercisesUsingPrefix
   */
  @Override
  protected ExerciseListRequest getExerciseListRequest(Map<String, Collection<String>> typeToSection,
                                                       String prefix,
                                                       boolean onlyUninspected) {

    ExerciseListRequest exerciseListRequest = super
        .getExerciseListRequest(typeToSection, prefix, onlyUninspected)
        .setOnlyWithAnno(true)
        .setQC(true)
        .setAddContext(isContext);
   // logger.info("getExerciseListRequest req " + exerciseListRequest);

    return exerciseListRequest;
  }

  protected ExerciseListRequest getExerciseListRequest(String prefix) {
    ExerciseListRequest exerciseListRequest =
        super.getExerciseListRequest(prefix)
            .setOnlyWithAnno(true)
            .setQC(true)
            .setAddContext(isContext);
   // logger.info("getExerciseListRequest prefix req " + exerciseListRequest);
//    String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception("getExerciseListRequest prefix "));
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
        .setOnlyWithAnno(true)
        .setExampleRequest(isContext);
  }

  @Override
  protected void onLastItem() {
    new ModalInfoDialog(COMPLETE, LIST_COMPLETE, hiddenEvent -> showEmptySelection());
  }

  @Override protected String getEmptySearchMessage() {
    return "<b>You've completed fixing defects for this selection.</b>" +
        "<p>Please clear one of your selections and select a different unit or chapter.</p>";
  }
}