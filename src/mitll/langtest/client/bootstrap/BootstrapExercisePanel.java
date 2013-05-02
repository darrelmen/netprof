package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.Paragraph;
import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.github.gwtbootstrap.client.ui.base.ProgressBarBase;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SimplePanel;
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
  private static final int DELAY_MILLIS = 1200;//1250;
  private static final int DELAY_MILLIS_LONG = 3000;
  private Column scoreFeedbackColumn;
 // private static final String TIMES_HELP_SHOWN = "TimesHelpShown";
  private static final String FEEDBACK_TIMES_SHOWN = "FeedbackTimesShown";
  private static final int PERIOD_MILLIS = 500;
  private static final int MAX_INTRO_FEEBACK_COUNT = 5;
  private static final int KEY_PRESS = 256;
  private static final int KEY_UP = 512;
  private static final int SPACE_CHAR = 32;
  private static final int HIDE_DELAY = 2500;

  private final AudioHelper audioHelper = new AudioHelper();
  private List<MyRecordButtonPanel> answerWidgets = new ArrayList<MyRecordButtonPanel>();
  private Storage stockStore = null;

  private Image waitingForResponseImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "animated_progress48.gif"));
  private Image recordImage1 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-3.png"));
  private Image recordImage2 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-4.png"));
  private Image correctImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "checkmark48.png"));
  private Image incorrectImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "redx48.png"));
  private Image enterImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "blueSpaceBar.png"));//"space48_2.png"));
  private static final String HELP_IMAGE = LangTest.LANGTEST_IMAGES + "/helpIconBlue.png";

  private Heading recoOutput;
  private boolean keyIsDown;
  private boolean isDemoMode;
  private int feedback = 0;
  private Timer t;
  private ProgressBar scoreFeedback = new ProgressBar();


  /**
   * @see mitll.langtest.client.flashcard.FlashcardExercisePanelFactory#getExercisePanel(mitll.langtest.shared.Exercise)
   * @param e
   * @param service
   * @param controller
   */
  public BootstrapExercisePanel(final Exercise e, final LangTestDatabaseAsync service,
                                final ExerciseController controller) {
    setStyleName("exerciseBackground");
    addStyleName("cardBorder");
   // int times = 0;
    stockStore = Storage.getLocalStorageIfSupported();
    if (stockStore != null) {
     // times = getCookieValue(TIMES_HELP_SHOWN);
      feedback = getCookieValue(FEEDBACK_TIMES_SHOWN);
    }

    FluidRow fluidRow = new FluidRow();
    FlowPanel helpRow;
    add(helpRow = new FlowPanel());
    helpRow.addStyleName("floatRight");
    helpRow.addStyleName("helpPadding");

    // add help image on right side
    Image image = new Image(HELP_IMAGE);

    image.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        controller.showFlashHelp();
      }
    });
    helpRow.add(image);

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
    String stimulus = e.getEnglishSentence();
    if (stimulus == null) stimulus = e.getContent();
    Widget hero = new Heading(1, stimulus);
    hero.addStyleName("cardText");
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

    FluidRow recoOutputRow = new FluidRow();
    add(recoOutputRow);
    Paragraph paragraph2 = new Paragraph();
    paragraph2.addStyleName("alignCenter");

    isDemoMode = controller.isDemoMode();
    recoOutputRow.add(new Column(12, paragraph2));
    recoOutput = new Heading(3,"Answer");
    recoOutput.addStyleName("cardHiddenText");
    DOM.setStyleAttribute(recoOutput.getElement(), "color", "#e8eaea");

    paragraph2.add(recoOutput);

    FluidRow feedbackRow = new FluidRow();
    add(feedbackRow);
    SimplePanel widgets = new SimplePanel();
    widgets.setHeight("40px");
    scoreFeedbackColumn = new Column(6, 3, widgets);
    feedbackRow.add(scoreFeedbackColumn);
  }
  protected MyRecordButtonPanel getAnswerWidget(final Exercise exercise, LangTestDatabaseAsync service, ExerciseController controller, final int index) {
    return new MyRecordButtonPanel(service, controller, exercise, index);
  }

  public void grabFocus() {
    for (MyRecordButtonPanel widget : answerWidgets) {
      System.out.println("Grab focus!!!");
      widget.getRecordButton().setFocus(true);
    }
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

                                                     if ("[object KeyboardEvent]".equals(ne.getString())) {

                          /*                             System.out.println(new java.util.Date() + " : key handler : Got " + event + " type int " +
                                                         event.getTypeInt() + " assoc " + event.getAssociatedType() +
                                                         " native " + event.getNativeEvent() + " source " + event.getSource() + " " + ne.getCharCode());
*/
                                                       int typeInt = event.getTypeInt();
                                                       boolean keyPress = typeInt == KEY_PRESS;
                                                       boolean isSpace = ne.getCharCode() == SPACE_CHAR;

                                                       if (keyPress && !keyIsDown && isSpace) {
                                                         keyIsDown = true;
                                                         doClick();
                                                       } else {
                                                         boolean keyUp = typeInt == KEY_UP;
                                                         if (keyUp && keyIsDown) {
                                                           keyIsDown = false;
                                                           doClick();
                                                         }
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
      recordButton = new ImageAnchor() {
        @Override
        protected void onLoad() {
          super.onLoad();
          recordButton.setFocus(true);
        }
      };
      recordButton.setResource(enterImage);
      recordButton.setFocus(true);
      recordButton.addFocusHandler(new FocusHandler() {
        @Override
        public void onFocus(FocusEvent event) {
          System.out.println("\n\n\n record button got the focus! -------------- \n\n\n");
        }
      });

      recordButton.addKeyDownHandler(new KeyDownHandler() {
        @Override
        public void onKeyDown(KeyDownEvent event) {
          System.out.println("\n\n\n record button got key down : " +event+
            "-------------- \n\n\n");
        }
      });

      return recordButton;
    }

    private boolean first = true;
    @Override
    public void showRecording() {
      recordButton.setResource(recordImage1);
      recordButton.setHeight("112px");

      flipImage();
    }

    private void flipImage() {
      t = new Timer() {
        @Override
        public void run() {
          recordButton.setResource(first ? recordImage2 : recordImage1);
          recordButton.setHeight("112px");
          first = !first;
        }
      };
      t.scheduleRepeating(PERIOD_MILLIS);
    }

    @Override
    public void showStopped() {
      t.cancel();
      recordButton.setResource(waitingForResponseImage);
      recordButton.setHeight("112px");

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
      boolean correct = result.isCorrect();
      double score = result.getScore();
      System.out.println("answer correct = " + correct + " pron score : " + score);

      recordButton.setResource(correct ? correctImage : incorrectImage);
      recordButton.setHeight("112px");

      boolean hasRefAudio = exercise.getRefAudio() != null;

      if (result.validity != AudioAnswer.Validity.OK) {
        showPopup(result.validity.getPrompt());
        nextAfterDelay(correct);
      }
      else if (correct) {
        showPronScoreFeedback(score);

        audioHelper.playCorrect();
        if (feedback < MAX_INTRO_FEEBACK_COUNT) {
          String correctPrompt = "Correct! It's: " + exercise.getRefSentence();
          recoOutput.setText(correctPrompt);
          DOM.setStyleAttribute(recoOutput.getElement(), "color", "#000000");
          incrCookie(FEEDBACK_TIMES_SHOWN);
        }
      } else {   // incorrect!!
        showPronScoreFeedback(score);

        if (hasRefAudio) {
          audioHelper.play(exercise.getRefAudio());
        } else {
          audioHelper.playIncorrect();
        }
        String correctPrompt = "Answer: " + exercise.getRefSentence() +
          (exercise.getTranslitSentence().length() > 0 ? " (" + exercise.getTranslitSentence() + ")" : "");

        if (isDemoMode) {
          correctPrompt = "Heard: " + result.decodeOutput + "<p>" + correctPrompt;
        }
        recoOutput.setText(correctPrompt);
        DOM.setStyleAttribute(recoOutput.getElement(), "color", "#000000");

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
          if (audioHelper.hasEnded()) {
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

  private void showPronScoreFeedback(double score) {
    if (score < 0) score = 0;
    double percent = 100 * score;

    scoreFeedbackColumn.clear();
    scoreFeedbackColumn.add(scoreFeedback);

    int percent1 = (int) percent;
    scoreFeedback.setPercent(percent1  < 40 ? 40 : percent1);   // just so the words will show up

    scoreFeedback.setText("Pronunciation score " + (int) percent + "%");
    scoreFeedback.setVisible(true);
    scoreFeedback.setColor(
      score > 0.8 ? ProgressBarBase.Color.SUCCESS :
      score > 0.6 ? ProgressBarBase.Color.DEFAULT :
        score > 0.4 ? ProgressBarBase.Color.WARNING : ProgressBarBase.Color.DANGER);
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
    t.schedule(HIDE_DELAY);
  }

  @Override
  protected void onUnload() {
    for (MyRecordButtonPanel answers : answerWidgets) {
      answers.onUnload();
    }
  }
}
