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

package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.IconAnchor;
import com.github.gwtbootstrap.client.ui.base.ProgressBarBase;
import com.github.gwtbootstrap.client.ui.constants.ToggleType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.initial.PopupHelper;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.recorder.FlashcardRecordButton;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.recorder.RecordButtonPanel;
import mitll.langtest.client.scoring.ClickableWords;
import mitll.langtest.client.download.DownloadContainer;
import mitll.langtest.client.scoring.ScoreFeedbackDiv;
import mitll.langtest.client.sound.CompressedAudio;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.Validity;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.MutableAnnotationExercise;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.scoring.PretestScore;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static mitll.langtest.client.scoring.SimpleRecordAudioPanel.OGG;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 3/6/13
 * Time: 3:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class BootstrapExercisePanel<T extends CommonExercise & MutableAnnotationExercise>
    extends FlashcardPanel<T>
    implements AudioAnswerListener {
  private Logger logger;

  private static final int FEEDBACK_LEFT_MARGIN = PROGRESS_LEFT_MARGIN;

  private static final int FIRST_STEP = 35;
  private static final int SECOND_STEP = 75;

  private static final double FIRST_STEP_PCT = ((double) FIRST_STEP) / 100d;
  private static final double SECOND_STEP_PCT = ((double) SECOND_STEP) / 100d;

  private Panel recoOutput;

  private static final int DELAY_MILLIS_LONG = 3000;
  public static final int HIDE_DELAY = 2500;
  static final int DELAY_MILLIS = 100;

  /**
   * @see #getFeedbackGroup(ControlState)
   */
  private static final String PLAY_ON_MISTAKE = "PLAY ON MISTAKE";
  private static final String AVP_RECORD_BUTTON = "AVP_RecordButton";

  /**
   * @param e
   * @param controller
   * @param soundFeedback
   * @param endListener
   * @param instance
   * @param exerciseList
   * @param prompt
   * @see StatsPracticePanel#StatsPracticePanel
   */
  BootstrapExercisePanel(final T e,
                         final ExerciseController controller,
                         boolean addKeyBinding,
                         final ControlState controlState,
                         MySoundFeedback soundFeedback,
                         SoundFeedback.EndListener endListener,
                         String instance,
                         ListInterface exerciseList, PolyglotDialog.PROMPT_CHOICE prompt) {
    super(e, controller, addKeyBinding, controlState, soundFeedback, endListener, instance, exerciseList);
    downloadContainer = new DownloadContainer();
  }

  /**
   * @param controlState
   * @return
   * @see #getRightColumn(mitll.langtest.client.flashcard.ControlState)
   */
  @Override
  protected ControlGroup getFeedbackGroup(final ControlState controlState) {
    ControlGroup group = new ControlGroup(PLAY_ON_MISTAKE);
    ButtonToolbar w = new ButtonToolbar();
    group.add(w);
    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.setToggle(ToggleType.RADIO);
    w.add(buttonGroup);

    Button onButton = makeGroupButton(buttonGroup, ON);

    onButton.addClickHandler(event -> {
      controlState.setAudioFeedbackOn(true);
      setAutoPlay(false);
    });
    onButton.setActive(controlState.isAudioFeedbackOn());

    Button offButton = makeGroupButton(buttonGroup, OFF);

    offButton.addClickHandler(event -> {
      controlState.setAudioFeedbackOn(false);
      setAutoPlay(false);
    });
    offButton.setActive(!controlState.isAudioFeedbackOn());

    return group;
  }

  private Button makeGroupButton(ButtonGroup buttonGroup, String title) {
    Button onButton = new Button(title);
    onButton.getElement().setId(PLAY_ON_MISTAKE + "_" + title);
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
   * @param exerciseID
   * @param controller used in subclasses for audio control
   * @param toAddTo
   * @see #FlashcardPanel
   */
  @Override
  protected void addRecordingAndFeedbackWidgets(int exerciseID,
                                                ExerciseController controller,
                                                Panel toAddTo) {
    if (logger == null) {
      logger = Logger.getLogger("BootstrapExercisePanel");
    }
    // logger.info("called  addRecordingAndFeedbackWidgets ");
    // add answer widget to do the recording
    toAddTo.add(getAnswerAndRecordButtonRow(exerciseID, controller));

    scoreFeedbackRow = new DivWidget();
    scoreFeedbackRow.addStyleName("bottomFiveMargin");
    scoreFeedbackRow.setHeight("52px");
    toAddTo.add(scoreFeedbackRow);

    DivWidget wrapper = new DivWidget();
    wrapper.getElement().getStyle().setTextAlign(Style.TextAlign.CENTER);

    recoOutput = new DivWidget();
    wrapper.add(recoOutput);

    recoOutput.getElement().getStyle().setDisplay(Style.Display.INLINE_BLOCK);
    toAddTo.add(wrapper);
  }

  private RecordButtonPanel answerWidget;
  private Widget button;
  private RecordButton realRecordButton;

  /**
   * @param exerciseID
   * @param controller
   * @return
   * @see FlashcardPanel#addRecordingAndFeedbackWidgets(int, ExerciseController, Panel)
   */
  private Widget getAnswerAndRecordButtonRow(int exerciseID, ExerciseController controller) {
    // logger.info("BootstrapExercisePanel.getAnswerAndRecordButtonRow = " + instance);
    RecordButtonPanel answerWidget = getAnswerWidget(exerciseID, controller, addKeyBinding);
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
   * @see #getAnswerAndRecordButtonRow
   * @see FlashcardPanel#addRecordingAndFeedbackWidgets(int, ExerciseController, Panel)
   */
  private Panel getRecordButtonRow(Widget recordButton) {
   // Panel recordButtonRow = getCenteredWrapper(recordButton);
    Panel recordButtonRow = new DivWidget();
    recordButtonRow.setWidth("100%");

    DivWidget bDiv =new DivWidget();
    bDiv.add(recordButton);
    recordButtonRow.add(bDiv);
    recordButtonRow.getElement().setId("recordButtonRow");


    recordButtonRow.addStyleName("alignCenter");
    recordButton.addStyleName("alignCenter");


   // recordButtonRow.addStyleName("leftTenMargin");
  //  recordButtonRow.addStyleName("rightTenMargin");
   // recordButtonRow.getElement().getStyle().setMarginRight(10, Style.Unit.PX);

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
   * @param exerciseID
   * @param controller
   * @param addKeyBinding
   * @return
   * @see #getAnswerAndRecordButtonRow
   */
  private RecordButtonPanel getAnswerWidget(final int exerciseID,
                                            ExerciseController controller,
                                            final boolean addKeyBinding) {
    AudioAnswerListener exercisePanel = this;
    return new FlashcardRecordButtonPanel(exercisePanel, controller, exerciseID, 1) {
      final FlashcardRecordButtonPanel outer = this;

      @NotNull
      protected String getDeviceType() {
        return getDeviceTypeValue();
      }

      @Override
      protected String getDevice() {
        return getDeviceValue();
      }

      @Override
      protected RecordButton makeRecordButton(final ExerciseController controller, String buttonTitle) {
        // logger.info("makeRecordButton : using " + instance);
        final FlashcardRecordButton widgets = new FlashcardRecordButton(
            controller.getRecordTimeout(),
            this,
            addKeyBinding,
            controller,
            BootstrapExercisePanel.this.instance) {
          @Override
          protected void start() {
            controller.logEvent(this, AVP_RECORD_BUTTON, exerciseID, "Start_Recording");
            super.start();
            setAutoPlay(false);
            recordingStarted();
          }

          @Override
          public void stop(long duration) {
            controller.logEvent(this, AVP_RECORD_BUTTON, exerciseID, "Stop_Recording");
            outer.setAllowAlternates(showOnlyEnglish);
            super.stop(duration);
          }


          /**
           * @see FlashcardRecordButton#checkKeyDown
           */
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
          protected void gotUpArrow() {
            gotDownArrow();
          }

          @Override
          protected void gotDownArrow() {
            if (!selectShowFL()) {
              flipCard();
            }
          }

          @Override
          protected void gotEnter() {

            playRef();
          }

          @Override
          protected boolean shouldIgnoreKeyPress() {
            return super.shouldIgnoreKeyPress() || otherReasonToIgnoreKeyPress();
          }
        };

        // without this, the arrow keys may go to the chapter selector
        Scheduler.get().scheduleDeferred((Command) () -> widgets.setFocus(true));
        return widgets;
      }
    };
  }

  /**
   * @see #getAnswerWidget
   */
  void recordingStarted() {
  }

  String getDeviceValue() {
    return controller.getBrowserInfo();
  }

  String getDeviceTypeValue() {
    return controller.getBrowserInfo();
  }

  /**
   * Show progress bar with score percentage, colored by score.
   * Note it has to be wide enough to hold the text "number %"
   *
   * @param correct
   * @param score
   * @see
   */
  private void showPronScoreFeedback(boolean correct, double score) {
    scoreFeedbackRow.clear();
    scoreFeedbackRow.add(showScoreFeedback(correct, score));
  }

  /**
   * TODO : use same as in learn tab or vice versa
   *
   * @param score
   * @see #showPronScoreFeedback(boolean, double)
   */
  private DivWidget showScoreFeedback(boolean correct, double score) {
    if (score < 0) score = 0;
    double percent = 100 * score;

    ProgressBar scoreFeedback = new ProgressBar();
    int percent1 = (int) percent;
    scoreFeedback.setPercent(percent1 < 40 ? 40 : percent1);   // just so the words will show up

    scoreFeedback.setHeight("25px");
    scoreFeedback.setText("Score " + percent1 + "%");
    //   scoreFeedback.setVisible(true);
//    scoreFeedback.setColor(
//        score > 0.8 ? ProgressBarBase.Color.SUCCESS :
//            score > 0.6 ? ProgressBarBase.Color.DEFAULT :
//                score > 0.4 ? ProgressBarBase.Color.WARNING :
//                    ProgressBarBase.Color.DANGER);

    scoreFeedback.setColor(
        score > FIRST_STEP_PCT ?
            ProgressBarBase.Color.SUCCESS : score > SECOND_STEP_PCT ?
            ProgressBarBase.Color.WARNING :
            ProgressBarBase.Color.DANGER);


    DivWidget container = new DivWidget();

    container.setId("containerAndBar");

    IconAnchor correctIcon = new IconAnchor();
    correctIcon.setBaseIcon(correct ? MyCustomIconType.correct : MyCustomIconType.incorrect);

    DivWidget iconContainer = new DivWidget();
    iconContainer.addStyleName("floatLeft");

    iconContainer.add(correctIcon);
    container.add(iconContainer);

    DivWidget scoreContainer = new DivWidget();
    scoreContainer.addStyleName("floatLeft");

    scoreContainer.addStyleName("topMargin");
    scoreContainer.addStyleName("leftFiveMargin");
    scoreContainer.add(scoreFeedback);

    scoreContainer.setWidth("73%");

    container.setWidth("78%");
    container.getElement().getStyle().setMarginLeft(FEEDBACK_LEFT_MARGIN, Style.Unit.PCT);
    container.add(scoreContainer);

    return container;
  }

  /**
   * @param result
   * @see mitll.langtest.client.recorder.RecordButtonPanel#receivedAudioAnswer
   */
  public void receivedAudioAnswer(final AudioAnswer result) {
    String path = exercise.getRefAudio() != null ? exercise.getRefAudio() : exercise.getSlowAudioRef();
    final boolean hasRefAudio = path != null;
    boolean correct = result.isCorrect();
    final double score = result.getScore();
    setDownloadHref(result.getPath());

    boolean badAudioRecording = result.getValidity() != Validity.OK;

    //    logger.info("BootstrapExercisePanel.receivedAudioAnswer: correct " + correct + " pron score : " + score +
//        " has ref " + hasRefAudio + " bad audio " + badAudioRecording + " result " + result);

    if (badAudioRecording) {
      controller.logEvent(button, "Button", exercise.getID(), "bad recording");
      putBackText();
      if (!realRecordButton.checkAndShowTooLoud(result.getValidity())) {
        if (button instanceof ComplexPanel) {
          button = ((ComplexPanel) button).getWidget(0);
          logger.info("receivedAudioAnswer: show popup for " + result.getValidity() + " on " + button.getElement().getId());
        }
//        else {
//          logger.info("receivedAudioAnswer: NOPE : show popup for " + result.getValidity() + " on " + button.getElement().getId());
//        }
        showPopup(result.getValidity().getPrompt(), button);
      }
      initRecordButton();
      clearFeedback();
      recoOutput.clear();
    } else {
      String prefix = "";
      if (isCorrect(correct, score)) {
        showCorrectFeedback(score, result.getPretestScore());
      } else {   // incorrect!!
        showIncorrectFeedback(result, score, hasRefAudio);
        prefix = "in";
      }

      controller.logEvent(button, "Button", exercise.getID(), prefix + "correct response - score " +
          Math.round(score * 100f));

      // load audio?  why fetch it? unless we're going to play it?
      playAudioPanel.startSong(CompressedAudio.getPath(result.getPath()), false);
    }
  }

  boolean isCorrect(boolean correct, double score) {
    return correct;
  }

  private void clearFeedback() {
    scoreFeedbackRow.clear();
  }

  /**
   * @param html
   * @see #receivedAudioAnswer
   */
  private void showPopup(String html, Widget button) {
    new PopupHelper().showPopup(html, button, HIDE_DELAY);
  }

  /**
   * TODO : if polyglot, auto advance if score higher than threshold.
   *
   * @param score
   * @see #receivedAudioAnswer
   */
  private void showCorrectFeedback(double score, PretestScore pretestScore) {
    showOtherText(); // if only one of foreign or english showing
    playCorrectDing();
    showRecoFeedback(score, pretestScore, true);

    maybeAdvance(score);
  }

  void showRecoFeedback(double score, PretestScore pretestScore, boolean correct) {
    showPronScoreFeedback(correct, score);
    showRecoOutput(pretestScore);
  }

  void playCorrectDing() {
    getSoundFeedback().queueSong(SoundFeedback.CORRECT);
  }

  /**
   * Polyglot does auto advance if score is high enough.
   * @param score
   */
  void maybeAdvance(double score) {
  }

  private void showRecoOutput(PretestScore pretestScore) {
    recoOutput.clear();

    playAudioPanel = new PlayAudioPanel(controller.getSoundManager(), null, controller, exercise);
    ScoreFeedbackDiv scoreFeedbackDiv = new ScoreFeedbackDiv(playAudioPanel, downloadContainer);
    downloadContainer.getDownloadContainer().setVisible(true);
    recoOutput.add(scoreFeedbackDiv.getWordTableContainer(pretestScore, new ClickableWords<>().isRTL(exercise)));
  }

  PlayAudioPanel playAudioPanel;
  private final DownloadContainer downloadContainer;

  private void setDownloadHref(String audioPath) {
    if (audioPath != null) {
      String audioPathToUse = audioPath.endsWith(OGG) ? audioPath.replaceAll(OGG, ".mp3") : audioPath;
      downloadContainer.setDownloadHref(audioPathToUse, exercise.getID(), controller.getUser(), controller.getHost());
    }
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
  private void showIncorrectFeedback(AudioAnswer result, double score, boolean hasRefAudio) {
    if (showScoreFeedback(result)) { // if they said the right answer, but poorly, show pron score
      showPronScoreFeedback(false, score);
    }
    showOtherText();

/*
      logger.info("showIncorrectFeedback : said answer " +result.isSaidAnswer()+
          "result " + result + " score " + score + " has ref " + hasRefAudio);*/

    if (hasRefAudio) {
      if (controlState.isAudioFeedbackOn()) {
        String path = getRefAudioToPlay();
        if (path == null) {
          playIncorrect(); // this should never happen
        } else {
          playRef();
        }
      } else {
        playIncorrect();
      }
    } else {
      tryAgain();
    }

    showRecoOutput(result.getPretestScore());

    maybeAdvance(score);
  }

  boolean showScoreFeedback(AudioAnswer result) {
    return result.isSaidAnswer();
  }

  /**
   * @see #showIncorrectFeedback
   */
  private void tryAgain() {
    playIncorrect();
    Timer t = new Timer() {
      @Override
      public void run() {
        initRecordButton();
      }
    };
    t.schedule(DELAY_MILLIS_LONG);
  }

  private void showOtherText() {
    if (controlState.isEnglish()) showForeign();
    else if (controlState.isForeign()) showEnglish();
  }

  private void putBackText() {
    if (controlState.isEnglish()) showEnglish();
    else if (controlState.isForeign()) showForeign();
  }

  void playIncorrect() {
    getSoundFeedback().queueSong(SoundFeedback.INCORRECT,
        new SoundFeedback.EndListener() {
          @Override
          public void songStarted() {
            disableRecord();
          }

          @Override
          public void songEnded() {
            enableRecord();
          }
        });
  }

  void disableRecord() {
    realRecordButton.setEnabled(false);
  }
  void enableRecord() {
    realRecordButton.setEnabled(true);
  }

  @Override
  boolean otherReasonToIgnoreKeyPress() {
    return super.otherReasonToIgnoreKeyPress() || !realRecordButton.isEnabled();
  }

  /**
   * @see #timerCancelled
   * @see StatsPracticePanel#recordingStarted
   */
  void removePlayingHighlight() {
    removePlayingHighlight(foreign);
  }

  private void initRecordButton() {
    answerWidget.initRecordButton();
  }

  /**
   * @param controller
   * @return
   * @see FlashcardPanel#FlashcardPanel
   */
  @Override
  DivWidget getFirstRow(ExerciseController controller) {
    DivWidget firstRow = super.getFirstRow(controller);
    List<CorrectAndScore> scores = exercise.getScores();
    if (scores != null && !scores.isEmpty()) {
      DivWidget historyDiv = new DivWidget();
      historyDiv.getElement().setId("historyDiv");
      firstRow.add(historyDiv);

      List<Boolean> adjusted = new ArrayList<>();
      for (CorrectAndScore score : scores) {
        boolean correct = isCorrect(score.isCorrect(), score.getScore());
        adjusted.add(correct);
      }
      String history = SetCompleteDisplay.getScoreHistory(adjusted);
      String s = "<span style='float:right;'>" + history + "&nbsp;" + Math.round(getAvgScore(scores)) +
          "</span>";

      historyDiv.add(new HTML(s));
    }
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
