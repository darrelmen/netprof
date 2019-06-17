/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.client.dialog;

import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.ProgressBarBase;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.banner.Emoticon;
import mitll.langtest.client.banner.IBanner;
import mitll.langtest.client.banner.NewContentChooser;
import mitll.langtest.client.banner.SessionManager;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.flashcard.SessionStorage;
import mitll.langtest.client.recorder.KeyPressDelegate;
import mitll.langtest.client.recorder.RecordingKeyPressHelper;
import mitll.langtest.client.scoring.IRecordDialogTurn;
import mitll.langtest.client.scoring.RecordDialogExercisePanel;
import mitll.langtest.client.scoring.ScoreProgressBar;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.Validity;
import mitll.langtest.shared.dialog.DialogSession;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.ClientExercise;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

import static com.google.gwt.dom.client.Style.Unit.PCT;
import static com.google.gwt.dom.client.Style.Unit.PX;

/**
 * Created by go22670 on 4/5/17.
 */
public class RehearseViewHelper<T extends RecordDialogExercisePanel>
    extends ListenViewHelper<T>
    implements SessionManager, IRehearseView, KeyPressDelegate {
  private final Logger logger = Logger.getLogger("RehearseViewHelper");

  private static final double HUNDRED = 100.0D;

  private static final String DIALOG_INTRO_SHOWN_REHEARSAL = "dialogIntroShownRehearsal";

  private static final double MAX_RATE_RATIO = 3D;

  private static final int VALUE = -73;

  private static final String THEY_SPEAK = "Listen to";
  private static final String YOU_SPEAK = "Speak";

  private boolean directClick = false;

  /**
   * @see #showScoreFeedback
   * @see #clearScores
   */
  private ProgressBar scoreProgress, rateProgress;
  /**
   *
   */
  private final Emoticon overallSmiley = new Emoticon();
  private static final SafeUri animated = UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "animated_progress48.gif");

  private final Image waitCursor = new Image(animated);

  private final Map<Integer, Float> exToScore = new HashMap<>();
  private final Map<Integer, Float> exToStudentDur = new HashMap<>();
  private final Map<Integer, Float> exToRefDur = new HashMap<>();

  /**
   * @see #currentTurnPlayEnded
   * @see #startRecordingTurn
   */
  private final Set<Integer> exStartedRecording = new HashSet<>();

  private final Map<Integer, T> exToTurn = new HashMap<>();

  private final List<IRecordDialogTurn> recordDialogTurns = new ArrayList<>();

  private final SessionStorage sessionStorage;
  /**
   * @see #showOverallDialogScore()
   * @see ListenViewHelper#showDialogGetRef(int, IDialog, Panel)
   */

  private HTML leftSpeakerHint, rightSpeakerHint;

  private T currentRecordingTurn = null;

  private DialogSession dialogSession = null;
  String rehearsalKey = DIALOG_INTRO_SHOWN_REHEARSAL;

  private static final boolean DEBUG = false;
  private static final boolean DEBUG_PLAY = false;
  private static final boolean DEBUG_RECORDING = false;
  private static final boolean DEBUG_SILENCE = false;
  private static final boolean DEBUG_PLAY_ENDED = false;
  private static final boolean DEBUG_OVERALL_SCORE = false;

  /**
   * @param controller
   * @param thisView
   * @see NewContentChooser#NewContentChooser(ExerciseController, IBanner)
   */
  public RehearseViewHelper(ExerciseController controller, INavigation.VIEWS thisView) {
    super(controller, thisView);
    this.sessionStorage = new SessionStorage(controller.getStorage(), "rehearseSession");
    //  this.rehearsalPrompt = thisView.isPressAndHold() ? HOLD_THE_RED_RECORD_BUTTON : RED_RECORD_BUTTON;
  }

  @Override
  public boolean isPressAndHold() {
    //  boolean pressAndHold = getView().isPressAndHold();
//    logger.info("isPressAndHold for " + getView() + " " + pressAndHold);
    return getView().isPressAndHold();
  }

  /**
   * @return
   * @see RecordDialogExercisePanel#shouldShowRecordButton
   */
  @Override
  public boolean isSimpleDialog() {
    return !isInterpreter();
  }

  @Override
  public void showContent(Panel listContent, INavigation.VIEWS instanceName) {
    super.showContent(listContent, instanceName);

    exToScore.clear();
    exToStudentDur.clear();
    exToRefDur.clear();
    exToTurn.clear();
    recordDialogTurns.clear();
    exStartedRecording.clear();

    currentRecordingTurn = null;

    controller.registerStopDetected(this::mySilenceDetected);
  }

  /**
   * @param dialogID
   * @param dialog
   * @param child
   * @see #showContent
   */
  @Override
  protected void showDialogGetRef(int dialogID, IDialog dialog, Panel child) {
    super.showDialogGetRef(dialogID, dialog, child);
    startSession();
  }

  @NotNull
  @Override
  protected DivWidget getOverallFeedback() {
    final RecordingKeyPressHelper helper = new RecordingKeyPressHelper(null, this, controller);
    helper.addKeyListener(controller);

    DivWidget breadRow = new DivWidget() {
      @Override
      protected void onDetach() {
        super.onDetach();
        safeStop(helper);
      }

      @Override
      protected void onUnload() {
        super.onUnload();
        safeStop(helper);
      }

      private void safeStop(RecordingKeyPressHelper helper) {
        helper.removeListener();
        safeStopRecording();
      }
    };

    helper.setWidget(breadRow);

    styleOverallFeedback(breadRow);
    breadRow.add(showScoreFeedback());

    return breadRow;
  }

  /**
   * @see #getButtonBarChoices
   */
  protected void gotRehearse() {
    super.gotRehearse();
    recordDialogTurns.forEach(IRecordDialogTurn::switchAudioToReference);
  }

  protected void gotHearYourself() {
    super.gotHearYourself();
    recordDialogTurns.forEach(IRecordDialogTurn::switchAudioToStudent);
  }

  @NotNull
  @Override
  protected DivWidget getLeftSpeaker(String firstSpeaker) {
    if (super.isInterpreter()) return super.getLeftSpeaker(firstSpeaker);
    else {
      DivWidget leftSpeaker = super.getLeftSpeaker(firstSpeaker);
      DivWidget container = new DivWidget();
      container.getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);
      container.add(leftSpeaker);
      if (!super.isInterpreter())
        container.add(getLeftHintHTML());

      return container;
    }
  }

  @NotNull
  @Override
  protected DivWidget getRightSpeaker(String secondSpeaker) {
    if (super.isInterpreter()) return super.getRightSpeaker(secondSpeaker);
    else {
      DivWidget rightSpeaker = super.getRightSpeaker(secondSpeaker);
      DivWidget container = new DivWidget();
      container.addStyleName("floatRight");

      container.add(rightSpeaker);
      if (!super.isInterpreter()) {
        container.getElement().getStyle().setMarginTop(VALUE, PX);
        container.add(getRightHintHTML());
      }
      return container;
    }
  }

  private HTML getLeftHintHTML() {
    leftSpeakerHint = new HTML(THEY_SPEAK);
    leftSpeakerHint.addStyleName("floatLeft");

    Style style = leftSpeakerHint.getElement().getStyle();
    style.setClear(Style.Clear.BOTH);
    style.setMarginLeft(5, PX);
    style.setFontStyle(Style.FontStyle.ITALIC);
    return leftSpeakerHint;
  }

  private HTML getRightHintHTML() {
    rightSpeakerHint = new HTML(YOU_SPEAK);
    // rightSpeakerHint.addStyleName("floatRight");

    Style style = rightSpeakerHint.getElement().getStyle();
    style.setMarginLeft(10, PX);
    style.setFontStyle(Style.FontStyle.ITALIC);

    return rightSpeakerHint;
  }

  @NotNull
  private String getLeftHint() {
    //return isLeftSpeakerSet() ? THEY_SPEAK : YOU_SPEAK;
    return THEY_SPEAK;
  }

  @NotNull
  private String getRightHint() {
    //return isRightSpeakerSet() ? THEY_SPEAK : YOU_SPEAK;
    return YOU_SPEAK;
  }

  /**
   * clicking on a turn while rehearsing should mean stop...?
   *
   * @param turn
   */
  @Override
  protected void gotTurnClick(T turn) {
    if (DEBUG) {
      logger.info(" gotTurnClick on " + turn.getExID());
    }

//    String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception("gotTurnClick"));
//    logger.info("logException stack " + exceptionAsString);

    T currentTurn = getCurrentTurn();
    if (isRecordingTurn(currentTurn) && turn == currentTurn) {
      if (DEBUG) logger.info(" gotTurnClick SKIP CLICK on " + turn);

   /*   List<T> respSeq = getRespSeq();
      if (respSeq != null) {
        respSeq.forEach(t -> logger.info("resp turn : " + t));
      }*/
    } else {
      setDirectClick(true);

      int exID = currentTurn == null ? -1 : currentTurn.getExID();
      if (DEBUG) logger.info("gotTurnClick current turn is " + exID);

      if (currentTurn != turn) {
        if (DEBUG) logger.info("gotTurnClick switch to " + turn);
        removeMarkCurrent();
        setCurrentTurn(turn);
        markCurrent();
      }
    }

  }

  private DivWidget moveOnNudge;

  /**
   * Three parts : wait cursor, emoticon for overall score, progress bar for quantitive feedback.
   *
   * @return
   * @see
   */
  private DivWidget showScoreFeedback() {
    DivWidget container = new DivWidget();

    container.setId("feedbackContainerAndBar");

    // style the feedback container
    {
      container.setWidth("78%");

      Style style = container.getElement().getStyle();
      style.setOverflow(Style.Overflow.HIDDEN);
      style.setMarginLeft(20, PCT);
      style.setMarginBottom(20, PX);
    }

    container.add(getWaitCursor());
    container.add(getOverallEmoticon());

    DivWidget progressBarDiv = getProgressBarDiv(scoreProgress = new ProgressBar(ProgressBarBase.Style.DEFAULT));
    container.add(progressBarDiv);
    rateProgress = new ProgressBar(ProgressBarBase.Style.DEFAULT);

    container.add(getProgressBarDiv(rateProgress));
    moveOnNudge = getMoveOnNudge(getNextView());
    moveOnNudge.setVisible(false);
    moveOnNudge.addStyleName("topFiveMargin");
    container.add(moveOnNudge);
    return container;
  }

  @NotNull
  private DivWidget getProgressBarDiv(ProgressBar scoreProgress) {
    DivWidget scoreContainer = new DivWidget();
    scoreContainer.addStyleName("rehearseScoreContainer");
    scoreContainer.addStyleName("floatLeft");

    scoreContainer.addStyleName("topFiveMargin");
    scoreContainer.addStyleName("leftFiveMargin");
    scoreContainer.getElement().getStyle().setMarginBottom(0, PX);
    scoreContainer.add(scoreProgress);

    styleProgressBar(scoreProgress);

    scoreContainer.setWidth("73%");
    return scoreContainer;
  }

  @NotNull
  private DivWidget getOverallEmoticon() {
    DivWidget iconContainer = new DivWidget();

    iconContainer.addStyleName("floatLeft");

    iconContainer.add(overallSmiley);

    iconContainer.setWidth("72px");
    iconContainer.setHeight("72px");

    styleAnimatedSmiley();
    return iconContainer;
  }

  @NotNull
  private DivWidget getWaitCursor() {
    DivWidget iconContainer = new DivWidget();
    iconContainer.addStyleName("floatLeft");

    iconContainer.add(waitCursor);
    hideWaitSpinner();
    waitCursor.setWidth("72px");
    waitCursor.setHeight("72px");
    return iconContainer;
  }

  private void styleAnimatedSmiley() {
    overallSmiley.setWidth("36px");
    overallSmiley.setHeight("36px");
    overallSmiley.setVisible(false);
    overallSmiley.getElement().getStyle().setPosition(Style.Position.RELATIVE);
    overallSmiley.getElement().getStyle().setLeft(18, PX);
    overallSmiley.getElement().getStyle().setTop(18, PX);
  }

  private void styleProgressBar(ProgressBar progressBar) {
    Style style = progressBar.getElement().getStyle();
    style.setMarginTop(5, PX);
    style.setMarginLeft(5, PX);
    style.setMarginBottom(0, PX);

    style.setHeight(25, PX);
    style.setFontSize(16, PX);

    //  progressBar.setWidth(PROGRESS_BAR_WIDTH + "%");
    progressBar.setVisible(false);
  }

  private int beforeCount = 0;

  /**
   * silence analyzer has triggered...
   * Ideally we'd look at packet duration here...
   *
   * @see #showContent
   */
  private void mySilenceDetected() {
    //logger.info("mySilenceDetected got silence : " + currentRecordingTurn);
    boolean hasRecordingTurn = currentRecordingTurn != null;
    if (hasRecordingTurn && currentRecordingTurn.isRecording()) {
      int size = validities.size();
      if (//!validities.isEmpty() &&
          size > 4) {
        Validity lastValidity = validities.get(size - 1);
        Validity penultimateValidity = validities.get(size - 2);
        //   logger.info("mySilenceDetected : last lastValidity was " + lastValidity);
        if (isSilence(penultimateValidity) && isSilence(lastValidity)) {
          if (DEBUG_SILENCE)
            logger.info("mySilenceDetected : OK, server agrees with client side silence detector... have seen " + size);
          gotEndSilenceMaybeStopRecordingTurn();
          beforeCount = 0;
        } else {
          if (beforeCount < size) {

            if (DEBUG_SILENCE) {
              StringBuffer buffer = new StringBuffer();
              //  validities.forEach(validity1 -> buffer.append(validity1).append(" "));
              logger.info("mySilenceDetected : silence for " + currentRecordingTurn +
                  " packets (" + size + ") : " + buffer);
            }

            beforeCount = size;
          }
        }
      } else if (size > 0) {
        if (DEBUG_SILENCE) {
          logger.info("mySilenceDetected : stop recording, num validities " + size);
        }

        gotEndSilenceMaybeStopRecordingTurn();
        beforeCount = 0;
      }
    } else if (DEBUG_SILENCE) {
      if (hasRecordingTurn) {
        logger.info("silenceDetected got silence but current turn is not recording???");
      } else {
        logger.info("silenceDetected got silence but no recording turn?");
      }
    }
  }

  private boolean isSilence(Validity lastValidity) {
    return lastValidity == Validity.TOO_QUIET || lastValidity == Validity.SNR_TOO_LOW || lastValidity == Validity.MIC_DISCONNECTED;
  }

  /**
   * @see #mySilenceDetected()
   */
  private void gotEndSilenceMaybeStopRecordingTurn() {
    if (currentRecordingTurn.gotEndSilenceMaybeStopRecordingTurn()) {
      if (DEBUG) logger.info("mySilenceDetected : stopped " + currentRecordingTurn);
    }
  }

  /**
   * check - is the next turn a recording turn? if so we wait (for now) for the
   * recording to finish completely - we have to send an END and get back a response.
   *
   * @see #stopRecording
   */
  private void recordingHasStopped() {
    if (DEBUG_PLAY_ENDED) {
      logger.info("recordingHasStopped ...");
    }
    moveOnAfterRecordingStopped();
  }

  private void moveOnAfterRecordingStopped() {
    setCurrentRecordingTurn(null);
    currentTurnPlayEnded();
  }


  /**
   * Forget about scores after showing them...
   * <p>
   * Show scores if just recorded the last record turn.
   *
   * @param exid
   * @param score
   * @param recordDialogTurn
   * @return true if we have a score for the last turn
   * @see RecordDialogExercisePanel#addWidgets
   * @see #useResult
   */
  private boolean addScore(int exid, float score, IRecordDialogTurn recordDialogTurn) {
    exToScore.put(exid, score);
    recordDialogTurns.add(recordDialogTurn);

    if (DEBUG) {
      logger.info("addScore exid " + exid +
          " score " + score +
          "\n\tnow ex->score (" + exToScore.keySet().size() + ") : " + exToScore.keySet() +
          "\n\tvs expected   (" + getRespSeq().size() + ") : " + getRespSeq());
    }

    return checkAtEnd();
  }

  /**
   * So if the response is the last turn, go ahead and show the scores
   * <p>
   * as long as we have the last score, we're OK to show the scores and summary - we could start part way into the dialog...
   *
   * @return true if we have a score for the last turn
   * @see #addScore(int, float, IRecordDialogTurn)
   */
  private boolean checkAtEnd() {
    boolean hasLast = doWeHaveTheLastResponseScore();
    int numScores = exToScore.size();
    int numResponses = getRespSeq().size();
    if (hasLast) {// && numScores == numResponses) {
      if (DEBUG_PLAY_ENDED) {
        logger.info("checkAtEnd : hasLast show scores! score " + hasLast + " and # known scores = " + numScores + " vs " + numResponses);
      }

      //  showScores();
      return true;
    } else {
      if (numScores > numResponses) {
        logger.warning("\n\n\nhuh? something is wrong! checkAtEnd : hasLast score " + hasLast + " and # known scores = " + numScores + " vs " + numResponses);
      } else {
        if (DEBUG_PLAY_ENDED) {
          logger.info("checkAtEnd : hasLast score " + hasLast + " and # known scores = " + numScores + " vs " + numResponses);
        }
      }
      return false;
    }
  }

  /**
   * @see #checkAtEnd()
   * @see ListenViewHelper#currentTurnPlayEnded()
   */
  private void showScores() {
    if (overallSmiley.isVisible()) {
      logger.info("showScores - skip!");
    } else {
      showOverallDialogScore();
      recordDialogTurns.forEach(IRecordDialogTurn::showScoreInfo);
    }
  }


  /**
   * Move current turn to first turn when we switch who is the prompting speaker.
   */
  void gotSpeakerChoice() {
    makeFirstTurnCurrent();

    if (!isInterpreter()) {
      setHints();
    }
  }

  private void setHints() {
    leftSpeakerHint.setHTML(getLeftHint());
    rightSpeakerHint.setHTML(getRightHint());
  }

  private void makeFirstTurnCurrent() {
    removeMarkCurrent();
    setCurrentTurn(leftTurnPanels.get(0));
    markCurrent();
  }

  /**
   * If we're recording and we hit one of the forward/backward turns, stop recording right there...
   */
  @Override
  void gotBackward() {
    super.gotBackward();
    safeStopRecording();
  }

  @Override
  public void gotForward(T editorTurn) {
    super.gotForward(editorTurn);
    safeStopRecording();
  }

  private void safeStopRecording() {
    if (isRecording()) {
      currentRecordingTurn.cancelRecording();
    }
  }

  private boolean isRecording() {
    return currentRecordingTurn != null && currentRecordingTurn.isRecording();
  }

  /**
   * @param isRight
   * @param clientExercise
   * @param prevColumn
   * @param index
   * @return
   * @see #getTurnPanel
   */
  @NotNull
  @Override
  protected T reallyGetTurnPanel(ClientExercise clientExercise, COLUMNS columns, COLUMNS prevColumn, int index) {
    T turnPanel = getRecordingTurnPanel(clientExercise, columns, prevColumn);
    exToTurn.put(clientExercise.getID(), turnPanel);
    return turnPanel;
  }

  protected T getRecordingTurnPanel(ClientExercise clientExercise, COLUMNS columns, COLUMNS prevColumn) {
    T widgets = makeRecordingTurnPanel(clientExercise, columns, prevColumn);

//    if (columns == ITurnContainer.COLUMNS.MIDDLE) {
//      widgets.addStyleName("inlineFlex");
//    }

    //  widgets.setWidth("100%");
    return widgets;
  }

  protected T makeRecordingTurnPanel(ClientExercise clientExercise, COLUMNS columns, COLUMNS prevColumn) {
    return (T) new RecordDialogExercisePanel(clientExercise, controller,
        null, alignments, this, this, columns, prevColumn);
  }

  /**
   * TODO : way too complicated!
   * <p>
   * If a turn is recording, then cancel recording
   * If a turn is playing audio, pause it.
   * If a turn is paused, play the audio.
   * If a turn has completed playing and it's the last one, maybe we don't have a current turn anymore???
   *
   * @see ListenViewHelper#getControls
   */
  @Override
  protected void gotClickOnPlay() {
    setDirectClick(false);
    firstStepsWhenPlay();

    if (DEBUG_PLAY) {
      logger.info("gotClickOnPlay " +
          (getCurrentTurn() == null ?
              "no current " :
              "current is " + getCurrentTurn().getExID() + " " + getCurrentTurn().getText()));
    }

    boolean onARecordingTurn = getCurrentTurn() != null && isCurrentTurnARecordingTurn();
    if (onARecordingTurn && getCurrentTurn().isRecording()) {  // cancel current recording
      if (DEBUG_PLAY) {
        logger.info("gotClickOnPlay on recording turn - so abort!");
      }

      setPlayButtonToPlay();
      getCurrentTurn().cancelRecording();
    } else {
      if (isDoRehearse()) {
        if (DEBUG_PLAY) {
          logger.info("gotClickOnPlay do rehearsal ");
        }
        if (isSessionGoingNow()) {
          if (DEBUG_PLAY) {
            logger.info("gotClickOnPlay session started... ");
          }
          maybeClearScores();

          if (onARecordingTurn) {
            getCurrentTurn().enableRecordButton();
          } else {
            playCurrentTurn();
          }
        } else {
          if (DEBUG_PLAY) {
            logger.info("gotClickOnPlay no session started -- must be a pause/stop!");
          }

          if (onARecordingTurn) {
            getCurrentTurn().disableRecordButton();
          }

//          if (isFirstPrompt(getCurrentTurn())) {
//            doRecordingNoticeMaybe();
//          } else {
          //   rehearseTurn();
          //}
        }
      } else {  // playback your audio
        if (DEBUG_PLAY) {
          logger.info("gotClickOnPlay play current turn ");
        }
        ifOnLastJumpBackToFirst();
        playCurrentTurn();
      }
    }
  }

  private void maybeClearScores() {
    if (overallSmiley.isVisible()) {   // start over
      sessionStorage.storeSession();
      clearScores();
    }
  }

  private boolean isCurrentTurnARecordingTurn() {
    return !isTurnAPrompt();
  }

  private boolean isTurnAPrompt() {
    return !isRecordingTurn(getCurrentTurn());
  }

  private boolean isRecordingTurn(T currentTurn) {
    return getRespSeq().contains(currentTurn);
  }

  @Override
  protected void setNextTurnForSide() {
    if (!isDoRehearse()) { // i.e. in playback
      if (onLastTurn()) {
        if (DEBUG) logger.info("setNextTurnForSide : wrap around ?");
        super.setNextTurnForSide();
      } else {
        logger.info("setNextTurnForSide : skip - not on last");
      }
    }
  }

  /**
   * @see #gotClickOnPlay()
   */
  void clearScores() {
    overallSmiley.setVisible(false);
    overallSmiley.removeStyleName("animation-target");

    scoreProgress.setVisible(false);
    rateProgress.setVisible(false);
    exToScore.clear();
    exToStudentDur.clear();

    getAllTurns().forEach(IRecordDialogTurn::clearScoreInfo);
    recordDialogTurns.clear();
    moveOnNudge.setVisible(false);
  }

  @Override
  public int getDialogSessionID() {
    int i = dialogSession == null ? -1 : dialogSession.getID();
    //   logger.info("getDialogSessionID #" + i);
    return i;
  }

  /**
   * @see #startRecordingTurn
   * @see #showOverallDialogScore
   */
  private void startSession() {
    int dialogID = getDialogID();
    if (dialogID == -1) {
      logger.warning("startSession huh? dialog id is  " + dialogID + "???\n\n\n\n");
    } else {
      setSession(dialogID);
    }
  }

  /**
   * What if we log out or something in the middle here?
   */
  private void setSession(int dialogID) {
    int projectid = controller.getProjectStartupInfo() == null ? -1 : controller.getProjectStartupInfo().getProjectid();
    dialogSession = new DialogSession(
        controller.getUser(),
        projectid,
        dialogID,
        getView());

    controller.getDialogService().addSession(dialogSession, new AsyncCallback<Integer>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("creating new dialog session", caught);
      }

      @Override
      public void onSuccess(Integer result) {
        if (result > 0) {
          //  logger.info("startSession : made new session = " + result);
          dialogSession.setID(result);
        } else {
          logger.warning("setSession invalid req " + dialogSession);
          String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception("setSession"));
          logger.info("logException stack " + exceptionAsString);
          //    controller.getNavigation().showInitialState();

        }
      }
    });
  }

  /**
   * @see #playStopped()
   * @see #gotEndSilenceMaybeStopRecordingTurn()
   */
  protected void currentTurnPlayEnded() {
    if (directClick) {
      setDirectClick(false);
      if (DEBUG_PLAY_ENDED) logger.info("currentTurnPlayEnded direct click ");
      markCurrent();
    } else {
      T currentTurn = getCurrentTurn();
      if (DEBUG_PLAY_ENDED) logger.info("currentTurnPlayEnded (rehearse) - turn " + currentTurn);

      boolean isCurrentPrompt = isTurnAPrompt(currentTurn);

      int i2 = getAllTurns().indexOf(currentTurn);
      int nextOtherSide = i2 + 1;

      if (DEBUG_PLAY_ENDED) {
        logger.info("currentTurnPlayEnded" +
            "\n\tcurrent a prompt? " + isCurrentPrompt +
            "\n\tcurrent index     " + i2 + " = " + currentTurn
        );
      }

      if (!isDoRehearse() && !isCurrentPrompt) {  // if playback of scored turn just finished, show the score again...
        //   logger.info("\n\ncurrentTurnPlayEnded showScoreInfo on " + currentTurn);
        currentTurn.revealScore();
      } else {
        // logger.info("currentTurnPlayEnded NOT DOING showScoreInfo on " + currentTurn);
      }

      if (nextOtherSide < getAllTurns().size()) {  // not on the last turn
        notOnTheLastTurn(nextOtherSide);
      } else {  // on the last turn!
        onTheLastTurn(currentTurn, isCurrentPrompt);
      }
    }
  }

  private void notOnTheLastTurn(int nextOtherSide) {
    T nextTurn = getAllTurns().get(nextOtherSide);

    if (DEBUG) logger.info("currentTurnPlayEnded next is " + nextTurn + "  at " + nextOtherSide);
    showNextTurn(nextTurn);

    if (isTurnAPrompt(nextTurn)) {
      if (DEBUG_PLAY_ENDED) logger.info("currentTurnPlayEnded - play next " + nextTurn);
      playCurrentTurn();
    } else {
      if (DEBUG_PLAY_ENDED) logger.info("currentTurnPlayEnded - startRecording " + nextTurn);
      startRecordingTurn(nextTurn); // advance automatically
    }
  }

  private void onTheLastTurn(T currentTurn, boolean isCurrentPrompt) {
    if (isCurrentPrompt) {
      logger.info("currentTurnPlayEnded - isCurrentPrompt " + isCurrentPrompt);

      // TODO :
      //  a race - does the play end before the score is available, or is the score available before the play ends?
      if (doWeHaveTheLastResponseScore()) {
        if (DEBUG_PLAY_ENDED) {
          logger.info("currentTurnPlayEnded - on last " + currentTurn);
        }
        if (isDoRehearse()) {
          showScores();
        } else {
          if (DEBUG_PLAY_ENDED) {
            logger.info("currentTurnPlayEnded - maybe show first turn ??? " + getAllTurns().get(0));
          }
          showFirstTurn();
        }
        setPlayButtonToPlay();
      } else if (!getRespSeq().isEmpty()) { // could be we don't have any response turns yet.
        if (DEBUG_PLAY_ENDED)
          logger.info("currentTurnPlayEnded - no score for " + currentTurn.getExID() + " know about " + exToScore.keySet() + " exercises so waiting...");

        if (haveRecordedAllTurns()) {
          showWaitSpiner();  // wait for it
        }
      } else {  // no resp turns
        showFirstTurn();
        setPlayButtonToPlay();
      }
    } else {
      if (isDoRehearse()) {
        if (DEBUG_PLAY_ENDED) logger.info("currentTurnPlayEnded skip last (why?) " + currentTurn);
      } else {  // ok, we're doing hear yourself, so make sure we set the first turn to be current and reset the play button state
        showFirstTurn();
        setPlayButtonToPlay();
      }
    }
  }

  private void showFirstTurn() {
    showNextTurn(getAllTurns().get(0));
  }

  private void setDirectClick(boolean b) {
    directClick = b;
  }

  private boolean haveRecordedAllTurns() {
    return exStartedRecording.size() == getRespSeq().size();
  }

  private void showNextTurn(T nextTurn) {
    removeMarkCurrent();
    setCurrentTurn(nextTurn);
    markCurrent();

    makeCurrentTurnVisible();
  }

  private boolean isTurnAPrompt(T currentTurn) {
    return getPromptSeq().contains(currentTurn);
  }

  private boolean doWeHaveTheLastResponseScore() {
    List<T> respSeq = getRespSeq();
    return !respSeq.isEmpty() && exToScore.containsKey(respSeq.get(respSeq.size() - 1).getExID());
  }

  /**
   * TODO : don't keep validities here - keep them on each turn
   *
   * @param toStart
   * @see #gotClickOnPlay()
   * @see ListenViewHelper#currentTurnPlayEnded()
   */
  private void startRecordingTurn(T toStart) {
    if (isDoRehearse()) {
      if (dialogSession == null) {
        startSession();
      }

      exStartedRecording.add(toStart.getExID());

      toStart.startRecording();
      setCurrentRecordingTurn(toStart);

      if (DEBUG) logger.info("startRecordingTurn : " + currentRecordingTurn);

      validities.clear();
    } else {
      playCurrentTurn();
    }
  }

  private void setCurrentRecordingTurn(T toStart) {
    //  logger.info("\n\n\nsetCurrentRecordingTurn BEFORE : " + toStart);
    currentRecordingTurn = toStart;
    if (DEBUG) logger.info("setCurrentRecordingTurn : " + currentRecordingTurn);
  }

  private void showWaitSpiner() {
    waitCursor.setVisible(isDoRehearse());
  }

  private void hideWaitSpinner() {
    waitCursor.setVisible(false);
  }

  /**
   * @see #showScores
   */
  private void showOverallDialogScore() {
    hideWaitSpinner();
    setPlayButtonToPlay();
    makeVisible(overallFeedback);

    int num = exToScore.values().size();

    double total = getTotalScore();
    double studentTotal = getTotal(this.exToStudentDur);
    double refTotal = getTotal(this.exToRefDur);

    if (DEBUG_OVERALL_SCORE)
      logger.info("showOverallDialogScore student total " + studentTotal + " vs ref " + refTotal + " duration.");

    total = setScoreProgressLevel(total, num);

    {
      double totalRatio = refTotal == 0D ? 0D : studentTotal / (MAX_RATE_RATIO * refTotal);
      //double totalAvgRate = totalRatio / ((float) num);
      if (DEBUG_OVERALL_SCORE)
        logger.info("showOverallDialogScore avg rate " + studentTotal + " vs " + refTotal + " = " + totalRatio);
      setRateProgress(totalRatio);
    }


    if (refTotal > 0) {
      double actualRatio = studentTotal / refTotal;

      float v = roundToTens(actualRatio);
      if (DEBUG_OVERALL_SCORE) logger.info("showOverallDialogScore rate to show " + v + " vs raw " + actualRatio);
      rateProgress.setText("Rate " + v + "x");
    }

    rateProgress.setVisible(refTotal > 0);

    overallSmiley.setEmoticon(total, controller.getLanguageInfo());

    overallSmiley.setVisible(true);
    overallSmiley.addStyleName("animation-target");
    //  hearYourself.setEnabled(true);
    setHearYourself(true);

    startSession();  // OK, start a new session

    if (DEBUG) logger.info("total score " + total);

    moveOnNudge.setVisible(total > 0.5);
  }

  private float roundToTens(double totalHours) {
    return ((float) ((Math.round(totalHours * 10d)))) / 10f;
  }

  /**
   * @param total
   * @param num
   * @return ratio of total to num
   */
  private double setScoreProgressLevel(double total, int num) {
    ProgressBar scoreProgress = this.scoreProgress;

    total /= (float) num;
    if (DEBUG) logger.info("showOverallDialogScore total   " + total + " vs " + num);

    double percent = total * HUNDRED;
    double round = percent;// Math.max(percent, 30);
    if (percent == 0D) round = HUNDRED;

    {
      double percent1 = num == 0 ? HUNDRED : percent;
      percent1 = Math.max(percent1, 40.0D);
      //  scoreProgress.setPercent(percent1);
      cheesySetPercent(scoreProgress, percent1);
    }

    scoreProgress.setVisible(true);

    {
      long round1 = Math.round(percent);
      if (DEBUG) logger.info("showing " + round1 + "%");
      scoreProgress.setText("Score " + round1 + "%");
    }

    new ScoreProgressBar(false).setColor(scoreProgress, total, round);
    return total;
  }

  /**
   * @param total
   * @see #showOverallDialogScore
   */
  private void setRateProgress(double total) {
    ProgressBar scoreProgress = this.rateProgress;
    scoreProgress.setVisible(true);

    double percent1 = (1F - total);
    //   logger.info("setRateProgress score to color " + percent1);
    // new ScoreProgressBar(false).setColor(scoreProgress, percent1);

    if (percent1 < 0.4) scoreProgress.setColor(ProgressBarBase.Color.SUCCESS);
    else if (percent1 < 0.6) scoreProgress.setColor(ProgressBarBase.Color.INFO);
    else if (percent1 < 0.8) scoreProgress.setColor(ProgressBarBase.Color.WARNING);
    else scoreProgress.setColor(ProgressBarBase.Color.DANGER);

    {
      double percent = total * 100D;
      //   logger.info("setRateProgress percent " + total + " vs " + percent);
      //scoreProgress.setPercent(Math.max(33.0D, percent));
      cheesySetPercent(scoreProgress, Math.max(33.0D, percent));
    }
  }

  @NotNull
  private void cheesySetPercent(ComplexPanel practicedProgress, double percent1) {
    Widget theBar = practicedProgress.getWidget(0);
    theBar.getElement().getStyle().setWidth(Double.valueOf(percent1).intValue(), Style.Unit.PCT);
    // return theBar;
  }

  private double getTotalScore() {
    return getTotal(exToScore);
  }

  private double getTotal(Map<Integer, Float> exToStudentDur) {
    double total = 0D;
    for (Float score : exToStudentDur.values()) {
      total += score.doubleValue();
    }
    return total;
  }

  /**
   * @see mitll.langtest.client.scoring.ContinuousDialogRecordAudioPanel#stopRecording()
   */
  @Override
  public void stopRecording() {
    List<T> allTurns = getAllTurns();
    if (getCurrentTurn() == allTurns.get(allTurns.size() - 1) && !exToScore.isEmpty()) {
      waitCursor.setVisible(true);
    }
//    logger.info("stopRecording received!");

//    String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception("stopRecording"));
//    logger.info("logException stack " + exceptionAsString);

    recordingHasStopped();
  }

  private final List<Validity> validities = new ArrayList<>();

  @Override
  public void addPacketValidity(Validity validity) {
    validities.add(validity);
  }

  @Override
  public int getNumValidities() {
    return validities.size();
  }

  /**
   * Race!
   * <p>
   * Answers may come much later - we need to find the corresponding turn...
   *
   * @param audioAnswer
   * @see mitll.langtest.client.scoring.ContinuousDialogRecordAudioPanel#useResult
   */
  @Override
  public void useResult(AudioAnswer audioAnswer) {
    int exid = audioAnswer.getExid();
    T matchingTurn = getTurnForID(exid);

    if (DEBUG_RECORDING) {
      logger.info("useResult set answer" +
          "\n\ton " + matchingTurn +
          "\n\tto " + audioAnswer);
    }

    matchingTurn.useResult(audioAnswer);

    exToStudentDur.put(exid, matchingTurn.getStudentSpeechDur());
    exToRefDur.put(exid, matchingTurn.getRefSpeechDur());

    boolean atEnd = addScore(exid, (float) audioAnswer.getScore(), matchingTurn);

    if (atEnd) {
      if (DEBUG_PLAY_ENDED) logger.info("useResult at end...");
      if (waitCursor.isVisible() || isLast(matchingTurn)) {
        maybeShowScores();
      }
    } else if (DEBUG_RECORDING) logger.info("no at end...");
  }

  private void maybeShowScores() {
    if (isDoRehearse()) {  // or could happen in currentTurnPlayEnded
      showScores();
    }
  }

  /**
   * @param exid
   * @see RecordDialogExercisePanel.ContinuousDialogRecordAudioPanel#useInvalidResult(int, boolean)
   */
  @Override
  public void useInvalidResult(int exid) {
    if (DEBUG_RECORDING) {
      logger.info("useInvalidResult " +
          "\n\texid " + exid);
    }

    T matchingTurn = getTurnForID(exid);
    boolean atEnd = addScore(exid, 0F, matchingTurn);
    matchingTurn.useInvalidResult();

    if (!isPressAndHold() && atEnd) {
      moveOnAfterRecordingStopped();

      if (waitCursor.isVisible()) {
        maybeShowScores();
      }
    } else if (atEnd) {
      if (waitCursor.isVisible()) {
        maybeShowScores();
      }
    }
  }

  private T getTurnForID(int exid) {
    return exToTurn.get(exid);
  }

  @Override
  public String getSession() {
    return "" + sessionStorage.getSession();
  }

  @Override
  public void gotRightArrow() {
    gotForward(null);
  }

  @Override
  public void gotLeftArrow() {
    gotBackward();
  }

  @Override
  public void gotUpArrow() {

  }

  @Override
  public void gotDownArrow() {

  }

  @Override
  public void gotEnter() {

  }

  @Override
  public void stopRecordingSafe() {
    T currentTurn = getCurrentTurn();
    if (currentTurn.isPushToTalk()) {
      currentTurn.stopRecordingSafe();
    }
  }

  protected boolean mouseDown = false;

  @Override
  public void gotSpaceBar() {
    if (!mouseDown) {
      mouseDown = true;

      T currentTurn = getCurrentTurn();
      if (currentTurn.isPushToTalk()) {
        currentTurn.reallyStartOrStopRecording();
      }
    }
  }

  @Override
  public void gotSpaceBarKeyUp() {
    if (!mouseDown) {
      logger.warning("huh? mouse down = false");
    } else {
      mouseDown = false;
      T currentTurn = getCurrentTurn();
      if (currentTurn.isPushToTalk()) {
        currentTurn.reallyStartOrStopRecording();
      }
    }
  }

  /**
   * @param exercises
   * @param afterThisTurn
   * @see DialogEditor#getAsyncForNewTurns
   */
  void addTurns(IDialog updated, int exid) {
    this.setDialog(updated);

    addAllTurns(getDialog(), turnContainer);

    List<T> allTurns = getAllTurns();
    T turnByID = getTurnByID(exid);

    T next = getCurrentTurn();
    if (turnByID == null) {
      logger.warning("addTurns : can't find exid " + exid);
    } else {
      //  T current = collect.get(0);
      //   int i = allTurns.indexOf(turnByID) + 1;
      next = allTurns.get(allTurns.indexOf(turnByID) + 1);
    }

    final T fnext = next;

    Scheduler.get().scheduleDeferred(() -> {
      logger.info("addTurns : focus will be on " + fnext);

      setCurrentTurn(fnext);
      markCurrent();
      fnext.grabFocus();
    });
    //   addTurnForEachExercise(turnContainer, getFirstSpeakerLabel(dialog), getSecondSpeakerLabel(dialog), exercises);
  }

}
