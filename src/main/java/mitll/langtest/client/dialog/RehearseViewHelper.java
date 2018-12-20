package mitll.langtest.client.dialog;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.ProgressBarBase;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.banner.*;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.flashcard.SessionStorage;
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
    implements SessionManager, IRehearseView {
  private final Logger logger = Logger.getLogger("RehearseViewHelper");

  private static final double MAX_RATE_RATIO = 3D;

  private static final int PROGRESS_BAR_WIDTH = 49;

  private static final String REHEARSE = "Rehearse";
  /**
   *
   */
  private static final String HEAR_YOURSELF = "Hear yourself";
  private static final int VALUE = -98;

  private static final String THEY_SPEAK = "Listen to : ";
  private static final String YOU_SPEAK = "Speak : ";

  private static final boolean DEBUG = false;
  private static final boolean DEBUG_SILENCE = false;
  private static final int TOP_TO_USE = 10;

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
  private DivWidget overallFeedback;
  private HTML leftSpeakerHint, rightSpeakerHint;

  private T currentRecordingTurn = null;
  private boolean doRehearse = true;

  private DialogSession dialogSession = null;

  /**
   * @param controller
   * @see NewContentChooser#NewContentChooser(ExerciseController, IBanner)
   */
  public RehearseViewHelper(ExerciseController controller) {
    super(controller);
    this.sessionStorage = new SessionStorage(controller.getStorage(), "rehearseSession");
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
    child.add(overallFeedback = getOverallFeedback());
    startSession();
  }

  @NotNull
  private DivWidget getOverallFeedback() {
    DivWidget breadRow = new DivWidget();
    breadRow.getElement().setId("overallFeedbackRow");

    Style style = breadRow.getElement().getStyle();
    style.setMarginTop(10, PX);
    style.setMarginBottom(10, PX);
    style.setClear(Style.Clear.BOTH);

    breadRow.addStyleName("cardBorderShadow");
    // breadRow.setWidth(WIDTH);  //??? why 97???
    breadRow.add(showScoreFeedback());

    return breadRow;
  }

  @NotNull
  @Override
  protected INavigation.VIEWS getPrevView() {
    return INavigation.VIEWS.LISTEN;
  }

  @NotNull
  @Override
  protected INavigation.VIEWS getNextView() {
    return INavigation.VIEWS.PERFORM;
  }

  @NotNull
  @Override
  protected DivWidget getControls() {
    DivWidget controls = super.getControls();
    controls.add(getButtonBarChoices());
    return controls;
  }

  private Button rehearseChoice;
  private Button hearYourself;

  private Widget getButtonBarChoices() {
    ButtonToolbar toolbar = new ButtonToolbar();
//    toolbar.getElement().setId("Choices_" + type);
    toolbar.getElement().getStyle().setClear(Style.Clear.BOTH);
    styleToolbar(toolbar);

    {
      ButtonGroup buttonGroup = new ButtonGroup();
      toolbar.add(buttonGroup);

      {
        rehearseChoice = getChoice(buttonGroup, REHEARSE, event -> gotRehearse());
        rehearseChoice.setActive(true);
        rehearseChoice.setType(ButtonType.DEFAULT);
        buttonGroup.add(rehearseChoice);
        doRehearse = true;
      }

      {
        hearYourself = getChoice(buttonGroup, HEAR_YOURSELF, event -> gotHearYourself());
        hearYourself.setActive(false);
        hearYourself.setEnabled(false);
        hearYourself.setType(ButtonType.DEFAULT);
        buttonGroup.add(hearYourself);
      }
    }
    return toolbar;
  }

  /**
   * @see #getButtonBarChoices
   */
  private void gotRehearse() {
    recordDialogTurns.forEach(IRecordDialogTurn::switchAudioToReference);
    doRehearse = true;
    rehearseChoice.setActive(true);
    hearYourself.setActive(false);
  }

  private void gotHearYourself() {
    recordDialogTurns.forEach(IRecordDialogTurn::switchAudioToStudent);
    doRehearse = false;
    if (DEBUG) logger.info("gotHearYourself : doRehearse = " + doRehearse);
    rehearseChoice.setActive(false);
    hearYourself.setActive(true);
  }

  private Button getChoice(ButtonGroup buttonGroup, final String text, ClickHandler handler) {
    Button onButton = new Button(text);
    configure(handler, onButton);
    buttonGroup.add(onButton);
    return onButton;
  }

  private void configure(ClickHandler handler, Button onButton) {
    onButton.setType(ButtonType.INFO);
    onButton.addClickHandler(handler);
    onButton.setActive(false);
  }


  private void styleToolbar(ButtonToolbar toolbar) {
    Style style = toolbar.getElement().getStyle();
    style.setMarginTop(TOP_TO_USE, PX);
    style.setMarginBottom(TOP_TO_USE, PX);
  }

  @NotNull
  protected DivWidget getLeftSpeakerDiv(CheckBox checkBox) {
    if (isInterpreter) {
      return super.getLeftSpeakerDiv(checkBox);
    } else {
      DivWidget rightDiv = new DivWidget();
      checkBox.getElement().getStyle().setClear(Style.Clear.BOTH);

      {
        leftSpeakerHint = new HTML(THEY_SPEAK);
        leftSpeakerHint.addStyleName("floatLeft");

        Style style = leftSpeakerHint.getElement().getStyle();
        style.setClear(Style.Clear.BOTH);
        style.setMarginLeft(41, PX);

        rightDiv.add(leftSpeakerHint);
      }
      rightDiv.add(checkBox);

      return rightDiv;
    }
  }

  @NotNull
  DivWidget getRightSpeakerDiv(CheckBox checkBox) {
    if (isInterpreter) {
      return super.getRightSpeakerDiv(checkBox);
    } else {
      DivWidget rightDiv = new DivWidget();

      {
        rightSpeakerHint = new HTML(YOU_SPEAK);
        rightSpeakerHint.addStyleName("floatRight");

        Style style = rightSpeakerHint.getElement().getStyle();
        style.setMarginRight(4, PX);
        style.setMarginTop(VALUE, PX);

        rightDiv.add(rightSpeakerHint);
      }
      checkBox.getElement().getStyle().setClear(Style.Clear.BOTH);
      rightDiv.add(checkBox);

      return rightDiv;
    }
  }

  @Override
  protected CheckBox addRightSpeaker(DivWidget rowOne, String label) {
    if (isInterpreter) {
      return super.addRightSpeaker(rowOne, label);
    } else {
      CheckBox checkBox = super.addRightSpeaker(rowOne, label);
      checkBox.getElement().getStyle().setMarginTop(-74, PX);
      return checkBox;
    }
  }

  @NotNull
  private String getLeftHint() {
    return isLeftSpeakerSet() ? THEY_SPEAK : YOU_SPEAK;
  }

  @NotNull
  private String getRightHint() {
    return isRightSpeakerSet() ? THEY_SPEAK : YOU_SPEAK;
  }

  private boolean directClick = false;

  @Override
  protected void gotTurnClick(T turn) {
    directClick = true;
    super.gotTurnClick(turn);
  }

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

    progressBar.setWidth(PROGRESS_BAR_WIDTH + "%");
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
      if (!validities.isEmpty() && validities.size() > 4) {
        Validity lastValidity = validities.get(validities.size() - 1);
        Validity penultimateValidity = validities.get(validities.size() - 2);
        //   logger.info("mySilenceDetected : last lastValidity was " + lastValidity);
        if (isSilence(penultimateValidity) && isSilence(lastValidity)) {
          if (DEBUG_SILENCE)
            logger.info("mySilenceDetected : OK, server agrees with client side silence detector... have seen " + validities.size());
          stopRecordingTurn();
          beforeCount = 0;
        } else {
          if (beforeCount < validities.size()) {

            if (DEBUG_SILENCE) {
              StringBuffer buffer = new StringBuffer();
              validities.forEach(validity1 -> buffer.append(validity1).append(" "));
              logger.info("mySilenceDetected : silence for " + currentRecordingTurn +
                  " packets (" + validities.size() + ") : " + buffer);
            }

            beforeCount = validities.size();
          }
        }
      } else {
        if (DEBUG_SILENCE) {
          logger.info("mySilenceDetected : stopRecordingTurn, num validities " + validities.size());
        }

        stopRecordingTurn();
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
  private void stopRecordingTurn() {
    if (currentRecordingTurn.stopRecording()) {
      if (DEBUG) logger.info("mySilenceDetected : stopped " + currentRecordingTurn);
      //recordingHasStopped();
    }
  }

  /**
   * check - is the next turn a recording turn? if so we wait (for now) for the
   * recording to finish completely - we have to send an END and get back a response.
   */
  private void recordingHasStopped() {
    if (isNextTurnAPrompt(getCurrentTurn())) {
      // logger.info("recordingHasStopped OK, next turn is a prompt!");
      moveOnAfterRecordingStopped();
    } else {
      logger.info("recordingHasStopped next turn not a prompt so not advancing...");
    }
  }

  private void moveOnAfterRecordingStopped() {
    setCurrentRecordingTurn(null);
    currentTurnPlayEnded(true);
  }

  private boolean isNextTurnAResp(T currentTurn) {
    return !isNextTurnAPrompt(currentTurn);
  }

  private boolean isNextTurnAPrompt(T currentTurn) {
    int i2 = allTurns.indexOf(currentTurn);
    int nextOtherSide = i2 + 1;
    if (nextOtherSide < allTurns.size()) {
      T nextTurn = allTurns.get(nextOtherSide);
      return (isTurnAPrompt(nextTurn));
    } else {
      return true;
    }
  }

  /**
   * Forget about scores after showing them...
   *
   * Show scores if just recorded the last record turn.
   *
   * @param exid
   * @param score
   * @param recordDialogTurn
   * @see RecordDialogExercisePanel#addWidgets
   * @see #useResult(AudioAnswer)
   */

  private void addScore(int exid, float score, IRecordDialogTurn recordDialogTurn) {
    exToScore.put(exid, score);
    recordDialogTurns.add(recordDialogTurn);

    if (DEBUG) {
      logger.info("addScore exid " + exid +
          " score " + score +
          "\n\tnow ex->score (" + exToScore.keySet().size() + ") : " + exToScore.keySet() +
          "\n\tvs expected   (" + getRespSeq().size() + ") : " + getRespSeq());
    }

    checkAtEnd();
  }

  /**
   * So if the response is the last turn, go ahead and show the scores
   *
   * @see #addScore(int, float, IRecordDialogTurn)
   */
  private void checkAtEnd() {
    boolean hasLast = doWeHaveTheLastResponseScore();
    int numScores = exToScore.size();
    int numResponses = getRespSeq().size();
    if (hasLast && numScores == numResponses) {
      if (DEBUG)
        logger.info("checkAtEnd : hasLast show scores! score " + hasLast + " and # known scores = " + numScores + " vs " + numResponses);
      showScores();
    } else {
      if (numScores > numResponses) {
        logger.warning("\n\n\nhuh? something is wrong! checkAtEnd : hasLast score " + hasLast + " and # known scores = " + numScores + " vs " + numResponses);
      } else {
        if (DEBUG)
          logger.info("checkAtEnd : hasLast score " + hasLast + " and # known scores = " + numScores + " vs " + numResponses);
      }
    }
  }

  /**
   * @see #checkAtEnd()
   * @see ListenViewHelper#currentTurnPlayEnded(boolean)
   */
  private void showScores() {
    if (!overallSmiley.isVisible()) {
      showOverallDialogScore();
      recordDialogTurns.forEach(IRecordDialogTurn::showScoreInfo);
      setPlayButtonToPlay();
      setCurrentTurn(getPromptSeq().get(0));
    }
  }

  protected void setRightTurnInitialValue(CheckBox checkBox) {
  }

  /**
   * On user click of speaker.
   *
   * @param value
   * @see #addLeftSpeaker
   */
  protected void speakerOneCheck(Boolean value) {
    setRightSpeaker(!value);
    gotSpeakerChoice();
  }

  /**
   * On user click of speaker.
   *
   * @param value
   */
  protected void speakerTwoCheck(Boolean value) {
    setLeftSpeaker(!value);
    gotSpeakerChoice();
  }

  /**
   * Move current turn to first turn when we switch who is the prompting speaker.
   */
  void gotSpeakerChoice() {
    setPlayButtonToPlay();
    makeFirstTurnCurrent();

    if (!isInterpreter) {
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
    if (isRecording()) {
      currentRecordingTurn.cancelRecording();
//      if (currentRecordingTurn.stopRecording()) {
//        logger.info("gotBackward : stopped " + currentRecordingTurn);
//        setCurrentRecordingTurn(null);
//      }
    }
  }

  @Override
  void gotForward() {
    super.gotForward();
    if (isRecording()) {
      currentRecordingTurn.cancelRecording();
//      if (currentRecordingTurn.stopRecording()) {
//        logger.info("gotBackward : stopped " + currentRecordingTurn);
//        setCurrentRecordingTurn(null);
//      }
    }
  }

  private boolean isRecording() {
    return currentRecordingTurn != null && currentRecordingTurn.isRecording();
  }

  /**
   * @param clientExercise
   * @param isRight
   * @return
   * @see #getTurnPanel
   */
  @NotNull
  @Override
  protected T reallyGetTurnPanel(ClientExercise clientExercise, COLUMNS columns) {
    T turnPanel = getRecordingTurnPanel(clientExercise, columns);
    exToTurn.put(clientExercise.getID(), turnPanel);
    return turnPanel;
  }

  private T getRecordingTurnPanel(ClientExercise clientExercise, COLUMNS columns) {
    T widgets = (T) new RecordDialogExercisePanel(clientExercise, controller,
        null, alignments, this, this, columns);

    if (columns == COLUMNS.MIDDLE) {
      widgets.addStyleName("inlineFlex");
    }

    widgets.setWidth("100%");
    return widgets;
  }

  /**
   * TODO : way too complicated!
   *
   * @see ListenViewHelper#getControls
   */
  @Override
  protected void gotPlay() {
    if (getCurrentTurn() != null &&
        isCurrentTurnARecordingTurn() &&
        getCurrentTurn().isRecording()) {  // cancel current recording

      if (DEBUG) logger.info("gotPlay on recording turn - so abort!");
      setPlayButtonToPlay();
      getCurrentTurn().cancelRecording();
    } else {
      if (doRehearse) {
        rehearseTurn();
      } else {
        ifOnLastJumpBackToFirst();
        playCurrentTurn();
      }
    }
  }

  private void rehearseTurn() {
    boolean showingScoresNow = overallSmiley.isVisible();

    if (showingScoresNow) {   // start over
      makeFirstTurnCurrent();
      sessionStorage.storeSession();
      clearScores();
    }

    T currentTurn = getCurrentTurn();

    if (DEBUG) logger.info("gotPlay currentTurn - " + currentTurn);

    // TODO : way too confusing
    if (currentTurn == null || !currentTurn.isPlaying()) {
      setTurnToPromptSide();
    }

    if (currentTurn == null) {
      logger.info("gotPlay no current turn");
      setCurrentTurn(getPromptSeq().get(0));
    } else {
      if (DEBUG) logger.info("gotPlay (rehearse) Current turn = " + currentTurn);
    }

    if (onFirstPromptTurn()) {  // if got play on the first turn, start a new session -
      if (currentTurn == null || !currentTurn.isPlaying()) {
        if (!showingScoresNow) { // we already did this above!
          sessionStorage.storeSession();
          clearScores();
        }
      } else {
        // logger.info("gotPlay ignoring turn " + currentTurn);
      }
    }

    // TODO for interpreter, only do recording with interpreter turn
    if (isTurnAPrompt(currentTurn)) {  // is the current turn a prompt? if so play the prompt
      playCurrentTurn();  // could do play/pause!
    } else { // the current turn is a response, start recording it
      startRecordingTurn(getCurrentTurn()); // advance automatically
    }
  }

  private boolean isCurrentTurnARecordingTurn() {
    return !isTurnAPrompt();
  }

  private boolean isTurnAPrompt() {
    return !getRespSeq().contains(getCurrentTurn());
  }

  @Override
  protected void setNextTurnForSide() {
    if (!doRehearse) { // i.e. in playback
      if (onLastTurn()) {
        if (DEBUG) logger.info("setNextTurnForSide : wrap around ?");
        super.setNextTurnForSide();
      } else {
        logger.info("setNextTurnForSide : skip - not on last");
      }
    }
  }

  /**
   * @see #gotPlay()
   */
  void clearScores() {
    overallSmiley.setVisible(false);
    overallSmiley.removeStyleName("animation-target");

    scoreProgress.setVisible(false);
    rateProgress.setVisible(false);
    exToScore.clear();
    exToStudentDur.clear();
    exToRefDur.clear();

    allTurns.forEach(IRecordDialogTurn::clearScoreInfo);
    recordDialogTurns.clear();
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
    if (dialogID == -1) {
      logger.warning("huh? dialog id is  " + dialogID + "???\n\n\n\n");
    } else {
      setSession();
    }
  }

  private void setSession() {
    dialogSession = new DialogSession(
        controller.getUser(),
        controller.getProjectStartupInfo().getProjectid(),
        dialogID,
        getView());

    controller.getDialogService().addSession(dialogSession, new AsyncCallback<Integer>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("creating new dialog session", caught);
      }

      @Override
      public void onSuccess(Integer result) {
        //  logger.info("startSession : made new session = " + result);
        dialogSession.setID(result);
      }
    });
  }

  protected INavigation.VIEWS getView() {
    return INavigation.VIEWS.REHEARSE;
  }

  private static final boolean DEBUG_PLAY_ENDED = false;

  /**
   * @param wasRecording
   * @see #playStopped()
   * @see #stopRecordingTurn()
   */
  protected void currentTurnPlayEnded(boolean wasRecording) {
    if (directClick) {
      directClick = false;
      markCurrent();
    } else {
      T currentTurn = getCurrentTurn();
      if (DEBUG_PLAY_ENDED) logger.info("currentTurnPlayEnded (rehearse) - turn " + currentTurn);

      boolean isCurrentPrompt = isTurnAPrompt(currentTurn);

      int i2 = allTurns.indexOf(currentTurn);
      int nextOtherSide = i2 + 1;

      if (DEBUG_PLAY_ENDED) {
        logger.info("currentTurnPlayEnded" +
            //"\n\t seq  " + seq.size() +
            //"\n\tleft  " + isLeftSpeakerSet() +
            // "\n\tright " + isRightSpeakerSet() +
            // "\n\tboth  " + allTurns.size() +
            "\n\tcurrent a prompt? " + isCurrentPrompt +
            "\n\tcurrent index " + i2 + " = " + currentTurn
        );
      }

      if (!doRehearse && !isCurrentPrompt) {  // if playback of scored turn just finished, show the score again...
        //   logger.info("\n\ncurrentTurnPlayEnded showScoreInfo on " + currentTurn);
        currentTurn.revealScore();
      } else {
        // logger.info("currentTurnPlayEnded NOT DOING showScoreInfo on " + currentTurn);
      }

      if (nextOtherSide < allTurns.size()) {
        T nextTurn = allTurns.get(nextOtherSide);

        if (DEBUG) logger.info("currentTurnPlayEnded next is " + nextTurn + "  at " + nextOtherSide);
        removeMarkCurrent();
        setCurrentTurn(nextTurn);
        markCurrent();

        makeCurrentTurnVisible();

        if (isTurnAPrompt(nextTurn)) {
          if (DEBUG_PLAY_ENDED) logger.info("currentTurnPlayEnded - play next " + nextTurn);
          playCurrentTurn();
        } else {
          if (DEBUG_PLAY_ENDED) logger.info("currentTurnPlayEnded - startRecording " + nextTurn);
          startRecordingTurn(nextTurn); // advance automatically
        }
      } else {  // on the last turn!
        if (isCurrentPrompt) {
          // logger.info("currentTurnPlayEnded - showScores " + exID);
          // TODO : a race - does the play end before the score is available, or is the score available before the play ends?
          if (doWeHaveTheLastResponseScore()) {
            if (DEBUG) logger.info("currentTurnPlayEnded - on last " + currentTurn);
            if (doRehearse) {
              showScores();
            }
          } else {
            if (DEBUG)
              logger.info("currentTurnPlayEnded - no score for " + currentTurn.getExID() + " know about " + exToScore.keySet() + " so waiting...");

            if (exStartedRecording.size() == getRespSeq().size()) {
              showWaitSpiner();  // wait for it
            }
          }
        } else {
          if (DEBUG) logger.info("currentTurnPlayEnded skip last! " + currentTurn);
        }
      }
    }
  }

  private boolean isTurnAPrompt(T currentTurn) {
    return getPromptSeq().contains(currentTurn);
  }

  private boolean doWeHaveTheLastResponseScore() {
    List<T> respSeq = getRespSeq();
    T lastRespTurn = respSeq.get(respSeq.size() - 1);

    return exToScore.containsKey(lastRespTurn.getExID());
  }

  /**
   * TODO : don't keep validities here - keep them on each turn
   *
   * @param toStart
   * @see #gotPlay()
   * @see ListenViewHelper#currentTurnPlayEnded(boolean)
   */
  private void startRecordingTurn(T toStart) {
    if (doRehearse) {
      setPlayButtonToPause();

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
    waitCursor.setVisible(doRehearse);
  }

  private void hideWaitSpinner() {
    waitCursor.setVisible(false);
  }

  /**
   * @see #showScores
   */
  private void showOverallDialogScore() {
    hideWaitSpinner();
    makeVisible(overallFeedback);

    int num = exToScore.values().size();

    double total = getTotalScore();
    double studentTotal = getTotal(this.exToStudentDur);
    double refTotal = getTotal(this.exToRefDur);

    logger.info("showOverallDialogScore student total " + studentTotal + " vs ref " + refTotal);

    total = setScoreProgressLevel(total, num);
    {
      double totalRatio = refTotal == 0D ? 0D : studentTotal / (MAX_RATE_RATIO * refTotal);
      //double totalAvgRate = totalRatio / ((float) num);
      //   logger.info("showOverallDialogScore avg rate " + studentTotal + " vs " + refTotal + " = " + totalRatio);
      setRateProgress(totalRatio);
    }
    {
      double actualRatio = studentTotal / refTotal;

      float v = roundToTens(actualRatio);
      //  logger.info("showOverallDialogScore rate to show " + v);
      rateProgress.setText("Rate " + v + "x");
    }

    overallSmiley.setEmoticon(total, controller.getLanguageInfo());

    overallSmiley.setVisible(true);
    overallSmiley.addStyleName("animation-target");
    hearYourself.setEnabled(true);

    startSession();  // OK, start a new session
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
    logger.info("showOverallDialogScore total   " + total + " vs " + num);

    double percent = total * 100;
    double round = percent;// Math.max(percent, 30);
    if (percent == 0d) round = 100d;
    double percent1 = num == 0 ? 100 : percent;
    percent1 = Math.max(percent1, 30);
    scoreProgress.setPercent(percent1);
    scoreProgress.setVisible(true);

    scoreProgress.setText("Score " + Math.round(percent) + "%");

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
      scoreProgress.setPercent(Math.max(33, percent));
    }
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
   * @see RecordDialogExercisePanel#addWidgets
   */
  @Override
  public void stopRecording() {
    if (getCurrentTurn() == allTurns.get(allTurns.size() - 1) && !exToScore.isEmpty()) {
      waitCursor.setVisible(true);
    }
    // logger.info("stopRecording received!");
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
   * Answers may come much later - we need to find the corresponding turn...
   *
   * @param audioAnswer
   */
  @Override
  public void useResult(AudioAnswer audioAnswer) {
    int exid = audioAnswer.getExid();
    T matchingTurn = getTurnForID(exid);

    if (DEBUG) {
      logger.info("useResult set answer" +
          "\n\ton " + matchingTurn +
          "\n\tto " + audioAnswer);
    }

    matchingTurn.useResult(audioAnswer);

    exToStudentDur.put(exid, matchingTurn.getStudentSpeechDur());
    exToRefDur.put(exid, matchingTurn.getRefSpeechDur());

    addScore(exid, (float) audioAnswer.getScore(), matchingTurn);

    maybeMoveOnToNextTurn();
    //maybeMoveOnIfNextTurnARecordingTurn();
  }

  @Override
  public void useInvalidResult(int exid) {
    T matchingTurn = getTurnForID(exid);
    addScore(exid, 0F, matchingTurn);
    matchingTurn.useInvalidResult();

    maybeMoveOnToNextTurn();
  }

  private void maybeMoveOnToNextTurn() {
    T currentTurn = getCurrentTurn();

    boolean nextTurnAResp = isNextTurnAResp(currentTurn);

    boolean turnAPrompt = isTurnAPrompt(currentTurn);

    logger.info("Current turn is " + currentTurn +
        "\n\tis playing " + currentTurn.isPlaying() +
        "\n\tprompt     " + turnAPrompt +
        "\n\tnextTurnAResp " + nextTurnAResp
    );
    //  logger.info("Current turn is a prompt " + turnAPrompt);

    if (!turnAPrompt && nextTurnAResp) {
      moveOnAfterRecordingStopped();
    }
  }

  private T getTurnForID(int exid) {
    return exToTurn.get(exid);
  }

  @Override
  public String getSession() {
    return "" + sessionStorage.getSession();
  }
}
