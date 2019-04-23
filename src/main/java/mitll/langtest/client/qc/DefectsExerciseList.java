/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

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
 // private final Logger logger = Logger.getLogger("DefectsExerciseList");
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
