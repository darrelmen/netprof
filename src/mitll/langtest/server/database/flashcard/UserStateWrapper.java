package mitll.langtest.server.database.flashcard;

import mitll.flashcard.UserState;
import mitll.langtest.shared.Exercise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

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
  private int pincorrect = 0;

  private int counter = 0;
  private final List<Exercise> exercises;
  private final Set<String> completed = new HashSet<String>();
  private final Random random;
  private boolean initial = true;
  /**
   * @see mitll.langtest.server.database.DatabaseImpl#getUserStateWrapper(long, java.util.List)
   * @param state
   * @param userID
   * @param exercises
   */
  public UserStateWrapper(UserState state, long userID, List<Exercise> exercises) {
    this.state = state;
    this.random = new Random(userID);
    if (exercises != null) {
      this.exercises = new ArrayList<Exercise>(exercises);
      Collections.shuffle(exercises, random);
    }
    else this.exercises = Collections.emptyList();
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
  public void addCompleted(String exid) { completed.add(exid); }

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#getCompletedExercises(int)
   * @return
   */
  public Set<String> getCompleted() { return completed; }

  public void setIncorrect(int incorrect) {
    this.incorrect = incorrect;
  }

  public int getNumExercises() { return exercises.size();  }

  public boolean isComplete() { return counter == exercises.size(); }

  public void reset() {
    correct = 0;
    incorrect = 0;
    shuffle();
  }

  public void shuffle() {
    Collections.shuffle(exercises, random);
    counter = 0;
  }

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#getFlashcardResponse(long, boolean, java.util.List, boolean)
   * @return
   */
  public Exercise getNextExercise() {
    if (initial) return getFirst();
    else {
      return exercises.get(++counter % exercises.size());
    }
  }

  private Exercise getFirst() {
    if (exercises.isEmpty()) {
      System.err.println("huh? no exercises in " + this);
      return null;
    }
    initial = false;
    return exercises.iterator().next();
  }

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#getFlashcardResponse(long, boolean, java.util.List, boolean)
   * @return
   */
  public Exercise getPrevExercise() {
    if (counter == 0) {
      counter = exercises.size();
    }
    return exercises.get(--counter % exercises.size());
  }

  public boolean onFirst() {  return counter == 0;  }
  public boolean onLast() {   return counter % exercises.size() == 0;  }

  public int getPincorrect() {  return pincorrect;  }

  public void setPincorrect(int pincorrect) {  this.pincorrect = pincorrect;  }

  public String toString() {
    return "UserState : correct " + correct + " incorrect " + getIncorrect() +
      " num exercises " + exercises.size() + " is complete " + isComplete() + " num completed " + completed.size();
  }
}
