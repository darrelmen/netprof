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
import com.google.gwt.user.client.rpc.AsyncCallback;
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

//  private static final String PLEASE_CHOOSE_A_QUIZ = "Please choose a quiz on the left.";
/*
  private static final String NO_QUIZZES_YET = "No quizzes yet - please come back later.";

  private static final String QUIZ = "QUIZ";
  private static final String Unit = "Unit";
*/

//  private static final List<String> QUIZ_TYPE_ORDER = Arrays.asList(QUIZ, Unit);

  // private PolyglotDialog.MODE_CHOICE candidateMode = PolyglotDialog.MODE_CHOICE.NOT_YET;
  private PolyglotDialog.MODE_CHOICE mode = PolyglotDialog.MODE_CHOICE.NOT_YET;
  //
//  private PolyglotDialog.PROMPT_CHOICE candidatePrompt = PolyglotDialog.PROMPT_CHOICE.NOT_YET;
  private PolyglotDialog.PROMPT_CHOICE prompt = PolyglotDialog.PROMPT_CHOICE.NOT_YET;
  private final INavigation navigation;
  //private String historyToken;

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

      int chosenList = -1;

      @Override
      public Panel getExercisePanel(CommonExercise e) {
        FacetExerciseList exerciseList = (FacetExerciseList) this.exerciseList;
        boolean hasSelectionForType = exerciseList.hasSelectionForType(LISTS);
        if (hasSelectionForType) {
          return super.getExercisePanel(e);
        } else {
          return new QuizIntro(exerciseList.getIdToList(), listid -> {
            // logger.info("got yes " + listid);

            Map<String, String> candidate = new HashMap<>(exerciseList.getTypeToSelection());
            candidate.put(LISTS, "" + listid);
            exerciseList.setHistory(candidate);

            chosenList = listid;
            showQuizForReal();
          });
        }
      }

      @Override
      public int getRoundTimeMinutes(boolean isDry) {
        FacetExerciseList exerciseList = (FacetExerciseList) this.exerciseList;
        Map<Integer, IUserList> idToList = exerciseList.getIdToList();
        if (idToList == null) {
          logger.info("getRoundTimeMinutes no user lists yet ");
          return 10;
        }
        else {
          IUserList iUserList = idToList.get(chosenList);
          logger.info("getRoundTimeMinutes iUserList "+iUserList);
          return iUserList == null ? 10 : iUserList.getRoundTimeMinutes();
        }
      }

      /**
       * @see PolyglotPracticePanel#reallyStartOver
       */
      @Override
      public void showQuiz() {
        super.showQuiz();

        FacetExerciseList exerciseList = (FacetExerciseList) this.exerciseList;
        Map<String, String> candidate = new HashMap<>(exerciseList.getTypeToSelection());
        candidate.remove(LISTS);
        exerciseList.setHistory(candidate);
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
  public void showContent(Panel listContent, String instanceName) {
    super.showContent(listContent, instanceName);
    hideList();
//    logger.info("Set visible  on " + rememberedTopRow.getElement().getId());
//    logger.info("Set visible with children " + rememberedTopRow.getElement().getChildCount());
    rememberedTopRow.setVisible(false);
   }


  private class MyPracticeFacetExerciseList extends PracticeFacetExerciseList {
    MyPracticeFacetExerciseList(Panel topRow, Panel currentExercisePanel, String instanceName, DivWidget listHeader) {
      super(QuizHelper.this.controller, QuizHelper.this, topRow, currentExercisePanel, instanceName, listHeader);
    }

    protected UserList.LIST_TYPE getListType() {
      return UserList.LIST_TYPE.QUIZ;
    }

 /*   public void showEmpty() {
      showEmptyExercise(getEmptySearchMessage());
    }*/

    /**
     * @return
     * @see #showEmptySelection
     */
  /*  protected String getEmptySearchMessage() {
      logger.info("getEmptySearchMessage -- ");
      return hasValues ? PLEASE_CHOOSE_A_QUIZ : NO_QUIZZES_YET;
    }*/

     /*     protected void addListsAsLinks(Collection<IUserList> result, long then,
                                         Map<String, Set<MatchInfo>> finalTypeToValues, ListItem liForDimensionForType) {
            hasValues=
          super.addListsAsLinks(result,then,finalTypeToValues,liForDimensionForType);
          }*/
/*
          @Override
          protected void getTypeOrder() {
            typeOrder = getTypeOrderSimple();
            //    logger.info("getTypeOrder type order " + typeOrder);
            this.rootNodesInOrder = Collections.singletonList(QUIZ);
          }

          @Override
          protected List<String> getTypeOrderSimple() {
            return QUIZ_TYPE_ORDER;
          }

          @Override
          protected String getChildForParent(String childType) {
            Map<String, String> parentToChild = new HashMap<>();
            parentToChild.put(QUIZ, Unit);

            //Map<String, String> parentToChild = parentToChild;
            String s = parentToChild.get(childType);
//    logger.info("getChildForParent parent->child " + parentToChild);
//    logger.info("getChildForParent childType     " + childType + " = " + s);
            return s;
          }

          @Override
          protected ListItem addListFacet(Map<String, Set<MatchInfo>> typeToValues) {
            return null;
          }*/

/*          @Override
          protected ExerciseListRequest getRequest(String prefix) {
            ExerciseListRequest request = super.getRequest(prefix);
            request.setQuiz(true);
            return request;
          }

          protected void gotFilterResponse(FilterResponse response, long then, Map<String, String> typeToSelection) {
            super.gotFilterResponse(response, then, typeToSelection);

            Map<String, Set<MatchInfo>> typeToValues = response.getTypeToValues();
            Set<MatchInfo> matchInfos = typeToValues.get(QUIZ);

            hasValues = matchInfos != null && !matchInfos.isEmpty();

            *//*
            logger.info("gotFilterResponse took " + (System.currentTimeMillis() - then) + " to get" +
                "\n\ttype to select : " + typeToSelection +
                "\n\ttype to values : " + response.getTypeToValues() +
                "\n\thas values     : " + hasValues
            );
*//*

            Set<String> knownTypes = typeToSelection.keySet();
            if (knownTypes.contains(QUIZ) && !knownTypes.contains(Unit)) {
              showQuizDialog(getHistoryToken(), this);
            } else if (knownTypes.isEmpty()) {
              logger.info("no known types");
            }
          }*/
  }
}
