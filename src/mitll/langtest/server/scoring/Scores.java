/**
 * 
 */
package mitll.langtest.server.scoring;

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
  public Float hydecScore;
  public Map<String, Map<String, Float>> eventScores ;

  /**
   *
   * @param hydecScore
   * @param eventScores
   */
  public Scores(Float hydecScore, Map<String, Map<String, Float>> eventScores) {
    this.hydecScore = hydecScore;
    this.eventScores = eventScores;
  }
}