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

package mitll.langtest.client.list;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.ExerciseListRequest;
import mitll.langtest.shared.exercise.ScoredExercise;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Only the exercises you need to study to practice a dialog.
 *
 * @param <T>
 */
public class StudyExerciseList<T extends CommonShell & ScoredExercise> extends ClientExerciseFacetExerciseList<T> {
  private Logger logger = Logger.getLogger("StudyExerciseList");

  private static final int TWO = 2;
  private static final int THREE = 3;
  private static final int FIVE = 5;
  private static final List<Integer> STUDY_CHOICES = Arrays.asList(1, TWO, THREE, FIVE, 10, 25);
  //private static final boolean DEBUG = false;

  /**
   *  @param secondRow
   * @param currentExerciseVPanel
   * @param controller
   * @param listOptions
   * @param listHeader
   */
  public StudyExerciseList(Panel secondRow,
                           Panel currentExerciseVPanel,
                           ExerciseController controller,
                           ListOptions listOptions,
                           DivWidget listHeader) {
    super(secondRow, currentExerciseVPanel, controller, listOptions, listHeader, INavigation.VIEWS.STUDY);
  }

  @Override
  protected List<Integer> getPageSizeChoiceValues() {
    if (logger == null) logger = Logger.getLogger("StudyExerciseList");
    return STUDY_CHOICES;
  }

  @Override
  protected int getFirstPageSize() {
    int clientHeight = Window.getClientHeight();
    return clientHeight < 650 ? TWO : clientHeight < 800 ? THREE : FIVE;
  }

  @Override
  protected ExerciseListRequest getExerciseListRequest(String prefix) {
    return super.getExerciseListRequest(prefix)
        .setDialogID(getDialogFromURL())
        .setOnlyFL(true);
  }

  private int getDialogFromURL() {
    int dialog = new SelectionState().getDialog();
    if (dialog == -1) dialog=-2;
    return dialog;
  }

  protected void goGetNextPage() {
  }
}
