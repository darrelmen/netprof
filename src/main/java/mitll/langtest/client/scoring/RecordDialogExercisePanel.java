package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.banner.*;
import mitll.langtest.client.dialog.IRehearseView;
import mitll.langtest.client.dialog.ListenViewHelper;
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
                                   ListenViewHelper.COLUMNS columns) {
    super(commonExercise, controller, listContainer, alignments, listenView, columns);
    this.rehearseView = listenView;

    setMinExpectedDur(commonExercise);

    this.sessionManager = sessionManager;
  //  addStyleName("inlineFlex");
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

  private TreeMap<TranscriptSegment, IHighlightSegment> transcriptToHighlight = null;

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
      AlignmentOutput alignmentOutput = studentAudioAttribute.getAlignmentOutput();
      alignmentOutput.setShowPhoneScores(true);
      transcriptToHighlight =
          showAlignment(0, studentAudioAttribute.getDurationInMillis(), alignmentOutput);
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

  public void obscureText() {
    flclickables.forEach(IHighlightSegment::obscureText);
  }

  public void restoreText() {
    flclickables.forEach(IHighlightSegment::restoreText);
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
   * @see RehearseViewHelper#useResult
   * @return
   */
/*  public float getSpeakingRate() {
    float v = (refSpeechDur == 0F || studentSpeechDur == 0F) ? -1F : (studentSpeechDur / refSpeechDur);
    logger.info("getSpeakingRate " + getExID() + " student " + studentSpeechDur + " ref " + refSpeechDur + " ratio " + v);
    return v;
  }*/

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
    NoFeedbackRecordAudioPanel<ClientExercise> recordPanel =
        new NoFeedbackRecordAudioPanel<ClientExercise>(exercise, controller, sessionManager) {
      /**
       *
       * SO in an async world, this result may not be for this exercise panel!
       *
       * @see PostAudioRecordButton#onPostSuccess(AudioAnswer, long)
       * @see FeedbackPostAudioRecordButton#useResult
       * @param result
       */
      @Override
      public void useResult(AudioAnswer result) {
        super.useResult(result);
        rehearseView.useResult(result);

        if (false) {
          logger.info("useResult got for ex " + result.getExid() + " vs local " + getExID() +
              " = " + result.getValidity() + " " + result.getPretestScore());
        }
        // logger.info("useResult got words " + result.getPretestScore().getWordScores());
      }

      @Override
      Widget getPopupTargetWidget() {
        return myGetPopupTargetWidget();
      }

      /**
       * @see PostAudioRecordButton#usePartial
       * @param response
       */
      @Override
      public void usePartial(StreamResponse response) {
        RecordDialogExercisePanel.this.usePartial(response);
      }

      @Override
      public void gotAbort() {
        logger.info("OK got abort!");
      }

      /**
       *
       */
      @Override
      public void onPostFailure() {
        logger.info("onPostFailure exid " + getExID());
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

        logger.warning("useInvalidResult got valid = " + isValid);
      }

      /**
       * @see RecordButton.RecordingListener#stopRecording(long, boolean)
       */
      @Override
      public void stopRecording() {
        super.stopRecording();
        rehearseView.stopRecording();
      }

      @Override
      public int getDialogSessionID() {
        return rehearseView.getDialogSessionID();
      }
    };

    this.recordAudioPanel = recordPanel;

    recordPanel.addWidgets();

    DivWidget flContainer = getHorizDiv();
    if (columns == ListenViewHelper.COLUMNS.RIGHT) {
      addStyleName("floatRight");
    } else if (columns == ListenViewHelper.COLUMNS.LEFT){
      flContainer.addStyleName("floatLeft");
    }

    // add hidden button
    {
      PostAudioRecordButton postAudioRecordButton = recordPanel.getPostAudioRecordButton();
      postAudioRecordButton.setVisible(false);
      flContainer.add(postAudioRecordButton);
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

  private void usePartial(StreamResponse response) {
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
        gotStreamStop = true;
      }
    } else {
      logger.warning("hmm " + report() + " getting response " + response + " but not recording...?");
    }
  }

  private String report() {
    return this.toString();
  }

  private Widget myGetPopupTargetWidget() {
    return this;
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
   * @see #addWidgets(boolean, boolean, PhonesChoices)
   * @return
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
   * @see RehearseViewHelper#currentTurnPlayEnded
   */
  public void startRecording() {
    start = System.currentTimeMillis();
    logger.info("startRecording for " + getExID() + " at " + start + " or " + new Date(start));
    firstVAD = -1;
    recordAudioPanel.getPostAudioRecordButton().startOrStopRecording();
  }

  /**
   * TODO : simplify this a lot...
   * Check against expected duration to see when to end.
   *
   * @see #startRecording
   * @see RehearseViewHelper#mySilenceDetected()
   */
  public boolean stopRecording() {
    long now = System.currentTimeMillis();

    long diff = now - start;

    long minDurPlusMoveOn = minDur + MOVE_ON_DUR;
    long diffVAD = now - firstVAD;
    boolean clientVAD = firstVAD > 0 && diffVAD > minDur + END_SILENCE;
    boolean vadCheck = clientVAD && gotStreamStop;


    if (vadCheck || diff > minDurPlusMoveOn) {
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
      recordAudioPanel.getPostAudioRecordButton().startOrStopRecording();
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
    return recordAudioPanel.getPostAudioRecordButton().isRecording();
  }

  public float getRefSpeechDur() {
    return refSpeechDur;
  }
  public float getStudentSpeechDur() {
    return studentSpeechDur;
  }
}