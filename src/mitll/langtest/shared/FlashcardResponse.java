package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

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

  public FlashcardResponse() {}
  public FlashcardResponse(Exercise e, int correct, int incorrect) {this.e = e; this.correct =correct; this.incorrect = incorrect;}
}
