package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.banner.IListenView;
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

import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

public class RecordDialogExercisePanel<T extends ClientExercise> extends TurnPanel<T> implements IRecordDialogTurn {
  private final Logger logger = Logger.getLogger("RecordDialogExercisePanel");

  private static final int DIM = 40;
  private static final long END_DUR_SKEW = 900L;
  private long start = 0;

  private NoFeedbackRecordAudioPanel<T> recordAudioPanel;
  private static final float DELAY_SCALAR = 1.0F;

  private long minDur;
  private Image emoticon;
  private AlignmentOutput alignmentOutput;
  private final SessionManager sessionManager;

  public RecordDialogExercisePanel(final T commonExercise,
                                   final ExerciseController controller,
                                   final ListInterface<?, ?> listContainer,
                                   Map<Integer, AlignmentOutput> alignments,
                                   IListenView listenView,
                                   SessionManager sessionManager,
                                   boolean isRight) {
    super(commonExercise, controller, listContainer, alignments, listenView, isRight);
    if (commonExercise.hasRefAudio()) {
      minDur = commonExercise.getAudioAttributes().iterator().next().getDurationInMillis();
      minDur = (long) (((float) minDur) * DELAY_SCALAR);
      minDur -= END_DUR_SKEW;
    }
    this.sessionManager = sessionManager;
    addStyleName("inlineFlex");
  }

  /**
   * TODO : do this better - should
   */
  @Override
  public void showScoreInfo() {
    emoticon.setVisible(true);

   /* List<TranscriptSegment> transcriptSegments = alignmentOutput.getTypeToSegments().get(NetPronImageType.WORD_TRANSCRIPT);
    transcriptSegments.sort(TranscriptSegment::compareTo);
    Iterator<TranscriptSegment> scoredWords = transcriptSegments.iterator();
    Iterator<IHighlightSegment> highlightSegment = flclickables.iterator();

    while (scoredWords.hasNext()) {
      TranscriptSegment scoredWord = scoredWords.next();
      if (highlightSegment.hasNext()) {

        IHighlightSegment next1 = highlightSegment.next();
        while (!next1.isClickable()) {
          next1 = highlightSegment.next();
        }

        String color = SimpleColumnChart.getColor(scoredWord.getScore());

        logger.info("word '" + scoredWord.getDisplayEvent() +
            "' or '" + scoredWord.getEvent() +
            "' = " + scoredWord.getScore() + " = " + color);

        next1.setHighlightColor(color);
        next1.showHighlight();
      }
    }*/

    TreeMap<TranscriptSegment, IHighlightSegment> transcriptSegmentIHighlightSegmentTreeMap = showAlignment(0, durationInMillis, alignmentOutput);

    if (transcriptSegmentIHighlightSegmentTreeMap != null) {
      transcriptSegmentIHighlightSegmentTreeMap.forEach((k, v) -> {
        String color = SimpleColumnChart.getColor(k.getScore());
/*

        logger.info("word '" + k.getDisplayEvent() +
            "' or '" + k.getEvent() +
            "' = " + k.getScore() + " = " + color);
*/

        v.setHighlightColor(color);
        v.showHighlight();
      });
    }
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

  private long durationInMillis;

  @Override
  public void addWidgets(boolean showFL, boolean showALTFL, PhonesChoices phonesChoices) {
    NoFeedbackRecordAudioPanel<T> recordPanel = new NoFeedbackRecordAudioPanel<T>(exercise, controller, sessionManager) {
      @Override
      public void useResult(AudioAnswer result) {
        super.useResult(result);
        alignmentOutput = result.getPretestScore();
        durationInMillis = result.getDurationInMillis();
        double score = result.getScore();
        listenView.addScore(result.getExid(), (float) score, RecordDialogExercisePanel.this);
        listenView.setSmiley(emoticon, score);

        logger.info("useResult got for   " + getExID() + " = " + result.getValidity() + " " + score);
        // logger.info("useResult got words " + result.getPretestScore().getWordScores());
      }

      @Override
      Widget getPopupTargetWidget() {
        return myGetPopupTargetWidget();
      }

      @Override
      public void usePartial(Validity validity) {
        listenView.addPacketValidity(validity);
      }

      @Override
      public void onPostFailure() {
        logger.info("onPostFailure exid " +getExID());
        stopRecording();
      }

      /**
       * TODO : do something smarter here on invalid state
       * @param isValid
       */
      @Override
      public void useInvalidResult(boolean isValid) {
        super.useInvalidResult(isValid);
        logger.info("useInvalidResult got valid = " + isValid);
      }


      /**
       * @see FeedbackPostAudioRecordButton#stopRecording(long)
       */
      @Override
      public void stopRecording() {
        super.stopRecording();
        listenView.stopRecording();
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
    recordAudioPanel.getPostAudioRecordButton().startOrStopRecording();
  }


  /**
   * @return
   * @see RehearseViewHelper#mySilenceDetected()
   */
  public boolean isRecording() {
    return recordAudioPanel.getPostAudioRecordButton().isRecording();
  }

  /**
   * Check against expected duration too see when to end.
   *
   * @see RehearseViewHelper#mySilenceDetected()
   */
  public boolean stopRecording() {
    long now = System.currentTimeMillis();

    long diff = now - start;
    if (diff > minDur) {
      recordAudioPanel.getPostAudioRecordButton().startOrStopRecording();
      return true;
    } else {
//      logger.info("stopRecording ignore too short " + diff + " vs " + minDur);
      return false;
    }
  }
}
