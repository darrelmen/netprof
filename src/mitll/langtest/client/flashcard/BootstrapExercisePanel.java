package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.ProgressBarBase;
import com.github.gwtbootstrap.client.ui.constants.ToggleType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PopupHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.recorder.FlashcardRecordButton;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.recorder.RecordButtonPanel;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.flashcard.CorrectAndScore;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 3/6/13
 * Time: 3:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class BootstrapExercisePanel extends FlashcardPanel implements AudioAnswerListener {
  private Heading recoOutput;
  private static final int DELAY_MILLIS_LONG = 3000;
  private static final int LONG_DELAY_MILLIS = 3500;
  private static final int DELAY_CHARACTERS = 40;
  private static final int HIDE_DELAY = 2500;

  private static final boolean NEXT_ON_BAD_AUDIO = false;
  private static final String FEEDBACK = "PLAY ON MISTAKE";

  /**
   *
   *
   * @param e
   * @param service
   * @param controller
   * @param soundFeedback
   * @param endListener
   * @param instance
   * @param exerciseList
   * @see StatsFlashcardFactory.StatsPracticePanel#StatsPracticePanel
   *
   */
  public BootstrapExercisePanel(final CommonExercise e, final LangTestDatabaseAsync service,
                                final ExerciseController controller, boolean addKeyBinding,
                                final ControlState controlState,
                                StatsFlashcardFactory.MySoundFeedback soundFeedback,
                                SoundFeedback.EndListener endListener,
                                String instance, ListInterface exerciseList) {
    super(e, service, controller, addKeyBinding, controlState, soundFeedback, endListener, instance, exerciseList);
  }

  /**
   * @see #getRightColumn(mitll.langtest.client.flashcard.ControlState)
   * @param controlState
   * @return
   */
  @Override
  protected ControlGroup getFeedbackGroup(final ControlState controlState) {
    ControlGroup group = new ControlGroup(FEEDBACK);
    ButtonToolbar w = new ButtonToolbar();
    group.add(w);
    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.setToggle(ToggleType.RADIO);
    w.add(buttonGroup);

    Button onButton = makeGroupButton(buttonGroup, ON);

    onButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        controlState.setAudioFeedbackOn(true);
        //System.out.println("now on " + controlState);
      }
    });
    onButton.setActive(controlState.isAudioFeedbackOn());

    Button offButton = makeGroupButton(buttonGroup, OFF);

    offButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        controlState.setAudioFeedbackOn(false);
        //System.out.println("now off " + controlState);
      }
    });
    offButton.setActive(!controlState.isAudioFeedbackOn());

    return group;
  }

  private Button makeGroupButton(ButtonGroup buttonGroup,String title) {
    Button onButton = new Button(title);
    onButton.getElement().setId(FEEDBACK+"_"+title);
    controller.register(onButton, exercise.getID());
    buttonGroup.add(onButton);
    return onButton;
  }

  private DivWidget scoreFeedbackRow;

  /**
   * Three rows below the stimulus word/expression:<p></p>
   * record space bar image <br></br>
   * reco feedback - whether the recorded audio was correct/incorrect, etc.  <br></br>
   * score feedback - pron score
   *
   * @param e
   * @param service
   * @param controller used in subclasses for audio control
   * @param toAddTo
   * @see #BootstrapExercisePanel
   */
  @Override
  protected void addRecordingAndFeedbackWidgets(CommonExercise e, LangTestDatabaseAsync service, ExerciseController controller,
                                      Panel toAddTo) {
    // add answer widget to do the recording
    Widget answerAndRecordButtonRow = getAnswerAndRecordButtonRow(e, service, controller);
    toAddTo.add(answerAndRecordButtonRow);

    if (controller.getProps().showFlashcardAnswer()) {
      toAddTo.add(getRecoOutputRow());
    }
    scoreFeedbackRow = new DivWidget();
    scoreFeedbackRow.addStyleName("bottomFiveMargin");
    scoreFeedbackRow.setHeight("52px");
    toAddTo.add(scoreFeedbackRow);
  }

  private RecordButtonPanel answerWidget;
  private Widget button;
  private RecordButton realRecordButton;

  /**
   *
   * @param e
   * @param service
   * @param controller
   * @return
   * @see #addRecordingAndFeedbackWidgets(mitll.langtest.shared.CommonExercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, com.google.gwt.user.client.ui.Panel)
   */
  private Widget getAnswerAndRecordButtonRow(CommonExercise e, LangTestDatabaseAsync service, ExerciseController controller) {
    //System.out.println("BootstrapExercisePanel.getAnswerAndRecordButtonRow = " + instance);
    RecordButtonPanel answerWidget = getAnswerWidget(e, service, controller, addKeyBinding, instance);
    this.answerWidget = answerWidget;
    button = answerWidget.getRecordButton();
    realRecordButton = answerWidget.getRealRecordButton();

    return getRecordButtonRow(button);
  }

  @Override
  protected void setClickToFlipHeight(DivWidget clickToFlipContainer) {
    clickToFlipContainer.setHeight("12px");
  }

  @Override
  protected void setMarginTop(HTML clickToFlip, Widget icon) {
    icon.getElement().getStyle().setMarginTop(2, Style.Unit.PX);
  }

  /**
   * Center align the record button image.
   *
   * @param recordButton
   * @return
   * @see #getAnswerAndRecordButtonRow(mitll.langtest.shared.CommonExercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController)
   * @see BootstrapExercisePanel#addRecordingAndFeedbackWidgets(mitll.langtest.shared.CommonExercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, com.google.gwt.user.client.ui.Panel)
   */
  Panel getRecordButtonRow(Widget recordButton) {
    Panel recordButtonRow = getCenteredWrapper(recordButton);
    recordButtonRow.getElement().setId("recordButtonRow");

    recordButtonRow.addStyleName("leftTenMargin");
    recordButtonRow.addStyleName("rightTenMargin");
    recordButtonRow.getElement().getStyle().setMarginRight(10, Style.Unit.PX);

    return recordButtonRow;
  }

  private Panel getCenteredWrapper(Widget recordButton) {
    Panel recordButtonRow = new FluidRow();
    Paragraph recordButtonContainer = new Paragraph();
    recordButtonContainer.addStyleName("alignCenter");
    recordButtonContainer.add(recordButton);
    recordButton.addStyleName("alignCenter");
    recordButtonRow.add(new Column(12, recordButtonContainer));
    return recordButtonRow;
  }

  /**
   * Center align the text feedback (correct/incorrect)
   *
   * @return
   */
  private Panel getRecoOutputRow() {
    recoOutput = new Heading(3, "");
    recoOutput.addStyleName("cardHiddenText2");   // same color as background so text takes up space but is invisible
    recoOutput.getElement().getStyle().setColor("#ffffff");

    Panel recoOutputRow = new FluidRow();
    Panel recoOutputContainer = new Paragraph();
    recoOutputContainer.addStyleName("alignCenter");

    recoOutputRow.add(new Column(12, recoOutputContainer));
    recoOutputContainer.add(recoOutput);
    recoOutputRow.getElement().setId("recoOutputRow");

    return recoOutputRow;
  }

  /**
   * @param exercise
   * @param service
   * @param controller
   * @param addKeyBinding
   * @param instance
   * @return
   * @see #getAnswerAndRecordButtonRow(mitll.langtest.shared.CommonExercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController)
   */
  RecordButtonPanel getAnswerWidget(final CommonExercise exercise, LangTestDatabaseAsync service,
                                    ExerciseController controller, final boolean addKeyBinding, String instance) {
    return new FlashcardRecordButtonPanel(this, service, controller, exercise, 1, instance) {
      @Override
      protected RecordButton makeRecordButton(final ExerciseController controller, String buttonTitle) {
       // System.out.println("makeRecordButton : using " + instance);
        final FlashcardRecordButton widgets = new FlashcardRecordButton(controller.getRecordTimeout(), this, true,
            addKeyBinding, controller,
            BootstrapExercisePanel.this.instance) {
          @Override
          protected void start() {
            controller.logEvent(this, "AVP_RecordButton", exercise, "Start_Recording");
            super.start();
            recordingStarted();
          }

          @Override
          public void stop() {
            controller.logEvent(this, "AVP_RecordButton", exercise, "Stop_Recording");
            super.stop();
          }

          @Override
          protected void gotLeftArrow() {
            exerciseList.loadPrev();
          }

          @Override
          protected void gotRightArrow() {
            if (!exerciseList.isPendingReq()) {
              gotClickOnNext();
            }
          }

          @Override
          protected boolean shouldIgnoreKeyPress() {
            return super.shouldIgnoreKeyPress() || otherReasonToIgnoreKeyPress();
          }
        };

        // without this, the arrow keys may go to the chapter selector
        Scheduler.get().scheduleDeferred(new Command() {
          public void execute() {
            widgets.setFocus(true);
          }
        });
        return widgets;
      }
    };
  }

  protected void recordingStarted() {}

  /**
   * Show progress bar with score percentage, colored by score.
   * Note it has to be wide enough to hold the text "pronunciation score xxx %"
   *
   * @param score
   * @see
   */
  void showPronScoreFeedback(double score) {
    scoreFeedbackRow.add(showScoreFeedback(score));
  }


  /**
   * @param score
   * @seex #showCRTFeedback(Double, mitll.langtest.client.sound.SoundFeedback, String, boolean)
   * @paramx centerVertically
   * @paramx useShortWidth
   * @see BootstrapExercisePanel#showPronScoreFeedback(double)
   */
  private ProgressBar showScoreFeedback(double score) {
    if (score < 0) score = 0;
    double percent = 100 * score;

    ProgressBar scoreFeedback = new ProgressBar();
    int percent1 = (int) percent;
    scoreFeedback.setPercent(percent1 < 40 ? 40 : percent1);   // just so the words will show up

    scoreFeedback.setText("Score " + (int) percent + "%");
    scoreFeedback.setVisible(true);
    scoreFeedback.setColor(
      score > 0.8 ? ProgressBarBase.Color.SUCCESS :
        score > 0.6 ? ProgressBarBase.Color.DEFAULT :
          score > 0.4 ? ProgressBarBase.Color.WARNING : ProgressBarBase.Color.DANGER);

    return scoreFeedback;
  }

  void clearFeedback() {  scoreFeedbackRow.clear(); }
  private Heading getRecoOutput() {
    return recoOutput;
  }

  /**
   * @see mitll.langtest.client.recorder.RecordButtonPanel#receivedAudioAnswer
   * @param result
   */
  public void receivedAudioAnswer(final AudioAnswer result) {
    String path = exercise.getRefAudio() != null ? exercise.getRefAudio() : exercise.getSlowAudioRef();
    final boolean hasRefAudio = path != null;
    boolean correct = result.isCorrect();
    final double score = result.getScore();

    boolean badAudioRecording = result.getValidity() != AudioAnswer.Validity.OK;
    System.out.println("BootstrapExercisePanel.receivedAudioAnswer: correct " + correct + " pron score : " + score +
        " has ref " + hasRefAudio + " bad audio " + badAudioRecording + " result " + result);

    String feedback = "";
    if (badAudioRecording) {
      controller.logEvent(button, "Button", exercise.getID(), "bad recording");
      putBackText();
      if (!realRecordButton.checkAndShowTooLoud(result.getValidity())) {
        System.out.println("\n\n\nreceivedAudioAnswer: show popup for " + result.getValidity());
        //new Exception().printStackTrace();
        showPopup(result.getValidity().getPrompt(), button);
      }
      initRecordButton();
      clearFeedback();
    } else if (correct) {
      controller.logEvent(button, "Button", exercise.getID(), "correct response - score " + Math.round(score * 100f));

      showCorrectFeedback(score);
    } else {   // incorrect!!
      controller.logEvent(button, "Button", exercise.getID(), "incorrect response - score " + Math.round(score * 100f));

      feedback = showIncorrectFeedback(result, score, hasRefAudio);
    }
    if (!badAudioRecording && (correct || !hasRefAudio)) {
      System.out.println("\treceivedAudioAnswer: correct " + correct + " pron score : " + score + " has ref " + hasRefAudio);
      nextAfterDelay(correct, feedback);
    }
  }

  /**
   * @param html
   * @see #receivedAudioAnswer
   */
  private void showPopup(String html, Widget button) { new PopupHelper().showPopup(html,button,HIDE_DELAY);  }

  /**
   *
   * @param score
   * @see #receivedAudioAnswer(mitll.langtest.shared.AudioAnswer)
   */
  private void showCorrectFeedback(double score) {
    showPronScoreFeedback(score);
    showOtherText();
    getSoundFeedback().queueSong(SoundFeedback.CORRECT);
  }

  /**
   * If there's reference audio, play it and wait for it to finish.
   * What to do when the user says the wrong word.
   *
   * @param result
   * @param score
   * @param hasRefAudio
   * @see #receivedAudioAnswer
   */
  private String showIncorrectFeedback(AudioAnswer result, double score, boolean hasRefAudio) {
    if (result.isSaidAnswer()) { // if they said the right answer, but poorly, show pron score
      showPronScoreFeedback(score);
    }
    showOtherText();

    System.out.println("showIncorrectFeedback : result " + result + " score " + score + " has ref " + hasRefAudio);

    String correctPrompt = getCorrectDisplay();
    if (hasRefAudio) {
      if (controlState.isAudioFeedbackOn()) {
        String path = getRefAudioToPlay();
        if (path == null) {
          playIncorrect(); // this should never happen
        } else if (!preventFutureTimerUse) {
          playRefAndGoToNext(path);
        }
      } else {
        playIncorrect();
        int delay = 1000;
        goToNextAfter(delay);
      }
    } else {
      tryAgain();
    }

    if (controller.getProps().isDemoMode()) {
      correctPrompt = "Heard: " + result.getDecodeOutput() + "<p>" + correctPrompt;
      Heading recoOutput = getRecoOutput();
      if (recoOutput != null && controlState.isAudioFeedbackOn()) {
        recoOutput.setText(correctPrompt);
        recoOutput.getElement().getStyle().setColor("#000000");
      }
    }

    return correctPrompt;
  }

  private void showOtherText() {
    if (controlState.isEnglish()) showForeign();
    else if (controlState.isForeign()) showEnglish();
  }

  private void putBackText() {
    if (controlState.isEnglish()) showEnglish();
    else if (controlState.isForeign()) showForeign();
  }

  private void playIncorrect() {
    getSoundFeedback().queueSong(SoundFeedback.INCORRECT);
  }

  /**
   * @see #showIncorrectFeedback(mitll.langtest.shared.AudioAnswer, double, boolean)
   * @paramx correctPrompt
   * @param path
   */
  private void playRefAndGoToNext(String path) {
    getSoundFeedback().queueSong(getPath(path), new SoundFeedback.EndListener() {
      @Override
      public void songStarted() {
        Widget widget = isSiteEnglish() ? english : foreign;
        widget.addStyleName(PLAYING_AUDIO_HIGHLIGHT);
        endListener.songStarted();
      }

      @Override
      public void songEnded() {
        endListener.songEnded();
       // removePlayingHighlight(textWidget);
        loadNext();
      }
    });
  }

  /**
   * @see #cancelTimer()
   * @see mitll.langtest.client.flashcard.StatsFlashcardFactory.StatsPracticePanel#recordingStarted()
   */
  protected void removePlayingHighlight() {
    removePlayingHighlight(isSiteEnglish() ? english : foreign);
  }

  /**
   * @see #showIncorrectFeedback(mitll.langtest.shared.AudioAnswer, double, boolean)
   */
  private void tryAgain() {
    playIncorrect();

    Timer t = new Timer() {
      @Override
      public void run() {
        initRecordButton();
      }
    };
    int incorrectDelay = DELAY_MILLIS_LONG;
    t.schedule(incorrectDelay);
  }

  private void goToNextAfter(int delay) {
    loadNextOnTimer(controller.getProps().isDemoMode() ? LONG_DELAY_MILLIS : delay);
  }

  private int getFeedbackLengthProportionalDelay(String feedback) {
    int mult1 = feedback.length() / DELAY_CHARACTERS;
    int mult = Math.max(3, mult1);
    return mult * DELAY_MILLIS;
  }

  private String getCorrectDisplay() {
    String refSentence = exercise.getForeignLanguage();
    String translit = exercise.getTransliteration().length() > 0 ? "<br/>(" + exercise.getTransliteration() + ")" : "";
    return refSentence + translit;
  }

  private Timer currentTimer = null;
  /**
   * @param correct
   * @param feedback
   * @see #receivedAudioAnswer(mitll.langtest.shared.AudioAnswer)
   */
  protected void nextAfterDelay(boolean correct, String feedback) {
    if (NEXT_ON_BAD_AUDIO) {
      System.out.println("doing nextAfterDelay : correct " + correct + " feedback " + feedback);
      // Schedule the timer to run once in 1 seconds.
      Timer t = new Timer() {
        @Override
        public void run() {
          loadNext();
        }
      };
      int incorrectDelay = DELAY_MILLIS_LONG;
      if (!feedback.isEmpty()) {
        int delay = getFeedbackLengthProportionalDelay(feedback);
        incorrectDelay += delay;
        System.out.println("nextAfterDelay Delay is " + incorrectDelay + " len " + feedback.length());
      }
      int delayMillis = controller.getProps().isDemoMode() ? LONG_DELAY_MILLIS : correct ? DELAY_MILLIS : incorrectDelay;
      if (correct && (controlState.isEnglish() || controlState.isForeign())) {
        delayMillis *= 2;
      }
      t.schedule(delayMillis);
    } else {
      System.out.println("doing nextAfterDelay : correct " + correct + " feedback " + feedback);

      if (correct) {
        // go to next item
        loadNextOnTimer(100);//DELAY_MILLIS);
      } else {
        initRecordButton();
        clearFeedback();
      }
    }
  }

  /**
   *
   * @param delay
   * @see #goToNextAfter(int)
   * @see #nextAfterDelay(boolean, String)
   * @see mitll.langtest.client.flashcard.StatsFlashcardFactory.StatsPracticePanel#nextAfterDelay(boolean, String)
   */
  protected void loadNextOnTimer(final int delay) {
    //System.out.println("loadNextOnTimer ----> load next on " + delay);

    if (!preventFutureTimerUse) {
      //if (delay > 100) {
      //  System.out.println("loadNextOnTimer ----> load next on " + delay);
     // }
      Timer t = new Timer() {
        @Override
        public void run() {
          currentTimer = null;
          loadNext();
        }
      };
      currentTimer = t;
      t.schedule(delay);
    } //else {
      //System.out.println("\n\n\n----> ignoring next ");
    //}
  }

  private boolean preventFutureTimerUse = false;
  protected void cancelTimer() {
    removePlayingHighlight();

    preventFutureTimerUse = true;
    if (currentTimer != null) currentTimer.cancel();
  }
  private void initRecordButton() {  answerWidget.initRecordButton();  }

  /**
   * @see #nextAfterDelay(boolean, String)
   */
  protected void loadNext() {}

  /**
   * @see FlashcardPanel#FlashcardPanel
   * @param controller
   * @return
   */
  @Override
  DivWidget getFirstRow(ExerciseController controller) {
    DivWidget firstRow = super.getFirstRow(controller);
    List<CorrectAndScore> scores = exercise.getScores();
    if (!scores.isEmpty()) {
      DivWidget historyDiv = new DivWidget();
      historyDiv.getElement().setId("historyDiv");
      firstRow.add(historyDiv);
      String history = SetCompleteDisplay.getScoreHistory(scores);
      String s = "<span style='float:right;" +
          "'>" + history + "&nbsp;" + Math.round(getAvgScore(scores)) +
          "</span>";

      historyDiv.add(new HTML(s));
    }
    //else {
    //  System.out.println("scores are empty??? for " + exercise);
    //}
    return firstRow;
  }

  private float getAvgScore(List<CorrectAndScore> toUse) {
    if (toUse.isEmpty()) return 0f;

    float c = 0;
    float n = 0;
    if (toUse.size() > 5) toUse = toUse.subList(toUse.size() - 5, toUse.size());
    for (CorrectAndScore correctAndScore : toUse) {
      if (correctAndScore.getScore() > 0) {
        c += (float) correctAndScore.getPercentScore();
        n++;
      }
    }
    return n > 0f ? c / n : 0f;
  }
}
