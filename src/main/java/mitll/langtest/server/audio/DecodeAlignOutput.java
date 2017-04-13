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

package mitll.langtest.server.audio;

import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;

import java.util.List;
import java.util.Map;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/21/15.
 */
public class DecodeAlignOutput {
  private float score;
  private String json;
  private int numPhones;
  private long processDurInMillis;
  private boolean isCorrect;

  /**
   * @see AudioFileHelper#decodeOneAttribute
   * @param alignmentScore
   */
  DecodeAlignOutput(PretestScore alignmentScore, boolean isDecode) {
    this(
        alignmentScore.getHydecScore(),
        new ScoreToJSON().getJsonObject(alignmentScore).toString(),
        alignmentScore.getProcessDur(),
        false,
        alignmentScore);
  }

//  private static String getJson(PretestScore alignmentScore) {
//    return new ScoreToJSON().getJsonObject(alignmentScore).toString();
//  }

  DecodeAlignOutput(AudioAnswer decodeAnswer, boolean isDecode) {
    PretestScore pretestScore = decodeAnswer.getPretestScore();
    this.score = (float) decodeAnswer.getScore();
    this.json = new ScoreToJSON().getJsonFromAnswer(decodeAnswer).toString();
    this.numPhones = pretestScore == null ? 0 : numPhones(pretestScore);
    this.processDurInMillis = pretestScore == null ? 0 : decodeAnswer.getPretestScore().getProcessDur();
    this.isCorrect = decodeAnswer.isCorrect();
  }

  DecodeAlignOutput(float score,
                    String json,
                    long processDurInMillis,
                    boolean isCorrect,
                    PretestScore pretestScore) {
    //this(score,json,numPhones(pretestScore),processDurInMillis,isCorrect);
    this.score = score;
    this.json = json;
    this.numPhones = numPhones(pretestScore);
    this.processDurInMillis = processDurInMillis;
    this.isCorrect = isCorrect;
  }

  public DecodeAlignOutput(float score, String json,
                    long processDurInMillis, boolean isCorrect,
                    int numPhones) {
    this.score = score;
    this.json = json;
    this.numPhones = numPhones;
    this.processDurInMillis = processDurInMillis;
    this.isCorrect = isCorrect;
  }

  private int numPhones(PretestScore pretestScore) {
    int c = 0;
    if (pretestScore != null) {
      Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap = pretestScore.getTypeToSegments();
      List<TranscriptSegment> phones = netPronImageTypeListMap.get(NetPronImageType.PHONE_TRANSCRIPT);

      if (phones != null) {
        for (TranscriptSegment pseg : phones) {
          String pevent = pseg.getEvent();
          if (!pevent.equals(SLFFile.UNKNOWN_MODEL) && !pevent.equals("sil")) {
            c++;
          }
        }
      }
    }
    return c;
  }

  public float getScore() {
    return score;
  }

  public String getJson() {
    return json;
  }

  public int getNumPhones() {
    return numPhones;
  }

  @Override
  public String toString() {
    return "Took " + processDurInMillis + " to score " + score + " # phones " + numPhones;
  }

  public int getProcessDurInMillis() {
    return (int) processDurInMillis;
  }

  public boolean isCorrect() {
    return isCorrect;
  }
 /*   public boolean isDecode() {
      return isDecode;
    }*/
}