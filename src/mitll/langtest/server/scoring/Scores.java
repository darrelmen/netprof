/**
 * 
 */
package mitll.langtest.server.scoring;

import mitll.langtest.shared.Result;
import pronz.speech.Audio;

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
	private static final String PHONES = "phones";
	public float hydraScore;
  public final Map<String, Map<String, Float>> eventScores;
	private int processDur = 0;

	public Scores(int processDur) {
		eventScores = Collections.emptyMap();
		this.processDur = processDur;
	}

	/**
   *
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

	public Scores(String[] scoreSplit) {
		float s = Float.parseFloat(scoreSplit[0]);
		this.hydraScore = Float.isNaN(s) ? 0.0f : s;
		this.eventScores = new HashMap<String, Map<String, Float>>();
		eventScores.put(PHONES, new HashMap<String, Float>());
		for (int i = 1; i < scoreSplit.length; i += 2) {
			eventScores.get(PHONES).put(scoreSplit[i], Float.parseFloat(scoreSplit[i + 1]));
		}
	}

	public int getProcessDur() {
		return processDur;
	}

	public String toString() {
		return "Scores score " + hydraScore + " events " + eventScores + " took " + processDur + " millis";
	}
}