/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.shared.scoring;

import mitll.langtest.client.scoring.AudioPanel;
import mitll.langtest.server.scoring.ASR;
import mitll.langtest.server.scoring.AlignDecode;
import mitll.langtest.shared.instrumentation.TranscriptSegment;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PretestScore extends AlignmentAndScore {
  private static final String SIL = "sil";
  private static final String SIL1 = "SIL";
  private static final String UNKNOWN_MODEL = "UNKNOWNMODEL";

  private int reqid = 0;
  private Map<String, Float> phoneScores;
  private Map<String, Float> wordScores;
  private Map<NetPronImageType, String> sTypeToImage = new HashMap<>();

  private transient String recoSentence = "";

  private float wavFileLengthSeconds;

  private transient int processDur = 0;

  private String json;
  private transient boolean ranNormally;
  private String status = "";
  private String message = "";
  private float rawOverallScore;

  public PretestScore() {
  } // required for serialization

  /**
   * @param score
   * @see mitll.langtest.server.scoring.ASRWebserviceScoring#scoreRepeatExercise
   */
  public PretestScore(float score) {
    this.overallScore = score;
    ranNormally = false;
  }

  /**
   * @param overallScore
   * @param phoneScores
   * @param wordScores
   * @param sTypeToImage
   * @param sTypeToEndTimes
   * @param recoSentence
   * @param processDur
   * @param isFullMatch
   * @see mitll.langtest.server.scoring.ASRWebserviceScoring#getPretestScore
   */
  public PretestScore(float overallScore,
                      Map<String, Float> phoneScores,
                      Map<String, Float> wordScores,
                      Map<NetPronImageType, String> sTypeToImage,
                      Map<NetPronImageType, List<TranscriptSegment>> sTypeToEndTimes,
                      String recoSentence,
                      float wavFileLengthSeconds,
                      int processDur,
                      boolean isFullMatch) {
    super(sTypeToEndTimes, overallScore, isFullMatch);
    this.sTypeToImage = sTypeToImage;
    this.phoneScores = phoneScores;
    this.wordScores = wordScores;
    this.recoSentence = recoSentence;
    this.wavFileLengthSeconds = wavFileLengthSeconds;
    this.processDur = processDur;
    this.ranNormally = true;
  }

  public AlignmentAndScore getSkinny() {
    return new AlignmentAndScore(getTypeToSegments(), getOverallScore(), isFullMatch());
  }

  /**
   * @return
   * @see mitll.langtest.client.scoring.ReviewScoringPanel#scoreAudio(String, int, String, String, AudioPanel.ImageAndCheck, AudioPanel.ImageAndCheck, int, int, int)
   */
  public Map<String, Float> getPhoneScores() {
    return phoneScores;
  }

  /**
   * @return
   * @see mitll.langtest.client.scoring.ReviewScoringPanel#scoreAudio(String, int, String, String, AudioPanel.ImageAndCheck, AudioPanel.ImageAndCheck, int, int, int)
   * @see mitll.langtest.client.scoring.ReviewScoringPanel#addWordScoreTable(PretestScore)
   */
  public Map<String, Float> getWordScores() {
    return wordScores;
  }

  /**
   * Arithmetic average!
   *
   * @return
   */
  public float getAvgWordScore() {
    return wordScores == null || wordScores.isEmpty() ? getSegmentAverage(getWordSegments()) : getAverage(wordScores.values());
  }

  public List<TranscriptSegment> getWordSegments() {
    return getTypeToSegments().get(NetPronImageType.WORD_TRANSCRIPT);
  }

  private float getAverage(Collection<Float> values) {
    float total = 0F;
    for (float vv : values) total += vv;
    return values.isEmpty() ? 0F : total / Integer.valueOf(values.size()).floatValue();
  }

  public float getAvgPhoneScore() {
    return getSegmentAverage(getTypeToSegments().get(NetPronImageType.PHONE_TRANSCRIPT));
  }

  private float getSegmentAverage(List<TranscriptSegment> transcriptSegments) {
    if (transcriptSegments == null || transcriptSegments.isEmpty()) {
      return 0F;
    } else {
      float numValidSegments = getNumValidSegments(transcriptSegments);
      if (numValidSegments == 0) {
        return 0F;
      } else {
        return getValidSegmentScoreTotal(transcriptSegments) / numValidSegments;
      }
    }
  }

  public float getValidSegmentScoreTotal(List<TranscriptSegment> transcriptSegments) {
    float total = 0F;
    for (TranscriptSegment seg : transcriptSegments) {
      if (isValidEvent(seg.getEvent())) {
        total += seg.getScore();
      }
    }
    return total;
  }

  public float getNumValidSegments(List<TranscriptSegment> transcriptSegments) {
    float total = 0F;
    for (TranscriptSegment seg : transcriptSegments) {
      if (isValidEvent(seg.getEvent())) {
        total += 1F;
      }
    }
    return total;
  }

  private boolean isValidEvent(String event) {
    return !event.equals(UNKNOWN_MODEL) && !event.equals(SIL) && !event.equals(SIL1);
  }

  public Map<NetPronImageType, String> getsTypeToImage() {
    return sTypeToImage;
  }

  public String getRecoSentence() {
    return recoSentence;
  }

  /**
   * @return
   * @see mitll.langtest.client.scoring.ClickableTranscript#getClickedOnSegment
   */
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

  public String getJson() {
    return json;
  }

  /**
   * @param json
   * @see AlignDecode#getASRScoreForAudio
   * @see AlignDecode#getASRScoreForAudio
   */
  public void setJson(String json) {
    this.json = json;
  }

  /**
   * @return
   * @see mitll.langtest.server.autocrt.AutoCRT#markCorrectnessOnAnswer
   */
  public boolean isRanNormally() {
    return ranNormally;
  }

  public String getStatus() {
    return status;
  }

  /**
   * @param status
   * @return
   */
  public PretestScore setStatus(String status) {
    this.status = status;
    return this;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public float getRawOverallScore() {
    return rawOverallScore;
  }

  public void setRawOverallScore(float rawOverallScore) {
    this.rawOverallScore = rawOverallScore;
  }

  public String toString() {
    String ss = status.isEmpty() ? "" : "\n\tstatus         " + status;
    String ms = message.isEmpty() ? "" : "\n\tmessage        " + message;
    String ts = getsTypeToImage().isEmpty() ? "" : "\n\ttype->image    " + getsTypeToImage();
    String fm = isFullMatch() ? "" : "\n\tfull match     " + isFullMatch();

    return "score" +
        ss +
        ms +
        "\n\tscore          " + getOverallScore() +
        "\n\traw overall    " + getRawOverallScore() +
        "\n\tword avg       " + getAvgWordScore() +
        "\n\tphone avg      " + getAvgPhoneScore() +
        "\n\tphones         " + getPhoneScores() +
        ts +
        "\n\ttype->endtimes " + getTypeToSegments() +
        "\n\ttook           " + processDur + " millis" +
        fm
        ;
  }
}
