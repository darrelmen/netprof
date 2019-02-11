package mitll.langtest.client.qc;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.recording.RecordingFacetExerciseList;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.HistoryExerciseList;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.project.ProjectStartupInfo;
import mitll.langtest.shared.project.ProjectType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

class DefectsExerciseList<T extends CommonShell & ScoredExercise> extends RecordingFacetExerciseList<T> {
  private final Logger logger = Logger.getLogger("DefectsExerciseList");

  // private final boolean isContext;

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
        isContext);
    //  this.isContext = isContext;
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
        //.setProjectType(controller.getProjectStartupInfo().getProjectType())
        .setMode(controller.getMode())
        .setExampleRequest(isContext);
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

   // ProjectType projectType = getProjectType();
    ExerciseListRequest exerciseListRequest = super
        .getExerciseListRequest(typeToSection, prefix, onlyUninspected)
        .setOnlyUninspected(true)
        .setQC(true)
        .setAddContext(isContext)
        .setOnlyUnrecordedByMe(false)
        //.setProjectType(projectType)
        .setMode(controller.getMode())
    ;

    //  logger.info("getExerciseListRequest req " + exerciseListRequest);

    return exerciseListRequest;
  }

  private ProjectType getProjectType() {
    ProjectStartupInfo projectStartupInfo = controller.getProjectStartupInfo();
    return projectStartupInfo == null ? ProjectType.DEFAULT : projectStartupInfo.getProjectType();
  }

  protected ExerciseListRequest getExerciseListRequest(String prefix) {
    //ProjectType projectType = getProjectType();
    ExerciseListRequest exerciseListRequest =
        super.getExerciseListRequest(prefix)
            .setOnlyUninspected(true)
            .setQC(true)
            .setAddContext(isContext)
            .setMode(controller.getMode());
    //   logger.info("getExerciseListRequest prefix req " + exerciseListRequest);

//    String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception("getExerciseListRequest prefix "));
//    logger.info("logException stack " + exceptionAsString);
    return exerciseListRequest;
  }
}
