package mitll.langtest.client.flashcard;

import mitll.langtest.client.custom.KeyStorage;
import mitll.langtest.shared.CommonExercise;

/**
 * Remember the state of the flashcards in the localStorage browser cache.
 * Created by go22670 on 7/8/14.
 */
public class StickyState {
  private static final String INCORRECT = "Incorrect";
  private static final String SCORE = "Score";

  private static final String CURRENT_EXERCISE = "currentExercise";
  private static final String CORRECT1 = "correct";
  private static final String SKIPPED = "skipped";
  private final KeyStorage storage;

  /**
   * @see mitll.langtest.client.flashcard.MyFlashcardExercisePanelFactory
   * @param storage
   */
  public StickyState(KeyStorage storage) { this.storage = storage; }
  protected void storeCurrent(CommonExercise e) {
   // System.out.println("store current " + e.getID());


    storage.storeValue(CURRENT_EXERCISE, e.getID());
  }
  public String getCurrentExerciseID() {

    String value = storage.getValue(CURRENT_EXERCISE);
   // System.out.println("getCurrentExerciseID " + value);
    return value;
  }

  protected String getIncorrect() {
    return storage.getValue(INCORRECT);
  }

  protected String getCorrect() {
    return storage.getValue(CORRECT1);
  }

  protected String getSkipped() {
    return storage.getValue(SKIPPED);
  }

  protected String getScore() {
    return storage.getValue(SCORE);
  }

  protected void storeSkipped(StringBuilder builder) {
    storage.storeValue(SKIPPED, builder.toString());
  }

  protected void storeScore(StringBuilder builder3) {
    storage.storeValue(SCORE, builder3.toString());
  }

  protected void storeIncorrect(StringBuilder builder2) {
    storage.storeValue(INCORRECT, builder2.toString());
  }

  protected void storeCorrect(StringBuilder builder) {
    storage.storeValue(CORRECT1, builder.toString());
  }

  public void resetStorage() {
   // System.out.println("reset storage for "+ storage);

    storage.removeValue(CORRECT1);
    storage.removeValue(INCORRECT);
    storage.removeValue(CURRENT_EXERCISE);
    storage.removeValue(SCORE);
    storage.removeValue(SKIPPED);
  }
}
