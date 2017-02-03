/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.amas;

import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Icon;
import com.github.gwtbootstrap.client.ui.Paragraph;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconSize;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PopupHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.flashcard.AudioAnswerListener;
import mitll.langtest.client.flashcard.FlashcardRecordButtonPanel;
import mitll.langtest.client.recorder.FlashcardRecordButton;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.recorder.RecordButtonPanel;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.shared.answer.AudioAnswer;

import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 3/6/13
 * Time: 3:07 PM
 * To change this template use File | Settings | File Templates.
 */
class PressAndHoldExercisePanel extends VerticalPanel implements AudioAnswerListener {
  private final Logger logger = Logger.getLogger("PressAndHoldExercisePanel");

  private static final String ANSWER_UPDATED = "Answer Updated";
  private Heading recoOutput;
  private static final int HIDE_DELAY = 2500;

  private final int exerciseID;

  private final MySoundFeedback soundFeedback;
  private final ExerciseController controller;

  private final String instance;
  private ScoreFeedback scoreFeedback;

  private RecordButtonPanel answerWidget;
  private Widget button;
  private RecordButton realRecordButton;

  private PlayAudioPanel playAudioPanel;
  private final DivWidget iconContainer = new DivWidget();
  private Panel recoOutputContainer;

  /**
   * @param exerciseID
   * @param service
   * @param controller
   * @param instance
   * @param qid
   * @param typeToSelection
   * @see mitll.langtest.client.recorder.FeedbackRecordPanel.AnswerPanel#addComboAnswer
   */
  public PressAndHoldExercisePanel(final int exerciseID,
                                   final LangTestDatabaseAsync service,
                                   final ExerciseController controller,
                                   MySoundFeedback soundFeedback,
                                   String instance, int qid, Map<String, Collection<String>> typeToSelection) {
    this.exerciseID = exerciseID;
    this.controller = controller;
    this.instance = instance;
    this.soundFeedback = soundFeedback;
    addRecordingAndFeedbackWidgets(exerciseID, service, controller, this, qid,typeToSelection);
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
  private void addRecordingAndFeedbackWidgets(int e, LangTestDatabaseAsync service, ExerciseController controller,
                                              Panel toAddTo, int qid, Map<String, Collection<String>> typeToSelection) {
    Widget answerAndRecordButtonRow = getAnswerAndRecordButtonRow(e, service, controller, qid,typeToSelection);
    answerAndRecordButtonRow.addStyleName("topFiveMargin");
    answerAndRecordButtonRow.addStyleName("bottomFiveMargin");
    toAddTo.add(answerAndRecordButtonRow);
    Panel recoOutputRow = getRecoOutputRow();
   // toAddTo.add(recoOutputRow);
  }

  /**
   * @param exerciseID
   * @param service
   * @param controller
   * @param qid
   * @return
   * @see #addRecordingAndFeedbackWidgets(mitll.langtest.shared.CommonExercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, com.google.gwt.user.client.ui.Panel, int)
   */
  private Widget getAnswerAndRecordButtonRow(int exerciseID, LangTestDatabaseAsync service,
                                             ExerciseController controller, int qid, Map<String, Collection<String>> typeToSelection) {
    RecordButtonPanel answerWidget = getAnswerWidget(exerciseID, service, controller, false // = DO NOT add key binding! - bad for now for multi-question responses
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
  private RecordButtonPanel getAnswerWidget(final int exercise, LangTestDatabaseAsync service,
                                            final ExerciseController controller, final boolean addKeyBinding,
                                            String instance,
                                            int qid,
                                            Map<String, Collection<String>> typeToSelection) {
    PressAndHoldExercisePanel widgets = this;
    return new FlashcardRecordButtonPanel(widgets, controller, exercise, qid) {
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
          public void stop(long duration) {
            controller.logEvent(this, "AVP_RecordButton", exercise, "Stop_Recording");
            super.stop(duration);
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
       * @see RecordButton.RecordingListener#stopRecording(long)
       */
      @Override
      protected void receivedAudioAnswer(AudioAnswer result, final Panel outer) {
        recordButton.setEnabled(true);
        super.receivedAudioAnswer(result, outer);
        scoreFeedback.hideWaiting();
        scoreFeedback.showCRTFeedback(controller);
      }

      @Override
      public void stopRecording(long duration) {
        scoreFeedback.setWaiting();
        super.stopRecording(duration);
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
  void showPronScoreFeedback(double score) {
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
      controller.logEvent(button, "Button", exerciseID, "bad recording");
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
        controller.logEvent(button, "Button", exerciseID, "correct response - score " + Math.round(score * 100f));
        showCorrectFeedback(result, score);
      } else {   // incorrect!!
        controller.logEvent(button, "Button", exerciseID, "incorrect response - score " + Math.round(score * 100f));
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
   * @see #receivedAudioAnswer(AudioAnswer)
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
   * @see #receivedAudioAnswer(AudioAnswer)
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
