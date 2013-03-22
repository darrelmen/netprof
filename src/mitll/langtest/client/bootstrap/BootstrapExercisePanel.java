package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.PageHeader;
import com.github.gwtbootstrap.client.ui.Paragraph;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.media.client.Audio;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.BrowserCheck;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExerciseQuestionState;
import mitll.langtest.client.recorder.RecordButton;
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
  private static final int LONG_DELAY_MILLIS = 3500;
  private static final int DELAY_MILLIS = 1250;
  private static final int DELAY_MILLIS_LONG = 2500;
  private static final double CORRECT_THRESHOLD = 0.6;
 // private static final String TIMES_HELP_SHOWN = "TimesHelpShown";
  private static final String FEEDBACK_TIMES_SHOWN = "FeedbackTimesShown";
  private static final int PERIOD_MILLIS = 500;
  public static final int MAX_INTRO_FEEBACK_COUNT = 5;
  private List<MyRecordButtonPanel> answerWidgets = new ArrayList<MyRecordButtonPanel>();
  private Audio mistakeAudio;
  private Storage stockStore = null;

  private Image waitingForResponseImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "animated_progress48.gif"));
  private Image recordImage1 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-3.png"));
  private Image recordImage2 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-4.png"));
  private Image correctImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "checkmark48.png"));
  private Image incorrectImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "redx48.png"));
  private Image enterImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "space.png"));
  private Heading recoOutput;
  private boolean keyIsDown;
  private boolean isDemoMode;
  private BrowserCheck browserCheck = new BrowserCheck().checkForCompatibleBrowser();
  private int feedback = 0;
  private Timer t;

  public BootstrapExercisePanel(final Exercise e, final LangTestDatabaseAsync service,
                                final ExerciseController controller) {
    setStyleName("exerciseBackground");
   // int times = 0;
    stockStore = Storage.getLocalStorageIfSupported();
    if (stockStore != null) {
     // times = getCookieValue(TIMES_HELP_SHOWN);
      feedback = getCookieValue(FEEDBACK_TIMES_SHOWN);
    }

    FluidRow fluidRow = new FluidRow();
    add(fluidRow);
    fluidRow.add(new Column(12, getQuestionContent(e)));
    addQuestions(e, service, controller, 1/*, times == 0*/);
  }

  private int getCookieValue(String key) {
    int times = 0;
    String timesHelpShown = stockStore.getItem(key);
    if (timesHelpShown == null) {
      stockStore.setItem(key,"1");
    }
    else {
      try {
        times = Integer.parseInt(timesHelpShown);
      } catch (NumberFormatException e1) {
        times = 10;
      }
    }
    return times;
  }

  private void incrCookie(String key) {
    int cookieValue = getCookieValue(key);
    stockStore.setItem(key,""+(cookieValue+1));
  }

  private Widget getQuestionContent(Exercise e) {
    PageHeader hero = new PageHeader();
    hero.addStyleName("hero-unit");
    hero.setText(e.getContent());
    return hero;
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
   * @paramx showHelp
   */
  private void addQuestions(Exercise e, LangTestDatabaseAsync service, ExerciseController controller, int questionNumber/*, boolean showHelp*/) {
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

    isDemoMode = controller.isDemoMode();
    FluidRow row2 = new FluidRow();
    add(row2);
    Paragraph paragraph2 = new Paragraph();
    paragraph2.addStyleName("alignCenter");

    row2.add(new Column(12, paragraph2));
    recoOutput = new Heading(3);
    paragraph2.add(recoOutput);
  }

  protected MyRecordButtonPanel getAnswerWidget(final Exercise exercise, LangTestDatabaseAsync service, ExerciseController controller, final int index) {
    return new MyRecordButtonPanel(service, controller, exercise, index);
  }

  private class MyRecordButtonPanel extends RecordButtonPanel {
    private final Exercise exercise;
    public MyRecordButtonPanel(LangTestDatabaseAsync service, ExerciseController controller, Exercise exercise, int index) {
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

                                                     if (//ne.getKeyCode() == KeyCodes.KEY_ENTER &&
                                                       "[object KeyboardEvent]".equals(ne.getString())) {

                          /*                             System.out.println(new java.util.Date() + " : key handler : Got " + event + " type int " +
                                                         event.getTypeInt() + " assoc " + event.getAssociatedType() +
                                                         " native " + event.getNativeEvent() + " source " + event.getSource() + " " + ne.getCharCode());
*/
                                                       int typeInt = event.getTypeInt();
                                                       //        boolean keyDown = typeInt == 128;
                                                       boolean keyPress = typeInt == 256;
                                                       boolean keyUp = typeInt == 512;
                                                       boolean isSpace = ne.getCharCode() == 32;

                                                       if (keyPress && !keyIsDown && isSpace) {
                                                         keyIsDown = true;
                                                         doClick();
                                                       } else if (keyUp && keyIsDown) {
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
      return recordButton;
    }

    private boolean first = true;
    @Override
    public void showRecording() {
      recordButton.setResource(recordImage1);

      flipImage();
    }

    private void flipImage() {
      t = new Timer() {
        @Override
        public void run() {
          recordButton.setResource(first ? recordImage2 : recordImage1);
          first = !first;
        }
      };
      t.scheduleRepeating(PERIOD_MILLIS);
    }

    @Override
    public void showStopped() {
      t.cancel();
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
      boolean correct = result.score > CORRECT_THRESHOLD;
      recordButton.setResource(correct ? correctImage : incorrectImage);
      boolean hasRefAudio = exercise.getRefAudio() != null;

      if (result.validity == AudioAnswer.Validity.TOO_SHORT) {
        showPopup("Audio was too short. Please record again.");
        nextAfterDelay(correct);
      }
      else if (result.validity == AudioAnswer.Validity.TOO_QUIET) {
        showPopup("Audio was too quiet. Please check your mic and record again.");
        nextAfterDelay(correct);
      }
      else if (correct) {
        playCorrect();
        if (feedback < MAX_INTRO_FEEBACK_COUNT) {
          String correctPrompt = "Correct! It's: " + exercise.getRefSentence();
          recoOutput.setText(correctPrompt);
          incrCookie(FEEDBACK_TIMES_SHOWN);
        }
      } else {
        if (hasRefAudio) {
          play(exercise.getRefAudio());
        }
        else {
          playIncorrect();
        }
        String correctPrompt = "Correct Answer: " + exercise.getRefSentence() +
          (exercise.getTranslitSentence().length() > 0 ? " (" + exercise.getTranslitSentence() + ")" : "");

        if (isDemoMode) {
          correctPrompt = "Heard: " + result.decodeOutput + "<p>" + correctPrompt;
        }
        recoOutput.setText(correctPrompt);

        if (hasRefAudio) {
          waitForAudioToFinish();
        }
      }
      if (correct || !hasRefAudio) {
        nextAfterDelay(correct);
      }
    }

    private void nextAfterDelay(boolean correct) {
      // Schedule the timer to run once in 1 seconds.
      Timer t = new Timer() {
        @Override
        public void run() {
          controller.loadNextExercise(exercise);
        }
      };
      t.schedule(isDemoMode ? LONG_DELAY_MILLIS : correct ? DELAY_MILLIS : DELAY_MILLIS_LONG);
    }

    private void waitForAudioToFinish() {
      // Schedule the timer to run once in 1 seconds.
      Timer t = new Timer() {
        @Override
        public void run() {
          if (mistakeAudio.hasEnded()) {
            // Schedule the timer to run once in 1 seconds.
            Timer t = new Timer() {
              @Override
              public void run() {
                controller.loadNextExercise(exercise);
              }
            };
            t.schedule(isDemoMode ? LONG_DELAY_MILLIS : DELAY_MILLIS);
          }
          else {
            waitForAudioToFinish();
          }
        }
      };
      t.schedule(500);
    }
    @Override
    protected void receivedAudioFailure() {
      recordButton.setResource(enterImage);
    }
  }

  private void showPopup(String html) {
    final PopupPanel pleaseWait = new DecoratedPopupPanel();
    pleaseWait.setAutoHideEnabled(true);
    pleaseWait.add(new HTML(html));
    pleaseWait.center();

    Timer t = new Timer() {
      @Override
      public void run() {
        pleaseWait.hide();
      }
    };
    t.schedule(2000);
  }

  @Override
  protected void onUnload() {
    for (MyRecordButtonPanel answers : answerWidgets) {
      answers.onUnload();
    }
  }

  private void playCorrect() {
    play("langtest/sounds/correct4.wav","langtest/sounds/correct4.mp3");
  }

  private void playIncorrect() {
    play("langtest/sounds/incorrect1.wav","langtest/sounds/incorrect1.mp3");
  }

  private void play(String mp3Audio) {
    play(mp3Audio.replace(".mp3",".wav"),mp3Audio.replace(".wav",".mp3"));
  }

  private void play(String openAudio, String mp3Audio) {
    mistakeAudio = Audio.createIfSupported();
    if (mistakeAudio == null) {
      Window.alert("audio playback not supported.");
    } else {
      playAudio(openAudio, mp3Audio);
      mistakeAudio.play();
    }
  }

  private void playAudio(String openAudio,String mp3Audio) {
    if (browserCheck.isFirefox()) {
      mistakeAudio.setSrc(openAudio);
    } else {
      mistakeAudio.setSrc(mp3Audio);
    }
  }
}
