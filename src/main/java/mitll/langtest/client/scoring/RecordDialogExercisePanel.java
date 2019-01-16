package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.banner.Emoticon;
import mitll.langtest.client.banner.SessionManager;
import mitll.langtest.client.dialog.IRehearseView;
import mitll.langtest.client.dialog.ListenViewHelper.COLUMNS;
import mitll.langtest.client.dialog.PerformViewHelper;
import mitll.langtest.client.dialog.RehearseViewHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.gauge.SimpleColumnChart;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.sound.IHighlightSegment;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.Validity;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.instrumentation.SlimSegment;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.AlignmentOutput;
import mitll.langtest.shared.scoring.NetPronImageType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static mitll.langtest.client.LangTest.RED_X_URL;
import static mitll.langtest.client.dialog.ListenViewHelper.COLUMNS.*;

public class RecordDialogExercisePanel extends TurnPanel implements IRecordDialogTurn {
  private final Logger logger = Logger.getLogger("RecordDialogExercisePanel");

  private static final boolean DEBUG_PARTIAL = false;

  private static final long MOVE_ON_DUR = 3000L;
  private static final long END_SILENCE = 300L;

  private static final int DIM = 40;
  private static final long END_DUR_SKEW = 900L;

  /**
   * @see #startRecording
   */
  private long start = 0L;
  private long firstVAD = -1L;

  private NoFeedbackRecordAudioPanel<ClientExercise> recordAudioPanel;
  private static final float DELAY_SCALAR = 1.0F;
  /**
   *
   */
  private long minDur;
  private Emoticon emoticon;
  private final SessionManager sessionManager;

  private final IRehearseView rehearseView;

  private float refSpeechDur = 0F;
  private float studentSpeechDur = 0F;

  private AudioAttribute studentAudioAttribute;
  private boolean gotStreamStop;
  private TreeMap<TranscriptSegment, IHighlightSegment> transcriptToHighlight = null;
  boolean doPushToTalk = false;

  /**
   * @param commonExercise
   * @param controller
   * @param listContainer
   * @param alignments
   * @param listenView
   * @param sessionManager
   * @param isRight
   * @see RehearseViewHelper#getRecordingTurnPanel
   */
  public RecordDialogExercisePanel(final ClientExercise commonExercise,
                                   final ExerciseController controller,
                                   final ListInterface<?, ?> listContainer,
                                   Map<Integer, AlignmentOutput> alignments,
                                   IRehearseView listenView,
                                   SessionManager sessionManager,
                                   COLUMNS columns) {
    super(commonExercise, controller, listContainer, alignments, listenView, columns);
    this.rehearseView = listenView;
    setMinExpectedDur(commonExercise);
    this.sessionManager = sessionManager;
  }

  private void setMinExpectedDur(ClientExercise commonExercise) {
    if (commonExercise.hasRefAudio()) {
      minDur = commonExercise.getFirst().getDurationInMillis();
      minDur = (long) (((float) minDur) * DELAY_SCALAR);
      minDur -= END_DUR_SKEW;
    }
  }

  /**
   * @param id
   * @param duration
   * @param alignmentOutput
   * @return
   * @see #showScoreInfo
   * @see #maybeShowAlignment
   * @see #audioChanged
   */
  @Override
  protected TreeMap<TranscriptSegment, IHighlightSegment> showAlignment(int id, long duration, AlignmentOutput alignmentOutput) {
    TreeMap<TranscriptSegment, IHighlightSegment> transcriptSegmentIHighlightSegmentTreeMap =
        super.showAlignment(id, duration, alignmentOutput);
    this.refSpeechDur = getSpeechDur(id, alignmentOutput);
    return transcriptSegmentIHighlightSegmentTreeMap;
  }

