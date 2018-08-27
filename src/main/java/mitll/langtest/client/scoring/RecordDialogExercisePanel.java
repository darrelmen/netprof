package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.banner.IRehearseView;
import mitll.langtest.client.banner.RehearseViewHelper;
import mitll.langtest.client.banner.SessionManager;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.gauge.SimpleColumnChart;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.sound.IHighlightSegment;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.Validity;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.AlignmentOutput;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class RecordDialogExercisePanel<T extends ClientExercise> extends TurnPanel<T> implements IRecordDialogTurn {
  private final Logger logger = Logger.getLogger("RecordDialogExercisePanel");

  private static final long MOVE_ON_DUR = 3000L;
  private static final long END_SILENCE = 300L;

  private static final int DIM = 40;
  private static final long END_DUR_SKEW = 900L;
  private long start = 0L;
  private long firstVAD = -1L;

  private NoFeedbackRecordAudioPanel<T> recordAudioPanel;
  private static final float DELAY_SCALAR = 1.0F;

  private long minDur;
  private Image emoticon;
  private AlignmentOutput alignmentOutput;
  private final SessionManager sessionManager;
  private long durationInMillis;
  private IRehearseView rehearseView;

  /**
   * @param commonExercise
   * @param controller
   * @param listContainer
   * @param alignments
   * @param listenView
   * @param sessionManager
   * @param isRight
   * @see RehearseViewHelper#reallyGetTurnPanel(ClientExercise, boolean)
   */
  public RecordDialogExercisePanel(final T commonExercise,
                                   final ExerciseController controller,
                                   final ListInterface<?, ?> listContainer,
                                   Map<Integer, AlignmentOutput> alignments,
                                   IRehearseView listenView,
                                   SessionManager sessionManager,
                                   boolean isRight) {
    super(commonExercise, controller, listContainer, alignments, listenView, isRight);
    this.rehearseView = listenView;

    if (commonExercise.hasRefAudio()) {
      minDur = commonExercise.getAudioAttributes().iterator().next().getDurationInMillis();
      minDur = (long) (((float) minDur) * DELAY_SCALAR);
      minDur -= END_DUR_SKEW;
    }

    this.sessionManager = sessionManager;
    addStyleName("inlineFlex");
  }

  /**
   * TODOx : do this better - should
   *
   * @see RehearseViewHelper#showScores
   */
  @Override
  public void showScoreInfo() {
    emoticon.setVisible(true);

    TreeMap<TranscriptSegment, IHighlightSegment> transcriptSegmentIHighlightSegmentTreeMap = showAlignment(0, durationInMillis, alignmentOutput);

    if (transcriptSegmentIHighlightSegmentTreeMap != null) {
      transcriptSegmentIHighlightSegmentTreeMap.forEach(this::showWordScore);
    }
  }

  private void showWordScore(TranscriptSegment k, IHighlightSegment v) {
    v.setHighlightColor(SimpleColumnChart.getColor(k.getScore()));
    v.showHighlight();
    v.restoreText();
  }

  @Override
  public void clearScoreInfo() {
    emoticon.setVisible(false);

    flclickables.forEach(iHighlightSegment -> {
      iHighlightSegment.setHighlightColor(IHighlightSegment.DEFAULT_HIGHLIGHT);
      iHighlightSegment.clearHighlight();
    });

    maybeShowAlignment(getRegularSpeedIfAvailable(exercise));
  }

  /**
   * @see mitll.langtest.client.banner.PerformViewHelper#getTurnPanel
   * Or should we use exact match?
   * @param coreVocab
   */
  public void maybeSetObscure(Collection<String> coreVocab) {
    flclickables.forEach(iHighlightSegment ->
    {
      List<String> matches = coreVocab
          .stream()
          .filter(core -> clickableWords.isSearchMatch(iHighlightSegment.getContent(), core)).collect(Collectors.toList());
      if (!matches.isEmpty()) {
        logger.info("maybeSetObscure for " + iHighlightSegment + " found " + matches);
        iHighlightSegment.setObscurable();
      }
    });
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
    alignmentOutput = result.getPretestScore();
    durationInMillis = result.getDurationInMillis();
    rehearseView.setEmoticon(emoticon, result.getScore());
  }

  private static final String RED_X = LangTest.LANGTEST_IMAGES + "redx32.png";
  private static final SafeUri RED_X_URL = UriUtils.fromSafeConstant(RED_X);

  /**
   * Maybe color in the words red?
   */
  @Override
  public void useInvalidResult() {
    // rehearseView.setEmoticon(emoticon, 0F);
    emoticon.setUrl(RED_X_URL);
  }

  /**
   *
   * @param showFL
   * @param showALTFL
   * @param phonesChoices
   */

  @Override
  public void addWidgets(boolean showFL, boolean showALTFL, PhonesChoices phonesChoices) {
    NoFeedbackRecordAudioPanel<T> recordPanel = new NoFeedbackRecordAudioPanel<T>(exercise, controller, sessionManager) {
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
        logger.info("useResult got for ex " + result.getExid() + " vs local " + getExID() +
            " = " + result.getValidity() + " " + result.getPretestScore());
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
        if (isRecording()) {
          Validity validity = response.getValidity();
          rehearseView.addPacketValidity(validity);

          if (validity == Validity.OK && firstVAD == -1) {
            firstVAD = response.getStreamTimestamp();//System.currentTimeMillis();
            logger.info("usePartial : (" + rehearseView.getNumValidities() +
                ") got first vad : " + firstVAD +
                " (" + (start - firstVAD) + ")" +
                " for " + report() + " diff " + (System.currentTimeMillis() - start));
          } else {
            logger.info("usePartial : (" + rehearseView.getNumValidities() +
                " packets) skip validity " + validity +
                " first vad " + firstVAD +
                " (" + (start - firstVAD) + ")" +
                " for " + report() + " diff " + (System.currentTimeMillis() - start));
          }
        } else {
          logger.warning("hmm " + report() + " getting response " + response + " but not recording...?");
        }
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
       * @see FeedbackPostAudioRecordButton#stopRecording(long)
       */
      @Override
      public void stopRecording() {
        super.stopRecording();
        rehearseView.stopRecording();
      }
    };

    this.recordAudioPanel = recordPanel;

    recordPanel.addWidgets();

    DivWidget flContainer = getHorizDiv();
    if (isRight) {
      addStyleName("floatRight");
    } else {
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
      Image w = getEmoticonPlaceholder();
      emoticon = w;
      flContainer.add(w);
    }

    add(flContainer);
    super.addWidgets(showFL, showALTFL, phonesChoices);
  }

  private String report() {
    return this.toString();
  }

  private Widget myGetPopupTargetWidget() {
    return this;
  }

  public void cancelRecording() {
    recordAudioPanel.cancelRecording();
  }

  @NotNull
  private Image getEmoticonPlaceholder() {
    Image w = new Image();
    w.setVisible(false);
    w.setHeight(DIM + "px");
    w.setWidth(DIM + "px");
    w.getElement().getStyle().setMarginTop(7, Style.Unit.PX);
    return w;
  }

  @NotNull
  private DivWidget getHorizDiv() {
    DivWidget flContainer = new DivWidget();
    flContainer.addStyleName("inlineFlex");
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
    firstVAD = -1;
    recordAudioPanel.getPostAudioRecordButton().startOrStopRecording();
  }

  /**
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
    boolean vadCheck = firstVAD > 0 && diffVAD > minDur + END_SILENCE;
    if (vadCheck || diff > minDurPlusMoveOn) {
      logger.info("stopRecording " + this +
          "\n\tvadCheck  " + vadCheck +
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
      logger.info("stopRecording ignore too short " + diff + " vs " + minDur);
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

  public boolean abortRecording() {
    return recordAudioPanel.getPostAudioRecordButton().stopRecordingSafe();
  }
}
