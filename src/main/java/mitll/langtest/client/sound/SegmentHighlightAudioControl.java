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

  /**
   * @param typeToSegmentToWidget
   * @see mitll.langtest.client.scoring.SimpleRecordAudioPanel#getWordTableContainer
   */
  public SegmentHighlightAudioControl(Map<NetPronImageType,
      TreeMap<TranscriptSegment, IHighlightSegment>> typeToSegmentToWidget) {
    wordSegments = new SegmentAudioControl(typeToSegmentToWidget.get(NetPronImageType.WORD_TRANSCRIPT));
    TreeMap<TranscriptSegment, IHighlightSegment> words =
        typeToSegmentToWidget.get(NetPronImageType.PHONE_TRANSCRIPT);
    if (words != null) {
      phoneSegments = new SegmentAudioControl(words);
    }
  }

  public String toString() {
    return "SegmentHighlightAudioControl";
  }

  @Override
  public void reinitialize() {
   // logger.info("reinitialize ");
    //    initialize();
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
    if (phoneSegments != null) phoneSegments.update(position);
  }

  @Override
  public void songFinished() {
    wordSegments.songFinished();
    if (phoneSegments != null) phoneSegments.songFinished();
  }

  public static class SegmentAudioControl implements AudioControl {
    private final Logger logger = Logger.getLogger("SegmentAudioControl");
    private TranscriptSegment currentWord;
    private final TreeMap<TranscriptSegment, IHighlightSegment> words;
    //private boolean isWordHighlighted = false;
    private static boolean DEBUG = false;

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
        currentWord = words.keySet().iterator().next();
        if (DEBUG) logger.info("initialize current word " + currentWord);
      }
    }

    @Override
    public void reinitialize() {
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
      if (currentWord == null) {
        if (DEBUG) logger.info("no current word - update ignore : " + posInMillis);
      } else {
        if (posInMillis < currentWord.getStart()) { // before
          if (DEBUG) logger.info("before current word - update ignore : " + posInMillis);
        } else if (posInMillis >= currentWord.getEnd()) { // after
          removeHighlight();
          currentWord = words.higherKey(currentWord);  // next word

          if (currentWord != null) {
            if (currentWord.contains(posInMillis)) {
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
            if (currentWord.contains(posInMillis)) {
              showHighlight();
            } else {
              if (DEBUG) logger.info(currentWord + " doesn't contain " + posInMillis);
            }
          } else {
            if (DEBUG) logger.info(currentWord + " is already highlighted.");
          }
        }
      }
    }

    @Override
    public void songFinished() {
      if (DEBUG) logger.info("songFinished..." + words.values().size() + " segments...");
//      if (isHighlighted()) {
//        removeHighlight();
//      }
//      else {
      for (IHighlightSegment segment : words.values()) {
        if (segment.isHighlighted()) {
          logger.info("segment "+ segment + " highlighted");
          segment.clearBlue();
        }
      }
      // if (DEBUG) logger.info("nothing highlighted...");
//      }
      initialize();
    }

    private boolean isHighlighted() {
      return currentWord != null && words.get(currentWord).isHighlighted();
    }

    private void removeHighlight() {
      if (currentWord != null) {
        words.get(currentWord).clearBlue();
//        widget.getElement().getStyle().setBackgroundColor(backgroundColor);
      } else {
        logger.warning("no current word....");
      }
    }

    private void showHighlight() {
      words.get(currentWord).setBlue();
    }
  }
}
