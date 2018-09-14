package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.ProgressBarBase;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.flashcard.SessionStorage;
import mitll.langtest.client.scoring.IRecordDialogTurn;
import mitll.langtest.client.scoring.RecordDialogExercisePanel;
import mitll.langtest.client.scoring.ScoreProgressBar;
import mitll.langtest.shared.dialog.DialogStatus;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.Validity;
import mitll.langtest.shared.dialog.DialogSession;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.project.Language;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by go22670 on 4/5/17.
 */
public class RehearseViewHelper<T extends RecordDialogExercisePanel<ClientExercise>>
    extends ListenViewHelper<T>
    implements SessionManager, IRehearseView {
  private final Logger logger = Logger.getLogger("RehearseViewHelper");

  private static final String REHEARSE = "Rehearse";
  private static final String HEAR_YOURSELF = "Hear yourself";
  private static final int VALUE = -98;

  private static final String WIDTH = "97%";
  private static final String THEY_SPEAK = "Listen to : ";
  private static final String YOU_SPEAK = "Speak : ";

  private static final boolean DEBUG = false;
  private static final int TOP_TO_USE = 10;
  private final List<Float> thresholds;


  /**
   * @see #clearScores
   */
  private ProgressBar scoreProgress;
  /**
   *
   */
  private final Image overallSmiley = new Image();
  private static final SafeUri animated = UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "animated_progress48.gif");

  private final Image waitCursor = new Image(animated);

  private final Map<Integer, Float> exToScore = new HashMap<>();
  private final Map<Integer, Float> exToRate = new HashMap<>();
  private final Set<Integer> exStartedRecording = new HashSet<>();

  final Map<Integer, T> exToTurn = new HashMap<>();

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
  private static final float NATIVE_HARD_CODE = 0.70F;

  private DialogSession dialogSession = null;
  /**
   * 9/13/18 histo from production
   * Did histogram of 85K korean scores, split 6 ways, even 12K piles
   */
  private static final List<Float> koreanThresholds = new ArrayList<>(Arrays.asList(0.31F, 0.43F, 0.53F, 0.61F, NATIVE_HARD_CODE));
  /**
   * Did histogram of 18K english scores, split 6 ways, even 3K piles
   */
  private static final List<Float> englishThresholds = new ArrayList<>(Arrays.asList(0.23F, 0.36F, 0.47F, 0.58F, NATIVE_HARD_CODE));  // technically last is 72

  private static final List<String> emoticons = new ArrayList<>(Arrays.asList(
      "frowningEmoticon.png", // <0.3
      "confusedEmoticon.png", //0.4
      "thinkingEmoticon.png", //0.5
      "neutralEmoticon.png", // 0.6
      "smilingEmoticon.png", // 0.7
      "grinningEmoticon.png"
  ));
  private static final String BEST_EMOTICON = emoticons.get(emoticons.size() - 1);

  /**
   * @param controller
   * @see NewContentChooser#NewContentChooser(ExerciseController, IBanner)
   */
  RehearseViewHelper(ExerciseController controller) {
    super(controller);
    this.sessionStorage = new SessionStorage(controller.getStorage(), "rehearseSession");
    this.thresholds = controller.getLanguageInfo() == Language.KOREAN ? koreanThresholds : englishThresholds;
  }

  @Override
  public void showContent(Panel listContent, INavigation.VIEWS instanceName) {
    super.showContent(listContent, instanceName);
    exToScore.clear();
    exToRate.clear();
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
   */
  @Override
  protected void showDialogGetRef(int dialogID, IDialog dialog, Panel child) {
    super.showDialogGetRef(dialogID, dialog, child);
    child.add(overallFeedback = getOverallFeedback());
  }

  @Override
  protected void setControlRowHeight(DivWidget rowOne) {
  }

  @NotNull
  private DivWidget getOverallFeedback() {
    DivWidget breadRow = new DivWidget();
    breadRow.getElement().setId("overallFeedbackRow");

    Style style = breadRow.getElement().getStyle();
    style.setMarginTop(10, Style.Unit.PX);
    style.setMarginBottom(10, Style.Unit.PX);
    style.setClear(Style.Clear.BOTH);

    breadRow.addStyleName("cardBorderShadow");
    breadRow.setWidth(WIDTH);  //??? why 97???
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

    //  logger.info("doRehearse = " + doRehearse);

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
    style.setMarginTop(TOP_TO_USE, Style.Unit.PX);
    style.setMarginBottom(TOP_TO_USE, Style.Unit.PX);
  }

  @NotNull
  protected DivWidget getLeftSpeakerDiv(CheckBox checkBox) {
    DivWidget rightDiv = new DivWidget();
    checkBox.getElement().getStyle().setClear(Style.Clear.BOTH);

    {
      leftSpeakerHint = new HTML(THEY_SPEAK);
      leftSpeakerHint.addStyleName("floatLeft");

      Style style = leftSpeakerHint.getElement().getStyle();
      style.setClear(Style.Clear.BOTH);
      style.setMarginLeft(41, Style.Unit.PX);

      rightDiv.add(leftSpeakerHint);
    }
    rightDiv.add(checkBox);

    return rightDiv;
  }

  @NotNull
  DivWidget getRightSpeakerDiv(CheckBox checkBox) {
    DivWidget rightDiv = new DivWidget();

    {
      rightSpeakerHint = new HTML(YOU_SPEAK);
      rightSpeakerHint.addStyleName("floatRight");

      Style style = rightSpeakerHint.getElement().getStyle();
      style.setMarginRight(4, Style.Unit.PX);
      style.setMarginTop(VALUE, Style.Unit.PX);

      rightDiv.add(rightSpeakerHint);
    }
    checkBox.getElement().getStyle().setClear(Style.Clear.BOTH);
    rightDiv.add(checkBox);

    return rightDiv;
  }

  @Override
  protected CheckBox addRightSpeaker(DivWidget rowOne, String label) {
    CheckBox checkBox = super.addRightSpeaker(rowOne, label);
    checkBox.getElement().getStyle().setMarginTop(-74, Style.Unit.PX);

    return checkBox;
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
      style.setMarginLeft(20, Style.Unit.PCT);
      style.setMarginBottom(20, Style.Unit.PX);
    }

    container.add(getWaitCursor());
    container.add(getOverallEmoticon());
    container.add(getProgressBarDiv(scoreProgress = new ProgressBar(ProgressBarBase.Style.DEFAULT)));

    return container;
  }

  @NotNull
  private DivWidget getProgressBarDiv(ProgressBar scoreProgress) {
    DivWidget scoreContainer = new DivWidget();
    scoreContainer.addStyleName("rehearseScoreContainer");
    scoreContainer.addStyleName("floatLeft");

    scoreContainer.addStyleName("topMargin");
    scoreContainer.addStyleName("leftFiveMargin");
    scoreContainer.add(scoreProgress);
    styleProgressBarContainer(scoreProgress);

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
    overallSmiley.getElement().getStyle().setLeft(18, Style.Unit.PX);
    overallSmiley.getElement().getStyle().setTop(18, Style.Unit.PX);
  }

  private void styleProgressBarContainer(ProgressBar progressBar) {
    Style style = progressBar.getElement().getStyle();
    style.setMarginTop(5, Style.Unit.PX);
    style.setMarginLeft(5, Style.Unit.PX);

    style.setHeight(25, Style.Unit.PX);
    style.setFontSize(16, Style.Unit.PX);

    progressBar.setWidth("49%");
    progressBar.setVisible(false);
  }

  private int beforeCount = 0;

  /**
   * silence analyzer has triggered...
   * Ideally we'd look at packet duration here...
   *
   * @see RehearseViewHelper#showContent
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
          if (DEBUG)
            logger.info("mySilenceDetected : OK, server agrees with client side silence detector... have seen " + validities.size());
          stopRecordingTurn();
          beforeCount = 0;
        } else {
          StringBuffer buffer = new StringBuffer();
          if (beforeCount < validities.size()) {
            validities.forEach(validity1 -> buffer.append(validity1).append(" "));
            logger.info("mySilenceDetected : silence for " + currentRecordingTurn +
                " packets (" + validities.size() + ") : " + buffer);
            beforeCount = validities.size();
          }
        }
      } else {
        logger.info("mySilenceDetected : stopRecordingTurn, num validities " + validities.size());
        stopRecordingTurn();
        beforeCount = 0;
      }
    } else if (DEBUG) {
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
      setCurrentRecordingTurn(null);
      currentTurnPlayEnded(true);
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

    logger.info("addScore exid " + exid +
        " score " + score +
        "\n\tnow ex->score (" + exToScore.keySet().size() + ") : " + exToScore.keySet() +
        "\n\tvs expected   (" + getRespSeq().size() + ") : " + getRespSeq());

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
    showOverallDialogScore();
    recordDialogTurns.forEach(IRecordDialogTurn::showScoreInfo);
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
    rightSpeakerBox.setValue(!value);
    gotSpeakerChoice();
  }

  /**
   * On user click of speaker.
   *
   * @param value
   */
  protected void speakerTwoCheck(Boolean value) {
    leftSpeakerBox.setValue(!value);
    gotSpeakerChoice();
  }

  /**
   * Move current turn to first turn when we switch who is the prompting speaker.
   */
  void gotSpeakerChoice() {
    setPlayButtonToPlay();
    makeFirstTurnCurrent();
    setHints();
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
    if (currentRecordingTurn != null && currentRecordingTurn.isRecording()) {
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
    if (currentRecordingTurn != null && currentRecordingTurn.isRecording()) {
      currentRecordingTurn.cancelRecording();
//      if (currentRecordingTurn.stopRecording()) {
//        logger.info("gotBackward : stopped " + currentRecordingTurn);
//        setCurrentRecordingTurn(null);
//      }
    }
  }

  /**
   * @param clientExercise
   * @param isRight
   * @return
   * @see #getTurnPanel
   */
  @NotNull
  @Override
  protected T reallyGetTurnPanel(ClientExercise clientExercise, boolean isRight) {
    // logger.info("making record dialog ex panel for " + clientExercise.getID());
    T turnPanel =
        (T) new RecordDialogExercisePanel<ClientExercise>(clientExercise, controller,
            null, alignments, this, this, isRight);

    exToTurn.put(clientExercise.getID(), turnPanel);
    return turnPanel;
  }

  /**
   * TODO : way too complicated!
   *
   * @see ListenViewHelper#getControls
   */
  @Override
  protected void gotPlay() {
    if (getCurrentTurn() != null &&
        !isCurrentTurnAPrompt() &&
        getCurrentTurn().isRecording()) {

      if (DEBUG) logger.info("gotPlay on recording turn - so abort!");

      getCurrentTurn().cancelRecording();
    } else {
      if (doRehearse) {
        boolean startOver = overallFeedback.isVisible();

        if (startOver) {   // start over
          makeFirstTurnCurrent();
          sessionStorage.storeSession();
          clearScores();
        }

        T currentTurn = getCurrentTurn();

        if (DEBUG) logger.info("gotPlay currentTurn - " + currentTurn);

        if (currentTurn == null || !currentTurn.isPlaying()) {
          setTurnToPromptSide();
        }

        if (currentTurn == null) {
          logger.info("gotPlay no current turn");
          setCurrentTurn(getSeq().get(0));
        } else {
          if (DEBUG) logger.info("gotPlay (rehearse) Current turn = " + currentTurn);
        }

        if (onFirstTurn()) {  // if got play on the first turn, start a new session -
          if (currentTurn == null || !currentTurn.isPlaying()) {
            if (!startOver) { // we already this above!
              sessionStorage.storeSession();
              clearScores();
            }
          } else {
            logger.info("gotPlay ignoring turn " + currentTurn);
          }
        }

        if (getSeq().contains(currentTurn)) {  // is the current turn a prompt? if so play the prompt
          playCurrentTurn();  // could do play/pause!
        } else { // the current turn is a response, start recording it
          startRecordingTurn(getCurrentTurn()); // advance automatically
        }
      } else {
        super.gotPlay();
      }
    }
  }

  private boolean isCurrentTurnAPrompt() {
    return getSeq().contains(getCurrentTurn());
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
    exToScore.clear();
    exToRate.clear();

    bothTurns.forEach(IRecordDialogTurn::clearScoreInfo);
    recordDialogTurns.clear();

    dialogSession = new DialogSession(-1, -1, controller.getProjectStartupInfo().getProjectid(), dialogID,
        System.currentTimeMillis(), System.currentTimeMillis(), getView(), DialogStatus.DEFAULT, 0, 0F, 0F);
  }

  protected INavigation.VIEWS getView() {
    return INavigation.VIEWS.REHEARSE;
  }

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

      //     int exID = currentTurn.getExID();
      if (DEBUG) logger.info("currentTurnPlayEnded (rehearse) - turn " + currentTurn.getExID());
      List<T> seq = getSeq();

      boolean isCurrentPrompt = seq.contains(currentTurn);

      int i2 = bothTurns.indexOf(currentTurn);
      int nextOtherSide = i2 + 1;

      if (DEBUG) {
        logger.info("currentTurnPlayEnded" +
            //"\n\t seq  " + seq.size() +
            //"\n\tleft  " + isLeftSpeakerSet() +
            // "\n\tright " + isRightSpeakerSet() +
            // "\n\tboth  " + bothTurns.size() +
            "\n\t is current playing " + isCurrentPrompt +
            "\n\ti2    " + i2 + " = " + currentTurn
        );
      }

      if (!doRehearse && !isCurrentPrompt) {  // if playback of scored turn just finished, show the score again...
        //   logger.info("\n\ncurrentTurnPlayEnded showScoreInfo on " + currentTurn);
        currentTurn.revealScore();
      } else {
        // logger.info("currentTurnPlayEnded NOT DOING showScoreInfo on " + currentTurn);
      }

      if (nextOtherSide < bothTurns.size()) {
        T nextTurn = bothTurns.get(nextOtherSide);

        if (DEBUG) logger.info("currentTurnPlayEnded next is " + nextTurn.getId() + "  at " + nextOtherSide);
        removeMarkCurrent();
        setCurrentTurn(nextTurn);
        markCurrent();

        makeCurrentTurnVisible();
        if (isCurrentPrompt) {
          if (DEBUG) logger.info("currentTurnPlayEnded - startRecording " + nextTurn.getExID());
          startRecordingTurn(nextTurn); // advance automatically
        } else {
          if (DEBUG) logger.info("currentTurnPlayEnded - play current " + nextTurn.getExID());
          playCurrentTurn();
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
    double totalRatio = getTotalRatios();
    double totalAvgRate = totalRatio / ((float) num);

    logger.info("showOverallDialogScore avg rate " + totalAvgRate);

    total = setScoreProgressLevel(total, num);

    setEmoticon(overallSmiley, total);

    overallSmiley.setVisible(true);
    overallSmiley.addStyleName("animation-target");
    hearYourself.setEnabled(true);
  }

  /**
   * @param total
   * @param num
   * @return ratio of total to num
   */
  private double setScoreProgressLevel(double total, int num) {
    ProgressBar scoreProgress = this.scoreProgress;

    total /= (float) num;
    //  logger.info("showOverallDialogScore total   " + total);

    double percent = total * 100;
    double round = percent;// Math.max(percent, 30);
    if (percent == 0d) round = 100d;
    scoreProgress.setPercent(num == 0 ? 100 : percent);
    scoreProgress.setVisible(true);
    scoreProgress.setText("Score " + Math.round(percent) + "%");


    new ScoreProgressBar(false).setColor(scoreProgress, total, round);
    return total;
  }

  private double getTotalScore() {
    double total = 0D;
    for (Float score : exToScore.values()) {
      total += score;
    }
    return total;
  }

  private double getTotalRatios() {
    double total = 0D;
    for (Float score : exToRate.values()) {
      total += score;
    }
    return total;
  }


  @Override
  public void setEmoticon(Image smiley, double total) {
    String choice = BEST_EMOTICON;

    for (int i = 0; i < thresholds.size(); i++) {
      if (total < thresholds.get(i)) {
        choice = emoticons.get(i);
        break;
      }
    }

 /*   if (total < 0.3) {
      choice = "frowningEmoticon.png";
    } else if (total < 0.4) {
      choice = "confusedEmoticon.png";
    } else if (total < 0.5) {
      choice = "thinkingEmoticon.png";
    } else if (total < 0.6) {
      choice = "neutralEmoticon.png";
    } else if (total < 0.7) {
      choice = "smilingEmoticon.png";
    } else {
      choice = "grinningEmoticon.png";
    }*/

    smiley.setUrl(LangTest.LANGTEST_IMAGES + choice);
  }

  /**
   * @see RecordDialogExercisePanel#addWidgets
   */
  @Override
  public void stopRecording() {
    if (getCurrentTurn() == bothTurns.get(bothTurns.size() - 1) && !exToScore.isEmpty()) {
      waitCursor.setVisible(true);
    }
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

    exToRate.put(exid, matchingTurn.getSpeakingRate());

    addScore(exid, (float) audioAnswer.getScore(), matchingTurn);
  }

  @Override
  public void useInvalidResult(int exid) {
    T matchingTurn = getTurnForID(exid);
    addScore(exid, 0F, matchingTurn);
    matchingTurn.useInvalidResult();
  }

  private T getTurnForID(int exid) {
    return exToTurn.get(exid);
  }

  @Override
  public String getSession() {
    return "" + sessionStorage.getSession();
  }
}
