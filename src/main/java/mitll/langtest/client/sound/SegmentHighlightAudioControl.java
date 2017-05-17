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
  private final Logger logger = Logger.getLogger("SegmentHighlightAudioControl");
  private SegmentAudioControl wordSegments, phoneSegments = null;

  public SegmentHighlightAudioControl(Map<NetPronImageType, TreeMap<TranscriptSegment, IHighlightSegment>> typeToSegmentToWidget) {
    setTypeToSegments(typeToSegmentToWidget);
  }

  private void setTypeToSegments(Map<NetPronImageType, TreeMap<TranscriptSegment, IHighlightSegment>> typeToSegmentToWidget) {
    TreeMap<TranscriptSegment, IHighlightSegment> words = typeToSegmentToWidget.get(NetPronImageType.WORD_TRANSCRIPT);
    wordSegments = new SegmentAudioControl(words);
    TreeMap<TranscriptSegment, IHighlightSegment> phones = typeToSegmentToWidget.get(NetPronImageType.PHONE_TRANSCRIPT);
    if (phones != null) {
      phoneSegments = new SegmentAudioControl(phones);
/*      logger.info("phoneSegments now has " + phones.size());
      logger.info("wordSegments  now has " + words.size());*/
    }
  }

  public String toString() {
    return "SegmentHighlightAudioControl";
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

  public static class SegmentAudioControl implements AudioControl {
    private final Logger logger = Logger.getLogger("SegmentAudioControl");
    private TranscriptSegment currentSegment;
    private final TreeMap<TranscriptSegment, IHighlightSegment> words;
    private static boolean DEBUG = false;
    private static boolean DEBUG_DETAIL = false;
    /**
     * @param words
     * @see SegmentHighlightAudioControl#SegmentHighlightAudioControl
     */
    SegmentAudioControl(TreeMap<TranscriptSegment, IHighlightSegment> words) {
      this.words = words;
      initialize();
    }

    private void initialize() {
      if (words != null && !words.isEmpty()) {
        currentSegment = words.keySet().iterator().next();
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
          if (DEBUG) logger.info( currentSegment + " remove highlight at " +posInMillis);
          removeHighlight();

          while (currentSegment != null && posInMillis >= currentSegment.getEnd()) {
            currentSegment = words.higherKey(currentSegment);  // next word
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
      if (DEBUG_DETAIL) logger.info("songFinished..." + words.values().size() + " segments...");
      for (IHighlightSegment segment : words.values()) {
        if (segment.isHighlighted()) {
          segment.clearBlue();
        }
      }
      initialize();
    }

    private boolean isHighlighted() {
      return currentSegment != null && words.get(currentSegment).isHighlighted();
    }

    /**
     * @see #update(double)
     * @see #songLoaded(double)
     */
    private void removeHighlight() {
      if (currentSegment != null) {
        IHighlightSegment segment = words.get(currentSegment);
        segment.clearBlue();
      } else {
        logger.warning("removeHighlight no current word....");
      }
    }

    private void showHighlight() {
      words.get(currentSegment).setBlue();
    }
  }
}
