package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.PageHeader;
import com.github.gwtbootstrap.client.ui.base.IconAnchor;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExerciseQuestionState;
import mitll.langtest.client.recorder.RecordButtonPanel;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.Exercise;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 3/6/13
 * Time: 3:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class BootstrapExercisePanel extends FluidContainer {
  private List<MyRecordButtonPanel> answerWidgets = new ArrayList<MyRecordButtonPanel>();
  private Image image = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "animated_progress.gif"));

  public BootstrapExercisePanel(final Exercise e, final LangTestDatabaseAsync service,
                                final ExerciseController controller) {
    setStyleName("exerciseBackground");

    FluidRow fluidRow = new FluidRow();
    add(fluidRow);
    fluidRow.add(new Column(12, getQuestionContent(e)));
    addQuestions(e, service, controller, 1);
  }

  private Widget getQuestionContent(Exercise e) {
    PageHeader widgets = new PageHeader();
    widgets.addStyleName("correct");
    widgets.setText(getRefSentence(e));
    return widgets;
  }

  private String getRefSentence(Exercise other) {
    String e1 = other.getRefSentence().trim();
    if (e1.contains(";")) e1 = e1.split(";")[0];
    return e1;
  }

  /**
   * For every question,
   * <ul>
   * <li>show the text of the question,  </li>
   * <li>the prompt to the test taker (e.g "Speak your response in English")  </li>
   * <li>an answer widget (either a simple text box, an flash audio record and playback widget, or a list of the answers, when grading </li>
   * </ul>     <br></br>
   * Remember the answer widgets so we can notice which have been answered, and then know when to enable the next button.
   *
   * @param e
   * @param service
   * @param controller     used in subclasses for audio control
   * @param questionNumber
   */
  private void addQuestions(Exercise e, LangTestDatabaseAsync service, ExerciseController controller, int questionNumber) {
    //for (Exercise.QAPair pair : e.getQuestions()) {
    // add question header
    questionNumber++;
    // add question prompt
    FluidRow row = new FluidRow();
    add(row);
    //row.add(new Column(12,new HTML(getQuestionPrompt(e.promptInEnglish))));

    // add answer widget
    MyRecordButtonPanel answerWidget = getAnswerWidget(e, service, controller, questionNumber - 1);
    this.answerWidgets.add(answerWidget);
    Widget recordButton = answerWidget.getRecordButton();
    row.add(new Column(2, 5, recordButton,image));
    image.setVisible(false);
    //  }
  }

  protected MyRecordButtonPanel getAnswerWidget(final Exercise exercise, LangTestDatabaseAsync service, ExerciseController controller, final int index) {
    return new MyRecordButtonPanel(service, controller, exercise, index, this);
  }

  private class MyRecordButtonPanel extends RecordButtonPanel {
    private final Exercise exercise;
    private Panel outerPanel;
    public MyRecordButtonPanel(LangTestDatabaseAsync service, ExerciseController controller, Exercise exercise, int index,Panel outerPanel) {
      super(service, controller, exercise, null, index);
      this.outerPanel = outerPanel;
      this.exercise = exercise;
      recordImage.setHeight("32px");
      stopImage.setHeight("32px");
    }

    @Override
    protected void layoutRecordButton() {}

    @Override
    protected void stopRecording() {
      recordButton.hide();
      image.setVisible(true);
      super.stopRecording();
    }

    /**
     * @see mitll.langtest.client.recorder.RecordButtonPanel#stopRecording()
     * @param result
     * @param questionState
     * @param outer
     */
    @Override
    protected void receivedAudioAnswer(AudioAnswer result, ExerciseQuestionState questionState, Panel outer) {
      image.setVisible(false);
      recordImage.setUrl(RECORD_PNG);

      double score = result.score;
      outerPanel.addStyleName((score > 0.6) ? "correctCard" : "incorrectCard");

      Timer t = new Timer() {
        @Override
        public void run() {
          controller.loadNextExercise(exercise);
        }
      };

      // Schedule the timer to run once in 1 seconds.
      t.schedule(300);
    }

    @Override
    protected void receivedAudioFailure() {
      image.setVisible(false);
      recordImage.setUrl(RECORD_PNG);
    }
  }

  @Override
  protected void onUnload() {
    for (MyRecordButtonPanel answers : answerWidgets) {
      answers.onUnload();
    }
  }
}
