/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.analysis;

import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Created by go22670 on 10/21/15.
 *
 * @see mitll.langtest.client.analysis.WordContainer#getTableWithPager(List)
 */
public class WordScore implements Serializable, Comparable<WordScore> {
  private String fileRef;
  private String nativeAudio;
  private String id;
  private long timestamp;
  private float pronScore;
  private int resultID;
  private Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap;

  public WordScore() {
  }

  /**
   * @param bs
   * @param netPronImageTypeListMap
   * @see mitll.langtest.server.database.analysis.Analysis#getWordScore(List)
   */
  public WordScore(BestScore bs, Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap) {
    this(bs.getExId(), bs.getScore(), bs.getTimestamp(), bs.getResultID(), bs.getFileRef(), bs.getNativeAudio(), netPronImageTypeListMap);
  }

  private WordScore(String id, float pronScore, long timestamp, int resultID, String fileRef, String nativeAudio,
                    Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap) {
    this.id = id;
    this.pronScore = (pronScore < 0) ? 0 : pronScore;
    this.timestamp = timestamp;
    this.resultID = resultID;
    this.fileRef = fileRef;
    this.nativeAudio = nativeAudio;
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

  public Map<NetPronImageType, List<TranscriptSegment>> getNetPronImageTypeListMap() {
    return netPronImageTypeListMap;
  }

  public String getFileRef() {
    return fileRef;
  }


  public String getNativeAudio() {
    return nativeAudio;
  }

  public String toString() {
    return "exid " + id + "/" + resultID + " score " + pronScore + "  : " + netPronImageTypeListMap;
  }
}
