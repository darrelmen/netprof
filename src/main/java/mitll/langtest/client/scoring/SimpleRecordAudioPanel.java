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

package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.banner.SessionManager;
import mitll.langtest.client.dialog.IListenView;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.gauge.ASRHistoryPanel;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.WaitCursorHelper;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.sound.CompressedAudio;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.SimpleAudioAnswer;
import mitll.langtest.shared.exercise.HasID;
import mitll.langtest.shared.exercise.ScoredExercise;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static mitll.langtest.client.scoring.TwoColumnExercisePanel.CONTEXT_INDENT;

/**
 * An ASR scoring panel with a record button.
 */
public class SimpleRecordAudioPanel<T extends HasID & ScoredExercise> extends NoFeedbackRecordAudioPanel<T>
    implements SessionManager {
//  private final Logger logger = Logger.getLogger("SimpleRecordAudioPanel");

  private static final String MP3 = ".mp3";
  public static final String OGG = ".ogg";

  private static final float HUNDRED = 100.0f;
  private WaitCursorHelper waitCursorHelper = null;

  private MiniScoreListener miniScoreListener;

  private String audioPath;


  private boolean hasScoreHistory;
  private final ListInterface<?, ?> listContainer;
  private final boolean isRTL;
  private ScoreFeedbackDiv scoreFeedbackDiv;
  private final boolean addPlayer;
  private Widget scoreHistory;
  /**
   * @see #getDialogSessionID
   */
  private final IListenView listenView;

  /**
   * @param controller
   * @param exercise
   * @param listContainer
   * @param addPlayer
   * @param listenView
   * @param sessionManager
   * @see TwoColumnExercisePanel#getItemContent
   * @see TwoColumnExercisePanel#addContextFields
   */
  SimpleRecordAudioPanel(ExerciseController controller,
                         T exercise,
                         ListInterface<?, ?> listContainer,
                         boolean addPlayer,
                         IListenView listenView,
                         SessionManager sessionManager) {
    super(exercise, controller, sessionManager);
    this.listenView = listenView;
    this.listContainer = listContainer;
    this.addPlayer = addPlayer;
    getElement().setId("SimpleRecordAudioPanel_" + exercise.getID());
    this.isRTL = controller.isRightAlignContent();
    setWidth("100%");
    addWidgets();
    List<CorrectAndScore> scores = exercise.getScores();

  //  logger.info("SimpleRecordAudioPanel : exercise " + exercise.getID() + " has\n\t" + scores + " scores");

    showRecordingHistory(scores);
    hasScoreHistory = scores != null && !scores.isEmpty();
    setVisible(hasScoreHistory);
  }

  @Override
  public int getDialogSessionID() {
    return listenView.getDialogSessionID();
  }

  /**
   * Replace the html 5 audio tag with our fancy waveform widget.
   *
   * @return
   * @seex #AudioPanel
   * @see SimpleRecordAudioPanel#SimpleRecordAudioPanel
   */
  protected void addWidgets() {
    //long then = System.currentTimeMillis();
    DivWidget col = new DivWidget();
    col.add(scoreFeedback = new DivWidget());
    getScoreFeedback().getElement().setId("scoreFeedback_" + exercise.getID());

    {
      DivWidget historyHoriz = new DivWidget();
      historyHoriz.addStyleName("inlineFlex");
      DivWidget spacer = new DivWidget();
      spacer.getElement().getStyle().setProperty("minWidth", CONTEXT_INDENT + "px");

      historyHoriz.add(spacer);
      historyHoriz.add(scoreHistory = getScoreHistory());
      col.add(historyHoriz);
    }
    makeWaitCursor();
    recordFeedback = makePlayAudioPanel().getRecordFeedback(makeWaitCursor().getWaitCursor());
    recordFeedback.getElement().getStyle().setProperty("minWidth", CONTEXT_INDENT + "px");

    getScoreFeedback().add(recordFeedback);

    this.scoreFeedbackDiv = new ScoreFeedbackDiv(playAudioPanel, playAudioPanel, playAudioPanel.getRealDownloadContainer(), true);

    add(col);
    // long now = System.currentTimeMillis();
    // logger.info("addWidgets "+ (now-then)+ " millis");
    //scoreFeedback.getElement().setId("scoreFeedbackRow");
  }

  private void addMiniScoreListener(MiniScoreListener l) {
    this.miniScoreListener = l;
  }

  /**
   * @return
   * @see RecorderPlayAudioPanel#RecorderPlayAudioPanel
   * @see #addWidgets
   */
  @NotNull
  private ASRHistoryPanel getScoreHistory() {
    ASRHistoryPanel historyPanel = new ASRHistoryPanel(controller, exercise.getID(), addPlayer);
    addMiniScoreListener(historyPanel);
    historyPanel.showChart(controller.getHost());
    historyPanel.setWidth("50%");
    return historyPanel;
  }

  /**
   * @see mitll.langtest.server.DownloadServlet#returnAudioFile
   * @see #useResult(AudioAnswer)
   */
  private void setDownloadHref() {
    String audioPathToUse = audioPath.endsWith(OGG) ? audioPath.replaceAll(OGG, MP3) : audioPath;
    playAudioPanel.setDownloadHref(audioPathToUse, exercise.getID(), getUser(), controller.getHost());
  }

  private int getUser() {
    return controller.getUserState().getUser();
  }

  /**
   * @param result
   * @param isRTL
   * @see #useResult
   */
  private void scoreAudio(SimpleAudioAnswer result, boolean isRTL) {
    clearScoreFeedback();
    PretestScore pretestScore = result.getPretestScore();
    getScoreFeedback().add(scoreFeedbackDiv.getWordTableContainer(pretestScore, isRTL));
    useScoredResult(pretestScore, false, result.getPath());

    // Gotta remember the score on the exercise now...
    exercise.getScores().add(new CorrectAndScore(result));
//    logger.info("exercise " + exercise.getID() + " now has " + exercise.getScores().size() + " scores");
  }

  @Override
  public void startRecording() {
    //logger.info("startRecording...");
    super.startRecording();

    scoreFeedbackDiv.hideScore();

    waitCursorHelper.showFinished();

    scoreHistory.setVisible(false);
  }

  /**
   * @see RecordButton.RecordingListener#stopRecording
   */
  @Override
  public void stopRecording() {
    super.stopRecording();

    scoreHistory.setVisible(true);
    waitCursorHelper.scheduleWaitTimer();
  }

  public void gotShortDurationRecording() {
    waitCursorHelper.showFinished();
    super.gotShortDurationRecording();
    scoreHistory.setVisible(true);
  }

  @Override
  public void useResult(AudioAnswer result) {
    super.useResult(result);
    //logger.info("useResult " + result);
    waitCursorHelper.showFinished();
    hasScoreHistory = true;

    audioPath = result.getPath();
    setDownloadHref();
    scoreAudio(result, isRTL);
  }

  @Override
  public void useInvalidResult(int exid, boolean isValid) {
    //  logger.info("useInvalidResult " + isValid);
    waitCursorHelper.showFinished();
    setVisible(hasScoreHistory);

    super.useInvalidResult(exid, isValid);
  }

  /**
   * TODO : this is a bad idea to alter the score unless the praise is based on the same score...
   *
   * @param result
   * @param scoredBefore
   * @param path
   * @see #scoreAudio
   */
  private void useScoredResult(PretestScore result, boolean scoredBefore, String path) {
    float hydecScore = result.getHydecScore();
    boolean isValid = hydecScore > 0;
    if (!scoredBefore && miniScoreListener != null && isValid) {
      miniScoreListener.gotScore(result, path);
    }
    getReadyToPlayAudio(path);
    if (isValid) {
      List<TranscriptSegment> transcriptSegments = result.getTypeToSegments().get(NetPronImageType.WORD_TRANSCRIPT);
      if (transcriptSegments != null && transcriptSegments.size() == 1) {
        float wordScore = transcriptSegments.get(0).getScore();
        //  logger.info("useScoredResult using word score " + wordScore + " instead of hydec score " + hydecScore);
        hydecScore = wordScore;
      }

      scoreFeedbackDiv.showScore(Math.min(HUNDRED, hydecScore * HUNDRED), result.isFullMatch());
      listContainer.setScore(exercise.getID(), hydecScore);
    } else {
      scoreFeedbackDiv.hideScore();
    }
  }

  @Nullable
  private void getReadyToPlayAudio(String path) {
    //logger.info("getReadyToPlayAudio : get ready to play " +path);
    path = CompressedAudio.getPath(path);
    if (path != null) {
      this.audioPath = path;
    }
    if (playAudioPanel != null) {
      //logger.info("getReadyToPlayAudio startSong ready to play " +path);
      playAudioPanel.startSong(path, true);
    }
  }

  /**
   * @param scores
   * @see #SimpleRecordAudioPanel
   */
  private void showRecordingHistory(List<CorrectAndScore> scores) {
    if (scores != null) {

      for (CorrectAndScore score : scores) {
        miniScoreListener.addScore(score);
      }
      setVisible(hasScoreHistory);
    }
    //else {
    //logger.warning("scores is null?");
    // }

    miniScoreListener.showChart(controller.getHost());
  }

  private WaitCursorHelper makeWaitCursor() {
    if (waitCursorHelper == null) {
      waitCursorHelper = new WaitCursorHelper();
      waitCursorHelper.showFinished();
    }
    return waitCursorHelper;
  }

  @Override
  public String getSession() {
    return controller.getBrowserInfo();
  }
}
