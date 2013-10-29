package mitll.langtest.shared.flashcard;

import mitll.langtest.shared.Exercise;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 3/6/13
 * Time: 6:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class FlashcardResponse implements IsSerializable {
  private Exercise nextExercise;
  public int correct, incorrect;
  public boolean finished = false;
  private boolean onFirst = false;
  private boolean onLast = false;
  private double score;

  public FlashcardResponse() {}

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#getFlashcardResponse
   * @param e
   * @param correct
   * @param incorrect
   * @param onFirst
   * @param onLast
   */
  public FlashcardResponse(Exercise e, int correct, int incorrect, boolean onFirst, boolean onLast) {
    this.nextExercise = e;
    this.correct = correct;
    this.incorrect = incorrect;
    this.onFirst = onFirst;
    this.onLast = onLast;
  }

  public FlashcardResponse(boolean finished, int correct, int incorrect) {
    this.finished = finished;
    this.correct = correct;
    this.incorrect = incorrect;
  }

  public FlashcardResponse( int correct, int incorrect, double score) {
    this(false,correct,incorrect);
    this.score = score;
  }

  /**
   * @see mitll.langtest.client.bootstrap.BootstrapFlashcardExerciseList.FlashcardResponseAsyncCallback#onSuccess(FlashcardResponse)
   * @return
   */
  public Exercise getNextExercise() { return nextExercise; }
  public boolean isOnFirst() { return onFirst; }
  public boolean isOnLast() { return onLast; }

  public double getScore() {
    return score;
  }

  public String toString() {
    return "FlashcardResponse : " +
      (nextExercise != null ? "exercise id=" + nextExercise.getID() : " no exercise") +
      " correct " + correct + " incorrect " + incorrect +
      " finished= " + finished + " on first " + onFirst + " on last " + onLast;
  }
}
