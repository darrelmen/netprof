/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.flashcard;

import mitll.langtest.client.custom.KeyStorage;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.shared.exercise.Shell;

import java.util.logging.Logger;

/**
 * Remember the state of the flashcards in the localStorage browser cache.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 7/8/14.
 */
class StickyState {
  private final Logger logger = Logger.getLogger("StickyState");

  private static final String INCORRECT = "Incorrect";
  private static final String SCORE = "Score";

  private static final String CURRENT_EXERCISE = "currentExercise";
  private static final String CORRECT1 = "correct";
  private final KeyStorage storage;

  /**
   * @param storage
   * @see StatsFlashcardFactory
   */
  StickyState(KeyStorage storage) {
    this.storage = storage;
  }

  /**
   * @param e
   * @see ExercisePanelFactory#getExercisePanel(Shell)
   * @see mitll.langtest.client.flashcard.StatsFlashcardFactory.StatsPracticePanel#onSetComplete()
   */
  void storeCurrent(Shell e) {
    logger.info("StickyState.storeCurrent store current " + e.getID());
    storage.storeValue(CURRENT_EXERCISE, "" + e.getID());
  }

  int getCurrentExerciseID() {
    String value = storage.getValue(CURRENT_EXERCISE);
    logger.info("StickyState.getCurrentExerciseID '" + value + "'");
    return value.isEmpty() ? -1 : Integer.parseInt(value);
  }

  void clearCurrent() {
    storage.removeValue(CURRENT_EXERCISE);
  }

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

  void resetStorage() {
//    System.out.println("StickyState : reset storage for "+ storage);
    storage.removeValue(CORRECT1);
    storage.removeValue(INCORRECT);
    storage.removeValue(CURRENT_EXERCISE);
    storage.removeValue(SCORE);
  }
}
