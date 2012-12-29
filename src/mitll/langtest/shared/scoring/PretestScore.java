/**
 * 
 */
package mitll.langtest.shared.scoring;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author gregbramble
 *
 */
public class PretestScore implements IsSerializable {
  private int reqid = 0;
	private float hydecScore = -1f;
	private Float[] svScoreVector;
	private float transformedSVScore = -1f;
	private Map<String, Float> phoneScores;
  private Map<NetPronImageType, String> sTypeToImage = new HashMap<NetPronImageType, String>();
  private Map<NetPronImageType, List<Float>> sTypeToEndTimes = new HashMap<NetPronImageType, List<Float>>();
  private String recoSentence;

  public PretestScore(){} // required for serialization

  /**
   * @see mitll.langtest.server.scoring.DTWScoring#score
   * @paramx reqid
   * @param svScoreVector
   * @param sTypeToImage
   */
  public PretestScore(Float[] svScoreVector,
                      Map<NetPronImageType, String> sTypeToImage
  ) {
    this.svScoreVector = svScoreVector;

    if (svScoreVector != null && svScoreVector.length > 0) {
      int i = svScoreVector.length - 1;
      if (i < 0) i = 0;
      transformedSVScore = svScoreVector[i];
      if (transformedSVScore > 1f) transformedSVScore = 1f;
    }
    else {
     // System.err.println("PretestScore : no sv score vector?");
    }
    this.setsTypeToImage(sTypeToImage);
  }

  public void setReqid(int r) { this.reqid = r;}
  public int getReqid() {  return reqid;  }

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
                      Map<NetPronImageType, List<Float>> sTypeToEndTimes,
                      String recoSentence) {
    this(new Float[]{},sTypeToImage);
    this.hydecScore = hydecScore;
    this.phoneScores = phoneScores;
    this.sTypeToEndTimes = sTypeToEndTimes;
    this.recoSentence = recoSentence;
	}
	
  public float getHydecScore() {
    return hydecScore;
  }

	public float getTransformedSVScore() {
		return transformedSVScore;
	}
	
	public Map<String, Float> getPhoneScores() {
		return phoneScores;
	}

  public Map<NetPronImageType, String> getsTypeToImage() {
    return sTypeToImage;
  }

  public void setsTypeToImage(Map<NetPronImageType, String> sTypeToImage) {
    this.sTypeToImage = sTypeToImage;
  }

  public Map<NetPronImageType, List<Float>> getsTypeToEndTimes() {
    return sTypeToEndTimes;
  }

  public String getRecoSentence() {
    return recoSentence;
  }

  public float getWavFileLengthInSeconds() {
    List<Float> endTimes = getsTypeToEndTimes().get(NetPronImageType.WORD_TRANSCRIPT);
    if (endTimes == null) {
      endTimes = getsTypeToEndTimes().get(NetPronImageType.PHONE_TRANSCRIPT);
    }
    if (endTimes != null && !endTimes.isEmpty()) {
      return endTimes.get(endTimes.size() - 1);
    }
    else {
      return 0f;
    }
  }

  public String toString() {
    StringBuilder b = new StringBuilder();

    if (svScoreVector != null)  {
      for (Float f : svScoreVector) b.append(f).append(",");
    }
    return "hydec " + hydecScore +
        " transformed dtw " + transformedSVScore +
        " phones " + getPhoneScores() + " sv " +b+
        " type->image " + getsTypeToImage() +
        " type->endtimes " + getsTypeToEndTimes()
        ;
  }
}