  private float getSpeechDur(int id, AlignmentOutput alignmentOutput) {
    List<TranscriptSegment> transcriptSegments = alignmentOutput == null ? null : alignmentOutput.getTypeToSegments().get(NetPronImageType.WORD_TRANSCRIPT);
    if (transcriptSegments == null || transcriptSegments.isEmpty()) {
      logger.warning("getSpeechDur : huh? no transcript for " + id + " for ex " + exercise.getID() + " " + exercise.getForeignLanguage() + " " + exercise.getEnglish());
      return 0F;
    } else {
      TranscriptSegment first = transcriptSegments.get(0);
      float start = first.getStart();
      TranscriptSegment last = transcriptSegments.get(transcriptSegments.size() - 1);
      float end = last.getEnd();
      // logger.info("getSpeechDur " + first.getEvent() + " - " + last.getEvent());
      return end - start;
    }
  }

  @Override
  public void clearScoreInfo() {
    transcriptToHighlight = null;
    gotStreamStop = false;
    emoticon.setVisible(false);

    flclickables.forEach(iHighlightSegment -> {
      iHighlightSegment.setHighlightColor(IHighlightSegment.DEFAULT_HIGHLIGHT);
      iHighlightSegment.clearHighlight();
    });

    rememberAudio(getRegularSpeedIfAvailable(exercise));
  }

  /**
   * TODOx : do this better - should
   *
   * @see RehearseViewHelper#showScores
   * @see #switchAudioToStudent
   */
  @Override
  public void showScoreInfo() {
    //   logger.info("showScoreInfo for " + this);
    emoticon.setVisible(true);

    if (transcriptToHighlight == null) {
      if (studentAudioAttribute != null) {
        AlignmentOutput alignmentOutput = studentAudioAttribute.getAlignmentOutput();
        alignmentOutput.setShowPhoneScores(true);

        transcriptToHighlight = showAlignment(0, studentAudioAttribute.getDurationInMillis(), alignmentOutput);
      } else {
        logger.warning("showScoreInfo no student audio for " + this);
      }
    }

    revealScore();
  }

  public void revealScore() {
    if (transcriptToHighlight != null) {
      transcriptToHighlight.forEach(this::showWordScore);
    } else {
      logger.warning("revealScore : no transcript map for " + this);
    }
  }

  private void showWordScore(SlimSegment withScore, IHighlightSegment v) {
    v.restoreText();
    v.setHighlightColor(SimpleColumnChart.getColor(withScore.getScore()));
    v.showHighlight();
  }

  /**
   * Rules:
   *
   * 1) don't obscure everything
   * 2) obscure something
   * 3) Don't obscure more than one or two or three? words?
   * 4) if choosing only two out of all of them, choose the longest ones?
   * 3) if you have a choice, don't obscure first token? ?
   *
   * @param coreVocab
   * @see PerformViewHelper#getTurnPanel
   * Or should we use exact match?
   */
  public void maybeSetObscure(Collection<String> coreVocab) {
    IHighlightSegment candidate = getObscureCandidate(coreVocab, true);

    if (candidate == null || flclickables.indexOf(candidate) == 0) {
      candidate = getObscureCandidate(coreVocab, false);
    }

    if (candidate != null) {
      candidate.setObscurable();
    }
  }

  @Nullable
  private IHighlightSegment getObscureCandidate(Collection<String> coreVocab, boolean useExact) {
    IHighlightSegment candidate = null;

    boolean isFirst = true;

    for (IHighlightSegment segment : flclickables) {
      List<String> matches = coreVocab
          .stream()
          .filter(core ->
              useExact ?
                  clickableWords.isExactMatch(segment.getContent(), core) :
                  clickableWords.isSearchMatch(segment.getContent(), core)
          ).collect(Collectors.toList());

      if (!matches.isEmpty()) {
        // logger.info("getObscureCandidate for " + segment + " found " + matches + (useExact ? " exact" : " contains"));
        candidate = segment;
        if (!isFirst && candidate.getContent().length() > 1) break;
      }

      isFirst = false;
    }
    return candidate;
  }

