package mitll.langtest.shared.taboo;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.server.database.taboo.OnlineUsers;
import mitll.langtest.shared.ExerciseShell;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
* Created with IntelliJ IDEA.
* User: GO22670
* Date: 8/27/13
* Time: 4:00 PM
* To change this template use File | Settings | File Templates.
*/
public class Game implements IsSerializable {
  private List<ExerciseShell> allSelectedExercises;
  private int numGames;
  private int numExercisesInGame;

  public Game(){}

  public Game(List<ExerciseShell> allSelectedExercises) {
    System.out.println("Game with " + allSelectedExercises.size());
    this.allSelectedExercises = new ArrayList<ExerciseShell>(allSelectedExercises);
    numGames = (int) Math.ceil((float)allSelectedExercises.size()/(float)OnlineUsers.GAME_SIZE);
  }

  public List<ExerciseShell> startGame() {
    List<ExerciseShell> exercisesToDo = new ArrayList<ExerciseShell>();
    for (int i = 0; i < Math.min(allSelectedExercises.size(),OnlineUsers.GAME_SIZE); i++) {
      exercisesToDo.add(allSelectedExercises.get(i));
    }
    System.out.println("startGame... from " + allSelectedExercises.size());

    //OnlineUsers.logger.info("---> new game!");
   // List<ExerciseShell> exercisesToDo = randomSample2(allSelectedExercises, OnlineUsers.GAME_SIZE, rnd);
    allSelectedExercises.removeAll(exercisesToDo);

    System.out.println("startGame... returning " + exercisesToDo.size() + " : " + exercisesToDo);

    this.numExercisesInGame = exercisesToDo.size();
    return exercisesToDo;
  }

/*  public List<ExerciseShell> startGameOld(Random rnd) {
    System.out.println("startGame... from " + allSelectedExercises.size());

    //OnlineUsers.logger.info("---> new game!");
    List<ExerciseShell> exercisesToDo = randomSample2(allSelectedExercises, OnlineUsers.GAME_SIZE, rnd);
    allSelectedExercises.removeAll(exercisesToDo);

    System.out.println("startGame... returning " + exercisesToDo.size() + " : " + exercisesToDo);

    this.numExercisesInGame = exercisesToDo.size();
    return exercisesToDo;
  }*/

  public int getNumExercises() { return numExercisesInGame; }

  public boolean anyGamesRemaining() {
    return !allSelectedExercises.isEmpty();
  }
/*
  public int numGamesRemaining() {
    float ratio = (float) allSelectedExercises.size() / (float) OnlineUsers.GAME_SIZE;
    System.out.println("numGamesRemaining : ratio " + ratio);
    return (int) Math.ceil(ratio);
  }*/

  public int getNumGames() {
    return numGames;
  }
/*   private long storeTwo(long low, long high) {
    long combined = low;
    combined += high << 32;
    return combined;
  }*/

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
