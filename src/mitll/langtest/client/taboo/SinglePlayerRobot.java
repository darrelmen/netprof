package mitll.langtest.client.taboo;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.taboo.Game;
import mitll.langtest.shared.taboo.GameInfo;
import mitll.langtest.shared.taboo.StimulusAnswerPair;

import java.util.Collections;
import java.util.List;
import java.util.Random;

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
  private Exercise currentExercise = null;
  private List<String> synonymSentences = Collections.emptyList();
  private int numClues = 0;
  private PropertyHandler propertyHandler;

  /**
   * @see mitll.langtest.client.LangTest#setTabooFactory(long, boolean, boolean)
   * @param service
   * @param propertyHandler
   */
  public SinglePlayerRobot(LangTestDatabaseAsync service, PropertyHandler propertyHandler) {
    this.service = service;
    this.propertyHandler = propertyHandler;
  }

  /**
   * @see ReceiverExerciseFactory.ReceiverPanel#checkForStimulus
   * @param async
   */
  public void checkForStimulus(AsyncCallback<StimulusAnswerPair> async) {
    if (exercisesRemaining == null) {
   //   System.out.println("checkForStimulus " + exercisesRemaining);
      StimulusAnswerPair stimulusAnswerPair = new StimulusAnswerPair();
      stimulusAnswerPair.setNoStimYet(true);
      async.onSuccess(stimulusAnswerPair); // async query not complete yet
    } else {
     /* if (exercisesRemaining.isEmpty() && synonymSentences.isEmpty()) {
        async.onSuccess(new StimulusAnswerPair(true,!anyGamesRemaining())); // no more chapters, no more exercises, we're done -- TODO : start over?
      } else {*/
        if (synonymSentences.isEmpty()) {
       //   System.out.println("checkForStimulus " + exercisesRemaining.size());
          getNextExercise(async);
        } else {
          String rawStim = synonymSentences.remove(0);
          final String refSentence = getRefSentence();
          boolean empty = synonymSentences.isEmpty();
          // System.out.println("stim left " + synonymSentences.size() + " empty " + empty);

          String obfuscated = getObfuscated(rawStim, refSentence);
          async.onSuccess(new StimulusAnswerPair(currentExercise.getID(), obfuscated, refSentence, empty, false, numClues, isGameOver()));
        }
      //}
    }
  }

  private boolean isGameOver() {
    return exercisesRemaining.isEmpty() && synonymSentences.isEmpty();
  }

  /**
   * @see #checkForStimulus(com.google.gwt.user.client.rpc.AsyncCallback
   * @param async
   */
  private void getNextExercise(final AsyncCallback<StimulusAnswerPair> async) {
    System.out.println("SinglePlayerRobot.getNextExercise exercisesRemaining = " + exercisesRemaining.size());

    ExerciseShell nextExerciseShell = exercisesRemaining.get(0);
    System.out.println("SinglePlayerRobot.getNextExercise for = " + nextExerciseShell.getID());

    service.getExercise(nextExerciseShell.getID(), new AsyncCallback<Exercise>() {
      @Override
      public void onFailure(Throwable caught) {
        Window.alert("Couldn't contact server.");
      }

      @Override
      public void onSuccess(Exercise result) {
        System.out.println("SinglePlayerRobot.getNextExercise.onSuccess got " + result);

        currentExercise = result;
        exercisesRemaining.remove(0);

        synonymSentences = Game.randomSample2(currentExercise.getSynonymSentences(), ReceiverExerciseFactory.MAX_CLUES_TO_GIVE, rnd);
        numClues = synonymSentences.size();
        final String refSentence = getRefSentence();

        if (synonymSentences.isEmpty()) {
          System.err.println("huh? no stim sentences for " + currentExercise);
          async.onSuccess(new StimulusAnswerPair(result.getID(), "Data error on server, please report.", refSentence, false, false, numClues, true));
        } else {
          String rawStim = synonymSentences.remove(0);
          boolean empty = synonymSentences.isEmpty();
          // System.out.println("getNextExercise stim left " + synonymSentences.size() + " empty " + empty);
          async.onSuccess(new StimulusAnswerPair(result.getID(), getObfuscated(rawStim, refSentence), refSentence,
            empty, false, numClues, isGameOver()));
        }
      }
    });
  }

  private String getRefSentence() {
    return propertyHandler.doTabooEnglish() ? currentExercise.getEnglishSentence().trim() : currentExercise.getRefSentence().trim();
  }

  private String getObfuscated(String exampleToSend, String refSentence) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < refSentence.length(); i++) builder.append('_');
/*    if (!exampleToSend.contains(refSentence)) {
      System.err.println("huh? '" + exampleToSend + "' doesn't contain '" + refSentence + "'");
    }*/
    return exampleToSend.replaceAll(refSentence, builder.toString());
  }

  /**
   * @see ReceiverExerciseFactory.ReceiverPanel#registerAnswer(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.taboo.ReceiverExerciseFactory.ReceiverPanel, boolean, boolean)
   * @param correct
   */
  public void registerAnswer(boolean correct) {
    if (correct) synonymSentences = Collections.emptyList();
  }

  public GameInfo getGame() { return game; }
  public boolean anyGamesRemaining() { return game.anyGamesRemaining(); }

  private Game game;
  private Random rnd = new Random();

  /**
   * @see ReceiverExerciseFactory#setExerciseShells
   * @param exerciseShells
   */
  public void setExerciseShells(List<ExerciseShell> exerciseShells) {
    game = new Game(exerciseShells);
  //  startGame();
    //exercisesRemaining = new ArrayList<ExerciseShell>(exerciseShells);
    //Random rand = new Random();

    //shuffler.shuffle(exercisesRemaining, rand);
  }

  public Game startGame() {
    exercisesRemaining = game.startGame();

    synonymSentences = Collections.emptyList();
    return game;
  }

}
