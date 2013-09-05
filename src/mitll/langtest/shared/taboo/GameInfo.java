package mitll.langtest.shared.taboo;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.client.taboo.ReceiverExerciseFactory;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseShell;

import java.util.Date;
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
  protected List<ExerciseShell> itemsInGame;
  protected int gameCount = 0;
  protected long timestamp = System.currentTimeMillis();
  private int totalClues = 0;

  public GameInfo() {}

  public GameInfo(int numGames, List<ExerciseShell> itemsInGame, long timestamp, int gameCount) {
    this.numGames = numGames;
    this.itemsInGame = itemsInGame;
    if (itemsInGame == null || itemsInGame.isEmpty()) {
      System.err.println("GameInfo :game has no items???");
    }
    else {
      setTotalClues(itemsInGame);
    }
    this.timestamp = timestamp;
    //this.initNumExercises = itemsInGame == null ? -1 : itemsInGame.size();
    this.gameCount = gameCount;
  }

  protected void setTotalClues(List<ExerciseShell> itemsInGame) {
    totalClues = itemsInGame.size() * ReceiverExerciseFactory.MAX_CLUES_TO_GIVE;
   // System.out.println("GameInfo :totalClues : " + totalClues);
  }

  public ExerciseShell getNext(Exercise current) {
    int indexOfItem = getIndexOfItem(current);
    if (indexOfItem == -1) {
      System.err.println("GameInfo : getNextID can't find  : " + current.getID());
      return null;
    }
    else {
      return itemsInGame.get(indexOfItem + 1);
    }
  }

  public int getTotalClues() { return totalClues; }

  public int getIndexOfItem(Exercise e) {
    return getIndexOfItem(e.getID());
  }

  public int getIndexOfItem(String id) {
    if (itemsInGame == null) return -1;
    for (int i = 0; i < itemsInGame.size(); i++) {
      if (itemsInGame.get(i).getID().equals(id)) return i;
    }
    System.out.println("couldn't find " + id + " in " + itemsInGame);
    return -1;
  }

 // public ExerciseShell getFirst() { return itemsInGame.iterator().next(); }

  public int getNumGames() {
    return numGames;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp() { timestamp = System.currentTimeMillis(); }

  public int getNumExercises() {
    return itemsInGame == null ? -1 : itemsInGame.size();
  }

  public boolean onLast(Exercise e) {
    int i = getNumExercises() - 1;
    int indexOfItem = getIndexOfItem(e);
    System.out.println("GameInfo:onLast " + e.getID() + " last index " + i + " vs ex index " + indexOfItem);
    return i == indexOfItem;
  }

/*  public int getInitialNumExercises() {
    return initNumExercises;
  }*/

  protected void restartGames() {
    gameCount = 0;
  }

  protected void incrementGames() {
    gameCount++;
    setTimestamp();
  }

  public int getGameCount() { return gameCount; }

  public boolean anyGamesRemaining() {
    return gameCount < numGames;
  }

  public boolean hasStarted() { return itemsInGame != null; }

  public List<ExerciseShell> getGameItems() { return itemsInGame; }

  public String toString() { return "GameInfo : " + (hasStarted() ? " started " : " not started ") +
    "Count " + gameCount + " total num games " +
    numGames + " num exercises " + getNumExercises() + " clues " + totalClues +
    " timestamp " + new Date(timestamp); }
}
