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
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.IViewContaner;
import mitll.langtest.client.custom.SimpleChapterNPFHelper;
import mitll.langtest.client.custom.content.FlexListLayout;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.flashcard.HidePolyglotFactory;
import mitll.langtest.client.flashcard.PolyglotDialog;
import mitll.langtest.client.flashcard.PolyglotPracticePanel;
import mitll.langtest.client.flashcard.QuizIntro;
import mitll.langtest.client.list.FacetExerciseList;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.shared.custom.IUserList;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static mitll.langtest.client.flashcard.PolyglotDialog.MODE_CHOICE.POLYGLOT;
import static mitll.langtest.client.list.FacetExerciseList.LISTS;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 2/4/16.
 */
public class QuizHelper extends PracticeHelper {
  private final Logger logger = Logger.getLogger("QuizHelper");

  private PolyglotDialog.PROMPT_CHOICE prompt = PolyglotDialog.PROMPT_CHOICE.NOT_YET;
  private final INavigation navigation;

  /**
   * @param controller
   * @param viewContaner
   * @param myView
   * @see NewContentChooser#NewContentChooser(ExerciseController, IBanner)
   */
  QuizHelper(ExerciseController controller, IViewContaner viewContaner, INavigation.VIEWS myView,
             INavigation navigation) {
    super(controller, viewContaner, myView);
    this.navigation = navigation;
  }

  public interface QuizChoiceListener {
    void gotChoice(int listid);
  }

  @Override
  protected ExercisePanelFactory<CommonShell, CommonExercise> getFactory(PagingExerciseList<CommonShell, CommonExercise> exerciseList) {

    polyglotFlashcardFactory = new HidePolyglotFactory<CommonShell, CommonExercise>(controller, exerciseList, "Quiz") {
      private int chosenList = -1;

      @Override
      public Panel getExercisePanel(CommonExercise e) {
        FacetExerciseList exerciseList = (FacetExerciseList) this.getExerciseList();
        boolean hasSelectionForType = exerciseList.hasSelectionForType(LISTS);
        if (hasSelectionForType) {
          return super.getExercisePanel(e);
        } else {
          return new QuizIntro(exerciseList.getIdToList(), listid -> {
            Map<String, String> candidate = new HashMap<>(exerciseList.getTypeToSelection());
            candidate.put(LISTS, "" + listid);
            exerciseList.setHistory(candidate);

            chosenList = listid;
            showQuizForReal();
          },
              controller.getUserManager().getUserID());
        }
      }

      @Override
      public PolyglotDialog.MODE_CHOICE getMode() {
        return POLYGLOT;
      }

      @Override
      public int getRoundTimeMinutes(boolean isDry) {
        FacetExerciseList exerciseList = (FacetExerciseList) this.getExerciseList();
        Map<Integer, IUserList> idToList = exerciseList.getIdToList();
        if (idToList == null) {
          logger.info("getRoundTimeMinutes no user lists yet ");
          return 10;
        } else {
          if (chosenList == -1) {
            setChosenList(exerciseList);
            reallyStartOver();
          }

          IUserList iUserList = idToList.get(chosenList);
          //   logger.info("getRoundTimeMinutes iUserList " + iUserList + " for " + chosenList);
          //   logger.info("getRoundTimeMinutes iUserList " + idToList.keySet());
          return iUserList == null ? 10 : iUserList.getRoundTimeMinutes();
        }
      }

      private void setChosenList(FacetExerciseList exerciseList) {
        Map<String, String> candidate = new HashMap<>(exerciseList.getTypeToSelection());
        String s = candidate.get(LISTS);
        //   logger.info("getRoundTimeMinutes iUserList " + s);
        if (s != null && !s.isEmpty()) {
          try {
            chosenList = Integer.parseInt(s);
          } catch (NumberFormatException e) {
            logger.warning("couldn't parse list id " + s);
          }
        }
      }

      /**
       * @see PolyglotPracticePanel#reallyStartOver
       */
      @Override
      public void showQuiz() {
        super.showQuiz();
        logger.info("showQuiz clearListSelection ");
        clearListSelection();
      }
    };

    statsFlashcardFactory = polyglotFlashcardFactory;
    statsFlashcardFactory.setContentPanel(outerBottomRow);
    return statsFlashcardFactory;
  }


  private Panel rememberedTopRow;

  /**
   * @param outer
   * @return
   */
  @Override
  protected FlexListLayout<CommonShell, CommonExercise> getMyListLayout(SimpleChapterNPFHelper<CommonShell, CommonExercise> outer) {
    return new MyFlexListLayout<CommonShell, CommonExercise>(controller, outer) {
      @Override
      protected PagingExerciseList<CommonShell, CommonExercise> makeExerciseList(Panel topRow,
                                                                                 Panel currentExercisePanel,
                                                                                 String instanceName, DivWidget listHeader, DivWidget footer) {
        rememberedTopRow = topRow;
        return new MyPracticeFacetExerciseList(topRow, currentExercisePanel, instanceName, listHeader);
      }

      @Override
      protected void styleBottomRow(Panel bottomRow) {
        bottomRow.addStyleName("centerPractice");
        outerBottomRow = bottomRow;
      }
    };
  }

  private void showQuizForReal() {
    setMode(POLYGLOT, prompt);
    setNavigation(navigation);
    hideList();
  }

  @Override
  public void showContent(Panel listContent, String instanceName, boolean fromClick) {
    super.showContent(listContent, instanceName, fromClick);
    //  hideList();
//    logger.info("Set visible  on " + rememberedTopRow.getElement().getId());
//    logger.info("Set visible with children " + rememberedTopRow.getElement().getChildCount());
    rememberedTopRow.getParent().setVisible(false);
    //rememberedTopRow.setVisible(false);
  }

  void showQuizIntro() {
    logger.info("showQuizIntro clearListSelection ");
    Scheduler.get().scheduleDeferred(this::clearListSelection);
  }

  private void clearListSelection() {
    logger.info("---> clearListSelection ");
    FacetExerciseList exerciseList = (FacetExerciseList) getPolyglotFlashcardFactory().getExerciseList();
    exerciseList.clearListSelection();
  }

  private class MyPracticeFacetExerciseList extends PracticeFacetExerciseList {
    MyPracticeFacetExerciseList(Panel topRow, Panel currentExercisePanel, String instanceName, DivWidget listHeader) {
      super(QuizHelper.this.controller, QuizHelper.this, topRow, currentExercisePanel, instanceName, listHeader);
    }

    protected UserList.LIST_TYPE getListType() {
      return UserList.LIST_TYPE.QUIZ;
    }
  }
}
