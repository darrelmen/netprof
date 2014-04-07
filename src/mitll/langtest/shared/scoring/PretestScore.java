package mitll.langtest.shared.scoring;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.shared.instrumentation.TranscriptSegment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class PretestScore implements IsSerializable {
  private int reqid = 0;
	private float hydecScore = -1f;
	private Map<String, Float> phoneScores;
  private Map<NetPronImageType, String> sTypeToImage = new HashMap<NetPronImageType, String>();
  private Map<NetPronImageType, List<TranscriptSegment>> sTypeToEndTimes = new HashMap<NetPronImageType, List<TranscriptSegment>>();
  private String recoSentence;
  private float wavFileLengthSeconds;

  public PretestScore(){} // required for serialization
  public PretestScore(float score) { this.hydecScore = score; }

  public void setReqid(int r) { this.reqid = r;}
  public int  getReqid()      { return reqid;  }

  /**
   * @see mitll.langtest.server.scoring.ASRScoring#scoreRepeatExercise
   * @param hydecScore
   * @param phoneScores
   * @param sTypeToImage
   * @param sTypeToEndTimes
   * @param recoSentence
   */
  public PretestScore(float hydecScore,
                      Map<String, Float> phoneScores,
                      Map<NetPronImageType, String> sTypeToImage,
                      Map<NetPronImageType, List<TranscriptSegment>> sTypeToEndTimes,
                      String recoSentence,
                      float wavFileLengthSeconds) {
    this.sTypeToImage = sTypeToImage;
    this.hydecScore = hydecScore;
    this.phoneScores = phoneScores;
    this.sTypeToEndTimes = sTypeToEndTimes;
    this.recoSentence = recoSentence;
    this.wavFileLengthSeconds = wavFileLengthSeconds;
	}
	
  public float getHydecScore() {
    return hydecScore;
  }

	public Map<String, Float> getPhoneScores() {
		return phoneScores;
	}

  public Map<NetPronImageType, String> getsTypeToImage() {
    return sTypeToImage;
  }

/*
  public void setsTypeToImage(Map<NetPronImageType, String> sTypeToImage) {
    this.sTypeToImage = sTypeToImage;
  }
*/

  public Map<NetPronImageType, List<TranscriptSegment>> getsTypeToEndTimes() {
    return sTypeToEndTimes;
  }

  public String getRecoSentence() {
    return recoSentence;
  }

  public float getWavFileLengthInSeconds() {
    return wavFileLengthSeconds;
/*    List<Float> endTimes = getsTypeToEndTimes().get(NetPronImageType.WORD_TRANSCRIPT);
    if (endTimes == null) {
      endTimes = getsTypeToEndTimes().get(NetPronImageType.PHONE_TRANSCRIPT);
    }
    if (endTimes != null && !endTimes.isEmpty()) {
      return endTimes.get(endTimes.size() - 1);
    }
    else {
      return 0f;
    }*/
  }

/*  public float getWavFileLengthSeconds() {
    return wavFileLengthSeconds;
  }*/

  public String toString() {
    return "hydec " + hydecScore +
      " phones " + getPhoneScores() +
      " type->image " + getsTypeToImage() +
      " type->endtimes " + getsTypeToEndTimes()
      ;
  }
}
