/**
 * 
 */
package mitll.langtest.shared.scoring;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author gregbramble
 *
 */
public class PretestScore implements IsSerializable {
	//private static final long serialVersionUID = 4879486889011650301L;
	private float hydecScore;
	private float transformedHydecScore;
	private Float[] svScoreVector;
	private float transformedSVScore;
	//private Collection<Float> historicalScores;
	private Map<String, Float> phoneScores;
	//private Map<NetPronImageType, Map<Float, NetPronTranscriptEvent>> transcriptEvents;
  public Map<NetPronImageType, String> sTypeToImage;

  public PretestScore(){} // required for serialization
	
	public PretestScore(float hydecScore, Float[] svScoreVector,
                      //ArrayList<Float> historicalScores,
                      Map<String, Float> phoneScores,
                      Map<NetPronImageType, String> sTypeToImage
                    //  Map<NetPronImageType, Map<Float, NetPronTranscriptEvent>> transcriptEvents
  ) {
		this.hydecScore = hydecScore;
		this.svScoreVector = svScoreVector;
		//this.historicalScores = historicalScores;
		this.phoneScores = phoneScores;
	//	this.transcriptEvents = transcriptEvents;
    this.sTypeToImage = sTypeToImage;
	}
	
  public float getHydecScore() {
    return hydecScore;
  }
  
	public float getTransformedHydecScore() {
		return transformedHydecScore;
	} 
	
	public void setTransformedHydecScore(float score) {
	  transformedHydecScore = score;
	}
	
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
	public Collection<Float> getHistoricalScores() {
		return Collections.emptyList();
	}
	
/*  public void setHistoricalScores(ArrayList<Float> scores) {
    historicalScores = scores;
  }*/
  
	public Map<String, Float> getPhoneScores() {
		return phoneScores;
	}
	
/*	public Map<NetPronImageType, Map<Float, NetPronTranscriptEvent>> getTranscriptEvents() {
		return transcriptEvents;
	}*/

  public String toString() { return "hydec " + hydecScore + " transformed " + transformedHydecScore + " phones " + getPhoneScores() + " type->image " +sTypeToImage; }
}
