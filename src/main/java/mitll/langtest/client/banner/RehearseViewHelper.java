package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.ProgressBarBase;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.dialog.ExceptionHandlerDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.flashcard.SessionStorage;
import mitll.langtest.client.scoring.IRecordDialogTurn;
import mitll.langtest.client.scoring.RecordDialogExercisePanel;
import mitll.langtest.client.scoring.ScoreProgressBar;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.Validity;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.ClientExercise;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by go22670 on 4/5/17.
 */
public class RehearseViewHelper<T extends RecordDialogExercisePanel<ClientExercise>>
    extends ListenViewHelper<T>
    implements SessionManager, IRehearseView {
  private final Logger logger = Logger.getLogger("RehearseViewHelper");

  private static final String THEY_SPEAK = "Listen to : ";
  private static final String YOU_SPEAK = "Speak : ";

  private static final boolean DEBUG = false;

  private static final int DELAY_MILLIS = 20;

  private ProgressBar scoreProgress;
  /**
   *
   */
  private Image overallSmiley = new Image();
  private static final SafeUri animated = UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "animated_progress48.gif");

  private Image waitCursor = new Image(animated);

  private final Map<Integer, Float> exToScore = new HashMap<>();
  private final Map<Integer, T> exToTurn = new HashMap<>();
  private List<IRecordDialogTurn> recordDialogTurns = new ArrayList<>();
  private SessionStorage sessionStorage;
  private DivWidget overallFeedback;
  private HTML leftSpeakerHint, rightSpeakerHint;

  private T currentRecordingTurn = null;

  /**
   * @param controller
   * @see NewContentChooser#NewContentChooser(ExerciseController, IBanner)
   */
  RehearseViewHelper(ExerciseController controller) {
    super(controller);
    this.sessionStorage = new SessionStorage(controller.getStorage(), "rehearseSession");
  }

  @Override
  public void showContent(Panel listContent, INavigation.VIEWS instanceName, boolean fromClick) {
    super.showContent(listContent, instanceName, fromClick);
    controller.registerStopDetected(this::mySilenceDetected);
  }

  /**
   * @param dialog
   * @param child
   */
  @Override
  protected void showDialogGetRef(IDialog dialog, Panel child) {
    super.showDialogGetRef(dialog, child);
    child.add(overallFeedback = getOverallFeedback());
  }

  @Override
  int getControlRowHeight() {
    return 55;
  }

  @NotNull
  private DivWidget getOverallFeedback() {
    DivWidget breadRow = new DivWidget();
    Style style = breadRow.getElement().getStyle();
    style.setMarginTop(10, Style.Unit.PX);
    style.setMarginBottom(10, Style.Unit.PX);
    style.setClear(Style.Clear.BOTH);
    breadRow.addStyleName("cardBorderShadow");
    breadRow.setWidth("97%");  //??? why 97???
    breadRow.getElement().setId("breadRow");
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
      style.setMarginTop(-46, Style.Unit.PX);

      rightDiv.add(rightSpeakerHint);
    }
    checkBox.getElement().getStyle().setClear(Style.Clear.BOTH);
    rightDiv.add(checkBox);

    return rightDiv;
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
   * @return
   */
  private DivWidget showScoreFeedback() {
    DivWidget container = new DivWidget();

    container.setId("feedbackContainerAndBar");

    {
      DivWidget iconContainer = new DivWidget();
      iconContainer.addStyleName("floatLeft");

      iconContainer.add(waitCursor);
      waitCursor.setVisible(false);
      waitCursor.setWidth("72px");
      waitCursor.setHeight("72px");
      container.add(iconContainer);
    }

    {
      DivWidget iconContainer = new DivWidget();
      iconContainer.addStyleName("floatLeft");

      iconContainer.add(overallSmiley);

      iconContainer.setWidth("72px");
      iconContainer.setHeight("72px");

      styleAnimatedSmiley();

      container.add(iconContainer);
    }

    {
      DivWidget scoreContainer = new DivWidget();
      scoreContainer.addStyleName("rehearseScoreContainer");
      scoreContainer.addStyleName("floatLeft");

      scoreContainer.addStyleName("topMargin");
      scoreContainer.addStyleName("leftFiveMargin");
      scoreContainer.add(scoreProgress = new ProgressBar(ProgressBarBase.Style.DEFAULT));
      styleProgressBarContainer(scoreProgress);

      scoreContainer.setWidth("73%");

      container.setWidth("78%");
      Style style = container.getElement().getStyle();
      style.setOverflow(Style.Overflow.HIDDEN);
      style.setMarginLeft(20, Style.Unit.PCT);
      style.setMarginBottom(20, Style.Unit.PX);
      container.add(scoreContainer);
    }

    return container;
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

  /**
   * silence analyzer has triggered...
   * Ideally we'd look at packet duration here...
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
          logger.info("mySilenceDetected : OK, server agrees with client side silence detector... have seen " + validities.size());
          stopRecordingTurn();
        } else {
          StringBuffer buffer = new StringBuffer();
          validities.forEach(validity1 -> buffer.append(validity1).append(" "));
          logger.info("mySilenceDetected : silence for " + currentRecordingTurn +
              " packets (" + validities.size() + ") : " + buffer);
        }
      } else {
        logger.warning("mySilenceDetected : stopRecordingTurn ");
        stopRecordingTurn();
      }
    } else {
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
      logger.info("mySilenceDetected : stopped " + currentRecordingTurn);
      setCurrentRecordingTurn(null);
      currentTurnPlayEnded();
    }
  }

  private int count = 0;

  /**
   * Forget about scores after showing them...
   *
   * Show scores if just recorded the last record turn.
   *
   * @param exid
   * @param score
   * @param recordDialogTurn
   * @see RecordDialogExercisePanel#addWidgets
   */
  @Override
  public void addScore(int exid, float score, IRecordDialogTurn recordDialogTurn) {
    exToScore.put(exid, score);
    recordDialogTurns.add(recordDialogTurn);

    T turn = exToTurn.get(exid);
    // logger.info("addScore exid " + exid + " score " + score + " now " + exToScore.keySet());

    checkAtEnd(turn);
  }

  /**
   * So if the response is the last turn, go ahead and show the scores
   *
   * @param turn
   */
  private void checkAtEnd(T turn) {
    boolean onLast = bothTurns.indexOf(turn) == bothTurns.size() - 1;
    if (onLast) {
      showScores();
    }
    //   logger.info("checkAtEnd : not at end");
  }

  private void showScores() {
    showScore();
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
  protected void gotSpeakerChoice() {
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
   * @see ListenViewHelper#getControls
   */
  @Override
  protected void gotPlay() {
    if (getCurrentTurn() != null &&
        !isCurrentTurnAPrompt() &&
        getCurrentTurn().isRecording()) {

      logger.info("gotPlay on recording turn - so abort!");

      getCurrentTurn().cancelRecording();
    } else {
      if (overallSmiley.isVisible()) {
        makeFirstTurnCurrent();
        sessionStorage.storeSession();
        clearScores();
      }

      setTurnToPromptSide();

      T currentTurn = getCurrentTurn();
      if (currentTurn == null) {
        logger.info("gotPlay no current turn");
        setCurrentTurn(getSeq().get(0));
      } else logger.info("gotPlay (rehearse) Current turn for ex " + currentTurn.getExID());

      if (onFirstTurn()) {
        sessionStorage.storeSession();
        clearScores();
      }

      if (getSeq().contains(currentTurn)) {  // is the current turn a prompt? if so play the prompt
        playCurrentTurn();
      } else { // the current turn is a response, start recording it
        startRecordingTurn(getCurrentTurn()); // advance automatically
      }
    }
  }

  private boolean isCurrentTurnAPrompt() {
    return getSeq().contains(getCurrentTurn());
  }

  protected void setNextTurnForSide() {
  }

  private void clearScores() {
    overallSmiley.setVisible(false);
    overallSmiley.removeStyleName("animation-target");

    scoreProgress.setVisible(false);
    exToScore.clear();

    bothTurns.forEach(IRecordDialogTurn::clearScoreInfo);
    recordDialogTurns.clear();
  }

  /**
   * @see #playStopped()
   * @see #stopRecordingTurn()
   */
  protected void currentTurnPlayEnded() {
    if (directClick) {
      directClick = false;
      markCurrent();
    } else {
      T currentTurn = getCurrentTurn();
      int exID = currentTurn.getExID();
      if (DEBUG) logger.info("currentTurnPlayEnded (rehearse) - turn " + exID);
      List<T> seq = getSeq();

      boolean isCurrentPrompt = seq.contains(currentTurn);

      int i2 = bothTurns.indexOf(currentTurn);
      int nextOtherSide = i2 + 1;

      if (DEBUG || true) {
        logger.info("currentTurnPlayEnded" +
            "\n\t seq  " + seq.size() +
            "\n\tleft  " + isLeftSpeakerSet() +
            "\n\tright " + isRightSpeakerSet() +
            "\n\tboth  " + bothTurns.size() +
            "\n\t is current playing " + isCurrentPrompt +
            "\n\ti2    " + i2 + " = " + currentTurn +
            "\n\tnext  "
        );
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
      } else {
        if (isCurrentPrompt) {
          logger.info("currentTurnPlayEnded - showScores " + exID);

          // TODO : a race - does the play end before the score is available, or is the score available before the play ends?

          List<T> respSeq = getRespSeq();
          T widgets = respSeq.get(respSeq.size() - 1);

          boolean hasScore = exToScore.containsKey(widgets.getExID());
          if (hasScore) {
            showScores();
          } else {
            logger.info("currentTurnPlayEnded - no score for " + exID + " know about " + exToScore.keySet());

            waitCursor.setVisible(true);  // wait for it
          }
        }
      }
    }
  }

  /**
   * @param toStart
   * @see #gotPlay()
   */
  private void startRecordingTurn(T toStart) {
    toStart.startRecording();
    setCurrentRecordingTurn(toStart);

    logger.info("startRecordingTurn : " + currentRecordingTurn);

    validities.clear();
  }

  private void setCurrentRecordingTurn(T toStart) {
    //  logger.info("\n\n\nsetCurrentRecordingTurn BEFORE : " + toStart);
    currentRecordingTurn = toStart;
    logger.info("setCurrentRecordingTurn : " + currentRecordingTurn);
  }

  /**
   * @see #showScores
   */
  private void showScore() {
    waitCursor.setVisible(false);

    int num = exToScore.values().size();
//
//    if (num == expected) {
    double total = getTotal();
    // logger.info("showScore showing " + num);
    total /= (float) num;
    //  logger.info("showScore total   " + total);

    double percent = total * 100;
    double round = percent;// Math.max(percent, 30);
    if (percent == 0d) round = 100d;
    scoreProgress.setPercent(num == 0 ? 100 : percent);
    scoreProgress.setVisible(true);
    scoreProgress.setText("Score " + Math.round(percent) + "%");

    makeVisible(overallFeedback);

    new ScoreProgressBar(false).setColor(scoreProgress, total, round);

    setEmoticon(overallSmiley, total);

    overallSmiley.setVisible(true);
    overallSmiley.addStyleName("animation-target");
  }

  private double getTotal() {
    double total = 0D;
    for (Float score : exToScore.values()) {
      total += score;
    }
    return total;
  }

  @Override
  public void setEmoticon(Image smiley, double total) {
    String choice;

    if (total < 0.3) {
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
    }

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

  private List<Validity> validities = new ArrayList<>();

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
    T matchingTurn = exToTurn.get(audioAnswer.getExid());
    addScore(audioAnswer.getExid(), (float) audioAnswer.getScore(), matchingTurn);
//    logger.info("useResult set answer on " + matchingTurn + " to " + audioAnswer);
    matchingTurn.useResult(audioAnswer);
  }

  @Override
  public void useInvalidResult(int exid) {
    T matchingTurn = exToTurn.get(exid);
    addScore(exid,0,matchingTurn);
    matchingTurn.useInvalidResult();
  }

  @Override
  public String getSession() {
    return "" + sessionStorage.getSession();
  }
}
