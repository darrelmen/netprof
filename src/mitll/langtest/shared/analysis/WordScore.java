package mitll.langtest.shared.analysis;

import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Created by go22670 on 10/21/15.
 * @see mitll.langtest.client.analysis.WordContainer#getTableWithPager(List)
 */
public class WordScore implements Serializable, Comparable<WordScore>{
  private String fileRef;
  private String id;
  private long timestamp;
  private float pronScore;
  private int resultID;
  private Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap;

  public WordScore() {
  }

  /**
   * @see mitll.langtest.server.database.analysis.Analysis#getWordScore(List)
   * @param bs
   * @param netPronImageTypeListMap
   */
  public WordScore(BestScore bs, Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap) {
    this(bs.getId(), bs.getScore(), bs.getTimestamp(), bs.getResultID(), bs.getFileRef(), netPronImageTypeListMap);
  }

  private WordScore(String id, float pronScore, long timestamp, int resultID, String fileRef, Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap) {
    this.id = id;
    this.pronScore = (pronScore < 0) ? 0 : pronScore;
    this.timestamp = timestamp;
    this.resultID = resultID;
    this.fileRef = fileRef;
    this.netPronImageTypeListMap = netPronImageTypeListMap;
  }

  @Override
  public int compareTo(WordScore o) {
    int i = Float.valueOf(getPronScore()).compareTo(o.getPronScore());
    if (i == 0) {
      i = Long.valueOf(timestamp).compareTo(o.timestamp);
    }
    return i;
  }

  public String getId() {
    return id;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public float getPronScore() {
    return pronScore;
  }
/*
  public int getResultID() {
    return resultID;
  }*/

  public Map<NetPronImageType, List<TranscriptSegment>> getNetPronImageTypeListMap() {
    return netPronImageTypeListMap;
  }

  public String toString() { return id +"/" +resultID + " score " + pronScore+
      "  : " + netPronImageTypeListMap; }

  public String getFileRef() {
    return fileRef;
  }
}
