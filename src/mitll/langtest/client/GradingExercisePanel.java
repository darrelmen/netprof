package mitll.langtest.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.ResultsAndGrades;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 5/11/12
 * Time: 11:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class GradingExercisePanel extends ExercisePanel {
  private UserFeedback userFeedback;
  /**
   * @seex LangTest#loadExercise
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
   * Has a answerPanel mark to indicate when the saved audio has been successfully posted to the server.
   *
   *
   * @see ExercisePanel#ExercisePanel(mitll.langtest.shared.Exercise, LangTestDatabaseAsync, UserFeedback, ExerciseController)
   * @param exercise
   * @param service
   * @param controller
   * @param index
   * @return
   */
  @Override
  protected Widget getAnswerWidget(Exercise exercise, final LangTestDatabaseAsync service, ExerciseController controller, final int index) {
    final SimplePanel vp = new SimplePanel();
    final int n = exercise.getNumQuestions();
    final GradingExercisePanel outer = this;
    service.getResultsForExercise(exercise.getID(), new AsyncCallback<ResultsAndGrades>() {
      public void onFailure(Throwable caught) {}

      public void onSuccess(ResultsAndGrades resultsAndGrades) {
        ResultManager rm = new ResultManager(service, userFeedback);
        rm.setFeedback(outer);
        boolean moreThanOneQuestion = n > 1;
        Collection<Result> results = resultsAndGrades.results;
        rm.setPageSize(8);
        if (moreThanOneQuestion) {
          List<Result> filtered = new ArrayList<Result>();
          for (Result r : results) if (r.qid == index) filtered.add(r);
          results = filtered;
          rm.setPageSize(4);
        }

        vp.add(rm.getTable(results, true, false, resultsAndGrades.grades));
       // if (result.isEmpty()) recordCompleted(outer);
      }
    });
    return vp;
  }

  @Override
  protected String getQuestionPrompt(Exercise e) {
    return "";
  }

  /**
   * TODO : on the server, notice which audio posts have arrived, and take the latest ones...
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
}
