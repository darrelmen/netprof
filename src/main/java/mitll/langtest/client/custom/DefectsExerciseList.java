package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.recording.NoListFacetExerciseList;
import mitll.langtest.client.dialog.ExceptionHandlerDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.HistoryExerciseList;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.shared.exercise.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

class DefectsExerciseList<T extends CommonShell & ScoredExercise> extends NoListFacetExerciseList<T> {
  private final Logger logger = Logger.getLogger("DefectsExerciseList");

  //  private MarkDefectsChapterNPFHelper markDefectsChapterNPFHelper;
  //Logger logger = Logger.getLogger("NPExerciseList_Defects");
  // private CheckBox filterOnly, uninspectedOnly;
  private boolean isContext;

  DefectsExerciseList(ExerciseController controller,
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
   * @param onlyWithAudioAnno
   * @param onlyDefaultUser
   * @param onlyUninspected
   * @return
   * @see HistoryExerciseList#loadExercisesUsingPrefix
   */
  @Override
  protected ExerciseListRequest getExerciseListRequest(Map<String, Collection<String>> typeToSection,
                                                       String prefix,
                                                       boolean onlyWithAudioAnno,
                                                       boolean onlyDefaultUser,
                                                       boolean onlyUninspected) {

    ExerciseListRequest exerciseListRequest = super
        .getExerciseListRequest(typeToSection, prefix, onlyWithAudioAnno, onlyDefaultUser, onlyUninspected)
        .setOnlyUninspected(true)
        .setQC(true)
        .setAddContext(isContext);

    logger.info("getExerciseListRequest req " + exerciseListRequest);

    return exerciseListRequest;
  }

  protected ExerciseListRequest getExerciseListRequest(String prefix) {
    ExerciseListRequest exerciseListRequest =
        super.getExerciseListRequest(prefix)
            .setOnlyUninspected(true)
            .setQC(true)
            .setAddContext(isContext);
    logger.info("getExerciseListRequest prefix req " + exerciseListRequest);

    String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception("getExerciseListRequest prefix "));
    logger.info("logException stack " + exceptionAsString);

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
        .setOnlyUninspected(true)
        .setExampleRequest(isContext);
  }
}
