package mitll.langtest.client.grading;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.ResultManager;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanel;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Grade;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.ResultsAndGrades;

import java.util.*;

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
   */
  public GradingExercisePanel(final Exercise e, final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                              final ExerciseController controller) {
    super(e,service,userFeedback,controller);
    this.userFeedback = userFeedback;
    enableNextButton(true);
  }

  /**
   * If controller is english only, then show the answer too.
   *
   * @param i
   * @param total
   * @param engQAPair
   * @param pair
   * @param showAnswer
   */
  @Override
  protected void getQuestionHeader(int i, int total, Exercise.QAPair engQAPair, Exercise.QAPair pair, boolean showAnswer) {
    String english = engQAPair.getQuestion();
    String prefix = "Question" +
        (total > 1 ? " #" + i : "") +
        " : ";

    if (showAnswer)  {
      String answer = engQAPair.getAnswer();
      add(new HTML("<br></br><b>" + prefix + "</b>" +english));
      add(new HTML("<b>Answer : &nbsp;&nbsp;</b>"+answer + "<br></br>"));
    }
    else {
      String questionHeader = prefix + pair.getQuestion();
      add(new HTML("<h4>" + questionHeader + " / " + english + "</h4>"));
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

  protected int getQuestionPromptSpacer() {
    return 0;
  }

  /**
   * Partitions results into 3 (or fewer) separate tables for each of the
   * possible spoken/written & english/f.l. combinations.
   * <br></br>
   * Uses a result manager table (simple pager).  {@link mitll.langtest.client.ResultManager#getTable}<br></br>
   * If the controller says this is an English only grading mode, then only show english answers.
   * @see ExercisePanel#ExercisePanel(mitll.langtest.shared.Exercise, LangTestDatabaseAsync, UserFeedback, ExerciseController)
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
            for (boolean isForeign : BOOLEANS) {
              if (englishOnly && isForeign) continue; // skip non-english
              List<Result> results = langToResult.get(isForeign);
              if (results != null) {
                anyAnswers = true;
                String prompt = getPrompt(isSpoken, isForeign, outer);
                System.out.println("\tgetResultsForExercise add answer group for results (index = " + index+ ")" + results);

                vp.add(addAnswerGroup(resultsAndGrades.grades, results, bigPage, prompt, outer, service, n, index));
              }
            }
          }
        }
        if (!anyAnswers) vp.add(new HTML("<b><i>No answers yet.</i></b>"));
        // if (result.isEmpty()) recordCompleted(outer);
      }
    });
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
   * @param outer
   * @param service
   * @param n
   * @param index
   * @return
   */
  private Widget addAnswerGroup(Collection<Grade> grades,
                                List<Result> results, boolean bigPage, String prompt,
                                GradingExercisePanel outer, LangTestDatabaseAsync service, int n, int index) {
    VerticalPanel vp = new VerticalPanel();

    int oneQuestionPageSize = bigPage ? BIG_ONE_QUESTION_PAGE_SIZE : ONE_QUESTION_PAGE_SIZE;
    int twoQuestionPageSize = bigPage ? BIG_TWO_QUESTION_PAGE_SIZE : TWO_QUESTION_PAGE_SIZE;
    vp.add(new HTML(prompt));
    SimplePanel spacer = new SimplePanel();
    spacer.setSize("50px", "5px");
    vp.add(spacer);

    vp.add(showResults(results, grades, service, outer, n > 1, index,
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
   * @param outer
   * @param moreThanOneQuestion
   * @param index
   * @param pageSize
   * @param twoQPageSize
   * @param grader
   * @return
   */
  private Widget showResults(Collection<Result> results, Collection<Grade> grades,
                             LangTestDatabaseAsync service, GradingExercisePanel outer,
                             boolean moreThanOneQuestion, int index, int pageSize, int twoQPageSize, int grader) {
    ResultManager rm = new GradingResultManager(service, userFeedback, false);
    rm.setFeedback(outer);
    rm.setPageSize(pageSize);
    if (moreThanOneQuestion) {
      List<Result> filtered = new ArrayList<Result>();
      for (Result r : results) if (r.qid == index) filtered.add(r);
      results = filtered;
      rm.setPageSize(twoQPageSize);
    }

    return rm.getTable(results, true, false, grades, grader, controller.getNumGradesToCollect());
  }

  @Override
  protected String getQuestionPrompt(boolean promptInEnglish) { return ""; }

  /**
   * Consider : on the server, notice which audio posts have arrived, and take the latest ones...
   *
   * @param service
   * @param userFeedback
   * @param controller
   * @param completedExercise
   */
  @Override
  protected void postAnswers(LangTestDatabaseAsync service, UserFeedback userFeedback, ExerciseController controller, Exercise completedExercise) {
    controller.loadNextExercise(completedExercise);
  }

  @Override
  protected String getNextButtonText() {
    return "Next Ungraded";
  }
}
