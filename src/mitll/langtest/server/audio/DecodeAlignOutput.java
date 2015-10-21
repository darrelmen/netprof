package mitll.langtest.server.audio;

import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;

import java.util.List;
import java.util.Map;

/**
 * Created by go22670 on 10/21/15.
 */
public class DecodeAlignOutput {
  private float score;
  private String json;
  private int numPhones;
  private long processDurInMillis;
  private boolean isCorrect;
  //private boolean isDecode;

  /**
   * @param alignmentScore
   */
  public DecodeAlignOutput(PretestScore alignmentScore, boolean isDecode) {
    this(alignmentScore.getHydecScore(), new ScoreToJSON().getJsonObject(alignmentScore).toString(),
        //numPhones(alignmentScore),
        alignmentScore.getProcessDur(), false, isDecode, alignmentScore);
  }

  public DecodeAlignOutput(AudioAnswer decodeAnswer, boolean isDecode) {
    this((float) decodeAnswer.getScore(),
        new ScoreToJSON().getJsonFromAnswer(decodeAnswer).toString(),
        //     numPhones(decodeAnswer.getPretestScore()),
        decodeAnswer.getPretestScore().getProcessDur(), decodeAnswer.isCorrect(), isDecode, decodeAnswer.getPretestScore());
  }

  public DecodeAlignOutput(float score, String json,
                           //int numPhones,
                           long processDurInMillis, boolean isCorrect, boolean isDecode, PretestScore pretestScore) {
    this.score = score;
    this.json = json;
    this.numPhones = numPhones(pretestScore);
    this.processDurInMillis = processDurInMillis;
    this.isCorrect = isCorrect;
    //    this.isDecode = isDecode;
  }

  private int numPhones(PretestScore pretestScore) {
    int c = 0;
    if (pretestScore != null) {
      Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap = pretestScore.getsTypeToEndTimes();
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