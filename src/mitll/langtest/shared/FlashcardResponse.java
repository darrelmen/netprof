package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 3/6/13
 * Time: 6:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class FlashcardResponse implements IsSerializable {
  public Exercise e;
  public int correct, incorrect;
  public boolean finished = false;
  private List<Integer> correctHistory;

  public List<Integer> getCorrectHistory() { return correctHistory; }
  public void setCorrectHistory(List<Integer> h) { correctHistory = h; }

  public FlashcardResponse() {}
  public FlashcardResponse(Exercise e, int correct, int incorrect) {this.e = e; this.correct =correct; this.incorrect = incorrect;}
  public FlashcardResponse(boolean finished, int correct, int incorrect) {this.finished = finished; this.correct =correct; this.incorrect = incorrect;}
}
