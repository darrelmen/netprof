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
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.exercise.Shell;

import java.util.*;

/**
 * Remember the state of the flashcards in the localStorage browser cache.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 7/8/14.
 */
public class StickyState {
  // private final Logger logger = Logger.getLogger("StickyState");
  private static final String INCORRECT = "Incorrect";
  private static final String SCORE = "Score";

  private static final String CURRENT_EXERCISE = "currentExercise";
  private static final String CORRECT1 = "correct";
  private final KeyStorage storage;


  private final Map<Integer, Boolean> exToCorrect = new HashMap<>();
  private final Map<Integer, Double> exToScore = new HashMap<>();
  private final Map<Integer, AudioAnswer> exToAnswer = new LinkedHashMap<>();

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
   * @see StatsPracticePanel#onSetComplete
   */
  public void storeCurrent(Shell e) {
    // logger.info("StickyState.storeCurrent store current " + e.getID());
    storage.storeValue(CURRENT_EXERCISE, "" + e.getID());
  }

  int getCurrentExerciseID() {
    String value = storage.getValue(CURRENT_EXERCISE);
    // logger.info("StickyState.getCurrentExerciseID '" + value + "'");
    return value.isEmpty() ? -1 : Integer.parseInt(value);
  }

  void clearCurrent() {
    storage.removeValue(CURRENT_EXERCISE);
  }

  private String getIncorrect() {
    return storage.getValue(INCORRECT);
  }

  private String getCorrect() {
    return storage.getValue(CORRECT1);
  }

  private String getScore() {
    return storage.getValue(SCORE);
  }


  public void populateCorrectMap() {
    String value = getCorrect();
    if (value != null && !value.trim().isEmpty()) {
      // logger.info("using correct map " + value);
      for (int ex : getIDsFromStorage(value)) {
        exToCorrect.put(ex, Boolean.TRUE);
      }
    }

    value = getIncorrect();
    if (value != null && !value.trim().isEmpty()) {
      //  logger.info("using incorrect map " + value);
      for (int ex : getIDsFromStorage(value)) {
        exToCorrect.put(ex, Boolean.FALSE);
      }
    }

    value = getScore();
    if (value != null && !value.trim().isEmpty()) {
      for (String pair : getIDsFroStorage(value)) {
        String[] split = pair.split("=");
        if (split.length == 2) {
          String s = split[0];
          int id = Integer.parseInt(s);
          exToScore.put(id, Double.parseDouble(split[1]));
        }
      }
    }
  }

  private Collection<Integer> getIDsFromStorage(String value) {
    String[] split = getIDsFroStorage(value);
    Collection<Integer> ids = new ArrayList<>();
    for (String ex : split) ids.add(Integer.parseInt(ex));
    return ids;
  }

  private String[] getIDsFroStorage(String value) {
    return value.split(",");
  }


  void resetStorage() {
    storage.removeValue(CORRECT1);
    storage.removeValue(INCORRECT);
    storage.removeValue(CURRENT_EXERCISE);
    storage.removeValue(SCORE);
  }

  void reset() {
    exToCorrect.clear();
    exToScore.clear();
    exToAnswer.clear(); // why wouldn't we do that too?
    clearCurrent();
  }

  void storeAnswer(final AudioAnswer result, int id) {
    // int id = exercise.getID();
    exToScore.put(id, result.getScore());
    exToCorrect.put(id, isCorrect(result.isCorrect(), result.getScore()));

    StringBuilder builder = new StringBuilder();
    StringBuilder builder2 = new StringBuilder();
    for (Map.Entry<Integer, Boolean> pair : exToCorrect.entrySet()) {
      if (pair.getValue()) {
        builder.append(pair.getKey()).append(",");
      } else {
        builder2.append(pair.getKey()).append(",");
      }
    }
    storeCorrect(builder);
    storeIncorrect(builder2);

    StringBuilder builder3 = new StringBuilder();
    for (Map.Entry<Integer, Double> pair : exToScore.entrySet()) {
      builder3.append(pair.getKey()).append("=").append(pair.getValue()).append(",");
    }
    storeScore(builder3);

    exToAnswer.put(id, result);
  }

  private void storeScore(StringBuilder builder3) {
    storage.storeValue(SCORE, builder3.toString());
  }

  private void storeIncorrect(StringBuilder builder2) {
    storage.storeValue(INCORRECT, builder2.toString());
  }

  private void storeCorrect(StringBuilder builder) {
    storage.storeValue(CORRECT1, builder.toString());
  }

  boolean isCorrect(boolean correct, double score) {
    return correct;
  }

  private Collection<Boolean> getCorrectValues() {
    return exToCorrect.values();
  }

  Collection<Double> getScores() {
    return exToScore.values();
  }

  public boolean isComplete(int num) {
    return num == exToScore.size();
  }

  Collection<AudioAnswer> getAnswers() {
    return exToAnswer.values();
  }

  public Map<Long, Integer> getTimeToID() {
    Map<Long, Integer> timeToID = new HashMap<>();
    exToAnswer.forEach((k, v) -> {
      timeToID.put(v.getTimestamp(), k);
    });
    return timeToID;
  }

  void clearAnswers() {
    exToAnswer.clear();
  }

  int getCorrectCount() {
    int count = 0;
    for (Boolean val : getCorrectValues()) {
      if (val) count++;
    }
    return count;
  }

  int getIncorrectCount() {
    int count = 0;
    for (Boolean val : getCorrectValues()) {
      if (!val) count++;
    }
    return count;
  }

  public AudioAnswer getAnswer(int id) {
    return exToAnswer.get(id);
  }
}
