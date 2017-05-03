package mitll.langtest.shared.scoring;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.shared.instrumentation.TranscriptSegment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by go22670 on 4/13/17.
 */
public class AlignmentOutput implements IsSerializable {
  private Map<NetPronImageType, List<TranscriptSegment>> sTypeToEndTimes = new HashMap<NetPronImageType, List<TranscriptSegment>>();

  public AlignmentOutput() {
  }

  /**
   * @param sTypeToEndTimes
   * @see mitll.langtest.server.services.ScoringServiceImpl#getAlignments
   */
  public AlignmentOutput(Map<NetPronImageType, List<TranscriptSegment>> sTypeToEndTimes) {
    this.sTypeToEndTimes = sTypeToEndTimes;
  }

  public Map<NetPronImageType, List<TranscriptSegment>> getTypeToSegments() {
    return sTypeToEndTimes;
  }

  public String toString() {
    List<TranscriptSegment> transcriptSegments = sTypeToEndTimes.get(NetPronImageType.WORD_TRANSCRIPT);
    return transcriptSegments.isEmpty() ? " EMPTY " : transcriptSegments.toString();
  }
}
