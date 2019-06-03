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

package mitll.langtest.client.dialog;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.banner.IBanner;
import mitll.langtest.client.banner.NewContentChooser;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.SimpleChapterNPFHelper;
import mitll.langtest.client.custom.content.FlexListLayout;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.shared.dialog.IDialog;

/**
 * Created by go22670 on 4/5/17.
 */
public class DialogViewHelper extends SimpleChapterNPFHelper<IDialog, IDialog> {
  /**
   * @param controller
   * @see NewContentChooser#NewContentChooser(ExerciseController, IBanner)
   */
  public DialogViewHelper(ExerciseController controller) {
    super(controller);
  }

  @Override
  protected FlexListLayout<IDialog, IDialog> getMyListLayout(SimpleChapterNPFHelper<IDialog, IDialog> outer) {
    return new MyFlexListLayout<IDialog, IDialog>(controller, outer) {
      /**
       * @see FlexListLayout#makeNPFExerciseList
       * @param topRow
       * @param currentExercisePanel
       * @param instanceName
       * @param listHeader
       * @param footer
       * @return
       */
      @Override
      protected PagingExerciseList<IDialog, IDialog> makeExerciseList(Panel topRow,
                                                                      Panel currentExercisePanel,
                                                                      INavigation.VIEWS instanceName,
                                                                      DivWidget listHeader,
                                                                      DivWidget footer) {
        return new DialogExerciseList(topRow, currentExercisePanel, instanceName, listHeader, controller);
      }
    };
  }

  protected ExercisePanelFactory<IDialog, IDialog> getFactory(final PagingExerciseList<IDialog, IDialog> exerciseList) {
    return new ExercisePanelFactory<IDialog, IDialog>(controller, exerciseList) {
      @Override
      public Panel getExercisePanel(IDialog e) {
        return null;
      }
    };
  }
}
