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
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.IViewContaner;
import mitll.langtest.client.custom.SimpleChapterNPFHelper;
import mitll.langtest.client.custom.content.FlexListLayout;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.flashcard.HidePolyglotFactory;
import mitll.langtest.client.flashcard.PolyglotDialog;
import mitll.langtest.client.flashcard.PolyglotFlashcardFactory;
import mitll.langtest.client.flashcard.StatsFlashcardFactory;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.project.ProjectType;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 2/4/16.
 */
class PracticeHelper<T extends CommonShell, U extends CommonExercise & ClientExercise> extends SimpleChapterNPFHelper<T, U> {
  // private final Logger logger = Logger.getLogger("PracticeHelper");
  private static final String PRACTICE = "practice";

  StatsFlashcardFactory<T, U> statsFlashcardFactory;
  PolyglotFlashcardFactory<T, U> polyglotFlashcardFactory = null;
  Widget outerBottomRow;
  private PolyglotDialog.MODE_CHOICE mode;
  private PolyglotDialog.PROMPT_CHOICE promptChoice;
  private INavigation navigation;

  /**
   * @param controller
   * @param viewContaner
   * @param myView
   * @see NewContentChooser#NewContentChooser(ExerciseController, IBanner)
   */
  PracticeHelper(ExerciseController controller, IViewContaner viewContaner, INavigation.VIEWS myView) {
    super(controller, viewContaner, myView);
  }

  /**
   * @param exerciseList
   * @return
   * @see SimpleChapterNPFHelper.MyFlexListLayout#getFactory
   */
  @Override
  protected ExercisePanelFactory<T, U> getFactory(PagingExerciseList<T, U> exerciseList) {
    if (controller.getProjectStartupInfo().getProjectType() == ProjectType.POLYGLOT) {
      polyglotFlashcardFactory = new HidePolyglotFactory<>(controller, exerciseList, PRACTICE);
      statsFlashcardFactory = polyglotFlashcardFactory;
    } else {
      statsFlashcardFactory = new StatsFlashcardFactory<>(controller, exerciseList);
    }

    statsFlashcardFactory.setContentPanel(outerBottomRow);
    return statsFlashcardFactory;
  }

  /**
   * @param outer
   * @return
   */
  @Override
  protected FlexListLayout<T, U> getMyListLayout(SimpleChapterNPFHelper<T, U> outer) {
    return new MyFlexListLayout<T, U>(controller, outer) {
      @Override
      protected PagingExerciseList<T, U> makeExerciseList(Panel topRow,
                                                          Panel currentExercisePanel,
                                                          String instanceName, DivWidget listHeader, DivWidget footer) {
        return new PracticeFacetExerciseList(controller, PracticeHelper.this, topRow, currentExercisePanel, instanceName, listHeader);
      }

      @Override
      protected void styleBottomRow(Panel bottomRow) {
        bottomRow.addStyleName("centerPractice");
        outerBottomRow = bottomRow;
      }


    };
  }

  void setMode(PolyglotDialog.MODE_CHOICE mode, PolyglotDialog.PROMPT_CHOICE promptChoice) {
    this.mode = mode;
    this.promptChoice = promptChoice;
    if (polyglotFlashcardFactory != null) {
      polyglotFlashcardFactory.setMode(mode);
    }
  }

  public void setVisible(boolean visible) {
    flexListLayout.setVisible(visible);
  }

  public void setNavigation(INavigation navigation) {
    this.navigation = navigation;
  }

  StatsFlashcardFactory<T, U> getStatsFlashcardFactory() {
    return statsFlashcardFactory;
  }

  PolyglotFlashcardFactory<T, U> getPolyglotFlashcardFactory() {
    return polyglotFlashcardFactory;
  }

  public PolyglotDialog.MODE_CHOICE getMode() {
    return mode;
  }

  PolyglotDialog.PROMPT_CHOICE getPromptChoice() {
    return promptChoice;
  }

  public INavigation getNavigation() {
    return navigation;
  }
}
