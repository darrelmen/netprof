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

  public AlignmentOutput(Map<NetPronImageType, List<TranscriptSegment>> sTypeToEndTimes) {
    this.sTypeToEndTimes = sTypeToEndTimes;
  }

  public Map<NetPronImageType, List<TranscriptSegment>> getTypeToSegments() {
    return sTypeToEndTimes;
  }
}
