package mitll.langtest.client;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.recorder.SimpleRecordPanel;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Result;

import java.util.Collections;
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
  }

  /**
   * Has a answerPanel mark to indicate when the saved audio has been successfully posted to the server.
   *
   *
   * @see ExercisePanel#ExercisePanel(mitll.langtest.shared.Exercise, LangTestDatabaseAsync, UserFeedback, ExerciseController)
   * @param exercise
   * @param service
   * @param controller
   * @param index  @return
   */
  @Override
  protected Widget getAnswerWidget(Exercise exercise, final LangTestDatabaseAsync service, ExerciseController controller, final int index) {
    final SimplePanel vp = new SimplePanel();
    final int n = exercise.getNumQuestions();
    final GradingExercisePanel outer = this;
    service.getResultsForExercise(exercise.getID(), new AsyncCallback<List<Result>>() {
      public void onFailure(Throwable caught) {}

      /**
       * TODO : consider sorting by answer type (audio vs text)
       * @param result
       */
      public void onSuccess(List<Result> result) {
        ResultManager rm = new ResultManager(service, userFeedback);
        rm.setFeedback(outer);
        vp.add(rm.getTable(result, true,(n > 1)));
        //System.err.println("found " +result.size() + " results");
        if (result.isEmpty()) enableNextButton(true);
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
