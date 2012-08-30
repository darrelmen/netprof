package mitll.langtest.client.goodwave;

import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanel;
import mitll.langtest.client.recorder.SimpleRecordPanel;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Exercise;

/**
 * Mainly delegates recording to the {@link mitll.langtest.client.recorder.SimpleRecordPanel}.
 * User: GO22670
 * Date: 5/11/12
 * Time: 11:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class GoodwaveExercisePanel extends ExercisePanel {
  /**
   * @see mitll.langtest.client.exercise.ExercisePanelFactory#getExercisePanel(mitll.langtest.shared.Exercise)
   * @param e
   * @param service
   * @param userFeedback
   * @param controller
   */
  public GoodwaveExercisePanel(final Exercise e, final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                               final ExerciseController controller) {
    super(e,service,userFeedback,controller);
  }

  @Override
  protected Widget getQuestionContent(Exercise e) {
    String content = e.getContent();
    if (content.contains("audio")) {
     // System.err.println("content " + content);

      int i = content.indexOf("source src=");
      String s = content.substring(i + "source src=".length()+1).split("\\\"")[0];
      System.err.println("audio path '" + s + "'");
    }


    // TODO make a good wave panel that plays audio, displays the wave form...
    return super.getQuestionContent(e);    //To change body of overridden methods use File | Settings | File Templates.
  }

  /**
   * Has a answerPanel mark to indicate when the saved audio has been successfully posted to the server.
   *
   *
   * @see mitll.langtest.client.exercise.ExercisePanel#ExercisePanel(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserFeedback, mitll.langtest.client.exercise.ExerciseController)
   * @param exercise
   * @param service
   * @param controller
   * @param index
   * @return
   */
  @Override
  protected Widget getAnswerWidget(Exercise exercise, LangTestDatabaseAsync service, ExerciseController controller, final int index) {

    GoodwaveRecordPanel widgets = new GoodwaveRecordPanel(service, controller, exercise, this, index);
    GoodWaveCaptionPanel testCaptionPanel = new GoodWaveCaptionPanel("User Recorder", widgets);
    testCaptionPanel.add(widgets);

    testCaptionPanel.setHeight("600px");
    return testCaptionPanel;
  }

  @Override
  protected String getQuestionPrompt(boolean promptInEnglish) {
    return getSpokenPrompt(promptInEnglish);
  }

  /**
   * on the server, notice which audio posts have arrived, and take the latest ones...
   * <br></br>
   * Move on to next exercise...
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
