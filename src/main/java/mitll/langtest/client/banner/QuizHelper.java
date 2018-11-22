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
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.SimpleChapterNPFHelper;
import mitll.langtest.client.custom.content.FlexListLayout;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.flashcard.HidePolyglotFactory;
import mitll.langtest.client.flashcard.PolyglotDialog;
import mitll.langtest.client.flashcard.PolyglotPracticePanel;
import mitll.langtest.client.flashcard.QuizIntro;
import mitll.langtest.client.list.*;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.ScoredExercise;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.logging.Logger;

import static mitll.langtest.client.flashcard.PolyglotDialog.MODE_CHOICE.POLYGLOT;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 2/4/16.
 */
public class QuizHelper<T extends CommonShell & ScoredExercise, U extends ClientExercise> extends PracticeHelper<T, U> {
  private final Logger logger = Logger.getLogger("QuizHelper");

  private final INavigation navigation;
  private int chosenList = -1;

  /**
   * @param controller
   * @see NewContentChooser#NewContentChooser(ExerciseController, IBanner)
   */
  QuizHelper(ExerciseController controller, INavigation navigation) {
    super(controller, INavigation.VIEWS.QUIZ);
    this.navigation = navigation;
  }

  public interface QuizChoiceListener {
    void gotChoice(int listid);
  }

  @Override
  protected ExercisePanelFactory<T, U> getFactory(PagingExerciseList<T, U> exerciseList) {
    polyglotFlashcardFactory = new HidePolyglotFactory<T, U>(controller, exerciseList, INavigation.VIEWS.QUIZ) {

      @Override
      public PolyglotDialog.MODE_CHOICE getMode() {
        return POLYGLOT;
      }

      /**
       * @see PolyglotPracticePanel#reallyStartOver
       */
      @Override
      public void showQuiz() {
        super.showQuiz();
        clearListSelection();
        QuizPracticeFacetExerciseList exerciseList = (QuizPracticeFacetExerciseList) this.getExerciseList();
        exerciseList.showQuizIntro();
      }
    };

    statsFlashcardFactory = polyglotFlashcardFactory;
    statsFlashcardFactory.setContentPanel(outerBottomRow);
    return statsFlashcardFactory;
  }


  private Panel rememberedTopRow;

  /**
   * TODO : why can't compiler figure this out????
   * What am I doing wrong?
   *
   * @param outer
   * @return
   */
  @Override
  protected FlexListLayout<T, U> getMyListLayout(SimpleChapterNPFHelper<T, U> outer) {
    return new MyFlexListLayout<T, U>(controller, outer) {
      @Override
      protected PagingExerciseList<T, U> makeExerciseList(Panel topRow,
                                                          Panel currentExercisePanel,
                                                          INavigation.VIEWS instanceName, DivWidget listHeader, DivWidget footer) {
        rememberedTopRow = topRow;
        return (PagingExerciseList<T, U>) new QuizPracticeFacetExerciseList(topRow, currentExercisePanel, listHeader);
      }

      @Override
      protected void styleBottomRow(Panel bottomRow) {
        bottomRow.addStyleName("centerPractice");
        outerBottomRow = bottomRow;
      }
    };
  }

  /**
   * @see QuizPracticeFacetExerciseList#getQuizIntro
   */
  private void showQuizForReal() {
    setMode(POLYGLOT);
    setNavigation(navigation);
    hideList();
  }

  @Override
  public void showContent(Panel listContent, INavigation.VIEWS views) {
    super.showContent(listContent, views);
    rememberedTopRow.getParent().setVisible(false);
  }

  void showQuizIntro() {
    //Scheduler.get().scheduleDeferred(this::clearListSelection);

    clearListSelection();
  }

  private void clearListSelection() {
    logger.info("clearListSelection");
    ListFacetExerciseList exerciseList = (ListFacetExerciseList) getPolyglotFlashcardFactory().getExerciseList();
    exerciseList.clearListSelection();
  }

  private class QuizPracticeFacetExerciseList extends PracticeFacetExerciseList<T, U> {
    QuizPracticeFacetExerciseList(Panel topRow, Panel currentExercisePanel, DivWidget listHeader) {
      super(topRow, currentExercisePanel, QuizHelper.this.controller,
          new ListOptions().setInstance(INavigation.VIEWS.QUIZ).setShowPager(false),
          listHeader, INavigation.VIEWS.QUIZ, QuizHelper.this
      );
    }


    @Override
    protected void loadFirstExercise(String searchIfAny) {
      SelectionState selectionState = new SelectionState(History.getToken(), false);
      Collection<String> lists = selectionState.getTypeToSection().get(LISTS);
      logger.info("loadFirstExercise chosen = " + chosenList);

      if (//chosenList == -1 &&
          (lists == null || lists.isEmpty())) {
        logger.info("skip load first exercise - no list");
        showQuizIntro();
      } else {
        if (chosenList == -1) {
          String next = lists.iterator().next();
          try {
            chosenList = Integer.parseInt(next);
            logger.info("chosen list now " + chosenList);
          } catch (NumberFormatException e) {
            logger.warning("couldn't parse " + next);
          }
        }
        super.loadFirstExercise(searchIfAny);
      }
    }

    void showQuizIntro() {
      createdPanel = getQuizIntro();
      innerContainer.setWidget(createdPanel);
    }

    @NotNull
    private QuizIntro getQuizIntro() {
      return new QuizIntro(getIdToList(), listid -> {
        polyglotFlashcardFactory.cancelRoundTimer();
        chosenList = listid;
        // logger.info("got choice " + listid);
        polyglotFlashcardFactory.removeItemFromHistory(chosenList);
        showQuizForReal();
        polyglotFlashcardFactory.startQuiz();
      },
          controller.getUserManager().getUserID());
    }

    @Override
    @NotNull
    protected DivWidget getPagerAndSort(ExerciseController controller) {
      DivWidget pagerAndSort = super.getPagerAndSort(controller);
      pagerAndSort.setVisible(false);
      return pagerAndSort;
    }

    protected UserList.LIST_TYPE getListType() {
      return UserList.LIST_TYPE.QUIZ;
    }
  }
}
