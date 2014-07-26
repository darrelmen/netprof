package mitll.langtest.shared.scoring;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.shared.instrumentation.TranscriptSegment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything you'd want to know about audio alignment.
 *
 * Overall score, word scores, phone scores, file length, and segment offsets {@link mitll.langtest.shared.instrumentation.TranscriptSegment}
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

  /**
   * @see mitll.langtest.server.scoring.ASRScoring#scoreRepeatExercise(String, String, String, String, String, int, int, boolean, boolean, String, boolean, String)
   * @param score
   */
  public PretestScore(float score) { this.hydecScore = score; }

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

  public Map<NetPronImageType, List<TranscriptSegment>> getsTypeToEndTimes() {
    return sTypeToEndTimes;
  }

  public String getRecoSentence() {
    return recoSentence;
  }

  public float getWavFileLengthInSeconds() { return wavFileLengthSeconds; }

  public void setReqid(int r) { this.reqid = r;}
  public int  getReqid()      { return reqid;  }

  public String toString() {
    return "hydec score " + hydecScore +
      " phones " + getPhoneScores() +
      " type->image " + getsTypeToImage() +
      " type->endtimes " + getsTypeToEndTimes()
      ;
  }
}
