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

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ButtonGroup;
import com.github.gwtbootstrap.client.ui.ButtonToolbar;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.IconAnchor;
import com.github.gwtbootstrap.client.ui.constants.ToggleType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.download.DownloadContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.initial.PopupHelper;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.recorder.FlashcardRecordButton;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.recorder.RecordButtonPanel;
import mitll.langtest.client.scoring.ClickableWords;
import mitll.langtest.client.scoring.FieldType;
import mitll.langtest.client.scoring.ScoreFeedbackDiv;
import mitll.langtest.client.scoring.ScoreProgressBar;
import mitll.langtest.client.sound.CompressedAudio;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.Validity;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.scoring.AlignmentAndScore;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static mitll.langtest.client.scoring.DialogExercisePanel.BLUE;
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
public class BootstrapExercisePanel<L extends CommonShell, T extends ClientExercise> //T extends CommonExercise & MutableAnnotationExercise>
    extends FlashcardPanel<L, T>
    implements AudioAnswerListener {
  private final Logger logger = Logger.getLogger("BootstrapExercisePanel");

  private static final int RECO_OUTPUT_WIDTH = 725;

  /**
   * Auto fetch their response as a compressed version.
   * TODO :  Bug - first time you click it, button returns to play state too early...
   */
  private static final boolean DO_AUTOLOAD = false;

  private static final int FEEDBACK_LEFT_MARGIN = PROGRESS_LEFT_MARGIN;
  private Panel recoOutput;

  private static final int DELAY_MILLIS_LONG = 3000;
  public static final int HIDE_DELAY = 2500;
  static final int DELAY_MILLIS = 100;
  private static final boolean DEBUG = false;

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
   * @param exerciseList
   * @see StatsPracticePanel#StatsPracticePanel
   */
  BootstrapExercisePanel(final T e,
                         final ExerciseController controller,
                         boolean addKeyBinding,
                         final ControlState controlState,
                         MySoundFeedback soundFeedback,
                         SoundFeedback.EndListener endListener,
                         ListInterface exerciseList) {
    super(e, controller, addKeyBinding, controlState, soundFeedback, endListener, exerciseList);
    downloadContainer = new DownloadContainer();
    //logger.info("Bootstrap instance " + instance);
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
  private Panel contextSentenceWhileWaiting;

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
    // add answer widget to do the recording
    toAddTo.add(getAnswerAndRecordButtonRow(exerciseID, controller));

    if (exercise.hasContext()) {
      addContextSentenceToShowWhileWaiting(controller, toAddTo);
    }
    scoreFeedbackRow = new DivWidget();
    scoreFeedbackRow.addStyleName("bottomFiveMargin");
    scoreFeedbackRow.setHeight("52px");


    toAddTo.add(scoreFeedbackRow);

    DivWidget wrapper = new DivWidget();
    wrapper.getElement().getStyle().setTextAlign(Style.TextAlign.CENTER);

    recoOutput = new DivWidget();
    recoOutput.getElement().getStyle().setProperty("maxWidth", RECO_OUTPUT_WIDTH + "px");

    wrapper.add(recoOutput);

    recoOutput.getElement().getStyle().setDisplay(Style.Display.INLINE_BLOCK);
    toAddTo.add(wrapper);
  }

  private void addContextSentenceToShowWhileWaiting(ExerciseController controller, Panel toAddTo) {
    ClientExercise next = exercise.getDirectlyRelated().iterator().next();
    int fontSize = controller.getProjectStartupInfo().getLanguageInfo().getFontSize();

    ClickableWords commonExerciseClickableWords =
        new ClickableWords(null, exercise.getID(), controller.getLanguage(), fontSize, BLUE);

    String flToShow = next.getFLToShow();
    String flToShow1 = exercise.getFLToShow();
    DivWidget contentWidget = commonExerciseClickableWords.getClickableWordsHighlight(flToShow, flToShow1,
        FieldType.FL, new ArrayList<>(), false);

    contextSentenceWhileWaiting = getCenteredRow(contentWidget);

    contextSentenceWhileWaiting.setVisible(false);
    IconAnchor waiting = new IconAnchor();
    waiting.setBaseIcon(MyCustomIconType.waiting);

    contextSentenceWhileWaiting.add(waiting);

    contextSentenceWhileWaiting.getElement().getStyle().setFontStyle(Style.FontStyle.ITALIC);
    toAddTo.add(contextSentenceWhileWaiting);
  }

  private RecordButtonPanel answerWidget;
  private Widget button;
  private RecordButton realRecordButton;

  /**
   * @param exerciseID
   * @param controller
   * @return
   * @see #addRecordingAndFeedbackWidgets(int, ExerciseController, Panel)
   */
  private Widget getAnswerAndRecordButtonRow(int exerciseID, ExerciseController controller) {
    // logger.info("BootstrapExercisePanel.getAnswerAndRecordButtonRow = " + instance);
    RecordButtonPanel answerWidget = getAnswerWidget(exerciseID, controller, addKeyBinding);
    this.answerWidget = answerWidget;
    button = answerWidget.getRecordButton();
    realRecordButton = answerWidget.getRealRecordButton();
    realRecordButton.setVisible(controller.shouldRecord());

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
    Panel recordButtonRow = new DivWidget();
    recordButtonRow.setWidth("100%");

    getCenteredRow(recordButton, recordButtonRow);

    return recordButtonRow;
  }

  private Panel getCenteredRow(Widget recordButton) {
    Panel recordButtonRow = new DivWidget();
    getCenteredRow(recordButton, recordButtonRow);
    Style style = recordButtonRow.getElement().getStyle();
    style.setProperty("marginLeft", "auto");
    style.setProperty("marginRight", "auto");
    style.setWidth(75, Style.Unit.PCT);

    return recordButtonRow;
  }

  @NotNull
  private void getCenteredRow(Widget recordButton, Panel recordButtonRow) {
    DivWidget bDiv = new DivWidget();
    bDiv.add(recordButton);

    recordButtonRow.add(bDiv);
    recordButtonRow.getElement().setId("recordButtonRow");

    recordButtonRow.addStyleName("alignCenter");
    recordButton.addStyleName("alignCenter");
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
    return new FlashcardRecordButtonPanel(exercisePanel, controller) {
      final FlashcardRecordButtonPanel outer = this;
     // private Timer waitTimer = null;

 /*     @Override
      protected void showWaiting() {
        if (contextSentenceWhileWaiting != null) {
          scheduleWaitTimer();
        } else {
          super.showWaiting();
        }
      }
*/

   /*   @Override
      protected void hideWaiting() {
        super.hideWaiting();
        cancelTimer();
        if (contextSentenceWhileWaiting != null) {
          contextSentenceWhileWaiting.setVisible(false);
        }
      }*/

  /*    private void scheduleWaitTimer() {
        cancelTimer();

        waitTimer = new Timer() {
          @Override
          public void run() {
            if (contextSentenceWhileWaiting != null) {
              contextSentenceWhileWaiting.setVisible(true);
            }
          }
        };
        waitTimer.schedule(500);
      }*/

   /*   private void cancelTimer() {
        if (waitTimer != null) {
           waitTimer.cancel();
        }

      }*/

      @NotNull
      protected String getDeviceType() {
        return getDeviceTypeValue();
      }

      /**
       * @seex RecordButtonPanel#postAudioFile
       * @return
       */
      @Override
      protected String getDevice() {
        return getDeviceValue();
      }

      @Override
      protected RecordButton makeRecordButton(final ExerciseController controller, String buttonTitle) {
        // logger.info("makeRecordButton : using " + instance);
        FlashcardRecordButtonPanel recordingListener = this;
        final FlashcardRecordButton widgets = new FlashcardRecordButton(
            exerciseID,
            controller,
            recordingListener,
            addKeyBinding
        ) {

          /**
           * @see RecordButton#startRecordingWithTimer
           */
          @Override
          protected void start() {
            controller.logEvent(this, AVP_RECORD_BUTTON, exerciseID, "Start_Recording");
            super.start();
            setAutoPlay(false);
            recordingStarted();
          }

          @Override
          public void stop(long duration, boolean abort) {
            controller.logEvent(this, AVP_RECORD_BUTTON, exerciseID, "Stop_Recording");
            outer.setAllowAlternates(showOnlyEnglish);
            //logger.info("BootstrapExercisePlugin : stop recording " + duration);
            super.stop(duration, abort);
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
          protected String getDevice() {
            return getDeviceValue();
          }

          /**
           * @see FlashcardRecordButton#checkKeyDown
           */
          @Override
          protected void gotEnter() {
            playRef();
          }

          @Override
          protected boolean shouldIgnoreKeyPress() {
            boolean b = super.shouldIgnoreKeyPress() || otherReasonToIgnoreKeyPress();
            //   if (b) logger.info("ignore key press?");
            return b;
          }

          @Override
          public void useResult(AudioAnswer result) {
            receivedAudioAnswer(result);
          }

          @Override
          protected void useInvalidResult(int exid, Validity validity, double dynamicRange) {
          //  super.useInvalidResult(exid, validity, dynamicRange);
            receivedAudioAnswer(new AudioAnswer("", validity, -1, 0, exid));
          }
        };

        // without this, the arrow keys may go to the chapter selector
        grabFocus(widgets);
        return widgets;
      }
    };
  }


  private void grabFocus(IconAnchor widgets) {
    Scheduler.get().scheduleDeferred((Command) () -> {
      if (widgets != null) {
        //logger.warning("getAnswerWidget set focus on " + widgets.getElement().getId());
        widgets.setFocus(true);
      } else {
        logger.warning("getAnswerWidget no widgets ");
      }
    });
  }

  /**
   * @see #getAnswerWidget
   */
  void recordingStarted() {
  }

  String getDeviceValue() {
//    logger.warning("getDeviceValue default ");

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
   * @param isFullMatch
   * @see #showCorrectFeedback
   * @see #showRecoFeedback
   */
  private void showPronScoreFeedback(boolean correct, double score, boolean isFullMatch) {
    scoreFeedbackRow.clear();
    scoreFeedbackRow.add(showScoreFeedback(correct, score, isFullMatch));
  }

  /**
   * use same as in learn tab or vice versa
   *
   * @param score
   * @param isFullMatch
   * @see #showPronScoreFeedback(boolean, double, boolean)
   */
  private DivWidget showScoreFeedback(boolean correct, double score, boolean isFullMatch) {
    DivWidget container = new DivWidget();

    container.setId("feedbackContainerAndBar");

    {
      IconAnchor correctIcon = new IconAnchor();
//      logger.info("showScoreFeedback correct" + correct + " is full " + isFullMatch);
      correctIcon.setBaseIcon(correct && isFullMatch ? MyCustomIconType.correct : MyCustomIconType.incorrect);

      DivWidget iconContainer = new DivWidget();
      iconContainer.addStyleName("floatLeft");

      iconContainer.add(correctIcon);
      container.add(iconContainer);
    }

    {
      DivWidget scoreContainer = new DivWidget();
      scoreContainer.addStyleName("floatLeft");

      scoreContainer.addStyleName("topMargin");
      scoreContainer.addStyleName("leftFiveMargin");
      scoreContainer.add(getProgressBar(score, isFullMatch));

      scoreContainer.setWidth("73%");

      container.setWidth("78%");
      container.getElement().getStyle().setMarginLeft(FEEDBACK_LEFT_MARGIN, Style.Unit.PCT);
      container.add(scoreContainer);
    }

    return container;
  }

  @NotNull
  private DivWidget getProgressBar(double score, boolean isFullMatch) {
    DivWidget widgets = new ScoreProgressBar().showScore(score * 100, isFullMatch);
    widgets.setHeight("25px");
    return widgets;
  }

  @Override
  public void postedAudio() {

  }

  /**
   * @param result
   * @see mitll.langtest.client.recorder.RecordButtonPanel#receivedAudioAnswer
   */
  public void receivedAudioAnswer(final AudioAnswer result) {
    setDownloadHref(result.getPath());

    boolean badAudioRecording = result.getValidity() != Validity.OK;

    //    logger.info("BootstrapExercisePanel.receivedAudioAnswer: correct " + correct + " pron score : " + score +
//        " has ref " + hasRefAudio + " bad audio " + badAudioRecording + " result " + result);

    if (badAudioRecording) {
      onBadRecording(result);
    } else {
      //String prefix = "";
      final double score = result.getScore();

      if (isCorrect(result.isCorrect(), score)) {
        showCorrectFeedback(score, result.getPretestScore());
      } else {   // incorrect!!
        showIncorrectFeedback(result, score, hasRefAudio());
      }
/*
      controller.logEvent(button, "Button", exercise.getID(), prefix + "correct response - score " +
          Math.round(score * 100f));*/

      // load audio?  why fetch it? unless we're going to play it?
      playAudioPanel.startSong(CompressedAudio.getPath(result.getPath()), DO_AUTOLOAD);
    }
  }

  private boolean hasRefAudio() {
    String path = exercise.getRefAudio() != null ? exercise.getRefAudio() : exercise.getSlowAudioRef();
    return path != null;
  }

  private void onBadRecording(AudioAnswer result) {
    controller.logEvent(button, "Button", exercise.getID(), "bad recording");
    putBackText();
    if (!realRecordButton.checkAndShowTooLoud(result.getValidity())) {
      if (button instanceof ComplexPanel) {
        button = ((ComplexPanel) button).getWidget(0);
        //    logger.info("receivedAudioAnswer: show popup for " + result.getValidity() + " on " + button.getElement().getId());
      }
//        else {
//          logger.info("receivedAudioAnswer: NOPE : show popup for " + result.getValidity() + " on " + button.getElement().getId());
//        }
      showPopup(result.getValidity().getPrompt(), button);
    }
    initRecordButton();
    clearFeedback();
    recoOutput.clear();
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
  private void showCorrectFeedback(double score, AlignmentAndScore pretestScore) {
    showOtherText(); // if only one of foreign or english showing
    playCorrectDing();
    showRecoFeedback(score, pretestScore, true);

    maybeAdvance(score, pretestScore.isFullMatch());
  }

  /**
   * @param score
   * @param pretestScore
   * @param correct
   * @see #showCorrectFeedback
   */
  void showRecoFeedback(double score, AlignmentAndScore pretestScore, boolean correct) {
    showPronScoreFeedback(correct, score, pretestScore.isFullMatch());
    showRecoOutput(pretestScore);
  }

  void playCorrectDing() {
    getSoundFeedback().queueSong(SoundFeedback.CORRECT);
  }

  /**
   * Polyglot does auto advance if score is high enough.
   *
   * @param score
   * @param isFullMatch
   */
  void maybeAdvance(double score, boolean isFullMatch) {
  }

  private void showRecoOutput(AlignmentAndScore pretestScore) {
    recoOutput.clear();

    playAudioPanel = new PlayAudioPanel(null, controller, exercise.getID());
    ScoreFeedbackDiv scoreFeedbackDiv = new ScoreFeedbackDiv(playAudioPanel, playAudioPanel, downloadContainer, true);
    downloadContainer.getDownloadContainer().setVisible(true);
    recoOutput.add(scoreFeedbackDiv.getWordTableContainer(pretestScore, new ClickableWords().isRTL(exercise.getForeignLanguage())));
  }

  PlayAudioPanel playAudioPanel;
  /**
   * @see #BootstrapExercisePanel
   */
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
    if (true) {//showScoreFeedback(result)) { // if they said the right answer, but poorly, show pron score
      showPronScoreFeedback(false, score, result.getPretestScore().isFullMatch());
    }
    showOtherText();

    if (DEBUG) {
      logger.info("showIncorrectFeedback : " +
          // "said answer " + result.isSaidAnswer() +
          "\n\tcorrect " + result.isCorrect() +
          "\n\tresult " + result +
          "\n\tscore " + score +
          "\n\thas ref " + hasRefAudio);
    }

    if (hasRefAudio) {
      if (controlState.isAudioFeedbackOn()) {
        playRefOnError();
      } else {
        playIncorrect();
      }
    } else {
      tryAgain();
    }

    showRecoOutput(result.getPretestScore());

    maybeAdvance(score, result.getPretestScore().isFullMatch());
  }

  /**
   * don't play ref in polyglot -subclass there
   */
  void playRefOnError() {
    String path = getRefAudioToPlay();
    if (path == null) {
      playIncorrect(); // this should never happen
    } else {
      logger.info("showIncorrectFeedback play ref audio on error");
      playRef();
    }
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

  private void disableRecord() {
    realRecordButton.setEnabled(false);
  }

  private void enableRecord() {
    realRecordButton.setEnabled(true);
  }

  @Override
  boolean otherReasonToIgnoreKeyPress() {
    boolean b = !realRecordButton.isEnabled();
    if (b) logger.warning("Bootstrap record button disabled?");
    boolean b1 = super.otherReasonToIgnoreKeyPress();
    if (b1) logger.warning("Bootstrap otherReasonToIgnoreKeyPress?");
    return b1 || b;
  }

  /**
   * @see #timerCancelled
   * @see StatsPracticePanel#recordingStarted
   */
  void removePlayingHighlight() {
    removePlayingHighlight(foreign);
  }

  @Override
  public void stopRecording() {
    if (realRecordButton != null) {
      realRecordButton.stopRecordingSafe();
    }
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
        adjusted.add(isCorrect(score.isCorrect(), score.getScore()));
      }
      String history = SetCompleteDisplay.getScoreHistory(adjusted);
      String s = "<span style='float:right;'>" + history + "&nbsp;" + Math.round(getAvgScore(scores)) + "</span>";

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
