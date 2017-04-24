package mitll.langtest.client.sound;

import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.sound.AudioControl;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;

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
  public SegmentHighlightAudioControl(Map<NetPronImageType, TreeMap<TranscriptSegment, Widget>> typeToSegmentToWidget) {
    wordSegments = new SegmentAudioControl(typeToSegmentToWidget.get(NetPronImageType.WORD_TRANSCRIPT));
    TreeMap<TranscriptSegment, Widget> words = typeToSegmentToWidget.get(NetPronImageType.PHONE_TRANSCRIPT);
    if (words != null) {
      phoneSegments = new SegmentAudioControl(words);
    }
  }

  public String toString() {
    return "SegmentHighlightAudioControl";
  }

  @Override
  public void reinitialize() {
    logger.info("reinitialize ");
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
    private TranscriptSegment currentWord;
    private final TreeMap<TranscriptSegment, Widget> words;
    private boolean isWordHighlighted = false;

    public SegmentAudioControl(TreeMap<TranscriptSegment, Widget> words) {
      this.words = words;
      if (words != null && !words.isEmpty()) {
        currentWord = words.keySet().iterator().next();
      }
    }

    private void initialize() {
      if (words != null && !words.isEmpty()) {
        currentWord = words.keySet().iterator().next();
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
      if (isWordHighlighted) removeHighlight();
    }

    @Override
    public void update(double position) {
      position /= 1000;
      //   logger.info("update " +position);
      if (currentWord == null) {
        //    logger.info("no current word - update ignore : " +position);
      } else {
        if (position < currentWord.getStart()) { // before
          //    logger.info("before current word - update ignore : " +position);
        } else if (position >= currentWord.getEnd()) { // after
          removeHighlight();
          currentWord = words.higherKey(currentWord);  // next word

          if (currentWord != null) {
            if (isWordHighlighted = currentWord.contains(position)) {
              showHighlight();
            }
          } else isWordHighlighted = false;
        } else { // contains
          if (!isWordHighlighted) {
            if (isWordHighlighted = currentWord.contains(position)) {
              showHighlight();
            }
          }
        }
      }
    }

    @Override
    public void songFinished() {
      if (isWordHighlighted) removeHighlight();
      initialize();
    }

    private void removeHighlight() {
      if (currentWord != null) {
        Widget widget = words.get(currentWord);
        widget.getElement().getStyle().setBackgroundColor(backgroundColor);
      }
    }

    private String backgroundColor;

    private void showHighlight() {
      Widget nwidget = words.get(currentWord);
      backgroundColor = nwidget.getElement().getStyle().getBackgroundColor();
      nwidget.getElement().getStyle().setBackgroundColor("#2196F3");
    }
  }
}
