package mitll.langtest.shared.taboo;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.shared.ExerciseShell;

import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 8/9/13
 * Time: 6:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class TabooState implements IsSerializable {
  private List<ExerciseShell> exerciseShells;
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
  public TabooState(boolean anyAvailable, boolean joinedPair, boolean giver, List<ExerciseShell> exerciseShells) {
    this.anyAvailable = anyAvailable;
    this.joinedPair = joinedPair;
    this.giver = giver;
    this.exerciseShells = exerciseShells;
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

  public List<ExerciseShell> getExerciseShells() {
    return exerciseShells;
  }

  public String toString() {
    String s = isAnyAvailable() ? " some available " : isJoinedPair() ? " just joined as " + (giver ? " giver " : " receiver ") : "none available";
    return s;
  }
}
