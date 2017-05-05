/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.shared.scoring;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.scoring.AlignDecode;
import mitll.langtest.shared.instrumentation.TranscriptSegment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything you'd want to know about audio alignment.
 * <p>
 * Overall score, word scores, phone scores, file length, and segment offsets {@link mitll.langtest.shared.instrumentation.TranscriptSegment}
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since
 */
public class PretestScore extends AlignmentOutput implements IsSerializable {
  private int reqid = 0;
  private float hydecScore = -1f;
  private Map<String, Float> phoneScores;
  private Map<String, Float> wordScores;
  private Map<NetPronImageType, String> sTypeToImage = new HashMap<NetPronImageType, String>();
  private String recoSentence;
  private float wavFileLengthSeconds;
  private int processDur = 0;
  private String json;
  private transient boolean ranNormally;

  public PretestScore() {
  } // required for serialization

  /**
   * @param score
   * @seex mitll.langtest.server.scoring.ASRScoring#scoreRepeatExercise
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
   * @seex mitll.langtest.server.scoring.ASRScoring#getPretestScore
   * @see mitll.langtest.server.scoring.ASRWebserviceScoring#getPretestScore
   */
  public PretestScore(float hydecScore,
                      Map<String, Float> phoneScores,
                      Map<String, Float> wordScores,
                      Map<NetPronImageType, String> sTypeToImage,
                      Map<NetPronImageType, List<TranscriptSegment>> sTypeToEndTimes,
                      String recoSentence,
                      float wavFileLengthSeconds,
                      int processDur) {
    super(sTypeToEndTimes);
    this.sTypeToImage = sTypeToImage;
    this.hydecScore = hydecScore;
    this.phoneScores = phoneScores;
    this.wordScores = wordScores;
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
        " type->endtimes " + getTypeToSegments() + " took " + processDur + " millis"
        ;
  }

  public String getJson() {
    return json;
  }

  /**
   * @see AlignDecode#getASRScoreForAudio
   * @see AudioFileHelper#getASRScoreForAudio
   * @param json
   */
  public void setJson(String json) {
    this.json = json;
  }

  /**
   * @see AlignDecode#getASRScoreForAudio
   * @return
   */
  public boolean isRanNormally() {
    return ranNormally;
  }
}
