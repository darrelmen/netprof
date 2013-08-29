package mitll.langtest.shared.taboo;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.shared.ExerciseShell;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 8/29/13
 * Time: 1:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class GameInfo implements IsSerializable {
  protected int numGames;
  // private  int numItemsInGame;
  protected List<ExerciseShell> itemsInGame;
  //private List<ExerciseShell> currentGameItems;
/*  private int numGames;
  private int numExercisesInGame;*/
  protected int gameCount = 0;

  public GameInfo() {
  }

  public GameInfo(int numGames, List<ExerciseShell> itemsInGame) {
    this.numGames = numGames;
    this.itemsInGame = itemsInGame;
  }

  public int getNumGames() {
    return numGames;
  }

  public int getNumExercises() {
    return itemsInGame.size();
  }

  protected void restartGames() {
    gameCount = 0;
  }

  protected void incrementGames() {
    gameCount++;
  }

  public boolean anyGamesRemaining() {
    return gameCount < numGames;
  }
}
