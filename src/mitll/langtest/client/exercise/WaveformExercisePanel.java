package mitll.langtest.client.exercise;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.scoring.ResizableCaptionPanel;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Result;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Does fancy flashing record bulb while recording.
 * User: GO22670
 * Date: 12/18/12
 * Time: 6:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class WaveformExercisePanel extends ExercisePanel {
  private boolean isBusy = false;
  private Collection<RecordAudioPanel> audioPanels;

  /**
   * @see mitll.langtest.client.exercise.ExercisePanelFactory#getExercisePanel(mitll.langtest.shared.Exercise)
   * @param e
   * @param service
   * @param userFeedback
   * @param controller
   */
  public WaveformExercisePanel(final Exercise e, final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                                   final ExerciseController controller, ListInterface exerciseList) {
    super(e, service, userFeedback, controller, exerciseList);
    getElement().setId("WaveformExercisePanel");
  }

  public void setBusy(boolean v) {
    this.isBusy = v;
    setButtonsEnabled(!isBusy);
  }
  public boolean isBusy() { return isBusy;  }

  /**
   * Has a answerPanel mark to indicate when the saved audio has been successfully posted to the server.
   *
   *
   * @see ExercisePanel#ExercisePanel(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserFeedback, ExerciseController, ListInterface)
   * @param exercise
   * @param service
   * @param controller
   * @param index
   * @return
   */
  @Override
  protected Widget getAnswerWidget(Exercise exercise, LangTestDatabaseAsync service, ExerciseController controller, final int index) {
    audioPanels = new ArrayList<RecordAudioPanel>();
    if (controller.getProps().getAudioType().equalsIgnoreCase(Result.AUDIO_TYPE_FAST_AND_SLOW)) {
      VerticalPanel vp = new VerticalPanel();

      RecordAudioPanel fast = new RecordAudioPanel(exercise, controller, this, service, index, true, Result.AUDIO_TYPE_REGULAR);
      ResizableCaptionPanel cp = new ResizableCaptionPanel("Regular speed recording");
      cp.setContentWidget(fast);
      vp.add(cp);

      audioPanels.add(fast);
      addAnswerWidget(index, fast);

      RecordAudioPanel slow = new RecordAudioPanel(exercise, controller, this, service, index+1, true, Result.AUDIO_TYPE_SLOW);

      cp = new ResizableCaptionPanel("Slow speed recording");
      cp.setContentWidget(slow);
      audioPanels.add(slow);
      addAnswerWidget(index+1, slow);

      vp.add(cp);
      return vp;
    } else {
      RecordAudioPanel fast = new RecordAudioPanel(exercise, controller, this, service, index, true, Result.AUDIO_TYPE_REGULAR);
      audioPanels.add(fast);
      addAnswerWidget(index, this);
      return fast;
    }
  }

  protected Widget getContentScroller(HTML maybeRTLContent) {
    return maybeRTLContent;
  }

  @Override
  protected String getInstructions() {
    String prefix = "<br/>" + THREE_SPACES;
    return prefix +"<i>Record the word or phrase twice, first at normal speed, then again at slow speed.</i>";
  }

  @Override
  public void onResize() {
    for (RecordAudioPanel ap : audioPanels) {
      ap.onResize();
    }
  }

  /**
   * @see ExercisePanel#addQuestionPrompt(com.google.gwt.user.client.ui.Panel, mitll.langtest.shared.Exercise)
   * @param promptInEnglish
   * @return
   */
  @Override
  protected String getQuestionPrompt(boolean promptInEnglish) {
    return getSpokenPrompt(promptInEnglish);
  }

  /**
   * on the server, notice which audio posts have arrived, and take the latest ones...
   * <br></br>
   * Move on to next exercise...
   *
   * @param controller
   * @param completedExercise
   */
  @Override
  public void postAnswers(ExerciseController controller, Exercise completedExercise) {
    exerciseList.addCompleted(completedExercise.getID());
    exerciseList.loadNextExercise(completedExercise);
  }
}
