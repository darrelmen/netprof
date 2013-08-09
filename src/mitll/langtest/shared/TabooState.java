package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 8/9/13
 * Time: 6:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class TabooState implements IsSerializable {
  public boolean anyAvailable;
  public boolean joinedPair;

  public TabooState() {}

  public TabooState(boolean anyAvailable, boolean joinedPair) {
    this.anyAvailable = anyAvailable;
    this.joinedPair = joinedPair;
  }

  public String toString() { return anyAvailable ? " some available " : joinedPair ? " just joined!" : "none available"; }
}
