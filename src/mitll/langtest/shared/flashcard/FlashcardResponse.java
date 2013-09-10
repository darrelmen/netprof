package mitll.langtest.shared.flashcard;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.shared.Exercise;

import java.util.List;

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
  private List<Integer> correctHistory;

 // public List<Integer> getCorrectHistory() { return correctHistory; }

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#getFlashcardResponse
   * @param h
   */
  public void setCorrectHistory(List<Integer> h) { correctHistory = h; }

  public FlashcardResponse() {}

  public FlashcardResponse(Exercise e, int correct, int incorrect) {
    this.nextExercise = e;
    this.correct = correct;
    this.incorrect = incorrect;
  }
  public FlashcardResponse(boolean finished, int correct, int incorrect) {this.finished = finished; this.correct =correct; this.incorrect = incorrect;}

  public String toString() {
    return "FlashcardResponse : " +
      (nextExercise != null ? "exercise id=" + nextExercise.getID() : " no exercise") +
      " correct " + correct + " incorrect " + incorrect +
      " finished= " + finished +
      (correctHistory != null ? " " + correctHistory.size() + " items in history." : "");
  }

  /**
   * @see mitll.langtest.client.bootstrap.BootstrapFlashcardExerciseList.FlashcardResponseAsyncCallback#onSuccess(FlashcardResponse)
   * @return
   */
  public Exercise getNextExercise() {
    return nextExercise;
  }
}
