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

package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.ListItem;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.IViewContaner;
import mitll.langtest.client.custom.SimpleChapterNPFHelper;
import mitll.langtest.client.custom.content.FlexListLayout;
import mitll.langtest.client.custom.content.NPFlexSectionExerciseList;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.flashcard.HidePolyglotFactory;
import mitll.langtest.client.flashcard.PolyglotDialog;
import mitll.langtest.client.flashcard.PolyglotFlashcardFactory;
import mitll.langtest.client.flashcard.StatsFlashcardFactory;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.project.ProjectStartupInfo;
import mitll.langtest.shared.project.ProjectType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 2/4/16.
 */
public class QuizHelper extends PracticeHelper {
  private final Logger logger = Logger.getLogger("QuizHelper");

  /**
   * @param controller
   * @param viewContaner
   * @param myView
   * @see NewContentChooser#NewContentChooser(ExerciseController, IBanner)
   */
  QuizHelper(ExerciseController controller, IViewContaner viewContaner, INavigation.VIEWS myView) {
    super(controller, viewContaner, myView);
  }

  @Override
  protected ExercisePanelFactory<CommonShell, CommonExercise> getFactory(PagingExerciseList<CommonShell, CommonExercise> exerciseList) {

    polyglotFlashcardFactory = new HidePolyglotFactory<>(controller, exerciseList, "Quiz");
    statsFlashcardFactory = polyglotFlashcardFactory;


    statsFlashcardFactory.setContentPanel(outerBottomRow);
    return statsFlashcardFactory;
  }

  /**
   * @param outer
   * @return
   */
  @Override
  protected FlexListLayout<CommonShell, CommonExercise> getMyListLayout(SimpleChapterNPFHelper<CommonShell, CommonExercise> outer) {
    return new MyFlexListLayout<CommonShell, CommonExercise>(controller, outer) {
      final FlexListLayout outer = this;

      @Override
      protected PagingExerciseList<CommonShell, CommonExercise> makeExerciseList(Panel topRow,
                                                                                 Panel currentExercisePanel,
                                                                                 String instanceName, DivWidget listHeader, DivWidget footer) {
        return new PracticeFacetExerciseList(controller, QuizHelper.this, topRow, currentExercisePanel, instanceName, listHeader) {
          @NotNull
          @Override
          protected FilterRequest getRequest(int userListID, List<Pair> pairs) {
            return new FilterRequest(incrReqID(), pairs, userListID).setQuiz(true);
          }

          @Override
          protected void getTypeOrder() {
            typeOrder = getTypeOrderSimple();
            //    logger.info("getTypeOrder type order " + typeOrder);
            this.rootNodesInOrder = new ArrayList<>(typeOrder);
          }

          @Override
          protected List<String> getTypeOrderSimple() {
            return Arrays.asList("QUIZ");
          }

          @Override
          protected ListItem addListFacet(Map<String, Set<MatchInfo>> typeToValues) {
            return null;
          }

          @Override
          protected ExerciseListRequest getRequest(String prefix) {
            ExerciseListRequest request = super.getRequest(prefix);
            request.setQuiz(true);
            return request;
          }

        };
      }

      @Override
      protected void styleBottomRow(Panel bottomRow) {
        bottomRow.addStyleName("centerPractice");
        outerBottomRow = bottomRow;
      }
    };
  }

}
