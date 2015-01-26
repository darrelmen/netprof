/**
 * 
 */
package mitll.langtest.server.scoring;

import java.util.Collections;
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
  public final Map<String, Map<String, Float>> eventScores ;

  public Scores() { eventScores = Collections.emptyMap(); }
  /**
   *
   * @param hydraScore
   * @param eventScores
   */
  public Scores(float hydraScore, Map<String, Map<String, Float>> eventScores) {
    this.hydraScore = hydraScore;
    this.eventScores = eventScores;
  }

  public String toString() { return "Scores score " + hydraScore + " events " + eventScores; }
}