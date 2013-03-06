package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExerciseQuestionState;
import mitll.langtest.client.recorder.RecordButtonPanel;
import mitll.langtest.client.user.UserFeedback;
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
  private static final String THE_FOREIGN_LANGUAGE = " the foreign language";
  private static final String ENGLISH = "English";
  //private static final String TYPE_YOUR_ANSWER_IN = "Type your answer in ";
  private static final String SPEAK_AND_RECORD_YOUR_ANSWER_IN = "Speak your answer in ";
  private List<MyRecordButtonPanel> answerWidgets = new ArrayList<MyRecordButtonPanel>();

  public BootstrapExercisePanel(final Exercise e, final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                                final ExerciseController controller) {
    setStyleName("exerciseBackground");

    FluidRow fluidRow = new FluidRow();
    add(fluidRow);
    fluidRow.add(new Column(12,getQuestionContent(e)));
    addQuestions(e, service, controller, 1);
  }

  private Widget getQuestionContent(Exercise e) { return new HTML(e.getContent()); }
  /**
   * For every question,
   * <ul>
   *  <li>show the text of the question,  </li>
   *  <li>the prompt to the test taker (e.g "Speak your response in English")  </li>
   *  <li>an answer widget (either a simple text box, an flash audio record and playback widget, or a list of the answers, when grading </li>
   * </ul>     <br></br>
   * Remember the answer widgets so we can notice which have been answered, and then know when to enable the next button.
   * @param e
   * @param service
   * @param controller used in subclasses for audio control
   * @param questionNumber
   */
  private void addQuestions(Exercise e, LangTestDatabaseAsync service, ExerciseController controller, int questionNumber) {
    //for (Exercise.QAPair pair : e.getQuestions()) {
      // add question header
      questionNumber++;
      // add question prompt
      FluidRow row = new FluidRow();
      add(row);
      row.add(new Column(12,new HTML(getQuestionPrompt(e.promptInEnglish))));

      // add answer widget
      MyRecordButtonPanel answerWidget = getAnswerWidget(e, service, controller, questionNumber - 1);
      this.answerWidgets.add(answerWidget);
      row.add(new Column(12, answerWidget.getRecordButton()));
  //  }
  }

  protected String getQuestionPrompt(boolean promptInEnglish) {
    return "Click record to check your pronunciation.";//SPEAK_AND_RECORD_YOUR_ANSWER_IN + (promptInEnglish ? ENGLISH : THE_FOREIGN_LANGUAGE) + " ";
  }

  //private List<MyRecordButtonPanel> answerWidgets
  protected MyRecordButtonPanel getAnswerWidget(final Exercise exercise, LangTestDatabaseAsync service, ExerciseController controller, final int index) {
     return new MyRecordButtonPanel(service, controller, exercise, index);
  }

  private static class MyRecordButtonPanel extends RecordButtonPanel {
    private final Exercise exercise;

    public MyRecordButtonPanel(LangTestDatabaseAsync service, ExerciseController controller, Exercise exercise, int index) {
      super(service, controller, exercise, null, index);
      this.exercise = exercise;
    }

    @Override
    protected void layoutRecordButton() {
    }

    @Override
    protected void receivedAudioAnswer(AudioAnswer result, ExerciseQuestionState questionState, Panel outer) {
      controller.loadNextExercise(exercise);
    }
  }

  @Override
  protected void onUnload() {
    for (MyRecordButtonPanel answers : answerWidgets) {
      answers.onUnload();
    }
  }
}
