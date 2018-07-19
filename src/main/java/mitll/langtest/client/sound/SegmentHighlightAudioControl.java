package mitll.langtest.client.sound;

import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;

import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * Created by go22670 on 4/24/17.
 */
public class SegmentHighlightAudioControl implements AudioControl {
  // private final Logger logger = Logger.getLogger("SegmentHighlightAudioControl");
  private SegmentAudioControl wordSegments, phoneSegments = null;

  /**
   * @param typeToSegmentToWidget
   * @see mitll.langtest.client.scoring.DialogExercisePanel#setPlayListener(int, long, Map, HeadlessPlayAudio)
   */
  public SegmentHighlightAudioControl(Map<NetPronImageType, TreeMap<TranscriptSegment, IHighlightSegment>> typeToSegmentToWidget) {
    wordSegments = new SegmentAudioControl(typeToSegmentToWidget.get(NetPronImageType.WORD_TRANSCRIPT));
    TreeMap<TranscriptSegment, IHighlightSegment> phones = typeToSegmentToWidget.get(NetPronImageType.PHONE_TRANSCRIPT);
    if (phones != null) {
      phoneSegments = new SegmentAudioControl(phones);
/*      logger.info("phoneSegments now has " + phones.size());
      logger.info("wordSegments  now has " + transcriptToHighlight.size());*/
    }
  }

  @Override
  public void reinitialize() {
    // logger.info("reinitialize ");
    songFinished();
  }

  @Override
  public void songFirstLoaded(double durationEstimate) {
  }

  @Override
  public void repeatSegment(float startInSeconds, float endInSeconds) {
  }

  @Override
  public void songLoaded(double duration) {
    wordSegments.songLoaded(duration);
    if (phoneSegments != null) phoneSegments.songLoaded(duration);
  }

  @Override
  public void update(double position) {
    wordSegments.update(position);
    if (phoneSegments != null) {
      phoneSegments.update(position);
    }
  }

  @Override
  public void songFinished() {
    wordSegments.songFinished();
    if (phoneSegments != null) phoneSegments.songFinished();
  }

  /**
   * @see #SegmentHighlightAudioControl
   */
  private static class SegmentAudioControl implements AudioControl {
    private final Logger logger = Logger.getLogger("SegmentAudioControl");
    private TranscriptSegment currentSegment;
    private final TreeMap<TranscriptSegment, IHighlightSegment> transcriptToHighlight;
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_DETAIL = false;

    /**
     * @param words
     * @see SegmentHighlightAudioControl#SegmentHighlightAudioControl
     */
    SegmentAudioControl(TreeMap<TranscriptSegment, IHighlightSegment> words) {
      this.transcriptToHighlight = words;
      initialize();
    }

    private void initialize() {
      if (transcriptToHighlight != null && !transcriptToHighlight.isEmpty()) {
        currentSegment = transcriptToHighlight.keySet().iterator().next();
        if (DEBUG) logger.info("initialize current word " + currentSegment);
      }
    }

    @Override
    public void reinitialize() {
      songFinished();
    }

    @Override
    public void songFirstLoaded(double durationEstimate) {
    }

    @Override
    public void repeatSegment(float startInSeconds, float endInSeconds) {
    }

    @Override
    public void songLoaded(double duration) {
      if (isHighlighted()) removeHighlight();
    }

    @Override
    public void update(double positionInSec2) {
      double posInMillis = positionInSec2 / 1000;
      //   logger.info("update " +positionInSec);
      if (currentSegment == null) {
        if (DEBUG) logger.info("no current word - update ignore : " + posInMillis);
      } else {
        if (posInMillis < currentSegment.getStart()) { // before
          if (DEBUG) logger.info("before current word - update ignore : " + posInMillis);
        } else if (posInMillis >= currentSegment.getEnd()) { // after
          if (DEBUG) logger.info(currentSegment + " remove highlight at " + posInMillis);
          removeHighlight();

          // get next segment
          while (currentSegment != null && posInMillis >= currentSegment.getEnd()) {
            currentSegment = transcriptToHighlight.higherKey(currentSegment);  // next word
          }

          if (currentSegment != null) {
            if (currentSegment.contains(posInMillis)) {
              if (DEBUG) logger.info(currentSegment + " (1) highlighted at " + posInMillis);
              showHighlight();
            }
          } else {
            if (DEBUG) logger.info("no current word " + posInMillis);
          }
          //else {
          //  isWordHighlighted = false;
          // }
        } else { // contains
          if (!isHighlighted()) {
            if (currentSegment.contains(posInMillis)) {
              if (DEBUG) logger.info(currentSegment + " (2) highlighted at " + posInMillis);
              showHighlight();
            } else {
              if (DEBUG) logger.info(currentSegment + " doesn't contain " + posInMillis);
            }
          } else {
            if (DEBUG_DETAIL) logger.info(currentSegment + " is already highlighted.");
          }
        }
      }
    }

    @Override
    public void songFinished() {
      if (DEBUG_DETAIL) logger.info("songFinished..." + transcriptToHighlight.values().size() + " segments...");
      clearHighlights();
      initialize();
    }

    private void clearHighlights() {
      transcriptToHighlight.values().forEach(IHighlightSegment::checkClearHighlight);
    }

    private boolean isHighlighted() {
      return currentSegment != null && transcriptToHighlight.get(currentSegment).isHighlighted();
    }

    /**
     * @see #update(double)
     * @see #songLoaded(double)
     */
    private void removeHighlight() {
      if (currentSegment != null) {
        transcriptToHighlight.get(currentSegment).clearHighlight();
      } else {
        logger.warning("removeHighlight no current word....");
      }
    }

    private void showHighlight() {
      transcriptToHighlight.get(currentSegment).showHighlight();
    }
  }

  public String toString() {
    return "SegmentHighlightAudioControl";
  }

}
