package mitll.langtest.shared.scoring;

import mitll.langtest.shared.instrumentation.TranscriptSegment;

import java.util.List;
import java.util.Map;

/**
 * @see mitll.langtest.client.scoring.ScoreFeedbackDiv#showScoreFeedback
 */
public class AlignmentAndScore extends AlignmentOutput {
  protected float hydecScore = -1f;
  private boolean fullMatch = true;

  public AlignmentAndScore() {
  }

  public AlignmentAndScore(Map<NetPronImageType, List<TranscriptSegment>> sTypeToEndTimes, float hydecScore, boolean isFullMatch) {
    super(sTypeToEndTimes);
    this.hydecScore = hydecScore;
    this.fullMatch = isFullMatch;
  }

  public float getHydecScore() {
    return hydecScore;
  }

  public boolean isFullMatch() {
    return fullMatch;
  }
}
