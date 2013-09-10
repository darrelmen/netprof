package mitll.langtest.server.database.flashcard;

import mitll.flashcard.UserState;
import mitll.langtest.shared.Exercise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
* Created with IntelliJ IDEA.
* User: GO22670
* Date: 8/21/13
* Time: 3:23 PM
* To change this template use File | Settings | File Templates.
*/
public class UserStateWrapper {
  public final UserState state;
  private int correct = 0;
  private int incorrect = 0;

  private int pcorrect = 0;
  private int pincorrect = 0;

  private int counter = 0;
  private List<Integer> correctHistory = new ArrayList<Integer>();
  private final List<Exercise> exercises;
  private final Random random;

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#getUserStateWrapper(long, java.util.List)
   * @param state
   * @param userID
   * @param exercises
   */
  public UserStateWrapper(UserState state, long userID, List<Exercise> exercises) {
    this.state = state;
    this.random = new Random(userID);
    this.exercises = new ArrayList<Exercise>(exercises);
    Collections.shuffle(exercises, random);
  }

  public int getCorrect() {
    return correct;
  }

  public void setCorrect(int correct) {
    this.correct = correct;
  }

  public int getIncorrect() {
    return incorrect;
  }

  public void setIncorrect(int incorrect) {
    this.incorrect = incorrect;
  }

  public List<Integer> getCorrectHistory() { return correctHistory; }

  public int getNumExercises() {
    return exercises.size();
  }

  public boolean isComplete() { return counter == exercises.size(); }

  public void reset() {
    correctHistory.add(correct);
    correct = 0;
    incorrect = 0;
    shuffle();
  }

  public void shuffle() {
    Collections.shuffle(exercises, random);
    counter = 0;
  }

  public Exercise getNextExercise() {
    return exercises.get(counter++ % exercises.size()); // defensive
  }

  public int getPcorrect() {
    return pcorrect;
  }

  public void setPcorrect(int pcorrect) {
    this.pcorrect = pcorrect;
  }

  public int getPincorrect() {
    return pincorrect;
  }

  public void setPincorrect(int pincorrect) {
    this.pincorrect = pincorrect;
  }

  public String toString() {
    return "UserState : correct " + correct + " incorrect " + getIncorrect() +
      " num exercises " + getNextExercise() + " is complete " + isComplete();
  }
}
