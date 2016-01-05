package mitll.langtest.client.amas;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconSize;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PopupHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.flashcard.FlashcardRecordButtonPanel;
import mitll.langtest.client.recorder.FlashcardRecordButton;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.recorder.RecordButtonPanel;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.Shell;

import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 3/6/13
 * Time: 3:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class PressAndHoldExercisePanel extends VerticalPanel implements AudioAnswerListener {
  private final Logger logger = Logger.getLogger("PressAndHoldExercisePanel");

  private static final String ANSWER_UPDATED = "Answer Updated";
  private Heading recoOutput;
  public static final int HIDE_DELAY = 2500;

  private final Shell exercise;

  private final MySoundFeedback soundFeedback;
  private final ExerciseController controller;

  private final String instance;
  private ScoreFeedback scoreFeedback;

  private RecordButtonPanel answerWidget;
  private Widget button;
  private RecordButton realRecordButton;

  private PlayAudioPanel playAudioPanel;
  private DivWidget iconContainer = new DivWidget();
  private Panel recoOutputContainer;

  /**
   * @param e
   * @param service
   * @param controller
   * @param instance
   * @param qid
   * @param typeToSelection
   * @see mitll.langtest.client.recorder.FeedbackRecordPanel.AnswerPanel#addComboAnswer
   */
  public PressAndHoldExercisePanel(final Shell e,
                                   final LangTestDatabaseAsync service,
                                   final ExerciseController controller,
                                   MySoundFeedback soundFeedback,
                                   String instance, int qid, Map<String, Collection<String>> typeToSelection) {
    this.exercise = e;
    this.controller = controller;
    this.instance = instance;
    this.soundFeedback = soundFeedback;
    addRecordingAndFeedbackWidgets(e, service, controller, this, qid,typeToSelection);
  }

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
   * @param qid
   * @seex #FlashcardPanel
   */
  private void addRecordingAndFeedbackWidgets(CommonExercise e, LangTestDatabaseAsync service, ExerciseController controller,
                                              Panel toAddTo, int qid, Map<String, Collection<String>> typeToSelection) {
    Widget answerAndRecordButtonRow = getAnswerAndRecordButtonRow(e, service, controller, qid,typeToSelection);
    answerAndRecordButtonRow.addStyleName("topFiveMargin");
    answerAndRecordButtonRow.addStyleName("bottomFiveMargin");
    toAddTo.add(answerAndRecordButtonRow);
    Panel recoOutputRow = getRecoOutputRow();
   // toAddTo.add(recoOutputRow);
  }

  /**
   * @param e
   * @param service
   * @param controller
   * @param qid
   * @return
   * @see #addRecordingAndFeedbackWidgets(mitll.langtest.shared.CommonExercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, com.google.gwt.user.client.ui.Panel, int)
   */
  private Widget getAnswerAndRecordButtonRow(CommonExercise e, LangTestDatabaseAsync service, ExerciseController controller, int qid, Map<String, Collection<String>> typeToSelection) {
    RecordButtonPanel answerWidget = getAnswerWidget(e, service, controller, false // = DO NOT add key binding! - bad for now for multi-question responses
        , instance, qid,typeToSelection);
    this.answerWidget = answerWidget;
    button = answerWidget.getRecordButton();
    realRecordButton = answerWidget.getRealRecordButton();
    realRecordButton.setEnabled(controller.isMicAvailable());

    return getRecordButtonRow(button);
  }

  /**
   * Left to right : reco output container (for spinning wait icon), record button, icon audio feedback icon, and play button
   *
   * @param recordButton
   * @return
   * @see #getAnswerAndRecordButtonRow(mitll.langtest.shared.CommonExercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int)
   * @see PressAndHoldExercisePanel#addRecordingAndFeedbackWidgets(mitll.langtest.shared.CommonExercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, com.google.gwt.user.client.ui.Panel, int)
   */
  private Panel getRecordButtonRow(Widget recordButton) {
    Panel recordButtonRow = new HorizontalPanel();
    recordButtonRow.getElement().setId("recordButtonRow");
  //  recordButtonRow.add(recoOutputContainer = new DivWidget());
    recordButtonRow.add(recordButton);

    configureIconContainer();
    recordButtonRow.add(iconContainer);

    if (!controller.isMicAvailable()) {
      addIcon(IconType.MICROPHONE_OFF);
    }

    VerticalPanel vp = new VerticalPanel();
    vp.add(getPlayAudioPanel());
    vp.add(new HTML("Check Answer"));
    recordButtonRow.add(vp);
    return recordButtonRow;
  }

  private Panel getPlayAudioPanel() {
    playAudioPanel = new PlayAudioPanel(controller, "");
    playAudioPanel.getElement().getStyle().setMarginTop(5, Style.Unit.PX);
    playAudioPanel.getElement().getStyle().setMarginLeft(20, Style.Unit.PX);
    playAudioPanel.setEnabled(false);
    return playAudioPanel;
  }

  private void configureIconContainer() {
    iconContainer.getElement().getStyle().setMarginLeft(5, Style.Unit.PX);
    iconContainer.getElement().getStyle().setMarginTop(5, Style.Unit.PX);
    iconContainer.setHeight("39px");
    iconContainer.setWidth("39px");
  }

  /**
   * Center align the text feedback (correct/incorrect)
   *
   * @return
   * @see #addRecordingAndFeedbackWidgets
   */
  private Panel getRecoOutputRow() {
    recoOutput = new Heading(3, "");
    recoOutput.getElement().setId("recoOutputHeading");
    recoOutput.getElement().getStyle().setProperty("fontFamily", "sans-serif");
    recoOutput.addStyleName("cardHiddenText2");   // same color as background so text takes up space but is invisible
    recoOutput.getElement().getStyle().setColor("#ffffff");

    Panel recoOutputRow = new FluidRow();
  //  recoOutputRow.setHeight("60px");
    recoOutputRow.getElement().setId("recoOutputRow");

    Panel recoOutputContainer = new Paragraph();
    recoOutputContainer.getElement().setId("recoOutputContainer");
    recoOutputContainer.addStyleName("alignCenter");

    recoOutputRow.add(new Column(11, recoOutputContainer));
    recoOutputRow.add(new Column(1, this.recoOutputContainer = new DivWidget()));
    recoOutputContainer.add(recoOutput);
    return recoOutputRow;
  }

  /**
   * @param exercise
   * @param service
   * @param controller
   * @param addKeyBinding
   * @param instance
   * @param qid
   * @return
   * @see #getAnswerAndRecordButtonRow(mitll.langtest.shared.CommonExercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int)
   */
  private RecordButtonPanel getAnswerWidget(final CommonExercise exercise, LangTestDatabaseAsync service,
                                            ExerciseController controller, final boolean addKeyBinding, String instance, int qid, Map<String, Collection<String>> typeToSelection) {
    return new FlashcardRecordButtonPanel(this, service, controller, exercise, qid, instance, typeToSelection) {
      @Override
      public Widget getRecordButton() {
        return super.getRealRecordButton();
      }

      /**
       * @see mitll.langtest.client.recorder.RecordButtonPanel#RecordButtonPanel
       * @param controller
       * @param buttonTitle
       * @return
       */
      @Override
      protected RecordButton makeRecordButton(final ExerciseController controller, String buttonTitle) {

        final FlashcardRecordButton widgets = new FlashcardRecordButton(controller.getRecordTimeout(), this, true,
            addKeyBinding, controller,
            PressAndHoldExercisePanel.this.instance) {

          @Override
          protected void start() {
            controller.logEvent(this, "AVP_RecordButton", exercise, "Start_Recording");
            super.start();
          }

          @Override
          public void stop() {
            controller.logEvent(this, "AVP_RecordButton", exercise, "Stop_Recording");
            super.stop();
          }

          @Override
          protected void gotLeftArrow() {
          }

          @Override
          protected void gotRightArrow() {
          }

          @Override
          protected boolean shouldIgnoreKeyPress() {
            return super.shouldIgnoreKeyPress();
          }

          @Override
          protected String getPrompt() {
            return "Speak in " + controller.getLanguage();
          }
        };

        // don't do this with search box on the page...
        // without this, the arrow keys may go to the chapter selector
/*
        Scheduler.get().scheduleDeferred(new Command() {
          public void execute() {
            widgets.setFocus(true);
          }
        });
*/
        return widgets;
      }

      @Override
      public void startRecording() {
        super.startRecording();
        playAudioPanel.setEnabled(false);
        scoreFeedback.clearFeedback();
        recoOutput.setText("");
      }

      /**
       * @param result        from server about the audio we just posted
       * @paramx questionState so we keep track of which questions have been answered
       * @param outer
       * @see mitll.langtest.client.recorder.RecordButtonPanel#stopRecording()
       */
      @Override
      protected void receivedAudioAnswer(AudioAnswer result, final Panel outer) {
        recordButton.setEnabled(true);
        super.receivedAudioAnswer(result, outer);
        scoreFeedback.hideWaiting();
        scoreFeedback.showCRTFeedback(controller);
      }

      @Override
      public void stopRecording() {
        scoreFeedback.setWaiting();
        super.stopRecording();
        recordButton.setVisible(true);
        recordButton.setEnabled(false);
      }
    };
  }

  /**
   * Show progress bar with score percentage, colored by score.
   * Note it has to be wide enough to hold the text "pronunciation score xxx %"
   *
   * @param score
   * @see #showCorrectFeedback
   * @see #showIncorrectFeedback
   */
  protected void showPronScoreFeedback(double score) {
  }
//  void clearFeedback() {}
//  private Heading getRecoOutput() { return recoOutput;  }

  private boolean prevRecording = false;

  /**
   * @param result
   * @see mitll.langtest.client.recorder.RecordButtonPanel#receivedAudioAnswer
   */
  public void receivedAudioAnswer(final AudioAnswer result) {
    final boolean hasRefAudio = false;// path != null;
    boolean correct = result.isCorrect();
    final double score = result.getScore();

    boolean badAudioRecording = result.getValidity() != AudioAnswer.Validity.OK;

    logger.info("PressAndHoldExercisePanel.receivedAudioAnswer: correct " + correct + " pron score : " + score +
        " has ref " + hasRefAudio + " bad audio " + badAudioRecording + " result " + result);

    iconContainer.clear();
    if (badAudioRecording) {
      controller.logEvent(button, "Button", exercise.getID(), "bad recording");
      if (!realRecordButton.checkAndShowTooLoud(result.getValidity())) {
        showPopup(result.getValidity().getPrompt(), realRecordButton.getParent());
      }
      initRecordButton();

      AudioAnswer.Validity validity = result.getValidity();
      if (validity.equals(AudioAnswer.Validity.TOO_LOUD)) {
        addIcon(IconType.BULLHORN);
      } else if (validity.equals(AudioAnswer.Validity.TOO_SHORT)) {
        addIcon(IconType.WARNING_SIGN);
      } else if (validity.equals(AudioAnswer.Validity.TOO_QUIET)) {
        addIcon(IconType.VOLUME_OFF);
      } else if (validity.equals(AudioAnswer.Validity.MIC_DISCONNECTED)) {
        addIcon(IconType.MICROPHONE_OFF);
      }

      playIncorrect();
    } else {

      logger.info("PressAndHoldExercisePanel.receivedAudioAnswer: correct " + correct + " pron score : " + score +
          " has ref " + hasRefAudio + " valid audio result " + result);

      playAudioPanel.setEnabled(true);
      playAudioPanel.loadAudioAgain(result.getPath());

      if (prevRecording) {
        addIcon(IconType.CHECK);
        showPopup(ANSWER_UPDATED, realRecordButton.getParent());
      }
      prevRecording = true;

      if (correct) {
        controller.logEvent(button, "Button", exercise.getID(), "correct response - score " + Math.round(score * 100f));
        showCorrectFeedback(result, score);
      } else {   // incorrect!!
        controller.logEvent(button, "Button", exercise.getID(), "incorrect response - score " + Math.round(score * 100f));
        showIncorrectFeedback(result, score);
      }
    }
    if (!badAudioRecording && (correct || !hasRefAudio)) {
      logger.info("\treceivedAudioAnswer: correct " + correct + " pron score : " + score + " has ref " + hasRefAudio);
      nextAfterDelay();
    }
  }

  private void addIcon(IconType microphoneOff) {
    Icon w = new Icon(microphoneOff);
    w.setIconSize(IconSize.THREE_TIMES);
    iconContainer.add(w);
  }

  /**
   * @param html
   * @see #receivedAudioAnswer
   */
  private void showPopup(String html, Widget button) {
    new PopupHelper().showPopup(html, button, HIDE_DELAY);
  }

  /**
   * @param score
   * @see #receivedAudioAnswer(mitll.langtest.shared.AudioAnswer)
   */
  private void showCorrectFeedback(AudioAnswer result, double score) {
    showPronScoreFeedback(score);
    logger.warning("\n\n\n----> correct showCorrectFeedback");
    getSoundFeedback().queueSong(SoundFeedback.CORRECT);
//    showRecoOutput(result);
  }

  /**
   * If there's reference audio, play it and wait for it to finish.
   * What to do when the user says the wrong word.
   *
   * @param result
   * @param score
   * @see #receivedAudioAnswer
   */
  private String showIncorrectFeedback(AudioAnswer result, double score) {
    showPronScoreFeedback(score);
    getSoundFeedback().queueSong(SoundFeedback.CORRECT);
    // playIncorrect();
//    showRecoOutput(result);
    return "";
  }

/*
  protected void showRecoOutput(AudioAnswer result) {
    String s = "Heard: ";
    String correctPrompt = s + result.getDecodeOutput();// + "<p>" + correctPrompt;
    Heading recoOutput = getRecoOutput();
    PretestScore pretestScore = result.getPretestScore();
    logger.info("got pretest score " + pretestScore);
    recoOutput.getElement().getStyle().setBackgroundColor(SimpleColumnChart.getColor(pretestScore.getHydecScore()));
    recoOutput.setText(result.getPretestScore().getHydecScore() < 0.4 ? "Not sure what you said." : correctPrompt);
    recoOutput.getElement().getStyle().setColor("#000000");
  }
*/

  private MySoundFeedback getSoundFeedback() {
    return soundFeedback;
  }

  /**
   * @param scoreFeedback
   * @see mitll.langtest.client.recorder.FeedbackRecordPanel.AnswerPanel#addAudioAnswer(int, PressAndHoldExercisePanel)
   */
  public void setScoreFeedback(ScoreFeedback scoreFeedback) {
    this.scoreFeedback = scoreFeedback;
  }

  private void playIncorrect() {
    getSoundFeedback().queueSong(SoundFeedback.INCORRECT);
  }

  /**
   * @paramx correct
   * @paramx feedback
   * @see #receivedAudioAnswer(mitll.langtest.shared.AudioAnswer)
   */
  private void nextAfterDelay() {
    initRecordButton();
  }

  private void initRecordButton() {
    answerWidget.initRecordButton();
  }

  public Panel getRecoOutputContainer() {
    return recoOutputContainer;
  }
}
