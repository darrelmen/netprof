/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.amas;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.SimpleChapterNPFHelper;
import mitll.langtest.client.custom.content.FlexListLayout;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.services.ExerciseServiceAsync;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.amas.AmasExerciseImpl;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 1/22/15.
 */
public class AutoCRTChapterNPFHelper extends SimpleChapterNPFHelper {
  //private Logger logger = Logger.getLogger("AutoCRTChapterNPFHelper");
  private QuizScorePanel child;
  private ResponseExerciseList exerciseList;

  /**
   * @param service
   * @param feedback
   * @param userManager
   * @param controller
   * @param exerciseServiceAsync
   * @see AMASInitialUI#populateBelowHeader
   */
  AutoCRTChapterNPFHelper(LangTestDatabaseAsync service, UserFeedback feedback, UserManager userManager,
                          ExerciseController controller, ExerciseServiceAsync exerciseServiceAsync) {
    super(controller, null);
  }

  /**
   * @param exerciseList
   * @return
   * @see FlexListLayout#getFactory
   */
  protected ExercisePanelFactory getFactory(final PagingExerciseList exerciseList) {
    return new ExercisePanelFactory<AmasExerciseImpl, AmasExerciseImpl>(controller, exerciseList) {
      @Override
      public Panel getExercisePanel(AmasExerciseImpl e) {
        if (child != null) {
          child.setVisible(true);
        }
        return new FeedbackRecordPanel(e, controller, (ResponseExerciseList) exerciseList, child);
      }
    };
  }

  @Override
  public void showContent(Panel listContent, String instanceName) {
    super.showContent(listContent, instanceName);
    //if (!controller.getProps().isAdminView()) {
    hideList();
    //}
  }

  /**
   * TODO : parameterize this
   *
   * @param outer
   * @return
   * @see mitll.langtest.client.custom.SimpleChapterNPFHelper#SimpleChapterNPFHelper
   */
  @Override
  protected FlexListLayout getMyListLayout(SimpleChapterNPFHelper outer) {
    return new MyFlexListLayout(controller, outer) {
      @Override
      protected PagingExerciseList makeExerciseList(Panel topRow, Panel currentExercisePanel, String instanceName, DivWidget listHeader, DivWidget footer) {
        exerciseList = new ResponseExerciseList(topRow, currentExercisePanel, controller, instanceName);
        return exerciseList;
      }

      /**
       * @see FlexListLayout#doInternalLayout
       * @return
       */
      @Override
      protected Panel getCurrentExercisePanel() {
        Panel currentExercisePanel = super.getCurrentExercisePanel();
        currentExercisePanel.getElement().getStyle().setPadding(5, Style.Unit.PX);
        return currentExercisePanel;
      }

      /**
       * @see #doInternalLayout
       * @param bottomRow
       */
      @Override
      protected void addThirdColumn(Panel bottomRow) {
        child = new QuizScorePanel();
        child.setVisible(false);
        bottomRow.add(child);
        exerciseList.setQuizPanel(child);
      }
    };
  }
}
