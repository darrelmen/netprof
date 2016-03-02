/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.flashcard;

import mitll.langtest.client.custom.KeyStorage;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.Shell;

/**
 * Remember the state of the flashcards in the localStorage browser cache.
 * Created by go22670 on 7/8/14.
 */
class StickyState {
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
   * @see ExercisePanelFactory#getExercisePanel(mitll.langtest.shared.exercise.Shell)
   * @see mitll.langtest.client.flashcard.StatsFlashcardFactory.StatsPracticePanel#onSetComplete()
   */
  void storeCurrent(Shell e) {
 //   System.out.println("StickyState.storeCurrent store current " + e.getID());
    storage.storeValue(CURRENT_EXERCISE, e.getID());
  }

  public String getCurrentExerciseID() {
    String value = storage.getValue(CURRENT_EXERCISE);
  //  System.out.println("StickyState.getCurrentExerciseID '" + value +"'");
    return value;
  }

  public void clearCurrent() { storage.removeValue(CURRENT_EXERCISE); }

  String getIncorrect() {
    return storage.getValue(INCORRECT);
  }

  String getCorrect() {
    return storage.getValue(CORRECT1);
  }

  String getScore() {
    return storage.getValue(SCORE);
  }

  void storeScore(StringBuilder builder3) {
    storage.storeValue(SCORE, builder3.toString());
  }

  void storeIncorrect(StringBuilder builder2) {
    storage.storeValue(INCORRECT, builder2.toString());
  }

  void storeCorrect(StringBuilder builder) {
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
