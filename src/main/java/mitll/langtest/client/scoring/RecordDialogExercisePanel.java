package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import mitll.langtest.client.banner.IListenView;
import mitll.langtest.client.banner.RehearseViewHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.gauge.SimpleColumnChart;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.sound.IHighlightSegment;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.AlignmentOutput;
import mitll.langtest.shared.scoring.NetPronImageType;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

public class RecordDialogExercisePanel<T extends ClientExercise> extends DialogExercisePanel<T> implements IRecordDialogTurn {
  private Logger logger = Logger.getLogger("RecordDialogExercisePanel");

  private NoFeedbackRecordAudioPanel<T> recordAudioPanel;
  private static final float DELAY_SCALAR = 1.1F;

  private long minDur;
  private Image emoticon;
  private AlignmentOutput alignmentOutput;

  public RecordDialogExercisePanel(final T commonExercise,
                                   final ExerciseController controller,
                                   final ListInterface<?, ?> listContainer,
                                   Map<Integer, AlignmentOutput> alignments,
                                   IListenView listenView) {
    super(commonExercise, controller, listContainer, alignments, listenView);
    minDur = commonExercise.getAudioAttributes().iterator().next().getDurationInMillis();
    minDur = (long) (((float) minDur) * DELAY_SCALAR);
    //  logger.info("ex " + commonExercise.getID() + " min dur " + minDur);
  }

  @Override
  public void showScoreInfo() {
    emoticon.setVisible(true);
    Iterator<TranscriptSegment> scoredWords = alignmentOutput.getTypeToSegments().get(NetPronImageType.WORD_TRANSCRIPT).iterator();
    Iterator<IHighlightSegment> highlightSegment = flclickables.iterator();
    while (scoredWords.hasNext()) {
      TranscriptSegment scoredWord = scoredWords.next();
      if (highlightSegment.hasNext()) {
        IHighlightSegment next1 = highlightSegment.next();
        next1.setHighlightColor(SimpleColumnChart.getColor(scoredWord.getScore()));
        next1.showHighlight();
      }
    }
  }

  @Override
  public void clearScoreInfo() {
    emoticon.setVisible(false);
    flclickables.forEach(iHighlightSegment -> {
      iHighlightSegment.setHighlightColor(IHighlightSegment.DEFAULT_HIGHLIGHT);
      iHighlightSegment.clearHighlight();
    });
  }

  @Override
  public void addWidgets(boolean showFL, boolean showALTFL, PhonesChoices phonesChoices) {
    NoFeedbackRecordAudioPanel<T> recordPanel = new NoFeedbackRecordAudioPanel<T>(exercise, controller) {
      @Override
      public void useResult(AudioAnswer result) {
        super.useResult(result);
        alignmentOutput = result.getPretestScore();
        listenView.addScore(result.getExid(), (float) result.getScore(), RecordDialogExercisePanel.this);
        listenView.setSmiley(emoticon, result.getScore());
        // logger.info("useResult got " + result.getValidity() + " " + result.getScore());
      }
    };
    this.recordAudioPanel = recordPanel;

    recordPanel.addWidgets();

    DivWidget flContainer = getHorizDiv();
    if (isRight) {
      addStyleName("floatRight");
    } else flContainer.addStyleName("floatLeft");
//    addStyleName(isRight?"floatRight":"floatLeft");

    // add hidden button
    {
      //DivWidget recordButtonContainer = new DivWidget();
      //recordButtonContainer.addStyleName("recordingRowStyle");
      PostAudioRecordButton postAudioRecordButton = recordPanel.getPostAudioRecordButton();
      //recordButtonContainer.add(postAudioRecordButton);
      postAudioRecordButton.setVisible(false);
      flContainer.add(postAudioRecordButton);

    }

    flContainer.add(recordPanel.getScoreFeedback());
    Image w = new Image();
    emoticon = w;
    w.setVisible(false);
    w.setHeight("32px");
    w.setWidth("32px");
    flContainer.add(w);

    //long now = System.currentTimeMillis();
    //  logger.info("makeFirstRow for " + e.getID() + " took " + (now - then) + " to add rec");

    //   makePlayAudio(exercise, flContainer);

//    logger.info("adding widgets ");
//    recordAudioPanel = new NoFeedbackRecordAudioPanel<>(exercise, controller);
//
//    recordAudioPanel.addWidgets();
    //add(recordAudioPanel);

    add(flContainer);
    super.addWidgets(showFL, showALTFL, phonesChoices);
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

  long start = 0;

  /**
   * @see RehearseViewHelper#currentTurnPlayEnded
   */
  public void startRecording() {
    start = System.currentTimeMillis();
    recordAudioPanel.getPostAudioRecordButton().startOrStopRecording();
  }

  /**
   * @return
   * @see RehearseViewHelper#silenceDetected()
   */
  public boolean isRecording() {
    return recordAudioPanel.getPostAudioRecordButton().isRecording();
  }

  /**
   * @see RehearseViewHelper#silenceDetected()
   */
  public boolean stopRecording() {
    long now = System.currentTimeMillis();

    long diff = now - start;
    if (diff > minDur) {
      recordAudioPanel.getPostAudioRecordButton().startOrStopRecording();
      return true;
    } else {
      logger.info("stopRecording ignore too short " + diff + " vs " + minDur);
      return false;
    }
  }
}
