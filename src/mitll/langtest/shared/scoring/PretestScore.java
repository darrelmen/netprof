/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.scoring;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.instrumentation.TranscriptSegment;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything you'd want to know about audio alignment.
 * <p>
 * Overall score, word scores, phone scores, file length, and segment offsets {@link mitll.langtest.shared.instrumentation.TranscriptSegment}
 */
public class PretestScore implements IsSerializable {
  private int reqid = 0;
  private float hydecScore = -1f;
  private Map<String, Float> phoneScores;
  private Map<String, Float> wordScores;
  private Map<NetPronImageType, String> sTypeToImage = new HashMap<NetPronImageType, String>();
  private Map<NetPronImageType, List<TranscriptSegment>> sTypeToEndTimes = new HashMap<NetPronImageType, List<TranscriptSegment>>();
  private String recoSentence;
  private float wavFileLengthSeconds;
  private int processDur = 0;
  private String json;
  private transient boolean ranNormally;

  public PretestScore() {
  } // required for serialization

  /**
   * @param score
   * @see mitll.langtest.server.scoring.ASRScoring#scoreRepeatExercise(String, String, String, String, String, int, int, boolean, boolean, String, boolean, String, Result)
   */
  public PretestScore(float score) {
    this.hydecScore = score;
    ranNormally = false;
  }

  /**
   * @param hydecScore
   * @param phoneScores
   * @param wordScores
   * @param sTypeToImage
   * @param sTypeToEndTimes
   * @param recoSentence
   * @param processDur
   * @see mitll.langtest.server.scoring.ASRScoring#getPretestScore
   * @see mitll.langtest.server.scoring.ASRWebserviceScoring#getPretestScore
   */
  public PretestScore(float hydecScore,
                      Map<String, Float> phoneScores,
                      Map<String, Float> wordScores, Map<NetPronImageType, String> sTypeToImage,
                      Map<NetPronImageType, List<TranscriptSegment>> sTypeToEndTimes,
                      String recoSentence,
                      float wavFileLengthSeconds,
                      int processDur) {
    this.sTypeToImage = sTypeToImage;
    this.hydecScore = hydecScore;
    this.phoneScores = phoneScores;
    this.wordScores = wordScores;
    this.sTypeToEndTimes = sTypeToEndTimes;
    this.recoSentence = recoSentence;
    this.wavFileLengthSeconds = wavFileLengthSeconds;
    this.processDur = processDur;
    this.ranNormally = true;
  }

  public float getHydecScore() {
    return hydecScore;
  }

  public Map<String, Float> getPhoneScores() {
    return phoneScores;
  }

  public Map<String, Float> getWordScores() {
    return wordScores;
  }

  public Map<NetPronImageType, String> getsTypeToImage() {
    return sTypeToImage;
  }

  public Map<NetPronImageType, List<TranscriptSegment>> getsTypeToEndTimes() {
    return sTypeToEndTimes;
  }

  public String getRecoSentence() {
    return recoSentence;
  }

  public float getWavFileLengthInSeconds() {
    return wavFileLengthSeconds;
  }

  public void setReqid(int r) {
    this.reqid = r;
  }

  public int getReqid() {
    return reqid;
  }

  public int getProcessDur() {
    return processDur;
  }

  public String toString() {
    return "hydec score " + hydecScore +
        " phones " + getPhoneScores() +
        " type->image " + getsTypeToImage() +
        " type->endtimes " + getsTypeToEndTimes() + " took " + processDur + " millis"
        ;
  }

  public String getJson() {
    return json;
  }

  /**
   * @see mitll.langtest.server.audio.AudioFileHelper#getASRScoreForAudio(int, String, String, Collection, int, int, boolean, boolean, String, boolean, String, Result, boolean, boolean)
   * @param json
   */
  public void setJson(String json) {
    this.json = json;
  }

  public boolean isRanNormally() {
    return ranNormally;
  }
}
