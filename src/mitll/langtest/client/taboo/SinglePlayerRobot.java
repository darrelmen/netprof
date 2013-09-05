package mitll.langtest.client.taboo;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.taboo.Game;
import mitll.langtest.shared.taboo.StimulusAnswerPair;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * TODO : add start/end game to protocol -- with starting a game, both sides get an item list.
 *  When come to the end of the list, the game ends, and we register the score, and show the leaderboard feedback.
 *
 * User: GO22670
 * Date: 8/15/13
 * Time: 12:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class SinglePlayerRobot {
  private final LangTestDatabaseAsync service;
  private List<ExerciseShell> exercisesRemaining = null;
  private Set<String> currentIDs = new HashSet<String>();
  private Exercise currentExercise = null;
  private List<Exercise.QAPair> clueAnswerPairs = Collections.emptyList();
  private int numClues = 0;

  /**
   * @see mitll.langtest.client.LangTest#setTabooFactory(long, boolean, boolean)
   * @param service
   */
  public SinglePlayerRobot(LangTestDatabaseAsync service) {
    this.service = service;
  }

  /**
   * @see ReceiverExerciseFactory.ReceiverPanel#checkForStimulus
   * @param async
   */
  public void checkForStimulus(AsyncCallback<StimulusAnswerPair> async) {
    if (exercisesRemaining == null) {
      System.out.println("SinglePlayerRobot.checkForStimulus " + exercisesRemaining);
      StimulusAnswerPair stimulusAnswerPair = new StimulusAnswerPair();
      stimulusAnswerPair.setNoStimYet(true);
      async.onSuccess(stimulusAnswerPair); // async query not complete yet
    } else {
      if (clueAnswerPairs.isEmpty()) {
        System.out.println("SinglePlayerRobot.checkForStimulus " + exercisesRemaining.size());
        getNextExercise(async,5);
      } else {
        replyWithNextClue(currentExercise.getID(), async);
      }
    }
  }

  private boolean isGameOver() {
    return exercisesRemaining.isEmpty();
  }

  /**
   * @see #checkForStimulus(com.google.gwt.user.client.rpc.AsyncCallback
   * @param async
   */
  private void getNextExercise(final AsyncCallback<StimulusAnswerPair> async, final int n) {
    ExerciseShell nextExerciseShell = exercisesRemaining.get(0);
    System.out.println("SinglePlayerRobot.getNextExercise : exercisesRemaining = " + exercisesRemaining.size() +
      " ex id = " + nextExerciseShell.getID());

    service.getExercise(nextExerciseShell.getID(), new AsyncCallback<Exercise>() {
      @Override
      public void onFailure(Throwable caught) {
        Window.alert("getExercise : Couldn't contact server.");
      }

      @Override
      public void onSuccess(Exercise result) {
        System.out.println("SinglePlayerRobot.getNextExercise.onSuccess got " + result);

        if (!currentIDs.contains(result.getID())) {
          System.out.println("SinglePlayerRobot.getNextExercise.onSuccess exercise is stale : " + result);

          if (n > 0) {
            getNextExercise(async, n - 1);
          } else { // should NEVER happen
            System.err.println("SinglePlayerRobot.getNextExercise.onSuccess exercise is stale : " + result);

          }
        } else {
          currentExercise = result;
          exercisesRemaining.remove(0);

          clueAnswerPairs = Game.randomSample2(currentExercise.getQuestions(), ReceiverExerciseFactory.MAX_CLUES_TO_GIVE, rnd);
          numClues = clueAnswerPairs.size();

          if (clueAnswerPairs.isEmpty()) {
            System.err.println("huh? no stim sentences for " + currentExercise);
            async.onSuccess(new StimulusAnswerPair(result.getID(), "Data error on server, please report.", "", false, false, numClues, true));
          } else {
            replyWithNextClue(currentExercise.getID(), async);
          }
        }
      }
    });
  }

  private void replyWithNextClue(String exerciseID, AsyncCallback<StimulusAnswerPair> async) {
    Exercise.QAPair clueAnswerPair = clueAnswerPairs.remove(0);
    String rawStim = clueAnswerPair.getQuestion();
    String answer = clueAnswerPair.getAnswer();
    async.onSuccess(new StimulusAnswerPair(exerciseID, rawStim, answer, clueAnswerPairs.isEmpty(), false, numClues, isGameOver()));
  }

  /**
   * @see ReceiverExerciseFactory.ReceiverPanel#registerAnswer(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.taboo.ReceiverExerciseFactory.ReceiverPanel, boolean, boolean)
   * @param correct
   */
  public void registerAnswer(boolean correct) {
    if (correct) clueAnswerPairs = Collections.emptyList();
  }

  private Game game;
  private Random rnd = new Random();

  /**
   * @see ReceiverExerciseFactory#setExerciseShells
   * @param exerciseShells
   */
  public void setExerciseShells(List<ExerciseShell> exerciseShells) {
    game = new Game(exerciseShells);
  }

  /**
   * @see ReceiverExerciseFactory#setExerciseShells(java.util.List, java.util.Map)
   * @return
   */
  public Game startGame() {
    exercisesRemaining = game.startGame();
    System.out.println("startGame " + exercisesRemaining.size());
    currentIDs.clear();
    for (ExerciseShell es : exercisesRemaining) {
      currentIDs.add(es.getID());
    }
    clueAnswerPairs = Collections.emptyList();
    return game;
  }
}
