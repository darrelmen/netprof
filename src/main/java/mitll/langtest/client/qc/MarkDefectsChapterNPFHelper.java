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
import mitll.langtest.client.custom.content.FlexListLayout;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.ScoredExercise;

import static mitll.langtest.client.custom.INavigation.VIEWS.QC_ENTRIES;
import static mitll.langtest.client.custom.INavigation.VIEWS.QC_SENTENCES;

public class MarkDefectsChapterNPFHelper<T extends CommonShell & ScoredExercise> extends SimpleChapterNPFHelper<T, ClientExercise> {
//  private final Logger logger = Logger.getLogger("MarkDefectsChapterNPFHelper");
  private final boolean forSentences;

  /**
   * @param controller
   * @param forSentences
   * @see mitll.langtest.client.banner.NewContentChooser#showView(INavigation.VIEWS, boolean, boolean)
   * @see
   */
  public MarkDefectsChapterNPFHelper(ExerciseController controller, boolean forSentences) {
    super(controller);
    this.forSentences = forSentences;
  }

  /**
   * Adds two checkboxes to filter for uninspected items and items with audio that's old enough it's not marked
   * by gender.
   *
   * @param outer
   * @return
   */
  @Override
  protected FlexListLayout<T, ClientExercise> getMyListLayout(SimpleChapterNPFHelper<T, ClientExercise> outer) {
    return new MyFlexListLayout<T, ClientExercise>(controller, outer) {
      /**
       *
       * @param bottomRow
       */
      protected void styleBottomRow(Panel bottomRow) {
        bottomRow.addStyleName("centerPractice");
      }

      @Override
      protected PagingExerciseList<T, ClientExercise> makeExerciseList(Panel topRow,
                                                                       Panel currentExercisePanel,
                                                                       INavigation.VIEWS instanceName,
                                                                       DivWidget listHeader,
                                                                       DivWidget footer) {
        return new DefectsExerciseList<T>(controller,
            topRow, currentExercisePanel, instanceName, listHeader, instanceName == INavigation.VIEWS.QC_SENTENCES);
      }
    };
  }

  protected ExercisePanelFactory<T, ClientExercise> getFactory(PagingExerciseList<T, ClientExercise> exerciseList) {
    final PagingExerciseList<T, ClientExercise> outerExerciseList = exerciseList;
    return new ExercisePanelFactory<T, ClientExercise>(controller, exerciseList) {
      @Override
      public Panel getExercisePanel(ClientExercise e) {
        return new QCNPFExercise<>(e, controller, outerExerciseList, forSentences ? QC_SENTENCES : QC_ENTRIES);
      }
    };
  }
}
