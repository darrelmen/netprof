/**
 * 
 */
package mitll.langtest.server.scoring;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Scores is a simple holder for the combination of scores returned when scoring an utterance.
 * Depending on the function passed in to Audio.sv and Audio.multisv,
 * these scores might just be raw or might be transformed to be in the range 0.0-1.0.
 * Some values may be null if they have not been computed.
 * @author dhalbert
 *
 */
public class Scores {
  public float hydraScore;
  public final Map<String, Map<String, Float>> eventScores;

  public Scores() { eventScores = Collections.emptyMap(); }
  /**
   *
   * @param hydraScore
   * @param eventScores
   */
  public Scores(float hydecScore, Map<String, Map<String, Float>> eventScores) {
    this.hydraScore = hydecScore;
    this.eventScores = eventScores;
  }
  
  /*public Scores(String scoreStr) {
	this.eventScores = new HashMap<String, Map<String, Float>>();
	String[] split = scoreStr.split(";");
	this.hydraScore = Float.parseFloat(split[0]);
	eventScores.put("phones", new HashMap<String, Float>());
	for(int i = 1; i < split.length; i+=2) {
		eventScores.get("phones").put(split[i], Float.parseFloat(split[i+1]));
	}
  }*/
  
  public Scores(String[] scoreSplit) {
	this.eventScores = new HashMap<String, Map<String, Float>>();
	//String[] split = scoreStr.split(";");
	float s = Float.parseFloat(scoreSplit[0]);
	this.hydraScore = Float.isNaN(s) ? 0.0f : s;//Float.parseFloat(scoreSplit[0]);
	eventScores.put("phones", new HashMap<String, Float>());
	for(int i = 1; i < scoreSplit.length; i+=2) {
		eventScores.get("phones").put(scoreSplit[i], Float.parseFloat(scoreSplit[i+1]));
	}
  }

  public String toString() { return "Scores score " + hydraScore + " events " + eventScores; }
}