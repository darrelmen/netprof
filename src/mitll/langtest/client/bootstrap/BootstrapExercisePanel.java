package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.PageHeader;
import com.github.gwtbootstrap.client.ui.Paragraph;
import com.github.gwtbootstrap.client.ui.base.IconAnchor;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExerciseQuestionState;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.recorder.RecordButtonPanel;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.Exercise;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 3/6/13
 * Time: 3:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class BootstrapExercisePanel extends FluidContainer {
  private static final int LONG_DELAY_MILLIS = 3500;
  private static final int DELAY_MILLIS = 1000/2;
  private static final int DELAY_MILLIS_LONG = 1500/2;
  private List<MyRecordButtonPanel> answerWidgets = new ArrayList<MyRecordButtonPanel>();

  private Image waitingForResponseImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "animated_progress48.gif"));
  private Image listeningImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "audio-input-microphone-3.png"));
  private Image correctImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "checkmark48.png"));
  private Image incorrectImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "redx48.png"));
  private Image enterImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "enter48.png"));
  private Heading recoOutput;
  private boolean keyIsDown;

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
    widgets.setText(getRefSentence(e));
    return widgets;
  }

  private String getRefSentence(Exercise other) {
    String e1 = other.getRefSentence().trim();
    if (e1.contains(";")) {
      e1 = e1.split(";")[0];
    }
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
    Paragraph paragraph = new Paragraph();
    paragraph.addStyleName("alignCenter");
    paragraph.add(recordButton);
    recordButton.addStyleName("alignCenter");
    row.add(new Column(12, paragraph));

    if (controller.isDemoMode()) {
      FluidRow row2 = new FluidRow();
      add(row2);
      Paragraph paragraph2 = new Paragraph();
      paragraph2.addStyleName("alignCenter");

      row2.add(new Column(12, paragraph2));
      recoOutput = new Heading(3);
      paragraph2.add(recoOutput);

    }
  }

  protected MyRecordButtonPanel getAnswerWidget(final Exercise exercise, LangTestDatabaseAsync service, ExerciseController controller, final int index) {
    return new MyRecordButtonPanel(service, controller, exercise, index, this);
  }

  private class MyRecordButtonPanel extends RecordButtonPanel {
    private final Exercise exercise;
    public MyRecordButtonPanel(LangTestDatabaseAsync service, ExerciseController controller, Exercise exercise, int index,Panel outerPanel) {
      super(service, controller, exercise, null, index);
      this.exercise = exercise;
    }

   @Override
    protected RecordButton makeRecordButton(ExerciseController controller, final RecordButtonPanel outer) {
      return new RecordButton(controller.getRecordTimeout()) {
        @Override
        protected void stopRecording() {
          outer.stopRecording();
        }

        @Override
        protected void startRecording() {
          outer.startRecording();
        }

        @Override
        protected void showRecording() {
          outer.showRecording();
        }

        @Override
        protected void showStopped() {
          outer.showStopped();
        }

        @Override
        public void doClick() {
          super.doClick();
        }

        @Override
        protected HandlerRegistration addKeyHandler() {
          return Event.addNativePreviewHandler(new
                                                 Event.NativePreviewHandler() {

                                                   @Override
                                                   public void onPreviewNativeEvent(Event.NativePreviewEvent event) {
                                                     NativeEvent ne = event.getNativeEvent();


                                                     if (ne.getKeyCode() == KeyCodes.KEY_ENTER &&
                                                       //typeInt == 512 &&
                                                       "[object KeyboardEvent]".equals(ne.getString())) {

                                                       int typeInt = event.getTypeInt();
                                                       boolean keyDown = typeInt == 128;
                                                       boolean keyUp = typeInt == 512;

                                               /*        System.out.println(new Date() + " : Click handler : Got " + event + " type int " +
                                                         event.getTypeInt() + " assoc " + event.getAssociatedType() +
                                                         " native " + event.getNativeEvent() + " source " + event.getSource());*/

                                                       if (keyDown && !keyIsDown) {
                                                         keyIsDown = true;
                                                         doClick();
                                                       }
                                                       else if (keyUp && keyIsDown) {
                                                         keyIsDown = false;
                                                         doClick();
                                                       }
                                                     }
                                                   }
                                                 });
        }
      };
   }

    @Override
    protected void layoutRecordButton() {}

    @Override
    protected Anchor makeRecordButton() {
      recordButton = new ImageAnchor();
      recordButton.setResource(enterImage);
      recordButton.setHeight("48px");
      //recordButton.setWidth("100%");
      return recordButton;
    }

    @Override
    public void showRecording() {
      recordButton.setResource(listeningImage);
    }

    @Override
    public void showStopped() {
      recordButton.setResource(waitingForResponseImage);
      onUnload();
    }

    /**
     * @see mitll.langtest.client.recorder.RecordButtonPanel#stopRecording()
     * @param result
     * @param questionState
     * @param outer
     */
    @Override
    protected void receivedAudioAnswer(AudioAnswer result, ExerciseQuestionState questionState, Panel outer) {
      double score = result.score;
      boolean correct = score > 0.6;
      recordButton.setResource(correct ? correctImage : incorrectImage);
      if (recoOutput != null) {
        recoOutput.setText("Heard: "+result.decodeOutput);
      }
      Timer t = new Timer() {
        @Override
        public void run() {
          controller.loadNextExercise(exercise);
        }
      };

      // Schedule the timer to run once in 1 seconds.
      t.schedule(recoOutput != null ? LONG_DELAY_MILLIS : correct ? DELAY_MILLIS : DELAY_MILLIS_LONG);

    }

    @Override
    protected void receivedAudioFailure() {
      recordButton.setResource(enterImage);
    }
  }

  @Override
  protected void onUnload() {
    for (MyRecordButtonPanel answers : answerWidgets) {
      answers.onUnload();
    }
  }
}