  public void reallyObscure() {
    //  logger.info("reallyObscure For " + exercise.getID() + " obscure " + flclickables.size() + " clickables");
    flclickables.forEach(iHighlightSegment -> {
      iHighlightSegment.setObscurable();
      boolean b = iHighlightSegment.obscureText();
      if (!b) logger.info("huh? didn't obscure");
    });
    flClickableRowPhones.setVisible(false);
  }

  public void obscureText() {
//    logger.info("obscureText For " + exercise.getID() + " obscure " + flclickables.size() + " clickables");
    flclickables.forEach(IHighlightSegment::obscureText);
  }

  public void restoreText() {
    flclickables.forEach(IHighlightSegment::restoreText);
    flClickableRowPhones.setVisible(true);
  }

  /**
   * @param result
   * @see RehearseViewHelper#useResult
   */
  @Override
  public void useResult(AudioAnswer result) {
    this.studentSpeechDur = getSpeechDur(result.getExid(), result.getPretestScore());

    studentAudioAttribute = new AudioAttribute();
    studentAudioAttribute.setAudioRef(result.getPath());
    studentAudioAttribute.setAlignmentOutput(result.getPretestScore());
    studentAudioAttribute.setDurationInMillis(result.getDurationInMillis());

    emoticon.setEmoticon(result.getScore(), controller.getLanguageInfo());
  }

  @Override
  public void switchAudioToStudent() {
    //logger.info("switchAudioToStudent " + this);
    playAudio.rememberAudio(studentAudioAttribute);
    showScoreInfo();
  }

  @Override
  public void switchAudioToReference() {
    //logger.info("switchAudioToReference " + this);
    clearScoreInfo();
  }

  /**
   * @see RehearseViewHelper#useInvalidResult
   * Maybe color in the words red?
   */
  @Override
  public void useInvalidResult() {
    emoticon.setUrl(RED_X_URL);
  }

  /**
   * @param showFL
   * @param showALTFL
   * @param phonesChoices
   */

  @Override
  public void addWidgets(boolean showFL, boolean showALTFL, PhonesChoices phonesChoices) {
    boolean isRehearse = rehearseView instanceof PerformViewHelper;
    logger.info("is perform " + isRehearse);

    NoFeedbackRecordAudioPanel<ClientExercise> recordPanel =
//        isRehearse ?
//            new PushToTalkDialogRecordAudioPanel(exercise, controller, sessionManager, rehearseView, this) :
        new ContinuousDialogRecordAudioPanel(exercise, controller, sessionManager, rehearseView, this);

    this.recordAudioPanel = recordPanel;

    recordPanel.addWidgets();

    DivWidget flContainer = getHorizDiv();
    if (columns == RIGHT) {
      addStyleName("floatRight");
    } else if (columns == LEFT) {
      flContainer.addStyleName("floatLeft");
    }

    // add hidden button
    {
      PostAudioRecordButton postAudioRecordButton = getPostAudioRecordButton(recordPanel);
      postAudioRecordButton.setVisible(false);
      DivWidget buttonContainer = new DivWidget();
      buttonContainer.setId("recordButtonContainer_" + getExID());
      buttonContainer.add(postAudioRecordButton);
      //   postAudioRecordButton.setEnabled(false);
      flContainer.add(buttonContainer);
    }

    flContainer.add(recordPanel.getScoreFeedback());
    {
      Emoticon w = getEmoticonPlaceholder();
      emoticon = w;
      flContainer.add(w);
    }

    add(flContainer);
    super.addWidgets(showFL, showALTFL, phonesChoices);
  }

  private PostAudioRecordButton getPostAudioRecordButton(NoFeedbackRecordAudioPanel<ClientExercise> recordPanel) {
    return recordPanel.getPostAudioRecordButton();
  }

