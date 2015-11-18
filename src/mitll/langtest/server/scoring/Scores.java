/**
 *
 */
package mitll.langtest.server.scoring;

import mitll.langtest.shared.Result;
import org.apache.log4j.Logger;
import pronz.speech.Audio;

import java.util.*;

/**
 * Scores is a simple holder for the combination of scores returned when scoring an utterance.
 * Depending on the function passed in to Audio.sv and Audio.multisv,
 * these scores might just be raw or might be transformed to be in the range 0.0-1.0.
 * Some values may be null if they have not been computed.
 *
 * @author dhalbert
 */
public class Scores {
  private static final Logger logger = Logger.getLogger(Scores.class);

  public static final String PHONES = "phones";
  public static final String WORDS = "words";
  public float hydraScore = 0f;
  public final Map<String, Map<String, Float>> eventScores;
  private int processDur = 0;

  public Scores() {
    eventScores = Collections.emptyMap();
  }

  public Scores(int processDur) {
    eventScores = Collections.emptyMap();
    this.processDur = processDur;
  }

  /**
   * @param hydecScore
   * @param eventScores
   * @param processDur
   * @see ASRScoring#getEmptyScores()
   * @see ASRScoring#getScoresFromHydec(Audio, String, String)
   * @see ASRScoring#scoreRepeatExercise(String, String, String, String, String, int, int, boolean, boolean, String, boolean, String, Result)
   */
  public Scores(float hydecScore, Map<String, Map<String, Float>> eventScores, int processDur) {
    this.hydraScore = hydecScore;
    this.eventScores = eventScores;
    this.processDur = processDur;
  }

  /**
   * TODO : do we need word scores?
   *
   * @param scoreSplit
   * @see ASRWebserviceScoring#runHydra(String, String, Collection, String, boolean, int)
   */
  public Scores(String[] scoreSplit) {
    this.eventScores = new HashMap<String, Map<String, Float>>();
    eventScores.put(PHONES, new HashMap<String, Float>());

    try {
      float s = Float.parseFloat(scoreSplit[0]);
      this.hydraScore = Float.isNaN(s) ? 0.0f : s;
      for (int i = 1; i < scoreSplit.length; i += 2) {
        eventScores.get(PHONES).put(scoreSplit[i], Float.parseFloat(scoreSplit[i + 1]));
      }
    } catch (NumberFormatException e) {
      logger.error("Parsing " + Arrays.asList(scoreSplit) + " Got " + e, e);
    }
  }

  public int getProcessDur() {
    return processDur;
  }

  public String toString() {
    return "Scores score " + hydraScore + " events " + eventScores + " took " + processDur + " millis";
  }
}