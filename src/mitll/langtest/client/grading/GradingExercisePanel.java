package mitll.langtest.client.grading;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanel;
import mitll.langtest.client.exercise.NavigationHelper;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.result.ResultManager;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.grade.Grade;
import mitll.langtest.shared.grade.ResultsAndGrades;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Allows a grader to grade answers entered in the default mode.
 *
 * User: GO22670
 * Date: 5/11/12
 * Time: 11:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class GradingExercisePanel extends ExercisePanel {
  private static final int ONE_QUESTION_PAGE_SIZE = 3;
  private static final int TWO_QUESTION_PAGE_SIZE = ONE_QUESTION_PAGE_SIZE;

  private static final int BIG_ONE_QUESTION_PAGE_SIZE = 8;
  private static final int BIG_TWO_QUESTION_PAGE_SIZE = BIG_ONE_QUESTION_PAGE_SIZE/2;
  private static final List<Boolean> BOOLEANS = Arrays.asList(true, false);
  private UserFeedback userFeedback;

  /**
   * @see GradingExercisePanelFactory#getExercisePanel(mitll.langtest.shared.Exercise)
   * @param e
   * @param service
   * @param userFeedback
   * @param controller
   * @param listContainer
   */
  public GradingExercisePanel(final Exercise e, final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                              final ExerciseController controller, ListInterface listContainer) {
    super(e,service,userFeedback,controller, listContainer);
    this.userFeedback = userFeedback;
    enableNextButton(true);
  }

  /**
   * If controller is english only, then show the answer too.
   *
   * @param i
   * @param total
   * @param engQAPair
   * @param flQAPair
   * @param showAnswer
   * @param toAddTo
   */
  @Override
  protected void getQuestionHeader(int i, int total, Exercise.QAPair engQAPair, Exercise.QAPair flQAPair, boolean showAnswer, HasWidgets toAddTo) {
    String english = engQAPair.getQuestion();
    String prefix = "Question" +
        (total > 1 ? " #" + i : "") +
        " : ";

    if (showAnswer)  {
      String answer = engQAPair.getAnswer();
      toAddTo.add(new HTML("<br></br><b>" + prefix + "</b>" +english));
      toAddTo.add(new HTML("<b>Answer : &nbsp;&nbsp;</b>"+answer + "<br></br>"));
    }
    else {
      String questionHeader = prefix + flQAPair.getQuestion();
      toAddTo.add(new HTML("<h4>" + questionHeader + " / " + english + "</h4>"));
    }
  }

  /**
   * If controller is english only, then show the answer too.
   * @return true if in english only mode
   */
  @Override
  protected boolean shouldShowAnswer() {
    return super.shouldShowAnswer() || controller.getEnglishOnly();
  }

  /**
   * Partitions results into 3 (or fewer) separate tables for each of the
   * possible spoken/written & english/f.l. combinations.
   * <br></br>
   * Uses a result manager table (simple pager).  {@link mitll.langtest.client.result.ResultManager#getTable}<br></br>
   * If the controller says this is an English only grading mode, then only show english answers.
   * @see ExercisePanel#getQuestionPanel(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int, java.util.List, java.util.List, int, mitll.langtest.shared.Exercise.QAPair, com.google.gwt.user.client.ui.HasWidgets)
   * @param exercise
   * @param service
   * @param controller
   * @param index of the question (0 for first, 1 for second, etc.)
   * @return
   */
  @Override
  protected Widget getAnswerWidget(final Exercise exercise, final LangTestDatabaseAsync service,
                                   final ExerciseController controller, final int index) {
    final VerticalPanel vp = new VerticalPanel();
    final int n = exercise.getNumQuestions();
    final GradingExercisePanel outer = this;
    final boolean englishOnly = controller.getEnglishOnly();
    service.getResultsForExercise(exercise.getID(), controller.isArabicTextDataCollect(), new AsyncCallback<ResultsAndGrades>() {
      public void onFailure(Throwable caught) {}

      public void onSuccess(ResultsAndGrades resultsAndGrades) {
        System.out.println("getResultsForExercise (success) : " + exercise.getID() + " : " + resultsAndGrades);

        boolean anyAnswers = false;
        int count = countDistinctTypes(resultsAndGrades);
        boolean bigPage = count == 1 || englishOnly || controller.getNumGradesToCollect() > 1;
        for (boolean isSpoken : BOOLEANS) {
          Map<Boolean, List<Result>> langToResult = resultsAndGrades.spokenToLangToResult.get(isSpoken);
          if (langToResult != null) { // there might not be any written types
            System.out.println("spoken : " +isSpoken + " has " +langToResult.size() + " results");
            for (boolean isForeign : BOOLEANS) {
              if (englishOnly && isForeign) {
                System.out.println("\tskipping! spoken : " +isSpoken + " isFLQ " + isForeign +" has " +langToResult.size() + " results");

                continue; // skip non-english
              }
              List<Result> results = langToResult.get(isForeign);
              if (results != null) {
                System.out.println("\tspoken : " +isSpoken + " isFLQ " + isForeign +" has " +results.size() + " results");
                anyAnswers = true;
                String prompt = getPrompt(isSpoken, isForeign, outer);
                System.out.println("\tgetResultsForExercise add answer group for results (index = " + index+ ") size = " + results.size());

                vp.add(addAnswerGroup(resultsAndGrades.grades, results, bigPage, prompt, service, controller.getProps(), n, index));
              }
              else {
                System.out.println("\tspoken : " +isSpoken + " isFLQ " + isForeign);
              }
            }
          }
          else {
            System.out.println("spoken : " +isSpoken + " has " +langToResult + " results");
          }
        }
        if (!anyAnswers) vp.add(new HTML("<b><i>No answers yet.</i></b>"));
        // if (result.isEmpty()) recordCompleted(outer);
      }
    });
    addAnswerWidget(index, vp);

    return vp;
  }

  private String getPrompt(boolean isSpoken, boolean isForeign, GradingExercisePanel outer) {
    boolean isEnglish = !isForeign;
    return isSpoken ? outer.getSpokenPrompt(isEnglish) : outer.getWrittenPrompt(isEnglish);
  }

  /**
   * @see #getAnswerWidget
   * @param grades
   * @param results
   * @param bigPage
   * @param prompt
   * @param service
   * @param propertyHandler
   * @param n
   * @param index
   * @return
   */
  private Widget addAnswerGroup(Collection<Grade> grades,
                                List<Result> results, boolean bigPage, String prompt,
                                LangTestDatabaseAsync service, PropertyHandler propertyHandler, int n, int index) {
    VerticalPanel vp = new VerticalPanel();

    int oneQuestionPageSize = bigPage ? BIG_ONE_QUESTION_PAGE_SIZE : ONE_QUESTION_PAGE_SIZE;
    int twoQuestionPageSize = bigPage ? BIG_TWO_QUESTION_PAGE_SIZE : TWO_QUESTION_PAGE_SIZE;
    vp.add(new HTML(prompt));
    SimplePanel spacer = new SimplePanel();
    spacer.setSize("50px", "5px");
    vp.add(spacer);

    vp.add(showResults(results, grades, service, propertyHandler, n > 1, index,
        oneQuestionPageSize, twoQuestionPageSize, controller.getUser()));
    return vp;
  }

  /**
   * How many distinct types there are - combinations of spoken/written & flq/english q.
   *
   * @see #getAnswerWidget(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int)
   * @param resultsAndGrades
   * @return 1-3
   */
  private int countDistinctTypes(ResultsAndGrades resultsAndGrades) {
    int count = 0;
    for (boolean s : BOOLEANS) {
      Map<Boolean, List<Result>> langToResult = resultsAndGrades.spokenToLangToResult.get(s);
      if (langToResult != null) { // there might not be any written types
        for (boolean l : BOOLEANS) {
          List<Result> results = langToResult.get(l);
          if (results != null) {
             count++;
          }
        }
      }
    }
    return count;
  }

  /**
   * @see #getAnswerWidget(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int)
   * @param results
   * @param grades
   * @param service
   * @param propertyHandler
   * @param moreThanOneQuestion
   * @param index
   * @param pageSize
   * @param twoQPageSize
   * @param grader
   * @return
   */
  private Widget showResults(Collection<Result> results, Collection<Grade> grades,
                             LangTestDatabaseAsync service, PropertyHandler propertyHandler,
                             boolean moreThanOneQuestion, int index, int pageSize, int twoQPageSize, int grader) {
    ResultManager rm = new GradingResultManager(service, userFeedback, false, propertyHandler);
    rm.setPageSize(pageSize);
    if (moreThanOneQuestion) {
      List<Result> filtered = new ArrayList<Result>();
      for (Result r : results) if (r.qid == index) filtered.add(r);
      results = filtered;
      rm.setPageSize(twoQPageSize);
    }

    return rm.getTable(results, false, grades, grader, controller.getNumGradesToCollect());
  }

  @Override
  protected String getQuestionPrompt(boolean promptInEnglish) { return ""; }

  /**
   * Consider : on the server, notice which audio posts have arrived, and take the latest ones...
   *
   * @param controller
   * @param completedExercise
   */
  @Override
  public void postAnswers(ExerciseController controller, Exercise completedExercise) {
    exerciseList.loadNextExercise(completedExercise);
  }

  @Override
  protected NavigationHelper getNavigationHelper(ExerciseController controller) {
    return new NavigationHelper(exercise,controller, this, exerciseList, true) {
      @Override
      protected String getNextButtonText() {
        return "Next Ungraded";
      }
    };
  }
}
