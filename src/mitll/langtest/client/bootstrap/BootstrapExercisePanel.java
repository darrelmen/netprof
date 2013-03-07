package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.PageHeader;
import com.google.gwt.user.client.Timer;
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
import java.util.Iterator;
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
    widgets.setText(e.getTooltip());
    return widgets;
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
    row.add(new Column(2, 5, recordButton));
    //  }
  }

  protected MyRecordButtonPanel getAnswerWidget(final Exercise exercise, LangTestDatabaseAsync service, ExerciseController controller, final int index) {
    return new MyRecordButtonPanel(service, controller, exercise, index, this);
  }

  private static class MyRecordButtonPanel extends RecordButtonPanel {
    private final Exercise exercise;
    private Panel outerPanel;
    public MyRecordButtonPanel(LangTestDatabaseAsync service, ExerciseController controller, Exercise exercise, int index,Panel outerPanel) {
      super(service, controller, exercise, null, index);
      this.outerPanel = outerPanel;
      this.exercise = exercise;
    }

    @Override
    protected void layoutRecordButton() {
    }

    @Override
    protected void receivedAudioAnswer(AudioAnswer result, ExerciseQuestionState questionState, Panel outer) {
      double score = result.score;
     // setStyleRecurse(outerPanel,score);
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

    private void setStyleRecurse(Panel outerPanel, double score) {
      Iterator<Widget> iterator = outerPanel.iterator();
      for (; iterator.hasNext(); ) {
        Widget next = iterator.next();

        next.addStyleName((score > 0.6) ? "correctCard" : "incorrectCard");
        if (next instanceof Panel) {
          setStyleRecurse((Panel)next,score);
        }
      }
    }
  }

  @Override
  protected void onUnload() {
    for (MyRecordButtonPanel answers : answerWidgets) {
      answers.onUnload();
    }
  }
}
