package mitll.langtest.shared.taboo;

import mitll.langtest.server.database.taboo.OnlineUsers;
import mitll.langtest.shared.ExerciseShell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
* Created with IntelliJ IDEA.
* User: GO22670
* Date: 8/27/13
* Time: 4:00 PM
* To change this template use File | Settings | File Templates.
*/
public class Game extends GameInfo {
  private List<ExerciseShell> allSelectedExercises;

  public Game(){}

  public GameInfo getGameInfo() {
    if (getGameItems() == null) System.err.println("getGameInfo : huh? game items is null?");
    return new GameInfo(getNumGames(), getGameItems(), getTimestamp());
  }

  public Game(List<ExerciseShell> allSelectedExercises) {
    System.out.println("Game with " + allSelectedExercises.size());
    this.allSelectedExercises = Collections.unmodifiableList(allSelectedExercises);
    numGames = (int) Math.ceil((float)allSelectedExercises.size()/(float)OnlineUsers.GAME_SIZE);
  }

  /**
   * @see OnlineUsers#startGame(long)
   * @return
   */
  public List<ExerciseShell> startGame() {
    List<ExerciseShell> exercisesToDo = new ArrayList<ExerciseShell>();
    if (!anyGamesRemaining()) {
      System.out.println("wrap around... ?");
      restartGames();
    }
    int fromIndex = gameCount * OnlineUsers.GAME_SIZE;
    int endIndex = Math.min(allSelectedExercises.size(), (gameCount + 1) * OnlineUsers.GAME_SIZE);
    incrementGames();
    exercisesToDo.addAll(allSelectedExercises.subList(fromIndex, endIndex));
    System.out.println("Game.startGame... from " + allSelectedExercises.size() + " (" +fromIndex+
      "-" +endIndex+
      ") items, " + "startGame... returning " + exercisesToDo.size() + " : " + exercisesToDo);

    this.itemsInGame = exercisesToDo;
    initNumExercises = itemsInGame.size();
    return exercisesToDo;
  }

  public String toString() { return "Game : " + super.toString(); }

  public static <T> List<T> randomSample2(List<T> items, int m, Random rnd){
    if (m > items.size()) m = items.size();
    for(int i=0;i<m;i++){
      int pos = i + rnd.nextInt(items.size() - i);
      T tmp = items.get(pos);
      items.set(pos, items.get(i));
      items.set(i, tmp);
    }
    return new ArrayList<T>(items.subList(0, m));
  }
}