  public void usePartial(StreamResponse response) {
    if (isRecording()) {
      Validity validity = response.getValidity();
      rehearseView.addPacketValidity(validity);

      if (validity == Validity.OK && firstVAD == -1) {
        firstVAD = response.getStreamTimestamp();//System.currentTimeMillis();
        if (DEBUG_PARTIAL) {
          logger.info("usePartial : (" + rehearseView.getNumValidities() +
              ") got first vad : " + firstVAD +
              " (" + (start - firstVAD) + ")" +
              " for " + report() + " diff " + (System.currentTimeMillis() - start));
        }
      } else {
        if (DEBUG_PARTIAL) {
          logger.info("usePartial : (" + rehearseView.getNumValidities() +
              " packets) skip validity " + validity +
              " vad " + firstVAD +
              " for " + report() + " diff " + (System.currentTimeMillis() - start));
        }
      }

      if (response.isStreamStop()) {
        // logger.info("usePartial stopStream");
        gotStreamStop = true;
      }
    } else {
      logger.warning("hmm " + report() + " getting response " + response + " but not recording...?");
    }
  }

  private String report() {
    return this.toString();
  }

  public Widget myGetPopupTargetWidget() {
    return this;
  }

  /**
   * TODO :subclass!
   */
   @Override
  public void markCurrent() {
    super.markCurrent();

    if (doPushToTalk) {
      if (columns == MIDDLE) {
        getRecordButton().setVisible(true);
      }
    }
  }

  @Override
  public void removeMarkCurrent() {
    super.removeMarkCurrent();

    if (doPushToTalk) {
      if (getRecordButton().isRecording()) {
        cancelRecording();
      }
    }
  }

  /**
   * @see RehearseViewHelper#gotPlay()
   */
  public void cancelRecording() {
    recordAudioPanel.cancelRecording();
  }

  @NotNull
  private Emoticon getEmoticonPlaceholder() {
    Emoticon w = new Emoticon();
    w.setVisible(false);
    w.setHeight(DIM + "px");
    w.setWidth(DIM + "px");
    w.getElement().getStyle().setMarginTop(7, Style.Unit.PX);
    return w;
  }

  /**
   * @return
   * @see #addWidgets(boolean, boolean, PhonesChoices)
   */
  @NotNull
  private DivWidget getHorizDiv() {
    DivWidget flContainer = new DivWidget();
    flContainer.addStyleName("inlineFlex");
    flContainer.getElement().getStyle().setMarginTop(15, Style.Unit.PX);
    flContainer.getElement().setId("RecordDialogExercisePanel_horiz");
    return flContainer;
  }

  protected void addMarginLeft(Style style2) {
    style2.setMarginLeft(15, Style.Unit.PX);
  }

  /**
   * @see RehearseViewHelper#startRecordingTurn
   */
  public void startRecording() {
    start = System.currentTimeMillis();
    logger.info("startRecording for " + getExID() + " at " + start + " or " + new Date(start));
    firstVAD = -1;

    if (columns == MIDDLE && doPushToTalk) {
      getRecordButton().setVisible(true);
    } else {
      getRecordButton().startOrStopRecording();
    }
  }

  private PostAudioRecordButton getRecordButton() {
    return getPostAudioRecordButton(recordAudioPanel);
  }

