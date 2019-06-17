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
import com.google.gwt.dom.client.Style;
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
  //private final Logger logger = Logger.getLogger("SimpleRecordAudioPanel");

  private static final String MP3 = ".mp3";
  public static final String OGG = ".ogg";

  private static final float HUNDRED = 100.0f;
  /**
   * Try to line up history word(s) with prompt
   */
  private static final int LEFT_MARGIN_FOR_SCORE = 112;
  private WaitCursorHelper waitCursorHelper = null;

  private MiniScoreListener miniScoreListener;

  private String audioPath;

  private boolean hasScoreHistory;
  private final ListInterface<?, ?> listContainer;
  private final boolean isRTL;
  private ScoreFeedbackHelper scoreFeedbackHelper;
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
  public SimpleRecordAudioPanel(ExerciseController controller,
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
   * First is record feedback
   * next is score feedback
   * next is history
   *
   * @return
   * @seex #AudioPanel
   * @see SimpleRecordAudioPanel#SimpleRecordAudioPanel
   */
  @Override
  public void addWidgets() {
    //long then = System.currentTimeMillis();
    DivWidget col = new DivWidget();
    col.getElement().setId("scoreFeedback_" + exercise.getID() + "_container");

    recordFeedback = makePlayAudioPanel().getRecordFeedback(makeWaitCursor().getWaitCursor());
    recordFeedback.getElement().getStyle().setProperty("minWidth", CONTEXT_INDENT + "px");
    col.add(recordFeedback);

    col.add(scoreFeedback = new DivWidget());
    getScoreFeedback().getElement().setId("scoreFeedback_" + exercise.getID());

    col.add(getScoreHistoryDiv());

    this.scoreFeedbackHelper = new ScoreFeedbackHelper(getPlayAudioPanel(), getPlayAudioPanel(), getPlayAudioPanel().getRealDownloadContainer(), true);

    add(col);
    // long now = System.currentTimeMillis();
    // logger.info("addWidgets "+ (now-then)+ " millis");
  }

  /**
   * So just make the score invisible, to avoid the record button bouncing up at the moment when you press it.
   */
  @Override
  void clearScoreFeedback() {
    scoreFeedback.getElement().getStyle().setOpacity(0.0);
  }

  @NotNull
  private Widget getScoreHistoryDiv() {
//    DivWidget historyHoriz = new DivWidget();
//    historyHoriz.addStyleName("inlineFlex");
//    DivWidget spacer = new DivWidget();
//    spacer.getElement().getStyle().setProperty("minWidth", CONTEXT_INDENT + "px");
//
//    historyHoriz.add(spacer);

    // historyHoriz.add(scoreHistory = getScoreHistory());
    scoreHistory = getScoreHistory();
    scoreHistory.getElement().getStyle().setMarginLeft(LEFT_MARGIN_FOR_SCORE, Style.Unit.PX);

    return scoreHistory;
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
    historyPanel.showChart();
    return historyPanel;
  }

  /**
   * @see mitll.langtest.server.DownloadServlet#returnAudioFile
   * @see #useResult(AudioAnswer)
   */
  private void setDownloadHref() {
    String audioPathToUse = audioPath.endsWith(OGG) ? audioPath.replaceAll(OGG, MP3) : audioPath;
    getPlayAudioPanel().setDownloadHref(audioPathToUse, exercise.getID(), getUser(), controller.getHost());
  }

  private int getUser() {
    return controller.getUserState().getUser();
  }


  @Override
  public void startRecording() {
    //logger.info("startRecording...");
    super.startRecording();
    // scoreFeedbackHelper.hideScore();
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
   * Really remove the former score when we get a new one.
   * Make the score div visible again.
   *
   * @param result
   * @param isRTL
   * @see #useResult
   */
  private void scoreAudio(SimpleAudioAnswer result, boolean isRTL) {
    scoreFeedback.clear();

    clearScoreFeedback();

    PretestScore pretestScore = result.getPretestScore();
    getScoreFeedback().add(scoreFeedbackHelper.getWordTableContainer(pretestScore, isRTL));
    scoreFeedback.getElement().getStyle().setOpacity(1.0);
    scoreHistory.getElement().getStyle().setMarginLeft(5, Style.Unit.PX);
    useScoredResult(pretestScore, result.getPath());

    // Gotta remember the score on the exercise now...
    exercise.getScores().add(new CorrectAndScore(result));
//    logger.info("exercise " + exercise.getID() + " now has " + exercise.getScores().size() + " scores");
  }

  /**
   * TODO : this is a bad idea to alter the score unless the praise is based on the same score...
   *
   * Has hack to use word score for the overall score if there's only one word - needed???
   *
   * @param result
   * @param path
   * @see #scoreAudio
   */
  private void useScoredResult(PretestScore result, String path) {
    float overallScore = result.getOverallScore();
    boolean isValid = overallScore > 0;
    if (miniScoreListener != null && isValid) {
      miniScoreListener.gotScore(result, path);
    }
    if (path != null) getReadyToPlayAudio(path);  // not sure why path would ever be null...
    if (isValid) {
/*      List<TranscriptSegment> transcriptSegments = result.getTypeToSegments().get(NetPronImageType.WORD_TRANSCRIPT);
      if (transcriptSegments != null && transcriptSegments.size() == 1) {
        //  float wordScore = transcriptSegments.get(0).getScore();
        //  logger.info("useScoredResult using word score " + wordScore + " instead of hydec score " + overallScore);
        float wordScore = transcriptSegments.get(0).getScore();
        if (Math.abs(overallScore - wordScore) > 0.001) {
          logger.info("useScoredResult using word score " + wordScore + " instead of overall score " + overallScore);
          overallScore = wordScore;
        }
      }*/

      scoreFeedbackHelper.showScore(Math.min(HUNDRED, overallScore * HUNDRED), result.isFullMatch());
      listContainer.setScore(exercise.getID(), overallScore);
    } else {
      scoreFeedbackHelper.hideScore();
    }
  }

  @Nullable
  private void getReadyToPlayAudio(String path) {
    //logger.info("getReadyToPlayAudio : get ready to play " +path);
    path = CompressedAudio.getPath(path);
    this.audioPath = path;
    if (getPlayAudioPanel() != null) {
      //logger.info("getReadyToPlayAudio startSong ready to play " +path);
      getPlayAudioPanel().startSong(path, true);
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
    miniScoreListener.showChart();
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
