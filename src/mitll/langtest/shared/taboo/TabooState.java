package mitll.langtest.shared.taboo;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 8/9/13
 * Time: 6:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class TabooState implements IsSerializable {
  private boolean anyAvailable;
  private boolean joinedPair;
  private boolean giver;

  public TabooState() {}

  /**
   * @see mitll.langtest.server.database.taboo.OnlineUsers#anyAvailable(long)
   * @param anyAvailable
   * @param joinedPair
   * @param giver
   */
  public TabooState(boolean anyAvailable, boolean joinedPair, boolean giver) {
    this.anyAvailable = anyAvailable;
    this.joinedPair = joinedPair;
    this.giver = giver;
  }

  public boolean isAnyAvailable() {
    return anyAvailable;
  }

  public boolean isJoinedPair() {
    return joinedPair;
  }

  public boolean isGiver() {
    return giver;
  }

  public String toString() {
    String s = isAnyAvailable() ? " some available " : isJoinedPair() ? " just joined as " + (giver ? " giver " : " receiver ") : "none available";
    return s;
  }
}
