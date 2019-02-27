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
import mitll.langtest.client.custom.SimpleChapterNPFHelper;
import mitll.langtest.client.custom.recording.NoListFacetExerciseList;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.HistoryExerciseList;
import mitll.langtest.shared.exercise.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A list for items to fix (review).
 *
 * @param <T>
 */
class FixExerciseList<T extends CommonShell & ScoredExercise> extends NoListFacetExerciseList<T> {
  // private final Logger logger = Logger.getLogger("FixExerciseList");
  private final boolean isContext;

  /**
   * @param controller
   * @param topRow
   * @param currentExercisePanel
   * @param instanceName
   * @param listHeader
   * @param isContext
   * @see FixNPFHelper#getMyListLayout(SimpleChapterNPFHelper)
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

  @Override
  protected String getEmptySearchMessage() {
    return "<b>You've completed fixing defects for this selection.</b>" +
        "<p>Please clear one of your selections and select a different unit or chapter.</p>";
  }
}