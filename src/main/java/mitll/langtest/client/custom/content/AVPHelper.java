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

package mitll.langtest.client.custom.content;

import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.client.flashcard.StatsFlashcardFactory;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.client.list.NPExerciseList;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/8/13
 * Time: 5:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class AVPHelper extends NPFHelper {
  private UserList ul;

  /**
   * @param controller
   * @see mitll.langtest.client.custom.Navigation#Navigation
   */
  public AVPHelper(ExerciseController controller) {  super(controller, false, false);  }

  /**
   * @param ul
   * @param instanceName
   * @return
   * @see #doInternalLayout(mitll.langtest.shared.custom.UserList, String)
   */
  @Override
  protected Panel getRightSideContent(UserList<CommonShell> ul, String instanceName) {
    Panel npfContentPanel = new SimplePanel();
    npfContentPanel.getElement().setId("AVPHelper_internalLayout_RightSideContent");
    this.ul = ul;
    npfExerciseList = makeNPFExerciseList(npfContentPanel, instanceName + "_" + ul.getID(), false);
    return npfContentPanel;
  }

  /**
   * TODO : parameterize exercise list by different SectionWidget
   *
   * @param right
   * @param listOptions
   * @return
   * @see NPFHelper#makeNPFExerciseList(Panel, String, boolean)
   */
  @Override
  PagingExerciseList<CommonShell, CommonExercise> makeExerciseList(final Panel right, ListOptions listOptions) {
    PagingExerciseList<CommonShell, CommonExercise> widgets =
        new NPExerciseList(right,
            controller,
            listOptions.setIncorrectFirst(true)) {
          @Override
          protected void onLastItem() {
          } // TODO : necessary?

          @Override
          protected void addTableWithPager(SimplePagingContainer<?> pagingContainer) {
            pagingContainer.getTableWithPager(listOptions);
          }

          @Override
          protected void addMinWidthStyle(Panel leftColumn) {
          }
        };
    final ExercisePanelFactory<CommonShell, CommonExercise> factory = getFactory(null, listOptions.getInstance(), true);
    widgets.setFactory(factory);
    return widgets;
  }

  @Override
  ExercisePanelFactory<CommonShell, CommonExercise> getFactory(PagingExerciseList<CommonShell, CommonExercise> exerciseList,
                                                               final String instanceName, boolean showQC) {
    StatsFlashcardFactory<CommonShell, CommonExercise> avpHelper =
        new StatsFlashcardFactory<>( controller, exerciseList, "AVPHelper", ul);
    avpHelper.setContentPanel(contentPanel);
    return avpHelper;
  }

  void addExerciseListOnLeftSide(Panel left, Widget exerciseListOnLeftSide) {
  }

  @Override
  public void onResize() {
  }
}
