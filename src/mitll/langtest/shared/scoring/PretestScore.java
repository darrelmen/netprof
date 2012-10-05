/**
 * 
 */
package mitll.langtest.shared.scoring;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Map;

/**
 * @author gregbramble
 *
 */
public class PretestScore implements IsSerializable {
  public int reqid;
	private float hydecScore = -1f;
//	private float transformedHydecScore;
	private Float[] svScoreVector;
	private float transformedSVScore = -1f;
	private Map<String, Float> phoneScores;
	//private Map<NetPronImageType, Map<Float, NetPronTranscriptEvent>> transcriptEvents;
  public Map<NetPronImageType, String> sTypeToImage;

  public PretestScore(){} // required for serialization

  /**
   * @see mitll.langtest.server.scoring.DTWScoring#score
   * @param reqid
   * @param svScoreVector
   * @param sTypeToImage
   */
  public PretestScore(int reqid, Float[] svScoreVector,
                      Map<NetPronImageType, String> sTypeToImage
  ) {
    this.reqid = reqid;
    this.svScoreVector = svScoreVector;

    if (svScoreVector != null) {
      int i = svScoreVector.length - 1;
      if (i < 0) i = 0;
      transformedSVScore = svScoreVector[i];
      System.out.println("trans score is " + transformedSVScore);
      if (transformedSVScore > 1f) transformedSVScore = 1f;
    }
    else {
      System.err.println("PretestScore : no sv score vector?");
    }
    this.sTypeToImage = sTypeToImage;
  }

  public void setReqid(int r) { this.reqid = r;}

  /**
   * @see mitll.langtest.server.scoring.ASRScoring#scoreRepeatExercise(String, String, String, String, String, String, String, String, int, int)
   * @param reqid
   * @param hydecScore
   * @param svScoreVector
   * @param phoneScores
   * @param sTypeToImage
   */
  public PretestScore(int reqid, float hydecScore, Float[] svScoreVector,
                      Map<String, Float> phoneScores,
                      Map<NetPronImageType, String> sTypeToImage
  ) {
    this(reqid, svScoreVector,sTypeToImage);
    this.hydecScore = hydecScore;
    this.phoneScores = phoneScores;
	}
	
  public float getHydecScore() {
    return hydecScore;
  }
  
/*	public float getTransformedHydecScore() {
		return transformedHydecScore;
	} */
	
/*	public void setTransformedHydecScore(float score) {
	  transformedHydecScore = score;
	}*/
	
  public Float[] getSVScoreVector() {
    return svScoreVector;
  }
  
	public float getTransformedSVScore() {
		return transformedSVScore;
	}
	
	public void setTransformedSVScore(float score) {
	  transformedSVScore = score;
	}

  /**
   * TODO : remember in client
   * @return
   */
/*	public Collection<Float> getHistoricalScores() {
		return Collections.emptyList();
	}*/
	
/*  public void setHistoricalScores(ArrayList<Float> scores) {
    historicalScores = scores;
  }*/
  
	public Map<String, Float> getPhoneScores() {
		return phoneScores;
	}
	
/*	public Map<NetPronImageType, Map<Float, NetPronTranscriptEvent>> getTranscriptEvents() {
		return transcriptEvents;
	}*/

  public String toString() {
    StringBuilder b = new StringBuilder();

    if (svScoreVector != null)  {
      for (Float f : svScoreVector) b.append(f).append(",");
    }
    return "hydec " + hydecScore +
       // " transformed hydec " + transformedHydecScore +
        " transformed dtw " + transformedSVScore +
        " phones " + getPhoneScores() + " sv " +b+" type->image " +sTypeToImage;
  }
}
