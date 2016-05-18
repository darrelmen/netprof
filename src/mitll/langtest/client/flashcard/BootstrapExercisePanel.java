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
import com.github.gwtbootstrap.client.ui.base.ProgressBarBase;
import com.github.gwtbootstrap.client.ui.constants.ToggleType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PopupHelper;
import mitll.langtest.client.custom.exercise.CommentNPFExercise;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.recorder.FlashcardRecordButton;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.recorder.RecordButtonPanel;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.flashcard.CorrectAndScore;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
public class BootstrapExercisePanel<T extends CommonShell & AudioRefExercise & AnnotationExercise & ScoredExercise & MutableAnnotationExercise>
    extends FlashcardPanel<T>
    implements AudioAnswerListener {
  private Logger logger;

  private Heading recoOutput;
  private static final int DELAY_MILLIS_LONG = 3000;
  private static final int LONG_DELAY_MILLIS = 3500;
  private static final int DELAY_CHARACTERS = 40;
  private static final int HIDE_DELAY = 2500;

  private static final boolean NEXT_ON_BAD_AUDIO = false;
  private static final String FEEDBACK = "PLAY ON MISTAKE";
  private static final String AVP_RECORD_BUTTON = "AVP_RecordButton";

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
  public BootstrapExercisePanel(final T e,
                                final LangTestDatabaseAsync service,
                                final ExerciseController controller,
                                boolean addKeyBinding,
                                final ControlState controlState,
                                MySoundFeedback soundFeedback,
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
        //logger.info("now on " + controlState);
      }
    });
    onButton.setActive(controlState.isAudioFeedbackOn());

    Button offButton = makeGroupButton(buttonGroup, OFF);

    offButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        controlState.setAudioFeedbackOn(false);
        //logger.info("now off " + controlState);
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
   * @param exerciseID
   * @param service
   * @param controller used in subclasses for audio control
   * @param toAddTo
   * @see #BootstrapExercisePanel
   */
  @Override
  protected void addRecordingAndFeedbackWidgets(String exerciseID, LangTestDatabaseAsync service,
                                                ExerciseController controller,
                                                Panel toAddTo) {
    if (logger == null) {
      logger = Logger.getLogger("BootstrapExercisePanel");
    }
  //  logger.info("called  addRecordingAndFeedbackWidgets ");
    // add answer widget to do the recording
  //  String exerciseID = exerciseID.getID();
    Widget answerAndRecordButtonRow = getAnswerAndRecordButtonRow(exerciseID, service, controller);
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
   * @param exerciseID
   * @param service
   * @param controller
   * @return
   * @see FlashcardPanel#addRecordingAndFeedbackWidgets(String, LangTestDatabaseAsync, ExerciseController, Panel)
   */
  private Widget getAnswerAndRecordButtonRow(String exerciseID, LangTestDatabaseAsync service, ExerciseController controller) {
   // logger.info("BootstrapExercisePanel.getAnswerAndRecordButtonRow = " + instance);
    RecordButtonPanel answerWidget = getAnswerWidget(exerciseID, service, controller, addKeyBinding, instance);
    this.answerWidget = answerWidget;
    button = answerWidget.getRecordButton();
    realRecordButton = answerWidget.getRealRecordButton();

    return getRecordButtonRow(button);
  }

  @Override
  protected void setClickToFlipHeight(DivWidget clickToFlipContainer) { clickToFlipContainer.setHeight("12px");  }

  @Override
  protected void setMarginTop(HTML clickToFlip, Widget icon) {
    icon.getElement().getStyle().setMarginTop(2, Style.Unit.PX);
  }

  /**
   * Center align the record button image.
   *
   * @param recordButton
   * @return
   * @see #getAnswerAndRecordButtonRow(String, LangTestDatabaseAsync, ExerciseController)
   * @see FlashcardPanel#addRecordingAndFeedbackWidgets(String, LangTestDatabaseAsync, ExerciseController, Panel)
   */
  private Panel getRecordButtonRow(Widget recordButton) {
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
    recoOutput.getElement().setId("recoOutput");

    Panel recoOutputRow = new FluidRow();
    recoOutputRow.getElement().setId("recoOutputRow");

    Panel recoOutputContainer = new Paragraph();
    recoOutputContainer.addStyleName("alignCenter");

    recoOutputRow.add(new Column(12, recoOutputContainer));
    recoOutputContainer.add(recoOutput);

    return recoOutputRow;
  }

  /**
   * @param exerciseID
   * @param service
   * @param controller
   * @param addKeyBinding
   * @param instance
   * @return
   * @see #getAnswerAndRecordButtonRow(String, LangTestDatabaseAsync, ExerciseController)
   */
  private RecordButtonPanel getAnswerWidget(final String exerciseID, LangTestDatabaseAsync service,
                                            ExerciseController controller, final boolean addKeyBinding, String instance) {
    Map<String, Collection<String>> typeToSelection = Collections.emptyMap();
    AudioAnswerListener exercisePanel = this;
    return new FlashcardRecordButtonPanel(exercisePanel, service, controller, exerciseID, 1, instance, typeToSelection) {
      final FlashcardRecordButtonPanel outer = this;
      @Override
      protected RecordButton makeRecordButton(final ExerciseController controller, String buttonTitle) {
       // logger.info("makeRecordButton : using " + instance);
        final FlashcardRecordButton widgets = new FlashcardRecordButton(controller.getRecordTimeout(), this, true,
            addKeyBinding, controller,
            BootstrapExercisePanel.this.instance) {
          @Override
          protected void start() {
            controller.logEvent(this, AVP_RECORD_BUTTON, exerciseID, "Start_Recording");
            super.start();
            recordingStarted();
          }

          @Override
          public void stop() {
            controller.logEvent(this, AVP_RECORD_BUTTON, exerciseID, "Stop_Recording");
            outer.setAllowAlternates(showOnlyEnglish);
            super.stop();
          }

          @Override
          protected void gotLeftArrow() { exerciseList.loadPrev();   }

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

  void recordingStarted() {}

  /**
   * Show progress bar with score percentage, colored by score.
   * Note it has to be wide enough to hold the text "pronunciation score xxx %"
   *
   * @param score
   * @see
   */
  private void showPronScoreFeedback(double score) {  scoreFeedbackRow.add(showScoreFeedback(score));  }


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

  private void clearFeedback() {  scoreFeedbackRow.clear(); }
  private Heading getRecoOutput() { return recoOutput;  }

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
    logger.info("BootstrapExercisePanel.receivedAudioAnswer: correct " + correct + " pron score : " + score +
        " has ref " + hasRefAudio + " bad audio " + badAudioRecording + " result " + result);

    String feedback = "";
    if (badAudioRecording) {
      controller.logEvent(button, "Button", exercise.getID(), "bad recording");
      putBackText();
      if (!realRecordButton.checkAndShowTooLoud(result.getValidity())) {
        //logger.info("receivedAudioAnswer: show popup for " + result.getValidity());
        showPopup(result.getValidity().getPrompt(), button);
      }
      initRecordButton();
      clearFeedback();
    } else {
      long round = Math.round(score * 100f);
      String heard = result.getDecodeOutput();

      if (correct) {
        controller.logEvent(button, "Button", exercise.getID(), "correct response - score " + round);
        showCorrectFeedback(score, heard);
      } else {   // incorrect!!
        controller.logEvent(button, "Button", exercise.getID(), "incorrect response - score " + round);
        feedback = showIncorrectFeedback(result, score, hasRefAudio, heard);
      }
    }
    if (!badAudioRecording && (correct || !hasRefAudio)) {
      logger.info("\treceivedAudioAnswer: correct " + correct + " pron score : " + score + " has ref " + hasRefAudio);
      nextAfterDelay(correct, feedback);
    }
  }

  /**
   * @param html
   * @see #receivedAudioAnswer
   */
  private void showPopup(String html, Widget button) { new PopupHelper().showPopup(html, button, HIDE_DELAY);  }

  /**
   *
   * @param score
   * @see #receivedAudioAnswer(mitll.langtest.shared.AudioAnswer)
   */
  private void showCorrectFeedback(double score, String heard) {
    showPronScoreFeedback(score);
    showOtherText();
    getSoundFeedback().queueSong(SoundFeedback.CORRECT);

   // if (showOnlyEnglish) {
      showHeard(heard);
    //}
  }

  /**
   * @see #showCorrectFeedback(double, String)
   * @see #showIncorrectFeedback(AudioAnswer, double, boolean, String)
   * @param heard
   */
  private void showHeard(String heard) {
    String removedTruth = removePunct(exercise.getForeignLanguage());
    String removedHeard = removePunct(heard);
    if (!removedHeard.equalsIgnoreCase(removedTruth)) {
      logger.info("heard '" + heard + "' '" +removedHeard +
          "'" +
          " vs '" + exercise.getForeignLanguage() + " '" + removedTruth+
          "'"+
          "'");
      Heading recoOutput = getRecoOutput();
      if (recoOutput != null) {
        recoOutput.setText("Heard " + heard);
        recoOutput.getElement().getStyle().setColor("#000000");
      }
    }
  }

  private String removePunct(String t) {
    return t.replaceAll("/", " ").replaceAll(CommentNPFExercise.PUNCT_REGEX, "");
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
  private String showIncorrectFeedback(AudioAnswer result, double score, boolean hasRefAudio,String heard) {
    if (result.isSaidAnswer()) { // if they said the right answer, but poorly, show pron score
      showPronScoreFeedback(score);
      showHeard(heard);
    }
    showOtherText();

    logger.info("showIncorrectFeedback : result " + result + " score " + score + " has ref " + hasRefAudio);

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
   * @see #showIncorrectFeedback
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
  void removePlayingHighlight() {
    removePlayingHighlight(isSiteEnglish() ? english : foreign);
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
   * TODO : whole thing is bogus - we shouldn't just flash the answer up and then move on
   * advance to next should be separate action.
   * @param correct
   * @param feedback make delay dependent on how long the text is
   * @see #receivedAudioAnswer(mitll.langtest.shared.AudioAnswer)
   */
  void nextAfterDelay(boolean correct, String feedback) {
    if (NEXT_ON_BAD_AUDIO) {
      logger.info("doing nextAfterDelay : correct " + correct + " feedback " + feedback);
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
        logger.info("nextAfterDelay Delay is " + incorrectDelay + " len " + feedback.length());
      }
      int delayMillis = controller.getProps().isDemoMode() ? LONG_DELAY_MILLIS : correct ? DELAY_MILLIS : incorrectDelay;
      if (correct && (controlState.isEnglish() || controlState.isForeign())) {
        delayMillis *= 2;
      }
      t.schedule(delayMillis);
    } else {
      logger.info("doing nextAfterDelay : correct " + correct + " feedback " + feedback);

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
  void loadNextOnTimer(final int delay) {
    //logger.info("loadNextOnTimer ----> load next on " + delay);

    if (!preventFutureTimerUse) {
      //if (delay > 100) {
      //  logger.info("loadNextOnTimer ----> load next on " + delay);
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
      //logger.info("\n\n\n----> ignoring next ");
    //}
  }

  private boolean preventFutureTimerUse = false;
  void cancelTimer() {
    removePlayingHighlight();

    preventFutureTimerUse = true;
    if (currentTimer != null) currentTimer.cancel();
  }
  private void initRecordButton() {  answerWidget.initRecordButton();  }

  /**
   * @see #nextAfterDelay(boolean, String)
   */
  void loadNext() {}

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
