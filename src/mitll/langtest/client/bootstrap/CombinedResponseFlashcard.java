package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExerciseQuestionState;
import mitll.langtest.client.recorder.AutoCRTRecordPanel;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.Exercise;

import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/4/13
 * Time: 3:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class CombinedResponseFlashcard extends TextCRTFlashcard implements ExerciseQuestionState {
  private Set<Widget> completed = new HashSet<Widget>();

  public CombinedResponseFlashcard(Exercise e, LangTestDatabaseAsync service, ExerciseController controller, UserManager userManager) {
    super(e, service, controller, userManager);
  }

  @Override
  protected void gotScoreForGuess(Double result, Button check, ScoreFeedback scoreFeedback) {
    super.gotScoreForGuess(result,check,scoreFeedback);
    completed.add(check);
    enableNext();
  }

  public void recordIncomplete(Widget answer) {
    completed.remove(answer);
    enableNext();
  }

  public void recordCompleted(Widget answer) {
    completed.add(answer);
    enableNext();
  }

  private void enableNext() {
   // System.out.println("enabled " + completed.size());
    navigationHelper.enableNextButton((completed.size() == 2));
  }

  protected void addRecordingAndFeedbackWidgets(Exercise exercise, LangTestDatabaseAsync service, ExerciseController controller,
                                                int feedbackHeight) {
    super.addRecordingAndFeedbackWidgets(exercise,service,controller,feedbackHeight);
    autoCRTRecordPanel = new AutoCRTRecordPanel(service, controller, exercise, this, 1) {
      @Override
      protected void receivedAudioAnswer(AudioAnswer result, final ExerciseQuestionState questionState, final Panel outer) {
        super.receivedAudioAnswer(result, questionState, outer);
        scoreFeedback.hideFeedback();
      }
    };
    Panel panel = autoCRTRecordPanel.getPanel();
    add(getRecordButtonRow(getVerticalPanel(panel, new ScoreFeedback(false), controller)));
  }

  private AutoCRTRecordPanel autoCRTRecordPanel;
  public Panel getVerticalPanel(Panel panel, ScoreFeedback scoreFeedback, ExerciseController controller) {
    FluidRow outer = new FluidRow();

    FluidContainer container = new FluidContainer();
    outer.add(new Column(5));
    outer.add(container);

    FluidRow row1 = new FluidRow();
    container.add(row1);
    row1.add(panel);

    SimplePanel simplePanel = new SimplePanel(scoreFeedback.getFeedbackImage());
    simplePanel.addStyleName("floatLeft");

    Panel scoreFeedbackRow = scoreFeedback.getScoreFeedbackRow2(simplePanel, 48);

    scoreFeedback.getScoreFeedback().setWidth(Window.getClientWidth() * 0.5 + "px");
    scoreFeedback.getScoreFeedback().addStyleName("topBarMargin");

    container.add(scoreFeedbackRow);

    HTML warnNoFlash = new HTML(BootstrapExercisePanel.WARN_NO_FLASH);
    autoCRTRecordPanel.setSoundFeedback(new SoundFeedback(controller.getSoundManager(), warnNoFlash));
    autoCRTRecordPanel.setScoreFeedback(scoreFeedback);
    warnNoFlash.setVisible(false);
    FluidRow row3 = new FluidRow();
    container.add(row3);
    row3.add(warnNoFlash);

    return outer;
  }
}