  /**
   * TODO : simplify this a lot...
   * Check against expected duration to see when to end.
   *
   * @see #startRecording
   * @see RehearseViewHelper#stopRecordingTurn()
   */
  public boolean stopRecording() {
    long now = System.currentTimeMillis();

    long diff = now - start;

    long minDurPlusMoveOn = minDur + MOVE_ON_DUR;
    long diffVAD = now - firstVAD;
    boolean clientVAD = firstVAD > 0 && diffVAD > minDur + END_SILENCE;
    boolean vadCheck = clientVAD && gotStreamStop;


    if (vadCheck || diff > minDurPlusMoveOn || doPushToTalk) {
      logger.info("stopRecording " + this +
          "\n\tvadCheck  " + vadCheck +
          "\n\tgotStreamStop " + gotStreamStop +
          "\n\tfirstVAD  " + firstVAD +
          "\n\tVAD delay " + (diffVAD - diff) +
          "\n\tdiffVAD   " + diffVAD +
          "\n\tminDur    " + minDur +
          "\n\tminDur+move on " + minDurPlusMoveOn +
          "\n\tdiff      " + diff
      );
      getRecordButton().startOrStopRecording();
      return true;
    } else {
/*
      logger.info("stopRecording for " + getExID() +
          " : ignore too short " + diffVAD + " vad vs " + minDur + " expected client " + clientVAD + " vs server " + gotStreamStop);
*/
      return false;
    }
  }

  /**
   * @return
   * @see RehearseViewHelper#mySilenceDetected()
   */
  public boolean isRecording() {
    return getRecordButton().isRecording();
  }

  public float getRefSpeechDur() {
    return refSpeechDur;
  }

  public float getStudentSpeechDur() {
    return studentSpeechDur;
  }

  private static class ContinuousDialogRecordAudioPanel extends NoFeedbackRecordAudioPanel<ClientExercise> {
    IRehearseView rehearseView;
    IRecordDialogTurn recordDialogTurn;
    private final Logger logger = Logger.getLogger("ContinuousDialogRecordAudioPanel");

    ContinuousDialogRecordAudioPanel(ClientExercise exercise, ExerciseController controller, SessionManager sessionManager,
                                     IRehearseView rehearseView,
                                     IRecordDialogTurn recordDialogTurn) {
      super(exercise, controller, sessionManager);
      this.rehearseView = rehearseView;
      this.recordDialogTurn = recordDialogTurn;
    }

    /**
     * SO in an async world, this result may not be for this exercise panel!
     *
     * @param result
     * @see PostAudioRecordButton#onPostSuccess(AudioAnswer, long)
     * @see FeedbackPostAudioRecordButton#useResult
     */
    @Override
    public void useResult(AudioAnswer result) {
      super.useResult(result);
      getPostAudioRecordButton().setVisible(false);
      rehearseView.useResult(result);

      if (DEBUG) {
        logger.info("useResult got for ex " + result.getExid() + " vs local " + exercise.getID() +
            " = " + result.getValidity() + " " + result.getPretestScore());
      }
      // logger.info("useResult got words " + result.getPretestScore().getWordScores());
    }

    @Override
    Widget getPopupTargetWidget() {
      return recordDialogTurn.myGetPopupTargetWidget();
    }

    /**
     * @param response
     * @see PostAudioRecordButton#usePartial
     */
    @Override
    public void usePartial(StreamResponse response) {
      recordDialogTurn.usePartial(response);
    }

//    @Override
//    public void gotAbort() {
//      //logger.info("OK got abort!");
//    }

    /**
     *
     */
    @Override
    public void onPostFailure() {
      logger.info("onPostFailure exid " + exercise.getID());
      stopRecording();
    }

    /**
     * TODO : do something smarter here on invalid state????
     *
     * @param exid
     * @param isValid
     * @see PostAudioRecordButton#useInvalidResult
     */
    @Override
    public void useInvalidResult(int exid, boolean isValid) {
      super.useInvalidResult(exid, isValid);
      rehearseView.useInvalidResult(exid);
      getPostAudioRecordButton().setVisible(false);

      logger.warning("useInvalidResult got valid = " + isValid);
    }

    /**
     * @see RecordButton.RecordingListener#stopRecording(long, boolean)
     */
    @Override
    public void stopRecording() {
      // logger.info("stopRecording for " + exercise.getID() + " " + exercise.getEnglish() + " " + exercise.getForeignLanguage());
      super.stopRecording();
      rehearseView.stopRecording();
    }

    @Override
    public int getDialogSessionID() {
      return rehearseView.getDialogSessionID();
    }
  }
}