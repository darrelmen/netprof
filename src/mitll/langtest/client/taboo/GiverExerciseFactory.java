package mitll.langtest.client.taboo;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.Controls;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.RadioButton;
import com.github.gwtbootstrap.client.ui.Row;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.bootstrap.BootstrapExercisePanel;
import mitll.langtest.client.exercise.BusyPanel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.taboo.Game;
import mitll.langtest.shared.taboo.GameInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * TODO : add receiver factory -- user does a free form input of the guess...
 * what if they get part of the word?  feedback when they get it correct.
 * <p/>
 * User: GO22670
 * Date: 7/9/12
 * Time: 6:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class GiverExerciseFactory extends ExercisePanelFactory {
  private static final int CHECK_FOR_CORRECT_POLL_PERIOD = 1000;
  public static final int MAX_CLUES = 3;
  private int exerciseCount = 0;
  private int stimulusCount;
  //private int numExercisesInGame = 0;
  //private int numGames = 0;
 // private int gameCount;
 // private GameInfo game;
 private GameInfo gameInfo;

  GiverPanel giverPanel ;
  /**
   * @param service
   * @param userFeedback
   * @param controller
   * @see mitll.langtest.client.LangTest#setTabooFactory(long, boolean, boolean)
   */
  public GiverExerciseFactory(final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                              final ExerciseController controller) {
    super(service, userFeedback, controller);
  }

  /**
   * @see TabooExerciseList#rememberExercises(java.util.List)
   */
  public void startOver() { /*gameCount = 0; */}

  /**
   * @see mitll.langtest.client.exercise.ExerciseList#makeExercisePanel
   * @param e
   * @return
   */
  public Panel getExercisePanel(final Exercise e) {
    System.out.println("GiverExerciseFactory.getExercisePanel getting panel ...");
    controller.pingAliveUser();

    giverPanel = new GiverPanel(e);
    return giverPanel;
  }
  private long lastTimestamp;

  /**
   * @see TabooExerciseList#setGame(mitll.langtest.shared.taboo.GameInfo)
   * @param game
   */
  public void setGame(GameInfo game) {
    if (gameInfo == null) gameInfo = game;
    if (giverPanel != null) {
      int numExercises = game.getNumExercises();
      if (numExercises > -1 && game.getTimestamp() != lastTimestamp) {
        System.out.println("setGame : last timestamp " + lastTimestamp + " <> new timestamp " + game.getTimestamp() + " num exercises " + numExercises);
        giverPanel.startGame();
        giverPanel.showGame(gameInfo);
        this.gameInfo = game;
        lastTimestamp = game.getTimestamp();
      }
      //else {
      //}
    }
  }

  private class GiverPanel extends FluidContainer implements BusyPanel {
    private List<Exercise.QAPair> sentItems = new ArrayList<Exercise.QAPair>();
    private Map<RadioButton, Exercise.QAPair> choiceToExample;
    private Controls choice = new Controls();
    private final Button send = new Button("Send");
    private Heading pleaseWait = new Heading(4, "Please wait for receiver to answer...");
    List<Exercise.QAPair> clueAnswerPairs;
    private Set<Exercise.QAPair> otherExerciseClues = new HashSet<Exercise.QAPair>();
    private Set<String> otherClues = new HashSet<String>();
    private Heading exerciseDisplay = new Heading(3);
    private Heading stimulus = new Heading(3);
    private Set<String> validClues = new HashSet<String>();

    public GiverPanel(final Exercise exercise) {
      if (exercise == null) {
        System.err.println("huh? exercise is null?");
        return;
      }
      exerciseCount++;
     // System.out.println("GiverPanel ------------->");
      Row w5 = new Row();
      w5.add(exerciseDisplay);
      add(w5);

      clueAnswerPairs = Game.randomSample2(exercise.getQuestions(), ReceiverExerciseFactory.MAX_CLUES_TO_GIVE, rnd);

      if (gameInfo != null) {
        showGame(gameInfo);
      }
      else System.out.println("game info is null");
      Row w33 = new Row();
      w33.add(stimulus);
      add(w33);

      HTML warnNoFlash = new HTML(BootstrapExercisePanel.WARN_NO_FLASH);
      warnNoFlash.setVisible(false);
      final SoundFeedback soundFeedback = new SoundFeedback(controller.getSoundManager(), warnNoFlash);

      Row w4 = new Row();
      w4.add(new Heading(3, "User is trying to guess: "));
      add(w4);

      Row w = new Row();

      final String refSentence = controller.getProps().doTabooEnglish() ? exercise.getEnglishSentence().trim() : exercise.getRefSentence().trim();
      w.add(new Heading(2, refSentence));
      add(w);

      Row w2 = new Row();
      w2.add(new Column(12));
      add(w2);

      Row w3 = new Row();
      w3.add(new Heading(4, "Choose a hint sentence to send : "));
      add(w3);

      otherClues.clear();
      otherExerciseClues.clear();

      getUnrelatedClues(5);
      // recordingStyle.add(new ControlLabel("<b>Audio Recording Style</b>"));
      final ControlGroup recordingStyle = new ControlGroup(); // necessary?
      recordingStyle.add(choice);
      add(choice);

      add(warnNoFlash);
      send.setType(ButtonType.PRIMARY);
      send.setEnabled(true);
     // send.setTitle("Hit Enter to send.");

      send.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) { clickOnSend(exercise, soundFeedback); }
      });

      add(send);
      add(pleaseWait);
      pleaseWait.setVisible(false);
    }

    private void getUnrelatedClues(final int n) {
      controller.askForRandomExercise(new AsyncCallback<Exercise>() {
        @Override
        public void onFailure(Throwable caught) {
          //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void onSuccess(Exercise result) {
          List<Exercise.QAPair> questions = result.getQuestions();
        //  System.out.println("getUnrelatedClues " + n + " for " + result.getID() + " got " + questions.size());
          for (Exercise.QAPair pair : questions) {
            if (!otherClues.contains(pair.getQuestion())) {
              otherClues.add(pair.getQuestion());
              otherExerciseClues.add(pair);
            }
          }

          if (otherExerciseClues.size() > 5) {
            choiceToExample = populateChoices(clueAnswerPairs, otherExerciseClues);
          } else {
            if (n > 0) getUnrelatedClues(n - 1);
          }
        }
      });
    }

    private void startGame() {
      exerciseCount = 1;
      stimulusCount = 1;
    //  gameCount++;
    }

    private void showGame(GameInfo gameInfo) {
      String gameInfoString = "Game " + (gameInfo.getGameCount()) + " of " + gameInfo.getNumGames();
      int numExercises = gameInfo.getNumExercises();
      exerciseDisplay.setText(gameInfoString + ", item " + (exerciseCount) + " of " + numExercises);
      stimulus.setText("Clue " + (stimulusCount) + " of " + clueAnswerPairs.size());
    }

    private void clickOnSend(Exercise exercise, SoundFeedback soundFeedback) {
      final Exercise.QAPair stimulus = getSelectedItem();
      if (stimulus != null) {
        if (!validClues.contains(stimulus.getQuestion())) {
          showPopup("Sorry, that clue doesn't go with this item.  Please choose another.");
        }
        else {
          sentItems.add(stimulus);
          send.setEnabled(false);
          pleaseWait.setVisible(true);
          controller.pingAliveUser();
          boolean lastChoiceRemaining = (clueAnswerPairs.size() - sentItems.size()) == 1;
          sendStimulus(stimulus.getQuestion(), exercise,
            stimulus.getAnswer(), soundFeedback, lastChoiceRemaining, clueAnswerPairs.size());
        }
      } else {
        showPopup("Please select a sentence to send.");
      }
    }

    private Random rnd = new Random();
    private String lastSentExercise = "";

    /**
     * @see GiverPanel#clickOnSend
     * @param stimulus
     * @paramx refSentence
     * @param exercise
     * @param soundFeedback
     * @param lastChoiceRemaining
     * @param numClues
     */
    private void sendStimulus(final String stimulus, final  Exercise exercise,
                              String answer, final SoundFeedback soundFeedback,
                              boolean lastChoiceRemaining, int numClues) {
      //final String toSendWithBlankedOutItem = //getObfuscated(stimulus, refSentence);


     // List<String> alternateAnswers = exercise.getQuestions().get(0).getAlternateAnswers();
      final int user = controller.getUser();
      String exerciseID = exercise.getID();
      boolean differentExercise = lastSentExercise.length() > 0 && !lastSentExercise.equals(exerciseID);
      lastSentExercise = exerciseID;
      // TODO : figure out if this the last item in a game and the last stimulus
      boolean isGameOver = lastChoiceRemaining && exerciseCount == gameInfo.getNumExercises();
      stimulusCount++;
      service.sendStimulus(user, exerciseID, stimulus, answer, lastChoiceRemaining,
        differentExercise, numClues, isGameOver, new AsyncCallback<Integer>() {
        @Override
        public void onFailure(Throwable caught) { Window.alert("couldn't contact server."); }

        @Override
        public void onSuccess(Integer result) {
          if (result == 0) {
            System.out.println("sendStimulus.onSuccess : Giver " + user + " Sent '" + stimulus + "'");// and not '" + stimulus + "'");
            checkForCorrect(user, stimulus, exercise, /*refSentence,*/ soundFeedback);
          } else {
            //showUserState("Partner Signed Out","Your partner signed out, will check for another if any available.");
          }
        }
      });
    }


    /**
     * @see #clickOnSend
     * @return
     */
    private Exercise.QAPair getSelectedItem() {
      for (RadioButton choice : choiceToExample.keySet()) {
        if (choice.getValue()) {
          return choiceToExample.get(choice);
        }
      }
      return null;
    }

    /**
     * TODO : ask server what items have been sent instead of keeping in client, since if we reload the page,
     * we loose the history.  OR we could shove it into the cache...?
     *
     * TODO : show mix of items...
     *
     * @param clueAnswerPairs
     * @return
     */
    private Map<RadioButton, Exercise.QAPair> populateChoices(List<Exercise.QAPair> clueAnswerPairs,
                                                              Set<Exercise.QAPair> otherExerciseClues) {
      choice.clear();
      final Map<RadioButton, Exercise.QAPair> choiceToExample = new HashMap<RadioButton, Exercise.QAPair>();
      List<Exercise.QAPair> notSentYet = getNotSentYetHints(clueAnswerPairs);

      Set<String> cluePhrases = new HashSet<String>();

     // if (notSentYet.size() > MAX_CLUES) notSentYet = notSentYet.subList(0, MAX_CLUES);
      Set<Exercise.QAPair> allClues = new HashSet<Exercise.QAPair>(notSentYet);

      validClues.clear();
      Iterator<Exercise.QAPair> iterator = notSentYet.iterator();
      while (allClues.size() < MAX_CLUES && iterator.hasNext()) {
        Exercise.QAPair next = iterator.next();
        String clue = next.getQuestion();
        if (!cluePhrases.contains(clue)) {
          cluePhrases.add(clue);
          allClues.add(next);
          validClues.add(clue);
        }
      }

     iterator = otherExerciseClues.iterator();
      while (allClues.size() < ReceiverExerciseFactory.MAX_CLUES_TO_GIVE && iterator.hasNext()) {
        Exercise.QAPair next = iterator.next();
        if (!cluePhrases.contains(next.getQuestion())) {
          cluePhrases.add(next.getQuestion());
          allClues.add(next);
        }
      }

      List<Exercise.QAPair> clues = new ArrayList<Exercise.QAPair>(allClues);

      new Shuffler().shuffle2(clues, rnd);  // TODO parameterize?

      for (Exercise.QAPair example : clues) {
        final RadioButton give = new RadioButton("Giver", example.getQuestion());
        choiceToExample.put(give, example);
        choice.add(give);
      }
      return choiceToExample;
    }

    private List<Exercise.QAPair> getNotSentYetHints(List<Exercise.QAPair> clueAnswerPairs) {
      List<Exercise.QAPair> notSentYet = new ArrayList<Exercise.QAPair>();
     // System.out.println("getNotSentYetHints for " + synonymSentences.size());
      for (Exercise.QAPair candidate : clueAnswerPairs) {
        if (sentItems.contains(candidate)) {
          System.out.println("---> already sent " + candidate);
        } else {
          notSentYet.add(candidate);
        }
      }
     // System.out.println("getNotSentYetHints now " + notSentYet.size());

      return notSentYet;
    }

    int lastCorrectResponse = -2;
    /**
     * Keep asking server if the receiver has made a correct answer or not.
     *
     * @param userid
     * @param stimulus
     * @param current
     * @paramx refSentence
     */
    private void checkForCorrect(final long userid, final String stimulus, final Exercise current,
                                 //final String refSentence,
                                 final SoundFeedback feedback) {
      service.checkCorrect(userid, stimulus, new AsyncCallback<Integer>() {
        @Override
        public void onFailure(Throwable caught) {
          Window.alert("couldn't contact server.");
        }

        @Override
        public void onSuccess(Integer result) {
          if (lastCorrectResponse != result) {
            System.out.println("GiverExerciseFactory : checkForCorrect got " + result);
            lastCorrectResponse = result;
          }
          if (result == 0) { // incorrect
            feedback.playIncorrect();
            otherClues.clear();
            otherExerciseClues.clear();
            getUnrelatedClues(5);

            if (choiceToExample.isEmpty()) {
              showPopup("They didn't guess correctly, moving to next item...", new CloseHandler<PopupPanel>() {
                @Override
                public void onClose(CloseEvent<PopupPanel> event) {
                  controller.loadNextExercise(current);
                }
              });
            } else {
              showPopup("They didn't guess correctly, please send another sentence.");

              send.setEnabled(true);
              pleaseWait.setVisible(false);
            }
          } else if (result == 1) {
            lastSentExercise = ""; // clear last sent hint -- they got it correct
            showPopup("They guessed correctly!  Moving on to next item.");
            feedback.playCorrect();

            controller.loadNextExercise(current);
            send.setEnabled(true);
            pleaseWait.setVisible(false);
          } else { // they haven't answered yet
            Timer t = new Timer() {
              @Override
              public void run() {
                checkForCorrect(userid, stimulus, current, /*refSentence, */feedback);
              }
            };
            t.schedule(CHECK_FOR_CORRECT_POLL_PERIOD);
          }
        }
      });
    }

    private void showPopup(String html) {
      showPopup(html, null);
    }

    private void showPopup(String html, CloseHandler<PopupPanel> closeHandler) {
      final PopupPanel pleaseWait = new DecoratedPopupPanel();
      pleaseWait.setAutoHideEnabled(true);
      pleaseWait.add(new HTML(html));
      pleaseWait.center();
      if (closeHandler != null)
        pleaseWait.addCloseHandler(closeHandler);

      Timer t = new Timer() {
        @Override
        public void run() {
          pleaseWait.hide();
        }
      };
      t.schedule(3000);
    }

    @Override
    public boolean isBusy() {
      return pleaseWait.isVisible();
    }
  }
}
