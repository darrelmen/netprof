/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

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
  private final KeyStorage storage;

  /**
   * @see StatsFlashcardFactory
   * @param storage
   */
  public StickyState(KeyStorage storage) { this.storage = storage; }

  /**
   *
   * @param e
   * @see mitll.langtest.client.flashcard.StatsFlashcardFactory#getExercisePanel(mitll.langtest.shared.CommonExercise)
   * @see mitll.langtest.client.flashcard.StatsFlashcardFactory.StatsPracticePanel#onSetComplete()
   */
  protected void storeCurrent(CommonExercise e) {
 //   System.out.println("StickyState.storeCurrent store current " + e.getID());
    storage.storeValue(CURRENT_EXERCISE, e.getID());
  }

  public String getCurrentExerciseID() {
    String value = storage.getValue(CURRENT_EXERCISE);
  //  System.out.println("StickyState.getCurrentExerciseID '" + value +"'");
    return value;
  }

  public void clearCurrent() { storage.removeValue(CURRENT_EXERCISE); }

  protected String getIncorrect() {
    return storage.getValue(INCORRECT);
  }

  protected String getCorrect() {
    return storage.getValue(CORRECT1);
  }

  protected String getScore() {
    return storage.getValue(SCORE);
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
//    System.out.println("StickyState : reset storage for "+ storage);
    storage.removeValue(CORRECT1);
    storage.removeValue(INCORRECT);
    storage.removeValue(CURRENT_EXERCISE);
    storage.removeValue(SCORE);
  }
}
